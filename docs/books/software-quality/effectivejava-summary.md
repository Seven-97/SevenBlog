---
title: EffectiveJava总结
category: 计算机书籍
---



## 创建和销毁对象

### 1、静态工厂方法代替构造器

**优点**

- 静态工厂方法有名称，能确切地描述正被返回的对象。

- 不必每次调用都创建一个新的对象。

- 可以返回原返回类型的任何子类对象。

- 创建参数化类型实例时更加简洁，比如调用构造 HashMap 时，使用 Map < String,List < String > m = HashMap.newInstance() ，与 Map < String,List < String > m > = new HashMap < String,List < String > >(); 

 

缺点

- 没有公共或受保护构造方法的类不能被子类化

- 不像构造方法一样容易被找到

 

### 2、遇到多个构造参数时要考虑用建造者模式

其它方式的缺点

- 静态工厂和构造器不能很好地扩展到大量的可选参数。

- JavaBean 模式下使用 setter 来设置各个参数，无法仅通过检验构造器参数的有效性来保证一致性，会试图使用不一致状态的对象。

 

Builder 的建造者模式：使用必须的参数调用构造器，得到一个 Builder 对象，再在 builder 对象上调用类似 setter 的方法设置各个可选参数，最后调用无参的 build 方法生成不可变对象，new Instance.Builder(必须参数).setter(可选参数).build()。

[建造者模式详解](https://www.seven97.top/system-design/design-pattern/creationalpattern.html#%E5%BB%BA%E9%80%A0%E8%80%85%E6%A8%A1%E5%BC%8F)

Builder 模式让类的创建和表示分离，使得相同的创建过程可以创建不同的表示。

```java
public class Computer {
    private final String cpu;//必须
    private final String ram;//必须
    private final int usbCount;//可选
    private final String keyboard;//可选
    private final String display;//可选

    private Computer(Builder builder){
        this.cpu=builder.cpu;
        this.ram=builder.ram;
        this.usbCount=builder.usbCount;
        this.keyboard=builder.keyboard;
        this.display=builder.display;
    }
    public static class Builder{
        private String cpu;//必须
        private String ram;//必须
        private int usbCount;//可选
        private String keyboard;//可选
        private String display;//可选

        public Builder(String cup,String ram){
            this.cpu=cup;
            this.ram=ram;
        }

        public Builder setUsbCount(int usbCount) {
            this.usbCount = usbCount;
            return this;
        }
        public Builder setKeyboard(String keyboard) {
            this.keyboard = keyboard;
            return this;
        }
        public Builder setDisplay(String display) {
            this.display = display;
            return this;
        }        
        public Computer build(){
            return new Computer(this);
        }
    }
  //省略getter方法
}

// 使用：
Computer computer=new Computer.Builder("因特尔","三星")
                .setDisplay("三星24寸")
                .setKeyboard("罗技")
                .setUsbCount(2)
                .build();
```



但是，建造者模式也有缺点：

- 为了创建对象，首先得它的创建Builder，在性能关键的情况下可能会出现问题

- Builder模式比伸缩构造方法模式更冗长，因此只有在足够参数的情况下才值得使用，比如四个以上

 

### 3、使用私有构造方法或枚举实现Singleton属性

Singleton指最多会被实例化一次的类。通常情况下，以前的做法是没有问题的。但是在某些高级情况，通过使用反射的相关知识访问private的构造函数，破坏Singleton。

```java
public class Elvis{
    // 注意，公有final对象
    public static final Elvis INSTANCE = new Elvis();
    private Elvis(){}
}
```

另一种情况，在序列化的过程中，反序列化得到的对象已经不再是以前的对象（破坏了Singleton），这种情况下，可以通过单元素枚举型处理。

```java
public enum Elvis{
    INSTANCE;
    // some methods
}
```

[单例模式](https://www.seven97.top/system-design/design-pattern/singletonpattern.html) 详解看这篇文章

 

### 4、使用私有构造方法执行非实例化

有一些工具类，仅仅是提供一些能力，自己本身不具备任何属性，所以，不适合提供构造函数。然而，缺失构造函数编译器会自动添加上一个无参的构造器。所以，需要提供一个私有化的构造函数。为了防止在类内部误用，再加上一个保护措施和注释。

```java
public class Util{
    private Util(){
        // 抛出异常，防止内部误调用
        throw new AssertionError();
    }
}
```

弊端是无法对该类进行继承（子类会调用super()）。

 

### 5、依赖注入优于硬链接资源

**硬连接资源：**

上面使用的方式都是硬连接（硬编码）；什么是硬连接，我在此处的理解是在代码中直接引用某个固定的资源。但是如果一个类依赖多种资源呢？以下面的例子为例，引用的字典资源可能是中文字典，也可能是英文字典，每次资源发生变化时都需要在代码中去修改，这样的方式显然不够灵活。

 

许多类依赖于一个或多个底层资源。例如，拼写检查器依赖于字典。

通过静态方法来替代硬资源连接 :

```java
// Inappropriate use of static utility - inflexible & untestable!
public class SpellChecker {
    //仅提供了一个资源
    private static final Lexicon dictionary = "依赖的资源";
    
     //私有构造方法，非实例化
    private SpellChecker() {} // Noninstantiable
    
     //拼音检查器提供给外部类调用的方法
    public static boolean isValid(String word) { ... }
    public static List<String> suggestions(String typo) { ... }
}
```

使用单例来替代硬连接资源，同样也是不灵活的：

```java
// Inappropriate use of singleton - inflexible & untestable!
public class SpellChecker {
    private final Lexicon dictionary = ...;

    private SpellChecker(...) {}
    public static INSTANCE = new SpellChecker(...);

    public boolean isValid(String word) { ... }
    public List<String> suggestions(String typo) { ... }
}
```

也可以在类需要字典资源的时候再去指定，例如将属性设置为非 final，再提供一个方法去修改资源，但这样的设置显得非常笨拙、容易出错、并且无法并行工作。静态工具类和单例类不适合与需要引用底层资源的类。

 

**依赖注入：**

使用依赖注入替代硬连接资源，依赖注入提供了灵活性和可测试性。虽然下方的实例仍是一个基础资源，但是它由外部类提供，这样保证了我们的SepllChecker提供的功能不变，但是数据却是灵活多变的。

```java
public class SpellChecker03 {

    private final Lexicon dictionary;

    //需要检查什么资源，由外部类提供基础的数据
    public SpellChecker03(Lexicon dictionary) {
        this.dictionary = Objects.requireNonNull(dictionary);
    }

    public boolean isValid(String word) { ... }
}
```

依赖注入模式非常简单，许多程序员使用它多年而不知道它有一个名字。 虽然拼写检查器的例子只有一个资源（字典），但是依赖项注入可以使用任意数量的资源和任意依赖图。 它保持了不变性（详见第 17 条），因此多个客户端可以共享依赖对象（假设客户需要相同的底层资源）。 依赖注入同样适用于构造器、静态工厂

 

但是使用依赖注入来实现硬资源连接，也有一定的弊端，一般大型的项目，可能有数千个依赖项，这让项目变得混乱，不过可以使用Spring框架来消除这些混乱。依赖注入，它极大地提升了类的灵活性、可重用性和可测试性。

 

### 6、避免创建不必要的对象

- 对于 String 类型，String s = new String("") 每次执行时都会创建一个新的实例，而使用 String s = "" 则不会，因为对于虚拟机而言，包含相同的字符串字面常量会重用，而不是每次执行时都创建一个新的实例。

- 重用相同功能的对象：

  - 重用那些已知不会被修改的可变对象，如Date 、Calendar

  - 可变对象的重用：比如，Map的keySet()方法就会返回Map对象所有key的Set视图。这个视图是可变的，但是当Map对象不变时，在任何地方返回的任何一个keySet都是一样的，当Map对象改变时，所有的keySet也会相应的发生改变。

- 小心自动装箱（auto boxing）：优先使用基本类型，避免无意识的自动装箱。

- 对于廉价的对象，慎用对象池。现代JVM对廉价对象的创建和销毁非常快，此时不适于使用对象池。

- 用静态工厂方法而不是构造器

 

### 7、消除过期的对象引用

以下三种情况可能会造成内存泄露：

- 自己管理的内存（数组长度减小后，pop出的对象容易导致内存泄漏）

- 缓存

- 监听和回调

 

自己管理的内存：

```java
public class Stack {
     // …… 
     
    public Object pop() {
        if (size == 0) {
            throw new EmptyStackException();
        }

        return elements[--size];
    }
    

    // …… 
}
```

这个程序在发生内存泄露：

```java
return elements[--size];
```



如果一个栈是先增长再收缩，那么从栈中弹出来的对象将不会被GC回收，即使使用栈的程序不会在引用这些对象，这是因为，栈会维持着elements[size]的过期引用，即永远也不会被解除的引用。

应该要这样修改：

```java
public Object pop(){
    if(size == 0){
        throw new EmptyStackException();
    }
    Object result = elements[--size];
    //消除引用
    elements[size] = null;
    return result;
}
```



缓存：

缓存的对象容易被程序员遗忘，需要设置机制来维护缓存，例如不定期回收不再使用的缓存（使用定时器）。某些情况下，使用WeakHashMap可以达到缓存回收的功效。注，只有缓存依赖于外部环境，而不是依赖于值时，WeakHashMap才有效。

 

监听或回调：

使用监听和回调要记住取消注册。确保回收的最好的实现是使用弱引用（weak reference），例如，只将他们保存成WeakHashMap的键。

 

### 8、避免使用 Finalizer和Cleaner 机制

Java的GC有强大的回收机制，可以简单的记住：不要显示调用finalizer。可以这样理解：

JVM是针对具体的硬件设计的，然而程序却不是针对具体硬件设计的，所以，Java代码无法很好的解决GC问题（因为他具有平台差异化）。另外，finalizer的性能开销也非常大，从这个角度上考虑也不应该使用它。

 

Java 9中，Finalizer 机制已被弃用，但仍被 Java 类库所使用。 Java9中 Cleaner 机制代替了 Finalizer 机制。

Cleaner 机制不如 Finalizer 机制那样危险，但仍然是不可预测，运行缓慢，通常是不必要的。

 

## 对于所有对象都通用的方法

### 10、覆盖 equals要遵守通用约定

当重写equals方法时，必须遵守以下约定

- 自反性：对于任何非空引用x。x.equals(x) 必须返回 true

- 对称性：对于任何非空引用x，y。当且仅当y.equals(x) 为 true时，x.equals(y) 也必须为 true

- 传递性。对于任何非空引用x，y，z。如果 (x.equals(y)&&y.equals(z)) 为 true，那么y.equals(z) 也为 true

- 一致性：对于任何非空引用x，y。如果在equals中比较的信息没有修改，那么 x.equals(y) 的调用必须始终为true或始终为 false

- 非空性：对于任何非空引用x。x.equals(null) 必须返回 false

 

编写高质量equals方法：

- 使用 == 操作符检查”参数是否为这个对象的引用“。

- 使用 instanceof 操作符检查“参数是否为正确的类型”。

- 把参数转换成正确的类型。因为转换操作在 instanceof 中已经处理过，所以肯定会成功。

- 对于该类中的每个关键域，检查参数中的域是否与该对象中对应的域相匹配。

  - 域可能为基本数据类型，非float和double类型，使用==来进行比较

  - 可能为引用类型，递归调用equals方法

  - 对于float类型，使用Float.compare(float,float)处理，对于double类型，使用Double.compare(double,double)处理，但会导致自动装箱，消耗性能。并且由于存在Float.NaN，因此需要额外处理

- 由于可能存在null，因此使用Object.equals(Object,Object)来检查是否相等

 

其他提醒：

- 当重写 equals 方法时，同时也要重写 hashCode 方法

- 不要让 equals 方法试图太聪明。如果只是简单地测试用于相等的属性，那么要遵守 equals 约定并不困难。

- 在 equal 时方法声明中，不要将参数 Object 替换成其他类型，否则只是重载了方法。应使用@Override注解来避免犯这个错误

 

### 11、覆盖 equals 方法时，总要覆盖 hashCode 方法

当覆盖equals方法时，必须要覆盖hashCode方法。因为相等的对象必须要有相同的哈希码。如果没有一起去覆盖 hashcode，则会导致俩个相等的对象未必有相等的散列码，造成该类无法结合所有基于散列的集合一起工作。

详情请关注 [equals和hashcode](https://www.seven97.top/java/basis/01-basic-knowledge.html#equals%E5%92%8Chashcode)

 

### 12、始终重写 toString 方法

Object 所提供的 toString，是该对象的描述，输出的是 【类名@散列码】的格式。因此，为了更容易阅读，应该重写 toString 方法

 

注意：

- 重写toString方法时，如果有格式，则应该严格按照格式返回。

- 自己覆盖的 toString，返回对象中包含的所有值得关注的信息。

- 在静态工具类 和 枚举类 中重写 toString 方法是没有意义的

- 如果是抽象类，应该重写 toString 方法来指定格式。如集合实现的toString 方法都是从抽象集合类中继承的/

```java
// java.util.AbstractCollection#toString
public String toString() {
    Iterator<E> it = iterator();
    if (! it.hasNext())
        return "[]";

    StringBuilder sb = new StringBuilder();
    sb.append('[');
    for (;;) {
        E e = it.next();
        sb.append(e == this ? "(this Collection)" : e);
        if (! it.hasNext())
            return sb.append(']').toString();
        sb.append(',').append(' ');
    }
}
```



不足：

- 当类被广泛使用，一旦指定格式，那就会编写出相应的代码来解析这种字符串表示法，以及把字符串表示法嵌入持久化数据中，之后如果想改变这种格式，则会遭到破坏。

 

### 13、谨慎重写 clone 方法

调用clone()方法需要对象实现Cloneable接口，但是这个接口有着许多缺陷。最主要的缺陷在于，Cloneable接口中不包含任何方法，而Object中的clone方法是protect的，也就是说如果一个类只是继承了Cloneable接口，但是却没有重写clone()方法，那么对于这个类的对象，clone()方法依然是不可用的。

既然Cloneable接口中没有任何方法，那么它有什么作用呢，可以看看Object中的clone()方法：

```java
protected Object clone() throws CloneNotSupportedException {
    if (!(this instanceof Cloneable)) {
        throw new CloneNotSupportedException("Class " + getClass().getName() +" doesn't implement Cloneable");
    }
    return internalClone();
}
```



从上面的代码中可以看出Cloneable接口实际上决定了clone()方法的行为，按书中所说：“这是接口的一种极端非典型的用法，并不值得仿效。”

也就是说如果clone()方法想要实现预想中的效果，就需要遵守一个“相当复杂的，不可实施的，并且基本上没有文档说明的协议”。

但是即使是java.lang.Object规范中的约定内容中也存在着许多问题，例如：“不调用构造器”，这一条规定太过强硬，因为行为良好的clone()方法可以调用构造器来创建对象，构造之后在复制内部数据。

在最理想的状况下，如果需要使用clone方法，只需要实现Cloneable接口，并且重写clone()方法即可：

```java
@Override
public Object clone(){
    try{
        return super.clone();
    }catch(CloneNotSupportedException e){
        throw new AssertionError();
    }
}
```

但是这种方法只适用于需要浅拷贝的情况。但如果这么做，而且拷贝的类中存在可变的对象，就会导致灾难性的结果。

 

例如希望通过上面的方式clone一个如下的Stack：

```java
public class Stack{
    private Object[] elements;
    private int size = 0;
    //other......
}
```



这样clone出来的Stack对象与原Stack对象却使用的是同一个elements引用，但是size属性却是相互独立的。这样很快就会发现这两个Stack对象已经无法正常工作。

 

对于这种情况，可以通过深拷贝来避免这种情况的发生，例如上面的Stack类，虽简单的做法就是在elements数组中递归地调用clone：

```java
@Override
public Stack clone(){
    try{
        Stack result = (Stack) super.clone();
        result.elements = elements.clone();
        return result;
    }catch(CloneNotSupportedException e){
        throw new AssertionError();
    }
}
```



克隆复杂对象还有一个方法：

先调用super.clone()，然后把返回对象中的所有域都设为空白状态，再将原对象中各个域的值赋给克隆对象中的相应部分，但是它运行起来通常没有“直接操作对象及其克隆对象的内部状态的clone方法”快。

 

正是因为 Cloneable 接口有着这么多的问题，可以肯定的说，其他接口都不应该扩展此接口；那些为了继承而设计的类也不应该实现此接口。

 

因此应使用其它方式来克隆对象：

- 使用序列化和反序列化实现深拷贝，但要注意，被克隆的对象及其所有属性都必须是可序列化的。此外，序列化和反序列化过程中可能会抛出 IOException 和 ClassNotFoundException 异常，需要进行处理。

- 使用第三方工具，例如 Apache Commons BeanUtils 库的 BeanUtils.cloneBean() 方法和 Spring Framework 的 ObjectUtils.clone() 方法。

 

### 14、考虑实现 Comparable 接口

如果类实现了comparable 接口，便可以跟许多泛型算法以及依赖该接口的集合实现协作，比如可以使用 Array.sort 等集合的排序。

如果是具有明显自然顺序(如字母顺序，数字顺序或时间顺序)的值类，则应该实现Comparable 接囗

 

排序的另一种实现方式是实现Comparator接口，性能大概比Comparable慢10%，例如：

```java
// Comparable with comparator construction methods
private static final Comparator<PhoneNumber> COMPARATOR = comparingInt((PhoneNumber pn) -> pn.areaCode)
    .thenComparingInt(pn -> pn.prefix)
    .thenComparingInt(pn -> pn.lineNum);

public int compareTo(PhoneNumber pn) {
    return COMPARATOR.compare(this, pn);
}
```



有时人们会用计算差值的方法来实现compare方法，例如：

```java
// BROKEN difference-based comparator - violates transitivity!
class DateWrap{
    Date date;
}

static Comparator<DateWrap> order = new Comparator<>() {
    public int compare(DateWrap d1, DateWrap d2) {
        return d1.date.getTime() - d2.date.getTme();
    }
};
```



但是这种做法是有缺陷的。在上例中，Date.getTime返回long型时间戳，两者的差值有可能溢出int。所以应该用Date类自带的比较方法：

```java
// Comparator based on static compare method s
static Comparator<DateWrap> order = new Comparator<>() {
    public int compare(DateWrap d1, DateWrap d2) {
        return d1.date.compareTo(d2.date);
    }
};
```



 

## 类和接口

### 15、使类和成员的可访问性最小化

封装和信息隐藏是软件设计的基本原则。 Java的访问控制机制指定了类、接口和成员的可访问性。它们的可访问性由其声明的位置以及声明中出现的访问修饰符（private、protected 和 public）决定。

控制可访问级别的最佳实践是：在不影响软件功能的前提下，让每个类或成员尽可能不可访问。

对于顶级（非嵌套）类和接口，只有两个可能的访问级别：包级和公共。使用哪个级别，取决于是否需要将API对外导出。

如果包级顶级类或接口只被一个类使用，那么可以考虑在使用它的这个类中，将顶级类设置为私有静态嵌套类。

对于成员（字段、方法、嵌套类和嵌套接口），存在四种访问级别，按可访问性从低到高分别是：

1. 私有（private）：成员只能从声明它的顶级类访问。
2. 包级（package-private）：成员可以从声明它的类所在的包访问。不加修饰符时默认的访问级别。
3. 保护（protected）：成员可以从声明它的类的子类和声明它的类所在的包访问。
4. 公共（public）：成员可以从任意地方访问。

如果一个方法覆盖了超类方法，那么它在子类中的访问级别就不能比超类更严格。

公共类的实例字段很少用public修饰，除非是静态final常量。带有公共可变字段的类通常不是线程安全的。

Java 9的模块系统引入了两个额外的隐式访问级别。模块是包的分组，它可以显式导出一些包，未导出包的公共类的公共和保护成员在模块外不可访问，它们产生了两个隐式访问级别，即模块内公共和模块内保护。

 

### 16、在公共类中，使用访问器方法，而不是公共字段

对于公共类中的可变字段，不应该将它们设为公共，因为这样破坏了类的封装性。应该通过setter、getter方法访问。对于公共类中的不可变字段，设为公共的危害要小一些。

对于包级类或私有嵌套类，公开它们的字段是允许的。因为这两种类的访问受到限制，所以对它们字段的更改也能限制在一定范围内。

 

### 17、最小化可变性

应该尽量降低类的可变性。

不可变类是实例不能被修改的类。Java中有很多不可变类，如String、Integer等。不可变类的优点是：简单、线程安全，可作为缓存共享。

不可变类需满足以下5条规则：

1. 不要提供修改状态的方法。
2. 确保类不能被继承。可以通过为类加上final修饰符，或者通过静态工厂方法对外提供创建对象的唯一方法。
3. 所有字段用final修饰。
4. 所有字段设为私有。
5. 确保对可变对象引用的独占访问。不要给用户提供访问这些引用的方法。

不可变类的缺点是每个不同的值都需要一个单独的对象。这样会产生很多对象创建和回收的开销。解决办法是提供一个公共可变伴随类。例如String的公共可变伴随类就是StringBuilder，用后者处理多个字符串的拼接时可以减少对象创建数量。

对于那么无法做到不可变的类，应尽量限制它的可变性。这样可以减少出错的可能性。每个字段如果可以，尽量设为私有final的。

 

### 18、组合优于继承

继承并不适合于所有场合。在同一个包中使用继承是安全的，对专为扩展而设计的类使用继承也是安全的。但对普通的具体类做跨包的继承是危险的。

继承打破了封装性，除非超类是专门为了扩展而设计的。超类若在后续的发行版本中获得新的方法，并且其子类覆盖超类中与新方法有关的方法，则可能会发生错误。

组合：在新的类中增加一个私有域，引用现有类。它不依赖现有类的实现细节，对现有类进行转发。

两个类只有满足is-a关系时才应该建立继承关系。Java库中有很多违反这一原则的地方，如Stack不是Vector却继承了后者，Properties不是HashTable也继承了后者。这些情况本应该使用组合。

 

### 19、继承要设计良好并且具有文档，否则禁止使用

为了避免继承影响子类实现的正确性，需要为可覆盖方法提供详细的文档，说明它的实现细节，以及覆盖它产生的影响。

这看上去违反了一条准则：好的API文档应该描述一个方法做什么，而不是如何做。确实，这是继承违反封装导致的后果。

 

### 20、接口优于抽象类

接口相对抽象类的优点是：

1. 一个类只能继承单个抽象类，却能实现多个接口。
2. 接口的使用更加灵活，可以很容易对现有类进行改造，实现新的接口。
3. 接口允许构造非层次化类型结构。

为了代码复用，可以为接口提供默认方法。但是默认方法有不少限制，例如编译器会阻止你提供一个与Object类中的方法重复的默认方法，而且接口不允许包含实例字段或非公共静态成员（私有静态方法除外）。

这时可以实现一个抽象骨架类来结合抽象类和接口的优点。例如Java类库中的AbstractList就是典型的抽象骨架类。抽象骨架类使用了设计模式中的模板模式。

 

### 21、为后代设计接口

在Java 8之前，向接口添加方法会导致现有的实现出现编译错误，影响版本兼容性。为了解决这个问题，在Java 8中添加了默认方法的功能，允许向现有接口添加方法，但是这个添加过程存在很大风险。

由于默认方法被注入到已有实现的过程对实现者是透明的，实现者无需对此做任何反应。但是有时很难为所有的实现提供一个通用的默认方法，提供的默认方法很可能在某些场合是错误的。

下面的例子是Java 8 中被添加到集合接口中的 removeIf 方法：

```java
// Default method added to the Collection interface in Java 8
default boolean removeif(predicate<? super e> filter) {
    objects.requirenonnull(filter);
    boolean result = false;
    for (iterator<e> it = iterator(); it.hasnext(); ) {
        if (filter.test(it.next())) {
            it.remove();
            result = true;
        }
    }
    return result;
}
```



很遗憾，它在实际使用的一些 Collection 实现中导致了问题。例如，考虑 org.apache.commons.collections4.collection.SynchronizedCollection。这个类提供了Java Collection的同步实现版本，但由于继承了removeIf的默认实现，新增了一个非同步方法，与这个类的设计初衷不符。

总结下来，设计接口前应该三思，应仔细考虑接口应包含的方法集合。虽然默认方法可以做到接口发布后新增方法，但是你不能依赖这种有很大风险的方式。

笔者注：这个观点需要辩证理解。本书作者作为Java类库的设计者之一，他的很多观点都来自他在设计这些类库时的经验总结。对于像基础Java类库或是使用广泛的开源项目，同时需要保证严格的版本兼容性的，接口发布后新增默认方法确实会带来很大的风险。但是对于广大业务开发者来说，如果接口只是局限在自己的业务代码中，所有引用都能用IDE反向查找到，那么风险是可控的。

 

### 22、接口只用于定义类型

接口只应该用来定义类型，不要用来导出常量。

常量接口时对接口的不良使用。实现常量接口，会导致把这样的实现细节泄漏给该类的导出 API 中，当类不再需要这些常量时，还必须实现这个接口以确保兼容性。如果非final类实现了该常量接口，它的所有子类的命名空间都将被接口中的常量污染。

想要导出常量，可以把它们放在相关的类中，如Integer类中的MAX_VALUE；或者定义一个XXXConstants类来存放一组相关的常量。

 

### 23、类层次结构优于带标签的类

对于有两种或两种以上的样式的类，我们有时会定义一个标签字段来表示不同的样式。例如，下面的类能够表示一个圆或一个矩形：

```java
// Tagged class - vastly inferior to a class hierarchy!
class Figure {
    enum Shape {RECTANGLE, CIRCLE};

    // Tag field - the shape of this figure
    final Shape shape;

    // These fields are used only if shape is RECTANGLE
    double length;

    double width;

    // This field is used only if shape is CIRCLE
    double radius;

    // Constructor for circle
    Figure(double radius) {
        shape = Shape.CIRCLE;
        this.radius = radius;
    }

    // Constructor for rectangle
    Figure(double length, double width) {
        shape = Shape.RECTANGLE;
        this.length = length;
        this.width = width;
    }

    double area() {
        switch (shape) {
            case RECTANGLE:
                return length * width;
            case CIRCLE:
                return Math.PI * (radius * radius);
            default:
                throw new AssertionError(shape);
        }
    }
}
```



标签类的缺点是：不同的实现逻辑混杂在一起，可读性较低，容易出错。

标签类本质上是对类层次结构的模仿。所以还不如直接用类层次结构来编写，得到更加简单明了的代码：

```java
// Class hierarchy replacement for a tagged class
abstract class Figure {
    abstract double area();
}

class Circle extends Figure {
    final double radius;

    Circle(double radius) {
        this.radius = radius;
    }

    @Override
    double area() {
        return Math.PI * (radius * radius);
    }
}

class Rectangle extends Figure {
    final double length;
    final double width;

    Rectangle(double length, double width) {
        this.length = length;
        this.width = width;
    }

    @Override
    double area() {
        return length * width;
    }
}
```



类层次结构的另一个优点是，可以反映类型之间的自然层次关系，而这是标签类做不到的。下面例子表示正方形是一种特殊的矩形：

```java
class Square extends Rectangle {
  Square(double side) {
    super(side, side);
  }
}
```



 

### 24、静态成员类优于非静态成员类

嵌套类共有四种：静态成员类、非静态成员类、匿名类和局部类。它们各自有不同的适用场合。判断方法见如下流程图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405022252621.png)

 

每种嵌套类的常见例子总结如下：

1. 静态成员类：作为公共的辅助类，如Map中的Entry类。
2. 非静态成员类：Map的entrySet、keySet方法返回的视图，List、Set中的迭代器。
3. 匿名类：新建Comparable、Runnable接口实现类，可用lambda表达式替代。
4. 局部类：与匿名类的区别仅为有名字，可重复使用。

如果成员类不需要访问外部实例，那么始终应该设置其为静态的。因为非静态的成员类会持有对外部类的引用，增加时间空间代价，而且会影响对外部类的垃圾回收。

 

### 25、源文件仅限有单个顶层类

虽然Java编译器允许在单个源文件中定义多个顶层类，但是这样做没有任何好处，反而存在重大风险。请看下面的例子，这个源文件引用了另外两个顶层类（Utensil 和 Dessert）：

```java
public class Main {
    public static void main(String[] args) {
        System.out.println(Utensil.NAME + Dessert.NAME);
    }
}
```



然后你在一个名为 Utensil.java 的源文件中提供了 Utensil 类和 Dessert 类的实现：

```java
// Two classes defined in one file. Don't ever do this!
class Utensil {
    static final String NAME = "pan";
}

class Dessert {
    static final String NAME = "cake";
}
```



不料在你不知情的情况下，另一个人在名为 Dessert.java的源文件中定义了相同的两个类：

```java
// Two classes defined in one file. Don't ever do this!
class Utensil {
    static final String NAME = "pot";
}

class Dessert {
    static final String NAME = "pie";
}
```



如果你使用 javac Main.java Dessert.java 命令编译程序，编译将失败，编译器将告诉你多重定义了 Utensil 和 Dessert。这是因为编译器将首先编译 Main.java，当它看到对 Utensil 的引用（在对 Dessert 的引用之前）时，它将在 Utensil.java 中查找这个类，并找到餐具和甜点。当编译器在命令行上遇到 Dessert.java 时，（编译器）也会载入该文件，导致（编译器）同时遇到 Utensil 和 Dessert 的定义。

如果你使用命令 javac Main.java 或 javac Main.java Utensil.java 编译程序，它将按我们的预期打印出pancake。但是如果你使用命令 javac Dessert.java Main.java 编译程序，它将按别人的实现打印出 potpie。因此，程序的行为受到源文件传递给编译器的顺序的影响，这显然是不可接受的。

要避免这种问题，只需将顶层类（Utensil和Dessert）分割到单独的源文件中，或者放弃将它们作为顶层类，转而使用静态成员类。如下所示：

```java
// Static member classes instead of multiple top-level classes
public class Test {

    public static void main(String[] args) {
        System.out.println(Utensil.NAME + Dessert.NAME);
    }

    private static class Utensil {
        static final String NAME = "pan";
    }

    private static class Dessert {
        static final String NAME = "cake";
    }
}
```



## 泛型

### 26、 不要使用原始类型

每个泛型都定义了一个原始类型，它是没有任何相关类型参数的泛型的名称。例如，List&lt;E> 对应的原始类型是 List。原始类型的行为就好像所有泛型信息都从类型声明中删除了一样。它们的存在主要是为了与之前的泛型代码兼容。

使用原始类型是类型不安全的，编译器会发出警告，而且容易出现类型相关的运行时异常。 如下面的程序：

```java
// Fails at runtime - unsafeAdd method uses a raw type (List)!

public static void main(String[] args) {
    List<String> strings = new ArrayList<>();
    unsafeAdd(strings, Integer.valueOf(42));
    String s = strings.get(0); // Has compiler-generated cast
}

private static void unsafeAdd(List list, Object o) {
    list.add(o);
}
```



因为使用了原始类型List，编译器会给出一个警告：

```java
Test.java:10: warning: [unchecked] unchecked call to add(E) as a
member of the raw type List
list.add(o);
^
```



然后运行程序时，会在将strings.get(0)` 的结果强制转换为字符串的地方抛出一个 ClassCastException。

如果你想使用泛型，但不知道或不关心实际的类型参数是什么，那么可以使用问号代替。例如，泛型集合 Set&lt;E> 的无界通配符类型是 Set<?>。它是最通用的参数化集合类型，能够容纳任何集合：

```java
// Uses unbounded wildcard type - typesafe and flexible
static int numElementsInCommon(Set<?> s1, Set<?> s2) { ... }
```



无界通配符类型和原始类型之间的区别是：前者中不能放入任何元素（除了 null）。如果你这样做，会得到一个编译报错：

```java
WildCard.java:13: error: incompatible types: String cannot be converted to CAP#1
c.add("verboten");
^ where CAP#1
is a fresh type-variable:
CAP#1 extends Object from capture of ?
```



为便于参考，本条目中介绍的术语（以及后面将要介绍的一些术语）总结如下：

| 术语           | 例子                                    | 条目           |
| -------------- | --------------------------------------- | -------------- |
| 参数化类型     | List&lt;String>                         | 第26条         |
| 实际的类型参数 | String                                  | 第26条         |
| 泛型类型       | List&lt;E>                              | 第26条、第29条 |
| 形式化类型参数 | E                                       | 第26条         |
| 无界泛型表达式 | List<?>                                 | 第26条         |
| 原始类型       | List                                    | 第26条         |
| 有界类型参数   | &lt;E  extends Number>                  | 第29条         |
| 递归类型限制   | <T  extends Comparable&lt;T>>           | 第30条         |
| 有界泛型表达式 | List<?  extends Number>                 | 第31条         |
| 泛型方法       | static  &lt;E> List&lt;E> asList(E[] a) | 第30条         |
| 类型记号       | String.class                            | 第33条         |

 

### 27、消除 unchecked 警告

 使用泛型编程时，很容易看到unchecked编译器警告。应该尽可能消除这些警告。消除所有这些警告后，就能确保代码是类型安全的。

有时unchecked警告很容易消除，例如下面不规范的代码会导致编译器警告：

```java
Set<Lark> exaltation = new HashSet();
```

可以修改一下，取消警告：

```java
Set<Lark> exaltation = new HashSet<>();
```

但有时警告无法消除，如果可以证明代码是类型安全的，可以通过SuppressWarnings("unchecked") 注解来抑制警告。

SuppressWarnings应该在尽可能小的范围内使用。如下例在一个变量上使用这个注解：

```java
// Adding local variable to reduce scope of @SuppressWarnings
public <T> T[] toArray(T[] a) {
    if (a.length < size) {
        // This cast is correct because the array we're creating
        // is of the same type as the one passed in, which is T[].
        @SuppressWarnings("unchecked") T[] result = (T[]) Arrays.copyOf(elements, size, a.getClass());
        return result;
    }
    System.arraycopy(elements, 0, a, 0, size);
    if (a.length > size)
        a[size] = null;
    return a;
}
```

每次使用注解时，要添加一条注释，说明这样做是安全的，以帮助他人理解代码。

 

### 28、列表优于数组

 使用泛型时，优先考虑用list，而非数组。

数组和泛型有两个重要区别，这让它们在一起工作不那么协调。

**区别一：数组是协变的，而泛型不是**。如果 Sub 是 Super 的子类型，那么类型 Sub[] 就是类型 Super[] 的子类型，而`List<Sub>`并非`List<Super>`的子类型。

例如，下面这段代码是合法的：

```java
// Fails at runtime!
Object[] objectArray = new Long[1];
objectArray[0] = "I don't fit in"; // Throws ArrayStoreException
```

但这一段代码就不是：

```java
// Won't compile!
List<Object> ol = new ArrayList<Long>(); // Incompatible types
ol.add("I don't fit in");
```

两种方法都不能将 String 放入 Long 容器，但使用数组，会得到一个运行时错误；使用 list，你可以在编译时发现问题。后者当然是更加安全的。



**区别二：数组是具体化的，而泛型通过擦除来实现**。这意味着，数组在运行时知道并强制执行他们的元素类型，而泛型只在编译时执行类型约束，在运行时丢弃类型信息，这样做是为了与不使用泛型的老代码兼容。

由于这些差异，数组和泛型不能很好地混合。例如，创建泛型、参数化类型或类型参数的数组是非法的。因此，这些数组创建表达式都是非法的：`new List<E>[]`、`new List<String>[]`、`new E[]`。所有这些行为都会在编译时报错，原因是它们并非类型安全。如果合法，那么类型错误可能延迟到运行时才出现，这违反了泛型系统的基本保证。

例如下面代码：

```java
// Why generic array creation is illegal - won't compile!
List<String>[] stringLists = new List<String>[1]; // (1)
List<Integer> intList = List.of(42); // (2)
Object[] objects = stringLists; // (3)
objects[0] = intList; // (4)
String s = stringLists[0].get(0); // (5)
```

如果第一行是合法的，那么会运行时到第5行才抛出运行时异常。

 

### 29、优先考虑泛型

应该尽量在自己编写的类型中使用泛型，这会保证类型安全，并使代码更易使用。

下面通过例子来看下如何对一个现有类做泛型化改造。



首先是一个简单的堆栈实现：

```java
// Object-based collection - a prime candidate for generics
public class Stack {
    private Object[] elements;
    private int size = 0;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    public Stack() {
        elements = new Object[DEFAULT_INITIAL_CAPACITY];
    }

    public void push(Object e) {
        ensureCapacity();
        elements[size++] = e;
    }

    public Object pop() {
        if (size == 0)
            throw new EmptyStackException();
        Object result = elements[--size];
        elements[size] = null; // Eliminate obsolete reference
        return result;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    private void ensureCapacity() {
        if (elements.length == size)
            elements = Arrays.copyOf(elements, 2 * size + 1);
    }
}
```

我们用适当的类型参数替换所有的 Object 类型，然后尝试编译修改后的程序：

```java
// Initial attempt to generify Stack - won't compile!
public class Stack<E> {
    private E[] elements;
    private int size = 0;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    public Stack() {
        elements = new E[DEFAULT_INITIAL_CAPACITY];
    }

    public void push(E e) {
        ensureCapacity();
        elements[size++] = e;
    }

    public E pop() {
        if (size == 0)
            throw new EmptyStackException();
        E result = elements[--size];
        elements[size] = null; // Eliminate obsolete reference
        return result;
    } ... // no changes in isEmpty or ensureCapacity
}
```

这时生成一个错误：

```java
Stack.java:8: generic array creation
elements = new E[DEFAULT_INITIAL_CAPACITY];
^
```

上一条目中讲到不能创建一个非具体化类型的数组。因此我们修改为：

```java
// The elements array will contain only E instances from push(E).
// This is sufficient to ensure type safety, but the runtime
// type of the array won't be E[]; it will always be Object[]!
@SuppressWarnings("unchecked")
public Stack() {
    elements = (E[]) new Object[DEFAULT_INITIAL_CAPACITY];
}
```

另一种解决编译错误的方法是将字段元素的类型从 E[] 更改为 Object[]。这时会得到一个不同的错误：

```java
Stack.java:19: incompatible types
found: Object, required: E
E result = elements[--size];
^
```

You can change this error into a warning by casting the element retrieved from the array to E, but you will get a warning:

通过将从数组中检索到的元素转换为 E，可以将此错误转换为警告。我们对警告做抑制：

```java
// Appropriate suppression of unchecked warning
public E pop() {
    if (size == 0)
        throw new EmptyStackException();
    // push requires elements to be of type E, so cast is correct
    @SuppressWarnings("unchecked")
    E result =(E) elements[--size];
    elements[size] = null; // Eliminate obsolete reference
    return result;
}
```



### 30、优先使用泛型方法

应尽量使方法支持泛型，这样可以保证类型安全，并让代码更容易使用。例如下面代码：

```java
// Generic method
public static <E> Set<E> union(Set<E> s1, Set<E> s2) {
    Set<E> result = new HashSet<>(s1);
    result.addAll(s2);
    return result;
}
```

有时，你需要创建一个对象，该对象是不可变的，但适用于许多不同类型，这时可以用泛型单例工厂模式来实现，如 Collections.emptySet。



下面的例子实现了一个恒等函数分发器：

```java
// Generic singleton factory pattern
private static UnaryOperator<Object> IDENTITY_FN = (t) -> t;

@SuppressWarnings("unchecked")
public static <T> UnaryOperator<T> identityFunction() {
    return (UnaryOperator<T>) IDENTITY_FN;
}
```

然后是对这个恒等函数分发器的使用：

```java
// Sample program to exercise generic singleton
public static void main(String[] args) {
    String[] strings = { "jute", "hemp", "nylon" };
    UnaryOperator<String> sameString = identityFunction();

    for (String s : strings)
        System.out.println(sameString.apply(s));

    Number[] numbers = { 1, 2.0, 3L };
    UnaryOperator<Number> sameNumber = identityFunction();

    for (Number n : numbers)
        System.out.println(sameNumber.apply(n));
}
```

递归类型限定允许类型参数被包含该类型参数本身的表达式限制，常见的场合是用在Comparable接口上。例如：

```java
// Using a recursive type bound to express mutual comparability
public static <E extends Comparable<E>> E max(Collection<E> c);
```



### 31、使用限定通配符来增加 API 的灵活性

在泛型中使用有界通配符，可以让API更加灵活。

考虑第29条中的堆栈类。我们创建一个`Stack<Number>`类型的堆栈，并在其中插入integer。

```java
Stack<Number> numberStack = new Stack<>();
Iterable<Integer> integers = ... ;
numberStack.pushAll(integers);
```

这个例子在直觉上似乎是没问题的。然而实际执行的时候会报错：

```java
StackTest.java:7: error: incompatible types: Iterable<Integer>
cannot be converted to Iterable<Number>
        numberStack.pushAll(integers);
                    ^
```

解决办法是使用带extends的有界通配符类型。下面代码表示泛型参数为E的子类型（包括E类型本身）：

```java
// Wildcard type for a parameter that serves as an E producer
public void pushAll(Iterable<? extends E> src) {
    for (E e : src)
        push(e);
}
```



作为堆栈，还需要对外提供弹出元素的方法，以下是基础实现：

```java
// popAll method without wildcard type - deficient!
public void popAll(Collection<E> dst) {
    while (!isEmpty())
        dst.add(pop());
}
```

但是这个实现在遇到下面场景时也会出现运行时报错，错误信息与前面的非常类似：`Collection<Object>`不是 `Collection<Number>` 的子类型。

```java
Stack<Number> numberStack = new Stack<Number>();
Collection<Object> objects = ... ;
numberStack.popAll(objects);
```

解决办法是使用带super的有界通配符类型。下面例子表示泛型参数为E的超类（包括E类型本身）。

```java
// Wildcard type for parameter that serves as an E consumer
public void popAll(Collection<? super E> dst) {
  while (!isEmpty()
    dst.add(pop());
}
```

总结上面例子的经验，就是生产者用extends通配符，消费者用super通配符。可以简记为PECS原则：producer-extends, consumer-super。



### 32、合理地结合泛型和可变参数

可变参数方法和泛型在一起工作时不那么协调，因此需要特别注意。

可变参数方法的设计属于一个抽象泄漏，即当你调用可变参数方法时，将创建一个数组来保存参数；该数组本应是实现细节，却是可见的。因此会出现编译器警告。

下面是一个可变参数和泛型混用，造成类型错误的例子：

```java
// Mixing generics and varargs can violate type safety!
static void dangerous(List<String>... stringLists) {
    List<Integer> intList = List.of(42);
    Object[] objects = stringLists;
    objects[0] = intList; // Heap pollution
    String s = stringLists[0].get(0); // ClassCastException
}
```

有人可能会问：为什么使用泛型可变参数声明方法是合法的，而显式创建泛型数组是非法的？因为带有泛型或参数化类型的可变参数的方法在实际开发中非常有用，因此语言设计人员选择忍受这种不一致性。

要保证可变参数和泛型混用时的类型安全，有以下两种方式：

1. 为方法添加 SafeVarargs标记，这代表方法的编写者承诺这个方法是类型安全的，需要方法编写者自己保证。这时编译器警告会被消除。
2. 如果方法内部保证可变参数只读，不做任何修改，那么这个方法也是类型安全的。

将数组传递给另一个使用 @SafeVarargs 正确注释的可变参数方法是安全的，将数组传递给仅计算数组内容的某个函数的非可变方法也是安全的。



### 33、优先考虑类型安全的异构容器

在前面的例子中，类型参数都是固定数量，例如`Map<K, V>`就只有两个类型参数。如果我们需要无限数量的类型参数，可以通过将类型参数放置在键上而不是容器上。例如，可以使用 DatabaseRow 类型表示数据库行，并使用泛型类型 `Column<T>` 作为它的键。

下面的例子实现了以类型作为键的缓存，它是类型安全的，例如以String类型为键读取时，读到的对象肯定也是String类型，而不是Integer类型。

```java
// Typesafe heterogeneous container pattern - implementation
public class Favorites {
  private Map<Class<?>, Object> favorites = new HashMap<>();

  public <T> void putFavorite(Class<T> type, T instance) {
    favorites.put(Objects.requireNonNull(type), instance);
  }

  public <T> T getFavorite(Class<T> type) {
    return type.cast(favorites.get(type));
  }
    // Typesafe heterogeneous container pattern - client
  public static void main(String[] args) {
        Favorites f = new Favorites();
        f.putFavorite(String.class, "Java");
        f.putFavorite(Integer.class, 0xcafebabe);
        f.putFavorite(Class.class, Favorites.class);
        String favoriteString = f.getFavorite(String.class);
        int favoriteInteger = f.getFavorite(Integer.class);
        Class<?> favoriteClass = f.getFavorite(Class.class);
        System.out.printf("%s %x %s%n", favoriteString,favoriteInteger, favoriteClass.getName());
  }
}
```



## 枚举和注解

### 34、用枚举类型代替 int 常量

枚举类型相比int常量有不少优点，如：能提供类型安全性，能提供 toString 方法打印字符串，还允许添加任意方法和字段并实现任意接口，使得枚举成为功能齐全的抽象（富枚举类型）。

一般来说，枚举在性能上可与 int 常量相比，不过加载和初始化枚举类型需要花费空间和时间，实际应用中这一点可能不太明显。

 

### 35、使用实例字段替代序数

所有枚举都有一个 ordinal 方法，该方法返回枚举类型中每个枚举常量的数值位置：

```java
// Abuse of ordinal to derive an associated value - DON'T DO THIS
public enum Ensemble {
    SOLO, DUET, TRIO, QUARTET, QUINTET,SEXTET, SEPTET, OCTET, NONET, DECTET;

    public int numberOfMusicians() { return ordinal() + 1; }
}
```

这样写虽然可行，但难以维护。如果常量被重新排序，numberOfMusicians 方法将被破坏。 更好的办法是使用一个额外的字段来代表序数：

```java
public enum Ensemble {
    SOLO(1), DUET(2), TRIO(3), QUARTET(4), QUINTET(5),SEXTET(6), SEPTET(7), OCTET(8), DOUBLE_QUARTET(8),NONET(9), DECTET(10),TRIPLE_QUARTET(12);

    private final int numberOfMusicians;

    Ensemble(int size) { this.numberOfMusicians = size; }

    public int numberOfMusicians() { return numberOfMusicians; }
}
```

ordinal是为基于枚举的通用数据结构（EnumSet 和 EnumMap）设计的。除非你用到这些数据结构，否则最好完全避免使用这个方法。



### 36、用 EnumSet 替代位字段

如果枚举类型的元素主要在 Set 中使用，传统上使用 int 枚举模式，通过不同的 2 的平方数为每个常量赋值：

```java
// Bit field enumeration constants - OBSOLETE!
public class Text {
    public static final int STYLE_BOLD = 1 << 0; // 1
    public static final int STYLE_ITALIC = 1 << 1; // 2
    public static final int STYLE_UNDERLINE = 1 << 2; // 4
    public static final int STYLE_STRIKETHROUGH = 1 << 3; // 8
    // Parameter is bitwise OR of zero or more STYLE_ constants
    public void applyStyles(int styles) { ... }
}
```

这种表示方式称为位字段，允许你使用位运算的或操作将几个常量组合成一个 Set：

```java
text.applyStyles(STYLE_BOLD | STYLE_ITALIC);
```

位字段具有 int 枚举常量所有缺点，例如：

1. 被打印成数字时难以理解
2. 没有简单的方法遍历位字段所有元素
3. 一旦确定了int或long作为位字段的存储类型，就不能超过它的范围（32位或64位）

EnumSet类是一种更好的选择，它避免了以上缺点。而且由于它在底层实现上与位操作类似，因此与位字段性能相当。

将之前的示例修改为使用EnumSet的方法，更加简单清晰：

```java
// EnumSet - a modern replacement for bit fields
public class Text {
    public enum Style { BOLD, ITALIC, UNDERLINE, STRIKETHROUGH }
    // Any Set could be passed in, but EnumSet is clearly best
    public void applyStyles(Set<Style> styles) { ... }
}
```

下面是将 EnumSet 实例传递给 applyStyles 方法的用户代码。EnumSet 类提供了一组丰富的静态工厂，可以方便地创建 Set：

```java
text.applyStyles(EnumSet.of(Style.BOLD, Style.ITALIC));
```



### 37、使用 EnumMap 替换序数索引

**用序数索引数组不如使用 EnumMap ，应尽量少使用 `ordinal()` 。**

例如这个类表示一种植物：

```java
class Plant {
    enum LifeCycle { ANNUAL, PERENNIAL, BIENNIAL }
    final String name;
    final LifeCycle lifeCycle;

    Plant(String name, LifeCycle lifeCycle) {
        this.name = name;
        this.lifeCycle = lifeCycle;
    }

    @Override public String toString() {
        return name;
    }
}
```

假设有一个代表花园全部植物的 Plant 数组 plantsByLifeCycle，用于列出按生命周期（一年生、多年生或两年生）排列的植物：

有些程序员会将这些集合放到一个按照类型序号进行索引的数组实现这一点。

```java
// 泛型数组
Set<Plant>[] plantsByLifeCycle =(Set<Plant>[]) new Set[Plant.LifeCycle.values().length];

for (int i = 0; i < plantsByLifeCycle.length; i++)
    plantsByLifeCycle[i] = new HashSet<>();

for (Plant p : garden)
    // Using ordinal() to index into an array - DON'T DO THIS!
    plantsByLifeCycle[p.lifeCycle.ordinal()].add(p);

// Print the results
for (int i = 0; i < plantsByLifeCycle.length; i++) {
    System.out.printf("%s: %s%n",
    Plant.LifeCycle.values()[i], plantsByLifeCycle[i]);
}
```

这种技术有如下问题：

1. 数组与泛型不兼容，需要 unchecked 转换。

   1. **类型擦除**：Java 的泛型是在编译时实现的，也就是说，在运行时，泛型类型信息会被擦除。而在Java中，数组是一个协变的、具有运行时类型信息的数据结构。由于泛型信息在运行时被擦除，因此在运行时无法知道具体的泛型参数类型，这和数组的运行时类型信息是相冲突的。
   2. **类型安全性**：Java为了保证类型安全，禁止了泛型数组的创建。假设Java允许我们创建泛型数组，则有可能发生下面的情况：

      ```java
      List<String>[] stringLists = new List<String>[1];
      Object[] objects = stringLists;
      objects[0] = new ArrayList<Integer>();
      ```

      这里，将一个`ArrayList<Integer>`赋给了一个`Object[]`引用，并没有引发任何警告或错误。但是，现在`stringLists[0]`实际上就是一个Integer列表，如果我们试图在其中放入一个字符串，就会在运行时抛出`ClassCastException`。

2. 数组不知道索引表示什么，必须手动标记输出。
3. int 不提供枚举的类型安全性，无法检验int值的正确性。



解决方案：

1. 使用 `java.util.EnumMap`：
   ```java
   // Using an EnumMap to associate data with an enum
   Map<Plant.LifeCycle, Set<Plant>> plantsByLifeCycle =new EnumMap<>(Plant.LifeCycle.class);
   
   for (Plant.LifeCycle lc : Plant.LifeCycle.values())
       plantsByLifeCycle.put(lc, new HashSet<>());
   
   for (Plant p : garden)
       plantsByLifeCycle.get(p.lifeCycle).add(p);
   
   System.out.println(plantsByLifeCycle);
   ```

   这个程序比原来的版本更短，更清晰，更安全，速度也差不多。速度相当的原因是，EnumMap 在内部使用这样的数组，但是它向程序员隐藏了实现细节。

2. 使用流可以进一步缩短程序：
   ```java
   // Naive stream-based approach - unlikely to produce an EnumMap!
   System.out.println(Arrays.stream(garden).collect(groupingBy(p -> p.lifeCycle)));
   ```

   这段代码的性能较差，因为底层不是基于 EnumMap，而是自己实现Map。

3. 要改进性能，可以使用 mapFactory 参数指定 Map 实现：
   ```java
   // Using a stream and an EnumMap to associate data with an enum
   System.out.println(
       Arrays.stream(garden).collect(groupingBy(p -> p.lifeCycle,() -> new EnumMap<>(LifeCycle.class), toSet()))
   );
   ```

   



下面例子用二维数组描述了气液固三台之间的装换，并使用序数索引读取二维数组的值：

```java
// Using ordinal() to index array of arrays - DON'T DO THIS!
public enum Phase {
    SOLID, LIQUID, GAS;

    public enum Transition {
        MELT, FREEZE, BOIL, CONDENSE, SUBLIME, DEPOSIT;

        // Rows indexed by from-ordinal, cols by to-ordinal
        private static final Transition[][] TRANSITIONS = {
            { null, MELT, SUBLIME },
            { FREEZE, null, BOIL },
            { DEPOSIT, CONDENSE, null }
        };

        // Returns the phase transition from one phase to another
        public static Transition from(Phase from, Phase to) {
            return TRANSITIONS[from.ordinal()][to.ordinal()];
        }
    }
}
```

这个程序的问题与前面 garden 示例一样，编译器无法知道序数和数组索引之间的关系。如果你在转换表中出错，或者在修改 Phase 或 `Phase.Transition` 枚举类型时忘记更新，程序将在运行时失败。



使用 EnumMap 可以做得更好：

```java
// Using a nested EnumMap to associate data with enum pairs
public enum Phase {
    SOLID, LIQUID, GAS;

    public enum Transition {
        MELT(SOLID, LIQUID), FREEZE(LIQUID, SOLID),
        BOIL(LIQUID, GAS), CONDENSE(GAS, LIQUID),
        SUBLIME(SOLID, GAS), DEPOSIT(GAS, SOLID);
        private final Phase from;
        private final Phase to;

        Transition(Phase from, Phase to) {
            this.from = from;
            this.to = to;
        }

    // Initialize the phase transition map
    private static final Map<Phase, Map<Phase,Transition> m =
        new EnumMap<Phase, Map<Phase ,Transition>>(Phase.class);

    static{
        for (Phase p : Phase. values())
            m.put(p,new EnumMap<Phase,Transition (Phase.class));
        for (Transition trans : Transition.values() )
            m.get(trans. src).put(trans.dst, trans) ;
    }

    public static Transition from(Phase src, Phase dst) {
        return m.get(src).get(dst);
    }
}
```

如果你想向系统中加入一种新阶段：等离子体。这个阶段只有两个变化：电离、去离子作用。修改基于EnumMap的版本要比基于数组的版本容易得多，而且更加清晰安全：

```java
// Adding a new phase using the nested EnumMap implementation
public enum Phase {
    SOLID, LIQUID, GAS, PLASMA;
    public enum Transition {
        MELT(SOLID, LIQUID), FREEZE(LIQUID, SOLID),
        BOIL(LIQUID, GAS), CONDENSE(GAS, LIQUID),
        SUBLIME(SOLID, GAS), DEPOSIT(GAS, SOLID),
        IONIZE(GAS, PLASMA), DEIONIZE(PLASMA, GAS);
        ... // Remainder unchanged
    }
}
```



### 38、使用接口模拟可扩展枚举

**虽然不能编写可扩展枚举类型，但是可以通过编写接口来模拟它。**

Java语言层面不支持可扩展枚举类型，但有时我们需要实现类似的需求，如操作码。操作码是一种枚举类型，其元素表示某些机器上的操作，例如第34条中的 Operation 类，它表示简单计算器上的函数。有时候，我们希望 API 的用户提供自己的操作，从而有效地扩展 API 提供的操作集。

一种思路是为操作码类型定义一个接口，并为接口的标准实现定义一个枚举：

```java
// Emulated extensible enum using an interface
public interface Operation {
    double apply(double x, double y);
}

public enum BasicOperation implements Operation {
    PLUS("+") {
        public double apply(double x, double y) { return x + y; }
    },
    MINUS("-") {
        public double apply(double x, double y) { return x - y; }
    },
    TIMES("*") {
        public double apply(double x, double y) { return x * y; }
    },
    DIVIDE("/") {
        public double apply(double x, double y) { return x / y; }
    };

    private final String symbol;

    BasicOperation(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
```

用这种方法可以轻松扩展自己的实现：

```java
// Emulated extension enum
public enum ExtendedOperation implements Operation {
    EXP("^") {
        public double apply(double x, double y) {
            return Math.pow(x, y);
        }
    },
    REMAINDER("%") {
        public double apply(double x, double y) {
            return x % y;
        }
    };

    private final String symbol;

    ExtendedOperation(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
```

通过传入扩展枚举类型到方法，可以编写一个上述代码的测试程序，它执行了前面定义的所有扩展操作：

```java
public static void main(String[] args) {
    double x = Double.parseDouble(args[0]);
    double y = Double.parseDouble(args[1]);
    test(ExtendedOperation.class, x, y);
}

private static <T extends Enum<T> & Operation> void test(Class<T> opEnumType, double x, double y) {
    for (Operation op : opEnumType.getEnumConstants())
        System.out.printf("%f %s %f = %f%n",x, op, y, op.apply(x, y));
}
```



第二个选择是传递一个 `Collection<? extends Operation>`，而非类对象：

```java
public static void main(String[] args) {
    double x = Double.parseDouble(args[0]);
    double y = Double.parseDouble(args[1]);
    test(Arrays.asList(ExtendedOperation.values()), x, y);
}

private static void test(Collection<? extends Operation> opSet,double x, double y) {
    for (Operation op : opSet)
        System.out.printf("%f %s %f = %f%n",x, op, y, op.apply(x, y));
}
```

这种方法的优点是：代码更简单，且允许调用者组合来自多个实现类型的操作。缺点是：放弃了在指定操作上使用 EnumSet和EnumMap的能力。

接口模拟可扩展枚举的一个小缺点是实现不能从一个枚举类型继承到另一个枚举类型。如果实现代码不依赖于任何状态，则可以使用默认实现将其放置在接口中。



### 39、注解优于命名模式

**如果可以使用注解，那么就没有理由使用命名模式。**

历史上，使用命名模式来标明某些程序元素需要框架特殊处理是很常见的。例如，JUnit 4以前的版本要求其用户通过以字符 test 开头的名称来指定测试方法。这种技术是有效的，但是有几个很大的缺点：

1. 拼写错误会导致没有提示的失败，导致一种正确执行了测试的假象。
2. 无法在类的级别使用test命名模式。例如，命名一个类 为TestSafetyMechanisms，希望 JUnit 3 能够自动测试它的所有方法，是行不通的。
3. 没有提供将参数值与程序元素关联的好方法。例如，希望支持只有在抛出特定异常时才成功的测试类别。如果将异常类型名称编码到测试方法名称中，那么代码将不好看且脆弱。

JUnit 从版本 4 开始使用注解解决了上述问题。在本条目中，我们将编写自己的示例测试框架来展示注解是如何工作的：

```java
// Marker annotation type declaration
import java.lang.annotation.*;

/**
* Indicates that the annotated method is a test method.
* Use only on parameterless static methods.
*/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test {

}
```

`@Retention(RetentionPolicy.RUNTIME)` 元注解表明测试注解在运行时生效。`@Target.get(ElementType.METHOD)` 元注解表明测试注解仅对方法生效。

Test的注释说明它只能用于无参的静态方法，但实际上编译器并没有对此做强制。

下面是 Test 注解实际使用时的样子。如果程序员拼错 Test 或将 Test 注解应用于除方法声明之外的元素，程序将无法编译：

```java
// Program containing marker annotations
public class Sample {
    @Test
    public static void m1() { } // Test should pass

    public static void m2() { }

    @Test
    public static void m3() { // Test should fail
        throw new RuntimeException("Boom");
    }

    public static void m4() { }

    @Test
    public void m5() { } // INVALID USE: nonstatic method

    public static void m6() { }

    @Test
    public static void m7() { // Test should fail
        throw new RuntimeException("Crash");
    }

    public static void m8() { }
}
```

Sample 类有 7 个静态方法，其中 4 个被注解为 Test。其中两个方法 m3 和 m7 抛出异常，另外两个 m1 和 m5 没有抛出异常。但是，m5 不是静态方法，所以不是有效的用例。总之，Sample 包含四个测试用例：一个通过，两个失败，一个无效。

以下是解析并运行Test 注解标记的测试的例子：

```java
// Program to process marker annotations
import java.lang.reflect.*;

public class RunTests {
    public static void main(String[] args) throws Exception {
        int tests = 0;
        int passed = 0;
        Class<?> testClass = Class.forName(args[0]);
        for (Method m : testClass.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Test.class)) {
                tests++;
                try {
                    m.invoke(null);
                    passed++;
                } catch (InvocationTargetException wrappedExc) {
                    Throwable exc = wrappedExc.getCause();
                    System.out.println(m + " failed: " + exc);
                } catch (Exception exc) {
                    System.out.println("Invalid @Test: " + m);
                }
        }
    }
    System.out.printf("Passed: %d, Failed: %d%n",passed, tests - passed);
    }
}
```

如果在 Sample 上运行 RunTests，输出如下：

```java
public static void Sample.m3() failed: RuntimeException: Boom
Invalid @Test: public void Sample.m5()
public static void Sample.m7() failed: RuntimeException: Crash
Passed: 1, Failed: 3
```

现在让我们添加一个只在抛出特定异常时才成功的测试支持。需要一个新的注解类型：

```java
// Annotation type with a parameter
import java.lang.annotation.*;

/**
* Indicates that the annotated method is a test method that
* must throw the designated exception to succeed.
*/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExceptionTest {
    Class<? extends Throwable> value();
}
```

下面是这个注解的实际应用：

```java
// Program containing annotations with a parameter
public class Sample2 {
    @ExceptionTest(ArithmeticException.class)
    public static void m1() { // Test should pass
        int i = 0;
        i = i / i;
    }

    @ExceptionTest(ArithmeticException.class)
    public static void m2() { // Should fail (wrong exception)
        int[] a = new int[0];
        int i = a[1];
    }

    @ExceptionTest(ArithmeticException.class)
    public static void m3() { } // Should fail (no exception)
}
```

修改RunTests类来处理新的注解。向 main 方法添加以下代码：

```java
if (m.isAnnotationPresent(ExceptionTest.class)) {
    tests++;
    try {
        m.invoke(null);
        System.out.printf("Test %s failed: no exception%n", m);
    } catch (InvocationTargetException wrappedEx) {
        Throwable exc = wrappedEx.getCause();
        Class<? extends Throwable> excType =m.getAnnotation(ExceptionTest.class).value();
        if (excType.isInstance(exc)) {
            passed++;
        } else {
            System.out.printf("Test %s failed: expected %s, got %s%n",m, excType.getName(), exc);
        }
    }
    catch (Exception exc) {
        System.out.println("Invalid @Test: " + m);
    }
}
```

这段代码提取注解参数的值，并使用它来检查测试抛出的异常是否是正确的类型。

进一步修改异常测试示例，将允许的指定异常扩展到多个：

```java
// Annotation type with an array parameter
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExceptionTest {
    Class<? extends Exception>[] value();
}
```

以下是对应的测试用例：

```java
// Code containing an annotation with an array parameter
@ExceptionTest({ IndexOutOfBoundsException.class,NullPointerException.class })
public static void doublyBad() {
    List<String> list = new ArrayList<>();
    // The spec permits this method to throw either
    // IndexOutOfBoundsException or NullPointerException
    list.addAll(5, null);
}
```

修改RunTests类：

```java
if (m.isAnnotationPresent(ExceptionTest.class)) {
    tests++;
    try {
        m.invoke(null);
        System.out.printf("Test %s failed: no exception%n", m);
    } catch (Throwable wrappedExc) {
        Throwable exc = wrappedExc.getCause();
        int oldPassed = passed;
        Class<? extends Exception>[] excTypes =m.getAnnotation(ExceptionTest.class).value();
        for (Class<? extends Exception> excType : excTypes) {
            if (excType.isInstance(exc)) {
                passed++;
                break;
            }
        }
        if (passed == oldPassed)
            System.out.printf("Test %s failed: %s %n", m, exc);
    }
}
```

Java 8 中可以在注解声明上使用 `@Repeatable` （重复注解）达到类似效果，提升程序可读性：

```java
// Repeatable annotation type
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ExceptionTestContainer.class)
public @interface ExceptionTest {
    Class<? extends Exception> value();
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExceptionTestContainer {
    ExceptionTest[] value();
}
```

使用重复注解代替数组值注解的测试用例：

```java
// Code containing a repeated annotation
@ExceptionTest(IndexOutOfBoundsException.class)
@ExceptionTest(NullPointerException.class)
public static void doublyBad() { ... }
```

为重复注解版本对应修改RunTests类：

```java
// Processing repeatable annotations
if (m.isAnnotationPresent(ExceptionTest.class)|| m.isAnnotationPresent(ExceptionTestContainer.class)) {
    tests++;
    try {
        m.invoke(null);
        System.out.printf("Test %s failed: no exception%n", m);
    } catch (Throwable wrappedExc) {
        Throwable exc = wrappedExc.getCause();
        int oldPassed = passed;
        ExceptionTest[] excTests =m.getAnnotationsByType(ExceptionTest.class);
        for (ExceptionTest excTest : excTests) {
            if (excTest.value().isInstance(exc)) {
                passed++;
                break;
            }
        }
        if (passed == oldPassed)
            System.out.printf("Test %s failed: %s %n", m, exc);
    }
}
```



### 40、坚持使用 @Override 注解

**在每个方法声明上都使用 `@Override` 注解来覆盖超类型声明，编译器可以帮助减少有害错误的影响。**

下面的类Bigram表示一个二元有序的字母对：

```java
// Can you spot the bug?
public class Bigram {
    private final char first;
    private final char second;

    public Bigram(char first, char second) {
        this.first = first;
        this.second = second;
    }

    public boolean equals(Bigram b) {
        return b.first == first && b.second == second;
    }

    public int hashCode() {
        return 31 * first + second;
    }

    public static void main(String[] args) {
        Set<Bigram> s = new HashSet<>();

        for (int i = 0; i < 10; i++)
            for (char ch = 'a'; ch <= 'z'; ch++)
                s.add(new Bigram(ch, ch));

        System.out.println(s.size());
    }
}
```

主程序重复地向一个集合中添加 26 个 bigram，每个 bigram 由两个相同的小写字母组成。然后它打印该集合的大小。运行该程序，它打印的不是 26 而是 260。

Bigram 类的作者打算覆盖 equals 方法，但实际上重载了它，因为参数类型不同。这个继承来的 equals 方法只能检测对象同一性，就像 == 操作符一样。每组中的每个 bigram 副本都不同于其他 9 个，这就解释了为什么程序最终打印 260。

如果使用Override注解就可以避免这个错误：

```java
@Override
public boolean equals(Bigram b) {
    return b.first == first && b.second == second;
}
```

这时编译器将生成如下错误消息：

```java
Bigram.java:10: method does not override or implement a method from a supertype
@Override public boolean equals(Bigram b) {
^
```

再修改为正确的实现：

```java
@Override
public boolean equals(Object o) {
    if (!(o instanceof Bigram))
        return false;
    Bigram b = (Bigram) o;
    return b.first == first && b.second == second;
}
```

因此，应该在要覆盖超类声明的每个方法声明上使用 `@Override` 注解。只有一个例外，如果你正在编写一个非抽象类，并且它覆盖了其超类中的抽象方法，那么不一定需要添加 `@Override` 注解。



### 41、使用标记接口定义类型

标记接口是一种不包含任何方法声明的接口，它只是标记它的实现类具有某种特性。如 Serializable 接口，实现此接口的类可以写入ObjectOutputStream（被序列化）。

**如果要定义类型，那么使用标记接口优于使用标记注解。**

标记接口相比标记注解有两个优点：

1. 标记接口定义的类型由标记类的实例实现；标记注解不会。前者在编译时捕获错误，后者在运行时才能捕捉错误。
2. 可以更精确地定位。标记注解可以应用于任何类或接口，而标记接口限定于它的实现类。

标记注解的优点是：它们可以是其他注解功能的一部分。

总之，如果你想要定义一个没有与之关联的新方法的类型，可以使用标记接口。如果你希望标记类和接口之外的程序元素，或者将标记符放入已经大量使用注解类型的框架中，那么应该使用标记注解。



## λ表达式和流

### 42、λ 表达式优于匿名类

**表示小函数对象时，lambda表达式优于匿名类。**

带有单个抽象方法的接口称为函数类型，它们的实例称为函数对象。历史上，创建函数对象的主要方法是匿名类。下面是一段按长度对字符串列表进行排序的代码，使用一个匿名类来创建排序的比较函数：

```java
// Anonymous class instance as a function object - obsolete!
Collections.sort(words, new Comparator<String>() {
    public int compare(String s1, String s2) {
        return Integer.compare(s1.length(), s2.length());
    }
});
```

匿名类的缺点是代码过于冗长。

在 Java 8 中，将具有单个抽象方法的接口称为函数式接口，允许使用 lambda 表达式创建这些接口的实例。Lambda 表达式更加简洁：

```java
// Lambda expression as function object (replaces anonymous class)
Collections.sort(words,(s1, s2) -> Integer.compare(s1.length(), s2.length()));
```

在上面的lambda 表达式中看不到对参数及其返回值类型的定义，这是因为类型是由编译器使用类型推断从上下文中推理出来的。

关于类型推断，需要注意：

1. 第26条：不要使用原始类型。

2. 第29条：优先使用泛型。

3. 第30条：优先使用泛型方法。

这些建议都是为了方便编译器从泛型中获取类型推断所需的信息。否则需要在lambda表达式中手动指定类型，这会使代码更加冗长。

使用 comparator 构造方法代替 lambda 表达式，可以让代码变得更加简洁：

```java
Collections.sort(words, comparingInt(String::length));
```

通过 Java 8 中添加到 List 接口的 sort 方法，可以使代码变得更短：

```java
words.sort(comparingInt(String::length));
```

我们可以使用lambda表达式优化第34条中的操作枚举类型，以下是原代码：

```java
// Enum type with constant-specific class bodies & data (Item 34)
public enum Operation {
    PLUS("+") {
        public double apply(double x, double y) { return x + y; }
    },
    MINUS("-") {
        public double apply(double x, double y) { return x - y; }
    },
    TIMES("*") {
        public double apply(double x, double y) { return x * y; }
    },
    DIVIDE("/") {
        public double apply(double x, double y) { return x / y; }
    };

    private final String symbol;

    Operation(String symbol) { this.symbol = symbol; }

    @Override
    public String toString() { return symbol; }

    public abstract double apply(double x, double y);
}
```

使用lambda表达式优化为可读性更好的版本：

```java
// Enum with function object fields & constant-specific behavior
public enum Operation {
    PLUS ("+", (x, y) -> x + y),
    MINUS ("-", (x, y) -> x - y),
    TIMES ("*", (x, y) -> x * y),
    DIVIDE("/", (x, y) -> x / y);

    private final String symbol;

    private final DoubleBinaryOperator op;

    Operation(String symbol, DoubleBinaryOperator op) {
        this.symbol = symbol;
        this.op = op;
    }

    @Override public String toString() { return symbol; }

    public double apply(double x, double y) {
        return op.applyAsDouble(x, y);
    }
}
```

其中 DoubleBinaryOperator 接口是 `java.util.function` 中预定义的函数式接口之一。它表示一个函数，接收两个 double 类型参数，返回值也为 double 类型。

lambda表达式并非任何场合都适合，以下情况就不适合： 

1. 算法较难理解时。写在带有名字会更有助于理解。 
2. 代码行数过多时。一般三行是合理的最大值。
3. 需要在lambda表达式中访问枚举实例成员时。无法访问，因为传递给enum构造函数的参数在静态上下文中计算。



有一些匿名类可以做的事情是 lambda 表达式不能做的： 

1. 创建抽象类的实例。Lambda表达式仅限于函数式接口。 
2. 创建具有多个抽象方法的接口实例。 
3. 获得对自身的引用。在 lambda 表达式中，this 关键字指的是外层实例，而非lambda表达式本身。



### 43、方法引用优于 λ 表达式

**方法引用通常比lambda 表达式更简洁，应优先使用。**

下面一个程序的代码片段的功能是：如果数字 1 不在映射中，则将其与键关联，如果键已经存在，则将关联值递增：

```java
map.merge(key, 1, (count, incr) -> count + incr);
```

在 Java 8 中，Integer类提供了一个静态方法 sum，它的作用完全相同，且更简单：

```java
map.merge(key, 1, Integer::sum);
```

如果 lambda 表达式太长或太复杂，那么可以将代码从 lambda 表达式提取到一个新方法中，并以对该方法的引用替换 lambda 表达式。可以为该方法起一个好名字并将其文档化。

有时候，lambda 表达式也会比方法引用更简洁。当方法与 lambda 表达式在同一个类中时，如以下代码片段发生在一个名为 GoshThisClassNameIsHumongous 的类中：

```java
service.execute(GoshThisClassNameIsHumongous::action);
```

使用 lambda 表达式会更简洁：

```java
service.execute(() -> action());
```

许多方法引用指向静态方法，但是有四种方法不指向静态方法。其中两个是绑定和非绑定实例方法引用。在绑定引用中，接收对象在方法引用中指定。在未绑定引用中，在应用函数对象时通过方法声明参数之前的附加参数指定接收对象。最后，对于类和数组，有两种构造函数引用。五种类型的方法汇总如下表：

| 方法引用类型 | 例子                   | 等价的lambda表达式                               |
| ------------ | ---------------------- | ------------------------------------------------ |
| 静态         | Integer::parseInt      | str -> Integer.parseInt(str)                     |
| 绑定         | Instant.now()::isAfter | Instant then =Instant.now(); t ->then.isAfter(t) |
| 非绑定       | String::toLowerCase    | str ->str.toLowerCase()                          |
| 类构造方法   | TreeMap<K,V>::new      | () -> new TreeMap<K,V>                           |
| 数组构造方法 | int[]::new             | len -> new int[len]                              |



### 44、优先使用标准函数式接口

自从 Java 已经有了 lambda 表达式，编写 API 的最佳实践变化很大。例如，以前会用子类覆盖基类方法以实现多态，现代的替代方法是提供一个静态工厂或构造函数，它接受一个函数对象来实现相同的效果。

例如 LinkedHashMap。可以通过覆盖受保护的 removeEldestEntry 方法将该类用作缓存，每当向映射添加新键时，put 都会调用该方法。当该方法返回 true 时，映射将删除传递给该方法的最老条目。下面的覆盖保证在每次添加新键时删除最老的条目，维护 100 个最近的条目：

```java
protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
    return size() > 100;
}
```

如果使用 lambda 表达式改造，那么LinkedHashMap将有一个静态工厂或构造函数，它接受一个函数对象，函数对象实现的函数式接口如下：

```java
// Unnecessary functional interface; use a standard one instead.
@FunctionalInterface interface EldestEntryRemovalFunction<K,V>{
    boolean remove(Map<K,V> map, Map.Entry<K,V> eldest);
}
```

上面对于新接口的声明不是必需的，因为`java.util.function` 包已经提供了大量的标准函数接口。**如果一个标准的函数式接口可以完成这项工作，那么你通常应该优先使用它，而不是使用专门构建的函数式接口。** 在LinkedHashMap 示例中，应该优先使用标准的 `BiPredicate<Map<K,V>`、`Map.Entry<K,V>>` 接口。

`java.util.function` 中有 43 个接口。可以只用记住 6 个基本接口，其余的接口在需要时派生出来：

1. Operator 接口：表示结果和参数类型相同的函数。（根据参数数量可分为一元、二元）
2. Predicate 接口：表示接受参数并返回布尔值的函数。
3. Function 接口：表示参数和返回类型不同的函数。
4. Supplier 接口：表示一个不接受参数并返回值的函数。
5. Consumer 接口：表示一个函数，该函数接受一个参数，但不返回任何内容，本质上是使用它的参数。

六个基本的函数式接口总结如下：

| 接口              | 方法签名            | 例子                |
| ----------------- | ------------------- | ------------------- |
| UnaryOperator<T>  | T apply(T t)        | String::toLowerCase |
| BinaryOperator<T> | T apply(T t1, T t2) | BigInteger::add     |
| Predicate<T>      | boolean test(T t)   | Collection::isEmpty |
| Function<T,R>     | R apply(T t)        | Arrays::asList      |
| Supplier<T>       | T get()             | Instant::now        |
| Consumer<T>       | void accept(T t)    | System.out::println |

大多数标准函数式接口的存在只是为了提供对基本类型的支持。例如，一个接受 int 的 Predicate 就是一个 IntPredicate，一个接受两个 long 值并返回一个 long 的二元操作符就是一个 LongBinaryOperator。不要尝试用带有包装类的基本函数式接口替代它们，因为这会带来糟糕的性能。

如果标准的函数式接口都不能满足需求，那么需要自行编写，例如，如果你需要一个接受三个参数的 Predicate，或者一个抛出已检查异常的 Predicate。

但是有时候即使其中一个标准接口在结构上满足需求，也应该编写自己的函数接口。以 `Comparator<T>`为例，它在结构上与 `ToIntBiFunction<T,T>` 接口相同。有几个原因前者优于后者：

1. Comparator的名称提供了优秀的文档，且使用频繁。
2. 通过实现接口，保证遵守其约定。
3. 该接口拥有大量用于转换和组合比较器的有用默认方法。

如果需要一个与 Comparator 共享以下特性的函数式接口，那么应该考虑编写一个专用的函数式接口，而不是使用标准接口：

1. 将被广泛使用，并且从描述性名称中获益。
2. 有一个严格的约定。
3. 受益于自定义默认方法。

建议总是用 `@FunctionalInterface` 注释你的函数接口。 有三个好处：

1. 告诉文档读者，接口的设计是为了启用 lambda 表达式。
2. 除非只有一个抽象方法，否则编译会报错。
3. 防止维护者在接口发展过程中意外地向接口添加抽象方法。



### 45、明智地使用流

**根据具体场合决定使用流还是迭代。**

在 Java 8 中添加了流 API，以简化序列或并行执行批量操作的任务。其中有两个关键的抽象：流（表示有限或无限的数据元素序列）和流管道（表示对这些元素的多阶段计算）。流中的数据元素可以是对象的引用或基本数据类型。支持三种基本数据类型：int、long 和 double。

流管道由源流、零个或多个中间操作（intermediate）和一个终止操作（terminal ）组成。每个中间操作以某种方式转换流，例如将每个元素映射到该元素的一个函数，或者过滤掉不满足某些条件的所有元素。终止操作做最终计算，例如将其元素存储到集合中、返回特定元素、或打印其所有元素。

流管道的计算是惰性的：直到调用终止操作时才开始计算，并且对完成终止操作不需要的数据元素永远不会计算。注意，没有终止操作的流管道是无动作的，因此不要忘记包含一个终止操作。

如果使用得当，流可以使程序更短、更清晰；否则，它们会降低程序可读性。

下面的程序从字典文件中读取单词并打印所有大小满足用户指定最小值的变位词组。如果两个单词以不同的顺序由相同的字母组成，那么它们就是变位词，属于同一个变位词组：

```java
// Prints all large anagram groups in a dictionary iteratively
public class Anagrams {
    public static void main(String[] args) throws IOException {
        File dictionary = new File(args[0]);
        int minGroupSize = Integer.parseInt(args[1]);
        Map<String, Set<String>> groups = new HashMap<>();
        try (Scanner s = new Scanner(dictionary)) {
            while (s.hasNext()) {
                String word = s.next();
                groups.computeIfAbsent(alphabetize(word),(unused) -> new TreeSet<>()).add(word);
            }
        }
        for (Set<String> group : groups.values())
        if (group.size() >= minGroupSize)
            System.out.println(group.size() + ": " + group);
    }

    private static String alphabetize(String s) {
        char[] a = s.toCharArray();
        Arrays.sort(a);
        return new String(a);
    }
}
```

将每个单词插入到 Map 中使用了computeIfAbsent方法：如果键存在，那么该方法仅返回与其关联的值；否则，该方法通过将给定的函数对象应用于键来计算一个值，并将该键值对放置到Map中。

将其修改成使用流的版本：

```java
// Overuse of streams - don't do this!
public class Anagrams {
    public static void main(String[] args) throws IOException {
        Path dictionary = Paths.get(args[0]);
        int minGroupSize = Integer.parseInt(args[1]);
        try (Stream<String> words = Files.lines(dictionary)) {
            words.collect(
            groupingBy(word -> word.chars().sorted()
            .collect(StringBuilder::new,(sb, c) -> sb.append((char) c),
            StringBuilder::append).toString()))
            .values().stream()
            .filter(group -> group.size() >= minGroupSize)
            .map(group -> group.size() + ": " + group)
            .forEach(System.out::println);
        }
    }
}
```

上面代码难以阅读，原因是过度使用了流。我们减少一些非必要使用流的地方，优化成如下代码：

```java
// Tasteful use of streams enhances clarity and conciseness
public class Anagrams {
    public static void main(String[] args) throws IOException {
        Path dictionary = Paths.get(args[0]);
        int minGroupSize = Integer.parseInt(args[1]);
        try (Stream<String> words = Files.lines(dictionary)) {
            words.collect(groupingBy(word -> alphabetize(word)))
            .values().stream()
            .filter(group -> group.size() >= minGroupSize)
            .forEach(g -> System.out.println(g.size() + ": " + g));
        }
    }
    // alphabetize method is the same as in original version
}
```

注意，参数 g 实际上应该命名为 group，但是生成的代码太长，在没有显式类型的情况下，lambda 表达式参数的谨慎命名对于流管道的可读性至关重要。另外，单词的字母化是在一个单独的字母化方法中完成的，好处是将实现细节排除在主程序之外，从而增强可读性。

使用流处理 char 值有风险，应尽量避免。例如下面代码：

```java
"Hello world!".chars().forEach(System.out::print);
```

你可能希望它打印 Hello world!，但实际打印 721011081081113211911111410810033。这是因为 `"Hello world!".chars()` 返回的流元素不是 char 值，而是 int 值，因此调用了 print 的 int 重载。可以通过强制调用正确的重载来修复：

```java
"Hello world!".chars().forEach(x -> System.out.print((char) x));
```

迭代使用的是代码块，而流通常使用的是lambda表达式或方法引用。有些事情可以在代码块中做，却不能在lambda表达式中做：

1. 读取或修改作用域中的任何局部变量。lambda 表达式中不能修改局部变量。
2. 从封闭方法返回、中断或继续封闭循环。
3. 抛出声明要抛出的任何已检查异常。

流擅长做的事情：

1. 元素序列的统一变换。
2. 过滤元素序列。
3. 使用单个操作组合元素序列（例如添加、连接或计算它们的最小值）。
4. 将元素序列累积到一个集合中，可能是按某个公共属性对它们进行分组。
5. 在元素序列中搜索满足某些条件的元素。

使用流很难做到的一件事是从管道的多个阶段同时访问相应的元素：一旦你将一个值映射到另一个值，原始值就会丢失。一个解决方案是将每个值映射到包含原始值和新值的 pair 对象，但这会让代码更加冗长。更好的解决方案是在需要访问早期阶段值时反转映射。

例如，编写一个程序来打印前 20 个 Mersenne 素数。一个 Mersenne 素数的数量是一个数字形式 2p − 1。如果 p 是素数，对应的 Mersenne 数可以是素数；如果是的话，这就是 Mersenne 素数。作为管道中的初始流，我们需要所有质数：

```java
static Stream<BigInteger> primes() {
    return Stream.iterate(TWO, BigInteger::nextProbablePrime);
}
```

下面是打印前 20 个 Mersenne 素数的程序：

```java
public static void main(String[] args) {
    primes().map(p -> TWO.pow(p.intValueExact()).subtract(ONE))
    .filter(mersenne -> mersenne.isProbablePrime(50))
    .limit(20)
    .forEach(System.out::println);
}
```

现在假设我们想要在每个 Mersenne 素数之前加上它的指数，这个值只在初始流中存在。幸运的是，通过对第一个中间操作中发生的映射求逆，可以很容易地计算出 Mersenne 数的指数：

```java
.forEach(mp -> System.out.println(mp.bitLength() + ": " + mp));
```

在许多任务中，使用流还是迭代并不明显。例如，考虑初始化一副新纸牌的任务。假设 Card 是一个不可变的值类，它封装了 Rank 和 Suit，它们都是 enum 类型。此任务代表需要计算可从两个集合中选择的所有元素对的任何任务，也称为这两个集合的笛卡尔积：

```java
// Iterative Cartesian product computation
private static List<Card> newDeck() {
    List<Card> result = new ArrayList<>();
    for (Suit suit : Suit.values())
    for (Rank rank : Rank.values())
    result.add(new Card(suit, rank));
    return result;
}
```

下面是一个基于流的实现:

```java
// Stream-based Cartesian product computation
private static List<Card> newDeck() {
    return Stream.of(Suit.values())
    .flatMap(suit ->Stream.of(Rank.values())
    .map(rank -> new Card(suit, rank)))
    .collect(toList());
}
```

两个版本的 newDeck 哪个更好？这个只能说见仁见智了。

### 46、在流中使用无副作用的函数

**应保证传递到流操作（包括中间操作和终止操作）中的任何函数对象都应该没有副作用。**

下面代码用于构建文本文件中单词的频率表：

```java
// Uses the streams API but not the paradigm--Don't do this!
Map<String, Long> freq = new HashMap<>();
try (Stream<String> words = new Scanner(file).tokens()) {
    words.forEach(word -> {
        freq.merge(word.toLowerCase(), 1L, Long::sum);
    });
}
```

上面代码根本不是流代码，而是伪装成流代码的迭代代码，而且比普通迭代代码更冗长。原因是这段代码在一个 终止操作中（forEach）执行它的所有工作，这是一种不当用法，终止操作本应只用来收集计算结果。正确的写法应该是：

```java
// Proper use of streams to initialize a frequency table
Map<String, Long> freq;
try (Stream<String> words = new Scanner(file).tokens()) {
    freq = words.collect(groupingBy(String::toLowerCase, counting()));
}
```

改进后的代码使用了 收集器（collector），它是用来收集计算结果的集合。收集器有三种：`toList()`、`toSet()` 和 `toCollection(collectionFactory)`，分别返回 List、Set 和程序员指定的集合类型。下面代码用收集器从 freq 表中提取前 10 个元素来构成一个新 List。

```java
// Pipeline to get a top-ten list of words from a frequency table
List<String> topTen = freq.keySet().stream()
    .sorted(comparing(freq::get).reversed())
    .limit(10)
    .collect(toList());
```

最简单的 Map 收集器是 `toMap(keyMapper, valueMapper)`，它接受两个函数，一个将流元素映射到键，另一个映射到值。我们在第34条中的 fromString 实现中使用了该收集器来创建枚举的字符串形式到枚举本身的映射：

```java
// Using a toMap collector to make a map from string to enum
private static final Map<String, Operation> stringToEnum =Stream.of(values()).collect(toMap(Object::toString, e -> e));
```

注意：需保证流中的每个元素映射到唯一的键。如果多个流元素映射到同一个键，管道将以 IllegalStateException 结束。

toMap 的三参数形式，允许为toMap 方法提供一个 merge 函数。例如，从有一个由不同艺术家录制的唱片流，可以得到一个从艺术家到最畅销唱片的映射：

```java
// Collector to generate a map from key to chosen element for key
Map<Artist, Album> topHits = albums.collect(
        toMap(Album::artist, a->a, maxBy(comparing(Album::sales)
    )
));
```

toMap 的三参数形式的另一个用途是，当发生键冲突时，它强制执行后写覆盖的策略。例如：

```java
// Collector to impose last-write-wins policy
toMap(keyMapper, valueMapper, (v1, v2) -> v2)
```

toMap 也提供了四参数的版本，当你想要指定一个特定的 Map 实现（如 EnumMap 或 TreeMap）时，可以使用它。

还有前三个版本的 toMap 的变体形式，名为 toConcurrentMap，它们可以有效地并行运行，同时生成 ConcurrentHashMap 实例。

收集器API 还提供 groupingBy 方法，该方法生成基于分类器函数将元素分组为类别的映射。例如下面代码：

```java
words.collect(groupingBy(word -> alphabetize(word)))
```

groupingBy也提供两参数形式，将 `counting()` 作为下游收集器传递。这将生成一个 Map，它将每个类别与类别中的元素数量相关联：

```java
Map<String, Long> freq = words.collect(groupingBy(String::toLowerCase, counting()));
```

另一个收集器方法是 join，它只对 CharSequence 实例流（如字符串）执行操作。它接受一个名为 delimiter 的 CharSequence 参数，然后在相邻元素之间插入分隔符。如果传入逗号作为分隔符，收集器将返回逗号分隔的值字符串。



### 47、优先选择 Collection 而不是流作为返回类型

如果一个 API 只返回一个流，而一些用户希望使用 for-each 循环遍历返回的序列，那么这些用户将会感到困难。例如：

```java
// Won't compile, due to limitations on Java's type inference
for (ProcessHandle ph : ProcessHandle.allProcesses()::iterator) {
    // Process the process
}
```

编译时会报错：

```java
Test.java:6: error: method reference not expected here
for (ProcessHandle ph : ProcessHandle.allProcesses()::iterator) {
^
```

解决方法是将方法引用转换为适当参数化的 Iterable：

```java
// Hideous workaround to iterate over a stream
for (ProcessHandle ph : (Iterable<ProcessHandle>)ProcessHandle.allProcesses()::iterator)
```

上面的代码太繁琐，更好的解决方案是使用适配器方法：

```java
// Adapter from Stream<E> to Iterable<E>
public static <E> Iterable<E> iterableOf(Stream<E> stream) {
    return stream::iterator;
}
```

通过这个适配器可以使用 for-each 语句遍历流：

```java
for (ProcessHandle p : iterableOf(ProcessHandle.allProcesses())) {
    // Process the process
}
```

同样，如果返回的是迭代器，而希望使用的是流，那么也需要编写适配器：

```java
// Adapter from Iterable<E> to Stream<E>
public static <E> Stream<E> streamOf(Iterable<E> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
}
```

如果考虑到作为公共API对外提供，那么建议使用Collection作为方法返回值。Collection 接口是 Iterable 的一个子类型，它有一个流方法，因此它提供了迭代和流两种访问方式。

假设你想要返回给定集合的幂集，该集合由它的所有子集组成。例如`{a, b, c}` 的排列组合有 `{{}, {a}, {b}, {c}, {a, b}, {a, c}, {b, c}, {a, b, c}}`。下面是代码：

```java
// Returns the power set of an input set as custom collection
public class PowerSet {
    public static final <E> Collection<Set<E>> of(Set<E> s) {
        List<E> src = new ArrayList<>(s);
        if (src.size() > 30)
            throw new IllegalArgumentException("Set too big " + s);

        return new AbstractList<Set<E>>() {
            @Override
            public int size() {
                return 1 << src.size(); // 2 to the power srcSize
            }

            @Override
            public boolean contains(Object o) {
                return o instanceof Set && src.containsAll((Set)o);
            }

            @Override
            public Set<E> get(int index) {
                Set<E> result = new HashSet<>();
                for (int i = 0; index != 0; i++, index >>= 1)
                    if ((index & 1) == 1)
                        result.add(src.get(i));
                return result;
            }
        };
    }
}
```

另一个例子是实现一个输入列表的所有子列表的流：

```java
// Returns a stream of all the sublists of its input list
public class SubLists {
    public static <E> Stream<List<E>> of(List<E> list) {
        return Stream.concat(Stream.of(Collections.emptyList()),prefixes(list).flatMap(SubLists::suffixes));
    }

    private static <E> Stream<List<E>> prefixes(List<E> list) {
        return IntStream.rangeClosed(1, list.size()).mapToObj(end -> list.subList(0, end));
    }

    private static <E> Stream<List<E>> suffixes(List<E> list) {
        return IntStream.range(0, list.size()).mapToObj(start -> list.subList(start, list.size()));
    }
}
```

我们的子列表实现在本质上类似于嵌套的 for 循环：

```java
for (int start = 0; start < src.size(); start++)
    for (int end = start + 1; end <= src.size(); end++)
        System.out.println(src.subList(start, end));
```

结论是，在编写返回元素序列的方法时，遵循以下建议：

1. 如果可以返回集合，那么就这样做。
2. 如果你已经在一个集合中拥有了元素，或者序列中的元素数量足够小，可以创建一个新的元素，那么返回一个标准集合，例如 ArrayList 。
3. 否则像对 power 集合那样实现自定义集合。
4. 如果返回集合不可行，则返回流或 iterable，以看起来更自然的方式返回。

### 48、谨慎使用并行流

**绝大多数情况下，不要并行化流，除非通过测试证明它能保持计算的正确性以及提高性能。**

编写正确且快速的并发程序是件困难的事情。考虑第45条中打印素数的程序：

```java
// Stream-based program to generate the first 20 Mersenne primes
public static void main(String[] args) {
    primes().map(p -> TWO.pow(p.intValueExact()).subtract(ONE))
    .filter(mersenne -> mersenne.isProbablePrime(50))
    .limit(20)
    .forEach(System.out::println);
}

static Stream<BigInteger> primes() {
    return Stream.iterate(TWO, BigInteger::nextProbablePrime);
}
```

假设尝试通过向流管道添加 `parallel()` 来加速它，性能会有提升吗？结果是它不会打印任何东西，但是 CPU 使用率会飙升到 90%，并且会无限期地停留在那里（活跃性失败）。

原因是stream 库不知道如何并行化这个管道。如果源来自 `Stream.iterate` 或使用中间操作限制，并行化管道不太可能提高其性能。

并行性能带来明显性能提升的场合：

1. ArrayList、HashMap、HashSet 和 ConcurrentHashMap 实例
2. 数组
3. 有界的IntStream和LongStream

这些数据结构的共同之处是：

1. 可以被精确且廉价地分割成任意大小的子结构，这使得在并行线程之间划分工作变得很容易。

2. 顺序处理时提供了良好的引用局部性（locality of reference）。引用局部性是指，当一个存储位置被处理器访问时，短时间内这个位置及附近位置被重复访问的趋势。良好的引用局部性可以充分利用处理器的多级缓存，带来性能提升。

流管道的终止操作的性质也会影响并行执行的效果。如果在终止操作中做了大量工作，且操作是顺序的，那么并行带来的提升很有限。并行的最佳终止操作是缩减型的方法，如reduce、min、max、count和sum。短路操作anyMatch、allMatch 和 noneMatch 也适用于并行。collect 方法执行的操作称为可变缩减，它们不是并行的好候选，因为组合集合的开销是昂贵的。

并行化流使用的函数对象还需要遵守一些规范，否则可能导致不正确的结果。这些规范例如：传递给流的reduce 操作的累加器和组合器函数必须是**关联的、不干扰的和无状态**的。

并行化流必须经过严格的性能测试，确保带来的收益超过代价。一般的经验是，流中的元素数量乘以每个元素执行的代码行数至少应该是100000。

某些领域，如机器学习和数据处理，特别适合使用并行流。例如下面代码：

```java
// Prime-counting stream pipeline - parallel version
static long pi(long n) {
    return LongStream.rangeClosed(2, n)
    .parallel()
    .mapToObj(BigInteger::valueOf)
    .filter(i -> i.isProbablePrime(50))
    .count();
}
```



## 方法

###  49、检查参数的有效性

**每次编写方法或构造函数时，都应该考虑参数存在哪些限制，并在文档中记录下来，然后在方法的开头显式地检查。**

如果没有在方法开头就验证参数，可能会违反故障原子性。因为方法可能会在执行过程中出现让人困惑的异常而失败，或者计算出错误的结果然后返回，甚至可能埋藏隐患，导致将来在不确定的某处代码产生错误。

对于公共方法和受保护的方法，使用`@throws` 标签记录违反参数限制会引发的异常，例如 IllegalArgumentException、IndexOutOfBoundsException 或 NullPointerException。见下面例子：

```java
/**
* Returns a BigInteger whose value is (this mod m). This method
* differs from the remainder method in that it always returns a
* non-negative BigInteger.
**
@param m the modulus, which must be positive
* @return this mod m
* @throws ArithmeticException if m is less than or equal to 0
*/
public BigInteger mod(BigInteger m) {
    if (m.signum() <= 0)
        throw new ArithmeticException("Modulus <= 0: " + m);
    ... // Do the computation
}
```

这里并没有记录m为null导致的NullPointerException，因为它被记录在类级别的文档注释中，这样不用在每个方法上再单独记录。

在 Java 7 中添加的 `Objects.requireNonNull` 方法非常灵活和方便，它可以检查输入对象是否为null，如果是，那么抛出带指定消息的NullPointerException；否则返回输入对象：

```java
// Inline use of Java's null-checking facility
this.strategy = Objects.requireNonNull(strategy, "strategy");
```

对于私有方法，作者应该确保只传递有效的参数值。因此，私有方法可以使用断言检查参数，如下所示：

```java
// Private helper function for a recursive sort
private static void sort(long a[], int offset, int length) {
    assert a != null;
    assert offset >= 0 && offset <= a.length;
    assert length >= 0 && length <= a.length - offset;
    ... // Do the computation
}
```

如果断言没有启用，那么不存在成本。你可以通过将 `-ea`标志传递给 java 命令来启用它们。

应在对象构造时就检查参数，而不是等待对象使用时再检查。这样做的好处是让问题第一时间暴露，否则后面再暴露，调试起来会比较麻烦。典型的如一个静态工厂方法，它接受一个 int 数组并返回数组的 List 视图。如果客户端传入 null，那么方法就立即抛出 NullPointerException。类似的例子还有构造函数。

但这条规则也有例外，当有效性检查成本较高或计算过程本身就包含参数检查时，会选择在计算过程中隐式检查。例如，考虑一个为对象 List 排序的方法，比如 `Collections.sort(List)`。List 中的所有对象必须相互比较，如果不能比较，那么将抛出 ClassCastException，这个检查过程放在比较过程中隐式进行，而不是预先检查。

对参数的限制并非越多越好。相反，你应该设计出尽可能通用的方法，对参数的限制越少越好。

### 50、在需要时制作防御性拷贝

Java 是一种安全的语言，在没有本地方法的情况下，它不受缓冲区溢出、数组溢出、非法指针和其他内存损坏错误的影响。

即使使用一种安全的语言，也可能在不经意间提供修改对象内部状态的方法。例如，下面的类表示一个不可变的时间段：

```java
// Broken "immutable" time period class
public final class Period {
    private final Date start;
    private final Date end;

    /**
    * @param start the beginning of the period
    * @param end the end of the period; must not precede start
    * @throws IllegalArgumentException if start is after end
    * @throws NullPointerException if start or end is null
    */
    public Period(Date start, Date end) {
        if (start.compareTo(end) > 0)
            throw new IllegalArgumentException(start + " after " + end);
        this.start = start;
        this.end = end;
    }

    public Date start() {
        return start;
    }

    public Date end() {
        return end;
    }
    ... // Remainder omitted
}
```

这个类要求时间段的开始时间不能在结束时间之后。然而，可以通过修改内部保存的Date绕过这个约束：

```java
// Attack the internals of a Period instance
Date start = new Date();
Date end = new Date();
Period p = new Period(start, end);
end.setYear(78); // Modifies internals of p!
```

从 Java 8 开始，解决这个问题的典型方法就是使用不可变的 Instant（或 LocalDateTime 或 ZonedDateTime）来代替**已过时的Date**。但是对于包含Date的老代码，必须找到一个通用的解决办法。

解决办法就是将每个可变参数的防御性拷贝复制给构造函数：

```java
// Repaired constructor - makes defensive copies of parameters
public Period(Date start, Date end) {
    this.start = new Date(start.getTime());
    this.end = new Date(end.getTime());
    if (this.start.compareTo(this.end) > 0)
        throw new IllegalArgumentException(this.start + " after " + this.end);
}
```

新的构造函数保证之前的攻击不会对 Period 实例产生影响。注意，防御性拷贝是在检查参数的有效性之前制作的，这样保证在检查参数和复制参数之间的时间段，类不受来自其他线程更改的影响。在计算机安全领域，这被称为 time-of-check/time-of-use漏洞或TOCTOU攻击。

之所以没有使用 Date 的 clone 方法来创建防御性拷贝，是因为 Date 不是 final 的，所以不能保证 clone 方法返回一个Date 的实例对象，它可以返回一个不受信任子类的实例，有从这里发起恶意破坏的风险。

还可以用另一种方式修改Period内部状态：

```java
// Second attack on the internals of a Period instance
Date start = new Date();
Date end = new Date();
Period p = new Period(start, end);
p.end().setYear(78); // Modifies internals of p!
```

解决办法是在访问器上返回可变内部字段的防御性拷贝：

```java
// Repaired accessors - make defensive copies of internal fields
public Date start() {
    return new Date(start.getTime());
}

public Date end() {
    return new Date(end.getTime());
}
```

有了新的构造函数和新的访问器，Period 实际上是不可变的，除非使用反射或本地方法修改。

应尽量使用不可变对象作为对象的组件，这样就不必操心防御性拷贝。

防御性拷贝可能会带来性能损失。如果一个类信任它的调用者不会去修改内部组件，那么可以不用做防御性拷贝，而在类文档上加以说明：调用者不能修改相关的参数对象或返回值。

### 51、仔细设计方法签名

本条目是多条 API 设计经验的汇总。

**仔细选择方法名字。** 应选择可理解的、与同一包中的其他名称风格一致的、被广泛认可的名字。

**提供便利的方法不应做过了头。** 太多的方法会使得类难以维护。对于类或接口支持的每个操作，一般只提供一个功能齐全的方法就可以了，只有在经常使用时才考虑提供快捷方式。

**避免长参数列表。** 设定四个或更少的参数，大多数程序员记不住更长的参数列表。长序列的同类型参数尤其有害，极易导致错误。

有三种方法可以缩短过长的参数列表：

1. 将原方法分解为多个子方法。原方法的功能由多个字方法组合实现，这样每个子方法只需要参数的一个子集。
2. 创建 helper 类来保存参数组。
3. 从对象构建到方法调用都采用建造者模式。

**对于方法的参数类型，优先选择接口而不是类**。例如优先选择Map而不是HashMap作为方法的参数类型。

**对于方法的参数类型，优先选择双元素枚举类型而不是boolean** 。枚举使代码更容易维护。此外，它们使以后添加更多选项变得更加容易。例如，你可能有一个Thermometer（温度计）类型与静态工厂，采用枚举：

```java
public enum TemperatureScale { FAHRENHEIT, CELSIUS } // 华氏、摄氏
```

`Thermometer.newInstance(TemperatureScale.CELSIUS)` 不仅比 `Thermometer.newInstance(true)` 更有意义，而且你可以在将来的版本中向 TemperatureScale 添加 KELVIN（开尔文、热力学温标）。

### 52、明智地使用重载

> 注意，这里说的重载是指overload，而不是重写override。

下面的程序是一个善意的尝试，根据一个 Collection 是 Set、List 还是其他的集合类型来进行分类：

```java
// Broken! - What does this program print?
public class CollectionClassifier {
    public static String classify(Set<?> s) {
        return "Set";
    }

    public static String classify(List<?> lst) {
        return "List";
    }

    public static String classify(Collection<?> c) {
        return "Unknown Collection";
    }

    public static void main(String[] args) {
        Collection<?>[] collections = {
            new HashSet<String>(),new ArrayList<BigInteger>(),new HashMap<String, String>().values()
        };
        for (Collection<?> c : collections)
            System.out.println(classify(c));
    }
}
```

你期望的是：这个程序打印 Set、List 和 Unknown Collection，但结果是：它打印 Unknown Collection 三次。因为classify方法被重载，并且在编译时就决定了要调用哪个重载。



修复 CollectionClassifier 程序的最佳方法是用一个方法中用instanceof做类型判断：

```java
public static String classify(Collection<?> c) {
    return c instanceof Set ? "Set" :c instanceof List ? "List" : "Unknown Collection";
}
```

应该避免混淆重载的用法。最保守的策略是永远不生成具有相同参数数量的两个重载。或者为方法提供不同的名字，这样就可以不用重载。

如果必须生成具有相同参数数量的两个重载，那么须保证至少有一个参数在这两个重载中具有**完全不同的**类型。一个反例与Java的自动装箱机制有关：

```java
public class SetList {
public static void main(String[] args) {
    Set<Integer> set = new TreeSet<>();
    List<Integer> list = new ArrayList<>();
    for (int i = -3; i < 3; i++) {
        set.add(i);
        list.add(i);
    }
    for (int i = 0; i < 3; i++) {
        set.remove(i);
        list.remove(i);
    }
    System.out.println(set +""+list);
    }
}
```

我们期望从set和list中分别删除0、1、2这三个数字，但是只在set中如愿，实际在list中删除的是对应下标0、1、2的三个元素。因为对 `list.remove(i)` 的调用选择的是重载方法 `remove(int i)`，而不是 `remove(Object o)`。正确的写法应该是:

```java
for (int i = 0; i < 3; i++) {
    set.remove(i);
    list.remove((Integer) i); // or remove(Integer.valueOf(i))
}
```

lambda 表达式和方法引用也容易引起重载中的混淆。例如：

```java
new Thread(System.out::println).start();
ExecutorService exec = Executors.newCachedThreadPool();
exec.submit(System.out::println);
```

Thread 构造函数调用和 submit 方法调用看起来很相似，但是前者能通过编译而后者不能。因为submit 方法有一个重载，它接受一个 `Callable<T>`，而线程构造函数没有。所以编译器会组织后者以免产生歧义。

### 53、明智地使用可变参数

可变参数方法接受指定类型的零个或多个参数。可变参数的底层是一个数组。

一个简单的可变参数例子：

```java
// Simple use of varargs
static int sum(int... args) {
    int sum = 0;
    for (int arg : args)
        sum += arg;
    return sum;
}
```

假设要编写一个函数来计算其参数的最小值。如果客户端不传递参数，那么在运行时检查参数长度，并抛出异常：

```java
// The WRONG way to use varargs to pass one or more arguments!
static int min(int... args) {
    if (args.length == 0)
        throw new IllegalArgumentException("Too few arguments");
    int min = args[0];
    for (int i = 1; i < args.length; i++)
        if (args[i] < min)
    		min = args[i];
    return min;
}
```

这个解决方案有几个问题： 

1. 如果不带参数调用此方法，那么会在运行时而非编译时失败。 
2. 不美观。不能使用for-each循环，除非将min初始化为Integer.MAX_VALUE。



以下写法能避免这些问题：

```java
// The right way to use varargs to pass one or more arguments
static int min(int firstArg, int... remainingArgs) {
    int min = firstArg;
    for (int arg : remainingArgs)
        if (arg < min)
    		min = arg;
    return min;
}
```

在性能关键的情况下使用可变参数要小心。每次调用可变参数方法都会导致数组创建和初始化。有一种折中的办法。假设 95% 的调用只需要三个或更少的参数，那么只对三个以上参数使用可变参数：

```java
public void foo() { }
public void foo(int a1) { }
public void foo(int a1, int a2) { }
public void foo(int a1, int a2, int a3) { }
public void foo(int a1, int a2, int a3, int... rest) { }
```



### 54、返回空集合或数组，而不是 null

如下方法很常见：

```java
// Returns null to indicate an empty collection. Don't do this!
private final List<Cheese> cheesesInStock = ...;
/**
* @return a list containing all of the cheeses in the shop,
* or null if no cheeses are available for purchase.
*/
public List<Cheese> getCheeses() {
    return cheesesInStock.isEmpty() ? null: new ArrayList<>(cheesesInStock);
}
```

这样写的坏处是方法调用时需做非null的判断：

```java
List<Cheese> cheeses = shop.getCheeses();
if (cheeses != null && cheeses.contains(Cheese.STILTON))
    System.out.println("Jolly good, just the thing.");
```

当忘记做非null的判断时就会出错，应该改成返回空的列表：

```java
//The right way to return a possibly empty collection
public List<Cheese> getCheeses() {
    return new ArrayList<>(cheesesInStock);
}
```

如果新创建空集合会损害性能，那么可以通过重复返回空的不可变集合来避免新的创建：

```java
// Optimization - avoids allocating empty collections
public List<Cheese> getCheeses() {
    return cheesesInStock.isEmpty() ? Collections.emptyList(): new ArrayList<>(cheesesInStock);
}
```

数组与集合情况类似。永远不要返回 null，而应该返回零长度的数组。下面代码将一个零长度的数组传递到 toArray 方法中：

```java
//The right way to return a possibly empty array
public Cheese[] getCheeses() {
    return cheesesInStock.toArray(new Cheese[0]);
}
```

如果你创建零长度数组会损害性能，你可以重复返回相同的零长度数组：

```java
// Optimization - avoids allocating empty arrays
private static final Cheese[] EMPTY_CHEESE_ARRAY = new Cheese[0];
public Cheese[] getCheeses() {
    return cheesesInStock.toArray(EMPTY_CHEESE_ARRAY);
}
```

不要为了提高性能而预先分配传递给 toArray 的数组:

```java
// Don’t do this - preallocating the array harms performance!
return cheesesInStock.toArray(new Cheese[cheesesInStock.size()]);
```



### 55、明智地的返回 Optional

在 Java 8 之前，在编写在某些情况下无法返回值的方法时，可以采用两种方法：抛出异常或者返回 null。这两种方法都不完美。抛出异常代价高昂，返回null需对这种情况做特殊判断。

在 Java 8 中，可以通过返回`Optional<T>` 解决上述问题。它表示理论上应返回 T，但在某些情况下可能无法返回 T。

在第30条中，有一个根据集合元素的自然顺序计算集合最大值的例子：

```java
// Returns maximum value in collection - throws exception if empty
public static <E extends Comparable<E>> E max(Collection<E> c) {
    if (c.isEmpty())
        throw new IllegalArgumentException("Empty collection");
    E result = null;
    for (E e : c)
        if (result == null || e.compareTo(result) > 0)
    		result = Objects.requireNonNull(e);
    return result;
}
```

更好的替代方法是返回 `Optional<E>`：

```java
// Returns maximum value in collection as an Optional<E>
public static <E extends Comparable<E>> Optional<E> max(Collection<E> c) {
    if (c.isEmpty())
        return Optional.empty();
    E result = null;
    for (E e : c)
        if (result == null || e.compareTo(result) > 0)
    		result = Objects.requireNonNull(e);
    return Optional.of(result);
}
```

注意，**永远不要从拥有Optional返回值的方法返回空值**，因为它违背了这个功能的设计初衷。

许多流上的 Terminal 操作返回 Optional。如果我们使用一个流来重写 max 方法，那么流版本的 max 操作会为我们生成一个 Optional：

```java
// Returns max val in collection as Optional<E> - uses stream
public static <E extends Comparable<E>> Optional<E> max(Collection<E> c) {
    return c.stream().max(Comparator.naturalOrder());
}
```

对于返回Optional的方法，调用者可以选择如果该方法不能返回值要采取什么操作。你可以指定一个默认值：

```java
// Using an optional to provide a chosen default value
String lastWordInLexicon = max(words).orElse("No words...");
```

或者可以抛出任何适当的异常：

```java
// Using an optional to throw a chosen exception
Toy myToy = max(toys).orElseThrow(TemperTantrumException::new);
```

如果你能判断一个 Optional 非空，那么可以从 Optional 直接获取值，而无需指定空值对应的操作。但如果判断错误，会抛出一个 NoSuchElementException：

```java
// Using optional when you know there’s a return value
Element lastNobleGas = max(Elements.NOBLE_GASES).get();
```

并非所有的返回类型都适合用Optional封装，不适合的类型有：容器类型，包括集合、Map、流、数组和 Optional。例如应返回空的List，而不是`Optional<List>`。

适合使用Optional的场景是：方法有可能不会返回值，如果没有返回值，调用者不得不做特殊处理。

在性能关键的场合请谨慎使用Optional，因为它有封装原始对象的额外性能代价。

不应该在除方法返回值以外的任何地方使用Optional。



### 56、为所有公开的 API 元素编写文档注释

要正确地编写 API 文档，必须在每个公开的类、接口、构造函数、方法和字段声明之前加上文档注释。

方法的文档注释应该简洁地描述方法与其调用者之间的约定，包括： 

1. 说明方法做了什么，而不是如何做。 
2. 应列举方法所有的前置条件和后置条件。 
3. 说明方法产生的副作用。如启动一个新的后台线程。
4. 应包含必要的@param、@return 和@throw注释。



以下是一个满足上面要求的方法注释：

```java
/**
* Returns the element at the specified position in this list.
**
<p>This method is <i>not</i> guaranteed to run in constant
* time. In some implementations it may run in time proportional
* to the element position.
**
@param index index of element to return; must be
* non-negative and less than the size of this list
* @return the element at the specified position in this list
* @throws IndexOutOfBoundsException if the index is out of range
* ({@code index < 0 || index >= this.size()})
*/
E get(int index);
```

为泛型类型或方法编写文档时，请确保说明所有的类型参数：

```java
/**
* An object that maps keys to values. A map cannot contain
* duplicate keys; each key can map to at most one value.
**
(Remainder omitted)
**
@param <K> the type of keys maintained by this map
* @param <V> the type of mapped values
*/
public interface Map<K, V> { ... }
```

对于枚举类型，文档要覆盖枚举类型本身、常量以及公开的方法：

```java
/**
* An instrument section of a symphony orchestra.
*/
public enum OrchestraSection {
/** Woodwinds, such as flute, clarinet, and oboe. */
WOODWIND,
/** Brass instruments, such as french horn and trumpet. */
BRASS,
/** Percussion instruments, such as timpani and cymbals. */
PERCUSSION,
/** Stringed instruments, such as violin and cello. */
STRING;
}
```

对于注解类型，文档要覆盖注解类型本身和所有成员：

```java
/**
* Indicates that the annotated method is a test method that
* must throw the designated exception to pass.
*/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExceptionTest {
/**
* The exception that the annotated test method must throw
* in order to pass. (The test is permitted to throw any
* subtype of the type described by this class object.)
*/
Class<? extends Throwable> value();
}
```

无论类或静态方法是否线程安全，都应该说明它的线程安全级别。如果一个类是可序列化的，那么应该说明它的序列化形式。

Java自带的Javadoc会根据代码自动生成文档，并且检查文档是否符合本条目中提及的许多建议。在 Java 8 和 Java 9 中，默认启用了自动检查。诸如 checkstyle 之类的 IDE 插件在检查是否符合





## 通用程序设计

###  57、将局部变量的作用域最小化

 将局部变量的作用域最小化，最有效的办法就是在第一次使用它的地方声明。

每个局部变量声明都应该包含一个初始化表达式。如果还没足够的信息初始化，那么应该推迟声明。这个规则的一个例外是try-catch语句，如果一个变量要在try块之外使用，那么就要在try块之前声明，此时可能还没有足够的信息初始化这个变量。

最小化局部变量作用域的第二种方法是使用for循环。如果一个变量在循环之后就不再使用，那么使用for循环优于while循环。下面是一个for-each循环的例子：

```java
// Preferred idiom for iterating over a collection or array
for (Element e : c) {
    ... // Do Something with e
}
```

当需要iterator或者remove时，使用传统for循环替代for-each循环：

```java
// Idiom for iterating when you need the iterator
for (Iterator<Element> i = c.iterator(); i.hasNext(); ) {
    Element e = i.next();
    ... // Do something with e and i
}
```

下面的例子说明了为什么for循环优于while循环，因为这个例子暗含了一个bug（将i2错写为i）：

```java
Iterator<Element> i = c.iterator();
while (i.hasNext()) {
    doSomething(i.next());
}
...
Iterator<Element> i2 = c2.iterator();
while (i.hasNext()) { // BUG!
    doSomethingElse(i2.next());
}
```

如果改成使用for循环，那么在编译期间就可以发现这个问题：

```java
for (Iterator<Element> i = c.iterator(); i.hasNext(); ) {
Element e = i.next();
... // Do something with e and i
}
...
// Compile-time error - cannot find symbol i
for (Iterator<Element> i2 = c2.iterator(); i.hasNext(); ) {
Element e2 = i2.next();
... // Do something with e2 and i2
}
```

for循环相对while循环的另一个优点是，它更短，可读性更好。如下面例子：

```java
for (int i = 0, n = expensiveComputation(); i < n; i++) {
    ... // Do something with i;
}
```

最小化局部变量作用域的第三种方法是保持方法小而集中。例子一个方法包含了两段不同的逻辑，一个变量只用于其中一段逻辑中，那么可以将这个方法按逻辑拆分成两个不同的方法。



### 58、 for-each 循环优于传统的 for 循环

下面是使用传统for循环遍历容器的例子：

```java
// Not the best way to iterate over a collection!
for (Iterator<Element> i = c.iterator(); i.hasNext(); ) {
    Element e = i.next();
    ... // Do something with e
}
```

改用for-each要简洁得多：

```java
// The preferred idiom for iterating over collections and arrays
for (Element e : elements) {
    ... // Do something with e
}
```

嵌套循环中使用传统for循环比for-each循环更容易出bug。如下面使用传统for循环的代码暗含bug，因为执行i.next()的次数超出预期：

```java
// Can you spot the bug?
enum Suit { CLUB, DIAMOND, HEART, SPADE }
enum Rank { ACE, DEUCE, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT,NINE, TEN, JACK, QUEEN, KING }
...
static Collection<Suit> suits = Arrays.asList(Suit.values());
static Collection<Rank> ranks = Arrays.asList(Rank.values());
List<Card> deck = new ArrayList<>();
for (Iterator<Suit> i = suits.iterator(); i.hasNext(); )
	for (Iterator<Rank> j = ranks.iterator(); j.hasNext(); )
		deck.add(new Card(i.next(), j.next()));
```

改成使用for-each循环就能避免这个问题：

```java
// Preferred idiom for nested iteration on collections and arrays
for (Suit suit : suits)
	for (Rank rank : ranks)
		deck.add(new Card(suit, rank));
```

但是有三种情况不应该使用for-each：

1. 破坏性过滤：如果需要在[遍历过程中删除元素](https://www.seven97.top/java/collection/02-collection1-arraylist.html#遍历时删除-添加-常见陷阱)，那么应该使用iterator和remove方法。Java 8中可以使用Collection类中提供的removeIf方法达到同样效果。
2. 转换：如果需要在遍历List或者数组的时候替换其中部分元素的值，那么需要使用迭代器或者数组索引。
3. 并行迭代：如果需要并行遍历多个容器，那么需要使用迭代器，自行控制迭代进度。

for-each循环还可以用来遍历实现Iterable接口的任何对象。



### 59、了解并使用库

 有时候程序员喜欢自己造一些轮子。例如实现一个获取随机数的方法：

```java
// Common but deeply flawed!
static Random rnd = new Random();
static int random(int n) {
    return Math.abs(rnd.nextInt()) % n;
}
```

这个方法不仅不能做到输出均匀分布，而且在边界情况运行时可能会报错。

正确的做法是使用java库中的Random类。在Java 7之后，更应该选择性能更好的ThreadLocalRandom取代Random。

使用库的好处有：

1. 利用专家知识，实现特定的复杂算法或逻辑。

2. 让调用者专注于业务开发，不用在底层实现上多费时间。

3. 库的性能会不断提升，调用者无需付出额外努力。

4. 让代码更精简，更容易维护。

在Java每个主要版本中，都会向库中添加许多特性，了解这些新增特性是值得的。

如果说要库的内容太多不好掌握，那么每个程序员至少应该熟悉 java.lang、java.util 和 [http://java.io](https://link.zhihu.com/?target=http%3A//java.io) 及其子包。其中collections框架、streams库和java.util.concurrent库应该重点掌握。



### 60、若需要精确答案就应避免使用 float 和 double 类型

float 和 double 类型主要用于工程计算和科学计算。它们本质上是二进制浮点类型，因此不能用来表示精确的结果。特别不适合进行货币计算，因为10的任意负次幂无法精确表示为float或double。

假设原有1.03美元，消费掉0.42美元，还剩多少钱？输出会是0.6100000000000001。

```java
System.out.println(1.03 - 0.42);
```

假设架上有一排糖果，价格依次为10美分、20美分、30美分......你有1美元，按次数购买糖果，直到把钱用完：

```java
// Broken - uses floating point for monetary calculation!
public static void main(String[] args) {
    double funds = 1.00;
    int itemsBought = 0;
    for (double price = 0.10; funds >= price; price += 0.10) {
        funds -= price;
        itemsBought++;
    }
    System.out.println(itemsBought +"items bought.");
    System.out.println("Change: $" + funds);
}
```

结果显示只买了三颗糖果，而你还剩0.399999999999999999美元。这是错误的答案。解决方案是使用 BigDecimal、int 或 long 进行货币计算。

改为使用BigDecimal计算，可以得到正确结果：

```java
public static void main(String[] args) {
    final BigDecimal TEN_CENTS = new BigDecimal(".10");
    int itemsBought = 0;
    BigDecimal funds = new BigDecimal("1.00");
    for (BigDecimal price = TEN_CENTS;funds.compareTo(price) >= 0;price = price.add(TEN_CENTS)) {
        funds = funds.subtract(price);
    itemsBought++;
    }
    System.out.println(itemsBought +"items bought.");
    System.out.println("Money left over: $" + funds);
}
```

使用[BigDecimal](https://www.seven97.top/java/basis/01-basic-knowledge.html#bigdecimal)的另一个好处是：它有8种舍入模式可以选择，如果业务需要做数值舍入，那么将非常方便。不过它也有两个缺点：它与原始算术类型相比使用很不方便，而且速度要慢得多。

另一种方案是使用int表示的美分来计算：

```java
public static void main(String[] args) {
    int itemsBought = 0;
    int funds = 100;
    for (int price = 10; funds >= price; price += 10) {
        funds -= price;
        itemsBought++;
    }
    System.out.println(itemsBought +"items bought.");
    System.out.println("Cash left over: " + funds + " cents");
}
```



### 61、基本数据类型优于包装类

基本类型和包装类型之间有三个主要区别： 

1. 基本类型只有值，而包装类型具有与其值不同的标识。 
2. 基本类型只有全功能值，而每个包装类型除了对应的基本类型的所有功能值外，还有一个非功能值null。
3. 基本类型比包装类型更节省时间和空间。

基本类型和包装类型的区别会带来一些麻烦。例如下面的比较器对Integer做数值比较：

```java
// Broken comparator - can you spot the flaw?
Comparator<Integer> naturalOrder =(i, j) -> (i < j) ? -1 : (i == j ? 0 : 1);
```

这里有个隐藏的bug。当执行`naturalOrder.compare(new Integer(42), new Integer(42))`时，执行结果是1，而非期待中的0。原因是用`==`比较两个不同的Integer对象，结果肯定是false。将`==`操作符应用于包装类型几乎都是错误的。

正确的写法是先自动拆箱再比较：

```java
Comparator<Integer> naturalOrder = (iBoxed, jBoxed) -> {
    int i = iBoxed, j = jBoxed; // Auto-unboxing
    return i < j ? -1 : (i == j ? 0 : 1);
};
```

下面的程序不会打印出Unbelievable，而是抛出NullPointerException。原因是在操作中混合使用基本类型和包装类型时，包装类型就会自动拆箱：

```java
public class Unbelievable {
static Integer i;
public static void main(String[] args) {
    if (i == 42)
        System.out.println("Unbelievable");
    }
}
```

下面的代码性能很差，因为sum是Long型的变量，它在与long型的变量i计算时，会经历反复的拆箱和装箱：

```java
// Hideously slow program! Can you spot the object creation?
public static void main(String[] args) {
    Long sum = 0L;
    for (long i = 0; i < Integer.MAX_VALUE; i++) {
        sum += i;
    }
    System.out.println(sum);
}
```

下列场合应该使用包装类型，而不能使用基本类型：

1. 作为容器中的元素、键和值。
2. 参数化的类型和方法的类型参数。
3. 在做反射方法调用时。



### 62、其他类型更合适时应避免使用字符串

字符串被设计用来表示文本。当在其他场景时，如果有更适合的其他类型，那么就应该避免使用字符串。

字符串是其他值类型的糟糕替代品。当从IO输入流读取数据时，通常是字符串的形式。但如果数据本质上是别的类型，如数值类型，那么就应该转换成合适的数值类型，如float、int等。

字符串是枚举类型的糟糕替代品。参见第34条中的讨论。

字符串是聚合类型的糟糕替代品。如果一个实体有多个组件，那么就不适合把它表示成单个字符串。如下所示：

```java
// Inappropriate use of string as aggregate type
String compoundKey = className + "#" + i.next();
```

有以下缺点：

1. 如果分隔符#出现在任何一个字段中，将会导致混乱。
2. 要访问各个字段，需要解析字符串，带来额外的性能代价。
3. 无法自行提供equals、toString和compareTo方法，只能使用String提供的相应方法。

更好的办法是把这个聚合实体表示成一个类，通常是私有静态成员类。

字符串不能很好地替代capabilities。例如在Java 1.2引入ThreadLocal前，有人提出了如下的线程局部变量设计，用字符串类型的键来标识每个线程局部变量：

```java
// Broken - inappropriate use of string as capability!
public class ThreadLocal {
    private ThreadLocal() { } // Noninstantiable

    // Sets the current thread's value for the named variable.
    public static void set(String key, Object value);

    // Returns the current thread's value for the named variable.
    public static Object get(String key);
}
```

这样设计的问题是：调用者提供的字符串键必须是惟一的。如果两位调用者恰巧为各自的线程局部变量使用了相同的字符串键，那么无意中就会共享一个变量，造成潜在的bug。

改进方案是用一个不可伪造的键（有时称为 capability）来替换字符串：

```java
public class ThreadLocal {
    private ThreadLocal() { } // Noninstantiable

    public static class Key { // (Capability)
        Key() { }
}

// Generates a unique, unforgeable key
public static Key getKey() {
    return new Key();
}

public static void set(Key key, Object value);

public static Object get(Key key);
}
```

做进一步的优化。键不再是线程局部变量的键值，而是成为线程局部变量本身：

```java
public final class ThreadLocal {
    public ThreadLocal();
    public void set(Object value);
    public Object get();
}
```

最后的优化是修改为支持泛型的类。这样能保证类型安全。

```java
public final class ThreadLocal<T> {
    public ThreadLocal();
    public void set(T value);
    public T get();
}
```



### 63、当心字符串连接引起的性能问题

使用字符串连接运算符（+）连接 n 个字符串需要 n 的平方级时间。因为连接两个字符串时，会复制这两个字符串的内容。

详情可以看这篇文章 [拼接字符串建议stringbuilder](https://www.seven97.top/java/basis/01-basic-knowledge.html#拼接字符串建议stringbuilder)

不要使用字符串连接符操作多个字符串，除非性能无关紧要。



### 64、通过接口引用对象

如果存在合适的接口类型，那么应该使用接口类型声明参数、返回值、变量和字段。下面例子遵循了这个准则：

```java
// Good - uses interface as type
Set<Son> sonSet = new LinkedHashSet<>();
```

而不应该像下面例子这样：

```java
// Bad - uses class as type!
LinkedHashSet<Son> sonSet = new LinkedHashSet<>();
```

使用接口作为类型会使程序更加灵活。例如可以灵活调整Set的实现：

```java
Set<Son> sonSet = new HashSet<>();
```

如果没有合适的接口存在，那么用类引用对象是完全合适的。 此时应使用类层次结构中提供所需功能最底层的类。



### 65、接口优于反射

反射机制java.lang.reflect提供对任意类的编程访问。反射提供的功能包括：

1. 获取类的成员名、字段类型、方法签名。
2. 构造类的实例，调用类的方法，访问类的字段。
3. 允许一个类使用另一个编译时还不存在的类。

但是反射也有一些缺点：

1. 失去了编译时类型检查的所有好处，包括异常检查。
2. 执行反射访问时所需的代码比普通代码更加冗长。
3. 反射方法调用比普通方法调用慢得多。

对于许多程序，它们必须用到在编译时无法获取的类。这时可以用反射创建实例，并通过它们的接口或超类访问它们。

下面例子是一个创建 `Set<String>` 实例的程序，类由第一个命令行参数指定，剩余的命令行参数被插入到集合中打印出来：

```java
// Reflective instantiation with interface access
public static void main(String[] args) {

    // Translate the class name into a Class object
    Class<? extends Set<String>> cl = null;
    try {
        cl = (Class<? extends Set<String>>) // Unchecked cast!
        Class.forName(args[0]);
    } catch (ClassNotFoundException e) {
        fatalError("Class not found.");
    }

    // Get the constructor
    Constructor<? extends Set<String>> cons = null;
    try {
        cons = cl.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
        fatalError("No parameterless constructor");
    }

    // Instantiate the set
    Set<String> s = null;
    try {
        s = cons.newInstance();
    } catch (IllegalAccessException e) {
        fatalError("Constructor not accessible");
    } catch (InstantiationException e) {
        fatalError("Class not instantiable.");
    } catch (InvocationTargetException e) {
        fatalError("Constructor threw " + e.getCause());
    } catch (ClassCastException e) {
        fatalError("Class doesn't implement Set");
    }

    // Exercise the set
    s.addAll(Arrays.asList(args).subList(1, args.length));
    System.out.println(s);
}

private static void fatalError(String msg) {
    System.err.println(msg);
    System.exit(1);
}
```



### 66、慎重使用本地方法

Java 本地接口（JNI）允许 Java 程序调用本地方法。本地方法有三种用途： 

1. 提供对特定于平台的功能（如注册表）的访问。 
2. 提供对现有本地代码库的访问。 
3. 通过本地语言编写应用程序中注重性能的部分，以提高性能。

使用本地方法访问特定于平台的机制是合法的，但是很少有必要。因为随着Java的发展，它不断提供以前只能在特定平台上才有的特性。

为了提高性能，很少建议使用本地方法。同样也是因为Java不断对底层类库做着优化，很多时候本地方法并未有明显性能优势。

使用本地方法有严重的缺点：

1. 不再免受内存毁坏错误的影响。
2. 可移植性较差，也更难调试。
3. 垃圾收集器无法自动跟踪本地内存使用情况。
4. 需要粘合代码，更难维护。



### 67、明智地进行优化

有三条关于优化的格言是每个人都应该知道的：

1. William A. Wulf：与任何其他单一原因（包括盲目愚蠢）相比，以效率为名（不一定能够实现）犯下的错误更多。
2. Donald E. Knuth：不要去计较效率上的一些小小的得失，在 97% 的情况下，不成熟的优化才是一切问题的根源。
3. M. A. Jackson：在你还没有绝对清晰的未优化方案之前，请不要进行优化。

以上格言说明：优化很容易弊大于利，过早的优化是万恶之源。

不要为了性能而牺牲合理的架构。努力编写好的程序，而不是快速的程序。 如果一个好的程序还不够快，那么再着手去优化。

尽量避免限制性能的设计决策。设计中最难更改是那些与外部世界交互的组件。例如API、线路协议和持久数据格式。这些组件可能对系统性能造成重大限制。

考虑API设计决策的性能后果。例如，如果一个公共类型是可变的，那么可能需要大量不必要的防御性拷贝。

通常情况下，好的 API 设计与好的性能是一致的。为了获得良好的性能而改变 API 是一个非常糟糕的想法。因为API的性能可能会在未来的版本有改进，但是改变API带来的问题却会一直存在。

在每次尝试优化之前和之后测量性能。自己的估计可能会和实际的测量结果有差距。有时候花大量时间做的优化没有明显的性能提升。



### 68、遵守被广泛认可的命名约定

命名约定分为两类：排版和语法。

排版约定包括：

1. 包名和模块名应该是分层的，组件之间用句点分隔。

2. 包名以当前组织的域名开头，并将组件颠倒过来，如com.google。包名其余部分应该由描述包的一个或多个组件组成，如util、awt。

3. 类和接口名称，包括枚举和注释类型名称，应该由一个或多个单词组成，每个单词的首字母大写，如List、FutureTask。

4. 方法和字段名遵循与类和接口名相同的排版约定，除了方法或字段名的第一个字母应该是小写，如如 remove、ensureCapacity。

5. 常量字段的名称应该由一个或多个大写单词组成，由下划线分隔，如NEGATIVE_INFINITY。

6. 局部变量名与成员名具有类似的排版命名约定，但允许缩写，如i、denom、houseNum。

7. 类型参数名通常由单个字母组成。最常见的是以下五种类型之一：T 表示任意类型，E 表示集合的元素类型，K 和 V 表示 Map 的键和值类型，X 表示异常，R表示函数的返回类型。

排版约定举例参见下表：

| 标识符类型 | 例子                                             |
| ---------- | ------------------------------------------------ |
| 包或者模块 | org.junit.jupiter.api, com.google.common.collect |
| 类或接口   | Stream, FutureTask, LinkedHashMap,HttpClient     |
| 方法或字段 | remove, groupingBy, getCrc                       |
| 常量字段   | MIN_VALUE, NEGATIVE_INFINITY                     |
| 局部变量   | i, denom, houseNum                               |
| 类型参数   | T, E, K, V, X, R, U, V, T1, T2                   |

语法约定包括：

1. 实例化的类，包括枚举类型，通常使用一个或多个名词短语来命名，例如 Thread、PriorityQueue 或 ChessPiece。
2. 不可实例化的实用程序类通常使用复数名词来命名，例如 Collectors 或 Collections。
3. 接口的名称类似于类，例如 Collection 或 Comparator，或者以 able 或 ible 结尾的形容词，例如 Runnable、Iterable 或 Accessible。
4. 执行某些操作的方法通常用动词或动词短语命名，例如，append 或 drawImage。
5. 返回布尔值的方法的名称通常以单词 is 或 has开头，后面跟一个名词或者形容词，例如 isDigit、isProbablePrime、isEmpty、isEnabled 或 hasSiblings。
6. 获取对象属性的方法通常使用以 get 开头的名词、名词短语或动词短语来命名，例如 size、hashCode 或 getTime。
7. 类型为 boolean 的字段的名称通常类似于 boolean 访问器方法，省略了初始值 is，例如 initialized、composite。

详情可以看 [阿里巴巴Java开发手册(黄山版)](https://www.seven97.top/books/software-quality/alibaba-developmentmanual.html)



## 异常

### 9、使用 try-with-resources 替代 try-finally 

原因关注 [异常详解](https://www.seven97.top/java/basis/04-exceptions.html#try-with-resources)

 

### 69、仅在有异常条件时使用异常

下面的代码用来遍历一个数组。不过写法很糟糕，需要仔细读才能弄懂用意：

```java
// Horrible abuse of exceptions. Don't ever do this!
try {
    int i = 0;
    while(true)
        range[i++].climb();
    }
    catch (ArrayIndexOutOfBoundsException e) {
}
```

正常的写法应该是：

```java
for (Mountain m : range)
    m.climb();
```

这样的写的原因可能是代码编写者误认为标准for-each循环做结束判断性能不佳，应该使用异常机制来提升性能。这种思路有三点误区：

1. 异常是为特殊场合设计的，所以 JVM 实现几乎不会让它们像显式判断一样快。
2. 将代码放在 try-catch 块中会抑制 JVM 可能执行的某些优化。
3. 遍历数组的标准习惯用法不一定会导致冗余检查。许多 JVM 实现对它们进行了优化。

实际上，建立一个异常对象，是建立一个普通Object耗时的约20倍，详情可以看这篇文章[异常耗时](https://www.seven97.top/java/basis/04-exceptions.html#异常耗时)

并且使用异常做循环终止，性能远低于标准用法。而且这样做还可能导致对真正数组越界报错的掩盖。所以，异常只适用于确实有异常的情况，它们不应该用于一般的控制流程。

API的设计也应该遵循同样的思路。例如，迭代器中应同时提供hasNext()和next()方法，我们称next()为“状态依赖”的，因为它需要通过一个“状态测试”的方法hasNext()才能判断调用是否合法。我们通过hasNext()判断循环是否应该终止：

```java
for (Iterator<Foo> i = collection.iterator(); i.hasNext(); ) {
    Foo foo = i.next();
    ...
}
```

而不应该使用异常来终止循环：

```java
// Do not use this hideous code for iteration over a collection!
try {
    Iterator<Foo> i = collection.iterator();
    while(true) {
        Foo foo = i.next();
        ...
    }
}
catch (NoSuchElementException e) {
}
```

除了提供“状态测试”，另一种设计思路是让“状态依赖”的方法返回一个Optional对象，或者在不能执行计算时返回null。



### 70、对可恢复情况使用受检异常，对编程错误使用运行时异常

Java 提供了三种可抛出项：受检异常（checked exception）、运行时异常（runtime exception）和错误（error）。

使用受检异常的情况是为了期望调用者能够从中恢复。其他两种可抛出项都是非受检的。

使用运行时异常来表示编程错误。 例如数组越界ArrayIndexOutOfBoundsException。如果对于选择受检异常还是运行时异常有疑问，那么推荐还是使用运行时异常。

错误保留给 JVM 使用，用于表示：资源不足、不可恢复故障或其他导致无法继续执行的条件。不要自己定义新的错误类型。

详情请看[异常详解](https://www.seven97.top/java/basis/04-exceptions.html)



### 71、避免不必要地使用受检异常

使用受检异常应满足：正确使用 API 也不能防止异常情况，并且使用 API 在遇到异常时可以采取一些有用的操作；否则应使用非受检异常。

```java
} catch (TheCheckedException e) {
    throw new AssertionError(); // Can't happen!
}
} catch (TheCheckedException e) {
    e.printStackTrace(); // Oh well, we lose.
    System.exit(1);
}
```

以上两种处理受检异常的方式都很糟糕。

受检异常会给程序员带来额外负担，消除受检异常的最简单方法是返回所需结果类型的 Optional 对象，当存在受检异常时返回空的Optional对象。这种方法缺点是无法提供附加信息说明为何不能继续执行计算。

另一种消除受检异常的方法是拆分方法逻辑。例如下面的方法原本需要捕获受检异常：

```java
// Invocation with checked exception
try {
    obj.action(args);
}
catch (TheCheckedException e) {
    ... // Handle exceptional condition
}
```

可以将其逻辑拆分：若参数合法，则走第一部分无受检异常的逻辑；否则走第二部分处理异常条件的逻辑。

```java
// Invocation with state-testing method and unchecked exception
if (obj.actionPermitted(args)) {
    obj.action(args);
}
else {
    ... // Handle exceptional condition
}
```

如果我们确定调用一定成功，或者不介意调用失败导致线程中止，甚至可以简化逻辑为下面语句：

```java
obj.action(args);
```



### 72、鼓励复用标准异常

Java 库提供了一组标准异常，涵盖了日常的大多数异常抛出需求。

复用异常的好处是使代码易于阅读和维护。

此表总结了最常见的可复用异常：

| Exception                       | Occasion for Use                       |
| ------------------------------- | -------------------------------------- |
| IllegalArgumentException        | 非null参数值不合适                     |
| IllegalStateException           | 对象状态不适用于方法调用               |
| NullPointerException            | 禁止参数为null时仍传入 null            |
| IndexOutOfBoundsException       | 索引参数值超出范围                     |
| ConcurrentModificationException | 在禁止并发修改对象的地方检测到并发修改 |
| UnsupportedOperationException   | 对象不支持该方法调用                   |

不要直接复用 Exception、RuntimeException、Throwable 或 Error，应当将这些类当做抽象类。实际使用的异常类应该是这些类的继承类。

异常复用必须基于文档化的语义，而不仅仅是基于名称。另外，如果你想添加更多的细节，可以子类化标准异常。



### 73、抛出与抽象级别相匹配的异常

为了保证抽象的层次性，高层应该捕获低层异常，并确保抛出的异常可以用高层抽象解释。 这个习惯用法称为异常转换：

```java
// Exception Translation
try {
    ... // Use lower-level abstraction to do our bidding
} catch (LowerLevelException e) {
    throw new HigherLevelException(...);
}
```

下面是来自 AbstractSequentialList 类的异常转换示例：

```java
/**
* Returns the element at the specified position in this list.
* @throws IndexOutOfBoundsException if the index is out of range
* ({@code index < 0 || index >= size()}).
*/
public E get(int index) {
    ListIterator<E> i = listIterator(index);
    try {
        return i.next();
    }
    catch (NoSuchElementException e) {
        throw new IndexOutOfBoundsException("Index: " + index);
    }
}
```

如果低层异常有助于调试高层异常的问题，那么需要一种称为链式异常的特殊异常转换形式。低层异常作为原因传递给高层异常，高层异常提供一个访问器方法（Throwable 的 getCause 方法）来访问低层异常：

```java
// Exception Chaining
try {
    ... // Use lower-level abstraction to do our bidding
}
catch (LowerLevelException cause) {
    throw new HigherLevelException(cause);
}
```

这个链式异常的实现代码如下：

```java
// Exception with chaining-aware constructor
class HigherLevelException extends Exception {
    HigherLevelException(Throwable cause) {
        super(cause);
    }
}
```

虽然异常转换可以有助于屏蔽低层异常，但不应被滥用。更好的办法是确保低层方法避免异常，例如在将高层方法的参数传递到低层之前检查它们的有效性。

另一种让屏蔽低层异常的方法是：让高层静默处理这些异常。例如可以使用一些适当的日志工具（如 java.util.logging）来记录异常。



### 74、为每个方法记录会抛出的所有异常

仔细记录每个方法抛出的所有异常是非常重要的。

始终单独声明受检异常，并使用 Javadoc 的 @throw 标记精确记录每次抛出异常的条件。

使用 Javadoc 的 @throw 标记记录方法会抛出的每个异常，但是不要对非受检异常使用 throws 关键字。

如果一个类中的许多方法都因为相同的原因抛出异常，你可以在类的文档注释中记录异常， 而不是为每个方法单独记录异常。例如，在类的文档注释中可以这样描述NullPointerException：“如果在任何参数中传递了 null 对象引用，该类中的所有方法都会抛出 NullPointerException”。



### 75、详细消息中应包含失败捕获的信息

要捕获失败，异常的详细消息应该包含导致异常的所有参数和字段的值。例如，IndexOutOfBoundsException 的详细消息应该包含下界、上界和未能位于下界之间的索引值。

详细消息中不应包含密码、加密密钥等敏感信息。

确保异常在其详细信息中包含足够的故障捕获信息的一种方法是，在其构造函数中配置，而不是以传入字符串方式引入这些信息：

```java
/**
* Constructs an IndexOutOfBoundsException.
**
@param lowerBound the lowest legal index value
* @param upperBound the highest legal index value plus one
* @param index the actual index value
*/
public IndexOutOfBoundsException(int lowerBound, int upperBound, int index) {
    // Generate a detail message that captures the failure
    super(String.format("Lower bound: %d, Upper bound: %d, Index: %d",lowerBound, upperBound, index));
    // Save failure information for programmatic access
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
    this.index = index;
}
```



### 76、尽力保证故障原子性

失败的方法调用应该使对象能恢复到调用之前的状态。 具有此属性的方法称为具备故障原子性。

保证故障原子性的方法有如下几种： 

1. 设计不可变对象。

2. 对于操作可变对象的方法，在执行操作之前检查参数的有效性：
   ```java
   public Object pop() {
       if (size == 0)
           throw new EmptyStackException();
       Object result = elements[--size];
       elements[size] = null; // Eliminate obsolete reference
       return result;
   }
   ```

3. 对计算进行排序，以便可能发生故障的部分都先于修改对象的部分发生。

4. 以对象的临时副本执行操作，并在操作完成后用临时副本替换对象的内容。

1. 编写恢复代码，拦截在操作过程中发生的故障，并使对象回滚到操作开始之前的状态。这种方法主要用于持久的（基于磁盘的）数据结构。

故障原子性并不总是可以实现的。例如，多线程修改同一个容器类对象，导致抛出ConcurrentModificationException，此时是不可恢复的。



### 77、不要忽略异常

以上的空 catch 块违背了异常的目的， 异常的存在是为了强制你处理异常情况。

```java
// Empty catch block ignores exception - Highly suspect!
try {
    ...
}
catch (SomeException e) {
}
```



在某些情况下忽略异常是合适的，例如在关闭 FileInputStream 时。你没有更改文件的状态，因此不需要执行任何恢复操作，也没有理由中止正在进行的操作。也可选择记录异常。如果你选择忽略异常，catch 块应该包含一条注释，解释为什么这样做是合适的，并且应该将变量命名为 ignore：

```java
Future<Integer> f = exec.submit(planarMap::chromaticNumber);
int numColors = 4; // Default; guaranteed sufficient for any map
try {
    numColors = f.get(1L, TimeUnit.SECONDS);
}
catch (TimeoutException | ExecutionException ignored) {
    // Use default: minimal coloring is desirable, not required
}
```



## 并发

###  78、对共享可变数据的同步访问

同步不仅能防止线程修改的对象处于不一致状态，而且保证每个线程修改的结果为其他线程可见。

线程之间能可靠通信以及实施互斥，同步是所必需的。

即使是原子读写，没有设置同步也会造成糟糕的后果。下面代码展示从一个线程中使另一个线程停止，不要使用 Thread.stop，而是通过设置标志变量：

```java
// Broken! - How long would you expect this program to run?
public class StopThread {
    private static boolean stopRequested;

    public static void main(String[] args) throws InterruptedException {
        Thread backgroundThread = new Thread(() -> {
        int i = 0;
        while (!stopRequested)
            i++;
        });

    backgroundThread.start();
    TimeUnit.SECONDS.sleep(1);
    stopRequested = true;
    }
}
```

在某些机器上，线程永远不会停止。这是因为在没有同步的情况下，虚拟机可能会将下面代码

```java
while (!stopRequested)
    i++;
```

优化为如下代码：

```java
if (!stopRequested)
    while (true)
        i++;
```

解决上面问题的办法是对stopRequested变量做同步读写，程序会立即结束：

```java
// Properly synchronized cooperative thread termination
public class StopThread {
    private static boolean stopRequested;

    private static synchronized void requestStop() {
        stopRequested = true;
    }

    private static synchronized boolean stopRequested() {
        return stopRequested;
    }

    public static void main(String[] args) throws InterruptedException {
        Thread backgroundThread = new Thread(() -> {
            int i = 0;
            while (!stopRequested())
            i++;
        });

        backgroundThread.start();
        TimeUnit.SECONDS.sleep(1);
        requestStop();
    }
}
```

仅同步写方法是不够的。除非读和写操作都同步，否则不能保证同步生效。

一种更简单、更高效的做法是使用[volatile](https://www.seven97.top/java/concurrent/02-keyword2-volatile.html)。虽然 volatile 不保证互斥，但是它保证任何读取字段的线程都会看到最近修改的值：

```java
// Cooperative thread termination with a volatile field
public class StopThread {
    private static volatile boolean stopRequested;

    public static void main(String[] args) throws InterruptedException {
        Thread backgroundThread = new Thread(() -> {
        int i = 0;
        while (!stopRequested)
            i++;
    });

    backgroundThread.start();
    TimeUnit.SECONDS.sleep(1);
    stopRequested = true;
    }
}
```

注意volatile不保证变量读写的原子性，因此下面的代码不能保证每次生成的序列号是严格递增的：

```java
// Broken - requires synchronization!
private static volatile int nextSerialNumber = 0;

public static int generateSerialNumber() {
    return nextSerialNumber++;
}
```

解决办法是使用原子变量，如[原子类atomic](https://www.seven97.top/java/concurrent/03-juclock1-cas-unsafe-atomic.html#原子类atomic)：

```java
// Lock-free synchronization with java.util.concurrent.atomic
private static final AtomicLong nextSerialNum = new AtomicLong();

public static long generateSerialNumber() {
    return nextSerialNum.getAndIncrement();
}
```

为避免出现本条目中出现的问题，最好办法是不共享可变数据。应当将可变数据限制在一个线程中。



### 79、避免过度同步

为避免活性失败和安全故障，永远不要在同步方法或块中将控制权交给用户。

为了展示这个问题，下面的代码实现了观察者模式，当元素被添加到集合中时，允许用户订阅通知：

```java
// Broken - invokes alien method from synchronized block!
public class ObservableSet<E> extends ForwardingSet<E> {
    public ObservableSet(Set<E> set) { super(set); }

    private final List<SetObserver<E>> observers= new ArrayList<>();

    public void addObserver(SetObserver<E> observer) {
        synchronized(observers) {
            observers.add(observer);
        }
    }

    public boolean removeObserver(SetObserver<E> observer) {
        synchronized(observers) {
            return observers.remove(observer);
        }
    }

    private void notifyElementAdded(E element) {
        synchronized(observers) {
            for (SetObserver<E> observer : observers)
                observer.added(this, element);
        }
    }

    @Override
    public boolean add(E element) {
        boolean added = super.add(element);
        if (added)
            notifyElementAdded(element);
        return added;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean result = false;
        for (E element : c)
            result |= add(element); // Calls notifyElementAdded
        return result;
    }
}
```

观察者通过调用 addObserver 方法订阅通知，调用 removeObserver 方法取消订阅：

```java
@FunctionalInterface
public interface SetObserver<E> {
    // Invoked when an element is added to the observable set
    void added(ObservableSet<E> set, E element);
}
```

粗略地检查一下，ObservableSet 似乎工作得很好。例如，打印从 0 到 99 的数字：

```java
public static void main(String[] args) {
    ObservableSet<Integer> set =new ObservableSet<>(new HashSet<>());
    set.addObserver((s, e) -> System.out.println(e));
    for (int i = 0; i < 100; i++)
        set.add(i);
}
```

现在让我们尝试一些更有想象力的事情。假设我们将 addObserver 调用替换为一个传递观察者的调用，该观察者打印添加到集合中的整数值，如果该值为 23，则该调用将删除自身：

```java
set.addObserver(new SetObserver<>() {
    public void added(ObservableSet<Integer> s, Integer e) {
        System.out.println(e);
        if (e == 23)
            s.removeObserver(this);
    }
});
```

你可能期望这个程序会打印数字0到23，然后终止。但是实际上它会抛出ConcurrentModificationException，虽然我们对observers加了并发，也无法阻止对它的并发修改。

现在让我们尝试一些奇怪的事情：编写一个观察者来取消订阅，但是它没有直接调用 removeObserver，而是使用executor启动另一个线程来执行：

```java
// Observer that uses a background thread needlessly
set.addObserver(new SetObserver<>() {
    public void added(ObservableSet<Integer> s, Integer e) {
        System.out.println(e);
        if (e == 23) {
            ExecutorService exec = Executors.newSingleThreadExecutor();
            try {
                exec.submit(() -> s.removeObserver(this)).get();
            } catch (ExecutionException | InterruptedException ex) {
                throw new AssertionError(ex);
            } finally {
                exec.shutdown();
            }
        }
    }
});
```

结果是它不会抛出异常，而是形成死锁，原因是主线程调用addObserver后一直持有observers锁并等待子线程执行完毕，可是子线程调用removeObserver也需要获取observers锁，形成循环依赖。

一种解决办法是把遍历集合的代码移到同步块以外：

```java
// Alien method moved outside of synchronized block - open calls
private void notifyElementAdded(E element) {
    List<SetObserver<E>> snapshot = null;
    synchronized(observers) {
        snapshot = new ArrayList<>(observers);
    }
    for (SetObserver<E> observer :snapshot)
        observer.added(this, element);
}
```

另一种更好的办法是使用CopyOnWriteArrayList，适合用在很少修改和经常遍历的场合：

```java
// Thread-safe observable set with CopyOnWriteArrayList
private final List<SetObserver<E>> observers =new CopyOnWriteArrayList<>();

public void addObserver(SetObserver<E> observer) {
    observers.add(observer);
}

public boolean removeObserver(SetObserver<E> observer) {
    return observers.remove(observer);
}

private void notifyElementAdded(E element) {
    for (SetObserver<E> observer : observers)
        observer.added(this, element);
}
```

应该在同步区域内做尽可能少的工作，将耗时的代码移出同步块。

对一些Java类库的选择：

1. 以java.util.concurrent中的类取代Vector和Hashtable。
2. 在单线程环境下，以StringBuilder取代StringBuffer。
3. 在单线程环境下，以ThreadLocalRandom取代Random。



### 80、Executor、task、stream优于直接使用线程

Executor框架使用非常方便：

```java
ExecutorService exec = Executors.newSingleThreadExecutor();
exec.execute(runnable);
exec.shutdown();
```

应该使用Executor，而非直接使用线程。后者既是工作单元，又是执行机制；前者做了对工作单元和执行机制做了很好的分离，可以根据实际情况灵活选择执行机制。



### 81、并发实用工具优于wait-notify

直接使用wait-notify就像使用“并发汇编语言”编程一样原始，你应该使用更高级别的并发实用工具。

并发集合接口配备了依赖于状态的修改操作，这些操作将多个基本操作组合成单个原子操作。例如下面例子演示了Map的putIfAbsent(key, value)方法的使用，用于模拟实现String.intern的行为：

```java
// Concurrent canonicalizing map atop ConcurrentMap - not optimal
private static final ConcurrentMap<String, String> map =new ConcurrentHashMap<>();
public static String intern(String s) {
    String previousValue = map.putIfAbsent(s, s);
    return previousValue == null ? s : previousValue;
}
```

事实上可以进一步优化。ConcurrentHashMap 针对 get 等检索操作进行了优化。因此，只有在 get 表明有必要时，才值得调用 putIfAbsent:

```java
// Concurrent canonicalizing map atop ConcurrentMap - faster!
public static String intern(String s) {
    String result = map.get(s);
    if (result == null) {
        result = map.putIfAbsent(s, s);
        if (result == null)
        result = s;
    }
    return result;
}
```

使用并发集合而非同步集合。例如，使用 ConcurrentHashMap 而不是 Collections.synchronizedMap。

下面例子展示了如何构建一个简单的框架来为一个操作的并发执行计时。在 wait 和 notify 的基础上直接实现这种逻辑会有点麻烦，但是在 CountDownLatch 的基础上实现起来却非常简单：

```java
// Simple framework for timing concurrent execution
public static long time(Executor executor, int concurrency,Runnable action) throws InterruptedException {
    CountDownLatch ready = new CountDownLatch(concurrency);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(concurrency);

    for (int i = 0; i < concurrency; i++) {
        executor.execute(() -> {
            ready.countDown(); // Tell timer we're ready
            try {
                start.await(); // Wait till peers are ready
                action.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown(); // Tell timer we're done
            }
        });
    }

    ready.await(); // Wait for all workers to be ready
    long startNanos = System.nanoTime();
    start.countDown(); // And they're off!
    done.await(); // Wait for all workers to finish
    return System.nanoTime() - startNanos;
}
```

对于间隔计时，始终使用 System.nanoTime 而不是 System.currentTimeMillis。 System.nanoTime 不仅更准确和精确，而且不受系统实时时钟调整的影响。

有时候维护老代码还需要使用wait-notify。下面是wait-notify的基本用法：

```java
// The standard idiom for using the wait method
synchronized (obj) {
    while (<condition does not hold>)
        obj.wait(); // (Releases lock, and reacquires on wakeup)
    ... // Perform action appropriate to condition
}
```

始终使用循环来调用 wait 方法。 notifyAll 方法通常应该优先于 notify。如果使用 notify，则必须非常小心以确保其活性。



### 82、使线程安全文档化

每个类都应该详细描述或使用线程安全注解记录其线程安全属性。

不要依赖synchronized修饰符来记录线程安全，它只是实现细节，而不是API的一部分，不能可靠表明方法是线程安全的。

类必须仔细地记录它支持的线程安全级别，级别可分为：

1. 不可变的：常量，不需要同步，如String、Integer。
2. 无条件线程安全：可变但有足够的内部同步，无需依赖外部同步，如AtomicLong和ConcurrentHashMap。
3. 有条件的线程安全：与无条件线程安全类似，但有些方法需要外部同步才能使用，如Collections.synchronized包装器返回的集合，其迭代器需要外部同步。
4. 非线程安全：可变，需使用外部同步包围每个方法调用，例如ArrayList和HashMap。
5. 线程对立：即使每个方法调用都被外部同步包围，也不是线程安全的。

在记录一个有条件的线程安全类时需要小心，要指出哪些调用方法需要外部同步。如Collections.synchronizedMap在遍历集合时，需要在map上做同步：

```java
Map<K, V> m = Collections.synchronizedMap(new HashMap<>());
Set<K> s = m.keySet(); // Needn't be in synchronized block
...
synchronized(m) { // Synchronizing on m, not s!
    for (K key : s)
        key.f();
}
```

为了防止用户恶意长期持有公开锁，可以使用私有锁，并放在对外提供的方法内部：

```java
// Private lock object idiom - thwarts denial-of-service attack
private final Object lock = new Object();
public void foo() {
    synchronized(lock) {
        ...
    }
}
```

Lock 字段应该始终声明为 final，防止无意中对其修改。



### 83、明智地使用延迟初始化

在大多数情况下，常规初始化优于延迟初始化。延迟初始化只有在必要时才这么做。

延迟初始化适合的场景：如果一个字段只在类的一小部分实例上访问，并且初始化该字段的代价很高，那么可以考虑延迟初始化。如 [单例模式](https://www.seven97.top/system-design/design-pattern/singletonpattern.html)

在多线程竞争的情况下，使用延迟初始化容易导致错误。

下面是一个常规初始化的例子：

```java
// Normal initialization of an instance field
private final FieldType field = computeFieldValue();
```

改成延迟初始化的版本，需使用synchronized同步：

```java
// Lazy initialization of instance field - synchronized accessor
private FieldType field;
private synchronized FieldType getField() {
    if (field == null)
        field = computeFieldValue();
    return field;
}
```

如果需要在静态字段上使用延迟初始化提升性能，请使用延迟初始化持有类模式：

```java
// Lazy initialization holder class idiom for static fields
private static class FieldHolder {
    static final FieldType field = computeFieldValue();
}
private static FieldType getField() { return FieldHolder.field; }
```

如果需要在实例字段上使用延迟初始化提升性能，请使用双重检查模式：

```java
// Double-check idiom for lazy initialization of instance fields
private volatile FieldType field;
private FieldType getField() {
    FieldType result = field;
    if (result == null) { // First check (no locking)
        synchronized(this) {
            if (field == null) // Second check (with locking)
                field = result = computeFieldValue();
        }
    }
    return result;
}
```

如果可以容忍重复初始化，可以改为单检查模式：

```java
// Single-check idiom - can cause repeated initialization!
private volatile FieldType field;
private FieldType getField() {
    FieldType result = field;
    if (result == null)
        field = result = computeFieldValue();
    return result;
}
```



### 84、不要依赖线程调度器

任何依赖线程调度器来保证正确性或性能的程序都无法保证可移植性。

线程不应该在循环中检查共享对象状态变化。除了使程序容易受到线程调度器变化无常的影响之外，循环检查状态变化还大幅增加了处理器的负载，并影响其他线程获取处理器进行工作。例如下面实现的CountDownLatch版本性能很糟糕：

```java
// Awful CountDownLatch implementation - busy-waits incessantly!
public class SlowCountDownLatch {

    private int count;

    public SlowCountDownLatch(int count) {
        if (count < 0)
            throw new IllegalArgumentException(count + " < 0");
        this.count = count;
    }

    public void await() {
        while (true) {
            synchronized(this) {
                if (count == 0)
                return;
            }
        }
    }

    public synchronized void countDown() {
        if (count != 0)
            count--;
    }
}
```

当有1000个线程竞争时，上面例子的执行时间是Java中CountDownLatch的10倍。

通过调用Thread.yield来优化上面的程序，可以勉强让程序运行起来，但它是不可移植的。更好的做法是重构代码，减少并发线程的数量。

类似地，不要依赖调整线程优先级。线程优先级是 Java 中最不可移植的特性之一。

 

## 序列化

### 85、优先选择 Java 序列化的替代方案

当序列化特性被引入Java时，它还从未在生产语言中出现过。当时的设计者评估认为收益大于风险。

后来的历史证明并非如此。序列化导致了严重的安全漏洞，而且由于它的可攻击范围太大，难以防范，问题还在不断增多。

下面例子演示了一个“反序列化炸弹”，反序列化需要执行很长时间：

```java
// Deserialization bomb - deserializing this stream takes forever
static byte[] bomb() {
    Set<Object> root = new HashSet<>();
    Set<Object> s1 = root;
    Set<Object> s2 = new HashSet<>();
    for (int i = 0; i < 100; i++) {
        Set<Object> t1 = new HashSet<>();
        Set<Object> t2 = new HashSet<>();
        t1.add("foo"); // Make t1 unequal to t2
        s1.add(t1); s1.add(t2);
        s2.add(t1); s2.add(t2);
        s1 = t1;
        s2 = t2;
    }
    return serialize(root); // Method omitted for brevity
}
```

避免序列化漏洞的最好方法是永远不要反序列化任何东西。没有理由在你编写的任何新系统中使用Java序列化。

用于取代Java序列化，领先的跨平台结构化数据是JSON和Protobuf。

如果需要在老系统中使用序列化，那么永远不要反序列化不可信的数据。

Java 9中添加的对象反序列化筛选机制，允许接收或拒绝某些类。优先选择白名单而不是黑名单， 因为黑名单只保护你免受已知的威胁。



### 86、非常谨慎地实现Serializable

实现Serializable接口会带来以下代价： 

1. 一旦类的实现被发布，它就会降低更改该类实现的灵活性。需要永远支持序列化的形式。
2. 增加了出现 bug 和安全漏洞的可能性。 
3. 它增加了与发布类的新版本相关的测试负担。

实现 Serializable 接口并不是一个轻松的决定。

为继承而设计的类很少情况适合实现 Serializable 接口，接口也很少适合继承Serializable。

内部类不应该实现 Serializable。



### 87、考虑使用自定义序列化形式

 在没有考虑默认序列化形式是否合适之前，不要接受它。

如果对象的物理表示与其逻辑内容相同，则默认的序列化形式可能是合适的。如下面的类表示一个人的名字：

```java
// Good candidate for default serialized form
public class Name implements Serializable {
    /**
    * Last name. Must be non-null.
    * @serial
    */
    private final String lastName;

    /**
    * First name. Must be non-null.
    * @serial
    */
    private final String firstName;

    /**
    * Middle name, or null if there is none.
    * @serial
    */
    private final String middleName;
    ... // Remainder omitted
}
```

即使你认为默认的序列化形式是合适的，你通常也必须提供readObject方法来确保不变性和安全性。

下面的例子不适合使用默认序列化，因为它会镜像出链表中的所有项，以及这些项之间的双向链接：

```java
// Awful candidate for default serialized form
public final class StringList implements Serializable {
    private int size = 0;
    private Entry head = null;
    private static class Entry implements Serializable {
        String data;
        Entry next;
        Entry previous;
    }
    ... // Remainder omitted
}
```

当对象的物理表示与其逻辑数据内容有很大差异时，使用默认的序列化形式有四个缺点： 

1. 将导出的 API 永久地绑定到当前的内部实现。 
2. 占用过多的空间。 
3. 消耗过多的时间。 
4. 可能导致堆栈溢出。

StringList的合理序列化形式是列表中的字符串数量和字符串本身。这构成了由StringList表示的逻辑数据，去掉了其物理表示的细节。下面是修改后的StringList版本，其中transient修饰符表示要从类的默认序列化中省略该实例字段：

```java
// StringList with a reasonable custom serialized form
public final class StringList implements Serializable {
    private transient int size = 0;
    private transient Entry head = null;
    // No longer Serializable!

    private static class Entry {
        String data;
        Entry next;
        Entry previous;
    }
    // Appends the specified string to the list
    public final void add(String s) { ... }

    /**
    * Serialize this {@code StringList} instance.
    **
    @serialData The size of the list (the number of strings
    * it contains) is emitted ({@code int}), followed by all of
    * its elements (each a {@code String}), in the proper
    * sequence.
    */
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(size);
        // Write out all elements in the proper order.
        for (Entry e = head; e != null; e = e.next)
            s.writeObject(e.data);
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        int numElements = s.readInt();
        // Read in all elements and insert them in list
        for (int i = 0; i < numElements; i++)
            add((String) s.readObject());
    }

    ... // Remainder omitted
}
```

必须对对象序列化强制执行任何同步操作，就如同对读取对象整个状态的任何其他方法那样强制执行：

```java
// writeObject for synchronized class with default serialized form
private synchronized void writeObject(ObjectOutputStream s) throws IOException {
    s.defaultWriteObject();
}
```

无论选择哪种序列化形式，都要在编写的每个可序列化类中声明显式的序列化版本UID：

```java
private static final long serialVersionUID = randomLongValue;
```

不要更改序列化版本UID，除非你想破坏与现有序列化所有实例的兼容性。



### 88、防御性地编写readObject方法

第50条中编写了一个包含Date字段的不变日期范围类，它通过防御性地复制Date对象来保证其不变性：

```java
// Immutable class that uses defensive copying
public final class Period {
    private final Date start;
    private final Date end;

    /**
    * @param start the beginning of the period
    * @param end the end of the period; must not precede start
    * @throws IllegalArgumentException if start is after end
    * @throws NullPointerException if start or end is null
    */
    public Period(Date start, Date end) {
        this.start = new Date(start.getTime());
        this.end = new Date(end.getTime());
        if (this.start.compareTo(this.end) > 0)
            throw new IllegalArgumentException(start + " after " + end);
    }

    public Date start () { return new Date(start.getTime()); }

    public Date end () { return new Date(end.getTime()); }

    public String toString() { return start + " - " + end; }

    ... // Remainder omitted
}
```

如果我们让这个类支持Java默认的序列化，那么会产生漏洞：可以从字节流反序列出一个有问题的对象，其结束时间小于开始时间，绕过原构造方法做的限制：

```java
public class BogusPeriod {
// Byte stream couldn't have come from a real Period instance!
    private static final byte[] serializedForm = {
        (byte)0xac, (byte)0xed, 0x00, 0x05, 0x73, 0x72, 0x00, 0x06,
        0x50, 0x65, 0x72, 0x69, 0x6f, 0x64, 0x40, 0x7e, (byte)0xf8,
        0x2b, 0x4f, 0x46, (byte)0xc0, (byte)0xf4, 0x02, 0x00, 0x02,
        0x4c, 0x00, 0x03, 0x65, 0x6e, 0x64, 0x74, 0x00, 0x10, 0x4c,
        0x6a, 0x61, 0x76, 0x61, 0x2f, 0x75, 0x74, 0x69, 0x6c, 0x2f,
        0x44, 0x61, 0x74, 0x65, 0x3b, 0x4c, 0x00, 0x05, 0x73, 0x74,
        0x61, 0x72, 0x74, 0x71, 0x00, 0x7e, 0x00, 0x01, 0x78, 0x70,
        0x73, 0x72, 0x00, 0x0e, 0x6a, 0x61, 0x76, 0x61, 0x2e, 0x75,
        0x74, 0x69, 0x6c, 0x2e, 0x44, 0x61, 0x74, 0x65, 0x68, 0x6a,
        (byte)0x81, 0x01, 0x4b, 0x59, 0x74, 0x19, 0x03, 0x00, 0x00,
        0x78, 0x70, 0x77, 0x08, 0x00, 0x00, 0x00, 0x66, (byte)0xdf,
        0x6e, 0x1e, 0x00, 0x78, 0x73, 0x71, 0x00, 0x7e, 0x00, 0x03,
        0x77, 0x08, 0x00, 0x00, 0x00, (byte)0xd5, 0x17, 0x69, 0x22,
        0x00, 0x78
    };

    public static void main(String[] args) {
        Period p = (Period) deserialize(serializedForm);
        System.out.println(p);
    }

    // Returns the object with the specified serialized form
    static Object deserialize(byte[] sf) {
        try {
            return new ObjectInputStream(new ByteArrayInputStream(sf)).readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
```

解决办法是在readObject方法中检查反序列化对象的有效性：

```java
// readObject method with validity checking - insufficient!
private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    // Check that our invariants are satisfied
    if (start.compareTo(end) > 0)
        throw new InvalidObjectException(start +" after "+ end);
}
```

然而还有另外的问题。攻击者可以通过反序列化访问Period对象中原有的私有Date字段。通过修改这些Date实例，攻击者可以修改Period实例：

```java
public class MutablePeriod {
    // A period instance
    public final Period period;

    // period's start field, to which we shouldn't have access
    public final Date start;

    // period's end field, to which we shouldn't have access
    public final Date end;

    public MutablePeriod() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);

            // Serialize a valid Period instance
            out.writeObject(new Period(new Date(), new Date()));

            /*
            * Append rogue "previous object refs" for internal
            * Date fields in Period. For details, see "Java
            * Object Serialization Specification," Section 6.4.
            */
            byte[] ref = { 0x71, 0, 0x7e, 0, 5 }; // Ref #5
            bos.write(ref); // The start field
            ref[4] = 4; // Ref # 4
            bos.write(ref); // The end field

            // Deserialize Period and "stolen" Date references
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
            period = (Period) in.readObject();
            start = (Date) in.readObject();
            end = (Date) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }
}
```

在作者的机器上，可以产生如下输出：

```java
Wed Nov 22 00:21:29 PST 2017 - Wed Nov 22 00:21:29 PST 1978
Wed Nov 22 00:21:29 PST 2017 - Sat Nov 22 00:21:29 PST 1969
public static void main(String[] args) {
    MutablePeriod mp = new MutablePeriod();
    Period p = mp.period;
    Date pEnd = mp.end;

    // Let's turn back the clock
    pEnd.setYear(78);
    System.out.println(p);

    // Bring back the 60s!
    pEnd.setYear(69);
    System.out.println(p);
}
Wed Nov 22 00:21:29 PST 2017 - Wed Nov 22 00:21:29 PST 1978
Wed Nov 22 00:21:29 PST 2017 - Sat Nov 22 00:21:29 PST 1969
```

对象被反序列化时，对任何私有字段进行防御性地复制至关重要。例如：

```java
// readObject method with defensive copying and validity checking
private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    // Defensively copy our mutable components
    start = new Date(start.getTime());
    end = new Date(end.getTime());
    // Check that our invariants are satisfied
    if (start.compareTo(end) > 0)
        throw new InvalidObjectException(start +" after "+ end);
}
```

修改后会产生如下输出：

```java
Wed Nov 22 00:23:41 PST 2017 - Wed Nov 22 00:23:41 PST 2017
Wed Nov 22 00:23:41 PST 2017 - Wed Nov 22 00:23:41 PST 2017
```

下面是编写 readObject 方法的指导原则：

1. 对象引用字段必须保持私有的的类，应防御性地复制该字段中的每个对象。不可变类的可变组件属于这一类。
2. 检查任何不变量，如果检查失败，那么抛出InvalidObjectException。检查动作应该跟在任何防御性复制之后。
3. 如果必须在反序列化后验证整个对象图，那么使用ObjectInputValidation接口。
4. 不要直接或间接地调用类中任何可被覆盖的方法。



### 89、对于实例控制，枚举类型优于readResolve

第3条中编写了如下的单例模式代码：

```java
public class Elvis {
    public static final Elvis INSTANCE = new Elvis();
    private Elvis() { ... }
    public void leaveTheBuilding() { ... }
}
```

如果该类实现序列化接口，那么它就不再是单例的。因为readObject方法会返回一个新的实例，与类初始化时创建的实例不同。

不过如果在类中定义了readResolve方法，那么反序列化新创建的对象将调用这个方法，并以这个方法返回的对象引用替代新创建的对象。故可以通过以下代码实现单例：

```java
// readResolve for instance control - you can do better!
private Object readResolve() {
    // Return the one true Elvis and let the garbage collector
    // take care of the Elvis impersonator.
    return INSTANCE;
}
```

如果你依赖readResolve进行实例控制，那么所有具有对象引用类型的实例字段都必须声明为 transient。否则，有的攻击者有可能在运行反序列化对象的readResolve方法之前”窃取“对该对象的引用，并之后发起攻击。



### 90、考虑以序列化代理代替序列化实例

使用序列化代理可以降低使用普通序列化面临的风险。

以下代码实现一个序列化代理。

首先，编写一个私有静态内部类，它的字段与外围类一样，拥有一个以外围类对象为参数的构造方法：

```java
// Serialization proxy for Period class
private static class SerializationProxy implements Serializable {
    private final Date start;
    private final Date end;
    SerializationProxy(Period p) {
        this.start = p.start;
        this.end = p.end;
    }
    private static final long serialVersionUID =234098243823485285L; // Any number will do (Item 87)
}
```

为外围类编写writeReplace方法，它在序列化之前将外围类的实例转换为其序列化代理：

```java
// writeReplace method for the serialization proxy pattern
private Object writeReplace() {
    return new SerializationProxy(this);
}
```

这样，序列化系统将永远不会生成外围类的序列化实例，但是攻击者可能会创建一个实例，试图违反类的不变性。为了保证这样的攻击会失败，只需将这个readObject方法添加到外围类中：

```java
// readObject method for the serialization proxy pattern
private void readObject(ObjectInputStream stream) throws InvalidObjectException {
    throw new InvalidObjectException("Proxy required");
}
```

最后，在SerializationProxy类上提供一个readResolve方法，该方法返回外围类的逻辑等效实例。此方法的存在导致序列化系统在反序列化时将序列化代理转换回外围类的实例：

```java
// readResolve method for Period.SerializationProxy
private Object readResolve() {
    return new Period(start, end); // Uses public constructor
}
```

不过，使用序列化代理的开销通常比用保护性拷贝要高。





