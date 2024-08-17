---
title: Spring - 概述
category: 常用框架
tag:
  - Spring
---



> Spring框架系列文章建议阅读顺序：Spring - 概述 -》IOC - 概述 -》 AOP - 概述 -》 MVC - 概述；而后再阅读其它文章详细内容

## 为什么用Spring

### 什么是Spring

Spring 是一款开源的轻量级 Java 开发框架，旨在提高开发人员的开发效率以及系统的可维护性。

**Spring的一个最大的目的就是使JAVA EE开发更加容易**。同时，Spring之所以与Struts、Hibernate等单层框架不同，是因为Spring致力于提供一个以统一的、高效的方式构造整个应用，并且可以将单层框架以最佳的组合揉和在一起建立一个连贯的体系。可以说Spring是一个提供了更完善开发环境的一个框架，可以为POJO(Plain Ordinary Java Object)对象提供企业级的服务。



### Spring的特性和优势

从Spring 框架的**特性**来看：

- 非侵入式：基于Spring开发的应用中的对象可以不依赖于Spring的API
- 控制反转：Inversion of Control(IOC)，指的是将对象的创建权交给 Spring 去创建，是一个轻量级的IOC容器。使用 Spring 之前，对象的创建都是由我们自己在代码中new创建。而使用 Spring 之后。对象的创建都是给了 Spring 框架，实现**松耦合**。
- 依赖注入：Dependency Injection(DI)，是指依赖的对象不需要手动调用 setXX 方法去设置，而是通过配置赋值。
- 面向切面编程：Aspect Oriented Programming(AOP)，把应用业务逻辑和系统服务分开，通过切面和模板减少样板式代码。
- 容器：Spring 是一个容器，它包含并且管理应用对象的生命周期
- 组件化：Spring 实现了使用简单的组件配置组合成一个复杂的应用。在 Spring 中可以使用XML和Java注解组合这些对象。
- 声明式事务的支持：可以从单调繁冗的事务管理代码中解脱出来，通过声明式方式灵活地进行事务的管理，可向下扩展到（例如使用一个单一的数据库）本地事务并扩展到全局事务（例如，使用 JTA），提高开发效率和质量。
- 一站式：在 IOC 和 AOP 的基础上可以整合各种企业应用的开源框架和优秀的第三方类库（实际上 Spring 自身也提供了表现层的 SpringMVC 和持久层的 Spring JDBC）

从使用Spring 框架的**优势**看：

- Spring 可以使开发人员使用 POJOs 开发企业级的应用程序。只使用 POJOs 的好处是你不需要一个应用程序服务器，但是你可以选择使用一个健壮的 servlet 容器，比如 Tomcat 或者一些商业产品。
- Spring 在一个单元模式中是有组织的。即使包和类的数量非常大，你只要担心你需要的，而其它的就可以忽略了。
- Spring 不会让你白费力气做重复工作，它可以整合一些现有的技术，像 ORM 框架、日志框架、JEE、Quartz 和 JDK 计时器，其他视图技术。
- 测试一个用 Spring 编写的应用程序很容易，因为环境相关的代码被移动到这个框架中。此外，通过使用 JavaBean-style POJOs，它在使用依赖注入测试数据时变得更容易。
- Spring 的 web 框架是一个设计良好的 web MVC 框架，它为比如 Structs 或者其他工程上的或者不怎么受欢迎的 web 框架提供了一个很好的供替代的选择。MVC 模式导致应用程序的不同方面(输入逻辑，业务逻辑和UI逻辑)分离，同时提供这些元素之间的松散耦合。模型(Model)封装了应用程序数据，通常它们将由 POJO 类组成。视图(View)负责渲染模型数据，一般来说它生成客户端浏览器可以解释 HTML 输出。控制器(Controller)负责处理用户请求并构建适当的模型，并将其传递给视图进行渲染。
- Spring 对 JavaEE 开发中非常难用的一些 API（JDBC、JavaMail、远程调用等），都提供了封装，使这些API应用难度大大降低。



### 相关资料

- [官网](https://spring.io/projects/spring-framework)
- [归档文档](https://docs.spring.io/spring-framework/docs/)
- [官方github源码](https://github.com/spring-projects/spring-framework)



## Spring的组件

![Spring5.x主要模块](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407201016227.png)

Spring5.x 版本中 Web 模块的 Portlet 组件已经被废弃掉，同时增加了用于异步响应式处理的 WebFlux 组件。

![Spring 各个模块的依赖关系](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407201016817.png)

从最下层往上介绍

### Spring Test

Spring 团队提倡测试驱动开发（TDD）。有了控制反转 (IoC)的帮助，单元测试和集成测试变得更简单。

Spring 的测试模块对 JUnit（单元测试框架）、TestNG（类似 JUnit）、Mockito（主要用来 Mock 对象）、PowerMock（解决 Mockito 的问题比如无法模拟 final, static， private 方法）等等常用的测试框架支持的都比较好。而且还额外提供了一些基于 Spring 的测试功能，比如在测试 Web 框架时，模拟 Http 请求的功能。

包含Mock Objects, TestContext Framework, Spring MVC Test, WebTestClient。

源码对应模块如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407201048121.png)



### Core  Container

Spring 框架的核心模块，也可以说是基础模块，主要提供 IoC 依赖注入功能的支持。由 Beans 模块、Core 核心模块、Context 上下文模块和 SpEL 表达式语言模块组成，没有这些核心容器，也不可能有 AOP、Web 等上层的功能。

- **spring-core**：封装了 Spring 框架的底层部分，包括资源访问、类型转换及一些常用工具类。
- **spring-beans**：提供对 bean 的创建、配置和管理等功能的支持，包括控制反转和依赖注入。
- **spring-context**：建立在 Core 和 Beans 模块的基础之上，集成 Beans 模块功能并添加资源绑定、数据验证、国际化、Java EE 支持、容器生命周期、事件传播等。ApplicationContext 接口是上下文模块的焦点。
- **spring-expression**：提供对表达式语言（Spring Expression Language） SpEL 的支持，只依赖于 core 模块，不依赖于其他模块，可以单独使用。支持访问和修改属性值，方法调用，支持访问及修改数组、容器和索引器，命名变量，支持算数和逻辑运算，支持从 Spring 容器获取 Bean，它也支持列表投影、选择和一般的列表聚合等。

对应源码模块如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407201046700.png)



### AOP、Aspects、Instrumentation和Messaging

- **spring-aspects**：该模块为与 AspectJ 的集成提供支持，是一个功能强大且成熟的面向切面编程（AOP）框架。
- **spring-aop**：提供了面向切面的编程实现。提供比如日志记录、权限控制、性能统计等通用功能和业务逻辑分离的技术，并且能动态的把这些功能添加到需要的代码中，这样各司其职，降低业务逻辑和通用功能的耦合。
- **spring-instrument**：提供了为 JVM 添加代理（agent）的功能。 具体来讲，它为 Tomcat 提供了一个织入代理，能够为 Tomcat 传递类文 件，就像这些文件是被类加载器加载的一样。没有理解也没关系，这个模块的使用场景非常有限。
- **spring-messaging**：是从 Spring4.0 开始新加入的一个模块，主要职责是为 Spring 框架集成一些基础的报文传送应用。
- **spring-jcl 模块**： Spring 5.x中新增了日志框架集成的模块。

对应源码模块如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407201051187.png)



### Data Access/Integration

- **spring-jdbc**：提供了对数据库访问的抽象 JDBC。不同的数据库都有自己独立的 API 用于操作数据库，而 Java 程序只需要和 JDBC API 交互，这样就屏蔽了数据库的影响。
- **spring-tx**：支持编程和声明式事务管理。
- **spring-orm**：提供对 Hibernate、JPA、iBatis 和 MyBatis 等 ORM 框架的支持。而且还可以使用 Spring 事务管理，无需额外控制事务。
- **spring-oxm**：提供一个抽象层支撑 OXM(Object-to-XML-Mapping)，例如：JAXB、Castor、XMLBeans、JiBX 和 XStream 等。将 Java 对象映射成 XML 数据，或者将XML 数据映射成 Java 对象。
- **spring-jms** : 指 Java 消息服务，提供一套 “消息生产者、消息消费者”模板用于更加简单的使用 JMS，JMS 用于用于在两个应用程序之间，或分布式系统中发送消息，进行异步通信。自 Spring Framework 4.1 以后，它还提供了对 spring-messaging 模块的继承。

对应源码模块：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407201055987.png)



### Spring Web

- **spring-web**：提供了基本的 Web 开发集成特性，例如多文件上传功能、使用的 Servlet 监听器的 IOC 容器初始化以及 Web 应用上下文。
- **spring-webmvc**：提供了一个 Spring MVC Web 框架实现。Spring MVC 框架提供了基于注解的请求资源注入、更简单的数据绑定、数据验证等及一套非常易用的 JSP 标签，完全无缝与 Spring 其他技术协作。
- **spring-websocket**：提供了对 WebSocket 的支持，WebSocket 可以让客户端和服务端进行双向通信。
- **spring-webflux**：提供对 WebFlux 的支持。WebFlux 是 Spring Framework 5.0 中引入的新的响应式框架。与 Spring MVC 不同，它不需要 Servlet API，是完全异步，并且通过Reactor项目实现了Reactive Streams规范。Spring WebFlux 用于创建基于事件循环执行模型的完全异步且非阻塞的应用程序。

对应源码模块如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407201102080.png)



## Spring、SpringMVC、SpringBoot之间的关系

Spring 包含了多个功能模块（上面刚刚提到过），其中最重要的是 Spring-Core（主要提供 IoC 依赖注入功能的支持） 模块， Spring 中的其他模块（比如 Spring MVC）的功能实现基本都需要依赖于该模块。

Spring MVC 是 Spring 中的一个很重要的模块，主要赋予 Spring 快速构建 MVC 架构的 Web 程序的能力。MVC 是模型(Model)、视图(View)、控制器(Controller)的简写，其核心思想是通过将业务逻辑、数据、显示分离来组织代码。

使用 Spring 进行开发各种配置过于麻烦比如开启某些 Spring 特性时，需要用 XML 或 Java 进行显式配置。于是，Spring Boot 诞生了！

Spring 旨在简化 J2EE 企业应用程序开发。Spring Boot 旨在简化 Spring 开发（减少配置文件，开箱即用！）。

Spring Boot 只是简化了配置，如果你需要构建 MVC 架构的 Web 程序，你还是需要使用 Spring MVC 作为 MVC 框架，只是说 Spring Boot 帮你简化了 Spring MVC 的很多配置，真正做到开箱即用！



## Spring 用到的设计模式

1. **简单工厂模式**：`BeanFactory`就是简单工厂模式的体现，根据传入一个唯一标识来获得 Bean 对象。

   ```java
   @Override
   public Object getBean(String name) throws BeansException {
       assertBeanFactoryActive();
       return getBeanFactory().getBean(name);
   }
   ```

2. **工厂方法模式**：`FactoryBean`就是典型的工厂方法模式。spring在使用`getBean()`调用获得该bean时，会自动调用该bean的`getObject()`方法。每个 Bean 都会对应一个 `FactoryBean`，如 `SqlSessionFactory` 对应 `SqlSessionFactoryBean`。

3. **单例模式**：一个类仅有一个实例，提供一个访问它的全局访问点。Spring 创建 Bean 实例默认是单例的。

4. **适配器模式**：SpringMVC中的适配器`HandlerAdatper`。由于应用会有多个Controller实现，如果需要直接调用Controller方法，那么需要先判断是由哪一个Controller处理请求，然后调用相应的方法。当增加新的 Controller，需要修改原来的逻辑，违反了开闭原则（对修改关闭，对扩展开放）。
   为此，Spring提供了一个适配器接口，每一种 Controller 对应一种 `HandlerAdapter` 实现类，当请求过来，SpringMVC会调用`getHandler()`获取相应的Controller，然后获取该Controller对应的 `HandlerAdapter`，最后调用`HandlerAdapter`的`handle()`方法处理请求，实际上调用的是Controller的`handleRequest()`。每次添加新的 Controller 时，只需要增加一个适配器类就可以，无需修改原有的逻辑。
   常用的处理器适配器：`SimpleControllerHandlerAdapter`，`HttpRequestHandlerAdapter`，`AnnotationMethodHandlerAdapter`。

   ```java
   // Determine handler for the current request.
   mappedHandler = getHandler(processedRequest);
   
   HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
   
   // Actually invoke the handler.
   mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
   
   public class HttpRequestHandlerAdapter implements HandlerAdapter {
   
       @Override
       public boolean supports(Object handler) {//handler是被适配的对象，这里使用的是对象的适配器模式
           return (handler instanceof HttpRequestHandler);
       }
   
       @Override
       @Nullable
       public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
           throws Exception {
   
           ((HttpRequestHandler) handler).handleRequest(request, response);
           return null;
       }
   }
   ```

5. **代理模式**：spring 的 aop 使用了动态代理，有两种方式`JdkDynamicAopProxy`和`Cglib2AopProxy`。

6. **观察者模式**：spring 中 observer 模式常用的地方是 listener 的实现，如`ApplicationListener`。

7. **模板模式**： Spring 中 `jdbcTemplate`、`hibernateTemplate` 等，就使用到了模板模式。



## HelloWorld-xml

> 这里只是表示这是Spring第一个项目，以HelloWorld作为标注。实际需求是获取 用户列表信息，并打印执行日志

### 案例

案例源码点击[这里](https://github.com/Seven-97/Spring-Demo/tree/master/01-spring-framework-helloworld-xml)

- 引入依赖

```xml
<properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <spring.version>5.3.37</spring.version>
        <aspectjweaver.version>1.9.6</aspectjweaver.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
            <version>${spring.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-core</artifactId>
            <version>${spring.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-beans</artifactId>
            <version>${spring.version}</version>
        </dependency>
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
            <version>${aspectjweaver.version}</version>
        </dependency>
    </dependencies>
```

- POJO - User

```java
public class User {

    private String name;

    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
```

- DAO 获取 POJO， UserDaoServiceImpl (mock 数据)

```java
public class UserDaoImpl{

    public List<User> findUserList() {
        return Collections.singletonList(new User("seven", 18));
    }
}
```

- 业务层 UserServiceImpl（调用DAO层）

```java
public class UserServiceImpl {
    private UserDaoImpl userDao;

    public void setUserDao(UserDaoImpl userDao) {
        this.userDao = userDao;
    }

    public List<User> findUserList() {
        return userDao.findUserList();
    }
}
```

- 拦截所有service中的方法，并输出记录

```java
@Aspect
public class LogAspect {

    @Around("execution(* com.seven.springhelloworldxml.service.*.*(..))")
    public Object businessService(ProceedingJoinPoint pjp) throws Throwable {
        // get attribute through annotation
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        System.out.println("execute method: " + method.getName());

        // continue to process
        return pjp.proceed();
    }
}
```

- 添加并增加spring.xml和aspects.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="userDao" class="com.seven.springhelloworldxml.dao.UserDaoImpl">
        <!-- additional collaborators and configuration for this bean go here -->
    </bean>

    <bean id="userService" class="com.seven.springhelloworldxml.service.UserServiceImpl">
        <property name="userDao" ref="userDao"/>
        <!-- additional collaborators and configuration for this bean go here -->
    </bean>
    
</beans>
```

```java
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
 http://www.springframework.org/schema/beans/spring-beans.xsd
 http://www.springframework.org/schema/aop
 http://www.springframework.org/schema/aop/spring-aop.xsd
 http://www.springframework.org/schema/context
 http://www.springframework.org/schema/context/spring-context.xsd
">

    <context:component-scan base-package="com.seven.springhelloworldxml" />

    <aop:aspectj-autoproxy/>

    <bean id="logAspect" class="com.seven.springhelloworldxml.aspects.LogAspect">
        <!-- configure properties of aspect here as normal -->
    </bean>
    <!-- more bean definitions for data access objects go here -->
</beans>
```

- APP中设置xml文件

```java
public class APP {

    public static void main(String[] args) {
        // create and configure beans
        ApplicationContext context = new ClassPathXmlApplicationContext("aspects.xml", "spring.xml");

        // retrieve configured instance
        UserServiceImpl service = context.getBean("userService", UserServiceImpl.class);

        // use configured instance
        List<User> userList = service.findUserList();

        // print info from beans
        userList.forEach(a -> System.out.println(a.getName() + "," + a.getAge()));
    }
}
```

运行结果：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407201420026.png)

### 如何体现的Spring优势

#### 控制反转 - IOC

**查询用户**（service通过调用dao查询pojo)，本质上就是如何创建User/Dao/Service？

- **如果没有Spring框架，需要自己创建User/Dao/Service等**，比如：

```java
UserDaoImpl userDao = new UserDaoImpl();
UserSericeImpl userService = new UserServiceImpl();
userService.setUserDao(userDao);
List<User> userList = userService.findUserList();
```

- **有了Spring框架，可以将原有Bean的创建工作转给框架, 需要用时从Bean的容器中获取即可，这样便简化了开发工作**

Bean的创建和使用分离了。

```java
// create and configure beans
ApplicationContext context = new ClassPathXmlApplicationContext("aspects.xml", "spring.xml");

// retrieve configured instance
UserServiceImpl service = context.getBean("userService", UserServiceImpl.class);

// use configured instance
List<User> userList = service.findUserList();
```



更进一步，**便能理解为何会有如下的知识点了**：

1. Spring框架管理这些Bean的创建工作，即由用户管理Bean转变为框架管理Bean，这个就叫**控制反转 - Inversion of Control (IoC)**
2. Spring 框架托管创建的Bean放在哪里呢？ 这便是**IoC Container**;
3. Spring 框架为了更好让用户配置Bean，必然会引入**不同方式来配置Bean？ 这便是xml配置，Java配置，注解配置**等支持
4. Spring 框架既然接管了Bean的生成，必然需要**管理整个Bean的生命周期**等；
5. 应用程序代码从Ioc Container中获取依赖的Bean，注入到应用程序中，这个过程叫 **依赖注入(Dependency Injection，DI)** ； 所以说控制反转是通过依赖注入实现的，其实它们是同一个概念的不同角度描述。通俗来说就是**IoC是设计思想，DI是实现方式**
6. 在依赖注入时，有哪些方式呢？这就是构造器方式，@Autowired, @Resource, @Qualifier... 同时Bean之间存在依赖（可能存在先后顺序问题，以及**循环依赖问题**等）



####  面向切面 - AOP

第二个需求：**给Service所有方法调用添加日志**（调用方法时)，本质上是解耦问题；

- **如果没有Spring框架，需要在每个service的方法中都添加记录日志的方法**，比如：

```java
public List<User> findUserList() {
    System.out.println("execute method findUserList");
    return this.userDao.findUserList();
}
```

- 有了Spring框架，通过@Aspect注解 定义了切面，这个切面中定义了拦截所有service中的方法，并记录日志； 可以明显看到，框架将日志记录和业务需求的代码解耦了，不再是侵入式的了

```java
/**
* aspect for every methods under service package.
*/
@Around("execution(* com.seven.springhelloworldxml.service.*.*(..))")
public Object businessService(ProceedingJoinPoint pjp) throws Throwable {
    // get attribute through annotation
    Method method = ((MethodSignature) pjp.getSignature()).getMethod();
    System.out.println("execute method: " + method.getName());

    // continue to process
    return pjp.proceed();
}
```

更进一步，**便能理解为何会有如下的知识点了**：

1. Spring 框架通过定义切面, 通过拦截切点实现了不同业务模块的解耦，这个就叫**面向切面编程 - Aspect Oriented Programming (AOP)**
2. 为什么@Aspect注解使用的是aspectj的jar包呢？这就引出了**Aspect4J和Spring AOP的历史渊源**，只有理解了Aspect4J和Spring的渊源才能理解有些注解上的兼容设计
3. 如何支持**更多拦截方式**来实现解耦， 以满足更多场景需求呢？ 这就是@Around, @Pointcut... 等的设计
4. 那么Spring框架又是如何实现AOP的呢？ 这就引入**代理技术，分静态代理和动态代理**，动态代理又包含JDK代理和CGLIB代理



## Spring框架逐步简化开发

### Java 配置方式改造

案例源码点击[这里](https://github.com/Seven-97/Spring-Demo/tree/master/02-spring-framework-helloworld-config)

在前文的例子中， 通过xml配置方式实现的，这种方式实际上比较麻烦； 我通过Java配置进行改造：

- User，UserDaoImpl, UserServiceImpl，LogAspect不用改
- 将原通过.xml配置转换为Java配置

```java
@EnableAspectJAutoProxy
@Configuration
public class BeansConfig {

    /**
     * @return user dao
     */
    @Bean("userDao")
    public UserDaoImpl userDao() {
        return new UserDaoImpl();
    }

    /**
     * @return user service
     */
    @Bean("userService")
    public UserServiceImpl userService() {
        UserServiceImpl userService = new UserServiceImpl();
        userService.setUserDao(userDao());
        return userService;
    }

    /**
     * @return log aspect
     */
    @Bean("logAspect")
    public LogAspect logAspect() {
        return new LogAspect();
    }
}
```

- 在App中加载BeansConfig的配置

```java
public class APP {

    public static void main(String[] args) {
        // create and configure beans
        ApplicationContext context = new AnnotationConfigApplicationContext(BeansConfig.class);

        // retrieve configured instance
        UserServiceImpl service = context.getBean("userService", UserServiceImpl.class);

        // use configured instance
        List<User> userList = service.findUserList();

        // print info from beans
        userList.forEach(a -> System.out.println(a.getName() + "," + a.getAge()));
    }

}
```



这里简单提一下实现原理：

1. 当应用启动时，Spring框架会使用Java的反射API来检查所有带有`@Configuration`  注解的类（这里不懂的可以看下[注解实现的原理](https://www.seven97.top/java/basis/03-annotations.html#注解实现的原理)）。Spring框架内置了一个注解处理器`ConfigurationClassPostProcessor`，它是`BeanFactoryPostProcessor`  的一个实现。

2. 接着`ConfigurationClassPostProcessor`  会在容器初始化时被调用，它会查找所有带有`@Configuration`  注解的类，并解析这些类中定义的`@Bean`  方法。

   - **BeanDefinition**：对于每个`@Configuration` 类，Spring会为其中的每个`@Bean` 方法生成一个`BeanDefinition` 对象。这些`BeanDefinition` 对象会包含创建和配置Bean所需的所有信息。

   - **处理嵌套配置**：如果一个`@Configuration` 类中包含了另一个`@Configuration` 类的引用，`ConfigurationClassPostProcessor` 会递归地处理这些嵌套的配置类。

3. **注册BeanDefinition**： 一旦所有的`BeanDefinition`  被创建，它们会被注册到Spring容器的`BeanFactory`  中。这样，Spring容器就可以在需要时创建和注入这些Bean。
4. **代理配置类**： 为了支持嵌套配置类和循环依赖等特性，Spring会为每个`@Configuration`  类创建一个代理，这个代理会在运行时处理相关的逻辑。



### 注解配置方式改造

案例源码点击[这里](https://github.com/Seven-97/Spring-Demo/tree/master/03-spring-framework-helloworld-anno)

更进一步，Java 5开始提供注解支持，Spring 2.5 开始完全支持基于注解的配置并且也支持JSR250 注解。在Spring后续的版本发展倾向于通过注解和Java配置结合使用.

- BeanConfig 不再需要Java配置

```java
@EnableAspectJAutoProxy
@Configuration
public class BeansConfig {

}
```



- UserDaoImpl 增加了 @Repository注解

```java
@Repository
public class UserDaoImpl{

    public List<User> findUserList() {
        return Collections.singletonList(new User("seven", 18));
    }
}
```



- UserServiceImpl 增加了@Service 注解，并通过@Autowired注入userDao

```java
@Service
public class UserServiceImpl {
    @Autowired
    private UserDaoImpl userDao;

    public List<User> findUserList() {
        return userDao.findUserList();
    }
}
```



- 日志类添加@Component注解

```java
@Component
@Aspect
public class LogAspect {

    @Around("execution(* com.seven.springhelloworldanno.service.*.*(..))")
    public Object businessService(ProceedingJoinPoint pjp) throws Throwable {
        // get attribute through annotation
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        System.out.println("execute method: " + method.getName());

        // continue to process
        return pjp.proceed();
    }

}
```



- 在App中扫描com.seven.springhelloworldanno包

```java
public static void main(String[] args) {
        // create and configure beans
        ApplicationContext context = new AnnotationConfigApplicationContext("com.seven.springhelloworldanno");

        // retrieve configured instance
        UserServiceImpl service = context.getBean(UserServiceImpl.class);

        // use configured instance
        List<User> userList = service.findUserList();

        // print info from beans
        userList.forEach(a -> System.out.println(a.getName() + "," + a.getAge()));
    }
```



> 这里要提一嘴的是，现在大多数的Spring项目，基本都是这种方式。主要步骤就是：
> 1、对类添加@Component相关的注解，比如@Controller，@Service，@Repository
> 2、设置ComponentScan的basePackage, 比如在xml文件里设置`<context:component-scan base-package='com.seven.springframework'>`, 或者在配置类中设置`@ComponentScan("com.seven.springframework")`注解，或者 直接在APP类中 `new AnnotationConfigApplicationContext("com.seven.springframework")`指定扫描的basePackage.





### SpringBoot托管配置

Springboot实际上通过约定大于配置的方式，使用xx-starter统一的对Bean进行默认初始化，用户只需要很少的配置就可以进行开发了。







<!-- @include: @article-footer.snippet.md -->     





