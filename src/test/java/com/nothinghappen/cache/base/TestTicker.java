package com.nothinghappen.cache.base;

import com.nothinghappen.cache.Ticker;

import java.util.concurrent.TimeUnit;

public class TestTicker implements Ticker {

    public long ticks = 0;

    @Override
    public long read() {
        return ticks;
    }
    @Override
    public long convert(long value, TimeUnit unit) {
        return TimeUnit.MILLISECONDS.convert(value, unit);
    }
    @Override
    public long read(TimeUnit timeUnit) {
        return timeUnit.convert(read(), TimeUnit.MILLISECONDS);
    }
}
