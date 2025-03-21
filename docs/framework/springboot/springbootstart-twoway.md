---
title: SpringBoot使用内置Tomcat与外置Tomcat的深度剖析
category: 常用框架
tags:
  - SpringBoot
head:
  - - meta
    - name: keywords
      content: SpringBoot,Tomcat启动SpringBoot
  - - meta
    - name: description
      content: 全网最全的SpringBoot知识点总结，让天下没有难学的八股文！
---



## 使用内置tomcat启动

### 配置案例

#### 启动方式

1. IDEA中main函数启动

2. mvn springboot-run

3. java -jar XXX.jar 
   使用这种方式时，为保证服务在后台运行，会使用nohup 
   
   ```java
   nohup java -jar -Xms128m -Xmx128m -Xss256k -XX:+PrintGCDetails -XX:+PrintHeapAtGC -Xloggc:/data/log/web-gc.log web.jar >/data/log/web.log &
   ```
   
   使用java -jar默认情况下，不会启动任何嵌入式Application Server，该命令只是启动一个执行jar main的JVM进程，当spring-boot-starter-web包含嵌入式tomcat服务器依赖项时，执行java -jar则会启动Application Server
   
   

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

#### 从main函数说起

```java
public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
    return run(new Class[]{primarySource}, args);
}
 
// 这里run方法返回的是ConfigurableApplicationContext
public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {
 	return (new SpringApplication(primarySources)).run(args);
}
```

 ```java
 public ConfigurableApplicationContext run(String... args) {
  ConfigurableApplicationContext context = null;
  Collection<SpringBootExceptionReporter> exceptionReporters = new ArrayList();
  this.configureHeadlessProperty();
  SpringApplicationRunListeners listeners = this.getRunListeners(args);
  listeners.starting();
  
  Collection exceptionReporters;
  try {
   ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
   ConfigurableEnvironment environment = this.prepareEnvironment(listeners, applicationArguments);
   this.configureIgnoreBeanInfo(environment);
   
   //打印banner，这里可以自己涂鸦一下，换成自己项目的logo
   Banner printedBanner = this.printBanner(environment);
   
   //创建应用上下文
   context = this.createApplicationContext();
   exceptionReporters = this.getSpringFactoriesInstances(SpringBootExceptionReporter.class, new Class[]{ConfigurableApplicationContext.class}, context);
  
   //预处理上下文
   this.prepareContext(context, environment, listeners, applicationArguments, printedBanner);
   
   //刷新上下文
   this.refreshContext(context);
   
   //再刷新上下文
   this.afterRefresh(context, applicationArguments);
   
   listeners.started(context);
   this.callRunners(context, applicationArguments);
  } catch (Throwable var10) {
   
  }
  
  try {
   listeners.running(context);
   return context;
  } catch (Throwable var9) {
   
  }
 }
 ```

既然我们想知道tomcat在SpringBoot中是怎么启动的，那么run方法中，重点关注创建应用上下文（createApplicationContext）和刷新上下文（refreshContext）。

 

#### 创建上下文

```java
//创建上下文
protected ConfigurableApplicationContext createApplicationContext() {
 Class<?> contextClass = this.applicationContextClass;
 if (contextClass == null) {
  try {
   switch(this.webApplicationType) {
    case SERVLET:
                    //创建AnnotationConfigServletWebServerApplicationContext
        contextClass = Class.forName("org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext");
     break;
    case REACTIVE:
     contextClass = Class.forName("org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext");
     break;
    default:
     contextClass = Class.forName("org.springframework.context.annotation.AnnotationConfigApplicationContext");
   }
  } catch (ClassNotFoundException var3) {
   throw new IllegalStateException("Unable create a default ApplicationContext, please specify an ApplicationContextClass", var3);
  }
 }
 
 return (ConfigurableApplicationContext)BeanUtils.instantiateClass(contextClass);
}
```

 这里会创建AnnotationConfigServletWebServerApplicationContext类。而AnnotationConfigServletWebServerApplicationContext类继承了ServletWebServerApplicationContext，而这个类是最终集成了AbstractApplicationContext。

 

#### 刷新上下文

```java
//SpringApplication.java
//刷新上下文
private void refreshContext(ConfigurableApplicationContext context) {
 this.refresh(context);
 if (this.registerShutdownHook) {
  try {
   context.registerShutdownHook();
  } catch (AccessControlException var3) {
  }
 }
}
 
//这里直接调用最终父类AbstractApplicationContext.refresh()方法
protected void refresh(ApplicationContext applicationContext) {
 ((AbstractApplicationContext)applicationContext).refresh();
}
```

 ```java
 //AbstractApplicationContext.java
 public void refresh() throws BeansException, IllegalStateException {
  synchronized(this.startupShutdownMonitor) {
   this.prepareRefresh();
   ConfigurableListableBeanFactory beanFactory = this.obtainFreshBeanFactory();
   this.prepareBeanFactory(beanFactory);
  
   try {
    this.postProcessBeanFactory(beanFactory);
    this.invokeBeanFactoryPostProcessors(beanFactory);
    this.registerBeanPostProcessors(beanFactory);
    this.initMessageSource();
    this.initApplicationEventMulticaster();
    //调用各个子类的onRefresh()方法，也就说这里要回到子类：ServletWebServerApplicationContext，调用该类的onRefresh()方法
    this.onRefresh();
    this.registerListeners();
    this.finishBeanFactoryInitialization(beanFactory);
    this.finishRefresh();
   } catch (BeansException var9) {
    this.destroyBeans();
    this.cancelRefresh(var9);
    throw var9;
   } finally {
    this.resetCommonCaches();
   }
  
  }
 }
 ```

```java
//ServletWebServerApplicationContext.java
//在这个方法里看到了熟悉的面孔，this.createWebServer，神秘的面纱就要揭开了。
protected void onRefresh() {
 super.onRefresh();
 try {
  this.createWebServer();
 } catch (Throwable var2) {
  
 }
}
 
//ServletWebServerApplicationContext.java
//这里是创建webServer，但是还没有启动tomcat，这里是通过ServletWebServerFactory创建，那么接着看下ServletWebServerFactory
private void createWebServer() {
 WebServer webServer = this.webServer;
 ServletContext servletContext = this.getServletContext();
 if (webServer == null && servletContext == null) {
  ServletWebServerFactory factory = this.getWebServerFactory();
  this.webServer = factory.getWebServer(new ServletContextInitializer[]{this.getSelfInitializer()});
 } else if (servletContext != null) {
  try {
   this.getSelfInitializer().onStartup(servletContext);
  } catch (ServletException var4) {
  
  }
 }
 
 this.initPropertySources();
}
 
//接口
public interface ServletWebServerFactory {
    WebServer getWebServer(ServletContextInitializer... initializers);
}
 
//实现
AbstractServletWebServerFactory
JettyServletWebServerFactory
TomcatServletWebServerFactory
UndertowServletWebServerFactory
```



这里ServletWebServerFactory接口有4个实现类，对应着四种容器：

而其中我们常用的有两个：TomcatServletWebServerFactory和JettyServletWebServerFactory。

```java
//TomcatServletWebServerFactory.java
//这里我们使用的tomcat，所以我们查看TomcatServletWebServerFactory。到这里总算是看到了tomcat的踪迹。
@Override
public WebServer getWebServer(ServletContextInitializer... initializers) {
 Tomcat tomcat = new Tomcat();
 File baseDir = (this.baseDirectory != null) ? this.baseDirectory : createTempDir("tomcat");
 tomcat.setBaseDir(baseDir.getAbsolutePath());
    //创建Connector对象
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
 
protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
 return new TomcatWebServer(tomcat, getPort() >= 0);
}
 
//Tomcat.java
//返回Engine容器，看到这里，如果熟悉tomcat源码的话，对engine不会感到陌生。
public Engine getEngine() {
    Service service = getServer().findServices()[0];
    if (service.getContainer() != null) {
        return service.getContainer();
    }
    Engine engine = new StandardEngine();
    engine.setName( "Tomcat" );
    engine.setDefaultHost(hostname);
    engine.setRealm(createDefaultRealm());
    service.setContainer(engine);
    return engine;
}
//Engine是最高级别容器，Host是Engine的子容器，Context是Host的子容器，Wrapper是Context的子容器
```

 getWebServer这个方法创建了Tomcat对象，并且做了两件重要的事情：把Connector对象添加到tomcat中，configureEngine(tomcat.getEngine());

 getWebServer方法返回的是TomcatWebServer。 

```java
//TomcatWebServer.java
//这里调用构造函数实例化TomcatWebServer
public TomcatWebServer(Tomcat tomcat, boolean autoStart) {
 Assert.notNull(tomcat, "Tomcat Server must not be null");
 this.tomcat = tomcat;
 this.autoStart = autoStart;
 initialize();
}
 
private void initialize() throws WebServerException {
    //在控制台会看到这句日志
 logger.info("Tomcat initialized with port(s): " + getPortsDescription(false));
 synchronized (this.monitor) {
  try {
   addInstanceIdToEngineName();
 
   Context context = findContext();
   context.addLifecycleListener((event) -> {
    if (context.equals(event.getSource()) && Lifecycle.START_EVENT.equals(event.getType())) {
     removeServiceConnectors();
    }
   });
 
   //===启动tomcat服务===
   this.tomcat.start();
 
   rethrowDeferredStartupExceptions();
 
   try {
    ContextBindings.bindClassLoader(context, context.getNamingToken(), getClass().getClassLoader());
   }
   catch (NamingException ex) {
                
   }
            
            //开启阻塞非守护进程
   startDaemonAwaitThread();
  }
  catch (Exception ex) {
   stopSilently();
   destroySilently();
   throw new WebServerException("Unable to start embedded Tomcat", ex);
  }
 }
}
```

```java
//Tomcat.java
public void start() throws LifecycleException {
 getServer();
 server.start();
}
//这里server.start又会回到TomcatWebServer的
public void stop() throws LifecycleException {
 getServer();
 server.stop();
}
```

```java
//TomcatWebServer.java
//启动tomcat服务
@Override
public void start() throws WebServerException {
 synchronized (this.monitor) {
  if (this.started) {
   return;
  }
  try {
   addPreviouslyRemovedConnectors();
   Connector connector = this.tomcat.getConnector();
   if (connector != null && this.autoStart) {
    performDeferredLoadOnStartup();
   }
   checkThatConnectorsHaveStarted();
   this.started = true;
   //在控制台打印这句日志，如果在yml设置了上下文，这里会打印
   logger.info("Tomcat started on port(s): " + getPortsDescription(true) + " with context path '"
     + getContextPath() + "'");
  }
  catch (ConnectorStartFailedException ex) {
   stopSilently();
   throw ex;
  }
  catch (Exception ex) {
   throw new WebServerException("Unable to start embedded Tomcat server", ex);
  }
  finally {
   Context context = findContext();
   ContextBindings.unbindClassLoader(context, context.getNamingToken(), getClass().getClassLoader());
  }
 }
}
 
//关闭tomcat服务
@Override
public void stop() throws WebServerException {
 synchronized (this.monitor) {
  boolean wasStarted = this.started;
  try {
   this.started = false;
   try {
    stopTomcat();
    this.tomcat.destroy();
   }
   catch (LifecycleException ex) {
    
   }
  }
  catch (Exception ex) {
   throw new WebServerException("Unable to stop embedded Tomcat", ex);
  }
  finally {
   if (wasStarted) {
    containerCounter.decrementAndGet();
   }
  }
 }
}
```



## 使用外置tomcat部署

### 配置案例

[外置Tomcat启动SpringBoot源码点击这里](https://github.com/Seven-97/SpringBoot-Demo/tree/master/02-helloworld-tomcat)

#### 继承SpringBootServletInitializer

- 外部容器部署的话，就不能依赖于Application的main函数了，而是要以类似于web.xml文件配置的方式来启动Spring应用上下文，此时需要在启动类中继承SpringBootServletInitializer，并重写configure方法；还添加 @SpringBootApplication 注解，这是为了能扫描到所有Spring注解的bean

方式一：启动类继承SpringBootServletInitializer实现configure：

```java
@SpringBootApplication
public class SpringBootHelloWorldTomcatApplication extends SpringBootServletInitializer {
	@Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Application.class);
    }
}
```

这个类的作用与在web.xml中配置负责初始化Spring应用上下文的监听器作用类似，只不过在这里不需要编写额外的XML文件了。



方式二：新增加一个类继承SpringBootServletInitializer实现configure：

```java
public class ServletInitializer extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        //此处的Application.class为带有@SpringBootApplication注解的启动类
        return builder.sources(Application.class);
    }
}
```





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

```xml
<dependency>
      <groupId>javax.servlet</groupId>
      <artifactId>javax.servlet-api</artifactId>
      <version>3.0.1</version>
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

tomcat不会主动去启动springboot应用 ，， 所以tomcat启动的时候肯定调用了**SpringBootServletInitializer**的SpringApplicationBuilder ， 就会启动springboot。

> ServletContainerInitializer的实现放在jar包的META-INF/services文件夹下，有一个名为javax.servlet.ServletContainerInitializer的文件，内容就是ServletContainerInitializer的实现类的全类名。当servlet容器启动时候就会去该文件中找到ServletContainerInitializer的实现类，从而创建它的实例调用onstartUp。这里就是用了[SPI机制](https://www.seven97.top/java/basis/06-SPI.html)



#### HandlesTypes(WebApplicationInitializer.class)

- @HandlesTypes传入的类为ServletContainerInitializer感兴趣的
- 容器会自动在classpath中找到 WebApplicationInitializer，会传入到onStartup方法的webAppInitializerClasses中
- `Set<Class<?>> webAppInitializerClasses`这里面也包括之前定义的TomcatStartSpringBoot

```java
@HandlesTypes(WebApplicationInitializer.class)
public class SpringServletContainerInitializer implements ServletContainerInitializer {
}
```

```java
@Override
public void onStartup(@Nullable Set<Class<?>> webAppInitializerClasses, ServletContext servletContext)
      throws ServletException {

   List<WebApplicationInitializer> initializers = new LinkedList<>();

   if (webAppInitializerClasses != null) {
      for (Class<?> waiClass : webAppInitializerClasses) {
        // 如果不是接口 不是抽象 跟WebApplicationInitializer有关系  就会实例化
         if (!waiClass.isInterface() && !Modifier.isAbstract(waiClass.getModifiers()) &&
               WebApplicationInitializer.class.isAssignableFrom(waiClass)) {
            try {
               initializers.add((WebApplicationInitializer)
                     ReflectionUtils.accessibleConstructor(waiClass).newInstance());
            }
            catch (Throwable ex) {
               throw new ServletException("Failed to instantiate WebApplicationInitializer class", ex);
            }
         }
      }
   }

   if (initializers.isEmpty()) {
      servletContext.log("No Spring WebApplicationInitializer types detected on classpath");
      return;
   }

   servletContext.log(initializers.size() + " Spring WebApplicationInitializers detected on classpath");
   // 排序
   AnnotationAwareOrderComparator.sort(initializers);
   for (WebApplicationInitializer initializer : initializers) {
      initializer.onStartup(servletContext);
   }
}
```

```java
@Override
public void onStartup(ServletContext servletContext) throws ServletException {
   // Logger initialization is deferred in case an ordered
   // LogServletContextInitializer is being used
   this.logger = LogFactory.getLog(getClass());
   WebApplicationContext rootApplicationContext = createRootApplicationContext(servletContext);
   if (rootApplicationContext != null) {
      servletContext.addListener(new SpringBootContextLoaderListener(rootApplicationContext, servletContext));
   }
   else {
      this.logger.debug("No ContextLoaderListener registered, as createRootApplicationContext() did not "
            + "return an application context");
   }
}
```



#### SpringBootServletInitializer

```java
protected WebApplicationContext createRootApplicationContext(ServletContext servletContext) {
   SpringApplicationBuilder builder = createSpringApplicationBuilder();
   builder.main(getClass());
   ApplicationContext parent = getExistingRootWebApplicationContext(servletContext);
   if (parent != null) {
      this.logger.info("Root context already created (using as parent).");
      servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, null);
      builder.initializers(new ParentContextApplicationContextInitializer(parent));
   }
   builder.initializers(new ServletContextApplicationContextInitializer(servletContext));
   builder.contextClass(AnnotationConfigServletWebServerApplicationContext.class);
   // 调用configure
   builder = configure(builder); //①
   builder.listeners(new WebEnvironmentPropertySourceInitializer(servletContext));
   SpringApplication application = builder.build();//②
   if (application.getAllSources().isEmpty()
         && MergedAnnotations.from(getClass(), SearchStrategy.TYPE_HIERARCHY).isPresent(Configuration.class)) {
      application.addPrimarySources(Collections.singleton(getClass()));
   }
   Assert.state(!application.getAllSources().isEmpty(),
         "No SpringApplication sources have been defined. Either override the "
               + "configure method or add an @Configuration annotation");
   // Ensure error pages are registered
   if (this.registerErrorPageFilter) {
      application.addPrimarySources(Collections.singleton(ErrorPageFilterConfiguration.class));
   }
   application.setRegisterShutdownHook(false);
   return run(application);//③
}
```



① 当调用configure就会来到TomcatStartSpringBoot .configure，将Springboot启动类传入到builder.source

```java
@Override
protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    return builder.sources(Application.class);
}
```

② 调用SpringApplication application = builder.build(); 就会根据传入的Springboot启动类来构建一个SpringApplication

```java
public SpringApplication build(String... args) {
   configureAsChildIfNecessary(args);
   this.application.addPrimarySources(this.sources);
   return this.application;
}
```

③ 调用 return run(application); 就会启动springboot应用

```java
protected WebApplicationContext run(SpringApplication application) {
   return (WebApplicationContext) application.run();
}
```

也就相当于Main函数启动：

```java
public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
}
```



之后的流程就与上面 使用内置Tomcat的Main函数一致了





<!-- @include: @article-footer.snippet.md -->     

