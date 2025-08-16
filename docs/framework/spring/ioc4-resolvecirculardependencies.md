---
title: Spring中的循环依赖是怎么个事？
category: 常用框架
tags:
  - Spring
head:
  - - meta
    - name: keywords
      content: spring,spring ioc,IOC,依赖注入,循环依赖
  - - meta
    - name: description
      content: 全网最全的Spring知识点总结，让天下没有难学的八股文！
---





首先，有两种Bean注入的方式：构造器注入和属性注入。

1. 对于构造器注入的循环依赖，Spring处理不了，会直接抛出BeanCurrentlylnCreationException异常。
2. 对于属性注入的循环依赖	
   1. 单例模式下，是通过三级缓存处理来循环依赖的。
   2. 非单例对象的循环依赖，则无法处理。



## 单例模式下的属性依赖

先来看下这三级缓存

```java
/** Cache of singleton objects: bean name --> bean instance */
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(256);
 
/** Cache of early singleton objects: bean name --> bean instance */
private final Map<String, Object> earlySingletonObjects = new HashMap<String, Object>(16);

/** Cache of singleton factories: bean name --> ObjectFactory */
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<String, ObjectFactory<?>>(16);
```

- **第一层缓存（singletonObjects）**：单例对象缓存池，已经实例化并且属性赋值，这里的对象是**成熟对象**；
- **第二层缓存（earlySingletonObjects）**：单例对象缓存池，已经实例化但尚未属性赋值，这里的对象是**半成品对象**；
- **第三层缓存（singletonFactories）**: 单例工厂的缓存

这三个 map 是如何配合的呢?
1. 首先，获取单例 Bean 的时候会通过 BeanName 先去 singletonObjects(一级缓存)查找完整的 Bean，如果找到则直接返回，否则进行步骤 2
2. 看对应的 Bean 是否在创建中，如果不在直接返回找不到(返回null)，如果是，则会去 earlySingletonObjects(二级缓存) 查找 Bean，如果找到则返回，否则进行步骤 3
3. 去 singletonfactores(三级缓存)通过 BeanName查找到对应的工厂，如果存着工厂则通过工厂创建 Bean，并目放置到earlySingletonObjects 中
4. 如果三个缓存都没找到，则返回 null

从上面的步骤我们可以得知，如果查询发现 Bean 还未创建，到第二步就直接返回 null，不会继续查二级和三级缓存。返回 null 之后，说明这个Bean 还未创建，这个时候会标记这个 Bean 正在创建中，然后再调用 createBean 来创建 Bean，而实际创建是调用方法 doCreateBean。

如下是获取单例中：getSingleton 在 **doGetBean方法中调用**

```java
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
  // Spring首先从singletonObjects（一级缓存）中尝试获取
  Object singletonObject = this.singletonObjects.get(beanName);
  // 若是获取不到而且对象在建立中，则尝试从earlySingletonObjects(二级缓存)中获取
  if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
    synchronized (this.singletonObjects) {
        singletonObject = this.earlySingletonObjects.get(beanName);
        if (singletonObject == null && allowEarlyReference) {
          ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
          if (singletonFactory != null) {
            //若是仍是获取不到而且容许从singletonFactories经过getObject获取，则经过singletonFactory.getObject()(三级缓存)获取
              singletonObject = singletonFactory.getObject();
              //若是获取到了则将singletonObject放入到earlySingletonObjects,也就是将三级缓存提高到二级缓存中
              this.earlySingletonObjects.put(beanName, singletonObject);
              this.singletonFactories.remove(beanName);
          }
        }
    }
  }
  return (singletonObject != NULL_OBJECT ? singletonObject : null);
}
```

补充一些方法和参数

- `isSingletonCurrentlyInCreation()`：判断当前单例bean是否正在建立中，也就是没有初始化完成(好比A的构造器依赖了B对象因此得先去建立B对象， 或则在A的populateBean过程当中依赖了B对象，得先去建立B对象，这时的A就是处于建立中的状态。)
- `allowEarlyReference` ：是否容许从singletonFactories中经过getObject拿到对象

分析getSingleton()的整个过程，Spring首先从一级缓存singletonObjects中获取。若是获取不到，而且对象正在建立中，就再从二级缓存earlySingletonObjects中获取。若是仍是获取不到且容许singletonFactories经过getObject()获取，就从三级缓存singletonFactory.getObject()(三级缓存)获取，若是获取到了则从三级缓存移动到了二级缓存。

从上面三级缓存的分析，咱们能够知道，Spring解决循环依赖的诀窍就在于singletonFactories这个三级cache。这个cache的类型是ObjectFactory，定义以下：

```java
public interface ObjectFactory<T> {
    T getObject() throws BeansException;
}
```

在bean建立过程当中，有两处比较重要的匿名内部类实现了该接口。一处是Spring利用其建立bean的时候，另外一处就是:

```java
addSingletonFactory(beanName, new ObjectFactory<Object>() {
   @Override   public Object getObject() throws BeansException {
      return getEarlyBeanReference(beanName, mbd, bean);
   }});
```

此处就是解决循环依赖的关键，这段代码发生在createBeanInstance以后，也就是说单例对象此时已经被建立出来的。这个对象已经被生产出来了，虽然还不完美（尚未进行初始化的第二步和第三步），可是已经能被人认出来了（根据对象引用能定位到堆中的对象），因此Spring此时将这个对象提早曝光出来让你们认识，让你们使用。

好比“A对象setter依赖B对象，B对象setter依赖A对象”，A首先完成了初始化的第一步，而且将本身提早曝光到singletonFactories中，此时进行初始化的第二步，发现本身依赖对象B，此时就尝试去get(B)，发现B尚未被create，因此走create流程，B在初始化第一步的时候发现本身依赖了对象A，因而尝试get(A)，尝试一级缓存singletonObjects(确定没有，由于A还没初始化彻底)，尝试二级缓存earlySingletonObjects（也没有），尝试三级缓存singletonFactories，因为A经过ObjectFactory将本身提早曝光了，因此B可以经过ObjectFactory.getObject拿到A对象(半成品)，B拿到A对象后顺利完成了初始化阶段一、二、三，彻底初始化以后将本身放入到一级缓存singletonObjects中。此时返回A中，A此时能拿到B的对象顺利完成本身的初始化阶段二、三，最终A也完成了初始化，进去了一级缓存singletonObjects中，并且更加幸运的是，因为B拿到了A的对象引用，因此B如今hold住的A对象完成了初始化。




## 为什么不能解决非单例属性之外的循环依赖？

1. 为何不能解决构造器的循环依赖？
   1. 构造器注入形成的循环依赖： 也就是beanB需要在beanA的构造函数中完成初始化，beanA也需要在beanB的构造函数中完成初始化，这种情况的结果就是两个bean都不能完成初始化，循环依赖难以解决。
   2. Spring解决循环依赖主要是依赖三级缓存，但是的**在调用构造方法之前还未将其放入三级缓存之中**，因此后续的依赖调用构造方法的时候并不能从三级缓存中获取到依赖的Bean，因此不能解决。
2. 为什么不能解决prototype作用域循环依赖？
   1. 这种循环依赖同样无法解决，因为spring不会缓存‘prototype’作用域的bean，而spring中循环依赖的解决正是通过缓存来实现的
3. 为什么不能解决多例的循环依赖？
   1. 多实例Bean是每次调用一次getBean都会执行一次构造方法并且给属性赋值，根本没有三级缓存，因此不能解决循环依赖。

**其它循环依赖如何解决**

这类循环依赖问题解决方法很多，主要有：

- 使用@Lazy注解，延迟加载
  - 构造器循环依赖这类循环依赖问题可以通过使用@Lazy注解解决
- 使用@DependsOn注解，指定加载先后关系
  - 使用@DependsOn产生的循环依赖：这类循环依赖问题要找到@DependsOn注解循环依赖的地方，迫使它不循环依赖就可以解决问题。
- 修改文件名称，改变循环依赖类的加载顺序
- 多例循环依赖这类循环依赖问题可以通过把bean改成单例的解决。

### 为什么必须都是单例

如果从源码来看的话，循环依赖的 Bean 是原型模式，会直接抛错：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202508032121884.png)

所以 Spring 只支持单例的循环依赖，但是为什么呢?

按照理解，如果两个Bean都是原型模式的话，那么创建A1需要创建一个B1，创建B1的时候要创建一个A2，创建 A2又要创建一个B2，创建 B2又要创建一个A3，创建 A3 又要创建一个 B3.就又卡 BUG 了，是吧，因为原型模式都需要创建新的对象，不能跟用以前的对象。

如果是单例的话，创建 A 需要创建 B，而创建的 B 需要的是之前的个 A，不然就不叫单例了，对吧?
也是基于这点， Spring 就能操作操作了。

具体做法就是：先创建A，此时的A是不完整的(没有注入B)，用个 map 保存这个不完整的A，再创建B，B需要A，所以从那个map 得到“不完整”的A，此时的B就完整了，然后A就可以注入B，然后A就完整了，B也完整了，且它们是相互依赖的。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202508032156256.png)

### 为什么不能全是构造器注入？一个set注入，一个构造器注入一定能成功?

**为什么不能全是构造器注入？**

在 Spring 中创建 Bean 分三步：
1. 实例化，createBeanlnstance，就是 new 了个对象
2. 属性注入，populateBean， 就是 set 一些属性值
3. 初始化，initializeBean，执行一些 aware 接口中的方法，initMethod，AOP代理等

明确了上面这三点，再结合上面说的“不完整的”，我们来理一下。

如果全是构造器注入，比如A(B b)，那表明在 new的时候，就需要得到B，此时需要 new B，但是B也是要在构造的时候注入A，即B(A a)，这时候B需要在一个 map 中找到不完整的A，发现找不到。

为什么找不到?因为A 还没 new 完呢，所以找不到完整的 A，因此如果全是构造器注入的话，那么 Spring 无法处理循环依赖。

**一个set注入，一个构造器注入一定能成功?**

假设我们 A 是通过 set 注入 B，B 通过构造函数注入 A，此时是成功的。

我们来分析下：实例化A之后，此时可以在 map中存入A，开始为A进行属性注入，发现需要B，此时 new B，发现构造器需要A，此时从 map中得到A，B构造完毕，B进行属性注入，初始化，然后A注入B完成属性注入，然后初始化 A。

整个过程很顺利，没毛病。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202508101024624.png)

假设 A 是通过构造器注入 B，B 通过 set 注入 A，此时是失败的。

我们来分析下：实例化A，发现构造函数需要B，此时去实例化B，然后进行B 的属性注入，从 map 里面找不到A，因为 A 还没 new 成功，所以B也卡住了，然后就 循环了。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202508101025934.png)

看到这里，仔细思考的小伙伴可能会说，可以先实例化 B，往 map 里面塞入不完整的 B，这样就能成功实例化 A 了。确实，思路没错但是 Spring 容器是按照字母序创建 Bean 的，A 的创建永远排在 B 前面。

现在我们总结一下:
- 如果循环依赖都是构造器注入，则失败
- 如果循环依赖不完全是构造器注入，则可能成功，可能失败，具体跟BeanName的字母序有关系，

## 二级缓存能不能解决循环依赖？  

Spring 之所以需要三级缓存而不是简单的二级缓存，主要原因在于AOP代理和Bean的早期引用问题。

- 如果只是循环依赖导致的死循环的问题： 一级缓存就可以解决 ，但是无法解决在并发下获取不完整的Bean。  
- 二级缓存虽然可以解决循环依赖的问题，但在涉及到动态代理(OP)时，直接使用二级缓存不做任问处理会导致我们拿到的 Bean 是未代理的原始对象。如果二级缓存内存放的都是代理对象，则违反了 Bean 的生命周期 

### 进一步理解为什么需要三级缓存？

很明显，如果仅仅只是为了破解循环依赖，二级缓存够了，压根就不必要三级。

思考一下，在实例化 BeanA之后，在二级 map里面塞入这个A，然后继续属性注入，发现A依赖B所以要创建 Bean B，这时候B就能以二级 map得到A，完成B的建立之后，A自然而然能完成

所以为什么要搞个三级缓存，且里面存的是创建 Bean 的工厂呢?我们来看下调用工厂的 getObject 到底会做什么，实际会调用下面这个方法：

```java
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (SmartInstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().smartInstantiationAware) {
            exposedObject = bp.getEarlyBeanReference(exposedObject, beanName);
        }
    }
    return exposedObject;
}
```

重点就在中间的判断，如果 false，返回就是参数传进来的 bean，没任何变化。

如果是 tnue 说明有 InstantiationAwarebeanPostProcessors ，且循环的 smartInstantiationAware 类型，如有这个 BeanPostProcessor 说明 Bean 需要被 aop 代理。

我们都知道如果有代理的话，那么我们想要直接拿到的是代理对象，也就是说如果A需要被代理，那么 B依赖的A是已经被代理的A，所以我们不能返回A给 B，而是返回代理的 A 给 B。这个工厂的作用就是判断这个对象是否需要代理，如果否则直接返回，如果是则返回代理对象。

看到这有的小伙伴肯定会问，那跟三级缓存有什么关系，我可以在要放到二级缓存的时候判断这个 Bean ,是否需要代理，如果要直接放代理的对象不就完事儿了。是的，这个思路看起来没任何问题，问题就出在时机，这跟 Bean 的生命周期有关系。

正常代理对象的生成是基于后置处理器，是在被代理的对象初始化后期调用生成的，所以如果你提早代理了其实是违背了 Bean 定义的生命周期，所以 Spring 先在一个三级缓存放置一个工厂，如果产生循环依赖，那么就调用这个工厂提早得到代理对象，如果没产生依赖，这个工厂根本不会被调用，所以 Bean 的生命周期就是对的。至此，我想你应该明白为什么会有三级缓存了。

也明白，其实破坏循环依赖，其实只有二级缓存就够了，但是碍于生命周期的问题，提前暴露工厂延迟代理对象的生成。

对了，不用担心三级缓存因为没有循环依赖，数据堆积的问题，最终单例 Bean 创建完毕都会加入一级缓存，此时会清理二、三级缓存。


<!-- @include: @article-footer.snippet.md -->     
