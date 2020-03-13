package com.nothinghappen.cache.datastruct;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class WheelTimerTest {

    @Test
    public void schedule_advance() {
        do_schedule_advance(0);
        do_schedule_advance(1000);
        do_schedule_advance(-1000);
    }

    private void do_schedule_advance(long now) {
        HashSet<String> strings = new HashSet<>();
        WheelTimer wheelTimer = new WheelTimer(2, now);
        for (int i = 0; i < 10; i++) {
            strings.clear();
            wheelTimer.schedule(new TestWheelTimerTask(() -> strings.add("1")), now);
            wheelTimer.schedule(new TestWheelTimerTask(() -> strings.add("2")), now + 1);

            wheelTimer.schedule(new TestWheelTimerTask(() -> strings.add("3")), now + 3);
            wheelTimer.schedule(new TestWheelTimerTask(() -> strings.add("4")), now + 4);
            wheelTimer.schedule(new TestWheelTimerTask(() -> strings.add("5")), now + 5);

            wheelTimer.schedule(new TestWheelTimerTask(() -> strings.add("6")), now + 15);
            wheelTimer.schedule(new TestWheelTimerTask(() -> strings.add("7")), now + 16);
            wheelTimer.schedule(new TestWheelTimerTask(() -> strings.add("8")), now + 17);

            wheelTimer.schedule(new TestWheelTimerTask(() -> strings.add("9")), now + 63);
            wheelTimer.schedule(new TestWheelTimerTask(() -> strings.add("10")), now + 64);
            wheelTimer.schedule(new TestWheelTimerTask(() -> strings.add("11")), now + 65);

            wheelTimer.advance(now);
            Assert.assertTrue(strings.isEmpty());
            wheelTimer.advance(now + 1);
            Assert.assertTrue(strings.contains("1"));
            Assert.assertTrue(strings.contains("2"));
            Assert.assertEquals(2, strings.size());
            wheelTimer.advance(now + 2);
            Assert.assertEquals(2, strings.size());
            wheelTimer.advance(now + 3);
            Assert.assertTrue(strings.contains("3"));
            Assert.assertEquals(3, strings.size());
            wheelTimer.advance(now + 4);
            Assert.assertTrue(strings.contains("4"));
            Assert.assertEquals(4, strings.size());
            wheelTimer.advance(now + 5);
            Assert.assertTrue(strings.contains("5"));
            Assert.assertEquals(5, strings.size());
            wheelTimer.advance(now + 6);
            Assert.assertEquals(5, strings.size());
            wheelTimer.advance(now + 15);
            Assert.assertTrue(strings.contains("6"));
            Assert.assertEquals(6, strings.size());
            wheelTimer.advance(now + 16);
            Assert.assertTrue(strings.contains("7"));
            Assert.assertEquals(7, strings.size());
            wheelTimer.advance(now + 17);
            Assert.assertTrue(strings.contains("8"));
            Assert.assertEquals(8, strings.size());
            wheelTimer.advance(now + 62);
            Assert.assertEquals(8, strings.size());
            wheelTimer.advance(now + 63);
            Assert.assertTrue(strings.contains("9"));
            Assert.assertEquals(9, strings.size());
            wheelTimer.advance(now + 64);
            Assert.assertTrue(strings.contains("10"));
            Assert.assertEquals(10, strings.size());
            wheelTimer.advance(now + 65);
            Assert.assertTrue(strings.contains("11"));
            Assert.assertEquals(11, strings.size());
            now += 65;
        }
    }

    @Test
    public void add_advance_max() {
        HashSet<String> strings = new HashSet<>();
        WheelTimer wheelTimer = new WheelTimer(2, 0);
        wheelTimer.schedule(new TestWheelTimerTask(() -> strings.add("value")), Long.MAX_VALUE);
        wheelTimer.advance(Long.MAX_VALUE - 1);
        Assert.assertTrue(strings.isEmpty());
        wheelTimer.advance(Long.MAX_VALUE);
        Assert.assertTrue(strings.contains("value"));
    }

    @Test
    public void add_bad_params() {
        HashSet<String> strings = new HashSet<>();
        WheelTimer wheelTimer = new WheelTimer(2, 0);
        wheelTimer.schedule(new TestWheelTimerTask(() -> strings.add("value")), -1);
        wheelTimer.advance(0);
        Assert.assertTrue(strings.isEmpty());
        wheelTimer.advance(1);
        Assert.assertTrue(strings.contains("value"));
    }

    @Test
    public void advance_bad_params() {
        HashSet<String> strings = new HashSet<>();
        WheelTimer wheelTimer = new WheelTimer(2, 4);
        wheelTimer.schedule(new TestWheelTimerTask(() -> strings.add("value")), 21);
        wheelTimer.advance(3); // less than now
        Assert.assertTrue(strings.isEmpty());
    }

    @Test
    public void schedule() {
        AtomicInteger count = new AtomicInteger();
        TestWheelTimerTask task = new TestWheelTimerTask(count::incrementAndGet);
        WheelTimer wheelTimer = new WheelTimer(2, 0);
        wheelTimer.schedule(task, 1);
        wheelTimer.schedule(task, 2);
        wheelTimer.schedule(task, 5);
        wheelTimer.advance(1);
        Assert.assertEquals(count.intValue(), 0);
        wheelTimer.advance(2);
        Assert.assertEquals(count.intValue(), 0);
        wheelTimer.advance(5);
        Assert.assertEquals(count.intValue(), 1);
    }

    @Test
    public void unschedule() {
        AtomicInteger count = new AtomicInteger();
        TestWheelTimerTask task = new TestWheelTimerTask(count::incrementAndGet);
        WheelTimer wheelTimer = new WheelTimer(2, 0);
        wheelTimer.schedule(task, 1);
        wheelTimer.unschedule(task);
        wheelTimer.unschedule(task);
        wheelTimer.unschedule(task);
        wheelTimer.advance(1);
        Assert.assertEquals(count.intValue(), 0);
    }


    private static class TestWheelTimerTask extends WheelTimerTask {

        private Runnable command;

        public TestWheelTimerTask(Runnable command) {
            this.command = command;
        }

        @Override
        public void run(long delay) {
            this.command.run();
        }
    }
}
