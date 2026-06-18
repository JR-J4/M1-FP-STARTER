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
- Package the runnable jar: `./mvnw package` (output `target/J4-M1-FP-1.0-SNAPSHOT.jar`)
- Run the jar: `java -jar target/J4-M1-FP-1.0-SNAPSHOT.jar -e -k 5 -f path.txt`
- CI: `.github/workflows/run_tests.yaml` runs `mvn package` on every push.

## CLI convention — important

The starter and its tests use **option-style** arguments, deliberately modelled on the [POSIX Utility Conventions](https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap12.html):

```
-e | -d | -b       command (encrypt / decrypt / brute force)
-k <int>           key (required for -e and -d; NOT passed for -b)
-f <path>          file path
```

Example: `-e -k 5 -f /path/to/file.txt`. Order must be arbitrary (pinned by `EncryptFileTests#argumentOrderIsArbitrary`) — students write the parser themselves and the validation tests cover every error condition. All command flags are single-character (`-b`, not `-bf`) to satisfy POSIX Guideline 3 and avoid the Guideline 5 grouping ambiguity where `-bf` would parse as `-b -f`.

Note that the PDF spec describes a different, **positional** convention (`ENCRYPT <path> <key>` / `BRUTE_FORCE`). The starter intentionally diverges to teach the more standard, tool-portable POSIX shape — when in doubt, match the tests, not the PDF.

## Output file naming

Encrypted output goes to `foo [ENCRYPTED].txt`; decrypted output to `foo [DECRYPTED].txt`. The `[DECRYPTED]` marker *replaces* `[ENCRYPTED]` rather than being appended — `foo [ENCRYPTED].txt` → `foo [DECRYPTED].txt`, not `foo [ENCRYPTED] [DECRYPTED].txt`. Filenames are assumed to end in `.txt`. Tests pin these rules via `MainTest$FileTests`.

## Cipher model — important

The English alphabet is **26 letters with case preserved**, modelled as two independent rings (upper and lower). A shift never crosses case: `A`−1=`Z`, `a`−1=`z` (NOT `A`−1=`z`). Keys normalize mod 26, so 26 ≡ 0 and 27 ≡ 1. This is the conventional Caesar behaviour and is pinned by `EncryptEdgeCases#negativeKeyWrapsWithinCase`. (The optional Ukrainian alphabet is a separate 33-letter ring.)

## Architecture

Single-module Maven project, package root `ua.com.javarush.j4`. The starter ships exactly one class:

- `Main` — entry point with an empty `main(String[] args)` body. Students design and implement everything: argument parsing, file I/O, Caesar cipher, brute-force, output naming, Ukrainian-alphabet support. There is no prescribed class layout; only the externally observable behaviour pinned by `MainTest` matters.

Important behaviour constraint: `Main.main` MUST NOT propagate exceptions for invalid CLI arguments or missing files — `MainTest$ValidationTests` asserts `assertDoesNotThrow(...)`. Catch and report cleanly.

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
