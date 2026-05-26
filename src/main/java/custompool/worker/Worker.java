package custompool.worker;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class Worker implements Runnable {

    private final int id;
    private final BlockingQueue<Runnable> taskQueue;

    private final long keepAliveTime;
    private final TimeUnit timeUnit;

    private final BooleanSupplier shutdownSupplier;
    private final BooleanSupplier shutdownNowSupplier;
    private final BooleanSupplier canStopByIdleTimeout;
    private final Consumer<Worker> terminationCallback;

    private volatile Thread thread;
    private volatile boolean idle;
    private volatile boolean stopped;

    public Worker(
            int id,
            int queueSize,
            long keepAliveTime,
            TimeUnit timeUnit,
            BooleanSupplier shutdownSupplier,
            BooleanSupplier shutdownNowSupplier,
            BooleanSupplier canStopByIdleTimeout,
            Consumer<Worker> terminationCallback
    ) {
        this.id = id;
        this.taskQueue = new ArrayBlockingQueue<>(queueSize);
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.shutdownSupplier = shutdownSupplier;
        this.shutdownNowSupplier = shutdownNowSupplier;
        this.canStopByIdleTimeout = canStopByIdleTimeout;
        this.terminationCallback = terminationCallback;
    }

    public int getId() {
        return id;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public boolean offerTask(Runnable task) {
        return taskQueue.offer(task);
    }

    public boolean isIdle() {
        return idle && taskQueue.isEmpty() && !stopped;
    }

    public boolean isStopped() {
        return stopped;
    }

    public int getQueueSize() {
        return taskQueue.size();
    }

    public int clearQueue() {
        int removedTasks = taskQueue.size();
        taskQueue.clear();
        return removedTasks;
    }

    public void interrupt() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    public void interruptIfIdle() {
        if (idle && thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (shutdownNowSupplier.getAsBoolean()) {
                    break;
                }

                if (shutdownSupplier.getAsBoolean() && taskQueue.isEmpty()) {
                    break;
                }

                Runnable task;

                try {
                    idle = true;
                    task = taskQueue.poll(keepAliveTime, timeUnit);
                    idle = false;
                } catch (InterruptedException e) {
                    idle = false;

                    if (shutdownNowSupplier.getAsBoolean()) {
                        break;
                    }

                    if (shutdownSupplier.getAsBoolean() && taskQueue.isEmpty()) {
                        break;
                    }

                    continue;
                }

                if (task == null) {
                    if (!shutdownSupplier.getAsBoolean() && canStopByIdleTimeout.getAsBoolean()) {
                        System.out.println("[Worker] "
                                + Thread.currentThread().getName()
                                + " idle timeout, stopping.");

                        break;
                    }

                    continue;
                }

                if (shutdownNowSupplier.getAsBoolean()) {
                    break;
                }

                System.out.println("[Worker] "
                        + Thread.currentThread().getName()
                        + " executes "
                        + task);

                try {
                    task.run();
                } catch (Exception e) {
                    System.out.println("[Worker] Task " + task + " failed: " + e.getMessage());
                }
            }
        } finally {
            stopped = true;
            idle = false;

            terminationCallback.accept(this);

            System.out.println("[Worker] "
                    + Thread.currentThread().getName()
                    + " terminated.");
        }
    }
}