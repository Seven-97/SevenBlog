---
title: Java 8 新特性
category: Java
tag:
 - 版本新特性
head:
  - - meta
    - name: keywords
      content: Java,版本新特性,Java8
  - - meta
    - name: description
      content: 全网最全的Java 版本新特性知识点总结，让天下没有难学的八股文！
---





## Lambda

### 为什么使用Lambda 表达式

Lambda 是一个 匿名函数，我们可以把Lambda表达式理解为是 一段可以传递的代码（将代码像数据一样进行传递）。可以写出更简洁、更灵活的代码。作为一种更紧凑的代码风格，使Java的语言表达能力得到了提升

什么时候可以写Lambda表达式：是个接口，并且接口里需要重写的方法只有一个



### Lambda 表达式

从匿名类到 Lambda 的转换

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947913.gif)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947923.gif)



### Lambda 表达式语法

Lambda 表达式在Java 语言中引入了一个新的语法元素和操作符。这个操作符为 “- >” ， 该操作符被称为 Lambda 操作符或箭头操作符。它将 Lambda 分为两个部分：

左侧 ：指定了 Lambda 表达式需要的所有参数

右侧 ：指定了 Lambda 体，即 Lambda 表达式要执行的功能。



#### 语法一：无参，无返回值，Lambda体只需一条语句

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947927.gif)



#### 语法二：Lambda需要一个参数

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947926.gif)



#### 语法三：Lambda只需要一个参数时，参数的小括号可以省略

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947922.gif)



#### 语法四：Lambda需要两个参数，并且有返回值

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947932.gif)



#### 语法五：当 Lambda体只有一条语句时，return与大括号可以省略

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947636.gif)



#### 数据类型可以省略，因为可由编译器推断得出，称为“类型推断”

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947661.gif)



###  类型推断

上述 Lambda 表达式中的参数类型都是由编译器推断得出的。Lambda 表达式中无需指定类型，程序依然可以编译，这是因为 javac 根据程序的上下文，在后台推断出了参数的类型。Lambda 表达式的类型依赖于上下文环境，是由编译器推断出来的。这就是所谓的 “类型推断”

 

## 函数式接口

### 什么是函数式接口

- 只包含一个抽象方法的接口，称为函数式接口。

- 你可以通过 Lambda 表达式来创建该接口的对象。（若 Lambda表达式抛出一个受检异常，那么该异常需要在目标接口的抽象方法上进行声明）。

- 我们可以在任意函数式接口上使用@FunctionalInterface 注解，这样做可以检查它是否是一个函数式接口，同时 javadoc 也会包含一条声明，说明这个接口是一个函数式接口。



### 自定义函数式接口

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947685.gif)



### 作为参数传递 递Lambda 表达式

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947709.gif)

作为参数传递Lambda 表达式 ： 为了将Lambda 表达式作为参数传递，接收a Lambda 表达式的参数类型必须是与该Lambda 表达式兼容的函数式接口的类型 。



### Java 内置四大核心函数式接口

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947736.gif)



### 其他接口

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947768.gif)



## 方法引用与构造器引用

### 方法引用

当要传递给Lambda体的操作，已经有实现的方法了，可以使用方法引用！（实现抽象方法的参数列表，必须与方法引用方法的参数列表保持一致！）方法引用：使用操作符 “ ::” 将方法名和对象或类的名字分隔开来。

如下三种主要使用情况 ：

- 类 :: 静态方法

- 类 :: 实例方法 （当需要引用方法的第一个参数是调用对象 ， 并且第二个参数是需要引用方法的第二 个 参数( ( 或无参数) ) 时可以使用）

- 对象 :: 实例方法

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947251.gif)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947292.gif)

 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947325.gif)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947357.gif)

 

### 构造器引用

格式 ： ClassName::new

与函数式接口相结合，自动与函数式接口中方法兼容。可以把构造器引用赋值给定义的方法，与构造器参数列表要与接口中抽象方法的参数列表一致！

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947487.gif)



### 数组引用

格式 ： type[] :: new

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947564.gif)



## 强大的stream

### 了解Stream

Java8中有两大最为重要的改变。第一个是 Lambda 表达式；另外一个则是Stream API(java.util.stream.*) 。Stream 是 Java8 中处理集合的关键抽象概念，它可以指定你希望对集合进行的操作，可以执行非常复杂的查找、过滤和映射数据等操作。使用Stream API 对集合数据进行操作，就类似于使用 SQL 执行的数据库查询。也可以使用 Stream API 来并行执行操作。简而言之，Stream API 提供了一种高效且易于使用的处理数据的方式。

但需要**注意**的是，Stream的中间操作（比如map、filter等）是惰性的，这意味着它们在终端操作（比如collect、 forEach等）被调用之前不会实际执行。

### 什么是Stream

流) (Stream) 到底是什么呢？是数据渠道，用于操作数据源（集合、数组等）所生成的元素序列。“ 集合讲的是数据 ， 流讲的是 计 算 ！ ”

注意 ：

①Stream 自己不会存储元素。

②Stream 不会改变源对象。相反，他们会返回一个持有结果的新Stream。

③Stream 操作是延迟执行的。这意味着他们会等到需要结果的时候才执行。

详情可以看这篇文章 [Stream流原理](https://www.seven97.top/java/basis/stream.html)

 

## 新时间日期API

### 使用LocalDate 、LocalTime 、LocalDateTime

LocalDate、LocalTime、LocalDateTime 类的实例是不可变的对象，分别表示使用 ISO-8601日历系统的日期、时间、日期和时间。它们提供了简单的日期或时间，并不包含当前的时间信息。也不包含与时区相关的信息。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947060.gif)



### Instant 时间戳

用于“时间戳”的运算。它是以Unix元年(传统的设定为UTC时区1970年1月1日午夜时分)开始所经历的描述进行运算



### Duration 和 Period

Duration:用于计算两个“时间”间隔

Period:用于计算两个“日期”间隔



### 日期的操纵

TemporalAdjuster : 时间校正器。有时我们可能需要获取例如：将日期调整到“下个周日”等操作。

TemporalAdjusters: 该类通过静态方法提供了大量的常用 TemporalAdjuster 的实现。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947150.gif)



### 解析与格式化

java.time.format.DateTimeFormatter类：该类提供了三种格式化方法：

- 预定义的标准格式

- 语言环境相关的格式

- 自定义的格式



### 时区 的处理

Java8 中加入了对时区的支持，带时区的时间为分别为：

ZonedDate、ZonedTime、ZonedDateTime

其中每个时区都对应着ID，地区ID都为“{区域}/{城市}”的格式

例如 ：Asia/Shanghai 等

ZoneId：该类中包含了所有的时区信息

getAvailableZoneIds() : 可以获取所有时区时区信息

of(id) : 用指定的时区信息获取ZoneId对象



### 与传统日期处理的转换

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947266.gif)



## 接口中的默认方法与静态方法

### 接口中的默认方法

Java 8中允许接口中包含具有具体实现的方法，该方法称为“默认方法”，默认方法使用 default 关键字修饰。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947667.gif)

为什么要有默认方法：

1. 向后兼容性：在Java 8之前，如果你向一个接口添加方法，那么所有实现了该接口的类都必须修改，以实现新增的方法。这在大型项目中是不现实的，因为这会破坏现有的实现。通过使用default方法，你可以向接口中添加新方法，而无需改变实现该接口的类。
2. 代码复用：default方法允许在接口内部提供方法的实现，从而使得代码复用变得可能。这意味着如果多个实现类可以共享某个方法的实现逻辑，那么这个逻辑就可以作为一个default方法放在接口中，避免了在每个实现类中重复此逻辑。
3. 多继承的灵活性：尽管Java不允许类多重继承（以避免多重继承的复杂性和歧义），但通过default方法，Java可以在接口层面上提供类似多重继承的能力。一个类可以实现多个接口，并且每个接口可以通过default方法提供具体实现，这给Java增加了额外的灵活性和功能。
4. 弥补抽象类的不足：在Java 8之前，如果你想为一组类提供公共实现，通常的方法是使用抽象类。然而，由于Java不支持从多个类继承，这种方法的使用非常受限。default方法提供了一种机制，通过它接口可以提供实现，而不需要抽象类，同时也不受单继承的限制。



### 接口中的静态方法

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947737.gif)



## 其他新特性

### Optional 类

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



### 重复注解与类型注解

Java 8对注解处理提供了两点改进：可重复的注解及可用于类型的注解。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947915.gif)

 

<!-- @include: @article-footer.snippet.md -->     



