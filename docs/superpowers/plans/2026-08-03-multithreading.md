# Concurrency Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add parallel brute-force cracking, parallel chunked cipher transforms, and concurrent batch file processing, with byte-identical output to the sequential implementation and no measurable cost on small inputs.

**Architecture:** One `TaskExecutor` interface with a same-thread implementation and a lazily-started `ForkJoinPool` implementation, injected at the `CryptoService` composition root. Each parallel component self-gates on input size via `ParallelPolicy` and falls back to its sequential algorithm below threshold. Parallelism engages at exactly one level — the outermost stage with enough work — so batch tasks hand their inner commands a same-thread executor.

**Tech Stack:** Java 17, Maven wrapper (`./mvnw`), JUnit Jupiter 5.9.2, picocli.

## Global Constraints

- **Java 17.** No virtual threads, no structured concurrency, no `ExecutorService implements AutoCloseable`. Use `ForkJoinPool` and define your own `AutoCloseable`.
- **`src/test/java/ua/com/javarush/j4/MainTest.java` must not be edited.** It is the authoritative behaviour contract.
- **Output must stay byte-identical** to the sequential implementation for every input.
- Package root is `ua.com.javarush.j4`.
- Run tests with `./mvnw test` (never `mvn test` — the wrapper is required).
- Existing code comments are in Ukrainian where they document SOLID principles. New code comments: English, matching the surrounding Javadoc style.
- Commit after each task. Conventional commit format (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`). No AI attribution in commit messages.

## File Structure

**New package `ua.com.javarush.j4.concurrent`** — the executor seam and its policy:

| File | Responsibility |
|---|---|
| `TaskExecutor.java` | Interface: run tasks, return results in submission order |
| `DirectTaskExecutor.java` | Same-thread implementation |
| `PooledTaskExecutor.java` | `ForkJoinPool`-backed implementation |
| `LazyPooledTaskExecutor.java` | Defers pool creation to first use |
| `ParallelPolicy.java` | Thread count + the three size thresholds |

**New package `ua.com.javarush.j4.app.batch`** — multi-file processing:

| File | Responsibility |
|---|---|
| `FileOutcome.java` | Per-file result: success with output path, or failure with cause |
| `BatchReport.java` | Aggregated outcomes + printing |
| `BatchProcessor.java` | Fans files across the executor with per-file error isolation |

**Additions to existing packages:**

| File | Responsibility |
|---|---|
| `error/ConcurrentExecutionException.java` | Task failure wrapper |
| `crack/ParallelCaesarCracker.java` | Range-partitioned keyspace sweep |
| `cipher/PositionDependentCipher.java` | Opt-in capability for offset-aware chunking |
| `cipher/ParallelCipher.java` | Chunking decorator over any `Cipher` |
| `cli/PathExpander.java` | Expands glob arguments to concrete paths |

**Modified:** `Main.java`, `VigenereCipher.java`, `BruteForceCommand.java`, `CryptoService.java`, `CryptoRequest.java`, `CryptoCli.java`.

---

### Task 0: Restore the entry point and establish a green baseline

`Main.main` is currently a reflection scratchpad with the real CLI call commented out. All 30 end-to-end failures come from this. Nothing else can be verified until it is fixed.

**Files:**
- Modify: `src/main/java/ua/com/javarush/j4/Main.java`

**Interfaces:**
- Consumes: nothing
- Produces: a green test suite for every later task to compare against

- [ ] **Step 1: Run the suite and record the failure count**

Run: `./mvnw test`
Expected: FAIL — `Tests run: 88, Failures: 30, Errors: 0, Skipped: 10`

- [ ] **Step 2: Replace the whole file**

The scratch experiments are preserved in git history at commit `391c9bc`; deleting them here is intentional.

```java
package ua.com.javarush.j4;

import ua.com.javarush.j4.cli.CryptoCli;

/**
 * Entry point. Delegates to the picocli front end, which parses arguments and
 * runs the requested command. Never propagates exceptions for bad input.
 */
public class Main {
    public static void main(String[] args) {
        new CryptoCli().run(args);
    }
}
```

- [ ] **Step 3: Run the suite and confirm it is green**

Run: `./mvnw test`
Expected: PASS — `Tests run: 88, Failures: 0, Errors: 0, Skipped: 10`

If any test still fails, stop and investigate before continuing. Every later task depends on this baseline.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/Main.java
git commit -m "fix: restore CLI delegation in Main"
```

---

### Task 1: The `TaskExecutor` seam

**Files:**
- Create: `src/main/java/ua/com/javarush/j4/error/ConcurrentExecutionException.java`
- Create: `src/main/java/ua/com/javarush/j4/concurrent/TaskExecutor.java`
- Create: `src/main/java/ua/com/javarush/j4/concurrent/DirectTaskExecutor.java`
- Create: `src/main/java/ua/com/javarush/j4/concurrent/PooledTaskExecutor.java`
- Create: `src/main/java/ua/com/javarush/j4/concurrent/LazyPooledTaskExecutor.java`
- Test: `src/test/java/ua/com/javarush/j4/concurrent/TaskExecutorTest.java`

**Interfaces:**
- Consumes: `ua.com.javarush.j4.error.CryptanalysisException(String, Throwable)`
- Produces:
  - `interface TaskExecutor extends AutoCloseable` with `<T> List<T> invokeAll(List<? extends Callable<T>> tasks)`, `int parallelism()`, `void close()`
  - `class DirectTaskExecutor implements TaskExecutor` — no-arg constructor
  - `class PooledTaskExecutor implements TaskExecutor` — `PooledTaskExecutor(int parallelism)`
  - `class LazyPooledTaskExecutor implements TaskExecutor` — `LazyPooledTaskExecutor(int parallelism)`, package-private `boolean poolStarted()`
  - `class ConcurrentExecutionException extends CryptanalysisException`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/ua/com/javarush/j4/concurrent/TaskExecutorTest.java`. The test class sits in the same package as the production code so it can call the package-private `poolStarted()`.

```java
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
                        () -> executor.invokeAll(List.of(() -> { throw boom; })));

                assertSame(boom, thrown.getCause());
            }
        }
    }

    @Nested
    @DisplayName("PooledTaskExecutor")
    class ForkJoin {

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
                                () -> { throw boom; })));

                assertSame(boom, thrown.getCause());
            }
        }

        @Test
        void reportsFirstFailureInSubmissionOrder() {
            try (PooledTaskExecutor executor = new PooledTaskExecutor(4)) {
                ConcurrentExecutionException thrown = assertThrows(ConcurrentExecutionException.class,
                        () -> executor.invokeAll(List.<Callable<Integer>>of(
                                () -> { throw new IllegalStateException("first"); },
                                () -> { throw new IllegalStateException("second"); })));

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
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -Dtest='TaskExecutorTest' test`
Expected: FAIL — compilation error, `package ua.com.javarush.j4.concurrent does not exist`

- [ ] **Step 3: Create the exception**

`src/main/java/ua/com/javarush/j4/error/ConcurrentExecutionException.java`:

```java
package ua.com.javarush.j4.error;

/** Thrown when a task submitted to a {@code TaskExecutor} fails. */
public class ConcurrentExecutionException extends CryptanalysisException {
    public ConcurrentExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 4: Create the interface**

`src/main/java/ua/com/javarush/j4/concurrent/TaskExecutor.java`:

```java
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
```

- [ ] **Step 5: Create `DirectTaskExecutor`**

```java
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
```

- [ ] **Step 6: Create `PooledTaskExecutor`**

Note it awaits *every* future before throwing, so a failure never leaves tasks running behind it.

```java
package ua.com.javarush.j4.concurrent;

import ua.com.javarush.j4.error.ConcurrentExecutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Runs tasks on a dedicated {@link ForkJoinPool} sized at construction. */
public final class PooledTaskExecutor implements TaskExecutor {
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final ForkJoinPool pool;
    private final int parallelism;

    public PooledTaskExecutor(int parallelism) {
        this.parallelism = Math.max(1, parallelism);
        this.pool = new ForkJoinPool(this.parallelism);
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
```

- [ ] **Step 7: Create `LazyPooledTaskExecutor`**

```java
package ua.com.javarush.j4.concurrent;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Defers pool creation until the first batch of tasks actually runs.
 *
 * <p>Every parallel component in this project checks its input size before submitting
 * work. Wrapping the pool in this class means a run over small inputs — which is every
 * run the test suite makes — never starts a thread at all.
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
```

- [ ] **Step 8: Run the test to verify it passes**

Run: `./mvnw -Dtest='TaskExecutorTest' test`
Expected: PASS — all 10 tests green

- [ ] **Step 9: Run the full suite**

Run: `./mvnw test`
Expected: PASS — `Failures: 0`, still 10 skipped

- [ ] **Step 10: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/concurrent src/main/java/ua/com/javarush/j4/error/ConcurrentExecutionException.java src/test/java/ua/com/javarush/j4/concurrent
git commit -m "feat: add TaskExecutor seam with direct and fork-join implementations"
```

---

### Task 2: `ParallelPolicy`

**Files:**
- Create: `src/main/java/ua/com/javarush/j4/concurrent/ParallelPolicy.java`
- Test: `src/test/java/ua/com/javarush/j4/concurrent/ParallelPolicyTest.java`

**Interfaces:**
- Consumes: nothing
- Produces: `ParallelPolicy.of(int requested)`, `int threads()`, `boolean shouldParallelizeCrack(int textLength)`, `boolean shouldParallelizeTransform(int textLength)`, `boolean shouldParallelizeBatch(int fileCount)`, and the constants `MIN_CHARS_FOR_CRACK = 8192`, `MIN_CHARS_FOR_TRANSFORM = 65536`, `MIN_FILES_FOR_BATCH = 2`

- [ ] **Step 1: Write the failing test**

```java
package ua.com.javarush.j4.concurrent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelPolicyTest {

    @Test
    void zeroOrLessMeansEveryAvailableCore() {
        int cores = Runtime.getRuntime().availableProcessors();
        assertEquals(cores, ParallelPolicy.of(0).threads());
        assertEquals(cores, ParallelPolicy.of(-1).threads());
    }

    @Test
    void anExplicitCountIsHonoured() {
        assertEquals(3, ParallelPolicy.of(3).threads());
    }

    @Test
    void oneThreadDisablesEveryParallelPath() {
        ParallelPolicy policy = ParallelPolicy.of(1);
        assertFalse(policy.shouldParallelizeCrack(Integer.MAX_VALUE));
        assertFalse(policy.shouldParallelizeTransform(Integer.MAX_VALUE));
        assertFalse(policy.shouldParallelizeBatch(Integer.MAX_VALUE));
    }

    @Test
    void crackThresholdIsInclusive() {
        ParallelPolicy policy = ParallelPolicy.of(4);
        assertFalse(policy.shouldParallelizeCrack(ParallelPolicy.MIN_CHARS_FOR_CRACK - 1));
        assertTrue(policy.shouldParallelizeCrack(ParallelPolicy.MIN_CHARS_FOR_CRACK));
    }

    @Test
    void transformThresholdIsInclusive() {
        ParallelPolicy policy = ParallelPolicy.of(4);
        assertFalse(policy.shouldParallelizeTransform(ParallelPolicy.MIN_CHARS_FOR_TRANSFORM - 1));
        assertTrue(policy.shouldParallelizeTransform(ParallelPolicy.MIN_CHARS_FOR_TRANSFORM));
    }

    @Test
    void batchNeedsAtLeastTwoFiles() {
        ParallelPolicy policy = ParallelPolicy.of(4);
        assertFalse(policy.shouldParallelizeBatch(1));
        assertTrue(policy.shouldParallelizeBatch(2));
    }

    @Test
    void shippedTestFixturesStayOnTheSequentialPath() {
        // hamlet.txt is 838 chars and orwell.txt is ~4 KB: MainTest must never go parallel.
        ParallelPolicy policy = ParallelPolicy.of(8);
        assertFalse(policy.shouldParallelizeCrack(4_200));
        assertFalse(policy.shouldParallelizeTransform(4_200));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -Dtest='ParallelPolicyTest' test`
Expected: FAIL — `cannot find symbol: class ParallelPolicy`

- [ ] **Step 3: Write the implementation**

```java
package ua.com.javarush.j4.concurrent;

/**
 * Decides when parallel execution is worth its overhead.
 *
 * <p>Thresholds live here so there is a single place to tune them, but the decision to
 * apply them lives in each parallel component — that is where the input size is known.
 * Lengths are measured in characters, not bytes.
 */
public final class ParallelPolicy {

    /** Cracking does {@code keyspace × 2} full passes, so it clears handoff cost early. */
    public static final int MIN_CHARS_FOR_CRACK = 8_192;

    /** A transform is a single pass, so it needs far more text to be worth splitting. */
    public static final int MIN_CHARS_FOR_TRANSFORM = 65_536;

    public static final int MIN_FILES_FOR_BATCH = 2;

    private final int threads;

    private ParallelPolicy(int threads) {
        this.threads = threads;
    }

    /** {@code requested <= 0} means "every available core"; {@code 1} means fully sequential. */
    public static ParallelPolicy of(int requested) {
        return new ParallelPolicy(requested <= 0
                ? Runtime.getRuntime().availableProcessors()
                : requested);
    }

    public int threads() {
        return threads;
    }

    public boolean shouldParallelizeCrack(int textLength) {
        return threads > 1 && textLength >= MIN_CHARS_FOR_CRACK;
    }

    public boolean shouldParallelizeTransform(int textLength) {
        return threads > 1 && textLength >= MIN_CHARS_FOR_TRANSFORM;
    }

    public boolean shouldParallelizeBatch(int fileCount) {
        return threads > 1 && fileCount >= MIN_FILES_FOR_BATCH;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./mvnw -Dtest='ParallelPolicyTest' test`
Expected: PASS — 7 tests green

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/concurrent/ParallelPolicy.java src/test/java/ua/com/javarush/j4/concurrent/ParallelPolicyTest.java
git commit -m "feat: add ParallelPolicy thresholds"
```

---

### Task 3: `ParallelCaesarCracker`

The keyspace splits into contiguous ascending ranges rather than one task per key. One task per key would hold up to 33 full decrypted copies of the text at once — ~330 MB for a 10 MB input — whereas ranges cap live copies at roughly `2 × threads`.

**Determinism is the point of this task.** `CaesarCracker` uses strict `score > bestScore`, so the *lowest* key wins a tie. The parallel version must match exactly.

**Files:**
- Create: `src/main/java/ua/com/javarush/j4/crack/ParallelCaesarCracker.java`
- Test: `src/test/java/ua/com/javarush/j4/crack/ParallelCaesarCrackerTest.java`

**Interfaces:**
- Consumes: `TaskExecutor`, `ParallelPolicy`, `Cracker`, `CrackResult(int key, String plaintext)`, `FitnessScorer.score(String)`, `Alphabet.keyspaceSize()`, `CaesarCipher(Alphabet, int)`
- Produces: `ParallelCaesarCracker(Alphabet alphabet, FitnessScorer scorer, TaskExecutor executor, ParallelPolicy policy) implements Cracker`

- [ ] **Step 1: Write the failing test**

```java
package ua.com.javarush.j4.crack;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.cipher.CaesarCipher;
import ua.com.javarush.j4.concurrent.DirectTaskExecutor;
import ua.com.javarush.j4.concurrent.PooledTaskExecutor;
import ua.com.javarush.j4.concurrent.ParallelPolicy;
import ua.com.javarush.j4.concurrent.TaskExecutor;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParallelCaesarCrackerTest {

    private static final Alphabet EN = Alphabets.ENGLISH;
    private static final LanguageProfile PROFILE = LanguageProfiles.ENGLISH;

    /** Long enough to clear MIN_CHARS_FOR_CRACK so the parallel path really engages. */
    private static String longEnglishText() {
        StringBuilder text = new StringBuilder();
        while (text.length() < ParallelPolicy.MIN_CHARS_FOR_CRACK * 2) {
            text.append("to be or not to be that is the question whether it is nobler in the mind ");
        }
        return text.toString();
    }

    private static String randomText(Random random, int length) {
        StringBuilder text = new StringBuilder(length);
        String words = "the quick brown fox jumps over a lazy dog and then it is time for us all ";
        while (text.length() < length) {
            text.append(words.charAt(random.nextInt(words.length())));
        }
        return text.toString();
    }

    @Test
    void matchesTheSequentialCrackerForEveryKey() {
        String plaintext = longEnglishText();
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cracker sequential = new CaesarCracker(EN, new DictionaryScorer(PROFILE));
            Cracker parallel = new ParallelCaesarCracker(
                    EN, new DictionaryScorer(PROFILE), executor, ParallelPolicy.of(4));

            for (int key = 0; key < EN.keyspaceSize(); key++) {
                String ciphertext = new CaesarCipher(EN, key).encrypt(plaintext);
                assertEquals(sequential.crack(ciphertext), parallel.crack(ciphertext),
                        "mismatch for key " + key);
            }
        }
    }

    @Test
    void matchesTheSequentialCrackerOnRandomTexts() {
        Random random = new Random(20260803L);
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cracker sequential = new CaesarCracker(EN, new FrequencyScorer(PROFILE));
            Cracker parallel = new ParallelCaesarCracker(
                    EN, new FrequencyScorer(PROFILE), executor, ParallelPolicy.of(4));

            for (int trial = 0; trial < 20; trial++) {
                String ciphertext = new CaesarCipher(EN, random.nextInt(26))
                        .encrypt(randomText(random, ParallelPolicy.MIN_CHARS_FOR_CRACK + 500));
                assertEquals(sequential.crack(ciphertext), parallel.crack(ciphertext),
                        "mismatch on trial " + trial);
            }
        }
    }

    @Test
    void breaksScoreTiesTowardTheLowestKey() {
        // A scorer that rates everything equally: the sequential loop's strict '>' keeps
        // key 0, and the parallel reduction must reach the same answer.
        FitnessScorer flat = text -> 1.0;
        String ciphertext = longEnglishText();
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cracker parallel = new ParallelCaesarCracker(EN, flat, executor, ParallelPolicy.of(4));

            assertEquals(0, parallel.crack(ciphertext).key());
        }
    }

    @Test
    void isStableAcrossRepeatedRuns() {
        String ciphertext = new CaesarCipher(EN, 7).encrypt(longEnglishText());
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cracker parallel = new ParallelCaesarCracker(
                    EN, new DictionaryScorer(PROFILE), executor, ParallelPolicy.of(4));
            CrackResult first = parallel.crack(ciphertext);

            for (int run = 0; run < 50; run++) {
                assertEquals(first, parallel.crack(ciphertext), "run " + run + " differed");
            }
        }
    }

    @Test
    void fallsBackToSequentialBelowTheThreshold() {
        String shortText = new CaesarCipher(EN, 3).encrypt("the quick brown fox");
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cracker sequential = new CaesarCracker(EN, new DictionaryScorer(PROFILE));
            Cracker parallel = new ParallelCaesarCracker(
                    EN, new DictionaryScorer(PROFILE), executor, ParallelPolicy.of(4));

            assertEquals(sequential.crack(shortText), parallel.crack(shortText));
        }
    }

    @Test
    void worksWithASingleThreadedExecutor() {
        String ciphertext = new CaesarCipher(EN, 11).encrypt(longEnglishText());
        try (TaskExecutor executor = new DirectTaskExecutor()) {
            Cracker sequential = new CaesarCracker(EN, new DictionaryScorer(PROFILE));
            Cracker parallel = new ParallelCaesarCracker(
                    EN, new DictionaryScorer(PROFILE), executor, ParallelPolicy.of(4));

            assertEquals(sequential.crack(ciphertext), parallel.crack(ciphertext));
        }
    }

    @Test
    void handlesMorePartitionsThanKeys() {
        String ciphertext = new CaesarCipher(EN, 5).encrypt(longEnglishText());
        try (TaskExecutor executor = new PooledTaskExecutor(64)) {
            Cracker sequential = new CaesarCracker(EN, new DictionaryScorer(PROFILE));
            Cracker parallel = new ParallelCaesarCracker(
                    EN, new DictionaryScorer(PROFILE), executor, ParallelPolicy.of(64));

            assertEquals(sequential.crack(ciphertext), parallel.crack(ciphertext));
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -Dtest='ParallelCaesarCrackerTest' test`
Expected: FAIL — `cannot find symbol: class ParallelCaesarCracker`

- [ ] **Step 3: Write the implementation**

```java
package ua.com.javarush.j4.crack;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.cipher.CaesarCipher;
import ua.com.javarush.j4.concurrent.ParallelPolicy;
import ua.com.javarush.j4.concurrent.TaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * The same sweep as {@link CaesarCracker}, with the keyspace split into contiguous
 * ranges scored in parallel.
 *
 * <p>Results are identical to the sequential sweep, tie-breaking included. Ranges are
 * contiguous and ascending, each range is scanned in ascending key order with a strict
 * {@code >}, and the partial winners are folded left to right with a strict {@code >}.
 * So the lowest key wins a tie inside a range, and the lowest range wins a tie across
 * ranges — exactly what a single ascending scan would do.
 *
 * <p>Ranges rather than one task per key: a task holds a full decrypted copy of the text,
 * so per-key tasks would keep the whole keyspace of copies alive at once.
 */
public final class ParallelCaesarCracker implements Cracker {
    private final Alphabet alphabet;
    private final FitnessScorer scorer;
    private final TaskExecutor executor;
    private final ParallelPolicy policy;
    private final Cracker sequential;

    public ParallelCaesarCracker(Alphabet alphabet, FitnessScorer scorer,
                                 TaskExecutor executor, ParallelPolicy policy) {
        this.alphabet = alphabet;
        this.scorer = scorer;
        this.executor = executor;
        this.policy = policy;
        this.sequential = new CaesarCracker(alphabet, scorer);
    }

    @Override
    public CrackResult crack(String ciphertext) {
        int keyspace = alphabet.keyspaceSize();
        int partitions = Math.min(keyspace, executor.parallelism());
        if (partitions <= 1 || !policy.shouldParallelizeCrack(ciphertext.length())) {
            return sequential.crack(ciphertext);
        }

        List<Callable<Candidate>> tasks = new ArrayList<>(partitions);
        for (int partition = 0; partition < partitions; partition++) {
            int from = (int) ((long) keyspace * partition / partitions);
            int to = (int) ((long) keyspace * (partition + 1) / partitions);
            tasks.add(() -> bestInRange(ciphertext, from, to));
        }

        Candidate best = null;
        for (Candidate candidate : executor.invokeAll(tasks)) {
            if (best == null || candidate.score() > best.score()) {
                best = candidate;
            }
        }
        return new CrackResult(best.key(), best.plaintext());
    }

    private Candidate bestInRange(String ciphertext, int fromInclusive, int toExclusive) {
        Candidate best = null;
        for (int key = fromInclusive; key < toExclusive; key++) {
            String plaintext = new CaesarCipher(alphabet, key).decrypt(ciphertext);
            double score = scorer.score(plaintext);
            if (best == null || score > best.score()) {
                best = new Candidate(key, plaintext, score);
            }
        }
        return best;
    }

    private record Candidate(int key, String plaintext, double score) {
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./mvnw -Dtest='ParallelCaesarCrackerTest' test`
Expected: PASS — 7 tests green

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/crack/ParallelCaesarCracker.java src/test/java/ua/com/javarush/j4/crack/ParallelCaesarCrackerTest.java
git commit -m "feat: add range-partitioned parallel Caesar cracker"
```

---

### Task 4: `PositionDependentCipher` and the Vigenère refactor

Caesar, ROT13 and Atbash transform each character independently, so their chunks can be processed in any order. Vigenère advances its key once per alphabet letter, so a chunk's result depends on how many letters precede it. This task adds the narrow interface that lets `ParallelCipher` handle that.

**Files:**
- Create: `src/main/java/ua/com/javarush/j4/cipher/PositionDependentCipher.java`
- Modify: `src/main/java/ua/com/javarush/j4/cipher/VigenereCipher.java`
- Test: `src/test/java/ua/com/javarush/j4/cipher/VigenereOffsetTest.java`

**Interfaces:**
- Consumes: `Alphabet.position(char) -> OptionalInt`, `Alphabet.shift(char, int)`
- Produces: `interface PositionDependentCipher` with `int alphabetLetterCount(String)`, `String encryptFrom(String, int)`, `String decryptFrom(String, int)`; `VigenereCipher implements Cipher, PositionDependentCipher`

- [ ] **Step 1: Write the failing test**

```java
package ua.com.javarush.j4.cipher;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.alphabet.Alphabets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VigenereOffsetTest {

    private static final Alphabet EN = Alphabets.ENGLISH;
    private static final String TEXT =
            "Attack at dawn, and hold the line until the second company arrives!";

    private static VigenereCipher cipher() {
        return new VigenereCipher(EN, "lemon");
    }

    @Test
    void offsetZeroMatchesPlainEncrypt() {
        assertEquals(cipher().encrypt(TEXT), cipher().encryptFrom(TEXT, 0));
    }

    @Test
    void splittingAtAnyPointReproducesTheWholeEncryption() {
        VigenereCipher cipher = cipher();
        String expected = cipher.encrypt(TEXT);

        for (int split = 0; split <= TEXT.length(); split++) {
            String head = TEXT.substring(0, split);
            String tail = TEXT.substring(split);
            String joined = cipher.encryptFrom(head, 0)
                    + cipher.encryptFrom(tail, cipher.alphabetLetterCount(head));

            assertEquals(expected, joined, "split at " + split);
        }
    }

    @Test
    void splittingAtAnyPointReproducesTheWholeDecryption() {
        VigenereCipher cipher = cipher();
        String ciphertext = cipher.encrypt(TEXT);

        for (int split = 0; split <= ciphertext.length(); split++) {
            String head = ciphertext.substring(0, split);
            String tail = ciphertext.substring(split);
            String joined = cipher.decryptFrom(head, 0)
                    + cipher.decryptFrom(tail, cipher.alphabetLetterCount(head));

            assertEquals(TEXT, joined, "split at " + split);
        }
    }

    @Test
    void countsOnlyAlphabetMembers() {
        assertEquals(0, cipher().alphabetLetterCount(", .!?123"));
        assertEquals(3, cipher().alphabetLetterCount("a, b. c!"));
        assertEquals(0, cipher().alphabetLetterCount(""));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -Dtest='VigenereOffsetTest' test`
Expected: FAIL — `cannot find symbol: method encryptFrom(String,int)`

- [ ] **Step 3: Create the interface**

```java
package ua.com.javarush.j4.cipher;

/**
 * A cipher whose result for a chunk of text depends on how many alphabet letters
 * precede that chunk.
 *
 * <p>Implemented only by ciphers that advance a key per letter — {@link VigenereCipher}
 * is the only one here. {@link ParallelCipher} uses it to transform chunks out of order
 * without changing the output.
 *
 * <p>Deliberately narrow, and deliberately separate from {@link Cipher}: ciphers that do
 * not need it are not forced to implement it. Compare with the widened {@code TextReader}
 * in {@code docs/SOLID.md}, which is kept as the counter-example.
 */
public interface PositionDependentCipher {

    /** How many characters of {@code chunk} advance the key. */
    int alphabetLetterCount(String chunk);

    /** Encrypts {@code chunk} as if {@code letterOffset} alphabet letters came before it. */
    String encryptFrom(String chunk, int letterOffset);

    /** Decrypts {@code chunk} as if {@code letterOffset} alphabet letters came before it. */
    String decryptFrom(String chunk, int letterOffset);
}
```

- [ ] **Step 4: Refactor `VigenereCipher`**

The only change to existing behaviour is that `process` takes a starting key index; `encrypt`/`decrypt` pass 0 and are unchanged in effect.

```java
package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.error.InvalidArgumentsException;

/** Polyalphabetic cipher: each enciphered letter is shifted by the next keyword letter. */
public final class VigenereCipher implements Cipher, PositionDependentCipher {
    private final Alphabet alphabet;
    private final String keyword;

    public VigenereCipher(Alphabet alphabet, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new InvalidArgumentsException("Vigenère cipher requires a non-empty --keyword");
        }
        this.alphabet = alphabet;
        this.keyword = keyword;
    }

    @Override
    public String encrypt(String text) {
        return process(text, 1, 0);
    }

    @Override
    public String decrypt(String text) {
        return process(text, -1, 0);
    }

    @Override
    public String encryptFrom(String chunk, int letterOffset) {
        return process(chunk, 1, letterOffset);
    }

    @Override
    public String decryptFrom(String chunk, int letterOffset) {
        return process(chunk, -1, letterOffset);
    }

    @Override
    public int alphabetLetterCount(String chunk) {
        int count = 0;
        for (int i = 0; i < chunk.length(); i++) {
            if (alphabet.position(chunk.charAt(i)).isPresent()) {
                count++;
            }
        }
        return count;
    }

    private String process(String text, int sign, int startLetterIndex) {
        StringBuilder out = new StringBuilder(text.length());
        int keyIndex = startLetterIndex;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (alphabet.position(c).isPresent()) {
                char keyChar = keyword.charAt(keyIndex % keyword.length());
                int shift = alphabet.position(keyChar).orElse(0) * sign;
                out.append(alphabet.shift(c, shift));
                keyIndex++;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./mvnw -Dtest='VigenereOffsetTest' test`
Expected: PASS — 4 tests green

- [ ] **Step 6: Run the full suite to confirm nothing regressed**

Run: `./mvnw test`
Expected: PASS — `Failures: 0`

- [ ] **Step 7: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/cipher/PositionDependentCipher.java src/main/java/ua/com/javarush/j4/cipher/VigenereCipher.java src/test/java/ua/com/javarush/j4/cipher/VigenereOffsetTest.java
git commit -m "feat: make Vigenere cipher offset-aware for chunked processing"
```

---

### Task 5: `ParallelCipher`

**Files:**
- Create: `src/main/java/ua/com/javarush/j4/cipher/ParallelCipher.java`
- Test: `src/test/java/ua/com/javarush/j4/cipher/ParallelCipherTest.java`

**Interfaces:**
- Consumes: `Cipher`, `PositionDependentCipher`, `TaskExecutor`, `ParallelPolicy`
- Produces: `ParallelCipher(Cipher delegate, TaskExecutor executor, ParallelPolicy policy) implements Cipher`

- [ ] **Step 1: Write the failing test**

```java
package ua.com.javarush.j4.cipher;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.concurrent.DirectTaskExecutor;
import ua.com.javarush.j4.concurrent.PooledTaskExecutor;
import ua.com.javarush.j4.concurrent.ParallelPolicy;
import ua.com.javarush.j4.concurrent.TaskExecutor;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParallelCipherTest {

    private static final Alphabet EN = Alphabets.ENGLISH;
    private static final Alphabet MIXED = Alphabets.DEFAULT;

    /** Comfortably past MIN_CHARS_FOR_TRANSFORM so the chunked path really engages. */
    private static String largeText(int minLength) {
        StringBuilder text = new StringBuilder(minLength + 128);
        while (text.length() < minLength) {
            text.append("Ukrainian та English mixed, з punctuation 123 — and newlines\n");
        }
        return text.toString();
    }

    private static List<Cipher> allCiphers() {
        return List.of(
                new CaesarCipher(MIXED, 7),
                new Rot13Cipher(MIXED),
                new AtbashCipher(MIXED),
                new VigenereCipher(MIXED, "ключ"));
    }

    @Test
    void matchesTheDelegateForEveryCipher() {
        String text = largeText(ParallelPolicy.MIN_CHARS_FOR_TRANSFORM * 3);
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            for (Cipher delegate : allCiphers()) {
                Cipher parallel = new ParallelCipher(delegate, executor, ParallelPolicy.of(4));

                assertEquals(delegate.encrypt(text), parallel.encrypt(text),
                        delegate.getClass().getSimpleName() + " encrypt mismatch");
                assertEquals(delegate.decrypt(text), parallel.decrypt(text),
                        delegate.getClass().getSimpleName() + " decrypt mismatch");
            }
        }
    }

    @Test
    void roundTripsThroughTheParallelPath() {
        String text = largeText(ParallelPolicy.MIN_CHARS_FOR_TRANSFORM * 2);
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            for (Cipher delegate : allCiphers()) {
                Cipher parallel = new ParallelCipher(delegate, executor, ParallelPolicy.of(4));

                assertEquals(text, parallel.decrypt(parallel.encrypt(text)),
                        delegate.getClass().getSimpleName() + " round trip failed");
            }
        }
    }

    @Test
    void handlesTextsThatAreNotAWholeNumberOfChunks() {
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cipher delegate = new VigenereCipher(EN, "lemon");
            Cipher parallel = new ParallelCipher(delegate, executor, ParallelPolicy.of(4));

            for (int extra : new int[]{0, 1, 7, 4_099}) {
                String text = largeText(ParallelPolicy.MIN_CHARS_FOR_TRANSFORM + extra);
                assertEquals(delegate.encrypt(text), parallel.encrypt(text), "extra=" + extra);
            }
        }
    }

    @Test
    void delegatesDirectlyBelowTheThreshold() {
        String small = "the quick brown fox";
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cipher delegate = new CaesarCipher(EN, 3);
            Cipher parallel = new ParallelCipher(delegate, executor, ParallelPolicy.of(4));

            assertEquals(delegate.encrypt(small), parallel.encrypt(small));
            assertEquals("", parallel.encrypt(""));
        }
    }

    @Test
    void delegatesDirectlyWithASingleThreadedExecutor() {
        String text = largeText(ParallelPolicy.MIN_CHARS_FOR_TRANSFORM * 2);
        try (TaskExecutor executor = new DirectTaskExecutor()) {
            Cipher delegate = new VigenereCipher(EN, "lemon");
            Cipher parallel = new ParallelCipher(delegate, executor, ParallelPolicy.of(4));

            assertEquals(delegate.encrypt(text), parallel.encrypt(text));
        }
    }

    @Test
    void neverSplitsASurrogatePair() {
        // Emoji are outside every alphabet ring, so they must pass through byte-for-byte.
        StringBuilder text = new StringBuilder();
        Random random = new Random(20260803L);
        while (text.length() < ParallelPolicy.MIN_CHARS_FOR_TRANSFORM * 2) {
            text.append("abc 😀 ").append(random.nextInt(10));
        }
        String withEmoji = text.toString();

        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cipher delegate = new CaesarCipher(EN, 5);
            Cipher parallel = new ParallelCipher(delegate, executor, ParallelPolicy.of(4));

            assertEquals(delegate.encrypt(withEmoji), parallel.encrypt(withEmoji));
        }
    }

    @Test
    void isStableAcrossRepeatedRuns() {
        String text = largeText(ParallelPolicy.MIN_CHARS_FOR_TRANSFORM * 2);
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cipher parallel = new ParallelCipher(
                    new VigenereCipher(MIXED, "ключ"), executor, ParallelPolicy.of(4));
            String first = parallel.encrypt(text);

            for (int run = 0; run < 50; run++) {
                assertEquals(first, parallel.encrypt(text), "run " + run + " differed");
            }
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -Dtest='ParallelCipherTest' test`
Expected: FAIL — `cannot find symbol: class ParallelCipher`

- [ ] **Step 3: Write the implementation**

```java
package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.concurrent.ParallelPolicy;
import ua.com.javarush.j4.concurrent.TaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Splits a large text into chunks, transforms them in parallel, and rejoins them in
 * order. Output is identical to the wrapped cipher's, character for character.
 *
 * <p>A position-dependent delegate (see {@link PositionDependentCipher}) needs two
 * phases: count the alphabet letters in each chunk, turn those counts into starting
 * offsets with a prefix sum, then transform each chunk from its own offset. Everything
 * else transforms in one phase, since each character is independent.
 */
public final class ParallelCipher implements Cipher {
    private static final int MIN_CHUNK_CHARS = 16_384;
    private static final int CHUNKS_PER_THREAD = 4;

    private final Cipher delegate;
    private final TaskExecutor executor;
    private final ParallelPolicy policy;

    public ParallelCipher(Cipher delegate, TaskExecutor executor, ParallelPolicy policy) {
        this.delegate = delegate;
        this.executor = executor;
        this.policy = policy;
    }

    @Override
    public String encrypt(String text) {
        return process(text, true);
    }

    @Override
    public String decrypt(String text) {
        return process(text, false);
    }

    private String process(String text, boolean encrypting) {
        if (executor.parallelism() <= 1 || !policy.shouldParallelizeTransform(text.length())) {
            return encrypting ? delegate.encrypt(text) : delegate.decrypt(text);
        }

        List<String> chunks = split(text);
        List<String> transformed = delegate instanceof PositionDependentCipher positional
                ? transformPositionDependent(chunks, positional, encrypting)
                : transformIndependent(chunks, encrypting);

        StringBuilder out = new StringBuilder(text.length());
        transformed.forEach(out::append);
        return out.toString();
    }

    private List<String> transformIndependent(List<String> chunks, boolean encrypting) {
        List<Callable<String>> tasks = new ArrayList<>(chunks.size());
        for (String chunk : chunks) {
            tasks.add(() -> encrypting ? delegate.encrypt(chunk) : delegate.decrypt(chunk));
        }
        return executor.invokeAll(tasks);
    }

    private List<String> transformPositionDependent(List<String> chunks,
                                                    PositionDependentCipher cipher,
                                                    boolean encrypting) {
        List<Callable<Integer>> counters = new ArrayList<>(chunks.size());
        for (String chunk : chunks) {
            counters.add(() -> cipher.alphabetLetterCount(chunk));
        }
        List<Integer> counts = executor.invokeAll(counters);

        int[] offsets = new int[chunks.size()];
        int running = 0;
        for (int i = 0; i < chunks.size(); i++) {
            offsets[i] = running;
            running += counts.get(i);
        }

        List<Callable<String>> tasks = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            int offset = offsets[i];
            tasks.add(() -> encrypting
                    ? cipher.encryptFrom(chunk, offset)
                    : cipher.decryptFrom(chunk, offset));
        }
        return executor.invokeAll(tasks);
    }

    /**
     * Splits into roughly equal chunks, never inside a surrogate pair. The alphabets
     * shipped here are all BMP, so the surrogate check never fires in practice — it keeps
     * the splitter correct for arbitrary input.
     */
    private List<String> split(String text) {
        int target = Math.max(MIN_CHUNK_CHARS,
                text.length() / (executor.parallelism() * CHUNKS_PER_THREAD) + 1);

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + target);
            if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) {
                end++;
            }
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./mvnw -Dtest='ParallelCipherTest' test`
Expected: PASS — 7 tests green

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/cipher/ParallelCipher.java src/test/java/ua/com/javarush/j4/cipher/ParallelCipherTest.java
git commit -m "feat: add chunking ParallelCipher decorator"
```

---

### Task 6: Wire the cracker and cipher into `CryptoService`, add `--threads`

**Files:**
- Modify: `src/main/java/ua/com/javarush/j4/app/command/BruteForceCommand.java`
- Modify: `src/main/java/ua/com/javarush/j4/app/CryptoService.java`
- Modify: `src/main/java/ua/com/javarush/j4/cli/CryptoCli.java:53-59`
- Test: `src/test/java/ua/com/javarush/j4/app/CryptoServiceThreadingTest.java`

**Interfaces:**
- Consumes: `ParallelCaesarCracker`, `ParallelCipher`, `ParallelPolicy`, `LazyPooledTaskExecutor`, `DirectTaskExecutor`
- Produces:
  - `CryptoService implements AutoCloseable`, `CryptoService()` (all cores), `CryptoService(ParallelPolicy)`
  - `BruteForceCommand(Path, LanguageDetector, LanguageProfile, String, TextReaders, TextWriter, OutputNaming, TaskExecutor, ParallelPolicy)`

- [ ] **Step 1: Write the failing test**

```java
package ua.com.javarush.j4.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.com.javarush.j4.concurrent.ParallelPolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CryptoServiceThreadingTest {

    private static String largeEnglishText() {
        StringBuilder text = new StringBuilder();
        while (text.length() < ParallelPolicy.MIN_CHARS_FOR_TRANSFORM * 2) {
            text.append("to be or not to be that is the question whether it is nobler\n");
        }
        return text.toString();
    }

    private static Path write(Path dir, String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private static String encryptWith(Path dir, int threads, String content) throws IOException {
        Path input = write(dir, "in-" + threads + ".txt", content);
        try (CryptoService service = new CryptoService(ParallelPolicy.of(threads))) {
            Path output = service.execute(new CryptoRequest(
                    Operation.ENCRYPT, input, 5, "caesar", null, "default", "dictionary"));
            return Files.readString(output, StandardCharsets.UTF_8);
        }
    }

    @Test
    void encryptionIsIdenticalWhateverTheThreadCount() throws IOException {
        Path dir = Files.createTempDirectory("threading");
        String text = largeEnglishText();

        assertEquals(encryptWith(dir, 1, text), encryptWith(dir, 8, text));
    }

    @Test
    void bruteForceIsIdenticalWhateverTheThreadCount(@TempDir Path dir) throws IOException {
        String plaintext = largeEnglishText();
        Path sequentialInput = write(dir, "seq.txt", plaintext);
        Path parallelInput = write(dir, "par.txt", plaintext);

        String sequential = crackRoundTrip(sequentialInput, 1);
        String parallel = crackRoundTrip(parallelInput, 8);

        assertEquals(plaintext, sequential);
        assertEquals(sequential, parallel);
    }

    private static String crackRoundTrip(Path input, int threads) throws IOException {
        try (CryptoService service = new CryptoService(ParallelPolicy.of(threads))) {
            Path encrypted = service.execute(new CryptoRequest(
                    Operation.ENCRYPT, input, 5, "caesar", null, "en", "dictionary"));
            Path cracked = service.execute(new CryptoRequest(
                    Operation.BRUTE_FORCE, encrypted, null, "caesar", null, "en", "dictionary"));
            return Files.readString(cracked, StandardCharsets.UTF_8);
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -Dtest='CryptoServiceThreadingTest' test`
Expected: FAIL — `CryptoService(ParallelPolicy)` constructor not found; `CryptoService` is not `AutoCloseable`

- [ ] **Step 3: Give `BruteForceCommand` an executor**

Change only the constructor and the cracker construction; `transform`'s language-detection comment and `outputPath` stay as they are.

```java
package ua.com.javarush.j4.app.command;

import ua.com.javarush.j4.concurrent.ParallelPolicy;
import ua.com.javarush.j4.concurrent.TaskExecutor;
import ua.com.javarush.j4.crack.CrackResult;
import ua.com.javarush.j4.crack.Cracker;
import ua.com.javarush.j4.crack.DictionaryScorer;
import ua.com.javarush.j4.crack.FitnessScorer;
import ua.com.javarush.j4.crack.FrequencyScorer;
import ua.com.javarush.j4.crack.LanguageDetector;
import ua.com.javarush.j4.crack.LanguageProfile;
import ua.com.javarush.j4.crack.ParallelCaesarCracker;
import ua.com.javarush.j4.error.InvalidArgumentsException;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.nio.file.Path;

/** Brute-force: detect language (unless one is forced), sweep keys, write best decryption. */
public final class BruteForceCommand extends CryptoCommand {
    private final LanguageDetector detector;
    private final LanguageProfile forcedProfile; // null => auto-detect
    private final String scorerName;
    private final TaskExecutor executor;
    private final ParallelPolicy policy;

    public BruteForceCommand(Path input, LanguageDetector detector, LanguageProfile forcedProfile,
                             String scorerName, TextReaders readers, TextWriter writer,
                             OutputNaming naming, TaskExecutor executor, ParallelPolicy policy) {
        super(input, readers, writer, naming);
        this.detector = detector;
        this.forcedProfile = forcedProfile;
        this.scorerName = scorerName;
        this.executor = executor;
        this.policy = policy;
    }

    @Override
    protected String transform(String text) {
        /*
         * Language detection runs on the ciphertext, not plaintext. This is reliable
         * for distinguishing scripts (Latin vs Cyrillic) because a Caesar shift stays
         * within an alphabet ring and leaves the script unchanged. For two same-script
         * languages (e.g. Ukrainian vs Russian), detection relies on distinctive-letter
         * frequencies surviving the shift and has been validated for the shipped fixtures
         * (Hamlet EN, Orwell UA). For ambiguous real-world input, prefer passing an
         * explicit --alphabet flag to force a profile rather than relying on auto-detect.
         */
        LanguageProfile profile = forcedProfile != null ? forcedProfile : detector.detect(text);
        FitnessScorer scorer = buildScorer(scorerName, profile);
        Cracker cracker = new ParallelCaesarCracker(profile.alphabet(), scorer, executor, policy);
        CrackResult result = cracker.crack(text);
        return result.plaintext();
    }

    private static FitnessScorer buildScorer(String name, LanguageProfile profile) {
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "dictionary" -> new DictionaryScorer(profile);
            case "frequency"  -> new FrequencyScorer(profile);
            default -> throw new InvalidArgumentsException(
                    "Unknown scorer '" + name + "'. Valid values: dictionary, frequency");
        };
    }

    @Override
    protected Path outputPath(OutputNaming naming, Path input) {
        return naming.forDecrypt(input);
    }
}
```

- [ ] **Step 4: Make `CryptoService` own the executor**

Keep the existing Ukrainian SOLID comment block above the class exactly as it is; add to it rather than replacing it.

```java
package ua.com.javarush.j4.app;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.app.command.BruteForceCommand;
import ua.com.javarush.j4.app.command.CryptoCommand;
import ua.com.javarush.j4.app.command.DecryptCommand;
import ua.com.javarush.j4.app.command.EncryptCommand;
import ua.com.javarush.j4.cipher.Cipher;
import ua.com.javarush.j4.cipher.CipherFactory;
import ua.com.javarush.j4.cipher.ParallelCipher;
import ua.com.javarush.j4.concurrent.DirectTaskExecutor;
import ua.com.javarush.j4.concurrent.LazyPooledTaskExecutor;
import ua.com.javarush.j4.concurrent.ParallelPolicy;
import ua.com.javarush.j4.concurrent.TaskExecutor;
import ua.com.javarush.j4.crack.LanguageDetector;
import ua.com.javarush.j4.crack.LanguageProfile;
import ua.com.javarush.j4.crack.LanguageProfiles;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/** Facade: turns a CryptoRequest into the right command and runs it. */
// ── SOLID ▸ D — Принцип інверсії залежностей (DIP): «корінь композиції» ──
// Саме тут абстракції «зшиваються» з конкретними реалізаціями: сервіс створює
// CipherFactory, TextReaders, TextWriter, LanguageDetector і ВПРОВАДЖУЄ їх у
// команди через конструктори. Завдяки цьому команди й Cracker залишаються
// залежними лише від інтерфейсів, а всі рішення «що з чим з'єднати» зібрані в
// одному місці. Клас також є Фасадом (SRP: єдиний обов'язок — оркеструвати запит).
//
// TaskExecutor впроваджується так само, як і решта залежностей: команди не знають,
// виконуються вони послідовно чи на пулі потоків.
public final class CryptoService implements AutoCloseable {
    private final TextReaders readers = new TextReaders();
    private final TextWriter writer = new TextWriter();
    private final OutputNaming naming = new OutputNaming();
    private final CipherFactory ciphers = new CipherFactory();
    private final LanguageDetector detector = new LanguageDetector();
    private final ParallelPolicy policy;
    private final TaskExecutor executor;

    public CryptoService() {
        this(ParallelPolicy.of(0));
    }

    public CryptoService(ParallelPolicy policy) {
        this.policy = policy;
        this.executor = policy.threads() > 1
                ? new LazyPooledTaskExecutor(policy.threads())
                : new DirectTaskExecutor();
    }

    public Path execute(CryptoRequest request) throws IOException {
        return command(request, executor).execute();
    }

    @Override
    public void close() {
        executor.close();
    }

    private CryptoCommand command(CryptoRequest request, TaskExecutor taskExecutor) {
        Path file = request.file();
        return switch (request.operation()) {
            case ENCRYPT -> new EncryptCommand(file, cipher(request, taskExecutor), readers, writer, naming);
            case DECRYPT -> new DecryptCommand(file, cipher(request, taskExecutor), readers, writer, naming);
            case BRUTE_FORCE -> new BruteForceCommand(
                    file, detector, forcedProfile(request.alphabetName()),
                    request.scorerName(), readers, writer, naming, taskExecutor, policy);
        };
    }

    private Cipher cipher(CryptoRequest request, TaskExecutor taskExecutor) {
        Alphabet alphabet = Alphabets.byName(request.alphabetName());
        Cipher cipher = ciphers.create(request.cipherName(), alphabet, request.key(), request.keyword());
        return new ParallelCipher(cipher, taskExecutor, policy);
    }

    /** For brute force: a named language forces its profile; "default"/"auto" means auto-detect. */
    private LanguageProfile forcedProfile(String alphabetName) {
        return switch (alphabetName.toLowerCase(Locale.ROOT)) {
            case "en", "english" -> LanguageProfiles.ENGLISH;
            case "ua", "ukrainian" -> LanguageProfiles.UKRAINIAN;
            case "ru", "russian" -> LanguageProfiles.RUSSIAN;
            default -> null;
        };
    }
}
```

- [ ] **Step 5: Add `--threads` to the CLI**

Add the option field after the existing `-s/--scorer` field, and wrap the service in try-with-resources.

```java
    @Option(names = "--threads", defaultValue = "0",
            description = "Worker threads (0 = every available core, 1 = fully sequential)")
    private int threads;

    @Override
    public Integer call() throws Exception {
        Operation operation = selectedOperation();
        try (CryptoService service = new CryptoService(ParallelPolicy.of(threads))) {
            service.execute(new CryptoRequest(operation, file, key, cipher, keyword, alphabet, scorer));
        }
        return 0;
    }
```

Add the import `ua.com.javarush.j4.concurrent.ParallelPolicy;`.

- [ ] **Step 6: Run the new test**

Run: `./mvnw -Dtest='CryptoServiceThreadingTest' test`
Expected: PASS — 2 tests green

- [ ] **Step 7: Run the full suite, including the Ukrainian cases**

Run: `./mvnw test && ./mvnw test -Dtest.lang.ua=true`
Expected: PASS both times. The second run reports 0 skipped.

If `CryptoServiceTest` fails to compile, it is constructing `CryptoService` directly — the no-arg constructor still exists, so only a missing `close()` would break it. Wrap its service in try-with-resources.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/app src/main/java/ua/com/javarush/j4/cli/CryptoCli.java src/test/java/ua/com/javarush/j4/app/CryptoServiceThreadingTest.java
git commit -m "feat: wire parallel cracker and cipher into CryptoService with --threads"
```

---

### Task 7: Batch mode

**Files:**
- Create: `src/main/java/ua/com/javarush/j4/app/batch/FileOutcome.java`
- Create: `src/main/java/ua/com/javarush/j4/app/batch/BatchReport.java`
- Create: `src/main/java/ua/com/javarush/j4/app/batch/BatchProcessor.java`
- Create: `src/main/java/ua/com/javarush/j4/cli/PathExpander.java`
- Modify: `src/main/java/ua/com/javarush/j4/app/CryptoRequest.java`
- Modify: `src/main/java/ua/com/javarush/j4/app/CryptoService.java`
- Modify: `src/main/java/ua/com/javarush/j4/cli/CryptoCli.java`
- Test: `src/test/java/ua/com/javarush/j4/app/batch/BatchProcessorTest.java`
- Test: `src/test/java/ua/com/javarush/j4/cli/PathExpanderTest.java`

**Interfaces:**
- Consumes: `TaskExecutor`, `CryptoCommand.execute() throws IOException`, `CryptoRequest`
- Produces:
  - `record FileOutcome(Path input, Path output, Throwable failure)` with `static FileOutcome success(Path, Path)`, `static FileOutcome failed(Path, Throwable)`, `boolean succeeded()`
  - `record BatchReport(List<FileOutcome> outcomes)` with `int succeeded()`, `int failed()`, `boolean anyFailed()`, `void printTo(PrintWriter)`
  - `BatchProcessor(TaskExecutor)` with `BatchReport process(List<Path>, Function<Path, CryptoCommand>)`
  - `PathExpander.expand(List<Path>) -> List<Path>`
  - `CryptoRequest.withFile(Path)`
  - `CryptoService.executeAll(CryptoRequest template, List<Path> files) -> BatchReport`

- [ ] **Step 1: Write the failing tests**

`src/test/java/ua/com/javarush/j4/app/batch/BatchProcessorTest.java`:

```java
package ua.com.javarush.j4.app.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.com.javarush.j4.app.command.CryptoCommand;
import ua.com.javarush.j4.cipher.CaesarCipher;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.app.command.EncryptCommand;
import ua.com.javarush.j4.concurrent.PooledTaskExecutor;
import ua.com.javarush.j4.concurrent.TaskExecutor;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchProcessorTest {

    private static CryptoCommand encryptCommand(Path input) {
        return new EncryptCommand(input, new CaesarCipher(Alphabets.DEFAULT, 3),
                new TextReaders(), new TextWriter(), new OutputNaming());
    }

    @Test
    void processesEveryFileAndReportsInInputOrder(@TempDir Path dir) throws IOException {
        List<Path> inputs = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Path file = dir.resolve("file" + i + ".txt");
            Files.writeString(file, "hello world " + i, StandardCharsets.UTF_8);
            inputs.add(file);
        }

        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            BatchReport report = new BatchProcessor(executor)
                    .process(inputs, BatchProcessorTest::encryptCommand);

            assertEquals(6, report.succeeded());
            assertEquals(0, report.failed());
            assertFalse(report.anyFailed());
            assertEquals(inputs, report.outcomes().stream().map(FileOutcome::input).toList());
        }
    }

    @Test
    void oneFailureDoesNotAbortTheRestOfTheBatch(@TempDir Path dir) throws IOException {
        Path good = dir.resolve("good.txt");
        Files.writeString(good, "hello", StandardCharsets.UTF_8);
        Path missing = dir.resolve("missing.txt");
        Path alsoGood = dir.resolve("also-good.txt");
        Files.writeString(alsoGood, "world", StandardCharsets.UTF_8);

        List<Path> inputs = List.of(good, missing, alsoGood);
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            BatchReport report = new BatchProcessor(executor)
                    .process(inputs, BatchProcessorTest::encryptCommand);

            assertEquals(2, report.succeeded());
            assertEquals(1, report.failed());
            assertTrue(report.anyFailed());
            assertEquals(inputs, report.outcomes().stream().map(FileOutcome::input).toList());
            assertFalse(report.outcomes().get(1).succeeded());
            assertTrue(report.outcomes().get(0).succeeded());
            assertTrue(report.outcomes().get(2).succeeded());
        }
    }

    @Test
    void writesAnOutputFilePerInput(@TempDir Path dir) throws IOException {
        Path input = dir.resolve("note.txt");
        Files.writeString(input, "abc", StandardCharsets.UTF_8);

        try (TaskExecutor executor = new PooledTaskExecutor(2)) {
            BatchReport report = new BatchProcessor(executor)
                    .process(List.of(input), BatchProcessorTest::encryptCommand);

            Path output = report.outcomes().get(0).output();
            assertEquals("note [ENCRYPTED].txt", output.getFileName().toString());
            assertEquals("def", Files.readString(output, StandardCharsets.UTF_8));
        }
    }
}
```

`src/test/java/ua/com/javarush/j4/cli/PathExpanderTest.java`:

```java
package ua.com.javarush.j4.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import ua.com.javarush.j4.error.InvalidArgumentsException;

class PathExpanderTest {

    @Test
    void aLiteralPathPassesThroughUntouched(@TempDir Path dir) {
        Path missing = dir.resolve("nope.txt");

        assertEquals(List.of(missing), new PathExpander().expand(List.of(missing)));
    }

    @Test
    void aGlobExpandsInSortedOrder(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("b.txt"), "b", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("a.txt"), "a", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("c.md"), "c", StandardCharsets.UTF_8);

        List<Path> expanded = new PathExpander().expand(List.of(dir.resolve("*.txt")));

        assertEquals(List.of(dir.resolve("a.txt"), dir.resolve("b.txt")), expanded);
    }

    @Test
    void repeatedEntriesAreConcatenated(@TempDir Path dir) throws IOException {
        Path one = dir.resolve("one.txt");
        Path two = dir.resolve("two.txt");
        Files.writeString(one, "1", StandardCharsets.UTF_8);
        Files.writeString(two, "2", StandardCharsets.UTF_8);

        assertEquals(List.of(one, two), new PathExpander().expand(List.of(one, two)));
    }

    @Test
    void aGlobMatchingNothingIsRejected(@TempDir Path dir) {
        InvalidArgumentsException thrown = assertThrows(InvalidArgumentsException.class,
                () -> new PathExpander().expand(List.of(dir.resolve("*.nomatch"))));

        assertTrue(thrown.getMessage().contains("*.nomatch"));
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./mvnw -Dtest='BatchProcessorTest,PathExpanderTest' test`
Expected: FAIL — `package ua.com.javarush.j4.app.batch does not exist`

- [ ] **Step 3: Create `FileOutcome`**

```java
package ua.com.javarush.j4.app.batch;

import java.nio.file.Path;

/** What happened to one file in a batch: an output path, or the failure that stopped it. */
public record FileOutcome(Path input, Path output, Throwable failure) {

    public static FileOutcome success(Path input, Path output) {
        return new FileOutcome(input, output, null);
    }

    public static FileOutcome failed(Path input, Throwable failure) {
        return new FileOutcome(input, null, failure);
    }

    public boolean succeeded() {
        return failure == null;
    }
}
```

- [ ] **Step 4: Create `BatchReport`**

```java
package ua.com.javarush.j4.app.batch;

import java.io.PrintWriter;
import java.util.List;

/** Every outcome of a batch run, in input order. */
public record BatchReport(List<FileOutcome> outcomes) {

    public BatchReport {
        outcomes = List.copyOf(outcomes);
    }

    public int succeeded() {
        return (int) outcomes.stream().filter(FileOutcome::succeeded).count();
    }

    public int failed() {
        return outcomes.size() - succeeded();
    }

    public boolean anyFailed() {
        return failed() > 0;
    }

    /** Prints one line per file in input order, then a summary. */
    public void printTo(PrintWriter out) {
        for (FileOutcome outcome : outcomes) {
            if (outcome.succeeded()) {
                out.println("  OK      " + outcome.input() + " -> " + outcome.output());
            } else {
                out.println("  FAILED  " + outcome.input() + ": " + outcome.failure().getMessage());
            }
        }
        out.println(succeeded() + " succeeded, " + failed() + " failed");
        out.flush();
    }
}
```

- [ ] **Step 5: Create `BatchProcessor`**

```java
package ua.com.javarush.j4.app.batch;

import ua.com.javarush.j4.app.command.CryptoCommand;
import ua.com.javarush.j4.concurrent.TaskExecutor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Runs one command per input file across the executor.
 *
 * <p>Each task captures its own failure instead of throwing, so a single unreadable file
 * never aborts the batch. Because {@link TaskExecutor#invokeAll} preserves submission
 * order, outcomes come back in input order however the work interleaves.
 */
public final class BatchProcessor {
    private final TaskExecutor executor;

    public BatchProcessor(TaskExecutor executor) {
        this.executor = executor;
    }

    public BatchReport process(List<Path> inputs, Function<Path, CryptoCommand> commandFactory) {
        List<Callable<FileOutcome>> tasks = new ArrayList<>(inputs.size());
        for (Path input : inputs) {
            tasks.add(() -> runOne(input, commandFactory));
        }
        return new BatchReport(executor.invokeAll(tasks));
    }

    private static FileOutcome runOne(Path input, Function<Path, CryptoCommand> commandFactory) {
        try {
            return FileOutcome.success(input, commandFactory.apply(input).execute());
        } catch (Exception e) {
            return FileOutcome.failed(input, e);
        }
    }
}
```

- [ ] **Step 6: Create `PathExpander`**

```java
package ua.com.javarush.j4.cli;

import ua.com.javarush.j4.error.InvalidArgumentsException;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns the raw {@code -f} values into concrete paths.
 *
 * <p>A literal path passes through untouched, including one that does not exist — that
 * failure belongs to the reader, which reports it properly. A value whose file name holds
 * {@code *} or {@code ?} is matched inside its parent directory and sorted, so batch
 * output order never depends on how the filesystem enumerates entries.
 */
public final class PathExpander {

    public List<Path> expand(List<Path> raw) {
        List<Path> expanded = new ArrayList<>();
        for (Path candidate : raw) {
            if (isGlob(candidate)) {
                expanded.addAll(matches(candidate));
            } else {
                expanded.add(candidate);
            }
        }
        return expanded;
    }

    private static boolean isGlob(Path candidate) {
        String name = candidate.getFileName().toString();
        return name.indexOf('*') >= 0 || name.indexOf('?') >= 0;
    }

    private static List<Path> matches(Path pattern) {
        Path directory = pattern.getParent() != null ? pattern.getParent() : Path.of(".");
        String glob = pattern.getFileName().toString();

        List<Path> found = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, glob)) {
            stream.forEach(found::add);
        } catch (IOException e) {
            throw new InvalidArgumentsException("Cannot expand pattern '" + pattern + "': " + e.getMessage());
        }
        if (found.isEmpty()) {
            throw new InvalidArgumentsException("Pattern '" + pattern + "' matched no files");
        }
        found.sort(Comparator.comparing(Path::toString));
        return found;
    }
}
```

- [ ] **Step 7: Add `withFile` to `CryptoRequest`**

```java
package ua.com.javarush.j4.app;

import java.nio.file.Path;

/** A fully-parsed user request, independent of how it was parsed. */
public record CryptoRequest(
        Operation operation,
        Path file,
        Integer key,
        String cipherName,
        String keyword,
        String alphabetName,
        String scorerName) {

    /** The same request pointed at a different file — used to fan a batch out. */
    public CryptoRequest withFile(Path other) {
        return new CryptoRequest(operation, other, key, cipherName, keyword, alphabetName, scorerName);
    }
}
```

- [ ] **Step 8: Add `executeAll` to `CryptoService`**

Add these imports: `ua.com.javarush.j4.app.batch.BatchProcessor`, `ua.com.javarush.j4.app.batch.BatchReport`, `java.util.List`. Then add the method next to `execute`:

```java
    /**
     * Runs the same request over several files. Fan-out happens here and nowhere else:
     * each file's command gets a same-thread executor, so cracking and chunking inside it
     * stay sequential and the pool is never oversubscribed.
     */
    public BatchReport executeAll(CryptoRequest template, List<Path> files) {
        TaskExecutor batchExecutor = policy.shouldParallelizeBatch(files.size())
                ? executor
                : new DirectTaskExecutor();
        TaskExecutor perFileExecutor = new DirectTaskExecutor();

        return new BatchProcessor(batchExecutor)
                .process(files, file -> command(template.withFile(file), perFileExecutor));
    }
```

- [ ] **Step 9: Make the CLI accept several files**

Replace the `-f` field and `call()`. Picocli collects repeated occurrences of an option into a `List`, so `-f a.txt` still yields a one-element list and the existing single-file contract is untouched.

```java
    @Option(names = "-f", required = true,
            description = "Input file path. Repeatable; a quoted glob such as '*.txt' is expanded.")
    private List<Path> files;

    @Override
    public Integer call() throws Exception {
        Operation operation = selectedOperation();
        List<Path> resolved = new PathExpander().expand(files);
        CryptoRequest template = new CryptoRequest(
                operation, resolved.get(0), key, cipher, keyword, alphabet, scorer);

        try (CryptoService service = new CryptoService(ParallelPolicy.of(threads))) {
            if (resolved.size() == 1) {
                service.execute(template);
                return 0;
            }
            BatchReport report = service.executeAll(template, resolved);
            report.printTo(spec.commandLine().getOut());
            return report.anyFailed() ? 1 : 0;
        }
    }
```

Add these imports: `java.util.List`, `ua.com.javarush.j4.app.batch.BatchReport`, `picocli.CommandLine.Model.CommandSpec`, `picocli.CommandLine.Spec`. Add the spec field next to the other options so `printTo` writes to picocli's configured stream:

```java
    @Spec
    private CommandSpec spec;
```

- [ ] **Step 10: Run the new tests**

Run: `./mvnw -Dtest='BatchProcessorTest,PathExpanderTest' test`
Expected: PASS — 7 tests green

- [ ] **Step 11: Run the full suite**

Run: `./mvnw test`
Expected: PASS — `Failures: 0`. `MainTest$ValidationTests` still passes: a non-existent literal path is not a glob, so it flows through to the reader and fails exactly as before.

- [ ] **Step 12: Verify batch mode by hand**

```bash
./mvnw -q package
mkdir -p /tmp/batch-check && printf 'hello world\n' > /tmp/batch-check/a.txt && printf 'second file\n' > /tmp/batch-check/b.txt
java -jar target/J4-M1-FP-1.0-SNAPSHOT.jar -e -k 3 -f '/tmp/batch-check/*.txt'
ls /tmp/batch-check/
```

Expected: report lists `a.txt` then `b.txt`, prints `2 succeeded, 0 failed`, and the directory holds `a [ENCRYPTED].txt` and `b [ENCRYPTED].txt`.

- [ ] **Step 13: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/app src/main/java/ua/com/javarush/j4/cli src/test/java/ua/com/javarush/j4/app/batch src/test/java/ua/com/javarush/j4/cli/PathExpanderTest.java
git commit -m "feat: add concurrent batch file processing with glob expansion"
```

---

### Task 8: Documentation and final verification

**Files:**
- Modify: `CLAUDE.md`
- Modify: `ARCHITECTURE.md` (only if it exists — check first with `ls ARCHITECTURE.md`)

**Interfaces:**
- Consumes: everything built above
- Produces: nothing code-facing

- [ ] **Step 1: Update the architecture section of `CLAUDE.md`**

Add these two bullets to the existing package list, after the `io/` entry:

```markdown
- `concurrent/` — `TaskExecutor` seam (`DirectTaskExecutor`, `PooledTaskExecutor`, `LazyPooledTaskExecutor`) + `ParallelPolicy` thresholds.
- `app/batch/` — `BatchProcessor` + `BatchReport`/`FileOutcome` for concurrent multi-file runs.
```

- [ ] **Step 2: Document the concurrency model in `CLAUDE.md`**

Add this section immediately after the "Cipher model — important" section:

```markdown
## Concurrency model — important

Three workloads run in parallel: the brute-force key sweep (`ParallelCaesarCracker`),
large-file cipher transforms (`ParallelCipher`), and batch multi-file runs
(`BatchProcessor`). All of them go through the `TaskExecutor` seam injected at
`CryptoService`.

Two rules keep this predictable:

1. **Each component self-gates on input size** via `ParallelPolicy` and falls back to its
   sequential algorithm below threshold (8192 chars to crack, 65536 to chunk, 2 files to
   batch). The shipped test fixtures are all well under these, so `MainTest` always takes
   the sequential path and `LazyPooledTaskExecutor` never starts a thread.
2. **Fan-out happens at exactly one level** — the outermost stage with enough work. Batch
   runs hand each file's command a `DirectTaskExecutor`, so inner cracking stays
   sequential and the pool is never oversubscribed.

Output is byte-identical whatever `--threads` is set to. The brute-force sweep matches the
sequential tie-break rule (lowest key wins an equal score) because keyspace ranges are
contiguous and ascending and `TaskExecutor.invokeAll` returns results in submission order.

Use `--threads 1` to force fully sequential execution.
```

- [ ] **Step 3: Document the new CLI flags in `CLAUDE.md`**

Update the CLI convention block to show the additions:

```markdown
-f <path>          file path (repeatable; a quoted glob like '*.txt' is expanded)
--threads <int>    worker threads (0 = every core, 1 = fully sequential)
```

- [ ] **Step 4: Run the complete verification**

```bash
./mvnw test
./mvnw test -Dtest.lang.ua=true
./mvnw package
```

Expected: all three succeed. The second reports `Skipped: 0`. Record the actual test counts — do not claim success without reading the output.

- [ ] **Step 5: Confirm no threads leak**

```bash
java -jar target/J4-M1-FP-1.0-SNAPSHOT.jar -e -k 5 -f src/main/resources/input.txt && echo "exited cleanly"
```

Expected: the command returns promptly and prints `exited cleanly`. A hung process means a pool was left running.

- [ ] **Step 6: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: document the concurrency model and new CLI flags"
```

---

## Self-Review

**Spec coverage.** Walked each spec section against the plan:

| Spec section | Task |
|---|---|
| Baseline restoration | 0 |
| `TaskExecutor` + three implementations | 1 |
| `ConcurrentExecutionException` | 1 |
| `ParallelPolicy` thresholds | 2 |
| `ParallelCaesarCracker` + determinism | 3 |
| `PositionDependentCipher` + Vigenère | 4 |
| `ParallelCipher` + surrogate safety | 5 |
| Lifecycle, `AutoCloseable`, `--threads` | 6 |
| Batch, glob, per-file isolation | 7 |
| Docs + verification | 8 |

No gaps.

**Placeholder scan.** No TBD/TODO, no "add error handling", no "similar to Task N". Every code step carries complete source.

**Type consistency.** Checked across tasks: `TaskExecutor.invokeAll`/`parallelism`/`close` used identically in Tasks 3, 5, 7. `ParallelPolicy.shouldParallelizeCrack`/`shouldParallelizeTransform`/`shouldParallelizeBatch` match their definitions in Task 2. `CrackResult(int key, String plaintext)` and `CryptoRequest`'s seven components match the existing records. `FileOutcome.success`/`failed` are static factories named distinctly from the instance `succeeded()`, so there is no overload clash.

**One correctness note carried into Task 3:** `bestInRange` can only return `null` if given an empty range, which `partitions = min(keyspace, parallelism)` makes impossible — every range gets at least one key.
