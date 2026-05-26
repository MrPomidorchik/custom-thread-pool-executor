package custompool.rejection;

import java.util.concurrent.RejectedExecutionException;

public class AbortPolicy implements CustomRejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable task) {
        String message = "[Rejected] Task " + task + " was rejected due to overload!";
        System.out.println(message);
        throw new RejectedExecutionException(message);
    }
}
