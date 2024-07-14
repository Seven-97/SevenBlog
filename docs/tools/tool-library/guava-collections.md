---
title: Guava - 集合
category: 开发工具
tag:
 - 工具类库
---



## Immutable

如[《Effective Java》Item1](https://www.seven97.top/books/software-quality/effectivejava-summary.html#_1、静态工厂方法代替构造器))所述，在设计类的时候，倾向优先使用静态工厂方法(static factory method)而非构造函数(constructor)创建对象，优点在于:

1. 静态工厂方法多了一层名称信息，比构造函数更富表达性。
2. 可以更灵活地创建对象，比如缓式初始化，缓存已创建对象。
3. 静态方法内部返回的对象类型，可以是其声明类型的子类。

`ImmutableList`遵循了最佳实践。首先，`ImmutableList`不可以通过构造函数实例化，更准确地说，不可以在`package`外部通过构造函数实例化。

而在程序设计中使用不可变对象，也可以提高代码的可靠性和可维护性，其优势包括：

1. **线程安全性（Thread Safety）**：不可变对象是线程安全的，无需同步操作，避免了竞态条件
2. **安全性**：可以防止在程序运行时被意外修改，提高了程序的安全性
3. **易于理解和测试**：不可变对象在创建后不会发生变化，更容易理解和测试
4. **克隆和拷贝**：不可变对象不需要实现可变对象的复制（Clone）和拷贝（Copy）逻辑，因为它们的状态不可变，克隆即是自己

创建对象的不可变拷贝是一项很好的防御性编程技巧。Guava为所有JDK标准集合类型和Guava新集合类型都提供了简单易用的不可变版本。JDK也提供了Collections.unmodifiableXXX方法把集合包装为不可变形式.

### JDK不可变集合存在的问题

JDK 的 Collections 提供了 Unmodified Collections 不可变集合，但仅仅是通过装饰器模式提供了一个只读的视图，unmodifiableList本身是无法进行add等修改操作，但并没有阻止对原始集合的修改操作，所以说Collections.unmodifiableList实现的不是真正的不可变集合。

```java
List<String> list=new ArrayList<String>();
list.add("a");
list.add("b");
list.add("c");

//通过list创建一个不可变的unmodifiableList集合
List<String> unmodifiableList = Collections.unmodifiableList(list);
System.out.println(unmodifiableList);//[a,b,c]

//通过list添加元素
list.add("ddd");
System.out.println("往list添加一个元素:" + list);//[a,b,c,ddd]
System.out.println("通过list添加元素之后的unmodifiableList:" + unmodifiableList);[]//[a,b,c,ddd]

 //通过unmodifiableList添加元素
unmodifiableList.add("eee");//报错
System.out.println("往unmodifiableList添加一个元素:" + unmodifiableList);

```

- 笨重而且累赘：不能舒适地用在所有想做防御性拷贝的场景；
- 不安全：要保证没人通过原集合的引用进行修改，返回的集合才是事实上不可变的；
- 低效：包装过的集合仍然保有可变集合的开销，比如并发修改的检查、散列表的额外空间，等等。



### Guava不可变集合案例

而 Guava 提供的不可变集合更加简单高效，确保了真正的不可变性。

**注意**：每个Guava immutable集合类的实现都拒绝null值。如果确实需要能接受null值的集合类，可以考虑用Collections.unmodifiableXXX。

immutable集合可以有以下几种方式来创建：

1. 用copyOf方法，比如，ImmutableSet.copyOf(set)
2. 使用of方法，比如，ImmutableSet.of("a", "b", "c") 或者 ImmutableMap.of("a", 1, "b", 2)
3. 使用Builder类：减少中间对象的创建，提高内存使用效率。

```java
List<String> list = new ArrayList<>();
list.add("a");
list.add("b");
list.add("c");
System.out.println("list：" + list);//[a, b, c]

ImmutableList<String> imlist = ImmutableList.copyOf(list);
System.out.println("imlist：" + imlist);//[a, b, c]

ImmutableList<String> imOflist = ImmutableList.of("seven", "seven1", "seven2");
System.out.println("imOflist：" + imOflist);//[seven, seven1, seven2]

ImmutableSortedSet<String> imSortList = ImmutableSortedSet.of("a", "b", "c", "a", "d", "b");
System.out.println("imSortList：" + imSortList);//[a, b, c, d]

list.add("seven");
System.out.println("list add a item after list:" + list);//[a, b, c, seven]
System.out.println("list add a item after imlist:" + imlist);//[a, b, c]

ImmutableSet<Color> imColorSet =
       ImmutableSet.<Color>builder()
             .add(new Color(0, 255, 255))
             .add(new Color(0, 191, 255))
             .build();

System.out.println("imColorSet:" + imColorSet); //[java.awt.Color[r=0,g=255,b=255], java.awt.Color[r=0,g=191,b=255]]
```



### 更智能的copyOf

ImmutableXXX.copyOf会在合适的情况下避免拷贝元素的操作。

```java
ImmutableSet<String> imSet = ImmutableSet.of("seven", "lisa", "seven1", "lisa1");
System.out.println("imSet：" + imSet);//[seven, lisa, seven1, lisa1]
ImmutableList<String> imlist = ImmutableList.copyOf(imSet);
System.out.println("imlist：" + imlist);//[seven, lisa, seven1, lisa1]
ImmutableSortedSet<String> imSortSet = ImmutableSortedSet.copyOf(imSet);
System.out.println("imSortSet：" + imSortSet);//[lisa, lisa1, seven, seven1]

List<String> list = new ArrayList<>();
for (int i = 0; i < 20; i++) {
       list.add(i + "x");
 }
System.out.println("list：" + list);//[0x, 1x, 2x, 3x, 4x, 5x, 6x, 7x, 8x, 9x, 10x, 11x, 12x, 13x, 14x, 15x, 16x, 17x, 18x, 19x]
ImmutableList<String> imInfolist = ImmutableList.copyOf(list.subList(2, 18));
System.out.println("imInfolist：" + imInfolist);//[2x, 3x, 4x, 5x, 6x, 7x, 8x, 9x, 10x, 11x, 12x, 13x, 14x, 15x, 16x, 17x]
int imInfolistSize = imInfolist.size();
System.out.println("imInfolistSize：" + imInfolistSize);//16
ImmutableSet<String> imInfoSet = ImmutableSet.copyOf(imInfolist.subList(2, imInfolistSize - 3));
System.out.println("imInfoSet：" + imInfoSet);//[4x, 5x, 6x, 7x, 8x, 9x, 10x, 11x, 12x, 13x, 14x]
```

在这段代码中，ImmutableList.copyOf(imSet)会智能地直接返回 imSet.asList()，它是一个ImmutableSet的常量时间复杂度的List视图。



实际上，要实现`copyOf`方法，最简单的就是直接将底层的每个元素做深拷贝然后生成`ImmutableList`。但是对于所有情况都深拷贝的话，性能和存储开销必然比较大，那么源码里面是如何优化的呢？

所有不可变集合都有一个asList() 方法提供ImmutableList视图，让我们可以用列表形式方便地读取集合元素。例如，我们可以使用sortedSet.asList().get(k) 从 ImmutableSortedSet 中读取第k个最小元素。
asList()返回的ImmutableList 通常是（但并不总是）开销稳定的视图实现，而不是简单地把元素拷贝进List，也就是说，asList返回的列表视图通常比一般的列表平均性能更好，比如，在底层集合支持的情况下，它总是使用高效的contains方法。

源码如下：

```java
// com.google.common.collect.ImmutableList#copyOf(java.util.Collection<? extends E>)
public static <E> ImmutableList<E> copyOf(Collection<? extends E> elements) {
    //判断是否是不可变集合
    if (elements instanceof ImmutableCollection) {
        //如果传入的结合本身就是一个不可变集合，那么asList获取视图后返回；其实就是直接复用原来的collection
        ImmutableList<E> list = ((ImmutableCollection)elements).asList();
        //判断是否是要返回局部视图：是的话重新构建->调用Arrays.copyOf做深拷；不是的话就复用原来的
        return list.isPartialView() ? asImmutableList(list.toArray()) : list;
    } else {//如果不是，则执行construct方法：底层调用Arrays.copyOf做深拷贝
        return construct(elements.toArray());
    }
}

// com.google.common.collect.ImmutableCollection#asList
public ImmutableList<E> asList() {
    switch (this.size()) {
        case 0:
            // 返回一个空的不可变集合，这个空集合是个static final常量，可复用
            return ImmutableList.of();
        case 1:
            // 返回一个不可变的 SingletonImmutableList 集合
            return ImmutableList.of(this.iterator().next());
        default:
            return new RegularImmutableAsList(this, this.toArray());
    }
}

//com.google.common.collect.RegularImmutableAsList#RegularImmutableAsList(com.google.common.collect.ImmutableCollection<E>, java.lang.Object[])
RegularImmutableAsList(ImmutableCollection<E> delegate, Object[] array) {
    this(delegate, ImmutableList.asImmutableList(array));
}
RegularImmutableAsList(ImmutableCollection<E> delegate, ImmutableList<? extends E> delegateList) {
    this.delegate = delegate;
    this.delegateList = delegateList;
}
```



实际上，ImmutableXXX.copyOf(ImmutableCollection)会试图对如下情况避免线性时间拷贝：

- **在常量时间内使用底层数据结构是可能的**：因为会获取视图后返回
- **不会造成内存泄露**：例如，有个很大的不可变集合`ImmutableList<String>
  hugeList`， `ImmutableList.copyOf(hugeList.subList(0, 10))`就会显式地拷贝（如上源码，会判断是否是局部视图），以免不必要地持有hugeList的引用。
- **不改变语义**：所以ImmutableSet.copyOf(myImmutableSortedSet)都会显式地拷贝，因为和基于比较器的ImmutableSortedSet相比，ImmutableSet对hashCode()和equals有不同语义。

在可能的情况下避免线性拷贝，可以最大限度地减少防御性编程风格所带来的性能开销。



### Guava集合和不可变对应关系

| **可变集合类型**       | **可变集合源：JDK or Guava?** | **Guava不可变集合**         |
| ---------------------- | ----------------------------- | --------------------------- |
| Collection             | JDK                           | ImmutableCollection         |
| List                   | JDK                           | ImmutableList               |
| Set                    | JDK                           | ImmutableSet                |
| SortedSet/NavigableSet | JDK                           | ImmutableSortedSet          |
| Map                    | JDK                           | ImmutableMap                |
| SortedMap              | JDK                           | ImmutableSortedMap          |
| Multiset               | Guava                         | ImmutableMultiset           |
| SortedMultiset         | Guava                         | ImmutableSortedMultiset     |
| Multimap               | Guava                         | ImmutableMultimap           |
| ListMultimap           | Guava                         | ImmutableListMultimap       |
| SetMultimap            | Guava                         | ImmutableSetMultimap        |
| BiMap                  | Guava                         | ImmutableBiMap              |
| ClassToInstanceMap     | Guava                         | ImmutableClassToInstanceMap |
| Table                  | Guava                         | ImmutableTable              |

 

## Lists

```java
private Lists() {
}
```

私有的构造方法，可以看到这是一个真正的功能函数，下面对其函数进行分析

 

### 功能函数

首先根据每一个函数的更能进行了分类：

| 功能                                                     | 方法                                                         |
| -------------------------------------------------------- | ------------------------------------------------------------ |
| 创建ArrayList方法                                        | 1、newArrayList()<br/>2、newArrayList(E... elements)<br/>3、newArrayList(Iterable<? extends E> elements)<br/>4、newArrayList(Iterator<? extends E> elements)<br/>5、newArrayListWithCapacity(int initialArraySize)<br/>6、newArrayListWithExpectedSize(int estimatedSize) |
| 创建LinkedList方法                                       | 1、newLinkedList()<br />2、newLinkedList(Iterable<? extends E> elements) |
| 创建CopyOnWriteArrayList方法                             | 1、newCopyOnWriteArrayList()<br />2、newCopyOnWriteArrayList(Iterable<? extends E> elements) |
| 创建自制List规则                                         | 1、asList(@Nullable E first, E[] rest)<br />2、asList(@Nullable E first, @Nullable E second, E[] rest) |
| 对多个List做笛卡尔乘积                                   | 1、cartesianProduct(List<? extends List<? extends B>> lists)<br />2、cartesianProduct(List<? extends B>... lists) |
| 将一个fromList中的元素根据指定的function转换为另一种类型 | transform(List&lt;F> fromList, Function<? super F, ? extends T> function) |
| 对list进行分批输出的方法（作用之一：分页）               | partition(List&lt;T> list, int size)                         |
| 将字符串作为字符数组进行操作                             | 1、charactersOf(String string)<br />2、charactersOf(CharSequence sequence) |
| 将list逆序                                               | reverse(List&lt;T> list)                                     |



### 创建ArrayList方法

1. 没有参数的创建ArrayList

   ```java
   public static <E> ArrayList<E> newArrayList() {
       return new ArrayList();//直接返回一个新的ArrayList容器
   }
   ```

2. 传入一个数组，返回一个ArrayList

   ```java
   public static <E> ArrayList<E> newArrayList(E... elements) {
       //对数组进行判空处理
       Preconditions.checkNotNull(elements);
       // computeArrayListCapacity对当前数量进行优化
       int capacity = computeArrayListCapacity(elements.length);
       //这里根据优化后的数量进行创建一个新的ArrayList
       ArrayList list = new ArrayList(capacity);
       //将数组里面的元素都存储在List中
       Collections.addAll(list, elements);
       return list;
   }
   
   //目的是为了给定一个期望的元素数量（arraySize），计算出为了避免或减少动态扩容带来的开销，列表初始化时应该分配的容量
   @VisibleForTesting
   static int computeArrayListCapacity(int arraySize) {
       //确保arraySize非负数
       CollectPreconditions.checkNonnegative(arraySize, "arraySize");
       //确保最后的数值在 int范围内
       return Ints.saturatedCast(5L + (long)arraySize + (long)(arraySize / 10));
   }
   ```

3. 传入一个集合顶级接口，然后返回一个ArrayList

   ```java
   public static <E> ArrayList<E> newArrayList(Iterable<? extends E> elements) {
       //对集合进行判空
       Preconditions.checkNotNull(elements);
       //根据传入的实际类型，进行分别处理
       return elements instanceof Collection ? new ArrayList(Collections2.cast(elements)):newArrayList((Iterator)elements.
   iterator());
   }
   ```

4. 传入一个迭代器，返回一个ArrayList

   ```java
   public static <E> ArrayList<E> newArrayList(Iterator<? extends E> elements) {
       ArrayList list = newArrayList();
       //根据Iterators中的addAll方法将迭代器中的源码装入list集合中
       Iterators.addAll(list, elements);
       return list;
   }
   ```

5. 传入想要的list长度，返回一个与传入值等长的ArrayList

   ```java
   //直接返回一个新的ArrayList，并且长度为传入的长度
   public static <E> ArrayList<E> newArrayListWithCapacity(int initialArraySize) {
       CollectPreconditions.checkNonnegative(initialArraySize, "initialArraySize");
       return new ArrayList(initialArraySize);
   }
   ```

6. 传入一个想要的list长度，返回一个程序调优后的长度的ArrayList

   ```java
   public static <E> ArrayList<E> newArrayListWithExpectedSize(int estimatedSize) {
       return new ArrayList(computeArrayListCapacity(estimatedSize));//返回一个长度调优后的ArrayList
   }
   ```



### 创建LinkedList方法

1. 不传入参数，直接返回一个LinkedList

   ```java
   public static <E> LinkedList<E> newLinkedList() {
       return new LinkedList();//直接返回一个LinkedList
   }
   ```

2. 传入一个容器，返回一个LinkedList

   ```java
   public static <E> LinkedList<E> newLinkedList(Iterable<? extends E> elements) {
       LinkedList list = newLinkedList();
       //将传入的容器，使用Iterables的addAll方法进行的数据转移
       Iterables.addAll(list, elements);
       return list;
   }
   ```



### 创建CopyOnWriteArrayList方法

1. 不传入参数，直接返回一个新的CopyOnWriteArrayList

   ```java
   public static <E> CopyOnWriteArrayList<E> newCopyOnWriteArrayList() {
       return new CopyOnWriteArrayList();//直接返回一个新的CopyOnWriteArrayList
   }
   ```

2. 传入一个容器，返回一个CopyOnWriteArrayList，带有传入容器的值

   ```java
   public static <E> CopyOnWriteArrayList<E> newCopyOnWriteArrayList(Iterable<? extends E> elements) {
       Object elementsCollection = elements instanceof Collection?Collections2.cast(elements):newArrayList((Iterable)elements);
       return new CopyOnWriteArrayList((Collection)elementsCollection);
   }
   ```



### 创建自制List规则

使用案例：

```java
String leader = "leader";
String[] members = {"member1", "member2", "member3"};

List<String> group = Lists.asList(leader, members);
System.out.println(group);
```

这样做的一个好处是可以提高代码的可读性，因为它明确地区分了 "leader" 和 "members"，而不是将它们混在一起。而且，如果 "members" 是动态确定的（例如，它们来自另一个方法或计算结果），那么这个 `asList` 方法将比手动创建 `List` 并添加元素更为方便。



1. 根据参数生成一个多一个参数的List

   ```java
   public static <E> List<E> asList(@Nullable E first, E[] rest) {
       return new Lists.OnePlusArrayList(first, rest);//返回一个Lists中的内部类OnePlusArrayList
   }
   
   private static class OnePlusArrayList<E> extends AbstractList<E> implements Serializable, RandomAccess {
       final E first;
       final E[] rest;
       private static final long serialVersionUID = 0L;
    
       OnePlusArrayList(@Nullable E first, E[] rest) {
           this.first = first;
           this.rest = (Object[])Preconditions.checkNotNull(rest);
       }
    
       // 重写了size和get方法
       // 因为是比原来数组多1个数，所以size方法在原来的基础上进行了+1操作。
       public int size() {
           return this.rest.length + 1;
       }
    
       //对get的重写是将所有的下标逻辑上后移了一位。
       public E get(int index) {
           Preconditions.checkElementIndex(index, this.size());
           return index == 0 ? this.first:this.rest[index - 1];
       }
   }
   ```

2. 根据参数生成一个多两个个参数的List

   ```java
   public static <E> List<E> asList(@Nullable E first, @Nullable E second, E[] rest) {
       return new Lists.TwoPlusArrayList(first, second, rest);//返回一个Lists中的内部类TwoPlusArrayList
   }
   
   private static class TwoPlusArrayList<E> extends AbstractList<E> implements Serializable, RandomAccess {
       final E first;
       final E second;
       final E[] rest;
       private static final long serialVersionUID = 0L;
    
       TwoPlusArrayList(@Nullable E first, @Nullable E second, E[] rest) {
           this.first = first;
           this.second = second;
           this.rest = (Object[])Preconditions.checkNotNull(rest);
       }
    
       // 重写了size和get两个方法，size在实际数组长度上进行了+2的操作
       public int size() {
           return this.rest.length + 2;
       }
    
       //get则在逻辑上将下标进行了后移两位
       public E get(int index) {
           switch(index) {
           case 0:
               return this.first;
           case 1:
               return this.second;
           default:
               Preconditions.checkElementIndex(index, this.size());
               return this.rest[index - 2];
           }
       }
   } 
   ```

   

### 对多个List做笛卡尔乘积

```java
public static <B> List<List<B>> cartesianProduct(List<? extends List<? extends B>> lists) {
    return CartesianList.create(lists);
}

//整个方法的逻辑是将输入的一组列表转换成一个表示它们所有可能组合（笛卡尔积）的列表结构，同时确保输入列表的不可变性。
//这种方法在需要处理多维数据组合时特别有用，例如在配置管理、测试用例生成等应用场景中。
static <E> List<List<E>> create(List<? extends List<? extends E>> lists) {
    ImmutableList.Builder<List<E>> axesBuilder = new ImmutableList.Builder(lists.size());
    Iterator var2 = lists.iterator();

    while(var2.hasNext()) {
        List<? extends E> list = (List)var2.next();
        List<E> copy = ImmutableList.copyOf(list);
        if (copy.isEmpty()) {
            //如果任意一个子列表为空，则整个笛卡尔积列表也应为空（因为任何东西与空集的笛卡尔积都是空集），因此直接返回一个空的不可变列表。
            return ImmutableList.of();
        }

        axesBuilder.add(copy);
    }

    return new CartesianList(axesBuilder.build());
}
```



### 将fromList中的元素根据指定的function转换为另一种类型

使用案例：

```java
List<String> stringList = Arrays.asList("1", "2", "3");
List<Integer> integerList = transform(stringList, s -> Integer.parseInt(s));
```



源码：

```java
public static <F, T> List<T> transform(List<F> fromList, Function<? super F, ? extends T> function) {
    //依据 fromList 是否支持随机访问（例如 ArrayList），来决定使用哪种内部实现方式，以提高效率。
    //不过无论使用哪种方式进行处理，在他们的实现类中都重写了listIterator方法
    return (List)(fromList instanceof RandomAccess?new Lists.TransformingRandomAccessList(fromList, function):new Lists.TransformingSequentialList(fromList, function));
}


private static class TransformingRandomAccessList<F, T> extends AbstractList<T> implements RandomAccess, Serializable {
    final List<F> fromList;
    final Function<? super F, ? extends T> function;
    private static final long serialVersionUID = 0L;
 
    TransformingRandomAccessList(List<F> fromList, Function<? super F, ? extends T> function) {
        this.fromList = (List)Preconditions.checkNotNull(fromList);
        this.function = (Function)Preconditions.checkNotNull(function);
    }
    public ListIterator<T> listIterator(int index) {
        //当通过 listIterator 方法获取迭代器时，实际上是获取了原始列表 fromList 的迭代器，并将其包装在 TransformedListIterator 中。
		//每次迭代时，TransformedListIterator 会调用 transform 方法，该方法使用 function 将原始列表中的元素转换为目标类型 T。
        return new TransformedListIterator(this.fromList.listIterator(index)) {
            T transform(F from) {
                return TransformingRandomAccessList.this.function.apply(from);
            }
        };
    }
}
```



### 对list进行分批输出的方法（作用之一：分页）



```java
// 程序根据传入的List进行分类处理
public static <T> List<List<T>> partition(List<T> list, int size) {
    Preconditions.checkNotNull(list);
    Preconditions.checkArgument(size > 0);
    return (List)(list instanceof RandomAccess?new Lists.RandomAccessPartition(list, size):new Lists.Partition(list, size));
}
```

这里的RandomAccessPartition是Partition的子类，且RandomAccessPartition对其的处理是直接调用了父类的方法，所以我们只需要解析Partition类就可以了

```java
private static class Partition<T> extends AbstractList<List<T>> {
    final List<T> list;
    final int size;
    Partition(List<T> list, int size) {
        this.list = list;
        this.size = size;
    }
    //get方法进行了截取操作，而它的截取底层是subList实现的。它所get的下标为第几组List
    public List<T> get(int index) {
        Preconditions.checkElementIndex(index, this.size());
        int start = index * this.size;
        int end = Math.min(start + this.size, this.list.size());
        return this.list.subList(start, end);
    }
    public int size() {
        return IntMath.divide(this.list.size(), this.size, RoundingMode.CEILING);
    }
    public boolean isEmpty() {
        return this.list.isEmpty();
    }
}
```



### 将字符串作为字符数组进行操作

主要用于将一个字符串转换为一个不可变的 `List<Character>`，使得字符串的字符可以像列表元素一样进行操作。

```java
//两个方法分别接收字符串和CharSequence作为参数，将参数传入CharSequenceAsList类中进行处理：
public static ImmutableList<Character> charactersOf(String string) {
    return new Lists.StringAsImmutableList((String)Preconditions.checkNotNull(string));
}
public static List<Character> charactersOf(CharSequence sequence) {
    return new Lists.CharSequenceAsList((CharSequence)Preconditions.checkNotNull(sequence));
}
```



```java
//实际上就是对间接使用String类中的方法进行处理字符串
private static final class StringAsImmutableList extends ImmutableList<Character> {
    private final String string;
 
    StringAsImmutableList(String string) {
        this.string = string;
    }
 
    public int indexOf(@Nullable Object object) {
        return object instanceof Character?this.string.indexOf(((Character)object).charValue()):-1;
    }
 
    public int lastIndexOf(@Nullable Object object) {
        return object instanceof Character?this.string.lastIndexOf(((Character)object).charValue()):-1;
    }
 
    public ImmutableList<Character> subList(int fromIndex, int toIndex) {
        Preconditions.checkPositionIndexes(fromIndex, toIndex, this.size());
        return Lists.charactersOf((String)this.string.substring(fromIndex, toIndex));
    }
 
    boolean isPartialView() {
        return false;
    }
 
    public Character get(int index) {
        Preconditions.checkElementIndex(index, this.size());
        return Character.valueOf(this.string.charAt(index));
    }
 
    public int size() {
        return this.string.length();
    }
}
```



### list逆序

```java
public static <T> List<T> reverse(List<T> list) {
    if (list instanceof ImmutableList) {
        List<?> reversed = ((ImmutableList)list).reverse();
        List<T> result = reversed;
        return result;
    } else if (list instanceof ReverseList) {
        return ((ReverseList)list).getForwardList();
    } else {
        return (List)(list instanceof RandomAccess ? new RandomAccessReverseList(list) : new ReverseList(list));
    }
}
```

实际上调用了ImmutableList类的reverse方法进行处理的逆序



## Maps

```java
private Maps() {
}
```

私有的构造方法，可以看到这是一个真正的功能函数，下面对其函数进行分析



### 功能函数

| 功能                                     | 方法                                                         |
| ---------------------------------------- | ------------------------------------------------------------ |
| 创建EnumMap                              | 1、EnumMap<K, V> newEnumMap(Class&lt;K> type)<br />2、EnumMap<K, V> newEnumMap(Map<K, ? extends V> map) |
| 返回不可变EnumMap                        | ImmutableMap<K, V> immutableEnumMap(Map<K, ? extends V> map) |
| 创建HashMap                              | 1、HashMap<K, V> newHashMap()<br/>2、HashMap<K, V> newHashMapWithExpectedSize(int expectedSize)<br/>3、HashMap<K, V> newHashMap(Map<? extends K, ? extends V> map) |
| 创建LinkedHashMap                        | 1、LinkedHashMap<K, V> newLinkedHashMap()<br />2、LinkedHashMap<K, V> newLinkedHashMap(Map<? extends K, ? extends V> map) |
| 创建ConcurrentMap                        | ConcurrentMap<K, V> newConcurrentMap()                       |
| 创建TreeMap                              | 1、TreeMap<K, V> newTreeMap()<br/>2、TreeMap<K, V> newTreeMap(SortedMap<K, ? extends V> map)<br/>3、TreeMap<K, V> newTreeMap(@Nullable Comparator&lt;C> comparator) |
| 创建IdentityHashMap                      | IdentityHashMap<K, V> newIdentityHashMap()                   |
| 获取两个Map中不同元素的值                | 1、MapDifference<K, V> difference(Map<? extends K, ? extends V> left, Map<? extends K, ? extends V> right)<br/>2、MapDifference<K, V> difference(Map<? extends K, ? extends V> left, Map<? extends K, ? extends V> right, Equivalence<? super V> valueEquivalence)<br/>3、SortedMapDifference<K, V> difference(SortedMap<K, ? extends V> left, Map<? extends K, ? extends V> right) |
| 根据函数和set，构造Map                   | 1、Map<K, V> asMap(Set&lt;K> set, Function<? super K, V> function)<br/>2、SortedMap<K, V> asMap(SortedSet&lt;K> set, Function<? super K, V> function)<br/>3、NavigableMap<K, V> asMap(NavigableSet&lt;K> set, Function<? super K, V> function) |
| 根据函数和迭代器，构造不可变的Map        | 1、ImmutableMap<K, V> toMap(Iterator&lt;K> keys, Function<? super K, V> valueFunction)<br/>2、ImmutableMap<K, V> toMap(Iterator&lt;K> keys, Function<? super K, V> valueFunction)<br/>3、ImmutableMap<K, V> uniqueIndex(Iterable&lt;V> values, Function<? super V, K> keyFunction)<br/>4、ImmutableMap<K, V> uniqueIndex(Iterator&lt;V> values, Function<? super V, K> keyFunction) |
| 从配置文件中读取数据，创建不可变的Map    | ImmutableMap<String, String> fromProperties(Properties properties) |
| 返回Entry或Entry集合                     | 1、Entry<K, V> immutableEntry(@Nullable K key, @Nullable V value)<br/>2、Set<Entry<K, V>> unmodifiableEntrySet(Set<Entry<K, V>> entrySet)<br/>3、Entry<K, V> unmodifiableEntry(final Entry<? extends K, ? extends V> entry) |
| 返回特殊的BiMap类                        | 1、BiMap<K, V> synchronizedBiMap(BiMap<K, V> bimap)<br />2、BiMap<K, V> unmodifiableBiMap(BiMap<? extends K, ? extends V> bimap) |
| 根据Map和函数对Map进行转型               | 1、Map<K, V2> transformValues(Map<K, V1> fromMap, Function<? super V1, V2> function)<br/>2、SortedMap<K, V2> transformValues(SortedMap<K, V1> fromMap, Function<? super V1, V2> function)<br/>3、NavigableMap<K, V2> transformValues(NavigableMap<K, V1> fromMap, Function<? super V1, V2> function)<br />4、Map<K, V2> transformEntries(Map<K, V1> fromMap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer)<br/>5、SortedMap<K, V2> transformEntries(SortedMap<K, V1> fromMap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer)<br/>6、NavigableMap<K, V2> transformEntries(NavigableMap<K, V1> fromMap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer) |
| 使用函数进行过滤Map，然后返回同类型的Map | 分为4种过滤<br/>一、针对Key进行过滤<br/>1.Map<K, V> filterKeys(Map<K, V> unfiltered, Predicate<? super K> keyPredicate)<br/>2.SortedMap<K, V> filterKeys(SortedMap<K, V> unfiltered, Predicate<? super K> keyPredicate)<br/>3.NavigableMap<K, V> filterKeys(NavigableMap<K, V> unfiltered, Predicate<? super K> keyPredicate)<br/>4.BiMap<K, V> filterKeys(BiMap<K, V> unfiltered, Predicate<? super K> keyPredicate)<br/>二、针对Value进行过滤<br/>1.Map<K, V> filterValues(Map<K, V> unfiltered, Predicate<? super V> valuePredicate)<br/>2.SortedMap<K, V> filterValues(SortedMap<K, V> unfiltered, Predicate<? super V> valuePredicate)<br/>3.NavigableMap<K, V> filterValues(NavigableMap<K, V> unfiltered, Predicate<? super V> valuePredicate)<br/>4.BiMap<K, V> filterValues(BiMap<K, V> unfiltered, Predicate<? super V> valuePredicate)<br/>三、针对Entry进行过滤<br/>1.Map<K, V> filterEntries(Map<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate)<br/>2.SortedMap<K, V> filterEntries(SortedMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate)<br/>3.SortedMap<K, V> filterSortedIgnoreNavigable(SortedMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate)<br/>4.NavigableMap<K, V> filterEntries(NavigableMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate)<br/>5.BiMap<K, V> filterEntries(BiMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate)<br/>四、为含有过滤规则的Map进行过滤<br/>1.Map<K, V> filterFiltered(Maps.AbstractFilteredMap<K, V> map, Predicate<? super Entry<K, V>> entryPredicate)<br/>2.SortedMap<K, V> filterFiltered(Maps.FilteredEntrySortedMap<K, V> map, Predicate<? super Entry<K, V>> entryPredicate)<br/>3.NavigableMap<K, V> filterFiltered(Maps.FilteredEntryNavigableMap<K, V> map, Predicate<? super Entry<K, V>> entryPredicate)<br/>4.BiMap<K, V> filterFiltered(Maps.FilteredEntryBiMap<K, V> map, Predicate<? super Entry<K, V>> entryPredicate) |



### 创建EnumMap

1. 传入一个Class变量，返回一个EnumMap
   ```java
   public static <K extends Enum<K>, V> EnumMap<K, V> newEnumMap(Class<K> type) {
       return new EnumMap((Class)Preconditions.checkNotNull(type));
   }
   ```

2. 传入一个Map变量，返回一个EnumMap
   ```java
   public static <K extends Enum<K>, V> EnumMap<K, V> newEnumMap(Map<K, ? extends V> map) {
       return new EnumMap(map);
   }
   
   //java.util.EnumMap#EnumMap(java.util.Map<K,? extends V>)
   public EnumMap(Map<K, ? extends V> m) {
       if (m instanceof EnumMap) {
           EnumMap<K, ? extends V> em = (EnumMap<K, ? extends V>) m;
           keyType = em.keyType;
           keyUniverse = em.keyUniverse;
           vals = em.vals.clone();
           size = em.size;
       } else {
           if (m.isEmpty())
               throw new IllegalArgumentException("Specified map is empty");
           keyType = m.keySet().iterator().next().getDeclaringClass();
           keyUniverse = getKeyUniverse(keyType);
           vals = new Object[keyUniverse.length];
           //对于传入的Map赋值给新的EnumMap步骤就由EnumMap的putAll方法进行操作了。
           putAll(m);
       }
   }
   ```



### 返回不可变EnumMap

传入一个Map，返回一个不可变的Map容器

```java
public static <K extends Enum<K>, V> ImmutableMap<K, V> immutableEnumMap(Map<K, ? extends V> map) {
    if(map instanceof ImmutableEnumMap) {
        //如果map为ImmutableEnumMap类型，直接强转为ImmutableEnumMap类型的容器
        ImmutableEnumMap<K, V> result = (ImmutableEnumMap)map;
        return result;
    } else  {
        Iterator<? extends Map.Entry<K, ? extends V>> entryItr = map.entrySet().iterator();
        if (!entryItr.hasNext()) {
            //如果map为空，则直接返回新建的空的ImmutableMap容器
            return ImmutableMap.of();
        } else {
            Map.Entry<K, ? extends V> entry1 = (Map.Entry)entryItr.next();
            K key1 = (Enum)entry1.getKey();
            V value1 = entry1.getValue();
            CollectPreconditions.checkEntryNotNull(key1, value1);
            Class<K> clazz = key1.getDeclaringClass();
            EnumMap<K, V> enumMap = new EnumMap(clazz);
            enumMap.put(key1, value1);

            while(entryItr.hasNext()) {
                Map.Entry<K, ? extends V> entry = (Map.Entry)entryItr.next();
                K key = (Enum)entry.getKey();
                V value = entry.getValue();
                //key和value都不能为null
                CollectPreconditions.checkEntryNotNull(key, value);
                enumMap.put(key, value);
            }
			
            //以上成立，则直接使用ImmutableEnumMap中函数为map进行转换
            return ImmutableEnumMap.asImmutable(enumMap);
        }
    }
}
```



### 创建HashMap

1. 直接返回一个新的HashMap
   ```java
   public static <K, V> HashMap<K, V> newHashMap() {
       return new HashMap();
   }
   ```

2. 返回一个有初始长度的HashMap
   ```java
   public static <K, V> HashMap<K, V> newHashMapWithExpectedSize(int expectedSize) {
       return new HashMap(capacity(expectedSize));
   }
   
   //根据传入的长度，返回一个实际可用expectedSize的HashMap，capacity方法就是计算实际长度的一个方法
   static int capacity(int expectedSize) {
       if(expectedSize < 3) {
           CollectPreconditions.checkNonnegative(expectedSize, "expectedSize");
           return expectedSize + 1;
       } else {
           // 假设传入的是8，返回的就是8/0.75 + 1 = 11，根据HashMap的原则，会扩容成16。所以这里考虑的是什么？为什么这么设置
           return expectedSize < 1073741824 ? (int)((float)expectedSize / 0.75F + 1.0F) : Integer.MAX_VALUE;
       }
   }
   ```

   > 这里为什么是 0.75这个系数呢？
   >

3. 传入一个Map型变量，返回一个HashMap
   ```java
   public static <K, V> HashMap<K, V> newHashMap(Map<? extends K, ? extends V> map) {
       return new HashMap(map);//这个步骤就由HashMap的putMapEntries方法进行操作了
   }
   ```

   

### 创建LinkedHashMap

1. 直接返回一个新的LinkedHashMap
   ```java
   public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
       return new LinkedHashMap();
   }
   ```

2. 传入一个Map变量，返回一个LinkedHashMap
   ```java
   public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(Map<? extends K, ? extends V> map) {
       return new LinkedHashMap(map);//这个步骤就由LinkedHashMap的putMapEntries方法进行操作了。
   }
   ```

   

### 创建ConcurrentMap

直接返回一个新的ConcurrentMap
```java
public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
    return new ConcurrentHashMap();
}
```



### 创建TreeMap

1. 直接返回一个新的TreeMap
   ```java
   public static <K extends Comparable, V> TreeMap<K, V> newTreeMap() {
       return new TreeMap();
   }
   ```

2. 传入一个Map变量，返回一个TreeMap，并将Map的值赋值给TreeMap
   ```java
   public static <K, V> TreeMap<K, V> newTreeMap(SortedMap<K, ? extends V> map) {
       return new TreeMap(map);
   }
   ```

3. 传入一个比较接口，返回一个根据传入的比较规则形成的TreeMap
   ```java
   public static <C, K extends C, V> TreeMap<K, V> newTreeMap(@Nullable Comparator<C> comparator) {
       return new TreeMap(comparator);
   }
   ```

   

### 创建IdentityHashMap

直接返回一个identityHashMap

```java
public static <K, V> IdentityHashMap<K, V> newIdentityHashMap() {
    return new IdentityHashMap();
}
```

IdentityHashMap与HashMap不同之处在于可以存入相同类的相同值，实际上他在put里的添加操作是不是使用equals而是用的==进行判断的



### 获取两个Map中不同元素的值

在说明Maps中这三个方法之前，先了解一下MapDifference接口和的实现类MapDifferenceImpl可以表现什么吧：

```java
public interface MapDifference<K, V> {
    boolean areEqual();
    Map<K, V> entriesOnlyOnLeft();
    Map<K, V> entriesOnlyOnRight();
    Map<K, V> entriesInCommon();
    Map<K, ValueDifference<V>> entriesDiffering();
    boolean equals(@CheckForNull Object var1);
    int hashCode();

    @DoNotMock("Use Maps.difference")
    public interface ValueDifference<V> {
        @ParametricNullness
        V leftValue();
        @ParametricNullness
        V rightValue();
        
        boolean equals(@CheckForNull Object var1);
        int hashCode();
    }
}

static class MapDifferenceImpl<K, V> implements MapDifference<K, V> {
    final Map<K, V> onlyOnLeft;
    final Map<K, V> onlyOnRight;
    final Map<K, V> onBoth;
    final Map<K, ValueDifference<V>> differences;
 
    MapDifferenceImpl(Map<K, V> onlyOnLeft, Map<K, V> onlyOnRight, Map<K, V> onBoth, Map<K, ValueDifference<V>> differences) {
        this.onlyOnLeft = Maps.unmodifiableMap(onlyOnLeft);
        this.onlyOnRight = Maps.unmodifiableMap(onlyOnRight);
        this.onBoth = Maps.unmodifiableMap(onBoth);
        this.differences = Maps.unmodifiableMap(differences);
    }
    
    public boolean areEqual() {
        return this.onlyOnLeft.isEmpty() && this.onlyOnRight.isEmpty() && this.differences.isEmpty();
    }
    public Map<K, V> entriesOnlyOnLeft() {return this.onlyOnLeft; }
    public Map<K, V> entriesOnlyOnRight() {return this.onlyOnRight; }
    public Map<K, V> entriesInCommon() {return this.onBoth; }
    public Map<K, ValueDifference<V>> entriesDiffering() { return this.differences; }
    
    //equals等其它方法...
}
```

可以看到 MapDifferenceImpl 实现类中有4个变量

- onlyOnLeft只存变量名为left的Map中独有的；
- onlyOnRight只存变量名为right的Map中独有的；
- onBoth存储两个map中共有的key并且value也相等的元素；
- differences因为value存储的类型为ValueDifference，differences中存储的是共有的key并且value不同的元素



1. 传入两个Map变量，根据left变量名的Map的类型进行判断交给哪个difference方法去处理
   ```java
   public static <K, V> MapDifference<K, V> difference(Map<? extends K, ? extends V> left, Map<? extends K, ? extends V> right) {
       if(left instanceof SortedMap) {
           SortedMap<K, ? extends V> sortedLeft = (SortedMap)left;
           return difference(sortedLeft, right);
       } else {
           MapDifference<K, V> result = difference(left, right, Equivalence.equals());
           return result;
       }
   }
   ```

2. 传入两个Map，使用doDifference方法将两个Map中的元素进行分类

   ```java
   public static <K, V> MapDifference<K, V> difference(Map<? extends K, ? extends V> left, Map<? extends K, ? extends V> right, Equivalence<? super V> valueEquivalence) {
       Preconditions.checkNotNull(valueEquivalence);
       HashMap onlyOnLeft = newHashMap();
       HashMap onlyOnRight = new HashMap(right);
       HashMap onBoth = newHashMap();
       Map<K, MapDifference.ValueDifference<V>> differences = newLinkedHashMap();
       doDifference(left, right, valueEquivalence, onlyOnLeft, onlyOnRight, onBoth, differences);
       return new Maps.MapDifferenceImpl(onlyOnLeft, onlyOnRight, onBoth, differences);
   }
   
   private static <K, V> void doDifference(Map<? extends K, ? extends V> left, Map<? extends K, ? extends V> right, Equivalence<? super V> valueEquivalence, Map<K, V> onlyOnLeft, Map<K, V> onlyOnRight, Map<K, V> onBoth, Map<K, MapDifference.ValueDifference<V>> differences) {
       Iterator var7 = left.entrySet().iterator();
   
       //对left进行遍历操作
       while(var7.hasNext()) {
           Map.Entry<? extends K, ? extends V> entry = (Map.Entry)var7.next();
           K leftKey = entry.getKey();
           V leftValue = entry.getValue();
           //查看right中是否包括这个key值
           if (right.containsKey(leftKey)) {
               //如果right中也包括这个key值，则将这个值在right中去除
               V rightValue = NullnessCasts.uncheckedCastNullableTToT(onlyOnRight.remove(leftKey));
               //判断这个key值的left和right的value值是否相等
               if (valueEquivalence.equivalent(leftValue, rightValue)) {
                   //如果两个value值相等，将其填入onBoth的Map容器中
                   onBoth.put(leftKey, leftValue);
               } else {
                   //如果两个value值不相等，将其填入differences的Map容器中
                   differences.put(leftKey, Maps.ValueDifferenceImpl.create(leftValue, rightValue));
               }
           } else {
               //如果这个key在right中不存在，则将其填入onlyOnLeft容器中
               onlyOnLeft.put(leftKey, leftValue);
           }
       }
   }
   ```

3. 传入一个SortedMap和一个Map变量，返回一个分类后的类
   ```java
   public static <K, V> SortedMapDifference<K, V> difference(SortedMap<K, ? extends V> left, Map<? extends K, ? extends V> right) {
       Preconditions.checkNotNull(left);
       Preconditions.checkNotNull(right);
       Comparator<? super K> comparator = orNaturalOrder(left.comparator());
       SortedMap<K, V> onlyOnLeft = newTreeMap(comparator);
       SortedMap<K, V> onlyOnRight = newTreeMap(comparator);
       onlyOnRight.putAll(right);
       SortedMap<K, V> onBoth = newTreeMap(comparator);
       SortedMap<K, MapDifference.ValueDifference<V>> differences = newTreeMap(comparator);
       doDifference(left, right, Equivalence.equals(), onlyOnLeft, onlyOnRight, onBoth, differences);
       return new SortedMapDifferenceImpl(onlyOnLeft, onlyOnRight, onBoth, differences);
   }
   ```

   

### 根据函数和set，构造Map

这个方法展示了如何将一个`Set`和一个`Function` 结合起来，创建一个视图（view），这个视图在每次查询时通过应用函数来动态生成键值对。

1. **视图特性**：`AsMapView`创建的是一个视图，而不是一个独立的新集合。这意味着原始集合（`Set`）的任何修改都会反映在`AsMapView`中，反之亦然。这种行为类似于如何通过`Collections.unmodifiableList()`方法得到的不可修改的视图，但`AsMapView`是可修改的，其修改会影响到原始的集合。
2. **延迟加载**：`AsMapView`在实际调用其`get()`方法之前不会计算键对应的值。这意味着如果你有一个非常昂贵的转换逻辑，它只有在实际需要该值时才会执行，这有助于提高效率。
3. **用途**：这个类非常适合创建动态计算的映射，其中映射的值是依赖于键的，并且可能不希望提前计算所有可能的值。例如，可以用它来根据需要生成配置设置、进行数据转换等。
4. **实现**：在内部，`AsMapView`使用了提供的`Set`和`Function`来实现`Map`接口。当调用`get(Object key)`时，如果`key`存在于集合中，则使用`Function`来计算值。

使用案例：

```java
//假设有一组产品ID，需要根据产品ID动态获取产品价格。价格计算可能依赖于多种因素，如商品的实时供需状况、促销活动等，这些都可以通过一个函数来实现：
Set<Integer> productIds = new HashSet<>();
productIds.add(1);
productIds.add(2);

Function<Integer, Double> priceFunction = productId -> {
    // 假设这里进行一些复杂的计算来决定价格
    return productId * 10.0; // 简单的示例
};

Map<Integer, Double> priceMap = Maps.asMap(productIds, priceFunction);

System.out.println("Price of Product 1: " + priceMap.get(1));  // 输出 10.0
System.out.println("Price of Product 2: " + priceMap.get(2));  // 输出 20.0
```



方法介绍：

1. 传入一个set和一个规则，返回一个Map

   ```java
   public static <K, V> Map<K, V> asMap(Set<K> set, Function<? super K, V> function) {
       return new AsMapView(set, function);
   }
   ```

2. 传入一个SortedSet和一个规则，返回一个SortMap
   ```java
   public static <K, V> SortedMap<K, V> asMap(SortedSet<K> set, Function<? super K, V> function) {
       return new SortedAsMapView(set, function);
   }
   ```

3. 传入一个NavigableSet和一个规则，返回一个NavigableMap
   ```java
   public static <K, V> NavigableMap<K, V> asMap(NavigableSet<K> set, Function<? super K, V> function) {
       return new Maps.NavigableAsMapView(set, function);
   }
   ```

   

### 根据函数和迭代器，构造不可变的Map

此类方法传入的是一个容器的迭代器和一个规则，然后返回一个不可变的Map容器

1. 传入一个key值容器和一个规则，直接交给重载函数去处理，返回一个不可变的Map容器
   ```java
   public static <K, V> ImmutableMap<K, V> toMap(Iterable<K> keys, Function<? super K, V> valueFunction) {
       return toMap((Iterator)keys.iterator(), valueFunction);
   }

2. 传入一个key值迭代器和一个规则返回一个不可变的map容器
   ```java
   public static <K, V> ImmutableMap<K, V> toMap(Iterator<K> keys, Function<? super K, V> valueFunction) {
       Preconditions.checkNotNull(valueFunction);
       LinkedHashMap builder = newLinkedHashMap();
       //使用迭代器中的值作为key值，使用规则计算出的值作为value值，存入builder中
       while(keys.hasNext()) {
           Object key = keys.next();
           builder.put(key, valueFunction.apply(key));
       }
       //返回一个不可变的容器
       return ImmutableMap.copyOf(builder);
   }
   ```

3. 传入一个value值容器和一个规则，直接交给重载函数去处理，返回一个不可变的Map容器
   ```java
   public static <K, V> ImmutableMap<K, V> uniqueIndex(Iterable<V> values, Function<? super V, K> keyFunction) {
       return uniqueIndex((Iterator)values.iterator(), keyFunction);
   }
   ```

4. 传入一个value值迭代器和一个规则返回一个不可变的map容器
   ```java
   public static <K, V> ImmutableMap<K, V> uniqueIndex(Iterator<V> values, Function<? super V, K> keyFunction) {
       Preconditions.checkNotNull(keyFunction);
       Builder builder = ImmutableMap.builder();
       //使用迭代器中的值作为value值，使用规则计算出的值作为key值，存入builder中
       while(values.hasNext()) {
           Object value = values.next();
           builder.put(keyFunction.apply(value), value);
       }
       //返回一个不可变的容器
       return builder.build();
   }
   ```

   

### 从配置文件中读取数据，创建不可变的Map

从Properties获取的key和value，返回一个不可变的Map
```java
public static ImmutableMap<String, String> fromProperties(Properties properties) {
    Builder builder = ImmutableMap.builder();
    Enumeration e = properties.propertyNames();
    //从properties获取的key和value，赋值到builder中
    while(e.hasMoreElements()) {
        String key = (String)e.nextElement();
        builder.put(key, properties.getProperty(key));
    }
    //返回一个不可变的Map
    return builder.build();
}
```



### 返回Entry或Entry集合

传入一个key和一个value，返回一个不可变的Entry

```java
public static <K, V> Entry<K, V> immutableEntry(@Nullable K key, @Nullable V value) {
    return new ImmutableEntry(key, value);
}
```



### 返回特殊的BiMap类

Guava 提供了 BiMap 支持支持双向的映射关系，关于BiMap详情可以看后文

1. 传入一个BiMap返回一个线程安全的BiMap
   ```java
   public static <K, V> BiMap<K, V> synchronizedBiMap(BiMap<K, V> bimap) {
       return Synchronized.biMap(bimap, (Object)null);
   }
   ```

2. 传入一个BiMap返回一个unmodifiableBiMap
   ```java
   public static <K, V> BiMap<K, V> unmodifiableBiMap(BiMap<? extends K, ? extends V> bimap) {
       return new Maps.UnmodifiableBiMap(bimap, (BiMap)null);
   }
   ```

   

### 根据Map和函数对Map进行转型

此类方法使用使用到了函数式编程，将一个Map的value作为新的Map的key，根据函数的规则计算出新的Map的Value，而这个转换只有在查看的时候才会做计算，而真正存储的是传入的map

1. 传入一个Map和一个规则，返回一个有规则计算出来的Map
   ```java
   public static <K, V1, V2> Map<K, V2> transformValues(Map<K, V1> fromMap, Function<? super V1, V2> function) {
       return transformEntries((Map)fromMap, asEntryTransformer(function));
   }
   ```

2. 传入一个SortedMap和一个规则，返回一个由规则计算出来的新的Map
   ```java
   public static <K, V1, V2> SortedMap<K, V2> transformValues(SortedMap<K, V1> fromMap, Function<? super V1, V2> function) {
       return transformEntries((SortedMap)fromMap, asEntryTransformer(function));
   }
   ```

3. 传入一个NavigableMap和一个规则，返回一个由规则计算出来的NavigableMap
   ```java
   public static <K, V1, V2> NavigableMap<K, V2> transformValues(NavigableMap<K, V1> fromMap, Function<? super V1, V2> function) {
       return transformEntries((NavigableMap)fromMap, asEntryTransformer(function));
   }
   ```

4. 传入一个Map和一个Maps规定的规则格式，根据规则返回一个新的Map
   ```java
   public static <K, V1, V2> Map<K, V2> transformEntries(Map<K, V1> fromMap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
       return (Map)(fromMap instanceof SortedMap?transformEntries((SortedMap)((SortedMap)fromMap), transformer):new Maps.TransformedEntriesMap(fromMap, transformer));
   }
   ```

5. 传入一个NavigableMap和一个Maps规定的规则格式，根据规则返回一个新的NavigableMap
   ```java
   public static <K, V1, V2> NavigableMap<K, V2> transformEntries(NavigableMap<K, V1> fromMap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
       return new Maps.TransformedEntriesNavigableMap(fromMap, transformer);
   }
   ```



### 使用函数进行过滤Map，然后返回同类型的Map

> 这里我们主要针对Key进行过滤的源码进行分析。当然Maps还提供了一些对Value、Entry、含有过滤器的Map进行过滤的方法。与上面过滤key的方法大体一样，都是继承了AbstractFilteredMap抽象类，实现了各自的过滤功能

使用keyPredicate函数接口制定过滤规则，对Map进行过滤，对于Map中Key进行过滤的类FilteredKeyMap源码如下：

```java
// 对KeySet进行了过滤处理，使用了Set中的filter方法
private static class FilteredKeyMap<K, V> extends Maps.AbstractFilteredMap<K, V> {
    Predicate<? super K> keyPredicate;
    FilteredKeyMap(Map<K, V> unfiltered, Predicate<? super K> keyPredicate, Predicate<? super Entry<K, V>> entryPredicate) {
        super(unfiltered, entryPredicate);
        this.keyPredicate = keyPredicate;
    }
    protected Set<Entry<K, V>> createEntrySet() {
        return Sets.filter(this.unfiltered.entrySet(), this.predicate);
    }
 
    Set<K> createKeySet() {
        return Sets.filter(this.unfiltered.keySet(), this.keyPredicate);
    }
 
    public boolean containsKey(Object key) {
        return this.unfiltered.containsKey(key) && this.keyPredicate.apply(key);
    }
}
```



1. 传入一个Map和过滤他的规则，返回一个新的Map
   ```java
   public static <K, V> Map<K, V> filterKeys(Map<K, V> unfiltered, Predicate<? super K> keyPredicate) {
           Preconditions.checkNotNull(keyPredicate);
           Predicate<Map.Entry<K, ?>> entryPredicate = keyPredicateOnEntries(keyPredicate);
           return (Map)(unfiltered instanceof AbstractFilteredMap ? filterFiltered((AbstractFilteredMap)unfiltered, entryPredicate) : new FilteredKeyMap((Map)Preconditions.checkNotNull(unfiltered), keyPredicate, entryPredicate));
       }
   ```

2. 传入一个SortedMap，然后交给filterEntries方法进行处理
   ```java
   public static <K, V> SortedMap<K, V> filterKeys(SortedMap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
       return filterEntries(unfiltered, keyPredicateOnEntries(keyPredicate));
   }
   ```

3. 传入一个NavigableMap，然后交给filterEntries方法进行处理
   ```java
   public static <K, V> NavigableMap<K, V> filterKeys(NavigableMap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
       return filterEntries((NavigableMap)unfiltered, keyPredicateOnEntries(keyPredicate));
   }
   ```

4. 传入一个BiMap，然后交给filterEntries方法进行处理
   ```java
   public static <K, V> BiMap<K, V> filterKeys(BiMap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
       Preconditions.checkNotNull(keyPredicate);
       return filterEntries((BiMap)unfiltered, keyPredicateOnEntries(keyPredicate));
   }
   ```



## Sets

### 功能函数

| 功能                              | 方法                                                         |
| --------------------------------- | ------------------------------------------------------------ |
| 创建一个不可变的set               | 1、ImmutableSet&lt;E> immutableEnumSet(E anElement, E... otherElements)<br />2、ImmutableSet&lt;E> immutableEnumSet(Iterable&lt;E> elements) |
| 创建一个HashSet                   | 1、HashSet&lt;E> newHashSet()<br/>2、HashSet&lt;E> newHashSet(E... elements)<br/>3、HashSet&lt;E> newHashSetWithExpectedSize(int expectedSize)<br/>4、HashSet&lt;E> newHashSet(Iterable<? extends E> elements)<br/>5、HashSet&lt;E> newHashSet(Iterator<? extends E> elements) |
| 创建一个线程安全的Set             | 1、Set&lt;E> newConcurrentHashSet()<br />2、Set&lt;E> newConcurrentHashSet(Iterable<? extends E> elements) |
| 创建一个LinkedHashMap             | 1、LinkedHashSet&lt;E> newLinkedHashSet()<br/>2、LinkedHashSet&lt;E> newLinkedHashSetWithExpectedSize(int expectedSize)<br/>3、LinkedHashSet&lt;E> newLinkedHashSet(Iterable<? extends E> elements) |
| 创建一个TreeSet                   | 1、TreeSet&lt;E> newTreeSet()<br />2、TreeSet&lt;E> newTreeSet(Iterable<? extends E> elements)<br />3、TreeSet&lt;E> newTreeSet(Comparator<? super E> comparator) |
| 创建一个IdentityHashSet           | Set&lt;E> newIdentityHashSet()                               |
| 创建一个CopyOnWriteArraySet       | 1、CopyOnWriteArraySet&lt;E> newCopyOnWriteArraySet()<br />2、CopyOnWriteArraySet&lt;E> newCopyOnWriteArraySet(Iterable<? extends E> elements) |
| 创建一个EnumSet                   | 1、EnumSet&lt;E> newEnumSet(Iterable&lt;E> iterable, Class&lt;E> elementType)<br/>2、EnumSet&lt;E> complementOf(Collection&lt;E> collection)<br/>3、EnumSet&lt;E> complementOf(Collection&lt;E> collection, Class&lt;E> type)<br/>4、EnumSet&lt;E> makeComplementByHand(Collection&lt;E> collection, Class&lt;E> type) |
| 根据一个Map创建一个Set            | Set&lt;E> newSetFromMap(Map<E, Boolean> map)                 |
| 以两个Set的并集作为视图           | Sets.SetView&lt;E> union(final Set<? extends E> set1, final Set<? extends E> set2) |
| 以两个Set的交集作为视图           | Sets.SetView&lt;E> intersection(final Set&lt;E> set1, final Set<?> set2) |
| 以两个Set的互不重叠的部分作为视图 | Sets.SetView&lt;E> difference(final Set&lt;E> set1, final Set<?> set2) |
| 以两个Set的对称部分作为视图       | Sets.SetView&lt;E> symmetricDifference(Set<? extends E> set1, Set<? extends E> set2) |
| 过滤Set                           | 1、filter(Set&lt;E> unfiltered, Predicate<? super E> predicate)<br/>2、SortedSet&lt;E> filter(SortedSet&lt;E> unfiltered, Predicate<? super E> predicate)<br/>3、SortedSet&lt;E> filterSortedIgnoreNavigable(SortedSet&lt;E> unfiltered, Predicate<? super E> predicate)<br/>4、NavigableSet&lt;E> filter(NavigableSet&lt;E> unfiltered, Predicate<? super E> predicate) |
| 获取两个Set集合的笛卡尔积         | 1、Set<List&lt;B>> cartesianProduct(List<? extends Set<? extends B>> sets)<br />2、Set<List&lt;B>> cartesianProduct(Set<? extends B>... sets) |



### 创建一个不可变的Set





