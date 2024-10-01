---
title: 对象工具类 - Objects&Optional
category: 工具类库
tag:
  - Java工具类
---





Java 的 Objects 类是一个实用工具类，包含了一系列静态方法，用于处理对象。它位于 java.util 包中，自 Java 7 引入。Objects 类的主要目的是降低代码中的空指针异常(NullPointerException) 风险，同时提供一些非常实用的方法供我们使用。

## Objects - 对null的判断

### 对象判空

在 Java 中，万物皆对象，对象的判空可以说无处不在。Objects 的 `isNull` 方法用于判断对象是否为空，而 `nonNull` 方法判断对象是否不为空。例如：

```java
String str = "null";

if (Objects.isNull(str)) {
    System.out.println("对象为空");
}

if (Objects.nonNull(str)) {
    System.out.println("对象不为空");
}
```

源码很简单，就是直接判断是否为 null：

```java
public static boolean isNull(Object obj) {
    return obj == null;
}

public static boolean nonNull(Object obj) {
    return obj != null;
}
```





### 对象为空时抛异常requireNonNull

如果想在对象为空时，抛出空指针异常，可以使用 Objects 的 `requireNonNull` 方法。例如：

```java
String str = "null";

Objects.requireNonNull(str);
Objects.requireNonNull(str, "参数不能为空");
Objects.requireNonNull(str, () - > "参数不能为空");
```



源码如下：

```java
// 判断是否为空，空则返回异常，反之返回传入的参数 
public static < T > T requireNonNull(T obj) {
     if (obj == null)
         throw new NullPointerException();
     return obj;
 }

// 与第一种一样，区别是可以传入一个自定义的提示
 public static < T > T requireNonNull(T obj, String message) {
     if (obj == null)
         throw new NullPointerException(message);
     return obj;
 }

// 与第二种一样，区别是对自定义的提示做了一个判断，如果自定义的信息为null 则报错时就是默认的格式（和第一种一样，有点鸡肋了）
 public static < T > T requireNonNull(T obj, Supplier < String > messageSupplier) {
     if (obj == null)
         throw new NullPointerException(messageSupplier == null ?
             null : messageSupplier.get());
     return obj;
 }
```



### requireNonNullElse

源码

```java
// 对传进来的数据进行判断，返回第一个非空值（obj）否则返回第二个非空值（defaultObj）
// 可以看到里面调用了requireNoNull，若第二个也为空则报错，在错误后面提示defualtObj；
public static < T > T requireNonNullElse(T obj, T defaultObj) {
    return (obj != null) ? obj : requireNonNull(defaultObj, "defaultObj");
}
```



用法与requireNonNull相似

```java
String str = null;
System.out.println(Objects.requireNonNullElse(str, "我不是null")); // 我不是null
```



### requireNonNullElseGet

源码：

```java
public static < T > T requireNonNullElseGet(T obj, Supplier <? extends T > supplier) {
    return (obj != null) ? obj : requireNonNull(requireNonNull(supplier, "supplier").get(), "supplier.get()");
}
```



在调用时传入一个对象，和一个Supplier的实现类；如果传入的对象是null则调用Supplier的get方法，非空则调用其toString方法
尖括号里的类型要与传入对象一样

```java
String s = null;
String tips = "这是空的";

System.out.println(Objects.requireNonNullElseGet(s, new Supplier <String> () {
    @Override
    public String get() {
        // TODO Auto-generated method stub
        return tips;
    }
}));
```





## Objects - 判断两个对象是否相等

有两个equals方法，一个是equals，另一个是deepEquals，这两个都可以比较传入的任意类型的数据是否相等，返回值均为boolean类型

Objects中的两个equals方法有一个特别的地方，就是它可以比较值为null的两个对象其他的equals方法在调用者的值为null时就直接报错了

### equals

首先Objects中的equals源码为：
Objects中的equals首先就判断传进来的参数是否同一个对象；不是同一个对象判断第一个参数是否为null，若不为空则调用该参数的equals方法去比较。

```java
public static boolean equals(Object a, Object b) {
    return (a == b) || (a != null && a.equals(b));
}
```

举例：

```java
String s1 = null;
String s2 = null;
boolean result = Objects.equals(s1, s2);
System.out.println(result);
System.out.println(s1.equals(s2));
        
结果为：true
Exception in thread "main" java.lang.NullPointerException
	at test.ObjectsTest.main(ObjectsTest.java:12)
```

再看一个例子：

```java
String[] s1 = {"q", "w", "e"};
String[] s2 = {"q", "w", "e"};
String[] s3 = {"a", "s", "d"};
System.out.println(Objects.equals(s1, s2));//false
System.out.println(Objects.equals(s1, s3));//false
```

因为数组是属于引用类型的变量，在进行比较时比较的是其内存地址，s1和s2虽然值一样，但是
内存地址不一样，所以为false



### deepEquals

deepEquals字面意思就是比equlas进行更深度的比较，还是上面的例子，只是将equals换成了deepEquals

```java
String[] s1 = {"q", "w", "e"};
String[] s2 = {"q", "w", "e"};
String[] s3 = {"a", "s", "d"};
System.out.println(Objects.deepEquals(s1, s2));//true
System.out.println(Objects.deepEquals(s1, s3));//false
```

源码：

```java
public static boolean deepEquals(Object a, Object b) {
    if (a == b)
        return true;
    else if (a == null || b == null)
        return false;
    else
        //根据传入的类型进行比较，如果是数组则会一个个进行比较
        return Arrays.deepEquals0(a, b);
}
```



1. 这两种方法的区别就在于deepEquals是比较传入对象的值完全一致才会返回ture
2. 所以在比较类类型，数组等引用类型时用deepEquals比较好，当然也可以选择
3. 但其实重写equalse方法会更符合要求

> Arrays的相关源码可看下一篇文章



## Optional 

`Optional<T>` 类(java.util.Optional) 是一个容器类，代表一个值存在或不存在，原来用 null 表示一个值不存在，现在 Optional 可以更好的表达这个概念，并且可以避免空指针异常。主要思想其实是为了告知调用者，这个方法可能会返回一个空值，需要进行判断。

在 Java 8 之前，任何访问对象方法或属性的调用都可能导致 NullPointerException,如下面这段代码

```java
String isocode = user.getAddress().getCountry().getIsocode().toUpperCase();
```

如果要确保上面的代码不触发异常，就得在访问每一个值之前对其进行明确地检查：

```java
if (user != null) {
    Address address = user.getAddress();
    if (address != null) {
        Country country = address.getCountry();
        if (country != null) {
            String isocode = country.getIsocode();
            if (isocode != null) {
                isocode = isocode.toUpperCase();
            }
        }
    }
}
```

上面这段代码显得很冗长，难以维护。为了简化这个过程，我们来看看用 Optional 类是怎么做的

### 创建 Optional 实例

重申一下，这个类型的对象可能包含值，也可能为空。可以使用同名方法创建一个空的 Optional。

```java
public void test1() {
    //Optional.empty()返回的是一个Optional类的常量Optional<?> EMPTY = new Optional<>();
    Optional<User> emptyOpt = Optional.empty();
    emptyOpt.get();
}
```

毫不奇怪，emptyOpt不会为null，但是如果尝试访问 emptyOpt 变量的值会导致 NoSuchElementException。



可以使用 of() 创建包含值的 Optional

```java
public void test2() {
    Optional<User> opt = Optional.of(user);
}
```

源码如下：可以看到如果传入了null，则of() 方法会抛出 NullPointerException：

```java
public static < T > Optional < T > of(T value) {
    return new Optional < > (value);
}

private Optional(T value) {
    //调用了Objects.requireNonNull方法，
    this.value = Objects.requireNonNull(value);
}
```

看到这里，好像并没有完全摆脱 NullPointerException？也就是说，在明确对象不为 null 的时候才能使用 of()。



也就是说，如果对象即可能是 null 也可能是非 null，就应该使用 ofNullable() 方法：

```java
Optional<User> opt = Optional.ofNullable(user);
```

源码：

```java
public static < T > Optional < T > ofNullable(T value) {
    //不为null才调用 of 方法，否则返回Optional类的常量Optional<?> EMPTY = new Optional<>();
    return value == null ? empty() : of(value);
}
```



### 访问 Optional 对象的值

#### get() 方法

从 Optional 实例中取回实际值对象的方法之一是使用 get() 方法：

```java
public void test3(){
    String name = "seven";
    Optional<String> opt = Optional.ofNullable(name);

    assertEquals("seven", opt.get());
}
```

但是这个方法会在值为 null 的时候抛出 NoSuchElementException 异常。要避免异常，可以选择首先验证是否有值：

```java
public void test4() {
    User user = new User("seven97@qq.com", "1234");
    Optional<User> opt = Optional.ofNullable(user);
    assertTrue(opt.isPresent());
    assertEquals(user.getEmail(), opt.get().getEmail());
}
```

isPresent()方法很简单，就是检查下 opt.value 是否为 null



检查是否有值的另一个选择是 ifPresent() 方法。

```java
opt.ifPresent( u -> assertEquals(user.getEmail(), u.getEmail()));
```

该方法除了执行检查，还接受一个Consumer 参数，如果对象不是空的，就执行传入的 Lambda 表达式

```java
public void ifPresent(Consumer <? super T > action) {
    if (value != null) {
        action.accept(value);
    }
}
```

这个例子中，只有 user 用户不为 null 的时候才会执行断言。



#### orElse()

见名知意，这个方法表示  如果有值则返回该值，否则返回传递给它的参数值：

```java
public void test5() {
    User user = null;
    User user2 = new User("seven97@qq.com", "1234");
    User result = Optional.ofNullable(user).orElse(user2);

    assertEquals(user2.getEmail(), result.getEmail());
}
```

这里 user 对象是空的，所以返回了作为默认值的 user2。如果对象的初始值不是 null，那么默认值会被忽略

源码：

```java
public T orElse(T other) {
    return value != null ? value : other;
}
```



####  orElseGet()

这个方法会在有值的时候返回值，如果没有值，它会执行作为参数传入的 Supplier函数式接口，并返回其执行结果：

```
User result = Optional.ofNullable(user).orElseGet( () -> user2);
```

源码：

```java
public T orElseGet(Supplier <? extends T > supplier) {
    return value != null ? value : supplier.get();
}
```





#### orElse() 和 orElseGet() 的区别

乍一看，这两种方法似乎起着同样的作用。然而事实并非如此。创建一些示例来突出二者行为上的异同。

先来看看对象为空时他们的行为：

```java
public void test6() {
    User user = null
    logger.debug("Using orElse");
    User result = Optional.ofNullable(user).orElse(createNewUser());
    logger.debug("Using orElseGet");
    User result2 = Optional.ofNullable(user).orElseGet(() -> createNewUser());
}

private User createNewUser() {
    logger.debug("Creating New User");
    return new User("seven97@qq.com", "1234");
}
```

上面的代码中，两种方法都调用了 createNewUser() 方法，会记录一个消息并返回 User 对象。代码输出如下：

```java
Using orElse
Creating New User
Using orElseGet
Creating New User
```

由此可见，当对象为空而返回默认对象时，行为并无差异。

接下来看一个类似的示例，但这里 Optional 不为空：

```java
public void test7() {
    User user = new User("seven97@qq.com", "1234");
    logger.info("Using orElse");
    User result = Optional.ofNullable(user).orElse(createNewUser());
    logger.info("Using orElseGet");
    User result2 = Optional.ofNullable(user).orElseGet(() -> createNewUser());
}
```

这个示例中，两个 Optional 对象都包含非空值，两个方法都会返回对应的非空值。不过，orElse() 方法仍然创建了 User 对象。与之相反，orElseGet() 方法不创建 User 对象。

这是因为使用Supplier能够做到 惰性计算，即 使用orElseGet时，只有在需要的时候才会计算结果。具体到我们的场景，使用orElse的时候，每次它都会执行计算结果的过程，而对于orElseGet，只有Optional中的值为空时，它才会计算备选结果。这样做的好处是可以避免提前计算结果的风险。

显然，在执行较密集的调用时，比如调用 Web 服务或数据查询，这个差异会对性能产生重大影响。









<!-- @include: @article-footer.snippet.md -->     







