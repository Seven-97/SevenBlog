---
title: LinkedList深度剖析：源码解读与核心机制
category: Java
tags:
  - 集合
head:
  - - meta
    - name: keywords
      content: Java,LinkedList,LinkedList源码,java linkedlist,源码分析
  - - meta
    - name: description
      content: 全网最全的Java集合知识点总结，让天下没有难学的八股文！
---





 

## 介绍

**LinkedList**同时实现了**List**接口和**Deque**接口，也就是说它既可以看作一个顺序容器，又可以看作一个队列(**Queue**)，同时又可以看作一个栈(**Stack**)。这样看来，**LinkedList**简直就是个全能冠军。当你需要使用栈或者队列时，可以考虑使用**LinkedList**，一方面是因为Java官方已经声明不建议使用**Stack**类，更遗憾的是，Java里根本没有一个叫做**Queue**的类(它是个接口名字，无法直接创建)。关于栈或队列，现在的首选是**ArrayDeque**，它有着比**LinkedList**(当作栈或队列使用时)有着更好的性能。

对于频繁的插入或删除元素的操作，建议使用LinkedList类，效率较高；底层使用双向链表存储

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250842836.gif)

 

```java
//存储链表的第一个节点
transient Node<E> first;

//存储链表的最后一个节点
transient Node<E> last;
```



![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250842845.jpg)

**LinkedList**的实现方式决定了所有跟下标相关的操作都是线性时间，而在首段或者末尾删除元素只需要常数时间。为追求效率**LinkedList**没有实现同步(synchronized)，如果需要多个线程并发访问，可以先采用Collections.synchronizedList()方法对其进行包装。

## 底层实现

### 底层数据结构

**LinkedList**底层**通过双向链表实现**。双向链表的每个节点用内部类**Node**表示。**LinkedList**通过first和last引用分别指向链表的第一个和最后一个元素。注意这里没有所谓的哑元（也就是没有虚拟变量），当链表为空的时候first和last都指向null。

```java
transient int size = 0;

/**
 * Pointer to first node.
 * Invariant: (first == null && last == null) ||
 *            (first.prev == null && first.item != null)
 */
transient Node <E> first;

/**
 * Pointer to last node.
 * Invariant: (first == null && last == null) ||
 *            (last.next == null && last.item != null)
 */
transient Node <E> last;
```



其中Node是私有的内部类:

```java
private static class Node <E> {
    E item;
    Node <E> next;
    Node <E> prev;

    Node(Node <E> prev, E element, Node <E> next) {
        this.item = element;
        this.next = next;
        this.prev = prev;
    }
}
```



### 构造函数

```java
 /**
  * Constructs an empty list.
  */
 public LinkedList() {}

 /**
  * Constructs a list containing the elements of the specified
  * collection, in the order they are returned by the collection's
  * iterator.
  *
  * @param  c the collection whose elements are to be placed into this list
  * @throws NullPointerException if the specified collection is null
  */
 public LinkedList(Collection <? extends E> c) {
     this();
     addAll(c);
 }
```



### getFirst(), getLast()

获取第一个元素， 和获取最后一个元素:

```java
/**
 * Returns the first element in this list.
 *
 * @return the first element in this list
 * @throws NoSuchElementException if this list is empty
 */
public E getFirst() {
    final Node <E> f = first;
    if (f == null)
        throw new NoSuchElementException();
    return f.item;
}

/**
 * Returns the last element in this list.
 *
 * @return the last element in this list
 * @throws NoSuchElementException if this list is empty
 */
public E getLast() {
    final Node <E> l = last;
    if (l == null)
        throw new NoSuchElementException();
    return l.item;
}
```



### get方法

get方法是根据索引获取元素的

```java
public E get(int index) {
    checkElementIndex(index);
    return node(index).item;
}

//找到具体索引位置的  Node
Node <E> node(int index) {
    // assert isElementIndex(index);

    //类似二分法，size >> 1 即元素数量的一半
    if (index < (size >> 1)) { //索引小于一半，就从头开始找
        Node <E> x = first;
        for (int i = 0; i < index; i++)
            x = x.next;
        return x;
    } else { //否则就从尾部开始找
        Node <E> x = last;
        for (int i = size - 1; i > index; i--)
            x = x.prev;
        return x;
    }
}
```



### removeFirst(), removeLast(), remove(e), remove(index)

remove()方法也有两个版本

- 删除跟指定元素相等的第一个元素remove(Object o)

- 删除指定下标处的元素remove(int index)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250842847.jpg)



删除元素：指的是删除第一次出现的这个元素, 如果没有这个元素，则返回false；判断的依据是equals方法， 如果equals，则直接unlink这个node；
由于LinkedList可存放null元素，故也可以删除第一次出现null的元素；

 ```java
 public boolean remove(Object o) {
     if (o == null) {
         for (Node <E> x = first; x != null; x = x.next) {
             if (x.item == null) {
                 unlink(x);
                 return true;
             }
         }
     } else {
         for (Node <E> x = first; x != null; x = x.next) {
             if (o.equals(x.item)) {
                 unlink(x);
                 return true;
             }
         }
     }
     return false;
 }
 
 /**
  * Unlinks non-null node x.
  */
 E unlink(Node <E> x) {
     // assert x != null;
     final E element = x.item;
     final Node <E> next = x.next;
     final Node <E> prev = x.prev;
 
     if (prev == null) { // 第一个元素
         first = next;
     } else {
         prev.next = next;
         x.prev = null;
     }
 
     if (next == null) { // 最后一个元素
         last = prev;
     } else {
         next.prev = prev;
         x.next = null;
     }
 
     x.item = null; // GC
     size--;
     modCount++;
     return element;
 }
 ```



linklist是有序的，可重复存储数据的，并且可以存储null值，但remove方法在找到需要删除的元素时，就return了，因此只能删除找到的第一个元素

```java
LinkedList<String> list = new LinkedList();
list.add("1");
list.add(null);
list.add(null);
list.add("1");
System.out.println(list);
list.remove(null);
list.remove("1");
System.out.println(list);
//输出：
[1, null, null, 1]
[null, 1]
```



### add方法

**add()方法有两个版本：**

- **add(E e)，该方法在LinkedList**的末尾插入元素，因为有last指向链表末尾，在末尾插入元素的花费是常数时间。只需要简单修改几个相关引用即可；

- add(int index, E element)，该方法是在指定下表处插入元素，需要先通过线性查找找到具体位置，然后修改相关引用完成插入操作。

```java
public boolean add(E e) {
    linkLast(e);//默认add方法是在尾部插入元素
    return true;
}

void linkLast(E e) {
    final Node<E> l = last;//旧的尾部元素
    final Node<E> newNode = new Node<>(l, e, null);
    last = newNode;//尾部元素为 newNode
    if (l == null)//说明集合中没有元素
        first = newNode;
    else
        l.next = newNode;//旧的尾部元素  指向 newNode，即添加newNode
    size++;
    modCount++;
}

//同理。链表头部添加元素
 private void linkFirst(E e) {
    final Node<E> f = first;
    final Node<E> newNode = new Node<>(null, e, f);
    first = newNode;
    if (f == null)
        last = newNode;
    else
        f.prev = newNode;
    size++;
    modCount++;
}
```



![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250842849.jpg)

 

add(int index, E element)，当index==size时，等同于add(E e); 如果不是，则分两步: 

1. 先根据index找到要插入的位置,即node(index)方法；
2. 修改引用，完成插入操作。

```java
public void add(int index, E element) {
    checkPositionIndex(index);

    if (index == size)
        linkLast(element);
    else
        linkBefore(element, node(index));
}
```

上面代码中的node(int index)函数有一点小小的trick，因为链表双向的，可以从开始往后找，也可以从结尾往前找，具体朝那个方向找取决于条件index < (size >> 1)，也即是index是靠近前端还是后端。从这里也可以看出，linkedList通过index查询元素的效率没有arrayList高。

```java
Node <E> node(int index) {
    // assert isElementIndex(index);

    if (index < (size >> 1)) {
        Node <E> x = first;
        for (int i = 0; i < index; i++)
            x = x.next;
        return x;
    } else {
        Node <E> x = last;
        for (int i = size - 1; i > index; i--)
            x = x.prev;
        return x;
    }
}
```



### addAll()

addAll(index, c) 实现方式并不是直接调用add(index,e)来实现，主要是因为效率的问题，另一个是fail-fast中modCount只会增加1次；

 ```java
  public boolean addAll(Collection <? extends E> c) {
      return addAll(size, c);
  }
 
  public boolean addAll(int index, Collection <? extends E> c) {
      checkPositionIndex(index);
 
      Object[] a = c.toArray();
      int numNew = a.length;
      if (numNew == 0)
          return false;
 
      Node <E> pred, succ;
      if (index == size) {
          succ = null;
          pred = last;
      } else {
          succ = node(index);
          pred = succ.prev;
      }
 
      for (Object o: a) {
          @SuppressWarnings("unchecked") E e = (E) o;
          Node <E> newNode = new Node <> (pred, e, null);
          if (pred == null)
              first = newNode;
          else
              pred.next = newNode;
          pred = newNode;
      }
 
      if (succ == null) {
          last = pred;
      } else {
          pred.next = succ;
          succ.prev = pred;
      }
 
      size += numNew;
      modCount++;
      return true;
  }
 ```



### clear()

这里是将node之间的引用关系都变成null，这样GC可以在[年轻代](https://www.seven97.top/java/jvm/02-gc1-theory.html)回收元素了

```java
public void clear() {
    // Clearing all of the links between nodes is "unnecessary", but:
    // - helps a generational GC if the discarded nodes inhabit
    //   more than one generation
    // - is sure to free memory even if there is a reachable Iterator
    for (Node <E> x = first; x != null;) {
        Node <E> next = x.next;
        x.item = null;
        x.next = null;
        x.prev = null;
        x = next;
    }
    first = last = null;
    size = 0;
    modCount++;
}
```

注意：有些同学可能注意到这里的判定条件是x != null，但是 LinkedList 是可以设置null值的，那是不是当链表中有null的元素，并且爱clear遍历到元素为null时，条件 `x != null`就为false了，那就不会继续执行，也就是后续的元素都无法置为空了？
 实际上不是的，这里的`x != null`是没有这个元素，而LinkedList 中可以设置null值是指，x.item = null ，也就是有Node，但是Node的item属性为null



### Positional Access 方法

通过index获取元素

```java
 public E get(int index) {
     checkElementIndex(index);
     return node(index).item;
 }
```



将某个位置的元素重新赋值:

```java
public E set(int index, E element) {
    checkElementIndex(index);
    Node <E> x = node(index);
    E oldVal = x.item;
    x.item = element;
    return oldVal;
}
```



将元素插入到指定index位置:

```java
 public void add(int index, E element) {
     checkPositionIndex(index);

     if (index == size)
         linkLast(element);
     else
         linkBefore(element, node(index));
 }
```



删除指定位置的元素:

 ```java
  public E remove(int index) {
      checkElementIndex(index);
      return unlink(node(index));
  }
 ```



其它位置的方法:

```java
 private boolean isElementIndex(int index) {
     return index >= 0 && index < size;
 }


 private boolean isPositionIndex(int index) {
     return index >= 0 && index <= size;
 }


 private String outOfBoundsMsg(int index) {
     return "Index: " + index + ", Size: " + size;
 }

 private void checkElementIndex(int index) {
     if (!isElementIndex(index))
         throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
 }

 private void checkPositionIndex(int index) {
     if (!isPositionIndex(index))
         throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
 }
```



### 查找操作

查找操作的本质是查找元素的下标:

查找第一次出现的index, 如果找不到返回-1；

```java
public int indexOf(Object o) {
    int index = 0;
    if (o == null) {
        for (Node <E> x = first; x != null; x = x.next) {
            if (x.item == null)
                return index;
            index++;
        }
    } else {
        for (Node <E> x = first; x != null; x = x.next) {
            if (o.equals(x.item))
                return index;
            index++;
        }
    }
    return -1;
}
```



查找最后一次出现的index, 如果找不到返回-1；

```java
public int lastIndexOf(Object o) {
     int index = size;
     if (o == null) {
         for (Node <E> x = last; x != null; x = x.prev) {
             index--;
             if (x.item == null)
                 return index;
         }
     } else {
         for (Node <E> x = last; x != null; x = x.prev) {
             index--;
             if (o.equals(x.item))
                 return index;
         }
     }
     return -1;
 }
```



 

## LinkedList 存在的性能问题




在现代计算机架构下，**数组在绝大多数场景下都比链表快，包括那些理论上链表应该占优的场景**。

原因很简单：
1. **CPU 缓存命中率**：数组很高，链表很低
2. **预取机制**：CPU 能预测数组的下一个访问位置，但对链表不可以
3. 链表的「伪O(1)」插入删除
4. **内存访问模式**：数组线性访问，链表随机跳跃

  
### CPU缓存行

CPU 缓存不是按字节加载的，而是按照缓存行为单位，通常是 64 字节。

当你访问一个 int（4字节）时，CPU 会把它附近的 60 字节一起加载到缓存

对数组来说，这就是天赐良机：

```java
int arr[16];  // 64字节，正好一个缓存行
arr[0];       // 触发缓存加载
arr[1];       // 已经在缓存了
arr[2];       // 已经在缓存了
...
arr[15];      // 直到这里还在缓存
```

**一次内存访问，换来 16 次缓存命中。** 这就是数组的很快的原因。

但链表的每个节点可能分散在内存的各个角落，从而导致每次都是 Cache Miss...

你加载了一个缓存行，但只用了其中 8-16 字节（一个指针），剩下的 50 多字节完全浪费。**缓存利用率不到 20%。**

这就是为什么链表在实际中慢得要死——不是因为指针操作慢，而是因为**每次访问都在等内存**

  
计科里有两个重要的概念：

- **空间局部性**：访问了地址 X，很可能接下来访问 X+1, X+2...
- **时间局部性**：访问了地址 X，短时间内可能再次访问 X

数组就可以做到两者兼具：

- 遍历时，天然的空间局部性
- 多次访问同一个元素，时间局部性也很好

链表只有时间局部性（如果你反复访问同一个节点的话），空间局部性基本为零。

  
### CPU预取机制

当 CPU 检测到你在顺序访问内存时（比如遍历数组），它会主动把后面的数据加载到缓存，在你真正访问之前就准备好了。

这就是为什么遍历大数组时，几乎感觉不到内存延迟——CPU 已经提前帮你准备好了

但链表呢？CPU 看着你的访问模式：

```text
地址: 0x1000 -> 0x5f8a0 -> 0x2340 -> 0x9abc0 -> ...
```

完全随机，毫无规律。预取器直接放弃治疗。每次访问都是实打实的内存延迟。

关闭 CPU 预取功能后，数组遍历速度能慢 5-10 倍；但链表遍历几乎没变化——因为本来就没预取啊......

### 插入删除

- 链表的「伪O(1)」插入删除

书里说链表插入删除是 O(1)，但那是建立在「你已经找到了目标位置」的前提下！

但在现实中，你几乎总是需要先找到插入位置：

```java
// 链表插入：先遍历找到位置 O(n)，再插入 O(1)
Node* current = head;
while (current->value < target) {  // O(n)
    current = current->next;
}
insert_after(current, new_node);  // O(1)
```

**总复杂度还是 O(n)的** 而且是个缓存友好度极差的 O(n)。


- 数组的相对没那么慢的插入删除

数组插入需要移动元素，理论上是 O(n)：

```java
// 数组插入：找到位置 O(1)，移动元素 O(n)
int pos = find_position(arr, target);  // O(log n) if sorted
memmove(&arr[pos+1], &arr[pos], (size - pos) * sizeof(int));  // O(n)
arr[pos] = new_value;
```

但注意，**这个 O(n) 的移动操作是连续内存拷贝，完全在缓存中完成！**



### 内存分配

数组通常是一次分配一大块连续内存或者预分配后逐渐增长。而链表每次插入都需要 new 一个节点，每次删除都需要 delete。

new 和 delete 可不是免费的：

- 需要找到合适大小的空闲内存块
- 可能触发系统调用
- 需要维护内存管理的元数据
- 可能导致内存碎片

从Java的角度来看，LinkedList 需要创建一个 Node 对象，当执行 new 操作的时候，其过程是非常慢的，会经历两个过程：类加载过程 、对象创建过程
	- 类加载过程
		- 会先判断这个类是否已经初始化，如果没有初始化，会执行类的加载过程
		- 类的加载过程：加载、验证、准备、解析、初始化等等阶段，之后会执行 `<cnit>` 方法，初始化静态变量，执行静态代码块等等
	- 对象创建过程
	    - 如果类已经初始化了，直接执行对象的创建过程
	    - 对象的创建过程：在堆内存中开辟一块空间，给开辟空间分配一个地址，之后执行初始化，会执行  `<cnit>` 方法，初始化普通变量，调用普通代码块


高频的小对象分配（链表节点通常很小），new 的开销能占到总时间的 30-50%。而且，malloc/new 分配的内存往往是分散的，进一步恶化了缓存局部性。数组动态扩容虽然偶尔需要拷贝，但平摊下来，分配开销远小于链表。

  
## 链表真实使用场景

链表作为一种特殊的数据结构在某些特定场景下还是有价值的

- 真正的 O(1) 插入删除

如果你**已经持有某个节点的指针**，想在这个位置插入或删除，那链表确实是 O(1)，而且这个 O(1) 很实在。比如 LRU 缓存的实现，需要频繁地把节点移到链表头部，这种场景下链表就很合适。

- 不需要遍历的情况

像队列、栈这种只操作头尾的数据结构，链表还凑合。虽然数组实现的队列（环形队列）通常还是更快......

- 内存受限而且大小不确定

数组需要连续内存，如果内存碎片化严重，可能分配不出来。链表的离散存储在这种极端情况下可能是唯一选择。

但说实话，这种场景在现代系统中越来越少见了。

- 或者在某些特殊的算法

有些算法天生就是为链表设计的，比如一些图算法中的邻接表。强行用数组改写可能反而更复杂。

但我感觉**链表的适用场景其实是越来越窄了**。很多老项目里用链表的代码，现在重写成数组后，性能都能有质的提升。


<!-- @include: @article-footer.snippet.md -->     





