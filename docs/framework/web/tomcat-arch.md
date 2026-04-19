---
title: Tomcat - 架构设计
category: 常用框架
tags:
  - Tomcat
head:
  - - meta
    - name: keywords
      content: Tomcat,Tomcat配置调优
  - - meta
    - name: description
      content: 全网最全的Tomcat知识点总结，让天下没有难学的八股文！
---
## 引入

### Tomcat和Catalina是什么关系？

Tomcat的前身为Catalina，Catalina又是一个轻量级的Servlet容器。在美国，catalina是一个很美的小岛。所以Tomcat作者的寓意可能是想把Tomcat设计成一个优雅美丽且轻量级的web服务器。Tomcat从4.x版本开始除了作为支持Servlet的容器外，额外加入了很多的功能，比如：jsp、el、naming等等，所以说**Tomcat不仅仅是Catalina**。

### 什么是Servlet？

> 所谓Servlet，其实就是Sun为了让Java能实现动态可交互的网页，从而进入Web编程领域而制定的一套标准！

在互联网兴起之初，当时的Sun公司（后面被Oracle收购）已然看到了这次机遇，于是设计出了Applet来对Web应用的支持。不过事实却并不是预期那么得好，Sun悲催地发现Applet并没有给业界带来多大的影响。经过反思，Sun就想既然机遇出现了，市场前景也非常不错，总不能白白放弃了呀，怎么办呢？于是又投入精力去搞一套规范出来，这时Servlet诞生了！

一个Servlet主要做下面三件事情：

- 创建并填充Request对象，包括：URI、参数、method、请求头信息、请求体信息等
- 创建Response对象
- 执行业务逻辑，将结果通过Response的输出流输出到客户端

**Servlet没有main方法，所以，如果要执行，则需要在一个容器里面才能执行，这个容器就是为了支持Servlet的功能而存在，Tomcat其实就是一个Servlet容器的实现**。


## 核心架构设计

官网：https://tomcat.apache.org/tomcat-8.0-doc/architecture/overview.html

Tomcat 的架构设计以 ‌**模块化、分层、解耦**‌ 为核心，遵循 Java Servlet 规范，同时支持高性能、高扩展的 Web 服务。其整体架构可概括为 ‌**“连接器（Connector）- 容器（Container）” 双层模型**‌，并通过 ‌**Lifecycle 生命周期管理机制**‌ 和 ‌**责任链模式（Pipeline-Valve）**‌ 实现组件协同。


Tomcat的架构呈“套娃式”嵌套：Server → Service → (Connector + Engine) → Host → Context → Wrapper 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202603081022091.png)


核心架构组成：

- ‌**Server**‌：代表整个 Tomcat 实例，是顶级容器，管理多个 Service。
- ‌**Service**‌：将一个或多个 Connector 与一个 Engine 绑定，构成独立服务单元。
	- Manager：管理器，用于管理会话Session
	- Logger：日志器，用于管理日志
	- Loader：加载器，和类加载有关，只会开放给Context所使用
	- Pipeline：管道组件，配合Valve实现过滤器功能
	- Valve：阀门组件，配合Pipeline实现过滤器功能
	- Realm：认证授权组件
- ‌**Connector**（连接器）‌：负责处理外部 HTTP/AJP 请求，实现网络通信与协议解析。
- ‌**Container**（容器）‌：负责加载和管理 Servlet，处理业务逻辑，包含四级嵌套容器：
    - ‌**Engine**‌：处理所有请求，每个 Service 仅有一个。
    - ‌**Host**‌：虚拟主机，对应一个域名或 IP。
    - ‌**Context**‌：Web 应用上下文，对应一个 WAR 包或目录。
    - ‌**Wrapper**‌：最底层容器，封装单个 Servlet。


### 从web.xml配置和模块对应角度

上述模块的理解不是孤立的，它可以直接映射为Tomcat的web.xml配置，让我们联系起来看

```xml
<Server port="8005" shutdown="SHUTDOWN">
  <Listener className="org.apache.catalina.startup.VersionLoggerListener" />

  <Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" />

  <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener" />
  <Listener className="org.apache.catalina.mbeans.GlobalResourcesLifecycleListener" />
  <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />

  <GlobalNamingResources>
    <Resource name="UserDatabase" auth="Container"
              type="org.apache.catalina.UserDatabase"
              description="User database that can be updated and saved"
              factory="org.apache.catalina.users.MemoryUserDatabaseFactory"
              pathname="conf/tomcat-users.xml" />
  </GlobalNamingResources>

  <Service name="Catalina">

    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" />
    <Engine name="Catalina" defaultHost="localhost">
      <Realm className="org.apache.catalina.realm.LockOutRealm">
        <Realm className="org.apache.catalina.realm.UserDatabaseRealm"
               resourceName="UserDatabase"/>
      </Realm>

      <Host name="localhost"  appBase="webapps"
            unpackWARs="true" autoDeploy="true">
        <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs"
               prefix="localhost_access_log" suffix=".txt"
               pattern="%h %l %u %t &quot;%r&quot; %s %b" />

      </Host>
    </Engine>
  </Service>
</Server>
```

### 从一个完整请求的角度来看

> 通过一个完整的HTTP请求，我们还需要把它贯穿起来

假设来自客户的请求为：http://localhost:8080/test/index.jsp 请求被发送到本机端口8080，被在那里侦听的Coyote HTTP/1.1 Connector,然后

- Connector把该请求交给它所在的Service的Engine来处理，并等待Engine的回应
- Engine获得请求localhost:8080/test/index.jsp，匹配它所有虚拟主机Host
- Engine匹配到名为localhost的Host(即使匹配不到也把请求交给该Host处理，因为该Host被定义为该Engine的默认主机)
- localhost Host获得请求/test/index.jsp，匹配它所拥有的所有Context
- Host匹配到路径为/test的Context(如果匹配不到就把该请求交给路径名为""的Context去处理)
- path="/test"的Context获得请求/index.jsp，在它的mapping table中寻找对应的servlet
- Context匹配到URL PATTERN为*.jsp的servlet，对应于JspServlet类，构造HttpServletRequest对象和HttpServletResponse对象，作为参数调用JspServlet的doGet或doPost方法
- Context把执行完了之后的HttpServletResponse对象返回给Host
- Host把HttpServletResponse对象返回给Engine
- Engine把HttpServletResponse对象返回给Connector
- Connector把HttpServletResponse对象返回给客户browser

### 从源码的设计角度看

> 从功能的角度将Tomcat源代码分成5个子模块，分别是:

- **Jsper模**: 这个子模块负责jsp页面的解析、jsp属性的验证，同时也负责将jsp页面动态转换为java代码并编译成class文件。在Tomcat源代码中，凡是属于org.apache.jasper包及其子包中的源代码都属于这个子模块;
- **Servlet和Jsp模块**: 这个子模块的源代码属于javax.servlet包及其子包，如我们非常熟悉的javax.servlet.Servlet接口、javax.servet.http.HttpServlet类及javax.servlet.jsp.HttpJspPage就位于这个子模块中;
- **Catalina模块**: 这个子模块包含了所有以org.apache.catalina开头的java源代码。该子模块的任务是规范了Tomcat的总体架构，定义了Server、Service、Host、Connector、Context、Session及Cluster等关键组件及这些组件的实现，这个子模块大量运用了Composite设计模式。同时也规范了Catalina的启动及停止等事件的执行流程。从代码阅读的角度看，这个子模块应该是我们阅读和学习的重点。
- **Connector模块**: 如果说上面三个子模块实现了Tomcat应用服务器的话，那么这个子模块就是Web服务器的实现。所谓连接器(Connector)就是一个连接客户和应用服务器的桥梁，它接收用户的请求，并把用户请求包装成标准的Http请求(包含协议名称，请求头Head，请求方法是Get还是Post等等)。同时，这个子模块还按照标准的Http协议，负责给客户端发送响应页面，比如在请求页面未发现时，connector就会给客户端浏览器发送标准的Http 404错误响应页面。
- **Resource模块**: 这个子模块包含一些资源文件，如Server.xml及Web.xml配置文件。严格说来，这个子模块不包含java源代码，但是它还是Tomcat编译运行所必需的。
    

### 从后续深入理解的角度

> 我们看完上述组件结构后，后续应该重点从哪些角度深入理解Tomcat呢？

- **基于组件的架构**

我们知道组成Tomcat的是各种各样的组件，每个组件各司其职，组件与组件之间有明确的职责划分，同时组件与组件之间又通过一定的联系相互通信。Tomcat整体就是一个个组件的堆砌！

- **基于JMX**

我们在后续阅读Tomcat源码的时候，会发现代码里充斥着大量的类似于下面的代码。

```
Registry.getRegistry(null, null).invoke(mbeans, "init", false);
Registry.getRegistry(null, null).invoke(mbeans, "start", false);
```

而这实际上就是通过JMX来管理相应对象的代码。这儿我们不会详细讲述什么是JMX，我们只是简单地说明一下JMX的概念，参考JMX百度百科。

> JMX（Java Management Extensions，即Java管理扩展）是一个为应用程序、设备、系统等植入管理功能的框架。JMX可以跨越一系列异构操作系统平台、系统体系结构和网络传输协议，灵活的开发无缝集成的系统、网络和服务管理应用。

- **基于生命周期**

如果我们查阅各个组件的源代码，会发现绝大多数组件实现了Lifecycle接口，这也就是我们所说的基于生命周期。生命周期的各个阶段的触发又是基于事件的方式。


<!-- @include: @article-footer.snippet.md -->    