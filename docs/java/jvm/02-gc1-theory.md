---
title: GC - 理论基础
category: Java
tag:
 - JVM
head:
  - - meta
    - name: keywords
      content: Java,JVM,弱引用,软引用,垃圾回收,GC,GC算法,标记清除,标记整理,三色标记,垃圾收集器
  - - meta
    - name: description
      content: 全网最全的的Java JVM知识点总结，让天下没有难学的八股文！
---







## 如何判断一个引用是否存活

### 引用计数法

给对象中添加一个引用计数器，每当有一个地方引用它，计数器就加 1；当引用失效，计数器就减 1；任何时候计数器为 0 的对象就是不可能再被使用的。

优点：可即刻回收垃圾，当对象计数为0时，会立刻回收；

弊端：循环引用时，两个对象的计数都为1，导致两个对象都无法被释放。JVM不用这种算法

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647806.gif)

### 可达性分析算法

通过 GC Root 对象为起点，从这些节点向下搜索，搜索所走过的路径叫引用链，当一个对象到 GC Root没有任何的引用链相连时，说明这个对象是不可用的。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647808.gif)

- JVM中的垃圾回收器通过可达性分析来探索所有存活的对象

- 扫描堆中的对象，看能否沿着GC Root对象为起点的引用链找到该对象，如果找不到，则表示可以回收

### GC Root的对象有哪些？

- 虚拟机栈（栈帧中的本地变量表）中引用的对象，例如各个线程被调用的方法栈用到的参数、局部变量或者临时变量等。　

- 方法区中类静态属性引用的对象或者说Java类中的引用类型的静态变量。

- 方法区中常量引用的对象或者运行时常量池中的引用类型变量。

- 本地方法栈中JNI（即一般说的Native方法）引用的对象

- JVM内部的内存数据结构的一些引用、同步的监控对象（被修饰同步锁）。

### 方法区的回收

因为方法区主要存放永久代对象，而永久代对象的回收率比新生代低很多，因此在方法区上进行回收性价比不高。

主要是对常量池的回收和对类的卸载。

在大量使用反射、动态代理、CGLib 等 ByteCode 框架、动态生成 JSP 以及 OSGi 这类频繁自定义 ClassLoader 的场景都需要虚拟机具备类卸载功能，以保证不会出现内存溢出。

类的卸载条件很多，需要满足以下三个条件，并且满足了也不一定会被卸载:

- 该类所有的实例都已经被回收，也就是堆中不存在该类的任何实例。

- 加载该类的 ClassLoader 已经被回收。

- 该类对应的 Class 对象没有在任何地方被引用，也就无法在任何地方通过反射访问该类方法。

可以通过 -Xnoclassgc 参数来控制是否对类进行卸载。

### finalize()

finalize() 类似 C++ 的析构函数，用来做关闭外部资源等工作。但是 try-finally 等方式可以做的更好，并且该方法运行代价高昂，不确定性大，无法保证各个对象的调用顺序，因此最好不要使用。（Java 9中已弃用）

当一个对象可被回收时，如果需要执行该对象的 finalize() 方法，那么就有可能通过在该方法中让对象重新被引用，从而实现自救。自救只能进行一次，如果回收的对象之前调用了 finalize() 方法自救，后面回收时不会调用 finalize() 方法。

## 引用类型

**四个引用的特点：**

- 强引用：gc时不会回收

- 软引用：只有在内存不够用时，gc才会回收

- 弱引用：只要gc就会回收

- 虚引用：是否回收都找不到引用的对象，仅用于管理直接内存

### 强引用

平时常见的

```java
Object object = new Object();
```

只要一个对象有强引用，垃圾回收器就不会进行回收。即便内存不够了，抛出OutOfMemoryError异常也不会回收。因此强引用是造成java内存泄漏的主要原因之一。 对于一个普通的对象，如果没有其他的引用关系，只要超过了引用的作用域或者显式地将相 应（强）引用赋值为 null，就是可以被垃圾收集的了，具体回收时机还是要看垃圾收集策略。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647804.gif)

```java
/**
 * 一个对象
 * 重写finalize方法，可以知道已经被回收的状态
 */
public class OneObject {
    @Override
    protected void finalize() throws Throwable {
        System.out.println("啊哦~OneObject被回收了");
    }
}

/**
 * 强引用例子
 */
public class ShowStrongReference {
    public static void main(String[] args) {
        // 直接new一个对象，就是强引用
        OneObject oneObject = new OneObject();
        System.out.println("输出对象地址：" + oneObject);
        System.gc();
        System.out.println("第一次gc后输出对象地址：" + oneObject);
        oneObject = null;
        System.gc();
        System.out.println("置为null后gc输出对象地址：" + oneObject);
    }
}

//输出：
输出对象地址：com.esparks.pandora.learning.references.OneObject@72ea2f77
第一次gc后输出对象地址：com.esparks.pandora.learning.references.OneObject@72ea2f77
置为null后gc输出对象地址：null
啊哦~OneObject被回收了
```



### 软引用

特点：软引用通过java.lang.SoftReference类实现。只有在内存不够用时，gc才会回收

软引用的生命周期比强引用短一些。只有当 JVM 认为内存不足时，才会去试图回收软引用指向的对象：即JVM 会确保在抛出 OutOfMemoryError 之前，清理软引用指向的对象。软引用可以和一个引用队列（ReferenceQueue）联合使用，如果软引用所引用的对象被垃圾回收器回收，Java虚拟机就会把这个软引用加入到与之关联的引用队列中。后续，我们可以调用ReferenceQueue的poll()方法来检查是否有它所关心的对象被回收。如果队列为空，将返回一个null；否则该方法返回队列中前面的一个Reference对象。

```java
SoftReference<OneObject> oneObjectSr = new SoftReference<>(new OneObject());
```



![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647817.gif)

当内存足够的时候，垃圾回收器不会进行回收。当内存不够时，就会回收只存在软引用的对象释放内存。

常用于本地缓存处理。

```java
/**
 * 软引用
 * 内存不够了就会回收
 * 注意，运行时需要保证heap大小为35m，即小于实验中全部对象的大小，才能触发gc
 * -Xmx35m
 *
 */
public class ShowSoftReference {
    public static void main(String[] args) {
        // 我们需要通过SoftReference来创建软引用
        SoftReference<OneObject> oneObjectSr = new SoftReference<>(new OneObject());
        // 我们这里创建一个大小为20m的数组
        SoftReference<byte[]> arraySr = new SoftReference<>(new byte[1024 * 1024 * 20]);
        System.out.println("软引用对象oneObjectSr的地址：" + oneObjectSr);
        System.out.println("通过oneObjectSr关联的oneObject对象的地址：" + oneObjectSr.get());
        System.out.println("数组的地址：" + arraySr);
        System.out.println("通过arraySr关联的byte数组的地址：" + arraySr.get());
        System.gc();
        System.out.println("正常gc一次之后，oneObject对象并没有回收。地址" + oneObjectSr.get());

        // 再创建另一个大小为20m的数组，这样heap就不够大了，从而系统自动gc。如果依旧不够，会把已有的软引用关联的对象都回收掉。
        System.out.println("创建另一个大小为20m的数组otherArray");
        byte[] otherArray = new byte[1024 * 1024 * 20];
        System.out.println("otherArray的地址：" + otherArray);

        // gc后，软引用对象还在，但是通过软引用对象创建的对象就被回收了
        System.out.println("现在软引用对象oneObjectSr的地址：" + oneObjectSr);
        System.out.println("通过oneObjectSr关联的oneObject对象的地址：" + oneObjectSr.get());
        System.out.println("现在数组的地址：" + arraySr);
        System.out.println("现在arraySr中关联的byte数组的地址：" + arraySr.get());
    }
}
```

执行代码，可以看到以下输出：

```java
软引用对象oneObjectSr的地址：java.lang.ref.SoftReference@4f8e5cde
通过oneObjectSr关联的oneObject对象的地址：test.niuke.Test1$OneObject@504bae78
数组的地址：java.lang.ref.SoftReference@3b764bce
通过arraySr关联的byte数组的地址：[B@759ebb3d
正常gc一次之后，oneObject对象并没有回收。地址test.niuke.Test1$OneObject@504bae78
创建另一个大小为20m的数组otherArray
otherArray的地址：[B@484b61fc
现在软引用对象oneObjectSr的地址：java.lang.ref.SoftReference@4f8e5cde
通过oneObjectSr关联的oneObject对象的地址：null
现在数组的地址：java.lang.ref.SoftReference@3b764bce
现在arraySr中关联的byte数组的地址：null
```



### 弱引用

特点：弱引用通过WeakReference类实现。只要gc就会回收

弱引用的生命周期比软引用短。在垃圾回收器线程扫描它所管辖的内存区域的过程中，一旦发现了具有弱引用的对象，不管当前内存空间足够与否，都会回收它的内存。由于垃圾回收器是一个优先级很低的线程，因此不一定会很快回收弱引用的对象。弱引用可以和一个引用队列（ReferenceQueue）联合使用，如果弱引用所引用的对象被垃圾回收，Java虚拟机就会把这个弱引用加入到与之关联的引用队列中。

```java
WeakReference<OneObject> oneObjectWr = new WeakReference<>(new OneObject());
```



![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647819.gif)

只要发生gc，就会回收只存在弱引用的对象。

常用于Threadlocal。

```java
/**
 * 弱引用
 * 只要gc就会回收
 */
public class ShowWeakReference {
    public static void main(String[] args) {
        // 我们需要通过WeakReference来创建弱引用
        WeakReference<OneObject> objectWr = new WeakReference<>(new OneObject());
        System.out.println("弱引用objectWr的地址：" + objectWr);
        System.out.println("弱引用objectWr关联的oneObject对象的地址：" + objectWr.get());

        System.gc();

        // gc后，弱引用对象还在，但是通过弱引用对象创建的对象就被回收了
        System.out.println("gc后，弱引用objectWr的地址：" + objectWr);
        System.out.println("gc后，弱引用objectWr关联的oneObject对象的地址：" + objectWr.get());
    }
}
```

执行代码，可以看到以下输出：

```java
弱引用objectWr的地址：java.lang.ref.WeakReference@72ea2f77
弱引用objectWr关联的oneObject对象的地址：com.esparks.pandora.learning.references.OneObject@33c7353a
gc后，弱引用objectWr的地址：java.lang.ref.WeakReference@72ea2f77
gc后，弱引用objectWr关联的oneObject对象的地址：null
啊哦~OneObject被回收了
```



### 虚引用

特点：虚引用也叫幻象引用，通过PhantomReference类来实现。是否回收都找不到引用的对象，仅用于管理直接内存

无法通过虚引用访问对象的任何属性或函数。幻象引用仅仅是提供了一种确保对象被 finalize 以后，做某些事情的机制。如果一个对象仅持有虚引用，那么它就和没有任何引用一样，在任何时候都可能被垃圾回收器回收。虚引用必须和引用队列 （ReferenceQueue）联合使用。当垃圾回收器准备回收一个对象时，如果发现它还有虚引用，就会在回收对象的内存之前，把这个虚引用加入到与之关联的引用队列中。 程序可以通过判断引用队列中是否已经加入了虚引用，来了解被引用的对象是否将要被垃圾回收。如果程序发现某个虚引用已经被加入到引用队列，那么就可以在所引用的对象的内存被回收之前采取一些程序行动。

```java
private ReferenceQueue<OneObject> queue = new ReferenceQueue<>();
PhantomReference<OneObject> oneObjectPr = new PhantomReference<>(new OneObject(), queue);
```

无论是否gc，其实都获取不到通过PhantomReference创建的对象。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647823.gif)

**其仅用于管理直接内存，起到通知的作用。**

这里补充一下背景。因为垃圾回收器只能管理JVM内部的内存，无法直接管理系统内存的。对于一些存放在系统内存中的数据，JVM会创建一个引用（类似于指针）指向这部分内存。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647416.gif)

当这个引用在回收的时候，就需要通过虚引用来管理指向的系统内存。这里还需要依赖一个队列来实现。当触发gc对一个虚引用对象回收时，会将虚引用放入创建时指定的ReferenceQueue中。之后单独对这个队列进行轮询，并做额外处理。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647438.gif)

 

```java
/**
 * 虚引用
 * 只用于管理直接内存，起到通知的作用
 */
public class ShowPhantomReference {
    /**
     * 虚引用需要的队列
     */
    private static final ReferenceQueue<OneObject> QUEUE = new ReferenceQueue<>();

    public static void main(String[] args) {
        // 我们需要通过 PhantomReference来创建虚引用
        PhantomReference<OneObject> objectPr = new PhantomReference<>(new OneObject(), QUEUE);
        System.out.println("虚引用objectPr的地址：" + objectPr);
        System.out.println("虚引用objectPr关联的oneObject对象的地址：" + objectPr.get());

        // 触发gc，然后检查队列中是否有虚引用
        while (true) {
            System.gc();
            Reference<? extends OneObject> poll = QUEUE.poll();
            if (poll != null) {
                System.out.println("队列里找到objectPr啦" + poll);
                break;
            }
        }
    }
}
```

输出：

```java
虚引用objectPr的地址：java.lang.ref.PhantomReference@72ea2f77
虚引用objectPr关联的oneObject对象的地址：null
队列里找到objectPr啦null
队列里找到objectPr啦java.lang.ref.PhantomReference@72ea2f77
```



### 终结器引用

所有的类都继承自Object类，Object类有一个finalize方法。当某个对象不再被其他的对象所引用时，会先将终结器引用对象放入引用队列中，然后根据终结器引用对象找到它所引用的对象，然后调用该对象的finalize方法。调用以后，该对象就可以被垃圾回收了如上图，B对象不再引用A4对象。这时终结器对象就会被放入引用队列中，引用队列会根据它，找到它所引用的对象。然后调用被引用对象的finalize方法。调用以后，该对象就可以被垃圾回收了

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647458.gif)

### 引用队列

软引用和弱引用可以配合引用队列

在弱引用和虚引用所引用的对象被回收以后，会将这些引用放入引用队列中，方便一起回收这些软/弱引用对象

虚引用和终结器引用必须配合引用队列

虚引用和终结器引用在使用时会关联一个引用队列



### 四种引用的应用场景

- 强引用：普通用法

- 软引用：缓存，软引用可以用于缓存非必须的数据

- 弱引用：防止一些关于map的内存泄漏。Threadlocal中防内存泄漏；线程池，当一个线程不再使用时，垃圾回收器会回收其所占用的内存空间，以便释放资源。

- 虚引用：用来管理直接内存

## 垃圾回收简介

### Minor GC、Major GC、Full GC

JVM 在进行 GC 时，并非每次都对堆内存（新生代、老年代；方法区）区域一起回收的，大部分时候回收的都是指新生代。

针对 HotSpot VM 的实现，它里面的 GC 按照回收区域又分为两大类：部分收集（Partial GC），整堆收集（Full GC）

- 部分收集：不是完整收集整个 Java 堆的垃圾收集。其中又分为：

  - 新生代收集（Minor GC/Young GC）：只是新生代的垃圾收集

  - 老年代收集（Major GC/Old GC）：只是老年代的垃圾收集

    - 目前，只有 CMS GC 会有单独收集老年代的行为

    - 很多时候 Major GC 会和 Full GC 混合使用，需要具体分辨是老年代回收还是整堆回收

  - 混合收集（Mixed GC）：收集整个新生代以及部分老年代的垃圾收集
    - 目前只有 G1 GC 会有这种行为

- 整堆收集（Full GC）：收集整个 Java 堆和方法区的垃圾

### 对象在堆中的生命周期

1. 在 JVM 内存模型的堆中，堆被划分为新生代和老年代 
   - 新生代又被进一步划分为 **Eden区Survivor区From SurvivorTo Survivor**

2. 当创建一个对象时，对象会被优先分配到新生代的 Eden 区 
   - 此时 JVM 会给对象定义一个**对象年轻计数器** -XX:MaxTenuringThreshold

3. 当 Eden 空间不足时，JVM 将执行新生代的垃圾回收（Minor GC） 

   - JVM 会把存活的对象转移到 Survivor 中，并且对象年龄 +1

   - 对象在 Survivor 中同样也会经历 Minor GC，每经历一次 Minor GC，对象年龄都会+1

4. 如果分配的对象超过了 -XX:PetenureSizeThreshold **直接被分配到老年代**

### 内存分配策略

- **对象优先在 Eden 分配：** 大多数情况下，对象在新生代 Eden 上分配，当 Eden 空间不够时，触发 Minor GC 

- **大对象直接进入老年代：** 当遇到一个较大的对象时，就算新生代的伊甸园为空，也无法容纳该对象时，会将该对象直接晋升为老年代，最典型的大对象有长字符串和大数组。可以设置JVM参数 -XX:PretenureSizeThreshold ，大于此值的对象直接在老年代分配。

- **长期存活的对象进入老年代：** 通过参数 -XX:MaxTenuringThreshold 可以设置对象进入老年代的年龄阈值。对象在 Survivor 区每经过一次 Minor GC ，年龄就增加 1 岁，当它的年龄增加到一定程度，就会被晋升到老年代中。

- **动态对象年龄判定：** 并非对象的年龄必须达到 MaxTenuringThreshold 才能晋升老年代，如果在 Survivor 中相同年龄所有对象大小的总和大于 Survivor 空间的一半，则年龄大于或等于该年龄的对象可以直接进入老年代，无需达到 MaxTenuringThreshold 年龄阈值。

- **空间分配担保：** 在发生 Minor GC 之前，虚拟机先检查老年代最大可用的连续空间是否大于新生代所有对象总空间，如果条件成立的话，那么 Minor GC 是安全的。如果不成立的话虚拟机会查看HandlePromotionFailure 的值是否允许担保失败。如果允许，那么就会继续检查老年代最大可用的连续空间是否大于历次晋升到老年代对象的平均大小，如果大于，将尝试着进行一次 Minor GC,尽管这次Minor GC是有风险的；（也就是说，会把原先新生代的对象挪到老年代中） ；如果小于，或者 HandlePromotionFailure 的值为不允许担保失败，那么就要进行一次 Full GC 。

 

下面解释一下空间分配担保时的 “冒险”是冒了什么风险？

新生代使用复制收集算法，但为了内存利用率，只使用其中一个Survivor空间来作为轮换备份，因此当出现大量对象在Minor GC后仍然存活的情况（最极端的情况就是内存回收后新生代中所有对象都存活），就需要老年代进行分配担保，把Survivor无法容纳的对象直接进入老年代。但前提是老年代本身还有容纳这些对象的剩余空间，一共有多少对象会活下来在实际完成内存回收之前是无法明确知道的，所以只好取之前每一次回收晋升到老年代对象容量的平均大小值作为经验值，与老年代的剩余空间进行比较，决定是否进行Full GC来让老年代腾出更多空间。

取平均值进行比较其实仍然是一种动态概率的手段，也就是说，如果某次Minor GC存活后的对象突增，远远高于平均值的话，依然会导致担保失败（Handle Promotion Failure）。如果出现了HandlePromotionFailure失败，那就只好在失败后重新发起一次Full GC。虽然担保失败时绕的圈子是最大的，但大部分情况下都还是会将HandlePromotionFailure开关打开，避免Full GC过于频繁

### Full GC 的触发条件

对于 Minor GC，其触发条件非常简单，当 Eden 空间满时，就将触发一次 Minor GC。而 Full GC 则相对复杂，有以下条件:•

- **用 System.gc()：** 只是建议虚拟机执行 Full GC，但是虚拟机不一定真正去执行。不建议使用这种方式，而是让虚拟机管理内存。

- **老年代空间不足：** 老年代空间不足的常见场景为前文所讲的大对象直接进入老年代、长期存活的对象进入老年代等。为了避免以上原因引起的 Full GC，应当尽量不要创建过大的对象以及数组、注意编码规范避免内存泄露。除此之外，可以通过 -Xmn 参数调大新生代的大小，让对象尽量在新生代被回收掉，不进入老年代。还可以通过 -XX:MaxTenuringThreshold 调大对象进入老年代的年龄，让对象在新生代多存活一段时间。

- **空间分配担保失败：** 当程序创建一个大对象时，Eden区域放不下大对象，使用复制算法的 Minor GC 需要老年代的内存空间作担保，如果担保失败会执行一次 Full GC。

- **JDK 1.7 及以前的永久代空间不足：** 在 JDK 1.7 及以前，HotSpot 虚拟机中的方法区是用永久代实现的，永久代中存放的为一些 Class 的信息、常量、静态变量等数据。当系统中要加载的类、反射的类和调用的方法较多时，永久代可能会被占满，在未配置为采用 CMS GC 的情况下也会执行 Full GC。如果经过 Full GC 仍然回收不了，那么虚拟机会抛出 java.lang.OutOfMemoryError 。（**JDK 8以后元空间不足**）

- Concurrent Mode Failure：执行 CMS GC 的过程中同时有对象要放入老年代，而此时老年代空间不足(可能是 GC 过程中浮动垃圾过多导致暂时性的空间不足)，便会报 Concurrent Mode Failure 错误，并触发 Full GC。

### Java对象内存分配过程

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647486.jpg)

对象的分配过程：

1. 编译器通过逃逸分析优化手段，确定对象是否在栈上分配还是堆上分配。
2. 如果在堆上分配，则确定是否大对象，如果是则直接进入老年代空间分配， 不然则走3。
3. 对比tlab， 如果tlab_top + size <= tlab_end， 则在tlab上直接分配，并且增加tlab_top值，如果tlab不足以空间放当前对象，则重新申请一个tlab尝试放入当前对象，如果还是不行则往下走4。
4. 分配在Eden空间，当eden空间不足时发生YGC， 幸存者区是否年龄晋升、动态年龄、老年代剩余空间不足发生Full GC 。
5. 当YGC之后仍然不足当前对象放入，则直接分配老年代。

TLAB**作用原理**：Java在内存新生代Eden区域开辟了一小块线程私有区域，这块区域为TLAB，默认占Eden区域大小的1%， 作用于小对象，因为小对象用完即丢，不存在线程共享，快速消亡GC，JVM优先将小对象分配在TLAB是线程私有的，所以没有锁的开销，效率高，每次只需要线程在自己的缓冲区分配即可，不需要进行锁同步堆 。

 

对象除了基本类型的不一定是在堆内存分配，在JVM拥有逃逸分析，能够分析出一个新的对象所拥有的范围，从而决定是否要将这个对象分配到堆上，是JVM的默认行为；Java 逃逸分析是一种优化技术，可以通过分析 Java 对象的作用域和生命周期，确定对象的内存分配位置和生命周期，从而减少不必要的内存分配和垃圾回收。可以在栈上分配，可以在栈帧上创建和销毁，分离对象或标量替换，同步消除。

 

## 垃圾回收算法

### 标记清除算法

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647508.gif)

 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647531.gif)

**定义：** 标记清除算法顾名思义，将存活的对象进行标记，然后清理掉未被标记的对象，给堆内存腾出相应的空间

- 这里的腾出内存空间并不是将内存空间的字节清0，而是记录下这段内存的起始结束地址，下次分配内存的时候，会直接覆盖这段内存

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647013.gif)

**优点：**

- 实现简单，与其他算法组合也简单

- 与保守式GC算法兼容，因为他们都不需要移动对象。

**缺点：**

- 碎片化。简单来说就是随着分配和回收的进行会产生很多小的空闲对象散落在堆中，彼此也不连续。碎片化带来的问题是无法分配大的空闲空间，尽管总的空闲空间是够用的。碎片化带来的另一个问题是局部性原理失效，因为具有引用关系的数据分配到的空闲空间并不连续。

- 分配速度慢。因为空闲链表是单链表结构，分配时需要遍历链表，时间复杂度是O(n)。

- 与写时复制不兼容。因为标记阶段会修改堆内对象，导致大量拷贝。

### 标记整理

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647029.gif)

 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647079.gif)

GC标记压缩算法分为**标记阶段**和**压缩阶段**。它是将GC标记清除算法的清除阶段换成了压缩，而且这里的压缩不是将活动对象从一个空间复制到另一个空间，而是将活动对象整体前移，挤占非活动对象的空间。

**优点：**

- 堆的利用率高

- 分配速度快

- 不会产生碎片化

GC标记压缩算法缺点是吞吐量低。因为在压缩阶段我们需要遍历堆3次，耗费时间与堆大小成正比，堆越大，耗费时间越久。

### 复制算法

GC复制算法的思路是将堆一分为二，暂时叫它们A堆和B堆。申请内存时，统一在A堆分配，当A堆用完了，将A堆中的活动对象全部复制到B堆，然后A堆的对象就可以全部回收了。这时不需要将B堆的对象又搬回A堆，只需要将A和B互换以下就行了，这样原来的A堆变成B堆，原来的B堆变成了A堆。经过这一轮复制，活动对象搬了新家，垃圾也被回收了。

GC复制算法就是在两个堆之间来回倒腾。JVM中的新生区就是使用的这种方式，总结为：在幸存区中，谁空谁是to

由于对象地址发生了变化，GC复制算法在复制过程中还需要重写指针。从复制的角度来看，活动对象是从A堆复制到B堆。因此我们也将A堆称为From空间，将B堆称为To空间。经过复制，原本散落在From空间中的活动对象被集中放到了To空间开头的连续空间内，这一过程也叫做压缩。

现在的虚拟机都采用这种收集算法来回收新生代，但是并不是将新生代划分为大小相等的两块，而是分为一块较大的 Eden 空间和两块较小的 Survivor 空间，每次使用 Eden 空间和其中一块 Survivor。在回收时，将 Eden 和 Survivor 中还存活着的对象一次性复制到另一块 Survivor 空间上，最后清理 Eden 和使用过的那一块 Survivor。

HotSpot 虚拟机的 Eden 和 Survivor 的大小比例默认为 8:1，保证了内存的利用率达到 90%。如果每次回收有多于 10% 的对象存活，那么一块 Survivor 空间就不够用了，此时需要依赖于老年代进行分配担保，也就是借用老年代的空间存储放不下的对象

**优点：**

- 吞吐量优秀。这得益于GC复制算法只会搜索复制活动对象，能在较短时间内完成GC，而且时间与堆的大小无关，只与活动对象数成正比。相比于需要搜索整个堆的GC标记清除算法，GC复制算法吞吐量更高，而且堆越大，差距越明显。

- 分配速度快。因为不需要搜索空闲链表，在O(1)的时间复杂度就能完成分配。

- 不会发生碎片化。因为每次复制都会执行压缩。

- 与缓存兼容。因为复制过程中使用了深度优先遍历，具有引用关系的对象会被复制到相邻的位置，局部性原理可以很好发挥作用。

**缺点：**

- 堆的使用效率低。这是一个最显眼的问题，因为要留一半的空间用来复制，所以堆的利用率总小于50%。

- 不兼容保守式GC。因为GC复制算法需要移动对象。

- 复制时存在递归调用，需要消耗栈空间，并可能导致栈溢出。

### 分代回收

根据对象存活周期将内存划分为几块，不同块采用适当的收集算法。

一般将堆分为新生代和老年代。

- 新生代使用: 复制算法

- 老年代使用: 标记 - 清除 或者 标记 - 整理 算法

开始新创建的对象都被放在了新生代的伊甸园中

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647108.gif)

当多创建几个对象的后发现伊甸园装不下了。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647201.gif)

当伊甸园中的内存不足时，就会进行一次垃圾回收，这时的回收叫做轻GC Minor GC。一次小的垃圾回收，根据可达性算法找到不能被回收的，把这些不能被回收的对象复制到幸存区To中（用的复制算法），然后把幸存的对象的寿命+1，然后回收掉伊甸园里面的全部对象。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647259.gif)

再把From区和To区的指向互换,那么这是to区就又空出来了

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647553.gif)

再次创建对象，若新生代的伊甸园又满了，则会再次触发 Minor GC（会触发 stop the world， 暂停其他用户线程，只让垃圾回收线程工作），

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647588.gif)

这时不仅会回收伊甸园中的垃圾，还会回收幸存区中的垃圾，From也会被垃圾回收检查，再将活跃对象复制到幸存区TO中。回收以后会交换两个幸存区，并让幸存区中的对象寿命加1。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647636.gif)

> 这里1是从伊甸园区新进幸存区的对象，2是原本就存活在幸存区寿命+1后为2

这时就有对象在两次GC中存活下来那么他的存活次数就会是2，如果幸存区中的对象的寿命超过某个阈值（最大为15，4bit），那么就会晋升到老年代中去

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647663.gif)

因为老年代的垃圾回收频率比较低,这个对象在新生代里面反复GC都没有回收掉说明长时间在用,那么就没有必要在新生代中反复GC

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647744.gif)

如果新生代老年代中的内存都满了，就会先触发Minor GC，再触发Full GC，扫描新生代和老年代中所有不再使用的对象并回收。如果两次GC后还是放不下， 就会报OOM异常

在报堆内存溢出之前，还会去尝试minorGC一次如果minorGC了释放出来的空间还是放不下，那么就会触发一次fullGC(类似于大扫除,老年代和新生代都会被GC)，如果还是没办法放下那么就会报java.lang.OutOfMemoryError: Java heap space

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647797.gif)

**总结:** 在新生代中，每次垃圾收集时都有大批对象死去，只有少量存活，使用复制算法比较合适，只需要付出少量存活对象的复制成本就可以完成收集。老年代对象存活率高，适合使用标记-清理或者标记-整理算法进行垃圾回收。

## 并发标记算法（三色标记法）

CMS和G1在并发标记时使用的是同一个算法：三色标记法，使用白灰黑三种颜色标记对象。白色是未标记；灰色自身被标记，引用的对象未标记；黑色自身与引用对象都已标记。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647055.gif)

GC 开始前所有对象都是白色，GC 一开始所有根能够直达的对象被压到栈中，待搜索，此时颜色是灰色。然后灰色对象依次从栈中取出搜索子对象，子对象也会被涂为灰色，入栈。当其所有的子对象都涂为灰色之后该对象被涂为黑色。当 GC 结束之后灰色对象将全部没了，剩下黑色的为存活对象，白色的为垃圾。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647114.gif)

## 垃圾收集器

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647149.jpg)

以上是 HotSpot 虚拟机中的 7 个垃圾收集器，连线表示垃圾收集器可以配合使用。

**并行收集：** 指多条垃圾收集线程并行工作，但此时用户线程仍处于等待状态。

**并发收集：** 指用户线程与垃圾收集线程同时工作（不一定是并行的可能会交替执行）。用户程序在继续运行，而垃圾收集程序运行在另一个CPU上

**吞吐量：** 即CPU用于运行用户代码的时间与CPU总消耗时间的比值（吞吐量 = 运行用户代码时间 / ( 运行用户代码时间 + 垃圾收集时间 )），也就是。例如：虚拟机共运行100分钟，垃圾收集器花掉1分钟，那么吞吐量就是99%

### 串行Serial / Serial Old 收集器

特点：单线程、简单高效（与其他收集器的单线程相比），采用**复制算法**。对于限定单个CPU的环境来说，Serial收集器由于没有线程交互的开销，专心做垃圾收集自然可以获得最高的单线程收集效率。收集器进行垃圾回收时，必须暂停其他所有的工作线程，直到它结束（Stop The World）参数：-XX:+UseSerialGC  -XX:+UseSerialOldGC

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647174.gif)

**安全点：** 让其他线程都在这个点停下来，以免垃圾回收时移动对象地址，使得其他线程找不到被移动的对象因为是串行的，所以只有一个垃圾回收线程。且在该线程执行回收工作时，其他线程进入阻塞状态

**Serial Old**是Serial收集器的老年代版本：采用**标记整理**算法



**特点：**

- 单线程收集器

  - 收集效率高，不会产生对象引用变更

  - STW时间长

- 使用场景：适合内存小几十兆以内，比较适合简单的服务或者单CPU服务，避免了线程交互的开销。

- 优点：小堆内存且单核CPU执行效率高。

- 缺点：堆内存大，多核CPU不适合，回收时长非常长。

### ParNew 收集器

年轻代：-XX:+UserParNewGC 老年代搭配 CMS

ParNew收集器其实就是Serial收集器的多线程版本

- **特点：** 多线程、ParNew收集器默认开启的收集线程数与CPU的数量相同，在CPU非常多的环境中，可以使用-XX:ParallelGCThreads参数来限制垃圾收集的线程数。和Serial收集器一样存在Stop The World问题

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647242.gif)

### CMS 收集器

老年代：-XX:+UserConcMarkSweepGC年轻代搭配ParNew

Concurrent Mark Sweep，一种以获取最短回收停顿时间为目标的老年代收集器

- **特点：** 基于**标记清除算法**实现。并发收集、低停顿，但是会产生内存碎片

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647453.jpg)

运行过程分分为下列4步：

1. **初始标记：** 标记GCRoots直接关联的对象以及年轻代指向老年代的对象，会发生Stop the word。但是这个阶段的速度很快，因为没有向下追溯，即只标记一层。

> 例如：Math math = new Math();此时new Math()即为math的直接引用对象，再往下为间接引用不做记录，例如构造方法中引用了其他成员变量

2. **并发标记：** 接着从gc roots的直接引用对象开始遍历整条引用链并进行标记，此过程耗时较长，但无需停顿用户线程，可与垃圾收集线程一起并发运行。由于用户线程继续运行，因此可能会导致已经标记过的对象状态发生改变，这个阶段采用**三色标记算法**， 在对象头(Mark World)标识了一个颜色属性，不同的颜色代表不同阶段，扫描过程中给与对象一个颜色，记录扫描位置，防止cpu时间片切换不需要重新扫描。
3. **重新标记：** 为了**修正并发标记**期间因用户程序继续运行而导致标记产生变动的那一部分对象的标记记录。仍然存在Stop The World问题，这里会慢一些
4. **并发清理：** 标记结束之后开启用户线程，同时垃圾收集线程也开始对未标记的区域进行清除，此阶段若有新增对象则会被标记为黑色，不做任何处理

在整个过程中耗时最长的并发标记和并发清除过程中，收集器线程都可以与用户线程一起工作，不需要进行停顿。



- **应用场景：**适用于注重服务的响应速度，希望系统停顿时间最短，给用户带来更好的体验等场景下。如web程序、b/s服务

  

- **优点：**

  - 并发收集；

  - STW时间相对短，低停顿；

    

- **缺点：**
  - 吞吐量低: 低停顿时间是以牺牲吞吐量为代价的，导致 CPU 利用率不够高。
  - 内存碎片问题：CMS本质上是实现了标记清除算法的收集器，这意味着会产生内存碎片，当碎片化非常严重的时候，这时候有大对象进入无法分配内存时会触发FullGC，特殊场景下会使用Serial收集器，导致停顿不可控。
  - 无法处理浮动垃圾，需要预留空间，可能出现 Concurrent Mode Failure。浮动垃圾是指并发清除阶段由于用户线程继续运行而产生的垃圾，这部分垃圾只能到下一次 GC 时才能进行回收。由于浮动垃圾的存在，因此需要预留出一部分内存，意味着 CMS 收集不能像其它收集器那样等待老年代快满的时候再回收。如果预留的内存不够存放浮动垃圾，就会出现 Concurrent Mode Failure，这时虚拟机将临时启用 Serial Old 来替代 CMS，会导致停顿时间更长

#### 三色标记算法

三色标记

- 黑色：代表了自己已经被扫描完毕，并且自己的引用对象也已经确定完毕。

- 灰色：代表自己已经被扫描完毕了， 但是自己的引用还没标记完。

- 白色：则代表还没有被扫描过。标记过程结束后，所有未被标记的对象都是不可达的，可以被回收。



三色标记算法的**问题场景**：当业务线程做了对象引用变更，会发生B对象不会被扫描，当成垃圾回收。

```java
public class Demo3 {
 
    public static void main(String[] args) {
        R r = new R();
        r.a = new A();
        B b = new B();
        // GCroot遍历R， R为黑色， R下面的a引用链还未扫完置灰灰色，R.b无引用， 切换时间分片
        r.a.b = b;
        // 业务线程发生了引用改变， 原本r.a.b的引用置为null
        r.a.b = null;
        // GC线程回来继续上次扫描，发现r.a.b无引用，则认为b对象无任何引用清除
        r.b = b;
        // GC 回收了b， 业务线程无法使用b
    }
}
 
class R {
    A a;
    B b;
}
 
class A {
    B b;
}
 
class B {
}
```

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251659654.png)

当GC线程标记A时，CPU时间片切换，业务线程进行了对象引用改变，这时候时间片回到了GC线程，继续扫描对象A， 发现A没有任何引用，则会将A赋值黑色扫描完毕，这样B则不会被扫描，会标记B是垃圾， 在清理阶段将B回收掉，错误的回收正常的对象，发生业务异常。

CMS基于这种错误标记的解决方案是采取**写屏障 + 增量更新Incremental Update** ， 在业务线程发生对象变化时，重新将R标识为灰色，重新扫描一遍，Incremental Update 在特殊场景下还是会产生漏标。即当黑色对象被新增一个白色对象的引用的时候，记录下发生引用变更的黑色对象，并将它重新改变为灰色对象，重新标记。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251659457.png)

 ```java
 public class Demo3 {
  
     public static void main(String[] args) {
         // Incremental Update还会产生的问题
         R r = new R();
         A a = new A();
         A b = new A();
         r.a1 = a;
         // GC线程切换， r扫完a1， 但是没有扫完a2， 还是灰色
         r.a2 = b;
         // 业务线程发生引用切换， r置灰灰色（本身灰色）
         r.a1 = b;
         // GC线程继续扫完a2， R为黑色， b对象又漏了~
     }
 }
  
 class R {
     A a1;
     A a2;
 }
  
 class A {
 }
 ```

当GC 1线程正在标记O， 已经标记完O的属性 O.1， 准备标记O.2时，业务线程把属性O,1 = B，这时候将O对象再次标记成灰色， GC 1线程切回，将O.2线程标记完成，这时候认为O已经全部标记完成，O标记为黑色， B对象产生了漏标， CMS针对Incremental Update产生的问题，只能在remark阶段，暂停所有线程，将这些发生过引用改变过的，重新扫描一遍。

### 吞吐量优先Parallel

- 多线程

- 堆内存较大，多核CPU

- 单位时间内，STW（stop the world，停掉其他所有工作线程）时间最短

- JDK1.8默认使用的垃圾回收器

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251647585.gif)

#### Parallel Scavenge 收集器

新生代收集器，基于**复制算法**实现的收集器。特点是吞吐量优先，故也称为吞吐量优先收集器，能够并行收集的多线程收集器，允许多个垃圾回收线程同时运行，降低垃圾收集时间，提高吞吐量。 Parallel Scavenge 收集器关注点是吞吐量，高效率的利用 CPU 资源。 CMS 垃圾收集器关注点更多的是用户线程的停顿时间。

Parallel Scavenge 收集器提供了两个参数用于精确控制吞吐量，分别是控制最大垃圾收集停顿时间的

-XX：MaxGCPauseMillis 参数以及直接设置吞吐量大小的-XX：GCTimeRatio 参数。

- -XX：MaxGCPauseMillis 参数的值是一个大于0的毫秒数，收集器将尽量保证内存回收花费的时间不超过用户设定值。

- -XX：GCTimeRatio 参数的值大于0小于100，即垃圾收集时间占总时间的比率，相当于吞吐量的倒数。

该收集器的目标是达到一个可控制的吞吐量。还有一个值得关注的点是：**GC自适应调节策略**（与ParNew收集器最重要的一个区别）

> **GC自适应调节策略：** Parallel Scavenge收集器可设置-XX:+UseAdptiveSizePolicy参数。当开关打开时不需要手动指定新生代的大小（-Xmn）、Eden与Survivor区的比例（-XX:SurvivorRation）、晋升老年代的对象年龄（-XX:PretenureSizeThreshold）等，虚拟机会根据系统的运行状况收集性能监控信息，动态设置这些参数以提供最优的停顿时间和最高的吞吐量，这种调节方式称为GC的自适应调节策略。

- 使用场景：适用于内存在几个G之间，适用于后台计算服务或者不需要太多交互的服务，保证吞吐量的服务。

- 优点：可控吞吐量、保证吞吐量，并行收集。

- 缺点：回收期间STW，随着堆内存增大，回收暂停时间增大。

  

#### Parallel Old 收集器

是Parallel Scavenge收集器的老年代版本

特点：多线程，采用**标记整理算法**（老年代没有幸存区）

- 响应时间优先

- 多线程

- 堆内存较大，多核CPU

- 尽可能让单次STW时间变短（尽量不影响其他线程运行）

 

 


<!-- @include: @article-footer.snippet.md -->     

