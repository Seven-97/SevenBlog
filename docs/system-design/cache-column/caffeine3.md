---
title: Caffeine - 数据驱逐淘汰机制与用法
category: 系统设计
tag:
 - 缓存
 - Caffeine
---



> 来源：稀土掘金社区[深入理解缓存原理与实战设计](https://juejin.cn/column/7140852038258147358)，Seven进行了部分补充完善



## Caffeine的异步淘汰清理机制

在惰性删除实现机制这边，Caffeine做了一些改进优化以提升在并发场景下的性能表现。我们可以和Guava Cache的基于容量大小的淘汰处理做个对比。

当限制了`Guava Cache`最大容量之后，有新的记录写入超过了总大小，会立即触发数据淘汰策略，然后腾出空间给新的记录写入。比如下面这段逻辑：

```java
public static void main(String[] args) {
    Cache<String, String> cache = CacheBuilder.newBuilder()
            .maximumSize(1)
            .removalListener(notification -> System.out.println(notification.getKey() + "被移除，原因：" + notification.getCause()))
            .build();
    cache.put("key1", "value1");
    System.out.println("key1写入后，当前缓存内的keys：" + cache.asMap().keySet());
    cache.put("key2", "value1");
    System.out.println("key2写入后，当前缓存内的keys：" + cache.asMap().keySet());
}
```

其运行后的结果显示如下，可以很明显的看出，超出容量之后继续写入，会在**写入前先执行缓存移除**操作。

```css
key1写入后，当前缓存内的keys：[key1]
key1被移除，原因：SIZE
key2写入后，当前缓存内的keys：[key2]
```

同样地，我们看下使用`Caffeine`实现一个限制容量大小的缓存对象的处理表现，代码如下：

```java
public static void main(String[] args) {
    Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(1)
            .removalListener((key, value, cause) -> System.out.println(key + "被移除，原因：" + cause))
            .build();
    cache.put("key1", "value1");
    System.out.println("key1写入后，当前缓存内的keys：" + cache.asMap().keySet());
    cache.put("key2", "value1");
    System.out.println("key2写入后，当前缓存内的keys：" + cache.asMap().keySet());
}
```

运行这段代码，会发现Caffeine的容量限制功能似乎“**失灵**”了！从输出结果看**并没有限制住**：

```css
key1写入后，当前缓存内的keys：[key1]
key2写入后，当前缓存内的keys：[key1, key2]
```



什么原因呢？

`Caffeine`为了提升读写操作的并发效率而将数据淘汰清理操作改为了**异步处理**，而异步处理时会有微小的延时，由此导致了上述看到的容量控制“失灵”现象。为了证实这一点，我们对上述的测试代码稍作修改，打印下调用线程与数据淘汰清理线程的线程ID，并且最后添加一个sleep等待操作：

```java
public static void main(String[] args) throws Exception {
    System.out.println("当前主线程：" + Thread.currentThread().getId());
    Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(1)
            .removalListener((key, value, cause) ->
                    System.out.println("数据淘汰执行线程：" + Thread.currentThread().getId()
                            + "，" + key + "被移除，原因：" + cause))
            .build();
    cache.put("key1", "value1");
    System.out.println("key1写入后，当前缓存内的keys：" + cache.asMap().keySe());
    cache.put("key2", "value1");
    Thread.sleep(1000L); // 等待一段时间时间，等待异步清理操作完成
    System.out.println("key2写入后，当前缓存内的keys：" + cache.asMap().keySet());
}
```

再次执行上述测试代码，发现结果变的符合预期了，也可以看出Caffeine的确是另起了**独立线程**去执行*数据淘汰*操作的。

```css
当前主线程：1
key1写入后，当前缓存内的keys：[key1]
数据淘汰执行线程：13，key1被移除，原因：SIZE
key2写入后，当前缓存内的keys：[key2]
```



深扒一下源码的实现，可以发现`Caffeine`在读写操作时会使用**独立线程**池执行对应的清理任务，如下图中的调用链执行链路 —— 这也证实了上面我们的分析。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092212845.webp)

所以，严格意义来说，Caffeine的大小容量限制并不能够保证完全精准的小于设定的值，会存在**短暂的误差**，但是作为一个以*高并发吞吐量*为优先考量点的组件而言，这一点点的误差也是可以接受的。关于这一点，如果阅读源码仔细点的小伙伴其实也可以发现在很多场景的注释中，Caffeine也都会有明确的说明。比如看下面这段从源码中摘抄的描述，就清晰的写着“**如果有同步执行的插入或者移除操作，实际的元素数量可能会出现差异**”。

```java
public interface Cache<K, V> {
    /**
     * Returns the approximate number of entries in this cache. The value returned is an estimate; the
     * actual count may differ if there are concurrent insertions or removals, or if some entries are
     * pending removal due to expiration or weak/soft reference collection. In the case of stale
     * entries this inaccuracy can be mitigated by performing a {@link #cleanUp()} first.
     *
     * @return the estimated number of mappings
     */
    @NonNegative
    long estimatedSize();

  // 省略其余内容...
}
```

同样道理，不管是基于大小、还是基于过期时间或基于引用的数据淘汰策略，由于数据淘汰处理是异步进行的，都会存在**短暂**的**不够精确**的情况。



## Caffeine多种数据驱逐机制

上面提到并演示了Caffeine基于整体容量进行的数据驱逐策略。除了基于容量大小之外，Caffeine还支持基于时间与基于引用等方式来进行数据驱逐处理。

### 基于时间

Caffine支持**基于时间**进行数据的淘汰驱逐处理。这部分的能力与Guava Cache相同，支持根据记录**创建时间**以及**访问时间**两个维度进行处理。

数据的过期时间在创建缓存对象的时候进行指定，Caffeine在创建缓存对象的时候提供了`3种`设定过期策略的方法。

| 方式              | 具体说明                                                     |
| ----------------- | ------------------------------------------------------------ |
| expireAfterWrite  | 基于创建时间进行过期处理                                     |
| expireAfterAccess | 基于最后访问时间进行过期处理                                 |
| expireAfter       | 基于**个性化定制**的逻辑来实现过期处理（可以定制基于`新增`、`读取`、`更新`等场景的过期策略，甚至支持为*不同记录指定不同过期时间*） |

下面逐个看下。

#### expireAfterWrite

`expireAfterWrite`用于指定数据创建之后多久会过期，使用方式举例如下：

```java
Cache<String, User> userCache = 
    Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.SECONDS)
        .build();
userCache.put("123", new User("123", "张三"))；
```

当记录被写入缓存之后达到指定的时间之后，就会被过期淘汰（**惰性删除**，并不会立即从内存中移除，而是在下一次操作的时候触发清理操作）。



#### expireAfterAccess

`expireAfterAccess`用于指定缓存记录多久没有被访问之后就会过期。使用方式与expireAfterWrite类似：

```java
Cache<String, User> userCache = 
    Caffeine.newBuilder()
        .expireAfterAccess(1, TimeUnit.SECONDS)
        .build();
    userCache.get("123", s -> userDao.getUser(s));
```

这种是基于最后一次访问时间来计算数据是否过期，如果一个数据一直被访问，则其就不会过期。比较适用于**热点数据**的存储场景，可以保证较高的缓存命中率。同样地，数据过期时也不会被立即从内存中移除，而是基于**惰性删除**机制进行处理。



#### expireAfter

上面两种设定过期时间的策略与Guava Cache是相似的。为了提供更为灵活的过期时间设定能力，Caffeine提供了一种全新的的过期时间设定方式，也即这里要介绍的`expireAfter`方法。其支持传入一个自定义的`Expiry`对象，自行实现数据的过期策略，甚至是针对不同的记录来定制不同的过期时间。

先看下Expiry接口中需要实现的三个方法：

| 方法名称          | 含义说明                                                     |
| ----------------- | ------------------------------------------------------------ |
| expireAfterCreate | 指定一个过期时间，从记录创建的时候开始计时，超过指定的时间之后就过期淘汰，效果类似`expireAfterWrite`，但是支持**更灵活**的定制逻辑。 |
| expireAfterUpdate | 指定一个过期时间，从记录最后一次被更新的时候开始计时，超过指定的时间之后就过期。每次执行**更新操作**之后，都会重新计算过期时间。 |
| expireAfterRead   | 指定一个过期时间，从记录最后一次被访问的时候开始计时，超过指定时间之后就过期。效果类似`expireAfterAccess`，但是支持**更高级**的定制逻辑。 |

比如下面的代码中，定制了`expireAfterCreate`方法的逻辑，根据缓存key来决定过期时间，如果key以字母A开头则设定1s过期，否则设定2s过期：

```java
public static void main(String[] args) {
    try {
        LoadingCache<String, User> userCache = Caffeine.newBuilder()
                .removalListener((key, value, cause) -> {
                    System.out.println(key + "移除，原因：" + cause);
                })
                .expireAfter(new Expiry<String, User>() {
                    @Override
                    public long expireAfterCreate(@NonNull String key, @NonNullUser value, long currentTime) {
                        if (key.startsWith("A")) {
                            return TimeUnit.SECONDS.toNanos(1);
                        } else {
                            return TimeUnit.SECONDS.toNanos(2);
                        }
                    }
                    @Override
                    public long expireAfterUpdate(@NonNull String key, @NonNullUser value, long currentTime,
                                                  @NonNegative longcurrentDuration) {
                        return Long.MAX_VALUE;
                    }
                    @Override
                    public long expireAfterRead(@NonNull String key, @NonNull Uservalue, long currentTime,
                                                @NonNegative long currentDuration){
                        return Long.MAX_VALUE;
                    }
                })
                .build(key -> userDao.getUser(key));
        userCache.put("123", new User("123", "123"));
        userCache.put("A123", new User("A123", "A123"));
        Thread.sleep(1100L);
        System.out.println(userCache.get("123"));
        System.out.println(userCache.get("A123"));
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

执行代码进行测试，可以发现，**不同的key拥有了不同的过期时间**：

```ini
User(userName=123, userId=123, departmentId=null)
A123移除，原因：EXPIRED
User(userName=A123, userId=A123, departmentId=null)
```

除了根据`key`来定制不同的过期时间，也可以根据`value`的内容来指定不同的过期时间策略。也可以同时定制上述三个方法，搭配来实现更复杂的过期策略。

按照这种方式来定时过期时间的时候需要注意一点，如果不需要设定某一维度的过期策略的时候，需要将对应实现方法的返回值设置为一个非常大的数值，比如可以像上述示例代码中一样，指定为`Long.MAX_VALUE`值。



### 基于大小

除了前面提到的基于访问时间或者创建时间来执行数据过期淘汰的方式之外，Caffeine还支持针对缓存**总体容量**大小进行限制，如果容量满的时候，基于`W-TinyLFU`算法，淘汰最不常被使用的数据，腾出空间给新的记录写入。

Caffeine支持按照`Size`（记录条数）或者按照W`eighter`（记录权重）值进行总体容量的限制。

#### maximumSize

在创建Caffeine缓存对象的时候，可以通过`maximumSize`来指定允许缓存的最大条数。

比如下面这段代码：

```java
Cache<Integer, String> cache = Caffeine.newBuilder()
        .maximumSize(1000L) // 限制最大缓存条数
        .build();
```



#### maximumWeight

在创建Caffeine缓存对象的时候，可以通过`maximumWeight`与`weighter`组合的方式，指定按照权重进行限制缓存总容量。比如一个字符串value值的缓存场景下，我们可以根据字符串的长度来计算权重值，最后根据总权重大小来限制容量。

代码示意如下：

```java
Cache<Integer, String> cache = Caffeine.newBuilder()
        .maximumWeight(1000L) // 限制最大权重值
        .weigher((key, value) -> (String.valueOf(value).length() / 1000) + 1)
        .build();
```



#### 使用注意点

需要注意一点：如果创建的时候指定了`weighter`，则必须同时指定`maximumWeight`值，如果不指定、或者指定了maximumSize，会**报错**（这一点与Guava Cache一致）：

```css
java.lang.IllegalStateException: weigher requires maximumWeight
	at com.github.benmanes.caffeine.cache.Caffeine.requireState(Caffeine.java:201)
	at com.github.benmanes.caffeine.cache.Caffeine.requireWeightWithWeigher(Caffeine.java:1215)
	at com.github.benmanes.caffeine.cache.Caffeine.build(Caffeine.java:1099)
	at com.veezean.skills.cache.caffeine.CaffeineCacheService.main(CaffeineCacheService.java:254)
```



### 基于引用

基于引用回收的策略，核心是利用`JVM`虚拟机的**GC机制**来达到数据清理的目的。当一个对象不再被引用的时候，JVM会选择在适当的时候将其回收。Caffeine支持`三种`不同的基于引用的回收方法：

| 方法       | 具体说明                                                     |
| ---------- | ------------------------------------------------------------ |
| weakKeys   | 采用`弱引用`方式存储key值内容，当key对象不再被引用的时候，由**GC**进行回收 |
| weakValues | 采用`弱引用`方式存储value值内容，当value对象不再被引用的时候，由**GC**进行回收 |
| softValues | 采用`软引用`方式存储value值内容，当内存容量满时基于**LRU**策略进行回收 |

关于JVM不同引用的介绍看这篇文章[GC - 理论基础 | Seven的菜鸟成长之路 (seven97.top)](https://www.seven97.top/java/jvm/02-gc1-theory.html#引用类型)

#### weakKeys

默认情况下，我们创建出一个Caffeine缓存对象并写入`key-value`映射数据时，key和value都是以**强引用**的方式存储的。而使用`weakKeys`可以指定将缓存中的key值以**弱引用**（WeakReference）的方式进行存储，这样一来，如果程序运行时没有其它地方使用或者依赖此缓存值的时候，该条记录就可能会被`GC回收`掉。

```java
java复制代码 LoadingCache<String,  User> loadingCache = Caffeine.newBuilder()
                .weakKeys()
                .build(key -> userDao.getUser(key));
```

小伙伴们应该都有个基本的认知，就是两个对象进行比较是否相等的时候，要使用`equals`方法而非`==`。而且很多时候我们会主动去覆写`hashCode`方法与`equals`方法来指定两个对象的相等判断逻辑。但是基于引用的数据淘汰策略，关注的是引用地址值而非实际内容值，也即一旦使用**weakKeys**指定了基于引用方式回收，那么查询的时候将只能是使用同一个key对象（内存地址相同）才能够查询到数据，因为这种情况下查询的时候，使用的是`==`判断是否为同一个key。

看下面的例子：

```java
public static void main(String[] args) {
    Cache<String, String> cache = Caffeine.newBuilder()
            .weakKeys()
            .build();
    String key1 = "123";
    cache.put(key1, "value1");
    System.out.println(cache.getIfPresent(key1));
    String key2 = new String("123");
    System.out.println("key1.equals(key2) ： " + key1.equals(key2));
    System.out.println("key1==key2 ： " + (key1==key2));
    System.out.println(cache.getIfPresent(key2));
}
```

执行之后，会发现使用存入时的key1进行查询的时候是可以查询到数据的，而使用key2去查询的时候并没有查询到记录，虽然key1与key2的值都是字符串123！

```csharp
csharp复制代码value1
key1.equals(key2) ： true
key1==key2 ： false
null
```

在实际使用的时候，这一点务必需要注意，对于新手而言，**很容易踩进坑里**。



#### weakValues

与weakKeys类似，我们可以在创建缓存对象的时候使用`weakValues`指定将value值以**弱引用**的方式存储到缓存中。这样当这条缓存记录的对象不再被引用依赖的时候，就会被JVM在适当的时候回收释放掉。

```java
LoadingCache<String,  User> loadingCache = Caffeine.newBuilder()
                .weakValues()
                .build(key -> userDao.getUser(key));
```

实际使用的时候需要注意`weakValues`**不支持**在`AsyncLoadingCache`中使用。比如下面的代码：

```java
public static void main(String[] args) {
    AsyncLoadingCache<String, User> cache = Caffeine.newBuilder()
            .weakValues()
            .buildAsync(key -> userDao.getUser(key));
}
```

启动运行的时候，就会报错：

```php
Exception in thread "main" java.lang.IllegalStateException: Weak or soft values cannot be combined with AsyncLoadingCache
	at com.github.benmanes.caffeine.cache.Caffeine.requireState(Caffeine.java:201)
	at com.github.benmanes.caffeine.cache.Caffeine.buildAsync(Caffeine.java:1192)
	at com.github.benmanes.caffeine.cache.Caffeine.buildAsync(Caffeine.java:1167)
	at com.veezean.skills.cache.caffeine.CaffeineCacheService.main(CaffeineCacheService.java:297)
```

当然咯，很多时候也可以将`weakKeys`与`weakValues`组合起来使用，这样可以获得到两种能力的综合加成。

```java
LoadingCache<String,  User> loadingCache = Caffeine.newBuilder()
                .weakKeys()
                .weakValues()
                .build(key -> userDao.getUser(key));
```



#### softValues

`softValues`是指将缓存内容值以**软引用**的方式存储在缓存容器中，当**内存容量满**的时候Caffeine会以`LRU`（least-recently-used，最近最少使用）顺序进行数据淘汰回收。对比下其与weakValues的差异：

| 方式       | 具体描述                                                     |
| ---------- | ------------------------------------------------------------ |
| weakValues | **弱引用**方式存储，一旦不再被引用，则会被GC回收             |
| softValues | **软引用**方式存储，不会被GC回收，但是在内存容量满的时候，会基于**LRU策略**数据回收 |

具体使用的时候，可以在创建缓存对象的时候进行指定基于软引用方式数据淘汰：

```java
LoadingCache<String,  User> loadingCache = Caffeine.newBuilder()
                .softValues()
                .build(key -> userDao.getUser(key));
```

与weakValues一样，需要注意`softValues`也**不支持**在`AsyncLoadingCache`中使用。此外，还需要注意`softValues`与`weakValues`两者也**不可以**一起使用。

```java
public static void main(String[] args) {
    LoadingCache<String, User> cache = Caffeine.newBuilder()
            .weakKeys()
            .weakValues()
            .softValues()
            .build(key -> userDao.getUser(key));
}
```

启动运行的时候，也会报错：

```java
Exception in thread "main" java.lang.IllegalStateException: Value strength was already set to WEAK
	at com.github.benmanes.caffeine.cache.Caffeine.requireState(Caffeine.java:201)
	at com.github.benmanes.caffeine.cache.Caffeine.softValues(Caffeine.java:572)
	at com.veezean.skills.cache.caffeine.CaffeineCacheService.main(CaffeineCacheService.java:297)
```



