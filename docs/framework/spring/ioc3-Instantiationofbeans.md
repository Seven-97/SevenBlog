---
title: IOC - Bean的实例化
category: 常用框架
tag:
  - Spring
---



## 生命周期的整体流程

Spring 容器可以管理 singleton 作用域 Bean 的生命周期，在此作用域下，Spring 能够精确地知道该 Bean 何时被创建，何时初始化完成，以及何时被销毁。

而对于 prototype 作用域的 Bean，Spring 只负责创建，当容器创建了 Bean 的实例后，Bean 的实例就交给客户端代码管理，Spring 容器**将不再跟踪其生命周期**。每次客户端请求 prototype 作用域的 Bean 时，Spring 容器都会创建一个新的实例，并且不会管那些被配置成 prototype 作用域的 Bean 的生命周期。

了解 Spring 生命周期的意义就在于，**可以利用 Bean 在其存活期间的指定时刻完成一些相关操作**。这种时刻可能有很多，但一般情况下，会在 Bean 被初始化后和被销毁前执行一些相关操作。

 



在执行初始化方法之前和之后，还需要对Bean的后置处理器BeanPostProcessors进行处理

1. 在invokeInitMethods 的前后进行applyBeanPostProcessorsBeforeInitialization，applyBeanPostProcessorsAfterInitialization

2. 在后置处理中处理了包括：AOP【AnnotationAwareAspectJAutoProxyCreator】，负责 构造后@PostConstruct 和 销毁前@PreDestroy 的 InitDestoryAnnotationBeanPostProcessor 等

3. 以及通过实现BeanPostProcessor接口的自定义处理器



整体流程如下：

1. 加载Bean定义：通过 loadBeanDefinitions 扫描所有xml配置、注解将Bean记录在beanDefinitionMap中。即[IOC容器的初始化过程](https://www.seven97.top/framework/spring/ioc2-initializationprocess.html)

2. Bean实例化：遍历 beanDefinitionMap 创建bean，最终会使用getBean中的doGetBean方法调用 createBean来创建Bean对象

   1. 构建对象：容器通过 createBeanInstance 进行对象构造

      1. 获取构造方法（大部分情况下只有一个构造方法）

         1. 如果只有一个构造方法，无论这个构造方法有没有入参，都用这个构造方法

         2. 有多个构造方法时

            1.  先拿带有@Autowired的构造方法，但是如果多个构造方法都有@Autowired就会报错

            2.  如果没有带有@Autowired的构造方法，那就找没有入参的；如果多个构造方法都是有入参的，那也会报错

      2. 准备参数 

         1. 先根据类进行查找

         2. 如果这个类有多个实例，则再根据参数名匹配

         3. 如果没有找到则报错

      3. 构造对象：无参构造方法则直接实例化

   2. 填充属性：通过populateBean方法为Bean内部所需的属性进行赋值，通常是 @Autowired 注解的变量；通过三级缓存机制进行填充，也就是依赖注入

   3. 初始化Bean对象：通过initializeBean对填充后的实例进行初始化

      1. 执行Aware：检查是否有实现着三个Aware，BeanNameAware，BeanClassLoaderAware, BeanFactoryAware；让实例化后的对象能够感知自己在Spring容器里的存在的位置信息，创建信息

      2. 初始化前：BeanPostProcessor，也就是拿出所有的后置处理器对bean进行处理，当有一个处理器返回null，将不再调用后面的处理器处理。

      3. 初始化：afterPropertiesSet，init- method；

         1. 实现了InitializingBean接口的类执行其afterPropertiesSet()方法

         2. 从BeanDefinition中获取initMethod方法

      4. 初始化后：BeanPostProcessor,；获取所有的bean的后置处理器去执行。AOP也是在这里做的

   4. 注册销毁：通过reigsterDisposableBean处理实现了DisposableBean接口的Bean的注册

      1. Bean是否有注册为DisposableBean的资格：

         1. 是否有destroyMethod。

         2. 是否有执行销毁方法的后置处理器。

      2. DisposableBeanAdapter： 推断destoryMethod

      3. 完成注册

3. 添加到单例池：通过 addSingleton 方法，将Bean 加入到单例池 singleObjects

4. 销毁

   1. 销毁前：如果有@PreDestory 注解的方法就执行

   2. 如果有自定义的销毁后置处理器，通过 postProcessBeforeDestruction 方法调用destoryBean逐一销毁Bean

   3. 销毁时：如果实现了destroyMethod就执行 destory方法

   4. 执行客户自定义销毁：调用 invokeCustomDestoryMethod执行在Bean上自定义的destroyMethod方法

      1. 有这个自定义销毁就会执行

      2. 没有自定义destroyMethod方法就会去执行close方法

      3. 没有close方法就会去执行shutdown方法

      4. 都没有的话就都不执行，不影响





## Bean的实例化

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281112999.png)

BeanFactory定义了Bean容器的规范，其中包含根据bean的名字, Class类型和参数等来得到bean实例。

```java
// 根据bean的名字和Class类型等来得到bean实例    
Object getBean(String name) throws BeansException;    
Object getBean(String name, Class requiredType) throws BeansException;    
Object getBean(String name, Object... args) throws BeansException;
<T> T getBean(Class<T> requiredType) throws BeansException;
<T> T getBean(Class<T> requiredType, Object... args) throws BeansException;
```

IoC初始化时，最终是将Bean的定义即BeanDefinition放到beanDefinitionMap中，本质上是一个ConcurrentHashMap；并且BeanDefinition接口中包含了这个类的Class信息以及是否是单例等；

当需要进行创建Bean对象时，就是通过遍历beanDefinitionMap来创建Bean

### 主体思路

BeanFactory实现getBean方法在AbstractBeanFactory中，这个方法重载都是调用doGetBean方法进行实现的：

```java
public Object getBean(String name) throws BeansException {
  return doGetBean(name, null, null, false);
}
public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
  return doGetBean(name, requiredType, null, false);
}
public Object getBean(String name, Object... args) throws BeansException {
  return doGetBean(name, null, args, false);
}
public <T> T getBean(String name, @Nullable Class<T> requiredType, @Nullable Object... args)
    throws BeansException {
  return doGetBean(name, requiredType, args, false);
}
```



### doGetBean

我们来看下doGetBean方法(这个方法很长，我们主要看它的整体思路和设计要点）：

```java
// 参数typeCheckOnly：bean实例是否包含一个类型检查
protected <T> T doGetBean(
			String name, @Nullable Class<T> requiredType, @Nullable Object[] args, boolean typeCheckOnly)
			throws BeansException {

  // 解析bean的真正name，如果bean是工厂类，name前缀会加&，需要去掉
  String beanName = transformedBeanName(name);
  Object beanInstance;

  // Eagerly check singleton cache for manually registered singletons.
  Object sharedInstance = getSingleton(beanName);
  if (sharedInstance != null && args == null) {
    // 无参单例从缓存中获取
    beanInstance = getObjectForBeanInstance(sharedInstance, name, beanName, null);
  }

  else {
    // 如果bean实例还在创建中，则直接抛出异常
    if (isPrototypeCurrentlyInCreation(beanName)) {
      throw new BeanCurrentlyInCreationException(beanName);
    }

    // 如果 bean definition 存在于父的bean工厂中，委派给父Bean工厂获取
    BeanFactory parentBeanFactory = getParentBeanFactory();
    if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
      // Not found -> check parent.
      String nameToLookup = originalBeanName(name);
      if (parentBeanFactory instanceof AbstractBeanFactory) {
        return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
            nameToLookup, requiredType, args, typeCheckOnly);
      }
      else if (args != null) {
        // Delegation to parent with explicit args.
        return (T) parentBeanFactory.getBean(nameToLookup, args);
      }
      else if (requiredType != null) {
        // No args -> delegate to standard getBean method.
        return parentBeanFactory.getBean(nameToLookup, requiredType);
      }
      else {
        return (T) parentBeanFactory.getBean(nameToLookup);
      }
    }

    if (!typeCheckOnly) {
      // 将当前bean实例放入alreadyCreated集合里，标识这个bean准备创建了
      markBeanAsCreated(beanName);
    }

    StartupStep beanCreation = this.applicationStartup.start("spring.beans.instantiate")
        .tag("beanName", name);
    try {
      if (requiredType != null) {
        beanCreation.tag("beanType", requiredType::toString);
      }
      RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
      checkMergedBeanDefinition(mbd, beanName, args);

      // 确保它的依赖也被初始化了.
      String[] dependsOn = mbd.getDependsOn();
      if (dependsOn != null) {
        for (String dep : dependsOn) {
          if (isDependent(beanName, dep)) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
          }
          registerDependentBean(dep, beanName);
          try {
            getBean(dep); // 初始化它依赖的Bean
          }
          catch (NoSuchBeanDefinitionException ex) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
          }
        }
      }

      // 创建Bean实例：单例
      if (mbd.isSingleton()) {
        sharedInstance = getSingleton(beanName, () -> {
          try {
            // 真正创建bean的方法
            return createBean(beanName, mbd, args);
          }
          catch (BeansException ex) {
            // Explicitly remove instance from singleton cache: It might have been put there
            // eagerly by the creation process, to allow for circular reference resolution.
            // Also remove any beans that received a temporary reference to the bean.
            destroySingleton(beanName);
            throw ex;
          }
        });
        beanInstance = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
      }
      // 创建Bean实例：原型
      else if (mbd.isPrototype()) {
        // It's a prototype -> create a new instance.
        Object prototypeInstance = null;
        try {
          beforePrototypeCreation(beanName);
          prototypeInstance = createBean(beanName, mbd, args);
        }
        finally {
          afterPrototypeCreation(beanName);
        }
        beanInstance = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
      }
      // 创建Bean实例：根据bean的scope创建
      else {
        String scopeName = mbd.getScope();
        if (!StringUtils.hasLength(scopeName)) {
          throw new IllegalStateException("No scope name defined for bean ´" + beanName + "'");
        }
        Scope scope = this.scopes.get(scopeName);
        if (scope == null) {
          throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
        }
        try {
          Object scopedInstance = scope.get(beanName, () -> {
            beforePrototypeCreation(beanName);
            try {
              return createBean(beanName, mbd, args);
            }
            finally {
              afterPrototypeCreation(beanName);
            }
          });
          beanInstance = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
        }
        catch (IllegalStateException ex) {
          throw new ScopeNotActiveException(beanName, scopeName, ex);
        }
      }
    }
    catch (BeansException ex) {
      beanCreation.tag("exception", ex.getClass().toString());
      beanCreation.tag("message", String.valueOf(ex.getMessage()));
      cleanupAfterBeanCreationFailure(beanName);
      throw ex;
    }
    finally {
      beanCreation.end();
    }
  }

  return adaptBeanInstance(name, beanInstance, requiredType);
}
```

逻辑流程如下：

- 解析bean的真正name，如果bean是工厂类，name前缀会加&，需要去掉
- 无参单例先从缓存中尝试获取
- 如果bean实例还在创建中，则直接抛出异常
- 如果bean definition 存在于父的bean工厂中，委派给父Bean工厂获取
- 标记这个beanName的实例正在创建
- 确保它的依赖也被初始化
- 真正创建 
  - 单例时
  - 原型时
  - 根据bean的scope创建



接下来就是Bean真正创建的过程

## createBean

这个方法整体流程图如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281115207.png)

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBean(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition, java.lang.Object[])

```java
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) throws BeanCreationException {
    if (this.logger.isTraceEnabled()) {
        this.logger.trace("Creating instance of bean '" + beanName + "'");
    }

    RootBeanDefinition mbdToUse = mbd;
    //类加载
    Class<?> resolvedClass = this.resolveBeanClass(mbd, beanName, new Class[0]);
    if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
        mbdToUse = new RootBeanDefinition(mbd);
        mbdToUse.setBeanClass(resolvedClass);
    }

    try {
        //对通过XML定义的bean中的look-up方法进行预处理
        mbdToUse.prepareMethodOverrides();
    } catch (BeanDefinitionValidationException var9) {
        throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(), beanName, "Validation of method overrides failed", var9);
    }

    Object beanInstance;
    try {
        //实例化前，null
        beanInstance = this.resolveBeforeInstantiation(beanName, mbdToUse);
        if (beanInstance != null) {//这里就是实例化前去执行了 “初始化之前和之后” 的流程，那么就有可能返回一个不是null的Bean
            return beanInstance;//就直接返回这个Bean对象了，不会再往正常走后续的Spring正常流程了
        }
    } catch (Throwable var10) {
        throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName, "BeanPostProcessor before instantiation of bean failed", var10);
    }

    try {
        //这个就是走正常的Spring创建Bean的方法
        beanInstance = this.doCreateBean(beanName, mbdToUse, args);
        if (this.logger.isTraceEnabled()) {
            this.logger.trace("Finished creating instance of bean '" + beanName + "'");
        }

        return beanInstance;
    } catch (ImplicitlyAppearedSingletonException | BeanCreationException var7) {
        throw var7;
    } catch (Throwable var8) {
        throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", var8);
    }
}
```

### 实例化之前

会对Bean的后置处理器BeanPostProcessors进行处理

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#resolveBeforeInstantiation

```java
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
    Object bean = null;
    if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
        //检查有没有后置处理器
        if (!mbd.isSynthetic() && this.hasInstantiationAwareBeanPostProcessors()) {
            Class<?> targetType = this.determineTargetType(beanName, mbd);
            if (targetType != null) {
                //实例化前
                bean = this.applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
                if (bean != null) {//正常情况bean是null,如果不是Null
                    //那就去执行初始化之后的方法。但是这样的话就不会走正常spring创建Bean的流程了，会直接返回这个bean对象
                    bean = this.applyBeanPostProcessorsAfterInitialization(bean, beanName);
                }
            }
        }

        mbd.beforeInstantiationResolved = bean != null;
    }

    return bean;
}
```

```java
@Nullable
protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
    Iterator var3 = this.getBeanPostProcessors().iterator();
    //拿到所有的后置处理器去执行
    while(var3.hasNext()) {
        BeanPostProcessor bp = (BeanPostProcessor)var3.next();
        if (bp instanceof InstantiationAwareBeanPostProcessor) {
            InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor)bp;
            Object result = ibp.postProcessBeforeInstantiation(beanClass, beanName);
            if (result != null) {
                return result;
            }
        }
    }

    return null;
}
```



### doCreateBean

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean

```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) throws BeanCreationException {
    BeanWrapper instanceWrapper = null;
    if (mbd.isSingleton()) {
        // factoryBeanObjectCache：存的是beanName对应的factoryBean.getObjetc返回的对象
        // factoryBeanInstanceCache：存的是beanName对应的FactoryBean实例对象
        instanceWrapper = (BeanWrapper)this.factoryBeanInstanceCache.remove(beanName);
    }
    
    //1.实例化
    if (instanceWrapper == null) {
        //这里就开始构造对象了
        instanceWrapper = this.createBeanInstance(beanName, mbd, args);
    }
    
    //原始对象
    Object bean = instanceWrapper.getWrappedInstance();
    Class<?> beanType = instanceWrapper.getWrappedClass();
    if (beanType != NullBean.class) {
        mbd.resolvedTargetType = beanType;
    }

    synchronized(mbd.postProcessingLock) {
        if (!mbd.postProcessed) {
            try {
                //运行修改合并娃了 "BeanDefinition
                //这里会查Autowired的注入点(InjectedELement),并把这些注入点添加到mbd的属性external
                this.applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
            } catch (Throwable var17) {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Post-processing of merged bean definition failed", var17);
            }

            mbd.postProcessed = true;
        }
    }
    
    //如果当前创建的是单例bean，并且允许循环依赖，并且还在创建过程中，那么则提早暴露
    boolean earlySingletonExposure = mbd.isSingleton() && this.allowCircularReferences && this.isSingletonCurrentlyInCreation(beanName);
    if (earlySingletonExposure) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace("Eagerly caching bean '" + beanName + "' to allow for resolving potential circular references");
        }
        
        //此时的bean还没有完成属性注入，是一个非常简单的对象
        //构造一个对象工厂添加到singletonFactories中
        //第四次调用后置处理器
        this.addSingletonFactory(beanName, () -> {
            return this.getEarlyBeanReference(beanName, mbd, bean);
        });
    }
    
    //对象已经暴露出去了
    Object exposedObject = bean;

    try {
        //2. 填充属性
        this.populateBean(beanName, mbd, instanceWrapper);
        //3.初始化Bean对象
        exposedObject = this.initializeBean(beanName, exposedObject, mbd);
    } catch (Throwable var18) {
        if (var18 instanceof BeanCreationException && beanName.equals(((BeanCreationException)var18).getBeanName())) {
            throw (BeanCreationException)var18;
        }

        throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Initialization of bean failed", var18);
    }

    if (earlySingletonExposure) {
        // 在解决循环依赖时，当AService的属性注入完了之后，从getSingleton中得到AService AOP之后的代理对象
        Object earlySingletonReference = this.getSingleton(beanName, false);
       
        if (earlySingletonReference != null) {
             // 如果提前暴露的对象和经过了完整的生命周期后的对象相等，则把代理对象赋值给exposedObject
            // 最终会添加到singletonObjects中去
            if (exposedObject == bean) {
                exposedObject = earlySingletonReference;
            }
            // 如果提前暴露的对象和经过了完整的生命周期后的对象不相等
            // allowRawInjectionDespiteWrapping表示在循环依赖时，只能 
            else if (!this.allowRawInjectionDespiteWrapping && this.hasDependentBean(beanName)) {
                String[] dependentBeans = this.getDependentBeans(beanName);
                Set<String> actualDependentBeans = new LinkedHashSet(dependentBeans.length);
                String[] var12 = dependentBeans;
                int var13 = dependentBeans.length;

                for(int var14 = 0; var14 < var13; ++var14) {
                    String dependentBean = var12[var14];
                    if (!this.removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                        actualDependentBeans.add(dependentBean);
                    }
                }

                if (!actualDependentBeans.isEmpty()) {
                    // 也就是说其他bean没有用到AService的最终版本
                    throw new BeanCurrentlyInCreationException(beanName, "Bean with name '" + beanName + "' has been injected into other beans [" + StringUtils.collectionToCommaDelimitedString(actualDependentBeans) + "] in its raw version as part of a circular reference, but has eventually been wrapped. This means that said other beans do not use the final version of the bean. This is often the result of over-eager type matching - consider using 'getBeanNamesForType' with the 'allowEagerInit' flag turned off, for example.");
                }
            }
        }
    }

    try {
        //4.注册销毁流程
        this.registerDisposableBeanIfNecessary(beanName, bean, mbd);
        return exposedObject;
    } catch (BeanDefinitionValidationException var16) {
        throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Invalid destruction signature", var16);
    }
}
```

#### 构建对象

**createBeanInstance**创建一个Bean实例，就是返回一个原始对象

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance

```java
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
    //1.得到Bean的class,并验证class的访问权限是不是public
    Class<?> beanClass = this.resolveBeanClass(mbd, beanName, new Class[0]);
    if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
        throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
    } else {
        
        //2. Spring提供给开发者的扩展点
        //如果开发者要自己来实现创建对象的过程，那么，那么就可以提供一个Supplier的实现类
        //当一个BeanDefinition中存在一个Supplier的实现类，Spring就利用这个类的get方法获取实例
        //而不再走Spring的创建逻辑
        Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
        if (instanceSupplier != null) {
            return this.obtainFromSupplier(instanceSupplier, beanName);
        } else if (mbd.getFactoryMethodName() != null) {
            //3. 通过FactoryMethod实例化这个Bean
            // FactoryMethod这个名称在xml中还是比较常见的，即通过工厂方法来创建Bean对象
            return this.instantiateUsingFactoryMethod(beanName, mbd, args);
        } else {
            boolean resolved = false;
            boolean autowireNecessary = false;
            //如果在创建bean时没有手动指定构造方法的参数。那么则看BeanDefinition是不是已经确定了要使用的构造方法
            //注意:如果没有手动指定参数，那么就肯定时自动推断出来的，所以一旦发现当BeanDefinition中已经确定了要使用
            //那么就要使autowireConstructor()方法来构造一个bean对象
            if (args == null) {
                synchronized(mbd.constructorArgumentLock) {
                    if (mbd.resolvedConstructorOrFactoryMethod != null) {
                        resolved = true;
                        autowireNecessary = mbd.constructorArgumentsResolved;
                    }
                }
            }

            if (resolved) {
                //resolved为true表示当前bean的构造方法已经确定了
                return autowireNecessary ? 
                this.autowireConstructor(beanName, mbd, (Constructor[])null, (Object[])null) 
                //如果构造方法已经确定了，但是没有确定构造方法参数，那就表示没有构造方法参数，用无参来实现构造方法
                : this.instantiateBean(beanName, mbd);
            } else {
                //推断构造法
                Constructor<?>[] ctors = this.determineConstructorsFromBeanPostProcessors(beanClass, beanName);
                //通过BeanPostProcessor找出了构造方法
                //或者BeanDefinition的autowire属性为AUTOWIRE_ CONSTRUCTOR
                //或者BeanDefinition中指定J构造方法参数值
                //或者在getBean()时指定了args
                if (ctors == null && mbd.getResolvedAutowireMode() != 3 && !mbd.hasConstructorArgumentValues() && ObjectUtils.isEmpty(args)) {
                    ctors = mbd.getPreferredConstructors();                    
                    return ctors != null ? this.autowireConstructor(beanName, mbd, ctors, (Object[])null) : this.instantiateBean(beanName, mbd);
                } else {
                    //进行构造方法推断并实例化
                    return this.autowireConstructor(beanName, mbd, ctors, args);
                }
            }
        }
    }
}
```

#### 填充属性

Spring使用实现了InstantiationAwareBeanPostProcessor的后置处理器对实例化后的Bean进行处理

- @Autowired注解的AutowiredAnnotationBeanPostProcessor
- @Resource注解的CommonAnnotationBeanPostProcessor
- byName和byType|



org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean

```java
protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
    if (bw == null) {
        if (mbd.hasPropertyValues()) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
        }
    } else {
        //可以提供InstantiationAwareBeanPostProcessor,控制对象的属性注入
        //可以自己写一个InstantiationAwareBeanPostProcessor,然后重写postProcessAfterInstantiation,
        if (!mbd.isSynthetic() && this.hasInstantiationAwareBeanPostProcessors()) {
            Iterator var4 = this.getBeanPostProcessors().iterator();

            while(var4.hasNext()) {
                BeanPostProcessor bp = (BeanPostProcessor)var4.next();
                if (bp instanceof InstantiationAwareBeanPostProcessor) {
                    InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor)bp;
                    if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                        return;
                    }
                }
            }
        }
        
        //是否在BeanDefinition中设置了属性值
        PropertyValues pvs = mbd.hasPropertyValues() ? mbd.getPropertyValues() : null;
        int resolvedAutowireMode = mbd.getResolvedAutowireMode();
        // byname是根据根据属性名字找bean, 1就是byname
        // bytype足根据属性所对应的set方法的参数类型找bean，2就是bytype
        //找到bean之后都要调set方法进行注入
        if (resolvedAutowireMode == 1 || resolvedAutowireMode == 2) {
            MutablePropertyValues newPvs = new MutablePropertyValues((PropertyValues)pvs);
            if (resolvedAutowireMode == 1) {
                this.autowireByName(beanName, mbd, bw, newPvs);
            }

            if (resolvedAutowireMode == 2) {
                this.autowireByType(beanName, mbd, bw, newPvs);
            }
            
            //总结一下
            //其实就是Spring自动的根据某个类中的set方法来找bean, byName 就是根据基个set方法所对应的属性名去找Bean
            // byType, 就是根据菜个set方法的参数类型去找bean
            //注意，执行完这里的代码之后，这是把属性以及找到的值存在了pvs里面，并没有完成反射赋值
            pvs = newPvs;
        }
        
        //执行完了Spring的自动注入之后，就开始解Autowired,这里叫做实例化回调
        boolean hasInstAwareBpps = this.hasInstantiationAwareBeanPostProcessors();
        boolean needsDepCheck = mbd.getDependencyCheck() != 0;
        
        // @Autowired注解的AutowiredAnnotationBeanPostProcessor
        // @Resource注解的CommonAnnotationBeanPos tProcessor .
        PropertyDescriptor[] filteredPds = null;
        if (hasInstAwareBpps) {
            if (pvs == null) {
                pvs = mbd.getPropertyValues();
            }

            Iterator var9 = this.getBeanPostProcessors().iterator();

            while(var9.hasNext()) {
                BeanPostProcessor bp = (BeanPostProcessor)var9.next();
                if (bp instanceof InstantiationAwareBeanPostProcessor) {
                    InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor)bp;
                    //调BeanPostProcessor 分别解析 @Autowired. @Resource、 @Value， 得到属性值
                    PropertyValues pvsToUse = ibp.postProcessProperties((PropertyValues)pvs, bw.getWrappedInstance(), beanName);
                    if (pvsToUse == null) {
                        if (filteredPds == null) {
                            filteredPds = this.filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
                        }

                        pvsToUse = ibp.postProcessPropertyValues((PropertyValues)pvs, filteredPds, bw.getWrappedInstance(), beanName);
                        if (pvsToUse == null) {
                            return;
                        }
                    }

                    pvs = pvsToUse;
                }
            }
        }

        if (needsDepCheck) {
            if (filteredPds == null) {
                filteredPds = this.filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
            }

            this.checkDependencies(beanName, mbd, filteredPds, (PropertyValues)pvs);
        }

        if (pvs != null) {
            //pvs其实就是属性已经对应的值
            //这里面的逻辑就是通过反射获取到name和value来进行填充了
            this.applyPropertyValues(beanName, mbd, bw, (PropertyValues)pvs);
        }

    }
}
```



#### 初始化Bean对象

通过initializeBean对填充属性后的实例进行初始化

1. 执行Aware：检查是否有实现着三个Aware，BeanNameAware，BeanClassLoaderAware, BeanFactoryAware
2. 初始化前：BeanPostProcessor
3. 初始化：afterPropertiesSet，init- method
4. 初始化后：BeanPostProcessor, AOP

如果是单列Bean,则加入单例池当中。以后使用单例，从单例池中获取

```java
protected Object initializeBean(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {
    if (System.getSecurityManager() != null) {
        AccessController.doPrivileged(() -> {
            this.invokeAwareMethods(beanName, bean);
            return null;
        }, this.getAccessControlContext());
    } else {
        //1.执行Aware
        this.invokeAwareMethods(beanName, bean);
    }

    Object wrappedBean = bean;
    if (mbd == null || !mbd.isSynthetic()) {
        //2.初始化前：就是执行所有的后置处理器
        wrappedBean = this.applyBeanPostProcessorsBeforeInitialization(bean, beanName);
    }

    try {
        //3.初始化
        this.invokeInitMethods(beanName, wrappedBean, mbd);
    } catch (Throwable var6) {
        throw new BeanCreationException(mbd != null ? mbd.getResourceDescription() : null, beanName, "Invocation of init method failed", var6);
    }

    if (mbd == null || !mbd.isSynthetic()) {
        //4. 初始化后 AOP就是在这里做的
        wrappedBean = this.applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }

    return wrappedBean;
}
```

##### 执行Aware

就是看看有没有实现这三个Aware，有的话就执行相关方法

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#invokeAwareMethods

```java
private void invokeAwareMethods(String beanName, Object bean) {
    if (bean instanceof Aware) {
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware)bean).setBeanName(beanName);
        }

        if (bean instanceof BeanClassLoaderAware) {
            ClassLoader bcl = this.getBeanClassLoader();
            if (bcl != null) {
                ((BeanClassLoaderAware)bean).setBeanClassLoader(bcl);
            }
        }

        if (bean instanceof BeanFactoryAware) {
            ((BeanFactoryAware)bean).setBeanFactory(this);
        }
    }

}
```

##### 初始化前

拿出所有的后置处理器对bean进行处理，当有一个处理器返回null，将不再调用后面的处理器处理。

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsBeforeInitialization

```java
public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws BeansException {
    Object result = existingBean;

    Object current;
    for(Iterator var4 = this.getBeanPostProcessors().iterator(); var4.hasNext(); result = current) {
        BeanPostProcessor processor = (BeanPostProcessor)var4.next();
        current = processor.postProcessBeforeInitialization(result, beanName);
        if (current == null) {
            return result;
        }
    }

    return result;
}
```

完成初始化前的操作有2种方式：

1. TestBeanPostProcessor 实现了 BeanPostProcessor ，重写postProcessBeforeInitialization方法。
2. 使用@PostConstruct。由CommonAnnotationBeanPostProcessor处理器处理。@PostConstruct是在实例化bean完成之后

##### 初始化时

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#invokeInitMethods

```java
protected void invokeInitMethods(String beanName, Object bean, @Nullable RootBeanDefinition mbd) throws Throwable {
    boolean isInitializingBean = bean instanceof InitializingBean;
    //如果Bean实现了InitializingBean，则进行处理
    if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
        }

        if (System.getSecurityManager() != null) {
            try {
                AccessController.doPrivileged(() -> {
                    //执行afterPropertiesSet
                    ((InitializingBean)bean).afterPropertiesSet();
                    return null;
                }, this.getAccessControlContext());
            } catch (PrivilegedActionException var6) {
                throw var6.getException();
            }
        } else {
            ((InitializingBean)bean).afterPropertiesSet();
        }
    }

    if (mbd != null && bean.getClass() != NullBean.class) {
        String initMethodName = mbd.getInitMethodName();
        if (StringUtils.hasLength(initMethodName) && (!isInitializingBean || !"afterPropertiesSet".equals(initMethodName)) && !mbd.isExternallyManagedInitMethod(initMethodName)) {
            //执行自定义的init-method
            this.invokeCustomInitMethod(beanName, bean, mbd);
        }
    }

}
```

1. 实现了InitializingBean接口的类执行其afterPropertiesSet()方法
2. 从BeanDefinition中获取initMethod方法。

可以是xml文件中配置的：

```xml
<bean id="userService" class="com.spring.service.UserService" init-method="initMethod"/>
```

或者是@Bean注解配置：

```java
@Bean(initMethod = "initMethod", destroyMethod = "destroyMethod")
public UserService userService() {
    return new UserService();
}
```

##### 初始化后

getBeanPostProcessors()获取所有的后置处理器，包括自己定义的

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsBeforeInitialization

```java
public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws BeansException {
    Object result = existingBean;

    Object current;
    for(Iterator var4 = this.getBeanPostProcessors().iterator(); var4.hasNext(); result = current) {
        BeanPostProcessor processor = (BeanPostProcessor)var4.next();
        current = processor.postProcessBeforeInitialization(result, beanName);
        if (current == null) {
            return result;
        }
    }

    return result;
}
```



特别注意：.在实例化前如果获取到了bean那么将不执行spring正常创建bean的流程，而是直接调用初始化后的方法完成初始化后的操作。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404281120393.png)

#### 注册销毁

Spring在容器关闭时，会remove容器里所有的Bean。如果需要某些Bean在被Spring删除前执行一些逻辑，Spring也可以做到，那么就需要在Bean完成创建时将这个Bean注册为DisposableBean。

1.  Bean是否有注册为DisposableBean的资格，是否有destroyMethod。
2. 是否有执行销毁方法的后置处理器。
3. DisposableBeanAdapter
   1. 推断destoryMethod
   2. 指定的destroyMethod, AutoCloseable(close方 法)，没有shutdown方法
4. 完成注册：就是将完成包装的DisposableBeanAdapter, put到 销毁容器中。

##### 是否有注册为DisposableBean的资格

```java
protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
    AccessControlContext acc = System.getSecurityManager() != null ? this.getAccessControlContext() : null;
    //1. Bean是否有注册为DisposableBean的资格
    、、 不能是原型Bean（因为原型Bean不会放到Spring容器中），再通过requiresDestruction方法去判断一下是否需要销毁。
    if (!mbd.isPrototype() && this.requiresDestruction(bean, mbd)) {
        if (mbd.isSingleton()) {
            this.registerDisposableBean(beanName, new DisposableBeanAdapter(bean, beanName, mbd, this.getBeanPostProcessors(), acc));
        } else {
            Scope scope = (Scope)this.scopes.get(mbd.getScope());
            if (scope == null) {
                throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
            }

            scope.registerDestructionCallback(beanName, new DisposableBeanAdapter(bean, beanName, mbd, this.getBeanPostProcessors(), acc));
        }
    }

}
```

判断某个bean是否拥有destroyMethod方法

```java
protected boolean requiresDestruction(Object bean, RootBeanDefinition mbd) {
    // 1. 实现了DisposableBean接口或AutoCloseable按口
    // 2. BeanDefinition 中定义了destroyMethodName
    // 3. 类中是否存在@PreDestroy注解的方法
    // 4. 由DestructionAwareBeanPostProcessor判断是否需要销毁
    return bean.getClass() != NullBean.class 
    && (DisposableBeanAdapter.hasDestroyMethod(bean, mbd) 
    || this.hasDestructionAwareBeanPostProcessors() 
    && DisposableBeanAdapter.hasApplicableProcessors(bean, this.getBeanPostProcessors()));
}
```



##### 是否有执行销毁方法的后置处理器

由DestructionAwareBeanPostProcessor去标识一个Bean是否需要销毁,并且交给这个后置处理器去处理销毁前的逻辑。

```java
    public static boolean hasApplicableProcessors(Object bean, List<BeanPostProcessor> postProcessors) {
        if (!CollectionUtils.isEmpty(postProcessors)) {
            for (BeanPostProcessor processor : postProcessors) {
                if (processor instanceof DestructionAwareBeanPostProcessor) {
                    DestructionAwareBeanPostProcessor dabpp = (DestructionAwareBeanPostProcessor) processor;
                    if (dabpp.requiresDestruction(bean)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
```

##### DisposableBeanAdapter

当一个bean有资格成为DisposableBean的时候，Spring不是将这个Bean直接放到需要销毁的容器当中，而是将其包装为DisposableBeanAdapter对象放入销毁容器中。

```java
    protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
        AccessControlContext acc = (System.getSecurityManager() != null ? getAccessControlContext() : null);
        if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
            if (mbd.isSingleton()) {
    
                registerDisposableBean(beanName,
                        new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
            }
            else {
                
                Scope scope = this.scopes.get(mbd.getScope());
                if (scope == null) {
                    throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
                }
                scope.registerDestructionCallback(beanName,
                        new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
            }
        }
    }
```

单例Bean销毁容器是一个以beanName为key，DisposableBean为value的Map。

```java
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

    /** Disposable bean instances: bean name to disposable instance. */ 
    private final Map<String, Object> disposableBeans = new LinkedHashMap<>();

    public void registerDisposableBean(String beanName, DisposableBean bean) {
        synchronized (this.disposableBeans) {
            this.disposableBeans.put(beanName, bean);
        }
    }
}    
```



推断destoryMethod

在将Bean包装为DisposableBeanAdapter时，要去推断destoryMethod。

```java
    //挑选到底执行哪一个销毁方法，仅仅返回方法名称。
    @Nullable
    private String inferDestroyMethodIfNecessary(Object bean, RootBeanDefinition beanDefinition) {
        //1.指定的destroyMethod就用
        String destroyMethodName = beanDefinition.getDestroyMethodName();

        //2.没有指定destroyMethodName，没有(inferred)，而且没有实现AutoCloseable(close方法)，返回null
        if (AbstractBeanDefinition.INFER_METHOD.equals(destroyMethodName) ||
                (destroyMethodName == null && bean instanceof AutoCloseable)) {

            // Only perform destroy method inference or Closeable detection
            // in case of the bean not explicitly implementing DisposableBean

            // 3.有(inferred)，或者实现了AutoCloseable(close方法)
            if (!(bean instanceof DisposableBean)) {
                try {
                    //没有实现DisposableBean(destroy方法)，并且有close方法就返回。
                    return bean.getClass().getMethod(CLOSE_METHOD_NAME).getName();
                }
                catch (NoSuchMethodException ex) {
                    try {
                        //没有close会报错，看有没有shutdown方法,有就返回
                        return bean.getClass().getMethod(SHUTDOWN_METHOD_NAME).getName();
                    }
                    catch (NoSuchMethodException ex2) {
                        //没有shutdown方法报错，但异常被吞了，不做任何处理，返回null
                        // no candidate destroy method found
                    }
                }
            }
            return null;
        }
        return (StringUtils.hasLength(destroyMethodName) ? destroyMethodName : null);
    }
```

1. 推断方法名：
   1. 如果指定了DestroyMethod 就返回指定的。
   2. 有推断标记或者实现了AutoCloseable接口，再看是否实现DisposableBean接口的；如果实现了就直接返回null,如果没有实现就去推断是否有公有close方法，有就返回，没有再去推断是否有公有的shutdown方法，有就返回。
2. 推断方法：

```java
    @Nullable
    private Method findDestroyMethod(String name) {
        return (this.nonPublicAccessAllowed ?
                BeanUtils.findMethodWithMinimalParameters(this.bean.getClass(), name) :
                BeanUtils.findMethodWithMinimalParameters(this.bean.getClass().getMethods(), name));
    }
```

根据方法名推断方法,获取最少参数的方法对象method。如果method的参数大于1会报错，如果等于1且参数类型不为boolean类型也会报错。

##### 完成注册

就是将完成包装的DisposableBeanAdapter, put到 销毁容器中。



## 添加到单例池

org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton(java.lang.String, org.springframework.beans.factory.ObjectFactory)

```java
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
    Assert.notNull(beanName, "Bean name must not be null");
    synchronized(this.singletonObjects) {
        Object singletonObject = this.singletonObjects.get(beanName);
        //如果不存在实例，则创建单例bean
        if (singletonObject == null) {
            //当前bean正在销毁
            if (this.singletonsCurrentlyInDestruction) {
                throw new BeanCreationNotAllowedException(beanName, "Singleton bean creation not allowed while singletons of this factory are in destruction (Do not request a bean from a BeanFactory in a destroy method implementation!)");
            }

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
            }

            this.beforeSingletonCreation(beanName);
            boolean newSingleton = false;
            boolean recordSuppressedExceptions = this.suppressedExceptions == null;
            if (recordSuppressedExceptions) {
                this.suppressedExceptions = new LinkedHashSet();
            }

            try {
                singletonObject = singletonFactory.getObject();
                newSingleton = true;
            } catch (IllegalStateException var16) {
                singletonObject = this.singletonObjects.get(beanName);
                if (singletonObject == null) {
                    throw var16;
                }
            } catch (BeanCreationException var17) {
                BeanCreationException ex = var17;
                if (recordSuppressedExceptions) {
                    Iterator var8 = this.suppressedExceptions.iterator();

                    while(var8.hasNext()) {
                        Exception suppressedException = (Exception)var8.next();
                        ex.addRelatedCause(suppressedException);
                    }
                }

                throw ex;
            } finally {
                if (recordSuppressedExceptions) {
                    this.suppressedExceptions = null;
                }
                //将刚刚正在创建的beanName从SingletonCreation中移除
                this.afterSingletonCreation(beanName);
            }

            if (newSingleton) {
                //将创建好的单例bean添加到单例池singletonObjects中
                this.addSingleton(beanName, singletonObject);
            }
        }

        return singletonObject;
    }
}
```

## 销毁

这里只关心单例Bean的销毁。Spring容器关闭时，会去销毁单例Bean。如果不去手动关闭容器，那么以上destroyMethod都不能执行。

总结销毁逻辑：

1. 销毁前：如果有@PreDestory 注解的方法就执行
2. 如果有自定义的销毁后置处理器，通过 postProcessBeforeDestruction 方法调用destoryBean逐一销毁Bean
3. 销毁时：如果实现了destroyMethod就执行 destory方法
4. 执行客户自定义销毁：调用 invokeCustomDestoryMethod执行在Bean上自定义的destroyMethod方法

手动执行close方法

org.springframework.context.support.AbstractApplicationContext#close

```java
public void close() {
    synchronized(this.startupShutdownMonitor) {
        this.doClose();
        if (this.shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
            } catch (IllegalStateException var4) {
            }
        }

    }
}

protected void doClose() {
    if (this.active.get() && this.closed.compareAndSet(false, true)) {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Closing " + this);
        }

        LiveBeansView.unregisterApplicationContext(this);

        try {
            this.publishEvent((ApplicationEvent)(new ContextClosedEvent(this)));
        } catch (Throwable var3) {
            this.logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", var3);
        }

        if (this.lifecycleProcessor != null) {
            try {
                this.lifecycleProcessor.onClose();
            } catch (Throwable var2) {
                this.logger.warn("Exception thrown from LifecycleProcessor on context close", var2);
            }
        }

        this.destroyBeans();
        this.closeBeanFactory();
        this.onClose();
        if (this.earlyApplicationListeners != null) {
            this.applicationListeners.clear();
            this.applicationListeners.addAll(this.earlyApplicationListeners);
        }

        this.active.set(false);
    }

}

protected void destroyBeans() {
    this.getBeanFactory().destroySingletons();
}
```



org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#destroySingletons

```java
public void destroySingletons() {
    if (this.logger.isTraceEnabled()) {
        this.logger.trace("Destroying singletons in " + this);
    }

    synchronized(this.singletonObjects) {
        this.singletonsCurrentlyInDestruction = true;
    }

    String[] disposableBeanNames;
    synchronized(this.disposableBeans) {//注册销毁的容器
        disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
    }
    
    //对注册销毁容器进行遍历
    for(int i = disposableBeanNames.length - 1; i >= 0; --i) {
        this.destroySingleton(disposableBeanNames[i]);
    }

    this.containedBeanMap.clear();
    this.dependentBeanMap.clear();
    this.dependenciesForBeanMap.clear();
    this.clearSingletonCache();
}

public void destroySingleton(String beanName) {
    this.removeSingleton(beanName);
    DisposableBean disposableBean;
    synchronized(this.disposableBeans) {
        disposableBean = (DisposableBean)this.disposableBeans.remove(beanName);
    }

    this.destroyBean(beanName, disposableBean);
}


protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
    Set dependencies;
    //先处理依赖的bean
    synchronized(this.dependentBeanMap) {
        dependencies = (Set)this.dependentBeanMap.remove(beanName);
    }

    if (dependencies != null) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
        }

        Iterator var4 = dependencies.iterator();

        while(var4.hasNext()) {
            String dependentBeanName = (String)var4.next();
            this.destroySingleton(dependentBeanName);
        }
    }

    if (bean != null) {
        try {
            //bean的销毁，这里就执行@PreDestroy注解的方法  和 自定义的销毁后置处理器
            bean.destroy();
        } catch (Throwable var13) {
            if (this.logger.isWarnEnabled()) {
                this.logger.warn("Destruction of bean with name '" + beanName + "' threw an exception", var13);
            }
        }
    }

    Set containedBeans;
    synchronized(this.containedBeanMap) {
        containedBeans = (Set)this.containedBeanMap.remove(beanName);
    }

    if (containedBeans != null) {
        Iterator var15 = containedBeans.iterator();

        while(var15.hasNext()) {
            String containedBeanName = (String)var15.next();
            this.destroySingleton(containedBeanName);
        }
    }

    synchronized(this.dependentBeanMap) {
        Iterator it = this.dependentBeanMap.entrySet().iterator();

        while(it.hasNext()) {
            Entry<String, Set<String>> entry = (Entry)it.next();
            Set<String> dependenciesToClean = (Set)entry.getValue();
            dependenciesToClean.remove(beanName);
            if (dependenciesToClean.isEmpty()) {
                it.remove();
            }
        }
    }

    this.dependenciesForBeanMap.remove(beanName);
}
```



<!-- @include: @article-footer.snippet.md -->     

