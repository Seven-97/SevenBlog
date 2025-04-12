---
title: Ehcache
category: 系统设计
tag:
 - 缓存
 - Ehcache
---



> 来源：稀土掘金社区[深入理解缓存原理与实战设计](https://juejin.cn/column/7140852038258147358)，Seven进行了部分补充完善



## 介绍

**Ehcache**最初是由Greg Luck于2003年开始开发，截止目前，Ehcache已经演进到了`3.10.0`版本，各方面的能力已经构建的非常完善。[Ehcache官网](https://link.juejin.cn?target=https%3A%2F%2Fwww.ehcache.org%2F)上也毫不谦虚的描述自己是“*Java's most widely-used cache*”，即JAVA中使用最广泛的缓存，足见`Ehcache`的强大与自信。



### Ehcache的闪光特性

#### 支持多级缓存

之前文章中我们介绍过的Guava Cache或者是Caffeine，都是纯**内存缓存**，使用上会受到内存大小的制约，而Ehcache则打破了这一约束。**Ehcache2.x**时代就已经支持了基于`内存`和`磁盘`的二级缓存能力，而演进到**Ehcache3.x**版本时进一步扩展了此部分能力，增加了对于`堆外缓存`的支持。此外，结合Ehcache原生支持的`集群`能力，又可以打破单机的限制，完全解决容量这一制约因素。

综合而言，Ehcache支持的缓存形式就有了如下四种：



##### 堆内缓存（heap）

所谓的`堆内`（heap）缓存，就是我们常规意义上说的*内存缓存*，严格意义上来说，是指**被JVM托管**占用的部分内存。内存缓存最大的优势就是具有超快的读写速度，但是不足点就在于`容量有限`、且`无法持久化`。

在创建缓存的时候可以指定使用堆内缓存，也可以一并指定堆内缓存允许的`最大字节数`。

```java
// 指定使用堆内缓存，并限制最大容量为100M
ResourcePoolsBuilder.newResourcePoolsBuilder().heap(100, MemoryUnit.MB);
```

除了按照总字节大小限制，还可以按照`记录数`进行约束：

```java
// 指定使用堆内缓存，并限制最大容量为100个Entity记录
ResourcePoolsBuilder.newResourcePoolsBuilder().heap(100, EntryUnit.ENTRIES);
```



##### 堆外缓存（off-heap）

`堆外`（off-heap）缓存，同样是存储在**内存**中。其实就是在内存中开辟一块区域，将其当做磁盘进行使用。由于内存的读写速度特别快，所以将数据存储在这个区域，读写上可以获得比本地磁盘读取更优的表现。这里的“堆外”，主要是相对与JVM的堆内存而言的，因为这个区域**不在JVM的堆内存**中，所以叫堆外缓存。这块的关系如下图示意：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406110032919.webp)



看到这里，不知道大家是否有这么个疑问：既然都是内存中存储，那为何多此一举非要将其划分为堆外缓存呢？直接将这部分的空间类驾到堆内缓存上，不是一样的效果吗？

我们知道**JVM**会基于`GC机制`自动的对内存中不再使用的对象进行*垃圾回收*，而`GC`的时候对系统性能的影响是非常大的。堆内缓存的数据越多，GC的压力就会越大，对系统性能的影响也会越明显。所以为了降低大量缓存对象的GC回收动作的影响，便出现了`off-heap`处理方式。在JVM堆外的内存中开辟一块空间，可以像使用本地磁盘一样去使用这块内存区域，这样就既享受了内存的高速读写能力，又**避免频繁GC**带来的烦恼。

可以在创建缓存的时候，通过`offheap`方法来指定使用堆外缓存并设定堆外缓存的容量大小，这样当heap缓存容量满之后，其余的数据便会存储到堆外缓存中。

```java
ResourcePoolsBuilder.newResourcePoolsBuilder()
        .heap(100, MemoryUnit.KB) // 堆内缓存100K
        .offheap(10, MemoryUnit.MB); // 堆外缓存10M
```

堆外缓存的时候，**offheap**的大小设定需要注意两个原则：

1. offheap需要**大于heap**的容量大小（前提是heap大小设定的是*字节数*而非Entity数）
2. offheap大小**必须1M以上**。

如果设定的时候不满足上述条件，会报错：

```csharp
Caused by: java.lang.IllegalArgumentException: The value of maxBytesLocalOffHeap is less than the minimum allowed value of 1M. Reconfigure maxBytesLocalOffHeap in ehcache.xml or programmatically.
	at org.ehcache.impl.internal.store.offheap.HeuristicConfiguration.<init>(HeuristicConfiguration.java:55)
	at org.ehcache.impl.internal.store.offheap.OffHeapStore.createBackingMap(OffHeapStore.java:102)
	at org.ehcache.impl.internal.store.offheap.OffHeapStore.access$500(OffHeapStore.java:69)
```

总结下堆内缓存与堆外缓存的区别与各自**优缺点**：

1. `堆内缓存`是由**JVM管理**的，在JVM中可以直接去以**引用**的形式去读取，所以读写的*速度会特别高*。而且JVM会负责其内容的回收与清理，使用起来比较“省心”。
2. `堆外缓存`是在内存中划定了一块独立的存储区域，然后可以将这部分内存当做“磁盘”进行使用。需要使用方自行维护数据的清理，读写前需要**序列化**与**反序列化**操作，但可以省去GC的影响。



##### 磁盘缓存（disk）

当我们需要缓存的数据量特别大、内存容量无法满足需求的时候，可以使用`disk`磁盘存储来作为补充。相比于内存，磁盘的读写速度显然要慢一些、但是胜在其价格便宜，*容量*可以足够大。

我们可以在缓存创建的时候，指定使用磁盘缓存，作为堆内缓存或者堆外缓存的补充。

```java
ResourcePoolsBuilder.newResourcePoolsBuilder()
        .heap(10, MemoryUnit.MB) 
        .offheap(1, MemoryUnit.MB)
        .disk(10, MemoryUnit.GB); // 指定使用10G磁盘缓存空间
```

需要注意这里磁盘的容量设定一定要**大于**前面的`heap`以及`offHeap`的大小，否则会报错：

```java
Exception in thread "main" java.lang.IllegalArgumentException: Tiering Inversion: 'Pool {100 MB offheap}' is not smaller than 'Pool {20 MB disk}'
	at org.ehcache.impl.config.ResourcePoolsImpl.validateResourcePools(ResourcePoolsImpl.java:137)
	at org.ehcache.config.builders.ResourcePoolsBuilder.<init>(ResourcePoolsBuilder.java:53)
```



##### 集群缓存（Cluster）

作为单机缓存，数据都是存在各个进程内的，在分布式组网系统中，如果缓存数据发生变更，就会出现各个进程节点中缓存**数据不一致**的问题。为了解决这一问题，Ehcache支持通过**集群**的方式，将多个分布式节点组网成一个整体，保证相互节点之间的数据同步。



##### 小结

需要注意的是，除了堆内缓存属于JVM堆内部，可以直接通过引用的方式进行访问，其余几种类型都属于JVM外部的数据交互，所以对这部分数据的读写时，需要先进行`序列化`与`反序列化`，因此要求缓存的数据对象一定要支持序列化与反序列化。

不同的缓存类型具有不同的运算处理速度：**堆内缓存 > 堆外缓存 > 集群缓存**

为了兼具处理性能与缓存容量，可以采用多种缓存形式组合使用的方式，构建`多级缓存`来实现。组合上述几种不同缓存类型然后构建多级缓存的时候，也需要遵循几个约束：

1. 多级缓存中必须有**堆内缓存**，必须按照`堆内缓存 < 堆外缓存 < 磁盘缓存 < 集群缓存`的顺序进行组合；
2. 多级缓存中的容量设定必须遵循`堆内缓存 < 堆外缓存 < 磁盘缓存 < 集群缓存`的原则；
3. 多级缓存中**不允许***磁盘缓存*与**集群缓存**同时出现；

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406110036850.webp)

按照上述原则，可以组合出所有合法的多级缓存类型：

- 堆内缓存 + 堆外缓存
- 堆内缓存 + 堆外缓存 + 磁盘缓存 
- 堆内缓存 + 堆外缓存 + 集群缓存 
- 堆内缓存 + 磁盘缓存 
- 堆内缓存 + 集群缓存



#### 支持缓存持久化

常规的基于内存的缓存都有一个通病就是无法持久化，每次重新启动的时候，缓存数据都会丢失，需要重新去构建。而Ehcache则支持使用磁盘来对缓存内容进行**持久化**保存。

如果需要开启持久化保存能力，我们首先需要在创建缓存的时候先指定下持久化结果存储的磁盘根目录，然后需要指定组合使用磁盘存储的容量，并选择开启持久化数据的能力。

```java
public static void main(String[] args) {
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("myCache", CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class,
                    String.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                            .heap(1, MemoryUnit.MB)
                            .disk(10, MemoryUnit.GB, true)) // 指定需要持久化到磁盘
                    .build())
            .with(CacheManagerBuilder.persistence("d:\\myCache\\")) // 指定持久化磁盘路径
            .build(true);
    Cache<Integer, String> myCache = cacheManager.getCache("myCache", Integer.class, String.class);
    myCache.put(1, "value1");
    myCache.put(2, "value2");
    System.out.println(myCache.get(2));
    cacheManager.close();
}
```

执行之后，指定的目录里面会留有对应的持久化文件记录：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406110038127.webp)

这样在进程重新启动的时候，会自动从持久化文件中读取内容并加载到缓存中，可以直接使用。比如我们将代码修改下，缓存创建完成后不执行`put`操作，而是直接去读取数据。比如还是上面的这段代码，将`put`操作注释掉，重新启动执行，依旧可以获取到缓存值。



#### 支持变身分布式缓存

在集群多节点场景下本地缓存经常会出现的一个[**缓存漂移**](https://www.seven97.top/system-design/cache-column/cache-basic.html)问题。

对于分布式系统，或者是集群场景下，并非是本地缓存的主战场。为了保证集群内数据的一致性，很多场景往往就直接选择`Redis`等**集中式缓存**。但是集中式缓存也弊端，比如有些数据并不怎么更新、但是每个节点对其依赖度却非常高，如果频繁地去Redis请求交互，又会导致大量的性能损耗在**网络IO**交互处理上。

针对这种情况，Ehcache给出了一个相对完美的答案：`本地 + 集群化`策略。即在本地缓存的基础上，将集群内各本地节点组成一个相互连接的网，然后基于某种机制，将一个节点上发生的变更同步给其余节点进行同步更新自身缓存数据，这样就可以实现各个节点的缓存数据一致。

Ehcache提供了多种不同的解决方案，可以将其由本地缓存变身为“分布式缓存”：

- `RMI`组播方式
- `JMS`消息方式
- `Cache Server`模式
- `JGroup`方式
- `Terracotta`方式



#### 更灵活和细粒度的过期时间设定

前面介绍过的本地缓存框架Caffeine与Guava Cache，它们支持设定过期时间，但是仅允许为设定缓存**容器级别统一**的过期时间，容器内的所有元素都遵循同一个过期时间。

Ehcache不仅支持缓存容器对象级别统一的过期时间设定，还会支持为容器中每一条缓存记录设定**独立过期时间**，允许不同记录有不同的过期时间。这在某些场景下还是非常友好的，可以指定部分热点数据一个相对较长的过期时间，避免热点数据因为过期导致的*缓存击穿*。



#### 同时支持JCache与SpringCache规范

Ehcache作为一个标准化构建的通用缓存框架，同时支持了JAVA目前业界最为主流的两大缓存标准，即官方的JSR107标准以及使用非常广泛的Spring Cache标准，这样使得业务中可以基于标准化的缓存接口去调用，避免了Ehcache深度耦合到业务逻辑中去。

作为当前绝对主流的Spring框架，Ehcache可以做到无缝集成，便于项目中使用。



### Hibernate的默认缓存策略

`Hibernate`是一个著名的开源**ORM框架**实现，提供了对`JDBC`的轻量级封装实现，可以在代码中以面向对象的方式去操作数据库数据，此前著名的`SSH`框架中的`H`，指的便是Hibernate框架。Hibernate支持一二级缓存，其中一级缓存是*session级别*的缓存，默认开启。而Hibernate的二级缓存，默认使用的便是Ehcache来实现的。能够被大名鼎鼎的Hibernate选中作为默认的缓存实现，也可以证明Ehcache不俗的实力。



### Ehcache、Caffeine、Redis如何选择

之前的文章中介绍过Caffeine的相关特性与用法，两者虽然同属JVM级别的本地缓存框架，但是两者在目标细分领域，还是各有侧重的。而作为具备分布式能力的本地缓存，Ehcache与天生的分布式集中式缓存之间似乎也存在一些功能上的重合度，那么`Ehcache`、`Caffeine`、`Redis`三者之间应该**如何选择**呢？先看下三者的定位：



- **Caffeine**

1. 更加**轻量级**，使用更加简单，可以理解为一个*增强版的HashMap*；
2. 足够**纯粹**，适用于仅需要本地缓存数据的常规场景，可以获取到绝佳的命中率与并发访问性能。

- **Redis**

1. 纯粹的**集中**缓存，为集群化、分布式多节点场景而生，可以保证缓存的一致性；
2. 业务需要通过网络进行交互，相比与本地缓存而言*性能上会有损耗*。

- **Ehcache**

1. 支持多级缓存扩展能力。通过`内存+磁盘`等多种存储机制，解决缓存容量问题，适合本地缓存中对容量有特别要求的场景；
2. 支持缓存数据`持久化`操作。允许将内存中的缓存数据持久化到磁盘上，进程启动的时候从磁盘加载到内存中；
3. 支持多节点`集群化`组网。可以将分布式场景下的各个节点组成集群，实现缓存数据一致，解决缓存漂移问题。



相比而言，Caffeine专注于提供纯粹且简单的本地基础缓存能力、Redis则聚焦统一缓存的数据一致性方面，而Ehcache的功能则是更为的**中庸**，介于两者之间，既具有本地缓存无可比拟的性能优势，又兼具分布式缓存的多节点数据一致性与容量扩展能力。项目里面进行选型的时候，可以结合上面的差异点，评估下自己的实际诉求，决定如何选择。

简单来说，把握如下原则即可：

- 如果只是本地简单、少量缓存数据使用的，选择`Caffeine`；
- 如果本地缓存数据量较大、内存不足需要使用磁盘缓存的，选择`EhCache`；
- 如果是大型分布式多节点系统，业务对缓存使用较为重度，且各个节点需要依赖并频繁操作同一个缓存，选择`Redis`。



## Ehcache的使用

### Ehcache的依赖集成与配置

#### 依赖引入

集成使用Ehcache的第一步，就是要引入对应的依赖包。对于Maven项目而言，可以在pom.xml中添加对应依赖：

```xml
<dependency>
  <groupId>org.ehcache</groupId>
  <artifactId>ehcache</artifactId>
  <version>3.10.0</version>
</dependency>      
```

依赖添加完成后，还需要对缓存进行配置后方可使用。



#### 缓存的配置与创建

##### 使用代码配置与创建Ehcache

Ehcache支持在代码中手动创建缓存对象，并指定对应缓存参数信息。在使用之前，需要先了解几个关键代码类：

| 类名                      | 具体说明                                                     |
| ------------------------- | ------------------------------------------------------------ |
| CacheManagerBuilder       | CacheManager对象的构造器对象，可以方便的指定相关参数然后创建出符合条件的CacheManager对象。 |
| ResourcePoolsBuilder      | 用于指定缓存的存储形式（ResourcePools）的配置构造器对象，可以指定缓存是`堆内`缓存、`堆外`缓存、`磁盘`缓存或者多者的组合，以及各个类型缓存的容量信息、是否持久化等信息。 |
| CacheConfiguration        | 用于承载所有指定的关于缓存的配置属性值。                     |
| CacheConfigurationBuilder | 用于生成最终缓存总体配置信息的构造器，可以指定缓存`存储形式`（ResourcePools）、`过期策略`（ExpiryPolicy）、`键值类型`等等各种属性值。 |

通过组合使用上述Builder构造器，我们便可以在代码中完成对缓存Cache属性的设置。比如下面这样：

```java
public static void main(String[] args) {
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
            .with(CacheManagerBuilder.persistence("d:\\myCache\\"))
            .build(true);
    // 指定缓存的存储形式，采用多级缓存，并开启缓存持久化操作
    ResourcePools resourcePools = ResourcePoolsBuilder.newResourcePoolsBuilder()
            .heap(1, MemoryUnit.MB)
            .disk(10, MemoryUnit.GB, true)
            .build();
    // 封装缓存配置对象，指定了键值类型、指定了使用TTL与TTI联合的过期淘汰策略
    CacheConfiguration<Integer, String> cacheConfiguration =
            CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class, String.class, resourcePools)
                    .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofSeconds(10)))
                    .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(5)))
                    .build();
    // 使用给定的配置参数，创建指定名称的缓存对象
    Cache<Integer, String> myCache = cacheManager.createCache("myCache", cacheConfiguration);
}
```

上面的示例中，我们创建了一个基于`heap + disk`的**二级缓存**对象，并开启了缓存的持久化，以及指定了持久化结果文件的存储路径。



##### 基于XML配置Ehcache

因为Ehcache在创建缓存的时候可以指定的参数较多，如果通过上面的代码方式指定配置，略显繁琐且不够清晰直观，并且当需要创建多个不同的缓存对象的时候比较麻烦。好在Ehcache还提供了一种通过`XML`来进行参数配置的途径，并且支持在一个xml中配置多个不同的缓存对象信息。

在项目的resource目录下添加个Ehcache的配置文件，比如取名`ehcache.xml`，项目层级结构示意如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406110047794.webp)

然后我们在ehcache.xml中添加配置内容。内容示例如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:jsr107='http://www.ehcache.org/v3/jsr107'
        xmlns='http://www.ehcache.org/v3'
        xsi:schemaLocation="http://www.ehcache.org/v3 http://www.ehcache.org/schema/ehcache-core-3.1.xsd
        http://www.ehcache.org/v3/jsr107 http://www.ehcache.org/schema/ehcache-107-ext-3.1.xsd">

    <persistence directory="D:\myCache"/>

    <cache alias="myCache">
        <key-type>java.lang.Integer</key-type>
        <value-type>java.lang.String</value-type>
        <expiry>
            <tti unit="minutes">5</tti>
        </expiry>
        <resources>
            <heap unit="MB">10</heap>
            <offheap unit="MB">50</offheap>
            <disk persistent="true" unit="MB">500</disk>
        </resources>
    </cache>
</config>
```

上面演示的`Ehcache3.x`版本中的配置实现方式（配置文件与*Ehcache2.x*存在**较大差异**，不要混用，运行会报错），在xml中指定了`myCache`的key与value对应的类型，指定了基于TTI的5分钟过期淘汰策略，并规定了采用`heap + offheap + disk`的三级缓存机制，此外还开启了缓存持久化能力，并指定了持久化文件的存储路径。

通过xml配置的方式，可以很直观的看出这个缓存对象的所有关键属性约束，也是相比于代码中直接配置的方式更有优势的一个地方。在xml配置文件中，也可以同时配置多个缓存对象信息。此外，为了简化配置，Ehcache还支持通过`<cache-template>`来将一些公用的配置信息抽取出来成为模板，然后各个Cache独立配置的时候只需要增量配置各自差异化的部分即可，当然也可以基于给定的模板进行个性化的修改覆写配置。

比如下面这个配置文件，配置了两个Cache对象信息，复用了同一个配置模板，然后各自针对模板中不符合自己的配置进行了重新改写。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:jsr107='http://www.ehcache.org/v3/jsr107'
        xmlns='http://www.ehcache.org/v3'
        xsi:schemaLocation="
        http://www.ehcache.org/v3 http://www.ehcache.org/schema/ehcache-core-3.1.xsd
        http://www.ehcache.org/v3/jsr107 http://www.ehcache.org/schema/ehcache-107-ext-3.1.xsd">

    <persistence directory="D:\myCache"/>

    <cache-template name="myTemplate">
        <key-type>java.lang.String</key-type>
        <value-type>java.lang.String</value-type>
        <expiry>
            <ttl unit="minutes">30</ttl>
        </expiry>
        <resources>
            <heap unit="MB">10</heap>
            <disk unit="GB" persistent="true">2</disk>
        </resources>
    </cache-template>

    <cache alias="myCache" uses-template="myTemplate">
        <key-type>java.lang.Integer</key-type>
    </cache>
    <cache alias="myCache2" uses-template="myTemplate">
        <expiry>
            <ttl unit="minutes">60</ttl>
        </expiry>
    </cache>
</config>
```

配置完成之后，我们还需要在代码中指定使用此配置文件进行CacheManager创建与配置，并且完成CacheManager的*init初始化*操作。

```java
public Cache<Integer, String> createCacheWithXml() {
    // 获取配置文件
    URL xmlConfigUrl = this.getClass().getClassLoader().getResource("./ehcache.xml");
    // 解析对应的配置文件并创建CacheManager对象
    XmlConfiguration xmlConfiguration = new XmlConfiguration(xmlConfigUrl);
    CacheManager cacheManager = CacheManagerBuilder.newCacheManager(xmlConfiguration);
    // 执行初始化操作
    cacheManager.init();
    // 直接从CacheManager中根据名称获取对应的缓存对象
    return cacheManager.getCache("myCache", Integer.class, String.class);
}
```

这样，Ehcache的集成与配置就算完成了，接下来直接获取Cache对象并对其进行操作即可。

```java
public static void main(String[] args) {
    EhcacheService ehcacheService = new EhcacheService();
    Cache<Integer, String> cache = ehcacheService.createCacheWithXml();
    cache.put(1, "value1");
    System.out.println(cache.get(1));
}
```



当然，Ehcache3.x版本中使用xml方式配置的时候，有**几个坑**需要提防，避免踩坑。

1. 对于过期时间的设定*只允许选择ttl或者tti中的一者*，不允许两者同时存在——而通过代码配置的时候则没有这个问题。如果在xml中同时指定ttl与tti则运行的时候会抛异常。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406110047859.webp)

2. `<cache>`节点下面配置的时候，`<expire>`节点需要放在`<configuration>`节点的前面，否则会报错*Schema校验失败*。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406110047963.webp)



#### 业务中使用

缓存设置并创建完成后，业务代码中便可以通过Ehcache提供的接口，进行缓存数据的相关操作。业务使用是通过对Cache对象的操作来进行的，Cache提供的API接口与JDK中的Map接口极其相似，所以在使用上毫无门槛，可以直接上手。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406110049963.webp)

实际编码中，根据业务的实际诉求，通过Cache提供的API接口来完成缓存数据的增删改查操作。

```java
public static void main(String[] args) {
    EhcacheService ehcacheService = new EhcacheService();
    Cache<Integer, String> cache = ehcacheService.getCache();
    // 存入单条记录到缓存中
    cache.put(1, "value1");
    Map<Integer, String> values = new HashMap<>();
    values.put(2, "value2");
    values.put(3, "value3");
    // 批量向缓存中写入数据
    cache.putAll(values);
    // 当缓存不存在的时候才写入缓存
    cache.putIfAbsent(2, "value2");
    // 查询单条记录
    System.out.println(cache.get(2));
    // 批量查询操作
    System.out.println(cache.getAll(Stream.of(1,2,3).collect(Collectors.toSet())));
    // 移除单条记录
    cache.remove(1);
    System.out.println(cache.get(1));
    // 清空缓存记录
    cache.clear();
    System.out.println(cache.get(1));
}
```

从上述代码可以看出，EhCache具体使用起来与普通Map操作无异。虽然使用简单，但是这样也存在个问题就是业务代码所有使用缓存的地方，都需要强依赖Ehcache的具体接口，导致业务代码与Ehcache的依赖耦合度太高，后续如果想要更换缓存组件时，难度会非常大。



[JAVA业界的缓存标准规范](https://www.seven97.top/system-design/cache-column/javacacherule.html)，主要有`JSR107`标准与`Spring Cache`标准，如果可以通过标准的接口方式进行访问，这样就可以解决与EhCache深度耦合的问题了。令人欣慰的是，*Ehcache同时提供了对JSR107与Spring Cache规范的支持*！

下面一起看下如何通过JSR107规范接口以及Spring Cache的标准来使用Ehcache。



### 通过JCache API来使用Ehcache

#### 依赖集成与配置

如果要使用JCache标准方式来使用，需要额外引入JCache对应依赖包：

```xml
<dependency>
    <groupId>javax.cache</groupId>
    <artifactId>cache-api</artifactId>
    <version>1.1.1</version>
</dependency>
```

按照JCache的规范，必须通过CacheManager才能获取到Cache对象（这一点与Ehcache相同），而CacheManager则又需要通过`CacheProvider`来获取。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406110106247.webp)

遵循这一原则，我们可以按照JCache的方式来得到Cache对象：

```java
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;

public class JsrCacheService {
    public Cache<Integer, String> getCache() {
        CachingProvider cachingProvider = Caching.getCachingProvide();
        CacheManager cacheManager = cachingProvider.getCacheManager();
        MutableConfiguration<Integer, String> configuration =
                new MutableConfiguration<Integer, String>()
                        .setTypes(Integer.class, String.class)
                        .setStoreByValue(false)
                        .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ONE_MINUTE));
        Cache<Integer, String> myCache = cacheManager.createCach    ("myCache", configuration);
        System.out.println(myCache.getClass().getCanonicalName());
        return myCache;
    }
}
```

从`import`的内容可以看出上述代码没有调用到任何Ehcache的类，调用上述代码执行并打印出构建出来的Cache对象具体类型如下，可以看出的的确确创建出来的是Ehcache提供的`Eh107Cache`类：

```
org.ehcache.jsr107.Eh107Cache
```

这是为什么呢？其实原理很简单，之前介绍JCache API的文章中也有解释过。JCache中的CacheProvider其实是一个**SPI接口**，Ehcache实现并向JVM中注册了这一接口，所以JVM可以直接加载使用了Ehcache提供的实际能力。翻看下Ehcache的源码，我们也可以找到其SPI注册对应的配置信息：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406110106316.webp)

这里还有一个需要注意的点，因为SPI接口有可能被多个组件实现，而且可能会有多个组件同时往JVM中注册了*javax.cache.spi.CachingProvider*这一SPI接口的实现类，这种情况下，上述代码执行的时候会报错，因为没有指定具体使用哪一个SPI，所以JVM出现了选择困难症，只能抛异常了：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406110106311.webp)

所以为了避免这种情况的发生，我们可以在获取CacheProvider的时候，指定加载使用Ehcache提供的具体实现类`org.ehcache.jsr107.EhcacheCachingProvider`即可。

```java
CachingProvider cachingProvider = Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
```

上面代码中，使用了JCache的`MutableConfiguration`类来实现缓存配置的设定。作为通用规范，JCache仅定义了所有缓存实现者需要实现的功能的最小集，而Ehcache除了JCache提供的最低限度缓存功能外，还有很多其余缓存不具备的增强特性。如果需要使用这些特性，则需要使用Ehcache自己的缓存配置类来实现。

举个例子，MutableConfiguration只能设定基于内存缓存的一些行为参数，而如果需要配置Ehcache提供的`heap+offheap+disk`三级缓存能力，或者是要开启Ehcache的持久化能力，则MutableConfiguration就有点爱莫能助，只能Ehcache亲自出马了。

比如下面这样：

```java
public Cache<Integer, String> getCache() {
    CacheConfiguration<Integer, String> cacheConfiguration =
            CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class, String.class,
                    ResourcePoolsBuilder.heap(10).offheap(20, MemoryUnit.MB)).build();
    EhcacheCachingProvider cachingProvider = (EhcacheCachingProvider) Caching.getCachingProvider();
    CacheManager cacheManager = cachingProvider.getCacheManager();
    return cacheManager.createCache("myCache",
            Eh107Configuration.fromEhcacheCacheConfiguration(cacheConfiguration));
}
```

当然，也可以在JCache中继续使用Ehcache的xml配置方式。如下示意：

```java
public Cache<Integer, String> getCache3() throwsURISyntaxException {
    CachingProvider cachingProvider = Caching.getCachingProvider();
    CacheManager manager = cachingProvider.getCacheManager(
            getClass().getClassLoader().getResource("./ehcache.xml").toURI(),
            getClass().getClassLoader());
    return manager.getCache("myCache", Integer.class, String.class);
}
```

相比于使用纯粹的`JCache API`方式，上述两种使用Ehcache自己配置的方式可以享受到Ehcache提供的一些高级特性。但**代价**就是业务代码与Ehcache的解耦不是那么彻底，好在这些依赖仅在创建缓存的地方，对整体代码的耦合度影响不是很高，属于可接受的范围。



#### 业务中使用

完成了通过JCache API获取Cache对象，然后业务层代码中，便可以基于Cache对象提供的一系列方法，对缓存的具体内容进行操作了。

```java
public static void main(String[] args) throws Exception {
    JsrCacheService service = new JsrCacheService();
    Cache<Integer, String> cache = service.getCache();
    cache.put(1,"value1");
    cache.put(2,"value2");
    System.out.println(cache.get(1));
    cache.remove(1);
    System.out.println(cache.containsKey(1));
}
```



### 在Spring中集成Ehcache

作为JAVA领域霸主级别的存在，Spring凭借其优良的设计与出色的表现俘获了大批开发人员青睐，大部分项目都使用Spring作为基础框架来简化编码逻辑。Ehcache可以整合到Spring中，并搭配`Spring Cache`的标准化注解，让代码可以以一种更加优雅的方式来实现缓存的操作。

#### 依赖集成与配置

以SpringBoot项目为例进行说明，首先需要引入对应的依赖包。对于maven项目，在pom.xml中添加如下配置：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.ehcache</groupId>
    <artifactId>ehcache</artifactId>
</dependency>
```

依赖引入之后，我们需要在配置文件中指定使用Ehcache作为集成的缓存能力提供者，并且可以指定`ehcache.xml`独立的配置文件（ehcache.xml配置文件需要放置在resource目录下）：

```properties
spring.cache.type=ehcache
spring.cache.ehcache.config=./ehcache.xml
```

然后我们需要在项目启动类上添加上`@EnableCaching`来声明**开启缓存**能力：

```java
@SpringBootApplication
@EnableCaching
public class CrawlerApplication {
    // ...
}
```

到这里，对于`Ehcache2.x`版本而言，就已经完成集成预配置操作，可以直接在代码中进行操作与使用了。但是对于`Ehcache3.x`版本而言，由于**Spring并未提供对应的CacheManager对其进行支持**，如果这个时候我们直接启动程序，会在启动的时候就被无情的泼上一盆冷水：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406112302020.webp)

为了实现*Ehcache3.x*与*Spring*的集成，解决上述的问题，需要做一些额外的适配逻辑。根据报错信息，首先可以想到的就是手动实现cacheManager的创建与初始化。而由于Spring Cache提供了对JSR107规范的支持，且Ehcache3.x也全面符合JSR107规范，所以我们可以将三者结合起来，**以JSR107规范作为桥梁**，实现SpringBoot与Ehcache3.x的集成。

这个方案也即目前比较常用的"`SpringBoot + JCache + Ehcache`"组合模式。首先需要在前面已有实现的基础上，额外增加对JCache的依赖：

```xml
<dependency>
    <groupId>javax.cache</groupId>
    <artifactId>cache-api</artifactId>
    <version>1.1.1</version>
</dependency>
```

其次，需要修改下`application.properties`配置文件，将Spring Cache声明使用的缓存类型改为**JCache**。

```properties
spring.cache.type=jcache
spring.cache.jcache.config=./ehcache.xml
```

上面的配置看着**略显魔幻**，也是很多不清楚原有的小伙伴们会比较疑惑的地方（我曾经刚在项目中看到这种写法的时候，就一度怀疑是别人代码配置写错了）。但是经过上述的原因阐述，应该就明白其中的寓意了。

接下来，需要在项目中手动指定使用ehcache.xml配置文件来构建cacheManager对象。

```java
@Configuration
public class EhcacheConfig {
    @Bean
    public JCacheManagerFactoryBean cacheManagerFactoryBean() throws Exception {
        JCacheManagerFactoryBean factoryBean = new JCacheManagerFactoryBean();
        factoryBean.setCacheManagerUri(getClass().getClassLoader().getResource("ehcache.xml").toURI());
        return factoryBean;
    }
    @Bean
    public CacheManager cacheManager(javax.cache.CacheManager cacheManager) {
        JCacheCacheManager cacheCacheManager = new JCacheCacheManager();
        cacheCacheManager.setCacheManager(cacheManager);
        return cacheCacheManager;
    }
}
```

这样，就完成了通过JCache桥接来实现Spring中使用Ehcache3.x版本的目的了。



#### 支持Spring Cache注解操作

完成了Spring与Ehcache的整合之后，便可以使用Spring Cache提供的标准注解来实现对Ehcache缓存的操作。

首先需了解Spring Cache几个常用的注解及其含义：

| 注解           | 含义说明                                                     |
| -------------- | ------------------------------------------------------------ |
| @EnableCaching | 开启使用缓存能力                                             |
| @Cacheable     | 添加相关内容到缓存中                                         |
| @CachePut      | 更新相关缓存记录                                             |
| @CacheEvict    | 删除指定的缓存记录，如果需要清空指定容器的全部缓存记录，可以指定`allEntities=true`来实现 |

通过注解的方式，可以轻松的实现将某个方法调用的入参与响应映射自动缓存起来，基于AOP机制，实现了对业务逻辑无侵入式的静默缓存处理。

```java
@Service
@Slf4j
public class TestService {
    @Cacheable(cacheNames = "myCache", key = "#id")
    public String queryById(int id) {
        log.info("queryById方法被执行");
        return "value" + id;
    }
    @CachePut(cacheNames = "myCache", key = "#id")
    public String updateIdValue(int id, String newValue) {
        log.info("updateIdValue方法被执行");
        return newValue;
    }
    @CacheEvict(cacheNames = "myCache", key = "#id")
    public void deleteById(int id) {
        log.info("deleteById方法被执行");
    }
}
```

通过注解的方式指定了各个方法需要配套执行的缓存操作，具体业务代码里面则聚焦于自身逻辑，无需操心缓存的具体实现。可以通过下面的代码测试下集成后的效果：

```java
@GetMapping("/test")
public String test() {
    String value = testService.queryById(123);
    System.out.println("第一次查询，结果：" + value);
    value = testService.queryById(123);
    System.out.println("第二次查询，结果：" +value);
    testService.updateIdValue(123, "newValue123");
    value = testService.queryById(123);
    System.out.println("更新后重新查询，结果：" + value);
    testService.deleteById(123);
    value = testService.queryById(123);
    System.out.println("删除后重新查询，结果：" + value);
    return "OK";
}
```

执行结果如下：

```erlang
queryById方法被执行
第一次查询，结果：value123
第二次查询，结果：value123
updateIdValue方法被执行
更新后重新查询，结果：newValue123
deleteById方法被执行
queryById方法被执行
删除后重新查询，结果：newValue123
```

从测试结果可以看出，查询之后方法的入参与返回值被做了缓存，再次去查询的时候并没有真正的执行具体的查询操作方法，而调用删除方法之后再次查询，又会触发了真正的查询方法的执行。



## 分布式缓存

### 本地缓存或者集中缓存的问题

在正式开始阐述Ehcache的集群解决方案前，先来做个铺垫，了解下单机缓存与集中式缓存各自存在的问题。



#### 单机缓存不可言说的痛

对于**单机缓存**而言，缓存数据维护在进程中，应用系统部署完成之后，各个节点进程就会自己维护自己内存中的数据。在集群化部署的业务场景中，各个进程独自维护自己内存中的数据，而经由负载均衡器分发到各个节点进行处理的请求各不相同，这就导致了进程内缓存数据不一致，进而出现各种问题 —— 比较典型的就是*缓存漂移*问题。

缓存漂移，是单机缓存在分布式系统下无法忽视的一个问题。在这种情况下，大部分的项目使用中会选择避其锋芒、或者自行实现同步策略进行应对。常见的策略有：

- 本地缓存中仅存储一些固定不变、或者不常变化的数据。
- 通过过期重新加载、定时refresh等策略定时更新本地的缓存，忍受数据有一定时间内的*不一致*。
- 对于少量更新的场景，借助MQ构建*更新机制*，有变更就发到MQ中然后所有节点消费变更事件然后更新自身数据。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406112307010.webp)



#### 集中式缓存也并非万能银弹

在集群部署的场景下，为了简化缓存数据一致性方面的处理逻辑，大部分的场景会直接选择使用Redis等**集中式缓存**。集中式缓存的确是为分布式集群场景而生的，通过将缓存数据集中存放，使得每个业务节点读取与操作的都是同一份缓存记录。这样只需要由缓存服务保证并发原子性即可。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406112307968.webp)

但集中式缓存也并非是分布式场景下缓存方案的万能银弹。

项目中使用缓存的目的，主要是为了提升整体的运算处理效率，降低对外的IO请求等等。而集中式缓存是独立于进程之外部署的远端服务，需要基于*网络IO交互*的方式来获取，如果一个业务逻辑中涉及到非常频繁的缓存操作，势必会导致引入大量的网络IO交互，进而导致非常严重的**性能损耗**。

为了解决这个问题，很多时候还是需要本地缓存结合集中式缓存的方式，构建`多级缓存`的方式来解决。



### Ehcache分布式集群方案

相比纯粹的本地缓存，**Ehcache自带集群解决方案**，通过相应的配置可以让本地缓存变身集群版本，以此来应付分布式场景下各个节点缓存数据不一致的问题，并且由于数据都缓存在进程内部，所以也可以避免集中是缓存频繁在业务流程中频繁网络交互的弊端。

Ehcache官方提供了多种集群方案供选择，下面一起看下。



#### RMI组播

`RMI`是一种点对点（P2P）的通信交互机制，Ehcache利用RMI来实现多个节点之间数据的互通有无，相互知会彼此更新数据。对于集群场景下，这就要求集群内所有节点之间要两两互通，组成一张网状结构。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406112307490.webp)

在集群方式下进行数据通信交互，要求被传输的数据一定是要*可序列化与反序列化*的，对于JAVA而言，直白的说，就是对象一定是要实现了`Serializable`接口。

基于RMI组播的方式，Ehcache会向对应地址发送`RMI UDP`组播包，由于Ehcache对于组播的实现较为简单，所以在一些网络情况较为复杂的场景的支持度不是很完善，方案选择的时候需注意。此外，由于是即时消息模式，如果中途某个进程由于某些原因不可达，也可能会导致同步消息的丢失。所以对于可靠性以及数据一致性要求较高的场景需要**慎选**。



#### JMS消息

`JMS`消息方案是一种很常用的Ehcache集群方案。JMS是一套JAVA中两个进程之间的*异步通信API*，定义了消息通讯所必须的一组通用能力接口，比如消息的创建、发送、接收读取等。

JMS也支持构建基于事件触发模型的消息交互机制，也即生产者消费者模式（又称*发布订阅模式*），其核心就是一个消息队列，集群内各个业务节点都订阅对应的消息队列topic主题，如果有数据变更事件，也发送到消息队列的对应的topic主题下供其它节点消费。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406112308161.webp)

相比于RMI组播方式，JMS消息方式有个很大的优势在于不需要保证所有节点都全部同时在线，因为是基于发布订阅模式，所以即使有节点中途某些原因宕机又重启了，重启之后仍然可以接收其他节点已发布的变更，然后保证自己的缓存数据与其它节点一致。

Ehcache支持对接多种不同的MQ来实现基于JMS消息的集群组网方案，默认使用`ActiveMQ`，也可以切换为`Kafka`或者`RabbitMQ`等消息队列组件。



#### Cache Server模式

Ehcache的`Cache Server`是一种比较特殊的存在形式，它通常是一个独立的进程进行部署，然后多个独立的进程之间组成一个分布式集群。Cache Server是一个纯粹的缓存集群，对外提供*restful*接口或者*soap*接口，各个业务可以通过接口来获取缓存 —— 这个其实已经不是本地进程内缓存的概念了，其实就是一个独立的集中式缓存，类似Redis般的感觉。

看一下一个典型的高可用水平扩容模式的Cache Server组网与业务调用的场景示意图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406112308650.webp)

可以看到不管业务模块是用的什么编码语言，或者是什么形态的，都可以通过http接口去访问缓存数据，而Cache Server就是一个集中式缓存。在Cache Server中，集群内部可以有一个或者多个节点，这些节点具有完全相同的数据内容，做到了数据的冗余备份，而集群之间数据可以不同，实现了数据容量的水平扩展。

值得注意的一点是，如果你访问Ehcache的官网，会发现其官方提供的`3.x版本`的说明文档中**不再有Cache Server的身影**，而在2.x版本中都会作为一个单独的章节进行介绍。为什么在3.x版本中不再提供Cache Server模式呢？我在官方文档中没找到相关的说明，个人猜测主要有下面几个原因：

- *定位过于尴尬*，如果说要作为集中式缓存来使用，完全可以直接使用redis，没有必要费事劳神的去搭建Cache Server
- `Terracotta`方式相比而言功能上更加的完备，兼具水平扩展与本地缓存的双重优势，*完全可以取代Cache Server*



#### JGroups方式

`JGroups`的方式其实和RMI有点类似。JGroups是一个开源的群组通讯工具，可以用来创建一个组，这个组中的成员可以给其他成员发送消息。其工作模式基于IP组播（IP multicast），但可以在可靠性和群组成员管理上进行扩展，而且JGroups的架构上设计非常灵活，提供可以兼容多种协议的协议栈。

JGroups的**可靠性**体现在下面几个方面：

1. 对所有接收者的消息的无丢失传输（通过丢失消息的重发）
2. 大消息的分割传输和重组
3. 消息的顺序发送和接收
4. 保证原子性，消息要么被所有接收者接收，要么所有接收者都收不到

也正是由于JGroups具备的上述诸多优秀特性，它常常被选择作为集群内各个节点之间数据同步的解决方案。而Ehcache也一样，支持基于JGroups实现的集群方案，通过IP组播消息，保证集群内各个节点之间数据的同步。



#### Terracotta方式

`Terracotta`是什么？看下来自百度百科的介绍：

> Terracotta是一款由美国Terracotta公司开发的著名开源Java集群平台。它在JVM与Java应用之间实现了一个专门处理集群功能的抽象层，以其特有的增量检测、智能定向传送、分布式协作、服务器镜像、分片等技术，允许用户在不改变现有系统代码的情况下实现单机Java应用向集群化应用的无缝迁移。使得用户可以专注于商业逻辑的开发，由Terracotta负责实现高性能、高可用性、高稳定性的企业级Java集群。

所以说，Terracotta是一个JVM层专门负责做分布式节点间协同处理的平台框架。那么当优秀的JVM级缓存框架Ehcache与同样优秀的JVM间多节点协同框架Terracotta组合到一起，势必会有不俗的表现。

看下来自Ehcache官网的对于其Terracotta集群模式的图片说明：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406112308544.webp)

基于Terracotta方式，Ehcache可以支持：

- 热点数据存储在进程本地，然后根据热度进行优化存储，热度高的会优先存储在更快的位置（比如heap中）。
- 存储在其中一台应用节点上的缓存数据，可以被集群中其它节点访问到。
- 缓存数据在集群层面是完整的，也支持按照HA模式设定高可用备份。

可以说这种模式下，*既保留了Ehcache本地缓存的超高处理性能，又享受到了分布式缓存带来的集群优势*，不失为一种比较亮眼的组合。



### 引申思考 - 本地缓存的设计边界与定位

如上所言，纵使Ehcache提供了多种集群化策略，但略显尴尬的是实际中各个公司项目并没有大面积的使用。其实分析下来也很好理解：

> 如果真的需要很明确的诉求去解决分布式场景下的缓存一致性问题，直接选择redis、memcache等主流的集中式缓存组件即可

所以Ehcache的整体综合功能虽然是最强大的，整体定位偏向于大而全，但也导致在各个细分场景下表现不够极致：

- 相比`Caffeine`：略显臃肿， 因为提供了很多额外的功能，比如使用磁盘缓存、比如支持多节点间集群组网等；
- 相比`Redis`： 先天不足，毕竟是个本地缓存，纵使支持了多种组网模式，依旧无法媲美集中式缓存在分布式场景下的体验。

但在一些相对简单的集群数据同步场景下，或者对可靠性要求不高的集群缓存数据同步场景下，Ehcache还是很有优势的、尤其是`Terracotta集群`模式，也不啻为一个很好的选择。





<!-- @include: @article-footer.snippet.md -->     

