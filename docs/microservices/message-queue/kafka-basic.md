---
title: Kafka - 入门
category: 微服务
tag:
  - Kafka
  - 消息队列
head:
  - - meta
    - name: keywords
      content: 微服务,分布式,高可用,消息队列,Kafka,Kafka安装部署,Kafka基础命令
  - - meta
    - name: description
      content: 全网最全的微服务、分布式知识点总结，让天下没有难学的八股文！
---
> 来源：尚硅谷，Seven对其进行了补充完善


## Kafka基础架构

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182232939.gif)

在Kafka2.8版本前，Zookeeper的Consumer文件中存放消息被消费的记录（offset）

在Kafka2.8版本走，消息被消费的记录（offset）存放在Kafka中。

（1）Producer：消息生产者，就是向 Kafka broker 发消息的客户端。

（2）Consumer：消息消费者，向 Kafka broker 取消息的客户端。

（3）Consumer Group（CG）：消费者组，由多个 consumer 组成。消费者组内每个消费者负责消费不同分区的数据，一个分区只能由一个组内消费者消费；消费者组之间互不影响。所有的消费者都属于某个消费者组，即消费者组是逻辑上的一个订阅者。

（4）Broker：一台 Kafka 服务器就是一个 broker。一个集群由多个 broker 组成。一个broker 可以容纳多个 topic。

（5）Topic：可以理解为一个队列，生产者和消费者面向的都是一个 topic。

（6）Partition：为了实现扩展性，一个非常大的 topic 可以分布到多个 broker（即服务器）上，一个 topic 可以分为多个 partition，每个 partition 是一个有序的队列。

（7）Replica：副本。一个 topic 的每个分区都有若干个副本，一个 Leader 和若干个Follower。

（8）Leader：每个分区多个副本的“主”，生产者发送数据的对象，以及消费者消费数据的对象都是 Leader。

（9）Follower：每个分区多个副本中的“从”，实时从 Leader 中同步数据，保持和Leader 数据的同步。Leader 发生故障时，某个 Follower 会成为新的 Leader。





## 安装部署

### 集群规划

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182233888.gif)

### 集群部署

0）官方下载地址：http://kafka.apache.org/downloads.html

1）解压安装包

```shell
tar -zxvf kafka_2.12-3.0.0.tgz -C /opt/module/
```

2)修改解压后的文件名称

```shell
mv kafka_2.12-3.0.0/ kafka
```

3）进入到/opt/module/kafka 目录，修改配置文件

```shell
cd config/
vim server.properties
```

输入以下内容：

```shell
#broker 的全局唯一编号，不能重复，只能是数字。
broker.id=0
#处理网络请求的线程数量
num.network.threads=3
#用来处理磁盘 IO 的线程数量
num.io.threads=8
#发送套接字的缓冲区大小
socket.send.buffer.bytes=102400
#接收套接字的缓冲区大小
socket.receive.buffer.bytes=102400
#请求套接字的缓冲区大小
socket.request.max.bytes=104857600
#kafka 运行日志(数据)存放的路径，路径不需要提前创建，kafka 自动帮你创建，可以配置多个磁盘路径，路径与路径之间可以用"，"分隔
log.dirs=/opt/module/kafka/datas
#topic 在当前 broker 上的分区个数
num.partitions=1
#用来恢复和清理 data 下数据的线程数量
num.recovery.threads.per.data.dir=1
#每个 topic 创建时的副本数，默认时 1 个副本
offsets.topic.replication.factor=1
#segment 文件保留的最长时间，超时将被删除
log.retention.hours=168
#每个 segment 文件的大小，默认最大 1G
log.segment.bytes=1073741824
```



4）分发安装包

```shell
 xsync kafka/
```



5）分别在 hadoop103 和 hadoop104 上修改配置文件/opt/module/kafka/config/server.properties中的 broker.id=1、broker.id=2

注：broker.id 不得重复，整个集群中唯一。

```shell
vim kafka/config/server.properties
```

修改：

```shel
#The id of the broker. This must be set to a unique integer for each broker.
broker.id=1
```

6）配置环境变量

（1）在/etc/profile.d/my_env.sh 文件中增加 kafka 环境变量配置

```shell
sudo vim /etc/profile.d/my_env.sh
```

```shell
#KAFKA_HOME
export KAFKA_HOME=/opt/module/kafka
export PATH=$PATH:$KAFKA_HOME/bin
```



刷新环境变量

```shell
source /etc/profile
```



### 启动

（1） Zookeeper启动 (默认守护进程)

```shell
./zkServer.sh start
```



Zookeeper状态

```shell
./zkServer.sh status
```



Zookeeper停止

```shell
./zkServer.sh stop
```



Zookeeper客户端-常⽤命令

```shell
./zkCli.sh –server ip:port  //连接ZooKeeper服务端
quit              //断开连接
```



(2) 启动Kafka

Kafka 守护方式 (环境变量配置前提下)

```shell
kafka-server-start.sh -daemon /home/environment/kafka/config/server.properties
```

 

Kafka关闭

```shell
kafka-server-stop.sh
```



注意：停止 Kafka 集群时，一定要等 Kafka 所有节点进程全部停止后再停止 Zookeeper集群。因为 Zookeeper 集群当中记录着 Kafka 集群相关信息，Zookeeper 集群一旦先停止，Kafka 集群就没有办法再获取停止进程的信息，只能手动杀死 Kafka 进程了。

## Kafka命令行操作

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182238618.gif)

### 主题命令行操作

查看操作主题命令参数

```shell
bin/kafka-topics.sh
```



![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182238777.jpg)

2）查看当前服务器中的所有 topic

```shell
kafka-topics.sh --bootstrap-server 47.106.86.64:9092 --list
```

3）创建 first topic

```shell
kafka-topics.sh --bootstrap-server 47.106.86.64:9092 --create --partitions 1 --replication-factor 3 --topic first
```

选项说明：

```
--topic 定义 topic 名
--replication-factor 定义副本数
--partitions 定义分区数
```



4）查看 first 主题的详情

```shell
kafka-topics.sh --bootstrap-server 47.106.86.64:9092 --alter --topic first --partitions 3
```



5）修改分区数（注意：分区数只能增加，不能减少）

 ```shell
 kafka-topics.sh --bootstrap-server 47.106.86.64:9092 --alter --topic first --partitions 3
 ```



6）再次查看 first 主题的详情

```shell
kafka-topics.sh --bootstrap-server 47.106.86.64:9092 --describe --topic first
```



7）删除 topic(需要配置信息)

```shell
kafka-topics.sh --bootstrap-server 47.106.86.64:9092 --delete --topic first
```



### 生产者命令行操作

1）查看操作生产者命令参数

连接kafka生产者

```shell
kafka-console-producer.sh --bootstrap-server 47.106.86.64:9092 --topic first
```



参数 描述

```shell
--bootstrap-server <String: server toconnect to> 连接的 Kafka Broker 主机名称和端口号。
--topic <String: topic> 操作的 topic 名称。
```



2）发送消息

```shell
bin/kafka-console-producer.sh --
bootstrap-server hadoop102:9092 --topic first
hello world
Hi HI
```

 

### 消费者命令行操作 

1）查看操作消费者命令参数

连接kafka消费者

```shell
kafka-console-consumer.sh
```

参数 描述

```shell
–bootstrap-server <String: server toconnect to> 连接的 Kafka Broker 主机名称和端口号。
–topic <String: topic> 操作的 topic 名称。
–from-beginning 从头开始消费。
–group <String: consumer group id> 指定消费者组名称。
```



2）消费消息

（1）消费 first 主题中的数据。

```shell
kafka-console-consumer.sh --bootstrap-server 47.106.86.64:9092 --topic first
```



（2）把主题中所有的数据都读取出来（包括历史数据）。

```shell
kafka-console-consumer.sh --bootstrap-server 47.106.86.64:9092 --from-beginning --topic first
```







<!-- @include: @article-footer.snippet.md -->     







