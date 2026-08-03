# Architecture — keystone invariants

This document captures the **load-bearing invariants** of the cryptanalyzer: the
handful of facts that, if broken, fail *silently* (most tests stay green while
correctness quietly breaks). The package map and CLI live in
[`CLAUDE.md`](CLAUDE.md); this file is only the things you must not break.

The flow is one direction, edge → core:

```
Main → cli/CryptoCli → app/CryptoService (facade)
     → app/command/* (Template Method) → cipher | crack | io
```

---

## 1. Rings are shared by identity — the keystone

`alphabet/Alphabets.java` builds every `Alphabet` from the **same**
`CharacterRing` instances:

```java
ENGLISH   = [EN_UPPER, EN_LOWER]
UKRAINIAN = [UA_UPPER, UA_LOWER]
DEFAULT   = [EN_UPPER, EN_LOWER, UA_UPPER, UA_LOWER]   // reuses the very same rings
```

**Invariant:** a character shifted via `DEFAULT` and the same character shifted
via `ENGLISH`/`UKRAINIAN` must produce identical output.

**Why it matters:** encrypt/decrypt use `DEFAULT` (composite); brute-force cracks
with a *single-language* alphabet (`profile.alphabet()`). Exact recovery
(`decrypt(encrypt(text, k), k) == text`, and brute-force returning the precise
original) only holds because the rings are literally the same objects with the
same order. Rebuild `DEFAULT` from fresh ring strings and nothing fails to
compile — but if a ring's order drifts, exact recovery breaks and only the
brute-force fixture tests would catch it.

**Don't:** introduce a second copy of any ring, or reorder a ring. `DEFAULT`
deliberately contains **EN + UA only** (no Cyrillic overlap); Russian shares
letters with Ukrainian in a *different* order, so adding `RU_*` rings to
`DEFAULT` would make a shared Cyrillic letter shift two different ways depending
on ring precedence. Russian stays selectable only via `-a ru`.

## 2. Case never crosses; non-members pass through

`alphabet/CharacterRing.java`, `alphabet/Alphabet.java`.

An alphabet is a set of independent rings (upper, lower). `Alphabet.shift` finds
the ring that *contains* the character and shifts within it; characters in no
ring are returned unchanged. Keys normalize `mod ringSize`
(`((idx + k) % n + n) % n`), so `A-1=Z` (not `z`), `27 ≡ 1`, `-26 ≡ 0`.

**Invariant:** uppercase stays uppercase, lowercase stays lowercase, digits/
punctuation/whitespace are untouched, and English (26) vs Ukrainian (33) keep
their own ring sizes. This is pinned by `MainTest$EncryptEdgeCases` and the
`LanguageTests` single-letter cases.

**Coupling note:** `CharacterRing.shift`/`mirror` assume the char is a member
(they would index out of range otherwise). They are safe *only* because
`Alphabet` always checks membership first (`ringOf(c) == null ? c : …`). Call
`CharacterRing` directly at your peril; go through `Alphabet`.

## 3. A `Cipher` is a *configured* pure transform

`cipher/Cipher.java` is just `String encrypt(String)` / `String decrypt(String)`.
Key (Caesar) and keyword (Vigenère) are bound at **construction**, so all four
ciphers share one uniform Strategy interface with no key parameter leaking into
the signature. `CipherFactory` is the only place that knows how to assemble a
configured cipher from a name + key/keyword + alphabet.

**Invariant:** `decrypt` is the exact inverse of `encrypt` for the same
configuration. ROT13 and Atbash are self-inverse special cases; everything
delegates character math to `Alphabet` (no ring arithmetic is re-implemented in
the cipher classes).

## 4. Read happens before write — no output on failure

`app/command/CryptoCommand.java`, `execute()` (the `final` Template Method):

```
read(input)  →  transform(text)  →  outputPath(input)  →  write(output)
```

**Invariant:** if reading the input fails, **no output file is created** and the
exception does not escape `Main`. This is the *mechanism* behind
`MainTest$ValidationTests` (`assertDoesNotThrow` + "no new file appears"). A read
`IOException` is wrapped as `UnreadableSourceException` for a clean message.

**Don't:** compute the output path or open the writer before the read succeeds,
and don't move the read into a subclass — the ordering must stay in the base
`execute()`.

## 5. Errors are contained at the edge, never propagated

`Main.java` → `cli/CryptoCli.run(...)`. picocli's `execute()` routes **parse**
errors (unknown flag, non-numeric `-k`, missing `-f`, missing command via the
`@ArgGroup(multiplicity="1")`) through its parameter-exception handler, and
**execution** errors (the `error/` hierarchy, e.g. `InvalidArgumentsException`
from a missing key or unknown cipher/scorer) through the execution-exception
handler. Both print and return a non-zero code.

**Invariant:** `Main.main` never throws for bad input. The "key required for
caesar `-e`/`-d`, absent for `-b`" rule cannot be expressed by picocli
annotations alone — it is validated downstream before any write
(`CipherFactory` rejects a null Caesar key).

## 6. `CryptoRequest.alphabetName` is overloaded by design

`app/CryptoService.java`.

The same field drives two things depending on the operation:

- encrypt/decrypt → `Alphabets.byName(alphabetName)` selects the cipher alphabet.
- brute-force → `forcedProfile(alphabetName)`: `en`/`ua`/`ru` force that
  language profile; `default`/`auto` (and anything else) → `null` → auto-detect.

**Know this** before adding a flag: the CLI surfaces it as `-a/--alphabet`, but
for brute-force it means "language to score against," not "alphabet to shift."

## 7. Parallelism never changes the answer

`concurrent/`, `crack/ParallelCaesarCracker.java`, `cipher/ParallelCipher.java`.

The domain layer is immutable and stateless after construction (`Alphabet`,
`CharacterRing`, all four ciphers, both scorers, `LanguageProfile`), so it is
shared across threads with no locking. Everything concurrent hangs off one seam,
`TaskExecutor`, injected at `CryptoService` alongside the other dependencies.

**Invariant:** output is byte-identical whatever `--threads` is set to. Three
things hold it up, and breaking any one of them silently corrupts results:

1. **`TaskExecutor.invokeAll` returns results in submission order**, never
   completion order. Chunk reassembly and tie-breaking both depend on it.
2. **Keyspace ranges are contiguous and ascending**, and both the in-range scan
   and the cross-range fold use a strict `>`. That reproduces the sequential
   sweep's tie-break — lowest key wins an equal score. A `>=` anywhere flips it.
3. **Vigenère chunks carry a letter offset.** Its key advances per alphabet
   member, so a chunk's result depends on how many letters precede it;
   `PositionDependentCipher` supplies that via a prefix sum over per-chunk counts.
   Position-independent ciphers skip the phase entirely.

**Fan-out happens at exactly one level** — the outermost stage with enough work.
Batch runs hand each file's command a `DirectTaskExecutor`, so no nesting occurs
and the pool cannot be oversubscribed or starved. Each component also self-gates
on input size via `ParallelPolicy`, so small inputs never touch a thread at all.

## Known, accepted trade-offs (documented, not bugs)

- **Language detection runs on ciphertext** (`app/command/BruteForceCommand.java`,
  see the inline comment). Reliable across scripts (a shift stays Latin/Cyrillic);
  for same-script UA↔RU it depends on distinctive letters surviving the shift and
  is validated only for the shipped fixtures. Force with `-a ua|ru` for ambiguous
  input. The more robust design ("crack with every profile, keep the best-scoring
  plaintext") is intentionally deferred.
- **Registries dispatch by `switch`**, not a registered map (`CipherFactory`,
  `BruteForceCommand.buildScorer`, `Alphabets.byName`, `TextReaders`). Adding a
  cipher/scorer/alphabet means editing one `switch` — an accepted OCP compromise.
  The boundaries are drawn so a future move to `ServiceLoader`/SPI is a one-class
  change.
- **gzip output is not re-compressed** (`io/OutputNaming` + `io/TextWriter`):
  `.gz` input is read/decompressed, but output is written as plain UTF-8 with a
  `.gz`-suffixed name. Harmless under the spec's `.txt` assumption; the
  reader/writer abstractions are intentionally asymmetric here.
- **`CryptoService` constructs its own collaborators** (no DI container). Fine at
  this size; it is the main spot where DIP is not applied, if you later want to
  unit-test the facade in isolation.
