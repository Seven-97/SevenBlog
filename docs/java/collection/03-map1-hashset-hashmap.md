---
title: Map - HashSet & HashMap详解
category: Java
tag:
 - 集合
---





**HashSet**和**HashMap**者在Java里有着相同的实现，前者仅仅是对后者做了一层包装，也就是说**HashSet**里面有一个**HashMap**(适配器模式)。因此了解**HashMap源码也就了解HashSet了**

## 介绍

- Key的存储方式是基于哈希表的

- HashMap是 Map 接口 使用频率最高的实现类。

- 允许使用null键和null值，与HashSet一样，不保证映射的顺序。

- 所有的key构成的集合是无序的、唯一不可重复的。所以，key所在的类要重写：equals()和hashCode()

- 所有的value构成的集合是Collection:无序的、可以重复的。所以，value所在的类要重写：equals()

- 一个key-value构成一个entry

- 所有的entry构成的集合是Set:无序的、不可重复的

- HashMap 判断两个 key 相等的标准是：两个 key 通过 equals() 方法返回 true，hashCode 值也相等。

- HashMap 判断两个 value 相等的标准是：两个 value 通过 equals() 方法返回 true

## 底层原理介绍

### 底层数据结构和初始属性

```java
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; //  HashMap的默认初始容量，16
static final int MAXIMUM_CAPACITY = 1 << 30;//HashMap的最大支持容量，2^30
static final float DEFAULT_LOAD_FACTOR = 0.75f;//HashMap的默认加载因子
static final int TREEIFY_THRESHOLD = 8;//Bucket中链表长度大于该默认值，转化为红黑树
static final int UNTREEIFY_THRESHOLD = 6;//Bucket中红黑树存储的Node小于该默认值，转化为链表
/**
* 桶中的Node被树化时最小的hash表容量。
*（当桶中Node的数量大到需要变红黑树时，
* 若hash表容量小于MIN_TREEIFY_CAPACITY时，
* 此时应执行resize扩容操作这个MIN_TREEIFY_CAPACITY的值至少是TREEIFY_THRESHOLD的4倍。）
*/
static final int MIN_TREEIFY_CAPACITY = 64;

//存储元素的数组，总是2的n次幂
//通过数组存储，数组的元素是具体的Node<K,V>，这个Node有可能组成红黑树，可能是链表
transient Node<K,V>[] table;
//存储具体元素的集
 transient Set<Map.Entry<K,V>> entrySet;
//HashMap中存储的键值对的数量
transient int size;
//扩容的临界值，=容量*加载因子
int threshold;

//The load factor for the hash table.
final float loadFactor;
```



为什么默认负载因子是 0.75？官方答案如下：

> As a general rule, the default load factor (.75) offers a good tradeoff between time and space costs. Higher values decrease the space overhead but increase the lookup cost (reflected in most of the operations of the HashMap class, including get and put). The expected number of entries in the map and its load factor should be taken into account when setting its initial capacity, so as to minimize the number of rehash operations. If the initial capacity is greater than the maximum number of entries divided by the load factor, no rehash operations will ever occur.

上面的意思，简单来说是默认负载因子为 0.75，是因为它提供了空间和时间复杂度之间的良好平衡。 负载因子太低会导致大量的空桶浪费空间，负载因子太高会导致大量的碰撞，降低性能。0.75 的负载因子在这两个因素之间取得了良好的平衡。也就是说官方并未对负载因子为 0.75 做过的的解释，只是大概的说了一下，0.75 是空间和时间复杂度的平衡，但更多的细节是未做说明的，Stack Overflow 进行了[负载因子的科学推测](https://stackoverflow.com/questions/10901752/what-is-the-significance-of-load-factor-in-hashmap)，感兴趣的可以学习学习

### 构造方法

```java
//空参构造，初始化加载因子
public HashMap() {
    this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
}

//有参构造，可以初始化初始容量大小和加载因子
public HashMap(int initialCapacity, float loadFactor) {
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal initial capacity: " +
                                          initialCapacity);
    if (initialCapacity > MAXIMUM_CAPACITY)
        initialCapacity = MAXIMUM_CAPACITY;
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
        throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
    this.loadFactor = loadFactor;
    this.threshold = tableSizeFor(initialCapacity);//扩容的临界值，= 容量*加载因子
}
```



### put方法

```java
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}
```



先计算key的hash值，再对其put

```java
static final int hash(Object key) {
    int h;
    //为什么要右移16位？ 默认长度为2^5=16，与hash值&操作，容易获得相同的值。
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

这里分为了三步：

1. h=key.hashCode() //第一步 取hashCode值
2. h^(h>>>16) //第二步 高位参与运算，减少冲突，hash计算到这里
3. return h&(length-1); //第三步 取模运算，计算数据在桶中的位置，这里看后面的源码

第3步(n-1)&hash原理：

- 实际上(n-1) & hash等于 hash%n都可以得到元素在桶中的位置，但是(n-1)&hash操作更快。

- 取余操作如果除数是 2 的整数次幂可以优化为移位操作。这也是为什么扩容时必须是必须是2的n次方

> 位运算(&)效率要比代替取模运算(%)高很多，主要原因是位运算直接对内存数据进行操作，不需要转成十进制，因此处理速度非常快。

而计算hash是通过同时使用hashCode()的高16位异和低16位实现的(h >>> 16)：这么做可以在数组比较小的时候，也能保证考虑到高低位都参与到Hash的计算中，可以减少冲突，同时不会有太大的开销。

> hash值其实是一个int类型，二进制位为32位，而HashMap的table数组初始化size为16，取余操作为hashCode & 15 ==> hashCode & 1111 。这将会存在一个巨大的问题，1111只会与hashCode的低四位进行与操作，也就是hashCode的高位其实并没有参与运算，会导很多hash值不同而高位有区别的数，最后算出来的索引都是一样的。 举个例子，假设hashCode为1111110001，那么1111110001 & 1111 = 0001，如果有些key的hash值低位与前者相同，但高位发生了变化，如1011110001 & 1111 = 0001，1001110001 & 1111 = 0001，显然在高位发生变化后，最后算出来的索引还是一样，这样就会导致很多数据都被放到一个数组里面了，造成性能退化。 为了避免这种情况，HashMap将高16位与低16位进行异或，这样可以保证高位的数据也参与到与运算中来，以增大索引的散列程度，让数据分布得更为均匀（个人认为是为了分布均匀）

 

put流程如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250901133.jpg)

 ```java
 // 第四个参数 onlyIfAbsent 如果是 true，那么只有在不存在该 key 时才会进行 put 操作
 // 第五个参数 evict 我们这里不关心
 final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                boolean evict) {
     Node<K,V>[] tab; Node<K,V> p; int n, i;
     if ((tab = table) == null || (n = tab.length) == 0)//初始判断，初始数组为空时
         // resize()初始数组，需要进行扩容操作
         n = (tab = resize()).length;
 
     //这里就是上面的第三步，根据key的hash值找到数据在table中的位置
     if ((p = tab[i = (n - 1) & hash]) == null)
         //通过hash找到的数组下标，里面没有内容就直接赋值
         tab[i] = newNode(hash, key, value, null);
     else {//如果里面已经有内容了
         Node<K,V> e; K k;
         if (p.hash == hash &&
             //hash相同，key也相同，那就直接修改value值
             ((k = p.key) == key || (key != null && key.equals(k))))
             e = p;
         else if (p instanceof TreeNode)
             //key不相同，且节点为红黑树，那就把节点放到红黑树里
             e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
         else {
         //表示节点是链表
             for (int binCount = 0; ; ++binCount) {
                 if ((e = p.next) == null) {
                     //添加到链表尾部
                     p.next = newNode(hash, key, value, null);
                     if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                  //如果满足链表转红黑树的条件，则转红黑树
                         treeifyBin(tab, hash);
                     break;
                 }
                 if (e.hash == hash &&
                     ((k = e.key) == key || (key != null && key.equals(k))))
                     break;
                 p = e;
             }
         }
         //传入的K元素已经存在，直接覆盖value
         if (e != null) { // existing mapping for key
             V oldValue = e.value;
             if (!onlyIfAbsent || oldValue == null)
                 e.value = value;
             afterNodeAccess(e);
             return oldValue;
         }
     }
     ++modCount;
     if (++size > threshold)//检查元素个数是否大于阈值，大于就扩容
         resize();
     afterNodeInsertion(evict);
     return null;
 }
 ```

```java
final void treeifyBin(Node<K,V>[] tab, int hash) {
    int n, index; Node<K,V> e;
    //检查是否满足转换成红黑树的条件，如果数组大小还小于64，则先扩容
    if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
        resize();
    else if ((e = tab[index = (n - 1) & hash]) != null) {
        TreeNode<K,V> hd = null, tl = null;
     do {
           TreeNode<K,V> p = replacementTreeNode(e, null);
           if (tl == null)
                hd = p;
            else {
                p.prev = tl;
                tl.next = p;
            }
            tl = p;
       } while ((e = e.next) != null);
       if ((tab[index] = hd) != null)
           hd.treeify(tab);
    }
}
```

总结put方法流程：

1. 如果table没有初始化就先进行初始化过程

2. 使用hash算法计算key的索引判断索引处有没有存在元素，没有就直接插入

3. 如果索引处存在元素，则遍历插入，有两种情况，

   - 一种是链表形式就直接遍历到尾端插入，

   - 一种是红黑树就按照[红黑树](https://www.seven97.top/java/collection/03-map2-treeset-treemap.html#红黑树)结构

4. 插入链表的数量大于阈值8，且数组大小已经大等于64，就要转换成[红黑树](https://www.seven97.top/java/collection/03-map2-treeset-treemap.html#红黑树)的结构
5. 添加成功后会检查是否需要扩容

### 数组扩容

```java
//table数组的扩容操作
final Node<K,V>[] resize() {
    //引用扩容前的node数组
    Node<K,V>[] oldTab = table;
    //旧的容量
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    //旧的阈值
    int oldThr = threshold;
    //新的容量、阈值初始化为0
    int newCap, newThr = 0;
    
    //计算新容量
    if (oldCap > 0) {
        //如果旧容量已经超过最大容量，让阈值也等于最大容量，以后不再扩容
        if (oldCap >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return oldTab;
        }
        //没超过最大值，就令newcap为原来容量的两倍
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                 oldCap >= DEFAULT_INITIAL_CAPACITY)
            //如果旧容量翻倍没有超过最大值，且旧容量不小于初始化容量16，则翻倍
            newThr = oldThr << 1; // double threshold
    }
    else if (oldThr > 0) // initial capacity was placed in threshold
        //旧容量oldCap = 0时，但是旧的阈值大于0，令初始化容量设置为阈值
        newCap = oldThr;
    else {               // zero initial threshold signifies using defaults
        //两个值都为0的时候使用默认值初始化
        newCap = DEFAULT_INITIAL_CAPACITY;
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
    }
    
    
    if (newThr == 0) {
        //计算新阈值，如果新容量或新阈值大于等于最大容量，则直接使用最大值作为阈值，不再扩容
        float ft = (float)newCap * loadFactor;
        newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                  (int)ft : Integer.MAX_VALUE);
    }
    //设置新阈值
    threshold = newThr;
    
    //创建新的数组，并引用
    @SuppressWarnings({"rawtypes","unchecked"})
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    table = newTab;
    
    //如果老的数组有数据，也就是是扩容而不是初始化，才执行下面的代码，否则初始化的到这里就可以结束了
    if (oldTab != null) {
        //轮询老数组所有数据
        for (int j = 0; j < oldCap; ++j) {
            //以一个新的节点引用当前节点，然后释放原来的节点的引用
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {//如果这个桶，不为空，说明桶中有数据
                oldTab[j] = null;
                //如果e没有next节点，证明这个节点上没有hash冲突，则直接把e的引用给到新的数组位置上
                if (e.next == null)
                    //确定元素在新的数组里的位置
                    newTab[e.hash & (newCap - 1)] = e;
                //如果是红黑树，则进行分裂
                else if (e instanceof TreeNode)
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                //说明是链表
                else { // preserve order
                    Node<K,V> loHead = null, loTail = null;
                    Node<K,V> hiHead = null, hiTail = null;
                    Node<K,V> next;
                    //从这条链表上第一个元素开始轮询，如果当前元素新增的bit是0，则放在当前这条链表上
                    //如果是1，则放在"j+oldcap"这个位置上，生成“低位”和“高位”两个链表
                    do {
                        next = e.next;
                        if ((e.hash & oldCap) == 0) {
                            if (loTail == null)
                                loHead = e;
                            else
                                //元素是不断的加到尾部的
                                loTail.next = e;
                            //新增的元素永远是尾元素
                            loTail = e;
                        }
                        else {
                            //高位的链表与低位的链表处理逻辑一样，不断的把元素加到链表尾部
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = next) != null);
                    //低位链表放到j这个索引的位置上
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead;
                    }
                    //高位链表放到(j+oldCap)这个索引的位置上
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }
    return newTab;
}
```

显然，HashMap的扩容机制，就是当达到扩容条件时会进行扩容。扩容条件就是当HashMap中的元素个数超过临界值时就会自动扩容（threshold = loadFactor * capacity）。如果是初始化扩容，只执行resize的前半部分代码，但如果是随着元素的增加而扩容，HashMap需要重新计算oldTab中每个值的位置，即重建hash表，随着元素的不断增加，HashMap会发生多次扩容，这样就会非常影响性能。所以一般建议创建HashMap的时候指定初始化容量

但是当使用HashMap(int initialCapacity)来初始化容量的时候，HashMap并不会使用传进来的initialCapacity直接作为初始容量。JDK会默认计算一个相对合理的值当做初始容量。所谓合理值，其实是找到第一个比用户传入的值大的**2的幂**。也就是说，当new HashMap(7)创建HashMap的时候，JDK会通过计算，创建一个容量为8的Map；当new HashMap(9)创建HashMap的时候，JDK会通过计算，创建一个容量为16的Map。当然了，当创建一个`HashMap`时，表的大小并不会立即分配，而是在第一次put元素时进行分配，并且分配的大小会是大于或等于初始容量的最小的2的幂。

> 一般来说，initialCapacity = (需要存储的元素个数 / 负载因子) + 1。注意负载因子（即 loaderfactor）默认为 0.75，如果暂时无法确定初始值大小，请设置为 16（即默认值）。HashMap 需要放置 1024 个元素，由于没有设置容量初始大小，随着元素增加而被迫不断扩容，resize() 方法总共会调用 8 次，反复重建哈希表和数据迁移。当放置的集合元素个数达千万级时会影响程序性能。

```java
//初始化容量
public HashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
}

public HashMap(int initialCapacity, float loadFactor) {
    //保证initialCapacity在合理范围内，大于0小于最大容量
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
    if (initialCapacity > MAXIMUM_CAPACITY)
          initialCapacity = MAXIMUM_CAPACITY;
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
           throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
    this.loadFactor = loadFactor;
    this.threshold = tableSizeFor(initialCapacity);
}

static final int tableSizeFor(int cap) {
    int n = cap - 1;
    //|=计算方式：两个二进制对应位都为0时,结果等于0,否则结果等于1;
    n |= n >>> 1;
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```

```java
/**
 * 红黑树分裂方法
 */
final void split(HashMap<K,V> map, Node<K,V>[] tab, int index, int bit) {
     //当前这个节点的引用，即这个索引上的树的根节点
     TreeNode<K,V> b = this;
     // Relink into lo and hi lists, preserving order
     TreeNode<K,V> loHead = null, loTail = null;
     TreeNode<K,V> hiHead = null, hiTail = null;
     //高位低位的初始树节点个数都设成0
     int lc = 0, hc = 0;
     for (TreeNode<K,V> e = b, next; e != null; e = next) {
            next = (TreeNode<K,V>)e.next;
           e.next = null;
           //bit=oldcap,这里判断新bit位是0还是1，如果是0就放在低位树上，如果是1就放在高位树上，这里先是一个双向链表
           if ((e.hash & bit) == 0) {
               if ((e.prev = loTail) == null)
                    loHead = e;
               else
                    loTail.next = e;
               loTail = e;
               ++lc;
            }
            else {
                if ((e.prev = hiTail) == null)
                    hiHead = e;
                else
                    hiTail.next = e;
                hiTail = e;
                ++hc;
            }
       }
 
       if (loHead != null) {
            if (lc <= UNTREEIFY_THRESHOLD)
                //！！！如果低位的链表长度小于阈值6，则把树变成链表，并放到新数组中j索引位置
                tab[index] = loHead.untreeify(map);
            else {
                tab[index] = loHead;
                //高位不为空，进行红黑树转换
                if (hiHead != null) // (else is already treeified)
                    loHead.treeify(tab);
            }
        }
        if (hiHead != null) {
            if (hc <= UNTREEIFY_THRESHOLD)
                tab[index + bit] = hiHead.untreeify(map);
            else {
                tab[index + bit] = hiHead;
                if (loHead != null)
                    hiHead.treeify(tab);
            }
        }
}
```

```java
/**
 * 将树转变为单向链表
 */
final Node<K,V> untreeify(HashMap<K,V> map) {
     Node<K,V> hd = null, tl = null;
     for (Node<K,V> q = this; q != null; q = q.next) {
          Node<K,V> p = map.replacementNode(q, null);
          if (tl == null)
                hd = p;
          else
               tl.next = p;
          tl = p;
      }
     return hd;
}
```

```java
/**
 * 链表转换为红黑树，会根据红黑树特性进行颜色转换、左旋、右旋等
 */
final void treeify(Node<K,V>[] tab) {
      TreeNode<K,V> root = null;
      for (TreeNode<K,V> x = this, next; x != null; x = next) {
             next = (TreeNode<K,V>)x.next;
           x.left = x.right = null;
           if (root == null) {
                x.parent = null;
                x.red = false;
                root = x;
           }
           else {
                K k = x.key;
                int h = x.hash;
                Class<?> kc = null;
                for (TreeNode<K,V> p = root;;) {
                    int dir, ph;
                    K pk = p.key;
                    if ((ph = p.hash) > h)
                         dir = -1;
                    else if (ph < h)
                         dir = 1;
                    else if ((kc == null &&
                              (kc = comparableClassFor(k)) == null) ||
                             (dir = compareComparables(kc, k, pk)) == 0)
                          dir = tieBreakOrder(k, pk);
 
                    TreeNode<K,V> xp = p;
                    if ((p = (dir <= 0) ? p.left : p.right) == null) {
                        x.parent = xp;
                        if (dir <= 0)
                            xp.left = x;
                        else
                            xp.right = x;
                        //进行左旋、右旋调整
                        root = balanceInsertion(root, x);
                        break;
                    }
                 }
            }
        }
        moveRootToFront(tab, root);
}
```

总结HashMap的实现：

1. HashMap的内部存储结构其实是 数组+ 链表+ [红黑树](https://www.seven97.top/java/collection/03-map2-treeset-treemap.html#红黑树) 的结合。当实例化一个HashMap时，会初始化initialCapacity和loadFactor，此时还不会创建数组
2. 在put第一对映射关系时，系统会创建一个长度为initialCapacity（默认为16）的Node数组，这个长度在哈希表中被称为容量(Capacity)，在这个数组中可以存放元素的位置我们称之为“桶”(bucket)，每个bucket都有自己的索引，系统可以根据索引快速的查找bucket中的元素。
3. 每个bucket中存储一个元素，即一个Node对象，但每一个Node对象可以带一个引用变量next，用于指向下一个元素，因此，在一个桶中，就有可能生成一个Node链。也可能是一个一个TreeNode对象，每一个TreeNode对象可以有两个叶子结点left和right，因此，在一个桶中，就有可能生成一个TreeNode树。而新添加的元素作为链表的last，或树的叶子结点

总结HashMap的扩容和树形化：

1. 当HashMap中的元素个数超过数组大小(数组总大小length,不是数组中个数size)\*loadFactor 时，就会进行数 组扩容，loadFactor的默认值(DEFAULT_LOAD_FACTOR)为0.75，这是一个折中的取值。也就是说，默认情况下，数组大小(DEFAULT_INITIAL_CAPACITY)为16，那么当HashMap中元素个数超过16*0.75=12（这个值就是代码中的threshold值，也叫做临界值）的时候，就把数组的大小扩展为 2*16=32，即扩大一倍，然后重新计算每个元素在数组中的位置，而这是一个非常消耗性能的操作，所以如果我们已经预知HashMap中元素的个数，那么预设元素的个数能够有效的提高HashMap的性能
2. 当HashMap中的其中一个链的对象个数如果达到了8个，此时如果capacity没有达到64，那么HashMap会先扩容解决，如果已经达到了64，那么这个链会变成树，结点类型由Node变成TreeNode类型。当然，如果当映射关系被移除后，下次resize方法时判断树的结点个数低于6个，也会把树再转为链表。

即当数组的某一个索引位置上的元素以链表形式存在的数据个数>8且当前数组的长度>64时，此时此索引位置上的所有数据改为使用[红黑树](https://www.seven97.top/java/collection/03-map2-treeset-treemap.html#红黑树)存储

### get方法

```java
public V get(Object key) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
}

final Node<K,V> getNode(int hash, Object key) {
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) {
            //1、判断第一个元素是否与key匹配
            if (first.hash == hash &&
                ((k = first.key) == key || (key != null && key.equals(k))))
                return first;
            if ((e = first.next) != null) {
                //2、判断链表是否红黑树结构
                if (first instanceof TreeNode)
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);
                //3、如果不是红黑树结构，直接循环判断
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
}


/**
  * 这里面情况分很多中，主要是因为考虑了hash相同但是key值不同的情况，查找的最核心还是落在key值上
  */
final TreeNode<K,V> find(int h, Object k, Class<?> kc) {
      TreeNode<K,V> p = this;
      do {
           int ph, dir; K pk;
           TreeNode<K,V> pl = p.left, pr = p.right, q;
           //判断要查询元素的hash是否在树的左边
           if ((ph = p.hash) > h)
                p = pl;
           //判断要查询元素的hash是否在树的右边
                else if (ph < h)
                p = pr;
           //查询元素的hash与当前树节点hash相同情况
           else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                return p;
           //上面的三步都是正常的在二叉查找树中寻找对象的方法
           //如果hash相等，但是内容却不相等
           else if (pl == null)
                p = pr;
           else if (pr == null)
                p = pl;
           //如果可以根据compareTo进行比较的话就根据compareTo进行比较
           else if ((kc != null ||
                    (kc = comparableClassFor(k)) != null) &&
                    (dir = compareComparables(kc, k, pk)) != 0)
                p = (dir < 0) ? pl : pr;
                //根据compareTo的结果在右孩子上继续查询
            else if ((q = pr.find(h, k, kc)) != null)
                return q;
            //根据compareTo的结果在左孩子上继续查询
            else
                p = pl;
      } while (p != null);
      return null;
}
```

总结get方法：

1. 首先通过hash()函数得到对应数组下标，然后依次判断。
2. 判断第一个元素与key是否匹配，如果匹配就返回参数值；
3. 判断链表是否红黑树，如果是红黑树，就进入红黑树方法获取参数值；
4. 如果不是红黑树结构，直接循环遍历链表判断，直到获取参数为止；

### remove方法

jdk1.8的删除逻辑实现比较复杂，删除时有红黑树节点删除和调整：

1. 默认判断链表第一个元素是否是要删除的元素；
2. 如果第一个不是，就继续判断当前冲突链表是否是红黑树，如果是，就进入红黑树里面去找；
3. 如果当前冲突链表不是红黑树，就直接在链表中循环判断，直到找到为止；
4. 将找到的节点，删除掉，如果是红黑树结构，会进行颜色转换、左旋、右旋调整，直到满足红黑树特性为止；



## HashMap的遍历

HashMap **遍历从大的方向来说，可分为以下 4 类**：

1. 迭代器（Iterator）方式遍历；
2. For Each 方式遍历；
3. Lambda 表达式遍历（JDK 1.8+）;
4. Streams API 遍历（JDK 1.8+）。

但每种类型下又有不同的实现方式，因此具体的遍历方式又可以分为以下 7 种：

1. 使用迭代器（Iterator）EntrySet 的方式进行遍历；
2. 使用迭代器（Iterator）KeySet 的方式进行遍历；
3. 使用 For Each EntrySet 的方式进行遍历；
4. 使用 For Each KeySet 的方式进行遍历；
5. 使用 Lambda 表达式的方式进行遍历；
6. 使用 Streams API 单线程的方式进行遍历；
7. 使用 Streams API 多线程的方式进行遍历。

接下来我们来看每种遍历方式的具体实现代码。

### 遍历方式的具体实现

#### 迭代器 EntrySet

```java
public class HashMapTest {
    public static void main(String[] args) {
        // 创建并赋值 HashMap
        Map<Integer, String> map = new HashMap();
        map.put(1, "Java");
        map.put(2, "JDK");
        map.put(3, "Spring Framework");
        map.put(4, "MyBatis framework");
        map.put(5, "Java中文社群");
        // 遍历
        Iterator<Map.Entry<Integer, String>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, String> entry = iterator.next();
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
        }
    }
}
```



#### 迭代器 KeySet

```java
public class HashMapTest {
    public static void main(String[] args) {
        // 创建并赋值 HashMap
        Map<Integer, String> map = new HashMap();
        map.put(1, "Java");
        map.put(2, "JDK");
        map.put(3, "Spring Framework");
        map.put(4, "MyBatis framework");
        map.put(5, "Java中文社群");
        // 遍历
        Iterator<Integer> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            Integer key = iterator.next();
            System.out.println(key);
            System.out.println(map.get(key));
        }
    }
}
```



#### ForEach EntrySet

```java
public class HashMapTest {
    public static void main(String[] args) {
        // 创建并赋值 HashMap
        Map<Integer, String> map = new HashMap();
        map.put(1, "Java");
        map.put(2, "JDK");
        map.put(3, "Spring Framework");
        map.put(4, "MyBatis framework");
        map.put(5, "Java中文社群");
        // 遍历
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
        }
    }
}
```



#### ForEach KeySet

```java
public class HashMapTest {
    public static void main(String[] args) {
        // 创建并赋值 HashMap
        Map<Integer, String> map = new HashMap();
        map.put(1, "Java");
        map.put(2, "JDK");
        map.put(3, "Spring Framework");
        map.put(4, "MyBatis framework");
        map.put(5, "Java中文社群");
        // 遍历
        for (Integer key : map.keySet()) {
            System.out.println(key);
            System.out.println(map.get(key));
        }
    }
}
```



#### Lambda

```java
public class HashMapTest {
    public static void main(String[] args) {
        // 创建并赋值 HashMap
        Map<Integer, String> map = new HashMap();
        map.put(1, "Java");
        map.put(2, "JDK");
        map.put(3, "Spring Framework");
        map.put(4, "MyBatis framework");
        map.put(5, "Java中文社群");
        // 遍历
        map.forEach((key, value) -> {
            System.out.println(key);
            System.out.println(value);
        });
    }
}
```



#### Streams API 单线程

```java
public class HashMapTest {
    public static void main(String[] args) {
        // 创建并赋值 HashMap
        Map<Integer, String> map = new HashMap();
        map.put(1, "Java");
        map.put(2, "JDK");
        map.put(3, "Spring Framework");
        map.put(4, "MyBatis framework");
        map.put(5, "Java中文社群");
        // 遍历
        map.entrySet().stream().forEach((entry) -> {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
        });
    }
}
```



#### Streams API 多线程

```java
public class HashMapTest {
    public static void main(String[] args) {
        // 创建并赋值 HashMap
        Map<Integer, String> map = new HashMap();
        map.put(1, "Java");
        map.put(2, "JDK");
        map.put(3, "Spring Framework");
        map.put(4, "MyBatis framework");
        map.put(5, "Java中文社群");
        // 遍历
        map.entrySet().parallelStream().forEach((entry) -> {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
        });
    }
}
```



### 性能测试

接下来我们使用 Oracle 官方提供的性能测试工具 JMH（Java Microbenchmark Harness，JAVA 微基准测试套件）来测试一下这 7 种循环的性能。

首先，我们先要引入 JMH 框架，在 `pom.xml` 文件中添加如下配置：

```xml
<!-- https://mvnrepository.com/artifact/org.openjdk.jmh/jmh-core -->
<dependency>
    <groupId>org.openjdk.jmh</groupId>
    <artifactId>jmh-core</artifactId>
    <version>1.23</version>
</dependency>
<!-- https://mvnrepository.com/artifact/org.openjdk.jmh/jmh-generator-annprocess -->
<dependency>
    <groupId>org.openjdk.jmh</groupId>
    <artifactId>jmh-generator-annprocess</artifactId>
    <version>1.23</version>
    <scope>provided</scope>
</dependency>
```

然后编写测试代码，如下所示：

```java
@BenchmarkMode(Mode.AverageTime) // 测试完成时间
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS) // 预热 2 轮，每次 1s
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS) // 测试 5 轮，每次 1s
@Fork(1) // fork 1 个线程
@State(Scope.Thread) // 每个测试线程一个实例
public class HashMapCycleTest {
    static Map<Integer, String> map = new HashMap() {{
        // 添加数据
        for (int i = 0; i < 100; i++) {
            put(i, "val:" + i);
        }
    }};

    public static void main(String[] args) throws RunnerException {
        // 启动基准测试
        Options opt = new OptionsBuilder()
                .include(HashMapCycle.class.getSimpleName()) // 要导入的测试类
                .output("/Users/admin/Desktop/jmh-map.log") // 输出测试结果的文件
                .build();
        new Runner(opt).run(); // 执行测试
    }

    @Benchmark
    public void entrySet() {
        // 遍历
        Iterator<Map.Entry<Integer, String>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, String> entry = iterator.next();
            Integer k = entry.getKey();
            String v = entry.getValue();
        }
    }

    @Benchmark
    public void forEachEntrySet() {
        // 遍历
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            Integer k = entry.getKey();
            String v = entry.getValue();
        }
    }

    @Benchmark
    public void keySet() {
        // 遍历
        Iterator<Integer> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            Integer k = iterator.next();
            String v = map.get(k);
        }
    }

    @Benchmark
    public void forEachKeySet() {
        // 遍历
        for (Integer key : map.keySet()) {
            Integer k = key;
            String v = map.get(k);
        }
    }

    @Benchmark
    public void lambda() {
        // 遍历
        map.forEach((key, value) -> {
            Integer k = key;
            String v = value;
        });
    }

    @Benchmark
    public void streamApi() {
        // 单线程遍历
        map.entrySet().stream().forEach((entry) -> {
            Integer k = entry.getKey();
            String v = entry.getValue();
        });
    }

    public void parallelStreamApi() {
        // 多线程遍历
        map.entrySet().parallelStream().forEach((entry) -> {
            Integer k = entry.getKey();
            String v = entry.getValue();
        });
    }
}
```

所有被添加了 `@Benchmark` 注解的方法都会被测试，因为 parallelStream 为多线程版本性能一定是最好的，所以就不参与测试了，其他 6 个方法的测试结果如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202409211728570.webp)

其中 Units 为 ns/op 意思是执行完成时间（单位为纳秒），而 Score 列为平均执行时间， `±` 符号表示误差。从以上结果可以看出，两个 `entrySet` 的性能相近，并且执行速度最快，接下来是 `stream` ，然后是两个 `keySet`，性能最差的是 `KeySet` 。



**从以上结果可以看出 `entrySet` 的性能比 `keySet` 的性能高出了一倍之多，因此我们应该尽量使用 `entrySet` 来实现 Map 集合的遍历**。



#### 字节码分析

要理解以上的测试结果，我们需要把所有遍历代码通过 `javac` 编译成字节码来看具体的原因。

编译后，我们使用 Idea 打开字节码，内容如下：

```java
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class HashMapTest {
    static Map<Integer, String> map = new HashMap() {
        {
            for(int var1 = 0; var1 < 2; ++var1) {
                this.put(var1, "val:" + var1);
            }
        }
    };

    public HashMapTest() {
    }

    public static void main(String[] var0) {
        entrySet();
        keySet();
        forEachEntrySet();
        forEachKeySet();
        lambda();
        streamApi();
        parallelStreamApi();
    }

    public static void entrySet() {
        Iterator var0 = map.entrySet().iterator();

        while(var0.hasNext()) {
            Entry var1 = (Entry)var0.next();
            System.out.println(var1.getKey());
            System.out.println((String)var1.getValue());
        }

    }

    public static void keySet() {
        Iterator var0 = map.keySet().iterator();

        while(var0.hasNext()) {
            Integer var1 = (Integer)var0.next();
            System.out.println(var1);
            System.out.println((String)map.get(var1));
        }

    }

    public static void forEachEntrySet() {
        Iterator var0 = map.entrySet().iterator();

        while(var0.hasNext()) {
            Entry var1 = (Entry)var0.next();
            System.out.println(var1.getKey());
            System.out.println((String)var1.getValue());
        }

    }

    public static void forEachKeySet() {
        Iterator var0 = map.keySet().iterator();

        while(var0.hasNext()) {
            Integer var1 = (Integer)var0.next();
            System.out.println(var1);
            System.out.println((String)map.get(var1));
        }

    }

    public static void lambda() {
        map.forEach((var0, var1) -> {
            System.out.println(var0);
            System.out.println(var1);
        });
    }

    public static void streamApi() {
        map.entrySet().stream().forEach((var0) -> {
            System.out.println(var0.getKey());
            System.out.println((String)var0.getValue());
        });
    }

    public static void parallelStreamApi() {
        map.entrySet().parallelStream().forEach((var0) -> {
            System.out.println(var0.getKey());
            System.out.println((String)var0.getValue());
        });
    }
}
```

从结果可以看出，除了 Lambda 和 Streams API 之外，通过迭代器循环和 `for` 循环的遍历的 `EntrySet` 最终生成的代码是一样的，他们都是在循环中创建了一个遍历对象 `Entry` ，代码如下：

```java
public static void entrySet() {
    Iterator var0 = map.entrySet().iterator();
    while(var0.hasNext()) {
        Entry var1 = (Entry)var0.next();
        System.out.println(var1.getKey());
        System.out.println((String)var1.getValue());
    }
}
public static void forEachEntrySet() {
    Iterator var0 = map.entrySet().iterator();
    while(var0.hasNext()) {
        Entry var1 = (Entry)var0.next();
        System.out.println(var1.getKey());
        System.out.println((String)var1.getValue());
    }
}
```

而 `KeySet` 的代码也是类似的，如下所示：

```java
public static void keySet() {
    Iterator var0 = map.keySet().iterator();
    while(var0.hasNext()) {
        Integer var1 = (Integer)var0.next();
        System.out.println(var1);
        System.out.println((String)map.get(var1));
    }
} 
public static void forEachKeySet() {
    Iterator var0 = map.keySet().iterator();
    while(var0.hasNext()) {
        Integer var1 = (Integer)var0.next();
        System.out.println(var1);
        System.out.println((String)map.get(var1));
    }
}
```

所以我们在使用迭代器或是 `for` 循环 `EntrySet` 时，他们的性能都是相同的，因为他们最终生成的字节码基本都是一样的；同理 `KeySet` 的两种遍历方式也是类似的。

#### 性能分析

`EntrySet` 之所以比 `KeySet` 的性能高是因为，`KeySet` 在循环时使用了 `map.get(key)`，而 `map.get(key)` 相当于又遍历了一遍 Map 集合去查询 `key` 所对应的值。为什么要用“又”这个词？那是因为**在使用迭代器或者 for 循环时，其实已经遍历了一遍 Map 集合了，因此再使用 `map.get(key)` 查询时，相当于遍历了两遍**。

而 `EntrySet` 只遍历了一遍 Map 集合，之后通过代码“Entry<Integer, String> entry = iterator.next()”把对象的 `key` 和 `value` 值都放入到了 `Entry` 对象中，因此再获取 `key` 和 `value` 值时就无需再遍历 Map 集合，只需要从 `Entry` 对象中取值就可以了。

所以，**`EntrySet` 的性能比 `KeySet` 的性能高出了一倍，因为 `KeySet` 相当于循环了两遍 Map 集合，而 `EntrySet` 只循环了一遍**。



## HashSet

- Set 不能存放重复元素，无序的，允许一个null(基于HashMap 实现，HashMap的key可以为null)；

- Set 基于 Map 实现，Set 里的元素值就是 Map的键值。

 

HashSet 基于 HashMap 实现。放入HashSet中的元素实际上由HashMap的key来保存，而HashMap的value则存储了一个静态的Object对象。

底层源码

```java
public class HashSet<E>
    extends AbstractSet<E>
    implements Set<E>, Cloneable, java.io.Serializable {
    static final long serialVersionUID = -5024744406713321676L;

    private transient HashMap<E,Object> map; //基于HashMap实现
    //...
}
```

<!-- @include: @article-footer.snippet.md -->     
