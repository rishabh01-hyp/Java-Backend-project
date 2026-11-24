package service;

import model.PartUsage;
import model.Validation;

import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Producers submit to a bounded queue; ExecutorService workers consume it. */
public final class PartRequestProcessor implements AutoCloseable {
    @FunctionalInterface
    public interface Handler {
        int recordUsage(String jobId, String partId, int quantity) throws SQLException;
    }

    public record Submission(long id, CompletionStage<Integer> result) { }
    private record Request(String jobId, PartUsage usage, CompletableFuture<Integer> result) { }
    private final BlockingQueue<Request> queue;
    private final ExecutorService workers;
    private final Handler handler;
    private final AtomicLong ids = new AtomicLong();
    private volatile boolean accepting = true;

    public PartRequestProcessor(Handler handler, int workerCount, int queueCapacity) {
        if (workerCount < 1 || queueCapacity < 1) { throw new IllegalArgumentException("Workers and capacity must be positive."); }
        this.handler = java.util.Objects.requireNonNull(handler);
        queue = new ArrayBlockingQueue<>(queueCapacity);
        workers = Executors.newFixedThreadPool(workerCount);
        for (int i = 0; i < workerCount; i++) { workers.submit(this::consume); }
    }

    public synchronized Submission submit(String jobId, String partId, int quantity) {
        if (!accepting) { throw new RejectedExecutionException("Request processor is closed."); }
        String job = Validation.id(jobId, "Job ID");
        var usage = new PartUsage(partId, quantity);
        var result = new CompletableFuture<Integer>();
        if (!queue.offer(new Request(job, usage, result))) {
            throw new RejectedExecutionException("Request queue is full. Try again after checking results.");
        }
        // A read-only view: caller cancellation cannot remove an already accepted stock request.
        return new Submission(ids.incrementAndGet(), result.minimalCompletionStage());
    }

    private void consume() {
        try {
            while (accepting || !queue.isEmpty()) {
                Request request = queue.poll(100, TimeUnit.MILLISECONDS);
                if (request == null) { continue; }
                try {
                    request.result().complete(handler.recordUsage(request.jobId(),
                            request.usage().partId(), request.usage().quantity()));
                } catch (SQLException | RuntimeException e) {
                    request.result().completeExceptionally(e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Stop producers, drain accepted work, then terminate workers before returning. */
    @Override public void close() {
        synchronized (this) {
            accepting = false;
            workers.shutdown();
        }
        boolean interrupted = false;
        while (!workers.isTerminated()) {
            try { workers.awaitTermination(1, TimeUnit.DAYS); }
            catch (InterruptedException e) { interrupted = true; }
        }
        if (interrupted) { Thread.currentThread().interrupt(); }
    }
}
