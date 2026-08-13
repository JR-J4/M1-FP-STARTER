package ua.com.javarush.j4.concurrent;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Runs a batch of tasks to completion.
 *
 * <p>Results come back in <em>submission</em> order, never completion order. Callers rely
 * on that guarantee for deterministic output: it is what lets chunked text be rejoined
 * correctly.
 *
 * <p>This interface is also the single answer to "should this workload be split at all?".
 * There is deliberately no separate policy object holding a second copy of the thread count:
 * one existed, the two copies could disagree, and every caller had to consult both. Handing
 * a component a {@link DirectTaskExecutor} is now sufficient to make it sequential, which is
 * how batch runs keep fan-out to exactly one level.
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

    /**
     * Whether this executor is wide enough, and the workload big enough, to be worth
     * splitting. Each component supplies its own measured {@code minUnits}, because only the
     * component knows what a unit costs — characters for a transform, files for a batch.
     */
    default boolean worthSplitting(int workUnits, int minUnits) {
        return parallelism() > 1 && workUnits >= minUnits;
    }

    /** Releases any threads held. Safe to call more than once. */
    @Override
    void close();
}
