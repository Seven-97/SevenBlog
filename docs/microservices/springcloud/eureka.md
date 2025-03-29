---
title: Eureka注册中心
category: 微服务
tags:
  - SpringCloud
  - Eureka
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,SpringCloud,Eureka,服务注册,服务发现
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---




假如我们的服务提供者user-service部署了多个实例，如图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412311652372.png)



大家思考几个问题：

- order-service在发起远程调用的时候，该如何得知user-service实例的ip地址和端口？
- 有多个user-service实例地址，order-service调用时该如何选择？
- order-service如何得知某个user-service实例是否依然健康，是不是已经宕机？



## Eureka的结构

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



## 搭建eureka-server

首先大家注册中心服务端：eureka-server，这必须是一个独立的微服务

### 引入eureka依赖

引入SpringCloud为eureka提供的starter依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```



### 编写启动类

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



### 编写配置文件

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



### 启动服务

启动微服务，然后在浏览器访问：http://127.0.0.1:10086

看到下面结果就是成功了：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412311856340.png)







## 服务注册

下面，我们将user-service注册到eureka-server中去。

#### 引入依赖

在user-service的pom文件中，引入下面的eureka-client依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```



### 配置文件

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



### 启动多个user-service实例

为了演示一个服务有多个实例的场景，我们添加一个SpringBoot的启动配置，再启动一个user-service。

然后，在弹出的窗口中，填写信息：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412311921052.png)



启动两个user-service实例：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011050783.png)

查看eureka-server管理页面：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412311921516.png)





## 服务发现

下面，我们将order-service的逻辑修改：向eureka-server拉取user-service的信息，实现服务发现。

### 引入依赖

之前说过，服务发现、服务注册统一都封装在eureka-client依赖，因此这一步与服务注册时一致。

在order-service的pom文件中，引入下面的eureka-client依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```



### 配置文件

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



### 服务拉取和负载均衡

最后，我们要去eureka-server中拉取user-service服务的实例列表，并且实现负载均衡。

不过这些动作不用我们去做，只需要添加一些注解即可。



在order-service的OrderApplication中，给RestTemplate这个Bean添加一个@LoadBalanced注解：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412311922820.png)



修改order-service服务中的cn.seven.order.service包下的OrderService类中的queryOrderById方法。修改访问的url路径，用服务名代替ip、端口：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412311922382.png)


spring会自动帮助我们从eureka-server端，根据userservice这个服务名称，获取实例列表，而后完成负载均衡。



## 自我保护模式

**什么是自我保护模式**？

1. 自我保护的条件：一般情况下，微服务在 Eureka 上注册后，会每 30 秒发送心跳包，Eureka 通过心跳来判断服务是否健康，同时会定期删除超过 90 秒没有发送心跳服务。
2. 有两种情况会导致 Eureka Server 收不到微服务的心跳
   1. 是微服务自身的原因
   2. 是微服务与 Eureka 之间的网络故障

通常(微服务的自身的故障关闭)只会导致个别服务出现故障，一般不会出现大面积故障，而(网络故障)通常会导致 Eureka Server 在短时间内无法收到大批心跳。考虑到这个区别，Eureka 设置了一个阀值，当判断挂掉的服务的数量超过阀值时，Eureka Server 认为很大程度上出现了网络故障，将不再删除心跳过期的服务。

1. 那么这个阀值是多少呢？

15 分钟之内是否低于 85%；Eureka Server 在运行期间，会统计心跳失败的比例在 15 分钟内是否低于 85%,这种算法叫做 Eureka Server 的自我保护模式。



**为什么要自我保护**？

2. 因为同时保留"好数据"与"坏数据"总比丢掉任何数据要更好，当网络故障恢复后，这个 Eureka 节点会退出"自我保护模式"。

3. Eureka 还有客户端缓存功能(也就是微服务的缓存功能)。即便 Eureka 集群中所有节点都宕机失效，微服务的 Provider 和 Consumer都能正常通信。

4. 微服务的负载均衡策略会自动剔除死亡的微服务节点。

