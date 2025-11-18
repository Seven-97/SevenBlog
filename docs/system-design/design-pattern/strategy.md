---
title: 策略模式
category: 系统设计
tags:
  - 设计模式
---





## 概述

先看下面的图片，我们去旅游选择出行模式有很多种，可以骑自行车、可以坐汽车、可以坐火车、可以坐飞机。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271802358.png)

作为一个程序猿，开发需要选择一款开发工具，当然可以进行代码开发的工具有很多，可以选择Idea进行开发，也可以使用eclipse进行开发，也可以使用其他的一些开发工具。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271802372.png)

**定义**：该模式定义了一系列算法，并将每个算法封装起来，使它们可以相互替换，且算法的变化不会影响使用算法的客户。策略模式属于对象行为模式，它通过对算法进行封装，把使用算法的责任和算法的实现分割开来，并委派给不同的对象对这些算法进行管理。

## 结构

策略模式的主要角色如下：

- 抽象策略（Strategy）类：这是一个抽象角色，通常由一个接口或抽象类实现。此角色给出所有的具体策略类所需的接口。

- 具体策略（Concrete Strategy）类：实现了抽象策略定义的接口，提供具体的算法实现或行为。

- 环境（Context）类：持有一个策略类的引用，最终给客户端调用。

## 案例实现

【例】促销活动

一家百货公司在定年度的促销活动。针对不同的节日（春节、中秋节、圣诞节）推出不同的促销活动，由促销员将促销活动展示给客户。类图如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271802599.png)

代码如下：

定义百货公司所有促销活动的共同接口

```java
public interface Strategy {
    void show();
}
```



定义具体策略角色（Concrete Strategy）：每个节日具体的促销活动

```java          
//为春节准备的促销活动A
public class StrategyA implements Strategy {

    public void show() {
        System.out.println("买一送一");
    }
}

//为中秋准备的促销活动B
public class StrategyB implements Strategy {

    public void show() {
        System.out.println("满200元减50元");
    }
}

//为圣诞准备的促销活动C
public class StrategyC implements Strategy {

    public void show() {
        System.out.println("满1000元加一元换购任意200元以下商品");
    }
}
```

定义环境角色（Context）：用于连接上下文，即把促销活动推销给客户，这里可以理解为销售员

```java
public class SalesMan {                        
    //持有抽象策略角色的引用                              
    private Strategy strategy;                 
                                               
    public SalesMan(Strategy strategy) {       
        this.strategy = strategy;              
    }                                          
                                               
    //向客户展示促销活动                                
    public void salesManShow(){                
        strategy.show();                       
    }                                          
}                                              
```



## 优缺点

**优点：**

- 策略类之间可以自由切换：由于策略类都实现同一个接口，所以使它们之间可以自由切换。

- 易于扩展：增加一个新的策略只需要添加一个具体的策略类即可，基本不需要改变原有的代码，符合“开闭原则“

- 避免使用多重条件选择语句（if else），充分体现面向对象设计思想。

**缺点：**

- 客户端必须知道所有的策略类，并自行决定使用哪一个策略类。

- 策略模式将造成产生很多策略类，可以通过使用享元模式在一定程度上减少对象的数量。

## 使用场景

- 一个系统需要动态地在几种算法中选择一种时，可将每个算法封装到策略类中。

- 一个类定义了多种行为，并且这些行为在这个类的操作中以多个条件语句的形式出现，可将每个条件分支移入它们各自的策略类中以代替这些条件语句。

- 系统中各算法彼此完全独立，且要求对客户隐藏具体算法的实现细节时。

- 系统要求使用算法的客户不应该知道其操作的数据时，可使用策略模式来隐藏与算法相关的数据结构。

- 多个类只区别在表现行为不同，可以使用策略模式，在运行时动态选择具体要执行的行为。

## 源码解析 - Comparator

Comparator 中的策略模式。在Arrays类中有一个 sort() 方法，如下：

```java
public class Arrays{
    public static <T> void sort(T[] a, Comparator<? super T> c) {
        if (c == null) {
            sort(a);
        } else {
            if (LegacyMergeSort.userRequested)
                legacyMergeSort(a, c);
            else
                TimSort.sort(a, 0, a.length, c, null, 0, 0);
        }
    }
}
```



Arrays就是一个环境角色类，这个sort方法可以传一个新策略让Arrays根据这个策略来进行排序。就比如下面的测试类。

```java
public class demo {
    public static void main(String[] args) {

        Integer[] data = {12, 2, 3, 2, 4, 5, 1};
        // 实现降序排序
        Arrays.sort(data, new Comparator<Integer>() {
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        });
        System.out.println(Arrays.toString(data)); //[12, 5, 4, 3, 2, 2, 1]
    }
}
```



这里我们在调用Arrays的sort方法时，第二个参数传递的是Comparator接口的子实现类对象。所以Comparator充当的是抽象策略角色，而具体的子实现类充当的是具体策略角色。环境角色类（Arrays）应该持有抽象策略的引用来调用。那么，Arrays类的sort方法到底有没有使用Comparator子实现类中的 compare() 方法吗？让我们继续查看TimSort类的 sort() 方法，代码如下：

```java
class TimSort<T> {
    static <T> void sort(T[] a, int lo, int hi, Comparator<? super T> c,
                         T[] work, int workBase, int workLen) {
        assert c != null && a != null && lo >= 0 && lo <= hi && hi <= a.length;

        int nRemaining  = hi - lo;
        if (nRemaining < 2)
            return;  // Arrays of size 0 and 1 are always sorted

        // If array is small, do a "mini-TimSort" with no merges
        if (nRemaining < MIN_MERGE) {
            int initRunLen = countRunAndMakeAscending(a, lo, hi, c);
            binarySort(a, lo, hi, lo + initRunLen, c);
            return;
        }
        ...
    }   
        
    private static <T> int countRunAndMakeAscending(T[] a, int lo, int hi,Comparator<? super T> c) {
        assert lo < hi;
        int runHi = lo + 1;
        if (runHi == hi)
            return 1;

        // Find end of run, and reverse range if descending
        if (c.compare(a[runHi++], a[lo]) < 0) { // Descending
            while (runHi < hi && c.compare(a[runHi], a[runHi - 1]) < 0)
                runHi++;
            reverseRange(a, lo, runHi);
        } else {                              // Ascending
            while (runHi < hi && c.compare(a[runHi], a[runHi - 1]) >= 0)
                runHi++;
        }

        return runHi - lo;
    }
}
```

上面的代码中最终会跑到 countRunAndMakeAscending() 这个方法中。我们可以看见，只用了compare方法，所以在调用Arrays.sort方法只传具体compare重写方法的类对象就行，这也是Comparator接口中必须要子类实现的一个方法。


## 重构-替换 if-else

若遇到大量流程判断语句，几乎满屏都是if-else语句，**其实if-else是一种面向过程的实现**。那么，如何避免在面向对象编程里大量使用if-else呢？

假如有这样一个需求，需实现一周七天内分别知道要做事情的备忘功能，这里面就会涉及到一个流程判断，你可能会立马想到用if-else，那么，可能是会这样实现

```java
//if-else形式判断
public String getToDo(String day){
     if("Monday".equals(day)){          
         //......省略复杂语句
         return "今天上英语课";
     }else if("Tuesday".equals(day)){          
         //.....省略复杂语句          
         return "今天上语文课";
     }else if("Wednesday".equals(day)){         
         //......省略复杂语句
         return "今天上数学课";
     }else if("Thursday".equals(day)){         
         //......省略复杂语句
         return "今天上音乐课";
     }else if("sunday".equals(day)){         
         //......省略复杂语句
         return "今天上编程课";
     }else{
         //此处省略10086行......
     }
 }
```

这种代码，在业务逻辑里，少量还好，若是几百个判断呢，可能整块业务逻辑里都是满屏if-else,既不优雅也显得很少冗余。

### 工厂类

这种方法将对象的创建逻辑封装到一个专门的工厂类中，符合“开闭原则”，后续新增课程类型时，只需扩展工厂类即可。

**重构步骤：**
1. **定义公共接口**：创建一个接口（如 `CourseService`），声明公共方法（如 `getToDo()`）。
2. **实现具体类**：为每一种课程（如英语、语文）创建一个类，实现该接口，完成各自的复杂逻辑。
3. **创建工厂类**：构建一个工厂类（如 `CourseServiceFactory`），在静态代码块或初始化时，建立“星期几”到具体课程服务实例的映射关系（通常使用 `Map`）。
4. **修改主方法**：在原方法中，通过工厂类根据输入参数 `day`获取对应的服务实例，并调用其方法。

```java
// 1. 定义公共接口
public interface CourseService {
    String getToDo();
}

// 2. 实现具体类
public class EnglishCourseService implements CourseService {
    @Override
    public String getToDo() {
        //......原Monday分支的复杂语句
        return "今天上英语课";
    }
}
public class ChineseCourseService implements CourseService {
    @Override
    public String getToDo() {
        //......原Tuesday分支的复杂语句
        return "今天上语文课";
    }
}
// ... 为Wednesday, Thursday, Sunday等创建类似类

// 3. 创建工厂类
public class CourseServiceFactory {
    private static final Map<String, CourseService> map = new HashMap<>();
    static {
        map.put("Monday", new EnglishCourseService());
        map.put("Tuesday", new ChineseCourseService());
        // ... 注册其他项
    }
    public static CourseService getService(String day) {
        return map.get(day);
    }
}

// 4. 修改原方法
public String getToDo(String day) {
    CourseService service = CourseServiceFactory.getService(day);
    if (service != null) {
        return service.getToDo();
    }
    // 处理默认或错误情况，例如return "未知日程"; 或抛出异常
    return null;
}
```


### 枚举

枚举非常适合管理一组固定的常量，并且可以将每个常量对应的行为直接封装在枚举项内部，使代码非常清晰。

**重构步骤：**

1. **定义枚举类型**：创建一个枚举（如 `DayCourse`），枚举值就是星期几。
2. **封装行为与属性**：为枚举定义一个抽象方法（如 `getToDo()`），然后在每个枚举值中实现这个方法的具体逻辑。
3. **修改主方法**：原方法通过 `DayCourse.valueOf(day)`获取对应的枚举实例，并调用其方法。

```java
// 1. 定义枚举
public enum DayCourse {
    Monday {
        @Override
        public String getToDo() {
            //......原Monday分支的复杂语句
            return "今天上英语课";
        }
    },
    Tuesday {
        @Override
        public String getToDo() {
            //......原Tuesday分支的复杂语句
            return "今天上语文课";
        }
    },
    // ... 定义其他天数
    Sunday {
        @Override
        public String getToDo() {
            //......原Sunday分支的复杂语句
            return "今天上编程课";
        }
    };
    public abstract String getToDo();
}

// 2. 修改原方法
public String getToDo(String day) {
    try {
        DayCourse dayCourse = DayCourse.valueOf(day); // 将字符串转换为枚举值
        return dayCourse.getToDo();
    } catch (IllegalArgumentException e) {
        // 处理day不匹配任何枚举值的情况
        return "未知日程";
    }
}
```


### 命令模式

命令模式将请求封装为对象，使得你可以用不同的请求参数化其他对象，并支持请求的排队、记录、撤销等。

**重构步骤：**

1. **定义命令接口**：创建一个接口（如 `Command`），其中定义一个执行方法（如 `execute()`）。
2. **实现具体命令**：为每一种课程创建一个类，实现 `Command`接口。
3. **修改主方法**：原方法中，根据 `day`创建对应的具体命令对象，并调用其 `execute()`方法。

```java
// 1. 定义命令接口
public interface Command {
    String execute();
}

// 2. 实现具体命令
public class EnglishCommand implements Command {
    @Override
    public String execute() {
        //......原Monday分支的复杂语句
        return "今天上英语课";
    }
}
// ... 为其他天数创建类似的Command类

// 3. 修改原方法 (可结合简单工厂优化Command的创建)
public String getToDo(String day) {
    Command command;
    switch (day) {
        case "Monday": command = new EnglishCommand(); break;
        case "Tuesday": command = new ChineseCommand(); break;
        // ... 其他情况
        default: command = new UnknownCommand(); break;
    }
    return command.execute();
}
```


### 规则引擎

当业务规则非常复杂且需要动态变更时，规则引擎是理想选择，它实现了业务逻辑与执行逻辑的彻底分离。

**重构步骤：**

1. **定义规则接口**：创建一个接口（如 `Rule`），包含评估条件的方法（如 `evaluate`）和执行操作的方法（如 `execute`）。
2. **实现具体规则**：为每一天创建一个规则类，实现 `Rule`接口。
3. **创建规则引擎**：创建一个规则引擎类，用于注册和管理所有规则，并提供一个方法遍历所有规则，找到条件满足的规则并执行。
4. **修改主方法**：原方法中，调用规则引擎来执行。

```java
// 1. 定义规则接口
public interface Rule {
    boolean evaluate(String day);
    String execute();
}

// 2. 实现具体规则
public class MondayRule implements Rule {
    @Override
    public boolean evaluate(String day) {
        return "Monday".equals(day);
    }
    @Override
    public String execute() {
        //......原Monday分支的复杂语句
        return "今天上英语课";
    }
}
// ... 其他规则

// 3. 创建简单的规则引擎
public class SimpleRuleEngine {
    private List<Rule> rules = new ArrayList<>();
    public void addRule(Rule rule) {
        rules.add(rule);
    }
    public String run(String day) {
        for (Rule rule : rules) {
            if (rule.evaluate(day)) {
                return rule.execute();
            }
        }
        return "未知日程"; // 默认规则
    }
}

// 4. 修改原方法 (需初始化引擎和规则)
public class Scheduler {
    private SimpleRuleEngine ruleEngine = new SimpleRuleEngine();
    public Scheduler() {
        // 初始化，注册规则
        ruleEngine.addRule(new MondayRule());
        ruleEngine.addRule(new TuesdayRule());
        // ...
    }
    public String getToDo(String day) {
        return ruleEngine.run(day);
    }
}
```


### 策略模式

策略模式定义一族算法，并封装每个算法，使它们可以相互替换，让算法的变化独立于使用算法的客户。

**重构步骤：**

1. **定义策略接口**：与命令模式类似，定义一个策略接口（如 `CourseStrategy`）。
2. **实现具体策略**：为每一天创建一个策略类，实现该接口。
3. **创建策略上下文**：创建一个上下文类（如 `CourseContext`），它包含一个策略引用，并提供一个方法来执行当前策略。
4. **修改主方法**：原方法中，根据 `day`设置上下文对象的策略，然后执行。

```java
// 1. 定义策略接口
public interface CourseStrategy {
    String getToDo();
}

// 2. 实现具体策略
public class EnglishStrategy implements CourseStrategy {
    @Override
    public String getToDo() {
        //......原Monday分支的复杂语句
        return "今天上英语课";
    }
}
// ... 其他策略

// 3. 创建策略上下文
public class CourseContext {
    private CourseStrategy strategy;
    public void setStrategy(CourseStrategy strategy) {
        this.strategy = strategy;
    }
    public String executeStrategy() {
        if (strategy != null) {
            return strategy.getToDo();
        }
        throw new IllegalStateException("Strategy not set");
    }
}

// 4. 修改原方法 (可结合Map优化策略选择)
public String getToDo(String day) {
    CourseContext context = new CourseContext();
    switch (day) {
        case "Monday": context.setStrategy(new EnglishStrategy()); break;
        case "Tuesday": context.setStrategy(new ChineseStrategy()); break;
        // ...
        default: throw new IllegalArgumentException("Invalid day: " + day);
    }
    return context.executeStrategy();
}
```

### 策略枚举

这其实是属于 **外层枚举（路由枚举）+ 内层策略枚举**​ 的混合结构

首先，先定义一个getToDo()调用方法，假如传进的是“星期一”，即参数"Monday"。

```java
//策略枚举判断
public String getToDo(String day){
    CheckDay checkDay=new CheckDay();
    return checkDay.day(DayEnum.valueOf(day));
}
```

在getToDo()方法里，通过DayEnum.valueOf("Monday")可获取到一个DayEnum枚举元素，这里得到的是Monday。

接下来，执行checkDay.day(DayEnum.valueOf("Monday"))，会进入到day（）方法中，这里，**通过dayEnum.toDo()做了一个策略匹配时**。注意一点，DayEnum.valueOf("Monday")得到的是枚举中的Monday，这样，实质上就是执行了Monday.toDo()，也就是说，会执行Monday里的toDo()

```java
public class CheckDay {
    public String day( DayEnum dayEnum) {
        return dayEnum.toDo();
    }
}
```

上面的执行过程为什么会是这样子呢？只有进入到DayEnum枚举当中，才知道是怎么回事了

```java
public enum DayEnum {
    Monday {
        @Override
        public String toDo() {            ......省略复杂语句
            return "今天上英语课";
        }
    },
    Tuesday {
        @Override
        public String toDo() {            ......省略复杂语句
            return "今天上语文课";
        }
    },
    Wednesday {
        @Override
        public String toDo() {            ......省略复杂语句
            return "今天上数学课";
        }
    },
    Thursday {
        @Override
        public String toDo() {            ......省略复杂语句
            return "今天上音乐课";
        }
    };
    //定义了一个实现了toDo()抽象方法
    public abstract String toDo();
}
```

在每个枚举元素当中，都重写了该toDo()抽象方法。这样，当传参DayEnum.valueOf("Monday")流转到dayEnum.toDo()时，实质上是去DayEnum枚举里找到对应Monday定义的枚举元素，然后执行其内部重写的toDo()方法。用if-esle形式表示，就类似"Monday".equals(day)匹配为true时，可得到其内部东西。

总结一下，策略枚举就是**枚举当中使用了策略模式**，所谓的策略模式，即给你一把钥匙，按照某种约定的方式，可以立马被指引找到可以打开的门。例如，我给你的钥匙叫“Monday”，那么，就可以通过约定方式dayEnum.toDo()，立马找到枚举里的Monday大门，然后进到门里，去做想做的事toDo()，其中，每扇门后的房间都有不同的功能，但它们都有一个相同抽象功能——toDo()，即各房间共同地方都是可以用来做一些事情的功能，但具体可以什么事情，就各有不同了。在本文的案例里，每扇大门里的toDo()，根据不同策略模式可得到不同字符串返回，例如，"今天上英语课"、"今天上语文课"，等等。

可见，把流程判断抽取到策略枚举当中，还可以把一堆判断解耦出来，避免在业务代码逻辑里呈现一大片密密麻麻冗余的if-else。

这里，会出现一种情况，即，假如有多个重复共同样功能的判断话，例如，在if-else里，是这样

```java
public String getToDoByIfElse(String day){
    if("Monday".equals(day) || "Tuesday".equals(day) || "Wednesday".equals(day)){        
        //......省略复杂语句
        return "今天上英语课";
    }else if("Thursday".equals(day)){
        ......
    }
}
```

那么，在策略枚举下应该如何使用从而避免代码冗余呢？

可以参考一下以下思路，设置一个内部策略枚举，将有相同功能的外部引用指向同一个内部枚举元素，这样即可实现调用重复功能了

```java
public enum DayEnum {
    //指向内部枚举的同一个属性即可执行相同重复功能
    Monday("星期一", Type.ENGLISH),
    Tuesday("星期二", Type.ENGLISH),
    Wednesday("星期三", Type.ENGLISH),
    
    Thursday("星期四", Type.CHINESE);
    
    private final Type type;
    private final String day;
    DayEnum(String day, Type type) {
        this.day = day;
        this.type = type;
    }
    String toDo() {
        return type.toDo();
    }
    /**
     * 内部策略枚举
     */
    private enum Type {
        ENGLISH {
            @Override
            public String toDo() {                ......省略复杂语句
                return "今天上英语课";
            }
        },
        CHINESE {
            @Override
            public String toDo() {                ......省略复杂语句
                return "今天上语文课";
            }
        };
        public abstract String toDo();
    }
}
```

若要扩展其判断流程，只需要直接在枚举增加一个属性和内部toDo（实现），就可以增加新的判断流程了，而外部，仍旧用同一个入口dayEnum.toDo()即可。


总结一下，这种方式核心在于**将“路由”和“行为”进行了解耦**。
- **外层枚举 (`DayEnum`）作为“路由表”**：负责将具体的输入参数（如`"Monday"`）映射到一个抽象的策略类型（如`Type.ENGLISH`）。这使得**映射关系非常集中和清晰**。
- **内层枚举 (`Type`）作为“策略实现集”**：它封装了具体的业务逻辑。所有被路由到`Type.ENGLISH`的日期，都共享同一套复杂的业务逻辑。

这种做法极大地提升了代码的**可维护性和复用性**。如果需要修改“英语课”的逻辑，只需修改`Type.ENGLISH`的`toDo()`方法，所有关联的日期（如周一、周二、周三）都会自动生效

尽管这种写法在特定场景下非常优雅，但也存在一些限制：
1. **编译时绑定**：枚举最大的特点是所有值在编译时就必须确定。无法在程序运行时不修改代码、通过外部配置就动态地添加一个新的`DayEnum.Weekend`或一个新的课程类型`Type.MUSIC`。这对于需要高度动态配置的系统是一个硬约束。
2. **单一职责的权衡**：这种结构将路由和策略都塞进了一个枚举文件中。当策略逻辑非常复杂时（例如，`Type.CHINESE.toDo()`方法内部需要调用多个Service，完成一系列复杂操作），可能会导致这个枚举类变得臃肿。虽然可以通过在策略方法内部调用外部Service类来缓解，但这仍是需要权衡的点。

### 如何选择与小结

这几种重构方法各有侧重，可以根据实际场景选择：

- 如果业务逻辑相对稳定且类型固定，追求简洁直观，**枚举**是不错的选择。
- 如果每个分支的逻辑非常复杂且独立，希望将对象的创建与使用解耦，**工厂类**非常合适。
- 如果希望在运行时灵活切换算法，或者未来可能频繁增加新的课程类型，**策略模式**的扩展性更好。
- 如果需要将请求的发送与执行解耦，或者未来可能支持命令队列、撤销等高级功能，可以考虑**命令模式**。
- 如果业务规则极其复杂、多变，甚至需要从外部（如数据库、配置文件）动态加载规则，那么**规则引擎**能提供最大的灵活性。



<!-- @include: @article-footer.snippet.md -->