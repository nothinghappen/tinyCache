package com.nothinghappen.cache;

import java.util.concurrent.TimeUnit;

public interface Ticker {

    long read();

    long convert(long value, TimeUnit unit);

    long read(TimeUnit timeUnit);
}
