package custompool.factory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadFactory implements ThreadFactory {

    private final String poolName;
    private final AtomicInteger threadCounter = new AtomicInteger(1);

    public CustomThreadFactory(String poolName) {
        if (poolName == null || poolName.isBlank()) {
            throw new IllegalArgumentException("poolName must not be empty");
        }

        this.poolName = poolName;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String threadName = poolName + "-worker-" + threadCounter.getAndIncrement();

        System.out.println("[ThreadFactory] Creating new thread: " + threadName);

        return new Thread(() -> {
            try {
                runnable.run();
            } finally {
                System.out.println("[ThreadFactory] Thread finished: " + Thread.currentThread().getName());
            }
        }, threadName);
    }
}
