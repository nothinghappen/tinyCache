package com.nothinghappen.cache.datastruct;

public interface WheelTimerConsumer<T> {

    void accept(T value, WheelTimer<T> wheel, long deltaTicks);

}
