---
title: MVC中DispatcherServlet初始化的深度探索
category: 常用框架
tags:
  - SpringMVC
head:
  - - meta
    - name: keywords
      content: spring,spring mvc,MVC,实现原理,源码阅读
  - - meta
    - name: description
      content: 全网最全的Spring知识点总结，让天下没有难学的八股文！
---

## 概述

DispatcherServlet首先是Sevlet，Servlet有自己的生命周期的方法（init,destory等），那么在看DispatcherServlet初始化时首先需要看源码中DispatcherServlet的类结构设计。

首先我们看DispatcherServlet的类结构关系，在这个类依赖结构中找到init的方法

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281539257.png)

## 核心方法

### init

init()方法如下, 主要读取web.xml中servlet参数配置，并将交给子类方法initServletBean()继续初始化

```java
/**
  * Map config parameters onto bean properties of this servlet, and
  * invoke subclass initialization.
  * @throws ServletException if bean properties are invalid (or required
  * properties are missing), or if subclass initialization fails.
  */
@Override
public final void init() throws ServletException {

  // 读取web.xml中的servlet配置
  PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
  if (!pvs.isEmpty()) {
    try {
      // 转换成BeanWrapper，为了方便使用Spring的属性注入功能
      BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
      // 注入Resource类型需要依赖于ResourceEditor解析，所以注册Resource类关联到ResourceEditor解析器
      ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
      bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
      // 更多的初始化可以让子类去拓展
      initBeanWrapper(bw);
      // 让spring注入namespace,contextConfigLocation等属性
      bw.setPropertyValues(pvs, true);
    }
    catch (BeansException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
      }
      throw ex;
    }
  }

  // 让子类去拓展
  initServletBean();
}
```

读取配置可以从下图看出，正是初始化了我们web.xml中配置

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281539895.png)

再看下initServletBean()方法，位于FrameworkServlet类中

```java
/**
  * Overridden method of {@link HttpServletBean}, invoked after any bean properties
  * have been set. Creates this servlet's WebApplicationContext.
  */
@Override
protected final void initServletBean() throws ServletException {
  getServletContext().log("Initializing Spring " + getClass().getSimpleName() + " '" + getServletName() + "'");
  if (logger.isInfoEnabled()) {
    logger.info("Initializing Servlet '" + getServletName() + "'");
  }
  long startTime = System.currentTimeMillis();

  try {
    // 最重要的是这个方法
    this.webApplicationContext = initWebApplicationContext();

    // 可以让子类进一步拓展
    initFrameworkServlet();
  }
  catch (ServletException | RuntimeException ex) {
    logger.error("Context initialization failed", ex);
    throw ex;
  }

  if (logger.isDebugEnabled()) {
    String value = this.enableLoggingRequestDetails ?
        "shown which may lead to unsafe logging of potentially sensitive data" :
        "masked to prevent unsafe logging of potentially sensitive data";
    logger.debug("enableLoggingRequestDetails='" + this.enableLoggingRequestDetails +
        "': request parameters and headers will be " + value);
  }

  if (logger.isInfoEnabled()) {
    logger.info("Completed initialization in " + (System.currentTimeMillis() - startTime) + " ms");
  }
}
```

### initWebApplicationContext

initWebApplicationContext用来初始化和刷新WebApplicationContext。

这个方法主要做了以下几步：

1. 从ServletContext中获取第一步中创建的SpringMVC根上下文，为下面做准备
2. 根据init-param中的contextAttribute属性值从ServletContext查找是否存在上下文对象
3. 以XmlWebApplicationContext作为Class类型创建上下文对象，设置父类上下文，并完成刷新
4. 执行子类扩展方法onRefresh，在DispatcherServlet内初始化所有web相关组件
5. 将servlet子上下文对象发布到ServletContext

org.springframework.web.servlet.FrameworkServlet#initWebApplicationContext() 方法如下

```java
/**
  * Initialize and publish the WebApplicationContext for this servlet.
  * <p>Delegates to {@link #createWebApplicationContext} for actual creation
  * of the context. Can be overridden in subclasses.
  * @return the WebApplicationContext instance
  * @see #FrameworkServlet(WebApplicationContext)
  * @see #setContextClass
  * @see #setContextConfigLocation
  */
protected WebApplicationContext initWebApplicationContext() {
  WebApplicationContext rootContext =
      WebApplicationContextUtils.getWebApplicationContext(getServletContext());
  WebApplicationContext wac = null;

  // 如果在构造函数已经被初始化
  if (this.webApplicationContext != null) {
    // A context instance was injected at construction time -> use it
    wac = this.webApplicationContext;
    if (wac instanceof ConfigurableWebApplicationContext) {
      ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
      if (!cwac.isActive()) {
        // The context has not yet been refreshed -> provide services such as
        // setting the parent context, setting the application context id, etc
        if (cwac.getParent() == null) {
          // The context instance was injected without an explicit parent -> set
          // the root application context (if any; may be null) as the parent
          cwac.setParent(rootContext);
        }
        configureAndRefreshWebApplicationContext(cwac);
      }
    }
  }
  // 没有在构造函数中初始化，则尝试通过contextAttribute初始化
  if (wac == null) {
    // No context instance was injected at construction time -> see if one
    // has been registered in the servlet context. If one exists, it is assumed
    // that the parent context (if any) has already been set and that the
    // user has performed any initialization such as setting the context id
    wac = findWebApplicationContext();
  }

  // 还没有的话，只能重新创建了
  if (wac == null) {
    // No context instance is defined for this servlet -> create a local one
    wac = createWebApplicationContext(rootContext);
  }

  if (!this.refreshEventReceived) {
    // Either the context is not a ConfigurableApplicationContext with refresh
    // support or the context injected at construction time had already been
    // refreshed -> trigger initial onRefresh manually here.
    synchronized (this.onRefreshMonitor) {
      onRefresh(wac);
    }
  }

  if (this.publishContext) {
    // Publish the context as a servlet context attribute.
    String attrName = getServletContextAttributeName();
    getServletContext().setAttribute(attrName, wac);
  }

  return wac;
}
```

webApplicationContext只会初始化一次，依次尝试构造函数初始化，没有则通过contextAttribute初始化，仍没有则创建新的

 createWebApplicationContext方法创建SpringMVC的应用上下文，并调用configureAndRefreshWebApplicationContext方法进行上下文的刷新。创建的createWebApplicationContext方法如下

```java
/**
  * Instantiate the WebApplicationContext for this servlet, either a default
  * {@link org.springframework.web.context.support.XmlWebApplicationContext}
  * or a {@link #setContextClass custom context class}, if set.
  * <p>This implementation expects custom contexts to implement the
  * {@link org.springframework.web.context.ConfigurableWebApplicationContext}
  * interface. Can be overridden in subclasses.
  * <p>Do not forget to register this servlet instance as application listener on the
  * created context (for triggering its {@link #onRefresh callback}, and to call
  * {@link org.springframework.context.ConfigurableApplicationContext#refresh()}
  * before returning the context instance.
  * @param parent the parent ApplicationContext to use, or {@code null} if none
  * @return the WebApplicationContext for this servlet
  * @see org.springframework.web.context.support.XmlWebApplicationContext
  */
protected WebApplicationContext createWebApplicationContext(@Nullable ApplicationContext parent) {
  Class<?> contextClass = getContextClass();
  if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
    throw new ApplicationContextException(
        "Fatal initialization error in servlet with name '" + getServletName() +
        "': custom WebApplicationContext class [" + contextClass.getName() +
        "] is not of type ConfigurableWebApplicationContext");
  }

  // 通过反射方式初始化
  ConfigurableWebApplicationContext wac =
      (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);

  wac.setEnvironment(getEnvironment());
  wac.setParent(parent);
  String configLocation = getContextConfigLocation(); // 就是前面Demo中的springmvc.xml
  if (configLocation != null) {
    wac.setConfigLocation(configLocation);
  }

  // 初始化Spring环境
  configureAndRefreshWebApplicationContext(wac);

  return wac;
}
```

configureAndRefreshWebApplicationContext会先将新创建的这个上下文与servletcontext绑定，然后进行刷新操作，这个刷新操作就IOC的执行过程一样，configureAndRefreshWebApplicationContext方法初始化设置Spring环境

```java
protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
  // 设置context ID
  if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
    // The application context id is still set to its original default value
    // -> assign a more useful id based on available information
    if (this.contextId != null) {
      wac.setId(this.contextId);
    }
    else {
      // Generate default id...
      wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
          ObjectUtils.getDisplayString(getServletContext().getContextPath()) + '/' + getServletName());
    }
  }

  // 设置servletContext, servletConfig, namespace, listener...
  wac.setServletContext(getServletContext());
  wac.setServletConfig(getServletConfig());
  wac.setNamespace(getNamespace());
  wac.addApplicationListener(new SourceFilteringListener(wac, new ContextRefreshListener()));

  // The wac environment's #initPropertySources will be called in any case when the context
  // is refreshed; do it eagerly here to ensure servlet property sources are in place for
  // use in any post-processing or initialization that occurs below prior to #refresh
  ConfigurableEnvironment env = wac.getEnvironment();
  if (env instanceof ConfigurableWebEnvironment) {
    ((ConfigurableWebEnvironment) env).initPropertySources(getServletContext(), getServletConfig());
  }

  // 让子类去拓展
  postProcessWebApplicationContext(wac);
  applyInitializers(wac);

  // Spring环境初始化完了，就可以初始化DispatcherServlet处理流程中需要的组件了。做IOC容器的加载
  wac.refresh();
}
```

到这一步为止，就创建了3个context，分别为：

1. ServletContext：全局唯一，tomcat启动该web项目了创建
2. Spring的context：由servletcontext的监听器contextloaderlistener创建，默认读取servletcontext.xml配置，具体内容参考我们之前介绍的IOC的源码实现。同时该cntext与servletcontext互相关联，该context被注册到servletcontext中以webapplicationcontext属性存在。
3. SpringMVC的context：SpringMVC初始化DispacherServlet时创建的上下文，该context为Spring的context的子容器，可以访问Spring容器中的内容（子容器可以访问父容器中的内容，但是反过来不行）。

经过上面两个步骤，DispatcherServlet父类中的流程已经全部走完，这几个步骤主要的功能就是生成了SpringMVC容器，并将其与ServletContext、Spring容器相关联。

### refresh

有了webApplicationContext后，就开始刷新了（onRefresh()方法），这个方法是FrameworkServlet提供的模板方法，由子类DispatcherServlet来实现的。

```java
/**
  * This implementation calls {@link #initStrategies}.
  */
@Override
protected void onRefresh(ApplicationContext context) {
  initStrategies(context);
}
```

刷新主要是调用initStrategies(context)方法对DispatcherServlet中的组件进行初始化，这些组件就是在SpringMVC请求流程中包的主要组件。

```java
/**
  * Initialize the strategy objects that this servlet uses.
  * <p>May be overridden in subclasses in order to initialize further strategy objects.
    初始化SpringMVC的九大组件
  */
protected void initStrategies(ApplicationContext context) {
  //文件上传
  initMultipartResolver(context);
  initLocaleResolver(context);
  initThemeResolver(context);

  // 主要看如下三个方法
  initHandlerMappings(context);
  initHandlerAdapters(context);
  initHandlerExceptionResolvers(context);

  initRequestToViewNameTranslator(context);
  initViewResolvers(context);
  initFlashMapManager(context);
}
```

### initHanlderxxx

主要看initHandlerXXX相关的方法，它们之间的关系可以看SpringMVC的请求流程：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281540287.png)

1. HandlerMapping 是映射处理器
2. HandlerAdpter是**处理适配器**，它用来找到你的Controller中的处理方法
3. HandlerExceptionResolver是当遇到处理异常时的异常解析器

#### initMultipartResolver


文件的上传请求，则需要使用MultipartResolver

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202602011305916.png)



#### initHandlerMapping

该方法负责进行HandlerMapping接口实现类的加载。HandlerMapping接口主要用来提供request 请求对象和Handler对象 映射关系的接口。所谓request对象比如web应用中的http 请求，Handler对象则指的是对应rquest请求的相关处理逻辑。

首先判断是否查找所有HandlerMapping(默认为true)。如果为是，则从上下文(包括所有父上下文)中查询类型为HandlerMapping的Bean,并进行排序。如果为否，则从上下文中按指定名称去寻找。如果都没有找到，提供一个默认的实现。这个默认实现从DispatcherServlet同级目录的DispatcherServlet.properties中加载的。


initHandlerMapping方法如下，无非就是获取按照优先级排序后的HanlderMappings, 将来匹配时按照优先级最高的HanderMapping进行处理

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202602011033351.png)

initHandlerMapping 没有找到自定义配置的，则构建默认的。默认值在
DispatcherServlet.properties 文件中，九大组件均有默认值

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202602011034305.png)

#### initHandlerAdapters

通过initHandlerMappings已经将request通过HandlerMapping(处理器映射器)将请求映射到了对应的Handler上，这一步就需要考虑如何解析并执行该handler对象。

initHandlerAdapters方法和initHandlerExceptionResolvers方法也是类似的，如果没有找到，那就构建默认的。

 这里Spring使用了适配器模式，主要是因为handler对象有两种不同的类型。

```java
/**
  * Initialize the HandlerAdapters used by this class.
  * <p>If no HandlerAdapter beans are defined in the BeanFactory for this namespace,
  * we default to SimpleControllerHandlerAdapter.
  */
private void initHandlerAdapters(ApplicationContext context) {
  this.handlerAdapters = null;

  if (this.detectAllHandlerAdapters) {
    // Find all HandlerAdapters in the ApplicationContext, including ancestor contexts.
    Map<String, HandlerAdapter> matchingBeans =
        BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerAdapter.class, true, false);
    if (!matchingBeans.isEmpty()) {
      this.handlerAdapters = new ArrayList<>(matchingBeans.values());
      // We keep HandlerAdapters in sorted order.
      AnnotationAwareOrderComparator.sort(this.handlerAdapters);
    }
  }
  else {
    try {
      HandlerAdapter ha = context.getBean(HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);
      this.handlerAdapters = Collections.singletonList(ha);
    }
    catch (NoSuchBeanDefinitionException ex) {
      // Ignore, we'll add a default HandlerAdapter later.
    }
  }

  // Ensure we have at least some HandlerAdapters, by registering
  // default HandlerAdapters if no other adapters are found.
  if (this.handlerAdapters == null) {
    this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);
    if (logger.isTraceEnabled()) {
      logger.trace("No HandlerAdapters declared for servlet '" + getServletName() +
          "': using default strategies from DispatcherServlet.properties");
    }
  }
}

/**
  * Initialize the HandlerExceptionResolver used by this class.
  * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
  * we default to no exception resolver.
  */
private void initHandlerExceptionResolvers(ApplicationContext context) {
  this.handlerExceptionResolvers = null;

  if (this.detectAllHandlerExceptionResolvers) {
    // Find all HandlerExceptionResolvers in the ApplicationContext, including ancestor contexts.
    Map<String, HandlerExceptionResolver> matchingBeans = BeanFactoryUtils
        .beansOfTypeIncludingAncestors(context, HandlerExceptionResolver.class, true, false);
    if (!matchingBeans.isEmpty()) {
      this.handlerExceptionResolvers = new ArrayList<>(matchingBeans.values());
      // We keep HandlerExceptionResolvers in sorted order.
      AnnotationAwareOrderComparator.sort(this.handlerExceptionResolvers);
    }
  }
  else {
    try {
      HandlerExceptionResolver her =
          context.getBean(HANDLER_EXCEPTION_RESOLVER_BEAN_NAME, HandlerExceptionResolver.class);
      this.handlerExceptionResolvers = Collections.singletonList(her);
    }
    catch (NoSuchBeanDefinitionException ex) {
      // Ignore, no HandlerExceptionResolver is fine too.
    }
  }

  // Ensure we have at least some HandlerExceptionResolvers, by registering
  // default HandlerExceptionResolvers if no other resolvers are found.
  if (this.handlerExceptionResolvers == null) {
    this.handlerExceptionResolvers = getDefaultStrategies(context, HandlerExceptionResolver.class);
    if (logger.isTraceEnabled()) {
      logger.trace("No HandlerExceptionResolvers declared in servlet '" + getServletName() +
          "': using default strategies from DispatcherServlet.properties");
    }
  }
}
```

最后看下初始化的日志：

```shell
21:30:33.163 [RMI TCP Connection(2)-127.0.0.1] INFO org.springframework.web.servlet.DispatcherServlet - Initializing Servlet 'springmvc-demo'
21:30:38.242 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.web.context.support.XmlWebApplicationContext - Refreshing WebApplicationContext for namespace 'springmvc-demo-servlet'
21:30:39.256 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.context.annotation.ClassPathBeanDefinitionScanner - Identified candidate component class: file [/Users/pdai/pdai/www/tech-pdai-spring-demos/011-spring-framework-demo-springmvc/target/011-spring-framework-demo-springmvc-1.0-SNAPSHOT/WEB-INF/classes/tech/pdai/springframework/springmvc/controller/UserController.class]
21:30:39.261 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.context.annotation.ClassPathBeanDefinitionScanner - Identified candidate component class: file [/Users/pdai/pdai/www/tech-pdai-spring-demos/011-spring-framework-demo-springmvc/target/011-spring-framework-demo-springmvc-1.0-SNAPSHOT/WEB-INF/classes/tech/pdai/springframework/springmvc/dao/UserDaoImpl.class]
21:30:39.274 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.context.annotation.ClassPathBeanDefinitionScanner - Identified candidate component class: file [/Users/pdai/pdai/www/tech-pdai-spring-demos/011-spring-framework-demo-springmvc/target/011-spring-framework-demo-springmvc-1.0-SNAPSHOT/WEB-INF/classes/tech/pdai/springframework/springmvc/service/UserServiceImpl.class]
21:30:39.546 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.xml.XmlBeanDefinitionReader - Loaded 29 bean definitions from class path resource [springmvc.xml]
21:30:39.711 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.context.annotation.internalConfigurationAnnotationProcessor'
21:30:39.973 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.context.event.internalEventListenerProcessor'
21:30:39.984 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.context.event.internalEventListenerFactory'
21:30:39.995 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.context.annotation.internalAutowiredAnnotationProcessor'
21:30:40.003 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.context.annotation.internalCommonAnnotationProcessor'
21:30:40.042 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.ui.context.support.UiApplicationContextUtils - Unable to locate ThemeSource with name 'themeSource': using default [org.springframework.ui.context.support.ResourceBundleThemeSource@791af912]
21:30:40.052 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'userController'
21:30:40.136 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'userServiceImpl'
21:30:40.140 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'userDaoImpl'
21:30:40.147 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler#0'
21:30:40.153 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.web.servlet.handler.SimpleUrlHandlerMapping#0'
21:30:40.350 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.web.servlet.handler.MappedInterceptor#0'
21:30:40.356 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.format.support.FormattingConversionServiceFactoryBean#0'
21:30:40.741 [RMI TCP Connection(2)-127.0.0.1] DEBUG _org.springframework.web.servlet.HandlerMapping.Mappings - 'org.springframework.web.servlet.handler.SimpleUrlHandlerMapping#0' {/**=org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler@216c0f1f}
21:30:40.742 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'mvcCorsConfigurations'
21:30:40.742 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping'
21:30:40.792 [RMI TCP Connection(2)-127.0.0.1] DEBUG _org.springframework.web.servlet.HandlerMapping.Mappings - 'org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping' {}
21:30:40.792 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter'
21:30:40.793 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter'
21:30:40.794 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'localeResolver'
21:30:40.796 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'themeResolver'
21:30:40.798 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'viewNameTranslator'
21:30:40.799 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'flashMapManager'
21:30:40.805 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'mvcContentNegotiationManager'
21:30:40.887 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping'
21:30:41.150 [RMI TCP Connection(2)-127.0.0.1] DEBUG _org.springframework.web.servlet.HandlerMapping.Mappings - 
    t.p.s.s.c.UserController:
    { [/user]}: list(HttpServletRequest,HttpServletResponse)
21:30:41.202 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping - 1 mappings in 'org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping'
21:30:41.202 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter'
21:30:41.626 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter - ControllerAdvice beans: none
21:30:41.738 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'mvcUriComponentsContributor'
21:30:41.786 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter - ControllerAdvice beans: none
21:30:41.806 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver#0'
21:30:41.919 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver - ControllerAdvice beans: none
21:30:41.920 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver#0'
21:30:41.949 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver#0'
21:30:41.967 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.beans.factory.support.DefaultListableBeanFactory - Creating shared instance of singleton bean 'jspViewResolver'
21:30:44.214 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.web.servlet.DispatcherServlet - Detected AcceptHeaderLocaleResolver
21:30:44.214 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.web.servlet.DispatcherServlet - Detected FixedThemeResolver
21:31:02.141 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.web.servlet.DispatcherServlet - Detected org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator@d57bc91
21:31:03.483 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.web.servlet.DispatcherServlet - Detected org.springframework.web.servlet.support.SessionFlashMapManager@2b4e795e
21:44:08.180 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.jndi.JndiTemplate - Looking up JNDI object with name [java:comp/env/spring.liveBeansView.mbeanDomain]
21:44:08.185 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.jndi.JndiLocatorDelegate - Converted JNDI name [java:comp/env/spring.liveBeansView.mbeanDomain] not found - trying original name [spring.liveBeansView.mbeanDomain]. javax.naming.NameNotFoundException: 名称[spring.liveBeansView.mbeanDomain]未在此上下文中绑定。找不到[spring.liveBeansView.mbeanDomain]。
21:44:08.185 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.jndi.JndiTemplate - Looking up JNDI object with name [spring.liveBeansView.mbeanDomain]
21:44:08.185 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.jndi.JndiPropertySource - JNDI lookup for name [spring.liveBeansView.mbeanDomain] threw NamingException with message: 名称[spring.liveBeansView.mbeanDomain]未在此上下文中绑定。找不到[spring.liveBeansView.mbeanDomain]。. Returning null.
21:44:08.195 [RMI TCP Connection(2)-127.0.0.1] DEBUG org.springframework.web.servlet.DispatcherServlet - enableLoggingRequestDetails='false': request parameters and headers will be masked to prevent unsafe logging of potentially sensitive data
21:44:08.195 [RMI TCP Connection(2)-127.0.0.1] INFO org.springframework.web.servlet.DispatcherServlet 
```



## 总结

整体流程如下图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281540503.png)

初始化整体流程如下：

1. 加载配置文件：Spring MVC的配置文件一般为 XML 格式，通过 ApplicationContext 来加载配置文件，获取相关的bean。
2. 初始化 DispatcherServlet：DispatcherServlet 是整个 Spring MVC 的核心，它继承了 HttpServlet，实现了对 HTTP 请求的处理和响应。在初始化 DispatcherServlet 时，会调用其 init() 方法，并且为其设置一些参数，例如 SpringMVC 配置文件的位置等。
3. 初始化 HandlerMapping：HandlerMapping负责将请求映射到相应的Controller上，DispatcherServlet在初始化时会初始化HandlerMapping，并将其注册到自己的属性中。
4. 初始化 HandlerAdapter：HandlerAdapter 用于将请求对象转换成 ModelAndView 对象。在初始化时，DispatcherServlet 会根据 HandlerMapping 获取相应的 Controller，并生成相应的 HandlerAdapter。
5. 初始化 ViewResolver：ViewResolver 用于将 ModelAndView 对象渲染成具体的视图（如JSP、HTML等）。在初始化时，DispatcherServlet 会根据 ViewResolver 的配置，生成相应的ViewResolver 对象。
6. 启动 Spring MVC：DispatcherServlet 在初始化完成后，就可以监听HTTP请求，并将请求分发到相应的 Controller上 进行处理。处理完成后，将 ModelAndView 对象交给 ViewResolver 进行渲染。



<!-- @include: @article-footer.snippet.md -->     