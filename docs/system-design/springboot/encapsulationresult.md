---
title: 优雅的处理返回值
category: 系统设计
tag:
  - SpringBoot
---





上一篇文章中我们已经知道了如何优雅的校验传入的参数了，那么后端服务器如何实现把数据返回给前端呢？

本文相关Demo 可以 [点击这里](https://github.com/Seven-97/SpringBoot-Demo/tree/master/03-controller-validation)，与上一篇在在同一个位置



## 返回格式

后端返回给前端我们一般用 JSON 体方式，定义如下：

```json
{
    #返回状态码
    code:string,       
    #返回信息描述
    message:string,
    #返回值
    data:object
}
```



### CODE 状态码

Code 返回状态码，一般是在开发的时候需要什么，就添加什么。

如接口要返回用户权限异常，我们加一个状态码为 101 吧，下一次又要加一个数据参数异常，就加一个 102 的状态码。这样虽然能够照常满足业务，但状态码太凌乱了。

这里可以参考 [阿里巴巴开发手册](https://www.seven97.top/books/software-quality/alibaba-developmentmanual.html) 中前后端规约 以及 异常日志中的内容

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409091036235.png)



```
U表示用户，后面4位数字编号为
#U1000～U1999 区间表示用户参数相关
#U2000～U2999 区间表示用户相关接口异常
...
```

这样前端开发人员在得到返回值后，根据状态码就可以知道，大概什么错误，再根据 Message 相关的信息描述，可以快速定位。



### Message

这个字段相对理解比较简单，就是发生错误时，如何友好的进行提示。一般的设计是和 Code 状态码一起设计，如：

```java
@Data
public class Result {

    private String retCode;
    private String retMsg;
 
    //默认成功状态码
    public static final String SUCCESSCODE = "S0000";
    public static final String SUCCESSMSG = "成功";
    //默认失败状态码
    public static final String ERROR_CODE = "E2222";
    public static final String ERROR_MSG = "失败";

    public static final String COMMENT_CODE = "E3333";
    public static final String RUNNING_ERROR_MSG = "运行出错，请联系管理员";

    private Result() {
        this(SUCCESSCODE, SUCCESSMSG);
    }
}
```



再在枚举中定义，状态码：

```java
@Getter
public enum UserResultConstants implements ResultConstats{

    NOT_ID("U1001","未正确输入用户id"),
    ADD_FAIL("U1002","新增用户失败"),
    UPDATE_FAIL("U1003","更新用户失败"),
    DELETE_FAIL("U1004","删除用户失败"),
    USERNAME_EXIST("U1005","用户名已存在"),
    USER_NOT_EXIST("U1006","用户不存在")
    ;

    private String code;
    private String message;

    private UserResultConstants(String code, String message){
        this.code = code;
        this.message = message;
    }

}
```

状态码和信息就会一一对应，比较好维护。



### Data

返回数据体，JSON 格式，根据不同的业务又不同的 JSON 体。

我们要设计一个返回体类 Result：

```java
@Data
public class Result {

    private String retCode;
    private String retMsg;
    private Object data;

    public static final String SUCCESSCODE = "0000";
    public static final String SUCCESSMSG = "成功";
    public static final String ERROR_CODE = "2222";
    public static final String ERROR_MSG = "失败";

    public static final String COMMENT_CODE = "3333";
    public static final String RUNNING_ERROR_MSG = "运行出错，请联系管理员";

    public Result() {
        this(SUCCESSCODE, SUCCESSMSG, null);
    }

    public Result(Object data) {
        this(SUCCESSCODE, SUCCESSMSG, data);
    }

    public Result(String retCode, String retMsg, Object data) {
        this.retCode = retCode;
        this.retMsg = retMsg;
        this.data = data;
    }

    public Result(String retCode, String retMsg) {
        this(retCode, retMsg, null);
    }
}
```



## 控制层 Controller

我们会在 Controller 层处理业务请求，并返回给前端，以 用户管理 为例：

```java
@RestController
@RequestMapping("/user")
@Slf4j
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 查询所有用户信息
     *
     * @return 返回所有用户信息
     */
	@GetMapping(value = "/list")
    public Result userList() {
        List<UserVo> users = userService.selectUserList();
        Result result = new Result(users);
        log.info("userList查询的用户信息为:{}", JsonMapper.toJson(users));
        return result;
    }
}
```

我们看到在获得 users 对象之后，我们是用的 Result 构造方法进行包装赋值，然后进行返回。而[@RestController注解](https://www.seven97.top/system-design/springboot/springboot-annotation.html)**是@Controller和@ResponseBody的合集**，表示这是个控制器 bean,并且是将函数的返回值直接填入 HTTP 响应体中，是 REST 风格的控制器，更加适合目前前后端分离的架构下，返回 JSON 数据格式。

有没有发现，构造方法这样的包装是不是很麻烦，可以优化一下。



### 美观优化

我们可以在 Result 类中，加入静态方法，一看就懂：

```java
@Data
public class Result {

    //...
    
    public static Result ok() {
        return new Result();
    }

    public static Result ok(Object data) {
        return new Result(data);
    }

    public static Result ok(String retCode, String retMsg) {
        return new Result(retCode, retMsg);
    }

    public static Result error(String retCode, String retMsg) {
        return new Result(retCode, retMsg);
    }

}
```



再改造一下 Controller：

```java
@RestController
@RequestMapping("/user")
@Slf4j
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 查询所有用户信息
     *
     * @return 返回所有用户信息
     */
    @GetMapping(value = "/list")
    public Result userList() {
        List<UserVo> users = userService.selectUserList();
        log.info("userList查询的用户信息为:{}", JsonMapper.toJson(users));
        return Result.ok(users);
    }
}
```

代码是不是比较简洁了，也美观了。



### 优雅优化

上面我们看到在 Result 类中增加了静态方法，使得业务处理代码简洁了。

但大家有没有发现这样有几个问题：

- 每个方法的返回都是 Result 封装对象，没有业务含义。
- 在业务代码中，成功的时候我们调用 `Result.success`，异常错误调用 `Result.error`。是不是很多余。

我们最好的方式直接返回真实业务对象，最好不要改变之前的业务方式，如下：

```java
@RestController
@RequestMapping("/user")
@Slf4j
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 查询所有用户信息
     *
     * @return 返回所有用户信息
     */
    @GetMapping(value = "/list")
    public List<UserVo> userList() {
        List<UserVo> users = userService.selectUserList();
        log.info("userList查询的用户信息为:{}", JsonMapper.toJson(users));
        return users;
    }
}
```

这个和我们平时的代码是一样的，非常直观，直接返回 user 对象，这样是不是很完美。

那实现方案是什么呢？



## 返回值优雅方案实现

要优化这段代码很简单，只需要借助SpringBoot提供的`ResponseBodyAdvice`即可。

>  ResponseBodyAdvice的作用：拦截Controller方法的返回值，统一处理返回值/响应体，一般用来统一返回格式，加解密，签名等等。



### 重写返回体

```java
@RestControllerAdvice
@RequiredArgsConstructor
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @SneakyThrows
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        //如果Controller直接返回String的话，SpringBoot是直接返回，故我们需要手动转换成json。
        if (body instanceof String) {
            return objectMapper.writeValueAsString(Result.ok(body));
        }
        //如果已经封装成Result了，则不需要再次封装，否则就会导致出现多层
        if (body instanceof Result) {
            return body;
        }
        return Result.ok(body);
    }
}
```

上面代码就是判断是否需要返回值包装，如果需要就直接包装。这里我们只处理了正常成功的包装，如果方法体报异常怎么办？



### 异常问题处理

此时有个问题，由于没对异常进行处理，当调用的方法一旦出现异常，就会出现问题，此时就可以写全局异常，来通过全局异常返回同一格式的Result。

- 定义异常类

```java
public class UserException extends RuntimeException {
    public UserException(String message) {
        super(message);
    }
}
```

- 全局异常类

```java
@RestControllerAdvice
@Slf4j
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(value = UserException.class)
    public Result handlerUserException(UserException e, HttpServletRequest request) {
        String msg = e.getMessage();
        log.error("请求[ {} ] {} 的参数校验发生错误，错误信息：{}", request.getMethod(), request.getRequestURL(), msg, e);
        return Result.error(Result.COMMENT_CODE, msg);
    }
}
```





### 重写 Controller

```java
@RestController
@RequestMapping("/user")
@Slf4j
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 根据用户id查询用户信息
     *
     * @param id 用户id
     * @return 成功则返回用户信息
     */
    @GetMapping("/selectById")
    public UserVo selectById(@RequestParam(required = false) Integer id) {
        UserVo user = userService.selectUserById(id);
        log.info("selectById根据id:{}执行查询,查询的用户信息为:{}", id, JsonMapper.toJson(user));
        if (user == null) {
            throw new UserException("查询的用户不存在");
        }
        return user;
    }
}
```

到此返回的同一格式的设计思路完成，是不是又简洁，又优雅。











<!-- @include: @article-footer.snippet.md -->     