package com.nothinghappen.cache;

import com.nothinghappen.cache.annotations.RunIn;
import com.nothinghappen.cache.datastruct.AccessList;
import com.nothinghappen.cache.datastruct.AccessOrder;
import com.nothinghappen.cache.datastruct.Buffer;
import com.nothinghappen.cache.datastruct.RingBuffer;
import com.nothinghappen.cache.datastruct.UnboundedBuffer;
import com.nothinghappen.cache.datastruct.WheelTimer;
import com.nothinghappen.cache.datastruct.WheelTimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

class CacheImpl implements Cache {

    private final Logger LOGGER = LoggerFactory.getLogger(CacheImpl.class);

    final long MIN_DELAYED_NANO = NANOSECONDS.convert(5, TimeUnit.MILLISECONDS);

    final Ticker ticker;

    final Ticker timeWheelTicker;

    final long READ_EVENT_INTERVAL;

    final Buffer<Runnable> userEventBuffer = new UnboundedBuffer<>();

    final PriorityQueue<CacheItem> scheduleQueue = new PriorityQueue<>();

    final AccessList<CacheItem> accessList = new AccessLinkedList();

    final Buffer<CacheItem> readBuffer = new RingBuffer<>(9); // capacity = 512

    final BackendThread backend;

    final Executor refreshBackend;

    final boolean isEvict;

    final int capacity;

    final WheelTimer timeWheel;

    final AtomicInteger size = new AtomicInteger();

    final ConcurrentMap<String, CacheItem> data = new ConcurrentHashMap<>();

    final Listener listenerChain;

    CacheImpl(int capacity, Executor refreshBackend, AdvancedOption advancedOption, ListenerChain chain) {
        if (capacity > 0) {
            this.capacity = capacity;
            this.isEvict = true;
        } else {
            this.capacity = 0;
            isEvict = false;
        }
        this.refreshBackend = refreshBackend;
        this.timeWheelTicker = advancedOption.getTimeWheelTicker();
        this.timeWheel = new WheelTimer(advancedOption.getTimeWheelPower(), timeWheelTicker.read());
        this.ticker = advancedOption.getCacheTicker();
        this.READ_EVENT_INTERVAL = ticker.convert(1000, TimeUnit.MILLISECONDS);
        this.backend = new BackendThread();
        this.listenerChain = chain;
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
        return getOrLoad(key, loader, new FixedExpiration(expireAfterRefresh, unit, expireAfterAccess, unit));
    }

    @Override
    public Object getOrLoad(String key, CacheLoader loader, Expiration expiration) {
        requireNonNull(key, loader, expiration);
        try {
            // fast path
            CacheItem ci = data.get(key);
            if (ci == null) {
                // slow path
                ci = CompletableFuture.supplyAsync(() -> doGetOrLoad(key, loader, expiration), backend).get();
            }
            ci.load();
            afterRead(ci);
            return ci.value;
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("getOrLoad", e);
        }
        return null;
    }

    @Override
    public Object get(String key) {
        CacheItem ci = data.get(key);
        if (ci != null) {
            afterRead(ci);
            return ci.value;
        }
        return null;
    }

    @Override
    public Future<Void> removeAsync(String key) {
        requireNonNull(key);
        return CompletableFuture.runAsync(() -> doRemove(key, RemovalCause.USER_OPERATION), backend);
    }

    @Override
    public void remove(String key) {
        try {
            removeAsync(key).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("remove", e);
        }
    }

    @Override
    public Future<Void> addAsync(String key, Object value, long expireAfterRefresh, TimeUnit unit) {
        return addAsync(key, value, expireAfterRefresh, 0, unit);
    }

    @Override
    public Future<Void> addAsync(String key,
            Object value,
            long expireAfterRefresh,
            long expireAfterAccess,
            TimeUnit unit) {
        return addAsync(key, value, new FixedExpiration(expireAfterRefresh, unit, expireAfterAccess, unit));
    }

    @Override
    public Future<Void> addAsync(String key, Object value, Expiration expiration) {
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
            LOGGER.error("add", e);
        }
    }

    @Override
    public void add(String key, Object value, Expiration expiration) {
        try {
            addAsync(key, value, expiration).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("add", e);
        }
    }

    @Override
    public Future<Future<Void>> refreshAsync(String key) {
        requireNonNull(key);
        return CompletableFuture.supplyAsync(() -> doRefresh(key), backend);
    }

    @Override
    public void refresh(String key) {
        try {
            refreshAsync(key).get().get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("refresh", e);
        }
    }

    @Override
    public int size() {
        return size.intValue();
    }

    void startBackendThread() {
        this.backend.start();
    }

    void stopBackendThread() {
        this.backend.stop();
    }

    @RunIn("backend")
    private CacheItem doGetOrLoad(String key, CacheLoader loader, Expiration expiration) {
        CacheItem ci = data.get(key);
        if (ci != null) {
            return ci;
        }
        ci = new CacheItem(key, loader, expiration, this.ticker);
        afterWrite(ci);
        data.put(key, ci);
        return ci;
    }

    @RunIn("backend")
    private void doAdd(String key, Object value, Expiration expiration) {
        CacheItem existing = data.get(key);
        if (existing != null) {
            doRemove(key, RemovalCause.ADD);
        }
        CacheItem ci = new CacheItem(key, null, expiration, this.ticker);
        ci.value = value;
        afterWrite(ci);
        data.put(key, ci);
    }

    @RunIn("backend")
    private void doRemove(String key, RemovalCause cause) {
        CacheItem ci = data.get(key);
        if (ci == null) {
            return;
        }
        data.remove(key);
        afterRemove(ci, cause);
    }

    @RunIn("backend")
    private Future<Void> doRefresh(String key) {
        CacheItem ci = data.get(key);
        if (ci == null) {
            return CompletableFuture.completedFuture(null);
        }
        return doRefresh(ci);
    }

    @RunIn("backend")
    private Future<Void> doRefresh(CacheItem ci) {
        Future<Void> f = ci.refreshAsync();
        afterRefresh(ci);
        return f;
    }

    @RunIn("backend")
    private void scheduleInTimeWheel(CacheItem ci) {
        timeWheel.schedule(ci, timeWheelTicker.read() + timeWheelTicker.convert(ci.expireAfterAccess, NANOSECONDS));
    }

    @RunIn("backend")
    private void unScheduleInTimeWheel(CacheItem ci) {
        timeWheel.unschedule(ci);
    }

    @RunIn("backend")
    private void schedule(CacheItem ci) {
        scheduleQueue.add(ci);
    }

    @RunIn("backend")
    private void unSchedule(CacheItem ci) {
        scheduleQueue.remove(ci);
    }

    @RunIn("backend")
    private void afterWrite(CacheItem ci) {
        schedule(ci);
        if (isEvict()) {
            accessList.add(ci);
        }
        if (isExpireAfterAccess(ci)) {
            scheduleInTimeWheel(ci);
        }
        size.incrementAndGet();
        listenerChain.onWrite(ci.key);
    }

    @RunIn("backend")
    private void afterRemove(CacheItem ci, RemovalCause cause) {
        unSchedule(ci);
        if (isEvict()) {
            accessList.remove(ci);
        }
        if (isExpireAfterAccess(ci)) {
            unScheduleInTimeWheel(ci);
        }
        size.decrementAndGet();
        listenerChain.onRemove(ci.key, ci.value, cause);
    }

    private void afterRead(CacheItem ci) {
        if (isEvict() || isExpireAfterAccess(ci)) {
            long now = ticker.read();
            long lastAccessTime = ci.lastAccessTime.get();
            if (now - lastAccessTime > READ_EVENT_INTERVAL
                    && ci.lastAccessTime.compareAndSet(lastAccessTime, now) // avoiding too much read event
                    && readBuffer.offer(ci)) {
                backend.unpark();
            }
        }
        listenerChain.onRead(ci.key);
    }

    private void afterRefresh(CacheItem ci) {
        ci.updateDelay();
        long currentExpireAfterAccess = ci.expiration.expireAfterAccess(NANOSECONDS);
        if (ci.expireAfterAccess != currentExpireAfterAccess) {
            ci.expireAfterAccess = currentExpireAfterAccess;
            if (currentExpireAfterAccess <= 0) {
                unScheduleInTimeWheel(ci);
            } else {
                scheduleInTimeWheel(ci);
            }
        }
        listenerChain.onRefresh(ci.key);
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

    class CacheItem extends WheelTimerTask implements AccessOrder<CacheItem>, Delayed {

        private ReentrantLock lock = new ReentrantLock();

        private final String key;
        private final CacheLoader loader;
        private final Expiration expiration;

        private Scheduled scheduled;

        private volatile Object value;

        private AtomicLong lastAccessTime = new AtomicLong();
        private long expireAfterAccess;

        private CacheItem next;
        private CacheItem prev;

        CacheItem(String key, CacheLoader loader, Expiration expiration, Ticker ticker) {
            this.loader = loader;
            this.expiration = expiration;
            this.key = key;
            if (expiration != null) {
                this.scheduled = new Scheduled(expiration.expireAfterRefresh(NANOSECONDS), ticker);
                this.expireAfterAccess = expiration.expireAfterAccess(NANOSECONDS);
            }
        }

        public Object getValue() {
            return value;
        }

        @RunIn({"customer", "refreshBackend"})
        private void doRefresh() {
            try {
                if (this.loader == null) {
                    return;
                }
                Object newValue = loader.load(key, value);
                if (newValue == null) {
                    return;
                }
                this.value = newValue;
            } catch (Exception ex) {
                LOGGER.error("doRefresh", ex);
            }
        }

        private void load() {
            if (this.value == null) {
                try {
                    lock.lock();
                    if (this.value == null) {
                        doRefresh();
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

        private Future<Void> refreshAsync() {
            Future<Void> f = CompletableFuture.completedFuture(null);
            if (loader == null) {
                return f;
            }
            // doRefresh
            try {
                f = CompletableFuture.runAsync(this::doRefresh, refreshBackend);
            } catch (RejectedExecutionException ex) {
                LOGGER.error("refreshAsync rejected", ex);
            }
            return f;
        }

        /*-------------------------access order-----------------------------------------*/

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

        /*-------------------------scheduled-----------------------------------------*/

        @Override
        public long getDelay(TimeUnit unit) {
            return this.scheduled.getDelay(unit);
        }

        @Override
        public int compareTo(Delayed o) {
            return this.scheduled.compareTo(o);
        }

        public void resetDelay() {
            this.scheduled.reset();
        }

        public void updateDelay() {
            this.scheduled.update(expiration.expireAfterRefresh(NANOSECONDS));
        }

        /*-------------------------WheelTimerTask-----------------------------------------*/

        @Override
        @RunIn("backend")
        public void run(long delay) {
            long currentExpireAfterAccessNanos = this.expiration.expireAfterAccess(NANOSECONDS);
            if (currentExpireAfterAccessNanos != this.expireAfterAccess) {

                if (currentExpireAfterAccessNanos <= 0) {
                    this.expireAfterAccess = currentExpireAfterAccessNanos;
                    return;
                }
                // expireAfterAccess has been changed
                long currentExpireAfterAccessTicks =
                        timeWheelTicker.convert(currentExpireAfterAccessNanos, NANOSECONDS);
                long expireAfterAccessTicks = timeWheelTicker.convert(this.expireAfterAccess, NANOSECONDS);
                long nowTicks = timeWheelTicker.read();
                long rescheduleTicks = currentExpireAfterAccessTicks - expireAfterAccessTicks + delay;
                if (rescheduleTicks > 0) {
                    // still alive , according to new value of expireAfterAccess
                    timeWheel.schedule(this, nowTicks + rescheduleTicks);
                    this.expireAfterAccess = currentExpireAfterAccessNanos;
                    return;
                }
            }
            doRemove(this.key, RemovalCause.EXPIRE_AFTER_ACCESS);
        }
    }

    private class AccessLinkedList extends AccessList<CacheItem> {

        private CacheItem head = new CacheItem(null, null, null, null);

        AccessLinkedList() {
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
            if(isUnLinked(node)) {
                return;
            }
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

        private volatile boolean running = true;

        /**
         * backend thread
         */
        private final Thread thread;

        BackendThread() {
            this.thread = new Thread(this);
            this.thread.setDaemon(true);
        }

        void start() {
            this.thread.start();
        }

        void stop() {
            this.running = false;
            unpark();
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
        void maintenance() {
            drainUserEvent();
            doScheduleWork();
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

        @RunIn("backend")
        void doScheduleWork() {
            CacheItem ci;
            if ((ci = getScheduled()) != null) {
                if (ci.loader == null) {
                    doRemove(ci.key, RemovalCause.EXPIRE_AFTER_WRITE);
                    return;
                }
                doRefresh(ci);
                reschedule(ci);
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
                scheduleInTimeWheel(ci);
            }
        }

        @RunIn("backend")
        void evict() {
            int size = size();
            if (isEvict() && size > capacity) {
                CacheItem victim = accessList.head().getPrev();
                while (size > capacity && victim != accessList.head()) {
                    CacheItem nextVictim = victim.getPrev();
                    doRemove(victim.key, RemovalCause.EVICT);
                    victim = nextVictim;
                    size--;
                }
            }
        }

        @RunIn("backend")
        void expireAfterAccess() {
            timeWheel.advance(timeWheelTicker.read());
        }

        @RunIn("backend")
        void peekTaskAndWait() {
            long delayNanos = 0;
            CacheItem ci = scheduleQueue.peek();
            if (ci == null) {
                park();
            } else if ((delayNanos = ci.getDelay(NANOSECONDS)) > MIN_DELAYED_NANO) {
                park(delayNanos);
            }
        }

        private void reschedule(CacheItem ci) {
            ci.resetDelay();
            scheduleQueue.add(ci);
        }

        private CacheItem getScheduled() {
            CacheItem sc = scheduleQueue.peek();
            if (sc != null && sc.getDelay(NANOSECONDS) <= MIN_DELAYED_NANO) {
                return scheduleQueue.poll();
            }
            return null;
        }

        @RunIn("backend")
        private void park(long nanos) {
            LockSupport.parkNanos(nanos);
        }

        @RunIn("backend")
        private void park() {
            LockSupport.park();
        }

        private void unpark() {
            LockSupport.unpark(this.thread);
        }
    }
}
