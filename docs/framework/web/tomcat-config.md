---
title: Tomcat - 配置调优
category: 常用框架
tag:
  - Tomcat
head:
  - - meta
    - name: keywords
      content: Tomcat,Tomcat配置调优
  - - meta
    - name: description
      content: 全网最全的的Tomcat知识点总结，让天下没有难学的八股文！
---







### tomcat 相关配置修改

修改默认的http访问端口:8080 为 8181,在tomcat8.5/conf/server.xml文件里修改:示例



highlighter- code-theme-dark HTML

```
<Connector port="8181" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" />
```

#### 修改默认的session有效期

在tomcat/conf/web.xml文件里修改,范围是具体项目:示例



highlighter- code-theme-dark HTML

```
    <session-config>
       <session-timeout>30</session-timeout>
    </session-config>
```

在tomcat/conf/server.xml



highlighter- code-theme-dark HTML

```
<Context path="/test" docBase="/test"  
		　　defaultSessionTimeOut="3600" isWARExpanded="true"  
		　　isWARValidated="false" isInvokerEnabled="true"  
		　　isWorkDirPersistent="false"/>
```

在java后台代码里配置,具体页面的session有效时间



highlighter- code-theme-dark

```
session.setMaxInactiveInterval（30*60)；
```

> 生效优先级: 3 > 2 > 1,

###  tomcat 调优

> **tomcat 修改最大线程,在tomcat/conf/server.xml文件里修改:示例**

------

- **maxThreads=“X” 表示最多同时处理X个连接**
- **minSpareThreads=“X” 初始化X个连接**
- **maxSpareThreads=“X” 表示如果最多可以有X个线程，一旦超过X个,则会关闭不在需要的线程**
- **acceptCount=“X” 当同时连接的人数达到maxThreads时,还可以排队,队列大小为X.超过X就不处理**

#### tomcat 内存优化

- Windows 下的catalina.bat
- Linux 下的catalina.sh 如:

JAVA_OPTS=’-Xms256m -Xmx512m’
-Xms JVM初始化堆的大小
-Xmx JVM堆的最大值 实际参数大小根据服务器配置或者项目具体设置



#### 在server.xml中实现对Tomcat的IO切换

[APR](http://apr.apache.org/)是从操作系统级别来解决异步的IO问题，大幅度的提高性能。

1. APR(Apache Portable Runtime)是一个高可移植库，它是Apache HTTP Server 2.x的核心，能更好地和其它本地web技术集成，总体上让Java更有效率作为一个高性能web服务器平台而不是简单作为后台容器。
2. 在产品环境中，特别是直接使用Tomcat做WEB服务器的时候，应该使用Tomcat Native来提高其性能。如果不配APR，基本上300个线程狠快就会用满，以后的请求就只好等待。但是配上APR之后，并发的线程数量明显下降，从原来的300可能会马上下降到只有几十，新的请求会毫无阻塞的进来。
3. 在局域网环境测，就算是400个并发，也是一瞬间就处理/传输完毕，但是在真实的Internet环境下，页面处理时间只占0.1%都不到，绝大部分时间都用来页面传输。如果不用APR，一个线程同一时间只能处理一个用户，势必会造成阻塞。所以生产环境下用apr是非常必要的.

<!-- @include: @article-footer.snippet.md -->     