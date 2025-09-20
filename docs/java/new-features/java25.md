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


## 语言特性

### 基本类型模式匹配（JEP 507）

JDK25作为第三次preview

在Java的模式匹配框架`instanceof`和`switch`中直接支持原始类型（如`int`、`boolean`），使这种表达式更加直接，减少样板代码，例如：

```java
static void test(Object obj) {     
	if (obj instanceof int i) {
		System.out.println("It's an int: " + i);
	}  
}
 ```

### 模块导入声明（JEP 511）

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

### 压缩源文件与实例主方法（JEP 512）

该特性第一次预览是由 JEP 445（JDK 21 ）提出，随后经过了 JDK 22 、JDK 23 和 JDK 24 的改进和完善，最终在 JDK 25 顺利转正。

简化程序入口，支持类级别的`void main()`方法，无需`public static`修饰，允许我们在没有类声明的情况下编写脚本或演示：

```java
void main() {        
	 System.out.println("Hello Java 25!");
}
 ```

这是为了降低 Java 的学习门槛和提升编写小型程序、脚本的效率而迈出的一大步。初学者不再需要理解 `public static void main(String[] args)` 这一长串复杂的声明。对于快速原型验证和脚本编写，这也使得 Java 成为一个更有吸引力的选择。

### 灵活的构造函数体（JEP 513）

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


## 性能优化

### 压缩对象头（JEP 519）

该特性第一次预览是由 JEP 450 （JDK 24 ）提出，JDK 25 就顺利转正了。

减少了64位体系结构上的对象头大小，此更改通过在对象头中使用紧凑的同步和标识数据布局，减少了Java对象的内存占用。

紧凑对象头并没有成为 JVM 默认的对象头布局方式，需通过显式配置启用：
- JDK 24 需通过命令行参数组合启用：`$ java -XX:+UnlockExperimentalVMOptions -XX:+UseCompactObjectHeaders ...` ；
- JDK 25 之后仅需 `-XX:+UseCompactObjectHeaders` 即可启用。

### 结构化并发（JEP505 第五次预览）

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
### 分代 Shenandoah GC

Shenandoah GC 在 JDK12 中成为正式可生产使用的 GC，默认关闭，通过 `-XX:+UseShenandoahGC` 启用。

Redhat 主导开发的 Pauseless GC 实现，主要目标是 99.9% 的暂停小于 10ms，暂停与堆大小无关等

传统的 Shenandoah 对整个堆进行并发标记和整理，虽然暂停时间极短，但在处理年轻代对象时效率不如分代 GC。引入分代后，Shenandoah 可以更频繁、更高效地回收年轻代中的大量“朝生夕死”的对象，使其在保持极低暂停时间的同时，拥有了更高的吞吐量和更低的 CPU 开销。

Shenandoah GC 需要通过命令启用：
- JDK 24 需通过命令行参数组合启用：`-XX:+UseShenandoahGC -XX:+UnlockExperimentalVMOptions -XX:ShenandoahGCMode=generational`
- JDK 25 之后仅需 `-XX:+UseShenandoahGC -XX:ShenandoahGCMode=generational` 即可启用。

## 安全性增强

### 作用域值（ScopedValue）  

替代`ThreadLocal`，支持线程间安全共享不可变数据，简化生命周期管理。

>JDK19的[JEP 428: Structured Concurrency (Incubator)](https://link.segmentfault.com/?enc=HsU5iGHvjvOxf%2FjVdcXN7g%3D%3D.GKjKdrSxUrnjT71QfZlB6BpBaxCdMiJ5ZZpvs4iP5%2BQ%3D)作为第一次incubator  
JDK20的[JEP 437: Structured Concurrency (Second Incubator)](https://link.segmentfault.com/?enc=83T829PetTQfXnYlPwCpuw%3D%3D.QApbiJm8c9c7szwz%2FWvfuKkSGwUKcA6I6lcOHYsE6BE%3D)作为第二次incubator  
JDK21的[JEP 453: Structured Concurrency (Preview)](https://link.segmentfault.com/?enc=0KhEXaNFGmTvZDwDeiuImQ%3D%3D.dG23FYRQASUxKhb3HvfMlXybnSPuOxwNkgL3SXhQyQI%3D)作为首次preview  
JDK22的[JEP 462: Structured Concurrency (Second Preview)](https://link.segmentfault.com/?enc=1oOoNAwQC92P1prPN332vg%3D%3D.IB6kJgftjJlcD1tDUMVtS3kTtp9LvGBXlyyKu%2Fz6iTQ%3D)作为第二次preview  
JDK23的[JEP 480: Structured Concurrency (Third Preview)](https://link.segmentfault.com/?enc=Sg1XbrLe84dOe%2F8WN1R03Q%3D%3D.jud%2BG05iWxMHjB3Cskh2sVmZXYra%2FMfE6DGFrFd6ZAM%3D)作为第三次preview  
JDK24的[JEP 487: Scoped Values (Fourth Preview)](https://link.segmentfault.com/?enc=CbumIuiLkwmq9ASZwj0aFg%3D%3D.Av4NGoPoRKeDg9ouKHkO7o%2BRULH5LQ%2BHmdIMpFnxLC4%3D)作为第四次preview，与JDK23不同的是callWhere以及runWhere方法从ScopedValue类中移除，可以使用ScopedValue.where()再链式调用run(Runnable)或者call(Callable)

JDK25作为第五次preview，有个改动就是 ScopedValue.orElse 方法不再接受null作为参数

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


## 体验优化

### 飞行记录仪（JFR）升级

- 增强Linux系统CPU时间分析，精准定位性能瓶颈。
- 协作式采样支持安全线程栈检查，优化Java程序性能。

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






























