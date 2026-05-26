package custompool.executor;

import custompool.config.ThreadPoolConfig;
import custompool.factory.CustomThreadFactory;
import custompool.rejection.CustomRejectedExecutionHandler;
import custompool.task.TaskWrapper;
import custompool.worker.Worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CustomThreadPool implements CustomExecutor {

    private final ThreadPoolConfig config;
    private final CustomThreadFactory threadFactory;
    private final CustomRejectedExecutionHandler rejectedExecutionHandler;

    private final List<Worker> workers = new CopyOnWriteArrayList<>();

    private final AtomicInteger workerIdCounter = new AtomicInteger(1);
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicBoolean shutdownNow = new AtomicBoolean(false);

    private final Object workerLock = new Object();

    public CustomThreadPool(
            String poolName,
            ThreadPoolConfig config,
            CustomRejectedExecutionHandler rejectedExecutionHandler
    ) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.rejectedExecutionHandler = Objects.requireNonNull(
                rejectedExecutionHandler,
                "rejectedExecutionHandler must not be null"
        );
        this.threadFactory = new CustomThreadFactory(poolName);

        synchronized (workerLock) {
            for (int i = 0; i < config.getCorePoolSize(); i++) {
                addWorker();
            }
        }
    }

    @Override
    public void execute(Runnable command) {
        Objects.requireNonNull(command, "command must not be null");

        if (shutdown.get()) {
            rejectedExecutionHandler.rejectedExecution(command);
            return;
        }

        ensureMinSpareThreads();

        if (tryEnqueueRoundRobin(command)) {
            return;
        }

        synchronized (workerLock) {
            if (!shutdown.get() && workers.size() < config.getMaxPoolSize()) {
                Worker worker = addWorker();

                if (worker.offerTask(command)) {
                    System.out.println("[Pool] Task accepted into queue #" + worker.getId() + ": " + command);
                    return;
                }
            }
        }

        if (tryEnqueueRoundRobin(command)) {
            return;
        }

        rejectedExecutionHandler.rejectedExecution(command);
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        Objects.requireNonNull(callable, "callable must not be null");

        FutureTask<T> futureTask = new FutureTask<>(callable);

        Runnable wrappedTask = new TaskWrapper(
                futureTask,
                callable.toString()
        );

        execute(wrappedTask);

        return futureTask;
    }

    @Override
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            System.out.println("[Pool] shutdown() called. New tasks will be rejected.");

            for (Worker worker : workers) {
                worker.interruptIfIdle();
            }
        }
    }

    @Override
    public void shutdownNow() {
        if (shutdownNow.compareAndSet(false, true)) {
            shutdown.set(true);

            System.out.println("[Pool] shutdownNow() called. Queued tasks will be removed and workers interrupted.");

            for (Worker worker : workers) {
                int removedTasks = worker.clearQueue();

                if (removedTasks > 0) {
                    System.out.println("[Pool] Removed " + removedTasks
                            + " queued task(s) from queue #" + worker.getId());
                }

                worker.interrupt();
            }
        }
    }

    private Worker addWorker() {
        int workerId = workerIdCounter.getAndIncrement();

        AtomicReference<Worker> workerReference = new AtomicReference<>();

        Worker worker = new Worker(
                workerId,
                config.getQueueSize(),
                config.getKeepAliveTime(),
                config.getTimeUnit(),
                shutdown::get,
                shutdownNow::get,
                () -> tryRemoveWorkerByIdleTimeout(workerReference.get()),
                workers::remove
        );

        workerReference.set(worker);

        Thread thread = threadFactory.newThread(worker);

        worker.setThread(thread);
        workers.add(worker);

        thread.start();

        return worker;
    }

    private boolean tryRemoveWorkerByIdleTimeout(Worker worker) {
        if (worker == null) {
            return false;
        }

        synchronized (workerLock) {
            if (shutdown.get() || shutdownNow.get()) {
                return true;
            }

            if (workers.size() > config.getCorePoolSize()) {
                workers.remove(worker);
                return true;
            }

            return false;
        }
    }

    private void ensureMinSpareThreads() {
        synchronized (workerLock) {
            if (shutdown.get()) {
                return;
            }

            int spareThreads = countSpareThreads();
            int missingSpareThreads = config.getMinSpareThreads() - spareThreads;
            int possibleToCreate = config.getMaxPoolSize() - workers.size();

            int threadsToCreate = Math.min(missingSpareThreads, possibleToCreate);

            for (int i = 0; i < threadsToCreate; i++) {
                addWorker();
            }
        }
    }

    private int countSpareThreads() {
        int count = 0;

        for (Worker worker : workers) {
            if (worker.isIdle()) {
                count++;
            }
        }

        return count;
    }

    private boolean tryEnqueueRoundRobin(Runnable command) {
        List<Worker> snapshot = new ArrayList<>(workers);

        if (snapshot.isEmpty()) {
            return false;
        }

        int startIndex = Math.floorMod(
                roundRobinIndex.getAndIncrement(),
                snapshot.size()
        );

        for (int i = 0; i < snapshot.size(); i++) {
            int index = (startIndex + i) % snapshot.size();
            Worker worker = snapshot.get(index);

            if (worker.isStopped()) {
                continue;
            }

            if (worker.offerTask(command)) {
                System.out.println("[Pool] Task accepted into queue #" + worker.getId() + ": " + command);
                return true;
            }
        }

        return false;
    }
}