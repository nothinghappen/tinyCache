package com.nothinghappen.cache;

import com.nothinghappen.cache.base.Awaits;
import com.nothinghappen.cache.base.ConcurrentTest;
import com.nothinghappen.cache.base.TestTicker;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.ws.Holder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CacheTest {

    private static Cache cache;

    private static String VALUE = "value";
    private static ExecutorService refreshBackend = Executors.newFixedThreadPool(3);

    // @Test
    // public void add() {
    //
    //     cache = CacheBuilder.newBuilder().setRefreshBackend(refreshBackend).build();
    //     cache.add("key", "first", Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    //     Assert.assertEquals("first", cache.get("key"));
    //     cache.add("key", "second", Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    //     Assert.assertEquals("second", cache.get("key"));
    // }
    //
    // @Test
    // public void add_expire() throws InterruptedException {
    //
    //     cache = CacheBuilder.newBuilder().setRefreshBackend(refreshBackend).build();
    //
    //     cache.add("key", VALUE, 100, TimeUnit.MILLISECONDS);
    //     Assert.assertEquals(VALUE, cache.get("key"));
    //     Thread.sleep(150);
    //     Assert.assertNull(cache.get("key"));
    //
    //
    //     ConcurrentTest.run(10, () -> {
    //         cache.add("key1", VALUE, 100, TimeUnit.MILLISECONDS);
    //         cache.add("key2", VALUE, 200, TimeUnit.MILLISECONDS);
    //         cache.add("key3", VALUE, 300, TimeUnit.MILLISECONDS);
    //         cache.add("key4", VALUE, 400, TimeUnit.MILLISECONDS);
    //         cache.add("key5", VALUE, 500, TimeUnit.MILLISECONDS);
    //         cache.add("key6", VALUE, 600, TimeUnit.MILLISECONDS);
    //     });
    //     Assert.assertEquals(6, cache.size());
    //     Assert.assertEquals(VALUE, cache.get("key1"));
    //     Assert.assertEquals(VALUE, cache.get("key6"));
    //     Thread.sleep(150);
    //     Assert.assertEquals(5, cache.size());
    //     Assert.assertNull(cache.get("key1"));
    //     Thread.sleep(200);
    //     Assert.assertEquals(3, cache.size());
    //     Assert.assertNull(cache.get("key2"));
    //     Assert.assertNull(cache.get("key3"));
    //     Thread.sleep(200);
    //     Assert.assertEquals(1, cache.size());
    //     Assert.assertNull(cache.get("key4"));
    //     Assert.assertNull(cache.get("key5"));
    //     Thread.sleep(100);
    //     Assert.assertEquals(0, cache.size());
    //     Assert.assertNull(cache.get("key6"));
    // }
    //
    // @Test
    // public void refresh() {
    //     cache = CacheBuilder.newBuilder().setRefreshBackend(refreshBackend).build();
    //     Holder<String> valueSupplier = new Holder<>("world");
    //     cache.getOrLoad("hello", (k,v) -> valueSupplier.value, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //     Assert.assertEquals("world", cache.get("hello"));
    //     valueSupplier.value = "world2";
    //     cache.refresh("hello");
    //     Assert.assertEquals("world2", cache.get("hello"));
    //     // nonexistent key
    //     cache.refresh("hello2");
    //     Assert.assertNull(cache.get("hello2"));
    // }
    //
    // @Test
    // public void add_refresh() {
    //     cache = CacheBuilder.newBuilder().setRefreshBackend(refreshBackend).build();
    //
    //     cache.add("hello", VALUE, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //     cache.refresh("hello");
    //     Assert.assertNull(cache.get("hello"));
    // }
    //
    // @Test
    // public void remove() {
    //
    //     cache = CacheBuilder.newBuilder().setRefreshBackend(refreshBackend).build();
    //
    //     cache.add("key", VALUE, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //     Assert.assertEquals(VALUE, cache.get("key"));
    //     cache.remove("key");
    //     Assert.assertNull(cache.get("key"));
    //     // nonexistent key
    //     cache.remove("key");
    //     Assert.assertNull(cache.get("key"));
    // }
    //
    // @Test
    // public void get_or_load() throws InterruptedException {
    //
    //     cache = cache = CacheBuilder.newBuilder().setRefreshBackend(refreshBackend).build();
    //
    //     long currentThreadID = Thread.currentThread().getId();
    //     Holder<Long> loadThreadID = new Holder<>();
    //     cache.getOrLoad("key", (k,v) -> {
    //         loadThreadID.value = Thread.currentThread().getId();
    //         return VALUE;
    //     }, 100, TimeUnit.MILLISECONDS);
    //     // first loading in customer thread
    //     Assert.assertEquals(currentThreadID, loadThreadID.value.longValue());
    //     Assert.assertEquals(VALUE, cache.get("key"));
    //     Thread.sleep(150);
    //     // refreshing in refreshBackend
    //     Assert.assertNotEquals(currentThreadID, loadThreadID.value.longValue());
    //     cache.remove("key");
    //
    //     AtomicInteger loadCount = new AtomicInteger();
    //     ConcurrentTest.run(100, () -> {
    //         cache.getOrLoad("key", (k,v) -> {
    //             loadCount.incrementAndGet();
    //             return VALUE;
    //         }, 100, TimeUnit.MILLISECONDS);
    //     });
    //     // doRefresh only once
    //     Assert.assertEquals(1, loadCount.intValue());
    //     Thread.sleep(110);
    //     Assert.assertEquals(2, loadCount.intValue());
    //     cache.remove("key");
    //
    // }
    //
    // @Test
    // public void expireAfterAccess() {
    //
    //     TestTicker testTicker = new TestTicker();
    //     cache = new CacheImpl(0, refreshBackend, new AdvancedOption().setTimeWheelTicker(testTicker));
    //     CacheImpl cacheImpl = (CacheImpl) cache;
    //     cacheImpl.timeWheelTicker = testTicker;
    //
    //     cache.addAsync("key", VALUE, 10000, 100, TimeUnit.MILLISECONDS);
    //     CachingTest.maintenance(cacheImpl);
    //
    //     testTicker.ticks = 50;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertEquals(VALUE, cache.get("key"));
    //
    //     CachingTest.maintenance(cacheImpl);
    //     testTicker.ticks = 149;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertNotNull(cache.get("key"));
    //
    //     CachingTest.maintenance(cacheImpl);
    //     testTicker.ticks = 249;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertNull(cache.get("key"));
    // }
    //
    // @Test
    // public void expireAfterAccess2() {
    //
    //     TestTicker testTicker = new TestTicker();
    //     cache = new CacheImpl(0, refreshBackend, new AdvancedOption().setTimeWheelTicker(testTicker));
    //     CacheImpl cacheImpl = (CacheImpl) cache;
    //     cacheImpl.timeWheelTicker = testTicker;
    //
    //     Holder<Long> expireAfterAccess = new Holder<>(10L);
    //     cache.addAsync("key", VALUE, new Expiration() {
    //         @Override
    //         public long expireAfterRefresh(TimeUnit timeUnit) {
    //             return timeUnit.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //         }
    //         @Override
    //         public long expireAfterAccess(TimeUnit timeUnit) {
    //             return timeUnit.convert(expireAfterAccess.value, TimeUnit.MILLISECONDS);
    //         }
    //     });
    //     CachingTest.maintenance(cacheImpl);
    //     expireAfterAccess.value = 100L;
    //     testTicker.ticks = 10;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertTrue(CachingTest.peek(cacheImpl, "key"));
    //     CachingTest.maintenance(cacheImpl);
    //     testTicker.ticks = 99;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertTrue(CachingTest.peek(cacheImpl, "key"));
    //     testTicker.ticks = 100;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertFalse(CachingTest.peek(cacheImpl, "key"));
    //
    // }
    //
    // @Test
    // public void expireAfterAccess3() {
    //
    //     TestTicker testTicker = new TestTicker();
    //     cache = new CacheImpl(0, refreshBackend, new AdvancedOption().setTimeWheelTicker(testTicker));
    //     CacheImpl cacheImpl = (CacheImpl) cache;
    //     cacheImpl.timeWheelTicker = testTicker;
    //     Holder<Long> expireAfterAccess = new Holder<>(10L);
    //     cache.addAsync("key", VALUE, new Expiration() {
    //         @Override
    //         public long expireAfterRefresh(TimeUnit timeUnit) {
    //             return timeUnit.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //         }
    //         @Override
    //         public long expireAfterAccess(TimeUnit timeUnit) {
    //             return timeUnit.convert(expireAfterAccess.value, TimeUnit.MILLISECONDS);
    //         }
    //     });
    //
    //     CachingTest.maintenance(cacheImpl);
    //     expireAfterAccess.value = 100L;
    //     testTicker.ticks = 10;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertTrue(CachingTest.peek(cacheImpl, "key"));
    //     CachingTest.maintenance(cacheImpl);
    //     testTicker.ticks = 99;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertTrue(CachingTest.peek(cacheImpl, "key"));
    //     testTicker.ticks = 100;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertFalse(CachingTest.peek(cacheImpl, "key"));
    //
    //     cache.addAsync("key", VALUE, new Expiration() {
    //         @Override
    //         public long expireAfterRefresh(TimeUnit timeUnit) {
    //             return timeUnit.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //         }
    //         @Override
    //         public long expireAfterAccess(TimeUnit timeUnit) {
    //             return timeUnit.convert(expireAfterAccess.value, TimeUnit.MILLISECONDS);
    //         }
    //     });
    //     CachingTest.maintenance(cacheImpl);
    //
    //     testTicker.ticks = 199;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertTrue(CachingTest.peek(cacheImpl, "key"));
    //
    //     testTicker.ticks = 200;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertFalse(CachingTest.peek(cacheImpl, "key"));
    //
    //     cache.addAsync("key", VALUE, new Expiration() {
    //         @Override
    //         public long expireAfterRefresh(TimeUnit timeUnit) {
    //             return timeUnit.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //         }
    //         @Override
    //         public long expireAfterAccess(TimeUnit timeUnit) {
    //             return timeUnit.convert(expireAfterAccess.value, TimeUnit.MILLISECONDS);
    //         }
    //     });
    //     CachingTest.maintenance(cacheImpl);
    //
    //     testTicker.ticks = 300;
    //     expireAfterAccess.value = 101L;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertTrue(CachingTest.peek(cacheImpl, "key"));
    //
    //     testTicker.ticks = 301;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertFalse(CachingTest.peek(cacheImpl, "key"));
    //
    //     testTicker.ticks = 400;
    //     CachingTest.maintenance(cacheImpl);
    //
    //     expireAfterAccess.value = 100L;
    //     cache.addAsync("key", VALUE, new Expiration() {
    //         @Override
    //         public long expireAfterRefresh(TimeUnit timeUnit) {
    //             return timeUnit.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //         }
    //         @Override
    //         public long expireAfterAccess(TimeUnit timeUnit) {
    //             return timeUnit.convert(expireAfterAccess.value, TimeUnit.MILLISECONDS);
    //         }
    //     });
    //     CachingTest.maintenance(cacheImpl);
    //
    //     expireAfterAccess.value = 101L;
    //     testTicker.ticks = 501;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertFalse(CachingTest.peek(cacheImpl, "key"));
    //
    //     testTicker.ticks = 600;
    //     CachingTest.maintenance(cacheImpl);
    //
    //     expireAfterAccess.value = 100L;
    //     cache.addAsync("key", VALUE, new Expiration() {
    //         @Override
    //         public long expireAfterRefresh(TimeUnit timeUnit) {
    //             return timeUnit.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //         }
    //         @Override
    //         public long expireAfterAccess(TimeUnit timeUnit) {
    //             return timeUnit.convert(expireAfterAccess.value, TimeUnit.MILLISECONDS);
    //         }
    //     });
    //     CachingTest.maintenance(cacheImpl);
    //
    //     testTicker.ticks = 700;
    //     expireAfterAccess.value = 0L;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertTrue(CachingTest.peek(cacheImpl, "key"));
    //
    // }
    //
    // @Test
    // public void expireAfterAccess4() {
    //
    //     TestTicker testTicker = new TestTicker();
    //     cache = CacheBuilder.newBuilder()
    //             .setRefreshBackend(refreshBackend)
    //             .advancedOption(new AdvancedOption().setTimeWheelTicker(testTicker))
    //             .build();
    //     CacheImpl cacheImpl = (CacheImpl) cache;
    //     cacheImpl.timeWheelTicker = testTicker;
    //
    //     Holder<Long> expireAfterAccess = new Holder<>(Long.MAX_VALUE);
    //     cacheImpl.getOrLoad("key", (k,v) -> VALUE, new Expiration() {
    //         @Override
    //         public long expireAfterRefresh(TimeUnit timeUnit) {
    //             return timeUnit.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //         }
    //         @Override
    //         public long expireAfterAccess(TimeUnit timeUnit) {
    //             return timeUnit.convert(expireAfterAccess.value, TimeUnit.MILLISECONDS);
    //         }
    //     });
    //     cacheImpl.stopBackendThread();
    //
    //     expireAfterAccess.value = 10L;
    //     cacheImpl.refreshAsync("key");
    //     CachingTest.maintenance(cacheImpl);
    //
    //     testTicker.ticks = 9;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertTrue(CachingTest.peek(cacheImpl, "key"));
    //
    //     testTicker.ticks = 10;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertFalse(CachingTest.peek(cacheImpl, "key"));
    // }
    //
    // @Test
    // public void expireAfterAccess5() {
    //
    //     TestTicker testTicker = new TestTicker();
    //     cache = CacheBuilder.newBuilder()
    //             .setRefreshBackend(refreshBackend)
    //             .advancedOption(new AdvancedOption().setTimeWheelTicker(testTicker))
    //             .build();
    //     CacheImpl cacheImpl = (CacheImpl) cache;
    //     cacheImpl.timeWheelTicker = testTicker;
    //
    //     Holder<Long> expireAfterAccess = new Holder<>(0L);
    //     cacheImpl.getOrLoad("key", (k,v) -> VALUE, new Expiration() {
    //         @Override
    //         public long expireAfterRefresh(TimeUnit timeUnit) {
    //             return timeUnit.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //         }
    //         @Override
    //         public long expireAfterAccess(TimeUnit timeUnit) {
    //             return timeUnit.convert(expireAfterAccess.value, TimeUnit.MILLISECONDS);
    //         }
    //     });
    //     cacheImpl.stopBackendThread();
    //
    //     expireAfterAccess.value = 10L;
    //     cacheImpl.refreshAsync("key");
    //     CachingTest.maintenance(cacheImpl);
    //
    //     testTicker.ticks = 9;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertTrue(CachingTest.peek(cacheImpl, "key"));
    //
    //     testTicker.ticks = 10;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertFalse(CachingTest.peek(cacheImpl, "key"));
    // }
    //
    // @Test
    // public void expireAfterAccess6() {
    //
    //     TestTicker testTicker = new TestTicker();
    //     cache = CacheBuilder.newBuilder()
    //             .setRefreshBackend(refreshBackend)
    //             .advancedOption(new AdvancedOption().setTimeWheelTicker(testTicker))
    //             .build();
    //     CacheImpl cacheImpl = (CacheImpl) cache;
    //     cacheImpl.timeWheelTicker = testTicker;
    //
    //     Holder<Long> expireAfterAccess = new Holder<>(10L);
    //     cacheImpl.getOrLoad("key", (k,v) -> VALUE, new Expiration() {
    //         @Override
    //         public long expireAfterRefresh(TimeUnit timeUnit) {
    //             return timeUnit.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //         }
    //         @Override
    //         public long expireAfterAccess(TimeUnit timeUnit) {
    //             return timeUnit.convert(expireAfterAccess.value, TimeUnit.MILLISECONDS);
    //         }
    //     });
    //     cacheImpl.stopBackendThread();
    //
    //     expireAfterAccess.value = 0L;
    //     cacheImpl.refreshAsync("key");
    //     CachingTest.maintenance(cacheImpl);
    //
    //     testTicker.ticks = Long.MAX_VALUE;
    //     CachingTest.maintenance(cacheImpl);
    //     Assert.assertTrue(CachingTest.peek(cacheImpl, "key"));
    // }
    //
    // @Test
    // public void evict() throws InterruptedException {
    //     Cache cache = CacheBuilder.newBuilder().setCapacity(3).build();
    //     cache.add("key1", VALUE,Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //     cache.add("key2", VALUE,Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //     cache.add("key3", VALUE,Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //     cache.add("key4", VALUE,Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //     Thread.sleep(200);
    //     Assert.assertEquals(cache.size(), 3);
    //     Assert.assertNull(cache.get("key1"));
    // }
    //
    // @Test
    // public void evict2() throws InterruptedException {
    //     Cache cache = CacheBuilder.newBuilder().setCapacity(3).build();
    //     cache.add("key1", VALUE,Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //     cache.add("key2", VALUE,Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //     cache.add("key3", VALUE,Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //     cache.get("key1");
    //     cache.add("key4", VALUE,Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //     Thread.sleep(200);
    //     Assert.assertEquals(cache.size(), 3);
    //     Assert.assertNull(cache.get("key2"));
    // }

    /*---------------------------------get_or_load------------------------------------------------------------*/

    @Test
    public void getOrLoad() throws ExecutionException, InterruptedException {
        Cache cache = CacheBuilder.newBuilder().build();
        AtomicInteger loadCount = new AtomicInteger();
        Future<String>[] futures = ConcurrentTest.supplyAsync(100, () -> (String) cache.getOrLoad("key", (k, v) -> {
            loadCount.incrementAndGet();
            return VALUE;
        }, Long.MAX_VALUE, TimeUnit.MILLISECONDS));
        for (Future<String> f : futures) {
            Assert.assertEquals(VALUE, f.get());
        }
        Assert.assertEquals(1, cache.size());
        Assert.assertEquals(1, loadCount.intValue());
    }

    @Test
    public void getOrLoad_expire_less_than_zero() throws ExecutionException, InterruptedException {
        Cache cache = CacheBuilder.newBuilder().build();
        AtomicInteger loadCount = new AtomicInteger();
        Future<String>[] futures = ConcurrentTest.supplyAsync(100, () -> (String) cache.getOrLoad("key", (k, v) -> {
            loadCount.incrementAndGet();
            return VALUE;
        }, -Long.MAX_VALUE, TimeUnit.MILLISECONDS));
        for (Future<String> f : futures) {
            Assert.assertEquals(VALUE, f.get());
        }
        Assert.assertEquals(1, cache.size());
        Assert.assertEquals(1, loadCount.intValue());
    }

    @Test
    public void getOrLoad_load_null() throws ExecutionException, InterruptedException {
        Cache cache = CacheBuilder.newBuilder().build();
        AtomicInteger loadCount = new AtomicInteger();
        Holder<String> supplier = new Holder<>();
        CacheLoader loader = (k, v) -> {
            loadCount.incrementAndGet();
            return supplier.value;
        };
        Future<String>[] futures = ConcurrentTest.supplyAsync(100,
                () -> (String) cache.getOrLoad("key", loader, Long.MAX_VALUE, TimeUnit.MILLISECONDS));
        for (Future<String> f : futures) {
            Assert.assertNull(null, f.get());
        }
        Assert.assertEquals(100, loadCount.intValue());
        Assert.assertEquals(1, cache.size());
        Assert.assertTrue(CachingTest.peek((CacheImpl) cache, "key"));
        Assert.assertNull(cache.get("key"));

        supplier.value = VALUE;
        loadCount.set(0);
        futures = ConcurrentTest.supplyAsync(100,
                () -> (String) cache.getOrLoad("key", loader, Long.MAX_VALUE, TimeUnit.MILLISECONDS));
        for (Future<String> f : futures) {
            Assert.assertEquals(VALUE, f.get());
        }
        Assert.assertEquals(1, cache.size());
        Assert.assertEquals(1, loadCount.intValue());
    }

    @Test
    public void getOrLoad_load_throw_exception() throws ExecutionException, InterruptedException {
        Cache cache = CacheBuilder.newBuilder().build();
        AtomicInteger loadCount = new AtomicInteger();
        Holder<String> supplier = new Holder<>();
        CacheLoader loader = (k, v) -> {
            loadCount.incrementAndGet();
            if (supplier.value == null) {
                throw new IllegalStateException();
            }
            return supplier.value;
        };
        Future<String>[] futures = ConcurrentTest.supplyAsync(100,
                () -> (String) cache.getOrLoad("key", loader, Long.MAX_VALUE, TimeUnit.MILLISECONDS));
        for (Future<String> f : futures) {
            Assert.assertNull(null, f.get());
        }
        Assert.assertEquals(100, loadCount.intValue());
        Assert.assertEquals(1, cache.size());
        Assert.assertTrue(CachingTest.peek((CacheImpl) cache, "key"));
        Assert.assertNull(cache.get("key"));

        supplier.value = VALUE;
        loadCount.set(0);
        futures = ConcurrentTest.supplyAsync(100,
                () -> (String) cache.getOrLoad("key", loader, Long.MAX_VALUE, TimeUnit.MILLISECONDS));
        for (Future<String> f : futures) {
            Assert.assertEquals(VALUE, f.get());
        }
        Assert.assertEquals(1, loadCount.intValue());
    }

    @Test
    public void getOrLoad_refresh() {
        CacheImpl cache = CacheBuilder.newBuilder().build();
        Holder<Integer> holder = new Holder<>(0);
        AtomicInteger loadCount = new AtomicInteger();
        CacheLoader loader = (k,v) -> {
            loadCount.incrementAndGet();
            holder.value = holder.value + 1;
            return holder.value;
        };
        int value = (Integer) cache.getOrLoad("key", loader, 10, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, value);
        Awaits.await().until(() -> loadCount.intValue() > 2);
        value = (Integer) cache.getOrLoad("key", loader, 10, TimeUnit.MILLISECONDS);
        Assert.assertTrue(value > 1);
    }

    @Test
    public void getOrLoad_refresh_null() {
        CacheImpl cache = CacheBuilder.newBuilder().build();
        AtomicReference<Integer> holder = new AtomicReference<>(1);
        AtomicInteger loadCount = new AtomicInteger();
        CacheLoader loader = (k,v) -> {
            loadCount.incrementAndGet();
            return holder.get();
        };
        int value = (Integer) cache.getOrLoad("key", loader, 10, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, value);
        holder.set(null);
        Awaits.await().until(() -> loadCount.intValue() > 2);
        value = (Integer) cache.getOrLoad("key", loader, 10, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, value);
    }

    @Test
    public void getOrLoad_refresh_exception() {
        CacheImpl cache = CacheBuilder.newBuilder().build();
        AtomicReference<Integer> holder = new AtomicReference<>(1);
        AtomicInteger loadCount = new AtomicInteger();
        CacheLoader loader = (k,v) -> {
            loadCount.incrementAndGet();
            if (holder.get() == null) {
                throw new IllegalStateException();
            }
            return holder.get();
        };
        int value = (Integer) cache.getOrLoad("key", loader, 10, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, value);
        holder.set(null);
        Awaits.await().until(() -> loadCount.intValue() > 2);
        value = (Integer) cache.getOrLoad("key", loader, 10, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, value);
    }

    /*------------------------------add----------------------------------------------------------------------*/

    @Test
    public void add() {
        Cache cache = CacheBuilder.newBuilder().build();
        cache.add("key", VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, cache.size());
        Assert.assertEquals(VALUE, cache.get("key"));
    }

    @Test
    public void add_existed() {
        Cache cache = CacheBuilder.newBuilder().build();
        cache.add("key", "value1", Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        cache.add("key", "value2", Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        cache.add("key", "value3", Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, cache.size());
        Assert.assertEquals("value3", cache.get("key"));
    }

    @Test
    public void add_expire() {
        Cache cache = CacheBuilder.newBuilder().build();
        cache.add("key", VALUE, 10, TimeUnit.MILLISECONDS);
        Awaits.await().until(() -> cache.size() == 0);
        Assert.assertNull(cache.get("key"));
    }

    @Test
    public void add_expire_less_than_zero() {
        Cache cache = CacheBuilder.newBuilder().build();
        cache.add("key", VALUE, -Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, cache.size());
        Assert.assertEquals(VALUE, cache.get("key"));
    }

    /*---------------------------------get_or_load  add------------------------------------------------------*/

    @Test
    public void add_then_get_or_add() {
        Cache cache = CacheBuilder.newBuilder().build();
        cache.add("key", VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(VALUE, cache.get("key"));
        Assert.assertEquals(VALUE, cache.getOrLoad("key", (k,v) -> "value2", Long.MAX_VALUE, TimeUnit.MILLISECONDS));
    }

    @Test
    public void add_then_get_or_add_then_expire() {
        Cache cache = CacheBuilder.newBuilder().build();
        cache.add("key", VALUE, 100, TimeUnit.MILLISECONDS);
        Assert.assertEquals(VALUE, cache.get("key"));
        Assert.assertEquals(VALUE, cache.getOrLoad("key", (k,v) -> "value2", Long.MAX_VALUE, TimeUnit.MILLISECONDS));
        Awaits.await().until(() -> cache.size() == 0);
        Assert.assertNull(cache.get("key"));
    }

    @Test
    public void add_expire_then_get_or_add() {
        Cache cache = CacheBuilder.newBuilder().build();
        cache.add("key", VALUE, 10, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, cache.size());
        Awaits.await().until(() -> cache.size() == 0);
        Assert.assertNull(cache.get("key"));
        String value = (String) cache.getOrLoad("key", (k,v) -> "value2", Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals("value2", value);
        Assert.assertEquals("value2", cache.get("key"));
    }

    @Test
    public void get_or_add_then_add() {
        Cache cache = CacheBuilder.newBuilder().build();
        cache.getOrLoad("key", (k,v) -> VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(VALUE, cache.get("key"));
        cache.add("key", "value2", Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals("value2", cache.get("key"));
    }

    @Test
    public void get_or_add_then_add_expire() {
        Cache cache = CacheBuilder.newBuilder().build();
        cache.getOrLoad("key", (k,v) -> VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(VALUE, cache.get("key"));
        cache.add("key", "value2", 10, TimeUnit.MILLISECONDS);
        Awaits.await().until(() -> cache.size() == 0);
        Assert.assertNull(cache.get("key"));
    }

    /*------------------------------remove----------------------------------------------------------------------*/

    @Test
    public void remove() {
        Cache cache = CacheBuilder.newBuilder().build();
        cache.getOrLoad("key", (k,v) -> VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, cache.size());
        Assert.assertEquals(VALUE, cache.get("key"));
        cache.remove("key");
        Assert.assertEquals(0, cache.size());
        Assert.assertNull(cache.get("key"));
    }

    @Test
    public void remove_non_existed() {
        Cache cache = CacheBuilder.newBuilder().build();
        cache.remove("key");
        Assert.assertEquals(0, cache.size());
    }

    /*------------------------------refresh----------------------------------------------------------------------*/

    @Test
    public void refresh_get_or_load() {
        CacheImpl cache = CacheBuilder.newBuilder().build();
        Holder<Integer> holder = new Holder<>(0);
        CacheLoader loader = (k,v) -> {
            holder.value = holder.value + 1;
            return holder.value;
        };
        int value = (Integer) cache.getOrLoad("key", loader, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, value);
        cache.refresh("key");
        value = (Integer) cache.getOrLoad("key", loader, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(2, value);
    }

    @Test
    public void refresh_add() {
        CacheImpl cache = CacheBuilder.newBuilder().build();
        cache.add("key", VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, cache.size());
        Assert.assertEquals(VALUE, cache.get("key"));
        cache.refresh("key");
        Assert.assertEquals(1, cache.size());
        Assert.assertEquals(VALUE, cache.get("key"));
    }

    @Test
    public void refresh_non_existed() {
        CacheImpl cache = CacheBuilder.newBuilder().build();
        cache.refresh("key");
        Assert.assertEquals(0, cache.size());
    }

    /*------------------------------evict----------------------------------------------------------------------*/

    @Test
    public void evict() {
        Cache cache = CacheBuilder.newBuilder().setCapacity(3).build();
        cache.add("key1", VALUE, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        cache.add("key2", VALUE, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        cache.add("key3", VALUE, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        cache.add("key4", VALUE, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        Awaits.await().until(() -> cache.size() != 4);
        Assert.assertEquals(3, cache.size());
        Assert.assertNull(cache.get("key1"));
    }

    @Test
    public void evict_lru() {
        Cache cache = CacheBuilder.newBuilder().setCapacity(3).build();
        cache.add("key1", VALUE, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        cache.add("key2", VALUE, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        cache.add("key3", VALUE, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        cache.get("key1");
        cache.add("key4", VALUE, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        Awaits.await().until(() -> cache.size() != 4);
        Assert.assertEquals(3, cache.size());
        Assert.assertNull(cache.get("key2"));
    }

    /*------------------------------expireAfterAccess----------------------------------------------------------*/

    @Test
    public void expire_after_access() {
        Cache cache = CacheBuilder.newBuilder().build();
        cache.getOrLoad("key", (k,v) -> VALUE, 10, 1000, TimeUnit.MILLISECONDS);
        Awaits.await().until(() -> cache.size() == 0);
        Assert.assertNull(cache.get("key"));
    }

    /*------------------------------size----------------------------------------------------------*/

    @Test
    public void size() {
        Cache cache = CacheBuilder.newBuilder().build();
        Assert.assertEquals(0, cache.size());
        cache.add("key", VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, cache.size());
        cache.getOrLoad("key2", (k,v) -> VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(2, cache.size());
        cache.add("key", "value2", Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(2, cache.size());
        cache.add("key2", VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(2, cache.size());
        cache.remove("key2");
        Assert.assertEquals(1, cache.size());
        cache.remove("key");
        Assert.assertEquals(0, cache.size());
    }

    /*------------------------------listener----------------------------------------------------------*/

    @Test
    public void listener() {
        Map<String, Integer> readMap = new HashMap<>();
        Map<String, Integer> writeMap = new HashMap<>();
        Map<String, Integer> removeMap = new HashMap<>();
        Map<String, Integer> refreshMap = new HashMap<>();
        Listener listener = new Listener() {
            @Override
            public void onRead(String key) {
                int count = readMap.computeIfAbsent(key, k -> 0) + 1;
                readMap.put(key, count);
            }
            @Override
            public void onWrite(String key) {
                int count = writeMap.computeIfAbsent(key, k -> 0) + 1;
                writeMap.put(key, count);
            }
            @Override
            public void onRemove(String key, Object value, RemovalCause cause) {
                key += cause;
                int count = removeMap.computeIfAbsent(key, k -> 0) + 1;
                removeMap.put(key, count);
            }
            @Override
            public void onRefresh(String key) {
                int count = refreshMap.computeIfAbsent(key, k -> 0) + 1;
                refreshMap.put(key, count);
            }
        };

        Cache cache = CacheBuilder.newBuilder().setCapacity(2).registerListener(listener).build();

        // on write
        cache.getOrLoad("key", (k,v) -> VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, writeMap.get("key").intValue());

        cache.add("key", VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(2, writeMap.get("key").intValue());

        // on remove expireAfterWrite
        cache.add("key2", VALUE, 100, TimeUnit.MILLISECONDS);
        Awaits.await().until(() -> removeMap.containsKey("key2" + RemovalCause.EXPIRE_AFTER_WRITE));
        Assert.assertEquals(1, removeMap.get("key2" + RemovalCause.EXPIRE_AFTER_WRITE).intValue());

        // on remove expireAfterAccess
        cache.getOrLoad("key2", (k,v) -> VALUE, 10, 100,TimeUnit.MILLISECONDS);
        Awaits.await().until(() -> removeMap.containsKey("key2" + RemovalCause.EXPIRE_AFTER_ACCESS));
        Assert.assertEquals(1, removeMap.get("key2" + RemovalCause.EXPIRE_AFTER_ACCESS).intValue());

        // on remove evict
        cache.add("key2", VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(2, cache.size());
        cache.add("key3", VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Awaits.await().until(() -> removeMap.containsKey("key" + RemovalCause.EVICT));
        Assert.assertEquals(1, removeMap.get("key" + RemovalCause.EVICT).intValue());

        // on remove user operation
        cache.remove("key2");
        Assert.assertEquals(1, removeMap.get("key2" + RemovalCause.USER_OPERATION).intValue());

        // on remove add
        cache.add("key3", VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, removeMap.get("key3" + RemovalCause.ADD).intValue());

        // on refresh
        cache.getOrLoad("key4", (k,v) -> VALUE, 100, TimeUnit.MILLISECONDS);
        Awaits.await().until(() -> refreshMap.containsKey("key4"));
        Assert.assertEquals(1, refreshMap.get("key4").intValue());

        cache.getOrLoad("key5", (k,v) -> VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        cache.refresh("key5");
        Assert.assertEquals(1, refreshMap.get("key5").intValue());

        // on read
        cache.getOrLoad("key6", (k,v) -> VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, readMap.get("key6").intValue());
        cache.getOrLoad("key6", (k,v) -> VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Assert.assertEquals(2, readMap.get("key6").intValue());
        cache.get("key6");
        Assert.assertEquals(3, readMap.get("key6").intValue());
    }



}
