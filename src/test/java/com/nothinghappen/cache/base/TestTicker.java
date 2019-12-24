package com.nothinghappen.cache.base;

import com.nothinghappen.cache.Ticker;

import java.util.concurrent.TimeUnit;

public class TestTicker implements Ticker {

    public static TestTicker INSTANCE = new TestTicker();

    public long ticks = 0;

    public TimeUnit unit = TimeUnit.MILLISECONDS;

    @Override
    public long read() {
        return ticks;
    }
    @Override
    public long convert(long value, TimeUnit timeUnit) {
        return unit.convert(value, timeUnit);
    }
    @Override
    public long read(TimeUnit timeUnit) {
        return timeUnit.convert(read(), unit);
    }
}
