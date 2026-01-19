---
title: Sentinel - 使用
category: 微服务
tag:
 - Sentinel
 - 服务保护
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,Sentinel,请求限流,线程隔离,快速失败,服务熔断
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---

## 前言

Sentinel是阿里巴巴开源的一款微服务流量控制组件。官网地址：https://sentinelguard.io/zh-cn/index.html，官方wiki：

Sentinel 具有以下特征:
- 丰富的应用场景：Sentinel 承接了阿里巴巴近 10 年的双十一大促流量的核心场景，例如秒杀（即突发流量控制在系统容量可以承受的范围）、消息削峰填谷、集群流量控制、实时熔断下游不可用应用等。
- 完备的实时监控：Sentinel 同时提供实时的监控功能。可以在控制台中看到接入应用的单台机器秒级数据，甚至 500 台以下规模的集群的汇总运行情况。
- 广泛的开源生态：Sentinel 提供开箱即用的与其它开源框架/库的整合模块，例如与 Spring Cloud、Dubbo、gRPC 的整合。只需要引入相应的依赖并进行简单的配置即可快速地接入 Sentinel。
- 完善的 SPI 扩展点：Sentinel 提供简单易用、完善的 SPI 扩展接口。可以通过实现扩展接口来快速地定制逻辑。例如定制规则管理、适配动态数据源等。


## 服务保护技术对比

限流中间件的原理是在太有东西了，我这里简单列了一下他们之间的一些区别

|         | Sentinel                          | Hystrix                      |
| ------- | --------------------------------- | ---------------------------- |
| 隔离策略    | 信号量隔离                             | 线程池隔离/信号量隔离                  |
| 熔断降级策略  | 基于慢调用比例或异常比例                      | 基于失败比率                       |
| 实时指标实现  | 滑动窗口                              | 滑动窗口（基于 RxJava）              |
| 规则配置    | 支持多种数据源                           | 支持多种数据源                      |
| 扩展性     | 多个扩展点                             | 插件的形式                        |
| 基于注解的支持 | 支持                                | 支持                           |
| 限流      | 基于 QPS，支持基于调用关系的限流                | 有限的支持                        |
| 流量整形    | 支持慢启动、匀速排队模式                      | 不支持                          |
| 系统自适应保护 | 支持                                | 不支持                          |
| 控制台     | 开箱即用，可配置规则、查看秒级监控、机器发现等           | 不完善                          |
| 常见框架的适配 | Servlet、Spring Cloud、Dubbo、gRPC 等 | Servlet、Spring Cloud Netflix |



## 请求限流

簇点链路：就是项目内的调用链路，链路中被监控的每个接口就是一个资源。默认情况下sentinel会监控SpringMVC的每一个端点（Endpoint），因此SpringMVC的每一个端点（Endpoint）就是调用链路中的一个资源。

流控、熔断等都是针对簇点链路中的资源来设置的，因此我们可以点击对应资源后面的按钮来设置规则：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181645407.png)



点击资源/order/{orderId}后面的流控按钮，就可以弹出表单。表单中可以添加流控规则，如下图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181646639.png)

其含义是限制 /order/{orderId}这个资源的单机QPS为1，即每秒只允许1次请求，超出的请求会被拦截并报错。



### 流控模式

在添加限流规则时，点击高级选项，可以选择三种流控模式：

- 直接：统计当前资源的请求，触发阈值时对当前资源直接限流，也是默认的模式
- 关联：统计与当前资源相关的另一个资源，触发阈值时，对当前资源限流
- 链路：统计从指定链路访问到本资源的请求，触发阈值时，对指定链路限流

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181649726.png)

#### 关联模式

统计与当前资源相关的另一个资源，触发阈值时，对当前资源限流（高优先级资源触发阈值、对低优先级资源限流）

使用场景：比如用户支付时需要修改订单状态，同时用户要查询订单。查询和修改操作会争抢数据库锁，产生竞争。业务需求是有限支付和更新订单的业务，因此当修改订单业务触发阈值时，需要对查询订单业务限流。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181651653.png)

当/write资源访问量触发阈值时，就会对/read资源限流，避免影响/write资源。



也就是说，满足下面条件可以使用关联模式：

- 两个有竞争关系的资源
- 一个优先级较高，一个优先级较低

#### 链路模式

只针对从指定链路访问到本资源的请求做统计，判断是否超过阈值。

例如有两条请求链路：

- /test1  -》 /common
- /test2  -》 /common

如果只希望统计从/test2进入到/common的请求，则可以这样配置：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181652126.png)



**注意**：

Sentinel默认只标记Controller中的方法为资源，如果要标记其它方法，需要利用@SentinelResource注解，示例：

```java
@SentinelResource("goods")
public void queryGoods() {
    System.err.println("查询商品");
}
```



Sentinel默认会将Controller方法做context整合，导致链路模式的流控失效，需要修改application.yml，添加配置：

```yaml
spring:
	cloud:
		sentinel:
			web-context-unify: false # 关闭context整合
```



### 流控效果

流控效果是指请求达到流控阈值时应该采取的措施，包括三种：

- 快速失败：达到阈值后，新的请求会被立即拒绝并抛出FlowException异常。是默认的处理方式。
- warm up：预热模式，对超出阈值的请求同样是拒绝并抛出异常。但这种模式阈值会动态变化，从一个较小值逐渐增加到最大阈值。
- 排队等待：让所有的请求按照先后次序排队执行，两个请求的间隔不能小于指定时长

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181704440.png)

#### warm up

warm up也叫预热模式，是应对服务冷启动的一种方案。请求阈值初始值是 threshold / coldFactor，持续指定时长后，逐渐提高到threshold值。而coldFactor的默认值是3

例如，设置QPS的threshold为10，预热时间为5秒，那么初始阈值就是 10 / 3 ，也就是3，然后在5秒后逐渐增长到10

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181706853.png)

预热模式的QPS超过阈值时，会拒绝新的请求；QPS阈值是逐渐提升的，可以避免冷启动时高并发导致服务宕机。

#### 排队等待

当请求超过QPS阈值时，快速失败和warm up 会拒绝新的请求并抛出异常。

而排队等待则是让所有请求进入一个队列中，然后按照阈值允许的时间间隔依次执行。后来的请求必须等待前面执行完成，如果请求预期的等待时间超出最大时长，则会被拒绝。

例如：QPS = 5，意味着每200ms处理一个队列中的请求；timeout = 2000，意味着预期等待超过2000ms的请求会被拒绝并抛出异常

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181708838.png)

请求会进入队列，按照阈值允许的时间间隔依次执行请求；如果请求预期等待时长大于超时时间，直接拒绝



### 热点参数限流

之前的限流是统计访问某个资源的所有请求，判断是否超过QPS阈值。而热点参数限流是分别统计参数值相同的请求，判断是否超过QPS阈值。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181738735.png)

例如，以下示例：代表的含义是，对hot这个资源的0号参数（第一个参数）做统计，每1秒相同参数值的请求数不能超过5

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181739539.png)



在热点参数限流的高级选项中，可以对部分参数设置例外配置，例如有些商品的请求就是更频繁，可以额外设置限流

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181740593.png)

结合上一个配置，这里的含义是对0号的long类型参数限流，每1秒相同参数的QPS不能超过5，有两个例外：

- 如果参数值是100，则每1秒允许的QPS为10
- 如果参数值是101，则每1秒允许的QPS为15

> 注意：热点参数限流对默认的SpringMVC资源无效。只有被@SentinelResource注解的资源，才能配置热点参数限流



## 线程隔离

虽然限流可以尽量避免因高并发而引起的服务故障，但服务还会因为其它原因而故障。而要将这些故障控制在一定范围，避免雪崩，就要靠线程隔离（舱壁模式）和熔断降级手段了。

不管是线程隔离还是熔断降级，都是对客户端（调用方）的保护。



有两种方式实现：

- 线程池隔离
  - 优点：支持主动超时、支持异步调用
  - 缺点：线程的额外开销比较大
  - 场景：低扇出
- 信号量隔离（Sentinel默认采用）
  - 优点：轻量级，无额外开销
  - 缺点：不支持主动超时、不支持异步调用
  - 场景：高频调用、高扇出

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181753409.png)


在添加限流规则时，可以选择两种阈值类型：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181756644.png)

- QPS：就是每秒的请求数，就是请求限流
- 线程数：是该资源能使用的 tomcat 线程数的最大值。也就是通过限制线程数量，实现舱壁模式。

## 熔断降级

熔断降级是解决雪崩问题的重要手段。其思路是由**断路器**统计服务调用的异常比例、慢请求比例，如果超出阈值则会**熔断**该服务。即拦截访问该服务的一切请求；而当服务恢复时，断路器会放行访问该服务的请求。

状态描述：
- 关闭：熔断器默认处于关闭状态，熔断器本身带有计数能力（如滑动窗口实现）,当失败数量达到预设阀值后，触发状态变更，熔断器被打开
- 开启：在一定时间内，所有请求都会被拒绝，或采用备用链路处理。
- 半开启： 在刷新时间窗口后，会进入半开启状态,熔断器尝试接受请求，如果这阶段出现请求失败，直接恢复到开启状态。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181806066.png)


断路器熔断策略有三种：慢调用、异常比例、异常数

**慢调用**：业务的响应时长（RT）大于指定时长的请求认定为慢调用请求。在指定时间内，如果请求数量超过设定的最小数量，慢调用比例大于设定的阈值，则触发熔断。

例如：RT超过500ms的调用是慢调用，统计最近10000ms内的请求，如果请求量超过10次，并且慢调用比例 大等于0.5，则触发熔断，熔断时长为5秒。然后进入 half-open 状态，放行一次请求做测试。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181808630.png)



**异常比例、异常数**：统计指定时间内的调用，如果调用次数超过指定请求数，并且出现异常的比例达到设定的比例阈值（或超过指定异常数），则触发熔断。

例如：统计最近1000ms内的请求，如果请求量超过10次，并且异常比例 大等于0.5，则触发熔断，熔断时长为5秒。然后进入half-open状态，放行一次请求做测试。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181809686.png)

例如：统计最近1000ms内的请求，如果请求量超过10次，并且异常数大等于2 ，则触发熔断，熔断时长为5秒。然后进入half-open状态，放行一次请求做测试。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181811774.png)

## 授权规则

授权规则可以对调用方的来源做控制，有白名单和黑名单两种方式。

- 白名单：来源（origin）在白名单内的调用者允许访问
- 黑名单：来源（origin）在黑名单内的调用者不允许访问

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181813681.png)



例如，我们限定只允许从网关来的请求访问order-service，那么流控应用中就填写网关的名称

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181813984.png)



Sentinel是通过RequestOriginParser这个接口的parseOrigin来获取请求的来源的。

```java
public interface RequestOriginParser {
    /**
    * 从请求request对象中获取origin，获取方式自定义
     */
    String parseOrigin(HttpServletRequest request);
}
```

例如，我们尝试从request中获取一个名为origin的请求头，作为origin的值：

```java
@Component
public class HeaderOriginParser implements RequestOriginParser {
    @Override
    public String parseOrigin(HttpServletRequest request) {
        String origin = request.getHeader("origin");
        if(StringUtils.isEmpty(origin)){
            return "blank";
        }
        return origin;
    }
}
```





## 自定义异常

默认情况下，发生限流、降级、授权拦截时，都会抛出异常到调用方。如果要自定义异常时的返回结果，需要实现BlockExceptionHandler接口：

```java
public interface BlockExceptionHandler {
    /**
    * 处理请求被限流、降级、授权拦截时抛出的异常：BlockException
    */
    void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception;
}
```



而BlockException包含很多个子类，分别对应不同的场景：

| **异常**             | **说明**           |
| -------------------- | ------------------ |
| FlowException        | 限流异常           |
| ParamFlowException   | 热点参数限流的异常 |
| DegradeException     | 降级异常           |
| AuthorityException   | 授权规则异常       |
| SystemBlockException | 系统规则异常       |



可以实现BlockExceptionHandler接口来自定义异常：

```java
@Component
public class SentinelBlockHandler implements BlockExceptionHandler {
    @Override
    public void handle(
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse, BlockException e) throws Exception {
        String msg = "未知异常";
        int status = 429;
        if (e instanceof FlowException) {
            msg = "请求被限流了！";
        } else if (e instanceof DegradeException) {
            msg = "请求被降级了！"; 
        } else if (e instanceof ParamFlowException) {
            msg = "热点参数限流！";
        } else if (e instanceof AuthorityException) {
            msg = "请求没有权限！";
            status = 401;
        }
        httpServletResponse.setContentType("application/json;charset=utf-8");
        httpServletResponse.setStatus(status);
        httpServletResponse.getWriter().println("{\"message\": \"" + msg + "\", \"status\": " + status + "}");
    }
}
```



## 规则持久化

Sentinel的控制台规则管理有三种模式：

| **推送模式**      | **说明**                                                     | **优点**                     | **缺点**                                                     |
| ----------------- | ------------------------------------------------------------ | ---------------------------- | ------------------------------------------------------------ |
| 原始模式          | API 将规则推送至客户端并直接更新到内存中，扩展写数据源（WritableDataSource），默认就是这种 | 简单，无任何依赖             | 不保证一致性；规则保存在内存中，重启即消失。严重不建议用于生产环境 |
| Pull 模式         | 扩展写数据源（WritableDataSource）， 客户端主动向某个规则管理中心定期轮询拉取规则，这个规则中心可以是 RDBMS、文件 等 | 简单，无任何依赖；规则持久化 | 不保证一致性；实时性不保证，拉取过于频繁也可能会有性能问题。 |
| **Push** **模式** | 扩展读数据源（ReadableDataSource），规则中心统一推送，客户端通过注册监听器的方式时刻监听变化，比如使用 Nacos、Zookeeper 等配置中心。这种方式有更好的实时性和一致性保证。**生产环境下一般采用** **push** **模式的数据源。** | 规则持久化；一致性；         | 引入第三方依赖                                               |



原始模式：控制台配置的规则直接推送到Sentinel客户端，也就是我们的应用。然后保存在内存中，服务重启则丢失

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181823737.png)



pull模式：控制台将配置的规则推送到Sentinel客户端，而客户端会将配置规则保存在本地文件或数据库中。以后会定时去本地文件或数据库中查询，更新本地规则。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181823343.png)



push模式：控制台将配置规则推送到远程配置中心，例如Nacos。Sentinel客户端监听Nacos，获取配置变更的推送消息，完成本地配置更新。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202601181823197.png)



