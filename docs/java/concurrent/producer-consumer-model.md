---
title: 手写生产者消费者模型
category: Java
tag:
  - 并发编程
head:
  - - meta
    - name: keywords
      content: 线程,多线程,生产者消费者模型,阻塞队列,synchronized,lock,Condition
  - - meta
    - name: description
      content: 全网最全的Java并发编程知识点总结，让天下没有难学的八股文！
---

## 前言

生产者-消费者模式是一个十分经典的多线程并发协作模式，弄懂生产者-消费者问题能够让我们对并发编程的理解加深。

所谓的生产者-消费者，实际上包含了两类线程，一种是生产者线程用于生产数据，另一种是消费者线程用于消费数据，为了解耦生产者和消费者的关系，通常会采用共享的数据区域，就像是一个仓库，生产者生产数据之后直接放置在共享数据区中，并不需要关心消费者的行为；而消费者只需要从共享数据区中获取数据，不需要关心生产者的行为。

这个共享数据区域中应该具备这样的线程间并发协作功能：

1. 如果共享数据区已满的话，阻塞生产者继续生产数据；
2. 如果共享数据区为空的话，阻塞消费者继续消费数据；

在实现生产者消费者问题时，可以采用三种方式：
1. 使用 [BlockingQueue](https://www.seven97.top/java/collection/04-juc4-blockingqueue.html) 实现
2. 使用 [synchronized](https://www.seven97.top/java/concurrent/02-keyword1-synchronized.html)以及Object wait/notify 的消息通知机制；
3. 使用 Lock [Condition](https://www.seven97.top/java/concurrent/05-concurrenttools6-condition.html) 的 await/signal 消息通知机制；


## ## BlockingQueue 实现生产者-消费者

BlockingQueue 提供了可阻塞的插入和移除的方法。当队列容器已满，生产者线程会被阻塞，直到队列未满；当队列容器为空时，消费者线程会被阻塞，直至队列非空时为止。

有了这个队列，生产者就只需要关注生产，而不用管消费者的消费行为，更不用等待消费者线程执行完；消费者也只管消费，不用管生产者是怎么生产的，更不用等着生产者生产。

```java
public class ProductorConsumer {

    private static LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();

    public static void main(String[] args) {
        ExecutorService service = Executors.newFixedThreadPool(15);
        for (int i = 0; i < 5; i++) {
            service.submit(new Productor(queue));
        }
        for (int i = 0; i < 10; i++) {
            service.submit(new Consumer(queue));
        }
    }


    static class Productor implements Runnable {

        private BlockingQueue queue;

        public Productor(BlockingQueue queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Random random = new Random();
                    int i = random.nextInt();
                    System.out.println("生产者" + Thread.currentThread().getName() + "生产数据" + i);
                    queue.put(i);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class Consumer implements Runnable {
        private BlockingQueue queue;

        public Consumer(BlockingQueue queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Integer element = (Integer) queue.take();
                    System.out.println("消费者" + Thread.currentThread().getName() + "正在消费数据" + element);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
```

## synchronized 实现生产者-消费者

这其实也是手动实现阻塞队列的方式

```java
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CyclicBarrier;

public class MyBlockingQueue {

    //队列
    private final Queue<String> myQueue = new LinkedList<>();

    //最大长度
    private static final int MAXSIZE = 20;
    private static final int MINSIZE = 0;

    //获取队列长度
    public int getSize() {
        return myQueue.size();
    }

    //生产者
    public void push(String str) throws Exception {
        //拿到对象锁
        synchronized (myQueue) {

            //如果队列满了，则阻塞
            while (getSize() == MAXSIZE) {
                myQueue.wait();
            }

            myQueue.offer(str);
            System.out.println(Thread.currentThread().getName() + "放入元素" + str);

            //唤醒消费者线程，消费者和生产者自己去竞争锁
            myQueue.notify();
        }
    }

    //消费者
    public String pop() throws Exception {
        synchronized (myQueue) {
            String result = null;

            //队列为空则阻塞
            while (getSize() == MINSIZE) {
                myQueue.wait();
            }
            //先进先出
            result = myQueue.poll();

            System.out.println(Thread.currentThread().getName() + "取出了元素" + result);
            //唤醒生产者线程，消费者和生产者自己去竞争锁
            myQueue.notify();

            return result;
        }
    }

    public static void main(String args[]) {

        MyBlockingQueue myBlockingQueue = new MyBlockingQueue();

        //两个线程，都执行完成了打印
        CyclicBarrier barrier = new CyclicBarrier(2, () -> {
            System.out.println("生产结束，下班了，消费者明天再来吧！");
        });

        //生产者线程
        new Thread(() -> {
            //50个辛勤的生产者循环向队列中添加元素
            try {
                for (int i = 0; i < 50; i++) {
                    myBlockingQueue.push("——" + i);
                }
                //生产完了
                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "生产者").start();

        //消费者线程
        new Thread(() -> {
            //50个白拿的消费者疯狂向队列中获取元素
            try {
                for (int j = 0; j < 50; j++) {
                    myBlockingQueue.pop();
                }
                //消费完了
                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "消费者").start();

    }
}
```



## Condition 实现生产者-消费者

```java
public class BoundedQueue {

    /**
     * 生产者容器
     */
    private LinkedList<Object> buffer;
    /**
     * //容器最大值是多少
     */
    private int maxSize;
    
    private Lock lock;
    
    /**
     * 满了
     */
    private Condition fullCondition;
    
    /**
     * 不满
     */
    private Condition notFullCondition;

    BoundedQueue(int maxSize) {
        this.maxSize = maxSize;
        buffer = new LinkedList<Object>();
        lock = new ReentrantLock();
        fullCondition = lock.newCondition();
        notFullCondition = lock.newCondition();
    }

    /**
     * 生产者
     *
     * @param obj
     * @throws InterruptedException
     */
    public void put(Object obj) throws InterruptedException {
        //获取锁
        lock.lock();
        try {
            while (maxSize == buffer.size()) {
                //满了，添加的线程进入等待状态
                notFullCondition.await();
            }
            buffer.add(obj);
            //通知
            fullCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 消费者
     *
     * @return
     * @throws InterruptedException
     */
    public Object get() throws InterruptedException {
        Object obj;
        lock.lock();
        try {
            while (buffer.size() == 0) {
                //队列中没有数据了 线程进入等待状态
                fullCondition.await();
            }
            obj = buffer.poll();
            //通知
            notFullCondition.signal();
        } finally {
            lock.unlock();
        }
        return obj;
    }

}
```


## 生产者-消费者模式的应用场景

生产者-消费者模式一般用于将生产数据的一方和消费数据的一方分割开来，将生产数据与消费数据的过程解耦开来。

### Excutor 任务执行框架

通过将任务的提交和任务的执行解耦开来，提交任务的操作相当于生产者，执行任务的操作相当于消费者。

例如使用 Excutor 构建 Web 服务器，用于处理线程的请求：生产者将任务提交给线程池，线程池创建线程处理任务，如果需要运行的任务数大于线程池的基本线程数，那么就把任务扔到阻塞队列（通过线程池+阻塞队列的方式比只使用一个阻塞队列的效率高很多，因为消费者能够处理就直接处理掉了，不用每个消费者都要先从阻塞队列中取出任务再执行）

### 消息中间件 MQ

双十一的时候，会产生大量的订单，那么不可能同时处理那么多的订单，需要将订单放入一个队列里面，然后由专门的线程处理订单。

这里用户下单就是生产者，处理订单的线程就是消费者；再比如 12306 的抢票功能，先由一个容器存储用户提交的订单，然后再由专门处理订单的线程慢慢处理，这样可以在短时间内支持高并发服务。

### 任务的处理时间比较长的情况下

比如上传附件并处理，那么这个时候可以将用户上传和处理附件分成两个过程，用一个队列暂时存储用户上传的附件，然后立刻返回用户上传成功，然后有专门的线程处理队列中的附件。

生产者-消费者模式的优点：
- 解耦：将生产者类和消费者类进行解耦，消除代码之间的依赖性，简化工作负载的管理
- 复用：通过将生产者类和消费者类独立开来，对生产者类和消费者类进行独立的复用与扩展
- 调整并发数：由于生产者和消费者的处理速度是不一样的，可以调整并发数，给予慢的一方多的并发数，来提高任务的处理速度
- 异步：对于生产者和消费者来说能够各司其职，生产者只需要关心缓冲区是否还有数据，不需要等待消费者处理完；对于消费者来说，也只需要关注缓冲区的内容，不需要关注生产者，通过异步的方式支持高并发，将一个耗时的流程拆成生产和消费两个阶段，这样生产者因为执行 put 的时间比较短，可以支持高并发
- 支持分布式：生产者和消费者通过队列进行通讯，所以不需要运行在同一台机器上，在分布式环境中可以通过 redis 的 list 作为队列，而消费者只需要轮询队列中是否有数据。同时还能支持集群的伸缩性，当某台机器宕掉的时候，不会导致整个集群宕掉

