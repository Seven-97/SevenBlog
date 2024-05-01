---
title: IOC - 概述
category: 常用框架
tag:
  - Spring
---



## 介绍

IoC（Inversion of Control:控制反转） 是一种设计思想，而不是一个具体的技术实现。IoC 的思想就是将原本在程序中手动创建对象的控制权，交由 Spring 框架来管理，由Spring容器管理bean的整个生命周期。通俗来说就是**IoC是设计思想，DI是实现方式。**

通过反射实现对其他对象的控制，包括初始化、创建、销毁等，解放手动创建对象的过程，同时降低类之间的耦合度。

在 Spring 中， IoC 容器是 Spring 用来实现 IoC 的载体， **IoC 容器实际上就是个 Map（key，value），Map 中存放的是各种对象，根据BeanName或者Type获取对象**。



## IOC的好处

ioc的思想最核心的地方在于，资源不由使用资源者管理，而由不使用资源的第三方管理，这可以带来很多好处。

1. 资源集中管理，实现资源的可配置和易管理。
2. 降低类之间的耦合度。

比如在实际项目中一个 Service 类可能依赖了很多其他的类，假如我们需要实例化这个 Service，可能要每次都要搞清这个 Service 所有底层类的构造函数，这就变得复杂了。而如果使用 IoC 的话，只需要配置好，然后在需要的地方引用就行了，这大大增加了项目的可维护性且降低了开发难度。



## 依赖注入DI

其原理是将对象的依赖关系由外部容器来管理和注入。这样，对象只需要关注自身的核心功能，而不需要关心如何获取依赖对象。它的目的是解耦组件之间的依赖关系，提高代码的灵活性、可维护性和可测试性。

在Spring创建对象的过程中，把对象依赖的属性注入到对象中。

依赖注入主要有三种方式：构造器注入，Setter方式注入(属性注入)、接口注入(比较少用，因为需要对象实现额外的接口)



### 构造器注入

一般推荐构造器注入，为什么？Spring文档里的解释如下：

> The Spring team generally advocates constructor injection as it enables one to implement application components as immutable objects and to ensure that required dependencies are not null. Furthermore constructor-injected components are always returned to client (calling) code in a fully initialized state.

翻译一下就是：这个构造器注入的方式能够保证注入的组件不可变，并且确保需要的依赖不为空。此外，构造器注入的依赖总是能够在返回客户端（组件）代码的时候保证完全初始化的状态。

- **依赖不可变**：其实说的就是final关键字。

- **依赖不为空**：（省去了我们对其检查）：当要实例化UserServiceImpl的时候，由于自己实现了有参数的构造函数，所以不会调用默认构造函数，那么就需要Spring容器传入所需要的参数，所以就两种情况：1、有该类型的参数->传入，OK 。2：无该类型的参数->报错。

- **完全初始化的状态**：这个可以跟上面的依赖不为空结合起来，向构造器传参之前，要确保注入的内容不为空，那么肯定要调用依赖组件的构造方法完成实例化。而在Java类加载实例化的过程中，构造方法是最后一步（之前如果有父类先初始化父类，然后自己的成员变量，最后才是构造方法），所以返回来的都是初始化之后的状态。

如果使用setter注入，缺点显而易见，对于IOC容器以外的环境，除了使用反射来提供它需要的依赖之外，无法复用该实现类。而且将一直是个潜在的隐患，因为你不调用将一直无法发现NPE的存在

```java
// 这里只是模拟一下，正常来说我们只会暴露接口给客户端，不会暴露实现。 
UserServiceImpl userService =newUserServiceImpl(); userService.findUserList();// -> NullPointerException, 潜在的隐患
```





### Bean注入的七种方式

1. 使用xml方式来声明Bean的定义，Spring容器在启动会加载并解析这个xml，把bean装载到IOC容器中

2. 使用@CompontScan注解来扫描声明了@Controller、@Service、@Repository、@Component注解的类

3. 使用@Configuration注解声明配置类，并使用@Bean注解实现Bean的定义，这种方式其实是xml配置方式的一种演 变，是Spring迈入到无配置化时代的里程碑

4. 使用@Import注解，导入配置类或者普通的Bean

5. 使用FactoryBean工厂bean, 动态构建一个Bean实例，Spring Cloud OpenFeign 里面的动态代理实例就是使用FactoryBean来实现的

6. 实现ImportBeanDefinitionRegistrar接口，可以动态注入Bean实例。这个在Spring Boot里面的启动注解有用到

7. 实现ImportSelector接口，动态批量注入配置类或者Bean对象，这个在Spring Boot里面的自动装配机制里面有用到

   

## IOC的结构设计

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281027710.png)

Spring Bean的创建是典型的**工厂模式**，这一系列的Bean工厂，也即IOC容器为开发者管理对象间的依赖关系提供了很多便利和基础服务，在Spring中有许多的IOC容器的实现供用户选择和使用，这是IOC容器的基础；在顶层的结构设计主要围绕着BeanFactory和xxxRegistry进行：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281026702.png)

### BeanFactory

- BeanFactory： 工厂模式定义了IOC容器的基本功能规范

**BeanFactory作为最顶层的一个接口类，它定义了IOC容器的基本功能规范**，BeanFactory 有三个子类：ListableBeanFactory、HierarchicalBeanFactory 和AutowireCapableBeanFactory。先看下BeanFactory接口：

```java
public interface BeanFactory {    
      
    //用于取消引用实例并将其与FactoryBean创建的bean区分开来。例如，如果命名的bean是FactoryBean，则获取将返回Factory，而不是Factory返回的实例。
    String FACTORY_BEAN_PREFIX = "&"; 
        
    //根据bean的名字和Class类型等来得到bean实例    
    Object getBean(String name) throws BeansException;    
    Object getBean(String name, Class requiredType) throws BeansException;    
    Object getBean(String name, Object... args) throws BeansException;
    <T> T getBean(Class<T> requiredType) throws BeansException;
    <T> T getBean(Class<T> requiredType, Object... args) throws BeansException;

    //返回指定bean的Provider
    <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType);
    <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType);

    //检查工厂中是否包含给定name的bean，或者外部注册的bean
    boolean containsBean(String name);

    //检查所给定name的bean是否为单例/原型
    boolean isSingleton(String name) throws NoSuchBeanDefinitionException;
    boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

    //判断所给name的类型与type是否匹配
    boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException;
    boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException;

    //获取给定name的bean的类型
    @Nullable
    Class<?> getType(String name) throws NoSuchBeanDefinitionException;

    //返回给定name的bean的别名
    String[] getAliases(String name);
     
}
```



BeanFactory的其他接口主要是为了区分在 Spring 内部在操作过程中对象的传递和转化过程中，对对象的数据访问所做的限制。

- ListableBeanFactory：该接口定义了访问容器中 Bean 基本信息的若干方法，如查看Bean 的个数、获取某一类型 Bean 的配置名、查看容器中是否包括某一 Bean 等方法；
- HierarchicalBeanFactory：父子级联 IoC 容器的接口，子容器可以通过接口方法访问父容器； 通过 HierarchicalBeanFactory 接口， Spring 的 IoC 容器可以建立父子层级关联的容器体系，子容器可以访问父容器中的 Bean，但父容器不能访问子容器的 Bean。Spring 使用父子容器实现了很多功能，比如在 Spring MVC 中，展现层 Bean 位于一个子容器中，而业务层和持久层的 Bean 位于父容器中。这样，展现层 Bean 就可以引用业务层和持久层的 Bean，而业务层和持久层的 Bean 则看不到展现层的 Bean。
- ConfigurableBeanFactory：是一个重要的接口，增强了 IoC 容器的可定制性，它定义了设置类装载器、属性编辑器、容器初始化后置处理器等方法；
- ConfigurableListableBeanFactory: ListableBeanFactory 和 ConfigurableBeanFactory的融合；AutowireCapableBeanFactory：定义了将容器中的 Bean 按某种规则（如按名字匹配、按类型匹配等）进行自动装配的方法；

### BeanRegistry

- BeanRegistry： 向IOC容器手工注册 BeanDefinition 对象的方法

Spring 配置文件中每一个节点元素在 Spring 容器里都通过一个 BeanDefinition 对象表示，它描述了 Bean 的配置信息。而 BeanDefinitionRegistry 接口提供了向容器手工注册 BeanDefinition 对象的方法。

### BeanDefinition

各种Bean对象及其相互的关系

- BeanDefinition 定义了各种Bean对象及其相互的关系

- BeanDefinitionReader 这是BeanDefinition的解析器

- BeanDefinitionHolder 这是BeanDefination的包装类，用来存储BeanDefinition，name以及aliases等。

#### BeanDefinition

SpringIOC容器管理了定义的各种Bean对象及其相互的关系，Bean对象在Spring实现中是以BeanDefinition来描述的，其继承体系如下

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281028156.png)

#### BeanDefinitionReader

Bean 的解析过程非常复杂，功能被分的很细，因为这里需要被扩展的地方很多，必须保证有足够的灵活性，以应对可能的变化。Bean 的解析主要就是对 Spring 配置文件的解析。这个解析过程主要通过下图中的类完成：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281029913.png)

#### BeanDefinitionHolder

BeanDefinitionHolder 是BeanDefination的包装类，用来存储BeanDefinition，name以及aliases等

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281029574.png)

### ApplicationContext

IoC容器的接口类是ApplicationContext，很显然它必然继承BeanFactory对Bean规范（最基本的ioc容器的实现）进行定义。而ApplicationContext表示的是应用的上下文，除了对Bean的管理外，还至少应该包含了

- 访问资源： 对不同方式的Bean配置（即资源）进行加载。(实现ResourcePatternResolver接口)

- 国际化: 支持信息源，可以实现国际化。（实现MessageSource接口）

- 应用事件: 支持应用事件。(实现ApplicationEventPublisher接口)

#### 接口设计

ApplicationContext整体结构：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281030642.png)

- HierarchicalBeanFactory 和 ListableBeanFactory： ApplicationContext 继承了 HierarchicalBeanFactory 和 ListableBeanFactory 接口，在此基础上，还通过多个其他的接口扩展了
- BeanFactory的功能：ApplicationEventPublisher：让容器拥有发布应用上下文事件的功能，包括容器启动事件、关闭事件等。实现了 ApplicationListener 事件监听接口的 Bean 可以接收到容器事件 ， 并对事件进行响应处理 。 在 ApplicationContext 抽象实现类AbstractApplicationContext 中，我们可以发现存在一个 ApplicationEventMulticaster，它负责保存所有监听器，以便在容器产生上下文事件时通知这些事件监听者。
- MessageSource：为应用提供 i18n 国际化消息访问的功能；
- ResourcePatternResolver ： 所 有 ApplicationContext 实现类都实现了类似于PathMatchingResourcePatternResolver 的功能，可以通过带前缀的 Ant 风格的资源文件路径装载 Spring 的配置文件。
- LifeCycle：该接口是 Spring 2.0 加入的，该接口提供了 start()和 stop()两个方法，主要用于控制异步处理过程。在具体使用时，该接口同时被 ApplicationContext 实现及具体 Bean 实现， ApplicationContext 会将 start/stop 的信息传递给容器中所有实现了该接口的 Bean，以达到管理和控制 JMX、任务调度等目的

#### 接口的实现

ApplicationContext接口的实现，关键的点在于，不同Bean的配置方式（比如xml,groovy,annotation等）有着不同的资源加载方式，这便衍生除了众多ApplicationContext的实现类。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281030496.png)

1. 第一，从类结构设计上看， 围绕着是否需要Refresh容器衍生出两个抽象类：

   1. GenericApplicationContext： 是初始化的时候就创建容器，往后的每次refresh都不会更改

   2. AbstractRefreshableApplicationContext： AbstractRefreshableApplicationContext及子类的每次refresh都是先清除已有(如果不存在就创建)的容器，然后再重新创建；AbstractRefreshableApplicationContext及子类无法做到GenericApplicationContext混合搭配从不同源头获取bean的定义信息

2. 第二， 从加载的源来看（比如xml,groovy,annotation等）， 衍生出众多类型的ApplicationContext, 典型比如:

   1. FileSystemXmlApplicationContext： 从文件系统下的一个或多个xml配置文件中加载上下文定义，也就是说系统盘符中加载xml配置文件。

   2.  ClassPathXmlApplicationContext： 从类路径下的一个或多个xml配置文件中加载上下文定义，适用于xml配置的方式。

   3. AnnotationConfigApplicationContext： 从一个或多个基于java的配置类中加载上下文定义，适用于java注解的方式。

   4. ConfigurableApplicationContext： 扩展于 ApplicationContext，它新增加了两个主要的方法： refresh()和 close()，让 ApplicationContext 具有启动、刷新和关闭应用上下文的能力。在应用上下文关闭的情况下调用 refresh()即可启动应用上下文，在已经启动的状态下，调用 refresh()则清除缓存并重新装载配置信息，而调用close()则可关闭应用上下文。这些接口方法为容器的控制管理带来了便利，但作为开发者，我们并不需要过多关心这些方法。

3. 第三， 更进一步理解：

   1. 设计者在设计时AnnotationConfigApplicationContext为什么是继承GenericApplicationContext？ 因为基于注解的配置，是不太会被运行时修改的，这意味着不需要进行动态Bean配置和刷新容器，所以只需要GenericApplicationContext。

   2. 而基于XML这种配置文件，这种文件是容易修改的，需要动态性刷新Bean的支持，所以XML相关的配置必然继承AbstractRefreshableApplicationContext； 且存在多种xml的加载方式（位置不同的设计），所以必然会设计出AbstractXmlApplicationContext, 其中包含对XML配置解析成BeanDefination的过程。

 

## 生命周期的整体流程

Spring 容器可以管理 singleton 作用域 Bean 的生命周期，在此作用域下，Spring 能够精确地知道该 Bean 何时被创建，何时初始化完成，以及何时被销毁。

而对于 prototype 作用域的 Bean，Spring 只负责创建，当容器创建了 Bean 的实例后，Bean 的实例就交给客户端代码管理，Spring 容器**将不再跟踪其生命周期**。每次客户端请求 prototype 作用域的 Bean 时，Spring 容器都会创建一个新的实例，并且不会管那些被配置成 prototype 作用域的 Bean 的生命周期。

了解 Spring 生命周期的意义就在于，**可以利用 Bean 在其存活期间的指定时刻完成一些相关操作**。这种时刻可能有很多，但一般情况下，会在 Bean 被初始化后和被销毁前执行一些相关操作。

 



在执行初始化方法之前和之后，还需要对Bean的后置处理器BeanPostProcessors进行处理

1. 在invokeInitMethods 的前后进行applyBeanPostProcessorsBeforeInitialization，applyBeanPostProcessorsAfterInitialization

2. 在后置处理中处理了包括：AOP【AnnotationAwareAspectJAutoProxyCreator】，负责 构造后@PostConstruct 和 销毁前@PreDestroy 的 InitDestoryAnnotationBeanPostProcessor 等

3. 以及通过实现BeanPostProcessor接口的自定义处理器



整体流程如下：

1. 加载Bean定义：通过 loadBeanDefinitions 扫描所有xml配置、注解将Bean记录在beanDefinitionMap中。即IOC容器的初始化过程

2. Bean实例化：遍历 beanDefinitionMap 创建bean，最终会使用getBean中的doGetBean方法调用 createBean来创建Bean对象

   1. 构建对象：容器通过 createBeanInstance 进行对象构造

      1. 获取构造方法（大部分情况下只有一个构造方法）

         1. 如果只有一个构造方法，无论这个构造方法有没有入参，都用这个构造方法

         2. 有多个构造方法时

            1.  先拿带有@Autowired的构造方法，但是如果多个构造方法都有@Autowired就会报错

            2. 如果没有带有@Autowired的构造方法，那就找没有入参的；如果多个构造方法都是有入参的，那也会报错

      2. 准备参数 

         1. 先根据类进行查找

         2. 如果这个类有多个实例，则再根据参数名匹配

         3. 如果没有找到则报错

      3. 构造对象：无参构造方法则直接实例化

   2. 填充属性：通过populateBean方法为Bean内部所需的属性进行赋值，通常是 @Autowired 注解的变量；通过三级缓存机制进行填充，也就是依赖注入

   3. 初始化Bean对象：通过initializeBean对填充后的实例进行初始化

      1. 执行Aware：检查是否有实现着三个Aware，BeanNameAware，BeanClassLoaderAware, BeanFactoryAware；让实例化后的对象能够感知自己在Spring容器里的存在的位置信息，创建信息

      2. 初始化前：BeanPostProcessor，也就是拿出所有的后置处理器对bean进行处理，当有一个处理器返回null，将不再调用后面的处理器处理。

      3. 初始化：afterPropertiesSet，init- method；

         1. 实现了InitializingBean接口的类执行其afterPropertiesSet()方法

         2. 从BeanDefinition中获取initMethod方法

      4. 初始化后：BeanPostProcessor,；获取所有的bean的后置处理器去执行。AOP也是在这里做的

   4. 注册销毁：通过reigsterDisposableBean处理实现了DisposableBean接口的Bean的注册

      1. Bean是否有注册为DisposableBean的资格：

         1. 是否有destroyMethod。

         2. 是否有执行销毁方法的后置处理器。

      2. DisposableBeanAdapter： 推断destoryMethod

      3. 完成注册

3. 添加到单例池：通过 addSingleton 方法，将Bean 加入到单例池 singleObjects

4. 销毁

   1. 销毁前：如果有@PreDestory 注解的方法就执行

   2. 如果有自定义的销毁后置处理器，通过 postProcessBeforeDestruction 方法调用destoryBean逐一销毁Bean

   3. 销毁时：如果实现了destroyMethod就执行 destory方法

   4. 执行客户自定义销毁：调用 invokeCustomDestoryMethod执行在Bean上自定义的destroyMethod方法

      1. 有这个自定义销毁就会执行

      2. 没有自定义destroyMethod方法就会去执行close方法

      3. 没有close方法就会去执行shutdown方法

      4. 都没有的话就都不执行，不影响















