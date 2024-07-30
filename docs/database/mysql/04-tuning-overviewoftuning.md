---
title: 调优 - Mysql优化概述
category: 数据库
tag:
 - Mysql
---





## SQL优化一般步骤
### 慢日志定位

通过慢查日志等定位那些执行效率较低的SQL语句

MySQL的慢查询日志是MySQL提供的一种日志记录，它用来记录在MySQL中响应时间超过阀值的语句，具体指运行时间超过long_query_time值的SQL，则会被记录到慢查询日志中。long_query_time的默认值为10，意思是运行10S以上的语句。默认情况下，Mysql数据库并不启动慢查询日志，需要我们手动来设置这个参数，当然，如果不是调优需要的话，一般不建议启动该参数，因为开启慢查询日志会或多或少带来一定的性能影响。慢查询日志支持将日志记录写入文件，也支持将日志记录写入数据库表。

### explain 分析SQL的执行计划
需要重点关注type、rows、filtered、extra。

type由上至下，效率越来越高
- ALL：全表扫描
- index：索引全扫描
- range：索引范围扫描，常用语<,<=,>=,between,in等操作
- ref：使用非唯一索引扫描或唯一索引前缀扫描，返回单条记录，常出现在关联查询中
- eq_ref：类似ref，区别在于使用的是唯一索引，使用主键的关联查询
- const/system：单条记录，系统会把匹配行中的其他列作为常数处理，如主键或唯一索引查询
- null：MySQL不访问任何表或索引，直接返回结果 虽然上至下，效率越来越高，但是根据cost模型，假设有两个索引idx1(a, b, c),idx2(a, c)，SQL为"select * from t where a = 1 and b in (1, 2) order by c";如果走idx1，那么是type为range，如果走idx2，那么type是ref；当需要扫描的行数，使用idx2大约是idx1的5倍以上时，会用idx1，否则会用idx2

Extra
- Using filesort：MySQL需要额外的一次传递，以找出如何按排序顺序检索行。通过根据联接类型浏览所有行并为所有匹配WHERE子句的行保存排序关键字和行的指针来完成排序。然后关键字被排序，并按排序顺序检索行。
- Using temporary：使用了临时表保存中间结果，性能特别差，需要重点优化
- Using index：表示相应的 select 操作中使用了覆盖索引（Coveing Index）,避免访问了表的数据行，效率不错！如果同时出现 using where，意味着无法直接通过索引查找来查询到符合条件的数据。
- Using index condition：MySQL5.6之后新增的ICP，using index condtion就是使用了ICP（索引下推），在存储引擎层进行数据过滤，而不是在服务层过滤，利用索引现有的数据减少回表的数据。

> 索引下推：在联合索引遍历过程中，对联合索引中包含的字段先做判断，直接过滤掉不满足条件的记录，**减少回表次数**。

### show profile 分析
了解SQL执行的线程的状态及消耗的时间。默认是关闭的，开启语句“set profiling = 1;”

```sql
SHOW PROFILES ;
SHOW PROFILE FOR QUERY  #{id};
```

### trace
trace分析优化器如何选择执行计划，通过trace文件能够进一步了解为什么选择A执行计划而不选择B执行计划。

### 确定问题并采用相应的措施
- 优化索引
- 优化SQL语句：修改SQL、IN 查询分段、时间查询分段、基于上一次数据过滤
- 改用其他实现方式：ES、数仓等
- 数据碎片处理


## 优化索引(索引失效)

如果Mysql索引失效，会进行全表扫描，将极大的影响查询效率，因此应该尽量避免索引失效

以下说明都是在已创建了相关字段的索引情况下描述的。

> 当使用左或者左右模糊匹配的时候，也就是 like %xx 或者 like %xx%这两种方式都会造成索引失效；

因为索引 B+ 树是按照「索引值」有序排列存储的，只能根据前缀进行比较。

如果使用的like x% 匹配的时候，是可以走索引的，所以可以根据 x 的这个前缀进行匹配


> 当在查询条件中对索引列做了计算、函数、类型转换操作，这些情况下都会造成索引失效；



### 对索引使用左或者左右模糊匹配

当使用左或者左右模糊匹配的时候，也就是 like %xx 或者 like %xx% 这两种方式都会造成索引失效。



### 计算(使用函数)

因为索引保存的是索引字段的原始值，而不是经过计算后的值，自然就没办法走索引了。  

函数计算或者表达式计算都没办法走索引

```sql
//函数计算
select * from t_user where length(name)=6;

//表达式计算
select * from t_user where id + 1 = 10;
```

不过，从 MySQL 8.0 开始，索引特性增加了函数索引，即可以针对函数计算后的值建立一个索引，也就是说该索引的值是函数计算后的值，所以就可以通过扫描索引来查询数据。



### 类型转换

1. 如果索引字段是字符串类型，但是在条件查询中，输入的参数是整型的话，那么这条语句会走全表扫描。  
2. 但是如果索引字段是整型类型，查询条件中的输入参数即使字符串，是不会导致索引失效，还是可以走索引扫描。  

原因在于MySQL **在遇到字符串和数字比较的时候，会自动把字符串转为数字**，然后再进行比较。也就是说，如果索引字段是整型类型，查询条件中的输入参数是字符串，会自动转换成整型，所以索引不会失效。而索引字段是字符串，而输入的是整型，由于是字符串转数字，而索引不是整型类型，所以索引失效了。

因此在使用sql语句时：数值类型禁止加引号，字符串类型必须加引号



### 联合索引非最左匹配

联合索引要能正确使用需要遵循最左匹配原则，也就是按照最左优先的方式进行索引的匹配，否则就会导致索引失效。

原因是，在联合索引的情况下，数据是按照索引第一列排序，第一列数据相同时才会按照第二列排序。

比如，如果创建了一个 (a, b, c) 联合索引，如果查询条件是以下这几种，就可以匹配上联合索引：
- where a=1；
- where a=1 and b=2 and c=3；
- where a=1 and b=2；

因为有查询优化器，所以 a 字段在 where 子句的顺序并不重要。

但是，如果查询条件是以下这几种，因为不符合最左匹配原则，所以就无法匹配上联合索引，联合索引就会失效:
- where b=2；
- where c=3；
- where b=2 and c=3；

对于where a = 1 and c = 0 这个语句，前面的a = 1是会走索引的，后面的c不走索引。





### 不应使用 or

在 WHERE 子句中or，如果在 OR 前的条件列是索引列，而在 OR 后的条件列不是索引列，那么索引会失效。

OR 的含义就是两个只要满足一个即可，因此只有一个条件列是索引列是没有意义的，只要有条件列不是索引列，就会进行全表扫描。



### in

尽量使用IN代替OR。但是IN包含的值不应过多，应少于1000个。

因为 IN 通常是走索引的，当IN后面的数据在数据表中超过30%的匹配时是全表的扫描，不会走索引

其实就是 Mysql优化器会根据当前表的情况选择最优解。 Mysql优化器认为走全表扫描 比 走索引+回表快 那就不会走索引



### 范围查询阻断，后续字段不能走索引

索引
```sql
KEY `idx_shopid_created_status` (`shop_id`, `created_at`, `order_status`)
```

SQL语句
```sql
select * from _order where shop_id = 1 and created_at > '2021-01-01 00:00:00' and order_status = 10
```
范围查询还有“IN、between”

相关原理可以看这篇文章 [唯一索引范围查询](https://www.seven97.top/database/mysql/02-lock2-howtoaddrowlocks.html#唯一索引范围查询)



### 覆盖索引优化

覆盖索引是指 SQL 中 查询的所有字段，在这个二级索引 B+Tree 的叶子节点上都能找得到那些字段，从二级索引中查询得到记录，而不需要通过聚簇索引查询获得，就可以**避免回表**的操作。



### asc和desc混用

```sql
select * from _t where a=1 order by b desc, c asc
```
desc 和asc混用时会导致索引失效



### 避免更新索引列值

每当索引列的值发生变化时，数据库必须更新相应的索引结构，更新索引列值可能导致这些树结构的重平衡或重新构建，增加了额外的计算和I/O开销。



### 不等于、不包含不能用到索引的快速搜索

```sql
select * from _order where shop_id=1 and order_status not in (1,2)
select * from _order where shop_id=1 and order_status != 1
```

在索引上，避免使用NOT、!=、<>、!<、!>、NOT EXISTS、NOT IN、NOT LIKE等

> not in一定不走索引吗？  
>
> 答案是不一定。Mysql优化器会根据当前表的情况选择最优解。  
>
> 主要在于如果 MySQL 认为 全表扫描 比 走索引+回表效率高， 那么他会选择全表扫描。



### 重要SQL必须被索引

update、delete的where条件列、order by、group by、distinct字段、多表join字段（on后面的字段）

例如：select id from table_a where name = 'seven' order by address ; 此时建立 name + address的联合索引比较好(此处name条件必须是 = ,如果是范围则无效)；如果是order by主键，则只需要在name字段建立索引即可，因为name索引表中是包含主键的，也就是所谓了避免了回表操作。



### 避免使用子查询

对子查询产生的临时表再扫描无索引可走，只能全表扫描

必要时推荐用 JOIN 代替子查询





## SQL优化

### 大分页 limit
Mysql中常使用limit语句进行分页，如  
```sql
mysql> SELECT * FROM table LIMIT 5,10; //检索记录行6-15
```

今天看到一个问题，为什么以下两个查询语句的速度差那么多？
```sql
SELECT id from table limit 500000, 10; //0.1秒
SELECT * from table limit 500000, 10; //1.2秒
```



执行第一条语句，执行计划是index，走Primary主键索引。
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261921688.png)

第一行sql语句意思是，查询以id为500001开始的10条内容



执行第二条语句，执行计划是All，走的是全表扫描
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261921695.png)

我们知道，执行器实际上会将 select * 中的 * 符号，扩展为表上的所有列，因此第二行sql语句意思是，查询表中第500001行开始的10条内容

原因：在使用limit的时候没有对字段进行排序的时候，如果用id查走的是索引，按 **索引的存储位置** 取数据，\* 是查全表，按 **表中记录实际的存储位置** 取结果，如果查询结果一样那么只是巧合而已。所以在不使用order by 排序时出现的结果实际上是不同的。



对于大分页的场景，可以优先让产品优化需求，如果没有优化的，有如下两种优化方式， 一种是把上一次的最后一条数据，也即上面的c传过来，然后做“c < xxx”处理，但是这种一般需要改接口协议，并不一定可行。

#### 方法1：延迟关联
采用延迟关联的方式进行处理，减少SQL回表，但是要记得索引需要完全覆盖才有效果，SQL改动如下
```sql
select * from xxx  where id >=(select id from xxx order by id limit 500000, 1) order by id limit 10;
```

#### 方法2
将所有的数据**根据id主键进行排序**，然后分批次取，将当前批次的最大id作为下次筛选的条件进行查询。
```sql
select * from xxx where id > start_id order by id limit 10;
```
通过主键索引，每次定位到start_id的位置，然后往后遍历10个数据，这样不管数据多大，查询性能都较为稳定



### update

update这种加锁的语句要确保 where 条件中带上了索引列，并且测试确认该语句是否走的是索引扫描，防止因为扫描全表，结果全表加锁





### 复杂查询

```sql
select sum(amt) from _t where a = 1 and b in (1, 2, 3) and c > '2020-01-01';
select * from _t where a = 1 and b in (1, 2, 3) and c > '2020-01-01' limit 10;
```
如果是统计某些数据，可能改用数仓进行解决；

如果是业务上就有那么复杂的查询，可能就不建议继续走SQL了，而是采用其他的方式进行解决，比如使用ES等进行解决。







## 分库分表

当单表的数据量达到1000W或100G以后，优化索引、添加从库等可能对数据库性能提升效果不明显，此时就要考虑对其进行切分了。切分的目的就在于减少数据库的负担，缩短查询的时间。

数据切分可以分为两种方式：垂直划分和水平划分。

### 垂直划分

垂直划分数据库是根据业务进行划分，例如购物场景，可以将库中涉及商品、订单、用户的表分别划分出成一个库，通过降低单库的大小来提高性能。同样的，分表的情况就是将一个大表根据业务功能拆分成一个个子表，例如商品基本信息和商品描述，商品基本信息一般会展示在商品列表，商品描述在商品详情页，可以将商品基本信息和商品描述拆分成两张表。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261923116.png)

优点：行记录变小，数据页可以存放更多记录，在查询时减少I/O次数。

缺点：
- 主键出现冗余，需要管理冗余列；
- 会引起表连接JOIN操作，可以通过在业务服务器上进行join来减少数据库压力；
- 依然存在单表数据量过大的问题。

### 水平划分
水平划分是根据一定规则，例如时间或id序列值等进行数据的拆分。比如根据年份来拆分不同的数据库。每个数据库结构一致，但是数据得以拆分，从而提升性能。

![](https://sn-blog.oss-cn-guangzhou.aliyuncs.com/2023/07/22/5c279d76818f42ebae3dc72264a4b104.png)

优点：单库（表）的数据量得以减少，提高性能；切分出的表结构相同，程序改动较少。

缺点：
- 分片事务一致性难以解决
- 跨节点join性能差，逻辑复杂
- 数据分片在扩容时需要迁移



## 分区表(分片)

说在前面，目前分区表有几项限制：

- 只支持水平分区，不支持垂直分区
- null值无法通过分区列来过滤
- 分区个数也是有限的，随着分区个数的增加，分区表的性能会下降
- 只能通过主键过着唯一列进行分区
- 如果查询中没有分区列，查询则无法通过分区列进行过滤
- 想要重组分区，开销较大

总的来说，一般是不建议使用分区表的，不感兴趣的也可以不用看这部分内容了



### 分区
分区是把一张表的数据分成N多个区块。分区表是一个独立的逻辑表，但是底层由多个物理子表组成。

当查询条件的数据分布在某一个分区的时候，查询引擎只会去某一个分区查询，而不是遍历整个表。在管理层面，如果需要删除某一个分区的数据，只需要删除对应的分区即可。

分区一般都是放在单机里的，用的比较多的是时间范围分区，方便归档。只不过分库分表需要代码实现，分区则是mysql内部实现。分库分表和分区并不冲突，可以结合使用。

### 分片
MySQL分片查询是指将数据分散在不同的服务器上，并使用查询语句在多个服务器上并行查询，以提高查询效率。

1. 首先，需要准备多台MySQL服务器。每台服务器上需要有相同的数据表，表结构和表数据也要相同。
2. 接着，在应用程序中，需要使用分片算法将数据分散到不同的服务器上。常用的分片算法有hash分片、range分片和lookup分片。hash分片是将数据根据hash值分散到不同的服务器上；range分片是根据数据段的范围进行分片；lookup分片是通过路由表将数据指向相应的服务器。
3. 最后，需要使用MySQL集群中的代理节点来进行查询。代理节点收到查询请求后，会将请求分发到不同的服务器上并行查询，并将结果合并返回给应用程序。

综上所述，MySQL分片查询通过将数据分散到多个服务器上，并在代理节点上进行并行查询，可以提高查询效率，提高系统的吞吐量。

### 分区表类型
#### range分区
按照范围分区。比如按照时间范围分区

```sql
CREATE TABLE test_range_partition(
       id INT auto_increment,
       createdate DATETIME,
       primary key (id,createdate)
   ) 
   PARTITION BY RANGE (TO_DAYS(createdate) ) (
      PARTITION p201801 VALUES LESS THAN ( TO_DAYS('20180201') ),
      PARTITION p201802 VALUES LESS THAN ( TO_DAYS('20180301') ),
      PARTITION p201803 VALUES LESS THAN ( TO_DAYS('20180401') ),
      PARTITION p201804 VALUES LESS THAN ( TO_DAYS('20180501') ),
      PARTITION p201805 VALUES LESS THAN ( TO_DAYS('20180601') ),
      PARTITION p201806 VALUES LESS THAN ( TO_DAYS('20180701') ),
      PARTITION p201807 VALUES LESS THAN ( TO_DAYS('20180801') ),
      PARTITION p201808 VALUES LESS THAN ( TO_DAYS('20180901') ),
      PARTITION p201809 VALUES LESS THAN ( TO_DAYS('20181001') ),
      PARTITION p201810 VALUES LESS THAN ( TO_DAYS('20181101') ),
      PARTITION p201811 VALUES LESS THAN ( TO_DAYS('20181201') ),
      PARTITION p201812 VALUES LESS THAN ( TO_DAYS('20190101') )
   );
```

在/var/lib/mysql/data/可以找到对应的数据文件，每个分区表都有一个使用#分隔命名的表文件：
```
   -rw-r----- 1 MySQL MySQL    65 Mar 14 21:47 db.opt
   -rw-r----- 1 MySQL MySQL  8598 Mar 14 21:50 test_range_partition.frm
   -rw-r----- 1 MySQL MySQL 98304 Mar 14 21:50 test_range_partition#P#p201801.ibd
   -rw-r----- 1 MySQL MySQL 98304 Mar 14 21:50 test_range_partition#P#p201802.ibd
   -rw-r----- 1 MySQL MySQL 98304 Mar 14 21:50 test_range_partition#P#p201803.ibd
...
```

#### list分区
list分区和range分区相似，主要区别在于list是枚举值列表的集合，range是连续的区间值的集合。对于list分区，分区字段必须是已知的，如果插入的字段不在分区时的枚举值中，将无法插入.
```sql
create table test_list_partiotion
   (
       id int auto_increment,
       data_type tinyint,
       primary key(id,data_type)
   )partition by list(data_type)
   (
       partition p0 values in (0,1,2,3,4,5,6),
       partition p1 values in (7,8,9,10,11,12),
       partition p2 values in (13,14,15,16,17)
   );
```

#### hash分区
可以将数据均匀地分布到预先定义的分区中。
```sql
create table test_hash_partiotion
   (
       id int auto_increment,
       create_date datetime,
       primary key(id,create_date)
   )partition by hash(year(create_date)) partitions 10;
```

#### 分区的问题
1. 打开和锁住所有底层表的成本可能很高。当查询访问分区表时，MySQL 需要打开并锁住所有的底层表，这个操作在分区过滤之前发生，所以无法通过分区过滤来降低此开销，会影响到查询速度。可以通过批量操作来降低此类开销，比如批量插入、LOAD DATA INFILE和一次删除多行数据。
2. 维护分区的成本可能很高。例如重组分区，会先创建一个临时分区，然后将数据复制到其中，最后再删除原分区。
3. 所有分区必须使用相同的存储引擎。





## count(\*) 和 count(1)哪个快

按照性能排序是：count(\*) = count(1) > count(主键字段) > count(字段)

### count(主键字段)的执行过程
比如说，id是主键字段。
- 如果表里只有主键索引，那么，InnoDB 循环遍历聚簇索引，将读取到的记录返回给 server 层，然后读取记录中的 id 值，并根据 id 值判断**是否为 NULL**，如果不为 NULL，就将 count 变量加 1。
- 如果表里有二级索引时，InnoDB 循环遍历的对象就不是聚簇索引，而是二级索引。因为相同数量的二级索引记录可以比聚簇索引记录占用更少的存储空间，所以二级索引树比聚簇索引树小，这样遍历二级索引的 I/O 成本比遍历聚簇索引的 I/O 成本小，因此「优化器」优先选择的是二级索引。

### count(1) 的执行过程
如果表里只有主键索引，没有二级索引时。那么，InnoDB 循环遍历聚簇索引（主键索引），将读取到的记录返回给 server 层，但是**不会读取记录中的任何字段的值**，因为 count 函数的参数是 1，不是字段，所以不需要读取记录中的字段值。参数 1 很明显并不是 NULL，因此 server 层每从 InnoDB 读取到一条记录，就将 count 变量加 1。

显然，count(1) 相比 count(主键字段) 少一个步骤，就是不需要读取记录中的字段值，所以通常会说 count(1) 执行效率会比 count(主键字段) 高一点。

但是，如果表里有二级索引时，InnoDB 循环遍历的对象就二级索引了。

### count(\*) 的执行过程(mysql官方文档推荐)
count(\*) 其实等于 count(0)，也就是说，当你使用 count(\*) 时，MySQL 会将 \* 参数转化为参数 0 来处理。

所以，count(*) 执行过程跟 count(1) 执行过程基本一样的，性能没有什么差异。

### count(字段) 的执行过程
采用全表扫描的方式来统计









