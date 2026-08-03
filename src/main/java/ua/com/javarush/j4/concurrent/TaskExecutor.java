package ua.com.javarush.j4.concurrent;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Runs a batch of tasks to completion.
 *
 * <p>Results come back in <em>submission</em> order, never completion order. Callers rely
 * on that guarantee for deterministic output: it is what lets chunked text be rejoined
 * correctly and lets a brute-force sweep break score ties toward the lowest key.
 */
public interface TaskExecutor extends AutoCloseable {

    /**
     * Runs every task, blocks until all have finished, and returns their results in the
     * order the tasks were supplied.
     *
     * @throws ua.com.javarush.j4.error.ConcurrentExecutionException if any task failed,
     *         wrapping the first failure in submission order
     */
    <T> List<T> invokeAll(List<? extends Callable<T>> tasks);

    /** Configured width. Answering this must not start any threads. */
    int parallelism();

    /** Releases any threads held. Safe to call more than once. */
    @Override
    void close();
}
