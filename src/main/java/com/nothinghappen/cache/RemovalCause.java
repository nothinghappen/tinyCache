package com.nothinghappen.cache;

public enum RemovalCause {
    USER_OPERATION,
    EVICT,
    EXPIRE_AFTER_WRITE,
    EXPIRE_AFTER_ACCESS,
    ADD,
}
