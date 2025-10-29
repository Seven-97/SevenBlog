---
title: Java 11 新特性
category: Java
tags:
  - 版本新特性
head:
  - - meta
    - name: keywords
      content: Java,版本新特性,Java10,Java11,Java12,Java13
  - - meta
    - name: description
      content: 全网最全的Java 版本新特性知识点总结，让天下没有难学的八股文！
---







## Java 11新特性

### 直接运行

在以前的版本中，在命令提示下，需要先编译，生成class文件之后再运行，例如：

```java
javac HelloWorld.Java
java HelloWorld
```



在Java 11中，可以这样直接运行，当然这样直接运行是不产生字节码文件的

```java
java HelloWorld.Java
```



### String新增方法

strip方法：可以去除首尾空格，与之前的trim的区别是还可以去除unicode编码的空白字符，例如：

```java
char c = '\u2000';//Unicdoe空白字符
String str = c + "abc" + c;
System.out.println(str.strip());
System.out.println(str.trim());

System.out.println(str.stripLeading());//去除前面的空格
System.out.println(str.stripTrailing());//去除后面的空格
```



isBlank方法：判断字符串长度是否为0，或者是否是空格，制表符等其他空白字符

```java
String str = " ";
System.out.println(str.isBlank());
```



repeat方法：字符串重复的次数

```java
String str = "seven";
System.out.println(str.repeat(4));// 重复输出seven 4次
```



### lambda表达式中的变量类型推断

jdk11中允许在lambda表达式的参数中使用var修饰。——用处不大

函数式接口：

```java
@FunctionalInterface
public interface MyInterface {
    void m1(String a, int b);
}
```



测试类：

```java
//支持lambda表达式参数中使用var
MyInterface mi = (var a,var b)->{
    System.out.println(a);
    System.out.println(b);
};

mi.m1("seven",1024);
```






<!-- @include: @article-footer.snippet.md -->     

