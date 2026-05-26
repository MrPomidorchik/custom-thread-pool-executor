package custompool.rejection;

public class CallerRunsPolicy implements CustomRejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable task) {
        System.out.println("[Rejected] Task " + task + " will be executed in caller thread: "
                + Thread.currentThread().getName());

        task.run();
    }
}
