---
title: JUC锁 - CAS & Unsafe & 原子类
category: Java
tag:
 - 并发编程
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

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251040445.jpg)

如上图所示，Unsafe提供的API大致可分为内存操作、CAS、Class相关、对象操作、线程调度、系统信息获取、内存屏障、数组操作等几类，下面将对其相关方法和应用场景进行详细介绍。

### Unsafe与CAS

反编译出来的代码：

```java
public final int getAndAddInt(Object paramObject, long paramLong, int paramInt)
  {
    int i;
    do
      i = getIntVolatile(paramObject, paramLong);
    while (!compareAndSwapInt(paramObject, paramLong, i, i + paramInt));
    return i;
  }

  public final long getAndAddLong(Object paramObject, long paramLong1, long paramLong2)
  {
    long l;
    do
      l = getLongVolatile(paramObject, paramLong1);
    while (!compareAndSwapLong(paramObject, paramLong1, l, l + paramLong2));
    return l;
  }

  public final int getAndSetInt(Object paramObject, long paramLong, int paramInt)
  {
    int i;
    do
      i = getIntVolatile(paramObject, paramLong);
    while (!compareAndSwapInt(paramObject, paramLong, i, paramInt));
    return i;
  }

  public final long getAndSetLong(Object paramObject, long paramLong1, long paramLong2)
  {
    long l;
    do
      l = getLongVolatile(paramObject, paramLong1);
    while (!compareAndSwapLong(paramObject, paramLong1, l, paramLong2));
    return l;
  }

  public final Object getAndSetObject(Object paramObject1, long paramLong, Object paramObject2)
  {
    Object localObject;
    do
      localObject = getObjectVolatile(paramObject1, paramLong);
    while (!compareAndSwapObject(paramObject1, paramLong, localObject, paramObject2));
    return localObject;
  }
```



从源码中发现，内部使用自旋的方式进行CAS更新(while循环进行CAS更新，如果更新失败，则循环再次重试)。

又从Unsafe类中发现，原子操作其实只支持下面三个方法。

```java
public final native boolean compareAndSwapObject(Object paramObject1, long paramLong, Object paramObject2, Object paramObject3);

public final native boolean compareAndSwapInt(Object paramObject, long paramLong, int paramInt1, int paramInt2);

public final native boolean compareAndSwapLong(Object paramObject, long paramLong1, long paramLong2, long paramLong3);
```

我们发现Unsafe只提供了3种CAS方法：compareAndSwapObject、compareAndSwapInt和compareAndSwapLong。都是native方法。

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

### Unsafe其它功能

Unsafe 提供了硬件级别的操作，比如说获取某个属性在内存中的位置，比如说修改对象的字段值，即使它是私有的。不过 Java 本身就是为了屏蔽底层的差异，对于一般的开发而言也很少会有这样的需求。

举两个例子，比方说：

```java
public native long staticFieldOffset(Field paramField);
```

这个方法可以用来获取给定的 paramField 的内存地址偏移量，这个值对于给定的 field 是唯一的且是固定不变的。



再比如说：

```java
public native int arrayBaseOffset(Class paramClass);
public native int arrayIndexScale(Class paramClass);
```

前一个方法是用来获取数组第一个元素的偏移地址，后一个方法是用来获取数组的转换因子即数组中元素的增量地址的。

```java
public native long allocateMemory(long paramLong);
public native long reallocateMemory(long paramLong1, long paramLong2);
public native void freeMemory(long paramLong);
```

这三个方法分别用来分配内存，扩充内存和释放内存的。

 

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

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251040434.gif)

假设线程A和线程B同时执行getAndInt操作（分别跑在不同的CPU上）

1. AtomicInteger里面的value原始值为3，即主内存中AtomicInteger的 value 为3，根据JMM模型，线程A和线程B各自持有一份价值为3的副本，分别存储在各自的工作内存
2. 线程A通过getIntVolatile(var1 , var2) 拿到value值3，这时线程A被挂起（该线程失去CPU执行权）
3. 线程B也通过getIntVolatile(var1, var2)方法获取到value值也是3，此时刚好线程B没有被挂起，并执行了compareAndSwapInt方法，比较内存的值也是3，成功修改内存值为4，线程B打完收工，一切OK
4. 这时线程A恢复，执行CAS方法，比较发现自己手里的数字3和主内存中的数字4不一致，说明该值已经被其它线程抢先一步修改过了，那么A线程本次修改失败，只能够重新读取后在来一遍了，也就是继续执行do while
5. 线程A重新获取value值，因为变量value被volatile修饰，所以其它线程对它的修改，线程A总能够看到，线程A继续执行compareAndSwapInt进行比较替换，直到成功。

但是AtomicInteger会存在CAS循环开销大的问题，因此JDK8引入LongAdder来解决这个问题

### LongAdder

LongAdder主要使用分段CAS以及自动分段迁移的方式来大幅度提升多线程高并发执行CAS操作的性能

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251040438.gif)

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

###  

 

