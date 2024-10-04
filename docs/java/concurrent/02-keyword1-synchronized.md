---
title: 关键字 - synchronized详解
category: Java
tag:
 - 并发编程
head:
  - - meta
    - name: keywords
      content: synchronized,实现原理,底层源码,锁的优化,可重入性,锁粗化,偏向锁
  - - meta
    - name: description
      content: 全网最全的Java并发编程知识点总结，让天下没有难学的八股文！
---





## 概述

在应用Sychronized关键字时需要把握如下注意点：

- 一把锁只能同时被一个线程获取，没有获得锁的线程只能等待；

- 每个实例都对应有自己的一把锁(this),不同实例之间互不影响；例外：锁对象是*.class以及synchronized修饰的是static方法的时候，所有对象公用同一把锁

- synchronized修饰的方法，无论方法正常执行完毕还是抛出异常，都会释放锁

 

### 锁的范围

- 对于普通同步方法，锁是当前实例对象

- 对于静态同步方法，锁是当前类的CIass对象

- 对于同步方法块，锁是Synchonized括号里配置的对象。

 

## 底层实现

### 加锁释放锁原理

synchronized是 Java内建的同步机制，所以也被称为 Intrinsic Locking，提供了互斥的语义和可见性，当一个线程已经获取当前锁时，其他试图获取锁的线程时只能等待或者阻塞在那里。

synchronized是基于一对 monitorenter/monitorexit 指令实现的，Monitor对象是同步的基本实现单元，无论是显示同步,还是隐式同步都是如此。区别是同步代码块是通过明确的 monitorenter 和 monitorexit 指令实现，而同步方法通过ACC_SYNCHRONIZED 标志来隐式实现。

#### 同步代码块

```java
public class Test1 {
    public void fun1(){
        synchronized (this){
            System.out.println("fun111111111111");
        }
    }
}
```

将.java文件使用javac命令编译为.class文件，然后将class文件反编译出来。反编译的字节码文件截取：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251023096.jpg)

 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251023120.gif)

通过反编译后的内容查看可以发现，synchronized编译后，同步块的前后有monitorenter/monitorexit两个 字节码指令。在Java虚拟机规范中有描述两条指令的作用：翻译一下如下：

每个对象有一个监视器锁（monitor）。当monitor被占用时就会处于锁定状态，线程执行monitorenter指令时尝试获取monitor的所有权，过程如下：

1. 如果monitor的进入数为0，则该线程进入monitor，然后将进入数设置为1，该线程即为monitor的所有者。
2. 如果线程已经占有该monitor，只是重新进入，则进入monitor的进入数加1.
3. 如果其他线程已经占用了monitor，则该线程进入阻塞状态，直到monitor的进入数为0，再重新尝试获取monitor的所有权

monitorexit：

1. 执行monitorexit的线程必须是objectref所对应的monitor的所有者。
2. 指令执行时，monitor的进入数减1，如果减1后进入数为0，那线程退出monitor，不再是这个monitor的所有者。其他被这个monitor阻塞的线程可以尝试去获取这个 monitor 的所有权。

> Q：synchronized 代码块内出现异常会释放锁吗？
>
> A：会自动释放锁，查看字节码指令可以知道，monitorexit插入在方法结束处(13行)和异常处(19行)。从Exception table异常表中也可以看出。

#### 同步方法代码

```java
public class Test1 {
    //锁当前对象(this)
    public synchronized void fun2(){
        System.out.println("fun2222222222222222222222");
    }
     //静态synchronized修饰:使用的锁对象是当前类的class对象
    public synchronized static void fun3(){
        System.out.println("fun33333333333333");
    }
}
```



编译之后反编译截图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251023111.gif)

从反编译的结果来看，同步方法表面上不是通过monitorenter/monitorexit指令来完成，但是与普通方法相比，常量池中多出来了ACC_SYNCHRONIZED标识符。java虚拟机就是根据ACC_SYNCHRONIZED标识符来实现方法的同步，当调用方法时，调用指令先检查方法是否有 ACC_SYNCHRONIZED访问标志，如果存在，执行线程将先获取monitor，获取成功之后才执行方法体，执行完后再释放monitor。在方法执行期间，其他线程都无法再获取到同一个monitor对象。 虽然编译后的结果看起来不一样，但实际上没有本质的区别，只是方法的同步是通过隐式的方式来实现，无需通过字节码来完成。

ACC_SYNCHRONIZED的访问标志，其实就是代表：当线程执行到方法后，如果检测到有该访问标志就会隐式的去调用monitorenter/monitorexit两个命令来将方法锁住。

#### 小结

synchronized 同步代码块的实现是通过 monitorenter 和 monitorexit 指令，其中 monitorenter 指令指向同步代码块的开始位置，monitorexit 指令则指明同步代码块的结束位置。当执行 monitorenter 指令时，线程试图获取锁也就是获取 monitor的持有权（monitor对象存在于每个Java对象的对象头中， synchronized 锁便是通过这种方式获取锁的，也是为什么Java中任意对象可以作为锁的原因）。

其内部包含一个计数器，当计数器为0则可以成功获取，获取后将锁计数器设为1也就是加1。相应的在执行 monitorexit 指令后，将锁计数器设为0 ，表明锁被释放。如果获取对象锁失败，那当前线程就要阻塞等待，直到锁被另外一个线程释放为止

synchronized 修饰的方法并没有 monitorenter 指令和 monitorexit 指令，取得代之的确实是ACC_SYNCHRONIZED 标识，该标识指明了该方法是一个同步方法，JVM 通过该 ACC_SYNCHRONIZED 访问标志来辨别一个方法是否声明为同步方法，从而执行相应的同步调用。

### 可重入锁原理

ReentrantLock和synchronized都是可重入锁

**定义**

指的是 同一个线程的 可以多次获得 同一把锁（一个线程可以多次执行synchronized，重复获取同一把锁）。

```java
/*  可重入特性    指的是 同一个线程获得锁之后，可以再次获取该锁。*/
public class Demo01 {
    public static void main(String[] args) {
        Runnable sellTicket = new Runnable() {
            @Override
            public void run() {
                synchronized (Demo01.class) {
                    System.out.println("我是run");
                    test01();
                }
            }

            public void test01() {
                synchronized (Demo01.class) {
                    System.out.println("我是test01");
                }
            }
        };
        new Thread(sellTicket).start();
        new Thread(sellTicket).start();
    }
}
```



**原理**

synchronized 的锁对象中有一个计数器（recursions变量）会记录线程获得几次锁，每重入一次，计数器就 + 1，在执行完一个同步代码块时，计数器数量就会减1，直到计数器的数量为0才释放这个锁。

- 执行monitorenter获取锁 ：

  - （monitor计数器=0，可获取锁）

  - 执行method1()方法，monitor计数器+1 -> 1 （获取到锁）

  - 执行method2()方法，monitor计数器+1 -> 2

  - 执行method3()方法，monitor计数器+1 -> 3

- 执行monitorexit命令 ：

  - method3()方法执行完，monitor计数器-1 -> 2

  - method2()方法执行完，monitor计数器-1 -> 1

  - method2()方法执行完，monitor计数器-1 -> 0 （释放了锁）

  - （monitor计数器=0，锁被释放了）

**优点**

可以一定程度上避免死锁（如果不能重入，那就不能再次进入这个同步代码块，导致死锁）；更好地封装代码（可以把同步代码块写入到一个方法中，然后在另一个同步代码块中直接调用该方法实现可重入）；

### 保证可见性原理

这个主要在于内存模型和happens-before规则

Synchronized的happens-before规则，即监视器锁规则：对同一个监视器的解锁，happens-before于对该监视器的加锁。

```java
public class MonitorDemo {
    private int a = 0;

    public synchronized void writer() {     // 1
        a++;                                // 2
    }                                       // 3

    public synchronized void reader() {    // 4
        int i = a;                         // 5
    }                                      // 6
}
```



该代码的happens-before关系如图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251023090.jpg)

在图中每一个箭头连接的两个节点就代表之间的happens-before关系，黑色的是通过程序顺序规则推导出来，红色的为监视器锁规则推导而出：线程A释放锁happens-before线程B加锁，蓝色的则是通过程序顺序规则和监视器锁规则推测出来happens-befor关系，通过传递性规则进一步推导的happens-before关系。

这里是2 happens-before 5，通过这个关系可以得出：根据happens-before的定义中的一条:如果A happens-before B，则A的执行结果对B可见，并且A的执行顺序先于B。线程A先对共享变量A进行加一，由2 happens-before 5关系可知线程A的执行结果对线程B可见即线程B所读取到的a的值为1

## 对象布局

HotSpot虚拟机中，对象在内存中存储的布局可以分为三块区域：对象头（Header）、实例数据（Instance Data）和对齐填充（Padding）。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251023099.jpg)

### Java对象头

Hotspot的对象头主要包括三部分数据：Mark Word（标记字段）、Klass Pointer（类型指针）、数组长度

其中Mark Word是用于存储对象自身运行时数据，如HashCode、GC分代年龄、**锁状态标志、线程持有锁、偏向线程ID、偏向时间戳**等信息。

#### Mark Word

比如 hash码，对象所属的年代，对象锁，锁状态标志，偏向锁（线程）ID，偏向时间，数组长度（数组对象）等。Java对象头一般占有2个机器码（64位虚拟机中，1个机器码是8个字节，也就是64bit）。

![64位Mark Word的结构信息](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211310009.png)

关于对象头的详细内容可以看[Java对象头](https://www.seven97.top/java/jvm/01-jvmbasic1-objectstructure.html)这篇文章

## synchronized锁的优化

JVM中monitorenter和monitorexit字节码依赖于底层的操作系统的Mutex Lock来实现的，但是由于使用Mutex Lock需要将当前线程挂起并从用户态切换到内核态来执行，这种切换的代价是非常昂贵的；然而在现实中的大部分情况下，同步方法是运行在单线程环境(无锁竞争环境)如果每次都调用Mutex Lock那么将严重的影响程序的性能。**所以在jdk1.6中对锁的实现引入了大量的优化，如锁粗化(Lock Coarsening)、锁消除(Lock Elimination)、轻量级锁(Lightweight Locking)、偏向锁(Biased Locking)、适应性自旋(Adaptive Spinning)等技术来减少锁操作的开销**。

- 锁粗化(Lock Coarsening)：也就是减少不必要的紧连在一起的unlock，lock操作，将多个连续的锁扩展成一个范围更大的锁。

- 锁消除(Lock Elimination)：通过运行时JIT编译器的逃逸分析来消除一些没有在当前同步块以外被其他线程共享的数据的锁保护，通过逃逸分析也可以在线程本的Stack上进行对象空间的分配(同时还可以减少Heap上的垃圾收集开销)。

- 轻量级锁(Lightweight Locking)：这种锁实现的背后基于这样一种假设，即在真实的情况下我们程序中的大部分同步代码一般都处于无锁竞争状态(即单线程执行环境)，在无锁竞争的情况下完全可以避免调用操作系统层面的重量级互斥锁，取而代之的是在monitorenter和monitorexit中只需要依靠一条CAS原子指令就可以完成锁的获取及释放。当存在锁竞争的情况下，执行CAS指令失败的线程将调用操作系统互斥锁进入到阻塞状态，当锁被释放的时候被唤醒

- 偏向锁(Biased Locking)：是为了在无锁竞争的情况下避免在锁获取过程中执行不必要的CAS原子指令，因为CAS原子指令虽然相对于重量级锁来说开销比较小但还是存在非常可观的本地延迟。

- 适应性自旋(Adaptive Spinning)：当线程在获取轻量级锁的过程中执行CAS操作失败时，在进入与monitor相关联的操作系统重量级锁(mutex semaphore)前会进入忙等待(Spinning)然后再次尝试，当尝试一定的次数后如果仍然没有成功则调用与该monitor关联的semaphore(即互斥锁)进入到阻塞状态。

### Monitor

Monitor可以理解为一个同步工具或一种同步机制，通常被描述为一个对象。每一个Java对象就有一把看不见的锁，称为内部锁或者Monitor锁。

Monitor的基本结构是什么？

- Owner字段：初始时为NULL表示当前没有任何线程拥有该monitor record，当线程成功拥有该锁后保存线程唯一标识，当锁被释放时又设置为NULL

- EntryQ字段：关联一个系统互斥锁（semaphore），阻塞所有试图锁住monitor record失败的线程

- RcThis字段：表示blocked或waiting在该monitor record上的所有线程的个数

- Nest字段：用来实现重入锁的计数

- HashCode字段：保存从对象头拷贝过来的HashCode值（可能还包含GC age）

- Candidate字段：用来避免不必要的阻塞或等待线程唤醒，因为每一次只有一个线程能够成功拥有锁，如果每次前一个释放锁的线程唤醒所有正在阻塞或等待的线程，会引起不必要的上下文切换（从阻塞到就绪然后因为竞争锁失败又被阻塞）从而导致性能严重下降；Candidate只有两种可能的值0表示没有需要唤醒的线程1表示要唤醒一个继任线程来竞争锁

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251023238.gif)

1. 刚开始Monitor中Owner为null
2. 当Thread-2执行Synchronized(obj)就会将Monitor的所有者owner置为Thread-2。Monitor中只能有一个Owner。
3. 在Thread-2上锁的过程中，如果Thread-3，Thread-4，Thread-5也来执行Synchronized(obj)，就会进入EntryList Blocked。
4. Thread-2执行完同步代码块的内容，然后唤醒EntryList中等待的线程来竞争锁，竞争时是非公平的
5. 图中WaitSet中的Thread-0，Thread-1是之前获得过锁，但条件不满足而进入waiting状态的线程

Monitor是线程私有的数据结构，每一个线程都有一个可用monitor record列表，同时还有一个全局的可用列表。每一个被锁住的对象都会和一个monitor关联，同时monitor中有一个Owner字段存放拥有该锁的线程的唯一标识，表示该锁被这个线程占用。

synchronized通过Monitor来实现线程同步，Monitor是依赖于底层的操作系统的Mutex Lock（互斥锁）来实现的线程同步。这种依赖于操作系统Mutex Lock所实现的锁我们称之为“重量级锁”，因此，为了减少获得锁和释放锁带来的性能消耗，引入了“偏向锁”和“轻量级锁”。

### 锁的类型

在Java SE 1.6里Synchronied同步锁，一共有四种状态：无锁、偏向锁、轻量级锁、重量级锁，它会随着竞争情况逐渐升级。锁可以升级但是不可以降级，目的是为了提供获取锁和释放锁的效率。

锁升级过程： 无锁 → 偏向锁 → 轻量级锁 → 重量级锁 (此过程是不可逆的

synchronized是悲观锁，在操作同步资源之前需要给同步资源先加锁，这把锁就是存在Java对象头里的。

**四种锁状态对应的的Mark Word内容：**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251023264.gif)

### 锁粗化

原则上，我们都知道在加同步锁时，尽可能的将同步块的作用范围限制到尽量小的范围(只在共享数据的实际作用域中才进行同步，这样是为了使得需要同步的操作数量尽可能变小。在存在锁同步竞争中，也可以使得等待锁的线程尽早的拿到锁)。

大部分上述情况是完美正确的，但是如果存在连串的一系列操作都对同一个对象反复加锁和解锁，甚至加锁操作时出现在循环体中的，那即使没有线程竞争，频繁的进行互斥同步操作也会导致不必要的性能操作。

这里贴上根据上述Javap 编译的情况编写的实例java类

```java
public static String test04(String s1, String s2, String s3) {
    StringBuffer sb = new StringBuffer();
    sb.append(s1);
    sb.append(s2);
    sb.append(s3);
    return sb.toString();
}
```

在上述的连续append()操作中就属于这类情况。JVM会检测到这样一连串的操作都是对同一个对象加锁，那么JVM会将加锁同步的范围扩展(粗化)到整个一系列操作的 外部，使整个一连串的append()操作只需要加锁一次就可以了。

### 锁消除

锁消除是指虚拟机即时编译器再运行时，对一些代码上要求同步，但是被检测到不可能存在共享数据竞争的锁进行消除。锁消除的主要判定依据来源于**逃逸分析**的数据支持。意思就是：JVM会判断再一段程序中的同步明显不会逃逸出去从而被其他线程访问到，那JVM就把它们当作栈上数据对待，认为这些数据是线程独有的，不需要加同步。此时就会进行锁消除。

当然在实际开发中，我们很清楚的知道哪些是线程独有的，不需要加同步锁，但是在Java API中有很多方法都是加了同步的，那么此时JVM会判断这段代码是否需要加锁。如果数据并不会逃逸，则会进行锁消除。比如如下操作：在操作String类型数据时，由于String是一个不可变类，对字符串的连接操作总是通过生成的新的String对象来进行的。因此Javac编译器会对String连接做自动优化。在JDK 1.5之前会使用StringBuffer对象的连续append()操作，在JDK 1.5及以后的版本中，会转化为StringBuidler对象的连续append()操作。

```java
public static String test03(String s1, String s2, String s3) {
    String s = s1 + s2 + s3;
    return s;
}
```

上述代码使用javap 编译结果

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251023288.jpg)

众所周知，StringBuilder不是安全同步的，但是在上述代码中，JVM判断该段代码并不会逃逸，则将该代码带默认为线程独有的资源，并不需要同步，所以执行了锁消除操作。(还有Vector中的各种操作也可实现锁消除。在没有逃逸出数据安全防卫内)

### 偏向锁

> 通俗的讲，偏向锁就是在运行过程中，对象的锁偏向某个线程。即在开启偏向锁机制的情况下，某个线程获得锁，当该线程下次再想要获得锁时，不需要再获得锁（即忽略synchronized关键词），直接就可以执行同步代码，比较适合竞争较少的情况。

 为了解决这一问题，HotSpot的作者在Java SE 1.6 中对Synchronized进行了优化，引入了偏向锁。当一个线程访问同步块并获取锁时，会在对象头和栈帧中的锁记录里存储锁偏向的线程ID，以后该线程在进入和退出同步块时不需要进行CAS操作来加锁和解锁。只需要简单的测试一下对象头的Mark Word里是否存储着指向当前线程的偏向锁。如果成功，表示线程已经获取到了锁。

#### 偏向锁的获取流程

1. 查看Mark Word中偏向锁的标识以及锁标志位，若是否偏向锁为1且锁标志位为01，则该锁为可偏向状态。
2. 若为可偏向状态，则测试Mark Word中的线程ID是否与当前线程相同，若相同，则直接执行同步代码，否则进入下一步。
3. 当前线程通过CAS操作竞争锁，若竞争成功，则将Mark Word中线程ID设置为当前线程ID，然后执行同步代码，若竞争失败，进入下一步。
4. 当前线程通过CAS竞争锁失败的情况下，说明有竞争。当到达全局安全点时之前获得偏向锁的线程被挂起，偏向锁升级为轻量级锁，然后被阻塞在安全点的线程继续往下执行同步代码。

#### 偏向锁的释放流程：

偏向锁只有遇到其他线程尝试竞争偏向锁时，持有偏向锁状态的线程才会释放锁，线程不会主动去释放偏向锁。偏向锁的撤销，需要等待全局安全点（在这个时间点上没有字节码正在执行），它会首先暂停拥有偏向锁的线程，判断锁对象是否处于被锁定状态。撤销偏向锁后恢复到无锁（标志位为“01”）或升级到轻量级锁（标志位为“00”）的状态

### 轻量级锁

在JDK 1.6之后引入的轻量级锁，需要注意的是**轻量级锁并不是替代重量级锁的**，而是对在大多数情况下同步块并不会有竞争出现提出的一种优化。它可以减少重量级锁对线程的阻塞带来的线程开销。从而提高并发性能。

但是当多个线程同时竞争锁时，轻量级锁会膨胀为重量级锁。

#### 轻量级锁加锁

1. 当线程执行代码进入同步块时，若Mark Word为无锁状态，虚拟机先在当前线程的栈帧中建立一个名为Lock Record的空间，用于存储当前对象的Mark Word的拷贝，官方称之为“Dispalced Mark Word”
2. 复制对象头中的Mark Word到锁记录中。
3. 复制成功后，虚拟机将用CAS操作将对象的Mark Word更新为执行Lock Record的指针，并将Lock Record里的owner指针指向对象的Mark Word。如果更新成功，则执行4，否则执行5。；
4. 如果更新成功，则这个线程拥有了这个锁，并将锁标志设为00，表示处于轻量级锁状态
5. 如果更新失败，虚拟机会检查对象的Mark Word是否指向当前线程的栈帧，如果是则说明当前线程已经拥有这个锁，可进入执行同步代码。否则说明多个线程竞争，轻量级锁就会膨胀为重量级锁，Mark Word中存储重量级锁（互斥锁）的指针，后面等待锁的线程也要进入阻塞状态。

#### 轻量级锁解锁

使用原子的CAS操作将Displaced Mark Word替换回到对象头中，如果成功，则表示没有发生竞争关系。如果失败，表示当前锁存在竞争关系。锁就会膨胀成重量级锁。

### 锁的优缺点

| 锁       | 优点                                                         | 缺点                                                         | 使用场景                           |
| -------- | ------------------------------------------------------------ | ------------------------------------------------------------ | ---------------------------------- |
| 偏向锁   | 加锁和解锁不需要CAS操作，没有额外的性能消耗，和执行非同步方法相比仅存在纳秒级的差距 | 如果线程间存在锁竞争，会带来额外的锁撤销的消耗               | 适用于只有一个线程访问同步块的场景 |
| 轻量级锁 | 竞争的线程不会阻塞，提高了响应速度                           | 如线程始终得不到锁竞争的线程，使用自旋会消耗CPU性能          | 追求响应时间，同步块执行速度非常快 |
| 重量级锁 | 线程竞争不适用自旋，不会消耗CPU                              | 线程阻塞，响应时间缓慢，在多线程下，频繁的获取释放锁，会带来巨大的性能消耗 | 追求吞吐量，同步块执行速度较长     |

### 锁升级过程

1. 无锁：无竞争
2. 偏向锁：长时间只有一个线程访问，就会获得偏向锁
3. 轻量级锁：当前锁是偏向锁并且被另外一个线程访问时，偏向锁就会升级为轻量级锁
4. 重量级锁：若当前只有一个等待线程，则该线程通过自旋进行等待。但是当自旋超过一定的次数，或者一个线程在持有锁，一个在自旋，又有第三个来访时，轻量级锁升级为重量级锁。

## Synchronized与ReentrantLock

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251023310.jpg)

### synchronized的缺陷

- 效率：锁的释放情况少，只有代码执行完毕或者异常结束才会释放锁；试图获取锁的时候不能设定超时，不能中断一个正在使用锁的线程，相对而言，Lock可以中断和设置超时

- 不够灵活：加锁和释放的时机单一，每个锁仅有一个单一的条件(某个对象)，相对而言，读写锁更加灵活

- 无法知道是否成功获得锁，相对而言，Lock可以拿到状态

- synchronized无法中断

### Lock解决相应问题

Lock类这里不做过多解释，主要看里面的4个方法:

- lock()

- unlock()

- tryLock()

- tryLock(long,TimeUtil)

Synchronized加锁只与一个条件(是否获取锁)相关联，不灵活，后来Condition与Lock的结合解决了这个问题。

多线程竞争一个锁时，其余未得到锁的线程只能不停的尝试获得锁，而不能中断。高并发的情况下会导致性能下降。ReentrantLock的lockInterruptibly()方法可以优先考虑响应中断。 一个线程等待时间过长，它可以中断自己，然后ReentrantLock响应这个中断，不再让这个线程继续等待。有了这个机制，使用ReentrantLock时就不会像synchronized那样产生死锁了。

> ReentrantLock为常用类，它是一个可重入的互斥锁 Lock，它具有与使用 synchronized 方法和语句所访问的隐式监视器锁相同的一些基本行为和语义，但功能更强大。

 synchronized是通过软件(JVM)实现的，简单易用，即使在JDK5之后有了Lock，仍然被广泛的使用。

 

### Synchronized使用注意点

1. 锁对象不能为空，因为锁的信息都保存在对象头里
2. 作用域不宜过大，影响程序执行的速度，控制范围过大，编写代码也容易出错
3. 避免死锁
4. 在能选择的情况下，既不要用Lock也不要用synchronized关键字，用java.util.concurrent包中的各种各样的类，如果不用该包下的类，在满足业务的情况下，可以使用synchronized关键，因为代码量少，避免出错

 


<!-- @include: @article-footer.snippet.md -->     
