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





## Lambda表达式

### 为什么使用Lambda 表达式

Lambda 是一个 匿名函数，我们可以把Lambda表达式理解为是 一段可以传递的代码（将代码像数据一样进行传递）。可以写出更简洁、更灵活的代码。作为一种更紧凑的代码风格，使Java的语言表达能力得到了提升

什么时候可以写Lambda表达式：是个接口，并且接口里需要重写的方法只有一个



### Lambda 表达式

举些例子，比如给按钮添加点击事件、或者创建一个新线程执行操作，必须要自己new 接口并且编写接口的定义和实现代码。

```java
// Java 8 之前的写法，给按钮添加点击事件  
button.addActionListener(new ActionListener() {  
    @Override  
    public void actionPerformed(ActionEvent e) {  
        System.out.println("按钮被点击了");  
    }  
});  
  
// 使用线程的传统写法  
Thread thread = new Thread(new Runnable() {  
    @Override  
    public void run() {  
        System.out.println("线程正在运行");  
    }  
});
```



Lambda 表达式的出现，让代码变得简洁优雅，告别匿名内部类!

```java
// Java 8 Lambda 写法  
button.addActionListener(e -> System.out.println("按钮被点击了"));  
Thread thread = new Thread(() -> System.out.println("线程正在运行"));
```



### Lambda 表达式语法

Lambda 表达式在Java 语言中引入了一个新的语法元素和操作符。这个操作符为 “- >” ， 该操作符被称为 Lambda 操作符或箭头操作符。它将 Lambda 分为两个部分：

左侧 ：指定了 Lambda 表达式需要的所有参数

右侧 ：指定了 Lambda 体，即 Lambda 表达式要执行的功能。

```java
// 无参数的 Lambda  
Runnable r = () -> System.out.println("Hello Lambda!");  
  
// 单个参数（可以省略括号）  
Consumer<String> printer = s -> System.out.println(s);  
  
// 多个参数  
BinaryOperator<Integer> add = (a, b) -> a + b;  
Comparator<String> comparator = (a, b) -> a.compareTo(b);  
  
// 复杂的方法体（需要大括号和 return）  
Function<String, String> processor = input -> {  
    String processed = input.trim().toLowerCase();  
    if (processed.isEmpty()) {  
        return "空字符串";  
    }  
    return "处理后的字符串：" + processed;  
};
```




###  类型推断

上述 Lambda 表达式中的参数类型都是由编译器推断得出的。Lambda 表达式中无需指定类型，程序依然可以编译，这是因为 javac 根据程序的上下文，在后台推断出了参数的类型。Lambda 表达式的类型依赖于上下文环境，是由编译器推断出来的。这就是所谓的 “类型推断”

 

## 函数式接口

### 什么是函数式接口

- 只包含一个抽象方法的接口，称为函数式接口。

- 你可以通过 Lambda 表达式来创建该接口的对象。（若 Lambda表达式抛出一个受检异常，那么该异常需要在目标接口的抽象方法上进行声明）。

- 我们可以在任意函数式接口上使用@FunctionalInterface 注解，这样做可以检查它是否是一个函数式接口，同时 javadoc 也会包含一条声明，说明这个接口是一个函数式接口。

### Java 内置四大核心函数式接口

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404251947736.gif)

```java
// Predicate<T> 用于条件判断  
Predicate<Integer> isEven = n -> n % 2 == 0;  
Predicate<String> isEmpty = String::isEmpty;  
Predicate<String> isNotEmpty = isEmpty.negate();  // 取反  
  
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);  
List<Integer> evenNumbers = numbers.stream()  
    .filter(isEven)  
    .collect(Collectors.toList());
```

```java
// Function<T, R> 用于转换  
Function<String, Integer> stringLength = String::length;  
Function<Integer, String> intToString = Object::toString;  
  
// 函数组合  
Function<String, String> addPrefix = s -> "前缀-" + s;  
Function<String, String> addSuffix = s -> s + "-后缀";  
Function<String, String> combined = addPrefix.andThen(addSuffix);  
String result = combined.apply("Seven"); // "前缀-Seven-后缀"
```

```java
// Consumer<T> 用于消费数据（无返回值）  
Consumer<String> printer = System.out::println;  
Consumer<String> logger = s -> log.info("处理数据：{}", s);  
// 组合消费  
Consumer<String> combinedConsumer = printer.andThen(logger);  
  
// Supplier<T> 用于提供数据  
Supplier<String> randomId = () -> UUID.randomUUID().toString();  
Supplier<LocalDateTime> now = LocalDateTime::now;
```

```java
// BinaryOperator<T> 用于二元操作  
BinaryOperator<Integer> max = Integer::max;  
BinaryOperator<String> concat = (a, b) -> a + b;
```

### 自定义函数式接口

虽然实际开发中，我们更多的是使用Java 内置的函数式接口，但大家还是要了解一下自定义函数式接口的写法，有个印象。

```java
// 创建自定义函数式接口  
@FunctionalInterface  
public interface Calculator {  
    double calculate(double a, double b);  
}

// 使用自定义函数式接口  
Calculator addition = (a, b) -> a + b;  
Calculator subtraction = (a, b) -> a - b;
```


### 注意


- 函数式接口必须是接口类型，不能是类、抽象类或枚举。
- 必须且只能包含一个抽象方法。否则 Lambda 表达式可能无法匹配接口。
- 建议使用 @FunctionalInterface 注解。虽然这个注解不是强制的，但加上后编译器会帮你检査是否符合函数式接口的规范(是否只有一个抽象方法)，如果不符合会报错。
- 可以包含默认方法 default 和静态方法 static：函数式接口允许有多个默认方法和静态方法，因为它们不是抽象方法，不影响单一抽象方法的要求。

```java
// 创建自定义函数式接口  
@FunctionalInterface  
public interface Calculator {  
    double calculate(double a, double b);  
  
    // 可以有默认方法  
    default double add(double a, double b) {  
        return a + b;  
    }  
      
    // 可以有静态方法  
    static Calculator multiply() {  
        return (a, b) -> a * b;  
    }  
}
```


## 方法引用与构造器引用

### 方法引用

当要传递给Lambda体的操作，已经有实现的方法了，可以使用方法引用！（实现抽象方法的参数列表，必须与方法引用方法的参数列表保持一致！）方法引用：使用操作符 “ ::” 将方法名和对象或类的名字分隔开来。

如下三种主要使用情况 ：

- 类 :: 静态方法
- 类 :: 实例方法 （当需要引用方法的第一个参数是调用对象 ， 并且第二个参数是需要引用方法的第二 个 参数( ( 或无参数) ) 时可以使用）
- 对象 :: 实例方法

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202510292202280.png)

- 指向静态方法的引用
- 指向某个对象的实例方法的引用
- 指向某个类型的实例方法的引用
- 指向构造方法的引用



```java
例如:(x) -> ystem.out.println(x);
等同于:
System.out::println

例如:
Binaryoperator<Double> bo = (x,y) -> Math.pow(x,y);
等同于:
BinaryOperator<Double> bo = Math::pow;

例如:
compare((x，y) -> x.equals(y),"abcdef","abcdef");
等同于:
compare(String::equals,"abc","abc");
```



### 构造器引用

格式 ： ClassName::new

与函数式接口相结合，自动与函数式接口中方法兼容。可以把构造器引用赋值给定义的方法，与构造器参数列表要与接口中抽象方法的参数列表一致！

```java
例如:
Function<Integer,Myclass> fu n= (n) -> new MyClass(n);
等同于:
Function<Integer,MyClass> fun = MyClass::new;
```



### 数组引用

格式 ： type[] :: new

```java
例如:
Function<Integer, Integer[]> fun = (n) -> new Integer[n];
等同于:
Function<Integer, Integer[]> fun = Integerl[]::new;
```



## 强大的stream

### 了解Stream

Java8中有两大最为重要的改变。第一个是 Lambda 表达式；另外一个则是Stream API(java.util.stream.* ) 。Stream 是 Java8 中处理集合的关键抽象概念，它可以指定你希望对集合进行的操作，可以执行非常复杂的查找、过滤和映射数据等操作。使用Stream API 对集合数据进行操作，就类似于使用 SQL 执行的数据库查询。也可以使用 Stream API 来并行执行操作。简而言之，Stream API 提供了一种高效且易于使用的处理数据的方式。

但需要**注意**的是，Stream的中间操作（比如map、filter等）是惰性的，这意味着它们在终端操作（比如collect、 forEach等）被调用之前不会实际执行。

### 什么是Stream

流) (Stream) 到底是什么呢？是数据渠道，用于操作数据源（集合、数组等）所生成的元素序列。“ 集合讲的是数据 ， 流讲的是 计 算 ！ ”

注意 ：

①Stream 自己不会存储元素。

②Stream 不会改变源对象。相反，他们会返回一个持有结果的新Stream。

③Stream 操作是延迟执行的。这意味着他们会等到需要结果的时候才执行。

详情可以看这篇文章 [Stream流原理](https://www.seven97.top/java/basis/stream.html)


## 接口中的默认方法与静态方法

### 接口中的默认方法

Java 8 引入的接口默认方法解决了接口演化的问题。

在默认方法出现之前，如果你想给一个被广泛使用的接口添加新方法，就会影响所有已有的实现类。想象一下，如果要给 Collection 接口添加一个新方法，ArrayList、LinkedList 等所有的实现类都需要修改，成本很大。默认方法让接口可以在 不破坏现有代码的情况下添加新功能。

举个例子，如果想要给接口增加一个 drawwithBorder 方法

```java
public interface Drawable {  
    // 已有抽象方法  
    void draw();  
      
    // 默认方法  
    default void drawWithBorder() {  
        System.out.println("绘制边框");  
        draw();  
        System.out.println("边框绘制完成");  
    }  
}
```

使用默认方法后，实现类可以选择重写默认方法，也可以直接使用：

```java
// 实现类可以选择重写默认方法  
public class Circle implements Drawable {  
    @Override  
    public void draw() {  
        System.out.println("绘制圆形");  
    }  
      
    // 可以重写默认方法  
    @Override  
    public void drawWithBorder() {  
        System.out.println("绘制圆形边框");  
        draw();  
    }  
}
```

Java8为 Collection 接口添加了 stream、removelf 等方法，都是默认方法：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202510292210466.png)

需要注意的是，如果一个类实现多个接口，并且这些接口有相同的默认方法时，
需要显式解决冲突：

```java
interface A {  
    default void hello() {  
        System.out.println("Hello from A");  
    }  
}  
  
interface B {  
    default void hello() {  
        System.out.println("Hello from B");  
    }  
}  
  
// 实现类必须重写冲突的方法  
class C implements A, B {  
    @Override  
    public void hello() {  
        // 可以调用特定接口的默认方法  
        A.super.hello();  
        B.super.hello();  
        // 或者提供自己的实现  
        System.out.println("Hello from C");  
    }  
}
```


为什么要有默认方法：

1. 向后兼容性：在Java 8之前，如果你向一个接口添加方法，那么所有实现了该接口的类都必须修改，以实现新增的方法。这在大型项目中是不现实的，因为这会破坏现有的实现。通过使用default方法，你可以向接口中添加新方法，而无需改变实现该接口的类。
2. 代码复用：default方法允许在接口内部提供方法的实现，从而使得代码复用变得可能。这意味着如果多个实现类可以共享某个方法的实现逻辑，那么这个逻辑就可以作为一个default方法放在接口中，避免了在每个实现类中重复此逻辑。
3. 多继承的灵活性：尽管Java不允许类多重继承（以避免多重继承的复杂性和歧义），但通过default方法，Java可以在接口层面上提供类似多重继承的能力。一个类可以实现多个接口，并且每个接口可以通过default方法提供具体实现，这给Java增加了额外的灵活性和功能。
4. 弥补抽象类的不足：在Java 8之前，如果你想为一组类提供公共实现，通常的方法是使用抽象类。然而，由于Java不支持从多个类继承，这种方法的使用非常受限。default方法提供了一种机制，通过它接口可以提供实现，而不需要抽象类，同时也不受单继承的限制。



### 接口中的静态方法

```java
public interface Utility {  
    static void printVersion() {  
        System.out.println("Java 8");  
    }  
      
    static String formatMessage(String message) {  
        return "[INFO] " + message;  
    }  
}  
  
// 调用接口静态方法  
Utility.printVersion();  
String formatted = Utility.formatMessage("Hello World");
```


## Optional 类

`Optional<T>` 类(java.util.Optional) 是一个容器类，代表一个值存在或不存在，原来用 null 表示一个值不存在，现在 Optional 可以更好的表达这个概念。并且**可以避免空指针异常**。

之前，我们只能通过大量的 if 语句检査 nul 来避免空指针异常，不仅代码又臭又长，而且稍微不注意就漏掉了。

```java
// 传统的空值检查  
public String getDefaultName(User user) {  
    if (user != null) {  
        String name = user.getName();  
        if (name != null && !name.isEmpty()) {  
            return name.toUpperCase();  
        }  
    }  
    return "unknown";  
}
```

Optional 类的引入就是为了优雅地处理可能为空的值，可以先把它理解为"包装器"，把可能为空的对象封装起来。

创建 Optional 对象

```java
// 创建 Optional 对象  
Optional<String> optional1 = Optional.of("Hello");          // 不能为 null  
Optional<String> optional2 = Optional.ofNullable(getName()); // 可能为 null  
Optional<String> optional3 = Optional.empty();              // 空的 Optional
```


常用方法 ：

- Optional.of(T t) : 创建一个 Optional 实例
- Optional.empty() : 创建一个空的 Optional 实例
- Optional.ofNullable(T t):若 t 不为 null,创建 Optional 实例,否则创建空实例
- isPresent() : 判断是否包含值
- orElse(T t) : 如果调用对象包含值，返回该值，否则返回t
- orElseGet(Supplier s) :如果调用对象包含值，返回该值，否则返回 s 获取的值
- map(Function f): 如果有值对其处理，并返回处理后的Optional，否则返回 Optional.empty()
- flatMap(Function mapper):与 map 类似，要求返回值必须是Optional


## 新时间日期API

传统的日期处理方式
```java
// 旧版本的复杂日期处理  
Calendar cal = Calendar.getInstance();  
cal.set(2024, Calendar.JANUARY, 15); // 注意月份从0开始  
Date date = cal.getTime();  
  
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");  
String dateStr = sdf.format(date); // 线程不安全
```


使用新的日期时间 API，代码会更简洁

```java
// 当前日期时间  
LocalDate today = LocalDate.now(); // 2025-09-01  
LocalTime now = LocalTime.now();   // 14:30:25.123  
LocalDateTime dateTime = LocalDateTime.now(); // 2025-09-01T14:30:25.123  
  
// 指定的日期时间  
LocalDate specificDate = LocalDate.of(2025, 09, 01);  
LocalTime specificTime = LocalTime.of(14, 30, 0);  
LocalDateTime specificDateTime = LocalDateTime.of(2025, 09, 01, 14, 30, 0);
```

典型的应用场景是从字符串解析日期，一行代码就能搞定：

```java
// 从字符串解析  
LocalDate parsedDate = LocalDate.parse("2025-09-01");  
LocalDateTime parsedDateTime = LocalDateTime.parse("2025-09-01T14:30:25");  
  
// 自定义格式解析  
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
LocalDateTime customParsed = LocalDateTime.parse("2025/09/01 14:30:25", formatter);
```

还有日期和时间的计算，也变得更直观、见名知意：

```java
LocalDate today = LocalDate.now();  
  
// 基本的日期计算  
LocalDate nextWeek = today.plusWeeks(1);  
LocalDate lastMonth = today.minusMonths(1);  
LocalDate nextYear = today.plusYears(1);  
  
// 时间段计算  
LocalDate startDate = LocalDate.of(2024, 1, 28);  
LocalDate endDate = LocalDate.of(2025, 9, 1);  
Period period = Period.between(startDate, endDate);  
System.out.println("相差 " + period.getMonths() + " 个月 " + period.getDays() + " 天");  
  
// 精确时间差计算  
LocalDateTime start = LocalDateTime.now();  
LocalDateTime end = LocalDateTime.of(2025, 09, 01, 14, 30, 0);  
Duration duration = Duration.between(start, end);  
System.out.println("执行时间：" + duration.toMillis() + " 毫秒");
```

还支持时区处理和时间戳处理，不过这段代码就没必要记了，现在有了 AI，直接让它生成时间日期操作就好。

```java
// 带时区的日期时间  
ZonedDateTime beijingTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));  
ZonedDateTime newYorkTime = ZonedDateTime.now(ZoneId.of("America/New_York"));  
  
// 时区转换  
ZonedDateTime beijingToNewYork = beijingTime.withZoneSameInstant(ZoneId.of("America/New_York"));  
  
// 获取所有可用时区  
ZoneId.getAvailableZoneIds().stream()  
    .filter(zoneId -> zoneId.contains("Shanghai"))  
    .forEach(System.out::println);  
  
// 时间戳处理  
Instant instant = Instant.now();  
long epochSecond = instant.getEpochSecond();  
ZonedDateTime fromInstant = instant.atZone(ZoneId.systemDefault());
```


## 重复注解

Java8之前，同一个注解不能在同一个地方重复使用。如果需要多个相似的配置，只能使用数组形式的注解：

```java
// Java 8 之前的做法  
@Schedules({  
    @Schedule(dayOfMonth="last"),  
    @Schedule(dayOfWeek="Fri", hour="23")  
})  
public void doPeriodicCleanup() { ... }
```

 Java8引入了重复注解特性，让同一个注解可以在同一个地方多次使用：

```java
// Java 8 的重复注解  
@Schedule(dayOfMonth="last")  
@Schedule(dayOfWeek="Fri", hour="23")  
public void doPeriodicCleanup() { ... }
```

要实现重复注解，需要定义一个容器注解：

```java
@Retention(RetentionPolicy.RUNTIME)  
@Target(ElementType.METHOD)  
public @interface Schedule {  
    String dayOfMonth() default "first";  
    String dayOfWeek() default "Mon";  
    int hour() default 12;  
}  
  
@Retention(RetentionPolicy.RUNTIME)  
@Target(ElementType.METHOD)  
public @interface Schedules {  
    Schedule[] value();  
}
```

然后在:@Schedule 注解上使用@Repeatable 指定容器注解：
```java
@Repeatable(Schedules.class)  
@Retention(RetentionPolicy.RUNTIME)  
@Target(ElementType.METHOD)  
public @interface Schedule {  
    String dayOfMonth() default "first";  
    String dayOfWeek() default "Mon";  
    int hour() default 12;  
}
```

## 类型注解

Java8 扩展了注解的使用范围，现在注解可以用在任何使用类型的地方，包括：

```java
// 创建对象  
@NonNull String str = new @Interned String("Hello");  
  
// 类型转换  
String myString = (@NonNull String) str;  
  
// 继承  
class UnmodifiableList<T> implements @Readonly List<@Readonly T> { ... }  
  
// 抛出异常  
void monitorTemperature() throws @Critical TemperatureException { ... }  
  
// 泛型参数  
List<@NonNull String> strings = new ArrayList<>();  
Map<@NonNull String, @NonNull Integer> map = new HashMap<>();  
  
// 方法参数和返回值  
public @NonNull String process(@NonNull String input) {  
    return input.toUpperCase();  
}
```

类型注解主要用于静态分析工具(如 Checker Framework)进行更精确的类型检查，帮助发现潜在的空指针异常、并发问题等。







<!-- @include: @article-footer.snippet.md -->     



