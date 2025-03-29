---
title: 服务拆分与远程调用
category: 微服务
tags:
  - SpringCloud
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,SpringCloud,服务注册,服务发现
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---




任何分布式架构都离不开服务的拆分，微服务也是一样。

## 服务拆分原则

这里我总结了微服务拆分时的几个原则：

- 不同微服务，不要重复开发相同业务
- 微服务数据独立，不要访问其它微服务的数据库
- 微服务可以将自己的业务暴露为接口，供其它微服务调用

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011046433.png)



## 服务拆分示例

以课前资料中的微服务cloud-demo为例，其结构如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011046914.png)

cloud-demo：父工程，管理依赖

- order-service：订单微服务，负责订单相关业务
- user-service：用户微服务，负责用户相关业务

要求：

- 订单微服务和用户微服务都必须有各自的数据库，相互独立
- 订单服务和用户服务都对外暴露Restful的接口
- 订单服务如果需要查询用户信息，只能调用用户服务的Restful接口，不能查询用户数据库



## 实现远程调用案例



在order-service服务中，有一个根据id查询订单的接口：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011046792.png)

根据id查询订单，返回值是Order对象，如图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011047800.png)

其中的user为null





在user-service中有一个根据id查询用户的接口：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011047452.png)

查询的结果如图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011047375.png)





### 案例需求：

修改order-service中的根据id查询订单业务，要求在查询订单的同时，根据订单中包含的userId查询出用户信息，一起返回。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011047636.png)



因此，我们需要在order-service中 向user-service发起一个http的请求，调用http://localhost:8081/user/{userId}这个接口。

大概的步骤是这样的：

- 注册一个RestTemplate的实例到Spring容器
- 修改order-service服务中的OrderService类中的queryOrderById方法，根据Order对象中的userId查询User
- 将查询的User填充到Order对象，一起返回



### 注册RestTemplate

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



### 实现远程调用

修改order-service服务中的cn.seven.order.service包下的OrderService类中的queryOrderById方法：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011048194.png)







## 提供者与消费者

在服务调用关系中，会有两个不同的角色：

**服务提供者**：一次业务中，被其它微服务调用的服务。（提供接口给其它微服务）

**服务消费者**：一次业务中，调用其它微服务的服务。（调用其它微服务提供的接口）

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011048886.png)



但是，服务提供者与服务消费者的角色并不是绝对的，而是相对于业务而言。

如果服务A调用了服务B，而服务B又调用了服务C，服务B的角色是什么？

- 对于A调用B的业务而言：A是服务消费者，B是服务提供者
- 对于B调用C的业务而言：B是服务消费者，C是服务提供者



因此，服务B既可以是服务提供者，也可以是服务消费者。

