---
title: Spring事务管理源码篇
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

## 解析JDBC操作事务的本质

### 无事务操作

以下代码模拟了一次转账操作：从 `Seven`的账户向 `Eight`的账户转账 200 元。关键在于，我们**没有开启事务**，并且在两次更新操作之间人为制造了一个异常：

```java
public class NoTransactionExample {
    public static void main(String[] args) throws ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver"); // 1. 注册驱动
        
        String url = "jdbc:mysql://localhost:3306/your_database";
        String user = "seven";
        String password = "seven666";
        
        // 假设账户表初始状态：Seven有1000元，Eight有1900元
        String deductMoneySQL = "UPDATE accounts SET balance = balance - 200 WHERE name = ?"; // 扣款SQL
        String addMoneySQL = "UPDATE accounts SET balance = balance + 200 WHERE name = ?"; // 加款SQL
        
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement deductStmt = connection.prepareStatement(deductMoneySQL);
             PreparedStatement addStmt = connection.prepareStatement(addMoneySQL)) {
            
            // 默认情况下，connection.setAutoCommit(true)，即自动提交
            
            // 执行第一条SQL：从Seven账户扣款200元
            deductStmt.setString(1, "Seven");
            deductStmt.executeUpdate();
            System.out.println("已从Seven账户扣除200元。");
            
            // ！！！模拟在第二次更新前发生异常（如系统故障、业务逻辑异常等）
            int i = 1 / 0; // 这将抛出ArithmeticException异常
            
            // 执行第二条SQL：向Eight账户增加200元（因异常，此句不会执行）
            addStmt.setString(1, "Eight");
            addStmt.executeUpdate();
            System.out.println("已向Eight账户增加200元。");
            
            // 如果没有异常，两条SQL都会各自被自动提交
        } catch (Exception e) {
            e.printStackTrace();
            // 捕获到异常，但为时已晚，第一条UPDATE操作已被自动提交！
        }
        // 最终结果：Seven的账户少了200元，但Eight的账户并未收到钱，数据出现不一致。
    }
}
```

运行上述代码后，你会在控制台看到异常信息，但更重要的是，去数据库检查 `accounts`表，会发现：
- **`Seven`的余额变成了 800 元**（成功扣款 200）。
- **`Eight`的余额仍然是 1900 元**（并未收到钱）。

**问题的核心**在于 JDBC 连接的默认设置是 `setAutoCommit(true)`。这意味着**每执行一条 SQL 语句，都会立即被当作一个独立的事务提交到数据库**，无法撤销

### 解决方案：使用事务管理

要避免这种问题，核心是**将多个相关的数据库操作绑定成一个原子单位**。以下是正确的做法：

```java
// ... 省略相同的初始化代码 ...
try (Connection connection = DriverManager.getConnection(url, user, password);
     PreparedStatement deductStmt = connection.prepareStatement(deductMoneySQL);
     PreparedStatement addStmt = connection.prepareStatement(addMoneySQL)) {
    
    // 1. 关键步骤：关闭自动提交，开启事务
    connection.setAutoCommit(false);
    
    // 2. 执行两条SQL语句
    deductStmt.setString(1, "Seven");
    deductStmt.executeUpdate();
    
    //int i = 1 / 0; // 模拟异常
    
    addStmt.setString(1, "Eight");
    addStmt.executeUpdate();
    
    // 3. 如果所有操作成功，手动提交事务
    connection.commit();
    System.out.println("转账成功！");
    
} catch (Exception e) {
    e.printStackTrace();
    // 4. 如果发生异常，回滚事务，撤销所有操作
    if (connection != null) {
        try {
            connection.rollback();
            System.out.println("事务已回滚，所有操作被撤销。");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
// 此时，要么转账完全成功，要么双方余额均无变化，数据始终保持一致。
```


### 基于JDK代理的方式实现事务管理



### 基于AspectJ方式自定义事务实现






## Spring事务源码分析

以下源码均基于Spring4.3.12版本。主要从 创建事务、开启事务、提交事务、事务回滚 的维度来详细分析声明式事务。

### 事务简易流程图

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202411160956109.png)

### 代理类生成

在Spring框架中，当配置了事务管理器并声明了@Transactional注解时，Spring会在实例化bean时生成事务增强的代理类。创建代理类参考源码路径如下：

```java
AbstractAutowireCapableBeanFactory.createBean=>
            doCreateBean()=>
            initializeBean()=>
            applyBeanPostProcessorsAfterInitialization()=>
            postProcessAfterInitialization()（BeanPostProcessor内接口）=> 
      AbstractAutoProxyCreator.postProcessAfterInitialization()=>
            wrapIfNecessary()=>
            createProxy() 中  proxyFactory.setProxyTargetClass(true); //是否对类进行代理的设置，true为cglib代理
```

### 代理类中方法执行入口

从`TransactionInterceptor.invoke()`方法开始分析 (获取代理类，调用父类`TransactionAspectSupport.invokeWithinTransaction()`方法，该方法会将代理类的方法纳入事务中)。

```java
public class TransactionInterceptor extends TransactionAspectSupport implements MethodInterceptor, Serializable {

    public Object invoke(final MethodInvocation invocation) throws Throwable {
        // 返回代理类的目标类
        Class<?> targetClass = invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null;
        //事务中执行被代理的方法
        return this.invokeWithinTransaction(invocation.getMethod(), targetClass, new InvocationCallback() {
            public Object proceedWithInvocation() throws Throwable {
                return invocation.proceed();
            }
        });
    }
}
```



### 主要核心逻辑

`TransactionAspectSupport.invokeWithinTransaction()`方法负责获取事务属性和事务管理器，然后针对声明式事务和编程式事务区分处理流程（此处源码忽略编程式事务）。

```java
protected Object invokeWithinTransaction(Method method, Class<?> targetClass, final TransactionAspectSupport.InvocationCallback invocation) throws Throwable {
        // 获取事务属性 TransactionDefinition对象（回顾规则，隔离级别，只读等）
        final TransactionAttribute txAttr = this.getTransactionAttributeSource().getTransactionAttribute(method, targetClass);
        // 根据事务属性和方法，获取对应的事务管理器，（后续用于做事务的提交，回滚等操作），数据库的一些信息，
        final PlatformTransactionManager tm = this.determineTransactionManager(txAttr);
        // 获取事务方法全路径，
        final String joinpointIdentification = this.methodIdentification(method, targetClass, txAttr);
        //响应式编程事务，大多数情况下都会执行到 else中的语句；
        // CallbackPreferringPlatformTransactionManager 可以通过回掉函数来处理事务的提交和回滚操作， 此处不考虑，此处源码可以忽略
        if (txAttr != null && tm instanceof CallbackPreferringPlatformTransactionManager) {
        // 此处省略，此处为编程式事务 处理逻辑
        } else {
            //创建事务，事务属性等信息会被保存进 TransactionInfo,便于后续流程中的提交和回滚操作，详情见下文
            TransactionAspectSupport.TransactionInfo txInfo = this.createTransactionIfNecessary(tm, txAttr, joinpointIdentification);
            Object retVal = null;
            try {
                // 执行目标的方法 （执行具体的业务逻辑）
                retVal = invocation.proceedWithInvocation();
            } catch (Throwable var15) {
                //异常处理
                this.completeTransactionAfterThrowing(txInfo, var15);
                throw var15;
            } finally {
                //清除当前节点的事务消息，将旧事务节点消息通过ThreadLoacl更新到当前线程（事务的挂起操作就是在这执行）
                this.cleanupTransactionInfo(txInfo);
            }
            //提交事务
            this.commitTransactionAfterReturning(txInfo);
            return retVal;
        }
    }
    
```

### 开启事务

`TransactionAspectSupport.createTransactionIfNecessary()` 方法作用是检查当前是否存在事务，如果存在，则根据一定的规则创建一个新的事务。

```java
protected TransactionAspectSupport.TransactionInfo createTransactionIfNecessary(PlatformTransactionManager tm, TransactionAttribute txAttr, final String joinpointIdentification) {
        //如果事务名称不为空，则使用方法唯一标识。并使用 DelegatingTransactionAttribute 封装 txAttr 
        if (txAttr != null && ((TransactionAttributerollbackOn)txAttr).getName() == null) {
            txAttr = new DelegatingTransactionAttribute((TransactionAttribute)txAttr) {
                public String getName() {
                    return joinpointIdentification;
                }
            };
        }
        TransactionStatus status = null;
        if (txAttr != null) {
            if (tm != null) {
                // 获取事务状态。内部判断是否开启事务绑定线程与数据库连接。详情见下文
                status = tm.getTransaction((TransactionDefinition)txAttr);
            } else if (this.logger.isDebugEnabled()) {
                this.logger.debug("Skipping transactional joinpoint [" + joinpointIdentification + "] because no transaction manager has been configured");
            }
        }
        //构建事务消息，根据指定的属性与状态status 构建一个 TransactionInfo。将已经建立连接的事务所有信息，都记录在ThreadLocal下的TransactionInfo 实例中，包括目标方法的所有状态信息，如果事务执行失败，spring会根据TransactionInfo中的信息来进行回滚等后续操作
        return this.prepareTransactionInfo(tm, (TransactionAttribute)txAttr, joinpointIdentification, status);
    }
```



#### 获取当前事务对象

`AbstractPlatformTransactionManager.getTransaction()` 获取当前事务对象。通过这个方法，可以获取到关于事务的详细信息，如事务的状态、相关属性等。

```java
public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition)
   throws TransactionException {
            
  //definition 中存储的事务的注解信息，超时时间和隔离级别等
  TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());
  // 获取当前事务
  Object transaction = doGetTransaction();
  boolean debugEnabled = logger.isDebugEnabled();
  // 判断当前线程是否存在事务 
  if (isExistingTransaction(transaction)) {
   // 处理已经存在的事务
   return handleExistingTransaction(def, transaction, debugEnabled);
  }

  // 事务超时设置验证，超时时间小于-1 抛异常
  if (def.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
   throw new InvalidTimeoutException("Invalid transaction timeout", def.getTimeout());
  }

  // 如果当前线程不存在事务且 事务传播行为是 MANDATORY（用当前事务，如果当前没有事务，则抛出异常） 抛异常
  if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
   throw new IllegalTransactionStateException(
     "No existing transaction found for transaction marked with propagation 'mandatory'");
  }
  //以下三种事务传播行为 需要开启新事务
  else if (def.getPropagationBehavior() == TransactionDefinition.propagation_required ||
    def.getPropagationBehavior() == TransactionDefinition.propagation_requires_new ||
    def.getPropagationBehavior() == TransactionDefinition.propagation_nested) {
    //挂起原事务，因为这里不存在原事务 故设置为null。
    //当一个事务方法内部调用了另一个事务方法时，如果第二个事务方法需要独立于第一个事务方法，那么可以使用 suspend 方法来挂起当前事务，然后再开始一个新的事务
    AbstractPlatformTransactionManager.SuspendedResourcesHolder suspendedResources = this.suspend((Object)null);
            try {
                boolean newSynchronization = this.getTransactionSynchronization() != 2;
                DefaultTransactionStatus status = this.newTransactionStatus((TransactionDefinition)definition, transaction, true, newSynchronization, debugEnabled, suspendedResources);
                //开启事务
                this.doBegin(transaction, (TransactionDefinition)definition);
                //同步事务状态及书屋属性
                this.prepareSynchronization(status, (TransactionDefinition)definition);
                return status;
            } catch (RuntimeException var7) {
                this.resume((Object)null, suspendedResources);
                throw var7;
            }
  }
  else {
   boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);//0
    //创建默认状态 详情见 下文
   return prepareTransactionStatus(def, null, true, newSynchronization, debugEnabled, null);
  }
 }
```



#### 执行获取事务的具体操作

`AbstractPlatformTransactionManager.doGetTransaction()` 方法用于执行获取事务的具体操作。它可能会根据一些条件或规则，去查找和获取当前的事务对象，并进行相应的处理。

```java
 protected Object doGetTransaction() {
        DataSourceTransactionManager.DataSourceTransactionObject txObject = new DataSourceTransactionManager.DataSourceTransactionObject();
        //是否允许在一个事务内部开启另一个事务。
        txObject.setSavepointAllowed(this.isNestedTransactionAllowed());
        // this.dataSource数据源 配置
        //判断当前线程如果已经记录数据库连接则使用原连接
        ConnectionHolder conHolder = (ConnectionHolder)TransactionSynchronizationManager.getResource(this.dataSource);
        //false 表示不是新创建连接
        txObject.setConnectionHolder(conHolder, false);
        return txObject;
    }
```

- `this.dataSource()` 是我们配置DataSourceTransactionManager时传入的。

```java
 <bean id="valuationTransactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="valuationDataSource"/>
    </bean>
```

- `TransactionSynchronizationManager.getResource()` 方法的作用主要是获取与当前事务相关联的资源。TransactionSynchronizationManager 持有一个ThreadLocal的实例，存在一个key为dataSource ，value为ConnectionHolder 的Map信息。

```java
//ThreadLocal 存放 ConnectionHolder 信息，ConnectionHolder 可以理解为Connection（数据库连接）的包装类，其中最主要属性为  Connection
private static final ThreadLocal<Map<Object, Object>> resources = new NamedThreadLocal("Transactional resources");

 // 获取ConnectionHolder
 public static Object getResource(Object key) {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        //获取连接信息
        Object value = doGetResource(actualKey);
        return value;
    }
 //具体执行获取连接信息操作
 private static Object doGetResource(Object actualKey) {
        //从 ThreadLoacl中获取
        Map<Object, Object> map = (Map)resources.get();
        if (map == null) {
            return null;
        } else {
            Object value = map.get(actualKey);
            if (value instanceof ResourceHolder && ((ResourceHolder)value).isVoid()) {
                map.remove(actualKey);
                if (map.isEmpty()) {
                    resources.remove();
                }
                value = null;
            }
            return value;
        }
    }
```

#### 判断是否存在正在进行的事务

`AbstractPlatformTransactionManager.isExistingTransaction()` 方法用于判断是否存在正在进行的事务。它可以帮助我们确定当前的执行环境是否处于事务中，以便进行相应的处理。

```java
protected boolean isExistingTransaction(Object transaction) {
        DataSourceTransactionManager.DataSourceTransactionObject txObject = (DataSourceTransactionManager.DataSourceTransactionObject)transaction;
        return txObject.getConnectionHolder() != null && txObject.getConnectionHolder().isTransactionActive();
    }
```



#### 挂起事务

`AbstractPlatformTransactionManager.suspend()` 挂起事务,对有无同步的事务采取不同方案，`doSuspend()`执行挂起具体操作。

```java
protected final AbstractPlatformTransactionManager.SuspendedResourcesHolder suspend(Object transaction) throws TransactionException {
        //如果有同步的事务，则优先挂起同步事务
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            List suspendedSynchronizations = this.doSuspendSynchronization();
            try {
                Object suspendedResources = null;
                if (transaction != null) {
                    //执行挂起操作
                    suspendedResources = this.doSuspend(transaction);
                }
                //重置事务名称
                String name = TransactionSynchronizationManager.getCurrentTransactionName();
                TransactionSynchronizationManager.setCurrentTransactionName((String)null);
                //重置只读状态
                boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
                TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
                //重置隔离级别
                Integer isolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
                TransactionSynchronizationManager.setCurrentTransactionIsolationLevel((Integer)null);
                //重置事务激活状态
                boolean wasActive = TransactionSynchronizationManager.isActualTransactionActive();
                TransactionSynchronizationManager.setActualTransactionActive(false);
                //返回挂起的事务
                return new AbstractPlatformTransactionManager.SuspendedResourcesHolder(suspendedResources, suspendedSynchronizations, name, readOnly, isolationLevel, wasActive);
            } catch (RuntimeException var8) {
                this.doResumeSynchronization(suspendedSynchronizations);
                throw var8;
            } 
        } else if (transaction != null) {
            Object suspendedResources = this.doSuspend(transaction);
            return new AbstractPlatformTransactionManager.SuspendedResourcesHolder(suspendedResources);
        } else {
            return null;
        }
    }
```



- `AbstractPlatformTransactionManager.doSuspend()`执行挂起操作只是将当前ConnectionHolder设置为null，返回原有事务消息，方便后续恢复原有事务消息，并将当前正在进行的事务信息进行重置。

```java
protected Object doSuspend(Object transaction) {
        DataSourceTransactionManager.DataSourceTransactionObject txObject = (DataSourceTransactionManager.DataSourceTransactionObject)transaction;
        txObject.setConnectionHolder((ConnectionHolder)null);
        //接触绑定 
        return TransactionSynchronizationManager.unbindResource(this.dataSource);
    }

//解除绑定操作，将现有的事务消息remove并返回上一级
 public static Object unbindResource(Object key) throws IllegalStateException {
        Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
        //解绑操作，移除资源
        Object value = doUnbindResource(actualKey);
        if (value == null) {
            throw new IllegalStateException("No value for key [" + actualKey + "] bound to thread [" + Thread.currentThread().getName() + "]");
        } else {
            return value;
        }
    }
```



- `AbstractPlatformTransactionManager.doBegin()`数据库连接获取，当新事务时，则获取新的数据库连接，并为其设置隔离级别，是否只读等属性。

```java
protected void doBegin(Object transaction, TransactionDefinition definition) {
        DataSourceTransactionManager.DataSourceTransactionObject txObject = (DataSourceTransactionManager.DataSourceTransactionObject)transaction;
        Connection con = null;

        try {
            //新事务开启时将 ConnectionHolder 设置为null
            if (txObject.getConnectionHolder() == null || txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
                //获取新的数据库连接
                Connection newCon = this.dataSource.getConnection();
                txObject.setConnectionHolder(new ConnectionHolder(newCon), true);
            }
            txObject.getConnectionHolder().setSynchronizedWithTransaction(true);
            con = txObject.getConnectionHolder().getConnection();
            //设置事务隔离级别 和readOnly属性
            Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
            txObject.setPreviousIsolationLevel(previousIsolationLevel);
            if (con.getAutoCommit()) {
                txObject.setMustRestoreAutoCommit(true);
                // 交给Spring控制事务提交
                con.setAutoCommit(false);
            }
            this.prepareTransactionalConnection(con, definition);
            //设置当前线程的事务激活状态
            txObject.getConnectionHolder().setTransactionActive(true);
            int timeout = this.determineTimeout(definition);
            if (timeout != -1) {
                // 设置超时时间
                txObject.getConnectionHolder().setTimeoutInSeconds(timeout);
            }
            if (txObject.isNewConnectionHolder()) {
                TransactionSynchronizationManager.bindResource(this.getDataSource(), txObject.getConnectionHolder());
            }

        } catch (Throwable var7) {
            if (txObject.isNewConnectionHolder()) {
                DataSourceUtils.releaseConnection(con, this.dataSource);
                txObject.setConnectionHolder((ConnectionHolder)null, false);
            }

            throw new CannotCreateTransactionException("Could not open JDBC Connection for transaction", var7);
        }
    }
```



- `AbstractPlatformTransactionManager.prepareTransactionStatus()`创建默认Status,如果不需要开始事务 （比如SUPPORTS），则返回一个默认的状态。

```java
protected final DefaultTransactionStatus prepareTransactionStatus(TransactionDefinition definition, Object transaction, boolean newTransaction, boolean newSynchronization, boolean debug, Object suspendedResources) {
        DefaultTransactionStatus status = this.newTransactionStatus(definition, transaction, newTransaction, newSynchronization, debug, suspendedResources);
        this.prepareSynchronization(status, definition);
        return status;
    }
    
protected DefaultTransactionStatus newTransactionStatus(TransactionDefinition definition, Object transaction, boolean newTransaction, boolean newSynchronization, boolean debug, Object suspendedResources) {
        boolean actualNewSynchronization = newSynchronization && !TransactionSynchronizationManager.isSynchronizationActive();
        //创建 DefaultTransactionStatus 对象
        return new DefaultTransactionStatus(transaction, newTransaction, actualNewSynchronization, definition.isReadOnly(), debug, suspendedResources);
    }
```



- `AbstractPlatformTransactionManager.handleExistingTransaction()`针对不同的传播行为做不同的处理方法，比如挂起原事务开启新事务等等。

```java
 private TransactionStatus handleExistingTransaction(TransactionDefinition definition, Object transaction, boolean debugEnabled) throws TransactionException {
        //当传播行为是 NEVER 时抛出异常
        if (definition.getPropagationBehavior() == 5) {
            throw new IllegalTransactionStateException("Existing transaction found for transaction marked with propagation 'never'");
        } else {
            AbstractPlatformTransactionManager.SuspendedResourcesHolder suspendedResources;
            boolean newSynchronization;
            //当传播方式为NOT_SUPPORTED 时挂起当前事务，然后在无事务的状态下运行
            if (definition.getPropagationBehavior() == 4) {
                //挂起事务
                suspendedResources = this.suspend(transaction);
                newSynchronization = this.getTransactionSynchronization() == 0;
                //返回默认status
                return this.prepareTransactionStatus(definition, (Object)null, false, newSynchronization, debugEnabled, suspendedResources);
                //当传播方式为REQUIRES_NEW时，挂起当前事务，然后启动新事务
            } else if (definition.getPropagationBehavior() == 3) {
                //挂起原事务
                suspendedResources = this.suspend(transaction);
                try {
                    newSynchronization = this.getTransactionSynchronization() != 2;
                    DefaultTransactionStatus status = this.newTransactionStatus(definition, transaction, true, newSynchronization, debugEnabled, suspendedResources);
                    //启动新的事务
                    this.doBegin(transaction, definition);
                    this.prepareSynchronization(status, definition);
                    return status;
                } catch (Error|RuntimeException var7) {
                    this.resumeAfterBeginException(transaction, suspendedResources, var7);
                    throw var7;
                } 
            } else {
                boolean newSynchronization;
                //当传播方式为NESTED时，设置事务的保存点
                //存在事务，将该事务标注保存点，形成嵌套事务
                //嵌套事务中的子事务出现异常不会影响到父事务保存点之前的操作
                if (definition.getPropagationBehavior() == 6) {
                    if (!this.isNestedTransactionAllowed()) {
                        throw new NestedTransactionNotSupportedException("Transaction manager does not allow nested transactions by default - specify 'nestedTransactionAllowed' property with value 'true'");
                    } else {
                        if (this.useSavepointForNestedTransaction()) {
                            DefaultTransactionStatus status = this.prepareTransactionStatus(definition, transaction, false, false, debugEnabled, (Object)null);
                            //创建保存点，回滚时，只回滚到该保存点
                            status.createAndHoldSavepoint();
                            return status;
                        } else {
                            newSynchronization = this.getTransactionSynchronization() != 2;
                            DefaultTransactionStatus status = this.newTransactionStatus(definition, transaction, true, newSynchronization, debugEnabled, (Object)null);
                            //如果不支持保存点，就启动新的事务
                            this.doBegin(transaction, definition);
                            this.prepareSynchronization(status, definition);
                            return status;
                        }
                    }
                } else {
                    newSynchronization = this.getTransactionSynchronization() != 2;
                    return this.prepareTransactionStatus(definition, transaction, false, newSynchronization, debugEnabled, (Object)null);
                }
            }
        }
    }
```



### 回滚事务

`TransactionAspectSupport.completeTransactionAfterThrowing()` 判断事务是否存在，如不存在就不需要回滚，如果存在则在判断是否满足回滚条件。

```java
protected void completeTransactionAfterThrowing(TransactionAspectSupport.TransactionInfo txInfo, Throwable ex) {
        //判断是否存在事务
        if (txInfo != null && txInfo.hasTransaction()) {
            // 判断是否满足回滚条件。抛出的异常类型，和定义的回滚规则进行匹配
            if (txInfo.transactionAttribute.rollbackOn(ex)) {
                try {
                    // 回滚处理
                    txInfo.getTransactionManager().rollback(txInfo.getTransactionStatus());
                } 
            //省略代码
            } else {
                try {
                    //不满足回滚条件 出现异常 
                    txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
                } 
            //省略代码
            }
        }

    }
```



`AbstractPlatformTransactionManager.rollback()`当在事务执行过程中出现异常或其他需要回滚的情况时，就会调用这个方法，将事务进行回滚操作，撤销之前所做的数据库操作，以保证数据的一致性。

```java
 public final void rollback(TransactionStatus status) throws TransactionException {
        //判断事务是否已经完成，回滚时抛出异常
        if (status.isCompleted()) {
            throw new IllegalTransactionStateException("Transaction is already completed - do not call commit or rollback more than once per transaction");
        } else {
            DefaultTransactionStatus defStatus = (DefaultTransactionStatus)status;
            // 执行回滚操作。
            this.processRollback(defStatus);
        }
    }
```



`AbstractPlatformTransactionManager.processRollback()`方法主要用于处理事务的回滚操作。通过这个方法，可以确保事务在需要回滚时能够正确地执行回滚操作，保持数据的完整性。

```java
private void processRollback(DefaultTransactionStatus status) {
        try {
            try {
                //解绑线程和会话绑定关系
                this.triggerBeforeCompletion(status);
                if (status.hasSavepoint()) {
                    //如果有保存点（当前事务为单独的线程则会退到保存点）
                    status.rollbackToHeldSavepoint();
                } else if (status.isNewTransaction()) {
                    //如果是新事务直接回滚。调用数据库连接并调用rollback方法进行回滚。使用底层数据库连接提供的API
                    this.doRollback(status);
                } else if (status.hasTransaction()) {
                    if (status.isLocalRollbackOnly() || !this.isGlobalRollbackOnParticipationFailure()) {
                        //如果当前事务不是独立的事务，则只能等待事务链执行完成后再做回滚操作
                        this.doSetRollbackOnly(status);
                    } 
                } 
            } 
            //catch 等代码
            // 关闭会话，重置属性
            this.triggerAfterCompletion(status, 1);
        } finally {
            //清理并恢复挂起的事务
            this.cleanupAfterCompletion(status);
        }

    }
```



### 提交事务

`TransactionAspectSupport.commitTransactionAfterReturning()` 基本上和回滚一样，都是先判断是否有事务，在操作提交。

```java
  protected void commitTransactionAfterReturning(TransactionAspectSupport.TransactionInfo txInfo) {
        if (txInfo != null && txInfo.hasTransaction()) {
            //提交事务
            txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
        }
    }
```



`AbstractPlatformTransactionManager.commit()` 创建默认Status prepareTransactionStatu,发现是否有回滚标记，然后进行回滚。如果判断无需回滚就可以直接提交。

```java
public final void commit(TransactionStatus status) throws TransactionException {
        // 事务状态已完成则抛异常
        if (status.isCompleted()) {
            throw new IllegalTransactionStateException("Transaction is already completed - do not call commit or rollback more than once per transaction");
        } else {
            DefaultTransactionStatus defStatus = (DefaultTransactionStatus)status;
            //发现回滚标记
            if (defStatus.isLocalRollbackOnly()) {
                //回滚
                this.processRollback(defStatus);
            } else if (!this.shouldCommitOnGlobalRollbackOnly() && defStatus.isGlobalRollbackOnly()) {
                //回滚
                this.processRollback(defStatus);
                if (status.isNewTransaction() || this.isFailEarlyOnGlobalRollbackOnly()) {
                    throw new UnexpectedRollbackException("Transaction rolled back because it has been marked as rollback-only");
                }
            } else {
                // 提交操作
                this.processCommit(defStatus);
            }
        }
    }
```

`AbstractPlatformTransactionManager.processCommit()`处理事务的提交操作

```java
private void processCommit(DefaultTransactionStatus status) throws TransactionException {
        try {
            boolean beforeCompletionInvoked = false;

            try {
                this.prepareForCommit(status);
                this.triggerBeforeCommit(status);
                this.triggerBeforeCompletion(status);
                beforeCompletionInvoked = true;
                boolean globalRollbackOnly = false;
                if (status.isNewTransaction() || this.isFailEarlyOnGlobalRollbackOnly()) {
                    globalRollbackOnly = status.isGlobalRollbackOnly();
                }
                if (status.hasSavepoint()) {
                    //释放保存点信息
                    status.releaseHeldSavepoint();
                } else if (status.isNewTransaction()) {
                    // 是一个新的事务 则提交。 获取数据库连接后使用数据库API进行提交事务
                    this.doCommit(status);
                }

                if (globalRollbackOnly) {
                    throw new UnexpectedRollbackException("Transaction silently rolled back because it has been marked as rollback-only");
                }
            } catch (TransactionException var20) {
                if (this.isRollbackOnCommitFailure()) {
                    //提交异常回滚
                    this.doRollbackOnCommitException(status, var20);
                } else {
                    this.triggerAfterCompletion(status, 2);
                }

                throw var20;
            } 
            //省略其它异常拦截
            try {
                this.triggerAfterCommit(status);
            } finally {
                this.triggerAfterCompletion(status, 0);
            }
        } finally {
            // 清理事务消息
            this.cleanupAfterCompletion(status);
        }

    }
```



### 清除事务信息

`AbstractPlatformTransactionManager.cleanupAfterCompletion()` 这个方法主要用于在事务完成后进行清理工作。它会负责释放资源、清理临时数据等，以确保系统处于良好的状态。

```java
 private void cleanupAfterCompletion(DefaultTransactionStatus status) {
        //将当前事务设置为完成状态
        status.setCompleted();
        if (status.isNewSynchronization()) {
            // 清空当前事务消息
            TransactionSynchronizationManager.clear();
        }

        if (status.isNewTransaction()) {
            //如果是新事务 则在事务完成之后做清理操作
            this.doCleanupAfterCompletion(status.getTransaction());
        }
        if (status.getSuspendedResources() != null) {
            // 将原事务从挂起状态恢复
            this.resume(status.getTransaction(), (AbstractPlatformTransactionManager.SuspendedResourcesHolder)status.getSuspendedResources());
        }
    }
```

`AbstractPlatformTransactionManager.doCleanupAfterCompletion()`在新事务完成后会调用resetConnectionAfterTransaction方法重置数据库连接信息，并判断如果是新的数据库连接则将其放回连接池。

```java
protected void doCleanupAfterCompletion(Object transaction) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
    if (txObject.isNewConnectionHolder()) {
        // 将数据库连接从当前线程中解除绑定
        TransactionSynchronizationManager.unbindResource(this.dataSource);
    }
    Connection con = txObject.getConnectionHolder().getConnection();
    try {
        // 恢复数据库连接的autoCommit状态
        if (txObject.isMustRestoreAutoCommit()) {
            con.setAutoCommit(true); 
        }
        // 负责重置数据库连接信息，包括隔离级别、readOnly属性等
        DataSourceUtils.resetConnectionAfterTransaction(con, txObject.getPreviousIsolationLevel());
    }
    catch (Throwable ex) {
        logger.debug("Could not reset JDBC Connection after transaction", ex);
    }
    if (txObject.isNewConnectionHolder()) {
        // 如果是新的数据库连接则将数据库连接放回连接池
        DataSourceUtils.releaseConnection(con, this.dataSource);
    }
    txObject.getConnectionHolder().clear();
}
```



 `AbstractPlatformTransactionManager.resume()` 如果事务执行前有事务挂起，那么当前事务执行结束后需要将挂起的事务恢复，挂起事务时保存了原事务信息，重置了当前事务信息，所以恢复操作就是将当前的事务信息设置为之前保存的原事务信息。

```java
 protected final void resume(Object transaction, AbstractPlatformTransactionManager.SuspendedResourcesHolder resourcesHolder) throws TransactionException {
        if (resourcesHolder != null) {
            Object suspendedResources = resourcesHolder.suspendedResources;
            if (suspendedResources != null) {
                // 执行 恢复挂起事务 ，绑定资源bindResource
                this.doResume(transaction, suspendedResources);
            }
            List<TransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
            if (suspendedSynchronizations != null) {
                TransactionSynchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);
                TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(resourcesHolder.isolationLevel);
                TransactionSynchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);
                TransactionSynchronizationManager.setCurrentTransactionName(resourcesHolder.name);
                this.doResumeSynchronization(suspendedSynchronizations);
            }
        }
    }
```

`TransactionAspectSupport.cleanupTransactionInfo()`清除当前节点的事务消息，将旧事务节点信息通过thradLoacl更新到当前线程。

```java
protected void cleanupTransactionInfo(TransactionAspectSupport.TransactionInfo txInfo) {
        if (txInfo != null) {
            //从当前线程的 ThreadLocal 获取上层的事务信息，将当前事务出栈，继续执行上层事务
            txInfo.restoreThreadLocalStatus();
        }
    }

 private void restoreThreadLocalStatus() {
            //当前事务处理完之后，恢复上层事务上下文 
            TransactionAspectSupport.transactionInfoHolder.set(this.oldTransactionInfo);
        }
```



### 小结

如果方法正常执行完成且没有异常，调用`commitTransactionAfterReturning()`方法。如果执行中出现异常，调用`completeTransactionAfterThrowing()`方法。

两个方法内部都会判断是否存在事务以及是否满足回滚条件来决定最终执行提交操作还是回滚操作。













 

 <!-- @include: @article-footer.snippet.md -->     











