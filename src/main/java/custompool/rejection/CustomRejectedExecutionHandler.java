package custompool.rejection;

public interface CustomRejectedExecutionHandler {

    void rejectedExecution(Runnable task);
}
