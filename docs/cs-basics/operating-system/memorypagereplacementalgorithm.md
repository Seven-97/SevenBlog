---
title: 内存页面置换算法
category: 计算机基础
tag:
 - 操作系统
---



## 概述
当 CPU 访问的页面不在物理内存时，便会产生一个缺页中断，请求操作系统将所缺页调入到物理内存。但内存已无空闲空间时，这时候，就需要**页面置换算法**选择一个物理页，如果该物理页有被修改过（脏页），则把它换出到磁盘，然后把该被置换出去的页表项的状态改成「无效的」，最后把正在访问的页面装入到这个物理页中。

这里先引出页表项中的字段
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271130724.png)

- 状态位 R：用于表示该页是否有效，也就是说是否在物理内存中，供程序访问时参考。当页面被访问时设置页面的 R=1
- 访问字段：用于记录该页在一段时间被访问的次数，供页面置换算法选择出页面时参考。
- 修改位 M：表示该页在调入内存后是否有被修改过，由于内存中的每一页都在磁盘上保留一份副本，因此，如果没有修改，在置换该页时就不需要将该页写回到磁盘上，以减少系统的开销；如果已经被修改，则将该页重写到磁盘上，以保证磁盘中所保留的始终是最新的副本。当页面被修改时设置 M=1
- 硬盘地址：用于指出该页在硬盘上的地址，通常是物理块号，供调入该页时使用。

而页面置换算法目标则是，尽可能减少页面的换入换出的次数

## 最佳页面置换算法OPT
最佳页面置换算法基本思路是，置换在未来最长时间不访问的页面。

所以，该算法实现需要计算内存中每个逻辑页面的下一次访问时间，然后比较，选择未来最长时间不访问的页面。

如下图，刚开始时，1号页面要在4次之后再次访问，0号页面要在5次之后再次访问，2号页面要在6次之后再次访问；因此当新请求7号页面时，置换了标记数最大的页面，以此类推。当页面在之后不会被访问时，用 - 表示无穷大，将被优先置换
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271130369.png)

这很理想，但是实际系统中无法实现，因为操作系统无法得知每个页面要等待多长时间以后才会再次被访问，所以这种算法无法在现实中实现，通过用作页面置换算法的一种评价标准

## 最近最久未使用的置换算法LRU
虽然无法知道将来要使用的页面情况，但是可以知道过去使用页面的情况。

最近最久未使用（LRU）的置换算法的基本思路是，发生缺页时，选择最长时间没有被访问的页面进行置换，也就是说，该算法假设已经很久没有使用的页面很有可能在未来较长的一段时间内仍然不会被使用。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271130302.png)

实现 LRU，需要在内存中维护一个所有页面的链表。当一个页面被访问时，将这个页面移到链表表头。这样就能保证链表表尾的页面是最近最久未访问的。

因为每次访问都需要更新链表，因此这种方式实现的 LRU 代价很高。所以，LRU 虽然看上去不错，但是由于开销比较大，实际应用中比较少使用。

## 先进先出置换算法FIFO
FIFO（First-In First-Out，先进先出）算法思想非常简单，就是最先被放入内存的页面最早被换出。

实现方式也比较简单，可以由操作系统维护一个有长度限制的内存页链表，每次添加时从尾部插入，当超过长度限制时，就将链表头部的内存页置换掉。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271130193.png)

FIFO 算法可能会把经常使用的页面置换出去，为了避免这一问题，对该算法做一个简单的修改：检查最老页面的 R 位。如果 R 位是 0，那么这个页面既老又没有被使用，可以立刻置换掉；如果是 1，就将 R 位置0，并把该页面放到链表的尾端，使它就像刚装入一样，然后继续搜索。这一算法称为第二次机会（second chance）算法。

第二次机会算法就是寻找一个在最近的时钟间隔内没有被访问过的页面。如果所有的页面都被访问过，该算法就简化为纯粹的 FIFO 算法。

## 最不常用算法LFU
最不常用（LFU）算法，主要思想是当发生缺页中断时，选择「访问次数」最少的那个页面，并将其淘汰。这种选择的原因是，积极使用的页面应当具有大的引用计数。

它的实现方式是，对每个页面设置一个「访问计数器」，每当一个页面被访问时，该页面的访问计数器就累加 1。在发生缺页中断时，淘汰计数器值最小的那个页面。

然而，LFU 算法只考虑了频率问题，没考虑时间的问题。比如当一个页面在进程的初始阶段大量使用但是随后不再使用时，会出现问题。由于被大量使用，它有一个大的计数，即使很久没再使用了，却仍保留在内存中。一种解决方案是，定期地将计数右移1位，以形成指数衰减的平均使用计数，也就说，随着时间的流失，以前的高访问次数的页面会慢慢减少，相当于加大了被置换的概率

但是这个算法需要要增加一个计数器来实现，这个硬件成本是比较高的，另外如果要对这个计数器查找哪个页面访问次数最小，查找链表本身，如果链表长度很大，是非常耗时的，效率不高。

## 最常使用算法MFU

最经常使用(MFU)页面置换算法是基于如下论点：具有最小计数的页面可能刚刚被引入并且尚未使用。

MFU和LFU置换都不常用。这些算法的实现是昂贵的，并且它们不能很好地近似OPT置换。

## 时钟页面置换算法
FIFO的第二次机会算法需要在链表中移动页面，降低了效率。时钟算法使用环形链表将页面连接起来，再使用一个指针指向最老的页面。

因此时钟页面置换算法跟 LRU 近似，又是对 FIFO 的第二次机会算法的一种改进。

当发生缺页中断时，算法首先检查表指针指向的页面：
- R 位为 0 就淘汰该页面，并把新的页面插入这个位置，新的页R为置为1，然后把表指针向前移动一个位置
- R 位为 1 就清除 R 位并把表指针向前去移动一个位置，重复这个过程直到找到一个 R 位为 0 的页面为止

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271130516.png)