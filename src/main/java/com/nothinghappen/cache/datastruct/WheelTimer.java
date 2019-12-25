package com.nothinghappen.cache.datastruct;

public class WheelTimer {

    private int bucket_size = 16;
    private int mask = 15;
    private long ticks_interval;
    private long currentTicks;
    private int currentIdx;

    private WheelTimerTask[] buckets;

    private WheelTimer overflow;
    private WheelTimer root;

    /**
     * bucket_size = 2 ^ power
     */
    public WheelTimer(int power, long nowTicks) {
        if (power < 0) {
            power = 0;
        }
        if (power > 30) {
            power = 30;
        }
        this.init(1 << power, nowTicks, null, 1);
    }

    private WheelTimer(int bucket_size, long nowTicks, WheelTimer root, long interval) {
        this.init(bucket_size, nowTicks, root, interval);
    }


    public void schedule(WheelTimerTask t, long deadline) {
        unschedule(t);
        WheelTimerTask head = findBucket(deadline);
        t.setDeadline(deadline);
        link(head, t);
    }

    public void unschedule(WheelTimerTask timerNode) {
        unlinked(timerNode);
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

    private void init(int bucket_size, long nowTicks, WheelTimer root, long interval) {
        this.bucket_size = bucket_size;
        this.mask = bucket_size - 1;
        this.buckets = new WheelTimerTask[bucket_size];
        this.currentTicks = nowTicks;
        this.root = root;
        this.ticks_interval = interval;
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = createDummy();
        }
    }

    private WheelTimerTask createDummy() {
        WheelTimerTask timerNode = new DummyWheelTimerTask();
        timerNode.setNextTask(timerNode);
        timerNode.setPrevTask(timerNode);
        return timerNode;
    }

    private void link(WheelTimerTask head, WheelTimerTask timerNode) {
        WheelTimerTask prev = head.getPrevTask();
        prev.setNextTask(timerNode);
        timerNode.setPrevTask(prev);
        timerNode.setNextTask(head);
        head.setPrevTask(timerNode);
    }

    private void unlinked(WheelTimerTask timerNode) {
        if (isUnlinked(timerNode)) {
            return;
        }
        WheelTimerTask next = timerNode.getNextTask();
        WheelTimerTask prev = timerNode.getPrevTask();
        next.setPrevTask(prev);
        prev.setNextTask(next);
        timerNode.setNextTask(null);
        timerNode.setPrevTask(null);
    }

    private boolean isUnlinked(WheelTimerTask timerNode) {
        return timerNode.getNextTask() == null || timerNode.getPrevTask() == null;
    }

    private WheelTimerTask doFindBucket(long deadline) {
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

    private WheelTimerTask findBucket(long deadline) {
        WheelTimerTask head;
        if ((head = doFindBucket(deadline)) == null) {
            head = overFlow().findBucket(deadline);
        }
        return head;
    }

    private WheelTimer root() {
        return this.root == null ? this : this.root;
    }

    private int advanceTo(int index, int n) {
        return (index + n) & mask;
    }

    private WheelTimer overFlow() {
        if (this.overflow == null) {
            this.overflow = new WheelTimer(bucket_size,
                    currentTicks,
                    root(),
                    ticks_interval * bucket_size);
        }
        return this.overflow;
    }

    private void consume(WheelTimerTask head, long nowTicks) {
        WheelTimerTask current = head.getNextTask();
        while (current != head) {
            WheelTimerTask next = current.getNextTask();
            unlinked(current);
            if (current.getDeadline() > nowTicks) {
                // still alive
                link(root().findBucket(current.getDeadline()), current);
            } else {
                current.run(current.getDeadline() - nowTicks);
            }
            current = next;
        }
    }

    private static class DummyWheelTimerTask extends WheelTimerTask {
        @Override
        public void run(long deltaTicket) {
        }
    }
}
