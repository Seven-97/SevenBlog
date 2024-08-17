---
title: 调优 - Redis容量评估模型
category: 数据库
tag:
 - Redis
---



> 来源：[Redis 容量评估模型-腾讯云开发者社区-腾讯云 (tencent.com)](https://cloud.tencent.com/developer/article/1004898)，Seven对其进行了补充完善。



计算Redis容量，并不只是仅仅计算key占多少字节，value占多少字节，因为Redis为了维护自身的数据结构，也会占用部分内存，本文章简单介绍每种数据类型（`String`、`Hash`、`Set`、`ZSet`、`List`）占用内存量，供做Redis容量评估时使用。

在看这里之前，可以先看一下[底层 - 数据结构](https://www.seven97.top/database/redis/02-basement1-datastructure.html) 这篇文章

## jemalloc内存分配规则

jemalloc是一种通用的内存管理方法，着重于减少内存碎片和支持可伸缩的并发性，做redis容量评估前必须对jemalloc的内存分配规则有一定了解。

jemalloc基于申请内存的大小把内存分配分为三个等级：small，large，huge：

- Small Object 的size以8字节，16字节，32字节等分隔开，小于页大小；
- Large Object 的size以分页为单位，等差间隔排列，小于chunk的大小；
- Huge Object 的大小是chunk大小的整数倍。

对于64位系统，一般chunk大小为4M，页大小为4K，内存分配的具体规则如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405152041071.png)



jemalloc在分配内存块时会分配大于实际值的2^n的值，例如实际值时6字节，那么会分配8字节

| 数据类型    | 占用量                                                       |
| ----------- | ------------------------------------------------------------ |
| dicEntry    | 主要包括3个指针，key、value、哈希冲突时下个指针，耗费容量为8*3=24字节，jemalloc会分配32字节的内存块 |
| dict结构    | 88字节，jemalloc会分配 96 字节的内存块                       |
| redisObject | type(4bit)、encoding(4bit)、lru(24bit)、int(8byte)、ptr指针(8byte)。因此redisObject结构占用（4+4+24)/8 +4+8 = 16字节。 |
| key_SDS     | key的长度 + 9，jemalloc分配 >= 该值的2^n的值                 |
| val_SDS     | value的长度 + 9，jemalloc分配 >= 该值的2^n的值               |
| key的个数   | 所有的key的个数                                              |
| bucket个数  | 大于key的个数的2^n次方，例如key个数是2000，那么bucket=2048   |
| 指针大小    | 8 byte                                                       |



> SDS中的主要包括两个表示长度int占用大小为8字节，redis中字符串还用“/0”表示结束占用1字节，所以   sds占用大小为9字节 + 数据长度



> dict结构 这里会分配96 字节的内存块？为什么不是128？



## 内存划分

Redis内存占用主要可以划分为如下几个部分：

- **数据**：Redis数据占用内存`dataset.bytes`包括key-value占用内存、`dicEntry`占用内存、`SDS`占用内存等。

  > 数据所占内存 = 当前所占总内存`total.allocated` - 额外内存`overhead.total`

- **初始化内存**：redis启动初始化时使用的内存`startup.allocated`，属于额外内存`overhead.total`的一部分。

- **主从复制内存**：用于主从复制，属于额外内存一部分。

- **缓冲区内存**：缓冲内存包括客户端缓冲区、复制积压缓冲区、AOF缓冲区等；其中，客户端缓冲存储客户端连接的输入输出缓冲；复制积压缓冲用于部分复制功能；AOF缓冲区用于在进行AOF重写时，保存最近的写入命令。在了解相应功能之前，不需要知道这些缓冲的细节；这部分内存由jemalloc分配，因此会统计在used_memory中。

- **内存碎片**：内存碎片是Redis在分配、回收物理内存过程中产生的。例如，如果对数据的更改频繁，而且数据之间的大小相差很大，可能导致redis释放的空间在物理内存中并没有释放，但redis又无法有效利用，这就形成了内存碎片。

  > 内存碎片率 = Redis进程占用内存 / 当前所占内存`total.allocated`
  >
  > 
  >
  > 内存碎片涉及到内存碎片率`fragmentation`，该值对于查看内存是否够用比较重要：
  >
  > - 该值一般>1，数值越大，说明内存碎片越多。
  > - 如果<1，说明Redis占用了虚拟内存，而虚拟内存是基于磁盘的，速度会变慢，所以如果<1，就需要特别注意是否是内存不足了。
  > - 一般来说，mem_fragmentation_ratio在1.03左右是比较健康的状态（对于jemalloc来说）；



## redis数据内存容量评估

redis容量评估模型根据key类型而有所不同。

### string

一个简单的set命令最终会产生4个消耗内存的结构，中间free掉的不考虑：

- 1个dictEntry结构，24字节，负责保存具体的键值对；
- 1个redisObject结构，16字节，用作val对象；
- 1个SDS结构，（key长度 + 9）字节，用作key字符串；
- 1个SDS结构，（val长度 + 9）字节，用作val字符串；

当key个数逐渐增多，redis还会以rehash的方式扩展哈希表节点数组，即增大哈希表的bucket个数，每个bucket元素都是个指针（`dictEntry*`），占8字节，bucket个数是超过key个数向上求整的2的n次方。



#### 评估模型

真实情况下，每个结构最终真正占用的内存还要考虑jemalloc的内存分配规则，综上所述，string类型的容量评估模型为：

**总内存消耗 = （dictEntry大小 + redisObject大小 +`key_SDS`大小 +** **`val_SDS`大小）×key个数 + bucket个数 ×指针大小**



即：

**总内存消耗 = （32 + 16 + `key_SDS`大小 + `val_SDS`大小）×key个数 + bucket个数 × 8**

> 32是因为是24，但jemalloc会分配32字节的内存块



#### 测试验证

string类型容量评估测试脚本如下：

```javascript
#!/bin/sh

old_memory=`./redis-cli -h 0 -p 10009 info|grep used_memory:|awk -F: '{printf "%d", $2}'`
echo "before test, memory used: $old_memory"

for((i=1000; i<3000; i++))
do
    ./redis-cli -h 0 -p 10009 set test_key_$i test_value_$i > /dev/null
    sleep 0.2
done

new_memory=`./redis-cli -h 0 -p 10009 info|grep used_memory:|awk -F: '{printf "%d", $2}'`
echo "after test, memory used: $new_memory"

let difference=new_memory-old_memory
echo "difference is: $difference"
```

测试用例中，key长度为 13，value长度为15，key个数为2000，根据上面总结的容量评估模型，容量预估值为2000 ×（32 + 16 + 32 + 32） + 2048× 8 = 240384

运行测试脚本，得到结果如下：

![img](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405152047936.png)

结果都是240384，说明模型预估的十分精确。

### hash

哈希对象的底层实现数据结构可能是listpack或者hashtable，当同时满足下面这两个条件时，哈希对象使用listpack这种结构（此处列出的条件都是redis默认配置，可以更改）：

- 哈希对象保存的所有键值对的键和值的字符串长度都小于64字节；
- 哈希对象保存的键值对的数量都小于512个；

可以看出，业务侧真实使用场景基本都不能满足这两个条件，所以哈希类型大部分都是hashtable结构，因此本篇文章只讲hashtable。

与string类型不同的是，hash类型的值对象并不是指向一个SDS结构，而是指向又一个dict结构，dict结构保存了哈希对象具体的键值对，hash类型结构关系如图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405152047981.png)

一个hmset命令最终会产生以下几个消耗内存的结构：

- 1个dictEntry结构，24字节，负责保存当前的哈希对象；
- 1个SDS结构，（key长度 + 9）字节，用作key字符串；
- 1个redisObject结构，16字节，指向当前key下属的dict结构；
- 1个dict结构，88字节，负责保存哈希对象的键值对；
- n个dictEntry结构，24×n 字节，负责保存具体的field和value，n等于field个数；
- n个redisObject结构，16×n 字节，用作field对象；
- n个redisObject结构，16×n 字节，用作value对象；
- n个SDS结构，（field长度 + 9）× n字节，用作field字符串；
- n个SDS结构，（value长度 + 9）× n字节，用作value字符串；



#### 评估模型

因为hash类型内部有两个dict结构，所以最终会有产生两种rehash，一种rehash基准是field个数，另一种rehash基准是key个数，结合jemalloc内存分配规则，hash类型的容量评估模型为：

**总内存消耗 = [（redisObject大小 ×2 +`field_SDS`大小 +** **`val_SDS`大小 + dictEntry大小）× field个数 +** **`field_bucket`个数× 指针大小 + dict大小 + redisObject大小 +`key_SDS`大小 + dictEntry大小 ] × key个数 +** **`key_bucket`个数 × 指针大小**



即：

**总内存消耗 = [（16 ×2 +`field_SDS`大小 + `val_SDS`大小 + 32）× field个数 + `field_bucket`个数× 8 + 96 + 16 +`key_SDS`大小 + 32 ] × key个数 + `key_bucket`个数 × 8**



#### 测试验证

hash类型容量评估测试脚本如下：

```javascript
#!/bin/sh

value_prefix="test_value_123456789012345678901234567890123456789012345678901234567890_"

old_memory=`./redis-cli -h 0 -p 10009 info|grep used_memory:|awk -F: '{printf "%d", $2}'`
echo "before test, memory used: $old_memory"

for((i=100; i<300; i++))
do
    for((j=100; j<300; j++))
    do
        ./redis-cli -h 0 -p 10009 hset test_key_$i test_field_$j $value_prefix$j > /dev/null
    done
    sleep 0.5
done

new_memory=`./redis-cli -h 0 -p 10009 info|grep used_memory:|awk -F: '{printf "%d", $2}'`
echo "after test, memory used: $new_memory"

let difference=new_memory-old_memory
echo "difference is: $difference"
```

测试用例中，key长度为 12，field长度为14，value长度为75，key个数为200，field个数为200，根据上面总结的容量评估模型，容量预估值为[（16 + 16 + 32 + 96 + 32）×200 + 256×8 + 96 + 16 + 32 + 32 ]× 200 + 256× 8 = 8126848

运行测试脚本，得到结果如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405152047952.png)

结果相差40，说明模型预测比较准确。

### zset

同哈希对象类似，有序集合对象的底层实现数据结构也分两种：listpack或者skiplist，当同时满足下面这两个条件时，有序集合对象使用ziplist这种结构（此处列出的条件都是redis默认配置，可以更改）：

- 有序集合对象保存的元素数量小于128个；
- 有序集合保存的所有元素成员的长度都小于64字节；

业务侧真实使用时基本都不能同时满足这两个条件，因此这里只讲skiplist结构的情况。skiplist类型的值对象指向一个zset结构，zset结构同时包含一个字典和一个跳跃表，占用的总字节数为16，具体定义如下（`redis.h/zset`）：

```javascript
typedef struct zset {
    dict *dict;
    zskiplist *zsl;
} zset;
```

跳跃表按分值从小到大保存了所有集合元素，每个跳跃表节点都保存了一个集合元素，dict字典为有序集合创建了一个从成员到分值的映射，字典中的每个键值对都保存了一个集合元素，这两种数据结构会通过指针来共享相同元素的成员和分值，没有浪费额外的内存。zset类型的结构关系如图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405152047942.png)

一个zadd命令最终会产生以下几个消耗内存的结构：

- 1个dictEntry结构，24字节，负责保存当前的有序集合对象；
- 1个SDS结构，（key长度 + 9）字节，用作key字符串；
- 1个redisObject结构，16字节，指向当前key下属的zset结构；
- 1个zset结构，16字节，负责保存下属的dict和zskiplist结构；
- 1个dict结构，88字节，负责保存集合元素中成员到分值的映射；
- n个dictEntry结构，24×n字节，负责保存具体的成员和分值，n等于集合成员个数；
- 1个zskiplist结构，32字节，负责保存跳跃表的相关信息；
- 1个32层的zskiplistNode结构，24+16×32=536字节，用作跳跃表头结点；
- n个zskiplistNode结构，（24+16×m）×n字节，用作跳跃表节点，m等于节点层数；
- n个redisObject结构，16×n字节，用作集合中的成员对象；
- n个SDS结构，（value长度 + 9）×n字节，用作成员字符串；

因为每个zskiplistNode节点的层数都是根据幂次定律随机生成的，而容量评估需要确切值，因此这里采用概率中的期望值来代替单个节点的大小，结合jemalloc内存分配规则，经计算，单个zskiplistNode节点大小的期望值为53.336。

#### 评估模型

zset类型内部同样包含两个dict结构，所以最终会有产生两种rehash，一种rehash基准是成员个数，另一种rehash基准是key个数，zset类型的容量评估模型为：

**总内存消耗 = [（`val_SDS`大小 + redisObject大小 + zskiplistNode大小 + dictEntry大小）×value个数 +`value_bucket`个数 ×指针大小 + 32层zskiplistNode大小 + zskiplist大小 + dict大小 + zset大小 + redisObject大小 +** **`key_SDS`大小 + dictEntry大小 ] ×key个数 +`key_bucket`个数 × 指针大小**



即：

**总内存消耗 = [（`val_SDS`大小 + 16 + 53.336 + 32）×value个数 +`value_bucket`个数 × 8 + 640 +32 + 96 + 16 + 16 +** **`key_SDS`大小 + 32 ] ×key个数 +`key_bucket`个数 × 8**



#### 测试验证

zset类型容量评估测试脚本如下：

```javascript
#!/bin/sh

value_prefix="test_value_123456789012345678901234567890123456789012345678901234567890_"

old_memory=`./redis-cli -h 0 -p 10009 info|grep used_memory:|awk -F: '{printf "%d", $2}'`
echo "before test, memory used: $old_memory"

for((i=100; i<300; i++))
do
    for((j=100; j<300; j++))
    do
        ./redis-cli -h 0 -p 10009 zadd test_key_$i $j $value_prefix$j > /dev/null
    done
    sleep 0.5
done

new_memory=`./redis-cli -h 0 -p 10009 info|grep used_memory:|awk -F: '{printf "%d", $2}'`
echo "after test, memory used: $new_memory"

let difference=new_memory-old_memory
echo "difference is: $difference"
```

测试用例中，key长度为 12，value长度为75，key个数为200，value个数为200，根据上面总结的容量评估模型，容量预估值为[（96 + 16 + 53.336 + 32）×200 + 256×8 + 640 + 32 + 96 + 16 + 16 + 32 + 32 ] ×200 + 256 × 8 = 8477888



运行测试脚本，得到结果如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405152047969.png)

结果相差672，说明模型预测比较准确。

### list

列表对象的底层实现数据结构同样分两种：listpack或者linkedlist，当同时满足下面这两个条件时，列表对象使用listpack这种结构（此处列出的条件都是redis默认配置，可以更改）：

- 列表对象保存的所有字符串元素的长度都小于64字节；
- 列表对象保存的元素数量小于512个；

因为实际使用情况，这里同样只讲linkedlist结构。linkedlist类型的值对象指向一个list结构，具体结构关系如图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405152047964.png)

一个rpush或者lpush命令最终会产生以下几个消耗内存的结构：

- 1个dictEntry结构，24字节，负责保存当前的列表对象；
- 1个SDS结构，（key长度 + 9）字节，用作key字符串；
- 1个redisObject结构，16字节，指向当前key下属的list结构；
- 1个list结构，48字节，负责管理链表节点；
- n个listNode结构，24×n字节，n等于value个数；
- n个redisObject结构，16×n字节，用作链表中的值对象；
- n个SDS结构，（value长度 + 9）×n字节，用作值对象指向的字符串；

#### 评估模型

list类型内部只有一个dict结构，rehash基准为key个数，综上，list类型的容量评估模型为：

**总内存消耗 = [（`val_SDS`大小 + redisObject大小 + listNode大小）× value个数 + list大小 + redisObject大小 +** **`key_SDS`大小 + dictEntry大小 ] × key个数 +** **`key_bucket`个数 × 指针大小**



即：

**总内存消耗 = [（`val_SDS`大小 +16 + 32）× value个数 + 16 + 32 +** **`key_SDS`大小 + 32 ] × key个数 +** **`key_bucket`个数 × 8**



#### 测试验证

list类型容量评估测试脚本如下：

```javascript
#!/bin/sh

value_prefix="test_value_123456789012345678901234567890123456789012345678901234567890_"

old_memory=`./redis-cli -h 0 -p 10009 info|grep used_memory:|awk -F: '{printf "%d", $2}'`
echo "before test, memory used: $old_memory"

for((i=100; i<300; i++))
do
    for((j=100; j<300; j++))
    do
        ./redis-cli -h 0 -p 10009 rpush test_key_$i $value_prefix$j > /dev/null
    done
    sleep 0.5
done

new_memory=`./redis-cli -h 0 -p 10009 info|grep used_memory:|awk -F: '{printf "%d", $2}'`
echo "after test, memory used: $new_memory"

let difference=new_memory-old_memory
echo "difference is: $difference"
```

测试用例中，key长度为 12，value长度为75，key个数为200，value个数为200，根据上面总结的容量评估模型，容量预估值为[（96 + 16 + 32） ×200 + 48 + 16 + 32 + 32 ] × 200 + 256 ×8 = 5787648



运行测试脚本，得到结果如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405152047356.png)

结果都是5787648，说明模型预估的十分精确。



### Set

一个sadd命令最终会产生以下几个消耗内存的结构：

- 1个dictEntry结构，24字节，负责保存当前的set对象；
- 1个SDS结构，（key长度 + 9）字节，用作key字符串；
- 1个redisObject结构，16字节，指向当前key下属的dict结构；
- 1个dict结构，88字节，负责保存哈希对象的键值对；
- n个dictEntry结构，24×n 字节，负责保存具体的member，n等于member个数；
- n个redisObject结构，16×n 字节，用作member对象；
- n个SDS结构，（field长度 + 9）× n字节，用作member字符串；

#### 评估模型

set与hash类似，只是value部分没有具体的值。与hash类型一样，内部有两个dict结构，所以最终会有产生两种rehash，一种rehash基准是member个数，另一种rehash基准是key个数，结合jemalloc内存分配规则，hash类型的容量评估模型为：

**总内存消耗 = [（redisObject大小 +`member_SDS`大小  + dictEntry大小）× member个数 + `member_bucket`个数× 指针大小 + dict大小 + redisObject大小 +`key_SDS`大小 + dictEntry大小 ] × key个数 + `key_bucket`个数×指针大小**



即：

**总内存消耗 = [（16  +`member_SDS`大小 + 32）× member个数 + `member_bucket`个数× 8 + 96 + 16 +`key_SDS`大小 + 32 ] × key个数 + `key_bucket`个数×8**



#### 测试验证

待更新……

<!-- @include: @article-footer.snippet.md -->     