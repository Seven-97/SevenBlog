---
title: TCP & HTTP 长连接
category: 计算机基础
tag:
  - 计算机网络
---

> 来源：https://www.xiaolincoding.com/ ，Seven进行了部分补充完善

TCP Keepalive 和 HTTP Keep-Alive 是一个东西吗？

结论：

**这两个完全是两样不同东西**，实现的层面也不同：

- HTTP 的 Keep-Alive，是由**应用层（用户态）** 实现的，称为 HTTP 长连接；
- TCP 的 Keepalive，是由 **TCP 层（内核态）** 实现的，称为 TCP 保活机制；



## HTTP 的 Keep-Alive

HTTP 协议采用的是「请求-应答」的模式，也就是客户端发起了请求，服务端才会返回响应，一来一回。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051104929.png)

由于 HTTP 是基于 TCP 传输协议实现的，客户端与服务端要进行 HTTP 通信前，需要先建立 TCP 连接，然后客户端发送 HTTP 请求，服务端收到后就返回响应，至此「请求-应答」的模式就完成了，随后就会释放 TCP 连接。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051105899.png)

如果每次请求都要经历这样的过程：建立 TCP -> 请求资源 -> 响应资源 -> 释放连接，那么此方式就是 **HTTP 短连接**，如下图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051105164.png)



因此，能不能在第一个 HTTP 请求完后，先不断开 TCP 连接，让后续的 HTTP 请求继续使用此连接？

而HTTP 的 Keep-Alive 就是实现了这个功能，可以使用同一个 TCP 连接来发送和接收多个 HTTP 请求/应答，避免了连接建立和释放的开销，这个方法称为 **HTTP 长连接**。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051105811.png)



HTTP 长连接的特点是，只要任意一端没有明确提出断开连接，则保持 TCP 连接状态。



### 开启/关闭

而HTTP 长连接在 HTTP 1.0 中默认是关闭的，如果浏览器要开启 Keep-Alive，它必须在请求的包头中添加：

```text
Connection: Keep-Alive
```

然后当服务器收到请求，作出回应的时候，它也添加一个头在响应中：

```text
Connection: Keep-Alive
```

这样做，连接就不会中断，而是保持连接。当客户端发送另一个请求时，它会使用同一个连接。这一直继续到客户端或服务器端提出断开连接。



**从 HTTP 1.1 开始， 就默认是开启了 Keep-Alive**，如果要关闭 Keep-Alive，需要在 HTTP 请求的包头里添加：

```text
Connection:close
```

现在大多数浏览器都默认是使用 HTTP/1.1，所以 Keep-Alive 都是默认打开的。一旦客户端和服务端达成协议，那么长连接就建立好了。



HTTP 长连接不仅仅减少了 TCP 连接资源的开销，而且这给 **HTTP 流水线**技术提供了可实现的基础。

所谓的 HTTP 流水线，是**客户端可以先一次性发送多个请求，而在发送过程中不需先等待服务器的回应**，可以减少整体的响应时间。

举例来说，客户端需要请求两个资源。以前的做法是，在同一个 TCP 连接里面，先发送 A 请求，然后等待服务器做出回应，收到后再发出 B 请求。HTTP 流水线机制则允许客户端同时发出 A 请求和 B 请求。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051107828.png)

但是**服务器还是按照顺序响应**，先回应 A 请求，完成后再回应 B 请求。

而且要等服务器响应完客户端第一批发送的请求后，客户端才能发出下一批的请求，也就说如果服务器响应的过程发生了阻塞，那么客户端就无法发出下一批的请求，此时就造成了 [队头阻塞](https://www.seven97.top/cs-basics/network/03-http1-statuscodeandheader.html#http-1-1-%E7%9A%84%E6%80%A7%E8%83%BD) 的问题，HTTP的对头阻塞问题在[HTTP2](https://www.seven97.top/cs-basics/network/03-http3-http2and3.html#http-2%E7%9A%84%E4%BC%98%E5%8C%96) 进行了解决



### 保活时间

但是，如果使用了 HTTP 长连接，如果客户端完成一个 HTTP 请求后，就不再发起新的请求，此时这个 TCP 连接一直占用着不是挺浪费资源的吗？

对没错，所以为了避免资源浪费的情况，web 服务软件一般都会提供 `keepalive_timeout` 参数，用来指定 HTTP 长连接的超时时间。

比如设置了 HTTP 长连接的超时时间是 60 秒，web 服务软件就会**启动一个定时器**，如果客户端在完后一个 HTTP 请求后，在 60 秒内都没有再发起新的请求，**定时器的时间一到，就会触发回调函数来释放该连接。**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051114464.png)



## TCP 的 Keepalive

TCP 的 Keepalive 这东西其实就是 **TCP 的保活机制**。

如果两端的 TCP 连接一直没有数据交互，达到了触发 TCP 保活机制的条件，那么内核里的 TCP 协议栈就会发送探测报文。

- 如果对端程序是正常工作的。当 TCP 保活的探测报文发送给对端, 对端会正常响应，这样 **TCP 保活时间会被重置**，等待下一个 TCP 保活时间的到来。
- 如果对端主机宕机（*注意不是进程崩溃，进程崩溃后操作系统在回收进程资源的时候，会发送 FIN 报文，而主机宕机则是无法感知的，所以需要 TCP 保活机制来探测对方是不是发生了主机宕机*），或对端由于其他原因导致报文不可达。当 TCP 保活的探测报文发送给对端后，石沉大海，没有响应，连续几次，达到保活探测次数后，**TCP 会报告该 TCP 连接已经死亡**。

所以，TCP 保活机制可以在双方没有数据交互的情况，通过探测报文，来确定对方的 TCP 连接是否存活，这个工作是在内核完成的。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051120690.png)



### 开启/关闭

注意，应用程序若想使用 TCP 保活机制需要通过 socket 接口设置 `SO_KEEPALIVE` 选项才能够生效，如果没有设置，那么就无法使用 TCP 保活机制。

如果开启了 TCP 保活，需要考虑以下几种情况：

- 第一种，对端程序是正常工作的。当 TCP 保活的探测报文发送给对端, 对端会正常响应，这样 **TCP 保活时间会被重置**，等待下一个 TCP 保活时间的到来。
- 第二种，对端主机宕机并重启。当 TCP 保活的探测报文发送给对端后，对端是可以响应的，但由于没有该连接的有效信息，**会产生一个 RST 报文**，这样很快就会发现 TCP 连接已经被重置。
- 第三种，是对端主机宕机（注意不是进程崩溃，进程崩溃后操作系统在回收进程资源的时候，会发送 FIN 报文，而主机宕机则是无法感知的，所以需要 TCP 保活机制来探测对方是不是发生了主机宕机），或对端由于其他原因导致报文不可达。当 TCP 保活的探测报文发送给对端后，石沉大海，没有响应，连续几次，达到保活探测次数后，**TCP 会报告该 TCP 连接已经死亡**。



### 保活时间

在linux内核内，有对应的参数设置保活时间、保活探测的次数，保活探测的时间间隔，以下为默认值：

```shell
net.ipv4.tcp_keepalive_time=7200
net.ipv4.tcp_keepalive_intvl=75  
net.ipv4.tcp_keepalive_probes=9
net.ipv4.tcp_retries1 = 3 //
net.ipv4.tcp_retries2 = 15 //尝试发送的间隔
```

- tcp_keepalive_time=7200：表示保活时间是 7200 秒（2小时），也就 2 小时内如果没有任何连接相关的活动，则会启动保活机制
- tcp_keepalive_intvl=75：在开始发送 Keepalive 报文后，两个 Keepalive 报文之间的时间间隔，以秒为单位。表示每次检测间隔 75 秒；
- tcp_keepalive_probes=9：如果对端没有响应任何一个 Keepalive 报文，内核将发送多次重试，直到达到设定的次数为止。表示检测 9 次无响应，认为对方是不可达的，从而中断本次的连接。
- net.ipv4.tcp_retries1 = 3 ： TCP 连接建立过程中的重试次数，这个参数通常用于控制 SYN（同步）包的重传次数，即当一个 TCP 连接请求（SYN）被发送但未收到回应时，内核将尝试重新发送 SYN 包的次数。
- net.ipv4.tcp_retries2 = 15：控制在连接建立之后的重试次数，当一个 TCP 连接已经建立，但在通信过程中出现了数据包丢失或连接中断时，内核将尝试重新发送数据或恢复连接的次数。



也就是说在 Linux 系统中，最少需要经过 2 小时 11 分 15 秒才可以发现一个「死亡」连接。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051129319.webp)



## 总结

TCP `keepalive` 和 HTTP 长连接实际上是两个不同层面的机制，它们在通信协议栈中处于不同的层级：

1. TCP `keepalive` 是在传输层提供的一种机制，它可以帮助检测死连接（即长时间没有数据交换的连接）。如果开启了 TCP `keepalive`，操作系统会在指定的间隔发送 `keepalive` 探针（probe）以检查连接是否仍然有效。如果经过几次尝试后仍无法得到响应，系统会认为连接已经断开，并通知应用程序。
2. HTTP 长连接，通常指的是 HTTP 1.1 的 "Connection: keep-alive" 头部，它是在应用层提供的一种机制。这个机制的目的是允许在一个 TCP 连接上发送和接收多个 HTTP 请求和响应，而不是每个请求/响应都重新建立一个新的连接。这可以减少建立和关闭连接的开销，提高通信效率。



到这里可能会有疑问：

如果tcp未开启keepalive，而应用层如http开启了长连接，会发生什么？

- TCP 连接不会自动发送 `keepalive` 探针来检查连接是否存活，这意味着如果连接在一个方向上长时间没有数据传输，TCP 协议本身不会主动去验证对方是否还在。
- **HTTP 层的长连接仍然有效**，应用程序可以在同一个 TCP 连接上发送多个 HTTP 请求，而不需要每次都建立新的连接。
- 如果网络中断或对端崩溃导致连接实际上已经断开，但因为没有 TCP `keepalive` 探针，应用可能不会立即意识到这一点。这样，应用可能会在一个已经死亡的连接上尝试发送 HTTP 请求，这将导致发送失败，并且可能会有一个超时的延迟，直到应用程序的 TCP 栈检测到发送失败。
- 在某些情况下，如 NAT 超时，如果长时间没有数据流动，NAT 设备可能会丢弃表项。由于没有 TCP `keepalive` 探针，应用程序在尝试发送下一个 HTTP 请求时可能遇到连接问题。

综上所述，即使 HTTP 层启用了长连接，在没有相应的传输层（TCP 层）`keepalive` 检测的情况下，仍然可能遇到连接可靠性问题。因此，在依赖长连接的应用中，最佳实践是同时在 TCP 层启用 `keepalive`，以确保连接的持久性和可靠性。



## 长连接的黑洞问题

### 问题描述

假设OS的配置参数如下：

```shell
net.ipv4.tcp_keepalive_intvl = 30
net.ipv4.tcp_keepalive_probes = 5
net.ipv4.tcp_keepalive_time = 10
net.ipv4.tcp_retries1 = 3
net.ipv4.tcp_retries2 = 15
```

关键在于参数net.ipv4.tcp_retries2 = 15，这意味着TCP长连接在发送包的时候，超时重传的最大次数。

不过 tcp_retries2 设置了 15 次，并不代表 TCP 超时重传了 15 次才会通知应用程序终止该 TCP 连接，**内核会根据 tcp_retries2 设置的值，计算出一个 timeout**（*如果 tcp_retries2 =15，那么计算得到的 timeout = 924600 ms*），**如果重传间隔超过这个 timeout，则认为超过了阈值，就会停止重传，然后就会断开 TCP 连接**。所以我们经常看到业务需要15分钟左右才恢复。而这个问题存在所有TCP长连接中。

> 在发生超时重传的过程中，每一轮的超时时间（RTO）都是**倍数增长**的，比如如果第一轮 RTO 是 200 毫秒，那么第二轮 RTO 是 400 毫秒，第三轮 RTO 是 800 毫秒，以此类推。
>
> 而 RTO 是基于 RTT（一个包的往返时间） 来计算的，如果 RTT 较大，那么计算出来的 RTO 就越大，那么经过几轮重传后，很快就达到了上面的 timeout 值了。
>
> 举个例子，如果 tcp_retries2 =15，那么计算得到的 timeout = 924600 ms，如果重传总间隔时长达到了 timeout 就会停止重传，然后就会断开 TCP 连接：
>
> - 如果 RTT 比较小，那么 RTO 初始值就约等于下限 200ms，也就是第一轮的超时时间是 200 毫秒，由于 timeout 总时长是 924600 ms，表现出来的现象刚好就是重传了 15 次，超过了 timeout 值，从而断开 TCP 连接
>
> - 如果 RTT 比较大，假设 RTO 初始值计算得到的是 1000 ms，也就是第一轮的超时时间是 1 秒，那么根本不需要重传 15 次，重传总间隔就会超过 924600 ms。
>
>   
>
>   最小 RTO 和最大 RTO 是在 Linux 内核中定义好了：
>
>   ```c
>   #define TCP_RTO_MAX ((unsigned)(120*HZ))
>   #define TCP_RTO_MIN ((unsigned)(HZ/5))
>   ```
>
>   Linux 2.6+ 使用 1000 毫秒的 HZ，因此`TCP_RTO_MIN`约为 200 毫秒，`TCP_RTO_MAX`约为 120 秒。
>
>   如果`tcp_retries`设置为`15`，且 RTT 比较小，那么 RTO 初始值就约等于下限 200ms，这意味着**它需要 924.6 秒**才能将断开的 TCP 连接通知给上层（即应用程序），每一轮的 RTO 增长关系如下表格：
>
>   ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051152393.png)



如果Server突然消失(宕机、断网，来不及发RST)客户端如果正在发东西给Server就会遵循TCP重传逻辑不断地TCP retran，如果一直收不到Server的ack，大约重传15次，900秒左右。就存在了900秒的长连接黑洞问题



### 解决方案

#### **业务方**

一般来说，业务方要对自己的请求超时时间有控制和兜底，不能任由一个请求长时间Hang在那里。

比如JDBC URL支持设置SocketTimeout、ConnectTimeout，业务方要设置这些值，不设置就要900多秒后才恢复。

##### SocketTimeout

只要是连接有机会设置SocketTimeout就一定要设置，具体值可以根据能接受的慢查询来设置；分析、AP类的请求可以设置大一点。

**最重要的：任何业务只要用到了TCP长连接一定要配置一个恰当的SocketTimeout**，比如Jedis是连接池模式，底层超时之后，会销毁当前连接，下一次重新建连，就会连接到新的切换节点上去并恢复。



##### RFC 5482 TCP_USER_TIMEOUT

RFC 5482[1]中增加了TCP_USER_TIMEOUT这个配置，通常用于定制当TCP网络连接中出现数据传输问题时，可以等待多长时间前释放网络资源。

TCP_USER_TIMEOUT是一个整数值，它指定了当TCP连接的数据包在发送后多长时间内未被确认（即没有收到ACK），TCP连接会考虑释放这个连接。

打个比方，设置TCP_USER_TIMEOUT后，应用程序就可以指定说：“如果在30秒内我发送的数据没有得到确认，那我就认定网络连接出了问题，不再尝试继续发送，而是直接断开连接。”这对于确保连接质量和维护用户体验是非常有帮助的。

在Linux中，可以使用setsockopt函数来设置某个特定socket的 TCP_USER_TIMEOUT值：

```c
int timeout = 30000; // 30 seconds
setsockopt(sock, IPPROTO_TCP, TCP_USER_TIMEOUT, (char *)&timeout, sizeof(timeout));
```

在这行代码中，sock是已经established的TCP socket，我们将该socket的 TCP_USER_TIMEOUT设置为30000毫秒，也就是30秒。如果设置成功，这个TCP连接在发送数据包后30秒内如果没有收到ACK确认，将开始进行TCP连接的释放流程。

TCP_USER_TIMEOUT相较SocketTimeout可以做到更精确(不影响慢查询)，SocketTimeout超时是不区分ACK还是请求响应时间的，但是TCP_USER_TIMEOUT要求下层的API、OS都支持。比如JDK不支持TCP_USER_TIMEOUT，但是Netty框架自己搞了Native 来实现对TCP_USER_TIMEOUT以及其它OS参数的设置，在这些基础上Redis的Java客户端lettuce依赖了Netty，所以也可以设置TCP_USER_TIMEOUT。



##### ConnectTimeout

这个值是针对新连接创建超时时间设置，一般设置3-5秒就够长了。



##### 连接池

建议参考这篇 [数据库连接池配置推荐](https://www.seven97.top/database/mysql/04-tuning-databaseconnectionpoolconfiguration)这篇里的很多建议也适合业务、应用等，你把数据库看成一个普通服务就好理解了。





#### **OS兜底**

假如业务是一个AP查询/一次慢请求，一次查询/请求就是需要半个小时，将 SocketTimeout设置太小影响正常的查询，那么可以将如下OS参数改小，从OS层面进行兜底。

```c
net.ipv4.tcp_retries2 = 8
net.ipv4.tcp_syn_retries = 4
```



##### keepalive

keepalive默认7200秒太长了，建议改成20秒，可以在OS镜像层面固化，然后各个业务可以设置自己的值；

如果一条连接限制超过900秒LVS就会Reset这条连接，但是我们将keepalive设置小于900秒的话，即使业务上一直闲置，因为有keepalive触发心跳包，让LVS不至于Reset，这也就避免了当业务取连接使用的时候才发现连接已经不可用被断开了，往往这个时候业务抛错误的时间很和真正Reset时间还差了很多，不好排查。

在触发TCP retransmission后会停止keepalive探测。













<!-- @include: @article-footer.snippet.md -->     





