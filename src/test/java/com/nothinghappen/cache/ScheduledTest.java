package com.nothinghappen.cache;

import com.nothinghappen.cache.base.TestTicker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

public class ScheduledTest {

    @Test
    public void test_constructor_overflow() {
        TestTicker ticker = new TestTicker();
        ticker.ticks = 1;
        ticker.unit = TimeUnit.NANOSECONDS;
        Scheduled s1 = new Scheduled(Long.MAX_VALUE, ticker);
        Scheduled s2 = new Scheduled(Long.MAX_VALUE, ticker);
        Assert.assertEquals(Long.MAX_VALUE - 1 , s1.getDelay(TimeUnit.NANOSECONDS));
        Assert.assertEquals(Long.MAX_VALUE - 1 , s2.getDelay(TimeUnit.NANOSECONDS));
    }

    @Test
    public void test_constructor_less_than_zero() {
        TestTicker ticker = new TestTicker();
        ticker.ticks = 1;
        ticker.unit = TimeUnit.NANOSECONDS;
        Scheduled s1 = new Scheduled( -Long.MAX_VALUE, ticker);
        Scheduled s2 = new Scheduled( -Long.MAX_VALUE, ticker);
        Assert.assertEquals(Long.MAX_VALUE - 1 , s1.getDelay(TimeUnit.NANOSECONDS));
        Assert.assertEquals(Long.MAX_VALUE - 1 , s2.getDelay(TimeUnit.NANOSECONDS));
    }

    @Test
    public void compareTo() {
        Scheduled s = new Scheduled(10, TestTicker.INSTANCE);
        Scheduled s1 = new Scheduled(10, TestTicker.INSTANCE);
        Scheduled s2 = new Scheduled(11, TestTicker.INSTANCE);
        Assert.assertTrue(s.compareTo(s2) < 0);
        Assert.assertTrue(s2.compareTo(s) > 0);
        Assert.assertEquals(0, s.compareTo(s1));
    }

}
