---
title: AOP注解的妙用 - 实现接口限流、防重、防抖
category: 系统设计
tag:
  - SpringBoot
  - Redisson
  - Spring
---



接口限流、防重复提交、接口防抖，是保证接口安全、稳定提供服务，以及防止错误数据 和 脏数据产生的重要手段。

关于AOP不理解的可以看[这篇文章](https://www.seven97.top/framework/spring/aop1-summary.html)



## 接口限流

接口限流是一种控制应用程序或服务访问速率的技术措施，主要用于防止因请求过多导致系统过载、响应延迟或服务崩溃。在高并发场景下，合理地实施接口限流对于保障系统的稳定性和可用性至关重要。

- 自定义接口限流注解类 `@AccessLimit`

```java
/**
* 接口限流
*/
@Retention(RUNTIME)
@Target(METHOD)
public @interface AccessLimit {
   //限制时间窗口间隔长度，默认10秒
   int seconds() default 10;
   //上述时间窗口内允许的最大请求数量，默认为5次
   int maxCount() default 5;
   //提示语
   String msg() default "您操作频率太过频繁，稍后再试";
}
```

- 利用`AOP`实现接口限流

```java
/**
* 通过AOP实现接口限流
*/
@@Component
@Aspect
@Slf4j
@RequiredArgsConstructor
public class AccessLimitAspect {
 
    private final RedissonClient redissonClient;
 
    @Around("@annotation(accessLimit)")
    public Object around(ProceedingJoinPoint point, AccessLimit accessLimit) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = attributes.getResponse();
        if (attributes == null) return null;
 
        MethodSignature signature = (MethodSignature) point.getSignature();
        String methodName = signature.getMethod().getName();
        String className = point.getTarget().getClass().getName();
 
        // 构建Redis中的key，可以考虑加入类名和方法名以区分不同接口的限制
        String key = className + ":" + methodName;
 
        // 获取并检查限制条件
        int seconds = accessLimit.seconds();
        int maxCount = accessLimit.maxCount();
        boolean hasKey = stringRedisTemplate.hasKey(key);
 
        if (!hasKey || stringRedisTemplate.getExpire(key, TimeUnit.SECONDS) <= 0) {
            redisRepository.expire(key, seconds, TimeUnit.SECONDS);
        }
 
        long count = redisRepository.opsForValue().increment(key);
        if (count > maxCount) {
            // 设置HTTP状态码为429 Too Many Requests
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS); 
            // 返回提示信息
            response.getWriter().write(accessLimit.msg());
            // 阻止执行
            return null; 
        }
        // 继续执行目标方法
        return point.proceed(); 
    }
}
```

- 调用示例实现

```java
/**
* 调用示例
*/
@GetMapping("/limit")
//如果只@AccessLimit，不设置后面的值，则取默认值
@AccessLimit(seconds=5, maxCount=3 , msg="您操作频率太过频繁，稍后再试")
public ResultVO AccessLimit(){
    return new ResultVO();
}
```







## 防重复提交

接口防重复提交是防止用户在短时间内多次点击提交按钮或重复发送相同请求导致的多次执行同一操作的问题，这对于保护数据一致性、避免资源浪费非常重要

- 自定义接口防重注解类 `@RepeatSubmit`

```java
/**
* 自定义接口防重注解类
*/
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatSubmit {
    /**
     * 定义了两种防止重复提交的方式，PARAM 表示基于方法参数来防止重复，TOKEN 则可能涉及生成和验证token的机制
     */
    enum Type { PARAM, TOKEN }
    /**
     * 设置默认的防重提交方式为基于方法参数。开发者可以不指定此参数，使用默认值。
     * @return Type
     */
    Type limitType() default Type.PARAM;
 
 
    /**
     * 允许设置加锁的过期时间，默认为5秒。这意味着在第一次请求之后的5秒内，相同的请求将被视为重复并被阻止
     * @return
     */
    long lockTime() default 5;
    
    //提供了一个可选的服务ID参数，通过token时用作KEY计算
    String serviceId() default ""; 
}
```



- 利用`AOP`实现接口防重处理

```java
/**
* @author 吴农软语
* 利用AOP实现接口防重处理
*/
@Aspect
@Slf4j
public class RepeatSubmitAspect {
    @Resource
    private RedisRepository redisRepository;
 
    @Resource
    private RedissonClient redissonClient;
 
    @Pointcut("@annotation(repeatSubmit)")
    public void pointCutNoRepeatSubmit(RepeatSubmit repeatSubmit) {
 
    }
 
    /**
     * 环绕通知, 围绕着方法执行
     * 两种方式
     * 方式一：加锁 固定时间内不能重复提交
     * 方式二：先请求获取token，这边再删除token,删除成功则是第一次提交
     */
    @Around("pointCutNoRepeatSubmit(repeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, RepeatSubmit repeatSubmit) throws Throwable {
 
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String serviceId = repeatSubmit.serviceId();
        //用于记录成功或者失败
        boolean res = false;
        //防重提交类型
        String type = repeatSubmit.limitType().name();
        if (type.equalsIgnoreCase(RepeatSubmit.Type.PARAM.name())) {
            //方式一，参数形式防重提交
            //通过 redissonClient 获取分布式锁，基于IP地址、类名、方法名和服务ID生成唯一key
            long lockTime = repeatSubmit.lockTime();
            String ipAddr = AddrUtil.getRemoteAddr(request);
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();
            String className = method.getDeclaringClass().getName();
            String key = repeatSubmit.serviceId() + ":repeat_submit:" + AddrUtil.MD5(String.format("%s-%s-%s-%s", ipAddr, className, method, serviceId));
            
            //使用 tryLock 尝试获取锁，如果无法获取（即锁已被其他请求持有），则认为是重复提交，直接返回null
            RLock lock = redissonClient.getLock(key);
            //锁自动过期时间为 lockTime 秒，确保即使程序异常也不会永久锁定资源,尝试加锁，最多等待0秒，上锁以后5秒自动解锁 [lockTime默认为5s, 可以自定义]
            res = lock.tryLock(0, lockTime, TimeUnit.SECONDS);
 
        } else {
            //方式二，令牌形式防重提交
            //从请求头中获取 request-token，如果不存在，则抛出异常
            String requestToken = request.getHeader("request-token");
            if (StringUtils.isBlank(requestToken)) {
                throw new RuntimeException("请求未包含令牌");
            }
            //使用 request-token 和 serviceId 构造Redis的key，尝试从Redis中删除这个键。如果删除成功，说明是首次提交；否则认为是重复提交
            String key = String.format(CommonConstants.SERVICE_SUBMIT_TOKEN_KEY, repeatSubmit.serviceId(), requestToken);
            res = redisRepository.del(key);
        }
        if (!res) {
            log.error("请求重复提交");
            return null;
        }
        //在环绕通知的前后记录日志，有助于跟踪方法执行情况和重复提交的检测
        log.info("环绕通知执行前");
        Object obj = joinPoint.proceed();
        log.info("环绕通知执行后");
        return obj;
    }
}
```

- 调用示例

```java
  @PostMapping("/users/save")
    @RepeatSubmit(serviceId = "saveUser", limitType = RepeatSubmit.Type.TOKEN, lockTime = 5)
    public ResponseEntity<String> saveUser(@RequestBody User user) {
        userService.save(user);
        return ResponseEntity.ok("用户保存成功");
    }
```







## 接口防抖

接口防抖是一种常见的前端性能优化策略，用于限制在一定时间内连续触发的函数只会执行一次，常用于搜索框的输入监听、按钮防连击等情况，以减少不必要的计算或网络请求。

后端接口防抖处理主要是为了避免在短时间内接收到大量相同的请求，特别是由于前端操作（如快速点击按钮）、网络重试或异常情况导致的重复请求。后端接口防抖通常涉及记录最近的请求信息，并在特定时间窗口内拒绝处理相同或相似的请求。

- 定义自定义注解 `@AntiShake`

```java
// 该注解只能用于方法
@Target(ElementType.METHOD) 
// 运行时保留，这样才能在AOP中被检测到
@Retention(RetentionPolicy.RUNTIME) 
public @interface AntiShake {
    // 默认防抖时间1秒
    long value() default 1000L; 
}
```

- 实现`AOP`切面处理防抖

```java
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
 
@Aspect // 标记为切面类
@Component // 让Spring管理这个Bean
public class AntiShakeAspect {
 
    private ThreadLocal<Long> lastInvokeTime = new ThreadLocal<>();
 
    @Around("@annotation(antiShake)") // 拦截所有标记了@AntiShake的方法
    public Object aroundAdvice(ProceedingJoinPoint joinPoint, AntiShake antiShake) throws Throwable {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastInvokeTime.get() != null ? lastInvokeTime.get() : 0;
        
        if (currentTime - lastTime < antiShake.value()) {
            // 如果距离上次调用时间小于指定的防抖时间，则直接返回，不执行方法
            return null; // 或者根据业务需要返回特定值
        }
        
        lastInvokeTime.set(currentTime);
        return joinPoint.proceed(); // 执行原方法
    }
}
```

- 调用示例代码

```java
@Service // 假设是服务层的一个方法，确保它处于Spring管理的Bean中
public class SomeService {
   @AntiShake(value = 2000) // 设置防抖时间为2秒
   public String someMethodThatNeedsToBeDebounced(String param) {
        // ... 方法实现
        return "Result";
    }
}
```

















