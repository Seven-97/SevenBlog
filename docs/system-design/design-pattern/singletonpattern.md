---
title: 单例模式
category: 系统设计
tag:
 - 设计模式
---



## 单例设计模式概念
就是采取一定的方法保证在整个的软件系统中，对某个类只能存在一个对象实例，并且该类只提供一个取得其对象实例的方法。如果我们要让**类在一个虚拟机中只能产生一个对象**，我们首先必须将类的构造器的访问权限设置为private，这样，就不能用new操作符在类的外部产生类的对象了，但在类内部仍可以产生该类的对象。因为在类的外部开始还无法得到类的对象，只能调用该类的某个静态方法以返回类内部创建的对象，静态方法只能访问类中的静态成员变量，所以，指向类内部产生的该类对象的变量也必须定义成静态的。

## 饿汉式
```java
class Singleton {
    // 1.私有化构造器
    private Singleton() {
    }
    // 2.内部提供一个当前类的实例
    // 4.此实例也必须静态化
    private static Singleton single = new Singleton();
    // 3.提供公共的静态的方法，返回当前类的对象；在内存中自始至终都存在
    public static Singleton getInstance() {
        return single;
    }
}
```
案例：
```java
public static void main(String[] args) {
        User user1 = User.getUser();
        System.out.println(user1);
        User user2 = User.getUser();
        System.out.println(user2);
}
class User{
    //1、私有化构造器
    private User() {
    }
    //2、内部提供一个当前类的实例,此实例也必须静态化
    private static User user = new User();
    //3、提供公共的静态的方法，返回当前类的对象；在内存中自始至终都存在
    public static User getUser() {
        return user;
    }
}
//结果是一样的，即同一个对象
com.gupao.singleton.User@6d6f6e28
com.gupao.singleton.User@6d6f6e28 
```

static变量在类加载的时候初始化，此时不会涉及到多个线程对象访问该对象的问题，虚拟机保证只会装载一次该类，肯定不会发生并发问题，无需使用synchronized 关键字

存在的问题：如果只是加载了本类，而并不需要调用getUser，则会造成资源的浪费。

**总结：线程安全、非懒加载、效率高，资源浪费**

## 懒汉式

延迟对象的创建

### 方式1：普通创建
```java
public class Singleton {
    //私有构造方法
    private Singleton() {}

    //在成员位置创建该类的对象
    private static Singleton instance;

    //对外提供静态方法获取该对象
    public static Singleton getInstance() {

        if(instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
}
```
如果是多线程环境，以上代码会出现线程安全问题。

### 方式2：方法加锁
```java
class Singleton {
    // 1.私有化构造器
    private Singleton() {
    }
    // 2.内部提供一个当前类的实例
    // 4.此实例也必须静态化
    private static Singleton instance;
    // 3.提供公共的静态的方法，返回当前类的对象
    public static synchronized Singleton getInstance() {//注意多线程情况
        if(instance== null) {
        instance= new Singleton();
        }
    return instance;
    }
}
```

以上使用同步方法会造成每次获取实例的线程都要等锁，会对系统性能造成影响，未能完全发挥系统性能，可使用同步代码块来解决

### 方式3：双重检查锁
对于 `getInstance()` 方法来说，绝大部分的操作都是读操作，读操作是线程安全的，所以我们没必让每个线程必须持有锁才能调用该方法，我们需要调整加锁的时机。由此也产生了一种新的实现模式：双重检查锁模式

```java
public class Singleton { 

    //私有构造方法
    private Singleton() {}

    private volatile static Singleton instance;

   //对外提供静态方法获取该对象
    public static Singleton getInstance() {
		//第一次判断，如果instance不为null，不进入抢锁阶段，直接返回实例
        if(instance == null) { // ①
            synchronized (Singleton.class) {
                //抢到锁之后再次判断是否为null
                if(instance == null) {
                    instance = new Singleton();// ②
                }
            }
        }
        return instance;
    }
}
```
#### 为什么判断两次instance==null

第一次判断是在代码块前，第二次是进入代码块后，第二个判断想必都知道，多个线程都堵到代码块前等待锁的释放，进入代码块后要获取到最新的instance值，如果为空就进行创建对象。  
那么为什么还要进行第一个判断，第一个判断起到优化作用，假设如果instance已经不为空了，那么没有第一个判断仍然会有线程堵在代码块前等待进一步判断，所以如果不为空，有了第一个判断就不用再去进入代码块进行判断，也就不用再去等锁了，直接返回。

#### 为什么要加volatile？

1. 是为了防止指令重排序，给私有变量加 volatile 主要是为了防止第 ② 处执行时，也就是“instance = new Singleton()”执行时的指令重排序的，这行代码看似只是一个创建对象的过程，然而它的实际执行却分为以下 3 步：
	1. 创建内存空间。
	2. 在内存空间中初始化对象 Singleton。
	3. 将内存地址赋值给 instance 对象（执行了此步骤，instance 就不等于 null 了）。
	
	>试想一下，如果不加 volatile，那么线程A在执行到上述代码的第 ② 处时就可能会执行指令重排序，将原本是 1、2、3 的执行顺序，重排为 1、3、2。但是特殊情况下，线程 A在执行完第 3 步之后，如果来了线程 B执行到上述代码的第 ① 处，判断 instance 对象已经不为 null，但此时线程 A还未将对象实例化完，那么线程B将会得到一个被实例化“一半”的对象，从而导致程序执行出错，这就是为什么要给私有变量添加 volatile 的原因了。
2. 优化作用，synchronized块只有执行完才会同步到主内存，那么比如说instance刚创建完成，不为空，但还没有跳出synchronized块，此时又有10000个线程调用方法，那么如果没有volatile,此使instance在主内存中仍然为空，这一万个线程仍然要通过第一次判断，进入代码块前进行等待，正是有了volatile，一旦instance改变，那么便会同步到主内存，即使没有出synchronized块，instance仍然同步到了主内存，通过不了第一个判断也就避免了新加的10000个线程进入去争取锁。

**总结：线程安全、懒加载、效率高。**

## 静态内部类（延迟初始化占位类）
静态内部类单例模式中实例由内部类创建，由于 JVM 在加载外部类的过程中, 是不会加载静态内部类的, 只有内部类的属性/方法被调用时才会被加载, 并初始化其静态属性。静态属性由于被 `static` 修饰，保证只被实例化一次，并且严格保证实例化顺序。
```java
public class Singleton {

    private Singleton() {
    }

    private static class SingletonHolder{
        private  static final Singleton Instance = new Singleton();
    }

    public static Singleton getInstance(){
        return SingletonHolder.Instance;
    }
}
```
第一次加载Singleton类时不会去初始化INSTANCE，只有第一次调用getInstance，虚拟机加载SingletonHolder并初始化INSTANCE，这样不仅能确保线程安全，也能保证 Singleton 类的唯一性。

静态内部类单例模式是一种优秀的单例模式，是开源项目中比较常用的一种单例模式。在没有加任何锁的情况下，保证了多线程下的安全，并且没有任何性能影响和空间的浪费。

**总结：线程安全、懒加载、效率高。**

## 枚举
枚举类实现单例模式是极力推荐的单例实现模式，因为枚举类型是线程安全的，并且只会装载一次，设计者充分的利用了枚举的这个特性来实现单例模式，枚举的写法非常简单，而且枚举类型是所用单例实现中唯一一种不会被破坏的单例实现模式。

```java
public enum Singleton {

     INSTANCE;

}
```

提供了序列化机制，保证线程安全，绝对防止多次实例化，即使是在面对复杂的序列化或者反射攻击的时候。

枚举方式属于饿汉式方式，会浪费资源

**总结：线程安全、非懒加载、效率高。**


## 几种方式对比
| 方式                   | 优点                     | 缺点               |
| ---------------------- | ------------------------ | ------------------ |
| 饿汉式                 | 线程安全、效率高         | 非懒加载，资源浪费 |
| 懒汉式synchronized方法 | 线程安全、懒加载         | 效率低             |
| 懒汉式双重检测         | 线程安全、懒加载、效率高 | 无                 |
| 静态内部类             | 线程安全、懒加载、效率高 | 无                 |
| 枚举                   | 线程安全、效率高         | 非懒加载，资源浪费 |

可能有人看了以上表格，觉得枚举有缺点，为什么Joshua Bloch还推荐使用枚举？  
这就要提到单例的破解了。普通的单例模式是可以通过反射和序列化/反序列化来破解的，而Enum由于自身的特性问题，是无法破解的。当然，由于这种情况基本不会出现，因此我们在使用单例模式的时候也比较少考虑这个问题。

## 枚举类是实现单例模式最好的方式

在单例模式的实现中，除去**枚举**方法实现的单例模式，其它的实现都可以利用反射构造新的对象，从而破坏单例模式，但是枚举就不行，下面说说原因：  
破坏单例的方式有 3 种，反射、克隆以及序列化，下面详细介绍：

### 反射
常见的单例模式实现中，往往有一个私有的构造函数，防止外部程序的调用，但是通过反射可以轻而易举的破坏这个限制：
```java
public class DobleCheckSingleton {

    private DobleCheckSingleton(){}

    private static volatile DobleCheckSingleton dobleCheckSingleton;

    public static DobleCheckSingleton getSingleton(){
        if (dobleCheckSingleton == null){
            synchronized (DobleCheckSingleton.class){
                if (dobleCheckSingleton == null){
                    dobleCheckSingleton = new DobleCheckSingleton();
                }
            }
        }
        return dobleCheckSingleton;
    }

    public static void main(String[] args) {
        try {
            DobleCheckSingleton dobleCheckSingleton = DobleCheckSingleton.getSingleton();
            Constructor<DobleCheckSingleton> constructor = DobleCheckSingleton.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            DobleCheckSingleton reflectInstance = constructor.newInstance();
            System.out.println(dobleCheckSingleton == reflectInstance);
        } catch (Exception e  e.printStackTrace();
        }
    }
}

输出：false，单例被破坏
```
显然，通过反射可以破坏所有含有无参构造器的单例类，如**可以破坏懒汉式、饿汉式、静态内部类的单例模式**

但是**反射无法破坏通过枚举实现的单例模式**，利用反射构造新的对象，由于 enum 没有无参构造器，结果会抛出 NoSuchMethodException 异常
```java
public enum EnumSingleton {

    INSTANCE;

    public static void main(String[] args) {
        try {
            EnumSingleton enumSingleton = EnumSingleton.INSTANCE;
            // 获取无参的构造函数
            Constructor<EnumSingleton> constructor = null;
            constructor = EnumSingleton.class.getDeclaredConstructor();
            // 使用构造函数创建对象
            constructor.setAccessible(true);
            EnumSingleton reflectInstance = constructor.newInstance();
            System.out.println(enumSingleton == reflectInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

输出：java.lang.NoSuchMethodException: singleton.EnumSingleton.<init>()
    at java.lang.Class.getConstructor0(Class.java:3082)
    at java.lang.Class.getDeclaredConstructor(Class.java:2178)
    at singleton.EnumSingleton.main(EnumSingleton.java:19)
```

#### 枚举安全的原因解释:  
对 EnumSingleton 文件进行反编译，可以发现 EnumSingleton 继承于 Enum，而 Enum 类确实没有无参的构造器，所以抛出 NoSuchMethodException。
```java
枚举类 EnumSingleton 反编译结果
public final class singleton.EnumSingleton extends java.lang.Enum<singleton.EnumSingleton>

Enum 类的构造方法
protected Enum(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
}
```

进一步，通过调用父类有参构造器构造枚举实例对象，样例程序又抛出 IllegalArgumentException 异常。
```java
public enum EnumSingleton {

    INSTANCE;

    public EnumSingleton getInstance(){
        return INSTANCE;
    }

    public static void main(String[] args) {
        try {
            EnumSingleton enumSingleton = EnumSingleton.INSTANCE;
            // 获取无参的构造函数
            Constructor<EnumSingleton> constructor = null;
            constructor = EnumSingleton.class.getDeclaredConstructor(String.class,int.class);
            // 使用构造函数创建对象
            constructor.setAccessible(true);
            EnumSingleton reflectInstance = constructor.newInstance("test",1);
            System.out.println(enumSingleton == reflectInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

输出：
java.lang.IllegalArgumentException: Cannot reflectively create enum objects
    at java.lang.reflect.Constructor.newInstance(Constructor.java:417)
    at singleton.EnumSingleton.main(EnumSingleton.java:31)
```

因为 Constructor 的 newInstance 方法限定了 clazz 的类型不能是 enum，否则抛出异常
```java
@CallerSensitive
 public T newInstance(Object ... initargs)
     throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException
 {
    ...
     if ((clazz.getModifiers() & Modifier.ENUM) != 0)
         throw new IllegalArgumentException("Cannot reflectively create enum objects");
     ...
 }
```
所以枚举类不能通过反射构建构造函数的方式构建新的实例

### 序列化
先看看通过序列化破坏单例的例子，其中 Singleton 实现了 Serializable 接口，才有可能通过序列化破坏单例。
```java
public class DobleCheckSingleton implements Serializable {

    private DobleCheckSingleton() {
    }

    private static volatile DobleCheckSingleton dobleCheckSingleton;

    public static DobleCheckSingleton getSingleton() {
        if (dobleCheckSingleton == null) {
            synchronized (DobleCheckSingleton.class) {
                if (dobleCheckSingleton == null) {
                    dobleCheckSingleton = new DobleCheckSingleton();
                }
            }
        }
        return dobleCheckSingleton;
    }

    public static void main(String[] args) {
        try {
            DobleCheckSingleton dobleCheckSingleton = DobleCheckSingleton.getSingleton();

            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("SerSingleton.obj"));
            oos.writeObject(dobleCheckSingleton);
            oos.flush();
            oos.close();

            FileInputStream fis = new FileInputStream("SerSingleton.obj");
            ObjectInputStream ois = new ObjectInputStream(fis);
            DobleCheckSingleton s1 = (DobleCheckSingleton) ois.readObject();
            ois.close();

            System.out.println(dobleCheckSingleton == s1);
			} catch (Exception e) {
            e.printStackTrace();
        }
    }
}
输出：false，单例被破坏
```

枚举类实现，枚举类不实现 Serializable 接口，都可以进行序列化，并且返回原来的单例。
```java
public enum EnumSingleton {

    INSTANCE;

    public static void main(String[] args) {
        try {
            EnumSingleton enumSingleton = EnumSingleton.INSTANCE;

            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("SerSingleton.obj"));
            oos.writeObject(enumSingleton);
            oos.flush();
            oos.close();

            FileInputStream fis = new FileInputStream("SerSingleton.obj");
            ObjectInputStream ois = new ObjectInputStream(fis);
            EnumSingleton s1 = (EnumSingleton) ois.readObject();
            ois.close();

            System.out.println(enumSingleton == s1);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

输出：true
```
**原因：** 枚举类的 writeObject 方法仅仅是将 Enum.name 写到文件中，反序列化时，根据 readObject 方法的源码定位到 Enum 的 valueOf 方法，他会根据名称返回原来的对象。

### 克隆
实现 Cloneable 接口重写 clone 方法，但是 Enum 类中 clone 的方法是 final 类型，无法重写，也就不能通过克隆破坏单例。
```java
public abstract class Enum<E extends Enum<E>>
        implements Comparable<E>, Serializable {
     protected final Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}  
```

## 不用枚举如何防止单例模式破坏
若实现了序列化接口，重写 readResolve 方法即可，反序列化时将调用该方法返回对象实例
```java
public Object readResolve() throws ObjectStreamException {
    return dobleCheckSingleton;
}
```

通过反射破坏单例的场景，可以在构造方法中判断实例是否已经创建，若已创建则抛出异常
```java
private Singleton(){
    if (instance !=null){
        throw new RuntimeException("实例已经存在，请通过 getInstance()方法获取");
    }
}
```

通过clone破坏单例的场景，可以重写clone方法，返回已有单例对象

## Runtime类
Runtime类就是使用的单例设计模式。

```java
public class Runtime {
    private static Runtime currentRuntime = new Runtime();

    /**
     * Returns the runtime object associated with the current Java application.
     * Most of the methods of class <code>Runtime</code> are instance
     * methods and must be invoked with respect to the current runtime object.
     *
     * @return  the <code>Runtime</code> object associated with the current
     *          Java application.
     */
    public static Runtime getRuntime() {
        return currentRuntime;
    }

    /** Don't let anyone else instantiate this class */
    private Runtime() {}
    ...
}
```
可以看出Runtime类使用的是饿汉式（静态属性）方式来实现单例模式的。


<!-- @include: @article-footer.snippet.md -->     
