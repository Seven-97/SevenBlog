---
title: 抽象队列同步器 - AQS详解（以ReentrantLock为例）
category: Java
tag:
  - 并发编程
  - JUC
head:
  - - meta
    - name: keywords
      content: Java,并发编程,多线程,Thread,AQS,抽象队列同步器,实现原理,底层源码,ReentrantLock,锁
  - - meta
    - name: description
      content: 全网最全的Java并发编程知识点总结，让天下没有难学的八股文！
---





 

## 概述

AQS ( Abstract Queued Synchronizer ）是一个抽象的队列同步器，通过维护一个共享资源状态（ Volatile Int State ）来表示同步状态 和一个先进先出（ FIFO ）的线程**等待队列**来完成资源获取的排队工作，通过CAS完成对State值的修改。

AQS整体框架如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047981.gif)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047987.gif)

当有自定义同步器接入时，只需重写第一层所需要的部分方法即可，不需要关注底层具体的实现流程。当自定义同步器进行加锁或者解锁操作时，先经过第一层的API进入AQS内部方法，然后经过第二层进行锁的获取，接着对于获取锁失败的流程，进入第三层和第四层的等待队列处理，而这些处理方式均依赖于第五层的基础数据提供层

## 原理

AQS 为每个共享资源都设置一个共享资源锁，线程在需要访问共享资源时首先需要获取共享资源锁，如果获取到了共享资源锁，便可以在当前线程中使用该共享资源，如果获取不到，则将该线程放入线程等待队列，等待下一次资源调度，流程图如下所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047983.gif)

Java中的大部分同步类（Lock、Semaphore、ReentrantLock等）都是基于AbstractQueuedSynchronizer（简称为AQS）实现的。

## 底层结构

### state：状态

Abstract Queued Synchronizer 维护了 volatile int 类型的变量，用于表示当前的同步状态。volatile虽然不能保证操作的原子性，但是能保证当前变量state的可见性。

state的访问方式有三种： getState()、setState()和 compareAndSetState()，均是原子操作，其中，compareAndSetState的实现依赖于 Unsafe的compareAndSwaplnt()

```java
// java.util.concurrent.locks.AbstractQueuedSynchronizer
private volatile int state;

protected final int getState() {
    return state;
}

protected final void setState(int newState) {
    state = newState;
}

protected final boolean compareAndSetState(int expect, int update) {
    // See below for intrinsics setup to support this
    return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
}
```



 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047977.gif)

 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047997.gif)

### CLH队列

Craig、Landin and Hagersten队列，是单向链表，AQS中的队列是CLH变体的虚拟双向队列（FIFO），AQS是通过将每条请求共享资源的线程封装成一个节点来实现锁的分配。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047000.gif)

AQS使用一个Volatile的int类型的成员变量来表示同步状态，通过内置的FIFO队列来完成资源获取的排队工作，通过CAS完成对State值的修改。

### AQS的独占式和共享式

- 独占式:只有一个线程能执行，具体的 Java 实现有 ReentrantLock。

- 共享式：多个线程可同时执行，具体的 Java 实现有 Semaphore和CountDownLatch。

AQS只是一个框架 ，只定义了一个接口，具体资源的获取、释放都由自定义同步器去实现。不同的自定义同步器争用共享资源的方式也不同，自定义同步器在实现时只需实现共享资源state的获取与释放方式即可，至于具体线程等待队列的维护，如获取资源失败入队、唤醒出队等， AQS 已经在顶层实现好（就是模板方法模式），不需要具体的同步器再做处理。自定义同步器实现时主要实现以下几种方法：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047629.gif)

 

- 以ReentrantLock为例，ReentrantLock中的state初始值为0表示无锁状态。在线程执行 tryAcquire()获取该锁后ReentrantLock中的state+1，这时该线程独占ReentrantLock锁，其他线程在通过tryAcquire() 获取锁时均会失败，直到该线程释放锁后state再次为0，其他线程才有机会获取该锁。该线程在释放锁之前可以重复获取此锁，每获取一次便会执行一次state+1, 因此ReentrantLock也属于可重入锁。 但获取多少次锁就要释放多少次锁，这样才能保证state最终为0。如果获取锁的次数多于释放锁的次数，则会出现该线程一直持有该锁的情况；如果获取锁的次数少于释放锁的次数，则运行中的程序会报锁异常。

- 以CountDownLatch以例，任务分为N个子线程去执行，state也初始化为N（注意N要与线程个数一致）。这N个子线程是并行执行的，每个子线程执行完后countDown()一次，state会CAS减1。等到所有子线程都执行完后(即state=0)，会unpark()主调用线程，然后主调用线程就会从await()函数返回，继续后面的动作。

- 以Semaphore为例，state则代表可以同时访问的线程数量，也可能理解为访问的许可证（permit）数量。每个线程访问(acquire)时需要拿到对应的许可证，否则进行阻塞，访问结束则返还（release）许可证。state只能在Semaphore的构造方法中进行初始化，后续不能进行修改。

一般来说，自定义同步器要么是独占方法，要么是共享方式，他们也只需实现tryAcquire-tryRelease、tryAcquireShared-tryReleaseShared中的一种即可。但AQS也支持自定义同步器同时实现独占和共享两种方式，如ReentrantReadWriteLock。

### Node节点

Node即为上面CLH变体队列中的节点。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047676.gif)

Node结点是每一个等待获取资源的线程的封装，其包含了需要同步的线程本身及其等待状态waitStatus

Node中几个方法和属性值的含义：

- waitStatus：当前节点在队列中的状态

- thread：表示处于该节点的线程

- prev：前驱指针

- predecessor：返回前驱节点，没有的话抛出npe

- nextWaiter：指向下一个处于CONDITION状态的节点（由于本篇文章不讲述Condition Queue队列，这个指针不多介绍）

- next：后继指针

### 等待状态waitStatus

waitStatus有下面几个枚举值：如是否被阻塞、是否等待唤醒、是否已经被取消等。共有5种取值CANCELLED、SIGNAL、CONDITION、PROPAGATE、0。

- CANCELLED(1)：表示当前结点已取消调度，不再想去获取资源了。当timeout或被中断（响应中断的情况下），会触发变更为此状态，进入该状态后的结点将不会再变化。

- SIGNAL(-1)：表示后继结点在等待当前结点唤醒。后继结点入队时，会将前继结点的状态更新为SIGNAL。

- CONDITION(-2)：表示结点等待在Condition上，当其他线程调用了Condition的signal()方法后，CONDITION状态的结点将从等待队列转移到同步队列中，等待获取同步锁。

- PROPAGATE(-3)：共享模式下，前继结点不仅会唤醒其后继结点，同时也可能会唤醒后继的后继结点。

- 0：新结点入队时的默认状态。

注意，负值表示结点处于有效等待状态，而正值表示结点已被取消。所以源码中很多地方用>0、<0来判断结点的状态是否正常。

## 源码

以ReentrantLock的非公平锁为例，将加锁和解锁的交互流程单独拎出来强调一下

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047699.gif)

加锁：

1. 通过ReentrantLock的加锁方法Lock进行加锁操作。
2. 会调用到内部类 Sync的Lock方法，由于Sync#lock是抽象方法，根据 ReentrantLock初始化选择的公平锁和非公平锁，执行相关内部类的Lock方法，本质上都会执行AQS的 Acquire 方法。
3. AQS的 Acquire 方法会执行 tryAcquire 方法，但是由于tryAcquire需要自定义同步器实现，因此执行了ReentrantLock中的tryAcquire方法，由于ReentrantLock是通过公平锁和非公平锁内部类实现的tryAcquire方法，因此会根据锁类型不同，执行不同的tryAcquire。
4. tryAcquire是获取锁逻辑，获取失败后，会执行框架AQS的后续逻辑，跟ReentrantLock自定义同步器无关。

解锁：

1. 通过ReentrantLock的解锁方法Unlock进行解锁。
2. Unlock会调用内部类Sync的Release方法，该方法继承于AQS。
3. Release中会调用tryRelease方法，tryRelease需要自定义同步器实现，tryRelease只在ReentrantLock中的Sync实现，因此可以看出，释放锁的过程，并不区分是否为公平锁。
4. 释放成功后，所有处理由AQS框架完成，与自定义同步器无关。

### acquire(int)

此方法是独占模式下线程获取共享资源的顶层入口。如果获取到资源，线程直接返回，否则进入等待队列，直到获取到资源为止，且整个过程忽略中断的影响。这也正是lock()的语义，当然不仅仅只限于lock()。获取到资源后，线程就可以去执行其临界区代码了。

```java
public final void acquire(int arg) {
     if (!tryAcquire(arg) &&
         acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
         selfInterrupt();
}
```

函数流程如下：

1. tryAcquire()尝试直接去获取资源，如果成功则直接返回（这里体现了非公平锁，每个线程获取锁时会尝试直接抢占加塞一次，而CLH队列中可能还有别的线程在等待）；
2. addWaiter()将该线程加入等待队列的尾部，并标记为独占模式；
3. acquireQueued()使线程阻塞在等待队列中获取资源，一直获取到资源后才返回。如果在整个等待过程中被中断过，则返回true，否则返回false。
4. 如果线程在等待过程中被中断过，它是不响应的。只是获取资源后才再进行自我中断selfInterrupt()，将中断补上。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047722.gif)

关于整个函数流程详解，可以往下看

#### tryAcquire(int)

此方法尝试去获取独占资源。如果获取成功，则直接返回true，否则直接返回false。这也正是tryLock()的语义，当然不仅仅只限于tryLock()。

```java
protected boolean tryAcquire(int arg) {
     throw new UnsupportedOperationException();
}
```

这里是AQS的方法，所以直接throw异常，而没有具体的实现。原因就在于AQS只是一个框架，具体资源的获取/释放方式交由自定义同步器去实现。

这里之所以没有定义成abstract，是因为独占模式下只用实现tryAcquire-tryRelease，而共享模式下只用实现tryAcquireShared-tryReleaseShared。如果都定义成abstract，那么每个模式也要去实现另一模式下的接口。

 

**ReentrantLock实现公平锁非公平锁则主要体现在tryAcquire的实现上：**

公平锁中实现的tryAcquire：

```java
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
           if (!hasQueuedPredecessors() &&  //公平锁加锁时判断等待队列中是否存在有效节点的方法
                compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
           }
     }
     else if (current == getExclusiveOwnerThread()) {
           int nextc = c + acquires;
           if (nextc < 0)
                throw new Error("Maximum lock count exceeded");
           setState(nextc);
           return true;
     }
     return false;
}
```



非公平锁中实现的tryAcquire：

```java
protected final boolean tryAcquire(int acquires) {
    return nonfairTryAcquire(acquires);
}

final boolean nonfairTryAcquire(int acquires) {
     final Thread current = Thread.currentThread();
     int c = getState();
     if (c == 0) {
           if (compareAndSetState(0, acquires)) {
               setExclusiveOwnerThread(current);
               return true;
           }
      }
      else if (current == getExclusiveOwnerThread()) {
           int nextc = c + acquires;
           if (nextc < 0) // overflow
                throw new Error("Maximum lock count exceeded");
           setState(nextc);
           return true;
      }
      return false;
}
```



- 公平锁中多了一层 !hasQueuedPredecessors() 的判断，这是公平锁加锁时判断等待队列中是否存在有效节点的方法。如果返回False，说明当前线程可以获取共享资源；如果返回True，说明队列中存在有效节点，当前线程必须加入到等待队列中。

- 而在非公平锁中，没有这个判断，直接尝试获取锁，能获取到锁则不用加入等待队列。

```java
public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s;
        return h != t && ((s = h.next) == null || s.thread != Thread.currentThread());
}
```



这里的判断 h != t && ((s = h.next) == null || s.thread != Thread.currentThread());为什么要判断的头结点的下一个节点？第一个节点储存的数据是什么？

双向链表中，第一个节点为虚节点，其实并不存储任何信息，只是占位。真正的第一个有数据的节点，是在第二个节点开始的。当h != t时： 如果(s = h.next) == null，等待队列正在有线程进行初始化，但只是进行到了Tail指向Head，没有将Head指向Tail，此时队列中有元素，需要返回True。 如果(s = h.next) != null，说明此时队列中至少有一个有效节点。如果此时s.thread == Thread.currentThread()，说明等待队列的第一个有效节点中的线程与当前线程相同，那么当前线程是可以获取资源的；如果s.thread != Thread.currentThread()，说明等待队列的第一个有效节点线程与当前线程不同，当前线程必须加入进等待队列。

#### addWaiter(Node)

此方法用于将当前线程加入到等待队列的队尾，并返回当前线程所在的结点。

```java
private Node addWaiter(Node mode) {
    //以给定模式构造结点。mode有两种：EXCLUSIVE（独占）和SHARED（共享）
    Node node = new Node(Thread.currentThread(), mode);

    //尝试快速方式直接放到队尾。
    Node pred = tail;
    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }

    //上一步失败则通过enq入队。
    enq(node);
    return node;
}
```



主要的流程如下：

1. 通过当前的线程和锁模式新建一个节点。
2. Pred指针指向尾节点Tail。
3. 将New中Node的Prev指针指向Pred。
4. 通过compareAndSetTail方法，完成尾节点的设置。这个方法主要是对tailOffset和Expect进行比较，如果tailOffset的Node和Expect的Node地址是相同的，那么设置Tail的值为Update的值。

```java
// java.util.concurrent.locks.AbstractQueuedSynchronizer

static {
    try {
        stateOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("state"));
        headOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("head"));
        tailOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
        waitStatusOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("waitStatus"));
        nextOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));
    } catch (Exception ex) { 
    throw new Error(ex); 
  }
}
```

从AQS的静态代码块可以看出，都是获取一个对象的属性相对于该对象在内存当中的偏移量，这样我们就可以根据这个偏移量在对象内存当中找到这个属性。tailOffset指的是tail对应的偏移量，所以这个时候会将new出来的Node置为当前队列的尾节点。同时，由于是双向链表，也需要将前一个节点指向尾节点。

 

如果Pred指针是Null（说明等待队列中没有元素），或者当前Pred指针和Tail指向的位置不同（说明被别的线程已经修改）,就需要enq入队

```java
private Node enq(final Node node) {
    //CAS"自旋"，直到成功加入队尾
    for (;;) {
        Node t = tail;
        if (t == null) { // 队列为空，创建一个空的标志结点作为head结点，并将tail也指向它。
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {//正常流程，放入队尾
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}
```



如果没有被初始化，需要进行初始化一个头结点出来。但请注意，初始化的头结点并不是当前线程节点，而是调用了无参构造函数的节点。如果经历了初始化或者并发导致队列中有元素，则与之前的方法相同。其实，addWaiter就是一个在双端链表添加尾节点的操作，需要注意的是，双端链表的头结点是一个无参构造函数的头结点。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047743.gif)

#### acquireQueued(Node, int)

通过tryAcquire()和addWaiter()，该线程获取资源失败，已经被放入等待队列尾部了。addWaiter()返回的是一个包含该线程的Node。而这个Node会作为参数，进入到acquireQueued方法中。acquireQueued方法可以对排队中的线程进行“获锁”操作。那么下一步就是：如果获取不到锁，那么就进入阻塞状态休息，直到其他线程彻底释放资源后唤醒自己，自己再拿到资源，然后就可以去干自己想干的事了。

acquireQueued：在等待队列中排队拿号（中间没其它事干可以阻塞休息），直到拿到号后再返回。

```java
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;//标记是否成功拿到资源
    try {
        boolean interrupted = false;//标记等待过程中是否被中断过

        //CAS“自旋”！
        for (;;) {
            final Node p = node.predecessor();//拿到前驱
            //如果前驱是head，即该结点已成老二，那么便有资格去尝试获取资源，也就是当前节点在真实数据队列的首部，就尝试获取锁（别忘了头结点是虚节点）。
            if (p == head && tryAcquire(arg)) {
                setHead(node);// 获取锁成功，头指针移动到当前node
                p.next = null; // setHead中node.prev已置为null，此处再将head.next置为null，就是为了方便GC回收以前的head结点。也就意味着之前拿完资源的结点出队了！
                failed = false; // 成功获取资源
                return interrupted;//返回等待过程中是否被中断过
            }

            // 说明p为头节点且当前没有获取到锁（可能是非公平锁被抢占了）或者 是p不为头结点，这个时候就要判断当前node是否要被阻塞（被阻塞条件：前驱节点的waitStatus为-1），防止无限循环浪费资源。
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;//如果等待过程中被中断过，哪怕只有那么一次，就将interrupted标记为true
        }
    } finally {
        if (failed) //说明发生了意料之外的异常，将节点移除，避免影响到其他节点
            cancelAcquire(node);
    }
}
```

setHead方法是把当前节点置为虚节点，但并没有修改waitStatus，因为它是一直需要用的数据。

```java
// java.util.concurrent.locks.AbstractQueuedSynchronizer

private void setHead(Node node) {
    head = node;
    node.thread = null;
    node.prev = null;
}
```



acquireQueued函数的具体流程：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047762.gif)

从上图可以看出，跳出当前循环的条件是当“前置节点是头结点，且当前线程获取锁成功”。为了防止因死循环导致CPU资源被浪费，我们会判断前置节点的状态来决定是否要将当前线程挂起，shouldParkAfterFailedAcquire代码：

```java
// java.util.concurrent.locks.AbstractQueuedSynchronizer

// 靠前驱节点判断当前线程是否应该被阻塞
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        // 获取头结点的节点状态
        int ws = pred.waitStatus;
        // 说明头结点处于唤醒状态
        if (ws == Node.SIGNAL)
            return true; 
        // 通过枚举值我们知道waitStatus>0是取消状态
        if (ws > 0) {
            do {
                // 循环向前查找取消节点，把取消节点从队列中剔除
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            // 设置前任节点等待状态为SIGNAL
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
}
```

parkAndCheckInterrupt主要用于挂起当前线程，阻塞调用栈，返回当前线程的中断状态。

```java
// java.util.concurrent.locks.AbstractQueuedSynchronizer

private final boolean parkAndCheckInterrupt() {
    LockSupport.park(this);//调用park()使线程进入waiting状态
    return Thread.interrupted();//如果被唤醒，查看自己是不是被中断的。
}
```



具体挂起流程用流程图表示如下（shouldParkAfterFailedAcquire流程）：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047278.gif)

整个流程中，如果前驱结点的状态不是SIGNAL，那么自己就不能安心去休息，需要去找个安心的休息点，同时可以再尝试下看有没有机会轮到自己拿号。

park()会让当前线程进入waiting状态。在此状态下，有两种途径可以唤醒该线程：1）被unpark()；2）被interrupt()。需要注意的是，Thread.interrupted()会清除当前线程的中断标记位。

那么shouldParkAfterFailedAcquire中取消节点是怎么生成的呢？什么时候会把一个节点的waitStatus设置为-1？

是在什么时间释放节点通知到被挂起的线程呢？

#### CANCELLED状态节点生成

回看acquireQueued方法中的Finally代码：

```java
// java.util.concurrent.locks.AbstractQueuedSynchronizer

final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
        ...
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    ...
                    failed = false;
                    ...
                }
                ...
        } finally {
            if (failed)
                cancelAcquire(node);
            }
}
```

显然，当failed为true时才会执行方法cancelAcquire，那什么情况下failed为true呢？try代码段执行过程中出现异常。

> 这里不知道哪里会出现异常？假设tryAcquire出现的异常，那么acquire方法就已经不会往后执行，也就不会执行到acquireQueued

通过cancelAcquire方法，将Node的状态标记为CANCELLED。

```java
// java.util.concurrent.locks.AbstractQueuedSynchronizer

private void cancelAcquire(Node node) {
  // 将无效节点过滤
    if (node == null)
        return;
  // 设置该节点不关联任何线程，也就是虚节点
    node.thread = null;
    Node pred = node.prev;
  // 通过前驱节点，跳过取消状态的node
    while (pred.waitStatus > 0)
        node.prev = pred = pred.prev;
  // 获取过滤后的前驱节点的后继节点
    Node predNext = pred.next;
  // 把当前node的状态设置为CANCELLED
    node.waitStatus = Node.CANCELLED;
  // 如果当前节点是尾节点，将从后往前的第一个非取消状态的节点设置为尾节点
  // 更新失败的话，则进入else，如果更新成功，将tail的后继节点设置为null
    if (node == tail && compareAndSetTail(node, pred)) {
        compareAndSetNext(pred, predNext, null);
    } else {
        int ws;
    // 如果当前节点不是head的后继节点，1:判断当前节点前驱节点的是否为SIGNAL，2:如果不是，则把前驱节点设置为SINGAL看是否成功
    // 如果1和2中有一个为true，再判断当前节点的线程是否为null
    // 如果上述条件都满足，把当前节点的前驱节点的后继指针指向当前节点的后继节点
        if (pred != head && ((ws = pred.waitStatus) == Node.SIGNAL || (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) && pred.thread != null) {
            Node next = node.next;
            if (next != null && next.waitStatus <= 0)
                compareAndSetNext(pred, predNext, next);
        } else {
      // 如果当前节点是head的后继节点，或者上述条件不满足，那就唤醒当前节点的后继节点
            unparkSuccessor(node);
        }
        node.next = node; // help GC
    }
}
```

cancelAcquire方法的流程：

1. 获取当前节点的前驱节点，如果前驱节点的状态是CANCELLED，那就一直往前遍历，找到第一个waitStatus <= 0的节点，将找到的Pred节点和当前Node关联，将当前Node设置为CANCELLED。

2. 根据当前节点的位置，考虑以下三种情况：

   1. 当前节点是尾节点。

   2. 当前节点是Head的后继节点。

   3. 当前节点不是Head的后继节点，也不是尾节点。

当前节点是尾节点：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047311.gif)

当前节点是Head的后继节点：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047378.gif)

当前节点不是Head的后继节点，也不是尾节点：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047405.gif)

通过上面的流程，我们对于CANCELLED节点状态的产生和变化已经有了大致的了解，但是为什么所有的变化都是对Next指针进行了操作，而没有对Prev指针进行操作呢？什么情况下会对Prev指针进行操作？

执行cancelAcquire的时候，当前节点的前置节点可能已经从队列中出去了（已经执行过Try代码块中的shouldParkAfterFailedAcquire方法了），如果此时修改Prev指针，有可能会导致Prev指向另一个已经移除队列的Node，因此这块变化Prev指针不安全。

shouldParkAfterFailedAcquire方法中，会执行下面的代码，其实就是在处理Prev指针。shouldParkAfterFailedAcquire是获取锁失败的情况下才会执行，进入该方法后，说明共享资源已被获取，当前节点之前的节点都不会出现变化，因此这个时候变更Prev指针比较安全。

```java
do {
    node.prev = pred = pred.prev;
} while (pred.waitStatus > 0);
```



### release(int)

此方法是独占模式下线程释放共享资源的顶层入口。它会释放指定量的资源，如果彻底释放了（即state=0）,它会唤醒等待队列里的其他线程来获取资源。这也正是unlock()的语义，当然不仅仅只限于unlock()。

```java
public final boolean release(int arg) {
    if (tryRelease(arg)) {
        Node h = head;//找到头结点
        // 头结点不为空并且头结点的waitStatus不是初始化节点情况，解除线程挂起状态
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);//唤醒等待队列里的下一个线程
        return true;
    }
    return false;
}
```

根据tryRelease()的返回值来判断该线程是否已经完成释放掉资源了！所以自定义同步器在设计tryRelease()

这里的判断条件为什么是h != null && h.waitStatus != 0？

- h null Head还没初始化。初始情况下，head null，第一个节点入队，Head会被初始化一个虚拟节点。所以说，这里如果还没来得及入队，就会出现head == null 的情况。

- h != null && waitStatus == 0 表明后继节点对应的线程仍在运行中，不需要唤醒。

- h != null && waitStatus < 0 表明后继节点可能被阻塞了，需要唤醒。

#### tryRelease(int)

```java
protected boolean tryRelease(int arg) {
    throw new UnsupportedOperationException();
}
```



跟tryAcquire()一样，这个方法是需要独占模式的自定义同步器去实现的。正常来说，tryRelease()都会成功的，因为这是独占模式，该线程来释放资源，那么它肯定已经拿到独占资源了，直接减掉相应量的资源即可(state-=arg)，也不需要考虑线程安全的问题。但要注意它的返回值，上面已经提到了，release()是根据tryRelease()的返回值来判断该线程是否已经完成释放掉资源了！所以自义定同步器在实现时，如果已经彻底释放资源(state=0)，要返回true，否则返回false。

```java
// java.util.concurrent.locks.ReentrantLock.Sync#tryRelease

@ReservedStackAccess
protected final boolean tryRelease(int releases) {
    int c = getState() - releases;//在未重入的情况下，getState() = 1，减去releases 1，因此c 为 0
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    boolean free = false;
    if (c == 0) {
        free = true;
        setExclusiveOwnerThread(null);//独占锁线程设置为null
    }
    setState(c);//恢复默认
    return free;
}
```



#### unparkSuccessor(Node)

此方法用于唤醒等待队列中下一个线程。

```java
private void unparkSuccessor(Node node) {
    //这里，node一般为当前线程所在的结点。
    int ws = node.waitStatus;
    if (ws < 0)//置零当前线程所在的结点状态，允许失败。
        compareAndSetWaitStatus(node, ws, 0);

    Node s = node.next;//找到下一个需要唤醒的结点s
    if (s == null || s.waitStatus > 0) {//如果为空或已取消
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev) // 从后向前找。
            if (t.waitStatus <= 0)//从这里可以看出，<=0的结点，都是还有效的结点。
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);//唤醒
}
```

这个函数并不复杂。一句话概括：用unpark()唤醒等待队列中最前边的那个未放弃线程s。此时，再和acquireQueued()联系起来，s被唤醒后，进入if (p == head && tryAcquire(arg))的判断（即使p!=head也没关系，它会再进入shouldParkAfterFailedAcquire()寻找一个安全点。这里既然s已经是等待队列中最前边的那个未放弃线程了，那么通过shouldParkAfterFailedAcquire()的调整，s也必然会跑到head的next结点，下一次自旋p==head就成立了），然后s把自己设置成head标杆结点，表示自己已经获取到资源了，acquire()也返回了！

在队列中查找时是从后向前找的，为什么这么做？

从源码上看，先找到后继结点s，如果s状态正常那么直接唤醒。但有两种异常情况，会导致next链不一致：

1. s==null，在新结点入队时可能会出现

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047450.gif)

2. s.waitStatus > 0，中间有节点取消时会出现（如超时）

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047479.gif)

关于并发问题，addWaiter()入队操作和cancelAcquire()取消排队操作都会造成next链的不一致，而prev链是强一致的，所以这时从后往前找是最安全的。

> 为什么prev链是强一致的？
>
> 因为addWaiter()里每次compareAndSetTail(pred, node)之前都有node.prev = pred，即使compareAndSetTail失败，enq()会反复尝试，直到成功。一旦compareAndSetTail成功，该node.prev就成功挂在之前的tail结点上了，而且是唯一的，这时其他新结点的prev只能尝试往新tail结点上挂。这里的组合用法非常巧妙，能保证CAS之前的prev链强一致，但不能保证CAS后的next链强一致。

### acquireShared(int)

此方法是共享模式下线程获取共享资源的顶层入口。它会获取指定量的资源，获取成功则直接返回，获取失败则进入等待队列，直到获取到资源为止，整个过程忽略中断。

```java
public final void acquireShared(int arg) {
     if (tryAcquireShared(arg) < 0)
        doAcquireShared(arg);
}
```

这里tryAcquireShared()依然需要自定义同步器去实现。但是AQS已经把其返回值的语义定义好了：负值代表获取失败；0代表获取成功，但没有剩余资源；正数表示获取成功，还有剩余资源，其他线程还可以去获取。所以这里acquireShared()的流程就是：

1. tryAcquireShared()尝试获取资源，成功则直接返回；
2. 失败则通过doAcquireShared()进入等待队列，直到获取到资源为止才返回。

#### doAcquireShared(int)

此方法用于将当前线程加入等待队列尾部休息，直到其他线程释放资源唤醒自己，自己成功拿到相应量的资源后才返回。

```java
private void doAcquireShared(int arg) {
    final Node node = addWaiter(Node.SHARED);//加入队列尾部
    boolean failed = true;//是否成功标志
    try {
        boolean interrupted = false;//等待过程中是否被中断过的标志
        for (;;) {
            final Node p = node.predecessor();//前驱
            if (p == head) {//如果到head的下一个，因为head是拿到资源的线程，此时node被唤醒，很可能是head用完资源来唤醒自己的
                int r = tryAcquireShared(arg);//尝试获取资源
                if (r >= 0) {//成功
                    setHeadAndPropagate(node, r);//将head指向自己，还有剩余资源可以再唤醒之后的线程
                    p.next = null; // help GC
                    if (interrupted)//如果等待过程中被打断过，此时将中断补上。
                        selfInterrupt();
                    failed = false;
                    return;
                }
            }

            //判断状态，寻找安全点，进入waiting状态，等着被unpark()或interrupt()
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```

这里跟acquireQueued()的流程并没有太大区别。只不过这里将补中断的selfInterrupt()放到doAcquireShared()里了，而独占模式是放到acquireQueued()之外，但实际上都一样。

跟独占模式比，还有一点需要注意的是，这里只有线程是head.next时（“老二”），才会去尝试获取资源，有剩余的话还会唤醒之后的队友。

那么问题就来了，假如老大用完后释放了5个资源，而老二需要6个，老三需要1个，老四需要2个。老大先唤醒老二，老二一看资源不够，他是把资源让给老三呢，还是不让？答案是否定的！老二会继续park()等待其他线程释放资源，也更不会去唤醒老三和老四了。独占模式，同一时刻只有一个线程去执行，这样做未尝不可；但共享模式下，多个线程是可以同时执行的，现在因为老二的资源需求量大，而把后面量小的老三和老四也都卡住了。当然，这并不是问题，只是AQS保证严格按照入队顺序唤醒罢了（保证公平，但降低了并发）。

setHeadAndPropagate(Node, int):此方法在setHead()的基础上多了一步，就是自己苏醒的同时，如果条件符合（比如还有剩余资源），还会去唤醒后继结点，毕竟是共享模式！

private void setHeadAndPropagate(Node node, int propagate) {

```java
private void setHeadAndPropagate(Node node, int propagate) {
    Node h = head;
    setHead(node);//head指向自己
     //如果还有剩余量，继续唤醒下一个邻居线程
    if (propagate > 0 || h == null || h.waitStatus < 0) {
        Node s = node.next;
        if (s == null || s.isShared())
            doReleaseShared();
    }
}
```



### releaseShared()

此方法是共享模式下线程释放共享资源的顶层入口。它会释放指定量的资源，如果成功释放且允许唤醒等待线程，它会唤醒等待队列里的其他线程来获取资源。

```java
public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {//尝试释放资源
        doReleaseShared();//唤醒后继结点
        return true;
    }
    return false;
}
```



此方法的流程也比较简单，一句话：释放掉资源后，唤醒后继。跟独占模式下的release()相似，但有一点稍微需要注意：独占模式下的tryRelease()在完全释放掉资源（state=0）后，才会返回true去唤醒其他线程，这主要是基于独占下可重入的考量；而共享模式下的releaseShared()则没有这种要求，共享模式实质就是控制一定量的线程并发执行，那么拥有资源的线程在释放掉部分资源时就可以唤醒后继等待结点。例如，资源总量是13，A（5）和B（7）分别获取到资源并发运行，C（4）来时只剩1个资源就需要等待。A在运行过程中释放掉2个资源量，然后tryReleaseShared(2)返回true唤醒C，C一看只有3个仍不够继续等待；随后B又释放2个，tryReleaseShared(2)返回true唤醒C，C一看有5个够自己用了，然后C就可以跟A和B一起运行。而ReentrantReadWriteLock读锁的tryReleaseShared()只有在完全释放掉资源（state=0）才返回true，所以自定义同步器可以根据需要决定tryReleaseShared()的返回值

#### doReleaseShared()

此方法主要用于唤醒后继

```java
private void doReleaseShared() {
    for (;;) {
        Node h = head;
        if (h != null && h != tail) {
            int ws = h.waitStatus;
            if (ws == Node.SIGNAL) {
                if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                    continue;
                unparkSuccessor(h);//唤醒后继
            }
            else if (ws == 0 &&
                     !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                continue;
        }
        if (h == head)// head发生变化
            break;
    }
}
```



## 应用

Mutex是一个不可重入的互斥锁实现。锁资源（AQS里的state）只有两种状态：0表示未锁定，1表示锁定。核心源码：

```java
class Mutex implements Lock, java.io.Serializable {
    // 自定义同步器
    private static class Sync extends AbstractQueuedSynchronizer {
        // 判断是否锁定状态
        protected boolean isHeldExclusively() {
            return getState() == 1;
        }

        // 尝试获取资源，立即返回。成功则返回true，否则false。
        public boolean tryAcquire(int acquires) {
            assert acquires == 1; // 这里限定只能为1个量
            if (compareAndSetState(0, 1)) {//state为0才设置为1，不可重入！
                setExclusiveOwnerThread(Thread.currentThread());//设置为当前线程独占资源
                return true;
            }
            return false;
        }

        // 尝试释放资源，立即返回。成功则为true，否则false。
        protected boolean tryRelease(int releases) {
            assert releases == 1; // 限定为1个量
            if (getState() == 0)//既然来释放，那肯定就是已占有状态了。只是为了保险，多层判断！
                throw new IllegalMonitorStateException();
            setExclusiveOwnerThread(null);
            setState(0);//释放资源，放弃占有状态
            return true;
        }
    }

    // 真正同步类的实现都依赖继承于AQS的自定义同步器！
    private final Sync sync = new Sync();

    //lock<-->acquire。两者语义一样：获取资源，即便等待，直到成功才返回。
    public void lock() {
        sync.acquire(1);
    }

    //tryLock<-->tryAcquire。两者语义一样：尝试获取资源，要求立即返回。成功则为true，失败则为false。
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    //unlock<-->release。两者语文一样：释放资源。
    public void unlock() {
        sync.release(1);
    }

    //锁是否占有状态
    public boolean isLocked() {
        return sync.isHeldExclusively();
    }
}
```

除了Mutex，ReentrantLock/CountDownLatch/Semphore这些同步类的实现方式都差不多，不同的地方就在获取-释放资源的方式tryAcquire-tryRelelase。

 
## ReentrantLock 的使用

ReentrantLock 的使用方式与 [synchronized](https://www.seven97.top/java/concurrent/02-keyword1-synchronized.html) 关键字类似，都是通过加锁和释放锁来实现同步的。我们来看看 ReentrantLock 的使用方式，以非公平锁为例：

```java
public class ReentrantLockTest {
    private static final ReentrantLock lock = new ReentrantLock();
    private static int count = 0;

    public static void main(String[] args) throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                lock.lock();
                try {
                    count++;
                } finally {
                    lock.unlock();
                }
            }
        });
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                lock.lock();
                try {
                    count++;
                } finally {
                    lock.unlock();
                }
            }
        });
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        System.out.println(count);
    }
}
```
 

 代码很简单，两个线程分别对 count 变量进行 10000 次累加操作，最后输出 count 的值。我们来看看运行结果：

```
20000
```

可以看到，两个线程对 count 变量进行了 20000 次累加操作，说明 ReentrantLock 是支持重入性的。再来看看公平锁的使用方式，只需要将 ReentrantLock 的构造方法改为公平锁即可：

```java
private static final ReentrantLock lock = new ReentrantLock(true);
```

运行结果为：

```
20000
```

可以看到，公平锁的运行结果与非公平锁的运行结果一致，这是因为公平锁的实现方式与非公平锁的实现方式基本一致，只是在获取锁时增加了判断当前节点是否有前驱节点的逻辑判断。

- 公平锁: 按照线程请求锁的顺序获取锁，即先到先得。
- 非公平锁: 线程获取锁的顺序可能与请求锁的顺序不同，可能导致某些线程获取锁的速度较快。

需要注意的是，使用 ReentrantLock 时，锁必须在 try 代码块开始之前获取，并且加锁之前不能有异常抛出，否则在 finally 块中就无法释放锁（ReentrantLock 的锁必须在 finally 中手动释放）。

错误示例：

```java
Lock lock = new XxxLock();
// ...
try {
    // 如果在此抛出异常，会直接执行 finally 块的代码
    doSomething();
    // 不管锁是否成功，finally 块都会执行
    lock.lock();
    doOthers();

} finally {
    lock.unlock();
}
```

正确示例：

```java
Lock lock = new XxxLock();
// ...
lock.lock();
try {
    doSomething();
    doOthers();
} finally {
    lock.unlock();
}
```

 

 <!-- @include: @article-footer.snippet.md -->     

 

 

 

 

