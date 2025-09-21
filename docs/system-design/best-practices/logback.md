---
title: "Java 日志管理的黄金组合: SLF4J+Logback"
category: 系统设计
tag:
  - Lombok
  - 最佳实践
head:
  - - meta
    - name: keywords
      content: slf4j,logback
---

## slf4j 的前世今生
### Log4J、Log4J2和LogBack的历史故事

使用过Log4J和LogBack的同学肯定能发现，这两个框架的设计理念极为相似，使用方法也如出一辙。其实这个两个框架的作者都是一个人，Ceki Gülcü，俄罗斯程序员。

Log4J 最初是基于Java开发的日志框架，发展一段时间后，作者Ceki Gülcü将Log4j捐献给了Apache软件基金会，使之成为了Apache日志服务的一个子项目。 又由于Log4J出色的表现，后续又被孵化出了支持C, C++, C#, Perl, Python, Ruby等语言的子框架。

然而，伟大的程序员好像都比较有个性。Ceki Gülcü由于不满Apache对Log4J的管理，决定不再参加Log4J的开发维护。“出走”后的Ceki Gülcü另起炉灶，开发出了LogBack这个框架（SLF4J是和LogBack一起开发出来的）。LogBack改进了很多Log4J的缺点，在性能上有了很大的提升，同时使用方式几乎和Log4J一样，许多用户开始慢慢开始使用LogBack。

由于受到LogBack的冲击，Log4J开始式微。终于，2015年9月，Apache软件基金业宣布，Log4j不在维护，建议所有相关项目升级到Log4j2。Log4J2是Apache开发的一个新的日志框架，改进了很多Log4J的缺点，同时也借鉴了LogBack，号称在性能上也是完胜LogBack。性能这块后面我会仔细分析。

### 那slf4j和这些有什么关系？

SLF4J的全称是Simple Logging Facade for Java，slf4j是[**门面模式**](https://www.seven97.top/system-design/design-pattern/facade.html)的典型应用

回答这个问题之前，我们先看看如果需要用上面几个日志框架来打印日志，一般怎么做，具体代码如下：

```java
// 使用log4j，需要log4j.jar 
import org.apache.log4j.Logger; 
Logger logger_log4j = Logger.getLogger(Test.class); 
logger_log4j.info("Hello World!"); 

// 使用log4j2，需要log4j-api.jar、log4j-core.jar 
import org.apache.logging.log4j.LogManager; 
import org.apache.logging.log4j.Logger; 
Logger logger_log4j2 = LogManager.getLogger(Test.class); 
logger_log4j2.info("Hello World!"); 

// logback，需要logback-classic.jar、logback-core.jar 
import ch.qos.logback.classic.Logger; 
import ch.qos.logback.classic.LoggerContext; 
Logger logger_logback = new LoggerContext().getLogger(Test.class); logger_logback.info("Hello World!");
```

从上面不难看出，使用不同的日志框架，就要引入不同的jar包，使用不同的代码获取Logger。如果项目升级需要更换不同的框架，那么就需要修改所有的地方来获取新的Logger，这将会产生巨大的工作量。

基于此，**我们需要一种接口来将不同的日志框架的使用统一起来，这也是为什么要使用slf4j的原因。**

**SLF4J，即简单日志门面（Simple Logging Facade for Java），不是具体的日志解决方案，它只服务于各种各样的日志系统。按照官方的说法，SLF4J是一个用于日志系统的简单Facade，允许最终用户在部署其应用时使用其所希望的日志系统。**

> 注意：类似的日志门面还有Jakarta Common logging（JCL），主要区别在于，SLF4J是一个比较新的日志框架，它更加灵活，性能更好，支持更多的日志实现，而且JCL基于classLoader在运行时动态加载日志框架，可能会产生很多意想不到的安全问题

通过上面的介绍，我们可以知道JCL和SLF4J都是日志门面（Facade），而Log4J、Log4J2和LogBack都是子系统角色（SunSystem），也就是具体的日志实现框架。他们的关系如下，JUL是JDK本身提供的一种实现。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202509211008864.png)


**SLF4J 的核心价值**在于它提供了**解耦设计**​：应用程序代码只依赖 slf4j-api，而具体日志实现（如 Logback、Log4j2）可以在部署时动态绑定。这种架构使得项目升级或更换日志框架变得非常简单，无需修改业务代码中的日志记录语句。

### slf4j怎么和日志框架结合使用？

使用slf4j后，当我们在打印日志时，就可以使用下面的方式：

```java
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 
Logger logger = LoggerFactory.getLogger(Test.class); 
logger.info("Hello World!")
```

这又引入了另外一个问题，slf4j如何决定使用哪个框架日志呢，并且引入哪些jar包呢？官方为我们准备了组合依赖：
- **slf4j + logback**： slf4j-api.jar + logback-classic.jar + logback-core.jar
- **slf4j + log4j**： slf4j-api.jar + slf4j-log412.jar + log4j.jar
- **slf4j + jul**： slf4j-api.jar + slf4j-jdk14.jar
- **也可以只用slf4j无日志实现**：slf4j-api.jar + slf4j-nop.jar


## SLF4J 的基本使用

在代码中使用 SLF4J 非常简单，首先需要通过 Maven 添加依赖：

```xml
<!-- Maven 依赖配置 -->
<dependencies>
    <!-- SLF4J API -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>
    <!-- Logback Classic 实现 -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.11</version>
    </dependency>
</dependencies>
```

在代码中获取 Logger 并记录日志：
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleService {
    // 获取Logger实例
    private static final Logger logger = LoggerFactory.getLogger(ExampleService.class);
    
    public void processUser(String userId) {
        logger.debug("Processing user: {}", userId); // 使用占位符，避免字符串拼接
        logger.info("User processing started");
        
        try {
            // 业务逻辑
            logger.info("User {} processed successfully", userId);
        } catch (Exception e) {
            logger.error("Failed to process user: " + userId, e); // 记录异常信息
        }
    }
}
```


SLF4J 的 ​**参数化日志消息**​（使用 `{}`占位符）是其一个重要特性，它不仅有更好的可读性，还能**提升性能**——当日志级别高于当前配置时（如配置为 INFO 级别时调用 debug 语句），不会执行字符串拼接操作。

## Logback 架构与核心组件

Logback 是 SLF4J 的**原生实现**框架，由三个相互协作的模块组成，每个模块都有独特的功能定位

###  Logback 的模块化设计

|模块|说明|
|---|---|
|`logback-core`|核心模块，提供基础日志服务，其他两个模块都依赖它|
|`logback-classic`|实现 SLF4J API，完全兼容 SLF4J 接口，同时兼容 Log4j|
|`logback-access`|与 Servlet 容器集成，用于 HTTP 访问日志记录|

Logback 相比 Log4j 有显著**性能提升**，特别是在异步日志记录方面，减少了线程阻塞和上下文切换开销。它还支持**自动重载配置**，可以在不重启应用的情况下修改日志配置。

### Logback 核心概念

Logback 架构基于三个核心概念：Logger、Appender 和 Layout/Encoder。

​**Logger（日志记录器）​**​：
- 采用**层次化命名**​（如 `com.example.service.UserService`）
- 具有**继承性**​：子 Logger 继承父 Logger 的 Appender 和 Level
- 通过 `LoggerFactory.getLogger()`获取实例


​**Appender（输出目的地）​**​：

Appender 负责将日志事件发送到不同目标，Logback 支持多种 Appender：

| Appender 类型           | 说明             |
| --------------------- | -------------- |
| `ConsoleAppender`     | 输出到控制台         |
| `FileAppender`        | 输出到文件          |
| `RollingFileAppender` | 滚动文件（按大小/时间）   |
| `SocketAppender`      | 发送到远程服务器       |
| `SMTPAppender`        | 邮件告警           |
| `KafkaAppender`       | 发送到 Kafka（需扩展） |

​**Layout/Encoder（格式化器）​**​：

定义日志输出格式，常用占位符包括：
- `%d{yyyy-MM-dd HH:mm:ss.SSS}`：时间戳
- `%level`：日志级别
- `%thread`：线程名
- `%logger{36}`：Logger 名（缩写）
- `%msg`：日志消息
- `%n`：换行符

## Logback 配置详解与案例

Logback 支持 XML 和 Groovy 两种配置格式，其中 XML 是最常用的方式。下面通过实际案例详细讲解 Logback 的配置。

### 基础配置结构

Logback 配置文件通常命名为 `logback.xml`或 `logback-spring.xml`（Spring 环境），放置在 `src/main/resources/`目录下。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="30 seconds">
    
    <!-- 定义变量 -->
    <property name="LOG_HOME" value="./logs"/>
    <property name="APP_NAME" value="myapp"/>
    
    <!-- 开发环境开关 -->
    <springProfile name="dev">
        <property name="LOG_LEVEL" value="DEBUG"/>
    </springProfile>
    <springProfile name="prod">
        <property name="LOG_LEVEL" value="INFO"/>
    </springProfile>
    
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- 更多Appender配置 -->
    
    <!-- 根日志器 -->
    <root level="${LOG_LEVEL:-INFO}">
        <appender-ref ref="CONSOLE"/>
    </root>
    
    <!-- 特定包的日志级别 -->
    <logger name="com.example.service" level="DEBUG"/>
</configuration>
```

### Console Appender 配置

Console Appender 用于将日志输出到控制台，是开发环境中最常用的 Appender：

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
    <!-- 过滤器：只输出INFO及以上级别 -->
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
        <level>INFO</level>
    </filter>
</appender>
```


### File Appender 与滚动策略

生产环境中通常需要将日志输出到文件，并使用滚动策略防止文件过大：

```xml
<!-- 滚动文件输出（按天） -->
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_HOME}/${APP_NAME}.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <!-- 每天生成一个文件 -->
        <fileNamePattern>${LOG_HOME}/archive/%d{yyyy-MM-dd}/${APP_NAME}.%i.log.gz</fileNamePattern>
        <!-- 保留30天 -->
        <maxHistory>30</maxHistory>
        <!-- 单个文件最大100MB -->
        <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
            <maxFileSize>100MB</maxFileSize>
        </timeBasedFileNamingAndTriggeringPolicy>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```


### 日志级别配置

Logback 支持多个日志级别，合理配置级别对系统性能和可观测性至关重要

```xml
<!-- 根日志器设置 -->
<root level="${LOG_LEVEL:-INFO}">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
</root>

<!-- 特定包/类级别设置 -->
<!-- 特定日志器：将 com.example.service 包的日志级别设为 DEBUG，且日志仅输出到文件，不传递给根日志器（避免控制台重复输出） -->
<logger name="com.example.service" level="DEBUG" additivity="false">
    <appender-ref ref="FILE" />
</logger>

<!-- 特定日志器：将 org.springframework 包的日志级别设为 WARN，减少不必要的日志输出 -->
<logger name="org.springframework" level="WARN" />
```

在这个配置中：
- 绝大多数日志遵循根的 `INFO`级别设置。
- 唯独 `com.example.service`包下的日志可以输出 `DEBUG`级别及以上的内容，并且这些调试信息**只写入文件**，不会出现在控制台（因为 `additivity="false"`）。
- 所有来自 `org.springframework`包的日志，只有 `WARN`和 `ERROR`级别才会被记录。



**根日志器 (`<root>`)​**​ 和**特定包/类日志器 (`<logger>`)​**​ 的设置是日志配置的两个核心层面，主要在于作用和范围的区别：

| 特性         | 根日志器 (`<root>`)                          | 特定包/类日志器 (`<logger>`)                         |
| ---------- | ---------------------------------------- | --------------------------------------------- |
| ​**作用范围**​ | ​**全局默认**。影响所有未被特定 `<logger>`明确配置的日志记录器。 | ​**局部特定**。仅影响通过 `name`属性指定的包或类及其子包/子类。        |
| ​**配置目的**​ | 设置应用程序的**基础日志级别和输出策略**。                  | 为**特定模块**提供更精细的日志控制（如更详细或更严格的级别）。             |
| ​**继承性**​  | 是所有日志器层次的**根节点**，其他日志器默认继承其配置。           | 从其父日志器（可能是根或其他上层日志器）​**继承**未被自身覆盖的设置。         |
| ​**常用级别**​ | 生产环境常设为 `INFO`或 `WARN`；开发环境可设为 `DEBUG`。  | 根据需求灵活设置，如将关注模块设为 `DEBUG`，将嘈杂的第三方库设为 `ERROR`。 |

Logback 支持的日志级别从低到高依次为：
- `TRACE`：最细粒度的信息，通常只在开发过程中使用
- `DEBUG`：logger.debug信息
- `INFO`：logger.info 信息
- `WARN`：logger.warn 信息
- `ERROR`：logger.error 信息

### 高级特性与性能优化

Logback 提供了多种高级功能，可以满足复杂场景下的日志需求。

#### MDC（Mapped Diagnostic Context）

MDC 用于在日志中添加上下文信息（如请求 ID、用户 ID），非常适合分布式系统跟踪：

```java
// 在代码中使用MDC
import org.slf4j.MDC;

public class UserService {
    public void processUserRequest(String userId, String requestId) {
        // 将上下文信息放入MDC
        MDC.put("userId", userId);
        MDC.put("requestId", requestId);
        
        logger.info("Processing user request"); // 自动包含MDC信息
        
        // 业务处理...
        
        // 清理MDC
        MDC.clear();
    }
}
```

在配置中使用 MDC：

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{userId}] [%X{requestId}] %-5level %logger{36} - %msg%n</pattern>
```

#### 异步日志提升性能

对于生产环境，特别是高并发场景，使用异步日志可以显著提升性能

```xml
<!-- 异步日志（提升性能） -->
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="FILE"/>
    <!-- 队列大小 -->
    <queueSize>256</queueSize>
    <!-- 丢弃级别低于ERROR的日志（可选） -->
    <discardingThreshold>0</discardingThreshold>
</appender>
```


#### 条件化配置

Logback 支持根据不同的环境（如开发、测试、生产）使用不同的配置

```xml
<!-- 开发环境配置 -->
<springProfile name="dev">
    <root level="DEBUG">
        <appender-ref ref="CONSOLE"/>
    </root>
</springProfile>

<!-- 生产环境配置 -->
<springProfile name="prod">
    <root level="INFO">
        <appender-ref ref="ASYNC_FILE"/>
    </root>
</springProfile>
```

#### 自定义过滤器

Logback 允许创建自定义过滤器来实现复杂的日志过滤逻辑

```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
	...
    <!-- 只记录ERROR级别日志 -->
    <filter class="ch.qos.logback.classic.filter.LevelFilter">
        <level>ERROR</level>
        <onMatch>ACCEPT</onMatch>
        <onMismatch>DENY</onMismatch>
    </filter>
</appender>
```


这个 Logback 配置定义了一个名为 `FILE`的 `RollingFileAppender`（滚动文件输出器），并为其配置了一个 ​**LevelFilter（级别过滤器）​**。这个过滤器的设置使得该 Appender ​**只记录 `ERROR`级别的日志**，而拒绝所有其他级别的日志（如 DEBUG、INFO、WARN 等）。

| **配置项**               | ​**值/类型**     | **说明**                                                         |
| --------------------- | ------------- | -------------------------------------------------------------- |
| **`<filter class>`**​ | `LevelFilter` | 使用级别过滤器，根据日志事件的级别进行过滤。                                         |
| ​**`<level>`**​       | `ERROR`       | 设置过滤器的级别为 `ERROR`。                                             |
| ​**`<onMatch>`**​     | `ACCEPT`      | ​**当日志事件的级别与过滤器设置的级别（`ERROR`）匹配时，接受（ACCEPT）该日志事件**，允许其被输出。     |
| ​**`<onMismatch>`**​  | `DENY`        | ​**当日志事件的级别与过滤器设置的级别不匹配时，拒绝（DENY）该日志事件**，该日志将不会被此 Appender 输出。 |
