---
title: 图
category: 计算机基础
tag:
  - 数据结构
head:
  - - meta
    - name: keywords
      content: 数据结构,图
  - - meta
    - name: description
      content: 全网最全的计算机基础（数据结构）知识点总结，让天下没有难学的八股文！
---



## 概述

图是一种较为复杂的非线性结构。 **为啥说其较为复杂呢？**

根据前面的内容，我们知道：

- 线性数据结构的元素满足唯一的线性关系，每个元素(除第一个和最后一个外)只有一个直接前趋和一个直接后继。
- 树形数据结构的元素之间有着明显的层次关系。

但是，图形结构的元素之间的关系是任意的。

**何为图呢？** 简单来说，图就是由顶点的有穷非空集合和顶点之间的边组成的集合。通常表示为：**G(V,E)**，其中，G 表示一个图，V 表示顶点的集合，E 表示边的集合。

下图所展示的就是图这种数据结构

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202502221807527.png)

同时图⼜分为有向图与⽆向图，上⾯的是⽆向图，因为边没有指明⽅向，只是表示两者关联关系，⽽有向图则是这样：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202502221808611.png)

图在我们日常生活中的例子很多！比如我们在社交软件上好友关系就可以用图来表示。

## 图的基本概念

### 顶点

图中的数据元素，我们称之为顶点，图至少有一个顶点（非空有穷集合）

对应到好友关系图，每一个用户就代表一个顶点。

### 边

顶点之间的关系用边表示。

对应到好友关系图，两个用户是好友的话，那两者之间就存在一条边。

### 度

度表示一个顶点包含多少条边，在有向图中，还分为出度和入度，出度表示从该顶点出去的边的条数，入度表示进入该顶点的边的条数。

对应到好友关系图，度就代表了某个人的好友数量。

### 无向图和有向图

边表示的是顶点之间的关系，有的关系是双向的，比如同学关系，A 是 B 的同学，那么 B 也肯定是 A 的同学，那么在表示 A 和 B 的关系时，就不用关注方向，用不带箭头的边表示，这样的图就是无向图。

有的关系是有方向的，比如父子关系，师生关系，微博的关注关系，A 是 B 的爸爸，但 B 肯定不是 A 的爸爸，A 关注 B，B 不一定关注 A。在这种情况下，我们就用带箭头的边表示二者的关系，这样的图就是有向图。

### 无权图和带权图

对于一个关系，如果我们只关心关系的有无，而不关心关系有多强，那么就可以用无权图表示二者的关系。

对于一个关系，如果我们既关心关系的有无，也关心关系的强度，比如描述地图上两个城市的关系，需要用到距离，那么就用带权图来表示，带权图中的每一条边一个数值表示权值，代表关系的强度。

下图就是一个带权有向图。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270853799.png)

## 图的存储

### 邻接矩阵存储

邻接矩阵将图用二维矩阵存储，是一种较为直观的表示方式。

如果第 i 个顶点和第 j 个顶点之间有关系，且关系权值为 n，则 `A[i][j]=n` 。

在无向图中，我们只关心关系的有无，所以当顶点 i 和顶点 j 有关系时，`A[i][j]`=1，当顶点 i 和顶点 j 没有关系时，`A[i][j]`=0。如下图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270853041.png)

值得注意的是：**无向图的邻接矩阵是一个对称矩阵，因为在无向图中，顶点 i 和顶点 j 有关系，则顶点 j 和顶点 i 必有关系。**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270853683.png)

邻接矩阵存储的方式优点是简单直接（直接使用一个二维数组即可），并且，在获取两个定点之间的关系的时候也非常高效（直接获取指定位置的数组元素的值即可）。但是，这种存储方式的缺点也比较明显，那就是比较浪费空间，

### 邻接表存储

针对上面邻接矩阵比较浪费内存空间的问题，诞生了图的另外一种存储方法—**邻接表** 。

邻接链表使用一个链表来存储某个顶点的所有后继相邻顶点。对于图中每个顶点 Vi，把所有邻接于 Vi 的顶点 Vj 链成一个单链表，这个单链表称为顶点 Vi 的 **邻接表**。如下图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270853405.png)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270853521.png)

大家可以数一数邻接表中所存储的元素的个数以及图中边的条数，你会发现：

- 在无向图中，邻接表元素个数等于边的条数的两倍，如左图所示的无向图中，边的条数为 7，邻接表存储的元素个数为 14。
- 在有向图中，邻接表元素个数等于边的条数，如右图所示的有向图中，边的条数为 8，邻接表存储的元素个数为 8。

## 图的搜索

图⾥⾯遍历⼀般分为⼴度优先遍历和深度优先遍历，⼴度优先遍历是指优先遍历与当前顶点直接相关的顶点，⼀般借助队列实现。⽽深度优先遍历则是往⼀个⽅向⼀直⾛到不能再⾛，有点不撞南墙不回头的意思，⼀般使⽤递归实现。

### 广度优先搜索

广度优先搜索就像水面上的波纹一样一层一层向外扩展，如下图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270853909.png)

**广度优先搜索的具体实现方式用到了之前所学过的线性数据结构——队列** 。具体过程如下图所示：

**第 1 步：**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270853701.png)

**第 2 步：**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270854594.png)

**第 3 步：**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270854159.png)

**第 4 步：**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270854209.png)

**第 5 步：**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270854035.png)

**第 6 步：**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270854481.png)





### 深度优先搜索

深度优先搜索就是“一条路走到黑”，从源顶点开始，一直走到没有后继节点，才回溯到上一顶点，然后继续“一条路走到黑”，如下图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270854166.png)

**和广度优先搜索类似，深度优先搜索的具体实现用到了另一种线性数据结构——栈** 。具体过程如下图所示：

**第 1 步：**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270854647.png)

**第 2 步：**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270854744.png)

**第 3 步：**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270854629.png)

**第 4 步：**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270854323.png)

**第 5 步：**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270854026.png)

**第 6 步：**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270854390.png)



算法框架如下：

```java
// 记录被遍历过的节点
boolean[] visited;
// 记录从起点到当前节点的路径
boolean[] onPath;

/* 图遍历框架 */
void traverse(Graph graph, int s) {
    if (visited[s]) return;
    // 经过节点 s，标记为已遍历
    visited[s] = true;
    // 做选择：标记节点 s 在路径上
    onPath[s] = true;
    for (int neighbor : graph.neighbors(s)) {
        traverse(graph, neighbor);
    }
    // 撤销选择：节点 s 离开路径
    onPath[s] = false;
}
```







## 图的经典算法

建图函数

```java
List<Integer>[] buildGraph(int numCourses, int[][] prerequisites) {
    // 图中共有 numCourses 个节点
    List<Integer>[] graph = new LinkedList[numCourses];
    for (int i = 0; i < numCourses; i++) {
        graph[i] = new LinkedList<>();
    }
    for (int[] edge : prerequisites) {
        int from = edge[1], to = edge[0];
        // 添加一条从 from 指向 to 的有向边
        // 边的方向是「被依赖」关系，即修完课程 from 才能修课程 to
        graph[from].add(to);
    }
    return graph;
}
```



### 环检测算法

#### DFS

```java
// 记录一次递归堆栈中的节点
boolean[] onPath;
// 记录遍历过的节点，防止走回头路
boolean[] visited;
// 记录图中是否有环
boolean hasCycle = false;

boolean canFinish(int numCourses, int[][] prerequisites) {
    List<Integer>[] graph = buildGraph(numCourses, prerequisites);

    visited = new boolean[numCourses];
    onPath = new boolean[numCourses];

    for (int i = 0; i < numCourses; i++) {
        // 遍历图中的所有节点
        traverse(graph, i);
    }
    // 只要没有循环依赖可以完成所有课程
    return !hasCycle;
}

void traverse(List<Integer>[] graph, int s) {
    if (onPath[s]) {
        // 出现环
        hasCycle = true;
    }

    if (visited[s] || hasCycle) {
        // 如果已经找到了环，也不用再遍历了
        return;
    }
    // 前序代码位置
    visited[s] = true;
    onPath[s] = true;
    for (int t : graph[s]) {
        traverse(graph, t);
    }
    // 后序代码位置
    onPath[s] = false;
}
```



#### BFS

```java
// 主函数
public boolean canFinish(int numCourses, int[][] prerequisites) {
    // 建图，有向边代表「被依赖」关系
    List<Integer>[] graph = buildGraph(numCourses, prerequisites);
    // 构建入度数组
    int[] indgree = new int[numCourses];
    for (int[] edge : prerequisites) {
        int from = edge[1], to = edge[0];
        // 节点 to 的入度加一
        indgree[to]++;
    }

    // 根据入度初始化队列中的节点
    Queue<Integer> q = new LinkedList<>();
    for (int i = 0; i < numCourses; i++) {
        if (indgree[i] == 0) {
            // 节点 i 没有入度，即没有依赖的节点
            // 可以作为拓扑排序的起点，加入队列
            q.offer(i);
        }
    }

    // 记录遍历的节点个数
    int count = 0;
    // 开始执行 BFS 循环
    while (!q.isEmpty()) {
        // 弹出节点 cur，并将它指向的节点的入度减一
        int cur = q.poll();
        count++;
        for (int next : graph[cur]) {
            indgree[next]--;
            if (indgree[next] == 0) {
                // 如果入度变为 0，说明 next 依赖的节点都已被遍历
                q.offer(next);
            }
        }
    }

    // 如果所有节点都被遍历过，说明不成环
    return count == numCourses;
}
```

这段 BFS 算法的思路：

1、构建邻接表，和之前一样，边的方向表示「被依赖」关系。

2、构建一个 `indegree` 数组记录每个节点的入度，即 `indegree[i]` 记录节点 `i` 的入度。

3、对 BFS 队列进行初始化，将入度为 0 的节点首先装入队列。

**4、开始执行 BFS 循环，不断弹出队列中的节点，减少相邻节点的入度，并将入度变为 0 的节点加入队列**。

**5、如果最终所有节点都被遍历过（`count` 等于节点数），则说明不存在环，反之则说明存在环**。



### 拓扑排序算法

对一个有向无环图(Directed Acyclic Graph简称DAG)G进行拓扑排序，是将G中所有顶点排成一个线性序列，使得图中任意一对顶点u和v，若边(u,v)∈E(G)，则u在线性序列中出现在v之前。通常，这样的线性序列称为满足拓扑次序(Topological Order)的序列，简称拓扑序列。简单的说，由某个集合上的一个偏序得到该集合上的一个全序，这个操作称之为拓扑排序。

#### DFS

```java
// 记录后序遍历结果
List<Integer> postorder = new ArrayList<>();
// 记录是否存在环
boolean hasCycle = false;
boolean[] visited, onPath;

// 主函数
public int[] findOrder(int numCourses, int[][] prerequisites) {
    List<Integer>[] graph = buildGraph(numCourses, prerequisites);
    visited = new boolean[numCourses];
    onPath = new boolean[numCourses];
    // 遍历图
    for (int i = 0; i < numCourses; i++) {
        traverse(graph, i);
    }
    // 有环图无法进行拓扑排序
    if (hasCycle) {
        return new int[]{};
    }
    // 逆后序遍历结果即为拓扑排序结果
    Collections.reverse(postorder);
    int[] res = new int[numCourses];
    for (int i = 0; i < numCourses; i++) {
        res[i] = postorder.get(i);
    }
    return res;
}

// 图遍历函数
void traverse(List<Integer>[] graph, int s) {
    if (onPath[s]) {
        // 发现环
        hasCycle = true;
    }
    if (visited[s] || hasCycle) {
        return;
    }
    // 前序遍历位置
    onPath[s] = true;
    visited[s] = true;
    for (int t : graph[s]) {
        traverse(graph, t);
    }
    // 后序遍历位置
    postorder.add(s);
    onPath[s] = false;
}
```

#### BFS

```java
// 主函数
public int[] findOrder(int numCourses, int[][] prerequisites) {
    // 建图，和环检测算法相同
    List<Integer>[] graph = buildGraph(numCourses, prerequisites);
    // 计算入度，和环检测算法相同
    int[] indgree = new int[numCourses];
    for (int[] edge : prerequisites) {
        int from = edge[1], to = edge[0];
        indgree[to]++;
    }

    // 根据入度初始化队列中的节点，和环检测算法相同
    Queue<Integer> q = new LinkedList<>();
    for (int i = 0; i < numCourses; i++) {
        if (indgree[i] == 0) {
            q.offer(i);
        }
    }

    // 记录拓扑排序结果
    int[] res = new int[numCourses];
    // 记录遍历节点的顺序（索引）
    int count = 0;
    // 开始执行 BFS 算法
    while (!q.isEmpty()) {
        int cur = q.poll();
        // 弹出节点的顺序即为拓扑排序结果
        res[count] = cur;
        count++;
        for (int next : graph[cur]) {
            indgree[next]--;
            if (indgree[next] == 0) {
                q.offer(next);
            }
        }
    }

    if (count != numCourses) {
        // 存在环，拓扑排序不存在
        return new int[]{};
    }

    return res;
}
```



### 二分图判定算法

二分图的顶点集可分割为两个互不相交的子集，图中每条边依附的两个顶点都分属于这两个子集，且两个子集内的顶点不相邻。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270956044.png)

**给你一幅「图」，请你用两种颜色将图中的所有顶点着色，且使得任意一条边的两个端点的颜色都不相同，你能做到吗**？

这就是图的「双色问题」，其实这个问题就等同于二分图的判定问题，如果你能够成功地将图染色，那么这幅图就是一幅二分图，反之则不是

#### DFS

```java
// 记录图是否符合二分图性质
private boolean ok = true;
// 记录图中节点的颜色，false 和 true 代表两种不同颜色
private boolean[] color;
// 记录图中节点是否被访问过
private boolean[] visited;

// 主函数，输入邻接表，判断是否是二分图
public boolean isBipartite(int[][] graph) {
    int n = graph.length;
    color =  new boolean[n];
    visited =  new boolean[n];
    // 因为图不一定是联通的，可能存在多个子图
    // 所以要把每个节点都作为起点进行一次遍历
    // 如果发现任何一个子图不是二分图，整幅图都不算二分图
    for (int v = 0; v < n; v++) {
        if (!visited[v]) {
            traverse(graph, v);
        }
    }
    return ok;
}

// DFS 遍历框架
private void traverse(int[][] graph, int v) {
    // 如果已经确定不是二分图了，就不用浪费时间再递归遍历了
    if (!ok) return;

    visited[v] = true;
    for (int w : graph[v]) {
        if (!visited[w]) {
            // 相邻节点 w 没有被访问过
            // 那么应该给节点 w 涂上和节点 v 不同的颜色
            color[w] = !color[v];
            // 继续遍历 w
            traverse(graph, w);
        } else {
            // 相邻节点 w 已经被访问过
            // 根据 v 和 w 的颜色判断是否是二分图
            if (color[w] == color[v]) {
                // 若相同，则此图不是二分图
                ok = false;
            }
        }
    }
}
```

#### BFS

```java
// 记录图是否符合二分图性质
private boolean ok = true;
// 记录图中节点的颜色，false 和 true 代表两种不同颜色
private boolean[] color;
// 记录图中节点是否被访问过
private boolean[] visited;

public boolean isBipartite(int[][] graph) {
    int n = graph.length;
    color =  new boolean[n];
    visited =  new boolean[n];
    
    for (int v = 0; v < n; v++) {
        if (!visited[v]) {
            // 改为使用 BFS 函数
            bfs(graph, v);
        }
    }
    
    return ok;
}

// 从 start 节点开始进行 BFS 遍历
private void bfs(int[][] graph, int start) {
    Queue<Integer> q = new LinkedList<>();
    visited[start] = true;
    q.offer(start);
    
    while (!q.isEmpty() && ok) {
        int v = q.poll();
        // 从节点 v 向所有相邻节点扩散
        for (int w : graph[v]) {
            if (!visited[w]) {
                // 相邻节点 w 没有被访问过
                // 那么应该给节点 w 涂上和节点 v 不同的颜色
                color[w] = !color[v];
                // 标记 w 节点，并放入队列
                visited[w] = true;
                q.offer(w);
            } else {
                // 相邻节点 w 已经被访问过
                // 根据 v 和 w 的颜色判断是否是二分图
                if (color[w] == color[v]) {
                    // 若相同，则此图不是二分图
                    ok = false;
                }
            }
        }
    }
}
```



### Union-Find并查集

大白话就是当我们需要判断两个元素是否在同一个集合里的时候，我们就要想到用并查集。

并查集主要有两个功能：

- 将两个元素添加到一个集合中。
- 判断两个元素在不在同一个集合

名称"并查集"直接体现了它的核心功能：合并集合与查询元素所属集合。在英文中，它通常被称为"Union-Find"数据结构或"Disjoint-Set"数据结构。

并查集的基本思想是使用树形结构来表示每个集合，树的根节点作为集合的代表元素。

并查集核心特性：
1. 快速查找：能够快速判断两个元素是否属于同一集合
2. 快速合并：能够快速将两个集合合并为一个
3. 路径压缩：优化查找操作，使树的高度尽量小
4. 按秩合并：优化合并操作，减少树的高度增长

Union-Find 算法主要需要实现这两个 API：

```java
class UF {
    /* 将 p 和 q 连接 */
    public void union(int p, int q);
    /* 判断 p 和 q 是否连通 */
    public boolean connected(int p, int q);
    /* 返回图中有多少个连通分量 */
    public int count();
}
```

这里所说的「连通」是一种等价关系，也就是说具有如下三个性质：

1、自反性：节点`p`和`p`是连通的。

2、对称性：如果节点`p`和`q`连通，那么`q`和`p`也连通。

3、传递性：如果节点`p`和`q`连通，`q`和`r`连通，那么`p`和`r`也连通。

比如说有一幅图，0～9 任意两个**不同**的点都不连通，调用`connected`都会返回 false，连通分量为 10 个。

如果现在调用`union(0, 1)`，那么 0 和 1 被连通，连通分量降为 9 个。

再调用`union(1, 2)`，这时 0,1,2 都被连通，调用`connected(0, 2)`也会返回 true，连通分量变为 8 个。

#### 基础算法

设定树的每个节点有一个指针指向其父节点，如果是根节点的话，这个指针指向自己。比如说刚才那幅 10 个节点的图，一开始的时候没有相互连通，就是这样：

```java
class UF {
    // 记录连通分量
    private int count;
    // 节点 x 的节点是 parent[x]
    private int[] parent;

    /* 构造函数，n 为图的节点总数 */
    public UF(int n) {
        // 一开始互不连通
        this.count = n;
        // 父节点指针初始指向自己
        parent = new int[n];
        for (int i = 0; i < n; i++)
            parent[i] = i;
    }

    /* 其他函数 */
}
```

**如果某两个节点被连通，则让其中的（任意）一个节点的根节点接到另一个节点的根节点上**：

```java
public void union(int p, int q) {
    int rootP = find(p);
    int rootQ = find(q);
    if (rootP == rootQ)
        return;
    // 将两棵树合并为一棵
    parent[rootP] = rootQ;
    // parent[rootQ] = rootP 也一样
    count--; // 两个分量合二为一
}

/* 返回某个节点 x 的根节点 */
private int find(int x) {
    // 根节点的 parent[x] == x
    while (parent[x] != x)
        x = parent[x];
    return x;
}

/* 返回当前的连通分量个数 */
public int count() { 
    return count;
}
```

**这样，如果节点`p`和`q`连通的话，它们一定拥有相同的根节点**：

```java
public boolean connected(int p, int q) {
    int rootP = find(p);
    int rootQ = find(q);
    return rootP == rootQ;
}
```

至此，Union-Find 算法就基本完成了。

那么这个算法的复杂度是多少呢？我们发现，主要 API`connected`和`union`中的复杂度都是`find`函数造成的，所以说它们的复杂度和`find`一样。

`find`主要功能就是从某个节点向上遍历到树根，其时间复杂度就是树的高度。我们可能习惯性地认为树的高度就是`logN`，但这并不一定。`logN`的高度只存在于平衡二叉树，对于一般的树可能出现极端不平衡的情况，使得「树」几乎退化成「链表」，树的高度最坏情况下可能变成`N`。

所以说上面这种解法，`find`,`union`,`connected`的时间复杂度都是 O(N)。这个复杂度很不理想的，你想图论解决的都是诸如社交网络这样数据规模巨大的问题，对于`union`和`connected`的调用非常频繁，每次调用需要线性时间完全不可忍受。

#### 平衡性优化

要知道哪种情况下可能出现不平衡现象，关键在于`union`过程：

```java
public void union(int p, int q) {
    int rootP = find(p);
    int rootQ = find(q);
    if (rootP == rootQ)
        return;
    // 将两棵树合并为一棵
    parent[rootP] = rootQ;
    // parent[rootQ] = rootP 也可以
    count--; 
}
```

我们一开始就是简单粗暴的把`p`所在的树接到`q`所在的树的根节点下面，那么这里就可能出现「头重脚轻」的不平衡状况，比如下面这种局面：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271016400.png)

长此以往，树可能生长得很不平衡。**我们其实是希望，小一些的树接到大一些的树下面，这样就能避免头重脚轻，更平衡一些**。解决方法是额外使用一个`size`数组，记录每棵树包含的节点数，我们不妨称为「重量」：

```java
class UF {
    private int count;
    private int[] parent;
    // 新增一个数组记录树的“重量”
    private int[] size;

    public UF(int n) {
        this.count = n;
        parent = new int[n];
        // 最初每棵树只有一个节点
        // 重量应该初始化 1
        size = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }
    /* 其他函数 */
}
```

比如说`size[3] = 5`表示，以节点`3`为根的那棵树，总共有`5`个节点。这样我们可以修改一下`union`方法：

```java
public void union(int p, int q) {
    int rootP = find(p);
    int rootQ = find(q);
    if (rootP == rootQ)
        return;

    // 小树接到大树下面，较平衡
    if (size[rootP] > size[rootQ]) {
        parent[rootQ] = rootP;
        size[rootP] += size[rootQ];
    } else {
        parent[rootP] = rootQ;
        size[rootQ] += size[rootP];
    }
    count--;
}
```

这样，通过比较树的重量，就可以保证树的生长相对平衡，树的高度大致在`logN`这个数量级，极大提升执行效率。

此时，`find`,`union`,`connected`的时间复杂度都下降为 O(logN)，即便数据规模上亿，所需时间也非常少。

#### 路径压缩

**其实我们并不在乎每棵树的结构长什么样，只在乎根节点**。

因为无论树长啥样，树上的每个节点的根节点都是相同的，所以能不能进一步压缩每棵树的高度，使树高始终保持为常数？

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271018216.png)



如图所示，这样每个节点的父节点就是整棵树的根节点，`find`就能以 O(1) 的时间找到某一节点的根节点，相应的，`connected`和`union`复杂度都下降为 O(1)。

要做到这一点主要是修改`find`函数逻辑，非常简单，但你可能会看到两种不同的写法。

第一种是在`find`中加一行代码：

```java
private int find(int x) {
    while (parent[x] != x) {
        // 这行代码进行路径压缩
        parent[x] = parent[parent[x]];
        x = parent[x];
    }
    return x;
}
```

用语言描述就是，每次 while 循环都会把一对儿父子节点改到同一层，这样每次调用`find`函数向树根遍历的同时，顺手就将树高缩短了。

路径压缩的第二种写法是这样：

```java
// 第二种路径压缩的 find 方法
public int find(int x) {
    if (parent[x] != x) {
        parent[x] = find(parent[x]);
    }
    return parent[x];
}
```

这个递归过程有点不好理解，你可以自己手画一下递归过程。我把这个函数做的事情翻译成迭代形式，方便你理解它进行路径压缩的原理：

```java
// 这段迭代代码方便你理解递归代码所做的事情
public int find(int x) {
    // 先找到根节点
    int root = x;
    while (parent[root] != root) {
        root = parent[root];
    }
    // 然后把 x 到根节点之间的所有节点直接接到根节点下面
    int old_parent = parent[x];
    while (x != root) {
        parent[x] = root;
        x = old_parent;
        old_parent = parent[old_parent];
    }
    return root;
}
```

这种路径压缩的效果如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271020276.png)

比起第一种路径压缩，显然这种方法压缩得更彻底，直接把一整条树枝压平，一点意外都没有。就算一些极端情况下产生了一棵比较高的树，只要一次路径压缩就能大幅降低树高，从 [摊还分析](https://cloud.tencent.com/developer/tools/blog-entry?target=https%3A%2F%2Fmp.weixin.qq.com%2Fs%3F__biz%3DMzAxODQxMDM0Mw%3D%3D%26mid%3D2247496738%26idx%3D1%26sn%3D2c7d16c8b0ee64d8101abb35e06b08cc%26scene%3D21%23wechat_redirect&source=article&objectId=2190285) 的角度来看，所有操作的平均时间复杂度依然是 O(1)，所以从效率的角度来说，推荐你使用这种路径压缩算法。

**另外，如果使用路径压缩技巧，那么`size`数组的平衡优化就不是特别必要了**。所以你一般看到的 Union Find 算法应该是如下实现：

```java
class UF {
    // 连通分量个数
    private int count;
    // 存储每个节点的父节点
    private int[] parent;

    // n 为图中节点的个数
    public UF(int n) {
        this.count = n;
        parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
    }

    // 将节点 p 和节点 q 连通
    public void union(int p, int q) {
        int rootP = find(p);
        int rootQ = find(q);

        if (rootP == rootQ)
            return;

        parent[rootQ] = rootP;
        // 两个连通分量合并成一个连通分量
        count--;
    }

    // 判断节点 p 和节点 q 是否连通
    public boolean connected(int p, int q) {
        int rootP = find(p);
        int rootQ = find(q);
        return rootP == rootQ;
    }

    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }

    // 返回图中的连通分量个数
    public int count() {
        return count;
    }
}
```



Union-Find 算法的复杂度可以这样分析：构造函数初始化数据结构需要 O(N) 的时间和空间复杂度；连通两个节点`union`、判断两个节点的连通性`connected`、计算连通分量`count`所需的时间复杂度均为 O(1)。

到这里，相信你已经掌握了 Union-Find 算法的核心逻辑，总结一下我们优化算法的过程：

1、用`parent`数组记录每个节点的父节点，相当于指向父节点的指针，所以`parent`数组内实际存储着一个森林（若干棵多叉树）。

2、用`size`数组记录着每棵树的重量，目的是让`union`后树依然拥有平衡性，保证各个 API 时间复杂度为 O(logN)，而不会退化成链表影响操作效率。

3、在`find`函数中进行路径压缩，保证任意树的高度保持在常数，使得各个 API 时间复杂度为 O(1)。使用了路径压缩之后，可以不使用`size`数组的平衡优化。

#### 优点

1. 查找和合并操作的平均时间复杂度接近O(1)
2. 实现简单，易于理解
3. 空间复杂度低，只需要两个数组
4. 适用于处理大量动态连通性问题

#### 缺点

1. 不支持分裂操作（将一个集合分成两个）
2. 不方便查询集合中的所有元素
3. 在某些特殊情况下，性能可能退化

#### 应用场景

Kruskal最小生成树算法：在Kruskal算法中，并查集是核心数据结构。该算法按权重从小到大遍历边，使用并查集判断加入某条边是否会形成环，从而高效构建最小生成树。

网络连通性问题：并查集可高效解决动态连通性问题，比如判断网络中两个节点是否连通、社交网络中用户间的关系连接等。当关系变化时，只需执行简单的union操作，判断连通性时使用find操作即可。

等价类划分：在编译器设计、电路分析等领域，并查集可用于等价类识别与合并。当系统发现两个元素等价时执行union操作，需要判断等价关系时使用find操作，这种动态维护等价关系的能力正是并查集的优势所在。

判断无向图中的环：当向无向图中添加边时，如果边的两个端点已在同一个集合中，则添加这条边会形成环。在很多图算法和网络设计问题中都可以使用这一特性。


### Kruskal 最小生成树算法

Kruskal 的 关键就是 并查集算法



**先说「树」和「图」的根本区别：树不会包含环，图可以包含环**。

如果一幅图没有环，完全可以拉伸成一棵树的模样。说的专业一点，树就是「无环连通图」。

那么什么是图的「生成树」呢，其实按字面意思也好理解，就是在图中找一棵包含图中的所有节点的树。专业点说，生成树是含有图中所有顶点的「无环连通子图」。

容易想到，一幅图可以有很多不同的生成树，比如下面这幅图，红色的边就组成了两棵不同的生成树：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404271025443.webp)

对于加权图，每条边都有权重，所以每棵生成树都有一个权重和。比如上图，右侧生成树的权重和显然比左侧生成树的权重和要小。

**那么最小生成树很好理解了，所有可能的生成树中，权重和最小的那棵生成树就叫「最小生成树」**。

> PS：一般来说，我们都是在**无向加权图**中计算最小生成树的，所以使用最小生成树算法的现实场景中，图的边权重一般代表成本、距离这样的标量。



所谓最小生成树，就是图中若干边的集合（我们后文称这个集合为`mst`，最小生成树的英文缩写），你要保证这些边：

1、包含图中的所有节点。

2、形成的结构是树结构（即不存在环）。

3、权重和最小。

前两条其实可以很容易地利用 Union-Find 算法做到，关键在于第 3 点，如何保证得到的这棵生成树是权重和最小的。

这里就用到了贪心思路：**将所有边按照权重从小到大排序，从权重最小的边开始遍历，如果这条边和`mst`中的其它边不会形成环，则这条边是最小生成树的一部分，将它加入`mst`集合；否则，这条边不是最小生成树的一部分，不要把它加入`mst`集合**。

这样，最后`mst`集合中的边就形成了最小生成树，算法代码如下：

```java
int minimumCost(int n, int[][] connections) {
    // 城市编号为 1...n，所以初始化大小为 n + 1
    UF uf = new UF(n + 1);
    // 对所有边按照权重从小到大排序
    Arrays.sort(connections, (a, b) -> (a[2] - b[2]));
    // 记录最小生成树的权重之和
    int mst = 0;
    for (int[] edge : connections) {
        int u = edge[0];
        int v = edge[1];
        int weight = edge[2];
        // 若这条边会产生环，则不能加入 mst
        if (uf.connected(u, v)) {
            continue;
        }
        // 若这条边不会产生环，则属于最小生成树
        mst += weight;
        uf.union(u, v);
    }
    // 保证所有节点都被连通
    // 按理说 uf.count() == 1 说明所有节点被连通
    // 但因为节点 0 没有被使用，所以 0 会额外占用一个连通分量
    return uf.count() == 2 ? mst : -1;
}

class UF {
    // 见上文并查集代码实现
}
```

复杂度分析：

假设一幅图的节点个数为V，边的条数为E，首先需要O(E)的空间装所有边，而且 Union-Find 算法也需要O(V)的空间，所以 Kruskal 算法总的空间复杂度就是O(V + E)。

时间复杂度主要耗费在排序，需要O(ElogE)的时间，Union-Find 算法所有操作的复杂度都是O(1)，套一个 for 循环也不过是O(E)，所以总的时间复杂度为O(ElogE)。

### PRIM 最小生成树算法

**Prim 算法也使用贪心思想来让生成树的权重尽可能小**，也就是「切分定理」。

**其次，Prim 算法使用** **BFS 算法思想** **和** **visited** **布尔数组避免成环**，来保证选出来的边最终形成的一定是一棵树。

Prim 算法不需要事先对所有边排序，而是利用优先级队列动态实现排序的效果，所以 Prim 算法类似于 Kruskal 的动态过程。

> 切分定理：对于任意一种「切分」，其中权重最小的那条「横切边」一定是构成最小生成树的一条边。只要把图中的节点切成两个不重叠且非空的节点集合即可算作一个合法的「切分」

> 反证法证明：给定一幅图的最小生成树，那么随便给一种「切分」，一定至少有一条「横切边」属于最小生成树。假设这条「横切边」不是权重最小的，那说明最小生成树的权重和就还有再减小的余地，那这就矛盾了，最小生成树的权重和本来就是最小的，怎么再减？所以切分定理是正确的。



算法实现：Prim 算法的逻辑就是，每次切分都能找到最小生成树的一条边，然后又可以进行新一轮切分，直到找到最小生成树的所有边为止。

```java
class Prim {
    // 核心数据结构，存储「横切边」的优先级队列
    private PriorityQueue<int[]> pq;
    // 类似 visited 数组的作用，记录哪些节点已经成为最小生成树的一部分
    private boolean[] inMST;
    // 记录最小生成树的权重和
    private int weightSum = 0;
    // graph 是用邻接表表示的一幅图，
    // graph[s] 记录节点 s 所有相邻的边，
    // 三元组 int[]{from, to, weight} 表示一条边
    private List<int[]>[] graph;

    public Prim(List<int[]>[] graph) {
        this.graph = graph;
        this.pq = new PriorityQueue<>((a, b) -> {
            // 按照边的权重从小到大排序
            return a[2] - b[2];
        });
        // 图中有 n 个节点
        int n = graph.length;
        this.inMST = new boolean[n];

        // 随便从一个点开始切分都可以，我们不妨从节点 0 开始
        inMST[0] = true;
        cut(0);
        // 不断进行切分，向最小生成树中添加边
        while (!pq.isEmpty()) {
            int[] edge = pq.poll();
            int to = edge[1];
            int weight = edge[2];
            if (inMST[to]) {
                // 节点 to 已经在最小生成树中，跳过
                // 否则这条边会产生环
                continue;
            }
            // 将边 edge 加入最小生成树
            weightSum += weight;
            inMST[to] = true;
            // 节点 to 加入后，进行新一轮切分，会产生更多横切边
            cut(to);
        }
    }

    // 将 s 的横切边加入优先队列
    private void cut(int s) {
        // 遍历 s 的邻边
        for (int[] edge : graph[s]) {
            int to = edge[1];
            if (inMST[to]) {
                // 相邻接点 to 已经在最小生成树中，跳过
                // 否则这条边会产生环
                continue;
            }
            // 加入横切边队列
            pq.offer(edge);
        }
    }

    // 最小生成树的权重和
    public int weightSum() {
        return weightSum;
    }

    // 判断最小生成树是否包含图中的所有节点
    public boolean allConnected() {
        for (int i = 0; i < inMST.length; i++) {
            if (!inMST[i]) {
                return false;
            }
        }
        return true;
    }
}
```

复杂度分析：

复杂度主要在优先级队列 pq 的操作上，由于 pq 里面装的是图中的「边」，假设一幅图边的条数为 E，那么最多操作 O(E) 次 pq。每次操作优先级队列的时间复杂度取决于队列中的元素个数，取最坏情况就是 O(logE)。

这种 Prim 算法实现的总时间复杂度是 O(ElogE)



### Dijkstra 最短路径规划算法

算法签名：

```java
// 输入一幅图和一个起点 start，计算 start 到其他节点的最短距离
int[] dijkstra(int start, List<Integer>[] graph);
```

输入是一幅图 graph 和一个起点 start，返回是一个记录最短路径权重的数组。

比方说，输入起点 start = 3，函数返回一个 int[] 数组，假设赋值给 distTo 变量，那么从起点 3 到节点 6 的最短路径权重的值就是 distTo[6]。标准的 Dijkstra 算法会把从起点 start 到所有其他节点的最短路径都算出来。

```java
class State {
    // 图节点的 id
    int id;
    // 从 start 节点到当前节点的距离
    int distFromStart;

    State(int id, int distFromStart) {
        this.id = id;
        this.distFromStart = distFromStart;
    }
}
```

使用 distFromStart 变量记录从起点 start 到当前这个节点的距离。

普通 BFS 算法中，根据 BFS 的逻辑和无权图的特点，第一次遇到某个节点所走的步数就是最短距离，所以用一个 visited 数组防止走回头路，每个节点只会经过一次。

加权图中的 Dijkstra 算法和无权图中的普通 BFS 算法不同，在 Dijkstra 算法中，你第一次经过某个节点时的路径权重，不见得就是最小的，所以对于同一个节点，可能会经过多次，而且每次的 distFromStart 可能都不一样，取 distFromStart 最小的那次，就是从起点 start 到节点 5 的最短路径权重

Dijkstra 可以理解成一个带 dp table（或者说备忘录）的 BFS 算法，伪码如下：

```java
// 返回节点 from 到节点 to 之间的边的权重
int weight(int from, int to);

// 输入节点 s 返回 s 的相邻节点
List<Integer> adj(int s);

// 输入一幅图和一个起点 start，计算 start 到其他节点的最短距离
int[] dijkstra(int start, List<Integer>[] graph) {
    // 图中节点的个数
    int V = graph.length;
    // 记录最短路径的权重，你可以理解为 dp table
    // 定义：distTo[i] 的值就是节点 start 到达节点 i 的最短路径权重
    int[] distTo = new int[V];
    // 求最小值，所以 dp table 初始化为正无穷
    Arrays.fill(distTo, Integer.MAX_VALUE);
    // base case，start 到 start 的最短距离就是 0
    distTo[start] = 0;

    // 优先级队列，distFromStart 较小的排在前面
    Queue<State> pq = new PriorityQueue<>((a, b) -> {
        return a.distFromStart - b.distFromStart;
    });

    // 从起点 start 开始进行 BFS
    pq.offer(new State(start, 0));

     while (!pq.isEmpty()) {
        State curState = pq.poll();
        int curNodeID = curState.id;
        int curDistFromStart = curState.distFromStart;

        if (curDistFromStart > distTo[curNodeID]) {
            // 已经有一条更短的路径到达 curNode 节点了
            continue;
        }
        // 将 curNode 的相邻节点装入队列
        for (int nextNodeID : adj(curNodeID)) {
            // 看看从 curNode 达到 nextNode 的距离是否会更短
            int distToNextNode = distTo[curNodeID] + weight(curNodeID, nextNodeID);
            if (distTo[nextNodeID] > distToNextNode) {
                // 更新 dp table
                distTo[nextNodeID] = distToNextNode;
                // 将这个节点以及距离放入队列
                pq.offer(new State(nextNodeID, distToNextNode));
            }
        }
    }
    return distTo;
}
```



计算起点到终点end的最短距离

```java
// 输入起点 start 和终点 end，计算起点到终点的最短距离
int dijkstra(int start, int end, List<Integer>[] graph) {

    // ...

    while (!pq.isEmpty()) {
        State curState = pq.poll();
        int curNodeID = curState.id;
        int curDistFromStart = curState.distFromStart;

        // 在这里加一个判断就行了，其他代码不用改
        if (curNodeID == end) {
            return curDistFromStart;
        }

        if (curDistFromStart > distTo[curNodeID]) {
            continue;
        }

        // ...
    }

    // 如果运行到这里，说明从 start 无法走到 end
    return Integer.MAX_VALUE;
}
```



### A* 算法

Dijkstra算法的优点在于其简单可靠，能够保证找到全局最优解。然而，其缺点也明显：对大规模图的处理效率低下，因为它需要遍历整个图。

Astar 是一种 广搜的改良版。有的 Astar是 dijkstra 的改良版。

其实只是场景不同而已，在搜索最短路的时候， 如果是无权图（边的权值都是1） 那就用广搜，代码简洁，时间效率和 dijkstra 差不多 （具体要取决于图的稠密）如果是有权图（边有不同的权值），优先考虑 dijkstra。

而 Astar 关键在于 启发式函数， 也就是 影响 广搜或者 dijkstra 从 容器（队列）里取元素的优先顺序。

#### 实现机制

1. 启发式搜索的优势
   A\*算法引入了启发式函数h(v)，它预估了从节点v到目标节点的最优路径成本。这使得A\*能够在搜索过程中具有方向性，优先探索那些更有可能导向目标的路径，从而减少不必要的探索，提高搜索效率。

2. 实现机制

   - 评估函数：A\*的关键在于f(v)=g(v)+h(v)，其中g(v)是从起点到节点v的实际成本，h(v)是启发式函数，通常表示 当前节点 到终点的距离。因此两者相加就是起点到终点的距离。

   - 开放与关闭集合：算法维护两个集合，开放集合存放待评估的节点，关闭集合存放已评估节点。每次迭代从开放集合中选择f值最小的节点进行扩展，直到目标节点被加入关闭集合。



**BFS 是没有目的性的 一圈一圈去搜索， 而 A\* 是有方向性的去搜索**。

那么 A\* 为什么可以有方向性的去搜索，它的如何知道方向呢？**其关键在于 启发式函数**。

计算两点距离通常有如下三种计算方式：这也一般被选为启发式函数，用来预估当前节点到终点的距离

1. 曼哈顿距离，计算方式：d = abs(x1-x2)+abs(y1-y2)
2. 欧氏距离（欧拉距离） ，计算方式：d = sqrt( (x1-x2)^2 + (y1-y2)^2 )
3. 切比雪夫距离，计算方式：d = max(abs(x1 - x2), abs(y1 - y2))



#### 与Dijkstra的对比分析

- 计算效率：A\*由于采用了启发式信息，通常比Dijkstra算法更快找到解，尤其在复杂路网中更为显著。
- 路径质量：理论上，只要启发式函数满足可接纳性条件，A\*保证找到最短路径。Dijkstra同样保证最短路径，但缺乏效率优势。
- 资源消耗：A\*在内存使用上可能更高，因为它需要维护开放集合和关闭集合，而Dijkstra只需维护未访问集合和前驱节点映射。
- 适用场景：Dijkstra适用于小型或中型规模、对实时性要求不高的场景；A\*更适合大型图搜索或对实时性要求较高的无场景。



#### 代码实现

实现代码如下：启发式函数 采用 欧拉距离计算方式

```java
class Node {
    //表示节点在网格中的位置
    int x, y;
    //gCost表示从起点到该节点的实际代价，hCost表示从该节点到目标节点的估计代价（启发式值），fCost是两者之和。
    double gCost, hCost, fCost;
    //用于重构路径
    Node parent;

    public Node(int x, int y) {
        this.x = x;
        this.y = x;
    }

    //计算当前节点到目标节点的欧拉距离。
    public double calculateHeuristic(Node target) {
        return Math.sqrt(Math.pow(this.x - target.x, 2) + Math.pow(this.y - target.y, 2));
    }

    public void updateCosts(Node target, double gCost) {
        this.gCost = gCost;
        this.hCost = calculateHeuristic(target);
        this.fCost = this.gCost + this.hCost;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return x == node.x && y == node.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}

class AStar {
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    public List<Node> findPath(Node start, Node target, int[][] grid) {
        //表示待处理节点
        Set<Node> openSet = new HashSet<>();
        //表示已处理节点
        Set<Node> closedSet = new HashSet<>();
        //用于获取具有最小fCost的节点
        PriorityQueue<Node> priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));

        start.updateCosts(target, 0);
        openSet.add(start);
        priorityQueue.add(start);

        while (!openSet.isEmpty()) {
            Node current = priorityQueue.poll();
            openSet.remove(current);
            closedSet.add(current);

            if (current.equals(target)) {
                return reconstructPath(current);
            }

            for (int[] direction : DIRECTIONS) {
                int newX = current.x + direction[0];
                int newY = current.y + direction[1];

                if (!isInBounds(newX, newY, grid) || grid[newX][newY] == 1) {
                    continue;
                }

                Node neighbor = new Node(newX, newY);
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                double tentativeGCost = current.gCost + current.calculateHeuristic(neighbor);
                if (!openSet.contains(neighbor) || tentativeGCost < neighbor.gCost) {
                    neighbor.updateCosts(target, tentativeGCost);
                    neighbor.parent = current;

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                        priorityQueue.add(neighbor);
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    // 检查节点是否在网格范围内。
    private boolean isInBounds(int x, int y, int[][] grid) {
        return x >= 0 && y >= 0 && x < grid.length && y < grid[0].length;
    }

    //从目标节点回溯构建路径。
    private List<Node> reconstructPath(Node node) {
        List<Node> path = new ArrayList<>();
        while (node != null) {
            path.add(node);
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }
}
```



#### 复杂度分析

A\* 算法的时间复杂度 其实是不好去量化的，因为他取决于 启发式函数怎么写。

- 最坏情况下，A\* 退化成广搜，算法的时间复杂度 是 O(n \* 2)，n 为节点数量。

- 最佳情况，是从起点直接到终点，时间复杂度为 O(dlogd)，d 为起点到终点的深度。因为在搜索的过程中也需要堆排序，所以是 O(dlogd)。

实际上 A\* 的时间复杂度是介于 最优 和最坏 情况之间， 可以 非常粗略的认为 A\* 算法的时间复杂度是 O(nlogn) ，n 为节点数量。

A\* 算法的空间复杂度 O(b ^ d) ,d 为起点到终点的深度，b 是 图中节点间的连接数量



#### A\* 的缺点

大家看上述 A * 代码的时候，可以看到 我们向队列里添加了很多节点，但真正从队列里取出来的 仅仅是 靠启发式函数判断 距离终点最近的节点。

相对于普通BFS，A\* 算法只从 队列里取出 距离终点最近的节点。

那么问题来了，A\* 在一次路径搜索中，大量不需要访问的节点都在队列里，会造成空间的过度消耗。

IDA\* 算法对这一空间增长问题进行了优化，关于 IDA\* 算法，后续再更新 //to do

另外还有一种场景 是 A\* 解决不了的。

如果给出多个可能的目标，然后在这多个目标中选择最近的目标，这种 A\* 就不擅长了， A\* 只擅长给出明确的目标 然后找到最短路径。

如果是多个目标找最近目标（特别是潜在目标数量很多的时候），可以考虑 Dijkstra ，BFS 或者 Floyd。




<!-- @include: @article-footer.snippet.md -->     