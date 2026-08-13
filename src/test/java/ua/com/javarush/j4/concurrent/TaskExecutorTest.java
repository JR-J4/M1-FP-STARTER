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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

        @Test
        void answersParallelismWithoutStartingAPool() {
            PooledTaskExecutor executor = new PooledTaskExecutor(4);
            try {
                assertEquals(4, executor.parallelism());
                assertTrue(executor.worthSplitting(100, 10));
                assertFalse(executor.poolStarted(), "asking about width must not start threads");
            } finally {
                executor.close();
            }
        }

        @Test
        void startsThePoolOnFirstUse() {
            PooledTaskExecutor executor = new PooledTaskExecutor(4);
            try {
                assertEquals(List.of(0, 1, 2, 3), executor.invokeAll(scrambledTasks(4)));
                assertTrue(executor.poolStarted());
            } finally {
                executor.close();
            }
        }

        @Test
        void closeIsSafeWhenNeverUsedAndWhenRepeated() {
            PooledTaskExecutor executor = new PooledTaskExecutor(4);
            executor.close();
            assertFalse(executor.poolStarted());

            executor.invokeAll(List.<Callable<Integer>>of(() -> 1));
            assertTrue(executor.poolStarted());
            executor.close();
            executor.close();
            assertFalse(executor.poolStarted());
        }
    }

    @Nested
    @DisplayName("worthSplitting")
    class WorthSplitting {

        @Test
        void oneThreadNeverSplits() {
            try (TaskExecutor sequential = new DirectTaskExecutor()) {
                assertFalse(sequential.worthSplitting(Integer.MAX_VALUE, 1));
            }
        }

        @Test
        void theThresholdIsInclusive() {
            try (TaskExecutor executor = new PooledTaskExecutor(4)) {
                assertFalse(executor.worthSplitting(999, 1_000));
                assertTrue(executor.worthSplitting(1_000, 1_000));
                assertTrue(executor.worthSplitting(1_001, 1_000));
            }
        }
    }

    @Nested
    @DisplayName("TaskExecutors")
    class Factory {

        @Test
        void zeroOrLessMeansEveryAvailableCore() {
            int cores = Runtime.getRuntime().availableProcessors();
            try (TaskExecutor everyCore = TaskExecutors.of(0); TaskExecutor negative = TaskExecutors.of(-1)) {
                assertEquals(cores, everyCore.parallelism());
                assertEquals(cores, negative.parallelism());
            }
        }

        @Test
        void oneThreadYieldsTheSameThreadExecutor() {
            try (TaskExecutor executor = TaskExecutors.of(1)) {
                assertInstanceOf(DirectTaskExecutor.class, executor);
                assertEquals(1, executor.parallelism());
            }
        }

        @Test
        void anExplicitCountIsHonoured() {
            try (TaskExecutor executor = TaskExecutors.of(3)) {
                assertInstanceOf(PooledTaskExecutor.class, executor);
                assertEquals(3, executor.parallelism());
            }
        }
    }
}
