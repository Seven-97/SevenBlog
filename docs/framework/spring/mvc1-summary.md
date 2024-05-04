---
title: MVC - 概述
category: 常用框架
tag:
  - SpringMVC
---



## 常用组件

- 前端控制器（DispatcherServlet）：接收用户请求，给用户返回结果。

- 处理器映射器（HandlerMapping）：根据请求的url路径，通过注解或者xml配置，寻找匹配的Handler。

- 处理器适配器（HandlerAdapter）：Handler 的适配器，调用 handler 的方法处理请求。

- 处理器（Handler）：执行相关的请求处理逻辑，并返回相应的数据和视图信息，将其封装到ModelAndView对象中。

- 视图解析器（ViewResolver）：将逻辑视图名解析成真正的视图View。

- 视图（View）：接口类，实现类可支持不同的View类型（JSP、FreeMarker、Excel等）



## MVC的常用注解

- @Controller：用于标识此类的实例是一个控制器。

- @RequestMapping：映射Web请求（访问路径和参数）。

- @ResponseBody：注解返回数据而不是返回页面

- @RequestBody：注解实现接收 http 请求的 json 数据，将 json 数据转换为 java 对象。

- @PathVariable：获得URL中路径变量中的值

- @RestController：@Controller+@ResponseBody

- @ExceptionHandler标识一个方法为全局异常处理的方法。

 

### @RestController 和 @Controller 的区别

@RestController 注解，在 @Controller 基础上，增加了 @ResponseBody 注解，更加适合目前前后端分离的架构下，提供 Restful API ，返回 JSON 数据格式。

 

### @RequestMapping 和 @GetMapping 注解的区别

@RequestMapping：可注解在类和方法上；@GetMapping 仅可注册在方法上

@RequestMapping：可进行 GET、POST、PUT、DELETE 等请求方法；@GetMapping 是@RequestMapping 的GET请求方法的特例

 

### @RequestParam 和 @PathVariable 的区别

两个注解都用于方法参数，获取参数值的方式不同：@RequestParam 注解的参数从请求携带的参数中获取，而 @PathVariable 注解从请求的 URI 中获取

 

### @RequestBody和@RequestParam的区别

@RequestBody一般处理的是在ajax请求中声明contentType: "application/json; charset=utf-8"时候。也就是json数据或者xml数据。

@RequestParam一般就是在ajax里面没有声明contentType的时候，为默认的x-www-form-urlencoded格式时。

 

## 异常处理

- 使用系统定义好的异常处理器 SimpleMappingExceptionResolver

- 使用自定义异常处理器

- 使用异常处理注解

 

## MVC 拦截器

Spring MVC 拦截器对应HandlerInterctor接口，该接口位于org.springframework.web.servlet的包中，定义了三个方法，若要实现该接口，就要实现其三个方法：

1. 前置处理（preHandle()方法）：该方法在执行控制器方法之前执行。返回值为Boolean类型，如果返回false，表示拦截请求，不再向下执行，如果返回true，表示放行，程序继续向下执行（如果后面没有其他Interceptor，就会执行controller方法）。所以此方法可对请求进行判断，决定程序是否继续执行，或者进行一些初始化操作及对请求进行预处理。
2. 后置处理（postHandle()方法）：该方法在执行控制器方法调用之后，且在返回ModelAndView之前执行。由于该方法会在DispatcherServlet进行返回视图渲染之前被调用，所以此方法多被用于处理返回的视图，可通过此方法对请求域中的模型和视图做进一步的修改。
3. 已完成处理（afterCompletion()方法）：该方法在执行完控制器之后执行，由于是在Controller方法执行完毕后执行该方法，所以该方法适合进行一些资源清理，记录日志信息等处理操作。

可以通过拦截器进行权限检验，参数校验，记录日志等操作



### MVC 的Interctor和 Filter 过滤器的区别

- 功能相同：Interctor和 Filter 都能实现相应的功能

- 容器不同：Interctor构建在 Spring MVC 体系中；Filter 构建在 Servlet 容器之上

- 拦截内容不同：Filter对所有访问进行增强，Interctor仅对MVC访问进行增强

- 使用便利性不同：Interctor提供了三个方法，分别在不同的时机执行；过滤器仅提供一个方法



### 多拦截器执行顺序

- 当配置多个拦截器时，会形成拦截器链

- 拦截器的运行顺序参照拦截器添加顺序为准，即addInterctor的顺序

- 当拦截器中出现对原始处理器的拦截，后面的拦截器均终止运行

- 当拦截器运行中断，仅运行配置在前面的拦截器afterCompletion

流程解析看下图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281539349.png)

 

 









