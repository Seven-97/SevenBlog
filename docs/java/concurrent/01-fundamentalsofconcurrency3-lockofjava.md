---
title: Java中到底有哪些锁？
category: Java
tags:
  - 并发编程
head:
  - - meta
    - name: keywords
      content: 多线程,锁，偏向锁,自旋锁,乐观锁,悲观锁,公平锁,非公平锁,独享锁,共享锁,轻量级锁
  - - meta
    - name: description
      content: 全网最全的Java并发编程知识点总结，让天下没有难学的八股文！
---





## 乐观锁和悲观锁

不是具体的锁，是指看待并发同步的角度

悲观锁：对于同一个数据的并发操作，悲观锁认为自己在使用数据的时候一定有别的线程来修改数据，因此在获取数据的时候会先加锁，确保数据不会被别的线程修改。Java中，synchronized关键字和Lock的实现类都是悲观锁。

乐观锁：乐观锁不是真的锁，而是一种实现。乐观锁认为自己在使用数据时不会有别的线程修改数据，所以不会添加锁，只是在更新数据的时候去判断之前有没有别的线程更新了这个数据。如果这个数据没有被更新，当前线程将自己修改的数据成功写入。如果数据已经被其他线程更新，则根据不同的实现方式执行不同的操作（例如报错或者自动重试）。

在JAVA中 悲观锁是指利用各种锁机制，而乐观锁是指无锁编程，最常采用的是CAS算法，典型的原子类，通过CAS算法实现原子类操作的更新。

可以发现：

- 悲观锁适合写操作多的场景，先加锁可以保证写操作时数据正确。

- 乐观锁适合读操作多的场景，不加锁的特点能够使其读操作的性能大幅提升。

## 自旋锁 && 适应性自旋锁

### 自旋锁

阻塞或唤醒一个Java线程需要操作系统切换CPU状态来完成，这种状态转换需要耗费处理器时间。如果同步代码块中的内容过于简单，状态转换消耗的时间有可能比用户代码执行的时间还要长。

在许多场景中，同步资源的锁定时间很短，为了这一小段时间去切换线程，线程挂起和恢复现场的花费可能会让系统得不偿失。如果物理机器有多个处理器，能够让两个或以上的线程同时并行执行，就可以让后面那个请求锁的线程不放弃CPU的执行时间，看看持有锁的线程是否很快就会释放锁。

而为了让当前线程“稍等一下”，就让当前线程进行自旋，如果在自旋完成后前面锁定同步资源的线程已经释放了锁，那么当前线程就可以不必阻塞而是直接获取同步资源，从而避免切换线程的开销。这就是自旋锁。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251014768.jpg)

也就是说尝试获取锁的线程不会立即阻塞，而是采用循环的方式去尝试获取锁。

**总的来说就是**，线程的切换需要进行上下文切换，上下文的切换开销可能比执行任务代码的开销还要大，那么这种情况就可以先"自旋"，避免切换线程的开销

自旋锁的缺点：自旋等待虽然避免了线程切换的开销，但它要占用处理器时间。如果锁被占用的时间很短，自旋等待的效果就会非常好。反之，如果锁被占用的时间很长，那么自旋的线程只会白浪费处理器资源。

自旋锁的实现原理同样也是CAS，AtomicInteger中调用unsafe进行自增操作的源码中的do-while循环就是一个自旋操作，如果修改数值失败则通过循环来执行自旋，直至修改成功。

### 适应性自旋锁

自适应意味着自旋的时间（次数）不再固定，而是由前一次在同一个锁上的自旋时间及锁的拥有者的状态来决定。如果在同一个锁对象上，自旋等待刚刚成功获得过锁，并且持有锁的线程正在运行中，那么虚拟机就会认为这次自旋也是很有可能再次成功，进而它将允许自旋等待持续相对更长的时间。如果对于某个锁，自旋很少成功获得过，那在以后尝试获取这个锁时将可能省略掉自旋过程，直接阻塞线程，避免浪费处理器资源。

## 无锁，偏向锁，轻量级锁，重量级锁

Java中的synchronized锁升级机制是指synchronized锁在不同的竞争情况下，会根据竞争的激烈程度确定锁的状态，为了优化性能，主要通过对象监视器在对象头的字段来表明。

### 相关概念

#### Java对象头

synchronized是悲观锁，在操作同步资源之前需要给同步资源先加锁，这把锁就是存在Java对象头里的。

Hotspot的对象头主要包括两部分数据：Mark Word（标记字段）、Klass Pointer（类型指针）。

Mark Word：用于存储对象自身运行时数据，如HashCode、GC分代年龄、锁状态标志、线程持有锁、偏向线程ID、偏向时间戳等信息。这些信息都是与对象自身定义无关的数据，所以Mark Word被设计成一个非固定的数据结构以便在极小的空间内存存储尽量多的数据。它会根据对象的状态复用自己的存储空间，也就是说在运行期间Mark Word里存储的数据会随着锁标志位的变化而变化。

Klass Point：对象指向它的类元数据的指针，虚拟机通过这个指针来确定这个对象是哪个类的实例。

#### Monitor

Monitor可以理解为一个同步工具或一种同步机制，通常被描述为一个对象。每一个Java对象就有一把看不见的锁，称为内部锁或者Monitor锁。

Monitor的基本结构是什么？

- Owner字段：初始时为NULL表示当前没有任何线程拥有该monitor record，当线程成功拥有该锁后保存线程唯一标识，当锁被释放时又设置为NULL

- EntryQ字段：关联一个系统互斥锁（semaphore），阻塞所有试图锁住monitor record失败的线程

- RcThis字段：表示blocked或waiting在该monitor record上的所有线程的个数

- Nest字段：用来实现重入锁的计数

- HashCode字段：保存从对象头拷贝过来的HashCode值（可能还包含GC age）

- Candidate字段：用来避免不必要的阻塞或等待线程唤醒，因为每一次只有一个线程能够成功拥有锁，如果每次前一个释放锁的线程唤醒所有正在阻塞或等待的线程，会引起不必要的上下文切换（从阻塞到就绪然后因为竞争锁失败又被阻塞）从而导致性能严重下降；Candidate只有两种可能的值0表示没有需要唤醒的线程1表示要唤醒一个继任线程来竞争锁

 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251014754.gif)

1. 刚开始Monitor中Owner为null
2. 当Thread-2执行Synchronized(obj)就会将Monitor的所有者owner置为Thread-2。Monitor中只能有一个Owner。
3. 在Thread-2上锁的过程中，如果Thread-3，Thread-4，Thread-5也来执行Synchronized(obj)，就会进入EntryList Blocked。
4. Thread-2执行完同步代码块的内容，然后唤醒EntryList中等待的线程来竞争锁，竞争时是非公平的
5. 图中WaitSet中的Thread-0，Thread-1是之前获得过锁，但条件不满足而进入waiting状态的线程

Monitor是线程私有的数据结构，每一个线程都有一个可用monitor record列表，同时还有一个全局的可用列表。每一个被锁住的对象都会和一个monitor关联，同时monitor中有一个Owner字段存放拥有该锁的线程的唯一标识，表示该锁被这个线程占用。

synchronized通过Monitor来实现线程同步，Monitor是依赖于底层的操作系统的Mutex Lock（互斥锁）来实现的线程同步。这种依赖于操作系统Mutex Lock所实现的锁我们称之为“重量级锁”，因此，为了减少获得锁和释放锁带来的性能消耗，引入了“偏向锁”和“轻量级锁”。

#### 四种锁状态对应的的Mark Word内容：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251014760.gif)

### 无锁

当一个线程进入synchronized代码块时，并且没有其他线程竞争这个锁时，锁处于无锁状态。

### 偏向锁

当一个线程进入synchronized代码块，并且没有其他线程竞争这个锁时，JVM会偏向于这个线程，将对象头的Mark Word设置为指向线程的ID。这样，下次该线程再次进入同步块时，无需进行加锁操作，可以直接进入，显然可以提高性能。

偏向锁只有遇到其他线程尝试竞争偏向锁时，持有偏向锁的线程才会释放锁，线程不会主动释放偏向锁。偏向锁的撤销，需要等待全局安全点（在这个时间点上没有字节码正在执行），它会首先暂停拥有偏向锁的线程，判断锁对象是否处于被锁定状态。撤销偏向锁后恢复到无锁（标志位为“01”）或升级到轻量级锁（标志位为“00”）的状态。

### 轻量级锁

轻量级锁：当前锁是偏向锁并且**被另外一个线程访问**时，**偏向锁就会升级为轻量级锁**，其他线程会通过CAS自旋的方式尝试获取锁，不会阻塞其他线程。轻量级锁使用CAS操作来避免线程竞争，不会引起线程的阻塞和唤醒，使得线程在竞争较少的情况下可以更高效地获取锁。

- 在代码进入同步块的时候，如果同步对象锁状态为无锁状态（锁标志位为“01”状态，是否为偏向锁为“0”），虚拟机首先将在当前线程的栈帧中建立一个名为锁记录（Lock Record）的空间，用于存储锁对象目前的Mark Word的拷贝，然后拷贝对象头中的Mark Word复制到锁记录中。

- 拷贝成功后，虚拟机将使用CAS操作尝试将对象的Mark Word更新为指向Lock Record的指针，并将Lock Record里的owner指针指向对象的Mark Word。

- 如果这个更新动作成功了，那么这个线程就拥有了该对象的锁，并且对象Mark Word的锁标志位设置为“00”，表示此对象处于轻量级锁定状态。

- 如果轻量级锁的更新操作失败了，虚拟机首先会检查对象的Mark Word是否指向当前线程的栈帧，如果是就说明当前线程已经拥有了这个对象的锁，那就可以直接进入同步块继续执行，否则说明多个线程竞争锁。

若当前只有一个等待线程，则该线程通过自旋进行等待。但是当自旋超过一定的次数，或者一个线程在持有锁，一个在自旋，又有第三个来访时，轻量级锁升级为重量级锁。

### 重量级锁

升级为重量级锁时，锁标志的状态值变为“10”，此时Mark Word中存储的是指向重量级锁的指针，此时等待锁的线程都会进入阻塞状态。

重量级锁：当前锁是轻量级锁，另一个线程自旋到**一定次数**还没获取到锁，或者又有第三个线程来访时，轻量级锁就会升级重量级锁。重量级锁使用操作系统的互斥量（Mutex）来实现，使得其他竞争线程阻塞等待锁的释放。

### 小结

无锁：无竞争

偏向锁：长时间只有一个线程访问，就会获得偏向锁

轻量级锁：当前锁是偏向锁并且被另外一个线程访问时，偏向锁就会升级为轻量级锁

重量级锁：若当前只有一个等待线程，则该线程通过自旋进行等待。但是当自旋超过一定的次数，或者一个线程在持有锁，一个在自旋，又有第三个来访时，轻量级锁升级为重量级锁。

## 公平锁和非公平锁

公平锁：多个线程按照申请锁的顺序来获取锁

公平锁的优点是等待锁的线程不会饿死。缺点是整体吞吐效率相对非公平锁要低，等待队列中除第一个线程以外的所有线程都会阻塞，CPU唤醒阻塞线程的开销比非公平锁大。

非公平锁：多个线程加锁时直接尝试获取锁，获取不到才会到等待队列的队尾等待。但如果此时锁刚好可用，那么这个线程可以无需阻塞直接获取到锁，所以非公平锁有可能出现后申请锁的线程先获取锁的场景。

非公平锁的优点是可以减少唤起线程的开销，整体的吞吐效率高，因为线程有几率不阻塞直接获得锁，CPU不必唤醒所有线程。缺点是处于等待队列中的线程可能会饿死，或者等很久才会获得锁。

在JAVA中，ReentrantLock可通过构造函数至指定是否是公平锁，默认是非公平锁

```java
//********************* 公平锁加锁 ***********************
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        if (!hasQueuedPredecessors() && //区别在这，!hasQueuedPredecessors()
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

//********************* 非公平锁加锁 ***********************
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

通过上面的源代码对比，可以明显的看出公平锁与非公平锁的lock()方法唯一的区别就在于公平锁在获取同步状态时多了一个限制条件：hasQueuedPredecessors()。这个方法主要是判断当前线程是否位于同步队列中的第一个。如果是则返回true，否则返回false。也就是说公平锁按照队列等待顺序来加锁的

```java
public final boolean hasQueuedPredecessors() {
    Node t = tail; // Read fields in reverse initialization order
    Node h = head;
    Node s;
    return h != t &&
        ((s = h.next) == null || s.thread != Thread.currentThread());
}
```

synchronized默认是非公平锁并且不能变为公平锁



## 可重入锁(递归锁) 和 非可重入锁

可重入锁：指在同一线程在外层方法获取锁的时候，进入内层方法时会自动获取锁，，不会因为之前已经获取过还没释放而阻塞。

Java中ReentrantLock和synchronized都是可重入锁

ReentrantLock主要通过AQS的state变量实现可重入性，synchronized通过计数器（recursions变量）实现可重入性

ReentrantLock中，当线程尝试获取锁时，可重入锁先尝试获取并更新state值，如果state == 0表示没有其他线程在执行同步代码，则把state置为1，当前线程开始执行。如果state != 0，则判断当前线程是否是获取到这个锁的线程，如果是的话执行state+1，且当前线程可以再次获取锁。而非可重入锁是直接去获取并尝试更新当前state的值，如果state != 0的话会导致其获取锁失败，当前线程阻塞。

释放锁时，可重入锁同样先获取当前state的值，在当前线程是持有锁的线程的前提下。如果state-1 == 0，则表示当前线程所有重复获取锁的操作都已经执行完毕，然后该线程才会真正释放锁。而非可重入锁则是在确定当前线程是持有锁的线程之后，直接将state置为0，将锁释放。

## 独享锁和共享锁

独享锁和共享锁是一种概念

独享锁也叫排它锁，即一个锁只能被一个线程所持有。ReentrantLock和synchronzied方法是独享锁。

共享锁：一个锁可被多个线程持有。获得共享锁的线程只能读数据，不能修改数据。

独享锁与共享锁也是通过AQS来实现的，通过实现不同的方法，来实现独享或者共享。ReentrantReadWriteLock有两把锁：ReadLock和WriteLock。

```java
public class ReentrantReadWriteLock
        implements ReadWriteLock, java.io.Serializable {
   
    //……
    private final ReentrantReadWriteLock.ReadLock readerLock;
    
    private final ReentrantReadWriteLock.WriteLock writerLock;

    //……

    public ReentrantReadWriteLock.WriteLock writeLock() { return writerLock; }
    public ReentrantReadWriteLock.ReadLock  readLock()  { return readerLock; }

    //……
}
```



在ReentrantReadWriteLock里面，读锁和写锁的锁主体都是Sync，但读锁和写锁的加锁方式不一样。读锁是共享锁，写锁是独享锁。读锁的共享锁可保证并发读非常高效，而读写、写读、写写的过程互斥，因为读锁和写锁是分离的。所以ReentrantReadWriteLock的并发性相比一般的互斥锁有了很大提升。

AQS有个state字段，该字段用来描述有多少线程获持有锁。在独享锁中这个值通常是0或者1（如果是重入锁的话state值就是重入的次数），在共享锁中state就是持有锁的数量。

但是在ReentrantReadWriteLock中有读、写两把锁，所以需要在整型变量state上分别描述读锁和写锁的数量（或者也可以叫状态）。于是将state变量“按位切割”切分成了两个部分，高16位表示读锁状态（读锁个数），低16位表示写锁状态（写锁个数）

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251014753.gif)

### 写锁加锁源码：

```java
protected final boolean tryAcquire(int acquires) {
    Thread current = Thread.currentThread();
    int c = getState();//获取当前锁的个数
    int w = exclusiveCount(c);//取写锁的个数
    if (c != 0) {//如果已经有线程持有了锁
        // (Note: if c != 0 and w == 0 then shared count != 0)
        
        //如果写线程数为0 （也就是说只有读锁），或者持有锁的线程不是当前线程，就返回失败
        if (w == 0 || current != getExclusiveOwnerThread())
            return false;
        //如果写入锁的数量大于最大数65535，就抛出error
        if (w + exclusiveCount(acquires) > MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
        // Reentrant acquire
        setState(c + acquires);
        return true;
    }
    
    //执行到这里说明还没有线程持有锁
    //那么就表示写线程数也为0，并且当前线程需要阻塞那么就返回失败；或者如果通过CAS增加写线程数失败也返回失败。
    if (writerShouldBlock() ||
        !compareAndSetState(c, c + acquires))
        return false;
    
    //前面都没有返回，执行到这里
    //如果c=0，w=0或者c>0，w>0（重入），则设置当前线程或锁的拥有者，返回成功！
    setExclusiveOwnerThread(current);
    return true;
}
```

上面这段代码流程如下：

1. 首先取到当前锁的个数c，然后再通过c来获取写锁的个数w。因为写锁是低16位，所以取低16位的最大值与当前的c做与运算（ int w = exclusiveCount(c); ），高16位和0与运算后是0，剩下的就是低位运算的值，同时也是持有写锁的线程数目。
2. 在取到写锁线程的数目后，首先判断是否已经有线程持有了锁。如果已经有线程持有了锁（c!=0），则查看当前写锁线程的数目，如果写线程数为0（即此时存在读锁）或者持有锁的线程不是当前线程就返回失败（涉及到公平锁和非公平锁的实现）。
3. 如果写入锁的数量大于最大数（65535，2的16次方-1）就抛出一个Error。
4. 如果当前还没有线程持有锁 c=0，即写线程数为0（那么读线程也应该为0），并且当前线程需要阻塞那么就返回失败；如果通过CAS增加写线程数失败也返回失败。
5. 如果c=0，w=0或者c>0，w>0（重入），则设置当前线程或锁的拥有者，返回成功！

tryAcquire()除了重入条件（当前线程为获取了写锁的线程）之外，增加了一个读锁是否存在的判断。如果存在读锁，则写锁不能被获取，原因在于：必须确保写锁的操作对读锁可见，如果允许读锁在已被获取的情况下对写锁的获取，那么正在运行的其他读线程就无法感知到当前写线程的操作。因此，只有等待其他读线程都释放了读锁，写锁才能被当前线程获取，而写锁一旦被获取，则其他读写线程的后续访问均被阻塞。写锁的释放与ReentrantLock的释放过程基本类似，每次释放均减少写状态，当写状态为0时表示写锁已被释放，然后等待的读写线程才能够继续访问读写锁，同时前次写线程的修改对后续的读写线程可见。

### 读锁加锁源码：

```java
protected final int tryAcquireShared(int unused) {
    
    Thread current = Thread.currentThread();
    int c = getState();
    if (exclusiveCount(c) != 0 &&
        getExclusiveOwnerThread() != current)
        return -1;//如果其他线程已经获取了写锁，则当前线程获取读锁失败，进入等待状态
    int r = sharedCount(c);
    if (!readerShouldBlock() &&
        r < MAX_COUNT &&
        compareAndSetState(c, c + SHARED_UNIT)) {
        if (r == 0) {
            firstReader = current;
            firstReaderHoldCount = 1;
        } else if (firstReader == current) {
            firstReaderHoldCount++;
        } else {
            HoldCounter rh = cachedHoldCounter;
            if (rh == null || rh.tid != getThreadId(current))
                cachedHoldCounter = rh = readHolds.get();
            else if (rh.count == 0)
                readHolds.set(rh);
            rh.count++;
        }
        return 1;
    }
    return fullTryAcquireShared(current);
}
```

在tryAcquireShared(int unused)方法中，如果其他线程已经获取了写锁，则当前线程获取读锁失败，进入等待状态。如果当前线程获取了写锁或者写锁未被获取，则当前线程（线程安全，依靠CAS保证）增加读状态，成功获取读锁。读锁的每次释放（线程安全的，可能有多个读线程同时释放读锁）均减少读状态，减少的值是“1<<16”。所以读写锁才能实现读读的过程共享，而读写、写读、写写的过程互斥。

共享式与独占式的最主要区别在于：同一时刻独占式只能有一个线程获取同步状态，而共享式在同一时刻可以有多个线程获取同步状态。例如读操作可以有多个线程同时进行，而写操作同一时刻只能有一个线程进行写操作，其他操作都会被阻塞。

## 互斥锁和读写锁

互斥锁和读写锁是共享锁和独享锁的具体表现。



## 分段锁

分段锁并不是具体的一个锁，其目的是细化锁的粒度。

比如要保证数组中数据的线程安全，我们可以对其上锁，但是这样会影响效率，线程A在操作数组的时候，其他线程是不允许操作的。想一下如果线程A修改数组中下标0~9对应的元素，线程B要修改下标10~15的元素，这两个线程同时操作也不会出现线程安全问题，那可以对数组采用两把锁来控制，一把锁控制下标0~9的元素，另一把锁控制下标10~15的元素，这就是分段锁。相比于单个锁来说可以提高性能。

jdk7中的[ConcurrentHashMap](https://www.seven97.top/java/collection/04-juc2-concurrenthashmap.html)就采用了分段锁。


## 分布式锁

[Redis实现分布式锁 | Seven的菜鸟成长之路 (seven97.top)](https://www.seven97.top/database/redis/05-implementdistributedlocks.html)

[ZooKeeper - 分布式锁 | Seven的菜鸟成长之路 (seven97.top)](https://www.seven97.top/microservices/service-registration-and-discovery/zookeeper-distributedlocks.html)


<!-- @include: @article-footer.snippet.md -->     
