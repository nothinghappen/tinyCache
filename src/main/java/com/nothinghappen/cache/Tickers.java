package com.nothinghappen.cache;

import java.util.concurrent.TimeUnit;

public enum Tickers implements Ticker{

    NANO {
        @Override
        public long read() {
            return System.nanoTime();
        }
        @Override
        public long convert(long value, TimeUnit unit) {
            return TimeUnit.NANOSECONDS.convert(value, unit);
        }
        @Override
        public long read(TimeUnit timeUnit) {
            return timeUnit.convert(read(), TimeUnit.NANOSECONDS);
        }
    },
    MILLIS {
        @Override
        public long read() {
            return TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
        }
        @Override
        public long convert(long value, TimeUnit unit) {
            return TimeUnit.MILLISECONDS.convert(value, unit);
        }
        @Override
        public long read(TimeUnit timeUnit) {
            return timeUnit.convert(read(), TimeUnit.MILLISECONDS);
        }
    },
    SECONDS {
        @Override
        public long read() {
            return TimeUnit.SECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
        }
        @Override
        public long convert(long value, TimeUnit unit) {
            return TimeUnit.SECONDS.convert(value, unit);
        }
        @Override
        public long read(TimeUnit timeUnit) {
            return timeUnit.convert(read(), TimeUnit.SECONDS);
        }
    };

    @Override
    public abstract long read();
}
