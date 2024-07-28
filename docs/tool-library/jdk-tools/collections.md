---
title: 集合工具类 - Collections
shortTitle: Collections工具类
category: 工具类库
tag:
  - Java
  - 集合
---



Collections 是 JDK 提供的一个工具类，位于 java.util 包下，提供了一系列的静态方法，方便我们对集合进行各种操作，算是集合框架的一个大管家。

Collections 的用法很简单，在 Intellij IDEA 中敲完 `Collections.` 之后就可以看到它提供的方法了，大致看一下方法名和参数就能知道这个方法是干嘛的。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407282314408.png)

为了节省大家的学习时间，我将这些方法做了一些分类，并列举了一些简单的例子。

## 排序操作

- `reverse(List list)`：反转顺序
- `shuffle(List list)`：洗牌，将顺序打乱
- `sort(List list)`：自然升序
- `sort(List list, Comparator c)`：按照自定义的比较器排序
- `swap(List list, int i, int j)`：将 i 和 j 位置的元素交换位置

来看例子：

```java
List<String> list = new ArrayList<>();
list.add("沉默王二");
list.add("沉默王三");
list.add("沉默王四");
list.add("沉默王五");
list.add("沉默王六");

System.out.println("原始顺序：" + list);

// 反转
Collections.reverse(list);
System.out.println("反转后：" + list);

// 洗牌
Collections.shuffle(list);
System.out.println("洗牌后：" + list);

// 自然升序
Collections.sort(list);
System.out.println("自然升序后：" + list);

// 交换
Collections.swap(list, 2,4);
System.out.println("交换后：" + list);
```

输出后：

```
原始顺序：[沉默王二, 沉默王三, 沉默王四, 沉默王五, 沉默王六]
反转后：[沉默王六, 沉默王五, 沉默王四, 沉默王三, 沉默王二]
洗牌后：[沉默王五, 沉默王二, 沉默王六, 沉默王三, 沉默王四]
自然升序后：[沉默王三, 沉默王二, 沉默王五, 沉默王六, 沉默王四]
交换后：[沉默王三, 沉默王二, 沉默王四, 沉默王六, 沉默王五]
```

## 查找操作

- `binarySearch(List list, Object key)`：二分查找法，前提是 List 已经排序过了
- `max(Collection coll)`：返回最大元素
- `max(Collection coll, Comparator comp)`：根据自定义比较器，返回最大元素
- `min(Collection coll)`：返回最小元素
- `min(Collection coll, Comparator comp)`：根据自定义比较器，返回最小元素
- `fill(List list, Object obj)`：使用指定对象填充
- `frequency(Collection c, Object o)`：返回指定对象出现的次数

来看例子：

```java
System.out.println("最大元素：" + Collections.max(list));
System.out.println("最小元素：" + Collections.min(list));
System.out.println("出现的次数：" + Collections.frequency(list, "沉默王二"));

// 没有排序直接调用二分查找，结果是不确定的
System.out.println("排序前的二分查找结果：" + Collections.binarySearch(list, "沉默王二"));
Collections.sort(list);
// 排序后，查找结果和预期一致
System.out.println("排序后的二分查找结果：" + Collections.binarySearch(list, "沉默王二"));

Collections.fill(list, "沉默王八");
System.out.println("填充后的结果：" + list);
```

输出后：

```
原始顺序：[沉默王二, 沉默王三, 沉默王四, 沉默王五, 沉默王六]
最大元素：沉默王四
最小元素：沉默王三
出现的次数：1
排序前的二分查找结果：0
排序后的二分查找结果：1
填充后的结果：[沉默王八, 沉默王八, 沉默王八, 沉默王八, 沉默王八]
```



## 同步控制

ArrayList 是线程不安全的，没法在多线程环境下使用，那 Collections 工具类中提供了多个 synchronizedXxx 方法，这些方法会返回一个同步的对象，从而解决多线程中访问集合时的安全问题。

![](https://cdn.tobebetterjavaer.com/tobebetterjavaer/images/common-tool/collections-02.png)

使用起来也非常的简单：

```java
SynchronizedList synchronizedList = Collections.synchronizedList(list);
```

看一眼 SynchronizedList 的源码就明白了，不过是在方法里面使用 synchronized 关键字 加了一层锁而已。

```java
static class SynchronizedList<E>
    extends SynchronizedCollection<E>
    implements List<E> {
    private static final long serialVersionUID = -7754090372962971524L;

    final List<E> list;

    SynchronizedList(List<E> list) {
        super(list); // 调用父类 SynchronizedCollection 的构造方法，传入 list
        this.list = list; // 初始化成员变量 list
    }

    // 获取指定索引处的元素
    public E get(int index) {
        synchronized (mutex) {return list.get(index);} // 加锁，调用 list 的 get 方法获取元素
    }
    
    // 在指定索引处插入指定元素
    public void add(int index, E element) {
        synchronized (mutex) {list.add(index, element);} // 加锁，调用 list 的 add 方法插入元素
    }
    
    // 移除指定索引处的元素
    public E remove(int index) {
        synchronized (mutex) {return list.remove(index);} // 加锁，调用 list 的 remove 方法移除元素
    }
}
```

那这样的话，其实效率和那些直接在方法上加 synchronized 关键字的 Vector、Hashtable 差不多（JDK 1.0 时期就有了），而这些集合类基本上已经废弃了，几乎不怎么用。正确的做法是使用并发包下的 CopyOnWriteArrayList、ConcurrentHashMap。



## 不可变集合

- `emptyXxx()`：制造一个空的不可变集合
- `singletonXxx()`：制造一个只有一个元素的不可变集合
- `unmodifiableXxx()`：为指定集合制作一个不可变集合

举个例子：

```java
List emptyList = Collections.emptyList();
emptyList.add("非空");
System.out.println(emptyList);
```

这段代码在执行的时候就抛出错误了。

```
Exception in thread "main" java.lang.UnsupportedOperationException
	at java.util.AbstractList.add(AbstractList.java:148)
	at java.util.AbstractList.add(AbstractList.java:108)
	at com.itwanger.s64.Demo.main(Demo.java:61)
```

这是因为 `Collections.emptyList()` 会返回一个 Collections 的内部类 EmptyList，而 EmptyList 并没有重写父类 AbstractList 的 `add(int index, E element)` 方法，所以执行的时候就抛出了不支持该操作的 UnsupportedOperationException 了。

这是从分析 add 方法源码得出的原因。除此之外，emptyList 方法是 final 的，返回的 EMPTY_LIST 也是 final 的，种种迹象表明 emptyList 返回的就是不可变对象，没法进行增删改查。

```java
public static final <T> List<T> emptyList() {
    return (List<T>) EMPTY_LIST;
}

public static final List EMPTY_LIST = new EmptyList<>();
```



## 其他

还有两个方法比较常用：

- `addAll(Collection<? super T> c, T... elements)`，往集合中添加元素
- `disjoint(Collection<?> c1, Collection<?> c2)`，判断两个集合是否没有交集

举个例子：

```java
List<String> allList = new ArrayList<>();
Collections.addAll(allList, "沉默王九","沉默王十","沉默王二");
System.out.println("addAll 后：" + allList);

System.out.println("是否没有交集：" + (Collections.disjoint(list, allList) ? "是" : "否"));
```

输出后：

```
原始顺序：[沉默王二, 沉默王三, 沉默王四, 沉默王五, 沉默王六]
addAll 后：[沉默王九, 沉默王十, 沉默王二]
是否没有交集：否
```





