---
title: 底层 - Redis是如何建立连接和处理命令的
category: 数据库
tag:
 - Redis
 - 缓存
head:
  - - meta
    - name: keywords
      content: redis,底层源码
  - - meta
    - name: description
      content: 全网最全的Redis知识点总结，让天下没有难学的八股文！
---

今天来讲讲 Redis 的请求监听，通俗点说，就是Redis是如何监听客户端发出的set、get等命令的。

### 基础架构

众所周知，Redis 是单进程单线程架构，虽然是单进程单线程，但是Redis的性能却毫不逊色，能轻松应对一般的高并发场景，那么Redis究竟是施了什么魔法呢？

其实 Redis 的原理和 Nginx 差不多，都利用了 [**IO 多路复用**](https://www.seven97.top/cs-basics/operating-system/selectpollepoll.html)来提高处理能力，所谓多路复用，就是一个线程可以同时处理多个IO操作，当 某个 IO 操作 Ready 时，操作系统会主动通知进程。使用 IO 多路复用，我们可以使用 epoll、kqueue、select，API 都差不多。

与Redis 不同的是，Nginx 并不是单进程架构，而是采用了多进程来处理请求。Nginx跑起来后会先启动Master进程，Master进程接着启动多个 Worker 进程，每个Worker 进程都会参与请求的监听和处理。这样可以充分发挥CPU的多核特性。

虽然 Redis 是单进程单线程，不能利用多核，但同样也避免了多进程的并发问题，也就没有了锁带来的开销。

## 源码探究

Redis 入口是 server.c 中的 main（）方法，main（）中会调用 initServer（）初始化 Redis 服务。


### 初始化 Redis 服务

Redis 为了进行事件监听，特地封装了一个 **struct：aeEventLoop**，定义在 ae.c 中。

**为什么需要 aeEventLoop 呢**？因为 Redis 针对于 epoll、evport、select 还有 kqueue 的 API做了封装，分别为 ae_epoll、ae_evport、ae_select 和 ae_kqueue。但是 Redis 在使用监听的时候，统一调用的都是 aeEventLoop 中的方法。ae.c 在预处理阶段使用了条件编译，这样便可以选择平台支持的 API 来使用多路复用。

```
#ifdef HAVE_EVPORT
#include "ae_evport.c"
#else
    #ifdef HAVE_EPOLL
    #include "ae_epoll.c"
    #else
        #ifdef HAVE_KQUEUE
        #include "ae_kqueue.c"
        #else
        #include "ae_select.c"
        #endif
    #endif
#endif
```

这里Redis 选择了 Mac 平台支持的 kqueue 使用多路复用，所以后面源码分析的时候，我都会基于 kqueue 来讲解。

#### 创建内核事件队列

initServer（）第一件做的事情就是为当前服务创建了一个 aeEventLoop，后续的所有命令监听，都是基于 aeEventLoop 来做的。

```c
server.el = aeCreateEventLoop(server.maxclients+CONFIG_FDSET_INCR);
```

aeCreateEventLoop（）内部会调用 aeApiCreate（），选择性的调用epoll、select 或者 kqueue 的 API，创建事件监听。这里拿 kqueue 来举例子，我们可以看下 ae_kqueue.c 中的实现。

```c
static int aeApiCreate(aeEventLoop *eventLoop) {
    aeApiState *state = zmalloc(sizeof(aeApiState));
    ......
    state->events = zmalloc(sizeof(struct kevent)*eventLoop->setsize);
    state->kqfd = kqueue();
    ......
    eventLoop->apidata = state;
    return 0;
}
```

上面创建了一个 aeApiState，并且调用了 kqueue 的API：kqueue（）：

```c
// 创建一个内核消息队列，返回队列描述符
int  kqueue(void);
```

这里创建了一个内核事件队列，并将该队列的文件描述符存入了 state->kqfd 中。

#### 创建套接字，监听端口

**要想监听客户端的命令，首先要做的就是监听端口，initServer（）在创建好 aeEventLoop 后，就会开始执行端口监听操作：**

```c
listenToPort(server.port,server.ipfd,&server.ipfd_count) == C_ERR)
```

其中 server.ipfd 是个数组，用来存放端口监听后创建的描述符。为什么是个数组？因为Redis 可以同时 bind 多个 IP 地址，所以listenToPort（）内部会遍历配置文件中配置的多个IP地址，一次进行监听，并将创建的描述符存入 server.ipfd [] 中。

#### 注册监听到内核队列

监听好端口后，initServer（）会遍历刚刚创建好的套接字描述符 ipfd []，依次注册到事件监听中。

```c
aeCreateFileEvent(server.el, server.ipfd[j], AE_READABLE, acceptTcpHandler,NULL)
```

我们进入aeCreateFileEvent（）中看看。

```c
int aeCreateFileEvent(aeEventLoop *eventLoop, int fd, int mask, aeFileProc *proc, void *clientData){
    ......
    aeFileEvent *fe = &eventLoop->events[fd];

    if (aeApiAddEvent(eventLoop, fd, mask) == -1)
        return AE_ERR;
    ......
    return AE_OK;
}
```

上面主要是初始化了 aeFileEvent，存放在 eventLoop 中的events [] 内。并且调用 aeApiAddEvent（），将监听端口的描述符注册到（1）中创建的内核队列中。对于 kqueue来说，这里调用的是 kqueue 的 EV_SET（）：

```c
static int aeApiAddEvent(aeEventLoop *eventLoop, int fd, int mask) {
    aeApiState *state = eventLoop->apidata;
    struct kevent ke;

    if (mask & AE_READABLE) {
        EV_SET(&ke, fd, EVFILT_READ, EV_ADD, 0, 0, NULL);
        if (kevent(state->kqfd, &ke, 1, NULL, 0, NULL) == -1) return -1;
    }
    if (mask & AE_WRITABLE) {
        EV_SET(&ke, fd, EVFILT_WRITE, EV_ADD, 0, 0, NULL);
        if (kevent(state->kqfd, &ke, 1, NULL, 0, NULL) == -1) return -1;
    }
    return 0;
}
```

  

### 启动事件监听循环，监听请求

经过了上面的一系列初始化操作，Redis 就会正式进入事件监听的循环中，即 aeMain（）。

```c
void aeMain(aeEventLoop *eventLoop) {
    eventLoop->stop = 0;
    while (!eventLoop->stop) {
        if (eventLoop->beforesleep != NULL)
            eventLoop->beforesleep(eventLoop);
        aeProcessEvents(eventLoop, AE_ALL_EVENTS|AE_CALL_AFTER_SLEEP);
    }
}
```

这段代码看起来很简单，只要事件监听没有停止，Redis 就会一直循环调用aeProcessEvents（）处理事件。

aeProcessEvents（）中的核心逻辑就是两步：

#### 调用 aeApiPoll （）来等待接收事件。

```c
numevents = aeApiPoll(eventLoop, tvp);
```

我们看下使用kqueue时，ae_kqueue.c 中的实现。

```c
static int aeApiPoll(aeEventLoop *eventLoop, struct timeval *tvp) {
    aeApiState *state = eventLoop->apidata;
    int retval, numevents = 0;
    ......
    retval = kevent(state->kqfd, NULL, 0, state->events, eventLoop->setsize,
                        NULL);
    ......
    numevents = retval;
    ......
    return numevents;
}
```

这里调用的是 kevent（）来接收事件。kevent（）描述如下：

```c
// 用途：注册\反注册 监听事件，等待事件通知
// kq，上面创建的消息队列描述符
// changelist，需要注册的事件
// changelist，changelist数组大小
// eventlist，内核会把返回的事件放在该数组中
// nevents，eventlist数组大小
// timeout，等待内核返回事件的超时事件，NULL 即为无限等待
int  kevent(int kq, 
         const struct kevent *changelist, int nchanges,
         struct kevent *eventlist, int nevents,
         const struct timespec *timeout);
```

#### 遍历返回的就绪事件数组，处理事件。

```c
for (j = 0; j < numevents; j++) {
    aeFileEvent *fe = &eventLoop->events[eventLoop->fired[j].fd];
    int mask = eventLoop->fired[j].mask;
    int fd = eventLoop->fired[j].fd;
    int fired = 0; /* Number of events fired for current fd. */

    // 处理可读事件  
    if (!invert && fe->mask & mask & AE_READABLE) {
        fe->rfileProc(eventLoop,fd,fe->clientData,mask);
        fired++;
    }

    // 处理可写事件
    if (fe->mask & mask & AE_WRITABLE) {
        if (!fired || fe->wfileProc != fe->rfileProc) {
            fe->wfileProc(eventLoop,fd,fe->clientData,mask);
            fired++;
        }
    }

    if (invert && fe->mask & mask & AE_READABLE) {
        if (!fired || fe->wfileProc != fe->rfileProc) {
            fe->rfileProc(eventLoop,fd,fe->clientData,mask);
            fired++;
        }
    }

    processed++;
}
```

每次遍历，都会先从eventLoop 的 events[] 中，根据描述符取出对应的 aeFileEvent，接着调用 aeFileEvent 的 rfileProc（）或者 wfileProc（）来处理事件，那么 aeFileEvent 的 fileProc（）时从哪来的呢？

记得 initServer（）时，Redis 调用 aeCreateFileEvent（）将监听端口后的套接字描述符注册到内核事件队列中吗？

```c
aeCreateFileEvent(server.el, server.ipfd[j], AE_READABLE, acceptTcpHandler,NULL) == AE_ERR)
```

此时传入的 acceptTcpHandler（）就作为 rFileProc ，设置到此次创建的 aeFileEvent 之中 了。**acceptTcpHandler（）的作用就是处理新客户端的连接请求，与新客户端建立 TCP 连接。**

**在 Redis 服务端调用 accept（）与客户端建立连接后，会创建一个 client 对象，用来描述该连接。接着 Redis 会将accept（）返回的新连接的描述符再次调用aeCreateFileEvent（），注册到内核事件队列中，用来接收该连接后续的事件，说白了，就是监听后续该客户端的 set、get 等请求：**

```c
aeCreateFileEvent(server.el,fd,AE_READABLE, readQueryFromClient, c) == AE_ERR
```

到现在读者应该知道aeCreateFileEvent（）的第四个参数时用来干嘛的了，就是监听到事件后用来处理事件的。**可见用来处理客户端命令请求的函数是：readQueryFromClient（）。**

  

### 处理客户端命令

现在我们知道了用来处理客户端命令请求的函数是 readQueryFromClient（），由函数名就能看出它的意义：读取客户端的查询请求。

readQueryFromClient（）调用了 processInputBuffer（）从缓冲区读取并解析请求内容，之后 processInputBuffer（）便会**调用 processCommand（）处理命令。**

#### 查询命令

processCommand（）第一步要做的就是根据请求找到对应的命令。Redis 的所有命令都预先定义在 server.c 的 redisCommandTable [] 中：

```c
struct redisCommand redisCommandTable[] = {
    {"module",moduleCommand,-2,"as",0,NULL,0,0,0,0,0},
    {"get",getCommand,2,"rF",0,NULL,1,1,1,0,0},
    {"set",setCommand,-3,"wm",0,NULL,1,1,1,0,0},
    {"setnx",setnxCommand,3,"wmF",0,NULL,1,1,1,0,0},
    ......
}
```

redisCommandTable [] 中定义了每个命令对应的处理函数，如set：setCommand， get：getCommand。

Redis 在启动后会调用 server.c 的 initServerConfig（）方法用来初始化配置，其中就调用了 populateCommandTable（）。populateCommandTable（）的作用就是遍历redisCommandTable []，将所有的命令以 name 作为 key，以自身 redisCommand 作为value，存入到一个字典中：**server.commands**。

processCommand（）调用了lookupCommand（）查询命令。

```c
c->cmd = c->lastcmd = lookupCommand(c->argv[0]->ptr);
```

那么该去哪找对应的命令呢？当然就是上面所说这个字典中。

```c
struct redisCommand *lookupCommand(sds name) {
	return dictFetchValue(server.commands, name);
}
```

#### 执行命令

找到请求对应的命令后，该干嘛？想都不用想，当然是去执行命令了。

```c
if (c->flags & CLIENT_MULTI &&
    c->cmd->proc != execCommand && c->cmd->proc != discardCommand &&
    c->cmd->proc != multiCommand && c->cmd->proc != watchCommand)
{
    queueMultiCommand(c);
    addReply(c,shared.queued);
} else {
    call(c,CMD_CALL_FULL);
    c->woff = server.master_repl_offset;
    if (listLength(server.ready_keys))
        handleClientsBlockedOnKeys();
}
```

**上面这段代码的门道就在于：**

- 如果处于事务中，就将命令先放入队列中，不执行。
    
- 如果不在事务中，就直接调用call（）执行命令。
    

call（）中会调用命令对应的处理函数：

```c
c->cmd->proc(c);
```

如 set 命令对应的处理函数就是 t_string.c 中的 setCommand（）。

## 总结

一步一步跟下来，对于网络请求这块，Redis 和 Nginx 确实太像了，都是用了IO多路复用。

**Redis 监听命令主要就是下面几个步骤。**

1. 创建套接字，监听端口，也就是监听新客户端的建立连接请求。
2. 创建内核事件队列，并注册上述的套接字描述符到队列中。
3. 开启循环，监听队列中的就绪事件。
4. 当端口有新事件时，调用 accept（）与新客户端建立连接，并再次将新连接的描述符注册到内核事件队列中，监听该TCP连接上的事件。
5. 当与客户端的TCP连接上有新事件时，调用对应的事件处理函数，从该连接上读取并解析请求，并执行请求对应的命令。



<!-- @include: @article-footer.snippet.md -->     

