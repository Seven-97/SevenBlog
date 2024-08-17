---
title: Spring中的事务
category: 常用框架
tag:
  - Spring
---



 

事务管理在系统开发中是不可缺少的一部分，Spring提供了很好事务管理机制

## 分类

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



## 事务传播方式propagation

Spring的传播特性（行为）指的是：当一个事务方法调用另一个事务方法时，事务该如何进行。Spring的传播行为有七种，可以分为三大类类型。

场景：以下都会使用这个例子，方便理解；有A类内部有个a方法、B类内部有个b方法

### 支持当前事务

1. REQUIRED：如果当前方法存在一个事务，则加入这个事务。若当前方法不存在事务，则新建一个事务。
   例：b方法的传播特性是REQUIRED，当a方法调用到b方法时，若a方法存在事务，则b方法加入这个事务，与a共用一个事务。若a方法不存在事务，则b方法新建一个事务。回滚怎么判断：前者两个方法使用同一个事务，当a或者b任意一个方法出现异常，a和b都会回滚；后者a没有事务不会回滚，b方法有事务，会回滚。

2. SUPPORTS：如果当前方法存在事务，则加入事务。反之，则以非事务方式运行。
   例：b方法的传播特性是SUPPORTS，当a方法调用到b方法时，若a方法存在事务，则b方法加入这个事务。若a方法不存在事务，则b方法以非事务进行。

3. MANDATORY：如果当前方法存在事务则加入事务，若不存在则抛出异常。
   例：b方法的传播特性是MANDATORY，当a方法调用到b方法时，若a方法存在事务，则b方法加入这个事务。若a方法不存在事务，则抛出异常。



### 不支持当前事务

1. REQUIRES_NEW：新建一个事务，如果当前方法存在事务，则把当前事务挂起。
   例：b方法的传播特性是REQUIRES_NEW，当a方法调用到b方法时，b方法新建一个事务，如果a方法存在事务，则把a方法事务挂起。回滚怎么判断：要明白一点就是a和b的事务没有关系。当a没有事务时，只有b有事务，a方法不会回滚，b方法会回滚。后者a和b有不同事务，当b方法出现异常时b会回滚。而a方法会不会回滚需要分为两种情况：若b中的异常自己捕获则a不会回滚，若b中是是抛出异常，则a也会回滚。

2. NOT_SUPPORTED：以非事务方式执行，若存在当前事务，则把当前事务挂起。
   例：b方法的传播特性是NOT_SUPPORTED，当a方法调用到b方法时，b方法以非事务方式执行，若a方法存在事务，则挂起a方法事务。

3. NEVER：以非事务方式执行，若存在当前事务，则会抛出异常。
   例：b方法的传播特性是NEVER，当a方法调用到b方法时，b方法以非事务方式执行，若a方法存在事务，则抛出异常。



### 嵌套事务

NESTED：如果当前存在事务，则在嵌套事务内执行。如果当前没有事务，则执行与REQUIRED类似的操作。

例：b方法的传播特性是NESTED，当a方法内调用到b方法时。若a方法存在事务，则在a方法内嵌套b方法的事务，两者是有联系的，b事务相当于a事务的子事务。若a方法不存在事务，则b方法新建一个事务。回滚怎么判断：前者a和b都有事务，并且是相关联的事务（a的事务相当于父事务、b的事务相当于子事务），当a方法出现异常时，a和b都会回滚。当b方法出现异常时则要分两种情况分析：若b中的异常自己捕获则a不会回滚，若b中是是抛出异常，则a也会回滚。



## 其他属性

### isolation 属性

isolation ：事务的隔离级别，默认值为 Isolation.DEFAULT。

- Isolation.DEFAULT：使用底层数据库默认的隔离级别

- Isolation.READ_COMMITTED：读已提交

- Isolation.READ_UNCOMMITTED：读未提交

- Isolation.REPEATABLE_READ：可重复读

- Isolation.SERIALIZABLE：串行化

### timeout 属性

timeout ：事务的超时时间，。如果超过该时间限制但事务还没有完成，则自动回滚事务。默认值为 -1，默认不限制时间

### readOnly 属性

readOnly ：指定事务是否为只读事务，默认值为 false；为了忽略那些不需要事务的方法，比如读取数据，可以设置 read-only 为 true。

### rollbackFor 属性

rollbackFor ：用于指定能够触发事务回滚的异常类型，可以指定多个异常类型。

### noRollbackFor属性

noRollbackFor：抛出指定的异常类型，不回滚事务，也可以指定多个异常类型。

## @Transactional

@Transactional 可以作用在接口、类、类方法。

- 作用于类：当把@Transactional 注解放在类上时，表示所有该类的public方法都配置相同的事务属性信息。

- 作用于方法：当类配置了@Transactional，方法也配置了@Transactional，方法的事务会覆盖类的事务配置信息。

- 作用于接口：不推荐这种使用方法，因为一旦标注在Interface上并且配置了Spring AOP 使用CGLib动态代理，将会导致@Transactional注解失效

### 失效场景

#### 应用在非 public 修饰的方法上

之所以会失效是因为在Spring AOP 代理时，如上图所示 TransactionInterceptor （事务拦截器）在目标方法执行前后进行拦截，DynamicAdvisedInterceptor（CglibAopProxy 的内部类）的 intercept 方法或 JdkDynamicAopProxy 的 invoke 方法会间接调用 AbstractFallbackTransactionAttributeSource的 computeTransactionAttribute 方法，获取Transactional 注解的事务配置信息。

#### propagation 设置错误

若是错误的配置以下三种 propagation，事务将不会发生回滚。

- TransactionDefinition.PROPAGATION_SUPPORTS：如果当前存在事务，则加入该事务；如果当前没有事务，则以非事务的方式继续运行。

- TransactionDefinition.PROPAGATION_NOT_SUPPORTED：以非事务方式运行，如果当前存在事务，则把当前事务挂起。

- TransactionDefinition.PROPAGATION_NEVER：以非事务方式运行，如果当前存在事务，则抛出异常。

#### rollbackFor 设置错误

rollbackFor 可以指定能够触发事务回滚的异常类型。Spring默认抛出了未检查unchecked异常（继承自 RuntimeException 的异常）或者 Error才回滚事务；其他异常（如IOException）不会触发回滚事务。如果在事务中抛出其他类型的异常，但却期望 Spring 能够回滚事务，就需要指定 rollbackFor属性。

#### 同一个类中方法调用

比如有一个类Test，它的一个方法A，A再调用本类的方法B（不论方法B是用public还是private修饰），但方法A没有声明注解事务，而B方法有。则外部调用方法A之后，方法B的事务是不会起作用的。

那为啥会出现这种情况？其实这还是由于使用Spring AOP代理造成的，因为只有当事务方法被当前类以外的代码调用时，才会由Spring生成的代理对象来管理。

#### 异常被 catch了

spring的事务是在调用业务方法之前开始的，业务方法执行完毕之后才执行commit or rollback，事务是否执行取决于是否抛出runtime异常。如果抛出runtime exception 并在你的业务方法中没有catch到的话，事务会回滚。

在业务方法中一般不需要catch异常，如果非要catch一定要抛出throw new RuntimeException()，或者注解中指定抛异常类型@Transactional(rollbackFor=Exception.class)，否则会导致事务失效，数据commit造成数据不一致，所以有些时候try catch反倒会画蛇添足。

#### 数据库引擎不支持事务

事务能否生效数据库引擎是否支持事务是关键。常用的MySQL数据库默认使用支持事务的innodb引擎。一旦数据库引擎切换成不支持事务的myisam，那事务就从根本上失效了。

 

 

 

 <!-- @include: @article-footer.snippet.md -->     











