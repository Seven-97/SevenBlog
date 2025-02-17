---
title: Java对象头：深入理解对象存储的核心机制
category: Java
tags:
  - JVM
head:
  - - meta
    - name: keywords
      content: Java,JVM,对象结构,对象布局,对象头,Class Pointer,Mark Word,指针压缩
  - - meta
    - name: description
      content: 全网最全的Java JVM知识点总结，让天下没有难学的八股文！
---



## Java对象结构

实例化一个Java对象之后，该对象在内存中的结构是怎么样的？Java对象（Object实例）结构包括三部分：对象头、对象体和对齐字节，具体下图所示

![Java对象结构](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211207665.png)



### Java对象的三部分

#### 对象头

对象头包括三个字段，第一个字段叫作Mark Word（标记字），用于存储自身运行时的数据，例如GC标志位、哈希码、锁状态等信息。

第二个字段叫作Class Pointer（类对象指针），用于存放方法区Class对象的地址，虚拟机通过这个指针来确定这个对象是哪个类的实例。

第三个字段叫作Array Length（数组长度）。如果对象是一个Java数组，那么此字段必须有，用于记录数组长度的数据；如果对象不是一个Java数组，那么此字段不存在，所以这是一个可选字段。

#### 对象体

对象体包含对象的实例变量（成员变量），用于成员属性值，包括父类的成员属性值。这部分内存按4字节对齐。

#### 对齐字节

对齐字节也叫作填充对齐，其作用是用来保证Java对象所占内存字节数为8的倍数。**HotSpot VM的内存管理要求**对象起始地址必须是8字节的整数倍。对象头本身是8的倍数，当对象的实例变量数据不是8的倍数时，便需要填充数据来保证8字节的对齐。



### Mark Word的结构信息

用于存储对象自身运行时数据，如HashCode、GC分代年龄、锁状态标志、线程持有锁、偏向线程ID、偏向时间戳等信息。这些信息都是与对象自身定义无关的数据，所以Mark Word被设计成一个非固定的数据结构以便在极小的空间内存存储尽量多的数据。它会根据对象的状态复用自己的存储空间，也就是说在运行期间Mark Word里存储的数据会随着锁标志位的变化而变化。

Mark Word的位长度为JVM的一个Word大小，也就是说32位JVM的Mark Word为32位，64位JVM为64位。Mark Word的位长度不会受到Oop对象指针压缩选项的影响。

Java内置锁的状态总共有4种，级别由低到高依次为：无锁、偏向锁、轻量级锁和重量级锁。其实在JDK 1.6之前，Java内置锁还是一个重量级锁，是一个效率比较低下的锁，在JDK 1.6之后，JVM为了提高锁的获取与释放效率，对synchronized的实现进行了优化，引入了偏向锁和轻量级锁，从此以后Java内置锁的状态就有了4种（无锁、偏向锁、轻量级锁和重量级锁），并且4种状态会随着竞争的情况逐渐升级，而且**是不可逆的过程，即不可降级**，也就是说只能进行锁升级（从低级别到高级别）。以下是64位的Mark Word在不同的锁状态下的结构信息：

![64位Mark Word的结构信息](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211316686.gif)

由于目前主流的JVM都是64位，因此我们使用64位的Mark Word。接下来对64位的Mark Word中各部分的内容进行具体介绍。

- lock：**锁状态标记位**，占两个二进制位，由于希望用尽可能少的二进制位表示尽可能多的信息，因此设置了lock标记。该标记的值不同，整个Mark Word表示的含义就不同。

- biased_lock：对象是否启用偏向锁标记，只占1个二进制位。为1时表示对象启用偏向锁，为0时表示对象没有偏向锁。lock和biased_lock两个标记位组合在一起共同表示Object实例处于什么样的锁状态。二者组合的含义具体如下表所示

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211207799.png)

- age：4位的Java对象分代年龄。在GC中，对象在Survivor区复制一次，年龄就增加1。当对象达到设定的阈值时，就会晋升到老年代。默认情况下，并行GC的年龄阈值为15，并发GC的年龄阈值为6。由于age只有4位，因此最大值为15，这就是-XX:MaxTenuringThreshold选项最大值为15的原因。
- identity_hashcode：31位的对象标识HashCode（哈希码）采用延迟加载技术，当调用Object.hashCode()方法或者System.identityHashCode()方法计算对象的HashCode后，其结果将被写到该对象头中。当对象被锁定时，该值会移动到Monitor（监视器）中。
- thread：54位的线程ID值为持有偏向锁的线程ID。
- epoch：偏向时间戳。
- ptr_to_lock_record：占62位，在轻量级锁的状态下指向栈帧中锁记录的指针。



## 使用JOL工具查看对象的布局

### JOL工具的使用

JOL工具是一个jar包，使用它提供的工具类可以轻松解析出运行时java对象在内存中的结构，使用时首先需要引入maven GAV信息

```xml
<!--Java Object Layout -->
<dependency>
    <groupId>org.openjdk.jol</groupId>
    <artifactId>jol-core</artifactId>
    <version>0.17</version>
</dependency>
```

截止至24年9月，最新版本是0.17版本，据观察，它和0.15之前（不包含0.15）的版本输出信息差异比较大，而普遍现在使用的版本都比较低，但是不妨碍在这里使用该工具做实验。

jol-core 常用的几个方法

- `ClassLayout.parseInstance(object).toPrintable()`：查看对象内部信息.
- `GraphLayout.parseInstance(object).toPrintable()`：查看对象外部信息，包括引用的对象.
- `GraphLayout.parseInstance(object).totalSize()`：查看对象总大小.
- `VM.current().details()`：输出当前虚拟机信息

首先创建一个简单的类Hello

```java
public class Hello {
    private Integer a = 1;   
}
```

接下来写一个启动类测试下

```java
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.VM;

public class JalTest {

    public static void main(String[] args) {
        System.out.println(VM.current().details());
        Hello hello = new Hello();
        System.out.printf(ClassLayout.parseInstance(hello).toPrintable());
    }
}
```

输出结果：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211353623.png)

### 结果分析

在代码中，首先使用了`VM.current().details()` 方法获取到了当前java虚拟机的相关信息：

- VM mode: 64 bits - 表示当前虚拟机是64位虚拟机
- Compressed references (oops): 3-bit shift - 开启了对象指针压缩，在64位的Java虚拟机上，对象指针通常需要占用8字节（64位），但通过使用压缩指针技术，可以减少对象指针的占用空间，提高内存利用率。"3-bit shift" 意味着使用3位的位移操作来对对象指针进行压缩。通过将对象指针右移3位，可以消除指针中的一些无用位，从而减少对象指针的实际大小，使其占用更少的内存。
- Compressed class pointers: 3-bit shift - 开启了类指针压缩，其余同上。
- Object alignment: 8 bytes - 字节对齐使用8字节

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211207181.png)

这部分输出表示 引用类型、boolean、byte、char、short、int、float、long、doubl e类型的数据所占的字节数大小以及在数组中的大小和偏移量。

需要注意的是数组偏移量的概念，数组偏移量的数值其实就是对象头的大小，在上图中的16字节表示如果当前对象是数组，那对象头就是16字节，不要忘了，对象头中还有数组长度，在未开启对象指针压缩的情况下，它要占据4字节大小。

接下来是对象结构的输出分析。



## 对象结构输出解析

先回顾下对象结构

![Java对象结构](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211207665.png)

再来回顾下对象结构输出结果

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211207424.png)

- **OFF**：偏移量，单位字节
- **SZ**：大小，单位字节
- **TYPE DESCRIPTION**：类型描述，这里显示的比较直观，甚至可以看到是对象头的哪一部分
- **VALUE**：值，使用十六进制字符串表示，注意一个字节是8bit，占据两个16进制字符串，JOL0.15版本之前是小端序展示，0.15（包含0.15）版本之后使用大端序展示。



### Mark Word解析

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211207912.png)

因为当前虚拟机是64位的虚拟机，所以Mark Word在对象头中占据8字节，也就是64位。它不受指针压缩的影响，占据内存大小只和当前虚拟机有关系。

当前的值是十六进制数值：`0x0000000000000001`，为了好看点，将它按照字节分割开：`00 00 00 00 00 00 00 01`，然后，来回顾下mark workd的内存结构：

![64位Mark Word的结构信息](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211356429.gif)

最后一个字节是十六进制的01，转化为二进制数，就是`00000001`，那倒数三个bit就是`001`，偏向锁标志位biased是0，lock标志位是01，对应的是**无锁状态**下的mark word数据结构。



### Class Pointer 解析

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211207956.png)

该字段在64位虚拟机下开启指针压缩占据4字节，未开启指针压缩占据8字节，它指向方法区的内存地址，即Class对象所在的位置。



### 对象体解析

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211207966.png)

Hello类只有一个Integer类型的变量a，它在64位虚拟机下开启指针压缩占据4字节，未开启指针压缩占据8字节大小。需要注意的是，这里的8字节存储的是Integer对象指针大小，而非int类型的数值所占内存大小。



## 不同条件下的对象结构变化

### Mark Word中的hashCode

在无锁状态下，对象头中的mark word字段有31bit是用于存放hashCode的值的，但是在之前的打印输出中，hashCode全是0，这是为什么？

想要hashCode的值能够在mark word中展示，需要满足两个条件：

1. 目标类不能重写hashCode方法
2. 目标对象需要调用hashCode方法生成hashCode

上面的实验中，Hello类很简单

```java
public class Hello {
    private Integer a = 1;   
}
```

没有重写hashCode方法，使用JOL工具分析没有看到hashCode值，是因为没有调用hashCode()方法生成hashCode值

接下来改下启动类，调用下hashCode方法，重新输出解析结果

```java
public class JalTest {

    public static void main(String[] args) {
        System.out.println(VM.current().details());
        Hello hello = new Hello();
        hello.hashCode();
        System.out.printf(ClassLayout.parseInstance(hello).toPrintable());
    }
}
```

输出结果

![image-20240921140530950](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211405001.png)

可以看到，Mark Word中已经有了hashCode的值。



### 字节对齐

从JOL输出上来看，使用的是8字节对齐，而对象正好是16字节，是8的整数倍，所以并没有使用字节对齐，为了能看到字节对齐的效果，再给Hello类新增一个成员变量`Integer b = 2`，已知一个整型变量在这里占用4字节大小空间，对象大小会变成20字节，那就不是8的整数倍，会有4字节的对齐字节填充，改下Hello类

```java
public class Hello {
    private Integer a = 1;
    private Integer b = 2;
}
```

然后查看运行结果：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211406858.png)

果然，为了对齐8字节，多了4字节的填充，整个对象实例大小变成了24字节。



### 数组类型的对象结构

数组类型的对象和普通的对象肯定不一样，甚至在对象头中专门有个“数组长度”来记录数组的长度。改变下启动类，看看Integer数组的对象结构

```java
public class JalTest {

    public static void main(String[] args) {
        System.out.println(VM.current().details());
        Integer[] a = new Integer[]{1, 2, 3};
        a.hashCode();
        System.out.printf(ClassLayout.parseInstance(a).toPrintable());
    }
}
```

输出结果

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211407465.png)

标红部分相对于普通的对象，数组对象多了个数组长度的字段；而且接下来3个整数，共占据了12字节大小的内存空间。

再仔细看看，加上数组长度部分，对象头部分一共占据了16字节大小的空间，这个和上面的Array base offsets的大小一致，这是因为要想访问到真正的对象值，从对象开始要经过16字节的对象头才能读取到对象，这16字节也就是每个元素读取的“偏移量”了。



### 指针压缩

开启指针压缩：` -XX:+UseCompressedOops`

关闭指针压缩：` -XX:-UseCompressedOops`

在Intelij中，在VM Options中添加该参数即可，需要注意的是，指针压缩在java8及以后的版本中是默认开启的。

接下来看看指针压缩在开启和没开启的情况下，相同的解析代码打印出来的结果

代码：

```java
@Slf4j
public class JalTest {

    public static void main(String[] args) {
        System.out.println(VM.current().details());
        Integer[] a = new Integer[]{1, 2, 3};
        a.hashCode();
        System.out.printf(ClassLayout.parseInstance(a).toPrintable());
    }
}
```

开启指针压缩的解析结果：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211207861.png)

未开启指针压缩的结果：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211207018.png)

以开启指针压缩后的结果为基础，观察下未开启指针压缩的结果

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211207037.png)

需要注意的是这里的Integer[]数组里面都是Integer对象，而非int类型的数值，它是Integer基本类型包装类的实例，这里的数组内存地址中存储的是每个Integer对象的指针引用，从输出的VM信息的对照表中，“ref”类型占据8字节，所以才是3*8为24字节大小。

可以看到，开启指针压缩以后，会产生两个影响

1. 对象引用类型会从8字节变成4字节
2. 对象头中的Class Pointer类型会从8字节变成4字节

确实能节省空间。



## 扩展阅读

### 大端序和小端序

大端序（Big Endian）和小端序（Little Endian）是两种不同的存储数据的方式，特别是在多字节数据类型（比如整数）在计算机内存中的存储顺序方面有所体现。

- **大端序（Big Endian）**：在大端序中，数据的高位字节存储在低地址，而低位字节存储在高地址。类比于数字的书写方式，高位数字在左边，低位数字在右边。因此，数据的最高有效字节（Most Significant Byte，MSB）存储在最低的地址处。
- **小端序（Little Endian）**：相反地，在小端序中，数据的低位字节存储在低地址，而高位字节存储在高地址。这种方式与我们阅读数字的顺序一致，即从低位到高位。因此，数据的最低有效字节（Least Significant Byte，LSB）存储在最低的地址处。

这两种存储方式可以用一个简单的例子来说明：

假设要存储一个 4 字节的整数 `0x12345678`：

- 在大端序中，存储顺序为 `12 34 56 78`。
- 在小端序中，存储顺序为 `78 56 34 12`。







