---
title: 优雅的参数校验
category: 系统设计
tag:
  - SpringBoot
---



## 前言

在日常的开发工作中，为了程序的健壮性，大部分方法都需要进行入参数据校验。最直接的当然是在相应方法内对数据进行手动校验，但是这样代码里就会有很多冗余繁琐的if-else。

比如如下的保存用户信息的方法：

```java
@RestController
public class TestController {

    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(^\\d{15}$)|(^\\d{18}$)|(^\\d{17}(\\d|X|x)$)");
    private static final Pattern MOBILE_PHONE_PATTERN = Pattern.compile("^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$");

    @RequestMapping(value = "/api/saveUser", method = RequestMethod.POST)
    public Result<Boolean> saveUser(UserRequest user) {
        if (StringUtils.isBlank(user.getUserName())) {
            throw new IllegalArgumentException("用户姓名不能为空");
        }
        if (Objects.isNull(user.getGender())) {
            throw new IllegalArgumentException("性别不能为空");
        }
        if (Objects.isNull(GenderType.getGenderType(user.getGender()))) {
            throw new IllegalArgumentException("性别错误");
        }
        if (Objects.isNull(user.getAge())) {
            throw new IllegalArgumentException("年龄不能为空");
        }
        if (user.getAge() < 0 || user.getAge() > 150) {
            throw new IllegalArgumentException("年龄必须在0-150之间");
        }
        if (StringUtils.isBlank(user.getIdCard())) {
            throw new IllegalArgumentException("身份证号不能为空");
        }
        if (!ID_CARD_PATTERN.matcher(user.getIdCard()).find()) {
            throw new IllegalArgumentException("身份证号格式错误");
        }
        if (StringUtils.isBlank(user.getMobilePhone())) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (!MOBILE_PHONE_PATTERN.matcher(user.getIdCard()).find()) {
            throw new IllegalArgumentException("手机号格式错误");
        }
        // 省略其他业务代码
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
```

上面的方法因为请求对象里的参数很多，所以就有了好多if/else。当然这么写没问题，但是：

1. 扩展性差：如果后续 UserRequest 里又新增了参数，那还得在方法实现里增加校验代码，参数校验和业务代码混在一起。
2. 可读性差：当参数校验过多时，代码会十分冗长，违背阿里巴巴代码规约。
3. 不易复用：其他比如更新用户信息的方法，可能也会有类似的参数校验。如果也手动校验的话，会存在很多重复代码。

那么该怎么优雅的进行参数校验呢？

## spring validation

Java在早在2009年就提出了 Bean Validation（JSR）规范，其中定义了一系列的校验注解，比如 @NotEmpty、@NotNull等，支持通过注解的方式对字段进行校验，避免在业务逻辑中耦合冗余的校验逻辑。

不过，以上注解本身不做校验，只是能给开发者做个提醒。如果需要达到参数校验的目的，还需要其他配置。

### Controller方法参数校验

相关Demo 可以 [点击这里](https://github.com/Seven-97/SpringBoot-Demo/tree/master/03-controller-validation)

#### 效果示例

Spring 提供了相应的 Bean Validation 实现：Java Bean Validation，并在 Spring MVC 中添加了自动校验，默认就会对 @Valid/@Validated 修饰的方法参数使用 Validator 来做校验逻辑。

举个栗子：

引入依赖：

```xml
<dependency>
       <groupId>javax.validation</groupId>
       <artifactId>validation-api</artifactId>
       <version>2.0.1.Final</version>
</dependency>
<dependency>
       <groupId>org.hibernate</groupId>
       <artifactId>hibernate-validator</artifactId>
       <version>6.1.3.Final</version>
</dependency>
```

> validation-api是一套标准（JSR-303），叫做Bean Validation，Hibernate Validator是Bean Validation的参考实现，提供了JSR-303 规范中所有内置constraint的实现，除此之外Hibernate Validator还附加了一些constraint。
>



- 在方法在入参对应元素上配置校验注解：

```java
@Data
public class UserRequest {
    @NotBlank(message = "用户ID不能为空")
    private String userId;
  
    @NotBlank(message = "电话号码不能为空")
    @Pattern(regexp = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$", message = "电话号码格式错误")
    private String mobilePhone;

    @Min(message = "年龄必须大于0", value = 0)
    @Max(message = "年龄不能超过150", value = 150)
    private Integer age;


    @NotNull(message = "用户详情不能为空")
    @Valid
    private UserDetail userDetail;
    
    //省略其他参数
}
```



- 在 Controller 相应方法中，使用 @Valid/@Validated 注解开启数据校验功能：

```java
@RestController
public class TestController {

    @RequestMapping(value = "/api/saveUser", method = RequestMethod.POST)
    public ResponseEntity<BaseResult> saveUser(@Validated @RequestBody UserRequest user) {
        // 省略其他业务代码
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
```



如果数据校验通过，就会继续执行方法里的业务逻辑；否则，就会抛出一个 MethodArgumentNotValidException 异常。默认情况下，Spring 会将该异常及其信息以错误码 400 进行下发，返回结果示例如下：

```json
{
  "timestamp": 1666777674977,
  "status": 400,
  "error": "Bad Request",
  "exception": "org.springframework.web.bind.MethodArgumentNotValidException",
  "errors": [
    {
      "codes": [
        "NotBlank.UserRequest.mobilePhone",
        "NotBlank.mobilePhone",
        "NotBlank.java.lang.String",
        "NotBlank"
      ],
      "arguments": [
        {
          "codes": [
            "UserRequest.mobilePhone",
            "mobilePhone"
          ],
          "arguments": null,
          "defaultMessage": "mobilePhone",
          "code": "mobilePhone"
        }
      ],
      "defaultMessage": "电话号码不能为空",
      "objectName": "UserRequest",
      "field": "mobilePhone",
      "rejectedValue": null,
      "bindingFailure": false,
      "code": "NotBlank"
    }
  ],
  "message": "Validation failed for object='UserRequest'. Error count: 1",
  "path": "/api/saveUser"
}
```



但是返回的异常结果不是需要的格式，所以再来个全局异常捕获器拦截该异常，就可以得到一个完美的异常结果：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResult handlerMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().get(0);
        return new BaseResult(CommonResultCode.ILLEGAL_PARAMETERS.getErrorCode(),
                "入参中的" + fieldError.getField() + fieldError.getDefaultMessage(), EagleEye.getTraceId());
    }

}
```

设置了如上捕获器后，如果数据校验不通过，返回的结果为：

```json
{
  "success": false,
  "errorCode": "ILLEGAL_PARAMETERS",
  "errorMessage": "入参中的mobilePhone电话号码不能为空",
  "traceId": "1ef9749316674663696111017d73c9",
  "extInfo": {}
}
```

借助Spring和约束注解，就非常简单明了、优雅地完成了方法参数校验。

而且，假如以后入参对象里新增了参数，只需要顺便添加一个注解，而不用去改业务代码了！﻿



#### @Valid 和 @Validated

- @Valid注解，是 Bean Validation 所定义，可以添加在普通方法、构造方法、方法参数、方法返回、成员变量上，表示它们需要进行约束校验。

- @Validated注解，是 Spring Validation 所定义，可以添加在类、方法参数、普通方法上，表示它们需要进行约束校验。

两者的区别在于 @Validated 有 value 属性，支持分组校验，即根据不同的分组采用不同的校验机制，**@Valid 可以添加在成员变量上，支持嵌套校验**。所以建议的使用方式就是：启动校验（即 Controller 层）时使用 @Validated 注解，嵌套校验时使用 @Valid 注解，这样就能同时使用分组校验和嵌套校验功能。

> 注意：单参数校验需要在类上添加 @Validated 注解



#### 分组校验

但是，对于同个参数，不同的场景可能需要不同的校验，这时候就可以用分组校验能力。

比如创建 User 时，userId为空；但是更新 User 时，userId值则不能为空。示例如下：

```java
@Data
public class UserRequest {
    @NotBlank(message = "用户ID不能为空", groups = {UpdateUser.class})
    private String userId;
  
    @NotBlank(message = "电话号码不能为空", groups = {UpdateUser.class, InsertUser.class})
    @Pattern(regexp = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$", message = "电话号码格式错误")
    private String mobilePhone;

    @Min(message = "年龄必须大于0", value = 0, groups = {UpdateUser.class, InsertUser.class})
    @Max(message = "年龄不能超过150", value = 150, groups = {UpdateUser.class, InsertUser.class})
    private Integer age;


    @NotNull(message = "用户详情不能为空", groups = {UpdateUser.class, InsertUser.class})
    @Valid
    private UserDetail userDetail;
    
    //省略其他参数
}
```

```java
@RestController
public class TestController {

    @RequestMapping(value = "/api/saveUser", method = RequestMethod.POST)
    public ResponseEntity<BaseResult> saveUser(@Validated(value = InsertUser.class) @RequestBody UserRequest user) {
        // 省略其他业务代码
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
```



#### 自定义校验注解

还有，如果现有的基础校验注解没法满足校验需求，那就可以使用自定义注解。由两部分组成：

- 由 @Constraint 注解的注解。

- 实现了 javax.validation.ConstraintValidator 的 validator。

两者通过 @Constraint 关联到一起。

假设有个性别枚举，需要校验用户的性别是否属于此范围内，示例如下：

```java
public interface BasicEnum {
    String getEnumCode();

    String getEnumDesc();
}
```



 ```java
 public enum GenderEnum implements BasicEnum {
     MALE("1", "男性"),
     FEMALE("2", "女性"),
     SECRET("0", "保密");
 
     private String code;
 
     private String desc;
     // 省略其他
 }
 ```



第一步，自定义约束注解 InEnum，可以参考 NotNull 的定义：

```java
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = InEnumValidator.class)
public @interface InEnum {
    /**
     * 枚举类型
     *
     * @return
     */
    Class<? extends BasicEnum> enumType();

    String message() default "枚举类型不匹配";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
```



第二步，自定义约束校验器 InEnumValidator。如果校验通过，返回 true；反之返回 false：

```java
public class InEnumValidator implements ConstraintValidator<InEnum, Object> {
    private Class<? extends BasicEnum> enumType;

    @Override
    public void initialize(InEnum inEnum) {
        enumType = inEnum.enumType();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        if (object == null) {
            return true;
        }

        if (enumType == null || !enumType.isEnum()) {
            return false;
        }

        for (BasicEnum basicEnum : enumType.getEnumConstants()) {
            if (basicEnum.getEnumCode().equals(object)) {
                return true;
            }
        }
        return false;
    }
}
```



第三步，参数上增加 @InEnum 注解校验：

```java
@Data
public class UserRequest {
    @InEnum(enumType = GenderEnum.class, message = "用户性别不在枚举范围内")
    private String gender;
    
    //省略其他参数
}
```



设置了如上校验后，如果数据校验不通过，返回的结果为：

```json
{
  "success": false,
  "errorCode": "ILLEGAL_PARAMETERS",
  "errorMessage": "入参中的gender用户性别不在枚举范围内",
  "traceId": "1ef9749316674663696111017d73c9",
  "extInfo": {}
}
```



#### 校验原理

1. MethodValidationPostProcessor是 Spring 提供的来实现基于方法的JSR校验的核心处理器，能让约束作用在方法入参、返回值上。关于校验方面的逻辑在切面MethodValidationInterceptor。

```java
public class MethodValidationPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor
    implements InitializingBean {

  private Class<? extends Annotation> validatedAnnotationType = Validated.class;

    // 这个是javax.validation.Validator
  private Validator validator;


  // 可以传入自定义注解
  public void setValidatedAnnotationType(Class<? extends Annotation> validatedAnnotationType) {
    Assert.notNull(validatedAnnotationType, "'validatedAnnotationType' must not be null");
    this.validatedAnnotationType = validatedAnnotationType;
  }

    // 默认情况下，使用LocalValidatorFactoryBean进行校验，也可以传入自定义的Validator
  public void setValidator(Validator validator) {
    // Unwrap to the native Validator with forExecutables support
    if (validator instanceof LocalValidatorFactoryBean) {
      this.validator = ((LocalValidatorFactoryBean) validator).getValidator();
    }
    else if (validator instanceof SpringValidatorAdapter) {
      this.validator = validator.unwrap(Validator.class);
    }
    else {
      this.validator = validator;
    }
  }

    // 可以传入自定义ValidatorFactory
  public void setValidatorFactory(ValidatorFactory validatorFactory) {
    this.validator = validatorFactory.getValidator();
  }


  @Override
  public void afterPropertiesSet() {
    Pointcut pointcut = new AnnotationMatchingPointcut(this.validatedAnnotationType, true);
    this.advisor = new DefaultPointcutAdvisor(pointcut, createMethodValidationAdvice(this.validator));
  }

    // 使用MethodValidationInterceptor切面
  protected Advice createMethodValidationAdvice(Validator validator) {
    return (validator != null ? new MethodValidationInterceptor(validator) : new MethodValidationInterceptor());
  }

}
```



2. MethodValidationInterceptor：用于处理方法的数据校验

```java
public class MethodValidationInterceptor implements MethodInterceptor {

  //xxx

  @Override
  @SuppressWarnings("unchecked")
  public Object invoke(MethodInvocation invocation) throws Throwable {
    // 如果是FactoryBean.getObject() 方法，就不要校验了
    if (isFactoryBeanMetadataMethod(invocation.getMethod())) {
      return invocation.proceed();
    }

    // 获取方法上@Validated注解里的分组信息
    Class<?>[] groups = determineValidationGroups(invocation);

    if (forExecutablesMethod != null) {
      // Standard Bean Validation 1.1 API
      Object execVal;
      try {
        execVal = ReflectionUtils.invokeMethod(forExecutablesMethod, this.validator);
      }
      catch (AbstractMethodError err) {
        Validator nativeValidator = this.validator.unwrap(Validator.class);
        execVal = ReflectionUtils.invokeMethod(forExecutablesMethod, nativeValidator);
        this.validator = nativeValidator;
      }

      Method methodToValidate = invocation.getMethod();
      Set<ConstraintViolation<?>> result;
            
      // 对方法的参数进行验证，验证结果保存在 result 中，如果验证失败，result 不为空，
      // 此时会抛出异常 ConstraintViolationException
      try {
        result = (Set<ConstraintViolation<?>>) ReflectionUtils.invokeMethod(validateParametersMethod,
            execVal, invocation.getThis(), methodToValidate, invocation.getArguments(), groups);
      }
      catch (IllegalArgumentException ex) {
        // Probably a generic type mismatch between interface and impl as reported in SPR-12237 / HV-1011
        // Let's try to find the bridged method on the implementation class...
        methodToValidate = BridgeMethodResolver.findBridgedMethod(
            ClassUtils.getMostSpecificMethod(invocation.getMethod(), invocation.getThis().getClass()));
        result = (Set<ConstraintViolation<?>>) ReflectionUtils.invokeMethod(validateParametersMethod,
            execVal, invocation.getThis(), methodToValidate, invocation.getArguments(), groups);
      }
      if (!result.isEmpty()) {
        throw new ConstraintViolationException(result);
      }

      // 调用目标方法
      Object returnValue = invocation.proceed();

      // 对方法执行的返回值进行验证 ，验证结果保存在 result 中，如果验证失败，result 不为空，
      // 此时会抛出异常 ConstraintViolationException
      result = (Set<ConstraintViolation<?>>) ReflectionUtils.invokeMethod(validateReturnValueMethod,
          execVal, invocation.getThis(), methodToValidate, returnValue, groups);
      if (!result.isEmpty()) {
        throw new ConstraintViolationException(result);
      }
      return returnValue;
    }

    else {
      // Hibernate Validator 4.3's native API
      return HibernateValidatorDelegate.invokeWithinValidation(invocation, this.validator, groups);
    }
  }

  //xxx

}
```



3. LocalValidatorFactoryBean：最终是使用它来执行验证功能的，它也是Spring MVC默认的验证器。默认情况下， LocalValidatorFactoryBean 会配置一个 SpringConstraintValidatorFactory 实例。如果有指定的 ConstraintValidatorFactory，就会使用指定的，因此在遇到自定义约束注解的时候，就会自动实例化 @Constraint指定的关联 Validator，从而完成数据校验过程。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272147677.png)

4. SpringValidatorAdapter：是javax.validation.Validator到Spring的Validator的适配，通过它就可以对接到Bean Validation来完成校验了。

 ```java
 public class SpringValidatorAdapter implements SmartValidator, javax.validation.Validator {
 
   // 约束注解必要的三个成员属性
   private static final Set<String> internalAnnotationAttributes = new HashSet<String>(3);
 
   static {
     // 命中约束的错误提示信息
     internalAnnotationAttributes.add("message"); 
     // 分组校验使用
     internalAnnotationAttributes.add("groups");
     // 负载
     internalAnnotationAttributes.add("payload");
   }
 
   private javax.validation.Validator targetValidator;
 
 
   // 创建一个适配器
   public SpringValidatorAdapter(javax.validation.Validator targetValidator) {
     Assert.notNull(targetValidator, "Target Validator must not be null");
     this.targetValidator = targetValidator;
   }
 
   SpringValidatorAdapter() {
   }
 
   void setTargetValidator(javax.validation.Validator targetValidator) {
     this.targetValidator = targetValidator;
   }
 
   @Override
   public boolean supports(Class<?> clazz) {
     return (this.targetValidator != null);
   }
 
   // 调用校验器校验目标对象，把Validator校验的结果-ConstraintViolations错误信息，都放在Errors的BindingResult里
   // 总之，就是把失败信息对象转换成spring内部的验证失败信息对象
   @Override
   public void validate(Object target, Errors errors) {
     if (this.targetValidator != null) {
       processConstraintViolations(this.targetValidator.validate(target), errors);
     }
   }
   // 省略其他
 }
 ```



### Service方法参数校验

#### 效果示例

更多情况下是需要对 Service 层的接口进行参数校验的，那么该怎么配置呢？

在校验方法入参的约束时，若是 @Override 父类/接口的方法，那么这个入参约束只能写在父类/接口上面。（至于为什么只能写在接口处，其实是和 Bean Validation 的实现产品有关，可参考此类：OverridingMethodMustNotAlterParameterConstraints）



1. 如果入参是平铺的参数

首先需要在父类/接口的方法入参里增加注解约束，然后用 @Validated 修饰实现类。

```java
public interface SchedulerServiceClient {
    /**
     * 获取应用不同环境的所有定时任务
     * @param appName 应用名称
     * @param env 环境
     * @param status 任务状态
     * @param userId 用户工号
     * @return
     */
    List<JobConfigInfo> queryJobList(@NotBlank(message = "应用名称不能为空")String appName, 
                                     @NotBlank(message = "环境不能为空")String env, 
                                     Integer status, 
                                     @NotBlank(message = "用户id不能为空")String userId);
}
```

```java
@Component
@HSFProvider(serviceInterface = SchedulerServiceClient.class, clientTimeout = 3000)
@Validated
public class SchedulerServiceClientImpl implements SchedulerServiceClient {

    @Override
    @Log(type = LogSourceEnum.SERVICE)
    public List<JobConfigInfo> queryJobList(String appName, String env, Integer status, String userId) {
        // 省略业务代码
    }
}
```

如果数据校验通过，就会继续执行方法里的业务逻辑；否则，就会抛出一个 ConstraintViolationException 异常。



2. 如果入参是对象：在实际开发中，其实大多数情况下方法入参是个对象，而不是单单平铺的参数。

首先需要在方法入参类里增加 @NotNull 等注解约束，然后在父类/接口的方法入参里增加 @Valid（便于嵌套校验），最后用 @Validated 修饰实现类。

```java
@Data
public class CreateDingNotificationRequest extends ToString {
    /**
     * 通知类型
     */
    @NotNull(message = "通知类型不能为空")
    @InEnum(enumType = ProcessControlDingTypeEnum.class, message = "通知类型不在枚举值范围内")
    private String dingType;
    // 省略其他
}
```

```java
public interface ProcessControlDingService {

    /**
    * 发送钉钉通知
    * @param request
    * @return
    **/
    void createDingNotification(@Valid CreateDingNotificationRequest request);

}
```

```java

@Component
@HSFProvider(serviceInterface = ProcessControlDingService.class, clientTimeout = 5000)
@Validated
public class ProcessControlDingServiceImpl implements ProcessControlDingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerNames.BIZ_SERVICE);

    @Autowired
    private ProcessControlTaskService processControlTaskService;

    @Override
    @Log(type = LogSourceEnum.SERVICE)
    public void createDingNotification(CreateDingNotificationRequest request) {
        // 省略业务代码
    }
}
```

如果需要格式化错误结果，可以再来个异常处理切面，就可以得到一个完美的异常结果。﻿



#### 较简洁的方式：FastValidatorUtils

```java
// 返回 bean 中所有约束违反约束校验结果
Set<ConstraintViolation<T>> violationSet = FastValidatorUtils.validate(bean);
```

原理见《FastValidator 原理与最佳实践》

具体示例如下：

自定义注解@RequestValid和对应切面RequestValidAspect。注解在具体的方法上，对于被注解的方法，在 AOP 中会扫描所有入参，对参数进行校验。

 ```java
 @Aspect
 @Component
 public class RequestValidAspect {
 
     @Around("@annotation(requestValid)")
     public Object around(ProceedingJoinPoint joinPoint, RequestValid requestValid) throws Throwable {
         // 获取方法入参、入参类型、出参类型
         Object[] args = joinPoint.getArgs();
         MethodSignature signature = (MethodSignature) joinPoint.getSignature();
         Class<?>[] parameterTypes = signature.getParameterTypes();
         Class<?> returnType = signature.getMethod().getReturnType();
 
         // 调用前校验每个入参
         for (Object arg : args) {
             if (arg == null) {
                 continue;
             }
             try {
                 if (arg instanceof List && ((List<?>) arg).size() > 0) {
                     for (int j = 0; j < ((List<?>) arg).size(); j++) {
                         validate(((List<?>) arg).get(j));
                     }
                 } else {
                     validate(arg);
                 }
             } catch (AlscBoltBizValidateException e) {
                 // 将异常处理为需要的格式返回
             }
         }
 
         // 方法运行后校验是否有入参约束
         Object result;
         try {
             result = joinPoint.proceed();
         } catch (ConstraintViolationException e) {
             // 将异常处理为需要的格式返回
         }
         return result;
     }
 
     public static <T> void validate(T t) {
         try {
             Set<ConstraintViolation<T>> res = FastValidatorUtils.validate(t);
             if (!res.isEmpty()) {
                 ConstraintViolation<T> constraintViolation = res.iterator().next();
                 FastValidatorHelper.throwFastValidateException(constraintViolation);
             }
 
             LoggerUtil.info(log, "validator,校验成功");
         } catch (FastValidatorException e) {
             LoggerUtil.error(log, "validator,校验报错,request=[{}],result=[{}]", JSON.toJSONString(t),
                     e.getMessage());
             throw new AlscBoltBizValidateException(CommonResultCode.ILLEGAL_PARAMETERS, e.getMessage());
         }
     }
 }
 ```



<!-- @include: @article-footer.snippet.md -->     