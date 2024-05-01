---
title: Java 8 新特性
category: Java
tag:
 - 版本新特性
---





## 一、Lambda

### 1、为什么使用 用Lambda 表达式

Lambda 是一个 匿名函数，我们可以把Lambda表达式理解为是 一段可以传递的代码（将代码像数据一样进行传递）。可以写出更简洁、更灵活的代码。作为一种更紧凑的代码风格，使Java的语言表达能力得到了提升

什么时候可以写Lambda表达式：是个接口，并且接口里需要重写的方法只有一个



### 2、Lambda 表达式

从匿名类到 Lambda 的转换

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947913.gif)

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947923.gif)



### 3、Lambda 表达式语法

Lambda 表达式在Java 语言中引入了一个新的语法元素和操作符。这个操作符为 “- >” ， 该操作符被称为 Lambda 操作符或箭头操作符。它将 Lambda 分为两个部分：

左侧 ：指定了 Lambda 表达式需要的所有参数

右侧 ：指定了 Lambda 体，即 Lambda 表达式要执行的功能。



#### 3.1 语法格式一：无参，无返回值，Lambda体只需一条语句

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947927.gif)



#### 3.2 语法格式二：Lambda需要一个参数

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947926.gif)



#### 3.3 语法格式三：Lambda只需要一个参数时，参数的小括号可以省略

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947922.gif)



#### 3.4 语法格式四：Lambda需要两个参数，并且有返回值

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947932.gif)



#### 3.5 语法格式五：当 Lambda体只有一条语句时，return与大括号可以省略

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947636.gif)



#### 3.6 数据类型可以省略，因为可由编译器推断得出，称为“类型推断”

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947661.gif)



### 4、 类型推断

上述 Lambda 表达式中的参数类型都是由编译器推断得出的。Lambda 表达式中无需指定类型，程序依然可以编译，这是因为 javac 根据程序的上下文，在后台推断出了参数的类型。Lambda 表达式的类型依赖于上下文环境，是由编译器推断出来的。这就是所谓的 “类型推断”

 

## 二、函数式接口

### 1、什么是函数式接口

- 只包含一个抽象方法的接口，称为函数式接口。

- 你可以通过 Lambda 表达式来创建该接口的对象。（若 Lambda表达式抛出一个受检异常，那么该异常需要在目标接口的抽象方法上进行声明）。

- 我们可以在任意函数式接口上使用@FunctionalInterface 注解，这样做可以检查它是否是一个函数式接口，同时 javadoc 也会包含一条声明，说明这个接口是一个函数式接口。



### 2、自定义函数式接口

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947685.gif)



### 3、作为参数传递 递Lambda 表达式

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947709.gif)

作为参数传递Lambda 表达式 ： 为了将Lambda 表达式作为参数传递，接收a Lambda 表达式的参数类型必须是与该Lambda 表达式兼容的函数式接口的类型 。



### 4、Java 内置四大核心函数式接口

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947736.gif)



### 5、其他接口

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947768.gif)



## 三、方法引用与构造器引用

### 1、方法引用

当要传递给Lambda体的操作，已经有实现的方法了，可以使用方法引用！（实现抽象方法的参数列表，必须与方法引用方法的参数列表保持一致！）方法引用：使用操作符 “ ::” 将方法名和对象或类的名字分隔开来。

如下三种主要使用情况 ：

- 类 :: 静态方法

- 类 :: 实例方法 （当需要引用方法的第一个参数是调用对象 ， 并且第二个参数是需要引用方法的第二 个 参数( ( 或无参数) ) 时可以使用）

- 对象 :: 实例方法

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947251.gif)

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947292.gif)

 

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947325.gif)

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947357.gif)

 

### 2、构造器引用

格式 ： ClassName::new

与函数式接口相结合，自动与函数式接口中方法兼容。可以把构造器引用赋值给定义的方法，与构造器参数列表要与接口中抽象方法的参数列表一致！

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947487.gif)



### 3、数组引用

格式 ： type[] :: new

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947564.gif)



## 四、强大的stream

### 1、了解Stream

Java8中有两大最为重要的改变。第一个是 Lambda 表达式；另外一个则是Stream API(java.util.stream.*) 。Stream 是 Java8 中处理集合的关键抽象概念，它可以指定你希望对集合进行的操作，可以执行非常复杂的查找、过滤和映射数据等操作。使用Stream API 对集合数据进行操作，就类似于使用 SQL 执行的数据库查询。也可以使用 Stream API 来并行执行操作。简而言之，Stream API 提供了一种高效且易于使用的处理数据的方式。



### 2、什么是Stream

流) (Stream) 到底是什么呢？是数据渠道，用于操作数据源（集合、数组等）所生成的元素序列。“ 集合讲的是数据 ， 流讲的是 计 算 ！ ”

注意 ：

①Stream 自己不会存储元素。

②Stream 不会改变源对象。相反，他们会返回一个持有结果的新Stream。

③Stream 操作是延迟执行的。这意味着他们会等到需要结果的时候才执行。



### 3、Stream 的操作三个步骤

- 创建 Stream

一个数据源（如：集合、数组），获取一个流

- 中间操作

一个中间操作链，对数据源的数据进行处理

- 终止操作( ( 终端操作) )

一个终止操作，执行中间操作链，并产生结果

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947750.gif)



### 4、创建Stream

#### 4.1 由集合接口创建

Java8 中的 Collection 接口被扩展，提供了两个获取流的方法 ：

- default Stream&lt;E> stream() : 返回一个顺序流

- default Stream&lt;E> parallelStream() : 返回一个并行流



#### 4.2、由数组创建流

Java8 中的 Arrays 的静态方法 stream() 可以获取数组流：

- static &lt;T> Stream&lt;T> stream(T[] array): 返回一个流

重载形式 ， 能够处理对应基本类型的数组 ：

- public static IntStream stream(int[] array)

- public static LongStream stream(long[] array)

- public static DoubleStream stream(double[] array)



#### 4.3、由值创建流

可以使用静态方法 Stream.of(), 通过显示值

创建一个流。它可以接收任意数量的参数。

- public static&lt;T> Stream&lt;T> of(T... values) : 返回一个流



#### 4.4、由函数创建流 ： 创建无限流

可以使用静态方法 Stream.iterate() 和Stream.generate(), 创建无限流。

- 迭代：public static&lt;T> Stream&lt;T> iterate(final T seed, final UnaryOperator&lt;T> f)

- 生成：public static&lt;T> Stream&lt;T> generate(Supplier&lt;T> s) :



### 5、Stream 的中间操作

多个 中间操作可以连接起来形成一个 流 水 线，除非流水线上触发终止操作，否则 中 间操作 不 会执行 任 何 的 处 理！而在终止操作时一次性全部处理 ， 称为 “ 惰 性 求 值“



#### 5.1 筛选与切片

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947852.gif)



#### 5.2 映射

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947037.gif)



#### 5.3 排序

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947084.gif)



### 6、Stream 的终止操作

终端操作会从流的流水线生成结果。其结果可以是任何不是流的值，例如：List、Integer，甚至是 void 。

#### 6.1 查找与匹配

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947139.gif)

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947176.gif)



#### 6.2 归约

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947406.gif)

备注：map 和 reduce 的连接通常称为map-reduce 模式，因 Google 用它来进行网络搜索而出名。



#### 6.3 收集

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947678.gif)

Collector 接口中方法的实现决定了如何对流执行收集操作(如收集到 List、Set、Map)。但是 Collectors 实用类提供了很多静态方法，可以方便地创建常见收集器实例，具体方法与实例如下表：

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947820.gif)

 

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947872.gif)



### 7、并行流 与 串行 流

并行流 就是把一个内容分成多个数据块，并用不同的线程分别处理每个数据块的流。

Java 8 中将并行进行了优化，我们可以很容易的对数据进行并行操作。Stream API 可以声明性地通过 parallel() 与sequential() 在并行流与顺序流之间进行切换。



### 8、 了解Fork/Join 框架

Fork/Join 框架 ：就是在必要的情况下，将一个大任务，进行拆分(fork)成若干个小任务（拆到不可再拆时），再将一个个的小任务运算的结果进行 join 汇总.

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947005.gif)

 

Fork/Join 框架与传统线程池的区别

采用 “工作窃取”模式（work-stealing）：当执行新的任务时它可以将其拆分分成更小的任务执行，并将小任务加到线程队列中，然后再从一个随机线程的队列中偷一个并把它放在自己的队列中。相对于一般的线程池实现,fork/join框架的优势体现在对其中包含的任务的处理方式上.在一般的线程池中,如果一个线程正在执行的任务由于某些原因无法继续运行,那么该线程会处于等待状态.而在fork/join框架实现中,如果某个子问题由于等待另外一个子问题的完成而无法继续运行.那么处理该子问题的线程会主动寻找其他尚未运行的子问题来执行.这种方式减少了线程的等待时间,提高了性能



## 五、新时间日期API

### 1、使用LocalDate 、LocalTime 、LocalDateTime

LocalDate、LocalTime、LocalDateTime 类的实例是不可变的对象，分别表示使用 ISO-8601日历系统的日期、时间、日期和时间。它们提供了简单的日期或时间，并不包含当前的时间信息。也不包含与时区相关的信息。

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947060.gif)



### 2、Instant 时间戳

用于“时间戳”的运算。它是以Unix元年(传统的设定为UTC时区1970年1月1日午夜时分)开始所经历的描述进行运算



### 3、Duration 和 Period

Duration:用于计算两个“时间”间隔

Period:用于计算两个“日期”间隔



### 4、日期的操纵

TemporalAdjuster : 时间校正器。有时我们可能需要获取例如：将日期调整到“下个周日”等操作。

TemporalAdjusters: 该类通过静态方法提供了大量的常用 TemporalAdjuster 的实现。

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947150.gif)



### 5、解析与格式化

java.time.format.DateTimeFormatter类：该类提供了三种格式化方法：

- 预定义的标准格式

- 语言环境相关的格式

- 自定义的格式



### 6、时区 的处理

Java8 中加入了对时区的支持，带时区的时间为分别为：

ZonedDate、ZonedTime、ZonedDateTime

其中每个时区都对应着ID，地区ID都为“{区域}/{城市}”的格式

例如 ：Asia/Shanghai 等

ZoneId：该类中包含了所有的时区信息

getAvailableZoneIds() : 可以获取所有时区时区信息

of(id) : 用指定的时区信息获取ZoneId对象



### 7、与传统日期处理的转换

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947266.gif)



## 六、接口中的默认方法与静态方法

### 1、接口中的默认方法

Java 8中允许接口中包含具有具体实现的方法，该方法称为“默认方法”，默认方法使用 default 关键字修饰。

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947667.gif)

为什么要有默认方法：

1. 向后兼容性：在Java 8之前，如果你向一个接口添加方法，那么所有实现了该接口的类都必须修改，以实现新增的方法。这在大型项目中是不现实的，因为这会破坏现有的实现。通过使用default方法，你可以向接口中添加新方法，而无需改变实现该接口的类。
2. 代码复用：default方法允许在接口内部提供方法的实现，从而使得代码复用变得可能。这意味着如果多个实现类可以共享某个方法的实现逻辑，那么这个逻辑就可以作为一个default方法放在接口中，避免了在每个实现类中重复此逻辑。
3. 多继承的灵活性：尽管Java不允许类多重继承（以避免多重继承的复杂性和歧义），但通过default方法，Java可以在接口层面上提供类似多重继承的能力。一个类可以实现多个接口，并且每个接口可以通过default方法提供具体实现，这给Java增加了额外的灵活性和功能。
4. 弥补抽象类的不足：在Java 8之前，如果你想为一组类提供公共实现，通常的方法是使用抽象类。然而，由于Java不支持从多个类继承，这种方法的使用非常受限。default方法提供了一种机制，通过它接口可以提供实现，而不需要抽象类，同时也不受单继承的限制。



### 2、接口中的静态方法

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947737.gif)



## 七、其他新特性

### 1、Optional 类

Optional&lt;T> 类(java.util.Optional) 是一个容器类，代表一个值存在或不存在，原来用 null 表示一个值不存在，现在 Optional 可以更好的表达这个概念。并且可以避免空指针异常。

常用方法 ：

Optional.of(T t) : 创建一个 Optional 实例

Optional.empty() : 创建一个空的 Optional 实例

Optional.ofNullable(T t):若 t 不为 null,创建 Optional 实例,否则创建空实例

isPresent() : 判断是否包含值

orElse(T t) : 如果调用对象包含值，返回该值，否则返回t

orElseGet(Supplier s) :如果调用对象包含值，返回该值，否则返回 s 获取的值

map(Function f): 如果有值对其处理，并返回处理后的Optional，否则返回 Optional.empty()

flatMap(Function mapper):与 map 类似，要求返回值必须是Optional



### 2、重复注解与类型注解

Java 8对注解处理提供了两点改进：可重复的注解及可用于类型的注解。

![截图.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947915.gif)

 





