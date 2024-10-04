---
title: Async注解底层异步线程池原理
category: 常用框架
tag:
  - Spring
head:
  - - meta
    - name: keywords
      content: spring,异步,Async,实现原理,源码阅读
  - - meta
    - name: description
      content: 全网最全的Spring知识点总结，让天下没有难学的八股文！
---



## 前言

开发中我们经常会用到异步方法调用，具体到代码层面，异步方法调用的实现方式有很多种，比如最原始的通过实现Runnable接口或者继承Thread类创建异步线程，然后启动异步线程；再如，可以直接用java.util.concurrent包提供的线程池相关API实现异步方法调用。

如果说可以用一行代码快速实现异步方法调用，那是不是比上面方法香很多。

Spring提供了Async注解，就可以帮助我们一行代码搞定异步方法调用。Async注解用起来是很爽，但是如果不对其底层实现做深入研究，难免有时候也会心生疑虑，甚至会因使用不当，遇见一些让人摸不着头脑的问题。

本文首先将对Async注解做简单介绍，然后和大家分享一个我们项目中因Async注解使用不当的线上问题，接着再深扒Spring源码，对Async注解底层异步线程池的实现原理一探究竟。



## Async注解简介

### Async注解定义源码

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406150047558.webp)

从源码可以看出@Async注解定义很简单，只需要关注两点：

- Target({ElementType.TYPE, ElementType.METHOD})标志Async注解可以作用在方法和类上，作用在类上时，类的所有方法可以实现异步调用。
- String value( ) default ""是唯一字段属性，用来指定异步线程池，且该字段有缺省值。



### Async注解异步调用实现原理概述

在Spring框架中，**Async注解的实现是通过AOP来实现的**。具体来说，Async注解是由AsyncAnnotationAdvisor这个切面类来实现的。

AsyncAnnotationAdvisor类是Spring框架中用于处理Async注解的切面，**它会在被Async注解标识的方法被调用时，创建一个异步代理对象来执行方法**。这个异步代理对象会**在一个新的线程中调用被@Async注解标识的方法**，从而实现方法的异步执行。

在AsyncAnnotationAdvisor中，会使用AsyncExecutionInterceptor来处理Async注解。AsyncExecutionInterceptor是实现了MethodInterceptor接口的类，用于拦截被Async注解标识的方法的调用，并在一个新的线程中执行这个方法。

通过AOP的方式实现Async注解的异步执行，Spring框架可以在方法调用时动态地创建代理对象来实现异步执行，而不需要在业务代码中显式地创建新线程。

总的来说，Async注解的实现是通过AOP机制来实现的，具体的切面类是AsyncAnnotationAdvisor，它利用AsyncExecutionInterceptor来处理被Async注解标识的方法的调用，实现方法的异步执行。





## 获取Async注解线程池主流程解析

进入到Spring源码Async注解AOP切面实现部分，我们重点剖析异步调用实现中线程池是怎么处理的。下图是org.springframework.aop.interceptor.AsyncExecutionInterceptor#invoke方法的实现，可以看出是调用determineAsyncExecutor方法获取异步线程池。

![AsyncExecutionInterceptor#invoke](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406150047555.webp)



下图是determineAsyncExecutor方法实现：

![AsyncExecutionInterceptor#determineAsyncExecutor](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406150047564.webp)

![AsyncExecutionAspectSupport#getExecutorQualifier](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406150047570.webp)



从代码实现中可以看到determineAsyncExecutor获取线程池的大致流程：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406150047581.webp)

如果在使用Async注解时指定了自定义线程池比较好理解，如果使用Async注解时没有指定自定义线程池，Spring是怎么处理默认线程池呢？继续深入源码看看Spring提供的默认线程池的实现。



## Spring是怎么为Async注解提供默认线程池的

Async注解默认线程池有下面两个方法实现：  

- org.springframework.aop.interceptor.AsyncExecutionInterceptor#getDefaultExecutor
- org.springframework.aop.interceptor.AsyncExecutionAspectSupport#getDefaultExecutor

![AsyncExecutionInterceptor#getDefaultExecutor](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406150047553.webp)

可以看出AsyncExecutionInterceptor#getDefaultExecutor方法比较简单：先尝试调用父类AsyncExecutionAspectSupport#getDefaultExecutor方法获取线程池，**如果父类方法获取不到线程池再用创建SimpleAsyncTaskExecutor对象作为Async的线程池返回**。

![AsyncExecutionAspectSupport#getDefaultExecutor](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406150047532.webp)

再来看父类AsyncExecutionAspectSupport#getDefaultExecutor方法的实现，可以看到Spring根据类型从Spring容器中获取TaskExecutor类的实例，先记住这个关键点。

我们知道，Spring根据类型获取实例时，如果Spring容器中有且只有一个指定类型的实例对象，会直接返回，否则的话，会抛出NoUniqueBeanDefinitionException异常或者NoSuchBeanDefinitionException异常。

但是，对于Executor类型，Spring容器却“网开一面”，有一个特殊处理：当从Spring容器中获取Executor实例对象时，**如果满足@ConditionalOnMissingBean(Executor.class)条件，Spring容器会自动装载一个ThreadPoolTaskExecutor实例对象，而ThreadPoolTaskExecutor是TaskExecutor的实现类**。

![TaskExecutionAutoConfiguration](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406150047620.webp)

![TaskExecutionProperties](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406150047632.webp)

从TaskExecutionProperties和TaskExecutionAutoConfiguration两个配置类我们看到Spring自动装载的**ThreadPoolTaskExecutor线程池对象的参数：核心线程数=8；最大线程数=Integer.MAX_VALUE；队列大小=Integer.MAX_VALUE。**



## 总结

现在Async注解线程池源码已经看的差不多了，下面这张图是Spring处理Async异步线程池的流程：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406150047614.webp)

归纳一下：如果在使用Async注解时没有指定自定义的线程池会出现以下几种情况：

- 当Spring容器中有且仅有一个TaskExecutor实例时，Spring会用这个线程池来处理Async注解的异步任务，这可能会踩坑，如果这个TaskExecutor实例是第三方jar引入的，可能会出现很诡异的问题。
- Spring创建一个**核心线程数=8、最大线程数=Integer.MAX_VALUE、队列大小=Integer.MAX_VALUE的线程池**来处理Async注解的异步任务，**这时候也可能会踩坑，由于线程池参数设置不合理，核心线程数=8，队列大小过大，如果有大批量并发任务，可能会出现OOM**。
- Spring创建**SimpleAsyncTaskExecutor实例**来处理Async注解的异步任务，**SimpleAsyncTaskExecutor不是一个好的线程池实现类，SimpleAsyncTaskExecutor根据需要在当前线程或者新线程中执行异步任务。如果当前线程已经有空闲线程可用，任务将在当前线程中执行，否则将创建一个新线程来执行任务。由于这个线程池没有线程管理的能力，每次提交任务都实时创建新线程，所以如果任务量大，会导致性能下降**。


<!-- @include: @article-footer.snippet.md -->     

