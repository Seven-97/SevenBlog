---
title: 并发工具 - CompletableFuture详解
category: Java
tag:
 - 并发编程
---









## 前言

CompletableFuture是jdk8的新特性。`CompletableFuture`的实现与使用上，处处体现出了**函数式异步编程**的味道。一个`CompletableFuture`对象可以被一个环节接一个环节的处理、也可以对两个或者多个`CompletableFuture`进行组合处理或者等待结果完成。通过对`CompletableFuture`各种方法的合理使用与组合搭配，可以在很多的场景都可以应付自如。

CompletableFuture实现了CompletionStage接口和Future接口，前者是对后者的一个扩展，增加了异步会点、流式处理、多个Future组合处理的能力，使Java在处理多任务的协同工作时更加顺畅便利。



假设现在需求如下：
从网上查询某个产品的最低价格，例如可以从淘宝、京东、拼多多去获取某个商品的价格、优惠金额，并计算出实际的付款金额，最终返回价格最低的价格信息。

这里假设每个平台获取原价格与优惠券的接口已经实现、且都是需要调用HTTP接口查询的耗时操作，接口每个耗时`1s`左右。

根据需求理解，可以很自然的写出对应实现代码：

```java
public int getCheapestPlatAndPrice(String product){
    int taoBaoPrice = computeRealPrice(HttpRequestMock.getTaoBaoPrice(product), HttpRequestMock.getTaoBaoDiscounts(product));
    int jingDongPrice = computeRealPrice(HttpRequestMock.getJingDongPrice(product), HttpRequestMock.getJingDongDiscounts(product));
    int pinDuoDuoPrice = computeRealPrice(HttpRequestMock.getPinDuoDuoPrice(product), HttpRequestMock.getPinDuoDuoDiscounts(product));

    // 计算并选出实际价格最低的平台
    return Stream.of(taoBaoPrice, jingDongPrice, pinDuoDuoPrice).min(Comparator.comparingInt(p - > p)).get();
}
```

运行测试下：

```ini
14:58:32.330228700[main]获取淘宝上iphone16的价格完成： 5199
14:58:33.351948100[main]获取淘宝上iphone16的折扣价格完成： 200
14:58:33.352933400[main]计算实际价格完成： 4999
14:58:34.364138900[main]获取京东上iphone16的价格完成： 5299
14:58:35.377258800[main]获取京东上iphone16的折扣价格完成： 150
14:58:35.378257300[main]计算实际价格完成： 5149
14:58:36.392813800[main]获取拼多多上iphone16的价格完成： 5399
14:58:37.405863200[main]获取拼多多上iphone16的折扣价格完成： 99
14:58:37.406712600[main]计算实际价格完成： 5300
4999
耗时：6142ms
```

结果符合预期，功能正常，但是耗时较长。试想一下，假如你在某个APP操作需要等待6s才返回最终计算结果，那不得直接摔手机？



梳理下代码的实现思路：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409111511884.jpg)



可以知道所有的环节都是`串行实现的`的，由于每个查询接口的耗时都是1s，因此每个环节耗时加到一起，接口总耗时超过6s。

但实际上，每个平台之间的操作是**互不干扰**的，那其实就可以通过`多线程`的方式，同时去分别执行各个平台的逻辑处理，最后将各个平台的结果汇总到一起比对得到最低价格。

所以整个执行过程会变成如下的效果：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409111515372.jpg)



因此为了提升性能，可以采用**线程池**来负责多线程的处理操作，因为需要得到各个子线程处理的结果，所以需要使用 `Future`来实现：

```java
public Integer getCheapestPlatAndPrice2(String product) {
    Future <Integer> taoBaoFuture = threadPool.submit(() -> computeRealPrice(HttpRequestMock.getTaoBaoPrice(product), HttpRequestMock.getTaoBaoDiscounts(product)));
    Future <Integer> jingDongFuture = threadPool.submit(() -> computeRealPrice(HttpRequestMock.getJingDongPrice(product), HttpRequestMock.getJingDongDiscounts(product)));
    Future <Integer> pinDuoDuoFuture = threadPool.submit(() -> computeRealPrice(HttpRequestMock.getPinDuoDuoPrice(product), HttpRequestMock.getPinDuoDuoDiscounts(product)));

    // 等待所有线程结果都处理完成，然后从结果中计算出最低价
    return Stream.of(taoBaoFuture, jingDongFuture, pinDuoDuoFuture)
        .map(price - > {
            try {
                return price.get();
            } catch (Exception e) {
                return null;
            }
        })
        .min(Comparator.comparingInt(p - > p))
        .get();
}
```

上述代码中，将三个不同平台对应的`Callable`函数逻辑放入到`ThreadPool`中去执行，返回`Future`对象，然后再逐个通过`Future.get()`接口**阻塞**获取各自平台的结果，最后经比较处理后返回最低价信息。

执行代码，可以看到执行结果与过程如下：

```ini
15:19:25.793891500[pool-1-thread-3]获取拼多多上iphone16的价格完成： 5399
15:19:25.793891500[pool-1-thread-2]获取京东上iphone16的价格完成： 5299
15:19:25.794891500[pool-1-thread-1]获取淘宝上iphone16的价格完成： 5199
15:19:26.816140300[pool-1-thread-2]获取京东上iphone16的折扣价格完成： 150
15:19:26.816140300[pool-1-thread-3]获取拼多多上iphone16的折扣价格完成： 99
15:19:26.816923600[pool-1-thread-3]计算实际价格完成： 5300
15:19:26.816923600[pool-1-thread-2]计算实际价格完成： 5149
15:19:26.817921500[pool-1-thread-1]获取淘宝上iphone16的折扣价格完成： 200
15:19:26.820923400[pool-1-thread-1]计算实际价格完成： 4999
4999
耗时：2085ms
```

接口总耗时从`6s`下降到了`2s`，效果还是很显著的。但是，是否还能再压缩一些呢？



基于上面按照平台拆分并行处理的思路继续推进，我们可以看出每个平台内的处理逻辑其实可以分为3个主要步骤：

1. 获取原始价格（耗时操作）
2. 获取折扣优惠（耗时操作）
3. 得到原始价格和折扣优惠之后，计算实付价格

这3个步骤中，其实第1、2两个耗时操作也是相对独立的，如果也能并行处理的话，响应时长上应该也能继续缩短，即如下的处理流程：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409111524403.jpg)

这里当然也可以继续使用上面提到的`线程池+Future`的方式，但`Future`在应对并行结果组合以及后续处理等方面显得力不从心，**弊端**明显：

> 代码写起来会**非常拖沓**：先封装`Callable`函数放到线程池中去执行查询操作，然后分三组`阻塞等待`结果并计算出各自结果，最后再`阻塞等待`价格计算完成后汇总得到最终结果。



说到这里呢，就需要`CompletableFuture`登场了，`CompletableFuture`可以很轻松的来完成任务的并行处理，以及各个并行任务结果之间的组合再处理等操作。使用`CompletableFuture`编写实现代码如下：

```java
public Integer getCheapestPlatAndPrice3(String product) {
    CompletableFuture <Integer> taoBao = CompletableFuture.supplyAsync(() -> HttpRequestMock.getTaoBaoPrice(product)).thenCombine(CompletableFuture.supplyAsync(() -> HttpRequestMock.getTaoBaoDiscounts(product)), this::computeRealPrice);
    CompletableFuture <Integer> jingDong = CompletableFuture.supplyAsync(() -> HttpRequestMock.getJingDongPrice(product)).thenCombine(CompletableFuture.supplyAsync(() -> HttpRequestMock.getJingDongDiscounts(product)), this::computeRealPrice);
    CompletableFuture <Integer> pinDuoDuo = CompletableFuture.supplyAsync(() -> HttpRequestMock.getPinDuoDuoPrice(product)).thenCombine(CompletableFuture.supplyAsync(() -> HttpRequestMock.getPinDuoDuoDiscounts(product)), this::computeRealPrice);

    // 排序并获取最低价格
    return Stream.of(taoBao, jingDong, pinDuoDuo)
        .map(CompletableFuture::join)
        .min(Comparator.comparingInt(p - > p))
        .get();
}
```

看下执行结果符合预期，而接口耗时则降到了`1s`（因为依赖的每一个查询实际操作的接口耗时都是模拟的1s，所以这个结果已经算是此复合接口能达到的极限值了）。

```ini
15:29:04.911516600[ForkJoinPool.commonPool-worker-1]获取淘宝上iphone16的价格完成： 5199
15:29:04.911516600[ForkJoinPool.commonPool-worker-4]获取京东上iphone16的折扣价格完成： 150
15:29:04.911516600[ForkJoinPool.commonPool-worker-2]获取淘宝上iphone16的折扣价格完成： 200
15:29:04.911516600[ForkJoinPool.commonPool-worker-3]获取京东上iphone16的价格完成： 5299
15:29:04.911516600[ForkJoinPool.commonPool-worker-5]获取拼多多上iphone16的价格完成： 5399
15:29:04.911516600[ForkJoinPool.commonPool-worker-6]获取拼多多上iphone16的折扣价格完成： 99
15:29:04.924568[ForkJoinPool.commonPool-worker-2]计算实际价格完成： 4999
15:29:04.924568[ForkJoinPool.commonPool-worker-3]计算实际价格完成： 5149
15:29:04.924568[ForkJoinPool.commonPool-worker-6]计算实际价格完成： 5300
4999
耗时：1071ms
```

这里**CompletableFuture**执行时所使用的默认线程池是[`ForkJoinPool`](https://www.seven97.top/java/concurrent/04-threadpool4-forkjoin.html)。



## Future与CompletableFuture

首先，先来理一下Future与CompletableFuture之间的关系。

### Future

如果接触过多线程相关的概念，那`Future`应该不会陌生，早在**Java5**中就已经存在了。

该如何理解`Future`呢？举个生活中的例子：

> 你去咖啡店点了一杯咖啡，然后服务员会给你一个订单小票。 当服务员在后台制作咖啡的时候，你并没有在店里等待，而是出门到隔壁甜品店又买了个面包。 当面包买好之后，你回到咖啡店，拿着订单小票去取咖啡。 取到咖啡后，你边喝咖啡边把面包吃了……嗝~

是不是很熟悉的生活场景？ 对比到我们多线程异步编程的场景中，咖啡店的订单小票其实就是Future，通过Future可以让稍后适当的时候可以获取到对应的异步执行线程中的执行结果。

上面的场景，我们翻译为代码实现逻辑：

```java
public void buyCoffeeAndOthers() throws ExecutionException, InterruptedException {
    goShopping();
    // 子线程中去处理做咖啡这件事，返回future对象
    Future<Coffee> coffeeTicket = threadPool.submit(this::makeCoffee);
    // 主线程同步去做其他的事情
    Bread bread = buySomeBread();
    // 主线程其他事情并行处理完成，阻塞等待获取子线程执行结果
    Coffee coffee = coffeeTicket.get();
    // 子线程结果获取完成，主线程继续执行
    eatAndDrink(bread, coffee);
}
```

Future相关的了解可以看这篇文章：[FutureTask](https://www.seven97.top/java/concurrent/04-threadpool3-futuretask.html)是Future的基础实现



### CompletableFuture

Future在应对一些简单且相互独立的异步执行场景很便捷，但是在一些复杂的场景，比如同时需要多个有依赖关系的异步独立处理的时候，或者是一些类似流水线的异步处理场景时，就显得力不从心了。比如：

- 同时执行多个并行任务，等待最快的一个完成之后就可以继续往后处理
- 多个异步任务，每个异步任务都需要依赖前一个异步任务执行的结果再去执行下一个异步任务，最后只需要一个最终的结果
- 获取计算结果的 `get()` 方法为阻塞调用

Java 8 才被引入`CompletableFuture` 类可以解决`Future` 的这些缺陷。`CompletableFuture` 除了提供了更为好用和强大的 `Future` 特性之外，还提供了函数式编程、异步任务编排组合（可以将多个异步任务串联起来，组成一个完整的链式调用）等能力。

可以看到，`CompletableFuture` 同时实现了 `Future` 和 `CompletionStage` 接口。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409111537617.jpeg)



## CompletableFuture使用方式

### 创建**CompletableFuture**并执行

当需要进行异步处理的时候，可以通过`CompletableFuture.supplyAsync`方法，传入一个具体的要执行的处理逻辑函数，这样就轻松的完成了**CompletableFuture**的创建与触发执行。

| 方法名称    | 作用描述                                                     |
| ----------- | ------------------------------------------------------------ |
| supplyAsync | 静态方法，用于构建一个`CompletableFuture<T>`对象，并异步执行传入的函数，允许执行函数有返回值`T`。 |
| runAsync    | 静态方法，用于构建一个`CompletableFuture<Void>`对象，并异步执行传入函数，与supplyAsync的区别在于此方法传入的是Callable类型，**仅执行，没有返回值**。 |



使用示例：

```java
public void testCreateFuture(String product) {
    // supplyAsync， 执行逻辑有返回值Integer
    CompletableFuture<Integer> supplyAsyncResult =
            CompletableFuture.supplyAsync(() -> HttpRequestMock.getTaoBaoPrice(product));
    
    // runAsync, 执行逻辑没有返回值
    CompletableFuture<Void> runAsyncResult =
            CompletableFuture.runAsync(() -> System.out.println(product));
}
```

特别补充：

> `supplyAsync`或者`runAsync`创建后便会立即执行，无需手动调用触发。



### 线程串行化方法

#### 使用方法

在流水线处理场景中，往往都是一个任务环节处理完成后，下一个任务环节接着上一环节处理结果继续处理。`CompletableFuture`用于这种流水线环节驱动类的方法有很多，相互之间主要是在返回值或者给到下一环节的入参上有些许差异，使用时需要注意区分：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409111557853.png)

具体的方法的描述归纳如下：

| 方法名称    | 作用描述                                                     |
| ----------- | ------------------------------------------------------------ |
| thenApply   | 对`CompletableFuture`的执行后的具体结果进行追加处理，并将当前的`CompletableFuture`泛型对象更改为处理后新的对象类型，返回当前`CompletableFuture`对象。 |
| thenCompose | 与`thenApply`类似。区别点在于：此方法的入参函数是一个`CompletableFuture`类型对象，适用于回调函数需要启动另一个异步计算，并且想要一个扁平化的结果CompletableFuture，而不是嵌套的`CompletableFuture<CompletableFuture<U>>` |
| thenAccept  | 与`thenApply`方法类似，区别点在于`thenAccept`返回**void**类型，**没有具体结果输出**，适合无需返回值的场景。 |
| thenRun     | 与`thenAccept`类似，区别点在于`thenAccept`可以将前面`CompletableFuture`执行的实际结果作为入参进行传入并使用，但是`thenRun`方法**没有任何入参**，只能执行一个Runnable函数，并且**返回void类型**。 |

因为上述`thenApply`、`thenCompose`方法的输出仍然都是一个**CompletableFuture**对象，所以各个方法是可以一环接一环的进行调用，形成流水线式的处理逻辑：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409111558423.png)

##### thenApply

上面任务执行完执行 + 能获取上步返回值 + 自己有返回值

```java
@Test
public void thenApplyAsync() throws ExecutionException, InterruptedException {
    CompletableFuture<String> thenApplyAsync = CompletableFuture.supplyAsync(() -> {
        System.out.println("thenApplyAsync当前线程：" + Thread.currentThread().getId());
        int i = 10 / 2;
        System.out.println("thenApplyAsync运行结果：" + i);
        return i;
    }, executor).thenApply(result -> {
        System.out.println("thenApplyAsync任务2启动了。。。。。上步结果：" + result);
        return "hello" + result * 2;
    });
    System.out.println("main.................end....." + thenApplyAsync.get());
}
```

结果：

```java
thenApplyAsync当前线程：33
thenApplyAsync运行结果：5
thenApplyAsync任务2启动了。。。。。上步结果：5
main.................end.....hello10
```



##### thenAccept

上面任务执行完执行 + 能获取上步返回值

```java
@Test
public void thenAcceptAsync() throws ExecutionException, InterruptedException {
    CompletableFuture<Void> thenAcceptAsync = CompletableFuture.supplyAsync(() -> {
        System.out.println("thenAcceptAsync当前线程：" + Thread.currentThread().getId());
        int i = 10 / 2;
        System.out.println("thenAcceptAsync运行结果：" + i);
        return i;
    }, executor).thenAccept(result -> {
        System.out.println("thenAcceptAsync任务2启动了。。。。。上步结果：" + result);
    });
}
```

结果：

```java
thenAcceptAsync当前线程：33
thenAcceptAsync运行结果：5
thenAcceptAsync任务2启动了。。。。。上步结果：5
```



##### thenRun

上面任务执行完执行

```java
@Test
public void thenRunAsync() throws ExecutionException, InterruptedException {
    System.out.println("main.................start.....");
    CompletableFuture<Void> voidCompletableFuture = CompletableFuture.supplyAsync(() -> {
        System.out.println("当前线程：" + Thread.currentThread().getId());
        int i = 10 / 2;
        System.out.println("运行结果：" + i);
        return i;
    }, executor).thenRun(() -> {
        System.out.println("任务2启动了。。。。。");
    });
}
```

结果：

```java
main.................start.....
当前线程：33
运行结果：5
任务2启动了。。。。。
```



##### thenCompose

接收返回值并生成新的任务

```java
@Test
public void thenCompose() {
    CompletableFuture cf = CompletableFuture.completedFuture("hello")
            .thenCompose(str -> CompletableFuture.supplyAsync(() -> {
                return str + ": thenCompose";
            },executor));
    System.out.println(cf.join());
}
```

- thenApply()：转换的是泛型中的类型，相当于将CompletableFuture 转换生成新的CompletableFuture
- thenCompose()：用来连接两个CompletableFuture，是生成一个新的CompletableFuture。





#### 串联示例

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
    System.out.println("supplyAsync first");
    return "first";
}, fixedThreadPool).thenApply(s -> {
    Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
    System.out.println("supplyAsync second");
    return "second " + s;
}).whenComplete((s, t) -> {//s，是上面的返回值，t是上面可能会抛出的Throwable对象
    if (t == null) {
        System.out.println("whenComplete succeed:" + s);
    } else {
        System.out.println("whenComplete error");
    }
});
        
System.out.println(future.get());

//结果：
supplyAsync first
supplyAsync second
whenComplete succeed:secondfirst
second first
```





### 线程并联方法

很多时候为了提升并行效率，一些没有依赖的环节我们会让他们同时去执行，然后在某些环节需要依赖的时候，进行结果的依赖合并处理，类似如下图的效果。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409111622730.png)



`CompletableFuture`相比于`Future`的一大优势，就是可以方便的实现多个并行环节的合并处理。相关涉及方法介绍归纳如下：

| 方法名称       | 作用描述                                                     |
| -------------- | ------------------------------------------------------------ |
| thenCombine    | 将两个`CompletableFuture`对象组合起来进行下一步处理，可以拿到两个执行结果，并传给自己的执行函数进行下一步处理，最后返回一个新的`CompletableFuture`对象。 |
| thenAcceptBoth | 与`thenCombine`类似，区别点在于`thenAcceptBoth`传入的执行函数没有返回值，即thenAcceptBoth返回值为`CompletableFuture<Void>`。 |
| runAfterBoth   | 等待两个`CompletableFuture`都执行完成后再执行某个Runnable对象，再执行下一个的逻辑，类似thenRun。 |
| applyToEither  | 两个`CompletableFuture`中任意一个完成的时候，继续执行后面给定的新的函数处理。再执行后面给定函数的逻辑，类似thenApply。 |
| acceptEither   | 两个`CompletableFuture`中任意一个完成的时候，继续执行后面给定的新的函数处理。再执行后面给定函数的逻辑，类似thenAccept。 |
| runAfterEither | 等待两个`CompletableFuture`中任意一个执行完成后再执行某个Runnable对象，可以理解为`thenRun`的升级版，注意与`runAfterBoth`对比理解。 |
| allOf          | 静态方法，**阻塞**等待所有给定的`CompletableFuture`执行结束后，返回一个`CompletableFuture<Void>`结果。 |
| anyOf          | 静态方法，阻塞等待任意一个给定的`CompletableFuture`对象执行结束后，返回一个`CompletableFuture<Void>`结果。 |



#### 使用方法

##### thenCombine

消费两个结果 + 返回结果

```java
@Test
public void thenCombine() throws ExecutionException, InterruptedException {
    CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务1线程：" + Thread.currentThread().getId());
        int i = 10 / 2;
        System.out.println("任务1运行结果：" + i);
        return i;
    }, executor);

    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务2线程：" + Thread.currentThread().getId());
        System.out.println("任务2运行结果：");
        return "hello";
    }, executor);
    
    CompletableFuture<String> thenCombineAsync = future1.thenCombine(future2, (result1, result2) -> {
        System.out.println("任务5启动。。。结果1：" + result1 + "。。。结果2：" + result2);
        return result2 + "-->" + result1;
    });
    System.out.println("任务5结果" + thenCombineAsync.get());
}
```

结果：

```java
任务1线程：33
任务1运行结果：5
任务2线程：34
任务2运行结果：
任务5启动。。。结果1：5。。。结果2：hello
任务5结果hello-->5
```



##### thenAcceptBoth

消费两个结果 + 无返回

```java
@Test
public void thenAcceptBothAsync() throws ExecutionException, InterruptedException {
    CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务1线程：" + Thread.currentThread().getId());
        int i = 10 / 2;
        System.out.println("任务1运行结果：" + i);
        return i;
    }, executor);

    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务2线程：" + Thread.currentThread().getId());
        System.out.println("任务2运行结果：");
        return "hello";
    }, executor);

    CompletableFuture<Void> thenAcceptBothAsync = future1.thenAcceptBoth(future2, (result1, result2) -> {
        System.out.println("任务4启动。。。结果1：" + result1 + "。。。结果2：" + result2);
    });

}
```

结果

```java
任务1线程：33
任务1运行结果：5
任务2线程：34
任务2运行结果：
任务4启动。。。结果1：5。。。结果2：hello
```



##### runAfterBoth

两个任务都完成后，再接着运行

```java
@Test
public void runAfterBothAsync() {
    CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务1线程：" + Thread.currentThread().getId());
        int i = 10 / 2;
        System.out.println("任务1运行结果：" + i);
        return i;
    }, executor);

    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务2线程：" + Thread.currentThread().getId());
        System.out.println("任务2运行结果：");
        return "hello";
    }, executor);

    CompletableFuture<Void> runAfterBothAsync = future1.runAfterBoth(future2, () -> {
        System.out.println("任务3启动。。。");
    });

}
```

结果

```java
任务1线程：33
任务1运行结果：5
任务2线程：34
任务2运行结果：
任务3启动。。。
```



##### applyToEither

只要有一个执行完就执行 + 获取返回值 + 有返回值

```java
@Test
public void applyToEither() throws ExecutionException, InterruptedException {
    CompletableFuture<Object> future1 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务1线程：" + Thread.currentThread().getId());
        int i = 10 / 2;
        try {
            Thread.sleep(3000);
            System.out.println("任务1运行结果：" + i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return i;
    }, executor);

    CompletableFuture<Object> future2 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务2线程：" + Thread.currentThread().getId());
        System.out.println("任务2运行结果：");
        return "hello";
    }, executor);

    CompletableFuture<String> applyToEitherAsync = future1.applyToEither(future2, result -> {
        System.out.println("任务5开始执行。。。结果：" + result);
        return result.toString() + " world";
    });
    System.out.println("任务5结果：" + applyToEitherAsync.get());
}
```

结果

```java
任务1线程：33
任务2线程：34
任务2运行结果：
任务5开始执行。。。结果：hello
任务5结果：hello world
```



##### acceptEither

只要有一个执行完就执行 + 获取返回值

```java
@Test
public void acceptEither() {
    CompletableFuture<Object> future1 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务1线程：" + Thread.currentThread().getId());
        int i = 10 / 2;
        try {
            Thread.sleep(3000);
            System.out.println("任务1运行结果：" + i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return i;
    }, executor);

    CompletableFuture<Object> future2 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务2线程：" + Thread.currentThread().getId());
        System.out.println("任务2运行结果：");
        return "hello";
    }, executor);

    CompletableFuture<Void> acceptEitherAsync = future1.acceptEither(future2, result -> {
        System.out.println("任务4开始执行。。。结果：" + result);
    });

}
```

结果

```java
任务1线程：33
任务2线程：34
任务2运行结果：
任务4开始执行。。。结果：hello
```



##### runAfterEither

只要有一个执行完就执行

```java
@Test
public void runAfterEither() {
    CompletableFuture<Object> future1 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务1线程：" + Thread.currentThread().getId());
        int i = 10 / 2;
        try {
            Thread.sleep(3000);
            System.out.println("任务1运行结果：" + i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return i;
    }, executor);

    CompletableFuture<Object> future2 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务2线程：" + Thread.currentThread().getId());
        System.out.println("任务2运行结果：");
        return "hello";
    }, executor);

    CompletableFuture<Void> runAfterEitherAsync = future1.runAfterEither(future2, () -> {
        System.out.println("任务3开始执行。。。");
    });
}
```

结果

```java
任务1线程：33
任务2线程：34
任务2运行结果：
任务3开始执行。。。
```



##### allOf

等待全部完成后才执行

```java
@Test
public void allOf() throws ExecutionException, InterruptedException {
    CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务1");
        return "任务1";
    }, executor);
    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
        try {
            Thread.sleep(2000);
            System.out.println("任务2");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "任务2";
    }, executor);
    CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务3");
        return "任务3";
    }, executor);

    CompletableFuture<Void> allOf = CompletableFuture.allOf(future1, future2, future3);
    //等待所有任务完成
    //allOf.get();
    allOf.join();
    System.out.println("allOf" + future1.get() + "-------" + future2.get() + "-------" + future3.get());

}
```

结果

```java
任务1
任务3
任务2
allOf任务1-------任务2-------任务3
```



##### anyOf

等待其中之一完成后就执行

```java
@Test
public void anyOf() throws ExecutionException, InterruptedException {
    CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务1");
        return "任务1";
    }, executor);
    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
        try {
            Thread.sleep(2000);
            System.out.println("任务2");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "任务2";
    }, executor);

    CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
        System.out.println("任务3");
        return "任务3";
    }, executor);
    CompletableFuture<Object> anyOf = CompletableFuture.anyOf(future1, future2, future3);
    System.out.println("anyOf--最先完成的是" + anyOf.get());
    //等待future2打印
    System.out.println("等等任务2");
    Thread.sleep(3000);
}
```

结果

```java
任务1
anyOf--最先完成的是任务1
任务3
等等任务2
任务2
```





#### 并联示例

```java
ExecutorService fixedThreadPool = Executors.newFixedThreadPool(5);
CompletableFuture<String> firstfuture = CompletableFuture.supplyAsync(() -> {
    Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
    System.out.println("supplyAsync first");
    return "first";
}, fixedThreadPool);
CompletableFuture<String> secondfuture = CompletableFuture.supplyAsync(() -> {
    Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
    System.out.println("supplyAsync second");
    return "second";
}, fixedThreadPool);
CompletableFuture<String> thirdfuture = CompletableFuture.supplyAsync(() -> {
    Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
    System.out.println("supplyAsync third");
    return "third";
}, fixedThreadPool);

CompletableFuture.allOf(firstfuture, secondfuture, thirdfuture)
       .whenComplete((aVoid, t) -> {
             try {
                  System.out.println("whenComplete succeed:" + firstfuture.get() + secondfuture.get() + thirdfuture.get());
              } catch (Exception e) {
                  System.out.println("error");
              }
        });
```





### 结果等待与获取

在执行线程中将任务放到工作线程中进行处理的时候，执行线程与工作线程之间是异步执行的模式，如果执行线程需要获取到共工作线程的执行结果，则可以通过`get`或者`join`方法，**阻塞等待**并从`CompletableFuture`中获取对应的值。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409111647261.png)

对`get`和`join`的方法功能含义说明归纳如下：

| 方法名称            | 作用描述                                                     |
| ------------------- | ------------------------------------------------------------ |
| get()               | 等待`CompletableFuture`执行完成并获取其具体执行结果，可能会抛出异常，**需要**代码调用的地方手动`try...catch`进行处理。 |
| get(long, TimeUnit) | 与get()相同，只是**允许设定阻塞等待超时时间**，如果等待超过设定时间，则会抛出异常终止阻塞等待。 |
| join()              | 等待`CompletableFuture`执行完成并获取其具体执行结果，可能会抛出运行时异常，**无需**代码调用的地方手动try...catch进行处理。 |

从介绍上可以看出，两者的区别就在于是否需要调用方**显式的进行try...catch处理逻辑**，使用代码示例如下：

```java
public void testGetAndJoin(String product) {
    // join无需显式try...catch...
    PriceResult joinResult = CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouXiXiPrice(product))
            .join();
    
    try {
        // get显式try...catch...
        PriceResult getResult = CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouXiXiPrice(product))
                .get(5L, TimeUnit.SECONDS);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```



### 异常处理

在编排流水线的时候，如果某一个环节执行抛出异常了，会导致整个流水线后续的环节就没法再继续下去了，比如下面的例子：

```java
public void testExceptionHandle() {
    CompletableFuture.supplyAsync(() -> {
        throw new RuntimeException("supplyAsync excetion occurred...");
    }).thenApply(obj -> {
        System.out.println("thenApply executed...");
        return obj;
    }).join();
}
```

执行之后会发现，supplyAsync抛出异常后，后面的thenApply并没有被执行。



那如果想要让流水线的每个环节处理失败之后都能让流水线继续往下面环节处理，让后续环节可以拿到前面环节的结果或者是抛出的异常并进行对应的应对处理，就需要用到`handle`和`whenCompletable`方法了。

先看下两个方法的作用描述：

| 方法名称      | 作用描述                                                     |
| ------------- | ------------------------------------------------------------ |
| handle        | 与`thenApply`类似，区别点在于handle执行函数的入参有两个，一个是`CompletableFuture`执行的实际结果，一个是**Throwable对象**，这样如果前面执行出现异常的时候，可以通过handle获取到异常并进行处理。 |
| whenComplete  | 与`handle`类似，区别点在于`whenComplete`执行后**无返回值**。 |
| exceptionally | 捕获异常并返回指定值                                         |



#### handle

入参为 结果 或者 异常，返回新结果

```java
@Test
public void handle() throws ExecutionException, InterruptedException {
    System.out.println("main.................start.....");
    final CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
        System.out.println("当前线程：" + Thread.currentThread().getId());
        int i = 10 / 0;
        System.out.println("运行结果：" + i);
        return i;
    }, executor).handleAsync((in, throwable) -> {
        if (throwable != null) {
            return "报错返回";
        }
        return "正确了";
    });
    System.out.println("main.................end....." + completableFuture.get());

}
```

结果

```java
main.................start.....
当前线程：33
main.................end.....报错返回
```



#### whenComplete

whenComplete虽然得到异常信息，但是不能修改返回信息

```java
@Test
public void whenComplete() {
    System.out.println("main.................start.....");
    final CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
        System.out.println("当前线程：" + Thread.currentThread().getId());
        int i = 10 / 0;
        System.out.println("运行结果：" + i);
        return i;
    }, executor).whenComplete((result, throwable) -> {
        //whenComplete虽然得到异常信息，但是不能修改返回信息
        System.out.println("异步完成。。。。结果是：" + result + "...异常是：" + throwable);
    });

    try {
        System.out.println("main.................end..T..." + completableFuture.get());
    } catch (InterruptedException e) {
        System.out.println("报错了1");
    } catch (ExecutionException e) {
        System.out.println("报错了2");
    }
}
```

结果

```java
main.................start.....
当前线程：33
异步完成。。。。结果是：null...异常是：java.util.concurrent.CompletionException: java.lang.ArithmeticException: 除以零
报错了2
```



#### exceptionally

```java
@Test
public void exceptionally() throws ExecutionException, InterruptedException {
    System.out.println("main.................start.....");
    CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
        System.out.println("当前线程：" + Thread.currentThread().getId());
        int i = 10 / 0;
        System.out.println("运行结果：" + i);
        return i;
    }, executor).exceptionally(throwable -> {
        //R apply(T t);
        //exceptionally可以感知错误并返回指定值
        System.out.println("执行了exceptionally");
        return 0;
    });
    System.out.println("main.................end....." + completableFuture.get());
}
```

结果

```java
main.................start.....
当前线程：33
执行了exceptionally
main.................end.....0
```



### 实现超时

由于网络波动或者连接节点下线等种种问题，对于大多数网络异步任务的执行常常会进行超时限制，在异步开发中可以看成是一个常见的问题。

在 Java 9 中，`CompletableFuture` 引入了支持超时和延迟执行的改进，这两个功能对于控制异步操作行为至关重要。

#### orTimeout()

允许为 CompletableFuture 设置一个超时时间。如果在指定的超时时间内未完成，CompletableFuture 将以 TimeoutException 完成

- 示例

```java
@Test
public void orTimeTest() {
    try {
        CompletableFuture completableFuture = CompletableFuture.runAsync(() - > {
            System.out.println("异步任务开始执行....");
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).orTimeout(2, TimeUnit.SECONDS);

        completableFuture.join();
    } catch (Exception e) {
        System.out.println(e);
    }
}
```



#### completeOnTimeout()

允许在指定的超时时间内如果未完成，则用一个默认值来完成 `CompletableFuture`。该方法提供了一种优雅的回退机制，确保即使在超时的情况下也能保持异步流的连续性和完整性。

- 示例

```java
@Test
public void completeOnTimeoutTest() {
    CompletableFuture <String> completableFuture = CompletableFuture.supplyAsync(() - > {
        System.out.println("异步任务开始执行....");
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "死磕 Java 新特性";
    }).completeOnTimeout("死磕 Java", 2, TimeUnit.SECONDS);

    System.out.println("执行结果为：" + completableFuture.join());
}
```



### 延迟执行

`CompletableFuture` 提供了`delayedExecutor()` 来支持延迟执行，该方法创建一个延迟执行的 `Executor`，可以将任务的执行推迟到未来某个时间点。能够让我们更加精确地控制异步任务的执行时机，特别是在需要根据时间安排任务执行的场景中。

- 示例

```java
@Test
public void completeOnTimeoutTest() {
    // 创建一个延迟执行的Executor
    Executor delayedExecutor = CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS);

    // 使用延迟的Executor执行一个简单任务
    CompletableFuture <Void> future = CompletableFuture.runAsync(() - > {
        System.out.println("任务延迟后执行...");
    }, delayedExecutor);

    // 等待异步任务完成
    future.join();
}
```









## CompletableFuture的Async版本

在使用**CompletableFuture**的时候会发现，有很多的方法，都会同时有两个以**Async**命名结尾的方法版本。以`thenCombine`方法为例：

1. thenCombine(CompletionStage, BiFunction)
2. thenCombineAsync(CompletionStage, BiFunction)
3. thenCombineAsync(CompletionStage, BiFunction, Executor)

从参数上看，区别并不大，仅第三个方法入参中多了线程池Executor对象。看下三个方法的源码实现，会发现其整体实现逻辑都是一致的，仅仅是使用线程池这个地方的逻辑有一点点的差异：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092159870.webp)

有兴趣的可以去翻一下此部分的源码实现，这里概括下三者的区别：

1. thenCombine方法，沿用上一个执行任务所使用的线程池进行处理
2. thenCombineAsync两个入参的方法，使用默认的ForkJoinPool线程池中的工作线程进行处理
3. themCombineAsync三个入参的方法，支持自定义线程池并指定使用自定义线程池中的线程作为工作线程去处理待执行任务。



为了更好的理解下上述的三个差异点，通过下面的代码来演示下：

- **用法1： **其中thenCombineAsync指定使用自定义线程池，supplyAsync方法不指定线程池（使用默认线程池）

```java
public PriceResult getCheapestPlatAndPrice4(String product) {
    // 构造自定义线程池
    ExecutorService executor = Executors.newFixedThreadPool(5);
    
    return
        CompletableFuture.supplyAsync(
            () -> HttpRequestMock.getPinDuoDuoPrice(product)
        ).thenCombineAsync(
            CompletableFuture.supplyAsync(() -> HttpRequestMock.getPinDuoDuoDiscounts(product)),
            this::computeRealPrice,
            executor
        ).join();
}
```

没有指定自定义线程池的supplyAsync方法，其使用了默认的`ForkJoinPool`工作线程来运行，而指定了自定义线程池的方法，则使用了自定义线程池来执行。

```java
17:23:50.683636700[ForkJoinPool.commonPool-worker-1]获取拼多多上iphone16的价格完成： 5399
17:23:50.683636700[ForkJoinPool.commonPool-worker-2]获取拼多多上iphone16的折扣价格完成： 99
17:23:50.696637100[pool-2-thread-1]计算实际价格完成： 5300
5300
耗时：1079ms
```





- **用法2**： 不指定自定义线程池，使用默认线程池策略，使用thenCombine方法

```java
public PriceResult getCheapestPlatAndPrice5(String product) {
    return
        CompletableFuture.supplyAsync(
            () -> HttpRequestMock.getPinDuoDuoPrice(product)
        ).thenCombine(
            CompletableFuture.supplyAsync(() -> HttpRequestMock.getPinDuoDuoDiscounts(product)),
            this::computeRealPrice
        ).join();
}
```

执行结果如下，可以看到执行线程名称与**用法1**示例相比发生了变化。因为没有指定线程池，所以两个`supplyAsync`方法都是用的默认的`ForkJoinPool`线程池，而`thenCombine`使用的是上一个任务所使用的线程池，所以也是用的`ForkJoinPool`。

```ini
17:24:53.840945700[ForkJoinPool.commonPool-worker-2]获取拼多多上iphone16的折扣价格完成： 99
17:24:53.840945700[ForkJoinPool.commonPool-worker-1]获取拼多多上iphone16的价格完成： 5399
17:24:53.850944100[ForkJoinPool.commonPool-worker-1]计算实际价格完成： 5300
5300
耗时：1083ms
```



现在，我们知道了方法名称带有Async和不带Async的实现策略上的差异点就在于使用哪个线程池来执行而已。那么，对我们实际的指导意义是啥呢？实际使用的时候，应该怎么判断自己应该使用带Async结尾的方法、还是不带Async结尾的方法呢？

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092200130.webp)

上面是Async结尾方法默认使用的ForkJoinPool创建的逻辑，这里可以看出，默认的线程池中的工作线程数是`CPU核数 - 1`，并且指定了默认的丢弃策略等，这就是一个主要关键点。所以说，符合以下几个条件的时候，可以考虑使用带有Async后缀的方法，指定自定义线程池：

- 默认线程池的线程数满足不了实际诉求
- 默认线程池的类型不符合自己业务诉求
- 默认线程池的队列满处理策略不满足自己诉求



## 使用注意点

### 与Stream结合

在涉及批量进行并行处理的时候，通过`Stream`与`CompletableFuture`结合使用，可以简化很多编码逻辑。但是**在使用细节方面需要注意下**，避免达不到使用`CompletableFuture`的预期效果。

> **需求场景：** 在同一个平台内，传入多个商品，查询不同商品对应的价格与优惠信息，并选出实付价格最低的商品信息。

结合前面的介绍分析，我们应该知道最佳的方式，就是同时并行的方式去各自请求数据，最后合并处理即可。所以我们规划按照如下的策略来实现：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409111654500.jpeg)



先看第一种编码实现：

```java
public int comparePriceInOnePlat(List <String> products) {
    return products.stream()
        .map(product -> CompletableFuture.supplyAsync(() -> HttpRequestMock.getTaoBaoPrice(product))
            .thenCombine(CompletableFuture.supplyAsync(() -> HttpRequestMock.getTaoBaoDiscounts(product)),
                this::computeRealPrice))
        .map(CompletableFuture::join)
        .min(Comparator.comparingInt(p -> p))
        .get();
}
```

对于List的处理场景，这里采用了Stream方式来进行遍历与结果的收集、排序与返回。看似正常，但是执行的时候会发现，并没有达到我们预期的效果：

```ini
16:59:22.384338900[ForkJoinPool.commonPool-worker-2]获取淘宝上iphone16的折扣价格完成： 200
16:59:22.384338900[ForkJoinPool.commonPool-worker-1]获取淘宝上iphone16的价格完成： 5199
16:59:22.396881[ForkJoinPool.commonPool-worker-1]计算实际价格完成： 4999
16:59:23.404683800[ForkJoinPool.commonPool-worker-2]获取淘宝上iphone17的折扣价格完成： 200
16:59:23.404683800[ForkJoinPool.commonPool-worker-1]获取淘宝上iphone17的价格完成： 5199
16:59:23.404683800[ForkJoinPool.commonPool-worker-1]计算实际价格完成： 4999
16:59:24.416418500[ForkJoinPool.commonPool-worker-2]获取淘宝上iphone18的折扣价格完成： 200
16:59:24.417266700[ForkJoinPool.commonPool-worker-1]获取淘宝上iphone18的价格完成： 5199
16:59:24.417266700[ForkJoinPool.commonPool-worker-1]计算实际价格完成： 4999
4999
耗时：3116ms
```

从上述执行结果可以看出，其具体处理的时候，其实是按照下面的逻辑去处理了：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409111703727.jpg)

为什么会出现这种实际与预期的差异呢？原因就在于使用的Stream上面！虽然Stream中使用两个`map`方法，但Stream处理的时候并不会分别遍历两遍，其实写法等同于下面这种写到`1个`map中处理，改为下面这种写法，其实也就更容易明白为啥会没有达到我们预期的整体并行效果了：

```java
public int comparePriceInOnePlat1(List < String > products) {
    return products.stream()
        .map(product -> CompletableFuture.supplyAsync(() -> HttpRequestMock.getTaoBaoPrice(product))
            .thenCombine(CompletableFuture.supplyAsync(() -> HttpRequestMock.getTaoBaoDiscounts(product)), this::computeRealPrice).join())
        .min(Comparator.comparingInt(p -> p))
        .get();
}
```

既然如此，这种场景是不是就不能使用Stream了呢？也不是，其实**拆开成两个Stream**分步操作下其实就可以了。



再看下面的第二种实现代码：

```java
public int comparePriceInOnePlat2(List < String > products) {
    // 先触发各自平台的并行处理
    List <CompletableFuture <Integer>> completableFutures = products.stream()
        .map(product -> CompletableFuture.supplyAsync(() -> HttpRequestMock.getTaoBaoPrice(product))
            .thenCombine(CompletableFuture.supplyAsync(() -> HttpRequestMock.getTaoBaoDiscounts(product)), this::computeRealPrice))
        .collect(Collectors.toList());
    // 在独立的流中，等待所有并行处理结束，做最终结果处理
    return completableFutures.stream()
        .map(CompletableFuture::join)
        .min(Comparator.comparingInt(p -> p))
        .get();
}
```

执行结果：

```ini
17:08:00.052684200[ForkJoinPool.commonPool-worker-2]获取淘宝上iphone16的折扣价格完成： 200
17:08:00.051681700[ForkJoinPool.commonPool-worker-5]获取淘宝上iphone18的价格完成： 5199
17:08:00.051681700[ForkJoinPool.commonPool-worker-6]获取淘宝上iphone18的折扣价格完成： 200
17:08:00.052684200[ForkJoinPool.commonPool-worker-3]获取淘宝上iphone17的价格完成： 5199
17:08:00.051681700[ForkJoinPool.commonPool-worker-1]获取淘宝上iphone16的价格完成： 5199
17:08:00.051681700[ForkJoinPool.commonPool-worker-4]获取淘宝上iphone17的折扣价格完成： 200
17:08:00.064680500[ForkJoinPool.commonPool-worker-4]计算实际价格完成： 4999
17:08:00.064680500[ForkJoinPool.commonPool-worker-1]计算实际价格完成： 4999
17:08:00.063680100[ForkJoinPool.commonPool-worker-6]计算实际价格完成： 4999
4999
耗时：1083ms
```

从执行结果可以看出，三个商品并行处理，整体处理耗时相比前面编码方式有很大提升，达到了预期的效果。



**归纳下**：因为Stream的操作具有**惰性执行**的特点，且只有遇到终止操作（比如collect方法）的时候才会真正的执行。所以遇到这种需要并行处理且需要合并多个并行处理流程的情况下，需要将并行流程与合并逻辑放到两个Stream中，这样分别触发完成各自的处理逻辑，就可以了。



### 使用自定义线程池

`CompletableFuture` 默认使用`ForkJoinPool.commonPool()` 作为执行器，这个线程池是全局共享的，可能会被其他任务占用，导致性能下降或者饥饿。因此，建议使用自定义的线程池来执行 `CompletableFuture` 的异步任务，可以提高并发度和灵活性。

```java
private ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10,
        0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>());

CompletableFuture.runAsync(() -> {
     //...
}, executor);
```



### 尽量避免使用get()

`CompletableFuture`的`get()`方法是阻塞的，尽量避免使用。如果必须要使用的话，需要添加超时时间，否则可能会导致主线程一直等待，无法执行其他任务。

```java
    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "Hello, world!";
    });

    // 获取异步任务的返回值，设置超时时间为 5 秒
    try {
        String result = future.get(5, TimeUnit.SECONDS);
        System.out.println(result);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
        // 处理异常
        e.printStackTrace();
    }
}

```







<!-- @include: @article-footer.snippet.md -->     



