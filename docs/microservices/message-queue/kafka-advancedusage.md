---
title: Kafka - 高级使用
category: 微服务
tag:
  - Kafka
---



> 来源：尚硅谷，Seven对其进行了补充完善



## Kafka 生产者

### 生产者消息发送流程

#### 发送原理

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182244201.jpg)

1.  在消息发送的过程中，涉及到两个线程，main线程和sender线程，
   1. main线程是消息的生产线程
   2. sender线程是jvm单例的线程，专门用于消息的发送。

2. 在jvm的内存中开辟了一块缓存空间叫RecordAccumulator（消息累加器），用于将多条消息合并成一个批次，然后由sender线程发送给kafka集群。



 一条消息在生产过程执行的步骤：

1. 调用send方法然后经过拦截器经过序列化器，再经过分区器确定消息发送在具体topic下的哪个分区
   1. 拦截器：可以对数据进行加工
   2. 序列化器：序列化数据
   3. 分区器：决定将数据发往哪个分区（一个分区会创建一个队列）
2. 再将数据发送到对应的消息累加器（默认32MB）中，消息累加器是多个双端队列。并且每个队列和主题分区都具有一一映射关系。消息在累加器中，进行合并。达到了**对应的size（batch.size）或者等待超过对应的等待时间(linger.ms)**，都会触发sender线程的发送。
3. sender线程有一个请求池，默认缓存五个请求（ max.in.flight.requests.per.connection ），发送消息后，会等待服务端的ack
   1. 如果没收到ack就会重试，默认重试次数是 int 最大值（ retries ）。
   2. 如果ack成功就会删除累加器中的消息批次，并相应到生产端。



当双端队列中的DQueue满足 batch.size 或者 linger.ms 条件时触发sender线程。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182244256.jpg)

#### 生产者重要参数列表

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191142710.png)

### 发送

#### 普通异步发送

1）需求：创建 Kafka 生产者，采用异步的方式发送到 Kafka Broker

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182244227.jpg)

2）代码编写

（1）创建工程 kafka

（2）导入依赖

```xml
<dependencies>
     <dependency>
         <groupId>org.apache.kafka</groupId>
         <artifactId>kafka-clients</artifactId>
         <version>3.0.0</version>
     </dependency>
</dependencies>
```



（3）编写不带回调函数的 API 代码

```java
public class CustomProducer {

    public static void main(String[] args) {

        // 1. 给 kafka 配置对象添加配置信息：bootstrap.servers
        Properties properties = new Properties();
        //服务信息
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.181.131:9092,192.168.181.132:9092");
        //配置序列化
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());
        // 2. 创建 kafka 生产者的配置对象
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String,String>(properties);

        // 3. 创建 kafka 生产者对象
        for (int i = 0; i < 5; i++) {
            kafkaProducer.send(new ProducerRecord("first", "one" + i));
        }
        kafkaProducer.close();
    }
}
```

测试：在Linux上开启Kafka验证

 

#### 带回调函数的异步发送

 回调函数会在 producer 收到 ack 时调用，为异步调用，该方法有两个参数，分别是元数据信息（Record Metadata）和异常信息（Exception）

- 如果 Exception 为 null，说明消息发送成功
- 如果 Exception 不为 null，说明消息发送失败

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182244127.gif)

注意：消息发送失败会自动重试，不需要我们在回调函数中手动重试。



![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182244165.jpg)

 

```java

public class CustomProducer {

    public static void main(String[] args) {

        // 1. 给 kafka 配置对象添加配置信息：bootstrap.servers
        Properties properties = new Properties();
        //服务信息
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.181.131:9092");
        //配置序列化
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());
        // 2. 创建 kafka 生产者的配置对象
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String,String>(properties);

        // 3. 创建 kafka 生产者对象
        for (int i = 0; i < 5; i++) {
            kafkaProducer.send(new ProducerRecord("first", "one" + i), new Callback() {
                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    if (e == null) {
                        System.out.println( "分区 ： " + recordMetadata.partition() + " 主题： " + recordMetadata.topic() );
                    }
                }
            });
        }
        kafkaProducer.close();
    }
}
```





#### 同步发送API

1. 先处理已经堆积在DQueue中的数据。
2. RecordAccumulator再处理外部数据。

同步发送原理：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182244310.jpg)



只需在异步发送的基础上，再调用一下 get()方法即可。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182244362.jpg)

 

```java
public class CustomProducerSync {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // 1. 给 kafka 配置对象添加配置信息：bootstrap.servers
        Properties properties = new Properties();
        //服务信息
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.181.131:9092");
        //配置序列化
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());
        // 2. 创建 kafka 生产者的配置对象
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String,String>(properties);

        // 3. 创建 kafka 生产者对象
        for (int i = 0; i < 5; i++) {
            kafkaProducer.send(new ProducerRecord("first", "one" + i), new Callback() {
                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    if (e == null) {
                        System.out.println( "分区 ： " + recordMetadata.partition() + " 主题： " + recordMetadata.topic() );
                    }

                }
            }).get();
            Thread.sleep(100);
        }
        kafkaProducer.close();
    }
}
```



 

### 生产者拦截器

生产者拦截器 （ProducerInterceptor）

拦截器接口一共有三个方法。三个方法内的实现如果抛出异常，会被ProducerInterceptors内部捕获，并不会抛到上层。

```java
public interface ProducerInterceptor<K, V> extends Configurable {
    ProducerRecord<K, V> onSend(ProducerRecord<K, V> record);
    void onAcknowledgement(RecordMetadata metadata, Exception exception);
    void close();
}
```

onSend：方法在消息分区之前，可以对消息进行一定的修改，比如给key添加前缀，甚至可以修改我们的topic，如果需要使用kafka实现延时队列高级应用，我们就可以通过拦截器对消息进行判断，并修改，暂时放入我们的延时主题中，等时间达到再放回普通主题队列。

onAcknowledgement：该方法是在我们服务端对sender线程进行消息确认，或消息发送失败后的一个回调。优先于我们send方法的callback回调。我们可以对发送情况做一个统计。但是该方法在我们的sender线程也就是唯一的IO线程执行，逻辑越少越好。

close：该方法可以在关闭拦截器时，进行一些资源的释放。

（1） 实现自定义拦截器

```java
public MyInterceptor implements ProducerInterceptor {
    ProducerRecord<K, V> onSend(ProducerRecord<K, V> record);
    void onAcknowledgement(RecordMetadata metadata, Exception exception);
    void close();
}
```



（2）将自定义拦截器加入设置中

```java
properties.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,MyInterceptor.getClass.getName());
```



### 生产者分区

#### 分区的好处

- 从存储的角度：合理使用存储资源，实现负载均衡

- 从计算的角度：提高并行计算的可行性

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182244278.jpg)



#### 默认的分区器 DefaultPartitioner

在 IDEA 中 ctrl +n，全局查找 DefaultPartitioner。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182244313.jpg)



Kafka支持三种分区策略

- 指定分区；
- 指定key，计算hash得分区；
- 指定随机粘性分区；

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182244349.jpg)



#### 自定义分区器

如果研发人员可以根据企业需求，自己重新实现分区器。

1. 需求

例如我们实现一个分区器实现，发送过来的数据中如果包含 Hi，就发往 0 号分区，不包含 Hi，就发往 1 号分区。

2. 实现步骤

（1）定义类实现 Partitioner 接口。

（2）重写 partition()方法。

```java
public class MyPartitioner implements Partitioner {
    /**
     * @param topic 主题
     * @param key 消息的 key
     * @param keyBytes 消息的 key 序列化后的字节数组
     * @param value 消息的 value
     * @param valueBytes 消息的 value 序列化后的字节数组
     * @param cluster 集群元数据可以查看分区信息
     */
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        String string = value.toString();
        if (string.contains("vi")){
            return 2;
        }else{
            return 1;
        }
    }
}
```



（3）使用分区器的方法，在生产者的配置中添加分区器参数。

```java
//自定义分区规则 
properties.put(ProducerConfig.PARTITIONER_CLASS_CONFIG,MyPartitioner.class.getName());
```

（4）开启测试



### 生产者如何提高吞吐量

通过提高吞吐量达到低延迟的效果

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182244387.jpg)

Batch.size 与 linger.ms 配合使用，根据生成数据的大小指定。

RecordAccumlator：在异步发送并且分区很多的情况下，32M的数据量容易被满足，进程交互加大，可以适当提高到64M。

 ```java
  // batch.size：批次大小，默认 16K
 properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
 // linger.ms：等待时间，默认 0
 properties.put(ProducerConfig.LINGER_MS_CONFIG, 1);
 // RecordAccumulator：缓冲区大小，默认 32M：buffer.memory
 properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG,33554432);
 
 // compression.type：压缩，默认 none，可配置值 gzip、snappy、lz4 和 zstd
 properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
 ```



#### 消息累加器

消息累加器（RecordAccumulator）

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182244449.jpg)

 为了提高生产者的吞吐量，我们通过累加器将多条消息合并成一批统一发送。在broker中将消息批量存入。减少多次的网络IO。

- 消息累加器默认32m，如果生产者的发送速率大于sender发送的速率，消息就会堆满累加器。生产者就会阻塞，或者报错，报错取决于阻塞时间的配置。

- 累加器的存储形式为ConcurrentMap<TopicPartition, Deque&lt;ProducerBatch>>，可以看出来就是一个分区对应一个双端队列，队列中存储的是ProducerBatch一般大小是16k根据batch.size配置，新的消息会append到ProducerBatch中，满16k就会创建新的ProducerBatch，并且触发sender线程进行发送。

- 如果消息量非常大，生成了大量的ProducerBatch，在发送后，又需要JVM通过GC回收这些ProducerBatch就变得非常影响性能，所以kafka通过 BufferPool作为内存池来管理ProducerBatch的创建和回收，需要申请一个新的ProducerBatch空间时，调用 free.allocate(size, maxTimeToBlock)找内存池申请空间。

- 如果单条消息大于16k，那么就不会复用内存池了，会生成一个更大的ProducerBatch专门存放大消息，发送完后GC回收该内存空间。

为了进一步减小网络中消息传输的带宽。我们也可以通过**消息压缩**的方式，在生产端将消息追加进ProducerBatch就对每一条消息进行压缩了。常用的有Gzip、Snappy、Lz4 和 Zstd，这是时间换空间的手段。压缩的消息会在消费端进行解压。

 

#### 消息发送线程（Sender）

 消息保存在内存后，Sender线程就会把符合条件的消息按照批次进行发送。除了发送消息，元数据的加载也是通过Sender线程来处理的。

 Sender线程发送消息以及接收消息，都是基于java NIO的Selector。通过Selector把消息发出去，并通过Selector接收消息。

 Sender线程默认容纳5个未确认的消息，消息发送失败后会进行重试。



### 数据可靠性

#### 消息确认机制-ACK

producer提供了三种消息确认的模式，通过配置acks来实现

- 为0时， 表示生产者将数据发送出去就不管了，不等待任何返回。这种情况下数据传输效率最高，但是数据可靠性最低，当 server挂掉的时候就会丢数据；

- 为1时（默认），表示数据发送到Kafka后，经过leader成功接收消息的的确认，才算发送成功，如果leader宕机了，就会丢失数据。

- 为-1/all时，表示生产者需要等待ISR中的所有follower都确认接收到数据后才算发送完成，这样数据不会丢失，因此可靠性最高，性能最低。



![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182244091.jpg)

- **数据完全可靠条件**（至少一次At Least Once） = ACK级别设置为-1 + 分区副本大于等于2 + ISR里应答的最小副本数量大于等于2

- 最多一次（At Most One） = ACK设置为0

- AR = ISR + ORS

  - 正常情况下，如果所有的follower副本都应该与leader副本保持一定程度的同步，则AR = ISR，OSR = null。

  - ISR 表示在指定时间内和leader保存数据同步的集合；

  - ORS表示不能在指定的时间内和leader保持数据同步集合，称为OSR(Out-Sync Relipca set)。



```java
// Ack 设置
properties.put(ProducerConfig.ACKS_CONFIG,"1");

// 重试次数, 默认的重试次数是 Max.Integer
properties.put(ProducerConfig.RETRIES_CONFIG,3);
```





#### 数据去重-幂等性

1）[幂等性](https://www.seven97.top/microservices/protocol/idempotence.html) 原理

在一般的MQ模型中，常有以下的消息通信概念

- 至少一次（At Least Once）： ACK级别设置为-1 + 分区副本大于等于2 + ISR里应答的最小副本数量>=2。可以保证数据不丢失，但是**不能保证数据不重复**。

- 最多一次（At Most Once）：ACK级别设置为0 。可以保证数据不重复，但是**不能保证数据不丢失**。

- 精确一次（Exactly Once）：至少一次 + 幂等性 。 即能保证数据不丢失也不重复

 幂等性，简单地说就是对接口的多次调用所产生的结果和调用一次是一致的。生产者在进行重试的时候有可能会重复写入消息，而使用Kafka 的幂等性功能之后就可以避免这种情况。（不产生重复数据）



重复数据的判断标准：具有<PID, Partition, SeqNumber>相同主键的消息提交时，Broker只会持久化一条。

- ProducerId（pid）是Kafka每次重启都会分配一个新的；
- Partition 表示分区号；
- Sequence Number 序列化号，是单调自增的。



broker中会在内存维护一个 **pid+分区 对应的序列号**。如果收到的序列号正好比内存序列号大一，才存储消息，如果小于内存序列号，意味着消息重复，那么会丢弃消息，并应答。如果远大于内存序列号，意味着消息丢失，会抛出异常。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191654317.png)

所以生产者的幂等解决的是sender到broker间，由于网络波动可能造成的重发问题。用幂等来标识唯一消息。并且幂等性只能保证的是在 **单分区单会话内不重复**。不同分区的会话还是有可能存在重复的消息



2）如何使用幂等性

 开启幂等性功能的方式很简单，只需要显式地将生产者客户端参数 enable.idempotence设置为true 即可(这个参数的默认值为true)，并且还需要确保生产者客户端的retries、acks、max.in.filght.request.per.connection参数不被配置错，默认值就是对的。



#### 消息事务

由于幂等性不能跨分区运作，为了保证同时发的多条消息，要么全成功，要么全失败。kafka引入了事务的概念。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182244101.jpg)



开启事务需要producer设置 transactional.id 的值并同时开启幂等性。



通过事务协调器，来实现事务，相关API如下：

```java
// 1 初始化事务
void initTransactions();
// 2 开启事务
void beginTransaction() throws ProducerFencedException;
// 3 在事务内提交已经消费的偏移量（主要用于消费者）
void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets,
 String consumerGroupId) throws 
ProducerFencedException;
// 4 提交事务
void commitTransaction() throws ProducerFencedException;
// 5 放弃事务（类似于回滚事务的操作）
void abortTransaction() throws ProducerFencedException;
```



![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182244067.jpg)



### 数据有序

消息在单分区内有序，多分区内无序（如果对多分区进行排序，造成分区无法工作需要等待排序，浪费性能）

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182326753.png)

kafka只能保证单分区下的消息顺序性，为了保证消息的顺序性，需要做到如下几点。

- 如果未开启幂等性，需要 max.in.flight.requests.per.connection 设置为1。（缓冲队列最多放置1个请求）

- 如果开启幂等性，需要 max.in.flight.requests.per.connection 设置为小于5。
  - 这是因为启用幂等后，broker端会缓存生产者发来的最近5个请求的元数据，故无论如何，都可以保证最近5个请求的数据都是有序的。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182326674.png)

如果 Request3 在失败重试后才发往到集群中，必然会导致乱序，但是集群会重新按照序列号进行排序（最多一次排序5个）。



## Kafka Broker

### Broker设计

我们都知道kafka能堆积非常大的数据，一台服务器，肯定是放不下的。由此出现的**集群**的概念，集群不仅可以让消息负载均衡，还能提高消息存取的吞吐量。kafka集群中，会有多台broker，每台broker分别在不同的机器上。



为了提高吞吐量，每个topic也会都**多个分区**，同时为了保持可靠性，每个分区还会有**多个副本**。这些分区副本被均匀的散落在每个broker上，其中每个分区副本中有一个副本为leader，其他的为follower。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405182328769.png)



Controller节点的主要职能：

1. **Broker状态管理**：Controller会跟踪集群中所有Broker的在线状态，并在Broker宕机或者恢复时更新集群的状态。
2. **分区状态管理**：当新的Topic被创建，或者已有的Topic被删除时，Controller会负责管理这些变化，并更新集群的状态。
3. **分区领导者选举**：当一台Broker节点宕机时，并且宕机的机器上包含分区领导者副本时，Controller会负责对其上的所有Partition进行新的领导者选举。
4. **副本状态管理**：Controller负责管理Partition的ISR列表，当Follower副本无法及时跟随Leader副本时，Controller会将其从ISR列表中移除。
5. **分区重平衡**：当添加或删除Broker节点时，Controller会负责对Partition的分布进行重平衡，以确保数据的均匀分布。
6. **存储集群元数据**：Controller保存了集群中最全的元数据信息，并通过发送请求同步到其他Broker上面。



### Zookeeper作用

Zookeeper在Kafka中扮演了重要的角色，kafka使用zookeeper进行元数据管理，保存broker注册信息，包括主题（Topic）、分区（Partition）信息等，选择分区leader。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191029053.png)

### Broker选举Leader

 这里需要先明确一个概念leader选举，因为kafka中涉及多处选举机制，容易搞混，Kafka由三个方面会涉及到选举：

- broker（控制器）选leader
- 分区多副本选 leader
- 消费者选 Leader

 在kafka集群中由很多的broker（也叫做控制器），但是他们之间需要选举出一个leader，其他的都是follower。broker的leader有很重要的作用，诸如：创建、删除主题、增加分区并分配leader分区；集群broker管理，包括新增、关闭和故障处理；分区重分配（auto.leader.rebalance.enable=true），分区leader选举。 

每个broker都有唯一的brokerId，他们在启动后会去竞争注册zookeeper上的Controller结点，谁先抢到，谁就是broker leader。而其他broker会监听该结点事件，以便后续leader下线后触发重新选举。

简图：

- broker（控制器）选leader

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191030894.png)



详细图：

- broker选leader （也就是controller选举leader）
- 分区多副本选leader

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191030349.png)

流程如下：

1. 注册 Controller 节点
   当 Kafka 集群启动时，每个 Broker 都会尝试在 Zookeeper 中的 `/controller` 路径下创建一个临时节点。因为同一时刻只能存在一个 `/controller` 节点，所以只有一个 Broker 成功创建节点并成为Controller。其他 Broker 会收到节点创建失败的通知，然后转为观察者（Observer）状态，监视Controller节点路径的变化。
2. 监听 Controller 节点
    所有非Controller的 Broker 都会在 Zookeeper 中对 `/controller` 路径设置一个 Watcher 事件。这样当Controller节点发生变化时（例如，Controller失效），所有非Controller就会收到一个 Watcher 事件
3. 选举新的Controller
   当某个 Broker 接收到Controller节点变化的通知后，它会再次尝试在 Zookeeper 中的 `/controller` 路径下创建一个临时节点。与启动时的过程类似，只有一个 Broker 能够成功创建节点并成为新的Controller。新Controller会在选举成功后接管集群元数据的管理工作。
4. 更新集群元数据
   新Controller在选举成功后需要更新集群元数据，包括分区状态、副本状态等。同时，新控制器会通知所有相关的 Broker 更新它们的元数据信息。这样，集群中的所有 Broker 都能够知道新Controller的身份，并进行协同工作。



#### 脑裂问题

脑裂问题是分布式系统中经常出现的现象，Kafka脑列问题是由于网络或其他原因导致多个Broker认为自己是Controller，从而导致元数据不一致和分区状态混乱的问题。

Kafka是通过**epoch number（纪元编号）**来解决脑裂问题，epoch number是一个单调递增的版本号。

脑裂问题产生和处理过程如下：

- 假设有三个Broker，分别是Broker 0，Broker 1和Broker 2。Broker 0是Controller，它在ZooKeeper中创建了/controller节点，并设置epoch number值为1。Broker 1和Broker 2在/controller节点设置了Watcher。
- 由于某种原因，Broker 0出现了Full GC，导致它与ZooKeeper的会话超时。ZooKeeper删除了/controller节点，并通知Broker 1和Broker 2进行新的Controller选举。
- Broker 1和Broker 2同时尝试在ZooKeeper中创建/controller节点，假设Broker 1成功了，那么它就成为了新的Controller，设置epoch number值为2，并向Broker 2同步数据。
- Broker 0的Full GC结束后，继续向Broker 1和Broker 2同步数据，Broker 1和Broker 2接收到数据后，发现epoch number小于当前值，就会拒绝这些消息。并通知Broker 0最新的epoch number，然后Broker 0发现自己已经不是Controller了，最后与新的Controller建立连接。





#### Broker重要参数

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191033922.png)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191033143.png)



### 节点服役和退役

#### 服役新节点

(1) 启动一台新的KafKa服务端（加入原有的Zookeeper集群）

(2) 查看原有的 分区信息 describe

```shell
$ kafka-topics.sh --bootstrap-server 192.168.181.131:9092 --topic first --describe

Topic: first	TopicId: 4DtkHPe4R1KyXNF7QyVqBA	PartitionCount: 3	ReplicationFactor: 3	Configs: segment.bytes=1073741824
	Topic: first	Partition: 0	Leader: 1	Replicas: 2,1,0	Isr: 1,0
	Topic: first	Partition: 1	Leader: 0	Replicas: 0,1,2	Isr: 0,1
	Topic: first	Partition: 2	Leader: 1	Replicas: 1,2,0	Isr: 1,0
```

(3) 指定需要均衡的主题

```shell
$ vim topics-to-move.json
```

```shell
{
 "topics": [
 {"topic": "first"}
 ],
 "version": 1
}
```

(4) 生成负载均衡计划(只是生成计划)

```shell
bin/kafka-reassign-partitions.sh --bootstrap-server 192.168.181.131:9092 --topics-to-move-json-file topics-to-move.json --broker-list "0,1,2,3" --generate

```

```shell
Current partition replica assignment
{"version":1,"partitions":[{"topic":"first","partition":0,"replic
as":[0,2,1],"log_dirs":["any","any","any"]},{"topic":"first","par
tition":1,"replicas":[2,1,0],"log_dirs":["any","any","any"]},{"to
pic":"first","partition":2,"replicas":[1,0,2],"log_dirs":["any","
any","any"]}]}
Proposed partition reassignment configuration
{"version":1,"partitions":[{"topic":"first","partition":0,"replic
as":[2,3,0],"log_dirs":["any","any","any"]},{"topic":"first","par
tition":1,"replicas":[3,0,1],"log_dirs":["any","any","any"]},{"to
pic":"first","partition":2,"replicas":[0,1,2],"log_dirs":["any","
any","any"]}]}
```

（3）创建副本存储计划（所有副本存储在 broker0、broker1、broker2、broker3 中）。

```shell
vim increase-replication-factor.json
```

```shell
{"version":1,"partitions":[{"topic":"first","partition":0,"replic
as":[2,3,0],"log_dirs":["any","any","any"]},{"topic":"first","par
tition":1,"replicas":[3,0,1],"log_dirs":["any","any","any"]},{"to
pic":"first","partition":2,"replicas":[0,1,2],"log_dirs":["any","
any","any"]}]}
```

(5) 执行副本计划

```shell
kafka-reassign-partitions.sh --bootstrap-server 192.168.181.131:9092 --reassignment-json-file increase-replication-factor.json --execute
```

(6) 验证计划

```shell
kafka-reassign-partitions.sh --bootstrap-server 192.168.181.131:9092 --reassignment-json-file increase-replication-factor.json --verify
```

```shell
Status of partition reassignment:
Reassignment of partition first-0 is complete.
Reassignment of partition first-1 is complete.
Reassignment of partition first-2 is complete.
Clearing broker-level throttles on brokers 0,1,2,3
Clearing topic-level throttles on topic first
```



#### 退役旧节点

执行负载均衡操作:先按照退役一台节点，生成执行计划，然后按照服役时操作流程执行负载均衡。

不同于服役计划的 --broker-list "0,1,2" 退役了 Broker3 ；

```shell
kafka-reassign-partitions.sh --bootstrap-server 192.168.181.131:9092 --topics-to-move-json-file topics-to-move.json --broker-list "0,1,2" --generate
```

### 副本机制

#### 副本基本信息

- Replica ：副本，同一分区的不同副本保存的是相同的消息，为保证集群中的某个节点发生故障时，该节点上的 partition 数据不丢失 ，提高副本可靠性，且 kafka 仍然能够继续工作，kafka 提供了副本机制，一个 topic 的每个分区都有若干个副本，一个 leader 和若干个 follower。
- Leader ：每个分区的多个副本中的"主副本"，生产者以及消费者只与 Leader 交互。
- Follower ：每个分区的多个副本中的"从副本"，负责实时从 Leader 中同步数据，保持和 Leader 数据的同步。Leader 发生故障时，从 Follower 副本中重新选举新的 Leader 副本对外提供服务。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191037772.png)

- AR: 分区中的所有 Replica 统称为 AR = ISR +OSR
- ISR: 表示和Leader保持同步的Follow集合，如果Follow长时间未向Leader发送通信请求或同步数据，则该Fellow将被踢出ISR。该时间阈值由replica.lag.time.max.ms参数设定，默认时间是30s，Leader发生故障后，就会从ISR中选举新的Leader。
- OSR: 从ISR被踢出的Fellow，就进入OSR，也就是与 Leader 副本同步滞后过多的 Replica 组成了 OSR
- LEO: 每个副本的最后一个offset，LEO其实就是最新的offset+1.
- HW: 高水位，代表所有ISR中的LEO最低的那个offset，也是消费者可见的最大消息offset。

#### 副本选举 Leader

 Kafka 集群中有一个 broker 的 Controller 会被选举为 Controller Leader ，负责管理集群Broker 的上下线，所有 topic 的分区副本分配和 Leader 选举等工作。



##### 触发副本leader的选举的时机

- **Leader Replica 失效：**当 Leader Replica 出现故障或者失去连接时，Kafka 会触发 Leader Replica 选举。
- **Broker 宕机：**当 Leader Replica 所在的 Broker 节点发生故障或者宕机时，Kafka 也会触发 Leader Replica 选举。
- **新增 Broker：**当集群中新增 Broker 节点时，Kafka 还会触发 Leader Replica 选举，以重新分配 Partition 的 Leader。
- **新建分区：**当一个新的分区被创建时，需要选举一个 Leader Replica。
- **ISR 列表数量减少：**当 Partition 的 ISR 列表数量减少时，可能会触发 Leader Replica 选举。当 ISR 列表中副本数量小于 **Replication Factor（副本因子）**时，为了保证数据的安全性，就会触发 Leader Replica 选举。
- **手动触发：**通过 Kafka 管理工具（kafka-preferred-replica-election.sh），可以手动触发选举，以平衡负载或实现集群维护。



#####  Leader Replica 选举策略

有以下三种

1. ISR 选举策略：默认情况下，Kafka 只会从 ISR 集合的副本中选举出新的 Leader Replica，OSR 集合中的副本不具备参选资格。

2. 首选副本选举策略（Preferred Replica Election）：首选副本选举策略也是 Kafka 默认的选举策略。在这种策略下，每个分区都有一个首选副本（Preferred Replica），通常是副本集合中的第一个副本。当触发选举时，控制器会优先选择该首选副本作为新的 Leader Replica，只有在首选副本不可用的情况下，才会考虑其他副本。当然，也可以使用命令手动指定每个分区的首选副本：

   ```shell
   bin/kafka-topics.sh --zookeeper localhost:2181 --topic my-topic-name --replica-assignment 0:1,1:2,2:0 --partitions 3
   //意思是：my-topic-name有3个partition，partition0的首选副本是Broker1，partition1首选副本是Broker2，partition2的首选副本是Broker0
   ```

3. 不干净副本选举策略（Unclean Leader Election）：在某些情况下，ISR 选举策略可能会失败，例如当所有 ISR 副本都不可用时。在这种情况下，可以使用 Unclean Leader 选举策略。Unclean Leader 选举策略会从所有副本中（包含OSR集合）选择一个副本作为新的 Leader 副本，即使这个副本与当前 Leader 副本不同步。这种选举策略可能会导致数据丢失，因此只应在紧急情况下使用。
   修改下面的配置，可以开启 Unclean Leader 选举策略，默认关闭。

   ```shell
   unclean.leader.election.enable=true
   ```

   

 Broker中Controller 的信息同步工作是依赖于 Zookeeper 的 ./broker/topic 目录下的信息。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191038640.png)

分为两个阶段，第一个阶段是候选人的提名和投票阶段，第二个阶段是Leader的确认阶段。具体过程如下：

1. 候选人提名和投票阶段
   - 在Leader Replica失效时，ISR集合中所有Follower Replica都可以成为新的Leader Replica候选人。每个Follower Replica会在选举开始时向其他Follower Replica发送成为候选人的请求，并附带自己的元数据信息，包括自己的当前状态和Lag值。而Preferred replica优先成为候选人。
   - 其他Follower Replica在收到候选人请求后，会根据请求中的元数据信息，计算每个候选人的Lag值，并将自己的选票投给Lag最小的候选人。如果多个候选人的Lag值相同，则随机选择一个候选人。
2. Leader确认阶段
   - 在第一阶段结束后，所有的Follower Replica会重新计算每位候选人的Lag值，并投票给Lag值最小的候选人。此时，选举的结果并不一定出现对候选人的全局共识。为了避免出现这种情况，Kafka中使用了ZooKeeper来实现分布式锁，确保只有一个候选人能够成为新的Leader Replica。
   - 当ZooKeeper确认有一个候选人已经获得了分布式锁时，该候选人就成为了新的Leader Replica，并向所有的Follower Replica发送一个LeaderAndIsrRequest请求，更新Partition的元数据信息。其他Follower Replica接收到请求后，会更新自己的Partition元数据信息，将新的Leader Replica的ID添加到ISR列表中。



#### 副本故障处理

1. follower故障流程

如果follower落后leader过多，体现在落后时间 repca.lag.time.max.ms ，或者落后偏移量repca.lag.max.messages(由于kafka生成速度不好界定，后面取消了该参数)，follower就会被移除ISR队列，等待该队列LEO追上HW，才会重新加入ISR中。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191040094.png)



2. leader故障流程

旧Leader先被从ISR队列中踢出，然后从ISR中选出一个新的Leader来；此时为了保证多个副本之间的数据一致性，其他的follower会先将各自的log文件中高于HW的部分截取掉，然后从新的leader同步数据（由此可知这只能保证副本之间数据一致性，并不能保证数据不丢失或者不重复）。体现了设置 ACK-all 的重要性。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191040346.png)



#### 分区副本分配

如果 kafka 服务器只有 4 个节点，那么设置 kafka 的分区数大于服务器台数，在 kafka底层如何分配存储副本呢？



创建 16 分区，3 个副本

（1）创建一个新的 topic，名称为 second。

```shell
kafka-topics.sh --bootstrap-server 192.168.181.131:9092 --create --partitions 16 --replication-factor 3 --topic second
```

（2）查看分区和副本情况。

```shell
kafka-topics.sh --bootstrap-server 192.168.181.131:9092  --describe --topic second
Topic: second4 Partition: 0 Leader: 0 Replicas: 0,1,2 Isr: 0,1,2
Topic: second4 Partition: 1 Leader: 1 Replicas: 1,2,3 Isr: 1,2,3
Topic: second4 Partition: 2 Leader: 2 Replicas: 2,3,0 Isr: 2,3,0
Topic: second4 Partition: 3 Leader: 3 Replicas: 3,0,1 Isr: 3,0,1
Topic: second4 Partition: 4 Leader: 0 Replicas: 0,2,3 Isr: 0,2,3
Topic: second4 Partition: 5 Leader: 1 Replicas: 1,3,0 Isr: 1,3,0
Topic: second4 Partition: 6 Leader: 2 Replicas: 2,0,1 Isr: 2,0,1
Topic: second4 Partition: 7 Leader: 3 Replicas: 3,1,2 Isr: 3,1,2
Topic: second4 Partition: 8 Leader: 0 Replicas: 0,3,1 Isr: 0,3,1
Topic: second4 Partition: 9 Leader: 1 Replicas: 1,0,2 Isr: 1,0,2
Topic: second4 Partition: 10 Leader: 2 Replicas: 2,1,3 Isr: 2,1,3
Topic: second4 Partition: 11 Leader: 3 Replicas: 3,2,0 Isr: 3,2,0
Topic: second4 Partition: 12 Leader: 0 Replicas: 0,1,2 Isr: 0,1,2
Topic: second4 Partition: 13 Leader: 1 Replicas: 1,2,3 Isr: 1,2,3
Topic: second4 Partition: 14 Leader: 2 Replicas: 2,3,0 Isr: 2,3,0
Topic: second4 Partition: 15 Leader: 3 Replicas: 3,0,1 Isr: 3,0,1
```

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191041957.png)

#### 手动调整分区副本

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191042767.png)

手动调整分区副本存储的步骤如下：

（1）创建一个新的 topic，名称为 three。

```shell
kafka-topics.sh --bootstrap-server  192.168.181.131:9092  --create --partitions 4 --replication-factor 2 --topic three
```

（3）创建副本存储计划（所有副本都指定存储在 broker0、broker1 中）。

```shell
$ vim increase-replication-factor.json
输入如下内容：
{
"version":1,
"partitions":[
{"topic":"three","partition":0,"replicas":[0,1]},
{"topic":"three","partition":1,"replicas":[0,1]},
{"topic":"three","partition":2,"replicas":[1,0]},
{"topic":"three","partition":3,"replicas":[1,0]}]
}
```

（4）执行副本存储计划。

```shell
kafka-reassign-partitions.sh --bootstrap-server  192.168.181.131:9092  --reassignment-json-file increase-replication-factor.json --execute
```

（5）验证副本存储计划。

```shell
kafka-reassign-partitions.sh --bootstrap-server  192.168.181.131:9092  --reassignment-json-file increase-replication-factor.json --verify
```

#### 分区自动调整

 一般情况下，我们的分区都是平衡散落在broker的，随着一些broker故障，会慢慢出现leader集中在某台broker上的情况，造成集群负载不均衡，这时候就需要分区平衡。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191043396.png)

为了解决上述问题kafka出现了自动平衡的机制。kafka提供了下面几个参数进行控制：

- auto.leader.rebalance.enable：自动leader parition平衡，默认是true;
- leader.imbalance.per.broker.percentage：每个broker允许的不平衡的leader的比率，默认是10%，如果超过这个值，控制器将会触发leader的平衡
- leader.imbalance.check.interval.seconds：检查leader负载是否平衡的时间间隔，默认是300秒
- 但是在生产环境中是不开启这个自动平衡，因为触发leader partition的自动平衡会损耗性能，或者可以将触发自动平和的参数leader.imbalance.per.broker.percentage的值调大点。

我们也可以通过修改配置，然后手动触发分区的再平衡。

#### 增加副本因子

在生产环境当中，由于某个主题的重要等级需要提升，我们考虑增加副本。副本数的增加需要先制定计划，然后根据计划执行。不能通过命令行的方法添加副本。

1. 创建 topic

```shell
bin/kafka-topics.sh --bootstrap-server 192.168.181.131:9092 --create --partitions 3 --replication-factor 1 --topic four
```



2. 手动增加副本存储

（1）创建副本存储计划（所有副本都指定存储在 broker0、broker1、broker2 中）。

```shell
vim increase-replication-factor.json
```

```shell
{"version":1,"partitions":[
{"topic":"four","partition":0,"replicas":[0,1,2]},
{"topic":"four","partition":1,"replicas":[0,1,2]},
{"topic":"four","partition":2,"replicas":[0,1,2]}]}
```



（2）执行副本存储计划。

```shell
kafka-reassign-partitions.sh --bootstrap-server 192.168.181.131:9092 --reassignment-json-file increase-replication-factor.json --execute
```

### 文件存储

#### 存储结构

在Kafka中主题（Topic）是一个逻辑上的概念，分区（partition）是物理上的存在的。每个partition对应一个log文件，该log文件中存储的就是Producer生产的数据。Producer生产的数据会被不断**追加到该log文件末端**。为防止log文件过大导致数据定位效率低下，Kafka采用了分片和索引机制，将每个partition分为多个segment，每个segment默认1G（ log.segment.bytes ）， 每个segment包括.index文件、.log文件和**.timeindex**等文件。这些文件位于文件夹下，该文件命名规则为：topic名称+分区号。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191046998.png)

Segment的三个文件需要通过特定工具打开才能看到信息

```shell
kafka-run-class.sh kafka.tools.DumpLogSegments --files ./00000000000000000000.index
kafka-run-class.sh kafka.tools.DumpLogSegments --files ./00000000000000000000.log
```

当log文件写入4k（这里可以通过log.index.interval.bytes设置）数据，就会写入一条索引信息到index文件中，这样的index索引文件就是一个稀疏索引，它并不会每条日志都建立索引信息。

 当Kafka查询一条offset对应实际消息时，可以通过index进行二分查找，获取最近的低位offset，然后从低位offset对应的position开始，从实际的log文件中开始往后查找对应的消息。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191047057.png)

`时间戳索引文件`，它的作用是可以查询某一个时间段内的消息，它的数据结构是：时间戳（8byte）+ 相对offset（4byte），如果要使用这个索引文件，先要通过时间范围找到对应的offset，然后再去找对应的index文件找到position信息，最后在遍历log文件，这个过程也是需要用到index索引文件的。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191047667.png)



#### 文件清理策略

 Kafka将消息存储在磁盘中，为了控制磁盘占用空间的不断增加就需要对消息做一定的清理操作。Kafka 中每一个分区副本都对应一个Log，而Log又可以分为多个日志分段，这样也便于日志的清理操作。Kafka提供了两种日志清理策略。

- 日志删除(delete) :按照一定的保留策略直接删除不符合条件的日志分段。
- 日志压缩(compact) :针对每个消息的key进行整合，对于有相同key的不同value值，只保留最后一个版本。

我们可以通过修改broker端参数 log.cleanup.policy 来进行配置



1. 日志删除

kafka中默认的日志保存时间为7天，可以通过调整如下参数修改保存时间。

- log.retention.hours：最低优先级小时，默认7天
- log.retention.minutes：分钟
- log.retention.ms：最高优先级毫秒
- log.retention.check.interval.ms：负责设置检查周期，默认5分钟
- file.delete.delay.ms：延迟执行删除时间
- log.retention.bytes：当设置为-1时表示运行保留日志最大值（相当于关闭）；当设置为1G时，表示日志文件最大值



具体的保留日志策略有三种：

- 基于时间策略

  -  日志删除任务会周期检查当前日志文件中是否有保留时间超过设定的阈值来寻找可删除的日志段文件集合；这里需要注意log.retention参数的优先级：log.retention.ms > log.retention.minutes > log.retention.hours，默认只会配置log.retention.hours参数，值为168即为7天。

  - 删除过期的日志段文件，并不是简单的根据日志段文件的修改时间计算，而是要根据该日志段中最大的时间戳来计算的，首先要查询该日志分段所对应的时间戳索引文件，查找该时间戳索引文件的最后一条索引数据，如果时间戳大于0就取值，否则才会使用最近修改时间。

  -  在删除的时候先从Log对象所维护的日志段的跳跃表中移除要删除的日志段，用来确保已经没有线程来读取这些日志段；接着将日志段所对应的所有文件，包括索引文件都添加上**.deleted的后缀；最后交给一个以delete-file命名的延迟任务来删除这些以.deleted为后缀的文件，默认是1分钟执行一次，可以通过file.delete.delay.ms**来配置。

- 基于日志大小策略
  - 日志删除任务会周期性检查当前日志大小是否超过设定的阈值（log.retention.bytes，默认是-1，表示无穷大），就从第一个日志分段中寻找可删除的日志段文件集合。如果超过阈值，

- 基于日志起始偏移量
  - 该策略判断依据是日志段的下一个日志段的起始偏移量 baseOffset是否小于等于 logStartOffset，如果是，则可以删除此日志分段。这里说一下logStartOffset，一般情况下，日志文件的起始偏移量 logStartOffset等于第一个日志分段的 baseOffset，但这并不是绝对的，logStartOffset的值可以通过 DeleteRecordsRequest请求、使用 kafka-delete-records.sh 脚本、日志的清理和截断等操作进行修改。



2. 日志压缩

日志压缩对于有相同key的不同value值，只保留最后一个版本。如果应用只关心 key对应的最新 value值，则可以开启 Kafka相应的日志清理功能，Kafka会定期将相同 key的消息进行合并，只保留最新的 value值。

- log.cleanup.policy = compact 所有数据启用压缩策略

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191049755.png)


 这种策略只适合特殊场景，比如消息的key是用户ID，value是用户的资料，通过这种压缩策略，整个消息集里就保存了所有用户最新的资料。一般用的比较少



### Kafka高效读数据

kafka之所以可以快速读写的原因如下：

- kafka是分布式集群，采用分区方式，并行操作
- 读取数据采用稀疏索引，可以快速定位消费数据
- 顺序写磁盘
- 页缓冲和零拷贝

#### 顺序写磁盘

Kafka 的 producer 生产数据，要写入到 log 文件中，写的过程是一直**追加**到文件末端，为顺序写。官网有数据表明，同样的磁盘，顺序写能到 600M/s，而随机写只有 100K/s。这与磁盘的机械机构有关，顺序写之所以快，是因为其省去了大量磁头寻址的时间。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191049264.png)



#### 页缓存

kafka高效读写的原因很大一部分取决于[页缓存](https://www.seven97.top/cs-basics/operating-system/pagecache.html")和[零拷贝](https://www.seven97.top/cs-basics/operating-system/zerocopytechnology.html)

在 Kafka 中，大量使用了 PageCache， 这也是 Kafka 能实现高吞吐的重要因素之一。

首先看一下读操作，当一个进程要去读取磁盘上的文件内容时，操作系统会先查看要读取的数据页是否缓冲在PageCache 中，如果存在则直接返回要读取的数据，这就减少了对于磁盘 I/O的 操作；但是如果没有查到，操作系统会向磁盘发起读取请求并将读取的数据页存入 PageCache 中，之后再将数据返回给进程，就和使用redis缓冲是一个道理。

 接着写操作和读操作是一样的，如果一个进程需要将数据写入磁盘，操作系统会检查数据页是否在PageCache 中已经存在，如果不存在就在 PageCache中添加相应的数据页，接着将数据写入对应的数据页。另外被修改过后的数据页也就变成了脏页，操作系统会在适当时间将脏页中的数据写入磁盘，以保持数据的一致性。

 具体的刷盘机制可以通过 log.flush.interval messages，log.flush .interval .ms 等参数来控制。同步刷盘可以提高消息的可靠性，防止由于机器 掉电等异常造成处于页缓存而没有及时写入磁盘的消息丢失。一般并不建议这么做，刷盘任务就应交由操作系统去调配，消息的可靠性应该由多副本机制来保障，而不是由同步刷盘这 种严重影响性能的行为来保障 。



#### 零拷贝

[零拷贝](https://www.seven97.top/cs-basics/operating-system/zerocopytechnology.html) 并不是不需要拷贝，而是减少不必要的拷贝次数，通常使用在IO读写过程中。常规应用程序IO过程如下图，会经过四次拷贝：

- 数据从磁盘经过DMA(直接存储器访问)到内核的Read Buffer；
- 内核态的Read Buffer到用户态应用层的Buffer
- 用户态的Buffer到内核态的Socket Buffer
- Socket Buffer到网卡的NIC Buffer

从上面的流程可以知道内核态和用户态之间的拷贝相当于执行两次无用的操作，之间切换也会花费很多资源；当数据从磁盘经过DMA 拷贝到内核缓存（页缓存）后，为了减少CPU拷贝的性能损耗，操作系统会将该内核缓存与用户层进行共享，减少一次CPU copy过程，同时用户层的读写也会直接访问该共享存储，本身由用户层到Socket缓存的数据拷贝过程也变成了从内核到内核的CPU拷贝过程，更加的快速，这就是零拷贝，IO流程如下图:

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191051139.png)

甚至如果我们的消息存在页缓存PageCache中，还避免了硬盘到内核的拷贝过程，更加一步提升了消息的吞吐量。 (大概就理解成传输的数据只保存在内核空间，不需要再拷贝到用户态的应用层)

Java的JDK NIO中方法transferTo()方法就能够实现零拷贝操作，这个实现依赖于操作系统底层的sendFile()实现的



## Kafka消费者

### 消费模式

常见的消费模式有两种：

- poll(拉)：消费者主动向服务端拉取消息。

- push(推)：服务端主动推送消息给消费者。

由于推模式很难考虑到每个客户端不同的消费速率，导致消费者无法消费消息而宕机，因此kafka采用的是poll的模式，但该模式有个缺点，如果服务端没有消息，消费端就会**一直空轮询**。为了避免过多不必要的空轮询，kafka做了改进，如果没消息服务端就会暂时保持该请求，在一段时间内有消息再回应给客户端。



### 消费工作流程

#### 消费者总体工作流程

 消费者对消息进行消费，就将已经消费的消息加入 `_consumer_offsets` 中。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191057142.png)

需要注意的是：

- 一个消费者可以消费多个分区数据
- 每个分区的数据只能由消费者中一个消费者消费（就把一个group当成一个整体即可）



#### 消费者组原理

Consumer Group（CG）：消费者组，由多个consumer组成。消费者的groupid相同，就会形成一个消费者组

- 消费者**组内每个消费者负责消费不同分区**的数据，一个分区只能由一个组内消费者消费。
- 消费者组之间互不影响。所有的消费者都属于某个消费者组，即消费者组是逻辑上的一个订阅者。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191057925.png)

 对于消息中间件而言，一般有两种消息投递模式：点对点(P2P, Point-to-Point)模式和发布／订阅(Pub/Sub)模式。点对点模式是基于队列的，消息生产者发送消息到队列，消息消费者从队列中接收消息。发布订阅模式定义了如何向一个内容节点发布和订阅消息，这个内容节点称为主题(Topic) , 主题可以认为是消息传递的中介，消息发布者将消息发布到某个主题， 而消息订阅者从主题中订阅消息。主题使得消息的订阅者和发布者互相保持独立，不需要进行接触即可保证消息的传递，发布／订阅模式在消息的一对多广播时采用。Kafka同时支待两种消息投递模式，而这正是得益于消费者与消费组模型的契合：

- 如果所有的消费者都隶属于同一个消费组，那么所有的消息都会被均衡地投递给每一个消费者，即每条消息只会被一个消费者处理，这就相当于点对点模式的应用。
- 如果所有的消费者都隶属于不同的消费组，那么所有的消息都会被广播给所有的消费者，即每条消息会被所有的消费者处理，这就相当于发布／订阅模式的应用。

#### 消费者组选举Leader

具体的消费者组初始化流程：

通过对GroupId进行Hash得到获得服务器的coordinator ，coordinator负责选出消费组中的Leader ，并且协调信息。真正存储消费记录的是 _consumer_offsets_partition 。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191058931.png)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191058765.png)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191058773.png)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191059964.png)

### 消费者API

消费组单消费者以及消费者组多消费者

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191059304.png)

注意：在消费者 API 代码中**必须配置消费者组 id**。命令行启动消费者不填写消费者组id 会被自动填写随机的消费者组 id。

```java
public class CustomConsumer {
    public static void main(String[] args) {
        //0.配置信息
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.181.131:9092");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "test");

        //1.创建消费者
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties);
        ArrayList<String> topic = new ArrayList<>();
        topic.add("first");
        kafkaConsumer.subscribe(topic);


        //2.消费信息
        while (true) {
            //Duration.ofSeconds(1)，1秒为一批次
            ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(1));
            records.forEach(record -> {
                System.out.println(record);
            });
        }
        //3.关闭
    }
}
```



### 分区平衡以及再平衡

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191100921.png)



- heartbeat.interval.ms Kafka 消费者和 coordinator 之间的心跳时间，默认 3s。该条目的值必须小于 session.timeout.ms，也不应该高于session.timeout.ms 的 1/3。
- session.timeout.ms Kafka 消费者和 coordinator 之间连接超时时间，默认 45s。超过该值，该消费者被移除，消费者组执行再平衡。
- max.poll.interval.ms 消费者处理消息的最大时长，默认是 5 分钟。超过该值，该消费者被移除，消费者组执行再平衡。
- partition.assignment.strategy 消 费 者 分 区 分 配 策 略 ， 默 认 策 略 是 Range + CooperativeSticky。Kafka 可以同时使用多个分区分配策略。可 以 选 择 的 策 略 包 括 ： Range 、 RoundRobin 、 Sticky 、CooperativeSticky (协作者粘性)



#### 分区分配策略

 我们知道一个 Consumer Group 中有多个 Consumer，一个 Topic 也有多个 Partition，所以必然会涉及到 Partition 的分配问题: 确定哪个 Partition 由哪个 Consumer 来消费的问题。

Kafka 客户端提供了3 种分区分配策略：RangeAssignor、RoundRobinAssignor 和 StickyAssignor，前两种 分配方案相对简单一些StickyAssignor分配方案相对复杂一些。

#### Range

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191100580.png)

Range 分区分配再平衡案例：

1. 停止掉 0 号消费者，快速重新发送消息观看结果（45s 以内，越快越好）。
   1 号消费者：消费到 3、4 号分区数据。
   2 号消费者：消费到 5、6 号分区数据。
   0 号消费者的任务会整体被分配到 1 号消费者或者 2 号消费者。 (被整体分配)
   说明：0 号消费者挂掉后，消费者组需要按照超时时间 45s 来判断它是否退出，所以需要等待，时间到了 45s 后，判断它真的退出就会把任务分配给其他 broker 执行。
2. 再次重新发送消息观看结果（45s 以后）。
   1 号消费者：消费到 0、1、2、3 号分区数据。
   2 号消费者：消费到 4、5、6 号分区数据。
   说明：消费者 0 已经被踢出消费者组，所以重新按照 range 方式分配。

#### RoundRobin

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191106159.png)

```java
// 修改分区分配策略
properties.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, "org.apache.kafka.clients.consumer.RoundRobinAssignor");
```

1. 停止掉 0 号消费者，快速重新发送消息观看结果（45s 以内，越快越好）。
   1 号消费者：消费到 2、5 号分区数据
   2 号消费者：消费到 4、1 号分区数据
   0 号消费者的任务会按照 RoundRobin 的方式，把数据轮询分成 0 、6 和 3 号分区数据，分别由 1 号消费者或者 2 号消费者消费。（采用轮询）
   说明：0 号消费者挂掉后，消费者组需要按照超时时间 45s 来判断它是否退出，所以需要等待，时间到了 45s 后，判断它真的退出就会把任务分配给其他 broker 执行。
2. 再次重新发送消息观看结果（45s 以后）。
   1 号消费者：消费到 0、2、4、6 号分区数据
   2 号消费者：消费到 1、3、5 号分区数据
   说明：消费者 0 已经被踢出消费者组，所以重新按照 RoundRobin 方式分配。

#### Sticky

StickyAssignor 分区分配算法是 Kafka 客户端提供的分配策略中最复杂的一种，可以通过 partition.assignment.strategy 参数去设置，从 0.11 版本开始引入，目的就是在执行新分配时，尽量在上一次分配结果上少做调整，其主要实现了以下2个目标：

1. Topic Partition 的分配要尽量均衡。
2. 当 Rebalance(重分配，后面会详细分析) 发生时，尽量与上一次分配结果保持一致。

该算法的精髓在于，重分配后，还能尽量与上一次结果保持一致，进而达到消费者故障下线，故障恢复后的均衡问题，在此就不举例了。

```java
// 修改分区分配策略
properties.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, "org.apache.kafka.clients.consumer.StickyAssignor");
```



### offset位移提交

#### offset 的默认维护位置

 Kafka 0.9 版本之前consumer默认将offset保存在Zookeeper中，从0.9版本之后consumer默认保存在Kafka一个内置的topic中，该topic为_consumer_offsets。

 消费者提交的offset值维护在**__consumer_offsets这个Topic中，具体维护在哪个分区中，是由消费者所在的消费者组groupid**决定，计算方式是：groupid的hashCode值对50取余。当kafka环境正常而消费者不能消费时，有可能是对应的__consumer_offsets分区leader为none或-1，或者分区中的日志文件损坏导致。

 __consumer_offsets 主题里面采用 key 和 value 的方式存储数据。key 是 group.id+topic+ 分区号，value 就是当前 offset 的值。每隔一段时间，kafka 内部会对这个 topic 进行 compact，也就是每个 group.id+topic+分区号就保留最新数据。

 一般情况下， 当集群中第一次有消费者消费消息时会自动创建主题_ consumer_ offsets, 不过它的副本因子还受offsets.topic .replication.factor参数的约束，这个参数的默认值为3 (下载安装的包中此值可能为1)，分区数可以通过offsets.topic.num.partitions参数设置，默认为50。

 在配置文件 config/consumer.properties 中添加配置 exclude.internal.topics=false，默认是 true，表示不能消费系统主题。为了查看该系统主题数据，所以该参数修改为 false。

```shell
kafka-console-consumer.sh --topic __consumer_offsets --bootstrap-server 192.168.181.131:9092 --consumer.config config/consumer.properties --formatter "kafka.coordinator.group.GroupMetadataManager\$OffsetsMessageFormatter" --from-beginning
```

```shell
[offset,atguigu,1]::OffsetAndMetadata(offset=7, 
leaderEpoch=Optional[0], metadata=, commitTimestamp=1622442520203, 
expireTimestamp=None)
[offset,atguigu,0]::OffsetAndMetadata(offset=8, 
leaderEpoch=Optional[0], metadata=, commitTimestamp=1622442520203, 
expireTimestamp=None)
```

消费者提交offset的方式有两种，自动提交和手动提交

#### 自动提交

为了使我们能够专注于自己的业务逻辑，Kafka提供了自动提交offset的功能。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191107383.png)

- enable.auto.commit：是否开启自动提交offset功能，默认是true
- auto.commit.interval.ms：自动提交offset的时间间隔，默认是5s

自动提交有可能出现消息消费失败，但是却提交了offset的情况，导致消息丢失。为了能够实现消息消费offset的精确控制，更推荐手动提交。

```java
// 自动提交
properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
// 提交时间间隔
properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG,1000);
```

#### 手动提交

虽然自动提交offset十分简单便利，但由于其是基于时间提交的，开发人员难以把握offset提交的时机。因此Kafka还提供了手动提交offset的API。手动提交offset的方法有两种：分别是commitSync（同步提交）和commitAsync（异步提交）。两者的相同点是，都会将本次提交的一批数据最高的偏移量提交；不同点是，同步提交阻塞当前线程，一直到提交成功，并且会自动失败重试（由不可控因素导致，也会出现提交失败）；而异步提交则没有失败重试机制，故有可能提交失败。

- commitSync（同步提交）：必须等待offset提交完毕，再去消费下一批数据。 阻塞线程，一直到提交到成功，会进行失败重试
- commitAsync（异步提交） ：发送完提交offset请求后，就开始消费下一批数据了。没有失败重试机制，会提交失败

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191108631.png)

#### 指定消费位置

在kafka中当消费者查找不到所记录的消费位移时，会根据auto.offset.reset的配置，决定从何处消费。

auto.offset.reset = earliest | latest | none 默认是 latest。

- earliest：自动将偏移量重置为最早的偏移量，–from-beginning。
- latest（默认值）：自动将偏移量重置为最新偏移量
- none：如果未找到消费者组的先前偏移量，则向消费者抛出异常。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191108717.png) 

Kafka中的消费位移是存储在一个内部主题中的， 而我们可以使用**seek()**方法可以突破这一限制：消费位移可以保存在任意的存储介质中， 例如数据库、 文件系统等。以数据库为例， 我们将消费位移保存在其中的一个表中， 在下次消费的时候可以读取存储在数据表中的消费位移并通过seek()方法指向这个具体的位置 。

```java
//配置信息
Properties properties = new Properties();
properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
```

##### 指定位移消费

```java
// 指定位置进行消费
Set<TopicPartition> assignment = kafkaConsumer.assignment();

//  保证分区分配方案已经制定完毕
while (assignment.size() == 0){
    kafkaConsumer.poll(Duration.ofSeconds(1));
    assignment = kafkaConsumer.assignment();
}

// 指定消费的offset
for (TopicPartition topicPartition : assignment) {
    kafkaConsumer.seek(topicPartition,600);
}

// 3  消费数据
while (true){

    ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(1));

    for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
        System.out.println(consumerRecord);
    }
}
```

##### 指定时间消费

 原理就是查到时间对应的offset再去指定位移消费，为了确保同步到分区信息，我们还需要确保能获取到分区，再去查询分区时间

```java
// 指定位置进行消费
Set<TopicPartition> assignment = kafkaConsumer.assignment();

//  保证分区分配方案已经制定完毕
while (assignment.size() == 0){
    kafkaConsumer.poll(Duration.ofSeconds(1));

    assignment = kafkaConsumer.assignment();
}

// 希望把时间转换为对应的offset
HashMap<TopicPartition, Long> topicPartitionLongHashMap = new HashMap<>();

// 封装对应集合
for (TopicPartition topicPartition : assignment) {
    topicPartitionLongHashMap.put(topicPartition,System.currentTimeMillis() - 1 * 24 * 3600 * 1000);
}

Map<TopicPartition, OffsetAndTimestamp> topicPartitionOffsetAndTimestampMap = kafkaConsumer.offsetsForTimes(topicPartitionLongHashMap);

// 指定消费的offset
for (TopicPartition topicPartition : assignment) {

    OffsetAndTimestamp offsetAndTimestamp = topicPartitionOffsetAndTimestampMap.get(topicPartition);

    kafkaConsumer.seek(topicPartition,offsetAndTimestamp.offset());
}

// 3  消费数据
while (true){

    ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(1));

    for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {

        System.out.println(consumerRecord);
    }
}
```



### 漏消费和重复消费

重复消费：已经消费了数据，但是 offset 没提交。
漏消费：先提交 offset 后消费，有可能会造成数据的漏消费。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191110531.png)



#### 消费者事务

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191111570.png)

### 数据积压（提高吞吐量）

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405191111584.png)

- fetch.max.bytes 默认Default: 52428800（50 m）。消费者获取服务器端一批消息最大的字节数。如果服务器端一批次的数据大于该值（50m）仍然可以拉取回来这批数据，因此，这不是一个绝对最大值。一批次的大小受 message.max.bytes （broker config）ormax.message.bytes （topic config）影响。
- max.poll.records 一次 poll 拉取数据返回消息的最大条数，默认是 500 条



### 拦截器

与生产者对应，消费者也有拦截器。我们来看看拦截器具体的方法。

```java
public interface ConsumerInterceptor<K, V> extends Configurable, AutoCloseable {

    ConsumerRecords<K, V> onConsume(ConsumerRecords<K, V> records);

    void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets);

    void close();
}
```

 Kafka Consumer会在poll()方法返回之前调用拦截器的onConsume()方法来对消息进行相应的定制化操作，比如修改返回的消息内容、按照某种规则过滤消息（可能会减少poll()方法返回 的消息的个数）。如果onConsume()方法中抛出异常， 那么会被捕获并记录到日志中， 但是异常不会再向上传递。

 Kafka Consumer会在提交完消费位移之后调用拦截器的**onCommit()**方法， 可以使用这个方法来记录跟踪所提交的位移信息，比如当消费者使用commitSync的无参方法时，我们不知道提交的消费位移的具体细节， 而使用拦截器的onCommit()方法却可以做到这 一点。











