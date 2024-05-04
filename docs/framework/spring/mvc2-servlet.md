---
title: MVC - Servlet介绍
category: 常用框架
tag:
  - SpringMVC
---



## 介绍

Servlet是在服务器端运行的Java程序，可以接收客户端请求并做出响应，是基于 Java 技术的 web 组件,该组件由容器托管,用于生成动态内容。他是用Java编写的服务器端程序。其主要功能在于交互式地浏览和修改数据，生成动态Web内容。

servlet说实在点就是个接口，浏览器发送请求给Tomcat（服务器），若是这个请求正好对应了servlet实现类的请求路径，Tomcat就会使用它来响应浏览器，这也就是request（请求）、response（响应）了。

```java
public interface Servlet {
    void init(ServletConfig var1) throws ServletException;

    ServletConfig getServletConfig();

    void service(ServletRequest var1, ServletResponse var2) throws ServletException, IOException;

    String getServletInfo();

    void destroy();
}
```



servlet有5个方法

## 执行流程



![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281539011.png)

1. 浏览器发出 http://localhost:8080/web-demo/demo1 请求，从请求中可以解析出三部分内容，分别是localhost:8080、web-demo、demo1：

   - 根据localhost:8080可以找到要访问的Tomcat Web服务器

   - 根据web-demo可以找到部署在Tomcat服务器上的web-demo项目

   - 根据demo1可以找到要访问的是项目中的哪个Servlet类，根据@WebServlet后面的值进行匹配

2. 找到ServletDemo1这个类后，Tomcat Web服务器就会为ServletDemo1这个类创建一个对象，然后调用对象中的service方法
3. ServletDemo1实现了Servlet接口，所以类中必然会重写service方法供Tomcat Web服务器进行调用
4. service方法中有ServletRequest和ServletResponse两个参数，ServletRequest封装的是请求数据，ServletResponse封装的是响应数据。

 

> 那么Servlet由谁创建? 
>
> Servlet方法由谁调用? Servlet由web服务器创建，Servlet方法由web服务器调用。

 

> 服务器怎么知道Servlet中一定有service方法?
>
>  因为自定义的Servlet，必须实现Servlet接口并复写其方法，而Servlet接口中有service方法。这就是接口的规范

 

## 生命周期

生命周期就是指对象在被创建到销毁的整个过程

Servlet运行在Servlet容器(web服务器)中，其生命周期由容器来管理，分为4个阶段：

1. 加载和实例化：默认情况下，当Servlet第一次被访问时，由容器创建Servlet对象。

   - 默认情况，Servlet会在第一次访问被容器创建，但是如果创建Servlet比较耗时的话，那么第一个访问的人等待的时间就比较长，用户的体验就比较差，可以修改loadOnStartup配置 -> @WebServlet(urlPatterns = "/demo1",loadOnStartup = 1) 

     - loadOnstartup的取值有两类情况：

     - 负整数:第一次访问时创建Servlet对象     

     - 0或正整数:服务器启动时创建Servlet对象，数字越小优先级越高

2. 初始化：在Servlet实例化之后，容器将调用Servlet的 init() 方法初始化这个对象，完成一些如加载配置文件、创建连接等初始化的工作。该方法只调用一次。
3. 请求处理：每次请求Servlet时，Servlet容器都会调用Servlet的 service() 方法对请求进行处理。 service方法在Servlet被访问的时候调用，每访问1次就调用1次
4. 服务终止：当需要释放内存或者容器关闭时，容器就会调用Servlet实例的 destroy() 方法完成资源的释放。在destroy()方法调用之后，容器会释放这个Servlet实例，该实例随后会被Java的垃圾收集器所回收。 destroy() 方法只调用一次。

## HttpServlet

前端发送GET和POST请求的时候，参数的位置不一致，GET请求参数在请求行中，POST请求参数在请求体中，为了能处理不同的请求方式，得在service方法中进行判断，然后写不同的业务处理。但是每个Servlet类中都将有相似的代码，因此就有了HttpServlet，来简化代码开发。

编写Servlet类的时候，只需要继承MyHttpServlet，重写父类中的doGet和doPost方法，就可以用来处理GET和POST请求的业务逻辑。

## 和MVC的对比

Spring MVC 和 Servlet 都是 Java Web 开发中常用的技术，它们之间的区别在于：

1. Spring MVC 是基于 Servlet 的框架，它提供了一种 MVC（Model-View-Controller）的架构模式来帮助组织代码，简化开发。而 Servlet 则是 Java Web 应用程序的基础，它允许 Java 开发人员处理 HTTP 请求和响应。
2. Spring MVC 框架提供了更加强大、灵活的控制器(Controller)层，支持不同的数据绑定、校验等功能，同时也提供了 AOP、IOC 等企业级特性。而 Servlet 只提供了基本的请求处理和转发功能。
3. Spring MVC 对前端技术支持更加友好，如内置 JSP 标签库、支持 RESTful 架构等。



Spring MVC 相比于 Servlet 的优势：

1. MVC模式：Spring MVC 提供了基于 MVC 模式的架构，可以将应用程序分为模型(Model)、视图(View)和控制器(Controller)，使代码更容易组织、维护和测试。而 Servlet 则需要手动处理请求和响应，代码比较混乱。
2. 易于扩展：Spring MVC 借助 Spring Framework 强大的 DI（依赖注入）和 AOP（面向切面编程）特性，更容易进行扩展和定制。开发人员可以轻松地添加新的拦截器(Interceptors)、过滤器(filters)等功能。而在Servlet中，需要手动开发这些功能，增加了开发难度和工作量。
3. 更好的灵活性：Spring MVC 支持多种视图技术，包括 JSP、FreeMarker、Velocity、Thymeleaf 等。并且支持 RESTful 架构, 可以方便地创建 REST 风格的应用程序。而Servlet通常使用JSP或者HTML进行前端展示。
4. 更好的测试性：Spring MVC 代码更容易进行单元测试，开发人员可以通过模拟请求来测试控制器的行为。而在 Servlet 中，由于代码比较混乱，测试方法也会比较复杂。
5. 更好的错误处理机制：Spring MVC 提供了更好的异常处理机制，可以集中处理应用程序中出现的各种异常，而Servlet则需要手动捕捉和处理异常。

 