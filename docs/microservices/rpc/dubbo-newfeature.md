---
title: dubbo - 3.0新特性
category: 微服务
tag:
 - dubbo
---

## 注册模型的改变

在服务注册领域，市面上有两种模型，一种是应用级注册，一种是接口级注册

在Spring Cloud中， 一个应用是一个微服务，而在Dubbo2.7中，一个接口是一个微服务。

 所以，Spring Cloud在进行服务注册时，是把应用名以及应用所在服务器的IP地址和应用所绑定的端 口注册到注册中心，相当于key是应用名，value是ip+port，而在Dubbo2.7中，是把接口名以及对应 应用的IP地址和所绑定的端口注册到注册中心，相当于key是接口名，value是ip+port。 所以在Dubbo2.7中，一个应用如果提供了10个Dubbo服务，那么注册中心中就会存储10对 keyvalue，而Spring Cloud就只会存一对keyvalue，所以以Spring Cloud为首的应用级注册是更加适 合的。 

而随着功能越来越多，一个应用势必会有越来越多的接口，这样存到注册中心的数据会更多，这样就会导致注册中心的压力越来越大。

所以Dubbo3.0 中将注册模型也改为了应用级注册，提升效率节省资源的同时，通过统一注册模型，也为各个微服务框架的互通打下了基础。

```java
dubbo.application.register-mode=instance|interface|all
instance：应用级注册
interface：接口级注册
all：都注册
```



## 新一代RPC协议-Triple协议

在发送数据时，一般都有一个协议格式进行发送

如果通过HTTP1.1协议，那实现起来就比较方便，把序列化标记放在请求头，Invocation对象序列化之后的结果放在请求体，服务端收到HTTP请求后，就先解析请求头得到序列化标记，再取请求体进行反序列化。

比如HTTP协议就描述了，从第一个字节开始，遇到第一个空格符时，那就是表示前面每个字节对应的字符组合成的字符串就表示请求方法（字符编码为ascii，一个字符对应一个字节），紧接着继续解析字节，将会按HTTP协议格式解析出请求行、请求头，解析完请求头之后，取出content-length对应的value，该value表示请求体的字节数，所以直接再获取content-length个字节，就表示获取到了请求体（Invocation对象序列化之后的字节），从而一个HTTP请求就获取出来，下一个字节就是另外一个HTTP请求了。

但是Dubbo觉得用HTTP1.x协议性能太低了，因此dubbo协议就有了自己的格式，比如：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404301944618.png)

dubbo协议在Dubbo框架内使用还是比较舒服的，并且dubbo协议相比于http1.x协议，性能会更好，因为请求中没有多余的无用的字节，都是必要的字节，并且每个Dubbo请求和响应中都有一个请求ID，这样可以基于一个Socket连接同时发送多个Dubbo请求，不用担心请求和响应对不上，所以dubbo协议成为了Dubbo框架中的默认协议。

但是dubbo协议一旦涉及到跨RPC框架，比如一个Dubbo服务要调用gPRC服务，就比较麻烦了，因为发一个dubbo协议的请求给一个gPRC服务，gPRC服务只会按照gRPC的格式来解析字节流，最终肯定会解析不成功的。如果需要使用，就需要进行格式转换

dubbo协议虽好，但是不够通用，所以这就出现了Triple协议，Triple协议是基于HTTP2，没有性能问题，另外HTTP协议非常通用，全世界都认它，兼容起来也比较简单，而且还有很多额外的功能，比如流式调用。

大概对比一下triple、dubbo、rest这三个协议

1. triple协议基于的是HTTP2，rest协议目前基于的是HTTP1，都可以做到跨语言。
2. triple协议兼容了gPRC（Triple服务可以直接调用gRPC服务，反过来也可以），rest协议不行
3. triple协议支持流式调用，rest协议不行
4. rest协议更方便浏览器、客户端直接调用，triple协议不行（原理上支持，当得对triple协议的底层实现比较熟悉才行，得知道具体的请求头、请求体是怎么生成的）
5. dubbo协议是Dubbo3.0之前的默认协议，triple协议是Dubbo3.0之后的默认协议，优先用Triple协议
6. dubbo协议不是基于的HTTP，不够通用，triple协议底层基于HTTP所以更通用（比如跨语言、跨异构系统实现起来比较方便）
7. dubbo协议不支持流式调用



## 跨语言调用

在工作中，我们用Java语言通过Dubbo提供了一个服务，另外一个应用（也就是消费者）想要使用这个服务，如果消费者应用也是用Java语言开发的，那没什么好说的，直接在消费者应用引入Dubbo和服务接口相关的依赖即可。

但是，如果消费者应用不是用Java语言写的呢，比如是通过python或者go语言实现的，那就至少需要满足两个条件才能调用Java实现的Dubbo服务：

Dubbo一开始是用Java语言实现的，那现在就需要一个go语言实现的Dubbo框架，也就是现在的dubbo-go，然后在go项目中引入dubbo-go，从而可以在go项目中使用dubbo，比如使用go语言去暴露和使用Dubbo服务。

在使用Java语言开发一个Dubbo服务时，会把服务接口和相关类，单独抽象成为一个Maven项目，实际上就相当于一个单独的jar包，这个jar能被Java项目所使用，但不能被go项目所使用，所以go项目中该如何使用Java语言所定义的接口呢？直接用是不太可能的，只能通过间接的方式来解决这个问题，除开Java语言之外，那有没有其他技术也能定义接口呢？并且该技术也是Java和go都支持，这就是protobuf


<!-- @include: @article-footer.snippet.md -->     
