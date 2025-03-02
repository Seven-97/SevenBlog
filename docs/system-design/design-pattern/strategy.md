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



## 策略枚举替换大量的if-else语句

若遇到大量流程判断语句，几乎满屏都是if-else语句，**其实if-else是一种面向过程的实现。**那么，如何避免在面向对象编程里大量使用if-else呢？

网络上有很多解决思路，有工厂模式、策略模式、甚至是规则引擎，但这些使用起来还是过于繁重了。虽说避免出现过多的if-else，但是，却会增加很多额外的类。

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

这时，就可以考虑使用策略枚举形式来替换这堆面向过程的if-else实现了。

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

<!-- @include: @article-footer.snippet.md -->