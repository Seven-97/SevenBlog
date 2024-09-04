---
title: Spring中的扩展点
category: 常用框架
tag:
  - Spring
---



## 介绍

Spring的核心思想就是容器，当容器refresh的时候，外部看上去风平浪静，其实内部则是一片惊涛骇浪，汪洋一片。Springboot更是封装了Spring，遵循约定大于配置，加上自动装配的机制。很多时候我们只要引用了一个依赖，几乎是零配置就能完成一个功能的装配。

由spring提供的、在容器或bean生命周期各个阶段、供spring框架回调使用的函数方法，即为扩展点。扩展点体现了Spring框架的灵活性、业务亲和性。使开发人员可以在不修改spring源码的情况下，对容器和bean的行为、属性进行额外的管理。

想要把自动装配玩的转，就必须要了解spring对于bean的构造生命周期以及各个扩展接口，当然了解了bean的各个生命周期也能促进我们加深对spring的理解。业务代码也能合理利用这些扩展点写出更优雅的代码。本文不讲原理，只将扩展点与使用方式讲清楚，原理可以移步[IOC系列文章](https://www.seven97.top/framework/spring/ioc2-initializationprocess.html)。

在网上搜索spring扩展点，发现很少有博文说的很全的，只有一些常用的扩展点的说明。所以在这篇文章里，我总结了几乎Spring & Springboot所有的扩展接口，各个扩展点的使用场景，并整理出一个bean在spring中从被加载到初始化到销毁的所有可扩展点的顺序调用图。

![img](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409021555378.png)



## ApplicationContextInitializer

> org.springframework.context.ApplicationContextInitializer

这是整个spring容器在刷新之前初始化`ConfigurableApplicationContext`的回调接口，简单来说，就是在**容器刷新之前调用**此类的`initialize`方法。也就是允许用户可以在整个spring容器还没被初始化之前做一些事情，对ConfiurableApplicationContext的实例做进一步的设置和处理。

此接口，`Spring Framework`自己没有提供任何的实现类，但在`SpringBoot`对它有较多的扩展实现。



### 扩展方式

实现ApplicationContextInitializer接口需要实现其唯一的方法`initialize`，该方法接受一个泛型参数`C extends ConfigurableApplicationContext`，表示正在创建的应用程序上下文。在该方法中，可以对应用程序上下文进行各种自定义操作，例如添加属性源、注册Bean定义、设置环境变量等。

```java
public class TestApplicationContextInitializer implements ApplicationContextInitializer {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        System.out.println("[ApplicationContextInitializer]");
    }
}
```



**使用场景**：

- 动态加载配置：通过实现ApplicationContextInitializer，可以在应用程序上下文创建之前加载一些动态的配置，例如从外部配置文件中读取配置信息并注入到Spring的环境中。
- 执行额外的初始化逻辑：如果有一些需要在应用程序上下文启动之前执行的初始化逻辑，例如初始化数据库连接池或启动一些后台任务，可以通过实现ApplicationContextInitializer来实现这些逻辑。



### Spring环境下添加扩展点

我们自定义实现一个`ApplicationContextInitializer`让并且它生效的方式有两种：

1. 手动调用的setXXX方法添加

```java
ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext();

// Add initializer
context.addApplicationListener(new MyApplicationContextInitializer());

// Set config locations and refresh context
context.setConfigLocation("classpath:applicationContext.xml");
context.refresh();

// Use the context
// ...

context.close();
```



2. Spring 的 XML 配置文件中注册

```java
<context:initializer class="com.example.MyApplicationContextInitializer"/>
```



3. web.xml 文件配置

```java
<context-param>
    <param-name>contextInitializerClasses</param-name>
    <param-value>com.example.MyApplicationContextInitializer</param-value>
</context-param>
```





### SpringBoot环境下添加扩展点

示例，展示了如何实现一个ApplicationContextInitializer来添加一个自定义的属性源：

```java
public class TestApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();
        
        // 创建自定义的属性源
        Map<String, Object> customProperties = new HashMap<>();
        customProperties.put("custom.property", "custom value");
        MapPropertySource customPropertySource = new MapPropertySource("customPropertySource", customProperties);
        
        // 将自定义属性源添加到应用程序上下文的属性源列表中
        propertySources.addFirst(customPropertySource);
    }
}
```



1. 在启动类中用`springApplication.addInitializers(new TestApplicationContextInitializer())`语句加入

```java
@SpringBootApplication
public class MySpringExApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(MySpringExApplication.class);
        application.addInitializers(new TestApplicationContextInitializer()); // 直接在SpringApplication中添加
        application.run(args);
    }
}
```



2. application配置文件 配置` com.seven.springsrpingbootextentions.extentions.TestApplicationContextInitializer`

```java
# application.yml文件
context:
  initializer:
    classes: com.seven.springsrpingbootextentions.extentions.TestApplicationContextInitializer
```



3. Spring SPI扩展，在spring.factories中加入（官方推荐）：

```java
com.seven.springsrpingbootextentions.extentions.TestApplicationContextInitializer
```



### SpringBoot内置的ApplicationContextInitializer

- **DelegatingApplicationContextInitializer**：使用环境属性context.initializer.classes指定的初始化器(initializers)进行初始化工作，如果没有指定则什么都不做。通过它使得我们可以把自定义实现类配置在application.properties里成为了可能。
- **ContextIdApplicationContextInitializer**：设置Spring应用上下文的ID,会参照环境属性。至于Id设置为啥值会参考环境属性：
  - spring.application.name
  - vcap.application.name
  - spring.config.name
  - spring.application.index
  - vcap.application.instance_index
  - 如果这些属性都没有，ID使用application。

- **ConfigurationWarningsApplicationContextInitializer**：对于一般配置错误在日志中作出警告
- **ServerPortInfoApplicationContextInitializer**：将内置servlet容器实际使用的监听端口写入到Environment环境属性中。这样属性local.server.port就可以直接通过@Value注入到测试中，或者通过环境属性Environment获取。
- **SharedMetadataReaderFactoryContextInitializer**：创建一个SpringBoot和ConfigurationClassPostProcessor共用的CachingMetadataReaderFactory对象。实现类为：ConcurrentReferenceCachingMetadataReaderFactory
- **ConditionEvaluationReportLoggingListener**：将ConditionEvaluationReport写入日志。





## BeanDefinitionRegistryPostProcessor

> org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor

BeanDefinitionRegistryPostProcessor为容器级后置处理器。**容器级的后置处理器会在Spring容器初始化后、刷新前执行一次，用于动态注册Bean到容器**。

通过 BeanFactoryPostProcessor 的子类BeanDefinitionRegistryPostProcessor，可以注册一个你自己的BeanDefinition对象到容器中，等待容器内部依次调用进行对象实例化就能当bean用了。

BeanDefinitionRegistryPostProcessor用于在bean解析后实例化之前通过BeanDefinitionRegistry对BeanDefintion进行增删改查。

### 扩展方式

```java
public class TestBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        System.out.println("[BeanDefinitionRegistryPostProcessor] postProcessBeanDefinitionRegistry");
    }

    //BeanFactoryPostProcessor方法
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        System.out.println("[BeanDefinitionRegistryPostProcessor] postProcessBeanFactory");
    }
}
```



**使用场景**：

- BeanDefinitionRegistryPostProcessor用于在bean解析后实例化之前通过BeanDefinitionRegistry对BeanDefintion进行增删改查。

- 比如依赖Redis.jar，如果该依赖jar存在，则用[redis当缓存](https://www.seven97.top/system-design/cache-column/redis.html)，否则就用[本地缓存](https://www.seven97.top/system-design/cache-column/localcache1.html)。这个需求完全可以在postProcessBeanDefinitionRegistry中利用Class.forName判断依赖，存在的话则注册对应class到容器。
- 另外，Mybatis就是用BeanFactoryPostProcessor去注册的mapper，参见org.mybatis.spring.mapper.MapperScannerConfigurer#postProcessBeanDefinitionRegistry。



### 使用案例

```java
public class User {
    String name;
    String password;
}


@Component
public class Test2BeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        System.out.println("进入[TestBeanDefinitionRegistryPostProcessor]...postProcessBeanDefinitionRegistry..." + beanDefinitionRegistry);
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(User.class);
        MutablePropertyValues propertyValues = new MutablePropertyValues();
        PropertyValue propertyValue1 = new PropertyValue("name", "Seven");
        PropertyValue propertyValue2 = new PropertyValue("password", "123456");
        propertyValues.addPropertyValue(propertyValue1);
        propertyValues.addPropertyValue(propertyValue2);
        beanDefinition.setPropertyValues(propertyValues);
        beanDefinitionRegistry.registerBeanDefinition("user", beanDefinition);
    }

    //BeanFactoryPostProcessor的方法
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        System.out.println("进入[TestBeanDefinitionRegistryPostProcessor]...postProcessBeanFactory..." + beanFactory);

        BeanDefinition beanDefinition = beanFactory.getBeanDefinition("user");
        System.out.println("打印[TestBeanDefinitionRegistryPostProcessor]...postProcessBeanFactory..." + beanDefinition.getBeanClassName());
        User user = beanFactory.getBean(User.class);
        System.out.println("打印[TestBeanDefinitionRegistryPostProcessor]...postProcessBeanFactory..." + user.getName());
        System.out.println("打印[TestBeanDefinitionRegistryPostProcessor]...postProcessBeanFactory..." + user.getPassword());
    }
}
```

该代码通过实现BeanDefinitionRegistryPostProcessor接口，在Spring容器启动时动态注册了一个名为"user"的Bean，并设置了其name和password属性的值。在后续的BeanFactory初始化过程中，可以通过beanFactory获取到该动态注册的Bean，并访问其属性值。





## BeanFactoryPostProcessor

> org.springframework.beans.factory.config.BeanFactoryPostProcessor

这个接口是`beanFactory`的扩展接口，调用时机在spring在读取`beanDefinition`信息之后，实例化bean之前。虽然此时不能再注册beanDefinition，但是可以趁着bean没有实例化，对已经注册进容器的BeanDefinition进行修改，例如Scope、依赖查找方式、初始化方法、修改属性值、添加额外的元数据等，进而影响初始化行为。

在应用程序启动时，Spring容器会自动检测并调用所有实现了BeanFactoryPostProcessor接口的类的postProcessBeanFactory方法。开发人员可以利用这个方法来实现自定义的逻辑，从而实现一些高级的自定义逻辑和功能扩展。

也可以在这里面添加自己的BeanPostProcessor，以及其他容器相关操作，此方法只调用一次，**同时记住不要在这里做Bean的实例化**。

**前文介绍的BeanDefinitionRegistryPostProcessor是这个接口的子接口**，因此实现BeanDefinitionRegistryPostProcessor这个接口，也可以重写其postProcessBeanFactory 方法。实现了BeanDefinitionRegistryPostProcessor的postProcessBeanFactory方法会先执行，再执行实现了BeanFactoryPostProcessor的postProcessBeanFactory



### 扩展方式

```java
public class TestBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        System.out.println("[BeanFactoryPostProcessor]");
    }
}
```





### 使用案例

示例，展示了如何实现动态的给Bean修改属性值：

```java
public class User {
    String name;
    String password;
}

@Component
public class Test3BeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        System.out.println("进入[TestBeanFactoryPostProcessor]...postProcessBeanFactory..." + beanFactory);

        BeanDefinition beanDefinition = beanFactory.getBeanDefinition("user");
        System.out.println("打印[TestBeanFactoryPostProcessor]...postProcessBeanFactory..." + beanDefinition.getBeanClassName());
        User user = beanFactory.getBean(User.class);
        System.out.println("打印[TestBeanFactoryPostProcessor]...postProcessBeanFactory..." + user.getName());
        System.out.println("打印[TestBeanFactoryPostProcessor]...postProcessBeanFactory..." + user.getPassword());
    }
}
```







## InstantiationAwareBeanPostProcessor

> org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor

该接口继承了`BeanPostProcessor`接口，因为InstantiationAwareBeanPostProcessor也属于Bean级的后置处理器，区别如下：

`BeanPostProcess`接口只**在bean的初始化阶段进行扩展**（注入spring上下文前后），而`InstantiationAwareBeanPostProcessor`接口在此基础上增加了3个方法，把可扩展的范围增加了实例化阶段和属性注入阶段。要注意的是，虽然InstantiationAwareBeanPostProcessor继承于BeanPostProcessor，但是InstantiationAwareBeanPostProcessor的**执行时机要稍早于BeanPostProcessor**；



该类主要的扩展点有以下6个方法，主要在bean生命周期的两大阶段：**实例化阶段**和**初始化阶段**，下面一起进行说明，按调用顺序为：

- `postProcessBeforeInstantiation`：在Bean实例化之前调用，如果返回null，一切按照正常顺序执行；如果返回的是一个实例的对象，那么`postProcessAfterInstantiation()`会执行，其他的扩展点将不再触发。
- `postProcessAfterInstantiation`：在Bean实例化之后调用，可以对已实例化的Bean进行进一步的自定义处理。
- `postProcessPropertyValues`（方法已弃用）：bean已经实例化完成，在属性注入时阶段触发，`@Autowired`，`@Resource`等注解原理基于此方法实现；可以修改Bean的属性值或进行其他自定义操作，**当postProcessAfterInstantiation返回true才执行。**
- `postProcessBeforeInitialization`(BeanPostProcessor的扩展)：初始化bean之前，相当于把bean注入spring上下文之前；可用于创建代理类，如果返回的不是 null（也就是返回的是一个代理类） ，那么后续只会调用 postProcessAfterInitialization() 方法
- `postProcessAfterInitialization`(BeanPostProcessor的扩展)：初始化bean之后，相当于把bean注入spring上下文之后；返回值会影响 postProcessProperties() 是否执行，其中返回 false 的话，是不会执行。
- `postProcessProperties()`：在 Bean 设置属性前调用；用于修改 bean 的属性，如果返回值不为空，那么会更改指定字段的值

使用场景：这个扩展点非常有用 ，无论是写中间件和业务中，都能利用这个特性。比如对实现了某一类接口的bean在各个生命期间进行收集，或者对某个类型的bean进行统一的设值等等。



### 扩展方式

```java
@Component
public class Test4InstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        System.out.println("[TestInstantiationAwareBeanPostProcessor]...postProcessBeforeInstantiation..." + beanName);
        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        System.out.println("[TestInstantiationAwareBeanPostProcessor]...postProcessAfterInstantiation..." + beanName);
        return true;
    }

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {
        System.out.println("[TestInstantiationAwareBeanPostProcessor]...postProcessPropertyValues..." + beanName);
        return pvs;
    }

    //BeanPostProcessor的方法
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("[TestInstantiationAwareBeanPostProcessor]...postProcessBeforeInitialization..." + beanName);
        return bean;
    }

    //BeanPostProcessor的方法
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("[TestInstantiationAwareBeanPostProcessor]...postProcessAfterInitialization..." + beanName);
        return bean;
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        System.out.println("[TestInstantiationAwareBeanPostProcessor]...postProcessProperties..." + beanName);
        return InstantiationAwareBeanPostProcessor.super.postProcessProperties(pvs, bean, beanName);
    }

}
```



### 使用案例

示例，演示了TestUser这个Bean内部的执行流程。

```java
// InstantiationAwareBeanPostProcessor扩展实现
@Component
public class Test4InstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        if (isMatchClass(beanClass)) {
            System.out.println("进入[TestInstantiationAwareBeanPostProcessor]...postProcessBeforeInstantiation..." + beanName);
        }
        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        if (isMatchClass(bean.getClass())) {
            System.out.println("进入[TestInstantiationAwareBeanPostProcessor]...postProcessAfterInstantiation..." + beanName);
        }
        return true;
    }

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {
        if (isMatchClass(bean.getClass())) {
            System.out.println("进入[TestInstantiationAwareBeanPostProcessor]...postProcessPropertyValues..." + beanName);
        }
        return pvs;
    }

    //BeanPostProcessor的方法
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (isMatchClass(bean.getClass())) {
            System.out.println("进入[TestInstantiationAwareBeanPostProcessor]...postProcessBeforeInitialization..." + beanName);
        }
        return bean;
    }

    //BeanPostProcessor的方法
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (isMatchClass(bean.getClass())) {
            System.out.println("进入[TestInstantiationAwareBeanPostProcessor]...postProcessAfterInitialization..." + beanName);
        }
        return bean;
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        if (isMatchClass(bean.getClass())) {
            System.out.println("进入[TestInstantiationAwareBeanPostProcessor]...postProcessProperties..." + beanName);
        }
        return InstantiationAwareBeanPostProcessor.super.postProcessProperties(pvs, bean, beanName);
    }

    private boolean isMatchClass(Class<?> beanClass) {
        return TestUser.class.equals(ClassUtils.getUserClass(beanClass));
    }
}

// TestUser测试类
@Component
public class TestUser implements InitializingBean {
    String name;
    String password;

    public TestUser() {
        System.out.println("创建【TestUser】对象");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        System.out.println("设置【name】属性");
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        System.out.println("设置【password】属性");
        this.password = password;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("所有属性设置完毕");
    }
}
```







## SmartInstantiationAwareBeanPostProcessor

> org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor

`SmartInstantiationAwareBeanPostProcessor` 与其他扩展点最明显的不同，就是在实际的业务开发场景中应用到的机会并不多，主要是在Spring内部应用。

该扩展接口有3个触发点方法：

- `predictBeanType`：该触发点发生在`postProcessBeforeInstantiation`之前(也就是在 InstantiationAwareBeanPostProcessor的方法之前，在图上并没有标明，因为一般不太需要扩展这个点)，这个方法用于预测Bean的类型，返回第一个预测成功的Class类型，如果不能预测，则返回null；当调用`BeanFactory.getType(name)`时当通过bean的名字无法得到bean类型信息时就调用该回调方法来决定类型信息。
- `determineCandidateConstructors`：该触发点发生在`postProcessBeforeInstantiation`之后，用于决定使用哪个构造器构造Bean，返回的是该bean的所有构造函数列表；如果不指定，默认为null，即bean的无参构造方法。用户可以扩展这个点，来自定义选择相应的构造器来实例化这个bean。
- `getEarlyBeanReference`：该触发点发生在`postProcessAfterInstantiation`之后，主要用于Spring循环依赖问题的解决，如果Spring中检测不到循环依赖，这个方法不会被调用；当存在Spring循环依赖这种情况时，会在InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation方法触发执行之后执行；



### 扩展方式

```java
public class TestSmartInstantiationAwareBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor {

    //预测Bean的类型，返回第一个预测成功的Class类型，如果不能预测返回null；
    @Override
    public Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
        System.out.println("[TestSmartInstantiationAwareBeanPostProcessor] predictBeanType " + beanName);
        return beanClass;
    }
    
    //决定使用哪个构造器构造Bean，如果不指定，默认为null，即bean的无参构造方法；
    @Override
    public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException {
        System.out.println("[TestSmartInstantiationAwareBeanPostProcessor] determineCandidateConstructors " + beanName);
        return null;
    }

     //获得提前暴露的bean引用，主要用于Spring循环依赖问题的解决；
    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
        System.out.println("[TestSmartInstantiationAwareBeanPostProcessor] getEarlyBeanReference " + beanName);
        return bean;
    }
}
```



### 使用案例

- 定义一个Student类，然后注入一个Teacher类；

```java
@Slf4j
@Component
public class Student {
    private String name="xiao ming";
    @Autowired
    private Teacher teacher;
 
    public Student() {
        log.info("----student的无参数构造方法被执行");
    }
 
    public Student(String name) {
        this.name = name;
        log.info("----student的有参数构造方法被执行");
    }
 
    public Student(Teacher teacher) {
        this.teacher = teacher;
        log.info("----student的有参数构造方法（teacher）被执行");
    }
 
    public Student(String name, Teacher teacher) {
        this.name = name;
        this.teacher = teacher;
        log.info("----student的有参数构造方法（name,teacher）被执行");
    }
 
    public String getName() {
        return name;
    }
 
    public void setName(String name) {
        this.name = name;
    }
 
    public Teacher getTeacher() {
        return teacher;
    }
 
    public void setTeacher(Teacher teacher) {
        log.info("----student中的setTeacher方法被调用");
        this.teacher = teacher;
    }
}
```

- Teacher.java：定义一个Teacher类，然后给Teacher类注入一个Student类，这样Student类和Teacher类就形成了循环依赖，方便后面验验MySmartInstantiationAwareBeanPostProcessor#getEarlyBeanReference执行情况；

```java
@Component
@Slf4j
public class Teacher {
    private String name="li lao shi";
    @Autowired
    private Student student;
    public Teacher() {
        log.info("----teacher的无参数构造方法被执行");
    }
 
    public String getName() {
        return name;
    }
 
    public void setName(String name) {
        this.name = name;
    }
 
    public Student getStudent() {
        return student;
    }
 
    public void setStudent(Student student) {
        log.info("----teacher中的setStudent方法被调用");
        this.student = student;
    }
}
```



- MySmartInstantiationAwareBeanPostProcessor 

```java
@Component
@Slf4j
public class MySmartInstantiationAwareBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor {
    @Override
    public Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
        if (beanName.equals("student")) {
            log.info("----predictBeanType方法被执行," + beanName);
            return Student.class;
        }
        return null;
    }
    //student类里是有四个构造方法，这里可以选择实际是两个：
    //一个无参数的构造方法，另一个是形参数是Teacher类型的
    @Override
    public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException {
        if (beanName.equals("student") ) {
            log.info("----determineCandidateConstructors方法被执行," + beanName);
            Constructor<?> constructor = beanClass.getConstructors()[3];
            Constructor<?>[] constructors={constructor};
            return constructors;
        }
        return null;
    }
 
    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
        if (beanName.equals("student") ) {
            log.info("----getEarlyBeanReference方法被执行," + beanName);
        }
        return bean;
    }
 
    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        if (beanName.equals("student")) {
            log.info("----postProcessBeforeInstantiation方法被执行，beanName:"+beanName);
            return null;
        }
        return null;
    }
 
}
```



- 验证

```java
 @Test
 public void test1() {
     log.info("----单元测试执行开始");
     AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("com.seven");
     Teacher student = context.getBean(Teacher.class);
     log.info("student的实际ClassName是----：" + student.getClass().getName());
     log.info("----单元测试执行完毕");

 }
```





## BeanFactoryAware

> org.springframework.beans.factory.BeanFactoryAware

这个类只有一个触发点，发生在bean的实例化之后，注入属性之前，也就是Setter之前。这个类的扩展点方法为`setBeanFactory`，可以拿到`BeanFactory`这个属性。

使用场景为，你可以在bean实例化之后，但还未初始化之前，拿到 `BeanFactory`，在这个时候，可以对每个bean作特殊化的定制。也或者可以把`BeanFactory`拿到进行缓存，日后使用。



### 扩展方式

```java
public class TestBeanFactoryAware implements BeanFactoryAware {
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        System.out.println("[TestBeanFactoryAware] " + beanFactory.getBean(TestBeanFactoryAware.class).getClass().getSimpleName());
    }
}
```

- 获取Bean工厂引用：当一个Bean需要在运行时获取自身所在工厂的引用，以便进行一些与工厂相关的操作
- 动态加载其他Bean：有时需要动态地加载其他Bean，而加载所需的工厂引用就是加载该Bean的工厂



### 使用案例

- 创建并实现接口

```java
@Component
public class DemoBean implements BeanFactoryAware {
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        System.out.println("【BeanFactoryAware】Bean 的工厂是：" + beanFactory);
    }
}
```

- 测试

```java
@Test
public void test3(){
    log.info("----单元测试执行开始");
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("com.seven");
    log.info("----单元测试执行完毕");
}
```





## ApplicationContextAwareProcessor

> org.springframework.context.support.ApplicationContextAwareProcessor

该类本身并没有扩展点，而是 BeanPostProcessor 扩展接口的具体实现，但是该类内部却有6个扩展点可供实现 ，这些扩展点的触发时机在bean实例化之后，初始化之前。

可以看到，该类用于执行各种驱动接口，在bean实例化之后，属性填充之后。其内部有6个扩展点可供实现，这几个接口都是Spring预留的重点扩展实现，与Spring的 [Bean的生命周期](https://www.seven97.top/framework/spring/ioc3-Instantiationofbeans.html) 密切相关。

- `EnvironmentAware`：用于获取`EnviromentAware`的一个扩展类，这个变量非常有用， 可以获得系统内的所有参数；另外也可以通过注入的方式来获得Environment，用哪种方式需要以实现场景而决定。当然个人认为这个Aware没必要去扩展，因为spring内部都可以通过注入的方式来直接获得。

- `EmbeddedValueResolverAware`：用于获取`StringValueResolver`的一个扩展类， `StringValueResolver`可以获取基于String类型的properties的变量；但一般我们都用`@Value`的方式来获取properties的变量，用哪种方式需要以实现场景而决定。如果实现了这个Aware接口，把`StringValueResolver`缓存起来，通过这个类去获取`String`类型的变量，效果是一样的。
- `ResourceLoaderAware`：用于获取`ResourceLoader`的一个扩展类，`ResourceLoader`可以用于获取classpath内所有的资源对象。
- `ApplicationEventPublisherAware`：用于获取`ApplicationEventPublisher`的一个扩展类，`ApplicationEventPublisher`可以用来发布事件；这个对象也可以通过spring注入的方式来获得，结合`ApplicationListener`来共同使用，下文在介绍`ApplicationListener`时会详细提到。
- `MessageSourceAware`：用于获取`MessageSource`的一个扩展类，`MessageSource`主要用来做国际化。
- `ApplicationContextAware`：用来获取`ApplicationContext`的一个扩展类，`ApplicationContext`就是spring上下文管理器，可以手动的获取任何在spring上下文注册的bean。较多的做法是扩展这个接口来缓存spring上下文，包装成静态方法。
  同时`ApplicationContext`也实现了`BeanFactory`，`MessageSource`，`ApplicationEventPublisher`等接口，也可以用来做相关接口的事情。



### 扩展方式





### 使用案例

- Bird类，实现ApplicationContextAware接口

```java
@Component
@Slf4j
public class Bird implements ApplicationContextAware {
    private String name="xiao niao";
    private ApplicationContext applicationContext;
 
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext=applicationContext;
        log.info("----Spring的上下文环境application被注入");
    }
}
```

- 测试

```java
@Test
public void test3(){
    log.info("----单元测试执行开始");
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("com.seven");
    log.info("----单元测试执行完毕");
}
```





## BeanNameAware

> org.springframework.beans.factory.BeanNameAware

可以看到，这个类也是Aware扩展的一种，触发点在bean的初始化之前，也就是`postProcessBeforeInitialization`之前，这个类的触发点方法只有一个：`setBeanName`



### 扩展方式

```java
public class NormalBeanA implements BeanNameAware{
    public NormalBeanA() {
        System.out.println("NormalBean constructor");
    }

    @Override
    public void setBeanName(String name) {
        System.out.println("[BeanNameAware] " + name);
    }
}
```

使用场景：用户可以扩展这个点，在初始化bean之前拿到spring容器中注册的的beanName，来自行修改这个beanName的值。





## @PostConstruct

> javax.annotation.PostConstruct

可以看出来其本身不是Spring定义的注解，但是Spring提供了具体的实现。这个并不算一个扩展点，其实就是一个标注。其作用是在bean的初始化阶段，如果对一个方法标注了`@PostConstruct`，会先调用这个方法。这里重点是要关注下这个标准的触发点，这个触发点是在`postProcessBeforeInitialization`之后，`InitializingBean.afterPropertiesSet`之前。



### 扩展方式

```java
public class NormalBeanA {
    public NormalBeanA() {
        System.out.println("NormalBean constructor");
    }

    @PostConstruct
    public void init(){
        System.out.println("[PostConstruct] NormalBeanA");
    }
}
```

使用场景：用户可以对某一方法进行标注，来进行初始化某一个属性



注意：

- 使用@PostConstruct注解标记的方法不能有参数，除非是拦截器，可以采用拦截器规范定义的InvocationContext对象。
- 使用@PostConstruct注解标记的方法不能有返回值，实际上如果有返回值，也不会报错，但是会忽略掉；
- 使用@PostConstruct注解标记的方法不能被static修饰，但是final是可以的；



### 使用案例

- 定义一个ExampleController类，采用setter的依赖注入的方式，注入exampleService属性，另外在定义一个myPostConstruct方法用@PostConstruct注解标记；

```java
@RestController
@Slf4j
public class ExampleController {
 
    private ExampleService exampleService;
 
    public ExampleController() {
        log.info("----ExampleController无参数构造方法被执行");
    }
    @Autowired
    public void setExampleService(ExampleService exampleService) {
        this.exampleService = exampleService;
        log.info("----ExampleController类的setExampleService方法被调用");
    }
    @PostConstruct
    public void myPostConstruct(){
        log.info("----ExampleController类的myPostConstruct方法被调用");
    }
}
```

- 定义ExampleService类

```java
@Service
@Slf4j
public class ExampleService {
    public ExampleService() {
        log.info("----ExampleService的无参数构造方法被调用");
    }
}
```

- 测试

```java
@Test
public void test4(){
    log.info("----单元测试执行开始");
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("com.seven");
    log.info("----单元测试执行完毕");
}
```





## InitializingBean

> org.springframework.beans.factory.InitializingBean

这个类，顾名思义，也是用来初始化bean的。`InitializingBean`接口为bean提供了初始化方法的方式，它只在bean实例化、属性注入后的提供了一个扩展点`afterPropertiesSet`方法，凡是继承该接口的类，在初始化bean的时候都会执行该方法。这个**扩展点的触发时机**在`postProcessAfterInitialization`之前。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409031058389.png)



### 扩展方式

实现方式很简单，需要bean实现InitializingBean接口并且重写afterPropertiesSet()，且bean要注册到Spring容器中，那么bean在实例化、属性注入后，重写的afterPropertiesSet()就会触发执行；

```java
public class NormalBeanA implements InitializingBean{
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("[InitializingBean] NormalBeanA");
    }
}
```



注意：

- 与InitializingBean#afterPropertiesSet()类似效果的是`init-method`，但是需要注意的是InitializingBean#afterPropertiesSet()执行时机要略早于init-method；
- InitializingBean#afterPropertiesSet()的调用方式是在bean初始化过程中真接调用bean的afterPropertiesSet()；
- bean自定义属性init-method是通过java反射的方式进行调用 ；



### 使用案例

- 定义Dog类，注入Food类型的属性，定义一个myInitMethod，且实现InitializingBean，用于观察Dog类的实例化时机、属性注入时机、afterPropertiesSet()和init-method定义方法的执行时机

```java
@Slf4j
public class Dog implements InitializingBean {
    private String name = "wang cai";
    private Food food;
    public Dog() {
        log.info("----Dog的无参构造方法被执行");
    }
    @Autowired
    public void setFood(Food food) {
        this.food = food;
        log.info("----dog的food属性被注入");
    }
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("----com.seven.entity.Dog.afterPropertiesSet触发执行");
    }
    public void myInitMethod() {
        log.info("----com.seven.entity.Dog.myInitMethod触发执行");
    }
}
```

- 定义Food类，用于注入到Dog类中；

```java
public class Food {
    private String name = "大骨头";
    public Food() {
        log.info("----Food的无参数构造方法被执行");
    }
}
```

- 把定义好的Dog、Food类，使用Configuriation配置类的方式注入到Spring容器中，并定义Dog类中的init-method方法为Dog#myInitMethod()；

```java
@Configuration
public class SpringConfig {
    @Bean(initMethod = "myInitMethod")
    public Dog dog(){
        Dog dog = new Dog();
        return dog;
    }
    @Bean
    public Food food(){
        Food food = new Food();
        return food;
    }
}
```

- 定义MyBeanPostProcessor类，并实现BeanPostProcessor接口，重写postProcessBeforeInitialization()和postProcessAfterInitialization()，用于观察InitializingBean#afterPropertiesSet()、init-method、postProcessBeforeInitialization()和postProcessAfterInitialization()的执行时机；

```java
@Component
@Slf4j
public class MyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (beanName.equals("dog")) {
            log.info("----postProcessBeforeInitialization---" + beanName);
            //如果特定的bean实例化完成后,还未执行InitializingBean.afterPropertiesSet()方法之前,有一些其他操作,可以在这里实现
        }
        return bean;
    }
 
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (beanName.equals("dog")) {
            log.info("----postProcessAfterInitialization---" + beanName);
            //如果特定的bean实例化完成,InitializingBean.afterPropertiesSet()方法执行后,有一些其他操作,可以在这里实现
        }
        return bean;
    }
}
```

- 测试

```java
@Test
public void test4(){
    log.info("----单元测试执行开始");
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("com.seven");
    log.info("----单元测试执行完毕");
}
```



## SmartInitializingSingleton

> org.springframework.beans.factory.SmartInitializingSingleton

这个接口中只有一个方法`afterSingletonsInstantiated`，其作用是是 在spring容器管理的所有单例对象（非懒加载对象）初始化完成之后调用的回调接口。其触发时机为`postProcessAfterInitialization`之后。

使用场景：用户可以扩展此接口在对所有单例对象初始化完毕后，做一些后置的业务处理。

### 扩展方式

```java
public class TestSmartInitializingSingleton implements SmartInitializingSingleton {
    @Override
    public void afterSingletonsInstantiated() {
        System.out.println("[TestSmartInitializingSingleton]");
    }
}
```



注意：

- 实现SmartInitializingSingleton接口的bean的作用域必须是单例，afterSingletonsInstantiated()才会触发；
- afterSingletonsInstantiated()触发执行时，**非懒加载的单例bean**已经完成实现化、属性注入以及相关的初始化操作；
- afterSingletonsInstantiated()的执行时机是在DefaultListableBeanFactory#preInstantiateSingletons()；



### 使用案例

- 定义Dog类，以setter注入方式进行属性注入，同时Dog类实现SmartInitializingSingleton接口，重写afterSingletonsInstantiated()，并在方法内部打印日志，如果在实际业务开发过程中用到了这个扩展点，相关的扩展操作逻辑就是在这个方法内实现；

```java
@Slf4j
public class Dog implements InitializingBean, DisposableBean, SmartInitializingSingleton {
    private String name = "wang cai";
 
    private Food food;
 
    public Dog() {
        log.info("----Dog的无参构造方法被执行");
    }
    @Autowired
    public void setFood(Food food) {
        this.food = food;
        log.info("----dog的food属性被注入");
    }
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("----com.seven.entity.Dog.afterPropertiesSet触发执行");
    }
 
    public void myInitMethod() {
        log.info("----com.seven.entity.Dog.myInitMethod触发执行");
    }
 
    @Override
    public void destroy() throws Exception {
        log.info("----com.seven.entity.Dog.destroy触发执行");
    }
 
    @Override
    public void afterSingletonsInstantiated() {
        log.info("----com.seven.entity.Dog.afterSingletonsInstantiated触发执行");
    }
}
```

- 定义Food类，作为Dog类的一个属性；

```java
@Slf4j
public class Food {
    private String name = "大骨头";
 
    public Food() {
        log.info("----Food的无参数构造方法被执行");
    }
}
```

- 使用Configuration配置类的方式注册Dog、Food到Spring容器里；默认是单例对象；

```java
@Configuration
public class SpringConfig {
    @Bean(initMethod = "myInitMethod")
    public Dog dog(){
        Dog dog = new Dog();
        return dog;
    }
    @Bean
    public Food food(){
        Food food = new Food();
        return food;
    }
}
```

- 测试

```java
@Test
public void test5(){
    log.info("----单元测试执行开始");
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("com.seven");
    log.info("----单元测试执行完毕");
}
```



## FactoryBean

> org.springframework.beans.factory.FactoryBean

一般情况下，Spring通过[反射机制](https://www.seven97.top/java/basis/05-reflection.html)利用bean的class属性指定支线类去实例化bean，在某些情况下，实例化Bean过程比较复杂，如果按照传统的方式，则需要在bean中提供大量的配置信息。Spring为此提供了一个`org.springframework.bean.factory.FactoryBean`的工厂类接口，用户可以通过实现该接口定制实例化Bean的逻辑。`FactoryBean`接口对于Spring框架来说占用重要的地位，Spring自身就提供了70多个`FactoryBean`的实现。它们隐藏了实例化一些复杂bean的细节，给上层应用带来了便利。

触发点：

例如其他框架技术与Spring集成的时候，如mybatis与Spring的集成，mybatis是通过SqlSessionFactory创建出Sqlsession来执行sql的，那么Service层在调用Dao层的接口来执行数据库操作时肯定得持有SqlSessionFactory，那么问题来了：Spring容器怎么才能持有SqlSessionFactory呢？答案就是SqlSessionFactoryBean，它实现了FactoryBean接口。



FactoryBean 与 BeanFactory 的区别

- FactoryBean接口有三个方法：
  - getObject()，用于获取bean，主**要应用在创建一些复杂的bean的场景**；
  - getObjectType()，用于获取返回bean的类型；
  - isSingleton()，用于判断返回bean是否属于单例，默认trure，通俗说就是工厂bean；
- BeanFactory是Spring bean容器的根接口，ApplicationContext是Spring bean容器的高级接口，继承于BeanFactory，通俗理解就是生成bean的工厂；



### 扩展方式

```java
public class TestFactoryBean implements FactoryBean<TestFactoryBean.TestFactoryInnerBean> {

    @Override
    public TestFactoryBean.TestFactoryInnerBean getObject() throws Exception {
        System.out.println("[FactoryBean] getObject");
        return new TestFactoryBean.TestFactoryInnerBean();
    }

    @Override
    public Class<?> getObjectType() {
        return TestFactoryBean.TestFactoryInnerBean.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public static class TestFactoryInnerBean{

    }
}
```

使用场景：用户可以扩展这个类，来为要实例化的bean作一个代理，比如为该对象的所有的方法作一个拦截，在调用前后输出一行log，模仿`ProxyFactoryBean`的功能。



### 使用案例

- 定义Computer类；

```java
@Slf4j
public class Computer {
    private String type;
 
    public String getType() {
        return type;
    }
 
    public void setType(String type) {
        this.type = type;
    }
 
    public Computer() {
        log.info("----Computer类无参数构造方法触发执行");
    }
 
    public Computer(String type) {
        this.type = type;
        log.info("----Computer类有参数构造方法触发执行");
    }
}
```

- 定义ComputerFactoryBean，实现FactoryBean接口，实现了getObject()，创建了一个“复杂”的Bean；在getObjectType()返回了Bean的类型；在isSingleton()指定Bean的作用域为单例；

```java
@Component
@Slf4j
public class ComputerFactoryBean implements FactoryBean {
    private String name = "ComputerFactoryBean本尊";
 
    public ComputerFactoryBean() {
        log.info("----ComputerFactoryBean无参数构造方法触发执行");
    }
 
    @Override
    public Object getObject() throws Exception {
        log.info("----com.seven.bean.ComputerFactoryBean.getObject()触发执行-start");
        Computer computer = new Computer("商用笔记本电脑");
        log.info("----com.seven.bean.ComputerFactoryBean.getObject()触发执行-end");
        return computer;
    }
 
    @Override
    public Class<?> getObjectType() {
        return Computer.class;
    }
 
    @Override
    public boolean isSingleton() {
        return true;
    }
}
```

- 测试：通常情况下，如果ComputerFactoryBean未实现FactoryBean接口，getBean("computeFactoryBean")的结果是computeFactoryBean对象；而实现了FactoryBean接口，getBean("computeFactoryBean")的结果的是getObject()方法中返回的结果，即computer，这就是FactoryBean如此神奇的地方。

```java
@Test
public void test6() {
    log.info("----单元测试执行开始");
    log.info("----Spring容器实例化开始");
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("com.seven");
    log.info("----Spring容器实例化完成");
    ComputerFactoryBean computerFactoryBean = ((ComputerFactoryBean) context.getBean("&computerFactoryBean"));
    Computer computer = (Computer) context.getBean("computerFactoryBean");
    Assert.isTrue(computerFactoryBean.getClass().getName().equals("com.seven.bean.ComputerFactoryBean"));
    Assert.isTrue(computer.getClass().getName().equals("com.seven.entity.Computer"));
    log.info("----单元测试执行完毕");
}
```







## CommandLineRunner和ApplicationRunner

> org.springframework.boot.CommandLineRunner

这两个是Springboot中新增的扩展点，之所以将这两个扩展点放在一起，是因为它两个功能特性高度相似，不同的只是名字、扩展方法形参数类型、执行先后的一些小的不同。

这两个接口触发时机为整个项目启动完毕后，自动执行。如果有多个`CommandLineRunner`，可以利用`@Order`来进行排序。



### 扩展方式

```java
public class TestCommandLineRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        System.out.println("[TestCommandLineRunner]");
    }
}
```

使用场景：用户扩展此接口，进行启动项目之后一些业务的预处理。

注意：

- CommandLineRunner和ApplicationRunner都有一个扩展方法run()，但是run()形参数类型不同；
- CommandLineRunner.run()方法的形参数类型是String... args，ApplicationRunner.run()的形参数类型是ApplicationArguments args；
- CommandLineRunner.run()的执行时机要晚于ApplicationRunner.run()一点；
- CommandLineRunner和ApplicationRunner触发执行时机是在Spring容器、Tomcat容器正式启动完成后，可以正式处理业务请求前，即项目启动的最后一步；
- CommandLineRunner和ApplicationRunner可以应用的场景：项目启动前，热点数据的预加载、清除临时文件、读取自定义配置信息等；



### 使用案例

- 定义MyCommandLineRunner和MyCommandLineRunner2，实现 CommandLineRunner, ApplicationRunner接口，并用@Order标记执行顺序，数字越小，执行知先级越高；

```java
@Component
@Slf4j
@Order(value = 1)
public class MyCommandLineRunner implements CommandLineRunner, ApplicationRunner {
 
    @Override
    public void run(String... args) throws Exception {
        log.info("----CommandLineRunner.run()触发执行，实现类是:"+this.getClass().getName());
    }
 
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("----ApplicationRunner.run()触发执行，实现类是:"+this.getClass().getName());
    }
}
```

```java
@Component
@Slf4j
@Order(value = 2)
public class MyCommandLineRunner2 implements CommandLineRunner, ApplicationRunner {
 
    @Override
    public void run(String... args) throws Exception {
        log.info("----CommandLineRunner.run()触发执行，实现类是:"+this.getClass().getName());
    }
 
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("----ApplicationRunner.run()触发执行，实现类是:"+this.getClass().getName());
    }
}
```



- 使用SpringApplication.run()启动SprIngboot项目；

```java
@SpringBootApplication
@Slf4j
public class sevenApplication {
    public static void main(String[] args) {
        log.info("----Springboot开始启动....");
        SpringApplication.run(sevenApplication.class, args);
        log.info("----Springboot启动完成");
    }
}
```



## ApplicationListener 和 ApplicationContextInitializer

> org.springframework.context.ApplicationListener

准确的说，这个应该不算spring&springboot当中的一个扩展点，`ApplicationListener`可以监听某个事件的`event`，触发时机可以穿插在业务方法执行过程中，用户可以自定义某个业务事件。但是spring内部也有一些内置事件，这种事件，可以穿插在启动调用中。我们也可以利用这个特性，来自己做一些内置事件的监听器来达到和前面一些触发点大致相同的事情。

接下来罗列下spring主要的内置事件：

- ContextRefreshedEvent
  ApplicationContext 被初始化或刷新时，该事件被发布。这也可以在`ConfigurableApplicationContext`接口中使用 `refresh()`方法来发生。此处的初始化是指：所有的Bean被成功装载，后处理Bean被检测并激活，所有Singleton Bean 被预实例化，`ApplicationContext`容器已就绪可用。
- ContextStartedEvent
  当使用 `ConfigurableApplicationContext` （ApplicationContext子接口）接口中的 start() 方法启动 `ApplicationContext`时，该事件被发布。你可以调查你的数据库，或者你可以在接受到这个事件后重启任何停止的应用程序。
- ContextStoppedEvent
  当使用 `ConfigurableApplicationContext`接口中的 `stop()`停止`ApplicationContext`时，发布这个事件。你可以在接受到这个事件后做必要的清理的工作
- ContextClosedEvent
  当使用 `ConfigurableApplicationContext`接口中的 `close()`方法关闭 `ApplicationContext` 时，该事件被发布。一个已关闭的上下文到达生命周期末端；它不能被刷新或重启
- RequestHandledEvent
  这是一个 web-specific 事件，告诉所有 bean HTTP 请求已经被服务。只能应用于使用DispatcherServlet的Web应用。在使用Spring作为前端的MVC控制器时，当Spring处理用户请求结束后，系统会自动触发该事件





## DisposableBean

> org.springframework.beans.factory.DisposableBean

这个扩展点也只有一个方法：`destroy()`，其触发时机为当此对象销毁、Spring容器关闭时，会自动执行这个方法。比如说运行`applicationContext.registerShutdownHook`时，就会触发这个方法。这个扩展点基本上用不到



### 扩展方式

```java
public class NormalBeanA implements DisposableBean {
    @Override
    public void destroy() throws Exception {
        System.out.println("[DisposableBean] NormalBeanA");
    }
}
```



注意：

- DisposableBean是一个接口，为Spring bean提供了一种释放资源的方式 ，只有一个扩展方法destroy()；
- 实现DisposableBean接口，并重写destroy()，可以在Spring容器销毁bean的时候获得一次回调；
- destroy()的回调执行时机是Spring容器关闭，需要销毁所有的bean时；
- 与InitializingBean比较类似的是，InitializingBean#afterPropertiesSet()是在bean初始化的时候触发执行，DisposableBean#destroy()是在bean被销毁的时候触发执行



### 使用案例

- 定义Dog类，实现InitializingBean、DisposableBean接口，并重写afterPropertiesSet()、destroy()

```java
@Slf4j
public class Dog implements InitializingBean, DisposableBean {
    private String name = "wang cai";
    private Food food;
    public Dog() {
        log.info("----Dog的无参构造方法被执行");
    }
    @Autowired
    public void setFood(Food food) {
        this.food = food;
        log.info("----dog的food属性被注入");
    }
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("----com.fanfu.entity.Dog.afterPropertiesSet触发执行");
    }
    public void myInitMethod() {
        log.info("----com.fanfu.entity.Dog.myInitMethod触发执行");
    }
    @Override
    public void destroy() throws Exception {
        log.info("----com.fanfu.entity.Dog.destroy触发执行");
    }
}
```



- 测试

```java
@Test
public void test5(){
    log.info("----单元测试执行开始");
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("com.fanfu");
    log.info("----开始关闭Spring容器");
    context.registerShutdownHook();
    log.info("----Spring容器已经关闭完成");
    log.info("----单元测试执行完毕");
}
```







## 总结

我们从这些spring&springboot的扩展点当中，大致可以窥视到整个bean的生命周期。在业务开发或者写中间件业务的时候，可以合理利用spring提供给我们的扩展点，在spring启动的各个阶段内做一些事情。以达到自定义初始化的目的。此篇总结，如果有错误或者疏漏的地方，恳请指正。













<!-- @include: @article-footer.snippet.md -->     