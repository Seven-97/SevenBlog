---
title: 策略 - 持久化技术
category: 数据库
tag:
 - Redis
---



Redis属于内存数据库，但为了防止宕机等导致的数据丢失，也有对应的数据持久化技术。持久化主要作用就是数据备份，即将数据存储在硬盘，保证数据不会因进程退出而丢失。

## AOF持久化
Append Only File 

类似于Mysql的binlog日志类似，会吧**写操作命令**以追加写的方式写入到AOF日志中。当重启redis后，先去读取这个文件里的命令，并且执行它，就相当于恢复了缓存数据。

Redis写入日志过程图：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270806619.png)

1. Redis 在执行完写操作命令后，并不会直接将命令写入到硬盘中的AOF日志中，因为这样将会产生大量的IO，而是会将命令追加到 server.aof_buf 缓冲区；
2. 然后通过 write() 系统调用，将 aof_buf 缓冲区的数据写入到 AOF 文件，此时数据并没有写入到硬盘，而是拷贝到了内核缓冲区 page cache，等待内核将数据写入硬盘；
3. 具体缓冲区的数据什么时候写入到硬盘，由**写回策略**来决定。

### 三种写回策略
Redis有三种写回策略：
- Always，每次写操作命令执行完后，总是会将 AOF 日志数据写回硬盘；
- Everysec，每次写操作命令执行完后，先将命令写入到 AOF 文件的内核缓冲区，然后每隔一秒将缓冲区里的内容写回到硬盘；
- No，意味着不由 Redis 控制写回硬盘的时机，转交给操作系统控制写回的时机，也就是每次写操作命令执行完后，先将命令写入到 AOF 文件的内核缓冲区，再由操作系统决定何时将缓冲区内容写回硬盘。

这三种写回策略在源码中其实就是在控制fsync()方法的调用时机。
```c
if (sdslen(server.aof_buf) == 0) {//检查aof_buf中有没有数据
    if (server.aof_fsync == AOF_FSYNC_EVERYSEC &&
        server.aof_last_incr_fsync_offset != server.aof_last_incr_size &&
        server.unixtime > server.aof_last_fsync &&
        !(sync_in_progress = aofFsyncInProgress())) {
        goto try_fsync;//控制每秒写回；异步执行，不影响主线程

    } else if (server.aof_fsync == AOF_FSYNC_ALWAYS &&
               server.aof_last_incr_fsync_offset != server.aof_last_incr_size){
        goto try_fsync;//总是写回；由主线程执行，未返回会阻塞主线程
    } else {
		//redis不控制写回，最终交给操作系统决定何时写回；不影响主线程
        return;
    }
}
```
显然Always写回策略是由`主进程`执行的，总是调用fsync函数；Everysec异步执行，不影响主线程；No则redis不控制写回，最终交给操作系统决定何时写回；不影响主线程

> fsync()函数会将内存中修改的数据和文件描述符的属性持久化到存储设备中，并且等到硬盘写操作完成后，**该函数才会返回**。  

三种写回策略的优缺点：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270806054.png)

### AOF 重写机制
重写机制主要就是为了压缩AOF文件的大小，当 AOF 文件的大小超过所设定的阈值后，Redis 就会启用 AOF 重写机制，来压缩 AOF 文件。

```c
//表示当前AOF文件空间（aof_current_size） 和上一次重写后AOF文件空间（aof_base_size） 的比值。
auto-aof-rewrite-percentage 100

//表示运行AOF重写时文件最小体积， 默认为64MB。
auto-aof-rewrite-min-size 64mb 
```

AOF 重写机制是在重写时，读取当前数据库中的所有键值对，然后将每一个键值对用一条命令记录到 **新的 AOF 文件**，等到全部记录完后，就将新的 AOF 文件替换掉现有的 AOF 文件。

重写机制的原理：如果某个键值对被多条写命令反复修改，最终也只需要根据这个键值对当前的最新状态，然后用一条命令去记录键值对，代替之前记录这个键值对的多条命令，这样就减少了 AOF 文件中的命令数量。

> 重写时为什么不复用当前AOF？  
> 如果 AOF 重写过程中失败了，现有的 AOF 文件就会造成污染，可能无法用于恢复使用。

Redis 的重写 AOF 过程是由后台`子进程 bgrewriteaof` 来完成的

## RDB快照
RDB 快照就是记录某一个瞬间的内存数据，记录的是实际数据，也就是说RDB是**全量快照**，也就是说每次执行RDB，都是把内存中的所有数据都记录到磁盘中。当需要恢复数据时， RDB 恢复数据的效率也会比 AOF 高些，因为直接将 RDB 文件读入内存就可以，不需要像 AOF 那样还需要额外执行操作命令的步骤才能恢复数据。

由于RDB是全量快照，因此不建议过于频繁，但频率过低也会导致丢失的数据更多。



### 执行命令

**RDB全量模式**持久化将数据写入磁盘的动作可以分为`SAVE`与`BGSAVE`两种。所谓BGSAVE就是background-save，也就是后台异步save，区别点在于SAVE是由Redis的**命令执行线程**按照普通命令的方式去执行操作，而BGSAVE是通过fork出一个新的进程，在新的**独立进程**里面去执行save操作。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406120048991.png)



Redis的请求命令执行是通过单线程的方式执行的，所以要尽量避免耗时操作，而save动作需要将内存全部数据写入到磁盘上，对于redis而言，这一操作是*非常耗时*的，会阻塞住全部正常业务请求，所以save操作的触发只有两个场景：

1. 客户端手动发送save命令执行
2. Redis在shutdown的时候自动执行

从数据保存完备性方面看，这两种方式都起不到自动持久化备份的能力，如果出现一些机器掉电等情况，是不会触发redis shutdown操作的，将面临数据丢失的风险。

相比而言，`bgsave`的杀伤力要小一些、适用度也更好一些，它可以保证在持久化期间Redis主进程可以继续处理业务请求。bgsave增加了过程中自动持久化操作的机制，触发条件更加的“智能”：

1. 客户端手动命令触发bgsave操作
2. Redis配置*定时*任务触发（支持间隔时间+变更数据量双重维度综合判断，达到任一条件则触发）

此外，在master-slave[主从部署](https://www.seven97.top/database/redis/04-ha1-masterslavereplication.html)的场景中还支持仅由slave节点触发bgsave操作，来降低对master节点的影响。



### 写时复制技术
Redis可以执行bgsave，将生成RDB的工作交给`子进程`来做，此时Redis主线程还可以继续处理操作命令。Redis为了实现后台把内存数据的快照写入文件，采用了操作系统提供的Copy On Write写时复制技术，也就是fork系统调用。

写时复制大致过程如下：
1. fork系统调用会产生一个子进程，与父进程共享相同的内存地址空间，这样进程在这一时刻就能拥有与父进程的相同的内存数据。
2. 虽然子进程与父进程共享同一块内存地址空间，但在fork子进程时，操作系统**需要拷贝父进程的内存页表给子进程**，如果整个Redis实例内存占用很大，那么它的内存页表也会很大，在拷贝时就会比较耗时，同时这个过程会消耗大量的CPU资源。在完成拷贝之前父进程也处于阻塞状态，无法处理客户端请求。
3. fork执行完之后，子进程就可以扫描自身所有的内存数据，然后把全部数据写入到RDB文件中。
4. 之后父进程依旧处理客户端的请求，当在处理写命令时，父进程会重新分配新的内存地址空间，从操作系统申请新的内存使用，不再与子进程共享，这个过程就是Copy On Write（写实复制）名字的由来。这样父子进程的内存就会逐渐分离，父进程申请新的内存空间并更改内存数据，子进程的内存数据不受影响。

比如：当主线程要修改共享数据里的某一块数据（比如键值对 A）时，就会发生写时复制，那么这块数据的物理内存就会被复制一份（键值对 A'），bgsave 子进程可以把原来的数据（键值对 A）写入到 RDB 文件中。与此同时，主线程可以在这个数据副本（键值对 A'）进行修改操作。

为了保证生成RDB时还能执行操作命令，引入的写时复制技术，但显然写时复制技术也有其缺点：  
- 在生成RDB的过程中，如果主线程修改了内存数据，RDB 快照无法写入主线程刚修改的数据，如果此时系统宕机了，也就**丢失了这部分修改的数据**。

- 极端情况下，所有数据都被修改，那么由于写时复制技术，**内存占用将会是原来的两倍**。如果机器剩余内存不足，则可能导致fork的时候两份内存数据量超过机器物理内存大小，导致系统启用虚拟内存，拷贝速度大打折扣（虚拟内存本质上就是把磁盘当内存用，操作速度相比物理内存大大降低），会阻塞住Redis主进程的命令执行

  

## 总结
区别：
1. 记录的数据不一样：
	- RDB 快照就是记录某一个瞬间的内存数据，**记录的是实际数据**，而 AOF 文件**记录的是命令操作的日志**
	- AOF 文件的内容是**操作命令**；RDB 文件的内容是**二进制数据**。
2. 恢复数据和执行频率：
	- RDB是全量快照，**恢复数据更快**，AOF则需要额外执行操作命令，相对更慢。
	- RDB是全量快照，**不宜频繁执行**，而AOF数据文件更新比较及时，比RDB保存更完整的数据，这样在数据恢复时能够恢复尽量完整的数据，降低丢失数据的风险。因此发生故障时，RDB丢失的数据会比 AOF 持久化的方式更多
3. 是否影响主进程
	- AOF的Always写回策略是`主进程`执行的，总是调用fsync函数；Everysec异步执行，不影响主线程；No则redis不控制写回，最终交给操作系统决定何时写回；不影响主线程。
	- RDB可以将工作交给`子进程`来做，此时Redis主线程还可以继续处理操作命令。

如果同时存在RDB文件和AOF文件，Redis会**优先使用AOF**文件进行数据恢复。

### 混合持久化
RDB 比 AOF 的数据恢复速度快，但是快照的频率不好把握：
- 如果频率太低，两次快照间一旦服务器发生宕机，就可能会比较多的数据丢失；
- 如果频率太高，频繁写入磁盘和创建子进程会带来额外的性能开销。

混合持久化就是混合使用 AOF 日志和RDB

混合持久化工作在 **AOF日志重写**过程中：  
会把 Redis 的持久化数据，以 RDB 的格式写入到 AOF 文件的开头，之后写时复制时修改数据再以 AOF 的格式化追加的文件的末尾，写入完成后再新的含有 RDB 格式和 AOF 格式的 AOF 文件替换旧的的 AOF 文件。

也就是说，使用了混合持久化，AOF 文件的前半部分是 RDB 格式的全量数据，后半部分是 AOF 格式的增量数据。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270806424.png)

Redis恢复数据源码：  
（AOF 格式的开头是 *，而 RDB 格式的开头是 REDIS。）
```c
if (fread(sig,1,5,fp) != 5 || memcmp(sig,"REDIS",5) != 0) {
    // AOF 文件开头非 RDB 格式，非混合持久化文件
    if (fseek(fp,0,SEEK_SET) == -1) goto readerr;
} else {
    /* RDB format. Pass loading the RDB functions. */
    rio rdb;
    int old_style = !strcmp(filename, server.aof_filename);
    if (old_style)
        serverLog(LL_NOTICE, "Reading RDB preamble from AOF file...");
    else 
        serverLog(LL_NOTICE, "Reading RDB base file on AOF loading..."); 

    if (fseek(fp,0,SEEK_SET) == -1) goto readerr;
    rioInitWithFile(&rdb,fp);
	// AOF 文件开头是 RDB 格式，先加载 RDB 再加载 AOF
    if (rdbLoadRio(&rdb,RDBFLAGS_AOF_PREAMBLE,NULL) != C_OK) {
        if (old_style)
            serverLog(LL_WARNING, "Error reading the RDB preamble of the AOF file %s, AOF loading aborted", filename);
        else
            serverLog(LL_WARNING, "Error reading the RDB base file %s, AOF loading aborted", filename);

        ret = AOF_FAILED;
        goto cleanup;
    } else {
        loadingAbsProgress(ftello(fp));
        last_progress_report_size = ftello(fp);
        if (old_style) serverLog(LL_NOTICE, "Reading the remaining AOF tail...");
    }
}
```

优点：
- 混合持久化结合了 RDB 和 AOF 持久化的优点，开头为 RDB 的格式，使得 Redis 可以更快的恢复数据，同时结合 AOF 的优点，减低了大量数据丢失的风险。

缺点：
- AOF 文件中添加了 RDB 格式的内容，使得 AOF 文件的可读性变得很差；
- 兼容性差，如果开启混合持久化，那么此混合持久化 AOF 文件，就不能用在 Redis 4.0 之前版本了。


<!-- @include: @article-footer.snippet.md -->     
