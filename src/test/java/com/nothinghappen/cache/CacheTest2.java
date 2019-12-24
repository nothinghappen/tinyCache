package com.nothinghappen.cache;

import com.nothinghappen.cache.base.MockUtils;
import com.nothinghappen.cache.base.SyncExecutor;
import com.nothinghappen.cache.base.TestTicker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.xml.ws.Holder;

import java.util.concurrent.TimeUnit;


public class CacheTest2 {

    private String VALUE = "value";

    @Before
    public void mock() {
        MockUtils.mockCompletableFuture();
    }

    @Test
    public void getOrLoad_refresh() {

        TestTicker ticker = new TestTicker();
        CacheImpl cache = new CacheImpl(0,
                new SyncExecutor(),
                new AdvancedOption().setCacheTicker(ticker),
                new ListenerChain());
        Holder<Integer> holder = new Holder<>(0);
        CacheLoader loader = (k, v) -> {
            holder.value = holder.value + 1;
            return holder.value;
        };
        long expireMills = 60000;
        long minDelayedMills = TimeUnit.MILLISECONDS.convert(cache.MIN_DELAYED_NANO, TimeUnit.NANOSECONDS);
        int value = (Integer) cache.getOrLoad("key", loader, expireMills, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, value);
        ticker.ticks = expireMills - minDelayedMills - 1;
        CachingTest.maintenance(cache);
        value = (Integer) cache.getOrLoad("key", loader, expireMills, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, value);
        ticker.ticks = expireMills - minDelayedMills;
        CachingTest.maintenance(cache);
        value = (Integer) cache.getOrLoad("key", loader, expireMills, TimeUnit.MILLISECONDS);
        Assert.assertEquals(2, value);
    }
}
