---
title: 相关注解
category: 系统设计
tag:
  - SpringBoot
---



## @SpringBootApplication

@SpringBootApplication看作是 @Configuration、@EnableAutoConfiguration、@ComponentScan 注解的集合。

- @Configuration：允许注册额外的 bean 或导入其他配置类

- @EnableAutoConfiguration：启用 SpringBoot 的自动配置机制

- @ComponentScan：扫描被@Component (@Repository,@Service,@Controller)注解的 bean，注解默认会扫描该类所在的包下所有的类。

[自动配置机制](https://www.seven97.top/framework/springboot/principleofautomaticassembly.html)？在启动过程中，Spring Boot会扫描应用程序的类路径，根据定义条件匹配的各种AutoConfiguration类进行自动配置。通过条件化配置和默认属性值，根据应用程序的类路径和其他条件来自动配置Spring应用程序的各种组件。自动配置的目标是让开发人员能够以最小的配置和干预自动获得功能完整的应用程序。

## Spring Bean 相关

### @Autowired

自动导入对象到类中，被注入进的类同样要被 Spring 容器管理比如：Service 类注入到 Controller 类中。



### @Qualifier

当注入的依赖存在多个候选者，可以使用一些方法来筛选出唯一候选者



例：

创建接口Car，以及两个实现其接口的bean

```java
public interface Car {
}
 
@Component
public class RedCar implements Car {
}
 
@Component
public class WhiteCar implements Car {
}
```

创建类型为Person的bean

```java
@Component
public class Person {
 
    @Autowired
    private Car car;
}
```



@Qualifier注解源码

![image-20240515001758389](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405150018597.png)

从源码中我们知道这个注解可以作用于属性、方法、参数、类、注解上面



作用于属性上

```java
@Component
public class Person {
 
    @Autowired
    @Qualifier("redCar")
    private Car car;
 
}
```



作用于方法上

```java
@Component
public class Person {
 
    @Autowired
    @Qualifier("redCar")
    private Car car;
 
    @Autowired
    @Qualifier("mimi")
    private Animal animal;
 
}
```



作用于类上

```java
@Component
@Qualifier("car666")
public class RedCar implements Car {
}
 
@Component
public class Person {
 
    @Autowired
    @Qualifier("car666")
    private Car car;
 
    @Autowired
    @Qualifier("mimi")
    private Animal animal;
 
}
```



作用于参数上

```java
@Component
public class Person {
 
    @Autowired
    @Qualifier("car666")
    private Car car;
 
    private Animal animal;
 
    public Person(@Qualifier("wangcai") Animal animal) {
        this.animal = animal;
    }
}
```







### @Component,@Repository,@Service, @Controller

一般使用 @Autowired 注解让 Spring 容器自动装配 bean。

但要想把类标识成可用于 @Autowired 注解自动装配的 bean 的类,可以采用以下注解实现：

- @Component：通用的注解，可标注任意类为 Spring 组件。如果一个 Bean 不知道属于哪个层，可以使用@Component 注解标注。

- @Repository : 对应持久层即 Dao 层，主要用于数据库相关操作。

- @Service : 对应服务层，主要涉及一些复杂的逻辑，需要用到 Dao 层。

- @Controller : 对应 Spring MVC 控制层，主要用于接受用户请求并调用 Service 层返回数据给前端页面。

### @RestController

@RestController注解是@Controller和@ResponseBody的合集,表示这是个控制器 bean,并且是将函数的返回值直接填入 HTTP 响应体中,是 REST 风格的控制器。

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

## 常见的 HTTP 请求类型

5 种常见的请求类型:

- GET：请求从服务器获取特定资源。举个例子：GET /users（获取所有学生）

- @GetMapping("users") 等价于@RequestMapping(value="/users",method=RequestMethod.GET)

- POST：在服务器上创建一个新的资源。举个例子：POST /users（创建学生）

- @PostMapping("users") 等价于@RequestMapping(value="/users",method=RequestMethod.POST)

- PUT：更新服务器上的资源（客户端提供更新后的整个资源）。举个例子：PUT /users/12（更新编号为 12 的学生）

- @PutMapping("/users/{userId}") 等价于@RequestMapping(value="/users/{userId}",method=RequestMethod.PUT)

- DELETE：从服务器删除特定的资源。举个例子：DELETE /users/12（删除编号为 12 的学生）

- @DeleteMapping("/users/{userId}")等价于@RequestMapping(value="/users/{userId}",method=RequestMethod.DELETE)

- PATCH：更新服务器上的资源（客户端提供更改的属性，可以看做作是部分更新），使用的比较少

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



## 读取配置信息

### @Value

使用 @Value("${property}") 读取比较简单的配置信息：

### @ConfigurationProperties(常用)

通过@ConfigurationProperties读取配置信息并与 bean 绑定。

### @PropertySource（不常用）

@PropertySource读取指定 properties 文件

## 参数校验

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

- @Digits(integer, fraction)被注释的元素必须是一个数字，其值必须在可接受的范围内

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



## 验证请求参数(Path Variables 和 Request Parameters)

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

- @ControllerAdvice :注解定义全局异常处理类

- @ExceptionHandler :注解声明异常处理方法

```java
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    /**
     * 请求参数异常处理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {
       ......
    }
}
```



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

@JsonFormat一般用来格式化 json 数据。

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

