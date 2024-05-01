---
title: Sentinel - 源码
category: 微服务
tag:
 - Sentinel
---





## 请求限流

### 滑动窗口的实现

#### 实现

##### LeapArray

Sentinel 中滑动窗口算法的核心类，先了解一下他的核心成员变量

```java
public abstract class LeapArray<T> {
 //毫秒时间周期，默认60*1000，例如计算QPS时，为1000
    protected int intervalInMs;
 //窗口数量，默认60
    protected int sampleCount;
//窗口时间长度，毫秒数，默认1000ms 该值 = intervalInMs / sampleCount
    protected int windowLengthInMs;
 
 // 存储时间窗口的数组
    protected final AtomicReferenceArray<WindowWrap<T>> array;
 
    public LeapArray(int sampleCount, int intervalInMs) {
        AssertUtil.isTrue(sampleCount > 0, "bucket count is invalid: " + sampleCount);
        AssertUtil.isTrue(intervalInMs > 0, "total time interval of the sliding window should be positive");
        AssertUtil.isTrue(intervalInMs % sampleCount == 0, "time span needs to be evenly divided");

        this.windowLengthInMs = intervalInMs / sampleCount;
        this.intervalInMs = intervalInMs;
        this.sampleCount = sampleCount;
  
        this.array = new AtomicReferenceArray<>(sampleCount);
    } 

}
```



##### calculateTimeIdx

利用一个数组实现时间轴，每个元素代表一个时间窗口

Sentinel 中 **数组长度是固定的**，通过方法 calculateTimeIdx 来 **确定时间戳在数组** 中的位置 （找到时间戳对应的窗口位置）

```java
private int calculateTimeIdx(long timeMillis) {
    long timeId = timeMillis / (long)this.windowLengthInMs;
    return (int)(timeId % (long)this.array.length());
}
```



怎么理解这个方法呢？

把数据带入进去，假设 windowLengthInMs = 500 ms （每个时间窗口大小是 500 ms）

如果 timestamp 从 0 开始的话，每个时间窗口为 [0,500) [500,1000) [1000,1500) ...

这时候先不考虑 timeId % array.length() ，也不考虑数组长度。假设当前 timeMillis = 601，将数值代入到 timeMillis / windowLengthInMs 其实就可以确定出当前的 timestamp 对应的时间窗口在数组中的位置了

由于数组长度是固定的，所以再加上求余数取模来确定时间窗在数组中的位置



##### currentWindow

先看下Window 的结构，计数器使用了泛型，可以更灵活

```java
public class WindowWrap<T> {

    /**
     * Time length of a single window bucket in milliseconds.
     */
    private final long windowLengthInMs;

    /**
     * Start timestamp of the window in milliseconds.
     */
    private long windowStart;

    /**
     * Statistic data.
     */
    private T value;

 // 省略。。。
}
```



 currentWindow方法根据传入的 timestamp **找到** 或者 **创建** 这个时间戳对应的 Window

```java
public WindowWrap<T> currentWindow(long timeMillis) {
    //当前时间如果小于0，返回空
    if (timeMillis < 0) {
        return null;
    }
    //计算时间窗口的索引
    int idx = calculateTimeIdx(timeMillis);
    // 计算当前时间窗口的开始时间
    long windowStart = calculateWindowStart(timeMillis);
     while (true) {
        //在窗口数组中获得窗口
        WindowWrap<T> old = array.get(idx);
        if (old == null) {
            /*
             *     B0       B1      B2    NULL      B4
             * ||_______|_______|_______|_______|_______||___
             * 200     400     600     800     1000    1200  timestamp
             *                             ^
             *                          time=888
             * 比如当前时间是888，根据计算得到的数组窗口位置是个空，所以直接创建一个新窗口就好了
             */
            WindowWrap<T> window = new WindowWrap<T>(
                windowLengthInMs, windowStart, newEmptyBucket(timeMillis));
            if (array.compareAndSet(idx, null, window)) {
                // Successfully updated, return the created bucket.
                return window;
            } else {
                // Contention failed, the thread will yield its time slice to
                // wait for bucket available.
                Thread.yield();
            }
        } else if (windowStart == old.windowStart()) {
            /*
             *     B0       B1      B2     B3      B4
             * ||_______|_______|_______|_______|_______||___
             * 200     400     600     800     1000    1200  timestamp
             *                             ^
             *                          time=888
             * 这个更好了，刚好等于，直接返回就行
             */
            return old;
        } else if (windowStart > old.windowStart()) {
            /*
             *     B0       B1      B2     B3      B4
             * |_______|_______|_______|_______|_______||___
             * 200     400     600     800     1000    1200  timestamp
             *             B0       B1      B2    NULL      B4
             * |_______||_______|_______|_______|_______|_______||___
             * ...    1200     1400    1600    1800    2000    2200  timestamp
             *                              ^
             *                           time=1676
             * 这个要当成圆形理解就好了，之前如果是1200一个完整的圆形，然后继续从1200开始，如果现在时间是1676，落在在B2的位置，
             * 窗口开始时间是1600，获取到的old时间其实会是600，所以肯定是过期了，直接重置窗口就可以了
             */
            if (updateLock.tryLock()) {
                try {
                    // Successfully get the update lock, now we reset the
                    // bucket.
                    return resetWindowTo(old, windowStart);
                } finally {
                    updateLock.unlock();
                }
            } else {
                Thread.yield();
            }
        } else if (windowStart < old.windowStart()) {
            // 这个不太可能出现，嗯。。时钟回拨
            return new WindowWrap<T>(
                windowLengthInMs, windowStart, newEmptyBucket(timeMillis));
        }
    }
}
```



方法逻辑分析如下：

1. 首先要做的两件事

   - 计算 timestamp 在数组中的位置，就是上文说的 calculateTimeIdx

   - 计算 timestamp 的 windowStart （窗口开始时间），通过 timeMillis - timeMillis % windowLengthInMs

2. 然后进入一个 while(true) 循环， 通过 WindowWrap&lt;T> old = array.get(idx) 找出对应的窗口，接下来就是三种情况了

   - old == null：这个时候代表数组中还没有这个 window，创建这个 window 加入到数组中（由于此时可能会有多个线程同时添加数组元素，所以一定要保证线程安全，所以这里使用的数组为 AtomicReferenceArray），添加成功后返回新建的 window

   - windowStart == old.windowStart()：window 已经存在了，直接返回即可

   - windowStart > old.windowStart()：代表数组中的元素已经至少是 25s 之前的了，重置当前窗口的 windowStart 和 计数器，这个操作同样也是一个多线程操作，所以使用了 updateLock.tryLock()。

   - windowStart < old.windowStart()：通常情况下不会走到这个逻辑分支

##### values

上文中提到计算流量时具体使用几个窗口，取决于窗口大小和单位时间大小

该方法的作用通过传入一个时间戳，找出本次计算所需的所有时间窗口

```java
public List<T> values(long timeMillis) {
    if (timeMillis < 0L) {
        return new ArrayList();
    } else {
        int size = this.array.length();
        List<T> result = new ArrayList(size);

        for(int i = 0; i < size; ++i) { //这里逻辑就是遍历数组将时间符合的窗口加入到 List 中
            WindowWrap<T> windowWrap = (WindowWrap)this.array.get(i);
            if (windowWrap != null && !this.isWindowDeprecated(timeMillis, windowWrap)) {
                result.add(windowWrap.value());
            }
        }
       
        return result;
    }
}

public boolean isWindowDeprecated(long time, WindowWrap<T> windowWrap) {
  // intervalInMs 在单机限流计算QPS时默认为 1000(ms)
        return time - windowWrap.windowStart() > intervalInMs;
 }
```

重点看一下 isWindowDeprecated 这个方法

还是像上面那样把数值带进去。每个窗口大小为 500 ms，例如 timestamp 为 1601，这个 timestamp 对应的 windowStart 为 1500，此时 (1601 - 1500 > 1000) = false 即这个窗口是有效的，再往前推算，上一个窗口 windowStart 为 1000 也是有效的。再往前推算，或者向后推算都是无效的窗口。



### Sentinel 限流思路

在理解了 LeapArray#currentWindow 和 LeapArray#values 方法的细节之后，其实我们就可以琢磨出限流的实现思路了

首先根据当前时间戳，找到对应的几个 window，根据 所有 window 中的流量总和 + 当前申请的流量数 决定能否通过

- 如果不能通过，抛出异常

- 如果能通过，则对应的窗口加上本次通过的流量数

### 漏斗

Sentinel 主要根据 FlowSlot 中的流控进行流量控制，其中 RateLimiterController 就是漏斗算法的实现

整体逻辑如下：

1. 首先计算出当前请求平摊到 1 秒内的时间花费，然后去计算这一次请求预计时间；
2. 如果小于当前时间的话，那么以当前时间为主，返回即可；
3. 反之如果超过当前时间的话，这时候就要进行排队等待了。等待的时候要判断是否超过当前最大的等待时间，超过就直接丢弃；
4. 没有超过就更新上一次的通过时间，然后再比较一次是否超时。如果还超时就重置时间，反之在等待时间范围之内的话就等待。如果都不是，那就可以通过了。

```java
public class RateLimiterController implements TrafficShapingController {
    //最大等待超时时间，默认500ms
    private final int maxQueueingTimeMs;
    //限流数量
    private final double count;
    //上一次的通过时间
    private final AtomicLong latestPassedTime = new AtomicLong(-1);
     @Override public boolean canPass(
        Node node, int acquireCount, boolean prioritized) {
        // Pass when acquire count is less or equal than 0.
        if (acquireCount <= 0) {
            return true;
        }
        // Reject when count is less or equal than 0.
        // Otherwise,the costTime will be max of long and waitTime will overflow
        // in some cases.
        if (count <= 0) {
            return false;
        }
         long currentTime = TimeUtil.currentTimeMillis();
        //时间平摊到1s内的花费
        long costTime = Math.round(
            1.0 * (acquireCount) / count * 1000); // 1 / 100 * 1000 = 10ms
        
            //计算这一次请求预计的时间
            long expectedTime = costTime + latestPassedTime.get();
        
            //花费时间小于当前时间，pass，最后通过时间 = 当前时间
            if (expectedTime <= currentTime) {
            latestPassedTime.set(currentTime);
            return true;
        }
        else {
            //预计通过的时间超过当前时间，要进行排队等待，重新获取一下，避免出现问题，差额就是需要等待的时间
            long waitTime = costTime + latestPassedTime.get()
                - TimeUtil.currentTimeMillis();
            //等待时间超过最大等待时间，丢弃
            if (waitTime > maxQueueingTimeMs) {
                return false;
            } else {
                //反之，可以更新最后一次通过时间了
                long oldTime = latestPassedTime.addAndGet(costTime);
                try {
                    waitTime = oldTime - TimeUtil.currentTimeMillis();
                    //更新后再判断，还是超过最大超时时间，那么就丢弃，时间重置
                    if (waitTime > maxQueueingTimeMs) {
                        latestPassedTime.addAndGet(-costTime);
                        return false;
                    }
                    //在时间范围之内的话，就等待
                    if (waitTime > 0) {
                        Thread.sleep(waitTime);
                    }
                    return true;
                } catch (InterruptedException e) {
                }
            }
        }
        return false;
    }
}
```



### 令牌桶

Sentinel 的令牌桶实现基于 Guava，代码在 WarmUpController 中。

拿到当前窗口和上一个窗口的 QPS；填充令牌，也就是往桶里丢令牌。

```java
public class WarmUpController implements TrafficShapingController {
    //限流QPS
    protected double count;
    //冷启动系数，默认=3
    private int coldFactor;
    //警戒的令牌数
    protected int warningToken = 0;
    //最大令牌数
    private int maxToken;
    //斜率，产生令牌的速度
    protected double slope;
    //存储的令牌数量
    protected AtomicLong storedTokens = new AtomicLong(0);
    //最后一次填充令牌时间
    protected AtomicLong lastFilledTime = new AtomicLong(0);
    
    public WarmUpController(
        double count, int warmUpPeriodInSec, int coldFactor) {
        construct(count, warmUpPeriodInSec, coldFactor);
    }
     public WarmUpController(double count, int warmUpPeriodInSec) {
        construct(count, warmUpPeriodInSec, 3);
    }
     private void construct(
        double count, int warmUpPeriodInSec, int coldFactor) {
        if (coldFactor <= 1) {
            throw new IllegalArgumentException(
                "Cold factor should be larger than 1");
        }
        this.count = count;
        this.coldFactor = coldFactor;
        
            // stableInterval 稳定产生令牌的时间周期，1/QPS
            // warmUpPeriodInSec 预热/冷启动时间 ,默认 10s
            warningToken = (int) (warmUpPeriodInSec * count) / (coldFactor - 1);
        maxToken = warningToken
            + (int) (2 * warmUpPeriodInSec * count / (1.0 + coldFactor));
        //斜率的计算参考Guava，当做一个固定改的公式
        slope = (coldFactor - 1.0) / count / (maxToken - warningToken);
    }
    
    @Override public boolean canPass(
        Node node, int acquireCount, boolean prioritized) {
        //当前时间窗口通过的QPS
        long passQps = (long) node.passQps();
        //上一个时间窗口QPS
        long previousQps = (long) node.previousPassQps();
        //填充令牌
        syncToken(previousQps);
        
            // 开始计算它的斜率
            // 如果进入了警戒线，开始调整他的qps
            long restToken = storedTokens.get();
        if (restToken >= warningToken) {
            //当前的令牌超过警戒线，获得超过警戒线的令牌数
            long aboveToken = restToken - warningToken;
            // 消耗的速度要比warning快，但是要比慢
            // current interval = restToken*slope+1/count
            double warningQps =
                Math.nextUp(1.0 / (aboveToken * slope + 1.0 / count));
            if (passQps + acquireCount <= warningQps) {
                return true;
            }
        } else {
            if (passQps + acquireCount <= count) {
                return true;
            }
        }
        return false;
    }
}
```

填充令牌的逻辑如下：

1. 拿到当前的时间，然后去掉毫秒数得到的就是秒级时间；
2. 判断时间小于这里就是为了控制每秒丢一次令牌；
3. 然后就是 coolDownTokens 去计算我们的冷启动 / 预热是怎么计算填充令牌的；
4. 后面计算当前剩下的令牌数，这个就不说了。减去上一次消耗的就是桶里剩下的令牌。

 ```java
 protected void syncToken(long passQps) {
     long currentTime = TimeUtil.currentTimeMillis();
     //去掉当前时间的毫秒
     currentTime = currentTime - currentTime % 1000;
     long oldLastFillTime = lastFilledTime.get();
     //控制每秒填充一次令牌
     if (currentTime <= oldLastFillTime) {
         return;
     }
     //当前的令牌数量
     long oldValue = storedTokens.get();
     //获取新的令牌数量，包含添加令牌的逻辑，这就是预热的逻辑
     long newValue = coolDownTokens(currentTime, passQps);
     if (storedTokens.compareAndSet(oldValue, newValue)) {
         //存储的令牌数量当然要减去上一次消耗的令牌
         long currentValue = storedTokens.addAndGet(0 - passQps);
         if (currentValue < 0) {
             storedTokens.set(0L);
         }
         lastFilledTime.set(currentTime);
     }
 }
 ```



1. 最开始的时候因为 lastFilledTime 和 oldValue 都是 0，所以根据当前时间戳会得到一个非常大的数字。最后，和 maxToken 取小的话就得到了最大的令牌数。所以第一次初始化的时候就会生成 maxToken 的令牌；
2. 之后我们假设系统的 QPS 一开始很低，然后突然飙高。所以，开始的时候回一直走到高于警戒线的逻辑里去，然后 passQps 又很低。所以，会一直处于把令牌桶填满的状态（currentTime - lastFilledTime.get() 会一直都是 1000，也就是 1 秒），所以每次都会填充最大 QPScount 数量的令牌；
3. 然后突增流量来了，QPS 瞬间很高。慢慢地令牌数量就会消耗到警戒线之下，走到我们 if 的逻辑里去，然后去按照 count 数量增加令牌。

```java
private long coolDownTokens(long currentTime, long passQps) {
    long oldValue = storedTokens.get();
    long newValue = oldValue;
    
    //水位低于警戒线，就生成令牌
    if (oldValue < warningToken) {
        //如果桶中令牌低于警戒线，根据上一次的时间差，得到新的令牌数，因为去掉了毫秒，1秒生成的令牌就是阈值count
        //第一次都是0的话，会生成count数量的令牌
        newValue = (long) (oldValue
            + (currentTime - lastFilledTime.get()) * count / 1000);
    }
    else if (oldValue > warningToken) {
        //反之，如果是高于警戒线，要判断QPS。因为QPS越高，生成令牌就要越慢，QPS低的话生成令牌要越快
        if (passQps < (int) count / coldFactor) {
            newValue = (long) (oldValue
                + (currentTime - lastFilledTime.get()) * count / 1000);
        }
    }
    //不要超过最大令牌数
    return Math.min(newValue, maxToken);
}
```



上面的逻辑理顺之后，我们就可以继续看限流的部分逻辑：

1. 令牌计算的逻辑完成，然后判断是不是超过警戒线。按照上面的说法，低 QPS 的状态肯定是一直超过的，所以会根据斜率来计算出一个 warningQps。因为我们处于冷启动的状态，所以这个阶段就是要根据斜率来计算出一个 QPS 数量，让流量慢慢地达到系统能承受的峰值。举个例子，如果 count 是 100，那么在 QPS 很低的情况下，令牌桶一直处于满状态。但是系统会控制 QPS，实际通过的 QPS 就是 warningQps，根据算法可能只有 10 或者 20（怎么算的不影响理解）。QPS 主键提高的时候，aboveToken 再逐渐变小，整个 warningQps 就在逐渐变大。直到走到警戒线之下，到了 else 逻辑里；
2. 流量突增的情况，就是 else 逻辑里低于警戒线的情况，我们令牌桶在不停地根据 count 去增加令牌。此时消耗令牌的速度超过我们生成令牌的速度，可能就会导致一直处于警戒线之下。这时候判断当然就需要根据最高 QPS 去判断限流了。

```java
long restToken = storedTokens.get();
if (restToken >= warningToken) {
    //当前的令牌超过警戒线，获得超过警戒线的令牌数
    long aboveToken = restToken - warningToken;
    // 消耗的速度要比warning快，但是要比慢
    // current interval = restToken*slope+1/count
    double warningQps = Math.nextUp(1.0 / (aboveToken * slope + 1.0 / count));
    if (passQps + acquireCount <= warningQps) {
        return true;
    }
} else {
    if (passQps + acquireCount <= count) {
        return true;
    }
}
```



所以，按照低 QPS 到突增高 QPS 的流程，来想象一下这个过程：

1. 刚开始，系统的 QPS 非常低，初始化我们就直接把令牌桶塞满了；
2. 然后这个低 QPS 的状态持续了一段时间，因为我们一直会填充最大 QPS 数量的令牌（因为取最小值，所以其实桶里令牌基本不会有变化），所以令牌桶一直处于满的状态，整个系统的限流也处于一个比较低的水平。这以上的部分一直处于警戒线之上。实际上就是叫做冷启动 / 预热的过程；
3. 接着系统的 QPS 突然激增，令牌消耗速度太快。就算我们每次增加最大 QPS 数量的令牌任然无法维持消耗，所以桶里的令牌在不断低减少。这个时候，冷启动阶段的限制 QPS 也在不断地提高，最后直到桶里的令牌低于警戒线；
4. 低于警戒线之后，系统就会按照最高 QPS 去限流，这个过程就是系统在逐渐达到最高限流的过程。那这样一来，实际就达到了我们处理突增流量的目的，整个系统在漫漫地适应突然飙高的 QPS，然后最终达到系统的 QPS 阈值；
5. 最后，如果 QPS 回复正常，那么又会逐渐回到警戒线之上，就回到了最开始的过程。

## 线程隔离

 待更新……

 

## 快速失败

 待更新……



## 服务熔断

 待更新……