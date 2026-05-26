package custompool.task;

public class DemoTask implements Runnable {

    private final String name;
    private final long durationMillis;

    public DemoTask(String name, long durationMillis) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Task name must not be empty");
        }

        if (durationMillis < 0) {
            throw new IllegalArgumentException("durationMillis must not be negative");
        }

        this.name = name;
        this.durationMillis = durationMillis;
    }

    @Override
    public void run() {
        System.out.println("[Task] " + name + " started.");

        try {
            Thread.sleep(durationMillis);
        } catch (InterruptedException e) {
            System.out.println("[Task] " + name + " interrupted.");
            Thread.currentThread().interrupt();
            return;
        }

        System.out.println("[Task] " + name + " finished.");
    }

    @Override
    public String toString() {
        return name;
    }
}
