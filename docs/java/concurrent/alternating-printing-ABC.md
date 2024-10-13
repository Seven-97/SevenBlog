---
title: 多线程交替顺序打印ABC的多种方式
category: Java
tags:
  - 并发编程
head:
  - - meta
    - name: keywords
      content: 线程,多线程,交替打印ABC,lock,Condition,AtomicInteger
  - - meta
    - name: description
      content: 全网最全的Java并发编程知识点总结，让天下没有难学的八股文！
---

面试题：有 3 个独立的线程，一个只会输出 A，一个只会输出 B，一个只会输出 C，在三个线程启动的情况下，请用合理的方式让他们按顺序打印 ABCABC。

## 使用lock，Condition

```java
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ABC {

    //可重入锁
    private final static Lock lock = new ReentrantLock();
    //判断是否执行：1表示应该A执行，2表示应该B执行，3表示应该C执行
    private static int state = 1;
    //condition对象
    private static Condition a = lock.newCondition();
    private static Condition b = lock.newCondition();
    private static Condition c = lock.newCondition();

    public static void printA() {
        //通过循环，hang住线程
        for (int i = 0; i < 10; i++) {
            try {
                //获取锁
                lock.lock();
                //并发情况下，不能用if，要用循环判断等待条件，避免虚假唤醒
                while (state != 1) {
                    a.await();
                }
                System.out.print("A");
                state = 2;
                b.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                //要保证不执行的时候，锁能释放掉
                lock.unlock();
            }
        }
    }

    public static void printB() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            try {
                lock.lock();
                //获取到锁，应该执行
                while (state != 2) {
                    b.await();
                }
                System.out.print("B");
                state = 3;
                c.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    public static void printC() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            try {
                lock.lock();
                while (state != 3) {
                    c.await();
                }
                //获取到锁，应该执行
                System.out.print("C");
                state = 1;
                a.signal();
            } finally {
                lock.unlock();
            }
        }
    }


    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ABC.printA();
            }
        }, "A").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ABC.printB();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "B").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ABC.printC();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "C").start();
    }

}
```



## 使用AtomicInteger

```java
import java.util.concurrent.atomic.AtomicInteger;

public class ABC2 {
    private static AtomicInteger state = new AtomicInteger(1);

    public static void printA() {
        for (int i = 0; i < 10; i++) {
            while (true) {
                if (state.get() == 1) {
                    System.out.print("A");
                    state.incrementAndGet();
                    break;
                }
            }
        }
    }

    public static void printB() {
        for (int i = 0; i < 10; i++) {
            while (true) {
                if (state.get() == 2) {
                    System.out.print("B");
                    state.incrementAndGet();
                    break;
                }
            }
        }
    }

    public static void printC() {
        for (int i = 0; i < 10; i++) {
            while (true) {
                if (state.get() == 3) {
                    System.out.print("C");
                    state.set(1);
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ABC2.printA();
            }
        }, "A").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                ABC2.printB();
            }
        }, "B").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                ABC2.printC();
            }
        }, "C").start();
    }

}
```


## 使用LockSupprt

```java
public class ABC3 {
    private static Thread t1, t2, t3;

    public static void main(String[] args) {
        t1 = new Thread(() -> {
            for (int i = 0; i < 2; i++) {
                LockSupport.park();
                System.out.print("A");
                LockSupport.unpark(t2);
            }
        });

        t2 = new Thread(() -> {
            for (int i = 0; i < 2; i++) {
                LockSupport.park();
                System.out.print("B");
                LockSupport.unpark(t3);
            }
        });

        t3 = new Thread(() -> {
            for (int i = 0; i < 2; i++) {
                LockSupport.park();
                System.out.print("C");
                LockSupport.unpark(t1);
            }
        });

        t1.start();
        t2.start();
        t3.start();

        // 主线程稍微等待一下，确保其他线程已经启动并且进入park状态。
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 启动整个流程
        LockSupport.unpark(t1);
    }
}
```






