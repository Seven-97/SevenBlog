---
title: dubbo - 高级特性
category: 微服务
tag:
 - dubbo
 - RPC
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,dubbo,RPC协议,序列化,超时重试
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---







## 高级特性

### 序列化

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405162348017.png)



- dubbo 内部已经将序列化和反序列化的过程内部封装了，只需要在定义pojo类时实现Serializable接口即可
- 一般会定义一个公共的pojo模块，让生产者和消费者都依赖该模块。

Dubbo 支持的序列化方式对比：

|序列化方式|特点|适用场景|配置示例|
|---|---|---|---|
|**hessian2 (默认)**​|二进制、跨语言、性能较好、兼容性佳|**推荐默认使用**，适合大多数Java应用|`serialization="hessian2"`|
|**kryo**​|**极致性能**、序列化体积小、但不支持跨语言|对性能要求极高的**纯Java环境**​|`serialization="kryo"`|
|**fst**​|性能接近kryo、兼容性更好|kryo的替代选择，稳定性要求高|`serialization="fst"`|
|**protobuf**​|Google出品、跨语言、高效、强schema约束|**多语言微服务**、需要严格数据契约|`serialization="protobuf"`|
|**avro**​|动态schema、适合大数据场景|Hadoop生态、数据演进频繁的场景|`serialization="avro"`|
|**jdk**​|Java原生、兼容性好但**性能最差**​|兼容性测试、不推荐生产环境|`serialization="jdk"`|
|**fastjson**​|文本格式、可读性好|需要人工调试、兼容老旧系统|`serialization="fastjson"`|
|**gson**​|Google的JSON库|与现有Gson系统集成|`serialization="gson"`|


### 地址缓存

注册中心挂了，服务是否可以正常访问？

- 可以，因为dubbo服务消费者在第一次调用时，会将服务提供方地址缓存到本地，以后在调用则不会访问注册中心。
- 当服务提供者地址发生变化时，注册中心会通知服务消费者。



### 超时与重试

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405162349178.png)



- 服务消费者在调用服务提供者的时候发生了阻塞、等待的情形，这个时候，服务消费者会一直等待下去。
- 在某个峰值时刻，大量的请求都在同时请求服务消费者，会造成线程的大量堆积，势必会造成雪崩。

因此dubbo 利用超时机制来解决这个问题

- 设置一个超时时间，在这个时间段内，无法完成服务访问，则自动断开连接。
- 使用timeout属性配置超时时间，默认值1000，单位毫秒。

但是如果出现网络抖动，则这一次请求就会失败。

- Dubbo 提供重试机制来避免类似问题的发生。通过 retries  属性来设置重试次数。默认为 2 次。

#### 多粒度超时时间

dubbo 支持多粒度配置 rpc 调用的超时时间：

优先级从高到低依次为  方法级别配置>服务级别配置 >全局配置 >默认值。


1. 配置全局默认超时时间为5s (不配置的情况下，所有服务的默认超时时间是 1s)

```java
dubbo:
  provider:
    timeout: 5000
```

2. 在消费端，指定 DemoService 服务调用的超时时间为 5s

```java
@DubboReference(timeout=5000)
private DemoService demoService;
```


3. 在提供端，指定 DemoService 服务调用的超时时间为 5s(可作为所有消费端的默认值，如果消费端有指定则优先级更高)

```java
@DubboService(timeout=5000)
public class DemoServiceImpl implements DemoService{}
```


4. 在消费端，指定 DemoService sayHello 方法调用的超时时间为 5s

```java
@DubboReference(methods = {@Method(name = "sayHello", timeout = 5000)})
private DemoService demoService;
```


5. 在提供端，指定 DemoService sayHello 方法调用的超时时间为 5s(可作为所有消费端的默认值，如果消费端有指定则优先级更高)
```java
@DubboService(methods = {@Method(name = "sayHello", timeout = 5000)})
public class DemoServiceImpl implements DemoService{}
```

#### Deadline 机制

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202504022142929.png)

我们来分析一下以上调用链路以及可能出现的超时情况：

A 调用 B 设置了超时时间 5s，因此 B -> C -> D 总计耗时不应该超过 5s，否则 A 就会收到超时异常

在任何情形下，只要 A 等待 5s 没有收到响应，整个调用链路就可以被终止了（如果此时 C 正在运行，则 C -> D 就没有发起的意义了）

理论上 B -> C、C -> D 都有自己独立的超时时间设置，超时计时也是独立计算的，它们不知道 A 作为调用发起方是否超时

在 Dubbo 框架中，A -> B 的调用就像一个开关，一旦启动，在任何情形下整个 A -> B -> C -> D 调用链路都会被完整执行下去，即便调用方 A 已经超时，后续的调用动作仍会继续。这在一些场景下是没有意义的，尤其是链路较长的情况下会带来不必要的资源消耗，deadline 就是设计用来解决这个问题，通过在调用链路中传递 deadline（deadline初始值等于超时时间，随着时间流逝而减少）可以确保调用链路只在有效期内执行，deadline 消耗殆尽之后，调用链路中其他尚未执行的任务将被取消。

因此 deadline 机制就是将 B -> C -> D 当作一个整体看待，这一系列动作必须在 5s 之内完成。随着时间流逝 deadline 会从 5s 逐步扣减，后续每一次调用实际可用的超时时间即是当前 deadline 值，比如 C 收到请求时已经过去了 3s，则 C -> D 的超时时间只剩下 2s。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202504022142726.png)

deadline 机制默认是关闭的，如果要启用 deadline 机制，需要配置以下参数：

```yml
dubbo:
  provider:
    timeout: 5000
    parameters.enable-timeout-countdown: true
```

也可以指定某个服务调用开启 deadline 机制：

```java
@DubboReference(timeout=5000, parameters={"enable-timeout-countdown", "true"})
private DemoService demoService;
```




### 多版本

灰度发布：当出现新功能时，会让一部分用户先使用新功能，用户反馈没问题时，再将所有用户迁移到新功能。

- dubbo 中使用version 属性来设置和调用同一个接口的不同版本



### 负载均衡

负载均衡策略（4种） ：

- Random ：按权重随机，默认值。按权重设置随机概率。
- RoundRobin ：按权重轮询
- LeastActive：最少活跃调用数，相同活跃数的随机。
- ConsistentHash：一致性 Hash，相同参数的请求总是发到同一提供者。



### 集群容错

Dubbo 的集群容错机制是保障服务高可用的核心。下面这张表格汇总了各策略的核心特征：

|容错策略|核心机制|推荐应用场景|关键配置|
|---|---|---|---|
|**`failfast`（快速失败）**​|只调用一次，失败立即抛出异常，**不进行任何重试**。|**非幂等性写操作**（如：创建订单、支付、资金扣减）。|`cluster="failfast"`|
|**`failover`（故障转移）**​|失败后自动重试其他服务器。Dubbo **默认策略**，默认重试2次。|**读操作**或**幂等性写操作**（如：查询、状态更新）。|`cluster="failover" retries="2"`（可调整重试次数）|
|**`failsafe`（失败安全）**​|调用失败时，**忽略异常**并返回一个空结果，记录错误日志。|**非核心业务**（如：日志记录、监控数据上报）。|`cluster="failsafe"`|
|**`failback`（失败自动恢复）**​|失败后，将请求记录到后台，通过**定时任务进行重试**。|**可容忍延迟**的场景（如：消息通知、后续可补偿的操作）。|`cluster="failback"`|
|**`forking`（并行调用）**​|并行调用多个服务提供者，**只要有一个成功就立即返回**。|**实时性要求极高**的读操作（能承受额外资源消耗）。|`cluster="forking" forks="2"`（设置最大并行数）|
|**`broadcast`（广播调用）**​|逐个调用所有提供者，**任意一台报错则报错**。|通知所有提供者更新本地资源（如：刷新本地缓存）。|`cluster="broadcast"`|

选择策略时，主要权衡业务的以下几个特性：

|业务特性|优先考虑的策略|原因|
|---|---|---|
|**操作是否幂等**​|幂等操作可选 `failover`；非幂等操作**必须**选 `failfast`。|防止因重试导致数据重复或不一致。|
|**对实时性的要求**​|要求高且资源充足用 `forking`；可接受延迟用 `failback`。|`forking`以资源换时间，`failback`保证最终成功。|
|**业务的核心程度**​|核心链路失败需快速告警（`failfast`）；非核心链路可忽略（`failsafe`）。|保证核心业务的强一致性和可见性，非核心业务不影响主流程。|


### 服务降级

mock="[fail|force]return|throw xxx"

- fail 或 force 关键字可选，表示调用失败或不调用强制执行 mock 方法，如果不指定关键字默认为 fail
- return 表示指定返回结果，throw 表示抛出指定异常
- xxx 根据接口的返回类型解析，可以指定返回值或抛出自定义的异常

例如

- mock=force:return null ：表示消费方对改服务的调用都直接返回null值，不发起远程调用。用来屏蔽不重要服务不可用时对调用方的影响
- mock=fail:return null ：表示调用方对该服务的方法在调用失败后，再返回null值，不抛异常。用来容忍不重要服务不稳定时对调用方的影响





## 与Feign的区别



相同点：Dubbo 与 Feign 都依赖注册中心、负载均衡，作用是提供远程接口调用。

> 常见的 实现远程调用的方式： Http接口（web接口、RestTemplate+Okhttp）、Feign、RPC调用（Dubbo、Socket编程）、Webservice。。



Dubbo除了注册中心需要进行整合，其它功能都自己实现了，而Feign大部分功能都是依赖全家桶的组件来实现的。



### 协议

- Dubbo：支持多传输协议(Dubbo、Rmi、http、redis等等)，可以根据业务场景选择最佳的方式。非常灵活。

默认的Dubbo协议：利用Netty，TCP传输，单一、异步、长连接，适合数据量小、高并发和服务提供者远远少于消费者的场景。



- Feign：基于Http传输协议，短连接，不适合高并发的访问。



### 负载均衡

- Dubbo：支持4种算法（随机、轮询、活跃度、Hash一致性），而且算法里面引入权重的概念。配置的形式不仅支持代码配置，还支持Dubbo控制台灵活动态配置。负载均衡的算法可以精准到某个服务接口的某个方法。



- Feign：只支持N种策略：轮询、随机、ResponseTime加权。负载均衡算法是Client级别的。Nacos注册中心很好的兼容了Feign，Feign默认集成了Ribbon，所以在Nacos下使用Fegin默认就实现了负载均衡的效果。



### 容错策略

- Dubbo：支持多种容错策略：failover、failfast、brodecast、forking等，也引入了retry次数、timeout等配置参数。

- Feign：利用熔断机制来实现容错的，处理的方式不一样。



### 小结

Dubbo支持更多功能、更灵活、支持高并发的RPC框架。

SpringCloud全家桶里面（Feign、Ribbon、Hystrix），特点是非常方便。Ribbon、Hystrix、Feign在服务治理中，配合Spring Cloud做微服务，使用上有很多优势，社区也比较活跃，看将来更新发展。

业务发展影响着架构的选型，当服务数量不是很大时，使用普通的分布式RPC架构即可，当服务数量增长到一定数据，需要进行服务治理时，就需要考虑使用流式计算架构。Dubbo可以方便的做更精细化的流量调度，服务结构治理的方案成熟，适合生产上使用，虽然Dubbo是尘封后重新开启，但这并不影响其技术价值。

如果项目对性能要求不是很严格，可以选择使用Feign，它使用起来更方便。
如果需要提高性能，避开基于Http方式的性能瓶颈，可以使用Dubbo。

Dubbo Spring Cloud的出现，使得Dubbo既能够完全整合到Spring Cloud的技术栈中，享受Spring Cloud生态中的技术支持和标准化输出，又能够弥补Spring Cloud中服务治理这方面的短板




<!-- @include: @article-footer.snippet.md -->     







