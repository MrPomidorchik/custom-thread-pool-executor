package custompool.task;

public class TaskWrapper implements Runnable {

    private final Runnable task;
    private final String description;

    public TaskWrapper(Runnable task, String description) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be empty");
        }

        this.task = task;
        this.description = description;
    }

    @Override
    public void run() {
        task.run();
    }

    @Override
    public String toString() {
        return description;
    }
}
