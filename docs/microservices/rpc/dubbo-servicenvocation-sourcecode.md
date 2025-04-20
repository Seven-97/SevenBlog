---
title: dubbo - 服务调用源码
category: 微服务
tag:
 - dubbo
 - RPC
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,dubbo,dubbo服务调用源码,实现原理 
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---
>本文图片全挂了，待更新
## 调用过程

dubbo的服务调用方是在xml配置了类似于 <dubbo:reference interface="com.jwfy.dubbo.product.ProductService" id="productService" />的配置，意味着后续在spring中通过 getBean('productService') 就可以获取到远程代理对象。dubbo:reference 本身映射成为的bean是ReferenceBean，其会存储整个dubbo需要的各种信息，例如控制中心的注册地址，服务端的具体IO和端口等。

![image.png](file:///C:/Users/HUAWEI%20MateBook%20Xpro/AppData/Local/Packages/oice_16_974fa576_32c1d314_38f6/AC/Temp/msohtmlclip1/01/clip_image002.jpg)

如上图就是ReferenceBean的类图，根据以往对spring的学习了解，有如下总结：

- 很清楚的认识到其是一个工厂Bean,后续需要getObject方法得到真正的对象（其实在这里不看源码，我们就应该能猜到常规做法是通过动态代理生成interface="com.jwfy.dubbo.product.ProductService"中接口对应的proxy对象），如果想获取ReferenceBean对象本身，则需要使用getBean("&productService")

- 通过InitializingBean的afterPropertiesSet 方法去为当前的bean注入注册中心、均衡负责的方式、使用的协议等属性数据。

 

在这里生成代理对象是通过Java的动态代理方式，因为指明的是接口，（**spring中默认的是如果通过接口生成代理对象，是使用JDK动态代理，否则是使用cglib**），那么根据对动态代理知识点的了解，InvocationHandler肯定是跑不掉的，通过invoke去调用执行函数的。

 



## 生成ReferenceBean的代理对象

在getObject方法为入口打断点，最后可以追踪到ReferenceConfig类的createProxy方法为真正的生成代理对象的操作。

```java
private T createProxy(Map<String, String> map) {
    URL tmpUrl = new URL("temp", "localhost", 0, map);
    // 这里的临时url可以是 temp://localhost?application=dubbo-consume&default.check=false
    //&dubbo=2.5.3&interface=com.jwfy.dubbo.product.ProductService&methods=print,getStr&owner=jwfy&pid=15813&side=consumer&timestamp=1525921242794
    final boolean isJvmRefer;
    if (isInjvm() == null) {
        if (url != null && url.length() > 0) { 
            // 指定URL的情况下，不做本地引用
            isJvmRefer = false;
        } else if (InjvmProtocol.getInjvmProtocol().isInjvmRefer(tmpUrl)) {
            // 默认情况下如果本地有服务暴露，则引用本地服务.
            isJvmRefer = true;
        } else {
            isJvmRefer = false;
        }
    } else {
        isJvmRefer = isInjvm().booleanValue();
    }
    
    if (isJvmRefer) {
          // 如果是本地的服务
        URL url = new URL(Constants.LOCAL_PROTOCOL, NetUtils.LOCALHOST, 0, interfaceClass.getName()).addParameters(map);
        invoker = refprotocol.refer(interfaceClass, url);
        if (logger.isInfoEnabled()) {
            logger.info("Using injvm service " + interfaceClass.getName());
        }
    } else {
        if (url != null && url.length() > 0) { 
        // 用户指定URL，指定的URL可能是对点对直连地址，也可能是注册中心URL
        // 用户可以在dubbo:reference 中直接配置url参数，配置了就直接连接
            String[] us = Constants.SEMICOLON_SPLIT_PATTERN.split(url);
            // 说白了就是通过  ； 去切割字符串
            if (us != null && us.length > 0) {
                for (String u : us) {
                    URL url = URL.valueOf(u);
                    if (url.getPath() == null || url.getPath().length() == 0) {
                        url = url.setPath(interfaceName);
                    }
                    if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
                        urls.add(url.addParameterAndEncoded(Constants.REFER_KEY, StringUtils.toQueryString(map)));
                    } else {
                        urls.add(ClusterUtils.mergeUrl(url, map));
                    }
                }
            }
        } else { // 只能是通过注册中心配置拼装URL
            List<URL> us = loadRegistries(false);
            // 这个false的值含义是非服务提供方，获取到连接注册中心的配置
            // 切记！！！不是获取服务提供方的url属性，而是注册中心的配置
            // 生成的url是类似于registry://127.0.0.1:2182/com.alibaba.dubbo.registry.RegistryService?
            // application=dubbo-consume&client=zkclient&dubbo=2.5.3&group=dubbo-demo&owner=jwfy&pid=1527&registry=zookeeper&timestamp=1525943117155
            if (us != null && us.size() > 0) {
                for (URL u : us) {
                    URL monitorUrl = loadMonitor(u);
                    if (monitorUrl != null) {
                        // 如果存在监控中心，则设置监控属性
                        map.put(Constants.MONITOR_KEY, URL.encode(monitorUrl.toFullString()));
                    }
                    urls.add(u.addParameterAndEncoded(Constants.REFER_KEY, StringUtils.toQueryString(map)));
                }
            }
            if (urls == null || urls.size() == 0) {
                 // 没有找到一个可用的连接到注册中心的数据
                throw new IllegalStateException("No such any registry to reference " + interfaceName  + " on the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", please config <dubbo:registry address=\"...\" /> to your spring config.");
            }
        }
        
        // 在这里interfaceClass说的是 ==> com.jwfy.dubbo.product.ProductService
        // urls还是register协议
        
        // 总之下面这段代码是非常关键的，后面再补充

        if (urls.size() == 1) {
            invoker = refprotocol.refer(interfaceClass, urls.get(0));
            // refprotocol就是包装了两次的RegistryProtocol，直接生成对应的invoker
        } else {
            List<Invoker<?>> invokers = new ArrayList<Invoker<?>>();
            URL registryURL = null;
            for (URL url : urls) {
                // 在这里能够猜到其中一定有向注册中心订阅获取所有的服务提供方的信息
                // 并把获取到的信息返回生成一个invoker对象
                invokers.add(refprotocol.refer(interfaceClass, url));
                if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
                    registryURL = url; // 用了最后一个registry url
                }
            }
            if (registryURL != null) { // 有 注册中心协议的URL
                // 对有注册中心的Cluster 只用 AvailableCluster
                URL u = registryURL.addParameter(Constants.CLUSTER_KEY, AvailableCluster.NAME); 
                invoker = cluster.join(new StaticDirectory(u, invokers));
            }  else { // 不是 注册中心的URL
                invoker = cluster.join(new StaticDirectory(invokers));
            }
        }
    }

    Boolean c = check;
    if (c == null && consumer != null) {
        c = consumer.isCheck();
    }
    if (c == null) {
        c = true; // default true
    }
    if (c && ! invoker.isAvailable()) {
        throw new IllegalStateException("Failed to check the status of the service " + interfaceName + ". No provider available for the service " + (group == null ? "" : group + "/") + interfaceName + (version == null ? "" : ":" + version) + " from the url " + invoker.getUrl() + " to the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion());
    }
    if (logger.isInfoEnabled()) {
        logger.info("Refer dubbo service " + interfaceClass.getName() + " from url " + invoker.getUrl());
    }
    // 创建服务代理
    // 上面已经说了proxyFactory是StubProxyFactoryWrapper包装了JavassistProxyFactory类
    return (T) proxyFactory.getProxy(invoker);
}
```



**JavassistProxyFactory 类**

```java
public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
    return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
}
```



 

## 生成Invoker

Invoker是对可执行对象的引用，需要明确可引用的具体位置（服务端的明确IP和端口等信息）

现在已经拿到了当前服务的注册中心的配置，那么接下来就需要连接到注册中心，并获取到可以调用的机器情况（现实开发中，分布式系统基本上都存在多个机器信息），并组合成为需要的invoker。

下面这个代码段就是生产具体的Invoke操作，接下来好好分析一下

```java
invoker = refprotocol.refer(interfaceClass, urls.get(0));
// refprotocol就是包装了两次的RegistryProtocol,直接生成对于的invoker
// url是registry开头的协议，在参数中包含了消息等协议和其参数
// interfaceClass是接口类
```



 

在分析源码之前，如果换成我们，我们会完成什么操作呢？

- 向注册中心订阅消费者，这样通过dubbo-admin就可以观察现有的生产者和消费者

- 从注册中心 获取 生产者的信息

- 类似均衡负责的操作，选择合适的生产者

- 直连生产者，获取结果

 

**RegistryProtocol 类**

```java
public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
   url = url.setProtocol(url.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_REGISTRY)).removeParameter(Constants.REGISTRY_KEY);
   // 替换协议，一般情况下都是替换成为zookeeper
   Registry registry = registryFactory.getRegistry(url);
   // 这一步是获取到注册中心的配置信息，这里和服务暴露的操作一致
   // 其中的key是zookeeper://127.0.0.1:2182/dubbo-demo/com.alibaba.dubbo.registry.RegistryService
   // 那么同一个jvm内的key其实都是一致的，所以其连接到控制中心的数据也一致
   // 如果没有连接，则需要创建一个新的链接实体，其中会把当前的信息存储到文件中
   // 也会有对应的监听者等，然后真正的调用连接zk操作，确保zk是真实存在的
   if (RegistryService.class.equals(type)) {
    // 如果类型是RegistryService，则使用getInvoke操作直接拼接生成一个invoke对象
    // 这个操作和服务提供方生成invoker的方式一致
       return proxyFactory.getInvoker((T) registry, type, url);
   }

   // group="a,b" or group="*"
   Map<String, String> qs = StringUtils.parseQueryString(url.getParameterAndDecoded(Constants.REFER_KEY));
   // 这个qs包含了需要生成invoker的所有参数，例如接口名称，函数名，所述范畴（消费者）等信息
   String group = qs.get(Constants.GROUP_KEY);
   // 查看分组信息
   if (group != null && group.length() > 0 ) {
       if ( ( Constants.COMMA_SPLIT_PATTERN.split( group ) ).length > 1
               || "*".equals( group ) ) {
           return doRefer( getMergeableCluster(), registry, type, url );
       }
   }
   
   // 看有分组和没分组，其实就是集群cluster不一样
   return doRefer(cluster, registry, type, url);
}

private Cluster getMergeableCluster() {
   return ExtensionLoader.getExtensionLoader(Cluster.class).getExtension("mergeable");
   // 生成MergeableCluster 集群
   // 如下代码Cluster$Adpative类则就是cluster，默认的是为FailoverCluster集群
}
```



 

**Cluster$Adpative 类**

```java
package com.alibaba.dubbo.rpc.cluster;

import com.alibaba.dubbo.common.extension.ExtensionLoader;

public class Cluster$Adpative implements com.alibaba.dubbo.rpc.cluster.Cluster {
   public com.alibaba.dubbo.rpc.Invoker join(
       com.alibaba.dubbo.rpc.cluster.Directory arg0)
       throws com.alibaba.dubbo.rpc.cluster.Directory {
       if (arg0 == null) {
           throw new IllegalArgumentException("com.alibaba.dubbo.rpc.cluster.Directory argument == null");
       }

       if (arg0.getUrl() == null) {
           throw new IllegalArgumentException("com.alibaba.dubbo.rpc.cluster.Directory argument getUrl() == null");
   }

       com.alibaba.dubbo.common.URL url = arg0.getUrl();
       String extName = url.getParameter("cluster", "failover");

       if (extName == null) {
           throw new IllegalStateException(
               "Fail to get extension(com.alibaba.dubbo.rpc.cluster.Cluster) name from url(" +
               url.toString() + ") use keys([cluster])");
       }

       com.alibaba.dubbo.rpc.cluster.Cluster extension = (com.alibaba.dubbo.rpc.cluster.Cluster) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.cluster.Cluster.class)
                                                                                                                .getExtension(extName);

       return extension.join(arg0);
   }
}
```



### doRefer

经过上面的操作，现在来到了doRefer函数操作，其结果返回的就是invoker对象

```java
private <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url) {
    // cluster 集群，目前可能为FailoverCluster，如果在有分组的情况下则是MergeableCluster
    // registry 注册中心信息
    // type 就是上面说的
    RegistryDirectory<T> directory = new RegistryDirectory<T>(type, url);
    directory.setRegistry(registry);
    directory.setProtocol(protocol);
    URL subscribeUrl = new URL(Constants.CONSUMER_PROTOCOL, NetUtils.getLocalHost(), 0, type.getName(), directory.getUrl().getParameters());
    if (! Constants.ANY_VALUE.equals(url.getServiceInterface())
            && url.getParameter(Constants.REGISTER_KEY, true)) {
            // 接口名字有意义而且是注册协议
            // 把当前url信息当做消费者节点注册到注册中心中区
        registry.register(subscribeUrl.addParameters(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY,
                Constants.CHECK_KEY, String.valueOf(false)));
    }
    directory.subscribe(subscribeUrl.addParameter(Constants.CATEGORY_KEY, 
            Constants.PROVIDERS_CATEGORY 
            + "," + Constants.CONFIGURATORS_CATEGORY 
            + "," + Constants.ROUTERS_CATEGORY));
        
    // 订阅服务，并生成invoker对象
    return cluster.join(directory);
}
```

在上面一笔带过了如何注册、订阅、生成invoke的，接下来依次拆分各个细节

### 注册到注册中心

```java
registry.register(subscribeUrl.addParameters(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY, Constants.CHECK_KEY, String.valueOf(false)));
```

- registry是ZookeeperRegistry对象

- subscribeUrl是consumer://192.168.10.123/com.jwfy.dubbo.product.ProductService?application=dubbo-consume&default.check=false&dubbo=2.5.3&interface=com.jwfy.dubbo.product.ProductService&methods=print,getStr&owner=jwfy&pid=1196&side=consumer&timestamp=1526204222984

> 表示是一个消费者

 

进入到FailbackRegistry类

```java
public void register(URL url) {
    super.register(url);
    // 会把当前的url添加到registered集合中，表示该url注册了
    failedRegistered.remove(url);
    failedUnregistered.remove(url);
    // 既然都要注册了，肯定从失败的集合和取消注册的集合中移除掉
    try {
        // 向服务器端发送注册请求，也是真正的开始注册操作
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
        failedRegistered.add(url);
    }
}

// 关于failedRegistered的重试操作，在构造函数中有
// 开启定时任务运行的操作，默认时间是5秒执行一次
    this.retryFuture = retryExecutor.scheduleWithFixedDelay(new Runnable() {
        public void run() {
            // 检测并连接注册中心
            try {
                retry();
                // 里面肯定有遍历failedRegistered集合的doRegister操作（事实上确实有）
            } catch (Throwable t) { // 防御性容错
                // 个人觉得这个代码写的很好，因为突出一个点，什么时候抛出异常，什么时候处理异常
                // 这点问题上，自己曾经踩了很多坑，也发现很多人也有这个毛病
                logger.error("Unexpected error occur at failed retry, cause: " + t.getMessage(), t);
            }
        }
    }, retryPeriod, retryPeriod, TimeUnit.MILLISECONDS);
```

在doRegister操作中，是利用zk的API存储如下的path

/dubbo-jwfy/com.jwfy.dubbo.product.ProductService/consumers/consumer%3A%2F%2F192.168.10.123%2Fcom.jwfy.dubbo.product.ProductService%3Fapplication%3Ddubbo-consume%26category%3Dconsumers%26check%3Dfalse%26default.check%3Dfalse%26dubbo%3D2.5.3%26interface%3Dcom.jwfy.dubbo.product.ProductService%26methods%3Dprint%2CgetStr%26owner%3Djwfy%26pid%3D1196%26side%3Dconsumer%26timestamp%3D1526204222984

如下图，在调用doRegister前后zk注册中心节点的情况，很明显已经注册成功



### 订阅服务

服务订阅说通俗些就是获取zk中需要的节点信息，本例中是**获取生产者的连接信息**

```java
directory.subscribe(subscribeUrl.addParameter(Constants.CATEGORY_KEY, 
            Constants.PROVIDERS_CATEGORY 
            + "," + Constants.CONFIGURATORS_CATEGORY 
            + "," + Constants.ROUTERS_CATEGORY));
// 这个url 添加了生产者、配置、路由三个节点的配置参数
```

- url是consumer://192.168.10.123/com.jwfy.dubbo.product.ProductService?application=dubbo-consume&category=providers,configurators,routers&default.check=false&dubbo=2.5.3&interface=com.jwfy.dubbo.product.ProductService&methods=print,getStr&owner=jwfy&pid=1196&side=consumer&timestamp=1526204222984

- directory 是RegistryDirectory，**其中参数registry就是上面的注册中心ZookeeperRegistry的配置**

 

**FailbackRegistry 类**

```java
public void subscribe(URL url, NotifyListener listener) {
    super.subscribe(url, listener);
    // 会设置registry的subscribed信息，其中subscribed是一个Map<Url,Set<NotifyListener>>的容器
    removeFailedSubscribed(url, listener);
    try {
        // 向服务器端发送订阅请求，真正干活的来了
        doSubscribe(url, listener);
    } catch (Exception e) {
        Throwable t = e;

        List<URL> urls = getCacheUrls(url);
        if (urls != null && urls.size() > 0) {
            notify(url, listener, urls);
            logger.error("Failed to subscribe " + url + ", Using cached list: " + urls + " from cache file: " + getUrl().getParameter(Constants.FILE_KEY, System.getProperty("user.home") + "/dubbo-registry-" + url.getHost() + ".cache") + ", cause: " + t.getMessage(), t);
        } else {
            // 如果开启了启动时检测，则直接抛出异常
            boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                    && url.getParameter(Constants.CHECK_KEY, true);
            boolean skipFailback = t instanceof SkipFailbackWrapperException;
            if (check || skipFailback) {
                if(skipFailback) {
                    t = t.getCause();
                }
                throw new IllegalStateException("Failed to subscribe " + url + ", cause: " + t.getMessage(), t);
            } else {
                logger.error("Failed to subscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
            }
        }

        // 将失败的订阅请求记录到失败列表，定时重试
        // 和上面注册的套路一致
        addFailedSubscribed(url, listener);
    }
}
```



来到了doSubscribe方法,在这里我们将会了解到如何从注册中心获取到生产者的连接信息的

```java
protected void doSubscribe(final URL url, final NotifyListener listener) {
    try {
        if (Constants.ANY_VALUE.equals(url.getServiceInterface())) {
            // Constants.ANY_VALUE是*
            // url.getServiceInterface 是 com.jwfy.dubbo.product.ProductService
            String root = toRootPath();
            ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
            if (listeners == null) {
                zkListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, ChildListener>());
                listeners = zkListeners.get(url);
            }
            ChildListener zkListener = listeners.get(listener);
            if (zkListener == null) {
                listeners.putIfAbsent(listener, new ChildListener() {
                    public void childChanged(String parentPath, List<String> currentChilds) {
                        for (String child : currentChilds) {
                            if (! anyServices.contains(child)) {
                                anyServices.add(child);
                                subscribe(url.setPath(child).addParameters(Constants.INTERFACE_KEY, child, 
                                        Constants.CHECK_KEY, String.valueOf(false)), listener);
                            }
                        }
                    }
                });
                zkListener = listeners.get(listener);
            }
            zkClient.create(root, false);
            List<String> services = zkClient.addChildListener(root, zkListener);
            if (services != null && services.size() > 0) {
                anyServices.addAll(services);
                for (String service : services) {
                    subscribe(url.setPath(service).addParameters(Constants.INTERFACE_KEY, service, 
                            Constants.CHECK_KEY, String.valueOf(false)), listener);
                }
            }
        } else {
            List<URL> urls = new ArrayList<URL>();
            for (String path : toCategoriesPath(url)) {
                // 这个就是上面说的三个目录节点，生产者，配置，路由 
                ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
                // zkListeners 是 Map<URL, Map<NotifyListener, ChildListener>>
                // 管理url与其对应的监听者和zk监听者的映射关系
                if (listeners == null) {
                    zkListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, ChildListener>());
                    listeners = zkListeners.get(url);
                }
                ChildListener zkListener = listeners.get(listener);
                // 从集合中获取到zk的监听者
                if (zkListener == null) {
                    // 如果没有，则需要手动创建一个新的，其中包含了自定义实现的更新子节点的函数操作
                    // 里面会调用notify订阅方法（这个方法很重要，由zkClient主动调用childChanged方法）
                    listeners.putIfAbsent(listener, new ChildListener() {
                        public void childChanged(String parentPath, List<String> currentChilds) {
                            ZookeeperRegistry.this.notify(url, listener, toUrlsWithEmpty(url, parentPath, currentChilds));
                        }
                    });
                    zkListener = listeners.get(listener);
                }
                zkClient.create(path, false);
                // 创建永久节点，此时的path可能为/dubbo-jwfy/com.jwfy.dubbo.product.ProductService/providers
                List<String> children = zkClient.addChildListener(path, zkListener);
                // 重点来了。。。。。这里将会获取到对应生产者的连接信息，具体看下面代码
                if (children != null) {
                    urls.addAll(toUrlsWithEmpty(url, path, children));
                }
            }
            notify(url, listener, urls);
        }
    } catch (Throwable e) {
        throw new RpcException("Failed to subscribe " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
    }
}
```



**AbstractZookeeperClient 类**

```java
public List<String> addChildListener(String path, final ChildListener listener) {
    ConcurrentMap<ChildListener, TargetChildListener> listeners = childListeners.get(path);
    // 从zkClient本身的childListeners获取path的监听者信息
    if (listeners == null) {
        childListeners.putIfAbsent(path, new ConcurrentHashMap<ChildListener, TargetChildListener>());
        listeners = childListeners.get(path);
    }
    TargetChildListener targetListener = listeners.get(listener);
    // 再获取目标节点信息（这个就包含了生产者的信息了）
    if (targetListener == null) {
        listeners.putIfAbsent(listener, createTargetChildListener(path, listener));
        targetListener = listeners.get(listener);
    }
    return addTargetChildListener(path, targetListener);
}


public List<String> addTargetChildListener(String path, final IZkChildListener listener) {
    return client.subscribeChildChanges(path, listener);
    // 这一步就会深入到zkClientjar包内进行最后的_zk.getChildren操作
    // 返回的List<String> 就是对应的path路径的内容
    
    // 此时path 是 /dubbo-jwfy/com.jwfy.dubbo.product.ProductService/providers
    // 返回的内容 是 dubbo%3A%2F%2F192.168.10.123%3A20880%2Fcom.jwfy.dubbo.product.ProductService
    // %3Fanyhost%3Dtrue%26application%3Ddubbo-demo%26default.loadbalance%3Drandom%26
    // dubbo%3D2.5.3%26interface%3Dcom.jwfy.dubbo.product.ProductService%26
    // methods%3Dprint%2CgetStr%26owner%3Djwfy%26pid%3D1081%26side%3Dprovider%26
    // timestamp%3D1526198287386%26token%3Dfdfdf
}
```

现在完成了和注册中心的操作了，通过path顺利拿到生产者的信息，如果仔细观察上述的参数信息，会发现pid是1081，再看看jps显示的进程号,如下图，恰好说明获取到的生产者信息是对的

![](C:\Users\HUAWEI MateBook Xpro\AppData\Roaming\Typora\typora-user-images\image-20240430192134320.png)

 接着来到toUrlsWithEmpty函数，如果有仔细观察这个方法会发现，参数列表都改成了(URL consumer, String path, List&lt;String> providers)这已经很明确的告诉我们，第一个参数是消费者的url，第二个是当前zk的path信息，第三个是获取到的生产者列表信息（为啥是列表呢？因为生产者可以是多个，而且存在多个的情况下，后续均衡负责还需要选择一个可用的生产者进行网络信息交互操作）

当然toUrlsWithEmpty函数主要是进行生产者和消费者的url信息对比操作，如果没有合适的url则添加一个empty协议的url信息（**后期就是通过这个empty判断是否存在有用的生产者，日常开发中的无效黑白名单的错误就产生在这里**）

### 刷新invoker

紧接着来到了notify方法，如下图的具体各个参数具体值

![](C:\Users\HUAWEI MateBook Xpro\AppData\Roaming\Typora\typora-user-images\image-20240430192153479.png)

 

紧接着来到了**AbstractRegistry类**

```java
protected void notify(URL url, NotifyListener listener, List<URL> urls) {
    // url 是注册到注册到注册中心的url
    // listener是监听器，其实就是RegistryDirectory
    // urls 是 从注册中心获取到到的 服务提供方的url集合
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
           // isMatch(URL consumerUrl, URL providerUrl) 函数的参数
           // 已经非常清楚的说明了url是服务调用方的url，u是服务提供方的url
           // 也和我们上面的描述是一致的
           // 对两个url的接口、类目、enable、分组、版本号、classifier等内容进行匹配
           // 如果匹配合适，就认为为true
           // 没有针对协议进行匹配操作
            String category = u.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
            List<URL> categoryList = result.get(category);
            if (categoryList == null) {
                categoryList = new ArrayList<URL>();
                result.put(category, categoryList);
            }
            categoryList.add(u);
        }
    }
    // 按照类目进行分组成为一个map
    
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
        // 替换notified.get(url)的map信息
        saveProperties(url);
        // 初次看，这个url就是上面的服务调用方的url信息，没必要每次都保存吧
        // 进入到这个函数可以发现，他会每次又调用notified.get(url)去获取最新的map数据，默认为异步保存数据
        listener.notify(categoryList);
        // 接着来到了RegistryDirectory的notify方法
    }
}
```



**RegistryDirectory 类**

```java
public synchronized void notify(List<URL> urls) {
    List<URL> invokerUrls = new ArrayList<URL>();
    List<URL> routerUrls = new ArrayList<URL>();
    List<URL> configuratorUrls = new ArrayList<URL>();
    for (URL url : urls) {
        String protocol = url.getProtocol();
        String category = url.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
        if (Constants.ROUTERS_CATEGORY.equals(category) 
                || Constants.ROUTE_PROTOCOL.equals(protocol)) {
            routerUrls.add(url);
        } else if (Constants.CONFIGURATORS_CATEGORY.equals(category) 
                || Constants.OVERRIDE_PROTOCOL.equals(protocol)) {
            configuratorUrls.add(url);
        } else if (Constants.PROVIDERS_CATEGORY.equals(category)) {
            invokerUrls.add(url);
        } else {
            logger.warn("Unsupported category " + category + " in notified url: " + url + " from registry " + getUrl().getAddress() + " to consumer " + NetUtils.getLocalHost());
        }
    }
    // configurators 
    if (configuratorUrls != null && configuratorUrls.size() >0 ){
        this.configurators = toConfigurators(configuratorUrls);
    }
    // routers
    if (routerUrls != null && routerUrls.size() >0 ){
        List<Router> routers = toRouters(routerUrls);
        if(routers != null){ // null - do nothing
            setRouters(routers);
        }
    }
    List<Configurator> localConfigurators = this.configurators; // local reference
    // 合并override参数
    this.overrideDirectoryUrl = directoryUrl;
    if (localConfigurators != null && localConfigurators.size() > 0) {
        for (Configurator configurator : localConfigurators) {
            this.overrideDirectoryUrl = configurator.configure(overrideDirectoryUrl);
        }
    }
    // invokerUrls只能是服务提供方目录去刷新已存的invoke
    refreshInvoker(invokerUrls);
}

private void refreshInvoker(List<URL> invokerUrls){
    if (invokerUrls != null && invokerUrls.size() == 1 && invokerUrls.get(0) != null
            && Constants.EMPTY_PROTOCOL.equals(invokerUrls.get(0).getProtocol())) {
            // empty协议，而且只有1个
            // 需要注意到这个方法只有服务提供方的url才可以被调用
            // 服务提供方只包含了一个empty协议的无效url，设置forbidden=true
            // 这个true就是后面dubbo中经常出现的黑白名单错误情况
        this.forbidden = true; // 禁止访问
        this.methodInvokerMap = null; // 置空列表
        destroyAllInvokers(); // 关闭所有Invoker
    } else {
        this.forbidden = false; // 允许访问
        Map<String, Invoker<T>> oldUrlInvokerMap = this.urlInvokerMap; // local reference
        if (invokerUrls.size() == 0 && this.cachedInvokerUrls != null){
            invokerUrls.addAll(this.cachedInvokerUrls);
        } else {
            this.cachedInvokerUrls = new HashSet<URL>();
            this.cachedInvokerUrls.addAll(invokerUrls);//缓存invokerUrls列表，便于交叉对比
        }
        if (invokerUrls.size() ==0 ){
            return;
        }
        // 后面的操作就是去刷新现存的invoker列表
        
        Map<String, Invoker<T>> newUrlInvokerMap = toInvokers(invokerUrls) ;// 将URL列表转成Invoker列表
        // 这个invoker是InvokerDelegete类，是包装了一层的InvokerWrapper
        Map<String, List<Invoker<T>>> newMethodInvokerMap = toMethodInvokers(newUrlInvokerMap); // 换方法名映射Invoker列表
        // state change
        //如果计算错误，则不进行处理.
        if (newUrlInvokerMap == null || newUrlInvokerMap.size() == 0 ){
            logger.error(new IllegalStateException("urls to invokers error .invokerUrls.size :"+invokerUrls.size() + ", invoker.size :0. urls :"+invokerUrls.toString()));
            return ;
        }
        this.methodInvokerMap = multiGroup ? toMergeMethodInvokerMap(newMethodInvokerMap) : newMethodInvokerMap;
        this.urlInvokerMap = newUrlInvokerMap;
        try{
            destroyUnusedInvokers(oldUrlInvokerMap,newUrlInvokerMap); // 关闭未使用的Invoker
        }catch (Exception e) {
            logger.warn("destroyUnusedInvokers error. ", e);
        }
    }
}
```

这个函数具体的操作包含了

1. 更新ZookeeperRegistry中的notify参数信息（**把生产者、配置、路由等url信息存入其中**）
2. 保存节点信息文档到dubbo的cache文件中
3. 刷新生成invoke的数据信息

到此整个的订阅服务的操作就完成了

 

### 生成Invoker

现在就剩下最关键的一句话cluster.join(directory)，会层层包装，最后形成的invoker如图所示

![](C:\Users\HUAWEI MateBook Xpro\AppData\Roaming\Typora\typora-user-images\image-20240430192319271.png)

在directory中包含了所有的注册信息，在后面的真正的函数调用其实也是通过invoker.invoker去调用执行

 

#### InvokerInvocationHandler 入口

被包装的类通过动态代理反射时，内嵌入的InvocationHandler类，具体方法调用则会进入到invoke方法中

```java
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    Class<?>[] parameterTypes = method.getParameterTypes();
      // 获取方法的名称以及方法的参数信息
    if (method.getDeclaringClass() == Object.class) {
        return method.invoke(invoker, args);
    }
    if ("toString".equals(methodName) && parameterTypes.length == 0) {
        return invoker.toString();
    }
    if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
        return invoker.hashCode();
    }
    if ("equals".equals(methodName) && parameterTypes.length == 1) {
        return invoker.equals(args[0]);
    }
    // 此时invoker是MockClusterInvoker
    // 还拼接生成了一个RpcInvocation
    return invoker.invoke(new RpcInvocation(method, args)).recreate();
}
```

来到了MockClusterInvoker类，此时需要注意到MockClusterInvoker类的invoke是FailoverClusterInvoker

FailoverClusterInvoker类可以进行重试操作，**如果有印象的可以知道在一个reference的xml配置中，可以加上重试次数retries属性字段的值，默认是3次，如果设置了小于0的数字，则为1次，重试次数0位的意思就是只进行一次操作**

 

#### MockClusterInvoker mock入口

```java
public Result invoke(Invocation invocation) throws RpcException {
    Result result = null;
    
    // 注册中心，服务提供方、服务调用方的信息都存储在directory中，后期均衡负责也是处理这里面的数据
    String value = directory.getUrl().getMethodParameter(invocation.getMethodName(), Constants.MOCK_KEY, Boolean.FALSE.toString()).trim(); 
    // 就是查看 `methodName.mock`或者`mock`的属性值，默认是“false”
    if (value.length() == 0 || value.equalsIgnoreCase("false")){
        // 不需要走Mock测试，进入到FailoverClusterInvoker中
        result = this.invoker.invoke(invocation);
    } else if (value.startsWith("force")) {
        if (logger.isWarnEnabled()) {
            logger.info("force-mock: " + invocation.getMethodName() + " force-mock enabled , url : " +  directory.getUrl());
        }
        //force:direct mock
        result = doMockInvoke(invocation, null);
    } else {
        //fail-mock
        try {
            result = this.invoker.invoke(invocation);
        }catch (RpcException e) {
            if (e.isBiz()) {
                throw e;
            } else {
                if (logger.isWarnEnabled()) {
                    logger.info("fail-mock: " + invocation.getMethodName() + " fail-mock enabled , url : " +  directory.getUrl(), e);
                }
                result = doMockInvoke(invocation, e);
            }
        }
    }
    return result;
}
```



#### AbstractClusterInvoker 负载均衡

进入到FailoverClusterInvoker类之前先进入到AbstractClusterInvoker类中

```java
public Result invoke(final Invocation invocation) throws RpcException {
    checkWheatherDestoried();
    LoadBalance loadbalance;
    
    List<Invoker<T>> invokers = list(invocation);
    // 筛选出合适的invokers列表，基于方法和路由信息
    // 其中路由信息则是通过MockInvokersSelector类处理获取到invocation中attachments保存的mock信息去筛选合适的invoker，所以重点是筛选
    // 调试发现，一般情况下在这里面attachments字段并没有数据
    if (invokers != null && invokers.size() > 0) {
        loadbalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(invokers.get(0).getUrl()
                .getMethodParameter(invocation.getMethodName(),Constants.LOADBALANCE_KEY, Constants.DEFAULT_LOADBALANCE));
    } else {
        loadbalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(Constants.DEFAULT_LOADBALANCE);
    }
    // 创建合适的均衡负责类loanbalance信息，一般情况是RandomLoadBalance类
    RpcUtils.attachInvocationIdIfAsync(getUrl(), invocation);
    // 确保幂等，如果是异步则需要往attachment参数中添加自增ID（这个自增ID是AtomicLong类，线程安全）
    // 这里就有往invocation的attachment填充数据的操作
    return doInvoke(invocation, invokers, loadbalance);
    // 现在进入到FailoverClusterInvoker类中了
}
```



#### FailoverClusterInvoker 重试机制

上面已经说了，这个类的主要作用是**重试操作**

```java
public Result doInvoke(Invocation invocation, final List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
      //  第一个是执行的参数信息，包含了函数名等信息
      // 第二个是被调用执行的参数，包含了服务提供方的IP:PORT信息
      // 第三个是均衡负责，在选择调用的服务方时，会根据该对象选择一个合适的服务方
    List<Invoker<T>> copyinvokers = invokers;
    checkInvokers(copyinvokers, invocation);
    // 检测invokers是否存在，如果不存在则提示没有可用的服务提供方被使用，请检查服务提供方是否被注册
    int len = getUrl().getMethodParameter(invocation.getMethodName(), Constants.RETRIES_KEY, Constants.DEFAULT_RETRIES) + 1;
    // 获取重试的次数，如果设置的值<=0,则只有1次操作机会，默认是3次
    if (len <= 0) {
        len = 1;
    }
    // retry loop.
    RpcException le = null; // last exception.
    List<Invoker<T>> invoked = new ArrayList<Invoker<T>>(copyinvokers.size()); // invoked invokers.
    Set<String> providers = new HashSet<String>(len);
    for (int i = 0; i < len; i++) {
        //重试时，进行重新选择，避免重试时invoker列表已发生变化.
        //注意：如果列表发生了变化，那么invoked判断会失效，因为invoker示例已经改变
        if (i > 0) {
            checkWheatherDestoried();
            copyinvokers = list(invocation);
            //重新检查一下
            // 注意一下这个list操作，这个list操作是重新更新可用的invoker列表
            checkInvokers(copyinvokers, invocation);
        }
        Invoker<T> invoker = select(loadbalance, invocation, copyinvokers, invoked);
        // 选择合适的服务提供方的invoker，在AbstractClusterInvoker类中去完成均衡负责的选择操作
        // 关于均衡负责，后面考虑分为一篇笔记去学习几种不同的负载方法，其中还包含了sticky 粘性连接
        invoked.add(invoker);
        RpcContext.getContext().setInvokers((List)invoked);
        try {
            Result result = invoker.invoke(invocation);
            // 这一步才是真正的执行调用远程方法的开始&入口
            if (le != null && logger.isWarnEnabled()) {
                // 存在重试了3次才终于成功的情况，这时候会告警提醒之前存在的错误信息输出
                logger.warn("Although retry the method " + invocation.getMethodName()
                        + " in the service " + getInterface().getName()
                        + " was successful by the provider " + invoker.getUrl().getAddress()
                        + ", but there have been failed providers " + providers 
                        + " (" + providers.size() + "/" + copyinvokers.size()
                        + ") from the registry " + directory.getUrl().getAddress()
                        + " on the consumer " + NetUtils.getLocalHost()
                        + " using the dubbo version " + Version.getVersion() + ". Last error is: "
                        + le.getMessage(), le);
            }
            return result;
        } catch (RpcException e) {
            // 遇到了RPCEXCEPTION 而且是biz类型的，则不重试直接抛出该异常
            if (e.isBiz()) { // biz exception.
                throw e;
            }
            le = e;
        } catch (Throwable e) {
            le = new RpcException(e.getMessage(), e);
        } finally {
            providers.add(invoker.getUrl().getAddress());
        }
    }
    // 重试多次依旧没有正常的结果返回，则抛出该异常
    throw new RpcException(le != null ? le.getCode() : 0, "Failed to invoke the method "
            + invocation.getMethodName() + " in the service " + getInterface().getName() 
            + ". Tried " + len + " times of the providers " + providers 
            + " (" + providers.size() + "/" + copyinvokers.size() 
            + ") from the registry " + directory.getUrl().getAddress()
            + " on the consumer " + NetUtils.getLocalHost() + " using the dubbo version "
            + Version.getVersion() + ". Last error is: "
            + (le != null ? le.getMessage() : ""), le != null && le.getCause() != null ? le.getCause() : le);
}
```



#### DubboInvoker invoke

上面说的Result result = invoker.invoke(invocation);，经过层层转发，来到了FutureFilter类

```java
public Result invoke(final Invoker<?> invoker, final Invocation invocation) throws RpcException {
    final boolean isAsync = RpcUtils.isAsync(invoker.getUrl(), invocation);
    // 看是否为异步的方法，添加有sync字段信息
    // 先从invocation的attachment中查看是否存在async字段，再看看url中的methodName.async ，再看看url的async属性
    
    fireInvokeCallback(invoker, invocation);
    Result result = invoker.invoke(invocation);
    // 进一步invoke操作
    if (isAsync) {
        asyncCallback(invoker, invocation);
    } else {
        syncCallback(invoker, invocation, result);
    }
    return result;
}
```



来到了MonitorFilter过滤器查看是否需要进行监控（通过查看url是否存在monitor字段，如果为true，则是需要监控）

再来到了AbstractInvoker类的invoke方法,本身是DubboInvoker

```java
public Result invoke(Invocation inv) throws RpcException {
    if(destroyed) {
        throw new RpcException("Rpc invoker for service " + this + " on consumer " + NetUtils.getLocalHost() 
                                        + " use dubbo version " + Version.getVersion()
                                        + " is DESTROYED, can not be invoked any more!");
    }
    RpcInvocation invocation = (RpcInvocation) inv;
    invocation.setInvoker(this);
    if (attachment != null && attachment.size() > 0) {
        invocation.addAttachmentsIfAbsent(attachment);
        // 添加attachment信息，调试中发现添加的是interface和token
    }
    Map<String, String> context = RpcContext.getContext().getAttachments();
    // 这个是利用了ThreadLocal持有的数据中获取
    if (context != null) {
      // 这代码写的冗余了，而且为啥不再加个empty的检测呢？
        invocation.addAttachmentsIfAbsent(context);
    }
    if (getUrl().getMethodParameter(invocation.getMethodName(), Constants.ASYNC_KEY, false)){
        invocation.setAttachment(Constants.ASYNC_KEY, Boolean.TRUE.toString());
        // 如果是异步的方法，添加async字段，
    }
    RpcUtils.attachInvocationIdIfAsync(getUrl(), invocation);
    // 如果是异步则添加自增ID
    
    try {
        return doInvoke(invocation);
        // 进入到DubboInvoker执行invoke操作了
    } catch (InvocationTargetException e) { // biz exception
        Throwable te = e.getTargetException();
        if (te == null) {
            return new RpcResult(e);
        } else {
            if (te instanceof RpcException) {
                ((RpcException) te).setCode(RpcException.BIZ_EXCEPTION);
            }
            return new RpcResult(te);
        }
    } catch (RpcException e) {
        if (e.isBiz()) {
            return new RpcResult(e);
        } else {
            throw e;
        }
    } catch (Throwable e) {
        return new RpcResult(e);
    }
}
```



**DubboInvoker 类**

```java
protected Result doInvoke(final Invocation invocation) throws Throwable {
    RpcInvocation inv = (RpcInvocation) invocation;
    final String methodName = RpcUtils.getMethodName(invocation);
    inv.setAttachment(Constants.PATH_KEY, getUrl().getPath());
    inv.setAttachment(Constants.VERSION_KEY, version);
    // 添加路径和版本概念，如果没有添加则是0.0.0 
    
    ExchangeClient currentClient;
    if (clients.length == 1) {
        currentClient = clients[0];
    } else {
        currentClient = clients[index.getAndIncrement() % clients.length];
    }
    // currentClient 是 后续需要连接netty操作的客户端
    try {
        boolean isAsync = RpcUtils.isAsync(getUrl(), invocation);
        // 是否为异步操作。。。。为啥确认个异步操作这么多重复操作
        boolean isOneway = RpcUtils.isOneway(getUrl(), invocation);
        // 是否设置了return=false 这个操作
        int timeout = getUrl().getMethodParameter(methodName, Constants.TIMEOUT_KEY,Constants.DEFAULT_TIMEOUT);
        // 超时设置的时间，默认为1s
        if (isOneway) {
            // 如果强制设置了return=false,异步的future都不需要设置了，也不需要关注超时字段
            boolean isSent = getUrl().getMethodParameter(methodName, Constants.SENT_KEY, false);
            currentClient.send(inv, isSent);
            RpcContext.getContext().setFuture(null);
            return new RpcResult();
        } else if (isAsync) {
            ResponseFuture future = currentClient.request(inv, timeout) ;
            // 调用的是request方法，异步的设置future
            RpcContext.getContext().setFuture(new FutureAdapter<Object>(future));
            return new RpcResult();
        } else {
            RpcContext.getContext().setFuture(null);
            // 同步方法，设置超时时间，等待返回
            // 其实也是异步方法，只是最后调用了get去获取future的结果
            return (Result) currentClient.request(inv, timeout).get();
        }
    } catch (TimeoutException e) {
        throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "Invoke remote method timeout. method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
    } catch (RemotingException e) {
        throw new RpcException(RpcException.NETWORK_EXCEPTION, "Failed to invoke remote method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
    }
}
```



#### NettyChannel netty请求

上述的request以及send方法，都被转发到HeaderExchangeChannel类中，这个类有一个非常关键的字段是channel，是NettyClient类，包含了服务提供方的IP：PORT信息

其实仔细看request方法和send方法最后的实现差不太多，只是request需要检测连接的channel是否存在，而send单独本身是不需要进行这个操作的。

```java
public ResponseFuture request(Object request, int timeout) throws RemotingException {
    if (closed) {
        throw new RemotingException(this.getLocalAddress(), null, "Failed to send request " + request + ", cause: The channel " + this + " is closed!");
    }
    // create request.
    Request req = new Request();
    req.setVersion("2.0.0");
    req.setTwoWay(true);
    req.setData(request);
    // 生成的req有个线程安全的自增的ID，可以通过这个统计出调用的次数
    DefaultFuture future = new DefaultFuture(channel, req, timeout);
    try{
        channel.send(req);
        // 进入到NettyChannel类中
    }catch (RemotingException e) {
        future.cancel();
        throw e;
    }
    return future;
    // 返回future，后续的超时就是通过对future操作
}
```



**NettyChannel 类**

```java
public void send(Object message, boolean sent) throws RemotingException {
    super.send(message, sent);
    
    boolean success = true;
    int timeout = 0;
    try {
        ChannelFuture future = channel.write(message);
        // 这个就是调用的netty的write操作完成数据发送操作
        // 这个就是经过层层嵌套包装向外发送数据的最终操作
        if (sent) {
           // url配置的send字段属性，如果为true
           // 则通过await等待超时的世界去查看请求是否成功
            timeout = getUrl().getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
            success = future.await(timeout);
        }
        Throwable cause = future.getCause();
        if (cause != null) {
            throw cause;
        }
    } catch (Throwable e) {
        throw new RemotingException(this, "Failed to send message " + message + " to " + getRemoteAddress() + ", cause: " + e.getMessage(), e);
        // 抛出远程发送消息失败的错误，打印出发送参数以及远程IP
    }
    
    if(! success) {
        throw new RemotingException(this, "Failed to send message " + message + " to " + getRemoteAddress()
                + "in timeout(" + timeout + "ms) limit");
    }
}
```



#### Future 结果处理 & 超时检测

看看异步拿到结果，判断是否超时等检测操作

**DefaultFuture 类**

```java
public Object get(int timeout) throws RemotingException {
    if (timeout <= 0) {
        timeout = Constants.DEFAULT_TIMEOUT;
    }
    if (! isDone()) {
        // 这个时候还是异步执行的，会立即执行到这里（时间非常的端，相比RPC的几百毫秒而言）
        long start = System.currentTimeMillis();
        lock.lock();
        try {
            while (! isDone()) {
                 // 时刻观察是否拿到response
                done.await(timeout, TimeUnit.MILLISECONDS);
                if (isDone() || System.currentTimeMillis() - start > timeout) {
                    // 如果拿到结果或者超时了，跳出循环
                    break;
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
        if (! isDone()) {
            // 这个时候还没拿到结果，肯定是认为超时了，抛出TimeoutException
            throw new TimeoutException(sent > 0, channel, getTimeoutMessage(false));
        }
    }
    return returnFromResponse();
}

private Object returnFromResponse() throws RemotingException {
    Response res = response;
    if (res == null) {
         // 拿到的结果是无效的
        throw new IllegalStateException("response cannot be null");
    }
    if (res.getStatus() == Response.OK) {
        // 这才是真的调用成功，返回数据了
        return res.getResult();
    }
    if (res.getStatus() == Response.CLIENT_TIMEOUT || res.getStatus() == Response.SERVER_TIMEOUT) {
        // 客户端超时或者服务端超时，抛出TimeoutException
        throw new TimeoutException(res.getStatus() == Response.SERVER_TIMEOUT, channel, res.getErrorMessage());
    }
    // 其他的就抛出RemotingException异常，并从res获取错误原因
    throw new RemotingException(channel, res.getErrorMessage());
}
```

**至此整个的远程调用就全部结束了**

 

## 总结整体流程

1. 结合spring配置好bean信息以及各种协议
2. 连接并注册到注册中心
3. 订阅消息，获取到服务提供方IP:PROT列表（具体得看服务提供方有多少）
4. 根据IP:PROT 生成对应的Invoker（Invoker是调用的实体，其中包含了所具备的信息）
5. 动态代理执行invoke反射执行方法
6. 是否会进行mock测试
7. 负载均衡选择合适的某一具体服务提供方
8. 加入重试机制，如果出现类似timeout等情况会进行重试操作（有一点需要注意，biz异常是不再进行重试，而直接上抛异常）
9. 服务异步调用或者有无返回值

 

 

![image.png](file:///C:/Users/HUAWEI%20MateBook%20Xpro/AppData/Local/Packages/oice_16_974fa576_32c1d314_38f6/AC/Temp/msohtmlclip1/01/clip_image014.gif)

 

服务提供者的集群集群⾥⾯有⼏个服务提供者，就有⼏个invoker，invoker理解成调⽤⼀个服务提供者需要的完整的细节，封装成的对象

那集群是怎么知道有⼏个服务提供者——从注册中⼼获得，注册中⼼获得的数据封装在RegistryDirectory对象中。那么RegistryDirectory怎么得到注册中⼼中的url地址呢？必须有⼀个zk客户端：ZookeeperRegistry

RegistryDirectory⾥包含了ZookeeperRegistry，RegistryDirectory维护了所有的invoker调⽤器，调⽤器通过RegsitryDirectory（ZookeeperRegistry）的⽅法获得的。

AbstractClusterInvoker⾥包含了RegistryDirectory，换句话说，RegistryDirectory被AbstractClusterInvoker所使⽤。真正执⾏的是AbstractClusterInvoker中的invoker⽅法，负载均衡也在⾥⾯。

proxy是由JavassistProxyFactory⽣成的，拼装代码来⽣成的。代理对象通过JavassistProxyFactory中的InvokerInvocationHandler ⽣成⼀个代理对象，来发起对集群的调⽤。

InvokerInvocationHandler⾥封装了RpcInvocation，RpcInvocation⾥封装的是这⼀次请求所需要的所有参数。

这个invoker如果⽤的是dubbo协议，那么就是DubboInvoker（还有http RMI等协议）

源码中的invoker.invoke()中的invoker，如果是dubbo协议，那么就是DubboInvoker。



<!-- @include: @article-footer.snippet.md -->     