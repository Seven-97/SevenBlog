---
title: Guava
category: 开发工具
tag:
 - 工具类库
---



## 集合

### Immutable

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

#### JDK不可变集合存在的问题

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



#### Guava不可变集合案例

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



#### 更智能的copyOf

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



#### Guava集合和不可变对应关系

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

 

### Lists

```java
private Lists() {
}
```

私有的构造方法，可以看到这是一个真正的功能函数，下面对其函数进行分析

 

#### 功能函数

首先根据每一个函数的更能进行了分类：

| 功能                                                     | 方法                                                         |
| -------------------------------------------------------- | ------------------------------------------------------------ |
| 创建ArrayList方法                                        | newArrayList()<br/>newArrayList(E... elements)<br/>newArrayList(Iterable<? extends E> elements)<br/>newArrayList(Iterator<? extends E> elements)<br/>newArrayListWithCapacity(int initialArraySize)<br/>newArrayListWithExpectedSize(int estimatedSize) |
| 创建LinkedList方法                                       | newLinkedList()<br />newLinkedList(Iterable<? extends E> elements) |
| 创建CopyOnWriteArrayList方法                             | newCopyOnWriteArrayList()<br />newCopyOnWriteArrayList(Iterable<? extends E> elements) |
| 创建自制List规则                                         | asList(@Nullable E first, E[] rest)<br />asList(@Nullable E first, @Nullable E second, E[] rest) |
| 对多个List做笛卡尔乘积                                   | cartesianProduct(List<? extends List<? extends B>> lists)<br />cartesianProduct(List<? extends B>... lists) |
| 将一个fromList中的元素根据指定的function转换为另一种类型 | transform(List&lt;F> fromList, Function<? super F, ? extends T> function) |
| 对list进行分批输出的方法（作用之一：分页）               | partition(List&lt;T> list, int size)                         |
| 将字符串作为字符数组进行操作                             | charactersOf(String string)<br />charactersOf(CharSequence sequence) |
| 将list逆序                                               | reverse(List&lt;T> list)                                     |



#### 创建ArrayList方法

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



#### 创建LinkedList方法

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



#### 创建CopyOnWriteArrayList方法

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



#### 创建自制List规则

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

   

#### 对多个List做笛卡尔乘积

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



#### 将一个fromList中的元素根据指定的function转换为另一种类型

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



#### 对list进行分批输出的方法（作用之一：分页）



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



#### 将字符串作为字符数组进行操作

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



#### list逆序

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



### Maps

```java
private Maps() {
}
```

私有的构造方法，可以看到这是一个真正的功能函数，下面对其函数进行分析



#### 功能函数







### 新集合类型

#### Multiset

Multiset 是一个新的集合类型，可以多次添加相等的元素，既可以看成是无序的列表，也可以看成存储元素和对应数量的键值对映射`[E1: cnt1; E2:cnt2]`。常用实现包括 `HashMultiset, TreeMultiset, LinkedHashMultiset...`

```java
Multiset<String> multiset = HashMultiset.create();
multiset.add("A");
multiset.add("A");
multiset.add("B");
// 输出：[A x 2, B]
log.debug("{}", multiset);

// 元素总数
log.debug("{}", multiset.size());
// 不重复元素个数
log.debug("{}", multiset.elementSet().size());
// 设置元素计数
multiset.setCount("A", 3);
// 获取元素个数
log.debug("{}", multiset.count("A"));
```

#### Multimap

支持将 key 映射到多个 value 的方式，而不用定义`Map<K, List<V>> 或 Map<K, Set<V>>`这样的哈皮形式。实现类包括`ArrayListMultimap, HashMultimap, LinkedListMultimap, TreeMultimap...`

```java
// 列表实现
ListMultimap<String, Integer> listMultimap = MultimapBuilder.hashKeys().arrayListValues().build();
// 集合实现
SetMultimap<String, Integer> setMultimap = MultimapBuilder.treeKeys().hashSetValues().build();

listMultimap.put("A", 1);
listMultimap.put("A", 2);
listMultimap.put("B", 1);
// {A=[1, 2], B=[1, 2]}
log.debug("{}", listMultimap);
// [1, 2]，不存在则返回一个空集合
log.debug("{}", listMultimap.get("A"));
// [1, 2] 移除 key 关联的所有 value
List<Integer> valList = listMultimap.removeAll("A");

// 返回普通 map 的视图，仅支持 remove，不能 put，且会更新原始的 listMultimap
Map<String, Collection<Integer>> map = listMultimap.asMap();
```

#### BiMap

Map 可以实现 key -> value 的映射，如果想要 value -> key 的映射，就需要定义两个 Map，并且同步更新，很不优雅。Guava 提供了 BiMap 支持支持双向的映射关系，常用实现有`HashMap, EnumBiMap, EnumHashBiMap...`

```java
BiMap<String, Integer> biMap = HashBiMap.create();
biMap.put("A", 100);

// 删除已存在的 KV，重新添加 KV
biMap.forcePut("A", 200);

// 获取反向映射
BiMap<Integer, String> inverse = biMap.inverse();
log.debug("{}", inverse.get(100));
```

#### Table

当需要同时对多个 key 进行索引时，需要定义`Map<key1, Map<key2, val>>`这样的形式，ugly and awkward。Guava 提供了 Table 用于支持类似 row、column 的双键映射。实现包括`HashBasedTable, TreeBasedTable, ArrayTable...`

```java
// Table<R, C, V>
Table<String, String, Integer> table = HashBasedTable.create();
table.put("row1", "col1", 1);
table.put("row1", "col2", 2);
table.put("row2", "col1", 3);
table.put("row2", "col2", 4);

// 获取类似 Map.Entry 的 Table.Cell<R, C, V>
table.cellSet().forEach(System.out::println);

// Map<R, Map<C, V>> 视图
Map<String, Map<String, Integer>> rowMap = table.rowMap();
Set<String> rowKeySet = table.rowKeySet();
// Map<C, Map<R, V>> 视图（基于列的索引效率通常比行低）
Map<String, Map<String, Integer>> columnMap = table.columnMap();
Set<String> columnedKeySet = table.columnKeySet();
```

#### 其它

- ClassToInstanceMap：Class -> Instance 的映射，可以避免强制类型转换
- RangeSet (Beta状态)：范围集合，自动合并、分解范围区间

```java
RangeSet<Integer> rangeSet = TreeRangeSet.create();
rangeSet.add(Range.closed(1, 10)); // {[1, 10]}
rangeSet.add(Range.closedOpen(11, 15)); // disconnected range: {[1, 10], [11, 15)}
rangeSet.add(Range.closedOpen(15, 20)); // connected range; {[1, 10], [11, 20)}
rangeSet.add(Range.openClosed(0, 0)); // empty range; {[1, 10], [11, 20)}
rangeSet.remove(Range.open(5, 10)); // splits [1, 10]; {[1, 5], [10, 10], [11, 20)}

// [[1..5], [10..10], [11..20)]
System.out.println(rangeSet);
```

- RangeMap (Beta状态)：range -> value 的映射，不会自动合并区间

```java
RangeMap<Integer, String> rangeMap = TreeRangeMap.create();
rangeMap.put(Range.closed(1, 10), "foo"); // {[1, 10] => "foo"}
rangeMap.put(Range.open(3, 6), "bar"); // {[1, 3] => "foo", (3, 6) => "bar", [6, 10] => "foo"}
rangeMap.put(Range.open(10, 20), "foo"); // {[1, 3] => "foo", (3, 6) => "bar", [6, 10] => "foo", (10, 20) => "foo"}
rangeMap.remove(Range.closed(5, 11)); // {[1, 3] => "foo", (3, 5) => "bar", (11, 20) => "foo"}

// [[1..3]=foo, (3..5)=bar, (11..20)=foo]
System.out.println(rangeMap);
```

### 集合工具

JDK 自带的`java.util.Collections`提供了很多实用的功能，而 Guava 针对特定接口提供了更多工具，例如：

- 提供很多静态工厂方法、包装器
- Iterables 支持懒加载的集合视图操作
- Sets 提供集合论运算
- Maps 提供 diff 计算
- Multimaps 提供 Map -> Multimap 的转换，并支持反向映射
- Tables 提供行列转置 | **Interface** | **JDK or Guava** | **Guava Utility Class** | | --- | --- | --- | | Collection | JDK | Collections2 | | List | JDK | Lists | | Set | JDK | Sets | | Map | JDK | Maps | | Queue | JDK | Queues | | Multiset | Guava | Multisets | | Multimap | Guava | Multimaps | | BiMap | Guava | Maps | | Table | Guava | Tables |

另外，还可以继承 Forwarding 通过装饰器模式装饰特殊实现，PeekingIterator 可以 peek 下一次返回的元素，AbstractIterator 自定义迭代方式等等。（PS：这些东西也许有点用，可是能用吗，业务里写这种代码怕是要被人喷死...）

## 缓存

Guava 提供的 Cache 按照 **有则取值，无则计算 **的逻辑，支持自动装载，自动移除等扩展功能，比传统的`ConcurrentHashMap`功能更加强大。

### 使用

除了继承 AbstractCache 自定义缓存实现外，通常可以直接使用 CacheBuilder 构建一个 LocalLoadingCache 缓存，通过 key 来懒加载相关联的 value。如果 key-value 没有关联关系可以使用无参的 build 方法返回一个`LocalManualCache`对象，等调用 get 时再传递一个 callable 对象获取 value。

```java
LoadingCache<String, Integer> loadingCache = CacheBuilder.newBuilder()
                // 最大容量
				.maximumSize(1000)
            	// 定时刷新
				.refreshAfterWrite(10, TimeUnit.MINUTES)
                // 添加移除时的监听器
                .removalListener(notification -> System.out.println(
                    notification.getKey() + " -> " + 
                    notification.getValue() + " is removed by " + 
                    notification.getCause()))
            	// 并发等级
				.concurrencyLevel(4)
				// 开启统计功能
            	.recordStats()
                .build(
                        new CacheLoader<String, Integer>() {
                            @Override
                            public Integer load(String key) {
                                System.out.println("Loading key: " + key);
                                return Integer.parseInt(key);
                            }
                        });

loadingCache.get("key");
// 使失效
loadingCache.invalidate("key");
// get with callable
loadingCache.get("key", () -> 2));
// 刷新缓存
loadingCache.refresh("key");
// 返回一个 map 视图
ConcurrentMap<String, Integer> map = loadingCache.asMap();
```

`LocalLoadingCache`继承自 `LocalManualCache`，里面封装了一个继承自 Map 的`localCache`成员存储实际的 KV 并通过分段锁实现线程安全，另外实现了 Cache 接口定义了一系列缓存操作。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406262329345.jpeg)

```java
class LocalCache<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>
```

### 失效

Guava 提供了三种基础的失效策略：

- 基于容量失效：
  - `maximumSize(long)` 指定最大容量
  - `weigher(Weigher)`实现自定义权重，配合`maximumWeight(long)`以权重作为容量
- 基于时间失效：在写入、偶尔读取期间执行定期维护
  - `expireAfterAccess(long, timeUnit)` 读写后超出指定时间即失效
  - `expireAfterWrite(long, timeUnit)` 写后超出指定时间即失效
- 基于引用失效：
  - `weakKeys()`通过弱引用存储 key
  - `weakValues()`通过弱引用存储 value
  - `softValues()` 通过软引用包装 value，以 LRU 方式进行 GC

另外，也可以通过`invalidate`手动清除缓存。缓存不会自动进行清理，Guava 会在写操作期间，或者偶尔在读操作时进行过期失效的维护工作。缓存刷新操作的时机也是类似的。

详情可以看这篇文章 [GuavaCache](https://www.seven97.top/system-design/cache-column/guavacache.html)

## 图

`com.google.common.graph`提供了多种图的实现：

- **Graph** 边是匿名的，且不关联任何信息
- **ValueGraph** 边拥有自己的值，如权重、标签
- **Network **边对象唯一，且期望实施对其引用的查询

整体看下来，感觉还是挺复杂的，不太常用。等有需要再学习吧...

## 并发

### ListenableFuture

JUC 的 Future 接口提供了一种异步获取任务执行结果的机制，表示一个异步计算的结果。

```java
ExecutorService executor = Executors.newFixedThreadPool(1);
Future<String> future = executor.submit(() -> {
    // 执行异步任务，返回一个结果
    return "Task completed";
});
// Blocked
String result = future.get();
```

Executor 实际返回的是实现类 FutureTask，它同时实现了 Runnable 接口，因此可以手动创建异步任务。

```java
FutureTask<String> futureTask = new FutureTask<>(new Callable<String>() {
    @Override
    public String call() throws Exception {
        return "Hello";
    }
});
        
new Thread(futureTask).start();
System.out.println(futureTask.get());
```

而 Guava 提供的 ListenableFuture 更进一步，允许注册回调，在任务完成后自动执行，实际也是使用它的实现类 ListenableFutureTask。

```java
// 装饰原始的线程池
ListeningExecutorService listeningExecutorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
ListenableFuture<String> future = listeningExecutorService.submit(() -> {
    // int i = 1 / 0;
    return "Hello";
});

// 添加回调 1
Futures.addCallback(future, new FutureCallback<String>() {
    // 任务成功时的回调
    @Override
    public void onSuccess(String result) {
        System.out.println(result);
    }

    // 任务失败时的回调
    @Override
    public void onFailure(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }
}, listeningExecutorService);

// 添加回调 2
future.addListener(new Runnable() {
    @Override
    public void run() {
        System.out.println("Done");
    }
}, listeningExecutorService);
```

### Service

Guava 的 Service 框架是一个用于管理服务生命周期的轻量级框架。它提供了一个抽象类 AbstractService 和一个接口 Service，可以通过继承 AbstractService 或者直接实现 Service 接口来创建自定义的服务，并使用 ServiceManager 来管理这些服务的生命周期。

```java
public class MyService extends AbstractService {
    @Override
    protected void doStart() {
        // 在这里执行启动服务的逻辑
        System.out.println("MyService is starting...");
        notifyStarted();
    }
    
    @Override
    protected void doStop() {
        // 在这里执行停止服务的逻辑
        System.out.println("MyService is stopping...");
        notifyStopped();
    }
}


@Test
public void testService() {
    Service service = new MyService();
    ServiceManager serviceManager = new ServiceManager(List.of(service));
    
    serviceManager.startAsync().awaitHealthy();
    
    // 主线程逻辑
    
    serviceManager.stopAsync().awaitStopped();
}
```



## 基本类型工具

Java 的基本类型包括8个：byte、short、int、long、float、double、char、boolean。Guava 提供了若干工具以支持基本类型和集合 API 的交互、字节数组转换、无符号形式的支持等等。

| **基本类型** | **Guava 工具类**                    |
| ------------ | ----------------------------------- |
| byte         | Bytes, SignedBytes, UnsignedBytes   |
| short        | Shorts                              |
| int          | Ints, UnsignedInteger, UnsignedInts |
| long         | Longs, UnsignedLong, UnsignedLongs  |
| float        | Floats                              |
| double       | Doubles                             |
| char         | Chars                               |
| boolean      | Booleans                            |

## Range

Guava 提供了 Range 类以支持范围类型，并且支持范围的运算，比如包含、交集、并集、查询等等。

```java
Range.open(a, b);			// (a, b)
Range.closed(a, b);			// [a..b]
Range.closedOpen(a, b);		// [a..b)
Range.openClosed(a, b);		// (a..b]
Range.greaterThan(a);		// (a..+∞)
Range.atLeast(a);			// [a..+∞)
Range.lessThan(a);			// (-∞..b)
Range.atMost(a);			// (-∞..b]
Range.all();				// (-∞..+∞)

// 通用创建方式
Range.range(a, BoundType.CLOSED, b, BoundType.OPEN);
```



## IO

Guava 使用术语 **流 **来表示可关闭的，并且在底层资源中有位置状态的 I/O 数据流。字节流对应的工具类为 ByteSterams，字符流对应的工具类为 CharStreams。
Guava 中为了避免和流直接打交道，抽象出可读的 **源 source** 和可写的 **汇 sink** 两个概念，指可以从中打开流的资源，比如 File、URL，同样也分别有字节和字符对应的源和汇，定义了一系列读写的方法。
除此之外，Files 工具类提供了对文件的操作。（PS：个人觉得，JDK 的 IO 流已经够麻烦的了，又来一套 API 太乱了，而且也没有更好用吧。还是先统一用 JDK 标准库里的好一点。）



## Hash

JDK 内置的哈希限定为 32 位的 int 类型，虽然速度很快但质量一般，容易产生碰撞。为此 Guava 提供了自己的 Hash 包。

- `Hashing` 类内置了一系列的散列函数对象 `HashFunction`，包括 `murmur3, sha256, adler32, crc32`等等。
- 确定 HashFunction 后进而拿到继承自 PrimitiveSink 的 Hasher 对象。
- 作为一个汇，可以往 Hasher 里输入数据，可以是内置类型，也可以是自定义类型，但需要传递一个 Funnel 定义对象分解的方式。
- 最后计算得到 HashCode 对象。

```java
HashFunction hf = Hashing.adler32();

User user = new User("chanper", 24);
HashCode hash = hf.newHasher()
        .putLong(20L)
        .putString("chanper", StandardCharsets.UTF_8)
    	// 输入自定义类，同时需要一个 Funnel
		.putObject(user, userFunnel)
        .hash();

// Funnel 定义对象分解的方式
Funnel<User> userFunnel = new Funnel<>() {
    @Override
    public void funnel(User user, PrimitiveSink into) {
        into
            .putString(user.name(), StandardCharsets.UTF_8)
            .putInt(user.age());
    }
};
```

另外，Guava 库也内置了一个使用简便布隆过滤器。

```java
// funnel 对象，预期的插入数量，false positive probability
BloomFilter<User> friends = BloomFilter.create(userFunnel, 500, 0.01);
for (int i = 0; i < 1000; i++) {
    friends.put(new User("user_" + i, 24));
}

if(friends.mightContain(somebody)) {
    System.out.println("somebody is in friends");
}
```

## EventBus

EventBus 是 Guava 提供的一个事件总线库，用于简化组件之间的通信。通过 EventBus，你可以实现发布/订阅模式，组件之间可以松散地耦合，使得事件的发布者（Producer）和订阅者（Subscriber）之间不需要直接依赖彼此。 使用时注意：

- 一个订阅者可以处理多个不同的事件，取决于处理方法的参数，并且支持泛型的通配符
- 没有对应的监听者则会把事件封装为`DeadEvent`，可以定义对应的监听器

```java
// 事件类型
public record MessageEvent(String message) {}

public class MessageSubscriber {
    // 事件处理方法标记
    @Subscribe
    public void handleMessageEvent(MessageEvent event) {
        System.out.println("Received message: " + event.message());
    }

    // 一个订阅者可以处理多个事件
    @Subscribe
    public void handleMessageEvent(MessageEvent2 event2) {
        System.out.println("Received message2: " + event2.message());
    }
}

@Test
public void testEvnetBus() {
    // 创建EventBus实例
    EventBus eventBus = new EventBus();
    
    // 注册订阅者
    MessageSubscriber subscriber = new MessageSubscriber();
    eventBus.register(subscriber);
    
    // 发布事件
    MessageEvent event = new MessageEvent("Hello, EventBus!");
    eventBus.post(event);
}
```