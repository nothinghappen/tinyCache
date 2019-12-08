package com.nothinghappen.cache;

import com.nothinghappen.cache.base.ConcurrentTest;
import com.nothinghappen.cache.datastruct.RingBuffer;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class RingBufferTest {

    @Test
    public void  test() {
        RingBuffer<String> rb = new RingBuffer<>(2);
        Assert.assertEquals(true, rb.offer("hello1"));
        Assert.assertEquals(true, rb.offer("hello2"));
        Assert.assertEquals(true, rb.offer("hello3"));
        Assert.assertEquals(true, rb.offer("hello4"));
        Assert.assertEquals(false, rb.offer("hello5"));
        Assert.assertEquals("hello1", rb.poll());
        Assert.assertEquals("hello2", rb.poll());
        Assert.assertEquals("hello3", rb.poll());
        Assert.assertEquals("hello4", rb.poll());
        Assert.assertNull(rb.poll());
    }

    @Test
    public void concurrentRun() throws InterruptedException {
        RingBuffer<String> ringBuffer = new RingBuffer<>(8);
        AtomicBoolean offerFlag = new AtomicBoolean(true);
        AtomicBoolean pollFlag = new AtomicBoolean(true);
        AtomicLong offer = new AtomicLong();
        AtomicLong poll = new AtomicLong();
        ConcurrentTest.runAsync(10, () -> {
            while (offerFlag.get()) {
                if (ringBuffer.offer("hello")) {
                    offer.incrementAndGet();
                }
            }
        });
        ConcurrentTest.runAsync(1, () -> {
            while (pollFlag.get()) {
                String s = ringBuffer.poll();
                if (s != null) {
                    poll.incrementAndGet();
                }
            }
        });
        Thread.sleep(3000);
        offerFlag.set(false);
        //drain buffer
        Thread.sleep(1000);
        pollFlag.set(false);
        Assert.assertEquals(offer.get(), poll.get());
    }
}
