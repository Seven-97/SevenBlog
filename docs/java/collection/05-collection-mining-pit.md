---
title: 集合采坑记录
category: Java
tag:
 - 集合
---





## subList

先来看下subList的简单使用：

```java
List<String> bookList = new ArrayList<>();
bookList.add("遥远的救世主");
bookList.add("背叛");
bookList.add("天幕红尘");
bookList.add("人生");
bookList.add("平凡的世界");

List<String> luyaoBookList = bookList.subList(3, 5);
System.out.println(bookList);
System.out.println(luyaoBookList);
```



运行结果如下图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946287.gif)

从运行结果可以看出，subList返回的是bookList中索引从fromIndex（包含）到toIndex（不包含）的元素集合。

 

### 注意点

使用起来很简单，也很好理解，不过还是有以下几点要注意，否则会造成程序错误或者异常：

- 修改原集合元素的值，会影响子集合；

- 修改原集合的结构，会引起 ConcurrentModificationException 异常；

- 修改子集合元素的值，会影响原集合；

- 修改子集合的结构，会影响原集合；

 

以上几点在《阿里巴巴Java开发手册》泰山版中是这样描述的：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946313.gif)

#### 修改原集合的值，会影响子集合

比如，修改下原集合bookList中某一元素的值（**非结构性修改**）：

```java
List<String> bookList = new ArrayList<>();
bookList.add("遥远的救世主");
bookList.add("背叛");
bookList.add("天幕红尘");
bookList.add("人生");
bookList.add("平凡的世界");

List<String> luyaoBookList = bookList.subList(3, 5);
System.out.println(bookList);
System.out.println(luyaoBookList);
// 修改原集合的值
bookList.set(3,"路遥-人生");
System.out.println(bookList);
System.out.println(luyaoBookList);
```



运行结果如下所示： 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946342.gif)

可以看出，虽然只是修改了原集合bookList的值，但是影响到了子集合luyaoBookList。

#### 修改原集合的结构，会引起ConcurrentModificationException异常

比如，我们往原集合bookList中添加一个元素（**结构性修改**）：

```java
List<String> bookList = new ArrayList<>();
bookList.add("遥远的救世主");
bookList.add("背叛");
bookList.add("天幕红尘");
bookList.add("人生");
bookList.add("平凡的世界");
List<String> luyaoBookList = bookList.subList(3, 5);
System.out.println(bookList);
System.out.println(luyaoBookList);

// 往原集合中添加元素
bookList.add("早晨从中午开始");
System.out.println(bookList);
System.out.println(luyaoBookList);
```



运行结果如下所示： 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946956.gif)

 可以看出，当我们往原集合中添加了元素（结构性修改）后，在遍历子集合时，发生了ConcurrentModificationException异常。

注意事项：以上异常并不是在添加元素时发生的，而是在添加元素后，遍历子集合时发生的。

 

关于这一点，在《阿里巴巴Java开发手册》泰山版中是这样描述的： 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946013.gif)



#### 修改子集合的值，会影响原集合

比如，我们修改下子集合luyaoBookList中某一元素的值（**非结构性修改**）：

```java
List<String> bookList = new ArrayList<>();
bookList.add("遥远的救世主");
bookList.add("背叛");
bookList.add("天幕红尘");
bookList.add("人生");
bookList.add("平凡的世界");

List<String> luyaoBookList = bookList.subList(3, 5);
System.out.println(bookList);
System.out.println(luyaoBookList);

// 修改子集合的值
luyaoBookList.set(1,"路遥-平凡的世界");
System.out.println(bookList);
System.out.println(luyaoBookList);
```



运行结果如下所示： 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946037.gif)

可以看出，虽然只是修改了子集合luyaoBookList的值，但是影响到了原集合bookList。

#### 修改子集合的结构，会影响原集合

比如，我们往子集合luyaoBookList中添加一个元素（**结构性修改**）：

```java
List<String> bookList = new ArrayList<>();
bookList.add("遥远的救世主");
bookList.add("背叛");
bookList.add("天幕红尘");
bookList.add("人生");
bookList.add("平凡的世界");

List<String> luyaoBookList = bookList.subList(3, 5);
System.out.println(bookList);
System.out.println(luyaoBookList);

// 往子集合中添加元素
luyaoBookList.add("早晨从中午开始");
System.out.println(bookList);
System.out.println(luyaoBookList);
```



运行结果如下所示： 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946072.gif)

可以看出，当我们往子集合中添加了元素（结构性修改）后，影响到了原集合bookList。

 

### 原因分析

首先，我们看下 subList 方法的注释，了解下它的用途： 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946237.gif)

> Returns a view of the portion of this list between the specified {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.

翻译过来意思就是：

返回指定的{@code fromIndex}(包含)和{@code toIndex}(排除)之间的列表部分的视图。

然后，我们看下它的源码：

```java
public List<E> subList(int fromIndex, int toIndex) {
    subListRangeCheck(fromIndex, toIndex, size);
    return new SubList<>(this, fromIndex, toIndex);
}
```



可以看到，它调用了SubList类的构造函数，该构造函数的源码如下图所示： 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946494.gif)

可以看出，SubList类是ArrayList的内部类，该构造函数中也并没有重新创建一个新的ArrayList，所以修改原集合或者子集合的元素的值，是会相互影响的。

### 总结

ArrayList的subList方法，返回的是原集合的一个子集合（视图），非结构性修改任意一个集合的元素的值，都会彼此影响，结构性修改原集合时，会报ConcurrentModificationException异常，结构性修改子集合时，会影响原集合，所以使用时要注意，避免程序错误或者异常

 

##  addAll() 方法

注意点：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946550.gif)

 

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946059.gif)

 

##  Collections.emptyList()原理

很多大佬写代码的时候，结果集为空的情况，他返回的不是null，而是:

```java
return Collections.EMPTY_LIST;
```

我们都知道返回null，很有可能造成空指针异常，可以使用emptyList或EMPTY_LIST就可以避免这个问题，除非你想捕获这个为空的信息.

好处：

**1.** new ArrayList()创建时有初始大小，占用内存，emptyList()不用创建一个新的对象，可以减少内存开销；
**2.** 方法返回一个emptyList()时，不会报空指针异常，如果直接返回Null，没有进行非空判断就会报空指针异常；



但是使用emptyList空的方法返回空集合的时候要注意，这个空集合是不可变的。原因是此List与常用的List不同，它是Collections类里的静态内部类，在继承AbstractList后并没有实现add()、remove()等方法，所以返回的List**不能**进行**增加**和**删除**元素操作。











 


<!-- @include: @article-footer.snippet.md -->     









