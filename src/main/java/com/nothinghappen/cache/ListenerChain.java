package com.nothinghappen.cache;

import java.util.ArrayList;
import java.util.List;

public class ListenerChain implements Listener {

    private List<Listener> listeners = new ArrayList<>();

    public void register(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void onRead(String key) {
        listeners.forEach(listener -> listener.onRead(key));
    }

    @Override
    public void onWrite(String key) {
        listeners.forEach(listener -> listener.onWrite(key));
    }

    @Override
    public void onRemove(String key, Object value, RemovalCause cause) {
        listeners.forEach(listener -> listener.onRemove(key, value, cause));
    }

    @Override
    public void onRefresh(String key) {
        listeners.forEach(listener -> listener.onRefresh(key));
    }
}
