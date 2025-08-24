---
title: 史上最全SpringBoot相关注解介绍
category: 系统设计
tags:
  - SpringBoot
---



## @SpringBootApplication

@SpringBootApplication看作是 @Configuration、@EnableAutoConfiguration、@ComponentScan 注解的集合。

- @Configuration：允许注册额外的 bean 或导入其他配置类

- @EnableAutoConfiguration：启用 SpringBoot 的自动配置机制

- @ComponentScan：扫描被@Component (@Repository,@Service,@Controller)注解的 bean，注解默认会扫描该类所在的包下所有的类。

[自动配置机制](https://www.seven97.top/framework/springboot/principleofautomaticassembly.html)？在启动过程中，Spring Boot会扫描应用程序的类路径，根据定义条件匹配的各种AutoConfiguration类进行自动配置。通过条件化配置和默认属性值，根据应用程序的类路径和其他条件来自动配置Spring应用程序的各种组件。自动配置的目标是让开发人员能够以最小的配置和干预自动获得功能完整的应用程序。



## Spring Bean 相关

### @Resource和@Autowired

- @Autowired：用于构造器、方法、参数或字段上，表明需要自动注入一个Bean。Spring会自动装配匹配的Bean，**默认是按类型装配**。
  - @Qualifier：与@Autowired一起使用时，指定要注入的Bean的名称，以避免与其他Bean的混淆。
  - @Primary: 当一个类型有多个Bean时，在不使用@Qualifier的情况下，Spring会优先选择标注了@Primary的Bean。

- @Resource：来自JDK，类似于@Autowired，但**默认是按名称装配**，也可以混合使用。
- @Inject：来自javax.inject包，类似于@Autowired，属于JSR-330标准的一部分。



#### 两者的区别

@Autowired 是Spring框架提供的注解，主要用于根据类型自动装配依赖项。行为和特性：

1. 按类型装配：默认情况下，@Autowired**按类型自动装配**Bean。
2. 可选依赖：如果你的依赖是可选的，可以使用 required=false 设置：
3. 构造器、方法或字段：可以用在构造器，属性字段或Setter方法上。
4. **结合@Qualifier**：可以和@Qualifier结合使用以实现按名称装配。
5. 作为Spring特有的注解，它更深度地集成在Spring的生态系统中，更适合与其他Spring注解一起使用。



@Resource 是JDK提供的注解，属于Java依赖注入规范（JSR-250）的一部分。行为和特性：

1. 按名称装配：默认情况下，@Resource**按名称装配**。如果没有匹配到名称，再按类型装配。
2. 不支持required属性：与@Autowired不同，@Resource不支持required属性。
3. 可以用于字段和Setter方法：虽然也可以用于构造器，但不常见。通常用在字段或Setter方法上。
4. 由于是Java EE规范的一部分，它可以与其他Java EE注解（如@PostConstruct和@PreDestroy）更好地配合使用。



#### 依赖注入的优先级(重要)

在使用 @Resource 注解进行依赖注入时，优先级规则如下：

1. 明确指定名称：
   1. 如果通过 @Resource(name="beanName") 明确指定了 Bean 的名称，那么 Spring 会首先按照名称匹配进行注入。
   2. 在这种情况下，@Primary 注解不会影响注入结果。
2. 按字段或属性名称匹配：
   1. 如果没有通过 name 属性指定 Bean 的名称，Spring 会尝试按照字段或属性的名称进行匹配。
   2. 在这种情况下，@Primary 注解也不会影响注入结果。
3. 按类型匹配：
   1. 如果按名称匹配失败（包括明确指定名称和按字段名称匹配都没有找到合适的 Bean），Spring 会按类型匹配。
   2. 在这种情况下，如果存在多个同类型的 Bean，则 @Primary 注解会起作用，标记为 @Primary 的 Bean 将被优先注入。



在使用 @Autowired 注解进行依赖注入时，优先级规则如下：

1. 按类型匹配：Spring 首先通过类型匹配找到所有符合要求的候选 Bean。如果只有一个候选 Bean，那么该 Bean 会被注入。
2. 按名称匹配结合 @Qualifier：
   1. 如果有多个同类型的 Bean，可以使用 @Qualifier 注解来指定具体的 Bean。
   2. @Qualifier 的值必须与一个候选 Bean 的名称匹配，匹配成功的 Bean 会被注入。
   3. 使用 @Primary：如果仍存在多个符合要求的 Bean，并且其中一个 Bean 标记了 @Primary，Spring 会优先选择标记了 @Primary 的 Bean 进行注入。
3. 按名称匹配字段或属性名称：在没有使用 @Qualifier 时，如果存在多个候选 Bean，Spring 会尝试通过字段或属性名称进行匹配。如果找到名称匹配的 Bean，则该 Bean 会被注入。
4. NoUniqueBeanDefinitionException：如果存在多个候选Bean，但没有使用@Qualifier指定名称，且没有标记@Primary，会抛出NoUniqueBeanDefinitionException，表明有多个Bean类型匹配但无法确定注入哪个。



> 在使用 @Bean 方法中的参数 进行依赖注入时，默认的行为与 @Autowired 注解的工作方式是一致的。



### @Component,@Repository,@Service, @Controller

一般使用 @Autowired 注解让 Spring 容器自动装配 bean。

但要想把类标识成可用于 @Autowired 注解自动装配的 bean 的类,可以采用以下注解实现：

- @Component：通用的注解，可标注任意类为 Spring 组件。如果一个 Bean 不知道属于哪个层，可以使用@Component 注解标注。

- @Repository : 对应持久层即 Dao 层，主要用于数据库相关操作。

- @Service : 对应服务层，主要涉及一些复杂的逻辑，需要用到 Dao 层。

- @Controller : 对应 Spring MVC 控制层，主要用于接受用户请求并调用 Service 层返回数据给前端页面。



### @RestController

@RestController注解**是@Controller和@ResponseBody的合集**，表示这是个控制器 bean,并且是将函数的返回值直接填入 HTTP 响应体中，是 REST 风格的控制器，更加适合目前前后端分离的架构下，返回 JSON 数据格式。

单独使用 @Controller 不加 @ResponseBody的话一般是用在要返回一个视图的情况，这种情况属于比较传统的 Spring MVC 的应用，对应于前后端不分离的情况。@Controller +@ResponseBody 返回 JSON 或 XML 形式数据



### @Scope

声明 Spring Bean 的作用域

常见的 Spring Bean 的作用域：

- singleton : 唯一 bean 实例，Spring 中的 bean 默认都是单例的。

- prototype : 每次请求都会创建一个新的 bean 实例。

- request : 每一次 HTTP 请求都会产生一个新的 bean，该 bean 仅在当前 HTTP request 内有效。

- session : 每一个 HTTP Session 会产生一个新的 bean，该 bean 仅在当前 HTTP session 内有效



### @Configuration

用来声明配置类，可以使用 @Component注解替代，不过使用@Configuration注解声明配置类更加语义化。



## 常见的 HTTP 请求

5 种常见的请求类型:

- GET：请求从服务器获取特定资源。举个例子：GET /users（获取所有学生）

  @GetMapping("users") 等价于@RequestMapping(value="/users",method=RequestMethod.GET)

- POST：在服务器上创建一个新的资源。举个例子：POST /users（创建学生）

  @PostMapping("users") 等价于@RequestMapping(value="/users",method=RequestMethod.POST)

- PUT：更新服务器上的资源（客户端提供更新后的整个资源）。举个例子：PUT /users/12（更新编号为 12 的学生）

  @PutMapping("/users/{userId}") 等价于@RequestMapping(value="/users/{userId}",method=RequestMethod.PUT)

- DELETE：从服务器删除特定的资源。举个例子：DELETE /users/12（删除编号为 12 的学生）

  @DeleteMapping("/users/{userId}")等价于@RequestMapping(value="/users/{userId}",method=RequestMethod.DELETE)

- PATCH：更新服务器上的资源（客户端提供更改的属性，可以看做作是部分更新），使用的比较少



### @RequestMapping 和 @GetMapping 注解的区别

@RequestMapping：可注解在类和方法上；@GetMapping 仅可注册在方法上

@RequestMapping：可进行 GET、POST、PUT、DELETE 等请求方法；@GetMapping 是@RequestMapping 的GET请求方法的特例


### @RequestHeader 注解

@RequestHeader 注解用于提取 HTTP请求头中的值，并将其注入到控制器方法的参数中。例如访间 Accept、Content-Type、User-Agent 等请求头信息

```java
@GetMapping("/header-info")
public String getHeaderInfo(@RequestHeader("User-Agent") String userAgent) {
    // 使用 userAgent 进行业务处理
    return "headerInfoView";
}
```

### @CookieValue 注解

@CookieValue 注解用于从 HTTP 请求的 Cookie 中提取值，并将其注入到控制器方法的参数中。

```java
@GetMapping("/cookie-info")
public String getCookieInfo(@CookieValue("sessionId") String sessionId) {
    // 使用 sessionId 进行业务处理
    return "cookieInfoView";
}
```

### @SessionAttribute 
@SessionAttribute 是 Spring MVC中的注解，用于从当前 HTTP会话(Session)中获取属性值并将其绑定到控制器方法的参数上，而无需手动从 Httpsession 获取

```java
@GetMapping("/profile")
public String getUserProfile(@SessionAttribute("loggedInUser") User user) {
    return "User Profile: " + user.getName();
}
```
在这个例子中，Spring 从会话中提取名为"loggedInUser”的属性值，并将其绑定到 user 对象中，传递给控制器方法。

## 前后端传值

### @PathVariable 和 @RequestParam

@PathVariable用于获取路径参数，@RequestParam用于获取查询参数。

```java
@GetMapping("/klasses/{klassId}/teachers")
public List<Teacher> getKlassRelatedTeachers(
         @PathVariable("klassId") Long klassId,
         @RequestParam(value = "type", required = false) String type ) {
...
}
```

如果请求的 url 是：/klasses/123456/teachers?type=web，那么后端获取到的数据就是：klassId=123456,type=web

### @RequestBody

用于读取 Request 请求（可能是 POST,PUT,DELETE,GET 请求）的 body 部分并且Content-Type 为 application/json 格式的数据，接收到数据之后会自动将数据绑定到 Java 对象上去。系统会使用HttpMessageConverter或者自定义的HttpMessageConverter将请求的 body 中的 json 字符串转换为 java 对象。

```java
@PostMapping("/sign-up")
public ResponseEntity signUp(@RequestBody @Valid UserRegisterRequest userRegisterRequest) {
  userService.save(userRegisterRequest);
  return ResponseEntity.ok().build();
}
```



![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407251044267.png)


### @ResponseBody
将控制器方法的返回结果直接写入 HTTP 响应体中。通常用于返回 JSON 或 XML 格式的数据，而不是视图页面。Spring 会将返回的 Java对象转换为 JSON 或 XML 格式，并写入响应体。

```java
@RestController
public class UserController {

    @GetMapping("/users/{id}")
    @ResponseBody
    public User getUser(@PathVariable Long id) {
        // 模拟从数据库中获取用户数据
        return new User(id, "mianshiya", 18);
    }
}
```

## 读取配置信息

### @Value

使用 @Value("${property}") 读取比较简单的配置信息：

### @ConfigurationProperties(常用)

通过@ConfigurationProperties读取配置信息并与 bean 绑定。

### @PropertySource（不常用）

@PropertySource读取指定 properties 文件



## 参数校验

详情可以看 [优雅的参数校验](https://www.seven97.top/system-design/springboot/parameterverification.html)

### 字段验证的注解

- @NotEmpty 被注释的字符串的不能为 null 也不能为空

- @NotBlank 被注释的字符串非 null，并且必须包含一个非空白字符

- @Null 被注释的元素必须为 null

- @NotNull 被注释的元素必须不为 null

- @AssertTrue 被注释的元素必须为 true

- @AssertFalse 被注释的元素必须为 false

- @Pattern(regex=,flag=)被注释的元素必须符合指定的正则表达式

- @Email 被注释的元素必须是 Email 格式。

- @Min(value)被注释的元素必须是一个数字，其值必须大于等于指定的最小值

- @Max(value)被注释的元素必须是一个数字，其值必须小于等于指定的最大值

- @DecimalMin(value)被注释的元素必须是一个数字，其值必须大于等于指定的最小值

- @DecimalMax(value) 被注释的元素必须是一个数字，其值必须小于等于指定的最大值

- @Size(max=, min=)被注释的元素的大小必须在指定的范围内

- @Digits(integer, fraction) 验证字符串是否是符合指定格式的数字，interger指定整数精度，fraction指定小数精度

- @Past被注释的元素必须是一个过去的日期@Future 被注释的元素必须是一个将来的日期

- .....

### 验证请求体(RequestBody)

需要验证的参数上加上了@Valid注解，如果验证失败，它将抛出MethodArgumentNotValidException。

```java
@RestController
@RequestMapping("/api")
public class PersonController {

    @PostMapping("/person")
    public ResponseEntity<Person> getPerson(@RequestBody @Valid Person person) {
        return ResponseEntity.ok().body(person);
    }
}
```



### 验证请求参数(Path Variables 和 Request Parameters)

要在类上加上 @Validated 注解，这个参数可以告诉 Spring 去校验方法参数。

```java
@RestController
@RequestMapping("/api")
@Validated
public class PersonController {

    @GetMapping("/person/{id}")
    public ResponseEntity<Integer> getPersonByID(@Valid @PathVariable("id") @Max(value = 5,message = "超过 id 的范围了") Integer id) {
        return ResponseEntity.ok().body(id);
    }
}
```





## 全局处理 Controller 层异常

- 使用系统定义好的异常处理器 SimpleMappingExceptionResolver

- 使用自定义异常处理器

- 使用异常处理注解

一般推荐使用注解的方式统一异常处理，具体会使用到 `@ControllerAdvice` + `@ExceptionHandler` 这两个注解 。

```java
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<?> handleAppException(BaseException ex, HttpServletRequest request) {
      //......
    }

    @ExceptionHandler(value = ResourceNotFoundException.class)
    public ResponseEntity<ErrorReponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
      //......
    }
}
```

这种异常处理方式下，会给所有或者指定的 `Controller` 织入异常处理的逻辑（AOP），当 `Controller` 中的方法抛出异常的时候，由被`@ExceptionHandler` 注解修饰的方法进行处理。

`ExceptionHandlerMethodResolver` 中 `getMappedMethod` 方法决定了异常具体被哪个被 `@ExceptionHandler` 注解修饰的方法处理异常。

```java
@Nullable
  private Method getMappedMethod(Class<? extends Throwable> exceptionType) {
    List<Class<? extends Throwable>> matches = new ArrayList<>();
    //找到可以处理的所有异常信息。mappedMethods 中存放了异常和处理异常的方法的对应关系
    for (Class<? extends Throwable> mappedException : this.mappedMethods.keySet()) {
      if (mappedException.isAssignableFrom(exceptionType)) {
        matches.add(mappedException);
      }
    }
    // 不为空说明有方法处理异常
    if (!matches.isEmpty()) {
      // 按照匹配程度从小到大排序
      matches.sort(new ExceptionDepthComparator(exceptionType));
      // 返回处理异常的方法
      return this.mappedMethods.get(matches.get(0));
    }
    else {
      return null;
    }
}
```

从源代码看出：**`getMappedMethod()`会首先找到可以匹配处理异常的所有方法信息，然后对其进行从小到大的排序，最后取最小的那一个匹配的方法(即匹配度最高的那个)。**





## 事务 @Transactional

在要开启事务的方法上使用@Transactional注解

```java
@Transactional(rollbackFor = Exception.class)
public void save() {
  ......
}
```

Exception 分为运行时异常 RuntimeException 和非运行时异常。在@Transactional注解中如果不配置rollbackFor属性,那么事务只会在遇到RuntimeException的时候才会回滚,加上rollbackFor=Exception.class,可以让事务在遇到非运行时异常时也回滚。

@Transactional 注解一般可以作用在类或者方法上。

- 作用于类：表示所有该类的 public 方法都配置相同的事务属性信息。

- 作用于方法：当类配置了@Transactional，方法也配置了@Transactional，方法的事务会覆盖类的事务配置信息。

## json 数据处理

### 过滤 json 数据

- @JsonIgnoreProperties 作用在类上用于过滤掉特定字段不返回或者不解析。

- @JsonIgnore一般用于类的属性上，用于过滤掉特定字段不返回或者不解析。

### 格式化 json 数据

@JsonFormat 一般用来格式化 json 数据。

### 扁平化对象

```java
@Getter
@Setter
@ToString
public class Account {
    private Location location;
    private PersonInfo personInfo;

  @Getter
  @Setter
  @ToString
  public static class Location {
     private String provinceName;
     private String countyName;
  }
  @Getter
  @Setter
  @ToString
  public static class PersonInfo {
    private String userName;
    private String fullName;
  }
}
```

<!-- @include: @article-footer.snippet.md -->     