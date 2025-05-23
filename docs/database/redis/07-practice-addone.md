---
title: Redis实现高并发场景下的计数器设计
category: 数据库
tags:
  - Redis
  - 缓存
head:
  - - meta
    - name: keywords
      content: redis,计数器,string
  - - meta
    - name: description
      content: 全网最全的Redis知识点总结，让天下没有难学的八股文！
---



大部分互联网公司都需要处理计数器场景，例如风控系统的请求频控、内容平台的播放量统计、电商系统的库存扣减等。

传统方案一般会直接使用`RedisUtil.incr(key)`，这是最简单的方式，但这种方式在生产环境中会暴露严重问题：

```java
// 隐患示例
public long addOne(String key) {
    Long result = RedisUtil.incr(key); 
    // 若未设置TTL，key将永久驻留内存
    return result;
}
```

> INCR 有**自动初始化机制**，即当 Redis 检测到目标 key 不存在时，会**自动将其初始化为 0**，再执行递增操作



## 高可用计数器的实现

### 原子操作保障计数准确性

#### NX+EX 原子初始化

```java
RedisUtil.set(key, "0", "nx", "ex", time);
```

通过Redis的`SET key value NX EX`命令，实现原子化的"不存在即创建+**设置过期时间**"，避免多个线程竞争初始化导致数据覆盖（如线程A初始化后，线程B用SET覆盖值为0）

Redis单线程模型保证命令原子性，无需额外分布式锁

使用setnx命令来设置了过期时间，防止key永不过期



#### INCR 原子递增

```java
long result = RedisUtil.incr(key);
```

先setnx命令后，再使用INCR来执行递增操作

即：

```java
public void addOne(String key) {
    RedisUtil.set(key, "0", "nx", "ex", time);
    Long result = RedisUtil.incr(key); 
	return result;
}
```



### 双重补偿机制解决过期异常

但只是使用以上两个命令还是有可能导致并发安全问题。

例如：

当两个线程同时执行 SETNX 时，未抢到初始化的线程直接执行INCR，导致key存在但无TTL

如果有一个线程A正在执行`SET key 0 NX EX 60` ，而线程B也执行方法addOne，此时线程A正在执行，线程B无法执行set操作，会直接继续执行后续命令（如 INCR），此时若线程A由于网络抖动等原因初始化key失败，那就有可能导致 key 永不过期。因此需要有补偿机制，完成redis key超时时间的设置

> 注意：当 SETNX 命令无法执行（即目标 key 已存在时），会直接继续执行后续命令（如 INCR），而不会阻塞等待



#### 首次递增补偿

因此可以通过判断`result == 1`来识别是否是首次递增，如果是首次递增的话，则强制续期

```java
if (result == 1) {
    RedisUtil.expire(key, time);
}
```



#### TTL异常检测补偿

在**极端场景**下(Redis主从切换、命令执行异常导致TTL丢失)，key 可能因未设置或过期时间丢失而长期存在

```java
if (RedisUtil.ttl(key) == -1) {
    RedisUtil.expire(key, time);
}
```

检查 TTL 是否为 `-1`（-1表示无过期时间），重新设置过期时间，作为兜底保护。



经过双重补偿机制后的代码如下：

```java
public void addOne(String key) {
    RedisUtil.set(key, "0", "nx", "ex", time);
    Long result = RedisUtil.incr(key); 
    //解决并发问题，否则会导致计数器永不清空
    //如果incr的结果为1，有两个结果，先进行set操作，此时有过期时间。第二种：直接执行incr操作，此时的redisKey没有过期时间。所以需要补偿处理
    if (result == 1) {
         RedisUtil.expire(key, time);
    }

    // 检查是否有过期时间， 对异常没有设置过期时间的key补偿
    if (RedisUtil.ttl(key) == -1) {
         RedisUtil.expire(key, time);
    }
    return result;
}
```





### 异常处理与降级策略

有时候可能会因网络抖动、服务短暂不可用、主备切换等**暂时性故障**，导致Redis操作失败，因此可以对这中异常进行处理，将需要完成的操作放入到队列中，再使用一个线程循环重试，保证最终一致性

```java
public void addOne(String key) {
    Long result = 1;
    try{
        RedisUtil.set(key, "0", "nx", "ex", time);
        result = RedisUtil.incr(key); 
        //解决并发问题，否则会导致计数器永不清空
        //如果incr的结果为1，有两个结果，先进行set操作，此时有过期时间。第二种：直接执行incr操作，此时的redisKey没有过期时间。所以需要补偿处理
        if (result == 1) {
             RedisUtil.expire(key, time);
        }

        // 检查是否有过期时间， 对异常没有设置过期时间的key补偿
        if (RedisUtil.ttl(key) == -1) {
             RedisUtil.expire(key, time);
        }
    } catch (Exception e) {
        //丢到重试队列中，一直重试
    	queue.offer(key); 
	}
    return result;
}
```



## 架构设计示意图



```mermaid
graph TD
    A[客户端请求] --> B{Key存在?}
    B -->|否| C[SET NX EX初始化]
    B -->|是| D[INCR原子递增]
    C --> D
    D --> E{result=1?}
    E -->|是| F[补偿设置TTL]
    E -->|否| G[检查TTL]
    G -->|TTL=-1| H[二次补偿]
    G -->|TTL正常| I[返回结果]
    H --> I
    F --> I
```

## 关键机制对比

| 机制         | 解决的问题           | Redis特性利用    | 性能影响    |
| ------------ | -------------------- | ---------------- | ----------- |
| SET NX EX    | 并发初始化竞争       | 原子单命令       | O(1)        |
| INCR         | 计数不准确/超卖      | 原子递增         | O(1)        |
| TTL双重补偿  | Key永不过期          | EXPIRE命令幂等性 | 额外1次查询 |
| 异常队列重试 | 网络抖动/Redis不可用 | 最终一致性       | 异步处理    |



这个方案充分挖掘了Redis原子命令的潜力，通过补偿机制弥补分布式系统的不确定性，最终在简单与可靠之间找到平衡点。