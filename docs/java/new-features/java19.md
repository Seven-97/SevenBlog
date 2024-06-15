---
title: Java 19新特性
category: Java
tag:
 - 版本新特性
---





## Virtual Threads (Preview)（虚拟线程）

[虚拟线程](https://www.seven97.top/java/collection/virtualthreads.html)



## Thread.builder接口

在jdk19中新增了一个密封（sealed）接口Builder，该接口只允许有两个子接口实现：

- OfPlatform：创建平台线程的时候使用，是一个密封接口，只允许ThreadBuilders.PlatformThreadBuilder实现。

- OfVirtual：创建虚拟线程的时候使用，是一个密封接口，只允许ThreadBuilders.VirtualThreadBuilder实现。

上面3种创建虚拟线程的方式本质都是通过OfVirtual来进行创建的，OfVirtual和OfPlatform接口中的api很多是相同的，OfPlatform中的方法更多，所以下面我们以OfPlatform为例演示他的使用方式。

通过OfPlatform中的factory()方法可以创建一个ThreadFactory线程工厂，学过线程池的同学对它应该并不陌生，它可以帮助我们创建出平台线程对象。

```java
ThreadFactory threadFactory = Thread.ofPlatform().factory();
```



除了上面的用法之外，还可以用它来创建平台线程对象

```java
//创建任务
Runnable task = () -> {
    System.out.println(Thread.currentThread().getName());
};

//将任务放到t线程中并运行
Thread t = Thread.ofPlatform().start(task);
```



上面创建平台线程的方式跟之前的new Thread是一样的，优点是我们可以用它来实现链式编程，比如要设置线程优先级，线程名字，守护线程：

```java
//创建任务
Runnable task = () -> {
    System.out.println(Thread.currentThread().getName());
};

//链式编程
Thread.ofPlatform().name("小").priority(Thread.MAX_PRIORITY).daemon(true).start(task);
```



## 虚拟线程中的ThreadLocal

由于虚拟线程的数量会比较多，所以在使用ThreadLocal的时候一定要注意。线程池中的线程在执行多个任务的时候，不要使用ThreadLocal。在Thread.Builder中提供了不支持ThreadLocal的方法。

```java
Thread.ofVirtual().allowSetThreadLocals(false);
Thread.ofVirtual().inheritInheritableThreadLocals(false);
```



## LockSupport对虚拟线程的支持

LockSupport是支持虚拟线程的，当调用park()方法时，虚拟线程会解除挂载，这样平台线程可以执行其他的操作，当调用unpark()方法时，虚拟线程会被调度器重新挂载到平台线程，再继续工作。

## java.io包下类的变化

为了减少内存的使用，BufferedOutputStream，BufferedWriter，OutputStreamWriter中默认的初始数组大小由之前的8192变成了512。

 

 

 

 

 

 

 