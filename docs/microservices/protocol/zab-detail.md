---
title: 解密ZAB协议：Zookeeper一致性的核心实现
category: 微服务
tags:
  - 理论-算法
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,共识算法,ZAB协议
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---



## 一致性问题

设计一个分布式系统必定会遇到一个问题—— **因为分区容忍性（partition tolerance）的存在，就必定要求我们需要在系统可用性（availability）和数据一致性（consistency）中做出权衡** 。这就是著名的 CAP 定理。

ZooKeeper 的处理方式，保证了 CP（数据一致性）

 

### 一致性协议和算法

而为了解决数据一致性问题，在科学家和程序员的不断探索中，就出现了很多的一致性协议和算法。比如 2PC（两阶段提交），3PC（三阶段提交），Paxos 算法等等。

**拜占庭将军问题** 。它意指 **在不可靠信道上试图通过消息传递的方式达到一致性是不可能的**， 所以所有的一致性算法的 **必要前提** 就是安全可靠的消息通道。

而为什么要去解决数据一致性的问题？想想，如果一个秒杀系统将服务拆分成了下订单和加积分服务，这两个服务部署在不同的机器上了，万一在消息的传播过程中积分系统宕机了，总不能你这边下了订单却没加积分吧？是需要保证两边的数据一致的

### 2PC（两阶段提交）

两阶段提交是一种保证分布式系统数据一致性的协议，现在很多数据库都是采用的两阶段提交协议来完成 **分布式事务** 的处理。

所需要解决的是在分布式系统中，整个调用链中，所有服务的数据处理要么都成功要么都失败，即所有服务的 **原子性问题** 。

 

在两阶段提交中，主要涉及到两个角色，分别是协调者和参与者。

1. 第一阶段：当要执行一个分布式事务的时候，事务发起者首先向协调者发起事务请求，然后协调者会给所有参与者发送 prepare 请求（其中包括事务内容）告诉参与者你们需要执行事务了，如果能执行我发的事务内容那么就先执行但不提交，执行后请给我回复。然后参与者收到 prepare 消息后，他们会开始执行事务（但不提交），并将 Undo 和 Redo 信息记入事务日志中，之后参与者就向协调者反馈是否准备好了。
2. 第二阶段：第二阶段主要是协调者根据参与者反馈的情况来决定接下来是否可以进行事务的提交操作，即提交事务或者回滚事务。

比如这个时候 **所有的参与者** 都返回了准备好了的消息，这个时候就进行事务的提交，协调者此时会给所有的参与者发送 **Commit 请求** ，当参与者收到 Commit 请求的时候会执行前面执行的事务的 **提交操作** ，提交完毕之后将给协调者发送提交成功的响应。

而如果在第一阶段并不是所有参与者都返回了准备好了的消息，那么此时协调者将会给所有参与者发送 **回滚事务的 rollback 请求**，参与者收到之后将会 **回滚它在第一阶段所做的事务处理** ，然后再将处理情况返回给协调者，最终协调者收到响应后便给事务发起者返回处理失败的结果。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404292106974.png)

2PC 实现得还是比较鸡肋的，因为事实上它只解决了各个事务的原子性问题，随之也带来了很多的问题。

- 单点故障问题：如果协调者挂了那么整个系统都处于不可用的状态了。

- 阻塞问题：即当协调者发送 prepare 请求，参与者收到之后如果能处理那么它将会进行事务的处理但并不提交，这个时候会一直占用着资源不释放，如果此时协调者挂了，那么这些资源都不会再释放了，这会极大影响性能。

- 数据不一致问题：比如当第二阶段，协调者只发送了一部分的 commit 请求就挂了，那么也就意味着，收到消息的参与者会进行事务的提交，而后面没收到的则不会进行事务提交，那么这时候就会产生数据不一致性问题

### 3PC（三阶段提交）

因为 2PC 存在的一系列问题，比如单点，容错机制缺陷等等，从而产生了 **3PC（三阶段提交）** 。那么这三阶段又分别是什么呢？

1. CanCommit 阶段：协调者向所有参与者发送 CanCommit 请求，参与者收到请求后会根据自身情况查看是否能执行事务，如果可以则返回 YES 响应并进入预备状态，否则返回 NO 
2. PreCommit 阶段：协调者根据参与者返回的响应来决定是否可以进行下面的 PreCommit 操作。如果上面参与者返回的都是 YES，那么协调者将向所有参与者发送 PreCommit 预提交请求，**参与者收到预提交请求后，会进行事务的执行操作，并将 Undo 和 Redo 信息写入事务日志中** ，最后如果参与者顺利执行了事务则给协调者返回成功的响应。如果在第一阶段协调者收到了 **任何一个 NO** 的信息，或者 **在一定时间内** 并没有收到全部的参与者的响应，那么就会中断事务，它会向所有参与者发送中断请求（abort），参与者收到中断请求之后会立即中断事务，或者在一定时间内没有收到协调者的请求，它也会中断事务。
3. **DoCommit 阶段**：这个阶段其实和 2PC 的第二阶段差不多，如果协调者收到了所有参与者在 PreCommit 阶段的 YES 响应，那么协调者将会给所有参与者发送 DoCommit 请求，**参与者收到 DoCommit 请求后则会进行事务的提交工作**，完成后则会给协调者返回响应，协调者收到所有参与者返回的事务提交成功的响应之后则完成事务。若协调者在 PreCommit 阶段 **收到了任何一个 NO 或者在一定时间内没有收到所有参与者的响应** ，那么就会进行中断请求的发送，参与者收到中断请求后则会 **通过上面记录的回滚日志** 来进行事务的回滚操作，并向协调者反馈回滚状况，协调者收到参与者返回的消息后，中断事务。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404292106036.png)

 

> 这里是 3PC 在成功的环境下的流程图，可以看到 3PC 在很多地方进行了超时中断的处理，比如协调者在指定时间内未收到全部的确认消息则进行事务中断的处理，这样能 **减少同步阻塞的时间** 。还有需要注意的是，**3PC 在 DoCommit 阶段参与者如未收到协调者发送的提交事务的请求，它会在一定时间内进行事务的提交**。为什么这么做呢？是因为这个时候肯定**保证了在第一阶段所有的协调者全部返回了可以执行事务的响应**，这个时候有理由**相信其他系统都能进行事务的执行和提交**，所以**不管**协调者有没有发消息给参与者，进入第三阶段参与者都会进行事务的提交操作。

总之，3PC 通过一系列的超时机制很好的缓解了阻塞问题，但是最重要的一致性并没有得到根本的解决，比如在 DoCommit 阶段，当一个参与者收到了请求之后其他参与者和协调者挂了或者出现了网络分区，这个时候收到消息的参与者都会进行事务提交，这就会出现数据不一致性问题。

所以，要解决一致性问题还需要靠 Paxos 算法 

### Paxos 算法

Paxos 算法是基于**消息传递且具有高度容错特性的一致性算法**，是目前公认的解决分布式一致性问题最有效的算法之一，**其解决的问题就是在分布式系统中如何就某个值（决议）达成一致** 。

在 Paxos 中主要有三个角色，分别为 Proposer提案者、Acceptor表决者、Learner学习者。Paxos 算法和 2PC 一样，也有两个阶段，分别为 Prepare 和 accept 阶段。

#### prepare 阶段

- Proposer提案者：负责提出 proposal，每个提案者在提出提案时都会首先获取到一个 **具有全局唯一性的、递增的提案编号 N**，即在整个集群中是唯一的编号 N，然后将该编号赋予其要提出的提案，在**第一阶段是只将提案编号发送给所有的表决者**。

- Acceptor表决者：每个表决者在 accept 某提案后，会将该提案编号 N 记录在本地，这样每个表决者中保存的已经被 accept 的提案中会存在一个**编号最大的提案**，其编号假设为 maxN。每个表决者仅会 accept 编号大于自己本地 maxN 的提案，在批准提案时表决者会将以前接受过的最大编号的提案作为响应反馈给 Proposer 。

 

下面是 prepare 阶段的流程图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404292106055.png)

 

#### accept 阶段

当一个提案被 Proposer 提出后，如果 Proposer 收到了超过半数的 Acceptor 的批准（Proposer 本身同意），那么此时 Proposer 会给所有的 Acceptor 发送真正的提案（你可以理解为第一阶段为试探），这个时候 Proposer 就会发送提案的内容和提案编号。

表决者收到提案请求后会再次比较本身已经批准过的最大提案编号和该提案编号，如果该提案编号 **大于等于** 已经批准过的最大提案编号，那么就 accept 该提案（此时执行提案内容但不提交），随后将情况返回给 Proposer 。如果不满足则不回应或者返回 NO 。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404292106207.png)

当 Proposer 收到超过半数的 accept ，那么它这个时候会向所有的 acceptor 发送提案的提交请求。需要注意的是，因为上述仅仅是超过半数的 acceptor 批准执行了该提案内容，其他没有批准的并没有执行该提案内容，所以这个时候需要**向未批准的 acceptor 发送提案内容和提案编号并让它无条件执行和提交**，而对于前面已经批准过该提案的 acceptor 来说 **仅仅需要发送该提案的编号** ，让 acceptor 执行提交就行了。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404292107506.png)

而如果 Proposer 如果没有收到超过半数的 accept 那么它将会将 **递增** 该 Proposal 的编号，然后 **重新进入 Prepare 阶段** 。

对于 Learner 来说如何去学习 Acceptor 批准的提案内容，这有很多方式，读者可以自己去了解一下，这里不做过多解释。

#### paxos 算法的死循环问题

比如说，此时提案者 P1 提出一个方案 M1，完成了 Prepare 阶段的工作，这个时候 acceptor 则批准了 M1，但是此时提案者 P2 同时也提出了一个方案 M2，它也完成了 Prepare 阶段的工作。然后 P1 的方案已经不能在第二阶段被批准了（因为 acceptor 已经批准了比 M1 更大的 M2），所以 P1 自增方案变为 M3 重新进入 Prepare 阶段，然后 acceptor ，又批准了新的 M3 方案，它又不能批准 M2 了，这个时候 M2 又自增进入 Prepare 阶段。

就这样无休无止的永远提案下去，这就是 paxos 算法的死循环问题。

那么如何解决呢？很简单，人多了容易吵架，那么现在 **就允许一个能提案** 就行了。



## ZAB 协议

Paxos 算法应该可以说是 ZooKeeper 的灵魂了。但是，ZooKeeper 并没有完全采用 Paxos 算法 ，而是专门定制了一个 ZAB 协议作为其保证数据一致性的核心算法。另外，在 ZooKeeper 的官方文档中也指出，ZAB 协议并不像 Paxos 算法那样，是一种通用的分布式一致性算法，它是一种特别为 Zookeeper 设计的崩溃可恢复的原子消息广播算法。

### ZAB 协议介绍

ZAB（ZooKeeper Atomic Broadcast 原子广播） 协议是为分布式协调服务 ZooKeeper 专门设计的一种支持崩溃恢复的原子广播协议。 在 ZooKeeper 中，主要依赖 ZAB 协议来实现分布式数据一致性，基于该协议，ZooKeeper 实现了一种主备模式的系统架构来保持集群中各个副本之间的数据一致性。

 

ZAB 协议包括两种基本的模式，分别是

- 崩溃恢复：当整个服务框架在启动过程中，或是当 Leader 服务器出现网络中断、崩溃退出与重启等异常情况时，ZAB 协议就会进入恢复模式并选举产生新的 Leader 服务器。当选举产生了新的 Leader 服务器，同时集群中已经有过半的机器与该 Leader 服务器完成了状态同步之后，ZAB 协议就会退出恢复模式。其中，**所谓的状态同步是指数据同步，用来保证集群中存在过半的机器能够和 Leader 服务器的数据状态保持一致**

- 消息广播：**当集群中已经有过半的 Follower 服务器完成了和 Leader 服务器的状态同步，那么整个服务框架就可以进入消息广播模式了。** 当一台同样遵守 ZAB 协议的服务器启动后加入到集群中时，如果此时集群中已经存在一个 Leader 服务器在负责进行消息广播，那么新加入的服务器就会自觉地进入数据恢复模式：找到 Leader 所在的服务器，并与其进行数据同步，然后一起参与到消息广播流程中去。

 

### 消息广播模式

说白了就是 ZAB 协议是如何处理写请求的，只有 Leader 能处理写请求嘛？那么 Follower 和 Observer 是不是也需要 **同步更新数据** 呢？总不能数据只在 Leader 中更新了，其他角色都没有得到更新吧？

不就是 **在整个集群中保持数据的一致性** 嘛？

第一步肯定需要 Leader 将写请求 **广播** 出去，让 Leader 问问 Followers 是否同意更新，如果超过半数以上的同意那么就进行 Follower 和 Observer 的更新（和 Paxos 一样）。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404292107541.png)

 

这两个Queue 哪冒出来的？**ZAB 需要让 Follower 和 Observer 保证顺序性** 。何为顺序性，比如现在有一个写请求 A，此时 Leader 将请求 A 广播出去，因为只需要半数同意就行，所以可能这个时候有一个 Follower F1 因为网络原因没有收到，而 Leader 又广播了一个请求 B，因为网络原因，F1 竟然先收到了请求 B 然后才收到了请求 A，这个时候请求处理的顺序不同就会导致数据的不同，从而 **产生数据不一致问题** 。

所以在 Leader 这端，它为每个其他的 zkServer 准备了一个 **队列** ，采用先进先出的方式发送消息。由于协议是 **通过 TCP** 来进行网络通信的，保证了消息的发送顺序性，接受顺序性也得到了保证。

除此之外，在 ZAB 中还定义了一个 **全局单调递增的事务 ID ZXID** ，它是一个 64 位 long 型，其中高 32 位表示 epoch 年代，低 32 位表示事务 id。epoch 是会根据 Leader 的变化而变化的，当一个 Leader 挂了，新的 Leader 上位的时候，年代（epoch）就变了。而低 32 位可以简单理解为递增的事务 id。

定义这个的原因也是为了顺序性，每个 proposal 在 Leader 中生成后需要 **通过其 ZXID 来进行排序** ，才能得到处理。

 

### 崩溃恢复模式

说到崩溃恢复首先要提到 ZAB 中的 Leader 选举算法，当系统出现崩溃影响最大应该是 Leader 的崩溃，因为我们只有一个 Leader ，所以当 Leader 出现问题的时候我们势必需要重新选举 Leader 。

 

**崩溃恢复** 是什么？主要就是 **当集群中有机器挂了，整个集群如何保证数据一致性？**

如果只是 Follower 挂了，而且挂的没超过半数的时候，因为在 Leader 中会维护队列，所以不用担心后面的数据没接收到导致数据不一致性。

如果 Leader 挂了那就麻烦了，肯定需要先暂停服务变为 Looking 状态然后进行 Leader 的重新选举，但这个就要分为两种情况了，分别是 **确保已经被 Leader 提交的提案最终能够被所有的 Follower 提交** 和 **跳过那些已经被丢弃的提案** 。

 

**确保已经被 Leader 提交的提案最终能够被所有的 Follower 提交**是什么意思呢？

假设 Leader (server2) 发送 commit 请求（忘了请看上面的消息广播模式），他发送给了 server3，然后要发给 server1 的时候突然挂了。这个时候重新选举的时候我们如果把 server1 作为 Leader 的话，那么肯定会产生数据不一致性，因为 server3 肯定会提交刚刚 server2 发送的 commit 请求的提案，而 server1 根本没收到所以会丢弃。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404292107546.png)

 

**事实上这个时候 server1 已经不可能成为 Leader 了，因为 server1 和 server3 进行投票选举的时候会比较 ZXID ，而此时 server3 的 ZXID 肯定比 server1 的大了**。

 

**跳过那些已经被丢弃的提案**又是什么意思呢？

假设 Leader (server2) 此时同意了提案 N1，自身提交了这个事务并且要发送给所有 Follower 要 commit 的请求，却在这个时候挂了，此时肯定要重新进行 Leader 的选举，比如说此时选 server1 为 Leader （这无所谓）。但是过了一会，这个 **挂掉的 Leader 又重新恢复了** ，此时它肯定会作为 Follower 的身份进入集群中，需要注意的是刚刚 server2 已经同意提交了提案 N1，但其他 server 并没有收到它的 commit 信息，所以其他 server 不可能再提交这个提案 N1 了，这样就会出现数据不一致性问题了，所以 **该提案 N1 最终需要被抛弃掉** 。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404292107054.png)


<!-- @include: @article-footer.snippet.md -->     

