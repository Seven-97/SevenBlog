---
title: InnoDB存储结构全解析：行页区段与单表2000W行的关系
category: 数据库
tags:
  - Mysql
head:
  - - meta
    - name: keywords
      content: mysql,mysql数据库,InnoDB的存储结构,单表不要超过2000W行,逻辑存储结构
  - - meta
    - name: description
      content: 全网最全的Mysql知识点总结，让天下没有难学的八股文！
---



## 逻辑存储结构
表空间由段（segment）、区（extent）、页（page）、行（row）组成，InnoDB存储引擎的逻辑存储结构大致如下图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261830288.png)


### 行（row）
数据库表中的记录都是按行（row）进行存放的，每行记录根据不同的行格式，有不同的存储结构。

### 页（page）
记录是按照行来存储的，但是数据库的读取并不以「行」为单位，否则一次读取（也就是一次 I/O 操作）只能处理一行数据，效率会非常低。

因此，InnoDB 的数据是按「页」为单位来读写的，也就是说，当需要读一条记录的时候，并不是将这个行记录从磁盘读出来，而是以页为单位，将其整体读入内存。

数据库的 I/O 操作的最小单位是页，InnoDB 数据页的默认大小是 16KB，意味着数据库每次读写都是以 16KB 为单位的，一次最少从磁盘中读取 16K 的内容到内存中，一次最少把内存中的 16K 内容刷新到磁盘中。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261830259.png)

页的类型有很多，常见的有数据页、undo 日志页、溢出页等等。数据表中的行记录是用「数据页」来管理的

在 File Header 中有两个指针，分别指向上一个数据页和下一个数据页，连接起来的页相当于一个双向的链表，如下图所示：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404261830220.png)

数据页中的记录按照「主键」顺序组成单向链表，单向链表的特点就是插入、删除非常方便，但是检索效率不高，最差的情况下需要遍历链表上的所有节点才能完成检索。

因此，数据页中有一个页目录，起到记录的索引作用，就像我们书那样，针对书中内容的每个章节设立了一个目录，想看某个章节的时候，可以查看目录，快速找到对应的章节的页数，而数据页中的页目录就是为了能快速找到记录。

### 区（extent）
我们知道 InnoDB 存储引擎是用 B+ 树来组织数据的。

B+ 树中每一层都是通过双向链表连接起来的，如果是以页为单位来分配存储空间，那么链表中相邻的两个页之间的物理位置并不是连续的，可能离得非常远，那么磁盘查询时就会有大量的随机I/O，随机 I/O 是非常慢的。

解决这个问题也很简单，就是让链表中相邻的页的物理位置也相邻，这样就可以使用顺序 I/O 了，那么在范围查询（扫描叶子节点）的时候性能就会很高。

那具体怎么解决呢？

在表中数据量大的时候，为某个索引分配空间的时候就不再按照页为单位分配了，而是按照区（extent）为单位分配。每个区的大小为 1MB，对于 16KB 的页来说，连续的 64 个页会被划为一个区，这样就使得链表中相邻的页的物理位置也相邻，就能使用顺序 I/O 了。

### 段（segment）
表空间是由各个段（segment）组成的，段是由多个区（extent）组成的。段一般分为数据段、索引段和回滚段等。

- 索引段：存放 B + 树的非叶子节点的区的集合；
- 数据段：存放 B + 树的叶子节点的区的集合；
- 回滚段：存放的是回滚数据的区的集合，之前讲事务隔离 (opens new window)的时候就介绍到了 MVCC 利用了回滚段实现了多版本查询数据。


## 为什么说MySQL 一般单表不要超过 2000W 行

总的来说，是因为超过2000W行后，B+树的层级会变高，导致IO次数增多

Mysql是根据数据页存储的，一个数据页默认是16KB。  

当要查询一条记录时，InnoDB 是会把整个页的数据加载到 Buffer Pool 中，因为，通过索引只能定位到磁盘中的页，而不能定位到页中的一条记录。将页加载到 Buffer Pool 后，再通过页里的页目录去定位到某条具体的记录。

在B+树中非叶子节点是不存储数据的，只存储下一级的索引，所以在同样一个 16K 的页，非叶子节点里的每条数据都指向新的页，而新的页有两种可能
- 如果是叶子节点，那么里面就是一行行的数据
- 如果是非叶子节点的话，那么就会继续指向新的页

假设：  
- 非叶子节点内指向其他页的数量为 x
- 叶子节点内能容纳的数据行数为 y
- B+ 数的层数为 z

所以B+树中存储数据的总数 Total =$x^{z-1} *y$，也就是说总数会等于 x 的 z-1 次方 与 Y 的乘积。

> x = ?

而页的中包含File Header (38 byte)、Page Header (56 Byte)、Infimum + Supermum（26 byte）、File Trailer（8byte）, 再加上页目录，大概 1k 左右，假设这些信息就是 1K, 那整个页的大小是 16K, 剩下 15k 用于存数据，在索引页中主要记录的是主键与页号，主键假设是 Bigint (8 byte), 而页号也是固定的（4Byte）, 那么索引页中的一条数据也就是 12byte。  

那么一页中就能存储  x= $15*1024/12$≈1280 行数据

> y = ?

叶子节点和非叶子节点的结构是一样的，同理，能放数据的空间也是 15k。

假设一条行数据 1k 来算，那一页就能存下 15 条，Y = $15*1024/1000$ ≈15。

Total =$x^{z-1} *y$，已知 x=1280，y=15：

- 假设 B+ 树是两层，那就是 z = 2， Total = $1280 ^1 *15$ = 19200
- 假设 B+ 树是三层，那就是 z = 3， Total = $1280 ^2 *15$ = 24576000 （约 2.45kw）

一般来说B+树最多为三层，因此数据2000W条是一个建议值

**但是**  

比如实际当行的数据占用空间不是 1K , 而是 5K, 那么单个数据页最多只能放下 3 条数据。  
还是按照 z = 3 的值来计算，那 Total = $1280 ^2*3$ = 4915200 （近 500w）  
那么建议值就是不超过500w

**总结**：  
- MySQL 的表数据是以页的形式存放的，页在磁盘中不一定是连续的。
- 页的空间是 16K, 并不是所有的空间都是用来存放数据的，会有一些固定的信息，如，页头，页尾，页码，校验码等等。
- 在 B+ 树中，叶子节点和非叶子节点的数据结构是一样的，区别在于，叶子节点存放的是实际的行数据，而非叶子节点存放的是主键和页号。
- 索引结构不会影响单表最大行数，2000W 只是推荐值，**超过了这个值可能会导致 B + 树层级更高，影响查询性能**。

<!-- @include: @article-footer.snippet.md -->     
