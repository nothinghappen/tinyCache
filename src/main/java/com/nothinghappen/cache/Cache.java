package com.nothinghappen.cache;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface Cache {

    Object getOrLoad(String key, CacheLoader loader, long expireAfterRefresh, TimeUnit unit);

    Object getOrLoad(String key, CacheLoader loader, long expireAfterRefresh, long expireAfterAccess, TimeUnit unit);

    Object getOrLoad(String key, CacheLoader loader, Expiration expiration);

    Object get(String key);

    Future<Void> removeAsync(String key);

    void remove(String key);

    Future<Void> addAsync(String key, Object value, long expireAfterRefresh, TimeUnit unit);

    Future<Void> addAsync(String key, Object value, long expireAfterRefresh, long expireAfterAccess, TimeUnit unit);

    Future<Void> addAsync(String key, Object value, Expiration expiration);

    void add(String key, Object value, long expireAfterRefresh, TimeUnit unit);

    void add(String key, Object value, long expireAfterRefresh, long expireAfterAccess, TimeUnit unit);

    void add(String key, Object value, Expiration expiration);

    Future<Future<Void>> refreshAsync(String key);

    void refresh(String key);

    int size();

}
