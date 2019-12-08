package com.nothinghappen.cache;

import com.nothinghappen.cache.annotations.RunIn;
import com.nothinghappen.cache.datastruct.AccessList;
import com.nothinghappen.cache.datastruct.AccessOrder;
import com.nothinghappen.cache.datastruct.Buffer;
import com.nothinghappen.cache.datastruct.RingBuffer;
import com.nothinghappen.cache.datastruct.UnboundedBuffer;
import com.nothinghappen.cache.datastruct.WheelTimer;
import com.nothinghappen.cache.datastruct.WheelTimerConsumer;

import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class CacheImpl implements Cache {

    Ticker ticker = Tickers.NANO;

    Ticker timeWheelTicker;

    final long MIN_DELAYED_NANO = NANOSECONDS.convert(5, TimeUnit.MILLISECONDS);

    final long READ_EVENT_INTERNAL = ticker.convert(1000, TimeUnit.MILLISECONDS);

    final Buffer<Runnable> userEventBuffer = new UnboundedBuffer<>();

    final PriorityQueue<ScheduledTask> scheduleQueue = new PriorityQueue<>();

    final AccessList<CacheItem> accessList = new AccessLinkedList();

    final Buffer<CacheItem> readBuffer = new RingBuffer<>(9); // capacity = 512

    final BackendThread backend;

    final ExecutorService refreshBackend;

    final boolean isEvict;

    final int capacity;

    final WheelTimer<CacheItem> timeWheel;
    
    ConcurrentMap<String, RunnableFuture<CacheItem>> data = new ConcurrentHashMap<>();

    private WheelTimerConsumer<CacheItem, WheelTimer<CacheItem>, Long> expireAfterAccessConsumer = (ci, timeWheel, delta) -> {
        long currentExpireAfterAccessNanos = ci.expiration.expireAfterAccess(NANOSECONDS);
        long currentExpireAfterAccessTicks = timeWheelTicker.convert(currentExpireAfterAccessNanos, NANOSECONDS);
        long expireAfterAccessTicks = timeWheelTicker.convert(ci.expireAfterAccess, NANOSECONDS);
        long nowTicks = timeWheelTicker.read();
        long rescheduleTicks = currentExpireAfterAccessTicks - expireAfterAccessTicks + delta;
        if (rescheduleTicks > 0) {
            ci.timeWheelNode = timeWheel.add(ci, nowTicks + rescheduleTicks);
            ci.expireAfterAccess = currentExpireAfterAccessNanos;
        } else {
            doRemove(ci.key);
        }
    };

    CacheImpl(int capacity, ExecutorService refreshBackend, AdvancedOption advancedOption) {
        if (capacity > 0) {
            this.capacity = capacity;
            this.isEvict = true;
        } else {
            this.capacity = 0;
            isEvict = false;
        }
        this.refreshBackend = refreshBackend;
        this.timeWheelTicker = advancedOption.getTimeWheelTicker();
        this.timeWheel = new WheelTimer<>(advancedOption.getTimeWheelPower(), expireAfterAccessConsumer, timeWheelTicker.read());
        this.backend = new BackendThread();
    }

    @Override
    public Object getOrLoad(String key, CacheLoader loader, long expireAfterRefresh, TimeUnit unit) {
        return getOrLoad(key, loader, expireAfterRefresh, 0, unit);
    }

    @Override
    public Object getOrLoad(String key,
            CacheLoader loader,
            long expireAfterRefresh,
            long expireAfterAccess,
            TimeUnit unit) {
        return getOrLoad(key, loader, new Expiration() {
            @Override
            public long expireAfterRefresh(TimeUnit timeUnit) {
                return timeUnit.convert(expireAfterRefresh, unit);
            }
            @Override
            public long expireAfterAccess(TimeUnit timeUnit) {
                return timeUnit.convert(expireAfterAccess, unit);
            }
        });
    }

    @Override
    public Object getOrLoad(String key, CacheLoader loader, Expiration expiration) {
        requireNonNull(key, loader, expiration);
        try {
            // fast path
            RunnableFuture<CacheItem> rf = data.get(key);
            if (rf == null) {
                // slow path
                rf = CompletableFuture.supplyAsync(() -> doGetOrLoad(key, loader, expiration), backend).get();
                rf.run();
            }
            CacheItem ci = rf.get();
            afterRead(ci);
            return ci.value;
        } catch (InterruptedException | ExecutionException e) {
        }
        return null;
    }

    @Override
    public Object get(String key) {
        try {
            RunnableFuture<CacheItem> rf = data.get(key);
            if (rf != null) {
                CacheItem ci = rf.get();
                afterRead(ci);
                return ci.value;
            }
        } catch (InterruptedException | ExecutionException e) {
        }
        return null;
    }

    @Override
    public CompletableFuture removeAsync(String key) {
        requireNonNull(key);
        return CompletableFuture.runAsync(() -> doRemove(key), backend);
    }

    @Override
    public void remove(String key) {
        try {
            removeAsync(key).get();
        } catch (InterruptedException | ExecutionException e) {
        }
    }

    @Override
    public CompletableFuture addAsync(String key, Object value, long expireAfterRefresh, TimeUnit unit) {
        return addAsync(key, value, expireAfterRefresh, 0, unit);
    }

    @Override
    public CompletableFuture addAsync(String key,
            Object value,
            long expireAfterRefresh,
            long expireAfterAccess,
            TimeUnit unit) {
        return addAsync(key, value, new Expiration() {
            @Override
            public long expireAfterRefresh(TimeUnit timeUnit) {
                return timeUnit.convert(expireAfterRefresh, unit);
            }
            @Override
            public long expireAfterAccess(TimeUnit timeUnit) {
                return timeUnit.convert(expireAfterAccess, unit);
            }
        });
    }

    @Override
    public CompletableFuture addAsync(String key, Object value, Expiration expiration) {
        requireNonNull(key, value, expiration);
        return CompletableFuture.runAsync(
                () -> doAdd(key, value, expiration), backend);
    }

    @Override
    public void add(String key, Object value, long expireAfterRefresh, TimeUnit unit) {
        add(key, value, expireAfterRefresh, 0, unit);
    }

    @Override
    public void add(String key, Object value, long expireAfterRefresh, long expireAfterAccess, TimeUnit unit) {
        try {
            addAsync(key, value, expireAfterRefresh, expireAfterAccess, unit).get();
        } catch (InterruptedException | ExecutionException e) {
        }
    }

    @Override
    public void add(String key, Object value, Expiration expiration) {
        try {
            addAsync(key, value, expiration).get();
        } catch (InterruptedException | ExecutionException e) {
        }
    }

    @Override
    public CompletableFuture<Future> refreshAsync(String key) {
        requireNonNull(key);
        return CompletableFuture.supplyAsync(() -> doRefresh(key), backend);
    }

    @Override
    public void refresh(String key) {
        try {
            Future f = refreshAsync(key).get();
            if (f == null) {
                // refresh failed , or had been removed (since no loader)
                return;
            }
            f.get();
        } catch (InterruptedException | ExecutionException e) {
        }
    }

    @Override
    public int size() {
        return data.size();
    }

    @RunIn("backend")
    private RunnableFuture<CacheItem> doGetOrLoad(String key, CacheLoader loader, Expiration expiration) {
        RunnableFuture<CacheItem> rf = data.get(key);
        if (rf != null) {
            return rf;
        }
        CacheItem ci = new CacheItem(key, loader, expiration);
        afterWrite(ci);
        rf = new FutureTask<>(() -> {
            ci.load();
            return ci;
        });
        data.put(key, rf);
        return rf;
    }

    @RunIn("backend")
    private void doAdd(String key, Object value, Expiration expiration) {
        RunnableFuture<CacheItem> exsiting = data.get(key);
        if (exsiting != null) {
            doRemove(key);
        }
        RunnableFuture<CacheItem> rf = data.computeIfAbsent(key, k -> new FutureTask<>(() -> {
            CacheItem ci = new CacheItem(key, null, expiration);
            ci.value = value;
            afterWrite(ci);
            return ci;
        }));
        rf.run();
        data.put(key, rf);
    }

    @RunIn("backend")
    private void doRemove(String key) {
        try {
            RunnableFuture<CacheItem> rf = data.get(key);
            if (rf == null) {
                return;
            }
            CacheItem ci = rf.get();
            data.remove(key);
            afterRemove(ci);
        } catch (InterruptedException | ExecutionException e) {
        }
    }

    @RunIn("backend")
    private Future doRefresh(String key) {
        try {
            RunnableFuture<CacheItem> rf = data.get(key);
            if (rf == null) {
                return null;
            }
            CacheItem ci = rf.get();
            return ci.refreshAsync();
        } catch (InterruptedException | ExecutionException e) {
        }
        return null;
    }

    @RunIn("backend")
    private void schedule(CacheItem ci) {
        long nano = ci.expiration.expireAfterRefresh(TimeUnit.NANOSECONDS);
        ci.scheduled = new ScheduledTask(ci, true, nano, TimeUnit.NANOSECONDS, this.ticker);
        scheduleQueue.add(ci.scheduled);
    }

    @RunIn("backend")
    private void unSchedule(CacheItem ci) {
        if (ci.scheduled != null) {
            scheduleQueue.remove(ci.scheduled);
            ci.scheduled = null;
        }
    }

    @RunIn("backend")
    private void afterWrite(CacheItem ci) {
        schedule(ci);
        if (isEvict()) {
            accessList.add(ci);
        }
        if (isExpireAfterAccess(ci)) {
            ci.timeWheelNode = timeWheel.add(
                    ci, timeWheelTicker.read() + timeWheelTicker.convert(ci.expireAfterAccess, NANOSECONDS));
        }
    }

    @RunIn("backend")
    private void afterRemove(CacheItem ci) {
        unSchedule(ci);
        if (isEvict()) {
            accessList.remove(ci);
        }
        if (isExpireAfterAccess(ci)) {
            timeWheel.unSchedule(ci.timeWheelNode);
        }
    }

    private void afterRead(CacheItem ci) {
        if (isEvict() || isExpireAfterAccess(ci)) {
            long now = ticker.read();
            long lastAccessTime = ci.lastAccessTime.get();
            if (now - lastAccessTime > READ_EVENT_INTERNAL
                && ci.lastAccessTime.compareAndSet(lastAccessTime, now) // avoiding too much read event
                && readBuffer.offer(ci)) {
                backend.unpark();
            }
        }
    }

    private boolean isExpireAfterAccess(CacheItem ci) {
        return ci.expireAfterAccess > 0;
    }

    private void requireNonNull(Object... objects) {
        for (Object object : objects) {
            Objects.requireNonNull(object);
        }
    }

    private boolean isEvict() {
        return this.isEvict;
    }

    private class CacheItem implements Runnable, AccessOrder<CacheItem> {

        private final String key;
        private final CacheLoader loader;
        private final Expiration expiration;
        private ScheduledTask scheduled;
        private WheelTimer.Node<CacheItem> timeWheelNode;
        private Object value;
        private AtomicLong lastAccessTime = new AtomicLong();
        private long expireAfterAccess;

        private CacheItem next;
        private CacheItem prev;

        CacheItem(String key, CacheLoader loader, Expiration expiration) {
            this.loader = loader;
            this.expiration = expiration;
            this.key = key;
            if (expiration != null) {
                this.expireAfterAccess = expiration.expireAfterAccess(NANOSECONDS);
            }
        }

        public Object getValue() {
            return value;
        }

        @RunIn({"customer", "refreshBackend"})
        public void load() {
            this.value = loader.load(key, value);
        }


        public Future refreshAsync() {
            if (loader == null) {
                doRemove(this.key);
                return null;
            }
            // load
            Future f = null;
            try {
                f = refreshBackend.submit(this::load);
            } catch (RejectedExecutionException ex) {
                // TODO: 2019/7/21 log refresh fail
            }
            if (scheduled != null) {
                scheduled.update(this.expiration.expireAfterRefresh(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
            }
            this.expireAfterAccess = this.expiration.expireAfterAccess(NANOSECONDS);
            return f;
        }

        @Override
        @RunIn("backend")
        public void run() {
            refreshAsync();
        }

        @Override
        public CacheItem getNext() {
            return this.next;
        }
        @Override
        public void setNext(CacheItem next) {
            this.next = next;
        }
        @Override
        public CacheItem getPrev() {
            return this.prev;
        }
        @Override
        public void setPrev(CacheItem prev) {
            this.prev = prev;
        }
    }


    private class AccessLinkedList extends AccessList<CacheItem> {

        private CacheItem head = new CacheItem(null, null, null);

        public AccessLinkedList() {
            head.setNext(head);
            head.setPrev(head);
        }

        @Override
        public void access(CacheItem node) {
            if (isUnLinked(node)) {
                // has been removed
                return;
            }
            unlink(node);
            linkHead(node);
        }

        @Override
        public void add(CacheItem node) {
            linkHead(node);
        }

        @Override
        public void remove(CacheItem node) {
            unlink(node);
            node.setNext(null);
            node.setPrev(null);
        }

        @Override
        public CacheItem head() {
            return head;
        }

    }


    class BackendThread implements Executor, Runnable {

        boolean running = true;

        /**
         * backend thread
         */
        final Thread thread;

        BackendThread() {
            this.thread = new Thread(this);
            this.thread.setDaemon(true);
            this.thread.start();
        }

        @Override
        @RunIn("backend")
        public void run() {
            while (running) {
                maintenance();
            }
        }

        @Override
        public void execute(Runnable command) {
            userEventBuffer.offer(command);
            unpark();
        }

        @RunIn("backend")
        void park(long nanos) {
            LockSupport.parkNanos(nanos);
        }

        @RunIn("backend")
        void park() {
            LockSupport.park();
        }

        public void unpark() {
            LockSupport.unpark(this.thread);
        }

        @RunIn("backend")
        void maintenance() {
            drainUserEvent();
            doScheduleTask();
            drainBuffer();
            evict();
            expireAfterAccess();
            peekTaskAndWait();
        }

        @RunIn("backend")
        void drainUserEvent() {
            Runnable work;
            while ((work = userEventBuffer.poll()) != null) {
                work.run();
            }
        }

        void doScheduleTask() {
            ScheduledTask task;
            if ((task = getTask()) != null) {
                task.run();
                rescheduleWork(task);
            }
        }

        @RunIn("backend")
        void drainBuffer() {
            int count = 0;
            CacheItem ci = null;
            while ((ci = readBuffer.poll()) != null && count < readBuffer.capacity()) {
                consumeBuffer(ci);
                count++;
            }
        }

        void consumeBuffer(CacheItem ci) {
            if (isEvict()) {
                accessList.access(ci);
            }
            if (isExpireAfterAccess(ci)) {
                timeWheel.reschedule(ci.timeWheelNode, timeWheelTicker.read() + timeWheelTicker.convert(ci.expireAfterAccess, NANOSECONDS));
            }
        }

        @RunIn("backend")
        void evict() {
            int size = size();
            if (isEvict() && size > capacity) {
                CacheItem victim = accessList.head().getPrev();
                while (size > capacity && victim != null) {
                    CacheItem nextVictim = victim.getPrev();
                    doRemove(victim.key);
                    victim = nextVictim;
                    size--;
                }
            }
        }

        @RunIn("backend")
        void expireAfterAccess() {
            timeWheel.advance(timeWheelTicker.read());
        }

        /**
         * reschedule if needed
         */
        void rescheduleWork(ScheduledTask work) {
            // reschedule
            work.reset();
            scheduleQueue.add(work);
        }

        ScheduledTask getTask() {
            ScheduledTask st = scheduleQueue.peek();
            if (st != null && st.getDelay(NANOSECONDS) <= MIN_DELAYED_NANO) {
                return scheduleQueue.poll();
            }
            return null;
        }

        void peekTaskAndWait() {
            long delayNanos = 0;
            ScheduledTask st = scheduleQueue.peek();
            if (st == null) {
                park();
            } else if ((delayNanos = st.getDelay(NANOSECONDS)) > MIN_DELAYED_NANO) {
                park(delayNanos);
            }
        }
    }


    static class ScheduledTask implements Delayed, Runnable {

        private long deadline;
        private long delayNanos;
        private final Runnable command;
        private final boolean scheduled;
        private final Ticker ticker;

        public ScheduledTask(Runnable command, boolean scheduled, long duration, TimeUnit timeUnit, Ticker ticker) {
            this.ticker = ticker;
            this.delayNanos = duration < 0 ? 0 : timeUnit.toNanos(duration);
            this.deadline = overflowFree(this.delayNanos, now());
            this.command = command;
            this.scheduled = scheduled;
        }

        private boolean isScheduled() {
            return this.scheduled;
        }

        public void reset() {
            this.deadline = overflowFree(this.delayNanos, now());
        }

        public void immediate() {
            this.deadline = now();
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(deadline - now(), TimeUnit.NANOSECONDS);
        }

        @Override
        public void run() {
            command.run();
        }

        public void update(long duration, TimeUnit timeUnit) {
            this.delayNanos = timeUnit.toNanos(duration);
        }

        @Override
        public int compareTo(Delayed other) {
            if (this == other) {
                return 0;
            }
            long diff = getDelay(NANOSECONDS) - other.getDelay(NANOSECONDS);
            return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
        }

        private long now() {
            return ticker.read(NANOSECONDS);
        }

        /**
         * Long.MAX_VALUE if it would positively overflow.
         */
        private long overflowFree(long delayNanos, long now) {
            if (Long.MAX_VALUE - delayNanos < now) {
                return Long.MAX_VALUE;
            }
            return delayNanos + now;
        }
    }
}
