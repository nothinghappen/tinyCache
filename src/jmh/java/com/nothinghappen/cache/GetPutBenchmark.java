package com.nothinghappen.cache;

import com.nothinghappen.cache.base.BasicCache;
import com.nothinghappen.cache.base.CacheType;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, jvmArgs = {"-server"})
@BenchmarkMode({Mode.Throughput})
@Warmup(iterations = 2, time = 30, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 30, timeUnit = TimeUnit.SECONDS)
@State(Scope.Group)
public class GetPutBenchmark {

    private static final int ITEMS_COUNT = 1000000;
    private static final int BATCH_SIZE = 1000;
    private String[] keys = new String[ITEMS_COUNT];

    @State(Scope.Thread)
    public static class ThreadState {
        public int[] index = new int[BATCH_SIZE];

        public ThreadState() {
            for (int i = 0; i < BATCH_SIZE; i++) {
                Random random = new Random();
                index[i] = random.nextInt(ITEMS_COUNT);
            }
        }
    }

    @Param({
            "Cache",
            "Caffeine"
    })
    private CacheType cacheType;
    private BasicCache<byte[]> cache;

    @Setup
    public void setup() {
        cache = cacheType.create(ITEMS_COUNT);
        for (int i = 0; i < ITEMS_COUNT; i++) {
            keys[i] = getKey(i);
            cache.put(getKey(i), getValue());
        }
    }

    @Benchmark
    @Group("read_only")
    @GroupThreads(1)
    public void readOnly(ThreadState state) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            cache.get(keys[state.index[i]]);
        }
    }

    @Benchmark
    @Group("read_put")
    @GroupThreads(2)
    public void read(ThreadState state) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            cache.get(keys[state.index[i]]);
        }
    }

    @Benchmark
    @Group("read_put")
    @GroupThreads(1)
    public void put(ThreadState state) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            cache.put(getKey(state.index[i]), getValue());
        }
    }

    private String getKey(int i) {
        return "key" + i;
    }

    private byte[] getValue() {
        return new byte[1024];
    }

}
