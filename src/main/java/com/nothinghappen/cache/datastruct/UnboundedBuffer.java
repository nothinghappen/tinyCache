package com.nothinghappen.cache.datastruct;

import java.util.concurrent.ConcurrentLinkedQueue;

public class UnboundedBuffer<T> implements Buffer<T> {

    final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();

    @Override
    public boolean offer(T t) {
        return queue.offer(t);
    }
    @Override
    public T poll() {
        return queue.poll();
    }
    @Override
    public long size() {
        return queue.size();
    }
    @Override
    public long capacity() {
        return -1;
    }
}
