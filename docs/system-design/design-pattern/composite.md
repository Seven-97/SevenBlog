---
title: 组合（整体）模式
category: 系统设计
tags:
  - 设计模式
---





## 概述

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271758612.png)

 对于这个图片肯定会非常熟悉，上图我们可以看做是一个文件系统，对于这样的结构我们称之为树形结构。在树形结构中可以通过调用某个方法来遍历整个树，当我们找到某个叶子节点后，就可以对叶子节点进行相关的操作。可以将这颗树理解成一个大的容器，容器里面包含很多的成员对象，这些成员对象可以容器对象也可以是叶子对象。但是由于容器对象和叶子对象在功能上面的区别，使得我们在使用的过程中必须要区分容器对象和叶子对象，但是这样就会给客户带来不必要的麻烦，作为客户而已，它始终希望能够一致的对待容器对象和叶子对象。

**定义：**

 又名部分整体模式，是用于把一组相似的对象当作一个单一的对象。组合模式依据树形结构来组合对象，用来表示部分以及整体层次。这种类型的设计模式属于结构型模式，它创建了对象组的树形结构。

## 结构

组合模式主要包含三种角色：

- 抽象根节点（Component）：定义系统各层次对象的共有方法和属性，可以预先定义一些默认行为和属性。

- 树枝节点（Composite）：定义树枝节点的行为，存储子节点，组合树枝节点和叶子节点形成一个树形结构。

- 叶子节点（Leaf）：叶子节点对象，其下再无分支，是系统层次遍历的最小单位。

##  案例实现

【例】软件菜单

如下图，我们在访问别的一些管理系统时，经常可以看到类似的菜单。一个菜单可以包含菜单项（菜单项是指不再包含其他内容的菜单条目），也可以包含带有其他菜单项的菜单，因此使用组合模式描述菜单就很恰当，我们的需求是针对一个菜单，打印出其包含的所有菜单以及菜单项的名称。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271758406.png)

要实现该案例，先画出类图：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271758772.png)

**代码实现：**

不管是菜单还是菜单项，都应该继承自统一的接口，这里姑且将这个统一的接口称为菜单组件。

```java
//菜单组件  不管是菜单还是菜单项，都应该继承该类
public abstract class MenuComponent {

    protected String name;
    protected int level;

    //添加菜单
    public void add(MenuComponent menuComponent){
        throw new UnsupportedOperationException();
    }

    //移除菜单
    public void remove(MenuComponent menuComponent){
        throw new UnsupportedOperationException();
    }

    //获取指定的子菜单
    public MenuComponent getChild(int i){
        throw new UnsupportedOperationException();
    }

    //获取菜单名称
    public String getName(){
        return name;
    }

    public void print(){
        throw new UnsupportedOperationException();
    }
}
```



这里的MenuComponent定义为抽象类，因为有一些共有的属性和行为要在该类中实现，Menu和MenuItem类就可以只覆盖自己感兴趣的方法，而不用搭理不需要或者不感兴趣的方法，举例来说，Menu类可以包含子菜单，因此需要覆盖add()、remove()、getChild()方法，但是MenuItem就不应该有这些方法。这里给出的默认实现是抛出异常，你也可以根据自己的需要改写默认实现。

```java
public class Menu extends MenuComponent {

    private List<MenuComponent> menuComponentList;

    public Menu(String name,int level){
        this.level = level;
        this.name = name;
        menuComponentList = new ArrayList<MenuComponent>();
    }

    @Override
    public void add(MenuComponent menuComponent) {
        menuComponentList.add(menuComponent);
    }

    @Override
    public void remove(MenuComponent menuComponent) {
        menuComponentList.remove(menuComponent);
    }

    @Override
    public MenuComponent getChild(int i) {
        return menuComponentList.get(i);
    }

    @Override
    public void print() {

        for (int i = 1; i < level; i++) {
            System.out.print("--");
        }
        System.out.println(name);
        for (MenuComponent menuComponent : menuComponentList) {
            menuComponent.print();
        }
    }
}
```



Menu类已经实现了除了getName方法的其他所有方法，因为Menu类具有添加菜单，移除菜单和获取子菜单的功能。

```java
public class MenuItem extends MenuComponent {

    public MenuItem(String name,int level) {
        this.name = name;
        this.level = level;
    }

    @Override
    public void print() {
        for (int i = 1; i < level; i++) {
            System.out.print("--");
        }
        System.out.println(name);
    }
}
```

MenuItem是菜单项，不能再有子菜单，所以添加菜单，移除菜单和获取子菜单的功能并不能实现。

## 组合模式的分类

在使用组合模式时，根据抽象构件类的定义形式，我们可将组合模式分为透明组合模式和安全组合模式两种形式。

- 透明组合模式

透明组合模式中，抽象根节点角色中声明了所有用于管理成员对象的方法，比如在示例中 MenuComponent 声明了 add、remove 、getChild 方法，这样做的好处是确保所有的构件类都有相同的接口。透明组合模式也是组合模式的标准形式。

透明组合模式的缺点是不够安全，因为叶子对象和容器对象在本质上是有区别的，叶子对象不可能有下一个层次的对象，即不可能包含成员对象，因此为其提供 add()、remove() 等方法是没有意义的，这在编译阶段不会出错，但在运行阶段如果调用这些方法可能会出错（如果没有提供相应的错误处理代码）

- 安全组合模式

在安全组合模式中，在抽象构件角色中没有声明任何用于管理成员对象的方法，而是在树枝节点 Menu 类中声明并实现这些方法。安全组合模式的缺点是不够透明，因为叶子构件和容器构件具有不同的方法，且容器构件中那些用于管理成员对象的方法没有在抽象构件类中定义，因此客户端不能完全针对抽象编程，必须有区别地对待叶子构件和容器构件。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271759610.png)

## 优缺点

优点：
- 组合模式可以清楚地定义分层次的复杂对象，表示对象的全部或部分层次，它让客户端忽略了层次的差异，方便对整个层次结构进行控制。
- 客户端可以一致地使用一个组合结构或其中单个对象，不必关心处理的是单个对象还是整个组合结构，简化了客户端代码。
- 在组合模式中增加新的树枝节点和叶子节点都很方便，无须对现有类库进行任何修改，符合“开闭原则”。
- 组合模式为树形结构的面向对象实现提供了一种灵活的解决方案，通过叶子节点和树枝节点的递归组合，可以形成复杂的树形结构，但对树形结构的控制却非常简单。


## 使用场景

组合模式正是应树形结构而生，所以组合模式的使用场景就是出现树形结构的地方。比如：文件目录显示，多级目录呈现等树形结构数据的操作。


## 源码解析

###  HashMap

HashMap 完美契合了组合模式的核心思想：**将对象组织成树形结构，以表示“部分-整体”的层次关系，使得用户对单个对象（叶子）和组合对象（容器）的使用具有一致性**

核心组件对应关系如下：

- **组件 (Component)**：`Map<K, V>`接口。它定义了所有操作的基本契约，如 `put`, `get`, `putAll`等。这使得客户端可以一致地对待单个元素和整个Map。
- **叶子 (Leaf)**：**`HashMap.Node`**​ 类。它实现了 `Map.Entry`接口，代表树形结构中的最小单元（即一个键值对）。叶子节点本身不能再包含其他组件。
- **组合体 (Composite)**：`HashMap`类本身。它实现了 `Map`接口，并内部维护了一个 `Node`类型的数组 (`Node<K,V>[] table`)。这个组合体不仅可以存储叶子节点（`Node`），还能通过链表和红黑树处理更复杂的结构（即包含其他 `Node`），并提供了管理这些子组件的方法（如 `putAll`）。

关键体现在于 `putAll(Map<? extends K, ? extends V> m)`方法。此方法接收另一个 `Map`（可视为一个组合体），并将其所有键值对（叶子节点）添加到当前 `HashMap`中。这使得你可以像操作一个普通对象一样，轻松地合并整个映射表，无需关心其内部有多少个 `Node`


### ArrayList 的“类似”与区别


ArrayList 的 `addAll(Collection<? extends E> c)`方法在**效果上**与 HashMap 的 `putAll`类似，都能将另一个集合的全部内容添加到当前集合中，提供了操作上的一致性。

但它们的**本质区别**在于：

- **结构差异**：HashMap 的 `Node`之间通过 `next`引用相互关联，形成了一个内在的、非线性的数据结构（链表或树）。而 ArrayList 中的元素是相互独立的，只是被顺序存储在了一个线性数组中。ArrayList 的结构不具备组合模式所要求的“部分-整体”的层次性。

- **设计意图**：组合模式旨在管理**递归的树形结构**。ArrayList 的设计目标是提供一个可动态增长的**线性序列**。它的 `addAll`方法更像是一种针对批量操作的优化，而非用于处理层次化的对象结构。


因此，**ArrayList 更准确地说是运用了“组合”的思想（将多个对象集合在一起），但并未采用经典的设计模式中的“组合模式（Composite Pattern）”**。



<!-- @include: @article-footer.snippet.md -->