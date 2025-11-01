---
title: Java 17 新特性
category: Java
tags:
  - 版本新特性
head:
  - - meta
    - name: keywords
      content: Java,版本新特性,Java17
  - - meta
    - name: description
      content: 全网最全的Java 版本新特性知识点总结，让天下没有难学的八股文！
---


Java17于2021年9月发布，是一个LTS（long term support）长期支持的版本，根据计划来看Java17会支持到2029年（java8会支持到2030年，OMG），同时Oracle提议下一个LTS版本是Java21，在2023年9月发布。

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404252014441.gif)


现在很多新的Java 开发框架和类库支持的最低JDK 版本就是 17 (比如 AI 开发框架 LangChain4j)。


## 正式特性
### Sealed Classes

在jdk15中已经添加了Sealed Classes，只不过当时是作为预览版，经历了2个版本之后，在jdk17中Sealed Classes已经成为正式版了。Sealed Classes的作用是可以限制一个类或者接口可以由哪些子类继承或者实现。

在很多 Java 开发者的印象中，一个类要么完全开放继承(任何类都能继承)，要么完全禁止继承(final 类)。

```java
// 选择1：完全开放继承  
public class Shape {  
    // 问题：不知道会有哪些子类，难以进行穷举  
}  
  
// 选择2：完全禁止继承  
public final class Circle {  
    // 问题：即使在同一个模块内也无法继承  
}
```


其实这样是没办法精确控制继承关系的，在设计 API 或领域模型时可能会遇到问题。Java 17 将 Sealed 密封类转正，让类的继承关系变得更可控和安全。

比如可以只允许某几个类继承

```java
public sealed class Shape   
    permits Circle, Rectangle, Triangle {  
    // 只允许这三个类继承  
}
```

但是，被允许继承的子类必须选择一种继承策略

1) final：到我为止，不能再继承了

```java
public final class Circle extends Shape {  
}
```

2) sealed：我也要控制谁能继承我

```java
public sealed class Triangle extends Shape   
    permits RightTriangle {  
}
```

3) non-sealed：我开放继承，任何人都可以继承我

```java
public non-sealed class Rectangle extends Shape {  
}
```

强制声明继承策略是为了 确保设计控制权的完整传递。如果不强制声明，sealed 类精确控制继承的价值就会被破坏，任何人都可以通过继承子类来绕过原始设计的限制。

注意，虽然看起来 non-sealed 打破了这个设计，但这也是设计者的主动选择。如果不需要强制声明，设计者可能会无意中失去控制权。

有了 Sealed 类后，某个接口可能的实现类型就尽在掌握了，可以让 switch 模式匹配变得更加安全

```java
// 编译器知道所有可能的子类型，可以进行完整性检查  
public double calculateArea(Shape shape) {  
    return switch (shape) {  
        case Circle c -> Math.PI * c.getRadius() * c.getRadius();  
        case Rectangle r -> r.getWidth() * r.getHeight();  
        case Triangle t -> 0.5 * t.getBase() * t.getHeight();  
        // 编译器确保我们处理了所有情况，无需 default 分支  
    };  
}
```


密封类的实际应用

```java
// 定义 API 响应的类型层次  
public sealed interface ApiResponse<T>   
    permits SuccessResponse, ErrorResponse {  
}  
  
public record SuccessResponse<T>(T data, String message) implements ApiResponse<T> {}  
public record ErrorResponse<T>(int errorCode, String errorMessage) implements ApiResponse<T> {}  
  
// 处理响应  
public <T> void handleResponse(ApiResponse<T> response) {  
    switch (response) {  
        case SuccessResponse<T>(var data, var message) -> {  
            System.out.println("成功: " + message);  
            processData(data);  
        }  
        case ErrorResponse<T>(var code, var error) -> {  
            System.err.println("错误 " + code + ": " + error);  
        }  
        // 不需要 default，编译器确保完整性  
    }  
}
```

### 增强的伪随机数生成器

Java 17 引入了全新的随机数生成器 API，提供了更优的性能和更多的算法选择

增加了伪随机数相关的类和接口来让开发者使用stream流进行操作
- RandomGenerator
- RandomGeneratorFactory

之前的java.util.Random和java.util.concurrent.ThreadLocalRandom都是RandomGenerator接口的实现类。

```java
// 传统的随机数  
Random oldRandom = new Random();  
int oldValue = oldRandom.nextInt(100);  
  
// 新的随机数生成器  
RandomGenerator generator = RandomGenerator.of("L32X64MixRandom");  
int newValue = generator.nextInt(100);
```

新的随机数生成器特性

```java
import java.util.random.RandomGenerator;  
import java.util.random.RandomGeneratorFactory;  
  
public class RandomExample {  
    public static void main(String[] args) {  
        // 列出所有可用的算法  
        RandomGeneratorFactory.all()  
            .map(RandomGeneratorFactory::name)  
            .sorted()  
            .forEach(System.out::println);  
          
        // 使用不同的算法  
        RandomGenerator xoroshiro = RandomGenerator.of("Xoroshiro128PlusPlus");  
        RandomGenerator l32x64 = RandomGenerator.of("L32X64MixRandom");  
          
        // 生成随机数  
        System.out.println("Xoroshiro: " + xoroshiro.nextInt(1, 101));  
        System.out.println("L32X64: " + l32x64.nextInt(1, 101));  
          
        // 生成随机流  
        xoroshiro.ints(10, 1, 101)  
                 .forEach(System.out::println);  
          
        // 跳跃功能（某些算法支持）  
        RandomGenerator splittable = RandomGenerator.of("L64X128MixRandom");  
        if (splittable instanceof RandomGenerator.SplittableGenerator split) {  
            RandomGenerator newGen = split.split();  
            System.out.println("分割后的生成器: " + newGen.nextInt(100));  
        }  
    }  
}
```

新 API 的优势
- 更多算法：支持多种高质量的 PRNG 算法
- 更好性能：针对现代 CPU 优化
- 功能丰富：支持跳跃、分割等高级功能
- 统一接口：所有算法使用相同的 API

### 强封装 JDK 内部 API

Java 17 进一步强化了对 JDK 内部 API 的封装，一些之前可以通过反射访问的内部类现在完全不可访问，比如:
- `sun.misc.Unsafe`
- `com.sun.*`包下的类
- `jdk.internal.*`包下的类

虽然这提高了 JDK 的安全性和稳定性，但可能需要迁移一些依赖内部 API 的老代码。

```java
// 替代 sun.misc.Unsafe 的现代方案  
import java.lang.invoke.MethodHandles;  
import java.lang.invoke.VarHandle;  
  
public class ModernUnsafeAlternative {  
    private static final VarHandle ARRAY_HANDLE = MethodHandles.arrayElementVarHandle(int[].class);  
      
    public static void main(String[] args) {  
        int[] array = new int[10];  
          
        // 使用 VarHandle 进行原子操作  
        ARRAY_HANDLE.setVolatile(array, 0, 42);  
        int value = (int) ARRAY_HANDLE.getVolatile(array, 0);  
          
        // CAS 操作  
        boolean success = ARRAY_HANDLE.compareAndSet(array, 0, 42, 100);  
        System.out.println("CAS 成功: " + success);  
    }  
}
```

### 恢复始终严格的浮点语义

Java 17 恢复了严格的浮点计算语义，确保跨平台的一致性

```java
// 浮点计算现在在所有平台上都是一致的  
public class StrictFloatExample {  
    public static void main(String[] args) {  
        double a = 0.1;  
        double b = 0.2;  
        double c = a + b;  
          
        // 现在在所有平台上结果都一致  
        System.out.println(c); // 0.30000000000000004  
          
        // 使用 BigDecimal 进行精确计算  
        BigDecimal bd1 = new BigDecimal("0.1");  
        BigDecimal bd2 = new BigDecimal("0.2");  
        BigDecimal bd3 = bd1.add(bd2);  
        System.out.println(bd3); // 0.3  
    }  
}
```

### 新的 macOs 渲染管道
Java 17 在 macOS 上使用新的渲染管道，提供更好的性能和兼容性

```java
# 默认使用新的渲染管道  
java -Dsun.java2d.metal=true MySwingApp  
  
# 如果遇到问题，可以回退到旧管道  
java -Dsun.java2d.metal=false MySwingApp
```


### macOS/AArch64 端口
Java 17 正式支持 Apple Silicon(M1/M2)芯片

```java
# 在 Apple Silicon Mac 上运行  
java -version  
# 输出会显示 aarch64 架构  
  
# 性能通常比 x86_64 模拟更好  
java -XX:+PrintGCDetails MyApp
```

### 弃用 Applet API

Java 17 将 Applet APl标记为弃用并计划删除

```java
// 这些 API 已被弃用  
// public class MyApplet extends Applet { }  
// public class MyJApplet extends JApplet { }  
  
// 推荐使用现代的 Web 技术替代  
// - JavaScript + HTML5  
// - WebAssembly  
// - 桌面应用程序
```

### 上下文特定的反序列化过滤器

Java 17 增强了反序列化安全性

```java
import java.io.*;  
import java.util.function.BinaryOperator;  
  
public class DeserializationFilterExample {  
    public static void main(String[] args) {  
        // 设置全局反序列化过滤器  
        ObjectInputFilter globalFilter = ObjectInputFilter.Config.createFilter(  
            "java.lang.String;java.lang.Number;!*"  // 只允许 String 和 Number  
        );  
        ObjectInputFilter.Config.setSerialFilter(globalFilter);  
          
        // 或者为特定的流设置过滤器  
        try (ObjectInputStream ois = new ObjectInputStream(inputStream)) {  
            ObjectInputFilter contextFilter = info -> {  
                Class<?> clazz = info.serialClass();  
                if (clazz != null) {  
                    // 只允许特定的类  
                    if (clazz == String.class || Number.class.isAssignableFrom(clazz)) {  
                        return ObjectInputFilter.Status.ALLOWED;  
                    }  
                    return ObjectInputFilter.Status.REJECTED;  
                }  
                return ObjectInputFilter.Status.UNDECIDED;  
            };  
              
            ois.setObjectInputFilter(contextFilter);  
            Object obj = ois.readObject();  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    }  
}
```


### 并发标记清除收集器完全移除

```java
# CMS 收集器在 Java 17 中完全不可用  
# java -XX:+UseConcMarkSweepGC MyApp  # 会报错  
  
# 推荐使用的收集器  
java -XX:+UseG1GC MyApp           # G1  
java -XX:+UseZGC MyApp             # ZGC  
java -XX:+UseShenandoahGC MyApp    # Shenandoah
```


### 去除了AOT和JIT

AOT（Ahead-of-Time）是Java9中新增的功能，可以先将应用中的字节码编译成机器码。

Graal编译器作为使用java开发的JIT（just-in-time ）即时编译器在java10中加入（注意这里的JIT不是之前java中的JIT，在JEP 317中有说明[https://openjdk.java.net/jeps/317）。](https://openjdk.java.net/jeps/317%EF%BC%89%E3%80%82)

以上两项功能由于使用量较少，且需要花费很多精力来维护，因此在java17中被移除了。当然你可以通过Graal VM来继续使用这些功能。



## 预览特性
### switch语法的变化(预览)

在之前版本中新增的instanceof 模式匹配的特性在switch中也支持了，即我们可以在switch中减少强转的操作。比如下面的代码：

Rabbit和Bird均实现了Animal接口

```java
interface Animal{}

class Rabbit implements Animal{
    //特有的方法
    public void run(){
        System.out.println("run");
    }
}

class Bird implements Animal{
    //特有的方法
    public void fly(){
        System.out.println("fly");
    }
}
```

新特性可以减少Animal强转操作代码的编写：

```java
public class Switch01{
    public static void main(String[] args) {
        Animal a = new Rabbit();
        animalEat(a);
    }

    public static void animalEat(Animal a){
        switch(a){
            //如果a是Rabbit类型，则在强转之后赋值给r，然后再调用其特有的run方法
            case Rabbit r -> r.run();
            //如果a是Bird类型，则在强转之后赋值给b，然后调用其特有的fly方法
            case Bird b -> b.fly();
            //支持null的判断
            case null -> System.out.println("null");
            default -> System.out.println("no animal");
        }
    }

}
```

与密封类结合：

```java
public sealed interface Expression   
    permits Constant, Addition, Multiplication {  
}  
  
public record Constant(int value) implements Expression {}  
public record Addition(Expression left, Expression right) implements Expression {}  
public record Multiplication(Expression left, Expression right) implements Expression {}  
  
// 使用模式匹配计算表达式  
public int evaluate(Expression expr) {  
    return switch (expr) {  
        case Constant(var value) -> value;  
        case Addition(var left, var right) -> evaluate(left) + evaluate(right);  
        case Multiplication(var left, var right) -> evaluate(left) * evaluate(right);  
        // 不需要 default，因为密封类保证了完整性  
    };  
}
```



## 孵化器特性

### 外部函数和内存 API(孵化器)
Java 17 继续完善外部函数和内存 API

```java
import jdk.incubator.foreign.*;  
  
public class ForeignAPIExample {  
    public static void main(String[] args) throws Throwable {  
        // 查找 C 标准库函数  
        SymbolLookup stdlib = CLinker.systemLookup();  
          
        // 查找 printf 函数  
        MemoryAddress printfAddr = stdlib.lookup("printf").orElseThrow();  
          
        // 创建函数描述符  
        FunctionDescriptor printfDesc = FunctionDescriptor.of(  
            CLinker.C_INT,      // 返回类型  
            CLinker.C_POINTER   // 参数类型（格式字符串）  
        );  
          
        // 创建方法句柄  
        MethodHandle printf = CLinker.getInstance()  
            .downcallHandle(printfAddr, printfDesc);  
          
        // 调用 printf  
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {  
            MemorySegment formatStr = CLinker.toCString("Hello from Java %d\n", scope);  
            printf.invoke(formatStr, 2024);  
        }  
    }  
}
```


### 向量 API(第二次孵化器)
Java 17 继续改进向量 API

```java
import jdk.incubator.vector.*;  
  
public class VectorMath {  
    public static void vectorAdd(float[] a, float[] b, float[] result) {  
        VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;  
        int upperBound = SPECIES.loopBound(a.length);  
          
        // 向量化循环  
        for (int i = 0; i < upperBound; i += SPECIES.length()) {  
            FloatVector va = FloatVector.fromArray(SPECIES, a, i);  
            FloatVector vb = FloatVector.fromArray(SPECIES, b, i);  
            FloatVector vc = va.add(vb);  
            vc.intoArray(result, i);  
        }  
          
        // 处理剩余元素  
        for (int i = upperBound; i < a.length; i++) {  
            result[i] = a[i] + b[i];  
        }  
    }  
      
    public static void main(String[] args) {  
        float[] a = {1, 2, 3, 4, 5, 6, 7, 8};  
        float[] b = {8, 7, 6, 5, 4, 3, 2, 1};  
        float[] result = new float[8];  
          
        vectorAdd(a, b, result);  
        System.out.println(Arrays.toString(result));  
        // [9.0, 9.0, 9.0, 9.0, 9.0, 9.0, 9.0, 9.0]  
    }  
}
```


<!-- @include: @article-footer.snippet.md -->     



