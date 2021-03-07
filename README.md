# 简介
一个简易的本地内存缓存组件，支持缓存查询，定时更新，过期删除，LRU缓存驱逐，并且支持并发读写

设计背景为读高频，写低频的场景

# 功能

## 创建
通过Builder创建一个Cache实例

```java
// 无容量限制的缓存
Cache cache = CacheBuilder.newBuilder().build();
// 容量限制为100的缓存
Cache cache = CacheBuilder.newBuilder().setCapacity(100).build();
```

## 添加
通过add方法添加缓存

```java
Cache cache = CacheBuilder.newBuilder().build();
// 添加一个10分钟过期的缓存
cache.add("key", "value", 10, TimeUnit.MINUTES);
// 添加一个10分钟，或1分钟未被访问过期的缓存
cache.add("key", "value", 10, 1, TimeUnit.MINUTES);
```

## 获取
获取缓存

```java
Cache cache = CacheBuilder.newBuilder().build();
// 添加一个10分钟过期的缓存
cache.add("key", "value", 10, TimeUnit.MINUTES);
String value  = (String) cache.get("key");
```

## 获取或加载
获取一个缓存，如果不存在则根据传入的CacheLoader加载缓存。多线程访问，缓存只会加载一次。

后台线程根据过期时间，周期性异步刷新缓存

在刷新的过程中，如果抛出任何异常，都会使旧值被保留，并且异常将会被打印日志 

```java
Cache cache = CacheBuilder.newBuilder().build();
cache.getOrLoad("key", (k,v) -> "value", 10, TimeUnit.MINUTES);
```

## 刷新与移除
刷新与移除缓存

注意，刷新缓存的key必须通过getOrLoad加载，否则该缓存没有CacheLoader无法执行刷新操作


```java
Cache cache = CacheBuilder.newBuilder().build();
cache.getOrLoad("key", (k,v) -> "value", 10, TimeUnit.MINUTES);
cache.refresh("key");
        
cache.remove("key");
```

## 异步方法
设计上，所有的写操作均又后台线程完成，用户线程仅将写事件提交给后台线程，并等待后台线程完成（具体见下面设计-并发一节）

因此，添加，刷新，移除均提供相应的异步方法，用户线程调用后会立即返回，而不会等待后台线程完成操作（尽管一般操作非常的快）

```java
Cache cache = CacheBuilder.newBuilder().build();
cache.getOrLoad("key", (k,v) -> "value", 10, TimeUnit.MINUTES);
cache.addAsync("key", "value2", 10, TimeUnit.MINUTES);
cache.refreshAsync("key");
cache.removeAsync("key");
```

## 监听器
显示的注册一个监听器，当缓存事件触发时，执行指定的操作

需要注意的是监听器中各方法执行的线程，onRead将会在用户线程中执行，因此可能会有多线程并发执行，实现时需要考虑线程安全

其余方法均在后台线程中执行，因此只会单线程执行

不要在方法中执行重量级的操作（比如读写DB），否则严重影响后台线程的处理效率，影响性能

可以在方法中向其他的线程池提交异步任务执行重量级的操作，使得后台线程能够快速的返回处理其他重要的操作。

```java
Listener listener = new Listener() {
        @Override
        public void onRead(String key) {
            // 当缓存被读取后
        }
        @Override
        public void onWrite(String key) {
            // 当缓存被写入后
        }
        @Override
        public void onRemove(String key, Object value, RemovalCause cause) {
            // 当缓存被移除后
        }
        @Override
        public void onRefresh(String key) {
            // 当缓存被刷新后
        }
    };
Cache cache = CacheBuilder.newBuilder().registerListener(listener).build();
```

## 自定义过期时间计算

可以通过传入Expiration的实现，自定义过期时间的获取。支持缓存过期时间的动态的修改

expireAfterRefresh 将在缓存初始化时，以及缓存刷新后，重新调度过程中计算过期时间时被调用。如果过期时间发生变化，则按照新的过期时间进行调度

expireAfterAccess 将在缓存初始化时，以及缓存过期时被调用。当缓存过期时，重新调用本方法查看过期时间是否变化，如果过期时间变短，则缓存依然过期移除，如果时间变长，根据 新过期时间 减 原过期时间 得到 剩余的过期时间， 重新调度。 

与监听器同理，不要在方法中执行重量级操作（比如从DB中读取过期时间配置）

```java
Expiration expiration = new Expiration() {
        @Override
        public long expireAfterRefresh(TimeUnit timeUnit) {
            return timeUnit.convert(10, TimeUnit.MINUTES);
        }
        @Override
        public long expireAfterAccess(TimeUnit timeUnit) {
            return timeUnit.convert(10, TimeUnit.MINUTES);
        }
    };
cache.getOrLoad("key", (k,v) -> "value", expiration);
```
# 设计

## 数据结构示意
![](https://nothinghappen.oss-cn-shanghai.aliyuncs.com/tinyCache/datastruct.JPG)

## 缓存数据
缓存数据通过一个 ConcurrentHashMap 进行保存，并由其支持并发的读写

## 驱逐
LRU驱逐策略，一个元素可以在O(1)的时间复杂度内从HashMap中找到并进行操作。

维护一个缓存项的双向环形链表，每当一个缓存项被访问，将其从链表中取下并重新链接至表头。最近最少使用的缓存项会在队尾，驱逐时将从队尾开始驱逐。

## 过期

过期策略支持两种
* expireAfterRefresh 缓存写入指定时间后过期
* expireAfterAccess 缓存指定时间内未被访问后过期

expireAfterRefresh 的实现，根据过期时间进行排序，通过一个最小堆组织缓存项，堆顶缓存为最近过期的缓存。后台线程每次尝试获取并等待堆顶元素过期，如果缓存指定了数据加载方法，则进行刷新并更新缓存的过期时间，重新加入最小堆重新调度，否则移除缓存。

expireAfterAccess 的实现，与 expireAfterRefresh 类似，需要一个数据结构组织缓存项，按照指定过期时间进行排序，使得能快速的获取最近过期的元素。最小堆是一个选择，它的插入，删除的时间复杂度均为O(logn)。但是，与 expireAfterRefresh 不同的是，每次缓存被访问，缓存项都要在 expireAfterAccess 的数据结构中重新调度（删除，更新过期时间并重新插入）。而本缓存组件的设计背景为读高频，写低频。因此尝试探索一种性能更高的数据结构，最终选择**分层时间轮**作为 expireAfterAccess 的数据结构实现。

时间轮在KafKa和Caffeine中均得到应用，是一个高效的延迟队列实现，插入，删除均摊下来时间复杂度为O(1)

## 并发
组件内部维护了4种不同的数据结构，包括之前提到的ConcurrentHashMap,双向环形链表,最小堆以及分层时间轮。

低效的实现为在每个读写操作时以整个缓存管理实例为粒度进行加锁，使得能够线程安全的修改上述4种数据结构。

由于设计背景为读高频，写低频。因此设计并发方案时，采用了启动一个后台守护线程的方式，所有写相关的操作均由该守护线程完成，其他用户线程通过一个写队列提交写事件，由守护线程读出并执行

用户线程读取缓存时，调用 ConcurrentHashMap 的 get 方法获取缓存，并且向一个读队列中添加该缓存的读事件，由守护线程读出并在LRU双向环形链表，expireAfterAccess 的分层时间轮中重新调度该缓存。

![](https://nothinghappen.oss-cn-shanghai.aliyuncs.com/tinyCache/concurrent.JPG)

### 写队列
写事件是非常重要的，必须保证所有的读事件被读出执行，因此一个无锁无界队列符合我们的要求。实现上直接使用 ConcurrentLinkedQueue

### 读队列
读事件主要用于维护LRU双向环形链表，expireAfterAccess 的分层时间轮，只影响缓存驱逐和过期，适当的丢弃并不影响全局，并且预期读操作为高频操作，考虑单后台守护线程的处理能力，避免读事件的堆积，读队列采用一个无锁有界队列符合我们的要求。因此自实现了一个的多生产者-单消费者的无锁有界队列

更近一步的，实现上为每一个缓存，设置了一个读事件入队最小时间间隔，在这个时间间隔范围内，一个缓存的读事件最多入队一次。通过该近似算法，以牺牲LRU, expireAfterAccess 过期策略的精度，避免了过多读事件的产生。

![](https://nothinghappen.oss-cn-shanghai.aliyuncs.com/tinyCache/lru_access.JPG)