---
title: Redis集群机制深度剖析
category: 数据库
tags:
  - Redis
  - 缓存
head:
  - - meta
    - name: keywords
      content: redis,高可用,colony,集群,数据分布,Hash槽,请求重定向,集群扩容,集群故障检测,集群运维
  - - meta
    - name: description
      content: 全网最全的Redis知识点总结，让天下没有难学的八股文！
---





## 概述

Redis单实例的架构，从最开始的[一主N从](https://www.seven97.top/database/redis/04-ha1-masterslavereplication.html)，到读写分离，再到[Sentinel](https://www.seven97.top/database/redis/04-ha2-sentinelmechanism.html)哨兵机制，单实例的Redis缓存足以应对大多数的使用场景，也能实现主从故障迁移。

但是，在某些场景下，单实例存Redis缓存会存在的几个问题：

- 写并发：Redis单实例读写分离可以解决读操作的负载均衡，但对于写操作，仍然是全部落在了master节点上面，在海量数据高并发场景，一个节点写数据容易出现瓶颈，造成master节点的压力上升。

- 海量数据的存储压力：单实例Redis本质上只有一台Master作为存储，如果面对海量数据的存储，一台Redis的服务器就应付不过来了，而且数据量太大意味着持久化成本高，严重时可能会阻塞服务器，造成服务请求成功率下降，降低服务的稳定性。

针对以上的问题，Redis集群提供了较为完善的方案，解决了存储能力受到单机限制，写操作无法负载均衡的问题。


Redis提供了去中心化的**集群部署**模式，集群内所有Redis节点之间两两连接，而很多的客户端工具会根据key将请求分发到对应的分片下的某一个节点上进行处理。Redis也内置了高可用机制，支持N个master节点，每个master节点都可以挂载多个slave节点，当master节点挂掉时，集群会提升它的某个slave节点作为新的master节点。

默认情况下，redis集群的读和写都是到master上去执行的，不支持slave节点读和写，跟Redis主从复制下读写分离不一样，因为redis集群的核心的理念，主要是使用slave做数据的热备，以及master故障时的主备切换，实现高可用的。Redis的读写分离，是为了横向任意扩展slave节点去支撑更大的读吞吐量。而redis集群架构下，本身master就是可以任意扩展的，如果想要支撑更大的读或写的吞吐量，都可以直接对master进行横向扩展。



一个典型的Redis集群部署场景如下图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202411301318769.png)

在Redis集群里面，又会划分出`分区`的概念，一个集群中可有多个分区。分区有几个特点：

1. 同一个分区内的Redis节点之间的数据完全一样，多个节点保证了数据有多份副本冗余保存，且可以提供高可用保障。
2. 不同分片之间的数据不相同。
3. 通过水平增加多个分片的方式，可以实现整体集群的容量的扩展。

按照Cluster模式进行部署的时候，要求最少需要部署`6个`Redis节点（3个分片，每个分片中1主1从），其中集群中每个分片的master节点负责对外提供读写操作，slave节点则作为故障转移使用（master出现故障的时候充当新的master）、对外提供只读请求处理。



## 集群数据分布策略

### Redis Sharding（数据分片）

在`Redis Cluster`前，为了解决数据分发到各个分区的问题，普遍采用的是`Redis Sharding`（数据分片）方案。所谓的Sharding，其实就是一种数据分发的策略。根据key的hash值进行取模，确定最终归属的节点。

优点就是比较简单，但是

- 扩容或者摘除节点时需要重新根据映射关系计算，会导致数据重新迁移。

- 集群扩容的时候，会导致请求被分发到错误节点上，导致缓存*命中率降低*。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202411301318343.png)

如果需要解决这个问题，就需要对原先扩容前已经存储的数据重新进行一次hash计算和取模操作，将全部的数据重新分发到新的正确节点上进行存储。这个操作被称为`重新Sharding`，重新sharding期间服务不可用，可能会对业务造成影响。



### 一致性Hash

为了降低节点的增加或者移除对于整体已有缓存数据访问的影响，最大限度的保证缓存命中率，改良后的[`一致性Hash`](https://www.seven97.top/microservices/protocol/consistencyhash.html)算法浮出水面。



### Hash槽

何为Hash槽？Hash槽的原理与HashMap有点相似，Redis集群中有16384个哈希槽（槽的范围是 0 -16383，哈希槽），将不同的哈希槽分布在不同的Redis节点上面进行管理，也就是说每个Redis节点只负责一部分的哈希槽。在对数据进行操作的时候，集群会对使用CRC16算法对key进行计算并对16384取模（slot = CRC16(key)%16383），得到的结果就是 Key-Value 所放入的槽，通过这个值，去找到对应的槽所对应的Redis节点，然后直接到这个对应的节点上进行存取操作。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202411301318133.png)

使用哈希槽的好处就在于可以方便的添加或者移除节点，并且无论是添加删除或者修改某一个节点，都不会造成集群不可用的状态。

- 当需要增加节点时，只需要把其他节点的某些哈希槽挪到新节点就可以了；
- 当需要移除节点时，只需要把移除节点上的哈希槽挪到其他节点就行了；



哈希槽数据分区算法具有以下几种特点：

- 解耦数据和节点之间的关系，简化了扩容和收缩难度；
- 节点自身维护槽的映射关系，不需要客户端代理服务维护槽分区元数据
- 支持节点、槽、键之间的映射查询，用于数据路由，在线伸缩等场景



**为什么redis集群采用“hash槽”来解决数据分配问题，而不采用“一致性hash”算法呢？**


- 一致性哈希的节点分布基于圆环，**无法很好的手动控制数据分布**，比如有些节点的硬件差，希望少存一点数据，这种很难操作（还得通过虚拟节点映射，总之较繁琐）。
- 而redis集群的槽位空间是可以用户手动自定义分配的，类似于 windows 盘分区的概念，可以手动控制大小。
- 其实，无论是 “一致性哈希” 还是 “hash槽” 的方式，在增减节点的时候，都会对一部分数据产生影响，都需要我们迁移数据。当然，**redis集群也提供了相关手动迁移槽数据的命令。**



## Hash槽原理详解

### clusterNode

保存节点的当前状态，比如节点的创建时间，节点的名字，节点当前的配置纪元，节点的IP和地址，等等。

```c
typedef struct clusterNode {
    //创建节点的时间
    mstime_t ctime;
    //节点的名字，由40个16进制字符组成
    char name[CLUSTER_NAMELEN];
    //节点标识 使用各种不同的标识值记录节点的角色(比如主节点或者从节点)
    char shard_id[CLUSTER_NAMELEN]; 
    //以及节点目前所处的状态(比如在线或者下线)
    int flags;     
    //节点当前的配置纪元，用于实现故障转移
    uint64_t configEpoch; 
    //由这个节点负责处理的槽
	//一共有REDISCLUSTER SLOTS/8个字节长 每个字节的每个位记录了一个槽的保存状态
    // 位的值为 1 表示槽正由本节点处理，值为0则表示槽并非本节点处理
    unsigned char slots[CLUSTER_SLOTS/8];
    uint16_t *slot_info_pairs; /* Slots info represented as (start/end) pair (consecutive index). */
    int slot_info_pairs_count; /* Used number of slots in slot_info_pairs */
    //该节点负责处理的槽数里
    int numslots; 
    //如果本节点是主节点，那么用这个属性记录从节点的数里
    int numslaves;  
    //指针数组，指向各个从节点
    struct clusterNode **slaves; 
    //如果这是一个从节点，那么指向主节点
    struct clusterNode *slaveof; 
    
    unsigned long long last_in_ping_gossip; /* The number of the last carried in the ping gossip section */
    //最后一次发送 PING 命令的时间
    mstime_t ping_sent;   
    //最后一次接收 PONG 回复的时间戳
    mstime_t pong_received;  
    mstime_t data_received;  
    //最后一次被设置为 FAIL 状态的时间
    mstime_t fail_time;     
    // 最后一次给某个从节点投票的时间
    mstime_t voted_time;     
    //最后一次从这个节点接收到复制偏移里的时间
    mstime_t repl_offset_time; 
    mstime_t orphaned_time;     
    //这个节点的复制偏移里
    long long repl_offset;     
    //节点的 IP 地址
    char ip[NET_IP_STR_LEN];   
    sds hostname;               /* The known hostname for this node */
    //节点的端口号
    int port;                   
    int pport;                  /* Latest known clients plaintext port. Only used
                                   if the main clients port is for TLS. */
    int cport;                  /* Latest known cluster port of this node. */
    //保存连接节点所需的有关信息
    clusterLink *link;          
    clusterLink *inbound_link;  /* TCP/IP link accepted from this node */
    //链表，记录了所有其他节点对该节点的下线报告
    list *fail_reports;         
} clusterNode;
```



### clusterState

记录当前节点所认为的集群目前所处的状态。

```c
typedef struct clusterState {
    //指向当前节点的指针
    clusterNode *myself;  
    //集群当前的配置纪元，用于实现故障转移
    uint64_t currentEpoch;
    // 集群当前的状态:是在线还是下线
    int state;            
    //集群中至少处理着一个槽的节点的数量。
    int size;      
    //集群节点名单(包括 myself 节点)
	//字典的键为节点的名字，字典的值为 clusterWode 结构
    dict *nodes;         
    dict *shards;         /* Hash table of shard_id -> list (of nodes) structures */
    //节点黑名单，用于CLUSTER FORGET 命令
	//防止被 FORGET 的命令重新被添加到集群里面
    dict *nodes_black_list; 
    //记录要从当前节点迁移到目标节点的槽，以及迁移的目标节点
    //migrating_slots_to[i]= NULL 表示槽 i 未被迁移
    //migrating_slots_to[i]= clusterNode_A 表示槽i要从本节点迁移至节点 A
    clusterNode *migrating_slots_to[CLUSTER_SLOTS];
    //记录要从源节点迁移到本节点的槽，以及进行迁移的源节点
    //importing_slots_from[i]= NULL 表示槽 i 未进行导入
    //importing_slots from[i]=clusterNode A 表示正从节点A中导入槽 i
    clusterNode *importing_slots_from[CLUSTER_SLOTS];
    //负责处理各个槽的节点
	//例如 slots[i]=clusterNode_A  表示槽i由节点A处理
    clusterNode *slots[CLUSTER_SLOTS];
    rax *slots_to_channels;
    
   //以下这些值被用于进行故障转移选举
	//上次执行选举或者下次执行选举的时间
    mstime_t failover_auth_time; 
    //节点获得的投票数量
    int failover_auth_count;    
    //如果值为 1，表示本节点已经向其他节点发送了投票请求
    int failover_auth_sent;     
    int failover_auth_rank;     /* This slave rank for current auth request. */
    uint64_t failover_auth_epoch; /* Epoch of the current election. */
    int cant_failover_reason;   /* Why a slave is currently not able to
                                   failover. See the CANT_FAILOVER_* macros. */
    
    /*共用的手动故障转移状态*/
	//手动故障转移执行的时间限制
    mstime_t mf_end;            
    /*主服务器的手动故障转移状态 */
    clusterNode *mf_slave;      
    /*丛服务器的手动故障转移状态 */
    long long mf_master_offset; 
    // 指示手动故障转移是否可以开始的标志值 值为非 0 时表示各个主服务器可以开始投票
    int mf_can_start;    
    
    /*以下这些值由主服务器使用，用于记录选举时的状态*/
    //集群最后一次进行投票的纪元
    uint64_t lastVoteEpoch;  
    //在进入下个事件循环之前要做的事情，以各个 flag 来记录
    int todo_before_sleep;
    /* Stats */
    //通过 cluster 连接发送的消息数量
    long long stats_bus_messages_sent[CLUSTERMSG_TYPE_COUNT];
    //通过cluster 接收到的消息数量
    long long stats_bus_messages_received[CLUSTERMSG_TYPE_COUNT];
    long long stats_pfail_nodes;    /* Number of nodes in PFAIL status,
                                       excluding nodes without address. */
    unsigned long long stat_cluster_links_buffer_limit_exceeded;  /* Total number of cluster links freed due to exceeding buffer limit */
} clusterState;
```



### 节点的槽指派信息

clusterNode数据结构的slots属性和numslot属性记录了节点负责处理那些槽：

slots属性是一个二进制位数组(bit array)，这个数组的长度为16384/8=2048个字节，共包含16384个二进制位。Master节点用bit来标识对于某个槽自己是否拥有，时间复杂度为O(1)



### 集群所有槽的指派信息

当收到集群中其他节点发送的信息时，通过将节点槽的指派信息保存在本地的clusterState.slots数组里面，程序要检查槽i是否已经被指派，又或者取得负责处理槽i的节点，只需要访问clusterState.slots[i]的值即可，时间复杂度仅为O(1)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202411301317804.png)

如上图所示，ClusterState 中保存的 Slots 数组中每个下标对应一个槽，每个槽信息中对应一个 clusterNode 也就是缓存的节点。这些节点会对应一个实际存在的 Redis 缓存服务，包括 IP 和 Port 的信息。Redis Cluster 的通讯机制实际上保证了每个节点都有其他节点和槽数据的对应关系。无论Redis 的客户端访问集群中的哪个节点都可以路由到对应的节点上，因为每个节点都有一份 ClusterState，它记录了所有槽和节点的对应关系。



## 集群的请求重定向

Redis集群在客户端层面是没有采用代理的，并且无论Redis 的客户端访问集群中的哪个节点都可以路由到对应的节点上，下面来看看 Redis 客户端是如何通过路由来调用缓存节点的：

### MOVED请求

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406152047331.png)

如上图所示，Redis 客户端通过 CRC16(key)%16383 计算出 Slot 的值，发现需要找“缓存节点1”进行数据操作，但是由于缓存数据迁移或者其他原因导致这个对应的 Slot 的数据被迁移到了“缓存节点2”上面。那么这个时候 Redis 客户端就无法从“缓存节点1”中获取数据了。但是由于“缓存节点1”中保存了所有集群中缓存节点的信息，因此它知道这个 Slot 的数据在“缓存节点2”中保存，因此向 Redis 客户端发送了一个 MOVED 的重定向请求。这个请求告诉其应该访问的“缓存节点2”的地址。Redis 客户端拿到这个地址，继续访问“缓存节点2”并且拿到数据。



### ASK请求

上面的例子说明了，数据 Slot 从“缓存节点1”已经迁移到“缓存节点2”了，那么客户端可以直接找“缓存节点2”要数据。那么如果两个缓存节点正在做节点的数据迁移，此时客户端请求会如何处理呢？

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406152047441.png)

Redis 客户端向“缓存节点1”发出请求，此时“缓存节点1”正向“缓存节点 2”迁移数据，如果没有命中对应的 Slot，它会返回客户端一个 ASK 重定向请求并且告诉“缓存节点2”的地址。客户端向“缓存节点2”发送 Asking 命令，询问需要的数据是否在“缓存节点2”上，“缓存节点2”接到消息以后返回数据是否存在的结果。



### 频繁重定向造成的网络开销的处理：smart客户端

#### 什么是 smart客户端：

在大部分情况下，可能都会出现一次请求重定向才能找到正确的节点，这个重定向过程显然会增加集群的网络负担和单次请求耗时。所以大部分的客户端都是smart的。所谓 smart客户端，就是指客户端本地维护一份hashslot => node的映射表缓存，大部分情况下，直接走本地缓存就可以找到hashslot => node，不需要通过节点进行moved重定向，



####  JedisCluster的工作原理：

- 在JedisCluster初始化的时候，就会随机选择一个node，初始化hashslot => node映射表，同时为每个节点创建一个JedisPool连接池。
- 每次基于JedisCluster执行操作时，首先会在本地计算key的hashslot，然后在本地映射表找到对应的节点node。
- 如果那个node正好还是持有那个hashslot，那么就ok；如果进行了reshard操作，可能hashslot已经不在那个node上了，就会返回moved。
- 如果JedisCluter API发现对应的节点返回moved，那么利用该节点返回的元数据，更新本地的hashslot => node映射表缓存
- 重复上面几个步骤，直到找到对应的节点，如果重试超过5次，那么就报错，JedisClusterMaxRedirectionException



#### hashslot迁移和ask重定向：

如果hashslot正在迁移，那么会返回ask重定向给客户端。客户端接收到ask重定向之后，会重新定位到目标节点去执行，但是因为ask发生在hashslot迁移过程中，所以JedisCluster API收到ask是不会更新hashslot本地缓存。

虽然ASK与MOVED都是对客户端的重定向控制，但是有本质区别。ASK重定向说明集群正在进行slot数据迁移，客户端无法知道迁移什么时候完成，因此只能是临时性的重定向，客户端不会更新slots缓存。但是MOVED重定向说明键对应的槽已经明确指定到新的节点，客户端需要更新slots缓存。



## Redis集群中节点的通信机制：goosip协议

redis集群的哈希槽算法解决的是数据的存取问题，不同的哈希槽位于不同的节点上，而不同的节点维护着一份它所认为的当前集群的状态，同时，Redis集群是去中心化的架构。那么，当集群的状态发生变化时，比如新节点加入、slot迁移、节点宕机、slave提升为新Master等等，我们希望这些变化尽快被其他节点发现，Redis是如何进行处理的呢？也就是说，Redis不同节点之间是如何进行通信进行维护集群的同步状态呢？

在Redis集群中，不同的节点之间采用[gossip协议](https://www.seven97.top/microservices/protocol/gossip-detail.html)进行通信，节点之间通讯的目的是为了维护节点之间的元数据信息。这些元数据就是每个节点包含哪些数据，是否出现故障，通过gossip协议，达到最终数据的一致性。

>  gossip协议，是基于流行病传播方式的节点或者进程之间信息交换的协议。原理就是在不同的节点间不断地通信交换信息，一段时间后，所有的节点就都有了整个集群的完整信息，并且所有节点的状态都会达成一致。每个节点可能知道所有其他节点，也可能仅知道几个邻居节点，但只要这些节可以通过网络连通，最终他们的状态就会是一致的。Gossip协议最大的好处在于，即使集群节点的数量增加，每个节点的负载也不会增加很多，几乎是恒定的。 



Redis集群中节点的通信过程如下：

- 集群中每个节点都会单独开一个TCP通道，用于节点间彼此通信。
- 每个节点在固定周期内通过待定的规则选择几个节点发送ping消息
- 接收到ping消息的节点用pong消息作为响应

使用gossip协议的优点在于将元数据的更新分散在不同的节点上面，降低了压力；但是缺点就是元数据的更新有延时，可能导致集群中的一些操作会有一些滞后。另外，由于 gossip 协议对服务器时间的要求较高，时间戳不准确会影响节点判断消息的有效性。而且节点数量增多后的网络开销也会对服务器产生压力，同时结点数太多，意味着达到最终一致性的时间也相对变长，因此官方推荐最大节点数为1000左右。



redis cluster架构下的每个redis都要开放两个端口号，比如一个是6379，另一个就是加1w的端口号16379。 

- 6379端口号就是redis服务器入口。
- 16379端口号是用来进行节点间通信的，也就是 cluster bus 的东西，cluster bus 的通信，用来进行故障检测、配置更新、故障转移授权。cluster bus 用的就是gossip 协议



## 集群的扩容与收缩

作为分布式部署的缓存节点总会遇到缓存扩容和缓存故障的问题。这就会导致缓存节点的上线和下线的问题。由于每个节点中保存着槽数据，因此当缓存节点数出现变动时，这些槽数据会根据对应的虚拟槽算法被迁移到其他的缓存节点上。所以对于redis集群，集群伸缩主要在于槽和数据在节点之间移动。



### 扩容

1. 启动新节点
2. 使用cluster meet命令将新节点加入到集群
3. 迁移槽和数据：添加新节点后，需要将一些槽和数据从旧节点迁移到新节点

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406152125451.png)

如上图所示，集群中本来存在“缓存节点1”和“缓存节点2”，此时“缓存节点3”上线了并且加入到集群中。此时根据虚拟槽的算法，“缓存节点1”和“缓存节点2”中对应槽的数据会应该新节点的加入被迁移到“缓存节点3”上面。

新节点加入到集群的时候，作为孤儿节点是没有和其他节点进行通讯的。因此需要在集群中任意节点执行 cluster meet 命令让新节点加入进来。假设新节点是 192.168.1.1 5002，老节点是 192.168.1.1 5003，那么运行以下命令将新节点加入到集群中。

>  192.168.1.1 5003> cluster meet 192.168.1.1 5002 

这个是由老节点发起的，有点老成员欢迎新成员加入的意思。新节点刚刚建立没有建立槽对应的数据，也就是说没有缓存任何数据。如果这个节点是主节点，需要对其进行槽数据的扩容；如果这个节点是从节点，就需要同步主节点上的数据。总之就是要同步数据。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406152125460.png)

如上图所示，由客户端发起节点之间的槽数据迁移，数据从源节点往目标节点迁移。

1. 客户端对目标节点发起准备导入槽数据的命令，让目标节点准备好导入槽数据。这里使用 cluster setslot {slot} importing {sourceNodeId} 命令。
2. 之后对源节点发起送命令，让源节点准备迁出对应的槽数据。使用命令 cluster setslot {slot} importing {sourceNodeId}。
3. 此时源节点准备迁移数据了，在迁移之前把要迁移的数据获取出来。通过命令 cluster getkeysinslot {slot} {count}。Count 表示迁移的 Slot 的个数。
4. 然后在源节点上执行，migrate {targetIP} {targetPort} “” 0 {timeout} keys {keys} 命令，把获取的键通过流水线批量迁移到目标节点。
5. 重复 3 和 4 两步不断将数据迁移到目标节点。
6. 完成数据迁移到目标节点以后，通过 cluster setslot {slot} node {targetNodeId} 命令通知对应的槽被分配到目标节点，并且广播这个信息给全网的其他主节点，更新自身的槽节点对应表。



### 收缩

1. 迁移槽。
2. 忘记节点。通过命令 cluster forget {downNodeId} 通知其他的节点

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406152125478.png)

为了安全删除节点，Redis集群只能下线没有负责槽的节点。因此如果要下线有负责槽的master节点，则需要先将它负责的槽迁移到其他节点。迁移的过程也与上线操作类似，不同的是下线的时候需要通知全网的其他节点忘记自己，此时通过命令 `cluster forget {downNodeId}` 通知其他的节点。



## 集群的故障检测与故障转恢复机制：

### 集群的故障检测

Redis集群的故障检测是基于gossip协议的，集群中的每个节点都会定期地向集群中的其他节点发送PING消息，以此交换各个节点状态信息，检测各个节点状态：在线状态、疑似下线状态PFAIL、已下线状态FAIL。

#### 主观下线（pfail）

当节点A检测到与节点B的通讯时间超过了cluster-node-timeout 的时候，就会更新本地节点状态，把节点B更新为主观下线。

主观下线并不能代表某个节点真的下线了，有可能是节点A与节点B之间的网络断开了，但是其他的节点依旧可以和节点B进行通讯。 

#### 客观下线

由于集群内的节点会不断地与其他节点进行通讯，下线信息也会通过 Gossip 消息传遍所有节点，因此集群内的节点会不断收到下线报告。

当半数以上的主节点标记了节点B是主观下线时，便会触发客观下线的流程（该流程只针对主节点，如果是从节点就会忽略）。将主观下线的报告保存到本地的 ClusterNode 的结构fail_reports链表中，并且对主观下线报告的时效性进行检查，如果超过 cluster-node-timeout*2 的时间，就忽略这个报告，否则就记录报告内容，将其标记为客观下线。

接着向集群广播一条主节点B的Fail 消息，所有收到消息的节点都会标记节点B为客观下线。



### 集群地故障恢复

当故障节点下线后，如果是持有槽的主节点则需要在其从节点中找出一个替换它，从而保证高可用。此时下线主节点的所有从节点都担负着恢复义务，这些从节点会定时监测主节点是否进入客观下线状态，如果是，则触发故障恢复流程。故障恢复也就是选举一个节点充当新的master，选举的过程是基于[Raft协议](https://www.seven97.top/microservices/protocol/raft-detail.html)选举方式来实现的。

1. 从节点过滤：检查每个slave节点与master节点断开连接的时间，如果超过了cluster-node-timeout * cluster-slave-validity-factor，那么就没有资格切换成master

2. 投票选举：

   1. 节点排序：对通过过滤条件的所有从节点进行排序，按照priority、offset、run id排序，排序越靠前的节点，越优先进行选举。 (在这步排序领先的从节点通常会获得更多的票，因为它触发选举的时间更早一些，获得票的机会更大)

      - priority的值越低，优先级越高

      - offset越大，表示从master节点复制的数据越多，选举时间越靠前，优先进行选举

      - 如果offset相同，run id越小，优先级越高

   2. 更新配置纪元：每个主节点会去更新配置纪元（clusterNode.configEpoch），这个值是不断增加的整数。这个值记录了每个节点的版本和整个集群的版本。每当发生重要事情的时候（例如：出现新节点，从节点精选）都会增加全局的配置纪元并且赋给相关的主节点，用来记录这个事件。更新这个值目的是，保证所有主节点对这件“大事”保持一致，大家都统一成一个配置纪元，表示大家都知道这个“大事”了。

   3. 发起选举：更新完配置纪元以后，从节点会向集群发起广播选举的消息（`CLUSTERMSG_TYPE_FAILOVER_AUTH_REQUEST`），要求所有收到这条消息，并且具有投票权的主节点进行投票。每个从节点在一个纪元中只能发起一次选举。

   4. 选举投票：如果一个主节点具有投票权，并且这个主节点尚未投票给其他从节点，那么主节点将向要求投票的从节点返回一条`CLUSTERMSG_TYPE_FAILOVER_AUTH_ACK`消息，表示这个主节点支持从节点成为新的主节点。每个参与选举的从节点都会接收`CLUSTERMSG_TYPE_FAILOVER_AUTH_ACK`消息，并根据自己收到了多少条这种消息来统计自己获得了多少主节点的支持。如果超过(N/2 + 1)数量的master节点都投票给了某个从节点，那么选举通过，这个从节点可以切换成master，如果在 cluster-node-timeout*2 的时间内从节点没有获得足够数量的票数，本次选举作废，更新配置纪元，并进行第二轮选举，直到选出新的主节点为止。

3. 替换主节点：当满足投票条件的从节点被选出来以后，会触发替换主节点的操作。删除原主节点负责的槽数据，把这些槽数据添加到自己节点上，并且广播让其他的节点都知道这件事情，新的主节点诞生了。
   1. 被选中的从节点执行`SLAVEOF NO ONE`命令，使其成为新的主节点
   2. 新的主节点会撤销所有对已下线主节点的槽指派，并将这些槽全部指派给自己
   3. 新的主节点对集群进行广播PONG消息，告知其他节点已经成为新的主节点
   4. 的主节点开始接收和处理槽相关的请求

>  备注：如果集群中某个节点的master和slave节点都宕机了，那么集群就会进入fail状态，因为集群的slot映射不完整。如果集群超过半数以上的master挂掉，无论是否有slave，集群都会进入fail状态。



## Redis集群的运维

### 数据迁移问题

Redis集群可以进行节点的动态扩容缩容，这一过程目前还处于半自动状态，需要人工介入。在扩缩容的时候，需要进行数据迁移。而 Redis为了保证迁移的一致性，迁移所有操作都是同步操作，执行迁移时，两端的 Redis均会进入时长不等的阻塞状态，对于小Key，该时间可以忽略不计，但如果一旦Key的内存使用过大，严重的时候会接触发集群内的故障转移，造成不必要的切换。



### 带宽消耗问题

Redis集群是无中心节点的集群架构，依靠Gossip协议协同自动化修复集群的状态，但goosip有消息延时和消息冗余的问题，在集群节点数量过多的时候，goosip协议通信会消耗大量的带宽，主要体现在以下几个方面：

- 消息发送频率：跟cluster-node-timeout密切相关，当节点发现与其他节点的最后通信时间超过 cluster-node-timeout/2时会直接发送ping消息
- 消息数据量：每个消息主要的数据占用包含：slots槽数组（2kb）和整个集群1/10的状态数据
- 节点部署的机器规模：机器的带宽上限是固定的，因此相同规模的集群分布的机器越多，每台机器划分的节点越均匀，则整个集群内整体的可用带宽越高

也就是说，每个节点的slot不能有太多，否则集群节点之间互相通信时，redis会有大量的时间和带宽在完成通信

集群带宽消耗主要分为：读写命令消耗+Gossip消息消耗，因此搭建Redis集群需要根据业务数据规模和消息通信成本做出合理规划：

- 在满足业务需求的情况下尽量避免大集群，同一个系统可以针对不同业务场景拆分使用若干个集群。
- 适度提供cluster-node-timeout降低消息发送频率，但是cluster-node-timeout还影响故障转移的速度，因此需要根据自身业务场景兼顾二者平衡
- 如果条件允许尽量均匀部署在更多机器上，避免集中部署。如果有60个节点的集群部署在3台机器上每台20个节点，这是机器的带宽消耗将非常严重



### Pub/Sub广播问题

集群模式下内部对所有publish命令都会向所有节点进行广播，加重带宽负担，所以集群应该避免频繁使用Pub/sub功能



### 集群倾斜

集群倾斜是指不同节点之间数据量和请求量出现明显差异，这种情况将加大负载均衡和开发运维的难度。因此需要理解集群倾斜的原因

1. 数据倾斜：

   - 节点和槽分配不均

   - 不同槽对应键数量差异过大

   - 集合对象包含大量元素

   - 内存相关配置不一致

2. 请求倾斜：合理设计键，热点大集合对象做拆分或者使用hmget代替hgetall避免整体读取



### 集群读写分离

集群模式下读写分离成本比较高，直接扩展主节点数量来提高集群性能是更好的选择。




<!-- @include: @article-footer.snippet.md -->     
