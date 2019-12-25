package com.nothinghappen.cache;

import com.nothinghappen.cache.datastruct.WheelTimer;
import com.nothinghappen.cache.datastruct.WheelTimerTask;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, jvmArgs = {"-server"})
@BenchmarkMode({Mode.Throughput})
@Warmup(iterations = 2, time = 30, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 30, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
public class WheelTimerBenchmark {


    private final int CAPACITY = 10000;
    // size = 16
    private WheelTimer timewheel = new WheelTimer(4, 0);
    private TestWheelTimerTask[] timerNodes = new TestWheelTimerTask[CAPACITY];

    private TreeSet<Integer> treeSet = new TreeSet<>();
    private Integer integer;


    @Param ({
        "15","255","4095"
    })
    public int delay;

    @Setup
    public void init() {

        integer = delay;
        for (int i = 0; i < CAPACITY; i++) {
            timerNodes[i] = new TestWheelTimerTask();
            timewheel.schedule(timerNodes[i], i);
            treeSet.add(i);
        }
    }

    @Benchmark
    public void wheelTimer() {
        timewheel.schedule(timerNodes[delay], delay);
    }

    @Benchmark
    public void treeSet() {
        treeSet.remove(integer);
        treeSet.add(integer);
    }

    private static class TestWheelTimerTask extends WheelTimerTask {

        @Override
        public void run(long deltaTicket) {

        }
    }
}
