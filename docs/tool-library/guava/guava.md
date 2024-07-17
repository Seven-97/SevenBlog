---
title: Guava
category: 开发工具
tag:
 - 工具类库
 - Guava
---



## 缓存

Guava 提供的 Cache 按照 **有则取值，无则计算 **的逻辑，支持自动装载，自动移除等扩展功能，比传统的`ConcurrentHashMap`功能更加强大。

### 使用

除了继承 AbstractCache 自定义缓存实现外，通常可以直接使用 CacheBuilder 构建一个 LocalLoadingCache 缓存，通过 key 来懒加载相关联的 value。如果 key-value 没有关联关系可以使用无参的 build 方法返回一个`LocalManualCache`对象，等调用 get 时再传递一个 callable 对象获取 value。

```java
LoadingCache<String, Integer> loadingCache = CacheBuilder.newBuilder()
                // 最大容量
				.maximumSize(1000)
            	// 定时刷新
				.refreshAfterWrite(10, TimeUnit.MINUTES)
                // 添加移除时的监听器
                .removalListener(notification -> System.out.println(
                    notification.getKey() + " -> " + 
                    notification.getValue() + " is removed by " + 
                    notification.getCause()))
            	// 并发等级
				.concurrencyLevel(4)
				// 开启统计功能
            	.recordStats()
                .build(
                        new CacheLoader<String, Integer>() {
                            @Override
                            public Integer load(String key) {
                                System.out.println("Loading key: " + key);
                                return Integer.parseInt(key);
                            }
                        });

loadingCache.get("key");
// 使失效
loadingCache.invalidate("key");
// get with callable
loadingCache.get("key", () -> 2));
// 刷新缓存
loadingCache.refresh("key");
// 返回一个 map 视图
ConcurrentMap<String, Integer> map = loadingCache.asMap();
```

`LocalLoadingCache`继承自 `LocalManualCache`，里面封装了一个继承自 Map 的`localCache`成员存储实际的 KV 并通过分段锁实现线程安全，另外实现了 Cache 接口定义了一系列缓存操作。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406262329345.jpeg)

```java
class LocalCache<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>
```

### 失效

Guava 提供了三种基础的失效策略：

- 基于容量失效：
  - `maximumSize(long)` 指定最大容量
  - `weigher(Weigher)`实现自定义权重，配合`maximumWeight(long)`以权重作为容量
- 基于时间失效：在写入、偶尔读取期间执行定期维护
  - `expireAfterAccess(long, timeUnit)` 读写后超出指定时间即失效
  - `expireAfterWrite(long, timeUnit)` 写后超出指定时间即失效
- 基于引用失效：
  - `weakKeys()`通过弱引用存储 key
  - `weakValues()`通过弱引用存储 value
  - `softValues()` 通过软引用包装 value，以 LRU 方式进行 GC

另外，也可以通过`invalidate`手动清除缓存。缓存不会自动进行清理，Guava 会在写操作期间，或者偶尔在读操作时进行过期失效的维护工作。缓存刷新操作的时机也是类似的。

详情可以看这篇文章 [GuavaCache](https://www.seven97.top/system-design/cache-column/guavacache.html)

## 图

`com.google.common.graph`提供了多种图的实现：

- **Graph** 边是匿名的，且不关联任何信息
- **ValueGraph** 边拥有自己的值，如权重、标签
- **Network **边对象唯一，且期望实施对其引用的查询

整体看下来，感觉还是挺复杂的，不太常用。等有需要再学习吧...



## 基本类型工具

Java 的基本类型包括8个：byte、short、int、long、float、double、char、boolean。Guava 提供了若干工具以支持基本类型和集合 API 的交互、字节数组转换、无符号形式的支持等等。

| **基本类型** | **Guava 工具类**                    |
| ------------ | ----------------------------------- |
| byte         | Bytes, SignedBytes, UnsignedBytes   |
| short        | Shorts                              |
| int          | Ints, UnsignedInteger, UnsignedInts |
| long         | Longs, UnsignedLong, UnsignedLongs  |
| float        | Floats                              |
| double       | Doubles                             |
| char         | Chars                               |
| boolean      | Booleans                            |

## Range

Guava 提供了 Range 类以支持范围类型，并且支持范围的运算，比如包含、交集、并集、查询等等。

```java
Range.open(a, b);			// (a, b)
Range.closed(a, b);			// [a..b]
Range.closedOpen(a, b);		// [a..b)
Range.openClosed(a, b);		// (a..b]
Range.greaterThan(a);		// (a..+∞)
Range.atLeast(a);			// [a..+∞)
Range.lessThan(a);			// (-∞..b)
Range.atMost(a);			// (-∞..b]
Range.all();				// (-∞..+∞)

// 通用创建方式
Range.range(a, BoundType.CLOSED, b, BoundType.OPEN);
```





## Hash

JDK 内置的哈希限定为 32 位的 int 类型，虽然速度很快但质量一般，容易产生碰撞。为此 Guava 提供了自己的 Hash 包。

- `Hashing` 类内置了一系列的散列函数对象 `HashFunction`，包括 `murmur3, sha256, adler32, crc32`等等。
- 确定 HashFunction 后进而拿到继承自 PrimitiveSink 的 Hasher 对象。
- 作为一个汇，可以往 Hasher 里输入数据，可以是内置类型，也可以是自定义类型，但需要传递一个 Funnel 定义对象分解的方式。
- 最后计算得到 HashCode 对象。

```java
HashFunction hf = Hashing.adler32();

User user = new User("chanper", 24);
HashCode hash = hf.newHasher()
        .putLong(20L)
        .putString("chanper", StandardCharsets.UTF_8)
    	// 输入自定义类，同时需要一个 Funnel
		.putObject(user, userFunnel)
        .hash();

// Funnel 定义对象分解的方式
Funnel<User> userFunnel = new Funnel<>() {
    @Override
    public void funnel(User user, PrimitiveSink into) {
        into
            .putString(user.name(), StandardCharsets.UTF_8)
            .putInt(user.age());
    }
};
```

另外，Guava 库也内置了一个使用简便布隆过滤器。

```java
// funnel 对象，预期的插入数量，false positive probability
BloomFilter<User> friends = BloomFilter.create(userFunnel, 500, 0.01);
for (int i = 0; i < 1000; i++) {
    friends.put(new User("user_" + i, 24));
}

if(friends.mightContain(somebody)) {
    System.out.println("somebody is in friends");
}
```

## EventBus

EventBus 是 Guava 提供的一个事件总线库，用于简化组件之间的通信。通过 EventBus，你可以实现发布/订阅模式，组件之间可以松散地耦合，使得事件的发布者（Producer）和订阅者（Subscriber）之间不需要直接依赖彼此。 使用时注意：

- 一个订阅者可以处理多个不同的事件，取决于处理方法的参数，并且支持泛型的通配符
- 没有对应的监听者则会把事件封装为`DeadEvent`，可以定义对应的监听器

```java
// 事件类型
public record MessageEvent(String message) {}

public class MessageSubscriber {
    // 事件处理方法标记
    @Subscribe
    public void handleMessageEvent(MessageEvent event) {
        System.out.println("Received message: " + event.message());
    }

    // 一个订阅者可以处理多个事件
    @Subscribe
    public void handleMessageEvent(MessageEvent2 event2) {
        System.out.println("Received message2: " + event2.message());
    }
}

@Test
public void testEvnetBus() {
    // 创建EventBus实例
    EventBus eventBus = new EventBus();
    
    // 注册订阅者
    MessageSubscriber subscriber = new MessageSubscriber();
    eventBus.register(subscriber);
    
    // 发布事件
    MessageEvent event = new MessageEvent("Hello, EventBus!");
    eventBus.post(event);
}
```