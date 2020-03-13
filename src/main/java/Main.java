import com.nothinghappen.cache.Cache;
import com.nothinghappen.cache.CacheBuilder;
import com.nothinghappen.cache.CacheLoader;
import com.nothinghappen.cache.Listener;
import com.nothinghappen.cache.RemovalCause;
import org.checkerframework.checker.units.qual.A;


import java.io.IOException;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class Main {

    private static ExecutorService es = Executors.newFixedThreadPool(100);
    private static Cache ci;
    private static String s;
    private static int RANDOM = 10000;
    private static Cache cache;

    public static void main(String[] args) throws IOException, InterruptedException {

        AtomicLong read = new AtomicLong();
        AtomicLong write = new AtomicLong();
        AtomicLong evict = new AtomicLong();
        AtomicLong refresh = new AtomicLong();
        AtomicLong expireAfterAccess = new AtomicLong();


        Listener listener = new Listener() {
            @Override
            public void onRead(String key) {
                read.incrementAndGet();
            }
            @Override
            public void onWrite(String key) {
                write.incrementAndGet();
            }
            @Override
            public void onRemove(String key, Object value, RemovalCause cause) {
                if (cause == RemovalCause.EVICT){
                    evict.incrementAndGet();
                } else if (cause == RemovalCause.EXPIRE_AFTER_ACCESS) {
                    expireAfterAccess.incrementAndGet();
                }
            }
            @Override
            public void onRefresh(String key) {
                refresh.incrementAndGet();
            }
        };

        cache = CacheBuilder.newBuilder().setCapacity(8000).registerListener(listener).build();

        for (int i = 0; i < 100; i++) {
            int finalI = i;
            es.submit(() -> {
                while (true) {
                    byte[] value = (byte[]) cache.getOrLoad(randomKey(), (k,v) -> randomValue(),
                            randomExpireMills(),
                            finalI % 2 == 0 ? 30000 : 0,
                            TimeUnit.MILLISECONDS);
                    randomWait();
                }
            });
        }
        while (true) {
            System.out.println(String.format("read:%s, write:%s, evict:%s, refresh:%s, expireAfterAccess:%s, size:%s",
                    read, write, evict, refresh, expireAfterAccess, cache.size()));
            LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(1000, TimeUnit.MILLISECONDS));
        }
    }

    public static String randomKey() {
        return "key" + new Random().nextInt(10000);
    }

    public static byte[] randomValue() {
        return new byte[1024 * 10];
    }

    public static int randomExpireMills() {
        return new Random().nextInt(1000) + 1000;
    }

    public static void randomWait() {
        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(new Random().nextInt(1000), TimeUnit.MILLISECONDS));
    }
}
