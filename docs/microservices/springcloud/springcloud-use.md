---
title: Springcloud使用篇
category: 微服务
tag:
 - SpringCloud
 - Eureka
 - Ribbon
 - Nacos
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,SpringCloud,Eureka,服务注册,服务发现
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---



## 认识微服务

随着互联网行业的发展，对服务的要求也越来越高，服务架构也从单体架构逐渐演变为现在流行的微服务架构。这些架构之间有怎样的差别呢？



### 单体架构

**单体架构**：将业务的所有功能集中在一个项目中开发，打成一个包部署。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011045132.png)

单体架构的优缺点如下：

**优点：**

- 架构简单
- 部署成本低

**缺点：**

- 耦合度高（维护困难、升级困难）



### 分布式架构

**分布式架构**：根据业务功能对系统做拆分，每个业务功能模块作为独立项目开发，称为一个服务。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011045158.png)



分布式架构的优缺点：

**优点：**

- 降低服务耦合
- 有利于服务升级和拓展

**缺点：**

- 服务调用关系错综复杂



分布式架构虽然降低了服务耦合，但是服务拆分时也有很多问题需要思考：

- 服务拆分的粒度如何界定？
- 服务之间如何调用？
- 服务的调用关系如何管理？

人们需要制定一套行之有效的标准来约束分布式架构。



### 微服务

微服务的架构特征：

- 单一职责：微服务拆分粒度更小，每一个服务都对应唯一的业务能力，做到单一职责
- 自治：团队独立、技术独立、数据独立，独立部署和交付
- 面向服务：服务提供统一标准的接口，与语言和技术无关
- 隔离性强：服务调用做好隔离、容错、降级，避免出现级联问题

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011045742.png)

微服务的上述特性其实是在给分布式架构制定一个标准，进一步降低服务之间的耦合度，提供服务的独立性和灵活性。做到高内聚，低耦合。

因此，可以认为**微服务**是一种经过良好架构设计的**分布式架构方案** 。

但方案该怎么落地？选用什么样的技术栈？全球的互联网公司都在积极尝试自己的微服务落地方案。

其中在Java领域最引人注目的就是SpringCloud提供的方案了。



### SpringCloud

SpringCloud是目前国内使用最广泛的微服务框架。官网地址：https://spring.io/projects/spring-cloud。

SpringCloud集成了各种微服务功能组件，并基于SpringBoot实现了这些组件的自动装配，从而提供了良好的开箱即用体验。

其中常见的组件包括：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011045561.png)



springcloud是一个基于Spring Boot实现的微服务架构开发工具。spring cloud包含多个子项目：

- Spring Cloud Config：配置管理工具，支持使用Git存储配置内容， 可以使用它实现应用配置的外部化存储， 并支持客户端配置信息刷新、加密／解密配置内容等。
- **Spring Cloud Netflix**：核心 组件，对多个Netflix OSS开源套件进行整合。
  - **Eureka**: 服务治理组件， 包含服务注册中心、服务注册与发现机制的实现。
  - **Hystrix**: 容错管理组件，实现断路器模式， 帮助服务依赖中出现的延迟和为故障提供强大的容错能力。
  - Ribbon: 客户端负载均衡的服务调用组件。
  - **Feign**: 基于Ribbon 和Hystrix 的声明式服务调用组件。
  - **Zuul**: 网关组件， 提供智能路由、访问过滤等功能。
  - Archaius: 外部化配置组件。
- **Spring Cloud Gateway**：
- Spring Cloud Bus: 事件、消息总线， 用于传播集群中的状态变化或事件， 以触发后续的处理， 比如用来动态刷新配置等。
- Spring Cloud Cluster: 针对ZooKeeper、Redis、Hazelcast、Consul 的选举算法和通用状态模式的实现。
- Spring Cloud Consul: 服务发现与配置管理工具。
- Spring Cloud ZooKeeper: 基于ZooKeeper 的服务发现与配置管理组件。
- Spring Cloud Security：Spring Security组件封装，提供用户验证和权限验证，一般与Spring Security OAuth2 组一起使用，通过搭建授权服务，验证Token或者JWT这种形式对整个微服务系统进行安全验证
- Spring Cloud Sleuth：分布式链路追踪组件，他分封装了Dapper、Zipkin、Kibana 的组件
- Spring Cloud Stream：Spring Cloud框架的数据流操作包，可以封装RabbitMq，ActiveMq，Kafka，Redis等消息组件，利用Spring Cloud Stream可以实现消息的接收和发送

spring-boot-starter-actuator：该模块能够自动为Spring Boot 构建的应用提供一系列用于监控的端点。



### 总结

- 单体架构：简单方便，高度耦合，扩展性差，适合小型项目。例如：学生管理系统

- 分布式架构：松耦合，扩展性好，但架构复杂，难度大。适合大型互联网项目，例如：京东、淘宝

- 微服务：一种良好的分布式架构方案

  ①优点：拆分粒度更小、服务更独立、耦合度更低

  ②缺点：架构非常复杂，运维、监控、部署难度提高

- SpringCloud是微服务架构的一站式解决方案，集成了各种优秀微服务功能组件





## 服务拆分和远程调用

任何分布式架构都离不开服务的拆分，微服务也是一样。

### 服务拆分原则

这里我总结了微服务拆分时的几个原则：

- 不同微服务，不要重复开发相同业务
- 微服务数据独立，不要访问其它微服务的数据库
- 微服务可以将自己的业务暴露为接口，供其它微服务调用

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011046433.png)



### 服务拆分示例

以课前资料中的微服务cloud-demo为例，其结构如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011046914.png)

cloud-demo：父工程，管理依赖

- order-service：订单微服务，负责订单相关业务
- user-service：用户微服务，负责用户相关业务

要求：

- 订单微服务和用户微服务都必须有各自的数据库，相互独立
- 订单服务和用户服务都对外暴露Restful的接口
- 订单服务如果需要查询用户信息，只能调用用户服务的Restful接口，不能查询用户数据库



### 实现远程调用案例



在order-service服务中，有一个根据id查询订单的接口：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011046792.png)

根据id查询订单，返回值是Order对象，如图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011047800.png)

其中的user为null





在user-service中有一个根据id查询用户的接口：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011047452.png)

查询的结果如图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011047375.png)





#### 案例需求：

修改order-service中的根据id查询订单业务，要求在查询订单的同时，根据订单中包含的userId查询出用户信息，一起返回。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011047636.png)



因此，我们需要在order-service中 向user-service发起一个http的请求，调用http://localhost:8081/user/{userId}这个接口。

大概的步骤是这样的：

- 注册一个RestTemplate的实例到Spring容器
- 修改order-service服务中的OrderService类中的queryOrderById方法，根据Order对象中的userId查询User
- 将查询的User填充到Order对象，一起返回



#### 注册RestTemplate

首先，我们在order-service服务中的OrderApplication启动类中，注册RestTemplate实例：

```java
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@MapperScan("com.seven.order.mapper")
@SpringBootApplication
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```



#### 实现远程调用

修改order-service服务中的cn.seven.order.service包下的OrderService类中的queryOrderById方法：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011048194.png)







### 提供者与消费者

在服务调用关系中，会有两个不同的角色：

**服务提供者**：一次业务中，被其它微服务调用的服务。（提供接口给其它微服务）

**服务消费者**：一次业务中，调用其它微服务的服务。（调用其它微服务提供的接口）

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011048886.png)



但是，服务提供者与服务消费者的角色并不是绝对的，而是相对于业务而言。

如果服务A调用了服务B，而服务B又调用了服务C，服务B的角色是什么？

- 对于A调用B的业务而言：A是服务消费者，B是服务提供者
- 对于B调用C的业务而言：B是服务消费者，C是服务提供者



因此，服务B既可以是服务提供者，也可以是服务消费者。





## Eureka注册中心

假如我们的服务提供者user-service部署了多个实例，如图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412311652372.png)



大家思考几个问题：

- order-service在发起远程调用的时候，该如何得知user-service实例的ip地址和端口？
- 有多个user-service实例地址，order-service调用时该如何选择？
- order-service如何得知某个user-service实例是否依然健康，是不是已经宕机？



### Eureka的结构

这些问题都需要利用SpringCloud中的注册中心来解决，其中最广为人知的注册中心就是Eureka，其结构如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412311652390.png)

Spring Cloud Eureka实现微服务架构中的服务治理功能，使用 Netflix Eureka 实现服务注册与发现，包含客户端组件和服务端组件。服务治理是微服务架构中最为核心和基础的模块。

Eureka 服务端就是服务注册中心。Eureka 客户端用于处理服务的注册和发现。客户端服务通过注解和参数配置的方式，嵌入在客户端应用程序的代码中， 在应用程序运行时，Eureka客户端向注册中心注册自身提供的服务并周期性地发送心跳来更新它的服务租约。同时，它也能从服务端查询当前注册的服务信息并把它们缓存到本地并周期性地刷新服务状态。



回答之前的各个问题。

问题1：order-service如何得知user-service实例地址？

获取地址信息的流程如下：

- user-service服务实例启动后，将自己的信息注册到eureka-server（Eureka服务端）。这个叫服务注册
- eureka-server保存服务名称到服务实例地址列表的映射关系
- order-service根据服务名称，拉取实例地址列表。这个叫服务发现或服务拉取



问题2：order-service如何从多个user-service实例中选择具体的实例？

- order-service从实例列表中利用负载均衡算法选中一个实例地址
- 向该实例地址发起远程调用



问题3：order-service如何得知某个user-service实例是否依然健康，是不是已经宕机？

- user-service会每隔一段时间（默认30秒）向eureka-server发起请求，报告自己状态，称为心跳
- 当超过一定时间没有发送心跳时，eureka-server会认为微服务实例故障，将该实例从服务列表中剔除
- order-service拉取服务时，就能将故障实例排除了



> 注意：一个微服务，既可以是服务提供者，又可以是服务消费者，因此eureka将服务注册、服务发现等功能统一封装到了eureka-client端

**服务注册**：在微服务架构中往往会有一个注册中心，每个微服务都会向注册中心去注册自己的地址及端口信息，注册中心维护着服务名称与服务实例的对应关系。每个微服务都会定时从注册中心获取服务列表，同时汇报自己的运行情况，这样当有的服务需要调用其他服务时，就可以从自己获取到的服务列表中获取实例地址进行调用。

**服务发现**：服务间的调用不是通过直接调用具体的实例地址，而是通过服务名发起调用。调用方需要向服务注册中心咨询服务，获取服务的实例清单，从而访问具体的服务实例。







因此，接下来我们动手实践的步骤包括：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412311652386.png)



### 搭建eureka-server

首先大家注册中心服务端：eureka-server，这必须是一个独立的微服务

#### 引入eureka依赖

引入SpringCloud为eureka提供的starter依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```



#### 编写启动类

给eureka-server服务编写一个启动类，一定要添加一个@EnableEurekaServer注解，开启eureka的注册中心功能：

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaApplication.class, args);
    }
}
```



#### 编写配置文件

编写一个application.yml文件，内容如下：

```yaml
server:
  port: 10086
spring:
  application:
    name: eureka-server
eureka:
  client:
    service-url: 
      defaultZone: http://127.0.0.1:10086/eureka
```



#### 启动服务

启动微服务，然后在浏览器访问：http://127.0.0.1:10086

看到下面结果就是成功了：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412311856340.png)







### 服务注册

下面，我们将user-service注册到eureka-server中去。

#### 引入依赖

在user-service的pom文件中，引入下面的eureka-client依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```



#### 配置文件

在user-service中，修改application.yml文件，添加服务名称、eureka地址：

```yaml
spring:
  application:
    name: userservice
eureka:
  client:
    service-url:
      defaultZone: http://127.0.0.1:10086/eureka
```



#### 启动多个user-service实例

为了演示一个服务有多个实例的场景，我们添加一个SpringBoot的启动配置，再启动一个user-service。

然后，在弹出的窗口中，填写信息：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412311921052.png)



启动两个user-service实例：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011050783.png)

查看eureka-server管理页面：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412311921516.png)





### 服务发现

下面，我们将order-service的逻辑修改：向eureka-server拉取user-service的信息，实现服务发现。

#### 引入依赖

之前说过，服务发现、服务注册统一都封装在eureka-client依赖，因此这一步与服务注册时一致。

在order-service的pom文件中，引入下面的eureka-client依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```



#### 配置文件

服务发现也需要知道eureka地址，因此第二步与服务注册一致，都是配置eureka信息：

在order-service中，修改application.yml文件，添加服务名称、eureka地址：

```yaml
spring:
  application:
    name: orderservice
eureka:
  client:
    service-url:
      defaultZone: http://127.0.0.1:10086/eureka
```



#### 服务拉取和负载均衡

最后，我们要去eureka-server中拉取user-service服务的实例列表，并且实现负载均衡。

不过这些动作不用我们去做，只需要添加一些注解即可。



在order-service的OrderApplication中，给RestTemplate这个Bean添加一个@LoadBalanced注解：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412311922820.png)



修改order-service服务中的cn.seven.order.service包下的OrderService类中的queryOrderById方法。修改访问的url路径，用服务名代替ip、端口：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412311922382.png)


spring会自动帮助我们从eureka-server端，根据userservice这个服务名称，获取实例列表，而后完成负载均衡。



### 自我保护模式

**什么是自我保护模式**？

1. 自我保护的条件：一般情况下，微服务在 Eureka 上注册后，会每 30 秒发送心跳包，Eureka 通过心跳来判断服务是否健康，同时会定期删除超过 90 秒没有发送心跳服务。
2. 有两种情况会导致 Eureka Server 收不到微服务的心跳
   1. 是微服务自身的原因
   2. 是微服务与 Eureka 之间的网络故障

通常(微服务的自身的故障关闭)只会导致个别服务出现故障，一般不会出现大面积故障，而(网络故障)通常会导致 Eureka Server 在短时间内无法收到大批心跳。考虑到这个区别，Eureka 设置了一个阀值，当判断挂掉的服务的数量超过阀值时，Eureka Server 认为很大程度上出现了网络故障，将不再删除心跳过期的服务。

3. 那么这个阀值是多少呢？

15 分钟之内是否低于 85%；Eureka Server 在运行期间，会统计心跳失败的比例在 15 分钟内是否低于 85%,这种算法叫做 Eureka Server 的自我保护模式。



**为什么要自我保护**？

1. 因为同时保留"好数据"与"坏数据"总比丢掉任何数据要更好，当网络故障恢复后，这个 Eureka 节点会退出"自我保护模式"。

2. Eureka 还有客户端缓存功能(也就是微服务的缓存功能)。即便 Eureka 集群中所有节点都宕机失效，微服务的 Provider 和 Consumer都能正常通信。

3. 微服务的负载均衡策略会自动剔除死亡的微服务节点。



## Ribbon负载均衡

上一节中，我们添加了@LoadBalanced注解，即可实现负载均衡功能，这是什么原理呢？



### 负载均衡原理

SpringCloud底层其实是利用了一个名为Ribbon的组件，来实现负载均衡功能的。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011121618.png)

那么我们发出的请求明明是http://userservice/user/1，怎么变成了http://localhost:8081的呢？



### 什么是 Ribbon

1. Ribbon 是一个基于 Http 和 TCP 的客服端负载均衡工具，它是基于 Netflix Ribbon 实现的。
2. 它不像 spring cloud 服务注册中心、配置中心、API 网关那样独立部署，但是它几乎存在于每个Spring cloud 微服务中。包括 feign 提供的声明式服务调用也是基于该 Ribbon实现的。
3. Ribbon 默认提供很多种负载均衡算法，例如 轮询、随机 等等。甚至包含自定义的负载均衡算法。

在客户端节点会维护可访问的服务器清单，服务器清单来自服务注册中心，通过心跳维持服务器清单的健康性。

开启客户端负载均衡调用：

1. 服务提供者启动多个服务实例注册到服务注册中心；
2. 服务消费者直接通过调用被@LoadBalanced 注解修饰过的RestTemplate 来实现面向服务的接口调用。



#### 集中式与进程内负载均衡的区别

目前业界主流的负载均衡方案可分成两类：

1. 集中式负载均衡, 即在 consumer 和 provider 之间使用独立的负载均衡设施(可以是硬件，如F5, 也可以是软件，如 Nginx), 由该设施负责把 访问请求 通过某种策略转发至 provider；
2. 进程内负载均衡，将负载均衡逻辑集成到 consumer，consumer 从服务注册中心获知有哪些地址可用，然后自己再从这些地址中选择出一个合适的 provider。Ribbon 就属于后者，它只是一个类库，集成于 consumer 进程，consumer 通过它来获取到 provider 的地址。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011128525.png)



### 源码跟踪

为什么我们只输入了service名称就可以访问了呢？之前还要获取ip和端口。

显然有人帮我们根据service名称，获取到了服务实例的ip和端口。它就是`LoadBalancerInterceptor`，这个类会在对RestTemplate的请求进行拦截，然后从Eureka根据服务id获取服务列表，随后利用负载均衡算法得到真实的服务地址信息，替换服务id。

我们进行源码跟踪：

#### LoadBalancerIntercepor

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011122160.png)

可以看到这里的intercept方法，拦截了用户的HttpRequest请求，然后做了几件事：

- `request.getURI()`：获取请求uri，本例中就是 http://user-service/user/8
- `originalUri.getHost()`：获取uri路径的主机名，其实就是服务id，`user-service`
- `this.loadBalancer.execute()`：处理服务id，和用户请求。

这里的`this.loadBalancer`是`LoadBalancerClient`类型，我们继续跟入。



#### LoadBalancerClient

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



#### 总结

SpringCloudRibbon的底层采用了一个拦截器，拦截了RestTemplate发出的请求，对地址做了修改。用一幅图来总结一下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011123623.png)



基本流程如下：

- 拦截我们的RestTemplate请求http://userservice/user/1
- RibbonLoadBalancerClient会从请求url中获取服务名称，也就是user-service
- DynamicServerListLoadBalancer根据user-service到eureka拉取服务列表
- eureka返回列表，localhost:8081、localhost:8082
- IRule利用内置负载均衡规则，从列表中选择一个，例如localhost:8081
- RibbonLoadBalancerClient修改请求地址，用localhost:8081替代userservice，得到http://localhost:8081/user/1，发起真实请求



### 负载均衡策略



#### 负载均衡策略

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



#### 自定义负载均衡策略

通过定义IRule实现可以修改负载均衡规则，有两种方式：

1. 代码方式：在order-service中的OrderApplication类中，定义一个新的IRule：

```java
@Bean
public IRule randomRule(){
    return new RandomRule();
}
```



2. 配置文件方式：在order-service的application.yml文件中，添加新的配置也可以修改规则：

```yaml
userservice: # 给某个微服务配置负载均衡规则，这里是userservice服务
  ribbon:
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule # 负载均衡规则 
```



> **注意**，一般用默认的负载均衡规则，不做修改。



### 饥饿加载

Ribbon默认是采用懒加载，即第一次访问时才会去创建LoadBalanceClient，请求时间会很长。

而饥饿加载则会在项目启动时创建，降低第一次访问的耗时，通过下面配置开启饥饿加载：

```yaml
ribbon:
  eager-load:
    enabled: true
    clients: userservice
```



## Hystrix

在微服务架构中，服务与服务之间通过远程调用的方式进行通信，一旦某个被调用的服务发生了故障，其依赖服务也会发生故障，此时就会发生故障的蔓延，最终导致灾难性雪崩效应。Hystrix实现了断路器模式，当某个服务发生故障时，通过断路器的监控，给调用方返回一个错误响应，而不是长时间的等待，这样就不会使得调用方由于**长时间得不到响应而占用线程**，从而防止故障的蔓延。Hystrix具备服务降级、服务熔断、线程隔离、请求缓存、请求合并及服务监控等强大功能。

### Hystrix介绍

#### 什么是灾难性的雪崩效应

什么是灾难性的雪崩效应?我们通过结构图来说明，如下

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011204426.png)

正常情况下各个节点相互配置，完成用户请求的处理工作

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011204416.png)

当某种请求增多，造成"服务T"故障的情况时，会延伸的造成"服务U"不可用，及继续扩展，如下

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011204437.png)

最终造成下面这种所有服务不可用的情况

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011204455.png)

这就是我们讲的灾难性雪崩，造成雪崩的原因可以归纳为以下三个:

1. 服务提供者不可用(硬件故障，程序Bug，缓存击穿，用户大量请求)
2. 重试加大流量(用户重试，代码逻辑重试)
3. 服务调用者不可用(同步等待造成的资源耗尽)



最终的结果就是一个服务不可用，导致一系列服务的不可用，而往往这种后果是无法预料的。



#### 如何解决灾难性雪崩效应

我们可以通过以下5种方式来解决雪崩效应

1. **降级**：超时降级、资源不足时(线程或信号量)降级，降级后可以配合降级接口返回托底数据。实现一个 fallback 方法, 当请求后端服务出现异常的时候, 可以使用 fallback 方法返回的值.
2. **缓存**：Hystrix 为了降低访问服务的频率，支持将一个请求与返回结果做缓存处理。如果再次请求的 URL 没有变化，那么 Hystrix 不会请求服务，而是直接从缓存中将结果返回。这样可以大大降低访问服务的压力。
3. **请求合并**：在微服务架构中，我们将一个项目拆分成很多个独立的模块，这些独立的模块通过远程调用来互相配合工作，但是，在高并发情况下，通信次数的增加会导致总的通信时间增加，同时，线程池的资源也是有限的，高并发环境会导致有大量的线程处于等待状态，进而导致响应延迟，为了解决这些问题，我们需要来了解 Hystrix 的请求合并。
4. **熔断**：当失败率(如因网络故障/超时造成的失败率高)达到阀值自动触发降级，熔断器触发的快速失败会进行快速恢复。
5. **隔离**（线程池隔离和信号量隔离）
     限制调用分布式服务的资源使用，某一个调用的服务出现问题不会影响其他服务调用。



### 降级

#### 场景介绍

先来看下正常服务调用的情况

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011204468.png)

当consumer调用provider服务出现问题的情况下:

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011204471.png)

此时我们对consumer的服务调用做降级处理

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011204251.png)

####  

#### 实现案例

创建一个基于Ribbon的Consumer服务，并添加对应的依赖

```xml
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
</dependency>
<!-- 添加Hystrix的依赖 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-hystrix</artifactId>
    <version>1.3.2.RELEASE</version>
</dependency>
```



#### 配置文件

```properties
spring.application.name=eureka-consumer-hystrix
server.port=9091

# 设置服务注册中心地址 执行Eureka服务端 如果有多个注册地址 那么用逗号连接
eureka.client.service-url.defaultZone=http://seven:123456@192.168.100.120:8761/eureka/,http://seven:123456@192.168.100.121:8761/eureka/

```



#### 修改启动类

在启动类中添加 开启熔断

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;

@EnableCircuitBreaker // 开启Hystrix的熔断
@SpringBootApplication
public class SpringcloudEurekaConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringcloudEurekaConsumerApplication.class, args);
    }

}
```



#### 业务层修改

业务层代码中的方法是通过Ribbon来获取负载均衡的服务器地址的，通过RestTemplate来调用服务，在方法的头部添加@HystrixCommand注解，通过fallbackMethod属性指定当调用Provider方法异常的时候fallback方法请求返回托底数据

```java
import com.seven.pojo.User;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    /**
     * Ribbon 实现的负载均衡
     *    LocadBalancerClient 通过服务名称可以获取对应服务的相关信息
     *                         ip 端口 等
     */
    @Autowired
    private LoadBalancerClient loadBalancerClient;

    /**
     * 远程调用 服务提供者获取用户信息的方法
     * 1.发现服务
     * 2.调用服务
     */
    @HystrixCommand(fallbackMethod = "fallBack")
    public List<User> getUsers(){
        // 1. 服务发现
        // 获取服务提供者的信息 ServiceInstance封装的有相关的信息
        ServiceInstance instance = loadBalancerClient.choose("eureka-provider");
        StringBuilder sb = new StringBuilder();
        // http://localhost:9090/user
        sb.append("http://")
                .append(instance.getHost())
                .append(":")
                .append(instance.getPort())
                .append("/user");
        System.out.println(sb.toString());
        // 2. 服务调用 SpringMVC中提供的有 调用组件 RestTemplate
        RestTemplate rt = new RestTemplate();
        ParameterizedTypeReference<List<User>> type = new ParameterizedTypeReference<List<User>>() {};
        ResponseEntity<List<User>> response = rt.exchange(sb.toString(), HttpMethod.GET, null, type);
        List<User> list = response.getBody();
        return list;
    }

    /**
     * 托底方法
     * @return
     */
    public List<User> fallBack(){
        List<User> list = new ArrayList<>();
        list.add(new User(333,"我是托底数据",28));
        return list;
    }
}

```



### 缓存

Hystrix 为了**降低访问**服务的**频率**，支持将一个请求与返回结果做**缓存处理**。如果再次请求的 URL 没有变化，那么 Hystrix 不会请求服务，而是直接从缓存中将结果返回。这样可以大大降低访问服务的压力。

Hystrix 自带缓存。有两个缺点：

1. 是一个本地缓存。在集群情况下缓存是不能同步的。
2. 不支持第三方缓存容器。Redis，memcache 不支持的。

所以我们使用Spring的cache。



#### 启动Redis服务

使用Redis作为缓存服务器



#### 添加相关的依赖

因为需要用到SpringDataRedis的支持，需要添加对应的依赖

```xml
<!-- 添加Hystrix的依赖 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-hystrix</artifactId>
    <version>1.3.2.RELEASE</version>
</dependency>
<!-- 添加SpringDataRedis的依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```





#### 修改属性文件

需要在属性文件中添加Redis的配置信息

```properties
spring.application.name=eureka-consumer-hystrix
server.port=9091

# 设置服务注册中心地址 执行Eureka服务端 如果有多个注册地址 那么用逗号连接
eureka.client.service-url.defaultZone=http://seven:123456@192.168.100.120:8761/eureka/,http://seven:123456@192.168.100.121:8761/eureka/
        
# Redis
spring.redis.database=0
#Redis 服务器地址
spring.redis.host=192.168.100.120
#Redis 服务器连接端口
spring.redis.port=6379
#Redis 服务器连接密码（默认为空）
spring.redis.password=
#连接池最大连接数（负值表示没有限制）
spring.redis.pool.max-active=100
#连接池最大阻塞等待时间（负值表示没有限制）
spring.redis.pool.max-wait=3000
#连接池最大空闭连接数
spring.redis.pool.max-idle=200
#连接汉最小空闲连接数
spring.redis.pool.min-idle=50
#连接超时时间（毫秒）
spring.redis.pool.timeout=600

```



#### 修改启动类

需要在启动类中开启缓存的使用

```java
@EnableCaching // 开启缓存
@EnableCircuitBreaker // 开启Hystrix的熔断
@SpringBootApplication
public class SpringcloudEurekaConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringcloudEurekaConsumerApplication.class, args);
    }

}
```



#### 业务处理

```java
import com.seven.pojo.User;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
// cacheNames 当前类中的方法在Redis中添加的Key的前缀
@CacheConfig(cacheNames = {"com.seven.cache"})
public class UserService {


    /**
     * Ribbon 实现的负载均衡
     *    LocadBalancerClient 通过服务名称可以获取对应服务的相关信息
     *                         ip 端口 等
     */
    @Autowired
    private LoadBalancerClient loadBalancerClient;

    /**
     * 远程调用 服务提供者获取用户信息的方法
     * 1.发现服务
     * 2.调用服务
     */
    @HystrixCommand(fallbackMethod = "fallBack")
    public List<User> getUsers(){
        // 1. 服务发现
        // 获取服务提供者的信息 ServiceInstance封装的有相关的信息
        ServiceInstance instance = loadBalancerClient.choose("eureka-provider");
        StringBuilder sb = new StringBuilder();
        // http://localhost:9090/user
        sb.append("http://")
                .append(instance.getHost())
                .append(":")
                .append(instance.getPort())
                .append("/user");
        System.out.println(sb.toString());
        // 2. 服务调用 SpringMVC中提供的有 调用组件 RestTemplate
        RestTemplate rt = new RestTemplate();
        ParameterizedTypeReference<List<User>> type = new ParameterizedTypeReference<List<User>>() {};
        ResponseEntity<List<User>> response = rt.exchange(sb.toString(), HttpMethod.GET, null, type);
        List<User> list = response.getBody();
        return list;

    }

    /**
     * 托底方法
     * @return
     */
    public List<User> fallBack(){
        List<User> list = new ArrayList<>();
        list.add(new User(333,"我是托底数据",28));
        return list;
    }

    @Cacheable(key="'user'+#id")
    public User getUserById(Integer id){
        System.out.println("*************查询操作*************"+ id);
        return new User(id,"缓存测试数据",22);
    }
}

```



使用到了缓存，所以会对POJO对象做持久化处理，所以需要实现序列化接口，否则会抛异常



### 请求合并

#### 没有合并请求的场景

没有合并的场景中，对于provider的调用会非常的频繁，容易造成处理不过来的情况

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011220264.png)



#### 合并请求的场景

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011220271.png)



#### 什么情况下使用请求合并

在微服务架构中，我们将一个项目拆分成很多个独立的模块，这些独立的模块通过远程调用来互相配合工作，但是，在高并发情况下，通信次数的增加会导致总的通信时间增加，同时，线程池的资源也是有限的，高并发环境会导致有大量的线程处于等待状态，进而导致响应延迟，为了解决这些问题，我们需要来了解 Hystrix 的请求合并。

#### 请求合并的缺点

设置请求合并之后，本来一个请求可能 5ms 就搞定了，但是现在必须再等 10ms 看看还有没有其他的请求一起的，这样一个请求的耗时就从 5ms 增加到 15ms 了，不过，如果我们要发起的命令本身就是一个高延迟的命令，那么这个时候就可以使用请求合并了，因为这个时候时间窗的时间消耗就显得微不足道了，另外高并发也是请求合并的一个非常重要的场景。





#### 案例实现

业务处理代码

```java
import com.seven.pojo.User;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCollapser;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

@Service
// cacheNames 当前类中的方法在Redis中添加的Key的前缀
@CacheConfig(cacheNames = {"com.seven.cache"})
public class UserService {


    /**
     * Ribbon 实现的负载均衡
     *    LocadBalancerClient 通过服务名称可以获取对应服务的相关信息
     *                         ip 端口 等
     */
    @Autowired
    private LoadBalancerClient loadBalancerClient;

    /**
     * 远程调用 服务提供者获取用户信息的方法
     * 1.发现服务
     * 2.调用服务
     * @return
     */
    @HystrixCommand(fallbackMethod = "fallBack")
    public List<User> getUsers(){
        // 1. 服务发现
        // 获取服务提供者的信息 ServiceInstance封装的有相关的信息
        ServiceInstance instance = loadBalancerClient.choose("eureka-provider");
        StringBuilder sb = new StringBuilder();
        // http://localhost:9090/user
        sb.append("http://")
                .append(instance.getHost())
                .append(":")
                .append(instance.getPort())
                .append("/user");
        System.out.println(sb.toString());
        // 2. 服务调用 SpringMVC中提供的有 调用组件 RestTemplate
        RestTemplate rt = new RestTemplate();
        ParameterizedTypeReference<List<User>> type = new ParameterizedTypeReference<List<User>>() {};
        ResponseEntity<List<User>> response = rt.exchange(sb.toString(), HttpMethod.GET, null, type);
        List<User> list = response.getBody();
        return list;

    }

    /**
     * 托底方法
     * @return
     */
    public List<User> fallBack(){
        List<User> list = new ArrayList<>();
        list.add(new User(333,"我是托底数据",28));
        return list;
    }

    @Cacheable(key="'user'+#id")
    public User getUserById(Integer id){
        System.out.println("*************查询操作*************"+ id);
        return new User(id,"缓存测试数据",22);
    }

    /**
     * Consumer中的Controller要调用的方法
     * 这个方法的返回值必须是 Future 类型
     *    利用Hystrix 合并请求
     */
    @HystrixCollapser(
            batchMethod = "batchUser"
            ,scope = com.netflix.hystrix.HystrixCollapser.Scope.GLOBAL
            ,collapserProperties = {
                    // 请求时间间隔在20ms以内的请求会被合并，默认值是10ms
                    @HystrixProperty(name = "timerDelayInMilliseconds",value = "20")
                    // 设置触发批处理执行之前 在批处理中允许的最大请求数
                    ,@HystrixProperty(name = "maxRequestsInBatch",value = "200")
    }
    )
    public Future<User> getUserId(Integer id){
        System.out.println("*****id*****");
        return null;
    }

    @HystrixCommand
    public List<User> batchUser(List<Integer> ids){
        for (Integer id : ids) {
            System.out.println(id);
        }
        List<User> list = new ArrayList<>();
        list.add(new User(1,"张三1",18));
        list.add(new User(2,"张三2",18));
        list.add(new User(3,"张三3",18));
        list.add(new User(4,"张三4",18));
        return list;
    }
}

```

控制器处理

```java
@RequestMapping("/getUserId")
public void getUserId() throws Exception{
        Future<User> f1 = service.getUserId(1);
        Future<User> f2 = service.getUserId(1);
        Future<User> f3 = service.getUserId(1);

        System.out.println("*************************");
        System.out.println(f1.get().toString());
        System.out.println(f2.get().toString());
        System.out.println(f3.get().toString());
}
```



![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011221884.png)



### 熔断

熔断其实是在降级的基础上引入了重试的机制。当某个时间内失败的次数达到了多少次就会触发熔断机制，具体的流程如下

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011222557.png)



案例核心代码

```java
    @HystrixCommand(fallbackMethod = "fallback",
            commandProperties = {
                    //默认 20 个;10s 内请求数大于 20 个时就启动熔断器，当请求符合熔断条件时将触发 getFallback()。
                    @HystrixProperty(name= HystrixPropertiesManager.CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD,
                            value="10"),
                    //请求错误率大于 50%时就熔断，然后 for 循环发起请求，当请求符合熔断条件时将触发 getFallback()。
                    @HystrixProperty(name=HystrixPropertiesManager.CIRCUIT_BREAKER_ERROR_THRESHOLD_PERCENTAGE,
                            value="50"),
                    //默认 5 秒;熔断多少秒后去尝试请求
                    @HystrixProperty(name=HystrixPropertiesManager.CIRCUIT_BREAKER_SLEEP_WINDOW_IN_MILLISECONDS,
                            value="5000"),
            })
    public List<User> getUsers(){
        // 1. 服务发现
        // 获取服务提供者的信息 ServiceInstance封装的有相关的信息
        ServiceInstance instance = loadBalancerClient.choose("eureka-provider");
        StringBuilder sb = new StringBuilder();
        // http://localhost:9090/user
        sb.append("http://")
                .append(instance.getHost())
                .append(":")
                .append(instance.getPort())
                .append("/user");
        System.out.println("---->"+sb.toString());
        // 2. 服务调用 SpringMVC中提供的有 调用组件 RestTemplate
        RestTemplate rt = new RestTemplate();
        ParameterizedTypeReference<List<User>> type = new ParameterizedTypeReference<List<User>>() {};
        ResponseEntity<List<User>> response = rt.exchange(sb.toString(), HttpMethod.GET, null, type);
        List<User> list = response.getBody();
        return list;

    }
```





![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011222131.png)



### 隔离

在应对服务雪崩效应时，除了前面介绍的降级，缓存，请求合并及熔断外还有一种方式就是隔离，隔离又分为线程池隔离和信号量隔离。接下来我们分别来介绍。

#### 线程池隔离

##### 概念介绍

我们通过以下几个图片来解释线程池隔离到底是怎么回事

在没有使用线程池隔离时：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011224219.png)

当接口A压力增大，接口C同时也会受到影响

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011224212.png)



使用线程池的场景

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011224230.png)

当服务接口A访问量增大时，因为接口C在不同的线程池中所以不会受到影响

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011224239.png)

通过上面的图片来看，线程池隔离的作用还是蛮明显的。但线程池隔离的使用也不是在任何场景下都适用的，线程池隔离的优缺点如下:

**优点**

1. 使用线程池隔离可以完全隔离依赖的服务(例如图中的A，B，C服务)，请求线程可以快速放回
2. 当线程池出现问题时，线程池隔离是独立的不会影响其他服务和接口
3. 当失败的服务再次变得可用时，线程池将清理并可立即恢复，而不需要一个长时间的恢复
4. 独立的线程池提高了并发性



**缺点**：线程池隔离的主要缺点是它们增加计算开销(CPU)，每个命令的执行涉及到排队，调度和上下文切换都是在一个单独的线程上运行的。



##### 案例实现

```java
    @HystrixCommand(
            groupKey = "eureka-provider"
            ,threadPoolKey = "getUsers"
            ,threadPoolProperties = {
                    @HystrixProperty(name = "coreSize",value = "30") // 线程池大小
                    ,@HystrixProperty(name = "maxQueueSize",value = "100") // 最大队列长度
                    ,@HystrixProperty(name = "keepAliveTimeMinutes",value = "2") // 线程存活时间
                    ,@HystrixProperty(name = "queueSizeRejectionThreshold",value = "15") // 拒绝请求
            },fallbackMethod = "fallBack"
    )
    public List<User> getUsersThreadPool(Integer id){
        System.out.println("--------》" + Thread.currentThread().getName());
        // 1. 服务发现
        // 获取服务提供者的信息 ServiceInstance封装的有相关的信息
        ServiceInstance instance = loadBalancerClient.choose("eureka-provider");
        StringBuilder sb = new StringBuilder();
        // http://localhost:9090/user
        sb.append("http://")
                .append(instance.getHost())
                .append(":")
                .append(instance.getPort())
                .append("/user");
        System.out.println("---->"+sb.toString());
        // 2. 服务调用 SpringMVC中提供的有 调用组件 RestTemplate
        RestTemplate rt = new RestTemplate();
        ParameterizedTypeReference<List<User>> type = new ParameterizedTypeReference<List<User>>() {};
        ResponseEntity<List<User>> response = rt.exchange(sb.toString(), HttpMethod.GET, null, type);
        List<User> list = response.getBody();
        return list;

    }
```

相关参数的描述

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011226789.png)



#### 信号量隔离

信号量隔离其实就是我们定义的队列并发时最多支持多大的访问，其他的访问通过托底数据来响应，如下结构图

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011226776.png)



```java
    @HystrixCommand(
            fallbackMethod = "fallBack"
            ,commandProperties = {
                    @HystrixProperty(name=HystrixPropertiesManager.EXECUTION_ISOLATION_STRATEGY
                            ,value = "SEMAPHORE") // 信号量隔离
                    ,@HystrixProperty(name=HystrixPropertiesManager.EXECUTION_ISOLATION_SEMAPHORE_MAX_CONCURRENT_REQUESTS
            ,value="100" // 信号量最大并发度
    )
    }
    )
    public List<User> getUsersSignal(Integer id){
        System.out.println("--------》" + Thread.currentThread().getName());
        // 1. 服务发现
        // 获取服务提供者的信息 ServiceInstance封装的有相关的信息
        ServiceInstance instance = loadBalancerClient.choose("eureka-provider");
        StringBuilder sb = new StringBuilder();
        // http://localhost:9090/user
        sb.append("http://")
                .append(instance.getHost())
                .append(":")
                .append(instance.getPort())
                .append("/user");
        System.out.println("---->"+sb.toString());
        // 2. 服务调用 SpringMVC中提供的有 调用组件 RestTemplate
        RestTemplate rt = new RestTemplate();
        ParameterizedTypeReference<List<User>> type = new ParameterizedTypeReference<List<User>>() {};
        ResponseEntity<List<User>> response = rt.exchange(sb.toString(), HttpMethod.GET, null, type);
        List<User> list = response.getBody();
        return list;

    }
```



![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011226781.png)



#### 两者的区别

线程池隔离和信号量隔离的区别

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011226809.png)







## Feign远程调用

基于Netflix Feign 实现，整合了Spring Cloud Ribbon 与Spring Cloud Hystrix， 它提供了一种声明式服务调用的方式。



先来看我们以前利用RestTemplate发起远程调用的代码：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011141062.png)

存在下面的问题：

- 代码可读性差，编程体验不统一
- 参数复杂URL难以维护

Feign是一个声明式的http客户端，官方地址：https://github.com/OpenFeign/feign

其作用就是帮助我们优雅的实现http请求的发送，解决上面提到的问题。



**什么是声明式，有什么作用，解决什么问题**？声明式调用就像调用本地方法一样调用远程方法;无感知远程 http 请求。

1. Spring Cloud 的声明式调用, 可以做到使用 HTTP 请求远程服务时能就像调用本地方法一样的体验，开发者完全感知不到这是远程方法，更感知不到这是个 HTTP 请求。
2. 它像 Dubbo 一样，consumer 直接调用接口方法调用 provider，而不需要通过常规的Http Client 构造请求再解析返回数据。
3. 它解决了让开发者调用远程接口就跟调用本地方法一样，无需关注与远程的交互细节，更无需关注分布式环境开发。



### Feign替代RestTemplate

Fegin的使用步骤如下：

#### 引入依赖

我们在order-service服务的pom文件中引入feign的依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```



#### 添加注解

在order-service的启动类添加注解开启Feign的功能，@EnableFeignClients



#### 编写Feign的客户端

在order-service中新建一个接口，内容如下：

```java
import cn.seven.order.pojo.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("userservice")
public interface UserClient {
    @GetMapping("/user/{id}")
    User findById(@PathVariable("id") Long id);
}
```



这个客户端主要是基于SpringMVC的注解来声明远程调用的信息，比如：

- 服务名称：userservice
- 请求方式：GET
- 请求路径：/user/{id}
- 请求参数：Long id
- 返回值类型：User

这样，Feign就可以帮助我们发送http请求，无需自己使用RestTemplate来发送了。



#### 测试

修改order-service中的OrderService类中的queryOrderById方法，使用Feign客户端代替RestTemplate：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011143736.png)

是不是看起来优雅多了。





#### 总结

使用Feign的步骤：

1. 引入依赖
2. 添加@EnableFeignClients注解
3. 编写FeignClient接口
4. 使用FeignClient中定义的方法代替RestTemplate



### 自定义配置

Feign可以支持很多的自定义配置，如下表所示：

| 类型                | 作用             | 说明                                                   |
| ------------------- | ---------------- | ------------------------------------------------------ |
| feign.Logger.Level  | 修改日志级别     | 包含四种不同的级别：NONE、BASIC、HEADERS、FULL         |
| feign.codec.Decoder | 响应结果的解析器 | http远程调用的结果做解析，例如解析json字符串为java对象 |
| feign.codec.Encoder | 请求参数编码     | 将请求参数编码，便于通过http请求发送                   |
| feign. Contract     | 支持的注解格式   | 默认是SpringMVC的注解                                  |
| feign. Retryer      | 失败重试机制     | 请求失败的重试机制，默认是没有，不过会使用Ribbon的重试 |

一般情况下，默认值就能满足我们使用，如果要自定义时，只需要创建自定义的@Bean覆盖默认Bean即可。



下面以日志为例来演示如何自定义配置。

#### 配置文件方式

基于配置文件修改feign的日志级别可以针对单个服务：

```yaml
feign:  
  client:
    config: 
      userservice: # 针对某个微服务的配置
        loggerLevel: FULL #  日志级别 
```

也可以针对所有服务：

```yaml
feign:  
  client:
    config: 
      default: # 这里用default就是全局配置，如果是写服务名称，则是针对某个微服务的配置
        loggerLevel: FULL #  日志级别 
```



而日志的级别分为四种：

- NONE：不记录任何日志信息，这是默认值。
- BASIC：仅记录请求的方法，URL以及响应状态码和执行时间
- HEADERS：在BASIC的基础上，额外记录了请求和响应的头信息
- FULL：记录所有请求和响应的明细，包括头信息、请求体、元数据。



#### Java代码方式

也可以基于Java代码来修改日志级别，先声明一个类，然后声明一个Logger.Level的对象：

```java
public class DefaultFeignConfiguration  {
    @Bean
    public Logger.Level feignLogLevel(){
        return Logger.Level.BASIC; // 日志级别为BASIC
    }
}
```



如果要**全局生效**，将其放到启动类的@EnableFeignClients这个注解中：

```java
@EnableFeignClients(defaultConfiguration = DefaultFeignConfiguration .class) 
```



如果是**局部生效**，则把它放到对应的@FeignClient这个注解中：

```java
@FeignClient(value = "userservice", configuration = DefaultFeignConfiguration .class) 
```







### Feign使用优化

Feign底层发起http请求，依赖于其它的框架。其底层客户端实现包括：

- URLConnection：默认实现，不支持连接池
- Apache HttpClient ：支持连接池
- OKHttp：支持连接池



因此提高Feign的性能主要手段就是使用**连接池**代替默认的URLConnection。



这里我们用Apache的HttpClient来演示。

#### 引入依赖

在order-service的pom文件中引入Apache的HttpClient依赖：

```xml
<!--httpClient的依赖 -->
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-httpclient</artifactId>
</dependency>
```



#### 配置连接池

在order-service的application.yml中添加配置：

```yaml
feign:
  client:
    config:
      default: # default全局的配置
        loggerLevel: BASIC # 日志级别，BASIC就是基本的请求和响应信息
  httpclient:
    enabled: true # 开启feign对HttpClient的支持
    max-connections: 200 # 最大的连接数
    max-connections-per-route: 50 # 每个路径的最大连接数
```



接下来，在FeignClientFactoryBean中的loadBalance方法中打断点：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011143731.png)

Debug方式启动order-service服务，可以看到这里的client，底层就是Apache HttpClient：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011143728.png)







总结，Feign的优化：

1. 日志级别尽量用basic
2. 使用HttpClient或OKHttp代替URLConnection
   1. 引入feign-httpClient依赖
   2. 配置文件开启httpClient功能，设置连接池参数



### 最佳实践

所谓最佳实践，就是使用过程中总结的经验，最好的一种使用方式。

Feign的客户端与服务提供者的controller代码非常相似：

feign客户端：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011143747.png)

UserController：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011143749.png)



有没有一种办法简化这种重复的代码编写呢？





#### 继承方式

一样的代码可以通过继承来共享：

1. 定义一个API接口，利用定义方法，并基于SpringMVC注解做声明。
2. Feign客户端和Controller都集成改接口



![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011143756.png)



优点：

- 简单
- 实现了代码共享

缺点：

- 服务提供方、服务消费方紧耦合

- 参数列表中的注解映射并不会继承，因此Controller中必须再次声明方法、参数列表、注解





#### 抽取方式

将Feign的Client抽取为独立模块，并且把接口有关的POJO、默认的Feign配置都放到这个模块中，提供给所有消费者使用。

例如，将UserClient、User、Feign的默认配置都抽取到一个feign-api包中，所有微服务引用该依赖包，即可直接使用。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011143329.png)



##### 抽取

首先创建一个module，命名为feign-api，

项目结构：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011144035.png)

在feign-api中然后引入feign的starter依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```



然后，order-service中编写的UserClient、User、DefaultFeignConfiguration都复制到feign-api项目中



##### 在order-service中使用feign-api

首先，删除order-service中的UserClient、User、DefaultFeignConfiguration等类或接口。



在order-service的pom文件中中引入feign-api的依赖：

```xml
<dependency>
    <groupId>cn.seven.demo</groupId>
    <artifactId>feign-api</artifactId>
    <version>1.0</version>
</dependency>
```



修改order-service中的所有与上述三个组件有关的导包部分，改成导入feign-api中的包

##### 重启测试

重启后，发现服务报错了

这是因为UserClient现在在cn.seven.feign.clients包下，

而order-service的@EnableFeignClients注解是在cn.seven.order包下，不在同一个包，无法扫描到UserClient。



##### 解决扫描包问题

方式一：

指定Feign应该扫描的包：

```java
@EnableFeignClients(basePackages = "cn.seven.feign.clients")
```



方式二：

指定需要加载的Client接口：

```java
@EnableFeignClients(clients = {UserClient.class})
```



## Gateway服务网关

Spring Cloud Gateway 项目是基于 Spring 5.0，Spring Boot 2.0 和 Project Reactor 等响应式编程和事件流技术开发的网关，它旨在为微服务架构提供一种简单有效的统一的 API 路由管理方式。



### 为什么需要网关

Gateway网关是我们服务的守门神，所有微服务的统一入口。

网关的**核心功能特性**：

- 请求路由
- 权限控制
- 限流

架构图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011229678.png)



- **权限控制**：网关作为微服务入口，需要校验用户是是否有请求资格，如果没有则进行拦截。
- **路由和负载均衡**：一切请求都必须先经过gateway，但网关不处理业务，而是根据某种规则，把请求转发到某个微服务，这个过程叫做路由。当然路由的目标服务有多个时，还需要做负载均衡。
- **限流**：当请求流量过高时，在网关中按照下流的微服务能够接受的速度来放行请求，避免服务压力过大。



在SpringCloud中网关的实现包括两种：

- gateway
- zuul

Zuul是基于Servlet的实现，属于阻塞式编程。而SpringCloudGateway则是基于Spring5中提供的WebFlux，属于响应式编程的实现，具备更好的性能。





### gateway快速入门

下面，我们就演示下网关的基本路由功能。基本步骤如下：

1. 创建SpringBoot工程gateway，引入网关依赖
2. 编写启动类
3. 编写基础配置和路由规则
4. 启动网关服务进行测试



#### 引入依赖

```xml
<!--网关-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<!--nacos服务发现依赖-->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```



#### 编写启动类

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}
}
```



#### 编写基础配置和路由规则

创建application.yml文件，内容如下：

```yaml
server:
  port: 10010 # 网关端口
spring:
  application:
    name: gateway # 服务名称
  cloud:
    nacos:
      server-addr: localhost:8848 # nacos地址
    gateway:
      routes: # 网关路由配置
        - id: user-service # 路由id，自定义，只要唯一即可
          # uri: http://127.0.0.1:8081 # 路由的目标地址 http就是固定地址
          uri: lb://userservice # 路由的目标地址 lb就是负载均衡，后面跟服务名称
          predicates: # 路由断言，也就是判断请求是否符合路由规则的条件
            - Path=/user/** # 这个是按照路径匹配，只要以/user/开头就符合要求
```



我们将符合`Path` 规则的一切请求，都代理到 `uri`参数指定的地址。

本例中，我们将 `/user/**`开头的请求，代理到`lb://userservice`，lb是负载均衡，根据服务名拉取服务列表，实现负载均衡。



#### 重启测试

重启网关，访问http://localhost:10010/user/1时，符合`/user/**`规则，请求转发到uri：http://userservice/user/1，得到了结果：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011231024.png)





#### 网关路由的流程图

整个访问的流程如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011231026.png)



总结：

网关搭建步骤：

1. 创建项目，引入nacos服务发现和gateway依赖

2. 配置application.yml，包括服务基本信息、nacos地址、路由

路由配置包括：

1. 路由id：路由的唯一标示

2. 路由目标（uri）：路由的目标地址，http代表固定地址，lb代表根据服务名负载均衡

3. 路由断言（predicates）：判断路由的规则，

4. 路由过滤器（filters）：对请求或响应做处理

接下来，就重点来学习路由断言和路由过滤器的详细知识





### 断言工厂

我们在配置文件中写的断言规则只是字符串，这些字符串会被Predicate Factory读取并处理，转变为路由判断的条件

例如Path=/user/**是按照路径匹配，这个规则是由`org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory`类来处理的，像这样的断言工厂在SpringCloudGateway还有十几个:

| **名称**   | **说明**                       | **示例**                                                     |
| ---------- | ------------------------------ | ------------------------------------------------------------ |
| After      | 是某个时间点后的请求           | -  After=2037-01-20T17:42:47.789-07:00[America/Denver]       |
| Before     | 是某个时间点之前的请求         | -  Before=2031-04-13T15:14:47.433+08:00[Asia/Shanghai]       |
| Between    | 是某两个时间点之前的请求       | -  Between=2037-01-20T17:42:47.789-07:00[America/Denver],  2037-01-21T17:42:47.789-07:00[America/Denver] |
| Cookie     | 请求必须包含某些cookie         | - Cookie=chocolate, ch.p                                     |
| Header     | 请求必须包含某些header         | - Header=X-Request-Id, \d+                                   |
| Host       | 请求必须是访问某个host（域名） | -  Host=**.somehost.org,**.anotherhost.org                   |
| Method     | 请求方式必须是指定方式         | - Method=GET,POST                                            |
| Path       | 请求路径必须符合指定规则       | - Path=/red/{segment},/blue/**                               |
| Query      | 请求参数必须包含指定参数       | - Query=name, Jack或者-  Query=name                          |
| RemoteAddr | 请求者的ip必须是指定范围       | - RemoteAddr=192.168.1.1/24                                  |
| Weight     | 权重处理                       |                                                              |

我们只需要掌握Path这种路由工程就可以了。



### 过滤器工厂

GatewayFilter是网关中提供的一种过滤器，可以对进入网关的请求和微服务返回的响应做处理：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011231031.png)



#### 路由过滤器的种类

Spring提供了31种不同的路由过滤器工厂。例如：

| **名称**             | **说明**                     |
| -------------------- | ---------------------------- |
| AddRequestHeader     | 给当前请求添加一个请求头     |
| RemoveRequestHeader  | 移除请求中的一个请求头       |
| AddResponseHeader    | 给响应结果中添加一个响应头   |
| RemoveResponseHeader | 从响应结果中移除有一个响应头 |
| RequestRateLimiter   | 限制请求的流量               |



#### 请求头过滤器

下面我们以AddRequestHeader 为例来讲解。

> **需求**：给所有进入userservice的请求添加一个请求头：Truth=seven is freaking awesome!



只需要修改gateway服务的application.yml文件，添加路由过滤即可：

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: user-service 
        uri: lb://userservice 
        predicates: 
        - Path=/user/** 
        filters: # 过滤器
        - AddRequestHeader=Truth, seven is freaking awesome! # 添加请求头
```

当前过滤器写在userservice路由下，因此仅仅对访问userservice的请求有效。





#### 默认过滤器

如果要对所有的路由都生效，则可以将过滤器工厂写到default下。格式如下：

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: user-service 
        uri: lb://userservice 
        predicates: 
        - Path=/user/**
      default-filters: # 默认过滤项
      - AddRequestHeader=Truth, seven is freaking awesome! 
```



#### 总结

过滤器的作用是什么？

1. 对路由的请求或响应做加工处理，比如添加请求头
2. 配置在路由下的过滤器只对当前路由的请求生效

defaultFilters的作用是什么？对所有路由都生效的过滤器



### 全局过滤器

过滤器，网关提供了31种，但每一种过滤器的作用都是固定的。如果我们希望拦截请求，做自己的业务逻辑则没办法实现。

#### 全局过滤器作用

全局过滤器的作用也是处理一切进入网关的请求和微服务响应，与GatewayFilter的作用一样。区别在于GatewayFilter通过配置定义，处理逻辑是固定的；而GlobalFilter的逻辑需要自己写代码实现。

定义方式是实现GlobalFilter接口。

```java
public interface GlobalFilter {
    /**
     *  处理当前请求，有必要的话通过{@link GatewayFilterChain}将请求交给下一个过滤器处理
     *
     * @param exchange 请求上下文，里面可以获取Request、Response等信息
     * @param chain 用来把请求委托给下一个过滤器 
     * @return {@code Mono<Void>} 返回标示当前过滤器业务结束
     */
    Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain);
}
```



在filter中编写自定义逻辑，可以实现下列功能：

- 登录状态判断
- 权限校验
- 请求限流等



#### 自定义全局过滤器

需求：定义全局过滤器，拦截请求，判断请求的参数是否满足下面条件：

- 参数中是否有authorization，

- authorization参数值是否为admin

如果同时满足则放行，否则拦截



实现：在gateway中定义一个过滤器：

```java
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Order(-1)
@Component
public class AuthorizeFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1.获取请求参数
        MultiValueMap<String, String> params = exchange.getRequest().getQueryParams();
        // 2.获取authorization参数
        String auth = params.getFirst("authorization");
        // 3.校验
        if ("admin".equals(auth)) {
            // 放行
            return chain.filter(exchange);
        }
        // 4.拦截
        // 4.1.禁止访问，设置状态码
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        // 4.2.结束处理
        return exchange.getResponse().setComplete();
    }
}
```





#### 过滤器执行顺序

请求进入网关会碰到三类过滤器：当前路由的过滤器、DefaultFilter、GlobalFilter

请求路由后，会将当前路由过滤器和DefaultFilter、GlobalFilter，合并到一个过滤器链（集合）中，排序后依次执行每个过滤器：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011231035.png)



排序的规则是什么呢？

- 每一个过滤器都必须指定一个int类型的order值，**order值越小，优先级越高，执行顺序越靠前**。
- GlobalFilter通过实现Ordered接口，或者添加@Order注解来指定order值，由我们自己指定
- 路由过滤器和defaultFilter的order由Spring指定，默认是按照声明顺序从1递增。
- 当过滤器的order值一样时，会按照 defaultFilter > 路由过滤器 > GlobalFilter的顺序执行。



详细内容，可以查看源码：

`org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator#getFilters()`方法是先加载defaultFilters，然后再加载某个route的filters，然后合并。

`org.springframework.cloud.gateway.handler.FilteringWebHandler#handle()`方法会加载全局过滤器，与前面的过滤器合并后根据order排序，组织过滤器链



### 跨域问题



#### 什么是跨域问题

跨域：域名不一致就是跨域，主要包括：

- 域名不同： www.taobao.com 和 www.taobao.org 和 www.jd.com 和 miaosha.jd.com

- 域名相同，端口不同：localhost:8080和localhost:8081

跨域问题：浏览器禁止请求的发起者与服务端发生跨域ajax请求，请求被浏览器拦截的问题



解决方案：CORS，这里不再赘述了，不知道的小伙伴可以查看https://www.ruanyifeng.com/blog/2016/04/cors.html



#### 模拟跨域问题

可以在浏览器控制台看到下面的错误：

![image-20210714215832675](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011231045.png)



从localhost:8090访问localhost:10010，端口不同，显然是跨域的请求。



#### 解决跨域问题

在gateway服务的application.yml文件中，添加下面的配置：

```yaml
spring:
  cloud:
    gateway:
      # ...
      globalcors: # 全局的跨域处理
        add-to-simple-url-handler-mapping: true # 解决options请求被拦截问题
        corsConfigurations:
          '[/**]':
            allowedOrigins: # 允许哪些网站的跨域请求 
              - "http://localhost:8090"
            allowedMethods: # 允许的跨域ajax的请求方式
              - "GET"
              - "POST"
              - "DELETE"
              - "PUT"
              - "OPTIONS"
            allowedHeaders: "*" # 允许在请求中携带的头信息
            allowCredentials: true # 是否允许携带cookie
            maxAge: 360000 # 这次跨域检测的有效期
```










