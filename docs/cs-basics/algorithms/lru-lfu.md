---
title: LRU & LFU
category: 计算机基础
tag:
  - 算法
head:
  - - meta
    - name: keywords
      content: 数据结构,LRU,LFU,Least Recently Used
  - - meta
    - name: description
      content: 全网最全的计算机基础（算法）知识点总结，让天下没有难学的八股文！
---



## LRU

LRU 的全称是 Least Recently Used，也就是说我们认为最近使用过的数据应该是是「有用的」，很久都没用过的数据应该是无用的，内存满了就优先删那些很久没用过的数据。

### 分析

要让 LRU 的 `put` 和 `get` 方法的时间复杂度为 O(1)，可以总结出 LRU 这个数据结构必要的条件：

1、显然  LRU  中的元素必须有时序，以区分最近使用的和久未使用的数据，当容量满了之后要删除最久未使用的那个元素腾位置。

2、要在  LRU 中快速找某个 `key` 是否已存在并得到对应的 `val`；

3、每次访问  LRU  中的某个 `key`，需要将这个元素变为最近使用的，也就是说  LRU  要支持在任意位置快速插入和删除元素。

那么，什么数据结构同时符合上述条件呢？哈希表查找快，但是数据无固定顺序；链表有顺序之分，插入删除快，但是查找慢。所以结合一下，形成一种新的数据结构：哈希链表 `LinkedHashMap`。

LRU 缓存算法的核心数据结构就是哈希链表，双向链表和哈希表的结合体。这个数据结构长这样：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271144787.png)

借助这个结构，逐一分析上面的 3 个条件：

1、如果我们每次默认从链表尾部添加元素，那么显然越靠尾部的元素就是最近使用的，越靠头部的元素就是最久未使用的。

2、对于某一个 `key`，我们可以通过哈希表快速定位到链表中的节点，从而取得对应 `val`。

3、链表显然是支持在任意位置快速插入和删除的，改改指针就行。只不过传统的链表无法按照索引快速访问某一个位置的元素，而这里借助哈希表，可以通过 `key` 快速映射到任意一个链表节点，然后进行插入和删除。



put方法流程图：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271144587.png)

### 代码实现

```java
class LRUCache {
    int cap;
    LinkedHashMap<Integer, Integer> cache = new LinkedHashMap<>();
    public LRUCache(int capacity) { 
        this.cap = capacity;
    }
    
    public int get(int key) {
        if (!cache.containsKey(key)) {
            return -1;
        }
        // 将 key 变为最近使用
        makeRecently(key);
        return cache.get(key);
    }
    
    public void put(int key, int val) {
        if (cache.containsKey(key)) {
            // 修改 key 的值
            cache.put(key, val);
            // 将 key 变为最近使用
            makeRecently(key);
            return;
        }
        
        if (cache.size() >= this.cap) {
            // 链表头部就是最久未使用的 key
            int oldestKey = cache.keySet().iterator().next();
            cache.remove(oldestKey);
        }
        // 将新的 key 添加链表尾部
        cache.put(key, val);
    }
    
    private void makeRecently(int key) {
        int val = cache.get(key);
        // 删除 key，重新插入到队尾
        cache.remove(key);
        cache.put(key, val);
    }
}
```



## LFU

LRU 算法的淘汰策略是 Least Recently Used，也就是每次淘汰那些最久没被使用的数据；而 LFU 算法的淘汰策略是 Least Frequently Used，也就是每次淘汰那些使用次数最少的数据。

### 分析

根据 LFU 算法的逻辑，先列举出算法执行过程中的几个显而易见的事实：

1、调用 `get(key)` 方法时，要返回该 `key` 对应的 `val`。

2、只要用 `get` 或者 `put` 方法访问一次某个 `key`，该 `key` 的 `freq` 就要加一。

3、如果在容量满了的时候进行插入，则需要将 `freq` 最小的 `key` 删除，如果最小的 `freq` 对应多个 `key`，则删除其中最旧的那一个。



希望能够在 O(1) 的时间内解决这些需求，可以使用基本数据结构来逐个解决：

1、使用一个`HashMap`存储`key`到`val`的映射，就可以快速计算`get(key)`。

```java
HashMap<Integer, Integer> keyToVal;
```

2、使用一个`HashMap`存储`key`到`freq`的映射，就可以快速操作`key`对应的`freq`。

```java
HashMap<Integer, Integer> keyToFreq;
```

3、这个需求是 LFU 算法的核心

3.1、首先，肯定是需要`freq`到`key`的映射，用来找到`freq`最小的`key`。

3.2、将`freq`最小的`key`删除，那你就得快速得到当前所有`key`最小的`freq`是多少。想要时间复杂度 O(1) 的话，肯定不能遍历一遍去找，那就用一个变量`minFreq`来记录当前最小的`freq`。

3.3、可能有多个`key`拥有相同的`freq`，所以 **`freq`对`key`是一对多的关系**，即一个`freq`对应一个`key`的列表。

3.4、希望`freq`对应的`key`的列表是存在时序的，便于快速查找并删除最旧的`key`。

3.5、希望能够快速删除`key`列表中的任何一个`key`，因为如果频次为`freq`的某个`key`被访问，那么它的频次就会变成`freq+1`，就应该从`freq`对应的`key`列表中删除，加到`freq+1`对应的`key`的列表中。

    HashMap<Integer, LinkedHashSet<Integer>> freqToKeys;
    int minFreq = 0;



`LinkedHashSet`，能满足我们 3.3，3.4，3.5 这几个要求。你会发现普通的链表`LinkedList`能够满足 3.3，3.4 这两个要求，但是由于普通链表不能快速访问链表中的某一个节点，所以无法满足 3.5 的要求。

`LinkedHashSet`顾名思义，是链表和哈希集合的结合体。链表不能快速访问链表节点，但是插入元素具有时序；哈希集合中的元素无序，但是可以对元素进行快速的访问和删除。那么，它俩结合起来就兼具了哈希集合和链表的特性，既可以在 O(1) 时间内访问或删除其中的元素，又可以保持插入的时序，高效实现 3.5 这个需求。



put方法流程：
![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271144663.png)







### 代码实现

```java
class LFUCache {
	// 记录最小的频次
	int minFreq = 0;
	// 记录 LFU 缓存的最大容量
	int cap;
	// key 到 val 的映射
	HashMap<Integer, Integer> keyToFreq = new HashMap<>();
	// key 到 freq 的映射
	HashMap<Integer, Integer> keyToVal = new HashMap<>();
	// freq 到 key 列表的映射
	HashMap<Integer, LinkedHashSet<Integer>> freqToKeys = new HashMap<>();

	public LFUCache(int capacity) {
		this.cap = capacity;
	}

	public int get(int key) {
		if (!keyToVal.containsKey(key)) {
			return -1;
		}
		// 增加 key 对应的 freq
		increaseFreq(key);
		return keyToVal.get(key);
	}


	public void put(int key, int value) {
		if (this.cap <= 0) return;

		/* 若 key 已存在，修改对应的 val 即可 */
		if (keyToVal.containsKey(key)) {
			keyToVal.put(key, value);
			// key 对应的 freq 加一
			increaseFreq(key);
			return;
		}

		/* key 不存在，需要插入 */
   		 /* 容量已满的话需要淘汰一个 freq 最小的 key */
		if (cap <= keyToVal.size()) {
			removeMinFreqKey();
		}
		
		/* 插入 key 和 val，对应的 freq 为 1 */
    	// 插入 KV 表
		keyToVal.put(key, value);
		// 插入 KF 表
		keyToFreq.put(key, 1);
		// 插入 FK 表
		freqToKeys.putIfAbsent(1, new LinkedHashSet<>());
		freqToKeys.get(1).add(key);
		// 插入新 key 后最小的 freq 肯定是 1
		this.minFreq = 1;

	}

	private void removeMinFreqKey() {
		// freq 最小的 key 列表
		LinkedHashSet<Integer> keyList = freqToKeys.get(this.minFreq);
		// 其中最先被插入的那个 key 就是该被淘汰的 key
		int deletedKey = keyList.iterator().next();
		/* 更新 FK 表 */
		keyList.remove(deletedKey);
		if (keyList.isEmpty()) {
			freqToKeys.remove(this.minFreq);//这里不需要更新 minFreq 的值
		}
		/* 更新 KV 表 */
		keyToVal.remove(deletedKey);
		/* 更新 KF 表 */
		keyToFreq.remove(deletedKey);
	}

	private void increaseFreq(int key) {
		int freq = keyToFreq.get(key);
		/* 更新 KF 表 */
		keyToFreq.put(key, freq + 1);
		/* 更新 FK 表 */
    	// 将 key 从 freq 对应的列表中删除
		freqToKeys.get(freq).remove(key);
		// 将 key 加入 freq + 1 对应的列表中
		freqToKeys.putIfAbsent(freq + 1, new LinkedHashSet<>());
		freqToKeys.get(freq + 1).add(key);

		// 如果 freq 对应的列表空了，移除这个 freq
		if (freqToKeys.get(freq).isEmpty()){
			freqToKeys.remove(freq);
			// 如果这个 freq 恰好是 minFreq，更新 minFreq
			if (freq == this.minFreq) {
				this.minFreq++;
			}
		}

	}
}
```



在removeMinFreqKey()方法中，如果`keyList`中只有一个元素，那么删除之后`minFreq`对应的`key`列表就为空了，也就是`minFreq`变量需要被更新。如何计算当前的`minFreq`是多少呢？

实际上没办法快速计算`minFreq`，只能线性遍历`FK`表或者`KF`表来计算，这样肯定不能保证 O(1) 的时间复杂度。**但是，其实这里没必要更新`minFreq`变量**，因为`removeMinFreqKey`这个函数是在`put`方法中插入新`key`时可能调用。而回头看`put`的代码，插入新`key`时一定会把`minFreq`更新成 1，所以说即便这里`minFreq`变了，也不需要管它。

<!-- @include: @article-footer.snippet.md -->     