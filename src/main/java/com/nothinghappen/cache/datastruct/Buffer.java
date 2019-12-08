package com.nothinghappen.cache.datastruct;

public interface Buffer<T> {

    boolean offer(T t);

    T poll();

    long size();

    /**
     * capacity of this buff, negative if unbounded
     */
    long capacity();

}
