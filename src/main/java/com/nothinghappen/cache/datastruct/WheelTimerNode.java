package com.nothinghappen.cache.datastruct;

public class WheelTimerNode<T> {

    private T value;
    private WheelTimerNode<T> next;
    private WheelTimerNode<T> prev;
    private long deadline;

    WheelTimerNode(T t, long deadline) {
        this.value = t;
        this.deadline = deadline;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public WheelTimerNode<T> getNext() {
        return next;
    }

    public void setNext(WheelTimerNode<T> next) {
        this.next = next;
    }

    public WheelTimerNode<T> getPrev() {
        return prev;
    }

    public void setPrev(WheelTimerNode<T> prev) {
        this.prev = prev;
    }
}
