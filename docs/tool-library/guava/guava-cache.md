---
title: 本地缓存 - GuavaCache
category: 工具类库
tag:
 - 缓存
 - GuavaCache
head:
  - - meta
    - name: keywords
      content: Guava,GuavaCache,本地缓存,GuavaCache缓存,淘汰策略,数据清理,并发能力
  - - meta
    - name: description
      content: 全网最全的Guava知识点总结，让天下没有难学的八股文！
---



> 来源：稀土掘金社区[深入理解缓存原理与实战设计](https://juejin.cn/column/7140852038258147358)，Seven进行了部分补充完善



## 介绍

本篇文章中，我们就来一起深入剖析JAVA本地缓存的优秀“**轮子**” —— 来自*Google*家族的`Guava Cache`。聊一聊其实现**机制**、看一看如何**使用**。

Guava 提供的 Cache 按照 **有则取值，无则计算**的逻辑，支持自动装载，自动移除等扩展功能，比传统的`ConcurrentHashMap`功能更加强大。

说在前面，Guava 提供的 Cache参考的是JDK 7的 `ConcurrentHashMap`，而Caffeine Cache参考的是JDK 8的 `ConcurrentHashMap`。

### 初识

**Guava**是Google提供的一套JAVA的工具包，而`Guava Cache`则是该工具包中提供的一套完善的**JVM级别**的高并发缓存框架。其实现机制类似*ConcurrentHashMap*，但是进行了众多的封装与能力扩展。作为JVM级别的本地缓存框架，`Guava Cache`具备缓存框架该有的众多基础特性。当然，Guava Cache能从众多本地缓存类产品中脱颖而出，除了具备上述基础缓存特性外，还有众多贴心的*能力增强*，绝对算得上是工具包届的**超级暖男**！为什么这么说呢？我们一起看下*Guava Cache*的能力介绍，应该可以有所体会。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406051811259.webp)

#### 支持缓存记录的过期设定

作为一个合格的缓存容器，支持缓存记录过期是一个基础能力。`Guava Cache`不但支持设定过期时间，还支持选择是根据`插入时间`进行过期处理（**创建过期**）、或者是根据最后`访问时间`进行过期处理（**访问过期**）。

| 过期策略 | 具体说明                                                     |
| -------- | ------------------------------------------------------------ |
| 创建过期 | 基于缓存记录的插入时间判断。比如设定10分钟过期，则记录加入缓存之后，*不管有没有访问*，10分钟时间到则过期 |
| 访问过期 | 基于最后一次的访问时间来判断是否过期。比如设定10分钟过期，如果缓存记录被访问到，则以最后一次访问时间重新计时；只有连续10分钟没有被访问的时候才会过期，否则将一直存在缓存中不会被过期。 |

实际使用的时候，可以在创建缓存容器的时候指定过期策略即可：

- 基于**创建时间**过期

```java
public Cache<String, User> createUserCache() {
    return CacheBuilder.newBuilder()
        .expireAfterWrite(30L, TimeUnit.MINUTES)
        .build();
}
```

- 基于**访问时间**过期

```java
public Cache<String, User> createUserCache() {
    return CacheBuilder.newBuilder()
        .expireAfterAccess(30L, TimeUnit.MINUTES)
        .build();
}
```



#### 支持缓存容量限制与不同淘汰策略

作为内存型缓存，必须要防止出现内存溢出的风险。Guava Cache支持设定缓存容器的最大存储上限，并支持根据缓存记录`条数`或者基于每条缓存记录的`权重`（后面会具体介绍）进行判断是否达到容量阈值。

当容量触达阈值后，支持根据`FIFO + LRU`策略实施具体淘汰处理以腾出位置给新的记录使用。

| 淘汰策略 | 具体说明                               |
| -------- | -------------------------------------- |
| FIFO     | 根据缓存记录写入的顺序，先写入的先淘汰 |
| LRU      | 根据访问顺序，淘汰最久没有访问的记录   |

实际使用的时候，同样是在创建缓存容器的时候指定容量上限与淘汰策略，这样就可以放心大胆的使用而不用担心内存溢出问题咯。

- 限制缓存**记录条数**

```java
public Cache<String, User> createUserCache() {
    return CacheBuilder.newBuilder()
            .maximumSize(10000L)
            .build();
}
```

- 限制缓存**记录权重**

```java
public Cache<String, User> createUserCache() {
    return CacheBuilder.newBuilder()
            .maximumWeight(10000L)
            .weigher((key, value) -> (int) Math.ceil(instrumentation.getObjectSize(value) / 1024L))
            .build();
    }
```

这里需要注意：按照权重进行限制缓存容量的时候必须要指定`weighter`属性才可以生效。上面代码中我们通过计算`value`对象的字节数（byte）来计算其权重信息，每1kb的字节数作为1个权重，整个缓存容器的总容量限制为1w，这样就可以实现将缓存内存占用控制在`10000*1k≈10M`左右。

有没有很省心？



#### 支持集成数据源能力

缓存有**三种模型**，分别是`旁路型`、`穿透型`、`异步型`。Guava Cache作为一个封装好的缓存框架，是一个典型的**穿透型缓存**。正常业务使用缓存时通常会使用旁路型缓存，即先去缓存中尝试查询获取数据，如果获取不到则会从数据库中进行查询并加入到缓存中；而为了简化业务端使用复杂度，Guava Cache支持集成数据源，业务层面调用接口查询缓存数据的时候，如果缓存数据不存在，则会*自动*去数据源中进行数据获取并加入缓存中。

```java
public User findUser(Cache<String, User> cache, String userId) {
    try {
        return cache.get(userId, () -> {
            System.out.println(userId + "用户缓存不存在，尝试回源查找并回填...");
            return userDao.getUser(userId);
        });
    } catch (ExecutionException e) {
        e.printStackTrace();
    }
    return null;
}
```

实际使用的时候如果查询的用户不存在，则会自动去回源查找并写入缓存里，再次获取的时候便可以从缓存直接获取：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052010554.webp)

上面的方法里，是通过在get方法里传入`Callable`实现的方式指定回源获取数据的方式，来实现缓存不存在情况的自动数据拉取与回填到缓存中的。实际使用的时候，除了Callable方式，还有一种`CacheLoader`的模式，也可以实现这一效果。

需要我们在创建缓存容器的时候声明容器为**LoadingCache**类型（下面的章节中有介绍），并且指定`CacheLoader`处理逻辑：

```java
public LoadingCache<String, User> createUserCache() {
    return CacheBuilder.newBuilder()
            .build(new CacheLoader<String, User>() {
                @Override
                public User load(String key) throws Exception {
                    System.out.println(key + "用户缓存不存在，尝试CacheLoader回源查找并回填...");
                    return userDao.getUser(key);
                }
            });
    }
```

这样，获取不到数据的时候，也会自动回源查询并填充。比如我们执行如下调用逻辑：

```java
public static void main(String[] args) {
        CacheService cacheService = new CacheService();
        LoadingCache<String, User> cache = cacheService.createUserCache();
        try {
            System.out.println(cache.get("123"));
            System.out.println(cache.get("124"));
            System.out.println(cache.get("123"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
```

执行结果如下：

```ini
123用户缓存不存在，尝试CacheLoader回源查找并回填...
User(userId=123, userName=铁柱, department=研发部)
124用户缓存不存在，尝试CacheLoader回源查找并回填...
User(userId=124, userName=翠花, department=测试部)
User(userId=123, userName=铁柱, department=研发部)
```

两种方式都可以实现这一效果，实际可以根据需要与场景选择合适的方式。



当然，有些时候，可能也会涉及到`CacheLoader`与`Callable`两种方式结合使用的场景，这种情况下**优先**会执行*Callable*提供的逻辑，Callable缺失的场景会使用*CacheLoader*提供的逻辑。

```java
public static void main(String[] args) {
    CacheService cacheService = new CacheService();
    LoadingCache<String, User> cache = cacheService.createUserCache();
    try {
        System.out.println(cache.get("123", () -> new User("xxx")));
        System.out.println(cache.get("124"));
        System.out.println(cache.get("123"));
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

执行后，可以看出Callable逻辑被*优先执行*，而CacheLoader作为*兜底策略*存在：

```sql
User(userId=xxx, userName=null, department=null)
124用户缓存不存在，尝试CacheLoader回源查找并回填...
User(userId=124, userName=翠花, department=测试部)
User(userId=xxx, userName=null, department=null)
```



#### 支持更新锁定能力

这个是与上面数据源集成一起的辅助增强能力。在高并发场景下，如果某个key值没有命中缓存，大量的请求同步打到下游模块处理的时候，很容易造成**缓存击穿**问题。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052020511.webp)

为了防止缓存击穿问题，可以通过**加锁**的方式来规避。当缓存不可用时，仅`持锁的线程`负责从数据库中查询数据并写入缓存中，其余请求重试时先尝试从缓存中获取数据，避免所有的并发请求全部同时打到数据库上。

作为穿透型缓存的保护策略之一，*Guava Cache*自带了`并发锁定`机制，同一时刻仅允许一个请求去回源获取数据并回填到缓存中，而其余请求则阻塞等待，不会造成数据源的压力过大。

有没有被暖心到？



#### 提供了缓存相关的一些监控统计

引入缓存的一个初衷是希望缓存能够提升系统的处理性能，而有限缓存容量中仅存储部分数据的时候，我们会希望存储的有限数据可以尽可能的覆盖并抗住大部分的请求流量，所以对缓存的**命中率**会非常关注。

Guava Cache深知这一点，所以提供了`stat`统计日志，支持查看缓存数据的*加载*或者*命中*情况统计。我们可以基于命中情况，不断的去优化代码中缓存的数据策略，以发挥出缓存的最大价值。

Guava Cache的统计信息封装为`CacheStats`对象进行承载，主要包含一下几个关键指标项：

| 指标               | 含义说明                                 |
| ------------------ | ---------------------------------------- |
| hitCount           | 命中缓存次数                             |
| missCount          | 没有命中缓存次数（查询的时候内存中没有） |
| loadSuccessCount   | 回源加载的时候加载成功次数               |
| loadExceptionCount | 回源加载但是加载失败的次数               |
| totalLoadTime      | 回源加载操作总耗时                       |
| evictionCount      | 删除记录的次数                           |

缓存容器创建的时候，可以通过`recordStats()`开启缓存行为的统计记录：

```java
public static void main(String[] args) {
        LoadingCache<String, User> cache = CacheBuilder.newBuilder()
                .recordStats()
                .build(new CacheLoader<String, User>() {
                    @Override
                    public User load(String key) throws Exception {
                        System.out.println(key + "用户缓存不存在，尝试CacheLoader回源查找并回填...");
                        User user = userDao.getUser(key);
                        if (user == null) {
                            System.out.println(key + "用户不存在");
                        }
                        return user;
                    }
                });

        try {
            System.out.println(cache.get("123");
            System.out.println(cache.get("124"));
            System.out.println(cache.get("123"));
            System.out.println(cache.get("126"));

        } catch (Exception e) {
        } finally {
            CacheStats stats = cache.stats();
            System.out.println(stats);
        }
    }
```

上述代码执行之后结果输出如下：

```ini
123用户缓存不存在，尝试CacheLoader回源查找并回填...
User(userId=123, userName=铁柱, department=研发部)
124用户缓存不存在，尝试CacheLoader回源查找并回填...
User(userId=124, userName=翠花, department=测试部)
User(userId=123, userName=铁柱, department=研发部)
126用户缓存不存在，尝试CacheLoader回源查找并回填...
126用户不存在
CacheStats{hitCount=1, missCount=3, loadSuccessCount=2, loadExceptionCount=1, totalLoadTime=1972799, evictionCount=0}
```

可以看出，一共执行了4次请求，其中1次命中，3次回源处理，2次回源加载成功，1次回源没找到数据，与打印出来的`CacheStats`统计结果完全吻合。

有着上述能力的加持，前面将Guava Cache称作“**暖男**”不过分吧？



### Guava Cache适用场景

作为一款纯粹的本地缓存框架，Guava Cache具备本地缓存该有的**优势**，也无可避免的存在着本地缓存的**弊端**。

| 维度 | 简要概述                                                     |
| ---- | ------------------------------------------------------------ |
| 优势 | 基于**空间换时间**的策略，利用内存的高速处理效率，提升机器的处理性能，减少大量对外的*IO请求*交互，比如读取DB、请求外部网络、读取本地磁盘数据等等操作。 |
| 弊端 | 整体**容量受限**，可能对本机内存造成压力。此外，对于分布式多节点集群部署的场景，缓存更新场景会出现**缓存漂移**问题，导致各个节点之间的缓存*数据不一致*。 |

鉴于上述优劣综合判断，可以大致圈定`Guava Cache`的实际适用场合：

- 数据**读多写少**且对**一致性要求不高**的场景

这类场景中，会将数据缓存到本地内存中，采用定时触发（或者事件推送）的策略重新加载到内存中。这样业务处理逻辑直接从内存读取需要的数据，修改系统配置项之后，需要等待一定的时间后方可生效。

很多的配置中心采用的都是这个缓存策略。**统一配置中心中管理配置数据**，然后各个业务节点会从统一配置中心拉取配置并存储在自己本地的内存中然后使用本地内存中的数据。这样可以有效规避配置中心的单点故障问题，降低了配置中心的请求压力，也提升了业务节点自身的业务处理性能（减少了与配置中心之间的网络交互请求）。



- 对**性能**要求**极其严苛**的场景

对于分布式系统而言，集中式缓存是一个常规场景中很好的选项。但是对于一些超大并发量且读性能要求严苛的系统而言，一个请求流程中需要频繁的去与Redis交互，其网络开销也是不可忍受的。所以可以采用将数据本机内存缓存的方式，分散redis的压力，降低对外请求交互的次数，提升接口响应速度。



- 简单的本地数据缓存，作为`HashMap/ConcurrentHashMap`的**替代品**

这种场景也很常见，我们在项目中经常会遇到一些数据的需要临时缓存一下，为了方便很多时候直接使用的`HashMap`或者`ConcurrentHashMap`来实现。而Guava Cache聚焦缓存场景做了很多额外的功能增强（比如数据过期能力支持、容量上限约束等），可以完美替换掉*HashMap/ConcurrentHashMap*，更适合缓存场景使用。



### Guava Cache使用

#### 引入依赖

使用Guava Cache，首先需要引入对应的依赖包。对于Maven项目，可以在`pom.xml`中添加对应的依赖声明即可：

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>31.1-jre</version>
</dependency>
```



#### 容器创建CacheBuilder

具体使用前首先面临的就是如何创建Guava Cache实例。可以借助`CacheBuilder`以一种优雅的方式来构建出合乎我们诉求的Cache实例。

对*CacheBuilder*中常见的属性方法，归纳说明如下：

| 方法                | 含义说明                                                     |
| ----------------- | -------------------------------------------------------- |
| newBuilder        | 构造出一个Builder实例类                                          |
| initialCapacity   | 待创建的缓存容器的初始容量大小（记录**条数**）                                |
| maximumSize       | 指定此缓存容器的最大容量(最大缓存记录**条数**)                               |
| maximumWeight     | 指定此缓存容器的最大容量（最大**比重**值），需结合`weighter`方可体现出效果             |
| expireAfterWrite  | 设定过期策略，按照数据**写入时间**进行计算                                  |
| expireAfterAccess | 设定过期策略，按照数据最后**访问时间**来计算                                 |
| weighter          | 入参为一个函数式接口，用于指定每条存入的缓存数据的权重占比情况。这个需要与`maximumWeight`结合使用 |
| refreshAfterWrite | 缓存写入到缓存之后进行定时自动刷新                                        |
| concurrencyLevel  | 用于控制缓存的并发处理能力，同时支持多少个线程**并发写入**操作                        |
| recordStats       | 设定开启此容器的数据加载与缓存命中情况统计                                    |

基于`CacheBuilder`及其提供的各种方法，我们可以轻松的进行缓存容器的构建、并指定容器的各种约束条件。

比如下面这样：

```java
public LoadingCache<String, User> createUserCache() {
    return CacheBuilder.newBuilder()
            .initialCapacity(1000) // 初始容量
            .maximumSize(10000L)   // 设定最大容量
            .expireAfterWrite(30L, TimeUnit.MINUTES) // 设定写入过期时间
            .concurrencyLevel(8)  // 设置最大并发写操作线程数
            .refreshAfterWrite(1L, TimeUnit.MINUTES) // 设定自动刷新数据时间
            .recordStats() // 开启缓存执行情况统计
            .build(new CacheLoader<String, User>() {
                @Override
                public User load(String key) throws Exception {
                    return userDao.getUser(key);
                }
            });
}
```



#### 业务层使用

Guava Cache容器对象创建完成后，可以基于其提供的对外接口完成相关缓存的具体操作。首先可以了解下Cache提供的对外操作接口：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052028814.webp)

对关键接口的含义梳理归纳如下：

| 接口名称      | 具体说明                                                     |
| ------------- | ------------------------------------------------------------ |
| get           | 查询指定key对应的value值，如果缓存中没匹配，则基于给定的`Callable`逻辑去获取数据回填缓存中并返回 |
| getIfPresent  | 如果缓存中存在指定的key值，则返回对应的value值，否则返回null（此方法**不会触发**自动回源与回填操作） |
| getAllPresent | 针对传入的key列表，返回缓存中存在的对应value值列表（**不会触发**自动回源与回填操作） |
| put           | 往缓存中添加key-value键值对                                  |
| putAll        | 批量往缓存中添加key-value键值对                              |
| invalidate    | 从缓存中删除指定的记录                                       |
| invalidateAll | 从缓存中批量删除指定记录，如果无参数，则清空所有缓存         |
| size          | 获取缓存容器中的总记录数                                     |
| stats         | 获取缓存容器当前的统计数据                                   |
| asMap         | 将缓存中的数据转换为`ConcurrentHashMap`格式返回              |
| cleanUp       | 清理所有的已过期的数据                                       |

在项目中，可以基于上述接口，实现各种缓存操作功能。

```java
public static void main(String[] args) {
    CacheService cacheService = new CacheService();
    LoadingCache<String, User> cache = cacheService.createUserCache6();
    cache.put("122", new User("122"));
    cache.put("122", new User("122"));
    System.out.println("put操作后查询：" + cache.getIfPresent("122"));
    cache.invalidate("122");
    System.out.println("invalidate操作后查询：" + cache.getIfPresent("122"));
    System.out.println(cache.stats());
}
```

执行后，结果如下：

```ini
put操作后查询：User(userId=122, userName=null, department=null)
invalidate操作后查询：null
CacheStats{hitCount=1, missCount=1, loadSuccessCount=0, loadExceptionCount=0, totalLoadTime=0, evictionCount=0}
```

当然，上述示例代码中这种使用方式有个明显的弊端就是业务层面对Guava Cache的`私有API`**依赖过深**，后续如果需要替换Cache组件的时候会比较痛苦，需要对业务调用的地方进行大改。所以真正项目里面，最好还是对其适当封装，以实现业务层面的**解耦**。如果你的项目是使用Spring框架，也可以基于`Spring Cache`统一规范来集成并使用Guava Cache，降低对业务逻辑的**侵入**。



## 容量限制策略

### 弄清Size与Weight

Guava Cache提供了对**缓存总量**的限制，并且支持从两个维度进行限制，这里我们首先要理清`size`与`weight`两个概念的区别与联系。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052032597.webp)

- **限制缓存条数size**

```java
public Cache<String, User> createUserCache() {
    return CacheBuilder.newBuilder().maximumSize(10000L).build();
}
```

- **限制缓存权重weight**

```java
public Cache<String, String> createUserCache() {
    return CacheBuilder.newBuilder()
            .maximumWeight(50000)
            .weigher((key, value) -> (int) Math.ceil(value.length() / 1000))
            .build();
    }
```

一般而言，限制容器的容量的**初衷**，是为了防止内存占用过大导致`内存溢出`，所以本质上是限制*内存的占用量*。从实现层面，往往会根据总内存占用量与预估每条记录字节数进行估算，将其转换为对缓存记录条数的限制。这种做法相对简单易懂，但是对于单条缓存记录占用字节数差异较大的情况下，会导致基于条数控制的结果**不够精准**。



比如：

> 需要限制缓存最大占用`500M`总量，缓存记录可能大小范围是1k~100k，按照每条`50k`进行估算，设定缓存容器最大容量为限制最大容量`1w`条。如果存储的都是1k大小的记录，则内存总占用量才10M（内存没有被有效利用起来）；若都存储的是100k大小的记录，又会导致内存占用为1000M，**远大于**预期的内存占用量（容易造成内存溢出）。

为了解决这个问题，Guava Cache中提供了一种**相对精准**的控制策略，即**基于权重**的总量控制，根据一定的规则，计算出每条value记录所占的权重值，然后以权重值进行总量的计算。



还是上面的例子，我们按照权重进行设定，假定1k对应基础权重1，则100k可转换为权重100。这样一来：

> 限制缓存最大占用`500M`，`1k`对应权重1,`Nk`代表权重N，则我们可以限制总权重为`50w`。这样假如存储的都是1k的记录，则最多可以缓存5w条记录；而如果都是100k大小的记录，则最多仅可以缓存5000条记录。根据存储数据的大小不同，最大存储的记录条数也不相同，但是最终占用的总体量可以实现基本吻合。

所以，基于`weight`权重的控制方式，比较适用于这种对容器体量控制**精度**有**严格诉求**的场景，可以在创建容器的时候指定每条记录的权重计算策略（比如基于字符串长度或者基于bytes数组长度进行计算权重）。



### 使用约束说明

在实际使用中，这几个参数之间有一定的使用约束，需要特别注意一下：

- 如果*没有指定*weight实现逻辑，则使用`maximumSize`来限制最大容量，按照容器中缓存记录的条数进行限制；这种情况下，即使设定了maximumWeight也不会生效。
- 如果*指定*了weight实现逻辑，则**必须使用** `maximumWeight` 来限制最大容量，按照容器中每条缓存记录的weight值累加后的总weight值进行限制。

看下面的一个反面示例，指定了weighter和maximumSize，却**没有指定** maximumWeight属性：

```java
public static void main(String[] args) {
    try {
        Cache<String, String> cache = CacheBuilder.newBuilder()
            .weigher((key, value) -> 2)
            .maximumSize(2)
            .build();
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        System.out.println(cache.size());
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

执行的时候，会报错，提示weighter和maximumSize不可以混合使用：

```css
java.lang.IllegalStateException: maximum size can not be combined with weigher
	at com.google.common.base.Preconditions.checkState(Preconditions.java:502)
	at com.google.common.cache.CacheBuilder.maximumSize(CacheBuilder.java:484)
	at com.veezean.skills.cache.guava.CacheService.main(CacheService.java:205)
```



## 淘汰策略

为了简单描述，我们将数据从缓存容器中移除的操作 统称数据淘汰。按照触发形态不同，我们可以将数据的清理与淘汰策略分为**被动淘汰**与**主动淘汰**两种。

### 被动淘汰

- **基于数据量（size或者weight）**

当容器内的缓存数量接近（注意是接近、而非达到）设定的最大阈值的时候，会触发guava cache的数据清理机制，会基于LRU或FIFO删除一些不常用的key-value键值对。这种方式需要在创建容器的时候指定其`maximumSize`或者`maximumWeight`，然后才会基于size或者weight进行判断并执行上述的清理操作。

看下面的实验代码：

```java
public static void main(String[] args) {
    try {
        Cache<String, String> cache = CacheBuilder.newBuilder()
                .maximumSize(2)
                .removalListener(notification -> {
                    System.out.println("---监听到缓存移除事件：" + notification);
                })
                .build();
        System.out.println("put放入key1");
        cache.put("key1", "value1");
        System.out.println("put放入key2");
        cache.put("key2", "value1");
        System.out.println("put放入key3");
        cache.put("key3", "value1");
        System.out.println("put操作后，当前缓存记录数：" + cache.size());
        System.out.println("查询key1对应值：" + cache.getIfPresent("key1"));
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

上面代码中，没有设置数据的过期时间，理论上数据是长期有效、不会被过期删除。为了便于测试，我们设定缓存最大容量为2条记录，然后往缓存容器中插入3条记录，观察下输出结果如下：

```text
put放入key1
put放入key2
put放入key3
---监听到缓存移除事件：key1=value1
put操作后，当前缓存记录数：2
查询key1对应值：null
```

从输出结果可以看到，即使*数据并没有过期*，但在插入第3条记录的时候，缓存容器还是自动将最初写入的key1记录给移除了，挪出了空间用于新的数据的插入。这个就是因为触发了Guava Cache的被动淘汰机制，以**确保**缓存容器中的数据量始终是在**可控范围**内。



- **基于过期时间**

Guava Cache支持根据`创建时间`或者根据`访问时间`来设定数据过期处理，实际使用的时候可以根据具体需要来选择对应的方式。

| 过期策略 | 具体说明                                                                                          |
| ---- | --------------------------------------------------------------------------------------------- |
| 创建过期 | 基于缓存记录的插入时间判断。比如设定10分钟过期，则记录加入缓存之后，*不管有没有访问*，10分钟时间到则过期                                       |
| 访问过期 | 基于最后一次的访问时间来判断是否过期。比如设定10分钟过期，如果缓存记录被访问到，则以最后一次访问时间重新计时；只有连续10分钟没有被访问的时候才会过期，否则将一直存在缓存中不会被过期。 |

看下面的实验代码：

```java
public static void main(String[] args) {
    try {
        Cache<String, String> cache = CacheBuilder.newBuilder()
                .expireAfterWrite(1L, TimeUnit.SECONDS)
                .recordStats()
                .build();
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        System.out.println("put操作后，当前缓存记录数：" + cache.size());
        System.out.println("查询key1对应值：" + cache.getIfPresent("key1"));
        System.out.println("统计信息：" + cache.stats());
        System.out.println("-------sleep 等待超过过期时间-------");
        Thread.sleep(1100L);
        System.out.println("执行key1查询操作：" + cache.getIfPresent("key1"));
        System.out.println("当前缓存记录数：" + cache.size());
        System.out.println("当前统计信息：" + cache.stats());
        System.out.println("剩余数据信息：" + cache.asMap());
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

在实验代码中，我们设置了缓存记录1s有效期，然后等待其过期之后查看其缓存中数据情况，代码执行结果如下：

```ini
put操作后，当前缓存记录数：3
查询key1对应值：value1
统计信息：CacheStats{hitCount=1, missCount=0, loadSuccessCount=0, loadExceptionCount=0, totalLoadTime=0, evictionCount=0}
-------sleep 等待超过过期时间-------
执行key1查询操作：null
当前缓存记录数：1
当前统计信息：CacheStats{hitCount=1, missCount=1, loadSuccessCount=0, loadExceptionCount=0, totalLoadTime=0, evictionCount=2}
剩余数据信息：{}
```

从结果中可以看出，超过过期时间之后，再次执行`get`操作已经获取不到已过期的记录，相关记录也被从缓存容器中移除了。请注意，上述代码中我们特地是在过期之后执行了一次`get`请求然后才去查看缓存容器中存留记录数量与统计信息的，主要是因为Guava Cache的过期数据淘汰是一种**被动触发**技能。

当然，细心的小伙伴可能会发现上面的执行结果有一个“问题”，就是前面一起`put`写入了3条记录，等到超过过期时间之后，只移除了2条过期数据，还剩了一条记录在里面？但是去获取剩余缓存里面的数据的时候又显示缓存里面是空的？

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052043569.webp)

Guava Cache作为一款优秀的本地缓存工具包，是不可能有这么个大的bug遗留在里面的，那是什么原因呢？

这个现象其实与Guava Cache的缓存淘汰实现机制有关系，前面说过Guava Cache的过期数据清理是一种被动触发技能，我们看下`getIfPresent`方法对应的实现源码，可以很明显的看出每次get请求的时候都会触发一次`cleanUp`操作：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052043586.webp)

为了实现高效的多线程并发控制，Guava Cache采用了类似ConcurrentHashMap一样的`分段锁`机制，数据被分为了不同分片，每个分片同一时间只允许有一个线程执行写操作，这样降低并发锁争夺的竞争压力。而上面代码中也可以看出，执行清理的时候，仅针对当前查询的记录所在的`Segment`分片执行清理操作，而其余的分片的过期数据**并不会**触发清理逻辑 —— 这个也就是为什么前面例子中，明明3条数据都过期了，却只清理掉了其中的2条的原因。



为了验证上述的原因说明，我们可以在创建缓存容器的时候将`concurrencyLevel`设置为允许并发数为1，强制所有的数据都存放在同一个分片中：

```java
public static void main(String[] args) {
    try {
        Cache<String, String> cache = CacheBuilder.newBuilder()
                .expireAfterWrite(1L, TimeUnit.SECONDS)
                .concurrencyLevel(1)  // 添加这一约束，强制所有数据放在一个分片中
                .recordStats()
                .build();

                // ...省略其余逻辑，与上一段代码相同

    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

重新运行后，从结果可以看出，这一次3条过期记录全部被清除了。

```ini
put操作后，当前缓存记录数：3
查询key1对应值：value1
统计信息：CacheStats{hitCount=1, missCount=0, loadSuccessCount=0, loadExceptionCount=0, totalLoadTime=0, evictionCount=0}
-------sleep 等待超过过期时间-------
执行key1查询操作：null
当前缓存记录数：0
当前统计信息：CacheStats{hitCount=1, missCount=1, loadSuccessCount=0, loadExceptionCount=0, totalLoadTime=0, evictionCount=3}
剩余数据信息：{}
```

在实际的使用中，我们倒也无需过于关注数据过期是否有被从内存中真实移除这一点，因为Guava Cache会在保证业务数据准确的情况下，尽可能的兼顾处理性能，在该清理的时候，自会去执行对应的清理操作，所以也无需过于担心。



- **基于引用**

基于引用回收的策略，核心是利用`JVM`虚拟机的**GC机制**来达到数据清理的目的。按照JVM的GC原理，当一个对象不再被引用之后，便会执行一系列的标记清除逻辑，并最终将其回收释放。这种实际使用的较少，此处不多展开。



### 主动淘汰

上述通过总体容量限制或者通过过期时间约束来执行的缓存数据清理操作，是属于一种**被动触发**的机制。

实际使用的时候也会有很多情况，我们需要从缓存中立即将指定的记录给删除掉。比如执行删除或者更新操作的时候我们就需要删除已有的历史缓存记录，这种情况下我们就需要**主动调用** Guava Cache提供的相关删除操作接口，来达到对应诉求。

| 接口名称            | 含义描述           |
| ------------------- | ------------------ |
| invalidate(key)     | 删除指定的记录     |
| invalidateAll(keys) | 批量删除给定的记录 |
| invalidateAll()     | 清空整个缓存容器   |



## 数据回源与回填策略

前面我们介绍过，Guava Cache提供的是一种**穿透型缓存**模式，当缓存中没有获取到对应记录的时候，支持自动去源端获取数据并回填到缓存中。这里**回源**获取数据的策略有两种，即`CacheLoader`方式与`Callable`方式，两种方式适用于不同的场景，实际使用中可以按需选择。

下面一起看下这两种方式。

### CacheLoader

`CacheLoader`适用于数据加载方式比较固定且统一的场景，在缓存容器创建的时候就需要指定此具体的加载逻辑。常见的使用方式如下：

```java
public LoadingCache<String, User> createUserCache() {
    return CacheBuilder.newBuilder()
            .build(new CacheLoader<String, User>() {
                @Override
                public User load(String key) throws Exception {
                    System.out.println(key + "用户缓存不存在，尝试CacheLoader回源查找并回填...");
                    return userDao.getUser(key);
                }
            });
    }
```

上述代码中，在使用*CacheBuilder*创建缓存容器的时候，如果在`build()`方法中传入一个**CacheLoader**实现类的方式，则最终创建出来的是一个`LoadingCache`具体类型的Cache容器：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052052843.webp)

默认情况下，我们需要继承CacheLoader类并实现其`load`抽象方法即可。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052052814.webp)

当然，`CacheLoader`类中还有一些其它的方法，我们也可以选择性的进行覆写来实现自己的自定义诉求。比如我们需要设定`refreshAfterWrite`来支持**定时刷新**的时候，就推荐覆写`reload`方法，提供一个**异步**数据加载能力，避免数据刷新操作对业务请求造成阻塞。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052052823.webp)



另外，有一点需要注意下，如果创建缓存的时候使用`refreshAfterWrite`指定了需要定时更新缓存数据内容，则必须在创建的时候指定CacheLoader实例，否则执行的时候会**报错**。因为在执行`refresh`操作的时候，必须调用CacheLoader对象的`reload`方法去执行数据的回源操作。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052052834.webp)



### Callable

与CacheLoader不同，`Callable`的方式在每次数据获取请求中进行指定，可以在不同的调用场景中，分别指定并使用不同的数据获取策略，更加的**灵活**。

```java
public static void main(String[] args) {
    try {
        GuavaCacheService cacheService = new GuavaCacheService();
        Cache<String, User> cache = cacheService.createCache();
        String userId = "123";
        // 获取userId， 获取不到的时候执行Callable进行回源
        User user = cache.get(userId, () -> cacheService.queryUserFromDb(userId));
        System.out.println("get对应用户信息：" + user);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

通过提供`Callable`实现函数并作为参数传递的方式，可以根据业务的需要，在不同业务调用场景下，指定使用不同的Callable回源策略。

但我认为，这种使用方式还是比较少的，因为我们一般会遵循单一职责原则，尽量减少耦合。对于不同业务场景，那就应该用不用cache实例，而不是都耦合在一个cache实例里

### 不回源查询

前面介绍了基于`CacheLoader`方式自动回源，或者基于`Callable`方式显式回源的两种策略。但是实际使用缓存的时候，并非是缓存中获取不到数据时就一定需要去执行回源操作。

比如下面这个场景：

> 用户论坛中维护了个黑名单列表，每次用户登录的时候，需要先判断下是否在黑名单中，如果在则禁止登录。

因为论坛中黑名单用户占整体用户量的比重是极少的，也就是几乎绝大部分登录的时候去查询缓存都是无法命中黑名单缓存的。这种时候如果每次查询缓存中没有的情况下都去执行回源操作，那几乎等同于每次请求都需要去访问一次DB了，这显然是**不合理**的。



所以，为了支持这种场景的访问，Guava cache也提供了一种**不会触发回源**操作的访问方式。如下：

| 接口          | 功能说明                                                     |
| ------------- | ------------------------------------------------------------ |
| getIfPresent  | 从内存中查询，如果存在则返回对应值，不存在则返回null         |
| getAllPresent | 批量从内存中查询，如果存在则返回存在的键值对，不存在的key则不出现在结果集里 |



上述两种接口，执行的时候仅会从当前内存中已有的缓存数据里面进行查询，不会触发回源的操作。

```java
public static void main(String[] args) {
    try {
        GuavaCacheService cacheService = new GuavaCacheService();
        Cache<String, User> cache = cacheService.createCache();
        cache.put("123", new User("123", "123"));
        cache.put("124", new User("124", "124"));
        System.out.println(cache.getIfPresent("125"));
        ImmutableMap<String, User> allPresentUsers =
                cache.getAllPresent(Stream.of("123", "124", "125").collect(Collectors.toList()));
        System.out.println(allPresentUsers);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

执行后，输入结果如下：

```scss
null
{123=User(userName=123, userId=123), 124=User(userName=124, userId=124)}
```



## 数据清理与加载刷新机制

在前面的CacheBuilder类中有提供了几种`expire`与`refresh`的方法，即`expireAfterAccess`、`expireAfterWrite`以及`refreshAfterWrite`。

这里我们对几个方法进行一些探讨。

### 数据过期

对于数据有过期时效诉求的场景，我们可以通过几种方式设定缓存的过期时间：

- expireAfterAccess
- expireAfterWrite

现在我们思考一个问题：数据过期之后，会立即被删除吗？在前面的文章中，我们自己构建本地缓存框架的时候，有介绍过缓存数据删除的几种机制：

| 删除机制 | 具体说明                                                     |
| -------- | ------------------------------------------------------------ |
| 主动删除 | 搞个定时线程不停的去扫描并清理所有已经过期的数据。           |
| 惰性删除 | 在数据访问的时候进行判断，如果过期则删除此数据。             |
| 两者结合 | 采用惰性删除为主，低频定时主动删除为兜底，兼顾处理性能与内存占用。 |

在Guava Cache中，为了最大限度的保证并发性，采用的是**惰性删除**的策略，而没有设计独立清理线程。所以这里我们就可以回答前面的问题，也即**过期的数据，并非是立即被删除的**，而是在`get`等操作访问缓存记录时触发过期数据的删除操作。

在get执行逻辑中进行数据过期清理以及重新回源加载的执行判断流程，可以简化为下图中的关键环节：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052057718.webp)

在执行get请求的时候，会先判断下当前查询的数据是否过期，如果已经过期，则会触发对当前操作的`Segment`的过期数据清理操作。



### 数据刷新

除了上述的2个过期时间设定方法，Guava Cache还提供了个`refreshAfterWrite`方法，用于设定定时自动`refresh`操作。项目中可能会有这么个情况：

> 为了提升性能，将最近访问系统的用户信息缓存起来，设定有效期30分钟。如果用户的角色出现变更，或者用户昵称、个性签名之类的发生更改，则需要最长等待30分钟缓存失效重新加载后才能够生效。

这种情况下，我们就可以在设定了过期时间的基础上，再设定一个每隔1分钟重新`refresh`的逻辑。这样既可以保证数据在缓存中的留存时长，又可以尽可能的缩短缓存变更生效的时间。这种情况，便该`refreshAfterWrite`登场了。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052057632.webp)

与expire清理逻辑相同，refresh操作依旧是采用一种**被动触发**的方式来实现。当get操作执行的时候会判断下如果创建时间已经超过了设定的刷新间隔，则会重新去执行一次数据的加载逻辑（前提是数据并没有过期）。

鉴于缓存**读多写少**的特点，Guava Cache在数据refresh操作执行的时候，采用了一种**非阻塞式**的加载逻辑，尽可能的保证并发场景下对读取线程的性能影响。

看下面的代码，模拟了两个并发请求同时请求一个需要刷新的数据的场景：

```java
public static void main(String[] args) {
    try {
        LoadingCache<String, User> cache =
                CacheBuilder.newBuilder().refreshAfterWrite(1L, TimeUnit.SECONDS).build(new MyCacheLoader());
        cache.put("123", new User("123", "ertyu"));
        Thread.sleep(1100L);
        Runnable task = () -> {
            try {
                System.out.println(Thread.currentThread().getId() + "线程开始执行查询操作");
                User user = cache.get("123");
                System.out.println(Thread.currentThread().getId() + "线程查询结果：" + user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        CompletableFuture.allOf(
                CompletableFuture.runAsync(task), CompletableFuture.runAsync(task)
        ).thenRunAsync(task).join();
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

执行后，结果如下：

```scss
14线程开始执行查询操作
13线程开始执行查询操作
13线程查询结果：User(userName=ertyu, userId=123)
14线程执行CacheLoader.reload()，oldValue=User(userName=ertyu, userId=123)
14线程执行CacheLoader.load()...
14线程执行CacheLoader.load()结束...
14线程查询结果：User(userName=97qx6, userId=123)
15线程开始执行查询操作
15线程查询结果：User(userName=97qx6, userId=123)
```

从执行结果可以看出，两个并发同时请求的线程只有1个执行了`load`数据操作，且两个线程所获取到的结果是不一样的。具体而言，可以概括为如下几点：

- 同一时刻仅允许一个线程执行数据重新加载操作，并**阻塞等待**重新加载完成之后该线程的查询请求才会返回对应的新值作为结果。
- 当一个线程正在阻塞执行`refresh`数据刷新操作的时候，其它线程此时来执行get请求的时候，会判断下数据需要refresh操作，但是因为没有获取到refresh执行锁，这些其它线程的请求**不会被阻塞**等待refresh完成，而是**立刻返回**当前refresh前的**旧值**。
- 当执行refresh的线程操作完成后，此时另一个线程再去执行get请求的时候，会判断无需refresh，直接返回当前内存中的当前值即可。

上述的过程，按照时间轴的维度来看，可以囊括成如下的执行过程：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052057114.webp)



### 数据expire与refresh关系

`expire`与`refresh`在某些实现逻辑上有一定的相似度，很多的文章中介绍的时候甚至很直白的说refresh比expire更好，因为其不会阻塞业务端的请求。个人认为这种看法有点片面，从单纯的字面含义上也可以看出这两种机制不是互斥的、而是一种**相互补充**的存在，并不存在谁比谁更好这一说，关键要看具体是应用场景。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052057842.webp)

`expire`操作就是采用的一种**严苛**的更新锁定机制，当一个线程执行数据重新加载的时候，其余的线程则阻塞等待。`refresh`操作执行过程中不会阻塞其余线程的get查询操作，会直接返回旧值。这样的设计**各有利弊**：

| 操作    | 优势                                                         | 弊端                                                         |
| ------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| expire  | 有效防止缓存击穿问题，且阻塞等待的方式可以保证业务层面获取到的缓存数据的强一致性。 | 高并发场景下，如果回源的耗时较长，会导致多个读线程被阻塞等待，影响整体的并发效率。 |
| refresh | 可以最大限度的保证查询操作的执行效率，避免过多的线程被阻塞等待。 | 多个线程并发请求同一个key对应的缓存值拿到的结果可能不一致，在对于一致性要求特别严苛的业务场景下可能会引发问题。 |

Guava Cache中的expire与fefresh两种机制，给人的另一个**困惑点**在于：两者都是被动触发的数据加载逻辑，不管是expire还是refresh，只要超过指定的时间间隔，其实都是依旧存在与内存中，等有新的请求查询的时候，都会执行自动的最新数据加载操作。那这两个实际使用而言，仅仅只需要依据是否需要阻塞加载这个维度来抉择？

并非如此。



expire存在的意义更多的是一种**数据生命周期终结**的意味，超过了expire有效期的数据，虽然依旧会存在于内存中，但是在一些触发了`cleanUp`操作的情况下，是会被释放掉以减少内存占用的。而refresh则仅仅只是执行数据更新，框架无法判断是否需要从内存释放掉，会始终占据内存。

所以在具体使用时，需要根据场景综合判断：

- 数据需要**永久存储**，且**不会变更**，这种情况下`expire`和`refresh`都并不需要设定
- 数据**极少变更**，或者对变更的感知诉求不强，且并发请求同一个key的竞争压力不大，直接使用`expire`即可
- 数据**无需过期**，但是可能**会被修改**，需要及时感知并更新缓存数据，直接使用`refresh`
- 数据**需要过期**（避免不再使用的数据始终留在内存中）、也需要在有效期内尽可能保证数据的**更新一致性**，则采用`expire`与`refresh`两者**结合**。


> 对于expire与refresh结合使用的场景，两者的时间间隔设置，需要注意：expire时间设定要**大于**refresh时间，否则的话refresh将永远没有机会执行



## 并发能力支持

### 采用分段锁降低锁争夺

前面我们提过Guava Cache支持多线程环境下的安全访问。我们知道锁的粒度越大，多线程请求的时候对锁的竞争压力越大，对性能的影响越大。而如果将锁的粒度拆分小一些，这样**同时请求到同一把锁的概率就会降低**，这样线程间争夺锁的竞争压力就会降低。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052057867.webp)

Guava Cache中采用的也就是这种**分段锁**策略来降低锁的粒度，可以在创建缓存容器的时候使用`concurrencyLevel`来指定允许的**最大并发线程数**，使得线程安全的前提下尽可能的减少锁争夺。而*concurrencyLevel*值与分段*Segment*的数量之间也存在一定的关系，这个关系相对来说会比较复杂、且受是否限制总容量等因素的影响，源码中这部分的计算逻辑可以看下：

```java
    int segmentShift = 0;
    int segmentCount = 1;
    while (segmentCount < concurrencyLevel && (!evictsBySize() || segmentCount * 20 <= maxWeight)) {
        ++segmentShift;
        segmentCount <<= 1;
    }
```

根据上述的控制逻辑代码，可以将`segmentCount`的取值约束概括为下面几点：

- segmentCount 是 2 的整数倍
- segmentCount 最大可能为`(concurrencyLevel -1)*2`
- 如果有按照权重设置容量，则segmentCount不得超过总权重值的`1/20`

从源码中可以比较清晰的看出这一点，Guava Cache在put写操作的时候，会首先计算出key对应的hash值，然后根据hash值来确定数据应该写入到哪个Segment中，进而对该Segment加锁执行写入操作。

```java
@Override
public V put(K key, V value) {
    // ... 省略部分逻辑
    int hash = hash(key);
    return segmentFor(hash).put(key, hash, value, false);
}
@Nullable
V put(K key, int hash, V value, boolean onlyIfAbsent) {
  lock();
    try {
        // ... 省略具体逻辑
    } finally {
        unlock();
        postWriteCleanup();
    }
}
```

根据上述源码也可以得出一个结论，`concurrencyLevel`只是一个**理想状态**下的最大同时并发数，也取决于同一时间的操作请求是否会平均的分散在各个不同的Segment中。极端情况下，如果多个线程操作的目标对象都在同一个分片中时，那么只有1个线程可以持锁执行，其余线程都会阻塞等待。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052057828.webp)

实际使用中，比较推荐的是将concurrencyLevel设置为**CPU核数的2倍**，以获得较优的并发性能。当然，concurrencyLevel也不是可以随意设置的，从其源码设置里面可以看出，允许的最大值为`65536`。

```java
static final int MAX_SEGMENTS = 1 << 16; // 65536
LocalCache(CacheBuilder<? super K, ? super V> builder, @Nullable CacheLoader<? super K, V> loader) {
    concurrencyLevel = Math.min(builder.getConcurrencyLevel(), MAX_SEGMENTS);
    // ... 省略其余逻辑
}
```



### 佛系抢锁策略

在put等写操作场景下，Guava Cache采用的是上述分段锁的策略，通过优化锁的粒度，来提升并发的性能。但是加锁毕竟还是对性能有一定的影响的，为了追求更加极致的性能表现，在get等读操作自身并没有发现加锁操作 —— 但是Guava Cache的get等处理逻辑也并非是纯粹的只读操作，它还兼具触发数据淘汰清理操作的删除逻辑，所以只在判断需要执行清理的时候才会尝试去**佛系抢锁**。

那么它是如何减少抢锁的几率的呢？从源码中可以看出，并非是每次请求都会去触发`cleanUp`操作，而是会尝试积攒一定次数之后再去尝试清理：

```java
static final int DRAIN_THRESHOLD = 0x3F;
void postReadCleanup() {
  if ((readCount.incrementAndGet() & DRAIN_THRESHOLD) == 0) {
    cleanUp();
  }
}
```

在高并发场景下，如果查询请求量巨大的情况下，即使按照上述的情况限制每次达到一定请求数量之后才去执行清理操作，依旧可能会出现多个get操作线程同时去抢锁执行清理操作的情况，这样岂不是依旧会阻塞这些读取请求的处理吗？

继续看下源码：

```java
void cleanUp() {
  long now = map.ticker.read();
  runLockedCleanup(now);
  runUnlockedCleanup();
}
void runLockedCleanup(long now) {
    // 尝试请求锁，请求到就处理，请求不到就放弃
  if (tryLock()) {
    try {
      // ... 省略部分逻辑
      readCount.set(0);
    } finally {
      unlock();
    }
  }
}
```

可以看到源码中采用的是`tryLock`方法来尝试去抢锁，如果抢到锁就继续后续的操作，如果没抢到锁就不做任何清理操作，直接返回 —— 这也是为什么前面将其形容为“**佛系抢锁**”的缘由。这样的小细节中也可以看出Google码农们还是有点内功修为的。



## 承前启后 —— Caffeine Cache

技术的更新迭代始终没有停歇的时候，Guava工具包作为Google家族的优秀成员，在很多方面提供了非常优秀的能力支持。随着JAVA8的普及，Google也基于语言的新特性，对Guava Cache部分进行了重新实现，形成了后来的`Caffeine Cache`，并在SpringBoot2.x中取代了Guava Cache。



<!-- @include: @article-footer.snippet.md -->     