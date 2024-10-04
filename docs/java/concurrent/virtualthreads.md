---
title: 虚拟线程
category: Java
tag:
 - 并发编程
head:
  - - meta
    - name: keywords
      content: 线程,虚拟线程,实现源码,新特性,携带器
  - - meta
    - name: description
      content: 全网最全的Java并发编程知识点总结，让天下没有难学的八股文！
---





这是Java19新增的预览版功能，到Java21正式可以使用

## 简介

该特性在java19中是预览版，虚拟线程是一种用户态下的线程，类似go语言中的goroutines 和Erlang中的processes，虚拟线程并非比线程快，而是提高了应用的吞吐量，相比于传统的线程是由操作系统调度来看，虚拟线程是我们自己程序调度的线程。如果你对之前java提供的线程API比较熟悉了，那么在学习虚拟线程的时候会比较轻松，传统线程能运行的代码，虚拟线程也可以运行。虚拟线程的出现，并没有修改java原有的并发模型，也不会替代原有的线程。虚拟线程主要作用是提升服务器端的吞吐量。

## 为什么要有虚拟线程

服务器应用程序的伸缩性受利特尔法则（Little’s Law）的制约，与下面3点有关

- 延迟：请求处理的耗时

- 并发量：同一时刻处理的请求数量

- 吞吐量：单位时间内处理的数据数量

比如一个服务器应用程序的延迟是50ms，处理10个并发请求，则吞吐量是200请求/秒（10 / 0.05），如果吞吐量要达到2000请求/秒，则处理的并发请求数量是100。按照1个请求对应一个线程的比例来看，要想提高吞吐量，线程数量也要增加。

java中的线程是在操作系统线程（OS thread）进行了一层包装（目前大部分语言实现采用的线程模型，都是用户态的线程一对一映射到内核线程上，好处是实现简单，统一由操作系统负责调度），OS线程的优点是它足够通用，不管是什么语言/什么应用场景，但OS线程的问题也正是来自于此：

- OS不知道用户态的程序会如何使用线程，它会给每条线程分配一个固定大小的堆栈，通常会比实际使用的要大很多；
- 线程的上下文切换要通过内核调度进行，相对更慢；
- 线程的调度算法需要做兼顾和妥协，很难做特定的优化，像web server中处理请求的线程和视频编解码的线程行为有很大的区别；

为了解决该问题，虚拟线程就出现了。也就是多对多的线程模型：经典的就是Erlang的进程和Go的goroutine，M:N 的映射关系，大量（M）虚拟的线程被调度在较少数量（N）的操作系统线程上运行。用户态的运行时负责调度用户态线程，OS则只需要负责OS线程，各司其职。灵活度更高，开发者基本不用担心线程数爆炸的问题。

 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404252017454.gif)

与虚拟地址可以映射到物理内存类似，java是将大量的虚拟线程映射到少量的操作系统线程，带来了一些好处：

- 线程的切换很快，无需系统调用和系统级别的上下文切换
- 分配线程的开销很低：一方面是创建和销毁很快，另一方面内存使用也更少
- 竞态条件和线程同步处理起来更简单
- 且虚拟线程的生命周期短暂，不会有很深的栈的调用
- 一个虚拟线程的生命周期中只运行一个任务，因此可以创建大量的虚拟线程，
- 虚拟线程无需池化



另一方面，虚拟线程不能带来什么？

要意识到虚拟线程是更轻量的线程，但并不是"更快"的线程，它每秒执行的CPU指令并不会比普通线程要多。假设有这样一个场景，需要同时启动10000个任务做一些事情：

```java
// 创建一个虚拟线程的Executor，该Executor每执行一个任务就会创建一个新的虚拟线程
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    IntStream.range(0, 10_000).forEach(i -> {
        executor.submit(() -> {
            doSomething();
            return i;
        });
    });
}  // executor.close() is called implicitly, and waits
```

考虑两种场景：

1. 如果doSomething()里执行的是某类IO操作，那么使用虚拟线程是非常合适的，因为虚拟线程创建和切换的代价很低，底层对应的可能只需要几个OS线程。如果没有虚拟线程，不考虑ForkJoin之类的工具，使用普通线程的话：

2. - Executors.newVirtualThreadPerTaskExecutor()换成Executors.newCachedThreadPool()。结果是程序会崩溃，因为大多数操作系统和硬件不支持这种规模的线程数。
   - 换成Executors.newFixedThreadPool(200)或者其他自定义的线程池，那这10000个任务将会共享200个线程，许多任务将按顺序运行而不是同时运行，并且程序需要很长时间才能完成。

3. 如果doSomething()里执行的是某类计算任务，例如给一个大数组排序，那么虚拟线程反而还可能带来多余的开销。

总结一下，虚拟线程真正擅长的是等待，等待大量阻塞操作完成。它能提供的是 scale（更高的吞吐量），而不是 speed（更低的延迟）。虚拟线程最适合的是原来需要更多线程数来处理计算无关业务的场景，典型的就是像web容器、数据库、文件操作一类的IO密集型的应用。



## 虚拟线程的理解

### 平台线程和虚拟线程

平台线程（platform thread）：指Java中的线程，比如通过Executors.newFixedThreadPool()创建出来的线程，我们称之为平台线程。

虚拟线程并不会直接分配给cpu去执行，而是通过调度器分配给平台线程，平台线程再被调度器管理。Java中虚拟线程的调度器采用了工作窃取的模式进行FIFO的操作，调度器的并行数默认是Jvm获取的处理器数量（通过该方法获取的数量Runtime.getRuntime().availableProcessors()），调度器并非分时（time sharing）的。在使用虚拟线程编写程序时，不能控制虚拟线程何时分配给平台线程，也不能控制平台线程何时分配给cpu。

以前任务和平台线程的关系：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404252017482.gif)



使用虚拟线程之后，任务-虚拟线程-调度器-平台线程的关系，1个平台线程可以被调度器分配不同的虚拟线程：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404252017484.gif)



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

### 虚拟线程和平台线程的区别

从内存空间上来说，虚拟线程的栈空间可以看作是一个大块的栈对象，它被存储在了java堆中，相比于单独存储对象，堆中存储虚拟线程的栈会造成一些空间的浪费，这点在后续的java版本中应该会得到改善，当然这样也是有一些好处的，就是可以重复利用这部分栈空间，不用多次申请开辟新的内存地址。虚拟线程的栈空间最大可以达到平台线程的栈空间容量。

虚拟线程并不是GC root，其中的引用不会出现stop-world，当虚拟线程被阻塞之后比如BlockingQueue.take()，平台线程既不能获取到虚拟线程，也不能获取到queue队列，这样该平台线程可能会被回收掉，虚拟线程在运行或阻塞时不会被GC

- 通过Thread构造方法创建的线程都是平台线程

- 虚拟线程是守护线程，不能通过setDaemon方法改成非守护线程

- 虚拟线程的优先级是默认的5，不能被修改，将来的版本可能允许修改

- 虚拟线程不支持stop()，suspend()，resume()方法



## 使用虚拟线程

java中创建的虚拟线程本质都是通过Thread.Builder.OfVirtual对象进行创建的，虚拟线程的API非常非常简单，在设计上与现有的Thread类完全兼容。虚拟线程创建出来后也是Thread实例，因此很多原先的代码可以无缝迁移。创建虚拟线程有三种方式：

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



## 性能对比

```java
public void tryCreateInfiniteThreads() {
    var adder = new LongAdder();
    Runnable job = () -> {
        adder.increment();
        System.out.println("Thread count = " + adder.longValue());
        LockSupport.park();
    };

    // 启动普通线程
    startThreads(() -> new Thread(job));
    // 或是启动虚拟线程
    startThreads(() -> Thread.ofVirtual().unstarted(job));
}

public void startThreads(Supplier<Thread> threadSupplier) {
    while (true) {
        Thread thread = threadSupplier.get();
        thread.start();
    }
}
```

**普通线程：**创建到4064个线程后程序报OOM错误崩溃。

```java
.......
Thread count = 4063
Thread count = 4064
[0.927s][warning][os,thread] Failed to start thread "Unknown thread" - pthread_create failed (EAGAIN) for attributes: stacksize: 1024k, guardsize: 4k, detached.
[0.927s][warning][os,thread] Failed to start the native thread for java.lang.Thread "Thread-4064"
Exception in thread "main" java.lang.OutOfMemoryError: unable to create native thread: possibly out of memory or process/resource limits reached
    at java.base/java.lang.Thread.start0(Native Method)
    at java.base/java.lang.Thread.start(Thread.java:1535)
    at com.rhino.vt.VtExample.startThread(VtExample.java:24)
    at com.rhino.vt.VtExample.main(VtExample.java:13)
```

**虚拟线程：**创建了超过360万个虚拟线程后被挂起，但没有崩溃，虚拟线程的计数一直在缓慢增长，这是因为被 park 的虚拟线程会被垃圾回收，然后 JVM 能够创建更多的虚拟线程并将其分配给底层的平台线程。



Github上有位老哥做了个更接近真实场景的测试，模拟远程服务请求数据，比较了使用普通线程阻塞式请求、CompletableFeature异步请求、虚拟线程的三种方式的差异，结果显示在连接数少的时候三者差别不大，连接数上去后虚拟线程在吞吐量、内存占用、延迟、CPU占用率方面都有比较大的优势，如下图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406160124979.webp)

可能这么对比还是不够公平，毕竟一般我们不会直接用这么简单的异步编程，还是会通过各种框架轮子搞。Oracle 的Helidon Níma 号称是第一个采用了虚拟线程的微服务框架，主要的卖点也是性能，可以参考其QPS性能测试数据：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406160124963.webp)

可以看到使用了虚拟线程的web服务器性能很好，与用Netty的差距很小，这也符合预期。相比起来虚拟线程使用起来更简单。



## 深入虚拟线程

**thread = continuation + scheduler**

回过头来讨论下：到底什么是**"线程"**？简单的定义是，"线程"是顺序执行的一系列计算机指令。由于我们处理的操作可能不仅涉及计算，还涉及 IO、定时暂停和同步等，线程会有包括运行、阻塞、等待在内的各种状态，并在状态之间调度流转。当一个线程阻塞或等待时，它应该腾出计算资源（CPU内核），并允许另一个线程运行，然后在等待的事件发生时恢复执行。这其中涉及到两个概念：

1. continuation（这个词实在不知道怎么翻译才恰当）：一系列顺序执行的指令序列，可能会暂停或阻塞，然后恢复执行；
2. scheduler：顾名思义，负责协调调度线程的机制；

两者是独立的，因此我们可以选择不同的实现。之前的普通线程，在VM层面仅仅是对OS线程的一层简单封装，continuation和scheduler都是交给OS管理，而虚拟线程实现则是在VM里完成这两件事情，当然底层还是需要有相应的OS线程作为载体线程（CarrierThread），并且这个对应并不是固定不变的，在虚拟线程恢复后，完全可能被调度到另一个载体线程。

| 组合                 | scheduler-OS     | scheduler-Runtime                       |
| -------------------- | ---------------- | --------------------------------------- |
| continuation-OS      | Java现在的Thread | 谷歌对Linux内核修改的User-Level Threads |
| continuation-Runtime | 糟糕的选择？     | 虚拟线程                                |

虚拟线程的调用堆栈存在Java堆上，而不是OS分配的栈区内。其内存占用开始时只有几百字节，并可以随着调用堆栈自动伸缩。虚拟线程的运行其实就是两个操作：

- 挂载（mount）：挂载虚拟线程意味着将所需的栈帧从堆中临时复制到载体线程的堆栈中，并在挂载时借用载体堆栈执行。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406160127387.webp)

- 卸载（unmount）：当在虚拟线程中运行的代码因为 IO、锁等原因阻塞后，它可以从载体线程中卸载，然后将修改的栈帧复制回堆中，从而释放载体线程以进行其他操作（例如运行另一个虚拟线程）。对应的，JDK 中几乎所有的阻塞点都已经过调整，因此当在虚拟线程上遇到阻塞操作时，虚拟线程会从其载体上卸载而不是阻塞。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406160127377.webp)


关于scheduler就比较简单了，因为JDK中有现成的ForkJoinPool可以用。work-stealing + FIFO，性能很好。scheduler的并行性是可用于调度虚拟线程的OS线程数。默认情况下，它等于可用CPU核数，也可以使用系统属性jdk.virtualThreadScheduler.parallelism进行调整。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406160127396.webp)


需要注意的是，JDK中的绝大多数阻塞操作将卸载虚拟线程，释放其载体线程来承担新的工作。但是，JDK中的一些阻塞操作不会卸载虚拟线程，因此会阻塞其载体线程。这是因为操作系统级别（例如，许多文件系统操作）或JDK级别（例如，Object.wait()）的限制。这些阻塞操作的解决方式是，通过临时扩展scheduler的并行性来补偿操作系统线程的捕获。因此，scheduler的ForkJoinPool中的平台线程数量可能暂时超过CPU核数。scheduler可用的最大平台线程数可以使用系统属性：`jdk.virtualThreadScheduler.maxPoolSize`进行调整。



## 虚拟线程源码

试着写一个使用虚拟线程进行网络IO的例子，来窥视下虚拟线程底层的魔法。

下面代码使用了基于虚拟线程的ExecutorService来获取一组URL的响应。每个URL任务会启动一个虚拟线程进行处理。

 ```java
 // record是JDK 14中引入的，这里作为简单的数据类，保存url和响应
 record URLData (URL url, byte[] response) { }
 
 public List<URLData> retrieveURLs(URL... urls) throws Exception {
     try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
         var tasks = Arrays.stream(urls)
             .map(url -> (Callable<URLData>)() -> getURL(url))
             .toList();
         return executor.invokeAll(tasks)
             .stream()
             .filter(Future::isDone)
             .map(this::getFutureResult)
             .toList();
     }
 }
 ```

获取响应的逻辑在getURL中实现，使用同步的URLConnectionAPI来读取数据。

 ```java
 URLData getURL(URL url) throws IOException {
   try (InputStream in = url.openStream()) {
     return new URLData(url, in.readAllBytes());
   }
 }
 ```



这里我模拟了两个HTTP接口，其中一个响应很慢，因此在运行后不会马上完成。

 ```java
 // test1接口sleep 1s返回，test2接口则sleep 100s
 example.retrieveURLs(new URL("http://localhost:7001/test1"), new URL("http://localhost:7001/test2"));
 ```

这样就可以用jcmd命令进行线程转储。

```java
$ jcmd `jps | grep VtExample | awk '{print $1}'` Thread.dump_to_file -format=json thread_dump.json
```

把结果中的普通线程堆栈去掉后，就得到了虚拟线程的堆栈：

```css
{
  "container": "java.util.concurrent.ThreadPerTaskExecutor@5d5a133a",
  "parent": "<root>",
  "owner": null,
  "threads": [
   {
     "tid": "24",
     "name": "",
     "stack": [
        "java.base\/jdk.internal.vm.Continuation.yield(Continuation.java:357)",
        "java.base\/java.lang.VirtualThread.yieldContinuation(VirtualThread.java:370)",
        "java.base\/java.lang.VirtualThread.park(VirtualThread.java:499)",
        "java.base\/java.lang.System$2.parkVirtualThread(System.java:2596)",
        "java.base\/jdk.internal.misc.VirtualThreads.park(VirtualThreads.java:54)",
        "java.base\/java.util.concurrent.locks.LockSupport.park(LockSupport.java:369)",
        "java.base\/sun.nio.ch.Poller.poll2(Poller.java:139)",
        "java.base\/sun.nio.ch.Poller.poll(Poller.java:102)",
        "java.base\/sun.nio.ch.Poller.poll(Poller.java:87)",
        "java.base\/sun.nio.ch.NioSocketImpl.park(NioSocketImpl.java:175)",
        "java.base\/sun.nio.ch.NioSocketImpl.park(NioSocketImpl.java:196)",
        "java.base\/sun.nio.ch.NioSocketImpl.implRead(NioSocketImpl.java:304)",
        "java.base\/sun.nio.ch.NioSocketImpl.read(NioSocketImpl.java:340)",
        "java.base\/sun.nio.ch.NioSocketImpl$1.read(NioSocketImpl.java:789)",
        "java.base\/java.net.Socket$SocketInputStream.read(Socket.java:1025)",
        "java.base\/java.io.BufferedInputStream.fill(BufferedInputStream.java:255)",
        "java.base\/java.io.BufferedInputStream.read1(BufferedInputStream.java:310)",
        "java.base\/java.io.BufferedInputStream.implRead(BufferedInputStream.java:382)",
        "java.base\/java.io.BufferedInputStream.read(BufferedInputStream.java:361)",
        "java.base\/sun.net.www.http.HttpClient.parseHTTPHeader(HttpClient.java:827)",
        "java.base\/sun.net.www.http.HttpClient.parseHTTP(HttpClient.java:759)",
        "java.base\/sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1684)",
        "java.base\/sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1585)",
        "java.base\/java.net.URL.openStream(URL.java:1162)",
        "com.rhino.vt.VtExample.getURL(VtExample.java:59)",
        "com.rhino.vt.VtExample.lambda$retrieveURLs$0(VtExample.java:40)",
        "java.base\/java.util.concurrent.ThreadPerTaskExecutor$ThreadBoundFuture.run(ThreadPerTaskExecutor.java:352)",
        "java.base\/java.lang.VirtualThread.run(VirtualThread.java:287)",
        "java.base\/java.lang.VirtualThread$VThreadContinuation.lambda$new$0(VirtualThread.java:174)",
        "java.base\/jdk.internal.vm.Continuation.enter0(Continuation.java:327)",
        "java.base\/jdk.internal.vm.Continuation.enter(Continuation.java:320)"
     ]
   }
  ],
  "threadCount": "1"
}
```

作为对比，把代码中的executor改成Executors.newCachedThreadPool()，再dump出直接使用普通线程的堆栈：

```css
{
   "tid": "23",
   "name": "pool-1-thread-2",
   "stack": [
      "java.base\/sun.nio.ch.SocketDispatcher.read0(Native Method)",
      "java.base\/sun.nio.ch.SocketDispatcher.read(SocketDispatcher.java:47)",
      "java.base\/sun.nio.ch.NioSocketImpl.tryRead(NioSocketImpl.java:251)",
      "java.base\/sun.nio.ch.NioSocketImpl.implRead(NioSocketImpl.java:302)",
      "java.base\/sun.nio.ch.NioSocketImpl.read(NioSocketImpl.java:340)",
      "java.base\/sun.nio.ch.NioSocketImpl$1.read(NioSocketImpl.java:789)",
      "java.base\/java.net.Socket$SocketInputStream.read(Socket.java:1025)",
      "java.base\/java.io.BufferedInputStream.fill(BufferedInputStream.java:255)",
      "java.base\/java.io.BufferedInputStream.read1(BufferedInputStream.java:310)",
      "java.base\/java.io.BufferedInputStream.implRead(BufferedInputStream.java:382)",
      "java.base\/java.io.BufferedInputStream.read(BufferedInputStream.java:361)",
      "java.base\/sun.net.www.http.HttpClient.parseHTTPHeader(HttpClient.java:827)",
      "java.base\/sun.net.www.http.HttpClient.parseHTTP(HttpClient.java:759)",
      "java.base\/sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1684)",
      "java.base\/sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1585)",
      "java.base\/java.net.URL.openStream(URL.java:1162)",
      "com.rhino.vt.VtExample.getURL(VtExample.java:59)",
      "com.rhino.vt.VtExample.lambda$retrieveURLs$0(VtExample.java:40)",
      "java.base\/java.util.concurrent.FutureTask.run(FutureTask.java:317)",
      "java.base\/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)",
      "java.base\/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)",
      "java.base\/java.lang.Thread.run(Thread.java:1589)"
   ]
 }
```

两个堆栈对比一下会发现，除了中间执行的业务逻辑部分是一致的，有两点不同：

1. 普通线程的入口是Thread.run，而虚拟线程的入口是Continuation，这个类是虚拟线程的核心类，是VM内部对上面所说的continuation的抽象。Continuation有两个关键方法：yield()和run()。

可以试着跑一下这段代码看看输出结果：

 ```java
 public void testContinuation() {
     var scope = new ContinuationScope("test");
     var continuation = new Continuation(scope, () -> {
         System.out.println("C1");
         Continuation.yield(scope);
         System.out.println("C2");
         Continuation.yield(scope);
         System.out.println("C3");
         Continuation.yield(scope);
     });
     System.out.println("start");
     continuation.run();
     System.out.println("came back");
     continuation.run();
     System.out.println("back again");
     continuation.run();
     System.out.println("back again again");
     continuation.run();
 }
 
 // Output:
 start
 C1
 came back
 C2
 back again
 C3
 back again again
 ```

PS：在Java19中还是预览版，需要加上下面的参数：（Java21后已经是正式版了）

```
--add-opens java.base/jdk.internal.vm=ALL-UNNAMED
```

 

2. 普通线程会阻塞在read本地方法调用上（底层应该就是read系统调用），而虚拟线程则会通过VirtualThread#park挂起，这也对应了上面说的，JDK中几乎所有的阻塞点都已经过调整了。VirtualThread维护了一组state状态，调用park后就会设置成PARKING，可以在注释里看到状态之间的流转逻辑。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406160131751.webp)


在线程dump文件里还能找到一个叫Read-Poller的线程（对应的还有一个写操作的 Write-Poller线程）：

 ```css
 {
    "tid": "27",
    "name": "Read-Poller",
    "stack": [
       "java.base\/sun.nio.ch.KQueue.poll(Native Method)",
       "java.base\/sun.nio.ch.KQueuePoller.poll(KQueuePoller.java:66)",
       "java.base\/sun.nio.ch.Poller.poll(Poller.java:363)",
       "java.base\/sun.nio.ch.Poller.pollLoop(Poller.java:270)",
       "java.base\/java.lang.Thread.run(Thread.java:1589)",
       "java.base\/jdk.internal.misc.InnocuousThread.run(InnocuousThread.java:186)"
    ]
  }
 ```



JDK底层做了什么调整呢？从Read-Poller可以看出，其实就是把原来的阻塞调用改为了非阻塞的IO调用。流程如下：

1. 在阻塞调用中，检查是否虚拟线程，如果是的话，就注册一个NIO handler，即将文件描述符注册到Read-Poller的selector上。然后调用Continuation.yield()暂停自身。因为我本机是mac，所以线程堆栈里显示的NIO handler用的是KQueue，如果换成Linux，那就是我们熟悉的epoll了。
2. Read-Poller底层维护了一组文件描述符 - 虚拟线程的映射，当一个文件描述符被注册到Read-Poller上时，同样也会将对应的虚拟线程加到这个映射里。 
3. 当Socket可读时，这个Read-Poller就会得到通知，随即调用wakeup()方法，从映射里找到文件描述符对应的虚拟线程，再将之前park()的虚拟线程unpark()，这样就完成了虚拟线程的唤醒。

```java
/**
 * Unparks any thread that is polling the given file descriptor.
 */
private void wakeup(int fdVal) {
    Thread t = map.remove(fdVal);
    if (t != null) {
        LockSupport.unpark(t);
    }
}
```

虚拟线程的unpark()方法如下：

```java
/**
 * Re-enables this virtual thread for scheduling. If the virtual thread was
 * {@link #park() parked} then it will be unblocked, otherwise its next call
 * to {@code park} or {@linkplain #parkNanos(long) parkNanos} is guaranteed
 * not to block.
 * @throws RejectedExecutionException if the scheduler cannot accept a task
 */
@Override
@ChangesCurrentThread
void unpark() {
    Thread currentThread = Thread.currentThread();
    if (!getAndSetParkPermit(true) && currentThread != this) {
        int s = state();
        // CAS设置线程状态
        if (s == PARKED && compareAndSetState(PARKED, RUNNABLE)) {
            if (currentThread instanceof VirtualThread vthread) {
                Thread carrier = vthread.carrierThread;
                carrier.setCurrentThread(carrier);
                try {
                    // 提交给scheduler执行
                    submitRunContinuation();
                } finally {
                    carrier.setCurrentThread(vthread);
                }
            } else {
                submitRunContinuation();
            }
        } else if (s == PINNED) {
            // unpark carrier thread when pinned.
            synchronized (carrierThreadAccessLock()) {
                Thread carrier = carrierThread;
                if (carrier != null && state() == PINNED) {
                    U.unpark(carrier);
                }
            }
        }
    }
}
```

在unpark()中，会将虚拟线程的状态重新设置为RUNNABLE，并且调用submitRunContinuation()方法将任务交给调度器执行，真正执行时，就会调用到Continuation.run()方法。另外，上面调用executor.invokeAll()方法提交任务时，底层同样也是调用了VirtualThread.submitRunContinuation()方法，这里的scheduler默认就是ForkJoinPool实例。

```java
/**
 * Submits the runContinuation task to the scheduler.
 * @param {@code lazySubmit} to lazy submit
 * @throws RejectedExecutionException
 * @see ForkJoinPool#lazySubmit(ForkJoinTask)
 */
private void submitRunContinuation(boolean lazySubmit) {
    try {
        if (lazySubmit && scheduler instanceof ForkJoinPool pool) {
            pool.lazySubmit(ForkJoinTask.adapt(runContinuation));
        } else {
            // 默认shceduler就是ForkJoinPool
            scheduler.execute(runContinuation);
        }
    } catch (RejectedExecutionException ree) {
        // 省略异常处理代码
    }
}
```

而在park()里，虚拟线程让出资源的关键方法是VirtualThread.yieldContinuation()，可以发现mount()和unmount()操作。

```java

/**
 * Unmounts this virtual thread, invokes Continuation.yield, and re-mounts the
 * thread when continued. When enabled, JVMTI must be notified from this method.
 * @return true if the yield was successful
 */
@ChangesCurrentThread
private boolean yieldContinuation() {
    boolean notifyJvmti = notifyJvmtiEvents;

    // unmount
    if (notifyJvmti) notifyJvmtiUnmountBegin(false);
    unmount();
    try {
        return Continuation.yield(VTHREAD_SCOPE);
    } finally {
        // re-mount
        mount();
        if (notifyJvmti) notifyJvmtiMountEnd(false);
    }
}
```

mount()和unmount()会在Java堆和本地线程栈之间做栈帧的拷贝，这是Project Loom中为数不多的在JVM层面实现的本地方法，感兴趣的可以去Loom的github库里搜一下continuationFreezeThaw.cpp。其余的大部分代码在JDK中实现， 参见java.base模块下的jdk.internal.vm包。



<!-- @include: @article-footer.snippet.md -->     
