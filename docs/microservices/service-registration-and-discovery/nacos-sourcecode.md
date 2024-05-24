---
title: Nacos - 实现原理
category: 微服务
tag:
  - Nacos
---



## Nacos介绍

现在微服务架构是目前开发的一个趋势。服务消费者要去调用多个服务提供者组成的集群。这里需要做到以下几点：

1. 服务消费者**需要在本地配置文件中维护服务提供者集群的每个节点的请求地址**。
2. 服务提供者集群中如果某个节点宕机，**服务消费者的本地配置中需要同步删除这个节点的请求地址**，防止请求发送到已经宕机的节点上造成请求失败。

因此需要引入服务注册中心，它具有以下几个功能：

1. 服务地址的管理。
2. 服务注册。
3. 服务动态感知。

而Nacos致力于解决微服务中的统一配置，服务注册和发现等问题。**Nacos集成了注册中心和配置中心**。其相关特性包括：

1. 服务发现和服务健康监测：Nacos支持基于DNS和RPC的服务发现，即**服务消费者可以使用DNS或者HTTP的方式来查找和发现服务**。Nacos提供对服务的实时的健康检查，阻止向不健康的主机或者服务实例发送请求。**Nacos支持传输层（Ping/TCP）、应用层（HTTP、Mysql）的健康检查。**

2. 动态配置服务：动态配置服务可以**以中心化、外部化和动态化的方式管理所有环境的应用配置和服务配置。**

3. 动态DNS服务：支持权重路由，让开发者更容易的实现中间层的负载均衡、更灵活的路由策略、流量控制以及DNS解析服务。

4. 服务和元数据管理：Nacos允许开发者从微服务平台建设的视角来管理数据中心的所有服务和元数据。如：服务的生命周期、静态依赖分析、服务的健康状态、服务的流量管理、路由和安全策略等。



## Nacos注册中心实现原理分析

### Nacos架构图

以下是Nacos的架构图：

![](https://mmbiz.qpic.cn/mmbiz_png/UkT70O7175PL33aBq8mTibG4yjjWAEG5sicb9eM5uUwnQLRPLmiakShejmBoFCgOzvslrCdTdX7vMAVxib0VI31pGw/640?wx_fmt=png&wxfrom=5&wx_lazy=1&wx_co=1&tp=webp)其中分为这么几个模块：

- **Provider APP**：服务提供者。

- **Consumer APP**：服务消费者。

- **Name Server**：通过Virtual IP或者DNS的方式实现Nacos高可用集群的服务路由。

- **Nacos Server**：Nacos服务提供者。

- - OpenAPI：功能访问入口
  - Config Service：Nacos提供的配置服务模块
  - Naming Service：Nacos提供的名字服务模块
  - Consistency Protocol：一致性协议，用来实现Nacos集群节点的数据同步，使用[Raft算法](https://www.seven97.top/microservices/protocol/raft-detail.html)实现。

- **Nacos Console**：Nacos控制台。



小总结：

- 服务提供者通过VIP（Virtual IP）访问Nacos Server高可用集群，基于OpenAPI完成服务的注册和服务的查询。
- Nacos Server的底层则通过数据一致性算法（Raft）来完成节点的数据同步。



### 注册中心的原理

首先，服务注册的功能体现在：

- 服务实例启动时注册到服务注册表、关闭时则注销（**服务注册**）。
- 服务消费者可以通过查询服务注册表来获得可用的实例（**服务发现**）。
- 服务注册中心需要调用服务实例的健康检查API来验证其是否可以正确的处理请求（**健康检查**）。

Nacos服务注册和发现的实现原理的图如下：

![](https://mmbiz.qpic.cn/mmbiz_png/UkT70O7175PL33aBq8mTibG4yjjWAEG5siaHapZNTFvYmcS4Z8VTVkDwYtGHIaBjb26emmt3gChDLZaT1bROqVibw/640?wx_fmt=png&wxfrom=5&wx_lazy=1&wx_co=1&tp=webp)





## Nacos服务注册源码

首先看下一个包：`spring-cloud-commons`

![](https://mmbiz.qpic.cn/mmbiz_png/UkT70O7175PL33aBq8mTibG4yjjWAEG5s3cCf6suBs55Tia1NenIGSibhkTWQYFLgjwZnGeoSWVz8WZhysmic8H0nw/640?wx_fmt=png&wxfrom=5&wx_lazy=1&wx_co=1&tp=webp)**这个ServiceRegistry接口是SpringCloud提供的服务注册的标准，集成到SpringCloud中实现服务注册的组件，都需要实现这个接口。**来看下它的结构：

```java
public interface ServiceRegistry<R extends Registration> {
    void register(R registration);
    void deregister(R registration);
    void close();
    void setStatus(R registration, String status);
    <T> T getStatus(R registration);
}
```

那么对于Nacos而言，该接口的实现类是`NacosServiceRegistry`，该类在这个pom包下：

![](https://mmbiz.qpic.cn/mmbiz_png/UkT70O7175PL33aBq8mTibG4yjjWAEG5sm05GO5oqPADYBtYthLjMc33VsaEyMj8IXx8uHmtI7UyFiaZ3gMlia2hQ/640?wx_fmt=png&wxfrom=5&wx_lazy=1&wx_co=1&tp=webp)

再回过头来看`spring-cloud-commons`包:

![](https://mmbiz.qpic.cn/mmbiz_png/UkT70O7175PL33aBq8mTibG4yjjWAEG5sictXFu9GFo8yoJOWd1rM0ggcrTDoBbibePyia6VBo0kLACIC2KxI2PmZA/640?wx_fmt=png&wxfrom=5&wx_lazy=1&wx_co=1&tp=webp)

`spring.factories`**主要是包含了自动装配的配置信息**，如图：

![](https://mmbiz.qpic.cn/mmbiz_png/UkT70O7175PL33aBq8mTibG4yjjWAEG5sNpOK7dgjOhROTyBqHAC31kukayUYjI6mNyFO8lAawibNFpibn62ZYzvw/640?wx_fmt=png&wxfrom=5&wx_lazy=1&wx_co=1&tp=webp)

在spring.factories中配置[EnableAutoConfiguration](https://www.seven97.top/framework/springboot/principleofautomaticassembly.html#enableautoconfiguration)的内容后，项目在启动的时候，会导入相应的自动配置类，那么也就允许对该类的相关属性进行一个自动装配。那么显然，在这里导入了`AutoServiceRegistrationAutoConfiguration`这个类，而这个类顾名思义是**服务注册相关的配置类**。

该类的完整代码如下：

```java
@Configuration(
    proxyBeanMethods = false
)
@Import({AutoServiceRegistrationConfiguration.class})
@ConditionalOnProperty(
    value = {"spring.cloud.service-registry.auto-registration.enabled"},
    matchIfMissing = true
)
public class AutoServiceRegistrationAutoConfiguration {
    @Autowired(
        required = false
    )
    private AutoServiceRegistration autoServiceRegistration;
    @Autowired
    private AutoServiceRegistrationProperties properties;

    public AutoServiceRegistrationAutoConfiguration() {
    }

    @PostConstruct
    protected void init() {
        if (this.autoServiceRegistration == null && this.properties.isFailFast()) {
            throw new IllegalStateException("Auto Service Registration has been requested, but there is no AutoServiceRegistration bean");
        }
    }
}
```

这里做一个分析，`AutoServiceRegistrationAutoConfiguration`中注入了`AutoServiceRegistration`实例，该类的关系图如下：![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405242005376.webp)

先来看一下这个抽象类`AbstractAutoServiceRegistration`：

```java
public abstract class AbstractAutoServiceRegistration<R extends Registration> implements AutoServiceRegistration, 
ApplicationContextAware, 
ApplicationListener<WebServerInitializedEvent> {
 public void onApplicationEvent(WebServerInitializedEvent event) {
     this.bind(event);
 }
}
```

这里实现了`ApplicationListener`接口，并且传入了`WebServerInitializedEvent`作为泛型，意思是：

- `NacosAutoServiceRegistration`监听`WebServerInitializedEvent`事件。
- **也就是WebServer初始化完成后**，会调用对应的事件绑定方法，调用`onApplicationEvent（）`，该方法最终调用`NacosServiceRegistry`的`register（）`方法（`NacosServiceRegistry`实现了Spring的一个服务注册标准接口）。

对于`register（）`方法，主要调用的是Nacos Client SDK中的**NamingService下的registerInstance（）方法完成服务的注册**。

```java
public void register(Registration registration) {
    if (StringUtils.isEmpty(registration.getServiceId())) {
        log.warn("No service to register for nacos client...");
    } else {
        String serviceId = registration.getServiceId();
        String group = this.nacosDiscoveryProperties.getGroup();
        Instance instance = this.getNacosInstanceFromRegistration(registration);

        try {
            this.namingService.registerInstance(serviceId, group, instance);
            log.info("nacos registry, {} {} {}:{} register finished", new Object[]{group, serviceId, instance.getIp(), instance.getPort()});
        } catch (Exception var6) {
            log.error("nacos registry, {} register failed...{},", new Object[]{serviceId, registration.toString(), var6});
            ReflectionUtils.rethrowRuntimeException(var6);
        }

    }
}

public void registerInstance(String serviceName, String groupName, Instance instance) throws NacosException {
    if (instance.isEphemeral()) {
        BeatInfo beatInfo = new BeatInfo();
        beatInfo.setServiceName(NamingUtils.getGroupedName(serviceName, groupName));
        beatInfo.setIp(instance.getIp());
        beatInfo.setPort(instance.getPort());
        beatInfo.setCluster(instance.getClusterName());
        beatInfo.setWeight(instance.getWeight());
        beatInfo.setMetadata(instance.getMetadata());
        beatInfo.setScheduled(false);
        long instanceInterval = instance.getInstanceHeartBeatInterval();
        beatInfo.setPeriod(instanceInterval == 0L ? DEFAULT_HEART_BEAT_INTERVAL : instanceInterval);
        // 1.addBeatInfo（）负责创建心跳信息实现健康监测。因为Nacos Server必须要确保注册的服务实例是健康的。
        // 而心跳监测就是服务健康监测的一种手段。
        this.beatReactor.addBeatInfo(NamingUtils.getGroupedName(serviceName, groupName), beatInfo);
    }
 // 2.registerService（）实现服务的注册
    this.serverProxy.registerService(NamingUtils.getGroupedName(serviceName, groupName), groupName, instance);
}
```

再来看一下心跳监测的方法`addBeatInfo（）`：

```java
public void addBeatInfo(String serviceName, BeatInfo beatInfo) {
    LogUtils.NAMING_LOGGER.info("[BEAT] adding beat: {} to beat map.", beatInfo);
    String key = this.buildKey(serviceName, beatInfo.getIp(), beatInfo.getPort());
    BeatInfo existBeat = null;
    if ((existBeat = (BeatInfo)this.dom2Beat.remove(key)) != null) {
        existBeat.setStopped(true);
    }

    this.dom2Beat.put(key, beatInfo);
    // 通过schedule（）方法，定时的向服务端发送一个数据包，然后启动一个线程不断地检测服务端的回应。
    // 如果在指定的时间内没有收到服务端的回应，那么认为服务器出现了故障。
    // 参数1：可以说是这个实例的相关信息。
    // 参数2：一个long类型的时间，代表从现在开始推迟执行的时间，默认是5000
    // 参数3：时间的单位，默认是毫秒，结合5000即代表每5秒发送一次心跳数据包
    this.executorService.schedule(new BeatReactor.BeatTask(beatInfo), beatInfo.getPeriod(), TimeUnit.MILLISECONDS);
    MetricsMonitor.getDom2BeatSizeMonitor().set((double)this.dom2Beat.size());
}
```

心跳检查如果正常，即代表这个需要注册的服务是健康的，那么执行下面的注册方法`registerInstance（）`：

```java
public void registerService(String serviceName, String groupName, Instance instance) throws NacosException {
    LogUtils.NAMING_LOGGER.info("[REGISTER-SERVICE] {} registering service {} with instance: {}", new Object[]{this.namespaceId, serviceName, instance});
    Map<String, String> params = new HashMap(9);
    params.put("namespaceId", this.namespaceId);
    params.put("serviceName", serviceName);
    params.put("groupName", groupName);
    params.put("clusterName", instance.getClusterName());
    params.put("ip", instance.getIp());
    params.put("port", String.valueOf(instance.getPort()));
    params.put("weight", String.valueOf(instance.getWeight()));
    params.put("enable", String.valueOf(instance.isEnabled()));
    params.put("healthy", String.valueOf(instance.isHealthy()));
    params.put("ephemeral", String.valueOf(instance.isEphemeral()));
    params.put("metadata", JSON.toJSONString(instance.getMetadata()));
    // 这里可以看出来，把上述服务实例的一些必要参数保存到一个Map中，通过OpenAPI的方式发送注册请求
    this.reqAPI(UtilAndComs.NACOS_URL_INSTANCE, params, (String)"POST");
}
```



### 案例1：用Debug来理解Nacos服务注册流程

下面直接Debug走一遍：

- 启动一个Nacos服务。
- 搞一个Maven项目，集成Nacos。

1. 项目初始化后，根据上文说法，会执行抽象类`AbstractAutoServiceRegistration`下面的`onApplicationEvent（）`方法，**即事件被监听到。**![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405242005418.webp)

   

2. 作为抽象类的子类实现`NacosAutoServiceRegistration`，监听到Web服务启动后， 开始执行`super.register（）`方法。![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405242005344.webp)

   

3. 执行`NacosServiceRegistry`下的`register（）`方法（**super**)，前面说过，集成到SpringCloud中实现服务注册的组件，都需要实现`ServiceRegistry`这个接口，而对于Nacos而言，`NacosServiceRegistry`就是具体的实现子类。执行注册方法需要传入的三个参数：

   - 实例名称serviceId。

   - 实例归属的组。

   - 具体实例


![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405242005354.webp)

而`registerInstance（）`主要做两件事：

- 检查服务的健康（`this.beatReactor.addBeatInfo（）`）。
- 执行服务的注册（`this.serverProxy.registerService（）`）。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405242005430.webp)

服务健康的检查：![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405242005400.webp)

检查通过后，发送OpenAPI进行服务的注册：![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405242005178.webp)



### 服务注册小结：

通过问答的形式来进行总结

1. **Nacos的服务注册为什么和spring-cloud-commons这个包扯上关系？**
   1. 首先，Nacos的服务注册肯定少不了pom包：spring-cloud-starter-alibaba-nacos-discovery吧。这个包下面包括了spring-cloud-commons包，那么这个包有什么用？
   2. spring-cloud-commons中有一个接口叫做ServiceRegistry，而集成到SpringCloud中实现服务注册的组件，都需要实现这个接口。
   3. 因此对于需要注册到Nacos上的服务，也需要实现这个接口，那么具体的实现子类为NacosServiceRegistry。



2. **为什么项目加了这几个依赖，服务启动时依旧没有注册到Nacos中？**
   1. 进行Nacos服务注册的时候，会有一个事件的监听过程，而监听的对象是WebServer，因此，这个项目需要是一个Web项目
   2. 因此需要查看pom文件中是否有依赖：spring-boot-starter-web



3. **spring-cloud-commons这个包还有什么作用？**
   1. 这个包下的spring.factories文件中，配置了相关的服务注册的置类，即支持其自动装配。
   2. 这个配置类叫做AutoServiceRegistrationAutoConfiguration。其注入了类AutoServiceRegistration，而NacosAutoServiceRegistration是该类的一个具体实现。
   3. 当WebServer初始化的时候，通过绑定的事件监听器，会实现监听，执行服务的注册逻辑。



说白了：

1. 第一件事情：引入一个Spring监听器，当容器初始化后，执行Nacos服务的注册。
2. 第二件事情：而Nacos服务注册的方法的实现，其需要实现的接口来自于该包下的`ServiceRegistry`接口。

------



接下来就对Nacos注册的流程进行一个总结：

1. 服务（项目）启动时，根据`spring-cloud-commons`中`spring.factories`的配置，自动装配了类`AutoServiceRegistrationAutoConfiguration`。
2. `AutoServiceRegistrationAutoConfiguration`类中注入了类`AutoServiceRegistration`，其最终实现子类实现了Spring的监听器。
3. 根据监听器，执行了服务注册方法。而这个服务注册方法则是调用了`NacosServiceRegistry`的`register（）`方法。
4. 该方法主要调用的是Nacos Client SDK中的`NamingService`下的`registerInstance（）`方法完成服务的注册。
5. `registerInstance（）`方法主要做两件事：**服务实例的健康监测和实例的注册。**
6. 通过`schedule（）`方法**定时的发送数据包，检测实例的健康。**
7. 若健康监测通过，调用`registerService（）`方法，通过OpenAPI方式执行服务注册，其中将实例Instance的相关信息存储到HashMap中。



## Nacos服务发现源码

有一点我们需要清楚：Nacos服务的发现发生在什么时候。例如：微服务发生远程接口调用的时候。一般我们在使用OpenFeign进行远程接口调用时，都需要用到对应的微服务名称，而这个名称就是用来进行服务发现的。

举个例子：

```
@FeignClient("test-application")
public interface MyFeignService {
    @RequestMapping("getInfoById")
    R info(@PathVariable("id") Long id);
}
```

接下来直接开始讲重点，Nacos在进行服务发现的时候，会调用`NacosServerList`类下的`getServers()`方法：

```
public class NacosServerList extends AbstractServerList<NacosServer> {
 private List<NacosServer> getServers() {
        try {
            String group = this.discoveryProperties.getGroup();
            // 1.通过唯一的serviceId（一般是服务名称）和组来获得对应的所有实例。
            List<Instance> instances = this.discoveryProperties.namingServiceInstance().selectInstances(this.serviceId, group, true);
            // 2.将List<Instance>转换成List<NacosServer>数据，然后返回。
            return this.instancesToServerList(instances);
        } catch (Exception var3) {
            throw new IllegalStateException("Can not get service instances from nacos, serviceId=" + this.serviceId, var3);
        }
    }
}
```

接下来来看一下`NacosNamingService.selectInstances（）`方法：

```
public List<Instance> selectInstances(String serviceName, String groupName, boolean healthy) throws NacosException {
   return this.selectInstances(serviceName, groupName, healthy, true);
}
```

该方法最终会调用到其**重载方法**：

```
public List<Instance> selectInstances(String serviceName, String groupName, List<String> clusters, 
  boolean healthy, boolean subscribe) throws NacosException {
 // 保存服务实例信息的对象
    ServiceInfo serviceInfo;
    // 如果该消费者订阅了这个服务，那么会在本地维护一个服务列表，服务从本地获取
    if (subscribe) {
        serviceInfo = this.hostReactor.getServiceInfo(NamingUtils.getGroupedName(serviceName, groupName), StringUtils.join(clusters, ","));
    } else {
    // 否则实例会从服务中心进行获取。
        serviceInfo = this.hostReactor.getServiceInfoDirectlyFromServer(NamingUtils.getGroupedName(serviceName, groupName), StringUtils.join(clusters, ","));
    }

    return this.selectInstances(serviceInfo, healthy);
}
```

这里应该重点关注`this.hostReactor`这个对象，它里面比较重要的是几个Map类型的存储结构：

```
public class HostReactor {
    private static final long DEFAULT_DELAY = 1000L;
    private static final long UPDATE_HOLD_INTERVAL = 5000L;
    // 存放线程异步调用的一个回调结果
    private final Map<String, ScheduledFuture<?>> futureMap;
    // 本地已存在的服务列表，key是服务名称，value是ServiceInfo
    private Map<String, ServiceInfo> serviceInfoMap;
    // 待更新的实例列表
    private Map<String, Object> updatingMap;
    // 定时任务（负责服务列表的实时更新）
    private ScheduledExecutorService executor;
    ....
}
```

再看一看它的`getServiceInfo（）`方法：

```
public ServiceInfo getServiceInfo(String serviceName, String clusters) {
    LogUtils.NAMING_LOGGER.debug("failover-mode: " + this.failoverReactor.isFailoverSwitch());
    String key = ServiceInfo.getKey(serviceName, clusters);
    if (this.failoverReactor.isFailoverSwitch()) {
        return this.failoverReactor.getService(key);
    } else {
     // 1.先通过serverName即服务名获得一个serviceInfo
        ServiceInfo serviceObj = this.getServiceInfo0(serviceName, clusters);
        // 如果没有serviceInfo，则通过传进来的参数new出一个新的serviceInfo对象，并且同时维护到本地Map和更新Map
        // 这里是serviceInfoMap和updatingMap
        if (null == serviceObj) {
            serviceObj = new ServiceInfo(serviceName, clusters);
            this.serviceInfoMap.put(serviceObj.getKey(), serviceObj);
            this.updatingMap.put(serviceName, new Object());
            // 2.updateServiceNow（），立刻去Nacos服务端拉去数据。
            this.updateServiceNow(serviceName, clusters);
            this.updatingMap.remove(serviceName);
        } else if (this.updatingMap.containsKey(serviceName)) {
            synchronized(serviceObj) {
                try {
                    serviceObj.wait(5000L);
                } catch (InterruptedException var8) {
                    LogUtils.NAMING_LOGGER.error("[getServiceInfo] serviceName:" + serviceName + ", clusters:" + clusters, var8);
                }
            }
        }
  // 3.定时更新实例信息
        this.scheduleUpdateIfAbsent(serviceName, clusters);
        // 最后返回服务实例数据（前面已经进行了更新）
        return (ServiceInfo)this.serviceInfoMap.get(serviceObj.getKey());
    }
}
```

来看下`scheduleUpdateIfAbsent（）`方法：

```
// 通过心跳的方式，每10秒去更新一次数据，并不是只有在调用服务的时候才会进行更新，而是通过定时任务来异步进行。
public void scheduleUpdateIfAbsent(String serviceName, String clusters) {
    if (this.futureMap.get(ServiceInfo.getKey(serviceName, clusters)) == null) {
        synchronized(this.futureMap) {
            if (this.futureMap.get(ServiceInfo.getKey(serviceName, clusters)) == null) {
             // 创建一个UpdateTask的更新线程任务，每10秒去异步更新集合数据
                ScheduledFuture<?> future = this.addTask(new HostReactor.UpdateTask(serviceName, clusters));
                this.futureMap.put(ServiceInfo.getKey(serviceName, clusters), future);
            }
        }
    }
}
```

### 案例2：用Debug来理解Nacos服务发现流程

1. 进行远程接口调用，触发服务的发现，调用`NacosServerList`的`getServers（）`方法。**传入的serviceId和对应Feign接口上的接口@FeignClient中的名称一致。**![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405242005207.webp)

2. 例如，这里调用的Feign接口是：

   ```java
   @FeignClient("gulimall-member")
   public interface MemberFeignService {
       @RequestMapping("/member/member/info/{id}")
       R info(@PathVariable("id") Long id);
   }
   ```

   这里可以看出来，返回的是一个Instance类型的List，对应的服务也发现并返回了。![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405242036082.webp)



2. 这里则调用了`NacosNamingService`的`selectInstances（）`方法，我这里的subscribe值是true，**即代表我这个消费者直接订阅了这个服务，因此最终的信息是从本地Map中获取，即Nacos维护了一个注册列表。**!

3. 再看下`HostReactor的getServiceInfo（）`方法：最终所需要的结果是从serviceInfoMap中获取，并且**通过多个Map进行维护服务实例，若存在数据的变化，还会通过强制睡眠5秒钟的方式来等待数据的更新。**![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405242005382.webp)



4. 无论怎样都会调用`this.scheduleUpdateIfAbsent(serviceName, clusters)`方法：![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405242005461.webp)



5. 通过`scheduleUpdateIfAbsent（）`方法定时的获取实时的实例数据，并且负责维护本地的服务注册列表，若服务发生更新，则更新本地的服务数据。![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405242005517.webp)



### 服务发现小结：

经常有人说过，Nacos有个好处，就是当一个服务挂了之后，短时间内不会造成影响，因为有个本地注册列表，在服务不更新的情况下，服务还能够正常的运转，其原因如下：

1. Nacos的服务发现，一般是**通过订阅的形式来获取服务数据**。
2. 而通过订阅的方式，**则是从本地的服务注册列表中获取（可以理解为缓存）**。相反，如果不订阅，那么服务的信息将会从Nacos服务端获取，这时候就需要对应的服务是健康的。（宕机就不能使用了）
3. **在代码设计上，通过Map来存放实例数据，key为实例名称，value为实例的相关信息数据（ServiceInfo对象）。**

最后，服务发现的流程就是：

1. 以调用远程接口（OpenFeign）为例，当执行远程调用时，需要经过服务发现的过程。
2. 服务发现先执行`NacosServerList`类中的`getServers()`方法，根据远程调用接口上@FeignClient中的属性作为serviceId传入`NacosNamingService.selectInstances（）`方法中进行调用。
3. **根据subscribe的值来决定服务是从本地注册列表中获取还是从Nacos服务端中获取。**
4. 以本地注册列表为例，通过调用`HostReactor.getServiceInfo（）`来获取服务的信息（serviceInfo），Nacos本地注册列表由3个Map来共同维护。
   1. 本地Map–>serviceInfoMap，
   2. 更新Map–>updatingMap
   3. 异步更新结果Map–>futureMap,
   4. 最终的结果从serviceInfoMap当中获取。


5. HostReactor类中的`getServiceInfo（）`方法通过`this.scheduleUpdateIfAbsent()` 方法和`updateServiceNow（）`方法实现服务的**定时更新和立刻更新。**

6. 而对于**scheduleUpdateIfAbsent（）方法，则通过线程池来进行异步的更新**，将回调的结果（Future）保存到futureMap中，并且发生提交线程任务时，还负责更新本地注册列表中的数据。



