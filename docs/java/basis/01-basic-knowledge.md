---
title: 基础知识点
category: Java
tag:
  - Java基础
---


## Integer缓存池

这里引申出一个经典问题，看下面代码

```java
Integer a = 100;

Integer b = 100;
System.out.println(a == b);

Integer c = 200;
Integer d = 200;
System.out.println(c == d);

//输出
true
false
```

为什么第一个输出的是true，第二个输出的是false？

Integer a = 100的这种直接赋值操作，是调⽤Integer.valueOf(100)方法，从Integer.valueOf()源码可以看到，返回的是Integer对象，但这里的实现并不是简单的new Integer，而是先判断 i 这个值是否在IntegerCache范围内，如果在，直接返回IntegerCache中的值，如果不在则new Integer

```java
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
        // high value may be configured by property
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
                // If the property cannot be parsed into an int, ignore it.
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
```

从源码可以看到，默认Integer cache 的下限是-128，上限默认127。当赋值100给Integer时，刚好在这个范围内，所以从cache中取对应的Integer并返回，所以a和b返回的是同一个对象，所以  比较是相等的，当赋值200给Integer时，不在cache 的范围内，所以会new Integer并返回，当然  比较的结果是不相等的。

> 扩展：Byte,Short,Integer,Long 这 4 种包装类默认创建了数值 [-128，127] 的相应类型的缓存数据，Character 创建了数值在 [0,127] 范围的缓存数据，Boolean 直接返回 True or False

```java
System.out.println(Integer.valueOf(-128) == Integer.valueOf(-128));//1.true
System.out.println(Integer.valueOf(127) == Integer.valueOf(127));//2.true
System.out.println(Integer.valueOf(128) == Integer.valueOf(128));//3.false
System.out.println(Integer.parseInt("128") == Integer.valueOf(128));//4.true
```

1、2、3都好理解，缓存范围是 [-128，127]，1、2都在范围内，返回的是缓存中的对象，因此输出true，3不在范围内，返回的时新 new 的Integer，因此输出false。

那为什么4输出的是true呢？ 128 在缓存范围外，按道理会 new 出一个Integer对象，为什么输出true呢？

- 首先Integer.parseInt方法返回的是int 基本数据类型，不是对象，也就是说 Integer.parseInt("128") = 128

```java
public static int parseInt(String s) throws NumberFormatException {
    return parseInt(s,10);
}
```

- 当进行比较("==")运算时，会进行自动拆箱，也就是说 Integer.valueOf(128) 生成的 Integer 会自动拆箱成128
- 那么比较两个相等的额数值自然是true的

> 当基础类型与它们的包装类有如下几种情况时，编译器会自动进行装箱或拆箱：
>
> 赋值操作（装箱或拆箱）
>
> 进行加减乘除混合运算 （拆箱）
>
> 进行>,<,==比较运算（拆箱）
>
> 调用equals进行比较（装箱）
>
> ArrayList、HashMap等集合类添加基础类型数据时（装箱）

注意：三目运算符 condition ? 表达式 1：表达式 2 中，高度注意表达式 1 和 2 在类型对齐时，可能抛出因自动拆箱导致的 NPE 异常

1. 表达式 1 或 表达式 2 的值只要有一个是原始类型。
2. 表达式 1 或 表达式 2 的值的类型不一致，会强制拆箱升级成表示范围更大的那个类型。

```java
Integer a = 1;
Integer b = 2;
Integer c = null;
Boolean flag = false;
// a*b 的结果是 int 类型，那么 c 会强制拆箱成 int 类型，抛出 NPE 异常
Integer result = (flag ? a * b : c);
```



## String

Java中的String是不可变对象

在面向对象及函数编程语言中，不可变对象（英语：Immutable object）是一种对象，在被创造之后，它的状态就不可以被改变。至于状态可以被改变的对象，则被称为可变对象（英语：mutable object）。--来自百度百科

### Java8 String源码

```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {
    /** The value is used for character storage. */
    private final char value[];//Java 9已经优化为byte数组了

    /** Cache the hash code for the string */
    private int hash; // Default to 0
    
    ...
}
```

显然String字符串内部是使用char[]数组来存储。

而这个char[]数组是用 **private final**来修饰的，**private**就体现着面向对象的封装特性，并且String没有提供供外部访问的方法，这就意味着这个属性无法被外部访问；**final**则意味着这个属性无法修改，无法重新指向其他对象。且String 类没有提供/暴露修改这个字符串的方法。

因此，String是不可变对象

#### 不可变的优点

- 线程安全。同一个字符串实例可以被多个线程共享，因为字符串不可变，本身就是线程安全的。
- 支持hash映射。因为String的hash值经常会使用到，比如作为 Map 的键，不可变的特性也就使得hash值不会变，不需要重新计算。
- 字符串常量池优化。String对象创建之后，会缓存到字符串常量池中，下次需要创建同样的对象时，可以直接返回缓存的引用。

#### 一定不可变吗

事实上，可以通过反射来改变String中的值

```java
String str = "abcdef";
System.out.println("修改前的地址值：" + str + ",hash值"+ str.hashCode());
Class<? extends String> aClass = str.getClass();
Field value = aClass.getDeclaredField("value");
value.setAccessible(true);
value.set(str,"ghi".getBytes());
System.out.println("修改后的地址值：" + str + ",hash值"+ str.hashCode());
```

显然修改后的还是同一个地址和hash值

```java
修改前的地址值：abcdef,hash值-1424385949

修改后的地址值：ghi,hash值-1424385949
```

由于hash是被缓存下来的，所以通过反射修改后hashCode并不会变。除非进行重新计算，但是用反射修改String的值破坏了String的immutable特征，可能会带来一些奇怪的副作用，最好不要这么做。



### String能存储多少字符

1. 首先String的length方法返回是int。所以理论上长度一定不会超过int的最大值。
2. 编译器源码如下，限制了字符串长度大于等于65535就会编译不通过：

```java
private void checkStringConstant(DiagnosticPosition var1, Object var2) {
    if (this.nerrs == 0 && var2 != null && var2 instanceof String &&   ((String)var2).length() >= 65535) {
        this.log.error(var1, "limit.string", new Object[0]);
        ++this.nerrs;
    }
}
```



Java中的字符常量都是使用UTF 8编码的，UTF 8编码使用1~4个字节来表示具体的Unicode字符。所以有的字符占用一个字节，而平时所用的大部分中文都需要3个字节来存储。

```java
//65534个字母，编译通过
String s1 = "dd..d";

//21845个中文”自“,编译通过
String s2 = "自自...自";

//一个英文字母d加上21845个中文”自“，编译失败
String s3 = "d自自...自";
```

- 对于s1，一个字母d的UTF8编码占用一个字节，65534字母占用65534个字节，长度是65534，长度和存储都没超过限制，所以可以编译通过。

- 对于s2，一个中文占用3个字节，21845个正好占用65535个字节，而且字符串长度是21845，长度和存储也都没超过限制，所以可以编译通过。

- 对于s3，一个英文字母d加上21845个中文”自“占用65536个字节，超过了存储最大限制，编译失败。

当然，这个限制是特定于编译器的实现，而不是Java语言本身的限制。



3. JVM规范对常量池有所限制。

量池中的每一种数据项都有自己的类型。Java中的UTF-8编码的Unicode字符串在常量池中以`CONSTANTUtf8`类型表示。`CONSTANTUtf8`的数据结构如下：

```c++
CONSTANT_Utf8_info {
    u1 tag;
    u2 length;
    u1 bytes[length];
}
```

重点关注长度为 length 的那个bytes数组，这个数组就是真正存储常量数据的地方，而 length 就是数组可以存储的**最大字节数**，而不是字符数。length 的类型是u2，u2是无符号的16位整数，因此理论上允许的的最大长度是`2^16-1=65535`。所以上面byte数组的最大长度可以是65535。

当然，考虑到UTF-8是一种变长编码，一个字符可能需要1到4个字节来表示（取决于字符的具体值）。因此，如果你的字符串包含大量使用多个字节编码的字符，那么它能包含的实际字符数将会少于65535。



4. 运行时限制

String 运行时的限制主要体现在 String 的构造函数上。下面是 String 的一个构造函数：

```java
public String(char value[], int offset, int count) {
    ...
}
```

上面的count值就是字符串的最大长度。在Java中，int的最大长度是2^31-1。所以在运行时，String 的最大长度是2^31-1。

但是这个也是理论上的长度，实际的长度还要看JVM的内存。来看下，最大的字符串会占用多大的内存。

```
(2^31-1)*16/8/1024/1024/1024 = 2GB
```

所以在最坏的情况下，一个最大的字符串要占用4GB的内存。如果JVM不能分配这么多内存的话，会直接报错的。



**总结**：因此，主要的还是JVM规范对常量池的限制，使得byte数组的最大长度不能超过65535

**补充**：JDK9以后对String的存储进行了优化。底层不再使用char数组存储字符串，而是使用byte数组。对于LATIN1字符的字符串可以节省一倍的内存空间。详情请看 [Java9 - string字符串的变化](https://www.seven97.top/java/new-features/java9.html#string%E5%AD%97%E7%AC%A6%E4%B8%B2%E7%9A%84%E5%8F%98%E5%8C%96)



### String, StringBuffer 和 StringBuilder

1. 可变性

   - String不可变

   - StringBuffer 和 StringBuilder 可变

2. 线程安全

   - String 不可变，因此是线程安全的

   - StringBuilder不是线程安全的

   - StringBuffer 是线程安全的，内部使用 synchronized 进行同步



StringBuffer的append方法

```java
@Override
public synchronized StringBuffer append(Object obj) {
    toStringCache = null;
    super.append(String.valueOf(obj));
    return this;
}
```



### 拼接字符串建议StringBuilder

#### 源码

String源码，存放字符串的地方

```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {
    /** The value is used for character storage. */
    private final char value[];
    
    ...
}
```

StringBuilder自身没有自定义存储的容器，而是继承了其父类的容器

```java
abstract class AbstractStringBuilder implements Appendable, CharSequence {
    /**The value is used for character storage.*/
    char[] value;
    
    ...
}
```

##### String案例

```java
String s1 = "第1个字符串";
String s2 = "第2个字符串";
String str = s1 + s2;
```

以上操作可以看成是

```java
//这里只作为理解，相当于新开拓一个字符数组，然后复制
final char c1[] = {'第','1','个','字','符','串'};
final char c2[] = {'第','2','个','字','符','串'};
final char c3[] = new char[12];
c3[] =  {'第','1','个','字','符','串','第','2','个','字','符','串'};
```

- 创建s1的时候其实就是创建了第一个不可变的char[]数组，创建s2的时候创建了第二个不可变的char[]数组

- 创建str的时候其实就是另外又创建了一个数组，再将s1和s2的数据复制到str中

##### StringBuilder

```java
StringBuilder sb = new StringBuilder();
System.out.println("初始容量：" + sb.capacity());
sb.append("十五个十五个十五个十五个十五个");
System.out.println("追加15个字后sb容量：" + sb.capacity());
sb.append("一");
System.out.println("已经十六个字SB容量：" + sb.capacity());
sb.append("添加");
System.out.println("超过16个字的SB容量：" + sb.capacity());
```

输出

```java
初始容量：16
追加15个字后sb容量：16
已经十六个字SB容量：16
超过16个字的SB容量：34
```

**StringBuilder特征**：StringBuilder初始化容量是16(无参构造)

```java
public StringBuilder() {
  super(16);
}
```

- 追加之前会计算一次容量，大于所需容量则会重新创建一个char[]数组，计算规则是 newCapacity = (value.length << 1) + 2; 也就是原来长度*2 + 2

- StringBuilder在运算的时候每次会计算容量是否足够，如果所需容量不小于自身容量,那么就会重新分配一个自身容量两倍 +2 的char[].

```java
//追加操作
public AbstractStringBuilder append(String str) {
    if (str == null)
        return appendNull();
    int len = str.length();
    //count是当前char[]数组的使用大小，len是要追加的字符串的长度
    ensureCapacityInternal(count + len);
    str.getChars(0, len, value, count);
    count += len;
    return this;
}

//这个方法是确保 char[]数组的大小能装下新追加的字符串
private void ensureCapacityInternal(int minimumCapacity) {
     // overflow-conscious code
    //判断所需要的容量是否小于char[]数组的容量
    if (minimumCapacity - value.length > 0) {
        value = Arrays.copyOf(value,
     newCapacity(minimumCapacity));//如果小于，就扩容，并拷贝数组内容
    }
}
    
private int newCapacity(int minCapacity) {
    // overflow-conscious code
   int newCapacity = (value.length << 1) + 2;//扩容数组大小，也就是原来长度*2+2
   if (newCapacity - minCapacity < 0) {
       newCapacity = minCapacity;
   }
   return (newCapacity <= 0 || MAX_ARRAY_SIZE - newCapacity < 0)
       ? hugeCapacity(minCapacity)
       : newCapacity;
}
```

因此，如果再一次追加的时候容量足够，就无需创建新数组，也就省去了很多创建char[]的次数.

#### 小结：

- String之所以慢是因为，大部分cpu资源都被浪费在分配资源拷贝资源的部分了，相比StringBuilder有更多的内存消耗。

- StringBuilder快就快在，相比String，他在运算的时候分配内存次数小，所以拷贝次数和内存占用也随之减少，当有大量字符串拼接时，StringBuilder创建char[]的次数会少很多。

- 由于GC的机制，即使原来的char[]没有引用了，那么也得等到GC触发的时候才能回收，String运算过多的时候就会产生大量垃圾，消耗内存。

因此：

- 如果目标字符串需要大量拼接的操作，那么这个时候应当使用StringBuilder.

- 反之，如果目标字符串操作次数极少，或者是常量，那么就直接使用String.

### String.intern()

调用字符串对象的intern方法，会将该字符串对象尝试放入到串池中

- 如果串池中没有该字符串对象，则放入成功

- 如果有该字符串对象，则放入失败

无论放入是否成功，都会返回串池中的字符串对象

注意：此时如果调用intern方法成功，堆内存与串池中的字符串对象是同一个对象；如果失败，则不是同一个对象

#### 例1：

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

#### 例2：

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



### 自定义一个String类

#### 包名不为java.lang

```java
package com.seven.jvm;

public final class String {
    /** The value is used for character storage. */
    private final char value[] = {};

    /** Cache the hash code for the string */
    private int hash; // Default to 0

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = -6849794470754667710L;

    public String(int hash) {
        this.hash = hash;
    }

    public String(){

    }

    static{
        System.out.println("静态代码块--自定义String");
    }

    {
        System.out.println("代码块--自定义String");
    }
    public int hashCode() {
        int h = hash;
        if (h == 0 && value.length > 0) {
            char val[] = value;

            for (int i = 0; i < value.length; i++) {
                h = 31 * h + val[i];
            }
            hash = h;
        }
        return h;
    }

}
```



#### 包名为java.lang.String

```java
package java.lang;


public final class String {

    /**
     * The value is used for character storage.
     */
    private final char value[] = {};

    /**
     * Cache the hash code for the string
     */
    private int hash; // Default to 0

    /**
     * use serialVersionUID from JDK 1.0.2 for interoperability
     */
    private static final long serialVersionUID = -6849794470754667710L;

    public String(int hash) {
        this.hash = hash;
    }

    public String() {

    }

    static {
        System.out.println("静态代码块--自定义String");
    }

    {
        System.out.println("代码块--自定义String");
    }

    public int hashCode() {
        int h = hash;
        if (h == 0 && value.length > 0) {
            char val[] = value;

            for (int i = 0; i < value.length; i++) {
                h = 31 * h + val[i];
            }
            hash = h;
        }
        return h;
    }

}
```



##### String类下写main方法

```java
package java.lang;

public final class String {

    public static void main(String[] args) {
        System.out.println("aaa");;
    }
}
```

输出：

```java
错误: 在类 java.lang.String 中找不到 main 方法, 请将 main 方法定义为:
   public static void main(String[] args)
否则 JavaFX 应用程序类必须扩展javafx.application.Application
```

原因在于双亲委派模型，先从父类加载器寻找能不能加载此类，如果没有则再到子类；因此在加载String类时，会最终委派给Bootstrap ClassLoader去加载，加载的是rt.jar包中的那个java.lang.String，而rt.jar包中的String类是没有main方法的，因此报错误

##### 同包下新建一个类写main方法

```java
package java.lang;

public class Main {
    public static void main(String[] args) {
       String str =  new String();
    }
}
```

输出：

```java
java.lang.SecurityException: Prohibited package name: java.lang
    at java.lang.ClassLoader.preDefineClass(ClassLoader.java:662)
    at java.lang.ClassLoader.defineClass(ClassLoader.java:761)
    at java.security.SecureClassLoader.defineClass(SecureClassLoader.java:142)
    at java.net.URLClassLoader.defineClass(URLClassLoader.java:467)
    at java.net.URLClassLoader.access$100(URLClassLoader.java:73)
    at java.net.URLClassLoader$1.run(URLClassLoader.java:368)
    at java.net.URLClassLoader$1.run(URLClassLoader.java:362)
    at java.security.AccessController.doPrivileged(Native Method)
    at java.net.URLClassLoader.findClass(URLClassLoader.java:361)
    at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
    at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:335)
    at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
    at sun.launcher.LauncherHelper.checkAndLoadMain(LauncherHelper.java:495)
Error: A JNI error has occurred, please check your installation and try again
Exception in thread "main" 
```

限制包名，不能自定义这个包名，与java类库冲突，安全管理器不通过，这里不管用不用到String都会有这个报错

原因：java.lang 是java 自带类库包，是属于rt.jar 包下的文件，而rt.jar 是通过启动类加载器(Bootstrap ClassLoader)加载的，由于双亲委派，因此java.lang 包肯定早于自定义的java.lang 包的加载，就会冲突.

##### 调用方法不在java.lang包中

```java
package com.seven;

public class Test {
    public static void main(String[] args) {
        String string = new String();
    }
}
```

无输出

原因，由于双亲委派，这里加载的String包是rt.jar中的java.lang.String类。因此这里并没有用到自定义的String类，因为不会加载到自定义的String（即便改自定义String的包名也叫java.lang）

#### 小结

1. 可以自定义包名不为java.lang的String类，并区别包名正常使用

2. 自定义包名为java.lang的String类

   - String类下写main方法：由于双亲委派模型，在加载String类时，会最终委派给Bootstrap ClassLoader去加载，加载的是rt.jar包中的那个java.lang.String，而rt.jar包中的String类是没有main方法的，因此报错误

   - 启动类也在java.lang包下：这里与是否用到String类无关，会报 Prohibited package name: java.lang错误。由于双亲委派，java.lang 包肯定早于自定义的java.lang 包的加载，就会冲突.

   - 调用方法不在java.lang包中：此时由于双亲委派模型的存在，并不会加载到自定义的String类

## Object通用方法

```java
public final native Class<?> getClass()

public native int hashCode()

public boolean equals(Object obj)

protected native Object clone() throws CloneNotSupportedException

public String toString()

public final native void notify()

public final native void notifyAll()

public final native void wait(long timeout) throws InterruptedException

public final void wait(long timeout, int nanos) throws InterruptedException

public final void wait() throws InterruptedException

protected void finalize() throws Throwable {}
```



### equals和hashcode

#### ==

"==" 是运算符

1. 如果比较的对象是基本数据类型，则比较的是其存储的值是否相等；

2. 如果比较的是引用数据类型，则比较的是所指向对象的地址值是否相等（是否是同一个对象）。

```java
 public static void main(String[] args) {
    Person p1 = new Person("123");
    Person p2 = new Person("123");
    int a = 10;
    int b = 10;
    System.out.println(a == b);
    System.out.println(p1 == p2);//显然不是同一个对象
}
//输出
true
false
```



#### equals

作用是 用来判断两个对象是否相等。通过判断两个对象的地址是否相等(即，是否是同一个对象)来区分它们是否相等。源码如下：

```java
public boolean equals(Object obj) {
    return (this == obj);
}
```

equals 方法不能用于比较基本数据类型，如果没有对 equals 方法进行重写，则相当于“==”，比较的是引用类型的变量所指向的对象的地址值。

一般情况下，类会重写equals方法用来比较两个对象的内容是否相等。比如String类中的equals()是被重写了，比较的是对象的值。

#### hashcode

1. hashcode特性体现主要在它的查找效率上，O(1)的复杂度，在Set和Map这种使用哈希表结构存储数据的集合中。hashCode方法的就大大体现了它的价值，主要用于在这些集合中确定对象在整个哈希表中存储的区域。

2. 如果两个对象相同，则这两个对象的equals方法返回的值一定为true，两个对象的hashCode方法返回的值也一定相同。(equals相同，hashcode一定相同，因为重写的hashcode就是计算属性的hashcode值)

3. 如果两个对象返回的HashCode的值相同，但不能够说明这两个对象的equals方法返回的值就一定为true，只能说明这两个对象在存储在哈希表中的同一个桶中。

#### 只重写了equals方法，未重写hashCode方法

在Java中equals方法用于判断两个对象是否相等，而HashCode方法在java中主要由于哈希算法中的寻域的功能（也就是寻找数据应该存储的区域的）。在类似于set和map集合的结构中，java为了提高在集合中查询匹配元素的效率问题，引入了哈希算法，通过某种算法及我们的HashCode方法得到对象的hash码，再通过hash码推算出数据应该存储的位置。然后再进行equals操作进行匹配，减少了比较次数，提高了效率。

```java
public class Person {
    String name;

    public Person(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return Objects.equals(name, person.name);
    }

    public static void main(String[] args) {
        Person p1 = new Person("123");
        Person p2 = new Person("123");

        System.out.println(p1 == p2);//1、false
        System.out.println(p1.hashCode() == p2.hashCode());//2、false
        System.out.println(p1.equals(p2));//3、true

        Set<Person> set = new HashSet<>();
        set.add(p1);
        set.add(p2);
        System.out.println(set.size());//4、2
    }
}
```

- 当只重写了equals方法，未重写hashCode方法时，equals方法判断两个对象是否相等时，返回的是true(第三个输出)，这是因为我们重写equals方法时，是对属性的比较；但判断两个对象的hashCode值是否相等时，返回的是false(第二个输出)，在没有重写hashCode方法的情况下，调用的是Object的hashCode方法，返回的是本对象的hashCode值，两个对象不一样，因此hashCode值不一样。

- 在set和map中，首先判断两个对象的hashCode方法返回的值是否相等，如果相等然后再判断两个对象的equals方法，如果hashCode方法返回的值不相等，则直接会认为两个对象不相等，不进行equals方法的判断。因此在set添加对象时，因为hashCode值已经不一致，判断出p1和p2是两个对象，都会添加进set集合中，因此返回集合中数据个数为 2 (第四个输出)

![image-20240425065031351](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250650480.png)

重写hashCode方法：重写hashcode方法时，一般也是对属性值进行hash

```java
@Override
public int hashCode() {
    return Objects.hash(name);
}
```

重写了hashCode后，其是对属性值的hash，p1和p2的属性值一致，因此p1.hashCode() == p2.hashCode()为true，再进行equals方法的判断也为true，认为是一个对象，因此set集合中只有一个对象数据。

##### 为什么重写hashCode一定也要重写equals方法？

如果两个对象的hashCode相同，它们是并不一定相同的，因为equals方法不相等而hashCode方法返回的值却有可能相同的，比如两个不同的对象hash到同一个桶中

hashCode方法实际上是通过一种算法得到一个对象的hash码，这个hash码是用来确定该对象在哈希表中具体的存储区域的。返回的hash码是int类型的所以它的数值范围为 [-2147483648 - +2147483647] 之间的，而超过这个范围，实际会产生溢出，溢出之后的值实际在计算机中存的也是这个范围的。比如最大值 2147483647 + 1 之后并不是在计算机中不存储了，它实际在计算机中存储的是-2147483648。在java中hash码也是通过特定算法得到的，所以很难说在这个范围内情况下不会不产生相同的hash码的。也就是说常说的哈希碰撞，因此不同对象可能有相同的hashCode的返回值。

**因此equals方法返回结果不相等，而hashCode方法返回的值却有可能相同！**

##### 为什么重写equals一定也要重写hashCode方法？

这个是针对set和map这类使用hash值的对象来说的

1. 只重写equals方法，不重写hashCode方法：

   * 有这样一个场景有两个Person对象，可是如果没有重写hashCode方法只重写了equals方法，equals方法认为如果两个对象的name相同则认为这两个对象相同。这对于equals判断对象相等是没问题的。

   * 对于set和map这类使用hash值的对象来说，由于没有重写hashCode方法，此时返回的hash值是不同的，因此不会去判断重写的equals方法，此时也就不会认为是相同的对象。

2. 重写hashCode方法不重写equals方法

   * 不重写equals方法实际是调用Object方法中的equals方法，判断的是两个对象的堆内地址。而hashCode方法认为相等的两个对象在equals方法处并不相等。因此也不会认为是用一个对象

   * 因此重写equals方法时一定也要重写hashCode方法，重写hashCode方法时也应该重写equals方法。

总结：**对于普通判断对象是否相等来说，只equals是可以完成需求的，但是如果使用set，map这种需要用到hash值的集合时，不重写hashCode方法，是无法满足需求的。**

#### 扩展：解决哈希冲突的三种方法

拉链法、开放地址法、再散列法

##### 拉链法

HashMap，HashSet其实都是采用的拉链法来解决哈希冲突的，就是在每个位桶实现的时候，采用链表（jdk1.8之后采用链表+红黑树）的数据结构来去存取发生哈希冲突的输入域的关键字（也就是被哈希函数映射到同一个位桶上的关键字）

拉链法的装载因子为n/m（n为输入域的关键字个数，m为位桶的数目）

##### 开放地址法

所谓开放地址法就是发生冲突时在散列表（也就是数组里）里去寻找合适的位置存取对应的元素，就是所有输入的元素全部存放在哈希表里。也就是说，位桶的实现是不需要任何的链表来实现的，换句话说，也就是这个哈希表的装载因子不会超过1。

它的实现是在插入一个元素的时候，先通过哈希函数进行判断，若是发生哈希冲突，就以当前地址为基准，根据再寻址的方法（探查序列），去寻找下一个地址，若发生冲突再去寻找，直至找到一个为空的地址为止。

探查序列的方法:

- 线性探查

- 平方探测

- 伪随机探测

###### 线性探查

di =1，2，3，…，m-1；这种方法的特点是：冲突发生时，顺序查看表中下一单元，直到找出一个空单元或查遍全表。

![image-20240425065332769](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250653895.png)

**（使用例子：ThreadLocal里面的ThreadLocalMap中的set方法）**

```java
private void set(ThreadLocal<?> key, Object value) {

    // We don't use a fast path as with get() because it is at
    // least as common to use set() to create new entries as
    // it is to replace existing ones, in which case, a fast
    // path would fail more often than not.

    Entry[] tab = table;
    int len = tab.length;
    int i = key.threadLocalHashCode & (len-1);

    //线性探测的关键代码
    for (Entry e = tab[i];
         e != null;
         e = tab[i = nextIndex(i, len)]) {
        ThreadLocal<?> k = e.get();

        if (k == key) {
            e.value = value;
            return;
        }

        if (k == null) {
            replaceStaleEntry(key, value, i);
            return;
        }
    }

    tab[i] = new Entry(key, value);
    int sz = ++size;
    if (!cleanSomeSlots(i, sz) && sz >= threshold)
        rehash();
}
```

但是这样会有一个问题，就是随着键值对的增多，会在哈希表里形成连续的键值对。当插入元素时，任意一个落入这个区间的元素都要一直探测到区间末尾，并且最终将自己加入到这个区间内。这样就会导致落在区间内的关键字Key要进行多次探测才能找到合适的位置，并且还会继续增大这个连续区间，使探测时间变得更长，这样的现象被称为“一次聚集（primary clustering）”。

![image-20240425065402242](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250654349.png)

![image-20240425065409114](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250654208.png)

###### 平方探测

在探测时不一个挨着一个地向后探测，我们可以跳跃着探测，这样就避免了一次聚集。

di=12，-12，22，-22，…，k2，-k2；这种方法的特点是：冲突发生时，在表的左右进行跳跃式探测，比较灵活。虽然平方探测法解决了线性探测法的一次聚集，但是它也有一个小问题，就是关键字key散列到同一位置后探测时的路径是一样的。这样对于许多落在同一位置的关键字而言，越是后面插入的元素，探测的时间就越长。

![image-20240425065419835](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250654938.png)

![image-20240425065426444](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250654538.png)

这种现象被称作“二次聚集(secondary clustering)”,其实这个在线性探测法里也有。

###### 伪随机探测

di=伪随机数序列；具体实现时，应建立一个伪随机数发生器，（如i=(i+p) % m），生成一个位随机序列，并给定一个随机数做起点，每次去加上这个伪随机数++就可以了。

##### 再散列法

再散列法其实很简单，就是再使用哈希函数去散列一个输入的时候，输出是同一个位置就再次散列，直至不发生冲突位置

缺点：每次冲突都要重新散列，计算时间增加。一般不用这种方式

### clone()

clone方法是Java中拷贝对象的方法

在Java中拷贝对象其实是指复制一个与原对象一样的新对象出来，但是在Java中 赋值 = 是复制对象引用，如果我们想要得到一个对象的副本，使用赋值操作是无法达到目的的。

看下面例子：

```java
Animal a1 = new Animal();
a1.category = "人类";
Animal a2 = a1;
System.out.println(a1==a2);
a2.category = "猫科动物";
System.out.println(a1.category);

//输出：
true
猫科动物
```

显然，a1==a2返回为true，a1和a2指向的是同一个引用

并且对对象a2的属性值进行修改后，a1的属性值也跟着改变，因此这不是拷贝，要实现拷贝，需要用到Object对象的clone()方法，实现对对象中各个属性的复制，但它的可见范围是protected的

```java
protected native Object clone() throws CloneNotSupportedException;
```

- 所以实体类使用clone()方法的前提是：实现Cloneable接口，这是一个标记接口，自身没有方法，这算是一种约定。调用clone方法时，会去判断有没有实现Cloneable接口，没有实现Cloneable的话会抛异常CloneNotSupportedException。

- 覆盖clone()方法，可见性提升为public。

```java
public class Animal implements Cloneable {
    String category;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static void main(String[] args) throws CloneNotSupportedException {
        Animal a1 = new Animal();
        a1.category = "人类";
        Animal a2 = (Animal) a1.clone();
        System.out.println(a1 == a2);
        a2.category = "猫科动物";

        System.out.println(a1.category);
    }
}
//输出：
false
人类
```

拷贝了一个新对象，与原对象引用不同，因此返回false，并且修改新对象的属性值，旧对象的属性值不改变

#### 浅拷贝

当类中含有引用类型的属性时，新对象和旧对象的引⽤类型属性指向的是同⼀个对象，即为浅拷贝

```java
public class Animal implements Cloneable {
    String category;
    Person person;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static void main(String[] args) throws CloneNotSupportedException {
        Animal a1 = new Animal();
        Person person = new Person("旧对象");
        a1.category = "人类";
        a1.person = person;

        Animal a2 = (Animal) a1.clone();
        a2.category = "新人类";
        a2.person.name = "新对象";

        System.out.println(a1.category + ":" + a1.person.name);

    }
}
//输出：
人类：新对象

```

如上，改变 a2中的引用类型 person.name = "新对象"后，a1的的引用类型 person.name 也发生了改变。

也就是说，新对象和旧对象的引⽤类型属性指向的是同⼀个对象，因此当改变新对象的引用类型(person)的属性值时，旧对象的引用类型的属性值也被改变。因此只是浅拷贝

> 这里可能就有疑问了，String类型也是引用类型，为什么进行了深拷贝，即如上所示，改变a2的属性值category，但却没有改变a1的category？ String类型有点特殊，它本身没有实现Cloneable接口，故根本无法克隆，只能传递引用（注意：Java只有值传递，只是这里传递是原来引用地址值）。在clone()后，克隆后的对象开始也是指向的原来引用地址值(刚克隆完检查a1.category == a2.category 为true)，但是一旦String的值发生改变（String作为不可更改的类——immutable class，在新赋值的时候，会创建了一个新的对象）就改变了克隆后对象指向的地址，让它指向了一个新的String地址，不会影响原对象的指向和值，原来的String对象还是指向的它自己的的地址。这样String在拷贝的时候就**表现出了深拷贝**的特点

#### 深拷贝

当类中含有引用类型的属性时，新对象和旧对象的引⽤类型属性指向的不是同⼀个对象，即为深拷贝

要实现深拷贝，在clone方法中不仅调用了super.clone，而且需要调用Person对象的clone方法（Person也要实现Cloneable接口并重写clone方法），从而实现了深拷贝。

```java
public class Animal implements Cloneable {
    String category;
    Person person;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Animal animal = null;
        animal = (Animal) super.clone();
        animal.person = (Person) person.clone();
        return animal;
    }

    public static void main(String[] args) throws CloneNotSupportedException {
        Animal a1 = new Animal();
        Person person = new Person("旧对象");
        a1.category = "人类";
        a1.person = person;

        Animal a2 = (Animal) a1.clone();
        a2.person.name = "新对象";

        System.out.println(a1.person.name);

    }
}
//输出：
旧对象
```

可以看到，新对象的引用类型 person 不会再受到旧对象的影响。

但是，在EffectiveJava中，反对使用clone方法来进行克隆，详情关注[谨慎重写 clone 方法](https://www.seven97.top/books/effectivejava-summary.html#_13、谨慎重写-clone-方法)

 

## 关键字

### final

1. 数据：声明数据为常量，可以是编译时常量，也可以是在运行时被初始化后不能被改变的常量。

   - 对于基本类型，final 使数值不变；

   - 对于引用类型，final 使引用不变，也就不能引用其它对象，但是被引用的对象本身是可以修改的。

2. 方法：声明方法不能被子类重写。
   - private 方法隐式地被指定为 final，如果在子类中定义的方法和基类中的一个 private 方法签名相同，此时子类的方法不是重写基类方法，而是在子类中定义了一个新的方法。

3. **类：**声明类不允许被继承。

### static

1. 静态变量
   - 静态变量: 又称为类变量，也就是说这个变量属于类的，类所有的实例都共享静态变量，可以直接通过类名来访问它；静态变量在内存中只存在一份。
   - 实例变量: 每创建一个实例就会产生一个实例变量，它与该实例同生共死。

2. **静态方法**

   - 静态方法在类加载的时候就存在了，它不依赖于任何实例。所以静态方法必须有实现，也就是说它不能是抽象方法(abstract)。

   - 只能访问所属类的静态字段和静态方法，方法中不能有 this 和 super 关键字。

3. 静态语句块
   - 静态语句块在类初始化时运行一次。

4. 静态内部类

   - 非静态内部类依赖于外部类的实例，而静态内部类不需要。

   - 静态内部类不能访问外部类的非静态的变量和方法。

5. 静态导包
   - 在使用静态变量和方法时不用再指明 ClassName，从而简化代码，但可读性大大降低。

#### 初始化顺序

1. 静态属性，静态代码块。

2. 普通属性，普通代码块。

3. 构造方法。

```java
public class InitOrder {

    // 静态属性
    private static String staticField = getStaticField();

    // 静态代码块
    static {
        System.out.println(staticField);
        System.out.println("静态代码块初始化");
    }

    // 普通属性
    private String field = getField();

    // 普通代码块
    {
        System.out.println(field);
        System.out.println("普通代码块初始化");
    }

    // 构造方法
    public InitOrder() {
        System.out.println("构造方法初始化");
    }

    // 静态方法
    public static String getStaticField() {
        String staticFiled = "静态属性初始化";
        return staticFiled;
    }

    // 普通方法
    public String getField() {
        String filed = "普通属性初始化";
        return filed;
    }

    public static void main(String[] argc) {
        new InitOrder();
    }

    /**
     *      静态属性初始化
     *      静态代码块初始化
     *      普通属性初始化
     *      普通代码块初始化
     *      构造方法初始化
     */
}
```



#### 静态方法和变量能否被继承

能

父类A：

```java
public class A {
    public static String staticStr = "A静态属性";
    public String nonStaticStr = "A非静态属性";
    public static void staticMethod(){
        System.out.println("A静态方法");
    }
    public void nonStaticMethod(){
        System.out.println("A非静态方法");
    }
}
```



子类B：

```java
public class B extends A{

    public static String staticStr = "B改写后的静态属性";
    public String nonStaticStr = "B改写后的非静态属性";

    public static void staticMethod(){
        System.out.println("B改写后的静态方法");
    }

    @Override
    public void nonStaticMethod() {
        System.out.println("B改写后的非静态方法");
    }
}
```



子类C：

```java
public class C extends A{
}
```



测试：

```java
public class Demo {
    public static void main(String[] args) {
        C c = new C();//C的引用指向C的对象
        System.out.println(c.nonStaticStr);//A非静态属性
        System.out.println(c.staticStr);//A静态属性
        c.nonStaticMethod();//A非静态方法
        c.staticMethod();//A静态方法
        //推出静态属性和静态方法可以被继承

        System.out.println("-------------------------------");

        A c1 = new C();//A的引用指向C的对象
        System.out.println(c1.nonStaticStr);//A非静态属性
        System.out.println(c1.staticStr);//A静态属性
        c1.nonStaticMethod();//A非静态方法
        c1.staticMethod();//A静态方法
        //推出静态属性和静态方法可以被继承

        System.out.println("-------------------------------");
        B b = new B();//B的引用指向B的对象
        System.out.println(b.nonStaticStr);//B改写后的非静态属性
        System.out.println(b.staticStr);//B改写后的静态属性
        b.nonStaticMethod();//B改写后的非静态方法
        b.staticMethod();//B改写后的静态方法

        System.out.println("-------------------------------");
        A b1 = new B();//A的引用指向B的对象
        System.out.println(b1.nonStaticStr);//A非静态属性
        System.out.println(b1.staticStr);//A静态属性
        b1.nonStaticMethod();//B改写后的非静态方法
        b1.staticMethod();//A静态方法
        //结果都是父类的静态方法，说明静态方法不可以被重写，不能实现多态
    }
}
```



**static小结：**

- 子类会继承父类的静态方法和静态变量，但是无法对静态方法进行重写

- 子类中可以直接调用父类的静态方法和静态变量

- 子类可以直接修改（如果父类中没有将静态变量设为private）静态变量，但这是子类自己的静态变量。

- 子类可以拥有和父类同名的，同参数的静态方法，但是这并不是对父类静态方法的重写，是子类自己的静态方法，子类只是把父类的静态方法隐藏了。

- 当父类的引用指向子类时，使用对象调用静态方法或者静态变量，是调用的父类中的静态方法或者变量（这比较好理解，因为静态方法或变量是属于类的，而引用指向的是一个对象，对象中并不会包含静态的方法和属性）。**也就是说，失去了多态。**

- 当子类的引用指向子类时，使用对象调用静态方法或者静态变量，就是调用的子类中自己的的静态方法或者变量了。

 

 

 













