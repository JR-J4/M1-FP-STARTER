package ua.com.javarush.j4.concurrent;

import ua.com.javarush.j4.error.ConcurrentExecutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs tasks on a fixed thread pool sized at construction.
 *
 * <p>A fixed pool rather than a {@code ForkJoinPool}: work is fanned out at exactly one
 * level in this design, so there is no nested {@code join} for work-stealing to help
 * with, and {@code ForkJoinTask} reconstructs a task's exception in the calling thread
 * instead of rethrowing the original. Preserving the real cause matters more than a
 * work-stealing queue this design never exercises.
 *
 * <p>Threads are daemons, so a pool that somehow escaped {@link #close()} could never
 * keep the JVM alive after the CLI has finished.
 */
public final class PooledTaskExecutor implements TaskExecutor {
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final ExecutorService pool;
    private final int parallelism;

    public PooledTaskExecutor(int parallelism) {
        this.parallelism = Math.max(1, parallelism);
        this.pool = Executors.newFixedThreadPool(this.parallelism, daemonThreadFactory());
    }

    private static ThreadFactory daemonThreadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "cryptanalyzer-worker-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    @Override
    public <T> List<T> invokeAll(List<? extends Callable<T>> tasks) {
        List<Future<T>> futures = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            futures.add(pool.submit(task));
        }

        List<T> results = new ArrayList<>(tasks.size());
        ConcurrentExecutionException firstFailure = null;
        for (Future<T> future : futures) {
            try {
                results.add(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                results.add(null);
                if (firstFailure == null) {
                    firstFailure = new ConcurrentExecutionException("Interrupted while awaiting a task", e);
                }
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                results.add(null);
                if (firstFailure == null) {
                    firstFailure = new ConcurrentExecutionException("Task failed: " + cause.getMessage(), cause);
                }
            }
        }

        if (firstFailure != null) {
            throw firstFailure;
        }
        return results;
    }

    @Override
    public int parallelism() {
        return parallelism;
    }

    @Override
    public void close() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }
    }
}
