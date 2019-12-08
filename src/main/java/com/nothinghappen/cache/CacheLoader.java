package com.nothinghappen.cache;

import java.util.concurrent.TimeUnit;

public interface CacheLoader {

    Object load(String key, Object oldValue);

}
