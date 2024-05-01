---
title: Java 19新特性
category: Java
tag:
 - 版本新特性
---





## Virtual Threads (Preview)（虚拟线程）

### 简介

该特性在java19中是预览版，虚拟线程是一种用户态下的线程，类似go语言中的goroutines 和Erlang中的processes，虚拟线程并非比线程快，而是提高了应用的吞吐量，相比于传统的线程是由操作系统调度来看，虚拟线程是我们自己程序调度的线程。如果你对之前java提供的线程API比较熟悉了，那么在学习虚拟线程的时候会比较轻松，传统线程能运行的代码，虚拟线程也可以运行。虚拟线程的出现，并没有修改java原有的并发模型，也不会替代原有的线程。虚拟线程主要作用是提升服务器端的吞吐量。

### 吞吐量的瓶颈

服务器应用程序的伸缩性受利特尔法则（Little’s Law）的制约，与下面3点有关

- 延迟：请求处理的耗时

- 并发量：同一时刻处理的请求数量

- 吞吐量：单位时间内处理的数据数量

比如一个服务器应用程序的延迟是50ms，处理10个并发请求，则吞吐量是200请求/秒（10 / 0.05），如果吞吐量要达到2000请求/秒，则处理的并发请求数量是100。按照1个请求对应一个线程的比例来看，要想提高吞吐量，线程数量也要增加。

java中的线程是在操作系统线程（OS thread）进行了一层包装，而操作系统中线程是重量级资源，在硬件配置确定的前提下，我们就不能创建更多的线程了，此时线程数量就限制了系统性能，为了解决该问题，虚拟线程就出现了。

 

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404252017454.gif)

与虚拟地址可以映射到物理内存类似，java是将大量的虚拟线程映射到少量的操作系统线程，多个虚拟线程可以使用同一个操作系统线程，其创建所耗费的资源也是极其低廉的，无需系统调用和系统级别的上下文切换，且虚拟线程的生命周期短暂，不会有很深的栈的调用，一个虚拟线程的生命周期中只运行一个任务，因此我们可以创建大量的虚拟线程，且虚拟线程无需池化。

### 虚拟线程的应用场景

在服务器端的应用程序中，可能会有大量的并发任务需要执行，而虚拟线程能够明显的提高应用的吞吐量。下面的场景能够显著的提高程序的吞吐量：

- 至少几千的并发任务量

- 任务为io密集型

下面代码中为每个任务创建一个线程，当任务量较多的时候，你的电脑可以感受到明显的卡顿（如果没有，可以增加任务数量试下）：

```java
//ExecutorService实现了AutoCloseable接口，可以自动关闭了
try (ExecutorService executor = Executors.newCachedThreadPool()) {
    //向executor中提交1000000个任务
    IntStream.range(0, 1000000).forEach(
        i -> {
            executor.submit(() -> {
                try {
                    //睡眠1秒，模拟耗时操作
                    Thread.sleep(Duration.ofSeconds(1));
                    System.out.println("执行任务:" + i);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            });
        });
} catch (Exception e) {
    e.printStackTrace();
}
```



将上面的代码改成虚拟线程之后，电脑不会感受到卡顿了：

```java
//newVirtualThreadPerTaskExecutor为每个任务创建一个虚拟线程
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    IntStream.range(0, 1000_000).forEach(i -> {
        executor.submit(() -> {
            try {
                //睡眠1秒，模拟耗时操作
                Thread.sleep(Duration.ofSeconds(1));
                System.out.println("执行任务:" + i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    });
}
```



### 平台线程和虚拟线程

平台线程（platform thread）：指Java中的线程，比如通过Executors.newFixedThreadPool()创建出来的线程，我们称之为平台线程。

虚拟线程并不会直接分配给cpu去执行，而是通过调度器分配给平台线程，平台线程再被调度器管理。Java中虚拟线程的调度器采用了工作窃取的模式进行FIFO的操作，调度器的并行数默认是Jvm获取的处理器数量（通过该方法获取的数量Runtime.getRuntime().availableProcessors()），调度器并非分时（time sharing）的。在使用虚拟线程编写程序时，不能控制虚拟线程何时分配给平台线程，也不能控制平台线程何时分配给cpu。

以前任务和平台线程的关系：

 

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404252017482.gif)

使用虚拟线程之后，任务-虚拟线程-调度器-平台线程的关系，1个平台线程可以被调度器分配不同的虚拟线程：

 

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404252017484.gif)



### 携带器

调度器将虚拟线程挂载到平台线程之后，该平台线程叫做虚拟线程的携带器（言外之意就是，平台线程携带着虚拟线程），调度器并不维护虚拟线程和携带器之间的关联关系，因此在一个虚拟线程的生命周期中可以被分配到不同的携带器，即虚拟线程运行了一小段代码后，可能会脱离携带器，此时其他的虚拟线程会被分配到这个携带器上。

携带器和虚拟线程是相互独立的，比如：

- 虚拟线程不能使用携带器的标识，Thread.current()方法获取的是虚拟线程本身。

- 两者有各自的栈空间。

- 两者不能访问对方的Thread Local变量。

在程序的执行过程中，虚拟线程遇到阻塞的操作时大部分情况下会被解除挂载，阻塞结束后，虚拟线程会被调度器重新挂载到携带器上，因此虚拟线程会频繁的挂载和解除挂载，这并不会导致操作系统线程的阻塞。下面的代码在执行两个get方法和send方法（会有io操作）时会使虚拟线程发生挂载和解除挂载：

```java
response.send(future1.get() + future2.get());
```

有些阻塞操作并不会导致虚拟线程解除挂载，这样会同时阻塞携带器和操作系统线程，例如：操作系统基本的文件操作，java中的Object.wait()方法。下面两种情况不会导致虚拟线程的解除挂载：

1. 执行synchronized同步代码（会导致携带器阻塞，所以建议使用ReentrantLock替换掉synchronized）
2. 执行本地方法或外部函数

### 虚拟线程和平台线程api的区别

从内存空间上来说，虚拟线程的栈空间可以看作是一个大块的栈对象，它被存储在了java堆中，相比于单独存储对象，堆中存储虚拟线程的栈会造成一些空间的浪费，这点在后续的java版本中应该会得到改善，当然这样也是有一些好处的，就是可以重复利用这部分栈空间，不用多次申请开辟新的内存地址。虚拟线程的栈空间最大可以达到平台线程的栈空间容量。

虚拟线程并不是GC root，其中的引用不会出现stop-world，当虚拟线程被阻塞之后比如BlockingQueue.take()，平台线程既不能获取到虚拟线程，也不能获取到queue队列，这样该平台线程可能会被回收掉，虚拟线程在运行或阻塞时不会被GC

- 通过Thread构造方法创建的线程都是平台线程

- 虚拟线程是守护线程，不能通过setDaemon方法改成非守护线程

- 虚拟线程的优先级是默认的5，不能被修改，将来的版本可能允许修改

- 虚拟线程不支持stop()，suspend()，resume()方法

### 创建虚拟线程的方式

java中创建的虚拟线程本质都是通过Thread.Builder.OfVirtual对象进行创建的，我们后面再来讨论这个对象，下面先看下创建虚拟线程的三种方式：

1. 通过Thread.startVirtualThread直接创建一个虚拟线程

```java
//创建任务
Runnable task = () -> {
    System.out.println("执行任务");
};

//创建虚拟线程将任务task传入并启动
Thread.startVirtualThread(task);

//主线程睡眠，否则可能看不到控制台的打印
TimeUnit.SECONDS.sleep(1);
```



2. 使用Thread.ofVirtual()方法创建

```java
//创建任务
Runnable task = () -> {
    System.out.println(Thread.currentThread().getName());
};

//创建虚拟线程命名为诺手，将任务task传入
Thread vt1 = Thread.ofVirtual().name("诺手").unstarted(task);
vt1.start();//启动虚拟线程

//主线程睡眠，否则可能看不到控制台的打印
TimeUnit.SECONDS.sleep(1);
```



3. 通过ExecutorService创建，为每个任务分配一个虚拟线程，下面代码中提交了100个任务，对应会有100个虚拟线程进行处理。

```java
/*
    通过ExecutorService创建虚拟线程
    ExecutorService实现了AutoCloseable接口，可以自动关闭了
*/
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    //向executor中提交100个任务
    IntStream.range(0, 100).forEach(i -> {
        executor.submit(() -> {
            //睡眠1秒
            try {
                Thread.sleep(Duration.ofSeconds(1));
                System.out.println(i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }                    
        });
    });
}
```



现在平台线程和虚拟线程都是Thread的对象，那该如何区分该对象是平台线程还是虚拟线程？可以利用Thread中的isVirtual()方法进行判断，返回true表示虚拟线程：

```java
//创建任务
Runnable task = () -> {
    System.out.println("执行任务");
};

//创建虚拟线程将任务task传入并启动
Thread vt = Thread.startVirtualThread(task);
System.out.println(vt.isVirtual());
```



### Thread.builder接口

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



### 虚拟线程中的ThreadLocal

由于虚拟线程的数量会比较多，所以在使用ThreadLocal的时候一定要注意。线程池中的线程在执行多个任务的时候，不要使用ThreadLocal。在Thread.Builder中提供了不支持ThreadLocal的方法。

```java
Thread.ofVirtual().allowSetThreadLocals(false);
Thread.ofVirtual().inheritInheritableThreadLocals(false);
```



### LockSupport对虚拟线程的支持

LockSupport是支持虚拟线程的，当调用park()方法时，虚拟线程会解除挂载，这样平台线程可以执行其他的操作，当调用unpark()方法时，虚拟线程会被调度器重新挂载到平台线程，再继续工作。

### java.io包下类的变化

为了减少内存的使用，BufferedOutputStream，BufferedWriter，OutputStreamWriter中默认的初始数组大小由之前的8192变成了512。

 

 

 

 

 

 

 