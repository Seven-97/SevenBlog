---
title: JVM基础 - JVM内存结构
category: Java
tag:
 - JVM
---





## 什么是JVM

定义：Java Virtual Machine，JAVA程序的运行环境（JAVA二进制字节码的运行环境）

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251633013.gif)

## 内存结构

JVM 内存布局规定了 Java 在运行过程中内存申请、分配、管理的策略，保证了 JVM 的高效稳定运行。不同的 JVM 对于内存的划分方式和管理机制存在着部分差异。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251633027.gif)

### 程序计数器

#### 定义

线程私有的，作为当前线程的行号指示器，用于记录当前虚拟机正在执行的线程指令地址。

#### 作用

1. 当前线程所执行的字节码的行号指示器，通过它实现代码的流程控制，如：顺序执行、选择、循环、异常处理。
2. 在多线程的情况下，程序计数器用于记录当前线程执行的位置，当线程被切换回来的时候能够知道它上次执行的位置。

#### 特点

- 线程私有

- **CPU会为每个线程分配时间片**，当当前线程的时间片使用完以后，CPU就会去执行另一个线程中的代码

- 程序计数器是每个线程所私有的，当另一个线程的时间片用完，又返回来执行当前线程的代码时，通过程序计数器可以知道应该执行哪一句指令

- 不会存在内存溢出

程序计数器是唯一一个不会出现 OutOfMemoryError 的内存区域，它的生命周期随着线程的创建而创建，随着线程的结束而死亡

### 虚拟机栈

#### 概述

定义：每个线程在创建的时候都会创建一个虚拟机栈，其内部保存一个个的栈帧(Stack Frame），对应着一次次 Java 方法调用，是线程私有的，生命周期和线程一致。

作用：主管 Java 程序的运行，它保存方法的局部变量、部分结果，并参与方法的调用和返回。

特点：

- 每个线程运行需要的内存空间，称为虚拟机栈。是线程私有的，每个线程都有各自的 Java 虚拟机栈，而且随着线程的创建而创建，随着线程的死亡而死亡

- Java 虚拟机栈是由一个个栈帧组成，对应着每次调用方法时所占用的内存。每一次函数调用都会有一个对应的栈帧被压入虚拟机栈，每一个函数调用结束后，都会有一个栈帧被弹出。两种返回函数的方式，不管用哪种方式，都会导致栈帧被弹出

  - 正常的函数返回，使用 return 指令

  - 抛出异常

- 每个线程只能有一个活动栈帧，栈顶存放当前当前正在执行的方法

#### 栈中内部结构

每个栈帧中都存储着：

- 局部变量表（Local Variables）

- 操作数栈（Operand Stack）(或称为表达式栈)

- 动态链接（Dynamic Linking）：指向运行时常量池的方法引用

- 方法返回地址（Return Address）：方法正常退出或异常退出的地址

##### 局部变量表

存放了编译期可知的各种基本类型(boolean、byte、char、short、int、float、long、double)、对象引用(reference 类型)和 returnAddress 类型(指向了一条字节码指令的地址)。（Java的基本数据类型不一定存放在栈中，**局部变量的基本类型存放在栈中，引用类型的变量名存放在栈中，但是变量名所指向的对象（基本数据类型）存放在堆中。而成员变量，不管是基本类型还是引用类型，变量名和对象都放在堆中**。）

由于局部变量表是建立在线程的栈上，是线程的私有数据，因此**不存在数据安全问题**

##### 操作数栈

是一个后进先出（Last-In-First-Out）的操作数栈，也可以称为**表达式栈**（Expression Stack）。**主要用于保存计算过程的中间结果，同时作为计算过程中变量临时的存储空间**

操作数栈，在方法执行过程中，根据字节码指令，往操作数栈中写入数据或提取数据，即入栈（push）、出栈（pop）。某些字节码指令将值压入操作数栈，其余的字节码指令将操作数取出栈。使用它们后再把结果压入栈。比如，执行复制、交换、求和等操作

##### 动态链接

每个栈帧都包含一个指向运行时常量池中该栈所属方法的符号引用，在方法调用过程中，会进行动态链接，将这个符号引用转化为直接引用。

- 部分符号引用在类加载阶段的时候就转化为直接引用，这种转化就是静态链接

- 部分符号引用在运行期间转化为直接引用，这种转化就是动态链接

##### 方法返回地址

用来存放调用该方法的 PC 寄存器的值。

一个方法的结束，有两种方式

- 正常执行完成

- 出现未处理的异常，非正常退出

无论通过哪种方式退出，在方法退出后都返回到该方法被调用的位置。方法正常退出时，调用者的 PC 计数器的值作为返回地址，即调用该方法的指令的下一条指令的地址。而通过异常退出的，返回地址是要通过异常表来确定的，栈帧中一般不会保存这部分信息。

本质上，**方法的退出就是当前栈帧出栈的过程**。此时，需要恢复上层方法的局部变量表、操作数栈、将返回值压入调用者栈帧的操作数栈、设置PC寄存器值等，让调用者方法继续执行下去。

正常完成出口和异常完成出口的区别在于：**通过异常完成出口退出的不会给他的上层调用者产生任何的返回**

#### 虚拟机栈的错误

Java 虚拟机栈会出现两种错误：**StackOverFlowError 和 OutOfMemoryError** 。可以通过 -Xss 参数来指定每个线程的虚拟机栈内存大小：

```java
java -Xss2M
```



##### stackOverflowError发生原因

- 虚拟机栈中，栈帧过多（无限递归）

- 每个栈帧所占用内存过大

##### OutOfMemoryError发生原因

- 在单线程程序中，无法出现OOM异常；但是通过循环创建线程（线程体调用方法），可以产生OOM异常。此时OOM异常产生的原因与栈空间是否足够大无关。

- 线程动态扩展，没有足够的内存供申请时会产生OOM

#### 问题辨析

1. 垃圾回收是否涉及栈内存？
   - 不需要。因为虚拟机栈中是由一个个栈帧组成的，在方法执行完毕后，对应的栈帧就会被弹出栈。所以无需通过垃圾回收机制去回收内存。

2. 栈内存的分配越大越好吗？
   - 不是。因为物理内存是一定的，栈内存越大，可以支持更多的递归调用，但是可执行的线程数就会越少。

3. 方法内的局部变量是否是线程安全的？
   - 如果方法内局部变量没有逃离方法的作用范围，则是线程安全的；如果如果局部变量引用了对象，并逃离了方法的作用范围，则需要考虑线程安全问题

#### 线程运行诊断

CPU占用过高

- Linux环境下运行某些程序的时候，可能导致CPU的占用过高，这时需要定位占用CPU过高的线程

- top命令，查看是哪个进程占用CPU过高

- ps H -eo pid, tid（线程id）, %cpu | grep 刚才通过top查到的进程号 通过ps命令进一步查看是哪个线程占用CPU过高

- jstack 进程id 通过查看进程中的线程的nid，刚才通过ps命令看到的tid来对比定位，注意jstack查找出的线程id是16进制的，需要转换

### 本地方法栈

也是线程私有的

虚拟机栈为虚拟机执行 Java 方法服务，而本地方法栈则为虚拟机使用到的 Native 方法服务。Native 方法一般是用其它语言（C、C++等）编写的。

本地方法被执行的时候，在本地方法栈也会创建一个栈帧，用于存放该本地方法的局部变量表、操作数栈、动态链接、出口信息。

使用本地方法的原因：一些带有native关键字的方法就是需要JAVA去调用C或者C++方法，因为**JAVA有时候没法直接和操作系统底层交互，所以需要用到本地方法**

Native Method Stack：它的具体做法是Native Method Stack中登记native方法，在( Execution Engine )执行引擎执行的时候加载Native Libraies

Native Interface本地接口：本地接口的作用是融合不同的编程语言为Java所用，它的初衷是融合C/C程序, Java在诞生的时候是C/C横行的时候，想要立足，必须有调用C、C++的程序，于是就在内存中专门开辟了块区域处理标记为native的代码，它的具体做法是在Native Method Stack 中登记native方法,在( Execution Engine )执行引擎执行的时候加载Native Libraies。  目前该方法使用的越来越少了，除非是与硬件有关的应用，比如通过Java程序驱动打印机或者Java系统管理生产设备，在企业级应用中已经比较少见。因为现在的异构领域间通信很发达，比如可以使用Socket通信,也可以使用Web Service等等

在 Hotspot JVM 中，直接将本地方法栈和虚拟机栈合二为一

**本地方法栈出现异常同虚拟机栈**

### 堆

#### 定义

通过new关键字创建的对象都会被放在堆内存

#### 特点

- 所有线程共享，堆内存中的对象都需要考虑线程安全问题

- 有垃圾回收机制

- 堆中的区域：新生代（ Eden 空间、 From Survivor 、 To Survivor 空间）和老年代。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251633019.gif)

年轻代被分为三个部分——伊甸园（Eden Memory）和两个幸存区（Survivor Memory，被称为from/to或s0/s1），默认比例是8:1:1

 

通过 -Xms 设定程序启动时占用内存大小，通过 -Xmx 设定程序运行期间最大可占用的内存大小。如果程序运行需要占用更多的内存，超出了这个设置值，就会抛出 OutOfMemory 异常。

```java
-Xms1M -Xmx2M
```

伊甸园区是对象创建的区域，但是**伊甸园区如果满了的话会调用轻GC进行垃圾回收**，如果此时对象被引用就会幸存下来进入到幸存区，此时伊甸园区的内存清空，垃圾被回收。**如果说幸存区也满了的话就会进入老年区**。

GC垃圾回收主要是在伊甸园区和老年区。

#### 设置堆内存大小和 OOM

Java 堆用于存储 Java 对象实例，那么堆的大小在 JVM 启动的时候就确定了，我们可以通过 -Xmx 和 -Xms 来设定

- -Xms 用来表示堆的起始内存，等价于 -XX:InitialHeapSize

- -Xmx 用来表示堆的最大内存，等价于 -XX:MaxHeapSize

如果堆的内存大小超过 -Xmx 设定的最大内存， 就会抛出 OutOfMemoryError 异常。

我们通常会将 -Xmx 和 -Xms 两个参数配置为相同的值，其目的是为了能够在垃圾回收机制清理完堆区后不再需要重新分隔计算堆的大小，从而提高性能

- 默认情况下，初始堆内存大小为：电脑内存大小/64

- 默认情况下，最大堆内存大小为：电脑内存大小/4

可以通过代码获取到设置值，当然也可以模拟 OOM：

OOM就是在连老年区都溢出了之后，整个内存已经无法承受，就会报出堆内存溢出的错误。也就是java.lang.OutofMemoryError ：java heap space. 堆内存溢出。

```java
public static void main(String[] args) {

  //返回 JVM 堆大小
  long initalMemory = Runtime.getRuntime().totalMemory() / 1024 /1024;
  //返回 JVM 堆的最大内存
  long maxMemory = Runtime.getRuntime().maxMemory() / 1024 /1024;
    //freeMemory 获取当前程序拿到的内存中，还没用上的，即是可以被 gc 回收的。
    
  System.out.println("-Xms : "+initalMemory + "M");
  System.out.println("-Xmx : "+maxMemory + "M");

  System.out.println("系统内存大小：" + initalMemory * 64 / 1024 + "G");
  System.out.println("系统内存大小：" + maxMemory * 4 / 1024 + "G");
}
```



#### 查看 JVM 堆内存分配

1. 在默认不配置 JVM 堆内存大小的情况下，JVM 根据默认值来配置当前内存大小
2. 默认情况下新生代和老年代的比例是 1:2，可以通过 –XX:NewRatio 来配置
   - 新生代中的 **EdenFrom SurvivorTo Survivor**的比例是**8:1:1，**可以通过-XX:SurvivorRatio来配置

3. JDK 8 默认开启-XX:+UseAdaptiveSizePolicy，**不要随意关闭**-XX:+UseAdaptiveSizePolicy，除非对堆内存的划分有明确的规划
4. 每次 GC 后都会重新计算 Eden、From Survivor、To Survivor 的大小

计算依据是**GC过程**中统计的**GC时间**、**吞吐量**、**内存占用量**

```java
java -XX:+PrintFlagsFinal -version | grep HeapSize
    uintx ErgoHeapSizeLimit                         = 0                                   {product}
    uintx HeapSizePerGCThread                       = 87241520                            {product}
    uintx InitialHeapSize                          := 134217728                           {product}
    uintx LargePageHeapSizeThreshold                = 134217728                           {product}
    uintx MaxHeapSize                              := 2147483648                          {product}
java version "1.8.0_211"
Java(TM) SE Runtime Environment (build 1.8.0_211-b12)
Java HotSpot(TM) 64-Bit Server VM (build 25.211-b12, mixed mode)
```

```java
$ jmap -heap 进程号
```



#### TLAB

TLAB是虚拟机在堆内存的eden划分出来的一块专用空间，是线程专属的。在虚拟机的TLAB功能启动的情况下，在线程初始化时，虚拟机会为每个线程分配一块TLAB空间（包含在 Eden 空间内），只给当前线程使用，这样每个线程都单独拥有一个空间，如果需要分配内存，就在自己的空间上分配，这样就不存在竞争的情况，可以大大提升分配效率。

多线程同时分配内存时，使用 TLAB 可以避免一系列的非线程安全问题，同时还能提升内存分配的吞吐量，因此我们可以将这种内存分配方式称为**快速分配策略**

为什么要有 TLAB ：

- 堆区是线程共享的，任何线程都可以访问到堆区中的共享数据

- 由于对象实例的创建在 JVM 中非常频繁，因此在并发环境下从堆区中划分内存空间是线程不安全的

- 为避免多个线程操作同一地址，需要使用加锁等机制，进而影响分配速度

当然了，不是所有的对象实例都能够在 TLAB 中成功分配内存，但 JVM 确实是将 TLAB 作为内存分配的首选。

在程序中，可以通过 -XX:UseTLAB 设置是否开启 TLAB 空间。

默认情况下，TLAB 空间的内存非常小，仅占有整个 Eden 空间的 1%，可以通过 -XX:TLABWasteTargetPercent 设置 TLAB 空间所占用 Eden 空间的百分比大小。

一旦对象在 TLAB 空间分配内存失败时，JVM 就会尝试着通过使用加锁机制确保数据操作的原子性，从而直接在 Eden 空间中分配内存

#### 逃逸分析技术

##### 对象一定分配在堆中吗？

不一定的，JVM通过「逃逸分析」，那些**逃不出方法的对象**就会在栈上分配。

##### 栈上分配的条件

- 作用域不会逃逸出方法的对象

- 小对象(一般几十个byte)；大对象无法在栈上分配

- 标量替换：若逃逸分析证明一个对象不会逃逸出方法，不会被外部访问，并且这个对象是可以被分解的，那程序在真正执行的时候可能不创建这个对象，而是直接创建这个对象分解后的标量来代替。这样就无需在对对象分配空间了，只在栈上为分解出的变量分配内存即可。

##### 什么是逃逸分析

逃逸分析(Escape Analysis)，是一种可以有效减少Java 程序中同步负载和内存堆分配压力的跨函数全局数据流分析算法。

通过逃逸分析，Java Hotspot编译器能够**分析出一个新的对象的引用的使用范围，从而决定是否要将这个对象分配到堆上。**

- 一个对象在方法中被定义后，对象如果只在方法内部使用，则认为没有发生逃逸；（没有发生逃逸的对象，会在栈上分配）

- 当一个对象在方法中被定义后，它被外部方法所引用，则认为发生了逃逸。

如何快速的判断是否发生了逃逸分析？

- 看new的对象实体是否有可能在方法外被调用。注意是看new 出来的实体，而不是那个引用变量。

通俗点讲，如果一个对象的指针被多个方法或者线程引用时，那么我们就称这个对象的指针发生了逃逸。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251633044.gif)

##### 逃逸分析的好处

- 栈上分配，可以降低垃圾收集器运行的频率。

- 同步消除，如果发现某个对象只能从一个线程可访问，那么在这个对象上的操作不需要同步。

- 标量替换，把对象分解成一个个基本类型，并且内存分配不再是分配在堆上，而是分配在栈上。这样的好处有

  - 减少内存使用，因为不用生成对象头。

  - 程序内存回收效率高，并且GC频率也会减少。

##### 小结：

关于逃逸分析的论文在1999年就已经发表了，但直到JDK 1.6才有实现，而且这项技术到如今也并不是十分成熟的。

**其根本原因就是无法保证逃逸分析的性能消耗一定能高于他的消耗。虽然经过逃逸分析可以做标量替换、栈上分配、和锁消除。但是逃逸分析自身也是需要进行一系列复杂的分析的，这其实也是一个相对耗时的过程。**

一个极端的例子，就是经过逃逸分析之后，发现没有一个对象是不逃逸的。那这个逃逸分析的过程就白白浪费掉了。

### 方法区

#### 结构

方法区与 Java 堆一样，是各个线程共享的内存区域，它用于存储已被虚拟机加载的类信息、常量、静态变量、即时编译器编译后的代码等数据。

对方法区进行垃圾回收的主要目标是**对常量池的回收和对类的卸载**。

**方法区（method area）只是 JVM 规范中定义的一个概念**，用于存储类信息、常量池、静态变量、JIT编译后的代码等数据，并没有规定如何去实现它，不同的厂商有不同的实现。而**永久代（PermGen）是 Hotspot 虚拟机特有的概念， Java8 的时候又被元空间**取代了，永久代和元空间都可以理解为方法区的落地实现。



#### 永久代和元空间

##### 永久代

方法区是 JVM 的规范，而永久代 PermGen 是方法区的一种实现方式，并且只有 HotSpot 有永久代。对于其他类型的虚拟机，如 JRockit 没有永久代。由于方法区主要存储类的相关信息，所以对于动态生成类的场景比较容易出现永久代的内存溢出。

永久区是常驻内存的，是用来存放JDK自身携带的Class对象和interface元数据。这样这些数据就不会占用空间。用于存储java运行时环境。

1. 在JDK1.7前，字符串存放在方法区之中
2. 在JDK1.7后字符串被放在了堆
3. 在Java8，取消了方法区，改用了直接使用直接内存的的元空间。即元空间逻辑上属于堆，但在物理内存上，元空间的内存并不由堆空间内存分配

##### 元空间

JDK 1.8 的时候， HotSpot 的永久代被彻底移除了，使用元空间替代。元空间的本质和永久代类似，都是对JVM规范中方法区的实现。两者最大的区别在于：元空间并不在虚拟机中，而是使用直接内存。

> 为什么要将永久代替换为元空间呢？永久代内存受限于 JVM 可用内存，而元空间使用的是直接内存，受本机可用内存的限制，虽然元空间仍旧可能溢出，但是相比永久代内存溢出的概率更小。

#### 内部结构

方法区用于存储已被虚拟机加载的类信息、常量、静态变量、即时编译器编译后的代码等数据。

##### 类型信息

对每个加载的类型（类 class、接口 interface、枚举 enum、注解 annotation），JVM 必须在方法区中存储以下类型信息

- 这个类型的完整有效名称（全名=包名.类名）

- 这个类型直接父类的完整有效名（对于 interface或是 java.lang.Object，都没有父类）

- 这个类型的修饰符（public，abstract，final 的某个子集）

- 这个类型直接接口的一个有序列表

##### 域（Field）信息

- JVM 必须在方法区中保存类型的所有域的相关信息以及域的声明顺序

- 域的相关信息包括：域名称、域类型、域修饰符（public、private、protected、static、final、volatile、transient 的某个子集）

##### 方法（Method）信息

JVM 必须保存所有方法的

- 方法名称

- 方法的返回类型

- 方法参数的数量和类型

- 方法的修饰符（public，private，protected，static，final，synchronized，native，abstract 的一个子集）

- 方法的字符码（bytecodes）、操作数栈、局部变量表及大小（abstract 和 native 方法除外）

- 异常表（abstract 和 native 方法除外） 
  - 每个异常处理的开始位置、结束位置、代码处理在程序计数器中的偏移地址、被捕获的异常类的常量池索引

##### 运行时常量池

运行时常量池（Runtime Constant Pool）是方法区的一部分）

###### 常量池

一个有效的字节码文件中除了包含类的版本信息、字段、方法以及接口等描述信息外，还包含一项信息那就是常量池表（Constant Pool Table），包含各种字面量和对类型、域和方法的符号引用。

为什么需要常量池？

一个 Java 源文件中的类、接口，编译后产生一个字节码文件。而 Java 中的字节码需要数据支持，通常这种数据会很大以至于不能直接存到字节码里，换另一种方式，可以存到常量池，这个字节码包含了指向常量池的引用。在动态链接的时候用到的就是运行时常量池。

如下，我们通过 jclasslib 查看一个只有 Main 方法的简单类，字节码中的 #2 指向的就是 Constant Pool

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251633055.jpg)

常量池可以看作是一张表，虚拟机指令根据这张常量表找到要执行的类名、方法名、参数类型、字面量等类型。

###### 运行时常量池

- 在加载类和结构到虚拟机后，就会创建对应的运行时常量池

- 常量池表（Constant Pool Table）是 Class 文件的一部分，用于存储编译期生成的各种字面量和符号引用，**这部分内容将在类加载后存放到方法区的运行时常量池中**

- JVM 为每个已加载的类型（类或接口）都维护一个常量池。池中的数据项像数组项一样，是通过索引访问的

- 运行时常量池中包含各种不同的常量，包括编译器就已经明确的数值字面量，也包括到运行期解析后才能够获得的方法或字段引用。此时不再是常量池中的符号地址了，这里换为真实地址
  - 运行时常量池，相对于 Class 文件常量池的另一个重要特征是：**动态性，**Java 语言并不要求常量一定只有编译期间才能产生，运行期间也可以将新的常量放入池中，String 类的intern()方法就是这样的

- 当创建类或接口的运行时常量池时，如果构造运行时常量池所需的内存空间超过了方法区所能提供的最大值，则 JVM 会抛出 OutOfMemoryError 异常

###### 字符串进入串池案例

字符串赋值：

```java
public static void main(String[] args) {
        String a = "a"; 
        String b = "b";
        String ab = "ab";
    }
```

常量池中的信息，都会被加载到运行时常量池中，但这时a b ab 仅是常量池中的符号，还没有成为java字符串

```java
0: ldc           #2                  // String a
2: astore_1
3: ldc           #3                  // String b
5: astore_2
6: ldc           #4                  // String ab
8: astore_3
9: return
```

当执行到 ldc #2 时，会把符号 a 变为 “a” 字符串对象，并放入串池中

当执行到 ldc #3 时，会把符号 b 变为 “b” 字符串对象，并放入串池中

当执行到 ldc #4 时，会把符号 ab 变为 “ab” 字符串对象，并放入串池中

最终**StringTable [“a”, “b”, “ab”]**

注意：字符串对象的创建都是懒惰的，只有当运行到那一行字符串且在串池中不存在的时候（如 ldc #2）时，该字符串才会被创建并放入串池中。



- 使用拼接字符串变量对象创建字符串的过程：

```java
public class StringTableStudy {
    public static void main(String[] args) {
        String a = "a";
        String b = "b";
        String ab = "ab";
        //拼接字符串对象来创建新的字符串
        String ab2 = a+b; //实际StringBuilder拼接形成
    }
}
```

反编译后的结果

```java
Code:
      stack=2, locals=5, args_size=1
         0: ldc           #2                  // String a
         2: astore_1
         3: ldc           #3                  // String b
         5: astore_2
         6: ldc           #4                  // String ab
         8: astore_3
         9: new           #5                  // class java/lang/StringBuilder
        12: dup
        13: invokespecial #6                  // Method java/lang/StringBuilder."<init>":()V
        16: aload_1
        17: invokevirtual #7                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        20: aload_2
        21: invokevirtual #7                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        24: invokevirtual #8                  // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
        27: astore        4
        29: return
```

通过拼接的方式来创建字符串的过程是：StringBuilder().append(“a”).append(“b”).toString()最后的toString方法的返回值是一个新的字符串，虽然字符串的值和拼接的字符串一致，但是两个不同的字符串，**一个存在于串池之中，一个存在于堆内存之中**

```java
String ab = "ab";
String ab2 = a+b;
//结果为false,因为ab是存在于串池之中，ab2是由StringBuilder的toString方法所返回的一个对象，存在于堆内存之中
System.out.println(ab == ab2);
```



- 使用拼接字符串常量对象的方法创建字符串

```java
public class StringTableStudy {
    public static void main(String[] args) {
        String a = "a";
        String b = "b";
        String ab = "ab";
        String ab2 = a+b;
        
    //使用拼接字符串常量的方法创建字符串
        String ab3 = "a" + "b";//ab3直接从串池中获取值,相当于ab3="ab"
    }
}
```



反编译后的结果

```java
Code:
      stack=2, locals=6, args_size=1
         0: ldc           #2                  // String a
         2: astore_1
         3: ldc           #3                  // String b
         5: astore_2
         6: ldc           #4                  // String ab
         8: astore_3
         9: new           #5                  // class java/lang/StringBuilder
        12: dup
        13: invokespecial #6                  // Method java/lang/StringBuilder."<init>":()V
        16: aload_1
        17: invokevirtual #7                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        20: aload_2
        21: invokevirtual #7                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        24: invokevirtual #8                  // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
        27: astore        4
        //ab3初始化时直接从串池中获取字符串
        29: ldc           #4                  // String ab
        31: astore        5
        33: return
```

使用拼接字符串常量的方法来创建新的字符串时，因为内容是常量，javac在编译期会进行优化，结果已在编译期确定为ab，而创建ab的时候已经在串池中放入了“ab”，所以ab3直接从串池中获取值，所以进行的操作和 ab = “ab” 一致。

使用拼接字符串变量的方法来创建新的字符串时，因为内容是变量，只能在运行期确定它的值，所以需要使用StringBuilder来创建



- intern方法 1.8

调用字符串对象的intern方法，会将该字符串对象尝试放入到串池中

- 如果串池中没有该字符串对象，则放入成功

- 如果有该字符串对象，则放入失败

无论放入是否成功，都会返回串池中的字符串对象

注意：此时如果调用intern方法成功，堆内存与串池中的字符串对象是同一个对象；如果失败，则不是同一个对象

例1：

```java
public class Main {
    public static void main(String[] args) {
        //"a" "b" 被放入串池中，str则存在于堆内存之中
        String str = new String("a") + new String("b");
        //调用str的intern方法，这时串池中没有"ab"，则会将该字符串对象放入到串池中，此时堆内存与串池中的"ab"是同一个对象
        String st2 = str.intern();
        //给str3赋值，因为此时串池中已有"ab"，则直接将串池中的内容返回
        String str3 = "ab";
        //因为堆内存与串池中的"ab"是同一个对象，所以以下两条语句打印的都为true
        System.out.println(str == st2);
        System.out.println(str == str3);
    }
}
```



例2：

```java
public class Main {
    public static void main(String[] args) {
        //此处创建字符串对象"ab"，因为串池中还没有"ab"，所以将其放入串池中
        String str3 = "ab";
        //"a" "b" 被放入串池中，str则存在于堆内存之中
        String str = new String("a") + new String("b");
        //此时因为在创建str3时，"ab"已存在于串池中，所以放入失败，但是会返回串池中的"ab"
        String str2 = str.intern();
        //false，str在堆内存，str2在串池
        System.out.println(str == str2);
        //false，str在堆内存，str3在串池
        System.out.println(str == str3);
        //true，str2和str3是串池中的同一个对象
        System.out.println(str2 == str3);
    }
}
```



 

#### 方法区的垃圾回收

方法区的垃圾回收主要有两种，分别是对废弃常量的回收和对无用类的回收。

1. 当一个常量对象不再任何地方被引用的时候，则被标记为废弃常量，这个常量可以被回收。

2. 无用类:

   - 该类所有的实例都已经被回收。

   - 加载该类的类加载器已经被回收。这个条件除非是经过精心设计的可替换类加载器的场景，如 OSGi、JSP 的重加载等，否则通常很难达成

   - 该类对应的 java.lang.Class 对象没有在任何地方被引用，无法在任何地方通过反射访问该类的方法。

虚拟机**可以**对满足上述 3 个条件的类进行回收，但**不一定**会进行回收。是否对类进行回收，HotSpot 虚拟机提供了 -Xnoclassgc 参数进行控制，还可以使用 -verbose:class 以及 -XX:+TraceClassLoading 、-XX:+TraceClassUnLoading 查看类加载和卸载信息。

在大量使用反射、动态代理、CGLib 等 ByteCode 框架、动态生成 JSP 以及 OSGi 这类频繁自定义 ClassLoader 的场景都需要虚拟机具备类卸载的功能，以保证永久代不会溢出。

 

## 内存模型实例

```java
public class  PersonDemo
{
    public static void main(String[] args) 
    {   //局部变量p和形参args都在main方法的栈帧中
        //new Person()对象在堆中分配空间
        Person p = new Person("zs",18);
        p.a = "cn1"；//重新赋值a在堆中,"cn1"被放入串池中
        //sum在栈中，new int[10]在堆中分配空间
        int[] sum = new int[10];
    }
}
class Person //模板在方法区
{   
    //实例变量在堆中。“cn”常量在常量池中
    private String a = "cn";
    //实例变量name和age在堆(Heap)中分配空间
    private String name;
    private int age;
    //类变量(引用类型)name1在方法区(Method Area)和"cn"在常量池中
    private static String name1 = "cn";
    //类变量(引用类型)name2在方法区(Method Area)
    //"cn"已存在，在常量池中，name2指向常量池中的"cn"
    private static String name2 = new String("cn");
    //num在堆中，new int[10]也在堆中
    private int[] num = new int[10];
    
    
    Person(String name,int age)
    {   
        //this及形参name、age在构造方法被调用时
        //会在构造方法的栈帧中开辟空间
        this.name = name;
        this.age = age;
    }
   
   //setName()方法属于类模板，加载在方法区中。但是调用时会压入栈中，并将局部变量name放入，而后name进行值传递传进name的地址
    public void setName(String name)
    {
        this.name = name;
    }
    //speak()方法在方法区中
    public void speak()
    {
        System.out.println(this.name+"..."+this.age);
    }
    //showCountry()方法在方法区中
    public static void  showCountry()
    {
        System.out.println("country");
    }
}
```



其内存模型如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251633035.gif)

这里虽然描述的常量池在方法区中，只作为理解。也可以理解为在堆中，或者元空间