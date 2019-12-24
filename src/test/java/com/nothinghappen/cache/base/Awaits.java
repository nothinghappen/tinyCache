package com.nothinghappen.cache.base;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

import java.util.concurrent.TimeUnit;

public class Awaits {

    public static ConditionFactory await() {
        return Awaitility.with()
                .pollDelay(1, TimeUnit.MILLISECONDS)
                .and()
                .pollInterval(1, TimeUnit.MILLISECONDS)
                .timeout(3, TimeUnit.SECONDS);
    }
}
