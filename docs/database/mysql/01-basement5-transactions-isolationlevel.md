---
title: MySQL事务原理：从ACID到隔离级别的全解析
category: 数据库
tags:
  - Mysql
head:
  - - meta
    - name: keywords
      content: mysql,mysql数据库,事务隔离级别,ACID,MVCC,快照读,当前读,可重复读,脏读,幻读
  - - meta
    - name: description
      content: 全网最全的Mysql知识点总结，让天下没有难学的八股文！
---



## 事务的四个特性ACID

- 原子性（Atomicity）：语句要么全执行，要么全不执行，是事务最核心的特性，事务本身就是以原子性来定义的；实现主要基于**undo log**
- 持久性（Durability）：保证事务提交后不会因为宕机等原因导致数据丢失；实现主要基于**redo log**
- 隔离性（Isolation）：数据库允许多个并发事务同时对其数据进行读写和修改的能力，隔离性保证事务执行尽可能不受其他事务影响；InnoDB默认的隔离级别是RR，RR的实现主要基于**锁机制**（包含next-key lock）、**MVCC**（包括数据的隐藏列、基于**undo log的版本链、ReadView**）
- 一致性（Consistency）：事务追求的最终目标，是指事务操作前和操作后，数据满足完整性约束，数据库保持一致性状态。一致性的实现既需要数据库层面的保障，也需要应用层面的保障

常见的InnoDB是支持事务的，但是MyISAM是不支持事务的

## 并发事务中可能存在的问题

### 脏读

当前事务(A)中可以**读到其他事务(B)未提交的数据**（脏数据），这种现象是脏读。举例如下（以账户余额表为例）：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856440.png)

### 不可重复读
在事务A中先后两次读取同一个数据，两次读取的结果不一样，这种现象称为不可重复读，**读取到了其它事务已提交的数据**。脏读与不可重复读的区别在于：前者读到的是其他事务未提交的数据，后者读到的是其他事务已提交的数据。举例如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856404.png)

### 幻读
在事务A中按照某个条件先后两次查询数据库，两次查询结果的条数不同，这种现象称为幻读。不可重复读与幻读的区别可以通俗的理解为：前者是数据变了，后者是数据的行数变了。举例如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856474.png)

### 总结
- 脏读：读到其他事务未提交的数据；
- 不可重复读：读取到其它事务已提交的数据，导致前后读取的数据不一致；
- 幻读：前后读取的记录数量不一致。

三者严重性排序：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856467.png)

## 事务的隔离级别 - 即隔离性

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856480.png)

- 读未提交（read uncommitted），指一个事务还没提交时，它做的变更就能被其他事务看到；
- 读已提交（read committed），指一个事务提交之后，它做的变更就能被其他事务看到；
- 可重复读（repeatable read，简称RR），指一个事务执行过程中看到的数据，一直跟这个事务启动时看到的数据是一致的，MySQL InnoDB 引擎的默认隔离级别；
- 串行化（serializable ）；会对记录加上读写锁，在多个事务对这条记录进行读写操作时，如果发生了读写冲突的时候，后访问的事务必须等前一个事务执行完成，才能继续执行；

因此，要解决脏读现象，就要升级到 **读已提交** 以上的隔离级别；要解决不可重复读现象，就要升级到 **可重复读** 的隔离级别，要解决幻读现象**不建议**将隔离级别升级到 串行化；

### 四种隔离级别是如何实现的

1. 对于**读未提交**隔离级别的事务来说，因为可以读到未提交事务修改的数据，所以直接读取最新的数据就好了；
2. 对于**串行化**隔离级别的事务来说，通过加读写锁的方式来避免并行访问；
3. 对于**读已提交**和**可重复读**隔离级别的事务来说，它们是通过 Read View 来实现的，它们的区别在于创建 Read View 的时机不同， Read View 就相当于一个数据快照。**读已提交**隔离级别是在**每个语句执行前**重新生成一个 **Read View**，而**可重复读**隔离级别是**启动事务时**生成一个 Read View，然后整个事务期间都在用这个 Read View。

### InnoDB可重复读可以尽量避免幻读
MySQL InnoDB 引擎的默认隔离级别虽然是可重复读，但是它很大程度上避免幻读现象（但并不是完全解决了），解决的方案有两种：
1. 针对快照读（普通 select 语句），是通过 MVCC 方式解决了幻读，因为可重复读隔离级别下，事务执行过程中看到的数据，一直跟这个事务启动时看到的数据是一致的，即使中途有其他事务插入了一条数据，是查询不出来这条数据的，所以就很好了避免幻读问题。
3. 针对当前读（select ... for update 等语句），是通过 next-key lock（记录锁+间隙锁）（详情请看[Mysql的锁](https://www.seven97.top/blog/54 "Mysql的锁")）方式解决了幻读，因为当执行 select ... for update 语句的时候，会加上 next-key lock，如果有其他事务在 next-key lock 锁范围内插入了一条记录，那么这个**插入语句就会被阻塞**，无法成功插入，所以就很好了避免幻读问题。

> 快照读：普通的查询select就是快照读。  
>
> 当前读：MySQL 里除了普通查询是快照读，其他都是当前读，比如 update、insert、delete，这些语句执行前都会**查询最新版本的数据**，读取时还要保证其他并发事务不能修改当前记录，会对读取的记录进行加锁。

### MVCC快照读没有完全解决幻读
Multi-Version Concurrency Control 多版本并发控制。

看到这里时，可以先看后文，ReadView部分

假如某一张表t_text，有如下4个数据：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856465.png)

#### 例1
同一个事物，两次查询之间对于其他事物新增的数据进行了修改

在可重复读的隔离级别下面，开启了两个事务。一个是事务A，另外一个是事务B，其事务ID如下
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856401.png)

两个事务的操作如下：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856391.png)

- t2时刻，因为查询不到这一行数据，因此没有输出。
- t3时刻，事务B插入了一条数据，因此，插入的id为5的那一行数据的trx_id的值为事物B的id，也就是52。
	![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856405.png)
- t4时刻，事物A对于这一行数据进行修改。因为修改操作，不受trx_id的限制。因此，在t4时刻，事物A会把这一行数据的trx_id改变为51.
	![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856388.png)
- 那么，在t5时刻，将会根据事务A开始时候创建的快照来进行判断（事务A的id为51，trx_id为51，可以进行读取，也就读到了id为5的这一行数据）。于是就产生了幻读

>  如果在t4时刻，当前事务A不修改id为5的值的话，那么事务A就会沿着版本链，找到小于事务A的Read View的事务id的第一条记录，也就没有产生幻读。

#### 例2
使用范围查询的时候，不用select...for update来查询

两次查询语句之间，另外一个事物提交了新增的数据。 
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856418.png)

- T2 时刻：事务 A 先执行「快照读语句」：select * from t_text where id > 100 得到了 3 条记录。
- T3 时刻：事务 B 往插入一个 id= 200 的记录并提交；
- T4 时刻：事务 A 再执行「当前读语句」 select * from t_text where id > 100 for update 就会得到 4 条记录，此时也发生了幻读现象。

由于在T4时刻，使用的是当前读，那么这个Read View对于当前读来说就失效了。也就是说，select...for update读取的是最新的数据。并且事务B已经提交了，因此就不会阻塞事务A使用select...for update进行读取。

> 要避免这类幻读，就尽量在开启事务之后，马上执行 select ... for update 这类当前读的语句，因为它会对记录加 next-key lock，从而避免其他事务插入一条新记录

#### 小结
因此可以说，快照读是一种一致性不加锁的读,是InnoDB并发如此之高的核心原因之一，但没有完全解决幻读现象。  

当前读可以说完全解决了幻读问题，因此每次读的都保证是最新的数据，但是是通过加锁实现的。

## ReadView
readView是MVCC多版本并发控制的一种实现手段。这个也叫快照读

> Read View是一个数据库的内部快照，保存着数据库某个时刻的数据信息。Read View会根据事务的隔离级别决定在某个事务开始时，该事务能看到什么信息。通过Read View，事务可以知道此时此刻能看到哪个版本的数据记录（有可能不是最新版本的，也有可能是最新版本的）。**读已提交**隔离级别是在**每个语句执行前**重新生成一个 **Read View**，而**可重复读**隔离级别是**启动事务时**生成一个 Read View，然后整个事务期间都在用这个 Read View。

### Read View 的四个重要字段

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856585.png)

1. creator_trx_id ：指的是创建该 Read View 的事务的事务 id。
2. m_ids ：指的是在创建 Read View 时，当前数据库中 **活跃事务** 的事务 id 列表，注意这是一个列表， **活跃事务** 指的就是，启动了但还没提交的事务。
3. min_trx_id ：指的是在创建 Read View 时，当前数据库中 **活跃事务** 中事务 id 最小的事务，也就是 m_ids 的最小值。
4. max_trx_id ：这个并不是 m_ids 的最大值，而是创建 Read View 时当前数据库中应该给下一个事务的 id 值，也就是全局事务中最大的事务 id 值 + 1；

> 注意：max_trx_id 并不是m_ids中的最大值，事务id是递增分配的。比如，现在有id为1，2，3这三个事务，之后id为3的事务**提交**了。那么一个新的读事务在生成ReadView时，m_ids就包括**还活跃的事务**1和2，min_trx_id的值就**活跃事务** 中事务 id 最小的事务，即1，max_trx_id的值就是4。

在创建 Read View 后，可以将记录中的 trx_id 划分这三种情况：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856176.png)

### undolog中每条记录的两个隐藏列
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856242.png)

- trx_id，当一个事务对某条聚簇索引记录进行改动时，就会把该事务的事务 id 记录在 trx_id 隐藏列里；
- roll_pointer，每次对某条聚簇索引记录进行改动时，都会把旧版本的记录写入到 undo 日志中，然后这个隐藏列是个指针，指向每一个旧版本记录，于是就可以通过它找到修改前的记录。

### Read View的规则 

1. 如果记录的 trx_id 值小于 Read View 中的 min_trx_id 值，表示这个版本的记录是在创建 Read View **前** 已经提交的事务生成的，所以该版本的记录**对当前事务可见**。
2. 如果记录的 trx_id 值大于等于 Read View 中的 max_trx_id 值，表示这个版本的记录是在创建 Read View  **后** 才启动的事务生成的，所以该版本的记录**对当前事务不可见**。
3. 如果记录的 trx_id 值在 Read View 的 min_trx_id 和 max_trx_id 之间，需要判断 trx_id 是否在 m_ids 列表中：
	- 如果记录的 trx_id 在 m_ids 列表中，表示生成该版本记录的活跃事务依然活跃着（还没提交事务），所以该版本的记录**对当前事务不可见**。
	- 如果记录的 trx_id 不在 m_ids列表中，表示生成该版本记录的活跃事务已经被提交，所以该版本的记录对当前事务可见。

### MVCC整体操作流程 

MVCC的实现方式：Read View + undolog

1. 首先获取事务自己的事务 ID；
2. 获取 ReadView；
3. 查询得到的数据，然后与 ReadView 中的事务版本号（m_ids，min_trx_id，max_trx_id）进行比较；
4. 如果不符合 ReadView 规则，就需要从 Undo Log 中（即根据roll_point）获取历史快照；
5. 最后返回符合规则的数据。 在隔离级别为读已提交（Read Committed）时，一个事务中的每一次 SELECT 查询都会重新获取一次Read View。

## 可重复读的执行流程

可重复读隔离级别是启动事务时生成一个 Read View，然后整个事务期间都在用这个 Read View。
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856156.png)

假设事务 A （事务 id 为51）启动后，紧接着事务 B （事务 id 为52）也启动了，那这两个事务创建的 Read View 如下：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856185.png)

事务 A 和 事务 B 的 Read View 具体内容如下：
- 在事务 A 的 Read View 中，它的事务 id 是 51，由于它是第一个启动的事务，所以此时活跃事务的事务 id 列表就只有 51，活跃事务的事务 id 列表中最小的事务 id 是事务 A 本身，下一个事务 id 则是 52。
- 在事务 B 的 Read View 中，它的事务 id 是 52，由于事务 A 是活跃的，所以此时活跃事务的事务 id 列表是 51 和 52，活跃的事务 id 中最小的事务 id 是事务 A，下一个事务 id 应该是 53。

假设在可重复读隔离级别下，事务 A 和事务 B 按顺序执行了以下操作：
1. 事务 A 读取zhangsan的账户余额记录，读到余额是 100 ；
2. 事务 B 读取zhangsan的账户余额记录，读到余额是 100 ；（事务B第一次读）
3. 事务 A 将zhangsan的账户余额记录修改成 200 万，并没有提交事务；
4. 事务 B 读取zhangsan的账户余额记录，读到余额还是 100 ；（事务B第二次读）
5. 事务 A 提交事务；
6. 事务 B 读取zhangsan的账户余额记录，读到余额依然还是 100 ；（事务B第三次读）

分析执行过程：
**事务 B 第一次读**zhangsan的账户余额记录，在找到记录后，它会先看这条记录的 trx_id，此时发现 trx_id 为 50，比事务 B 的 Read View 中的 min_trx_id 值（51）还小，这意味着修改这条记录的事务早就在事务 B 启动前提交过了，所以该版本的记录对事务 B 可见的，也就是事务 B 可以获取到这条记录。

接着，事务 A 通过 update 语句将这条记录修改了（还未提交事务），将zhangsan的余额改成 200 万，这时这条记录的trx_id 为事务A的事务id（trx_id = 51）， 并且MySQL 会记录相应的 undo log，并以链表的方式串联起来，形成版本链，如下图：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856223.png)

接着**事务 B 第二次读**取该记录，发现这条记录的 trx_id 值为 51，在事务 B 的 Read View 的 min_trx_id 和 max_trx_id 之间，则需要判断 trx_id 值是否在 m_ids 范围内，判断的结果是在的，那么说明这条记录是被还未提交的事务修改的，这时事务 B 并不会读取这个版本的记录。而是沿着 undo log 链条往下找旧版本的记录，直到找到 trx_id **小于** 事务 B 的 Read View 中的 min_trx_id 值的第一条记录，所以事务 B 能读取到的是 trx_id 为 50 的记录，也就是小林余额是 100 的这条记录。

最后，当事物 A 提交事务后，由于隔离级别时 **可重复读**，所以**事务 B 第三次读**取记录时，还是基于启动事务时创建的 Read View 来判断当前版本的记录是否可见。所以，即使事务 A 将zhangsan余额修改为 200 并提交了事务， 事务 B 第三次读取记录时，读到的记录都是zhangsan余额是 100 的这条记录。

## 读已提交的执行流程

读提交隔离级别是在每次读取数据时，都会生成一个新的 Read View。
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856664.png)

假设事务 A （事务 id 为51）启动后，紧接着事务 B （事务 id 为52）也启动了，接着按顺序执行了以下操作：
1. 事务 A 读取数据（创建 Read View），zhangsan的账户余额为 100 ；
2. 事务 B 读取数据（创建 Read View），zhangsan的账户余额为 100 ；（事务B第一次读）
3. 事务 A 修改数据（还没提交事务），将zhangsan的账户余额从 100 修改成了 200 ；
4. 事务 B 读取数据（创建 Read View），zhangsan的账户余额为 100 ；（事务B第二次读）
5. 事务 A 提交事务；
6. 事务 B 读取数据（创建 Read View），zhangsan的账户余额为 200 ；（事务B第三次读）

**事务B第一次读**时创建的Read View：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856925.png)
此时，在找到记录后，它会先看这条记录的 trx_id，此时发现 trx_id 为 50，比事务 B 的 Read View 中的 min_trx_id 值（51）还小，这意味着修改这条记录的事务早就在事务 B 启动前提交过了，所以该版本的记录对事务 B 可见的，也就是事务 B 可以获取到这条记录。因此读取到的zhangsan的账户余额为 100 ；

接着事务A修改数据（还没提交事务）：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856974.png)

**事务B第二次读**时创建的Read View：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856925.png)
显然，此时事务B读不到事务 A （还未提交事务)修改的数据。  

事务 B 在找到这条记录时，会看这条记录的 trx_id 是 51，在事务 B 的 Read View 的 min_trx_id 和 max_trx_id 之间，接下来需要判断 trx_id 值是否在 m_ids 范围内，判断的结果是在的，那么说明这条记录是被还未提交的事务修改的，这时事务 B 并不会读取这个版本的记录。而是，沿着 undo log 链条往下找旧版本的记录，直到找到 trx_id **小于** 事务 B 的 Read View 中的 min_trx_id 值的第一条记录，所以事务 B 能读取到的是 trx_id 为 50 的记录，也就是zhangsan的账户余额为 100 的这条记录。

接着事务A提交了事务：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856974.png)

**事务B第三次读**时创建的Read View：由于隔离级别是 **读提交 **，所以事务 B 在每次读数据的时候，会重新创建 Read View
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261856976.png)
此时事务 B 在找到这条记录时，会发现这条记录的 trx_id 是 51，比事务 B 的 Read View 中的 min_trx_id 值（52）还小，这意味着修改这条记录的事务早就在创建 Read View 前提交过了，所以该版本的记录对事务 B 是**可见**的。

正是因为在读提交隔离级别下，事务每次读数据时都重新创建 Read View，那么在事务期间的多次读取同一条数据，前后两次读的数据可能会出现不一致，因为可能这期间另外一个事务修改了该记录，并提交了事务，也就造成了不可重复读的问题。

## 总结
事务的 ACID四大特性，原子性、一致性、隔离性、持久性；原子性、隔离性、持久性都是为了保证最终的一致性

当多个事务并发执行的时候，会引发脏读、不可重复读、幻读这些问题。  

要解决脏读现象，就要将隔离级别升级到读已提交以上的隔离级别，要解决不可重复读现象，就要将隔离级别升级到可重复读以上的隔离级别。

对于**读已提交**和**可重复读**隔离级别的事务来说，它们是通过 Read View 来实现的，它们的区别在于创建 Read View 的时机不同：
- **读已提交**隔离级别是在每个 select 都会生成一个新的 Read View，也意味着，事务期间的多次读取同一条数据，前后两次读的数据可能会出现不一致，因为可能这期间另外一个事务修改了该记录，并提交了事务。
- **可重复读**隔离级别是启动事务时生成一个 Read View，然后整个事务期间都在用这个 Read View，这样就保证了在事务期间读到的数据都是事务启动前的记录。

这两个隔离级别实现是通过事务的**Read View 里的四个字段**和 **记录中的两个隐藏列（事务id和指针）** 的版本比对，来控制并发事务访问同一个记录时的行为，这就叫 MVCC（多版本并发控制）。



<!-- @include: @article-footer.snippet.md -->     
