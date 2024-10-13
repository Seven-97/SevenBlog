---
title: 并发工具 - Condition详解
category: Java
tag:
 - 并发编程
 - JUC
head:
  - - meta
    - name: keywords
      content: Java,并发编程,多线程,Thread,并发工具,Condition,实现原理,底层源码
  - - meta
    - name: description
      content: 全网最全的Java并发编程知识点总结，让天下没有难学的八股文！
---


## 概述

Condition 是一个多线程协调通信的工具类，可以让某些线程一起等待某个条件(condition)，只有满足条件时，线程才会被唤醒。

- 在使用Lock之前，使用的最多的同步方式应该是synchronized关键字来实现同步方式了。配合Object的wait()、notify()系列方法可以实现等待/通知模式。

- Condition接口也提供了类似Object的监视器方法，与Lock配合可以实现等待/通知模式，但是这两者在使用方式以及功能特性上还是有差别的。

Object和Condition接口的一些对比。

| 对比项                                               | Object 监视器方法         | Condition                                                    |
| ---------------------------------------------------- | ------------------------- | ------------------------------------------------------------ |
| 前置条件                                             | 获取对象的监视器锁        | 调用  Lock.lock() 获取锁调用 Lock.newCondition() 获取 Condition 对象 |
| 调用方法                                             | 直接调用如：object.wait() | 直接调用如：condition.await()                                |
| 等待队列个数                                         | 一个                      | 多个                                                         |
| 当前线程释放锁并进入等待队列                         | 支持                      | 支持                                                         |
| 当前线程释放锁并进入等待队列，在等待状态中不响应中断 | 不支持                    | 支持                                                         |
| 当前线程释放锁并进入超时等待状态                     | 支持                      | 支持                                                         |
| 当前线程释放锁并进入等待状态到将来的某个时间         | 不支持                    | 支持                                                         |
| 唤醒等待队列中的一个线程                             | 支持                      | 支持                                                         |
| 唤醒等待队列中的全部线程                             | 支持                      | 支持                                                         |

### 接口的介绍与示例

首先需要明白condition对象是依赖于lock对象的，意思就是说condition对象需要通过lock对象进行创建出来(调用Lock对象的newCondition()方法)。condition的使用方式非常的简单。但是需要注意在调用方法前获取锁。

```java
/**
 * condition使用示例：
 * 1、condition的使用必须要配合锁使用，调用方法时必须要获取锁
 * 2、condition的创建依赖于Lock lock.newCondition()；
 */
public class ConditionUseCase {
    /**
     * 创建锁
     */
    public Lock readLock = new ReentrantLock();
    /**
     * 创建条件
     */
    public Condition condition = readLock.newCondition();

    public static void main(String[] args) {
        ConditionUseCase useCase = new ConditionUseCase();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(() -> {
            //获取锁进行等待
            useCase.conditionWait();
        });
        executorService.execute(() -> {
            //获取锁进行唤起读锁
            useCase.conditionSignal();
        });
    }

    /**
     * 等待线程
     */
    public void conditionWait() {
        readLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "拿到锁了");
            System.out.println(Thread.currentThread().getName() + "等待信号");
            condition.await();
            System.out.println(Thread.currentThread().getName() + "拿到信号");
        } catch (Exception e) {

        } finally {
            readLock.unlock();
        }
    }

    /**
     * 唤起线程
     */
    public void conditionSignal() {
        readLock.lock();
        try {
            //睡眠5s 线程1启动
            Thread.sleep(5000);
            System.out.println(Thread.currentThread().getName() + "拿到锁了");
            condition.signal();
            System.out.println(Thread.currentThread().getName() + "发出信号");
        } catch (Exception e) {

        } finally {
            //释放锁
            readLock.unlock();
        }
    }

}

//执行结果
1 pool-1-thread-1拿到锁了
 2 pool-1-thread-1等待信号 ---释放锁-线程等待 t1
 3 pool-1-thread-2拿到锁了
 4 pool-1-thread-2发出信号 --- 唤起线程t2释放锁
 5 pool-1-thread-1拿到信号---t1继续执行
```

如示例所示，一般都会将Condition对象作为成员变量。当调用await()方法后，当前线程会释放锁并在此等待，而其他线程调用Condition对象的signal()方法，通知当前线程后，当前线程才从await()方法返回，并且在返回前已经获取了锁。

### 接口常用方法

condition可以通俗的理解为条件队列。当一个线程在调用了await方法以后，直到线程等待的某个条件为真的时候才会被唤醒。这种方式为线程提供了更加简单的等待/通知模式。Condition必须要配合锁一起使用，因为对共享状态变量的访问发生在多线程环境下。一个Condition的实例必须与一个Lock绑定，因此Condition一般都是作为Lock的内部实现。

1. **await()** ：造成当前线程在接到信号或被中断之前一直处于等待状态。
2. **boolean await(long time, TimeUnit unit)** ：造成当前线程在接到信号、被中断或到达指定等待时间之前一直处于等待状态---》是否超时，超时异常
3. **awaitNanos(long nanosTimeout)** ：造成当前线程在接到信号、被中断或到达指定等待时间之前一直处于等待状态。返回值表示剩余时间，如果在nanosTimesout之前唤醒，那么返回值 = nanosTimeout - 消耗时间，如果返回值 <= 0 ,则可以认定它已经超时了。
4. **awaitUninterruptibly()** ：造成当前线程在接到信号之前一直处于等待状态。【注意：该方法对中断不敏感】。
5. **awaitUntil(Date deadline)** ：造成当前线程在接到信号、被中断或到达指定最后期限之前一直处于等待状态。如果没有到指定时间就被通知，则返回true，否则表示到了指定时间，返回返回false。
6. **signal()** ：唤醒一个等待线程。该线程从等待方法返回前必须获得与Condition相关的锁。
7. **signalAll()** ：唤醒所有等待线程。能够从等待方法返回的线程必须获得与Condition相关的锁。

这里顺便回顾一下 Object 类的主要方法：
1.  `wait()`：线程等待直到被通知或者中断。
2. `wait(long timeout)`：线程等待指定的时间，或被通知，或被中断。
3. `wait(long timeout, int nanos)`：线程等待指定的时间，或被通知，或被中断。
4. `notify()`：唤醒一个等待的线程。
5. `notifyAll()`：唤醒所有等待的线程。

## 原理解析

Condition是[AQS](https://www.seven97.top/java/concurrent/03-juclock2-aqs.html)的内部类。可以通过 Lock.newCondition() 方法获取 Condition 对象，而 Lock 对于同步状态的实现都是通过内部的自定义同步器实现的，newCondition() 方法也不例外，所以，Condition 接口的唯一实现类是同步器 AQS 的内部类 ConditionObject，因为 Condition 的操作需要获取相关的锁，所以作为同步器的内部类也比较合理，该类定义如下：

```java
public class ConditionObject implements Condition, java.io.Serializable
```



### 等待队列

前面我们学过，[AQS](https://www.seven97.top/java/concurrent/03-juclock2-aqs.html) 内部维护了一个先进先出（FIFO）的双端队列，并使用了两个引用 head 和 tail 用于标识队列的头部和尾部。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251047000.gif)

Condition 内部也使用了同样的方式，内部维护了一个先进先出（FIFO）的单向队列，我们把它称为等待队列。该队列是 Condition 对象实现等待 / 通知功能的关键。等待队列是一个FIFO的队列，在队列中的每个节点都包含了一个线程引用，该线程就是在Condition对象上等待的线程，如果一个线程调用了Condition.await()方法，那么该线程将会释放锁、构造成节点加入等待队列并进入等待状态。

事实上，节点的定义复用了 AQS 中 Node 节点的定义，也就是说，同步队列和等待队列中节点类型都是 AQS 的静态内部类 AbstractQueuedSynchronized.Node。一个 Condition 包含一个等待队列，Condition 拥有首节点（firstWaiter）和尾节点（lastWaiter）。当前线程调用 Condition.await() 方法之后，将会以当前线程构造节点，并将节点从尾部加入等待队列，等待队列的基本结构如下所示。

![](https://cdn.tobebetterjavaer.com/stutymore/condition-20230901101925.png)

- 等待队列分为首节点和尾节点。当一个线程调用Condition.await()方法，将会以当前线程构造节点，并将节点从尾部加入等待队列。
- 新增节点就是将尾部节点指向新增的节点。节点引用更新本来就是在获取锁以后的操作，所以不需要CAS保证。同时也是线程安全的操作。

```java
public class ConditionObject implements Condition, java.io.Serializable {
    private static final long serialVersionUID = 1173984872572414699L;
    /** First node of condition queue. */
    private transient Node firstWaiter;
    /** Last node of condition queue. */
    private transient Node lastWaiter;
}
```

在 Object 的监视器模型上，一个对象拥有一个同步队列和等待队列，而并发包中的 Lock（更确切地说是同步器）拥有一个同步队列和多个等待队列

### 等待 await 方法

- 当线程调用了await方法以后。线程就作为队列中的一个节点被加入到等待队列中去了。同时会释放锁的拥有。

- 当从await方法返回的时候。一定会获取condition相关联的锁。当等待队列中的节点被唤醒的时候，则唤醒节点的线程开始尝试获取同步状态。

  - 如果不是通过 其他线程调用Condition.signal()方法唤醒，而是对等待线程进行中断，则会抛出InterruptedException异常信息。

  - 通知调用Condition的signal()方法，将会唤醒在等待队列中等待最长时间的节点（条件队列里的首节点），在唤醒节点前，会将节点移到同步队列中。

 

当前线程加入到等待队列中如图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251604016.png)

 

源码如下：

```java
public final void await() throws InterruptedException {
    // 检测线程中断状态
    if (Thread.interrupted())
        throw new InterruptedException();
    // 将当前线程包装为Node节点加入等待队列
    Node node = addConditionWaiter();
    // 释放同步状态，也就是释放锁
    int savedState = fullyRelease(node);
    int interruptMode = 0;
    // 检测该节点是否在同步队中，如果不在，则说明该线程还不具备竞争锁的资格，则继续等待
    while (!isOnSyncQueue(node)) {
        // 挂起线程
        LockSupport.park(this);
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }
    // 竞争同步状态
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    // 清理条件队列中的不是在等待条件的节点
    if (node.nextWaiter != null) // clean up if cancelled
        unlinkCancelledWaiters();
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
}
```

调用该方法的线程是成功获取了锁的线程，也就是同步队列中的首节点，该方法会将当前线程构造节点并加入等待队列中，然后释放同步状态，唤醒同步队列中的后继节点，然后当前线程会进入等待状态。

 可能会有这样几个问题：

1. 怎样将当前线程添加到等待队列中？
2. 释放锁的过程是？
3. 怎样才能从 await 方法中退出？

#### 问题1：怎样将当前线程添加到等待队列中？

调用 addConditionWaiter 方法会将当前线程添加到等待队列中，源码如下：

```java
private Node addConditionWaiter() {
    // 尾节点
    Node t = lastWaiter;
    // 尾节点如果不是CONDITION状态，则表示该节点不处于等待状态，需要清理节点
    if (t != null && t.waitStatus != Node.CONDITION) {
        unlinkCancelledWaiters();
        t = lastWaiter;
    }
    // 根据当前线程创建Node节点
    Node node = new Node(Thread.currentThread(), Node.CONDITION);
    // 将该节点加入等待队列的末尾
    if (t == null)
        firstWaiter = node;
    else
        t.nextWaiter = node;
    lastWaiter = node;
    return node;
}
```

首先将 t 指向尾节点，如果尾节点不为空并且它的`waitStatus!=-2`（-2 为 CONDITION，表示正在等待 Condition 条件），则将不处于等待状态的节点从等待队列中移除，并且将 t 指向新的尾节点。

然后将当前线程封装成 waitStatus 为-2 的节点追加到等待队列尾部。

如果尾节点为空，则表明队列为空，将首尾节点都指向当前节点。

![img](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202410131705987.png)

如果尾节点不为空，表明队列中有其他节点，则将当前尾节点的 nextWaiter 指向当前节点，将当前节点置为尾节点。

![img](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202410131705994.png)

简单总结一下，这段代码的作用就是**通过尾插入的方式将当前线程封装的 Node 插入到等待队列中**，同时可以看出，Condtion 的等待队列是一个**不带头节点的链式队列**，之前我们学习 [AQS](https://www.seven97.top/java/concurrent/03-juclock2-aqs.html) 时知道同步队列**是一个带头节点的链式队列**，这是两者的一个区别。

关于头节点的作用，我们这里简单说明一下。

不带头节点是指在链表数据结构中，链表的第一个节点就是实际存储的第一个数据元素，而不是一个特定的"头"节点，该节点不包含实际的数据。

1）不带头节点的链表：

- 链表的第一个节点就是第一个实际的数据节点。
- 当链表为空时，头引用（通常称为 head）指向 null。

2）带头节点的链表：

- 链表有一个特殊的节点作为链表的开头，这个特殊的节点称为头节点。
- 头节点通常不存储任何实际数据，或者它的数据字段不被使用。
- 无论链表是否为空，头节点总是存在的。当链表为空时，头节点的下一个节点指向 null。
- 使用头节点可以简化某些链表操作，因为你不必特殊处理第一个元素的插入和删除。

为了更好地解释这两种链表结构，我将为每种结构提供一个简单的整数链表插入方法的示例。

1）不带头节点的链表

```java
public class Node {
    public int data;
    public Node next;

    public Node(int data) {
        this.data = data;
        this.next = null;
    }
}

public class LinkedListWithoutHead {
    public Node head;

    public void insert(int value) {
        Node newNode = new Node(value);
        if (head == null) {
            head = newNode;
        } else {
            Node temp = head;
            while (temp.next != null) {
                temp = temp.next;
            }
            temp.next = newNode;
        }
    }
}
```

2）带头节点的链表

```java
public class NodeWithHead {
    public int data;
    public NodeWithHead next;

    public NodeWithHead(int data) {
        this.data = data;
        this.next = null;
    }
}

public class LinkedListWithHead {
    private NodeWithHead head;

    public LinkedListWithHead() {
        head = new NodeWithHead(-1);  // 初始化头节点
    }

    public void insert(int value) {
        NodeWithHead newNode = new NodeWithHead(value);
        NodeWithHead temp = head;
        while (temp.next != null) {
            temp = temp.next;
        }
        temp.next = newNode;
    }
}
```

这下是不是就彻底明白了？说明白了头节点，我们再回到 Condition 的 await 方法。

#### 问题 2：释放锁的过程是？

将当前线程加入到等待队列之后，需要释放同步状态，该操作通过 fullyRelease(Node) 方法来完成：

```java
final int fullyRelease(Node node) {
    boolean failed = true;
    try {
        // 获取同步状态
        int savedState = getState();
        // 释放锁
        if (release(savedState)) {
            failed = false;
            return savedState;
        } else {
            throw new IllegalMonitorStateException();
        }
    } finally {
        if (failed)
            node.waitStatus = Node.CANCELLED;
    }
}
```

这段代码也很容易理解，**调用 AQS 的模板方法 release 释放 AQS 的同步状态并且唤醒在同步队列中头节点的后继节点引用的线程**，如果释放成功则正常返回，若失败的话就抛出异常。

#### 问题3：怎样才能从 await 方法中退出？

怎样从 await 方法退出呢？现在回过头再来看 await 方法，其中有这样一段逻辑：

```java
while (!isOnSyncQueue(node)) {
	// 3. 当前线程进入到等待状态
    LockSupport.park(this);
    if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
        break;
}
```

isOnSyncQueue 方法用于判断当前线程所在的 Node 是否在同步队列中：

```java
final boolean isOnSyncQueue(Node node) {
    // 节点状态为CONDITION，或者前驱节点为null，返回false
    if (node.waitStatus == Node.CONDITION || node.prev == null)
        return false;
    // 后继节点不为null，那么肯定在同步队列中
    if (node.next != null) // If has successor, it must be on queue
        return true;
    
    return findNodeFromTail(node);
}
```

如果当前节点的 waitStatus=-2，说明它在等待队列中，返回 false；如果当前节点有前驱节点，则证明它在 AQS 队列中，但是前驱节点为空，说明它是头节点，而头节点是不参与锁竞争的，也返回 false。

如果当前节点既不在等待队列中，又不是 AQS 中的头节点且存在 next 节点，说明它存在于 AQS 中，直接返回 true。

这里有必要给大家看一下同步队列与等待队列的关系图了。

![img](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202410131712719.png)

当线程第一次调用 condition.await 方法时，会进入到这个 while 循环，然后通过 `LockSupport.park(this)` 使当前线程进入等待状态，那么要想退出 await，第一个前提条件就是要先退出这个 while 循环，出口就只两个地方：

1. 走到 break 退出 while 循环；
2. while 循环中的逻辑判断为 false。

出现第 1 种情况的条件是，当前等待的线程被中断后代码会走到 break 退出，第 2 种情况是当前节点被移动到了同步队列中（即另外一个线程调用了 condition 的 signal 或者 signalAll 方法），while 中逻辑判断为 false 后结束 while 循环。

总结一下，退出 await 方法的前提条件是**当前线程被中断或者调用 condition.signal 或者 condition.signalAll 使当前节点移动到同步队列后**。

当退出 while 循环后会调用`acquireQueued(node, savedState)`，该方法的作用是在**自旋过程中线程不断尝试获取同步状态，直到成功（线程获取到 lock）**。这样也说明了**退出 await 方法必须是已经获得了 condition 引用（关联）的 lock**。

到目前为止，上文提到的三个问题，我们都通过阅读源码的方式找到了答案，也加深了对 await 方法的理解。await 方法示意图如下：

![await方法示意图](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202410131712729.png)

如图，调用 condition.await 方法的线程必须是已经获得了 lock 的线程，也就是当前线程是同步队列中的头节点。调用该方法后会使得当前线程所封装的 Node 尾插入到等待队列中。

> 超时机制的支持

condition 还额外支持超时机制，使用者可调用 awaitNanos、awaitUtil 这两个方法，实现原理基本上与 AQS 中的 tryAcquire 方法如出一辙。

> 不响应中断的支持

要想不响应中断可以调用 `condition.awaitUninterruptibly()` 方法，该方法的源码如下：

```java
public final void awaitUninterruptibly() {
    Node node = addConditionWaiter();
    int savedState = fullyRelease(node);
    boolean interrupted = false;
    while (!isOnSyncQueue(node)) {
        LockSupport.park(this);
        if (Thread.interrupted())
            interrupted = true;
    }
    if (acquireQueued(node, savedState) || interrupted)
        selfInterrupt();
}
```

这段方法与上面的 await 方法基本一致，只不过减少了对中断的处理。



### 通知-signal/signalAll 实现原理

**调用 condition 的 signal 或者 signalAll 方法可以将等待队列中等待时间最长的节点移动到同步队列中**，使得该节点能够有机会获得 lock。等待队列是先进先出（FIFO）的，所以等待队列的头节点必然会是等待时间最长的节点，也就是每次调用 condition 的 signal 方法都会将头节点移动到同步队列中。

在调用signal()方法之前必须先判断是否获取到了锁。接着获取等待队列的首节点，将其移动到同步队列并且利用LockSupport唤醒节点中的线程。节点从等待队列移动到同步队列如下图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251605505.png)

被唤醒的线程将从await方法中的while循环中退出。随后加入到同步状态的竞争当中去。成功获取到竞争的线程则会返回到await方法之前的状态。

 

源码如下：

调用 Condition 的 signal() 方法，将会唤醒在等待队列中等待时间最长的节点（首节点），在唤醒节点之前，会将节点移到同步队列中。Condition 的 signal() 方法如下所示：

```java
public final void signal() {
    // 判断是否是当前线程获取了锁
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    // 唤醒等待队列的首节点
    Node first = firstWaiter;
    if (first != null)
        doSignal(first);
}
```


该方法最终调用 doSignal(Node) 方法来唤醒节点：

```java
private void doSignal(Node first) {
    do {
        // 把等待队列的首节点移除之后，要修改首结点
        if ( (firstWaiter = first.nextWaiter) == null)
            lastWaiter = null;
        first.nextWaiter = null;
    } while (!transferForSignal(first) &&
                (first = firstWaiter) != null);
}
```



真正对头节点做处理的逻辑，将节点移动到同步队列是通过 transferForSignal(Node) 方法完成的：

```java
final boolean transferForSignal(Node node) {
    // 尝试将该节点的状态从CONDITION修改为0
    if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
        return false;
    
    // 将节点加入到同步队列尾部，返回该节点的前驱节点
    Node p = enq(node);
    int ws = p.waitStatus;
    // 如果前驱节点的状态为CANCEL或者修改waitStatus失败，则直接唤醒当前线程
    if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
        LockSupport.unpark(node.thread);
    return true;
}
```

这段代码主要做了两件事情：

1. 将头节点的状态更改为 CONDITION；
2. 调用 enq 方法，将该节点尾插入到同步队列中，关于 enq 方法请看 [AQS 的底层实现这篇文章](https://www.seven97.top/java/concurrent/03-juclock2-aqs.html)。


节点从等待队列移动到同步队列的过程如下图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251602461.gif)

被唤醒后的线程，将从 await() 方法中的 while 循环中退出（因为此时 isOnSyncQueue(Node) 方法返回 true），进而调用 acquireQueued() 方法加入到获取同步状态的竞争中。

成功获取了锁之后，被唤醒的线程将从先前调用的 await() 方法返回，此时，该线程已经成功获取了锁。

> signalAll()

sigllAll 与 sigal 方法的区别体现在 doSignalAll 方法上，前面我们已经知道 **doSignal 方法只会对等待队列的头节点进行操作**， signalAll() 方法相当于对等待队列的每个节点均执行一次 signal() 方法，效果就是将等待队列中的所有节点移动到同步队列中。doSignalAll 的源码如下：

```java
private void doSignalAll(Node first) {
    lastWaiter = firstWaiter = null;
    do {
        Node next = first.nextWaiter;
        first.nextWaiter = null;
        transferForSignal(first);
        first = next;
    } while (first != null);
}
```

该方法会将等待队列中的每一个节点都移入到同步队列中，即“通知”当前调用 `condition.await()` 方法的每一个线程。

 

### await 与 signal/signalAll

文章开篇提到的等待/通知机制，通过 condition 的 await 和 signal/signalAll 方法就可以实现，而这种机制能够解决最经典的问题就是“[生产者与消费者问题](https://www.seven97.top/java/concurrent/producer-consumer-model)”

await、signal 和 signalAll 方法就像一个开关，控制着线程 A（等待方）和线程 B（通知方）。它们之间的关系可以用下面这幅图来说明，会更贴切：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202410131719513.png)

线程 awaitThread 先通过 `lock.lock()` 方法获取锁，成功后调用 condition.await 方法进入等待队列，而另一个线程 signalThread 通过 `lock.lock()` 方法获取锁成功后调用了 condition.signal 或者 signalAll 方法，使得线程 awaitThread 能够有机会移入到同步队列中，当其他线程释放 lock 后使得线程 awaitThread 能够有机会获取 lock，从而使得线程 awaitThread 能够从 await 方法中退出并执行后续操作。如果 awaitThread 获取 lock 失败会直接进入到同步队列。





## 总结

- 调用await方法后，将当前线程加入Condition等待队列中。当前线程释放锁。否则别的线程就无法拿到锁而发生死锁。自旋(while)挂起，不断检测节点是否在同步队列中了，如果是则尝试获取锁，否则挂起。

- 当线程被signal方法唤醒，被唤醒的线程将从await()方法中的while循环中退出来，然后调用acquireQueued()方法竞争同步状态。

 

 

 

 

 <!-- @include: @article-footer.snippet.md -->     

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 

 