package com.nothinghappen.cache;

import java.util.concurrent.TimeUnit;

public class FixedExpiration implements Expiration {

    private long expireAfterRefresh;
    private long expireAfterAccess;
    private TimeUnit refreshTimeUnit;
    private TimeUnit accessTimeUnit;

    public FixedExpiration(long expireAfterRefresh,
            TimeUnit refreshTimeUnit,
            long expireAfterAccess,
            TimeUnit accessTimeUnit) {
        this.expireAfterAccess = expireAfterAccess;
        this.expireAfterRefresh = expireAfterRefresh;
        this.refreshTimeUnit = refreshTimeUnit;
        this.accessTimeUnit = accessTimeUnit;
    }

    @Override
    public long expireAfterRefresh(TimeUnit timeUnit) {
        return timeUnit.convert(expireAfterRefresh, refreshTimeUnit);
    }
    @Override
    public long expireAfterAccess(TimeUnit timeUnit) {
        return timeUnit.convert(expireAfterAccess, accessTimeUnit);
    }
}
