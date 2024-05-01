---
title: 线程池 - Tomcat 线程池详解
category: Java
tag:
 - 并发编程
---





## Tomcat线程池

JDK 的线程池，是先使用核心线程数配置，接着使用队列长度，最后再使用最大线程配置。

Tomcat 的线程池，就是先使用核心线程数配置，再使用最大线程配置，最后才使用队列长度。

## 底层源码

###  runWorker

进入 runWorker 之后，这部分代码看起来很眼熟：

> org.apache.Tomcat.util.threads.ThreadPoolExecutor.Worker#run

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527858.gif)

在 getTask 方法里面，可以看到关于线程池的几个关键参数：

> org.apache.Tomcat.util.threads.ThreadPoolExecutor#getTask

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527919.gif)

- corePoolSize，核心线程数，值为 10。

- maximumPoolSize，最大线程数，值为 200。

而且基于 maximumPoolSize 这个参数，往前翻代码，会发现这个默认值就是 200：

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527873.gif)

Tomcat线程池默认队列长度：

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527879.gif)

 

Tomcat线程池：

- 核心线程数，值为 10。

- 最大线程数，值为 200。

- 队列长度，值为 Integer.MAX_VALUE。

 

往线程池里面提交任务的时候，会执行 execute 这个方法：

> org.apache.Tomcat.util.threads.ThreadPoolExecutor#execute(java.lang.Runnable)

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527886.gif)

对于 Tomcat 它会调用到 executeInternal 这个方法：

> org.apache.Tomcat.util.threads.ThreadPoolExecutor#executeInternal

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527901.gif)

这个方法里面，标号为 

- ① 的地方，就是判断当前工作线程数是否小于核心线程数，小于则直接调用 addWorker 方法，创建线程。

- ② 的地方主要是调用了 offer 方法，看看队列里面是否还能继续添加任务。

- 如果不能继续添加，说明队列满了，则来到标号为 ③ 的地方，看看是否能执行 addWorker 方法，创建非核心线程，即启用最大线程数。

 

接下来看 workQueue.offer(command) 这个逻辑。如果返回 true 则表示加入到队列，返回 false 则表示启用最大线程数嘛。

### workQueue.offer

这个 workQueue 是 TaskQueue，是 Tomcat 自己基于 LinkedBlockingQueue 搞的一个队列。

> org.apache.Tomcat.util.threads.TaskQueue#offer

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527735.gif)

标号为 ① 的地方，判断了 parent 是否为 null，如果是则直接调用父类的 offer 方法。说明要启用这个逻辑，我们的 parent 不能为 null。

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527934.gif)

parent 就是 Tomcat 线程池，通过其 set 方法可以知道，是在线程池完成初始化之后，进行了赋值。也就是说，在 Tomcat 的场景下，parent 不会为空。

标号为 ② 的地方，调用了 getPoolSizeNoLock 方法：这个方法是获取当前线程池中有多个线程。所以如果这个表达式为 true：

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527146.gif)

 

> parent.getPoolSizeNoLock() == parent.getMaximumPoolSize()

就表明当前线程池的线程数已经是配置的最大线程数了，那就调用 offer 方法，把当前请求放到到队列里面去。

 

标号为 ③ 的地方，是判断已经提交到线程池里面待执行或者正在执行的任务个数，是否比当前线程池的线程数还少。如果是，则说明当前线程池有空闲线程可以执行任务，则把任务放到队列里面去，就会被空闲线程给取走执行。

 

标号为 ④ 的地方。如果当前线程池的线程数比线程池配置的最大线程数还少，则返回 false。offer 方法返回 false，会出现什么情况？

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251527320.gif)

offer返回false 则开始到上图中标号为 ③ 的地方，去尝试添加非核心线程了，也就是启用最大线程数这个配置了。

 

## 总结：

JDK 的线程池，是先使用核心线程数配置，接着使用队列长度，最后再使用最大线程配置。

Tomcat 的线程池，就是先使用核心线程数配置，再使用最大线程配置，最后才使用队列长度。







