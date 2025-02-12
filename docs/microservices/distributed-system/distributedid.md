---
title: 分布式ID实现方案
category: 微服务
tags:
  - 分布式
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,分布式ID,雪花算法
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---
分布式系统中设计分布式 ID 对于确保订单、用户或记录等实体的唯一性至关重要。

## 分布式 ID 的设计需求

- 唯一性：ID 必须在所有服务或系统中全局唯一。
- 可扩展性：系统应能够在高负载下以高吞吐量生成 ID。
- 排序性：在某些用例中，ID 需要是有序或大致按时间排序的（例如用于排序）。
- 避免碰撞：两个 ID 相同的概率应当极小。
- 去中心化：ID 的生成应不依赖单一的生成器，避免单点故障。
- 可用性：即使在网络分区时，ID 生成系统也应能正常工作。
- 紧凑性：ID 的格式应在存储时高效，特别是在数据库或日志中。
- 透明性：有时 ID 需要嵌入元数据（如时间戳或机器 ID ）以便调试或追踪。


## 常见的分布式 ID 解决方案

### UUID

UUID (Universally Unique IDentifier) 通用唯一识别码 ，也称为 GUID (Globally Unique IDentifier) 全球唯一标识符。

UUID是一个长度为128位的标志符，能够在时间和空间上确保其唯一性。

UUID最初应用于Apollo网络计算系统，随后在Open Software Foundation（OSF）的分布式计算环境（DCE）中得到应用。

可让分布式系统可以不借助中心节点，就可以生成唯一标识， 比如唯一的ID进行日志记录。

UUID是基于时间戳、MAC地址、随机数等多种因素生成，理论上全球范围内几乎不可能重复。

在Java中可以通过UUID的randomUUID方法获取唯一字符串：

```java
import java.util.UUID;

/**
 * @author 苏三
 * @date 2024/9/13 上午10:38
 */
public class UuidTest {
    public static void main(String[] args) {
        String uuid = UUID.randomUUID().toString();
        System.out.println(uuid);
    }
}
```

运行结果：

```java
22527933-d0a7-4c2b-a377-aeb438a31b02
```

优点：UUID不借助中心节点，可以保持程序的独立性，可以保证程序在不同的数据库之间，做数据迁移，都不受影响。

缺点：UUID生成的字符串太长，通过索引查询数据的效率比较低。此外，UUID生成的字符串，顺序没有保证，不是递增的，不满足工作中的有些业务场景。

> 在分布式日志系统或者分布式链路跟踪系统中，可以使用UUID生成唯一标识，用于串联请求的日志。



### 数据库自增ID

在很多数据库中自增的主键ID，数据库本身是能够保证唯一的。

MySQL中的auto_increment。

Oracle中sequence。

我们在业务代码中，不需要做任何处理，这个ID的值，是由数据库自动生成的，并且它会保证数据的唯一性。

优点：非常简单，数据查询效率非常高。

缺点：只能保证单表的数据唯一性，如果跨表或者跨数据库，ID可能会重复。ID是自增的，生成规则很容易被猜透，有安全风险。ID是基于数据库生成的，在高并发下，可能会有性能问题。

> 在一些老系统或者公司的内部管理系统中，可能会用数据库递增ID作为分布式ID的方案，这些系统的用户并发量一般比较小，数据量也不多。



### 数据库号段模式

在高并发的系统中，频繁访问数据库，会影响系统的性能。

可以对数据库自增ID方案做一个优化。

一次生成一定步长的ID，比如：步长是1000，每次数据库自增1000，ID值从100001变成了101001。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409191856204.png)
将100002~101001这个号段的1000个ID，缓存到服务器的内存从。

当有获取分布式ID的请求过来时，先从服务器的内存中获取数据，如果能够获取到，则直接返回。

如果没有获取到，则说明缓存的号段的数据已经被获取完了。

这时需要重新从数据库中获取一次新号段的ID，缓存到服务器的内存中，这样下次又能直接从内存中获取ID了。

优点：实现简单，对数据库的依赖减弱了，可以提升系统的性能。

缺点：ID是自增的，生成规则很容易被猜透，有安全风险。如果数据库是单节点的，有岩机的风险。



### 数据库的多主模式

为了解决上面单节点岩机问题，我们可以使用数据库的多主模式。

即有多个master数据库实例。

![img](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409191856194.png)

在生成ID的时候，一个请求只能写入一个master实例。

为了保证在不同的master实例下ID的唯一性，我们需要事先规定好每个master下的大的区间，比如：master1的数据是10开头的，master2的数据是11开头的，master3的数据是12开头的。

然后每个master，还是按照数据库号段模式来处理。

优点：避免了数据库号段模式的单节点岩机风险，提升了系统的稳定性，由于结合使用了号段模式，系统性能也是OK的。

缺点：跨多个master实例下生成的ID，可能不是递增的。



### Redis生成ID

除了使用数据库之外，Redis其实也能产生自增ID。

我们可以使用Redis中的incr命令：

```sql
redis> SET ID_VALUE 1000
OK

redis> INCR ID_VALUE
(integer) 1001

redis> GET ID_VALUE 
"1001"
```

给ID_VALUE设置了值是1000，然后使用INCR命令，可以每次都加1。

这个方案跟我们之前讨论过的方案1（数据库自增ID）的方案类似。

优点：方案简单，性能比方案1更好，避免了跨表或者跨数据库，ID重复的问题。

缺点：ID是自增的，生成规则很容易被猜透，有安全风险。并且Redis可能也存在单节点，岩机的风险。



### Zookeeper生成ID

Zookeeper主要通过其znode数据版本来生成序列号，可以生成32位和64位的数据版本号，客户端可以使用这个版本号来作为唯一的序列号。

由于需要高度依赖Zookeeper，并且是同步调用API，如果在竞争较大的情况下，需要考虑使用分布式锁。

因此，性能在高并发的分布式环境下，也不太理想。

很少人会使用Zookeeper来生成唯一ID。



### 雪花算法

#### 概述

Snowflake，雪花算法是由Twitter开源的分布式ID生成算法，以划分命名空间的方式将 64-bit位分割成多个部分，每个部分代表不同的含义。而 Java中64bit的整数是Long类型，所以在 Java 中 SnowFlake 算法生成的 ID 就是 long 来存储的。

- 第1位占用1bit，其值始终是0，可看做是符号位不使用。

- 第2位开始的41位是时间戳，41-bit位可表示2^41个数，每个数代表毫秒，那么雪花算法可用的时间年限是(1L<<41)/(1000L360024*365)=69 年的时间。

- 中间的10-bit位可表示机器数，即2^10 = 1024台机器，但是一般情况下我们不会部署这么台机器。如果我们对IDC（互联网数据中心）有需求，还可以将 10-bit 分 5-bit 给 IDC，分5-bit给工作机器。这样就可以表示32个IDC，每个IDC下可以有32台机器，具体的划分可以根据自身需求定义。

- 最后12-bit位是自增序列，可表示2^12 = 4096个数。

这样的划分之后相当于**在一毫秒一个数据中心的一台机器上可产生4096个有序的不重复的ID**。但是我们 IDC 和机器数肯定不止一个，所以毫秒内能生成的有序ID数是翻倍的。
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409191856249.png)



优点：算法简单，在内存中进行，效率高。高并发分布式环境下生成不重复ID，每秒可生成百万个不重复ID。
基于时间戳，以及同一时间戳下序列号自增，基本保证ID有序递增。并且不依赖第三方库或者中间件，稳定性更好。

缺点：依赖服务器时间，服务器时钟回拨时可能会生成重复ID。



#### 代码实现

Snowflake 的Twitter官方原版是用Scala写的，对Scala语言有研究的同学可以去阅读下，以下是 Java 版本的写法

```java
/**
 * Twitter_Snowflake<br>
 * SnowFlake的结构如下(每部分用-分开):<br>
 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000 <br>
 * 1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0<br>
 * 41位时间截(毫秒级)，注意，41位时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截)
 * 得到的值），这里的的开始时间截，一般是我们的id生成器开始使用的时间，由我们程序来指定的（如下下面程序IdWorker类的startTime属性）。41位的时间截，可以使用69年，年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69<br>
 * 10位的数据机器位，可以部署在1024个节点，包括5位datacenterId和5位workerId<br>
 * 12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号<br>
 * 加起来刚好64位，为一个Long型。<br>
 * SnowFlake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，经测试，SnowFlake每秒能够产生26万ID左右。
 */
public class SnowflakeDistributeId {


    // ==============================Fields===========================================
    /**
     * 开始时间截 (2015-01-01)
     */
    private final long twepoch = 1420041600000L;

    /**
     * 机器id所占的位数
     */
    private final long workerIdBits = 5L;

    /**
     * 数据标识id所占的位数
     */
    private final long datacenterIdBits = 5L;

    /**
     * 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
     */
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);

    /**
     * 支持的最大数据标识id，结果是31
     */
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    /**
     * 序列在id中占的位数
     */
    private final long sequenceBits = 12L;

    /**
     * 机器ID向左移12位
     */
    private final long workerIdShift = sequenceBits;

    /**
     * 数据标识id向左移17位(12+5)
     */
    private final long datacenterIdShift = sequenceBits + workerIdBits;

    /**
     * 时间截向左移22位(5+5+12)
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    /**
     * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
     */
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    /**
     * 工作机器ID(0~31)
     */
    private long workerId;

    /**
     * 数据中心ID(0~31)
     */
    private long datacenterId;

    /**
     * 毫秒内序列(0~4095)
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间截
     */
    private long lastTimestamp = -1L;

    //==============================Constructors=====================================

    /**
     * 构造函数
     *
     * @param workerId     工作ID (0~31)
     * @param datacenterId 数据中心ID (0~31)
     */
    public SnowflakeDistributeId(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    // ==============================Methods==========================================

    /**
     * 获得下一个ID (该方法是线程安全的)
     *
     * @return SnowflakeId
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        //如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            //毫秒内序列溢出
            if (sequence == 0) {
                //阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        //时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }

        //上次生成ID的时间截
        lastTimestamp = timestamp;

        //移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - twepoch) << timestampLeftShift) //
                | (datacenterId << datacenterIdShift) //
                | (workerId << workerIdShift) //
                | sequence;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 返回以毫秒为单位的当前时间
     *
     * @return 当前时间(毫秒)
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }
}
```

测试的代码如下

```java
public static void main(String[] args) {
    SnowflakeDistributeId idWorker = new SnowflakeDistributeId(0, 0);
    for (int i = 0; i < 1000; i++) {
        long id = idWorker.nextId();
//      System.out.println(Long.toBinaryString(id));
        System.out.println(id);
    }
}
```



**雪花算法提供了一个很好的设计思想，雪花算法生成的ID是趋势递增，不依赖数据库等第三方系统，以服务的方式部署，稳定性更高，生成ID的性能也是非常高的，而且可以根据自身业务特性分配bit位，非常灵活**。

但是雪花算法强**依赖机器时钟**，如果机器上时钟回拨，会导致发号重复或者服务会处于不可用状态。如果恰巧回退前生成过一些ID，而时间回退后，生成的ID就有可能重复。官方对于此并没有给出解决方案，而是简单的抛错处理，这样会造成在时间被追回之前的这段时间服务不可用。

很多其他类雪花算法也是在此思想上的设计然后改进规避它的缺陷





### Leaf

Leaf是美团开源的分布式ID生成系统，它提供了两种生成ID的方式：

- Leaf-segment号段模式
- Leaf-snowflake雪花算法

Leaf-segment号段模式，需要创建一张表：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409191856155.png)
这个模式就是我们在第3节讲过的数据库号段模式。

biz_tag用来区分业务，max_id表示该biz_tag目前所被分配的ID号段的最大值，step表示每次分配的号段长度。

原来获取ID每次都需要写数据库，现在只需要把step设置得足够大，比如1000。那么只有当1000个号被消耗完了之后才会去重新读写一次数据库。

Leaf-snowflake雪花算法，是在传统雪花算法之上，加上Zookeeper，做了一点改造：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409191856171.png)

Leaf-snowflake服务需要从Zookeeper按顺序的获取workId，会缓存到本地。

如果Zookeeper出现异常，Leaf-snowflake服务会直接获取本地的workId，它相当于对Zookeeper是弱依赖的。

因为这种方案依赖时间，如果机器的时钟发生了回拨，那么就会有可能生成重复的ID号，它内部有一套机制解决机器时钟回拨的问题：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409191856198.png)

如果你想知道美团Leaf的更多细节，可以看看Github地址：https://github.com/Meituan-Dianping/Leaf

最近整理了一份10万字的面试宝典，可以免费送给大家，获取方式加我微信：su_san_java，备注：面试。



### Tinyid

Tinyid是滴滴用Java开发的一款分布式id生成系统，基于数据库号段算法实现。

Tinyid是在美团的ID生成算法Leaf的基础上扩展而来，支持数据库多主节点模式，它提供了REST API和JavaClient两种获取方式，相对来说使用更方便。

但跟美团Leaf不同的是，Tinyid只支持号段一种模式，并不支持Snowflake模式。

基于数据库号段模式的简单架构方案：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409191856941.png)
ID生成系统向外提供http服务，请求经过负载均衡router，能够路由到其中一台tinyid-server，这样就能从事先加载好的号段中获取一个ID了。

如果号段还没有加载，或者已经用完了，则需要向db再申请一个新的可用号段，多台server之间因为号段生成算法的原子性，而保证每台server上的可用号段不重，从而使id生成不重。

但也带来了这些问题：

- 当id用完时需要访问db加载新的号段，db更新也可能存在version冲突，此时id生成耗时明显增加。
- db是一个单点，虽然db可以建设主从等高可用架构，但始终是一个单点。
- 使用http方式获取一个id，存在网络开销，性能和可用性都不太好。

为了解决这些这些问题：增加了tinyid-client本地生成ID、使用双号段缓存、增加多 db 支持提高服务的稳定性。

最终的架构方案如下：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409191856155.png)

Tinyid方案主要做了下面这些优化：

- 增加tinyid-client：tinyid-client向tinyid-server发送请求来获取可用号段，之后在本地构建双号段、id生成，如此id生成则变成纯本地操作，性能大大提升。
- 使用双号段缓存：为了避免在获取新号段的情况下，程序获取唯一ID的速度比较慢。Tinyid中的号段在用到一定程度的时候，就会去异步加载下一个号段，保证内存中始终有可用号段。
- 增加多db支持：每个DB都能生成唯一ID，提高了可用性。

如果你想知道滴滴Tinyid的更多细节，可以看看Github地址：https://github.com/didi/tinyid



### UidGenerator

百度 UID-Generator 使用 Java 语言，基于雪花算法实现。

UidGenerator以组件形式工作在应用项目中, 支持自定义workerId位数和初始化策略, 从而适用于docker等虚拟化环境下实例自动重启、漂移等场景。

在实现上, UidGenerator通过借用未来时间来解决sequence天然存在的并发限制。

采用RingBuffer来缓存已生成的UID, 并行化UID的生产和消费, 同时对CacheLine补齐，避免了由RingBuffer带来的硬件级「伪共享」问题. 最终单机QPS可达600万。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409191856364.png)

Snowflake算法描述：指定机器 & 同一时刻 & 某一并发序列，是唯一的。据此可生成一个64 bits的唯一ID（long）。默认采用上图字节分配方式：

- sign(1bit)：固定1bit符号标识，即生成的UID为正数。
- delta seconds (28 bits) ：当前时间，相对于时间基点"2016-05-20"的增量值，单位：秒，最多可支持约8.7年
- worker id (22 bits)：机器id，最多可支持约420w次机器启动。内置实现为在启动时由数据库分配，默认分配策略为用后即弃，后续可提供复用策略。
- sequence (13 bits)：每秒下的并发序列，13 bits可支持每秒8192个并发。

sequence决定了UidGenerator的并发能力，13 bits的 sequence 可支持 8192/s 的并发，但现实中很有可能不够用，从而诞生了 CachedUidGenerator。

CachedUidGenerator 使用 RingBuffer 缓存生成的id。RingBuffer是个环形数组，默认大小为 8192 个（可以通过boostPower参数设置大小）。

RingBuffer环形数组，数组每个元素成为一个 slot。

Tail 指针、Cursor 指针用于环形数组上读写 slot：

- Tail指针：表示 Producer 生产的最大序号(此序号从 0 开始，持续递增)。Tail 不能超过 Cursor，即生产者不能覆盖未消费的 slot。当 Tail 已赶上 curosr，此时可通过 rejectedPutBufferHandler 指定 PutRejectPolicy。
- Cursor指针：表示 Consumer 消费到的最小序号(序号序列与 Producer 序列相同)。Cursor 不能超过 Tail，即不能消费未生产的 slot。当 Cursor 已赶上 tail，此时可通过 rejectedTakeBufferHandler 指定 TakeRejectPolicy。
  ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409191856469.png)

RingBuffer填充触发机制:

- 程序启动时，将RingBuffer填充满。
- 在调用getUID()方法获取id时，如果检测到RingBuffer中的剩余id个数小于总个数的50%，将RingBuffer填充满。
- 定时填充（可配置是否使用以及定时任务的周期）。

如果你想知道百度uid-generator的更多细节，可以看看Github地址：https://github.com/baidu/uid-generator


## 选择解决方案的考虑因素

- 吞吐量需求：如果系统需要每秒生成数百万个 ID，Snowflake 或 Redis-based 方案比 UUID 更合适。
- 有序还是随机：如果 ID 需要按时间排序，可以考虑 Snowflake、KSUID。
- 存储限制：与 KSUID 相比，Snowflake ID 更小，如果存储大小至关重要，可以选择更紧凑的格式。
- 元数据：如果需要在ID中包含元数据，Snowflake ID 或自定义哈希方案可以编码时间戳或机器 ID 等信息。

每种解决方案适合不同的用例，具体选择取决于扩展性、排序和存储大小等因素。Snowflake 是现代分布式系统中最常采用的方案。


 

 


<!-- @include: @article-footer.snippet.md -->     

