# Concurrency layer — design

**Date:** 2026-08-03
**Status:** approved, ready for implementation planning

## Context

The cryptanalyzer is single-threaded end to end. Every path reads a whole file into a
`String`, transforms it, and writes the result. Three workloads are worth parallelising:
the brute-force key sweep, the cipher transform of a large file, and processing many
files at once (which does not exist yet).

The domain layer is already shaped for this. `Alphabet`, `CharacterRing`, all four
ciphers, both scorers and `LanguageProfile` are immutable and stateless after
construction, so they can be shared across threads with no locking.

### Baseline

`./mvnw test` reports **88 tests, 30 failures, 10 skipped**. Every failure traces to
`Main.main` having been left as a reflection scratchpad with `new CryptoCli().run(args)`
commented out. Unit tests pass; only the end-to-end tests fail. Restoring that line is
step 0 and must produce a green baseline before any concurrency work begins.

### Constraints

- **Java 17.** No virtual threads, no structured concurrency, no `ExecutorService`
  implementing `AutoCloseable`. Plain `ExecutorService`/`ForkJoinPool`.
- **`MainTest` is the authoritative contract** and must stay green and untouched.
- **Test fixtures are tiny** — `hamlet.txt` is 838 B, `orwell.txt` is 4 KB, `test.txt`
  is 12 B. `MainTest` calls `Main.main(...)` fresh for each test.

That last constraint drives the central design decision: naive parallelism would add
pool-startup cost and non-determinism to all 88 existing tests while speeding up nothing.

## Goals

1. Parallel brute-force key sweep.
2. Parallel chunked cipher transform for large single files.
3. Batch mode: several files processed concurrently, with per-file error isolation.
4. Byte-identical output to the sequential implementation, always.
5. No measurable cost on small inputs.

## Non-goals

- Streaming / bounded-memory processing of files larger than RAM.
- Parallelising `LanguageDetector` or the scorers internally.
- Touching the deliberate ISP counter-example in `TextReader`, or the
  `Animal`/`PdfReader` practice classes.
- Any change to `MainTest`.

## Architecture

New package `ua.com.javarush.j4.concurrent`, built on one seam.

```java
public interface TaskExecutor extends AutoCloseable {
    /** Runs all tasks, blocks until done, returns results in SUBMISSION order. */
    <T> List<T> invokeAll(List<? extends Callable<T>> tasks);

    /** Configured width. May be answered without starting a pool. */
    int parallelism();

    @Override void close();
}
```

Results in submission order is a contract guarantee, not a caller responsibility. It is
what makes chunk reassembly and brute-force tie-breaking deterministic, so it belongs in
one place rather than being re-derived at each call site.

Implementations:

| Class | Behaviour |
|---|---|
| `DirectTaskExecutor` | Runs each task inline on the calling thread, in order. `parallelism()` is 1. `close()` is a no-op. |
| `ForkJoinTaskExecutor` | Owns a `ForkJoinPool(n)`. `close()` shuts down and awaits termination. |
| `LazyForkJoinTaskExecutor` | Delegates to a `ForkJoinTaskExecutor` created on first `invokeAll`. `parallelism()` answers from config without starting anything. `close()` is a no-op if never used. |

The executor is injected at `CryptoService`, the composition root already established for
`CipherFactory`, `TextReaders` and the rest. Concurrency becomes one more
constructor-injected abstraction, consistent with the existing DIP structure.

### Self-gating

**Each parallel component checks its own input size — by consulting `ParallelPolicy` —
and falls back to the sequential algorithm below threshold.** The thresholds live in
`ParallelPolicy` (one place to tune); the *decision to apply* them lives in each
component, because that is where the input is known. Deciding centrally up front does not
work: the cipher is constructed before the file is read, so text length is not yet
available.

A component also falls back to sequential when `executor.parallelism() == 1`, regardless
of size — chunking into a single-threaded executor is pure overhead.

Thresholds are measured in **characters** (`String.length()`), not bytes.

| Component | Threshold | Rationale |
|---|---|---|
| `ParallelCaesarCracker` | 8192 chars | Performs `keyspace × 2` full passes (~66× a plain transform), so it clears handoff cost early. |
| `ParallelCipher` | 65536 chars | Single pass; needs enough work to beat ~tens of µs of task handoff. |
| `BatchProcessor` | 2 files | Trivially worth it. |

Combined with `LazyForkJoinTaskExecutor`, a run over small inputs **never constructs a
thread pool at all**. All existing fixtures fall under every threshold, so the entire
current test suite keeps taking the sequential path.

### Single-level fan-out

Parallelism engages at exactly one level: the outermost stage with enough work.

- Many files → fan out over files; each file's command receives a `DirectTaskExecutor`,
  so cracking and chunking inside it stay sequential.
- One file → crack or chunk in parallel.

This removes oversubscription and thread-starvation deadlock from the design rather than
mitigating them, and it is a rule that can be stated in a sentence.

### Blocking I/O

Batch workers block briefly in `Files.readString`/`writeString`. This is accepted, not
compensated for: the CPU phase dominates and the idle window is negligible. Using
`ForkJoinPool.ManagedBlocker` was considered and rejected as unnecessary complexity for a
CLI that starts, does one job, and exits.

## Components

### `ParallelPolicy`

Value object holding thresholds and the resolved thread count.

```java
public static ParallelPolicy of(int requestedThreads);  // <= 0 => availableProcessors()
public int threads();
public boolean shouldParallelizeCrack(int textLength);
public boolean shouldParallelizeTransform(int textLength);
public boolean shouldParallelizeBatch(int fileCount);
```

### `ParallelCaesarCracker implements Cracker`

Same interface as `CaesarCracker`, so `BruteForceCommand` changes only in which
implementation it constructs.

Below 8192 chars, or when `parallelism() == 1`, it runs the plain sequential sweep.
Otherwise it partitions the keyspace into `min(keyspace, executor.parallelism())`
**contiguous ascending ranges**. Each task reduces its own range locally; a final fold combines the
partial winners. One task per key was rejected: 33 concurrent tasks each holding a full
decrypted copy of a 10 MB text is ~330 MB, whereas ranges cap live copies at
`2 × threads`.

```java
record Candidate(int key, String plaintext, double score) {}
```

**Determinism requirement.** The sequential loop uses strict `score > bestScore`, which
keeps the *lowest* key when scores tie. The parallel reduction must reproduce this
exactly, or brute-force results could vary between runs.

This holds because ranges are contiguous and ascending, each task scans its range in
ascending key order with strict `>`, and the final fold walks partials left to right with
strict `>`. Lowest key wins ties within a range; lowest range wins ties across ranges —
identical to the sequential scan. This gets an explicit test with an all-scores-equal
input asserting key 0.

### `ParallelCipher implements Cipher`

Decorator over any `Cipher`. Below 65536 chars, or when `parallelism() == 1`, it
delegates straight through without chunking.

Chunking targets `parallelism() × 4` chunks for load balance, with a 16 KB minimum chunk.
Boundaries snap off high surrogates so a surrogate pair is never split — EN/UA/RU are all
BMP so this never fires in practice, but it keeps the splitter correct for arbitrary
input. Reassembly uses a `StringBuilder` presized to `text.length()`.

Caesar, ROT13 and Atbash are per-character independent, so their chunks transform
directly. Vigenère is position-dependent and opts into a narrow second interface:

```java
/** A cipher whose chunk result depends on how many alphabet letters precede that chunk. */
public interface PositionDependentCipher {
    int alphabetLetterCount(String chunk);
    String encryptFrom(String chunk, int letterOffset);
    String decryptFrom(String chunk, int letterOffset);
}
```

Implemented only by `VigenereCipher`, whose existing `process(text, sign)` is refactored
to `process(text, sign, startLetterIndex)`. The letter-counting predicate is the same one
that already drives key advancement: `alphabet.position(c).isPresent()`.

For a position-dependent delegate, `ParallelCipher` runs two phases — count letters per
chunk in parallel, exclusive prefix sum sequentially (trivial), then transform each chunk
from its offset in parallel.

This is a deliberate ISP contrast to the widened `TextReader` documented in
`docs/SOLID.md` as a counter-example: a thin capability interface added only to the one
implementation that needs it, rather than pushed onto every implementor.

### Batch mode

`-f` becomes repeatable (`List<Path>`; picocli accumulates repeated occurrences). A
quoted argument containing `*` or `?` expands via `PathMatcher` against its parent
directory. `-f <path>` with a single occurrence — what `MainTest` pins — is unaffected.

```java
public record FileOutcome(Path input, Path output, Throwable failure) {
    public boolean succeeded() { return failure == null; }
}
public record BatchReport(List<FileOutcome> outcomes) {
    public int succeeded(); public int failed(); public boolean anyFailed();
}

public final class BatchProcessor {
    public BatchReport process(List<Path> inputs, Function<Path, CryptoCommand> commandFactory);
}
```

Each task catches its own failure and records an outcome, so one corrupt gzip never
aborts the batch. The report prints in **input order** regardless of completion order,
with a summary line. Exit code is non-zero if any file failed.

`CryptoService` gains a second entry point rather than changing the existing one:

```java
public Path execute(CryptoRequest request);                  // unchanged
public BatchReport executeAll(List<CryptoRequest> requests);  // new
```

The CLI builds one request per resolved path and calls `executeAll` when there is more
than one, `execute` otherwise. `CryptoRequest` is unchanged.

### CLI

New flag `--threads N`: `1` forces sequential everywhere; omitted or `0` means
`availableProcessors()`. Makes the feature measurable and provides an escape hatch.

### Lifecycle and errors

`CryptoService` becomes `AutoCloseable` and owns the executor; `CryptoCli.call()` uses
try-with-resources. `close()` is cheap when no pool was ever created.

New `ConcurrentExecutionException extends CryptanalysisException`, fitting the existing
hierarchy. Task failures are unwrapped from `ExecutionException` with the cause
preserved. `InterruptedException` restores the interrupt flag before wrapping.

Lazy pool creation is guarded by `synchronized` — it happens once, so contention is
irrelevant and the simplest correct construct wins.

## Testing

The core discipline is **equivalence**: every parallel component must produce
byte-identical output to its sequential counterpart.

| Test | Covers |
|---|---|
| `TaskExecutorTest` | Direct and fork-join implementations; submission-order results; exception unwrapping; lazy pool never started below threshold; clean shutdown |
| `ParallelCaesarCrackerTest` | Equivalence with `CaesarCracker` across generated texts × all keys; all-scores-tied input yields key 0; partition counts of 1, 2, and > keyspace |
| `ParallelCipherTest` | Equivalence with all four ciphers; chunk sizes of 1, a prime, and an exact multiple; text shorter than one chunk; empty string; Vigenère offset correctness across boundaries |
| `BatchProcessorTest` | Per-file failure isolation; deterministic report order under out-of-order completion |
| Stability | Each parallel component run 50× asserting identical results, to smoke out races |
| `MainTest` | Untouched; must go from 30 failures to 0 |

## Implementation order

0. Restore `Main.main` to `new CryptoCli().run(args)`; confirm green baseline.
1. `concurrent/` package: `TaskExecutor`, the three implementations, `ParallelPolicy`,
   `ConcurrentExecutionException`, plus tests.
2. `ParallelCaesarCracker` + equivalence and determinism tests; wire into
   `BruteForceCommand`.
3. `PositionDependentCipher`, `VigenereCipher` refactor, `ParallelCipher` + equivalence
   tests; wire into `CryptoService`.
4. Batch: repeatable `-f` with glob expansion, `BatchProcessor`, `BatchReport`,
   `FileOutcome` + tests; wire into `CryptoService` and the CLI.
5. `--threads` flag.
6. Update `CLAUDE.md` architecture section and `ARCHITECTURE.md`.
7. Full verification: `./mvnw test`, `./mvnw test -Dtest.lang.ua=true`, `./mvnw package`.

Each step lands green before the next begins.

## Observations, deliberately out of scope

- `CryptoCommand.execute()` calls `pick.getMeta()` and discards the result — a no-op left
  from the ISP demonstration. Harmless; not touched here.
- `DictionaryScorer.score` recompiles a regex split on every call, so brute force does it
  `keyspace` times. Parallelism hides most of this. Optimising it is a separate change.
