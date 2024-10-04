---
title: 策略 - 内存淘汰策略
category: 数据库
tag:
 - Redis
 - 缓存
head:
  - - meta
    - name: keywords
      content: redis,redis内存淘汰策略,LRU,LFU
  - - meta
    - name: description
      content: 全网最全的Redis知识点总结，让天下没有难学的八股文！
---



## 概述

过期删除策略，是删除已过期的 key，而当 Redis 的运行内存已经超过 Redis 设置的最大内存之后，则会使用内存淘汰策略删除符合条件的 key，以此来保障 Redis 高效的运行。这两个机制虽然都是做删除的操作，但是触发的条件和使用的策略都是不同的。

- **数据过期**，是符合业务预期的一种数据删除机制，为记录设定过期时间，过期后从缓存中移除。

- **数据淘汰**，是一种“*有损自保*”的`降级策略`，是业务预期之外的一种数据删除手段。指的是所存储的数据没达到过期时间，但缓存空间满了，对于新的数据想要加入内存时，为了避免OOM而需要执行的一种应对策略。



## 设置 Redis 最大运行内存
在配置文件redis.conf 中，可以通过参数 maxmemory &lt;bytes> 来设定最大内存，当数据内存达到 maxmemory 时，便会触发redis的内存淘汰策略



## 内存淘汰策略

### 不进行数据淘汰
noeviction：不会淘汰任何数据。  

当使用的内存空间超过 maxmemory 值时，也不会淘汰任何数据，但是再有写请求时，则返回OOM错误。



### 进行数据淘汰

- volatile-lru：针对**设置了过期时间的key**，使用lru算法进行淘汰。
- allkeys-lru：针对**所有key**使用lru算法进行淘汰。
- volatile-lfu：针对**设置了过期时间的key**，使用lfu算法进行淘汰。
- allkeys-lfu：针对**所有key**使用lfu算法进行淘汰。
- volatile-random：针对**设置了过期时间的key中**使用随机淘汰的方式进行淘汰。
- allkeys-random：针对**所有key**使用随机淘汰机制进行淘汰。
- volatile-ttl：针对设置了过期时间的key，**越早过期**的越先被淘汰。

`Redis`对随机淘汰和LRU策略进行的更精细化的实现，支持将淘汰目标范围细分为全部数据和设有过期时间的数据，这种策略相对更为合理一些。因为一般设置了过期时间的数据，本身就具备可删除性，将其直接淘汰对业务不会有逻辑上的影响；而没有设置过期时间的数据，通常是要求常驻内存的，往往是一些配置数据或者是一些需要当做白名单含义使用的数据（比如用户信息，如果用户信息不在缓存里，则说明用户不存在），这种如果强行将其删除，可能会造成业务层面的一些逻辑异常。



## Redis的内存淘汰算法

操作系统本身有其内存淘汰算法，针对内存页面淘汰，详情请看 [内存的页面置换算法](https://www.seven97.top/cs-basics/operating-system/memorypagereplacementalgorithm.html "内存的页面置换算法")

### LRU 算法
LRU（ Least Recently Used，最近最少使用）淘汰算法：是一种常用的页面置换算法，也就是说最久没有使用的缓存将会被淘汰。

传统的LRU 是基于链表结构实现的，链表中的元素按照操作顺序从前往后排列，最新操作的key会被移动到表头，当需要进行内存淘汰时，只需要删除链表尾部的元素即可，因为链表尾部的元素就代表最久未被使用的元素。

但是传统的LRU算法存在两个问题：
- 需要用链表管理所有的缓存数据，这会带来额外的空间开销；
- 当有数据被访问时，需要在链表上把该数据移动到头端，如果有大量数据被访问，就会带来很多链表元素的移动操作，会很耗时，进而会降低 Redis 缓存性能。

Redis 使用的是一种近似 LRU 算法，目的是为了更好的节约内存，它的实现方式是给现有的数据结构添加一个额外的字段，**用于记录此key的最后一次访问时间**。Redis 内存淘汰时，会使用随机采样的方式来淘汰数据，它是随机取 5 个值 (此值可配置) ，然后淘汰一个最少访问的key，之后把剩下的key暂存到一个池子中，继续随机取出一批key，并与之前池子中的key比较，再淘汰一个最少访问的key。以此循环，直到内存降到maxmemory之下。

```c
struct redisObject {
    unsigned type:4;
    unsigned encoding:4;
    unsigned lru:LRU_BITS; //记录 Key 的最后被访问时间
    int refcount;
    void *ptr;
};
```

在 **LRU 算法中**，Redis 对象头的 24 bits 的 lru 字段是用来记录 key 的访问时间戳，因此在 LRU 模式下，Redis可以根据对象头中的 lru 字段记录的值，来比较最后一次 key 的访问时间长，从而淘汰最久未被使用的 key。

但是 LRU 算法有一个问题，无法解决**缓存污染**问题，比如应用一次读取了大量的数据，而这些数据只会被读取这一次，那么这些数据会留存在 Redis 缓存中很长一段时间，造成缓存污染。Redis利用LFU解决这个问题

### LFU 算法

最不常用的算法是根据总访问次数来淘汰数据的，它的核心思想是“如果数据过去被访问多次，那么将来被访问的频率也更高”。

Redis 的 LFU 算法也是通过 robj 对象的 lru 字段来保存 Key 的访问频率的，**LFU 算法**把  lru 字段分为两部分，如下图：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270806979.png)

- 0 ~ 7 位：用于保存 Key 的访问频率计数器。
- 8 ~ 23 位：用于保存当前时间（以分钟计算）。

由于访问频率计数器只有8个位，所以取值范围为 0 ~ 255，如果每访问 Key 都增加 1，那么很快就用完，LFU 算法也就不起效果了。所以 Redis 使用了一种比较复杂的算法了计算访问频率，算法如下：
1. 先按照上次访问距离当前的时长，来对 logc 进行衰减；
2. 然后，再按照一定概率增加 logc 的值

具体为：
1. 在每次 key 被访问时，会先对 访问频率 做一个衰减操作，衰减的值跟前后访问时间的差距有关系，如果上一次访问的时间与这一次访问的时间差距很大，那么衰减的值就越大，这样实现的 LFU 算法是根据访问频率来淘汰数据的，而不只是访问次数。访问频率需要考虑 key 的访问是多长时间段内发生的。key 的先前访问距离当前时间越长，那么这个 key 的访问频率相应地也就会降低，这样被淘汰的概率也会更大。
2. 对 访问频率 做完衰减操作后，就开始对 访问频率 进行增加操作，增加操作并不是单纯的 + 1，而是根据概率增加，如果 访问频率 越大的 key，它的 访问频率 就越难再增加。


访问频率衰减算法：原理就是，Key 越久没被访问，衰减的程度就越大。
```c
unsigned long LFUDecrAndReturn(robj *o) {
    unsigned long ldt = o->lru >> 8;// 获取 Key 最后访问时间(单位为分钟)
    unsigned long counter = o->lru & 255;// 获取 Key 访问频率计数器的值
	// 下面是计算要衰减的数量
	// LFUTimeElapsed 函数用于获取 Key 没被访问的时间(单位为分钟)
	 // lfu_decay_time 是衰减的力度，通过配置项 lfu-decay-time 设置，值越大，衰减力度越小
    unsigned long num_periods = server.lfu_decay_time ? LFUTimeElapsed(ldt) / server.lfu_decay_time : 0;
	// 对访问频率计数器进行衰减操作
    if (num_periods)
        counter = (num_periods > counter) ? 0 : counter - num_periods;
    return counter;
}
```
从 LFUDecrAndReturn 函数可知，lfu-decay-time 设置越大，衰减的力度就越小。如果 lfu-decay-time 设置为1，并且 Key 在 10 分钟内没被访问的话，按算法可知，访问频率计数器就会被衰减10。

访问频率增加算法：
```c
uint8_t LFULogIncr(uint8_t counter) {
    if (counter == 255) return 255;
    double r = (double)rand()/RAND_MAX;// 获取随机数r
    double baseval = counter - LFU_INIT_VAL;// 计数器旧值
    if (baseval < 0) baseval = 0;
    double p = 1.0/(baseval*server.lfu_log_factor+1);// 根据计数器旧值与影响因子来计算出p
    if (r < p) counter++;// 如果 p 大于 r, 那么对计数器进行加以操作
    return counter;
}
```

LFU 算法更新 lru 字段和LRU 算法更新lru字段都是在 lookupKey 函数中完成的
```c
robj *lookupKey(redisDb *db, robj *key, int flags) {
    dictEntry *de = dictFind(db->dict,key->ptr);
    robj *val = NULL;
    if (de) {
        val = dictGetVal(de);
       //……
    }

    if (val) {
       //……
        if (!hasActiveChildProcess() && !(flags & LOOKUP_NOTOUCH)){
            if (server.maxmemory_policy & MAXMEMORY_FLAG_LFU) {// 如果配置的是LFU淘汰算法
                updateLFU(val);// 更新LFU算法的统计信息
            } else {// 如果配置的是LRU淘汰算法
                val->lru = LRU_CLOCK();// 更新 Key 最后被访问时间
            }
        }

       //……
    } else {
        //……
    }

    return val;
}
```



<!-- @include: @article-footer.snippet.md -->     

