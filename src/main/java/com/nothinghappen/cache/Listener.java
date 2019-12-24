package com.nothinghappen.cache;

public interface Listener {

    void onRead(String key);

    void onWrite(String key);

    void onRemove(String key, Object value, RemovalCause cause);

    void onRefresh(String key);
}
