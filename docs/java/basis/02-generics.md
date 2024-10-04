---
title: 泛型详解
category: Java
tag:
 - Java基础
head:
  - - meta
    - name: keywords
      content: Java,泛型,generic,java 泛型,java egeneric
  - - meta
    - name: description
      content: 全网最全的Java知识点总结，让天下没有难学的八股文！
---

## 概念

Java 泛型（generics）是 JDK 5 中引入的一个新特性, 泛型提供了编译时类型安全检测机制，该机制允许程序员在编译时检测到非法的类型。

泛型的本质是参数化类型，即给类型指定一个参数，然后在使用时再指定此参数具体的值，那样这个类型就可以在使用时决定了。这种参数类型可以用在类、接口和方法中，分别被称为泛型类、泛型接口、泛型方法。

### 优点

1. 在编译的时候检查类型安全；使用泛型可以在编译时期进行类型检查，从而避免在运行时期发生类型错误。泛型可以在编译时期捕获错误，从而提高了程序的稳定性和可靠性。
2. 避免了源代码中的许多强制类型转换，增加可读性；使用泛型可以使代码更加可读、清晰。通过泛型，可以更好地表达代码的意图，避免了使用Object等不具有明确含义的类型。
3. 提高了代码的重用性；可以在不改变代码的情况下创建多个不同类型的对象。例如，使用List可以创建不同类型的列表，而不需要为每种类型编写不同的代码。

### 只在编译阶段作用

如下：

```java
List<String> list = new ArrayList<>();
list.add("1");
list.add("seven");

Class<? extends List> aClass = list.getClass();
Method add = aClass.getDeclaredMethod("add", Object.class);
add.invoke(list,new Object());
System.out.println(list);

//输出
[1, seven, java.lang.Object@511baa65]
```

虽然指定List泛型为String，但只在编译阶段作用，在运行阶段会执行泛型擦除操作

 

## 泛型擦除

泛型的代码只存在于编译阶段，在进入JVM之前，与泛型相关的信息会被擦除掉，称之为类型擦除。

### 无限制类型擦除

当在类的定义时没有进行任何限制，那么在类型擦除后将会被替换成Object，例如&lt;T&gt;、<?> 都会被替换成Object。

 

### 有限制类型擦除

当类定义中的参数类型存在上下限（上下界），那么在类型擦除后就会被替换成类型参数所定义的上界或者下界，

- 例如<? extend Person>会被替换成Person，而<? super Person> 则会被替换成Object。

 

## 泛型的桥接方法

类型擦除会造成多态的冲突，而JVM解决方法就是**桥接方法**。

### 举例

现在有这样一个泛型类：

```java
class Pair<T> {  

    private T value;  

    public T getValue() {  
        return value;  
    }  

    public void setValue(T value) {  
        this.value = value;  
    }  
}
```



然后一个子类继承它

```java
class DateInter extends Pair<Date> {  

    @Override  
    public void setValue(Date value) {  
        super.setValue(value);  
    }  

    @Override  
    public Date getValue() {  
        return super.getValue();  
    }  
}
```



在这个子类中，设定父类的泛型类型为Pair&lt;Date&gt;，在子类中，覆盖了父类的两个方法，原意是这样的：将父类的泛型类型限定为Date，那么父类里面的两个方法的参数都为Date类型。

```java
public Date getValue() {  
    return value;  
}  

public void setValue(Date value) {  
    this.value = value;  
}
```



实际上，类型擦除后，父类的的泛型类型全部变为了原始类型Object，所以父类编译之后会变成下面的样子：

```java
class Pair {  
    private Object value;  

    public Object getValue() {  
        return value;  
    }  

    public void setValue(Object  value) {  
        this.value = value;  
    }  
} 
```



再看子类的两个重写的方法的类型：setValue方法，父类的类型是Object，而子类的类型是Date，参数类型不一样，这如果实在普通的继承关系中，根本就不会是重写，而是重载。 在一个main方法测试一下：

```java
public static void main(String[] args) throws ClassNotFoundException {  
        DateInter dateInter = new DateInter();  
        dateInter.setValue(new Date());                  
        dateInter.setValue(new Object()); //编译错误  
}
```

如果是重载，那么子类中两个setValue方法，一个是参数Object类型，一个是Date类型，可是根本就没有这样的一个子类继承自父类的Object类型参数的方法。所以说，确实是重写了，而不是重载了。



### 为什么这样？

原因是这样的，传入父类的泛型类型是Date，Pair&lt;Date&gt;，本意是将泛型类变为如下：

```java
class Pair {  
    private Date value;  
    public Date getValue() {  
        return value;  
    }  
    public void setValue(Date value) {  
        this.value = value;  
    }  
}
```

然后在子类中重写参数类型为Date的两个方法，实现继承中的多态。

可是由于种种原因，虚拟机并不能将泛型类型变为Date，只能将类型擦除掉，变为原始类型Object。这样，原来是想进行重写，实现多态，可是类型擦除后，只能变为了重载。这样，类型擦除就和多态有了冲突。于是JVM采用了一个特殊的方法，来完成这项功能，那就是桥方法。

### 原理

用javap -c className的方式反编译下DateInter子类的字节码，结果如下：

```java
class com.tao.test.DateInter extends com.tao.test.Pair<java.util.Date> {  
  com.tao.test.DateInter();  
    Code:  
       0: aload_0  
       1: invokespecial #8                  // Method com/tao/test/Pair."<init>":()V  
       4: return  

  public void setValue(java.util.Date);  //我们重写的setValue方法  
    Code:  
       0: aload_0  
       1: aload_1  
       2: invokespecial #16                 // Method com/tao/test/Pair.setValue:(Ljava/lang/Object;)V  
       5: return  

  public java.util.Date getValue();    //我们重写的getValue方法  
    Code:  
       0: aload_0  
       1: invokespecial #23                 // Method com/tao/test/Pair.getValue:()Ljava/lang/Object;  
       4: checkcast     #26                 // class java/util/Date  
       7: areturn  

  public java.lang.Object getValue();     //编译时由编译器生成的桥方法  
    Code:  
       0: aload_0  
       1: invokevirtual #28                 // Method getValue:()Ljava/util/Date 去调用我们重写的getValue方法;  
       4: areturn  

  public void setValue(java.lang.Object);   //编译时由编译器生成的桥方法  
    Code:  
       0: aload_0  
       1: aload_1  
       2: checkcast     #26                 // class java/util/Date  
       5: invokevirtual #30                 // Method setValue:(Ljava/util/Date; 去调用我们重写的setValue方法)V  
       8: return  
}
```

从编译的结果来看，本意重写setValue和getValue方法的子类，但是反编译后竟然有4个方法，其实最后的两个方法，**就是编译器自己生成的桥方法**。可以看到桥方法的参数类型都是Object，也就是说，子类中真正覆盖父类两个方法的就是这两个我们看不到的桥方法。而在setvalue和getValue方法上面的@Oveerride只不过是假象。而桥方法的内部实现，就只是去调用自己重写的那两个方法。

所以，虚拟机巧妙的使用了桥方法，来解决了类型擦除和多态的冲突。

 

并且，还有一点也许会有疑问，子类中的桥方法Object getValue()和Date getValue()是同时存在的，可是如果是常规的两个方法，他们的方法签名是一样的，如果是我们自己编写Java代码，这样的代码是无法通过编译器的检查的（返回值不同不能作为重载的条件），但是虚拟机却是允许这样做的，因为虚拟机通过参数类型和返回类型来确定一个方法，所以编译器为了实现泛型的多态允许自己做这个看起来“不合法”的事情，然后交给虚拟机去区别

 

## 获取泛型的参数类型

既然类型被擦除了，那么如何获取泛型的参数类型呢？可以通过反射（java.lang.reflect.Type）获取泛型

java.lang.reflect.Type是Java中所有类型的公共高级接口, 代表了Java中的所有类型. Type体系中类型的包括：数组类型(GenericArrayType)、参数化类型(ParameterizedType)、类型变量(TypeVariable)、通配符类型(WildcardType)、原始类型(Class)、基本类型(Class), 以上这些类型都实现Type接口。

```java
public class GenericType<T> {
    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static void main(String[] args) {
        GenericType<String> genericType = new GenericType<String>() {};
        Type superclass = genericType.getClass().getGenericSuperclass();
        //getActualTypeArguments 返回确切的泛型参数, 如Map<String, Integer>返回[String, Integer]
        Type type = ((ParameterizedType) superclass).getActualTypeArguments()[0]; 
        System.out.println(type);//class java.lang.String
    }
}
```



 

 <!-- @include: @article-footer.snippet.md -->     

 

 

 

 

 

 

 

 

 