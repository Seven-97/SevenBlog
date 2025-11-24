---
title: 享元模式
category: 系统设计
tags:
  - 设计模式
---





## 概述

**定义：**

 运用共享技术来有效地支持大量细粒度对象的复用。它通过共享已经存在的对象来大幅度减少需要创建的对象数量、避免大量相似对象的开销，从而提高系统资源的利用率。

享元模式是对象池的一种实现。类似于线程池，线程池可以避免不停的创建和销毁多个对象，消耗性能。

宗旨：共享细粒度对象，将多个对同一对象的访问集中起来

##  结构

享元（Flyweight ）模式中存在以下两种状态：

1. 内部状态，即不会随着环境的改变而改变的可共享部分。
2. 外部状态，指随环境改变而改变的不可以共享的部分。享元模式的实现要领就是区分应用中的这两种状态，并将外部状态外部化。

享元模式的主要有以下角色：

- 抽象享元角色（Flyweight）：通常是一个接口或抽象类，在抽象享元类中声明了具体享元类公共的方法，这些方法可以向外界提供享元对象的内部数据（内部状态），同时也可以通过这些方法来设置外部数据（外部状态）。

- 具体享元（Concrete Flyweight）角色 ：它实现了抽象享元类，称为享元对象；在具体享元类中为内部状态提供了存储空间。通常我们可以结合单例模式来设计具体享元类，为每一个具体享元类提供唯一的享元对象。

- 非享元（Unsharable Flyweight)角色 ：并不是所有的抽象享元类的子类都需要被共享，不能被共享的子类可设计为非共享具体享元类；当需要一个非共享具体享元类的对象时可以直接通过实例化创建。

- 享元工厂（Flyweight Factory）角色 ：负责创建和管理享元角色。当客户对象请求一个享元对象时，享元工厂检査系统中是否存在符合要求的享元对象，如果存在则提供给客户；如果不存在的话，则创建一个新的享元对象。

##  案例实现

【例】俄罗斯方块

下面的图片是众所周知的俄罗斯方块中的一个个方块，如果在俄罗斯方块这个游戏中，每个不同的方块都是一个实例对象，这些对象就要占用很多的内存空间，下面利用享元模式进行实现。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271800520.png)

**先来看类图：**

 ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271759184.png)

**代码如下：**

俄罗斯方块有不同的形状，我们可以对这些形状向上抽取出AbstractBox，用来定义共性的属性和行为。

```java
public abstract class AbstractBox {
    public abstract String getShape();

    public void display(String color) {
        System.out.println("方块形状：" + this.getShape() + " 颜色：" + color);
    }
}
```



接下来就是定义不同的形状了，IBox类、LBox类、OBox类等。

```java
public class IBox extends AbstractBox {

    @Override
    public String getShape() {
        return "I";
    }
}

public class LBox extends AbstractBox {

    @Override
    public String getShape() {
        return "L";
    }
}

public class OBox extends AbstractBox {

    @Override
    public String getShape() {
        return "O";
    }
}
```



提供了一个工厂类（BoxFactory），用来管理享元对象（也就是AbstractBox子类对象），该工厂类对象只需要一个，所以可以使用单例模式。并给工厂类提供一个获取形状的方法。

```java
public class BoxFactory {

    private static HashMap<String, AbstractBox> map;

    private BoxFactory() {
        map = new HashMap<String, AbstractBox>();
        AbstractBox iBox = new IBox();
        AbstractBox lBox = new LBox();
        AbstractBox oBox = new OBox();
        map.put("I", iBox);
        map.put("L", lBox);
        map.put("O", oBox);
    }

    public static final BoxFactory getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final BoxFactory INSTANCE = new BoxFactory();
    }

    public AbstractBox getBox(String key) {
        return map.get(key);
    }
}
```



## 优缺点和使用场景

**1，优点**

- 极大减少内存中相似或相同对象数量，节约系统资源，提供系统性能

- 享元模式中的外部状态相对独立，且不影响内部状态

**2，缺点：**

为了使对象可以共享，需要将享元对象的部分状态外部化，分离内部状态和外部状态，使程序逻辑复杂

**3，使用场景：**

- 一个系统有大量相同或者相似的对象，造成内存的大量耗费。

- 对象的大部分状态都可以外部化，可以将这些外部状态传入对象中。

- 在使用享元模式时需要维护一个存储享元对象的享元池，而这需要耗费一定的系统资源，因此，应当在需要多次重复使用享元对象时才值得使用享元模式。

## 享元模式和其它模式的区别

### 享元模式和代理模式

- 目的不同：享元模式的目的是**共享对象，减少内存中重复对象的总数**。例如，一个文本编辑器中有1000个字符“A”，通过享元模式，这1000个“A”可以共享同一个字符对象，从而极大节省内存。而代理模式的目的则是**控制访问**，它像一个“门卫”或“中介”，在客户端和真实对象之间加了一层，用于实现延迟加载、权限控制、日志记录等功能。比如，一个图片的虚拟代理可以延迟加载大图片，直到真正需要显示时才创建真实对象

- 对象管理方式不同：享元模式的核心是一个**享元工厂**（如 `CharacterFactory`），它维护一个**对象池**（享元池）。当客户端请求一个对象时，工厂会检查池中是否存在具有相同内部状态的对象。如果有，则直接返回已有对象；如果没有，则创建一个新对象并放入池中，再返回。代理模式则通常由**代理对象**持有对真实对象的引用。代理对象与真实对象实现相同的接口，客户端通过代理来间接访问真实对象，代理在访问前后可以执行额外逻辑


### 享元模式和单例模式

一般来说，在实现享元模式时，都会用到单例模式。但这俩存在本质区别

- 目的不同：享元模式的目的是**支持大量细粒度对象的共享，以优化内存使用**。而单例模式的目的是**确保一个类只有一个实例，并提供对该实例的全局访问点**，它更侧重于实例的唯一性控制，而非内存优化。
- **实例数量**：这是最直观的区别。单例模式确保**一个类在任何时候都只有一个实例**​。享元模式则允许**一个类有多个实例**，但这些实例会被大量重复使用。享元工厂中缓存的是多个不同的享元对象，例如，字符“A”是一个实例，字符“B”是另一个实例，它们都被多次共享使用


## 源码解析

### Integer

Integer类使用了享元模式。我们先看下面的例子：

```java
public class Demo {
    public static void main(String[] args) {
        Integer i1 = 127;
        Integer i2 = 127;

        System.out.println("i1和i2对象是否是同一个对象？" + (i1 == i2));

        Integer i3 = 128;
        Integer i4 = 128;

        System.out.println("i3和i4对象是否是同一个对象？" + (i3 == i4));
    }
}
```



运行上面代码，结果如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271800923.png)

为什么第一个输出语句输出的是true，第二个输出语句输出的是false？通过反编译软件进行反编译，代码如下：

```java
public class Demo {
    public static void main(String[] args) {
        Integer i1 = Integer.valueOf((int)127);
        Integer i2 Integer.valueOf((int)127);
        System.out.println((String)new StringBuilder().append((String)"i1\u548ci2\u5bf9\u8c61\u662f\u5426\u662f\u540c\u4e00\u4e2a\u5bf9\u8c61\uff1f").append((boolean)(i1 == i2)).toString());
        Integer i3 = Integer.valueOf((int)128);
        Integer i4 = Integer.valueOf((int)128);
        System.out.println((String)new StringBuilder().append((String)"i3\u548ci4\u5bf9\u8c61\u662f\u5426\u662f\u540c\u4e00\u4e2a\u5bf9\u8c61\uff1f").append((boolean)(i3 == i4)).toString());
    }
}
```



上面代码可以看到，直接给Integer类型的变量赋值基本数据类型数据的操作底层使用的是 valueOf() ，所以只需要看该方法即可

```java
public final class Integer extends Number implements Comparable<Integer> {
    
    public static Integer valueOf(int i) {
        if (i >= IntegerCache.low && i <= IntegerCache.high)
            return IntegerCache.cache[i + (-IntegerCache.low)];
        return new Integer(i);
    }
    
    private static class IntegerCache {
        static final int low = -128;
        static final int high;
        static final Integer cache[];

        static {
            int h = 127;
            String integerCacheHighPropValue =
                sun.misc.VM.getSavedProperty("java.lang.Integer.IntegerCache.high");
            if (integerCacheHighPropValue != null) {
                try {
                    int i = parseInt(integerCacheHighPropValue);
                    i = Math.max(i, 127);
                    // Maximum array size is Integer.MAX_VALUE
                    h = Math.min(i, Integer.MAX_VALUE - (-low) -1);
                } catch( NumberFormatException nfe) {
                }
            }
            high = h;
            cache = new Integer[(high - low) + 1];
            int j = low;
            for(int k = 0; k < cache.length; k++)
                cache[k] = new Integer(j++);
            // range [-128, 127] must be interned (JLS7 5.1.7)
            assert IntegerCache.high >= 127;
        }

        private IntegerCache() {}
    }
}
```



可以看到 Integer 默认先创建并缓存 -128 ~ 127 之间数的 Integer 对象，当调用 valueOf 时如果参数在 -128 ~ 127 之间则计算下标并从缓存中返回，否则创建一个新的 Integer 对象。


### String

字符串常量池 也是享元模式的一种体现

```java
public class StringPoolDemo {
    public static void main(String[] args) {
        // 方式一：使用字面量创建（享元模式生效）
        String s1 = "hello";
        String s2 = "hello";

        // 方式二：使用 new 关键字创建（强制在堆上创建新对象）
        String s3 = new String("hello");
        String s4 = new String("hello");

        // 比较引用地址
        System.out.println(s1 == s2); // 输出 true：s1和s2指向常量池中的同一个对象
        System.out.println(s1 == s3); // 输出 false：s1在池中，s3在堆中，是不同的对象
        System.out.println(s3 == s4); // 输出 false：s3和s4在堆中是两个不同的对象

        // 使用 intern() 方法将字符串显式加入常量池
        String s5 = s3.intern(); // 将s3对应的"hello"放入池（如果池中还没有的话）并返回池中引用
        System.out.println(s1 == s5); // 输出 true：s5返回的是池中已存在的"hello"的引用
    }
}
```



<!-- @include: @article-footer.snippet.md -->     
