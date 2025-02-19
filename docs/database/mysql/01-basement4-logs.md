---
title: MySQL底层真相：三大日志如何掌控数据库命运？
category: 数据库
tags:
  - Mysql
head:
  - - meta
    - name: keywords
      content: mysql,mysql数据库,mysql日志,undo log,redo log,binlog,两阶段提交
  - - meta
    - name: description
      content: 全网最全的Mysql知识点总结，让天下没有难学的八股文！
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

#### 事务回滚

逻辑格式的日志，在执行 undo 的时候，仅仅是将数据从逻辑上恢复至事务之前的状态，而不是从物理页面上操作实现的，这一点是不同于redo log 的。

那么事务如何通过Undo Log进行回滚操作呢？其实很简单，只需要在Undo Log日志中记录事务中的反向操作即可，发生回滚时直接通过Undo Log中记录的反向操作进行恢复。

每当 InnoDB 引擎对一条记录进行操作（修改、删除、新增）时，要把回滚时需要的信息都记录到 undo log 里，比如：

- 在**插入insert**一条记录时，要把这条记录的主键值记下来，这样之后回滚时只需要把这个主键值对应的**记录删掉delete**就好了；
- 在**删除delete**一条记录时，要把这条记录中的内容都记下来，这样之后回滚时再把由这些内容组成的**记录插入insert**到表中就好了；
- 在**更新**一条记录时，要把被更新的列的旧值记下来，这样之后回滚时再把这些列**更新为旧值**就好了。

#### MVCC

Undo Log 保存的是一个版本的链路，使用roll_pointer这个字段来连接的。多个事务的Undo Log 日志组成了一个版本链，如图：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856242.png)

- trx_id，当一个事务对某条聚簇索引记录进行改动时，就会把该事务的事务 id 记录在 trx_id 隐藏列里；
- roll_pointer，每次对某条聚簇索引记录进行改动时，都会把旧版本的记录写入到 undo 日志中，然后这个隐藏列是个指针，指向每一个旧版本记录，于是就可以通过它找到修改前的记录。

再说MVCC，实现了自己 Copy-On-Write思想提升并发能力的时候， 也需要数据的副本，如上图，既然已经存在了这么多Undo Log的副本，那么MVCC可以直接复用这些副本数据。

所以，Undo Log中的副本，可以用于实现多版本并发控制（MVCC），提升事务的并发性能，同时每一个事务操作自己的副本，实现事务的隔离性。

实现MVCC主要通过三个元素，一个是我们上面已经提到的Undo Log版本链，一个是readView，最后就是我们上面已经提到的这些字段。具体可以看[这篇文章](https://www.seven97.top/database/mysql/01-basement5-transactions-isolationlevel.html#readview)


### 什么时候产生
**事务开始之前**，MySQL 会先记录更新前的数据到 undo log 日志文件里面，当事务回滚时，可以利用 undo log 来进行回滚。同时undo 也会产生 redo 来保证undo log的可靠性。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261841443.jpeg)

### 什么时候刷盘
undo log 和数据页的刷盘策略是一样的，都需要通过 redo log 保证持久化。产生undo日志的时候，同样会伴随类似于保护事务持久化机制的redolog的产生。

为了 提升 Undo Log 读写性能， Undo页还存在于Buffer Pool中，因为Buffer Pool 是 InnoDB 的内存缓存，用于存储数据页和索引页，以便快速访问。它通过减少磁盘 I/O，提高数据库的整体性能。将Undo页放在缓存中，可以加速事务的回滚和数据恢复。而buffer pool 中的 undo 页的修改也都会记录到 redo log。redo log 会每秒刷盘，提交事务时也会刷盘，数据页和 undo 页都是靠这个机制保证持久化的，具体看下面内容。


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

Redo Log与Undo Log的配合作用如下：
- 若事务在提交前发生崩溃，则可以通过Undo Log回滚事务。
- 若事务在提交后发生崩溃，则可以通过Redo Log来恢复事务。

具体如下图：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261841327.jpeg)

事务开始之后就产生redo log，redo log的落盘并不是随着事务的提交才写入的，而是在事务的执行过程中，便开始写入redo log文件中。

当一个更新事务到达时，Redo Log的处理过程如下：
1. 首先，从磁盘读取原始数据到Buffer Pool（内存中），然后在内存中对数据进行修改，修改后的数据将被标记为脏页。
2. 接着，系统会生成一系列重做日志，并将其写入Redo Log Buffer，日志内容记录的是数据修改后的新值。
3. 当事务提交（commit）时，Redo Log Buffer中的内容会被刷新到Redo Log File中，并采用追加写的方式将日志记录写入文件。
4. 定期会将内存中修改过的数据刷新回磁盘，以保证数据持久性。

### 什么时候刷盘

 redo log 要写到磁盘，数据也要写磁盘，为什么要多此一举？  

 写入 redo log 的方式使用了追加操作， 所以磁盘操作是顺序写，而写入数据需要先找到写入位置，然后才写到磁盘，所以磁盘操作是随机写。磁盘的「顺序写 」比「随机写」 高效的多，因此 redo log 写入磁盘的开销更小。

但实际上， 执行一个事务的过程中，产生的 redo log 也不是直接写入磁盘的，因为这样会产生大量的 I/O 操作，而且磁盘的运行速度远慢于内存。 具体来说，当缓存页被修改（即变成脏页）后，相关的操作会先记录到Redo Log Buffer中。在事务提交（commit）时，后台线程会将Redo Log Buffer中的内容刷新到磁盘上（事务提交是Redo Log默认刷盘的时机）。此时，虽然脏页还没有写回磁盘，但只要Redo Log成功写入磁盘，就可以认为此次修改操作已完成。这是因为，即使发生故障导致脏页丢失，我们也可以通过磁盘上的Redo Log来恢复数据。也就是，Redo Log与Undo Log的配合作用如下：
- 若事务在提交前发生崩溃，则可以通过Undo Log回滚事务。
- 若事务在提交后发生崩溃，则可以通过Redo Log来恢复事务。


下面详细看下redolog的刷盘过程：

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

### redolog的写入机制
两个 redo 日志的文件名叫 ：ib_logfile0 和 ib_logfile1。

redo log文件组是以固定大小循环写的方式工作的，类似环形缓冲区。当日志文件写满时，会从头开始覆盖之前的内容。这样的设计是因为Redo Log记录的是数据页的修改，而一旦Buffer Pool中的数据页被刷写到磁盘，之前的Redo Log记录就不在有效。新的日志会覆盖这些过时的记录。

此外，硬盘上的Redo Log文件并非单一存在，而是以文件组的形式存储，每个文件的大小都相同。InnoDB 存储引擎会先写 ib_logfile0 文件，当 ib_logfile0 文件被写满的时候，会切换至 ib_logfile1 文件，当 ib_logfile1 文件也被写满时，会切换回 ib_logfile0 文件；相当于一个环形。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261841807.png)

在写入数据的同时，也需要执行擦除操作。Redo Log成功刷盘到磁盘后，才可以进行擦除。因此，我们使用两个指针来管理这一过程：
- write pos：表示当前日志记录写入的位置，即当前Redo Log文件已写到哪里。
- checkpoint：表示当前可以擦除的位置，即Redo Log文件中哪些记录已不在需要，可以被新的日志覆盖。

write pos 和 checkpoint 的移动都是顺时针方向；

write pos ～ checkpoint 之间的部分（图中的红色部分），表示剩余的可写入空间，即Redo Log文件的空闲/可用部分，用来记录新的更新操作；check point ～ write pos 之间的部分（图中蓝色部分），表示待落盘的脏数据页记录；当进行数据页刷盘操作（checkpoint）时，checkpoint指针会顺时针移动，覆盖掉已写入的蓝色区域，使其变为红色。


因此，如果 write pos 追上了 checkpoint，就意味着 redo log 文件满了，这时 MySQL 不能再执行新的更新操作，也就是说 MySQL 会被阻塞，此时必须强制执行checkpoint操作，刷新Buffer Pool中的脏页并将其写入磁盘。随后，checkpoint指针会被移动，这样就可以继续向Redo Log文件中写入新的数据。

### Redo Log Buffer刷盘策略

Redo Log Buffer刷盘策略主分为三种，由核心参数innodb_flush_log_at_trx_commit来控制。具体的流程如同下图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412142219501.png)

- **innodb_flush_log_at_trx_commit=0**

刷盘时机：InnoDB每秒钟会将日志缓存（Redo Log Buffer）刷新到磁盘，而不是在每次事务提交时进行刷新。即便事务已提交，日志仅会写入内存中的日志缓冲区，1秒钟后由后台线程将其写入磁盘。

持久性：如果发生数据库崩溃，可能会丢失最近1秒内的事务数据。

性能：这种方式提供最高的性能，因为磁盘写入的频率最小，但相应的持久性较弱。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202412142220755.png)

- **innodb_flush_log_at_trx_commit=1（默认设置）**

当设置为1时，表示每次事务提交时，都会执行刷盘操作。这意味着在默认配置下，系统提供高可靠性，但性能较低。

刷盘时机：每当事务提交时，InnoDB会将日志缓冲区的内容写入到文件系统缓存中，并立即使用fsync将其刷新到磁盘。

持久性：这种设置提供了最高级别的持久性，确保每次事务提交后日志已持久化到磁盘。如果发生崩溃，最多会丢失尚未提交的事务。

性能：性能较差，因为每次事务提交都需要进行磁盘写入操作。在高并发写入的环境中，频繁的磁盘I/O可能会成为系统的性能瓶颈。

- **innodb_flush_log_at_trx_commit=2**

当设置为2时，表示每次事务提交时，InnoDB仅将Redo Log缓冲区的内容写入操作系统的文件系统缓存（Page Cache），而实际的磁盘刷盘操作由操作系统来负责。

刷盘时机：在每次事务提交时，InnoDB会将日志缓冲区的内容写入操作系统的文件系统缓存（Page Cache），此时并不会执行fsync操作，日志的实际写入磁盘由操作系统决定。

持久性：如果数据库发生崩溃，但服务器没有崩溃，数据不会丢失。如果服务器也发生崩溃，Page Cache默认保留最近5秒的数据，最多丢失最近5秒内的事务。但与innodb_flush_log_at_trx_commit=0不同，日志至少会写入文件系统缓存，这为数据安全性提供了一定保障。

性能：性能较高，因为每次事务提交时，只需将日志写入操作系统的内存缓存，而不需要立即执行磁盘I/O操作，从而减少了磁盘操作的频率。

**以上三种策略中：**
- 如果是对数据安全性要求比较高的场景，则需要将参数设置为1，因为1的安全性最高。
- 如果是在一些可以容忍数据库崩溃时丢失 1s 数据的场景，我们可以将该值设置为 0，这样可以明显地减少日志同步到磁盘的 I/O 操作。
- 如果是需要安全性和性能折中的方案，可以将参数设置为2，虽然参数 2 没有参数 0 的性能高，但是数据安全性方面比参数 0 强，因为参数 2 只要操作系统不宕机，即使数据库崩溃了，也不会丢失数据，同时性能方便比参数 1 高。


## 二进制日志（binlog）

### 作用
Binlog是MySQL特有的一种日志机制，记录了所有导致数据库状态变化的操作（如INSERT、UPDATE、DELETE）。Binlog主要有两个功能：
- 用于复制，在主从复制中，从库利用主库上的binlog进行重放，实现主从同步。
- 用于数据库的基于时间点的还原，即备份恢复

### 内容
binlog 有 3 种格式类型，分别是 STATEMENT（默认格式）、ROW、 MIXED，区别如下：
- STATEMENT：每一条修改数据的 SQL 都会被记录到 binlog 中（相当于记录了逻辑操作，所以针对这种格式， binlog 可以称为逻辑日志），主从复制中 slave 端再根据 SQL 语句重现。但 STATEMENT 有动态函数的问题，比如你用了 uuid 或者 now 这些函数，你在主库上执行的结果并不是你在从库执行的结果，这种随时在变的函数会导致复制的数据不一致；
- ROW：记录行数据最终被修改成什么样了（这种格式的日志，就不能称为逻辑日志了），不会出现 STATEMENT 下动态函数的问题。但 ROW 的缺点是每行数据的变化结果都会被记录，比如执行批量 update 语句，更新多少行数据就会产生多少条记录，使 binlog 文件过大，而在 STATEMENT 格式下只会记录一个 update 语句而已；
- MIXED：包含了 STATEMENT 和 ROW 模式，它会根据不同的情况自动使用 ROW 模式和 STATEMENT 模式；


> 注意：不同的日志类型在主从复制下除了有动态函数的问题，同样对对更新时间也有影响。一般来说，数据库中的update_time都会设置成ON UPDATE CURRENT_TIMESTAMP，即自动更新时间戳列。在主从复制下，
> 如果日志格式类型是STATEMENT，由于记录的是sql语句，在salve端是进行语句重放，那么更新时间也是重放时的时间，此时slave会有时间延迟的问题；
> 如果日志格式类型是ROW，这是记录行数据最终被修改成什么样了，这种从库的数据是与主服务器完全一致的。


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

bin log日志与redo log类似，也有对应的缓存，叫 binlog cache。事务提交的时候，再把 binlog cache 写到 binlog 文件中。尽管每个线程有自己的Binlog Cache，最终这些日志都会写入到同一个Binlog文件中。
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261841371.png)

- 图中的 write，指的就是指把日志写入到 binlog 文件，但是并没有把数据持久化到磁盘，因为数据还缓存在文件系统的 page cache 里，write 的写入速度还是比较快的，因为不涉及磁盘 I/O。
- 图中的 fsync，才是将数据持久化到磁盘的操作，这里就会涉及磁盘 I/O，所以频繁的 fsync 会导致磁盘的 I/O 升高。

MySQL提供一个 sync_binlog 参数来控制数据库的 binlog 刷到磁盘上的频率：
- sync_binlog = 0：每次事务提交时，只进行写操作（write），不执行fsync，具体何时将数据持久化到磁盘由操作系统决定。
- sync_binlog = 1：每次事务提交时，先进行写操作（write），然后立即执行fsync，确保日志被持久化到磁盘。
- sync_binlog = N（N > 1）：每次事务提交时只执行写操作（write），但累积N个事务后才会执行fsync，将日志持久化到磁盘。在MySQL中，默认的sync_binlog设置为0，意味着没有强制性的磁盘刷新操作，这样可以获得最佳的性能，但也伴随较高的风险。如果操作系统发生异常重启，尚未持久化到磁盘的Binlog数据将会丢失。

显然，在MySQL中系统默认的设置是 sync_binlog = 0，也就是不做任何强制性的磁盘刷新指令，这时候的性能是最好的，但是风险也是最大的。因为一旦主机发生异常重启，还没持久化到磁盘的数据就会丢失。

而当 sync_binlog 设置为 1 的时候，是最安全但是性能损耗最大的设置。因为当设置为 1 的时候，即使主机发生异常重启，最多丢失一个事务的 binlog，而已经持久化到磁盘的数据就不会有影响，不过就是对写入性能影响太大。

如果能容少量事务的 binlog 日志丢失的风险，为了提高写入的性能，一般会 sync_binlog 设置为 100~1000 中的某个数值，从而在性能和安全性之间找到平衡。

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
