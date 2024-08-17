---
title: 整合Kafka
category: 系统设计
tag:
  - SpringBoot
  - Kafka
---



![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406161834850.png)



## 引入依赖

```xml
<dependency>
	<groupId>org.springframework.kafka</groupId>
	<artifactId>spring-kafka</artifactId>
</dependency>
```



## SpringBoot生产者

修改 SpringBoot核心配置文件 application.propeties, 添加生产者相关信息

```xml
# 应用名称
spring.application.name =seven_springboot_kafka

# 指定 kafka 的地址
spring.kafka.bootstrap-servers =hadoop102:9092,hadoop103:9092,hadoop104:9092

指定 key 和 value 的序列化器
spring.kafka.producer.key-serializer =org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer =org.apache.kafka.common.serialization.StringSerializer
```



创建 controller从浏览器接收数据 , 并写入指定的 topic

```java
import org.springframework.beans.factory.annotation. Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation. RequestMapping;
import org.springframework.web.bind.annotation. RestController;
@RestController
public class Producer Controller{
    // Kafka 模板用来向 kafka 发送数据
    @Autowired
    KafkaTemplate<String, String> kafka;
    
    @RequestMapping ("/kafkaproduce")
    public String data (String msg ){
        kafka.send("first", msg)
        return "ok";
    }
}
```



## SpringBoot消费者

修改 SpringBoot核心配置文件 application.propeties

```xml
# 指定 kafka 的地址
spring.kafka.bootstrap-servers =hadoop102:9092,hadoop103:9092,hadoop104:9092

# 指定 key 和 value 的反序列化器
spring.kafka.consumer.key-deserializer =org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer =org.apache.kafka.common.serialization.StringDeserializer

# 指定消费者组的 group_id
spring.kafka.consumer.group-id = atguigu
```



创建类消费 Kafka中指定 topic的数据

```java
import org.springframework.context.annotation. Configuration;
import org.springframework.kafka.annotation. KafkaListener;
@Configuration
public class KafkaConsumer {
    // 指定要监听的topic
    @KafkaListener(topics = "first")
    public void consumeTopic(String msg) { // 参数: 收到的value
    	System.out.println("收到的信息: " + msg);
    }
}
```



<!-- @include: @article-footer.snippet.md -->     