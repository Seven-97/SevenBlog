---
title: 线程池 - ThreadPoolExecutor详解
category: Java
tag:
 - 并发编程
---





## 频繁创建新线程的缺点？

### 不受控风险

系统资源有限，每个人针对不同业务都可以手动创建线程，并且创建标准不一样（比如线程没有名字）。当系统运行起来，所有线程都在疯狂抢占资源，毫无规则，不好管控。

另外，过多的线程自然也会引起上下文切换的开销。

### 频繁创建开销大

new Thread() 在操作系统层面并没有创建新的线程；

真正转换为操作系统层面创建一个线程，还要调用操作系统内核的API，然后操作系统要为该线程分配一系列的资源。

#### new Object() 过程

Object obj = new Object();

1. 分配一块内存 M
2. 在内存 M 上初始化该对象
3. 将内存 M 的地址赋值给引用变量 obj

#### 创建线程过程

1. JVM为一个线程栈分配内存，该栈为每个线程方法调用保存一个栈帧
2. 每一栈帧由一个局部变量数组、返回值、操作数堆栈和常量池组成
3. 一些支持本机方法的 jvm 也会分配一个本机堆栈
4. 每个线程获得一个程序计数器，告诉它当前处理器执行的指令是什么
5. 系统创建一个与Java线程对应的本机线程
6. 将与线程相关的描述符添加到JVM内部数据结构中
7. 线程共享堆和方法区域

### 使用线程池的优点

降低资源消耗。通过重复利用已创建的线程降低线程创建和销毁造成的消耗。

提高响应速度。当任务到达时，可以不需要等到线程创建就能立即执行。提高线程的可管理性。

统一管理线程，避免系统创建大量同类线程而导致消耗完内存。

## 线程池原理

### JDK线程池参数

ThreadPoolExecutor 的通用构造函数：

```java
public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler);
```

1. corePoolSize：当有新任务时，如果线程池中线程数没有达到核心线程池的大小corePoolSize，则会创建新的线程执行任务，否则将任务放入阻塞队列。当线程池中存活的线程数总是大于 corePoolSize 时，应该考虑调大 corePoolSize。

2. maximumPoolSize：当阻塞队列填满时，如果线程池中线程数没有超过最大线程数maximumPoolSize，则会创建新的线程运行任务。如果线程池中线程数已经达到最大线程数maximumPoolSiz，则会根据拒绝策略处理新任务。非核心线程类似于临时借来的资源，这些线程在空闲时间超过 keepAliveTime 之后，就应该退出，避免资源浪费。

3. BlockingQueue：阻塞队列，存储等待运行的任务。

4. keepAliveTime：非核心线程空闲后，保持存活的时间，此参数只对非核心线程有效。设置为0，表示多余的空闲线程会被立即终止。

5. TimeUnit：keepAliveTime的时间单位TimeUnit.DAYS

   - TimeUnit.HOURS

   - TimeUnit.MINUTES

   - TimeUnit.SECONDS

   - TimeUnit.MILLISECONDS

   - TimeUnit.MICROSECONDS

   - TimeUnit.NANOSECONDS

6. ThreadFactory：每当线程池创建一个新的线程时，都是通过线程工厂方法来完成的。在 ThreadFactory 中只定义了一个方法 newThread，每当线程池需要创建新线程就会调用它。

```java
public class MyThreadFactory implements ThreadFactory {
    private final String poolName;
    
    public MyThreadFactory(String poolName) {
        this.poolName = poolName;
    }
    
    public Thread newThread(Runnable runnable) {
        return new MyAppThread(runnable, poolName);//将线程池名字传递给构造函数，用于区分不同线程池的线程
    }
}
```

7. RejectedExecutionHandler：当队列和线程池都满了的时候，根据拒绝策略处理新任务。

   - AbortPolicy：默认的策略，直接抛出RejectedExecutionException。如果是比较关键的业务，推荐使用此拒绝策略，这样子在系统不能承载更大的并发量的时候，能够及时的通过异常发现。

   - DiscardPolicy：不处理，直接丢弃。建议是一些无关紧要的业务采用此策略。

   - DiscardOldestPolicy：将等待队列队首的任务丢弃，并执行当前任务。得根据实际业务是否允许丢弃老任务来认真衡量。

   - CallerRunsPolicy：由调用线程处理该任务CallerRunsPolicy：由调用线程处理该任务

### 线程池执行具体过程

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251113478.gif)

1. 当线程池里存活的线程数小于核心线程数corePoolSize时，这时对于一个新提交的任务，线程池会创建一个线程去处理任务。当线程池里面存活的线程数小于等于核心线程数corePoolSize时，线程池里面的线程会一直存活着，就算空闲时间超过了keepAliveTime，线程也不会被销毁，而是一直阻塞在那里一直等待任务队列的任务来执行。因为keepAliveTime只对非核心线程有效。
2. 当线程池里面存活的线程数已经等于corePoolSize了，这是对于一个新提交的任务，会被放进任务队列workQueue排队等待执行。
3. 当线程池里面存活的线程数已经等于corePoolSize了，并且任务队列也满了，假设maximumPoolSize>corePoolSize，这时如果再来新的任务，线程池就会继续创建新的线程来处理新的任务，直到线程数达到maximumPoolSize，就不会再创建了。
4. 如果当前的线程数达到了maximumPoolSize，并且任务队列也满了，如果还有新的任务过来，那就直接采用拒绝策略进行处理。默认的拒绝策略是抛出一个RejectedExecutionException异常。

### 线程池生命周期

| 线程池状态 | 状态释义                                                     |
| ---------- | ------------------------------------------------------------ |
| RUNNING    | 线程池被创建后的初始状态，能接受新提交的任务，并且也能处理阻塞队列中的任务 |
| SHUTDOWN   | 关闭状态，不再接受新提交的任务，但仍可以继续处理已进入阻塞队列中的任务 |
| STOP       | 会中断正在处理任务的线程，不能再接受新任务，也不继续处理队列中的任务 |
| TIDYING    | 所有的任务都已终止，workerCount(有效工作线程数)为0           |
| TERMINATED | 线程池彻底终止运行                                           |

 

Tips：千万不要把线程池的状态和线程的状态弄混了。补一张网上的线程状态图

![image-20240425111716736](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251117843.png)

Tips：当线程调用start()，线程在JVM中不一定立即执行，有可能要等待操作系统分配资源，此时为READY状态，当线程获得资源时进入RUNNING状态，才会真正开始执行。

### 线程池的核心组成

 ![image-20240425111726558](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251117645.png)

一个完整的线程池，应该包含以下几个核心部分：

- 任务提交：提供接口接收任务的提交；

- 任务管理：选择合适的队列对提交的任务进行管理，包括对拒绝策略的设置；

- 任务执行：由工作线程来执行提交的任务；

- 线程池管理：包括基本参数设置、任务监控、工作线程管理等

### 拒绝策略

- CallerRunsPolicy（在当前线程中执行）

- AbortPolicy（直接抛出RejectedExecutionException）

- DiscardPolicy（直接丢弃线程）

- DiscardOldestPolicy（丢弃一个未被处理的最久的线程，然后重试）

当没有显示指明拒绝策略时，默认使用AbortPolicy

![image-20240425111751711](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251117769.png)

![image-20240425111756100](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251117174.png)

### 任务执行机制

- 通过执行execute方法 该方法无返回值，为ThreadPoolExecutor自带方法，传入Runnable类型对象

![image-20240425111806429](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251118495.png)

- 通过执行submit方法 该方法返回值为Future对象，为抽象类AbstractExecutorService的方法，被ThreadPoolExecutor继承，其内部实现也是调用了接口类Executor的execute方法，通过上面的类图可以看到，该方法的实现依然是ThreadPoolExecutor的execute方法

![image-20240425111813578](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251118645.png)

![image-20240425111819130](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251118194.png)

#### execute()执行流程图

 ![image-20240425111822399](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251118470.png)



 

#### execute()源码

 ```java
     // 使用原子操作类AtomicInteger的ctl变量，前3位记录线程池的状态，后29位记录线程数
     private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
     // Integer的范围为[-2^31,2^31 -1], Integer.SIZE-3 =32-3= 29，用来辅助左移位运算
     private static final int COUNT_BITS = Integer.SIZE - 3;
     // 高三位用来存储线程池运行状态，其余位数表示线程池的容量
     private static final int CAPACITY   = (1 << COUNT_BITS) - 1;
 
     // 线程池状态以常量值被存储在高三位中
     private static final int RUNNING    = -1 << COUNT_BITS; // 线程池接受新任务并会处理阻塞队列中的任务
     private static final int SHUTDOWN   =  0 << COUNT_BITS; // 线程池不接受新任务，但会处理阻塞队列中的任务
     private static final int STOP       =  1 << COUNT_BITS; // 线程池不接受新的任务且不会处理阻塞队列中的任务，并且会中断正在执行的任务
     private static final int TIDYING    =  2 << COUNT_BITS; // 所有任务都执行完成，且工作线程数为0，将调用terminated方法
     private static final int TERMINATED =  3 << COUNT_BITS; // 最终状态，为执行terminated()方法后的状态
 
     // ctl变量的封箱拆箱相关的方法
     private static int runStateOf(int c)     { return c & ~CAPACITY; } // 获取线程池运行状态
     private static int workerCountOf(int c)  { return c & CAPACITY; } // 获取线程池运行线程数
     private static int ctlOf(int rs, int wc) { return rs | wc; } // 获取ctl对象
 ```

```java
public void execute(Runnable command) {
    if (command == null) // 任务为空，抛出NPE
        throw new NullPointerException();
        
    int c = ctl.get(); // 获取当前工作线程数和线程池运行状态（共32位，前3位为运行状态，后29位为运行线程数）
    if (workerCountOf(c) < corePoolSize) { // 如果当前工作线程数小于核心线程数
        if (addWorker(command, true)) // 在addWorker中创建工作线程并执行任务
            return;
        c = ctl.get();
    }
    
    // 核心线程数已满（工作线程数>核心线程数）才会走下面的逻辑
    if (isRunning(c) && workQueue.offer(command)) { // 如果当前线程池状态为RUNNING，并且任务成功添加到阻塞队列
        int recheck = ctl.get(); // 双重检查，因为从上次检查到进入此方法，线程池可能已成为SHUTDOWN状态
        if (! isRunning(recheck) && remove(command)) // 如果当前线程池状态不是RUNNING则从队列删除任务
            reject(command); // 执行拒绝策略
        else if (workerCountOf(recheck) == 0) // 当线程池中的workerCount为0时，此时workQueue中还有待执行的任务，则新增一个addWorker，消费workqueue中的任务
            addWorker(null, false);
    }
    // 阻塞队列已满才会走下面的逻辑
    else if (!addWorker(command, false)) // 尝试增加工作线程执行command
        // 如果当前线程池为SHUTDOWN状态或者线程池已饱和
        reject(command); // 执行拒绝策略
}
```

```java
private boolean addWorker(Runnable firstTask, boolean core) {
    retry: // 循环退出标志位
    for (;;) { // 无限循环
        int c = ctl.get();
        int rs = runStateOf(c); // 线程池状态

        // Check if queue empty only if necessary.
        if (rs >= SHUTDOWN && 
            ! (rs == SHUTDOWN && firstTask == null && ! workQueue.isEmpty()) // 换成更直观的条件语句
            // (rs != SHUTDOWN || firstTask != null || workQueue.isEmpty())
           )
           // 返回false的条件就可以分解为：
           //（1）线程池状态为STOP，TIDYING，TERMINATED
           //（2）线程池状态为SHUTDOWN，且要执行的任务不为空
           //（3）线程池状态为SHUTDOWN，且任务队列为空
            return false;

        // cas自旋增加线程个数
        for (;;) {
            int wc = workerCountOf(c); // 当前工作线程数
            if (wc >= CAPACITY ||
                wc >= (core ? corePoolSize : maximumPoolSize)) // 工作线程数>=线程池容量 || 工作线程数>=(核心线程数||最大线程数)
                return false;
            if (compareAndIncrementWorkerCount(c)) // 执行cas操作，添加线程个数
                break retry; // 添加成功，退出外层循环
            // 通过cas添加失败
            c = ctl.get();  
            // 线程池状态是否变化，变化则跳到外层循环重试重新获取线程池状态，否者内层循环重新cas
            if (runStateOf(c) != rs)
                continue retry;
            // else CAS failed due to workerCount change; retry inner loop
        }
    }
    // 简单总结上面的CAS过程：
    //（1）内层循环作用是使用cas增加线程个数，如果线程个数超限则返回false，否者进行cas
    //（2）cas成功则退出双循环，否者cas失败了，要看当前线程池的状态是否变化了
    //（3）如果变了，则重新进入外层循环重新获取线程池状态，否者重新进入内层循环继续进行cas

    // 走到这里说明cas成功，线程数+1，但并未被执行
    boolean workerStarted = false; // 工作线程调用start()方法标志
    boolean workerAdded = false; // 工作线程被添加标志
    Worker w = null;
    try {
        w = new Worker(firstTask); // 创建工作线程实例
        final Thread t = w.thread; // 获取工作线程持有的线程实例
        if (t != null) {
            final ReentrantLock mainLock = this.mainLock; // 使用全局可重入锁
            mainLock.lock(); // 加锁，控制并发
            try {
                // Recheck while holding lock.
                // Back out on ThreadFactory failure or if
                // shut down before lock acquired.
                int rs = runStateOf(ctl.get()); // 获取当前线程池状态

                // 线程池状态为RUNNING或者（线程池状态为SHUTDOWN并且没有新任务时）
                if (rs < SHUTDOWN ||
                    (rs == SHUTDOWN && firstTask == null)) {
                    if (t.isAlive()) // 检查线程是否处于活跃状态
                        throw new IllegalThreadStateException();
                    workers.add(w); // 线程加入到存放工作线程的HashSet容器，workers全局唯一并被mainLock持有
                    int s = workers.size();
                    if (s > largestPoolSize)
                        largestPoolSize = s;
                    workerAdded = true;
                }
            } finally {
                mainLock.unlock(); // finally块中释放锁
            }
            if (workerAdded) { // 线程添加成功
                t.start(); // 调用线程的start()方法
                workerStarted = true;
            }
        }
    } finally {
        if (! workerStarted) // 如果线程启动失败，则执行addWorkerFailed方法
            addWorkerFailed(w);
    }
    return workerStarted;
}
```

```java
private void addWorkerFailed(Worker w) {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        if (w != null)
            workers.remove(w); // 线程启动失败时，需将前面添加的线程删除
        decrementWorkerCount(); // ctl变量中的工作线程数-1
        tryTerminate(); // 尝试将线程池转变成TERMINATE状态
    } finally {
        mainLock.unlock();
    }
}
```

```java
final void tryTerminate() {
    for (;;) {
        int c = ctl.get();
        // 以下情况不会进入TERMINATED状态：
        //（1）当前线程池为RUNNING状态
        //（2）在TIDYING及以上状态
        //（3）SHUTDOWN状态并且工作队列不为空
        //（4）当前活跃线程数不等于0
        if (isRunning(c) ||
            runStateAtLeast(c, TIDYING) ||
            (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))
            return;
        if (workerCountOf(c) != 0) { // 工作线程数!=0
            interruptIdleWorkers(ONLY_ONE); // 中断一个正在等待任务的线程
            return;
        }

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 通过CAS自旋判断直到当前线程池运行状态为TIDYING并且活跃线程数为0
            if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                try {
                    terminated(); // 调用线程terminated()
                } finally {
                    ctl.set(ctlOf(TERMINATED, 0)); // 设置线程池状态为TERMINATED，工作线程数为0
                    termination.signalAll(); // 通过调用Condition接口的signalAll()唤醒所有等待的线程
                }
                return;
            }
        } finally {
            mainLock.unlock();
        }
        // else retry on failed CAS
    }
}
```



#### Worker源码

Worker是ThreadPoolExecutor类的内部类，此处只讲最重要的构造函数和run方法

```java
private final class Worker extends AbstractQueuedSynchronizer implements Runnable
{
    // 该worker正在运行的线程
    final Thread thread;
    
    // 将要运行的初始任务
    Runnable firstTask;
    
    // 每个线程的任务计数器
    volatile long completedTasks;

    // 构造方法   
    Worker(Runnable firstTask) {
        setState(-1); // 调用runWorker()前禁止中断
        this.firstTask = firstTask;
        this.thread = getThreadFactory().newThread(this); // 通过ThreadFactory创建一个线程
    }

    // 实现了Runnable接口的run方法
    public void run() {
        runWorker(this);
    }
    
    ... // 此处省略了其他方法
}
```

Worker实现了Runable接口，在调用start()方法候，实际执行的是run方法Worker实现了Runable接口，在调用start()方法候，实际执行的是run方法
```java
final void runWorker(Worker w) {
    Thread wt = Thread.currentThread();
    Runnable task = w.firstTask; // 获取工作线程中用来执行任务的线程实例
    w.firstTask = null;
    w.unlock(); // status设置为0，允许中断
    boolean completedAbruptly = true; // 线程意外终止标志
    try {
        // 如果当前任务不为空，则直接执行；否则调用getTask()从任务队列中取出一个任务执行
        while (task != null || (task = getTask()) != null) {
            w.lock(); // 加锁，保证下方临界区代码的线程安全
            // 如果状态值大于等于STOP且当前线程还没有被中断，则主动中断线程
            if ((runStateAtLeast(ctl.get(), STOP) ||
                 (Thread.interrupted() &&
                  runStateAtLeast(ctl.get(), STOP))) &&
                !wt.isInterrupted())
                wt.interrupt(); // 中断当前线程
            try {
                beforeExecute(wt, task); // 任务执行前的回调，空实现，可以在子类中自定义
                Throwable thrown = null;
                try {
                    task.run(); // 执行线程的run方法
                } catch (RuntimeException x) {
                    thrown = x; throw x;
                } catch (Error x) {
                    thrown = x; throw x;
                } catch (Throwable x) {
                    thrown = x; throw new Error(x);
                } finally {
                    afterExecute(task, thrown); // 任务执行后的回调，空实现，可以在子类中自定义
                }
            } finally {
                task = null; // 将循环变量task设置为null，表示已处理完成
                w.completedTasks++; // 当前已完成的任务数+1
                w.unlock();
            }
        }
        completedAbruptly = false;
    } finally {
        processWorkerExit(w, completedAbruptly);
    }
}
```



##### 从任务队列中取出一个任务

 ```java
 private Runnable getTask() {
     boolean timedOut = false; // 通过timeOut变量表示线程是否空闲时间超时了
     // 无限循环
     for (;;) {
         int c = ctl.get(); // 线程池信息
         int rs = runStateOf(c); // 线程池当前状态
 
         // 如果线程池状态>=SHUTDOWN并且工作队列为空 或 线程池状态>=STOP，则返回null，让当前worker被销毁
         if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
             decrementWorkerCount(); // 工作线程数-1
             return null;
         }
 
         int wc = workerCountOf(c); // 获取当前线程池的工作线程数
 
         // 当前线程是否允许超时销毁的标志
         // 允许超时销毁：当线程池允许核心线程超时 或 工作线程数>核心线程数
         boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;
 
         // 如果(当前线程数大于最大线程数 或 (允许超时销毁 且 当前发生了空闲时间超时))
         // 且(当前线程数大于1 或 阻塞队列为空)
         // 则减少worker计数并返回null
         if ((wc > maximumPoolSize || (timed && timedOut))
             && (wc > 1 || workQueue.isEmpty())) {
             if (compareAndDecrementWorkerCount(c))
                 return null;
             continue;
         }
 
         try {
             // 根据线程是否允许超时判断用poll还是take（会阻塞）方法从任务队列头部取出一个任务
             Runnable r = timed ?
                 workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                 workQueue.take();
             if (r != null)
                 return r; // 返回从队列中取出的任务
             timedOut = true;
         } catch (InterruptedException retry) {
             timedOut = false;
         }
     }
 }
 ```

总结一下哪些情况getTask()会返回null：

- 线程池状态为SHUTDOWN且任务队列为空

- 线程池状态为STOP、TIDYING、TERMINATED

- 线程池线程数大于最大线程数

- 线程可以被超时回收的情况下等待新任务超时

##### 工作线程退出

```java
private void processWorkerExit(Worker w, boolean completedAbruptly) {
    // 如果completedAbruptly为true则表示任务执行过程中抛出了未处理的异常
    // 所以还没有正确地减少worker计数，这里需要减少一次worker计数
    if (completedAbruptly) 
        decrementWorkerCount();

    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        // 把将被销毁的线程已完成的任务数累加到线程池的完成任务总数上
        completedTaskCount += w.completedTasks;
        workers.remove(w); // 从工作线程集合中移除该工作线程
    } finally {
        mainLock.unlock();
    }

    // 尝试结束线程池
    tryTerminate();

    int c = ctl.get();
    // 如果是RUNNING 或 SHUTDOWN状态
    if (runStateLessThan(c, STOP)) {
        // worker是正常执行完
        if (!completedAbruptly) {
            // 如果允许核心线程超时则最小线程数是0，否则最小线程数等于核心线程数
            int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
            // 如果阻塞队列非空，则至少要有一个线程继续执行剩下的任务
            if (min == 0 && ! workQueue.isEmpty())
                min = 1;
            // 如果当前线程数已经满足最小线程数要求，则不需要再创建替代线程
            if (workerCountOf(c) >= min)
                return; // replacement not needed
        }
        // 重新创建一个worker来代替被销毁的线程
        addWorker(null, false);
    }
}
```



### 线程池是如何保活和回收的

线程池的作用就是提高线程的利用率，需要线程时，可以直接从线程池中获取线程直接使用，而不用创建线程，那线程池中的线程，在没有任务执行时，是如何保活的呢?

在runWorker方法里，线程会循环getTask()获取阻塞队列中的任务。

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251113483.gif)

不断地的从阻塞队列中获取任务，主要调用的是workQueue.poll()方法或take()， 这两个方法都会阻塞式的从队列中获取元素，区别是poll()方法可以设置一个超时时间， take()不能设置超时时间，所以这也间接的使得线程池中的线程阻塞等待从而达到保活的效果。

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251113491.gif)

当然并不是线程池中的所有线程都需要一直保活，比如只有核心线程需要保活，非核心线程就不需要保活,那非核心线程是怎么回收的呢?

底层是这样的，当一个线程处理完当前任务后，就会开始去阻塞队列中获取任务，只不过，在调用poll或take方法之前， 会判断当前线程池中有多少个线程，如果多余核心线程数(也就是wc > corePlloSize)，那么timed为true，此时当前线程就会调用poll()并设置超时时间来获取阻塞队列中的任务,这样一旦时间到了还没有获取到任务，那么poll方法获取到的r就是null，返回给上一级，runWorker()里的getTask方法就获取到null了，此时while循环就会退出。那么就会调用processWorkerExit()方法，remove当前线程

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251113498.gif)

这里其实可以看到timed还有一个参数，allowCoreThreadTimeOut，这个主要是用来控制核心线程是否可以回收，默认是false，上面是讨论默认值false的情况，即核心线程不会超时。如果为true，工作线程可以全部销毁

实际上，虽然有核心线程数，但线程并没有区分是核心还是非核心，并不是先创建的就是核心，超过核心线程数后创建的就是非核心，最终保留哪些线程，完全随机。

### 线程池的关闭

shutdown方法会将线程池的状态设置为SHUTDOWN，线程池进入这个状态后，就拒绝再接受任务,然后会将剩余的任务全部执行完

```java
public void shutdown() {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        //检查是否可以关闭线程
        checkShutdownAccess();
        //设置线程池状态
        advanceRunState(SHUTDOWN);
        //尝试中断worker
        interruptIdleWorkers();
            //预留方法,留给子类实现
        onShutdown(); // hook for ScheduledThreadPoolExecutor
    } finally {
        mainLock.unlock();
    }
    tryTerminate();
}

private void interruptIdleWorkers() {
    interruptIdleWorkers(false);
}

private void interruptIdleWorkers(boolean onlyOne) {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        //遍历所有的worker
        for (Worker w : workers) {
            Thread t = w.thread;
            //先尝试调用w.tryLock(),如果获取到锁,就说明worker是空闲的,就可以直接中断它
            //注意的是,worker自己本身实现了AQS同步框架,然后实现的类似锁的功能
            //它实现的锁是不可重入的,所以如果worker在执行任务的时候,会先进行加锁,这里tryLock()就会返回false
            if (!t.isInterrupted() && w.tryLock()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                } finally {
                    w.unlock();
                }
            }
            if (onlyOne)
                break;
        }
    } finally {
        mainLock.unlock();
    }
}
```



shutdownNow做的比较绝，它先将线程池状态设置为STOP，然后拒绝所有提交的任务。最后中断左右正在运行中的worker,然后清空任务队列。

```java
public List<Runnable> shutdownNow() {
    List<Runnable> tasks;
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        //检测权限
        advanceRunState(STOP);
        //中断所有的worker
        interruptWorkers();
        //清空任务队列
        tasks = drainQueue();
    } finally {
        mainLock.unlock();
    }
    tryTerminate();
    return tasks;
}

private void interruptWorkers() {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        //遍历所有worker，然后调用中断方法
        for (Worker w : workers)
            w.interruptIfStarted();
    } finally {
        mainLock.unlock();
    }
}
```



## 线程池大小怎么设置？

首先应该明确线程池大小设置的目的是什么？其实就是为了**提高 CPU 的利用率**

> CPU利用率=CPU有效工作时间/CPU总的运行时间

如果线程池线程数量太小，当有大量请求需要处理，需要创建非核心线程池处理，导致系统响应比较慢，会影响用户体验，甚至会出现任务队列大量堆积任务导致OOM。

如果核心线程池数量太大，会导致其一直阻塞在那里等待任务队列的任务来执行，消耗内存。并且大量线程可能会同时抢占 CPU 资源，这样会导致大量的上下文切换，从而增加线程的执行时间，影响了执行效率。

### 根据线程数设置依据

最大线程数：原则上就是**性能最高线程数**，因为此时性能已经是最高，再设置比他大的线程数反而性能变低。极端情况下才会使用到最大线程数，正常情况下不应频繁出现超过核心线程数的创建。

核心线程数：基于性能考虑，及其他业务处理的最优效率考虑，估算平时的流量需要的线程数，设置核心线程数

阻塞队列：估算最大流量，设置阻塞队列长度

注意：需要通过压力测试来进行微调，只有经过压测的检验，才能最终保证的配置大小是准确的。

### 一般情况设置依据

一般用来计算核心线程数

#### CPU 密集型任务(N+1)：

这种任务消耗的主要是 CPU 资源，可以将线程数设置为 **N（CPU 核心数）+1**，多出来的一个线程是为了防止某些原因导致的线程阻塞（如IO操作，线程sleep，等待锁）而带来的影响。一旦某个线程被阻塞，释放了CPU资源，而在这种情况下多出来的一个线程就可以充分利用 CPU 的空闲时间。

#### I/O 密集型任务(2N)：

系统的大部分时间都在处理 IO 操作，此时线程可能会被阻塞，释放CPU资源，这时就可以将 CPU 交出给其它线程使用。因此在 IO 密集型任务的应用中，可以多配置一些线程，具体的计算方法：最佳线程数 = CPU核心数 * (1/CPU利用率) = CPU核心数 * (1 + (IO耗时/CPU耗时))，一般可设置为**2N**。

## ExecutorService 线程池实例

但是阿里为什么不推荐使用Executors来创建线程池，这是为了让写的同学更加明确线程池的运行规则，规避资源耗尽的风险。

### FixedThreadPool:

固定线程数的线程池。任何时间点，最多只有 nThreads 个线程处于活动状态执行任务。

```java
public static ExecutorService newFixedThreadPool(int nThreads) {
    return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
}
```

为什么不建议使用？

**使用无界队列 LinkedBlockingQueue**（队列容量为 Integer.MAX_VALUE），运行中的线程池不会拒绝任务，即不会调用RejectedExecutionHandler.rejectedExecution()方法。maxThreadPoolSize 是无效参数，故将它的值设置为与 coreThreadPoolSize 一致。**当添加任务的速度大于线程池处理任务的速度，可能会在队列堆积大量的请求，消耗很大的内存，甚至导致OOM。**

keepAliveTime 也是无效参数，设置为0L，因为此线程池里所有线程都是核心线程，核心线程不会被回收（除非设置了executor.allowCoreThreadTimeOut(true)）。

适用场景：适用于处理CPU密集型的任务，确保CPU在长期被工作线程使用的情况下，尽可能的少的分配线程，即适用执行长期的任务。需要注意的是，FixedThreadPool 不会拒绝任务，在任务比较多的时候会导致 OOM。

### SingleThreadExecutor

只有一个线程的线程池。

```java
public static ExecutionService newSingleThreadExecutor() {
    return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
}
```



为什么不建议使用？

**使用无界队列 LinkedBlockingQueue**。线程池只有一个运行的线程，新来的任务放入工作队列，线程处理完任务就循环从队列里获取任务执行。保证顺序的执行各个任务。

适用场景：适用于串行执行任务的场景，一个任务一个任务地执行。在任务比较多的时候也是会导致 OOM。

### CachedThreadPool

```java
public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
}
```

为什么不建议使用？

core是0，**最大线程数是Integer.MAX_VALUE**，因此当添加任务的速度大于线程池处理任务的速度，可能会创建大量的线程，极端情况下，这样会导致耗尽 cpu 和内存资源，甚至导致OOM。

使用没有容量的SynchronousQueue作为线程池工作队列，当线程池有空闲线程时，SynchronousQueue.offer(Runnable task)提交的任务会被空闲线程处理，否则会创建新的线程处理任务。

适用场景：用于并发执行大量短期的小任务。CachedThreadPool允许创建的线程数量为 Integer.MAX_VALUE ，可能会创建大量线程，从而导致 OOM。

### ScheduledThreadPoolExecutor

```java
public ScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS,
              new DelayedWorkQueue());
}
```

在给定的延迟后运行任务，或者定期执行任务。

为什么不建议使用？

**最大线程数是Integer.MAX_VALUE**，因此当添加任务的速度大于线程池处理任务的速度，可能会创建大量的线程，极端情况下，这样会导致耗尽 cpu 和内存资源，甚至导致OOM。

 











