# Caesar Cryptanalyzer — OOP Reimplementation Design

**Date:** 2026-06-18
**Branch:** `reimplement-oop-architecture`
**Status:** Approved

## Goal

Reimplement the JavaRush "Module 1" Caesar-cipher cryptanalyzer as a showcase of
clean, SOLID, pattern-driven OOP — while staying *effective, understandable, and
simple*. Patterns are introduced **only where they remove a conditional or an
`if`-on-type**, never for decoration.

The reimplementation is extensible along four axes the starter never had:

1. **Multiple ciphers** — Caesar, Vigenère, ROT13, Atbash (Strategy).
2. **Multiple alphabets** — English, Ukrainian, Russian, behind an `Alphabet`
   abstraction (Value Object + registry).
3. **Multiple file readers** — `.txt`, `.md`, `.gz`, chosen by extension
   (Factory / registry).
4. **Pluggable brute-force scorers** — letter-frequency and dictionary scorers
   behind a `FitnessScorer` interface (Strategy), language-aware.

## Hard constraint: behavior preservation

`src/test/java/.../MainTest.java` is the **authoritative behavior contract** and
MUST stay byte-for-byte green. It is the regression proof that the new
architecture preserves behavior. The existing CLI contract is unchanged:

```
-e | -d | -b       command (encrypt / decrypt / brute force)
-k <int>           key (required for -e and -d; NOT passed for -b)
-f <path>          file path
```

All new capability is **purely additive optional flags** with backward-compatible
defaults, so every existing `MainTest` invocation passes unchanged.

`Main.main` MUST NOT propagate exceptions for invalid CLI arguments or missing
files (`ValidationTests` asserts `assertDoesNotThrow` + "no new file appears").

## Architecture

Chosen shape: **Layered domain + GoF strategies.** A thin picocli edge delegates
to an application facade, which orchestrates single-responsibility domain
packages. Registries are designed so a later move to `ServiceLoader`/SPI plugin
discovery is a one-class change.

### Components & patterns

| Concept | Type | Pattern | Responsibility |
|---|---|---|---|
| `Cipher` | interface `encrypt(text)/decrypt(text)` | Strategy | A *configured* transform — key/keyword bound at construction, so it is a pure `String → String`. |
| `CaesarCipher`, `VigenereCipher`, `Rot13Cipher`, `AtbashCipher` | classes | | Concrete ciphers over an `Alphabet`. |
| `Alphabet` / `CharacterRing` | value objects | Value Object | An alphabet is an ordered set of case-independent rings (EN-upper, EN-lower, …). `shift(char, k)` moves a char within its own ring; non-members pass through unchanged. Makes `A+1=B`, `А+1=Б`, digits-untouched fall out with **no language detection for encrypt/decrypt**. |
| `Alphabets` | registry | Factory | Built-in EN, UA, RU. A composite "all rings" alphabet is the default, so mixed text just works. |
| `Cracker` / `CaesarCracker` | interface + impl | Strategy | Brute-force: enumerate the keyspace, score each candidate, return best `{key, plaintext}`. |
| `FitnessScorer` / `FrequencyScorer`, `DictionaryScorer` | interface + impls | Strategy | Language-aware "how natural is this text" score, driven by a `LanguageProfile` (letter frequencies + common words). |
| `LanguageDetector` | class | | Picks the `LanguageProfile` for brute-force by counting dominant letters. |
| `TextReader` / `PlainTextReader`, `MarkdownReader`, `GzipTextReader` | interface + impls | Factory / Chain | `supports(path)` + `read(path)`. `TextReaders` registry picks by extension. |
| `TextWriter` | class | | Symmetric output. |
| `OutputNaming` | class | | The `[ENCRYPTED]` ↔ `[DECRYPTED]` marker rules (replace, not append). |
| `CryptoService` | class | Facade | Orchestrates read → (cipher \| crack) → name → write. The single place the pipeline lives. |
| `EncryptCommand` / `DecryptCommand` / `BruteForceCommand` | classes | Command | One object per operation, built by the CLI, `execute()`d via the service. Mirrors the three CLI verbs. |
| `CipherFactory` | class | Factory | Builds a configured `Cipher` from a cipher name + key/keyword + alphabet. |

### Package structure

```
ua.com.javarush.j4
├── Main.java              // entry point: delegates to CLI, never throws
├── cli/      CryptoCli (picocli @Command), option groups → builds a Command
├── app/      CryptoService (facade) + command/ (Encrypt/Decrypt/BruteForce)
├── cipher/   Cipher, Caesar/Vigenere/Rot13/Atbash, CipherFactory
├── alphabet/ Alphabet, CharacterRing, Alphabets (registry), LanguageDetector
├── crack/    Cracker, CaesarCracker, FitnessScorer, Frequency/Dictionary, LanguageProfile
├── io/       TextReader, Plain/Markdown/Gzip, TextReaders (registry), TextWriter, OutputNaming
└── error/    exception hierarchy (CryptanalysisException, InvalidArgumentsException, …)
```

Each package has one responsibility; each class fits in working memory.

## CLI & backward compatibility

picocli models `-e/-d/-b` as a **required mutually-exclusive `@ArgGroup`**, `-f`
required, `-k` validated as required-iff-encrypt/decrypt (conditional check after
parse, since picocli annotations cannot express it directly). New additive flags:

- `-c/--cipher caesar|vigenere|rot13|atbash` (default `caesar`)
- `--keyword <word>` (for Vigenère)
- `-a/--alphabet en|ua|ru|auto` (default: composite/all-rings — identical to old behavior)
- auto `--help` / `--version` from picocli

**Validation mapping (keeps `ValidationTests` green):**

| Scenario | Mechanism |
|---|---|
| missing `-k` (with `-e`/`-d`) | post-parse conditional check → `InvalidArgumentsException` |
| missing `-f` | picocli required option → `ParameterException` |
| missing command | `@ArgGroup` multiplicity = 1 → `ParameterException` |
| unknown flag (`-x`) | picocli unmatched-argument → `ParameterException` |
| non-numeric key | picocli `TypeConversionException` |

All caught in `Main`, printed cleanly, **no file written, never propagated.**

### Build changes

`pom.xml`: add `info.picocli:picocli` dependency + `maven-shade-plugin` to produce
a runnable fat jar so `java -jar` and CI's `mvn package` stay green. Update jar
`Main-Class` if needed.

## Data flow

```
Main
 → picocli parse
 → CryptoCli builds a Command (operation + cipher spec + alphabet/lang + path)
 → CryptoService:
      TextReaders.pick(path).read()
      ├─ encrypt/decrypt: CipherFactory.create(spec).encrypt|decrypt(text)
      └─ brute force:     LanguageDetector → CaesarCracker.crack(text) → plaintext
      OutputNaming.forEncrypt|forDecrypt(path)
      TextWriter.write(outputPath, result)
```

## Error handling

Small exception hierarchy under `error/`:

- `CryptanalysisException` (base, unchecked)
- `InvalidArgumentsException`
- `UnreadableSourceException`

picocli parse errors and domain exceptions are both caught in `Main`, reported
with a clean message; **no partial output on error**; nothing propagates.

## Testing

- **`MainTest` stays byte-for-byte green** — the regression proof. Not reformatted.
- New unit tests via TDD: `AlphabetTest`, each `*CipherTest`,
  `CaesarCrackerTest`, `TextReadersTest`, `OutputNamingTest`.
- New integration tests for `--cipher`, `--keyword`, `--alphabet`, and `--help`.
- Existing `-Dtest.lang.ua=true` gating for Ukrainian cases is preserved.

## Out of scope (YAGNI)

- Runtime SPI/`ServiceLoader` plugin discovery (architecture leaves room; not built now).
- GUI / interactive REPL.
- Ciphers beyond the four named; alphabets beyond EN/UA/RU.
- Network or streaming I/O.
```
