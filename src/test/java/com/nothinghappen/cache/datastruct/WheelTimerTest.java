package com.nothinghappen.cache.datastruct;

import com.nothinghappen.cache.datastruct.WheelTimer;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.ws.Holder;
import java.util.HashSet;
import java.util.List;

public class WheelTimerTest {

    @Test
    public void add_advance() {
        do_add_advance(0);
        do_add_advance(1000);
        do_add_advance(-1000);
    }

    private void do_add_advance(long now) {
        HashSet<String> strings = new HashSet<>();
        WheelTimer<String> timewheel = new WheelTimer<>(2, (s, w, d) -> strings.add(s), now);
        for (int i = 0; i < 10; i++) {
            strings.clear();
            timewheel.add("1", now);
            timewheel.add("2", now + 1);

            timewheel.add("3", now + 3);
            timewheel.add("4", now + 4);
            timewheel.add("5", now + 5);

            timewheel.add("6", now + 15);
            timewheel.add("7", now + 16);
            timewheel.add("8", now + 17);

            timewheel.add("9", now + 63);
            timewheel.add("10", now + 64);
            timewheel.add("11", now + 65);

            timewheel.advance(now);
            Assert.assertTrue(strings.isEmpty());
            timewheel.advance(now + 1);
            Assert.assertTrue(strings.contains("1"));
            Assert.assertTrue(strings.contains("2"));
            Assert.assertEquals(2, strings.size());
            timewheel.advance(now + 2);
            Assert.assertEquals(2, strings.size());
            timewheel.advance(now + 3);
            Assert.assertTrue(strings.contains("3"));
            Assert.assertEquals(3, strings.size());
            timewheel.advance(now + 4);
            Assert.assertTrue(strings.contains("4"));
            Assert.assertEquals(4, strings.size());
            timewheel.advance(now + 5);
            Assert.assertTrue(strings.contains("5"));
            Assert.assertEquals(5, strings.size());
            timewheel.advance(now + 6);
            Assert.assertEquals(5, strings.size());
            timewheel.advance(now + 15);
            Assert.assertTrue(strings.contains("6"));
            Assert.assertEquals(6, strings.size());
            timewheel.advance(now + 16);
            Assert.assertTrue(strings.contains("7"));
            Assert.assertEquals(7, strings.size());
            timewheel.advance(now + 17);
            Assert.assertTrue(strings.contains("8"));
            Assert.assertEquals(8, strings.size());
            timewheel.advance(now + 62);
            Assert.assertEquals(8, strings.size());
            timewheel.advance(now + 63);
            Assert.assertTrue(strings.contains("9"));
            Assert.assertEquals(9, strings.size());
            timewheel.advance(now + 64);
            Assert.assertTrue(strings.contains("10"));
            Assert.assertEquals(10, strings.size());
            timewheel.advance(now + 65);
            Assert.assertTrue(strings.contains("11"));
            Assert.assertEquals(11, strings.size());
            now += 65;
        }
    }

    @Test
    public void add_advance_max() {
        HashSet<String> strings = new HashSet<>();
        WheelTimer<String> timewheel = new WheelTimer<>(2, (s, w, d) -> strings.add(s), 0);
        timewheel.add("value", Long.MAX_VALUE);
        timewheel.advance(Long.MAX_VALUE - 1);
        Assert.assertTrue(strings.isEmpty());
        timewheel.advance(Long.MAX_VALUE);
        Assert.assertTrue(strings.contains("value"));
    }

    @Test
    public void add_bad_params() {
        HashSet<String> strings = new HashSet<>();
        WheelTimer<String> timewheel = new WheelTimer<>(2, (s, w, d) -> strings.add(s), 0);
        timewheel.add("value", -1);
        timewheel.advance(0);
        Assert.assertTrue(strings.isEmpty());
        timewheel.advance(1);
        Assert.assertTrue(strings.contains("value"));
    }

    @Test
    public void advance_bad_params() {
        HashSet<String> strings = new HashSet<>();
        WheelTimer<String> timewheel = new WheelTimer<>(2, (s, w, d) -> strings.add(s), 4);
        timewheel.add("value", 21);
        timewheel.advance(3); // less than now
        Assert.assertTrue(strings.isEmpty());
    }
}
