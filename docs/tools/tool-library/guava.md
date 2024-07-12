---
title: Guava 用法&源码
category: 开发工具
tag:
 - 工具类库
---



## String相关工具

### Strings

Guava 提供了一系列用于字符串处理的工具：

#### 对字符串为null或空的处理

1. nullToEmpty(@Nullable String string)：如果非空，则返回给定的字符串；否则返回空字符串
   ```java
   public static String nullToEmpty(@Nullable String string) {
       //如果string为null则返回空字符串，否则返回给定的string
       return string == null ? "" : string;
   }
   ```

   

2. .isNullOrEmpty(@Nullable String string)：如果字符串为空或长度为0返回true，否则返回false
   ```java
   public static boolean isNullOrEmpty(@Nullable String string) {
       return string == null || string.length() == 0;
   }
   ```

   

3. emptyToNull(@Nullable String string)：如果非空，则返回给定的字符串；否则返回null
   ```java
   public static String emptyToNull(@Nullable String string) {
       //调用isNullOrEmpty方法，如果返回true则return null，否则返回原字符串
       return isNullOrEmpty(string)?null:string;
   }
   ```



#### 生成指定字符串的字符串副本

1. padStart(String string, int minLength, char padChar)：根据传入的minLength进行补充，如果minLength小于原来字符串的长度，则直接返回原来字符串，否则在字符串开头添加`string.length() - minLength`个padChar字符
   ```java
   public static String padStart(String string, int minLength, char padChar) {
       //使用Preconditions工具类进行字符串验空处理   
       Preconditions.checkNotNull(string);
       //如果原字符串长度大于传入的新长度则直接返回原字符串
       if(string.length() >= minLength) {
           return string;
       } else { //否则
           StringBuilder sb = new StringBuilder(minLength);
           //先在字符串前面添加string.length()-minLength个padChar字符
           for(int i = string.length(); i < minLength; ++i) {
               sb.append(padChar);
           }
           //最后将原始字符串添加到尾部
           sb.append(string);
           return sb.toString();
       }
   }
   ```

   

2. padEnd(String string, int minLength, char padChar)：根据传入的minLength进行补充，如果minLength小于原来字符串的长度，则直接返回原来字符串，否则在字符串结尾添加 `string.length() - minLength` 个padChar字符
   ```java
   public static String padEnd(String string, int minLength, char padChar) {
       Preconditions.checkNotNull(string);   
       //如果原字符串长度大于传入的新长度则直接返回原字符串
       if(string.length() >= minLength) {
           return string;
       } else {
           StringBuilder sb = new StringBuilder(minLength);
           //先将原始字符串添加到预生成的字符串当中
           sb.append(string);
           //在使用padChar进行填补
           for(int i = string.length(); i < minLength; ++i) {
               sb.append(padChar);
           }
           return sb.toString();
       }
   }
   ```

   

3. repeat(String string, int count)：返回count个 string字符串拼接成的字符串
   ```java
   public static String repeat(String string, int count) {
       Preconditions.checkNotNull(string);
       //如果小于1，则抛出异常
       if(count <= 1) {
           Preconditions.checkArgument(count >= 0, "invalid count: %s", new Object[]{Integer.valueOf(count)});
           return count == 0 ? "":string;
       } else {
           int len = string.length();
           long longSize = (long)len * (long)count;
           int size = (int)longSize;
           //如果新创建的字符串长度超出int最大值，则抛出需要的数组过长的异常
           if((long)size != longSize) {
               throw new ArrayIndexOutOfBoundsException((new StringBuilder(51)).append("Required array size too large: ").append(longSize).toString());
           } else {
               //实际上新建一个相当长度的字符数组，再将数据复制进去
               char[] array = new char[size];
               //将string从0开始len结束之间的字符串复制到array数组中，且array数组从0开始存储
               string.getChars(0, len, array, 0);
               int n;
               //复制数组，复制的步长为（1,2,4...n^2），所以这快提供了一个外层循环为ln2的算法
               for(n = len; n < size - n; n <<= 1) {
                   System.arraycopy(array, 0, array, n, n);
               }
               System.arraycopy(array, 0, array, n, size - n);
               return new String(array);
           }
       }
   }
   ```

   

#### 查找两个字符串的公共前缀或后缀

在看commonPrefix和commonSuffix 这两个方法之前需要先看下validSurrogatePairAt方法

```java
static boolean validSurrogatePairAt(CharSequence string, int index) {
    return index >= 0 && index <= string.length() - 2 && Character.isHighSurrogate(string.charAt(index)) && Character.isLowSurrogate(string.charAt(index + 1));
}
```

这个方法的作用是  判断最后两个字符是不是合法的“Java 平台增补字符

- Character.isHighSurrogate：确定给定char值是否为Unicode高位代理。这个值并不代表字符本身，而是在UTF-16编码的补充的字符的表示被使用。
- Character.isLowSurrogate：确定给定char值是否为一个Unicode低代理项代码单元（也称为尾部代理项代码单元）。这些值并不代表本身的字符，但用于表示增补字符的UTF-16编码。

> 简单的说就是Java 语言内部的字符信息是使用 UTF-16 编码。因为char 这个类型是 16 bit 的。它可以有65536种取值，即65536个编号，每个编号可以代表1种字符。而在Unicode字符集中，有一些字符的编码超出了16 bit的范围，也就是超过了`char`类型能够直接表示的范围，65536 就不够用。
>
> 为了能够在Java中表示这些字符，Unicode引入了一种叫做“代理对”（Surrogate Pair）的机制。从这65536个编号里，拿出2048个，规定它们是「Surrogates」，让它们两个为一组，来代表编号大于65536的那些字符。 更具体地，编号为 D800 至 DBFF 的规定为「High Surrogates」，共1024个。编号为 DC00至 DFFF 的规定为「Low Surrogates」，也是1024个。它们两两组合出现，就又可以多表示1048576种字符。
>
> 如果丢失一个高位代理Surrogates或者低位代理Surrogates，就会出现乱码。这就是为什么emoji会出现乱码了。例如输入了一个emoji:😆，假如可以写成这样：\uD83D\uDC34
>
> String s = '\uD83D' + '\uDC34' + "";
>
> 那么在按字节截取s的时候，就要考虑这个字符是不是高位代理Surrogates或者低位代理Surrogates，避免出现半个字符。



1. commonPrefix(CharSequence a, CharSequece b)：返回a和b两个字符串的公共前缀
   ```java
   public static String commonPrefix(CharSequence a, CharSequence b) {
       Preconditions.checkNotNull(a);
       Preconditions.checkNotNull(b);
       //将字符串a和字符串b两个中短的字符串长度赋值给maxPrefixLength
       int maxPrefixLength = Math.min(a.length(), b.length());
       int p;
       //遍历直到第一个两个字符不相等的位置，找出公共的前缀
       for(p = 0; p < maxPrefixLength && a.charAt(p) == b.charAt(p); ++p) {
           ;
       }
        //特殊情况：当最后一个匹配的字符是一个UTF-16编码的代理对的一部分时，需要把指针向前移动一位，以避免在返回结果时切断代理对，因为这将产生无效的Unicode序列
       if(validSurrogatePairAt(a, p - 1) || validSurrogatePairAt(b, p - 1)) {
           --p;
       }
       return a.subSequence(0, p).toString();
   }
   ```

   

2. commonSuffix(CharSequence a, CharSequence b)：返回字符串a和字符串b的公共后缀
   ```java
   public static String commonSuffix(CharSequence a, CharSequence b) {
       Preconditions.checkNotNull(a);
       Preconditions.checkNotNull(b);
       //将字符串a和字符串b两个中短的字符串长度赋值给maxPrefixLength
       int maxSuffixLength = Math.min(a.length(), b.length());
       int s;
       //遍历直到第一个两个字符不相等的位置，找出公共的后缀
       for(s = 0; s < maxSuffixLength && a.charAt(a.length() - s - 1) == b.charAt(b.length() - s - 1); ++s) {
           ;
       }
       if(validSurrogatePairAt(a, a.length() - s - 1) || validSurrogatePairAt(b, b.length() - s - 1)) {
           --s;
       }
       return a.subSequence(a.length() - s, a.length()).toString();
   }
   ```

   

### Joiner

将字符串数组按指定分隔符连接起来，或字符串串按指定索引开始使用指定分隔符连接起来，创建的**都是不可变实例，所以是线程安全的**。

底层实际是在用StringBuilder进行拼接操作。

#### 使用案例

```java
Joiner joiner = Joiner.on(";").useForNull("^");
// "A;B;^;D"
String joined = joiner.join("A", "B", null, "D");
```



#### 静态创建Joiner

```java
// 静态创建Joiner方法
public static Joiner on(String separator) {
    return new Joiner(separator);
}
 
public static Joiner on(char separator) {
    return new Joiner(String.valueOf(separator));
}
```

这两个方法一个传入字符串，一个传入字符，然后直接分别使用两个构造器构造



#### join()方法

对于4个join方法实际可以分为两类，一类是join实现类，另一类是join解析参数类

1. 解析参数类：

```java
//1. 因为 Iterable是所有集合类的顶级接口（除了Map系列），所以此参数为集合类或实现Iterable的类即可
public final String join(Iterable<?> parts) {
     //调用join实现类
     return this.join((Iterator)parts.iterator());
}
 
//2. 传入数组
public final String join(Object[] parts) {
     //将数组转为ArrayList然后强转为Iterable  
     return this.join((Iterable)Arrays.asList(parts));
}
 
//3. 传入两个参数和一个数组，最终这两个参数个数组一起构成一个新的数组
public final String join(@Nullable Object first, @Nullable Object second, Object... rest) {
     //使用iterable方法将参数和数组融合成一个数组
     return this.join((Iterable)iterable(first, second, rest));
}
```

第3个实现方法需要 iterable方法对数组进行融合，所以看一下 iterable的实现方式：

```java
private static Iterable<Object> iterable(final Object first, final Object second, final Object[] rest) {
    Preconditions.checkNotNull(rest);
    //返回一个AbstractList对象，并且这个对象重写了size和get方法
    return new AbstractList() {
        //使得当前容量比rest数组多2个
        public int size() {
        return rest.length + 2;
    }
 
    public Object get(int index) {
        switch(index) {
            case 0:
                return first;
            case 1:
                return second;
           default:
               return rest[index - 2];
        }
    }};  
}
```



2. join实现类

```java
public final String join(Iterator<?> parts) {
     //实际使用appendTo(StringBuilder,Iterator)方法
     return this.appendTo((StringBuilder)(new StringBuilder()), (Iterator)parts).toString();
}
 
public final StringBuilder appendTo(StringBuilder builder, Iterator<?> parts) {
     try {
         //调用了appendTo(A, Iterator)方法
         this.appendTo((Appendable)builder, (Iterator)parts);
         return builder;
     } catch (IOException var4) {
         throw new AssertionError(var4);
     }
}
 
public <A extends Appendable> A appendTo(A appendable, Iterator<?> parts) throws IOException {  
     Preconditions.checkNotNull(appendable);
     if(parts.hasNext()) {
         //如果第一个迭代器存在，将其添加到出传入的StringBuilder中
         appendable.append(this.toString(parts.next()));
         //从第二个迭代器开始就会循环方式就会发生变化，每个元素前都会添加规定的分隔符
         while(parts.hasNext()) {
             appendable.append(this.separator);
             appendable.append(this.toString(parts.next()));
         }
     }
     return appendable;
}
 
```



#### useForNull方法

将传入的字符串代替集合中的null输出

```java
public Joiner useForNull(final String nullText) {
    Preconditions.checkNotNull(nullText);
    // 返回一个Joiner重写了toString方法，将null的参数由 传入的nullText代替。
    return new Joiner(this, null) {
        CharSequence toString(@Nullable Object part) {
            return (CharSequence)(part == null ? nullText:Joiner.this.toString(part));
        }
 
        public Joiner useForNull(String nullTextx) {
            throw new UnsupportedOperationException("already specified useForNull");
        }
 
        public Joiner skipNulls() {
            throw new UnsupportedOperationException("already specified useForNull");
        }
    };
}
```

使用 useForNull方法后由于重写了useForNull和skipNulls方法，并且在两个方法中都抛出了异常。所以不能再次调用这两个方法。

**注意**：空字符串"" 无法命中这个方法，从源码中也可以看出来



#### skipNulls方法

自动跳过null元素进行拼接

```java
public Joiner skipNulls() {
    //返回一个Joiner，重写了appendTo方法，跳过null
    return new Joiner(this, null) {
        public <A extends Appendable> A appendTo(A appendable, Iterator<?> parts) throws IOException {
            Preconditions.checkNotNull(appendable, "appendable");
            Preconditions.checkNotNull(parts, "parts");
            Object part;
            // 与 原始appendTo方法不同的就是 多了 part != null 的判断语句
            while(parts.hasNext()) {
                part = parts.next();
                if(part != null) {
                    appendable.append(Joiner.this.toString(part));
                    break;
                }
            }
            // 给上面添加的 part 拼接上连接符 separator 
            while(parts.hasNext()) {
                part = parts.next(); 
                if(part != null) {
                    appendable.append(Joiner.this.separator);
                    appendable.append(Joiner.this.toString(part));
                }
            }
            return appendable;
        }
 
        public Joiner useForNull(String nullText) {
            throw new UnsupportedOperationException("already specified skipNulls");
        }
 
        public Joiner.MapJoiner withKeyValueSeparator(String kvs) {
            throw new UnsupportedOperationException("can\'t use .skipNulls() with maps");
        }
    };
}
```

**注意**：空字符串"" 无法命中这个方法，从源码中也可以看出来



#### 对Map解析的函数和类

```java
public Joiner.MapJoiner withKeyValueSeparator(String keyValueSeparator) {
    return new Joiner.MapJoiner(this, keyValueSeparator, null);
}
```

可以看到这个函数返回的是Joiner中的一个内部类，这个类里的大多方法都是在处理Map连接的函数

MapJoiner中的join方法实际上是对Map的entrySet即可以的集合进行拼接。整体思路与上面一致



### CharMatchers

字符序列匹配和处理的工具，内置了大量常用的匹配器。使用上通常分两步：

- 确定匹配的字符和模式
- 用匹配的字符做处理

```java
// 确定匹配的字符和模式，例如 anyOf, none, whitespace, digit, javaLetter, javaIsoControl...
CharMatcher matcher = CharMatcher.anyOf("abc");
// defg
log.debug("{}", matcher.removeFrom("abcdefg"));
// abc
log.debug("{}", matcher.retainFrom("abcdefg"));
// true
log.debug("{}", matcher.matchesAllOf("abc"));
// hhh 
log.debug("{}", matcher.trimFrom("abchhhabc"));
// ___hhh___
log.debug("{}", matcher.replaceFrom("abc hhh abc", "_"));
```



#### 实现类

| 实现类              | 类作用                                                      |
| ------------------- | ----------------------------------------------------------- |
| ANY                 | 匹配任何字符                                                |
| ASCII               | 匹配是否是ASCII字符                                         |
| BREAKING_WHITESPACE | 匹配所有可换行的空白字符(不包括非换行空白字符,例如"\u00a0") |
| JAVA_ISO_CONTROL    | 匹配ISO控制字符, 使用 Charater.isISOControl() 实现          |
| NONE                | 不匹配所有字符                                              |
| WHITESPACE          | 匹配所有空白字符                                            |



常用方法可分为4类：

#### 得到匹配指定规则的Matcher

```java
CharMatcher is(char match)：返回匹配指定字符的Matcher
CharMatcher isNot(char match)：返回不匹配指定字符的Matcher
CharMatcher anyOf(CharSequence sequence)：返回匹配sequence中任意字符的Matcher
CharMatcher noneOf(CharSequence sequence)：返回不匹配sequence中任何一个字符的Matcher
CharMatcher inRange(char startInclusive, char endInclusive)：返回匹配范围内任意字符的Matcher
CharMatcher negate()：返回当前Matcher相反的Matcher
CharMatcher and(CharMatcher other)：返回与other匹配条件组合做与来判断Matcher，即取两个Matcher的交集
CharMatcher or(CharMatcher other)：返回与other匹配条件组合做或来判断Matcher，即取两个Matcher的并集
```



#### 判断字符串是否匹配

```java
Boolean matchesAnyOf(CharSequence sequence)：只要sequence中有任意字符能匹配Matcher，返回true
Boolean matchesAllOf(CharSequence sequence)：sequence中所有字符能匹配Matcher，返回true
Boolean matchesNoneOf(CharSequence sequence)：sequence中所有字符都不能匹配Matcher，返回true
```



#### 获取字符串与Matcher匹配的位置信息

```java
int indexIn(CharSequence sequence): 返回sequence中匹配到的第一个字符的坐标
int indexIn(CharSequence sequence, int start): 返回从start开始,在sequence中匹配到的第一个字符的坐标
int lastIndexIn(CharSequence sequence): 返回sequence中最后一次匹配到的字符的坐标
int countIn(CharSequence sequence): 返回sequence中匹配到的字符计数
```



#### 对字符串进行怎样匹配处理

```java
String removeFrom(CharSequence sequence): 删除sequence中匹配到到的字符并返回
String retainFrom(CharSequence sequence): 保留sequence中匹配到的字符并返回
String replaceFrom(CharSequence sequence, char replacement): 替换sequence中匹配到的字符并返回
String trimFrom(CharSequence sequence): 删除首尾匹配到的字符并返回
String trimLeadingFrom(CharSequence sequence): 删除首部匹配到的字符
String trimTrailingFrom(CharSequence sequence): 删除尾部匹配到的字符
String collapseFrom(CharSequence sequence, char replacement): 将匹配到的组(连续匹配的字符)替换成replacement
String trimAndCollapseFrom(CharSequence sequence, char replacement): 先trim在replace
```



### Splitter

字符串分割工具，**创建的也是不可变实例，所以是线程安全的**。

底层用的是 String的subString方法

#### 使用案例

```java
// String#split 反直觉的输出：["", "a", "", "b"]
Arrays.stream(",a,,b,".split(",")).toList().forEach(System.out::println);
// ["foo", "bar", "qux"]
Iterable<String> split = Splitter.on(",")
    	// 结果自动 trim
        .trimResults()
    	// 忽略结果中的空串
        .omitEmptyStrings()
        // 限制分割数
        .limit(3)
        .split("foo,bar,,   qux");
Map<String, String> splitMap = Splitter.on(";")
	// 指定 K-V 的分隔符可以将键值对的串解析为 Map
	.withKeyValueSeparator("->")
	.split("A->1;B->2");
```



#### 两个内部类

在通读整片源码前先来了解其中的两个内部类，这两个内部类是真正去分解字符串的：

其中一个是 处理字符、字符串、正则的接口，此接口的定义实质为 [策略模式](https://www.seven97.top/system-design/design-pattern/behavioralpattern.html#策略模式)

```java
private interface Strategy {
    Iterator<String> iterator(Splitter var1, CharSequence var2);
}
```

这个接口中只有一个方法，返回的是一个Iterator迭代器，这里可以先联想到最终返回的集合的迭代器会与它有关系



这里实现了一个惰性迭代器：惰性迭代器就是指 直到不得不计算的时候才会去将字符串分割，即在迭代的时候才去分割字符串，无论将分隔符还是被分割的字符串加载到Splitter类中，都不会去分割，只有在迭代的时候才会**真正的去分割**。

```java
private abstract static class SplittingIterator extends AbstractIterator<String> {
    final CharSequence toSplit;
    final CharMatcher trimmer;
    final boolean omitEmptyStrings;
    int offset = 0;
    int limit;
    // 获取被分割字符串中第一个与分隔符匹配的位置
    abstract int separatorStart(int var1);
    // 获取当前分隔符在字符串中的结尾位置
    abstract int separatorEnd(int var1);
    // 构造函数，将当前的截取字符串信息赋值给SplittingIterator变量
    protected SplittingIterator(Splitter splitter, CharSequence toSplit) {
        this.trimmer = splitter.trimmer;
        this.omitEmptyStrings = splitter.omitEmptyStrings;
        this.limit = splitter.limit;
        this.toSplit = toSplit;
    }
 
    //重写迭代方法，就是这里实现的 惰性迭代器
    protected String computeNext() {
        int nextStart = this.offset;
        while(true) {
            while(this.offset != -1) {
                int start = nextStart;
                //根据separatorStart方法进行获取字符串中的第一个分隔符位置
                int separatorPosition = this.separatorStart(this.offset);
                int end;
                if(separatorPosition == -1) {
                    end = this.toSplit.length();
                    this.offset = -1;
                } else {
                    end = separatorPosition;
                    //根据separatorEnd方法进行获取字符串中的第一个分隔符的结束位置
                    this.offset = this.separatorEnd(separatorPosition);
                }
                if(this.offset != nextStart) {
                    while(start < end && this.trimmer.matches(this.toSplit.charAt(start))) {
                        ++start;
                    }
                    while(end > start && this.trimmer.matches(this.toSplit.charAt(end - 1))) {
                        --end;
                    }
                    //如果omitEmptyStrings为true，则对空结果跳过处理，否则进入
                    if(!this.omitEmptyStrings || start != end) {
                         //当规定的最多结果数值为1时，输出最后的所有字符串，然后结束迭代
                        if(this.limit == 1) {
                            end = this.toSplit.length();
                            for(this.offset = -1; end > start && this.trimmer.matches(this.toSplit.charAt(end - 1)); --end) {
                                ;
                            }
                        } else {
                            //没有到最后一个时，进行减1操作
                            --this.limit;
                        }
                        return this.toSplit.subSequence(start, end).toString();
                    }
                    nextStart = this.offset;
                } else {
                    ++this.offset;
                    if(this.offset > this.toSplit.length()) {
                        this.offset = -1;
                    }
                }
            }
            return (String)this.endOfData();
        }
    }
}
```

这是一个实现AbstractIterator的一个抽象类，实现了 computeNext方法（此方法可以在看集合源码的时候也多注意一下），这个方法实际上是规定了此迭代器的一个迭代规则。所以Splitter类为他分割完的结果集也写了一个迭代器并规定了自己的迭代规则。从这个迭代器的实现上，在结合Strategy 类便可以将整个字符串分割的过程给串起来了。



除了这两个，还有一个内部类是 MapSplitter，是用于处理Map，对Map进行分割的

#### 变量

```java
//移除指定字符项，即集合中当前元素与trimmer匹配，将其移除。如果没有设置trimmer，则将结果中的空格删除
//最终结论为：将结果集中的每个字符串前缀和后缀都去除trimmer，直到前缀或后缀没有这个字符了，字符串“中”的不用去除
private final CharMatcher trimmer;

//是否移除结果集中的空集，true为移除结果集中的空集，false为不用移除结果集中的空集
private final boolean omitEmptyStrings;

//这个变量最终会返回一个所有集合类的父接口，它是贯穿着整个字符串分解的变量
private final Splitter.Strategy strategy;

//最多将字符串分为几个集合，比如limit=3, 对”a,b,c,d”字符串进行'，'分割，返回的[”a”,”b”,”c,d”] 意思为最多可以分割成3段，这个可以在链式编程的limit方法参数设置
private final int limit;
```



#### 静态创建Splitter

两个构造函数都是私有构造器，所以不能直接使用这两个构造器去创建Splitter，想要创建Splitter只能使用静态方法。

```java
//接收 一个Strategy类对象
private Splitter(Splitter.Strategy strategy) {
    this(strategy, false, CharMatcher.NONE, 2147483647);
}
 
//此构造器为所有变量进行赋值
private Splitter(Splitter.Strategy strategy, boolean omitEmptyStrings, CharMatcher trimmer, int limit) {
    this.strategy = strategy;
    this.omitEmptyStrings = omitEmptyStrings;
    this.trimmer = trimmer;
    this.limit = limit;
}
```



静态创建Splitter函数有四种：

1. 接收**字符**的构造器
   ```java
   //接收一个字符的构造器，然后调用参数为 CharMatcher的构造器
   public static Splitter on(char separator) {
       return on((CharMatcher)CharMatcher.is(separator));
   }
    
   //接收一个CharMatcher的构造器
   public static Splitter on(final CharMatcher separatorMatcher) {
       //对字符判空
       Preconditions.checkNotNull(separatorMatcher);
       //返回一个Splitter对象，传入Strategy对象，并对Strategy接口进行实现
       return new Splitter(new Splitter.Strategy() {
           //实现接口Strategy的iterator方法
           public Splitter.SplittingIterator iterator(final Splitter splitter, final CharSequence toSplit) {
               //返回 SplittingIterator对象，并对 SplittingIterator 抽象类实现 separatorStart方法和 separatorEnd方法
               return new Splitter.SplittingIterator(splitter, toSplit) {
                   //返回从start 开始的第一个分隔符的开始位置
                   int separatorStart(int start) {
                       return separatorMatcher.indexIn(this.toSplit, start);
                   }
                   //返回当前分割符的末尾位置
                   int separatorEnd(int separatorPosition) {
                       return separatorPosition + 1;
                   }
               };
           }
       });
   }
   ```

   

2. 接收**字符串**的构造器
   ```java
   //传入一个字符串作为分隔符
   public static Splitter on(final String separator) {
       Preconditions.checkArgument(separator.length() != 0, "The separator may not be the empty string.");
       //如果当前字符串的长度为1，则直接调用解析单个字符的构造器上，否则会返回一个Splitter对象，传入Strategy对象，并对Strategy接口进行实现
       return separator.length() ==  1 ? on(separator.charAt(0)) : new Splitter(new Splitter.Strategy() {
           //实现Strategy接口
           public Splitter.SplittingIterator iterator(final Splitter splitter, final CharSequence toSplit) {
               return new Splitter.SplittingIterator(splitter, toSplit) {
                   //这个方法是被分割字符串从start开始，找到第一个分隔符的位置，没有找到返回-1
                   public int separatorStart(int start) {
                       //获取分割符长度
                       int separatorLength = separator.length();
                       //记录分割符开始位子
                       int p = start;
                       
                       label24:
                       //last是调用本类的 toSplit 变量即被分割的字符串长度，last取被分割的字符串长度与分割符的差值
                       //假设分割符号”,” 被分割字符串”a,b,c,d” last= 7-1 = 6
                       for(int last = this.toSplit.length() - separatorLength; p <= last; ++p) {
                           for(int i = 0; i < separatorLength; ++i) {
                               //找到匹配到分隔符的第一个位置
                               if (this.toSplit.charAt(i + p) != separator.charAt(i)) {
                                   continue label24;
                               }
                           }
                           return p;
                       }
                       return -1;
                   }
                   //传入分离器位置，返回分离器末尾位置
                   public int separatorEnd(int separatorPosition) {
                       return separatorPosition + separator.length();
                   }
               };
           }
       });
   }
   ```

   

3. 接收**正则表达式**的构造器

   > @GwtIncompatible用于指示某个类、方法或字段不兼容或不应被用于Google Web Toolkit (GWT)。GWT是一个开发工具，允许开发者编写Java代码，然后将这些代码编译成高效的JavaScript代码，以便在浏览器中运行。这个工具使得Java开发者可以编写前端代码，而不需要直接使用JavaScript。

   ```java
   //传入一个字符串，返回一个调用传入CommonPattern类型的on方法
   @GwtIncompatible
   public static Splitter onPattern(String separatorPattern) {
       return on((CommonPattern)Platform.compilePattern(separatorPattern));
   }
    
   //传入一个Pattern类型参数，返回一个调用传入CommonPattern类型的on方法
   @GwtIncompatible
   public static Splitter on(Pattern separatorPattern) {
       return on((CommonPattern)(new JdkPattern(separatorPattern)));
   }
    
   //传入一个 CommonPattern类型的构造器
   private static Splitter on(final CommonPattern separatorPattern) {
       Preconditions.checkArgument(!separatorPattern.matcher("").matches(), "The pattern may not match the empty string: %s", separatorPattern);
       //返回一个Splitter对象，传入Strategy对象，并对Strategy接口进行实现
       return new Splitter(new Splitter.Strategy() {
           //实现Strategy对象的iterator方法
           public Splitter.SplittingIterator iterator(final Splitter splitter, final CharSequence toSplit) {
               final CommonMatcher matcher = separatorPattern.matcher(toSplit);
               return new Splitter.SplittingIterator(splitter, toSplit) {
                   //返回从start开始的第一个分隔符的开始位置
                   public int separatorStart(int start) {
                       return matcher.find(start)?matcher.start():-1;
                   }
                   //返回当前分割符的末尾位置
                   public int separatorEnd(int separatorPosition) {
                       return matcher.end();
                   }
               };
           }
       });
   }
   ```

   

4. 按指定长度分割的构造器
   ```java
   public static Splitter fixedLength(final int length) {
       Preconditions.checkArgument(length > 0, "The length may not be less than 1");
       return new Splitter(new Splitter.Strategy() {
           public Splitter.SplittingIterator iterator(final Splitter splitter, final CharSequence toSplit) {
               return new Splitter.SplittingIterator(splitter, toSplit) {
                   //按 length长度进行跨步
                   public int separatorStart(int start) {
                       int nextChunkStart = start + length;
                       return nextChunkStart < this.toSplit.length()?nextChunkStart:-1;
                   }
                   public int separatorEnd(int separatorPosition) {
                       return separatorPosition;
                   }
               };
           }
       });
   }
   ```

   



#### 进行分割的函数 （split、splittingIterator）

1. 返回值是 Iterable 的函数：

   ```java
   public Iterable<String> split(final CharSequence sequence) {
       Preconditions.checkNotNull(sequence);
       //返回一个容器，然后重写了iterator和toString方法
       return new Iterable() {
           public Iterator<String> iterator() {
               //调用了 splittingIterator方法
               return Splitter.this.splittingIterator(sequence);
           }
    
           //重写了toString方法，将字符串用","隔开，并在前后用中括号包裹
           public String toString() {
               return Joiner.on(", ").appendTo((new StringBuilder()).append('['), this).append(']').toString();
           }
       };
   }
   
   private Iterator<String> splittingIterator(CharSequence sequence) {
       return this.strategy.iterator(this, sequence);
   }
   ```

   这里调用了Strategy的iterator方法，这个方法在 静态创建Splitter中 里面有多种的实现方法，再结合内部类中的 SplittingIterator类重写的迭代方法，这里就形成了一个特殊的容器返回。也就是说，真正的拆分字符串动作是在迭代的时候进行的，即在这个函数中进行的。







2. 返回值是List对象的函数

   ```java
   public List<String> splitToList(CharSequence sequence) {
       Preconditions.checkNotNull(sequence);
       Iterator iterator = this.splittingIterator(sequence);
       ArrayList result = new ArrayList();
       while(iterator.hasNext()) {
           result.add(iterator.next());
       }
       return Collections.unmodifiableList(result);
   }
   ```

   与上面一样是先调用了Strategy的iterator方法。再遍历将结果集放在了ArrayList容器中，再返回不可变的List。



#### 其它功能性方法

1. omitEmptyStrings方法：移去结果中的空字符串
   ```java
   public Splitter omitEmptyStrings() {
       return new Splitter(this.strategy, true, this.trimmer, this.limit);
   }
   ```

   这里就是将omitEmptyStrings标记位改为true，在computeNext方法中进行输出操作时将空结果略过



2. trimResults方法
   ```java
   //未输入参数的情况下，默认是将结果中的空格删除
   public Splitter trimResults() {
       return this.trimResults(CharMatcher.whitespace());
   }
    
   //输入参数，则是移除指定字符
   public Splitter trimResults(CharMatcher trimmer) {
       Preconditions.checkNotNull(trimmer);
       return new Splitter(this.strategy, this.omitEmptyStrings, trimmer, this.limit);
   }
   ```

   调用此方法可以将结果集中的每个字符串前缀和后缀都去除trimmer，他的实现也是在computeNext方法中进行的



3. limit方法：达到指定数目后停止字符串划分
   ```java
   public Splitter limit(int limit) {
       Preconditions.checkArgument(limit > 0, "must be greater than zero: %s", limit);
       return new Splitter(this.strategy, this.omitEmptyStrings, this.trimmer, limit);
   }
   ```

   将传入的limit值赋值给变量



## 通用/其它工具

### Preconditions

提供静态方法来检查方法或构造函数。如果方法失败则抛出 NullPointerException。

对于Preconditions类的静态方法可以分为三大类：

JDK 7 开始提供的 Objects 类也提供了一些类似的功能，具体可以参考 [JDK Doc](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Objects.html)。

#### 对null的处理

```java
public static <T> T checkNotNull(T reference)
public static <T> T checkNotNull(T reference, @Nullable Object errorMessage)
public static <T> T checkNotNull(T reference, @Nullable String errorMessageTemplate, @Nullable Object... errorMessageArgs)
```

根据三个方法的描述，reference是要进行判断的引用，接下来的信息是自定义异常打印信息

```java
public static <T> T checkNotNull(T reference) {
    //如果引用为null，则直接抛出异常，如果不为null，则直接返回这个引用
    if(reference == null) {
        throw new NullPointerException();
    } else {
        return reference;
    }
}
```



#### 对真假的处理

对真假的判断实现有两种实现函数checkArgument()和checkState()

```java
public static void checkArgument(boolean expression) {
    if (!expression) {
        throw new IllegalArgumentException();
    }
}

public static void checkState(boolean expression) {
    if (!expression) {
        throw new IllegalStateException();
    }
}
```

两个函数的实现大体一致，都是对传入的boolean类型参数进行判断

如果为false

- checkArgument方法会抛出IllegalArgumentException()
- checkState方法会抛出IllegalStateException()



#### 对数组下标是否符合的处理

对数组下标合格的判断有三种方法checkElementIndex()、checkPositionIndex()、checkPositionIndexs()

```java
checkElementIndex(int index, int size)：判断0 - size-1中是否存在index
checkPositionIndex(int index, int size)：判断 0 - size 中是否存在index
checkPositionIndexs(int start, int end, int size)：判断传入的start和end与size的顺序关系并且start和end是否在 0 ~ size 范围中
```



```java
@CanIgnoreReturnValue
public static int checkElementIndex(int index, int size) {
    return checkElementIndex(index, size, "index");
}

@CanIgnoreReturnValue
public static int checkElementIndex(int index, int size, String desc) {
    if (index >= 0 && index < size) {
        return index;
    } else {
        throw new IndexOutOfBoundsException(badElementIndex(index, size, desc));
    }
}
```



### Ints

1. compare(int a, int b)：比较两个指定的int值

   ```java
   public static int compare(int a, int b) {
       return a < b ? -1 : (a > b ? 1 : 0);
   }
   ```

   

2. asList(int... backingArray)：返回由指定数组支持的固定大小的列表，类似Arrays.asList(Object[]).

   ```java
   public static List<Integer> asList(int... backingArray) {
       return (List)(backingArray.length == 0?Collections.emptyList():new Ints.IntArrayAsList(backingArray));
   }
   ```

由源码可以看到，如果传入的参数长度为0，那么就会创建一个Collections.emptyList()，如果参数长度不为0，那么就会创建一个Ints的内部类IntArrayAsList。

**特殊说明**：Ints的asList与JDK的Arrays.asLis的不同点：

- Arrays.asList(Object[])返回的是一个List<数组>，而Ints的asList返回的是List&lt;Integer>。

- Ints的asList返回的是内部的一个不可变的List，没有重写add方法，因此执行add操作会抛异常。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407111711922.png)



3. contains(int[] array, int target)：如果array中存在target返回true，反之返回false

   ```java
   public static boolean contains(int[] array, int target) {
       int[] var2 = array;
       int var3 = array.length;
   
       for(int var4 = 0; var4 < var3; ++var4) {
           int value = var2[var4];
           //循环查找array中与target相等的元素，如果有相等的直接返回true
           if (value == target) {
               return true;
           }
      }
   
      return false;
   }
   ```

   







### 其它小工具

1. **Charsets**: 提供了6种标准的字符集常量引用，例如`Charsets.UTF_8`。JDK 7 以后建议使用内置的 `StandardCharsets`

2. **CaseFormat**: 大小写转换的工具

```java
// UPPER_UNDERSCORE -> LOWER_CAMEL
CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, "CONSTANT_NAME"));
```

| **Format**                                                   | **Example**      |
| ------------------------------------------------------------ | ---------------- |
| [LOWER_CAMEL](http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/base/CaseFormat.html#LOWER_CAMEL) | lowerCamel       |
| [LOWER_HYPHEN](http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/base/CaseFormat.html#LOWER_HYPHEN) | lower-hyphen     |
| [LOWER_UNDERSCORE](http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/base/CaseFormat.html#LOWER_UNDERSCORE) | lower_underscore |
| [UPPER_CAMEL](http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/base/CaseFormat.html#UPPER_CAMEL) | UpperCamel       |
| [UPPER_UNDERSCORE](http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/base/CaseFormat.html#UPPER_UNDERSCORE) | UPPER_UNDERSCORE |







## 集合

### Immutable

在程序设计中使用不可变对象，可以提高代码的可靠性和可维护性，其优势包括：

1. **线程安全性（Thread Safety）**：不可变对象是线程安全的，无需同步操作，避免了竞态条件
2. **安全性**：可以防止在程序运行时被意外修改，提高了程序的安全性
3. **易于理解和测试**：不可变对象在创建后不会发生变化，更容易理解和测试
4. **克隆和拷贝**：不可变对象不需要实现可变对象的复制（Clone）和拷贝（Copy）逻辑，因为它们的状态不可变，克隆即是自己

JDK 的 Collections 提供了 Unmodified Collections 不可变集合，但仅仅是通过装饰器模式提供了一个只读的视图，并没有阻止对原始集合的修改操作，并且效率较低。
而 Guava 提供的不可变集合更加简单高效，确保了真正的不可变性。

```java
// copyOf 会尝试在安全的时候避免做拷贝
ImmutableList<String> immutableList1 = ImmutableList.copyOf(origin);
ImmutableList<String> immutableList2 = ImmutableList.of("A", "B", "C");
ImmutableList<String> immutableList3 = ImmutableList.<String>builder()
                                                .add("A", "B", "C")
                                                .build();

// 任何不可变集合可以转变为 ImmutableList，且该列表视图通常性能很好
ImmutableList<String> list = immutable.asList();
```

### 新集合类型

#### Multiset

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

#### Multimap

支持将 key 映射到多个 value 的方式，而不用定义`Map<K, List<V>> 或 Map<K, Set<V>>`这样的哈皮形式。实现类包括`ArrayListMultimap, HashMultimap, LinkedListMultimap, TreeMultimap...`

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

#### BiMap

Map 可以实现 key -> value 的映射，如果想要 value -> key 的映射，就需要定义两个 Map，并且同步更新，很不优雅。Guava 提供了 BiMap 支持支持双向的映射关系，常用实现有`HashMap, EnumBiMap, EnumHashBiMap...`

```java
BiMap<String, Integer> biMap = HashBiMap.create();
biMap.put("A", 100);

// 删除已存在的 KV，重新添加 KV
biMap.forcePut("A", 200);

// 获取反向映射
BiMap<Integer, String> inverse = biMap.inverse();
log.debug("{}", inverse.get(100));
```

#### Table

当需要同时对多个 key 进行索引时，需要定义`Map<key1, Map<key2, val>>`这样的形式，ugly and awkward。Guava 提供了 Table 用于支持类似 row、column 的双键映射。实现包括`HashBasedTable, TreeBasedTable, ArrayTable...`

```java
// Table<R, C, V>
Table<String, String, Integer> table = HashBasedTable.create();
table.put("row1", "col1", 1);
table.put("row1", "col2", 2);
table.put("row2", "col1", 3);
table.put("row2", "col2", 4);

// 获取类似 Map.Entry 的 Table.Cell<R, C, V>
table.cellSet().forEach(System.out::println);

// Map<R, Map<C, V>> 视图
Map<String, Map<String, Integer>> rowMap = table.rowMap();
Set<String> rowKeySet = table.rowKeySet();
// Map<C, Map<R, V>> 视图（基于列的索引效率通常比行低）
Map<String, Map<String, Integer>> columnMap = table.columnMap();
Set<String> columnedKeySet = table.columnKeySet();
```

#### 其它

- ClassToInstanceMap：Class -> Instance 的映射，可以避免强制类型转换
- RangeSet (Beta状态)：范围集合，自动合并、分解范围区间

```java
RangeSet<Integer> rangeSet = TreeRangeSet.create();
rangeSet.add(Range.closed(1, 10)); // {[1, 10]}
rangeSet.add(Range.closedOpen(11, 15)); // disconnected range: {[1, 10], [11, 15)}
rangeSet.add(Range.closedOpen(15, 20)); // connected range; {[1, 10], [11, 20)}
rangeSet.add(Range.openClosed(0, 0)); // empty range; {[1, 10], [11, 20)}
rangeSet.remove(Range.open(5, 10)); // splits [1, 10]; {[1, 5], [10, 10], [11, 20)}

// [[1..5], [10..10], [11..20)]
System.out.println(rangeSet);
```

- RangeMap (Beta状态)：range -> value 的映射，不会自动合并区间

```java
RangeMap<Integer, String> rangeMap = TreeRangeMap.create();
rangeMap.put(Range.closed(1, 10), "foo"); // {[1, 10] => "foo"}
rangeMap.put(Range.open(3, 6), "bar"); // {[1, 3] => "foo", (3, 6) => "bar", [6, 10] => "foo"}
rangeMap.put(Range.open(10, 20), "foo"); // {[1, 3] => "foo", (3, 6) => "bar", [6, 10] => "foo", (10, 20) => "foo"}
rangeMap.remove(Range.closed(5, 11)); // {[1, 3] => "foo", (3, 5) => "bar", (11, 20) => "foo"}

// [[1..3]=foo, (3..5)=bar, (11..20)=foo]
System.out.println(rangeMap);
```

### 集合工具

JDK 自带的`java.util.Collections`提供了很多实用的功能，而 Guava 针对特定接口提供了更多工具，例如：

- 提供很多静态工厂方法、包装器
- Iterables 支持懒加载的集合视图操作
- Sets 提供集合论运算
- Maps 提供 diff 计算
- Multimaps 提供 Map -> Multimap 的转换，并支持反向映射
- Tables 提供行列转置 | **Interface** | **JDK or Guava** | **Guava Utility Class** | | --- | --- | --- | | Collection | JDK | Collections2 | | List | JDK | Lists | | Set | JDK | Sets | | Map | JDK | Maps | | Queue | JDK | Queues | | Multiset | Guava | Multisets | | Multimap | Guava | Multimaps | | BiMap | Guava | Maps | | Table | Guava | Tables |

另外，还可以继承 Forwarding 通过装饰器模式装饰特殊实现，PeekingIterator 可以 peek 下一次返回的元素，AbstractIterator 自定义迭代方式等等。（PS：这些东西也许有点用，可是能用吗，业务里写这种代码怕是要被人喷死...）

## 缓存

Guava 提供的 Cache 按照 **有则取值，无则计算 **的逻辑，支持自动装载，自动移除等扩展功能，比传统的`ConcurrentHashMap`功能更加强大。

### 使用

除了继承 AbstractCache 自定义缓存实现外，通常可以直接使用 CacheBuilder 构建一个 LocalLoadingCache 缓存，通过 key 来懒加载相关联的 value。如果 key-value 没有关联关系可以使用无参的 build 方法返回一个`LocalManualCache`对象，等调用 get 时再传递一个 callable 对象获取 value。

```java
LoadingCache<String, Integer> loadingCache = CacheBuilder.newBuilder()
                // 最大容量
				.maximumSize(1000)
            	// 定时刷新
				.refreshAfterWrite(10, TimeUnit.MINUTES)
                // 添加移除时的监听器
                .removalListener(notification -> System.out.println(
                    notification.getKey() + " -> " + 
                    notification.getValue() + " is removed by " + 
                    notification.getCause()))
            	// 并发等级
				.concurrencyLevel(4)
				// 开启统计功能
            	.recordStats()
                .build(
                        new CacheLoader<String, Integer>() {
                            @Override
                            public Integer load(String key) {
                                System.out.println("Loading key: " + key);
                                return Integer.parseInt(key);
                            }
                        });

loadingCache.get("key");
// 使失效
loadingCache.invalidate("key");
// get with callable
loadingCache.get("key", () -> 2));
// 刷新缓存
loadingCache.refresh("key");
// 返回一个 map 视图
ConcurrentMap<String, Integer> map = loadingCache.asMap();
```

`LocalLoadingCache`继承自 `LocalManualCache`，里面封装了一个继承自 Map 的`localCache`成员存储实际的 KV 并通过分段锁实现线程安全，另外实现了 Cache 接口定义了一系列缓存操作。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406262329345.jpeg)

```java
class LocalCache<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>
```

### 失效

Guava 提供了三种基础的失效策略：

- 基于容量失效：
  - `maximumSize(long)` 指定最大容量
  - `weigher(Weigher)`实现自定义权重，配合`maximumWeight(long)`以权重作为容量
- 基于时间失效：在写入、偶尔读取期间执行定期维护
  - `expireAfterAccess(long, timeUnit)` 读写后超出指定时间即失效
  - `expireAfterWrite(long, timeUnit)` 写后超出指定时间即失效
- 基于引用失效：
  - `weakKeys()`通过弱引用存储 key
  - `weakValues()`通过弱引用存储 value
  - `softValues()` 通过软引用包装 value，以 LRU 方式进行 GC

另外，也可以通过`invalidate`手动清除缓存。缓存不会自动进行清理，Guava 会在写操作期间，或者偶尔在读操作时进行过期失效的维护工作。缓存刷新操作的时机也是类似的。

详情可以看这篇文章 [GuavaCache](https://www.seven97.top/system-design/cache-column/guavacache.html)

## 图

`com.google.common.graph`提供了多种图的实现：

- **Graph** 边是匿名的，且不关联任何信息
- **ValueGraph** 边拥有自己的值，如权重、标签
- **Network **边对象唯一，且期望实施对其引用的查询

整体看下来，感觉还是挺复杂的，不太常用。等有需要再学习吧...

## 并发

### ListenableFuture

JUC 的 Future 接口提供了一种异步获取任务执行结果的机制，表示一个异步计算的结果。

```java
ExecutorService executor = Executors.newFixedThreadPool(1);
Future<String> future = executor.submit(() -> {
    // 执行异步任务，返回一个结果
    return "Task completed";
});
// Blocked
String result = future.get();
```

Executor 实际返回的是实现类 FutureTask，它同时实现了 Runnable 接口，因此可以手动创建异步任务。

```java
FutureTask<String> futureTask = new FutureTask<>(new Callable<String>() {
    @Override
    public String call() throws Exception {
        return "Hello";
    }
});
        
new Thread(futureTask).start();
System.out.println(futureTask.get());
```

而 Guava 提供的 ListenableFuture 更进一步，允许注册回调，在任务完成后自动执行，实际也是使用它的实现类 ListenableFutureTask。

```java
// 装饰原始的线程池
ListeningExecutorService listeningExecutorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
ListenableFuture<String> future = listeningExecutorService.submit(() -> {
    // int i = 1 / 0;
    return "Hello";
});

// 添加回调 1
Futures.addCallback(future, new FutureCallback<String>() {
    // 任务成功时的回调
    @Override
    public void onSuccess(String result) {
        System.out.println(result);
    }

    // 任务失败时的回调
    @Override
    public void onFailure(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }
}, listeningExecutorService);

// 添加回调 2
future.addListener(new Runnable() {
    @Override
    public void run() {
        System.out.println("Done");
    }
}, listeningExecutorService);
```

### Service

Guava 的 Service 框架是一个用于管理服务生命周期的轻量级框架。它提供了一个抽象类 AbstractService 和一个接口 Service，可以通过继承 AbstractService 或者直接实现 Service 接口来创建自定义的服务，并使用 ServiceManager 来管理这些服务的生命周期。

```java
public class MyService extends AbstractService {
    @Override
    protected void doStart() {
        // 在这里执行启动服务的逻辑
        System.out.println("MyService is starting...");
        notifyStarted();
    }
    
    @Override
    protected void doStop() {
        // 在这里执行停止服务的逻辑
        System.out.println("MyService is stopping...");
        notifyStopped();
    }
}


@Test
public void testService() {
    Service service = new MyService();
    ServiceManager serviceManager = new ServiceManager(List.of(service));
    
    serviceManager.startAsync().awaitHealthy();
    
    // 主线程逻辑
    
    serviceManager.stopAsync().awaitStopped();
}
```



## 基本类型工具

Java 的基本类型包括8个：byte、short、int、long、float、double、char、boolean。Guava 提供了若干工具以支持基本类型和集合 API 的交互、字节数组转换、无符号形式的支持等等。

| **基本类型** | **Guava 工具类**                    |
| ------------ | ----------------------------------- |
| byte         | Bytes, SignedBytes, UnsignedBytes   |
| short        | Shorts                              |
| int          | Ints, UnsignedInteger, UnsignedInts |
| long         | Longs, UnsignedLong, UnsignedLongs  |
| float        | Floats                              |
| double       | Doubles                             |
| char         | Chars                               |
| boolean      | Booleans                            |

## Range

Guava 提供了 Range 类以支持范围类型，并且支持范围的运算，比如包含、交集、并集、查询等等。

```java
Range.open(a, b);			// (a, b)
Range.closed(a, b);			// [a..b]
Range.closedOpen(a, b);		// [a..b)
Range.openClosed(a, b);		// (a..b]
Range.greaterThan(a);		// (a..+∞)
Range.atLeast(a);			// [a..+∞)
Range.lessThan(a);			// (-∞..b)
Range.atMost(a);			// (-∞..b]
Range.all();				// (-∞..+∞)

// 通用创建方式
Range.range(a, BoundType.CLOSED, b, BoundType.OPEN);
```



## IO

Guava 使用术语 **流 **来表示可关闭的，并且在底层资源中有位置状态的 I/O 数据流。字节流对应的工具类为 ByteSterams，字符流对应的工具类为 CharStreams。
Guava 中为了避免和流直接打交道，抽象出可读的 **源 source** 和可写的 **汇 sink** 两个概念，指可以从中打开流的资源，比如 File、URL，同样也分别有字节和字符对应的源和汇，定义了一系列读写的方法。
除此之外，Files 工具类提供了对文件的操作。（PS：个人觉得，JDK 的 IO 流已经够麻烦的了，又来一套 API 太乱了，而且也没有更好用吧。还是先统一用 JDK 标准库里的好一点。）



## Hash

JDK 内置的哈希限定为 32 位的 int 类型，虽然速度很快但质量一般，容易产生碰撞。为此 Guava 提供了自己的 Hash 包。

- `Hashing` 类内置了一系列的散列函数对象 `HashFunction`，包括 `murmur3, sha256, adler32, crc32`等等。
- 确定 HashFunction 后进而拿到继承自 PrimitiveSink 的 Hasher 对象。
- 作为一个汇，可以往 Hasher 里输入数据，可以是内置类型，也可以是自定义类型，但需要传递一个 Funnel 定义对象分解的方式。
- 最后计算得到 HashCode 对象。

```java
HashFunction hf = Hashing.adler32();

User user = new User("chanper", 24);
HashCode hash = hf.newHasher()
        .putLong(20L)
        .putString("chanper", StandardCharsets.UTF_8)
    	// 输入自定义类，同时需要一个 Funnel
		.putObject(user, userFunnel)
        .hash();

// Funnel 定义对象分解的方式
Funnel<User> userFunnel = new Funnel<>() {
    @Override
    public void funnel(User user, PrimitiveSink into) {
        into
            .putString(user.name(), StandardCharsets.UTF_8)
            .putInt(user.age());
    }
};
```

另外，Guava 库也内置了一个使用简便布隆过滤器。

```java
// funnel 对象，预期的插入数量，false positive probability
BloomFilter<User> friends = BloomFilter.create(userFunnel, 500, 0.01);
for (int i = 0; i < 1000; i++) {
    friends.put(new User("user_" + i, 24));
}

if(friends.mightContain(somebody)) {
    System.out.println("somebody is in friends");
}
```

## EventBus

EventBus 是 Guava 提供的一个事件总线库，用于简化组件之间的通信。通过 EventBus，你可以实现发布/订阅模式，组件之间可以松散地耦合，使得事件的发布者（Producer）和订阅者（Subscriber）之间不需要直接依赖彼此。 使用时注意：

- 一个订阅者可以处理多个不同的事件，取决于处理方法的参数，并且支持泛型的通配符
- 没有对应的监听者则会把事件封装为`DeadEvent`，可以定义对应的监听器

```java
// 事件类型
public record MessageEvent(String message) {}

public class MessageSubscriber {
    // 事件处理方法标记
    @Subscribe
    public void handleMessageEvent(MessageEvent event) {
        System.out.println("Received message: " + event.message());
    }

    // 一个订阅者可以处理多个事件
    @Subscribe
    public void handleMessageEvent(MessageEvent2 event2) {
        System.out.println("Received message2: " + event2.message());
    }
}

@Test
public void testEvnetBus() {
    // 创建EventBus实例
    EventBus eventBus = new EventBus();
    
    // 注册订阅者
    MessageSubscriber subscriber = new MessageSubscriber();
    eventBus.register(subscriber);
    
    // 发布事件
    MessageEvent event = new MessageEvent("Hello, EventBus!");
    eventBus.post(event);
}
```