---
title: dubbo - SPI机制
category: 微服务
tag:
 - dubbo
 - RPC
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,dubbo,RPC协议,dubbo的SPI机制
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---



## JDK的SPI机制的缺点

⽂件中的所有类都会被加载且被实例化。这样也就导致获取某个实现类的方式不够灵活，只能通过 Iterator 形式获取，不能根据某个参数来获取对应的实现类。如果不想用某些实现类，或者某些类实例化很耗时，它也被载入并实例化了，没有办法指定某⼀个类来加载和实例化，这就造成了浪费。

此时dubbo的SPI可以解决

 

## dubbo的SPI机制

dubbo⾃⼰实现了⼀套SPI机制来解决Java的SPI机制存在的问题。

dubbo中则采用了类似kv对的样式，在具体使用的时候则通过相关想法即可获取，而且获取的文件路径也不一致

 

**ExtensionLoader 类文件**

```java
private static final String SERVICES_DIRECTORY = "META-INF/services/";

private static final String DUBBO_DIRECTORY = "META-INF/dubbo/";
    
private static final String DUBBO_INTERNAL_DIRECTORY = DUBBO_DIRECTORY + "internal/";
```



 

如上述代码片段可知，dubbo是支持从META-INF/dubbo/,META-INF/dubbo/internal/以及META-INF/services/三个文件夹的路径去获取spi配置

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301931070.png)

 

 

例如com.alibaba.dubbo.rpc.Protocol 文件内容

```java
registry=com.alibaba.dubbo.registry.integration.RegistryProtocol
filter=com.alibaba.dubbo.rpc.protocol.ProtocolFilterWrapper
listener=com.alibaba.dubbo.rpc.protocol.ProtocolListenerWrapper
mock=com.alibaba.dubbo.rpc.support.MockProtocol
injvm=com.alibaba.dubbo.rpc.protocol.injvm.InjvmProtocol
dubbo=com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol
rmi=com.alibaba.dubbo.rpc.protocol.rmi.RmiProtocol
hessian=com.alibaba.dubbo.rpc.protocol.hessian.HessianProtocol
com.alibaba.dubbo.rpc.protocol.http.HttpProtocol
com.alibaba.dubbo.rpc.protocol.webservice.WebServiceProtocol
thrift=com.alibaba.dubbo.rpc.protocol.thrift.ThriftProtocol
memcached=memcom.alibaba.dubbo.rpc.protocol.memcached.MemcachedProtocol
redis=com.alibaba.dubbo.rpc.protocol.redis.RedisProtocol
```

不过观察上述文件会发现，HttpProtocol是没有对应的k值，那就是说无法通过kv对获取到其协议实现类。后面通过源码可以发现，如果没有对应的name的时候，dubbo会通过findAnnotationName方法获取一个可用的name

 

## Dubbo SPI 使用 & 源码学习

通过获取协议的代码来分析下具体的操作过程

### Protocol 获取

Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

```java
public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
    // 静态方法，意味着可以直接通过类调用
    if (type == null)
        throw new IllegalArgumentException("Extension type == null");
    if(!type.isInterface()) {
        throw new IllegalArgumentException("Extension type(" + type + ") is not interface!");
    }
    if(!withExtensionAnnotation(type)) {
        throw new IllegalArgumentException("Extension type(" + type + 
                ") is not extension, because WITHOUT @" + SPI.class.getSimpleName() + " Annotation!");
    }
    
    ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
    // 从map中获取该类型的ExtensionLoader数据
    if (loader == null) {
        EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
        // 如果没有则，创建一个新的ExtensionLoader对象，并且以该类型存储
        loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        // 再从map中获取
        // 这里这样做的原因就是为了防止并发的问题，而且map本是也是个ConcurrentHashMap
    }
    return loader;
}
```



```java
private ExtensionLoader(Class<?> type) {
    this.type = type;
    objectFactory = (type == ExtensionFactory.class ? null : ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
}
// 传入的type是Protocol.class，所以需要获取ExtensionFactory.class最合适的实现类
```



```java
public T getAdaptiveExtension() {
    Object instance = cachedAdaptiveInstance.get();
    if (instance == null) {
        if(createAdaptiveInstanceError == null) {
            synchronized (cachedAdaptiveInstance) {
                instance = cachedAdaptiveInstance.get();
                if (instance == null) {
                    try {
                        instance = createAdaptiveExtension();
                        // 创建对象，也是需要关注的函数
                        cachedAdaptiveInstance.set(instance);
                    } catch (Throwable t) {
                        createAdaptiveInstanceError = t;
                        throw new IllegalStateException("fail to create adaptive instance: " + t.toString(), t);
                    }
                }
            }
        }
        else {
            throw new IllegalStateException("fail to create adaptive instance: " + createAdaptiveInstanceError.toString(), createAdaptiveInstanceError);
        }
    }

    return (T) instance;
}
```



```java
private T createAdaptiveExtension() {
    try {
        return injectExtension((T) getAdaptiveExtensionClass().newInstance());
        // (T) getAdaptiveExtensionClass().newInstance() 创建一个具体的实例对象
        // getAdaptiveExtensionClass() 生成相关的class
        // injectExtension 往该对象中注入数据
    } catch (Exception e) {
        throw new IllegalStateException("Can not create adaptive extenstion " + type + ", cause: " + e.getMessage(), e);
    }
}
```



```java
private Class<?> getAdaptiveExtensionClass() {
    getExtensionClasses();
    // 通过加载SPI配置，获取到需要的所有的实现类存储到map中
    // 通过可能会去修改cachedAdaptiveClass数据，具体原因在spi配置文件解析中分析
    if (cachedAdaptiveClass != null) {
        return cachedAdaptiveClass;
    }
    return cachedAdaptiveClass = createAdaptiveExtensionClass();
}
```



```java
private Class<?> createAdaptiveExtensionClass() {
    String code = createAdaptiveExtensionClassCode();
    // 动态生成需要的代码内容字符串
    ClassLoader classLoader = findClassLoader();
    com.alibaba.dubbo.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
    return compiler.compile(code, classLoader);
    // 编译，生成相应的类
}
```



如下代码Protocol$Adpative 整个的类就是通过createAdaptiveExtensionClassCode()方法**生成的一个大字符串**

```java
public class Protocol$Adpative implements com.alibaba.dubbo.rpc.Protocol {  
    public void destroy() {throw new UnsupportedOperationException("method public abstract void com.alibaba.dubbo.rpc.Protocol.destroy() of interface com.alibaba.dubbo.rpc.Protocol is not adaptive method!");  
    }  
    public int getDefaultPort() {throw new UnsupportedOperationException("method public abstract int com.alibaba.dubbo.rpc.Protocol.getDefaultPort() of interface com.alibaba.dubbo.rpc.Protocol is not adaptive method!");  
    }  
    public com.alibaba.dubbo.rpc.Exporter export(com.alibaba.dubbo.rpc.Invoker arg0) throws com.alibaba.dubbo.rpc.RpcException {  
        // 暴露远程服务，传入的参数是invoke对象
        if (arg0 == null) throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument == null");  
  
        if (arg0.getUrl() == null) throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument getUrl() == null");  
  
        com.alibaba.dubbo.common.URL url = arg0.getUrl();  
  
        String extName = ( url.getProtocol() == null ? "dubbo" : url.getProtocol() );  
        // 如果没有具体协议，则使用dubbo协议
  
        if(extName == null) throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.Protocol) name from url(" + url.toString() + ") use keys([protocol])");  
  
        com.alibaba.dubbo.rpc.Protocol extension = (com.alibaba.dubbo.rpc.Protocol)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.Protocol.class).getExtension(extName);  
  
        return extension.export(arg0);  
    }  
    public com.alibaba.dubbo.rpc.Invoker refer(java.lang.Class arg0, com.alibaba.dubbo.common.URL arg1) throws com.alibaba.dubbo.rpc.RpcException {  
        // 引用远程对象，生成相对应的远程invoke对象
        if (arg1 == null) throw new IllegalArgumentException("url == null");  
  
        com.alibaba.dubbo.common.URL url = arg1;  
  
        String extName = ( url.getProtocol() == null ? "dubbo" : url.getProtocol() );  
  
        if(extName == null) throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.Protocol) name from url(" + url.toString() + ") use keys([protocol])");  
  
        com.alibaba.dubbo.rpc.Protocol extension = (com.alibaba.dubbo.rpc.Protocol)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.Protocol.class).getExtension(extName);  
  
        return extension.refer(arg0, arg1);  
    }  
}  
```



到现在可以认为是最上面的获取protocol的方法Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension() 返回了一个代码拼接而成然后编译操作的实现类Protocol$Adpative

可是得到具体的实现呢？在com.alibaba.dubbo.rpc.Protocol extension = (com.alibaba.dubbo.rpc.Protocol)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.Protocol.class).getExtension(extName)这个代码中，当然这个是有在具体的暴露服务或者引用远程服务才被调用执行的。

```java
public T getExtension(String name) {
    if (name == null || name.length() == 0)
        throw new IllegalArgumentException("Extension name == null");
    if ("true".equals(name)) {
        return getDefaultExtension();
    }
    Holder<Object> holder = cachedInstances.get(name);
    if (holder == null) {
        cachedInstances.putIfAbsent(name, new Holder<Object>());
        holder = cachedInstances.get(name);
    }
    // 无论是否真有数据，在cachedInstances存储的是一个具体的Holder对象
    Object instance = holder.get();
    if (instance == null) {
        synchronized (holder) {
            instance = holder.get();
            if (instance == null) {
                instance = createExtension(name);
                // 创建对象
                holder.set(instance);
            }
        }
    }
    return (T) instance;
}
```

```java
private T createExtension(String name) {
    Class<?> clazz = getExtensionClasses().get(name);
    // 获取具体的实现类的类
    if (clazz == null) {
        throw findException(name);
    }
    try {
        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        if (instance == null) {
            EXTENSION_INSTANCES.putIfAbsent(clazz, (T) clazz.newInstance());
            // clazz.newInstance 才是真正创建对象的操作
            instance = (T) EXTENSION_INSTANCES.get(clazz);
        }
        injectExtension(instance);
        // 往实例中反射注入参数
        Set<Class<?>> wrapperClasses = cachedWrapperClasses;
        if (wrapperClasses != null && wrapperClasses.size() > 0) {
            for (Class<?> wrapperClass : wrapperClasses) {
                instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
            }
        }
        return instance;
    } catch (Throwable t) {
        throw new IllegalStateException("Extension instance(name: " + name + ", class: " +
                type + ")  could not be instantiated: " + t.getMessage(), t);
    }
}
```

到这一步就可以认为是dubbo的spi加载整个的过程完成了，整个链路有些长，需要好好的梳理一下

### SPI配置文件解析

上文说到getExtensionClasses完成对spi文件的解析

```java
private Map<String, Class<?>> loadExtensionClasses() {
    final SPI defaultAnnotation = type.getAnnotation(SPI.class);
    // 查看该类是否存在SPI注解信息
    if(defaultAnnotation != null) {
        String value = defaultAnnotation.value();
        if(value != null && (value = value.trim()).length() > 0) {
            String[] names = NAME_SEPARATOR.split(value);
            if(names.length > 1) {
                throw new IllegalStateException("more than 1 default extension name on extension " + type.getName()
                        + ": " + Arrays.toString(names));
            }
            if(names.length == 1) cachedDefaultName = names[0];
            // 设置默认的名称，如果注解的值经过切割，发现超过1个的数据，则同样会认为错误
        }
    }
    
    Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();
    loadFile(extensionClasses, DUBBO_INTERNAL_DIRECTORY);
    // 加载文件
    loadFile(extensionClasses, DUBBO_DIRECTORY);
    loadFile(extensionClasses, SERVICES_DIRECTORY);
    return extensionClasses;
}
    
private void loadFile(Map<String, Class<?>> extensionClasses, String dir) {
    String fileName = dir + type.getName();
    // type.getName就是类名称，和java的类似
    try {
        Enumeration<java.net.URL> urls;
        ClassLoader classLoader = findClassLoader();
        if (classLoader != null) {
            urls = classLoader.getResources(fileName);
        } else {
            urls = ClassLoader.getSystemResources(fileName);
        }
        if (urls != null) {
            while (urls.hasMoreElements()) {
                java.net.URL url = urls.nextElement();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                    try {
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            final int ci = line.indexOf('#');
                            if (ci >= 0) line = line.substring(0, ci);
                            line = line.trim();
                            if (line.length() > 0) {
                                try {
                                    String name = null;
                                    int i = line.indexOf('=');
                                    // 对k=v这样的格式进行分割操作，分别获取
                                    if (i > 0) {
                                        name = line.substring(0, i).trim();
                                        line = line.substring(i + 1).trim();
                                    }
                                    if (line.length() > 0) {
                                        Class<?> clazz = Class.forName(line, true, classLoader);
                                        if (! type.isAssignableFrom(clazz)) {
                                            throw new IllegalStateException("Error when load extension class(interface: " +
                                                    type + ", class line: " + clazz.getName() + "), class " 
                                                    + clazz.getName() + "is not subtype of interface.");
                                        }
                                        if (clazz.isAnnotationPresent(Adaptive.class)) {
                                            // 如果获取的类包含了Adaptive注解
                                            if(cachedAdaptiveClass == null) {
                                                cachedAdaptiveClass = clazz;
                                            } else if (! cachedAdaptiveClass.equals(clazz)) {
                                               // 已经存在了该数据，现在又出现了，则抛出异常
                                                throw new IllegalStateException("More than 1 adaptive class found: "
                                                        + cachedAdaptiveClass.getClass().getName()
                                                        + ", " + clazz.getClass().getName());
                                            }
                                        } else {  // 不包含Adaptive注解信息
                                            try {
                                                clazz.getConstructor(type);
                                                // 查看构造函数是否包含了type的类型参数
                                                // 如果不存在，则抛出NoSuchMethodException异常
                                                Set<Class<?>> wrappers = cachedWrapperClasses;
                                                if (wrappers == null) {
                                                    cachedWrapperClasses = new ConcurrentHashSet<Class<?>>();
                                                    wrappers = cachedWrapperClasses;
                                                }
                                                wrappers.add(clazz);
                                                // 往wrappers中添加该类
                                            } catch (NoSuchMethodException e) {
                                                clazz.getConstructor();
                                                if (name == null || name.length() == 0) {
                                                   // 没用名字的那种，也就是不存在k=v这种样式
                                                   // 例如上面的HttpProtocol
                                                    name = findAnnotationName(clazz);
                                                    // 查看该类是否存在注解
                                                    if (name == null || name.length() == 0) {
                                                        if (clazz.getSimpleName().length() > type.getSimpleName().length()
                                                                && clazz.getSimpleName().endsWith(type.getSimpleName())) {
                                                            name = clazz.getSimpleName().substring(0, clazz.getSimpleName().length() - type.getSimpleName().length()).toLowerCase();
                                                        } else {
                                                            throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + url);
                                                        }
                                                    }
                                                }
                                                String[] names = NAME_SEPARATOR.split(name);
                                                // 可能存在多个，切割开
                                                if (names != null && names.length > 0) {
                                                    Activate activate = clazz.getAnnotation(Activate.class);
                                                    if (activate != null) {
                                                        // 如果类存在Activate的注解信息
                                                        cachedActivates.put(names[0], activate);
                                                    }
                                                    for (String n : names) {
                                                        if (! cachedNames.containsKey(clazz)) {
                                                            cachedNames.put(clazz, n);
                                                        }
                                                        Class<?> c = extensionClasses.get(n);
                                                        if (c == null) {
                                                            extensionClasses.put(n, clazz);
                                                            // 往容器中填充该键值对信息，k和v
                                                        } else if (c != clazz) {
                                                             // 存在多个同名扩展类，则抛出异常信息
                                                            throw new IllegalStateException("Duplicate extension " + type.getName() + " name " + n + " on " + c.getName() + " and " + clazz.getName());
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Throwable t) {
                                    IllegalStateException e = new IllegalStateException("Failed to load extension class(interface: " + type + ", class line: " + line + ") in " + url + ", cause: " + t.getMessage(), t);
                                    exceptions.put(line, e);
                                }
                            }
                        } // end of while read lines
                    } finally {
                        reader.close();
                    }
                } catch (Throwable t) {
                    logger.error("Exception when load extension class(interface: " +
                                        type + ", class file: " + url + ") in " + url, t);
                }
            } // end of while urls
        }
    } catch (Throwable t) {
        logger.error("Exception when load extension class(interface: " +
                type + ", description file: " + fileName + ").", t);
    }
}
```



这样完成了对spi配置文件的整个的扫描过程了

 

 <!-- @include: @article-footer.snippet.md -->     