---
title: Ribbon负载均衡
category: 微服务
tags:
  - SpringCloud
  - Ribbon
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,SpringCloud,Ribbon,负载均衡
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---




上一节中，我们添加了@LoadBalanced注解，即可实现负载均衡功能，这是什么原理呢？



## 负载均衡原理

SpringCloud底层其实是利用了一个名为Ribbon的组件，来实现负载均衡功能的。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011121618.png)

那么我们发出的请求明明是http://userservice/user/1，怎么变成了http://localhost:8081的呢？



## 什么是 Ribbon

1. Ribbon 是一个基于 Http 和 TCP 的客服端负载均衡工具，它是基于 Netflix Ribbon 实现的。
2. 它不像 spring cloud 服务注册中心、配置中心、API 网关那样独立部署，但是它几乎存在于每个Spring cloud 微服务中。包括 feign 提供的声明式服务调用也是基于该 Ribbon实现的。
3. Ribbon 默认提供很多种负载均衡算法，例如 轮询、随机 等等。甚至包含自定义的负载均衡算法。

在客户端节点会维护可访问的服务器清单，服务器清单来自服务注册中心，通过心跳维持服务器清单的健康性。

开启客户端负载均衡调用：

4. 服务提供者启动多个服务实例注册到服务注册中心；
5. 服务消费者直接通过调用被@LoadBalanced 注解修饰过的RestTemplate 来实现面向服务的接口调用。



### 集中式与进程内负载均衡的区别

目前业界主流的负载均衡方案可分成两类：

6. 集中式负载均衡, 即在 consumer 和 provider 之间使用独立的负载均衡设施(可以是硬件，如F5, 也可以是软件，如 Nginx), 由该设施负责把 访问请求 通过某种策略转发至 provider；
7. 进程内负载均衡，将负载均衡逻辑集成到 consumer，consumer 从服务注册中心获知有哪些地址可用，然后自己再从这些地址中选择出一个合适的 provider。Ribbon 就属于后者，它只是一个类库，集成于 consumer 进程，consumer 通过它来获取到 provider 的地址。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011128525.png)



## 源码跟踪

为什么我们只输入了service名称就可以访问了呢？之前还要获取ip和端口。

显然有人帮我们根据service名称，获取到了服务实例的ip和端口。它就是`LoadBalancerInterceptor`，这个类会在对RestTemplate的请求进行拦截，然后从Eureka根据服务id获取服务列表，随后利用负载均衡算法得到真实的服务地址信息，替换服务id。

我们进行源码跟踪：

### LoadBalancerIntercepor

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011122160.png)

可以看到这里的intercept方法，拦截了用户的HttpRequest请求，然后做了几件事：

- `request.getURI()`：获取请求uri，本例中就是 http://user-service/user/8
- `originalUri.getHost()`：获取uri路径的主机名，其实就是服务id，`user-service`
- `this.loadBalancer.execute()`：处理服务id，和用户请求。

这里的`this.loadBalancer`是`LoadBalancerClient`类型，我们继续跟入。



### LoadBalancerClient

继续跟入execute方法：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011122031.png)

代码是这样的：

- getLoadBalancer(serviceId)：根据服务id获取ILoadBalancer，而ILoadBalancer会拿着服务id去eureka中获取服务列表并保存起来。
- getServer(loadBalancer)：利用内置的负载均衡算法，从服务列表中选择一个。本例中，可以看到获取了8082端口的服务



放行后，再次访问并跟踪，发现获取的是8081：

 ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011122035.png)

果然实现了负载均衡。



#### 负载均衡策略IRule

在刚才的代码中，可以看到获取服务使通过一个`getServer`方法来做负载均衡:

 ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011122064.png)

我们继续跟入：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011122180.png)

继续跟踪源码chooseServer方法，发现这么一段代码：

 ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011122285.png)



我们看看这个rule是谁：

 ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011122941.png)

这里的rule默认值是一个`RoundRobinRule`，看类的介绍：

 ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011122436.png)

这不就是轮询的意思嘛。

到这里，整个负载均衡的流程我们就清楚了。



### 总结

SpringCloudRibbon的底层采用了一个拦截器，拦截了RestTemplate发出的请求，对地址做了修改。用一幅图来总结一下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011123623.png)



基本流程如下：

- 拦截我们的RestTemplate请求http://userservice/user/1
- RibbonLoadBalancerClient会从请求url中获取服务名称，也就是user-service
- DynamicServerListLoadBalancer根据user-service到eureka拉取服务列表
- eureka返回列表，localhost:8081、localhost:8082
- IRule利用内置负载均衡规则，从列表中选择一个，例如localhost:8081
- RibbonLoadBalancerClient修改请求地址，用localhost:8081替代userservice，得到http://localhost:8081/user/1，发起真实请求



## 负载均衡策略



### 负载均衡策略

负载均衡的规则都定义在IRule接口中，而IRule有很多不同的实现类：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011123608.png)

不同规则的含义如下：

| **内置负载均衡规则类**    | **规则描述**                                                 |
| ------------------------- | ------------------------------------------------------------ |
| RoundRobinRule            | 简单轮询服务列表来选择服务器。它是Ribbon默认的负载均衡规则。 |
| AvailabilityFilteringRule | 对以下两种服务器进行忽略：   <br />（1）在默认情况下，这台服务器如果3次连接失败，这台服务器就会被设置为“短路”状态。短路状态将持续30秒，如果再次连接失败，短路的持续时间就会几何级地增加。  <br />（2）并发数过高的服务器。如果一个服务器的并发连接数过高，配置了AvailabilityFilteringRule规则的客户端也会将其忽略。并发连接数的上限，可以由客户端的`<clientName>.<clientConfigNameSpace>.ActiveConnectionsLimit`属性进行配置。 |
| WeightedResponseTimeRule  | 为每一个服务器赋予一个权重值。服务器响应时间越长，这个服务器的权重就越小。这个规则会随机选择服务器，这个权重值会影响服务器的选择。 |
| **ZoneAvoidanceRule**     | 以区域可用的服务器为基础进行服务器的选择。使用Zone对服务器进行分类，这个Zone可以理解为一个机房、一个机架等。而后再对Zone内的多个服务做轮询。 |
| BestAvailableRule         | 忽略那些短路的服务器，并选择并发数较低的服务器。             |
| RandomRule                | 随机选择一个可用的服务器。                                   |
| RetryRule                 | 重试机制的选择逻辑                                           |



默认的实现就是ZoneAvoidanceRule，是一种轮询方案



### 自定义负载均衡策略

通过定义IRule实现可以修改负载均衡规则，有两种方式：

8. 代码方式：在order-service中的OrderApplication类中，定义一个新的IRule：

```java
@Bean
public IRule randomRule(){
    return new RandomRule();
}
```



9. 配置文件方式：在order-service的application.yml文件中，添加新的配置也可以修改规则：

```yaml
userservice: # 给某个微服务配置负载均衡规则，这里是userservice服务
  ribbon:
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule # 负载均衡规则 
```



> **注意**，一般用默认的负载均衡规则，不做修改。



## 饥饿加载

Ribbon默认是采用懒加载，即第一次访问时才会去创建LoadBalanceClient，请求时间会很长。

而饥饿加载则会在项目启动时创建，降低第一次访问的耗时，通过下面配置开启饥饿加载：

```yaml
ribbon:
  eager-load:
    enabled: true
    clients: userservice
```

