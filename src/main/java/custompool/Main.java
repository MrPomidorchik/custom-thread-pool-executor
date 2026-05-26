package custompool;

import custompool.config.ThreadPoolConfigLoader;
import custompool.executor.CustomExecutor;
import custompool.executor.CustomThreadPool;
import custompool.task.DemoTask;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public class Main {

    public static void main(String[] args) throws Exception {
        normalWorkloadDemo();

        System.out.println();
        System.out.println("==================================");
        System.out.println();

        overloadDemo();
    }

    private static void normalWorkloadDemo() throws Exception {
        System.out.println("=== Normal workload demo ===");

        ThreadPoolConfigLoader configLoader = new ThreadPoolConfigLoader("pool.env");

        CustomExecutor executor = new CustomThreadPool(
                configLoader.getPoolName(),
                configLoader.getThreadPoolConfig(),
                configLoader.getRejectedExecutionHandler()
        );

        for (int i = 1; i <= 8; i++) {
            executor.execute(new DemoTask("normal-task-" + i, 1500));
        }

        Future<String> future = executor.submit(new Callable<>() {
            @Override
            public String call() throws Exception {
                System.out.println("[Callable] callable-task started.");
                Thread.sleep(1000);
                System.out.println("[Callable] callable-task finished.");
                return "Callable result received";
            }

            @Override
            public String toString() {
                return "callable-task";
            }
        });

        System.out.println("[Main] Future result: " + future.get());

        Thread.sleep(4000);

        executor.shutdown();

        try {
            executor.execute(new DemoTask("task-after-shutdown", 1000));
        } catch (RejectedExecutionException e) {
            System.out.println("[Main] Expected rejection after shutdown: " + e.getMessage());
        }

        Thread.sleep(7000);

        System.out.println("[Main] Normal workload demo finished.");
    }

    private static void overloadDemo() throws InterruptedException {
        System.out.println("=== Overload demo ===");

        ThreadPoolConfigLoader configLoader = new ThreadPoolConfigLoader("overload-pool.env");

        CustomExecutor executor = new CustomThreadPool(
                configLoader.getPoolName(),
                configLoader.getThreadPoolConfig(),
                configLoader.getRejectedExecutionHandler()
        );

        for (int i = 1; i <= 10; i++) {
            try {
                executor.execute(new DemoTask("overload-task-" + i, 5000));
            } catch (RejectedExecutionException e) {
                System.out.println("[Main] Rejection handled: " + e.getMessage());
            }
        }

        Thread.sleep(3000);

        executor.shutdownNow();

        Thread.sleep(2000);

        System.out.println("[Main] Overload demo finished.");
    }
}