---
title: 注解详解
category: Java
tag:
  - Java基础
head:
  - - meta
    - name: keywords
      content: Java,注解,annotation,java 注解,java annotation
  - - meta
    - name: description
      content: 全网最全的的Java知识点总结，希望对你有帮助！
---


## 基础

注解是JDK1.5版本开始引入的一个特性，用于对代码进行说明，可以对包、类、接口、字段、方法参数、局部变量等进行注解。主要作用如下：

- 编写文档——通过注解中标识的元数据可以生成doc文档，这是最常见的，也是java 最早提供的注解。常用的有@param @return 等

- 代码分析——通过注解中标识元数据对代码进行分析。跟踪代码依赖性，实现替代配置文件功能。

- 编译检查——通过注解中标识的元数据，让编译器能够实现基本的编译检查，例如@Override重写，如果这个方法并不是覆盖了超类方法，则编译时就能检查出。

这么来说是比较抽象的，具体看下注解的常见分类：

- Java自带的标准注解，包括@Override 、@Deprecated 和 @SuppressWarnings，分别用于标明重写某个方法、标明某个类或方法过时、标明要忽略的警告，用这些注解标明后编译器就会进行检查。

- 元注解，元注解是用于定义注解的注解，包括@Retention、@Target、@Inherited 和 @Documented；@Retention用于标明注解被保留的阶段，@Target用于标明注解使用的范围，@Inherited用于标明注解可继承，@Documented用于标明是否生成javadoc文档。

- 自定义注解，可以根据自己的需求定义注解，并可用元注解对自定义注解进行注解。

### Java内置注解

Java 1.5开始自带的标准注解，包括@Override、@Deprecated和@SuppressWarnings：

- @Override ：表示当前的方法定义将覆盖父类中的方法
- @Deprecated：表示代码被弃用，如果使用了被@Deprecated注解的代码则编译器将发出警告

- @SuppressWarnings：表示关闭编译器警告信息

#### @Override

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
	public @interface Override {
}
```

从源码中看到，这个注解可以被用来修饰方法，并且它只在编译时有效，在编译后的class文件中便不再存在。这个注解的作用就是告诉编译器被修饰的方法是重写的父类的中的相同签名的方法，编译器会对此做出检查，若发现父类中不存在这个方法或是存在的方法签名不同，则会报错。

#### @Deprecated

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value={CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, PACKAGE, PARAMETER, TYPE})
	public @interface Deprecated {
}
```

从源码中可以知道，它会被文档化，能够保留到运行时，能够修饰构造方法、属性、局部变量、方法、包、参数、类型。这个注解的作用是告诉编译器被修饰的程序元素已被“废弃”，不再建议用户使用

#### @SuppressWarnings

```java
@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE})
@Retention(RetentionPolicy.SOURCE)
public @interface SuppressWarnings {
    String[] value();
}
```

它能够修饰的程序元素包括类型、属性、方法、参数、构造器、局部变量，只能存活在源码时，取值为String[]。它的作用是告诉编译器忽略指定的警告信息，它可以取的值如下所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250724252.png)

 

### 元注解

- @Target：用来限制注解的使用范围 

- @Retention：指定其所修饰的注解的保留策略

- @Document：该注解是一个标记注解，用于指示一个注解将被文档化

- @Inherited：该注解使父类的注解能被其子类继承

- @Repeatable：该注解是Java8新增的注解，用于开发重复注解 

- 类型注解（Type Annotation）：该注解是Java8新增的注解，可以用在任何用到类型的地方，做强类型检查

#### @Target

用来限制注解的使用范围，即指定被修饰的注解能用于哪些程序单元

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250724756.png)

取值范围定义在ElementType 枚举中

```java
public enum ElementType {
 
    TYPE, // 类、接口、枚举类
 
    FIELD, // 成员变量（包括：枚举常量）
 
    METHOD, // 成员方法
 
    PARAMETER, // 方法参数
 
    CONSTRUCTOR, // 构造方法
 
    LOCAL_VARIABLE, // 局部变量
 
    ANNOTATION_TYPE, // 注解类
 
    PACKAGE, // 可用于修饰：包
 
    TYPE_PARAMETER, // 类型参数，JDK 1.8 新增
 
    TYPE_USE // 使用类型的任何地方，JDK 1.8 新增
 
}
```



#### @Retention & @RetentionTarget

用于指定被修饰的注解可以保留多长时间，即指定JVM策略在哪个时间点上删除当前注解。保留策略值有以下三个：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250724156.png)

定义在RetentionPolicy枚举中。

```java
public enum RetentionPolicy {
 
    SOURCE,    // 源文件保留
    CLASS,       // 编译期保留，默认值
    RUNTIME   // 运行期保留，可通过反射去获取注解信息
}
```



#### @Document

用于指定被修饰的注解可以被javadoc工具提取成文档。定义注解类时使用@Document注解进行修饰，则所有使用该注解修饰的程序元素的API文档中将会包含该注解说明。

#### @Inherited

指定注解具有继承性，如果某个注解使用@Inherited进行修饰，则该类使用该注解时，其子类将自动被修饰

#### @Repeatable

用于开发重复注解。在Java8之前，同一个程序元素前只能使用一个相同类型的注解，如果需要在同一个元素前使用多个相同类型的注解必须通过注解容器来实现。

```java
public @interface Authority {
     String role();
}

public @interface Authorities {
    Authority[] value();
}

public class RepeatAnnotationUseOldVersion {

    @Authorities({@Authority(role="Admin"),@Authority(role="Manager")})
    public void doSomeThing(){
    }
}
```



从Java8开始，允许使用多个相同的类型注解来修饰同一个元素，前提是该类型的注解是可重复的，即在定义注解时要用 @Repeatable元注解进行修饰。

```java
@Repeatable(Authorities.class)
public @interface Authority {
     String role();
}

public @interface Authorities {
    Authority[] value();
}

public class RepeatAnnotationUseNewVersion {
    @Authority(role="Admin")
    @Authority(role="Manager")
    public void doSomeThing(){ }
}
```

不同的地方是，创建重复注解Authority时，加上@Repeatable,指向存储注解Authorities，在使用时候，直接可以重复使用Authority注解。从上面例子看出，java 8里面做法更适合常规的思维，可读性强一点

#### @Native

@Native 注解修饰成员变量，则表示这个变量可以被本地代码引用，常常被代码生成工具使用。对于 @Native 注解不常使用，了解即可

#### 类型注解

类型注解被用来支持在Java的程序中做强类型检查。配合插件式的check framework，可以在编译的时候检测出runtime error，以提高代码质量。这就是类型注解的作用了。

## 深入理解注解

### 注解是否支持继承

不支持继承

不能使用关键字extends来继承某个@interface，但注解在编译后，编译器会自动继承java.lang.annotation.Annotation接口.

虽然反编译后发现注解继承了Annotation接口，但即使Java的接口可以实现多继承，但定义注解时依然无法使用extends关键字继承@interface。

区别于注解的继承，被注解的子类继承父类注解可以用@Inherited： 如果某个类使用了被@Inherited修饰的Annotation，则其子类将自动具有该注解。

### 注解实现的原理

```java
//这里是元注解
public @interface MyAnno{
}
```

自定义的注解反编译后的内容

```java
public interface MyAnno extends java.lang.Innotation.Annotation {
}
```

也就是说，注解的本质其实就是一个接口，并且继承了java.lang.annotation.Annotation接口.

因此，注解里的属性都是常量，方法都是抽象方法。但是自定义注解不能使用void返回类型。而注解里的方法一般都叫 “属性”，因为用法一般都是：方法 = xxx

返回值类型有下列取值：

- 基本数据类型

- String

- 枚举

- 注解

- 以上类型的数组



### 运行时注解解析

定义了注解后，就可以在代码中使用了，但这还没完，还需要对注解进行解析和处理。在运行时需要用到反射来解析注解，反射API中有专门用于处理注解的API：

- AnnotatedElement ：这是反射接口处理注解的核心类型，它是反射类型Method，Field和Constructor的基类，通过它的方法来获取注解Annotation实例。
- 用Annotation来处理具体的注解

注意，注解的解析和处理用的是反射，所以注解定义时要用RententionPolicy.RUNTIME，否则用反射是拿不到注解信息的，因为反射是在运行时（Runtime）。



解析注解实例：

```java
public class MethodInfoParsing {
    public static void main(String[] args) {
        try {
            Method[] methods = MethodInfoParsing.class
                    .getClassLoader().loadClass("MethodInfoExample").getDeclaredMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(MethodInfo.class)) {
                    continue;
                }
                for (Annotation annotation : method.getDeclaredAnnotations()) {
                    System.out.println("Annotation " + annotation + " on method " + method.getName());
                }
                MethodInfo info = method.getAnnotation(MethodInfo.class);
                if ("Paul".equals(info.author())) {
                    System.out.println("From Pauls: " + method.getName());
                }
            }
        } catch (ClassNotFoundException e) {
        }
    }
}
```

<!-- @include: @article-footer.snippet.md -->     

