---
title: Java缓存规范
category: 系统设计
tag:
 - 缓存
---

> 来源：稀土掘金社区[深入理解缓存原理与实战设计](https://juejin.cn/column/7140852038258147358)，Seven进行了部分补充完善



## 为何需要规范

上一章中构建的最简化版本的缓存框架，虽然可以使用，但是也存在一个问题，就是它对外提供的实现接口都是框架根据自己的需要而自定义的。这样一来，项目集成了此缓存框架，后续如果想要更换缓存框架的时候，业务层面的改动会比较大。 —— 因为是自定义的框架接口，无法基于[`里氏替换`](https://www.seven97.top/system-design/design-pattern/overviewofdesignpatterns.html#里氏代换原则)原则来进行灵活的更换。

在业界各大厂商或者开源团队都会构建并提供一些自己实现的缓存框架或者组件，提供给开发者按需选择使用。如果大家都是各自**闭门造车**，势必导致业务中集成并使用某一缓存实现之后，想要更换缓存实现组件会难于登天。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406051741572.webp)

千古一帝秦始皇统一天下后，颁布了*书同文、车同轨*等一系列法规制度，使得所有的车辆都遵循统一的轴距，然后都可以在官道上正常的通行，大大提升了流通性。而正所谓“国有国法、行有行规”，为了保证缓存框架的通用性、提升项目的可移植性，JAVA行业也迫切需要这么一个**缓存规范**，来约束各个缓存提供商给出的缓存框架都遵循相同的规范接口，业务中按照标准接口进行调用，无需与缓存框架进行深度耦合，使得缓存组件的更换成为一件简单点的事情。

在JAVA的缓存领域，流传比较广泛的主要是`JCache API`和`Spring Cache`两套规范，下面就一起来看下。



## 虽迟但到的JSR107 —— JCache API

提到JAVA中的“行业规矩”，`JSR`是一个绕不开的话题。它的全称为`Java Specification Requests`，意思是**JAVA规范提案**。在该规范标准中，有公布过一个关于JAVA缓存体系的规范定义，也即`JSR 107`规范（*JCache API*），主要明确了JAVA中基于内存进行对象缓存构建的一些要求，涵盖内存对象的*创建*、*查询*、*更新*、*删除*、*一致性保证*等方面内容。

**JSR107**规范早在`2012年`时草案就被提出，但却直到`2014年`才正式披露首个规范版本，也即`JCache API 1.0.0`版本，至此JAVA领域总算是有个正式的关于缓存的官方规范要求。



### 揭秘JSR107 —— JCache API内容探究

**JSR107**规范具体的要求形式，都以接口的形式封装在`javax.cache`包中进行提供。我们要实现的缓存框架需要遵循该规范，也就是需要引入*javax.cache*依赖包，并实现其中提供的相关接口即可。对于使用maven构建的项目中，可以在`pom.xml`中引入javax.cache依赖：

```xml
<dependency>
    <groupId>javax.cache</groupId>
    <artifactId>cache-api</artifactId>
    <version>1.1.1</version>
</dependency>
```

在`JCache API`规范中，定义的缓存框架相关接口类之间的关系逻辑梳理如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406051745406.webp)

我们要实现自己的本地缓存框架，也即需要实现上述各个接口。对上述各接口类的含义介绍说明如下：

| 接口类          | 功能定位描述                                                 |
| --------------- | ------------------------------------------------------------ |
| CachingProvider | **SPI接口**，缓存框架的加载入口。每个`Provider`中可以持有1个或者多个`CacheManager`对象，用来提供不同的缓存能力 |
| CacheManager    | 缓存管理器接口，每个缓存管理器负责对具体的缓存容器的创建与管理，可以管理1个或者多个不同的`Cache`对象 |
| Cache           | `Cache`缓存容器接口，负责存储具体的缓存数据，可以提供不同的容器能力 |
| Entry           | `Cache`容器中存储的`key-value`键值对记录                     |

作为通用规范，这里将`CachingProvider`定义为了一个[**SPI接口**（`Service Provider Interface`，服务提供接口）](https://www.seven97.top/java/basis/06-SPI.html)，主要是借助JDK自带的服务提供发现能力，来实现按需加载各自实现的功能逻辑，有点`IOC`的意味。这样设计有一定的好处：

- **对于框架**：需要遵循规范，提供上述接口的实现类。然后可以实现热插拔，与业务解耦。

- **对于业务**：先指定需要使用的`SPI`的具体实现类，然后业务逻辑中便无需感知缓存具体的实现，直接基于`JCache API`通用接口进行使用即可。后续如果需要更换缓存实现框架，只需要切换下使用的`SPI`的具体实现类即可。

根据上述介绍，一个基于**JCache API**实现的缓存框架在实际项目中使用时的对象层级关系可能会是下面这种场景（假设使用*LRU策*略存储部门信息、使用*普通策略*存储用户信息）：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406051745450.webp)

那么如何去理解`JCache API`中几个接口类的关系呢？

几个简单的说明：

1. **CachingProvider** 并无太多实际逻辑层面的功能，只是用来基于SPI机制，方便项目中集成插拔使用。内部持有CacheManager对象，实际的缓存管理能力，由CacheManager负责提供。
2. **CacheManager** 负责具体的缓存管理相关能力实现，实例由`CachingProvider`提供并持有，CachingProvider可以持有一个或者多个不同的`CacheManager`对象。这些CacheManager对象可以是相同类型，也可以是不同类型，比如我们可以实现2种缓存框架，一种是`基于内存`的缓存，一种是`基于磁盘`的缓存，则可以分别提供两种不同的*CacheManager*，供业务按需调用。
3. **Cache** 是CacheManager负责创建并管理的具体的缓存容器，也可以有一个或者多个，如业务中会涉及到为用户列表和部门列表分别创建独立的`Cache`存储。此外，Cache容器也可以根据需要提供不同的Cache容器类型，以满足不同场景对于缓存容器的不同诉求，如我们可以实现一个类似`HashMap`的普通键值对Cache容器，也可以提供一个基于`LRU`淘汰策略的Cache容器。

至此呢，我们厘清了**JCache API**规范的大致内容。



### JCache API规范的实现

**JSR**作为JAVA领域正统行规，制定的时候往往考虑到各种可能的灵活性与通用性。作为JSR中根正苗红的`JCache API`规范，也沿袭了这一风格特色，框架接口的定义与实现也非常的丰富，几乎可以扩展自定义任何你需要的处理策略。 —— 但恰是这一点，也让其整个框架的接口定义过于**重量级**。对于缓存框架实现者而言，遵循`JCache API`需要实现众多的接口，需要做很多额外的实现处理。

比如，我们实现`CacheManager`的时候，需要实现如下这么多的接口：

```java
public class MemCacheManager implements CacheManager {
    private CachingProvider cachingProvider;
    private ConcurrentHashMap<String, Cache> caches;
    public MemCacheManager(CachingProvider cachingProvider, ConcurrentHashMap<String, Cache> caches) {
        this.cachingProvider = cachingProvider;
        this.caches = caches;
    }
    @Override
    public CachingProvider getCachingProvider() {
    }
    @Override
    public URI getURI() {
    }
    @Override
    public ClassLoader getClassLoader() {
    }
    @Override
    public Properties getProperties() {
    }
    @Override
    public <K, V, C extends Configuration<K, V>> Cache<K, V> createCache(String s, C c) throws IllegalArgumentException {
    }
    @Override
    public <K, V> Cache<K, V> getCache(String s, Class<K> aClass, Class<V> aClass1) {
    }
    @Override
    public <K, V> Cache<K, V> getCache(String s) {
    }
    @Override
    public Iterable<String> getCacheNames() {
    }
    @Override
    public void destroyCache(String s) {
    }
    @Override
    public void enableManagement(String s, boolean b) {
    }
    @Override
    public void enableStatistics(String s, boolean b) {
    }
    @Override
    public void close() {
    }
    @Override
    public boolean isClosed() {
    }
    @Override
    public <T> T unwrap(Class<T> aClass) {
    }
}
```

长长的一摞接口等着实现，看着都**令人上头**，作为缓存提供商，便需要按照自己的能力去实现这些接口，以保证相关缓存能力是按照规范对外提供。也正是因为JCache API这种不接地气的表现，让其虽是JAVA 领域的正统规范，却经常被*束之高阁*，沦落成为了一种名义规范。业界主流的本地缓存框架中，比较出名的当属`Ehcache`了（当然，`Spring4.1`中也增加了对JSR规范的支持）。此外，**Redis**的本地客户端`Redisson`也有实现全套JCache API规范，用户可以基于Redisson调用JCache API的标准接口来进行缓存数据的操作。



### JSR107提供的注解操作方法

前面提到了作为供应商想要实现*JSR107*规范的时候会比较复杂，需要做很多自己的处理逻辑。但是对于业务使用者而言，JSR107还是比较贴心的。比如JSR107中就将一些常用的API方法封装为`注解`，利用注解来大大简化编码的复杂度，降低缓存对于业务逻辑的*侵入性*，使得业务开发人员可以更加专注于业务本身的开发。

`JSR107`规范中常用的一些缓存操作注解方法梳理如下面的表格：

| 注解            | 含义说明                                                     |
| --------------- | ------------------------------------------------------------ |
| @CacheResult    | 将指定的`key`和`value`映射内容存入到缓存容器中               |
| @CachePut       | 更新指定缓存容器中指定`key`值缓存记录内容                    |
| @CacheRemove    | 移除指定缓存容器中指定`key`值对应的缓存记录                  |
| @CacheRemoveAll | 字面含义，移除指定缓存容器中的所有缓存记录                   |
| @CacheKey       | 作为接口参数前面修饰，用于指定特定的入参作为缓存`key`值的组成部分 |
| @CacheValue     | 作为接口参数前面的修饰，用于指定特定的入参作为缓存`value`值  |

上述注解主要是添加在方法上面，用于自动将方法的入参与返回结果之间进行一个映射与自动缓存，对于后续请求如果命中缓存则直接返回缓存结果而无需再次执行方法的具体处理，以此来提升接口的响应速度与承压能力。

比如下面的查询接口上，通过`@CacheResult`注解可以将查询请求与查询结果缓存起来进行使用：

```java
@CacheResult(cacheName = "books")
public Book findBookByName(@CacheKey String bookName) {
    return bookDao.queryByName(bookName);
}
```

当**Book**信息发生变更的时候，为了保证缓存数据的准确性，需要同步更新缓存内容。可以通过在更新方法上面添加`@CachePut`接口即可达成目的：

```java
@CachePut(cacheName = "books")
public void updateBookInfo(@CacheKey String bookName, @CacheValue Book book) {
    bookDao.updateBook(bookName, book);
}
```

这里分别使用了`@CacheKey`和`@CacheValue`指定了需要更新的缓存记录key值，以及需要将其更新为的新的value值。

同样地，借助注解`@CacheRemove`可以完成对应缓存记录的删除：

```java
@CacheRemove(cacheName = "books")
public void deleteBookInfo(@CacheKey String bookName) {
    bookDao.deleteBookByName(bookName)
}
```



## 爱屋及乌 —— Spring框架制定的Cache规范

JSR 107（JCache API）规范的诞生可谓是一路坎坷，拖拖拉拉直到**2014**年才发布了首个`1.0.0`版本规范。但是在JAVA界风头无两的**Spring**框架早在`2011`年就已经在其3.1版本中提供了缓存抽象层的规范定义，并借助Spring的优秀设计与良好生态，迅速得到了各个软件开发团体的青睐，各大缓存厂商也陆续提供了符合`Spring Cache`规范的自家缓存产品。

**Spring Cache**并非是一个具体的缓存实现，而是和JSR107类似的一套*缓存规范*，基于注解并可实现与Spring的各种高级特性无缝集成，受到了广泛的追捧。各大缓存提供商几乎都有基于Spring Cache规范进行实现的缓存组件。比如后面我们会专门介绍的`Guava Cache`、`Caffeine Cache`以及同样支持JSR107规范的`Ehcache`等等。

得力于Spring在JAVA领域无可撼动的地位，造就了**Spring Cache**已成为JAVA缓存领域的“事实标准”，深有“*功高盖主*”的味道。

### Spring Cache使用不同缓存组件

如果要基于`Spring Cache`规范来进行缓存的操作，首先在项目中需要引入此规范的定义：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

这样，在业务代码中，就可以使用Spring Cache规范中定义的一些注解方法。前面有提过，*Spring Cache只是一个规范声明*，可以理解为一堆接口定义，而并没有提供具体的接口功能实现。具体的功能实现，由业务根据实际选型需要，引入相应缓存组件的jar库文件依赖即可 —— 这一点是Spring框架中极其普遍的一种做法。

假如我们需要使用`Guava Cache`来作为我们实际缓存能力提供者，则我们只需要引入对应的依赖即可：

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>30.1.1-jre</version>
</dependency>
```

这样一来，我们便实现了使用Guava cache作为存储服务提供者、且基于Spring Cache接口规范进行缓存操作。Spring作为JAVA领域的一个相当优秀的框架，得益于其优秀的**封装**设计思想，使得更换缓存组件也显得非常容易。比如现在想要将上面的*Guava cache*更换为`Caffeine cache`作为新的缓存能力提供者，则业务代码中将依赖包改为Caffeine cache并简单的做一些细节配置即可：

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.1.1</version>
</dependency>
```

这样一来，对于业务使用者而言，可以方便的进行缓存具体实现者的替换。而作为缓存能力提供商而言，自己可以轻易的被同类产品替换掉，所以也鞭策自己去提供更好更强大的产品，巩固自己的地位，也由此促进整个生态的**良性演进**。



### Spring Cache规范提供的注解

需要注意的是，使用Spring Cache缓存前，需要先手动开启对于缓存能力的支持，可以通过`@EnableCaching`注解来完成。

除了 *@EnableCaching* ，在Spring Cache中还定义了一些其它的常用注解方法，梳理归纳如下：

| 注解           | 含义说明                                                     |
| -------------- | ------------------------------------------------------------ |
| @EnableCaching | 开启使用缓存能力                                             |
| @Cacheable     | 添加相关内容到缓存中                                         |
| @CachePut      | 更新相关缓存记录                                             |
| @CacheEvict    | 删除指定的缓存记录，如果需要清空指定容器的全部缓存记录，可以指定`allEntities=true`来实现 |

具体的使用上，其实和JSR107规范中提供的注解用法相似。

当然了，JAVA领域缓存事实规范地位虽已奠定，但是Spring Cache依旧是保持着一个兼收并蓄的姿态，并积极的兼容了JCache API相关规范，比如`Spring4.1`起项目中可以使用JSR107规范提供的相关注解方法来操作。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406051807812.webp)









<!-- @include: @article-footer.snippet.md -->     



