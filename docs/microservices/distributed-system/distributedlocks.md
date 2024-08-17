---
title: 分布式锁实现方案
category: 微服务
tag:
 - 分布式
---



## 什么是分布式锁

> 要介绍分布式锁，首先要提到与分布式锁相对应的是线程锁、进程锁。

- **线程锁**：主要用来给方法、代码块加锁。当某个方法或代码使用锁，在同一时刻仅有一个线程执行该方法或该代码段。线程锁只在同一JVM中有效果，因为线程锁的实现在根本上是依靠线程之间共享内存实现的，比如[synchronized](https://www.seven97.top/java/concurrent/02-keyword1-synchronized.html)是共享对象头，[Lock](https://www.seven97.top/java/concurrent/03-juclock2-aqs.html)是共享某个变量（state）。
- **进程锁**：为了控制同一操作系统中多个进程访问某个共享资源，因为进程具有独立性，各个进程无法访问其他进程的资源，因此无法通过synchronized等线程锁实现进程锁。
- **分布式锁**：当多个进程不在同一个系统中(比如分布式系统中控制共享资源访问)，用分布式锁控制多个进程对资源的访问。



## 分布式锁的设计原则

> 分布式锁的最小设计原则：**安全性**和**有效性**

[Redis的官网](https://redis.io/docs/reference/patterns/distributed-locks/)上对使用分布式锁提出至少需要满足如下三个要求：

1. **互斥**（属于安全性）：在任何给定时刻，只有一个客户端可以持有锁。
2. **无死锁**（属于有效性）：即使锁定资源的客户端崩溃或被分区，也总是可以获得锁；通常通过超时机制实现。
3. **容错性**（属于有效性）：只要大多数 Redis 节点都启动，客户端就可以获取和释放锁。

除此之外，分布式锁的设计中还可以/需要考虑：

1. 加锁解锁的**同源性**：A加的锁，不能被B解锁
2. 获取锁是**非阻塞**的：如果获取不到锁，不能无限期等待；
3. **高性能**：加锁解锁是高性能的



## 分布式锁的实现方案

> 就体系的角度而言，谈谈常见的分布式锁的实现方案。

- 基于数据库实现分布式锁
  - 基于数据库表（锁表，很少使用）
  - 乐观锁(基于版本号)
  - 悲观锁(基于排它锁)
- 基于 [redis 实现分布式锁](https://www.seven97.top/database/redis/05-implementdistributedlocks.html): 
  - 单个Redis实例：setnx(key,当前时间+过期时间) + Lua
  - Redis集群模式：Redlock
- 基于 [zookeeper实现分布式锁](https://www.seven97.top/microservices/service-registration-and-discovery/zookeeper-distributedlocks.html)
  - 临时有序节点来实现的分布式锁，Curator



<!-- @include: @article-footer.snippet.md -->     



