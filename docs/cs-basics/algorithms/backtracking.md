---
title: 回溯算法
category: 计算机基础
tags:
  - 算法
head:
  - - meta
    - name: keywords
      content: 数据结构,回溯算法
  - - meta
    - name: description
      content: 全网最全的计算机基础（算法）知识点总结，让天下没有难学的八股文！
---





## 概述

其实回溯算法和我们常说的 DFS 算法非常类似，本质上就是一种暴力穷举算法。回溯算法和 DFS 算法的细微差别是：回溯算法是在遍历「树枝」，DFS 算法是在遍历「节点」

**抽象地说，解决一个回溯问题，实际上就是遍历一棵决策树的过程，树的每个叶子节点存放着一个合法答案。你把整棵树遍历一遍，把叶子节点上的答案都收集起来，就能得到所有的合法答案**。



站在回溯树的一个节点上，你只需要思考 3 个问题：

1、路径：也就是已经做出的选择。

2、选择列表：也就是你当前可以做的选择。

3、结束条件：也就是到达决策树底层，无法再做选择的条件。



代码方面，回溯算法的框架：

```java
result = []
def backtrack(路径, 选择列表):
    if 满足结束条件:
        result.add(路径)
        return
    
    for 选择 in 选择列表:
        做选择
        backtrack(路径, 选择列表)
        撤销选择
```

for循环就是遍历集合区间，可以理解一个节点有多少个孩子，这个for循环就执行多少次。

backtracking这里自己调用自己，实现递归。

**其核心就是 for 循环里面的递归，在递归调用之前「做选择」，在递归调用之后「撤销选择」**

## 回溯题各种变体

无论是排列、组合还是子集问题，简单说无非就是让你从序列 `nums` 中以给定规则取若干元素，主要有以下几种变体：

**形式一、元素无重不可复选，即 `nums` 中的元素都是唯一的，每个元素最多只能被使用一次，这也是最基本的形式**。

以组合为例，如果输入 `nums = [2,3,6,7]`，和为 7 的组合应该只有 `[7]`。

**形式二、元素可重不可复选，即 `nums` 中的元素可以存在重复，每个元素最多只能被使用一次**。

以组合为例，如果输入 `nums = [2,5,2,1,2]`，和为 7 的组合应该有两种 `[2,2,2,1]` 和 `[5,2]`。

**形式三、元素无重可复选，即 `nums` 中的元素都是唯一的，每个元素可以被使用若干次**。

以组合为例，如果输入 `nums = [2,3,6,7]`，和为 7 的组合应该有两种 `[2,2,3]` 和 `[7]`。

当然，也可以说有第四种形式，即元素可重可复选。但既然元素可复选，那又何必存在重复元素呢？元素去重之后就等同于形式三，所以这种情况不用考虑。

上面用组合问题举的例子，但排列、组合、子集问题都可以有这三种基本形式，所以共有 9 种变化。

除此之外，题目也可以再添加各种限制条件，比如让你求和为 `target` 且元素个数为 `k` 的组合，那这么一来又可以衍生出一堆变体，怪不得面试笔试中经常考到排列组合这种基本题型。

**但无论形式怎么变化，其本质就是穷举所有解，而这些解呈现树形结构，所以合理使用回溯算法框架，稍改代码框架即可把这些问题一网打尽**。


## 元素无重不可复选

### 子集问题

使用start参数控制树枝的生长避免产生重复的子集，用track记录根节点到每个路径的值，同时在前序位置把每个节点的路径值收集起来，完成回溯树的遍历就收集了所有子集

```java
class Solution {

    List<List<Integer>> res = new LinkedList<>();
    // 记录回溯算法的递归路径
    LinkedList<Integer> track = new LinkedList<>();

    // 主函数
    public List<List<Integer>> combine(int[] nums) {
        backtrack(nums, 0);
        return res;
    }

    void backtrack(int[] nums, int start) {

		//前序位置，每个节点的值都是一个子集
        res.add(new LinkedList<>(track));

        // 回溯算法标准框架
        for (int i = start; i <= nums.length; i++) {
            // 选择
            track.addLast(nums[i]);
            // 通过 start 参数控制树枝的遍历，避免产生重复的子集
            backtrack(nums[], i+ 1 );
            // 撤销选择
            track.removeLast();
        }
    }
}
```
### 组合问题 

上面可以生成无重子集了，那么稍微改改代码就可以生成无重组合了

例如，在nums[1,2,3] 中拿2个元素形成所有组合，你要怎么做？其实大小为2的组合，不就是所有大小为2的子集嘛？

```java
class Solution {

    List<List<Integer>> res = new LinkedList<>();
    // 记录回溯算法的递归路径
    LinkedList<Integer> track = new LinkedList<>();

    // 主函数
    public List<List<Integer>> combine(int[] candidates, int k) {
        backtrack(candidates, 0, k);
        return res;
    }

    void backtrack(int[] candidates, int start, int k) {
        // base case
        if (k == track.size()) {
            // 遍历到了第 k 层，收集当前节点的值
            res.add(new LinkedList<>(track));
            return;
        }
        
        // 回溯算法标准框架
        for (int i = start; i <= candidates.length; i++) {
            // 选择
            track.addLast(candidates[i]);
            // 通过 start 参数控制树枝的遍历，避免产生重复的子集
            backtrack(candidates, i + 1, k);
            // 撤销选择
            track.removeLast();
        }
    }
}
```

### 排列问题

刚刚说的子集、组合使用start变量保证 nums[start] 之后会出现 nums[start + 1]中的元素，通过固定元素的相对位置保证不出现重复的子集

但排列问题本身就是让你穷举元素的位置，nums[i] 之后也可以出现 num[i] 左边的元素，所以需要使用额外的used 数组来标记哪些元素还可以被选择

用 used 数组标记已经在路径上的元素避免重复选择，然后收集所有叶子节点上的值，就是所有全排列的结果

```java
class Solution {

    List<List<Integer>> res = new LinkedList<>();
    // 记录回溯算法的递归路径
    LinkedList<Integer> track = new LinkedList<>();
    // track 中的元素会被标记为 true
    boolean[] used;

    /* 主函数，输入一组不重复的数字，返回它们的全排列 */
    public List<List<Integer>> permute(int[] nums) {
        used = new boolean[nums.length];
        backtrack(nums);
        return res;
    }

    // 回溯算法核心函数
    void backtrack(int[] nums) {
        // base case，到达叶子节点
        if (track.size() == nums.length) {
            // 收集叶子节点上的值
            res.add(new LinkedList(track));
            return;
        }

        // 回溯算法标准框架。这里i是从0开始了，因为可以选择 num[i] 左边的元素
        for (int i = 0; i < nums.length; i++) {
            // 已经存在 track 中的元素，不能重复选择
            if (used[i]) {
                continue;
            }
            // 做选择
            used[i] = true;
            track.addLast(nums[i]);
            // 进入下一层回溯树
            backtrack(nums);
            // 取消选择
            track.removeLast();
            used[i] = false;
        }
    }
}
```

#### 排列子集

但是如果题目要的不是全排列，而是让你算元素个数为 k 的排列，怎么算？

这个其实改下 backtrack 的base case，仅收集第k层节点值即可

```java
class Solution {

    List<List<Integer>> res = new LinkedList<>();
    // 记录回溯算法的递归路径
    LinkedList<Integer> track = new LinkedList<>();
    // track 中的元素会被标记为 true
    boolean[] used;

    /* 主函数，输入一组不重复的数字，返回它们的全排列 */
    public List<List<Integer>> permute(int[] nums, int k) {
        used = new boolean[nums.length];
        backtrack(nums, k);
        return res;
    }

    // 回溯算法核心函数
    void backtrack(int[] nums, int k) {
        // base case，到达叶子节点
        if (track.size() == k) {
            // 收集叶子节点上的值
            res.add(new LinkedList(track));
            return;
        }

        // 回溯算法标准框架。这里i是从0开始了，因为可以选择 num[i] 左边的元素
        for (int i = 0; i < nums.length; i++) {
            // 已经存在 track 中的元素，不能重复选择
            if (used[i]) {
                continue;
            }
            // 做选择
            used[i] = true;
            track.addLast(nums[i]);
            // 进入下一层回溯树
            backtrack(nums);
            // 取消选择
            track.removeLast();
            used[i] = false;
        }
    }
}
```

## 元素可重不可复选

### 子集问题

刚刚讲的标准子集问题输入的 nums 是没有重复元素的，但如果存在重复元素，怎么处理呢？

例如：给你一个整数数组 nums ，其中可能包含重复元素，请你返回该数组所有可能的子集

如`nums = [1,2,2]` ，那么应该输出`[[],[1],[2],[1,2],[2,2],[1,2,3]]`

对于两个相同的值，只遍历其中1条，剩下的都剪掉，不要去遍历

体现在代码上，需要先进行排序，让相同元素靠在一起，如果发现 nums[i] == num[i-1] 则跳过

```java
class Solution {

    List<List<Integer>> res = new LinkedList<>();
    // 记录回溯算法的递归路径
    LinkedList<Integer> track = new LinkedList<>();

    // 主函数
    public List<List<Integer>> combine(int[] nums) {
	    Arrays.sort(nums);
        backtrack(nums, 0);
        return res;
    }

    void backtrack(int[] nums, int start) {

		//前序位置，每个节点的值都是一个子集
        res.add(new LinkedList<>(track));

        // 回溯算法标准框架
        for (int i = start; i <= nums.length; i++) {
	        //剪枝逻辑，值相同的相邻树枝，只遍历第1条
	        if(i > start && num[i] == num[i - 1]){
		        continue;
	        }
        
            // 选择
            track.addLast(nums[i]);
            // 通过 start 参数控制树枝的遍历，避免产生重复的子集
            backtrack(nums[], i+ 1 );
            // 撤销选择
            track.removeLast();
        }
    }
}
```

### 组合问题

组合问题和子集问题其实是等价的

例如，输入 candidates 和一个目标值 target，从 candidates 中找出所有和为 tsrget 的组合

candidates 肯呢个存在重复元素，且其中的每个元素最多只能使用1次


```java
class Solution {

    List<List<Integer>> res = new LinkedList<>();
    // 记录回溯算法的递归路径
    LinkedList<Integer> track = new LinkedList<>();
    // 记录track中的元素之和
    int trackSum = 0;

    // 主函数
    public List<List<Integer>> combine(int[] candidates, int target) {
	    if(candidates == 0){
			return res;
		} 
    	Arrays.sort(candidates);
        backtrack(candidates, 0, target);
        return res;
    }

    void backtrack(int[] candidates, int start, int target) {
        // base case
        if (trackSum == target) {
            // 达到目标和
            res.add(new LinkedList<>(track));
            return;
        }
        
        //超过目标和，直接结束
        if(trackSum > tartget){
	        rturn;
        }
        
        // 回溯算法标准框架
        for (int i = start; i <= candidates.length; i++) {
	        //剪枝
	        if(i > start; && nums[i] == nums[i-1]){
		        continue;
	        }
        
            // 选择
            track.addLast(candidates[i]);
            trackSum += candidates[i];
            // 通过 start 参数控制树枝的遍历，避免产生重复的子集
            backtrack(candidates, i + 1, k);
            // 撤销选择
            trackSum -= candidates[i];
            track.removeLast();
        }
    }
}
```

### 排列问题

排列问题的输入如果存在重复，比子集/组合问题的稍微复杂一点

例如：输入一个包含重复数字的序列 nums，请你写一个算法，返回所有可能的全排列

例如输入 `nums = [1,2,2]`，返回`[[1,2,2],[2,1,2],[2,2,1]]`

先看代码：

```java
class Solution {

    List<List<Integer>> res = new LinkedList<>();
    // 记录回溯算法的递归路径
    LinkedList<Integer> track = new LinkedList<>();
    // track 中的元素会被标记为 true
    boolean[] used;

    /* 主函数，输入一组不重复的数字，返回它们的全排列 */
    public List<List<Integer>> permute(int[] nums) {
	    Arrays.sort(nums);
        used = new boolean[nums.length];
        backtrack(nums);
        return res;
    }

    // 回溯算法核心函数
    void backtrack(int[] nums) {
        // base case，到达叶子节点
        if (track.size() == nums.length) {
            // 收集叶子节点上的值
            res.add(new LinkedList(track));
            return;
        }

        // 回溯算法标准框架。这里i是从0开始了，因为可以选择 num[i] 左边的元素
        for (int i = 0; i < nums.length; i++) {
            // 已经存在 track 中的元素，不能重复选择
            if (used[i]) {
                continue;
            }
            
            //新增剪枝逻辑
            if(i >= 0 && nums[i] == nums[i - 1] && !used[i - 1]){
	            continue;
            }
            
            // 做选择
            used[i] = true;
            track.addLast(nums[i]);
            // 进入下一层回溯树
            backtrack(nums);
            // 取消选择
            track.removeLast();
            used[i] = false;
        }
    }
}
```

这里除了和组合问题一样添加了排序，还额外添加了剪枝逻辑，且剪枝逻辑和组合/子集的剪枝逻辑不同。 新增 used[i - 1] 的逻辑判断。

例如输入 `nums = [1,2,2]`，为了区分两个相同的2，这里假设 `nums = [1,2,2']`，标准全排列会出现以下答案：`[[1,2,2'],[1,2',2],[2.1.2'],[2,2',1],[2',1,2],[2',2,1]]`。

标准的全排列之所以出现重复，是因为把相同元素形成的排列序列视为不同的序列，但实际上它们应该是相同的，而如果固定相同元素形成的序列顺序，就可以避免重复。例如我们保证 2 始终在 2' 前面，那么上面就只有3个符合条件：`[[1,2,2'],[2.1.2'],[2,2',1],]`

我们再看这个剪枝代码：

```java
//新增剪枝逻辑
if(i >= 0 && nums[i] == nums[i - 1] && !used[i - 1]){
	continue;
}

//...
```

当出现重复元素时，如输入  `nums = [1,2,2']`，**2' 只有在 2 已经被使用的情况下才会被选择**，这就保证了相同元素在排列中的相对位置固定。

## 元素无重可复选

### 组合/子集问题

给你一个无重复元素的数组 candidates 和一个目标和 target，找出可以使数字和为目标数 target 的所有组合。candidates 中的每个数字可以无限制重复被读取。

这道题可以认为是组合问题，也可以认为是子集问题。 candidates 中哪些 子集的 和为 target？

我们先回顾下，标准的 子集/组合问题是如何保证不重复使用元素的：

```java
for (int i = start; i <= candidates.length; i++) {

	//...
	//递归遍历下一层回溯树
    backtrack(candidates, i + 1);
	//...
}
```

在于这里的输入参数 start，这个i 是从start 开始，那么下一层就是 从start + 1开始，从而保证 nums[start] 这个元素不会被重复使用，那么反过来，如果我想让每个元素被重复使用，我只要把i + 1 改为 i 不就可以了？ 那么这个元素不就可以不断的重复读取了

```java
for (int i = start; i <= candidates.length; i++) {

	//...
	//递归遍历下一层回溯树
    backtrack(candidates, i);
	//...
}
```

当然了，这样可能这个回溯算法会无限递归回溯下去，所以需要设置合适 结束条件。即 路径和大于 target 就不需要再继续遍历下去了，代码如下：

```java
class Solution {

    List<List<Integer>> res = new LinkedList<>();
    // 记录回溯算法的递归路径
    LinkedList<Integer> track = new LinkedList<>();
    // 记录track中的元素之和
    int trackSum = 0;

    // 主函数
    public List<List<Integer>> combine(int[] candidates, int target) {
	    if(candidates == 0){
			return res;
		} 
        backtrack(candidates, 0, target);
        return res;
    }

    void backtrack(int[] candidates, int start, int target) {
        // base case
        if (trackSum == target) {
            // 达到目标和
            res.add(new LinkedList<>(track));
            return;
        }
        
        //超过目标和，直接结束
        if(trackSum > tartget){
	        rturn;
        }
        
        // 回溯算法标准框架
        for (int i = start; i <= candidates.length; i++) {
            // 选择
            track.addLast(candidates[i]);
            trackSum += candidates[i];
            // 同一个元素可以重复使用
            backtrack(candidates, i, k);
            // 撤销选择
            trackSum -= candidates[i];
            track.removeLast();
        }
    }
}
```

### 排列问题

nums 数组中的元素无重复且可复选的情况下，会有哪些排列？如 `nums = [1,2,3]`，那么这种条件下的全排列拥有 3^3 = 27种：

```text
[
[1, 1, 1],[1, 1, 2],[1, 1, 3],[1, 2, 1],[1, 2, 2],[1, 2, 3],[1, 3, 1],[1, 3, 2],[1, 3, 3],
[2, 1, 1],[2, 1, 2],[2, 1, 3],[2, 2, 1],[2, 2, 2],[2, 2, 3],[2, 3, 1],[2, 3, 2],[2, 3, 3],
[3, 1, 1],[3, 1, 2],[3, 1, 3],[3, 2, 1],[3, 2, 2],[3, 2, 3],[3, 3, 1],[3, 3, 2],[3, 3, 3]
]
```

标准的全排列算法利用 used 数组进行剪枝，避免重复使用同一个元素。如果允许重复使用元素的话，直接去除所有 used的 的剪枝逻辑就行。那么问题就简单了，代码如下：

```java
class Solution {

    List<List<Integer>> res = new LinkedList<>();
    // 记录回溯算法的递归路径
    LinkedList<Integer> track = new LinkedList<>();

    /* 主函数，输入一组不重复的数字，返回它们的全排列 */
    public List<List<Integer>> permute(int[] nums) {
        backtrack(nums);
        return res;
    }

    // 回溯算法核心函数
    void backtrack(int[] nums) {
        // base case，到达叶子节点
        if (track.size() == nums.length) {
            // 收集叶子节点上的值
            res.add(new LinkedList(track));
            return;
        }

        // 回溯算法标准框架。这里i是从0开始了，因为可以选择 num[i] 左边的元素
        for (int i = 0; i < nums.length; i++) {
            track.addLast(nums[i]);
            // 进入下一层回溯树
            backtrack(nums);
            // 取消选择
            track.removeLast();
        }
    }
}
```

## 总结

回溯算法就是个多叉树的遍历问题，关键就是在前序遍历和后序遍历的位置做一些操作，算法框架如下：

```python
def backtrack(...):
    for 选择 in 选择列表:
        做选择
        backtrack(...)
        撤销选择
```

**写 `backtrack` 函数时，需要维护走过的「路径」和当前可以做的「选择列表」，当触发「结束条件」时，将「路径」记入结果集**。


<!-- @include: @article-footer.snippet.md -->     