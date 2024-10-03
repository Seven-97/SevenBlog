---
title: 如何控制Bean的加载顺序
category: 常用框架
tag:
  - Spring
head:
  - - meta
    - name: keywords
      content: spring,spring ioc,IOC,依赖注入,Bean的加载顺序
  - - meta
    - name: description
      content: 全网最全的的Spring知识点总结，让天下没有难学的八股文！
---



## 写在前面

`springboot`遵从约定大于配置的原则，极大程度的解决了配置繁琐的问题。在此基础上，又提供了spi机制，用`spring.factories`可以完成一个小组件的自动装配功能。

在一般业务场景，可能是不需要关心一个bean是如何被注册进spring容器的，只需要把需要注册进容器的bean声明为`@Component`即可，因为spring会自动扫描到这个Bean完成初始化并加载到spring上下文容器。

但是，如果加载Bean的过程中**部分Bean和Bean之间存在依赖关系**，也就是说`Bean A`的加载需要等待`Bean B`加载完成之后才能进行；或者你正在开发某个中间件需要完成自动装配时，你会声明自己的Configuration类，但是可能你面对的是好几个有互相依赖的Bean，如果不加以控制，这时候可能会报找不到依赖的错误。

而Spring框架在没有明确指定加载顺序的情况下是无法按照业务逻辑预期的顺序进行Bean加载，所以需要Spring框架提供能让开发人员显示地指定Bean加载顺序的能力。





## 几个误区

在正式说如何控制加载顺序之前，先说2个误区：

- 在标注了`@Configuration`的类中，写在前面的@Bean一定会被先注册吗？

这个不存在的，spring在xml的时代，也不存在写在前面一定会被先加载的逻辑。因为xml不是渐进的加载，而是全部parse好，再进行依赖分析和注册。到了springboot中，只是省去了xml被parse成spring内部对象的这一过程，但是加载方式并没有大的改变。



- 利用`@Order`这个标注就一定能进行加载顺序的控制吗？

严格的说，不是所有的Bean都可以通过`@Order`这个标注进行顺序的控制。因为把`@Order`这个标注加在普通的方法上或者类上是没有影响的，

那`@Order`能控制哪些bean的加载顺序呢？官方解释：

```java
{@code @Order} defines the sort order for an annotated component. Since Spring 4.0, annotation-based ordering is supported for many kinds of components in Spring, even for collection injection where the order values of the target components are taken into account (either from their target class or from their {@code @Bean} method). While such order values may influence priorities at injection points, please be aware that they do not influence singleton startup order which is an orthogonal concern determined by dependency relationships and {@code @DependsOn} declarations (influencing a runtime-determined dependency graph).
```

最开始`@Order`注解用于切面的优先级指定；在 4.0 之后对它的功能进行了增强，支持集合的注入时，指定集合中 bean 的顺序，并且特别指出了，它对于单实例的 bean 之间的顺序，没有任何影响。目前用的比较多的有以下3点：

- 控制AOP的类的加载顺序，也就是被`@Aspect`标注的类
- 控制`ApplicationListener`实现类的加载顺序
- 控制`CommandLineRunner`实现类的加载顺序

使用详情请看后文



## 如何控制

### @Conditional 条件注解家族

- @ConditionalOnClass：当类路径下存在指定的类时，配置类才会生效。

```java
@Configuration
// 当类路径下存在指定的类时，配置类才会生效。
@ConditionalOnClass(name = "com.example.SomeClass")
public class MyConfiguration {
	// ...
}
```



- @ConditionalOnMissingClass：当类路径下不存在指定的类时，配置类才会生效。
- @ConditionalOnBean：当容器中存在指定的Bean时，配置类才会生效。
- @ConditionalOnMissingBean：当容器中不存在指定的Bean时，配置类才会生效。





### @DependsOn

`@DependsOn`注解可以用来控制bean的创建顺序，该注解用于声明当前bean依赖于另外一个bean。所依赖的bean会被容器确保在当前bean实例化之前被实例化。

`@DependsOn`的使用：

- 直接或者间接标注在带有`@Component`注解的类上面;
- 直接或者间接标注在带有`@Bean`注解的方法上面;
- 使用`@DependsOn`注解到类层面仅仅在使用 component-scanning 方式时才有效，如果带有`@DependsOn`注解的类通过XML方式使用，该注解会被忽略，`<bean depends-on="..."/>`这种方式会生效。

示例：

```java
@Configuration
public class BeanOrderConfiguration {
 
    @Bean
    @DependsOn("beanB")
    public BeanA beanA(){
        System.out.println("bean A init");
        return new BeanA();
    }
 
    @Bean
    public BeanB beanB(){
        System.out.println("bean B init");
        return new BeanB();
    }
 
    @Bean
    @DependsOn({"beanD","beanE"})
    public BeanC beanC(){
        System.out.println("bean C init");
        return new BeanC();
    }
 
    @Bean
    @DependsOn("beanE")
    public BeanD beanD(){
        System.out.println("bean D init");
        return new BeanD();
    }
 
    @Bean
    public BeanE beanE(){
        System.out.println("bean E init");
        return new BeanE();
    }
}
```

以上代码bean的加载顺序为：

```java
bean B init
bean A init
bean E init
bean D init
bean C init
```



### 参数注入

在`@Bean`标注的方法上，如果传入了参数，springboot会自动会为这个参数在spring上下文里寻找这个类型的引用。并先初始化这个类的实例。

利用此特性，我们也可以控制bean的加载顺序。

示例：

```java
@Bean
public BeanA beanA(BeanB demoB){
  System.out.println("bean A init");
  return new BeanA();
}
 
 
@Bean
public BeanB beanB(){
  System.out.println("bean B init");
  return new BeanB();
}
```

以上结果，beanB先于beanA被初始化加载。

需要注意的是，springboot会按类型去寻找。如果这个类型有多个实例被注册到spring上下文，那就需要加上`@Qualifier("Bean的名称")`来指定



### 利用bean的生命周期中的扩展点

在spring体系中，从容器到Bean实例化&初始化都是有生命周期的，并且提供了很多的扩展点，允许在这些步骤时进行逻辑的扩展。

这些可扩展点的加载顺序由spring自己控制，大多数是无法进行干预的。可以利用这一点，扩展spring的扩展点。在相应的扩展点加入自己的业务初始化代码。从来达到顺序的控制。

具体关于spring容器中大部分的可扩展点的分析，之前已经写了一篇文章详细介绍了：[Spring&SpringBoot中所有的扩展点](https://www.seven97.top/framework/spring/extentions-use.html)





### 实现`Ordered/PriorityOrdered`接口/注解

在Spring中提供了如下的方法来进行Bean加载顺序的控制：

- 实现`Ordered/PriorityOrdered`接口，重写order方法
- 使用`@Order/@Priority`注解，`@Order`注解可以用于方法级别，而`@Priority`注解则不行；

针对**自定义的Bean**而言，上述的方式都可以实现Bean加载顺序的控制。无论是实现接口的方式还是使用注解的方式，**值设置的越小则优先级越高**，而通过实现PriorityOrdered接口或者使用@Priority注解的Bean时其加载优先级会**高于**实现Ordered接口或者使用@Order注解的Bean。

需要注意的是，使用上述方式只会改变实现同一接口Bean加载到集合*（比如List、Set等）*中的顺序（或者说优先级），但是这种方式并**不会影响到Spring应用上下文启动时不同Bean的初始化顺序**（startup order）。



- 错误案例：以下案例代码是无法指定配置顺序的

```java
@Component
@Order(1)
public class BeanA {
    // BeanA的定义
}

@Component
@Order(2)
public class BeanB {
    // BeanB的定义
}
```



- 正确使用案例：

首先定义两个 Bean 实现同一个接口，并添加上@Order注解。

```java
public interface IBean {
}

@Order(2)
@Component
public class AnoBean1 implements IBean {

    private String name = "ano order bean 1";

    public AnoBean1() {
        System.out.println(name);
    }
}

@Order(1)
@Component
public class AnoBean2 implements IBean {

    private String name = "ano order bean 2";

    public AnoBean2() {
        System.out.println(name);
    }
}
```

然后在一个测试 bean 中，注入`IBean`的列表，我们需要测试这个列表中的 Bean 的顺序是否和定义的`@Order`规则一致

```java
@Component
public class AnoTestBean {

    public AnoTestBean(List<IBean> anoBeanList) {
        for (IBean bean : anoBeanList) {
            System.out.println("in ano testBean: " + bean.getClass().getName());
        }
    }
}
```











### @AutoConfigureOrder

这个注解用来指定配置文件的加载顺序。但是在实际测试中发现，以下这样使用是不生效的：

```java
@Configuration
@AutoConfigureOrder(2)
public class BeanOrderConfiguration1 {
    @Bean
    public BeanA beanA(){
        System.out.println("bean A init");
        return new BeanA();
    }
}
 
 
@Configuration
@AutoConfigureOrder(1)
public class BeanOrderConfiguration2 {
    @Bean
    public BeanB beanB(){
        System.out.println("bean B init");
        return new BeanB();
    }
}
```

无论你2个数字填多少，都不会改变其加载顺序结果。那这个`@AutoConfigureOrder`到底是如何使用的呢？

@AutoConfigureOrder适用于外部依赖的包中 AutoConfig 的顺序，而不能用来指定本包内的顺序。能被你工程内部scan到的包，都是内部的Configuration，而spring引入外部的Configuration，都是通过spring特有的spi文件：`spring.factories`

换句话说，`@AutoConfigureOrder`能改变`spring.factories`中的`@Configuration`的顺序。

具体使用方式：

```java
@Configuration
@AutoConfigureOrder(10)
public class BeanOrderConfiguration1 {
    @Bean
    public BeanA beanA(){
        System.out.println("bean A init");
        return new BeanA();
    }
}
 
@Configuration
@AutoConfigureOrder(1)
public class BeanOrderConfiguration2 {
    @Bean
    public BeanB beanB(){
        System.out.println("bean B init");
        return new BeanB();
    }
}
```

`spring.factories`：

```java
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.example.demo.BeanOrderConfiguration1,\
  com.example.demo.BeanOrderConfiguration2
```



## 总结

其实在工作中，我相信很多人碰到过复杂的依赖关系的bean加载，把这种不确定性交给spring去做，还不如我们自己去控制，这样在阅读代码的时候 ，也能轻易看出bean之间的依赖先后顺序。





<!-- @include: @article-footer.snippet.md -->     