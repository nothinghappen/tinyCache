package com.nothinghappen.cache.base;

public interface BasicCache<T> {

    void put(String key, T value);

    T get(String key);

    void remove(String key);

    int size();
}
