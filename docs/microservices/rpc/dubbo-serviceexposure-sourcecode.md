---
title: dubbo - 服务暴露源码
category: 微服务
tag:
 - dubbo
 - RPC
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,dubbo,dubbo服务暴露源码,实现原理 
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---



## Dubbo 调用图解

dubbo的调用图（来自官网），如下图，共包含了5个模块

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301933480.png)

 

- Provider 服务提供方

- Registry 服务注册中心（**这里可以认为是zookeeper**

- Consumer 服务使用方

- Container 服务提供方的容器

- Monitor 服务监控中心

 

**服务调用流程**

1. 提供服务的容器启动之后，把该服务提交给服务提供方
2. 服务提供方把该服务细节以约定的协议（此处可认为是dubbo协议）把IP、端口、服务名等等上报给服务注册中心，由服务注册中心统一管理（另外存在心跳检测，便于及时了解服务健康情况；服务均衡负责等也由其管理）
3. 服务调用方从服务注册中心获取到一个可用的服务提供方的信息
4. 服务注册中心把合适的服务提供方的细节信息下发到调用方
5. 服务调用方持有服务提供方的调用信息，可直连服务提供方进行invoke调用操作
6. 服务提供方以及服务调用方的调用情况，在必备的情况下都可以定时上报到监控中心，从而了解服务调用的数据统计情况

 

接着把上述过程模块化，细化出来，如下图

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301933235.png)

 

## 引入

从spring解析xml开始，我们能够很明显的查看到DubboNamespaceHandler文件

```java
public class DubboNamespaceHandler extends NamespaceHandlerSupport implements ConfigurableSourceBeanMetadataElement {
    public DubboNamespaceHandler() {
    }

    public void init() {
        this.registerBeanDefinitionParser("application", new DubboBeanDefinitionParser(ApplicationConfig.class));
        this.registerBeanDefinitionParser("module", new DubboBeanDefinitionParser(ModuleConfig.class));
        this.registerBeanDefinitionParser("registry", new DubboBeanDefinitionParser(RegistryConfig.class));
        this.registerBeanDefinitionParser("config-center", new DubboBeanDefinitionParser(ConfigCenterBean.class));
        this.registerBeanDefinitionParser("metadata-report", new DubboBeanDefinitionParser(MetadataReportConfig.class));
        this.registerBeanDefinitionParser("monitor", new DubboBeanDefinitionParser(MonitorConfig.class));
        this.registerBeanDefinitionParser("metrics", new DubboBeanDefinitionParser(MetricsConfig.class));
        this.registerBeanDefinitionParser("tracing", new DubboBeanDefinitionParser(TracingConfig.class));
        this.registerBeanDefinitionParser("ssl", new DubboBeanDefinitionParser(SslConfig.class));
        this.registerBeanDefinitionParser("provider", new DubboBeanDefinitionParser(ProviderConfig.class));
        this.registerBeanDefinitionParser("consumer", new DubboBeanDefinitionParser(ConsumerConfig.class));
        this.registerBeanDefinitionParser("protocol", new DubboBeanDefinitionParser(ProtocolConfig.class));
        this.registerBeanDefinitionParser("service", new DubboBeanDefinitionParser(ServiceBean.class));
        this.registerBeanDefinitionParser("reference", new DubboBeanDefinitionParser(ReferenceBean.class));
        this.registerBeanDefinitionParser("annotation", new AnnotationBeanDefinitionParser());
    }

    public BeanDefinition parse(Element element, ParserContext parserContext) {
        BeanDefinitionRegistry registry = parserContext.getRegistry();
        this.registerAnnotationConfigProcessors(registry);
        DubboSpringInitializer.initialize(parserContext.getRegistry());
        BeanDefinition beanDefinition = super.parse(element, parserContext);
        this.setSource(beanDefinition);
        return beanDefinition;
    }

    private void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry) {
        AnnotationConfigUtils.registerAnnotationConfigProcessors(registry);
    }
}
```

在为外界暴露服务的时候，常使用<dubbo:service interface="com.XXX" ref="xXXX" />，再结合DubboBeanDefinitionParser类的parse细节，可知服务暴露，我们应该关注的类是**ServiceBean.class**

 

## ServiceBean 介绍

如下图，是ServiceBean类的继承关系图，右边圈出来的是关于dubbo的，下面底部的是和spring有关的。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301933709.png)

很清楚的看到serviceBean也是一个可由Spring管理的很普通的bean

- 通过BeanNameAware修改bean的名称

- ApplicationContextAware去获取Spring IOC的容器

- IntializingBean的afterPropertiesSet去自定义实现bean的实例对象

- ApplicationListener的onApplicationEvent接收各种事件

- DisposableBean的destroy去销毁bean

右侧则和dubbo有关，一层一层的config扩展实例化，包含了在xml配置中的各种参数配置。



### 属性

属性是整个的服务暴露的这个ServiceBean包含的各种属性信息，xml配置信息都会合并到这个属性中

**ServiceConfig 类**

```java
private String              interfaceName;    // 接口类型  
private Class<?>            interfaceClass;
private T                   ref;         // 接口实现类引用
private String              path;     // 服务名称
private List<MethodConfig>  methods;    // 方法配置
private ProviderConfig provider;        // 提供方配置
```



-  方法配置参数methods,一般情况下是没有设置的，也就意味着该接口下的所有的方法都会被暴露出去，如果设置了就意味着设置的方法才会被暴露出去。

- 提供方配置provider也是负责服务暴露方的一些熟悉信息，例如负载均衡等信息。

 

**AbstractServiceConfig 类**

```java
protected String               version;    // 服务版本
protected String               group;      // 服务分组
protected Boolean              deprecated;      // 服务是否已经deprecated
protected Integer              delay;     // 延迟暴露
protected Boolean              export;    // 是否暴露
protected Integer              weight;     // 权重
protected String               document;    // 应用文档
protected Boolean              dynamic;    // 在注册中心上注册成动态的还是静态的服务
protected String               token;     // 是否使用令牌
protected String               accesslog;   // 访问日志
private Integer                executes;   // 允许执行请求数
protected List<ProtocolConfig> protocols;   // 暴露的协议
private Boolean                register;   // 是否注册
```

其中protocols就是常说的dubbo协议了，这里指明list也就是意味着支持**可以同时多种协议对外暴露**

 

**AbstractInterfaceConfig 类**

```java
protected String               local;   // 服务接口的本地实现类名
protected String               stub;   // 服务接口的本地实现类名
protected MonitorConfig        monitor;   // 服务监控
protected String               proxy;    // 代理类型
protected String               cluster;   // 集群方式
protected String               filter;    // 过滤器
protected String               listener;   // 监听器
protected String               owner;   // 负责人
// 连接数限制,0表示共享连接，否则为该服务独享连接数
protected Integer              connections;
protected String               layer;    // 连接数限制
protected ApplicationConfig    application;    // 应用信息
protected ModuleConfig         module;    // 模块信息
protected List<RegistryConfig> registries;    // 注册中心
private Integer                callbacks;    // callback实例个数限制
protected String              onconnect;   // 连接事件
protected String              ondisconnect;   // 断开事件
// 服务暴露或引用的scope,如果为local，则表示只在当前JVM内查找.
private String scope;
```

注册中心registries应该是比较重要的属性信息了，包含了注册中心的数据，比如设置zk的相关属性信息，后期暴露也主要是把服务按照约定的协议推送给注册中心。

其他继承的类的属性更多的是涉及到系统管理、监控等层级的属性，在此不做过多介绍了

### 配置注入

bean继承了IntializingBean，那肯定就使用了afterPropertiesSet方法

> 注意在运行到这个时候，servicebean实例化是已经完成了的。

```java
public void afterPropertiesSet() throws Exception {
    if (getProvider() == null) {
        // 提供方为null
        Map<String, ProviderConfig> providerConfigMap = applicationContext == null ? null  : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProviderConfig.class, false, false);
        // 此处的applicationContext就是通过继承ApplicationContextAware注入的Spring IOC容器
        // 获取所有类型是ProviderConfig的bean信息
        if (providerConfigMap != null && providerConfigMap.size() > 0) {
            Map<String, ProtocolConfig> protocolConfigMap = applicationContext == null ? null  : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProtocolConfig.class, false, false);
            // 获取ProtocolConfig
            if ((protocolConfigMap == null || protocolConfigMap.size() == 0)
                    && providerConfigMap.size() > 1) { // 兼容旧版本
                    // 如果没有protocolconfig同时有providerconfig
                List<ProviderConfig> providerConfigs = new ArrayList<ProviderConfig>();
                for (ProviderConfig config : providerConfigMap.values()) {
                    if (config.isDefault() != null && config.isDefault().booleanValue()) {
                        // config不为空，而且是默认值
                        providerConfigs.add(config);
                    }
                }
                if (providerConfigs.size() > 0) {
                    setProviders(providerConfigs);
                }
            } else {
                ProviderConfig providerConfig = null;
                for (ProviderConfig config : providerConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault().booleanValue()) {
                        if (providerConfig != null) {
                            throw new IllegalStateException("Duplicate provider configs: " + providerConfig + " and " + config);
                        }
                        providerConfig = config;
                    }
                }
                if (providerConfig != null) {
                    // 默认的只应该存在一个providerconfig信息
                    setProvider(providerConfig);
                }
            }
        }
    }
    if (getApplication() == null
            && (getProvider() == null || getProvider().getApplication() == null)) {
        Map<String, ApplicationConfig> applicationConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ApplicationConfig.class, false, false);
        if (applicationConfigMap != null && applicationConfigMap.size() > 0) {
            ApplicationConfig applicationConfig = null;
            for (ApplicationConfig config : applicationConfigMap.values()) {
                if (config.isDefault() == null || config.isDefault().booleanValue()) {
                    if (applicationConfig != null) {
                        throw new IllegalStateException("Duplicate application configs: " + applicationConfig + " and " + config);
                    }
                    applicationConfig = config;
                }
            }
            if (applicationConfig != null) {
                // 填充ApplicationConfig信息
                setApplication(applicationConfig);
            }
        }
    }
    if (getModule() == null
            && (getProvider() == null || getProvider().getModule() == null)) {
        Map<String, ModuleConfig> moduleConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ModuleConfig.class, false, false);
        if (moduleConfigMap != null && moduleConfigMap.size() > 0) {
            ModuleConfig moduleConfig = null;
            for (ModuleConfig config : moduleConfigMap.values()) {
                if (config.isDefault() == null || config.isDefault().booleanValue()) {
                    if (moduleConfig != null) {
                        throw new IllegalStateException("Duplicate module configs: " + moduleConfig + " and " + config);
                    }
                    moduleConfig = config;
                }
            }
            if (moduleConfig != null) {
                // 填充ModuleConfig信息
                setModule(moduleConfig);
            }
        }
    }
    if ((getRegistries() == null || getRegistries().size() == 0)
            && (getProvider() == null || getProvider().getRegistries() == null || getProvider().getRegistries().size() == 0)
            && (getApplication() == null || getApplication().getRegistries() == null || getApplication().getRegistries().size() == 0)) {
        Map<String, RegistryConfig> registryConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, RegistryConfig.class, false, false);
        if (registryConfigMap != null && registryConfigMap.size() > 0) {
            List<RegistryConfig> registryConfigs = new ArrayList<RegistryConfig>();
            for (RegistryConfig config : registryConfigMap.values()) {
                if (config.isDefault() == null || config.isDefault().booleanValue()) {
                    registryConfigs.add(config);
                }
            }
            if (registryConfigs != null && registryConfigs.size() > 0) {
                // 填充注册信息 
                super.setRegistries(registryConfigs);
            }
        }
    }
    if (getMonitor() == null
            && (getProvider() == null || getProvider().getMonitor() == null)
            && (getApplication() == null || getApplication().getMonitor() == null)) {
        Map<String, MonitorConfig> monitorConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, MonitorConfig.class, false, false);
        if (monitorConfigMap != null && monitorConfigMap.size() > 0) {
            MonitorConfig monitorConfig = null;
            for (MonitorConfig config : monitorConfigMap.values()) {
                if (config.isDefault() == null || config.isDefault().booleanValue()) {
                    if (monitorConfig != null) {
                        throw new IllegalStateException("Duplicate monitor configs: " + monitorConfig + " and " + config);
                    }
                    monitorConfig = config;
                }
            }
            if (monitorConfig != null) {
                // 填充监控信息
                setMonitor(monitorConfig);
            }
        }
    }
    if ((getProtocols() == null || getProtocols().size() == 0)
            && (getProvider() == null || getProvider().getProtocols() == null || getProvider().getProtocols().size() == 0)) {
        Map<String, ProtocolConfig> protocolConfigMap = applicationContext == null ? null  : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProtocolConfig.class, false, false);
        if (protocolConfigMap != null && protocolConfigMap.size() > 0) {
            List<ProtocolConfig> protocolConfigs = new ArrayList<ProtocolConfig>();
            for (ProtocolConfig config : protocolConfigMap.values()) {
                if (config.isDefault() == null || config.isDefault().booleanValue()) {
                    protocolConfigs.add(config);
                }
            }
            if (protocolConfigs != null && protocolConfigs.size() > 0) {
                // 设置使用的协议
                super.setProtocols(protocolConfigs);
            }
        }
    }
    if (getPath() == null || getPath().length() == 0) {
        if (beanName != null && beanName.length() > 0 
                && getInterface() != null && getInterface().length() > 0
                && beanName.startsWith(getInterface())) {
            setPath(beanName);
        }
    }
    if (! isDelay()) {
        // 如果没有设置延迟，则立即暴露出去
        export();
    }
}
```

在这一段代码中就是从Spring IOC容器中获取合适的bean注入到ServiceBean中，例如使用的服务信息、服务注册中心、使用的协议、均衡负责的方式（provider的loanBanance）、监控等。

## 暴露服务启动

服务暴露其实就是export函数，如果设置了延迟，则会在ApplicationListener的事件中去暴露服务。

```java
public void onApplicationEvent(ApplicationEvent event) {
    if (ContextRefreshedEvent.class.getName().equals(event.getClass().getName())) {
     // 当前事件是bean刷新结束
        if (isDelay() && ! isExported() && ! isUnexported()) {
              // 是延期而且还没有暴富同时 没有 不希望 暴露服务
            if (logger.isInfoEnabled()) {
                logger.info("The service ready on spring started. service: " + getInterface());
            }
            // 跳转到ServiceConfig类中
            export();
        }
    }
}
```



**ServiceConfig 类**

```java
public synchronized void export() {
    // 这里面的provider等信息都是之前注入的信息属性
    if (provider != null) {
        if (export == null) {
            export = provider.getExport();
        }
        if (delay == null) {
            delay = provider.getDelay();
        }
    }
    if (export != null && ! export.booleanValue()) {
        return;
    }
    if (delay != null && delay > 0) {
         // 如果设置了延迟，则设置成为守护线程，睡眠延迟的时间数，再执行暴露服务的任务
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(delay);
                } catch (Throwable e) {
                }
                doExport();
            }
        });
        thread.setDaemon(true);
        thread.setName("DelayExportServiceThread");
        thread.start();
    } else {
        doExport();
    }
}
```



在doExport方法中更多的是对一些数据的check操作，随后来到了doExportUrls方法

```java
private void doExportUrls() {
    List<URL> registryURLs = loadRegistries(true);
    // 先获取注册中心的属性信息
    for (ProtocolConfig protocolConfig : protocols) {
        // 存在多个暴露协议，例如DUBBO、HTTP等，依次对外暴露
        doExportUrlsFor1Protocol(protocolConfig, registryURLs);
    }
}
```



## 服务注册中心属性获取

**AbstractInterfaceConfig 类**

```java
protected List<URL> loadRegistries(boolean provider) {
    checkRegistry();
    // 兼容老版本的dubbo服务
    List<URL> registryList = new ArrayList<URL>();
    if (registries != null && registries.size() > 0) {
        // 循环遍历所有的注册中心配置
        for (RegistryConfig config : registries) {
            String address = config.getAddress();
            // 注册中心的地址
            if (address == null || address.length() == 0) {
                address = Constants.ANYHOST_VALUE;
                // ANYHOST_VALUE = "0.0.0.0"
            }
            String sysaddress = System.getProperty("dubbo.registry.address");
            // 从系统属性中获取dubbo.registry.address的值
            if (sysaddress != null && sysaddress.length() > 0) {
                address = sysaddress;
                // 真存在这个数据，就替换掉之前的地址数据
            }
            if (address != null && address.length() > 0 
                    && ! RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(address)) {
                // RegistryConfig.NO_AVAILABLE = 'N\A',如果地址有效    
                Map<String, String> map = new HashMap<String, String>();
                appendParameters(map, application);
                appendParameters(map, config);
                map.put("path", RegistryService.class.getName());
                map.put("dubbo", Version.getVersion());
                map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
                // 把应用程序、当前的注册配置信息、以及dubbo、时间戳等信息注入到map中
                if (ConfigUtils.getPid() > 0) {
                    // 获取当前服务的进程PID
                    map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
                }
                if (! map.containsKey("protocol")) {
                    if (ExtensionLoader.getExtensionLoader(RegistryFactory.class).hasExtension("remote")) {
                        // 如果在SPI中包含了支持remote的类，则设置当前的协议为remote，否则是dubbo
                        map.put("protocol", "remote");
                    } else {
                        map.put("protocol", "dubbo");
                    }
                }
                List<URL> urls = UrlUtils.parseURLs(address, map);
                // 这里为啥会生成一个list数据的URL信息呢？
                // 主要的情况是address可能包含了多个，现实中zk也很多是zk集群了，使用逗号区分开来即可
                // 其实是通过Pattern.compile("\\s*[|;]+\\s*")的正则方式区分开来的
                for (URL url : urls) {
                    url = url.addParameter(Constants.REGISTRY_KEY, url.getProtocol());
                    // 添加注册协议，此处为dubbo
                    url = url.setProtocol(Constants.REGISTRY_PROTOCOL);
                    if ((provider && url.getParameter(Constants.REGISTER_KEY, true))
                            || (! provider && url.getParameter(Constants.SUBSCRIBE_KEY, true))) {
                            // 一定是注册方的信息，则添加到这个集合中
                        registryList.add(url);
                    }
                }
            }
        }
    }
    return registryList;
}
```

最后生成的 registryList 数据可能为 registry://127.0.0.1:2182/com.alibaba.dubbo.registry.RegistryService?application=dubbo-demo&client=zkclient&dubbo=2.5.3&group=dubbo-demo&owner=jwfy&pid=2772&registry=zookeeper&timestamp=1525276569763

仅仅是包含了一些注册的基本数据

## 服务暴露

现在来到了doExportUrlsFor1Protocol方法，当前protocolConfig为

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301934648.png)

 

```java
private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryURLs) {
    String name = protocolConfig.getName();
    // 获取对外暴露的协议，如果无效，则默认为dubbo
    if (name == null || name.length() == 0) {
        name = "dubbo";
    }

    String host = protocolConfig.getHost();
    if (provider != null && (host == null || host.length() == 0)) {
        // 如果提供的provider不为空而且暴露的协议配置host无效，设置为提供方的host数据
        host = provider.getHost();
    }
    boolean anyhost = false;
    if (NetUtils.isInvalidLocalHost(host)) {
       // 如果host无效，例如为null，空字符串，localhost，0.0.0.0，127开头的IP
        anyhost = true;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
            // 获取本机IP
        } catch (UnknownHostException e) {
            logger.warn(e.getMessage(), e);
        }
        if (NetUtils.isInvalidLocalHost(host)) {
            // 还是无效
            if (registryURLs != null && registryURLs.size() > 0) {
                for (URL registryURL : registryURLs) {
                    try {
                        Socket socket = new Socket();
                        try {
                            SocketAddress addr = new InetSocketAddress(registryURL.getHost(), registryURL.getPort());
                            socket.connect(addr, 1000);
                            host = socket.getLocalAddress().getHostAddress();
                            // 通过socket套接字的方式获取host信息
                            break;
                        } finally {
                            try {
                                socket.close();
                            } catch (Throwable e) {}
                        }
                    } catch (Exception e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
            if (NetUtils.isInvalidLocalHost(host)) {
                host = NetUtils.getLocalHost();
            }
        }
    }

    Integer port = protocolConfig.getPort();
    // 获取端口
    if (provider != null && (port == null || port == 0)) {
        port = provider.getPort();
    }
    final int defaultPort = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(name).getDefaultPort();
    // 通过SPI获取名称为name的实体类的默认端口信息，dubbo默认为20880
    if (port == null || port == 0) {
        port = defaultPort;
    }
    if (port == null || port <= 0) {
        port = getRandomPort(name);
        if (port == null || port < 0) {
            port = NetUtils.getAvailablePort(defaultPort);
            putRandomPort(name, port);
        }
        logger.warn("Use random available port(" + port + ") for protocol " + name);
    }

    Map<String, String> map = new HashMap<String, String>();
    if (anyhost) {
       // 如果起初的host为无效信息，设置anyhost为true
        map.put(Constants.ANYHOST_KEY, "true");
    }
    map.put(Constants.SIDE_KEY, Constants.PROVIDER_SIDE);
    map.put(Constants.DUBBO_VERSION_KEY, Version.getVersion());
    map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
    if (ConfigUtils.getPid() > 0) {
        map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
    }
    appendParameters(map, application);
    appendParameters(map, module);
    appendParameters(map, provider, Constants.DEFAULT_KEY);
    appendParameters(map, protocolConfig);
    appendParameters(map, this);
    if (methods != null && methods.size() > 0) {
        // 如果当前暴露的接口设置了暴露的方法列表
        for (MethodConfig method : methods) {
            appendParameters(map, method, method.getName());
            String retryKey = method.getName() + ".retry";
            if (map.containsKey(retryKey)) {
                String retryValue = map.remove(retryKey);
                if ("false".equals(retryValue)) {
                    map.put(method.getName() + ".retries", "0");
                }
            }
            List<ArgumentConfig> arguments = method.getArguments();
            if (arguments != null && arguments.size() > 0) {
                for (ArgumentConfig argument : arguments) {
                    //类型自动转换.
                    if(argument.getType() != null && argument.getType().length() >0){
                        Method[] methods = interfaceClass.getMethods();
                        //遍历所有方法
                        if(methods != null && methods.length > 0){
                            for (int i = 0; i < methods.length; i++) {
                                String methodName = methods[i].getName();
                                //匹配方法名称，获取方法签名.
                                if(methodName.equals(method.getName())){
                                    Class<?>[] argtypes = methods[i].getParameterTypes();
                                    //一个方法中单个callback
                                    if (argument.getIndex() != -1 ){
                                        if (argtypes[argument.getIndex()].getName().equals(argument.getType())){
                                            appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                                        }else {
                                            throw new IllegalArgumentException("argument config error : the index attribute and type attirbute not match :index :"+argument.getIndex() + ", type:" + argument.getType());
                                        }
                                    } else {
                                        //一个方法中多个callback
                                        for (int j = 0 ;j<argtypes.length ;j++) {
                                            Class<?> argclazz = argtypes[j];
                                            if (argclazz.getName().equals(argument.getType())){
                                                appendParameters(map, argument, method.getName() + "." + j);
                                                if (argument.getIndex() != -1 && argument.getIndex() != j){
                                                    throw new IllegalArgumentException("argument config error : the index attribute and type attirbute not match :index :"+argument.getIndex() + ", type:" + argument.getType());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }else if(argument.getIndex() != -1){
                        appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                    }else {
                        throw new IllegalArgumentException("argument config must set index or type attribute.eg: <dubbo:argument index='0' .../> or <dubbo:argument type=xxx .../>");
                    }

                }
            }
        } // end of methods for
    }
    // 到这里参数已经处理完成了

    if (generic) {
        // 暴露的服务是否为GenericService类的子类
        map.put("generic", String.valueOf(true));
        map.put("methods", Constants.ANY_VALUE);
    } else {
        String revision = Version.getVersion(interfaceClass, version);
        // 设置接口的版本号（便于新旧版本的更迭）
        if (revision != null && revision.length() > 0) {
            map.put("revision", revision);
        }

        String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
        // 获取该接口的所有的可用的方法名称集合
        if(methods.length == 0) {
            logger.warn("NO method found in service interface " + interfaceClass.getName());
            map.put("methods", Constants.ANY_VALUE);
        }
        else {
            map.put("methods", StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
            // 拼接的数据为 "getStr,print" 这样类似的数据
        }
    }
    if (! ConfigUtils.isEmpty(token)) {
        // 如果设置了校验令牌token
        if (ConfigUtils.isDefault(token)) {
           // 如果token的值为true或者default，随机设置一个UUID
            map.put("token", UUID.randomUUID().toString());
        } else {
            map.put("token", token);
        }
    }
    if ("injvm".equals(protocolConfig.getName())) {
        protocolConfig.setRegister(false);
        map.put("notify", "false");
    }
    // 导出服务
    String contextPath = protocolConfig.getContextpath();
    if ((contextPath == null || contextPath.length() == 0) && provider != null) {
        contextPath = provider.getContextpath();
    }
    URL url = new URL(name, host, port, (contextPath == null || contextPath.length() == 0 ? "" : contextPath + "/") + path, map);
    
    /*
    到这里生成的URL属于具体暴富服务的信息数据了
    例如dubbo://172.16.109.110:20880/com.jwfy.dubbo.product.ProductService?anyhost=true&application=dubbo-demo&default.loadbalance=random&dubbo=2.5.3&interface=com.jwfy.dubbo.product.ProductService&methods=print,getStr&owner=jwfy&pid=2772&side=provider&timestamp=1525313423899
    URL的协议是dubbo，也是暴露出去的协议
    */

    if (ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
            .hasExtension(url.getProtocol())) {
            // 如果配置工厂类有相应协议，则重新设置其值
        url = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                .getExtension(url.getProtocol()).getConfigurator(url).configure(url);
    }

    String scope = url.getParameter(Constants.SCOPE_KEY);
    //配置为none不暴露，此处为null,也就是既可以暴露为本地服务也可以暴露为远程服务
    if (! Constants.SCOPE_NONE.toString().equalsIgnoreCase(scope)) {

        //配置不是remote的情况下做本地暴露 (配置为remote，则表示只暴露远程服务)
        if (!Constants.SCOPE_REMOTE.toString().equalsIgnoreCase(scope)) {
            // 真正的暴露服务，后续详解
            exportLocal(url);
        }
        //如果配置不是local则暴露为远程服务.(配置为local，则表示只暴露远程服务)
        if (! Constants.SCOPE_LOCAL.toString().equalsIgnoreCase(scope) ){
            if (logger.isInfoEnabled()) {
                logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url);
            }
            if (registryURLs != null && registryURLs.size() > 0
                    && url.getParameter("register", true)) {
                // 对外暴露服务，必须要注册中心存在
                for (URL registryURL : registryURLs) {
                    url = url.addParameterIfAbsent("dynamic", registryURL.getParameter("dynamic"));
                    URL monitorUrl = loadMonitor(registryURL);
                    // 加载监控中心，后续详解
                    if (monitorUrl != null) {
                        url = url.addParameterAndEncoded(Constants.MONITOR_KEY, monitorUrl.toFullString());
                    }
                    if (logger.isInfoEnabled()) {
                        logger.info("Register dubbo service " + interfaceClass.getName() + " url " + url + " to registry " + registryURL);
                    }
                    Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, registryURL.addParameterAndEncoded(Constants.EXPORT_KEY, url.toFullString()));
                    // 远程服务暴露
                    Exporter<?> exporter = protocol.export(invoker);
                    exporters.add(exporter);
                }
            } else {
                Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, url);

                Exporter<?> exporter = protocol.export(invoker);
                exporters.add(exporter);
            }
        }
    }
    this.urls.add(url);
}
```



### 本地服务暴露

本地服务暴露的入口是exportLocal(url);

```java
private void exportLocal(URL url) {
    if (!Constants.LOCAL_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
        // 这时候的url.getProtocol 为dubbo，而本地服务是injvm
        // 而且如果服务已经是injvm 则不需要走这个操作暴露了
        URL local = URL.valueOf(url.toFullString())
                .setProtocol(Constants.LOCAL_PROTOCOL)
                .setHost(NetUtils.LOCALHOST)
                .setPort(0);
        // 替换为injvm协议和端口号，这个local数据就变成了
        /*
        injvm://127.0.0.1/com.jwfy.dubbo.product.ProductService?anyhost=true&application=dubbo-demo&default.loadbalance=random&dubbo=2.5.3&interface=com.jwfy.dubbo.product.ProductService&methods=print,getStr&owner=jwfy&pid=2772&side=provider&timestamp=1525313423899
        */
        Exporter<?> exporter = protocol.export(
                proxyFactory.getInvoker(ref, (Class) interfaceClass, local));
        // 通过getInvoke获取invoke对象，通过export进行暴露操作                       
        exporters.add(exporter);
        logger.info("Export dubbo service " + interfaceClass.getName() +" to local registry");
    }
}
```

现在应该是来到了真正服务暴露的入口了，`Exporter<?> exporter = protocol.export(proxyFactory.getInvoker(ref, (Class) interfaceClass, local))`;

### protocol & proxyFactory

根据之前对dubbo spi的学习和了解，到这里已经知道了protocol和proxyFactory指的具体是什么了。

 

**protocol 类**

```java
package com.alibaba.dubbo.rpc;
import com.alibaba.dubbo.common.extension.ExtensionLoader;

public class Protocol$Adpative implements com.alibaba.dubbo.rpc.Protocol {
    public com.alibaba.dubbo.rpc.Invoker refer(java.lang.Class arg0, com.alibaba.dubbo.common.URL arg1) throws java.lang.Class {
        if (arg1 == null) 
            throw new IllegalArgumentException("url == null");

        com.alibaba.dubbo.common.URL url = arg1;
        String extName = ( url.getProtocol() == null ? "dubbo" : url.getProtocol() );
        if(extName == null) 
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.Protocol) name from url(" + url.toString() + ") use keys([protocol])");

        com.alibaba.dubbo.rpc.Protocol extension = (com.alibaba.dubbo.rpc.Protocol)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.Protocol.class).getExtension(extName);

        return extension.refer(arg0, arg1);
    }

    public com.alibaba.dubbo.rpc.Exporter export(com.alibaba.dubbo.rpc.Invoker arg0) throws com.alibaba.dubbo.rpc.Invoker {
        if (arg0 == null) 
            throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument == null");

        if (arg0.getUrl() == null) 
            throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument getUrl() == null");com.alibaba.dubbo.common.URL url = arg0.getUrl();
        //根据URL配置信息获取Protocol协议，默认是dubbo
        String extName = ( url.getProtocol() == null ? "dubbo" : url.getProtocol() );
        if(extName == null) 
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.Protocol) name from url(" + url.toString() + ") use keys([protocol])");
            //根据协议名，获取Protocol实体类
        com.alibaba.dubbo.rpc.Protocol extension = (com.alibaba.dubbo.rpc.Protocol)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.Protocol.class).getExtension(extName);

        return extension.export(arg0);
    }

    public void destroy() {
        throw new UnsupportedOperationException("method public abstract void com.alibaba.dubbo.rpc.Protocol.destroy() of interface com.alibaba.dubbo.rpc.Protocol is not adaptive method!");
    }

    public int getDefaultPort() {
        throw new UnsupportedOperationException("method public abstract int com.alibaba.dubbo.rpc.Protocol.getDefaultPort() of interface com.alibaba.dubbo.rpc.Protocol is not adaptive method!");
    }
}
```



**proxyFactory 类**

```java
package com.alibaba.dubbo.rpc;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
public class ProxyFactory$Adpative implements com.alibaba.dubbo.rpc.ProxyFactory {
    public com.alibaba.dubbo.rpc.Invoker getInvoker(java.lang.Object arg0, java.lang.Class arg1, com.alibaba.dubbo.common.URL arg2) throws java.lang.Object {
        if (arg2 == null) 
            throw new IllegalArgumentException("url == null");

        com.alibaba.dubbo.common.URL url = arg2;
        String extName = url.getParameter("proxy", "javassist");
        if(extName == null) 
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.ProxyFactory) name from url(" + url.toString() + ") use keys([proxy])");

        com.alibaba.dubbo.rpc.ProxyFactory extension = (com.alibaba.dubbo.rpc.ProxyFactory)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.ProxyFactory.class).getExtension(extName);
        // 获取ProxyFactory类的名称为extName的实现类

        return extension.getInvoker(arg0, arg1, arg2);
        // arg0 是具体被调用的实现类
        // arg1是实现类的接口
        // arg2 是对外暴露的URL信息
        // 只是调用链路已经转交给具体的extension了
    }

    public java.lang.Object getProxy(com.alibaba.dubbo.rpc.Invoker arg0) throws com.alibaba.dubbo.rpc.Invoker {
        if (arg0 == null) 
            throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument == null");

       if (arg0.getUrl() == null) 
        throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument getUrl() == null");com.alibaba.dubbo.common.URL url = arg0.getUrl();

        String extName = url.getParameter("proxy", "javassist");
        // 获取URL中的proxy参数信息，没有则设置为javassist
        if(extName == null) 
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.ProxyFactory) name from url(" + url.toString() + ") use keys([proxy])");

        com.alibaba.dubbo.rpc.ProxyFactory extension = (com.alibaba.dubbo.rpc.ProxyFactory)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.ProxyFactory.class).getExtension(extName);

        return extension.getProxy(arg0);
    }
}
```

可以很明显的看出来默认的protocol是使用的dubbo协议，对应的实例是包装DubboProtocol后的实例，ProxyFactory使用的是javassist，对应的实例是JavassistProxyFactory

**暴露操作包含了两个部分，一个Invoke,另一个是export**

## 获取Invoke

### JavassistProxyFactory获取Invoke

proxyFactory.getInvoker(ref, (Class) interfaceClass, local) 方法获得具体的Invoke。

此时的ref是暴露的具体实现类，interfaceClass是对应的接口信息，local就是URL信息，具体内容是

> registry://127.0.0.1:2182/com.alibaba.dubbo.registry.RegistryService?application=dubbo-demo&client=zkclient&dubbo=2.5.3&export=dubbo%3A%2F%2F172.16.109.110%3A20880%2Fcom.jwfy.dubbo.product.ProductService%3Fanyhost%3Dtrue%26application%3Ddubbo-demo%26default.loadbalance%3Drandom%26dubbo%3D2.5.3%26interface%3Dcom.jwfy.dubbo.product.ProductService%26methods%3Dprint%2CgetStr%26owner%3Djwfy%26pid%3D13859%26side%3Dprovider%26timestamp%3D1525772505371%26token%3Dfdfdf&group=dubbo-demo&owner=jwfy&pid=13859&registry=zookeeper&timestamp=1525772500246

需要对外暴露的服务就是包含在URL信息中的ProductService信息

在本demo中，getInvoke操作获取到JavassistProxyFactory对象后执行他的getInvoke操作

 

**JavassistProxyFactory 类**

```java
public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
    // TODO Wrapper类不能正确处理带$的类名
    final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);
    // 这里生产的wrapper也是动态生成的
    return new AbstractProxyInvoker<T>(proxy, type, url) {
        @Override
        protected Object doInvoke(T proxy, String methodName, 
                                  Class<?>[] parameterTypes, 
                                  Object[] arguments) throws Throwable {
                                  // 这个proxy也就是具体的实现类，对应上面的ref
                                  // methodName是方法名
                                  // 剩下的是参数类型以及名称
            return wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments);
        }
    };
}
```

生产的invoke对象其实是个AbstractProxyInvoker，只不过在调用他的doInvoke操作时，最后会执行拼接生成的wrapper对象的invokeMethod方法上。

 

**getWrapper**

在获取wrapper操作，也同样是**动态拼接字符串生成**的，重点看其中的invokeMethod方法

```java
public Object invokeMethod(Object o, String n, Class[] p, Object[] v) 
                     throws java.lang.reflect.InvocationTargetException{ 
com.jwfy.dubbo.product.ProductServiceImpl w; 
// 看这个就已经非常明显的说明实现类的具体对象
try{ 
   w = ((com.jwfy.dubbo.product.ProductServiceImpl)$1); 
   // 格式化，强转类型
} catch (Throwable e) { 
  throw new IllegalArgumentException(e); 
} 

try{ 
  // 对函数名称和参数进行匹配校验操作
  if( "print".equals( $2 )  &&  $3.length == 0 ) {  
     w.print(); 
     // 函数本身是返回void类型，则直接返回
     return null; 
   } 
   if( "getStr".equals( $2 )  &&  $3.length == 0 ) {  
      // 调用执行结果后强转格式返回
      return ($w)w.getStr(); 
    } 
 } catch(Throwable e) {      
    throw new java.lang.reflect.InvocationTargetException(e);  
 } 

// 每个接口的方法都会遍历一遍，如果啥都没匹配到，就提示没有方法异常
throw new com.alibaba.dubbo.common.bytecode.NoSuchMethodException("Not found method \""+$2+"\" in class com.jwfy.dubbo.product.ProductServiceImpl."); 
}
```

现在应该就很清楚了，在执行invokeMethod的时候，**背后其实就是调用了实现类的对应方法**，只是这个wrapper本身是动态生成的

 

### JdkProxyFactory获取Invoke

上面说了在动态生成的代理工厂中默认实现的是JavassistProxyFactory，但是也可以使用java本身的协议，也就是JdkProxyFactory

```java
public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
    return new AbstractProxyInvoker<T>(proxy, type, url) {
        @Override
        protected Object doInvoke(T proxy, String methodName, 
                                  Class<?>[] parameterTypes, 
                                  Object[] arguments) throws Throwable {
            Method method = proxy.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(proxy, arguments);
        }
    };
}
```

完全就是通过java的反射去调用执行



### Invoke是什么

其实刚刚开始看源码的时候并不是非常的理解Invoke到底是个什么，现在可以说Invoke是**实现类的包装类，并包含了URL等信息**，后续可以通过invoke方法去调用具体服务方。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301934939.png)



## Invoke暴露为export

### 获取真实的Protocol类

protocol.export(invoke),其中的invoke就是上面生成的抽象invoke类,**可是在单步调试的时候却发现并没有直接进入到我们设想的RegistryProtocol类中**

这个需要追踪到Dubbo SPI中的cachedWrapperClasses数据处理中

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301935300.png)

上述代码已经很清楚了，获取wrapper，首先不应该被Adaptive注解（未贴出），其次一定得存在包含了**参数为type的构造函数**，而如下文件则是protocol的spi文件，可以知道只有filter和listener符合操作

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301935396.png)

 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301935837.png)

 这样就非常清楚了，在获取getExtension中，应该是获取到了RegistryProtocol对象，但是后续的cachedWrapperClasses操作又加上了包装操作，先后加入了ProtocolFilterWrapper、ProtocolListenerWrapper对象，使得在后续protocol.export操作不是进入到RegistryProtocol中，而是首先进入到ProtocolFilterWrapper

 

**ProtocolFilterWrapper 类**

```java
public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
    if (Constants.REGISTRY_PROTOCOL.equals(invoker.getUrl().getProtocol())) {
        // 如果invoke的协议是registry类型
       return protocol.export(invoker);
    }
    return protocol.export(buildInvokerChain(invoker, Constants.SERVICE_FILTER_KEY, Constants.PROVIDER));
}
```

 

**然后来到了ProtocolListenerWrapper类**

```java
public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
    if (Constants.REGISTRY_PROTOCOL.equals(invoker.getUrl().getProtocol())) {
        // 如果invoke的协议是registry类型
        return protocol.export(invoker);
    }
    return new ListenerExporterWrapper<T>(protocol.export(invoker), 
            Collections.unmodifiableList(ExtensionLoader.getExtensionLoader(ExporterListener.class)
                    .getActivateExtension(invoker.getUrl(), Constants.EXPORTER_LISTENER_KEY)));
}
```



### 注册协议暴露

不经过任何处理，通过两个wrapper的转发，直接来到RegistryProtocol的export操作

```java
public <T> Exporter<T> export(final Invoker<T> originInvoker) throws RpcException {
    final ExporterChangeableWrapper<T> exporter = doLocalExport(originInvoker);
    // 本地暴露服务
    final Registry registry = getRegistry(originInvoker);
    // 获取远程注册中心
    final URL registedProviderUrl = getRegistedProviderUrl(originInvoker);
    // 注册到注册中心的URL信息，其中包含了一个接口的信息以及协议等信息
    registry.register(registedProviderUrl);
    // 注册操作，如果查看zk记录，可以发现注册成功的操作日志
    
    // 订阅override数据
    // FIXME 提供者订阅时，会影响同一JVM即暴露服务，又引用同一服务的的场景，因为subscribed以服务名为缓存的key，导致订阅信息覆盖。
    final URL overrideSubscribeUrl = getSubscribedOverrideUrl(registedProviderUrl);
    final OverrideListener overrideSubscribeListener = new OverrideListener(overrideSubscribeUrl);
    overrideListeners.put(overrideSubscribeUrl, overrideSubscribeListener);
    registry.subscribe(overrideSubscribeUrl, overrideSubscribeListener);
    //保证每次export都返回一个新的exporter实例
    return new Exporter<T>() {
        public Invoker<T> getInvoker() {
            return exporter.getInvoker();
        }
        public void unexport() {
            try {
                exporter.unexport();
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
            try {
                registry.unregister(registedProviderUrl);
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
            try {
                overrideListeners.remove(overrideSubscribeUrl);
                registry.unsubscribe(overrideSubscribeUrl, overrideSubscribeListener);
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
        }
    };
}
```



#### 获取远程控制中心地址 getRegistry

```java
private Registry getRegistry(final Invoker<?> originInvoker){
    URL registryUrl = originInvoker.getUrl();
    // 获取的invoke的URL信息
    // registry://127.0.0.1:2182/com.alibaba.dubbo.registry.RegistryService?
    //application=dubbo-demo&client=zkclient&dubbo=2.5.3&export=dubbo
    //%3A%2F%2F192.168.10.123%3A20880%2Fcom.jwfy.dubbo.product.ProductService
    //%3Fanyhost%3Dtrue%26application%3Ddubbo-demo%26default.loadbalance%3Drandom
    //%26dubbo%3D2.5.3%26interface%3Dcom.jwfy.dubbo.product.ProductService%26methods
    // %3Dprint%2CgetStr%26owner%3Djwfy%26pid%3D12663%26side%3Dprovider
    // %26timestamp%3D1525733684167%26token%3Dfdfdf&
    // group=dubbo-demo&owner=jwfy&pid=12663&registry=zookeeper&timestamp=1525733671009
    if (Constants.REGISTRY_PROTOCOL.equals(registryUrl.getProtocol())) {
       // 如果协议是register
        String protocol = registryUrl.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_DIRECTORY);
        // 默认协议是dubbo
        registryUrl = registryUrl.setProtocol(protocol).removeParameter(Constants.REGISTRY_KEY);
        // 替换协议为dubbo，并移除参数中的注册数据
        // 现在的URL信息是
        // zookeeper://127.0.0.1:2182/com.alibaba.dubbo.registry.RegistryService?
        // application=dubbo-demo&client=zkclient&dubbo=2.5.3&export=dubbo%3A%2F%2F192.168.10.123%3A20880
        //%2Fcom.jwfy.dubbo.product.ProductService%3Fanyhost%3Dtrue%26application%3Ddubbo-demo%26default.loadbalance%3Drandom%26dubbo%3D2.5.3
        //%26interface%3Dcom.jwfy.dubbo.product.ProductService%26
        //methods%3Dprint%2CgetStr%26owner%3Djwfy%26pid%3D12663%26
        // side%3Dprovider%26timestamp%3D1525733684167%26
        //token%3Dfdfdf&group=dubbo-demo&owner=jwfy&pid=12663&timestamp=1525733671009
    }
    return registryFactory.getRegistry(registryUrl);
}
```

registryFactory 同样是在RegistryProtocol实例完后注入的动态对象

```java
import com.alibaba.dubbo.common.extension.ExtensionLoader;
public class RegistryFactory$Adpative implements com.alibaba.dubbo.registry.RegistryFactory {
    public com.alibaba.dubbo.registry.Registry getRegistry(com.alibaba.dubbo.common.URL arg0) {
    
        if (arg0 == null) throw new IllegalArgumentException("url == null");

        com.alibaba.dubbo.common.URL url = arg0;
        String extName = ( url.getProtocol() == null ? "dubbo" : url.getProtocol() );

        if(extName == null) throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.registry.RegistryFactory) name from url(" + url.toString() + ") use keys([protocol])");

        com.alibaba.dubbo.registry.RegistryFactory extension = (com.alibaba.dubbo.registry.RegistryFactory)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.registry.RegistryFactory.class).getExtension(extName);

        return extension.getRegistry(arg0);
    }
}
```



对应实现的对象是ZookeeperRegistryFactory，调用其getRegistry方法，来到了AbstractRegistryFactory类

```java
public Registry getRegistry(URL url) {
    url = url.setPath(RegistryService.class.getName())
            .addParameter(Constants.INTERFACE_KEY, RegistryService.class.getName())
            .removeParameters(Constants.EXPORT_KEY, Constants.REFER_KEY);
    String key = url.toServiceString();
    // 获取的key是zookeeper://127.0.0.1:2182/dubbo-demo/com.alibaba.dubbo.registry.RegistryService
    // 锁定注册中心获取过程，保证注册中心单一实例
    LOCK.lock();
    try {
        Registry registry = REGISTRIES.get(key);
        // 从缓存获取注册中心，REGISTRIES是一个线程安全的map
        if (registry != null) {
            return registry;
        }
        registry = createRegistry(url);
        // 创建一个注册中心ZookeeperRegistry
        if (registry == null) {
            throw new IllegalStateException("Can not create registry " + url);
        }
        REGISTRIES.put(key, registry);
        return registry;
    } finally {
        // 释放锁
        LOCK.unlock();
    }
}
```



**AbstractRegistry 类**

```java
public AbstractRegistry(URL url) {
    setUrl(url);
    // 启动文件保存定时器
    syncSaveFile = url.getParameter(Constants.REGISTRY_FILESAVE_SYNC_KEY, false);
    String filename = url.getParameter(Constants.FILE_KEY, System.getProperty("user.home") + "/.dubbo/dubbo-registry-" + url.getHost() + ".cache");
    // 文件名为/Users/XXX/.dubbo/dubbo-registry-127.0.0.1.cache
    File file = null;
    if (ConfigUtils.isNotEmpty(filename)) {
        file = new File(filename);
        if(! file.exists() && file.getParentFile() != null && ! file.getParentFile().exists()){
            if(! file.getParentFile().mkdirs()){
                throw new IllegalArgumentException("Invalid registry store file " + file + ", cause: Failed to create directory " + file.getParentFile() + "!");
            }
        }
    }
    this.file = file;
    loadProperties();
    // 从.cache 文件中获取已经存储的zk信息
    notify(url.getBackupUrls());
    // 通知订阅
}
```

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301935660.png)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301936156.png)

 

来到订阅notify方法

```java
protected void notify(List<URL> urls) {
    if(urls == null || urls.isEmpty()) return;
    
    for (Map.Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
        // 获取订阅者
        URL url = entry.getKey();
        
        if(! UrlUtils.isMatch(url, urls.get(0))) {
            continue;
        }
        
        Set<NotifyListener> listeners = entry.getValue();
        if (listeners != null) {
            for (NotifyListener listener : listeners) {
                try {
                    notify(url, listener, filterEmpty(url, urls));
                    // 真正的通知触发
                } catch (Throwable t) {
                    logger.error("Failed to notify registry event, urls: " +  urls + ", cause: " + t.getMessage(), t);
                }
            }
        }
    }
}
```

```java
protected void notify(URL url, NotifyListener listener, List<URL> urls) {
    if (url == null) {
        throw new IllegalArgumentException("notify url == null");
    }
    if (listener == null) {
        throw new IllegalArgumentException("notify listener == null");
    }
    if ((urls == null || urls.size() == 0) 
            && ! Constants.ANY_VALUE.equals(url.getServiceInterface())) {
        logger.warn("Ignore empty notify urls for subscribe url " + url);
        return;
    }
    if (logger.isInfoEnabled()) {
        logger.info("Notify urls for subscribe url " + url + ", urls: " + urls);
    }
    Map<String, List<URL>> result = new HashMap<String, List<URL>>();
    for (URL u : urls) {
        if (UrlUtils.isMatch(url, u)) {
            // 处理的URL信息u和url是匹配的
            String category = u.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
            List<URL> categoryList = result.get(category);
            if (categoryList == null) {
                categoryList = new ArrayList<URL>();
                result.put(category, categoryList);
            }
            categoryList.add(u);
        }
    }
    // 一个类目下存储的多个URL信息
    if (result.size() == 0) {
        return;
    }
    Map<String, List<URL>> categoryNotified = notified.get(url);
    if (categoryNotified == null) {
        notified.putIfAbsent(url, new ConcurrentHashMap<String, List<URL>>());
        categoryNotified = notified.get(url);
    }
    for (Map.Entry<String, List<URL>> entry : result.entrySet()) {
        String category = entry.getKey();
        List<URL> categoryList = entry.getValue();
        categoryNotified.put(category, categoryList);
        saveProperties(url);
        // 存储到文件中，对于上面的读取文件
        listener.notify(categoryList);
        // 由监听器掌握处理
    }
}
```



**FailbackRegistry 类**

```java
public FailbackRegistry(URL url) {
    super(url);
    int retryPeriod = url.getParameter(Constants.REGISTRY_RETRY_PERIOD_KEY, Constants.DEFAULT_REGISTRY_RETRY_PERIOD);
    // 设置重试的时间，默认为5s，当链接失败就会触发这个重连操作
    this.retryFuture = retryExecutor.scheduleWithFixedDelay(new Runnable() {
        public void run() {
            // 检测并连接注册中心
            try {
                retry();
            } catch (Throwable t) { // 防御性容错
                logger.error("Unexpected error occur at failed retry, cause: " + t.getMessage(), t);
            }
        }
    }, retryPeriod, retryPeriod, TimeUnit.MILLISECONDS);
}
```

链接失败时，进行重试的操作就是在这里进行的，retry就是获取当前registry中的failedRegistered等信息，如果failedRegistered中有URL信息存在，意味着之前存在链接失败的情况，现在执行retry进行重连操作

 

**ZookeeperRegistry 类**

```java
public ZookeeperRegistry(URL url, ZookeeperTransporter zookeeperTransporter) {
    super(url);
    if (url.isAnyHost()) {
        throw new IllegalStateException("registry address == null");
    }
    String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT);
    // dubbo 分组概念 group
    if (! group.startsWith(Constants.PATH_SEPARATOR)) {
        // 如果组名称不是/开头，则添加该数据
        group = Constants.PATH_SEPARATOR + group;
    }
    this.root = group;
    zkClient = zookeeperTransporter.connect(url);
    // 调用zk的操作方式连接注册中心，并持有该client
    zkClient.addStateListener(new StateListener() {
        public void stateChanged(int state) {
            if (state == RECONNECTED) {
                try {
                    recover();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    });
}
```



通过上述操作得到的注册中心对象实例，并且其URL为zookeeper://127.0.0.1:2182/com.alibaba.dubbo.registry.RegistryService?application=dubbo-demo&client=zkclient&dubbo=2.5.3&group=dubbo-demo&interface=com.alibaba.dubbo.registry.RegistryService&owner=jwfy&pid=12663&timestamp=1525733671009

 

 

#### 注册到注册中心

```java
final URL registedProviderUrl = getRegistedProviderUrl(originInvoker);
// 先获取invoke对象的注册URL
registry.register(registedProviderUrl);
// 注册到注册中心
// 此时观察zk的日志会发现注册操作
```



从invoke对错获取的注册URL是dubbo://192.168.10.123:20880/com.jwfy.dubbo.product.ProductService?anyhost=true&application=dubbo-demo&default.loadbalance=random&dubbo=2.5.3&interface=com.jwfy.dubbo.product.ProductService&methods=print,getStr&owner=jwfy&pid=12663&side=provider&timestamp=1525733684167&token=fdfdf

其包含了当前bean的基本信息，把这些信息提交给注册中心，服务使用方就可以获取到这些数据，然后反转生成invoke去调用执行

 

**FailbackRegistry 类**

```java
public void register(URL url) {
    super.register(url);
    failedRegistered.remove(url);
    failedUnregistered.remove(url);
    try {
        // 向服务器端发送注册请求
        // 真正调用ZK包的接口注册到zk注册中心
        doRegister(url);
    } catch (Exception e) {
        Throwable t = e;

        // 如果开启了启动时检测，则直接抛出异常
        boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                && url.getParameter(Constants.CHECK_KEY, true)
                && ! Constants.CONSUMER_PROTOCOL.equals(url.getProtocol());
        boolean skipFailback = t instanceof SkipFailbackWrapperException;
        if (check || skipFailback) {
            if(skipFailback) {
                t = t.getCause();
            }
            throw new IllegalStateException("Failed to register " + url + " to registry " + getUrl().getAddress() + ", cause: " + t.getMessage(), t);
        } else {
            logger.error("Failed to register " + url + ", waiting for retry, cause: " + t.getMessage(), t);
        }

        // 将失败的注册请求记录到失败列表，定时重试
        // 上面的rety方法就有使用这个failedRegistered容器内的数据
        failedRegistered.add(url);
    }
}
```



以下就是zk的输出日志，可以很清晰的看到确实创建了节点信息

```java
2018-05-08 22:52:52,808 - INFO  [NIOServerCxn.Factory:0.0.0.0/0.0.0.0:2182:NIOServerCnxn$Factory@251] - Accepted socket connection from /127.0.0.1:55708
2018-05-08 22:52:52,926 - INFO  [NIOServerCxn.Factory:0.0.0.0/0.0.0.0:2182:NIOServerCnxn@770] - Client attempting to renew session 0x163267acb7f000b at /127.0.0.1:55708
2018-05-08 22:52:52,941 - INFO  [NIOServerCxn.Factory:0.0.0.0/0.0.0.0:2182:NIOServerCnxn@1573] - Invalid session 0x163267acb7f000b for client /127.0.0.1:55708, probably expired
2018-05-08 22:52:52,949 - INFO  [NIOServerCxn.Factory:0.0.0.0/0.0.0.0:2182:NIOServerCnxn@1435] - Closed socket connection for client /127.0.0.1:55708 which had sessionid 0x163267acb7f000b
2018-05-08 22:52:53,437 - INFO  [NIOServerCxn.Factory:0.0.0.0/0.0.0.0:2182:NIOServerCnxn$Factory@251] - Accepted socket connection from /127.0.0.1:55709
2018-05-08 22:52:53,444 - INFO  [NIOServerCxn.Factory:0.0.0.0/0.0.0.0:2182:NIOServerCnxn@777] - Client attempting to establish new session at /127.0.0.1:55709
2018-05-08 22:52:53,454 - INFO  [SyncThread:0:NIOServerCnxn@1580] - Established session 0x163267acb7f000c with negotiated timeout 30000 for client /127.0.0.1:55709
2018-05-08 22:52:56,957 - INFO  [ProcessThread:-1:PrepRequestProcessor@419] - Got user-level KeeperException when processing sessionid:0x163267acb7f000c type:create cxid:0x1 zxid:0xfffffffffffffffe txntype:unknown reqpath:n/a Error Path:/dubbo-demo Error:KeeperErrorCode = NodeExists for /dubbo-demo
2018-05-08 22:52:57,018 - INFO  [ProcessThread:-1:PrepRequestProcessor@419] - Got user-level KeeperException when processing sessionid:0x163267acb7f000c type:create cxid:0x2 zxid:0xfffffffffffffffe txntype:unknown reqpath:n/a Error Path:/dubbo-demo/com.jwfy.dubbo.product.ProductService Error:KeeperErrorCode = NodeExists for /dubbo-demo/com.jwfy.dubbo.product.ProductService
2018-05-08 22:52:57,029 - INFO  [ProcessThread:-1:PrepRequestProcessor@419] - Got user-level KeeperException when processing sessionid:0x163267acb7f000c type:create cxid:0x3 zxid:0xfffffffffffffffe txntype:unknown reqpath:n/a Error Path:/dubbo-demo/com.jwfy.dubbo.product.ProductService/providers Error:KeeperErrorCode = NodeExists for /dubbo-demo/com.jwfy.dubbo.product.ProductService/providers
```

到这里服务注册到注册中心就已经完成了，同时还伴随着从文件加载注册信息和保存注册信息，可自行通过zKcli命令去



#### 暴露服务之注册

```java
private <T> ExporterChangeableWrapper<T>  doLocalExport(final Invoker<T> originInvoker){
    // invoke的URL信息是registry://127.0.0.1:2182/com.alibaba.dubbo.registry.RegistryService? 
    // application=dubbo-demo&client=zkclient&dubbo=2.5.3&export=dubbo%3A%2F%2F172.16.109.110%3A20880%2F
    // com.jwfy.dubbo.product.ProductService%3Fanyhost%3Dtrue%26application%3D
    // dubbo-demo%26default.loadbalance%3Drandom%26dubbo%3D2.5.3%26
    // interface%3Dcom.jwfy.dubbo.product.ProductService%26methods%3Dprint%2CgetStr%26
    // owner%3Djwfy%26pid%3D13375%26side%3Dprovider%26timestamp%3D1525749656129%26
    // token%3Dfdfdf&group=dubbo-demo&owner=jwfy&pid=13375&registry=zookeeper&
    // timestamp=1525749652060
    String key = getCacheKey(originInvoker);
    // key就是dubbo://172.16.109.110:20880/com.jwfy.dubbo.product.ProductService?  
    // anyhost=true&application=dubbo-demo&default.loadbalance=random&dubbo=2.5.3&interface=com.jwfy.dubbo.product.ProductService
    // &methods=print,getStr&owner=jwfy&pid=13375&side=provider&timestamp=1525749656129&token=fdfdf
    ExporterChangeableWrapper<T> exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
    if (exporter == null) {
        synchronized (bounds) {
            exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
            if (exporter == null) {
                // 又出现了double-check操作
                final Invoker<?> invokerDelegete = new InvokerDelegete<T>(originInvoker, getProviderUrl(originInvoker));
                // 包装类
                exporter = new ExporterChangeableWrapper<T>((Exporter<T>)protocol.export(invokerDelegete), originInvoker);
                
                bounds.put(key, exporter);
            }
        }
    }
    return (ExporterChangeableWrapper<T>) exporter;
}
```

在上面的protocol.export操作中，protocol也不是DubboProtocol本身，而是包装了ProtocolFilterWrapper、ProtocolListenerWrapper，协议不是register，各种处理之后进入到DubboProtocol的export进行暴露操作。

 

#### 网络端口开启



**DubboProtocol 类**

```java
public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
    URL url = invoker.getUrl();
    // URL是dubbo://172.16.109.110:20880/com.jwfy.dubbo.product.ProductService?anyhost=true&
    //application=dubbo-demo&default.loadbalance=random&dubbo=2.5.3&
    //interface=com.jwfy.dubbo.product.ProductService&methods=print,getStr&owner=jwfy
   // &pid=13375&side=provider&timestamp=1525749656129&token=fdfdf
    
    // export service.
    String key = serviceKey(url);
    // key是com.jwfy.dubbo.product.ProductService:20880
    DubboExporter<T> exporter = new DubboExporter<T>(invoker, key, exporterMap);
    // 组成成为一个新的包装类DubboExporter
    exporterMap.put(key, exporter);
    
    //export an stub service for dispaching event
    Boolean isStubSupportEvent = url.getParameter(Constants.STUB_EVENT_KEY,Constants.DEFAULT_STUB_EVENT);
    Boolean isCallbackservice = url.getParameter(Constants.IS_CALLBACK_SERVICE, false);
    if (isStubSupportEvent && !isCallbackservice){
        String stubServiceMethods = url.getParameter(Constants.STUB_EVENT_METHODS_KEY);
        if (stubServiceMethods == null || stubServiceMethods.length() == 0 ){
            if (logger.isWarnEnabled()){
                logger.warn(new IllegalStateException("consumer [" +url.getParameter(Constants.INTERFACE_KEY) +
                        "], has set stubproxy support event ,but no stub methods founded."));
            }
        } else {
            stubServiceMethodsMap.put(url.getServiceKey(), stubServiceMethods);
        }
    }

    openServer(url);
    // 来了，最关键的时候
    
    return exporter;
}
```

```java
private ExchangeServer createServer(URL url) {
    //默认开启server关闭时发送readonly事件
    url = url.addParameterIfAbsent(Constants.CHANNEL_READONLYEVENT_SENT_KEY, Boolean.TRUE.toString());
    //默认开启heartbeat
    url = url.addParameterIfAbsent(Constants.HEARTBEAT_KEY, String.valueOf(Constants.DEFAULT_HEARTBEAT));
    String str = url.getParameter(Constants.SERVER_KEY, Constants.DEFAULT_REMOTING_SERVER);
    // RPC服务的名称，默认是netty

    if (str != null && str.length() > 0 && ! ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(str))
        throw new RpcException("Unsupported server type: " + str + ", url: " + url);

    url = url.addParameter(Constants.CODEC_KEY, Version.isCompatibleVersion() ? COMPATIBLE_CODEC_NAME : DubboCodec.NAME);
    ExchangeServer server;
    try {
        server = Exchangers.bind(url, requestHandler);
        // 绑定操作
    } catch (RemotingException e) {
        throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
    }
    str = url.getParameter(Constants.CLIENT_KEY);
    if (str != null && str.length() > 0) {
        Set<String> supportedTypes = ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions();
        if (!supportedTypes.contains(str)) {
            throw new RpcException("Unsupported client type: " + str);
        }
    }
    return server;
}
```



**Exchangers 类**

```java
public static ExchangeServer bind(URL url, ExchangeHandler handler) throws RemotingException {
    if (url == null) {
        throw new IllegalArgumentException("url == null");
    }
    if (handler == null) {
        throw new IllegalArgumentException("handler == null");
    }
    url = url.addParameterIfAbsent(Constants.CODEC_KEY, "exchange");
    return getExchanger(url).bind(url, handler);
    // getExchanger 是通过SPI返回一个HeaderExchanger对象
}
```



**HeaderExchanger 类**

```java
public ExchangeServer bind(URL url, ExchangeHandler handler) throws RemotingException {
    // 包装一个HeaderExchangeHandler对象
    // 再包装一个DecodeHandler对象
    // Transporters.bind绑定操做
    // 最后包装成HeaderExchangeServer返回
    return new HeaderExchangeServer(
        Transporters.bind(url, 
            new DecodeHandler(
                new HeaderExchangeHandler(handler)
            )
        )
    );
}
```



其中Transporters.bind会先获取当前可用的其中Transporters，默认也是NettyTransporter对象，调用其bind方法

new NettyServer(url, listener)，来到了AbstractServer类

```java
public AbstractServer(URL url, ChannelHandler handler) throws RemotingException {
    super(url, handler);
    localAddress = getUrl().toInetSocketAddress();
    // 获取本地套接字地址，也就是IP端口
    String host = url.getParameter(Constants.ANYHOST_KEY, false) 
                    || NetUtils.isInvalidLocalHost(getUrl().getHost()) 
                    ? NetUtils.ANYHOST : getUrl().getHost();
    bindAddress = new InetSocketAddress(host, getUrl().getPort());
    // 绑定地址，如果是本地项目，host为"0.0.0.0"
    this.accepts = url.getParameter(Constants.ACCEPTS_KEY, Constants.DEFAULT_ACCEPTS);
    this.idleTimeout = url.getParameter(Constants.IDLE_TIMEOUT_KEY, Constants.DEFAULT_IDLE_TIMEOUT);
    try {
        doOpen();
        // 连接打开，当前默认是使用的NettyServer里的方法
        // 关于Netty的操作细节目前也不是很理解，后续补充
        if (logger.isInfoEnabled()) {
            logger.info("Start " + getClass().getSimpleName() + " bind " + getBindAddress() + ", export " + getLocalAddress());
        }
    } catch (Throwable t) {
        throw new RemotingException(url.toInetSocketAddress(), null, "Failed to bind " + getClass().getSimpleName() 
                                    + " on " + getLocalAddress() + ", cause: " + t.getMessage(), t);
    }
    if (handler instanceof WrappedChannelHandler ){
        executor = ((WrappedChannelHandler)handler).getExecutor();
    }
}
```

其最终返回一个NettyService,服务端已经开启了，客户端可以连接了，继续跳入到HeaderExchangeServer中

#### 开启心跳检测

**HeaderExchangeServer 类**

```java
public HeaderExchangeServer(Server server) {
    // 此处传递的service就是上面生成的NettyService
    if (server == null) {
        throw new IllegalArgumentException("server == null");
    }
    this.server = server;
    // 开始心跳检测
    this.heartbeat = server.getUrl().getParameter(Constants.HEARTBEAT_KEY, 0);
    // 默认是0（可是没找到在哪设置为了6000，也就是6s）
    this.heartbeatTimeout = server.getUrl().getParameter(Constants.HEARTBEAT_TIMEOUT_KEY, heartbeat * 3);
    // 定时任务间隔期 6*3 = 18s
    // 如果没有设置心跳检测的间隔期，则设置为心跳延迟时间的3倍
    if (heartbeatTimeout < heartbeat * 2) {
        // 设置的时间不符合要求
        throw new IllegalStateException("heartbeatTimeout < heartbeatInterval * 2");
    }
    startHeatbeatTimer();
    // 开启心跳检测
}
```

心跳检测最后真正执行的任务是如下代码

 

**HeartBeatTask 类**

```java
public void run() {
    try {
        long now = System.currentTimeMillis();
        // 具体所有IO的channel是通过HeaderExchangeServer.this.getChannels()获取到的
        for ( Channel channel : channelProvider.getChannels() ) {
            if (channel.isClosed()) {
                // 确保可用的channel
                continue;
            }
            try {
                Long lastRead = ( Long ) channel.getAttribute(
                        HeaderExchangeHandler.KEY_READ_TIMESTAMP );
                Long lastWrite = ( Long ) channel.getAttribute(
                        HeaderExchangeHandler.KEY_WRITE_TIMESTAMP );
                if ( ( lastRead != null && now - lastRead > heartbeat )
                        || ( lastWrite != null && now - lastWrite > heartbeat ) ) {
                    Request req = new Request();
                    req.setVersion( "2.0.0" );
                    req.setTwoWay( true );
                    req.setEvent( Request.HEARTBEAT_EVENT );
                    channel.send( req );
                    // 发生心跳检测包 req是内容
                    if ( logger.isDebugEnabled() ) {
                        logger.debug( "Send heartbeat to remote channel " + channel.getRemoteAddress()
                                              + ", cause: The channel has no data-transmission exceeds a heartbeat period: " + heartbeat + "ms" );
                    }
                }
                if ( lastRead != null && now - lastRead > heartbeatTimeout ) {
                    // channel因为超时可能存在关闭的情况
                    logger.warn( "Close channel " + channel
                                         + ", because heartbeat read idle time out: " + heartbeatTimeout + "ms" );
                    if (channel instanceof Client) {
                        try {
                            ((Client)channel).reconnect();
                        }catch (Exception e) {
                            //do nothing
                        }
                    } else {
                        channel.close();
                    }
                }
            } catch ( Throwable t ) {
                logger.warn( "Exception when heartbeat to remote channel " + channel.getRemoteAddress(), t );
            }
        }
    } catch ( Throwable t ) {
        logger.warn( "Unhandled exception when heartbeat, cause: " + t.getMessage(), t );
    }
}
```

到此整个的服务暴露就全部结束了

 

## 总结

### Spring

基于spring开发，做到无缝对接spring，在使用上只有极少数的xml配置学习成本，在Dubbo内部继承了若干个类，自定义实现的xsd和配套的解析方法等。**如果以后需要接入自定义的NameSpaceContext，同样需要类似的处理**

- 通过BeanNameAware修改bean的名称

- ApplicationContextAware去获取Spring IOC的容器

- IntializingBean的afterPropertiesSet去自定义实现bean的实例对象

- ApplicationListener的onApplicationEvent接收各种事件

- DisposableBean的destroy去销毁bean

获取Spring IOC容器之后，在自定义实现dubbo的服务bean时，就可以获取到系统配置的注册中心、使用的协议、均衡负责等内容（对注册中心、协议等对Spring而言只是个普通的bean而言），这样就可以完成对ServiceBean的属性输入。

### SPI

虽然也介绍过SPI的整个的处理细节，在整个的dubbo中也确实大量使用中。可是真正的在调试服务暴露中还是有不少细节被忽略掉了。

- 基本上所有的类都有通过拼接代码字符串再编译生成其对象

- 动态生成的对象其实并没有做很多事情，只是根据配置的xml参数，动态决定选取合适的实现类，再调用其方法

- 参数注入和spring的参数注入方法不一样的是，dubbo是获取类的所有set开头同时参数只有1个的公共方法，通过SPI取到参数的值，然后反射invoke注入的

- Dubbo为了一些附加的功能，存在包装类的情况，每一个接口都有wrapper的存在，如果存在则最后返回的对象不是对象本身，而是经过层层包装产生的对象，例如Protocol

### 服务暴露流程

如果是我们去完成这个任务会怎么去做呢？有两点肯定是要实现的

- 注册中心，需要把服务提供方的信息推送给注册中心统一管理，服务调用方能够感知到存在这个服务

- 对外的网络端口，可通过套接字和外界（服务调用方）发生信息交换

服务调用方感知到服务的存在，然后获取其服务的套接字信息，然后直接通过网络IO完成信息交换

Dubbo其实也基本上按照这种思路来实现的，只是支持了多种协议，并且还有监控、统计等功能，层次分明。

#### 获取注册中心

注册中心的基本配置就是在xml配置的**dubbo:registry**,生成的URL也是注册协议的URL，例如，registry://127.0.0.1:2182/com.alibaba.dubbo.registry.RegistryService?application=dubbo-demo&client=zkclient&dubbo=2.5.3&group=dubbo-demo&owner=jwfy&pid=2772&registry=zookeeper&timestamp=1525276569763 会把一个dubbo的xml配置的基本信息给提取出去

 

#### 服务信息的组装

既然注册中心已经准备好了，那现在就需要处理我们对外暴露的service信息，对外暴露服务也需要配置的，这个配置就是从**dubbo:protocol**来的。

这个配置会告诉系统，所有对外暴露的IO读取协议是什么，默认的是dubbo，其实也支持redis、http等

**是处理IO数据流方式的协议**

不过，如果没有IP、端口等数据，则从**dubbo:provider**提取

再配置其他属性数据，生成的URL，例如dubbo://172.16.109.110:20880/com.jwfy.dubbo.product.ProductService?anyhost=true&application=dubbo-demo&default.loadbalance=random&dubbo=2.5.3&interface=com.jwfy.dubbo.product.ProductService&methods=print,getStr&owner=jwfy &pid=2772&side=provider&timestamp=1525313423899

这个对外的服务使用的暴露的接口、具体可以被调用的函数 属于提供方

 

#### 服务IO端口提供

既然网络IO端的协议数据已经准备好了，那就可以开启，等待服务方的调用

这点可以直接跳到DubboProtocol类的export操作了（以Dubbo协议为例子）

针对每一个URL都提取出一个key，然后存储对应的invoker、url等信息

真正操作中取的是服务名+协议端口为key，DubboExporter为value的一个map，后续就可以通过key反取出invoke对象

接下来就是使用哪种Transporter操作了，默认是使用了netty的NettyTransporter（netty是一种基于java开发的异步的、事件驱动的高效的网络IO框架），如果需要使用其他的IO框架，则需要在**dubbo:provider**设置service字段

现在网络对外的协议Netty已经准备好了，服务信息也已经准备好了，那接下来就按照特定的格式decode操作，把数据转变为字节流，由netty向外开放出来即可

由此服务调用方就可以通过网络IO 链接到服务提供方上

 

#### 服务的注册

服务的注册是另一个操作，也就是把当前提供的服务告诉给注册中心，后续服务调用方就可以订阅注册中心，从而知道有哪些服务了，当前例子是使用了zookeeper作为注册中心

现在我们知道的是对外暴露服务的url信息（也就是invoke对象中的url）

所以操作也就很明显了，**先获取到注册中心的配置信息，然后把服务url信息转为zk协议的url信息，最后连接到注册中心，注册保存即可**

这里面就有使用zookeeper的jar包功能，需要和注册中心交互的操作

此外还有些附属的功能，例如超时重试、连接信息的保存和读取（如果注册中心突然出现问题，服务端和客户端还可以利用本地文件缓存继续工作，只是不能再实时获取最新的订阅信息罢了）

一般本地的连接信息存储在/Users/XXX/.dubbo中，如果出现了无法注册无法调用的情况，可以考虑删除该文件重启服务


<!-- @include: @article-footer.snippet.md -->     
