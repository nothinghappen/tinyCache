package com.nothinghappen.cache;

import com.nothinghappen.cache.base.MockUtils;
import com.nothinghappen.cache.base.SyncExecutor;
import com.nothinghappen.cache.base.TestTicker;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.xml.ws.Holder;

import java.util.concurrent.TimeUnit;


public class CacheTest2 {

    private static String VALUE = "value";
    private static String KEY = "key";

    private TestTicker ticker;
    private TestTicker timeWheelTicker;
    private CacheImpl cache;
    private CacheLoader loader;
    private long expireMills;

    @Before
    public void mock() {
        MockUtils.mockCompletableFuture();
    }

    @After
    public void after() {
        this.ticker = null;
        this.timeWheelTicker = null;
        this.cache = null;
        this.loader = null;
        this.expireMills = 0;
    }

    @Test
    public void get_or_Load_refresh() {

        this.ticker = new TestTicker();
        this.cache = createUnboundedCacheWithTicker(ticker);
        Holder<Integer> holder = new Holder<>(0);
        this.loader = (k, v) -> {
            holder.value = holder.value + 1;
            return holder.value;
        };
        this.expireMills = 60000;
        long minDelayedMills = TimeUnit.MILLISECONDS.convert(cache.MIN_DELAYED_NANO, TimeUnit.NANOSECONDS);

        refreshWhenExpire(0, 1);

        for (int i = 1; i < 1000; i++) {
            refreshWhenExpire(i * (expireMills - minDelayedMills) - 1, i);
            refreshWhenExpire(i * (expireMills - minDelayedMills), i + 1);
        }
    }

    @Test
    public void add_expire() {
        this.ticker = new TestTicker();
        this.cache = createUnboundedCacheWithTicker(ticker);
        this.expireMills = 60000;
        long minDelayedMills = TimeUnit.MILLISECONDS.convert(cache.MIN_DELAYED_NANO, TimeUnit.NANOSECONDS);

        cache.add(KEY, VALUE, expireMills, TimeUnit.MILLISECONDS);

        nullWhenExpire(0, false);

        nullWhenExpire(expireMills - minDelayedMills - 1, false);

        nullWhenExpire(expireMills - minDelayedMills, true);
    }

    @Test
    public void expire_after_access() {
        this.ticker = new TestTicker();
        this.timeWheelTicker = new TestTicker();
        this.cache = createUnboundedCacheWithTicker(ticker, timeWheelTicker);
        this.expireMills = 60000;
        this.loader = (k,v) -> VALUE;

        cache.getOrLoad(KEY, loader, Long.MAX_VALUE, expireMills, TimeUnit.MILLISECONDS);

        nullWhenExpireAfterAccess(0, false);

        nullWhenExpireAfterAccess(expireMills - 1, false);

        nullWhenExpireAfterAccess(expireMills, true);

        cache.getOrLoad(KEY, loader, Long.MAX_VALUE, expireMills, TimeUnit.MILLISECONDS);

        nullWhenExpireAfterAccess(expireMills, false);

        nullWhenExpireAfterAccess(2 * expireMills - 1, false);

        // access
        cache.get(KEY);

        nullWhenExpireAfterAccess(2 * expireMills, false);

        nullWhenExpireAfterAccess(3 * expireMills - 1, false);

        nullWhenExpireAfterAccess(3 * expireMills, true);

    }

    private void nullWhenExpireAfterAccess(long ticks, boolean isNull) {
        timeWheelTicker.ticks = ticks;
        ticker.ticks = ticks;
        CachingTest.maintenance(cache);
        if (isNull) {
            Assert.assertFalse(CachingTest.peek(cache, KEY));
        } else {
            Assert.assertTrue(CachingTest.peek(cache, KEY));
        }
    }

    private void nullWhenExpire(long ticks, boolean isNull) {
        ticker.ticks = ticks;
        CachingTest.maintenance(cache);
        String value = (String) cache.get(KEY);
        if (isNull) {
            Assert.assertNull(value);
        } else {
            Assert.assertNotNull(value);
        }
    }

    private void refreshWhenExpire(long ticks, int expected) {
        ticker.ticks = ticks;
        CachingTest.maintenance(cache);
        int value = (Integer) cache.getOrLoad(KEY, loader, expireMills, TimeUnit.MILLISECONDS);
        Assert.assertEquals(expected, value);
    }

    private static CacheImpl createUnboundedCacheWithTicker(Ticker ticker) {
        return new CacheImpl(0,
                new SyncExecutor(),
                new AdvancedOption().setCacheTicker(ticker),
                new ListenerChain());
    }

    private static CacheImpl createUnboundedCacheWithTicker(Ticker ticker, Ticker timeWheelTicker) {
        return new CacheImpl(0,
                new SyncExecutor(),
                new AdvancedOption().setCacheTicker(ticker).setTimeWheelTicker(timeWheelTicker),
                new ListenerChain());
    }
}
