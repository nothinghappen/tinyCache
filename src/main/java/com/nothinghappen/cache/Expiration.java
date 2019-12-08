package com.nothinghappen.cache;

import java.util.concurrent.TimeUnit;

public interface Expiration {

    /**
     *
     * @param timeUnit
     * @return
     */
    long expireAfterRefresh(TimeUnit timeUnit);

    /**
     *
     * @param timeUnit
     * @return
     */
    long expireAfterAccess(TimeUnit timeUnit);
}
