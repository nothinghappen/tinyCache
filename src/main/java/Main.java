import com.nothinghappen.cache.Cache;
import com.nothinghappen.cache.CacheBuilder;
import com.nothinghappen.cache.CacheImpl;
import com.nothinghappen.cache.CacheLoader;
import com.nothinghappen.cache.Ticker;
import com.nothinghappen.cache.Tickers;
import com.nothinghappen.cache.datastruct.WheelTimer;
import com.sun.javaws.IconUtil;

import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    private static ExecutorService es = Executors.newFixedThreadPool(100);
    private static CacheImpl ci;
    private static String s;
    private static int RANDOM = 10000;

    public static void main(String[] args) throws IOException, InterruptedException {

        ci = CacheBuilder.newBuilder().setCapacity(1000).build();
        getOrLoad("key1");
        System.in.read();
        run(2, () -> {
            while (true) {
                s = (String) ci.get("key1");
            }
        });
        System.out.println(ci.get("key1"));
    }

    public static void getOrLoad(String key) {
        String value = (String) ci.getOrLoad(key, new CacheLoader() {
            @Override
            public Object load(String key, Object oldValue) {
                System.out.println(key + " Load in Thread : " + Thread.currentThread().getId() + " time: " + System.currentTimeMillis());
                return key + System.currentTimeMillis();
            }
        }, 2000, TimeUnit.MILLISECONDS);
    }

    public static void run(int threadNum, Runnable run) {
        CyclicBarrier cb = new CyclicBarrier(threadNum);
        for (int i = 0; i < threadNum; i++) {
            es.execute(() -> {
                try {
                    cb.await();
                } catch (InterruptedException e) {
                } catch (BrokenBarrierException e) {
                }
                run.run();
            });
        }
    }

}
