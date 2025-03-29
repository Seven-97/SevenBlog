---
title: 认识微服务
category: 微服务
tags:
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



随着互联网行业的发展，对服务的要求也越来越高，服务架构也从单体架构逐渐演变为现在流行的微服务架构。这些架构之间有怎样的差别呢？



## 单体架构

**单体架构**：将业务的所有功能集中在一个项目中开发，打成一个包部署。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501011045132.png)

单体架构的优缺点如下：

**优点：**

- 架构简单
- 部署成本低

**缺点：**

- 耦合度高（维护困难、升级困难）



## 分布式架构

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



## 微服务

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



## SpringCloud

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



## 总结

- 单体架构：简单方便，高度耦合，扩展性差，适合小型项目。例如：学生管理系统
- 分布式架构：松耦合，扩展性好，但架构复杂，难度大。适合大型互联网项目，例如：京东、淘宝
- 微服务：一种良好的分布式架构方案
	- 优点：拆分粒度更小、服务更独立、耦合度更低
	- 缺点：架构非常复杂，运维、监控、部署难度提高
- SpringCloud是微服务架构的一站式解决方案，集成了各种优秀微服务功能组件





