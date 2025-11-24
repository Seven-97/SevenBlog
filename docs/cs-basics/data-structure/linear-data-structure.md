---
title: 线性数据结构
category: 计算机基础
tag:
  - 数据结构
head:
  - - meta
    - name: keywords
      content: 数据结构,数组,链表,栈,队列
  - - meta
    - name: description
      content: 全网最全的计算机基础（数据结构）知识点总结，让天下没有难学的八股文！
---
线性表示最常⽤⽽且最为简单的⼀种数据结构，⼀个线性表示 n 个数据元素的有限序列，有以下特点：
- 存在唯⼀的第⼀个的数据元素
- 存在唯⼀被称为最后⼀个的数据元素
- 除了第⼀个以外，集合中每⼀个元素均有⼀个前驱
- 除了最后⼀个元素之外，集合中的每⼀个数据元素都有⼀个后继元素

线性表包括下⾯⼏种：
- 数组：查询 / 更新快，查找/删除慢
- 链表
- 队列
- 栈

## 数组

### 数组简介

**数组（Array）** 是一种很常见的数据结构。它由相同类型的元素（element）组成，并且是使用一块连续的内存来存储。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202502221308273.png)

在Java 中表示为：

```java
int[] nums = new int[100];
int[] nums = {1,2,3,4,5};
Object[] Objects = new Object[100];
```

数组是⼀种线性的结构，⼀般在底层是连续的空间，存储相同类型的数据，由于连续紧凑结构以及天然索引⽀持，查询数据效率⾼。

假设我们知道数组a 的第 1 个值是 地址是 296，⾥⾯的数据类型占 2 个 单位，那么我们如果期望得到第 5 个： 296+（5-1）*2 = 304 , O(1) 的时间复杂度就可以获取到。更新的本质也是查找，先查找到该元素，就可以动⼿更新了：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202502221310223.png)

但是如果期望插⼊数据的话，需要移动后⾯的数据，⽐如下⾯的数组，插⼊元素6 ，最差的是移动所有的元素，时间复杂度为O(n)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202502221310820.png)

⽽删除元素则需要把后⾯的数据移动到前⾯，最差的时间复杂度同样为O(n)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202502221310800.png)

总结：数组的特点是：**提供随机访问** 并且容量有限。

### 时间复杂度

假如数组的长度为 n。

- 访问：O（1）//访问特定位置的元素
- 插入：O（n ）//最坏的情况发生在插入发生在数组的首部并需要移动所有元素时
- 删除：O（n）//最坏的情况发生在删除数组的开头发生并需要移动第一元素后面所有的元素时

### 数组的增删改查实现

```java
import java.util.Arrays;
public class MyArray {
    private int[] data;
    private int elementCount;
    private int length;
    
    public MyArray(int max) {
        length = max;
        data = new int[max];
        elementCount = 0;
    }
    
    public void add(int value) {
        if (elementCount == length) {
            length = 2 * length;
            data = Arrays.copyOf(data, length);
        }
        data[elementCount] = value;
        elementCount++;
    }
    
    public int find(int searchKey) {
        int i;
        for (i = 0; i < elementCount; i++) {
            if (data[i] == searchKey)
            break;
        }
        if (i == elementCount) {
            return -1;
        }
        return i;
    }
    
    public boolean delete(int value) {
        int i = find(value);
        if (i == -1) {
        	return false;
        }
        for (int j = i; j < elementCount - 1; j++) {
        	data[j] = data[j + 1];
        }
        elementCount--;
        return true;
    }
    
    public boolean update(int oldValue, int newValue) {
        int i = find(oldValue);
        if (i == -1) {
        	return false;
        }
        data[i] = newValue;
        return true;
    }
}

// 测试类
public class Test {
    public static void main(String[] args) {
        MyArray myArray = new MyArray(2);
        myArray.add(1);
        myArray.add(2);
        myArray.add(3);
        myArray.delete(2);
        System.out.println(myArray);
    }
}
```







## 链表

### 链表简介

**链表（LinkedList）** 虽然是一种线性表，但是并不会按线性的顺序存储数据，使用的不是连续的内存空间来存储数据。

我们可以看到数组是需要连续的空间，这⾥⾯如果空间⼤⼩只有 2 ，放到第 3 个元素的时候，就不得不扩容，不仅如此，还得拷⻉元素。⼀些删除，插⼊操作会引起较多的数据移动的操作。

链表，也就是链式数据结构，由于它不要求逻辑上相邻的数据元素在物理位置上也相邻，所以它没有顺序存储结构所具有的缺点，但是同时也失去了通过索引下标直接查找元素的优点。

链表的插入和删除操作的复杂度为 O(1) ，只需要知道目标位置元素的上一个元素即可。但是，在查找一个节点或者访问特定位置的节点的时候复杂度为 O(n) 。

使用链表结构可以克服数组需要预先知道数据大小的缺点，链表结构可以充分利用计算机内存空间,实现灵活的内存动态管理。但链表不会节省空间，相比于数组会占用更多的空间，因为链表中每个节点存放的还有指向其他节点的指针。除此之外，链表不具有数组随机读取的优点。

### 时间复杂度

- 查询： O(n)，需要遍历链表
- 插⼊： O(1) ，修改前后指针即可
- 删除： O(1) ，同样是修改前后指针即可
- 修改：不需要查询则为O(1) ，需要查询则为O(n)

### 链表分类

**常见链表分类：**

1. 单链表：链表中的每⼀个结点，都有且只有⼀个指针指向下⼀个结点，并且最后⼀个节点指向空。
2. 双向链表：每个节点都有两个指针（为⽅便，我们称之为前指针，后指针），分别指向上⼀个节点和下⼀个节点，第⼀个节点的前指针指向NULL ，最后⼀个节点的后指针指向NULL
3. 循环链表：每⼀个节点的指针指向下⼀个节点，并且最后⼀个节点的指针指向第⼀个节点（虽然是循环链表，但是必要的时候还需要标识头结点或者尾节点，避免死循环）
4. 双向循环链表：同时满足双向链表和循环链表的能力



#### 单链表

**单链表** 单向链表只有一个方向，结点只有一个后继指针 next 指向后面的节点。因此，链表这种数据结构通常在物理内存上是不连续的。我们习惯性地把第一个结点叫作头结点，链表通常有一个不保存任何值的 head 节点(头结点)，通过头结点我们可以遍历整个链表。尾结点通常指向 null。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270907398.png)

单向链表的查找更新⽐较简单，我们看看插⼊新节点的具体过程（这⾥只展示中间位置的插⼊，头尾插⼊⽐较简单）：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202502221329234.png)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202502221330355.png)

那如何删除⼀个中间的节点呢？下⾯是具体的过程：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202502221330623.png)

或许你会好奇， a5 节点只是指针没有了，那它去哪⾥了？

如果是Java 程序，垃圾回收器会收集这种没有被引⽤的节点，帮我们回收掉了这部分内存，但是为了加快垃圾回收的速度，⼀般不需要的节点我们会置空，⽐如 node = null , 如果在C++ 程序中，那么就需要⼿动回收了，否则容易造成内存泄漏等问题。





#### 循环链表

**循环链表** 其实是一种特殊的单链表，和单链表不同的是循环链表的尾结点不是指向 null，而是指向链表的头结点。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270907602.png)

#### 双向链表

**双向链表** 包含两个指针，一个 prev 指向前一个节点，一个 next 指向后一个节点。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270907364.png)

#### 双向循环链表

**双向循环链表** 最后一个节点的 next 指向 head，而 head 的 prev 指向最后一个节点，构成一个环。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270907440.png)

### 应用场景

- 如果需要支持随机访问的话，链表没办法做到。
- 如果需要存储的数据元素的个数不确定，并且需要经常添加和删除数据的话，使用链表比较合适。
- 如果需要存储的数据元素的个数确定，并且不需要经常添加和删除数据的话，使用数组比较合适。

### 数组 vs 链表

- 数组支持随机访问，而链表不支持。
- 数组使用的是连续内存空间对 CPU 的缓存机制友好，链表则相反。
- 数组的大小固定，而链表则天然支持动态扩容。如果声明的数组过小，需要另外申请一个更大的内存空间存放数组元素，然后将原数组拷贝进去，这个操作是比较耗时的！

### 单向链表的增删改查实现

```java
public class ListNode {
    int val;
    ListNode next = null;
    ListNode(int val) {
    	this.val = val;
    }
}
```

```java
public class MyList<T> {
    private ListNode<T> head;
    private ListNode<T> tail;
    private int size;
    
    public MyList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }
    
    public void add(T element) {
    	add(size, element);
    }
    
    public void add(int index, T element) {
        if (index < 0 || index > size) {
        	throw new IndexOutOfBoundsException("超出链表⻓度范围");
        }
        ListNode current = new ListNode(element);
        if (index == 0) {
        	if (head == null) {
            	head = current;
        		tail = current;
        	} else {
                current.next = head;
                head = current;
            }
        } else if (index == size) {
            tail.next = current;
            tail = current;
        } else {
            ListNode preNode = get(index - 1);
            current.next = preNode.next;
            preNode.next = current;
        }
        size++;
    }
    
    public ListNode get(int index) {
        if (index < 0 || index >= size) {
        	throw new IndexOutOfBoundsException("超出链表⻓度");
        }
        ListNode temp = head;
        for (int i = 0; i < index; i++) {
        	temp = temp.next;
        }
        return temp;
    }
    
    public ListNode delete(int index) {
        if (index < 0 || index >= size) {
        	throw new IndexOutOfBoundsException("超出链表节点范围");
        }
        ListNode node = null;
        if (index == 0) {
            node = head;
            head = head.next;
        } else if (index == size - 1) {
            ListNode preNode = get(index - 1);
            node = tail;
            preNode.next = null;
            tail = preNode;
        } else {
            ListNode pre = get(index - 1);
            pre.next = pre.next.next;
            node = pre.next;
        }
        size--;
        return node;
    }
    
    public void update(int index, T element) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("超出链表节点范围");
        }
        ListNode node = get(index);
        node.val = element;
    }
    
    public void display() {
        ListNode temp = head;
        while (temp != null) {
            System.out.print(temp.val + " -> ");
            temp = temp.next;
        }
        System.out.println("");
    }
}
```



### 扩展：跳表

链表如果搜索，是很麻烦的，如果这个节点在最后，需要遍历所有的节点，才能找到，查找效率实在太低，有没有什么好的办法呢？

办法总⽐问题多，但是想要绝对的” 多快好省“是不存在的，有舍有得，计算机的世界⾥，充满哲学的味道。既然搜索效率有问题，那么我们不如给链表排个序。排序后的链表，还是只能知道头尾节点，知道中间的范围，但是要找到中间的节点，还是得⾛遍历的⽼路。如果我们把中间节点存储起来呢？存起来，确实我们就知道数据在前⼀半，还是在后⼀半。⽐如找7 ，肯定就从中间节点开始找。如果查找4 ,就得从头开始找，最差到中间节点，就停⽌查找。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202502221734573.png)

但是如此，还是没有彻底解决问题，因为链表很⻓的情况，只能通过前后两部分查找。不如回到原则：空间和时间，我们选择时间，那就要舍弃⼀部分空间,我们每个节点再加⼀个指针，现在有 2 层指针（注意：节点只有⼀份，都是同⼀个节点，只是为了好看，弄了两份，实际上是同⼀个节点，有两个指针，⽐如 1 ，既指向2，也指向5）：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202502221734640.png)

两层指针，问题依然存在，那就不断加层，⽐如每两个节点，就加⼀层：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202502221734571.png)

这就是跳表了，跳表的定义如下：

跳表(SkipList，全称跳跃表)是⽤于有序元素序列快速搜索查找的⼀个数据结构，跳表是⼀个随机化的数据结构，实质就是⼀种可以进⾏⼆分查找的有序链表。跳表在原有的有序链表上⾯增加了多级索引，通过索引来实现快速查找。跳表不仅能提⾼搜索性能，同时也可以提⾼插⼊和删除操作的性能。它在性能上和红⿊树，AVL树不相上下，但是跳表的原理⾮常简单，实现也⽐红⿊树简单很多。

主要的原理是⽤空间换时间，可以实现近乎⼆分查找的效率，实际上消耗的空间，假设每两个加⼀层，1 + 2 + 4 + ... + n = 2n-1 ,多出了差不多⼀倍的空间。你看它像不像书的⽬录，⼀级⽬录，⼆级，三级 ...

如果我们不断往跳表中插⼊数据，可能出现某⼀段节点会特别多的情况，这个时候就需要动态更新索引，除了插⼊数据，还要插⼊到上⼀层的链表中，保证查询效率。redis 中使⽤了[跳表来实现zset](https://www.seven97.top/database/redis/02-basement1-datastructure.html#跳表) , redis 中使⽤⼀个随机算法来计算层级，计算出每个节点到底多少层索引，虽然不能绝对保证⽐较平衡，但是基本保证了效率，实现起来⽐那些平衡树，红⿊树的算法简单⼀点。

在跳表中，每个节点包含以下几个组成部分：

- 值（value）：节点存储的数据
- 前进指针数组（forward array）：指向不同层级的后继节点
- 层高（level）：该节点的最大层级

跳表核心特性：
1. 多层结构：由最底层的原始有序链表，以及若干层索引组成
2. 概率平衡：通过随机函数决定节点的层数，无需复杂的平衡操作
3. 快速查找：能够跳过大量节点，实现对数级别的查找效率
4. 有序性：所有节点按关键字排序
5. 空间换时间：使用额外的索引指针提高查询速度

#### 基本操作

- 查找（Search）：查找过程从最高层开始，沿着当前层前进，直到遇到大于或等于目标值的节点：
	1. 如果找到目标值，返回该节点
	2. 如果当前节点的下一个值大于目标值，则降至下一层继续查找
	3. 如果到达最底层仍未找到，则目标值不存在

- 插入（Insert）
	1. 查找新值的插入位置，同时记录每一层的"插入点"
	2. 随机生成新节点的层高
	3. 从底层到该节点的最高层，逐层调整指针，将新节点插入到每层链表中

- 删除（Delete）
	1. 查找待删除节点的位置，同时记录每一层的前驱节点
	2. 如果找到该节点，从该节点的最高层到底层，逐层调整前驱节点的指针，绕过待删除节点

#### 基础实现

```java
import java.util.Random;

public class SkipList<T extends Comparable<T>> {
    private static final int MAX_LEVEL = 16; // 最大层数
    private static final double P = 0.5;     // 提升层级的概率
    private int level;                       // 当前跳表的最大层数
    private final Node<T> header;            // 头节点
    private final Random random;             // 用于随机层数的生成

    // 节点定义
    private static class Node<T extends Comparable<T>> {
        T value;                  // 节点值
        Node<T>[] forward;        // 前进指针数组

        @SuppressWarnings("unchecked")
        Node(T value, int level) {
            this.value = value;
            this.forward = new Node[level + 1];
        }
    }

    // 构造函数
    public SkipList() {
        this.level = 0;
        this.header = new Node<>(null, MAX_LEVEL);
        this.random = new Random();
    }

    // 随机生成层数
    private int randomLevel() {
        int lvl = 0;
        while (lvl < MAX_LEVEL && random.nextDouble() < P) {
            lvl++;
        }
        return lvl;
    }

    // 查找操作
    public Node<T> search(T value) {
        Node<T> current = header;
        
        // 从最高层开始查找
        for (int i = level; i >= 0; i--) {
            // 在当前层向前移动，直到找到大于等于目标值的节点
            while (current.forward[i] != null && 
                   current.forward[i].value.compareTo(value) < 0) {
                current = current.forward[i];
            }
        }
        
        // 现在我们在第0层，并且current是目标值的前一个节点
        current = current.forward[0];
        
        // 检查是否找到目标值
        if (current != null && current.value.compareTo(value) == 0) {
            return current;
        } else {
            return null;
        }
    }

    // 插入操作
    public void insert(T value) {
        @SuppressWarnings("unchecked")
        Node<T>[] update = new Node[MAX_LEVEL + 1];
        Node<T> current = header;

        // 查找插入位置并记录每层的前驱节点
        for (int i = level; i >= 0; i--) {
            while (current.forward[i] != null && 
                   current.forward[i].value.compareTo(value) < 0) {
                current = current.forward[i];
            }
            update[i] = current;
        }
        
        // 获取新节点的随机层数
        int newLevel = randomLevel();
        
        // 如果新层数比当前跳表的最大层数大，更新跳表层数
        if (newLevel > level) {
            for (int i = level + 1; i <= newLevel; i++) {
                update[i] = header;
            }
            level = newLevel;
        }
        
        // 创建新节点
        Node<T> newNode = new Node<>(value, newLevel);
        
        // 插入节点到各层链表中
        for (int i = 0; i <= newLevel; i++) {
            newNode.forward[i] = update[i].forward[i];
            update[i].forward[i] = newNode;
        }
    }

    // 删除操作
    public void delete(T value) {
        @SuppressWarnings("unchecked")
        Node<T>[] update = new Node[MAX_LEVEL + 1];
        Node<T> current = header;

        // 查找删除位置并记录每层的前驱节点
        for (int i = level; i >= 0; i--) {
            while (current.forward[i] != null && 
                   current.forward[i].value.compareTo(value) < 0) {
                current = current.forward[i];
            }
            update[i] = current;
        }
        
        current = current.forward[0];
        
        // 如果找到节点，进行删除
        if (current != null && current.value.compareTo(value) == 0) {
            for (int i = 0; i <= level; i++) {
                // 如果当前层的前驱节点指向要删除的节点，则修改指针
                if (update[i].forward[i] == current) {
                    update[i].forward[i] = current.forward[i];
                }
            }
            
            // 更新跳表的最大层数
            while (level > 0 && header.forward[level] == null) {
                level--;
            }
        }
    }

    // 打印跳表内容（用于调试）
    public void printSkipList() {
        System.out.println("Skip List Structure:");
        for (int i = level; i >= 0; i--) {
            System.out.print("Level " + i + ": ");
            Node<T> node = header.forward[i];
            while (node != null) {
                System.out.print(node.value + " ");
                node = node.forward[i];
            }
            System.out.println();
        }
    }
}

// 跳表节点
class SkipListNode {
  constructor(value, level) {
    this.value = value;
    this.forward = new Array(level + 1).fill(null);
  }
}

// 使用示例
SkipList skipList = new SkipList();
skipList.insert(3);
skipList.insert(6);
skipList.insert(7);
console.log(skipList.search(6)); // true
skipList.insert(9);
skipList.delete(6);
console.log(skipList.search(6)); // false
```

## 栈

### 栈简介

**栈 (Stack)** 只允许在有序的线性数据集合的一端（称为栈顶 top）进行加入数据（push）和移除数据（pop）。因而按照 **后进先出（LIFO, Last In First Out）** 的原理运作。**在栈中，push 和 pop 的操作都发生在栈顶。**就像是⼀个桶，只能不断的放在上⾯，取出来的时候，也只能不断的取出最上⾯的数据。要想取出底层的数据，只有等到上⾯的数据都取出来，才能做到。当然，如果有这种需求，我们⼀般会使⽤双向队列。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202502221735195.png)

栈常用一维数组或链表来实现，用数组实现的栈叫作 **顺序栈** ，用链表实现的栈叫作 **链式栈** 。[JDK 底层的栈，是⽤数组实现的](https://www.seven97.top/java/collection/02-collection4-queue-stack.html)，封装之后，通过API 操作的永远都只能是最后⼀个元素，栈经常⽤来实现递归的功能。

元素加⼊称之为⼊栈（压栈），取出元素，称之为出栈，栈顶元素则是最后⼀次放进去的元素。使⽤数组实现简单的栈(注意仅供参考测试，实际会有线程安全等问题)

### 时间复杂度

假设堆栈中有n个元素。

- 访问：O（n）//最坏情况
- 插入删除：O（1）//顶端插入和删除元素



### 栈的常见应用场景

当我们我们要处理的数据只涉及在一端插入和删除数据，并且满足 **后进先出（LIFO, Last In First Out）** 的特性时，我们就可以使用栈这个数据结构。

#### 实现浏览器的回退和前进功能

我们只需要使用两个栈(Stack1 和 Stack2)和就能实现这个功能。比如你按顺序查看了 1,2,3,4 这四个页面，我们依次把 1,2,3,4 这四个页面压入 Stack1 中。当你想回头看 2 这个页面的时候，你点击回退按钮，我们依次把 4,3 这两个页面从 Stack1 弹出，然后压入 Stack2 中。假如你又想回到页面 3，你点击前进按钮，我们将 3 页面从 Stack2 弹出，然后压入到 Stack1 中。



#### 检查符号是否成对出现

> 给定一个只包括 `'('`，`')'`，`'{'`，`'}'`，`'['`，`']'` 的字符串，判断该字符串是否有效。
>
> 有效字符串需满足：
>
> 1. 左括号必须用相同类型的右括号闭合。
> 2. 左括号必须以正确的顺序闭合。
>
> 比如 "()"、"()[]{}"、"{[]}" 都是有效字符串，而 "(]"、"([)]" 则不是。

这个问题实际是 Leetcode 的一道题目，我们可以利用栈 `Stack` 来解决这个问题。

1. 首先我们将括号间的对应规则存放在 `Map` 中，这一点应该毋容置疑；
2. 创建一个栈。遍历字符串，如果字符是左括号就直接加入`stack`中，否则将`stack` 的栈顶元素与这个括号做比较，如果不相等就直接返回 false。遍历结束，如果`stack`为空，返回 `true`。

```java
public boolean isValid(String s){
    // 括号之间的对应规则
    HashMap<Character, Character> mappings = new HashMap<Character, Character>();
    mappings.put(')', '(');
    mappings.put('}', '{');
    mappings.put(']', '[');
    Stack<Character> stack = new Stack<Character>();
    char[] chars = s.toCharArray();
    for (int i = 0; i < chars.length; i++) {
        if (mappings.containsKey(chars[i])) {
            char topElement = stack.empty() ? '#' : stack.pop();
            if (topElement != mappings.get(chars[i])) {
                return false;
            }
        } else {
            stack.push(chars[i]);
        }
    }
    return stack.isEmpty();
}
```

#### 反转字符串

将字符串中的每个字符先入栈再出栈就可以了。

#### 维护函数调用

最后一个被调用的函数必须先完成执行，符合栈的 **后进先出（LIFO, Last In First Out）** 特性。  
例如递归函数调用可以通过栈来实现，每次递归调用都会将参数和返回地址压栈。

#### 深度优先遍历（DFS）

在深度优先搜索过程中，栈被用来保存搜索路径，以便回溯到上一层。

### 栈的实现

栈既可以通过数组实现，也可以通过链表来实现。不管基于数组还是链表，入栈、出栈的时间复杂度都为 O(1)。

下面我们使用数组来实现一个栈，并且这个栈具有`push()`、`pop()`（返回栈顶元素并出栈）、`peek()` （返回栈顶元素不出栈）、`isEmpty()`、`size()`这些基本的方法。

> 提示：每次入栈之前先判断栈的容量是否够用，如果不够用就用`Arrays.copyOf()`进行扩容；

```java
public class MyStack {
    private int[] storage;//存放栈中元素的数组
    private int capacity;//栈的容量
    private int count;//栈中元素数量
    private static final int GROW_FACTOR = 2;

    //不带初始容量的构造方法。默认容量为8
    public MyStack() {
        this.capacity = 8;
        this.storage=new int[8];
        this.count = 0;
    }

    //带初始容量的构造方法
    public MyStack(int initialCapacity) {
        if (initialCapacity < 1)
            throw new IllegalArgumentException("Capacity too small.");

        this.capacity = initialCapacity;
        this.storage = new int[initialCapacity];
        this.count = 0;
    }

    //入栈
    public void push(int value) {
        if (count == capacity) {
            ensureCapacity();
        }
        storage[count++] = value;
    }

    //确保容量大小
    private void ensureCapacity() {
        int newCapacity = capacity * GROW_FACTOR;
        storage = Arrays.copyOf(storage, newCapacity);
        capacity = newCapacity;
    }

    //返回栈顶元素并出栈
    private int pop() {
        if (count == 0)
            throw new IllegalArgumentException("Stack is empty.");
        count--;
        return storage[count];
    }

    //返回栈顶元素不出栈
    private int peek() {
        if (count == 0){
            throw new IllegalArgumentException("Stack is empty.");
        }else {
            return storage[count-1];
        }
    }

    //判断栈是否为空
    private boolean isEmpty() {
        return count == 0;
    }

    //返回栈中元素的个数
    private int size() {
        return count;
    }

}
```

验证

```java
MyStack myStack = new MyStack(3);
myStack.push(1);
myStack.push(2);
myStack.push(3);
myStack.push(4);
myStack.push(5);
myStack.push(6);
myStack.push(7);
myStack.push(8);
System.out.println(myStack.peek());//8
System.out.println(myStack.size());//8
for (int i = 0; i < 8; i++) {
    System.out.println(myStack.pop());
}
System.out.println(myStack.isEmpty());//true
myStack.pop();//报错：java.lang.IllegalArgumentException: Stack is empty.
```

## 队列

### 队列简介

**队列（Queue）** 是 **先进先出 (FIFO，First In, First Out)** 的线性表。在具体应用中通常用链表或者数组来实现，用数组实现的队列叫作 **顺序队列** ，用链表实现的队列叫作 **链式队列** 。**队列只允许在后端（rear）进行插入操作也就是入队 enqueue，在前端（front）进行删除操作也就是出队 dequeue**

队列的操作方式和堆栈类似，唯一的区别在于队列只允许新数据在后端进行添加。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202502221738698.png)

⼀般只要说到先进先出（ FIFO ）,全称First In First Out ,就会想到队列，但是如果你想拥有队列即可以从队头取出元素，⼜可以从队尾取出元素，那就需要⽤到特殊的队列（双向队列），双向队列⼀般使⽤双向链表实现会简单⼀点。

### 时间复杂度

假设队列中有n个元素。

- 访问：O（n）//最坏情况
- 插入删除：O（1）//后端插入前端删除元素



### 队列分类

#### 单队列

单队列就是常见的队列, 每次添加元素时，都是添加到队尾。单队列又分为 **顺序队列（数组实现）** 和 **链式队列（链表实现）**。

**顺序队列存在“假溢出”的问题也就是明明有位置却不能添加的情况。**

假设下图是一个顺序队列，我们将前两个元素 1,2 出队，并入队两个元素 7,8。当进行入队、出队操作的时候，front 和 rear 都会持续往后移动，当 rear 移动到最后的时候,我们无法再往队列中添加数据，即使数组中还有空余空间，这种现象就是 **”假溢出“** 。除了假溢出问题之外，如下图所示，当添加元素 8 的时候，rear 指针移动到数组之外（越界）。

> 为了避免当只有一个元素的时候，队头和队尾重合使处理变得麻烦，所以引入两个指针，front 指针指向对头元素，rear 指针指向队列最后一个元素的下一个位置，这样当 front 等于 rear 时，此队列不是还剩一个元素，而是空队列。——From 《大话数据结构》


#### 循环队列

循环队列可以解决顺序队列的假溢出和越界问题。解决办法就是：从头开始，这样也就会形成头尾相接的循环，这也就是循环队列名字的由来。


顺序队列中，我们说 `front==rear` 的时候队列为空，循环队列中则不一样，也可能为满，如上图所示。解决办法有两种：

1. 可以设置一个标志变量 `flag`,当 `front==rear` 并且 `flag=0` 的时候队列为空，当`front==rear` 并且 `flag=1` 的时候队列为满。
2. 队列为空的时候就是 `front==rear` ，队列满的时候，我们保证数组还有一个空闲的位置，rear 就指向这个空闲位置，如下图所示，那么现在判断队列是否为满的条件就是：`(rear+1) % QueueSize==front` 。

#### 双端队列

**双端队列 (Deque)** 是一种在队列的两端都可以进行插入和删除操作的队列，相比单队列来说更加灵活。

一般来说，我们可以对双端队列进行 `addFirst`、`addLast`、`removeFirst` 和 `removeLast` 操作。

#### 优先队列

**优先队列 (Priority Queue)** 从底层结构上来讲并非线性的数据结构，它一般是由堆来实现的。

1. 在每个元素入队时，优先队列会将新元素其插入堆中并调整堆。
2. 在队头出队时，优先队列会返回堆顶元素并调整堆。

关于堆的具体实现可以看[堆](https://www.seven97.top/cs-basics/data-structure/heap.html)这一节。

总而言之，不论我们进行什么操作，优先队列都能按照**某种排序方式**进行一系列堆的相关操作，从而保证整个集合的**有序性**。

虽然优先队列的底层并非严格的线性结构，但是在我们使用的过程中，我们是感知不到**堆**的，从使用者的眼中优先队列可以被认为是一种线性的数据结构：一种会自动排序的线性队列。

### 队列的常见应用场景

当我们需要按照一定顺序来处理数据的时候可以考虑使用队列这个数据结构。

- **阻塞队列：** 阻塞队列可以看成在队列基础上加了阻塞操作的队列。当队列为空的时候，出队操作阻塞，当队列满的时候，入队操作阻塞。使用阻塞队列我们可以很容易实现“生产者 - 消费者“模型。
- **线程池中的请求/任务队列：** 线程池中没有空闲线程时，新的任务请求线程资源时，线程池该如何处理呢？答案是将这些请求放在队列中，当有空闲线程的时候，会循环中反复从队列中获取任务来执行。队列分为无界队列(基于链表)和有界队列(基于数组)。无界队列的特点就是可以一直入列，除非系统资源耗尽，比如：`FixedThreadPool` 使用无界队列 `LinkedBlockingQueue`。但是有界队列就不一样了，当队列满的话后面再有任务/请求就会拒绝，在 Java 中的体现就是会抛出`java.util.concurrent.RejectedExecutionException` 异常。
- 栈：双端队列天生便可以实现栈的全部功能（`push`、`pop` 和 `peek`），并且在 Deque 接口中已经实现了相关方法。Stack 类已经和 Vector 一样被遗弃，现在在 Java 中普遍使用双端队列（Deque）来实现栈。
- 广度优先搜索（BFS），在图的广度优先搜索过程中，队列被用于存储待访问的节点，保证按照层次顺序遍历图的节点。
- Linux 内核进程队列（按优先级排队）
- 现实生活中的派对，播放器上的播放列表;
- 消息队列
- 等等……

### 单向队列的实现

```java
class Node<T> {
    public T data;
    public Node next;
    
    public Node(T data) {
    	this.data = data;
    }
}

public class MyQueue<T> {
    private Node<T> head;
    private Node<T> rear;
    private int size;
    
    public MyQueue() {
    	size = 0;
    }
    
    public void pushBack(T element) {
        Node newNode = new Node(element);
        if (isEmpty()) {
        	head = newNode;
        } else {
        	rear.next = newNode;
        }
        rear = newNode;
        size++;
    }
    
    public boolean isEmpty() {
    	return head == null;
    }
    
    public T popFront() {
        if (isEmpty()) {
        	throw new NullPointerException("队列没有数据");
        } else {
            Node<T> node = head;
            head = head.next;
            size--;
            return node.data;
    	}
	}
    
    public void dispaly() {
        Node temp = head;
        while (temp != null) {
            System.out.print(temp.data +" -> ");
            temp = temp.next;
        }
        System.out.println("");
    }
}
```





## 用栈实现队列以及队列实现栈

### 栈实现队列

- 栈的特性是先进后出
- 队列的特性是先进先出

有两个栈 stack1 , stack2 ；

- 如果有新的数据进⼊，那么我们可以直接 push 到 stack1 ；
- 如果需要取出数据，那么我们优先取出 stack2 的数据，如果 stack2 ⾥⾯数据是空的，那么我们需要把所有的 stack1 的数据倒⼊ stack2 。再从 stack2 取数据。

例如：

1. push 1 --> push 2

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202503161709874.png)

2. pop 1

![image-20250316170844217](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202503161709866.png)

3. push 3 --> push 4

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202503161709847.png)

4. pop 2

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202503161709761.png)


算法实现如下：

```java
class MyQueue {

    Stack<Integer> stackIn;
    Stack<Integer> stackOut;

    /** Initialize your data structure here. */
    public MyQueue() {
        stackIn = new Stack<>(); // 负责进栈
        stackOut = new Stack<>(); // 负责出栈
    }
    
    /** Push element x to the back of queue. */
    public void push(int x) {
        stackIn.push(x);
    }
    
    /** Removes the element from in front of queue and returns that element. */
    public int pop() {    
        dumpstackIn();
        return stackOut.pop();
    }
    
    /** Get the front element. */
    public int peek() {
        dumpstackIn();
        return stackOut.peek();
    }
    
    /** Returns whether the queue is empty. */
    public boolean empty() {
        return stackIn.isEmpty() && stackOut.isEmpty();
    }

    // 如果stackOut为空，那么将stackIn中的元素全部放到stackOut中
    private void dumpstackIn(){
        if (!stackOut.isEmpty()) return; 
        while (!stackIn.isEmpty()){
                stackOut.push(stackIn.pop());
        }
    }
}
```

- 时间复杂度: push和empty为O(1), pop和peek为O(n)
- 空间复杂度: O(n)



### 队列实现栈

**队列是先进先出的规则，把一个队列中的数据导入另一个队列中，数据的顺序并没有变，并没有变成先进后出的顺序。**

所以用栈实现队列， 和用队列实现栈的思路还是不一样的，这取决于这两个数据结构的性质。

但是依然还是要用两个队列来模拟栈，只不过没有输入和输出的关系，而是另一个队列完全用来备份的！

```java
class MyStack {

    Queue<Integer> queue1; // 和栈中保持一样元素的队列
    Queue<Integer> queue2; // 辅助队列

    /** Initialize your data structure here. */
    public MyStack() {
        queue1 = new LinkedList<>();
        queue2 = new LinkedList<>();
    }
    
    /** Push element x onto stack. */
    public void push(int x) {
        queue2.offer(x); // 先放在辅助队列中
        while (!queue1.isEmpty()){
            queue2.offer(queue1.poll());
        }
        Queue<Integer> queueTemp;
        queueTemp = queue1;
        queue1 = queue2;
        queue2 = queueTemp; // 最后交换queue1和queue2，将元素都放到queue1中
    }
    
    /** Removes the element on top of the stack and returns that element. */
    public int pop() {
        return queue1.poll(); // 因为queue1中的元素和栈中的保持一致，所以这个和下面两个的操作只看queue1即可
    }
    
    /** Get the top element. */
    public int top() {
        return queue1.peek();
    }
    
    /** Returns whether the stack is empty. */
    public boolean empty() {
        return queue1.isEmpty();
    }
}
```

- 时间复杂度: pop为O(n)，其他为O(1)
- 空间复杂度: O(n)



优化：

其实这道题目就是用一个队列就够了。

**一个队列在模拟栈弹出元素的时候只要将队列头部的元素（除了最后一个元素外） 重新添加到队列尾部，此时再去弹出元素就是栈的顺序了。**

```java
class MyStack {
    Queue<Integer> queue;
    
    public MyStack() {
        queue = new LinkedList<>();
    }
    
    public void push(int x) {
        queue.add(x);
    }
    
    public int pop() {
        rePosition();
        return queue.poll();
    }
    
    public int top() {
        rePosition();
        int result = queue.poll();
        queue.add(result);
        return result;
    }
    
    public boolean empty() {
        return queue.isEmpty();
    }

    public void rePosition(){
        int size = queue.size();
        size--;
        while(size-->0)
            queue.add(queue.poll());
    }
}
```

- 时间复杂度: pop为O(n)，其他为O(1)
- 空间复杂度: O(n)



<!-- @include: @article-footer.snippet.md -->     