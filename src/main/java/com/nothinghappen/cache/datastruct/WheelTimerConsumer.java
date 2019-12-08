package com.nothinghappen.cache.datastruct;

public interface WheelTimerConsumer<T, W, D> {

    void accept(T value, W wheel, D deltaTicks);

}
