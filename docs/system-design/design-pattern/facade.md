---
title: 外观(门面)模式
category: 系统设计
tags:
  - 设计模式
---





## 概述

有些人可能炒过股票，但其实大部分人都不太懂，这种没有足够了解证券知识的情况下做股票是很容易亏钱的，刚开始炒股肯定都会想，如果有个懂行的帮手就好，其实基金就是个好帮手，支付宝里就有许多的基金，它将投资者分散的资金集中起来，交由专业的经理人进行管理，投资于股票、债券、外汇等领域，而基金投资的收益归持有者所有，管理机构收取一定比例的托管管理费用。

**定义**：又名门面模式，是一种通过为多个复杂的子系统提供一个一致的接口，而使这些子系统更加容易被访问的模式。该模式对外有一个统一接口，外部应用程序不用关心内部子系统的具体的细节，这样会大大降低应用程序的复杂度，提高了程序的可维护性。

外观（Facade）模式是“[迪米特法则](https://www.seven97.top/system-design/design-pattern/overviewofdesignpatterns.html#迪米特法则)”的典型应用

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271757261.png)

 

## 结构

外观（Facade）模式包含以下主要角色：

- 外观（Facade）角色：为多个子系统对外提供一个共同的接口。

- 子系统（Sub System）角色：实现系统的部分功能，客户可以通过外观角色访问它。

## 案例

【例】智能家电控制

小明的爷爷已经60岁了，一个人在家生活：每次都需要打开灯、打开电视、打开空调；睡觉时关闭灯、关闭电视、关闭空调；操作起来都比较麻烦。所以小明给爷爷买了智能音箱，可以通过语音直接控制这些智能家电的开启和关闭。类图如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271758149.png)

代码如下：

```java
//灯类
public class Light {
    public void on() {
        System.out.println("打开了灯....");
    }

    public void off() {
        System.out.println("关闭了灯....");
    }
}

//电视类
public class TV {
    public void on() {
        System.out.println("打开了电视....");
    }

    public void off() {
        System.out.println("关闭了电视....");
    }
}

//空调类
public class AirCondition {
    public void on() {
        System.out.println("打开了空调....");
    }

    public void off() {
        System.out.println("关闭了空调....");
    }
}

//智能音箱
public class SmartAppliancesFacade {

    private Light light;
    private TV tv;
    private AirCondition airCondition;

    public SmartAppliancesFacade() {
        light = new Light();
        tv = new TV();
        airCondition = new AirCondition();
    }

    public void say(String message) {
        if(message.contains("打开")) {
            on();
        } else if(message.contains("关闭")) {
            off();
        } else {
            System.out.println("我还听不懂你说的！！！");
        }
    }

    //起床后一键开电器
    private void on() {
        System.out.println("起床了");
        light.on();
        tv.on();
        airCondition.on();
    }

    //睡觉一键关电器
    private void off() {
        System.out.println("睡觉了");
        light.off();
        tv.off();
        airCondition.off();
    }
}

//测试类
public class Client {
    public static void main(String[] args) {
        //创建外观对象
        SmartAppliancesFacade facade = new SmartAppliancesFacade();
        //客户端直接与外观对象进行交互
        facade.say("打开家电");
        facade.say("关闭家电");
    }
}
```



**好处：**

- 降低了子系统与客户端之间的耦合度，使得子系统的变化不会影响调用它的客户类。
- 对客户屏蔽了子系统组件，减少了客户处理的对象数目，并使得子系统使用起来更加容易。
- 符合迪米特原则

**缺点：**

- 不符合开闭原则，修改很麻烦
- 某些情况下可能违背单一职责原则

## 使用场景

- 对分层结构系统构建时，使用外观模式定义子系统中每层的入口点可以简化子系统之间的依赖关系。

- 当一个复杂系统的子系统很多时，外观模式可以为系统设计一个简单的接口供外界访问。例如 spring的controller就是门面模式

- 当客户端与多个子系统之间存在很大的联系时，引入外观模式可将它们分离，从而提高子系统的独立性和可移植性。

## 和代理模式的区别

门面模式其实是一种特殊的静态代理，但也有区别：
- 门面模式的重点在于封装
- 代理模式的重点在于增强

所以不做增强的静态代理，也可以认为是门面模式


## 源码解析 

### ServletRequest

使用tomcat作为web容器时，接收浏览器发送过来的请求，tomcat会将请求信息封装成ServletRequest对象，如下图①处对象。但是大家想想ServletRequest是一个接口，它还有一个子接口HttpServletRequest，而我们知道该request对象肯定是一个HttpServletRequest对象的子实现类对象，到底是哪个类的对象呢？可以通过输出request对象，我们就会发现是一个名为RequestFacade的类的对象。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271758811.png)

RequestFacade类就使用了外观模式。先看结构图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271758644.png)

**为什么在此处使用外观模式呢？**

定义 RequestFacade 类，分别实现 ServletRequest ，同时定义私有成员变量 Request ，并且方法的实现调用 Request 的实现。然后，将 RequestFacade上转为 ServletRequest 传给 servlet 的 service 方法，这样即使在 servlet 中被下转为 RequestFacade ，也不能访问私有成员变量对象中的方法。既用了 Request ，又能防止其中方法被不合理的访问。

### Spring JdbcUtils

Spring框架中的 `JdbcUtils`是一个抽象工具类，用门面模式的核心在于**简化JDBC编程中那些重复、繁琐且容易出错的样板式代码**，最典型的就是资源释放。

原生JDBC中，每次使用完 `Connection`、`Statement`、`ResultSet`后，都必须记得在 `finally`块中关闭它们，并且关闭时还需要处理 `SQLException`。这个过程非常模板化，且容易因遗漏而导致资源泄漏。
    
`JdbcUtils`提供了如 `closeConnection(Connection con)`、`closeStatement(Statement stmt)`、`closeResultSet(ResultSet rs)`等一系列静态方法。这些方法内部已经妥善处理了异常：即使关闭时发生异常，也会记录日志而不会抛出，确保资源能被尝试关闭，同时不会影响主业务逻辑。这使得客户端代码从繁琐的`try-catch`块中解放出来，变得更简洁

```java
// 未使用JdbcUtils (繁琐且易错)
Connection conn = null;
Statement stmt = null;
try {
    conn = dataSource.getConnection();
    stmt = conn.createStatement();
    // ... 执行SQL
} finally {
    if (stmt != null) {
        try {
            stmt.close();
        } catch (SQLException e) {
            // 记录日志
        }
    }
    if (conn != null) {
        try {
            conn.close();
        } catch (SQLException e) {
            // 记录日志
        }
    }
}

// 使用JdbcUtils (简洁清晰)
Connection conn = null;
Statement stmt = null;
try {
    conn = dataSource.getConnection();
    stmt = conn.createStatement();
    // ... 执行SQL
} finally {
    JdbcUtils.closeStatement(stmt); // 内部已处理异常
    JdbcUtils.closeConnection(conn); // 内部已处理异常
}
```


###  MyBatis Configuration

MyBatis 的 `Configuration`类是一个功能强大的**信息枢纽和对象工厂**，`Configuration`类的门面模式的应用层次更深，用于**统一管理和创建MyBatis执行SQL语句所需的各种核心组件**。

MyBatis 内部有 `Executor`（执行器）、`StatementHandler`（语句处理器）、`ParameterHandler`（参数处理器）、`ResultSetHandler`（结果集处理器）等多个重要组件。这些组件的实例化过程复杂，需要根据配置信息（如缓存设置、Executor类型、插件等）来决定具体创建哪个实现类并进行组装。如果让客户端直接处理这些细节，将无比复杂。
    
`Configuration`类提供了一系列以 `new`开头的方法，如 `newExecutor(Transaction transaction, ExecutorType executorType)`、`newStatementHandler(Executor executor, ...)`等。这些方法就是门面接口。它们封装了复杂的创建逻辑，例如根据配置选择是创建简单的`SimpleExecutor`还是支持批量的`BatchExecutor`，是否用`CachingExecutor`进行包装以支持二级缓存，以及如何应用拦截器（插件）链等。客户端（如`DefaultSqlSession`）只需调用 `configuration.newExecutor(...)`即可获得一个完全初始化好的、功能完整的执行器，无需关心其内部复杂的组装过程

`Configuration`的 `newExecutor`方法简化后逻辑如下，完美体现了门面模式：
```java
public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
    // 1. 根据配置决定创建哪种基础的Executor（如SIMPLE, BATCH, REUSE）
    Executor executor;
    if (ExecutorType.BATCH == executorType) {
        executor = new BatchExecutor(this, transaction);
    } else {
        executor = new SimpleExecutor(this, transaction);
    }

    // 2. 如果全局开启了缓存，用CachingExecutor装饰基础Executor（装饰器模式）
    if (cacheEnabled) {
        executor = new CachingExecutor(executor);
    }

    // 3. 应用所有已配置的插件（责任链模式），返回最终的Executor
    executor = (Executor) interceptorChain.pluginAll(executor);
    return executor;
}
```

通过这个门面方法，调用者无需知晓 `SimpleExecutor`、`CachingExecutor`或插件的存在与创建顺序，`Configuration`统一处理了这些复杂性


<!-- @include: @article-footer.snippet.md -->