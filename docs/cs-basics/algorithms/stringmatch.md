---
title: 字符串匹配算法
category: 计算机基础
tag:
 - 算法
head:
  - - meta
    - name: keywords
      content: 数据结构,字符串匹配算法
  - - meta
    - name: description
      content: 全网最全的计算机基础（算法）知识点总结，让天下没有难学的八股文！
---

## Rabin-Karp算法

Rabin-Karp算法是一种**基于哈希函数的字符串匹配算法**，由 Michael O. Rabin 和 Richard M. Karp 于1987年提出，核心思想是用**哈希函数**将模式串和文本串中的子串转换为数值进行比较，避免大量不必要的字符比较。这个算法特别适合**多模式串匹配场景**，时间复杂度平均为O(n+m)，n是文本串长度，m是模式串长度。

Rabin-Karp算法的关键在于使用**滚动哈希函数**（Rolling Hash），它可以在常数时间内计算出滑动窗口的新哈希值，保证算法在大多数情况下的高效性。

### 算法步骤

1. 计算模式串的哈希值
2. 计算文本串中长度为m的第一个子串的哈希值（m为模式串长度）
3. 在文本串上滑动窗口，对于每个位置：
    - 使用滚动哈希技术高效计算当前窗口的哈希值
    - 如果哈希值与模式串相等，则进行字符逐一比较以避免哈希冲突
    - 如果完全匹配，则找到一个匹配位置
4. 重复步骤3，直到处理完整个文本串


核心特性
- **基于哈希比较**：通过哈希值比较代替直接字符比较
- **滚动哈希**：O(1)时间复杂度计算下一窗口的哈希值
- **时间复杂度**：平均情况O(n+m)，最坏情况O(n*m)
- **空间复杂度**：O(1)，只需常数额外空间
- **适用范围**：单模式和多模式串匹配场景，特别是多模式匹配

### 基础实现

接下来大家一起看下Rabin-Karp算法的部分主流语言实现：

```java
public class RabinKarp {
    private final static int PRIME = 101; // 哈希计算使用的质数
    
    public static int search(String text, String pattern) {
        int m = pattern.length();
        int n = text.length();
        
        if (m > n) return -1;
        if (m == 0) return 0;
        
        // 计算哈希乘数，等于d^(m-1) % PRIME，用于滚动哈希计算
        int h = 1;
        for (int i = 0; i < m - 1; i++) {
            h = (h * 256) % PRIME;
        }
        
        // 计算模式串和第一个窗口的哈希值
        int patternHash = 0;
        int textHash = 0;
        for (int i = 0; i < m; i++) {
            patternHash = (256 * patternHash + pattern.charAt(i)) % PRIME;
            textHash = (256 * textHash + text.charAt(i)) % PRIME;
        }
        
        // 滑动窗口，比较哈希值
        for (int i = 0; i <= n - m; i++) {
            // 哈希值相等时，检查是否真正匹配
            if (patternHash == textHash) {
                boolean match = true;
                for (int j = 0; j < m; j++) {
                    if (text.charAt(i + j) != pattern.charAt(j)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return i; // 找到匹配
                }
            }
            
            // 计算下一个窗口的哈希值
            if (i < n - m) {
                textHash = (256 * (textHash - text.charAt(i) * h) + text.charAt(i + m)) % PRIME;
                // 处理负数哈希值
                if (textHash < 0) {
                    textHash += PRIME;
                }
            }
        }
        
        return -1; // 未找到匹配
    }
    
    // 打印结果
    public static void main(String[] args) {
        String text = "ABABCABABDABACDABABCABAB";
        String pattern = "ABABCABAB";
        
        int position = search(text, pattern);
        if (position == -1) {
            System.out.println("未找到匹配");
        } else {
            System.out.println("模式串在位置 " + position + " 处匹配");
            System.out.println(text);
            // 打印指示匹配位置的指针
            for (int i = 0; i < position; i++) {
                System.out.print(" ");
            }
            System.out.println(pattern);
        }
    }
}
```

### 优化：使用更好的哈希函数

比如使用更复杂的哈希函数来减少冲突

```java
public class ImprovedRabinKarp {
    private final static long PRIME1 = 1000000007; // 第一个哈希的质数
    private final static long PRIME2 = 1000000009; // 第二个哈希的质数
    
    // 使用双哈希来减少冲突
    public static int search(String text, String pattern) {
        int m = pattern.length();
        int n = text.length();
        
        if (m > n) return -1;
        if (m == 0) return 0;
        
        // 计算哈希乘数
        long h1 = 1;
        long h2 = 1;
        for (int i = 0; i < m - 1; i++) {
            h1 = (h1 * 256) % PRIME1;
            h2 = (h2 * 256) % PRIME2;
        }
        
        // 计算模式串和第一个窗口的哈希值
        long patternHash1 = 0;
        long patternHash2 = 0;
        long textHash1 = 0;
        long textHash2 = 0;
        
        for (int i = 0; i < m; i++) {
            patternHash1 = (256 * patternHash1 + pattern.charAt(i)) % PRIME1;
            patternHash2 = (256 * patternHash2 + pattern.charAt(i)) % PRIME2;
            textHash1 = (256 * textHash1 + text.charAt(i)) % PRIME1;
            textHash2 = (256 * textHash2 + text.charAt(i)) % PRIME2;
        }
        
        // 滑动窗口，比较哈希值
        for (int i = 0; i <= n - m; i++) {
            // 两个哈希都相等时，再进行字符比较
            if (patternHash1 == textHash1 && patternHash2 == textHash2) {
                boolean match = true;
                for (int j = 0; j < m; j++) {
                    if (text.charAt(i + j) != pattern.charAt(j)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return i; // 找到匹配
                }
            }
            
            // 计算下一个窗口的哈希值
            if (i < n - m) {
                textHash1 = (256 * (textHash1 - text.charAt(i) * h1) + text.charAt(i + m)) % PRIME1;
                textHash2 = (256 * (textHash2 - text.charAt(i) * h2) + text.charAt(i + m)) % PRIME2;
                
                // 处理负数哈希值
                if (textHash1 < 0) textHash1 += PRIME1;
                if (textHash2 < 0) textHash2 += PRIME2;
            }
        }
        
        return -1; // 未找到匹配
    }
}
```

### 优点

- 平均情况下时间复杂度为O(n+m)，接近线性时间
- 在多模式匹配场景下效率高
- 可以通过预处理模式串提高效率
- 滚动哈希计算使得算法高效移动窗口
- 实现相对简单，原理容易理解

### 缺点

- 哈希冲突可能导致额外的字符比较
- 最坏情况下的时间复杂度为O(n*m)
- 哈希函数的选择对算法性能影响很大
- 需要注意数值溢出问题
- 对于短模式串和文本串，预处理开销可能抵消算法优势

### 应用场景

1）文档相似度检测和抄袭检测
2）网络安全中的特征码匹配
3）多模式字符串搜索引擎
4）编译器中的词法分析器

### 扩展：Rabin-Karp指纹算法

Rabin-Karp算法的一个变种应用于文件相似度比较

```java
public class RabinKarpFingerprint {
    private final static long PRIME = 1000000007;
    private final static int WINDOW_SIZE = 5; // 指纹窗口大小
    
    public static Set<Long> generateFingerprints(String text) {
        Set<Long> fingerprints = new HashSet<>();
        int n = text.length();
        
        if (n < WINDOW_SIZE) {
            fingerprints.add(calculateHash(text, n));
            return fingerprints;
        }
        
        // 计算第一个窗口的哈希值
        long textHash = calculateHash(text, WINDOW_SIZE);
        fingerprints.add(textHash);
        
        // 计算哈希乘数
        long h = 1;
        for (int i = 0; i < WINDOW_SIZE - 1; i++) {
            h = (h * 256) % PRIME;
        }
        
        // 滑动窗口，计算所有长度为WINDOW_SIZE的子串哈希值
        for (int i = 0; i <= n - WINDOW_SIZE - 1; i++) {
            textHash = (256 * (textHash - text.charAt(i) * h) + text.charAt(i + WINDOW_SIZE)) % PRIME;
            if (textHash < 0) {
                textHash += PRIME;
            }
            fingerprints.add(textHash);
        }
        
        return fingerprints;
    }
    
    public static double calculateSimilarity(String text1, String text2) {
        Set<Long> fingerprints1 = generateFingerprints(text1);
        Set<Long> fingerprints2 = generateFingerprints(text2);
        
        // 计算交集大小
        Set<Long> intersection = new HashSet<>(fingerprints1);
        intersection.retainAll(fingerprints2);
        
        // 计算并集大小
        Set<Long> union = new HashSet<>(fingerprints1);
        union.addAll(fingerprints2);
        
        // 杰卡德相似度系数
        return (double) intersection.size() / union.size();
    }
    
    private static long calculateHash(String str, int length) {
        long hash = 0;
        for (int i = 0; i < length; i++) {
            hash = (256 * hash + str.charAt(i)) % PRIME;
        }
        return hash;
    }
}
```

###  扩展：子字符串哈希

一些编程竞赛里也使用Rabin-Karp思想进行高效的子字符串查询

```java
public class SubstringHash {
    private static final long PRIME = 1000000007;
    private static final int BASE = 256;
    
    private long[] hash; // 前缀哈希值
    private long[] pow;  // BASE的幂
    private String s;    // 源字符串
    
    public SubstringHash(String s) {
        this.s = s;
        int n = s.length();
        hash = new long[n + 1];
        pow = new long[n + 1];
        
        // 预计算BASE的幂
        pow[0] = 1;
        for (int i = 1; i <= n; i++) {
            pow[i] = (pow[i - 1] * BASE) % PRIME;
        }
        
        // 计算所有前缀的哈希值
        hash[0] = 0;
        for (int i = 0; i < n; i++) {
            hash[i + 1] = (hash[i] * BASE + s.charAt(i)) % PRIME;
        }
    }
    
    // 计算子串s[l..r]的哈希值（0-indexed）
    public long substringHash(int l, int r) {
        // 获取s[0...r]的哈希值，减去s[0...l-1]的哈希值（需要进行适当调整）
        long result = (hash[r + 1] - (hash[l] * pow[r - l + 1]) % PRIME) % PRIME;
        if (result < 0) {
            result += PRIME;
        }
        return result;
    }
    
    // 检查两个子串是否相同
    public boolean areSubstringsEqual(int l1, int r1, int l2, int r2) {
        if (r1 - l1 != r2 - l2) {
            return false; // 长度不同
        }
        return substringHash(l1, r1) == substringHash(l2, r2);
    }
}
```

### 相关的 LeetCode 热门题目

- [28. 实现 strStr()](https://leetcode.cn/problems/find-the-index-of-the-first-occurrence-in-a-string/)
- [187. 重复的DNA序列](https://leetcode.cn/problems/repeated-dna-sequences/) - 利用Rabin-Karp滚动哈希思想解决
- [1044. 最长重复子串](https://leetcode.cn/problems/longest-duplicate-substring/) - 结合二分查找和Rabin-Karp算法
- [1554. 只有一个不同字符的字符串](https://leetcode.cn/problems/strings-differ-by-one-character/)

Rabin-Karp算法巧妙结合哈希计算和滚动窗口技术，在字符串匹配领域提供了一种高效的解决方案，特别适合多模式匹配和大规模文本处理场景。

## Boyer-Moore算法

Boyer-Moore算法是一种高效的字符串匹配算法，由 Robert S. Boyer和J Strother Moore 设计于1977年。它**从右向左比较**字符，并利用两个启发式规则（坏字符规则和好后缀规则）在不匹配情况下实现较大跳跃，减少比较次数。Boyer-Moore算法在实际应用中大部分情况下**比朴素算法和KMP算法更高效**。

### 算法步骤

1. 预处理模式串，构建坏字符表和好后缀表
2. 将模式串对齐到文本串的开始位置
3. 从模式串的最右侧字符开始比较，从右向左进行匹配
4. 如果发生不匹配，通过以下规则计算跳转距离：
    - 坏字符规则：根据不匹配字符在模式串中的最右位置决定跳转距离
    - 好后缀规则：根据已匹配部分在模式串中的重复情况决定跳转距离
5. 选择两个规则中的最大跳转距离，移动模式串
6. 重复步骤3-5，直到找到匹配或到达文本串末尾


核心特性：
- **从右向左比较**：与大多数字符串匹配算法不同，从模式串的末尾开始比较
- **双规则跳转**：利用坏字符规则和好后缀规则计算跳转距离
- **时间复杂度**：最坏情况O(m*n)，m是模式串长度，n是文本串长度；平均情况接近O(n/m)
- **空间复杂度**：O(k+m)，其中k是字符集大小，m是模式串长度
- **适用范围**：特别适合长模式串和大字符集场景

### 基础实现

```java
public class BoyerMoore {
    private final int R; // 字符集大小
    private int[] badChar; // 坏字符表
    private int[] goodSuffix; // 好后缀表
    private int[] borderPos; // 边界位置表
    private String pattern; // 模式串
    
    public BoyerMoore(String pattern) {
        this.R = 256; // ASCII字符集
        this.pattern = pattern;
        int m = pattern.length();
        
        // 初始化坏字符表
        badChar = new int[R];
        for (int c = 0; c < R; c++) {
            badChar[c] = -1; // 初始化为-1
        }
        for (int j = 0; j < m; j++) {
            badChar[pattern.charAt(j)] = j; // 记录每个字符最右出现位置
        }
        
        // 初始化好后缀表和边界位置表
        goodSuffix = new int[m];
        borderPos = new int[m];
        processSuffixes();
    }
    
    // 预处理好后缀表
    private void processSuffixes() {
        int m = pattern.length();
        int i = m, j = m + 1;
        borderPos[i] = j;
        
        // 计算边界位置
        while (i > 0) {
            while (j <= m && pattern.charAt(i - 1) != pattern.charAt(j - 1)) {
                if (goodSuffix[j] == 0) {
                    goodSuffix[j] = j - i;
                }
                j = borderPos[j];
            }
            i--; j--;
            borderPos[i] = j;
        }
        
        // 计算好后缀表
        j = borderPos[0];
        for (i = 0; i <= m; i++) {
            if (goodSuffix[i] == 0) {
                goodSuffix[i] = j;
            }
            if (i == j) {
                j = borderPos[j];
            }
        }
    }
    
    // 搜索文本串中的匹配
    public int search(String text) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return 0;
        
        int skip;
        for (int i = 0; i <= n - m; i += skip) {
            skip = 0;
            for (int j = m - 1; j >= 0; j--) {
                if (pattern.charAt(j) != text.charAt(i + j)) {
                    // 坏字符规则
                    skip = Math.max(1, j - badChar[text.charAt(i + j)]);
                    // 好后缀规则
                    if (j < m - 1) {
                        skip = Math.max(skip, goodSuffix[j + 1]);
                    }
                    break;
                }
            }
            if (skip == 0) return i; // 找到匹配
        }
        return -1; // 没有找到匹配
    }
    
    // 测试
    public static void main(String[] args) {
        String text = "HERE IS A SIMPLE EXAMPLE";
        String pattern = "EXAMPLE";
        
        BoyerMoore bm = new BoyerMoore(pattern);
        int position = bm.search(text);
        
        if (position == -1) {
            System.out.println("未找到匹配");
        } else {
            System.out.println("模式串在位置 " + position + " 处匹配");
            System.out.println(text);
            for (int i = 0; i < position; i++) {
                System.out.print(" ");
            }
            System.out.println(pattern);
        }
    }
}
```

### 优化策略

#### 简化好后缀表构建

对于一些应用场景，可以只使用坏字符规则，简化算法实现

```java
public class SimplifiedBoyerMoore {
    private final int R; // 字符集大小
    private int[] badChar; // 坏字符表
    private String pattern; // 模式串
    
    public SimplifiedBoyerMoore(String pattern) {
        this.R = 256; // ASCII字符集
        this.pattern = pattern;
        int m = pattern.length();
        
        // 初始化坏字符表
        badChar = new int[R];
        for (int c = 0; c < R; c++) {
            badChar[c] = -1; // 初始化为-1
        }
        for (int j = 0; j < m; j++) {
            badChar[pattern.charAt(j)] = j; // 记录每个字符最右出现位置
        }
    }
    
    // 搜索文本串中的匹配
    public int search(String text) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return 0;
        
        int skip;
        for (int i = 0; i <= n - m; i += skip) {
            skip = 0;
            for (int j = m - 1; j >= 0; j--) {
                if (pattern.charAt(j) != text.charAt(i + j)) {
                    // 仅使用坏字符规则
                    skip = Math.max(1, j - badChar[text.charAt(i + j)]);
                    break;
                }
            }
            if (skip == 0) return i; // 找到匹配
        }
        return -1; // 没有找到匹配
    }
}
```

#### 缓存预计算结果

针对需要重复搜索同一模式串的场景，可以预计算并缓存结果

```java
public class CachedBoyerMoore {
    private Map<String, BoyerMoore> cache = new HashMap<>();
    
    public int search(String text, String pattern) {
        // 检查缓存中是否有预计算的Boyer-Moore对象
        BoyerMoore bm = cache.get(pattern);
        if (bm == null) {
            bm = new BoyerMoore(pattern);
            cache.put(pattern, bm);
        }
        
        return bm.search(text);
    }
}
```

### 优点

- 在实际应用中，大部分场景比KMP和朴素算法更高效
- 最好情况下可以跳过大量文本，实现亚线性时间复杂度
- 对于长模式串和大字符集特别有效
- 预处理跟模式串有关，与文本串长度无关

### 缺点

- 预处理复杂，特别是好后缀表的构建
- 需要额外空间存储坏字符表和好后缀表
- 最坏情况下时间复杂度仍为O(m*n)
- 对于短模式串，预处理开销可能抵消算法优势
- 好后缀规则的实现较复杂，容易出错

### 应用场景

1）文本编辑器的查找功能
2）网络安全中的特征码匹配
3）自然语言处理中的关键词检索
4）大规模文本数据处理

### 扩展：Horspool算法

Horspool算法是Boyer-Moore的简化版本，只使用坏字符规则，但是对坏字符表进行了修改

```java
public class Horspool {
    private final int R; // 字符集大小
    private int[] badChar; // 坏字符表
    private String pattern; // 模式串
    
    public Horspool(String pattern) {
        this.R = 256; // ASCII字符集
        this.pattern = pattern;
        int m = pattern.length();
        
        // 初始化坏字符表
        badChar = new int[R];
        // 所有字符默认移动模式串长度
        for (int c = 0; c < R; c++) {
            badChar[c] = m;
        }
        // 模式串中的字符（除了最后一个）设置为对应值
        for (int j = 0; j < m - 1; j++) {
            badChar[pattern.charAt(j)] = m - 1 - j;
        }
    }
    
    // 搜索文本串中的匹配
    public int search(String text) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return 0;
        if (m > n) return -1;
        
        int i = m - 1; // 从模式串最后一个字符对齐开始
        while (i < n) {
            int k = 0;
            while (k < m && pattern.charAt(m - 1 - k) == text.charAt(i - k)) {
                k++;
            }
            
            if (k == m) {
                return i - m + 1; // 找到匹配
            }
            
            // 使用坏字符规则移动
            i += badChar[text.charAt(i)];
        }
        
        return -1; // 没有找到匹配
    }
}
```


### 扩展：Sunday算法

Sunday算法是另一种Boyer-Moore的变种，它关注的是文本串中模式串后面的字符

```java
public class Sunday {
    private final int R; // 字符集大小
    private int[] shift; // 移动表
    private String pattern; // 模式串
    
    public Sunday(String pattern) {
        this.R = 256; // ASCII字符集
        this.pattern = pattern;
        int m = pattern.length();
        
        // 初始化移动表
        shift = new int[R];
        // 所有字符默认移动模式串长度+1
        for (int c = 0; c < R; c++) {
            shift[c] = m + 1;
        }
        // 模式串中的字符设置为对应值
        for (int j = 0; j < m; j++) {
            shift[pattern.charAt(j)] = m - j;
        }
    }
    
    // 搜索文本串中的匹配
    public int search(String text) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return 0;
        if (m > n) return -1;
        
        int i = 0; // 从文本串开始位置
        while (i <= n - m) {
            int j = 0;
            while (j < m && pattern.charAt(j) == text.charAt(i + j)) {
                j++;
            }
            
            if (j == m) {
                return i; // 找到匹配
            }
            
            // 下一个位置超出文本串长度，返回-1
            if (i + m >= n) {
                return -1;
            }
            
            // 使用Sunday算法的移动规则
            i += shift[text.charAt(i + m)];
        }
        
        return -1; // 没有找到匹配
    }
}
```


### 相关的 LeetCode 热门题目

- [28. 实现strStr()](https://leetcode.cn/problems/find-the-index-of-the-first-occurrence-in-a-string/)
- [459. 重复的子字符串](https://leetcode.cn/problems/repeated-substring-pattern/)
- [686. 重复叠加字符串匹配](https://leetcode.cn/problems/repeated-string-match/)
- [1392. 最长快乐前缀](https://leetcode.cn/problems/longest-happy-prefix/)


## KMP算法

KMP（Knuth-Morris-Pratt）算法是一种高效的字符串匹配算法，核心思想是**利用已经部分匹配的信息，避免重复比较**，在文本串中快速查找模式串。KMP算法特别适合**处理长文本和重复性高的模式串**，时间复杂度是O(m+n)，m是模式串长度，n是文本串长度。

KMP算法的关键在于构建一个部分匹配表（也叫失败函数或者next数组），这个表记录了当匹配失败时，模式串指针应该回退到的位置，让算法跳过已知不可能匹配的位置，提高匹配效率。

### 算法步骤

KMP算法主要分为两个阶段：

1. **预处理阶段**：计算模式串的部分匹配表（next数组）
    - 构建一个数组，记录每个位置的最长相等前后缀长度
    - 该数组用于在匹配失败时确定模式串指针的回退位置
2. **匹配阶段**：使用部分匹配表在文本串中查找模式串
    - 从左到右同时遍历文本串和模式串
    - 当字符不匹配时，根据next数组回退模式串指针
    - 当模式串完全匹配时，记录匹配位置并继续查找其他匹配

核心特性：
- **线性时间复杂度**：O(m+n)，其中m是模式串长度，n是文本串长度
- **高效利用历史信息**：通过预处理避免了重复比较
- **只需一次遍历文本串**：文本串指针不会回退
- **空间复杂度**：O(m)，仅需存储模式串的部分匹配表
- **适用场景**：特别适合长文本和具有重复性的模式串


### 基础实现

#### 暴力解法

```java
public class NaiveStringMatcher {
    
    /**
     * 朴素字符串匹配算法
     * @param text 文本串
     * @param pattern 模式串
     * @return 匹配成功则返回模式串在文本串中的起始位置，否则返回-1
     */
    public static int naiveSearch(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();
        
        // 特殊情况处理
        if (m == 0) return 0;
        if (n < m) return -1;
        
        // 尝试所有可能的匹配位置
        for (int i = 0; i <= n - m; i++) {
            int j;
            
            // 从当前位置开始比较模式串和文本串
            for (j = 0; j < m; j++) {
                if (text.charAt(i + j) != pattern.charAt(j)) {
                    break; // 发现不匹配字符，终止内层循环
                }
            }
            
            // 如果j等于m，说明模式串完全匹配
            if (j == m) {
                return i; // 返回匹配位置
            }
        }
        
        return -1; // 未找到匹配
    }
    
    // 使用示例
    public static void main(String[] args) {
        String text = "ABABDABACDABABCABAB";
        String pattern = "ABABCABAB";
        
        int position = naiveSearch(text, pattern);
        
        if (position == -1) {
            System.out.println("未找到匹配");
        } else {
            System.out.println("模式串在位置 " + position + " 处匹配");
        }
    }
}
```

上述实现暴力枚举所有可能的匹配位置，逐一比较文本串与模式串的每个字符，直到找到完全匹配或确定不存在匹配

#### KMP算法的实现

```java
public class KMP {
    // 构建部分匹配表（next数组）
    private static int[] buildNext(String pattern) {
        int m = pattern.length();
        int[] next = new int[m];
        next[0] = 0; // 第一个字符的最长相等前后缀长度为0
        
        for (int i = 1, j = 0; i < m; i++) {
            // 当前字符不匹配，回退j
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) {
                j = next[j - 1];
            }
            
            // 当前字符匹配，j向前移动
            if (pattern.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            
            // 记录当前位置的最长相等前后缀长度
            next[i] = j;
        }
        
        return next;
    }
    
    // KMP搜索算法
    public static int kmpSearch(String text, String pattern) {
        if (pattern == null || pattern.length() == 0) {
            return 0;
        }
        
        if (text == null || text.length() < pattern.length()) {
            return -1;
        }
        
        int n = text.length();
        int m = pattern.length();
        
        // 构建next数组
        int[] next = buildNext(pattern);
        
        // 进行匹配
        for (int i = 0, j = 0; i < n; i++) {
            // 当前字符不匹配，根据next数组回退j
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                j = next[j - 1];
            }
            
            // 当前字符匹配，j向前移动
            if (text.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            
            // 完全匹配，返回起始索引
            if (j == m) {
                return i - m + 1;
            }
        }
        
        return -1; // 未找到匹配
    }
    
    // 查找所有匹配位置
    public static List<Integer> kmpSearchAll(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();
        if (pattern == null || pattern.length() == 0) {
            return positions;
        }
        
        if (text == null || text.length() < pattern.length()) {
            return positions;
        }
        
        int n = text.length();
        int m = pattern.length();
        
        // 构建next数组
        int[] next = buildNext(pattern);
        
        // 进行匹配
        for (int i = 0, j = 0; i < n; i++) {
            // 当前字符不匹配，回退j
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                j = next[j - 1];
            }
            
            // 当前字符匹配，j向前移动
            if (text.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            
            // 完全匹配，记录位置并继续匹配
            if (j == m) {
                positions.add(i - m + 1);
                // 回退j以寻找下一个匹配
                j = next[j - 1];
            }
        }
        
        return positions;
    }
    
    public static void main(String[] args) {
        String text = "ABABDABACDABABCABAB";
        String pattern = "ABABCABAB";
        
        int pos = kmpSearch(text, pattern);
        List<Integer> allPos = kmpSearchAll(text, pattern);
        
        System.out.println("文本: " + text);
        System.out.println("模式: " + pattern);
        System.out.println("首次匹配位置: " + (pos != -1 ? pos : "未找到"));
        System.out.println("所有匹配位置: " + allPos);
        
        // 打印next数组，帮助理解
        int[] next = buildNext(pattern);
        System.out.print("next数组: ");
        for (int val : next) {
            System.out.print(val + " ");
        }
        System.out.println();
    }
}
```

在上述代码中：

```java
// 当前字符不匹配，回退j
while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
    j = next[j - 1];
}
```

是 KMP 算法的核心，在匹配失败时根据预先计算的next数组来确定模式串指针的回退位置。

### 优化

优化后的 next 数组

```java
// 优化next数组，避免匹配失败后回退到同样会失败的位置
private static int[] buildOptimizedNext(String pattern) {
    int m = pattern.length();
    int[] next = new int[m];
    next[0] = 0;
    
    for (int i = 1, j = 0; i < m; i++) {
        while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) {
            j = next[j - 1];
        }
        
        if (pattern.charAt(i) == pattern.charAt(j)) {
            j++;
        }
        
        // 当前位置匹配失败时，如果回退位置的字符与当前位置相同，则继续回退
        if (i + 1 < m && pattern.charAt(i + 1) == pattern.charAt(j)) {
            next[i] = next[j - 1];
        } else {
            next[i] = j;
        }
    }
    
    return next;
}
```

预处理减少分支实现

```java
// 预处理字符映射，减少字符比较的分支
public static int kmpSearchOptimized(String text, String pattern) {
    if (pattern == null || pattern.length() == 0) {
        return 0;
    }
    
    if (text == null || text.length() < pattern.length()) {
        return -1;
    }
    
    int n = text.length();
    int m = pattern.length();
    
    // 使用数组映射来加速字符比较（假设字符集为ASCII）
    // 为每个模式字符的每个位置创建一个状态转移表
    int[][] dfa = new int[256][m];
    
    // 初始化第一个字符的DFA
    dfa[pattern.charAt(0)][0] = 1;
    
    for (int X = 0, j = 1; j < m; j++) {
        // 复制匹配失败情况下的值
        for (int c = 0; c < 256; c++) {
            dfa[c][j] = dfa[c][X];
        }
        // 设置匹配成功情况下的值
        dfa[pattern.charAt(j)][j] = j + 1;
        // 更新重启状态
        X = dfa[pattern.charAt(j)][X];
    }
    
    // 模式匹配
    int i, j;
    for (i = 0, j = 0; i < n && j < m; i++) {
        j = dfa[text.charAt(i)][j];
    }
    
    if (j == m) {
        return i - m; // 找到匹配
    } else {
        return -1;    // 未找到匹配
    }
}
```

### 优点

- 时间复杂度为O(m+n)，优于朴素的字符串匹配算法(暴力解法)
- 文本串只需扫描一次，不会回退
- 对于包含重复模式的字符串会高效
- 预处理模式串，可以多次用于不同的文本串
- 能快速跳过已知不会匹配的位置

### 缺点

- 需要额外的空间存储next数组
- 构建next数组的逻辑较为复杂，不易理解
- 在模式串较短或无重复模式时，相比简单算法优势不明显
- 实现时容易出错，特别是处理边界情况

### 应用场景

1）生物信息学中的DNA序列匹配
2）网络入侵检测系统中的模式匹配
3）搜索引擎的关键词匹配
4）数据压缩算法中的模式识别

### 扩展：多模式字符串匹配

```java
// Aho-Corasick算法 - KMP的多模式扩展
public static class AhoCorasick {
    static class TrieNode {
        TrieNode[] children = new TrieNode[256];
        TrieNode fail;
        List<Integer> patternIndices = new ArrayList<>();
        
        public TrieNode() {
            fail = null;
        }
    }
    
    private TrieNode root;
    private String[] patterns;
    
    public AhoCorasick(String[] patterns) {
        this.patterns = patterns;
        buildTrie();
        buildFailureLinks();
    }
    
    private void buildTrie() {
        root = new TrieNode();
        
        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            TrieNode node = root;
            
            for (char c : pattern.toCharArray()) {
                if (node.children[c] == null) {
                    node.children[c] = new TrieNode();
                }
                node = node.children[c];
            }
            
            node.patternIndices.add(i);
        }
    }
    
    private void buildFailureLinks() {
        Queue<TrieNode> queue = new LinkedList<>();
        
        // 初始化根节点的子节点
        for (int i = 0; i < 256; i++) {
            if (root.children[i] != null) {
                root.children[i].fail = root;
                queue.offer(root.children[i]);
            } else {
                root.children[i] = root;
            }
        }
        
        // BFS构建失败链接
        while (!queue.isEmpty()) {
            TrieNode node = queue.poll();
            
            for (int i = 0; i < 256; i++) {
                if (node.children[i] != null) {
                    TrieNode failNode = node.fail;
                    
                    while (failNode != root && failNode.children[i] == null) {
                        failNode = failNode.fail;
                    }
                    
                    failNode = failNode.children[i];
                    node.children[i].fail = failNode;
                    
                    // 合并匹配结果
                    node.children[i].patternIndices.addAll(failNode.patternIndices);
                    
                    queue.offer(node.children[i]);
                }
            }
        }
    }
    
    public List<Pair<Integer, Integer>> search(String text) {
        List<Pair<Integer, Integer>> results = new ArrayList<>();
        TrieNode currentState = root;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            while (currentState != root && currentState.children[c] == null) {
                currentState = currentState.fail;
            }
            
            currentState = currentState.children[c];
            
            for (int patternIndex : currentState.patternIndices) {
                int endPos = i;
                int startPos = endPos - patterns[patternIndex].length() + 1;
                results.add(new Pair<>(patternIndex, startPos));
            }
        }
        
        return results;
    }
    
    static class Pair<K, V> {
        K first;
        V second;
        
        public Pair(K first, V second) {
            this.first = first;
            this.second = second;
        }
    }
}
```

### 相关的 LeetCode 热门题目


- [28. 找出字符串中第一个匹配项的下标](https://leetcode.cn/problems/find-the-index-of-the-first-occurrence-in-a-string/) - 标准的字符串匹配问题
- [214. 最短回文串](https://leetcode.cn/problems/shortest-palindrome/) - 可以使用KMP算法的next数组思想解决
- [459. 重复的子字符串](https://leetcode.cn/problems/repeated-substring-pattern/) - 使用KMP的next数组判断字符串是否由重复子串构成
- [1392. 最长快乐前缀](https://leetcode.cn/problems/longest-happy-prefix/)

KMP算法是字符串处理中的经典算法，用来解决字符串匹配问题，理解它对提升算法设计能力还是很有帮助的。



<!-- @include: @article-footer.snippet.md -->     

