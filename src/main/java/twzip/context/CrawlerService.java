package twzip.context;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 *
 * @author Kent Yeh
 */
@Service
@Lazy
public class CrawlerService extends ThreadPoolExecutor {

    private static final int core = Runtime.getRuntime().availableProcessors();
    private final AtomicInteger counter = new AtomicInteger(0);
    private final CountDownLatch cdl = new CountDownLatch(1);

    public CountDownLatch getCountDownLatch() {
        return cdl;
    }

    public CrawlerService() {
        super(core < 2 ? 2 : core, core * 2, 5, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(core * 2, true),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (counter.decrementAndGet() == 0) {
            shutdown();
            cdl.countDown();
        }
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        counter.incrementAndGet();
        super.beforeExecute(t, r);
    }

}
