---
title: Java 25 新特性
category: Java
tags:
  - 版本新特性
head:
  - - meta
    - name: keywords
      content: Java,版本新特性,Java25
  - - meta
    - name: description
      content: 全网最全的Java 版本新特性知识点总结，让天下没有难学的八股文！
---
JDK 25 是 LTS（长期支持版），至此为止，有 JDK8、JDK11、JDK17、JDK21 和 JDK 25 这四个长期支持版了。

JDK 25 共有 18 个新特性，这篇文章会挑选其中较为重要的一些新特性进行详细介绍


## 正式特性

### 作用域值（ScopedValue）  

替代`ThreadLocal`，支持线程间安全共享不可变数据，简化生命周期管理。

>JDK19的[JEP 428: Structured Concurrency (Incubator)](https://link.segmentfault.com/?enc=HsU5iGHvjvOxf%2FjVdcXN7g%3D%3D.GKjKdrSxUrnjT71QfZlB6BpBaxCdMiJ5ZZpvs4iP5%2BQ%3D)作为第一次incubator  
JDK20的[JEP 437: Structured Concurrency (Second Incubator)](https://link.segmentfault.com/?enc=83T829PetTQfXnYlPwCpuw%3D%3D.QApbiJm8c9c7szwz%2FWvfuKkSGwUKcA6I6lcOHYsE6BE%3D)作为第二次incubator  
JDK21的[JEP 453: Structured Concurrency (Preview)](https://link.segmentfault.com/?enc=0KhEXaNFGmTvZDwDeiuImQ%3D%3D.dG23FYRQASUxKhb3HvfMlXybnSPuOxwNkgL3SXhQyQI%3D)作为首次preview  
JDK22的[JEP 462: Structured Concurrency (Second Preview)](https://link.segmentfault.com/?enc=1oOoNAwQC92P1prPN332vg%3D%3D.IB6kJgftjJlcD1tDUMVtS3kTtp9LvGBXlyyKu%2Fz6iTQ%3D)作为第二次preview  
JDK23的[JEP 480: Structured Concurrency (Third Preview)](https://link.segmentfault.com/?enc=Sg1XbrLe84dOe%2F8WN1R03Q%3D%3D.jud%2BG05iWxMHjB3Cskh2sVmZXYra%2FMfE6DGFrFd6ZAM%3D)作为第三次preview  
JDK24的[JEP 487: Scoped Values (Fourth Preview)](https://link.segmentfault.com/?enc=CbumIuiLkwmq9ASZwj0aFg%3D%3D.Av4NGoPoRKeDg9ouKHkO7o%2BRULH5LQ%2BHmdIMpFnxLC4%3D)作为第四次preview，与JDK23不同的是callWhere以及runWhere方法从ScopedValue类中移除，可以使用ScopedValue.where()再链式调用run(Runnable)或者call(Callable)

ScopedValue 在JDK25 正式转正，有个改动就是 ScopedValue.orElse 方法不再接受null作为参数

```java
class Framework {
    private static final ScopedValue<FrameworkContext> CONTEXT
                        = ScopedValue.newInstance();    // (1)

    void serve(Request request, Response response) {
        var context = createContext(request);
        where(CONTEXT, context)                         // (2)
                   .run(() -> Application.handle(request, response));
    }
    
    public PersistedObject readKey(String key) {
        var context = CONTEXT.get();                    // (3)
        var db = getDBConnection(context);
        db.readKey(key);
    }
}
```

作用域值通过其“写入时复制”(copy-on-write)的特性，保证了数据在线程间的隔离与安全，同时性能极高，占用内存也极低。这个特性将成为未来 Java 并发编程的标准实践。

### 模块导入声明

该特性第一次预览是由 JEP 476（JDK 23 ）提出，随后在 JEP 494 （JDK 24）中进行了完善，JDK 25 顺利转正。

支持`import module`语句声明模块依赖，替代部分包导入，提升代码可读性和工具链兼容性，例如：

```java
import module java.base;  // 包含了import java.io.*; import java.util.*;

import module java.base;      // exports java.util, which has a public Date class
import module java.sql;       // exports java.sql, which has a public Date class

import java.sql.Date;         // resolve the ambiguity of the simple name Date!

...
Date d = ...                  // Ok!  Date is resolved to java.sql.Date
...
```


### 压缩源文件与实例主方法

该特性第一次预览是由 JEP 445（JDK 21 ）提出，随后经过了 JDK 22 、JDK 23 和 JDK 24 的改进和完善，最终在 JDK 25 顺利转正。

简化程序入口，支持类级别的`void main()`方法，无需`public static`修饰，允许我们在没有类声明的情况下编写脚本或演示：

```java
void main() {        
	 System.out.println("Hello Java 25!");
}
```

这是为了降低 Java 的学习门槛和提升编写小型程序、脚本的效率而迈出的一大步。初学者不再需要理解 `public static void main(String[] args)` 这一长串复杂的声明。对于快速原型验证和脚本编写，这也使得 Java 成为一个更有吸引力的选择。

### 新增IO类

Java 25 在 java.lang 包中新增了 IO 类，提供更简单的控制台 I/O 操作

```java
void main() {
    String name = IO.readln("请输入您的姓名: ");
    IO.print("很高兴认识您，");
    IO.println(name);
}
```

主要方法

```java
public static void print(Object obj);
public static void println(Object obj);
public static void println();
public static String readln(String prompt);
public static String readln();
```


### 灵活的构造函数体

该特性第一次预览是由 JEP 447（JDK 22）提出，随后在 JEP 482 （JDK 23）和 JEP 492（JDK 24）经历了预览，JDK 25 顺利转正。

Java 要求在构造函数中，`super(...)` 或 `this(...)` 调用必须作为第一条语句出现。这意味着我们无法在调用父类构造函数之前在子类构造函数中直接初始化字段。

灵活的构造函数体解决了这一问题，它允许在构造函数体内，在调用 `super(..)` 或 `this(..)` 之前编写语句，这些语句可以初始化字段，但不能引用正在构造的实例。这样可以防止在父类构造函数中调用子类方法时，子类的字段未被正确初始化，增强了类构造的可靠性。


```java
   class User {
       private final String id;
       User(String rawId) {
           super();
           this.id = validateAndFormat(rawId);
       }
  }
```

### 密钥派生函数 API

随着量子计算技术的发展，传统的密码学算法面临威胁，后量子密码学成为必然趋势。因此 Java 也顺应时代，推出了密钥派生函数(KDF)，这是一种从初始密钥材料、盐值等输入生成新密钥的加密算法。

简单来说，你理解为Java 出了一个新的加密工具类就好了，适用于对密码进行加强、从主密钥派生多个子密钥的场景。

核心是 javax.crypto.KDF 类，提供了两个主要方法
1. deriveKey() 生成 SecretKey 对象
2. deriveData()  生成字节数组

比如使用 HKDF(HMAC-based Key Derivation Function)算法

```java
// 创建 HKDF 实例
KDF hkdf = KDF.getInstance("HKDF-SHA256");

// 准备初始密钥材料和盐值
byte[] initialKeyMaterial = "my-secret-key".getBytes();
byte[] salt = "random-salt".getBytes();
byte[] info = "application-context".getBytes();

// 创建 HKDF 参数
AlgorithmParameterSpec params = HKDFParameterSpec.ofExtract()
    .addIKM(initialKeyMaterial)  // 添加初始密钥材料
    .addSalt(salt)               // 添加盐值
    .thenExpand(info, 32);       // 扩展为 32 字节

// 派生 AES 密钥
SecretKey aesKey = hkdf.deriveKey("AES", params);

// 或者直接获取字节数据
byte[] derivedData = hkdf.deriveData(params);
```

### 压缩对象头

该特性第一次预览是由 JEP 450 （JDK 24 ）提出，JDK 25 就顺利转正了。

减少了64位体系结构上的对象头大小，此更改通过在对象头中使用紧凑的同步和标识数据布局，减少了Java对象的内存占用。

紧凑对象头并没有成为 JVM 默认的对象头布局方式，需通过显式配置启用：
- JDK 24 需通过命令行参数组合启用：`$ java -XX:+UnlockExperimentalVMOptions -XX:+UseCompactObjectHeaders ...` ；
- JDK 25 之后仅需 `-XX:+UseCompactObjectHeaders` 即可启用。

了解过 Java 对象结构的同学应该知道，Java 对象除了存储数据外，还要通过 对象头 存储很多额外的信息，比如类型信息、GC 标记、锁状态等元数据。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202511012152374.png)

如果程序中要创建大量小对象，可能对象头本身占用的空间都比实际要存储的数据多了!

比如下面这段代码

```java
class Point {
    int x, y;  // 实际数据只有 8 字节
}

// 创建一堆 Point 对象
List<Point> points = new ArrayList<>();
for (int i = 0; i < 10000; i++) {
    points.add(new Point(i, i));  // 每个对象实际占用 24 字节！
}
```

用来存储传统的对象头在 64 位系统上占 16 字节，对于只有8字节数据的 Point 来说，开销确实有点大。

Java 25 将紧凑对象头特性转正，把对象头从 16 字节压缩到8字节，减少了小对象的内存开销。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202511012153571.png)

但是，紧凑对象头并不是默认开启的，需要手动指定。毕竟少存了一些信息，必须考虑到兼容性，比如下面这几种情况可能会有问题
- 使用了 JNI直接操作对象头的代码
- 依赖特定对象头布局的调试工具
- 某些第三方性能分析工具

### 分代 Shenandoah GC

Shenandoah GC 在 JDK12 中成为正式可生产使用的 GC，默认关闭，通过 `-XX:+UseShenandoahGC` 启用。

Redhat 主导开发的 Pauseless GC 实现，主要目标是 99.9% 的暂停小于 10ms，暂停与堆大小无关等

传统的 Shenandoah 对整个堆进行并发标记和整理，虽然暂停时间极短，但在处理年轻代对象时效率不如分代 GC。引入分代后，Shenandoah 可以更频繁、更高效地回收年轻代中的大量“朝生夕死”的对象，使其在保持极低暂停时间的同时，拥有了更高的吞吐量和更低的 CPU 开销。

Shenandoah GC 需要通过命令启用：
- JDK 24 需通过命令行参数组合启用：`-XX:+UseShenandoahGC -XX:+UnlockExperimentalVMOptions -XX:ShenandoahGCMode=generational`
- JDK 25 之后仅需 `-XX:+UseShenandoahGC -XX:ShenandoahGCMode=generational` 即可启用。

### 类文件 API 转正

标准化类文件解析与生成接口，取代ASM等第三方库。

### 垃圾回收器改进

- Shenandoah分代回收器正式转正，优化内存管理效率。
- G1垃圾回收器增强后期屏障，减少停顿时间。

### 弃用陈旧特性

完全删除32位x86平台的支持，包括：
- 删除相关源代码(如`HotSpot`虚拟机中的 x86-32 后端)；
- 移除构建配置、测试基础设施中与 x86-32 相关的内容；
- 只保留对x86-64(64 位)平台的支持，推动现代化硬件迁移。

### 飞行记录仪（JFR）升级

- 增强Linux系统CPU时间分析，精准定位性能瓶颈。
- 协作式采样支持安全线程栈检查，优化Java程序性能。

## 预览特性

### 基本类型模式匹配（第三次预览）

JDK25作为第三次preview

在Java的模式匹配框架`instanceof`和`switch`中直接支持原始类型（如`int`、`boolean`），使这种表达式更加直接，减少样板代码，例如：

```java
static void test(Object obj) {     
	if (obj instanceof int i) {
		System.out.println("It's an int: " + i);
	}  
}
```


### 结构化并发（第五次预览）

JDK 19 引入了结构化并发，一种多线程编程方法，目的是为了通过结构化并发 API 来简化多线程编程，并不是为了取代`java.util.concurrent`，目前处于孵化器阶段。

结构化并发将将子任务视为逻辑单元，父任务取消时自动终止子线程，简化错误处理和取消操作，防止资源泄漏，提升高并发可靠性。

结构化并发的基本 API 是`StructuredTaskScope`，它支持将任务拆分为多个并发子任务，在它们自己的线程中执行，并且子任务必须在主任务继续之前完成。

`StructuredTaskScope` 的基本用法如下：

```java
try (var scope = new StructuredTaskScope<Object>()) {  
    // 使用fork方法派生线程来执行子任务  
    Future<Integer> future1 = scope.fork(task1);  
    Future<String> future2 = scope.fork(task2);  
    // 等待线程完成  
    scope.join();  
    // 结果的处理可能包括处理或重新抛出异常  
    ... process results/exceptions ...  
} // close
```

结构化并发非常适合虚拟线程，虚拟线程是 JDK 实现的轻量级线程。许多虚拟线程共享同一个操作系统线程，从而允许非常多的虚拟线程。


### Stable Values 稳定值

这个特性对大多数开发者来说应该是没什么用的。

不信我先考考大家：final字段有什么问题?

答案是必须在构造时初始化。

举个例子

```java
class OrderController {
    private final Logger logger = Logger.create(OrderController.class);
}
```

这段代码中，logger 必须立刻初始化，如果创建logger 很耗时，所有实例都要等待，可能影响启动性能。

特别是在对象很多、但不是每个都会用到某个字段的场景下，这种强制初始化就很浪费。但我又想保证不可变性，怎么办呢?

Stable Values 可以解决上述问题，提供 延迟初始化的不可变性

```java
class OrderController {
    private final StableValue<Logger> logger = StableValue.of();
    
    Logger getLogger() {
        // 只在首次使用时初始化，之后直接返回同一个实例
        return logger.orElseSet(() -> Logger.create(OrderController.class));
    }
}
```

说白了其实就是包一层。。。

还有更简洁的写法，可以在声明时指定初始化逻辑

```java
class OrderController {
    private final Supplier<Logger> logger = 
        StableValue.supplier(() -> Logger.create(OrderController.class));
    
    void logOrder() {
        logger.get().info("处理订单");  // 自动延迟初始化
    }
}
```


还支持集合的延迟初始化

```java
class ConnectionPool {
    // 延迟创建连接池，每个连接按需创建
    private static final List<Connection> connections = 
        StableValue.list(POOL_SIZE, index -> createConnection(index));
    
    public static Connection getConnection(int index) {
        return connections.get(index);  // 第一次访问才创建这个连接
    }
}
```

重点是，StableValues 底层使用 JVM 的 @Stable 注解，享受和 fina 字段一样的优化(比如常量折叠)，所以不用担心性能。

这个特性特别适合
- 创建成本高的对象
- 不是每个实例都会用到的字段
- 需要延迟初始化但又要保证不可变的场景


## 孵化器特性

### Vector API(第 10 次孵化)

Vector API继续孵化，主要改进
- 更好的数学函数支持：现在通过 FFM API 调用本地数学库，提高可维护性
- Float16 支持增强：在支持的 CPU 上可以自动向量化 16 位浮点运算
- VectorShuffle 增强：支持与 MemorySegment 交互

这个 API对高性能计算、机器学习等领域很重要，尤其是现在 AI 的发展带火了向量运算。但是我想说真的别再孵化了，等你转正了，黄花菜都凉了。


















