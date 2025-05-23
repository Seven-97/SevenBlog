---
title: 深度揭秘SpringBoot自动装配的实现原理
category: 常用框架
tags:
  - SpringBoot
head:
  - - meta
    - name: keywords
      content: SpringBoot,SpringBoot自动装配,实现原理,源码阅读
  - - meta
    - name: description
      content: 全网最全的SpringBoot知识点总结，让天下没有难学的八股文！
---



## 引入

先看SpringBoot的主配置类

```java
@SpringBootApplication
public class DemoApplication{
    public static void main(String[] args)
    {
        SpringApplication.run(StartEurekaApplication.class, args);
    }
}
```



## @SpringBootApplication

点进@SpringBootApplication来看，发现@SpringBootApplication是一个组合注解。

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(excludeFilters = {
      @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
      @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
public @interface SpringBootApplication {

}
```



@SpringBootApplication 由 @Configuration、@EnableAutoConfiguration、@ComponentScan 注解的集合组成：

- @Configuration：允许注册额外的 bean 或导入其他配置类
- @EnableAutoConfiguration：启用 SpringBoot 的自动配置机制
- @ComponentScan：扫描被@Component (@Repository,@Service,@Controller)注解的 bean，注解默认会扫描该类所在的包下所有的类。



### @SpringBootConfiguration

@SpringBootConfiguration 注解源码如下：

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
public @interface SpringBootConfiguration {
}
```

可以看到这个注解除了元注解以外，就只有一个@Configuration，那也就是说这个注解相当于@Configuration，所以这两个注解作用是一样的，也就是能够去注册一些额外的Bean，并且导入一些额外的配置。



@Configuration还有一个作用就是把该类变成一个配置类，不需要额外的XML进行配置。所以@SpringBootConfiguration就相当于@Configuration。

进入@Configuration，发现@Configuration核心是@Component，说明Spring的配置类也是Spring的一个组件。

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {
    @AliasFor(
        annotation = Component.class
    )
    String value() default "";
}
```



### @EnableAutoConfiguration

继续看@EnableAutoConfiguration，这个注解是开启自动配置的功能，源码如下：

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigurationPackage
@Import({AutoConfigurationImportSelector.class})
public @interface EnableAutoConfiguration {
    String ENABLED_OVERRIDE_PROPERTY = "spring.boot.enableautoconfiguration";

    Class<?>[] exclude() default {};

    String[] excludeName() default {};
}
```

可以看到它是由 @AutoConfigurationPackage，@Import(EnableAutoConfigurationImportSelector.class)这两个而组成的，



#### @AutoConfigurationPackage

先看@AutoConfigurationPackage，这是为了让包中的类以及子包中的类能够被自动扫描到spring容器中。

源码如下：

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({Registrar.class})
public @interface AutoConfigurationPackage {
}
```



可以看到，这里使用@Import 来给Spring容器中导入一个组件 ，这里导入的是Registrar.class。来看下这个Registrar：

```java
static class Registrar implements ImportBeanDefinitionRegistrar, DeterminableImports {
        Registrar() {
        }

        public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
            AutoConfigurationPackages.register(registry, (new AutoConfigurationPackages.PackageImport(metadata)).getPackageName());
        }

        public Set<Object> determineImports(AnnotationMetadata metadata) {
            return Collections.singleton(new AutoConfigurationPackages.PackageImport(metadata));
        }
    }
```

就是通过以上这个方法获取扫描的包路径，可以debug查看具体的值：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051211289.webp)



那metadata是什么呢，可以看到是标注在@SpringBootApplication注解上的DemoApplication，也就是主配置类Application：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051211130.webp)

其实就是将主配置类（即@SpringBootApplication标注的类）的所在包及子包里面所有组件扫描加载到Spring容器。因此要把DemoApplication放在项目的最高级中（最外层目录）。



#### @Import(AutoConfigurationImportSelector.class)

看看注解@Import(AutoConfigurationImportSelector.class)，@Import注解就是给Spring容器中导入一些组件，这里传入了一个组件的选择器:AutoConfigurationImportSelector。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051211647.webp)

可以从图中看出AutoConfigurationImportSelector 继承了 DeferredImportSelector 继承了 ImportSelector，ImportSelector有一个方法为：selectImports。将所有需要导入的组件以全类名的方式返回，这些组件就会被添加到容器中。

```java
public String[] selectImports(AnnotationMetadata annotationMetadata) {
    if (!this.isEnabled(annotationMetadata)) {
        return NO_IMPORTS;
    } else {
        AutoConfigurationMetadata autoConfigurationMetadata = AutoConfigurationMetadataLoader.loadMetadata(this.beanClassLoader);
        AutoConfigurationImportSelector.AutoConfigurationEntry autoConfigurationEntry = 
        this.getAutoConfigurationEntry(autoConfigurationMetadata, annotationMetadata);
        return StringUtils.toStringArray(autoConfigurationEntry.getConfigurations());
    }
}
```



这里会给容器中导入 自动配置类（xxxAutoConfiguration），也就是给容器中导入这个场景需要的所有组件，并配置好这些组件。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501191149125.jpg)

有了自动配置类，就免去了手动编写配置注入功能组件等的工作。



那是如何获取到这些配置类的呢，看看下面这个方法：

```java
protected AutoConfigurationImportSelector.AutoConfigurationEntry 
  getAutoConfigurationEntry(AutoConfigurationMetadata autoConfigurationMetadata, AnnotationMetadata annotationMetadata) {
    if (!this.isEnabled(annotationMetadata)) {
        return EMPTY_ENTRY;
    } else {
        AnnotationAttributes attributes = this.getAttributes(annotationMetadata);
        List<String> configurations = this.getCandidateConfigurations(annotationMetadata, attributes);
        configurations = this.removeDuplicates(configurations);
        Set<String> exclusions = this.getExclusions(annotationMetadata, attributes);
        this.checkExcludedClasses(configurations, exclusions);
        configurations.removeAll(exclusions);
        configurations = this.filter(configurations, autoConfigurationMetadata);
        this.fireAutoConfigurationImportEvents(configurations, exclusions);
        return new AutoConfigurationImportSelector.AutoConfigurationEntry(configurations, exclusions);
    }
}
```



可以看到getCandidateConfigurations()这个方法，他的作用就是引入系统已经加载好的一些类，那么到底是那些类呢：

```java
protected List<String> getCandidateConfigurations(AnnotationMetadata metadata, AnnotationAttributes attributes) {
    List<String> configurations = SpringFactoriesLoader.loadFactoryNames(this.getSpringFactoriesLoaderFactoryClass(), this.getBeanClassLoader());
    Assert.notEmpty(configurations, 
    "No auto configuration classes found in META-INF/spring.factories. If you are using a custom packaging, make sure that file is correct.");
    return configurations;
}
public static List<String> loadFactoryNames(Class<?> factoryClass, @Nullable ClassLoader classLoader) {
    String factoryClassName = factoryClass.getName();
    return (List)loadSpringFactories(classLoader).getOrDefault(factoryClassName, Collections.emptyList());
}
```

会从META-INF/spring.factories中获取资源，然后通过Properties加载资源：

```java
private static Map<String, List<String>> loadSpringFactories(@Nullable ClassLoader classLoader) {
    MultiValueMap<String, String> result = (MultiValueMap)cache.get(classLoader);
    if (result != null) {
        return result;
    } else {
        try {
            Enumeration<URL> urls = classLoader != 
          null ? classLoader.getResources("META-INF/spring.factories") : ClassLoader.getSystemResources("META-INF/spring.factories");
            LinkedMultiValueMap result = new LinkedMultiValueMap();

            while(urls.hasMoreElements()) {
                URL url = (URL)urls.nextElement();
                UrlResource resource = new UrlResource(url);
                Properties properties = PropertiesLoaderUtils.loadProperties(resource);
                Iterator var6 = properties.entrySet().iterator();

                while(var6.hasNext()) {
                    Map.Entry<?, ?> entry = (Map.Entry)var6.next();
                    String factoryClassName = ((String)entry.getKey()).trim();
                    String[] var9 = StringUtils.commaDelimitedListToStringArray((String)entry.getValue());
                    int var10 = var9.length;

                    for(int var11 = 0; var11 < var10; ++var11) {
                        String factoryName = var9[var11];
                        result.add(factoryClassName, factoryName.trim());
                    }
                }
            }

            cache.put(classLoader, result);
            return result;
        } catch (IOException var13) {
            throw new IllegalArgumentException("Unable to load factories from location [META-INF/spring.factories]", var13);
        }
    }
}
```

可以知道SpringBoot在启动的时候从类路径下的META-INF/spring.factories中获取EnableAutoConfiguration指定的值，将这些值作为自动配置类导入到容器中，自动配置类就生效，帮我们进行自动配置工作。以前需要自己配置的东西，自动配置类都帮我们完成了。



如下图可以发现Spring常见的一些类已经自动导入。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051212942.webp)



### @ComponentScan

接下来看@ComponentScan注解，@ComponentScan(excludeFilters = { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class), @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })，这个注解就是扫描包，然后放入spring容器。

```java
@ComponentScan(excludeFilters = {
  @Filter(type = FilterType.CUSTOM,classes = {TypeExcludeFilter.class}), 
  @Filter(type = FilterType.CUSTOM,classes = {AutoConfigurationExcludeFilter.class})})
public @interface SpringBootApplication {}
```

总结下@SpringbootApplication：就是说，他已经把很多东西准备好，具体是否使用取决于我们的程序或者说配置。



### 小结

总的来说，SpringBoot的自动装配原理就是 通过`@EnableAutoConfiguration`注解在类路径的META-INF/spring.factories文件中找到所有的对应配置类，然后将这些自动配置类加载到spring容器中



## run方法

```java
public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
```



来看下在执行run方法到底有没有用到哪些自动配置的东西，点进run：

```java
public ConfigurableApplicationContext run(String... args) {
    //计时器
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    ConfigurableApplicationContext context = null;
    Collection<SpringBootExceptionReporter> exceptionReporters = new ArrayList();
    this.configureHeadlessProperty();
    //监听器
    SpringApplicationRunListeners listeners = this.getRunListeners(args);
    listeners.starting();

    Collection exceptionReporters;
    try {
        ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
        ConfigurableEnvironment environment = this.prepareEnvironment(listeners, applicationArguments);
        this.configureIgnoreBeanInfo(environment);
        Banner printedBanner = this.printBanner(environment);
        //准备上下文
        context = this.createApplicationContext();
        exceptionReporters = this.getSpringFactoriesInstances(SpringBootExceptionReporter.class,                       new Class[]{ConfigurableApplicationContext.class}, context);
        //预刷新context
        this.prepareContext(context, environment, listeners, applicationArguments, printedBanner);
        //刷新context
        this.refreshContext(context);
        //刷新之后的context
        this.afterRefresh(context, applicationArguments);
        stopWatch.stop();
        if (this.logStartupInfo) {
            (new StartupInfoLogger(this.mainApplicationClass)).logStarted(this.getApplicationLog(), stopWatch);
        }

        listeners.started(context);
        this.callRunners(context, applicationArguments);
    } catch (Throwable var10) {
        this.handleRunFailure(context, var10, exceptionReporters, listeners);
        throw new IllegalStateException(var10);
    }

    try {
        listeners.running(context);
        return context;
    } catch (Throwable var9) {
        this.handleRunFailure(context, var9, exceptionReporters, (SpringApplicationRunListeners)null);
        throw new IllegalStateException(var9);
    }
}
```



那我们关注的就是 refreshContext(context); 刷新context，我们点进来看。

```java
private void refreshContext(ConfigurableApplicationContext context) {
   refresh(context);
   if (this.registerShutdownHook) {
      try {
         context.registerShutdownHook();
      }
      catch (AccessControlException ex) {
         // Not allowed in some environments.
      }
   }
}
```



继续点进refresh(context);

```java
protected void refresh(ApplicationContext applicationContext) {
   Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext);
   ((AbstractApplicationContext) applicationContext).refresh();
}
```



会调用 ((AbstractApplicationContext) applicationContext).refresh();方法，点进来看：

```java
public void refresh() throws BeansException, IllegalStateException {
   synchronized (this.startupShutdownMonitor) {
      // Prepare this context for refreshing.
      prepareRefresh();
      // Tell the subclass to refresh the internal bean factory.
      ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
      // Prepare the bean factory for use in this context.
      prepareBeanFactory(beanFactory);

      try {
         // Allows post-processing of the bean factory in context subclasses.
         postProcessBeanFactory(beanFactory);
         // Invoke factory processors registered as beans in the context.
         invokeBeanFactoryPostProcessors(beanFactory);
         // Register bean processors that intercept bean creation.
         registerBeanPostProcessors(beanFactory);
         // Initialize message source for this context.
         initMessageSource();
         // Initialize event multicaster for this context.
         initApplicationEventMulticaster();
         // Initialize other special beans in specific context subclasses.
         onRefresh();
         // Check for listener beans and register them.
         registerListeners();
         // Instantiate all remaining (non-lazy-init) singletons.
         finishBeanFactoryInitialization(beanFactory);
         // Last step: publish corresponding event.
         finishRefresh();
      }catch (BeansException ex) {
         if (logger.isWarnEnabled()) {
            logger.warn("Exception encountered during context initialization - " +
                  "cancelling refresh attempt: " + ex);
         }
         // Destroy already created singletons to avoid dangling resources.
         destroyBeans();
         // Reset 'active' flag.
         cancelRefresh(ex);
         // Propagate exception to caller.
         throw ex;
      }finally {
         // Reset common introspection caches in Spring's core, since we
         // might not ever need metadata for singleton beans anymore...
         resetCommonCaches();
      }
   }
}
```

由此可知，就是一个spring的bean的加载过程。继续来看一个方法叫做 onRefresh()：

```java
protected void onRefresh() throws BeansException {
   // For subclasses: do nothing by default.
}
```



在这里并没有直接实现，找他的具体实现：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051212682.webp)



比如Tomcat跟web有关，可以看到有个ServletWebServerApplicationContext：

```java
@Override
protected void onRefresh() {
   super.onRefresh();
   try {
      createWebServer();
   }
   catch (Throwable ex) {
      throw new ApplicationContextException("Unable to start web server", ex);
   }
}
```



可以看到有一个createWebServer()方法，用于创建web容器，而Tomcat不就是web容器。

那是如何创建的呢：

```java
private void createWebServer() {
   WebServer webServer = this.webServer;
   ServletContext servletContext = getServletContext();
   if (webServer == null && servletContext == null) {
      ServletWebServerFactory factory = getWebServerFactory();
      this.webServer = factory.getWebServer(getSelfInitializer());
   }
   else if (servletContext != null) {
      try {
         getSelfInitializer().onStartup(servletContext);
      }
      catch (ServletException ex) {
         throw new ApplicationContextException("Cannot initialize servlet context",
               ex);
      }
   }
   initPropertySources();
}
```

factory.getWebServer(getSelfInitializer())，显然是通过工厂的方式创建的。

```java
public interface ServletWebServerFactory {
   WebServer getWebServer(ServletContextInitializer... initializers);
}
```

可以看到 它是一个接口，为什么会是接口。因为不止是Tomcat一种web容器，可以看到还有Jetty

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051212127.webp)



接下来看TomcatServletWebServerFactory：

```java
@Override
public WebServer getWebServer(ServletContextInitializer... initializers) {
   Tomcat tomcat = new Tomcat();
   File baseDir = (this.baseDirectory != null) ? this.baseDirectory
         : createTempDir("tomcat");
   tomcat.setBaseDir(baseDir.getAbsolutePath());
   Connector connector = new Connector(this.protocol);
   tomcat.getService().addConnector(connector);
   customizeConnector(connector);
   tomcat.setConnector(connector);
   tomcat.getHost().setAutoDeploy(false);
   configureEngine(tomcat.getEngine());
   for (Connector additionalConnector : this.additionalTomcatConnectors) {
      tomcat.getService().addConnector(additionalConnector);
   }
   prepareContext(tomcat.getHost(), initializers);
   return getTomcatWebServer(tomcat);
}
```

这块代码，就是要寻找的内置Tomcat，在这个过程当中，可以看到创建Tomcat的一个流程。



也就是：

1. 首先从main找到run()方法，在执行run()方法之前new一个SpringApplication对象
2. 进入run()方法，创建应用监听器SpringApplicationRunListeners开始监听
3. 然后加载SpringBoot配置环境(ConfigurableEnvironment)，然后把配置环境(Environment)加入监听对象中
4. 然后加载应用上下文(ConfigurableApplicationContext)，当做run方法的返回对象
5. 最后创建Spring容器，refreshContext(context)，实现starter自动化配置和bean的实例化等工作。
   


<!-- @include: @article-footer.snippet.md -->     
