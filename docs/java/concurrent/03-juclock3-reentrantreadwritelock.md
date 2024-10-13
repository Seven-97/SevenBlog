---
title: 并发读写锁-ReentrantReadWriteLock详解
category: Java
tags:
  - 并发编程
  - JUC
head:
  - - meta
    - name: keywords
      content: Java,并发编程,多线程,Thread,读写锁,ReentrantReadWriteLock,StampedLock,实现原理,底层源码
  - - meta
    - name: description
      content: 全网最全的Java并发编程知识点总结，让天下没有难学的八股文！
---

## ReentrantReadWriteLock使用场景

ReentrantReadWriteLock 是 Java 的一种读写锁，它允许多个读线程同时访问，但只允许一个写线程访问（会阻塞所有的读写线程）。这种锁的设计可以提高性能，特别是在读操作的数量远远超过写操作的情况下。

在并发场景中，为了解决线程安全问题，我们通常会使用关键字 [synchronized](https://www.seven97.top/java/concurrent/02-keyword1-synchronized.html) 或者 JUC 包中实现了 Lock 接口的 [ReentrantLock](https://www.seven97.top/java/concurrent/03-juclock2-aqs.html)。但它们都是独占式获取锁，也就是在同一时刻只有一个线程能够获取锁。

而在一些业务场景中，大部分只是读数据，写数据很少，如果仅仅是读数据的话并不会影响数据正确性，而如果在这种业务场景下，依然使用独占锁的话，很显然会出现性能瓶颈。针对这种读多写少的情况，Java 提供了另外一个实现 Lock 接口的 ReentrantReadWriteLock——读写锁。

`ReentrantReadWriteLock`其实就是 **读读并发、读写互斥、写写互斥**。如果一个对象并发读的场景大于并发写的场景，那就可以使用 `ReentrantReadWriteLock`来达到保证线程安全的前提下提高并发效率。首先，我们先了解一下Doug Lea为我们准备的两个demo。

### CachedData

一个缓存对象的使用案例，缓存对象在使用时，一般并发读的场景远远大于并发写的场景，所以缓存对象是非常适合使用`ReentrantReadWriteLock`来做控制的
```java
class CachedData {
   //被缓存的具体对象
   Object data;
   //当前对象是否可用，使用volatile来保证可见性
   volatile boolean cacheValid;
   //ReentrantReadWriteLock 
   final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

   //业务处理逻辑
   void processCachedData() {
     //要读取数据时，先加读锁，如果加成功，说明此时没有人在并发写
     rwl.readLock().lock();
     //拿到读锁后，判断当前对象是否有效
     if (!cacheValid) {
       // Must release read lock before acquiring write lock
       //这里的处理非常经典，当你持有读锁之后，不能直接获取写锁，
       //因为写锁是独占锁，如果直接获取写锁，那代码就在这里死锁了
       //所以必须要先释放读锁，然后手动获取写锁
       rwl.readLock().unlock();
       rwl.writeLock().lock();
       try {
         // Recheck state because another thread might have
         // acquired write lock and changed state before we did.
         //经典处理之二，在独占锁内部要处理数据时，一定要做二次校验
         //因为可能同时有多个线程全都在获取写锁，
         //当时线程1释放写锁之后，线程2马上获取到写锁，此时如果不做二次校验那可能就导致某些操作做了多次
         if (!cacheValid) {
           data = ...
           //当缓存对象更新成功后，重置标记为true
           cacheValid = true;
         }
         // Downgrade by acquiring read lock before releasing write lock
         //这里有一个非常神奇的锁降级操作，所谓降级是说当你持有写锁后，可以再次获取读锁
         //这里之所以要获取一次写锁是为了防止当前线程释放写锁之后，其他线程马上获取到写锁，改变缓存对象
         //因为读写互斥，所以有了这个读锁之后，在读锁释放之前，别的线程是无法修改缓存对象的
         rwl.readLock().lock();
       } finally {
         rwl.writeLock().unlock(); // Unlock write, still hold read
       }
     }

     try {
       use(data);
     } finally {
       rwl.readLock().unlock();
     }
   }
 }
```

### RWDictionary

Doug Lea给出的第二个demo，一个并发容器的demo。并发容器我们一般都是直接使用ConcurrentHashMap的，但是我们可以使用非并发安全的容器+ReentrantReadWriteLock来组合出一个并发容器。如果这个并发容器的读的频率>写的频率，那这个效率还是不错的

```java
class RWDictionary {
   //原来非并发安全的容器
   private final Map<String, Data> m = new TreeMap<String, Data>();
   private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
   private final Lock r = rwl.readLock();
   private final Lock w = rwl.writeLock();

   public Data get(String key) {
     //读数据，上读锁
     r.lock();
     try { return m.get(key); }
     finally { r.unlock(); }
   }
   public String[] allKeys() {
     //读数据，上读锁
     r.lock();
     try { return m.keySet().toArray(); }
     finally { r.unlock(); }
   }
   public Data put(String key, Data value) {
     //写数据，上写锁
     w.lock();
     try { return m.put(key, value); }
     finally { w.unlock(); }
   }
   public void clear() {
     //写数据，上写锁
     w.lock();
     try { m.clear(); }
     finally { w.unlock(); }
   }
 }
```


## ReentrantReadWriteLock的特性

**读写锁允许同一时刻被多个读线程访问，但是在写线程访问时，所有的读线程和其他的写线程都会被阻塞**。

在分析 WirteLock 和 ReadLock 的互斥性时，我们可以按照 WriteLock 与 WriteLock，WriteLock 与 ReadLock 以及 ReadLock 与 ReadLock 进行对比分析。

这里总结一下读写锁的特性：

1. **公平性选择**：支持非公平性（默认）和公平的锁获取方式，非公平的吞吐量优于公平；
2. **重入性**：支持重入，读锁获取后能再次获取，写锁获取之后能够再次获取写锁，同时也能够获取读锁；
3. **锁降级**：写锁降级是一种允许写锁转换为读锁的过程。通常的顺序是：
	- 获取写锁：线程首先获取写锁，确保在修改数据时排它访问。
	- 获取读锁：在写锁保持的同时，线程可以再次获取读锁。
	- 释放写锁：线程保持读锁的同时释放写锁。
	- 释放读锁：最后线程释放读锁。

这样，写锁就降级为读锁，允许其他线程进行并发读取，但仍然排除其他线程的写操作。

接下来额外说一下锁降级

- 锁降级

锁降级指的是写锁降级成为读锁。如果当前线程拥有写锁，然后将其释放，最后再获取读锁，这种分段完成的过程不能称之为锁降级。锁降级是指把持住(当前拥有的)写锁，再获取到读锁，随后释放(先前拥有的)写锁的过程。

接下来看一个锁降级的示例。因为数据不常变化，所以多个线程可以并发地进行数据处理，当数据变更后，如果当前线程感知到数据变化，则进行数据的准备工作，同时其他处理线程被阻塞，直到当前线程完成数据的准备工作，如代码如下所示：

```java
public void processData() {
    readLock.lock();
    if (!update) {
        // 必须先释放读锁
        readLock.unlock();
        // 锁降级从写锁获取到开始
        writeLock.lock();
        try {
            if (!update) {
                // 准备数据的流程(略)
                update = true;
            }
            readLock.lock();
        } finally {
            writeLock.unlock();
        }
        // 锁降级完成，写锁降级为读锁
    }
    try {
        // 使用数据的流程(略)
    } finally {
        readLock.unlock();
    }
}
```

上述示例中，当数据发生变更后，update变量(布尔类型且volatile修饰)被设置为false，此时所有访问processData()方法的线程都能够感知到变化，但只有一个线程能够获取到写锁，其他线程会被阻塞在读锁和写锁的lock()方法上。当前线程获取写锁完成数据准备之后，再获取读锁，随后释放写锁，完成锁降级。

锁降级中读锁的获取是否必要呢? 答案是必要的。主要是为了保证数据的可见性，如果当前线程不获取读锁而是直接释放写锁，假设此刻另一个线程(记作线程T)获取了写锁并修改了数据，那么当前线程无法感知线程T的数据更新。如果当前线程获取读锁，即遵循锁降级的步骤，则线程T将会被阻塞，直到当前线程使用数据并释放读锁之后，线程T才能获取写锁进行数据更新。

RentrantReadWriteLock不支持锁升级(把持读锁、获取写锁，最后释放读锁的过程)。目的也是保证数据可见性，如果读锁已被多个线程获取，其中任意线程成功获取了写锁并更新了数据，则其更新对其他获取到读锁的线程是不可见的。


## ReentrantReadWriteLock源码分析

### 类的继承关系

```java
public class ReentrantReadWriteLock implements ReadWriteLock, java.io.Serializable {}
```

说明: 可以看到，ReentrantReadWriteLock实现了ReadWriteLock接口，ReadWriteLock接口定义了获取读锁和写锁的规范，具体需要实现类去实现；同时其还实现了Serializable接口，表示可以进行序列化，在源代码中可以看到ReentrantReadWriteLock实现了自己的序列化逻辑。

### 类的内部类

ReentrantReadWriteLock有五个内部类，五个内部类之间也是相互关联的。内部类的关系如下图所示。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251057400.jpg)

说明: 如上图所示，Sync继承自AQS、NonfairSync继承自Sync类、FairSync继承自Sync类；ReadLock实现了Lock接口、WriteLock也实现了Lock接口。

### 内部类 -类Sync

-  Sync类的继承关系

```java
abstract static class Sync extends AbstractQueuedSynchronizer {}
```

说明: Sync抽象类继承自AQS抽象类，Sync类提供了对ReentrantReadWriteLock的支持。



-  Sync类的内部类

Sync类内部存在两个内部类，分别为HoldCounter和ThreadLocalHoldCounter，其中HoldCounter主要与读锁配套使用，其中，HoldCounter源码如下。

```java
// 计数器
static final class HoldCounter {
    // 计数
    int count = 0;
    // Use id, not reference, to avoid garbage retention
    // 获取当前线程的TID属性的值
    final long tid = getThreadId(Thread.currentThread());
}
```

说明: HoldCounter主要有两个属性，count和tid，其中count表示某个读线程重入的次数，tid表示该线程的tid字段的值，该字段可以用来唯一标识一个线程。ThreadLocalHoldCounter的源码如下

```java
// 本地线程计数器
static final class ThreadLocalHoldCounter
    extends ThreadLocal<HoldCounter> {
    // 重写初始化方法，在没有进行set的情况下，获取的都是该HoldCounter值
    public HoldCounter initialValue() {
        return new HoldCounter();
    }
}
```

说明: ThreadLocalHoldCounter重写了ThreadLocal的initialValue方法，ThreadLocal类可以将线程与对象相关联。在没有进行set的情况下，get到的均是initialValue方法里面生成的那个HolderCounter对象。



-  Sync类的属性

```java
abstract static class Sync extends AbstractQueuedSynchronizer {
    // 版本序列号
    private static final long serialVersionUID = 6317671515068378041L;        
    // 高16位为读锁，低16位为写锁
    static final int SHARED_SHIFT   = 16;
    // 读锁单位
    static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
    // 读锁最大数量
    static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
    // 写锁最大数量
    static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;
    // 本地线程计数器
    private transient ThreadLocalHoldCounter readHolds;
    // 缓存的计数器
    private transient HoldCounter cachedHoldCounter;
    // 第一个读线程
    private transient Thread firstReader = null;
    // 第一个读线程的计数
    private transient int firstReaderHoldCount;
}
```

说明: 该属性中包括了读锁、写锁线程的最大量。本地线程计数器等。



-  Sync类的构造函数

```java
// 构造函数
Sync() {
    // 本地线程计数器
    readHolds = new ThreadLocalHoldCounter();
    // 设置AQS的状态
    setState(getState()); // ensures visibility of readHolds
}
```

说明：在Sync的构造函数中设置了本地线程计数器和AQS的状态state。


### 类的属性

```java
public class ReentrantReadWriteLock
        implements ReadWriteLock, java.io.Serializable {
    // 版本序列号    
    private static final long serialVersionUID = -6992448646407690164L;    
    // 读锁
    private final ReentrantReadWriteLock.ReadLock readerLock;
    // 写锁
    private final ReentrantReadWriteLock.WriteLock writerLock;
    // 同步队列
    final Sync sync;
    
    private static final sun.misc.Unsafe UNSAFE;
    // 线程ID的偏移地址
    private static final long TID_OFFSET;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            // 获取线程的tid字段的内存地址
            TID_OFFSET = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("tid"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
```

说明: 可以看到ReentrantReadWriteLock属性包括了一个ReentrantReadWriteLock.ReadLock对象，表示读锁；一个ReentrantReadWriteLock.WriteLock对象，表示写锁；一个Sync对象，表示同步队列。

### 类的构造函数

- ReentrantReadWriteLock()型构造函数

```java
public ReentrantReadWriteLock() {
    this(false);
}
```

说明: 此构造函数会调用另外一个有参构造函数。

- ReentrantReadWriteLock(boolean)型构造函数

```java
public ReentrantReadWriteLock(boolean fair) {
    // 公平策略或者是非公平策略
    sync = fair ? new FairSync() : new NonfairSync();
    // 读锁
    readerLock = new ReadLock(this);
    // 写锁
    writerLock = new WriteLock(this);
}
```

说明: 可以指定设置公平策略或者非公平策略，并且该构造函数中生成了读锁与写锁两个对象。


### 内部类 - Sync核心函数分析

对ReentrantReadWriteLock对象的操作绝大多数都转发至Sync对象进行处理。下面对Sync类中的重点函数进行分析



- sharedCount函数

表示占有读锁的线程数量，源码如下

```java
static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
```

说明:：直接将state右移16位，就可以得到读锁的线程数量，因为state的高16位表示读锁，对应的低十六位表示写锁数量。



- exclusiveCount函数

表示占有写锁的线程数量，源码如下

```java
static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }
```

说明：

**EXCLUSIVE_MASK**为:

```java
static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;
```

EXCLUSIVE_MASK 为 1 左移 16 位然后减 1，即为 0x0000FFFF。而 exclusiveCount 方法是将同步状态（state 为 int 类型）与 0x0000FFFF 相与，即取同步状态的低 16 位。

那么低 16 位代表什么呢？根据 exclusiveCount 方法的注释为独占式获取的次数即写锁被获取的次数，现在就可以得出来一个结论**同步状态的低 16 位用来表示写锁的获取次数**。


#### 写锁的获取

同一时刻，ReentrantReadWriteLock 的写锁是不能被多个线程获取的，很显然 ReentrantReadWriteLock 的写锁是独占式锁，而实现写锁的同步语义是通过重写 [AQS](https://www.seven97.top/java/concurrent/03-juclock2-aqs.html) 中的 tryAcquire 方法实现的。

- tryAcquire函数

```java
protected final boolean tryAcquire(int acquires) {
    /*
        * Walkthrough:
        * 1. If read count nonzero or write count nonzero
        *    and owner is a different thread, fail.
        * 2. If count would saturate, fail. (This can only
        *    happen if count is already nonzero.)
        * 3. Otherwise, this thread is eligible for lock if
        *    it is either a reentrant acquire or
        *    queue policy allows it. If so, update state
        *    and set owner.
        */
    // 获取当前线程
    Thread current = Thread.currentThread();
    // 获取状态
    int c = getState();
    // 写线程数量
    int w = exclusiveCount(c);
    if (c != 0) { // 状态不为0
        // (Note: if c != 0 and w == 0 then shared count != 0)
        if (w == 0 || current != getExclusiveOwnerThread()) // 写线程数量为0或者当前线程没有占有独占资源
            return false;
        if (w + exclusiveCount(acquires) > MAX_COUNT) // 判断是否超过最高写线程数量
            throw new Error("Maximum lock count exceeded");
        // Reentrant acquire
        // 设置AQS状态
        setState(c + acquires);
        return true;
    }
    if (writerShouldBlock() ||
        !compareAndSetState(c, c + acquires)) // 写线程是否应该被阻塞
        return false;
    // 设置独占线程
    setExclusiveOwnerThread(current);
    return true;
}
```

说明: 此函数用于获取写锁：首先会获取state，判断是否为0；
	1. 若为0，表示此时没有读锁线程，再判断写线程是否应该被阻塞，而在非公平策略下总是不会被阻塞，在公平策略下会进行判断(判断同步队列中是否有等待时间更长的线程；若存在，则需要被阻塞，否则，无需阻塞)，之后在设置状态state，然后返回true。
	2. 若state不为0，则表示此时存在读锁或写锁线程，若写锁线程数量为0或者当前线程为独占锁线程，则返回false，表示不成功，否则，判断写锁线程的重入次数是否大于了最大值，若是，则抛出异常，否则，设置状态state，返回true，表示成功。其函数流程图如下

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251057430.jpg)

其主要逻辑为：**当读锁已经被读线程获取或者写锁已经被其他写线程获取，则写锁获取失败；否则，获取成功并支持重入，增加写状态。**


#### 写锁的释放

写锁释放通过重写 [AQS](https://www.seven97.top/java/concurrent/03-juclock2-aqs.html) 的 tryRelease 方法，源码为：

-  tryRelease函数

```java
/*
* Note that tryRelease and tryAcquire can be called by
* Conditions. So it is possible that their arguments contain
* both read and write holds that are all released during a
* condition wait and re-established in tryAcquire.
*/

protected final boolean tryRelease(int releases) {
    // 判断是否伪独占线程
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    // 计算释放资源后的写锁的数量
    int nextc = getState() - releases;
    boolean free = exclusiveCount(nextc) == 0; // 是否释放成功
    if (free)
        setExclusiveOwnerThread(null); // 设置独占线程为空
    setState(nextc); // 设置状态
    return free;
}
```

说明: 此函数用于释放写锁资源，首先会判断该线程是否为独占线程，若不为独占线程，则抛出异常，否则，计算释放资源后的写锁的数量，若为0，表示成功释放，资源不将被占用，否则，表示资源还被占用。其函数流程图如下。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251057433.jpg)


#### 读锁的获取

看完了写锁，再来看看读锁，读锁不是独占式锁，即同一时刻该锁可以被多个读线程获取，也就是一种共享式锁。按照之前对 [AQS](https://www.seven97.top/java/concurrent/03-juclock2-aqs.html) 的介绍，实现共享式同步组件的同步语义需要通过重写 AQS 的 tryAcquireShared 方法和 tryReleaseShared 方法。读锁的获取实现方法为：

 - tryAcquireShared函数

```java
private IllegalMonitorStateException unmatchedUnlockException() {
    return new IllegalMonitorStateException(
        "attempt to unlock read lock, not locked by current thread");
}

// 共享模式下获取资源
protected final int tryAcquireShared(int unused) {
    /*
        * Walkthrough:
        * 1. If write lock held by another thread, fail.
        * 2. Otherwise, this thread is eligible for
        *    lock wrt state, so ask if it should block
        *    because of queue policy. If not, try
        *    to grant by CASing state and updating count.
        *    Note that step does not check for reentrant
        *    acquires, which is postponed to full version
        *    to avoid having to check hold count in
        *    the more typical non-reentrant case.
        * 3. If step 2 fails either because thread
        *    apparently not eligible or CAS fails or count
        *    saturated, chain to version with full retry loop.
        */
    // 获取当前线程
    Thread current = Thread.currentThread();
    // 获取状态
    int c = getState();
    if (exclusiveCount(c) != 0 &&
        getExclusiveOwnerThread() != current) // 写线程数不为0并且占有资源的不是当前线程
        return -1;
    // 读锁数量
    int r = sharedCount(c);
    if (!readerShouldBlock() &&
        r < MAX_COUNT &&
        compareAndSetState(c, c + SHARED_UNIT)) { // 读线程是否应该被阻塞、并且小于最大值、并且比较设置成功
        if (r == 0) { // 读锁数量为0
            // 设置第一个读线程
            firstReader = current;
            // 读线程占用的资源数为1
            firstReaderHoldCount = 1;
        } else if (firstReader == current) { // 当前线程为第一个读线程
            // 占用资源数加1
            firstReaderHoldCount++;
        } else { // 读锁数量不为0并且不为当前线程
            // 获取计数器
            HoldCounter rh = cachedHoldCounter;
            if (rh == null || rh.tid != getThreadId(current)) // 计数器为空或者计数器的tid不为当前正在运行的线程的tid
                // 获取当前线程对应的计数器
                cachedHoldCounter = rh = readHolds.get();
            else if (rh.count == 0) // 计数为0
                // 设置
                readHolds.set(rh);
            rh.count++;
        }
        return 1;
    }
    return fullTryAcquireShared(current);
}
```

说明: 此函数表示读锁线程获取读锁。首先判断写锁是否为0并且当前线程不占有独占锁，直接返回；否则，判断读线程是否需要被阻塞并且读锁数量是否小于最大值并且比较设置状态成功，若当前没有读锁，则设置第一个读线程firstReader和firstReaderHoldCount；若当前线程线程为第一个读线程，则增加firstReaderHoldCount；否则，将设置当前线程对应的HoldCounter对象的值。流程图如下。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251057428.jpg)


 **当写锁被其他线程获取后，读锁获取失败**，否则获取成功，会利用 CAS 更新同步状态。

另外，当前同步状态需要加上 SHARED_UNIT（`(1 << SHARED_SHIFT)`，即 0x00010000）的原因，我们在上面也说过了，同步状态的高 16 位用来表示读锁被获取的次数。

如果 CAS 失败或者已经获取读锁的线程再次获取读锁时，是靠 fullTryAcquireShared 方法实现的。

- fullTryAcquireShared函数

```java
final int fullTryAcquireShared(Thread current) {
    /*
        * This code is in part redundant with that in
        * tryAcquireShared but is simpler overall by not
        * complicating tryAcquireShared with interactions between
        * retries and lazily reading hold counts.
        */
    HoldCounter rh = null;
    for (;;) { // 无限循环
        // 获取状态
        int c = getState();
        if (exclusiveCount(c) != 0) { // 写线程数量不为0
            if (getExclusiveOwnerThread() != current) // 不为当前线程
                return -1;
            // else we hold the exclusive lock; blocking here
            // would cause deadlock.
        } else if (readerShouldBlock()) { // 写线程数量为0并且读线程被阻塞
            // Make sure we're not acquiring read lock reentrantly
            if (firstReader == current) { // 当前线程为第一个读线程
                // assert firstReaderHoldCount > 0;
            } else { // 当前线程不为第一个读线程
                if (rh == null) { // 计数器不为空
                    // 
                    rh = cachedHoldCounter;
                    if (rh == null || rh.tid != getThreadId(current)) { // 计数器为空或者计数器的tid不为当前正在运行的线程的tid
                        rh = readHolds.get();
                        if (rh.count == 0)
                            readHolds.remove();
                    }
                }
                if (rh.count == 0)
                    return -1;
            }
        }
        if (sharedCount(c) == MAX_COUNT) // 读锁数量为最大值，抛出异常
            throw new Error("Maximum lock count exceeded");
        if (compareAndSetState(c, c + SHARED_UNIT)) { // 比较并且设置成功
            if (sharedCount(c) == 0) { // 读线程数量为0
                // 设置第一个读线程
                firstReader = current;
                // 
                firstReaderHoldCount = 1;
            } else if (firstReader == current) {
                firstReaderHoldCount++;
            } else {
                if (rh == null)
                    rh = cachedHoldCounter;
                if (rh == null || rh.tid != getThreadId(current))
                    rh = readHolds.get();
                else if (rh.count == 0)
                    readHolds.set(rh);
                rh.count++;
                cachedHoldCounter = rh; // cache for release
            }
            return 1;
        }
    }
}
```

说明: 在tryAcquireShared函数中，如果下列三个条件不满足(读线程是否应该被阻塞、小于最大值、比较设置成功)则会进行fullTryAcquireShared函数中，它用来保证相关操作可以成功。其逻辑与tryAcquireShared逻辑类似，可以继续再往后看。

#### 读锁的释放

读锁释放的实现主要通过方法 tryReleaseShared，源码如下，主要逻辑请看注释：

- tryReleaseShared函数

```java
protected final boolean tryReleaseShared(int unused) {
    // 获取当前线程
    Thread current = Thread.currentThread();
    if (firstReader == current) { // 当前线程为第一个读线程
        // assert firstReaderHoldCount > 0;
        if (firstReaderHoldCount == 1) // 读线程占用的资源数为1
            firstReader = null;
        else // 减少占用的资源
            firstReaderHoldCount--;
    } else { // 当前线程不为第一个读线程
        // 获取缓存的计数器
        HoldCounter rh = cachedHoldCounter;
        if (rh == null || rh.tid != getThreadId(current)) // 计数器为空或者计数器的tid不为当前正在运行的线程的tid
            // 获取当前线程对应的计数器
            rh = readHolds.get();
        // 获取计数
        int count = rh.count;
        if (count <= 1) { // 计数小于等于1
            // 移除
            readHolds.remove();
            if (count <= 0) // 计数小于等于0，抛出异常
                throw unmatchedUnlockException();
        }
        // 减少计数
        --rh.count;
    }
    for (;;) { // 无限循环
        // 获取状态
        int c = getState();
        // 获取状态
        int nextc = c - SHARED_UNIT;
        if (compareAndSetState(c, nextc)) // 比较并进行设置
            // Releasing the read lock has no effect on readers,
            // but it may allow waiting writers to proceed if
            // both read and write locks are now free.
            return nextc == 0;
    }
}
```

说明: 此函数表示读锁线程释放锁。首先判断当前线程是否为第一个读线程firstReader，若是，则判断第一个读线程占有的资源数firstReaderHoldCount是否为1，若是，则设置第一个读线程firstReader为空，否则，将第一个读线程占有的资源数firstReaderHoldCount减1；若当前线程不是第一个读线程，那么首先会获取缓存计数器(上一个读锁线程对应的计数器 )，若计数器为空或者tid不等于当前线程的tid值，则获取当前线程的计数器，如果计数器的计数count小于等于1，则移除当前线程对应的计数器，如果计数器的计数count小于等于0，则抛出异常，之后再减少计数即可。无论何种情况，都会进入无限循环，该循环可以确保成功设置状态state。其流程图如下

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251057425.jpg)

#### 锁降级

读写锁支持锁降级，**遵循按照获取写锁，获取读锁再释放写锁的次序，写锁能够降级成为读锁**，不支持锁升级，关于锁降级，下面的示例代码摘自 ReentrantWriteReadLock 源码：

```java
void processCachedData() {
    rwl.readLock().lock();
    if (!cacheValid) {
        // Must release read lock before acquiring write lock
        rwl.readLock().unlock();
        rwl.writeLock().lock();
        try {
            // Recheck state because another thread might have
            // acquired write lock and changed state before we did.
            if (!cacheValid) {
                data = ...
        cacheValid = true;
      }
      // Downgrade by acquiring read lock before releasing write lock
      rwl.readLock().lock();
    } finally {
      rwl.writeLock().unlock(); // Unlock write, still hold read
    }
  }

  try {
    use(data);
  } finally {
    rwl.readLock().unlock();
  }
}
```

这里的流程可以解释如下：

- 获取读锁：首先尝试获取读锁来检查某个缓存是否有效。
- 检查缓存：如果缓存无效，则需要释放读锁，因为在获取写锁之前必须释放读锁。
- 获取写锁：获取写锁以便更新缓存。此时，可能还需要重新检查缓存状态，因为在释放读锁和获取写锁之间可能有其他线程修改了状态。
- 更新缓存：如果确认缓存无效，更新缓存并将其标记为有效。
- 写锁降级为读锁：在释放写锁之前，获取读锁，从而实现写锁到读锁的降级。这样，在释放写锁后，其他线程可以并发读取，但不能写入。
- 使用数据：现在可以安全地使用缓存数据了。
- 释放读锁：完成操作后释放读锁。

这个流程结合了读锁和写锁的优点，确保了数据的一致性和可用性，同时允许在可能的情况下进行并发读取。使用读写锁的代码可能看起来比使用简单的互斥锁更复杂，但它提供了更精细的并发控制，可能会提高多线程应用程序的性能


## ReadWriteLock和StampedLock

### ReadWriteLock

ReadWriteLock 是Java**提供的一个接口**，全类名：java.util.concurrent.locks.ReadWriteLock，上面的ReentrantReadWriteLock就是继承自这个接口。它允许多个线程同时读取共享资源，但只允许一个线程写入共享资源。这种机制可以提高读取操作的并发性，但写入操作需要独占资源。

#### 特性

- 多个线程可以同时获取读锁，但只有一个线程可以获取写锁。
- 当一个线程持有写锁时，其他线程无法获取读锁和写锁，读写互斥。
- 当一个线程持有读锁时，其他线程可以同时获取读锁，读读共享。但无法获取写锁，读写互斥

#### 使用场景

ReadWriteLock 适用于读多写少的场景，例如缓存系统、数据库连接池等。在这些场景中，读取操作占据大部分时间，而写入操作较少。

#### 使用示例

使用 ReadWriteLock 的示例，实现了一个简单的缓存系统：

```java
public class Cache {
    private Map<String, Object> data = new HashMap<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public Object get(String key) {
        lock.readLock().lock();
        try {
            return data.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void put(String key, Object value) {
        lock.writeLock().lock();
        try {
            data.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
```

在上述示例中，Cache 类使用 ReadWriteLock 来实现对 data 的并发访问控制。get 方法获取读锁并读取数据，put 方法获取写锁并写入数据。

### StampedLock

StampedLock 是Java 8 中引入的一种新的锁机制，全类名：java.util.concurrent.locks.StampedLock，它提供了一种乐观读的机制，可以进一步提升读取操作的并发性能。

#### 特性

- 与 ReadWriteLock 类似，StampedLock 也支持多个线程同时获取读锁，但只允许一个线程获取写锁。
- 与 ReadWriteLock 不同的是，StampedLock 还提供了一个乐观读锁（Optimistic Read Lock），即不阻塞其他线程的写操作，但在读取完成后需要验证数据的一致性。

#### 使用场景

StampedLock 适用于读远远大于写的场景，并且对数据的一致性要求不高，例如统计数据、监控系统等。

#### 使用示例

使用 StampedLock 的示例，实现了一个计数器：

```java
public class Counter {
    private int count = 0;
    private StampedLock lock = new StampedLock();

    public int getCount() {
        long stamp = lock.tryOptimisticRead();
        int value = count;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                value = count;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return value;
    }

    public void increment() {
        long stamp = lock.writeLock();
        try {
            count++;
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}
```

在上述示例中，Counter 类使用 StampedLock 来实现对计数器的并发访问控制。getCount 方法首先尝试获取乐观读锁，并读取计数器的值，然后通过 validate 方法验证数据的一致性。如果验证失败，则获取悲观读锁，并重新读取计数器的值。increment 方法获取写锁，并对计数器进行递增操作。

### 小结

ReadWriteLock 和 StampedLock 都是Java中用于并发控制的重要机制。

- ReadWriteLock 适用于读多写少的场景;
- StampedLock 则适用于读远远大于写的场景，并且对数据的一致性要求不高;

在实际应用中，我们需要根据具体场景来选择合适的锁机制。通过合理使用这些锁机制，我们可以提高并发程序的性能和可靠性。


<!-- @include: @article-footer.snippet.md -->     