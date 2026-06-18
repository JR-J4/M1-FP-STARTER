# Caesar Cryptanalyzer — Clean-Code Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refine the existing OOP cryptanalyzer into a cleaner, more SOLID design — manual DI via a composition root, registries replacing switch-factories, a unified `Language`, a real PDFBox-backed PDF reader, and a brute-force bug fix — while keeping `MainTest` byte-for-byte green.

**Architecture:** Each task is a compiling, green slice that updates every reference it touches. Structural pieces (Language unification, cipher/scorer catalogs, IO interfaces) come first; the composition root + constructor injection come last and only relocate `new` calls. No DI framework, no JPMS, no SPI.

**Tech Stack:** Java 17, Maven wrapper (`./mvnw`), JUnit 5.9.2, picocli 4.7.6, maven-shade-plugin 3.6.0, Apache PDFBox 3.0.3.

## Global Constraints

- Java source/target: **17**. Package root: **`ua.com.javarush.j4`**. All files UTF-8.
- `src/test/java/.../MainTest.java` is the **locked contract** — never edit/reformat it; it stays green with `./mvnw test` and `./mvnw test -Dtest.lang.ua=true`.
- Legacy CLI unchanged: `-e | -d | -b`, `-k <int>` (required for `-e`/`-d` with caesar; absent for `-b`), `-f <path>`. Additive flags preserved: `-c/--cipher`, `--keyword`, `-a/--alphabet`, `-s/--scorer`.
- `Main.main` never propagates exceptions for bad input / missing files; **no output file written on error**.
- Cipher model unchanged: case-preserving rings (EN=26, UA=33), keys mod ring size, shifts never cross case; brute-force recovers the original exactly.
- **Ring-sharing keystone** (ARCHITECTURE.md §1): `Alphabets.DEFAULT` and per-language alphabets share the same `CharacterRing` instances. `Language` composes the shared `Alphabets.*` instance — do not redefine rings.
- No DI framework, no `module-info`, no `ServiceLoader`. Manual constructor injection + in-code registries only.
- Commit after every task with the exact message shown.

---

### Task 1: Build setup — PDFBox dependency + .gitignore

**Files:**
- Modify: `pom.xml`
- Create/Modify: `.gitignore`

**Interfaces:**
- Consumes: nothing.
- Produces: Apache PDFBox 3.0.3 on the compile classpath (bundled by shade); ignored build artifacts.

- [ ] **Step 1: Confirm the baseline is green**

Run: `./mvnw test`
Expected: BUILD SUCCESS (English-only suite green; Ukrainian cases skipped).

- [ ] **Step 2: Add the PDFBox dependency** to `pom.xml`, inside `<dependencies>`, after the picocli dependency:

```xml
        <dependency>
            <groupId>org.apache.pdfbox</groupId>
            <artifactId>pdfbox</artifactId>
            <version>3.0.3</version>
        </dependency>
```

- [ ] **Step 3: Ensure build artifacts are ignored.** Append these lines to `.gitignore` (create the file if missing) if not already present:

```
target/
dependency-reduced-pom.xml
```

- [ ] **Step 4: Verify it resolves and compiles**

Run: `./mvnw -q -DskipTests test-compile`
Expected: BUILD SUCCESS (PDFBox + fontbox downloaded).

- [ ] **Step 5: Commit**

```bash
git add pom.xml .gitignore
git commit -m "build: add Apache PDFBox dependency and ignore build artifacts"
```

---

### Task 2: Unify the language concept — `LanguageProfile`→`Language`, add `Languages.byCode`

**Files:**
- Rename: `crack/LanguageProfile.java` → `crack/Language.java`
- Rename: `crack/LanguageProfiles.java` → `crack/Languages.java`
- Modify: `crack/DictionaryScorer.java`, `crack/FrequencyScorer.java`, `crack/LanguageDetector.java`, `app/command/BruteForceCommand.java`, `app/CryptoService.java`
- Modify (tests): `crack/ScoringTest.java`, `crack/CaesarCrackerTest.java`
- Create (test): `crack/LanguagesTest.java`

**Interfaces:**
- Consumes: `alphabet/Alphabet`, `alphabet/Alphabets` (unchanged).
- Produces:
  - `record Language(String code, Alphabet alphabet, Set<String> commonWords, Map<Character,Double> letterFrequencies, Set<Character> distinctive)`.
  - `Languages` with constants `ENGLISH`, `UKRAINIAN`, `RUSSIAN`, `static List<Language> all()`, and `static Optional<Language> byCode(String code)` — `en/ua/ru/english/ukrainian/russian` → that `Language`; `default`/`auto` → `Optional.empty()`; anything else → `InvalidArgumentsException`.

- [ ] **Step 1: Rename the record file and type**

```bash
git mv src/main/java/ua/com/javarush/j4/crack/LanguageProfile.java \
       src/main/java/ua/com/javarush/j4/crack/Language.java
```
Then in `Language.java` rename the type `LanguageProfile` → `Language` (the record header becomes `public record Language(...)`). The 5 components and the existing Javadoc stay identical.

- [ ] **Step 2: Rename the registry file and type, add `byCode`**

```bash
git mv src/main/java/ua/com/javarush/j4/crack/LanguageProfiles.java \
       src/main/java/ua/com/javarush/j4/crack/Languages.java
```
In `Languages.java`: rename the class `LanguageProfiles` → `Languages`; change every `new LanguageProfile(...)` to `new Language(...)`; change the field/return types and `all()`'s return from `List<LanguageProfile>` to `List<Language>`. The word lists, frequency maps, and distinctive sets stay byte-for-byte identical (they already reference `Alphabets.ENGLISH/UKRAINIAN/RUSSIAN` — keep that). Add these imports and method:

```java
import ua.com.javarush.j4.error.InvalidArgumentsException;
import java.util.Locale;
import java.util.Optional;
```

```java
    /**
     * Resolves a language by code. {@code default}/{@code auto} mean "no specific
     * language" (composite alphabet / auto-detect) and return empty; an unknown
     * code is rejected.
     */
    public static Optional<Language> byCode(String code) {
        return switch (code.toLowerCase(Locale.ROOT)) {
            case "en", "english" -> Optional.of(ENGLISH);
            case "ua", "ukrainian" -> Optional.of(UKRAINIAN);
            case "ru", "russian" -> Optional.of(RUSSIAN);
            case "default", "auto" -> Optional.empty();
            default -> throw new InvalidArgumentsException("Unknown language/alphabet: " + code);
        };
    }
```

- [ ] **Step 3: Update consumers to the new type name.** In each file replace `LanguageProfile` with `Language` and `LanguageProfiles` with `Languages`:
  - `crack/DictionaryScorer.java`: constructor param type `Language profile`.
  - `crack/FrequencyScorer.java`: constructor param type `Language profile`.
  - `crack/LanguageDetector.java`: `detect(...)` returns `Language`; local `best`/loop type `Language`; `Languages.ENGLISH`, `Languages.all()`.
  - `app/command/BruteForceCommand.java`: field/param `Language forcedProfile`; local `Language profile`.
  - `app/CryptoService.java`: `forcedProfile(...)` return type `Language`; `LanguageProfiles.ENGLISH/UKRAINIAN/RUSSIAN` → `Languages.*`.

- [ ] **Step 4: Update the tests that reference the old names.** In `crack/ScoringTest.java` and `crack/CaesarCrackerTest.java`, replace `LanguageProfiles` with `Languages` and `LanguageProfile` with `Language` (e.g. `Languages.ENGLISH`, `new DictionaryScorer(Languages.ENGLISH)`, `Languages.UKRAINIAN.alphabet()`).

- [ ] **Step 5: Write the new `LanguagesTest`** `src/test/java/ua/com/javarush/j4/crack/LanguagesTest.java`

```java
package ua.com.javarush.j4.crack;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import static org.junit.jupiter.api.Assertions.*;

class LanguagesTest {

    @Test
    void resolvesKnownCodes() {
        assertEquals(Languages.ENGLISH, Languages.byCode("en").orElseThrow());
        assertEquals(Languages.UKRAINIAN, Languages.byCode("UA").orElseThrow());
        assertEquals(Languages.RUSSIAN, Languages.byCode("russian").orElseThrow());
    }

    @Test
    void defaultAndAutoMeanNoSpecificLanguage() {
        assertTrue(Languages.byCode("default").isEmpty());
        assertTrue(Languages.byCode("auto").isEmpty());
    }

    @Test
    void unknownCodeIsRejected() {
        assertThrows(InvalidArgumentsException.class, () -> Languages.byCode("klingon"));
    }
}
```

- [ ] **Step 6: Verify the full suite is green**

Run: `./mvnw test -Dtest.lang.ua=true`
Expected: BUILD SUCCESS, 0 failures, 0 skipped (rename is behavior-preserving; `LanguagesTest` passes).

- [ ] **Step 7: Commit**

```bash
git add -A src/main/java/ua/com/javarush/j4/crack src/main/java/ua/com/javarush/j4/app src/test/java/ua/com/javarush/j4/crack
git commit -m "refactor: unify LanguageProfile into Language with byCode resolver"
```

---

### Task 3: Cipher catalog + spec, Vigenère keyword validation

**Files:**
- Create: `cipher/CipherSpec.java`, `cipher/CipherCatalog.java`
- Delete: `cipher/CipherFactory.java`
- Modify: `cipher/VigenereCipher.java`, `app/CryptoService.java`
- Rename (test): `cipher/CipherFactoryTest.java` → `cipher/CipherCatalogTest.java` (and add a Vigenère non-alphabet-keyword case)

**Interfaces:**
- Consumes: `cipher/Cipher`, `cipher/CaesarCipher`, `cipher/Rot13Cipher`, `cipher/AtbashCipher`, `cipher/VigenereCipher`, `alphabet/Alphabet`, `error/InvalidArgumentsException`.
- Produces:
  - `record CipherSpec(String cipherName, Integer key, String keyword)`.
  - `CipherCatalog` with nested `@FunctionalInterface CipherCreator { Cipher create(Alphabet alphabet, Integer key, String keyword); }`, a `register(String name, CipherCreator)` (used by the composition root later) OR a default-populated map, and `Cipher create(CipherSpec spec, Alphabet alphabet)`. Unknown name → `InvalidArgumentsException`. Caesar with `null` key → `InvalidArgumentsException`.

- [ ] **Step 1: Write the failing test** — rename the existing test and add a Vigenère-validation case.

```bash
git mv src/test/java/ua/com/javarush/j4/cipher/CipherFactoryTest.java \
       src/test/java/ua/com/javarush/j4/cipher/CipherCatalogTest.java
```
Replace the contents of `CipherCatalogTest.java` with:

```java
package ua.com.javarush.j4.cipher;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import static org.junit.jupiter.api.Assertions.*;

class CipherCatalogTest {

    private final CipherCatalog catalog = CipherCatalog.withDefaults();

    private Cipher create(String name, Integer key, String keyword) {
        return catalog.create(new CipherSpec(name, key, keyword), Alphabets.ENGLISH);
    }

    @Test
    void buildsCaesar() {
        assertEquals("BCD", create("caesar", 1, null).encrypt("ABC"));
    }

    @Test
    void caesarWithoutKeyIsRejected() {
        assertThrows(InvalidArgumentsException.class, () -> create("caesar", null, null));
    }

    @Test
    void rot13IsReversible() {
        Cipher c = create("rot13", null, null);
        assertEquals("URYYB", c.encrypt("HELLO"));
        assertEquals("HELLO", c.decrypt("URYYB"));
    }

    @Test
    void atbashIsSelfInverse() {
        Cipher c = create("atbash", null, null);
        assertEquals("ZYX", c.encrypt("ABC"));
        assertEquals("ABC", c.decrypt("ZYX"));
    }

    @Test
    void vigenereMatchesKnownVector() {
        Cipher c = create("vigenere", null, "KEY");
        assertEquals("RIJVS", c.encrypt("HELLO"));
        assertEquals("HELLO", c.decrypt("RIJVS"));
    }

    @Test
    void vigenereWithoutKeywordIsRejected() {
        assertThrows(InvalidArgumentsException.class, () -> create("vigenere", null, null));
    }

    @Test
    void vigenereWithNonAlphabetKeywordIsRejected() {
        assertThrows(InvalidArgumentsException.class, () -> create("vigenere", null, "KE1"));
    }

    @Test
    void unknownCipherIsRejected() {
        assertThrows(InvalidArgumentsException.class, () -> create("enigma", 1, null));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -q -Dtest=CipherCatalogTest test`
Expected: FAIL — `CipherCatalog`/`CipherSpec` do not exist; `CipherFactory` still present.

- [ ] **Step 3: Create `CipherSpec`**

`src/main/java/ua/com/javarush/j4/cipher/CipherSpec.java`:
```java
package ua.com.javarush.j4.cipher;

/** The user-facing cipher selection: which cipher, plus its key/keyword (alphabet resolved separately). */
public record CipherSpec(String cipherName, Integer key, String keyword) {
}
```

- [ ] **Step 4: Create `CipherCatalog`**

`src/main/java/ua/com/javarush/j4/cipher/CipherCatalog.java`:
```java
package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Registry of named cipher creators. Replaces the old switch-based factory. */
public final class CipherCatalog {

    /** Builds a configured cipher from an alphabet plus optional key/keyword. */
    @FunctionalInterface
    public interface CipherCreator {
        Cipher create(Alphabet alphabet, Integer key, String keyword);
    }

    private final Map<String, CipherCreator> creators = new HashMap<>();

    public void register(String name, CipherCreator creator) {
        creators.put(name.toLowerCase(Locale.ROOT), creator);
    }

    public Cipher create(CipherSpec spec, Alphabet alphabet) {
        CipherCreator creator = creators.get(spec.cipherName().toLowerCase(Locale.ROOT));
        if (creator == null) {
            throw new InvalidArgumentsException("Unknown cipher: " + spec.cipherName());
        }
        return creator.create(alphabet, spec.key(), spec.keyword());
    }

    /** The built-in cipher set. */
    public static CipherCatalog withDefaults() {
        CipherCatalog catalog = new CipherCatalog();
        catalog.register("caesar", (alphabet, key, keyword) -> new CaesarCipher(alphabet, requireKey(key)));
        catalog.register("rot13", (alphabet, key, keyword) -> new Rot13Cipher(alphabet));
        catalog.register("atbash", (alphabet, key, keyword) -> new AtbashCipher(alphabet));
        catalog.register("vigenere", (alphabet, key, keyword) -> new VigenereCipher(alphabet, keyword));
        return catalog;
    }

    private static int requireKey(Integer key) {
        if (key == null) {
            throw new InvalidArgumentsException("Caesar cipher requires a key (-k <int>)");
        }
        return key;
    }
}
```

- [ ] **Step 5: Add keyword validation to `VigenereCipher`.** Replace the constructor in `src/main/java/ua/com/javarush/j4/cipher/VigenereCipher.java` with:

```java
    public VigenereCipher(Alphabet alphabet, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new InvalidArgumentsException("Vigenère cipher requires a non-empty --keyword");
        }
        for (int i = 0; i < keyword.length(); i++) {
            if (alphabet.position(keyword.charAt(i)).isEmpty()) {
                throw new InvalidArgumentsException(
                        "Vigenère keyword must contain only alphabet letters: '" + keyword + "'");
            }
        }
        this.alphabet = alphabet;
        this.keyword = keyword;
    }
```

- [ ] **Step 6: Switch `CryptoService` to the catalog and delete `CipherFactory`.**

Delete the old factory:
```bash
git rm src/main/java/ua/com/javarush/j4/cipher/CipherFactory.java
```
In `app/CryptoService.java`: replace the import `ua.com.javarush.j4.cipher.CipherFactory` with `ua.com.javarush.j4.cipher.CipherCatalog` and `ua.com.javarush.j4.cipher.CipherSpec`; change the field
```java
    private final CipherFactory ciphers = new CipherFactory();
```
to
```java
    private final CipherCatalog ciphers = CipherCatalog.withDefaults();
```
and change the `cipher(request)` method body to build a `CipherSpec`:
```java
    private Cipher cipher(CryptoRequest request) {
        Alphabet alphabet = Alphabets.byName(request.alphabetName());
        return ciphers.create(
                new CipherSpec(request.cipherName(), request.key(), request.keyword()), alphabet);
    }
```
(`Alphabets.byName` and the `CryptoRequest` field names are unchanged in this task — they are refactored in Task 6.)

- [ ] **Step 7: Run tests to verify they pass**

Run: `./mvnw -q -Dtest=CipherCatalogTest test`
Expected: PASS (8 tests).

- [ ] **Step 8: Full suite green**

Run: `./mvnw test -Dtest.lang.ua=true`
Expected: BUILD SUCCESS, 0 failures, 0 skipped.

- [ ] **Step 9: Commit**

```bash
git add -A src/main/java/ua/com/javarush/j4/cipher src/main/java/ua/com/javarush/j4/app src/test/java/ua/com/javarush/j4/cipher
git commit -m "refactor: replace CipherFactory with CipherCatalog registry; validate Vigenere keyword"
```

---

### Task 4: Scorer catalog — extract scorer selection from the command

**Files:**
- Create: `crack/ScorerCatalog.java`
- Modify: `app/command/BruteForceCommand.java`, `app/CryptoService.java`
- Create (test): `crack/ScorerCatalogTest.java`

**Interfaces:**
- Consumes: `crack/FitnessScorer`, `crack/DictionaryScorer`, `crack/FrequencyScorer`, `crack/Language`, `crack/Languages`, `crack/LanguageDetector`, `crack/CaesarCracker`, `error/InvalidArgumentsException`.
- Produces:
  - `ScorerCatalog` with `register(String, Function<Language,FitnessScorer>)`, `FitnessScorer create(String name, Language language)` (unknown → `InvalidArgumentsException`), and `static ScorerCatalog withDefaults()` (dictionary, frequency).
  - `BruteForceCommand(Path input, LanguageDetector detector, Language forcedProfile, ScorerCatalog scorerCatalog, String scorerName, TextReaders readers, TextWriter writer, OutputNaming naming)` — builds the scorer via the catalog after detecting language.

- [ ] **Step 1: Write the failing test** `src/test/java/ua/com/javarush/j4/crack/ScorerCatalogTest.java`

```java
package ua.com.javarush.j4.crack;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import static org.junit.jupiter.api.Assertions.*;

class ScorerCatalogTest {

    private final ScorerCatalog catalog = ScorerCatalog.withDefaults();

    @Test
    void buildsDictionaryScorer() {
        assertTrue(catalog.create("dictionary", Languages.ENGLISH) instanceof DictionaryScorer);
    }

    @Test
    void buildsFrequencyScorer() {
        assertTrue(catalog.create("frequency", Languages.ENGLISH) instanceof FrequencyScorer);
    }

    @Test
    void unknownScorerIsRejected() {
        assertThrows(InvalidArgumentsException.class, () -> catalog.create("magic", Languages.ENGLISH));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -q -Dtest=ScorerCatalogTest test`
Expected: FAIL — `ScorerCatalog` does not exist.

- [ ] **Step 3: Create `ScorerCatalog`**

`src/main/java/ua/com/javarush/j4/crack/ScorerCatalog.java`:
```java
package ua.com.javarush.j4.crack;

import ua.com.javarush.j4.error.InvalidArgumentsException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** Registry of named fitness scorers for brute-force. Replaces the in-command switch. */
public final class ScorerCatalog {

    private final Map<String, Function<Language, FitnessScorer>> creators = new HashMap<>();

    public void register(String name, Function<Language, FitnessScorer> creator) {
        creators.put(name.toLowerCase(Locale.ROOT), creator);
    }

    public FitnessScorer create(String name, Language language) {
        Function<Language, FitnessScorer> creator = creators.get(name.toLowerCase(Locale.ROOT));
        if (creator == null) {
            throw new InvalidArgumentsException(
                    "Unknown scorer '" + name + "'. Valid values: dictionary, frequency");
        }
        return creator.apply(language);
    }

    public static ScorerCatalog withDefaults() {
        ScorerCatalog catalog = new ScorerCatalog();
        catalog.register("dictionary", DictionaryScorer::new);
        catalog.register("frequency", FrequencyScorer::new);
        return catalog;
    }
}
```

- [ ] **Step 4: Refactor `BruteForceCommand`** to receive a `ScorerCatalog` and build the scorer via it (no private switch). Replace `src/main/java/ua/com/javarush/j4/app/command/BruteForceCommand.java` with:

```java
package ua.com.javarush.j4.app.command;

import ua.com.javarush.j4.crack.CaesarCracker;
import ua.com.javarush.j4.crack.FitnessScorer;
import ua.com.javarush.j4.crack.Language;
import ua.com.javarush.j4.crack.LanguageDetector;
import ua.com.javarush.j4.crack.ScorerCatalog;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.nio.file.Path;

/** Brute-force: detect language (unless one is forced), sweep keys, write best decryption. */
public final class BruteForceCommand extends CryptoCommand {
    private final LanguageDetector detector;
    private final Language forcedProfile; // null => auto-detect
    private final ScorerCatalog scorerCatalog;
    private final String scorerName;

    public BruteForceCommand(Path input, LanguageDetector detector, Language forcedProfile,
                             ScorerCatalog scorerCatalog, String scorerName,
                             TextReaders readers, TextWriter writer, OutputNaming naming) {
        super(input, readers, writer, naming);
        this.detector = detector;
        this.forcedProfile = forcedProfile;
        this.scorerCatalog = scorerCatalog;
        this.scorerName = scorerName;
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
        Language profile = forcedProfile != null ? forcedProfile : detector.detect(text);
        FitnessScorer scorer = scorerCatalog.create(scorerName, profile);
        CaesarCracker cracker = new CaesarCracker(profile.alphabet(), scorer);
        return cracker.crack(text).plaintext();
    }

    @Override
    protected Path outputPath(OutputNaming naming, Path input) {
        return naming.forDecrypt(input);
    }
}
```

- [ ] **Step 5: Update `CryptoService`** to own a `ScorerCatalog`, resolve the forced language via `Languages.byCode`, and pass them to `BruteForceCommand`. In `app/CryptoService.java`:
  - Add imports `ua.com.javarush.j4.crack.ScorerCatalog` and `ua.com.javarush.j4.crack.Languages`.
  - Add field: `private final ScorerCatalog scorers = ScorerCatalog.withDefaults();`
  - Replace the `BRUTE_FORCE` arm of `command(request)` with:
    ```java
            case BRUTE_FORCE -> new BruteForceCommand(
                    file, detector,
                    Languages.byCode(request.alphabetName()).orElse(null),
                    scorers, request.scorerName(), readers, writer, naming);
    ```
  - Delete the now-unused `forcedProfile(...)` private method.

- [ ] **Step 6: Run tests to verify they pass**

Run: `./mvnw -q -Dtest='ScorerCatalogTest,CryptoServiceTest' test`
Expected: PASS.

- [ ] **Step 7: Full suite green**

Run: `./mvnw test -Dtest.lang.ua=true`
Expected: BUILD SUCCESS, 0 failures, 0 skipped.

- [ ] **Step 8: Commit**

```bash
git add -A src/main/java/ua/com/javarush/j4/crack src/main/java/ua/com/javarush/j4/app src/test/java/ua/com/javarush/j4/crack
git commit -m "refactor: extract ScorerCatalog; build brute-force scorer via registry"
```

---

### Task 5: IO — `TextWriter` interface + `FileTextWriter`, injectable `TextReaders`, real `PdfReader`

**Files:**
- Modify: `io/TextWriter.java` (→ interface), `io/TextReaders.java`
- Create: `io/FileTextWriter.java`, `io/PdfReader.java`
- Modify: `app/CryptoService.java`
- Modify (test): `io/TextReadersTest.java`
- Create (test): `io/PdfReaderTest.java`

**Interfaces:**
- Consumes: `io/TextReader`, `io/PlainTextReader`, `io/GzipTextReader`, `io/MarkdownReader`.
- Produces:
  - `interface TextWriter { void write(Path path, String content) throws IOException; }`
  - `final class FileTextWriter implements TextWriter` (UTF-8 file write).
  - `final class PdfReader implements TextReader` (`.pdf`; extracts text via PDFBox `Loader` + `PDFTextStripper`).
  - `TextReaders(List<TextReader> readers)` constructor + `static TextReaders withDefaults()` returning the built-in list `[gzip, markdown, pdf, plain]` with a plain fallback.

- [ ] **Step 1: Write the failing `PdfReaderTest`** `src/test/java/ua/com/javarush/j4/io/PdfReaderTest.java`

```java
package ua.com.javarush.j4.io;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PdfReaderTest {

    private final PdfReader reader = new PdfReader();

    @Test
    void supportsPdfExtension() {
        assertTrue(reader.supports(Path.of("doc.pdf")));
        assertFalse(reader.supports(Path.of("doc.txt")));
    }

    @Test
    void extractsTextFromPdf(@TempDir Path dir) throws IOException {
        Path pdf = dir.resolve("hello.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Hello PDF World");
                cs.endText();
            }
            doc.save(pdf.toFile());
        }

        assertTrue(reader.read(pdf).contains("Hello PDF World"),
                "extracted text should contain the written line");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -q -Dtest=PdfReaderTest test`
Expected: FAIL — `PdfReader` does not exist.

- [ ] **Step 3: Create the real `PdfReader`**

`src/main/java/ua/com/javarush/j4/io/PdfReader.java`:
```java
package ua.com.javarush.j4.io;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/** Reads PDF documents and extracts their text with Apache PDFBox. */
public final class PdfReader implements TextReader {
    @Override
    public boolean supports(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    @Override
    public String read(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }
}
```

- [ ] **Step 4: Convert `TextWriter` to an interface**

Replace `src/main/java/ua/com/javarush/j4/io/TextWriter.java` with:
```java
package ua.com.javarush.j4.io;

import java.io.IOException;
import java.nio.file.Path;

/** Writes text content to a destination. */
public interface TextWriter {
    void write(Path path, String content) throws IOException;
}
```

- [ ] **Step 5: Create `FileTextWriter`**

`src/main/java/ua/com/javarush/j4/io/FileTextWriter.java`:
```java
package ua.com.javarush.j4.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes UTF-8 text content to a file. */
public final class FileTextWriter implements TextWriter {
    @Override
    public void write(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 6: Make `TextReaders` injectable with a default factory**

Replace `src/main/java/ua/com/javarush/j4/io/TextReaders.java` with:
```java
package ua.com.javarush.j4.io;

import java.nio.file.Path;
import java.util.List;

/** Picks the right reader for a path by extension; falls back to plain text. */
public final class TextReaders {
    private final List<TextReader> readers;
    private final TextReader plain;

    public TextReaders(List<TextReader> readers) {
        this.readers = List.copyOf(readers);
        this.plain = readers.stream()
                .filter(r -> r instanceof PlainTextReader)
                .findFirst()
                .orElseGet(PlainTextReader::new);
    }

    public TextReader pick(Path path) {
        return readers.stream()
                .filter(reader -> reader.supports(path))
                .findFirst()
                .orElse(plain);
    }

    /** The built-in reader set, ordered so specific formats win before the plain fallback. */
    public static TextReaders withDefaults() {
        return new TextReaders(List.of(
                new GzipTextReader(), new MarkdownReader(), new PdfReader(), new PlainTextReader()));
    }
}
```

- [ ] **Step 7: Update `CryptoService` IO wiring.** In `app/CryptoService.java` change the two IO fields:
```java
    private final TextReaders readers = TextReaders.withDefaults();
    private final TextWriter writer = new FileTextWriter();
```
and add the import `ua.com.javarush.j4.io.FileTextWriter`. (Full DI relocation happens in Task 7.)

- [ ] **Step 8: Update `TextReadersTest`** for the new constructor. In `src/test/java/ua/com/javarush/j4/io/TextReadersTest.java` replace `new TextReaders()` with `TextReaders.withDefaults()` (every occurrence). Add one case:

```java
    @Test
    void picksPdfReaderForPdf() {
        assertTrue(TextReaders.withDefaults().pick(java.nio.file.Path.of("a.pdf")) instanceof PdfReader);
    }
```

- [ ] **Step 9: Run tests to verify they pass**

Run: `./mvnw -q -Dtest='PdfReaderTest,TextReadersTest' test`
Expected: PASS.

- [ ] **Step 10: Full suite green**

Run: `./mvnw test -Dtest.lang.ua=true`
Expected: BUILD SUCCESS, 0 failures, 0 skipped.

- [ ] **Step 11: Commit**

```bash
git add -A src/main/java/ua/com/javarush/j4/io src/main/java/ua/com/javarush/j4/app src/test/java/ua/com/javarush/j4/io
git commit -m "feat: TextWriter interface + FileTextWriter, injectable TextReaders, real PDFBox PdfReader"
```

---

### Task 6: Request value objects, remove `Alphabets.byName`, brute-force-non-caesar fix

**Files:**
- Modify: `app/CryptoRequest.java`, `cli/CryptoCli.java`, `app/CryptoService.java`, `alphabet/Alphabets.java`
- Modify (test): `app/CryptoServiceTest.java`, `cli/CryptoCliTest.java`, `alphabet/AlphabetTest.java`

**Interfaces:**
- Consumes: `cipher/CipherSpec` (Task 3), `crack/Languages` (Task 2), `alphabet/Alphabets`.
- Produces:
  - `record CryptoRequest(Operation operation, Path file, CipherSpec cipherSpec, String languageCode, String scorerName)`.
  - `CryptoService` resolves the cipher alphabet via `Languages.byCode(languageCode).map(Language::alphabet).orElse(Alphabets.DEFAULT)`; brute-force on a non-caesar cipher throws `InvalidArgumentsException`.
  - `Alphabets.byName` removed.

- [ ] **Step 1: Write/extend failing tests.** Add to `src/test/java/ua/com/javarush/j4/cli/CryptoCliTest.java`:

```java
    @Test
    void bruteForceRejectsNonCaesarCipher(@TempDir Path dir) throws IOException {
        Path input = dir.resolve("bf.txt");
        Files.writeString(input, "HELLO");
        List<Path> before = list(dir);

        assertDoesNotThrow(() ->
                new CryptoCli().run(new String[]{"-b", "-c", "vigenere", "--keyword", "KEY", "-f", input.toString()}));
        assertEquals(before, list(dir), "no file should be written when brute-force rejects the cipher");
    }
```
(Reuse the existing `list(...)` helper and imports in that test file.)

- [ ] **Step 2: Run to verify it fails**

Run: `./mvnw -q -Dtest=CryptoCliTest test`
Expected: FAIL — currently `-b -c vigenere` silently Caesar-cracks and writes a file.

- [ ] **Step 3: Reshape `CryptoRequest`**

Replace `src/main/java/ua/com/javarush/j4/app/CryptoRequest.java` with:
```java
package ua.com.javarush.j4.app;

import ua.com.javarush.j4.cipher.CipherSpec;

import java.nio.file.Path;

/** A fully-parsed user request, independent of how it was parsed. */
public record CryptoRequest(
        Operation operation,
        Path file,
        CipherSpec cipherSpec,
        String languageCode,
        String scorerName) {
}
```

- [ ] **Step 4: Update `CryptoCli.call()`** to build a `CipherSpec` and pass `languageCode`. In `src/main/java/ua/com/javarush/j4/cli/CryptoCli.java` add the import `ua.com.javarush.j4.cipher.CipherSpec` and replace the `call()` body's request construction:
```java
    @Override
    public Integer call() throws Exception {
        Operation operation = selectedOperation();
        new CryptoService().execute(new CryptoRequest(
                operation, file, new CipherSpec(cipher, key, keyword), alphabet, scorer));
        return 0;
    }
```
(The option fields stay named `cipher`, `key`, `keyword`, `alphabet`, `scorer`; only the request assembly changes.)

- [ ] **Step 5: Update `CryptoService`** to use the new request shape, resolve the alphabet via `Languages`, and reject non-caesar brute-force. Replace the body of `app/CryptoService.java`'s `command`, `cipher`, and add a guard. The relevant methods become:

```java
    public Path execute(CryptoRequest request) throws IOException {
        return command(request).execute();
    }

    private CryptoCommand command(CryptoRequest request) {
        Path file = request.file();
        return switch (request.operation()) {
            case ENCRYPT -> new EncryptCommand(file, cipher(request), readers, writer, naming);
            case DECRYPT -> new DecryptCommand(file, cipher(request), readers, writer, naming);
            case BRUTE_FORCE -> bruteForce(request, file);
        };
    }

    private Cipher cipher(CryptoRequest request) {
        Alphabet alphabet = Languages.byCode(request.languageCode())
                .map(Language::alphabet)
                .orElse(Alphabets.DEFAULT);
        return ciphers.create(request.cipherSpec(), alphabet);
    }

    private CryptoCommand bruteForce(CryptoRequest request, Path file) {
        if (!"caesar".equalsIgnoreCase(request.cipherSpec().cipherName())) {
            throw new InvalidArgumentsException("Brute-force is supported only for the caesar cipher");
        }
        return new BruteForceCommand(
                file, detector,
                Languages.byCode(request.languageCode()).orElse(null),
                scorers, request.scorerName(), readers, writer, naming);
    }
```
Update imports in `CryptoService.java`: add `ua.com.javarush.j4.crack.Language`, `ua.com.javarush.j4.error.InvalidArgumentsException`; keep `Alphabets`; remove any now-unused imports. (`Alphabets.byName` is no longer called.)

- [ ] **Step 6: Remove `Alphabets.byName`** from `src/main/java/ua/com/javarush/j4/alphabet/Alphabets.java` (delete the `byName` method and the now-unused `InvalidArgumentsException`/`Locale` imports if they become unused).

- [ ] **Step 7: Update the tests touching these.**
  - `src/test/java/ua/com/javarush/j4/alphabet/AlphabetTest.java`: delete the `byNameRejectsUnknownAlphabet` test and the `InvalidArgumentsException` import (unknown-code rejection is now covered by `LanguagesTest`).
  - `src/test/java/ua/com/javarush/j4/app/CryptoServiceTest.java`: update every `new CryptoRequest(...)` to the new shape, e.g.:
    - encrypt: `new CryptoRequest(Operation.ENCRYPT, input, new CipherSpec("caesar", 5, null), "default", "dictionary")`
    - decrypt: `new CryptoRequest(Operation.DECRYPT, encrypted, new CipherSpec("caesar", 5, null), "default", "dictionary")`
    - brute-force auto: `new CryptoRequest(Operation.BRUTE_FORCE, encrypted, new CipherSpec("caesar", null, null), "auto", "dictionary")`
    - Vigenère: `new CryptoRequest(Operation.ENCRYPT, input, new CipherSpec("vigenere", null, "LEMON"), "en", "dictionary")`
    Add the import `ua.com.javarush.j4.cipher.CipherSpec`.

- [ ] **Step 8: Run tests to verify they pass**

Run: `./mvnw -q -Dtest='CryptoCliTest,CryptoServiceTest,AlphabetTest' test`
Expected: PASS (incl. the new brute-force-rejects-non-caesar case).

- [ ] **Step 9: Full suite green**

Run: `./mvnw test -Dtest.lang.ua=true`
Expected: BUILD SUCCESS, 0 failures, 0 skipped.

- [ ] **Step 10: Commit**

```bash
git add -A src/main/java src/test/java
git commit -m "refactor: CipherSpec + languageCode in CryptoRequest; reject non-caesar brute-force; drop Alphabets.byName"
```

---

### Task 7: Composition root + constructor injection

**Files:**
- Create: `Composition.java`
- Modify: `Main.java`, `cli/CryptoCli.java`, `app/CryptoService.java`
- Modify (test): `app/CryptoServiceTest.java`
- Create (test): `CompositionTest.java`

**Interfaces:**
- Consumes: everything built so far.
- Produces:
  - `CryptoService(CipherCatalog ciphers, ScorerCatalog scorers, LanguageDetector detector, TextReaders readers, TextWriter writer, OutputNaming naming)` — all collaborators injected; no internal `new`.
  - `CryptoCli(CryptoService service)` — service injected.
  - `Composition` with `CryptoService service()` and `CryptoCli cli()`.
  - `Main.main` delegates to `new Composition().cli().run(args)`.

- [ ] **Step 1: Make `CryptoService` constructor-injected.** Replace the field declarations and add a constructor in `app/CryptoService.java`. The fields become injected (remove the `= ...` initializers):

```java
    private final CipherCatalog ciphers;
    private final ScorerCatalog scorers;
    private final LanguageDetector detector;
    private final TextReaders readers;
    private final TextWriter writer;
    private final OutputNaming naming;

    public CryptoService(CipherCatalog ciphers, ScorerCatalog scorers, LanguageDetector detector,
                         TextReaders readers, TextWriter writer, OutputNaming naming) {
        this.ciphers = ciphers;
        this.scorers = scorers;
        this.detector = detector;
        this.readers = readers;
        this.writer = writer;
        this.naming = naming;
    }
```
Keep `execute`, `command`, `cipher`, `bruteForce` exactly as in Task 6. Remove imports that are now only used by the deleted initializers if any become unused (`FileTextWriter` moves to `Composition`).

- [ ] **Step 2: Make `CryptoCli` constructor-injected.** In `src/main/java/ua/com/javarush/j4/cli/CryptoCli.java`:
  - Add a field and constructor:
    ```java
        private final CryptoService service;

        public CryptoCli(CryptoService service) {
            this.service = service;
        }
    ```
  - In `call()` replace `new CryptoService().execute(...)` with `service.execute(...)`.

- [ ] **Step 3: Create the composition root**

`src/main/java/ua/com/javarush/j4/Composition.java`:
```java
package ua.com.javarush.j4;

import ua.com.javarush.j4.app.CryptoService;
import ua.com.javarush.j4.cipher.CipherCatalog;
import ua.com.javarush.j4.cli.CryptoCli;
import ua.com.javarush.j4.crack.LanguageDetector;
import ua.com.javarush.j4.crack.ScorerCatalog;
import ua.com.javarush.j4.io.FileTextWriter;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;

/**
 * The composition root: the single place that constructs and wires the object
 * graph. Every other class receives its collaborators via the constructor.
 */
public final class Composition {

    public CryptoService service() {
        return new CryptoService(
                CipherCatalog.withDefaults(),
                ScorerCatalog.withDefaults(),
                new LanguageDetector(),
                TextReaders.withDefaults(),
                new FileTextWriter(),
                new OutputNaming());
    }

    public CryptoCli cli() {
        return new CryptoCli(service());
    }
}
```

- [ ] **Step 4: Rewire `Main`**

Replace `src/main/java/ua/com/javarush/j4/Main.java` with:
```java
package ua.com.javarush.j4;

/**
 * Entry point. Builds the object graph via the composition root and hands it to
 * the picocli front end, which parses arguments and runs the requested command.
 * Never propagates exceptions for bad input.
 */
public class Main {
    public static void main(String[] args) {
        new Composition().cli().run(args);
    }
}
```

- [ ] **Step 5: Update `CryptoServiceTest` to build via injected fakes** (demonstrates DI testability). Replace `src/test/java/ua/com/javarush/j4/app/CryptoServiceTest.java` with:

```java
package ua.com.javarush.j4.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.com.javarush.j4.cipher.CipherCatalog;
import ua.com.javarush.j4.cipher.CipherSpec;
import ua.com.javarush.j4.crack.LanguageDetector;
import ua.com.javarush.j4.crack.ScorerCatalog;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.PlainTextReader;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {

    private CryptoService service() {
        return new CryptoService(
                CipherCatalog.withDefaults(),
                ScorerCatalog.withDefaults(),
                new LanguageDetector(),
                new TextReaders(List.of(new PlainTextReader())),
                new RecordingWriter(),
                new OutputNaming());
    }

    /** A TextWriter that records the last write — proves the service is testable without disk. */
    private static final class RecordingWriter implements TextWriter {
        Path path;
        String content;

        @Override
        public void write(Path path, String content) {
            this.path = path;
            this.content = content;
        }
    }

    @Test
    void encryptWritesShiftedTextToEncryptedPath(@TempDir Path dir) throws IOException {
        Path input = dir.resolve("msg.txt");
        Files.writeString(input, "ABC");
        RecordingWriter writer = new RecordingWriter();
        CryptoService service = new CryptoService(
                CipherCatalog.withDefaults(), ScorerCatalog.withDefaults(), new LanguageDetector(),
                new TextReaders(List.of(new PlainTextReader())), writer, new OutputNaming());

        service.execute(new CryptoRequest(
                Operation.ENCRYPT, input, new CipherSpec("caesar", 1, null), "default", "dictionary"));

        assertEquals("BCD", writer.content);
        assertTrue(writer.path.getFileName().toString().contains("[ENCRYPTED]"));
    }

    @Test
    void bruteForceRecoversEnglishWithAutoDetection(@TempDir Path dir) throws IOException {
        String original = "The quick brown fox jumps over the lazy dog and the cat.";
        Path input = dir.resolve("secret.txt");
        Files.writeString(input, original);
        // encrypt to disk first using a real file writer path via the service-under-test's cipher
        Path encrypted = dir.resolve("secret [ENCRYPTED].txt");
        Files.writeString(encrypted,
                new ua.com.javarush.j4.cipher.CaesarCipher(
                        ua.com.javarush.j4.alphabet.Alphabets.DEFAULT, 9).encrypt(original));

        RecordingWriter writer = new RecordingWriter();
        CryptoService service = new CryptoService(
                CipherCatalog.withDefaults(), ScorerCatalog.withDefaults(), new LanguageDetector(),
                new TextReaders(List.of(new PlainTextReader())), writer, new OutputNaming());

        service.execute(new CryptoRequest(
                Operation.BRUTE_FORCE, encrypted, new CipherSpec("caesar", null, null), "auto", "dictionary"));

        assertEquals(original, writer.content);
    }

    @Test
    void bruteForceRejectsNonCaesar(@TempDir Path dir) throws IOException {
        Path input = dir.resolve("v.txt");
        Files.writeString(input, "HELLO");
        assertThrows(RuntimeException.class, () -> service().execute(new CryptoRequest(
                Operation.BRUTE_FORCE, input, new CipherSpec("vigenere", null, "KEY"), "auto", "dictionary")));
    }
}
```

- [ ] **Step 6: Write `CompositionTest`** `src/test/java/ua/com/javarush/j4/CompositionTest.java`

```java
package ua.com.javarush.j4;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CompositionTest {

    @Test
    void buildsAFullyWiredCli() {
        assertNotNull(new Composition().cli(), "composition root should assemble the CLI");
        assertNotNull(new Composition().service(), "composition root should assemble the service");
    }
}
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `./mvnw -q -Dtest='CryptoServiceTest,CompositionTest,CryptoCliTest' test`
Expected: PASS.

- [ ] **Step 8: Full suite green (the MainTest contract exercises Main → Composition)**

Run: `./mvnw test -Dtest.lang.ua=true`
Expected: BUILD SUCCESS, 0 failures, 0 skipped.

- [ ] **Step 9: Commit**

```bash
git add -A src/main/java src/test/java
git commit -m "refactor: add composition root and constructor injection across the graph"
```

---

### Task 8: Docs + final packaging verification

**Files:**
- Modify: `ARCHITECTURE.md`, `CLAUDE.md`
- Verify: build + jar (manual, no new test file).

**Interfaces:**
- Consumes: the finished application.
- Produces: docs describing the DI/registry design; a verified runnable fat jar.

- [ ] **Step 1: Update `ARCHITECTURE.md`.** Add a short section after the existing invariants describing the wiring, and update any class names that changed:

```markdown
## 7. Wiring is centralized in one composition root

`Composition` (root package, beside `Main`) is the only class that constructs
collaborators. Everything else receives its dependencies via the constructor
(`CryptoCli(service)`, `CryptoService(ciphers, scorers, detector, readers,
writer, naming)`). `Main` is just `new Composition().cli().run(args)`. To swap an
implementation (e.g. an in-memory `TextWriter` in tests), construct the graph
differently — no production class hard-codes a collaborator.

Named behaviors are resolved through small registries, not `switch` statements:
`CipherCatalog` (caesar/rot13/atbash/vigenere), `ScorerCatalog`
(dictionary/frequency), `TextReaders` (gzip/markdown/pdf/plain), and
`Languages.byCode` (en/ua/ru, with default/auto → auto-detect). Each is populated
in `Composition` / a `withDefaults()` factory, so adding a variant is a one-line
registration.

A `Language` (en/ua/ru) composes the shared `Alphabets.*` instance plus its
linguistic data (common words, frequencies, distinctive letters) — one source of
truth for alphabets, used for both cipher selection and brute-force scoring.
```

- [ ] **Step 2: Update `CLAUDE.md` Architecture section** to reflect the new classes. Edit the `## Architecture` package bullets so they read:

```markdown
- `Composition` — the composition root; the only place that wires the object graph.
- `Main` — entry point; `new Composition().cli().run(args)`; never propagates exceptions.
- `cli/` — `CryptoCli` (picocli), constructor-injected with `CryptoService`.
- `app/` — `CryptoService` facade (constructor-injected) + `command/` (Template-Method commands) + `CryptoRequest`/`CipherSpec`.
- `cipher/` — `Cipher` strategy + Caesar/ROT13/Atbash/Vigenère + `CipherCatalog` registry + `CipherSpec`.
- `alphabet/` — `Alphabet`/`CharacterRing` value objects + `Alphabets` (registry; rings shared with `Language`).
- `crack/` — `Cracker`/`CaesarCracker`, `FitnessScorer` (+ `ScorerCatalog`), `LanguageDetector`, `Language`/`Languages`.
- `io/` — `TextReader` strategies (txt/md/gz/pdf) + `TextReaders`, `TextWriter` interface + `FileTextWriter`, `OutputNaming`.
- `error/` — `CryptanalysisException` hierarchy.
```

- [ ] **Step 3: Build the fat jar and verify end-to-end**

```bash
./mvnw -q clean package
printf 'Hello, World!' > /tmp/redesign-demo.txt
java -jar target/J4-M1-FP-1.0-SNAPSHOT.jar -e -k 5 -f /tmp/redesign-demo.txt
cat "/tmp/redesign-demo [ENCRYPTED].txt"
```
Expected: `clean package` BUILD SUCCESS (all tests run incl. `MainTest`); the encrypted file contains `Mjqqt, Btwqi!`.

- [ ] **Step 4: Verify `--help` and a non-caesar brute-force rejection from the jar**

```bash
java -jar target/J4-M1-FP-1.0-SNAPSHOT.jar --help
java -jar target/J4-M1-FP-1.0-SNAPSHOT.jar -b -c vigenere --keyword KEY -f /tmp/redesign-demo.txt; echo "exit=$?"
```
Expected: usage text lists `-e -d -b -k -f -c --keyword -a -s`; the brute-force-vigenère run prints an `Error: Brute-force is supported only for the caesar cipher` message, exits non-zero, and writes no new file.

- [ ] **Step 5: Commit**

```bash
git add ARCHITECTURE.md CLAUDE.md
git commit -m "docs: document composition root, registries, and unified Language"
```

---

## Self-Review

**Spec coverage:**
- DI + composition root → Task 7 (+ injectable seams prepared in Tasks 4-5). ✓
- Remove the 5 switch-factories → `CipherCatalog` (Task 3), `ScorerCatalog` (Task 4), `Languages.byCode` replacing `Alphabets.byName` + `forcedProfile` (Tasks 2, 4, 6). ✓
- Unify Alphabet+LanguageProfile → `Language`/`Languages` (Task 2). ✓
- Split dual-meaning field → `languageCode` + `CipherSpec` (Task 6). ✓
- Brute-force non-caesar bug fix → Task 6. ✓
- Primitive obsession → `CipherSpec` (Tasks 3, 6). ✓
- `TextWriter` interface + `FileTextWriter` → Task 5. ✓
- Real `PdfReader` (PDFBox) → Tasks 1, 5. ✓
- Vigenère keyword validation → Task 3. ✓
- gitignore artifacts → Task 1. ✓
- `MainTest` stays green, byte-for-byte; UA edit already reverted at the clean baseline → enforced every task (full-suite step), Task 8 final package. ✓
- Docs → Task 8. ✓

**Placeholder scan:** No TBD/TODO; every code step shows complete code or an exact, fully-specified rename; all run commands are concrete.

**Type consistency:** `Language`/`Languages.byCode(String):Optional<Language>`; `CipherSpec(String cipherName, Integer key, String keyword)`; `CipherCatalog.create(CipherSpec, Alphabet):Cipher` + `withDefaults()`; `ScorerCatalog.create(String, Language):FitnessScorer` + `withDefaults()`; `BruteForceCommand(Path, LanguageDetector, Language, ScorerCatalog, String, TextReaders, TextWriter, OutputNaming)`; `CryptoRequest(Operation, Path, CipherSpec, String languageCode, String scorerName)`; `CryptoService(CipherCatalog, ScorerCatalog, LanguageDetector, TextReaders, TextWriter, OutputNaming)`; `TextWriter.write(Path,String)`; `TextReaders(List<TextReader>)` + `withDefaults()`; `CryptoCli(CryptoService)`. Names are consistent across producing and consuming tasks.

**Scope:** One cohesive refactor of a single CLI tool; appropriate for one plan.
