---
title: 系统管理
category: 计算机基础
tag:
  - Linux
head:
  - - meta
    - name: keywords
      content: Linux命令,系统管理,find命令,rpm命令,ps命令,uname命令,kill命令,top命令,netstat命令,ss命令
  - - meta
    - name: description
      content: 全网最全的Linux命令总结，让天下没有难学的八股文！
---



## find命令 - 根据路径和条件搜索指定文件



find命令的功能是根据给定的路径和条件查找相关文件或目录，其参数灵活方便，且支持正则表达式，结合管道符后能够实现更加复杂的功能，是Linux系统运维人员必须掌握的命令之一。

find命令通常进行的是从根目录（/）开始的全盘搜索，有别于whereis、which、locate等有条件或部分文件的搜索。对于服务器负载较高的情况，建议不要在高峰时期使用find命令的模糊搜索，这会相对消耗较多的系统资源。 



### 语法格式

```shell
find [路径] [匹配条件] [动作]
```

**参数说明** :

**路径** 是要查找的目录路径，可以是一个目录或文件名，也可以是多个路径，多个路径之间用空格分隔，如果未指定路径，则默认为当前目录。

**expression** 是可选参数，用于指定查找的条件，可以是文件名、文件类型、文件大小等等。



匹配条件 中可使用的选项有二三十个之多，以下列出最常用的部份：

- `-name pattern`：按文件名查找，支持使用通配符 `*` 和 `?`。
- `-type type`：按文件类型查找，可以是 `f`（普通文件）、`d`（目录）、`l`（符号链接）等。
- `-size [+-]size[cwbkMG]`：按文件大小查找，支持使用 `+` 或 `-` 表示大于或小于指定大小，单位可以是 `c`（字节）、`w`（字数）、`b`（块数）、`k`（KB）、`M`（MB）或 `G`（GB）。
- `-mtime days`：按修改时间查找，支持使用 `+` 或 `-` 表示在指定天数前或后，days 是一个整数表示天数。
- `-user username`：按文件所有者查找。
- `-group groupname`：按文件所属组查找。



**动作:** 可选的，用于对匹配到的文件执行操作，比如删除、复制等。

find 命令中用于时间的参数如下：

- `-amin n`：查找在 n 分钟内被访问过的文件。
- `-atime n`：查找在 n*24 小时内被访问过的文件。
- `-cmin n`：查找在 n 分钟内状态发生变化的文件（例如权限）。
- `-ctime n`：查找在 n*24 小时内状态发生变化的文件（例如权限）。
- `-mmin n`：查找在 n 分钟内被修改过的文件。
- `-mtime n`：查找在 n*24 小时内被修改过的文件。

在这些参数中，n 可以是一个正数、负数或零。正数表示在指定的时间内修改或访问过的文件，负数表示在指定的时间之前修改或访问过的文件，零表示在当前时间点上修改或访问过的文件。

**正数应该表示时间之前，负数表示时间之内。**

例如：**-mtime 0** 表示查找今天修改过的文件，**-mtime -7** 表示查找一周以前修改过的文件。

关于时间 n 参数的说明：

- **+n**：查找比 n 天前更早的文件或目录。
- **-n**：查找在 n 天内更改过属性的文件或目录。
- **n**：查找在 n 天前（指定那一天）更改过属性的文件或目录。



### 实例

查找当前目录下名为 file.txt 的文件：

```shell
find . -name file.txt
```



将当前目录及其子目录下所有文件后缀为 **.c** 的文件列出来:

```shell
find . -name "*.c"
```



将当前目录及其子目录中的所有文件列出：

```shell
find . -type f
```



查找 /home 目录下大于 1MB 的文件：

```shell
find /home -size +1M
```



查找 /var/log 目录下在 7 天前修改过的文件：

```shell
find /var/log -mtime +7
```



查找过去 7 天内被访问的文件:

```shell
find /path/to/search -atime -7
```



在当前目录下查找最近 20 天内状态发生改变的文件和目录:

```shell
find . -ctime  20
```



将当前目录及其子目录下所有 20 天前及更早更新过的文件列出:

```shell
find . -ctime  +20
```



查找 /var/log 目录中更改时间在 7 日以前的普通文件，并在删除之前询问它们：

```shell
find /var/log -type f -mtime +7 -ok rm {} \;
```



查找当前目录中文件属主具有读、写权限，并且文件所属组的用户和其他用户具有读权限的文件：

```shell
find . -type f -perm 644 -exec ls -l {} \;
```



查找系统中所有文件长度为 0 的普通文件，并列出它们的完整路径：

```shell
find / -type f -size 0 -exec ls -l {} \;
```



找并执行操作（例如删除）：

```shell
find /path/to/search -name "pattern" -exec rm {} \;
```

这个例子中，**-exec** 选项允许你执行一个命令，**{}** 将会被匹配到的文件名替代，**\;** 表示命令结束。



## rpm命令 - RPM软件包管理器

rpm命令来自英文词组redhat package manager的缩写，中文译为“红帽软件包管理器”，其功能是在Linux系统下对软件包进行安装、卸载、查询、验证、升级等工作，常见的主流系统（如RHEL、CentOS、Fedora等）都采用这种软件包管理器，推荐用固定搭配“rpm-ivh 软件包名”安装软件，而卸载软件则用固定搭配“rpm -evh 软件包名”，简单好记又好用。 



### 语法格式

```shell
rpm 参数 软件包名 
```



常用参数

- -a ：查询所有套件。
- -b<完成阶段><套件档>+或-t <完成阶段><套件档>+ ：设置包装套件的完成阶段，并指定套件档的文件名称。
- -c ：只列出组态配置文件，本参数需配合"-l"参数使用。
- -d ：只列出文本文件，本参数需配合"-l"参数使用。
- -e<套件档>或--erase<套件档> ：删除指定的套件。
- -f<文件>+ ：查询拥有指定文件的套件。
- -h或--hash ：套件安装时列出标记。
- -i ：显示套件的相关信息。
- -i<套件档>或--install<套件档> ：安装指定的套件档。
- -l ：显示套件的文件列表。
- -p<套件档>+ ：查询指定的RPM套件档。
- -q ：使用询问模式，当遇到任何问题时，rpm指令会先询问用户。
- -R ：显示套件的关联性信息。
- -s ：显示文件状态，本参数需配合"-l"参数使用。
- -U<套件档>或--upgrade<套件档> ：升级指定的套件档。
- -v ：显示指令执行过程。
- -vv ：详细显示指令执行过程，便于排错。

### 实例

安装软件

```shell
# rpm -hvi dejagnu-1.4.2-10.noarch.rpm 
警告：dejagnu-1.4.2-10.noarch.rpm: V3 DSA 签名：NOKEY, key ID db42a60e
准备...           
########################################### [100%]
```



显示软件安装信息

```shell
# rpm -qi dejagnu-1.4.2-10.noarch.rpm

【第1次更新 教程、类似命令关联】
```



## ps命令 - 显示进程状态

ps命令来自英文单词process的缩写，中文译为“进程”，其功能是显示当前系统的进程状态。使用ps命令可以查看到进程的所有信息，例如进程的号码、发起者、系统资源（处理器与内存）使用占比、运行状态等。ps命令可帮助我们及时发现哪些进程出现“僵死”或“不可中断”等异常情‍况。 ps命令经常会与kill命令搭配使用，以中断和删除不必要的服务进程，避免服务器的资源浪费。 

### 语法格式：

```shell
ps [options] [--help]
```

ps 的参数非常多, 在此仅列出几个常用的参数并大略介绍含义

- -A：列出所有的进程

- -w ：显示加宽可以显示较多的资讯

- -au ：显示较详细的资讯

- -aux ：显示所有包含其他使用者的进程，用标准格式输出。

- -ef：与aux一样，但是是用bsd的格式输出

  

  au(x) ：输出格式 :

```shell
USER PID %CPU %MEM VSZ RSS TTY STAT START TIME CMD
```

  - USER: 进程拥有者
  - PID: pid，进程ID
  - %CPU: 占用的 CPU 使用率
  - %MEM: 占用的内存使用率
  - VSZ: 占用的虚拟内存大小，表示如果一个程序完全驻留在内存的话需要占用多少内存空间
  - RSS: 常驻集大小，指明了当前实际占用了多少内存
  - TTY: 终端的次要装置号码 (minor device number of tty)
  - STAT: 该行程的状态:
    - D: 无法中断的休眠状态 (通常 IO 的进程)
    - R: 正在执行中
    - S: 静止状态
    - T: 暂停执行
    - Z: 不存在但暂时无法消除
    - W: 没有足够的记忆体分页可分配
    - <: 高优先序的行程
    - N: 低优先序的行程
    - L: 有记忆体分页分配并锁在记忆体内 (实时系统或捱A I/O)
  - START: 进程开始时间
  - TIME: 执行的时间
  - COMMAND:所执行的指令

### 实例

查找指定进程格式：

```shell
ps -ef | grep 进程关键字
```

例如显示 php 的进程：

```shell
# ps -ef | grep php
root       794     1  0  2020 ?        00:00:52 php-fpm: master process (/etc/php/7.3/fpm/php-fpm.conf)
www-data   951   794  0  2020 ?        00:24:15 php-fpm: pool www
www-data   953   794  0  2020 ?        00:24:14 php-fpm: pool www
www-data   954   794  0  2020 ?        00:24:29 php-fpm: pool www
...
```



显示进程信息：

```shell
# ps -A 
PID TTY     TIME CMD
  1 ?    00:00:02 init
  2 ?    00:00:00 kthreadd
  3 ?    00:00:00 migration/0
  4 ?    00:00:00 ksoftirqd/0
  5 ?    00:00:00 watchdog/0
  6 ?    00:00:00 events/0
  7 ?    00:00:00 cpuset
  8 ?    00:00:00 khelper
  9 ?    00:00:00 netns
  10 ?    00:00:00 async/mgr
  11 ?    00:00:00 pm
  12 ?    00:00:00 sync_supers
  13 ?    00:00:00 bdi-default
  14 ?    00:00:00 kintegrityd/0
  15 ?    00:00:02 kblockd/0
  16 ?    00:00:00 kacpid
  17 ?    00:00:00 kacpi_notify
  18 ?    00:00:00 kacpi_hotplug
  19 ?    00:00:27 ata/0
……省略部分结果
30749 pts/0  00:00:15 gedit
30886 ?    00:01:10 qtcreator.bin
30894 ?    00:00:00 qtcreator.bin 
31160 ?    00:00:00 dhclient
31211 ?    00:00:00 aptd
31302 ?    00:00:00 sshd
31374 pts/2  00:00:00 bash
31396 pts/2  00:00:00 ps
```



显示指定用户信息

```shell
# ps -u root //显示root进程用户信息
 PID TTY     TIME CMD
  1 ?    00:00:02 init
  2 ?    00:00:00 kthreadd
  3 ?    00:00:00 migration/0
  4 ?    00:00:00 ksoftirqd/0
  5 ?    00:00:00 watchdog/0
  6 ?    00:00:00 events/0
  7 ?    00:00:00 cpuset
  8 ?    00:00:00 khelper
  9 ?    00:00:00 netns
  10 ?    00:00:00 async/mgr
  11 ?    00:00:00 pm
  12 ?    00:00:00 sync_supers
  13 ?    00:00:00 bdi-default
  14 ?    00:00:00 kintegrityd/0
  15 ?    00:00:02 kblockd/0
  16 ?    00:00:00 kacpid
……省略部分结果
30487 ?    00:00:06 gnome-terminal
30488 ?    00:00:00 gnome-pty-helpe
30489 pts/0  00:00:00 bash
30670 ?    00:00:00 debconf-communi 
30749 pts/0  00:00:15 gedit
30886 ?    00:01:10 qtcreator.bin
30894 ?    00:00:00 qtcreator.bin 
31160 ?    00:00:00 dhclient
31211 ?    00:00:00 aptd
31302 ?    00:00:00 sshd
31374 pts/2  00:00:00 bash
31397 pts/2  00:00:00 ps
```



显示所有进程信息，连同命令行

```shell
# ps -ef //显示所有命令，连带命令行
UID    PID PPID C STIME TTY     TIME CMD
root     1   0 0 10:22 ?    00:00:02 /sbin/init
root     2   0 0 10:22 ?    00:00:00 [kthreadd]
root     3   2 0 10:22 ?    00:00:00 [migration/0]
root     4   2 0 10:22 ?    00:00:00 [ksoftirqd/0]
root     5   2 0 10:22 ?    00:00:00 [watchdog/0]
root     6   2 0 10:22 ?    /usr/lib/NetworkManager
……省略部分结果
root   31302 2095 0 17:42 ?    00:00:00 sshd: root@pts/2 
root   31374 31302 0 17:42 pts/2  00:00:00 -bash
root   31400   1 0 17:46 ?    00:00:00 /usr/bin/python /usr/sbin/aptd
root   31407 31374 0 17:48 pts/2  00:00:00 ps -ef
```



## uname命令 - 显示系统内核信息

uname命令来自英文词组UNIX name的缩写，其功能是查看系统主机名、内核及硬件架构等信息。如果不加任何参数，默认仅显示系统内核名称（相当于-s参数）的作用。 

### 语法格式

```shell
uname [-amnrsv][--help][--version]
```



常用参数：

- -a 或--all 　显示全部的信息，包括内核名称、主机名、操作系统版本、处理器类型和硬件架构等。。
- -m 或--machine 　显示处理器类型。
- -n 或--nodename 　显示主机名。
- -r 或--release 　显示内核版本号。
- -s 或--sysname 　显示操作系统名称。
- -v 　显示操作系统的版本。
- --help 　显示帮助。
- --version 　显示版本信息。
- -p 显示处理器类型（与 -m 选项相同）。



### 实例

显示系统信息：

```shell
uname -a
Linux iZbp19byk2t6khuqj437q6Z 4.11.0-14-generic #20~16.04.1-Ubuntu SMP Wed Aug 9 09:06:22 UTC 2017 x86_64 x86_64 x86_64 GNU/Linux
```



显示计算机类型：

```shell
uname -m
x86_64
```



显示计算机名：

```shell
uname -n
runoob-linux
```



显示操作系统发行编号：

```shell
uname -r
4.11.0-14-generic
```



显示操作系统名称：

```shell
uname -s
Linux
```



显示系统版本与时间：

```shell
uname -v
#20~16.04.1-Ubuntu SMP Wed Aug 9 09:06:22 UTC 2017
```



## kill命令 - 杀死进程

kill命令的功能是杀死（结束）进程。Linux系统中如需结束某个进程，既可以使用如service或systemctl这样的管理命令来结束服务，也可以使用kill命令直接结束进程信息。 如使用kill命令后进程并没有结束，则可以使用信号9进行强制杀死动作。 

### 语法格式

```shell
kill [options] <PID>
```



常用参数：

- `-l`：列出所有可用的信号。
- `-<signal>`：发送特定的信号给目标进程，如 `-9` 表示发送 KILL 信号，即强制终止进程。



### 实例

**终止进程：**默认情况下，kill命令发送SIGTERM（信号15），这可以请求进程终止。如果进程没有捕获这个信号，它将被终止。

```shell
kill PID
```

其中 PID 是进程的 ID。

**发送指定信号：**通过 **-s** 选项可以发送指定的信号。

```shell
kill -s SIGNAL PID
```

例如，发送 SIGKILL（信号9）将立即结束进程，不能被忽略或捕获。

```shell
kill -9 PID
```

**杀死进程组：**使用 -9 选项可以杀死整个进程组。

```shell
kill -9 -PID
```

使用 kill -l 命令列出所有可用信号。

最常用的信号是：

- `SIGKILL`（信号9）：立即结束进程，不能被捕获或忽略。
- `SIGTERM`（信号15）：正常结束进程，可以被捕获或忽略。
- `SIGSTOP`（信号19）：暂停进程，不能被捕获、忽略或结束。
- `SIGCONT`（信号18）：继续执行被暂停的进程。
- `SIGINT`（信号2）：通常是Ctrl+C产生的信号，可以被进程捕获或忽略。



如：

终止 PID 为 1234 的进程：

```shell
kill 1234
```



强制终止 PID 为 1234 的进程：

```shell
kill -9 1234
```



向 PID 为 1234 的进程发送 SIGSTOP：

```shell
kill -s SIGSTOP 1234
```



显示信号

```shell
# kill -l
1) SIGHUP     2) SIGINT     3) SIGQUIT     4) SIGILL     5) SIGTRAP
6) SIGABRT     7) SIGBUS     8) SIGFPE     9) SIGKILL    10) SIGUSR1
11) SIGSEGV    12) SIGUSR2    13) SIGPIPE    14) SIGALRM    15) SIGTERM
16) SIGSTKFLT    17) SIGCHLD    18) SIGCONT    19) SIGSTOP    20) SIGTSTP
21) SIGTTIN    22) SIGTTOU    23) SIGURG    24) SIGXCPU    25) SIGXFSZ
26) SIGVTALRM    27) SIGPROF    28) SIGWINCH    29) SIGIO    30) SIGPWR
31) SIGSYS    34) SIGRTMIN    35) SIGRTMIN+1    36) SIGRTMIN+2    37) SIGRTMIN+3
38) SIGRTMIN+4    39) SIGRTMIN+5    40) SIGRTMIN+6    41) SIGRTMIN+7    42) SIGRTMIN+8
43) SIGRTMIN+9    44) SIGRTMIN+10    45) SIGRTMIN+11    46) SIGRTMIN+12    47) SIGRTMIN+13
48) SIGRTMIN+14    49) SIGRTMIN+15    50) SIGRTMAX-14    51) SIGRTMAX-13    52) SIGRTMAX-12
53) SIGRTMAX-11    54) SIGRTMAX-10    55) SIGRTMAX-9    56) SIGRTMAX-8    57) SIGRTMAX-7
58) SIGRTMAX-6    59) SIGRTMAX-5    60) SIGRTMAX-4    61) SIGRTMAX-3    62) SIGRTMAX-2
63) SIGRTMAX-1    64) SIGRTMAX
```



杀死指定用户所有进程:

```shell
kill -9 $(ps -ef | grep hnlinux) //方法一 过滤出hnlinux用户进程 
kill -u hnlinux //方法二
```



注意事项：

- 在使用 `kill` 命令时，需要具有相应的权限，否则可能无法终止进程。
- 某些进程可能需要发送多次信号才能终止，比如一些守护进程。
- 强制杀死进程可能会导致数据丢失或其他副作用，因此应谨慎使用。



## top命令 - 实时监控

Linux **top** 是一个在 Linux 和其他类 Unix 系统上常用的实时系统监控工具。它提供了一个动态的、交互式的实时视图，显示系统的整体性能信息以及正在运行的进程的相关信息。。

### 语法格式

```shell
top [-] [d delay] [q] [c] [S] [s] [i] [n] [b]
```

**参数说明**：

- `-d <秒数>`：指定 top 命令的刷新时间间隔，单位为秒。
- `-n <次数>`：指定 top 命令运行的次数后自动退出。
- `-p <进程ID>`：仅显示指定进程ID的信息。
- `-u <用户名>`：仅显示指定用户名的进程信息。
- `-H`：在进程信息中显示线程详细信息。
- `-i`：不显示闲置（idle）或无用的进程。
- `-b`：以批处理（batch）模式运行，直接将结果输出到文件。
- `-c`：显示完整的命令行而不截断。
- `-S`：累计显示进程的 CPU 使用时间。



### 显示信息

top 命令的一些常用功能和显示信息：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405261641462.png)

#### 总体系统信息

- 第一行信息：
  - 当前时间：15:08:39
  - 系统远行时间：28天5时35分
  - 当前登陆用户数： 1 
  - 系统负载，即任务队列的平均长度。 三个数值分别为 1分钟、5分钟、15分钟前到现在的平均值

- 第二行信息：
  - 进程总数：527
  - 正在运行的进程数：2
  - 睡眠的进程数：525
  - 停止的进程数：0
  - 僵尸进程数：0

- 第三行信息：
  - us：用户空间占用CPU百分比
  - sy：内核空间占用CPU百分比
  - ni：用户进程空间内改变过优先级的进程占用CPU百分比
  - id：空闲CPU百分比
  - wa：等待输入输出的CPU时间百分比
  - hi：硬中断（Hardware IRQ）占用CPU的百分比
  - si：软中断（Software Interrupts）占用CPU的百分比
  - st：用于有虚拟cpu的情况，用来指示被虚拟机偷掉的cpu时间

- 第四行信息：
  - 物理内存总量
  - 使用的物理内存总量
  - 空闲内存总量
  - 用作内核缓存的内存量

- 第五行信息：
  - 交换区总量
  - 使用的交换区总量
  - 空闲交换区总量
  - 缓冲的交换区总量



##### 系统负载load average的含义

load average显示的是最近1分钟、5分钟和15分钟的系统平均负载。系统平均负载被定义为在特定时间间隔内运行队列中(在CPU上运行或者等待运行多少进程)的平均进程数。load average数据是每隔5秒钟检查一次活跃的进程数，然后按特定算法计算出的数值。

如果一个进程满足以下条件则其就会位于运行队列中：
- 它没有在等待I/O操作的结果
- 它没有主动进入等待状态(也就是没有调用’wait’)
- 没有被停止(例如：等待终止)

Update：在Linux中，进程分为三种状态，一种是阻塞的进程blocked process，一种是可运行的进程runnable process，另外就是正在运行的进程running process。当进程阻塞时，进程会等待I/O设备的数据或者系统调用。

进程可运行状态时，它处在一个运行队列run queue中，与其他可运行进程争夺CPU时间。 系统的load是指正在运行running one和准备好运行runnable one的进程的总数。比如现在系统有2个正在运行的进程，3个可运行进程，那么系统的load就是5。load average就是一定时间内的load数量。

一般来说只要**每个CPU的当前活动进程数不大于3**那么系统的性能就是良好的，如果这个数除以逻辑CPU的数量，结果高于5的时候就表明系统在超负荷运转了。假设系统有两个CPU，load average为8.13，那么其每个CPU的当前任务数为：8.13/2=4.065。这表示该系统的性能是可以接受的。

> 这个具体场景具体分析。也有很多地方认为每个CPU满载是1，1是理想状态，只有负载小等于1 才认为系统性能是健康的。



#### 进程信息

- PID：进程的标识符。
- USER：运行进程的用户名。
- PR（优先级）：进程的优先级。
- NI（Nice值）：进程的优先级调整值。
- VIRT（虚拟内存）：进程使用的虚拟内存大小。
- RES（常驻内存）：进程实际使用的物理内存大小。
- SHR（共享内存）：进程共享的内存大小。
- %CPU：进程占用 CPU 的使用率。
- %MEM：进程占用内存的使用率。
- TIME+：进程的累计 CPU 时间。



#### 功能和交互操作

在 top 运行时可以使用一些按键命令进行操作如按下 "k" 可以终止一个进程，按下 "h" 可以显示帮助信息等。

- 排序：可以按照 CPU 使用率、内存使用率、进程 ID 等对进程进行排序。
  - P：以CPU的使用资源排序显示
  - M：以内存的使用资源排序显示
  - N：以pid排序显示
  - ‘x’ 打开/关闭排序列的加亮效果，可以按”shift+>”或者”shift+<”左右改变排序序列
- 刷新频率：可以设置 top 的刷新频率，以便动态查看系统信息。
- 按键盘数字“1”可以监控每个逻辑CPU的状况
- 按键盘字母“b"  打开关闭加亮效果，也可以按 ‘y’ 来打开或者关闭运行态进程的加亮效果

### 实例

显示进程信息

```shell
top
```



显示完整命令

```shell
top -c
```



以批处理模式显示程序信息

```shell
top -b
```



以累积模式显示程序信息

```shell
top -S
```



设置信息更新次数

```shell
top -n 2

//表示更新两次后终止更新显示
```



设置信息更新时间

```shell
top -d 3

//表示更新周期为3秒
```



显示指定的进程信息

```shell
top -p 139

//显示进程号为139的进程信息，CPU、内存占用率等
```



显示更新十次后退出

```shell
top -n 10
```



使用者将不能利用交谈式指令来对行程下命令

```shell
top -s
```



## netstat命令 - 显示网络状态

netstat命令来自英文词组network statistics的缩写，其功能是显示各种网络相关信息，例如网络连接状态、路由表信息、接口状态、NAT、多播成员等。

 netstat命令不仅应用于Linux系统，而且Windows XP、Windows 7、Windows 10及Windows 11均已默认支持，并且可用参数也相同。 



### 语法格式

```shell
netstat 参数 
```

常用参数：

- -a或--all ：显示所有连线中的Socket。
- -A<网络类型>或--<网络类型> ：列出该网络类型连线中的相关地址。
- -c或--continuous ：持续列出网络状态。
- -C或--cache ：显示路由器配置的快取信息。
- -e或--extend ：显示网络其他相关信息。
- -F或--fib ：显示路由缓存。
- -g或--groups ：显示多重广播功能群组组员名单。
- -i或--interfaces ：显示网络界面信息表单。
- -l或--listening ：显示监控中的服务器的Socket。
- -M或--masquerade ：显示伪装的网络连线。
- -n或--numeric ：直接使用IP地址，而不通过域名服务器。
- -N或--netlink或--symbolic ：显示网络硬件外围设备的符号连接名称。
- -o或--timers ：显示计时器。
- -p或--programs ：显示正在使用Socket的程序识别码和程序名称。
- -r或--route ：显示Routing Table。
- -s或--statistics ：显示网络工作信息统计表。
- -t或--tcp ：显示TCP传输协议的连线状况。
- -u或--udp ：显示UDP传输协议的连线状况。
- -v或--verbose ：显示指令执行过程。
- -V或--version ：显示版本信息。
- -w或--raw ：显示RAW传输协议的连线状况。
- -x或--unix ：此参数的效果和指定"-A unix"参数相同。
- --ip或--inet ：此参数的效果和指定"-A inet"参数相同。

### 实例

系统网络状态中的所有连接信息：

```
netstat -a
```



显示当前用户UDP连接状况

```
netstat -nu
```



显示UDP端口号的使用情况

```
# netstat -apu
Active Internet connections (servers and established)
Proto Recv-Q Send-Q Local Address        Foreign Address       State    PID/Program name  
udp    0   0 *:32768           *:*                   -          
udp    0   0 *:nfs            *:*                   -          
udp    0   0 *:641            *:*                   3006/rpc.statd   
udp    0   0 192.168.0.3:netbios-ns   *:*                   3537/nmbd      
udp    0   0 *:netbios-ns        *:*                   3537/nmbd      
udp    0   0 192.168.0.3:netbios-dgm   *:*                   3537/nmbd      
udp    0   0 *:netbios-dgm        *:*                   3537/nmbd      
udp    0   0 *:tftp           *:*                   3346/xinetd     
udp    0   0 *:999            *:*                   3366/rpc.rquotad  
udp    0   0 *:sunrpc          *:*                   2986/portmap    
udp    0   0 *:ipp            *:*                   6938/cupsd     
udp    0   0 *:1022           *:*                   3392/rpc.mountd   
udp    0   0 *:638            *:*                   3006/rpc.statd
```



显示网卡列表

```
# netstat -i
Kernel Interface table
Iface    MTU Met  RX-OK RX-ERR RX-DRP RX-OVR  TX-OK TX-ERR TX-DRP TX-OVR Flg
eth0    1500  0  181864   0   0   0  141278   0   0   0 BMRU
lo    16436  0   3362   0   0   0   3362   0   0   0 LRU
```

显示网络路由表状态信息：

```shell
# netstat -r 
Kernel IP routing table 
Destination Gateway Genmask Flags MSS Window irtt Iface 
default _gateway 0.0.0.0 UG 0 0 0 eth0 
192.168.10.0 0.0.0.0 255.255.240.0 U 0 0 0 eth0
```



显示组播组的关系

```
# netstat -g
IPv6/IPv4 Group Memberships
Interface    RefCnt Group
--------------- ------ ---------------------
lo       1   ALL-SYSTEMS.MCAST.NET
eth0      1   ALL-SYSTEMS.MCAST.NET
lo       1   ff02::1
eth0      1   ff02::1:ff0a:b0c
eth0      1   ff02::1
```



只列出监听中的连接：任何网络服务的后台进程都会打开一个端口，用于监听接入的请求。这些正在监听的套接字也和连接的套接字一样，也能被 netstat 列出来。使用 -l 选项列出正在监听的套接字。

```
$ netstat -tnl
Active Internet connections (only servers)
Proto Recv-Q Send-Q Local Address           Foreign Address         State      
tcp        0      0 127.0.1.1:53            0.0.0.0:*               LISTEN     
tcp        0      0 127.0.0.1:631           0.0.0.0:*               LISTEN     
tcp6       0      0 ::1:631                 :::*                    LISTEN
```



获取进程名、进程号以及用户 ID：

使用 -p 选项查看进程信息。但使用 -p 选项时，netstat 必须运行在 root 权限之下，不然它就不能得到运行在 root 权限下的进程名，而很多服务包括 http 和 ftp 都运行在 root 权限之下。

```
$ sudo netstat -nlpt
Active Internet connections (only servers)
Proto Recv-Q Send-Q Local Address           Foreign Address         State       PID/Program name
tcp        0      0 127.0.1.1:53            0.0.0.0:*               LISTEN      1144/dnsmasq    
tcp        0      0 127.0.0.1:631           0.0.0.0:*               LISTEN      661/cupsd  tcp6       0      0 ::1:631                 :::*                    LISTEN      661/c
```

如果想找出特定端口（例如端口 21）的进程名称，则添加：`| grep -w ':21'` 



## ss命令 - 网络状态工具

ss命令用来显示处于活动状态的套接字信息。ss命令可以用来获取socket统计信息，它可以显示和 netstat 类似的内容。但ss的优势在于它能够显示更多更详细的有关TCP和连接状态的信息，而且比netstat更快速更高效。

当服务器的socket连接数量变得非常大时，无论是使用netstat命令还是直接 cat /proc/net/tcp，执行速度都会很慢。

ss快的秘诀在于，它利用到了TCP协议栈中tcp_diag。tcp_diag是一个用于分析统计的模块，可以获得Linux 内核中第一手的信息，这就确保了ss的快捷高效。当然，如果你的系统中没有tcp_diag，ss也可以正常运行，只是效率会变得稍慢。



### 语法格式

```shell
ss [option]
```



常用参数：

- -h：显示帮助信息；
- -V：显示指令版本信息；
- -n：不解析服务名称，以数字方式显示；
- -a：显示所有的套接字；
- -l：显示处于监听状态的套接字；
- -o：显示计时器信息；
- -m：显示套接字的内存使用情况；
- -p：显示使用套接字的进程信息；
- -i：显示内部的TCP信息；
- -4：只显示ipv4的套接字；
- -6：只显示ipv6的套接字；
- -t：只显示tcp套接字；
- -u：只显示udp套接字；
- -d：只显示DCCP套接字；
- -w：仅显示RAW套接字；
- -x：仅显示UNIX域套接字。



### 实例

只显示监听的套接字

```shell
ss -lnt
```



不解析主机名

```shell
ss -nt
```



打印进程名和进程号

```shell
ss -ltp
```



仅显示IPv4 或 IPv6 连接

```shell
ss -tl -f inet 或 ss -tl6
```



列出处在 time-wait 状态的 IPv4 套接字

```shell
ss -t4 state time-wait
```

注意： 状态可以是以下任意一种
```shell
stablished 
yn-sent 
yn-recv 
in-wait-1 
in-wait-2 
ime-wait 
losed 
lose-wait 
ast-ack 
closing 
all – All of the above states 
connected – All the states except for listen and closed 
synchronized – All the connected states except for syn-sent 
bucket – Show states, which are maintained as minisockets, i.e. time-wait and syn-recv. 
big – Opposite to bucket state.
```



显示所有源端口或目的端口为 ssh 的套接字

```shell
ss -at '( dport = :ssh or sport = :ssh )'
```



显示目的端口是443或80的套接字

```shell
ss -nt '( dst :443 or dst :80 )'
```



对地址和端口过滤

```shell
ss -nt dst 103.245.222.184:80 
```



仅过滤端口

```shell
ss -nt dport = :80 
```



显示对方端口号小于100的套接字

```shell
ss -nt dport \< :100
```



显示端口号大于1024的套接字

```shell
sudo ss -nt sport gt :1024
```



显示对方端口是 80的套接字

```shell
sudo ss -nt state connected dport = :80
```


显示TCP连接

```shell
[root@localhost ~]# ss -t -a
State       Recv-Q Send-Q                            Local Address:Port                                Peer Address:Port   
LISTEN      0      0                                             *:3306                                           *:*       
LISTEN      0      0                                             *:http                                           *:*       
LISTEN      0      0                                             *:ssh                                            *:*       
LISTEN      0      0                                     127.0.0.1:smtp                                           *:*       
ESTAB       0      0                                112.124.15.130:42071                              42.156.166.25:http    
ESTAB       0      0                                112.124.15.130:ssh                              121.229.196.235:33398 
```



显示 Sockets 摘要

```shell
[root@localhost ~]# ss -s
Total: 172 (kernel 189)
TCP:   10 (estab 2, closed 4, orphaned 0, synrecv 0, timewait 0/0), ports 5
 
Transport Total     ip        IPv6
*         189       -         -        
RAW       0         0         0        
UDP       5         5         0        
TCP       6         6         0        
INET      11        11        0        
FRAG      0         0         0   
```

列出当前的established, closed, orphaned and waiting TCP sockets



列出所有打开的网络连接端口

```shell
[root@localhost ~]# ss -l
Recv-Q Send-Q                                 Local Address:Port                                     Peer Address:Port   
0      0                                                  *:3306                                                *:*       
0      0                                                  *:http                                                *:*       
0      0                                                  *:ssh                                                 *:*       
0      0                                          127.0.0.1:smtp                                                *:* 
```



查看进程使用的socket

```shell
[root@localhost ~]# ss -pl
Recv-Q Send-Q                                          Local Address:Port                                              Peer Address:Port   
0      0                                                           *:3306                                                         *:*        users:(("mysqld",1718,10))
0      0                                                           *:http                                                         *:*        users:(("nginx",13312,5),("nginx",13333,5))
0      0                                                           *:ssh                                                          *:*        users:(("sshd",1379,3))
0      0                                                   127.0.0.1:smtp                                                         *:*        us
```



找出打开套接字/端口应用程序

```shell
[root@localhost ~]# ss -pl | grep 3306
0      0                            *:3306                          *:*        users:(("mysqld",1718,10))
```



显示所有UDP Sockets  或 ss -aA udp

```shell
[root@localhost ~]# ss -u -a
State       Recv-Q Send-Q                                     Local Address:Port                                         Peer Address:Port   
UNCONN      0      0                                                      *:syslog                                                  *:*       
UNCONN      0      0                                         112.124.15.130:ntp                                                     *:*       
UNCONN      0      0                                            10.160.7.81:ntp                                                     *:*       
UNCONN      0      0                                              127.0.0.1:ntp                                                     *:*       
UNCONN      0      0                                                      *:ntp                                                     *:*
```


<!-- @include: @article-footer.snippet.md -->     
