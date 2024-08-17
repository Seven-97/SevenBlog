---
title: 关键字 - final详解
category: Java
tag:
 - 并发编程
---





## final域重排序规则

Java内存模型为了能让处理器和编译器底层发挥他们的最大优势，对底层的约束就很少，也就是说针对底层来说Java内存模型就是 弱内存数据模型。同时，处理器和编译为了性能优化会对指令序列有编译器和处理器重排序。

final能够做出如下保证：当创建一个对象时，使用final关键字能够使得另一个线程不会访问到处于“部分创建”的对象，否则是会可能发生的。这是因为，当用作对象的一个属性时，final有着如下的语义：

> 当构造函数结束时，final类型的值是被保证其他线程访问该对象时，它们的值是可见的

 

对于final域，编译器和处理器要遵守两个重排序规则。

- 在构造函数内对一个final域的写入，与随后把这个被构造对象的引用赋值给个引用变量，这两个操作之间不能重排序。

- 初次读一个包含final域的对象的引用，与随后初次读这个final域，这两个操作之间不能重排序。

### 为什么是必须的

使用final是所谓的安全发布（safe publication）的一种方式，这里 发布（publication）意味着在一个线程中创建它，同时另一个线程在之后的某时刻可以引用到该新创建的对象。存在如下场景：当JVM调用对象的构造函数时，它必须将各成员赋值，同时存储一个指向该对象的指针；但就像其他普通数据写入一样，这可能是乱序的。

而final可以防止此类事情的发生：如果某个成员是final的，JVM规范做出如下明确的保证：一旦对象引用对其他线程可见，则其final成员也必须正确的赋值了。

 

### final域为基本类型

先看一段示例性的代码：

```java
public class FinalDemo {
    private int a;  //普通域
    private final int b; //final域
    private static FinalDemo finalDemo;

    public FinalDemo() {
        a = 1; // 1. 写普通域
        b = 2; // 2. 写final域
    }

    public static void writer() {//线程A执行
        finalDemo = new FinalDemo();
    }

    public static void reader() {//线程B执行
        FinalDemo demo = finalDemo; // 3.读对象引用
        int a = demo.a;    //4.读普通域
        int b = demo.b;    //5.读final域
    }
}
```

假设线程A在执行writer()方法，线程B执行reader()方法。

#### 写final域重排序规则

写final域的重排序规则 禁止对final域的写重排序到构造函数之外，这个规则的实现主要包含了两个方面：

- JMM禁止编译器把final域的写重排序到构造函数之外；

- 编译器会在final域写之后，构造函数return之前，插入一个storestore屏障。这个屏障可以禁止处理器把final域的写重排序到构造函数之外。

再分析writer方法，虽然只有一行代码，但实际上做了两件事情：

- 构造了一个FinalDemo对象；

- 把这个对象赋值给成员变量finalDemo。

存在的一种可能执行时序图，如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251033469.gif)

由于a,b之间没有数据依赖性，普通域(普通变量)a可能会被重排序到构造函数之外，线程B就有可能读到的是普通变量a初始化之前的值(零值)，这样就可能出现错误。而final域变量b，根据重排序规则，会禁止final修饰的变量b重排序到构造函数之外，从而b能够正确赋值，线程B就能够读到final变量初始化后的值。

因此，写final域的重排序规则可以确保：在对象引用为任意线程可见之前，对象的final域已经被正确初始化过了，而普通域就不具有这个保障。比如在上例，线程B有可能就是一个未正确初始化的对象finalDemo。

#### 读final域重排序规则

读final域重排序规则为：在一个线程中，初次读对象引用和初次读该对象包含的final域，JMM会禁止这两个操作的重排序。(注意，这个规则仅仅是针对处理器)，处理器会在读final域操作的前面插入一个LoadLoad屏障。实际上，读对象的引用和读该对象的final域存在间接依赖性，一般处理器不会重排序这两个操作。但是有一些处理器会重排序，因此，这条禁止重排序规则就是针对这些处理器而设定的。

read()方法主要包含了三个操作：

- 初次读引用变量finalDemo;

- 初次读引用变量finalDemo的普通域a;

- 初次读引用变量finalDemo的final域b;

假设线程A写过程没有重排序，那么线程A和线程B有一种的可能执行时序为下图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251033502.gif)

读对象的普通域被重排序到了读对象引用的前面就会出现线程B还未读到对象引用就在读取该对象的普通域变量，这显然是错误的操作。而final域的读操作就“限定”了在读final域变量前已经读到了该对象的引用，从而就可以避免这种情况。

读final域的重排序规则可以确保：在读一个对象的final域之前，一定会先读这个包含这个final域的对象的引用。

### final域为引用类型

#### 对final修饰的对象的成员域写操作

针对引用数据类型，final域写针对编译器和处理器重排序增加了这样的约束：在构造函数内对一个final修饰的对象的成员域的写入，与随后在构造函数之外把这个被构造的对象的引用赋给一个引用变量，这两个操作是不能被重排序的。注意这里的是“增加”也就说前面对final基本数据类型的重排序规则在这里还是使用。这句话是比较拗口的，下面结合实例来看。

```java
public class FinalReferenceDemo {
    final int[] arrays;
    private FinalReferenceDemo finalReferenceDemo;

    public FinalReferenceDemo() {
        arrays = new int[1];  //1
        arrays[0] = 1;        //2
    }

    public void writerOne() {
        finalReferenceDemo = new FinalReferenceDemo(); //3
    }

    public void writerTwo() {
        arrays[0] = 2;  //4
    }

    public void reader() {
        if (finalReferenceDemo != null) {  //5
            int temp = finalReferenceDemo.arrays[0];  //6
        }
    }
}
```



针对上面的实例程序，线程A执行wirterOne方法，执行完后线程B执行writerTwo方法，然后线程C执行reader方法。下图就以这种执行时序出现的一种情况来讨论

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251033492.jpg)

由于对final域的写禁止重排序到构造方法外，因此1和3不能被重排序。由于一个final域的引用对象的成员域写入不能与随后将这个被构造出来的对象赋给引用变量重排序，因此2和3不能重排序。

#### 对final修饰的对象的成员域读操作

JMM可以确保线程C至少能看到写线程A对final引用的对象的成员域的写入，即能看下arrays[0] = 1，而写线程B对数组元素的写入可能看到可能看不到。JMM不保证线程B的写入对线程C可见，线程B和线程C之间存在数据竞争，此时的结果是不可预知的。如果可见的，可使用锁或者volatile。

### 关于final重排序的总结

按照final修饰的数据类型分类：

- 基本数据类型:

  - final域写：禁止final域写与构造方法重排序，即禁止final域写重排序到构造方法之外，从而保证该对象对所有线程可见时，该对象的final域全部已经初始化过。

  - final域读：禁止初次读对象的引用与读该对象包含的final域的重排序。

- 引用数据类型：
  - 额外增加约束：禁止在构造函数对一个final修饰的对象的成员域的写入与随后将这个被构造的对象的引用赋值给引用变量 重排序

## final的实现原理

写final域会要求编译器在final域写之后，构造函数返回前插入一个StoreStore屏障。读final域的重排序规则会要求编译器在读final域的操作前插入一个LoadLoad屏障。

很有意思的是，如果以X86处理为例，X86不会对写-写重排序，所以StoreStore屏障可以省略。由于不会对有间接依赖性的操作重排序，所以在X86处理器中，读final域需要的LoadLoad屏障也会被省略掉。也就是说，以X86为例的话，对final域的读/写的内存屏障都会被省略！具体是否插入还是得看是什么处理器

### “溢出”带来的重排序问题

上面对final域写重排序规则可以确保：在使用一个对象引用的时候该对象的final域已经在构造函数被初始化过了。但是这里其实是有一个前提条件的，也就是：在构造函数，不能让这个被构造的对象被其他线程可见，也就是说该对象引用不能在构造函数中“溢出”。以下面的例子来说：

如下所示，溢出意味着，构造函数FinalReferenceEscapeDemo()还未执行完，由于2的执行，导致referenceDemo就已经不为null了

```java
public class FinalReferenceEscapeDemo {
    private final int a;
    private FinalReferenceEscapeDemo referenceDemo;

    public FinalReferenceEscapeDemo() {
        a = 1;  //1
        referenceDemo = this; //2
    }

    public void writer() {
        new FinalReferenceEscapeDemo();
    }

    public void reader() {
        if (referenceDemo != null) {  //3
            int temp = referenceDemo.a; //4
        }
    }
}
```



可能的执行时序如图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251033496.gif)

假设一个线程A执行writer方法，另一个线程执行reader方法。因为构造函数中操作1和2之间没有数据依赖性，1和2可以重排序，先执行了2，这个时候引用对象referenceDemo是个没有完全初始化的对象，而当线程B去读取该对象时就会出错。尽管依然满足了final域写重排序规则：在引用对象对所有线程可见时，其final域已经完全初始化成功。但是，引用对象“this”逸出，该代码依然存在线程安全的问题。

### 使用 final 的限制条件和局限性

当声明一个 final 成员时，必须在构造函数退出前设置它的值。

```java
public class MyClass {
  private final int myField = 1;
  public MyClass() {
    ...
  }
}
```



或者

```java
public class MyClass {
  private final int myField;
  public MyClass() {
    ...
    myField = 1;
    ...
  }
}
```



将指向对象的成员声明为 final 只能将该引用设为不可变的，而非所指的对象。

下面的方法仍然可以修改该 list。

```java
private final List myList = new ArrayList();
myList.add("Hello");
```



声明为 final 可以保证如下操作不合法

```java
myList = new ArrayList();
myList = someOtherList;
```



如果一个对象将会在多个线程中访问并且你并没有将其成员声明为 final，则必须提供其他方式保证线程安全。

" 其他方式 " 可以包括声明成员为 volatile，使用 synchronized 或者显式 Lock 控制所有该成员的访问。

### final byte

```java
byte b1=1;
byte b2=3;
byte b3=b1+b2;//当程序执行到这一行的时候会出错，因为b1、b2可以自动转换成int类型的变量，运算时java虚拟机对它进行了转换，结果导致把一个int赋值给byte-----出错
```



如果对b1 b2加上final就不会出错

```java
final byte b1=1;
final byte b2=3;
byte b3=b1+b2;//不会出错，相信你看了上面的解释就知道原因了。
```



 <!-- @include: @article-footer.snippet.md -->     

