---
title: ZooKeeper - 集群机制
category: 微服务
tag:
 - ZooKeeper
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,ZooKeeper,ZooKeeper的集群机制
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---



## 介绍

在ZooKeeper集群服中务中有三个角色：

1. Leader领导者 ：    

   1. 处理事务请求(即读和写服务)

   2. 集群内部各服务器的调度者

2. Follower跟随者：

   1. 处理客户端非事务请求(即读服务)，转发事务请求给Leader服务器

   2. 参与Leader选举投票

3. Observer观察者：

   1. 处理客户端非事务请求(即读服务)，转发事务请求给Leader服务器

   2. 不参与Leader选举投票

   3. 不参与“过半写成功”策略

 

Leader选举：

- Serverid：服务器ID。编号越大权重越大

- Zxid：数据ID。值越大说明数据越新，权重越大

- 选举过程中，超过半数选票就成为Leader

## ZK集群 Leader 选举机制

为什么要进行Leader选举？

Leader 主要作用是保证分布式数据一致性，即每个节点的存储的数据同步。遇到以下两种情况需要进行Leader选举

- 服务器初始化启动

- 服务器运行期间无法和Leader保持连接，Leader节点崩溃，逻辑时钟崩溃。

 

ZooKeeper 集群中的服务器状态有下面几种：

- LOOKING：寻找 Leader。

- LEADING：Leader 状态，对应的节点为 Leader。

- FOLLOWING：Follower 状态，对应的节点为 Follower。

- OBSERVING：Observer 状态，对应节点为 Observer，该节点不参与 Leader 选举。

选举过程的状态变化：

1. Leader election（选举阶段）：节点在一开始都处于选举阶段，只要有一个节点得到超半数节点的票数，它就可以当选准 leader。
2. Discovery（发现阶段）：在这个阶段，followers 跟准 leader 进行通信，同步 followers 最近接收的事务提议。
3. Synchronization（同步阶段） :同步阶段主要是利用 leader 前一阶段获得的最新提议历史，同步集群中所有的副本。同步完成之后准 leader 才会成为真正的 leader。
4. Broadcast（广播阶段） :到了这个阶段，ZooKeeper 集群才能正式对外提供事务服务，并且 leader 可以进行消息广播。同时如果有新的节点加入，还需要对新节点进行同步。

 

### 服务器初始化时Leader选举

Zookeeper由于其自身的性质，一般建议选取奇数个节点进行搭建分布式服务器集群。以3个节点组成的服务器集群为例，说明服务器初始化时的选举过程。启动第一台安装Zookeeper的节点时，无法单独进行选举，启动第二台时，两节点之间进行通信，开始选举Leader。

1. 每个Server投出一票。他们两都选自己为Leader，投票的内容为（SID，ZXID）。

   1.  SID即Server的id，安装zookeeper时配置文件中所配置的myid；ZXID，事务id，为节点的更新程度，ZXID越大，代表Server对Znode的操作越新。

   2.  由于服务器初始化，每个Sever上的Znode为0，所以Server1投的票为（1,0），Server2为（2,0）。

2. 两Server将各自投票发给集群中其他机器。

3. 每个Server接收来自其他Server的投票。集群中的每个Server先判断投票有效性，如检查是不是本轮的投票，是不是来自Looking状态的服务器投的票。

4. 对投票结果进行处理。先了解下处理规则

   1. 首先对比ZXID。ZXID大的服务器优先作为Leader

   2. 若ZXID相同，比如初始化的时候，每个Server的ZXID都为0，

   3. 就会比较myid，myid大的选出来做Leader。

   4. 对于Server1而言，他接受到的投票为（2,0），因为自身的票为（1,0），所以此时它会选举Server2为Leader，将自己的更新为（2,0）。而Server2收到的投票为Server1的（1,0）由于比他自己小，Server2的投票不变。Server1和Server2再次将票投出，投出的票都为（2,0）。

5. 统计投票。每次投票之后，服务器都会统计投票信息，如果判定某个Server有过半的票数投它，那么该Server将会作为Leader。对于Server1和Server2而言,统计出已经有两台机器接收了（2,0）的投票信息，此时认为选出了Leader。
6. 改变服务器状态。当确定了Leader之后，每个Server更新自己的状态，
   1. Leader将状态更新为Leading，Follower将状态更新为Following。

 

### 服务器运行期间的Leader选举

Zookeeper运行期间，如果有新的Server加入，或者非Leader的Server宕机，那么Leader将会同步数据到新Server或者寻找其他备用Server替代宕机的Server。若Leader宕机，此时集群暂停对外服务，开始在内部选举新的Leader。假设当前集群中有Server1、Server2、Server3三台服务器，Server2为当前集群的Leader，由于意外情况，Server2宕机了，便开始进入选举状态。过程如下

1. 变更状态。其他的非Observer服务器将自己的状态改变为Looking，开始进入Leader选举。
2. 每个Server发出一个投票（myid，ZXID），由于此集群已经运行过，所以每个Server上的ZXID可能不同。
   1. 假设Server1的ZXID为145，Server3的为122，第一轮投票中，Server1和Server3都投自己，票分别为（1，145）、（3,122）,将自己的票发送给集群中所有机器。

3. 每个Server接收接收来自其他Server的投票，接下来的步骤与初始化时相同。

 

## 注意点

### ZK 集群为啥最好奇数台？

ZK 集群在宕掉几个ZK 服务器之后，如果剩下的 ZooKeeper 服务器个数大于宕掉的个数的话整个 ZooKeeper 才依然可用。假如我们的集群中有 n 台 ZooKeeper 服务器，那么也就是剩下的服务数必须大于 n/2。先说一下结论，2n 和 2n-1 的容忍度是一样的，都是 n-1，大家可以先自己仔细想一想，这应该是一个很简单的数学问题了。

比如假如我们有 3 台，那么最大允许宕掉 1 台 ZooKeeper 服务器，如果我们有 4 台的的时候也同样只允许宕掉 1 台。

 假如我们有 5 台，那么最大允许宕掉 2 台 ZooKeeper 服务器，如果我们有 6 台的的时候也同样只允许宕掉 2 台。

综上，何必增加那一个不必要的 ZooKeeper 呢？

### ZK 选举的过半机制防止脑裂

**何为集群脑裂？**

对于一个集群，通常多台机器会部署在不同机房，来提高这个集群的可用性。保证可用性的同时，会发生一种机房间网络线路故障，导致机房间网络不通，而集群被割裂成几个小集群。这时候子集群各自选主导致“脑裂”的情况。

举例说明：比如现在有一个由 6 台服务器所组成的一个集群，部署在了 2 个机房，每个机房 3 台。正常情况下只有 1 个 leader，但是当两个机房中间网络断开的时候，每个机房的 3 台服务器都会认为另一个机房的 3 台服务器下线，而选出自己的 leader 并对外提供服务。若没有过半机制，当网络恢复的时候会发现有 2 个 leader。仿佛是 1 个大脑（leader）分散成了 2 个大脑，这就发生了脑裂现象。脑裂期间 2 个大脑都可能对外提供了服务，这将会带来数据一致性等问题。



**过半机制是如何防止脑裂现象产生的？**

ZooKeeper 的过半机制导致不可能产生 2 个 leader，因为少于等于一半是不可能产生 leader 的，这就使得不论机房的机器如何分配都不可能发生脑裂。

 

所以为什么最好奇数台？

假设我们现在有四个，挂了一个也能工作，**但是挂了两个也不能正常工作了**，这是和三个一样的，而三个比四个还少一个，带来的效益是一样的，所以 Zookeeper 推荐奇数个 server 。



## 总结

1. ZooKeeper 本身就是一个分布式程序（只要半数以上节点存活，ZooKeeper 就能正常服务）。
2. 为了保证高可用，最好是以集群形态来部署 ZooKeeper，这样只要集群中大部分机器是可用的（能够容忍一定的机器故障），那么 ZooKeeper 本身仍然是可用的。
3. ZooKeeper 将数据保存在内存中，这也就保证了 高吞吐量和低延迟（但是内存限制了能够存储的容量不太大，此限制也是保持 znode 中存储的数据量较小的进一步原因）。
4. ZooKeeper 是高性能的。 在“读”多于“写”的应用程序中尤其地明显，因为“写”会导致所有的服务器间同步状态。（“读”多于“写”是协调服务的典型场景。）
5. ZooKeeper 有临时节点的概念。 当创建临时节点的客户端会话一直保持活动，瞬时节点就一直存在。而当会话终结时，瞬时节点被删除。持久节点是指一旦这个 znode 被创建了，除非主动进行 znode 的移除操作，否则这个 znode 将一直保存在 ZooKeeper 上。
6. ZooKeeper 底层其实只提供了两个功能：① 管理（存储、读取）用户程序提交的数据；② 为用户程序提供数据节点监听服务。



 

 

 <!-- @include: @article-footer.snippet.md -->     

 