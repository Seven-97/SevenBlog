---
title: Guava - 并发编程
category: 开发工具
tag:
 - 工具类库
 - Guava
---



## MoreExecutors

### directExecutor

```java
ExecutorService executor = Executors.newSingleThreadExecutor();
SettableFuture<Integer> future0 = SettableFuture.create();
// 使用其他线程去 set 对应的结果。
executor.submit(() -> {
    future0.set(1);
});

Futures.addCallback(future0, new FutureCallback<>() {
    @Override
    public void onSuccess(Integer result) {
        // main线程执行的
        System.out.println("result=" + result + "线程名：" + Thread.currentThread().getName());//main
    }

    @Override
    public void onFailure(Throwable t) {
    }
}, MoreExecutors.directExecutor());
```

执行 callback 的线程池这里指定为 `MoreExecutors#directExecutor` ，那么这里执行打印 result 的线程是**主线程**

于 `MoreExecutors#directExecutor` ，可以看到定义是这样的：

```
public final class MoreExecutors {
    // 省略了类内其他成员
    public static Executor directExecutor() {
        return DirectExecutor.INSTANCE;
  }
}
```

以及

```
@GwtCompatible
@ElementTypesAreNonnullByDefault
enum DirectExecutor implements Executor {
  INSTANCE;

  @Override
  public void execute(Runnable command) {
    command.run();
  }

  @Override
  public String toString() {
    return "MoreExecutors.directExecutor()";
  }
}
```

`MoreExecutors#directExecutor` 其实是一个假的线程池，表示直接执行。



再看下面这个例子：

```java
ExecutorService executor = Executors.newSingleThreadExecutor();
SettableFuture<Integer> future0 = SettableFuture.create();
// 使用其他线程去 set 对应的结果。
executor.submit(() -> {
    // 增加线程 sleep 的逻辑。
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    future0.set(1);
});

Futures.addCallback(future0, new FutureCallback<>() {
    @Override
    public void onSuccess(Integer result) {
        // 此时就会被 executor 的线程执行
        System.out.println("result=" + result + "线程名：" + Thread.currentThread().getName());//此时还未打印出来，主线程就结束了
    }

    @Override
    public void onFailure(Throwable t) {
    }
}, MoreExecutors.directExecutor());
```

那么这里清晰了：

- 如果 future 已经完成，那么 `MoreExecutor#directExecutor` 表示当前线程；
- 如果 future 未完成，那么 `MoreExecutor#directExecutor` 就是未来完成 future 的线程。

因此其实具体执行回调的线程某种程度上是不确定的。





## ListenableFuture

### 引言

jdk原生的future已经提供了异步操作，但是不能直接回调。guava对future进行了增强，核心接口就是ListenableFuture。jdk8从guava中吸收了精华新增的类[CompletableFuture](https://www.seven97.top/java/concurrent/05-concurrenttools7-completablefuture.html)，也可以直接看这个类的学习。



JUC 的 Future 接口提供了一种异步获取任务执行结果的机制，表示一个异步计算的结果。

```java
ExecutorService executor = Executors.newFixedThreadPool(1);
Future<String> future = executor.submit(() -> {
    // 执行异步任务，返回一个结果
    return "Task completed";
});
// Blocked
String result = future.get();
```

Executor 实际返回的是实现类 FutureTask，它同时实现了 Runnable 接口，因此可以手动创建异步任务。

```java
FutureTask<String> futureTask = new FutureTask<>(new Callable<String>() {
    @Override
    public String call() throws Exception {
        return "Hello";
    }
});
        
new Thread(futureTask).start();
System.out.println(futureTask.get());
```

而 Guava 提供的 ListenableFuture 更进一步，允许注册回调，在任务完成后自动执行，实际也是使用它的实现类 ListenableFutureTask。

```java
// 装饰原始的线程池
ListeningExecutorService listeningExecutorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
ListenableFuture<String> future = listeningExecutorService.submit(() -> {
    // int i = 1 / 0;
    return "Hello";
});

// 添加回调 1
Futures.addCallback(future, new FutureCallback<String>() {
    // 任务成功时的回调
    @Override
    public void onSuccess(String result) {
        System.out.println(result);
    }

    // 任务失败时的回调
    @Override
    public void onFailure(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }
}, listeningExecutorService);

// 添加回调 2
future.addListener(new Runnable() {
    @Override
    public void run() {
        System.out.println("Done");
    }
}, listeningExecutorService);
```



### 回调源码剖析

先看下ListenableFuture接口定义：

```java
public interface ListenableFuture<V> extends Future<V> {
    void addListener(Runnable listener, Executor executor);
}
```

可以看到，这个接口在Future接口的基础上增加了addListener方法，允许我们注册回调函数。当然，在编程时可能不会直接使用这个接口，因为这个接口只能传Runnable实例。



#### addListener方法

```java
@Override
  public void addListener(Runnable listener, Executor executor) {
    checkNotNull(listener, "Runnable was null.");
    checkNotNull(executor, "Executor was null.");
    // Checking isDone and listeners != TOMBSTONE may seem redundant, but our contract for
    // addListener says that listeners execute 'immediate' if the future isDone(). However, our
    // protocol for completing a future is to assign the value field (which sets isDone to true) and
    // then to release waiters, followed by executing afterDone(), followed by releasing listeners.
    // That means that it is possible to observe that the future isDone and that your listeners
    // don't execute 'immediately'.  By checking isDone here we avoid that.
    // A corollary to all that is that we don't need to check isDone inside the loop because if we
    // get into the loop we know that we weren't done when we entered and therefore we aren't under
    // an obligation to execute 'immediately'.
    if (!isDone()) {
      Listener oldHead = listeners; // 获取当前监听器的头结点
      if (oldHead != Listener.TOMBSTONE) {// 检查当前的头节点是否是TOMBSTONE。TOMBSTONE用来表示监听器列表不再接受新的监听器，通常是因为Future已经完成。 
        Listener newNode = new Listener(listener, executor);//通过这个listener新增一个一个节点，节点中包含executor
        do {
          newNode.next = oldHead;//将newNode.next指向当前头结点，此时newNode就是头结点
          if (ATOMIC_HELPER.casListeners(this, oldHead, newNode)) {//检查头节点是否更新成功
            return;//更新成功就可以返回了
          }
          oldHead = listeners; // 重新执行 头插法
        } while (oldHead != Listener.TOMBSTONE);// 如果头节点变成了TOMBSTONE，则退出循环；并且
      }
    }
    // If we get here then the Listener TOMBSTONE was set, which means the future is done, call
    // the listener.
    executeListener(listener, executor);//执行到这里意味着监听器TOMBSTONE就设置好了，也就是future已经完成，可以直接调用监听器
  }
```

这里其实就是在添加listener的方法中首先检查Future是否已经完成：

- 如果Future已经完成，那么就没有必要添加新的监听器，直接executeListener。
- 如果future没有完成，那么会新建一个Listener节点，并插入到链表头部（Listener就是一个链表）



如果已经完成，会直接执行executeListner 方法

```java
private static void executeListener(Runnable runnable, Executor executor) {
  try {
    executor.execute(runnable);//直接使用listener拥有的线程executor执行
  } catch (Exception e) { // sneaky checked exception
    // Log it and keep going -- bad runnable and/or executor. Don't punish the other runnables if
    // we're given a bad one. We only catch Exception because we want Errors to propagate up.
    log.get()
        .log(
            Level.SEVERE,
            "RuntimeException while executing runnable "
                + runnable
                + " with executor "
                + executor,
            e);
  }
}
```



> 那么如果没有完成呢，在listener链表中的什么时候会执行？看后续的回调函数的触发内容



#### addCallback方法

Futures类还提供了另一个回调方法：addCallback方法

```java
public static <V> void addCallback(
  final ListenableFuture<V> future,
  final FutureCallback<? super V> callback,
  Executor executor) {
	Preconditions.checkNotNull(callback);
	future.addListener(new CallbackListener<V>(future, callback), executor);//调用了addListener方法
}
```

这里调用了ListenableFuture接口的addListener方法，传入了一个CallbackListener实例。而这个实例由需要传入future和一个Callback实例，所以这个回调是可以拿到返回值的。本质上是guava基于Runnable封了一个回调接口。

看下这个CallbackListener接口：

```java
private static final class CallbackListener<V> implements Runnable {
    final Future<V> future;
    final FutureCallback<? super V> callback;
 
    CallbackListener(Future<V> future, FutureCallback<? super V> callback) {
      this.future = future;
      this.callback = callback;
    }
 
    @Override
    public void run() {//回调时的逻辑
      if (future instanceof InternalFutureFailureAccess) {
        Throwable failure =
            InternalFutures.tryInternalFastPathGetFailure((InternalFutureFailureAccess) future);
        if (failure != null) {
          callback.onFailure(failure);
          return;
        }
      }
      final V value;
      try {
        value = getDone(future);//获取返回值
      } catch (ExecutionException e) {
        callback.onFailure(e.getCause());//如果发生了异常，则会调用onFailure方法通知异常
        return;
      } catch (RuntimeException | Error e) {
        callback.onFailure(e);//如果发生了异常，则会调用onFailure方法通知异常
        return;
      }
      callback.onSuccess(value);//将返回值调用FutureCallback实例的onSuccess方法执行注册的回调逻辑
    }
}
```

> 那么这个回调函数什么时候会执行？看后续的回调函数的触发内容



#### 回调函数的触发

那么这些回调方法什么时候会触发呢？

```java
private static void complete(AbstractFuture<?> param) {
    // Declare a "true" local variable so that the Checker Framework will infer nullness.
    AbstractFuture<?> future = param;//获取future

    Listener next = null;
    outer:
    while (true) {
      future.releaseWaiters();//通知所有执行的方法
      // We call this before the listeners in order to avoid needing to manage a separate stack data
      // structure for them.  Also, some implementations rely on this running prior to listeners
      // so that the cleanup work is visible to listeners.
      // afterDone() should be generally fast and only used for cleanup work... but in theory can
      // also be recursive and create StackOverflowErrors
      future.afterDone();
      // push the current set of listeners onto next
      next = future.clearListeners(next);//反转listener链表
      future = null;
      while (next != null) {
        Listener curr = next;//获取当前listener
        next = next.next;
        /*
         * requireNonNull is safe because the listener stack never contains TOMBSTONE until after
         * clearListeners.
         */
        Runnable task = requireNonNull(curr.task);
        if (task instanceof SetFuture) {
          SetFuture<?> setFuture = (SetFuture<?>) task;
          // We unwind setFuture specifically to avoid StackOverflowErrors in the case of long
          // chains of SetFutures
          // Handling this special case is important because there is no way to pass an executor to
          // setFuture, so a user couldn't break the chain by doing this themselves.  It is also
          // potentially common if someone writes a recursive Futures.transformAsync transformer.
          future = setFuture.owner;
          if (future.value == setFuture) {
            Object valueToSet = getFutureValue(setFuture.future);
            if (ATOMIC_HELPER.casValue(future, setFuture, valueToSet)) {
              continue outer;
            }
          }
          // other wise the future we were trying to set is already done.
        } else {
          /*
           * requireNonNull is safe because the listener stack never contains TOMBSTONE until after
           * clearListeners.
           */
          executeListener(task, requireNonNull(curr.executor));// 交给listener拥有的线程池进行处理
        }
      }
      break;
    }
  }
```



那哪些方法会来调用这个complete方法呢？

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407171818459.png)





## Service

Guava 的 Service 框架是一个用于管理服务生命周期的轻量级框架。它提供了一个抽象类 AbstractService 和一个接口 Service，可以通过继承 AbstractService 或者直接实现 Service 接口来创建自定义的服务，并使用 ServiceManager 来管理这些服务的生命周期。

```java
public class MyService extends AbstractService {
    @Override
    protected void doStart() {
        // 在这里执行启动服务的逻辑
        System.out.println("MyService is starting...");
        notifyStarted();
    }
    
    @Override
    protected void doStop() {
        // 在这里执行停止服务的逻辑
        System.out.println("MyService is stopping...");
        notifyStopped();
    }
}

@Test
public void testService() {
    Service service = new MyService();
    ServiceManager serviceManager = new ServiceManager(List.of(service));
    
    serviceManager.startAsync().awaitHealthy();
    
    // 主线程逻辑
    
    serviceManager.stopAsync().awaitStopped();
}
```



