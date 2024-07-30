---
title: Stream流原理
category: Java
tag:
  - Java基础
---



Java 8的新特性之一就是流stream，配合同版本出现的 `Lambda` ，使得操作集合（Collection）提供了极大的便利。关于流的具体使用可以看[这篇文章](https://www.seven97.top/java/new-features/java8.html#强大的stream)



## 流的三大特点

流有三大特点：

1. 流并**不存储元素**。这些元素可能**存储在底层的集合中**，或者是按需生成。
2. 流的操作不会修改其数据元素，而是生成一个新的流。
3. 流的操作是尽可能**惰性执行**的。这意味着直至需要其结果时，操作才会执行。



## 流的运行流程

下面是一段比较简单常见的stream操作代码，经过映射与过滤操作后，最后得到的endList=["vb"]，下文讲解都会以此代码为例。

```java
List<String> startlist = Lists.newArrayList("s", "e", "v", "e", "n");
List<String> endList = startlist.stream().map(r -> r + "b").filter(r -> r.startsWith("v")).collect(Collectors.toList());
```



一段Stream代码的运行包括以下三部分：

1. 搭建流水线，定义各阶段功能。
2. 从终结点反向索引，生成操作实例Sink。
3. 数据源送入流水线，经过各阶段处理后，生成结果。



### 类图介绍

![Stream类图](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407302204530.webp)

Stream是一个接口，它**定义了对Stream的操作**，主要可分为中间操作与终结操作，中间操作对流进行转化，比如对数据元素进行映射/过滤/排序等行为。终结操作启动流水线，获取结果数据。

AbstractPipline是一个抽象类，定**义了流水线节点的常用属性**，sourceStage指向流水线首节点，previousStage 指向本节点上层节点，nextStage 指向本节点下层节点，depth代表本节点处于流水线第几层（从0开始计数），sourceSpliterator 指向数据源。

ReferencePipline实现Stream接口，继承AbstractPipline类，它主要对Stream中的各个操作进行实现，此外，它还定义了三个内部类Head/StatelessOp/StatefulOp。Head为流水线首节点，在集合转为流后，生成Head节点。StatelessOp为无状态操作，StatefulOp为有状态操作。无状态操作只对当前元素进行作用，比如filter操作只需判断“a”元素符不符合“startWith("a")”这个要求，无需在对“a”进行判断时关注数据源其他元素（“b”，“c”）的状态。有状态操作需要关注数据源中其他元素的状态，比如sorted操作要保留数据源其他元素，然后进行排序，生成新流。

![Stream操作分类](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407302204950.webp)

### 搭建流水线

首先需要区分一个概念，Stream(流)并不是一个容器，不存储数据，它更像是一个个具有不同功能的流水线节点，可相互串联，容许数据源挨个通过，最后随着终结操作生成结果。Stream流水线搭建包括三个阶段：

1. 创建一个流，如通过stream()产生Head，Head就是初始流，数据存储在Spliterator。
2. 将初始流转换成其他流的中间操作，可能包含多个步骤，比如上面map与filter操作。
3. 终止操作，用于产生结果，终结操作后，流也就走到了终点。



#### 定义输入源HEAD

只有实现了Collection接口的类才能创建流，所以Map并不能创建流，List与Set这种单列集合才可创建流。上述代码使用stream()方法创建流，也可使用Stream.of()创建任何数量引元的流，或是 Array.stream(array,from,to) 从数组中from到to的位置创建输入源。



#### stream()运行结果

示例代码中使用stream()方法生成流，让我们看看生成的流中有哪些内容：

```java
Stream<String> headStream = startlist.stream();
```

 ![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407302230750.png)

从运行结果来看，stream(）方法生成了ReferencPipeline$Head类，ReferencPipeline是Stream的实现类，Head是ReferencePipline的内部类。其中:

- sourceStage指向实例本身
- depth=0代表Head是流水线首层
- sourceSpliterator 指向底层存储数据的集合，其中list即初始数据源。



#### stream()源码分析

```java
// java.util.Collection#stream
default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
}

// java.util.Collection#spliterator
@Override
default Spliterator<E> spliterator() {
    return Spliterators.spliterator(this, 0);
}
```

spliterator()将 “调用stream()方法的对象本身startlist” 传入构造函数，生成Spliterator类，传入StreamSupport.stream()方法。

```java
// java.util.stream.StreamSupport#stream(java.util.Spliterator<T>, boolean)
public static <T> Stream<T> stream(Spliterator<T> spliterator, boolean parallel) {
    Objects.requireNonNull(spliterator);
    return new ReferencePipeline.Head<>(spliterator,
                                        StreamOpFlag.fromCharacteristics(spliterator),
                                        parallel);
}
```

StreamSupport.stream()返回了ReferencPipeline$Head类。



点击构造函数，一路追溯至 AbstractPipline 中，可看到使用sourceSpliterator指向数据源，sourceStage为Head实例本身，深度depth=0。

```java
// java.util.stream.AbstractPipeline#AbstractPipeline(java.util.Spliterator<?>, int, boolean)
AbstractPipeline(Spliterator<?> source, int sourceFlags, boolean parallel) {
    this.previousStage = null;
    this.sourceSpliterator = source;//指向传入的spliterator，也就是调用stream()方法的list，即数据源
    this.sourceStage = this; //Head实例本身
    this.sourceOrOpFlags = sourceFlags & StreamOpFlag.STREAM_MASK;
    // The following is an optimization of:
    // StreamOpFlag.combineOpFlags(sourceOrOpFlags, StreamOpFlag.INITIAL_OPS_VALUE);
    this.combinedFlags = (~(sourceOrOpFlags << 1)) & StreamOpFlag.INITIAL_OPS_VALUE;
    this.depth = 0;//深度为0
    this.parallel = parallel;
}
```



### 定义流水线中间节点

#### Map

##### map()运行结果

对数据进行映射，对每个元素后接"b"。

```java
Stream<String> mapStream =startlist.stream().map(r->r+"b");
```

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407302240793.png)

此时：（由于是多次dubug，因此对象的地址值与上面不一致，不影响与案例分析，下同）

- sourceStage与previousStage 皆指向Head节点
- depth变为1，表示为流水线第二节点
- 由于代码后续没接其他操作，所以nextStage为null
- mapper代表函数式接口，指向lambda代码块，即 “r->r+"b"” 这个操作。



##### map()源码分析

```java
//java.util.stream.ReferencePipeline#map
@Override
@SuppressWarnings("unchecked")
public final <R> Stream<R> map(Function<? super P_OUT, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return new StatelessOp<P_OUT, R>(this, StreamShape.REFERENCE,
                               StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
        @Override
        Sink<P_OUT> opWrapSink(int flags, Sink<R> sink) {
            return new Sink.ChainedReference<P_OUT, R>(sink) {
                @Override
                public void accept(P_OUT u) {
                    downstream.accept(mapper.apply(u));
                }
            };
        }
    };
}
```

可以看到，map()方法是在ReferencePipline中被实现的，返回了一个无状态操作StatelessOp，定义opWrapSink方法，运行时会将lambda代码块的内容替换apply方法，对数据元素u进行操作。opWrapSink方法将返回Sink对象，其用处将在下文讲解。downstream为opWrapSink的入参sink。



#### Filter

##### filter()运行结果

filter对元素进行过滤，只留存以“v”开头的数据元素。

```java
 Stream<String> filterStream = startlist.stream().map(r -> r + "b").filter(r -> r.startsWith("v"));
```

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407302248619.png)

Filter阶段：

- depth再次+1
- sourceStage指向Head
- predict指向lamda表达式的代码块：“r->r.startsWith("a")”
- previousStage指向前序Map节点
- Map节点中的nextStage 开始指向Filter，形成了双向链表。



##### filter()源码分析

```java
// java.util.stream.ReferencePipeline#filter
@Override
public final Stream<P_OUT> filter(Predicate<? super P_OUT> predicate) {
    Objects.requireNonNull(predicate);
    return new StatelessOp<P_OUT, P_OUT>(this, StreamShape.REFERENCE,
                                 StreamOpFlag.NOT_SIZED) {
        @Override
        Sink<P_OUT> opWrapSink(int flags, Sink<P_OUT> sink) {
            return new Sink.ChainedReference<P_OUT, P_OUT>(sink) {
                @Override
                public void begin(long size) {
                    downstream.begin(-1);
                }

                @Override
                public void accept(P_OUT u) {
                    if (predicate.test(u)) // "r->r.startsWith("v")"
                        downstream.accept(u);
                }
            };
        }
    };
}
```

filter()也是在ReferencePipline中被实现，返回一个无状态操作StatelessOp，实现opWrapSink方法，也是返回一个Sink，其中accept方法中的predicate.test="r->r.startsWith("v")"，用以过滤符合要求的元素。downstream等于opWrapSink入参Sink。

StatelessOp的基类AbstractPipline中有个构造方法帮助构造了双向链表。

```java
// java.util.stream.AbstractPipeline#AbstractPipeline(java.util.stream.AbstractPipeline<?,E_IN,?>, int)
AbstractPipeline(AbstractPipeline <? , E_IN, ?> previousStage, int opFlags) {
    if (previousStage.linkedOrConsumed)
        throw new IllegalStateException(MSG_STREAM_LINKED);
    previousStage.linkedOrConsumed = true;
    previousStage.nextStage = this;

    this.previousStage = previousStage;
    this.sourceOrOpFlags = opFlags & StreamOpFlag.OP_MASK;
    this.combinedFlags = StreamOpFlag.combineOpFlags(opFlags, previousStage.combinedFlags);
    this.sourceStage = previousStage.sourceStage;
    if (opIsStateful())
        sourceStage.sourceAnyStateful = true;
    this.depth = previousStage.depth + 1;
}
```



### 定义终结操作

#### collect()运行结果

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407302257007.png)

经过终结操作后，生成最终结果[“vb”]。

#### collect()源码分析

```java
// java.util.stream.ReferencePipeline#collect(java.util.stream.Collector<? super P_OUT,A,R>)
@Override
@SuppressWarnings("unchecked")
public final < R, A > R collect(Collector <? super P_OUT, A, R > collector) {
    A container;
    if (isParallel() //是并行操作
        	&& (collector.characteristics().contains(Collector.Characteristics.CONCURRENT)) 
        	&& (!isOrdered() || collector.characteristics().contains(Collector.Characteristics.UNORDERED))) {
        container = collector.supplier().get();
        BiConsumer < A, ? super P_OUT > accumulator = collector.accumulator();
        forEach(u - > accumulator.accept(container, u));
    } 
    else { // 不是并行操作
        container = evaluate(ReduceOps.makeRef(collector));
    }
    return collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH) 
        ? (R) container 
        : collector.finisher().apply(container);
}
```

同样的，collect终结操作也在ReferencePipline中被实现。由于不是并行操作，只要关注evaluate()方法即可。

```java
public static < T, I > TerminalOp < T, I > makeRef(Collector <? super T, I, ?> collector) {
    Supplier < I > supplier = Objects.requireNonNull(collector).supplier();
    BiConsumer < I, ? super T > accumulator = collector.accumulator();
    BinaryOperator < I > combiner = collector.combiner();
    class ReducingSink extends Box < I > implements AccumulatingSink < T, I, ReducingSink > {
        @Override
        public void begin(long size) {
            state = supplier.get();
        }

        @Override
        public void accept(T t) {
            accumulator.accept(state, t);
        }

        @Override
        public void combine(ReducingSink other) {
            state = combiner.apply(state, other.state);
        }
    }
    return new ReduceOp < T, I, ReducingSink > (StreamShape.REFERENCE) {
        @Override
        public ReducingSink makeSink() {
            return new ReducingSink();//new一个ReducingSInk对象
        }
 
        @Override
        public int getOpFlags() {
            return collector.characteristics().contains(Collector.Characteristics.UNORDERED) ? StreamOpFlag.NOT_ORDERED : 0;
        }
    };
}
```

makeRef()方法中也有个类似opWrapSink一样返回Sink的方法，不过没有以其他Sink为输入，而是直接new一个ReducingSInk对象。

至此，可以根据源码绘出下图，使用双向链表连接各个流水线节点，并将每个阶段的lambda代码块存入Sink类中。数据源使用sourceSpliterator引用。

![流水线搭建](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407302303235.webp)

灰色表示还未生成Sink实例。



### 反向回溯生成操作实例

还记得前面说的“惰性执行”么，在一层一层搭建中间节点时，并未有任何结果产生，而在终结操作collect之后，生成最终结果endList，让我们探究一下collect()方法中的evaluate方法。

```java
// java.util.stream.AbstractPipeline#evaluate(java.util.stream.TerminalOp<E_OUT,R>)
final < R > R evaluate(TerminalOp < E_OUT, R > terminalOp) {
    assert getOutputShape() == terminalOp.inputShape();
    if (linkedOrConsumed)
        throw new IllegalStateException(MSG_STREAM_LINKED);
    linkedOrConsumed = true;

    return isParallel() 
        ? terminalOp.evaluateParallel(this, sourceSpliterator(terminalOp.getOpFlags())) 
        : terminalOp.evaluateSequential(this, sourceSpliterator(terminalOp.getOpFlags()));
}
```

这里调用了Collect中定义的makeSink()方法，输入终结节点生成的sink与数据源spliterator。

```java
// java.util.stream.ReduceOps.ReduceOp#evaluateSequential
@Override
public < P_IN > R evaluateSequential(PipelineHelper < T > helper,
    Spliterator < P_IN > spliterator) {
    return helper.wrapAndCopyInto(makeSink(), spliterator).get();
}

// java.util.stream.AbstractPipeline#wrapAndCopyInto
@Override
final < P_IN, S extends Sink < E_OUT >> S wrapAndCopyInto(S sink, Spliterator < P_IN > spliterator) {
    copyInto(wrapSink(Objects.requireNonNull(sink)), spliterator);
    return sink;
}
```

先来看wrapSink方法，在这个方法里，中间节点的opWrapSink方法将发挥大作用，它利用previousStage反向索引，后一个节点的sink送入前序节点的opWrapSink方法中做入参，也就是downstream，生成当前sink，再索引向前，生成套娃Sink。

﻿![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407302309223.webp)﻿

最后索引到depth=1的Map节点，生成的结果Sink包含了depth2节点Filter与终结节点Collect的Sink。

﻿![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407302309227.webp)﻿

红色框图表示Map节点的Sink，包含当前Stream与downstream（Filter节点Sink），黄色代表Filter节点Sink，downstream指向Collect节点。

Sink被反向套娃实例化，一步步索引到Map节点，可以对图2进行完善。

﻿![反向索引生成Sink](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407302309214.webp)﻿

#### 启动流水线

一切准备就绪，就差把数据源冲入流水线。卷起来！在wrapSink方法套娃生成Sink之后，copyInto方法将数据源送入了流水线。

﻿![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407302310300.webp)﻿

﻿![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407302310288.webp)﻿

先是调用Sink中已定义好的begin方法，做些前序处理，Sink中的begin方法会不断调用下一个Sink的begin方法。

随后对数据源中各个元素进行遍历，调用Sink中定义好的accept方法处理数据元素。accept执行的就是咱在每一节点定义的lambda代码块。

﻿![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407302310286.webp)﻿

随后调用end方法做后序扫尾工作。

﻿![数据源冲入操作实例，生成最终结果](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407302310306.webp)﻿

一个简单Stream整体关联图如上所示，最后调用get()方法生成结果。

