---
title: 调优 - 内存碎片问题
category: 数据库
tag:
 - Redis
 - 缓存
head:
  - - meta
    - name: keywords
      content: redis,redis内存碎片
  - - meta
    - name: description
      content: 全网最全的Redis知识点总结，让天下没有难学的八股文！
---



## 什么是内存碎片

可以将内存碎片简单地理解为那些不可用的空闲内存。

当数据删除后，Redis 释放的内存空间会由内存分配器管理，并不会立即返回给操作系统。所以，操作系统仍然会记录着给 Redis 分配了大量内存。而Redis 释放的内存空间可能并不是连续的，那么，这些不连续的内存空间很有可能处于一种闲置的状态。也就产生了内存碎片

举个例子：操作系统为你分配了 32 字节的连续内存空间，而你存储数据实际只需要使用 24 字节内存空间，那这多余出来的 8 字节内存空间如果后续没办法再被分配存储其他数据的话，就可以被称为内存碎片。

Redis 内存碎片虽然不会影响 Redis 性能，但是会增加内存消耗。



## 怎么产生的

Redis 内存碎片产生比较常见的 2 个原因

### 内因

**Redis 存储数据的时候向操作系统申请的内存空间可能会大于数据实际需要的存储空间。**

以下是这段 Redis 官方的原话：

To store user keys, Redis allocates at most as much memory as the maxmemory setting enables (however there are small extra allocations possible).

Redis 使用 zmalloc 方法(Redis 自己实现的内存分配方法)进行内存分配的时候，除了要分配 size 大小的内存之外，还会多分配 PREFIX_SIZE 大小的内存。

zmalloc 方法源码如下（源码地址：https://github.com/antirez/redis-tools/blob/master/zmalloc.c）：

```c++
void *zmalloc(size_t size) {
   // 分配指定大小的内存
   void *ptr = malloc(size+PREFIX_SIZE);
   if (!ptr) zmalloc_oom_handler(size);
#ifdef HAVE_MALLOC_SIZE
   update_zmalloc_stat_alloc(zmalloc_size(ptr));
   return ptr;
#else
   *((size_t*)ptr) = size;
   update_zmalloc_stat_alloc(size+PREFIX_SIZE);
   return (char*)ptr+PREFIX_SIZE;
#endif
}
```



另外，Redis 可以使用多种内存分配器来分配内存（ libc、jemalloc、tcmalloc），默认使用 **jemalloc[1]**，而 jemalloc 按照一系列固定的大小（8 字节、16 字节、32 字节……）来分配内存的。jemalloc 划分的内存单元如下图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270811288.png)

当程序申请的内存最接近某个固定值时，jemalloc 会给它分配相应大小的空间，就比如说程序需要申请 17 字节的内存，jemalloc 会直接给它分配 32 字节的内存，这样会导致有 15 字节内存的浪费。不过，jemalloc 专门针对内存碎片问题做了优化，一般不会存在过度碎片化的问题。



### 外因

**频繁修改 Redis 中的数据也会产生内存碎片。**



当 Redis 中的某个数据删除时，Redis 通常不会轻易释放内存给操作系统。

这个在 Redis 官方文档中也有对应的原话:

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270811464.png)

文档地址：https://redis.io/topics/memory-optimization 。



也就是说，键值对会被修改和删除，这会导致空间的扩容和释放。

- 如果修改后的键值对变大或变小了，就需要占用额外的空间或者释放不用的空间。
- 删除的键值对就不再需要内存空间了，此时，就会把空间释放出来，形成空闲空间。



## 如何查看 Redis 内存碎片？

使用 info memory 命令即可查看 Redis 内存相关的信息。下图中每个参数具体的含义，Redis 官方文档有详细的介绍：https://redis.io/commands/INFO 。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270811308.png)

Redis 内存碎片率的计算公式：mem_fragmentation_ratio （内存碎片率）= used_memory_rss (操作系统实际分配给 Redis 的物理内存空间大小)/ used_memory(Redis 内存分配器为了存储数据实际申请使用的内存空间大小)

也就是说，mem_fragmentation_ratio （内存碎片率）的值越大代表内存碎片率越严重。

一定不要误认为used_memory_rss 减去 used_memory值就是内存碎片的大小！！！这不仅包括内存碎片，还包括其他进程开销，以及共享库、堆栈等的开销。

 

那么，多大的内存碎片率才是需要清理呢？通常情况下，我们认为 mem_fragmentation_ratio > 1.5 的话才需要清理内存碎片。mem_fragmentation_ratio > 1.5 意味着你使用 Redis 存储实际大小 2G 的数据需要使用大于 3G 的内存。

 

如果想要快速查看内存碎片率的话，还可以通过下面这个命令：

```shell
> redis-cli -p 6379 info | grep mem_fragmentation_ratio
```



另外，内存碎片率可能存在小于 1 的情况。这种情况我在日常使用中还没有遇到过，感兴趣的小伙伴可以看看这篇文章 [故障分析 | Redis 内存碎片率太低该怎么办？- 爱可生开源社区](https://mp.weixin.qq.com/s?__biz=MzU2NzgwMTg0MA==&mid=2247492545&idx=1&sn=134d8099469077b03f96b682979409a2&scene=21#wechat_redirect) 。



## 如何清理 Redis 内存碎片？

Redis4.0-RC3 版本以后自带了内存整理，可以避免内存碎片率过大的问题。

直接通过 config set 命令将 activedefrag 配置项设置为 yes 即可。

```shell
config set activedefrag yes
```



具体什么时候清理需要通过下面两个参数控制：

```shell
# 内存碎片占用空间达到 500mb 的时候开始清理
config set active-defrag-ignore-bytes 500mb
# 内存碎片率大于 1.5 的时候开始清理
config set active-defrag-threshold-lower 50
```



通过 Redis 自动内存碎片清理机制可能会对 Redis 的性能产生影响，我们可以通过下面两个参数来减少对 Redis 性能的影响：

```shell
# 内存碎片清理所占用 CPU 时间的比例不低于 20%
config set active-defrag-cycle-min 20
# 内存碎片清理所占用 CPU 时间的比例不高于 50%
config set active-defrag-cycle-max 50
```



另外，重启节点可以做到内存碎片重新整理。如果你采用的是高可用架构的 Redis 集群的话，你可以将碎片率过高的主节点转换为从节点，以便进行安全重启。

 

 

 <!-- @include: @article-footer.snippet.md -->     

 

 

 



