package com.nothinghappen.cache;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CacheBuilder {

    public static CacheBuilder newBuilder() {
        return new CacheBuilder();
    }

    private ExecutorService refreshBackend = new ThreadPoolExecutor(3,
            3,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());

    private int capacity = 0;

    private AdvancedOption advancedOption = new AdvancedOption();

    public CacheBuilder advancedOption(AdvancedOption advancedOption) {
        this.advancedOption = advancedOption;
        return this;
    }
    public CacheBuilder setCapacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    public CacheBuilder setRefreshBackend(ExecutorService refreshBackend) {
        this.refreshBackend = refreshBackend;
        return this;
    }

    public CacheImpl build() {
        return new CacheImpl(capacity, refreshBackend, advancedOption);
    }

}
