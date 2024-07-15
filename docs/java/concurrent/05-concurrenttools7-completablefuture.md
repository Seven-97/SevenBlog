---
title: 并发工具 - CompletableFuture详解
category: Java
tag:
 - 并发编程
---









## 前言

例如现在有这么个需求：

> **需求描述**： 实现一个全网比价服务，比如可以从某宝、某东、某夕夕去获取某个商品的价格、优惠金额，并计算出实际付款金额，最终返回价格最优的平台与价格信息。

这里假定每个平台获取原价格与优惠券的接口已经实现、且都是需要调用HTTP接口查询的耗时操作，Mock接口每个耗时`1s`左右。

根据最初的需求理解，我们可以很自然的写出对应实现代码：

```java
public PriceResult getCheapestPlatAndPrice(String product) {
    PriceResult mouBaoPrice = computeRealPrice(HttpRequestMock.getMouBaoPrice(product),            HttpRequestMock.getMouBaoDiscounts(product));
    PriceResult mouDongPrice = computeRealPrice(HttpRequestMock.getMouDongPrice(product),            HttpRequestMock.getMouDongDiscounts(product));
    PriceResult mouXiXiPrice = computeRealPrice(HttpRequestMock.getMouXiXiPrice(product),            HttpRequestMock.getMouXiXiDiscounts(product));
    // 计算并选出实际价格最低的平台
    return Stream.of(mouBaoPrice, mouDongPrice, mouXiXiPrice).            min(Comparator.comparingInt(PriceResult::getRealPrice))            .get();
}
```

一切顺利成章，运行测试下：

```ini
05:24:54.779[main|1]获取某宝上 Iphone13的价格完成： 5199
05:24:55.781[main|1]获取某宝上 Iphone13的优惠完成： -200
05:24:55.781[main|1]某宝最终价格计算完成：4999
05:24:56.784[main|1]获取某东上 Iphone13的价格完成： 5299
05:24:57.786[main|1]获取某东上 Iphone13的优惠完成： -150
05:24:57.786[main|1]某东最终价格计算完成：5149
05:24:58.788[main|1]获取某夕夕上 Iphone13的价格完成： 5399
05:24:59.791[main|1]获取某夕夕上 Iphone13的优惠完成： -5300
05:24:59.791[main|1]某夕夕最终价格计算完成：99
获取最优价格信息：【平台：某夕夕, 原价：5399, 折扣：0, 实付价：99】
-----执行耗时： 6122ms  ------
```

结果符合预期，功能一切正常，就是耗时长了点。试想一下，假如你在某个APP操作查询的时候，等待6s才返回结果，**估计会直接把APP给卸载了吧**？



梳理下前面代码的实现思路：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092116849.webp)

所有的环节都是`串行`的，每个环节耗时加到一起，接口总耗时肯定很长。

但实际上，每个平台之间的操作是**互不干扰**的，那我们自然而然的可以想到，可以通过`多线程`的方式，同时去分别执行各个平台的逻辑处理，最后将各个平台的结果汇总到一起比对得到最低价格。

所以整个执行过程会变成如下的效果：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092116907.webp)

为了提升性能，我们采用**线程池**来负责多线程的处理操作，因为我们需要得到各个子线程处理的结果，所以我们需要使用 `Future`来实现：

```java
public PriceResult getCheapestPlatAndPrice2(String product) {
    Future<PriceResult> mouBaoFuture = threadPool.submit(() -> computeRealPrice(HttpRequestMock.getMouBaoPrice(product), HttpRequestMock.getMouBaoDiscounts(product)));
    Future<PriceResult> mouDongFuture = threadPool.submit(() -> computeRealPrice(HttpRequestMock.getMouDongPrice(product), HttpRequestMock.getMouDongDiscounts(product)));
    Future<PriceResult> mouXiXiFuture = threadPool.submit(() -> computeRealPrice(HttpRequestMock.getMouXiXiPrice(product), HttpRequestMock.getMouXiXiDiscounts(product)));
    
    // 等待所有线程结果都处理完成，然后从结果中计算出最低价
    return Stream.of(mouBaoFuture, mouDongFuture, mouXiXiFuture)
        .map(priceResultFuture -> {
            try {
                return priceResultFuture.get(5L, TimeUnit.SECONDS);
            } catch (Exception e) {
                return null;
            }
        })
        .filter(Objects::nonNull).min(Comparator.comparingInt(PriceResult::getRealPrice)).get();
}
```

上述代码中，将三个不同平台对应的`Callable`函数逻辑放入到`ThreadPool`中去执行，返回`Future`对象，然后再逐个通过`Future.get()`接口**阻塞**获取各自平台的结果，最后经比较处理后返回最低价信息。

执行代码，可以看到执行结果与过程如下：

```ini
05:42:25.291[pool-1-thread-2|13]获取某东上 Iphone13的价格完成： 5299
05:42:25.291[pool-1-thread-3|14]获取某夕夕上 Iphone13的价格完成： 5399
05:42:25.291[pool-1-thread-1|12]获取某宝上 Iphone13的价格完成： 5199
05:42:26.294[pool-1-thread-2|13]获取某东上 Iphone13的优惠完成： -150
05:42:26.294[pool-1-thread-3|14]获取某夕夕上 Iphone13的优惠完成： -5300
05:42:26.294[pool-1-thread-1|12]获取某宝上 Iphone13的优惠完成： -200
05:42:26.294[pool-1-thread-2|13]某东最终价格计算完成：5149
05:42:26.294[pool-1-thread-3|14]某夕夕最终价格计算完成：99
05:42:26.294[pool-1-thread-1|12]某宝最终价格计算完成：4999
获取最优价格信息：【平台：某夕夕, 原价：5399, 折扣：0, 实付价：99】
-----执行耗时： 2119ms  ------
```

结果与第一种实现方式一致，但是接口总耗时从`6s`下降到了`2s`，效果还是很显著的。但是，是否还能再压缩一些呢？



基于上面按照平台拆分并行处理的思路继续推进，我们可以看出每个平台内的处理逻辑其实可以分为3个主要步骤：

1. 获取原始价格（耗时操作）
2. 获取折扣优惠（耗时操作）
3. 得到原始价格和折扣优惠之后，计算实付价格

这3个步骤中，第1、2两个耗时操作也是相对独立的，如果也能并行处理的话，响应时长上应该又会缩短一些，即如下的处理流程：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092116849.webp)

我们当然可以继续使用上面提到的`线程池+Future`的方式，但`Future`在应对并行结果组合以及后续处理等方面显得力不从心，**弊端**明显：

> 代码写起来会**非常拖沓**：先封装`Callable`函数放到线程池中去执行查询操作，然后分三组`阻塞等待`结果并计算出各自结果，最后再`阻塞等待`价格计算完成后汇总得到最终结果。



说到这里呢，就需要`CompletableFuture`登场了，通过它可以很轻松的来完成任务的并行处理，以及各个并行任务结果之间的组合再处理等操作。使用`CompletableFuture`编写实现代码如下：

```java
public PriceResult getCheapestPlatAndPrice3(String product) {
    CompletableFuture<PriceResult> mouBao = CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouBaoPrice(product)).thenCombine(CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouBaoDiscounts(product)), this::computeRealPrice);
    CompletableFuture<PriceResult> mouDong = CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouDongPrice(product)).thenCombine(CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouDongDiscounts(product)), this::computeRealPrice);
    CompletableFuture<PriceResult> mouXiXi = CompletableFuture.supplyAsync(() ->  HttpRequestMock.getMouXiXiPrice(product)).thenCombine(CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouXiXiDiscounts(product)), this::computeRealPrice);

    // 排序并获取最低价格
    return Stream.of(mouBao, mouDong, mouXiXi)
            .map(CompletableFuture::join)
            .sorted(Comparator.comparingInt(PriceResult::getRealPrice))
            .findFirst()
            .get();
}
```

看下执行结果符合预期，而接口耗时则降到了`1s`（因为我们依赖的每一个查询实际操作的接口耗时都是模拟的1s，所以这个结果已经算是此复合接口能达到的极限值了）。

```ini
06:01:13.354[ForkJoinPool.commonPool-worker-6|17]获取某夕夕上 Iphone13的优惠完成： -5300
06:01:13.354[ForkJoinPool.commonPool-worker-13|16]获取某夕夕上 Iphone13的价格完成： 5399
06:01:13.354[ForkJoinPool.commonPool-worker-4|15]获取某东上 Iphone13的优惠完成： -150
06:01:13.354[ForkJoinPool.commonPool-worker-9|12]获取某宝上 Iphone13的价格完成： 5199
06:01:13.354[ForkJoinPool.commonPool-worker-11|14]获取某东上 Iphone13的价格完成： 5299
06:01:13.354[ForkJoinPool.commonPool-worker-2|13]获取某宝上 Iphone13的优惠完成： -200
06:01:13.354[ForkJoinPool.commonPool-worker-13|16]某夕夕最终价格计算完成：99
06:01:13.354[ForkJoinPool.commonPool-worker-11|14]某东最终价格计算完成：5149
06:01:13.354[ForkJoinPool.commonPool-worker-2|13]某宝最终价格计算完成：4999
获取最优价格信息：【平台：某夕夕, 原价：5399, 折扣：0, 实付价：99】
-----执行耗时： 1095ms  ------
```

**CompletableFuture**执行时所使用的默认线程池是[`ForkJoinPool`](https://www.seven97.top/java/concurrent/04-threadpool4-forkjoin.html)，早在JAVA7版本就已经被引入，但是很多人对`ForkJoinPool`不是很了解，实际项目中使用的也比较少。其实对`ForkJoinPool`的合理利用，可以让我们在面对某些多线程场景时会更加的从容高效。



## CompletableFuture深入了解

作为`JAVA8`之后加入的新成员，`CompletableFuture`的实现与使用上，也处处体现出了**函数式异步编程**的味道。一个`CompletableFuture`对象可以被一个环节接一个环节的处理、也可以对两个或者多个`CompletableFuture`进行组合处理或者等待结果完成。通过对`CompletableFuture`各种方法的合理使用与组合搭配，可以让我们在很多的场景都可以应付自如。

下面就来一起了解下这些方法以及对应的使用方式吧。



### Future与CompletableFuture

首先，先来理一下Future与CompletableFuture之间的关系。

#### Future

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

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092118646.webp)



#### CompletableFuture

Future在应对一些简单且相互独立的异步执行场景很便捷，但是在一些复杂的场景，比如同时需要多个有依赖关系的异步独立处理的时候，或者是一些类似流水线的异步处理场景时，就显得力不从心了。比如：

- 同时执行多个并行任务，等待最快的一个完成之后就可以继续往后处理
- 多个异步任务，每个异步任务都需要依赖前一个异步任务执行的结果再去执行下一个异步任务，最后只需要一个最终的结果
- 等待多个异步任务全部执行完成后触发下一个动作执行
- ...

所以呢， 在JAVA8开始引入了全新的`CompletableFuture`类，它是Future接口的一个实现类。也就是在Future接口的基础上，额外封装提供了一些执行方法，用来解决Future使用场景中的一些不足，对**流水线**处理能力提供了支持。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092121573.webp)



### CompletableFuture使用方式

#### 创建**CompletableFuture**并执行

当我们需要进行异步处理的时候，我们可以通过`CompletableFuture.supplyAsync`方法，传入一个具体的要执行的处理逻辑函数，这样就轻松的完成了**CompletableFuture**的创建与触发执行。

| 方法名称    | 作用描述                                                     |
| ----------- | ------------------------------------------------------------ |
| supplyAsync | 静态方法，用于构建一个`CompletableFuture<T>`对象，并异步执行传入的函数，允许执行函数有返回值`T`。 |
| runAsync    | 静态方法，用于构建一个`CompletableFuture<Void>`对象，并异步执行传入函数，与supplyAsync的区别在于此方法传入的是Callable类型，**仅执行，没有返回值**。 |



使用示例：

```java
public void testCreateFuture(String product) {
    // supplyAsync， 执行逻辑有返回值PriceResult
    CompletableFuture<PriceResult> supplyAsyncResult =
            CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouBaoPrice(product));
    // runAsync, 执行逻辑没有返回值
    CompletableFuture<Void> runAsyncResult =
            CompletableFuture.runAsync(() -> System.out.println(product));
}
```

特别补充：

> `supplyAsync`或者`runAsync`创建后便会立即执行，无需手动调用触发。



#### 环环相扣处理

在流水线处理场景中，往往都是一个任务环节处理完成后，下一个任务环节接着上一环节处理结果继续处理。`CompletableFuture`用于这种流水线环节驱动类的方法有很多，相互之间主要是在返回值或者给到下一环节的入参上有些许差异，使用时需要注意区分：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092138787.webp)

具体的方法的描述归纳如下：

| 方法名称    | 作用描述                                                     |
| ----------- | ------------------------------------------------------------ |
| thenApply   | 对`CompletableFuture`的执行后的具体结果进行追加处理，并将当前的`CompletableFuture`泛型对象更改为处理后新的对象类型，返回当前`CompletableFuture`对象。 |
| thenCompose | 与`thenApply`类似。区别点在于：此方法的入参函数返回的是一个`CompletableFuture`类型对象，适用于回调函数需要启动另一个异步计算，并且想要一个扁平化的结果CompletableFuture，而不是嵌套的`CompletableFuture<CompletableFuture<U>>` |
| thenAccept  | 与`thenApply`方法类似，区别点在于`thenAccept`返回**void**类型，**没有具体结果输出**，适合无需返回值的场景。 |
| thenRun     | 与`thenAccept`类似，区别点在于`thenAccept`可以将前面`CompletableFuture`执行的实际结果作为入参进行传入并使用，但是`thenRun`方法**没有任何入参**，只能执行一个Runnable函数，并且**返回void类型**。 |

因为上述`thenApply`、`thenCompose`方法的输出仍然都是一个**CompletableFuture**对象，所以各个方法是可以一环接一环的进行调用，形成流水线式的处理逻辑：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092138708.webp)



期望总是美好的，但是实际情况却总不尽如人意。在我们编排流水线的时候，如果某一个环节执行抛出异常了，会导致整个流水线后续的环节就没法再继续下去了，比如下面的例子：

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

那如果我们想要让流水线的每个环节处理失败之后都能让流水线继续往下面环节处理，让后续环节可以拿到前面环节的结果或者是抛出的异常并进行对应的应对处理，就需要用到`handle`和`whenCompletable`方法了。

先看下两个方法的作用描述：

| 方法名称     | 作用描述                                                     |
| ------------ | ------------------------------------------------------------ |
| handle       | 与`thenApply`类似，区别点在于handle执行函数的入参有两个，一个是`CompletableFuture`执行的实际结果，一个是是**Throwable对象**，这样如果前面执行出现异常的时候，可以通过handle获取到异常并进行处理。 |
| whenComplete | 与`handle`类似，区别点在于`whenComplete`执行后**无返回值**。 |

我们对上面一段代码示例修改使用handle方法来处理：

```java
public void testExceptionHandle() {
    CompletableFuture.supplyAsync(() -> {
        throw new RuntimeException("supplyAsync excetion occurred...");
    }).handle((obj, e) -> {
        if (e != null) {
            System.out.println("thenApply executed, exception occurred...");
        }
        return obj;
    }).join();
}
```

再执行可以发现，即使前面环节出现异常，后面环节也可以继续处理，且可以拿到前一环节抛出的异常信息：

```erlang
thenApply executed, exception occurred...
```



#### 多个**CompletableFuture组合操作**

前面一直在介绍流水线式的处理场景，但是很多时候，流水线处理场景也不会是一个链路顺序往下走的情况，很多时候为了提升并行效率，一些没有依赖的环节我们会让他们同时去执行，然后在某些环节需要依赖的时候，进行结果的依赖合并处理，类似如下图的效果。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092145072.webp)

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



#### 结果等待与获取

在执行线程中将任务放到工作线程中进行处理的时候，执行线程与工作线程之间是异步执行的模式，如果执行线程需要获取到共工作线程的执行结果，则可以通过`get`或者`join`方法，阻塞等待并从`CompletableFuture`中获取对应的值。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092148982.webp)

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



### **CompletableFuture**方法及其Async版本

我们在使用**CompletableFuture**的时候会发现，有很多的方法，都会同时有两个以**Async**命名结尾的方法版本。以前面我们用的比较多的`thenCombine`方法为例：

1. thenCombine(CompletionStage, BiFunction)
2. thenCombineAsync(CompletionStage, BiFunction)
3. thenCombineAsync(CompletionStage, BiFunction, Executor)

从参数上看，区别并不大，仅第三个方法入参中多了线程池Executor对象。看下三个方法的源码实现，会发现其整体实现逻辑都是一致的，仅仅是使用线程池这个地方的逻辑有一点点的差异：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092159870.webp)

有兴趣的可以去翻一下此部分的源码实现，这里概括下三者的区别：

1. thenCombine方法，沿用上一个执行任务所使用的线程池进行处理
2. thenCombineAsync两个入参的方法，使用默认的ForkJoinPool线程池中的工作线程进行处理
3. themCombineAsync三个入参的方法，支持自定义线程池并指定使用自定义线程池中的线程作为工作线程去处理待执行任务。



为了更好的理解下上述的三个差异点，我们通过下面的代码来演示下：

- **用法1： **其中一个supplyAsync方法以及thenCombineAsync指定使用自定义线程池，另一个supplyAsync方法不指定线程池（使用默认线程池）

```java
public PriceResult getCheapestPlatAndPrice4(String product) {
    // 构造自定义线程池
    ExecutorService executor = Executors.newFixedThreadPool(5);
    
    return
        CompletableFuture.supplyAsync(
            () -> HttpRequestMock.getMouXiXiPrice(product), 
            executor
        ).thenCombineAsync(
            CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouXiXiDiscounts(product)),
            this::computeRealPrice,
            executor
        ).join();
}
```

对上述代码实现策略的解读，以及与执行结果的关系展示如下图所示，可以看出，没有指定自定义线程池的supplyAsync方法，其使用了默认的`ForkJoinPool`工作线程来运行，而另外两个指定了自定义线程池的方法，则使用了自定义线程池来执行。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092200490.webp)

- **用法2**： 不指定自定义线程池，使用默认线程池策略，使用thenCombine方法

```java
public PriceResult getCheapestPlatAndPrice5(String product) {
    return
        CompletableFuture.supplyAsync(
            () -> HttpRequestMock.getMouXiXiPrice(product)
        ).thenCombine(
            CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouXiXiDiscounts(product)),
            this::computeRealPrice
        ).join();
}
```

执行结果如下，可以看到执行线程名称与**用法1**示例相比发生了变化。因为没有指定线程池，所以两个`supplyAsync`方法都是用的默认的`ForkJoinPool`线程池，而`thenCombine`使用的是上一个任务所使用的线程池，所以也是用的`ForkJoinPool`。

```ini
14:34:27.815[ForkJoinPool.commonPool-worker-1|12]获取某夕夕上 Iphone13的价格
14:34:27.815[ForkJoinPool.commonPool-worker-2|13]获取某夕夕上 Iphone13的优惠
14:34:28.831[ForkJoinPool.commonPool-worker-2|13]获取某夕夕上 Iphone13的优惠完成： -5300
14:34:28.831[ForkJoinPool.commonPool-worker-1|12]获取某夕夕上 Iphone13的价格完成： 5399
14:34:28.831[ForkJoinPool.commonPool-worker-2|13]某夕夕最终价格计算完成：99
获取最优价格信息：【平台：某夕夕, 原价：5399, 折扣：0, 实付价：99】
-----执行耗时： 1083ms  ------
```



现在，我们知道了方法名称带有Async和不带Async的实现策略上的差异点就在于使用哪个线程池来执行而已。那么，对我们实际的指导意义是啥呢？实际使用的时候，我们怎么判断自己应该使用带Async结尾的方法、还是不带Async结尾的方法呢？

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092200130.webp)

上面是Async结尾方法默认使用的ForkJoinPool创建的逻辑，这里可以看出，默认的线程池中的工作线程数是`CPU核数 - 1`，并且指定了默认的丢弃策略等，这就是一个主要关键点。

所以说，符合以下几个条件的时候，可以考虑使用带有Async后缀的方法，指定自定义线程池：

- 默认线程池的线程数满足不了实际诉求
- 默认线程池的类型不符合自己业务诉求
- 默认线程池的队列满处理策略不满足自己诉求



### 与Stream结合使用的注意点

在涉及批量进行并行处理的时候，通过`Stream`与`CompletableFuture`结合使用，可以简化我们的很多编码逻辑。但是**在使用细节方面需要注意下**，避免达不到使用`CompletableFuture`的预期效果。

> **需求场景：** 在同一个平台内，传入多个商品，查询不同商品对应的价格与优惠信息，并选出实付价格最低的商品信息。

结合前面的介绍分析，我们应该知道最佳的方式，就是同时并行的方式去各自请求数据，最后合并处理即可。所以我们规划按照如下的策略来实现：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092200933.webp)

先看第一种编码实现：

```java
public PriceResult comparePriceInOnePlat(List<String> products) {
    return products.stream()
            .map(product ->
                    CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouBaoPrice(product))
                            .thenCombine(
                                    CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouBaoDiscounts(product)),
                                    this::computeRealPrice))
            .map(CompletableFuture::join)
            .sorted(Comparator.comparingInt(PriceResult::getRealPrice))
            .findFirst()
            .get();
}
```

对于List的处理场景，这里采用了Stream方式来进行遍历与结果的收集、排序与返回。看似正常，但是执行的时候会发现，并没有达到我们预期的效果：

```ini
07:37:15.408[ForkJoinPool.commonPool-worker-9|12]获取某宝上 Iphone13黑色的价格完成： 5199
07:37:15.408[ForkJoinPool.commonPool-worker-2|13]获取某宝上 Iphone13黑色的优惠完成： -200
07:37:15.408[ForkJoinPool.commonPool-worker-2|13]某宝最终价格计算完成：4999
07:37:16.410[ForkJoinPool.commonPool-worker-9|12]获取某宝上 Iphone13白色的价格完成： 5199
07:37:16.410[ForkJoinPool.commonPool-worker-11|14]获取某宝上 Iphone13白色的优惠完成： -200
07:37:16.410[ForkJoinPool.commonPool-worker-11|14]某宝最终价格计算完成：4999
07:37:17.412[ForkJoinPool.commonPool-worker-11|14]获取某宝上 Iphone13红色的价格完成： 5199
07:37:17.412[ForkJoinPool.commonPool-worker-9|12]获取某宝上 Iphone13红色的优惠完成： -200
07:37:17.412[ForkJoinPool.commonPool-worker-9|12]某宝最终价格计算完成：4999
获取最优价格信息：【平台：某宝, 原价：5199, 折扣：0, 实付价：4999】
-----执行耗时： 3132ms  ------
```

从上述执行结果可以看出，其具体处理的时候，其实是按照下面的逻辑去处理了：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092200937.webp)

为什么会出现这种实际与预期的差异呢？原因就在于我们使用的Stream上面！虽然Stream中使用两个`map`方法，但Stream处理的时候并不会分别遍历两遍，其实写法等同于下面这种写到`1个`map中处理，改为下面这种写法，其实大家也就更容易明白为啥会没有达到我们预期的整体并行效果了：

```java
public PriceResult comparePriceInOnePlat1(List<String> products) {
    return products.stream()
        .map(product -> CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouBaoPrice(product)).thenCombine(CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouBaoDiscounts(product)), this::computeRealPrice).join())
        .sorted(Comparator.comparingInt(PriceResult::getRealPrice))
        .findFirst()
        .get();
    }
```

既然如此，这种场景是不是就不能使用Stream了呢？也不是，其实我们**拆开成两个Stream**分步操作下其实就可以了。



再看下面的第二种实现代码：

```java
public PriceResult comparePriceInOnePlat2(List<String> products) {
    // 先触发各自平台的并行处理
    List<CompletableFuture<PriceResult>> completableFutures = products.stream()
            .map(product -> CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouBaoPrice(product)).thenCombine(CompletableFuture.supplyAsync(() -> HttpRequestMock.getMouBaoDiscounts(product)), this::computeRealPrice))
            .collect(Collectors.toList());
    // 在独立的流中，等待所有并行处理结束，做最终结果处理
    return completableFutures.stream()
            .map(CompletableFuture::join)
            .sorted(Comparator.comparingInt(PriceResult::getRealPrice))
            .findFirst()
            .get();
}
```

执行结果：

```ini
07:39:16.072[ForkJoinPool.commonPool-worker-6|17]获取某宝上 Iphone13红色的价格完成： 5199
07:39:16.072[ForkJoinPool.commonPool-worker-9|12]获取某宝上 Iphone13黑色的价格完成： 5199
07:39:16.072[ForkJoinPool.commonPool-worker-2|13]获取某宝上 Iphone13黑色的优惠完成： -200
07:39:16.072[ForkJoinPool.commonPool-worker-11|14]获取某宝上 Iphone13白色的价格完成： 5199
07:39:16.072[ForkJoinPool.commonPool-worker-4|15]获取某宝上 Iphone13白色的优惠完成： -200
07:39:16.072[ForkJoinPool.commonPool-worker-13|16]获取某宝上 Iphone13红色的优惠完成： -200
07:39:16.072[ForkJoinPool.commonPool-worker-2|13]某宝最终价格计算完成：4999
07:39:16.072[ForkJoinPool.commonPool-worker-4|15]某宝最终价格计算完成：4999
07:39:16.072[ForkJoinPool.commonPool-worker-13|16]某宝最终价格计算完成：4999
获取最优价格信息：【平台：某宝, 原价：5199, 折扣：0, 实付价：4999】
-----执行耗时： 1142ms  ------
```

从执行结果可以看出，三个商品并行处理，整体处理耗时相比前面编码方式有很大提升，达到了预期的效果。

**归纳下**：

> 因为Stream的操作具有**延迟执行**的特点，且只有遇到终止操作（比如collect方法）的时候才会真正的执行。所以遇到这种需要并行处理且需要合并多个并行处理流程的情况下，需要将并行流程与合并逻辑放到两个Stream中，这样分别触发完成各自的处理逻辑，就可以了。









