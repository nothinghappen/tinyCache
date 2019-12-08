package com.nothinghappen.cache.base;

import com.nothinghappen.cache.Cache;
import com.nothinghappen.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

public enum CacheType {

    Caffeine {
        @Override
        public <T> BasicCache<T> create(int size) {
            return new BasicCache<T>() {

                com.github.benmanes.caffeine.cache.Cache<String, T> cache =
                        com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                                .maximumSize(size)
                                .expireAfterAccess(1000, TimeUnit.DAYS)
                                .build();

                @Override
                public void put(String key, T value) {
                    cache.put(key, value);
                }
                @Override
                public T get(String key) {
                    return cache.getIfPresent(key);
                }
                @Override
                public void remove(String key) {
                    cache.invalidate(key);
                }
                @Override
                public int size() {
                    return (int) cache.estimatedSize();
                }
            };
        }
    },
    Cache {

        @Override
        public <T> BasicCache<T> create(int maximumSize) {
            return new BasicCache<T>() {

                com.nothinghappen.cache.Cache cache = CacheBuilder.newBuilder().setCapacity(maximumSize).build();

                @Override
                public void put(String key, T value) {
                    cache.add(key, value, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                }
                @Override
                public T get(String key) {
                    return (T) cache.get(key);
                }
                @Override
                public void remove(String key) {
                    cache.remove(key);
                }
                @Override
                public int size() {
                    return cache.size();
                }
            };
        }
    };

    public abstract <T> BasicCache<T> create(int maximumSize);
}
