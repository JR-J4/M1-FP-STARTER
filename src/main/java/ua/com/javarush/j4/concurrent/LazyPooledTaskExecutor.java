package ua.com.javarush.j4.concurrent;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Defers pool creation until the first batch of tasks actually runs.
 *
 * <p>Every parallel component in this project checks its input size before submitting
 * work. Wrapping the pool in this class means a run over small inputs — which is every
 * run the shipped test suite makes — never starts a thread at all.
 */
public final class LazyPooledTaskExecutor implements TaskExecutor {
    private final int parallelism;
    private PooledTaskExecutor delegate;

    public LazyPooledTaskExecutor(int parallelism) {
        this.parallelism = Math.max(1, parallelism);
    }

    @Override
    public <T> List<T> invokeAll(List<? extends Callable<T>> tasks) {
        return pool().invokeAll(tasks);
    }

    @Override
    public int parallelism() {
        return parallelism;
    }

    @Override
    public synchronized void close() {
        if (delegate != null) {
            delegate.close();
            delegate = null;
        }
    }

    private synchronized PooledTaskExecutor pool() {
        if (delegate == null) {
            delegate = new PooledTaskExecutor(parallelism);
        }
        return delegate;
    }

    /** Visible for testing: whether a pool has been created yet. */
    synchronized boolean poolStarted() {
        return delegate != null;
    }
}
