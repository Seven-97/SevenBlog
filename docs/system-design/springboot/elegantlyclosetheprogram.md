---
title: 优雅地关闭程序
category: 系统设计
tag:
  - SpringBoot
---



## kill -9

kill -9 pid ：彻底杀死进程的意思，一般情况使用它没有问题，但是在项目中使用它就有可能存在致命的问题。

kill 可将指定的信息送至程序。预设的信息为SIGTERM(15)，可将指定程序终止。若仍无法终止该程序，可使用SIGKILL(9)信息尝试强制删除程序。程序或工作的编号可利用ps指令或jobs指令查看（这段话来自菜鸟教程）。

讲的这个复杂，简单点来说就是用来杀死linux中的进程。

 

**kill -9 pid 带来的问题:**

由于kill -9 属于暴力删除，所以会给程序带来比较严重的后果，那究竟会带来什么后果呢？

举个栗子：转账功能，再给两个账户进行加钱扣钱的时候突然断电了？这个时候会发生什么事情？对于InnoDB存储引擎来说，没有什么损失，因为它支持事务，但是对于MyISAM引擎来说那简直就是灾难，为什么？假如给A账户扣了钱，现在需要将B账户加钱，这个时候停电了，就会造成，A的钱被扣了，但是B没有拿到这笔钱，这在生产环境是绝对不允许的，kill -9 相当于突然断电的效果。

当然了，像转账这种，肯定不是使用MyISAM引擎，但是如今分布式火了起来，跨服务转账已经是很平常的事情，这种时候如果使用kill -9 去停止服务，那就不是你的事务能保证数据的准确性了，这个时候你可能会想到分布式事务，这个世界上没有绝对的安全系统或者架构，分布式事务也是一样，他也会存在问题，概率很小，如果一旦发生，损失有可能是无法弥补的，所以一定不能使用kill -9 去停止服务，因为你不知道他会造成什么后果。

在MyISAM引擎中表现的更明显，比如用户的信息由两张表维护，管理员修改用户信息的时候需要修改两张表，但由于你的kill -9 暴力结束项目，导致只修改成功了一张表，这也会导致数据的不一致性，这是小事，因为大不了再修改一次，但是金钱、合同这些重要的信息如果由于你的暴力删除导致错乱，我觉得可能比删库跑路还严重，至少删库还能恢复，你这个都不知道错在哪里。

 

## 优雅结束服务

**那应该怎么结束项目呢？**

其实java提供了结束项目的功能，比如：tomcat可以使用 shutdown.bat/shutdown.sh 进行优雅结束。

什么叫优雅结束？

- 第一步：停止接收请求和内部线程。

- 第二步：判断是否有线程正在执行。

- 第三步：等待正在执行的线程执行完毕。

- 第四步：停止容器。

以上四步才是正常的结束流程，那Spring Boot怎么正常结束服务呢？

 

### kill -15 pid

这种方式也会比较优雅的结束进程（项目），使用他的时候需要慎重，为什么呢？

 

来看个例子

我写了一个普通的controller方法做测试

```java
@GetMapping(value = "/test")
public String test(){
    log.info("test --- start");
    try {
        Thread.sleep(100000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    log.info("test --- end");
    return "test";
}
```



代码很简单，打印：test — start之后让让程序休眠100秒，然后再打印：test — end，在线程休眠中我们使用kill -15 pid来结束这个进程，你们猜 test — end会被打印吗？

application.yml配置

```xml
server:
  port: 9988
```



启动项目

```shell
sudo mvn spring-boot:run
```



这是maven启动springboot项目的方式

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272204016.png)

看到这个就代表项目启动成了

 

找到项目的进程id

```shell
sudo ps -ef |grep shutdown
```

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272205431.png)

 

这个就是项目的进程号，接下来先测试test接口，让线程进入休眠状态，然后再使用kill -15 14086停止项目

```shell
sudo curl 127.0.0.1:9988/test
```



回到项目日志

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272205020.png)

 

发现请求已经到达服务，并且线程已经成功进入休眠，现在kill -15 14086结束进程

```shell
sudo kill -15 14086
```



 

回到日志

```
2020-04-24 10:53:14.939  INFO 14086 --- [nio-9988-exec-1] com.ymy.controller.TestController        : test --- start
2020-04-24 10:54:02.450  INFO 14086 --- [extShutdownHook] o.s.s.concurrent.ThreadPoolTaskExecutor  : Shutting down ExecutorService 'applicationTaskExecutor'java.lang.InterruptedException: sleep interruptedat java.lang.Thread.sleep(Native Method)at com.ymy.controller.TestController.test(TestController.java:26)at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)at java.lang.reflect.Method.invoke(Method.java:498)at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:190)at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:138)at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:105)at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:879)at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:793)at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87)at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1040)at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:943)at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006)at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:898)at javax.servlet.http.HttpServlet.service(HttpServlet.java:634)at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883)at javax.servlet.http.HttpServlet.service(HttpServlet.java:741)at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:231)at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53)at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100)at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93)at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)at org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter.doFilterInternal(WebMvcMetricsFilter.java:109)at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201)at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:202)at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:96)at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:541)at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:139)at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92)at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:74)at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:343)at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:373)at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65)at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:868)at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1594)at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49)at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)at java.lang.Thread.run(Thread.java:748)2020-04-24 10:54:04.574  INFO 14086 --- [nio-9988-exec-1] com.ymy.controller.TestController        : test --- end2020-04-24 10:54:04.610 ERROR 14086 --- [nio-9988-exec-1] o.s.web.servlet.HandlerExecutionChain    : HandlerInterceptor.afterCompletion threw exceptionjava.lang.NullPointerException: nullat org.springframework.boot.actuate.metrics.web.servlet.LongTaskTimingHandlerInterceptor.stopLongTaskTimers(LongTaskTimingHandlerInterceptor.java:123) ~[spring-boot-actuator-2.2.6.RELEASE.jar:2.2.6.RELEASE]at org.springframework.boot.actuate.metrics.web.servlet.LongTaskTimingHandlerInterceptor.afterCompletion(LongTaskTimingHandlerInterceptor.java:79) ~[spring-boot-actuator-2.2.6.RELEASE.jar:2.2.6.RELEASE]at org.springframework.web.servlet.HandlerExecutionChain.triggerAfterCompletion(HandlerExecutionChain.java:179) ~[spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]at org.springframework.web.servlet.DispatcherServlet.triggerAfterCompletion(DispatcherServlet.java:1427) [spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1060) [spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:943) [spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006) [spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:898) [spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]at javax.servlet.http.HttpServlet.service(HttpServlet.java:634) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883) [spring-webmvc-5.2.5.RELEASE.jar:5.2.5.RELEASE]at javax.servlet.http.HttpServlet.service(HttpServlet.java:741) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:231) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53) [tomcat-embed-websocket-9.0.33.jar:9.0.33]at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) [spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) [spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) [spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) [spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter.doFilterInternal(WebMvcMetricsFilter.java:109) [spring-boot-actuator-2.2.6.RELEASE.jar:2.2.6.RELEASE]at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) [spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) [spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) [spring-web-5.2.5.RELEASE.jar:5.2.5.RELEASE]at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:202) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:96) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:541) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:139) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:74) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:343) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:373) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:868) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1594) [tomcat-embed-core-9.0.33.jar:9.0.33]at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49) [tomcat-embed-core-9.0.33.jar:9.0.33]at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149) [na:1.8.0_242]at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624) [na:1.8.0_242]at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) [tomcat-embed-core-9.0.33.jar:9.0.33]at java.lang.Thread.run(Thread.java:748) [na:1.8.0_242]
```

居然报错了，但是test — end是打印出来了，为什么会报错呢？这就和sleep这个方法有关了，在线程休眠期间，当调用线程的interrupt方法的时候会导致sleep抛出异常，这里很明显就是kill -15 这个命令会让程序马上调用线程的interrupt方法，目的是为了让线程停止，虽然让线程停止，但线程什么时候停止还是线程自己说的算，这就是为什么还能看到：test — end的原因。

### ConfigurableApplicationContext colse

先看怎么实现

```java
@RestController
@Slf4j
public class TestController  implements ApplicationContextAware {
    private  ApplicationContext  context;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
    @GetMapping(value = "/test")
    public String test(){
        log.info("test --- start");
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("test --- end");
        return "test";
    }
    /**
     * 停机
     */
    @PostMapping(value = "shutdown")
    public void shutdown(){
        ConfigurableApplicationContext cyx = (ConfigurableApplicationContext) context;
        cyx.close();
    }
}
```



重点在：cyx.close();，为什么能停止Spring Boot项目呢？

 

请看源码

```java
public void close() {
    synchronized(this.startupShutdownMonitor) {
        this.doClose();
        if (this.shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
            } catch (IllegalStateException var4) {
            }
        }
    }
}
```



程序在启动的时候向jvm注册了一个关闭钩子，在执行colse方法的时候会删除这个关闭钩子，jvm就会知道这是需要停止服务。

 

看测试结果

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272205683.png)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272206482.png)

很明显，也出发了线程的interrupt方法导致线程报错，原理和kill -15差不多。

### actuator

这种方式是通过引入依赖的方式停止服务，actuator提供了很多接口，比如健康检查，基本信息等等，也可以使用他来优雅的停机。

 

引入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```



application.yml

```xml
server:
  port: 9988
management:
  endpoints:
    web:
      exposure:
        include: shutdown
  endpoint:
    shutdown:
      enabled: true
  server:
    port: 8888
```



这里对actuator的接口重新给定了一个接口，这样可提高安全性，下面来测试一下

```java
@RequestMapping(value = "/test",method = RequestMethod.GET)
public String test(){
        System.out.println("test --- start");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("test --- end");
    return "hello";
}
```



在请求test途中停止服务

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272206196.png)

 

发现发送停止服务请求之后还返回了提示信息，很人性化，看看控制台

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272206398.png)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272206678.png)

test — end被执行了，不过在停止线程池的时候还是调用了线程的interrupt方法，导致sleep报错，这三种方式都可以比较优雅的停止Spring Boot服务，如果项目中存在线程休眠，希望10秒以后再停止服务可以吗？肯定是可以的，只需要稍微做点修改就可以了。

#### 配置10秒后再停止服务

1. 新增停止Spring Boot服务类：ElegantShutdownConfig.java

```java
public class ElegantShutdownConfig implements TomcatConnectorCustomizer, ApplicationListener<ContextClosedEvent> {
    private volatile Connector connector;
    private final int waitTime = 10;
    @Override
    public void customize(Connector connector) {
        this.connector = connector;
    }
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        connector.pause();
        Executor executor = connector.getProtocolHandler().getExecutor();
        if (executor instanceof ThreadPoolExecutor) {
            try {
                ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
                threadPoolExecutor.shutdown();
                if (!threadPoolExecutor.awaitTermination(waitTime, TimeUnit.SECONDS)) {
                    System.out.println("请尝试暴力关闭");
                }
            } catch (InterruptedException ex) {
                System.out.println("异常了");
                Thread.currentThread().interrupt();
            }
        }
    }
}
```



2. 在启动类中加入bean

```java
@SpringBootApplication
public class ShutdownServerApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(ShutdownServerApplication.class, args);
        run.registerShutdownHook();
    }
    @Bean
    public ElegantShutdownConfig elegantShutdownConfig() {
        return new ElegantShutdownConfig();
    }
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.addConnectorCustomizers(elegantShutdownConfig());
        return tomcat;
    }
}
```



 

这样就配置好了，再来测试一遍，test的接口还是休眠10秒

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272206588.png)

发现这次没有报错了，他是等待了一段时间之后再结束的线程池，这个时间就是在ElegantShutdownConfig类中配置的waitTime。

 

那可能会有疑问了，jvm没有立即停止，那这个时候在有请求会发生什么呢？如果关闭的时候有新的请求，服务将不在接收此请求。

#### 数据备份操作

如果想在服务停止的时候做点备份操作啥的，应该怎么做呢？只要在要执行的方法上添加一个注解即可：

@PreDestroy:Destroy：消灭、毁灭，pre:前缀缩写

所以合在一起的意思就是在容器停止之前执行一次，可以在这里面做备份操作，也可以做记录停机时间等。

 

新增服务停止备份工具类：DataBackupConfig.java

```java
@Configuration
public class DataBackupConfig {
    @PreDestroy
    public  void backData(){
        System.out.println("正在备份数据。。。。。。。。。。。");
    }
}
```



再来测试然后打印控制台日志：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272206572.png)

 

 

 

 <!-- @include: @article-footer.snippet.md -->     

 

 

 

 