import java.sql.SQLException;
import java.util.*;

class ShopException extends IllegalArgumentException {
    public ShopException(String message) {
        super(message);
    }
}

abstract class Entity {
    private final String id;

    protected Entity(String id) {
        this.id = validId(id);
    }

    public String getId() { return id; }
    public abstract String description();

    static String validId(String value) {
        value = required(value);
        if (!value.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new ShopException("ID needs 1-64 letters, digits, underscores or hyphens.");
        }
        return value;
    }

    static String required(String value) {
        if (value == null || value.trim().isEmpty()) throw new ShopException("Text cannot be empty.");
        return value.trim();
    }

    static int nonnegative(int value) {
        if (value < 0) throw new ShopException("Quantity cannot be negative.");
        return value;
    }

    static int positive(int value) {
        if (value <= 0) throw new ShopException("Quantity must be positive.");
        return value;
    }
}

class Part extends Entity implements Comparable<Part> {
    private final String name;
    private final int quantity;

    public Part(String id, String name, int quantity) {
        super(id);
        this.name = required(name);
        this.quantity = nonnegative(quantity);
        if (this.name.length() > 255) throw new ShopException("Part name is too long.");
    }

    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    @Override public int compareTo(Part other) { return getId().compareTo(other.getId()); }
    @Override public String description() {
        return String.format("%-12s %-30s Stock: %d", getId(), name, quantity);
    }
}

class RepairJob extends Entity {
    private final String device;
    private final String issue;

    public RepairJob(String id, String device, String issue) {
        super(id);
        this.device = required(device);
        this.issue = required(issue);
        if (this.device.length() > 255) throw new ShopException("Device name is too long.");
    }

    public String getDevice() { return device; }
    public String getIssue() { return issue; }
    @Override public String description() {
        return getId() + " | " + device + " | " + issue;
    }
}

interface UsageTask {
    String perform() throws SQLException;
}

/** A single consumer processes tasks; this object's monitor protects the queue. */
class UsageWorker implements Runnable {
    private final Queue<UsageTask> queue = new LinkedList<>();
    private final List<String> results = new ArrayList<>();
    private final Thread thread = new Thread(this, "part-worker");
    private boolean closing;
    private boolean busy;

    public void start() { thread.start(); }

    public synchronized void submit(UsageTask task) {
        if (closing || queue.size() >= 100 || results.size() >= 100) {
            throw new ShopException("Queue unavailable. Use option 11 before submitting more.");
        }
        queue.add(task);
        notifyAll();
    }

    @Override public void run() {
        while (true) {
            UsageTask task;
            synchronized (this) {
                while (queue.isEmpty() && !closing) {
                    try { wait(); }
                    catch (InterruptedException e) { closing = true; }
                }
                if (queue.isEmpty()) return;
                task = queue.remove();
                busy = true;
            }
            String result;
            try { result = task.perform(); }
            catch (SQLException | RuntimeException e) { result = "Request failed: " + e.getMessage(); }
            synchronized (this) {
                results.add(result);
                busy = false;
                notifyAll();
            }
        }
    }

    public synchronized void awaitIdle() {
        boolean interrupted = false;
        while (busy || !queue.isEmpty()) {
            try { wait(); }
            catch (InterruptedException e) { interrupted = true; }
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    public synchronized void printResults() {
        results.forEach(System.out::println);
        results.clear();
    }

    public void finish() {
        synchronized (this) {
            closing = true;
            notifyAll();
        }
        boolean interrupted = false;
        while (thread.isAlive()) {
            try { thread.join(); }
            catch (InterruptedException e) { interrupted = true; }
        }
        if (interrupted) Thread.currentThread().interrupt();
    }
}
