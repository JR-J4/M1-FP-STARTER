package ua.com.javarush.j4.concurrent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.error.ConcurrentExecutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskExecutorTest {

    /** Tasks that finish in a deliberately scrambled order, to prove results come back sorted. */
    private static List<Callable<Integer>> scrambledTasks(int count) {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int value = i;
            tasks.add(() -> {
                Thread.sleep((count - value) * 5L);
                return value;
            });
        }
        return tasks;
    }

    @Nested
    @DisplayName("DirectTaskExecutor")
    class Direct {

        @Test
        void runsTasksInOrderOnOneThread() {
            try (DirectTaskExecutor executor = new DirectTaskExecutor()) {
                List<Long> threadIds = executor.invokeAll(List.of(
                        () -> Thread.currentThread().getId(),
                        () -> Thread.currentThread().getId()));

                assertEquals(Thread.currentThread().getId(), threadIds.get(0));
                assertEquals(Thread.currentThread().getId(), threadIds.get(1));
            }
        }

        @Test
        void reportsParallelismOfOne() {
            try (DirectTaskExecutor executor = new DirectTaskExecutor()) {
                assertEquals(1, executor.parallelism());
            }
        }

        @Test
        void wrapsTaskFailure() {
            IllegalStateException boom = new IllegalStateException("boom");
            try (DirectTaskExecutor executor = new DirectTaskExecutor()) {
                ConcurrentExecutionException thrown = assertThrows(ConcurrentExecutionException.class,
                        () -> executor.invokeAll(List.of(() -> {
                            throw boom;
                        })));

                assertSame(boom, thrown.getCause());
            }
        }
    }

    @Nested
    @DisplayName("PooledTaskExecutor")
    class Pooled {

        @Test
        void returnsResultsInSubmissionOrderNotCompletionOrder() {
            try (PooledTaskExecutor executor = new PooledTaskExecutor(4)) {
                assertEquals(IntStream.range(0, 8).boxed().toList(),
                        executor.invokeAll(scrambledTasks(8)));
            }
        }

        @Test
        void actuallyUsesMoreThanOneThread() {
            try (PooledTaskExecutor executor = new PooledTaskExecutor(4)) {
                List<Long> ids = executor.invokeAll(scrambledTasks(8).stream()
                        .map(task -> (Callable<Long>) () -> {
                            task.call();
                            return Thread.currentThread().getId();
                        })
                        .toList());

                assertTrue(ids.stream().distinct().count() > 1,
                        "expected work to land on several pool threads, got " + ids);
            }
        }

        @Test
        void wrapsTaskFailurePreservingCause() {
            IllegalStateException boom = new IllegalStateException("boom");
            try (PooledTaskExecutor executor = new PooledTaskExecutor(2)) {
                ConcurrentExecutionException thrown = assertThrows(ConcurrentExecutionException.class,
                        () -> executor.invokeAll(List.<Callable<Integer>>of(
                                () -> 1,
                                () -> {
                                    throw boom;
                                })));

                assertSame(boom, thrown.getCause());
            }
        }

        @Test
        void reportsFirstFailureInSubmissionOrder() {
            try (PooledTaskExecutor executor = new PooledTaskExecutor(4)) {
                ConcurrentExecutionException thrown = assertThrows(ConcurrentExecutionException.class,
                        () -> executor.invokeAll(List.<Callable<Integer>>of(
                                () -> {
                                    throw new IllegalStateException("first");
                                },
                                () -> {
                                    throw new IllegalStateException("second");
                                })));

                assertEquals("first", thrown.getCause().getMessage());
            }
        }
    }

    @Nested
    @DisplayName("LazyPooledTaskExecutor")
    class Lazy {

        @Test
        void answersParallelismWithoutStartingAPool() {
            LazyPooledTaskExecutor executor = new LazyPooledTaskExecutor(4);
            try {
                assertEquals(4, executor.parallelism());
                assertFalse(executor.poolStarted(), "asking for parallelism must not start threads");
            } finally {
                executor.close();
            }
        }

        @Test
        void startsThePoolOnFirstUse() {
            LazyPooledTaskExecutor executor = new LazyPooledTaskExecutor(4);
            try {
                assertEquals(List.of(0, 1, 2, 3), executor.invokeAll(scrambledTasks(4)));
                assertTrue(executor.poolStarted());
            } finally {
                executor.close();
            }
        }

        @Test
        void closeIsSafeWhenNeverUsed() {
            LazyPooledTaskExecutor executor = new LazyPooledTaskExecutor(4);
            executor.close();
            assertFalse(executor.poolStarted());
        }
    }
}
