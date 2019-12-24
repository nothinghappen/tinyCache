package com.nothinghappen.cache.datastruct;

public class WheelTimer<T> {

    private int bucket_size = 16;
    private int mask = 15;
    private long ticks_interval;
    private long currentTicks;
    private int currentIdx;

    private WheelTimerConsumer<T> consumer;

    private WheelTimerNode<T>[] buckets;

    private WheelTimer<T> overflow;
    private WheelTimer<T> root;


    /**
     * bucket_size = 2 ^ power
     * @param power
     * @param biConsumer
     * @param nowTicks
     */
    public WheelTimer(int power, WheelTimerConsumer<T> biConsumer, long nowTicks) {
        if (power < 0) {
            power = 0;
        }
        if (power > 30) {
            power = 30;
        }
        this.init(1 << power, biConsumer, nowTicks, null, 1);
    }

    public WheelTimer(int bucket_size, WheelTimerConsumer<T> biConsumer, long nowTicks, WheelTimer<T> root, long interval) {
        this.init(bucket_size, biConsumer, nowTicks, root, interval);
    }

    public WheelTimerNode<T> add(T t, long deadline) {
        WheelTimerNode<T> head = findBucket(deadline);
        WheelTimerNode<T> newWheelTimerNode = new WheelTimerNode<>(t, deadline);
        link(head, newWheelTimerNode);
        return newWheelTimerNode;
    }

    /**
     * reschedule a existed wheelTimerNode
     * @param wheelTimerNode
     * @param deadline
     */
    public void reschedule(WheelTimerNode<T> wheelTimerNode, long deadline) {
        unlinked(wheelTimerNode);
        wheelTimerNode.setDeadline(deadline);
        WheelTimerNode<T> head = findBucket(deadline);
        link(head, wheelTimerNode);
    }

    public void unSchedule(WheelTimerNode<T> wheelTimerNode) {
        unlinked(wheelTimerNode);
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

    private void init(int bucket_size, WheelTimerConsumer<T> consumer, long nowTicks, WheelTimer<T> root, long interval) {
        this.bucket_size = bucket_size;
        this.mask = bucket_size - 1;
        this.buckets = new WheelTimerNode[bucket_size];
        this.consumer = consumer;
        this.currentTicks = nowTicks;
        this.root = root;
        this.ticks_interval = interval;
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = createDummy();
        }
    }

    private WheelTimerNode<T> createDummy() {
        WheelTimerNode<T> wheelTimerNode = new WheelTimerNode<>(null, 0);
        wheelTimerNode.setNext(wheelTimerNode);
        wheelTimerNode.setPrev(wheelTimerNode);
        return wheelTimerNode;
    }

    private void link(WheelTimerNode<T> head, WheelTimerNode<T> wheelTimerNode) {
        WheelTimerNode<T> prev = head.getPrev();
        prev.setNext(wheelTimerNode);
        wheelTimerNode.setPrev(prev);
        wheelTimerNode.setNext(head);
        head.setPrev(wheelTimerNode);
    }

    private void unlinked(WheelTimerNode<T> wheelTimerNode) {
        if (isUnlinked(wheelTimerNode)) {
            return;
        }
        WheelTimerNode<T> next = wheelTimerNode.getNext();
        WheelTimerNode<T> prev = wheelTimerNode.getPrev();
        next.setPrev(prev);
        prev.setNext(next);
        wheelTimerNode.setNext(null);
        wheelTimerNode.setPrev(null);
    }

    private boolean isUnlinked(WheelTimerNode<T> wheelTimerNode) {
        return wheelTimerNode.getNext() == null || wheelTimerNode.getPrev() == null;
    }

    private WheelTimerNode<T> doFindBucket(long deadline) {
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

    private WheelTimerNode<T> findBucket(long deadline) {
        WheelTimerNode<T> head;
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

    private void consume(WheelTimerNode<T> head, long nowTicks) {
        WheelTimerNode<T> current = head.getNext();
        while (current != head) {
            WheelTimerNode<T> next = current.getNext();
            unlinked(current);
            if (current.getDeadline() > nowTicks) {
                // still alive
                link(root().findBucket(current.getDeadline()), current);
            } else {
                consumer.accept(current.getValue(), root(), current.getDeadline() - nowTicks);
            }
            current = next;
        }
    }
}
