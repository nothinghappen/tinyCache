package com.nothinghappen.cache;

import com.nothinghappen.cache.base.ConcurrentTest;
import com.nothinghappen.cache.base.TestTicker;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.ws.Holder;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheTest {

    private static Cache cache;

    private static String VALUE = "value";
    private static ExecutorService refreshBackend = Executors.newFixedThreadPool(3);

    @Test
    public void add_expire() throws InterruptedException {

        cache = CacheBuilder.newBuilder().setRefreshBackend(refreshBackend).build();

        cache.add("key", VALUE, 100, TimeUnit.MILLISECONDS);
        Assert.assertEquals(VALUE, cache.get("key"));
        Thread.sleep(150);
        Assert.assertNull(cache.get("key"));


        ConcurrentTest.run(10, () -> {
            cache.add("key1", VALUE, 100, TimeUnit.MILLISECONDS);
            cache.add("key2", VALUE, 200, TimeUnit.MILLISECONDS);
            cache.add("key3", VALUE, 300, TimeUnit.MILLISECONDS);
            cache.add("key4", VALUE, 400, TimeUnit.MILLISECONDS);
            cache.add("key5", VALUE, 500, TimeUnit.MILLISECONDS);
            cache.add("key6", VALUE, 600, TimeUnit.MILLISECONDS);
        });
        Assert.assertEquals(6, cache.size());
        Assert.assertEquals(VALUE, cache.get("key1"));
        Assert.assertEquals(VALUE, cache.get("key6"));
        Thread.sleep(150);
        Assert.assertEquals(5, cache.size());
        Assert.assertNull(cache.get("key1"));
        Thread.sleep(200);
        Assert.assertEquals(3, cache.size());
        Assert.assertNull(cache.get("key2"));
        Assert.assertNull(cache.get("key3"));
        Thread.sleep(200);
        Assert.assertEquals(1, cache.size());
        Assert.assertNull(cache.get("key4"));
        Assert.assertNull(cache.get("key5"));
        Thread.sleep(100);
        Assert.assertEquals(0, cache.size());
        Assert.assertNull(cache.get("key6"));
    }

    @Test
    public void refresh() throws InterruptedException {
        cache = CacheBuilder.newBuilder().setRefreshBackend(refreshBackend).build();
        Holder<String> valueSupplier = new Holder<>("world");
        cache.getOrLoad("hello", (k,v) -> valueSupplier.value, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        Assert.assertEquals("world", cache.get("hello"));
        valueSupplier.value = "world2";
        cache.refresh("hello");
        Assert.assertEquals("world2", cache.get("hello"));
    }

    @Test
    public void add_refresh() {
        cache = CacheBuilder.newBuilder().setRefreshBackend(refreshBackend).build();

        cache.add("hello", VALUE, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        cache.refresh("hello");
        Assert.assertNull(cache.get("hello"));
    }

    @Test
    public void remove() {

        cache = CacheBuilder.newBuilder().setRefreshBackend(refreshBackend).build();

        cache.add("key", VALUE, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        Assert.assertEquals(VALUE, cache.get("key"));
        cache.remove("key");
        Assert.assertNull(cache.get("key"));
    }

    @Test
    public void get_or_load() throws InterruptedException {

        cache = cache = CacheBuilder.newBuilder().setRefreshBackend(refreshBackend).build();

        long currentThreadID = Thread.currentThread().getId();
        Holder<Long> loadThreadID = new Holder<>();
        cache.getOrLoad("key", (k,v) -> {
            loadThreadID.value = Thread.currentThread().getId();
            return VALUE;
        }, 100, TimeUnit.MILLISECONDS);
        // first loading in customer thread
        Assert.assertEquals(currentThreadID, loadThreadID.value.longValue());
        Assert.assertEquals(VALUE, cache.get("key"));
        Thread.sleep(150);
        // refreshing in refreshBackend
        Assert.assertNotEquals(currentThreadID, loadThreadID.value.longValue());
        cache.remove("key");

        AtomicInteger loadCount = new AtomicInteger();
        ConcurrentTest.run(100, () -> {
            cache.getOrLoad("key", (k,v) -> {
                loadCount.incrementAndGet();
                return VALUE;
            }, 100, TimeUnit.MILLISECONDS);
        });
        // load only once
        Assert.assertEquals(1, loadCount.intValue());
        Thread.sleep(150);
        Assert.assertEquals(2, loadCount.intValue());
        cache.remove("key");

    }

    @Test
    public void expireAfterAccess() throws InterruptedException {

        TestTicker testTicker = new TestTicker();
        cache = CacheBuilder.newBuilder()
                .setRefreshBackend(refreshBackend)
                .advancedOption(new AdvancedOption().setTimeWheelTicker(testTicker))
                .build();
        CacheImpl cacheImpl = (CacheImpl) cache;
        CachingTest.stopBackend(cacheImpl);
        cacheImpl.timeWheelTicker = testTicker;

        cache.addAsync("key", VALUE, 10000, 100, TimeUnit.MILLISECONDS);
        CachingTest.maintenance(cacheImpl);
        testTicker.ticks = 50;
        CachingTest.maintenance(cacheImpl);
        Assert.assertEquals(VALUE, cache.get("key"));
        CachingTest.maintenance(cacheImpl);
        testTicker.ticks = 150;
        CachingTest.maintenance(cacheImpl);
        Assert.assertNull(cache.get("key"));
    }

    @Test
    public void expireAfterAccess2() throws InterruptedException {

        TestTicker testTicker = new TestTicker();
        cache = CacheBuilder.newBuilder()
                .setRefreshBackend(refreshBackend)
                .advancedOption(new AdvancedOption().setTimeWheelTicker(testTicker))
                .build();
        CacheImpl cacheImpl = (CacheImpl) cache;
        CachingTest.stopBackend(cacheImpl);
        cacheImpl.timeWheelTicker = testTicker;

        Holder<Long> expireAfterAccess = new Holder<>(10L);
        cache.addAsync("key", VALUE, new Expiration() {
            @Override
            public long expireAfterRefresh(TimeUnit timeUnit) {
                return timeUnit.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            }
            @Override
            public long expireAfterAccess(TimeUnit timeUnit) {
                return timeUnit.convert(expireAfterAccess.value, TimeUnit.MILLISECONDS);
            }
        });
        CachingTest.maintenance(cacheImpl);
        expireAfterAccess.value = 100L;
        testTicker.ticks = 10;
        CachingTest.maintenance(cacheImpl);
        Assert.assertTrue(CachingTest.peek(cacheImpl, "key"));
        CachingTest.maintenance(cacheImpl);
        testTicker.ticks = 99;
        CachingTest.maintenance(cacheImpl);
        Assert.assertTrue(CachingTest.peek(cacheImpl, "key"));
        testTicker.ticks = 100;
        CachingTest.maintenance(cacheImpl);
        Assert.assertFalse(CachingTest.peek(cacheImpl, "key"));

    }

    @Test
    public void expireAfterAccess3() throws InterruptedException {

        TestTicker testTicker = new TestTicker();
        cache = CacheBuilder.newBuilder()
                .setRefreshBackend(refreshBackend)
                .advancedOption(new AdvancedOption().setTimeWheelTicker(testTicker))
                .build();
        CacheImpl cacheImpl = (CacheImpl) cache;
        CachingTest.stopBackend(cacheImpl);
        cacheImpl.timeWheelTicker = testTicker;

        Holder<Long> expireAfterAccess = new Holder<>(10L);
        cache.addAsync("key", VALUE, new Expiration() {
            @Override
            public long expireAfterRefresh(TimeUnit timeUnit) {
                return timeUnit.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            }
            @Override
            public long expireAfterAccess(TimeUnit timeUnit) {
                return timeUnit.convert(expireAfterAccess.value, TimeUnit.MILLISECONDS);
            }
        });
        CachingTest.maintenance(cacheImpl);
        expireAfterAccess.value = 100L;
        testTicker.ticks = 100;
        CachingTest.maintenance(cacheImpl);
        Assert.assertFalse(CachingTest.peek(cacheImpl, "key"));
    }

    @Test
    public void evict() throws InterruptedException {
        Cache cache = CacheBuilder.newBuilder().setCapacity(3).build();
        cache.add("key1", new Object(),Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        cache.add("key2", new Object(),Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        cache.add("key3", new Object(),Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        cache.add("key4", new Object(),Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        Thread.sleep(200);
        Assert.assertEquals(cache.size(), 3);
        Assert.assertNull(cache.get("key1"));
    }

    @Test
    public void evict2() throws InterruptedException {
        Cache cache = CacheBuilder.newBuilder().setCapacity(3).build();
        cache.add("key1", new Object(),Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        cache.add("key2", new Object(),Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        cache.add("key3", new Object(),Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        cache.get("key1");
        cache.add("key4", new Object(),Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        Thread.sleep(200);
        Assert.assertEquals(cache.size(), 3);
        Assert.assertNull(cache.get("key2"));
    }

}
