---
title: 枚举：掌握常量管理的终极指南
category: Java
tags:
  - Java基础
head:
  - - meta
    - name: keywords
      content: Java,枚举,enum,java 枚举,java enum
  - - meta
    - name: description
      content: 全网最全的Java知识点总结，让天下没有难学的八股文！
---



## 枚举的定义

在JDK1.5之前，我们要是想定义一些有关常量的内容，例如定义几个常量，表示从周一到周末，一般都是在一个类，或者一个接口中，写类似于如下代码：

```java
public class WeekDayConstant {
    public static final int MONDAY = 0;
    public static final int TUESDAY = 1;
    public static final int WEDNESDAY = 2;
    public static final int THURSDAY = 3;
    //...
}
```

这样做也可以实现功能，有几个缺点：

- 各个常量的值可能会一样，出现混淆，例如不小心把TUESDAY 定义为0
- 使用起来并不是很方便，例如想要获取某一种枚举的所有枚举值列表，根名称获取值等，还要去编码实现
- 并不是很安全，例如反射修改常量的值，MONDAY 的值可能被修改为1
- 方式并不是很优雅

为了不重复造轮子，Java在JDK1.5的时候，引入了枚举enum关键字(enum就是enumeration的缩写)，我们可以定义枚举类型。

```java
访问修饰符 enum 枚举类型名称{
	//一个或多个枚举值定义，一般采用大写加下划线的方式，用英文逗号分隔，例如，
	A,B,C;
	//在最后一个枚举值后面建议加一个分号,对于与只有枚举值的枚举定义来说，可以没有分号
	//后面就是一些方法的定义
}
```



例如，周一到周末的枚举定义：

```java
public enum WeekDay {
    MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY;
}
```

可以单独定义一个文件，也可以定义在其他类的文件中。





## 枚举的实现原理

先从简单的来，我现在把我们上面使用的Subject枚举类改成下面的样子：

```java
public enum Subject {
    CHINESE,
    MATH,
    ENGLISH;
}
```

切换到class所在的目录，然后：

```java
javap -c Subject.class
```

javap是JDK自带的反汇编工具，得到下面的结果：

```java
Compiled from "Subject.java"
public final class com.victory.test.object_size.Subject extends java.lang.Enum<com.victory.test.object_size.Subject> {
  public static final com.victory.test.object_size.Subject CHINESE;

  public static final com.victory.test.object_size.Subject MATH;

  public static final com.victory.test.object_size.Subject ENGLISH;

  public static com.victory.test.object_size.Subject[] values();
    Code:
       0: getstatic     #1                  // Field $VALUES:[Lcom/victory/test/object_size/Subject;
       3: invokevirtual #2                  // Method "[Lcom/victory/test/object_size/Subject;".clone:()Ljava/lang/Object;
       6: checkcast     #3                  // class "[Lcom/victory/test/object_size/Subject;"
       9: areturn

  public static com.victory.test.object_size.Subject valueOf(java.lang.String);
    Code:
       0: ldc           #4                  // class com/victory/test/object_size/Subject
       2: aload_0
       3: invokestatic  #5                  // Method java/lang/Enum.valueOf:(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;
       6: checkcast     #4                  // class com/victory/test/object_size/Subject
       9: areturn

  static {};
    Code:
       0: new           #4                  // class com/victory/test/object_size/Subject
       3: dup
       4: ldc           #7                  // String CHINESE
       6: iconst_0
       7: invokespecial #8                  // Method "<init>":(Ljava/lang/String;I)V
      10: putstatic     #9                  // Field CHINESE:Lcom/victory/test/object_size/Subject;
      13: new           #4                  // class com/victory/test/object_size/Subject
      16: dup
      17: ldc           #10                 // String MATH
      19: iconst_1
      20: invokespecial #8                  // Method "<init>":(Ljava/lang/String;I)V
      23: putstatic     #11                 // Field MATH:Lcom/victory/test/object_size/Subject;
      26: new           #4                  // class com/victory/test/object_size/Subject
      29: dup
      30: ldc           #12                 // String ENGLISH
      32: iconst_2
      33: invokespecial #8                  // Method "<init>":(Ljava/lang/String;I)V
      36: putstatic     #13                 // Field ENGLISH:Lcom/victory/test/object_size/Subject;
      39: iconst_3
      40: anewarray     #4                  // class com/victory/test/object_size/Subject
      43: dup
      44: iconst_0
      45: getstatic     #9                  // Field CHINESE:Lcom/victory/test/object_size/Subject;
      48: aastore
      49: dup
      50: iconst_1
      51: getstatic     #11                 // Field MATH:Lcom/victory/test/object_size/Subject;
      54: aastore
      55: dup
      56: iconst_2
      57: getstatic     #13                 // Field ENGLISH:Lcom/victory/test/object_size/Subject;
      60: aastore
      61: putstatic     #1                  // Field $VALUES:[Lcom/victory/test/object_size/Subject;
      64: return
}
```

解释：

- 从反编译的代码可以看出编译器确实帮我们生成了一个Subject类，该类继承自java.lang.Enum类，生成的这个类是用final修饰的，因此枚举不能被继承，也不能继承其他类了，但是可以实现接口。

- 声明的每一个枚举常量，都是Subject类的一个实例（见前面几行），这充分说明了前面使用关键字enum定义的类型中的每种Subject枚举常量也是实实在在的Subject实例对象，只不过代表的内容不一样而已。
- 编译器还生成了两个静态方法，分别是values()和 valueOf()，
- 接着是编译器生成的静态代码块，后面是静态代码块的JVM指令



到此也就明白了，使用关键字enum定义的枚举类型，在编译期后，也将转换成为一个实实在在的类，而在该类中，会存在每个在枚举类型中定义好变量的对应实例对象，同时编译器会为该类创建两个方法，分别是values()和valueOf()。



## 枚举的常用方法

- name()：是一个实例方法，该方法在`java.lang.Enum`中，返回枚举的名称，枚举的名称就是定义枚举常量时用的字符串，该方法被final修饰，因此不能被重写
- values()：编译器生成的static方法，按照声明的顺序返回枚举类中定义的所有枚举常量组成的数组，这个方法是一个隐含的方法，由编译器生成的。
  ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407161552662.png)
- valueOf(String)：编译器生成的static方法，它根据一个名称返回一个枚举常量。
  如果名称所表示的枚举常量不存在，则抛出`java.lang.IllegalArgumentException`异常。这个方法是一个隐含的方法，由编译器生成的，对于一个具体的枚举类来说，这个方法是有的，但是`java.lang.Enum`中没有这个方法。
- valueOf(Class,String)：是一个静态的方法，存在于java.lang.Enum中，它的作用跟上一个方法类似，只不过第一个参数是Class类型的，需要指定获取那个类型的常量，第二个参数是常量的名称。
- getDeclaringClass() ：是一个实例方法，存在于java.lang.Enum中，可以获取代表当前枚举类型的Class对象，被final关键字修饰，不能被重写
- ordinal()：是一个实例方法，返回当前枚举常量的序号，序号是在枚举类中声明的顺序，从0开始，最大值是java.lang.Integer.MAX_VALUE，被final关键字修饰，不能被重写。如果枚举常量中的声明位置发生变化，那么ordinal方法获取到的值也随之变化，注意在[Effective item35](https://www.seven97.top/books/software-quality/effectivejava-summary.html#_35、使用实例字段替代序数)中认为大多数情况下都不应该使用该方法，毕竟它总是变幻莫测的。
- toString()：是一个实例方法，来自于java.lang.Object，在java.lang.Enum的实现是直接返回了name属性，就是name()方法的返回值，这个方法可以被重写。
- compareTo(E)：是一个实例方法，java.lang.Enum类实现了Comparable接口，用于比较当前枚举实例和指定的枚举实例，如果两个枚举实例的类型都不一样，直接会怕抛出异常，否则比较的是他们的ordinal值，这个值是ordinal()方法的返回值，由于这个方法由final修饰，因此不能被重写。
  



<!-- @include: @article-footer.snippet.md -->     

