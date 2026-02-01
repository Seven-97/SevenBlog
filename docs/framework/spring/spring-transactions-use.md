---
title: Spring事务管理使用篇
category: 常用框架
tags:
  - Spring
head:
  - - meta
    - name: keywords
      content: spring,spring的事务,@Transactional,@Transactional的失效场景
  - - meta
    - name: description
      content: 全网最全的Spring知识点总结，让天下没有难学的八股文！
---
## 事务介绍

事务是数据库中不可分割的操作序列，具有原子性、一致性、隔离性和持久性。要么全部执行成功，要么全部失败回滚，以保证数据的完整性和一致性。

事务的四个特性ACID：

- 原子性（Atomicity）：语句要么全执行，要么全不执行，是事务最核心的特性，事务本身就是以原子性来定义的；实现主要基于**undo log**
- 持久性（Durability）：保证事务提交后不会因为宕机等原因导致数据丢失；实现主要基于**redo log**
- 隔离性（Isolation）：数据库允许多个并发事务同时对其数据进行读写和修改的能力，隔离性保证事务执行尽可能不受其他事务影响；InnoDB默认的隔离级别是RR，RR的实现主要基于**锁机制**（包含next-key lock）、**MVCC**（包括数据的隐藏列、基于**undo log的版本链、ReadView**）
- 一致性（Consistency）：事务追求的最终目标，是指事务操作前和操作后，数据满足完整性约束，数据库保持一致性状态。一致性的实现既需要数据库层面的保障，也需要应用层面的保障


事务管理在系统开发中是不可缺少的一部分，Spring提供了很好事务管理机制

## 事务分类

主要分为编程式事务和声明式事务两种。

### 编程式事务

是指在代码中手动的管理事务的提交、回滚等操作，代码侵入性比较强，如下示例：

```java
try {
    //TODO something
    transactionManager.commit(status);
} catch (Exception e) {
    transactionManager.rollback(status);
    throw new InvoiceApplyException("异常失败");
}
```



### 声明式事务

基于AOP面向切面的，它将具体业务与事务处理部分解耦，代码侵入性很低，所以在实际开发中声明式事务用的比较多。声明式事务也有两种实现方式，一是基于TX和AOP的xml配置文件方式，第二种就是基于@Transactional注解了。

```java
@Transactional
@GetMapping("/test")
public String test() {
	int insert = cityInfoDictMapper.insert(cityInfoDict);
}
```

## Spring中事务实现


Spring 框架通过 AOP（面向切面编程）将事务管理逻辑从业务代码中分离，实现了声明式事务管理。其核心是 `PlatformTransactionManager` 接口，无论哪种配置方式，都离不开它

下面通过一个经典的“银行转账”场景，详解一下基于配置文件和基于注解的两种配置方式。

假设有 `AccountService`类，其中 `transfer`方法需要事务管理。

- 事务管理器与数据源

无论使用哪种方式，都必须先配置事务管理器和数据源。以下是基础的 XML 配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/tx
        http://www.springframework.org/schema/tx/spring-tx.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd">

    <!-- 1. 开启组件扫描，以便发现@Service等注解的Bean -->
    <context:component-scan base-package="com.example.service"/>

    <!-- 2. 配置数据源 (以简单的DriverManagerDataSource为例) -->
    <bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
        <property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/>
        <property name="url" value="jdbc:mysql://localhost:3306/test"/>
        <property name="username" value="root"/>
        <property name="password" value="password"/>
    </bean>

    <!-- 3. 配置JDBC事务管理器，并注入数据源 -->
    <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSource"/>
    </bean>

    <!-- 后续的特定配置将在这里进行 -->
</beans>
```

### 基于 XML 配置文件

这种方式将所有事务规则声明在 XML 中，实现了业务代码与事务配置的完全分离。

- 业务服务类 (`AccountServiceImpl.java`)：此类只关注业务逻辑，没有任何事务注解或代码，非常纯粹。

```java
package com.example.service.impl;

import com.example.dao.AccountDao;
import com.example.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("accountService") // 声明为Spring管理的Service Bean
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountDao accountDao;

    @Override
    public void transfer(String outAccount, String inAccount, Double amount) {
        // 纯粹的转账业务逻辑
        accountDao.decreaseMoney(outAccount, amount); // 转出
        accountDao.increaseMoney(inAccount, amount);  // 转入
        // 注意：此处没有事务控制的代码
    }
}
```

- 在 XML 中追加事务配置

在之前的基础配置后，添加 AOP 配置，将事务通知织入到匹配的方法上。

```xml
<!-- 接续上面的基础配置 -->

    <!-- 4. 配置事务通知 (Advice)，并关联事务管理器 -->
    <tx:advice id="txAdvice" transaction-manager="transactionManager">
        <tx:attributes>
            <!-- 
                配置具体的事务属性：
                name: 方法名匹配模式，如 transfer* 匹配以transfer开头的方法
                propagation: 传播行为，REQUIRED表示如果当前有事务则加入，没有则创建新事务
                rollback-for: 触发回滚的异常类型，默认是RuntimeException
            -->
            <tx:method name="transfer*" propagation="REQUIRED" rollback-for="Exception"/>
            <!-- 可以为其他方法设置不同的事务属性 -->
            <tx:method name="find*" read-only="true"/> 
        </tx:attributes>
    </tx:advice>

    <!-- 5. 配置AOP，将事务通知应用到指定的方法上 -->
    <aop:config>
        <!-- 定义切入点（哪些类的哪些方法），这里指定service包下所有类的所有方法 -->
        <aop:pointcut id="servicePointcut" expression="execution(* com.example.service.*.*(..))"/>
        <!-- 将事务通知与切入点关联 -->
        <aop:advisor advice-ref="txAdvice" pointcut-ref="servicePointcut"/>
    </aop:config>
```

### 基于注解

这种方式更为简洁，通过在Java代码上添加 `@Transactional`注解来声明事务规则，是当前的主流方式。

- 启用注解支持

首先，需要在XML配置中启用注解驱动的事务管理。

```xml
<!-- 接续最前面的基础配置 -->

    <!-- 4. 开启注解驱动的事务管理 -->
    <tx:annotation-driven transaction-manager="transactionManager"/>
```

或者，在纯Java配置类中，使用 `@EnableTransactionManagement`注解也能达到同样效果


- 使用注解的业务服务类 (`AccountServiceImpl.java`)

在类或方法上添加 `@Transactional`注解，定义事务行为。

```java
package com.example.service.impl;

import com.example.dao.AccountDao;
import com.example.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(
    propagation = Propagation.REQUIRED, // 传播行为：默认就是REQUIRED，可省略
    rollbackFor = Exception.class       // 回滚规则：发生任何Exception都回滚
)
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountDao accountDao;

    // 方法上的注解会覆盖类上的注解
    @Override
    public void transfer(String outAccount, String inAccount, Double amount) {
        accountDao.decreaseMoney(outAccount, amount);
        // 如果此处发生异常，整个事务将回滚，两次数据库操作都会撤销
        accountDao.increaseMoney(inAccount, amount);
    }

    // 查询方法可以设置为只读事务，数据库可能会进行优化
    @Transactional(readOnly = true)
    public Double checkBalance(String accountId) {
        return accountDao.findBalanceById(accountId);
    }
}
```



## 事务传播方式propagation

Spring的传播特性（行为）指的是：当一个事务方法调用另一个事务方法时，事务该如何进行。Spring的传播行为有七种，可以分为三大类类型。

场景：以下都会使用这个例子，方便理解；有A类内部有个a方法、B类内部有个b方法

```java
class ServiceA{
	void a(){
		b.b
	}
}

class ServiceB{
	void b(){
	}
}
```

### 支持当前事务

- REQUIRED：如果当前方法存在一个事务，则加入这个事务。若当前方法不存在事务，则新建一个事务。
	- 例：b方法的传播特性是REQUIRED，当a方法调用到b方法时，若a方法存在事务，则b方法加入这个事务，与a共用一个事务。若a方法不存在事务，则b方法新建一个事务。
	- 回滚判断：
		- 若a方法存在事务，即两个方法使用同一个事务，当a或者b任意一个方法出现异常，a和b都会回滚；
		- 若a方法没有事务，则a方法不会回滚；b方法有事务，会回滚。
	- **默认使用 REQUIRED**：满足大部分业务场景

- SUPPORTS：如果当前方法存在事务，则加入事务。反之，则以非事务方式运行。
	- 例：b方法的传播特性是SUPPORTS，当a方法调用到b方法时，若a方法存在事务，则b方法加入这个事务。若a方法不存在事务，则b方法以非事务进行。
	- **适用场景**：查询操作，可事务也可非事务执行

- MANDATORY：如果当前方法存在事务则加入事务，若不存在则抛出异常。
	- 例：b方法的传播特性是MANDATORY，当a方法调用到b方法时，若a方法存在事务，则b方法加入这个事务。若a方法不存在事务，则抛出异常。
	- **适用场景**：必须保证在事务中执行的核心业务方法


### 不支持当前事务

- REQUIRES_NEW：新建一个事务，如果当前方法存在事务，则把当前事务挂起。
	- 例：b方法的传播特性是REQUIRES_NEW，当a方法调用到b方法时，b方法新建一个事务，如果a方法存在事务，则把a方法事务挂起。
	- 回滚判断：要明白一点就是a和b的事务没有关系。
		- 当a没有事务时，只有b有事务，a方法不会回滚，b方法会回滚；
		- 当a有事务时，a和b也是不同的事务
			- 当a异常时，a会回滚，b已提交的操作不受影响
			- 当b方法出现异常时b会回滚。而a方法会不会回滚需要分为两种情况：
				- 若b中的异常自己捕获则a不会回滚
				- 若b中未捕获抛出异常，则a也会回滚。
	- **适用场景**：日志记录、审计操作等需要独立提交的业务


- NOT_SUPPORTED：以非事务方式执行，若存在当前事务，则把当前事务挂起。
	- 例：b方法的传播特性是NOT_SUPPORTED，当a方法调用到b方法时，b方法以非事务方式执行，若a方法存在事务，则挂起a方法事务。
	- **适用场景**：发送消息、记录非关键日志等

- NEVER：以非事务方式执行，若存在当前事务，则会抛出异常。
	- 例：b方法的传播特性是NEVER，当a方法调用到b方法时，b方法以非事务方式执行，若a方法存在事务，则抛出异常。
	- **适用场景**：必须非事务执行的方法，防止误用在事务中

### 嵌套事务

- NESTED：如果当前存在事务，则在嵌套事务内执行。如果当前没有事务，则执行与REQUIRED类似的操作。
	- 例：b方法的传播特性是NESTED，当a方法内调用到b方法时。若a方法存在事务，则在a方法内嵌套b方法的事务，两者是有联系的，b事务相当于a事务的子事务。若a方法不存在事务，则b方法新建一个事务。
	- 回滚判断：前者a和b都有事务，并且是相关联的事务（a的事务相当于父事务、b的事务相当于子事务）
		- 当a方法出现异常时，a和b都会回滚。
		- 当b方法出现异常时则要分两种情况分析：
			- 若b中的异常自己捕获则a不会回滚；
			- 若b中未捕获抛出异常，则a也会回滚。
	- **适用场景**：复杂的业务流程，部分操作可回滚不影响主流程


## 其他属性

### isolation 属性：隔离级别

isolation ：事务的隔离级别，默认值为 Isolation.DEFAULT。开发中基本都是 default 级别

- Isolation.DEFAULT：使用底层数据库默认的隔离级别
- Isolation.READ_COMMITTED：读已提交
- Isolation.READ_UNCOMMITTED：读未提交
- Isolation.REPEATABLE_READ：可重复读
- Isolation.SERIALIZABLE：串行化



**可能有朋友就会问了，如果应用程序配置的事务隔离级别与数据库默认隔离级别不一致时，会出现什么问题？**

当我们使用Spring配置了一个与数据库不一致的隔离级别（如SERIALIZABLE）时，Spring会在事务开始时设置数据库会话的隔离级别。具体来说，Spring会在事务开始时执行一条SQL语句来设置当前会话的事务隔离级别，例如：SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE

因此，在事务期间，数据库会话的隔离级别会被提升到SERIALIZABLE。这样，应用程序配置的隔离级别会生效，覆盖数据库的默认隔离级别。所以，不会出现两种隔离级别冲突的问题，因为Spring会确保数据库会话使用我们指定的隔离级别。但是，需要注意的是：

- 数据库必须支持所设置的隔离级别。如果数据库不支持SERIALIZABLE，那么设置会失败。
- 设置隔离级别需要相应的权限。如果没有权限，设置也会失败。
- 隔离级别的设置只对当前会话（或当前事务，取决于数据库的实现）有效，不会影响其他会话。

因此，当应用程序操作数据库时，事务会按照SERIALIZABLE隔离级别运行，不会出现因为数据库默认是REPEATABLE READ而导致的问题。

另外，如果数据库不支持SERIALIZABLE隔离级别，那么Spring会抛出异常。

也就是说，应用程序配置的隔离级别会覆盖数据库的默认隔离级别，事务会按照应用程序配置的隔离级别运行。不会出现不一致的问题，但要注意性能影响和数据库的支持情况。

### timeout 属性

timeout ：事务的超时时间。如果超过该时间限制但事务还没有完成，则自动回滚事务。默认值为 -1，默认不限制时间

### readOnly 属性

readOnly ：指定事务是否为只读事务，默认值为 false；为了忽略那些不需要事务的方法，比如读取数据，可以设置 read-only 为 true。

### rollbackFor 属性

rollbackFor ：用于指定能够触发事务回滚的异常类型，可以指定多个异常类型。

### noRollbackFor属性

noRollbackFor：抛出指定的异常类型，不回滚事务，也可以指定多个异常类型。



## @Transactional

### 作用范围

@Transactional 可以作用在接口、类、类方法。

- 作用于类：当把@Transactional 注解放在类上时，表示所有该类的public方法都配置相同的事务属性信息。
- 作用于方法：当类配置了@Transactional，方法也配置了@Transactional，方法的事务会覆盖类的事务配置信息。
- 作用于接口：不推荐这种使用方法，因为一旦标注在Interface上并且配置了Spring AOP 使用CGLib动态代理，将会导致@Transactional注解失效



### 错误使用场景

#### 无需事务的业务

在没有事务操作的业务方法上使用 @Transactional 注解；

例如，在只有查询的操作上，或者在只操作单表的情况下 使用 @Transactional 注解。

- 虽然对业务功能无影响，但从编码角度看是不规范的。其他开发者可能会认为该方法实际需要事务支持，从而增加理解代码的复杂性。
- @Transactional是通过动态代理实现的，每次调用带有 `@Transactional` 注解的方法时，事务管理器都会检查是否需要启动一个新事务，造成性能开销。虽然事务管理的开销在大多数现代数据库和应用服务器中相对较小，但仍然存在。

```java
@Transactional
public String testQuery() {
    standardBak2Service.getById(1L);
    return "testB";
}
```

#### 事务范围过大

有些同学为了省事直接将 @Transactional 注解加在了类上或者抽象类上，这样做导致的问题就是**类内的方法或抽象类的实现类中所有方法全部都被事务管理**。增加了不必要的性能开销或复杂性，建议按需使用，只在有事务逻辑的方法上加@Transactional。

以下是事务范围过大可能引发的问题及其相关处理建议：

- 锁竞争：事务范围过大可能会导致较长时间持有数据库锁，从而增加锁竞争的可能性。其他事务在等待锁释放期间，可能会导致响应时间变长或出现超时。
- [死锁](https://www.seven97.top/database/mysql/02-lock3-deadlock-mysql.html)：长时间持有锁的事务增加了死锁的风险。当多个事务相互持有对方需要的资源时，可能会陷入死锁状态。





### 失效场景

#### 应用在非 public 修饰的方法上

之所以会失效是因为@Transactional 注解依赖于Spring AOP切面来增强事务行为，这个 AOP 是通过代理来实现的

而无论是JDK动态代理还是CGLIB代理，Spring AOP的默认行为都是只代理`public`方法。



#### 被用 final 、static 修饰方法

和上边的原因类似，被用 `final` 、`static` 修饰的方法上加 @Transactional 也不会生效。

- static 静态方法属于类本身的而非实例，因此代理机制是无法对静态方法进行代理或拦截的
- final 修饰的方法不能被子类重写，事务相关的逻辑无法插入到 final 方法中，代理机制无法对 final 方法进行拦截或增强。



#### 同类中非事务方法调用事务方法

比如有一个类Test，它的一个方法A，A再调用本类的方法B（不论方法B是用public还是private修饰），但方法A没有声明注解事务，而B方法有。则外部调用方法A之后，方法B的事务是不会起作用的。但是如果是A声明了事务，A的事务是会生效的。

失效原因：事务是基于动态代理实现的，但本类中调用另一个方法默认是this调用关系，并非动态代理，故失效
解决方案：要么将操作移动到事务中，要么调用另一个Service中的事务方法



#### Bean 未被 spring 管理

上边我们知道 @Transactional 注解通过 AOP 来管理事务，而 AOP 依赖于代理机制。因此，**Bean 必须由Spring管理实例！** 要确保为类加上如 `@Controller`、`@Service` 或 `@Component`注解，让其被Spring所管理，这很容易忽视。



#### 异步线程调用

- 例1：

如果我们在 testMerge() 方法中使用异步线程执行事务操作，通常也是无法成功回滚的，来个具体的例子。

假设testMerge() 方法在事务中调用了 testA()，testA() 方法中开启了事务。接着，在 testMerge() 方法中，我们通过一个新线程调用了 testB()，testB() 中也开启了事务，并且在 testB() 中抛出了异常。此时，testA() 不会回滚 和 testB() 回滚。

testA() 无法回滚是因为没有捕获到新线程中 testB()抛出的异常；testB()方法正常回滚。

在多线程环境下，Spring 的事务管理器不会跨线程传播事务，事务的状态（如事务是否已开启）是存储在线程本地的 `ThreadLocal` 来存储和管理事务上下文信息。这意味着每个线程都有一个独立的事务上下文，事务信息在不同线程之间不会共享。



- 例2：

```java
@Transactional
public void transactionalMethod() {
    new Thread(() ->{
        Valuation v  = new Valuation();
        v.setUserName("张三");
        valuationMapper.insert(v);
    }).start();
}
```

Spring的事务是通过数据库连接来实现不同线程使用不同的数据库连接，且放在ThreadLocal中，基于同一个数据库连接的事务才能同时提交或回滚，多线程场景下，拿到的数据库连接不是同一个

解决方案：

1. 采用分布式事务保证 
2. 自己实现事务回滚



#### 数据库引擎不支持事务

事务能否生效数据库引擎是否支持事务是关键。常用的MySQL数据库默认使用支持事务的innodb引擎。一旦数据库引擎切换成不支持事务的myisam，那事务就从根本上失效了。



#### propagation 设置错误

若是错误的配置以下三种 propagation，事务将不会发生回滚。

- TransactionDefinition.PROPAGATION_SUPPORTS：如果当前存在事务，则加入该事务；如果当前没有事务，则以非事务的方式继续运行。

- TransactionDefinition.PROPAGATION_NOT_SUPPORTED：以非事务方式运行，如果当前存在事务，则把当前事务挂起。

- TransactionDefinition.PROPAGATION_NEVER：以非事务方式运行，如果当前存在事务，则抛出异常。



#### rollbackFor 设置错误

rollbackFor 可以指定能够触发事务回滚的异常类型。Spring 默认抛出了未检查unchecked异常（继承自 RuntimeException 的异常）或者 Error才回滚事务；其他异常（如IOException）不会触发回滚事务。如果在事务中抛出其他类型的异常，例如 `checked exceptions`（检查型异常），但却期望 Spring 能够回滚事务，就需要指定 rollbackFor属性。

失效原因：@Transactional 注解默认处理RuntimeException，即只有抛出运行时异常，才会触发事务回滚
解决方案：@Transactional 设置为 @Transactional(rollbackFor =Exception.class) 或者直接抛出运行时异常





#### 异常被 catch了

spring的事务是在调用业务方法之前开始的，业务方法执行完毕之后才执行commit or rollback，事务是否执行取决于是否抛出runtime异常。如果抛出runtime exception 并在你的业务方法中没有catch到的话，事务会回滚。

在业务方法中一般不需要catch异常，如果非要catch一定要抛出throw new RuntimeException()，或者注解中指定抛异常类型@Transactional(rollbackFor=Exception.class)，否则会导致事务失效，数据commit造成数据不一致，所以有些时候try catch反倒会画蛇添足。 

 

#### 嵌套事务问题

还有一种场景就是嵌套事务问题，比如，我们在 testMerge() 方法中调用了事务方法 testA() 和事务方法 testB()，此时不希望 testB() 抛出异常让整个 testMerge() 都跟着回滚；这就需要单独 try catch 处理 testB() 的异常，不让异常在向上抛。

```java
@RequiredArgsConstructor
@Slf4j
@Service
public class TestMergeService {

    private final TestBService testBService;

    private final TestAService testAService;
    @Transactional
    public String testMerge() {
    
        testAService.testA();

        try {
            testBService.testB();
        } catch (Exception e) {
            log.error("testMerge error:{}", e);
        }
        return "ok";
    }
}

@Service
public class TestAService {

    @Transactional
    public String testA() {
        standardBakService.save(entity);
        return "ok";
    }
}

@Service
public class TestBService {

    @Transactional
    public String testB() {
        standardBakService.save(entity2);
        
        throw new RuntimeException("test2");
    }
}
```



 <!-- @include: @article-footer.snippet.md -->     











