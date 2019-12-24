package com.nothinghappen.cache.base;

import mockit.Mock;
import mockit.MockUp;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class MockUtils {

    public static void mockCompletableFuture() {
        new MockUp<CompletableFuture>() {
            @Mock
            public <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier,
                    Executor executor) {
                return CompletableFuture.completedFuture(supplier.get());
            }
        };
    }
}
