---
title: Map - TreeSet & TreeMap 详解
category: Java
tag:
 - 集合
---





## 介绍

**TreeSet**和**TreeMap**在Java里有着相同的实现，前者仅仅是对后者做了一层包装，也就是说**TreeSet里面有一个TreeMap**(适配器模式)。

Java **TreeMap**实现了**SortedMap**接口，也就是说会按照key的大小顺序对**Map**中的元素进行排序，key大小的评判可以通过其本身的自然顺序(natural ordering)，也可以通过构造时传入的比较器(Comparator)。

**TreeMap底层通过红黑树(Red-Black tree)实现**，也就意味着containsKey(), get(), put(), remove()都有着log(n)的时间复杂度。

1. TreeMap存储 Key-Value 对时，需要根据 key-value 对进行排序。TreeMap 可以保证所有的 Key-Value 对处于 有序状态。

   - 正常情况下TreeMap是不能存入值为null的键的。

   - 通过自定义比较器能让TreeMap存入一个值为null的键。

   - 存入的值为null键对应的值不能通过通过它来获取，只能通过直接遍历Values。

2. TreeSet底层使用 红黑树结构存储数据

3. TreeMap 的 Key 的排序：

   - 自然排序：TreeMap 的所有的 Key 必须实现 Comparable 接口，而且所有的 Key 应该是同一个类的对象，否则将会抛出 ClasssCastException

   - 定制排序：创建 TreeMap 时，传入一个 Comparator 对象，该对象负责对TreeMap 中的所有 key 进行排序。此时不需要 Map 的 Key 实现Comparable 接口

4. TreeMap判断 两个key 相等的标准：两个key通过compareTo()方法或者compare()方法返回0。

## 底层实现

### 继承接口的关键方法

```java
public class TreeMap<K,V>
    extends AbstractMap<K,V>
    implements NavigableMap<K,V>, Cloneable, java.io.Serializable
```

返回用于排序此映射中的键的比较器，如果此映射使用其键的自然排序，则返回null。

 

SortedMap接口：

```java
Comparator<? super K> comparator()
返回用于排序此映射中的键的比较器，如果此映射使用其键的自然排序，则返回null。

Set<Map.Entry<K,V>> entrySet()
返回此映射中包含的映射的Set视图。

K firstKey()
返回当前映射中的第一个(最低)键。

K lastKey()
返回当前映射中的最后(最高)键。
```



NavigableMap接口：

```java
Map.Entry<K,V> ceilingEntry(K key)
返回与大于或等于给定键的最小键相关联的键值映射，如果没有这样的键则返回null。

K ceilingKey(K key)
返回大于或等于给定键的最小键，如果没有这样的键，则返回null。

NavigableMap<K,V> descendingMap()
返回此映射中包含的映射的倒序视图。

Map.Entry<K,V> firstEntry()
返回与该映射中最小的键关联的键值映射，如果映射为空，则返回null。

Map.Entry<K,V> floorEntry(K key)
返回与小于或等于给定键的最大键相关联的键值映射，如果没有这样的键则返回null。

SortedMap<K,V> headMap(K toKey)
返回该映射中键严格小于toKey的部分的视图。

Map.Entry<K,V> higherEntry(K key)
返回与严格大于给定键的最小键关联的键值映射，如果没有这样的键，则返回null。

Map.Entry<K,V> lastEntry()
返回与此映射中最大键关联的键值映射，如果映射为空，则返回null。

Map.Entry<K,V> lowerEntry(K key)
返回与严格小于给定键的最大键关联的键值映射，如果没有这样的键，则返回null。

Map.Entry<K,V> pollFirstEntry()
删除并返回与该映射中最小的键关联的键值映射，如果映射为空，则返回null。

Map.Entry<K,V> pollLastEntry()
删除并返回与此映射中最大键关联的键值映射，如果映射为空，则返回null。

SortedMap<K,V> subMap(K fromKey, K toKey)
返回该映射中键范围从fromKey(包含)到toKey(独占)的部分的视图。

SortedMap<K,V> tailMap(K fromKey)
返回该映射中键大于或等于fromKey的部分的视图。
```



### 初始属性

```java
//比较器
private final Comparator<? super K> comparator;

//root根的值
private transient Entry<K,V> root;

//map中数据量
private transient int size = 0;

//修改次数
private transient int modCount = 0;
```



### 构造方法

```java
public TreeMap() {
    comparator = null;
}

//有参构造，可以重新定义比较器
public TreeMap(Comparator<? super K> comparator) {
    this.comparator = comparator;
}

//有参构造，将其他map用TreeMap存储
public TreeMap(Map<? extends K, ? extends V> m) {
    comparator = null;
    putAll(m);
}
```



### put 方法

```java
public V put(K key, V value) {
    //将root赋值给局部变量
    Entry<K,V> t = root;
    if (t == null) {//初始操作
        //检查key是否为空
        compare(key, key); // type (and possibly null) check
        //将要添加的key value封装为一个entry对象，并赋值给root
        root = new Entry<>(key, value, null);
        size = 1;
        modCount++;
        return null;
    }
    int cmp;
    Entry<K,V> parent;//父节点
    // split comparator and comparable paths
    Comparator<? super K> cpr = comparator;//获取比较器
    if (cpr != null) {
        do {
            parent = t;//将root赋值给了parent
            cmp = cpr.compare(key, t.key);//和root节点比较大小
            if (cmp < 0)//key比根节点更小，t就使其为左节点
                t = t.left;
            else if (cmp > 0)//否则使其为右节点
                t = t.right;
            else
                return t.setValue(value);//大小相等，直接修改值
        } while (t != null);
    }
    else {
        if (key == null)
            throw new NullPointerException();
        @SuppressWarnings("unchecked")
            Comparable<? super K> k = (Comparable<? super K>) key;
        do {
            parent = t;
            cmp = k.compareTo(t.key);
            if (cmp < 0)
                t = t.left;
            else if (cmp > 0)
                t = t.right;
            else
                return t.setValue(value);
        } while (t != null);
    }
    // 到此 t 就是要插入节点的父节点，即parent
    //将 k v 对封装成entry对象
    Entry<K,V> e = new Entry<>(key, value, parent);
    if (cmp < 0)
        parent.left = e;//插入节点在 父节点 的左侧
    else 
        parent.right = e;//插入节点在 父节点 的右侧
    fixAfterInsertion(e);//实现红黑树的平衡
    size++;
    modCount++;
    return null;
}
```

关于红黑树的平衡，将在后文介绍

 

## 红黑树

**红黑树的性质**

- 每个节点要么是黑色，要么是红色。

- 根节点是黑色。

- 每个叶子节点（NIL）是黑色。

- 每个红色结点的两个子结点一定都是黑色。

- 任意一结点(包含本身)到其叶子结点的路径都包含数量相同的黑结点。

**红黑树的优点：**

由于在AVL树中，由于AVL树是绝对平衡的，所有在进行插入和删除的时候，为了维护其绝对的平衡性，有时候进行修改节点的操作，需要进行到根节点，旋转的次数比较多，所以出现了红黑树，当数据不是静态的数据而是动态的数据，进行插入和删除的时候就不用去维护绝对的平衡，也就减少了旋转的次数，照样可以提高效率，并且红黑树的平均查找效率还是logn

### 红黑树平衡源码

插入红黑树初始的颜色肯定为红色，注意:**插入节点必须为红色**，理由很简单，红色在父节点(如果存在)为黑色节点时，红黑树的黑色平衡没被破坏，不需要做自平衡操作。但如果插入结点是黑色，那么插入位置所在的子树黑色结点总是多1，必须做自平衡。

可以先看TreeMap红黑树平衡源码，也可以先看后文情景

```java
private void fixAfterInsertion(Entry<K,V> x) {
    x.color = RED;//插入时先把颜色设置为红色
    //循环条件是x不等于空，x不是根，且父节点为红色
    //原因在于①x为根的话可以直接置为黑色(对应情景1)；②父节点若为黑色，x直接以红色插入，不影响平衡条件(对应情景3)
    while (x != null && x != root && x.parent.color == RED) {
        //判断父节点  是否是  祖父节点的左侧节点
        if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
            //获取 祖父节点的右侧节点，也就是叔叔节点S
            Entry<K,V> y = rightOf(parentOf(parentOf(x)));
            //如果 叔叔节点为红色(对应情景4.1) 
            if (colorOf(y) == RED) {
                //1.将P和S设置为黑色2.将PP设置为红色3.将PP设置为当前插入节点再执行规则
                setColor(parentOf(x), BLACK);
                setColor(y, BLACK);
                setColor(parentOf(parentOf(x)), RED);
                x = parentOf(parentOf(x));
            } else {//叔叔节点不存在(对应情景4.2)
              //如果x 是父节点的右节点(对应情景4.2.2)
                if (x == rightOf(parentOf(x))) {
                    x = parentOf(x);//将x的父节点P作为插入节点
                    rotateLeft(x);//左旋
                } //左旋完之后 插入节点x就是P的左节点
                //如果x是父节点的左节点；那就只做一次右旋(对应情景4.2.1)
                setColor(parentOf(x), BLACK);//将P设置为黑色
                setColor(parentOf(parentOf(x)), RED);//将PP设置为红色
                rotateRight(parentOf(parentOf(x)));//右旋
            }
        } else {//父节点 是  祖父节点的右侧节点
            //获取 祖父节点的左侧节点，也就是叔叔节点S
            Entry<K,V> y = leftOf(parentOf(parentOf(x)));
            if (colorOf(y) == RED) {//(对应情景4.1) 
                setColor(parentOf(x), BLACK);
                setColor(y, BLACK);
                setColor(parentOf(parentOf(x)), RED);
                x = parentOf(parentOf(x));
            } else {//(对应情景4.3)
                if (x == leftOf(parentOf(x))) {//(对应情景4.3.2)
                    x = parentOf(x);
                    rotateRight(x);
                }
                //(对应情景4.3.1)
                setColor(parentOf(x), BLACK);
                setColor(parentOf(parentOf(x)), RED);
                rotateLeft(parentOf(parentOf(x)));
            }
        }
    }
    //根节点的颜色为黑色
    root.color = BLACK;
}
```



左旋：以某个节点作为支点(旋转节点)，其右子节点变为旋转节点的父节点，右子节点的左子节点变为旋转节点的右子节点，旋转节点的左子节点保持不变。右子节点的左子节点相当于从右子节点上“断开”，重新连接到旋转节点上。

```java
//左旋
private void rotateLeft(Entry<K,V> p) {
    if (p != null) {
        //
        Entry<K,V> r = p.right;
        p.right = r.left;
        if (r.left != null)
            r.left.parent = p;
        r.parent = p.parent;
        if (p.parent == null)
            root = r;
        else if (p.parent.left == p)
            p.parent.left = r;
        else
            p.parent.right = r;
        r.left = p;
        p.parent = r;
    }
}
```



右旋：以某个节点作为支点(旋转节点)，其左子节点变为旋转节点的父节点，左子节点的右子节点变为旋转节点的左子节点，旋转节点的右子节点保持不变。左子节点的右子节点相当于从左子节点上“断开”，重新连接到旋转节点上。

```java
private void rotateRight(Entry<K,V> p) {
    if (p != null) {
        Entry<K,V> l = p.left;
        p.left = l.right;
        if (l.right != null) l.right.parent = p;
        l.parent = p.parent;
        if (p.parent == null)
            root = l;
        else if (p.parent.right == p)
            p.parent.right = l;
        else p.parent.left = l;
        l.right = p;
        p.parent = l;
    }
}
```

**以下情景中插入的操作转载于：**[**https://www.jianshu.com/p/e136ec79235c**](https://www.jianshu.com/p/e136ec79235c)



### 情景1：红黑树为空树

最简单的一种情景，直接把插入结点作为根结点就行，但注意，根据红黑树性质2：根节点是黑色。还需要把插入结点设为黑色。

处理：把插入结点作为根结点，并把结点设置为黑色。



### 情景2：插入结点的Key已存在

插入结点的Key已存在，既然红黑树总保持平衡，在插入前红黑树已经是平衡的，那么把插入结点设置为将要替代结点的颜色，再把结点的值更新就完成插入（这里的更新的其实是相同 key的 value）

处理：

- 把I设为当前结点的颜色

- 更新当前结点的值为插入结点的值



### 情景3：插入结点的父结点为黑结点

由于插入的结点是红色的，当插入结点的黑色时，并不会影响红黑树的平衡，直接插入即可，无需做自平衡。

处理：直接插入。



### 情景4：插入结点的父结点为红结点

再次回想下红黑树的性质2：根结点是黑色。如果插入的父结点为红结点，那么该父结点不可能为根结点，所以插入结点总是存在祖父结点。这点很重要，因为后续的旋转操作肯定需要祖父结点的参与。



#### 情景4.1：叔叔结点存在并且为红结点

从红黑树性质4可以，祖父结点肯定为黑结点，因为不可以同时存在两个相连的红结点。那么此时该插入子树的红黑层数的情况是：黑红红。显然最简单的处理方式是把其改为：红黑红。

(以下描述中I为插入节点，P为父节点，PP为祖父节点，S为叔叔节点)



处理：

- 将P和S设置为黑色

- 将PP设置为红色

- 把PP设置为当前插入结点

 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250908345.gif)

 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250908361.gif)

把PP结点设为红色了，如果PP的父结点是黑色，那么无需再做任何处理；但如果PP的父结点是红色，根据性质4(每个红色结点的两个子结点一定都是黑色。)，此时红黑树已不平衡了，所以还需要把PP当作新的插入结点，继续做插入操作自平衡处理，直到平衡为止。

试想下PP刚好为根结点时，那么根据性质2，必须把PP重新设为黑色，那么树的红黑结构变为：黑黑红。换句话说，从根结点到叶子结点的路径中，黑色结点增加了。这也是唯一一种会增加红黑树黑色结点层数的插入情景。

我们还可以总结出另外一个经验：红黑树的生长是自底向上的。这点不同于普通的二叉查找树，普通的二叉查找树的生长是自顶向下的。



#### 情景4.2：叔叔结点不存在，并且插入结点的父亲结点是祖父结点的左子结点

单纯从插入前来看，也即不算情景4.1自底向上处理时的情况，叔叔结点非红即为叶子结点(Nil)。

我们没有考虑叔叔节点是黑节点情况，因为如果叔叔结点为黑结点，而父结点为红结点，那么叔叔结点所在的子树的黑色结点就比父结点所在子树的多了，这不满足红黑树的性质5。后续情景同样如此，不再多做说明了。



##### 情景4.2.1：插入结点是其父结点的左子结点

处理：

- 将P设为黑色

- 将PP设为红色

- 对PP进行右旋

 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250908355.jpg)

左边两个红结点，右边不存在，那么一边一个刚刚好，并且因为为红色，肯定不会破坏树的平衡。



##### 情景4.2.2：插入结点是其父结点的右子结点

这种情景显然可以转换为情景4.2.1



处理：

- 对P进行左旋

- 把P设置为插入结点，得到情景4.2.1

- 进行情景4.2.1的处理

 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250908370.jpg)



#### 情景4.3：叔叔结点不存在，并且插入结点的父亲结点是祖父结点的右子结点

该情景对应情景4.2，只是方向反转



##### 情景4.3.1：插入结点是其父结点的右子结点

处理：

- 将P设为黑色

- 将PP设为红色

- 对PP进行左旋

 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250908358.jpg)



##### 情景4.3.2：插入结点是其父结点的左子结点

处理：

- 对P进行右旋

- 把P设置为插入结点，得到情景4.3.1

- 进行情景4.3.1的处理

 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250908373.jpg)

 

 

 