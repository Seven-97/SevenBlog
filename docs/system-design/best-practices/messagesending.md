---
title: 如何写好发消息的代码
category: 系统设计
tag:
 - 最佳实践
---



> 来源：[阿里云开发者](https://mp.weixin.qq.com/s/_bYjQd3BCIT3w1N0cMWlCw)

## 问题描述

做技术的同学，尤其是业务开发同学都是经常和消息打交道的，大家也都喜欢研究像MetaQ这种消息中间件的一些实现代码。

```java
try {
  transactionTemplate.start();
  // 位置1
  orderManager.createOrder(order);
  // 位置2
  messageProducer.send(buildOrderCreatedMsg(order)); // 发送订单创建成功的消息。
  transactionTemplate.commit();
  // 位置3
} catch (Exception e) {
  transactionTemplate.rollback();
}
```

需要发送订单创建成功的消息，目前是在位置2上面发送的消息。那么为什么不是在位置1或者位置3上发送消息呢？



如果这段代码在运行过程中没有任何意外的行为，DB操作总是很快成功，那么看起来在位置1、位置2、位置3上发送消息好像差别并不是很大。但实际的运行环境肯定并不是这么完美的，因此需要做容错设计。

- 持久化前发消息：也就是代码中的位置1。经过线上观察，orderManager.createOrder这行代码的执行时间平均耗时在5ms，但极端情况下会有超过2s的案例，同时也能观察到极少数的执行失败的情况。这些现象表明我们不能在业务持久化前发消息，否则我们很可能为不存在的订单发送了创单成功消息。

- 持久化中发消息：也就是代码中的位置2。这个时候orderManager.createOrder已经执行成功了，按理说是个不错的位置，但这个时候事务实际上还没有提交，这个时候发出消息理论上和前面的位置1是比较类似的情况，只是说失败率要低很多。同时，如果发送消息的RT变大，甚至hang住的话，也会导致事务无法提交。

- 持久化后发消息：也就是代码中的位置3。这个时候可以确保订单已经是创建成功了，事务都提交了。但是代码上线过后，一个星期总是会遇到几笔丢消息的场景，即订单创建成功了，却没有消息发出来。虽然不多，但是很烦。排查下来，大多数都是发送消息时遇到了网络错误导致消息发送失败了，也有少数是发布的时候执行到这里的时候机器被重启了。



## 解决方案

### 使用事务消息

前面的3个方案，对这个场景来说都不是很完美，想到了事务消息。于是代码修改如下：

```java
try {
  transactionTemplate.start();
  messageTransactionProducer.send(buildOrderCreatedMsg(order), new TransactionExecutor(transactionTemplate){
    @override
    public TransactionStatus execute(Message msg) {
      orderManager.createOrder(order);
      return TransactionStatus.CommitTransaction;
    }
  });
} catch (Exception e) {
  transactionTemplate.rollback();
}

// 订单创建成功消息的半消息回查实现
public class OrderTransactionCheckerImpl implements TransactionChecker {

  @override
  public TransactionStatus check(Message msg) {
    if (orderManager.isOrderExist(getOrderId(msg)) {
      return TransactionStatus.CommitTransaction;
    } else {
      return TransactionStatus.RollbackTransaction;
    }
  }
}
```

但是，尽管使用上面的这个事务消息，还是有可能出现问题。如订单的数据库集群中有一个分库遇到磁盘IO故障，当DB发生故障的时候，创建订单的代码流程卡住了，事务并未提交，而半消息回查的通知很快过来了，这个时候通过订单号查询订单显然是查询不到的，所以再次修改了半消息回查的代码。

```java
// 订单创建成功消息的半消息回查实现
public class OrderTransactionCheckerImpl implements TransactionChecker {

  @override
  public TransactionStatus check(Message msg) {
    if (orderManager.isOrderExist(getOrderId(msg)) {
      return TransactionStatus.CommitTransaction;
    } else {
      // 如果消息发送时间距当前时间20s以上，查询不到订单则认为是真的查询不到，否则有可能是创单的事务还未提交
      // 20s的时间是来源于DB事务默认的超时时间设置15s加上5s的Buffer，不同团队会不同，这个值不能照搬
      if (new Date().getTime() - msg.getSendTime() > 20000) {
        return TransactionStatus.RollbackTransaction;
      } else {
        return TransactionStatus.Unknown;
      }
    }
  }
}
```

重点是增加了查询不到订单时20s的保护时间，在保护时间内返回Unkown的状态，因为这个时候我们是真的不能确认是否是创建订单的操作被Block住了。从此以后再也没有接到丢消息的投诉了。

### 使用消息表

还有一种简单的方式是不在业务流程中发消息，而是直接依赖数据库的高可用性在写入业务数据的同时在自建的消息表中写入一条记录，后续通过读取这条记录的状态和内容来做异步的动作。感谢评论中璞尧、逾明等同学的建议和反馈。代码如下：

```java

try {
  transactionTemplate.start();
  orderManager.createOrder(order);
  miniBus.addMsg(buildOrderCreatedMsg(order)); // 在订单库中创建一张消息表，这里实际的动作是在这个消息表中写入一条记录。因为消息表和订单表存在于同一个数据库中，这里巧妙的利用了数据的事务特性。
  transactionTemplate.commit();
} catch (Exception e) {
  transactionTemplate.rollback();
}
```

这种方式通常需要一个扫描任务一直在读取自己建立的消息表，并维护它们的后续流转状态，当流转完成后会对记录做物理删除。这种方案会增加一部分数据库的写入和读取压力（一般情况下并不大，毕竟我们的消息体通常是不大的），以及代码的实现复杂度（需要自建消息表的扫描任务、管理消息记录的状态等，但对于一个工程来说这是一次性的任务），同时这个自建的消息表碎片率肯定会很高（似乎不是什么大事儿），不过除此外优势也是相当明显的，消息与业务操作的一致性保障的很好，毕竟数据库是我们最值得依赖的存储。还有一个优势就是它让业务操作的代码看起来真的很整洁。



### 是否有最佳实践？

上面事务消息的方案，看起来就是最佳实践。那么大家一般会有几个问题：

- **照着最佳实践写就行了吗？**

比如orderManager.isOrderExist的订单是否存在的检查，得确保使用了主库，而不是因为读写分离读到了备库上面去。

这里是订单创建，可以很简单的判断订单是不是存在了。那如果是订单状态的变更呢？即update型的业务操作，就需要判断这个业务操作是不是发生了，那判断起来恐怕就要复杂不少了。（幸好delete型的业务操作都是软删除，好判断）

发送消息的Producer，大多数情况下默认的超时时间是3s，如果大面积发生卡住的问题能接受吗？这个时候是不是应当让创建订单失败？

- **弱依赖消息可不可以简单点？**

弱依赖这个说法背后的情况挺复杂的。大多数情况下大家可能说的是偶尔丢几条消息其实没事儿，然后就采用了前面的“持久化后发消息”的方案，就比如一个场景是给用户发送红包成功后的消息，用它来给用户发送收到红包的提醒，偶尔丢几个通知的确是出不了事的。不过我曾经遇到过2起消息中间件的故障，期间大概有5分钟发送消息会有大量（超过50%比例）的失败，大家再想想，是不是这种情况下也能接受？短时间大批量的丢消息，真的能接受吗？

要点是故障相关的情况，弱依赖到底是怎么个弱法是需要仔细斟酌的，尤其是失败的场景会是怎么样的失败形为需要有心理预期，同时失败后到底是怎么补偿要做好预案，并没有大家一开始认为的那么简单。（但是根据经验，一般为了做弱依赖的弥补，大多数时候做的事情反而要更多）

- **发个消息真的需要这么复杂吗？**

事务消息这么复杂，其实也可以用binlog监听。（但需要接受更高的延迟，以及更高的代码维护成本。同时根据DB记录拼装消息体可能要复杂一点儿。像营销、商品等等的缓存失效都是这么干的）



## 总结

前面零星提到的一些问题，这里再总结一下，根据以往的经验，实际上是没有最佳实践的，而是要根据自己的业务情况做取舍。同时，真的发生问题的时候，往往是发生最严重的问题，而不是我们想像的那样的小问题，事前做好容错设计才是确保稳定性的银弹。

1. 发送消息失败了，应该BLOCK业务流程吗？
2. 发送消息的超时时间是多少？大面积超时的情况下，业务流程应当是什么样的反应？
3. 能够接受丢消息吗？短时间大面积的丢消息呢？补偿方案是什么？
4. 消息的消费方做好消息的幂等了吗？我们能不能短时间大量进行消息重投？





