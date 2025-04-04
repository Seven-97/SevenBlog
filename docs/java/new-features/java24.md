---
title: Java 24 新特性
category: Java
tags:
  - 版本新特性
head:
  - - meta
    - name: keywords
      content: Java,版本新特性,Java24
  - - meta
    - name: description
      content: 全网最全的Java 版本新特性知识点总结，让天下没有难学的八股文！
---

Java 24作为2025年3月发布的最新版本，延续了Java平台每半年发布一次的节奏，带来了24项重要改进。本文将按照核心改进领域分类，详细解析每个特性的技术原理和实际价值，帮助开发者全面了解这一版本的能力边界和应用场景。

不过Java24是自Java 21 以来的第三个非长期支持版本，下一个长期支持版是 **Java 25**，预计25年 9 月份发布。

## 性能优化

### 分代Shenandoah垃圾回收器提升吞吐量与响应速度

[**JEP 404: Generational Shenandoah**](https://openjdk.org/jeps/404)

该JEP主要是提供了一个实验性的分代模式，将分代概念引入Shenandoah GC，通过区分新生代和老年代实现了显著的性能突破。传统Shenandoah作为全堆回收器，每次GC都需要扫描整个堆空间，而分代版本通过以下机制优化：

- **新生代专用回收策略**：采用复制算法快速回收短生命周期对象，减少老年代扫描频率
- **卡表(Card Table)优化**：精确记录老年代到新生代的跨代引用，降低GC停顿时间30%以上
- **并行标记增强**：在并发标记阶段优先处理新生代区域，使平均GC暂停时间控制在2ms以内 实测表明，在16GB堆内存的微服务场景下，分代Shenandoah相比原版吞吐量提升40%，同时保持亚毫秒级的最大暂停时间，成为低延迟应用的理想选择。

### 紧凑对象头设计减少内存占用

[**JEP 450: Compact Object Headers**](https://openjdk.org/jeps/450)

重构了Java对象的内存布局，将 HotSpot JVM中的对象标头大小从96到128位减少到64位。Java程序中的对象往往很小，作为 Project Lilliput的一部分进行的实验表明，许多工作负载的平均对象大小为 256 到 512 位（32 到 64 字节）。这意味着超过 20% 的实时数据可以单独由对象标头获取。因此，即使对象标头大小略有改进，也可以显著减少占用空间、数据局部性并减轻 GC压力。在实际应用程序中试用过Project Lilliput的早期采用者证实，内存占用通常会减少10%–20%。关键技术突破包括：

- **压缩锁标志位**：将原有的2字节Mark Word压缩为1字节，保留基本锁状态和hashcode信息
- **类型指针优化**：使用32位偏移量替代64位类指针，配合压缩类空间(Compressed Class Space)工作
- **对齐填充智能分配**：根据CPU缓存行特性动态调整对象填充策略 在包含百万级对象的电商应用中，该特性减少堆内存使用15%，同时由于更好的缓存局部性，使整体吞吐量提升8%。需要注意的是，该特性要求所有依赖JOL(Java Object Layout)工具的分析代码进行相应适配。

### G1垃圾回收器屏障优化提高效率

[**JEP 475: Late Barrier Expansion for G1**](https://openjdk.org/jeps/475)

该特性主要是将Late Barrier Expansion引进到G1中。Barrier expansion是指在垃圾回收过程中插入或生成额外代码（称为“屏障”）以管理内存并确保垃圾回收的正确性。这些屏障通常被插入到字节码中的特定位置，例如在内存访问之前或之后，以执行以下任务：

- 记住写操作：跟踪对象的更改，这有助于垃圾回收器识别需要扫描的堆的部分。例如，写屏障（write barrier）会在每次存储操作之前执行，记录哪些对象被修改，从而帮助垃圾回收器维护对象的可达性信息。
- 保持一致性：确保程序对内存的视图与垃圾回收器的视图保持一致，特别是在并发垃圾回收阶段。例如，读屏障（read barrier）会在读取操作之前检查指针是否指向堆内存，并记录这些操作，以防止垃圾回收器误判对象的可达性。
- 处理引用：管理对象之间的引用，特别是在压缩或迁移阶段。例如，在垃圾回收的增量收集中，屏障可以阻止指向未分配空间（fromspace）的指针进入寄存器，从而避免垃圾回收器无法追踪到这些对象。

Early barrier expansion的含义是这些屏障在编译过程的早期插入或生成，而如果在过程的后期进行（正如JEP所提议的），则可以实现更优化的放置，并可能减少这些屏障相关的开销，具体为：

- **动态屏障插入**：在JIT编译的优化阶段而非解析阶段插入写屏障，基于实际使用模式生成最小化屏障代码
- **条件屏障消除**：通过[逃逸分析](https://www.seven97.top/java/jvm/01-jvmbasic3-jvmmemorystructure.html#逃逸分析技术)识别不需要屏障的内存操作，在安全情况下完全省略屏障
- **SIMD屏障优化**：对数组批量操作生成向量化屏障指令，提升批量写操作的吞吐量 基准测试显示，该优化使G1在写密集型负载下的吞吐量提升12%，同时减少JIT编译代码大小5-7%。对于使用大量`ConcurrentHashMap`或`CopyOnWriteArrayList`的并发应用收益尤为明显。

## 安全性增强

### 新增密钥派生函数API支持现代加密标准

[**JEP 478: Key Derivation Functions API** ](https://openjdk.org/jeps/478)引入了符合NIST SP 800-56C标准的密钥派生实现。

随着量子计算领域的进步，传统加密算法变得更容易受到实际攻击。因此，Java平台必须整合后量子密码学（PQC），以抵御这些威胁。Java的长期目标是最终实现混合公钥加密（HPKE），以便无缝过渡到量子安全加密。JDK 21中包含的[KEM API（JEP 452）](https://openjdk.org/jeps/452)是HPKE的一个组成部分，标志着Java朝着HPKE迈出的第一步，并为后量子挑战做好了准备。该JEP提出了HPKE的另一个组成部分，作为这一方向上的下一步：密钥派生函数（KDFs）的API。

使用示例如下：

```java
// 示例：使用HKDF-SHA256从主密钥派生会话密钥 
KeyDerivationFunction kdf = KeyDerivationFunctions.of("HKDF-SHA256"); 
SecretKey sessionKey = kdf.deriveKey(masterKey, 
	"SessionKey".getBytes(StandardCharsets.UTF_8),
	256, // 密钥长度     
	new byte[32] // 可选盐值 );
```

该API支持：

- **HKDF**：基于HMAC的提取-扩展密钥派生框架
- **PBKDF2**：密码-Based密钥派生，替代已废弃的`PBEKeySpec`
- **Argon2**：抗侧信道攻击的内存困难型算法 特别在微服务间TLS通信场景中，开发者现在可以标准化密钥派生流程，避免各服务实现不一致导致的安全隐患。

> 由于是preview特性，需要执行的时候添加`--enable-preview`参数
### 永久禁用安全管理器

[JEP 486: Permanently Disable the Security Manager](https://openjdk.org/jeps/486)

安全性管理器（Security Manager）并不是Java客户端代码的主要安全手段，也极少用于服务器端代码。此外，维护它成本高昂。因此，在Java 17中通过[JEP 411: Deprecate the Security Manager for Removal](https://openjdk.org/jeps/411)将其弃用以备移除。本特性则完全禁止开发者启用安全性管理器，Security Manager API将在未来的版本中被移除。



### 后量子加密技术前瞻性支持

[JEP 496:Quantum-Resistant Module-Lattice-Based Key Encapsulation Mechanism](https://openjdk.org/jeps/496)

引入了三种抗量子计算攻击的算法：

1. **ML-KEM**（原CRYSTALS-Kyber）：基于格理论的密钥封装机制
2. **ML-DSA**（原CRYSTALS-Dilithium）：数字签名算法
3. **SLH-DSA**：基于哈希的签名方案

```java
// 生成抗量子密钥对示例 
KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA"); 
kpg.initialize(new MLDSAParameterSpec(MLDSAParameterSpec.ML_DSA_65)); 
KeyPair keyPair = kpg.generateKeyPair();
```
``
虽然这些算法尚未进入最终标准，但预览版允许金融、政务等敏感领域提前进行技术验证和性能测试，为即将到来的量子计算时代做好准备。

## 语言特性

### 作用域值（Scoped Values）

> JDK19的[JEP 428: Structured Concurrency (Incubator)](https://openjdk.org/jeps/428)作为第一次incubator  
JDK20的[JEP 437: Structured Concurrency (Second Incubator)](https://openjdk.org/jeps/437)作为第二次incubator  
JDK21的[JEP 453: Structured Concurrency (Preview)](https://openjdk.org/jeps/453)作为首次preview  
JDK22的[JEP 462: Structured Concurrency (Second Preview)](https://openjdk.org/jeps/462)作为第二次preview  
JDK23的[JEP 480: Structured Concurrency (Third Preview)](https://openjdk.org/jeps/480)作为第三次preview  
JDK24则作为第四次preview，与JDK23不同的是callWhere以及runWhere方法从ScopedValue类中移除，可以使用ScopedValue.where()再链式调用run(Runnable)或者call(Callable)

**并发编程模型革新**：

```java
final static ScopedValue<User> CURRENT_USER = ScopedValue.newInstance(); 
void processRequest(Request req) {     
	ScopedValue.where(CURRENT_USER, fetchUser(req)).run(() -> handleRequest());
} 
void handleRequest() {     
	User user = CURRENT_USER.get(); // 线程内安全访问     
	// ...业务逻辑 
}
```

**技术对比**：

|特性|ThreadLocal|ScopedValue|
|---|---|---|
|内存泄漏风险|高|零|
|子线程继承|需显式传递|自动继承|
|性能开销|约15ns/访问|约3ns/访问|

### 虚拟线程的同步而不固定平台线程

[JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491) 优化了虚拟线程与 `synchronized` 的工作机制。

JDK21引入虚拟线程时还有个pinning的问题，就是当虚拟线程在其载体上运行同步代码块时，它无法从载体上卸载。比如：

```java
class CustomerCounter {
    private final StoreRepository storeRepo;
    private int customerCount;
    CustomerCounter(StoreRepository storeRepo) {
        this.storeRepo = storeRepo;
        customerCount = 0;
    }
    synchronized void customerEnters() {
        if (customerCount < storeRepo.fetchCapacity()) {
            customerCount++;
        }
    }
    synchronized void customerExits() {
        customerCount--;
    }
}
```

> 如果是单纯调用storeRepo.fetchCapacity()则没问题，虚拟线程会从其载体unmount，释放平台线程给其他虚拟线程mount；但是如果是调用customerEnters，它用synchronized修饰则JVM会将该虚拟线程pin住防止其被unmount，这样子的话虚拟线程与平台线程都会blocked，直到fetchCapacity方法返回。

之所以pinning是因为synchronized依赖于monitors来确保它们只能由单个线程同时进入。在进入synchronized块之前，线程必须获取与实例相关联的monitor。JVM在平台线程级别跟踪这些monitor的所有权，而不是在虚拟线程级别跟踪。基于这些信息，假设不存在pinning，理论上，虚拟线程#1可以在synchronized块中间卸载，而虚拟线程#2可以装载到相同的平台线程上，并继续执行该synchronized块，因为承载线程是相同的，仍然持有对象的monitor。

从Java 24开始，虚拟线程可以获取、持有和释放监视器，而无需绑定到其载体线程。这意味着由于线程pinning而切换到不同的锁机制已不再是必需的。从现在起，无论是使用虚拟线程还是其他方法，性能表现都将相当一致。

在少数情况下，虚拟线程仍然会被pinning，其中一个情况是当它调用本地代码并返回到执行阻塞操作的Java代码时。在这种情况下，JDK Flight Recorder（JFR）会记录一个jdk.VirtualThreadPinned事件，如果要跟踪这些情况，可以启用JFR。

### 灵活构造函数体

[JEP 492 Flexible Constructor Bodies (Third Preview)](https://openjdk.org/jeps/492)

> JDK22的[JEP 447: Statements before super(...) (Preview)](https://openjdk.org/jeps/447)作为第一次preview  
JDK23的[JEP 482: Flexible Constructor Bodies (Second Preview)](https://openjdk.org/jeps/482)作为第二次preview  
JDK24作为第三次preview


灵活的构造函数体解决了这一问题，它允许在构造函数体内，在调用 super(..) 或 this(..) 之前编写语句，这些语句可以初始化字段，但不能引用正在构造的实例。这样可以防止在父类构造函数中调用子类方法时，子类的字段未被正确初始化，增强了类构造的可靠性。

这一特性解决了之前 Java 语法限制了构造函数代码组织的问题，让开发者能够更自由、更自然地表达构造函数的行为，例如在构造函数中直接进行参数验证、准备和共享，而无需依赖辅助方法或构造函数，提高了代码的可读性和可维护性。

```java
class Person {
    private final String name;
    private int age;

    public Person(String name, int age) {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative.");
        }
        this.name = name; 
        this.age = age;
        // ... 其他初始化代码
    }
}

class Employee extends Person {
    private final int employeeId;

    public Employee(String name, int age, int employeeId) {
        this.employeeId = employeeId; // 在调用父类构造函数之前初始化字段
        super(name, age); // 调用父类构造函数
        // ... 其他初始化代码
    }
}
```



### 原始类型模式匹配

[JEP 488: Primitive Types in Patterns, instanceof, and switch (Second Preview)](https://openjdk.org/jeps/488)

> JDK19的[JEP 405: Record Patterns (Preview)](https://openjdk.org/jeps/405)将Record的模式匹配作为第一次preview  
JDK20的[JEP 432: Record Patterns (Second Preview)](https://openjdk.org/jeps/432)作为Record模式匹配第二次preview  
JDK21的[JEP 440: Record Patterns](https://openjdk.org/jeps/440)则将Record模式匹配正式发布  
JDK23的[JEP 455: Primitive Types in Patterns, instanceof, and switch (Preview)](https://openjdk.org/jeps/455)将原始类型的匹配作为第一次preview  
JDK24作为第二次preview

**技术实现**：
```java
// 传统类型检查与转换 
if (obj instanceof Integer) {     
	int value = ((Integer)obj).intValue();     
	System.out.println(value * 2); 
} 

// Java 24新模式 
if (obj instanceof int value) {     
	System.out.println(value * 2); 
	// 自动拆箱为原始类型 
}
```


**底层优化**：

1. 字节码层面消除冗余的类型转换指令
2. 模式变量直接绑定到原始类型而非包装类
3. JIT编译器可进行更激进的标量替换优化 

**性能影响**：
- 数值计算密集型代码性能提升8-12%
- 减少50%的临时对象分配


## 工具链增强

### 流收集器

Stream Gatherers API（JEP 461扩展）

流收集器 `Stream::gather(Gatherer)` 是一个强大的新特性，它允许开发者定义自定义的中间操作，从而实现更复杂、更灵活的数据转换。`Gatherer` 接口是该特性的核心，它定义了如何从流中收集元素，维护中间状态，并在处理过程中生成结果。例如，可以使用 `Stream::gather` 实现滑动窗口、自定义规则的去重、或者更复杂的状态转换和聚合。

**复杂流处理示例**：

```java
List<Order> orders = ...; 
List<Order> window = orders.stream().gather(Gatherers.windowSliding(5)) // 5元素滑动窗口     
.filter(window -> window.stream().mapToDouble(Order::amount).average().orElse(0) > 1000)
.flatMap(List::stream)
.toList();
```

**新增内置Gatherers**：

1. `fold()`：实现可变状态聚合
2. `scan()`：生成中间结果的流
3. `fixedWindow()`：固定大小批处理 

**性能特性**：
- 比传统collect操作减少40%的中间集合分配
- 支持短路操作优化

### 向量API（第九次孵化）

> JDK16引入了[JEP 338: Vector API (Incubator)](https://openjdk.org/jeps/338)提供了jdk.incubator.vector来用于矢量计算  
JDK17进行改进并作为第二轮的incubator[JEP 414: Vector API (Second Incubator)](https://openjdk.org/jeps/414)  
JDK18的[JEP 417: Vector API (Third Incubator)](https://openjdk.org/jeps/417)进行改进并作为第三轮的incubator  
JDK19的[JEP 426:Vector API (Fourth Incubator)](https://openjdk.org/jeps/426)作为第四轮的incubator  
JDK20的[JEP 438: Vector API (Fifth Incubator)](https://openjdk.org/jeps/438)作为第五轮的incubator  
JDK21的[JEP 448: Vector API (Sixth Incubator)](https://openjdk.org/jeps/448)作为第六轮的incubator  
JDK22的[JEP 460: Vector API (Seventh Incubator)](https://openjdk.org/jeps/460)作为第七轮的incubator  
JDK23的[JEP 469: Vector API (Eighth Incubator)](https://openjdk.org/jeps/469)作为第八轮incubator  
JDK24则作为第九轮incubator，与JDK23相比做了一些变动：比如引入了一个新的基于值的类Float16，用于表示IEEE 754二进制16格式的16位浮点数。

**SIMD编程模型**：

```java
// 计算两个浮点数组的点积 
void vectorComputation(float[] a, float[] b, float[] c) {     
	for (int i = 0; i < a.length; i += FloatVector.SPECIES_512.length()) {         
		var va = FloatVector.fromArray(FloatVector.SPECIES_512, a, i);         
		var vb = FloatVector.fromArray(FloatVector.SPECIES_512, b, i);         
		var vc = va.mul(vb)
			.add(va.lanewise(VectorOperators.POW, 2))
			.add(vb.lanewise(VectorOperators.POW, 2));         
		vc.intoArray(c, i);     
	} 
}
```

**硬件加速支持**：

|指令集|支持操作|加速比|
|---|---|---|
|AVX-512|8x双精度浮点并行|6.8x|
|NEON|4x单精度浮点并行|3.2x|
|SVE|可变长度向量操作|4.5x|

### 结构化并发

[JEP 499: Structured Concurrency (Fourth Preview)](https://openjdk.org/jeps/499)

>  JDK19的[JEP 428: Structured Concurrency (Incubator)](https://openjdk.org/jeps/428)作为第一次incubator  
> JDK20的[JEP 437: Structured Concurrency (Second Incubator)](https://openjdk.org/jeps/437)作为第二次incubator  
> JDK21的[JEP 453: Structured Concurrency (Preview)](https://openjdk.org/jeps/453)作为首次preview  
> JDK22的[JEP 462: Structured Concurrency (Second Preview)](https://openjdk.org/jeps/462)作为第二次preview  
> JDK23的[JEP 480: Structured Concurrency (Third Preview)](https://openjdk.org/jeps/480)作为第三次preview  
> JDK24作为第四次preview

JDK 19 引入了结构化并发，一种多线程编程方法，目的是为了通过结构化并发 API 来简化多线程编程，并不是为了取代`java.util.concurrent`，目前处于孵化器阶段。

结构化并发将不同线程中运行的多个任务视为单个工作单元，从而简化错误处理、提高可靠性并增强可观察性。也就是说，结构化并发保留了单线程代码的可读性、可维护性和可观察性。

**错误处理改进**：
```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {     
	Future<String> user = scope.fork(() -> fetchUser());     
	Future<Integer> order = scope.fork(() -> fetchOrder());          
	scope.join(); // 等待所有子任务     
	return new Response(user.resultNow(), order.resultNow()); // 自动处理取消和异常传播
} 
```

结构化并发非常适合虚拟线程，虚拟线程是 JDK 实现的轻量级线程。许多虚拟线程共享同一个操作系统线程，从而允许非常多的虚拟线程。

**新增特性**：

1. deadline支持：`scope.withDeadline(Instant.now().plusSeconds(5))`
2. 嵌套scope的层次化取消
3. 与虚拟线程深度集成

## 开发者体验优化

### 提前类加载和链接

[JEP 483: Ahead-of-Time Class Loading & Linking](https://openjdk.org/jeps/483)

在传统 JVM 中，应用在每次启动时需要动态加载和链接类。这种机制对启动时间敏感的应用（如微服务或无服务器函数）带来了显著的性能瓶颈。该特性通过缓存已加载和链接的类，显著减少了重复工作的开销，显著减少 Java 应用程序的启动时间。该特性通过Ahead-of-Time Cache来存储已经读取、解析、加载和链接的类。

这个优化是零侵入性的，对应用程序、库或框架的代码无需任何更改，启动也方式保持一致，仅需添加相关 JVM 参数（如 `-XX:+ClassDataSharing`）。

- 首先运行application来记录AOT配置:
```shell
java -XX:AOTMode=record -XX:AOTConfiguration=app.aotconf -cp app.jar com.example.App ...
```
    
- 接着使用该配置来创建AOT缓存：
    
```shell
java -XX:AOTMode=create -XX:AOTConfiguration=app.aotconf -XX:AOTCache=app.aot -cp app.jar
```
    
- 最后使用AOT缓存启动：
    
```shell
java -XX:AOTCache=app.aot -cp app.jar com.example.App ...
```
    
AOT缓存将读取、解析、加载和链接（通常在程序执行期间即时完成）的任务提前到缓存创建的早期阶段。因此，在执行阶段，程序启动速度更快，因为其类可以从缓存中快速访问。其性能提升可以高达 42%。


### 类文件 API

[JEP 484: Class-File API](484)

> JDK22的[JEP 457: Class-File API (Preview)](https://openjdk.org/jeps/457)提供了一个用于解析、生成和转换 Java 类文件的标准 API  
 JDK23的[JEP 466: Class-File API (Second Preview)](https://openjdk.org/jeps/466)则作为第二次preview  
 JDK24则转为正式版本发布

类文件 API 的目标是提供一套标准化的 API，用于解析、生成和转换 Java 类文件，取代过去对第三方库（如 ASM）在类文件处理上的依赖。

使用Class-File API如下：

```java
// 创建一个 ClassFile 对象，这是操作类文件的入口。  
ClassFile cf = ClassFile.of();  
// 解析字节数组为 ClassModel  
ClassModel classModel = cf.parse(bytes);  
  
// 构建新的类文件，移除以 "debug" 开头的所有方法  
byte[] newBytes = cf.build(classModel.thisClass().asSymbol(),  
        classBuilder -> {  
            // 遍历所有类元素  
            for (ClassElement ce : classModel) {  
                // 判断是否为方法 且 方法名以 "debug" 开头  
                if (!(ce instanceof MethodModel mm  
                        && mm.methodName().stringValue().startsWith("debug"))) {  
                    // 添加到新的类文件中  
                    classBuilder.with(ce);  
                }  
            }  
        });
```


### 简化源文件启动（第四次预览）

[JEP 495: Simple Source Files and Instance Main Methods (Fourth Preview)](https://openjdk.org/jeps/495)

没有使用该特性之前定义一个 `main` 方法：

```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

使用该新特性之后定义一个 `main` 方法：

```java
class HelloWorld {
    void main() {
        System.out.println("Hello, World!");
    }
}
```

进一步简化（未命名的类允许我们省略类名）

```java
void main() {
   System.out.println("Hello, World!");
}
```

这里连类都没有了，隐式声明类继承自 Object，不实现接口，并且不能在源代码中按名称引用。此外，实例主方法也不再强制要求它们是 static 或 public 的，并且不带参数的方法也可以作为有效的程序入口点。

**编译执行变化**：

1. 隐式`class`生成规则优化
2. 支持包声明和模块指令
3. 错误消息指向用户代码行而非生成代码

### 使用`sun.misc.Unsafe`内存访问方法时发出警告

[JEP 498: Warn upon Use of Memory-Access Methods in sun.misc.Unsafe](https://openjdk.org/jeps/498)

> JDK9的[JEP 193: Variable Handles](https://openjdk.org/jeps/193)引入了VarHandle API用于替代sun.misc.Unsafe  
JDK14的[JEP 370: Foreign-Memory Access API (Incubator)](https://openjdk.org/jeps/370)引入了Foreign-Memory Access API作为incubator  
JDK15的[JEP 383: Foreign-Memory Access API (Second Incubator)](https://openjdk.org/jeps/383)Foreign-Memory Access API作为第二轮incubator  
JDK16的[JEP 393: Foreign-Memory Access API (Third Incubator)](https://openjdk.org/jeps/393)作为第三轮，它引入了Foreign Linker API (JEP [389](https://openjdk.org/jeps/389))  
FFM API在JDK 17的[JEP 412: Foreign Function & Memory API (Incubator)](https://openjdk.org/jeps/412)作为incubator引入  
FFM API在JDK 18的[JEP 419: Foreign Function & Memory API (Second Incubator)](https://openjdk.org/jeps/419)作为第二轮incubator  
JDK19的[JEP 424: Foreign Function & Memory API (Preview)](https://openjdk.org/jeps/424)则将FFM API作为preview API  
JDK20的[JEP 434: Foreign Function & Memory API (Second Preview)](https://openjdk.org/jeps/434)作为第二轮preview  
JDK21的[JEP 442: Foreign Function & Memory API (Third Preview)](https://openjdk.org/jeps/442)作为第三轮preview  
JDK22的[JEP 454: Foreign Function & Memory API](https://openjdk.org/jeps/454)则正式发布此特性  
JDK23的[JEP 471: Deprecate the Memory-Access Methods in sun.misc.Unsafe for Removal](https://openjdk.org/jeps/471)废弃sun.misc.Unsafe，以便后续版本移除  


JDK24默认情况下将在首次使用任何内存访问方法时发出警告，无论这些方法是直接调用还是通过反射调用。也就是说，无论使用了哪些内存访问方法，以及任何特定方法被调用的次数如何，最多只会发出一次警告。这将提醒应用程序开发者和用户即将移除这些方法，并需要升级库。

这些不安全的方法已有安全高效的替代方案：
- `java.lang.invoke.VarHandle` ：JDK 9 (JEP 193) 中引入，提供了一种安全有效地操作堆内存的方法，包括对象的字段、类的静态字段以及数组元素。
- `java.lang.foreign.MemorySegment` ：JDK 22 (JEP 454) 中引入，提供了一种安全有效地访问堆外内存的方法，有时会与 `VarHandle` 协同工作。
    
这两个类是 Foreign Function & Memory API（外部函数和内存 API） 的核心组件，分别用于管理和操作堆外内存。Foreign Function & Memory API 在 JDK 22 中正式成为标准特性。

## 升级建议

Java 24作为非LTS版本，建议根据具体场景评估升级策略：

**推荐升级场景**：
- 需要分代Shenandoah的实时交易系统
- 处理敏感数据的金融应用（利用新加密API）
- AI推理服务（受益于向量API优化） 

**暂缓升级场景**：

- 仍依赖安全管理器的遗留系统
- 使用sun.misc.Unsafe的底层库（如Netty、Cassandra需等待适配版本）
- 已稳定运行且无性能瓶颈的长期服务 


## 结语

Java 24通过分代Shenandoah、紧凑对象头等特性继续巩固其在性能敏感领域的地位，同时借助后量子加密等创新保持技术前瞻性。虽然非LTS版本的生产部署需要谨慎评估，但其在各方面的改进，无疑也为Java生态注入了新的活力。

