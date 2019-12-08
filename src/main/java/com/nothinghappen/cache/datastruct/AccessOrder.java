package com.nothinghappen.cache.datastruct;

public interface AccessOrder<T extends AccessOrder<T>> {

    T getNext();

    void setNext(T accessOrder);

    T getPrev();

    void setPrev(T accessOrder);

    // long getAccessTime();
    //
    // void setAccessTime(long accessTime);
}
