---
title: 锁 - Mysql的锁
category: 数据库
tag:
 - Mysql
---



## 全局锁
```sql
flush tables with read lock
```

执行后，整个数据库就处于只读状态了，这时其他线程执行以下操作，都会被阻塞：
- 对数据的增删改操作，比如 insert、delete、update等语句；
- 对表结构的更改操作，比如 alter table、drop table 等语句。

全局锁主要应用于做**全库逻辑备份**，这样在备份数据库期间，不会因为数据或表结构的更新，而出现备份文件的数据与预期的不一样。

但是加上全局锁后，整个数据库都是只读状态，无法更新数据。

因此，一般也不建议用全局锁进行全库逻辑备份。InnoDB支持可重复读的隔离级别，那么在备份数据库之前可以先开启事务，会先创建 Read View，然后整个事务执行期间都在用这个 Read View，而且由于 MVCC 的支持，备份期间业务依然可以对数据进行更新操作。因为在可重复读的隔离级别下，即使其他事务更新了表的数据，也不会影响备份数据库时的 Read View，这就是事务四大特性中的隔离性，这样备份期间备份的数据一直是在开启事务时的数据。

## 表级锁
innodb的 表锁，分成意向共享锁 和意向独占锁，表锁是innodb引擎⾃动加的，不⽤⾃⼰加。
- 意向共享锁：加共享⾏锁时，必须先加意向共享锁；
- 意向独占锁 ：给某⾏加独占锁时，必须先给表加意向独占锁。

insert、update、delete，innodb会⾃动给那⼀⾏加⾏级独占锁。  
普通的select啥锁都不加。但是可以手动加锁  
```sql
//先在表上加上意向共享锁，然后对读取的记录加共享锁
select ... lock in share mode;

//先在表上加上意向独占锁，然后对读取的记录加独占锁
select ... for update;
```

## 行级锁

InnoDB 引擎是支持行级锁的，而 MyISAM 引擎并不支持行级锁。

普通的 select 语句是不会对记录加锁的，因为它**属于快照读**。如果要在查询时对记录加行锁，可以使用下面这两个方式，这种查询会加锁的语句称为**当前读**。
```sql
//对读取的记录加共享锁
select ... lock in share mode;

//对读取的记录加独占锁
select ... for update;
```

共享锁（S锁）满足读读共享，读写互斥。独占锁（X锁）满足写写互斥、读写互斥。
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261902808.png)

行级锁的类型主要有三类：
- Record Lock，记录锁，也就是仅仅把一条记录锁上；
- Gap Lock，间隙锁，锁定一个范围，但是不包含记录本身；（左开右开区间）
- Next-Key Lock：Record Lock + Gap Lock 的组合，锁定一个范围，并且锁定记录本身。（左开右闭区间）

### Record Lock
Record Lock 称为记录锁，锁住的是一条记录。而且记录锁是有 S 锁和 X 锁之分的：

- 当一个事务对一条记录加了 S 型记录锁后，其他事务也可以继续对该记录加 S 型记录锁（S 型与 S 锁兼容），但是不可以对该记录加 X 型记录锁（S 型与 X 锁不兼容）;
- 当一个事务对一条记录加了 X 型记录锁后，其他事务既不可以对该记录加 S 型记录锁（S 型与 X 锁不兼容），也不可以对该记录加 X 型记录锁（X 型与 X 锁不兼容）。

**总而言之**，当一个事务对一条记录加了读锁后，其它事务也可读，但不可写；当一个事务对一条记录加了写锁后，其它事务不可读也不可写。

### Gap Lock
Gap Lock 称为间隙锁，只存在于可重复读隔离级别，目的是为了**解决可重复读隔离级别下幻读**的现象。

假设，表中有一个范围 id 为（3，5）间隙锁，那么其他事务就无法插入 id = 4 这条记录了，这样就有效的防止幻读现象的发生。
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261902850.png)

### Next-Key Lock
Next-Key Lock 称为临键锁，是 Record Lock + Gap Lock 的组合，锁定一个范围，并且锁定记录本身。

假设，表中有一个范围 id 为（3，5] 的 next-key lock，那么其他事务即不能插入 id = 4 记录，也不能修改 id = 5 这条记录
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261902835.png)


## 乐观锁和悲观锁
乐观锁和悲观锁都是针对读（select）来说的。

### 乐观锁
就是非常乐观，做什么事都往好处想； 对于数据库操作，就认为每次操作数据的时候都认为别的操作不会修改，所以不会加锁，而是通过一个类似于版本的字段来标识该数据是否修改过，在执行本次操作前先判断是否修改过，如果修改过就放弃本次操作重新再来。乐观锁适用于多读的应用类型，这样可以提高吞吐量;乐观锁策略:提交版本必须大于记录当前版本才能执行更新。

- 乐观锁不是数据库自带的，需要自己去实现。
- 乐观锁是指 更新数据库时认为操作不会导致冲突，在操作数据时不加锁，而在进行更新后，再去判断是否有冲突了。

通常实现是这样的：在表中的数据进行更新时，先给数据表加一个版本(version)字段，每操作一次，将那条记录的版本号加1。
- 先查询出那条记录，获取出version字段。
- 如果要对那条记录进行更新操作，则先判断此刻version的值是否与刚刚查询出来时的version的值相等。
- 如果相等，则说明这段期间没有其他程序对其进行操作，则可以执行更新，将version字段的值加1。
- 如果version值与刚刚获取出来的version的值不相等，则说明这段期间已经有其他程序对其进行操作了，则不进行更新操作。

### 悲观锁
就是非常悲观，做什么事都觉得不好；悲观锁就是在操作数据时，每次操作数据数据都会认为别的操作会修改当前数据，所以在进行每次操作时都要加锁才能进行对相同数据的操作，这点跟java中的synchronized很相似，所以悲观锁需要耗费较多的时间。

悲观锁涉及到的另外两个锁：就是共享锁与排它锁（select **** for update）。  
共享锁和排它锁是悲观锁的不同的实现，它俩都属于悲观锁的范畴。

因此，悲观锁在未通过索引条件检索数据时，会全表扫描，于是就会对所有记录加上 next-key 锁（记录锁 + 间隙锁），相当于把整个表锁住了。导致其他程序不允许“加锁的查询操作”，影响吞吐。故如果在查询居多的情况下，推荐使用乐观锁。
- 加锁的查询操作：加过排他锁的数据行在其他事务中是不能修改的，也不能通过for update或lock in share mode的加锁方式查询，但可以直接通过select ...from...查询数据，因为普通查询没有任何锁机制。
- 乐观锁更新有可能会失败，甚至是更新几次都失败，这是有风险的。所以如果写入居多，对吞吐要求不高，可使用悲观锁。

也就是一句话：读用乐观锁，写用悲观锁。

### 小结

悲观锁做事比较悲观，它认为同时修改共享资源的概率比较高，于是很容易出现冲突，所以访问共享资源前，先要上锁。

那相反的，如果多线程同时修改共享资源的概率比较低，就可以采用乐观锁。

乐观锁做事比较乐观，它假定冲突的概率很低，它的工作方式是：先修改完共享资源，再验证这段时间内有没有发生冲突，如果没有其他线程在修改资源，那么操作完成，如果发现有其他线程已经修改过这个资源，就放弃本次操作。
