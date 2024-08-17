---
title: AOP - 概述
category: 常用框架
tag:
  - Spring
---





## 介绍

面向切面编程，作为面向对象的一种补充，将公共逻辑（事务管理、日志、缓存、权限控制、限流等）封装成切面，跟业务代码进行分离，可以**减少系统的重复代码**和**降低模块之间的耦合度**。切面就是那些与业务无关，但所有业务模块都会调用的公共逻辑。

先看一个例子：如何给如下UserServiceImpl中所有方法添加进入方法的日志，

```java
public class UserServiceImpl implements IUserService {

    @Override
    public List<User> findUserList() {
        System.out.println("execute method： findUserList");
        return Collections.singletonList(new User("seven", 18));
    }

    @Override
    public void addUser() {
        System.out.println("execute method： addUser");
        // do something
    }

}
```

将记录日志功能解耦为日志切面，它的目标是解耦。进而引出AOP的理念：就是将分散在各个业务逻辑代码中相同的代码通过**横向切割**的方式抽取到一个独立的模块中！

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407211135724.png)

> OOP面向对象编程，针对业务处理过程的实体及其属性和行为进行抽象封装，以获得更加清晰高效的逻辑单元划分。
> AOP则是针对业务处理过程中的切面进行提取，它所面对的是处理过程的某个步骤或阶段，以获得逻辑过程的中各部分之间低耦合的隔离效果。这两种设计思想在目标上有着本质的差异。
> ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407211136020.png)



## AOP相关术语

> 首先要知道，aop不是spring所特有的，同样的，这些术语也不是spring所特有的。是由AOP联盟定义的

1. 切面（Aspect）：切面是**增强**和**切点**的结合，增强和切点共同定义了切面的全部内容。
   多个切面之间的执行顺序如何控制？首先要明确，在“进入”连接点的情况下，最高优先级的增强会先执行；在“退出”连接点的情况下，最高优先级的增强会最后执行。
   1. 通常使用@Order 注解直接定义切面顺序
   2. 实现Ordered 接口重写 getOrder 方法。Ordered.getValue()方法返回值（或者注解值）较低的那个有更高的优先级。

2. 连接点（Join point）：一般**指方法**，在Spring AOP中，一个连接点总是代表一个方法的执行。连接点是在应用执行过程中能够插入切面的一个点。这个点可以是调用方法时、抛出异常时、甚至修改一个字段时。切面代码可以利用这些点插入到应用的正常流程之中，并添加新的行为。当然，连接点也可能是**类初始化、方法执行、方法调用、字段调用或处理异常等**
3. 增强（或称为通知）（Advice）：在AOP术语中，**切面的工作被称为增强**。知实际上是程序运行时要通过Spring AOP框架来触发的代码段。
   1. 前置增强（Before）：在目标方法被调用之前调用增强功能；
   2. 后置增强（After）：在目标方法完成之后调用增强，此时不会关心方法的输出是什么；
   3. 返回增强（After-returning ）：在目标方法成功执行之后调用增强；
   4. 异常增强（After-throwing）：在目标方法抛出异常后调用增强；
   5. 环绕增强（Around）：增强包裹了被增强的方法，在被增强的方法调用之前和调用之后执行自定义的逻辑

4. 切点（Pointcut）：切点的定义会匹配增强所要织入的一个或多个连接点。**通常使用明确的类和方法名称**，或是利**用正则表达式定义所匹配的类和方法名称**来指定这些切点。以AspectJ举例，说白了就可以理解为是execution表达式
5. 引入（Introduction）：引入允许我们向现有类添加新方法或属性。 在AOP中表示为**干什么（引入什么）**；
6. 目标对象（Target Object）： 被一个或者多个切面（aspect）所增强（advise）的对象。它通常是一个代理对象。
7. 织入（Weaving）：织入是**把切面应用到目标对象并创建新的代理对象的过程**。在AOP中表示为**怎么实现的**；织入分为编译期织入、类加载期织入、运行期织入；SpringAOP是在运行期织入

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281427659.png)

execution表达式格式：
```java
execution（modifiers-pattern? ret-type-pattern declaring-type-pattern? name-pattern（param-pattern） throws-pattern?）
```

- ret-type-pattern 返回类型模式, name-pattern名字模式和param-pattern参数模式是必选的， 其它部分都是可选的。返回类型模式决定了方法的返回类型必须依次匹配一个连接点。 使用的最频繁的返回类型模式是`*`，**它代表了匹配任意的返回类型**。
- declaring-type-pattern, 一个全限定的类型名将只会匹配返回给定类型的方法。
- name-pattern 名字模式匹配的是方法名。 可以使用`*`通配符作为所有或者部分命名模式。
- param-pattern 参数模式稍微有点复杂：()匹配了一个不接受任何参数的方法， 而(..)匹配了一个接受任意数量参数的方法（零或者更多）。 模式(`*`)匹配了一个接受一个任何类型的参数的方法。 模式(*,String)匹配了一个接受两个参数的方法，第一个可以是任意类型， 第二个则必须是String类型。

例如：

```java
execution(* com.seven.springframeworkaopannojdk.service.*.*(..))
```





## Spring AOP和AspectJ的关系

AspectJ是一个java实现的AOP框架，它能够对java代码进行AOP编译（一般在编译期进行），让java代码具有AspectJ的AOP功能（当然需要特殊的编译器）。可以这样说AspectJ是目前实现AOP框架中最成熟，功能最丰富的语言，更幸运的是，AspectJ与java程序完全兼容，几乎是无缝关联，因此对于有java编程基础的工程师，上手和使用都非常容易。

1. AspectJ是更强的AOP框架，是实际意义的**AOP标准**；
2. Spring为何不写类似AspectJ的框架？ Spring AOP使用纯Java实现, 它不需要专门的编译过程, 它一个**重要的原则就是无侵入性（non-invasiveness）**; Spring 小组完全有能力写类似的框架，只是Spring AOP从来没有打算通过提供一种全面的AOP解决方案来与AspectJ竞争。Spring的开发小组相信无论是基于代理（proxy-based）的框架如Spring AOP或者是成熟的框架如AspectJ都是很有价值的，他们之间应该是**互补的而不是竞争的关系**。
3. Spring小组喜欢@AspectJ注解风格更胜于Spring XML配置； 所以**在Spring 2.0使用了和AspectJ 5一样的注解，并使用AspectJ来做切入点解析和匹配**。**但是，AOP在运行时仍旧是纯的Spring AOP，并不依赖于AspectJ的编译器或者织入器（weaver）**。
4. Spring 2.5对AspectJ的支持：在一些环境下，增加了对AspectJ的装载时编织支持，同时提供了一个新的bean切入点。

下表总结了 Spring AOP 和 AspectJ 之间的关键区别:

| Spring AOP                                       | AspectJ                                                      |
| ------------------------------------------------ | ------------------------------------------------------------ |
| 在纯 Java 中实现                                 | 使用 Java 编程语言的扩展实现                                 |
| 不需要单独的编译过程                             | 除非设置 LTW，否则需要 AspectJ 编译器 (ajc)                  |
| 只能使用运行时织入                               | 运行时织入不可用。支持编译时、编译后和加载时织入             |
| 功能不强 - 仅支持方法级编织                      | 更强大 - 可以编织字段、方法、构造函数、静态初始值设定项、最终类/方法等......。 |
| 只能在由 Spring 容器管理的 bean 上实现           | 可以在所有域对象上实现                                       |
| 仅支持方法执行切入点                             | 支持所有切入点                                               |
| 代理是由目标对象创建的, 并且切面应用在这些代理上 | 在执行应用程序之前 (在运行时) 前, 各方面直接在代码中进行织入 |
| 比 AspectJ 慢多了                                | 更好的性能                                                   |
| 易于学习和应用                                   | 相对于 Spring AOP 来说更复杂                                 |



## AOP的实现原理

AOP有两种实现方式：静态代理和动态代理。

### 静态代理

静态代理分为：编译时织入（特殊编译器实现）、类加载时织入（特殊的类加载器实现）。

代理类在编译阶段生成，在编译阶段将增强织入Java字节码中，也称编译时增强。**AspectJ使用的是静态代理**。

缺点：代理对象需要与目标对象实现一样的接口，并且实现接口的方法，会有冗余代码。同时，一旦接口增加方法，目标对象与代理对象都要维护。

### 动态代理

动态代理：代理类在程序运行时创建，AOP框架不会去修改字节码，而是在内存中临时生成一个代理对象，在运行期间对业务方法进行增强，不会生成新类



### Spring的AOP实现原理

而Spring的AOP的实现就是通过**动态代理**实现的。

如果为Spring的某个bean配置了切面，那么Spring在创建这个bean的时候，实际上创建的是这个bean的一个代理对象，后续对bean中方法的调用，实际上调用的是代理类重写的代理方法。而Spring的AOP使用了两种动态代理，分别是JDK的动态代理，以及CGLib的动态代理。

- 如果目标类实现了接口，Spring AOP会选择使用JDK动态代理目标类。代理类根据目标类实现的接口动态生成，不需要自己编写，生成的动态代理类和目标类都实现相同的接口。JDK动态代理的核心是`InvocationHandler`接口和`Proxy`类。
- 如果目标类没有实现接口，那么Spring AOP会选择使用CGLIB来动态代理目标类。CGLIB（Code Generation Library）可以在运行时动态生成类的字节码，动态创建目标类的子类对象，在子类对象中增强目标类。CGLIB是**通过继承的方式**做的动态代理，因此CGLIB存在的束：**类是final的**，或是方法是**final**的，或是方法是**private**，或是**静态方法**，也就是无法被子类实现的方法都无法使用CGLIB实现代理。



那么什么时候采用哪种动态代理呢？

1. 如果目标对象实现了接口，默认情况下会采用JDK的动态代理实现AOP
2. 如果目标对象实现了接口，可以强制使用CGLIB实现AOP
3. 如果目标对象没有实现了接口，必须采用CGLIB库



## AOP的配置方式

### 基于XML

Spring提供了使用"aop"命名空间来定义一个切面，我们来看个[例子](https://github.com/Seven-97/Spring-Demo/tree/master/04-spring-framework-aop-xml)

- **定义目标类**

```java
public class AopDemoServiceImpl {

    public void doMethod1() {
        System.out.println("AopDemoServiceImpl.doMethod1()");
    }

    public String doMethod2() {
        System.out.println("AopDemoServiceImpl.doMethod2()");
        return "hello world";
    }

    public String doMethod3() throws Exception {
        System.out.println("AopDemoServiceImpl.doMethod3()");
        throw new Exception("some exception");
    }
}
```

- **定义切面类**

```java
public class LogAspect {

    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("-----------------------");
        System.out.println("环绕通知: 进入方法");
        Object o = pjp.proceed();
        System.out.println("环绕通知: 退出方法");
        return o;
    }

    public void doBefore() {
        System.out.println("前置通知");
    }

    public void doAfterReturning(String result) {
        System.out.println("后置通知, 返回值: " + result);
    }

    public void doAfterThrowing(Exception e) {
        System.out.println("异常通知, 异常: " + e.getMessage());
    }

    public void doAfter() {
        System.out.println("最终通知");
    }

}
```

- **XML配置AOP**

```xml
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

    <context:component-scan base-package="com.seven.springframeworkaopxml" />

    <aop:aspectj-autoproxy/>

    <!-- 目标类 -->
    <bean id="demoService" class="com.seven.springframeworkaopxml.service.AopDemoServiceImpl">
        <!-- configure properties of bean here as normal -->
    </bean>

    <!-- 切面 -->
    <bean id="logAspect" class="com.seven.springframeworkaopxml.aspect.LogAspect">
        <!-- configure properties of aspect here as normal -->
    </bean>

    <aop:config>
        <!-- 配置切面 -->
        <aop:aspect ref="logAspect">
            <!-- 配置切入点 -->
            <aop:pointcut id="pointCutMethod" expression="execution(* com.seven.springframeworkaopxml.service.*.*(..))"/>
            <!-- 环绕通知 -->
            <aop:around method="doAround" pointcut-ref="pointCutMethod"/>
            <!-- 前置通知 -->
            <aop:before method="doBefore" pointcut-ref="pointCutMethod"/>
            <!-- 后置通知；returning属性：用于设置后置通知的第二个参数的名称，类型是Object -->
            <aop:after-returning method="doAfterReturning" pointcut-ref="pointCutMethod" returning="result"/>
            <!-- 异常通知：如果没有异常，将不会执行增强；throwing属性：用于设置通知第二个参数的的名称、类型-->
            <aop:after-throwing method="doAfterThrowing" pointcut-ref="pointCutMethod" throwing="e"/>
            <!-- 最终通知 -->
            <aop:after method="doAfter" pointcut-ref="pointCutMethod"/>
        </aop:aspect>
    </aop:config>

</beans>
```

- **测试类**

```java
public static void main(String[] args) {
    // create and configure beans
    ApplicationContext context = new ClassPathXmlApplicationContext("aspects.xml");

    // retrieve configured instance
    AopDemoServiceImpl service = context.getBean("demoService", AopDemoServiceImpl.class);

    // use configured instance
    service.doMethod1();
    service.doMethod2();
    try {
        service.doMethod3();
    } catch (Exception e) {
        // e.printStackTrace();
    }
}
```



### 基于AspectJ注解(直接写表达式)

基于XML的声明式AspectJ存在一些不足，需要在Spring配置文件配置大量的代码信息，为了解决这个问题，Spring 使用了@AspectJ框架为AOP的实现提供了一套注解。

| 注解名称        | 解释                                                         |
| --------------- | ------------------------------------------------------------ |
| @Aspect         | 用来定义一个切面。                                           |
| @pointcut       | 用于定义切入点表达式。在使用时还需要定义一个包含名字和任意参数的方法签名来表示切入点名称，这个方法签名就是一个返回值为void，且方法体为空的普通方法。 |
| @Before         | 用于定义前置通知，相当于BeforeAdvice。在使用时，通常需要指定一个value属性值，该属性值用于指定一个切入点表达式(可以是已有的切入点，也可以直接定义切入点表达式)。 |
| @AfterReturning | 用于定义后置通知，相当于AfterReturningAdvice。在使用时可以指定pointcut / value和returning属性，其中pointcut / value这两个属性的作用一样，都用于指定切入点表达式。 |
| @Around         | 用于定义环绕通知，相当于MethodInterceptor。在使用时需要指定一个value属性，该属性用于指定该通知被植入的切入点。 |
| @After-Throwing | 用于定义异常通知来处理程序中未处理的异常，相当于ThrowAdvice。在使用时可指定pointcut / value和throwing属性。其中pointcut/value用于指定切入点表达式，而throwing属性值用于指定-一个形参名来表示Advice方法中可定义与此同名的形参，该形参可用于访问目标方法抛出的异常。 |
| @After          | 用于定义最终final 通知，不管是否异常，该通知都会执行。使用时需要指定一个value属性，该属性用于指定该通知被植入的切入点。 |
| @DeclareParents | 用于定义引介通知，相当于IntroductionInterceptor (不要求掌握)。 |





#### 基于JDK动态代理

[基于JDK动态代理例子源码点这里](https://github.com/Seven-97/Spring-Demo/tree/master/05-spring-framework-aop-anno-jdk)

- 定义接口

```java
public interface IJdkProxyService {

    void doMethod1();

    String doMethod2();

    String doMethod3() throws Exception;
}
```

- 实现类

```java
@Service
public class JdkProxyDemoServiceImpl implements IJdkProxyService {

    @Override
    public void doMethod1() {
        System.out.println("JdkProxyServiceImpl.doMethod1()");
    }

    @Override
    public String doMethod2() {
        System.out.println("JdkProxyServiceImpl.doMethod2()");
        return "hello world";
    }

    @Override
    public String doMethod3() throws Exception {
        System.out.println("JdkProxyServiceImpl.doMethod3()");
        throw new Exception("some exception");
    }
}
```

- **定义切面**

```java
@EnableAspectJAutoProxy
@Component
@Aspect
public class LogAspect {

    /**
     * define point cut.
     */
    @Pointcut("execution(* com.seven.springframeworkaopannojdk.service.*.*(..))")
    private void pointCutMethod() {
    }


    /**
     * 环绕通知.
     *
     * @param pjp pjp
     * @return obj
     * @throws Throwable exception
     */
    @Around("pointCutMethod()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("-----------------------");
        System.out.println("环绕通知: 进入方法");
        Object o = pjp.proceed();
        System.out.println("环绕通知: 退出方法");
        return o;
    }

    /**
     * 前置通知.
     */
    @Before("pointCutMethod()")
    public void doBefore() {
        System.out.println("前置通知");
    }


    /**
     * 后置通知.
     *
     * @param result return val
     */
    @AfterReturning(pointcut = "pointCutMethod()", returning = "result")
    public void doAfterReturning(String result) {
        System.out.println("后置通知, 返回值: " + result);
    }

    /**
     * 异常通知.
     *
     * @param e exception
     */
    @AfterThrowing(pointcut = "pointCutMethod()", throwing = "e")
    public void doAfterThrowing(Exception e) {
        System.out.println("异常通知, 异常: " + e.getMessage());
    }

    /**
     * 最终通知.
     */
    @After("pointCutMethod()")
    public void doAfter() {
        System.out.println("最终通知");
    }

}
```

- APP启动

```java
public class App {
    public static void main(String[] args) {
        // create and configure beans
        ApplicationContext context = new AnnotationConfigApplicationContext("com.seven.springframeworkaopannojdk");

        // retrieve configured instance
        IJdkProxyService service = context.getBean(IJdkProxyService.class);

        // use configured instance
        service.doMethod1();
        service.doMethod2();
        try {
            service.doMethod3();
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }
}
```





#### 非接口使用Cglib代理

[基于Cglib代理例子源码点这里](https://github.com/Seven-97/Spring-Demo/tree/master/06-spring-framework-aop-anno-cglib)

- **类定义**

```java
@Service
public class CglibProxyDemoServiceImpl {

    public void doMethod1() {
        System.out.println("CglibProxyDemoServiceImpl.doMethod1()");
    }

    public String doMethod2() {
        System.out.println("CglibProxyDemoServiceImpl.doMethod2()");
        return "hello world";
    }

    public String doMethod3() throws Exception {
        System.out.println("CglibProxyDemoServiceImpl.doMethod3()");
        throw new Exception("some exception");
    }
}
```

- **切面定义**

和上面相同

- APP启动

```java
public class App {
    public static void main(String[] args) {
        // create and configure beans
        ApplicationContext context = new AnnotationConfigApplicationContext("com.seven.springframeworkaopannocglib");

        // cglib proxy demo
        CglibProxyDemoServiceImpl service = context.getBean(CglibProxyDemoServiceImpl.class);
        service.doMethod1();
        service.doMethod2();
        try {
            service.doMethod3();
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }
}
```



#### 使用注解装配AOP

上面使用AspectJ的注解，并配合一个复杂的`execution(* com.seven.springframeworkaopannojdk.service.*.*(..))` 语法来定义应该如何装配AOP。还有另一种方式，则是使用注解来装配AOP，这两者一般存在与不同的应用场景中：

- 对于业务开发来说，一般使用  注解的方式来装配AOP，因为如果要使用AOP进行增强，业务开发就需要配置注解，业务能够很好的感知到这个方法(这个类)进行了增强。如果使用 表达式来装配AOP，当后续新增Bean，如果不清楚现有的AOP装配规则，容易被强迫装配，而在开发时未感知到，导致出现线上故障。例如，Spring提供的`@Transactional`就是一个非常好的例子。如果自己写的Bean希望在一个数据库事务中被调用，就标注上`@Transactional`。
- 对于基础架构开发来说，无需业务感知到增强了什么方法，则可以使用表达式的方式来装配AOP



- 定义注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogAspectAnno {

}
```

- 修改切面类，使用注解的方式定义

```java
@EnableAspectJAutoProxy
@Component
@Aspect
public class LogAspect {

    @Around("@annotation(logaspectanno)") //注意,括号里为logaspectanno,而不是LogAspectAnno
    public Object doAround(ProceedingJoinPoint pjp, LogAspectAnno logaspectanno) throws Throwable {
        System.out.println("-----------------------");
        System.out.println("环绕通知: 进入方法");
        Object o = pjp.proceed();
        System.out.println("环绕通知: 退出方法");
        return o;
    }
    
}
```

- 修改实现类，这里只对 doMethod1 方法装配AOP

```java
@Service
public class CglibProxyDemoServiceImpl {

    @LogAspectAnno()
    public void doMethod1() {
        System.out.println("CglibProxyDemoServiceImpl.doMethod1()");
    }

    public String doMethod2() {
        System.out.println("CglibProxyDemoServiceImpl.doMethod2()");
        return "hello world";
    }
}

@Service
public class JdkProxyDemoServiceImpl implements IJdkProxyService {

    @LogAspectAnno
    @Override
    public void doMethod1() {
        System.out.println("JdkProxyServiceImpl.doMethod1()");
    }

    @Override
    public String doMethod2() {
        System.out.println("JdkProxyServiceImpl.doMethod2()");
        return "hello world";
    }
}
```

- APP类

```java
// create and configure beans
ApplicationContext context = new AnnotationConfigApplicationContext("com.seven.springframeworkaopannotation");

// cglib proxy demo
CglibProxyDemoServiceImpl service1 = context.getBean(CglibProxyDemoServiceImpl.class);
service1.doMethod1();
service1.doMethod2();

IJdkProxyService service2 = context.getBean(IJdkProxyService.class);
service2.doMethod1();
service2.doMethod2();
```



- 输出：

```java
-----------------------
环绕通知: 进入方法
CglibProxyDemoServiceImpl.doMethod1()
环绕通知: 退出方法
CglibProxyDemoServiceImpl.doMethod2()
-----------------------
环绕通知: 进入方法
JdkProxyServiceImpl.doMethod1()
环绕通知: 退出方法
JdkProxyServiceImpl.doMethod2()
```

可以看到，只有doMethod1方法被增强了，doMethod2没有被增强，就是因为@LogAspectAnno 只注解了 doMethod1() 方法，从而实现更精细化的控制，是业务感知到这个方法是被增强了。









<!-- @include: @article-footer.snippet.md -->     









