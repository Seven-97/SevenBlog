---
title: JUC锁 - CAS & Unsafe & 原子类
category: Java
tag:
 - 并发编程
head:
  - - meta
    - name: keywords
      content: Java,并发编程,多线程,Thread,cas,Unsafe,ABA问题,无锁并发,原子类,Atomic,LongAdder
  - - meta
    - name: description
      content: 全网最全的Java并发编程知识点总结，让天下没有难学的八股文！
---





## CAS

### 介绍

CAS 可以保证对共享变量操作的原子性

CAS全称Compare And Swap，比较与交换，是乐观锁的主要实现方式。CAS在不使用锁的情况下实现多线程之间的变量同步。ReentrantLock内部的AQS和原子类内部都使用了CAS。

CAS算法涉及到三个操作数：需要读写的内存值V。进行比较的值A。要写入的新值B。只有当V的值等于A时，才会使用原子方式用新值B来更新V的值，否则会继续重试直到成功更新值。

以AtomicInteger为例，AtomicInteger的getAndIncrement()方法底层就是CAS实现，关键代码是 compareAndSwapInt(obj, offset, expect, update)，其含义就是，如果obj内的value和expect相等，就证明没有其他线程改变过这个变量，那么就更新它为update，如果不相等，那就会继续重试直到成功更新值。

### CAS存在的问题

CAS不加锁，保证一次性，但是需要多次比较

#### 循环时间长，开销大

因为执行的是do while，如果比较不成功一直在循环，最差的情况，就是某个线程一直取到的值和预期值都不一样，这样就会无限循环）

解决方案：可以使用java8中的LongAdder，分段CAS和自动分段迁移。

#### 只能保证一个共享变量的原子操作

当对一个共享变量执行操作时，我们可以通过循环CAS的方式来保证原子操作

但是对于多个共享变量操作时，循环CAS就无法保证操作的原子性，这个时候只能用锁来保证原子性

解决方案：可以用AtomicReference，这个是封装自定义对象的，多个变量可以放一个自定义对象里，然后他会检查这个对象的引用是不是同一个。如果多个线程同时对一个对象变量的引用进行赋值，用AtomicReference的CAS操作可以解决并发冲突问题。

#### ABA问题

假设两个线程T1和T2访问同一个变量V，当T1访问变量V时，读取到V的值为A；此时线程T1被抢占了，T2开始执行，T2先将变量V的值从A变成B，然后又将变量V的值从B变成A；此时T1又抢占了主动权，继续执行，它发现V的值还是A，以为没有变化，所以就继续执行了。这个过程中，变量V从A变为B，再由B变为A就形象的称为ABA问题。

解决方案：可以引入版本号改变这个问题，每次改变版本号都+1

从Java 1.5开始，JDK的Atomic包里提供了一个类AtomicStampedReference来解决ABA问题。这个类的compareAndSet方法的作用是首先检查当前引用是否等于预期引用，并且检查当前标志是否等于预期标志，如果全部相等，则以原子方式将该引用和该标志的值设置为给定的更新值。

### 无锁并发

CAS 可以保证对共享变量操作的原子性，而volatile可以实现可见性和有序性，结合CAS和volatile可以实现无锁并发，适用于竞争不激烈，多核CPU的场景下。

>  CAS之所以效率高是因为在其内部没有使用synchronized关键字，CAS不会让线程进入阻塞状态，那么也就避免了synchronized当中用户态和内核态的切换所带来的的性能消耗问题，也避免了线程挂起等问题。如果竞争非常激烈，那么CAS就会出现线程大量重试，因为多线程来进行竞争，那么也就导致有可能很多的线程设置取值失败，那么又要进行while循环重试，即大量的线程进行重试操作，成功存的线程反而不多，那么这样的话反而会使性能大大降低。所以如果竞争太激烈还使用的是CAS机制，会导致其性能比synchronized还要低。

### 小结

CAS可以将比较和交换转换为原子操作，这个原子操作直接由处理器保证（由CPU支持），会拿旧的预估值与内存当中的最新值进行比较；如果相同就进行交换并且把最新的值赋值到内存当中的这个变量；

CAS 必须借助 volatile 才能读取到共享变量的最新值来实现【比较并交换】的效果。

使用CAS时，线程数不要超过CPU的核心数，每个CPU核心都能同时并行某个线程，超过的话想运行也运行不了，得发生上下文切换。线程的上下文切换的成本很高，要保存线程的信息，当从阻塞恢复成可运行，还要恢复线程的信息。

## Unsafe类

Unsafe是位于sun.misc包下的一个类，主要提供一些用于执行低级别、不安全操作的方法，如直接访问系统内存资源、自主管理内存资源等，这些方法在提升Java运行效率、增强Java语言底层资源操作能力方面起到了很大的作用。但由于Unsafe类使Java语言拥有了类似C语言指针一样操作内存空间的能力，这无疑也增加了程序发生相关指针问题的风险。在程序中过度、不正确使用Unsafe类会使得程序出错的概率变大，使得Java这种安全的语言变得不再“安全”，因此对Unsafe的使用一定要慎重。

这个类尽管里面的方法都是 public 的，但是并没有办法使用它们，JDK API 文档也没有提供任何关于这个类的方法的解释。总而言之，对于 Unsafe 类的使用都是受限制的，只有授信的代码才能获得该类的实例，当然 JDK 库里面的类是可以随意使用的。

先来看下这张图，对UnSafe类总体功能：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251040445.jpg)

如上图所示，Unsafe提供的API大致可分为内存操作、CAS、Class相关、对象操作、线程调度、系统信息获取、内存屏障、数组操作等几类，下面将对其相关方法和应用场景进行详细介绍。

### Unsafe 功能

#### 内存操作

##### 介绍

如果你是一个写过 C 或者 C++ 的程序员，一定对内存操作不会陌生，而在 Java 中是不允许直接对内存进行操作的，对象内存的分配和回收都是由 JVM 自己实现的。但是在 `Unsafe` 中，提供的下列接口可以直接进行内存操作：

```java
//分配新的本地空间
public native long allocateMemory(long bytes);
//重新调整内存空间的大小
public native long reallocateMemory(long address, long bytes);
//将内存设置为指定值
public native void setMemory(Object o, long offset, long bytes, byte value);
//内存拷贝
public native void copyMemory(Object srcBase, long srcOffset,Object destBase, long destOffset,long bytes);
//清除内存
public native void freeMemory(long address);
```

使用下面的代码进行测试：

```java
private void memoryTest() {
    int size = 4;
    long addr = unsafe.allocateMemory(size);
    long addr3 = unsafe.reallocateMemory(addr, size * 2);
    System.out.println("addr: "+addr);
    System.out.println("addr3: "+addr3);
    try {
        unsafe.setMemory(null,addr ,size,(byte)1);
        for (int i = 0; i < 2; i++) {
            unsafe.copyMemory(null,addr,null,addr3+size*i,4);
        }
        System.out.println(unsafe.getInt(addr));
        System.out.println(unsafe.getLong(addr3));
    }finally {
        unsafe.freeMemory(addr);
        unsafe.freeMemory(addr3);
    }
}
```

先看结果输出：

```plain
addr: 2433733895744
addr3: 2433733894944
16843009
72340172838076673
```

分析一下运行结果，首先使用`allocateMemory`方法申请 4 字节长度的内存空间，调用`setMemory`方法向每个字节写入内容为`byte`类型的 1，当使用 Unsafe 调用`getInt`方法时，因为一个`int`型变量占 4 个字节，会一次性读取 4 个字节，组成一个`int`的值，对应的十进制结果为 16843009。

你可以通过下图理解这个过程：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406202130396.png)

在代码中调用`reallocateMemory`方法重新分配了一块 8 字节长度的内存空间，通过比较`addr`和`addr3`可以看到和之前申请的内存地址是不同的。在代码中的第二个 for 循环里，调用`copyMemory`方法进行了两次内存的拷贝，每次拷贝内存地址`addr`开始的 4 个字节，分别拷贝到以`addr3`和`addr3+4`开始的内存空间上：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406202130404.png)

拷贝完成后，使用`getLong`方法一次性读取 8 个字节，得到`long`类型的值为 72340172838076673。

需要注意，通过这种方式分配的内存属于 堆外内存 ，是无法进行垃圾回收的，需要我们把这些内存当做一种资源去手动调用`freeMemory`方法进行释放，否则会产生内存泄漏。通用的操作内存方式是在`try`中执行对内存的操作，最终在`finally`块中进行内存的释放。

**为什么要使用堆外内存？**

- 对垃圾回收停顿的改善。由于堆外内存是直接受操作系统管理而不是 JVM，所以当我们使用堆外内存时，即可保持较小的堆内内存规模。从而在 GC 时减少回收停顿对于应用的影响。
- 提升程序 I/O 操作的性能。通常在 I/O 通信过程中，会存在堆内内存到堆外内存的数据拷贝操作，对于需要频繁进行内存间数据拷贝且生命周期较短的暂存数据，都建议存储到堆外内存。

##### 典型应用

`DirectByteBuffer` 是 Java 用于实现堆外内存的一个重要类，通常用在通信过程中做缓冲池，如在 Netty、MINA 等 NIO 框架中应用广泛。`DirectByteBuffer` 对于堆外内存的创建、使用、销毁等逻辑均由 Unsafe 提供的堆外内存 API 来实现。

下图为 `DirectByteBuffer` 构造函数，创建 `DirectByteBuffer` 的时候，通过 `Unsafe.allocateMemory` 分配内存、`Unsafe.setMemory` 进行内存初始化，而后构建 `Cleaner` 对象用于跟踪 `DirectByteBuffer` 对象的垃圾回收，以实现当 `DirectByteBuffer` 被垃圾回收时，分配的堆外内存一起被释放。

```java
DirectByteBuffer(int cap) {                   // package-private

    super(-1, 0, cap, cap);
    boolean pa = VM.isDirectMemoryPageAligned();
    int ps = Bits.pageSize();
    long size = Math.max(1L, (long)cap + (pa ? ps : 0));
    Bits.reserveMemory(size, cap);

    long base = 0;
    try {
        // 分配内存并返回基地址
        base = unsafe.allocateMemory(size);
    } catch (OutOfMemoryError x) {
        Bits.unreserveMemory(size, cap);
        throw x;
    }
    // 内存初始化
    unsafe.setMemory(base, size, (byte) 0);
    if (pa && (base % ps != 0)) {
        // Round up to page boundary
        address = base + ps - (base & (ps - 1));
    } else {
        address = base;
    }
    // 跟踪 DirectByteBuffer 对象的垃圾回收，以实现堆外内存释放
    cleaner = Cleaner.create(this, new Deallocator(base, size, cap));
    att = null;
}
```

#### 内存屏障

##### 介绍

在介绍内存屏障前，需要知道编译器和 CPU 会在保证程序输出结果一致的情况下，会对代码进行重排序，从指令优化角度提升性能。而指令重排序可能会带来一个不好的结果，导致 CPU 的高速缓存和内存中数据的不一致，而内存屏障（`Memory Barrier`）就是通过阻止屏障两边的指令重排序从而避免编译器和硬件的不正确优化情况。

在硬件层面上，内存屏障是 CPU 为了防止代码进行重排序而提供的指令，不同的硬件平台上实现内存屏障的方法可能并不相同。在 Java8 中，引入了 3 个内存屏障的函数，它屏蔽了操作系统底层的差异，允许在代码中定义、并统一由 JVM 来生成内存屏障指令，来实现内存屏障的功能。

`Unsafe` 中提供了下面三个内存屏障相关方法：

```java
//内存屏障，禁止load操作重排序。屏障前的load操作不能被重排序到屏障后，屏障后的load操作不能被重排序到屏障前
public native void loadFence();
//内存屏障，禁止store操作重排序。屏障前的store操作不能被重排序到屏障后，屏障后的store操作不能被重排序到屏障前
public native void storeFence();
//内存屏障，禁止load、store操作重排序
public native void fullFence();
```

内存屏障可以看做对内存随机访问的操作中的一个同步点，使得此点之前的所有读写操作都执行后才可以开始执行此点之后的操作。以`loadFence`方法为例，它会禁止读操作重排序，保证在这个屏障之前的所有读操作都已经完成，并且将缓存数据设为无效，重新从主存中进行加载。

看到这估计很多小伙伴们会想到`volatile`关键字了，如果在字段上添加了`volatile`关键字，就能够实现字段在多线程下的可见性。基于读内存屏障，我们也能实现相同的功能。下面定义一个线程方法，在线程中去修改`flag`标志位，注意这里的`flag`是没有被`volatile`修饰的：

```java
@Getter
class ChangeThread implements Runnable{
    /**volatile**/ boolean flag=false;
    @Override
    public void run() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("subThread change flag to:" + flag);
        flag = true;
    }
}
```

在主线程的`while`循环中，加入内存屏障，测试是否能够感知到`flag`的修改变化：

```java
public static void main(String[] args){
    ChangeThread changeThread = new ChangeThread();
    new Thread(changeThread).start();
    while (true) {
        boolean flag = changeThread.isFlag();
        unsafe.loadFence(); //加入读内存屏障
        if (flag){
            System.out.println("detected flag changed");
            break;
        }
    }
    System.out.println("main thread end");
}
```

运行结果：

```plain
subThread change flag to:false
detected flag changed
main thread end
```

而如果删掉上面代码中的`loadFence`方法，那么主线程将无法感知到`flag`发生的变化，会一直在`while`中循环。可以用图来表示上面的过程：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406202130484.png)

了解 Java 内存模型（`JMM`）的小伙伴们应该清楚，运行中的线程不是直接读取主内存中的变量的，只能操作自己工作内存中的变量，然后同步到主内存中，并且线程的工作内存是不能共享的。上面的图中的流程就是子线程借助于主内存，将修改后的结果同步给了主线程，进而修改主线程中的工作空间，跳出循环。

##### 典型应用

在 Java 8 中引入了一种锁的新机制——`StampedLock`，它可以看成是读写锁的一个改进版本。`StampedLock` 提供了一种乐观读锁的实现，这种乐观读锁类似于无锁的操作，完全不会阻塞写线程获取写锁，从而缓解读多写少时写线程“饥饿”现象。由于 `StampedLock` 提供的乐观读锁不阻塞写线程获取读锁，当线程共享变量从主内存 load 到线程工作内存时，会存在数据不一致问题。

为了解决这个问题，`StampedLock` 的 `validate` 方法会通过 `Unsafe` 的 `loadFence` 方法加入一个 `load` 内存屏障。

```java
public boolean validate(long stamp) {
   U.loadFence();
   return (stamp & SBITS) == (state & SBITS);
}
```

#### 对象操作

##### 介绍

**例子**

```java
import sun.misc.Unsafe;
import java.lang.reflect.Field;

public class Main {

    private int value;

    public static void main(String[] args) throws Exception{
        Unsafe unsafe = reflectGetUnsafe();
        assert unsafe != null;
        long offset = unsafe.objectFieldOffset(Main.class.getDeclaredField("value"));
        Main main = new Main();
        System.out.println("value before putInt: " + main.value);
        unsafe.putInt(main, offset, 42);
        System.out.println("value after putInt: " + main.value);
  System.out.println("value after putInt: " + unsafe.getInt(main, offset));
    }

    private static Unsafe reflectGetUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
```

输出结果：

```plain
value before putInt: 0
value after putInt: 42
value after putInt: 42
```

**对象属性**

对象成员属性的内存偏移量获取，以及字段属性值的修改，在上面的例子中我们已经测试过了。除了前面的`putInt`、`getInt`方法外，Unsafe 提供了全部 8 种基础数据类型以及`Object`的`put`和`get`方法，并且所有的`put`方法都可以越过访问权限，直接修改内存中的数据。阅读 openJDK 源码中的注释发现，基础数据类型和`Object`的读写稍有不同，基础数据类型是直接操作的属性值（`value`），而`Object`的操作则是基于引用值（`reference value`）。下面是`Object`的读写方法：

```java
//在对象的指定偏移地址获取一个对象引用
public native Object getObject(Object o, long offset);
//在对象指定偏移地址写入一个对象引用
public native void putObject(Object o, long offset, Object x);
```

除了对象属性的普通读写外，`Unsafe` 还提供了 **volatile 读写**和**有序写入**方法。`volatile`读写方法的覆盖范围与普通读写相同，包含了全部基础数据类型和`Object`类型，以`int`类型为例：

```java
//在对象的指定偏移地址处读取一个int值，支持volatile load语义
public native int getIntVolatile(Object o, long offset);
//在对象指定偏移地址处写入一个int，支持volatile store语义
public native void putIntVolatile(Object o, long offset, int x);
```

相对于普通读写来说，`volatile`读写具有更高的成本，因为它需要保证可见性和有序性。在执行`get`操作时，会强制从主存中获取属性值，在使用`put`方法设置属性值时，会强制将值更新到主存中，从而保证这些变更对其他线程是可见的。

有序写入的方法有以下三个：

```java
public native void putOrderedObject(Object o, long offset, Object x);
public native void putOrderedInt(Object o, long offset, int x);
public native void putOrderedLong(Object o, long offset, long x);
```

有序写入的成本相对`volatile`较低，因为它只保证写入时的有序性，而不保证可见性，也就是一个线程写入的值不能保证其他线程立即可见。为了解决这里的差异性，需要对内存屏障的知识点再进一步进行补充，首先需要了解两个指令的概念：

- `Load`：将主内存中的数据拷贝到处理器的缓存中
- `Store`：将处理器缓存的数据刷新到主内存中

顺序写入与`volatile`写入的差别在于，在顺序写时加入的内存屏障类型为`StoreStore`类型，而在`volatile`写入时加入的内存屏障是`StoreLoad`类型，如下图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406202130467.png)

在有序写入方法中，使用的是`StoreStore`屏障，该屏障确保`Store1`立刻刷新数据到内存，这一操作先于`Store2`以及后续的存储指令操作。而在`volatile`写入中，使用的是`StoreLoad`屏障，该屏障确保`Store1`立刻刷新数据到内存，这一操作先于`Load2`及后续的装载指令，并且，`StoreLoad`屏障会使该屏障之前的所有内存访问指令，包括存储指令和访问指令全部完成之后，才执行该屏障之后的内存访问指令。

综上所述，在上面的三类写入方法中，在写入效率方面，按照`put`、`putOrder`、`putVolatile`的顺序效率逐渐降低。

**对象实例化**

使用 `Unsafe` 的 `allocateInstance` 方法，允许我们使用非常规的方式进行对象的实例化，首先定义一个实体类，并且在构造函数中对其成员变量进行赋值操作：

```java
@Data
public class A {
    private int b;
    public A(){
        this.b =1;
    }
}
```

分别基于构造函数、反射以及 `Unsafe` 方法的不同方式创建对象进行比较：

```java
public void objTest() throws Exception{
    A a1=new A();
    System.out.println(a1.getB());
    A a2 = A.class.newInstance();
    System.out.println(a2.getB());
    A a3= (A) unsafe.allocateInstance(A.class);
    System.out.println(a3.getB());
}
```

打印结果分别为 1、1、0，说明通过`allocateInstance`方法创建对象过程中，不会调用类的构造方法。使用这种方式创建对象时，只用到了`Class`对象，所以说如果想要跳过对象的初始化阶段或者跳过构造器的安全检查，就可以使用这种方法。在上面的例子中，如果将 A 类的构造函数改为`private`类型，将无法通过构造函数和反射创建对象（可以通过构造函数对象 setAccessible 后创建对象），但`allocateInstance`方法仍然有效。

##### 典型应用

- **常规对象实例化方式**：我们通常所用到的创建对象的方式，从本质上来讲，都是通过 new 机制来实现对象的创建。但是，new 机制有个特点就是当类只提供有参的构造函数且无显示声明无参构造函数时，则必须使用有参构造函数进行对象构造，而使用有参构造函数时，必须传递相应个数的参数才能完成对象实例化。
- **非常规的实例化方式**：而 Unsafe 中提供 allocateInstance 方法，仅通过 Class 对象就可以创建此类的实例对象，而且不需要调用其构造函数、初始化代码、JVM 安全检查等。它抑制修饰符检测，也就是即使构造器是 private 修饰的也能通过此方法实例化，只需提类对象即可创建相应的对象。由于这种特性，allocateInstance 在 java.lang.invoke、Objenesis（提供绕过类构造器的对象生成方式）、Gson（反序列化时用到）中都有相应的应用。

#### 数组操作

##### 介绍

`arrayBaseOffset` 与 `arrayIndexScale` 这两个方法配合起来使用，即可定位数组中每个元素在内存中的位置。

```java
//返回数组中第一个元素的偏移地址
public native int arrayBaseOffset(Class<?> arrayClass);
//返回数组中一个元素占用的大小
public native int arrayIndexScale(Class<?> arrayClass);
```

##### 典型应用

这两个与数据操作相关的方法，在 `java.util.concurrent.atomic` 包下的 `AtomicIntegerArray`（可以实现对 `Integer` 数组中每个元素的原子性操作）中有典型的应用，如下图 `AtomicIntegerArray` 源码所示，通过 `Unsafe` 的 `arrayBaseOffset`、`arrayIndexScale` 分别获取数组首元素的偏移地址 `base` 及单个元素大小因子 `scale` 。后续相关原子性操作，均依赖于这两个值进行数组中元素的定位，如下图二所示的 `getAndAdd` 方法即通过 `checkedByteOffset` 方法获取某数组元素的偏移地址，而后通过 CAS 实现原子性操作。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406202130522.png)

#### CAS 操作

##### 介绍

这部分主要为 CAS 相关操作的方法。

```java
/**
  *  CAS
  * @param o         包含要修改field的对象
  * @param offset    对象中某field的偏移量
  * @param expected  期望值
  * @param update    更新值
  * @return          true | false
  */
public final native boolean compareAndSwapObject(Object o, long offset,  Object expected, Object update);

public final native boolean compareAndSwapInt(Object o, long offset, int expected,int update);

public final native boolean compareAndSwapLong(Object o, long offset, long expected, long update);
```

**什么是 CAS?** CAS 即比较并替换（Compare And Swap)，是实现并发算法时常用到的一种技术。CAS 操作包含三个操作数——内存位置、预期原值及新值。执行 CAS 操作的时候，将内存位置的值与预期原值比较，如果相匹配，那么处理器会自动将该位置值更新为新值，否则，处理器不做任何操作。我们都知道，CAS 是一条 CPU 的原子指令（cmpxchg 指令），不会造成所谓的数据不一致问题，`Unsafe` 提供的 CAS 方法（如 `compareAndSwapXXX`）底层实现即为 CPU 指令 `cmpxchg` 。

##### 典型应用

在 JUC 包的并发工具类中大量地使用了 CAS 操作，像在前面介绍`synchronized`和`AQS`的文章中也多次提到了 CAS，其作为乐观锁在并发工具类中广泛发挥了作用。在 `Unsafe` 类中，提供了`compareAndSwapObject`、`compareAndSwapInt`、`compareAndSwapLong`方法来实现的对`Object`、`int`、`long`类型的 CAS 操作。以`compareAndSwapInt`方法为例：

```java
public final native boolean compareAndSwapInt(Object o, long offset,int expected,int x);
```

参数中`o`为需要更新的对象，`offset`是对象`o`中整形字段的偏移量，如果这个字段的值与`expected`相同，则将字段的值设为`x`这个新值，并且此更新是不可被中断的，也就是一个原子操作。下面是一个使用`compareAndSwapInt`的例子：

```java
private volatile int a;
public static void main(String[] args){
    CasTest casTest=new CasTest();
    new Thread(()->{
        for (int i = 1; i < 5; i++) {
            casTest.increment(i);
            System.out.print(casTest.a+" ");
        }
    }).start();
    new Thread(()->{
        for (int i = 5 ; i <10 ; i++) {
            casTest.increment(i);
            System.out.print(casTest.a+" ");
        }
    }).start();
}

private void increment(int x){
    while (true){
        try {
            long fieldOffset = unsafe.objectFieldOffset(CasTest.class.getDeclaredField("a"));
            if (unsafe.compareAndSwapInt(this,fieldOffset,x-1,x))
                break;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
```

运行代码会依次输出：

```plain
1 2 3 4 5 6 7 8 9
```

在上面的例子中，使用两个线程去修改`int`型属性`a`的值，并且只有在`a`的值等于传入的参数`x`减一时，才会将`a`的值变为`x`，也就是实现对`a`的加一的操作。流程如下所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406202130526.png)

需要注意的是，在调用`compareAndSwapInt`方法后，会直接返回`true`或`false`的修改结果，因此需要我们在代码中手动添加自旋的逻辑。在`AtomicInteger`类的设计中，也是采用了将`compareAndSwapInt`的结果作为循环条件，直至修改成功才退出死循环的方式来实现的原子性的自增操作。

#### 线程调度

##### 介绍

`Unsafe` 类中提供了`park`、`unpark`、`monitorEnter`、`monitorExit`、`tryMonitorEnter`方法进行线程调度。

```java
//取消阻塞线程
public native void unpark(Object thread);
//阻塞线程
public native void park(boolean isAbsolute, long time);
//获得对象锁（可重入锁）
@Deprecated
public native void monitorEnter(Object o);
//释放对象锁
@Deprecated
public native void monitorExit(Object o);
//尝试获取对象锁
@Deprecated
public native boolean tryMonitorEnter(Object o);
```

方法 `park`、`unpark` 即可实现线程的挂起与恢复，将一个线程进行挂起是通过 `park` 方法实现的，调用 `park` 方法后，线程将一直阻塞直到超时或者中断等条件出现；`unpark` 可以终止一个挂起的线程，使其恢复正常。

此外，`Unsafe` 源码中`monitor`相关的三个方法已经被标记为`deprecated`，不建议被使用：

```java
//获得对象锁
@Deprecated
public native void monitorEnter(Object var1);
//释放对象锁
@Deprecated
public native void monitorExit(Object var1);
//尝试获得对象锁
@Deprecated
public native boolean tryMonitorEnter(Object var1);
```

`monitorEnter`方法用于获得对象锁，`monitorExit`用于释放对象锁，如果对一个没有被`monitorEnter`加锁的对象执行此方法，会抛出`IllegalMonitorStateException`异常。`tryMonitorEnter`方法尝试获取对象锁，如果成功则返回`true`，反之返回`false`。

##### 典型应用

Java 锁和同步器框架的核心类 `AbstractQueuedSynchronizer` (AQS)，就是通过调用`LockSupport.park()`和`LockSupport.unpark()`实现线程的阻塞和唤醒的，而 `LockSupport` 的 `park`、`unpark` 方法实际是调用 `Unsafe` 的 `park`、`unpark` 方式实现的。

```java
public static void park(Object blocker) {
    Thread t = Thread.currentThread();
    setBlocker(t, blocker);
    UNSAFE.park(false, 0L);
    setBlocker(t, null);
}
public static void unpark(Thread thread) {
    if (thread != null)
        UNSAFE.unpark(thread);
}
```

`LockSupport` 的`park`方法调用了 `Unsafe` 的`park`方法来阻塞当前线程，此方法将线程阻塞后就不会继续往后执行，直到有其他线程调用`unpark`方法唤醒当前线程。下面的例子对 `Unsafe` 的这两个方法进行测试：

```java
public static void main(String[] args) {
    Thread mainThread = Thread.currentThread();
    new Thread(()->{
        try {
            TimeUnit.SECONDS.sleep(5);
            System.out.println("subThread try to unpark mainThread");
            unsafe.unpark(mainThread);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }).start();

    System.out.println("park main mainThread");
    unsafe.park(false,0L);
    System.out.println("unpark mainThread success");
}
```

程序输出为：

```plain
park main mainThread
subThread try to unpark mainThread
unpark mainThread success
```

程序运行的流程也比较容易看懂，子线程开始运行后先进行睡眠，确保主线程能够调用`park`方法阻塞自己，子线程在睡眠 5 秒后，调用`unpark`方法唤醒主线程，使主线程能继续向下执行。整个流程如下图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406202130484.png)

#### Class 操作

##### 介绍

`Unsafe` 对`Class`的相关操作主要包括类加载和静态变量的操作方法。

**静态属性读取相关的方法**

```java
//获取静态属性的偏移量
public native long staticFieldOffset(Field f);
//获取静态属性的对象指针
public native Object staticFieldBase(Field f);
//判断类是否需要初始化（用于获取类的静态属性前进行检测）
public native boolean shouldBeInitialized(Class<?> c);
```

创建一个包含静态属性的类，进行测试：

```java
@Data
public class User {
    public static String name="Hydra";
    int age;
}
private void staticTest() throws Exception {
    User user=new User();
    // 也可以用下面的语句触发类初始化
    // 1.
    // unsafe.ensureClassInitialized(User.class);
    // 2.
    // System.out.println(User.name);
    System.out.println(unsafe.shouldBeInitialized(User.class));
    Field sexField = User.class.getDeclaredField("name");
    long fieldOffset = unsafe.staticFieldOffset(sexField);
    Object fieldBase = unsafe.staticFieldBase(sexField);
    Object object = unsafe.getObject(fieldBase, fieldOffset);
    System.out.println(object);
}
```

运行结果：

```plain
false
Hydra
```

在 `Unsafe` 的对象操作中，我们学习了通过`objectFieldOffset`方法获取对象属性偏移量并基于它对变量的值进行存取，但是它不适用于类中的静态属性，这时候就需要使用`staticFieldOffset`方法。在上面的代码中，只有在获取`Field`对象的过程中依赖到了`Class`，而获取静态变量的属性时不再依赖于`Class`。

在上面的代码中首先创建一个`User`对象，这是因为如果一个类没有被初始化，那么它的静态属性也不会被初始化，最后获取的字段属性将是`null`。所以在获取静态属性前，需要调用`shouldBeInitialized`方法，判断在获取前是否需要初始化这个类。如果删除创建 User 对象的语句，运行结果会变为：

```plain
true
null
```

**使用`defineClass`方法允许程序在运行时动态地创建一个类**

```java
public native Class<?> defineClass(String name, byte[] b, int off, int len, ClassLoader loader,ProtectionDomain protectionDomain);
```

在实际使用过程中，可以只传入字节数组、起始字节的下标以及读取的字节长度，默认情况下，类加载器（`ClassLoader`）和保护域（`ProtectionDomain`）来源于调用此方法的实例。下面的例子中实现了反编译生成后的 class 文件的功能：

```java
private static void defineTest() {
    String fileName="F:\\workspace\\unsafe-test\\target\\classes\\com\\cn\\model\\User.class";
    File file = new File(fileName);
    try(FileInputStream fis = new FileInputStream(file)) {
        byte[] content=new byte[(int)file.length()];
        fis.read(content);
        Class clazz = unsafe.defineClass(null, content, 0, content.length, null, null);
        Object o = clazz.newInstance();
        Object age = clazz.getMethod("getAge").invoke(o, null);
        System.out.println(age);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

在上面的代码中，首先读取了一个`class`文件并通过文件流将它转化为字节数组，之后使用`defineClass`方法动态的创建了一个类，并在后续完成了它的实例化工作，流程如下图所示，并且通过这种方式创建的类，会跳过 JVM 的所有安全检查。

![](https://oss.javaguide.cn/github/javaguide/java/basis/unsafe/image-20220717145000710.png)

除了`defineClass`方法外，Unsafe 还提供了一个`defineAnonymousClass`方法：

```java
public native Class<?> defineAnonymousClass(Class<?> hostClass, byte[] data, Object[] cpPatches);
```

使用该方法可以用来动态的创建一个匿名类，在`Lambda`表达式中就是使用 ASM 动态生成字节码，然后利用该方法定义实现相应的函数式接口的匿名类。在 JDK 15 发布的新特性中，在隐藏类（`Hidden classes`）一条中，指出将在未来的版本中弃用 `Unsafe` 的`defineAnonymousClass`方法。

##### 典型应用

Lambda 表达式实现需要依赖 `Unsafe` 的 `defineAnonymousClass` 方法定义实现相应的函数式接口的匿名类。

#### 系统信息

##### 介绍

这部分包含两个获取系统相关信息的方法。

```java
//返回系统指针的大小。返回值为4（32位系统）或 8（64位系统）。
public native int addressSize();
//内存页的大小，此值为2的幂次方。
public native int pageSize();
```

##### 典型应用

这两个方法的应用场景比较少，在`java.nio.Bits`类中，在使用`pageCount`计算所需的内存页的数量时，调用了`pageSize`方法获取内存页的大小。另外，在使用`copySwapMemory`方法拷贝内存时，调用了`addressSize`方法，检测 32 位系统的情况。



### Unsafe底层

再看看Unsafe的compareAndSwap*方法来实现CAS操作，它是一个本地方法，实现位于unsafe.cpp中。

```java
UNSAFE_ENTRY(jboolean, Unsafe_CompareAndSwapInt(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jint e, jint x))
  UnsafeWrapper("Unsafe_CompareAndSwapInt");
  oop p = JNIHandles::resolve(obj);
  jint* addr = (jint *) index_oop_from_field_offset_long(p, offset);
  return (jint)(Atomic::cmpxchg(x, addr, e)) == e;
UNSAFE_END
```

可以看到它通过 Atomic::cmpxchg 来实现比较和替换操作。其中参数x是即将更新的值，参数e是原内存的值。



如果是Linux的x86，Atomic::cmpxchg方法的实现如下：

```java
inline jint Atomic::cmpxchg (jint exchange_value, volatile jint* dest, jint compare_value) {
  int mp = os::is_MP();
  __asm__ volatile (LOCK_IF_MP(%4) "cmpxchgl %1,(%3)"
                    : "=a" (exchange_value)
                    : "r" (exchange_value), "a" (compare_value), "r" (dest), "r" (mp)
                    : "cc", "memory");
  return exchange_value;
}
```



而windows的x86的实现如下：

```java
inline jint Atomic::cmpxchg (jint exchange_value, volatile jint* dest, jint compare_value) {
    int mp = os::isMP(); //判断是否是多处理器
    _asm {
        mov edx, dest
        mov ecx, exchange_value
        mov eax, compare_value
        LOCK_IF_MP(mp)
        cmpxchg dword ptr [edx], ecx
    }
}

// Adding a lock prefix to an instruction on MP machine
// VC++ doesn't like the lock prefix to be on a single line
// so we can't insert a label after the lock prefix.
// By emitting a lock prefix, we can define a label after it.
#define LOCK_IF_MP(mp) __asm cmp mp, 0  \
                       __asm je L0      \
                       __asm _emit 0xF0 \
                       __asm L0:
```



如果是多处理器，为cmpxchg指令添加lock前缀。反之，就省略lock前缀(单处理器会不需要lock前缀提供的内存屏障效果)。这里的lock前缀就是使用了处理器的总线锁(最新的处理器都使用缓存锁代替总线锁来提高性能)。

>  cmpxchg(void* ptr, int old, int new)，如果ptr和old的值一样，则把new写到ptr内存，否则返回ptr的值，整个操作是原子的。在Intel平台下，会用lock cmpxchg来实现，使用lock触发缓存锁，这样另一个线程想访问ptr的内存，就会被block住。



 

## 原子类Atomic

### AtomicInteger

#### 问题

```java
public class Demo01 {
    // 定义一个共享变量 num
    private static int num = 0;

    public static void main(String[] args) throws InterruptedException {
        // 任务：对 num 进行10000次加操作
        Runnable mr = () -> {
            for (int i = 0; i < 10000; i++) {
                num++;    // num++并不是原子操作，就会导致原子性问题的产生
            }
        };

        ArrayList<Thread> ts = new ArrayList<>();
        // 同时开辟5个线程执行任务
        for (int i = 0; i < 5; i++) {
            Thread t = new Thread(mr);
            t.start();
            ts.add(t);
        }

        for (Thread t : ts) {
            t.join();
        }
        //因此最终会输出的num < 50000
        System.out.println("num = " + num);
    }
}
```



改为原子类

```java
public class Demo01 {

    public static void main(String[] args) throws InterruptedException {
        // 
        AtomicInteger atomicInteger = new AtomicInteger();
        // 任务：自增 10000 次
        Runnable mr = () -> {
            for (int i = 0; i < 10000; i++) {
                atomicInteger.incrementAndGet();    //该自增操作是一个原子性的操作
            }
        };

        ArrayList<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Thread t = new Thread(mr);
            t.start();
            ts.add(t);
        }

        for (Thread t : ts) {
            t.join();
        }
        //由于是原子操作，值将一直会是50000
        System.out.println("number = " + atomicInteger.get());
    }
}
```



#### 底层源码

AtomicInteger类当中其内部会包含一个叫做UnSafe的类，该类可以保证变量在赋值时的原子操作；

```java
/* AtomicInteger.java */
private volatile int value;    // value初始取值为0

public final int incrementAndGet() {
    // this：自己 new 好的 atomicInteger对象
    // valueOffset：内存偏移量
    // 1 ：这个方法时自增操作，因此是1
    return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
}
```

- 变量valueOffset：表示该变量值在内存中的偏移地址，因为Unsafe就是根据内存偏移地址获取数据的

- 变量value用volatile修饰：保证了多线程之间的内存可见性

```java
/* Unsafe.class */

// var1：上面的this，即atomicInteger对象；  var2：valueOffset ； var4：要添加的值
public final int getAndAddInt(Object var1, long var2, int var4) {
    // var5 旧的预估值
    int var5;
    do {
        // this 和 内存 valueOffset，目的是找出这个 value的当前最新值（旧的预估值）
        var5 = this.getIntVolatile(var1, var2);
    } while (!this.compareAndSwapInt(var1, var2, var5, var5 + var4));

    return var5;
}
```



变量解释：

- var5：就是从主内存中拷贝到工作内存中的值

- val1：AtomicInteger对象本身

- var2：该对象值的valueOffset

- var4：需要变动的数量

- var5：用var1和var2找到的内存中的真实值

compareAndSwapInt(var1, var2, var5, var5 + var4) 表示用该对象当前的值与var5比较

- 如果相同，更新成var5 + var4 并返回true

- 如果不同，继续取值然后再比较，直到更新完成

需要比较工作内存中的值，和主内存中的值进行比较

假设执行 compareAndSwapInt返回false，那么就一直执行 while方法，直到期望的值和真实值一样

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251040434.gif)

假设线程A和线程B同时执行getAndInt操作（分别跑在不同的CPU上）

1. AtomicInteger里面的value原始值为3，即主内存中AtomicInteger的 value 为3，根据JMM模型，线程A和线程B各自持有一份价值为3的副本，分别存储在各自的工作内存
2. 线程A通过getIntVolatile(var1 , var2) 拿到value值3，这时线程A被挂起（该线程失去CPU执行权）
3. 线程B也通过getIntVolatile(var1, var2)方法获取到value值也是3，此时刚好线程B没有被挂起，并执行了compareAndSwapInt方法，比较内存的值也是3，成功修改内存值为4，线程B打完收工，一切OK
4. 这时线程A恢复，执行CAS方法，比较发现自己手里的数字3和主内存中的数字4不一致，说明该值已经被其它线程抢先一步修改过了，那么A线程本次修改失败，只能够重新读取后在来一遍了，也就是继续执行do while
5. 线程A重新获取value值，因为变量value被volatile修饰，所以其它线程对它的修改，线程A总能够看到，线程A继续执行compareAndSwapInt进行比较替换，直到成功。

但是AtomicInteger会存在CAS循环开销大的问题，因此JDK8引入LongAdder来解决这个问题

### LongAdder

LongAdder主要使用分段CAS以及自动分段迁移的方式来大幅度提升多线程高并发执行CAS操作的性能

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251040438.gif)

实现过程：

1. 在LongAdder的底层实现中，首先有一个base值，刚开始多线程来不停的累加数值，都是对base进行累加的，比如刚开始累加成了base = 5。
2. 接着如果发现并发更新的线程数量过多，就会开始施行分段CAS的机制，也就是内部会搞一个Cell数组，每个数组是一个数值分段。
3. 这时，让大量的线程分别去对不同Cell内部的value值进行CAS累加操作，这样就把CAS计算压力分散到了不同的Cell分段数值中了！
4. 这样就可以大幅度的降低多线程并发更新同一个数值时出现的无限循环的问题，大幅度提升了多线程并发更新数值的性能和效率！
5. 内部实现了自动分段迁移的机制，也就是如果某个Cell的value执行CAS失败了，那么就会自动去找另外一个Cell分段内的value值进行CAS操作。这样也解决了线程空旋转、自旋不停等待执行CAS操作的问题，让一个线程过来执行CAS时可以尽快的完成这个操作。

最后，如果要从LongAdder中获取当前累加的总值，就会把base值和所有Cell分段数值加起来返回。

总的来说LongAdder减少了乐观锁的重试次数

#### add源码

```java
public void add(long x) {
        //as 表示cells引用
        //b 表示获取的base值
        // v 表示期望值
        // m表示cells的数组长度 - 1 （cells长度一定是2的幂）
        // a表示当前现成命中的cell单元格
        Cell[] as; long b, v; int m; Cell a;
        
        //条件一： true->表示cells已经初始化过了，当前线程应该将数据写入到对应的cell中
        //            false->表示cells未初始化，当前所有线程应该将数据写入到base中
        //条件二： 要执行到条件二，说明条件一是false
        //        true-> 表示发生竞争了，可能需要重试或者扩容
        //        false-> 表示当前现成CAS替换数据成功，
        if ((as = cells) != null || !casBase(b = base, b + x)) {
            //什么时候进入？
            //1.条件一 true->表示cells已经初始化过了，当前线程应该将数据写入到对应的cell中
            //2.条件二 true-> 表示发生竞争了，可能需要重试或者扩容
            
            // true 表示未发生竞争，false 发生竞争
            boolean uncontended = true;
            
            //条件一： true -> cells未初始化，说明此时是通过 2，多线程写base发生竞争进来的
            //          false -> cells初始化了，说明是 那么当前线程应该找自己的cell写值
            if (as == null || (m = as.length - 1) < 0 ||
            //条件一为false，就走条件二
            //条件二：getProbe()：获取当前线程的hash值  getProbe()&m 会 <= m 。因此as[getProbe() & m])就表示当前线程想把数据扔进去的单元格
            //        true->说明当前线程对应下标的cell为空，需要longAccumulate 创建
            //        false->说明当前线程对应下标的cell不为空，下一步想要将x值添加到cell中
                (a = as[getProbe() & m]) == null ||
                
                //如果条件二位false，就走条件三
                //条件三：将x值添加到cell的过程
                //         true->表示cas添加失败，意味着当前线程对应的cell有竞争
                //        false->表示cas成功，
                !(uncontended = a.cas(v = a.value, v + x)))
                
                //什么时候会调用这个方法？
                //1. 条件一： true -> cells未初始化，说明此时是通过 2，多线程写base发生竞争进来的。说明后续需要  重试 或者 初始化cells
                //2. 条件二：true->说明当前线程对应下标的cell为空，需要longAccumulate 创建
                //3. 条件三：true->表示cas添加失败，意味着当前线程对应的cell有竞争。后续需要  重试 或者 扩容
                longAccumulate(x, null, uncontended);
        }
    }
```



longAccumulate方法

```java
//什么时候会调用这个方法？
//1. 条件一： true -> cells未初始化，说明此时是通过 2，多线程写base发生竞争进来的。说明后续需要  重试 或者 初始化cells
//2. 条件二：true->说明当前线程对应下标的cell为空，需要longAccumulate 创建
//3. 条件三：true->表示cas添加失败，意味着当前线程对应的cell有竞争。后续需要  重试 或者 扩容

//wasUncontended：只有cells初始化之后，并且当前线程竞争修改失败，才会是false 
final void longAccumulate(long x, LongBinaryOperator fn,
                              boolean wasUncontended) {
        //当前线程的hash值
        int h;
        //条件 true->表示当前线程还未分配hash值
        if ((h = getProbe()) == 0) {
            //因此，执行分配hash值的逻辑
            ThreadLocalRandom.current(); // force initialization
            h = getProbe();
            //为什么？因为在这之前当前线程没有hash值，也就是0，那么当前线程肯定是写入到cells[0]的位置
            //如果没有分配hash值的都写到cells[0]，那就出现了cells[0]的竞争。那么就不应该把这次竞争当成真正的竞争，因此修改为true
            wasUncontended = true;
        }
        //表示 扩容意向，false：一定不会扩容；true：可能会扩容
        boolean collide = false;                // True if last slot nonempty
        
        //自旋
        for (;;) {
            // as 表示cells引用
            // a 表示当前线程命中的cell
            // n 表示cells数组长度
            // v 表示期望值
            Cell[] as; Cell a; int n; long v;
            
            //case1：条件1：true -> cells已经初始化，当前线程应该写入数据到对应cell中
            //       条件2：true -> 数组长度大于0，与上面一样
            if ((as = cells) != null && (n = as.length) > 0) {
                //什么时候来到case1 
                //2. 条件二：true->说明当前线程对应下标的cell为空，需要longAccumulate 创建
                //3. 条件三：true->表示cas添加失败，意味着当前线程对应的cell有竞争。后续需要  重试 或者 扩容
                //case 1.1：true -> 当前线程对应下标的cell为空，需要创建cell
                if ((a = as[(n - 1) & h]) == null) {
                    //true->当前是无锁未被占用，false->锁被占用
                    if (cellsBusy == 0) {       // Try to attach new Cell
                        //创建cell
                        Cell r = new Cell(x);   // Optimistically create
                        //条件一：cellsBusy == 0
                        //            true->当前无锁，当前线程可以竞争这把锁
                        //条件二：casCellsBusy()，竞争锁
                        //            true->当前线程获取锁
                        if (cellsBusy == 0 && casCellsBusy()) {
                            //定义是否创建成功的标记
                            boolean created = false;
                            try {               // Recheck under lock
                                //rs表示当前cells引用
                                //m cells长度
                                //j 当前线程命中下标
                                Cell[] rs; int m, j;
                                
                                //条件一条件二恒成立
                                //条件三：rs[j = (m - 1) & h] == null？ 在case1.1时已经判断过这个位置了，为什么这里还要判断？
                                //原因是多线程并发情况下，有线程可能已经在执行下述流程，此时在case1.1判断为null，但到这里已经有线程执行过了，因此需要重新判断
                                if ((rs = cells) != null &&
                                    (m = rs.length) > 0 &&
                                    rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                //释放锁
                                cellsBusy = 0;
                            }
                            if (created)
                                break;
                            continue;           // Slot is now non-empty
                        }
                    }
                    //扩容意向改为false
                    collide = false;
                }
                //case 1.2: wasUncontended：只有cells初始化之后，并且当前线程竞争修改失败，才会是false 
                else if (!wasUncontended)       // CAS already known to fail
                    wasUncontended = true;      // Continue after rehash
                //case1.3：什么时候到这，当前线程重置过hash值。新命中的cell不为空
                //        true->将数据写入cell成功，那就可以退出了
                //        false->表示重置hash后命中的新的cell也有竞争，重试1次，会执行case1.4
                else if (a.cas(v = a.value, ((fn == null) ? v + x :
                                             fn.applyAsLong(v, x))))
                    break;
                //case1.4：n>=NCPU
                //            true->数组长度大于等于CPU数量
                //            false->数组长度还可以扩容
                //        cells != as？
                //            true->其它线程已经扩容过了，当前线程就应该重置hash重试
                else if (n >= NCPU || cells != as)
                    //扩容意向改为false
                    collide = false;            // At max size or stale
                //case1.5：!collide
                //         true->表示设置扩容意向为true，但不一定扩容，因为需要自旋重新尝试
                else if (!collide)
                    collide = true;
                //case 1.6：真正扩容的逻辑
                //    条件一：cellsBusy == 0
                //            true->当前无锁，当前线程可以竞争这把锁
                //条件二：casCellsBusy()，竞争锁
                //            true->当前线程获取锁，当前线程执行可以扩容逻辑
                //false就说明有其他线程在执行扩容
                else if (cellsBusy == 0 && casCellsBusy()) {
                    try {
                        //这里与之前的逻辑一致，需要再次判断
                        if (cells == as) {      // Expand table unless stale
                            Cell[] rs = new Cell[n << 1];//扩容长度翻倍，长度是2的幂
                            for (int i = 0; i < n; ++i)
                                rs[i] = as[i];
                            cells = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;                   // Retry with expanded table
                }
                
                //重置当前线程hash值
                h = advanceProbe(h);
            }
            
            //case2: 显然要到case2，case1就为false,也就是cells还未初始化，as为null
            //         条件一：true -> 当前未加锁
            //        条件二：cells == as？ 原因在多线程并发情况下，有线程可能已经在执行下述流程，此时在case1判断为null，但到case2可能已经扩容完成了，cells可能就不为null了
            //        条件三：true -> 表示获取锁成功，casCellsBusy() = 1。
            //                false -> 表示其它线程正在持有锁
            else if (cellsBusy == 0 && cells == as && casCellsBusy() = 1。
            ) {
                boolean init = false;
                try {                           // Initialize table
                    //为了防止其它线程已经初始化了，当前线程再次初始化，丢失数据
                    if (cells == as) {
                        Cell[] rs = new Cell[2];
                        rs[h & 1] = new Cell(x);
                        cells = rs;
                        init = true;
                    }
                } finally {
                    //释放锁
                    cellsBusy = 0;
                }
                if (init)
                    break;
            }
            //case3：什么时候会到这个条件。
            //1. 当前casCellsBusy()锁已经被持有，说明其他线程正在初始化cells
            //2. cells被其他线程初始化了
            //那么此时就应该去累加数据了
            else if (casBase(v = base, ((fn == null) ? v + x :
                                        fn.applyAsLong(v, x))))
                break;                          // Fall back on using base
        }
    }
```



### AtomicStampedReference

AtomicStampedReference主要维护包含一个对象引用以及一个可以自动更新的整数"stamp"的pair对象来解决ABA问题。

```java
public class AtomicStampedReference<V> {
    private static class Pair<T> {
        final T reference;  //维护对象引用
        final int stamp;  //用于标志版本
        private Pair(T reference, int stamp) {
            this.reference = reference;
            this.stamp = stamp;
        }
        static <T> Pair<T> of(T reference, int stamp) {
            return new Pair<T>(reference, stamp);
        }
    }
    private volatile Pair<V> pair;
    ....
    
    /**
      * expectedReference ：更新之前的原始值
      * newReference : 将要更新的新值
      * expectedStamp : 期待更新的标志版本
      * newStamp : 将要更新的标志版本
      */
    public boolean compareAndSet(V   expectedReference,
                             V   newReference,
                             int expectedStamp,
                             int newStamp) {
        // 获取当前的(元素值，版本号)对
        Pair<V> current = pair;
        return
            // 引用没变
            expectedReference == current.reference &&
            // 版本号没变
            expectedStamp == current.stamp &&
            // 新引用等于旧引用
            ((newReference == current.reference &&
            // 新版本号等于旧版本号
            newStamp == current.stamp) ||
            // 构造新的Pair对象并CAS更新
            casPair(current, Pair.of(newReference, newStamp)));
    }

    private boolean casPair(Pair<V> cmp, Pair<V> val) {
        // 调用Unsafe的compareAndSwapObject()方法CAS更新pair的引用为新引用
        return UNSAFE.compareAndSwapObject(this, pairOffset, cmp, val);
    }
```

- 如果元素值和版本号都没有变化，并且和新的也相同，返回true；

- 如果元素值和版本号都没有变化，并且和新的不完全相同，就构造一个新的Pair对象并执行CAS更新pair。

可以看到，java中的实现跟我们上面讲的ABA的解决方法是一致的。

- 首先，使用版本号控制；
- 其次，不重复使用节点(Pair)的引用，每次都新建一个新的Pair来作为CAS比较的对象，而不是复用旧的；
- 最后，外部传入元素值及版本号，而不是节点(Pair)的引用。



<!-- @include: @article-footer.snippet.md -->     
