---
title: Java 21新特性
category: Java
tag:
 - 版本新特性
---







Java20中没有太大的变化，这里主要聊下Java21的新特性，21是继Java17之后，最新的LTS版本，该版本中虚拟线程称为了正式版，[Java 19](https://www.seven97.top/java/new-features/java19.html)中是预览版

 

## 字符串模板

字符串模板可以让开发者更简洁的进行字符串拼接（例如拼接sql，xml，json等）。该特性并不是为字符串拼接运算符+提供的语法糖，也并非为了替换SpringBuffer和StringBuilder。

利用STR模板进行字符串与变量的拼接：

```java
String sport = "basketball";
String msg = STR."i like \{sport}";

System.out.println(msg);//i like basketball
```



这个特性目前是预览版，编译和运行需要添加额外的参数：

```java
Javac --enable-preview -source 21 Test.Java
Java --enable-preview Test
```



在js中字符串进行拼接时会采用下面的字符串插值写法

```java
let sport = "basketball"
let msg = `i like ${sport}`
```



看起来字符串插值写法更简洁移动，不过若在Java中使用这种字符串插值的写法拼接sql，可能会出现sql注入的问题，为了防止该问题，Java提供了字符串模板表达式的方式。

上面使用的STR是Java中定义的模板处理器，它可以将变量的值取出，完成字符串的拼接。在每个Java源文件中都引入了一个public static final修饰的STR属性，因此我们可以直接使用STR，STR通过打印STR可以知道它是Java.lang.StringTemplate，是一个接口。

在StringTemplate中是通过调用interpolate方法来执行的，该方法分别传入了两个参数：

- fragements：包含字符串模板中所有的字面量，是一个List

- values：包含字符串模板中所有的变量，是一个List

而该方法又调用了JavaTemplateAccess中的interpolate方法，经过分析可以得知，它最终是通过String中的join方法将字面量和变量进行的拼接

其他使用示例，在STR中可以进行基本的运算（支持三元运算）

```java
int x = 10, y = 20;
String result = STR."\{x} + \{y} = \{x + y}";
System.out.println(result);//10 + 20 = 30
```



调用方法：

```java
String result = STR."获取一个随机数: \{Math.random()}";
System.out.println(result);
```



获取属性：

```java
String result = STR."int最大值是: \{Integer.MAX_VALUE}";
System.out.println(result);
```



查看时间：

```java
String result = STR."现在时间: \{new SimpleDateFormat("yyyy-MM-dd").format(new Date())}";
System.out.println(result);
```



计数操作：

```java
int index = 0;
String result = STR."\{index++},\{index++},\{index++}";
System.out.println(result);
```



获取数组数据：

```java
String[] cars = {"bmw","benz","audi"};
String result = STR."\{cars[0]},\{cars[1]},\{cars[2]}";
System.out.println(result);
```



拼接多行数据：

```java
String name    = "jordan";
String phone   = "13388888888";
String address = "北京";
String json = STR."""
{
"name":    "\{name}",
"phone":   "\{phone}",
"address": "\{address}"
}
""";

System.out.println(json);
```



自己定义字符串模板，通过StringTemplate来自定义模板

```java
var INTER = StringTemplate.Processor.of((StringTemplate st) -> {
    StringBuilder sb = new StringBuilder();
    Iterator<String> fragIter = st.fragments().iterator();
    for (Object value : st.values()) {
        sb.append(fragIter.next());//字符串中的字面量
        sb.append(value);
    }
    sb.append(fragIter.next());
    return sb.toString();
});


int x = 10, y = 20;
String s = INTER."\{x} plus \{y} equals \{x + y}";

System.out.println(s);
```



## scoped values

### ThreadLocal的问题

scoped values 是一个隐藏的方法参数，只有方法可以访问scoped values，它可以让两个方法之间传递参数时无需声明形参。例如在UserDao类中编写了saveUser方法，LogDao类中编写了saveLog方法，那么在保存用户的时候需要保证事务，此时就需要在service层获取Connection对象，然后将该对象分别传入到两个Dao的方法中，但对于saveUser方法来说并不是直接使用Connection对象，却又不得不在方法的形参中写上该对象，其实仅从业务上来看，该方法中只要传入User对象就可以了。

```java
int saveUser(Connection connection,User user);
```

对于上面的问题，开发者通常会使用ThreadLocal解决，但由于ThreadLocal在设计上的瑕疵，导致下面问题：

1. 内存泄漏，在用完ThreadLocal之后若没有调用remove，这样就会出现内存泄漏。
2. 增加开销，在具有继承关系的线程中，子线程需要为父线程中ThreadLocal里面的数据分配内存。
3. 混乱的可变，任何可以调用ThreadLocal中get方法的代码都可以随时调用set方法，这样就不易辨别哪些方法是按照什么顺序来更新的共享数据。

随着虚拟线程的到来，内存泄漏问题就不用担心了，由于虚拟线程会很快的终止，此时会自动删除ThreadLocal中的数据，这样就不用调用remove方法了。但虚拟线程的数量通常是多的，试想下上百万个虚拟线程都要拷贝一份ThreadLocal中的变量，这会使内存承受更大的压力。为了解决这些问题，scoped values就出现了。

 

### ScopeValue初体验

在Java21中新增了ScopeValue类，为了便于多个方法使用，通常会将该类的对象声明为static final ，每个线程都能访问自己的scope value，与ThreadLocal不同的是，它只会被write 1次且仅在线程绑定的期间内有效。

下面代码模拟了送礼和收礼的场景

```java
public class Test{
    private static final ScopedValue<String> GIFT = ScopedValue.newInstance();

    public static void main(String[] args) {
        Test t = new Test();
        t.giveGift();
    }     

    //送礼
    public void giveGift() {
        /*
            在对象GIFT中增加字符串手机，当run方法执行时，
            会拷贝一份副本与当前线程绑定，当run方法结束时解绑。
            由此可见，这里GIFT中的字符串仅在收礼方法中可以取得。
         */
        ScopedValue.where(GIFT, "手机").run(() -> receiveGift());
    }

    //收礼
    public void receiveGift() {
        System.out.println(GIFT.get()); // 手机
    }

}
```



### 多线程操作相同的ScopeValue

不同的线程在操作同一个ScopeValue时，相互间不会影响，其本质是利用了Thread类中scopedValueBindings属性进行的线程绑定。

```java
import Java.util.concurrent.ExecutorService;
import Java.util.concurrent.Executors;

public class Test{
    private static final ScopedValue<String> GIFT = ScopedValue.newInstance();

    public static void main(String[] args) {
        Test t = new Test();

        ExecutorService pool = Executors.newCachedThreadPool();

        for (int i = 0; i < 10; i++) {
            pool.submit(()->{
                t.giveGift();
            });
        }

        pool.shutdown();
    }     

    //向ScopedValue中添加当前线程的名字
    public void giveGift() {
        ScopedValue.where(GIFT, Thread.currentThread().getName()).run(() -> receiveGift());
    }

    public void receiveGift() {
        System.out.println(GIFT.get()); 
    }

}
```



### ScopeValue的修改

通过上面的示例可以看到，ScopeValue的值是在第一次使用where的时候就设置好了，该值在当前线程使用的期间是不会被修改的，这样就提高了性能。当然，我们也可以修改ScopeValue中的值，但需要注意，这里的修改会不影响本次方法中读取的值，而是会导致where后run中调用的方法里面的值发生变化。

```java
public class Test{
    private static final ScopedValue<String> GIFT = ScopedValue.newInstance();

    public static void main(String[] args) {
        Test t = new Test();
        t.giveGift();
    }     

    public void giveGift() {
        ScopedValue.where(GIFT, "500元购物卡").run(() -> receiveMiddleMan());
    }


    //中间人
    public void receiveMiddleMan(){
        System.out.println(GIFT.get());//500
        //修改GIFT中的值，仅对run中调用的receiveGift方法生效
        ScopedValue.where(GIFT, "200元购物卡").run(() -> receiveGift());
        System.out.println(GIFT.get());//500
    }

    public void receiveGift() {
        System.out.println(GIFT.get()); //200
    }

}
```



## 集合序列

![image-20240425202520731](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404252025824.png)

在Java.util包下新增了3个接口

1. SequencedCollection
2. SequencedSet
3. SequencedMap

通过这些接口可以为之前的部分List，Set，Map的实现类增加新的方法，以List为例：

```java
List<Integer> list = new LinkedList<>();
list.add(1);
list.add(2);
list.add(3);

List<Integer> reversedList = list.reversed();//反转List
System.out.println(reversedList);

list.addFirst(4);//从List前面添加元素
list.addLast(5);//从List后面添加元素
System.out.println(list);
```



## record pattern

Java14引入的新特性。通过该特性可以解构record类型中的值，例如

```java
public class Test{
    public static void main(String[] args) {
        Student s = new Student(10, "jordan");
        printSum(s);
    }

    static void printSum(Object obj) {
        //这里的Student(int a, String b)就是 record pattern
        if (obj instanceof Student(int a, String b)) {
            System.out.println("id:" + a);
            System.out.println("name:" + b);
        }
    }

}
record Student(int id, String name) {}
```



## switch格式匹配

之前的写法

```java
public class Test{
    public static void main(String[] args) {
        Integer i = 10;
        String str = getObjInstance(i);
        System.out.println(str);
    }

    public static String getObjInstance(Object obj) {
        String objInstance = "";
        if(obj == null){
            objInstance = "空对象"
        } else if (obj instanceof Integer i) {
            objInstance = "Integer 对象：" + i;
        } else if (obj instanceof Double d) {
            objInstance = "Double 对象：" + d;
        } else if (obj instanceof String s) {
            objInstance = "String 对象：" + s;
        }
        return objInstance;
    }

}
```



新的写法，代码更加简洁

```java
public class Test{
    public static void main(String[] args) {
        Integer i = 10;
        String str = getObjInstance(i);
        System.out.println(str);
    }

    public static String getObjInstance(Object obj) {

        return switch(obj){
            case null -> "空对象";
            case Integer i -> "Integer 对象：" + i;
            case Double d -> "Double对象：" + d;
            case String s -> "String对象：" + s;
            default -> obj.toString();
        };
    }

}
```



可以在switch中使用when

```java
public class Test{
    public static void main(String[] args) {
        yesOrNo("yes");
    }

    public static void yesOrNo(String obj) {

        switch(obj){
            case null -> {System.out.println("空对象");}
            case String s
                when s.equalsIgnoreCase("yes") -> {
                System.out.println("确定");
            }
            case String s
                when s.equalsIgnoreCase("no") -> {
                System.out.println("取消");
            }
                //最后的case要写，否则编译回报错
            case String s -> {
                System.out.println("请输入yes或no");
            }

        };

    }

}
```



## Unnamed Classes and Instance Main Methods

对于初学者来说，写的第一个HelloWorld代码有太多的概念，为了方便初学者快速编写第一段Java代码，这里提出了无名类和实例main方法，下面代码可以直接运行编译，相当于是少了类的定义，main方法的修饰符和形参也省略掉了

```java
void main() {
    System.out.println("Hello, World!");
}
```



## Structured Concurrency 

该特性主要作用是在使用虚拟线程时，可以使任务和子任务的代码编写起来可读性更强，维护性更高，更加可靠。

```java
import Java.util.concurrent.ExecutionException;
import Java.util.concurrent.StructuredTaskScope;
import Java.util.function.Supplier;

public class Test {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Food f = new Test().handle();
        System.out.println(f);
    }

    Food handle() throws ExecutionException, InterruptedException {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            Supplier<String> yaoZi = scope.fork(() -> "新鲜大腰子烤好了");// 烤腰子的任务
            Supplier<String> drink = scope.fork(() -> "奶茶做好了");// 买饮料的任务

            scope.join() // 将2个子任务都加入
            .throwIfFailed(); // 失败传播

            // 当两个子任务都成功后，最终才能吃上饭
            return new Food(yaoZi.get(), drink.get());
        }
    }

}

record Food(String yaoZi, String drink) {
}
```


<!-- @include: @article-footer.snippet.md -->     
