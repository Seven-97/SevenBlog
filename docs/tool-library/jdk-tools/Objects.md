---
title: 对象工具类 - Objects
category: 工具类库
tag:
  - Java
---





Java 的 Objects 类是一个实用工具类，包含了一系列静态方法，用于处理对象。它位于 java.util 包中，自 Java 7 引入。Objects 类的主要目的是降低代码中的空指针异常(NullPointerException) 风险，同时提供一些非常实用的方法供我们使用。

## 对象判空

在 Java 中，万物皆对象，对象的判空可以说无处不在。Objects 的 `isNull` 方法用于判断对象是否为空，而 `nonNull` 方法判断对象是否不为空。例如：

```java
Integer integer = new Integer(1);

if (Objects.isNull(integer)) {
    System.out.println("对象为空");
}

if (Objects.nonNull(integer)) {
    System.out.println("对象不为空");
}
```

## 对象为空时抛异常

如果我们想在对象为空时，抛出空指针异常，可以使用 Objects 的 `requireNonNull` 方法。例如：

```java
Integer integer1 = new Integer(128);

Objects.requireNonNull(integer1);
Objects.requireNonNull(integer1, "参数不能为空");
Objects.requireNonNull(integer1, () -> "参数不能为空");
```

## 判断两个对象是否相等

我们经常需要判断两个对象是否相等，Objects 给我们提供了 `equals` 方法，能非常方便的实现：

```java
Integer integer1 = new Integer(1);
Integer integer2 = new Integer(1);

System.out.println(Objects.equals(integer1, integer2));
```

执行结果：

```java
true
```

但使用这个方法有坑，比如例子改成：

```java
Integer integer1 = new Integer(1);
Long integer2 = new Long(1);

System.out.println(Objects.equals(integer1, integer2));
```

执行结果：

```java
false
```

不过，需要注意的是，虽然 `Objects.equals()` 方法本身是用来避免坑的，因为它可以处理 null 值的比较，而不会抛出空指针异常。然而，这并不意味着它没有任何潜在问题。实际上，`Objects.equals()` 方法的一个潜在问题是依赖于被比较对象的 `equals()` 方法实现。

当两个对象的类没有正确实现 `equals()` 方法时，`Objects.equals()` 方法可能会产生不符合预期的结果。举个例子：

```java
public class ObjectsDemo1 {
    public static void main(String[] args) {
        Person person1 = new Person("Seven", 18);
        Person person2 = new Person("Seven", 18);

        System.out.println(Objects.equals(person1, person2)); // 输出：false
    }
}
class Person {
    String name;
    int age;

    Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

在上面的例子中，我们创建了一个名为 Person 的类，但是没有重写 `equals()` 方法。然后我们创建了两个具有相同属性的 Person 对象，并使用 `Objects.equals()` 方法比较它们。尽管这两个对象的属性是相同的，但输出结果却是 false。这是因为 `Objects.equals()` 方法依赖于对象的 `equals()` 方法，而在这个例子中，Person 类没有正确地实现 `equals()` 方法，所以默认情况下会使用 Object 类的 `equals()` 方法，它只比较对象引用是否相同。

为了解决这个问题，需要在 Person 类中重写 `equals()` 方法：

```java
@Override
public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
        return false;
    }
    Person person = (Person) obj;
    return age == person.age && Objects.equals(name, person.name);
}
```

现在，当使用 `Objects.equals()` 方法比较两个具有相同属性的 Person 对象时，输出将是 true，符合我们的预期。

## 获取对象的hashCode

如果你想获取某个对象的 hashCode，可以使用 Objects 的 `hashCode` 方法。例如：

```java
String str = new String("Seven");
System.out.println(Objects.hashCode(str));
```

执行结果：

```java
867758096
```

## 比较两个对象

`compare()` 方法用于比较两个对象，通常用于自定义排序。它需要一个[比较器 (Comparator) ](https://javabetter.cn/basic-extra-meal/comparable-omparator.html)作为参数。如果比较器为 null，则使用自然顺序。以下是一个 `compare()` 方法的示例：

```java
class ObjectsCompareDemo {
    public static void main(String[] args) {
        PersonCompare person1 = new PersonCompare("itwanger", 30);
        PersonCompare person2 = new PersonCompare("chenqingyang", 25);

        Comparator<PersonCompare> ageComparator = Comparator.comparingInt(p -> p.age);
        int ageComparisonResult = Objects.compare(person1, person2, ageComparator);
        System.out.println("年龄排序: " + ageComparisonResult); // 输出：1（表示 person1 的 age 在 person2 之后）
    }
}

class PersonCompare {
    String name;
    int age;

    PersonCompare(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

## 比较两个数组

`deepEquals()` 用于比较两个 数组类型 的对象，当对象是非数组的话，行为和 `equals()` 一致。

```java
int[] array1 = {1, 2, 3};
int[] array2 = {1, 2, 3};
int[] array3 = {1, 2, 4};

System.out.println(Objects.deepEquals(array1, array2)); // 输出：true（因为 array1 和 array2 的内容相同）
System.out.println(Objects.deepEquals(array1, array3)); // 输出：false（因为 array1 和 array3 的内容不同）

// 对于非数组对象，deepEquals() 的行为与 equals() 相同
String string1 = "hello";
String string2 = "hello";
String string3 = "world";

System.out.println(Objects.deepEquals(string1, string2)); // 输出：true（因为 string1 和 string2 相同）
System.out.println(Objects.deepEquals(string1, string3)); // 输出：false（因为 string1 和 string3 不同）
```

再来个[二维数组](https://javabetter.cn/array/double-array.html)的：

```java
String[][] nestedArray1 = {{"A", "B"}, {"C", "D"}};
String[][] nestedArray2 = {{"A", "B"}, {"C", "D"}};
String[][] nestedArray3 = {{"A", "B"}, {"C", "E"}};

System.out.println(Objects.deepEquals(nestedArray1, nestedArray2)); // 输出：true (因为嵌套数组元素相同)
System.out.println(Objects.deepEquals(nestedArray1, nestedArray3)); // 输出：false (因为嵌套数组元素不同)
```

## 小结

除了上面提到的这些方法，Objects 还提供了一些其他的方法，比如说 toString，感兴趣的话可以试一下。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407282252773.jpeg)

总之，Objects 类提供的这些方法在许多情况下还是非常有用得，可以简化代码并减少出错的可能性

