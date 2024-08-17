---
title: MVC - 概述
category: 常用框架
tag:
  - SpringMVC
---





## 前言

MVC英文是Model View Controller，是模型(model)－视图(view)－控制器(controller)的缩写，一种软件设计规范，本质上也是一种解耦。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407212156709.png)

- **Model**（模型）是应用程序中用于处理应用程序数据逻辑的部分。通常模型对象负责在数据库中存取数据。
- **View**（视图）是应用程序中处理数据显示的部分。通常视图是依据模型数据创建的。
- **Controller**（控制器）是应用程序中处理用户交互的部分。通常控制器负责从视图读取数据，控制用户输入，并向模型发送数据。

而Spring Web MVC 则是一种基于Java 的实现了Web MVC 设计模式的请求驱动类型的轻量级Web 框架，即使用了MVC 架构模式的思想，将 web 层进行职责解耦，基于请求驱动指的就是使用请求-响应模型，框架的目的就是为了简化开 发，Spring Web MVC 也是要简化我们日常Web 开发的。
Spring MVC 下一般把后端项目分为 Service 层（处理业务）、Dao 层（数据库操作）、Entity 层（实体类）、Controller 层(控制层，返回数据给前台页面)。



常用组件：

- 前端控制器（DispatcherServlet）：接收用户请求，给用户返回结果。

- 处理器映射器（HandlerMapping）：根据请求的url路径，通过注解或者xml配置，寻找匹配的Handler。

- 处理器适配器（HandlerAdapter）：Handler 的适配器，调用 handler 的方法处理请求。

- 处理器（Handler）：执行相关的请求处理逻辑，并返回相应的数据和视图信息，将其封装到ModelAndView对象中。

- 视图解析器（ViewResolver）：将逻辑视图名解析成真正的视图View。

- 视图（View）：接口类，实现类可支持不同的View类型（JSP、FreeMarker、Excel等）







## MVC 拦截器

Spring MVC 拦截器对应HandlerInterctor接口，该接口位于org.springframework.web.servlet的包中，定义了三个方法，若要实现该接口，就要实现其三个方法：

1. 前置处理（preHandle()方法）：该方法在执行控制器方法之前执行。返回值为Boolean类型，如果返回false，表示拦截请求，不再向下执行，如果返回true，表示放行，程序继续向下执行（如果后面没有其他Interceptor，就会执行controller方法）。所以此方法可对请求进行判断，决定程序是否继续执行，或者进行一些初始化操作及对请求进行预处理。
2. 后置处理（postHandle()方法）：该方法在执行控制器方法调用之后，且在返回ModelAndView之前执行。由于该方法会在DispatcherServlet进行返回视图渲染之前被调用，所以此方法多被用于处理返回的视图，可通过此方法对请求域中的模型和视图做进一步的修改。
3. 已完成处理（afterCompletion()方法）：该方法在执行完控制器之后执行，由于是在Controller方法执行完毕后执行该方法，所以该方法适合进行一些资源清理，记录日志信息等处理操作。

可以通过拦截器进行权限检验，参数校验，记录日志等操作



### MVC 的Interctor和 Filter 过滤器的区别

- 功能相同：Interctor和 Filter 都能实现相应的功能

- 容器不同：Interctor构建在 Spring MVC 体系中；Filter 构建在 Servlet 容器之上

- 拦截内容不同：Filter对所有访问进行增强，Interctor仅对MVC访问进行增强

- 使用便利性不同：Interctor提供了三个方法，分别在不同的时机执行；过滤器仅提供一个方法



### 多拦截器执行顺序

- 当配置多个拦截器时，会形成拦截器链

- 拦截器的运行顺序参照拦截器添加顺序为准，即addInterctor的顺序

- 当拦截器中出现对原始处理器的拦截，后面的拦截器均终止运行

- 当拦截器运行中断，仅运行配置在前面的拦截器afterCompletion

流程解析看下图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281539349.png)



## MVC案例 

### 基于webxml

 [示例源码点击这里](https://github.com/Seven-97/Spring-Demo/tree/master/07-spring-mvc-helloworld)

#### maven引入

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.seven</groupId>
        <artifactId>spring-demo</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <artifactId>07-spring-mvc-helloworld</artifactId>
    <packaging>war</packaging>
    <name>07-spring-mvc-helloworld Maven Webapp</name>
    <url>http://maven.apache.org</url>
    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <spring.version>5.3.37</spring.version>
        <servlet.version>4.0.1</servlet.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webmvc</artifactId>
            <version>${spring.version}</version>
        </dependency>
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <version>${servlet.version}</version>
        </dependency>

        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>jstl</artifactId>
            <version>1.2</version>
        </dependency>
        <dependency>
            <groupId>taglibs</groupId>
            <artifactId>standard</artifactId>
            <version>1.1.2</version>
        </dependency>
    </dependencies>

    <build>
        <finalName>07-spring-mvc-helloworld</finalName>
    </build>
</project>
```

#### 业务代码编写

- entity的User类

```java
@Data
@AllArgsConstructor
public class User {

    private String name;

    private int age;
}
```



- dao层

```java
@Repository
public class UserDaoImpl {

    public List<User> findUserList() {
        return Collections.singletonList(new User("seven", 18));
    }

}
```



- service层

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



- controller层

```java
@Controller
public class UserController {

    @Autowired
    private UserServiceImpl userService;


    @RequestMapping("/user")
    public ModelAndView list(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("dateTime", new Date());
        modelAndView.addObject("userList", userService.findUserList());
        modelAndView.setViewName("userList"); // views目录下userList.jsp
        return modelAndView;
    }

}
```



#### webapp下的web.xml

```xml
<!DOCTYPE web-app PUBLIC
        "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
        "http://java.sun.com/dtd/web-app_2_3.dtd" >

<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd"
         version="3.1">
    <display-name>Archetype Created Web Application</display-name>

    <servlet>
        <servlet-name>springmvc-demo</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <!-- 通过初始化参数指定SpringMVC配置文件的位置和名称 -->
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>classpath:springmvc.xml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>springmvc-demo</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>

    <filter>
        <filter-name>encodingFilter</filter-name>
        <filter-class>org.springframework.web.filter.CharacterEncodingFilter</filter-class>
        <init-param>
            <param-name>encoding</param-name>
            <param-value>UTF-8</param-value>
        </init-param>
        <init-param>
            <param-name>forceEncoding</param-name>
            <param-value>true</param-value>
        </init-param>
    </filter>

    <filter-mapping>
        <filter-name>encodingFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

</web-app>
```



#### springmvc.xml

web.xml中配置初始化参数contextConfigLocation，路径是classpath:springmvc.xml，因此文件直接创建在resources目录下

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd
       http://www.springframework.org/schema/mvc http://www.springframework.org/schema/mvc/spring-mvc.xsd">

    <!-- 扫描注解 -->
    <context:component-scan base-package="com.seven.springmvchelloworld"/>

    <!-- 静态资源处理 -->
    <mvc:default-servlet-handler/>

    <!-- 开启注解 -->
    <mvc:annotation-driven/>

    <!-- 视图解析器 -->
    <bean id="jspViewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="viewClass" value="org.springframework.web.servlet.view.JstlView"/>
        <property name="prefix" value="/views/"/>
        <property name="suffix" value=".jsp"/>
    </bean>

</beans>
```

#### JSP视图

创建userList.jsp

```jsp
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>User List</title>

    <!-- Bootstrap -->
    <link rel="stylesheet" href="//cdn.bootcss.com/bootstrap/3.3.5/css/bootstrap.min.css">

</head>
<body>
<div class="container">
    <c:if test="${!empty userList}">
        <table class="table table-bordered table-striped">
            <tr>
                <th>Name</th>
                <th>Age</th>
            </tr>
            <c:forEach items="${userList}" var="user">
                <tr>
                    <td>${user.name}</td>
                    <td>${user.age}</td>
                </tr>
            </c:forEach>
        </table>
    </c:if>
</div>
</body>
</html>
```

之后就是使用tomcat部署测试了，这块就不说了



### 纯注解版

无需配置xml文件，依靠注解和配置类完成配置，注意需要注意满足sevlet3.0规范

具体源码[点击这里](https://github.com/Seven-97/Spring-Demo/tree/master/08-spring-mvc-helloworld-anno)

这个不做过多讲解，真实项目的用得较少。因为若是老项目，就是基于webxml的，若是新项目，则直接上springboot了。



<!-- @include: @article-footer.snippet.md -->     



