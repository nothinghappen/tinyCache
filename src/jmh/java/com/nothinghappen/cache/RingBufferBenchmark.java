package com.nothinghappen.cache;

import com.nothinghappen.cache.datastruct.RingBuffer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, jvmArgs = {"-server"})
@BenchmarkMode({Mode.Throughput})
@Warmup(iterations = 2, time = 30, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 30, timeUnit = TimeUnit.SECONDS)
@State(Scope.Group)
public class RingBufferBenchmark {

    private RingBuffer<String> rb = new RingBuffer<>(8);

    @Benchmark
    @Group("offer_poll")
    @GroupThreads(1)
    public void offer() {
        rb.offer("hello world");
    }

    @Benchmark
    @Group("offer_poll")
    @GroupThreads(1)
    public void poll() {
        rb.poll();
    }

}
