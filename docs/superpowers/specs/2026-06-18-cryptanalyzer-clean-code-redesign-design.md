# Caesar Cryptanalyzer — Clean-Code Redesign Design

**Date:** 2026-06-18
**Branch:** `reimplement-oop-architecture` (continues on the same branch)
**Status:** Approved
**Supersedes (refines):** `2026-06-18-cryptanalyzer-oop-reimplementation-design.md`

## Goal

Refine the existing OOP cryptanalyzer into a cleaner, more SOLID design **without
raising its altitude into enterprise ceremony**. The project's identity stays
"effective, very understandable, simple" (a JavaRush Module-1 syntax project).
Concretely: introduce real dependency injection (manual, via a composition root),
remove the OCP-violating string `switch` factories, unify the duplicated
language concept, fix a latent brute-force bug, and clean up honesty/quality
nits — while keeping `MainTest` byte-for-byte green.

**Explicitly NOT in scope** (rejected as over-engineering for this project):
a DI framework (Spring/Guice), Java Platform Module System (`module-info`), and
`ServiceLoader`/SPI plugin discovery. Manual constructor injection + in-code
registries deliver the same SOLID benefits at the right altitude.

## Problems being fixed

1. **No DI / DIP violated.** `CryptoService` constructs all 5 collaborators;
   `CryptoCli` constructs `CryptoService`; `BruteForceCommand` builds its scorer
   and cracker inside `transform()`. Nothing can be unit-tested with fakes.
2. **OCP via string `switch` in 5 places** (`CipherFactory`,
   `BruteForceCommand.buildScorer`, `Alphabets.byName`,
   `CryptoService.forcedProfile`, `LanguageProfiles.all`). Names are duplicated
   between CLI help text and factories.
3. **Conflated concept.** `CryptoRequest.alphabetName` means two different things
   (cipher alphabet vs brute-force language); `Alphabets` and `LanguageProfiles`
   are two registries that both define `en/ua/ru` and can drift.
4. **Latent bug + misplaced responsibility.** `-b` always runs `CaesarCracker`
   and silently ignores `-c` (so `-c vigenere -b` lies). A command should not own
   a scorer registry.
5. **Primitive obsession.** `key`/`keyword`/`cipherName`/`scorerName` ride as
   loose primitives.
6. **Honesty/quality nits.** `PdfReader` claims `.pdf` support but reads raw
   bytes (won't extract real PDF text) and breaks house style; Vigenère silently
   shifts by 0 for keyword chars outside the alphabet; `dependency-reduced-pom.xml`
   is not gitignored; `MainTest` is locally edited.

## Hard constraints (unchanged contract)

- `src/test/java/.../MainTest.java` is the locked behavior contract — stays
  byte-for-byte green; the local `UA_ENABLED = true` edit is reverted to the
  pristine `Boolean.getBoolean("test.lang.ua")`.
- Legacy CLI `-e | -d | -b`, `-k <int>`, `-f <path>` preserved; additive flags
  `-c/--cipher`, `--keyword`, `-a/--alphabet`, `-s/--scorer` preserved.
- `Main.main` never propagates exceptions for bad input / missing files; no
  output file written on error.
- Cipher model unchanged: case-preserving rings (EN=26, UA=33), keys mod ring
  size, shifts never cross case; brute-force recovers the original exactly.
- The **ring-sharing keystone** (see ARCHITECTURE.md §1) is preserved:
  `Alphabets.DEFAULT` and per-language alphabets share the same `CharacterRing`
  instances.
- Java 17; package root `ua.com.javarush.j4`; UTF-8.

## Design

### A. Dependency injection + composition root

- Every collaborator is passed in via constructor; no production class calls
  `new` on another collaborator except the composition root.
- New class **`Composition`** (the composition root, in the root package
  `ua.com.javarush.j4`, beside `Main`): assembles the full object graph —
  registers cipher creators and scorer creators, builds the reader list, writer,
  `OutputNaming`, `LanguageDetector`, `Languages`, the `CryptoService`, and
  finally a configured `CryptoCli`. Exposes `CryptoCli cli()`.
- `Main.main(args)` becomes: `new Composition().cli().run(args)`.
- `CryptoCli` gains a constructor taking the `CryptoService` (picocli still binds
  option fields by reflection on the instance we pass to `CommandLine`).
- `CryptoService` constructor takes: `CipherCatalog`, `ScorerCatalog`,
  `Languages`, `LanguageDetector`, `TextReaders`, `TextWriter`, `OutputNaming`.

### B. Unified `Language` (replaces LanguageProfile/LanguageProfiles)

- **`Language`** (record, package `crack`): `String code()`, `Alphabet alphabet()`,
  `Set<String> commonWords()`, `Map<Character,Double> letterFrequencies()`,
  `Set<Character> distinctive()`. It **composes** the shared `Alphabet` instance
  (`Languages.ENGLISH.alphabet() == Alphabets.ENGLISH`) — one source of truth for
  rings.
- **`Languages`** registry: `ENGLISH`, `UKRAINIAN`, `RUSSIAN`, `all()`, and
  `Optional<Language> byCode(String)` returning empty for `default`/`auto`.
- `Alphabet`/`CharacterRing`/`Alphabets` are unchanged value objects.
  `Alphabets.DEFAULT` (EN+UA composite, no linguistics) remains for
  encrypt/decrypt. `Alphabets.byName` is **removed** — alphabet resolution for
  ciphers now goes through `Languages.byCode(code)` (its alphabet) or
  `Alphabets.DEFAULT` when no language is selected.

### C. Registries (replace switch-factories)

- **`CipherCatalog`** — wraps `Map<String, CipherCreator>` where
  `@FunctionalInterface CipherCreator { Cipher create(Alphabet alphabet, Integer
  key, String keyword); }`. Method `Cipher create(CipherSpec spec, Alphabet
  alphabet)` looks up `spec.cipherName()` and invokes the creator with
  `(alphabet, spec.key(), spec.keyword())`. Registered creators: caesar, rot13,
  atbash, vigenere. Unknown name → `InvalidArgumentsException`.
- **`ScorerCatalog`** — wraps `Map<String, Function<Language, FitnessScorer>>`;
  `FitnessScorer create(String name, Language language)`. Registered: dictionary,
  frequency. Unknown → `InvalidArgumentsException`.
- **`TextReaders`** takes an injected `List<TextReader>` (composition root
  supplies gzip, markdown, pdf, plain) plus the plain fallback; `pick(Path)`
  unchanged behavior.
- Cipher/scorer name constants live in one place (the catalog registration in the
  composition root), removing the CLI-help/factory duplication.

### D. Request value objects + command layer

- **`CipherSpec`** (record): `String cipherName`, `Integer key`, `String keyword`.
- **`CryptoRequest`** (record): `Operation operation`, `Path file`,
  `CipherSpec cipherSpec`, `String languageCode`, `String scorerName`.
  (One `languageCode`, resolved coherently per operation — no more dual-meaning
  via two switches.)
- **Commands stay three named classes** under `app.command`:
  `EncryptCommand`, `DecryptCommand`, `BruteForceCommand`, all extending the
  Template-Method `CryptoCommand` (read → transform → name → write). **Encrypt and
  Decrypt are kept separate (not collapsed).**
- `BruteForceCommand` no longer builds its own scorer: it receives a configured
  `FitnessScorer` (and the resolved `Language`/`Alphabet`) from `CryptoService`.
- **Brute-force bug fix:** brute-force is Caesar-only. If `operation ==
  BRUTE_FORCE` and the cipher name is anything other than caesar, `CryptoService`
  throws `InvalidArgumentsException("brute-force is supported only for the caesar
  cipher")`. (`-b` with the default caesar still works with no `-k`.)
- `CryptoService.execute(request)` resolves the language/alphabet, builds the
  right command from injected catalogs, and runs it.

### E. IO honesty + symmetry

- **`TextWriter`** becomes an interface (`void write(Path, String) throws
  IOException`) with impl **`FileTextWriter`**. Symmetric with `TextReader`;
  enables an in-memory sink in tests.
- **`PdfReader`** implemented for real with **Apache PDFBox**
  (`org.apache.pdfbox:pdfbox`): `read()` loads the document and extracts text via
  `PDFTextStripper`; `supports()` matches `.pdf`. Added to `pom.xml` dependencies
  and bundled by the shade plugin. House style (`final`, 4-space) fixed.

### F. Quality cleanup

- `VigenereCipher` constructor rejects a keyword containing any character not in
  the configured alphabet (`InvalidArgumentsException`), eliminating the silent
  shift-by-0.
- `.gitignore` gains `dependency-reduced-pom.xml` and `target/`.
- `MainTest` `UA_ENABLED` reverted to `Boolean.getBoolean("test.lang.ua")`.

## Data flow (unchanged shape, cleaner wiring)

```
Main → Composition.cli() builds the graph
CryptoCli (picocli) → CryptoRequest(operation, file, CipherSpec, languageCode, scorerName)
CryptoService.execute(request):
   language  = Languages.byCode(languageCode)            // Optional
   alphabet  = language.map(Language::alphabet).orElse(Alphabets.DEFAULT)
   ENCRYPT/DECRYPT → cipher = cipherCatalog.create(spec, alphabet)
                   → Encrypt/DecryptCommand
   BRUTE_FORCE     → require caesar; profile = language.orElseGet(() -> detector.detect(text-after-read))
                   → scorer = scorerCatalog.create(scorerName, profile)
                   → BruteForceCommand(CaesarCracker(profile.alphabet(), scorer))
   command.execute()  // read (→ UnreadableSourceException on failure) → transform → name → write
```

Note: brute-force still detects on ciphertext when no language is forced —
documented limitation (ARCHITECTURE.md), unchanged by this redesign.

## Error handling

Unchanged contract: picocli parameter-exception handler (parse errors) +
execution-exception handler (`CryptanalysisException` hierarchy) in
`CryptoCli.run`; read failures wrapped as `UnreadableSourceException`; nothing
propagates from `Main`; no file written on error.

## Testing

- `MainTest` stays the locked, green contract (full suite green with
  `-Dtest.lang.ua=true`).
- New/updated unit tests enabled by DI:
  - `CryptoServiceTest` with a fake `TextReader`/`TextWriter` (no filesystem) for
    the orchestration paths, plus the brute-force-rejects-non-caesar case.
  - `CipherCatalogTest`, `ScorerCatalogTest` (registration + unknown-name throw).
  - `LanguagesTest` (byCode resolution incl. auto → empty).
  - `PdfReaderTest` — generate a small PDF with PDFBox in the test, read it back,
    assert extracted text.
  - `VigenereCipherTest` — keyword with a non-alphabet char throws.
- Existing per-component tests are migrated where types are renamed
  (LanguageProfile→Language), keeping behavior assertions intact.

## Files: created / changed / removed

- **Created:** `Composition`, `crack/Language`, `crack/Languages`,
  `cipher/CipherCatalog`, `cipher/CipherSpec`, `crack/ScorerCatalog`,
  `io/FileTextWriter`; tests `CipherCatalogTest`, `ScorerCatalogTest`,
  `LanguagesTest`, `PdfReaderTest`.
- **Changed:** `Main`, `cli/CryptoCli`, `app/CryptoService`, `app/CryptoRequest`,
  `app/command/BruteForceCommand`, `cipher/VigenereCipher`, `io/TextWriter`
  (→ interface), `io/TextReaders`, `io/PdfReader`, `pom.xml`, `.gitignore`,
  `MainTest` (revert UA edit), and tests touching renamed types.
- **Removed:** `cipher/CipherFactory`, `crack/LanguageProfile`,
  `crack/LanguageProfiles`, `alphabet/Alphabets.byName` (method).

## Out of scope (YAGNI)

DI framework, JPMS modules, SPI/ServiceLoader discovery, collapsing
Encrypt/Decrypt commands (kept separate by decision), micro-optimizing
`Alphabet`'s per-char ring scan, gzip-output re-compression.
