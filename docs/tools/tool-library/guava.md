---
title: Guava
category: 开发工具
tag:
 - 工具类库
---

## 基本工具

### Optional

Guava 的`Optional<T>`是用来处理可能为 null 值的容器类，在业务代码里，应该明确区分 null 和 空 的含义，避免混淆`null/空`的语义，提高程序的健壮性。JDK 8 开始也提供了 `Optional` 提供相同的功能，而且支持函数式编程的特性，因此建议使用 JDK 标准自带的 Optional。

```java
// 1. 创建包含非空值的 Optional
Optional<String> optional = Optional.of("Hello");

// 2. 创建可能为 null 的 Optional
Optional<String> optional = Optional.ofNullable(null);

// 3. 创建空的 Optional
Optional<String> emptyOptional = Optional.empty();

// 4. 判断 Optional 是否包含值并取值，空 Optional 调用 get 会抛出 NoSuchElementException
if(optional.isPresent())
    System.out.println(optional.get());

// 5. 提供默认值
String value = optional.orElse("Default Value");

// 6. 提供默认 Supplier
String value = optional.orElseGet(() -> generateDefaultValue());

// 7. 空 Optional 抛异常
String value = optional.orElseThrow(() -> new IllegalStateException("Value not present"));

// 8. Function 映射，并用 Optional 包装映射结果
Optional<String> transformedOptional = optional.map(value -> value.toUpperCase());

// 9. Function 扁平映射，不会嵌套包装
Optional<String> transformedOptional = optional.flatMap(value -> Optional.of(value.toUpperCase()));
```

### Preconditions

`Preconditions`提供了若干前置条件判断的使用方法，每个方法都有三种参数重载：

1. 仅 boolean 校验，抛出异常，没有错误消息
2. 指定 Object 对象，其 toString() 的结果作为错误消息
3. 指定 String 格式化串作为错误消息，并且可以附加 Object 作为消息的参数

```java
// 校验参数
checkArgument(boolean)

// 校验空值
checkNotNull(T)

// 校验索引
checkElementIndex(int index, int size)

// ......
```

JDK 7 开始提供的 Objects 类也提供了一些类似的功能，具体可以参考 [JDK Docopen in new window](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Objects.html)。

### Objects

```java
// 避免空指针异常
Objects.equal("a", "a");

Objects.hashCode(o1, o2);

MoreObjects.toStringHelper(object).add("key", "val").addValue("End").toString();

// 比较器链，从前往后直到比较到非零的结果结束
ComparisonChain.start()
        .compare(this.aString, that.aString)
        .compare(this.anInt, that.anInt)
        .compare(this.anEnum, that.anEnum, Ordering.natural().nullsLast())
        .result();
```

### Ordering

Guava Fluent 风格的比较器实现，可以构建复杂的逻辑完成集合排序。内置的排序器有以下几个：

| ARBITRARY_ORDERING    | 随机排序                       |
| --------------------- | ------------------------------ |
| NaturalOrdering       | 自然排序                       |
| AllEqualOrdering      | 全等排序，包括 null            |
| UsingToStringOrdering | 按对象的 toString() 做字典排序 |

当然也可以通过继承 Ording 抽象类，以及各种 API 自定义排序逻辑。

```java
// 对元素执行 Function -> null 元素前置 -> 自然排序 -> 逆序
Ordering<Foo> ordering = Ordering.natural().reverse().nullsFirst().onResultOf(new Function<Foo, String>() {
    public String apply(Foo foo) {
    	return foo.sortedBy;
    }
});

// 获取可迭代对象中，按排序器的逻辑最大的 k 个元素
ordering.greatest(Iterator, k);

// 是否已经按排序器有序
ordering.isOrdered(Iterable);

// 按排序器逻辑最小的元素
ordering.min(a, b, c...);
```

### Throwables

看了一下文档和 API，Throwables 工具类貌似没什么实用的意义，官方文档也在考虑这个类的作用，新版已经废弃了一部分 API，参考：[Why we deprecated Throwables.propagate](https://github.com/google/guava/wiki/Why-we-deprecated-Throwables.propagate)

```java
// 如果 t 是 Error/RuntimeException 直接抛出，否则包装成 RuntimeException 抛出
Throwables.propagate(t);

// t 为 aClass 类型才抛出
Throwables.propagateIfInstanceOf(t, aClass);

// t 为 aClass/Error/RuntimeException 才抛出
Throwables.propagateIfPossible(t, aClass);
```

## [集合](https://xchanper.github.io/coding/Guava.html#集合)

### Immutable

在程序设计中使用不可变对象，可以提高代码的可靠性和可维护性，其优势包括：

1. **线程安全性（Thread Safety）**：不可变对象是线程安全的，无需同步操作，避免了竞态条件
2. **安全性**：可以防止在程序运行时被意外修改，提高了程序的安全性
3. **易于理解和测试**：不可变对象在创建后不会发生变化，更容易理解和测试
4. **克隆和拷贝**：不可变对象不需要实现可变对象的复制（Clone）和拷贝（Copy）逻辑，因为它们的状态不可变，克隆即是自己

JDK 的 Collections 提供了 Unmodified Collections 不可变集合，但仅仅是通过装饰器模式提供了一个只读的视图，并没有阻止对原始集合的修改操作，并且效率较低。
而 Guava 提供的不可变集合更加简单高效，确保了真正的不可变性。

```java
// copyOf 会尝试在安全的时候避免做拷贝
ImmutableList<String> immutableList1 = ImmutableList.copyOf(origin);
ImmutableList<String> immutableList2 = ImmutableList.of("A", "B", "C");
ImmutableList<String> immutableList3 = ImmutableList.<String>builder()
                                                .add("A", "B", "C")
                                                .build();

// 任何不可变集合可以转变为 ImmutableList，且该列表视图通常性能很好
ImmutableList<String> list = immutable.asList();
```

### 新集合类型

#### Multiset

Multiset 是一个新的集合类型，可以多次添加相等的元素，既可以看成是无序的列表，也可以看成存储元素和对应数量的键值对映射`[E1: cnt1; E2:cnt2]`。常用实现包括 `HashMultiset, TreeMultiset, LinkedHashMultiset...`

```java
Multiset<String> multiset = HashMultiset.create();
multiset.add("A");
multiset.add("A");
multiset.add("B");
// 输出：[A x 2, B]
log.debug("{}", multiset);

// 元素总数
log.debug("{}", multiset.size());
// 不重复元素个数
log.debug("{}", multiset.elementSet().size());
// 设置元素计数
multiset.setCount("A", 3);
// 获取元素个数
log.debug("{}", multiset.count("A"));
```

#### Multimap

支持将 key 映射到多个 value 的方式，而不用定义`Map<K, List<V>> 或 Map<K, Set<V>>`这样的哈皮形式。实现类包括`ArrayListMultimap, HashMultimap, LinkedListMultimap, TreeMultimap...`

```java
// 列表实现
ListMultimap<String, Integer> listMultimap = MultimapBuilder.hashKeys().arrayListValues().build();
// 集合实现
SetMultimap<String, Integer> setMultimap = MultimapBuilder.treeKeys().hashSetValues().build();

listMultimap.put("A", 1);
listMultimap.put("A", 2);
listMultimap.put("B", 1);
// {A=[1, 2], B=[1, 2]}
log.debug("{}", listMultimap);
// [1, 2]，不存在则返回一个空集合
log.debug("{}", listMultimap.get("A"));
// [1, 2] 移除 key 关联的所有 value
List<Integer> valList = listMultimap.removeAll("A");

// 返回普通 map 的视图，仅支持 remove，不能 put，且会更新原始的 listMultimap
Map<String, Collection<Integer>> map = listMultimap.asMap();
```

#### BiMap

Map 可以实现 key -> value 的映射，如果想要 value -> key 的映射，就需要定义两个 Map，并且同步更新，很不优雅。Guava 提供了 BiMap 支持支持双向的映射关系，常用实现有`HashMap, EnumBiMap, EnumHashBiMap...`

```java
BiMap<String, Integer> biMap = HashBiMap.create();
biMap.put("A", 100);

// 删除已存在的 KV，重新添加 KV
biMap.forcePut("A", 200);

// 获取反向映射
BiMap<Integer, String> inverse = biMap.inverse();
log.debug("{}", inverse.get(100));
```

#### Table

当需要同时对多个 key 进行索引时，需要定义`Map<key1, Map<key2, val>>`这样的形式，ugly and awkward。Guava 提供了 Table 用于支持类似 row、column 的双键映射。实现包括`HashBasedTable, TreeBasedTable, ArrayTable...`

```java
// Table<R, C, V>
Table<String, String, Integer> table = HashBasedTable.create();
table.put("row1", "col1", 1);
table.put("row1", "col2", 2);
table.put("row2", "col1", 3);
table.put("row2", "col2", 4);

// 获取类似 Map.Entry 的 Table.Cell<R, C, V>
table.cellSet().forEach(System.out::println);

// Map<R, Map<C, V>> 视图
Map<String, Map<String, Integer>> rowMap = table.rowMap();
Set<String> rowKeySet = table.rowKeySet();
// Map<C, Map<R, V>> 视图（基于列的索引效率通常比行低）
Map<String, Map<String, Integer>> columnMap = table.columnMap();
Set<String> columnedKeySet = table.columnKeySet();
```

#### 其它

- ClassToInstanceMap：Class -> Instance 的映射，可以避免强制类型转换
- RangeSet (Beta状态)：范围集合，自动合并、分解范围区间

```java
RangeSet<Integer> rangeSet = TreeRangeSet.create();
rangeSet.add(Range.closed(1, 10)); // {[1, 10]}
rangeSet.add(Range.closedOpen(11, 15)); // disconnected range: {[1, 10], [11, 15)}
rangeSet.add(Range.closedOpen(15, 20)); // connected range; {[1, 10], [11, 20)}
rangeSet.add(Range.openClosed(0, 0)); // empty range; {[1, 10], [11, 20)}
rangeSet.remove(Range.open(5, 10)); // splits [1, 10]; {[1, 5], [10, 10], [11, 20)}

// [[1..5], [10..10], [11..20)]
System.out.println(rangeSet);
```

- RangeMap (Beta状态)：range -> value 的映射，不会自动合并区间

```java
RangeMap<Integer, String> rangeMap = TreeRangeMap.create();
rangeMap.put(Range.closed(1, 10), "foo"); // {[1, 10] => "foo"}
rangeMap.put(Range.open(3, 6), "bar"); // {[1, 3] => "foo", (3, 6) => "bar", [6, 10] => "foo"}
rangeMap.put(Range.open(10, 20), "foo"); // {[1, 3] => "foo", (3, 6) => "bar", [6, 10] => "foo", (10, 20) => "foo"}
rangeMap.remove(Range.closed(5, 11)); // {[1, 3] => "foo", (3, 5) => "bar", (11, 20) => "foo"}

// [[1..3]=foo, (3..5)=bar, (11..20)=foo]
System.out.println(rangeMap);
```

### 集合工具

JDK 自带的`java.util.Collections`提供了很多实用的功能，而 Guava 针对特定接口提供了更多工具，例如：

- 提供很多静态工厂方法、包装器
- Iterables 支持懒加载的集合视图操作
- Sets 提供集合论运算
- Maps 提供 diff 计算
- Multimaps 提供 Map -> Multimap 的转换，并支持反向映射
- Tables 提供行列转置 | **Interface** | **JDK or Guava** | **Guava Utility Class** | | --- | --- | --- | | Collection | JDK | Collections2 | | List | JDK | Lists | | Set | JDK | Sets | | Map | JDK | Maps | | Queue | JDK | Queues | | Multiset | Guava | Multisets | | Multimap | Guava | Multimaps | | BiMap | Guava | Maps | | Table | Guava | Tables |

另外，还可以继承 Forwarding 通过装饰器模式装饰特殊实现，PeekingIterator 可以 peek 下一次返回的元素，AbstractIterator 自定义迭代方式等等。（PS：这些东西也许有点用，可是能用吗，业务里写这种代码怕是要被人喷死...）

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

## 并发

### ListenableFuture

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

### Service

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

## Strings

Guava 提供了一系列用于字符串处理的工具：

1. **Joiner**：字符串拼接工具，创建的都是不可变实例

```java
Joiner joiner = Joiner.on(";").useForNull("^");
// "A;B;^;D"
String joined = joiner.join("A", "B", null, "D");
```

2. **Splitter**: 字符串分割工具，创建的也是不可变实例

```java
// String#split 反直觉的输出：["", "a", "", "b"]
Arrays.stream(",a,,b,".split(",")).toList().forEach(System.out::println);
// ["foo", "bar", "qux"]
Iterable<String> split = Splitter.on(",")
    	// 结果自动 trim
        .trimResults()
    	// 忽略结果中的空串
        .omitEmptyStrings()
        // 限制分割数
        .limit(3)
        .split("foo,bar,,   qux");
Map<String, String> splitMap = Splitter.on(";")
	// 指定 K-V 的分隔符可以将键值对的串解析为 Map
	.withKeyValueSeparator("->")
	.split("A->1;B->2");
```

3. CharMatchers：字符序列匹配和处理的工具，内置了大量常用的匹配器。使用上通常分两步：

- 确定匹配的字符和模式
- 用匹配的字符做处理

```java
// 确定匹配的字符和模式，例如 anyOf, none, whitespace, digit, javaLetter, javaIsoControl...
CharMatcher matcher = CharMatcher.anyOf("abc");
// defg
log.debug("{}", matcher.removeFrom("abcdefg"));
// abc
log.debug("{}", matcher.retainFrom("abcdefg"));
// true
log.debug("{}", matcher.matchesAllOf("abc"));
// hhh 
log.debug("{}", matcher.trimFrom("abchhhabc"));
// ___hhh___
log.debug("{}", matcher.replaceFrom("abc hhh abc", "_"));
```

4. **Charsets**: 提供了6种标准的字符集常量引用，例如`Charsets.UTF_8`。JDK 7 以后建议使用内置的 `StandardCharsets`
5. **CaseFormat**: 大小写转换的工具

```java
// UPPER_UNDERSCORE -> LOWER_CAMEL
CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, "CONSTANT_NAME"));
```

| **Format**                                                   | **Example**      |
| ------------------------------------------------------------ | ---------------- |
| [LOWER_CAMELopen in new window](http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/base/CaseFormat.html#LOWER_CAMEL) | lowerCamel       |
| [LOWER_HYPHENopen in new window](http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/base/CaseFormat.html#LOWER_HYPHEN) | lower-hyphen     |
| [LOWER_UNDERSCOREopen in new window](http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/base/CaseFormat.html#LOWER_UNDERSCORE) | lower_underscore |
| [UPPER_CAMELopen in new window](http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/base/CaseFormat.html#UPPER_CAMEL) | UpperCamel       |
| [UPPER_UNDERSCOREopen in new window](http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/base/CaseFormat.html#UPPER_UNDERSCORE) | UPPER_UNDERSCORE |

5. **Strings**：也提供了几个没什么大用的小工具



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



## IO

Guava 使用术语 **流 **来表示可关闭的，并且在底层资源中有位置状态的 I/O 数据流。字节流对应的工具类为 ByteSterams，字符流对应的工具类为 CharStreams。
Guava 中为了避免和流直接打交道，抽象出可读的 **源 source** 和可写的 **汇 sink** 两个概念，指可以从中打开流的资源，比如 File、URL，同样也分别有字节和字符对应的源和汇，定义了一系列读写的方法。
除此之外，Files 工具类提供了对文件的操作。（PS：个人觉得，JDK 的 IO 流已经够麻烦的了，又来一套 API 太乱了，而且也没有更好用吧。还是先统一用 JDK 标准库里的好一点。）



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