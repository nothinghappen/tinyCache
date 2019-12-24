package com.nothinghappen.cache;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class Scheduled implements Delayed {

    private long deadline;
    private long delayNanos;
    private final Ticker ticker;

    public Scheduled(long delayNanos, Ticker ticker) {
        this.ticker = ticker;
        this.delayNanos = negativeFree(delayNanos);
        this.deadline = overflowFree(this.delayNanos, now());
    }

    public void reset() {
        this.deadline = overflowFree(this.delayNanos, now());
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(deadline - now(), TimeUnit.NANOSECONDS);
    }


    public void update(long delayNanos) {
        this.delayNanos = negativeFree(delayNanos);
    }

    @Override
    public int compareTo(Delayed other) {
        if (this == other) {
            return 0;
        }
        long diff = getDelay(NANOSECONDS) - other.getDelay(NANOSECONDS);
        return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
    }

    private long now() {
        return ticker.read(NANOSECONDS);
    }

    /**
     * Long.MAX_VALUE if it would positively overflow.
     */
    private long overflowFree(long delayNanos, long now) {
        if (Long.MAX_VALUE - delayNanos < now) {
            return Long.MAX_VALUE;
        }
        return delayNanos + now;
    }

    /**
     * Long.MAX_VALUE if value <= 0
     * @param value
     * @return
     */
    private long negativeFree(long value) {
        return value <= 0 ? Long.MAX_VALUE : value;
    }
}
