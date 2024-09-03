---
title: AOP注解的妙用 - 实现接口限流、防重、防抖
category: 系统设计
tag:
  - SpringBoot
  - Redisson
  - Spring
---



最近上了一个新项目，考虑到一个问题，在高并发场景下，我们无法控制前端的请求频率和次数，这就可能导致服务器压力过大，响应速度变慢，甚至引发系统崩溃等严重问题。为了解决这些问题，我们需要在后端实现一些机制，如接口限流、防重复提交和接口防抖，而这些是保证接口安全、稳定提供服务，以及防止错误数据 和 脏数据产生的重要手段。

而AOP适合在在不改变业务代码的情况下，灵活地添加各种横切关注点，实现一些通用公共的业务场景，例如日志记录、事务管理、安全检查、性能监控、缓存管理、限流、防重复提交等功能。这样不仅提高了代码的可维护性，还使得业务逻辑更加清晰专注，关于AOP不理解的可以看[这篇文章](https://www.seven97.top/framework/spring/aop1-summary.html)。

本文源码位置[点击这里](https://github.com/Seven-97/SpringBoot-Demo/tree/master/09-aop-limit-repeatsubmit-antishake)

## 接口限流

接口限流是一种控制访问频率的技术，通过限制在一定时间内允许的最大请求数来保护系统免受过载。限流可以在应用的多个层面实现，比如在网关层、应用层甚至数据库层。常用的限流算法有[漏桶算法（Leaky Bucket）、令牌桶算法（Token Bucket）](https://www.seven97.top/microservices/protocol/requestflowlimitingalgorithm.html)等。限流不仅可以防止系统过载，还可以防止恶意用户的请求攻击。

限流框架大概有

1. spring cloud gateway集成redis限流，但属于网关层限流
2. 阿里Sentinel，功能强大、带监控平台
3. srping cloud hystrix，属于接口层限流，提供线程池与信号量两种方式
4. 其他：redisson、redis手撸代码

本文主要是通过 Redisson 的分布式计数来实现的 [固定窗口](https://www.seven97.top/microservices/protocol/requestflowlimitingalgorithm.html) 模式的限流，也可以通过 Redisson  分布式限流方案(令牌桶)的方式：RRateLimiter。

在高并发场景下，合理地实施接口限流对于保障系统的稳定性和可用性至关重要。

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





## 防重复提交

在一些业务场景中，重复提交同一个请求可能会导致数据的不一致，甚至严重影响业务逻辑的正确性。例如，在提交订单的场景中，重复提交可能会导致用户被多次扣款。为了避免这种情况，可以使用防重复提交技术，这对于保护数据一致性、避免资源浪费非常重要

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

接口防抖是一种优化用户操作体验的技术，主要用于减少短时间内高频率触发的操作。例如，当用户快速点击按钮时，我们可以通过防抖机制，只处理最后一次触发的操作，而忽略前面短时间内的多次操作。防抖技术常用于输入框文本变化事件、按钮点击事件等场景，以提高系统的性能和用户体验。

后端接口防抖处理主要是为了避免在短时间内接收到大量相同的请求，特别是由于前端操作（如快速点击按钮）、网络重试或异常情况导致的重复请求。后端接口防抖通常涉及记录最近的请求信息，并在特定时间窗口内拒绝处理相同或相似的请求。

- 定义自定义注解 `@AntiShake`

```java
// 该注解只能用于方法
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)// 运行时保留，这样才能在AOP中被检测到
public @interface AntiShake {

    String preKey() default "";

    // 默认防抖时间1秒
    long value() default 1000L;

    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}
```

- 实现`AOP`切面处理防抖

```java
@Aspect // 标记为切面类
@Component // 让Spring管理这个Bean
@RequiredArgsConstructor // 通过构造方法注入依赖
public class AntiShakeAspect {

    private final String ANTI_SHAKE_LOCK_KEY = "ANTI_SHAKE_LOCK_KEY";

    private final RedissonClient redissonClient;

    @Around("@annotation(antiShake)") // 拦截所有标记了@AntiShake的方法
    public Object aroundAdvice(ProceedingJoinPoint joinPoint, AntiShake antiShake) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        long currentTime = System.currentTimeMillis();

        // 获取方法签名和参数作为 Redis 键
        String ipAddr = IPUtil.getIpAddr(request);
        String key = generateTokenRedisKey(joinPoint, ipAddr, antiShake.preKey());

        RBucket<Long> bucket = redissonClient.getBucket(key);
        Long lastTime = bucket.get();

        if (lastTime != null && currentTime - lastTime < antiShake.value()) {
            // 如果距离上次调用时间小于指定的防抖时间，则直接返回，不执行方法
            return null; // 根据业务需要返回特定值
        }

        // 更新 Redis 中的时间戳
        bucket.set(currentTime, antiShake.value(), antiShake.timeUnit());
        return joinPoint.proceed(); // 执行原方法
    }

    private String generateTokenRedisKey(ProceedingJoinPoint joinPoint, String ipAddr, String preKey) {
        //根据ip地址、用户id、类名方法名、生成唯一的key
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        String className = method.getDeclaringClass().getName();
        String userId = "seven";
        return String.format("%s:%s:%s", ANTI_SHAKE_LOCK_KEY, preKey, DigestUtil.md5Hex(String.format("%s-%s-%s-%s", ipAddr, className, method, userId)));
    }
}
```

- 调用示例代码

```java
@PostMapping("/clickButton")
@AntiShake(value = 1000, timeUnit = TimeUnit.MILLISECONDS, preKey = "clickButton")
public Result clickButton() {
    return Result.success("成功点击按钮");
}
```

接口防抖整体思路与防重复提交思路类似，防重复提交代码也可重用于接口防抖





<!-- @include: @article-footer.snippet.md -->     











