---
title: 新集合 - BiMap&Multimap&Multiset
category: 工具类库
tag:
 - Guava
 - 集合
head:
  - - meta
    - name: keywords
      content: Guava,BiMap,Multimap,Multiset
  - - meta
    - name: description
      content: 全网最全的Guava知识点总结，让天下没有难学的八股文！
---



## BiMap

Map 可以实现 key -> value 的映射，如果想要 value -> key 的映射，就需要定义两个 Map，并且同步更新，很不优雅。Guava 提供了 BiMap 支持支持双向的映射关系，常用实现有`HashMap, EnumBiMap, EnumHashBiMap...`。

而它对key和value严格的保证唯一性。如果使用put方法添加相同的value值或key值则会抛出异常：java.lang.IllegalArgumentException，如果使用forcePut方法添加则会覆盖掉原来的value值。

```java
BiMap<String, Integer> biMap = HashBiMap.create();
biMap.put("A", 100);

// 删除已存在的 KV，重新添加 KV
biMap.forcePut("A", 200);

// 获取反向映射
BiMap<Integer, String> inverse = biMap.inverse();
log.debug("{}", inverse.get(100));
```

这里主要使用HashBiMap 进行分析

### 成员变量

```java
private static final double LOAD_FACTOR = 1.0D;
// BiEntry是HashBiMap中为的Map.Entry接口的实现类，这里定义了两个BiEntry，一个是实现使用Key找到value的，另一个是实现使用value找到key的
private transient HashBiMap.BiEntry<K, V>[] hashTableKToV;
private transient HashBiMap.BiEntry<K, V>[] hashTableVToK;
private transient int size;
private transient int mask;
private transient int modCount;
private transient BiMap<V, K> inverse;
```

HashMap做的是唯一key值对应的value可以不唯一，而Bimap做的是唯一key值，value值也要唯一，方便从key找到value，从value找到key

```java
private static final class BiEntry<K, V> extends ImmutableEntry<K, V> {
    //key的hash值
    final int keyHash;
    //value的hash值
    final int valueHash;
    @Nullable
    //为key链表做的指向下一个节点的变量
    HashBiMap.BiEntry<K, V> nextInKToVBucket;
    @Nullable
    //为value链表做的指向下一个节点的变量
    HashBiMap.BiEntry<K, V> nextInVToKBucket;
    BiEntry(K key, int keyHash, V value, int valueHash) {
        super(key, value);
        this.keyHash = keyHash;
        this.valueHash = valueHash;
    }
}
```

对比一下HashMap的Node源码：

```java
static class Node<K,V> implements Map.Entry<K,V> {
    //因为HashMap实现的功能只需要key找到value，所以这里的hash值默认就是key的hash值
    final int hash;
    final K key;
    V value;
    //在HashMap中的链表只做key的链表就好，所以只需要一个指向下一个节点的变量
    Node<K,V> next;
}
```



### 构造方法

```java
//传入期望容器长度
private HashBiMap(int expectedSize) {
    this.init(expectedSize);
}
```

可以看到构造方法是私有的，所以在类中一定会有静态方法构造器会用到这个私有的构造方法。

这个构造方法调用了init方法，可以看一下init方法的源码：

```java
private void init(int expectedSize) {
    CollectPreconditions.checkNonnegative(expectedSize, "expectedSize");
    //经过closedTableSize方法运算达到期望的实际值
    int tableSize = Hashing.closedTableSize(expectedSize, 1.0D);
    //初始化key和value存储链表的数组
    this.hashTableKToV = this.createTable(tableSize);
    this.hashTableVToK = this.createTable(tableSize);
    //初始化mask为数组最大小标值
    this.mask = tableSize - 1;
    //初始化modCount值为0
    this.modCount = 0;
    //初始化size值为0
    this.size = 0;
}
```



### 静态方法构造器

```java
public static <K, V> HashBiMap<K, V> create() {
    //调用另一个create构造器，期望长度为16
    return create(16);
}
public static <K, V> HashBiMap<K, V> create(int expectedSize) {
    //直接创建一个长度为expectedSize的HashBiMap
    return new HashBiMap(expectedSize);
}
public static <K, V> HashBiMap<K, V> create(Map<? extends K, ? extends V> map) {
    //创建一个与传入map相同长度的biMap
    HashBiMap bimap = create(map.size());
    //然后将传入map的值全部赋值给新的BiMap
    bimap.putAll(map);
    return bimap;
}
```



### 添加功能

添加功能有两种，一个是put方法，一个是forcePut方法：

```java
public V put(@Nullable K key, @Nullable V value) {
    return this.put(key, value, false);
}
public V forcePut(@Nullable K key, @Nullable V value) {
    return this.put(key, value, true);
}
```

可以看到，这两个方法同时调用了本类的put方法，只不过是这两个方法的第三个参数不同，一个为ture，一个为false，看一下put的源码，看看第三个参数有什么用

```java
private V put(@Nullable K key, @Nullable V value, boolean force) {
    //获取传入key的hash值
    int keyHash = hash(key);
    //获取传入value的hash值
    int valueHash = hash(value);
    //根据key的值和它的hash值查找是否存在这个节点，seekByKey方法就是遍历了这个keyhash所映射的下标上的链表进行查找。
    HashBiMap.BiEntry oldEntryForKey = this.seekByKey(key, keyHash);
    if(oldEntryForKey != null && valueHash == oldEntryForKey.valueHash && Objects.equal(value, oldEntryForKey.value)) {
        //如果这个key值存在，并且value也相等，则返回这个value
        return value;
    } else {
        //使用seekByValue查找这个value是否存在
        HashBiMap.BiEntry oldEntryForValue = this.seekByValue(value, valueHash);
        if(oldEntryForValue != null) {
			//如果存在，则判断force（第三个参数）是否为false
            if(!force) {//Value已经存在了，因此要判断是否允许强制插入
                //如果force（第三个参数）为false
                //则直接抛出异常
                String newEntry1 = String.valueOf(String.valueOf(value));
                throw new IllegalArgumentException((new StringBuilder(23 + newEntry1.length())).append("value already present: ").append(newEntry1).toString());
            }
            //如果force（第三个参数）为true，则删除这个节点，这个方法是双向删除
            this.delete(oldEntryForValue);
        }
        //如果key存在，则删除这个节点
        if(oldEntryForKey != null) {
            this.delete(oldEntryForKey);
        }
        //根据key，value，keyHash，valueHash创建一个BiEntry
        HashBiMap.BiEntry newEntry = new HashBiMap.BiEntry(key, keyHash, value, valueHash);
        //插入这个节点。
        this.insert(newEntry);
        //插入完成，刷新一下，看看是否需要扩容
        this.rehashIfNecessary();
        return oldEntryForKey == null?null:oldEntryForKey.value;
    }
}
```

```java
private void insert(HashBiMap.BiEntry<K, V> entry) {
    //计算出这个节点在key容器中的下标位置
    int keyBucket = entry.keyHash & this.mask;
    //使当前节点的keynext指向当前下标位置上
    entry.nextInKToVBucket = this.hashTableKToV[keyBucket];
    //将当前节点赋值给这个下标位置
    this.hashTableKToV[keyBucket] = entry;
 
    //value如key一样
    int valueBucket = entry.valueHash & this.mask;
    entry.nextInVToKBucket = this.hashTableVToK[valueBucket];
    this.hashTableVToK[valueBucket] = entry;
    //size加1
    ++this.size;
    ++this.modCount;
}
```



## Multimap

支持将 key 映射到多个 value 的方式，而不用定义`Map<K, List<V>> 或 Map<K, Set<V>>`这样的形式。实现类包括`ArrayListMultimap, HashMultimap, LinkedListMultimap, TreeMultimap...`

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



### HashMultimap构造器

因为他的构造方法是私有的，所有他会拥有静态方法构造器：

```java
public static <K, V> HashMultimap<K, V> create() {
	//new一个HashMultimap，不传入任何值
    return new HashMultimap();
}
public static <K, V> HashMultimap<K, V> create(int expectedKeys, int expectedValuesPerKey) {
	//new一个HashHultimap，传入两个值，一个是期望key的长度，另一个是期望value的长度
    return new HashMultimap(expectedKeys, expectedValuesPerKey);
}
public static <K, V> HashMultimap<K, V> create(Multimap<? extends K, ? extends V> multimap) {
	//传入一个Multimap值
    return new HashMultimap(multimap);
}
```

三个构造方法都调用了私有的构造器，私有构造器的源码如下：

```java
private HashMultimap() {
    //new一个新的map然后交给父类处理
    super(new HashMap());
}
private HashMultimap(int expectedKeys, int expectedValuesPerKey) {
    //获取一个expectedKeys 的map然后交给父类处理
    super(Maps.newHashMapWithExpectedSize(expectedKeys));
    Preconditions.checkArgument(expectedValuesPerKey >= 0);
    this.expectedValuesPerKey = expectedValuesPerKey;
}
private HashMultimap(Multimap<? extends K, ? extends V> multimap) {
    //获取一个multimap的长度的map交给父类处理
    super(Maps.newHashMapWithExpectedSize(multimap.keySet().size()));
    this.putAll(multimap);
}
```

三个私有构造方法都调用了父类的构造方法，接下来看看父类的构造器源码，发现最后的Multimap的数据结构也体现在AbstractMapBasedMultimap这个类中，所以看一下这个类的构造器个变量：

```java
//底层数据结构是一个key为一个Object类，value为一个容器
private transient Map<K, Collection<V>> map;
//Multimap总长度
private transient int totalSize;
protected AbstractMapBasedMultimap(Map<K, Collection<V>> map) {
    Preconditions.checkArgument(map.isEmpty());
    this.map = map;
}
```



### put方法的实现

```java
public boolean put(@Nullable K key, @Nullable V value) {
//首先在map容器中查看是否有这个key值存在。
Collection collection = (Collection)this.map.get(key);
//如果collection为null，则说明这个key值在map容器中不存在
if(collection == null) {
    //根据这个key创建一个容器
    collection = this.createCollection(key);
    //然后将value放在这个容器中
    if(collection.add(value)) {
        ++this.totalSize;
        this.map.put(key, collection);
        return true;
    } else {
        throw new AssertionError("New Collection violated the Collection spec");
    }
    //如果这个容器存在则直接放入value值
    } else if(collection.add(value)) {
        ++this.totalSize;
        return true;
    } else {
        return false;
    }
}
```



### get方法的实现

```java
public Collection<V> get(@Nullable K key) {
	//首先在map容器中查看是否有这个key值存在。
    Collection collection = (Collection)this.map.get(key);
    //如果为null，则为其创建一个容器
    if(collection == null) {
        collection = this.createCollection(key);
    }
    //根据本类的wrapCollection方法找到并返回一个集合类
    return this.wrapCollection(key, collection);
}
```



## Multiset

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



### 接口源码

```java
public interface Multiset<E> extends Collection<E> {
    //返回给定参数元素的个数
    int count(@Nullable Object var1);
	//向其中添加指定个数的元素
    int add(@Nullable E var1, int var2);
	//移除相应个数的元素
    int remove(@Nullable Object var1, int var2);
	//设定某一个元素的重复次数
    int setCount(E var1, int var2);
	//将符合原有重复个数的元素修改为新的重复次数
    boolean setCount(E var1, int var2, int var3);
	//将不同的元素放入一个Set中
    Set<E> elementSet();
    //类似与Map.entrySet 返回Set<Multiset.Entry>。包含的Entry支持使用getElement()和getCount()
    Set<Multiset.Entry<E>> entrySet();
    boolean equals(@Nullable Object var1);
    int hashCode();
    String toString();
	//返回迭代器
    Iterator<E> iterator();
	//判断是否存在某个元素
    boolean contains(@Nullable Object var1);
	//判断是否存在集合中所有元素
    boolean containsAll(Collection<?> var1);
	//添加元素
    boolean add(E var1);
	//删除某个元素
    boolean remove(@Nullable Object var1);
	//删除集合中所有元素
    boolean removeAll(Collection<?> var1);
    boolean retainAll(Collection<?> var1);
    public interface Entry<E> {
        E getElement();
        int getCount();
        boolean equals(Object var1);
        int hashCode();
        String toString();
    }
}
```

Multiset的接口中方法的实现在AbstractMapBasedMultiset抽象类中，下面针对AbstractMapBasedMultiset类的存储数据结构。add、remove、count和迭代器的实现进行分析



### 存储数据结构

```java
//可以看到实际存储结构为一个Map，key为存储元素，Count类型存储是key这个元素的个数，看一下Count源码：
private transient Map<E, Count> backingMap;

final class Count implements Serializable {
    //记录当前个数
    private int value;
    //构造方法，为变量赋值
    Count(int value) {
        this.value = value;
    }
   	//获取当前个数
    public int get() {
        return this.value;
    }
    //加上指定个数，先加在返回加完后的值
    public int getAndAdd(int delta) {
        int result = this.value;
        this.value = result + delta;
        return result;
    }
    //加上指定个数，先返回，在进行相加
    public int addAndGet(int delta) {
        return this.value += delta;
    }
    //直接设置当前个数
    public void set(int newValue) {
        this.value = newValue;
    }
    //先设置新的值在返回这个值大小
    public int getAndSet(int newValue) {
        int result = this.value;
        this.value = newValue;
        return result;
    }
    public int hashCode() {
        return this.value;
    }
    public boolean equals(@Nullable Object obj) {
        return obj instanceof Count && ((Count)obj).value == this.value;
    }
    public String toString() {
        return Integer.toString(this.value);
    }
}
```



### 构造方法

```java
protected AbstractMapBasedMultiset(Map<E, Count> backingMap) {
    //存储的Map可以为任意类型的Map
    this.backingMap = (Map)Preconditions.checkNotNull(backingMap);
    //获取当前Multiset长度
    this.size = (long)super.size();
}
```



### add方法

```java
public int add(@Nullable E element, int occurrences) {
    //如果想要添加的个数为0
    if(occurrences == 0) {
        //如果存在则返回这个元素的个数，否则返回0
        return this.count(element);
    } else {
        Preconditions.checkArgument(occurrences > 0, "occurrences cannot be negative: %s", new Object[]{Integer.valueOf(occurrences)});
        //根据想要插入的元素在map中找到Count
        Count frequency = (Count)this.backingMap.get(element);
        int oldCount;
        //如果key所对应的Count为null
        if(frequency == null) {
            //设置原来数据为0
            oldCount = 0;
            //将这个元素和所对应的Count添加到Map中
            this.backingMap.put(element, new Count(occurrences));
        } else {
            //获取原来个数
            oldCount = frequency.get();
            //计算出新的个数
            long newCount = (long)oldCount + (long)occurrences;
            Preconditions.checkArgument(newCount <= 2147483647L, "too many occurrences: %s", new Object[]{Long.valueOf(newCount)});
            //为key所对应的Count添加occurrences个
            frequency.getAndAdd(occurrences);
        }
        //将当前的size加上occurrences
        this.size += (long)occurrences;
        //返回原来数据
        return oldCount;
    }
}
```



### remove方法

```java
public int remove(@Nullable Object element, int occurrences) {
    //如果想要删除0个
    if(occurrences == 0) {
        //返回当前这个元素的个数，如果不存在容器中返回0
        return this.count(element);
    } else {
        Preconditions.checkArgument(occurrences > 0, "occurrences cannot be negative: %s", new Object[]{Integer.valueOf(occurrences)});
        //根据要删除的值作为key获取到他的Count
        Count frequency = (Count)this.backingMap.get(element);
        //如果对应的Count为null，则返回0
        if(frequency == null) {
            return 0;
        } else {
            //获取当前个数
            int oldCount = frequency.get();
            int numberRemoved;
            //如果原来个数大于想要删除的个数
            if(oldCount > occurrences) {
                numberRemoved = occurrences;
            } else {
                //如果原来个数小于想要删除的个数
                numberRemoved = oldCount;
                //直接将这个元素在Map中删除
                this.backingMap.remove(element);
            }
            //设置这个元素对应的Count
            frequency.addAndGet(-numberRemoved);
            this.size -= (long)numberRemoved;
            return oldCount;
        }
    }
}
```



### Count方法

```java
public int count(@Nullable Object element) {
    //以传入的作为key，在map容器中找到相对应的Count
    Count frequency = (Count)Maps.safeGet(this.backingMap, element);
    //如果这个Count为空，则返回0，否则返回Count中的值
    return frequency == null?0:frequency.get();
}
```



### 迭代器

```java
public Iterator<E> iterator() {
    return new AbstractMapBasedMultiset.MapBasedMultisetIterator();
}
```

Multiset中有一个实现了Iterator接口的类：

```java
private class MapBasedMultisetIterator implements Iterator<E> {
    final Iterator<java.util.Map.Entry<E, Count>> entryIterator;
    java.util.Map.Entry<E, Count> currentEntry;
    int occurrencesLeft;
    boolean canRemove;
    MapBasedMultisetIterator() {
        //获取当前map容器的迭代器
        this.entryIterator = AbstractMapBasedMultiset.this.backingMap.entrySet().iterator();
    }
    //根据当前迭代器判断是否还有元素
    public boolean hasNext() {
        return this.occurrencesLeft > 0 || this.entryIterator.hasNext();
    }
    public E next() {
        //如果occurrencesLeft为0，则说明现在处于刚开始，或上一个元素完成
        if(this.occurrencesLeft == 0) {
            //迭代器向下获取一个元素
            this.currentEntry = (java.util.Map.Entry)this.entryIterator.next();
            //获取到当前元素的个数
            this.occurrencesLeft = ((Count)this.currentEntry.getValue()).get();
        }
        //因为是获取一个元素，所以减去这一个
        --this.occurrencesLeft;
        this.canRemove = true;
        return this.currentEntry.getKey();
    }
    public void remove() {
        CollectPreconditions.checkRemove(this.canRemove);
        int frequency = ((Count)this.currentEntry.getValue()).get();
        if(frequency <= 0) {
            throw new ConcurrentModificationException();
        } else {
            if(((Count)this.currentEntry.getValue()).addAndGet(-1) == 0) {
                this.entryIterator.remove();
            }
 
            AbstractMapBasedMultiset.access$110(AbstractMapBasedMultiset.this);
            this.canRemove = false;
        }
    }
}
```

这个迭代器的好处是，存储多个相同的值，不会占用多个地方，只会占用1个位置。



<!-- @include: @article-footer.snippet.md -->     

