---
title: Java 22新特性
category: Java
tag:
 - 版本新特性
---





## pre-construction context（预览）

其实就是在super()上面可以添加代码了，目前还是预览状态

以前java语法规定了倘若使用super调用父类构造方法时，其必须出现在第一行，这有时就不利于我们编写更健壮的程序，比如：下面代码编译报错：

```java
public class Student extends Person{
    public Student(int age){
        if(age < 0)
            throw new IllegalArgumentException("年龄不能是负数");
        super(age);//该语句必须出现在构造方法的第一行
    }
}
```



为了解决上面的问题，在java22中提出了pre-construction context，它允许我们在super()上面编写不访问对象的代码。

也就是：

- 允许super(...)和this(...) 之前添加别的语句
  - 预览功能

- 限制仍然存在

  - 构造器自顶向下

  - 字段在完成初始化之前不能被访问

- 依靠编译器的检查：根据编译错误来修改

 

示例代码1

```java
public class Student extends Person{
    public Student(int age){
        this.toString();//报错
        System.out.print(this);//报错
        super.age;//报错
        super(age);//该语句必须出现在构造方法的第一行
    }
}
```



示例代码2

```java
public class User{
    private int id;
    
    public User(){
        id++;
        hashcode();
        super();
    }
}
```



## Stream收集器（预览）

预览版功能：可以自定义中间操作了

原版stream流存在的问题

- 只能使用Stream类提供的方法，不能随意添加

- distinct 只能根据对象相等性来去重，无法根据对象的属性值来去重

 

## 外部函数与内存 API

Java 程序可以通过该 API 与 Java 运行时之外的代码和数据进行互操作。通过高效地调用外部函数（即 JVM 之外的代码）和安全地访问外部内存（即不受 JVM 管理的内存），该 API 使 Java 程序能够调用本机库并处理本机数据，而不会像 JNI 那样危险和脆弱。



## 未命名模式和变量（预览）

未命名模式和变量使得我们可以使用下划线 _ 表示未命名的变量以及模式匹配时不使用的组件，旨在提高代码的可读性和可维护性。

未命名变量的典型场景是 try-with-resources 语句、 catch 子句中的异常变量和for循环。当变量不需要使用的时候就可以使用下划线 _代替，这样清晰标识未被使用的变量。

```java
try (var _ = ScopedContext.acquire()) {
  // No use of acquired resource
}
try { ... }
catch (Exception _) { ... }
catch (Throwable _) { ... }

for (int i = 0, _ = runOnce(); i < arr.length; i++) {
  ...
}
```



未命名模式是一个无条件的模式，并不绑定任何值。未命名模式变量出现在类型模式中。

```java
if (r instanceof ColoredPoint(_, Color c)) { ... c ... }

switch (b) {
    case Box(RedBall _), Box(BlueBall _) -> processBox(b);
    case Box(GreenBall _)                -> stopProcessing();
    case Box(_)                          -> pickAnotherBox();
}
```



以下场景可以使用Unnamed Variables

- 局部变量

- try-with-resource

- 循环头中声明的变量

- catch中声明的变量

- lambda表达式中的参数



## 字符串模板（第二次预览）

String Templates(字符串模板) 在 JDK21 中第一次预览，JDK22 是第二次预览。

String Templates 提供了一种更简洁、更直观的方式来动态构建字符串。通过使用占位符${}，我们可以将变量的值直接嵌入到字符串中，而不需要手动处理。在运行时，Java 编译器会将这些占位符替换为实际的变量值。并且，表达式支持局部变量、静态/非静态字段甚至方法、计算结果等特性。

实际上，String Templates（字符串模板）再大多数编程语言中都存在:

```java
"Greetings {{ name }}!";  //Angular
`Greetings ${ name }!`;    //Typescript
$"Greetings { name }!"    //Visual basic
f"Greetings { name }!"    //Python
```



Java 在没有 String Templates 之前，通常使用字符串拼接或格式化方法来构建字符串：

```java
//concatenation
message = "Greetings " + name + "!";

//String.format()
message = String.format("Greetings %s!", name);  //concatenation

//MessageFormat
message = new MessageFormat("Greetings {0}!").format(name);

//StringBuilder
message = new StringBuilder().append("Greetings ").append(name).append("!").toString();
```



这些方法或多或少都存在一些缺点，比如难以阅读、冗长、复杂。

Java 使用 String Templates 进行字符串拼接，可以直接在字符串中嵌入表达式，而无需进行额外的处理：

```java
String message = STR."Greetings \{name}!";
```



在上面的模板表达式中：

- STR 是模板处理器。

- \\{name}为表达式，运行时，这些表达式将被相应的变量值替换。

Java 目前支持三种模板处理器：

- STR：自动执行字符串插值，即将模板中的每个嵌入式表达式替换为其值（转换为字符串）。

- FMT：和 STR 类似，但是它还可以接受格式说明符，这些格式说明符出现在嵌入式表达式的左边，用来控制输出的样式

- RAW：不会像 STR 和 FMT 模板处理器那样自动处理字符串模板，而是返回一个 StringTemplate 对象，这个对象包含了模板中的文本和表达式的信息

```java
String name = "Lokesh";

//STR
String message = STR."Greetings \{name}.";

//FMT
String message = STR."Greetings %-12s\{name}.";

//RAW
StringTemplate st = RAW."Greetings \{name}.";
String message = STR.process(st);
```

除了 JDK 自带的三种模板处理器外，你还可以实现 StringTemplate.Processor 接口来创建自己的模板处理器。

我们可以使用局部变量、静态/非静态字段甚至方法作为嵌入表达式：

```java
//variable
message = STR."Greetings \{name}!";

//method
message = STR."Greetings \{getName()}!";

//field
message = STR."Greetings \{this.name}!";
```

还可以在表达式中执行计算并打印结果：

```java
int x = 10, y = 20;
String s = STR."\{x} + \{y} = \{x + y}";  //"10 + 20 = 30"
```

为了提高可读性，可以将嵌入的表达式分成多行:

```java
String time = STR."The current time is \{
    //sample comment - current time in HH:mm:ss
    DateTimeFormatter
      .ofPattern("HH:mm:ss")
      .format(LocalTime.now())
  }.";
```



## 向量 API（第七次孵化）

向量计算由对向量的一系列操作组成。向量 API 用来表达向量计算，该计算可以在运行时可靠地编译为支持的 CPU 架构上的最佳向量指令，从而实现优于等效标量计算的性能。

向量 API 的目标是为用户提供简洁易用且与平台无关的表达范围广泛的向量计算。

这是对数组元素的简单标量计算：

```java
void scalarComputation(float[] a, float[] b, float[] c) {
   for (int i = 0; i < a.length; i++) {
        c[i] = (a[i] * a[i] + b[i] * b[i]) * -1.0f;
   }
}
```



这是使用 Vector API 进行的等效向量计算：

```java
static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;

void vectorComputation(float[] a, float[] b, float[] c) {
    int i = 0;
    int upperBound = SPECIES.loopBound(a.length);
    for (; i < upperBound; i += SPECIES.length()) {
        // FloatVector va, vb, vc;
        var va = FloatVector.fromArray(SPECIES, a, i);
        var vb = FloatVector.fromArray(SPECIES, b, i);
        var vc = va.mul(va)
                   .add(vb.mul(vb))
                   .neg();
        vc.intoArray(c, i);
    }
    for (; i < a.length; i++) {
        c[i] = (a[i] * a[i] + b[i] * b[i]) * -1.0f;
    }
}
```

