---
title: Map - WeakHashMap详解
category: Java
tag:
 - 集合
---







## 介绍

从WeakHashMap名字也可以知道，这是一个弱引用的Map，当进行GC回收时，弱引用指向的对象会被GC回收。

因此当使用 **WeakHashMap** 时，即使没有显示的添加或删除任何元素，也可能发生如下情况:

- 调用两次size()方法返回不同的值；

- 两次调用isEmpty()方法，第一次返回false，第二次返回true；

- 两次调用containsKey()方法，第一次返回true，第二次返回false，尽管两次使用的是同一个key；

- 两次调用get()方法，第一次返回一个value，第二次返回null，尽管两次使用的是同一个对象。

WeakHashMap 特别适用于需要缓存的场景。在缓存场景下，由于内存是有限的，不能缓存所有对象；对象缓存命中可以提高系统效率，但缓存MISS也不会造成错误，因为可以通过计算重新得到。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250921308.jpg)

从上图可以看出：

1. WeakHashMap继承于AbstractMap，并且实现了Map接口。

2. WeakHashMap是哈希表，但是它的键是"弱键"。WeakHashMap中保护几个重要的成员变量：table, size, threshold, loadFactor, modCount, queue。

   - table是一个Entry[]数组类型，而Entry实际上就是一个单向链表。哈希表的"key-value键值对"都是存储在Entry数组中的。

   - size是Hashtable的大小，它是Hashtable保存的键值对的数量。

   - threshold是Hashtable的阈值，用于判断是否需要调整Hashtable的容量。threshold的值="容量*加载因子"。

   - loadFactor就是加载因子。

   - modCount是用来实现fail-fast机制的

   - queue保存的是“已被GC清除”的“弱引用的键”。

 

## 底层源码

### 构造器

```java
// 默认构造函数。
WeakHashMap()

// 指定“容量大小”的构造函数
WeakHashMap(int capacity)

// 指定“容量大小”和“加载因子”的构造函数
WeakHashMap(int capacity, float loadFactor)

// 包含“子Map”的构造函数
WeakHashMap(Map<? extends K, ? extends V> map)
```

从WeakHashMap的继承关系上来看，可知其继承AbstractMap，实现了Map接口。其底层数据结构是Entry数组，Entry的数据结构如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250921304.jpg)

从源码上可知，Entry的内部并没有存储key的值，而是通过调用父类的构造方法，传入key和ReferenceQueue，最终key和queue会关联到Reference中，这里是GC时，清清除key的关键，这里大致看下Reference的源码：

```java
private static class ReferenceHandler extends Thread {
   
           private static void ensureClassInitialized(Class<?> clazz) {
               try {
                  Class.forName(clazz.getName(), true, clazz.getClassLoader());
              } catch (ClassNotFoundException e) {
                  throw (Error) new NoClassDefFoundError(e.getMessage()).initCause(e);
              }
          }
  
          static {
             // pre-load and initialize InterruptedException and Cleaner classes
              // so that we don't get into trouble later in the run loop if there's
             // memory shortage while loading/initializing them lazily.
              ensureClassInitialized(InterruptedException.class);
              ensureClassInitialized(Cleaner.class);
          }
  
          ReferenceHandler(ThreadGroup g, String name) {
              super(g, name);
          }
  
          public void run() {
              // 注意这里为一个死循环
              while (true) {
                  tryHandlePending(true);
              }
          }
      }
      static boolean tryHandlePending(boolean waitForNotify) {
          Reference<Object> r;
        Cleaner c;
          try {
              synchronized (lock) {
                  if (pending != null) {
                      r = pending;
                      // 'instanceof' might throw OutOfMemoryError sometimes
                     // so do this before un-linking 'r' from the 'pending' chain...
                      c = r instanceof Cleaner ? (Cleaner) r : null;
                     // unlink 'r' from 'pending' chain
                      pending = r.discovered;
                      r.discovered = null;
                  } else {
                      // The waiting on the lock may cause an OutOfMemoryError
                      // because it may try to allocate exception objects.
                      if (waitForNotify) {
                          lock.wait();
                      }
                      // retry if waited
                      return waitForNotify;
                  }
              }
          } catch (OutOfMemoryError x) {
              // Give other threads CPU time so they hopefully drop some live references
              // and GC reclaims some space.
              // Also prevent CPU intensive spinning in case 'r instanceof Cleaner' above
              // persistently throws OOME for some time...
              Thread.yield();
             // retry
              return true;
          } catch (InterruptedException x) {
              // retry
              return true;
          }
  
          // Fast path for cleaners
          if (c != null) {
              c.clean();
              return true;
          }
          // 加入对列
          ReferenceQueue<? super Object> q = r.queue;
          if (q != ReferenceQueue.NULL) q.enqueue(r);
          return true;
      }
  
      static {
         ThreadGroup tg = Thread.currentThread().getThreadGroup();
          for (ThreadGroup tgn = tg;
               tgn != null;
               tg = tgn, tgn = tg.getParent());
          // 创建handler
         Thread handler = new ReferenceHandler(tg, "Reference Handler");
          /* If there were a special system-only priority greater than
           * MAX_PRIORITY, it would be used here
           */
          // 线程优先级最大 
          handler.setPriority(Thread.MAX_PRIORITY);
          // 设置为守护线程
          handler.setDaemon(true);
         handler.start();
 
        // provide access in SharedSecrets
          SharedSecrets.setJavaLangRefAccess(new JavaLangRefAccess() {
             @Override
              public boolean tryHandlePendingReference() {
                  return tryHandlePending(false);
              }
          });
     }
```



### put()

```java
      public V put(K key, V value) {
          // 确定key值，允许key为null
          Object k = maskNull(key);
          // 获取器hash值
          int h = hash(k);
          // 获取tab
          Entry<K,V>[] tab = getTable();
          // 确定在tab中的位置 简单的&操作
          int i = indexFor(h, tab.length);
         // 遍历，是否要进行覆盖操作  
         for (Entry<K,V> e = tab[i]; e != null; e = e.next) {
             if (h == e.hash && eq(k, e.get())) {
                 V oldValue = e.value;
                 if (value != oldValue)
                     e.value = value;
                 return oldValue;
             }
         }
         
         // 修改次数自增
         modCount++;
         // 取出i上的元素
         Entry<K,V> e = tab[i];
         // 构建链表，新元素在链表头
         tab[i] = new Entry<>(k, value, queue, h, e);
         // 检查是否需要扩容
         if (++size >= threshold)
             resize(tab.length * 2);
         return null;
     }
```

WeakHashMap的put操作与HashMap相似，都会进行覆盖操作(相同key)，但是注意插入新节点是放在链表头。上述代码中还要一个关键的函数getTable

### resize操作

WeakHashMap的扩容操作：这里因为key可能被GC掉，所以在扩容时也许要考虑这种情况

```java
void resize(int newCapacity) {
      Entry<K,V>[] oldTable = getTable();
      // 原数组长度
      int oldCapacity = oldTable.length;
      if (oldCapacity == MAXIMUM_CAPACITY) {
          threshold = Integer.MAX_VALUE;
          return;
      }
      // 创建新的数组  
     Entry<K,V>[] newTable = newTable(newCapacity);
     // 数据转移
     transfer(oldTable, newTable);
     table = newTable;
 
     /*
      * If ignoring null elements and processing ref queue caused massive
      * shrinkage, then restore old table.  This should be rare, but avoids
      * unbounded expansion of garbage-filled tables.
      */
     // 确定扩容阈值 
     if (size >= threshold / 2) {
         threshold = (int)(newCapacity * loadFactor);
     } else {
         // 清除被GC的value
         expungeStaleEntries();
         // 数组转移
         transfer(newTable, oldTable);
         table = oldTable;
     }
 }
     
  private void transfer(Entry<K,V>[] src, Entry<K,V>[] dest) {
     // 遍历原数组
     for (int j = 0; j < src.length; ++j) {
         // 取出元素
         Entry<K,V> e = src[j];
         src[j] = null;
         // 链式找元素
         while (e != null) {
             Entry<K,V> next = e.next;
             Object key = e.get();
             // key被回收的情况
             if (key == null) {
                e.next = null;  // Help GC
                 e.value = null; //  "   "
                 size--;
             } else {
                 // 确定在新数组的位置
                 int i = indexFor(e.hash, dest.length);
                 // 插入元素 注意这里为头插法，会倒序
                 e.next = dest[i];
                 dest[i] = e;
             }
             e = next;
         }
     }
 }
```



#### 过期元素（弱引用）清除

```java
 private void expungeStaleEntries() {
          // 从队列中取出被GC的Entry
          for (Object x; (x = queue.poll()) != null; ) {
              synchronized (queue) {
                  @SuppressWarnings("unchecked")
                      Entry<K,V> e = (Entry<K,V>) x;
                  // 确定元素在队列中的位置
                  int i = indexFor(e.hash, table.length);
                  // 取出数组中的第一个元素 prev   
                 Entry<K,V> prev = table[i];
                 Entry<K,V> p = prev;
                 // 循环
                 while (p != null) {
                     Entry<K,V> next = p.next;
                     // 找到
                     if (p == e) {
                         // 判断是否是链表头元素 第一次时
                         if (prev == e)
                             // 将next直接挂在i位置
                             table[i] = next;
                         else
                             // 进行截断 
                             prev.next = next;
                         // Must not null out e.next;
                         // stale entries may be in use by a HashIterator
                         e.value = null; // Help GC
                         size--;
                         break;
                    }
                     // 更新prev和p
                     prev = p;
                     p = next;
                 }
             }
         }
     }
```

该函数的主要作用就是清除Entry的value，该Entry是在GC清除key的过程中入队的。

## 总结

1. WeakHashMap非同步，默认容量为16，扩容因子默认为0.75，底层数据结构为Entry数组**（数组+链表）**。
2. WeakHashMap中的弱引用key会在下一次GC被清除，注意只会**清除key**，value会在每次map操作中清除。

 

 

 <!-- @include: @article-footer.snippet.md -->     

 

 

