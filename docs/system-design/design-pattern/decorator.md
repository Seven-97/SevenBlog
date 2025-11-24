---
title: 装饰者模式
category: 系统设计
tags:
  - 设计模式
---





## 概述

我们先来看一个快餐店的例子。

快餐店有炒面、炒饭这些快餐，可以额外附加鸡蛋、火腿、培根这些配菜，当然加配菜需要额外加钱，每个配菜的价钱通常不太一样，那么计算总价就会显得比较麻烦。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271757151.png)

使用继承的方式存在的问题：

- 扩展性不好：如果要再加一种配料（火腿肠），我们就会发现需要给FriedRice和FriedNoodles分别定义一个子类。如果要新增一个快餐品类（炒河粉）的话，就需要定义更多的子类。

- 产生过多的子类

**定义：**指在不改变现有对象结构的情况下，动态地给该对象增加一些职责（即增加其额外功能）的模式。

## 结构

装饰（Decorator）模式中的角色：

- 抽象构件（Component）角色 ：定义一个抽象接口以规范准备接收附加责任的对象。

- 具体构件（Concrete Component）角色 ：实现抽象构件，通过装饰角色为其添加一些职责。

- 抽象装饰（Decorator）角色 ： 继承或实现抽象构件，并包含具体构件的实例，可以通过其子类扩展具体构件的功能。

- 具体装饰（ConcreteDecorator）角色 ：实现抽象装饰的相关方法，并给具体构件对象添加附加的责任。

## 案例

我们使用装饰者模式对快餐店案例进行改进，体会装饰者模式的精髓。

类图如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271757505.png)

代码如下：

```java
//快餐接口
public abstract class FastFood {
    private float price;
    private String desc;

    public FastFood() {
    }

    public FastFood(float price, String desc) {
        this.price = price;
        this.desc = desc;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public float getPrice() {
        return price;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public abstract float cost();  //获取价格
}

//炒饭
public class FriedRice extends FastFood {

    public FriedRice() {
        super(10, "炒饭");
    }

    public float cost() {
        return getPrice();
    }
}

//炒面
public class FriedNoodles extends FastFood {

    public FriedNoodles() {
        super(12, "炒面");
    }

    public float cost() {
        return getPrice();
    }
}

//配料类
public abstract class Garnish extends FastFood {

    private FastFood fastFood;

    public FastFood getFastFood() {
        return fastFood;
    }

    public void setFastFood(FastFood fastFood) {
        this.fastFood = fastFood;
    }

    public Garnish(FastFood fastFood, float price, String desc) {
        super(price,desc);
        this.fastFood = fastFood;
    }
}

//鸡蛋配料
public class Egg extends Garnish {

    public Egg(FastFood fastFood) {
        super(fastFood,1,"鸡蛋");
    }

    public float cost() {
        return getPrice() + getFastFood().getPrice();
    }

    @Override
    public String getDesc() {
        return super.getDesc() + getFastFood().getDesc();
    }
}

//培根配料
public class Bacon extends Garnish {

    public Bacon(FastFood fastFood) {

        super(fastFood,2,"培根");
    }

    @Override
    public float cost() {
        return getPrice() + getFastFood().getPrice();
    }

    @Override
    public String getDesc() {
        return super.getDesc() + getFastFood().getDesc();
    }
}

//测试类
public class Client {
    public static void main(String[] args) {
        //点一份炒饭
        FastFood food = new FriedRice();
        //花费的价格
        System.out.println(food.getDesc() + " " + food.cost() + "元");

        System.out.println("========");
        //点一份加鸡蛋的炒饭
        FastFood food1 = new FriedRice();

        food1 = new Egg(food1);
        //花费的价格
        System.out.println(food1.getDesc() + " " + food1.cost() + "元");

        System.out.println("========");
        //点一份加培根的炒面
        FastFood food2 = new FriedNoodles();
        food2 = new Bacon(food2);
        //花费的价格
        System.out.println(food2.getDesc() + " " + food2.cost() + "元");
    }
}
```



**好处：**

- 装饰者模式可以带来比继承更加灵活性的扩展功能，使用更加方便，可以通过组合不同的装饰者对象来获取具有不同行为状态的多样化的结果。装饰者模式比继承更具良好的扩展性，完美的遵循开闭原则，继承是静态的附加责任，装饰者则是动态的附加责任。

- 装饰类和被装饰类可以独立发展，不会相互耦合，装饰模式是继承的一个替代模式，装饰模式可以动态扩展一个实现类的功能。

## 使用场景

- 当不能采用继承的方式对系统进行扩充或者采用继承不利于系统扩展和维护时。不能采用继承的情况主要有两类：

  - 第一类是系统中存在大量独立的扩展，为支持每一种组合将产生大量的子类，使得子类数目呈爆炸性增长；

  - 第二类是因为类定义不能继承（如final类）

- 在不影响其他对象的情况下，以动态、透明的方式给单个对象添加职责。

- 当对象的功能要求可以动态地添加，也可以再动态地撤销时。

##  源码解析

### IO流包装类

IO流中的包装类使用到了装饰者模式。BufferedInputStream，BufferedOutputStream，BufferedReader，BufferedWriter。

我们以BufferedWriter举例来说明，先看看如何使用BufferedWriter

```java
public class Demo {
    public static void main(String[] args) throws Exception{
        //创建BufferedWriter对象
        //创建FileWriter对象
        FileWriter fw = new FileWriter("C:\\Users\\Think\\Desktop\\a.txt");
        BufferedWriter bw = new BufferedWriter(fw);

        //写数据
        bw.write("hello Buffered");

        bw.close();
    }
}
```

使用起来感觉确实像是装饰者模式，接下来看它们的结构：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271757826.png)

小结：BufferedWriter使用装饰者模式对Writer子实现类进行了增强，添加了缓冲区，提高了写数据的效率。


### Spring TransactionAwareCacheDecorator

装饰任何实现了 `Cache`接口的缓存对象（如RedisCache、EhCache等）。

`TransactionAwareCacheDecorator`的核心目标是**将缓存操作与Spring管理的事务进行同步**，确保只有在事务成功提交后，对缓存的修改（如`put`、`evict`）才会真正生效。这有效防止了事务回滚后，脏数据污染缓存的问题。

核心实现机制是：

1. **检查事务状态**：当调用`put`或`evict`方法时，它首先通过 `TransactionSynchronizationManager.isSynchronizationActive()`判断当前是否有活跃的事务。
2. **延迟执行**：如果存在活跃事务，它会将一个回调（`TransactionSynchronization`）注册到当前事务中。这个回调的 `afterCommit()`方法会在事务成功提交后才被执行，此时才会去调用被装饰的底层缓存对象的对应方法（如 `targetCache.put(key, value)`）。
3. **立即执行**：如果当前没有活跃事务，则直接执行底层缓存的操作。

```java
public class TransactionAwareCacheDecorator implements Cache {
    private final Cache targetCache; // 被装饰的缓存对象

    public void put(final Object key, final Object value) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 有事务：延迟到事务提交后执行
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        targetCache.put(key, value); // 事务提交后才真正写入缓存
                    }
                });
        } else {
            // 无事务：立即执行
            this.targetCache.put(key, value);
        }
    }
}
```

### MyBatis Cache 装饰器

MyBatis的缓存设计是装饰器模式的一个经典案例。它有一个最基础的实现 `PerpetualCache`，其内部仅仅使用一个 `HashMap`来存储数据。所有其他缓存功能，都是通过不同的装饰器层层包装上去的。

**核心结构**:

- **组件（Component）**: `Cache`接口，定义了缓存的基本操作。
- **具体组件（ConcreteComponent）**: `PerpetualCache`，最基础的缓存实现。
- **装饰器（Decorator）**: 所有实现了 `Cache`接口的装饰器类，它们都持有一个 `Cache`类型的引用（即被装饰对象）。


**部分常用的MyBatis缓存装饰器**：

| 装饰器                     | 功能描述                   | 应用场景                      |
| ----------------------- | ---------------------- | ------------------------- |
| **LruCache**​           | 基于最近最少使用算法进行缓存淘汰       | 限制缓存大小，防止内存溢出。            |
| **FifoCache**​          | 基于先进先出算法进行缓存淘汰<br>     | 按进入顺序清理缓存。                |
| **SynchronizedCache**​  | 为缓存方法提供同步控制，保证线程安全     | 多线程环境下使用。                 |
| **BlockingCache**​      | 对同一个Key的访问进行阻塞，防止缓存击穿  | 高并发下，避免一个缓存失效导致大量请求涌向数据库。 |
| **LoggingCache**​       | 记录缓存命中次数等日志信息          | 用于监控和性能调优。                |
| **SerializedCache**​    | 在存取值时执行序列化和反序列化        | 确保返回的是对象的深拷贝，避免篡改。        |
| **TransactionalCache**​ | 管理事务中的缓存，在事务提交时才批量更新缓存 | 用于MyBatis二级缓存，保证事务一致性。    |

**装饰过程示例**：

可以像这样组合使用装饰器，创建一个功能强大的缓存对象：

```java
Cache cache = new PerpetualCache("DefaultCache"); // 1. 基础缓存
cache = new LruCache(cache);     // 2. 添加LRU淘汰策略
cache = new SynchronizedCache(cache); // 3. 添加线程安全
cache = new LoggingCache(cache);  // 4. 添加日志功能
cache = new SerializedCache(cache); // 5. 添加序列化功能
```

最终，这个 `cache`对象就同时具备了基础缓存、LRU淘汰、线程安全、日志记录和序列化等多种能力。这种设计极具弹性，可以根据需要任意组合和排序这些装饰器。



## 静态代理和装饰者的区别

装饰器模式是一种特殊的静态代理，装饰器模式强调自身的功能扩展，代理模式强调代理过程的控制。

相同点：
  - 都要实现与目标类相同的业务接口
  - 在两个类中都要声明目标对象
  - 都可以在不修改目标类的前提下增强目标方法

不同点：
  - 目的不同：装饰者是为了增强目标对象静态代理是为了保护和隐藏目标对象
  - 获取目标对象构建的地方不同：装饰者是由外界传递进来，可以通过构造方法传递静态代理是在代理类内部创建，以此来隐藏目标对象


<!-- @include: @article-footer.snippet.md -->