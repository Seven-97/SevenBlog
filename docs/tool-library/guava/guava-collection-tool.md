---
title: Guava - 集合工具
category: 开发工具
tag:
 - 工具类库
 - Guava
---



## Collections2

```java
private  Collections2() {
}
```

私有构造器，也没有静态构造器，所以可以很明确它是一个纯工具类了。



### filter过滤方法

传入一个带过滤的容器，和一个实现过滤规则的函数类，返回一个带有过滤动作的容器

```java
public static <E extends @Nullable Object> Collection<E> filter(
      Collection<E> unfiltered, Predicate<? super E> predicate) {
    if (unfiltered instanceof FilteredCollection) {
      // Support clear(), removeAll(), and retainAll() when filtering a filtered
      // collection.
      return ((FilteredCollection<E>) unfiltered).createCombined(predicate);
    }

    return new FilteredCollection<E>(checkNotNull(unfiltered), checkNotNull(predicate));
}
```

如果是Collections2.FilteredCollection类，则直接转型到Collections2.FilteredCollection，然后返回这个类。如果不是Collections2.FilteredCollection，则new一个，将传入的容器和规则传入。



```java
static class FilteredCollection<E extends @Nullable Object> extends AbstractCollection<E> {
    // 存储待处理的集合
    final Collection<E> unfiltered;
    //存储过滤规则
    final Predicate<? super E> predicate;

    FilteredCollection(Collection<E> unfiltered, Predicate<? super E> predicate) {
      this.unfiltered = unfiltered;
      this.predicate = predicate;
    }
   
    //根据新的过滤规则和原来的过滤规则合并创建一个新的容器
    FilteredCollection<E> createCombined(Predicate<? super E> newPredicate) {
      return new FilteredCollection<E>(unfiltered, Predicates.<E>and(predicate, newPredicate));
      // .<E> above needed to compile in JDK 5
    }

    //添加元素方法
    @Override
    public boolean add(@ParametricNullness E element) {
      //根据过滤规则进行测试是否符合，如果符合便进行添加
      checkArgument(predicate.apply(element));
      return unfiltered.add(element);
    }

    //添加一个容器中所有元素
    @Override
    public boolean addAll(Collection<? extends E> collection) {
      //遍历容器，并对每一个容器进行筛选
      for (E element : collection) {
        checkArgument(predicate.apply(element));
      }
      //如果都满足就添加
      return unfiltered.addAll(collection);
    }

    //...其他方法
}
```



### 转型方法

传入一个转型的类，再传入一个转型规则

```java
public static <F extends @Nullable Object, T extends @Nullable Object> Collection<T> transform(
      Collection<F> fromCollection, Function<? super F, T> function) {
    return new TransformedCollection<>(fromCollection, function);
}
```



```java
static class TransformedCollection<F extends @Nullable Object, T extends @Nullable Object>
      extends AbstractCollection<T> {
    //需要转型的容器
    final Collection<F> fromCollection;
    //转型规则
    final Function<? super F, ? extends T> function;

    TransformedCollection(Collection<F> fromCollection, Function<? super F, ? extends T> function) {
      this.fromCollection = checkNotNull(fromCollection);
      this.function = checkNotNull(function);
    }

   //根据转型规则进行迭代
    @Override
    public Iterator<T> iterator() {
      return Iterators.transform(fromCollection.iterator(), function);
    }

    //...其他方法
}
```



### 有序排列方法

```java
//第一种是调用了第二种
public static <E extends Comparable<? super E>> Collection<List<E>> orderedPermutations(Iterable<E> elements) {
    return orderedPermutations(elements, Ordering.natural());
}

//第二种直接创建了Collections2.OrderedPermutationCollection类
public static <E> Collection<List<E>> orderedPermutations(Iterable<E> elements, Comparator<? super E> comparator) {
    return new Collections2.OrderedPermutationCollection(elements, comparator);
}
```



```java
private static final class OrderedPermutationCollection<E> extends AbstractCollection<List<E>> {
    //不可变集合
    final ImmutableList<E> inputList;
    //比较器
    final Comparator<? super E> comparator;
    final int size;

    OrderedPermutationCollection(Iterable<E> input, Comparator<? super E> comparator) {
      this.inputList = ImmutableList.sortedCopyOf(comparator, input);
      this.comparator = comparator;
      this.size = calculateSize(inputList, comparator);
    }

    /**
     * The number of permutations with repeated elements is calculated as follows:
     *
     * <ul>
     *   <li>For an empty list, it is 1 (base case).对于空列表，它是1(基线条件)。
     *   <li>When r numbers are added to a list of n-r elements, the number of permutations is
     *       increased by a factor of (n choose r). 
     		当向n-r个元素的列表中添加r个数字时，排列的次数是(n choose r)的倍数。
     * </ul>
     */
    private static <E> int calculateSize(
        List<E> sortedInputList, Comparator<? super E> comparator) {
      int permutations = 1;//用于存储排列数，初始值为1
      int n = 1;//用于追踪当前元素的索引位置，初始值为1
      int r = 1;//用于追踪当前重复元素的次数，初始值为1
      while (n < sortedInputList.size()) {
         //使用比较器比较索引为n-1和n的元素
        int comparison = comparator.compare(sortedInputList.get(n - 1), sortedInputList.get(n));
        if (comparison < 0) {//如果索引n-1的元素小于索引n的元素
          // We move to the next non-repeated element.
            //更新排列数。
            //IntMath.binomial用于计算二项式系数的函数
            //IntMath.saturatedMultiply 用于计算两数相乘，但不会溢出int的最大最小值
          permutations = IntMath.saturatedMultiply(permutations, IntMath.binomial(n, r));
          r = 0;//重置r=0
          if (permutations == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
          }
        }
        n++;
        r++;
      }
      return IntMath.saturatedMultiply(permutations, IntMath.binomial(n, r));
    }

    @Override
    public Iterator<List<E>> iterator() {
      return new OrderedPermutationIterator<E>(inputList, comparator);
    }

    //..。其他方法
  }
```









