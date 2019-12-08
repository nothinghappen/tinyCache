package com.nothinghappen.cache;

import com.nothinghappen.cache.datastruct.WheelTimer;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.ws.Holder;
import java.util.HashSet;
import java.util.List;

public class WheelTimerTest {

    @Test
    public void test() {
        HashSet<String> strings = new HashSet<>();
        WheelTimer<String> timewheel = new WheelTimer<>(2, (s, w, d) -> strings.add(s), 0);
        long now = 0;
        for (int i = 0; i < 10; i++) {
            strings.clear();
            timewheel.add("1", now + 0);
            timewheel.add("2", now + 1);
            timewheel.add("3", now + 3);
            timewheel.add("4", now + 4);
            timewheel.add("5", now + 5);
            timewheel.add("6", now + 7);
            timewheel.add("7", now + 8);
            timewheel.add("8", now + 9);
            timewheel.add("9", now + 63);
            timewheel.add("10", now + 64);
            timewheel.add("11", now + 65);

            timewheel.advance(now + 0);
            Assert.assertTrue(strings.isEmpty());
            timewheel.advance(now + 1);
            Assert.assertTrue(strings.contains("1"));
            Assert.assertTrue(strings.contains("2"));
            timewheel.advance(now + 2);
            Assert.assertEquals(2, strings.size());
            timewheel.advance(now + 3);
            Assert.assertTrue(strings.contains("3"));
            timewheel.advance(now + 4);
            Assert.assertTrue(strings.contains("4"));
            timewheel.advance(now + 5);
            Assert.assertTrue(strings.contains("5"));
            timewheel.advance(now + 6);
            Assert.assertEquals(5, strings.size());
            timewheel.advance(now + 7);
            Assert.assertTrue(strings.contains("6"));
            timewheel.advance(now + 8);
            Assert.assertTrue(strings.contains("7"));
            timewheel.advance(now + 9);
            Assert.assertTrue(strings.contains("8"));
            timewheel.advance(now + 62);
            Assert.assertEquals(8, strings.size());
            timewheel.advance(now + 63);
            Assert.assertTrue(strings.contains("9"));
            timewheel.advance(now + 64);
            Assert.assertTrue(strings.contains("10"));
            timewheel.advance(now + 65);
            Assert.assertTrue(strings.contains("11"));
            now += 65;
        }
    }
}
