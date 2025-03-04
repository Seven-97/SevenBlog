---
title: Tomcat线程池：SpringBoot 应用可以同时并发处理多少请求？
category: Java
tags:
  - Tomcat
head:
  - - meta
    - name: keywords
      content: Java,并发编程,多线程,Thread,Tomcat线程池,实现原理,底层源码
  - - meta
    - name: description
      content: 全网最全的Java并发编程知识点总结，让天下没有难学的八股文！
---


Q：经典面试题，SpringBoot 应用可以同时并发处理多少请求？

A：SpringBoot 应用并发处理请求数主要由两个因素影响，使用的 Servlet容器（默认使用 Tomcat，常用的还有 jetty、undertow） 和 配置项。所以在默认配置下，SprigBoot 应用可以并发处理 200 请求。

那么这个200是怎么来的呢？SprigBoot 默认使用Tomcat，而Tomcat线程池的最大线程数就是200。到这里有朋友就有疑问了，并发数不是应该先受队列长度影响吗，难道队列长度也只有200，才会使用最大线程数吗？


## Tomcat线程池

JDK 的线程池，是先使用核心线程数配置，接着使用队列长度，最后再使用最大线程配置。

Tomcat 的线程池，就是先使用核心线程数配置，再使用最大线程配置，最后才使用队列长度。

## 底层源码

###  runWorker

进入 runWorker 之后，这部分代码看起来很眼熟：

![org.apache.Tomcat.util.threads.ThreadPoolExecutor.Worker#run](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527858.gif)

在 getTask 方法里面，可以看到关于线程池的几个关键参数：

![org.apache.Tomcat.util.threads.ThreadPoolExecutor#getTask](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527919.gif)

- corePoolSize，核心线程数，值为 10。

- maximumPoolSize，最大线程数，值为 200。

而且基于 maximumPoolSize 这个参数，往前翻代码，会发现这个默认值就是 200：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527873.gif)

Tomcat线程池默认队列长度：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527879.gif)

 

Tomcat线程池：

- 核心线程数，值为 10。

- 最大线程数，值为 200。

- 队列长度，值为 Integer.MAX_VALUE。

 

往线程池里面提交任务的时候，会执行 execute 这个方法：

![org.apache.Tomcat.util.threads.ThreadPoolExecutor#execute(java.lang.Runnable)](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527886.gif)

对于 Tomcat 它会调用到 executeInternal 这个方法：

![org.apache.Tomcat.util.threads.ThreadPoolExecutor#executeInternal](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527901.gif)

这个方法里面，标号为 

- ① 的地方，就是判断当前工作线程数是否小于核心线程数，小于则直接调用 addWorker 方法，创建线程。

- ② 的地方主要是调用了 offer 方法，看看队列里面是否还能继续添加任务。

- 如果不能继续添加，说明队列满了，则来到标号为 ③ 的地方，看看是否能执行 addWorker 方法，创建非核心线程，即启用最大线程数。

 

接下来看 workQueue.offer(command) 这个逻辑。如果返回 true 则表示加入到队列，返回 false 则表示启用最大线程数嘛。

### workQueue.offer

这个 workQueue 是 TaskQueue，是 Tomcat 自己基于 LinkedBlockingQueue 搞的一个队列。

![org.apache.Tomcat.util.threads.TaskQueue#offer](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527735.gif)

标号为 ① 的地方，判断了 parent 是否为 null，如果是则直接调用父类的 offer 方法。说明要启用这个逻辑，我们的 parent 不能为 null。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527934.gif)

parent 就是 Tomcat 线程池，通过其 set 方法可以知道，是在线程池完成初始化之后，进行了赋值。也就是说，在 Tomcat 的场景下，parent 不会为空。

标号为 ② 的地方，调用了 getPoolSizeNoLock 方法：这个方法是获取当前线程池中有多个线程。所以如果这个表达式为 true：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527146.gif)

 

> parent.getPoolSizeNoLock() == parent.getMaximumPoolSize()

就表明当前线程池的线程数已经是配置的最大线程数了，那就调用 offer 方法，把当前请求放到到队列里面去。

 

标号为 ③ 的地方，是判断已经提交到线程池里面待执行或者正在执行的任务个数，是否比当前线程池的线程数还少。如果是，则说明当前线程池有空闲线程可以执行任务，则把任务放到队列里面去，就会被空闲线程给取走执行。

 

标号为 ④ 的地方。如果当前线程池的线程数比线程池配置的最大线程数还少，则返回 false。offer 方法返回 false，会出现什么情况？

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527320.gif)

offer返回false 则开始到上图中标号为 ③ 的地方，去尝试添加非核心线程了，也就是启用最大线程数这个配置了。

 

## 总结

JDK 的线程池，是先使用核心线程数配置，接着使用队列长度，最后再使用最大线程配置。

Tomcat 的线程池，就是先使用核心线程数配置，再使用最大线程配置，最后才使用队列长度。

那么如何调整增大SpringBoot的最大线程数呢？看到这里应该已经明白了，可以通过调整 最大线程数来 控制并发数量


<!-- @include: @article-footer.snippet.md -->     



