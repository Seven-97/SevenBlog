---
title: Java 9~10新特性
category: Java
tags:
  - 版本新特性
head:
  - - meta
    - name: keywords
      content: Java,版本新特性,Java9
  - - meta
    - name: description
      content: 全网最全的Java 版本新特性知识点总结，让天下没有难学的八股文！
---




## Java 9新特性

Java 9 于2017年9月发布， 新增了很多特性，针对较为突出的便于理解的特性进行说明。除了下面罗列出的新特性之外还有一些其他的内容，这些内容有的不重要或者使用较少，所以没有罗列出来。

### 接口私有方法

在jdk9中新增了接口私有方法，即可以在接口中声明private修饰的方法了，其实就是为了让default方法调用，当然接口外部是无法访问私有方法的。这样的话，接口越来越像抽象类

```java
public interface MyInterface {
    //定义私有方法
    private void m1() {
        System.out.println("123");
    }
    
    //default中调用
    default void m2() {
        m1();
    }
}
```



### 改进的try with resource

Java7中新增了try with resource语法用来自动关闭资源文件，在IO流和jdbc部分使用的比较多。使用方式是将需要自动关闭的资源对象的创建放到try后面的小括号中，在jdk9中可以将这些资源对象的创建代码放到小括号外面，然后将需要关闭的对象名放到try后面的小括号中即可，示例：

```java
/*
    改进了try-with-resources语句，可以在try外进行初始化，在括号内填写引用名，即可实现资源自动关闭
 */
public class TryWithResource {
    public static void main(String[] args) throws FileNotFoundException {
        //jdk8以前
        try (FileInputStream fileInputStream = new FileInputStream("");
             FileOutputStream fileOutputStream = new FileOutputStream("")) {

        } catch (IOException e) {
            e.printStackTrace();
        }

        //jdk9
        FileInputStream fis = new FileInputStream("");
        FileOutputStream fos = new FileOutputStream("");
        //多资源用分号隔开
        try (fis; fos) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### 模块化

Java8中有个非常重要的包rt.jar，里面涵盖了Java提供的类文件，在程序员运行Java程序时jvm会加载rt.jar。这里有个问题是rt.jar中的某些文件我们是不会使用的，比如使用Java开发服务器端程序的时候通常用不到图形化界面的库Java.awt ，这就造成了内存的浪费。

Java9中将rt.jar分成了不同的模块，一个模块下可以包含多个包，模块之间存在着依赖关系，其中Java.base模块是基础模块，不依赖其他模块。上面提到的Java.awt被放到了其他模块下，这样在不使用这个模块的时候就无需让jvm加载，减少内存浪费。让jvm加载程序的必要模块，并非全部模块，达到了瘦身效果。

 

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251954529.gif)

jar包中含有.class文件，配置文件。jmod除了包含这两个文件之外，还有native library，legal licenses等，两者主要的区别是jmod主要用在编译期和链接期，并非运行期，因此对于很多开发者来说，在运行期仍然需要使用jar包。

模块化的优点：

- 精简jvm加载的class类，提升加载速度

- 对包更精细的控制，提高安全

模块与包类似，只不过一个模块下可以包含多个包。下面举例来看下

- 项目----公司

- 模块----部门：开发部，测试部

- 包名----小组：开发1组，开发2组

- 类 ----员工：张三，李四

接下来演示在测试部模块中调用开发部模块里面的类。

1. 创建项目，项目下分别创建开发部（命名develop），测试部（命名test）2个模块
2. 在开发部中创建下面2个包和类

```java
//dev1包
package com.seven97.dev1;

public class Cat {
    public void eat() {
        System.out.println("吃鱼");
    }
}

//dev2包
package com.seven97.dev2;

public class Apple {
    private String name;
}
```

3. 在src目录下创建module-info.Java文件，通过module-info.Java的文件来描述模块，开发部模块要将com.seven97.dev1包导出，在src目录下创建module-info.Java

```java
module develop {//develop名字跟模块名一致
    exports com.seven97.dev1;//导出的包，该包下的类可以被其他模块访问
    opens com.seven97.dev2;//导出包，该包下的类可以被其他模块通过反射访问
}
```



4. 在测试部模块的src目录下创建module-info.Java

```java
module test {// test名字跟模块名字一致
    requires develop;// 导入develop模块
}
```



5. 在测试部模块中创建

```java
package com.seven97.test1;

import com.seven97.dev1.Cat;


public class CatTest {
    public static void main(String[] args) throws Exception {
        Cat cat = new Cat();
        cat.eat();
        
        //反射
        Class<?> clazz = Class.forName("com.seven97.dev2.Apple");
        Object obj = clazz.getDeclaredConstructor().newInstance();
    }
}
```

上面例子演示了不同模块下类的使用，主要是依赖module-info.Java文件描述了模块的关系。


### 集合工厂方法

Java9 为集合类添加了便捷的工厂方法，能够轻松创建不可变集合。在这之前，创建不可变集合还是比较麻烦的，很多开发者会选择依赖第三方库(比如 Google Guava)。

传统的不可变集合创建方式：
```java
// Java 9 之前创建不可变集合的方式  
List<String> oldList = new ArrayList<>();  
oldList.add("苹果");  
oldList.add("香蕉");  
oldList.add("Seven");  
List<String> immutableList = Collections.unmodifiableList(oldList);  
  
// 或者使用 Google Guava 等第三方库  
List<String> guavaList = ImmutableList.of("苹果", "香蕉", "Seven");
```

有了 Java9 的工厂方法，创建不可变集合简直不要太简单

```java
// Java 9 的简洁写法  
List<String> fruits = List.of("苹果", "香蕉", "Seven");  
Set<Integer> numbers = Set.of(1, 2, 3, 4, 5);  
Map<String, Integer> scores = Map.of(  
    "张三", 85,   
    "Seven", 92,   
    "狗剩", 78  
);
```

这些集合是真正不可变的，任何修改操作都会抛出 Unsupported0perationException 异常。

如果想创建包含大量元素的不可变 Map，可以使用 ofEntries 方法
```java
Map<String, String> largeMap = Map.ofEntries(  
    Map.entry("key1", "value1"),  
    Map.entry("key2", "value2"),  
    Map.entry("key3", "value3")  
    // ... 可以有任意多个  
);
```


### jshell

在一些编程语言中，例如：python，Ruby等，都提供了REPL（Read Eval Print Loop 简单的交互式编程环境）。jshell就是Java语言平台中的REPL。

有的时候只是想写一段简单的代码，例如HelloWorld，按照以前的方式，还需要自己创建Java文件，创建class，编写main方法，但实际上里面的代码其实就是一个打印语句，此时还是比较麻烦的。在jdk9中新增了jshell工具，可以快速的运行一些简单的代码。

从命令提示符里面输入jshell，进入到jshell之后输入：

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251954531.gif)

如果要退出jshell的话，输入/exit即可。

 


### 不能使用下划线命名变量

下面语句在jdk9之前可以正常编译通过，但是在jdk9（含）之后编译报错，在后面的版本中会将下划线作为关键字来使用

```java
String _ = "seven97";
```



### String字符串的变化

写程序的时候会经常用到String字符串，在以前的版本中String内部使用了char数组存储，对于使用英语的人来说，一个英文字符用一个字节就能存储，使用char存储字符会浪费一半的内存空间，因此在JDK9中将String内部的char数组改成了byte数组，这样就节省了一半的内存占用。

也就是说，能优化包含大量ASCII字符的字符串。

```java
char c = 'a';//2个字节
byte b = 97;//1个字节
```



 String中增加了下面2个成员变量

- COMPACT_STRINGS：boolen类型，判断是否压缩，

  - 默认是true，表示开启压缩，英文字符可以节省空间

  - false，则表示不压缩，全部使用UTF16编码。也就是说，即使是英文字符，也用UTF16编码，则无法节省空间

- coder：byte类型，用来区分使用的字符编码，分别为LATIN1（值为0）和UTF16（值为1）。

byte数组如何存储中文呢？通过源码（StringUTF16类中的toBytes方法）得知，在使用中文字符串时，1个中文会被存储到byte数组中的两个元素上，即存储1个中文，byte数组长度为2，存储2个中文，byte数组长度为4。

以如下代码为例进行分析：

```java
String str = "好"
```

好 对应的Unicode码二进制为0101100101111101，分别取出高8位和低8位，放入到byte数组中{01011001,01111101}，这样就利用byte数组的2个元素保存了1个中文。

 

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251954498.gif)

当字符串中存储了中英混合的内容时，1个英文字符同样会占用2个byte数组位置，例如下面代码底层byte数组的长度为16：

```java
String str = "猴子monkey";
```

在获取字符串长度时，若存储的内容存在中文，是不能直接获取byte数组的长度作为字符串长度的，String源码中有向右移动1位的操作（即除以2），这样才能获取正确的字符串长度。

 

#### new String()底层源码

```java
public String(char value[]) {
    this(value, 0, value.length, null);
}

String(char[] value, int off, int len, Void sig) {
    if (len == 0) {
        this.value = "".value;
        this.coder = "".coder;
        return;
    }
    if (COMPACT_STRINGS) {
        // 检查是否都是英文，是的话就进行压缩
        byte[] val = StringUTF16.compress(value, off, len);
        if (val != null) {// 不为null则表示都是英文字符
            this.value = val;
            // 将编码设置为LATIN1
            this.coder = LATIN1;
            return;
        }
    }
    // 到这里则表示有其他字符，无法压缩，因此编码设置为UTF16
    this.coder = UTF16;
    // 以两字节的方式来存储一个字符
    this.value = StringUTF16.toBytes(value, off, len);
}

@HotSpotIntrinsicCandidate
public static byte[] toBytes(char[] value, int off, int len) {
    // 由于有中文字符了，因此就需要用两个字节来存储一个字符了，那就吧byte数组扩大一倍
    byte[] val = newBytesFor(len);
    for (int i = 0; i < len; i++) {
        putChar(val, i, value[off]);
        off++;
    }
    return val;
}

// 其实就是将byte数组扩大一倍
public static byte[] newBytesFor(int len) {
    if (len < 0) {
        throw new NegativeArraySizeException();
    }
    if (len > MAX_LENGTH) {
        throw new OutOfMemoryError("UTF16 String size is " + len +
                                   ", should be less than " + MAX_LENGTH);
    }
    return new byte[len << 1];
}

// 两字节字符拆分
static void putChar(byte[] val, int index, int c) {
    assert index >= 0 && index < length(val) : "Trusted caller missed bounds check";
    index <<= 1;// 每个字符占两个字节，因此每次索引都得乘2
    val[index++] = (byte)(c >> HI_BYTE_SHIFT);// 取低八位
    val[index]   = (byte)(c >> LO_BYTE_SHIFT);// 取高八位
}

// 字符合并
static char getChar(byte[] val, int index) {
    assert index >= 0 && index < length(val) : "Trusted caller missed bounds check";
    index <<= 1;
    return (char)(((val[index++] & 0xff) << HI_BYTE_SHIFT) |
                  ((val[index]   & 0xff) << LO_BYTE_SHIFT));
}
```

总结：如果字符串是纯英文，则编码默认为LATIN1，是可以压缩存储空间的。只要有中文，不管是否是中英混合，那么就都需要两个字节的byte数组来存储字符。



#### String字符串拼接"+"

代码：

```java
class Demo {
    public static String concatIndy(int i) {
        return  "value " + i;
    }
}
```



编译查看字节码：

```java
class Demo {
  Demo();
    Code:
       0: aload_0
       1: invokespecial #1 // Method java/lang/Object."<init>":()V
       4: return

  public static java.lang.String concatIndy(int);
    Code:
       0: iload_0
       1: invokedynamic #2,  0 // InvokeDynamic #0:makeConcatWithConstants:(I)Ljava/lang/String;
       6: areturn
}
```

可以看到，编译后的字节码和 JDK 8 是不一样的，不再是基于StringBuilder实现，而是基于`StringConcatFactory.makeConcatWithConstants`动态生成一个方法来实现。这个会比StringBuilder更快，不需要创建StringBuilder对象，也会减少一次数组拷贝。

这里由于是内部使用的数组，所以用了UNSAFE.allocateUninitializedArray的方式更快分配byte[]数组。通过：`StringConcatFactory.makeConcatWithConstants`而不是JavaC生成代码，是因为生成的代码无法使用JDK的内部方法进行优化，还有就是，如果有算法变化，存量的Lib不需要重新编译，升级新版本JDk就能提速。

这个字节码相当如下手工调用：`StringConcatFactory.makeConcatWithConstants`

```java
import java.lang.invoke.*;

static final MethodHandle STR_INT;

static {
    try {
        STR_INT = StringConcatFactory.makeConcatWithConstants(
            MethodHandles.lookup(),
            "concat_str_int",
            MethodType.methodType(String.class, int.class),
            "value \1"
        ).dynamicInvoker();
    } catch (Exception e) {
        throw new Error("Bootstrap error", e);
    }
}

static String concat_str_int(int value) throws Throwable {
    return (String) STR_INT.invokeExact(value);
}
```

`StringConcatFactory.makeConcatWithConstants`是公开API，可以用来动态生成字符串拼接的方法，除了编译器生成字节码调用，也可以直接调用。调用生成方法一次大约需要1微秒（千分之一毫秒）。



##### makeConcatWithConstants动态生成方法的代码

makeConcatWithConstants使用recipe ("value \1")动态生成的方法大致如下：

```java
import java.lang.StringConcatHelper;
import static java.lang.StringConcatHelper.mix;
import static java.lang.StringConcatHelper.newArray;
import static java.lang.StringConcatHelper.prepend;
import static java.lang.StringConcatHelper.newString;

public static String invokeStatic(String str, int value) throws Throwable {
    long lengthCoder = 0;
    lengthCoder = mix(lengthCoder, str);
    lengthCoder = mix(lengthCoder, value);
    byte[] bytes = newArray(lengthCoder);
    lengthCoder = prepend(lengthCoder, bytes, value);
    lengthCoder = prepend(lengthCoder, bytes, str);
    return newString(bytes, lengthCoder);
}
```



StringConcatHelper是：StringConcatFactory.makeConcatWithConstants实现用到的内部类。

```java

package java.lang;

class StringConcatHelper {
     static String newString(byte[] buf, long indexCoder) {
        // Use the private, non-copying constructor (unsafe!)
        if (indexCoder == LATIN1) {
            return new String(buf, String.LATIN1);
        } else if (indexCoder == UTF16) {
            return new String(buf, String.UTF16);
        }
    }
}


public class String {
    String(byte[] value, byte coder) {
        // 无拷贝构造
        this.value = value;
        this.coder = coder;
    }
}
```

可以看出，生成的方法是通过如下步骤来实现：

1. StringConcatHelper的mix方法计算长度和字符编码 (将长度和coder组合放到一个long中)；
2. 根据长度和编码构造一个byte[]；
3. 然后把相关的值写入到byte[]中；
4. 使用byte[]无拷贝的方式构造String对象。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202408291407245.webp)

上面的火焰图可以看到实现的细节。这样的实现，**和使用StringBuilder相比，减少了StringBuilder以及StringBuilder内部byte[]对象的分配，可以减轻GC的负担。也能避免可能产生的StringBuilder在latin1编码到UTF16时的数组拷贝**。



- StringBuilder缺省编码是LATIN1(ISO_8859_1)，如果append过程中遇到UTF16编码，会有一个将LATIN1转换为UTF16的动作，这个动作实现的方法是inflate。如果拼接的参数如果是带中文的字符串，使用StringBuilder还会多一次数组拷贝。

```java
class AbstractStringBuilder
    private void inflate() {
        if (!isLatin1()) {
            return;
        }
        byte[] buf = StringUTF16.newBytesFor(value.length);
        StringLatin1.inflate(value, 0, buf, 0, count);
        this.value = buf;
        this.coder = UTF16;
    }
}
```

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202408291408861.webp)



### @Deprecated注解的变化

该注解用于标识废弃的内容，在jdk9中新增了2个内容：

- String since() default “”：标识是从哪个版本开始废弃

- boolean forRemoval() default false：如果为true，标识该废弃的内容会在未来的某个版本中移除

  

### 弃用class.newInstance()

 

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251954524.gif)

官方说明：class.newInstance()方法传播由空构造函数引发的任何异常，包括已检查的异常。这种方法的使用有效地绕过了编译时异常检查，否则该检查将由编译器执行。

clazz.newInstance()方法由clazz.getDeclaredConstructor().newInstance()方法代替，该方法通过将构造函数抛出的任何异常包装在（InvocationTargetException）中来避免此问题。

也就是说使用class.newInstance()方法时由默认构造函数中抛出的异常，此方法检查不到，如下例子：

```java
public class ClassInstanceTest {

    public ClassInstanceTest() throws IOException {
        System.out.println("无参构造");
        throw new IOException();
    }

    public static void main(String[] args) {
        try {
            ClassInstanceTest classInstanceTest = ClassInstanceTest.class.newInstance();
        } catch (InstantiationException e) {
            System.out.println("InstantiationException");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            System.out.println("IllegalAccessException");
            e.printStackTrace();
        }
    }
}

输出：

无参构造
Exception in thread "main" java.io.IOException
    at ClassInstanceTest.<init>(ClassInstanceTest.java:9)
    at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
    at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)
    at java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
    at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
    at java.base/java.lang.reflect.ReflectAccess.newInstance(ReflectAccess.java:166)
    at java.base/jdk.internal.reflect.ReflectionFactory.newInstance(ReflectionFactory.java:404)
    at java.base/java.lang.Class.newInstance(Class.java:590)
    at ClassInstanceTest.main(ClassInstanceTest.java:14)
```

可以看到，没有被异常检查捕获到

 

接下来使用clazz.getDeclaredConstructor().newInstance()方法：

```java
public class ClassInstanceTest {

    public ClassInstanceTest() throws IOException {
        System.out.println("无参构造");
        throw new IOException();
    }

    public static void main(String[] args) {
        try {
            Class clazz = ClassInstanceTest.class;
            clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException e) {
            System.out.println("InstantiationException");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            System.out.println("IllegalAccessException");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            System.out.println("InvocationTargetException");
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            System.out.println("NoSuchMethodException");
            e.printStackTrace();
        }
    }
}

 输出：
 
 无参构造
InvocationTargetException
java.lang.reflect.InvocationTargetException
    at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
    at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)
    at java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
    at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
    at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
    at ClassInstanceTest.main(ClassInstanceTest.java:15)
Caused by: java.io.IOException
    at ClassInstanceTest.<init>(ClassInstanceTest.java:9)
    ... 6 more
```

可以看到异常被捕获到了

 

### 弃用finalized()方法

在 Java 中，finalize() 方法曾是对象生命周期的一部分，允许在垃圾收集器准备回收对象之前执行一些清理操作。然而，随着时间的推移，finalize() 方法逐渐暴露出一些问题，包括性能开销、不确定的行为、死锁风险以及增加了垃圾回收的复杂性。

#### 弃用原因

- 性能问题：该方法的执行会增加垃圾回收的停顿时间，因为 JVM 必须等待对象的 finalize() 方法执行完毕才能回收对象。这对于需要低延迟和高吞吐量的应用来说可能会产生性能问题

- 不确定性：执行时间是不确定的，因为它依赖于垃圾回收器的运行时机，而垃圾回收器的运行时机是不可预测的。这导致依赖 finalize() 进行资源清理的代码很难编写和调试

- 死锁风险：如果 finalize() 方法在执行过程中访问了其他对象，而这些对象又恰好正在被垃圾回收，那么就可能发生死锁

- 安全问题：可以被恶意代码利用来破坏系统的安全性。例如，恶意代码可以覆盖对象的 finalize() 方法，在对象被垃圾回收时执行恶意操作

因此，基于以上原因，它在 Java 9 中被标记为弃用（deprecated）

#### 替代方案

##### try-catch-resources

try-catch-resources 自动关闭实现了 AutoCloseable 接口的资源。这是一个优雅且安全的方式来管理资源，如文件、网络连接。

InputStream 实现了 AutoCloseable 接口，因此它可以在 try 结束时自动关闭，无需显式调用 close() 方法，源码如下：

```java
public abstract class InputStream implements Closeable {
    // 只展示结构，具体内容省略
}

public interface Closeable extends AutoCloseable {

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     *
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException;
}
```



##### Cleaner 

Java 9 引入了 Cleaner 类作为替代方案。允许注册一个回调，当对象变得幻象可达（phantom reachable）时，这个回调会被执行。与 finalize() 不同，Cleaner 不会延迟对象的回收，并且它的执行是异步的。

demo如下：

```java
import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CleanerDemo {

    // 模拟需要清理资源的类
    static class ManagedResource {
        private final AtomicBoolean isClosed = new AtomicBoolean(false);

        public void close() {
            if (isClosed.compareAndSet(false, true)) {
                System.out.println("资源已被清理。");
            }
        }

        public boolean isClosed() {
            return isClosed.get();
        }
    }

    // 模拟持有需要清理资源的类，并使用 Cleaner 进行清理
    static class ResourceHolder implements AutoCloseable {
        private final ManagedResource resource = new ManagedResource();
        private final Cleanable cleanable;

        public ResourceHolder() {
            this.cleanable = Cleaner.create().register(Executors.defaultThreadFactory(), () -> {
                System.out.println("Cleaner 正在执行清理...");
                resource.close();
            });
        }

        @Override
        public void close() {
            // 手动触发清理
            cleanable.clean(); 
        }

        public boolean isResourceClosed() {
            return resource.isClosed();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ResourceHolder holder = new ResourceHolder();

        // 这里是使用资源逻辑

        // 模拟应用程序逻辑结束，释放资源
        holder.close();

        // 检查资源是否已被清理
        System.out.println("资源是否已关闭: " + holder.isResourceClosed());

        // 强制进行垃圾回收以演示 Cleaner 的工作
        System.gc();
        // 等待一段时间以便 Cleaner 有机会执行（这只是示例，实际执行时间不确定）
        Thread.sleep(1000); 
    }
}

//输出
Cleaner 正在执行清理...
资源已被清理。
资源是否已关闭: true
```

在上述例子中，手动调用了 holder.close()，因此 Cleaner 的回调实际上不会被触发，因为资源已经在 Cleaner 有机会介入之前被清理了。如果注释掉 holder.close() 的调用，那么当 holder 对象变得幻象可达时，Cleaner 的回调可能会被触发（但这仍是不确定的）

因此使用Cleaner 进行资源管理一般也不是最佳实践。Cleaner 主要用于在对象被垃圾回收时执行一些额外的、不是很关键的清理任务

## Java 10新特性

Java 10 于 2018年3月发布，这是Java 采用新发布周期后的第一个版本。虽然特性不多，但引入了备受关注的 var 关键字，让 Java 代码可以更加简洁。

### 局部变量类型推断

在jdk10以前声明变量的时候，会像下面这样：

```java
String oldName = "jack";
int oldAge = 10;
long oldMoney = 88888888L;
Object oldObj = new Object();
```

上面声明的时候使用了4种不同类型的变量，在jdk10中前面的类型都可以使用var来代替，JVM会自动推断该变量是什么类型的，例如可以这样写：

```java
var newName = "jack";
var newAge = 10;
var newMoney = 88888888L;
var newObj = new Object();
```



注意：

当然这个var的使用是有限制的，仅适用于

- 局部变量
- 增强for循环的索引
- 普通for循环的本地变量
- 不能使用于方法形参，构造方法形参，方法返回类型等。

### 应用程序类数据共享

Java 10 扩展了类数据共享功能，允许应用程序类也参与共享(Application Class-Data Sharing)。在此之前，只有JDK 核心类可以进行类数据共享，应用程序类每次启动都需要重新加载和解析。

类数据共享的核心思路是：将JDK核心类和应用程序类的元数据都打包到共享归档文件中，多个 JVM 实例同时映射同一个归档文件，通过 共享读取 优化应用启动时间和减少内存占用。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202510292243086.png)


使用步骤:
1. 生成类列表：

```java
java -XX:DumpLoadedClassList=classes.lst MyApp
```

2. 创建归档文件：

```java
java -Xshare:dump -XX:SharedClassListFile=classes.lst -XX:SharedArchiveFile=myapp.jsa
```

3. 使用归档文件启动：

```java
java -Xshare:on -XX:SharedArchiveFile=myapp.jsa MyApp
```

这个特性主要用于容器化部署和微服务场景，可以显著减少启动时间。

### 垃圾收集器接口

Java 10 引入了统一的垃圾收集器接口，为不同的 GC实现提供了标准化的框架。这个改进主要是为了:
- 简化新 GC 算法的开发和集成
- 提高代码的可维护性
- 支持更灵活的 GC 配置

这个特性对普通开发者来说是透明的，主要影响 JVM 内部实现。

### G1的并行完整GC

在 Java 10 之前，G1 垃圾收集器的 Full GC 是单线程的，当堆内存不足时性能会很差。Java 10 改进了 G1 的 Full GC 实现，使用并行算法:

```java
# 启用 G1 垃圾收集器  
java -XX:+UseG1GC -XX:MaxGCPauseMillis=200 MyApp
```

并行 Ful GC 可以显著减少长时间的停顿，提升应用的响应性。

### 线程局部握手

线程局部握手(Thread-Local Handshakes)是一个 JVM 内部优化，允许在不停止所有线程的情况下对单个线程执行回调：
- 减少全局安全点的使用
- 降低 JVM 操作的延迟.
- 提高整体性能

这个特性对开发者透明，但可以改善JVM 的整体性能表现。

### 在备用内存设备上进行堆分配

Java 10 支持将 JMM 堆分配到非易失性内存(NVM)或其他备用内存设备上：

```java
# 使用备用内存设备  
java -XX:AllocateHeapAt=/mnt/pmem MyApp
```

这个特性主要用于：
- 利用大容量持久内存
- 减少应用重启时的数据重建时间
- 支持更大的堆内存配置

### 根证书

Java 10 在默认的 TrustStore 中包含了一组根证书颁发机构(CA)证书，解决了 OpenJDK构建版本中缺少根证书的问题。

这意味着 OpenJDK 现在可以直接支持 TLS 连接，无需额外配置证书：

```java
// 现在可以直接使用 HTTPS 连接  
URL url = new URL("https://www.example.com");  
HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();  
// 无需额外的证书配置
```



### Optional 增强

Optional 类新增了 orElseThrow()方法，作为 get()的更好替代：

```java
Optional<String> optional = Optional.of("Hello");  
  
// Java 10 新增的方法  
String value1 = optional.orElseThrow(); // 等同于 get()，但语义更清晰  
  
// 相比传统的 get() 方法，orElseThrow() 语义更明确  
String value2 = optional.get(); // 不推荐，语义不清
```


### Collection 增强


Collection 接口新增了 copy0f()方法，可以创建不可变的副本

```java
List<String> original = new ArrayList<>();  
original.add("A");  
original.add("B");  
  
// 创建不可变副本  
List<String> copy = List.copyOf(original);  
// copy.add("C"); // 会抛出 UnsupportedOperationException  
  
Set<Integer> originalSet = new HashSet<>();  
originalSet.add(1);  
originalSet.add(2);  
  
Set<Integer> copySet = Set.copyOf(originalSet);
```

### Collectors 增强

Collectors 类新增了 toUnmodifiableList()、toUnmodifiableSet()和 toUnmodifiableMap()方法:

```java
List<String> names = List.of("张三", "李四", "王五");  
  
// 收集到不可变列表  
List<String> upperNames = names.stream()  
    .map(String::toUpperCase)  
    .collect(Collectors.toUnmodifiableList());  
  
// 收集到不可变集合  
Set<Integer> lengths = names.stream()  
    .map(String::length)  
    .collect(Collectors.toUnmodifiableSet());  
  
// 收集到不可变映射  
Map<String, Integer> nameLength = names.stream()  
    .collect(Collectors.toUnmodifiableMap(  
        name -> name,  
        String::length  
    ));
```













 <!-- @include: @article-footer.snippet.md -->     

 