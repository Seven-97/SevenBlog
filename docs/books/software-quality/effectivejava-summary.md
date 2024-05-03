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

 

 

 

 

 

 

## 枚举和注解

 

 

## 方法

 

 

## 通用程序设计

 

 

 

## 异常

### 9、使用 try-with-resources 替代 try-finally 

原因关注 [04 异常详解](note://WEB8fe1555c51e56ec1bee14ea854ae0530)

 

## 并发

 

 

## 序列化

 





 