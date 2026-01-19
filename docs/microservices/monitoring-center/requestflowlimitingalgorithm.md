---
title: 深入理解请求限流算法的实现细节
category: 微服务
tags:
  - 服务保护
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,请求限流算法,漏斗限流,令牌桶限流算法
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---



## 固定窗口限流算法

固定窗口限流算法（Fixed Window Rate Limiting Algorithm）是一种最简单的限流算法，其原理是**在固定时间窗口(单位时间)内限制请求的数量**。该算法将时间分成固定的窗口，并在每个窗口内限制请求的数量。具体来说，算法将请求按照时间顺序放入时间窗口中，并计算该时间窗口内的请求数量，如果请求数量超出了限制，则拒绝该请求。

假设单位时间(固定时间窗口)是1秒，限流阀值为3。在单位时间1秒内，每来一个请求,计数器就加1，如果计数器累加的次数超过限流阀值3，后续的请求全部拒绝。等到1s结束后，计数器清0，重新开始计数。如下图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301632931.png)

### 伪代码实现

```java
   public static Integer counter = 0;  //统计请求数
   public static long lastAcquireTime =  0L;
   public static final Long windowUnit = 1000L ; //假设固定时间窗口是1000ms
   public static final Integer threshold = 10; // 窗口阀值是10
   
    /**
     * 固定窗口时间算法
     * @return
     */
    public synchronized boolean fixedWindowsTryAcquire() {
        long currentTime = System.currentTimeMillis();  //获取系统当前时间
        if (currentTime - lastAcquireTime > windowUnit) {  //检查是否在时间窗口内
            counter = 0;  // 计数器清0
            lastAcquireTime = currentTime;  //开启新的时间窗口
        }
        if (counter < threshold) {  // 小于阀值
            counter++;  //计数统计器加1
            return true;
        }
 
        return false;
    }
```



### 优缺点

优点：固定窗口算法非常简单，易于实现和理解。

缺点：存在**明显的临界问题**，比如: 假设限流阀值为5个请求，单位时间窗口是1s,如果我们在单位时间内的前0.8-1s和1-1.2s，分别并发5个请求。虽然都没有超过阀值，但是如果算0.8-1.2s,则并发数高达10，就已经超过单位时间1s不超过5阀值的定义。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301633295.png)

## 滑动窗口限流算法

滑动窗口限流算法是一种常用的限流算法，用于控制系统对外提供服务的速率，防止系统被过多的请求压垮。它**将单位时间周期分为n个小周期，分别记录每个小周期内接口的访问次数，并且根据时间滑动删除过期的小周期**。**它可以解决固定窗口临界值的问题**。

用一张图解释滑动窗口算法，如下：假设单位时间还是1s，滑动窗口算法把它划分为5个小周期，也就是滑动窗口（单位时间）被划分为5个小格子。每格表示0.2s。每过0.2s，时间窗口就会往右滑动一格。然后呢，每个小周期，都有自己独立的计数器，如果请求是0.83s到达的，0.8~1.0s对应的计数器就会加1。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301633825.png)

滑动窗口如何解决固定窗口的临界问题？

假设1s内的限流阀值还是5个请求，0.81.0s内（比如0.9s的时候）来了5个请求，落在黄色格子里。时间过了1.0s这个点之后，又来5个请求，落在紫色格子里。如果是固定窗口算法，是不会被限流的，但是滑动窗口的话，每过一个小周期，它会右移一个小格。过了1.0s这个点后，会右移一小格，当前的单位时间段是0.21.2s，这个区域的请求已经超过限定的5了，已触发限流啦，实际上，紫色格子的请求都被拒绝啦。

显然，当滑动窗口的格子周期划分的越多，那么滑动窗口的滚动就越平滑，限流的统计就会越精确。

### 伪代码实现

```java
 /**
     * 单位时间划分的小周期（单位时间是1分钟，10s一个小格子窗口，一共6个格子）
     */
    private int SUB_CYCLE = 10;
 
    /**
     * 每分钟限流请求数
     */
    private int thresholdPerMin = 100;
 
    /**
     * 计数器, k-为当前窗口的开始时间值秒，value为当前窗口的计数
     */
    private final TreeMap<Long, Integer> counters = new TreeMap<>();
 
   /**
     * 滑动窗口时间算法实现
     */
     public synchronized boolean slidingWindowsTryAcquire() {
        long currentWindowTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) / SUB_CYCLE * SUB_CYCLE; //获取当前时间在哪个小周期窗口
        int currentWindowNum = countCurrentWindow(currentWindowTime); //当前窗口总请求数
 
        //超过阀值限流
        if (currentWindowNum >= thresholdPerMin) {
            return false;
        }
 
        //计数器+1
        counters.get(currentWindowTime)++;
        return true;
    }
 
   /**
    * 统计当前窗口的请求数
    */
    private synchronized int countCurrentWindow(long currentWindowTime) {
        //计算窗口开始位置
        long startTime = currentWindowTime - SUB_CYCLE* (60s/SUB_CYCLE-1);
        int count = 0;
 
        //遍历存储的计数器
        Iterator<Map.Entry<Long, Integer>> iterator = counters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Integer> entry = iterator.next();
            // 删除无效过期的子窗口计数器
            if (entry.getKey() < startTime) {
                iterator.remove();
            } else {
                //累加当前窗口的所有计数器之和
                count =count + entry.getValue();
            }
        }
        return count;
    }
```



### 优缺点

优点：

- 简单易懂

- 精度高（通过调整时间窗口的大小来实现不同的限流效果）

- 可扩展性强（可以非常容易地与其他限流算法结合使用）

缺点：

- 突发流量无法处理（无法应对短时间内的大量请求，因为一旦到达限流后，请求都会直接暴力被拒绝。这样就会损失一部分请求，这其实对于产品来说，并不太友好），需要合理调整时间窗口大小。

### Redisson实现滑动窗口

```java
import org.redisson.api.RBatch;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Component
public class SlidingWindowCounter {

    private final RedissonClient redissonClient;
    // 用于存储脚本SHA摘要的字段
    private final String incrementScriptSha;
    private final String addUniqueItemScriptSha;

    public SlidingWindowCounter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        // 在构造函数中预加载脚本
        this.incrementScriptSha = loadScript(buildIncrementLuaScript());
        this.addUniqueItemScriptSha = loadScript(buildAddUniqueItemLuaScript());
    }
    
    private String loadScript(String luaScript) {
       // 使用 scriptLoad 方法将脚本预加载到Redis，并返回SHA摘要
       return redissonClient.getScript().scriptLoad(luaScript);
    }
    
    // 将脚本内容定义为常量或方法，便于管理和预加载
    private String buildIncrementLuaScript() {
    // 使用Lua脚本保证原子性：清除过期数据、添加新记录、统计总数、设置过期时间
       return "local key = KEYS[1] " +
                "local windowStart = tonumber(ARGV[1]) " +
                "local currentTime = tonumber(ARGV[2]) " +
                "local windowMillis = tonumber(ARGV[3]) " +
                // 1. 移除窗口之前的过期数据，也就是windowStart之前的数据
                "redis.call('zremrangebyscore', key, 0, windowStart) " +
                // 2. 添加当前请求记录（使用UUID确保成员唯一，避免覆盖）
                "redis.call('zadd', key, currentTime, currentTime .. '_' .. ARGV[4]) " +
                // 3. 获取当前窗口内的总元素个数（即总请求次数）
                "local totalCount = redis.call('zcard', key) " +
                // 4. 设置Key的过期时间，避免内存泄漏（过期时间略大于窗口时间）
                "redis.call('expire', key, windowMillis / 1000 + 60) " +
                // 5. 返回总数
                "return totalCount";
    }
    
    private String buildAddUniqueItemLuaScript() {
	    // Lua脚本：清除过期数据、添加/更新唯一项的时间戳、统计唯一项个数
	    return  "local key = KEYS[1] " +
                "local windowStart = tonumber(ARGV[1]) " +
                "local currentTime = tonumber(ARGV[2]) " +
                "local uniqueItem = ARGV[3] " +
                "local windowMillis = tonumber(ARGV[4]) " +
                // 1. 移除窗口之前的过期数据
                "redis.call('zremrangebyscore', key, 0, windowStart) " +
                // 2. 添加或更新唯一项（同一酒店ID会更新分数为最新时间）
                "redis.call('zadd', key, currentTime, uniqueItem) " +
                // 3. 获取当前窗口内的唯一元素个数（即不同酒店数）
                "local uniqueCount = redis.call('zcard', key) " +
                // 4. 设置过期时间
                "redis.call('expire', key, windowMillis / 1000 + 60) " +
                // 5. 返回唯一项数量
                "return uniqueCount";
    }

    /**
     * 增加计数
     * 例如：统计用户请求次数，同一用户的多次请求会累加。
     *
     * @param counterKey 计数器的唯一标识，如 "req:user123"
     * @param windowSize 窗口大小
     * @param unit       窗口单位
     * @return 当前窗口内的总请求次数
     */
    public long increment(String counterKey, long windowSize, TimeUnit unit) {

        long windowMillis = unit.toMillis(windowSize);
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - windowMillis;
        String redisKey = buildKey(counterKey, windowSize, unit);

        RScoredSortedSet<String> windowSet = redissonClient.getScoredSortedSet(redisKey);

        Long result = redissonClient.getScript().eval(
                org.redisson.api.RScript.Mode.READ_WRITE,
                incrementScriptSha,
                org.redisson.api.RScript.ReturnType.INTEGER,
                java.util.Collections.singletonList(redisKey),
                windowStart, currentTime, windowMillis, java.util.UUID.randomUUID().toString()
        );

        return result != null ? result : 0L;
    }

    /**
     * 记录唯一值 - 用于“不同数据的计数”
     * 例如：统计用户访问的不同酒店数，同一酒店ID多次出现只计一次。
     *
     * @param counterKey 计数器的唯一标识，如 "hotel:user123"
     * @param uniqueItem  需要统计的唯一项，如酒店ID "hotel_456"
     * @param windowSize 窗口大小
     * @param unit       窗口单位
     * @return 当前窗口内的唯一项数量
     */

    public long addUniqueItem(String counterKey, String uniqueItem, long windowSize, TimeUnit unit) {

        long windowMillis = unit.toMillis(windowSize);
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - windowMillis;
        String redisKey = buildKey(counterKey, windowSize, unit);

        RScoredSortedSet<String> windowSet = redissonClient.getScoredSortedSet(redisKey);

        Long result = redissonClient.getScript().eval(
                org.redisson.api.RScript.Mode.READ_WRITE,
                addUniqueItemScriptSha,
                org.redisson.api.RScript.ReturnType.INTEGER,
                java.util.Collections.singletonList(redisKey),
                windowStart, currentTime, uniqueItem, windowMillis
        );

        return result != null ? result : 0L;
    }

    /**
     * 获取当前窗口的计数
     *
     * @param counterKey 计数器的唯一标识
     * @param windowSize 窗口大小
     * @param unit       窗口单位
     * @return 当前窗口内的计数
     */
    public long getCount(String counterKey, long windowSize, TimeUnit unit) {

        long windowMillis = unit.toMillis(windowSize);
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - windowMillis;
        String redisKey = buildKey(counterKey, windowSize, unit);

        RScoredSortedSet<String> windowSet = redissonClient.getScoredSortedSet(redisKey);

        // 移除过期数据并返回当前数量
        windowSet.removeRangeByScore(0, true, windowStart, false);
        return windowSet.size();
    }

    /**
     * 获取当前窗口的所有唯一成员
     *
     * @param counterKey 计数器的唯一标识
     * @param windowSize 窗口大小
     * @param unit       窗口单位
     * @return 当前窗口内的所有唯一成员集合
     */

    public Collection<String> getUniqueItems(String counterKey, long windowSize, TimeUnit unit) {

        long windowMillis = unit.toMillis(windowSize);
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - windowMillis;
        String redisKey = buildKey(counterKey, windowSize, unit);

        RScoredSortedSet<String> windowSet = redissonClient.getScoredSortedSet(redisKey);

        // 移除过期数据
        windowSet.removeRangeByScore(0, true, windowStart, false);
        // 返回所有成员（即不同的酒店ID等）

        return windowSet.readAll();
    }

    /**
     * 构建Redis存储的Key，包含窗口信息
     */
    private String buildKey(String baseKey, long windowSize, TimeUnit unit) {

        // 示例：sliding:counter:req:user123:3600000 (窗口3600秒)
        return String.format("sliding:counter:%s:%d", baseKey, unit.toMillis(windowSize));
    }

}
```

## 漏斗限流算法

漏桶限流算法（Leaky Bucket Algorithm）是一种流量控制算法，用于控制流入网络的数据速率，以防止网络拥塞。它的思想是将数据包看作是水滴，漏桶看作是一个固定容量的水桶，数据包像水滴一样从桶的顶部流入桶中，并通过桶底的一个小孔以一定的速度流出，从而限制了数据包的流量。

漏桶限流算法的基本工作原理是：**对于每个到来的数据包，都将其加入到漏桶中，并检查漏桶中当前的水量是否超过了漏桶的容量。如果超过了容量，就将多余的数据包丢弃。如果漏桶中还有水，就以一定的速率从桶底输出数据包，保证输出的速率不超过预设的速率，从而达到限流的目的**。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301633213.png)

1. 流入的水滴，可以看作是访问系统的请求，这个流入速率是不确定的。
2. 桶的容量一般表示系统所能处理的请求数。
3. 如果桶的容量满了，就达到限流的阀值，就会丢弃水滴（拒绝请求）
4. 流出的水滴，是恒定过滤的，对应服务按照固定的速率处理请求。

### 伪代码实现

```java
 /**
 * LeakyBucket 类表示一个漏桶,
 * 包含了桶的容量和漏桶出水速率等参数，
 * 以及当前桶中的水量和上次漏水时间戳等状态。
 */
public class LeakyBucket {
    private final long capacity;    // 桶的容量
    private final long rate;        // 漏桶出水速率
    private long water;             // 当前桶中的水量
    private long lastLeakTimestamp; // 上次漏水时间戳
 
    public LeakyBucket(long capacity, long rate) {
        this.capacity = capacity;
        this.rate = rate;
        this.water = 0;
        this.lastLeakTimestamp = System.currentTimeMillis();
    }
 
    /**
     * tryConsume() 方法用于尝试向桶中放入一定量的水，如果桶中还有足够的空间，则返回 true，否则返回 false。
     * @param waterRequested
     * @return
     */
    public synchronized boolean tryConsume(long waterRequested) {
        leak();
        if (water + waterRequested <= capacity) {
            water += waterRequested;
            return true;
        } else {
            return false;
        }
    }
 
    /**
     * 。leak() 方法用于漏水，根据当前时间和上次漏水时间戳计算出应该漏出的水量，然后更新桶中的水量和漏水时间戳等状态。
     */
    private void leak() {
        long now = System.currentTimeMillis();
        long elapsedTime = now - lastLeakTimestamp;
        long leakedWater = elapsedTime * rate / 1000;
        if (leakedWater > 0) {
            water = Math.max(0, water - leakedWater);
            lastLeakTimestamp = now;
        }
    }
}
//注意: tryConsume() 和 leak() 方法中，都需要对桶的状态进行同步，以保证线程安全性。
```



### 优缺点

优点：

- 可以平滑限制请求的处理速度，避免瞬间请求过多导致系统崩溃或者雪崩。

- 可以控制请求的处理速度，使得系统可以适应不同的流量需求，避免过载或者过度闲置。

- 可以通过调整桶的大小和漏出速率来满足不同的限流需求，可以灵活地适应不同的场景。

缺点：

- 需要对请求进行缓存，会增加服务器的内存消耗。

- 对于流量波动比较大的场景，需要较为灵活的参数配置才能达到较好的效果。

- 但是**面对突发流量**的时候，漏桶算法还是循规蹈矩地处理请求。因为流量变突发时，肯定希望系统尽量快点处理请求，提升用户体验。

## 令牌桶限流算法

令牌桶算法是一种常用的限流算法，可以用于限制单位时间内请求的数量。该算法**维护一个固定容量的令牌桶，每秒钟会向令牌桶中放入一定数量的令牌。当有请求到来时，如果令牌桶中有足够的令牌，则请求被允许通过并从令牌桶中消耗一个令牌，否则请求被拒绝**。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301633664.png)

### 伪代码实现

```java
/**
 * TokenBucket 类表示一个令牌桶
 */
public class TokenBucket {
 
    private final int capacity;     // 令牌桶容量
    private final int rate;         // 令牌生成速率，单位：令牌/秒
    private int tokens;             // 当前令牌数量
    private long lastRefillTimestamp;  // 上次令牌生成时间戳
 
    /**
     * 构造函数中传入令牌桶的容量和令牌生成速率。
     * @param capacity
     * @param rate
     */
    public TokenBucket(int capacity, int rate) {
        this.capacity = capacity;
        this.rate = rate;
        this.tokens = capacity;
        this.lastRefillTimestamp = System.currentTimeMillis();
    }
 
    /**
     * allowRequest() 方法表示一个请求是否允许通过，该方法使用 synchronized 关键字进行同步，以保证线程安全。
     * @return
     */
    public synchronized boolean allowRequest() {
        refill();
        if (tokens > 0) {
            tokens--;
            return true;
        } else {
            return false;
        }
    }
 
    /**
     * refill() 方法用于生成令牌，其中计算令牌数量的逻辑是按照令牌生成速率每秒钟生成一定数量的令牌，
     * tokens 变量表示当前令牌数量，
     * lastRefillTimestamp 变量表示上次令牌生成的时间戳。
     */
    private void refill() {
        long now = System.currentTimeMillis();
        if (now > lastRefillTimestamp) {
            int generatedTokens = (int) ((now - lastRefillTimestamp) / 1000 * rate);
            tokens = Math.min(tokens + generatedTokens, capacity);
            lastRefillTimestamp = now;
        }
    }
}
```



### 优缺点

优点：

- 稳定性高：令牌桶算法可以控制请求的处理速度，可以使系统的负载变得稳定。

- 精度高：令牌桶算法可以根据实际情况动态调整生成令牌的速率，可以实现较高精度的限流。

- 弹性好：令牌桶算法可以处理突发流量，可以在短时间内提供更多的处理能力，以处理突发流量。

Guava的RateLimiter限流组件，就是基于令牌桶算法实现的。

缺点：

- 实现复杂：相对于固定窗口算法等其他限流算法，令牌桶算法的实现较为复杂。 对短时请求难以处理：在短时间内有大量请求到来时，可能会导致令牌桶中的令牌被快速消耗完，从而限流。这种情况下，可以考虑使用漏桶算法。

- 时间精度要求高：令牌桶算法需要在固定的时间间隔内生成令牌，因此要求时间精度较高，如果系统时间不准确，可能会导致限流效果不理想。

总体来说，令牌桶算法具有较高的稳定性和精度，但实现相对复杂，适用于对稳定性和精度要求较高的场景。

## 单机限流和分布式限流

本质上单机限流和分布式限流的区别其实就在于“阈值” 存放的位置,

### 单机限流

如果系统部署，是只有一台机器，那可以直接使用 单机限流的方案。可以使用Guava框架里的限流器

```java
<!-- https://mvnrepository.com/artifact/com.google.guava/guava -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>30.1.1-jre</version>
</dependency>
```

示例代码

```java
    public static void main(String[] args) throws InterruptedException {
        // 每秒产生 1 个令牌
        RateLimiter rt = RateLimiter.create(1, 1, TimeUnit.SECONDS);
        System.out.println("try acquire token: " + rt.tryAcquire(1) + " time:" + System.currentTimeMillis());
        System.out.println("try acquire token: " + rt.tryAcquire(1) + " time:" + System.currentTimeMillis());
        Thread.sleep(2000);
        System.out.println("try acquire token: " + rt.tryAcquire(1) + " time:" + System.currentTimeMillis());
        System.out.println("try acquire token: " + rt.tryAcquire(1) + " time:" + System.currentTimeMillis());

        System.out.println("-------------分隔符-----------------");

    }
```

`RateLimiter.tryAcquire()` 和 `RateLimiter.acquire()` 两个方法都通过限流器获取令牌

### 分布式限流

单机限流就上面所说的算法直接在单台服务器上实现就好了，而往往我们的服务是集群部署的，因此需要多台机器协同提供限流功能。

**固定窗口限流算法**：可以将计数器存放至 Redis 等分布式 K-V 存储中。

**滑动窗口限流算法**：滑动窗口的每个请求的时间记录可以利用 Redis的 zset存储，实现的过程是先使用 `ZSet` 的 `key` 存储限流的 `ID`，`score` 用来存储请求的时间，每次有请求访问来了之后，利用 ZREMRANGEBYSCORE 删除时间窗口之外的数据，再用 ZCARD 计数。

此实现方式存在的缺点有两个：

- 使用 ZSet 存储有每次的访问记录，如果数据量比较大时会占用大量的空间；
- 此代码的执行非原子操作，先判断后增加，中间空隙可穿插其他业务逻辑的执行，最终导致结果不准确。当然这个可以使用 lua脚本 来实现

滑动窗口限流算法可以查看[Redisson实现滑动窗口计数](https://www.seven97.top/database/redis/07-practice-addone.html#扩展：Redisson实现滑动窗口计数)


**令牌桶算法**：也可以将令牌数量放到 Redis 中。不过这样的方式会导致每一个请求都需要去 Redis 判断一下能不能通过，在性能上有一定的损耗。所以有个优化点就是**批量获取**，每次取令牌不是一个一取，而是取一批，不够了再去取一批，这样可以减少对 Redis 的请求。

不过要注意一点，批量获取会导致一定范围内的限流误差。比如取了10 个此时不用，等下一秒再用，那同一时刻集群机器总处理量可能会超过阈值。

其实「批量」这个优化点太常见了，不论是 MySQL的批量刷盘，还是Kafka 消息的批量发送还是分布式 ID 的高性能发号，都包含了「批量」的思想



## 限流的难点

可以看到，每个限流都有个阈值，这个阈值如何定是个难点。

定大了服务器可能顶不住，定小了就“误伤”了，没有资源利用最大化，对用户体验不好。

可以对历史数据进行分析，找到误伤和限流的平衡点。或者在限流上线之后先预估个大概的阈值，然后不执行真正的限流操作，而是采取日志记录方式，对日志进行分析查看限流的效果，然后调整阈值，推算出集群总的处理能力，和每台机子的处理能力(方便扩缩容)，然后将线上的流量进行重放，测试真正的限流效果，最终值确定，然后上线。

或者基于TCP拥塞控制的思想，根据请求响应在一个时间段的响应时间P90或者P99值来确定此时服务器的健康状况，来进行动态限流。在 Ease Gateway产品中实现了这套算法，有兴趣的同学可以自行搜索；

其实真实的业务场景很复杂，需要限流的条件和资源很多，每个资源限流要求还不一样


## 限流组件

一般而言，我们不需要自己实现限流算法来达到限流的目的，不管是接入层限流还是细粒度的接口限流，都有现成的轮子使用，其实现也是用了上述我们所说的限流算法。

比如 Google Guava 提供的限流工县类 Ratelimiter ，是基于令牌桶实现的，并目扩展了算法，支持预热功能。

阿里开源的限流框架 sentinel中的匀速排队限流策略，就采用了漏桶算法。

Nginx 中的限流模块 Limit_reg_zone，采用了漏桶算法，还有 OpenResty 中的 resty.limit.reg 库等等

具体的使用还是很简单的，有兴趣的同学可以自行搜索，对内部实现感兴趣的同学可以下个源码看看，学习下生产级别的限流是如何实现的。




<!-- @include: @article-footer.snippet.md -->     

