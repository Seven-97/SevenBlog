---
title: 异常：学会优雅处理错误的艺术
category: Java
tags:
  - Java基础
head:
  - - meta
    - name: keywords
      content: Java,异常,exception,java 异常,java exception
  - - meta
    - name: description
      content: 全网最全的Java知识点总结，让天下没有难学的八股文！
---





## 介绍

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250727540.gif)

### Throwable

Throwable 是 Java 语言中所有错误与异常的超类。

Throwable 包含两个子类：Error（错误）和 Exception（异常），它们通常用于指示发生了异常情况。

Throwable 包含了其线程创建时线程执行堆栈的快照，它提供了 printStackTrace() 等接口用于获取堆栈跟踪数据等信息。

#### Error（错误）

Error 类及其子类：程序中无法处理的错误，表示运行应用程序中出现了严重的错误。

此类错误一般表示代码运行时 JVM 出现问题。通常有

- Virtual MachineError（虚拟机运行错误）

- NoClassDefFoundError（类定义错误）

- OutOfMemoryError：内存不足错误

- StackOverflowError：栈溢出错误。

此类错误发生时，JVM 将终止线程。这些错误是不受检异常，非代码性错误。因此，当此类错误发生时，应用程序不应该去处理此类错误。按照Java惯例，我们是不应该实现任何新的Error子类的！

#### Exception（异常）

程序本身可以捕获并且可以处理的异常。Exception 这种异常又分为两类：运行时异常和编译时异常。

1. 运行时异常**（unchecked exceptions）**

   - 都是RuntimeException类及其子类异常，如NullPointerException(空指针异常)、IndexOutOfBoundsException(下标越界异常)等，这些异常是不检查异常，程序中可以选择捕获处理，也可以不处理。这些异常一般是由程序逻辑错误引起的，程序应该从逻辑角度尽可能避免这类异常的发生。

   - 运行时异常的特点是Java编译器不会检查它，也就是说，当程序中可能出现这类异常，即使没有用try-catch语句捕获它，也没有用throws子句声明抛出它，也会编译通过。

2. 非运行时异常**（checked exceptions，编译时异常）**
   - 是RuntimeException以外的异常，类型上都属于Exception类及其子类。从程序语法角度讲是必须进行处理的异常，如果不处理，程序就不能编译通过。如IOException、SQLException等以及用户自定义的Exception异常，一般情况下不自定义检查异常。



### 常见的异常

在Java中提供了一些异常用来描述经常发生的错误，对于这些异常，有的需要程序员进行捕获处理或声明抛出，有的是由Java虚拟机自动进行捕获处理。Java中常见的异常类:

- RuntimeException

  -  java.lang.ArrayIndexOutOfBoundsException 数组索引越界异常。当对数组的索引值为负数或大于等于数组大小时抛出。

  -  java.lang.ArithmeticException 算术条件异常。譬如：整数除零等。

  -  java.lang.NullPointerException 空指针异常。当应用试图在要求使用对象的地方使用了null时，抛出该异常。譬如：调用null对象的实例方法、访问null对象的属性、计算null对象的长度、使用throw语句抛出null等等

  -  java.lang.ClassNotFoundException 找不到类异常。当应用试图根据字符串形式的类名构造类，而在遍历CLASSPAH之后找不到对应名称的class文件时，抛出该异常。

  -  java.lang.NegativeArraySizeException 数组长度为负异常

  -  java.lang.ArrayStoreException 数组中包含不兼容的值抛出的异常

  -  java.lang.SecurityException 安全性异常

  -  java.lang.IllegalArgumentException 非法参数异常

- IOException

  - IOException：操作输入流和输出流时可能出现的异常。

  - EOFException 文件已结束异常

  - FileNotFoundException 文件未找到异常

- 其他

  - ClassCastException 类型转换异常类

  - ArrayStoreException 数组中包含不兼容的值抛出的异常

  - SQLException 操作数据库异常类

  - NoSuchFieldException 字段未找到异常

  - NoSuchMethodException 方法未找到抛出的异常

  - NumberFormatException 字符串转换为数字抛出的异常

  - StringIndexOutOfBoundsException 字符串索引超出范围抛出的异常

  - IllegalAccessException 不允许访问某类异常

  - InstantiationException 当应用程序试图使用Class类中的newInstance()方法创建一个类的实例，而指定的类对象无法被实例化时，抛出该异常



这些异常时Java内置的异常类，当然用户也可以根据业务自定义自己的异常类：

```java
public class MyException extends RuntimeException {
    // 无参构造器
    public MyException() {}

    // 带有详细信息的构造器
    public MyException(String message) {
        super(message);
    }

    // 带有引起此异常的原因的构造器
    public MyException(Throwable cause) {
        super(cause);
    }

    // 同时包含错误信息和原因的构造器
    public MyException(String message, Throwable cause) {
        super(message, cause);
    }
}

```





### 异常的处理方式

- try-catch：希望出现异常后程序继续运行，则在选中语句后，采用：

-  throw：在出现异常的条件下的方法体内直接throw出异常：执行throw则一定抛出了异常。可以理解为，在编程之前就预想到可能发生的异常，那么：
  ```java
  if（预想的异常情况出现）{    
      throw new 相应的异常()；//可以是自定义的异常
  } //还可以在括号内写上出现异常时的”输出语句“
  ```

  即：既要**发现**异常，又要**处理**异常。

  另外：这种具有针对性的声明只能抛出单个异常

- throws：与throw方法不同，throws跟在方法声明后面，扔出使用此方法可能发生（或者在定义可能出现异常的变量的当前类后面throws出异常）的异常。其只是发现异常，而不处理，交给方法的调用者来处理。并且一次可以抛出多个异常。



### 异常常用方法

1. **getMessage()**：返回关于发生的异常的详细信息，如有必要，还需要带上抛出异常时一些状态变量。
2. **printStackTrace()**：在标准错误流中输出异常堆栈。这个方法对于打印详细的错误信息非常有用，因为它显示了异常从发生到被捕获的代码执行路径，但是这个方法无论何时都不应该被调用。
3. **toString()**：返回一个简短的描述，包括：Throwable的非完全限定类名，然后是getMessage()的结果。
4. **getCause()**：返回造成此Throwable的原因，或者返回null如果原因不存在或者未知。
5. **getStackTrace()**：返回一个表示该Throwable堆栈跟踪的StackTraceElement数组。





## try-catch-finally

### 执行顺序

- 当try没有捕获到异常时：try语句块中的语句逐一被执行，程序将跳过catch语句块，执行finally语句块和其后的语句；

- 当try捕获到异常，catch语句块里没有处理此异常的情况：

  - 当try语句块里的某条语句出现异常时，而没有处理此异常的catch语句块时，此异常将会抛给JVM处理，finally语句块里的语句还是会被执行，但finally语句块后的语句不会被执行；

  - 当try捕获到异常，catch语句块里有处理此异常的情况：在try语句块中是按照顺序来执行的，当执行到某一条语句出现异常时，程序将跳到catch语句块，并与catch语句块逐一匹配，找到与之对应的处理程序，其他的catch语句块将不会被执行，而try语句块中，出现异常之后的语句也不会被执行，catch语句块执行完后，执行finally语句块里的语句，最后执行finally语句块后的语句；

执行流程图如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250727564.jpg)

也可以直接用try-finally，不使用catch，可用在不需要捕获异常的代码，可以保证资源在使用后被关闭。例如IO流中执行完相应操作后，关闭相应资源；使用Lock对象保证线程同步，通过finally可以保证锁会被释放；数据库连接代码时，关闭连接操作等等。



finally遇见如下情况不会执行：

- 在前面的代码中用了System.exit()退出程序。

- finally语句块中发生了异常。

- 程序所在的线程死亡。

- 关闭CPU。

### finally 经典异常处理代码题

#### 题目一

```java
public class Test {
    public static void main(String[] args) {
        System.out.println(test());
    }
    public static int test() {
        try {
            return 1;
        } catch (Exception e) {
            return 2;
        } finally {
            System.out.print("3");
        }
    }
}
//输出：
31
```

try、catch、finally 的基础用法，在 return 前会先执行 finally 语句块，所以会先输出 finally 里的 3，再输出 return 的 1。由于这里try中没有异常发生，因此catch中的return不会执行

#### 题目二

```java
public class Test {
    public static void main(String[] args) {
        System.out.println(test());
    }
    public static int test() {
        try {
            int i = 1/0;
            return 1;
        } catch (Exception e) {
            return 2;
        } finally {
            System.out.print("3");
        }
    }
}
//输出：
32
```

在 return 前会先执行 finally 语句块，所以会先输出 finally 里的 3，再输出 catch 中 return 的 2。由于这里try中有异常发生，因此try后续语句不会再执行

#### 题目三

```java
public class Test {
    public static void main(String[] args) {
        System.out.println(test());
    }
    public static int test() {
        try {
            return 2;
        } finally {
            return 3;
        }
    }
}
//输出：
3
```

try中的return前先执行 finally，结果 finally 直接 return 了，自然也就走不到 try 里面的 return 了。

#### 题目四

```java
public class Test {
    public static void main(String[] args) {
        System.out.println(test());
    }
    public static int test() {
        int i = 0;
        try {
            i = 2;
            return i;
        } finally {
            i = 3;
        }
    }
}
//输出：
2
```

在执行 finally 之前，JVM 会先将 i 的结果暂存起来，然后 finally 执行完毕后，会返回之前暂存的结果，而不是返回 i，所以即使 i 已经被修改为 3，最终返回的还是之前暂存起来的结果 2。

#### 总结

1. 无论try中是否有return，是否有异常，finally都一定会执行。
2. 如果try中有异常发生，会执行catch中的句子，try中异常后续的位置的语句不会再被执行
3. 当try与finally语句中均有return语句，会忽略try中return。
4. try中有return, 会先将值暂存，无论finally语句中对该值做什么处理，最终返回的都是try语句中的暂存值。

 

## try-with-resources语法糖

### 背景

每当有关闭资源的需求都会使用到try-finally这个语句，比如在使用锁的时候，无论是本地的可重入锁还是分布式锁都会有下面类似的结构代码，会在finally里面进行unlock，用于强制解锁：

```java
Lock lock = new ReentrantLock();
lock.lock();
try {
    // doSometing
} finally {
    lock.unlock();
}
```



或者使用java的文件流读取或者写入文件的时候，也会在finally中强制关闭文件流，防止资源泄漏。

```java
InputStream inputStream = new FileInputStream("file");
try {
    System.out.println(inputStream.read(new byte[4]));
} finally {
    inputStream.close();
}
```



其实乍一看 这样的写法应该没什么问题，但是如果出现了多个资源需要关闭我们应该怎么写呢？最常见的写法如下：

```java
InputStream inputStream = new FileInputStream("file");
OutputStream outStream = new FileOutputStream("file1");

try {
    System.out.println(inputStream.read(new byte[4]));
    outStream.write(new byte[4]);
} finally {
    inputStream.close();
    outStream.close();
}
```

在外面定义了两个资源，然后在finally里面依次对这两个资源进行关闭，那么这个哪里有问题呢？

问题其实在于如果在inputStream.close的时候抛出异常，那么outStream.close()就不会执行，这很明显不是想要的结果，所以后面就改成了下面这种多重嵌套的方式去写：

```java
InputStream inputStream = new FileInputStream("file");
try {
    System.out.println(inputStream.read(new byte[4]));
    try {
        OutputStream outStream = new FileOutputStream("file1");
        outStream.write(new byte[4]);
    } finally {
        outStream.close();
    }
} finally {
    inputStream.close();
}
```

在这种方式中即便是outStream.close()抛出了异常，但是依然会执行到inputStream.close(),因为他们是在不同的finally块，这个的确解决了问题，但是还有两个问题没有解决：

- 如果有不止两个资源，比如有十个资源，难道要写十个嵌套的语句吗？写完之后这个代码还能看吗？

- 如果在try里面出现异常，然后在finally里面又出现异常，就会导致异常覆盖，会导致finally里面的异常将try的异常覆盖了。

```java
public class CloseTest {

    public void close(){
        throw new RuntimeException("close");
    }

    public static void main(String[] args) {
        CloseTest closeTest = new CloseTest();
        try{
            throw new RuntimeException("doSomething");
        }finally {
            closeTest.close();
        }
    }

}
//输出结果：Exception in thread "main" java.lang.RuntimeException: close
```

上面这个代码，期望的是能抛出doSomething的这个异常，但是实际的数据结果却是close的异常，这和预期不符合。

### try-with-resources如何解决的

上面介绍了两个问题，于是在java7中引入了try-with-resources的语句，只要资源实现了AutoCloseable这个接口那就可以使用这个语句了,之前的文件流已经实现了这个接口，因此可以直接使用：

```java
try (InputStream inputStream = new FileInputStream("file"); 
    OutputStream outStream = new FileOutputStream("file1")) {
    System.out.println(inputStream.read(new byte[4]));
    outStream.write(new byte[4]);
}
```

所有的资源定义全部都在try后面的括号中进行定义，通过这种方式就可以解决上面所说的几个问题：

- 通过这样的方式，代码非常整洁，无论有多少个资源，都可以很简洁的去做。

- 异常覆盖问题，可以通过实验来看一下，将代码改写为如下：

```java
public class CloseTest implements AutoCloseable {

    @Override
    public void close(){
        System.out.println("close");
        throw new RuntimeException("close");
    }

    public static void main(String[] args) {
        try(CloseTest closeTest = new CloseTest();
            CloseTest closeTest1 = new CloseTest();){
            throw new RuntimeException("Something");
        }
    }

}
//输出结果为：
close
close
Exception in thread "main" java.lang.RuntimeException: Something
    at fudao.CloseTest.main(CloseTest.java:33)
    Suppressed: java.lang.RuntimeException: close
        at fudao.CloseTest.close(CloseTest.java:26)
        at fudao.CloseTest.main(CloseTest.java:34)
    Suppressed: java.lang.RuntimeException: close
        at fudao.CloseTest.close(CloseTest.java:26)
        at fudao.CloseTest.main(CloseTest.java:34)
```

在代码中定义了两个CloseTest，用来验证之前close出现异常是否会影响第二个，同时在close和try块里面都抛出不同的异常，可以看见结果，输出了两个close，证明虽然close抛出异常，但是两个close都会执行。然后输出了doSomething的异常，可以发现这里输出的就是try块里面所抛出的异常，并且close的异常以Suppressed的方式记录在异常的堆栈里面，通过这样的方式两种异常都能记录下来。

 

## 异常实现原理

### JVM处理异常的机制

Exception Table，称为异常表

#### try-catch

```java
public static void simpleTryCatch() {
   try {
       testNPE();
   } catch (Exception e) {
       e.printStackTrace();
   }
}
```



使用javap来分析这段代码

```java
//javap -c Main
public static void simpleTryCatch();
    Code:
       0: invokestatic  #3                  // Method testNPE:()V
       3: goto          11
       6: astore_0
       7: aload_0
       8: invokevirtual #5                  // Method java/lang/Exception.printStackTrace:()V
      11: return
    Exception table:
       from    to  target type
           0     3     6   Class java/lang/Exception
```

异常表中包含了一个或多个异常处理者(Exception Handler)的信息，这些信息包含如下

- from 可能发生异常的起始点

- to 可能发生异常的结束点

- target 上述from和to之前发生异常后的异常处理者的位置

- type 异常处理者处理的异常的类信息

当一个异常发生时，JVM处理异常的机制如下：

1. JVM会在当前出现异常的方法中，查找异常表，是否有合适的处理者来处理
2. 如果当前方法异常表不为空，并且异常符合处理者的from和to节点，并且type也匹配，则JVM调用位于target的调用者来处理。
3. 如果上一条未找到合理的处理者，则继续查找异常表中的剩余条目
4. 如果当前方法的异常表无法处理，则向上查找（弹栈处理）刚刚调用该方法的调用处，并重复上面的操作。
5. 如果所有的栈帧被弹出，仍然没有处理，则抛给当前的Thread，Thread则会终止。
6. 如果当前Thread为最后一个非守护线程，且未处理异常，则会导致JVM终止运行。



#### try-catch-finally

```java
public class TestCode {
    public int foo() {
        int x;
        try {
            x = 1;
            return x;
        } catch (Exception e) {
            x = 2;
            return x;
        } finally {
            x = 3;
        }
    }
}
```

同样使用javap分析一下代码

```java
public int foo();
    descriptor: ()I
    flags: ACC_PUBLIC
    Code:
      stack=1, locals=5, args_size=1
         0: iconst_1 //int型1入栈 -> 栈顶=1
         1: istore_1 // 将栈顶的int型数值存入第二个局部变量 -> 局部2 = 1
         2: iload_1 // 将第二个int型局部变量推送至栈顶 -> 栈顶=1
         3: istore_2 //!! 将栈顶int型数值存入第三个局部变量 -> 局部3 = 1
         
         4: iconst_3 // int型3入栈 -> 栈顶 = 3
         5: istore_1 // 将栈顶的int型数值存入第二个局部变量 -> 局部2 = 3
         6: iload_2 //!! 将第三个int型局部变量推送至栈顶 -> 栈顶=1
         7: ireturn // 从当前方法返回栈顶int数值 -> 1
         
         8: astore_2 // ->局部3=Exception
         9: iconst_2 // ->栈顶=2
        10: istore_1 // ->局部2=2
        11: iload_1 //->栈顶=2
        12: istore_3 //!! ->局部4=2
        
        13: iconst_3 // ->栈顶=3
        14: istore_1 // ->局部1=3
        15: iload_3 //!! ->栈顶=2
        16: ireturn // -> 2
        
        17: astore        4 //将栈顶引用型数值存入第五个局部变量=any
        19: iconst_3 //将int型数值3入栈 -> 栈顶3
        20: istore_1 //将栈顶第一个int数值存入第二个局部变量 -> 局部2=3
        21: aload         4 //将局部第五个局部变量(引用型)推送至栈顶
        23: athrow //将栈顶的异常抛出
      Exception table:
         from    to  target type
             0     4     8   Class java/lang/Exception //0到4行对应的异常，对应#8中储存的异常
             0     4    17   any //Exeption之外的其他异常
             8    13    17   any
            17    19    17   any
```

在字节码的4,5，以及13,14中执行的是同一个操作，就是将int型的3入操作数栈顶，并存入第二个局部变量。这正是源码在finally语句块中内容。也就是说，JVM在处理异常时，会在每个可能的分支都将finally语句重复执行一遍。

通过分析字节码，可以得出最后的运行结果是：

- 不发生异常时: return 1

- 发生异常时: return 2

- 发生非Exception及其子类的异常，抛出异常，不返回值



#### try-with-resources

try-with-resources语句其实是一种语法糖，通过编译之后又回到了开始说的嵌套的那种模式：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250727568.jpg)

可以发现try-with-resources被编译之后，又采取了嵌套的模式，但是和之前的嵌套有点不同，他close的时候都利用了catch去捕获了异常，然后添加到真正的异常中，整体逻辑比之前我们自己的嵌套要复杂一些。

 

### 异常耗时

下面的测试用例简单的测试了建立对象、建立异常对象、抛出并接住异常对象三者的耗时对比：

```java
public class ExceptionTest {  
  
    private int testTimes;  
  
    public ExceptionTest(int testTimes) {  
        this.testTimes = testTimes;  
    }  
  
    public void newObject() {  
        long l = System.nanoTime();  
        for (int i = 0; i < testTimes; i++) {  
            new Object();  
        }  
        System.out.println("建立对象：" + (System.nanoTime() - l));  
    }  
  
    public void newException() {  
        long l = System.nanoTime();  
        for (int i = 0; i < testTimes; i++) {  
            new Exception();  
        }  
        System.out.println("建立异常对象：" + (System.nanoTime() - l));  
    }  
  
    public void catchException() {  
        long l = System.nanoTime();  
        for (int i = 0; i < testTimes; i++) {  
            try {  
                throw new Exception();  
            } catch (Exception e) {  
            }  
        }  
        System.out.println("建立、抛出并接住异常对象：" + (System.nanoTime() - l));  
    }  
  
    public static void main(String[] args) {  
        ExceptionTest test = new ExceptionTest(10000);  
        test.newObject();  
        test.newException();  
        test.catchException();  
    }  
}  

//结果：
建立对象：575817  
建立异常对象：9589080  
建立、抛出并接住异常对象：47394475 
```

建立一个异常对象，是建立一个普通Object耗时的约20倍（实际上差距会比这个数字更大一些，因为循环也占用了时间，追求精确的读者可以再测一下空循环的耗时然后在对比前减掉这部分），而抛出、接住一个异常对象，所花费时间大约是建立异常对象的4倍。

为什么耗时？参考https://blog.csdn.net/zhangyiqian/article/details/83980484

### JVM 的 Fast Throw 优化

总的来说，异常处理对 **CPU 和存储**都有影响
- CPU：每次抛出异常时，JVM 需要生成完整的堆栈跟踪（stack trace），需遍历线程栈帧、获取方法名、类名、行号等信息，消耗 CPU 资源
- 异常对象本身和堆栈信息会占用堆内存，大量异常可能导致频繁 GC，甚至触发OOM

当然，JVM对大量相同异常也是有优化的，当同一异常在相同位置多次抛出（约数千次），JVM 会跳过堆栈生成逻辑，直接复用预分配的“空堆栈异常对象”。这种情况造成的副作用就是  高频异常的堆栈信息被隐去，日志中仅显示 `NullPointerException` 等基础信息，也就无法定位具体出现异常的代码行。这个就是JVM 的 Fast Throw 优化

如果要禁用 Fast Throw 优化，则在 JVM 启动参数中添加 `-XX:-OmitStackTraceInFastThrow`，强制保留完整堆栈信息


## try catch 应该在 for 循环里面还是外面？

try catch 放在 for循环 外面 和里面 ，如果出现异常，产生的效果是不一样的。

怎么用，就需要看好业务场景，去使用了。

### try  catch  在 for 循环 外面

代码示例 ：

```java
public static void tryOutside() {  
    try {  
        for (int count = 1; count <= 5; count++) {  
            if (count == 3) {  
                //故意制造一下异常  
                int num = 1 / 0;  
            } else {  
                System.out.println("count:" + count + " 业务正常执行");  
            }  
        }  
    } catch (Exception e) {  
        System.out.println("try catch  在for 外面的情形， 出现了异常，for循环显然被中断");  
    }  
}  
```

结果：

```java
count:1 业务正常执行
count:2 业务正常执行
try catch  在for 外面的情形， 出现了异常，for循环显然被中断
```

try  catch  在 for 循环 外面 的时候， 如果 for循环过程中出现了异常， 那么for循环会终止。

### try  catch  在 for 循环 里面

代码示例 ：

```java
public static void tryInside() {  
  
    for (int count = 1; count <= 5; count++) {  
        try {  
            if (count == 3) {  
                //故意制造一下异常  
                int num = 1 / 0;  
            } else {  
                System.out.println("count:" + count + " 业务正常执行");  
            }  
        } catch (Exception e) {  
            System.out.println("try catch  在for 里面的情形， 出现了异常，for循环显然继续执行");  
        }  
    }  
}  
```

结果：

```java
count:1 业务正常执行
count:2 业务正常执行
try catch  在for 里面的情形， 出现了异常，for循环显然继续执行
count:3 业务正常执行
count:4 业务正常执行
```

try  catch  在 for 循环 里面 的时候， 如果 for循环过程中出现了异常，异常被catch抓掉，不影响for循环 继续执行。



### 小结

其实就是看业务。需要出现异常就终止循环的，就放外头；

不需要终止循环，就放循环里。





 

<!-- @include: @article-footer.snippet.md -->      

 

 

 