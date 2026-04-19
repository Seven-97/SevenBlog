---
title: Tomcat的配置调优
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



## tomcat 相关配置修改

Tomcat的主配置文件是server.xml，它位于Tomcat安装目录下的conf文件夹中。这个文件包含了Tomcat服务器的核心配置信息，用于定义服务器运行的各种参数和属性。下面我们将详细解析server.xml文件的结构和常用元素。  

### server.xml文件结构  
server.xml文件是一个XML格式的文件，其结构大致可以分为以下几个部分：

1. Tomcat服务器配置（Server）
2. 连接器配置（Connector）
3. 引擎配置（Engine）
4. 主机配置（Host）
5. 上下文配置（Context）
6. 资源配置（Resource）
7. 其他自定义配置  

### 常用元素详解

#### Tomcat服务器配置（Server）  

在server.xml文件中，Server元素是根元素，用于定义整个Tomcat服务器的配置信息。常用的属性有：
- shutdown：用于指定关闭服务器的字符串，用于通过命令行或脚本关闭服务器。
- port：用于指定Tomcat服务器的监听端口。
- maxThreads：用于指定服务器最大线程数。
- scheme：用于指定URL解析使用的协议。
- secure：用于指定是否使用SSL连接。

#### 连接器配置（Connector）  

Connector元素用于定义Tomcat服务器的连接器，即Web应用程序与外部世界的通信方式。常用的属性有：
- protocol：指定使用的协议，如HTTP/1.1。
- port：指定连接器监听的端口号。
- redirectPort：指定重定向的端口号。
- executor：指定使用的线程池执行器。
- connectionTimeout：指定连接超时时间。
- maxThreads：指定最大线程数。
- maxConnections：指定最大并发连接数。
- acceptCount：指定队列中等待的连接数。
- acceptorThreadCount：指定接受连接的线程数。
- bufferSize：指定缓冲区大小。
- bufferPool：指定缓冲池大小。
- protocolHandler：指定协议处理器类型。
- compression：指定是否启用压缩功能。
- compressionMinSize：指定压缩的最小数据量。
- noCompressionUserAgents：指定不使用压缩的浏览器类型。
- compressableMimeType：指定压缩的MIME类型。
- secureConnectionOnly：指定是否仅使用安全的连接方式。
- sslProtocol：指定使用的SSL协议版本。
- sslCipherSuite：指定使用的SSL加密套件。
- sslCertificateFile：指定SSL证书文件的路径。
- sslCertificateKeyFile：指定SSL密钥文件的路径。
- sslCertificateChainFile：指定SSL证书链文件的路径。
- sslProtocol：指定使用的SSL协议版本。
- clientAuth：指定是否要求客户端验证。
- sslPasswordCipherSuite：指定SSL密码套件的密码算法。
- sslSessionCacheSize：指定SSL会话缓存的大小。
- sslSessionTimeout：指定SSL会话超时时间。

#### 引擎配置（Engine）  
Engine元素用于定义处理请求的引擎，它可以包含多个Host元素，每个Host元素表示一个虚拟主机。常用的属性有：

- name：指定引擎的名称。
- defaultHost：指定默认虚拟主机的名称。

#### 主机配置（Host）  

Host元素表示一个虚拟主机，它可以包含多个Context元素，每个Context元素表示一个Web应用程序的上下文信息。常用的属性有：

- name：指定虚拟主机的名称。
- appBase：指定Web应用程序所在的目录路径。
- unpackWARs：指定是否解压WAR文件到appBase目录下。
- autoDeploy：指定是否自动部署Web应用程序。


上下文配置（Context）和资源配置（Resource）等其他自定义配置这里就不一一列举了，它们根据实际需要而定，可以参考[Tomcat官方文档](https://tomcat.apache.org/tomcat-8.0-doc/architecture/overview.html)进行了解和配置。






### 示例
#### 修改端口

修改默认的http访问端口：8080 为 8181，在tomcat8.5/conf/server.xml文件里修改，示例


```xml
<Connector port="8181" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" />
```

#### 修改默认的session有效期

在tomcat/conf/web.xml文件里修改，范围是具体项目，示例

```xml
    <session-config>
       <session-timeout>30</session-timeout>
    </session-config>
```

在tomcat/conf/server.xml

```xml
<Context path="/test" docBase="/test"  
		　　defaultSessionTimeOut="3600" isWARExpanded="true"  
		　　isWARValidated="false" isInvokerEnabled="true"  
		　　isWorkDirPersistent="false"/>
```

在java后台代码里配置，具体页面的session有效时间

```xml
session.setMaxInactiveInterval（30*60)；
```

> 生效优先级: 3 > 2 > 1,




##  tomcat 调优

> **tomcat 修改最大线程,在tomcat/conf/server.xml文件里修改:示例**

- **maxThreads=“X” 表示最多同时处理X个连接**
- **minSpareThreads=“X” 初始化X个连接**
- **maxSpareThreads=“X” 表示如果最多可以有X个线程，一旦超过X个,则会关闭不在需要的线程**
- **acceptCount=“X” 当同时连接的人数达到maxThreads时,还可以排队,队列大小为X.超过X就不处理**

### tomcat 内存优化

- Windows 下的catalina.bat
- Linux 下的catalina.sh 如:

JAVA_OPTS=’-Xms256m -Xmx512m’
-Xms JVM初始化堆的大小
-Xmx JVM堆的最大值 实际参数大小根据服务器配置或者项目具体设置



### 在server.xml中实现对Tomcat的IO切换

[APR](http://apr.apache.org/)是从操作系统级别来解决异步的IO问题，大幅度的提高性能。

1. APR(Apache Portable Runtime)是一个高可移植库，它是Apache HTTP Server 2.x的核心，能更好地和其它本地web技术集成，总体上让Java更有效率作为一个高性能web服务器平台而不是简单作为后台容器。
2. 在产品环境中，特别是直接使用Tomcat做WEB服务器的时候，应该使用Tomcat Native来提高其性能。如果不配APR，基本上300个线程狠快就会用满，以后的请求就只好等待。但是配上APR之后，并发的线程数量明显下降，从原来的300可能会马上下降到只有几十，新的请求会毫无阻塞的进来。
3. 在局域网环境测，就算是400个并发，也是一瞬间就处理/传输完毕，但是在真实的Internet环境下，页面处理时间只占0.1%都不到，绝大部分时间都用来页面传输。如果不用APR，一个线程同一时间只能处理一个用户，势必会造成阻塞。所以生产环境下用apr是非常必要的.

<!-- @include: @article-footer.snippet.md -->     