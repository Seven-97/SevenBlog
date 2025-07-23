---
title: Spring AI 框架中如何集成 MCP？
category: 系统设计
tag:
  - SpringAI
  - LLM
  - MCP
---

## SpringAI MCP介绍

Spring AI MCP 为模型上下文协议提供 Java 和Spring 框架集成、它使 SpringAI 应用程序能够通过标准化的接口与不同的数据源和工是进行交互，支持同步和异步通信模式。整体架构如下:

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202507192156820.png)


Spring Al 通过以下 Spring Boot 启动器提供 MCP 集成：

客户端启动器
- spring-ai-starter-mcp-client 核心启动器提供 STDIO 和基于 HTTP 的 SSE 支持。
- spring-ai-starter-mcp-client-webflux 基于WebFlux的SSE流式传输实现

服务端启动器
- spring-ai-starter-mcp-server 核心服务器具有 STDIO 传输支持
- spring-ai-starter-mcp-server-webmvc 基于Spring MVC的SSE流式传输实现
- spring-ai-starter-mcp-server-webflux 基于WebFlux的SSE流式传输实现

## 基于stdio标准流
### MCP 服务端

基于 stdio 的实现是最常见的 MCP客户端方案，它通过标准输入输出流与 MCP 服务器进行通信，这种方式简单直观，能够直接通过进程间通信实现数据交互，避免了额外的网络通信开销，特别适用于本地部署的MCP服务器，可以在司一台机器上启动 MCP 服务器进程，与客户端无缝对接。

**引入依赖**

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-mcp-server-spring-boot-starter</artifactId>
  <version>1.0.0-M6</version>
</dependency>
```


**配置MCP服务端**

```yaml
spring:
  application:
    name: mcp-server
  main:
    web-application-type: none # 必须禁用web应用类型
    banner-mode: off # 禁用banner
  ai:
    mcp:
      server:
        stdio: true # 启用stdio模式
        name: mcp-server # 服务器名称
        version: 0.0.1 # 服务器版本
```

**实现MCP工具**

@Tool 是 SpingAI MCP框架中用于快速暴露业务能力为AI 工具的核心注解，该注解实现Java方法与MCP协议工具的自动银蛇，并且可以通过注解的属性description，有助于人工智能模型根据用户输入的信息决定是否调用这些工具，并返回相应的结果.

```java
@Service
public class OpenMeteoService {

    @Tool(description = "根据经纬度获取天气预报")
    public String getAirQuality(
        @ToolParameter(description = "纬度，例如：39.9042") String latitude,
        @ToolParameter(description = "经度，例如：116.4074") String longitude) {

        // 模拟数据，实际应用中应调用真实API
        return "当前位置（纬度：" + latitude + "，经度：" + longitude + "）的天气信息：\n 多云转阴";
    }
}
```

这个工具方法主要是用来根据经纬度获取天气预报的，这里为了方便演示，写了模拟数据

**注册MCP工具**

最后向 MCP 服务注册刚刚写的工具：

```java
    @Bean
    public ToolCallbackProvider serverTools(OpenMeteoService openMeteoService) {
        return MethodToolCallbackProvider.builder().toolObjects(openMeteoService).build();
    }
```

这段代码定义了一个 Spring 的 Bean，用于将查询天气服务 OpenMeteoService 中所有用 @Tool 注解标记的方法注册为工具，供 AI 模型调用。

ToolCallbackProvider 是Spring Al 中的一个接口，用于定义工具发现机制，主要负责将那些使用
@Tool 注解标记的方法转换为工具回调对象，并提供给 ChatClient 或ChatModel 使用，以便 AI 模型能够在对话过程中调用这些工具。

### MCP 客户端
**引入依赖**

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-mcp-client-spring-boot-starter</artifactId>
  <version>1.0.0-M6</version>
</dependency>
```

**配置MCP服务器**

因为服务端是通过 stdio 实现的，需要在 application.yml 中配置MCP服务器的一些参数：

```yaml
spring:
  ai:
    mcp:
      client:
        stdio:
          # 指定MCP服务器配置文件
          servers-configuration: classpath:/mcp-servers-config.json
  mandatory-file-encoding: UTF-8
```

其中 mcp-servers-config.json 的配置如下：

```json
{
  "mcpServers": {
    "weatherServer": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.stdio=true",
        "-Dspring.main.web-application-type=none",
        "-Dlogging.pattern.console=",
        "-jar",
        "/Users/gulihua/Documents/mcp-server/target/mcp-server-0.0.1-SNAPSHOT.jar"
      ],
      "env": {}
    }
  }
}
```

这个配置文件设置了MCP客户端的基本配置，包括 Java 命令参数，服务端 jar 包的绝对路径等，上述的 JSON 配置文件也可以直接写在 apllication.yaml 里，效果是一样的。

```yaml
    mcp:
      client:
        stdio:
         connections:
           server1:
             command: java
             args:
               - -Dspring.ai.mcp.server.stdio=true
               - -Dspring.main.web-application-type=none
               - -Dlogging.pattern.console=
               - -jar
               - /Users/gulihua/Documents/mcp-server/target/mcp-server-0.0.1-SNAPSHOT.jar
```

客户端我们使用问里巴巴的通义千问模型，所以引入 spring-ai-alibaba-starter 依赖，如果使用的是其他的模型，也可以使用对应的依赖项，比加 openAI 引入  sprine-ai-openai-spring-boot-starter 这个依赖就行了

配置大模型的密钥等信息：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${通义千问的key}
      chat:
        options:
          model: qwen-max
```


**初始化聊天客户端**

```java
@Bean
public ChatClient initChatClient(ChatClient.Builder chatClientBuilder,
                                 ToolCallbackProvider mcpTools) {
    return chatClientBuilder
    .defaultTools(mcpTools)
    .build();
}
```

该代码定义了一个 spring pean，用于初始化一个AI聊天客户端，里面有两个参数，chatcient.Buinider 是 SpnngAI 提供的AI聊天客户端构建器，用于构建 ChatCient实例，是由 Spring AI 自动注入的，另一个是 ToolCallbackProvider，用于从MCP客服端发现并获取AI工具。

然后就可以通过这个 chatclient 去调用了：

```java
chatClient.prompt()
.user(request.getContent())
.call()
.content();
```

## 基于SSE
### MCP服务端

除了基于 stdio 的实现外，Spring Al还提供了基于 Server-Sent vents(SSE)的 MCP客户端方案。相较于 stdio方式，SSE 更适用于远程部署的 MCP 服务器，客户端可以通过标准 HTTP 协议与服务器建立连接，实现单向的实时数据推送。基于 SSE的 MCP 服务器支持被多个客户端的远程调用。

**引入依赖**

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-mcp-server-webflux-spring-boot-starter</artifactId>
  <version>1.0.0-M6</version>
</dependency>
```

**配置MCP服务端**

```yaml
server:
  port: 8090

spring:
  application:
    name: mcp-server
  ai:
    mcp:
      server:
        name: mcp-server # MCP服务器名称
        version: 0.0.1   # 服务器版本号
```

除了引入的依赖包不一样，以及配置文件不同，其他的不需要修改。

### MCP 客户端

**引入依赖**

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-mcp-client-webflux-spring-boot-starter</artifactId>
  <version>1.0.0-M6</version>
</dependency>
```

**配置MCP服务器**

因为服务端是通过SSE实现的，需要在 application.yml 中配置MCP服务器的URL端口：

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        name: mcp-client
        version: 1.0.0
        request-timeout: 30s
        type: ASYNC # 类型同步或者异步
        sse:
          connections:
            server1:
              url: http://localhost:8090
```

和MCP服务端的修改一样，除了依赖和配置的修改，其他的也不需要调整

## 注意

除了上面基础的用法和配置，还应该考虑以下几个方面:

- 工具设计
	- 每个工具方法应具备明确的功能定义及参数说明。
	- 使用 @Tool 注解提供清晰、完整的工具描述，便于自动生成文档或展示给前端。
	- 使用 @ToolParameter 注解详细说明每个参数的用途，提升使用者的理解与正确性。
- 错误处理
	- 应全面捕获并妥善处理可能出现的异常，防止服务崩溃。
	- 返回结构化、具备可读性的错误信息，便于客户端识别错误原因并进行相应处理。
- 性能优化
	- 对于可能耗时的任务，建议使用异步处理机制，避免阻塞主线程，
	- 设置合理的超时时间，防止客户端长时间等待，提高系统响应性和稳定性。
- 安全性考虑
	- 对涉及敏感资源或关键操作的工具方法，应添加严格的权限校验逻辑
	- 禁止在工具方法中执行高风险操作(如执行任意系统命令)，以防止安全洞。
- 部署策略
	- Stdio 模式：适用于嵌入式场景，可作为客户端的子进程运行，便于集成与资源控制。
	- SSE模式：更适合部署为独立服务，支持多个客户端同时访问，适用于需要持续通信的远程调用场景。






