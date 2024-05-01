---
title: 集合采坑记录
category: Java
tag:
 - 集合
---





## Arrays.asList

### 可能会踩的坑

先来看下 Arrays.asList 的使用：

```java
List<Integer> statusList = Arrays.asList(1, 2);
System.out.println(statusList);
System.out.println(statusList.contains(1));
System.out.println(statusList.contains(3));
```

输出结果如下图所示： 

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946311.gif)

然后，往statusList中添加元素3，如下所示：

```java
statusList.add(3);
System.out.println(statusList.contains(3));
```

预期的结果，应该是输出true，但是实际却是抛出了 java.lang.UnsupportedOperationException 异常： 

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946348.gif)

不禁疑问，只是简单添加个元素，为啥会抛这么个异常呢，不科学啊。

### 原因分析

带着这个疑问，我们看下Arrays类提供的静态方法asList的源码： 

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946370.gif)

返回的是ArrayList，很熟悉，但是再细心一看，就会发现这个ArrayList并不是我们经常使用的ArrayList，因为我们平时经常使用的ArrayList是位于java.util包下的： 

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946362.gif)

 

但是此处的ArrayList却是Arrays类的内部类：

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946355.gif)

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946378.gif)

它也继承了 java.util.AbstractList 类，重写了很多方法，比如我们使用的contains方法，但是却没有重写add方法，最终是调用了父类的add(int, E)方法，所以我们在调用add方法时才会抛出java.lang.UnsupportedOperationException异常。

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946976.gif)

关于这一点，在《阿里巴巴Java开发手册》泰山版中，也有提及：

使用工具类 Arrays.asList()把数组转换成集合时，不能使用其修改集合相关的方法，它的 add/remove/clear 方法会抛出 UnsupportedOperationException 异常。 

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946029.gif)

所以大家在使用Arrays.asList时还是要注意下，避免踩坑。

 

但是set方法是可以的，因为这个类也重写了set方法

```java
list.set(1,3);
System.out.println(list.contains(3));
```



 

### 总结

Arrays.asList方法可以在一些简单的场合使用，比如快速声明一个集合，判断某个值是否在允许的范围内： 

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946265.gif)

但声明后不要再调用 add/remove/clear 等方法修改集合，否则会报java.lang.UnsupportedOperationException异常。

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

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946287.gif)

从运行结果可以看出，subList返回的是bookList中索引从fromIndex（包含）到toIndex（不包含）的元素集合。

 

### 注意点

使用起来很简单，也很好理解，不过还是有以下几点要注意，否则会造成程序错误或者异常：

- 修改原集合元素的值，会影响子集合；

- 修改原集合的结构，会引起 ConcurrentModificationException 异常；

- 修改子集合元素的值，会影响原集合；

- 修改子集合的结构，会影响原集合；

 

以上几点在《阿里巴巴Java开发手册》泰山版中是这样描述的：

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946313.gif)

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

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946342.gif)

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

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946956.gif)

 可以看出，当我们往原集合中添加了元素（结构性修改）后，在遍历子集合时，发生了ConcurrentModificationException异常。

注意事项：以上异常并不是在添加元素时发生的，而是在添加元素后，遍历子集合时发生的。

 

关于这一点，在《阿里巴巴Java开发手册》泰山版中是这样描述的： 

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946013.gif)

####  

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

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946037.gif)

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

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946072.gif)

可以看出，当我们往子集合中添加了元素（结构性修改）后，影响到了原集合bookList。

 

### 原因分析

首先，我们看下 subList 方法的注释，了解下它的用途： 

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946237.gif)

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

![stickPicture.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946494.gif)

可以看出，SubList类是ArrayList的内部类，该构造函数中也并没有重新创建一个新的ArrayList，所以修改原集合或者子集合的元素的值，是会相互影响的。

### 总结

ArrayList的subList方法，返回的是原集合的一个子集合（视图），非结构性修改任意一个集合的元素的值，都会彼此影响，结构性修改原集合时，会报ConcurrentModificationException异常，结构性修改子集合时，会影响原集合，所以使用时要注意，避免程序错误或者异常

 

##  addAll() 方法

注意点：

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946550.gif)

 

![image.png](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250946059.gif)

 

 

 

 

 

 

 

 

 



