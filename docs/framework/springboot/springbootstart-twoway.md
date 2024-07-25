---
title: Springboot启动的两种方式
category: 框架
tag:
  - SpringBoot
---



## 使用内置tomcat启动

### 配置案例

#### 启动方式

1. IDEA中main函数启动，[Main函数启动SpringBoot源码点击这里](https://github.com/Seven-97/SpringBoot-Demo/tree/master/01-helloworld-main)

2. mvn springboot-run 命令

3. java -jar XXX.jar：使用这种方式时，为保证服务在后台运行，会使用nohup 
   ```java
   nohup java -jar -Xms128m -Xmx128m -Xss256k -XX:+PrintGCDetails -XX:+PrintHeapAtGC -Xloggc:/data/log/web-gc.log web.jar >/data/log/web.log &
   ```

   

#### 配置内置tomcat属性

关于Tomcat的属性都在 `org.springframework.boot.autoconfigure.web.ServerProperties` 配置类中做了定义，我们只需在application.properties配置属性做配置即可。通用的Servlet容器配置都以 `server` 作为前缀

```bash
#配置程序端口，默认为8080
server.port= 8080
#用户会话session过期时间，以秒为单位
server.session.timeout=
#配置默认访问路径，默认为/
server.context-path=
```

而Tomcat特有配置都以 `server.tomcat` 作为前缀

```bash
# 配置Tomcat编码,默认为UTF-8
server.tomcat.uri-encoding=UTF-8
# 配置最大线程数
server.tomcat.max-threads=1000
```

> 注意：使用内置tomcat不需要有tomcat-embed-jasper和spring-boot-starter-tomcat依赖，因为在spring-boot-starter-web依赖中已经集成了tomcat



### 原理



## 使用外置tomcat部署

### 配置案例

[外置Tomcat启动SpringBoot源码点击这里](https://github.com/Seven-97/SpringBoot-Demo/tree/master/02-helloworld-tomcat)

#### 继承SpringBootServletInitializer

- 外部容器部署的话，就不能依赖于Application的main函数了，而是要以类似于web.xml文件配置的方式来启动Spring应用上下文，此时需要在启动类中继承SpringBootServletInitializer，并添加 @SpringBootApplication 注解，这是为了能扫描到所有Spring注解的bean

```java
@SpringBootApplication
public class SpringBootHelloWorldTomcatApplication extends SpringBootServletInitializer {

}
```

这个类的作用与在web.xml中配置负责初始化Spring应用上下文的监听器作用类似，只不过在这里不需要编写额外的XML文件了。



#### pom.xml修改tomcat相关的配置

首先需要将 jar 变成war `<packaging>war</packaging>`

如果要将**最终的打包形式改为war**的话，还需要对pom.xml文件进行修改，因为spring-boot-starter-web中包含内嵌的tomcat容器，所以直接部署在外部容器会冲突报错。因此需要将内置tomcat排除

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

在这里需要移除对嵌入式Tomcat的依赖，这样打出的war包中，在lib目录下才不会包含Tomcat相关的jar包，否则将会出现启动错误。

但是移除了tomcat后，原始的sevlet也被移除了，因此还需要额外引入servet的包

```java
<dependency>
      <groupId>javax.servlet</groupId>
      <artifactId>javax.servlet-api</artifactId>
      <version>${servlet.javax.servlet-api.version}</version>
</dependency>
```



#### 注意的问题

此时打成的包的名称应该和 application.properties 的 server.context-path=/test 保持一致

```xml
<build>
    <finalName>test</finalName>
</build>
```

如果不一样发布到tomcat的webapps下上下文会变化



### 原理







