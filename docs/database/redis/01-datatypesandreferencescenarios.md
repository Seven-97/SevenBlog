---
title: Redis数据类型全攻略：String、List、Hash、Set的实战应用
category: 数据库
tags:
  - Redis
  - 缓存
head:
  - - meta
    - name: keywords
      content: redis,redis数据类型,redis应用场景,string,list,hash,set
  - - meta
    - name: description
      content: 全网最全的Redis知识点总结，让天下没有难学的八股文！
---



## 五种常见数据类型
Redis中的数据类型指的是 value存储的数据类型，key都是以String类型存储的，value根据场景需要，可以以String、List等类型进行存储。
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270801454.png)

各数据类型介绍：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270801988.png)

Redis数据类型对应的底层数据结构
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270801490.png)

## String 类型的应用场景

### 常用命令

- set key value [EX seconds] [PX milliseconds] [NX|XX]：存放键值
	- [EX seconds]：单位是秒
	- [PX milliseconds]：单位是毫秒
	-  [NX|XX] : 也可以直接使用setnx/setex命令
	    - nx：如果key不存在则建立；如果key已存在则无返回值，不做任何动作
	    - xx：如果key存在则修改其值
- get key：获取指定key的字符串值
	- 若key存在且为字符串类型，返回对应值
	- 若key不存在或非字符串类型，返回`nil`

可以对获取的值进行判断，是否为空(别的命令的获取值的方法都可以用这个)：
```java

private static final String NIL = "nil";
private static final String NULL = "null";
 /**
 * 检查redis返回的结果是否是空数据
 *
 * @param result redis返回结果
 * @return 是否是空
 */
public static boolean isRedisReturnEmpty(String result) {
 return Strings.isNullOrEmpty(result)
 || StringUtils.equalsIgnoreCase(NULL, result)
 || StringUtils.equalsIgnoreCase(NIL, result);
}
```

- incr key：将key存储的整数值增加1（原子操作）
	- 若key不存在，自动初始化为`0`后再执行操作
	- 若值非整数，返回`ERR value is not an integer`错误
	- 一次想递增N用incrby命令，如果是浮点型数据可以用incrbyfloat命令递增。
	- 同样，递减使用decr、decrby命令。
- mset key value [key value ...]：原子性设置多个key-value对（覆盖已存在值）。
	- 始终返回`OK`
	- 覆盖所有已存在的key，无视类型（如将列表类型覆盖为字符串）
- mget key [key ...]：批量获取多个key的值（按输入顺序返回）。
	- 返回值：列表（不存在的key返回`nil`）

> mget，mset 只有1次网络请求；使原子性的
> 单机Redis下就是1个命令，集群中则可能键分布在不同的节点，导致拆分成多个请求(客户端仍视为一次调用)，因此集群模式下需确保所有key在同一哈希槽

- strlen key：获取key存储的字符串值的字节长度
	- 返回值：字符串长度（如中文UTF-8占3字节）
	- key不存在时返回0
- append key value：向key的字符串值末尾追加内容
	- 返回值：追加后的字符串总长度
	- key不存在时自动创建（等效`SET`）
- getrange key start end：截取字符串的子串（闭区间，支持负数索引）
	- 返回值：子字符串（包括`start`和`end`位置的字符）
	- 负数索引表示从末尾倒数（如`-1`为最后一个字符）
	- 超出范围自动截断
	- key不存在则返回空

### 缓存对象
使用 String 来缓存对象有两种方式：
- 直接缓存整个对象的 JSON，命令例子： SET user:1 '{"name":"seven", "age":18}'。
- 采用将 key 进行分离为 user:ID:属性，采用 MSET 存储，用 MGET 获取各属性值，命令例子： MSET user:1:name seven1 user:1:age 18 user:2:name seven2 user:2:age 20

### 常规计数
比如计算访问次数、点赞、转发、库存数量等等。

```
# 初始化文章的阅读量
> SET aritcle:readcount:1001 0
OK
#阅读量+1
> INCR aritcle:readcount:1001
1 
#阅读量+1
> INCR aritcle:readcount:1001
2 
```

[Redis实现高并发场景下的计数器设计](https://www.seven97.top/database/redis/07-practice-addone.html)

### [分布式锁](https://www.seven97.top/database/redis/05-implementdistributedlocks.html)

之所以采用Redis来作为分布式锁，可以有几方面理由：

1. redis足够的快
2. redis提供了`setnx + expire`的机制，完全契合分布式锁的实现要点
3. `Redisson`客户端的流行，使得基于redis的分布式锁更加简单



SET 命令有个 NX 参数可以实现「key不存在才插入」，可以用它来实现分布式锁：

- 如果 key 不存在，则显示插入成功，可以用来表示加锁成功；
- 如果 key 存在，则会显示插入失败，可以用来表示加锁失败。

一般而言，还会对分布式锁加上过期时间，分布式锁的命令如下：
```
SET lock_key unique_value NX PX 10000
```

- lock_key 就是 key 键；
- unique_value 是客户端生成的唯一的标识；
- NX 代表只在 lock_key 不存在时，才对 lock_key 进行设置操作；
- PX 10000 表示设置 lock_key 的过期时间为 10s，这是为了避免应用在运行过程中发生异常而无法释放锁。

### 共享 session 信息
通常情况下可以使用session信息保存用户的登录（会话）状态，由于这些 Session 信息会被保存在服务器端，如果用户一的 Session 信息被存储在服务器一，但第二次访问时用户一被分配到服务器二，这个时候服务器并没有用户一的 Session 信息，就会出现需要重复登录的问题。如下：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270801135.png)

可以借助 Redis 对这些 Session 信息进行统一的存储和管理，这样无论请求发送到那台服务器，服务器都会去同一个 Redis 获取相关的 Session 信息，这样就解决了分布式系统下 Session 存储的问题。
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270801688.png)

## List 类型的应用场景

### 常用命令

- 存储值：
	- lpush key value [value ...]：将一个或多个值插入到列表头部（左侧）
		- 返回值：插入后列表的长度。若key不存在，自动创建空列表后执行操作
		- 时间复杂度：O(N)，N为插入元素数量
	- rpush key value [value ...]：将一个或多个值插入到列表尾部（右侧）
		- 返回值：插入后列表的长度。若key不存在，自动创建空列表后执行操作
		- 时间复杂度：O(N)，N为插入元素数量
	- lset key index value：通过索引设置列表元素的值
		- 返回值​：成功返回`OK`，索引超范围返回错误
		- 索引从0开始，负数表示从末尾倒数（-1为最后一个元素）
		- 时间复杂度：O(N)，首尾元素操作时为O(1)
- 获取列表元素：
	- lrange key start stop：获取列表中指定区间的元素（闭区间）
		- 返回值​：元素列表。若key不存在返回空列表
		- 支持负数索引（如`LRANGE mylist 0 -1`获取全部元素）。若stop超出范围自动截断到列表末尾
	- lindex key index：通过索引获取单个元素
		- 返回值：元素值。若索引无效，则返回`nil`
		- 时间复杂度：O(N)，但对首尾元素操作时为O(1)
- 弹出元素：
	- lpop key：移除并返回列表头部（左侧）元素
		- 返回值：被移除的元素值。若列表为空，则返回`nil`
	- rpop key：移除并返回列表尾部（右侧）元素
		- 返回值：被移除的元素值。若列表为空，则返回`nil`
- llen key：获取元素个数
	- 返回值​：列表长度。若key不存在时返回0
	- 时间复杂度：O(1)
- 删除元素：
	- lrem key count value：移除列表中与value匹配的元素，count表示删除的数量
		- 返回值：实际移除的元素数量
		- `count > 0`表示从头部开始移除count个匹配元素；`count < 0`表示从尾部开始移除|count|个匹配元素；`count = 0`表示移除所有匹配元素
	- ltrim key start stop：修剪列表，只保留指定区间内的元素
		- 返回值​：成功返回`OK`
		- 若start > stop或key不存在，列表会被清空


### 消息队列

- 消息保序：使用 LPUSH + RPOP，对队列进行先进先出的消息处理；满足消息队列的保序性
- 阻塞读取：使用 BRPOP；阻塞读取队列中的数据，避免消费者不停地调用 RPOP 命令带了不必要的性能损失
- 重复消息处理：生产者实现全局唯一 ID；满足消息队列的处理重复消息的能力
- 消息的可靠性：使用 BRPOPLPUSH让消费者程序从一个 List 中读取消息，同时，Redis 会把这个消息再插入到另一个 List（可以叫作备份 List）留存；这样一来，如果消费者程序读了消息但没能正常处理，等它重启后，就可以从备份 List 中重新读取消息并进行处理了。满足消息队列的可靠性

但是有两个问题：
1. 生产者需要自行实现全局唯一 ID；
2. 不能以消费组形式消费数据

## Hash 类型

### 常用命令

- 存放值：
  - hset key field value：设置哈希表中字段的值（若字段已存在则覆盖）。
	  - 若字段是新建的，返回`1`；
	  - 若字段已存在且值被覆盖，返回`0`；
  - hmset key field value [field value ...]：批量设置哈希表中多个字段的值（覆盖已存在字段）。成功时返回`OK`
  - hsetnx key field value：仅当字段不存在时设置值（原子性操作）。
	  - 若字段新建成功，返回`1`；
	  - 若字段已存在，返回`0`
- 获取字段值：
  - hget key field：获取哈希表中单个字段的值。
	  - 若字段存在，返回对应值（字符串类型）；
	  - 若字段或键不存在，返回`nil`
  - hmget key field [field ...]：批量获取哈希表中多个字段的值。
	  - 返回顺序与请求字段顺序一致的列表；
	  - 不存在的字段返回`nil`
  - hgetall key：获取哈希表中所有字段和值（键值对交替排列）。
	  - 返回包含所有字段和值的列表（如`["field1","val1","field2","val2"]`）；
	  - 若键不存在或为空，返回空列表
  - hkeys key：返回哈希表中所有字段名（field names）组成的列表。若哈希表不存在或为空，返回空列表；
  - hvals key：返回哈希表中所有字段值（values）组成的列表。若哈希表不存在或为空，返回空列表；
- hexists key field：判断是否存在。
	- 若字段存在，则返回1
	- 若字段不存在或键不存在，则返回0
- hlen key：获取哈希表`key`中字段（field）的数量
	- 返回字段数量，整数
	- 若键不存在或哈希表为空，则返回0
- hincrby key field increment：递增/减，将哈希表`key`中字段`field`的整数值增加`increment`（可为负数，负数就是递减）
	- 操作后字段的新值（整数）
	- 若字段不存在，则自动创建并初始化为`0`后再执行操作
- hdel key field [field ...]：删除哈希表`key`中的一个或多个字段`field`
	- 成功删除的字段field数量（不统计不存在的字段）
	- 若删除后哈希表为空，键会自动删除，不存在的字段会被忽略
	- 时间复杂度：O(N)，N为删除的字段数量



### 缓存对象
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270801706.png)

一般对象用 String + Json 存储，对象中某些频繁变化的属性可以考虑抽出来用 Hash 类型存储。

### 购物车

以用户 id 为 key，商品 id 为 field，商品数量为 value，恰好构成了购物车的3个要素，如下图所示。
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270802408.png)

涉及的命令如下：
- 添加商品：HSET cart:{用户id} {商品id} 1
- 添加数量：HINCRBY cart:{用户id} {商品id} 1
- 商品总数：HLEN cart:{用户id}
- 删除商品：HDEL cart:{用户id} {商品id}
- 获取购物车所有商品：HGETALL cart:{用户id}

## Set 类型
聚合计算（并集、交集、差集）场景，比如点赞、共同关注、抽奖活动等。

### 常用命令

- sadd key member [member ...]：向集合中添加一个或多个成员元素
	- 返回值​：成功添加的新成员数量（已存在的成员不计入）。若已存在，则返回0
	- 若集合不存在，自动创建新集合
	- 元素唯一性：重复添加同一元素会被忽略
- smembers key：返回集合中所有成员（无序）
	- 返回值​：成员列表。若集合为空或键不存在返回空列表
	- 注意：大集合（如超过1万元素）慎用，可能阻塞Redis
- srandmember key count：随机返回集合中一个或多个元素（不删除元素）
	- 随机元素或列表（若count未指定则返回单个元素）
	- `count>0`表示返回不重复的count个元素；`count<0`表示返回可能重复的abs(count)个元素
- sismember key member：检查元素是否存在于集合中
	- 返回值​：返回`1`表示元素存在；返回`0`表示元素不存在或集合不存在
	- 时间复杂度：O(1)
- scard key：获取集合的基数（元素数量）
	- 返回值​：整数；若集合不存在时返回`0`
- srem key member [member ...]：移除集合中一个或多个指定成员
	- 返回值：实际移除的成员数量（忽略不存在的成员）
- spop key [count]：随机移除并返回一个或多个成员
	-  `count`可选，指定移除数量
	- 返回值：被移除的元素或列表（集合为空时返回`nil`）
	- 与SRANDMEMBER区别：SPOP会删除元素，SRANDMEMBER仅读取


### 点赞
可以保证**一个用户只能点一个赞**，已经点赞过的用户不能再点赞
```
# uid:1 用户对文章 article:1 点赞
> SADD article:1 uid:1
(integer) 1
# 第二次使用同一个value：uid:1 用户对文章 article:1 点赞。返回的是0
> SADD article:1 uid:1
(integer) 0

# uid:2 用户对文章 article:1 点赞
> SADD article:1 uid:2
(integer) 1
# uid:3 用户对文章 article:1 点赞
> SADD article:1 uid:3
(integer) 1

# uid:1 取消了对 article:1 文章点赞。
> SREM article:1 uid:1
(integer) 1

# 获取 article:1 文章所有点赞用户 :
> SMEMBERS article:1
1) "uid:3"
2) "uid:2"

# 获取 article:1 文章的点赞用户数量：
> SCARD article:1
(integer) 2  #不重复value数量
```

### 共同关注
Set 类型**支持交集运算**，所以可以用来计算共同关注的好友、公众号等。

key 可以是用户id，value 则是已关注的公众号的id。
```
# uid:1 用户关注公众号 id 为 5、6、7、8、9
> SADD uid:1 5 6 7 8 9
(integer) 5
# uid:2  用户关注公众号 id 为 7、8、9、10、11
> SADD uid:2 7 8 9 10 11
(integer) 5

# 获取共同关注
> SINTER uid:1 uid:2
1) "7"
2) "8"
3) "9"

# 给 uid:2 推荐 uid:1 关注的公众号：在uid:1中有但是uid:2中没有的
> SDIFF uid:1 uid:2
1) "5"
2) "6"

# 验证某个公众号是否同时被 uid:1 或 uid:2 关注:
> SISMEMBER uid:1 5
(integer) 1 # 返回0，说明关注了
> SISMEMBER uid:2 5
(integer) 0 # 返回0，说明没关注
```

### 抽奖活动
存储某活动中中奖的用户名 ，Set 类型因为有去重功能，可以**保证同一个用户不会中奖两次**。

```shell
# key为抽奖活动名，value为员工名称，把所有员工名称放入抽奖箱 ：
>SADD lucky Tom Jerry John Sean Marry Lindy Sary Mark
(integer) 5

# 如果允许重复中奖，可以使用 SRANDMEMBER 命令。
# 抽取 1 个一等奖：
> SRANDMEMBER lucky 1
1) "Tom"
# 抽取 2 个二等奖：
> SRANDMEMBER lucky 2
1) "Mark"
2) "Jerry"
# 抽取 3 个三等奖：
> SRANDMEMBER lucky 3
1) "Sary"
2) "Tom"
3) "Jerry"

# 如果不允许重复中奖，可以使用 SPOP 命令。
# 抽取一等奖1个
> SPOP lucky 1
1) "Sary"
# 抽取二等奖2个
> SPOP lucky 2
1) "Jerry"
2) "Mark"
# 抽取三等奖3个
> SPOP lucky 3
1) "John"
2) "Sean"
3) "Lindy"
```


## Zset 类型
排序场景，比如排行榜、电话和姓名排序等。

### 常用命令

- 存储值：zadd key [NX|XX] [CH] [INCR] score member [score member ...]
- 获取元素分数：zscore key member
- 获取排名范围：zrange key start stop [WITHSCORES]
- 获取指定分数范围排名：zrangebyscore key min max [WITHSCORES] [LIMIT offset count]
- 增加指定元素分数：zincrby key increment member
- 获取集合元素个数：zcard key
- 获取指定范围分数个数：zcount key min max
- 删除指定元素：zrem key member [member ...]
- 获取元素排名：zrank key member



### Zset结构
```C
typedef struct zset {
    dict *dict;//哈希表
    zskiplist *zsl;//跳表
} zset;
```

zset 结构体里有两个数据结构：一个是跳表，一个是哈希表。这样的好处是既能进行高效的范围查询（如 ZRANGEBYSCORE 操作，利用了跳表），也能进行高效单点查询（如 ZSCORE 操作，利用了hash表）。

### 排行榜

五篇博文，分别获得赞为 200、40、100、50、150。
```c
# arcticle:1 文章获得了200个赞
> ZADD user:seven:ranking 200 arcticle:1
(integer) 1
# arcticle:2 文章获得了40个赞
> ZADD user:seven:ranking 40 arcticle:2
(integer) 1
# arcticle:3 文章获得了100个赞
> ZADD user:seven:ranking 100 arcticle:3
(integer) 1
# arcticle:4 文章获得了50个赞
> ZADD user:seven:ranking 50 arcticle:4
(integer) 1
# arcticle:5 文章获得了150个赞
> ZADD user:seven:ranking 150 arcticle:5
(integer) 1

# 获取文章赞数最多的 3 篇文章, ZREVRANGE 命令（倒序获取有序集合 key 从start下标到stop下标的元素）
# WITHSCORES 表示把 score 也显示出来
> ZREVRANGE user:seven:ranking 0 2 WITHSCORES
1) "arcticle:1"
2) "200"
3) "arcticle:5"
4) "150"
5) "arcticle:3"
6) "100"

# 获取 100 赞到 200 赞的文章，ZRANGEBYSCORE 命令（返回有序集合中指定分数区间内的成员，分数由低到高排序）
> ZRANGEBYSCORE user:xiaolin:ranking 100 200 WITHSCORES
1) "arcticle:3"
2) "100"
3) "arcticle:5"
4) "150"
5) "arcticle:1"
6) "200"
```

### 电话，姓名排序
#### 电话排序
```
# 将电话号码存储到 SortSet 中，然后根据需要来获取号段：
> ZADD phone 0 13100111100 0 13110114300 0 13132110901 
(integer) 3
> ZADD phone 0 13200111100 0 13210414300 0 13252110901 
(integer) 3
> ZADD phone 0 13300111100 0 13310414300 0 13352110901 
(integer) 3

# 获取所有号码
> ZRANGEBYLEX phone - +
1) "13100111100"
2) "13110114300"
3) "13132110901"
4) "13200111100"
5) "13210414300"
6) "13252110901"
7) "13300111100"
8) "13310414300"
9) "13352110901"

# 获取 132 号段的号码：
> ZRANGEBYLEX phone [132 (133
1) "13200111100"
2) "13210414300"
3) "13252110901"

# 获取132、133号段的号码：
> ZRANGEBYLEX phone [132 (134
1) "13200111100"
2) "13210414300"
3) "13252110901"
4) "13300111100"
5) "13310414300"
6) "13352110901"
```

#### 姓名排序

```
> zadd names 0 Toumas 0 Jake 0 Bluetuo 0 Gaodeng 0 Aimini 0 Aidehua 
(integer) 6

# 获取所有人的名字:
> ZRANGEBYLEX names - +
1) "Aidehua"
2) "Aimini"
3) "Bluetuo"
4) "Gaodeng"
5) "Jake"
6) "Toumas"

# 获取名字中大写字母A开头的所有人：
> ZRANGEBYLEX names [A (B
1) "Aidehua"
2) "Aimini"

# 获取名字中大写字母 C 到 Z 的所有人：
> ZRANGEBYLEX names [C [Z
1) "Gaodeng"
2) "Jake"
3) "Toumas"
```

## BitMap（2.2 版新增）：

### 介绍

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270802681.png)

适用于二值状态统计的场景。

### 签到
只记录签到（1）或未签到（0）

```
# 记录用户 4 月 3 号已签到
SETBIT uid:sign:100:202304 2 1

# 检查该用户 6 月 3 日是否签到
> GETBIT uid:sign:100:202306 3
1

# 统计用户在 6 月份的签到次数
> BITCOUNT uid:sign:100:202206
1

# 统计这个月首次打卡时间；BITPOS key bitValue [start] [end]，start end 表示要检测的范围
BITPOS uid:sign:100:202206 1
```

### 判断用户登陆状态
key = login_status 表示存储用户登陆状态集合数据， 将用户 ID 作为 offset，在线就设置为 1，下线设置 0。通过 GETBIT判断对应的用户是否在线。 5000 万用户只需要 6 MB 的空间。

```
# 表示ID = 10086 的用户已登陆
SETBIT login_status 10086 1

# 检查该用户是否登陆，返回值 1 表示已登录
GETBIT login_status 10086

# 登出，将 offset 对应的 value 设置成 0。
SETBIT login_status 10086 0
```

### 连续签到用户总数

把每天的日期作为 Bitmap 的 key，userId 作为 offset，若是打卡则将 offset 位置的 bit 设置成 1。key 对应的集合的每个 bit 位的数据则是一个用户在该日期的打卡记录。

那就可以设置 7 个 Bitmap，对这 7 个 Bitmap 的对应的 bit 位做 『与』运算。那么当一个 userID 在 7 个 Bitmap 对应对应的 offset 位置的 bit = 1 就说明该用户 7 天连续打卡。结果保存到一个新 Bitmap 中，我们再通过 BITCOUNT 统计 bit = 1 的个数便得到了连续打卡 7 天的用户总数了。

## HyperLogLog（2.8 版新增）
海量数据基数统计的场景，提供不精确的去重计数。但要注意，HyperLogLog 的统计规则是基于概率完成的，不是非常准确，标准误算率是 0.81%。因此适用于海量数据的场景。

HyperLogLog 的优点是，在输入元素的数量或者体积非常非常大时，计算基数所需的内存空间总是固定的、并且是很小的。在 Redis 里面，每个 HyperLogLog 键只需要花费 12 KB 内存，就可以计算接近 2^64 个不同元素的基数，和元素越多就越耗费内存的 Set 和 Hash 类型相比，HyperLogLog 就非常节省空间。

### 百万级网页 UV 计数

在统计 UV 时，可以用 PFADD 命令（用于向 HyperLogLog 中添加新元素）把访问页面的每个用户都添加到 HyperLogLog 中。
```
PFADD page1:uv user1 user2 user3 user4 user5

# 可以用 PFCOUNT 命令直接获得 page1 的 UV 值，获取统计结果
PFCOUNT page1:uv
```

## GEO（3.2 版新增）
存储地理位置信息的场景

Redis GEO 操作方法有：

- geoadd：添加地理位置的坐标。
- geopos：获取地理位置的坐标。
- geodist：计算两个位置之间的距离。
- georadius：根据用户给定的经纬度坐标来获取指定范围内的地理位置集合。
- georadiusbymember：根据储存在位置集合里面的某个地点获取指定范围内的地理位置集合。
- georadius：以给定的经纬度为中心， 返回键包含的位置元素当中， 与中心的距离不超过给定最大距离的所有位置元素。


GEORADIUS方法参数：
```
GEORADIUS key longitude latitude radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count] [ASC|DESC] [STORE key] [STOREDIST key]
```
参数说明：
- m ：米，默认单位。
- km ：千米。
- mi ：英里。
- ft ：英尺。
- WITHDIST: 在返回位置元素的同时， 将位置元素与中心之间的距离也一并返回。
- WITHCOORD: 将位置元素的经度和维度也一并返回。
- WITHHASH: 以 52 位有符号整数的形式， 返回位置元素经过原始 geohash 编码的有序集合分值。这个选项主要用于底层应用或者调试， 实际中的作用并不大。
- COUNT 限定返回的记录数。
- ASC: 查找结果根据距离从近到远排序。
- DESC: 查找结果根据从远到近排序。

### 滴滴叫车
假设车辆 ID 是 33，经纬度位置是（116.034579，39.030452），可以用一个 GEO 集合保存所有车辆的经纬度，集合 key 是 cars:locations。
```
GEOADD cars:locations 116.034579 39.030452 33
```

当用户想要寻找自己附近的网约车时，LBS 应用就可以使用 GEORADIUS 命令。  
例如，LBS 应用执行下面的命令时，Redis 会根据输入的用户的经纬度信息（116.054579，39.030452 ），查找以这个经纬度为中心的 5 公里内的车辆信息，并返回给 LBS 应用。
```
GEORADIUS cars:locations 116.054579 39.030452 5 km ASC COUNT 10
```

### 附近的人

nearbyPeople 是一个总的 key，user_1 和 user_2 是相当于 nearbyPeople 里面的两个元素以及他们对应的经纬度，这个例子就是把 user_1 和 user_2 的经纬度存在了 nearbyPeople 这个 key 中
```
redis> GEOADD nearbyPeople 13.36 38.11 "user_1" 15.08 37.50 "user_2"  
(integer) 2
```

获取 nearbyPeople 中的元素 user_1 和 user_2 这两个元素的经纬度，当然如果之前没有 geoadd 相对应元素的经纬度的话，会返回 nil
```
redis> GEOPOS nearbyPeople user_1 user_21) 1) "13.36138933897018433"   2) "38.11555639549629859"2) 1) "15.08726745843887329"   2) "37.50266842333162032"
```

获取 nearbyPeople 中 user_1 和 user_2 这两个节点之间的距离，距离单位可以指定，如下所示：
- m ：米，默认单位。
- km ：千米。
- mi ：英里。
- ft ：英尺。
```
redis> GEODIST nearbyPeople user_1 user_2"166274.1516"redis> GEODIST nearbyPeople user_1 user_2 km"166.2742"redis> GEODIST nearbyPeople user_1 user_2 mi"103.3182"
```

把 nearbyPeople 中的 距离经纬度(15,37)200km 以内的元素都找出来，而且带上距离：
```
redis>GEORADIUS nearbyPeople 15 37 200 km WITHDIST  
1) 1) "user_1"  
   2) "190.4424"  
2) 1) "user_2"  
   2) "56.4413"
```





## Stream（5.0 版新增）

消息队列，解决了基于 List 类型实现的消息队列中存在的两个问题。
可以自动生成全局唯一消息ID，并支持以消费组形式消费数据。






<!-- @include: @article-footer.snippet.md -->     

