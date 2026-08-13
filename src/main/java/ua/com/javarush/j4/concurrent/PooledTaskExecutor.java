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
 * Runs tasks on a fixed thread pool of the configured width.
 *
 * <p>The pool is created on first use, not on construction. Every parallel component checks
 * its input size before submitting anything, so a run over small inputs — which is every run
 * the shipped test suite makes — never starts a thread at all.
 *
 * <p>A fixed pool rather than a {@code ForkJoinPool}: work is fanned out at exactly one level
 * in this design, so there is no nested {@code join} for work-stealing to help with, and
 * {@code ForkJoinTask} reconstructs a task's exception in the calling thread instead of
 * rethrowing the original. Preserving the real cause matters more than a work-stealing queue
 * this design never exercises.
 *
 * <p>Threads are daemons, so a pool that somehow escaped {@link #close()} could never keep
 * the JVM alive after the CLI has finished.
 */
public final class PooledTaskExecutor implements TaskExecutor {
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final int parallelism;
    private ExecutorService pool; // guarded by this

    public PooledTaskExecutor(int parallelism) {
        this.parallelism = Math.max(1, parallelism);
    }

    /**
     * Submission order is {@link ExecutorService#invokeAll}'s own guarantee, so there is
     * nothing to hand-roll here. It also cancels whatever is still outstanding if the calling
     * thread is interrupted.
     */
    @Override
    public <T> List<T> invokeAll(List<? extends Callable<T>> tasks) {
        List<Future<T>> futures;
        try {
            futures = pool().invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConcurrentExecutionException("Interrupted while awaiting tasks", e);
        }

        List<T> results = new ArrayList<>(futures.size());
        for (Future<T> future : futures) {
            results.add(resultOf(future));
        }
        return results;
    }

    /** Every future is already done, so the first one that failed is the first in submission order. */
    private static <T> T resultOf(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConcurrentExecutionException("Interrupted while collecting a result", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new ConcurrentExecutionException("Task failed: " + cause.getMessage(), cause);
        }
    }

    @Override
    public int parallelism() {
        return parallelism;
    }

    @Override
    public synchronized void close() {
        if (pool == null) {
            return;
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }
        pool = null;
    }

    private synchronized ExecutorService pool() {
        if (pool == null) {
            pool = Executors.newFixedThreadPool(parallelism, daemonThreadFactory());
        }
        return pool;
    }

    /** Visible for testing: whether a pool has been created yet. */
    synchronized boolean poolStarted() {
        return pool != null;
    }

    private static ThreadFactory daemonThreadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "cryptanalyzer-worker-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
