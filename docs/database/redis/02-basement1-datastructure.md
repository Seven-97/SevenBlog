---
title: 底层 - 数据结构
category: 数据库
tag:
 - Redis
---



## Redis数据库的数据结构

Redis 的键值对中的 **key 就是字符串对象**，而 **value 就是指Redis的数据类型**，可以是String，也可以是List、Hash、Set、 Zset 的数据类型。

其实是Redis 底层使用了一个`全局哈希表`保存所有键值对，哈希表的最大好处就是 O(1) 的时间复杂度快速查找到键值对。哈希表其实就是一个数组，数组中的元素叫做哈希桶。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270802767.png)

- redisDb 结构，表示 Redis 数据库的结构，结构体里存放了指向了 dict 结构的指针；//默认有16个数据库
- dict 结构，结构体里存放了 2 个哈希表，正常情况下都是用`哈希表1`，`哈希表2`只有在 rehash 的时候才用；
- ditctht 结构，表示哈希表的结构，结构里存放了哈希表数组，数组中的每个元素都是指向一个哈希表节点结构（dictEntry）的指针；
- dictEntry 结构，表示哈希表节点的结构，结构里存放了 `void* key` 和 `void* value` 指针， key 指向的是 String 对象，而 value 就是指Redis的几种数据类型。

```c
struct redisServer {
   //...
    redisDb *db;
	//...
	int dbnum; //默认16个
}

typedef struct redisDb {
    dict *dict;     //全局hash表
    //...
} redisDb;

struct dict {
   //...
    dictEntry **ht_table[2]; //两个dictEntry，一个开始为空，rehash迁移时使用
    //...
	long rehashidx; /* rehashing not in progress if rehashidx == -1 */
};

struct dictEntry {//具体的对象
    void *key; //key
    union {
        void *val;
        uint64_t u64;
        int64_t s64;
        double d;
    } v;//value
    struct dictEntry *next;    //下一个节点的指针
    void *metadata[];
};
```

void * key 和 void * value 指针指向的就是 Redis 对象。Redis中有全局hash表，key是String,value是不同类型的对象，如果是Java，那可以直接用Map\<String,Object>来通用表示。而Redis直接由C语言实现，因此具体的每个对象都由 redisObject 结构表示，用type来表示具体类型，如下：
```c
struct redisObject {
    unsigned type:4;
    unsigned encoding:4;
   //……
    void *ptr;
};
```

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270802790.png)

- type，标识该对象是什么类型的对象（String 对象、 List 对象、Hash 对象、Set 对象和 Zset 对象）；
- encoding，标识该对象使用了哪种底层的数据结构；
- ptr，指向底层数据结构的指针。

如图，Redis 数据类型（也叫 Redis 对象）和底层数据结构的对应关图：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270802383.png)

- 默认情况下hash使用listpack存储，当保存的字段-值的数量大于512个或者当个字段的值大于64个字节时，改为hashtable。
- 默认情况下zSet使用listpack做为存储结构，当集合中的元素大于等于128个或是单个值的字节数大于等于64，存储结构会修改为skiplist。

这几个值都是可以修改的，没必要记；在redis.conf里
```c
hash-max-listpack-entries 512
hash-max-listpack-value 64

zset-max-listpack-entries 128
zset-max-listpack-value 64
```

## SDS
Simple Dynamic String，简单动态字符串

### C语言中的缺陷

**获取字符串长度复杂度为O（n）**  

在 C 语言里，字符数组的结尾位置用“\0”表示，意思是指字符串的结束。  
因此c语言获取字符串长度的函数 strlen，就是遍历字符数组中的每一个字符，遇到字符为 “\0” 后，就会停止遍历，然后返回已经统计到的字符个数，即为字符串长度，因此复杂度为O（n）

**字符串只能保存文本数据**  

字符数组的结尾位置用“\0”表示  
因此，除了字符串的末尾之外，字符串里面不能含有 “\0” 字符，否则最先被程序读入的 “\0” 字符将被误认为是字符串结尾，这个限制使得 C 语言的字符串只能保存文本数据，不能保存像图片、音频、视频文化这样的二进制数据

**有可能发生缓冲区溢出**

C 语言的字符串是不会记录自身的缓冲区大小的，所以 strcat 函数假定程序员在执行这个函数时，已经为 dest 分配了足够多的内存，可以容纳 src 字符串中的所有内容，而一旦这个假定不成立，就会发生缓冲区溢出将可能会造成程序运行终止。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270802885.png)


### SDS结构
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270802017.png)

- len，记录了字符串长度。这样获取字符串长度的时候，只需要返回这个成员变量值就行，时间复杂度只需要 O（1）。
- alloc，分配给字符数组的空间长度。这样在修改字符串的时候，可以通过 alloc - len 计算出剩余的空间大小，可以用来判断空间是否满足修改需求，如果不满足的话，就会自动将 SDS 的空间**扩容**至执行修改所需的大小，然后才执行实际的修改操作。这样就不会发生缓冲区溢出
- flags，用来表示不同类型的 SDS。一共设计了 5 种类型，分别是 sdshdr5、sdshdr8、sdshdr16、sdshdr32 和 sdshdr64。
- buf[]，字符数组，用来保存实际数据。不仅可以保存字符串，也可以保存二进制数据。

SDS 的 API 使用二进制的方式来处理 SDS 存放在 buf[] 里的数据，使得 Redis 不仅可以保存文本数据，也可以保存任意格式的二进制数据。

SDS的动态其实指的就是动态扩容
```c
hisds hi_sdsMakeRoomFor(hisds s, size_t addlen)
{
    ... ...
    // s目前的剩余空间已足够，无需扩展，直接返回
    if (avail >= addlen)
        return s;
    //获取目前s的长度
    len = hi_sdslen(s);
    sh = (char *)s - hi_sdsHdrSize(oldtype);
    //扩展之后 s 至少需要的长度
    newlen = (len + addlen);
    //根据新长度，为s分配新空间所需要的大小
    if (newlen < HI_SDS_MAX_PREALLOC)
        //新长度<HI_SDS_MAX_PREALLOC 则分配所需空间*2的空间
		//默认定义HI_SDS_MAX_PREALLOC为(1024*1024)即1M
        newlen *= 2;
    else
        //否则，分配长度为目前长度+1MB
        newlen += HI_SDS_MAX_PREALLOC;
       ...
}

// #define HI_SDS_MAX_PREALLOC (1024*1024)
```
- 如果所需的 sds 长度小于 1 MB，那么最后的扩容是按照翻倍扩容来执行的，即 2 倍的newlen
- 如果所需的 sds 长度超过 1 MB，那么最后的扩容长度应该是 newlen + 1MB。

## 双向链表

Redis中的链表是双向链表结构
```c
typedef struct listNode {
    //前置节点
    struct listNode *prev;
    //后置节点
    struct listNode *next;
    //节点的值
    void *value;
} listNode;
```

不过，Redis 在 listNode 结构体基础上又封装了 list 这个数据结构；类似于Java定义了Node节点，同时再此基础上封装了LinkedList
```c
typedef struct list {
	//链表头节点
    listNode *head;
    //链表尾节点
    listNode *tail;
    //节点值复制函数
    void *(*dup)(void *ptr);
    //节点值释放函数
    void (*free)(void *ptr);
    //节点值比较函数
    int (*match)(void *ptr, void *key);
    //链表节点数量
    unsigned long len;
} list;
```

Redis的链表的优缺点与链表的优缺点一致

## 压缩列表
压缩列表是 Redis 为了节约内存而开发的，它是由连续内存块组成的顺序型数据结构，类似于数组。
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270803874.png)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270803029.png)

压缩列表有四个重要字段：
- zlbytes，记录整个压缩列表占用对内存字节数；
- zltail，记录压缩列表「尾部」节点距离起始地址由多少字节，也就是列表尾的偏移量；
- zllen，记录压缩列表包含的节点数量；
- zlend，标记压缩列表的结束点，固定值 0xFF（十进制255）。

在压缩列表中，如果要查找定位第一个元素和最后一个元素，可以通过表头三个字段（zllen）的长度直接定位，复杂度是 O(1)。而查找其他元素时，就没有这么高效了，只能逐个查找，此时的复杂度就是 O(N) 了，因此压缩列表不适合保存过多的元素。

压缩列表节点（entry）的构成如下：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270803681.png)
entry中的字段：

- prevlen，记录了**前一个节点**的长度，目的是为了实现从后向前遍历；
- encoding，记录了当前节点实际数据的**类型和长度**，类型主要有两种：字符串和整数。
- data，记录了当前节点的实际数据，类型和长度都由 encoding 决定；

往压缩列表中插入数据时，压缩列表会根据数据类型是字符串还是整数，以及数据的大小，会使用不同空间大小的 prevlen 和 encoding 这两个元素里保存的信息，这种**根据数据大小和类型进行不同的空间大小分配的设计思想**，正是 Redis 为了节省内存而采用的。

压缩列表里的每个节点中的 prevlen 属性都记录了前一个节点的长度，而且 prevlen 属性的空间大小跟前一个节点长度值有关，比如：
- 如果前一个节点的长度小于 254 字节，那么 prevlen 属性需要用 1 字节的空间来保存这个长度值；
- 如果前一个节点的长度大于等于 254 字节，那么 prevlen 属性需要用 5 字节的空间来保存这个长度值；

### 连锁更新
压缩列表新增某个元素或修改某个元素时，如果空间不不够，压缩列表占用的内存空间就需要重新分配。由于prevlen是前一个节点的长度，当新插入的元素较大时，那么就可能会导致后续元素的 prevlen 占用空间都发生变化（比如说1字节编程5字节），如果当前节点的prevlen属性从1字节变成5字节后导致下一个节点的prevlen属性也变大，那可能就会而引起「连锁更新」问题，导致每个元素的空间都要重新分配，造成访问压缩列表性能的下降。
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270803165.png)

### 压缩列表的优缺点
优点：
- 根据**数据大小和类型进行不同的空间大小分配**来压缩内存空间
- 压缩列表是一种内存紧凑型的数据结构，占用一块连续的内存空间，不仅可以利用 CPU 缓存（链表本身内存地址是不连续的，所以不能利用CPU缓存），而且会针对不同长度的数据，进行相应编码，这种方法可以有效地节省内存开销。

缺点：
- 不能保存过多的元素，否则查询效率就会降低；
- 新增或修改某个元素时，压缩列表占用的内存空间需要重新分配，甚至可能引发连锁更新的问题。

**目前已被listpack替代**

## 哈希表
全局哈希表 和 底层哈希表的对象都是使用的这个结构

哈希表中的节点结构
```c
struct dictEntry {
    void *key;
    union {
        void *val;
        uint64_t u64;
        int64_t s64;
        double d;
    } v;
    struct dictEntry *next;     /* Next entry in the same hash bucket. */
    void *metadata[];          
};
```
dictEntry 结构里不仅包含指向键和值的指针，还包含了指向下一个哈希表节点的指针，这个指针可以将多个哈希值相同的键值对链接起来，以此来解决哈希冲突的问题，这就是链式哈希。

关于解决hash冲突问题可以看这篇文章：[解决哈希冲突的三种方法](https://www.seven97.top/blog/15 "解决哈希冲突的三种方法")

而redis是**先通过拉链法**解决，**再通过rehash**来解决hash冲突问题的，即再hash法

### rehash
Redis 定义一个 dict 结构体，这个结构体里定义了两个哈希表（ht_table[2]）。
```c
struct dict {
   //...
    dictEntry **ht_table[2]; //两个dictEntry，一个开始为空，rehash迁移时使用
    //...
    long rehashidx; /* rehashing not in progress if rehashidx == -1 */
};
```

在正常服务请求阶段，插入的数据，都会写入到`哈希表 1`，此时的`哈希表 2`  并没有被分配空间。随着数据逐步增多（根据负载因子判断），触发了 rehash 操作，这个过程分为如下三步：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270803033.png)

如果`哈希表 1`的数据量非常大，那么在迁移至`哈希表 2`的时候，因为会涉及大量的数据拷贝，此时可能会对 Redis 造成阻塞，无法服务其他请求。因此redis采用了渐进式rehash

渐进式 rehash 步骤如下：
1. 先给`哈希表 2`分配空间；
2. 在 rehash 进行期间，每次哈希表元素进行新增、删除、查找或者更新操作时，Redis 除了会执行对应的操作之外，还会顺序将`哈希表 1`中索引位置上的所有 key-value 迁移到`哈希表 2`上；
3. 随着处理客户端发起的哈希表操作请求数量越多，最终在某个时间点会把`哈希表 1`的所有 key-value 迁移到`哈希表 2`，从而完成 rehash 操作。

这样就把一次性大量数据迁移工作的开销，分摊到了多次处理请求的过程中，避免了一次性 rehash 的耗时操作。

在进行渐进式 rehash 的过程中，会有两个哈希表，所以在渐进式 rehash 进行期间，哈希表元素的删除、查找、更新等操作都会在这两个哈希表进行。比如，在渐进式 rehash 进行期间，查找一个 key 的值的话，先会在`哈希表 1`里面进行查找，如果没找到，就会继续到`哈希表 2` 里面进行找到。新增一个 key-value 时，会被保存到`哈希表 2`里面，而`哈希表 1`则不再进行任何添加操作，这样保证了`哈希表 1`的 key-value 数量只会减少，随着 rehash 操作的完成，最终`哈希表 1`就会变成空表。

哈希表的查找过程：
```c
dictEntry *dictFind(dict *d, const void *key)
{
    dictEntry *he;
    uint64_t h, idx, table;

    if (dictSize(d) == 0) return NULL; /* dict is empty */
    if (dictIsRehashing(d)) _dictRehashStep(d);//检查是否正在渐进式 rehash，如果是，那就rehash一步
    h = dictHashKey(d, key);//计算key的hash值
	//哈希表元素的删除、查找、更新等操作都会在两个哈希表进行
    for (table = 0; table <= 1; table++) {
        idx = h & DICTHT_SIZE_MASK(d->ht_size_exp[table]);
        he = d->ht_table[table][idx];
        while(he) {
            void *he_key = dictGetKey(he);
            if (key == he_key || dictCompareKeys(d, key, he_key))
                return he;
            he = dictGetNext(he);
        }
        if (!dictIsRehashing(d)) return NULL;
    }
    return NULL;
}
```

关键在于哈希表插入时会去检查是都正在Rehash，如果不是，那就往0号hash表中插入；如果是，那就直接往1号hash表中插入，因为如果正在Rehash还往0号hash表中插入，那么最终还是要rehash到1号hash表的
```c
int htidx = dictIsRehashing(d) ? 1 : 0;
```

### rehash的触发条件

负载因子 = 哈希表已保存节点数量/哈希表大小

触发 rehash 操作的条件，主要有两个：
- 当负载因子大于等于 1 ，并且 Redis 没有在执行 bgsave 命令或者 bgrewiteaof 命令，也就是没有执行 RDB 快照或没有进行 AOF 重写的时候，就会进行 rehash 操作。
- 当负载因子大于等于 5 时，此时说明哈希冲突非常严重了，不管有没有有在执行 RDB 快照或 AOF 重写，都会强制进行 rehash 操作

## 整数集合
当一个 Set 对象只包含整数值元素，并且元素数量不大时，就会使用整数集这个数据结构作为底层实现。

```c
typedef struct intset {
    uint32_t encoding;
   //集合包含的元素数量
    uint32_t length;
    //保存元素的数组
    int8_t contents[];
} intset;
```

保存元素的容器是一个 contents 数组，虽然 contents 被声明为 int8_t 类型的数组，但是实际上 contents 数组并不保存任何 int8_t 类型的元素，contents 数组的真正类型取决于 intset 结构体里的 encoding 属性的值。比如：
- 如果 encoding 属性值为 INTSET_ENC_INT16，那么 contents 就是一个 int16_t 类型的数组，数组中每一个元素的类型都是 int16_t；
- 如果 encoding 属性值为 INTSET_ENC_INT32，那么 contents 就是一个 int32_t 类型的数组，数组中每一个元素的类型都是 int32_t；
- 如果 encoding 属性值为 INTSET_ENC_INT64，那么 contents 就是一个 int64_t 类型的数组，数组中每一个元素的类型都是 int64_t；

### 整数集合升级
将一个新元素加入到整数集合里面，如果新元素的类型（int32_t）比整数集合现有所有元素的类型（int16_t）都要长时，整数集合需要先进行升级，也就是按新元素的类型（int32_t）扩展 contents 数组的空间大小，然后才能将新元素加入到整数集合里，当然升级的过程中，也要维持整数集合的有序性。

整数集合升级的好处：    
如果要让一个数组同时保存 int16_t、int32_t、int64_t 类型的元素，最简单做法就是直接使用 int64_t 类型的数组。不过这样的话，当如果元素都是 int16_t 类型的，就会造成内存浪费。

使用整数集合主要思想就是为了节省内存开销。

## 跳表
跳表的优势是能支持平均 O(logN) 复杂度的节点查找。

跳表是在链表基础上改进过来的，实现了一种「多层」的有序链表，这样的好处是能快读定位数据。
```c
typedef struct zskiplist {
	//便于在O(1)时间复杂度内访问跳表的头节点和尾节点；
    struct zskiplistNode *header, *tail;
	//跳表的长度
    unsigned long length;
	//跳表的最大层数
    int level;
} zskiplist;

typedef struct zskiplistNode {
    //Zset 对象的元素值
    sds ele;
    //元素权重值
    double score;
    //后向指针，指向前一个节点，目的是为了方便从跳表的尾节点开始访问节点，方便倒序查找。
    struct zskiplistNode *backward;
  
    //节点的level数组，保存每层上的前向指针和跨度
    struct zskiplistLevel {
        struct zskiplistNode *forward;
        unsigned long span;
    } level[];
} zskiplistNode;
```

跳表结构如下：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270803939.png)


跳表的相邻两层的节点数量最理想的比例是 2:1，查找复杂度可以降低到 O(logN)。

### Redis中的跳表是两步两步跳的吗？
如果采用新增节点或者删除节点时，来调整跳表节点以维持比例2：1的方法的话，显然是会带来额外开销的。

跳表在创建节点时候，会生成范围为[0-1]的一个随机数，如果这个随机数小于 0.25（相当于概率 25%），那么层数就增加 1 层，然后继续生成下一个随机数，直到随机数的结果大于 0.25 结束，最终确定该节点的层数。因为随机数取值在[0,0.25）范围内概率不会超过25%，所以这也说明了增加一层的概率不会超过25%。这样的话，当插入一个新结点时，只需修改前后结点的指针，而其它结点的层数就不需要随之改变了，这样就降低插入操作的复杂度。
```c
// #define ZSKIPLIST_P 0.25
int zslRandomLevel(void) {
    static const int threshold = ZSKIPLIST_P*RAND_MAX;
    int level = 1; //初始化为一级索引
    while (random() < threshold)
        level += 1;//随机数小于 0.25就增加一层
	//如果level 没有超过最大层数就返回，否则就返回最大层数
    return (level<ZSKIPLIST_MAXLEVEL) ? level : ZSKIPLIST_MAXLEVEL;
}
```

### 为什么不用AVL树
- 在做范围查找的时候，跳表比AVL操作要简单。AVL在找到最小值后要中序遍历，跳表直接往后遍历就可
- 跳表实现上更简单。AVL的在插入和删除时可能会需要进行左旋右旋操作，带来额外的开销，而跳表的插入和删除只需要修改相邻节点的指针，操作更简单

## listpack
listpack，目的是替代压缩列表，它最大特点是 **listpack 中每个节点不再包含前一个节点的长度**了,解决压缩列表的连锁更新问题

listpack 采用了压缩列表的很多优秀的设计，比如还是用一块连续的内存空间来紧凑地保存数据，并且为了节省内存的开销，listpack 节点与压缩列表一样**采用不同的编码方式保存不同大小的数据**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270803916.png)

```c
typedef struct {
    /* When string is used, it is provided with the length (slen). */
    unsigned char *sval;
    uint32_t slen;
    /* When integer is used, 'sval' is NULL, and lval holds the value. */
    long long lval;
} listpackEntry;
```
- encoding，定义该元素的编码类型，会对不同长度的整数和字符串进行编码；
- data，实际存放的数据；
- len，encoding+data的总长度；

listpack 没有压缩列表中记录前一个节点长度的字段了，listpack 只记录当前节点的长度，向 listpack 加入一个新元素的时候，不会影响其他节点的长度字段的变化，从而避免了压缩列表的连锁更新问题。

## quicklist
目前版本的 quicklist 其实是 双向链表 + listpack 的组合，因为一个 quicklist 就是一个链表，而链表中的每个元素又是一个listpack。


```c
typedef struct quicklist {
	//quicklist的链表头
    quicklistNode *head;
	//quicklist的链表尾
    quicklistNode *tail;
	//所有listpacks中的总元素个数
    unsigned long count;        /* total count of all entries in all listpacks */
    //quicklistNodes的个数
	unsigned long len;          /* number of quicklistNodes */
    signed int fill : QL_FILL_BITS;       /* fill factor for individual nodes */
    unsigned int compress : QL_COMP_BITS; /* depth of end nodes not to compress;0=off */
    unsigned int bookmark_count: QL_BM_BITS;
    quicklistBookmark bookmarks[];
} quicklist;

typedef struct quicklistEntry {
    const quicklist *quicklist;
    quicklistNode *node;
	//指向listpack的指针
    unsigned char *zi;
    unsigned char *value;
    long long longval;
    size_t sz;
    int offset;
} quicklistEntry;

typedef struct quicklistNode {
	//前一个quicklistNode
    struct quicklistNode *prev;
	//下一个quicklistNode
    struct quicklistNode *next;
    unsigned char *entry;
	//listpack的的字节大小
    size_t sz;             /* entry size in bytes */
	//listpack列表的元素个数
    unsigned int count : 16;     /* count of items in listpack */
    unsigned int encoding : 2;   /* RAW==1 or LZF==2 */
    unsigned int container : 2;  /* PLAIN==1 or PACKED==2 */
    unsigned int recompress : 1; /* was this node previous compressed? */
    unsigned int attempted_compress : 1; /* node can't compress; too small */
    unsigned int dont_compress : 1; /* prevent compression of entry that will be used later */
    unsigned int extra : 9; /* more bits to steal for future usage */
} quicklistNode;
```

## BitMap
详情请看 [位图](https://www.seven97.top/cs-basics/data-structure/bitmap.html "位图")