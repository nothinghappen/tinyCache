package com.nothinghappen.cache.datastruct;

public abstract class AccessList<T extends AccessOrder<T>> {

    public abstract void access(T t);

    public abstract void add(T t);

    public abstract void remove(T t);

    public abstract T head();

    protected boolean isUnLinked(T node) {
        return node.getNext() == null || node.getPrev() == null;
    }

    protected void unlink(T node) {
        T prev = node.getPrev();
        T next = node.getNext();
        prev.setNext(next);
        next.setPrev(prev);
    }

    protected void link(T prev, T node) {
        T prevNext = prev.getNext();
        node.setNext(prevNext);
        prev.setNext(node);
        node.setPrev(prev);
        prevNext.setPrev(node);
    }

    protected void linkHead(T node) {
        link(head(), node);
    }

}
