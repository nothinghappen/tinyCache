package com.nothinghappen.cache.datastruct;

import java.util.concurrent.atomic.AtomicLong;

/**
 * a MPSC ring buffer
 * @param <T>
 */
public class RingBuffer<T> implements Buffer<T> {

    private Object[] data;
    private int capacity;
    private int mask;
    private AtomicLong head = new AtomicLong();
    private AtomicLong tail = new AtomicLong();


    /**
     * capacity = 2 ^ power
     * @param power
     */
    public RingBuffer(int power) {
        if (power < 0) {
            power = 0;
        }
        if (power > 30) {
            power = 30;
        }
        this.capacity = 1 << power;
        this.mask = this.capacity - 1;
        this.data = new Object[this.capacity];
    }

    /**
     * offer
     * @param element
     * @return false if buffer is full
     */
    @Override
    public boolean offer(T element) {
        for(;;) {
            long t = tail.get();
            long h = head.get();
            long size = t - h;
            if (size == capacity) {
                // is full
                return false;
            }
            if (tail.compareAndSet(t, t + 1)) {
                // lazy set
                data[(int) ((t + 1) & mask)] = element;
                return true;
            }
        }
    }

    /**
     * poll
     * @return null if buffer is empty
     */
    @Override
    public T poll() {
        long t = tail.get();
        long h = head.get();
        if (t == h) {
            // is empty
            return null;
        }
        int index = (int) ((h + 1) & mask);
        T element = (T) data[index];
        if (element == null) {
            // not published yet
            return null;
        }
        data[index] = null;
        head.set(h + 1);
        return element;
    }

    @Override
    public long size() {
        return tail.get() - head.get();
    }

    @Override
    public long capacity() {
        return this.capacity;
    }

}
