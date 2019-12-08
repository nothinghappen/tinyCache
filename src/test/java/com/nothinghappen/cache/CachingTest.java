package com.nothinghappen.cache;

import com.nothinghappen.cache.CacheImpl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class CachingTest {

    public static boolean peek(CacheImpl cache, String key) {
        return cache.data.get(key) != null;

    }

    public static void stopBackend(CacheImpl cache) {
        cache.backend.running = false;
    }

    public static void maintenance(CacheImpl cache) {
        cache.backend.drainUserEvent();
        cache.backend.doScheduleTask();
        cache.backend.drainBuffer();
        cache.backend.evict();
        cache.backend.expireAfterAccess();
    }


}
