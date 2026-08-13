# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project context

This is the **starter repository** for the JavaRush "Module 1. Java Syntax" final project (a Caesar-cipher cryptanalyzer). Students fork this repo, finish the implementation, and open a pull request back to the original. The full assignment spec (Ukrainian) lives at `src/main/resources/project-description.pdf` — that is the source of truth for requirements.

The starter ships only an empty `Main` — students design and write every class themselves. `MainTest` is the authoritative behaviour contract: green tests = working implementation. `CHECKLIST.md` is the test-to-task map.

## Build, test, run

- Java 17 + Maven wrapper (`./mvnw` — no local Maven install required).
- Run all tests: `./mvnw test` (English only; Ukrainian cases are skipped by default)
- Enable the optional Ukrainian cases: `./mvnw test -Dtest.lang.ua=true`
- Run a single nested group: `./mvnw -Dtest='MainTest$LanguageTests' test`
- Run a single test method: `./mvnw -Dtest='MainTest$LanguageTests#encrypt' test`
- Package the runnable jar: `./mvnw package` (output `target/J4-M1-FP-1.0-SNAPSHOT.jar`, a shaded fat jar including picocli)
- Run the jar: `java -jar target/J4-M1-FP-1.0-SNAPSHOT.jar -e -k 5 -f path.txt`
- Show help: `java -jar target/J4-M1-FP-1.0-SNAPSHOT.jar --help`
- CI: `.github/workflows/run_tests.yaml` runs `mvn package` on every push.

## Benchmark

Every performance claim in this file is reproducible on your own machine:

```
./mvnw -o test-compile
java -cp target/classes:target/test-classes ua.com.javarush.j4.bench.ConcurrencyBenchmark
```

Options: `--quick` (~10 s smoke test), `--threads N`, `--max-size N` (MB, default 16). A full run takes about 70 seconds. Add `-Xmx8g` for 16 MB texts — it says so if your heap is short.

It is a `main`, not a test, and must stay that way: it must never run in CI.

Seven sections. §2 is a correctness gate that runs *before* any timing — if the split path disagrees with the sequential one, the benchmark says so and exits non-zero, because timings for a wrong answer are worthless. §4 forces a split at every size, including below the gate, and prints both the cold and warm pool bars; it is how `MIN_CHARS_FOR_TRANSFORM` is derived. §6 and §7 carry the *previous* implementations — a whole-text key sweep, a linear ring scan — and time both, so the before/after ratios are measured on your hardware rather than quoted from mine. Each isolates exactly one change.

## CLI convention — important

The starter and its tests use **option-style** arguments, deliberately modelled on the [POSIX Utility Conventions](https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap12.html):

```
-e | -d | -b       command (encrypt / decrypt / brute force)
-k <int>           key (required for -e and -d; NOT passed for -b)
-f <path>          file path (repeatable; a quoted glob like '*.txt' is expanded)
--threads <int>    worker threads (0 = every core, 1 = fully sequential)
```

Example: `-e -k 5 -f /path/to/file.txt`. Order must be arbitrary (pinned by `EncryptFileTests#argumentOrderIsArbitrary`) — students write the parser themselves and the validation tests cover every error condition. All command flags are single-character (`-b`, not `-bf`) to satisfy POSIX Guideline 3 and avoid the Guideline 5 grouping ambiguity where `-bf` would parse as `-b -f`.

Note that the PDF spec describes a different, **positional** convention (`ENCRYPT <path> <key>` / `BRUTE_FORCE`). The starter intentionally diverges to teach the more standard, tool-portable POSIX shape — when in doubt, match the tests, not the PDF.

## Output file naming

Encrypted output goes to `foo [ENCRYPTED].txt`; decrypted output to `foo [DECRYPTED].txt`. The `[DECRYPTED]` marker *replaces* `[ENCRYPTED]` rather than being appended — `foo [ENCRYPTED].txt` → `foo [DECRYPTED].txt`, not `foo [ENCRYPTED] [DECRYPTED].txt`. Filenames are assumed to end in `.txt`. Tests pin these rules via `MainTest$FileTests`.

## Cipher model — important

The English alphabet is **26 letters with case preserved**, modelled as two independent rings (upper and lower). A shift never crosses case: `A`−1=`Z`, `a`−1=`z` (NOT `A`−1=`z`). Keys normalize mod 26, so 26 ≡ 0 and 27 ≡ 1. This is the conventional Caesar behaviour and is pinned by `EncryptEdgeCases#negativeKeyWrapsWithinCase`. (The optional Ukrainian alphabet is a separate 33-letter ring.)

## Concurrency model — important

**Two** workloads run in parallel: cipher transforms of texts over 8 MB (`ParallelCipher`) and batch multi-file runs (`BatchProcessor`). Both go through the `TaskExecutor` seam, created at `CryptoService` from `--threads`.

Brute force is deliberately *not* parallel — see "Why brute force is sequential" below. Do not re-add a parallel cracker without re-reading it.

Two rules keep this predictable:

1. **`TaskExecutor` is the only authority on width.** There is no separate policy object; `worthSplitting(workUnits, minUnits)` answers "wide enough, and enough work?" in one place, and each component passes its own measured threshold (`ParallelCipher.MIN_CHARS_FOR_TRANSFORM` = 8 MB, `BatchProcessor.MIN_FILES_FOR_BATCH` = 2). The shipped fixtures are far under these, so `MainTest` always takes the sequential path and `PooledTaskExecutor` — which creates its pool on first use — never starts a thread.
2. **Fan-out happens at exactly one level.** Batch runs hand each file's command `TaskExecutors.sequential()`, so nothing nests and the pool is never oversubscribed. Because width has one source of truth, handing a component the sequential executor is *sufficient* to make it sequential.

Output is byte-identical whatever `--threads` is set to, because `TaskExecutor.invokeAll` returns results in submission order and Vigenère chunks carry a letter offset. Use `--threads 1` to force fully sequential execution.

`PooledTaskExecutor` wraps a fixed daemon-thread pool rather than a `ForkJoinPool`: fan-out is single-level so there is no nested join for work-stealing to help with, and `ForkJoinTask` reconstructs a failed task's exception in the calling thread instead of rethrowing the original. `invokeAll` delegates to `ExecutorService.invokeAll`, whose submission-order guarantee is exactly what this design needs; it waits for every task and reports the first failure in submission order.

### Why brute force is sequential

`Alphabet` indexes every character it spans once at construction, so `shift`/`mirror`/`indexOf` are array reads rather than a linear scan of every ring per character. And `CaesarCracker` scores a 4 KB sample to pick the key, then decrypts the full text once — instead of decrypting and scoring the whole text once per key.

Together those made a 1 MB crack ~190× faster than the old sequential sweep and ~31× faster than the old 12-thread one. What remains is a 26-key sweep over 4 KB, which is sub-millisecond: thread handoff would cost more than the work. Below `CaesarCracker.SAMPLE_CHARS` the sample *is* the whole ciphertext, so short inputs behave exactly as a full sweep would.

### Measured (12 cores, JDK 25)

Transform only, warm JIT, best-of-15, DEFAULT alphabet — no JVM startup or I/O. Sizes under the 8 MB gate are shown with the split **forced**, since that is what the gate is choosing against:

| | sequential | 12 threads | saves | speedup |
|---|---|---|---|---|
| 64 KB | 0.16 ms | 0.11 ms | 0.04 ms | 1.5× |
| 256 KB | 0.62 ms | 0.25 ms | 0.37 ms | 2.5× |
| 1 MB | 2.54 ms | 1.22 ms | 1.32 ms | 2.1× |
| 4 MB | 10.27 ms | 2.73 ms | 7.54 ms | 3.8× |
| 16 MB | 40.96 ms | 9.96 ms | 31.00 ms | 4.1× |

**How the 8 MB gate is derived** — the rule matters more than the number, because the number moves: *a split must save more than the worst observed cost of the pool that performs it*. Two bars exist. A **cold** pool — the first in a process, classes not yet loaded — measured 3.3, 7.9, 8.2 and 12.6 ms across four runs on the same machine. Every pool after it costs ~0.1 ms. The cold bar is the one the CLI pays, being one-shot: start a JVM, split a few times, exit. It is one sample per JVM by construction, so that spread cannot be averaged away — taking the worst is what stops the constant drifting each time someone re-measures. Against ~13 ms, 8 MB is the smallest power of two that clears it.

Used as a library with a warm pool, the bar is 0.1 ms instead and a far lower gate would pay. `ConcurrencyBenchmark` §4 prints both bars and forces a split at every size — run it before moving the constant.

**End-to-end via the jar, 4.7 MB file:** brute force 0.26 s (was 3.46 s sequential / 1.68 s on 8 threads). Encrypt 0.18–0.29 s, and `--threads 1` is no slower than all cores. Batch of six 4.7 MB files: 0.38 s → 0.32 s.

Read that last line honestly: after the algorithmic fixes the CLI's wall clock is dominated by JVM startup and I/O, and threads barely move it. The parallel transform is real and measurable in-process, but for one-shot CLI runs the algorithm was the answer, not the thread count.

**Batch exit codes:** `CryptoCli.call()` returns 1 when any file failed, but the *process* exit code stays 0 — `Main.main` discards `run()`'s return value and never calls `System.exit`, because `MainTest` invokes `Main.main(...)` in-process and an exit would kill the surefire JVM. This predates the concurrency work.

## Architecture

Single-module Maven project, package root `ua.com.javarush.j4`, organised by responsibility:

- `Main` — entry point; delegates to the picocli CLI and never propagates exceptions.
- `cli/` — `CryptoCli` (picocli `@Command`); parses the legacy `-e/-d/-b`, `-k`, `-f` contract plus additive `--cipher`, `--keyword`, `--alphabet` flags.
- `app/` — `CryptoService` facade + `command/` (Template-Method `CryptoCommand`: Encrypt/Decrypt/BruteForce).
- `cipher/` — `Cipher` strategy + Caesar/ROT13/Atbash/Vigenère + `CipherFactory`.
- `alphabet/` — `Alphabet` (owns the O(1) character index) / `CharacterRing` (owns ring arithmetic, addressed by position) + `Alphabets` registry (EN/UA/RU + composite default).
- `crack/` — `Cracker`/`CaesarCracker` (samples to choose a key), pluggable `FitnessScorer` (dictionary + frequency), `LanguageDetector`/`LanguageProfile`.
- `io/` — `TextReader` strategies (txt/md/gz) + `TextReaders` registry, `TextWriter`, `OutputNaming`.
- `concurrent/` — `TaskExecutor` seam (`DirectTaskExecutor`, `PooledTaskExecutor`) + `TaskExecutors` factory.
- `app/batch/` — `BatchProcessor` + `BatchReport`/`FileOutcome` for concurrent multi-file runs.
- `error/` — `CryptanalysisException` hierarchy.

`MainTest` remains the authoritative externally-observable contract; the package layout above is the internal design that satisfies it.

## Test suite shape

End-to-end tests live in `src/test/java/.../MainTest.java`. Every test drives `Main.main(...)` with a `@TempDir` and asserts on the file the run creates (diffing directory listings before/after). Test fixtures (Hamlet EN, Orwell UA) live in `src/test/resources/hamlet.txt` and `orwell.txt`, loaded via `MainTest.loadResource`.

Nested groups in `MainTest`:
- `FileTests` — file creation, markers, filename transformation, plus content sanity checks (encrypt result matches expected ciphertext for small fixtures; round-trip restores original).
- `LanguageTests` — single parametrized group covering both English and Ukrainian: single-char encrypt, single-char decrypt, full encrypt→decrypt cycle, brute-force recovery. Each scenario runs once per language. **Ukrainian invocations are gated** behind `Boolean.getBoolean("test.lang.ua")` via a shared `assumeLanguageEnabled(lang)` helper — they report as *skipped* (JUnit assumption), not failed, unless `-Dtest.lang.ua=true` is set.
- `EncryptEdgeCases` — empty file, key=0/26/27/-26, digits/special chars, multiline. English-only by design.
- `OriginalFileSafety` — input file is unchanged after encrypt.
- `ValidationTests` — missing/unknown flags, non-numeric keys, non-existent file. All assert via "no new file appears in `@TempDir`" + `assertDoesNotThrow`.

Brute-force tests require the recovered text to equal the original exactly (case-sensitive). Students must find the correct key, not just *some* readable shift.

When tests fail because a command path isn't wired yet (typical early-stage failure: `UnsupportedOperationException` from a student stub), `MainTest.execute(...)` rethrows with a Ukrainian hint pointing at the relevant `CHECKLIST.md` step instead of a raw stack trace.

## Working with this repo

- Maven wrapper is included — use `./mvnw test` rather than `mvn test`. First run downloads Maven into `~/.m2/wrapper/dists/`.
- `pom.xml` configures `maven-jar-plugin` with `Main-Class: ua.com.javarush.j4.Main`. `./mvnw package` produces a runnable jar in `target/`.
- CI runs `mvn package` — fails (intentionally) until students implement enough to make all tests pass. CI being red on a student fork IS the signal.
- `CHECKLIST.md` is the test-to-task map students follow. Keep it in sync if you add/remove tests.
- `src/main/resources/input.txt` is a sample paragraph for manual `java -jar` testing — not used by automated tests.
- Don't reformat `MainTest` for cosmetic reasons. Students are graded against this baseline; noisy diffs hurt review.
