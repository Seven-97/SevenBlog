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

#### 将文本数据导入数据库

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

内连接需要用ON来指定两张表需要比较的字段，最终结果只显示满足条件的数据，工作步骤如下：

1. **生成笛卡尔积**：首先，`INNER JOIN` 会生成两个表的笛卡尔积（即每一行与另一表的每一行进行组合）。在实际实现中，数据库查询优化器不会显式生成笛卡尔积，而是会直接进行连接条件的过滤。
2. **应用连接条件**：然后，`INNER JOIN` 会根据指定的连接条件（通常是相等条件）过滤这些组合，只保留那些在连接条件上匹配的行。
3. **生成结果集**：最后，根据过滤后的匹配行生成最终结果集。

```sql
SELECT * FROM tab1 INNER JOIN tab2 ON tab1.id1 = tab2.id2
```



##### LEFT JOIN

左连接可以看做在内连接的基础上，把左表中不满足ON条件的数据也显示出来，但结果中的右表部分中的数据为NULL。

`LEFT JOIN` 的工作原理是：从左表中的每一行开始，将其与右表中的每一行进行匹配。如果匹配成功，则返回匹配的组合行；如果匹配失败，则返回左表的行并用 `NULL` 填充右表的列。因此，左表中的每一行都要扫描右表。

```sql
SELECT * FROM tab1 LEFT JOIN tab2 ON tab1.id1 = tab2.id2
```

​    ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407291702266.png)



在进行左连接时，需要保证左表尽可能的小。下同，右连接需要保证右表尽可能小。
因为当左表较小时，需要扫描的数据量较小，I/O开销较低。如果左表很大，每一行都需要进行多次匹配操作，导致更高的I/O成本和数据扫描量。



##### RIGHT JOIN

右连接就是与左连接完全相反，从右表中的每一行开始，将其与左表中的每一行进行匹配。如果匹配成功，则返回匹配的组合行；如果匹配失败，则返回右表的行并用 `NULL` 填充右表的列。因此，右表中的每一行都要扫描左表。

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
- 库名、表名禁止使用MySQL保留字。（保留字列表见[官方网站](https://dev.mysql.com/doc/refman/5.7/en/keywords.html)）
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
- 所有表、字段(除主键外)都需要添加注释。推荐采用英文标点，避免出现乱码。
- 禁止在数据库中存储图片、文件等大数据。
- 每张表数据量建议控制在[2000W](https://www.seven97.top/database/mysql/01-basement1-innodbstoragestructure.html#为什么说mysql-一般单表不要超过-2000w-行)以内。
- 禁止在线上做数据库压力测试。
- 禁止从测试、开发环境直连数据库



#### 索引规范

- 单张表中索引数量不超过5个。
- 单个索引中的字段数不超过5个。
- 索引名必须全部使用小写。
- 非唯一索引按照“idx\_字段名称[_字段名称]”进行命名。例如idx_age_name。
- 唯一索引按照“uniq\_字段名称[_字段名称]”进行命名。例如uniq_age_name。
- 组合索引建议包含所有字段名，过长的字段名可以采用缩写形式。例如idx_age_name_add。
- 表必须有主键，推荐使用UNSIGNED自增列作为主键。
- 唯一键由3个以下字段组成，并且字段都是整形时，可使用唯一键作为主键。其他情况下，建议使用自增列作主键。
- 禁止冗余索引：例如(a,b,c)、(a,b)，后者为冗余索引。那么就建议将(a,b)索引删除即可。
- 禁止重复索引：例如 primary key a; uniq index a; 重复索引增加维护负担、占用磁盘空间,同时没有任何益处。
- 合理创建联合索引，(a,b,c) 相当于 (a) 、(a,b) 、(a,b,c)。
- 禁止使用外键。
- 联表查询时JOIN列的数据类型必须相同，并且要建立索引。如果join列有索引，但是数据类型不相同，那么是不会走索引的
- 不在低基数列上建立索引，例如“性别”。
- 选择区分度大的列建立索引。组合索引中，区分度大的字段放在最前。
- 对字符串使用前缀索引，前缀索引长度建议不超过8个字符，需要根据业务实际需求确定。
- 不对过长的VARCHAR字段建立索引。建议优先考虑前缀索引，或添加CRC32或MD5伪列并建立索引。
- 合理使用覆盖索引减少IO，避免排序：覆盖索引能从索引中获取需要的所有字段,从而避免回表进行二次查找,节省IO。



#### 字符集规范

- 表字符集使用UTF8，必要时可申请使用UTF8MB4字符集
  - UTF8字符集存储汉字占用3个字节，存储英文字符占用一个字节。
  - UTF8统一而且通用，不会出现转码出现乱码风险。
  - 如果遇到EMOJ等表情符号的存储需求，可申请使用UTF8MB4字符集。
- 同一个实例的库表字符集必须一致，JOIN字段字符集必须一致。当JOIN字段字符集不一致时，即使JOIN字段有索引，数据库也不会走索引
- 禁止在字段级别设置字符集



### 字段设计规范

#### 库表设计

- 禁止使用分区表
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

- 建议使用UNSIGNED存储非负数值：同样的字节数，非负存储的数值范围更大。如TINYINT有符号为 -128-127，无符号为0-255。
- 建议使用INT UNSIGNED存储IPV4/IPV6：UNSINGED INT存储IP地址占用4字节，CHAR(15)则占用15字节。另外，计算机处理整数类型比字符串类型快。使用INT UNSIGNED而不是CHAR(15)来存储IPV4地址,通过MySQL函数inet_ntoa和inet_aton来进行转化。IPv6地址目前没有转化函数,需要使用DECIMAL或两个BIGINT来存储。
- 所有字段**均定义为NOT NULL，设置默认值**
- 用DECIMAL代替FLOAT和DOUBLE存储精确浮点数。例如与货币、金融相关的数据
- INT类型固定占用4字节存储，例如INT(4)仅代表显示字符宽度为4位，不代表存储长度
- 区分使用TINYINT、SMALLINT、MEDIUMINT、INT、BIGINT数据类型
- 使用VARBINARY存储大小写敏感的变长字符串或二进制内容
- 使用尽可能小的VARCHAR字段。VARCHAR(N)中的**N表示字符数而非字节数**
- 区分使用DATETIME和TIMESTAMP。存储年使用YEAR类型。存储日期使用DATE类型。存储时间(精确到秒)建议使用TIMESTAM
- 强烈建议使用TINYINT来代替ENUM类型：ENUM类型在需要修改或增加枚举值时，需要在线DDL，成本较大；ENUM列值如果含有数字类型，可能会引起默认值混淆。
- 禁止在数据库中存储明文密码
- 禁止使用order by rand() ：order by rand()会为表增加一个伪列，然后用rand()函数为每一行数据计算出rand()值，然后基于该行排序，这通常都会生成磁盘上的临时表，因此效率非常低。建议先使用rand()函数获得随机的主键值，然后通过主键获取数据。
- 建议尽可能不使用TEXT、BLOB类型，innodb是以页为单位，默认16K，如果使用TEXT、BLOB类型会对innodb有性能影响





### 规范中提到的点的解释

#### 为什么建议用自增id作为主键

这主要是和mysql的[索引类型是B+ 树](https://www.seven97.top/database/mysql/01-basement2-indexclassification.html) 有关系

1. 如果使用自增主键，那么每次插入的新数据就会按顺序添加到索引节点最后的那个位置，就不需要移动已有的数据。当页面写满，就会自动开辟一个新页面。因为是自增主键，每次插入一条新记录，都是追加操作，不需要重新移动数据，因此这种插入数据的方法效率非常高。
2. 如果使用非自增主键，由于每次插入主键的索引值都是随机的，因此每次插入新的数据时，就可能会插入到现有数据页中间的某个位置，那么就不得不移动其它数据来满足新数据的插入，当前页插入不下了，那就会发生页分裂。造成额外的开销。页分裂还有可能会造成大量的内存碎片，导致索引结构不紧凑，从而影响查询效率。

> InnoDB 存储引擎会根据不同的场景选择不同的列作为索引：  
>
> 1. 如果有主键，默认会使用主键作为聚簇索引的索引键（key）；  
> 2. 如果没有主键，就选择第一个不包含 NULL 值的唯一列作为聚簇索引的索引键（key）；  
> 3. 在上面两个都没有的情况下，InnoDB 将自动生成一个隐式自增 id 列作为聚簇索引的索引键（key）；  



#### 为什么不建议使用null作为默认值

Mysql不建议用Null作为列默认值不是因为不能使用索引，而是因为：

- 索引列存在 NULL 就会导致优化器在做索引选择的时候更加复杂，更加难以优化。比如进行索引统计时，[count(1),max(),min() 会省略值为NULL 的行](https://www.seven97.top/database/mysql/04-tuning-overviewoftuning.html#count-和-count-1-哪个快)。
- NULL 值是一个没意义的值，但是它会占用物理空间，所以会带来的存储空间的问题，因为 InnoDB 存储记录的时候，如果表中存在允许为 NULL 的字段，那么行格式 (opens new window)中**至少会用 1 字节空间存储 NULL 值列表**。建议用""或默认值0来代替NULL



不建议使用null作为默认值，并且**建议必须设置默认值**，原因如下：

- 既然都不可为空了，那就必须要有默认值，否则不插入这列的话，就会报错；
- 数据库不应该是用来查问题的，不能靠mysql报错来告知业务有问题，该不该插入应该由业务说了算；
- 对于DBA来说，允许使用null是没有规范的，因为不同的人不同的用法。

> 但像`合同生效时间`、`获奖时间` 等这种不可控字段，是可以不设置默认值的，但同样需要not null



#### 为什么禁止使用外键

- 外键会降低数据库的性能。在MySQL中，外键会自动加上索引，这会使得对该表的查询等操作变得缓慢，尤其是在大型数据表中。
- 外键也会限制了表结构的调整和更改。在实际应用中，表结构经常需要进行更改，而如果表之间使用了外键约束，这些更改可能会非常难以实现。因为更改一个表的结构，需要涉及到所有以其为父表的子表，这会导致长时间锁定整个数据库表，甚至可能会导致数据丢失。
- 在MySQL中，外键约束可能还会引发死锁问题。当想要对多个表中的数据进行插入、更新、删除操作时，由于外键约束的存在，可能会导致死锁，需要等待其他事务释放锁。
- MySQL中使用外键还会增加开发难度。开发人员需要处理数据在表之间的关系，而这样的处理需要花费更多的时间和精力，以及对数据库的深入理解。同时，外键也会增加代码的复杂度，使得SQL语句变得难以理解和调试。

在阿里巴巴开发手册中也有提到，[传送门](https://www.seven97.top/books/software-quality/alibaba-developmentmanual.html#sql-语句)



#### char和varchar的区别

CHAR

- CHAR类型用于存储固定长度字符串：MySQL总是**根据定义的字符串长度分配足够的空间**。当存储CHAR值时，MySQL会删除字符串中的末尾空格同时，CHAR值会根据需要采用空格进行剩余空间填充，以方便比较和检索。但正因为其长度固定，所以会占据多余的空间，也是一种空间换时间的策略；
- CHAR适合存储很短或长度近似的字符串。例如，**CHAR非常适合存储密码的MD5值、定长的身份证等，因为这些是定长的值**。
- 对于经常变更的数据，CHAR也比VARCHAR更好，因为定长的CHAR类型占用磁盘的存储空间是连续分配的，不容易产生碎片。
- 对于非常短的列，CHAR比VARCHAR在存储空间上也更有效率。例如用CHAR(1)来存储只有Y和N的值，如果采用单字节字符集只需要一个字节，但是VARCHAR(1)却需要两个字节，因为还有一个记录长度的额外字节。



VARCHAR：

- VARCHAR类型用于存储可变长度字符串，是最常见的字符串数据类型。它**比固定长度类型更节省空间**，因为它仅使用必要的空间(根据实际字符串的长度改变存储空间)。

- VARCHAR需要使用1或2个额外字节记录字符串的长度：如果列的最大长度小于或等于255字节，则只使用1个字节表示，否则使用2个字节。假设采用latinl字符集，一个VARCHAR(10)的列需要11个字节的存储空间。VARCHAR(1000)的列则需要1002 个字节，因为需要2个字节存储长度信息。
- VARCHAR节省了存储空间，所以对性能也有帮助。但是，由于行是变长的，在UPDATE时可能使行变得比原来更长，这就导致需要做额外的工作。如果一个行占用的空间增长，并且在页内没有更多的空间可以存储，在这种情况下，不同的存储引擎的处理方式是不一样的。例如，MylSAM会将行拆成不同的片段存储，InnoDB则需要分裂页来使行可以放进页内。
- 操作内存的方式：对于varchar数据类型来说，硬盘上的存储空间虽然都是根据字符串的实际长度来存储空间的，但在内存中是根据varchar类型定义的长度来分配占用的内存空间的，而不是根据字符串的实际长度来分配的。显然，这对于排序和临时表会较大的性能影响。







#### utf8 、utf8mb3和 utf8mb4的区别

##### 字符集

**字符集：** 是多个字符的`有序`集合，而有多种字符集是因为**每个字符集包含的字符数量不同**，然而各国的字符内容不一，需求不一，这就不得不新增一种标准，于是就出现了 ASCII 字符集、GB2312 字符集、ISO-8859-1 字符集、 GB18030 字符集、Unicode 字符集…

utf8 是 MySQL中的一种字符集，**utf8是utf8mb3的别名**，除此之外并无不同（如下图，MySQL官网已经给出说明）

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407310956190.png)



**utf8mb3**：只支持最长三个字节的BMP（Basic Multilingual Plane，基本多文种平面）字符（不支持补充字符）。

**utf8mb4**：mb4即 most bytes 4，即最多使用4个字节来表示完整的UTF-8，具有以下特征：

- 支持BMP和补充字符。
- 每个多字节字符最多需要四个字节。

utf8mb4是utf8的超集并完全兼容它，是MySQL 在 5.5.3 版本之后增加的一个新的字符集，能够用四个字节存储更多的字符，几乎包含了世界上所有能看到见的语言字符。



- **差异比较**

| 差异点         | utf8mb3                                                      | utf8mb4                                                      |
| -------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 最大使用字节数 | 3                                                            | 4                                                            |
| 支持字符类型   | BMP                                                          | BMP+其它字符                                                 |
| 字符类型       | 常见的 Unicode 字符                                          | 常见的 Unicode 字符 + 部分罕用汉字 + emoji表情 + 新增的 Unicode 字符等 |
| Unicode范围    | U0000 - U+FFFF（即BMP)                                       | U0000 - U+10FFFF                                             |
| 占用存储空间   | 略小（如CHAR(10) 需要10 * 3 = 30 个字节的空间；VARCHAR 类型需要额外使用1个字节来记录字符串的长度） | 稍大（如CHAR(10) 需要 10 * 4 = 40 个字节的空间；VARCHAR 类型需要额外使用2个字节来记录字符串的长度） |
| 兼容性         | 切换至utf8mb4 一般不会有问题，但要注意存储空间够不够、排序规则是否变化 | 切换至utf8mb3可能会有问题，字符丢失、报错或乱码              |
| 安全性         | 稍低，更容易被恶意字符串攻击                                 | 较高，保留恶意字符串，然后报错或乱码提示                     |

> 如何选择？一句话就是，根据具体的业务需求和实际情况，选择最合适的字符集。



##### 排序规则

排序规则（collation），字符串**在比较和排序时所遵循的规则**。不同的字符集有不同的排序规则，同一个字符集也可以有多种排序规则。在 MySQL 8.0.1 及更高版本中将 utf8mb4_0900_ai_ci（属于 utf8mb4_unicode_ci 中的一种） 作为默认排序规则，在这之前 utf8mb4_general_ci 是默认排序规则。

MySQL常用排序规则有：utf8mb4_general_ci、utf8mb4_unicode_ci、utf8mb4_bin、utf8mb4_0900_ai_ci：

- `_bin` ： 按二进制方式比较字符串，区分大小写和重音符号。
- `_ai_ci`：按照特定语言或地区方式比较字符串，不区分大小写和重音符号。
- `_unicode_ci`： 按 Unicode 标准方式比较字符串，不区分大小写和重音符号。
- `_general_ci`：按一般方式比较字符串，不区分大小写和重音符号。

utf8mb4_unicode_ci 和 utf8mb4_general_ci 的区别：

- `utf8mb4_unicode_ci`： 能够在各种语言之间精确排序，Unicode排序规则为了能够处理特殊字符的情况，实现了略微复杂的排序算法。
- `utf8mb4_general_ci`： 不支持扩展，它仅能够在字符之间进行逐个比较。utf8_general_ci 比较速度很快，但比 utf8mb4_unicode_ci 的准确性稍差一些。
- 准确性：utf8mb4_unicode_ci 是**精确排序**，utf8mb4_general_ci 没有实现 Unicode 排序规则，在遇到某些特殊语言或者字符集，排序结果可能不一致。
  因此，准确性是**utf8mb4_unicode_ci > utf8mb4_general_ci**
- 性能：utf8mb4_general_ci 在**比较和排序的时候更快**。



##### 对VARCHAR的的优化

对于 UTF8MB4 字符类型：

- 字符个数小于 50 个，建议设置为 VARCHAR(50)，或更小的字符长度。
- 字符个数接近 64（256/4=64）个，建议设置为 VARCHAR(64) 或更大的字符长度。



由于 UTF8MB4 为四字节编码字符集，即一个字节长度可存储 63.75（255/4）个字符，所以当我们将 VARCHAR(63) 修改为 VARCHAR(64) 时，需要增加一个字节去进行数据的存储，就要通过建立临时表的方式去完成本次长度扩容，故需要花费大量时间。

对于字段的最大字节长度在 256 字符内变化 (即 x\*4<256 且 Y\*4<256)，online ddl 走 inplace 模式，效率高。
对于字段的最大字节长度在 256 字符外变化 (即 x\*4>=256 且 Y\*4>=256) ，online ddl 走 inplace 模式，效率高。否则，online ddl 走 copy 模式，效率低.

UTF8(MB3) 同理。






<!-- @include: @article-footer.snippet.md -->     
