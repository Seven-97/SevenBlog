---
title: 底层原理 - Mysql的三大日志
category: 数据库
tag:
 - Mysql
---



## 概述

- undo log（回滚日志）：是 Innodb 存储引擎层生成的日志，实现了事务中的**原子性**，主要用于事务回滚和 MVCC。
- redo log（重做日志）：是 Innodb 存储引擎层生成的日志，实现了事务中的**持久性**，主要用于掉电等故障恢复；
- binlog （归档日志）：是 Server 层生成的日志，主要用于数据备份和主从复制；

## 回滚日志（undo log）
### 作用
- 保存了事务发生之前的数据的一个版本，可以用于回滚，保障原子性
- 实现多版本并发控制下的读（MVCC）的关键因素之一，也即非锁定读，MVCC通过Read View + undolog的版本链实现，可以具体看[MVCC的快照读](https://www.seven97.top/blog/53 "MVCC的快照读")

### 内容
逻辑格式的日志，在执行 undo 的时候，仅仅是将数据从逻辑上恢复至事务之前的状态，而不是从物理页面上操作实现的，这一点是不同于redo log 的。

每当 InnoDB 引擎对一条记录进行操作（修改、删除、新增）时，要把回滚时需要的信息都记录到 undo log 里，比如：
- 在**插入insert**一条记录时，要把这条记录的主键值记下来，这样之后回滚时只需要把这个主键值对应的**记录删掉delete**就好了；
- 在**删除delete**一条记录时，要把这条记录中的内容都记下来，这样之后回滚时再把由这些内容组成的**记录插入insert**到表中就好了；
- 在**更新**一条记录时，要把被更新的列的旧值记下来，这样之后回滚时再把这些列**更新为旧值**就好了。

### 什么时候产生
**事务开始之前**，MySQL 会先记录更新前的数据到 undo log 日志文件里面，当事务回滚时，可以利用 undo log 来进行回滚。同时undo 也会产生 redo 来保证undo log的可靠性。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261841443.jpeg)

### 什么时候刷盘
undo log 和数据页的刷盘策略是一样的，都需要通过 redo log 保证持久化。产生undo日志的时候，同样会伴随类似于保护事务持久化机制的redolog的产生。

buffer pool 中有 undo 页，对 undo 页的修改也都会记录到 redo log。redo log 会每秒刷盘，提交事务时也会刷盘，数据页和 undo 页都是靠这个机制保证持久化的，具体看下面内容。


## 重做日志（redo log）
### 作用
- 确保事务的持久性。  
	- 为了防止断电导致数据丢失的问题，当有一条记录需要更新的时候，InnoDB 引擎就会先更新内存（同时标记为脏页），然后将本次对这个页的修改以 redo log 的形式记录下来，这个时候更新就算完成了。也就是说， redo log 是为了防止 Buffer Pool 中的脏页丢失而设计的。  
	- 在重启mysql服务的时候，根据redo log进行重做，从而达到事务的持久性这一特性。
- 将写操作从「随机写」变成了「顺序写」，提升 MySQL 写入磁盘的性能。

### 内容
物理格式的日志，记录的是物理数据页面的修改的信息，其 redo log 是顺序写入redo log file 的物理文件中去的。同时，在内存修改 Undo log 后，也需要记录undo log对应的 redo log。

redo log 和 undo log 区别:
- redo log 记录了此次事务**完成后**的数据状态，记录的是更新之后的值；
- undo log 记录了此次事务**开始前**的数据状态，记录的是更新之前的值；

### 什么时候产生
事务开始之后就产生redo log，redo log的落盘并不是随着事务的提交才写入的，而是在事务的执行过程中，便开始写入redo log文件中。

事务提交之前发生了崩溃，重启后会通过 undo log 回滚事务，事务提交之后发生了崩溃，重启后会通过 redo log 恢复事务，如下图：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261841327.jpeg)

> redo log 要写到磁盘，数据也要写磁盘，为什么要多此一举？  
>
> 写入 redo log 的方式使用了追加操作， 所以磁盘操作是顺序写，而写入数据需要先找到写入位置，然后才写到磁盘，所以磁盘操作是随机写。磁盘的「顺序写 」比「随机写」 高效的多，因此 redo log 写入磁盘的开销更小。

### 什么时候刷盘

实际上， 执行一个事务的过程中，产生的 redo log 也不是直接写入磁盘的，因为这样会产生大量的 I/O 操作，而且磁盘的运行速度远慢于内存。


redo log有一个缓存区 Innodb_log_buffer，Innodb_log_buffer 的默认大小为 16M，每当产生一条 redo log 时，会先写入到 redo log buffer，后续再持久化到磁盘。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261841782.png)

然后会通过以下三种方式将innodb log buffer的日志刷新到磁盘:
- MySQL 正常关闭时；
- 当 redo log buffer 中记录的写入量大于 redo log buffer 内存空间的一半时，会触发落盘；
- InnoDB 的后台线程每隔 1 秒，将 redo log buffer 持久化到磁盘。
- 每次事务提交时都将缓存在 redo log buffer 里的 redo log 直接持久化到磁盘。

因此redo log buffer的写盘，并不一定是随着事务的提交才写入redo log文件的，而是随着事务的开始，逐步开始的。  

即使某个事务还没有提交，Innodb存储引擎仍然每秒会将redo log buffer刷新到redo log文件。  

这一点是必须要知道的，因为这可以很好地解释再大的事务的提交（commit）的时间也是很短暂的。

### redolog的文件
两个 redo 日志的文件名叫 ：ib_logfile0 和 ib_logfile1。

redo log文件组是以循环写的方式工作的， InnoDB 存储引擎会先写 ib_logfile0 文件，当 ib_logfile0 文件被写满的时候，会切换至 ib_logfile1 文件，当 ib_logfile1 文件也被写满时，会切换回 ib_logfile0 文件；相当于一个环形。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261841807.png)

- write pos 和 checkpoint 的移动都是顺时针方向；
- write pos ～ checkpoint 之间的部分（图中的红色部分），用来记录新的更新操作；
- check point ～ write pos 之间的部分（图中蓝色部分）：待落盘的脏数据页记录；

因此，如果 write pos 追上了 checkpoint，就意味着 redo log 文件满了，这时 MySQL 不能再执行新的更新操作，也就是说 MySQL 会被阻塞



## 二进制日志（binlog）



### 作用

- 用于复制，在主从复制中，从库利用主库上的binlog进行重放，实现主从同步。
- 用于数据库的基于时间点的还原，即备份恢复

### 内容
binlog 有 3 种格式类型，分别是 STATEMENT（默认格式）、ROW、 MIXED，区别如下：
- STATEMENT：每一条修改数据的 SQL 都会被记录到 binlog 中（相当于记录了逻辑操作，所以针对这种格式， binlog 可以称为逻辑日志），主从复制中 slave 端再根据 SQL 语句重现。但 STATEMENT 有动态函数的问题，比如你用了 uuid 或者 now 这些函数，你在主库上执行的结果并不是你在从库执行的结果，这种随时在变的函数会导致复制的数据不一致；
- ROW：记录行数据最终被修改成什么样了（这种格式的日志，就不能称为逻辑日志了），不会出现 STATEMENT 下动态函数的问题。但 ROW 的缺点是每行数据的变化结果都会被记录，比如执行批量 update 语句，更新多少行数据就会产生多少条记录，使 binlog 文件过大，而在 STATEMENT 格式下只会记录一个 update 语句而已；
- MIXED：包含了 STATEMENT 和 ROW 模式，它会根据不同的情况自动使用 ROW 模式和 STATEMENT 模式；

### 什么时候产生
事务**提交的时候**，一次性将事务中的sql语句（一个事物可能对应多个sql语句）按照一定的格式记录到binlog中。  

binlog 文件是记录了所有数据库表结构变更和表数据修改的日志，不会记录查询类的操作，比如 SELECT 和 SHOW 操作。  

这里与redo log很明显的差异就是binlog 是追加写，写满一个文件，就创建一个新的文件继续写，不会覆盖以前的日志，保存的是全量的日志。redo log 是循环写，日志空间大小是固定，全部写满就从头开始，保存未被刷入磁盘的脏页日志。  

也就是说，如果不小心整个数据库的数据被删除了，只能使用 bin log 文件恢复数据。因为redo log循环写会擦除数据。

### 主从复制的实现
MySQL 的主从复制依赖于 binlog ，也就是记录 MySQL 上的所有变化并以二进制形式保存在磁盘上。复制的过程就是将 binlog 中的数据从主库传输到从库上。

这个过程一般是异步的，也就是主库上执行事务操作的线程不会等待复制 binlog 的线程同步完成。
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261841428.jpeg)

MySQL 集群的主从复制过程如下：
- 写入 Binlog：MySQL 主库在收到客户端提交事务的请求之后，会先写入 binlog，再提交事务，更新存储引擎中的数据，事务提交完成后，返回给客户端“操作成功”的响应。
- 同步 Binlog：从库会创建一个专门的 I/O 线程，连接主库的 log dump 线程，来接收主库的 binlog 日志，再把 binlog 信息写入 relay log 的中继日志里，再返回给主库“复制成功”的响应。
- 回放 Binlog：从库会创建一个用于回放 binlog 的线程，去读 relay log 中继日志，然后回放 binlog 更新存储引擎中的数据，最终实现主从的数据一致性。

### 什么时候刷盘

在刷盘时机上与redolog不一样，redolog即使事务没提交，也可以每隔1秒就刷盘。但是一个事务的 binlog 是不能被拆开的，因此无论这个事务有多大（比如有很多条语句），也要保证一次性写入。如果一个事务的 binlog 被拆开的时候，在备库执行就会被当做多个事务分段自行，这样就破坏了原子性，是有问题的。

bin log日志与redo log类似，也有对应的缓存，叫 binlog cache。事务提交的时候，再把 binlog cache 写到 binlog 文件中。
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261841371.png)

- 图中的 write，指的就是指把日志写入到 binlog 文件，但是并没有把数据持久化到磁盘，因为数据还缓存在文件系统的 page cache 里，write 的写入速度还是比较快的，因为不涉及磁盘 I/O。
- 图中的 fsync，才是将数据持久化到磁盘的操作，这里就会涉及磁盘 I/O，所以频繁的 fsync 会导致磁盘的 I/O 升高。

MySQL提供一个 sync_binlog 参数来控制数据库的 binlog 刷到磁盘上的频率：
- sync_binlog = 0 的时候，表示每次提交事务都只 write，不 fsync，后续交由操作系统决定何时将数据持久化到磁盘；
- sync_binlog = 1 的时候，表示每次提交事务都会 write，然后马上执行 fsync；
- sync_binlog =N(N>1) 的时候，表示每次提交事务都 write，但累积 N 个事务后才 fsync。

显然，在MySQL中系统默认的设置是 sync_binlog = 0，也就是不做任何强制性的磁盘刷新指令，这时候的性能是最好的，但是风险也是最大的。因为一旦主机发生异常重启，还没持久化到磁盘的数据就会丢失。

而当 sync_binlog 设置为 1 的时候，是最安全但是性能损耗最大的设置。因为当设置为 1 的时候，即使主机发生异常重启，最多丢失一个事务的 binlog，而已经持久化到磁盘的数据就不会有影响，不过就是对写入性能影响太大。

如果能容少量事务的 binlog 日志丢失的风险，为了提高写入的性能，一般会 sync_binlog 设置为 100~1000 中的某个数值。

## 两阶段提交
事务提交后，redo log 和 binlog 都要持久化到磁盘，但是这两个是独立的逻辑，可能出现半成功的状态，这样就造成两份日志之间的逻辑不一致。如下：
1. 如果在将 redo log 刷入到磁盘之后， MySQL 突然宕机了，而 binlog 还没有来得及写入。那么机器重启后，这台机器会通过redo log恢复数据，但是这个时候binlog并没有记录该数据，后续进行机器备份的时候，就会丢失这一条数据，同时主从同步也会丢失这一条数据。
2. 如果在将 binlog 刷入到磁盘之后， MySQL 突然宕机了，而 redo log 还没有来得及写入。由于 redo log 还没写，崩溃恢复以后这个事务无效，而 binlog 里面记录了这条更新语句，在主从架构中，binlog 会被复制到从库，从库执行了这条更新语句，那么就与主库的值不一致性；

两阶段提交把单个事务的提交拆分成了 2 个阶段，分别是「准备（Prepare）阶段」和「提交（Commit）阶段」

### 具体过程

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261841899.png)
事务的提交过程有两个阶段，就是将 redo log 的写入拆成了两个步骤：prepare 和 commit，中间再穿插写入binlog，具体如下：
- prepare 阶段：将 XID（内部 XA 事务的 ID） 写入到 redo log，同时将 redo log 对应的事务状态设置为 prepare，然后将 redo log 持久化到磁盘（innodb_flush_log_at_trx_commit = 1 的作用）；
- commit 阶段：把 XID 写入到 binlog，然后将 binlog 持久化到磁盘（sync_binlog = 1 的作用），接着调用引擎的提交事务接口，将 redo log 状态设置为 commit，此时该状态并不需要持久化到磁盘，只需要 write 到文件系统的 page cache 中就够了，因为只要 binlog 写磁盘成功，就算 redo log 的状态还是 prepare 也没有关系，一样会被认为事务已经执行成功；

总的来说就是，事务提交后，redo log变成prepare 阶段，再写入binlog，返回成功后redo log 进入commit 阶段。



## 总结三个日志的具体流程

当优化器分析出成本最小的执行计划后，执行器就按照执行计划开始进行更新操作。

具体更新一条记录 UPDATE t_user SET name = 'xiaolin' WHERE id = 1; 的流程如下:
1. 检查在不在buffer Pool。 执行器负责具体执行，会调用存储引擎的接口，通过主键索引树搜索获取 id = 1 这一行记录：
	- 如果 id=1 这一行所在的数据页本来就在 buffer pool 中，就直接返回给执行器更新；
	- 如果记录不在 buffer pool，将数据页从磁盘读入到 buffer pool，返回记录给执行器。
2. 检查是否已经是要更新的值。执行器得到聚簇索引记录后，会看一下更新前的记录和更新后的记录是否一样：
	- 如果一样的话就不进行后续更新流程；
	- 如果不一样的话就把更新前的记录和更新后的记录都当作参数传给 InnoDB 层，让 InnoDB 真正的执行更新记录的操作；
3. 开启事务，记录undo log，并记录修改undo log对应的redo log：开启事务， InnoDB 层更新记录前，首先要记录相应的 undo log，因为这是更新操作，需要把被更新的列的旧值记下来，也就是要生成一条 undo log，undo log 会写入 Buffer Pool 中的 Undo 页面，不过在内存修改该 Undo 页面后，需要记录对应的 redo log。
4. 标记为脏页，并写入redo log：InnoDB 层开始更新记录，会先更新内存（同时标记为脏页），然后将记录写到 redo log 里面，这个时候更新就算完成了。为了减少磁盘I/O，不会立即将脏页写入磁盘，后续由后台线程选择一个合适的时机将脏页写入到磁盘。这就是 WAL 技术，MySQL 的写操作并不是立刻写到磁盘上，而是先写 redo 日志，然后在合适的时间再将修改的行数据写到磁盘上。
5. 至此，一条记录更新完了。
6. 记录binlog：在一条更新语句执行完成后，然后开始记录该语句对应的 binlog，此时记录的 binlog 会被保存到 binlog cache，并没有刷新到硬盘上的 binlog 文件，在事务提交时才会统一将该事务运行过程中的所有 binlog 刷新到硬盘。
7. 事务提交，redo log和binlog刷盘。



<!-- @include: @article-footer.snippet.md -->     
