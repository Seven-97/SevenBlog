---
title: dubbo - 高级特性
category: 微服务
tag:
 - dubbo
---







## 高级特性

### 序列化

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405162348017.png)



- dubbo 内部已经将序列化和反序列化的过程内部封装了，只需要在定义pojo类时实现Serializable接口即可
- 一般会定义一个公共的pojo模块，让生产者和消费者都依赖该模块。



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

- Failover Cluster：失败重试。默认值。当出现失败，重试其它服务器 ，默认重试2次，使用 retries 配置。一般用于读操作
- Failfast Cluster ：快速失败，只发起一次调用，失败立即报错。通常用于写操作。
- Failsafe Cluster ：失败安全，出现异常时，直接忽略。返回一个空结果。
- Failback Cluster ：失败自动恢复，后台记录失败请求，定时重发，直到成功。通常用于消息通知操作。
- Forking Cluster ：并行调用多个服务器，只要一个成功即返回。
- Broadcast  Cluster ：广播调用所有提供者，逐个调用，任意一台报错则报错。



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

- Dubbo：

支持多传输协议(Dubbo、Rmi、http、redis等等)，可以根据业务场景选择最佳的方式。非常灵活。
默认的Dubbo协议：利用Netty，TCP传输，单一、异步、长连接，适合数据量小、高并发和服务提供者远远少于消费者的场景。



- Feign：

基于Http传输协议，短连接，不适合高并发的访问。



### 负载均衡

- Dubbo：

支持4种算法（随机、轮询、活跃度、Hash一致性），而且算法里面引入权重的概念。
配置的形式不仅支持代码配置，还支持Dubbo控制台灵活动态配置。
负载均衡的算法可以精准到某个服务接口的某个方法。



- Feign：

只支持N种策略：轮询、随机、ResponseTime加权。
负载均衡算法是Client级别的。



Nacos注册中心很好的兼容了Feign，Feign默认集成了Ribbon，所以在Nacos下使用Fegin默认就实现了负载均衡的效果。



### 容错策略

- Dubbo：

支持多种容错策略：failover、failfast、brodecast、forking等，也引入了retry次数、timeout等配置参数。

- Feign：

利用熔断机制来实现容错的，处理的方式不一样。



### 小结

Dubbo支持更多功能、更灵活、支持高并发的RPC框架。

SpringCloud全家桶里面（Feign、Ribbon、Hystrix），特点是非常方便。Ribbon、Hystrix、Feign在服务治理中，配合Spring Cloud做微服务，使用上有很多优势，社区也比较活跃，看将来更新发展。

业务发展影响着架构的选型，当服务数量不是很大时，使用普通的分布式RPC架构即可，当服务数量增长到一定数据，需要进行服务治理时，就需要考虑使用流式计算架构。Dubbo可以方便的做更精细化的流量调度，服务结构治理的方案成熟，适合生产上使用，虽然Dubbo是尘封后重新开启，但这并不影响其技术价值。

如果项目对性能要求不是很严格，可以选择使用Feign，它使用起来更方便。
如果需要提高性能，避开基于Http方式的性能瓶颈，可以使用Dubbo。

Dubbo Spring Cloud的出现，使得Dubbo既能够完全整合到Spring Cloud的技术栈中，享受Spring Cloud生态中的技术支持和标准化输出，又能够弥补Spring Cloud中服务治理这方面的短板












