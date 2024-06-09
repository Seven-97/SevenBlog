---
title: Caffeine - 同步&异步回源方式
category: 系统设计
tag:
 - 缓存
 - Caffeine
---



> 来源：稀土掘金社区[深入理解缓存原理与实战设计](https://juejin.cn/column/7140852038258147358)，Seven进行了部分补充完善



作为一种对外提供黑盒缓存能力的专门组件，`Caffeine`基于**穿透型缓存**模式进行构建。也即对外提供数据查询接口，会优先在缓存中进行查询，若命中缓存则返回结果，未命中则尝试去真正的源端（如：数据库）去获取数据并回填到缓存中，返回给调用方。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092059826.webp)

与Guava Cache相似，*Caffeine*的**回源**填充主要有两种手段：

- `Callable`方式
- `CacheLoader`方式

根据执行调用方式不同，又可以细分为**同步阻塞**方式与**异步非阻塞**方式。

本文我们就一起探寻下Caffeine的多种不同的数据回源方式，以及对应的实际使用。



## 同步方式

**同步**方式是最常被使用的一种形式。查询缓存、数据回源、数据回填缓存、返回执行结果等一系列操作都是在一个调用线程中**同步阻塞**完成的。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092100692.webp)



### Callable

在每次`get`请求的时候，传入一个Callable函数式接口具体实现，当没有命中缓存的时候，Caffeine框架会执行给定的Callable实现逻辑，去获取真实的数据并且回填到缓存中，然后返回给调用方。

```java
public static void main(String[] args) {
    Cache<String, User> cache = Caffeine.newBuilder().build();
    User user = cache.get("123", s -> userDao.getUser(s));
    System.out.println(user);
}
```

`Callable`方式的回源填充，有个明显的**优势**就是调用方可以根据自己的场景，*灵活*的给定不同的回源执行逻辑。但是这样也会带来一个**问题**，就是如果需要获取缓存的地方太多，会导致每个调用的地方都得指定下对应Callable回源方法，调用起来比较**麻烦**，且对于需要保证回源逻辑统一的场景*管控能力不够强势*，无法约束所有的调用方使用相同的回源逻辑。

这种时候，便需要`CacheLoader`登场了。



### CacheLoader

在创建缓存对象的时候，可以通在`build()`方法中传入指定的**CacheLoader**对象的方式来指定回源时默认使用的回源数据加载器，这样当使用方调用`get`方法获取不到数据的时候，框架就会自动使用给定的**CacheLoader**对象执行对应的数据加载逻辑。

比如下面的代码中，便在创建缓存对象时指定了当缓存未命中时通过`userDao.getUser()`方法去*DB*中执行数据查询操作：

```java
public LoadingCache<String, User> createUserCache() {
    return Caffeine.newBuilder()
            .maximumSize(10000L)
            .build(key -> userDao.getUser(key));
}
```

相比于Callable方式，CacheLoader更**适用**于*所有回源场景使用的回源策略都固定且统一*的情况。对具体业务使用的时候更加的友好，调用`get`方法也更加简单，只需要传入带查询的`key`值即可。

上面的示例代码中还有个需要关注的点，即创建缓存对象的时候指定了CacheLoader，最终创建出来的缓存对象是**LoadingCache**类型，这个类型是Cache的一个子类，扩展提供了*无需传入Callable参数的get方法*。进一步地，我们打印出对应的详细类名，会发现得到的缓存对象具体类型为：

```
com.github.benmanes.caffeine.cache.BoundedLocalCache.BoundedLocalLoadingCache
```

当然，如果创建缓存对象的时候没有指定最大容量限制，则创建出来的缓存对象还可能会是下面这个：

```
com.github.benmanes.caffeine.cache.UnboundedLocalCache.UnboundedLocalManualCache
```

通过`UML图`，可以清晰的看出其与Cache之间的继承与实现链路情况：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092100430.webp)

因为LoadingCache是Cache对象的子类，根据JAVA中类继承的特性，`LoadingCache`也完全具备Cache所有的接口能力。所以，对于大部分场景都需要固定且统一的回源方式，但是某些特殊场景需要自定义回源逻辑的情况，也可以通过**组合使用**Callable的方式来实现。

比如下面这段代码：

```java
public static void main(String[] args) {
    LoadingCache<String, User> cache = Caffeine.newBuilder().build(userId -> userDao.getUser(userId));
    // 使用CacheLoader回源
    User user = cache.get("123");
    System.out.println(user);
    // 使用自定义Callable回源
    User techUser = cache.get("J234", userId -> {
        // 仅J开头的用户ID才会去回源
        if (!StringUtils.isEmpty(userId) && userId.startsWith("J")) {
            return userDao.getUser(userId);
        } else {
            return null;
        }
    });
    System.out.println(techUser);
}
```

上述代码中，构造的是一个指定了CacheLoader的LoadingCache缓存类型，这样对于大众场景可以直接使用`get`方法由CacheLoader提供**统一**的回源能力，而特殊场景中也可以在`get`方法中传入需要的**定制化回源**Callable逻辑。



### 不回源

在实际的缓存应用场景中，并非是所有的场景都要求缓存没有命中的时候要去执行回源查询。对于一些业务规划上无需执行回源操作的请求，也可以要求Caffeine不要执行回源操作（比如黑名单列表，只要用户在黑名单就禁止操作，不在黑名单则允许继续往后操作，因为大部分请求都不会命中到黑名单中，所以不需要执行回源操作）。为了实现这一点，在查询操作的时候，可以使用Caffeine提供的**免回源查询**方法来实现。

具体梳理如下：

| 接口          | 功能说明                                                     |
| ------------- | ------------------------------------------------------------ |
| getIfPresent  | 从内存中查询，如果存在则返回对应值，不存在则返回null         |
| getAllPresent | 批量从内存中查询，如果存在则返回存在的键值对，不存在的key则不出现在结果集里 |

代码使用演示如下：

```java
public static void main(String[] args) {
    LoadingCache<String, User> cache = Caffeine.newBuilder().build(userId -> userDao.getUser(userId));
    cache.put("124", new User("124", "张三"));
    User userInfo = cache.getIfPresent("123");
    System.out.println(userInfo);
    Map<String, User> presentUsers =
            cache.getAllPresent(Stream.of("123", "124", "125").collect(Collectors.toList()));
    System.out.println(presentUsers);
}
```

执行结果如下，可以发现执行的过程中并**没有触发**自动回源与回填操作：

```ini
null
{124=User(userName=张三, userId=124)}
```



## 异步方式

[`CompletableFuture`](https://www.seven97.top/java/concurrent/05-concurrenttools7-completablefuture.html)并行流水线能力，是`JAVA8`在**异步编程**领域的一个重大改进。可以将一系列耗时且无依赖的操作改为并行同步处理，并等待各自处理结果完成后继续进行后续环节的处理，由此来降低阻塞等待时间，进而达到降低请求链路时长的效果。

Caffeine完美的支持了在异步场景下的**流水线**处理使用场景，回源操作也支持**异步**的方式来完成。

### 异步Callable

要想支持异步场景下使用缓存，则创建的时候必须要创建一个异步缓存类型，可以通过`buildAsync()`方法来构建一个**AsyncCache**类型缓存对象，进而可以在异步场景下进行使用。

看下面这段代码：

```java
public static void main(String[] args) {
    AsyncCache<String, User> asyncCache = Caffeine.newBuilder().buildAsyn();
    CompletableFuture<User> userCompletableFuture = asyncCache.get("123", s -> userDao.getUser(s));
    System.out.println(userCompletableFuture.join());
}
```

上述代码中，get方法传入了Callable回源逻辑，然后会开始**异步**的加载处理操作，并返回了个CompletableFuture类型结果，最后如果需要获取其实际结果的时候，需要等待其异步执行完成然后获取到最终结果（通过上述代码中的`join()`方法等待并获取结果）。

我们可以比对下*同步*和*异步*两种方式下`Callable`逻辑执行线程情况。看下面的代码：

```java
public static void main(String[] args) {
    System.out.println("main thread:" + Thread.currentThread().getId());
    // 同步方式
    Cache<String, User> cache = Caffeine.newBuilder().build();
    cache.get("123", s -> {
        System.out.println("同步callable thread:" + Thread.currentThread().getId());
        return userDao.getUser(s);
    });
    // 异步方式
    AsyncCache<String, User> asyncCache = Caffeine.newBuilder().buildAsync();
    asyncCache.get("123", s -> {
        System.out.println("异步callable thread:" + Thread.currentThread().getId());
        return userDao.getUser(s);
    });
}
```

执行结果如下：

```arduino
main thread:1
同步callable thread:1
异步callable thread:15
```

结果很明显的可以看出，**同步**处理逻辑中，回源操作直接占用的*调用线程*进行操作，而**异步**处理时则是*单独线程*负责回源处理、**不会阻塞**调用线程的执行 —— 这也是异步处理的优势所在。

看到这里，也许会有小伙伴有疑问，虽然是异步执行的回源操作，但是最后还是要在调用线程里面阻塞等待异步执行结果的完成，似乎没有看出异步有啥优势？

异步处理的魅力，在于当一个耗时操作执行的同时，主线程可以继续去处理其它的事情，然后其余事务处理完成后，直接去取异步执行的结果然后继续往后处理。如果主线程无需执行其余处理逻辑，完全是阻塞等待异步线程加载完成，这种情况确实没有必要使用异步处理。

想象一个生活中的场景：

> 周末休息的你出去逛街，去咖啡店点了一杯咖啡，然后服务员会给你一个订单小票。 当服务员在后台制作咖啡的时候，你并没有在店里等待，而是出门到隔壁甜品店又买了个面包。 当面包买好之后，你回到咖啡店，拿着订单小票去取咖啡。 取到咖啡后，你边喝咖啡边把面包吃了……嗝~

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092104888.webp)

这种情况应该比较好理解了吧？如果是同步处理，你买咖啡的时候，需要在咖啡店一直等到咖啡做好然后才能再去甜品店买面包，这样耗时就比较长了。而采用异步处理的策略，你在等待咖啡制作的时候，继续去甜品店将面包买了，然后回来等待咖啡完成，这样整体的时间就缩短了。当然，如果你只想买个咖啡，也不需要买甜品面包，即你等待咖啡制作期间没有别的事情需要处理，那这时候你在不在咖啡店一直等到咖啡完成，都没有区别。

回到代码层面，下面代码演示了异步场景下`AsyncCache`的使用。

```java
public boolean isDevUser(String userId) {
    // 获取用户信息
    CompletableFuture<User> userFuture = asyncCache.get(userId, s -> userDao.getUser(s));
    // 获取公司研发体系部门列表
    CompletableFuture<List<String>> devDeptFuture =
            CompletableFuture.supplyAsync(() -> departmentDao.getDevDepartments());
    // 等用户信息、研发部门列表都拉取完成后，判断用户是否属于研发体系
    CompletableFuture<Boolean> combineResult =
            userFuture.thenCombine(devDeptFuture,
                    (user, devDepts) -> devDepts.contains(user.getDepartmentId()));
    // 等待执行完成，调用线程获取最终结果
    return combineResult.join();
}
```

在上述代码中，需要获取到用户详情与研发部门列表信息，然后判断用户对应的部门是否属于研发部门，从而判断员工是否为研发人员。整体采用**异步**编程的思路，并使用了Caffeine`异步缓存`的操作方式，实现了用户获取与研发部门列表获取这两个耗时操作并行的处理，提升整体处理效率。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092104868.webp)





### 异步CacheLoader

异步处理的时候，Caffeine也支持直接在创建的时候指定CacheLoader对象，然后生成支持异步回源操作的`AsyncLoadingCache`缓存对象，然后在使用`get`方法获取结果的时候，也是返回的`CompletableFuture`异步封装类型，满足在异步编程场景下的使用。

```java
public static void main(String[] args) {
    try {
        AsyncLoadingCache<String, User> asyncLoadingCache =
                Caffeine.newBuilder().maximumSize(1000L).buildAsync(key -> userDao.getUser(key));
        CompletableFuture<User> userCompletableFuture = asyncLoadingCache.get("123");
        System.out.println(userCompletableFuture.join());
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```



### 异步AsyncCacheLoader

除了上述这种方式，在创建的时候给定一个用于回源处理的CacheLoader之外，Caffeine还有一个`buildAsync`的重载版本，允许传入一个同样是支持异步并行处理的`AsyncCacheLoader`对象。使用方式如下：

```java
public static void main(String[] args) {
    try {
        AsyncLoadingCache<String, User> asyncLoadingCache =
                Caffeine.newBuilder().maximumSize(1000L).buildAsync(
                        (key, executor) -> CompletableFuture.supplyAsync(() -> userDao.getUser(key), executor)
                );
        CompletableFuture<User> userCompletableFuture = asyncLoadingCache.get("123");
        System.out.println(userCompletableFuture.join());
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

与上一章节中的代码比对可以发现，不管是使用`CacheLoader`还是`AsyncCacheLoader`对象，最终生成的缓存类型都是**AsyncLoadingCache**类型，使用的时候也并没有实质性的差异，两种方式的差异点仅在于传入`buildAsync`方法中的对象类型不同而已，使用的时候可以根据喜好自行选择。

进一步地，如果我们尝试将上面代码中的`asyncLoadingCache`缓存对象的具体类型打印出来，我们会发现其具体类型可能是：

```
com.github.benmanes.caffeine.cache.BoundedLocalCache.BoundedLocalAsyncLoadingCache
```

而如果我们在构造缓存对象的时候没有限制其最大容量信息，其构建出来的缓存对象类型还可能会是下面这个：

```
com.github.benmanes.caffeine.cache.UnboundedLocalCache.UnboundedLocalAsyncLoadingCache
```

与前面同步方式一样，我们也可以看下这两个具体的缓存类型对应的`UML类`图关系：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092206121.webp)

可以看出，异步缓存不同类型最终都实现了同一个AsyncCache顶层接口类，而`AsyncLoadingCache`作为继承自*AsyncCache*的子类，除具备了AsyncCache的所有接口外，还额外扩展了部分的接口，以支持未命中目标时自动使用指定的CacheLoader或者AysncCacheLoader对象去执行回源逻辑。

