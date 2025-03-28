---
title: Redis中的常见延迟问题
category: 数据库
tags:
  - Redis
  - 缓存
head:
  - - meta
    - name: keywords
      content: redis,redis调优,bigkey,持久化
  - - meta
    - name: description
      content: 全网最全的Redis知识点总结，让天下没有难学的八股文！
---



##  使用复杂度高的命令
Redis提供了慢日志命令的统计功能

首先设置Redis的慢日志阈值，只有超过阈值的命令才会被记录，这里的单位是微妙，例如设置慢日志的阈值为5毫秒，同时设置只保留最近1000条慢日志记录：
```shell
# 命令执行超过5毫秒记录慢日志
CONFIG SET slowlog-log-slower-than 5000
# 只保留最近1000条慢日志
CONFIG SET slowlog-max-len 1000
```

执行SLOWLOG get 5查询最近5条慢日志
```shell
127.0.0.1:6379> SLOWLOG get 5
1) 1) (integer) 32693       # 慢日志ID
   2) (integer) 1593763337  # 执行时间
   3) (integer) 5299        # 执行耗时(微秒)
   4) 1) "LRANGE"           # 具体执行的命令和参数
      2) "user_list_2000"
      3) "0"
      4) "-1"
2) 1) (integer) 32692
   2) (integer) 1593763337
   3) (integer) 5044
   4) 1) "GET"
      2) "book_price_1000"
...
```

通过查看慢日志记录，就可以知道在什么时间执行哪些命令比较耗时，如果服务请求量并不大，但Redis实例的CPU使用率很高，很有可能就是使用了复杂度高的命令导致的。

比如经常使用O(n)以上复杂度的命令，由于Redis是单线程执行命令，因此这种情况Redis处理数据时就会很耗时。例如

- sort：对列表（list）、集合（set）、有序集合（sorted set）中的元素进行排序。在最简单的情况下（没有权重、没有模式、没有 `LIMIT`），`SORT` 命令的时间复杂度近似于 `O(n*log(n))`

- sunion：用于计算两个或多个集合的并集。时间复杂度可以描述为 `O(N)`，其中 `N` 是所有参与运算集合的元素总数。如果有多个集合，每个集合有不同数量的元素参与运算，那么复杂度会是所有这些集合元素数量的总和。

- zunionstore：用于计算一个或多个有序集合的并集，并将结果存储到一个新的有序集合中。在最简单的情况下，`ZUNIONSTORE` 命令的时间复杂度是 `O(N*log(N))`，其中 `N` 是所有参与计算的集合中元素的总数。
- keys * ：获取所有的 key 操作；复杂度`O(n)`，`KEYS` 命令是一个阻塞命令，在执行时会遍历整个Redis数据库，并将所有的key一次性返回给客户端。这会导致Redis的CPU占用率飙升，甚至导致Redis的性能下降，出现阻塞或者延迟；数据量越大执行速度越慢，。可以使用`scan`命令替代
- Hgetall：返回哈希表中所有的字段和；
- smembers：返回集合中的所有成员；

解决方案就是，不使用这些复杂度较高的命令，并且一次不要获取太多的数据，每次尽量操作少量的数据，让Redis可以及时处理返回

### keys \*  的替代，scan

为了解决 `KEYS` 命令的性能问题，Redis提供了 `SCAN` 命令来代替。`SCAN` 命令是一个增量迭代器，可以分批次遍历所有的key。它不会一次性返回所有的结果，而是分多次返回部分结果，每次返回一部分key。这样可以避免Redis的阻塞，保证高性能


`SCAN` 命令的基本语法如下：
```shell
SCAN cursor [MATCH pattern] [COUNT count]
```

- `cursor`：游标，首次调用时传入`0`，后续调用返回的游标值作为下次调用的游标值。
- `MATCH pattern`：模式匹配，可以过滤key，类似`KEYS`命令的匹配方式。
- `COUNT count`：每次返回的key的数量，类似于分页。

相比`KEYS`，`SCAN` 命令有以下优势：
- **非阻塞**：`SCAN` 不会阻塞服务器，它是增量的，适合在生产环境中使用。
- **分批处理**： 每次返回部分结果，避免一次性返回过多数据导致Redis性能问题。
- **支持模式匹配** ：和 `KEYS` 命令一样，`SCAN` 支持模式匹配，能够按需筛选keys。

尽管 `SCAN` 命令具有增量的优点，但它也有一些需要注意的地方：
- **结果不完整性**：`SCAN` 命令可能会遗漏或重复某些key，这取决于Redis的执行过程。因此，在遍历过程中需要进行适当的处理，确保结果的正确性。
- **遍历时间**：如果Redis中有大量的数据，遍历的时间可能较长。虽然每次返回的数据量是有限的，但整个过程仍然可能需要较长时间

## 存储大key
如果查询慢日志发现，并不是复杂度较高的命令导致的，例如都是**SET、DELETE**操作出现在慢日志记录中，那么就要怀疑是否存在Redis写入了大key的情况。

### 多大才算大
如果一个 key 对应的 value 所占用的内存比较大，那这个 key 就可以看作是 bigkey。

- String 类型的 value 超过 1MB
- 复合类型（List、Hash、Set、Sorted Set 等）的 value 包含的元素超过 5000 个（不过，对于复合类型的 value 来说，不一定包含的元素越多，占用的内存就越多）。

### 产生原因

- 程序设计不当，比如直接使用 String 类型存储较大的文件对应的二进制数据。
- 对于业务的数据规模考虑不周到，比如使用集合类型的时候没有考虑到数据量的快速增长。
- 未及时清理垃圾数据，比如哈希中冗余了大量的无用键值对。

### 造成的问题
- 客户端超时阻塞：由于 Redis 执行命令是单线程处理，然后在操作大 key 时会比较耗时，那么就会阻塞 Redis，从客户端这一视角看，就是很久很久都没有响应。
- 网络阻塞：每次获取大 key 产生的网络流量较大，如果一个 key 的大小是 1 MB，每秒访问量为 1000，那么每秒会产生 1000MB 的流量，这对于普通千兆网卡的服务器来说是灾难性的。
- 工作线程阻塞：如果使用 del 删除大 key 时，会阻塞工作线程，这样就没办法处理后续的命令。
- 持久化阻塞（磁盘IO）：对[AOF 日志](https://www.seven97.top/database/redis/03-strategy1-persistence.html#aof%E6%8C%81%E4%B9%85%E5%8C%96)的影响
	- 使用Always 策略的时候，`主线程`在执行完命令后，会把数据写入到 AOF 日志文件，然后会调用 fsync() 函数，将内核缓冲区的数据直接写入到硬盘，等到硬盘写操作完成后，该函数才会返回。因此当使用 Always 策略的时候，如果写入是一个大 Key，`主线程`在执行 fsync() 函数的时候，阻塞的时间会比较久，因为当写入的数据量很大的时候，数据同步到硬盘这个过程是很耗时的。
	- 另外两种策略都不影响主线程

大 key 造成的阻塞问题还会进一步影响到主从同步和集群扩容。

### 如何发现 bigkey？
1. 使用 Redis 自带的 --bigkeys 参数来查找：这个命令会扫描(Scan) Redis 中的所有 key ，会对 Redis 的性能有一点影响，最好选择在从节点上执行该命令，因为主节点上执行时，会**阻塞**主节点。并且，这种方式只能找出每种数据结构 top 1 bigkey（占用内存最大的 String 数据类型，包含元素最多的复合数据类型）。然而，一个 key 的元素多并不代表占用内存也多，需要我们根据具体的业务情况来进一步判断。

2. Redis 自带的 SCAN 命令：SCAN 命令可以按照一定的模式和数量返回匹配的 key。获取了 key 之后，可以利用 STRLEN、HLEN、LLEN等命令返回其长度或成员数量。
    ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270810931.png)

3. 借助开源工具分析 RDB 文件：这种方案的前提是Redis 采用的是 RDB 持久化。网上有现成的工具：

4. - [redis-rdb-tools](https://github.com/sripathikrishnan/redis-rdb-tools)：Python 语言写的用来分析 Redis 的 RDB 快照文件用的工具
   - [rdb_bigkeys](https://github.com/weiyanwei412/rdb_bigkeys)：Go 语言写的用来分析 Redis 的 RDB 快照文件用的工具，性能更好。

### 如何处理 bigkey？
- 删除大 key：删除大 key 时建议采用分批次删除和异步删除的方式进行；

  - 因为删除大 key释放内存只是第一步，为了更加高效地管理内存空间，在应用程序释放内存时，**操作系统需要把释放掉的内存块插入一个空闲内存块的链表**，以便后续进行管理和再分配。这个过程本身需要一定时间，而且会**阻塞**当前释放内存的应用程序。

  - 所以，如果一下子释放了大量内存，空闲内存块链表操作时间就会增加，相应地就会造成 Redis **主线程的阻塞**，如果主线程发生了阻塞，其他所有请求可能都会超时，超时越来越多，会造成 Redis 连接耗尽，产生各种异常。

- 分割 bigkey：将一个 bigkey 分割为多个小 key。例如，将一个含有上万字段数量的 Hash 按照一定策略（比如二次哈希）拆分为多个 Hash。

- 手动清理：Redis 4.0+ 可以使用 UNLINK 命令来异步删除一个或多个指定的 key。Redis 4.0 以下可以考虑使用 SCAN 命令结合 DEL 命令来分批次删除。

- 采用合适的数据结构：例如，文件二进制数据不使用 String 保存、使用 HyperLogLog 统计页面 UV、Bitmap 保存状态信息（0/1）。

- 开启 lazy-free（惰性删除/延迟释放） ：lazy-free 特性是 Redis 4.0 开始引入的，指的是让 Redis 采用异步方式延迟释放 key 使用的内存，将该操作交给单独的子线程处理，避免阻塞主线程。



## 集中过期

Redis的过期策略采用  定期过期+懒惰过期两种策略：
- 定期过期：Redis内部维护一个定时任务，默认每秒进行10次(也就是每隔100毫秒一次)过期扫描，从过期字典中随机取出20个key，删除过期的key，如果过期key的比例还超过25%，则继续获取20个key，删除过期的key，循环往复，直到过期key的比例下降到25%或者这次任务的执行耗时超过了25毫秒，才会退出循环
- 懒惰过期：只有当访问某个key时，才判断这个key是否已过期，如果已经过期，则从实例中删除

Redis的定期删除策略是在Redis`主线程`中执行的，也就是说如果在执行定期删除的过程中，出现了需要大量删除过期key的情况，那么在业务访问时，必须等这个定期删除任务执行结束，才可以处理业务请求。此时就会出现，业务访问延时增大的问题，最大延迟为25毫秒。

为了尽量避免这个问题，在设置过期时间时，可以给过期时间设置一个随机范围，避免同一时刻过期。

伪代码可以这么写：
```shell
# 在过期时间点之后的5分钟内随机过期掉
redis.expireat(key, expire_time + random(300))
```

## 实例内存达到上限

生产中会给内存设置上限maxmemory，当数据内存达到 maxmemory 时，便会触发redis的内存淘汰策略

那么当实例的内存达到了maxmemory后，就会发现之后每次写入新的数据，就好像变慢了。导致变慢的原因是，当Redis内存达到maxmemory后，每次写入新的数据之前，会先根据内存淘汰策略先踢出一部分数据，让内存维持在maxmemory之下。

而内存淘汰策略就决定这个踢出数据的时间长短：
- 最常使用的一般是allkeys-lru或volatile-lru策略，Redis 内存淘汰时，会使用随机采样的方式来淘汰数据，它是随机取 5 个值 (此值可配置) ，然后淘汰一个最少访问的key，之后把剩下的key暂存到一个池子中，继续随机取出一批key，并与之前池子中的key比较，再淘汰一个最少访问的key。以此循环，直到内存降到maxmemory之下。
- 如果使用的是allkeys-random或volatile-random策略，那么就会快很多，因为是随机淘汰，那么就少了比较key访问频率时间的消耗了，随机拿出一批key后直接淘汰即可，因此这个策略要比上面的LRU策略执行快一些。

但以上这些淘汰策略的逻辑都是在访问Redis时，真正命令执行之前执行的，也就是它会影响真正需要执行的命令。

另外，如果此时Redis实例中有存储大key，那么在**淘汰大key释放内存时，这个耗时会更加久，延迟更大**



## AOF持久化

### 同步持久化

当 Redis 直接记录 AOF 日志时，如果有大量的写操作，并且配置为[同步持久化](https://www.seven97.top/database/redis/03-strategy1-persistence.html#aof%E6%8C%81%E4%B9%85%E5%8C%96)

```shell
appendfsync always
```

即每次发生数据变更会被立即记录到磁盘，并且Always写回策略是由`主进程`执行的，而写磁盘比较耗时，性能较差，所以有时会阻塞主线程。



### [AOF重写](https://www.seven97.top/database/redis/03-strategy1-persistence.html#aof-%E9%87%8D%E5%86%99%E6%9C%BA%E5%88%B6)

1. fork 出一条子线程来将文件重写，在执行 `BGREWRITEAOF` 命令时，Redis 服务器会维护一个 AOF 重写缓冲区，该缓冲区会在子线程创建新 AOF 文件期间，记录服务器执行的所有写命令。
2. 当子线程完成创建新 AOF 文件的工作之后，服务器会将重写缓冲区中的所有内容追加到新 AOF 文件的末尾，使得新的 AOF 文件保存的数据库状态与现有的数据库状态一致。
3. 最后，服务器用新的 AOF 文件替换旧的 AOF 文件，以此来完成 AOF 文件重写操作。

阻塞就是出现在第2步的过程中，将缓冲区中新数据写到新文件的过程中会产生**阻塞**。



## fork耗时

生成RDB和AOF重写都需要父进程fork出一个子进程进行数据的持久化，在fork执行过程中，父进程需要拷贝内存页表给子进程，如果整个实例内存占用很大，那么需要拷贝的内存页表会比较耗时，此过程会消耗大量的CPU资源，在完成fork之前，整个实例会被阻塞住，无法处理任何请求，如果此时CPU资源紧张，那么fork的时间会更长，甚至达到秒级。这会严重影响Redis的性能。

Redis 在进行 RDB 快照的时候，会调用系统函数 fork() ，创建一个子线程来完成临时文件的写入，而触发条件正是配置文件中的 save 配置。当达到配置时，就会触发 bgsave 命令创建快照，这种方式是不会阻塞主线程的，而手动执行 save 命令会在主线程中执行，**阻塞**主线程。

除了因为备份的原因生成RDB之外，在【主从复制】第一次建立连接全量复制时，主节点也会生成RDB文件给从节点进行一次全量同步，这时也会对Redis产生性能影响。

要想避免这种情况，需要规划好数据备份的周期，建议在**从节点上执行备份**，而且最好放在低峰期执行。如果对于丢失数据不敏感的业务，那么不建议开启AOF和AOF重写功能。

## 集群扩容

Redis 集群可以进行节点的动态扩容缩容，这一过程目前还处于**半自动**状态，需要人工介入。

在扩缩容的时候，需要进行数据迁移。而 Redis 为了保证迁移的一致性，迁移所有操作都是**同步**操作。

执行迁移时，两端的 Redis 均会进入时长不等的**阻塞**状态，对于小Key，该时间可以忽略不计，但如果一旦 Key 的内存使用过大，严重的时候会触发集群内的故障转移，造成不必要的切换。



## 总结
1. 使用复杂度高的命令，执行命令时就会耗时
2. 存储大key：如果一个key写入的数据非常大，Redis在分配内存、删除大key时都会耗时，并且持久化AOF的写回策略是always时会影响Redis性能
3. 集中过期：Redis的主动过期的定时任务，是在Redis`主线程`中执行的，最差的情况下会有25ms的阻塞
4. 实例内存达到上限时，淘汰策略的逻辑都是在访问Redis时，真正命令执行之前执行的，也就是它会影响真正需要执行的命令。
5. fork耗时：生成RDB和AOF重写都需要父进程fork出一个子进程进行数据的持久化，如果整个实例内存占用很大，那么需要拷贝的内存页表会比较耗时

额外总结大key的影响：
1. 如果一个key写入的数据非常大，Redis在分配内存、删除大key时都会耗时。
2. 当实例内存达到上限时，在淘汰大key释放内存时，内存淘汰策略的耗时会更加久，延迟更大
3. AOF持久化时，使用always机制，这个操作是在`主线程`中执行的，如果写入是一个大 Key，主线程在执行 fsync() 函数的时候，阻塞的时间会更久。
4. 生成RDB和AOF重写时会fork出一个子进程进行数据的持久化，父进程需要拷贝内存页表给子进程，如果整个实例内存占用很大，那么需要拷贝的内存页表会比较耗时。



<!-- @include: @article-footer.snippet.md -->     
