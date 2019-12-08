package com.nothinghappen.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface Cache {

    Object getOrLoad(String key, CacheLoader loader, long expireAfterRefresh, TimeUnit unit);

    Object getOrLoad(String key, CacheLoader loader, long expireAfterRefresh, long expireAfterAccess, TimeUnit unit);

    Object getOrLoad(String key, CacheLoader loader, Expiration expiration);

    Object get(String key);

    CompletableFuture removeAsync(String key);

    void remove(String key);

    CompletableFuture addAsync(String key, Object value, long expireAfterRefresh, TimeUnit unit);

    CompletableFuture addAsync(String key, Object value, long expireAfterRefresh, long expireAfterAccess, TimeUnit unit);

    CompletableFuture addAsync(String key, Object value, Expiration expiration);

    void add(String key, Object value, long expireAfterRefresh, TimeUnit unit);

    void add(String key, Object value, long expireAfterRefresh, long expireAfterAccess, TimeUnit unit);

    void add(String key, Object value, Expiration expiration);

    CompletableFuture refreshAsync(String key);

    void refresh(String key);

    int size();

}
