---
title: 负载均衡算法
category: 微服务
tag:
 - 理论-算法
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,负载均衡算法
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---



## 常见的负载均衡算法

### 轮询法(Round Robin)

将请求按顺序轮流地分配到后端服务器上，它均衡地对待后端的每一台服务器，而不关心服务器实际的连接数和当前的系统负载。

### 加权轮询法(Weight Round Robin)

不同的后端服务器可能机器的配置和当前系统的负载并不相同，因此它们的抗压能力也不相同。给配置高、负载低的机器配置更高的权重，让其处理更多的请；而配置低、负载高的机器，给其分配较低的权重，降低其系统负载，加权轮询能很好地处理这一问题，并将请求顺序且按照权重分配到后端。

### 随机法(Random)

通过系统的随机算法，根据后端服务器的列表大小值来随机选取其中的一台服务器进行访问。由概率统计理论可以得知，随着客户端调用服务端的次数增多，其实际效果越来越接近于平均分配调用量到后端的每一台服务器，也就是轮询的结果。

### 加权随机法(Weight Random)

与加权轮询法一样，加权随机法也根据后端机器的配置，系统的负载分配不同的权重。不同的是，它是按照权重随机请求后端服务器，而非顺序。

### 源地址哈希法(Hash)

源地址哈希的思想是根据获取客户端的IP地址，通过哈希函数计算得到的一个数值，用该数值对服务器列表的大小进行取模运算，得到的结果便是客服端要访问服务器的序号。采用源地址哈希法进行负载均衡，同一IP地址的客户端，当后端服务器列表不变时，它每次都会映射到同一台后端服务器进行访问。

### 最小连接数法(Least Connections)

最小连接数算法比较灵活和智能，由于后端服务器的配置不尽相同，对于请求的处理有快有慢，它是根据后端服务器当前的连接情况，动态地选取其中当前积压连接数最少的一台服务器来处理当前的请求，尽可能地提高后端服务的利用效率，将负责合理地分流到每一台服务器。

## Nginx的5种负载均衡算法

### 轮询法(Round Robin)(默认)

每个请求按时间顺序逐一分配到不同的后端服务器，如果后端服务器down掉，能自动剔除。

### 加权轮询法(Weight Round Robin)- weight

指定轮询几率，weight和访问比率成正比，用于后端服务器性能不均的情况。

例如:

```c++
upstream bakend {  
  server 192.168.0.14 weight=10;  
  server 192.168.0.15 weight=10;  
```

### 源地址哈希法(Hash)- ip_hash

每个请求按访问ip的hash结果分配，这样每个访客固定访问一个后端服务器，可以解决session的问题。

例如:

```c++
upstream bakend {  
  ip_hash;  
  server 192.168.0.14:88;  
  server 192.168.0.15:80;  
}
```



### fair(第三方)

按后端服务器的响应时间来分配请求，响应时间短的优先分配。

```c++
upstream backend {  
  server server1;  
  server server2;  
  fair;  
}
```



### url_hash(第三方)

按访问url的hash结果来分配请求，使每个url定向到同一个后端服务器，后端服务器为缓存时比较有效。

例: 在upstream中加入hash语句，server语句中不能写入weight等其他的参数，hash_method是使用的hash算法。

```c++
upstream backend {  
  server squid1:3128;  
  server squid2:3128;  
  hash $request_uri;  
  hash_method crc32;  
}
```



tips:

```c++
upstream bakend{#定义负载均衡设备的Ip及设备状态  
  ip_hash;  
  server 127.0.0.1:9090 down;  
  server 127.0.0.1:8080 weight=2;  
  server 127.0.0.1:6060;  
  server 127.0.0.1:7070 backup;  
}
```



在需要使用负载均衡的server中增加

```c++
proxy_pass http://bakend/;
```



每个设备的状态设置为:

- down 表示单前的server暂时不参与负载

- weight 默认为1.weight越大，负载的权重就越大。

- max_fails : 允许请求失败的次数默认为1.当超过最大次数时，返回proxy_next_upstream 模块定义的错误

- fail_timeout:max_fails次失败后，暂停的时间。

- backup: 其它所有的非backup机器down或者忙的时候，请求backup机器。所以这台机器压力会最轻。



nginx支持同时设置多组的负载均衡，用来给不用的server来使用。

- client_body_in_file_only: 设置为On，可以讲client post过来的数据记录到文件中用来做debug。

- client_body_temp_path: 设置记录文件的目录，可以设置最多3层目录。

- location: 对URL进行匹配，可以进行重定向或者进行新的代理，负载均衡

 

<!-- @include: @article-footer.snippet.md -->     