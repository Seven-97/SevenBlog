---
title: Java 9新特性
category: Java
tag:
 - 版本新特性
---





Java 9 新增了很多特性，针对较为突出的便于理解的特性进行说明。除了下面罗列出的新特性之外还有一些其他的内容，这些内容有的不重要或者使用较少，所以没有罗列出来。

## 接口私有方法

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



## 改进的try with resource

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



## 不能使用下划线命名变量

下面语句在jdk9之前可以正常编译通过,但是在jdk9（含）之后编译报错，在后面的版本中会将下划线作为关键字来使用

```java
String _ = "seven97";
```



## String字符串的变化

写程序的时候会经常用到String字符串，在以前的版本中String内部使用了char数组存储，对于使用英语的人来说，一个英文字符用一个字节就能存储，使用char存储字符会浪费一半的内存空间，因此在jdk9中将String内部的char数组改成了byte数组，这样就节省了一半的内存占用。

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

 

### new String()底层源码

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



## @Deprecated注解的变化

该注解用于标识废弃的内容，在jdk9中新增了2个内容：

- String since() default “”：标识是从哪个版本开始废弃

- boolean forRemoval() default false：如果为true，标识该废弃的内容会在未来的某个版本中移除

  

## 模块化

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



## jshell

在一些编程语言中，例如：python，Ruby等，都提供了REPL（Read Eval Print Loop 简单的交互式编程环境）。jshell就是Java语言平台中的REPL。

有的时候只是想写一段简单的代码，例如HelloWorld，按照以前的方式，还需要自己创建Java文件，创建class，编写main方法，但实际上里面的代码其实就是一个打印语句，此时还是比较麻烦的。在jdk9中新增了jshell工具，可以快速的运行一些简单的代码。

从命令提示符里面输入jshell，进入到jshell之后输入：

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251954531.gif)

如果要退出jshell的话，输入/exit即可。

 

## 弃用class.newInstance()

 

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

 

## 弃用finalized()方法

在 Java 中，finalize() 方法曾是对象生命周期的一部分，允许在垃圾收集器准备回收对象之前执行一些清理操作。然而，随着时间的推移，finalize() 方法逐渐暴露出一些问题，包括性能开销、不确定的行为、死锁风险以及增加了垃圾回收的复杂性。

### 弃用原因

- 性能问题：该方法的执行会增加垃圾回收的停顿时间，因为 JVM 必须等待对象的 finalize() 方法执行完毕才能回收对象。这对于需要低延迟和高吞吐量的应用来说可能会产生性能问题

- 不确定性：执行时间是不确定的，因为它依赖于垃圾回收器的运行时机，而垃圾回收器的运行时机是不可预测的。这导致依赖 finalize() 进行资源清理的代码很难编写和调试

- 死锁风险：如果 finalize() 方法在执行过程中访问了其他对象，而这些对象又恰好正在被垃圾回收，那么就可能发生死锁

- 安全问题：可以被恶意代码利用来破坏系统的安全性。例如，恶意代码可以覆盖对象的 finalize() 方法，在对象被垃圾回收时执行恶意操作

因此，基于以上原因，它在 Java 9 中被标记为弃用（deprecated）

### 替代方案

#### try-catch-resources

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



#### Cleaner 

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

 

 

 

 