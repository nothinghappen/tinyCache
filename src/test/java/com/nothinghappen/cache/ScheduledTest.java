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
        Scheduled<Void> s1 = new Scheduled<>(null, Long.MAX_VALUE, TimeUnit.NANOSECONDS, ticker);
        Scheduled<Void> s2 = new Scheduled<>(null, Long.MAX_VALUE, TimeUnit.MILLISECONDS, ticker);
        Assert.assertEquals(Long.MAX_VALUE - 1 , s1.getDelay(TimeUnit.NANOSECONDS));
        Assert.assertEquals(Long.MAX_VALUE - 1 , s2.getDelay(TimeUnit.NANOSECONDS));
    }

    @Test
    public void test_constructor_less_than_zero() {
        TestTicker ticker = new TestTicker();
        ticker.ticks = 1;
        ticker.unit = TimeUnit.NANOSECONDS;
        Scheduled<Void> s1 = new Scheduled<>(null, -Long.MAX_VALUE, TimeUnit.NANOSECONDS, ticker);
        Scheduled<Void> s2 = new Scheduled<>(null, -Long.MAX_VALUE, TimeUnit.MILLISECONDS, ticker);
        Assert.assertEquals(Long.MAX_VALUE - 1 , s1.getDelay(TimeUnit.NANOSECONDS));
        Assert.assertEquals(Long.MAX_VALUE - 1 , s2.getDelay(TimeUnit.NANOSECONDS));
    }

    @Test
    public void compareTo() {
        Scheduled<Void> s = new Scheduled<>(null, 10, TimeUnit.MILLISECONDS, TestTicker.INSTANCE);
        Scheduled<Void> s1 = new Scheduled<>(null, 10, TimeUnit.MILLISECONDS, TestTicker.INSTANCE);
        Scheduled<Void> s2 = new Scheduled<>(null, 11, TimeUnit.MILLISECONDS, TestTicker.INSTANCE);
        Assert.assertTrue(s.compareTo(s2) < 0);
        Assert.assertTrue(s2.compareTo(s) > 0);
        Assert.assertEquals(0, s.compareTo(s1));
    }

}
