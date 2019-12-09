package com.nothinghappen.cache.datastruct;

import java.util.function.BiConsumer;

public class WheelTimer<T> {

    private int bucket_size = 16;
    private int mask = 15;
    private long ticks_interval;
    private long currentTicks;
    private int currentIdx;

    private WheelTimerConsumer<T, WheelTimer<T>, Long> consumer;

    private Node<T>[] buckets;

    private WheelTimer<T> overflow;
    private WheelTimer<T> root;


    /**
     * bucket_size = 2 ^ power
     * @param power
     * @param biConsumer
     * @param nowTicks
     */
    public WheelTimer(int power, WheelTimerConsumer<T, WheelTimer<T>, Long> biConsumer, long nowTicks) {
        if (power < 0) {
            power = 0;
        }
        if (power > 30) {
            power = 30;
        }
        this.init(1 << power, biConsumer, nowTicks, null, 1);
    }

    public WheelTimer(int bucket_size, WheelTimerConsumer<T, WheelTimer<T>, Long> biConsumer, long nowTicks, WheelTimer<T> root, long interval) {
        this.init(bucket_size, biConsumer, nowTicks, root, interval);
    }

    public Node<T> add(T t, long deadline) {
        Node<T> head = findBucket(deadline);
        Node<T> newNode = new Node<>(t, deadline);
        link(head, newNode);
        return newNode;
    }

    /**
     * reschedule a existed node
     * @param node
     * @param deadline
     */
    public void reschedule(Node<T> node, long deadline) {
        unlinked(node);
        node.deadline = deadline;
        Node<T> head = findBucket(deadline);
        link(head, node);
    }

    public void unSchedule(Node<T> node) {
        unlinked(node);
    }

    public void advance(long nowTicks) {
        if (nowTicks < currentTicks) {
            return;
        }
        long diff = (nowTicks - currentTicks) / ticks_interval;
        int index = this.currentIdx;
        for (int i = 0; i < (diff > bucket_size ? bucket_size : diff); i++) {
            index = advanceTo(index, 1);
            consume(buckets[index], nowTicks);
        }
        this.currentTicks += diff * ticks_interval;
        this.currentIdx = index;
        if (this.overflow != null) {
            this.overflow.advance(nowTicks);
        }
    }

    private void init(int bucket_size, WheelTimerConsumer<T, WheelTimer<T>, Long> consumer, long nowTicks, WheelTimer<T> root, long interval) {
        this.bucket_size = bucket_size;
        this.mask = bucket_size - 1;
        this.buckets = new Node[bucket_size];
        this.consumer = consumer;
        this.currentTicks = nowTicks;
        this.root = root;
        this.ticks_interval = interval;
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = createDummy();
        }
    }

    private Node<T> createDummy() {
        Node<T> node = new Node<>(null, 0);
        node.setNext(node);
        node.setPrev(node);
        return node;
    }

    private void link(Node<T> head, Node<T> node) {
        Node<T> prev = head.getPrev();
        prev.setNext(node);
        node.setPrev(prev);
        node.setNext(head);
        head.setPrev(node);
    }

    private void unlinked(Node<T> node) {
        if (isUnlinked(node)) {
            return;
        }
        Node<T> next = node.getNext();
        Node<T> prev = node.getPrev();
        next.setPrev(prev);
        prev.setNext(next);
        node.setNext(null);
        node.setPrev(null);
    }

    private boolean isUnlinked(Node<T> node) {
        return node.getNext() == null || node.getPrev() == null;
    }

    private Node<T> doFindBucket(long deadline) {
        long n = (deadline - currentTicks) / ticks_interval;
        if (n <= 0) {
            n = 1;
        }
        if (n > bucket_size) {
            return null;
        } else {
            int index = advanceTo(this.currentIdx, (int) n);
            return buckets[index];
        }
    }

    private Node<T> findBucket(long deadline) {
        Node<T> head;
        if ((head = doFindBucket(deadline)) == null) {
            head = overFlow().findBucket(deadline);
        }
        return head;
    }

    private WheelTimer<T> root() {
        return this.root == null ? this : this.root;
    }

    private int advanceTo(int index, int n) {
        return (index + n) & mask;
    }

    private WheelTimer<T> overFlow() {
        if (this.overflow == null) {
            this.overflow = new WheelTimer<>(bucket_size,
                    consumer,
                    currentTicks,
                    root(),
                    ticks_interval * bucket_size);
        }
        return this.overflow;
    }

    private void consume(Node<T> head, long nowTicks) {
        Node<T> current = head.getNext();
        while (current != head) {
            Node<T> next = current.getNext();
            unlinked(current);
            if (current.deadline > nowTicks) {
                // still alive
                link(root().findBucket(current.deadline), current);
            } else {
                consumer.accept(current.value, root(), current.deadline - nowTicks);
            }
            current = next;
        }
    }

    public static class Node<T> implements AccessOrder<Node<T>> {

        private T value;
        private Node<T> next;
        private Node<T> prev;
        private long deadline;

        Node(T t, long deadline) {
            this.value = t;
            this.deadline = deadline;
        }

        public T getValue() {
            return value;
        }
        @Override
        public Node<T> getNext() {
            return next;
        }
        @Override
        public void setNext(Node<T> next) {
            this.next = next;
        }
        @Override
        public Node<T> getPrev() {
            return prev;
        }
        @Override
        public void setPrev(Node<T> prev) {
            this.prev = prev;
        }
    }


}
