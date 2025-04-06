---
title: 调试排错 - 排查工具单
category: Java
tag:
 - JVM
head:
  - - meta
    - name: keywords
      content: Java,JVM,排查工具
  - - meta
    - name: description
      content: 全网最全的Java JVM知识点总结，让天下没有难学的八股文！
---





## Java 调试入门工具

### jps

> jps是jdk提供的一个查看当前java进程的小工具， 可以看做是JavaVirtual Machine Process Status Tool的缩写。

jps常用命令

```java
jps # 显示进程的ID 和 类的名称
jps –l # 输出输出完全的包名，应用主类名，jar的完全路径名 
jps –v # 输出jvm参数
jps –q # 显示java进程号
jps -m # main 方法
jps -l xxx.xxx.xx.xx # 远程查看 
```



jps参数

```java
-q：仅输出VM标识符，不包括classname,jar name,arguments in main method 
-m：输出main method的参数 
-l：输出完全的包名，应用主类名，jar的完全路径名 
-v：输出jvm参数 
-V：输出通过flag文件传递到JVM中的参数(.hotspotrc文件或-XX:Flags=所指定的文件 
-Joption：传递参数到vm,例如:-J-Xms512m
```



jps原理

> java程序在启动以后，会在java.io.tmpdir指定的目录下，就是临时文件夹里，生成一个类似于hsperfdata_User的文件夹，这个文件夹里（在Linux中为/tmp/hsperfdata_{userName}/），有几个文件，名字就是java进程的pid，因此列出当前运行的java进程，只是把这个目录里的文件名列一下而已。 至于系统的参数什么，就可以解析这几个文件获得。

更多请参考 [jps - Java Virtual Machine Process Status Tool](https://docs.oracle.com/javase/1.5.0/docs/tooldocs/share/jps.html)

### jstack

> jstack是jdk自带的线程堆栈分析工具，使用该命令可以查看或导出 Java 应用程序中线程堆栈信息。线程快照是当前虚拟机内每一条线程上在执行的方法堆栈的集合，生成线程快照的主要目的是定位线程出现长时间停顿的原因，如线程间死锁、死循环、 请求外部资源导致的长时间等待等问题

注意：

- Jstack 可以直接检测死锁；
- Jstack  **并不能直接检测死循环**，但可以通过分析线程堆栈信息间接发现死循环的存在；例如
  - 线程一直处于 `RUNNABLE` 状态。
  - CPU 使用率异常高（可以通过 `top` 或 `pidstat` 等工具查看）。
  - 线程的堆栈信息中会反复出现相同的函数调用。



jstack常用命令:

```java
# 基本 jstack pid
jstack 2815

# java和native c/c++框架的所有栈信息
jstack -m 2815

# 额外的锁信息列表，查看是否死锁
jstack -l 2815
```

pid 是需要被打印配置信息的java进程id，可以用jps查询



jstack参数：

```java
-l 长列表. 打印关于锁的附加信息,例如属于java.util.concurrent 的 ownable synchronizers列表.

-F 当’jstack [-l] pid’没有相应的时候强制打印栈信息

-m 打印java和native c/c++框架的所有栈信息.

-h | -help 打印帮助信息
```



#### Jstack 使用

通过使用 jps 命令获取需要监控的进程的pid，然后使用 jstack pid 命令查看线程的堆栈信息。



通过 jstack 命令可以获取当前进程的所有线程信息。

每个线程堆中信息中，都可以查看到 线程ID、线程的状态（wait、sleep、running 等状态）、是否持有锁信息等。



#### 死锁示例

下面通过一个例子，来演示 jstack 检查死锁的一个例子，代码如下：

```java
public static void deathLock() {
    Thread t1 = new Thread() {
        @Override
        public void run() {
            try {
                lock1.lock();
                TimeUnit.SECONDS.sleep(1);
                lock2.lock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };
    Thread t2 = new Thread() {
        @Override
        public void run() {
            try {
                lock2.lock();
                TimeUnit.SECONDS.sleep(1);
                lock1.lock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };
    t1.setName("mythread1");
    t2.setName("mythread2");
    t1.start();
    t2.start();
}
```

使用 jstack -l pid 查看线程堆栈信息，发现在堆栈信息最后面检查出了一个死锁。如下图

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202504052153793.png)

可以清楚的看出 mythread2 等待 这个锁 “0x00000000d6eb82d0”，这个锁是由于mythread1线程持有。

mythread1线程等待这个锁“0x00000000d6eb8300”,这个锁是由mythread2线程持有。

“mythread1”线程堆栈信息如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202504052153420.png)

可以看出当前线程持有“0x00000000d6eb82d0”锁，等待“0x00000000d6eb8300”的锁

“mythread2”线程堆栈信息如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202504052154711.png)

“mythread2”的堆栈信息中可以看出当前线程持有“0x00000000d6eb8300”锁，等待“0x00000000d6eb82d0”的锁。



### jinfo

> jinfo 是 JDK 自带的命令，可以用来查看正在运行的 java 应用程序的扩展参数，包括Java System属性和JVM命令行参数；也可以动态的修改正在运行的 JVM 一些参数。当系统崩溃时，jinfo可以从core文件里面知道崩溃的Java应用程序的配置信息
>
> Javacore，也可以称为“threaddump”或是“javadump”，它是 Java 提供的一种诊断特性，能够提供一份可读的当前运行的 JVM 中线程使用情况的快照。即在某个特定时刻，JVM 中有哪些线程在运行，每个线程执行到哪一个类，哪一个方法。应用程序如果出现不可恢复的错误或是内存泄露，就会自动触发 Javacore 的生成。



jinfo常用命令:

```java
# 输出当前 jvm 进程的全部参数和系统属性
jinfo 2815

# 输出所有的参数
jinfo -flags 2815

# 查看指定的 jvm 参数的值
jinfo -flag PrintGC 2815

# 开启/关闭指定的JVM参数
jinfo -flag +PrintGC 2815

# 设置flag的参数
jinfo -flag name=value 2815

# 输出当前 jvm 进行的全部的系统属性
jinfo -sysprops 2815
```



jinfo参数：

```java
no option 输出全部的参数和系统属性
-flag name 输出对应名称的参数
-flag [+|-]name 开启或者关闭对应名称的参数
-flag name=value 设定对应名称的参数
-flags 输出全部的参数
-sysprops 输出系统属性
```



#### 示例一： no option

命令：jinfo pid
描述：输出当前 jvm 进程的全部参数和系统属性

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202504052213655.png)

#### 示例二： -flag name

命令：jinfo -flag name pid
 描述：输出对应名称的参数

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202504052213657.png)

使用该命令，可以查看指定的 jvm 参数的值。如：查看当前 jvm 进程是否开启打印 GC 日志。

#### 示例三：-flag [+|-]name

命令：jinfo -flag [+|-]name   pid
 描述：开启或者关闭对应名称的参数

使用 jinfo 可以在不重启虚拟机的情况下，可以动态的修改 jvm 的参数。尤其在线上的环境特别有用。

使用如下：



![img](https:////upload-images.jianshu.io/upload_images/2843224-45c81fe69caa36f9.png?imageMogr2/auto-orient/strip|imageView2/2/w/348/format/webp)

#### 示例四：-flag name=value

命令：jinfo -flag  name=value  pid
 描述：修改指定参数的值。

同示例三，但示例三主要是针对 boolean 值的参数设置的。
 如果是设置 value值，则需要使用 name=value  的形式。

使用如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202504052214955.png)

> 注意事项 ：jinfo虽然可以在java程序运行时动态地修改虚拟机参数，但并不是所有的参数都支持动态修改

#### 示例五： -flags

命令：jinfo -flags pid
 描述：输出全部的参数

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202504052214461.png)

#### 示例六：-sysprops

命令：jinfo -sysprops pid
描述：输出当前 jvm 进行的全部的系统属性

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202504052216083.png)





### jmap

> 命令jmap是一个多功能的命令。它可以生成 java 程序的 dump 文件， 也可以查看堆内对象示例的统计信息、查看 ClassLoader 的信息以及 finalizer 队列。

**注意**:此命令会导致虚拟机暂停工作1~3秒

两个用途

```java
# 查看堆的对象分配情况
jmap -heap 2815

# dump
jmap -dump:live,format=b,file=/tmp/heap2.bin 2815
jmap -dump:format=b,file=/tmp/heap3.bin 2815

# 查看堆的占用
jmap -histo 2815 | head -10
```



jmap参数

```java
no option： 查看进程的内存映像信息,类似 Solaris pmap 命令。
heap： 显示Java堆详细信息
histo[:live]： 显示堆中对象的统计信息
clstats：打印类加载器信息
finalizerinfo： 显示在F-Queue队列等待Finalizer线程执行finalizer方法的对象
dump:<dump-options>：生成堆转储快照
F： 当-dump没有响应时，使用-dump或者-histo参数. 在这个模式下,live子参数无效.
help：打印帮助信息
J<flag>：指定传递给运行jmap的JVM的参数
```

#### 示例一：no option

命令：jmap pid
 描述：查看进程的内存映像信息,类似 Solaris pmap 命令。

使用不带选项参数的jmap打印共享对象映射，将会打印目标虚拟机中加载的每个共享对象的起始地址、映射大小以及共享对象文件的路径全称。这与Solaris的pmap工具比较相似。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202504061341538.png)

#### 示例二：heap

命令：jmap -heap pid
 描述：显示Java堆详细信息

打印一个堆的摘要信息，包括使用的GC算法、堆配置信息和各内存区域内存使用信息

```text
C:\Users\jjs>jmap -heap 5932
Attaching to process ID 5932, please wait...
Debugger attached successfully.
Server compiler detected.
JVM version is 25.91-b15

using thread-local object allocation.
Parallel GC with 4 thread(s)

Heap Configuration:
   MinHeapFreeRatio         = 0
   MaxHeapFreeRatio         = 100
   MaxHeapSize              = 1073741824 (1024.0MB)
   NewSize                  = 42991616 (41.0MB)
   MaxNewSize               = 357564416 (341.0MB)
   OldSize                  = 87031808 (83.0MB)
   NewRatio                 = 2
   SurvivorRatio            = 8
   MetaspaceSize            = 21807104 (20.796875MB)
   CompressedClassSpaceSize = 1073741824 (1024.0MB)
   MaxMetaspaceSize         = 17592186044415 MB
   G1HeapRegionSize         = 0 (0.0MB)

Heap Usage:
PS Young Generation
Eden Space:
   capacity = 60293120 (57.5MB)
   used     = 44166744 (42.120689392089844MB)
   free     = 16126376 (15.379310607910156MB)
   73.25337285580842% used
From Space:
   capacity = 5242880 (5.0MB)
   used     = 0 (0.0MB)
   free     = 5242880 (5.0MB)
   0.0% used
To Space:
   capacity = 14680064 (14.0MB)
   used     = 0 (0.0MB)
   free     = 14680064 (14.0MB)
   0.0% used
PS Old Generation
   capacity = 120061952 (114.5MB)
   used     = 19805592 (18.888084411621094MB)
   free     = 100256360 (95.6119155883789MB)
   16.496143590935453% used

20342 interned Strings occupying 1863208 bytes.
```

#### 示例三：histo[:live]

命令：jmap -histo:live pid
 描述：显示堆中对象的统计信息

其中包括每个Java类、对象数量、内存大小(单位：字节)、完全限定的类名。打印的虚拟机内部的类名称将会带有一个’*’前缀。如果指定了live子选项，则只计算活动的对象。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202504061342938.png)

#### 示例四：clstats

命令：jmap -clstats pid
 描述：打印类加载器信息

-clstats是-permstat的替代方案，在JDK8之前，-permstat用来打印类加载器的数据
 打印Java堆内存的永久保存区域的类加载器的智能统计信息。对于每个类加载器而言，它的名称、活跃度、地址、父类加载器、它所加载的类的数量和大小都会被打印。此外，包含的字符串数量和大小也会被打印。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202504061342907.png)



#### 示例五：finalizerinfo

命令：jmap -finalizerinfo pid
 描述：打印等待终结的对象信息

![image-20250406134257131](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202504061342234.png)

Number of objects pending for finalization: 0 说明当前F-QUEUE队列中并没有等待Fializer线程执行final

#### 示例六：`dump:<dump-options>`

命令：jmap -dump:format=b,file=heapdump.phrof pid
 描述：生成堆转储快照dump文件。

以hprof二进制格式转储Java堆到指定filename的文件中。live子选项是可选的。如果指定了live子选项，堆中只有活动的对象会被转储。想要浏览heap dump，你可以使用jhat(Java堆分析工具)读取生成的文件。

> 这个命令执行，JVM会将整个heap的信息dump写入到一个文件，heap如果比较大的话，就会导致这个过程比较耗时，并且执行的过程中为了保证dump的信息是可靠的，所以会暂停应用， 线上系统慎用。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202504061343586.png)



更多请参考： [jmap - Memory Map](https://docs.oracle.com/javase/1.5.0/docs/tooldocs/share/jmap.html)


### jstat

它是 JDK 自带的工县，用于监控JVM 各种运行时信息

jstat参数众多，但是使用一个就够了

```java
jstat -gc <pid> 1000 10
```

- gc选项：显示垃圾收集信息(也可以用 gcutil，gcutil以百分比形式显示内存的使用情况，gc显示的是内存占用的字节数，以KB 的形式输出堆内存的使用情况)
- pid：Java 进程的 PID。
- 1000：每 1000 毫秒采样一次。
- 10：采样 10 次。

示例输出

```java
 S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC       MU       CCSC    CCSU     YGC     YGCT     FGC    FGCT     GCT
1536.0 1536.0  0.0    0.0    30720.0   1024.0  708608.0    2048.0   44800.0  43712.6   4864.0  4096.0      4    0.072   1      0.015    0.087
1536.0 1536.0  0.0    0.0    30720.0   2048.0  708608.0    2048.0   44800.0  43712.6   4864.0  4096.0      4    0.072   1      0.015    0.087
1536.0 1536.0  0.0    0.0    30720.0   3072.0  708608.0    2048.0   44800.0  43712.6   4864.0  4096.0      4    0.072   1      0.015    0.087
```

字段含义：

- S0C(Survivor Space 0 Capacity):第一个 Survivor 区域的容量(字节数).
- S1C(Survivor Space 1 Capacity):第二个 Survivor 区域的容量(字节数)。
- S0U(Survivor Space 0 Utilization):第一个 Survivor 区域的使用量(字节数)
- S1U(Survivor Space 1 Utilization):第二个 Survivor 区域的使用量(字节数)。
- EC(Eden Space Capacity): Eden 区域的容量(字节数)。
- EU(Eden Space Utilization): Eden 区域的使用量(字节数)
- OC(Old Generation Capacity): 老年代的容量(字节数)
- OU(Old Generation Utilization): 老年代的使用量(字节数)
- MC(Metaspace Capacity):方法区(Metaspace)的容量(字节数)
- MU (Metaspace Utilization):方法区的使用量(字节数)。
- CCSC(Compressed Class Space Capacity): 压缩类空间的容量(字节数)
- CCSU(Compressed Class Space Utilization): 压缩类空间的使用量(字节数)
- YGC (Young Generation GC Count):年轻代垃圾回收的次数
- YGCT (Young Generation GC Time):年轻代垃圾回收的总时间(秒)。
- FGC (Full GC Count): full gc 的次数。
- FGCT(Full GC Time): full gc 的总时间(秒)。
- GCT(Garbage Collection Time): 总的垃圾回收时间(秒)。

注意:如果 FGC 变化频率很高，则说明系统性能和吞吐量将下降，或者可能出现内存溢出。

### jdb

jdb可以用来预发debug,假设你预发的java_home是/opt/java/，远程调试端口是8000.那么

```java
jdb -attach 8000
```

出现以上代表jdb启动成功。后续可以进行设置断点进行调试。

具体参数可见oracle官方说明[jdb - The Java Debugger](http://docs.oracle.com/javase/7/docs/technotes/tools/windows/jdb.html)



### CHLSDB

CHLSDB感觉很多情况下可以看到更好玩的东西，不详细叙述了。 查询资料听说jstack和jmap等工具就是基于它的。

```java
java -classpath /opt/taobao/java/lib/sa-jdi.jar sun.jvm.hotspot.CLHSDB
```

更详细的可见[R大此贴](http://rednaxelafx.iteye.com/blog/1847971)




## Java 调试进阶工具

### btrace

首当其冲的要说的是btrace。真是生产环境&预发的排查问题大杀器。 简介什么的就不说了。直接上代码干

l 查看当前谁调用了ArrayList的add方法，同时只打印当前ArrayList的size大于500的线程调用栈

```java
@OnMethod(clazz = "java.util.ArrayList", method="add", location = @Location(value = Kind.CALL, clazz = "/./", method = "/./"))
public static void m(@ProbeClassName String probeClass, @ProbeMethodName String probeMethod, @TargetInstance Object instance, @TargetMethodOrField String method) {

    if(getInt(field("java.util.ArrayList", "size"), instance) > 479){
        println("check who ArrayList.add method:" + probeClass + "#" + probeMethod  + ", method:" + method + ", size:" + getInt(field("java.util.ArrayList", "size"), instance));
        jstack();
        println();
        println("===========================");
        println();
    }
}
```

- 监控当前服务方法被调用时返回的值以及请求的参数

```java
@OnMethod(clazz = "com.taobao.sellerhome.transfer.biz.impl.C2CApplyerServiceImpl", method="nav", location = @Location(value = Kind.RETURN))
public static void mt(long userId, int current, int relation, String check, String redirectUrl, @Return AnyType result) {

    println("parameter# userId:" + userId + ", current:" + current + ", relation:" + relation + ", check:" + check + ", redirectUrl:" + redirectUrl + ", result:" + result);
}
```

btrace 具体可以参考这里：https://github.com/btraceio/btrace

注意:

- 经过观察，1.3.9的release输出不稳定，要多触发几次才能看到正确的结果

- 正则表达式匹配trace类时范围一定要控制，否则极有可能出现跑满CPU导致应用卡死的情况

- 由于是字节码注入的原理，想要应用恢复到正常情况，需要重启应用。



### Greys

Greys是@杜琨的大作吧。说几个挺棒的功能(部分功能和btrace重合):

- sc -df xxx: 输出当前类的详情,包括源码位置和classloader结构

- trace class method: 打印出当前方法调用的耗时情况，细分到每个方法, 对排查方法性能时很有帮助。



### Arthas

> Arthas是基于Greys。

输入 dashboard 命令，按回车 enter，会展示当前进程的信息，按 ctrl+c 可以中断执行

```shell
$ dashboard
ID     NAME                   GROUP          PRIORI STATE  %CPU    TIME   INTERRU DAEMON
17     pool-2-thread-1        system         5      WAITIN 67      0:0    false   false
27     Timer-for-arthas-dashb system         10     RUNNAB 32      0:0    false   true
11     AsyncAppender-Worker-a system         9      WAITIN 0       0:0    false   true
9      Attach Listener        system         9      RUNNAB 0       0:0    false   true
3      Finalizer              system         8      WAITIN 0       0:0    false   true
2      Reference Handler      system         10     WAITIN 0       0:0    false   true
4      Signal Dispatcher      system         9      RUNNAB 0       0:0    false   true
26     as-command-execute-dae system         10     TIMED_ 0       0:0    false   true
13     job-timeout            system         9      TIMED_ 0       0:0    false   true
1      main                   main           5      TIMED_ 0       0:0    false   false
14     nioEventLoopGroup-2-1  system         10     RUNNAB 0       0:0    false   false
18     nioEventLoopGroup-2-2  system         10     RUNNAB 0       0:0    false   false
23     nioEventLoopGroup-2-3  system         10     RUNNAB 0       0:0    false   false
15     nioEventLoopGroup-3-1  system         10     RUNNAB 0       0:0    false   false
Memory             used   total max    usage GC
heap               32M    155M  1820M  1.77% gc.ps_scavenge.count  4
ps_eden_space      14M    65M   672M   2.21% gc.ps_scavenge.time(m 166
ps_survivor_space  4M     5M    5M           s)
ps_old_gen         12M    85M   1365M  0.91% gc.ps_marksweep.count 0
nonheap            20M    23M   -1           gc.ps_marksweep.time( 0
code_cache         3M     5M    240M   1.32% ms)
Runtime
os.name                Mac OS X
os.version             10.13.4
java.version           1.8.0_162
java.home              /Library/Java/JavaVir
                       tualMachines/jdk1.8.0
                       _162.jdk/Contents/Hom
                       e/jre
```

字段含义:

heap: 堆内存的使用情况：

- used 32M: 当前堆内存使用 32MB
- otal 155M: 堆内存总量为 155MB.
- max1820M:堆内存最大量为1820MB.
- usage 1.77%: 堆内存使用百分比为 1.77%

ps_eden_space: 年轻代 Eden 区域的使用情况

- used 14M: 当前 Eden 区域使用 14MB
- total 65M: Eden 区域总量为 65MB.
- max 672M: Eden 区域最大量为 672MB.
- usage 2.21%: Eden 区域使用百分比为 2.21%。

ps_survivor_space: 年轻代 Survivor 区域的使用情况。

- used 4M: 当前 Survivor 区域使用 4MB.
- total 5M: Survivor 区域总量为 5MB。
- max 5M: Survivor 区域最大量为 5MB。

ps_old_gen: 老年代的使用情况,

- used 12M: 当前老年代使用12MB
- total 85M: 老年代总量为 85MB。
- max 1365M: 老年代最大量为1365MB.
- usage 0.91%: 老年代使用百分比为 0.91%

nonheap: 非堆内存的使用情况。

- used 20M: 当前非堆内存使用 20MB.
- total 23M: 非堆内存总量为 23MB。

code cache: 代码缓存区的使用情况

- used 3M: 当前代码缓存区使用 3MB.
- total 5M: 代码缓存区总量为 5MB。
- max 240M: 代码缓存区最大量为 240MB。
- usage 1.32%: 代码缓存区使用百分比为 1.32%。





### javOSize

就说一个功能:

- classes：通过修改了字节码，改变了类的内容，即时生效。 所以可以做到快速的在某个地方打个日志看看输出，缺点是对代码的侵入性太大。但是如果自己知道自己在干嘛，的确是不错的玩意儿。

其他功能Greys和btrace都能很轻易做的到，不说了。

更多请参考：[官网](http://www.javosize.com/)



### JProfiler

之前判断许多问题要通过JProfiler，但是现在Greys和btrace基本都能搞定了。再加上出问题的基本上都是生产环境(网络隔离)，所以基本不怎么使用了，但是还是要标记一下。

更多请参考：[官网](https://www.ej-technologies.com/products/jprofiler/overview.html)



## 其它工具

### dmesg

如果发现自己的java进程悄无声息的消失了，几乎没有留下任何线索，那么dmesg一发，很有可能有你想要的。

sudo dmesg|grep -i kill|less 去找关键字oom_killer。找到的结果类似如下:

```java
[6710782.021013] java invoked oom-killer: gfp_mask=0xd0, order=0, oom_adj=0, oom_scoe_adj=0
[6710782.070639] [<ffffffff81118898>] ? oom_kill_process+0x68/0x140 
[6710782.257588] Task in /LXC011175068174 killed as a result of limit of /LXC011175068174 
[6710784.698347] Memory cgroup out of memory: Kill process 215701 (java) score 854 or sacrifice child 
[6710784.707978] Killed process 215701, UID 679, (java) total-vm:11017300kB, anon-rss:7152432kB, file-rss:1232kB
```

以上表明，对应的java进程被系统的OOM Killer给干掉了，得分为854. 解释一下OOM killer（Out-Of-Memory killer），该机制会监控机器的内存资源消耗。当机器内存耗尽前，该机制会扫描所有的进程（按照一定规则计算，内存占用，时间等），挑选出得分最高的进程，然后杀死，从而保护机器。

dmesg日志时间转换公式: log实际时间=格林威治1970-01-01+(当前时间秒数-系统启动至今的秒数+dmesg打印的log时间)秒数：

date -d "1970-01-01 UTC echo "$(date +%s)-$(cat /proc/uptime|cut -f 1 -d' ')+12288812.926194"|bc seconds" 剩下的，就是看看为什么内存这么大，触发了OOM-Killer了。

## JVM可视化工具

### JConsole

> Jconsole （Java Monitoring and Management Console），JDK自带的基于JMX的可视化监视、管理工具。 官方文档可以参考这里在新窗口打开

- 找到jconsole工具

```java
jaotc        jcmd        jinfo        jshell        rmid
jar        jconsole(这里)    jjs        jstack        rmiregistry
jarsigner    jdb        jlink        jstat        serialver
java        jdeprscan    jmap        jstatd        unpack200
javac        jdeps        jmod        keytool
javadoc        jhsdb        jps        pack200
javap        jimage        jrunscript    rmic
```



-l 打开jconsole

选择

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730604.jpg)

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730584.jpg)

 

- 查看概述、内存、线程、类、VM概要、MBean

概述

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730625.jpg)

 

内存

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730600.jpg)

 

线程

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730594.jpg)

 

类

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730611.jpg)

 

VM概要

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730674.jpg)

 

MBean

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730674.jpg)

### Visual VM

VisualVM 是一款免费的，集成了多个 JDK 命令行工具的可视化工具，它能为您提供强大的分析能力，对 Java 应用程序做性能分析和调优。这些功能包括生成和分析海量数据、跟踪内存泄漏、监控垃圾回收器、执行内存和 CPU 分析，同时它还支持在 MBeans 上进行浏览和操作。

Overview

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730697.jpg)

 

Monitor

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730716.jpg)

 

线程

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730735.jpg)

 

Sampler

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730758.jpg)



### Visual GC

visual gc 是 visualvm 中的图形化查看 gc 状况的插件。官方文档可以[参考这里](https://www.oracle.com/java/technologies/visual-garbage-collection-monitoring-tool.html)

比如我在IDEA中使用visual GC 插件来看GC状况。

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730781.jpg)



### JProfile

> JProfiler 是一个商业的主要用于检查和跟踪系统（限于Java开发的）的性能的工具。JProfiler可以通过时时的监控系统的内存使用情况，随时监视垃圾回收，线程运行状况等手段，从而很好的监视JVM运行情况及其性能。

JProfiler 是一个全功能的Java剖析工具（profiler），专用于分析J2SE和J2EE应用程序。它把CPU、执行绪和内存的剖析组合在一个强大的应用中。 JProfiler可提供许多IDE整合和应用服务器整合用途。JProfiler直觉式的GUI让你可以找到效能瓶颈、抓出内存漏失(memory leaks)、并解决执行绪的问题。它让你得以对heap walker作资源回收器的root analysis，可以轻易找出内存漏失；heap快照（snapshot）模式让未被参照（reference）的对象、稍微被参照的对象、或在终结（finalization）队列的对象都会被移除；整合精灵以便剖析浏览器的Java外挂功能。

#### 核心组件

JProfiler 包含用于采集目标 JVM 分析数据的 JProfiler agent、用于可视化分析数据的 JProfiler UI、提供各种功能的命令行工具，它们之间的关系如下图所示。

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730819.jpg)

 

- JProfiler agent：

JProfiler agent 是一个本地库，它可以在 JVM 启动时通过参数-agentpath:&lt;path to native library>进行加载或者在程序运行时通过[JVM Attach 机制](http://lovestblog.cn/blog/2014/06/18/jvm-attach/)进行加载。Agent 被成功加载后，会设置 JVMTI 环境，监听虚拟机产生的事件，如类加载、线程创建等。例如，当它监听到类加载事件后，会给这些类注入用于执行度量操作的字节码。

 

- JProfiler UI：JProfiler UI 是一个可独立部署的组件，它通过 socket 和 agent 建立连接。这意味着不论目标 JVM 运行在本地还是远端，JProfiler UI 和 agent 间的通信机制都是一样的。

JProfiler UI 的主要功能是展示通过 agent 采集上来的分析数据，此外还可以通过它控制 agent 的采集行为，将快照保存至磁盘，展示保存的快照。

 

- 命令行工具：JProfiler 提供了一系列命令行工具以实现不同的功能。

1. jpcontroller - 用于控制 agent 的采集行为。它通过 agent 注册的 JProfiler MBean 向 agent 传递命令。
2. jpenable - 用于将 agent 加载到一个正在运行的 JVM 上。
3. jpdump - 用于获取正在运行的 JVM 的堆快照。
4. jpexport & jpcompare - 用于从保存的快照中提取数据并创建 HTML 报告。



#### 运行测试

**运行一个SpringBoot测试工程，选择attach到JVM**

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730841.jpg)

选择指定的进程

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730861.jpg)

 

**设置数据采集模式**

JProfier 提供两种数据采集模式 Sampling 和 Instrumentation。

- Sampling - 适合于不要求数据完全精确的场景。优点是对系统性能的影响较小，缺点是某些特性不支持（如方法级别的统计信息）。

- Instrumentation - 完整功能模式，统计信息也是精确的。缺点是如果需要分析的类比较多，对应用性能影响较大。为了降低影响，往往需要和 Filter 一起使用。

由于我们需要获取方法级别的统计信息，这里选择了 Instrumentation 模式。

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730881.jpg)

 

概览

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730903.jpg)

 

内存

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730934.jpg)

 

实时内存分布（类对象）

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730975.jpg)

 

dump 堆内存

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730998.jpg)

 

dump完会直接打开显示

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730019.jpg)

 

线程存储

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730043.jpg)

 

导出HTML报告

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730064.jpg)

 

CPU 调用树

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730087.jpg)

 

线程历史

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730120.jpg)

 

JEE & 探针

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730141.jpg)

 

MBeans

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730165.jpg)


### Eclipse Memory Analyzer (MAT)

> MAT 是一种快速且功能丰富的 Java 堆分析器，可帮助你发现内存泄漏并减少内存消耗。 MAT在的堆内存分析问题使用极为广泛，需要重点掌握。

可以在[这里](https://www.eclipse.org/mat/)下载， 官方文档可以看[这里](http://help.eclipse.org/latest/index.jsp?topic=/org.eclipse.mat.ui.help/welcome.html)

 

- Overview：包含内存分布，以及潜在的问题推测

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730184.jpg)

 

- Histogram：可以列出内存中的对象，对象的个数以及大小。

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730206.jpg)

具体需要重点理解如下两个概念，可参考[官网文档](http://help.eclipse.org/latest/index.jsp?topic=/org.eclipse.mat.ui.help/welcome.html)的解释

1. Shallow Heap ：一个对象内存的消耗大小，不包含对其他对象的引用
2. Retained Heap ：是shallow Heap的总和，也就是该对象被GC之后所能回收到内存的总和

 

- Dominator Tree：可以列出那个线程，以及线程下面的那些对象占用的空间。

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730229.jpg)

 

- Top consumers：通过图形列出最大的object。

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730549.jpg)

 

- Leak Suspects：自动分析潜在可能的泄漏。

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251730571.jpg)

 

### GCeasy

[GCeasy](https://gceasy.io/)，它是一个分析 GC 日志文件的在线网站，能根据上传的 GC 日志，以图表形式分析 GC 情况:

 直接在主页上传堆转储文件即可，可以得到 GC的分析结果

 

 

 


<!-- @include: @article-footer.snippet.md -->     





