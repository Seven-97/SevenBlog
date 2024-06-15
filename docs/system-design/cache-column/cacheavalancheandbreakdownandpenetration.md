---
title: 缓存雪崩、击穿、穿透
category: 系统设计
tag:
 - Redis
 - 缓存
---



> 来源：[小林Coding](https://www.xiaolincoding.com/redis/)，Seven进行了部分补充完善



## 概述

用户的数据一般都是存储于数据库，数据库的数据是落在磁盘上的，磁盘的读写速度可以说是计算机里最慢的硬件了。
当用户的请求，都访问数据库的话，请求数量一上来，数据库很容易就奔溃的了，所以为了避免用户直接访问数据库，会用 Redis 作为缓存层。因为 Redis 是内存数据库，我们可以将数据库的数据缓存在 Redis 里，相当于数据缓存在内存，内存的读写速度比硬盘快好几个数量级，这样大大提高了系统性能。

当使用缓存时，通常有两个目标：第一，提升响应效率和并发量；第二，减轻数据库的压力。

而引入了缓存层，就会有缓存异常的三个问题，分别是缓存雪崩、缓存击穿、缓存穿透。这三个问题的发生，都是因为在某些特殊情况下，缓存失去了预期的功能所致。

当缓存失效或没有抵挡住流量，流量直接涌入到数据库，在高并发的情况下，就有可能可能直接击垮数据库，导致整个系统崩溃。

本章的缓存以redis为例。




## 缓存雪崩
在使用缓存时，通常会对缓存设置过期时间，一方面目的是保持缓存与数据库数据的一致性，另一方面是减少冷缓存占用过多的内存空间。

那么，当大量缓存数据**在短时间集体失效**或者 **Redis 故障宕机**时，请求全部转发到数据库，从而导致数据库压力骤增，甚至宕机，从而形成一系列连锁反应，造成整个系统崩溃，这就是**缓存雪崩**的问题。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270810306.png)

所以，发生缓存雪崩的场景通常有两个：
- 大量热点key同时过期；
- 缓存服务故障；

### 大量热点key同时过期
针对大量数据同时过期而引发的缓存雪崩问题，常见的应对方法有下面这几种：
1. **均匀失效**（建议）：将key的过期时间后面加上一个随机数（比如随机1-5分钟）
  - 如果要给缓存数据设置过期时间，应该避免将大量的数据设置成同一个过期时间。就可以给这些数据的过期时间加上一个随机数，这样就保证数据不会在同一时间过期。
2. 考虑用队列或者互斥锁的方式，保证缓存单线程写，但这种方案可能会影响并发量，**不建议**。
  - 当业务线程在处理用户请求时，如果发现访问的数据不在 Redis 里，就加个互斥锁，保证同一时间内只有一个请求来构建缓存（从数据库读取数据，再将数据更新到 Redis 里。也就不会有大量的热点数据需要从数据库读取数据了），即保证缓存单线程写。
  - 实现互斥锁的时候，最好设置超时时间，防止线程出现意外一直阻塞导致其它请求也无法获取锁。
3. 可以让热点数据永久有效，后台异步更新缓存，适用于不严格要求缓存一致性的场景。
  - 事实上即使数据永久有效，数据也不一定会一直在内存中，这是因为 [Redis的内存淘汰策略](https://www.seven97.top/database/redis/03-strategy3-memoryelimination.html) ，当系统内存紧张的时候，有些缓存数据会被“淘汰”。如果此时用户读取的是淘汰数据，那就有可能会返回空值，误以为数据丢失了。解决方式：
  	- 后台频繁地检测缓存是否有效，如果检测到缓存失效了，那就从数据库读取数据，并更新到缓存。但这个频繁 的时间不好掌握，总会有时间间隔，间隔时间内就有可能导致空值的返回；
  	- 用户读取数据时，发现数据不在Redis中，则通知后台线程更新缓存。后台线程收到消息后，发现数据不存在就读取数据库数据，并将数据加载到缓存。
4. 双key策略，主key设置过期时间，备key不设置过期时间，当主key失效时，直接返回备key值。
  - 这个只是 key 不一样，但是 value 值是一样的，相当于给缓存数据做了个副本。但是在更新缓存时，需要同时更新「主 key 」和「备 key 」的数据。
  - 双 key 策略的好处是，当主 key 过期了，有大量请求获取缓存数据的时候，直接返回备 key 的数据，这样可以快速响应请求。而不用因为 key 失效而导致大量请求被锁阻塞住（采用了互斥锁，仅一个请求来构建缓存），后续再通知后台线程，重新构建主 key 的数据。
  - 但是需要同时存储两份数据，增大了内存开销

### 缓存服务故障
1. 构建缓存高可用集群（针对缓存服务故障情况）。
	- 如果 Redis 缓存的主节点故障宕机，从节点可以切换成为主节点，继续提供缓存服务，避免了由于 Redis 故障宕机而导致的缓存雪崩问题
2. 当缓存雪崩发生时，可以实行服务熔断、限流、降级等措施进行保障。
	- 服务熔断机制，就是暂停业务应用对缓存服务的访问，直接返回错误，也就不用再继续访问数据库，从而降低对数据库的访问压力，保证数据库系统的正常运行，然后等到 Redis 恢复正常后，再允许业务应用访问缓存服务。这种方式就是暂停了业务访问
	- 请求限流机制，就是只将少部分请求发送到数据库进行处理，再多的请求就在入口直接拒绝服务，等到 Redis 恢复正常并把缓存预热完后，再解除请求限流的机制。



## 缓存击穿

缓存雪崩是指**大量热点key**同时失效的情况，如果是**单个热点key**，一直都有着大并发访问，那么在这个key失效的瞬间，这个大并发请求就会击破缓存，直接请求到数据库，好像蛮力击穿一样。这种情况就是缓存击穿（Cache Breakdown）。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270810159.png)

`缓存击穿`和前面提到的`缓存雪崩`产生的原因其实很相似。**区别点**在于：

- 缓存雪崩是**大面积的缓存失效**导致大量请求涌入数据库。
- 缓存击穿是**少量缓存失效**的时候恰好失效的数据**遭遇大并发量的请求**，导致这些请求全部涌入数据库中。

因此可以将缓存击穿视为缓存雪崩的子集，应对方案也是缓存雪崩说到的方案。



解决方案：

1. 过期时间**续期**：比如每次请求的时候自动将过期时间续期一下

2. 使用互斥锁（Mutex Key）：当缓存不可用时，仅`持锁的线程`负责从数据库中查询数据并写入缓存中，其余请求重试时先尝试从缓存中获取数据，避免所有的并发请求全部同时打到数据库上。步骤如下：

  1. 没有命中缓存的时候，先请求获取分布式锁，获取到分布式锁的线程，执行`DB查询`操作，然后将查询结果写入到缓存中；

  2. 没有抢到分布式锁的请求，原地`自旋等待`一定时间后进行再次重试；

  3. 未抢到锁的线程，再次重试的时候，先尝试去缓存中获取下是否能获取到数据，如果可以获取到数据，则`直接取缓存`已有的数据并返回；否则重复上述`1`、`2`、`3`步骤。

    ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270810586.png)

3. 逻辑过期：热点数据不设置过期时间，后台异步更新缓存，适用于**不严格要求缓存一致性**的场景。
    ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270810346.png)



对于业务中最常使用的`旁路型缓存`而言，通常会先读取缓存，如果不存在则去数据库查询，并将查询到的数据添加到缓存中，这样就可以使得后面的请求继续命中缓存。

但是这种常规操作存在个“漏洞”，因为大部分缓存容量有限制，且很多场景会基于`LRU策略`进行内存中热点数据的淘汰，假如有个恶意程序(比如爬虫)一直在刷历史数据，容易将内存中的热点数据变为历史数据，导致真正的用户请求被打到数据库层。

针对这种场景，在缓存的设计时，需要考虑到对这种**冷数据的加热机制**进行一些额外处理，如设定一个门槛，如果指定时间段内对一个冷数据的访问次数达到阈值，则将冷数据加热，添加到热点数据缓存中，并设定一个独立的过期时间，来解决此类问题。

比如上面的例子中，可以约定同一秒内对某条冷数据的请求超过`10次`，则将此条冷数据加热作为**临时热点**数据存入缓存，设定缓存过期时间为30天。通过这样的机制，来解决冷数据的突然窜热对系统带来的不稳定影响。



## 缓存穿透

缓存穿透（cache penetration）是用户访问的数据既不在缓存当中，也不在数据库中。出于容错的考虑，如果从底层数据库查询不到数据，则不写入缓存。这就导致每次请求都会到底层数据库进行查询，缓存也失去了意义。当高并发或有人利用不存在的Key频繁攻击时，数据库的压力骤增，甚至崩溃，这就是缓存穿透问题。

*缓存穿透*与*缓存击穿*同样非常相似，区别点在于**缓存穿透**的实际请求数据在数据库中也没有，而**缓存击穿**是仅仅在缓存中没命中，但是在数据库中其实是存在对应数据的。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270810948.png)

发生场景：
- 原来数据是存在的，但由于某些原因（误删除、主动清理等）在缓存和数据库层面被删除了，但前端或前置的应用程序依旧保有这些数据；
- 黑客恶意攻击，外部爬虫，故意大量访问某些读取不存在数据的业务；

缓存穿透解决方案：

1. 缓存空值（null）或默认值
	- 分析业务请求，如果是正常业务请求时发生缓存穿透现象，可针对相应的业务数据，在数据库查询不存在时，将其缓存为空值（null）或默认值，但是需要注意的是，针对空值的缓存失效时间不宜过长，一般设置为5分钟之内。当数据库被写入或更新该key的新数据时，缓存必须同时被刷新，避免数据不一致。
	![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270810583.png)
2. 业务逻辑前置校验
	- 在业务请求的入口处进行数据合法性校验，检查请求参数是否合理、是否包含非法值、是否恶意请求等，提前有效阻断非法请求。比如，根据年龄查询时，请求的年龄为-10岁，这显然是不合法的请求参数，直接在参数校验时进行判断返回。
3. 使用 [布隆过滤器](https://www.seven97.top/cs-basics/data-structure/bloomfilter.html)快速判断数据不存在（推荐）
	- 在写入数据时，使用布隆过滤器进行标记（相当于设置白名单），业务请求发现缓存中无对应数据时，可先通过查询布隆过滤器判断数据是否在白名单内（布隆过滤器可以判断数据一定不存在），如果不在白名单内，则直接返回空或失败。
	![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270810474.png)
4. 用户黑名单限制：当发生异常情况时，实时监控访问的对象和数据，分析用户行为，针对故意请求、爬虫或攻击者，进行特定用户的限制；
5. 添加反爬策略：比如添加请求签名校验机制、比如添加IP访问限制策略等等



## 总结

缓存异常会面临的三个问题：缓存雪崩、击穿和穿透。

缓存雪崩和缓存击穿主要原因是数据不在缓存中，而导致大量请求访问了数据库，数据库压力骤增，容易引发一系列连锁反应，导致系统奔溃。缓存雪崩是由于大量rediankey不在缓存中，缓存击穿是由于单一热点key不在缓存中。

缓存雪崩和缓存击穿的问题，一旦数据被重新加载回缓存，应用又可以从缓存快速读取数据，不再继续访问数据库，数据库的压力也会瞬间降下来。因此，缓存雪崩和缓存击穿应对的方案比较类似。

而缓存穿透主要原因是数据既不在缓存也不在数据库中。

三个问题的场景及解决方案：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270810415.png)