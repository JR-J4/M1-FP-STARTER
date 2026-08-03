package ua.com.javarush.j4.concurrent;

import ua.com.javarush.j4.error.ConcurrentExecutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Runs every task inline on the calling thread. Used below the parallelism thresholds,
 * inside batch workers to keep fan-out to a single level, and in tests where determinism
 * matters more than speed.
 */
public final class DirectTaskExecutor implements TaskExecutor {

    @Override
    public <T> List<T> invokeAll(List<? extends Callable<T>> tasks) {
        List<T> results = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            try {
                results.add(task.call());
            } catch (Exception e) {
                throw new ConcurrentExecutionException("Task failed: " + e.getMessage(), e);
            }
        }
        return results;
    }

    @Override
    public int parallelism() {
        return 1;
    }

    @Override
    public void close() {
        // holds no resources
    }
}
