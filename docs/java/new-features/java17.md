---
title: Java 17 新特性
category: Java
tags:
  - 版本新特性
head:
  - - meta
    - name: keywords
      content: Java,版本新特性,Java16,Java17,Java18
  - - meta
    - name: description
      content: 全网最全的Java 版本新特性知识点总结，让天下没有难学的八股文！
---







## Java17新特性

Java17是一个LTS（long term support）长期支持的版本，根据计划来看Java17会支持到2029年（java8会支持到2030年，OMG），同时Oracle提议下一个LTS版本是Java21，在2023年9月发布，这样讲LST版本的发布周期由之前的3年变为了2年。这里只介绍一些跟开发关联度较大的特性，除此之外JDK17还更新了一些其他新特性，感兴趣的同学可以从这里查看：https://www.oracle.com/news/announcement/oracle-releases-java-17-2021-09-14/

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404252014441.gif)

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



该功能在java17中是预览的，编译和运行需要加上额外的参数:

```java
javac --enable-preview -source 17 Switch01.java
java  --enable-preview Switch01
```



### Sealed Classes

在jdk15中已经添加了Sealed Classes，只不过当时是作为预览版，经历了2个版本之后，在jdk17中Sealed Classes已经成为正式版了。Sealed Classes的作用是可以限制一个类或者接口可以由哪些子类继承或者实现。

### 伪随机数的变化

增加了伪随机数相关的类和接口来让开发者使用stream流进行操作

- RandomGenerator

- RandomGeneratorFactory

之前的java.util.Random和java.util.concurrent.ThreadLocalRandom都是RandomGenerator接口的实现类。

### 去除了AOT和JIT

AOT（Ahead-of-Time）是Java9中新增的功能，可以先将应用中的字节码编译成机器码。

Graal编译器作为使用java开发的JIT（just-in-time ）即时编译器在java10中加入（注意这里的JIT不是之前java中的JIT，在JEP 317中有说明https://openjdk.java.net/jeps/317）。

以上两项功能由于使用量较少，且需要花费很多精力来维护，因此在java17中被移除了。当然你可以通过Graal VM来继续使用这些功能。




<!-- @include: @article-footer.snippet.md -->     



