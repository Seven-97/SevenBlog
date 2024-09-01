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

本文源码位置[点击这里](https://github.com/Seven-97/SpringBoot-Demo/tree/master/09-aop-limit-repeatsubmit-antishake)

## 接口限流

接口限流是一种控制应用程序或服务访问速率的技术措施，主要用于防止因请求过多导致系统过载、响应延迟或服务崩溃。在高并发场景下，合理地实施接口限流对于保障系统的稳定性和可用性至关重要。

- 自定义接口限流注解类 `@AccessLimit`

```java
/**
 * 接口限流
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AccessLimit {

    /**
     * 限制时间窗口间隔长度，默认10秒
     */
    int times() default 10;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 上述时间窗口内允许的最大请求数量，默认为5次
     */
    int maxCount() default 5;

    /**
     * redis key 的前缀
     */
    String preKey();

    /**
     * 提示语
     */
    String msg() default "服务请求达到最大限制，请求被拒绝！";
}

```

- 利用`AOP`实现接口限流

```java
/**
 * 通过AOP实现接口限流
 */
@Component
@Aspect
@Slf4j
@RequiredArgsConstructor
public class AccessLimitAspect {

    private static final String ACCESS_LIMIT_LOCK_KEY = "ACCESS_LIMIT_LOCK_KEY";

    private final RedissonClient redissonClient;

    @Around("@annotation(accessLimit)")
    public Object around(ProceedingJoinPoint point, AccessLimit accessLimit) throws Throwable {

        String prefix = accessLimit.preKey();
        String key = generateRedisKey(point, prefix);

        //限制窗口时间
        int time = accessLimit.times();
        //获取注解中的令牌数
        int maxCount = accessLimit.maxCount();
        //获取注解中的时间单位
        TimeUnit timeUnit = accessLimit.timeUnit();

        //分布式计数器
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);

        if (!atomicLong.isExists() || atomicLong.remainTimeToLive() <= 0) {
            atomicLong.expire(time, timeUnit);
        }

        long count = atomicLong.incrementAndGet();
        ;
        if (count > maxCount) {
            throw new LimitException(accessLimit.msg());
        }

        // 继续执行目标方法
        return point.proceed();
    }

    public String generateRedisKey(ProceedingJoinPoint point, String prefix) {
        //获取方法签名
        MethodSignature methodSignature = (MethodSignature) point.getSignature();
        //获取方法
        Method method = methodSignature.getMethod();
        //获取全类名
        String className = method.getDeclaringClass().getName();

        // 构建Redis中的key，加入类名、方法名以区分不同接口的限制
        return String.format("%s:%s:%s", ACCESS_LIMIT_LOCK_KEY, prefix, DigestUtil.md5Hex(String.format("%s-%s", className, method)));
    }
}
```

- 调用示例实现

```java
@GetMapping("/getUser")
@AccessLimit(times = 10, timeUnit = TimeUnit.SECONDS, maxCount = 5, preKey = "getUser", msg = "服务请求达到最大限制，请求被拒绝！")
public Result getUser() {
    return Result.success("成功访问");
}
```



以上是通过 Redission 的分布式计数来实现的 [固定窗口](https://www.seven97.top/microservices/protocol/requestflowlimitingalgorithm.html) 模式的限流，也可以通过 Redission  分布式限流方案(令牌桶)的的方式RRateLimiter。

限流框架大概有

1. spring cloud gateway集成redis限流，但属于网关层限流
2. 阿里Sentinel，功能强大、带监控平台
3. srping cloud hystrix，属于接口层限流，提供线程池与信号量两种方式
4. 其他：redission、redis手撸代码



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
     */
    long lockTime() default 5;
    
    //提供了一个可选的服务ID参数，通过token时用作KEY计算
    String serviceId() default ""; 
    
    /**
     * 提示语
     */
    String msg() default "请求重复提交！";
}
```



- 利用`AOP`实现接口防重处理

```java
/**
 * 利用AOP实现接口防重处理
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
@Component
public class RepeatSubmitAspect {

    private final String REPEAT_SUBMIT_LOCK_KEY_PARAM = "REPEAT_SUBMIT_LOCK_KEY_PARAM";

    private final String REPEAT_SUBMIT_LOCK_KEY_TOKEN = "REPEAT_SUBMIT_LOCK_KEY_TOKEN";

    private final RedissonClient redissonClient;

    private final RedisRepository redisRepository;

    @Pointcut("@annotation(repeatSubmit)")
    public void pointCutNoRepeatSubmit(RepeatSubmit repeatSubmit) {

    }

    /**
     * 环绕通知, 围绕着方法执行
     * 两种方式
     * 方式一：加锁 固定时间内不能重复提交
     * 方式二：先请求获取token，再删除token,删除成功则是第一次提交
     */
    @Around("pointCutNoRepeatSubmit(repeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, RepeatSubmit repeatSubmit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        //用于记录成功或者失败
        boolean res = false;

        //获取防重提交类型
        String type = repeatSubmit.limitType().name();
        if (type.equalsIgnoreCase(RepeatSubmit.Type.PARAM.name())) {
            //方式一，参数形式防重提交
            //通过 redissonClient 获取分布式锁，基于IP地址、类名、方法名生成唯一key
            String ipAddr = IPUtil.getIpAddr(request);
            String preKey = repeatSubmit.preKey();
            String key = generateTokenRedisKey(joinPoint, ipAddr, preKey);

            //获取注解中的锁时间
            long lockTime = repeatSubmit.lockTime();
            //获取注解中的时间单位
            TimeUnit timeUnit = repeatSubmit.timeUnit();

            //使用 tryLock 尝试获取锁，如果无法获取（即锁已被其他请求持有），则认为是重复提交，直接返回null
            RLock lock = redissonClient.getLock(key);
            //锁自动过期时间为 lockTime 秒，确保即使程序异常也不会永久锁定资源，尝试加锁，最多等待0秒，上锁以后 lockTime 秒自动解锁 [lockTime默认为5s, 可以自定义]
            res = lock.tryLock(0, lockTime, timeUnit);

        } else {
            //方式二，令牌形式防重提交
            //从请求头中获取 request-token，如果不存在，则抛出异常
            String requestToken = request.getHeader("request-token");
            if (StringUtils.isBlank(requestToken)) {
                throw new LimitException("请求未包含令牌");
            }
            //使用 request-token 和 serviceId 构造Redis的key，尝试从Redis中删除这个键。如果删除成功，说明是首次提交；否则认为是重复提交
            String key = String.format("%s:%s:%s", REPEAT_SUBMIT_LOCK_KEY_TOKEN, repeatSubmit.serviceId(), requestToken);
            res = redisRepository.del(key);
        }

        if (!res) {
            log.error("请求重复提交");
            throw new LimitException(repeatSubmit.msg());
        }

        return joinPoint.proceed();
    }

    private String generateTokenRedisKey(ProceedingJoinPoint joinPoint, String ipAddr, String preKey) {
        //根据ip地址、用户id、类名方法名、生成唯一的key
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        String className = method.getDeclaringClass().getName();
        String userId = "seven";
        return String.format("%s:%s:%s", REPEAT_SUBMIT_LOCK_KEY_PARAM, preKey, DigestUtil.md5Hex(String.format("%s-%s-%s-%s", ipAddr, className, method, userId)));
    }
}
```

- 调用示例

```java
@PostMapping("/saveUser")
@RepeatSubmit(limitType = RepeatSubmit.Type.PARAM,lockTime = 5,timeUnit = TimeUnit.SECONDS,preKey = "saveUser",msg = "请求重复提交")
public Result saveUser() {
    return Result.success("成功保存");
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
    // 默认防抖时间1秒，单位秒
    long value() default 1000L; 
}
```

- 实现`AOP`切面处理防抖

```java
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
@PostMapping("/clickButton")
@AntiShake(value = 1000)
public Result clickButton() {
    return Result.success("成功点击按钮");
}
```

















