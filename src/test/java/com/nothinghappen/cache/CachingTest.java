package com.nothinghappen.cache;

public class CachingTest {

    public static boolean peek(CacheImpl cache, String key) {
        return cache.data.get(key) != null;
    }

    public static void maintenance(CacheImpl cache) {
        cache.backend.drainUserEvent();
        cache.backend.doScheduleWork();
        cache.backend.drainBuffer();
        cache.backend.evict();
        cache.backend.expireAfterAccess();
    }


}
