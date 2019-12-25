package com.nothinghappen.cache.datastruct;

public abstract class WheelTimerTask {

    private WheelTimerTask next;
    private WheelTimerTask prev;
    private long deadline;

    protected WheelTimerTask() { }

    public abstract void run(long delay);

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public WheelTimerTask getNextTask() {
        return next;
    }

    public void setNextTask(WheelTimerTask next) {
        this.next = next;
    }

    public WheelTimerTask getPrevTask() {
        return prev;
    }

    public void setPrevTask(WheelTimerTask prev) {
        this.prev = prev;
    }


}
