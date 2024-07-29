---
title: Mysql使用篇
category: 数据库
tag:
 - Mysql
---



## Mysql命令大全



### mysql服务的启动和停止

```sql
  net stop mysql
  net start mysql
```



### 连接mysql
 ```sql
  mysql (-h) -u 用户名 -p 用户密码
 ```

> 注意，如果是连接到另外的机器上，则需要加入一个参数-h机器IP

键入命令mysql -u root -p， 回车后提示输入密码，然后回车即可进入到mysql中了



### 权限控制-DCL

#### 权限验证过程

mysql中存在4个控制权限的表，`分别为user表，db表，tables_priv表，columns_priv表`

mysql权限表的验证过程为：

1. 先从user表中的Host,User,Password这3个字段中判断连接的ip、用户名、密码是否存在，存在则通过验证。
2. 通过身份认证后，进行权限分配，按照user，db，tables_priv，columns_priv的顺序进行验证。即
   1. 先检查全局权限表user，如果user中对应的权限为Y，则此用户对所有数据库的权限都为Y，将不再检查db, tables_priv,columns_priv；
   2. 如果user中为N，则到db表中检查此用户对应的具体数据库，并得到db中为Y的权限；（一般来说，权限管理只会到db层，过于细化也不方便管理）
   3. 如果db中为N，则检查table_priv和columns_priv(如果是存储过程操作则检查mysql.procs_priv)，如果满足，则执行操作
   4. 如果以上检查均失败，则系统拒绝执行操作。



因为MySQL是使用User和Host两个字段来确定用户身份的，这样就带来一个问题，就是一个客户端到底属于哪个host。如果一个客户端同时匹配几个Host，对用户的确定将按照下面的优先级来排：`基本观点`越精确的匹配越优先

- Host列上，越是确定的Host越优先，[localhost, 192.168.1.1, wiki.seven97.top] 优先于[192.168.%, %.seven97.top]，优先于[192.%, %.top]，优先于[%]
- User列上，明确的username优先于空username。（空username匹配所有用户名，即匿名用户匹配所有用户）
- Host列优先于User列考虑

#### 增加新用户

```sql
grant 权限列表 on 数据库.* to 用户名@登录主机 identified by "密码"
```


例：增加一个用户user密码为password，让其可以在本机上登录， 并对所有数据库有查询、插入、修改、删除的权限。首先用以root用户连入mysql，然后键入以下命令：

```sql
grant select,insert,update,delete on . to user@localhost Identified by “password”;
```

如果希望该用户能够在任何机器上登陆mysql，则将localhost改为"%"。







### 数据库操作-DDL

帮助命令: help



登录到mysql中，然后在mysql的提示符下运行下列命令，每个命令以分号结束。

选择所创建的数据库

```sql
use 数据库名
```

导入.sql文件命令(例D:/mysql.sql):

```sql
source d:/mysql.sql;
```



#### 显示数据库列表

```sql
show databases;
```

缺省有两个数据库：mysql和test。 mysql库存放着mysql的系统和用户权限信息，我们改密码和新增用户，实际上就是对这个库进行操作。



#### 创建数据库

- 创建数据库：

```sql
CREATE DATABASE 数据库名称;
```

- 创建数据库(判断，如果不存在则创建)

```sql
CREATE DATABASE IF NOT EXISTS 数据库名称;
```

#### 使用数据库

- 使用数据库

```sql
USE 数据库名称;
```

- 查看当前使用的数据库

```sql
SELECT DATABASE();
```



#### 删除数据库

- 删除数据库

```sql
DROP DATABASE 数据库名称;
```

- 删除数据库(判断，如果存在则删除)

```sql
DROP DATABASE IF EXISTS 数据库名称;
```



#### 查询表

- 查询当前数据库下所有表名称

```sql
SHOW TABLES;
```

- 查询表结构

```sql
DESC 表名称;
```

- 查看建表语句

```sql
SHOW CREATE TABLE [表名]
```



#### 创建表

- 创建表

```sql
CREATE TABLE 表名 (
字段名1 数据类型1,
字段名2 数据类型2,
…
字段名n 数据类型n
);
```

> 注意：最后一行末尾，不能加逗号



#### 修改表

- 修改表名

```sql
ALTER TABLE 表名 RENAME TO 新的表名;
-- 将表名student修改为stu
alter table student rename to stu;
```

- 添加一列

```sql
ALTER TABLE 表名 ADD 列名 数据类型;
-- 给stu表添加一列address，该字段类型是varchar(50)
alter table stu add address varchar(50);
```

- 修改数据类型

```sql
ALTER TABLE 表名 MODIFY 列名 新数据类型;
-- 将stu表中的address字段的类型改为 char(50)
alter table stu modify address char(50);
```

- 修改列名和数据类型

```sql
ALTER TABLE 表名 CHANGE 列名 新列名 新数据类型;
-- 将stu表中的address字段名改为 addr，类型改为varchar(50)
alter table stu change address addr varchar(50);
```

- 删除列

```sql
ALTER TABLE 表名 DROP 列名;
-- 将stu表中的addr字段 删除
alter table stu drop addr;
```



##### 增加、删除和修改字段自增长

- 增加自增长字段

```sql
ALTER TABLE table_name ADD id INT NOT NULL AUTO_INCREMENT PRIMARY KEY;
```

>  注意：`table_name`代表要增加自增长字段的表名，`id`代表要增加的自增长字段名。

- 修改自增长字段

```sql
ALTER TABLE table_name CHANGE column_name new_column_name INT NOT NULL AUTO_INCREMENT PRIMARY KEY;
```

> `table_name`代表包含自增长字段的表名，`column_name`代表原始自增长字段名，`new_column_name`代表新的自增长字段名。请注意，将数据类型更改为INT，否则无法使该列成为自增长主键。完成后，需要重新启动表格才能使修改生效。



- 删除自增长字段

```sql
ALTER TABLE table_name MODIFY column_name datatype;
```

> 注意：`table_name`代表要删除自增长字段的表名，`column_name`代表要删除的自增长字段名，`datatype`代表要设置的数据类型。



##### 增加、删除和修改数据表的列

- 增加数据表的列

```sql
ALTER TABLE <表名> ADD COLUMN <列名> <数据类型>;
-- 在student表中增加一个名为age的INT类型列
ALTER TABLE student ADD COLUMN age INT;
```

- 删除数据表的列

```sql
ALTER TABLE <表名> DROP COLUMN <列名>;
-- 从student表中删除名为age的列
ALTER TABLE student DROP COLUMN age;
```

- 修改数据表的列

```sql
ALTER TABLE <表名> MODIFY COLUMN <列名> <数据类型>;
-- 将student表中的age列的数据类型修改为VARCHAR(10)
ALTER TABLE student MODIFY COLUMN age VARCHAR(10);
```



##### 添加、删除和查看索引

- 添加索引：

```sql
ALTER TABLE table_name ADD INDEX index_name (column_name);
-- 为名为users的表的email列添加名为idx_email的索引
ALTER TABLE users ADD INDEX idx_email (email);
```

- 删除索引：

```sql
ALTER TABLE table_name DROP INDEX index_name;
-- 删除名为users的表的idx_email索引
ALTER TABLE users DROP INDEX idx_email;
```

- 查看索引：

```sql
SHOW INDEX FROM table_name;
```



#### 删除表

- 删除表

```sql
DROP TABLE 表名;
```

- 删除表时判断表是否存在

```sql
DROP TABLE IF EXISTS 表名;
```



#### 创建其它类型表

- 创建临时表

```sql
CREATE TEMPORARY TABLE temp_table_name (  
    column1 datatype,  
    column2 datatype,  
    ...  
	);
-- 创建一个名为temp_users的临时表，其中包含id、name和email列。id列是主键。
CREATE TEMPORARY TABLE temp_users (  
    id INT PRIMARY KEY,  
    name VARCHAR(50),  
    email VARCHAR(100)  
);
```

- 创建内存表

```sql
CREATE TABLE mem_table_name (  
    column1 datatype,  
    column2 datatype,  
    ...  
) ENGINE=MEMORY;
-- 创建一个名为mem_users的内存表，其中包含id、name和email列。id列是主键
CREATE TABLE mem_users (  
    id INT PRIMARY KEY,  
    name VARCHAR(50),  
    email VARCHAR(100)  
) ENGINE=MEMORY;
```

> 注意：内存表存储在内存中，因此数据的修改会立即生效，并且对所有用户可见。但是，当MySQL服务器关闭时，内存表中的数据将丢失。因此，它适用于临时存储数据或缓存等场景。





### 数据操作-DML

#### 清空数据

```sql
truncate table 表名
delete from 表名
delete from 表名 where 列名="value "
drop form 表名
```

- truncate：删除所有数据，保留表结构，不能撤销还原
- delete：是逐行删除速度极慢，不适合大量数据删除
- drop：删除表，数据和表结构一起删除，快速





#### 添加记录

```sql
insert into 表名 values (字段列表);
```



批量插入添加记录

##### 循环插入

这个也是最普通的方式，如果数据量不是很大，可以使用，但是用for循环进行单条插入时，每次都是在获取连接(Connection)、释放连接和资源关闭等操作上，（如果数据量大的情况下）极其消耗资源，导致时间长。



##### 拼接一条sql

```sql
 INSERT INTO tablename ('username','password') values ('xxx','xxx'),('xxx','xxx'),('xxx','xxx'),('xxx','xxx')
```



##### 使用存储过程

```sql
1、修改 mysql 的界定符（语句结束符）
delimiter /// 或者 delimiter $$$      (左对齐，不要有空格)
2、创建存储过程
CREATE PROCEDURE 名字()
    declare i int default 0;
    set i=0;
    start transaction;
    while i<80000 do //这里是一次插入8万条
        //your insert sql 
        set i=i+1;
    end while;
commit;
$$$    # 表示结束，不加也可以，会随着存储过程的创建，会自动恢复 ;
delimiter;

3、调用存储过程
CALL 过程名称();
```





##### 使用MYSQL LOCAL_INFILE

LOAD DATA INFILE语句是MySQL中实现大数据批量插入的一种高效方式。该语句可以通过将文本文件中的数据加载到数据库表中，从而达到批量插入的目的。该语句的语法如下：

```sql
LOAD DATA [LOCAL] INFILE 'file_name'
[REPLACE|IGNORE]
INTO TABLE table_name
[CHARACTER SET charset_name]
[FIELD TERMINATED BY 'delimiter']
[LINES TERMINATED BY 'delimiter']
[IGNORE number LINES]
[(column1, column2, ..., column n)];
```

LOCAL为可选参数，表示将文本文件加载到本地MySQL客户端；file_name是文本文件的路径和名称；table_name是待插入数据的目标表；replace和ignore是可选参数，表示当目标表中存在同样的记录时，如何处理；charset_name是可选参数，指定文本文件的编码；delimiter是可选参数，指定字段和行的分隔符；number是可选参数，指定跳过文件的前几行；column1到column n表示待插入数据的字段名。

例如：

```sql
INTO TABLE mytable FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n' (col1, col2, col3);
```





#### 更新数据

```sql
update 表名 set 字段="值" where 子句 order by 子句 limit 子句
```

- WHERE 子句：可选项。用于限定表中要修改的行。若不指定，则修改表中所有的行。
- ORDER BY 子句：可选项。用于限定表中的行被修改的次序。
- LIMIT 子句：可选项。用于限定被修改的行数。



### 导出和导入数据

#### 导出数据：

```sql
mysqldump --opt test > mysql.test
```

即将数据库test数据库导出到mysql.test文本文件
例：

```sql
mysqldump -u root -p用户密码 --databases dbname > mysql.dbname
```

####  导入数据:

```sql
mysqlimport -u root -p用户密码 < mysql.dbname。
```

#### 将文本数据导入数据库:

文本数据的字段数据之间用tab键隔开。
```sql
use test;
load data local infile "文件名" into table 表名;
```



### 数据操作-select查询语句

#### 执行顺序

首先SQL语句的基本语法如下：

1. select 查询字段1，查询字段2，聚合函数(如count max)，distinct
2. from 表名
3. join on 表名
4. where 条件
5. group by 分组排列
6. having 条件
7. order by 排序（升序降序）
8. limit 结果限定

按照以上书写顺序，完整的执行顺序应该是这样：

1. from子句识别查询表的数据；
2. join on/union用于连接多表数据；
3. where子句基于指定的条件对记录进行筛选；
4. group by 子句将数据划分成多个组别，如按性别男、女分组；
5. 有聚合函数时，要使用聚集函数进行数据计算；
6. Having子句筛选满足第二条件的数据；
7. 执行select语句进行字段筛选
8. distinct筛选重复数据；
9. 对数据进行排序；
10. 执行limit进行结果限定。



举例，Mysql执行顺序：

1. select 查询字段 from 表列表名/视图列表名 where 条件； 
   执行顺序：先 from 再 where 最后select
2. select 查询字段 from 表列表名/视图列表名 where 条件 group by (列列表) having 条件
   执行顺序：先 from 再 where 再 group by 再 having 最后select
3. select 查询字段 from 表列表名/视图列表名 where 条件 group by (列列表) having 条件 order by 列列表
   执行顺序：先 from 再 where 再 group by 再 having 再 select 最后 order by
4. select 查询字段 from 表1 join 表2 on 表1.列1=表2.列1...join 表n on 表n.列1=表(n-1).列1 where 表1.条件 and 表2.条件...表n.
   执行顺序：先 from 再 join 再 where 最后 select

> having只能用于筛选分组后的结果，即group by之后



#### join

##### INNER JOIN

内连接需要用ON来指定两张表需要比较的字段，最终结果只显示满足条件的数据

```sql
SELECT * FROM tab1 INNER JOIN tab2 ON tab1.id1 = tab2.id2
```



##### LEFT JOIN

左连接可以看做在内连接的基础上，把左表中不满足ON条件的数据也显示出来，但结果中的右表部分中的数据为NULL

```sql
SELECT * FROM tab1 LEFT JOIN tab2 ON tab1.id1 = tab2.id2
```

​    ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407291702266.png)





##### RIGHT JOIN

右连接就是与左连接完全相反

```sql
SELECT * FROM tab1 RIGHT JOIN tab2 ON tab1.id1 = tab2.id2
```

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407291702188.png)





## 相关规范

### 命名规范

#### 库表命名规范

- 库名、表名必须使用小写字母，并采用下划线分割。 
- 库名、表名禁止超过32个字符。
- 库名、表名必须见名知意。命名与公司内业务、产品线等相关联。
- 库名、表名禁止使用MySQL保留字。（保留字列表见官方网站）
- 临时库、表名必须以tmp为前缀,并以日期为后缀。例如 tmp_test01_20130704。
- 备份库、表必须以bak为前缀,并以日期为后缀。例如 bak_test01_20130704。



#### 字段命名规范

- 字段名必须使用小写字母,并采用下划线分割，禁止驼峰式命名
- 字段名禁止超过32个字符。
- 字段名必须见名知意。命名与公司内业务、产品线等相关联。
- 字段名禁止使用MySQL保留字。（保留字列表见官方网站）



#### 索引命名规范

- 索引名必须全部使用小写字母，并采用下划线分割，禁用驼峰式。
- 非唯一索引按照“idx\_字段名称[_字段名称]”进用行命名。例如idx_age_name。
- 唯一索引按照“uniq\_字段名称[_字段名称]”进用行命名。例如uniq_age_name。
- 组合索引建议包含所有字段名，过长的字段名可以采用缩写形式。例如idx_age_name_add。





### 使用规范

#### 基础规范

- 使用INNODB存储引擎，必须要有主键，推荐使用业务不相关UNSIGNED  AUTO_INCREMENT列作为主键。
- 表字符集使用UTF8／UTF8MB4字符集。
- 所有表、字段都需要添加注释。推荐采用英文标点,避免出现乱码。
- 禁止在数据库中存储图片、文件等大数据。
- 每张表数据量建议控制在5000W以内。
- 禁止在线上做数据库压力测试。
- 禁止从测试、开发环境直连数据库



#### 索引规范

- 单张表中索引数量不超过5个。
- 单个索引中的字段数不超过5个。
- 索引名必须全部使用小写。
- 非唯一索引按照“idx_字段名称[_字段名称]”进行命名。例如idx_age_name。
- 唯一索引按照“uniq_字段名称[_字段名称]”进行命名。例如uniq_age_name。
- 组合索引建议包含所有字段名,过长的字段名可以采用缩写形式。例如idx_age_name_add。
- 表必须有主键,推荐使用UNSIGNED自增列作为主键。
- 唯一键由3个以下字段组成,并且字段都是整形时，可使用唯一键作为主键。其他情况下，建议使用自增列或发号器作主键。
- 禁止冗余索引。
- 禁止重复索引。
- 禁止使用外键。
- 联表查询时JOIN列的数据类型必须相同,并且要建立索引。
- 不在低基数列上建立索引，例如“性别”。
- 选择区分度大的列建立索引。组合索引中,区分度大的字段放在最前。
- 对字符串使用前缀索引，前缀索引长度建议不超过8个字符，需要根据业务实际需求确定。
- 不对过长的VARCHAR字段建立索引。建议优先考虑前缀索引，或添加CRC32或MD5伪列并建立索引。
- 合理创建联合索引，(a,b,c) 相当于 (a) 、(a,b) 、(a,b,c)。
- 合理使用覆盖索引减少IO，避免排序，因为索引本身就是有序的。



#### 字符集规范

- 表字符集使用UTF8，必要时可申请使用UTF8MB4字符集
  - UTF8字符集存储汉字占用3个字节，存储英文字符占用一个字节。
  - UTF8统一而且通用，不会出现转码出现乱码风险。
  - 如果遇到EMOJ等表情符号的存储需求，可申请使用UTF8MB4字符集。
- 同一个实例的库表字符集必须一致，JOIN字段字符集必须一致
- 禁止在字段级别设置字符集



### 字段设计规范

#### 库表设计

- 将大字段、访问频率低的字段拆分到单独的表中存储，分离冷热数据
- 表的默认字符集指定UTF8MB4(特殊需求除外)，无须指定排序规则
- 主键用整数类型，并且字段名称用id，使用AUTO_INCREMENT数据类，并指定UNSIGNE
- 禁止以非字母开头命名表名及库名
- 禁止使用分区表

#### 分表策略

- 推荐使用HASH进行散表，表名后缀使用十进制数，数字必须从0开始
- 按日期时间分表需符合YYYY\[MM]\[DD][HH]格式,例如2017011601。年份必须用4位数字表示。例如按日散表user_20170209、 按月散表user_201702
- 采用合适的分库分表策略。例如千库十表、十库百表等



### 字段设计及类型选择

- 建议使用UNSIGNED存储非负数值
- 建议使用INT UNSIGNED存储IPV4/IPV6，具体使用时根据函数进行转换
- 所有字段均定义为NOT NULL，设置默认值，Mysql不建议用Null作为列默认值不是因为不能使用索引，而是因为：
  - 索引列存在 NULL 就会导致优化器在做索引选择的时候更加复杂，更加难以优化。比如进行索引统计时，[count(1),max(),min() 会省略值为NULL 的行](https://www.seven97.top/database/mysql/04-tuning-overviewoftuning.html#count-和-count-1-哪个快)。
  - NULL 值是一个没意义的值，但是它会占用物理空间，所以会带来的存储空间的问题，因为 InnoDB 存储记录的时候，如果表中存在允许为 NULL 的字段，那么行格式 (opens new window)中**至少会用 1 字节空间存储 NULL 值列表**。建议用""或默认值0来代替NULL
- 用DECIMAL代替FLOAT和DOUBLE存储精确浮点数。例如与货币、金融相关的数据
- INT类型固定占用4字节存储，例如INT(4)仅代表显示字符宽度为4位，不代表存储长度
- 区分使用TINYINT、SMALLINT、MEDIUMINT、INT、BIGINT数据类型
- 使用VARBINARY存储大小写敏感的变长字符串或二进制内容
- 使用尽可能小的VARCHAR字段。VARCHAR(N)中的N表示字符数而非字节数
- 区分使用DATETIME和TIMESTAMP。存储年使用YEAR类型。存储日期使用DATE类型。存储时间(精确到秒)建议使用TIMESTAM
- 建议尽可能不使用ENUM类型替代TINYINT
- 禁止在数据库中存储明文密码
- 建议尽可能不使用TEXT、BLOB类型，innodb是以页为单位，默认16K，如果使用TEXT、BLOB类型会对innodb有性能影响

- 禁止使用外键
  - 外键会降低数据库的性能。在MySQL中，外键会自动加上索引，这会使得对该表的查询等操作变得缓慢，尤其是在大型数据表中。
  - 外键也会限制了表结构的调整和更改。在实际应用中，表结构经常需要进行更改，而如果表之间使用了外键约束，这些更改可能会非常难以实现。因为更改一个表的结构，需要涉及到所有以其为父表的子表，这会导致长时间锁定整个数据库表，甚至可能会导致数据丢失。
  - 在MySQL中，外键约束可能还会引发死锁问题。当想要对多个表中的数据进行插入、更新、删除操作时，由于外键约束的存在，可能会导致死锁，需要等待其他事务释放锁。
  - MySQL中使用外键还会增加开发难度。开发人员需要处理数据在表之间的关系，而这样的处理需要花费更多的时间和精力，以及对数据库的深入理解。同时，外键也会增加代码的复杂度，使得SQL语句变得难以理解和调试。





#### char和varchar的区别

VARCHAR：

- VARCHAR类型用于存储可变长度字符串，是最常见的字符串数据类型。它**比固定长度类型更节省空间**，因为它仅使用必要的空间(根据实际字符串的长度改变存储空间)。

- VARCHAR需要使用1或2个额外字节记录字符串的长度：如果列的最大长度小于或等于255字节，则只使用1个字节表示，否则使用2个字节。假设采用latinl字符集，一个VARCHAR(10)的列需要11个字节的存储空间。VARCHAR(1000)的列则需要1002 个字节，因为需要2个字节存储长度信息。
- VARCHAR节省了存储空间，所以对性能也有帮助。但是，由于行是变长的，在UPDATE时可能使行变得比原来更长，这就导致需要做额外的工作。如果一个行占用的空间增长，并且在页内没有更多的空间可以存储，在这种情况下，不同的存储引擎的处理方式是不一样的。例如，MylSAM会将行拆成不同的片段存储，InnoDB则需要分裂页来使行可以放进页内。



CHAR

- CHAR类型用于存储固定长度字符串：MySQL总是**根据定义的字符串长度分配足够的空间**。当存储CHAR值时，MySQL会删除字符串中的末尾空格
- 同时，CHAR值会根据需要采用空格进行剩余空间填充，以方便比较和检索。但正因为其长度固定，所以会占据多余的空间，也是一种空间换时间的策略；
- CHAR适合存储很短或长度近似的字符串。例如，CHAR非常适合存储密码的MD5值，因为这是一个定长的值。对于经常变更的数据，CHAR也比VARCHAR更好，因为定长的CHAR类型不容易产生碎片。对于非常短的列，CHAR比VARCHAR在存储空间上也更有效率。例如用CHAR(1)来存储只有Y和N的值，如果采用单字节字符集只需要一个字节，但是VARCHAR(1)却需要两个字节，因为还有一个记录长度的额外字节。
  
  

