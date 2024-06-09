---
title: GuavaCache - 介绍
category: 系统设计
tag:
 - 缓存
 - GuavaCache
---



> 来源：稀土掘金社区[深入理解缓存原理与实战设计](https://juejin.cn/column/7140852038258147358)，Seven进行了部分补充完善



本篇文章中，我们就来一起深入剖析JAVA本地缓存的优秀“**轮子**” —— 来自*Google*家族的`Guava Cache`。聊一聊其实现**机制**、看一看如何**使用**。



## Guava Cache初识

**Guava**是Google提供的一套JAVA的工具包，而`Guava Cache`则是该工具包中提供的一套完善的**JVM级别**的高并发缓存框架。其实现机制类似*ConcurrentHashMap*，但是进行了众多的封装与能力扩展。作为JVM级别的本地缓存框架，`Guava Cache`具备缓存框架该有的众多基础特性。当然，Guava Cache能从众多本地缓存类产品中脱颖而出，除了具备上述基础缓存特性外，还有众多贴心的*能力增强*，绝对算得上是工具包届的**超级暖男**！为什么这么说呢？我们一起看下*Guava Cache*的能力介绍，应该可以有所体会。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406051811259.webp)

### 支持缓存记录的过期设定

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

是不是很方便？



### 支持缓存容量限制与不同淘汰策略

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

这里需要注意：按照权重进行限制缓存容量的时候必须要指定`weighter`属性才可以生效。上面代码中我们通过计算`value`对象的字节数（byte）来计算其权重信息，每1kb的字节数作为1个权重，整个缓存容器的总权重限制为1w，这样就可以实现将缓存内存占用控制在`10000*1k≈10M`左右。

有没有很省心？



### 支持集成数据源能力

在前面文章中，我们有介绍过缓存的**三种模型**，分别是`旁路型`、`穿透型`、`异步型`。Guava Cache作为一个封装好的缓存框架，是一个典型的**穿透型缓存**。正常业务使用缓存时通常会使用旁路型缓存，即先去缓存中尝试查询获取数据，如果获取不到则会从数据库中进行查询并加入到缓存中；而为了简化业务端使用复杂度，Guava Cache支持集成数据源，业务层面调用接口查询缓存数据的时候，如果缓存数据不存在，则会*自动*去数据源中进行数据获取并加入缓存中。

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



### 支持更新锁定能力

这个是与上面数据源集成一起的辅助增强能力。在高并发场景下，如果某个key值没有命中缓存，大量的请求同步打到下游模块处理的时候，很容易造成**缓存击穿**问题。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406052020511.webp)

为了防止缓存击穿问题，可以通过**加锁**的方式来规避。当缓存不可用时，仅`持锁的线程`负责从数据库中查询数据并写入缓存中，其余请求重试时先尝试从缓存中获取数据，避免所有的并发请求全部同时打到数据库上。

作为穿透型缓存的保护策略之一，*Guava Cache*自带了`并发锁定`机制，同一时刻仅允许一个请求去回源获取数据并回填到缓存中，而其余请求则阻塞等待，不会造成数据源的压力过大。

有没有被暖心到？



### 提供了缓存相关的一些监控统计

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



## Guava Cache适用场景

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



## Guava Cache使用

### 引入依赖

使用Guava Cache，首先需要引入对应的依赖包。对于Maven项目，可以在`pom.xml`中添加对应的依赖声明即可：

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>31.1-jre</version>
</dependency>
```



### 容器创建 —— CacheBuilder

具体使用前首先面临的就是如何创建Guava Cache实例。可以借助`CacheBuilder`以一种优雅的方式来构建出合乎我们诉求的Cache实例。

对*CacheBuilder*中常见的属性方法，归纳说明如下：

| 方法              | 含义说明                                                     |
| ----------------- | ------------------------------------------------------------ |
| newBuilder        | 构造出一个Builder实例类                                      |
| initialCapacity   | 待创建的缓存容器的初始容量大小（记录**条数**）               |
| maximumSize       | 指定此缓存容器的最大容量(最大缓存记录**条数**)               |
| maximumWeight     | 指定此缓存容器的最大容量（最大**比重**值），需结合`weighter`方可体现出效果 |
| expireAfterWrite  | 设定过期策略，按照数据**写入时间**进行计算                   |
| expireAfterAccess | 设定过期策略，按照数据最后**访问时间**来计算                 |
| weighter          | 入参为一个函数式接口，用于指定每条存入的缓存数据的权重占比情况。这个需要与`maximumWeight`结合使用 |
| refreshAfterWrite | 缓存写入到缓存之后                                           |
| concurrencyLevel  | 用于控制缓存的并发处理能力，同时支持多少个线程**并发写入**操作 |
| recordStats       | 设定开启此容器的数据加载与缓存命中情况统计                   |

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



### 业务层使用

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