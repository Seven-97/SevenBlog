---
title: Java 11 新特性
category: Java
tags:
  - 版本新特性
head:
  - - meta
    - name: keywords
      content: Java,版本新特性,Java11
  - - meta
    - name: description
      content: 全网最全的Java 版本新特性知识点总结，让天下没有难学的八股文！
---

Java 11 是继 Java8之后的第二个 LTS 版本，于2018年9月发布，这个版本的重点是提供更好的开发体验和更强大的标准库功能，特别是在字符串处理、文件操作和 HTTP 客户端方面，增加了不少新方法。


## HTTP 客户端 API

HTTP 请求是后端开发常用的能力，之前我们只能基于内置的 HttpURLConnection 自己封装，或者使用 Apache HttpClient、OkHttp 第三方库。

```java
// 传统的 HttpURLConnection 使用方式  
URL url = new URL("https://www.seven97.top");  
HttpURLConnection connection = (HttpURLConnection) url.openConnection();  
connection.setRequestMethod("GET");  
connection.setRequestProperty("Accept", "application/json");  
  
int responseCode = connection.getResponseCode();  
BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));  
// 更多繁琐的代码...
```

Java 11 将 HTTP 客户端 API正式化，新的 HTTP 客户端提供了现代化的、支持 HTTP/2 和 WebSocket 的客户端实现，让网络编程变得简单。

```java
// 创建 HTTP 客户端  
HttpClient client = HttpClient.newBuilder()  
    .connectTimeout(Duration.ofSeconds(10))  
    .followRedirects(HttpClient.Redirect.NORMAL)  
    .build();  
  
// 构建 GET 请求  
HttpRequest getRequest = HttpRequest.newBuilder()  
    .uri(URI.create("https://www.seven97.top"))  
    .header("Accept", "application/json")  
    .header("User-Agent", "Java-HttpClient")  
    .timeout(Duration.ofSeconds(30))  
    .GET()  
    .build();  
  
// POST 请求  
HttpRequest postRequest = HttpRequest.newBuilder()  
    .uri(URI.create("https://www.seven97.top/users"))  
    .header("Content-Type", "application/json")  
    .POST(HttpRequest.BodyPublishers.ofString(jsonData))  
    .build();
```

支持发送同步和异步请求，能够轻松获取响应结果

```java
// 同步发送请求  
HttpResponse<String> response = client.send(getRequest,   
    HttpResponse.BodyHandlers.ofString());  
System.out.println("状态码: " + response.statusCode());  
System.out.println("响应头: " + response.headers().map());  
System.out.println("响应体: " + response.body());  
  
// 异步发送请求  
client.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString())  
    .thenApply(HttpResponse::body)  
    .thenAccept(System.out::println);
```

还支持自定义响应处理和 WebSocket 请求：

```java
// 自定义响应处理  
HttpResponse<String> customResponse = client.send(getRequest,   
    responseInfo -> {  
        if (responseInfo.statusCode() == 200) {  
            return HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);  
        } else {  
            return HttpResponse.BodySubscribers.discarding();  
        }  
    });  
  
// WebSocket 支持  
WebSocket webSocket = HttpClient.newHttpClient()  
    .newWebSocketBuilder()  
    .buildAsync(URI.create("ws://localhost:8080/websocket"), new WebSocket.Listener() {  
        @Override  
        public void onOpen(WebSocket webSocket) {  
            System.out.println("WebSocket 连接已打开");  
            webSocket.sendText("Hello WebSocket!", true);  
        }  
        @Override  
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {  
            System.out.println("收到消息: " + data);  
            return null;  
        }  
    })  
    .join();
```

上面这些代码都不用记，现在直接把接口文档甩给 AI，让它来帮你生成请求代码就好。

## String 类的新方法

Java 11 为 String 类添加了许多实用的方法，让字符串处理变得更加方便。我估计很多现在学 Java 的同学都已经区分不出来哪些是新增的方法、哪些是老方法了，反正能用就行~

1) 基本的字符串检查和处理：

```java
String text = "  Hello World  \n\n";  
String emptyText = "   ";  
String multiLine = "第一行\n第二行\n第三行";  
  
// isBlank() 检查字符串是否为空或只包含空白字符  
System.out.println(emptyText.isBlank());     // true  
System.out.println("hello".isBlank());       // false  
System.out.println("".isBlank());            // true
```

2) strip方法

可以去除首尾空格，与之前的trim的区别是还可以去除unicode编码的空白字符，例如：

```java
char c = '\u2000';//Unicdoe空白字符
String str = c + "abc" + c;
System.out.println(str.strip());
System.out.println(str.trim());

System.out.println(str.stripLeading());//去除前面的空格
System.out.println(str.stripTrailing());//去除后面的空格
```


3) lines() 方法，让多行字符串处理更简单：

```java
// 将字符串按行分割成 Stream  
multiLine.lines()  
    .map(line -> "处理: " + line)  
    .forEach(System.out::println);  
  
long lineCount = multiLine.lines().count();  
System.out.println("总行数: " + lineCount);
```

4) repeat方法：字符串重复的次数

```java
String str = "seven";
System.out.println(str.repeat(4));// 重复输出seven 4次
```

## Files 类的新方法

Java 11 为文件操作新增了更便捷的方法，不需要使用 FileReader/fileWriter 这种复杂的操作了。基本的文件读写操作，一个方法搞定：

```java
// 写入文件  
String content = "这是一个测试文件\n包含多行内容\n中文支持测试";  
Path tempFile = Files.writeString(  
    Paths.get("temp.txt"),   
    content,  
    StandardCharsets.UTF_8  
);  
  
// 读取文件  
String readContent = Files.readString(tempFile, StandardCharsets.UTF_8);  
System.out.println("读取的内容:\n" + readContent);
```

支持流式读取文件，适合文件较大的场景：

```java
try (Stream<String> lines = Files.lines(tempFile)) {  
    lines.filter(line -> !line.isBlank())  
         .map(String::trim)  
         .forEach(System.out::println);  
}
```

## Optional 的新方法

Java 11 为 Optional 类添加了 isEmpty()方法，和之前的 isPresent 正好相反，让空值检查更直观。

```java
Optional<String> optional1 = Optional.of("Hello");  
Optional<String> optional2 = Optional.empty();  
  
// 传统写法  
if (!optional2.isPresent()) {  
    System.out.println("没有值");  
}  
  
// Java 11 新写法，语义更清晰  
if (optional2.isEmpty()) {  
    System.out.println("没有值");  
}
```

## lambda表达式中的变量类型推断

jdk11中允许在lambda表达式的参数中使用var修饰。用处不大，因为大多数情况写参数名就够了

函数式接口：

```java
@FunctionalInterface
public interface MyInterface {
    void m1(String a, int b);
}
```



测试类：

```java
//支持lambda表达式参数中使用var
MyInterface mi = (var a,var b)->{
    System.out.println(a);
    System.out.println(b);
};

mi.m1("seven",1024);
```


## 基于嵌套的访问控制


Java 11 改进了嵌套类的访问控制，编译器不再需要生成合成的桥接方法来访问私有成员

```java
public class Outer {  
    private String outerField = "外部字段";  
      
    public class Inner {  
        public void accessOuter() {  
            // Java 11 中，内部类可以直接访问外部类的私有成员  
            // 不需要编译器生成额外的访问方法  
            System.out.println(outerField);  
        }  
    }  
      
    private void privateMethod() {  
        System.out.println("私有方法");  
    }  
      
    public class AnotherInner {  
        public void callPrivateMethod() {  
            // 直接调用外部类的私有方法  
            privateMethod();  
        }  
    }  
}
```

这个改进主要影响字节码生成，对开发者来说是透明的，但可以减少生成的类文件数量。

## 动态类文件常量

Java 11 在字节码层面引入了动态常量，这是一个底层特性，主要用于优化
- Lambda 表达式的性能
- 字符串连接操作
- 方法句柄的使用

对普通开发者来说这个特性是透明的，但可以提升运行时性能。

## Flight Recorder

Java Flight Recorder (FR)是一个低开销的事件记录框架，在 Java 11 中开源并免费使用：

```java
# 启动应用时启用 JFR  
java -XX:+FlightRecorder   
     -XX:StartFlightRecording=duration=60s,filename=myapp.jfr   
     MyApp  
  
# 运行时启用 JFR  
jcmd <pid> JFR.start duration=60s filename=myapp.jfr
```

JFR 可以记录：
- GC 事件
- 线程活动
- 方法调用
- I/O 操作
- 系统信息

生成的 .jfr 文件可以用 Java Mission Control (MC) 分析。

## 启动单文件源代码程序

Java 11 支持直接运行单个 .java 文件，无需先编译


在以前的版本中，在命令提示下，需要先编译，生成class文件之后再运行，例如：

```java
javac HelloWorld.Java
java HelloWorld
```


在Java 11中，可以这样直接运行，当然这样直接运行是不产生字节码文件的

```java
java HelloWorld.Java
```


这个特性主要用于
- 脚本编写
- 快速原型开发
- 教学演示

## 低开销堆分析

Java 11 引入了低开销的堆分析工具，可以通过 JVM 参数启用

```java
java -XX:+UnlockDiagnosticVMOptions   
     -XX:+DebugNonSafepoints   
     -XX:+FlightRecorder   
     -XX:StartFlightRecording=settings=profile   
     MyApp
```

相比传统的堆转储，这种方式:
- 开销更低
- 可以持续监控
- 提供更详细的分配信息



## ZGC 可扩展低延迟垃圾收集器

Java 11引入了 ZGC(Z Garbage Collector)作为实验特性，这是一个可扩展的低延迟垃圾收集器：

```java
# 启用 ZGC（实验特性）  
java -XX:+UnlockExperimentalVMOptions -XX:+UseZGC MyApp
```

ZGC 的特点：
- 超低延迟：暂停时间通常小于 10ms.
- 可扩展：支持几TB到16TB的堆大小
- 并发：大部分工作与应用线程并发执行

ZGC 适用于对延迟敏感的应用，如实时系统、高频交易等。

## TLS 1.3

Java 11 支持最新的 TLS 1.3 协议，提供更好的安全性和性能

```java
// TLS 1.3 会自动使用，无需特殊配置  
SSLContext context = SSLContext.getInstance("TLS");  
// 默认会尝试使用 TLS 1.3
```

TLS 1.3 的优势：
- 更少的握手往返
- 更强的加密算法
- 更好的前向保密性

## Unicode 10

Java 11 支持 Unicode 10.00.，新增了大量字符和表情符号

```java
// 支持新的 Unicode 字符  
String emoji = "🤩🎉🚀";  
System.out.println("表情符号：" + emoji);  
  
// 字符属性检查  
char ch = '🤩';  
System.out.println("是否为字母：" + Character.isLetter(ch));  
System.out.println("Unicode 类别：" + Character.getType(ch));
```

## Epsilon 无操作垃圾收集器

Java 11 引入了 Epsilon GC，这是一个“无操作"的垃圾收集器

```java
# 启用 Epsilon GC  
java -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC MyApp
```

Epsilon GC 的特点
- 不进行垃圾回收：只分配内存，不回收
- 最低开销：没有 GC 暂停
- 测试用途：主要用于性能测试和短期运行的应用

适用场景：
- 性能基准测试
- 内存压力测试
- 短期运行的批处理任务




<!-- @include: @article-footer.snippet.md -->     

