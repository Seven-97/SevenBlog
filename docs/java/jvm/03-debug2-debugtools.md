---
title: 调试排错 - 排查工具单
category: Java
tag:
 - JVM
---





## 调试

### Java 调试入门工具

#### jps

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

#### jstack

> jstack是jdk自带的线程堆栈分析工具，使用该命令可以查看或导出 Java 应用程序中线程堆栈信息。

jstack常用命令:

```java
# 基本
jstack 2815

# java和native c/c++框架的所有栈信息
jstack -m 2815

# 额外的锁信息列表，查看是否死锁
jstack -l 2815
```



jstack参数：

```java
-l 长列表. 打印关于锁的附加信息,例如属于java.util.concurrent 的 ownable synchronizers列表.

-F 当’jstack [-l] pid’没有相应的时候强制打印栈信息

-m 打印java和native c/c++框架的所有栈信息.

-h | -help 打印帮助信息
```

更多请参考: [jvm 性能调优工具之 jstack](https://www.jianshu.com/p/025cb069cb69)



#### jinfo

> jinfo 是 JDK 自带的命令，可以用来查看正在运行的 java 应用程序的扩展参数，包括Java System属性和JVM命令行参数；也可以动态的修改正在运行的 JVM 一些参数。当系统崩溃时，jinfo可以从core文件里面知道崩溃的Java应用程序的配置信息

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

更多请参考：[jvm 性能调优工具之 jinfo](https://www.jianshu.com/p/8d8aef212b25)



#### jmap

> 命令jmap是一个多功能的命令。它可以生成 java 程序的 dump 文件， 也可以查看堆内对象示例的统计信息、查看 ClassLoader 的信息以及 finalizer 队列。

两个用途

```java
# 查看堆的情况
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

更多请参考：[jvm 性能调优工具之 jmap](https://www.jianshu.com/p/a4ad53179df3) 和 [jmap - Memory Map](https://docs.oracle.com/javase/1.5.0/docs/tooldocs/share/jmap.html)



#### jstat

jstat参数众多，但是使用一个就够了

```java
jstat -gcutil 2815 1000 
```



#### jdb

jdb可以用来预发debug,假设你预发的java_home是/opt/java/，远程调试端口是8000.那么

```java
jdb -attach 8000
```

出现以上代表jdb启动成功。后续可以进行设置断点进行调试。

具体参数可见oracle官方说明[jdb - The Java Debugger](http://docs.oracle.com/javase/7/docs/technotes/tools/windows/jdb.html)



#### CHLSDB

CHLSDB感觉很多情况下可以看到更好玩的东西，不详细叙述了。 查询资料听说jstack和jmap等工具就是基于它的。

```java
java -classpath /opt/taobao/java/lib/sa-jdi.jar sun.jvm.hotspot.CLHSDB
```

更详细的可见[R大此贴](http://rednaxelafx.iteye.com/blog/1847971)
 



### Java 调试进阶工具

#### btrace

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



#### Greys

Greys是@杜琨的大作吧。说几个挺棒的功能(部分功能和btrace重合):

- sc -df xxx: 输出当前类的详情,包括源码位置和classloader结构

- trace class method: 打印出当前方法调用的耗时情况，细分到每个方法, 对排查方法性能时很有帮助。



#### Arthas

> Arthas是基于Greys。

具体请参考：[03 调试排错-应用在线调试Arthas](note://WEB4a6158deaf7d35bcbb068d5991e446f7)



#### javOSize

就说一个功能:

- classes：通过修改了字节码，改变了类的内容，即时生效。 所以可以做到快速的在某个地方打个日志看看输出，缺点是对代码的侵入性太大。但是如果自己知道自己在干嘛，的确是不错的玩意儿。

其他功能Greys和btrace都能很轻易做的到，不说了。

更多请参考：[官网](http://www.javosize.com/)



#### JProfiler

之前判断许多问题要通过JProfiler，但是现在Greys和btrace基本都能搞定了。再加上出问题的基本上都是生产环境(网络隔离)，所以基本不怎么使用了，但是还是要标记一下。

更多请参考：[官网](https://www.ej-technologies.com/products/jprofiler/overview.html)



### 其它工具

#### dmesg

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

 

 

 

 

 

 

 

 

 

 