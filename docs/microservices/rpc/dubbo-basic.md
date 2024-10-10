---
title: dubbo - 入门
category: 微服务
tag:
 - dubbo
 - RPC
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,dubbo,rpc,spring集成dubbo,SpringBoot集成dubbo
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---

## 什么是Dubbo

目前来说，Dubbo是最有名的RPC服务调用框架，他是阿里开源的一个SOA服务治理框架，功能较为完善，支持多种传输和序列化方案。Dubbo最常用的应用就是远程调用。

Dubbo中服务端最核心的对象有四个：
- **ApplicationConfig**：配置当前应用信息
- **ProtocolConfig**：配置提供服务的协议信息
- **RegistryConfig**：配置注册相关信息
- **ServiceConfig**：配置暴露的服务信息

Dubbo客户端中核心的对象有两个：
- **ApplicationConfig**：配置当前应用信息
- **ReferenceConfig**：配置引用的服务信息


## Dubbo的使用

接下来通过四种方式入门Dubbo。首先会通过代码直接展示dubbo的直连和注册中心实现方式，接着使用Spring、注解和SpringBoot的方式分别展示如何使用Dubbo。

在写dubbo相关代码前，我们首先要定义一个**公共的api服务**，这个服务里存放的是service接口。服务提供者引入这个工程，写实现类，提供dubbo接口；服务消费者引入这个工程，通过这个工程的service接口调用。

User类：
```java
@Data
public class User implements Serializable {
    private static final long serialVersionUID = -9206514891359830486L;
    private Long id;
    private String name;
    private String sex;
}
```


UserService：
```java
public interface UserService {
    User getUser(Long id);
}
```


### 直接代码

接下来通过直接代码的方式生成一个dubbo服务，并且用另外一个类去调用这个dubbo服务：

- 引入依赖

核心依赖就两个，一个dubbo的依赖，另外一个上面的公共接口方法，服务提供方和消费者都需要引入这两个依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo</artifactId>
        <version>2.7.4.1</version>
    </dependency>
	<dependency>  
	    <artifactId>dubbo-api</artifactId>  
	    <groupId>com.seven.direct-dubbo-demo.api</groupId>  
	    <version>1.0-SNAPSHOT</version>  
	</dependency>
</dependencies>
```



- 服务提供者

服务提供者主要配置以下几个属性：

1. application：设置应用的名称等信息
2. protocol ：设置服务的协议
3. register：设置服务的连接方式
4. service：将需要暴露的服务注册出来

```java
public class DubboProvider {
    public static void main(String[] args) throws IOException {
        //暴露UserService服务
        //1、application
        ApplicationConfig applicationConfig = new ApplicationConfig("sample-provider");
        //2、protocol -dubbo协议
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName("dubbo");
        protocolConfig.setPort(20880);
        //3、register
        //直连的方式，不暴露到注册中心
        RegistryConfig registryConfig = new RegistryConfig(RegistryConfig.NO_AVAILABLE);
        //4、service
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setInterface(UserService.class);
        serviceConfig.setRef(new UserServiceImpl());
        //5、将application、protocol、register注册到service
        serviceConfig.setRegistry(registryConfig);
        serviceConfig.setProtocol(protocolConfig);
        serviceConfig.setApplication(applicationConfig);
        serviceConfig.export();

        System.out.println("服务已经暴露");
        System.in.read();
    }
}
```



- 服务消费者

消费者的实现主要就三步：

1. 配置application：设置应用的名称等信息
2. 配置reference：主要配置要引用的信息
3. 获取到接口，调用服务。

```java
public class DubboConsumer {
    public static void main(String[] args) {
        //1、application
        ApplicationConfig applicationConfig =new ApplicationConfig("sample-consumer");
        //2、配置reference
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setApplication(applicationConfig);
        referenceConfig.setInterface(UserService.class);
        referenceConfig.setUrl("dubbo://127.0.0.1:20880/com.seven.directdubbodemo.api.service.UserService?anyhost=true&application=sample&bind.ip=127.0.0.1&bind.port=20880&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=com.seven.directdubbodemo.api.service.UserService&methods=getUser&pid=5936&release=2.7.4.1&side=provider&timestamp=1728390413736");
        UserService userService = (UserService) referenceConfig.get();
        User user = userService.getUser(1L);
        System.out.println(user);
    }
}
```

先启动提供者，再启动消费者，如果user信息打印出来了就说明调用成功。

- 使用zookeeper作为注册中心

上面的Register使用的是直连的方式，也可以使用**注册中心**，这里以zookeeper为例。首先在项目中引入zookeeper相关依赖：

```xml
<!-- zk客户端依赖：curator -->
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-recipes</artifactId>
    <version>2.13.0</version>
</dependency>
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-framework</artifactId>
    <version>2.13.0</version>
</dependency>
```
 

服务提供者修改一处地方，将RegistryConfig修改为zookeeper的连接方式

```java
//register
//直连的方式，不暴露到注册中心
//RegistryConfig registryConfig=new RegistryConfig(RegistryConfig.NO_AVAILABLE);
//通过注册中心暴露dubbo
RegistryConfig registryConfig = new RegistryConfig("zookeeper://192.168.130.131:2181");
```


消费者同样修改一处位置，将referenceConfig中的setUrl方法替换为zookeeper：

```java
RegistryConfig registryConfig = new RegistryConfig("zookeeper://192.168.130.131:2181");
ReferenceConfig referenceConfig = new ReferenceConfig();
referenceConfig.setRegistry(registryConfig);
referenceConfig.setApplication(applicationConfig);
referenceConfig.setInterface(UserService.class);
//referenceConfig.setUrl("dubbo://127.0.0.1:20880/com.seven.directdubbodemo.api.service.UserService?anyhost=true&application=sample&bind.ip=127.0.0.1&bind.port=20880&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=com.seven.directdubbodemo.api.service.UserService&methods=getUser&pid=5936&release=2.7.4.1&side=provider&timestamp=1728390413736");
```


### Spring集成dubbo

通过Spring的方式只不过是把上面写在Java中的代码拿到配置文件中去，并把接口注入到Bean容器中

- 引入spring相关依赖

在provider和consumer的模块下额外引入spring相关依赖
```java
<dependency>  
    <groupId>org.springframework</groupId>  
    <artifactId>spring-context</artifactId>  
    <version>5.3.37</version>  
</dependency>  
<dependency>  
    <groupId>org.springframework</groupId>  
    <artifactId>spring-core</artifactId>  
    <version>5.3.37</version>  
</dependency>  
<dependency>  
    <groupId>org.springframework</groupId>  
    <artifactId>spring-beans</artifactId>  
    <version>5.3.37</version>  
</dependency>
```

- 在resource文件夹下新建两个配置文件

provider.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>  
<beans xmlns="http://www.springframework.org/schema/beans"  
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
       xmlns:dubbo="http://dubbo.apache.org/schema/dubbo"  
       xsi:schemaLocation="http://www.springframework.org/schema/beans  
       http://www.springframework.org/schema/beans/spring-beans.xsd       http://dubbo.apache.org/schema/dubbo       http://dubbo.apache.org/schema/dubbo/dubbo.xsd">  
  
    <!-- 提供方应用信息，用于计算依赖关系 -->  
    <dubbo:application name="spring-provider"/>  
  
    <!-- 使用zookeeper广播注册中心暴露服务地址 -->  
    <dubbo:registry address="zookeeper://127.0.0.1:2181"/>  
  
    <!-- 用dubbo协议在20880端口暴露服务 -->  
    <dubbo:protocol name="dubbo" port="20880"/>  
  
    <!-- 声明需要暴露的服务接口 -->  
    <dubbo:service interface="com.seven.springdubbodemo.springdubboapi.service.UserService" ref="userService"/>  
  
    <!-- 和本地bean一样实现服务 -->  
    <bean id="userService" class="com.seven.springdubbodemo.springdubboprovider.impl.UserServiceImpl"/>  
</beans>
```



consumer.xml

```java
<beans xmlns="http://www.springframework.org/schema/beans"  
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
       xmlns:dubbo="http://dubbo.apache.org/schema/dubbo"  
       xsi:schemaLocation="http://www.springframework.org/schema/beans  
       http://www.springframework.org/schema/beans/spring-beans.xsd       
       http://dubbo.apache.org/schema/dubbo       
       http://dubbo.apache.org/schema/dubbo/dubbo.xsd">  
  
    <dubbo:application name="spring-consumer"  />  
    <dubbo:registry address="zookeeper://127.0.0.1:2181" />  
    <dubbo:reference id="userService" interface="com.seven.springdubbodemo.springdubboapi.service.UserService" />  
</beans>
```


- 启动类

这里的配置文件和上方的代码均一一对应。服务的提供者和消费者：

SpringDubboProvider：

```java
public class SpringDubboProvider {
    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("provider.xml");
        System.out.println("服务已经暴露");
        System.in.read();
    }
}
```

SpringDubboConsumer

```java
public class SpringDubboConsumer {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("consumer.xml");
        UserService bean = context.getBean(UserService.class);
        System.out.println(bean.getUser(1L));
    }
}
```


### 纯注解版

注解的方式就是不在xml文件中注入bean，xml文件中只需要写包名即可

- provider修改
provider.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>  
<beans xmlns="http://www.springframework.org/schema/beans"  
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
       xmlns:dubbo="http://dubbo.apache.org/schema/dubbo"  
       xsi:schemaLocation="http://www.springframework.org/schema/beans  
       http://www.springframework.org/schema/beans/spring-beans.xsd       http://dubbo.apache.org/schema/dubbo       http://dubbo.apache.org/schema/dubbo/dubbo.xsd">  
  
    <!-- 提供方应用信息，用于计算依赖关系 -->  
    <dubbo:application name="anno-provider"/>  
  
    <!-- 使用zookeeper广播注册中心暴露服务地址 -->  
    <dubbo:registry address="zookeeper://127.0.0.1:2181"/>  
  
    <!-- 用dubbo协议在20880端口暴露服务 -->  
    <dubbo:protocol name="dubbo" port="20880"/>  
  
    <!-- 注解版 -->  
    <dubbo:annotation package="com.seven.annotationdubbodemo.annodubboprovider.impl"/>  
</beans>
```

UserService实现类
```java
import org.apache.dubbo.config.annotation.Service;//dubbo的@Service注解
@Service  
public class UserServiceImpl implements UserService {  
    @Override  
    public User getUser(Long id) {  
        User user = new User();  
        user.setId(id);  
        user.setName("Seven");  
        user.setSex("男");  
        return user;  
    }  
}
```

- consumer修改
consumer.xml
```xml
<beans xmlns="http://www.springframework.org/schema/beans"  
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
       xmlns:dubbo="http://dubbo.apache.org/schema/dubbo"  
       xmlns:context="http://www.springframework.org/schema/context"  
       xsi:schemaLocation="http://www.springframework.org/schema/beans  
       http://www.springframework.org/schema/beans/spring-beans.xsd       http://dubbo.apache.org/schema/dubbo       http://dubbo.apache.org/schema/dubbo/dubbo.xsd        http://www.springframework.org/schema/context        http://www.springframework.org/schema/context/spring-context.xsd">  
  
    <dubbo:application name="anno-consumer"  />  
    <dubbo:registry address="zookeeper://127.0.0.1:2181" />  
  
    <!-- 注解版 -->  
    <dubbo:annotation package="com.seven.annotationdubbodemo.annodubboapi.service"/>  
    <context:component-scan base-package="com.seven.annotationdubbodemo.annodubboconsumer.controller"/>  
  
</beans>
```

controller类
```java
@Controller  
public class UserController {  

    @Reference //import org.apache.dubbo.config.annotation.Reference;  
    private UserService userService;  
  
    public User getUser(long id){  
        return userService.getUser(id);  
    }  
  
}
```


### SpringBoot集成dubo

引入dubbo和springboot的核心依赖 

```xml
<!-- 引入spring-boot-starter以及dubbo和curator的依赖 -->  
<dependency>  
    <groupId>org.apache.dubbo</groupId>  
    <artifactId>dubbo-spring-boot-starter</artifactId>  
    <version>2.7.5</version>  
</dependency>  
<!-- Spring Boot相关依赖 -->  
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter</artifactId>  
    <version>2.7.5</version>  
</dependency>
```


- 服务提供者provider
这里的配置都写在application.properties中：

```yml
#当前服务/应用的名字  
dubbo.application.name=springboot-provider  
  
#注册中心的协议和地址  
dubbo.registry.protocol=zookeeper  
dubbo.registry.address=127.0.0.1:2181  
  
#通信规则（通信协议和接口）  
dubbo.protocol.name=dubbo  
dubbo.protocol.port=20880
```


服务提供者需要写服务的实现类，这里需要注意@Service注解采用的是dubbo包下：

```java
import com.seven.springbootdubbodemo.api.entity.User;  
import com.seven.springbootdubbodemo.api.service.UserService;  
import org.apache.dubbo.config.annotation.Service;  

@Service  
public class UserServiceImpl implements UserService {  
    @Override  
    public User getUser(Long id) {  
        User user = new User();  
        user.setId(id);  
        user.setName("Seven");  
        user.setSex("男");  
        return user;  
    }  
}
```


接着在启动类上添加一个@EnableDubbo注解即可。
```java
@EnableDubbo  
@SpringBootApplication  
public class App{  
  
    public static void main(String[] args) {  
        SpringApplication.run(App.class, args);  
    }  
}
```

- 服务的消费者consumer

配置文件：

```yml
#避免和provider端口冲突，设为8081端口访问  
server.port=8081  
  
#当前服务/应用的名字  
dubbo.application.name=springboot-consumer  
  
#注册中心的协议和地址  
dubbo.registry.protocol=zookeeper  
dubbo.registry.address=127.0.0.1:2181  
  
#通信规则（通信协议和接口）  
dubbo.protocol.name=dubbo  
dubbo.protocol.port=20880
```


接着通过@Reference注解将service对象引进来

```java
@SpringBootApplication  
public class App{  
  
    @Reference  
    UserService userService;  
  
    public static void main(String[] args) {  
        SpringApplication.run(App.class, args);  
    }  
  
    @Bean  
    public ApplicationRunner getBean(){  
        return args -> {  
            System.out.println(userService.getUser(1L));  
        };  
    }  
}
```


## dubbo的常用配置

```
<dubbo:application/> 用于配置当前应用信息
<dubbo:register/> 用于配置连接注册相关信息
<dubbo:protocol/> 用于配置提供服务的协议信息，提供者指定协议，消费者被动接受

<dubbo:service/> 用于暴露一个服务，一个服务可以用多个协议暴露，一个服务也可以注册到多个注册中心。provider端配置

<dubbo:reference/> 用于创建一个远程服务代理。consumer端配置
```

更加具体的配置信息我在官网中找到了，大家可参考：

https://dubbo.apache.org/zh/docs/v2.7/user/references/xml/

## 企业中如何通过dubbo实现分布式调用

在企业中，如果消费者直接通过RPC去调用提供者，理论上需要把提供者的整个Jar包引入到项目中。但是这样的话服务提供这种的其他无关代码也会被引入其中，导致代码污染。

因此实际开发过程中，**服务提供者和调用者之间会增加一层Client模块**。这个Client中主要写的是Service的接口定义，接口的返回实例对象以及接口的请求实例对象。简单来讲，**所有的定义都在Client中完成**。

使用时，服务提供者引入这个Client，然后写实现方法，服务消费者引入这个Client，然后通过dubbo直接调用即可。

另外企业开发中，可能会出现多个接口实现，这种情况下可以给Service设定group、version等进行区分。

## 总结

Dubbo的基本使用就这些，Dubbo毕竟只是一个RPC的工具，我们可以用它很方便地暴露、消费服务。但是以上也只是会上手使用，内部的原理可以继续看其他的文章



















<!-- @include: @article-footer.snippet.md -->     