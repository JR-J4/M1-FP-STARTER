# Caesar Cryptanalyzer — OOP Reimplementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reimplement the Caesar-cipher cryptanalyzer as a clean, SOLID, pattern-driven OOP showcase — multiple ciphers, alphabets, file readers, and brute-force scorers — while keeping the existing `MainTest` contract byte-for-byte green.

**Architecture:** Thin picocli CLI edge → `CryptoService` facade → single-responsibility domain packages (`cipher`, `alphabet`, `crack`, `io`). Patterns used only where they remove a conditional or an `if`-on-type: Strategy (`Cipher`, `FitnessScorer`), Factory (`CipherFactory`, registries), Template Method (`CryptoCommand`), Value Object (`Alphabet`/`CharacterRing`), Facade (`CryptoService`).

**Tech Stack:** Java 17, Maven (wrapper), JUnit 5.9.2, picocli 4.7.6, maven-shade-plugin 3.6.0.

## Global Constraints

- Java source/target: **17** (do not raise).
- Package root: **`ua.com.javarush.j4`**.
- `src/test/java/.../MainTest.java` is the **locked behavior contract** — never edit or reformat it; it must stay green with `./mvnw test`.
- Existing CLI contract unchanged: `-e | -d | -b`, `-k <int>` (required for `-e`/`-d`, absent for `-b`), `-f <path>`. All command flags single-character.
- `Main.main` MUST NOT propagate exceptions for invalid args or missing files (`ValidationTests` asserts `assertDoesNotThrow` + "no new file appears in tempDir").
- Output naming: `foo [ENCRYPTED].txt`; decrypt **replaces** `[ENCRYPTED]` with `[DECRYPTED]`.
- Cipher model: English = 26 letters, case preserved as two independent rings; Ukrainian = 33-letter ring; keys normalize mod ring size; shifts never cross case.
- Brute-force must recover the original **exactly** (case-sensitive) for both EN (Hamlet) and UA (Orwell) fixtures.
- All source files UTF-8. New tests live under `src/test/java/ua/com/javarush/j4/...`.
- Commit after every task with the exact message shown.

---

### Task 1: Build setup — picocli dependency + shade plugin

**Files:**
- Modify: `pom.xml`

**Interfaces:**
- Consumes: nothing.
- Produces: picocli on the compile classpath; `./mvnw package` produces a runnable fat jar with `Main-Class: ua.com.javarush.j4.Main`.

- [ ] **Step 1: Add the picocli dependency** inside the existing `<dependencies>` block (after the JUnit dependency):

```xml
        <dependency>
            <groupId>info.picocli</groupId>
            <artifactId>picocli</artifactId>
            <version>4.7.6</version>
        </dependency>
```

- [ ] **Step 2: Add the shade plugin** inside `<build><plugins>` (after the `maven-jar-plugin`):

```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.6.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>ua.com.javarush.j4.Main</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
```

- [ ] **Step 3: Verify dependency resolves and project still compiles**

Run: `./mvnw -q -DskipTests test-compile`
Expected: BUILD SUCCESS (picocli downloaded, existing sources still compile).

- [ ] **Step 4: Commit**

```bash
git add pom.xml
git commit -m "build: add picocli dependency and shade plugin"
```

---

### Task 2: error hierarchy + alphabet package (CharacterRing, Alphabet, Alphabets)

**Files:**
- Create: `src/main/java/ua/com/javarush/j4/error/CryptanalysisException.java`
- Create: `src/main/java/ua/com/javarush/j4/error/InvalidArgumentsException.java`
- Create: `src/main/java/ua/com/javarush/j4/error/UnreadableSourceException.java`
- Create: `src/main/java/ua/com/javarush/j4/alphabet/CharacterRing.java`
- Create: `src/main/java/ua/com/javarush/j4/alphabet/Alphabet.java`
- Create: `src/main/java/ua/com/javarush/j4/alphabet/Alphabets.java`
- Test: `src/test/java/ua/com/javarush/j4/alphabet/AlphabetTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `CryptanalysisException(String)` extends `RuntimeException`; `InvalidArgumentsException(String)` and `UnreadableSourceException(String, Throwable)` extend it.
  - `CharacterRing(String chars)` with `int size()`, `boolean contains(char)`, `int indexOf(char)`, `char shift(char c, int k)`, `char mirror(char c)`.
  - `Alphabet(String name, List<CharacterRing> rings)` with `String name()`, `boolean contains(char)`, `char shift(char c, int k)`, `char mirror(char c)`, `java.util.OptionalInt position(char c)`, `int keyspaceSize()`.
  - `Alphabets` constants `ENGLISH`, `UKRAINIAN`, `RUSSIAN`, `DEFAULT` (composite EN+UA) and `static Alphabet byName(String)`.

- [ ] **Step 1: Write the failing test** `src/test/java/ua/com/javarush/j4/alphabet/AlphabetTest.java`

```java
package ua.com.javarush.j4.alphabet;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import static org.junit.jupiter.api.Assertions.*;

class AlphabetTest {

    @Test
    void shiftsWithinCaseAndWrapsByModulo() {
        Alphabet en = Alphabets.ENGLISH;
        assertEquals('B', en.shift('A', 1));
        assertEquals('z', en.shift('a', 25));
        assertEquals('Z', en.shift('A', -1));   // wraps within upper ring
        assertEquals('A', en.shift('A', 26));    // full cycle
        assertEquals('B', en.shift('A', 27));    // 27 mod 26 == 1
    }

    @Test
    void ukrainianIsA33LetterRing() {
        Alphabet ua = Alphabets.UKRAINIAN;
        assertEquals('Б', ua.shift('А', 1));
        assertEquals('Я', ua.shift('А', 32));
        assertEquals('я', ua.shift('а', 32));
    }

    @Test
    void nonMemberCharactersPassThrough() {
        Alphabet en = Alphabets.ENGLISH;
        assertEquals('5', en.shift('5', 3));
        assertEquals(' ', en.shift(' ', 3));
        assertEquals('А', en.shift('А', 3)); // Cyrillic not in English alphabet
    }

    @Test
    void mirrorImplementsAtbashWithinRing() {
        Alphabet en = Alphabets.ENGLISH;
        assertEquals('Z', en.mirror('A'));
        assertEquals('a', en.mirror('z'));
        assertEquals('5', en.mirror('5'));
    }

    @Test
    void positionReturnsRingIndex() {
        assertEquals(0, Alphabets.ENGLISH.position('A').orElse(-1));
        assertEquals(10, Alphabets.ENGLISH.position('k').orElse(-1));
        assertTrue(Alphabets.ENGLISH.position('5').isEmpty());
    }

    @Test
    void defaultAlphabetHandlesBothEnglishAndUkrainian() {
        Alphabet def = Alphabets.DEFAULT;
        assertEquals('B', def.shift('A', 1));
        assertEquals('Б', def.shift('А', 1));
        assertEquals(33, def.keyspaceSize()); // largest ring (Ukrainian)
    }

    @Test
    void byNameRejectsUnknownAlphabet() {
        assertThrows(InvalidArgumentsException.class, () -> Alphabets.byName("klingon"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=AlphabetTest test`
Expected: FAIL — compilation error (`alphabet`/`error` packages do not exist yet).

- [ ] **Step 3: Create the error classes**

`src/main/java/ua/com/javarush/j4/error/CryptanalysisException.java`:
```java
package ua.com.javarush.j4.error;

/** Base type for all expected, cleanly-reported failures in the cryptanalyzer. */
public class CryptanalysisException extends RuntimeException {
    public CryptanalysisException(String message) {
        super(message);
    }

    public CryptanalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

`src/main/java/ua/com/javarush/j4/error/InvalidArgumentsException.java`:
```java
package ua.com.javarush.j4.error;

/** Thrown when CLI arguments or configuration are invalid. */
public class InvalidArgumentsException extends CryptanalysisException {
    public InvalidArgumentsException(String message) {
        super(message);
    }
}
```

`src/main/java/ua/com/javarush/j4/error/UnreadableSourceException.java`:
```java
package ua.com.javarush.j4.error;

/** Thrown when the input file cannot be read. */
public class UnreadableSourceException extends CryptanalysisException {
    public UnreadableSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 4: Create `CharacterRing`**

`src/main/java/ua/com/javarush/j4/alphabet/CharacterRing.java`:
```java
package ua.com.javarush.j4.alphabet;

/** An ordered, cyclic sequence of characters of a single case (a Caesar "ring"). */
public final class CharacterRing {
    private final String chars;

    public CharacterRing(String chars) {
        if (chars == null || chars.isEmpty()) {
            throw new IllegalArgumentException("ring must be non-empty");
        }
        this.chars = chars;
    }

    public int size() {
        return chars.length();
    }

    public boolean contains(char c) {
        return chars.indexOf(c) >= 0;
    }

    public int indexOf(char c) {
        return chars.indexOf(c);
    }

    /** Shift {@code c} by {@code k} positions within this ring; caller guarantees membership. */
    public char shift(char c, int k) {
        int n = chars.length();
        int idx = chars.indexOf(c);
        int shifted = ((idx + k) % n + n) % n;
        return chars.charAt(shifted);
    }

    /** Atbash mirror: map index i to size-1-i. */
    public char mirror(char c) {
        return chars.charAt(size() - 1 - chars.indexOf(c));
    }
}
```

- [ ] **Step 5: Create `Alphabet`**

`src/main/java/ua/com/javarush/j4/alphabet/Alphabet.java`:
```java
package ua.com.javarush.j4.alphabet;

import java.util.List;
import java.util.OptionalInt;

/** An alphabet is an ordered set of case-independent character rings. */
public final class Alphabet {
    private final String name;
    private final List<CharacterRing> rings;

    public Alphabet(String name, List<CharacterRing> rings) {
        this.name = name;
        this.rings = List.copyOf(rings);
    }

    public String name() {
        return name;
    }

    public boolean contains(char c) {
        return ringOf(c) != null;
    }

    /** Shift within the char's own ring; characters outside the alphabet pass through. */
    public char shift(char c, int k) {
        CharacterRing ring = ringOf(c);
        return ring == null ? c : ring.shift(c, k);
    }

    /** Atbash mirror within the char's own ring; non-members pass through. */
    public char mirror(char c) {
        CharacterRing ring = ringOf(c);
        return ring == null ? c : ring.mirror(c);
    }

    /** Index of the char within its ring (used by Vigenère for keyword shifts). */
    public OptionalInt position(char c) {
        CharacterRing ring = ringOf(c);
        return ring == null ? OptionalInt.empty() : OptionalInt.of(ring.indexOf(c));
    }

    /** Largest ring size — the meaningful upper bound for a Caesar keyspace sweep. */
    public int keyspaceSize() {
        return rings.stream().mapToInt(CharacterRing::size).max().orElse(1);
    }

    private CharacterRing ringOf(char c) {
        for (CharacterRing ring : rings) {
            if (ring.contains(c)) {
                return ring;
            }
        }
        return null;
    }
}
```

- [ ] **Step 6: Create `Alphabets` registry**

`src/main/java/ua/com/javarush/j4/alphabet/Alphabets.java`:
```java
package ua.com.javarush.j4.alphabet;

import ua.com.javarush.j4.error.InvalidArgumentsException;

import java.util.List;
import java.util.Locale;

/** Built-in alphabets and a name-based lookup. Rings are shared so shifting is consistent. */
public final class Alphabets {

    private static final CharacterRing EN_UPPER = new CharacterRing("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    private static final CharacterRing EN_LOWER = new CharacterRing("abcdefghijklmnopqrstuvwxyz");
    private static final CharacterRing UA_UPPER = new CharacterRing("АБВГҐДЕЄЖЗИІЇЙКЛМНОПРСТУФХЦЧШЩЬЮЯ");
    private static final CharacterRing UA_LOWER = new CharacterRing("абвгґдеєжзиіїйклмнопрстуфхцчшщьюя");
    private static final CharacterRing RU_UPPER = new CharacterRing("АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ");
    private static final CharacterRing RU_LOWER = new CharacterRing("абвгдеёжзийклмнопрстуфхцчшщъыьэюя");

    public static final Alphabet ENGLISH = new Alphabet("en", List.of(EN_UPPER, EN_LOWER));
    public static final Alphabet UKRAINIAN = new Alphabet("ua", List.of(UA_UPPER, UA_LOWER));
    public static final Alphabet RUSSIAN = new Alphabet("ru", List.of(RU_UPPER, RU_LOWER));

    /** Default for encrypt/decrypt: Latin + Ukrainian rings (no overlap), so mixed text just works. */
    public static final Alphabet DEFAULT = new Alphabet("default", List.of(EN_UPPER, EN_LOWER, UA_UPPER, UA_LOWER));

    private Alphabets() {
    }

    public static Alphabet byName(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "en", "english" -> ENGLISH;
            case "ua", "ukrainian" -> UKRAINIAN;
            case "ru", "russian" -> RUSSIAN;
            case "default", "auto" -> DEFAULT;
            default -> throw new InvalidArgumentsException("Unknown alphabet: " + name);
        };
    }
}
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `./mvnw -q -Dtest=AlphabetTest test`
Expected: PASS (7 tests).

- [ ] **Step 8: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/error src/main/java/ua/com/javarush/j4/alphabet src/test/java/ua/com/javarush/j4/alphabet
git commit -m "feat: add error hierarchy and alphabet value objects"
```

---

### Task 3: cipher package — Cipher interface + CaesarCipher

**Files:**
- Create: `src/main/java/ua/com/javarush/j4/cipher/Cipher.java`
- Create: `src/main/java/ua/com/javarush/j4/cipher/CaesarCipher.java`
- Test: `src/test/java/ua/com/javarush/j4/cipher/CaesarCipherTest.java`

**Interfaces:**
- Consumes: `Alphabet`, `Alphabets` (Task 2).
- Produces:
  - `interface Cipher { String encrypt(String text); String decrypt(String text); }`
  - `CaesarCipher(Alphabet alphabet, int key)` implements `Cipher`.

- [ ] **Step 1: Write the failing test** `src/test/java/ua/com/javarush/j4/cipher/CaesarCipherTest.java`

```java
package ua.com.javarush.j4.cipher;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.alphabet.Alphabets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaesarCipherTest {

    @Test
    void encryptsWithinCase() {
        Cipher c = new CaesarCipher(Alphabets.DEFAULT, 1);
        assertEquals("BCD", c.encrypt("ABC"));
        assertEquals("Бб", c.encrypt("Аа"));
    }

    @Test
    void decryptIsInverseOfEncrypt() {
        Cipher c = new CaesarCipher(Alphabets.DEFAULT, 5);
        String text = "Hello, World! Привіт!";
        assertEquals(text, c.decrypt(c.encrypt(text)));
    }

    @Test
    void negativeKeyWrapsWithinCase() {
        Cipher c = new CaesarCipher(Alphabets.DEFAULT, -1);
        assertEquals("Z", c.encrypt("A"));
        assertEquals("z", c.encrypt("a"));
    }

    @Test
    void nonLettersAndFullCyclesPassThrough() {
        assertEquals("0123456789", new CaesarCipher(Alphabets.DEFAULT, 5).encrypt("0123456789"));
        assertEquals("Hello", new CaesarCipher(Alphabets.DEFAULT, 26).encrypt("Hello"));
        assertEquals(".,!? \t", new CaesarCipher(Alphabets.DEFAULT, 5).encrypt(".,!? \t"));
    }

    @Test
    void multilinePreservesNewlines() {
        assertEquals("bcd\nefg\n", new CaesarCipher(Alphabets.DEFAULT, 1).encrypt("abc\ndef\n"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=CaesarCipherTest test`
Expected: FAIL — `cipher` package does not exist.

- [ ] **Step 3: Create `Cipher`**

`src/main/java/ua/com/javarush/j4/cipher/Cipher.java`:
```java
package ua.com.javarush.j4.cipher;

/** A configured text transform. Key/keyword are bound at construction, so this is a pure String → String. */
public interface Cipher {
    String encrypt(String text);

    String decrypt(String text);
}
```

- [ ] **Step 4: Create `CaesarCipher`**

`src/main/java/ua/com/javarush/j4/cipher/CaesarCipher.java`:
```java
package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;

/** Shifts every character by a fixed key within its own alphabet ring. */
public final class CaesarCipher implements Cipher {
    private final Alphabet alphabet;
    private final int key;

    public CaesarCipher(Alphabet alphabet, int key) {
        this.alphabet = alphabet;
        this.key = key;
    }

    @Override
    public String encrypt(String text) {
        return shiftAll(text, key);
    }

    @Override
    public String decrypt(String text) {
        return shiftAll(text, -key);
    }

    private String shiftAll(String text, int by) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            out.append(alphabet.shift(text.charAt(i), by));
        }
        return out.toString();
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./mvnw -q -Dtest=CaesarCipherTest test`
Expected: PASS (5 tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/cipher src/test/java/ua/com/javarush/j4/cipher/CaesarCipherTest.java
git commit -m "feat: add Cipher strategy and CaesarCipher"
```

---

### Task 4: cipher package — Rot13, Atbash, Vigenère + CipherFactory

**Files:**
- Create: `src/main/java/ua/com/javarush/j4/cipher/Rot13Cipher.java`
- Create: `src/main/java/ua/com/javarush/j4/cipher/AtbashCipher.java`
- Create: `src/main/java/ua/com/javarush/j4/cipher/VigenereCipher.java`
- Create: `src/main/java/ua/com/javarush/j4/cipher/CipherFactory.java`
- Test: `src/test/java/ua/com/javarush/j4/cipher/CipherFactoryTest.java`

**Interfaces:**
- Consumes: `Cipher`, `CaesarCipher` (Task 3), `Alphabet`, `Alphabets` (Task 2), `InvalidArgumentsException` (Task 2).
- Produces:
  - `Rot13Cipher(Alphabet)`, `AtbashCipher(Alphabet)`, `VigenereCipher(Alphabet, String keyword)`, all implement `Cipher`.
  - `CipherFactory` with `Cipher create(String name, Alphabet alphabet, Integer key, String keyword)`. Names: `caesar`, `rot13`, `atbash`, `vigenere`. Caesar with `key == null` throws `InvalidArgumentsException`.

- [ ] **Step 1: Write the failing test** `src/test/java/ua/com/javarush/j4/cipher/CipherFactoryTest.java`

```java
package ua.com.javarush.j4.cipher;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import static org.junit.jupiter.api.Assertions.*;

class CipherFactoryTest {

    private final CipherFactory factory = new CipherFactory();

    @Test
    void buildsCaesar() {
        Cipher c = factory.create("caesar", Alphabets.DEFAULT, 1, null);
        assertEquals("BCD", c.encrypt("ABC"));
    }

    @Test
    void caesarWithoutKeyIsRejected() {
        assertThrows(InvalidArgumentsException.class,
                () -> factory.create("caesar", Alphabets.DEFAULT, null, null));
    }

    @Test
    void rot13IsReversible() {
        Cipher c = factory.create("rot13", Alphabets.ENGLISH, null, null);
        assertEquals("URYYB", c.encrypt("HELLO"));
        assertEquals("HELLO", c.decrypt("URYYB"));
    }

    @Test
    void atbashIsSelfInverse() {
        Cipher c = factory.create("atbash", Alphabets.ENGLISH, null, null);
        assertEquals("ZYX", c.encrypt("ABC"));
        assertEquals("ABC", c.decrypt("ZYX"));
    }

    @Test
    void vigenereMatchesKnownVector() {
        Cipher c = factory.create("vigenere", Alphabets.ENGLISH, null, "KEY");
        assertEquals("RIJVS", c.encrypt("HELLO"));
        assertEquals("HELLO", c.decrypt("RIJVS"));
    }

    @Test
    void vigenereWithoutKeywordIsRejected() {
        assertThrows(InvalidArgumentsException.class,
                () -> factory.create("vigenere", Alphabets.ENGLISH, null, null));
    }

    @Test
    void unknownCipherIsRejected() {
        assertThrows(InvalidArgumentsException.class,
                () -> factory.create("enigma", Alphabets.ENGLISH, 1, null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=CipherFactoryTest test`
Expected: FAIL — new classes do not exist.

- [ ] **Step 3: Create `Rot13Cipher`**

`src/main/java/ua/com/javarush/j4/cipher/Rot13Cipher.java`:
```java
package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;

/** ROT13: a Caesar cipher with a fixed shift of 13. */
public final class Rot13Cipher implements Cipher {
    private final CaesarCipher delegate;

    public Rot13Cipher(Alphabet alphabet) {
        this.delegate = new CaesarCipher(alphabet, 13);
    }

    @Override
    public String encrypt(String text) {
        return delegate.encrypt(text);
    }

    @Override
    public String decrypt(String text) {
        return delegate.decrypt(text);
    }
}
```

- [ ] **Step 4: Create `AtbashCipher`**

`src/main/java/ua/com/javarush/j4/cipher/AtbashCipher.java`:
```java
package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;

/** Atbash: mirror each letter within its ring. Self-inverse. */
public final class AtbashCipher implements Cipher {
    private final Alphabet alphabet;

    public AtbashCipher(Alphabet alphabet) {
        this.alphabet = alphabet;
    }

    @Override
    public String encrypt(String text) {
        return mirror(text);
    }

    @Override
    public String decrypt(String text) {
        return mirror(text);
    }

    private String mirror(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            out.append(alphabet.mirror(text.charAt(i)));
        }
        return out.toString();
    }
}
```

- [ ] **Step 5: Create `VigenereCipher`**

`src/main/java/ua/com/javarush/j4/cipher/VigenereCipher.java`:
```java
package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.error.InvalidArgumentsException;

/** Polyalphabetic cipher: each enciphered letter is shifted by the next keyword letter. */
public final class VigenereCipher implements Cipher {
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
        return process(text, 1);
    }

    @Override
    public String decrypt(String text) {
        return process(text, -1);
    }

    private String process(String text, int sign) {
        StringBuilder out = new StringBuilder(text.length());
        int keyIndex = 0;
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

- [ ] **Step 6: Create `CipherFactory`**

`src/main/java/ua/com/javarush/j4/cipher/CipherFactory.java`:
```java
package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import java.util.Locale;

/** Builds a configured {@link Cipher} from a cipher name plus key/keyword. */
public final class CipherFactory {

    public Cipher create(String name, Alphabet alphabet, Integer key, String keyword) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "caesar" -> new CaesarCipher(alphabet, requireKey(key));
            case "rot13" -> new Rot13Cipher(alphabet);
            case "atbash" -> new AtbashCipher(alphabet);
            case "vigenere" -> new VigenereCipher(alphabet, keyword);
            default -> throw new InvalidArgumentsException("Unknown cipher: " + name);
        };
    }

    private int requireKey(Integer key) {
        if (key == null) {
            throw new InvalidArgumentsException("Caesar cipher requires a key (-k <int>)");
        }
        return key;
    }
}
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `./mvnw -q -Dtest=CipherFactoryTest test`
Expected: PASS (7 tests).

- [ ] **Step 8: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/cipher src/test/java/ua/com/javarush/j4/cipher/CipherFactoryTest.java
git commit -m "feat: add ROT13, Atbash, Vigenere ciphers and CipherFactory"
```

---

### Task 5: crack package — language profiles, scorers, detector

**Files:**
- Create: `src/main/java/ua/com/javarush/j4/crack/LanguageProfile.java`
- Create: `src/main/java/ua/com/javarush/j4/crack/LanguageProfiles.java`
- Create: `src/main/java/ua/com/javarush/j4/crack/FitnessScorer.java`
- Create: `src/main/java/ua/com/javarush/j4/crack/DictionaryScorer.java`
- Create: `src/main/java/ua/com/javarush/j4/crack/FrequencyScorer.java`
- Create: `src/main/java/ua/com/javarush/j4/crack/LanguageDetector.java`
- Test: `src/test/java/ua/com/javarush/j4/crack/ScoringTest.java`

**Interfaces:**
- Consumes: `Alphabet`, `Alphabets` (Task 2).
- Produces:
  - `record LanguageProfile(String name, Alphabet alphabet, Set<String> commonWords, Map<Character,Double> letterFrequencies, Set<Character> distinctive)`.
  - `LanguageProfiles` constants `ENGLISH`, `UKRAINIAN`, `RUSSIAN` and `static List<LanguageProfile> all()`.
  - `interface FitnessScorer { double score(String text); }`.
  - `DictionaryScorer(LanguageProfile)` and `FrequencyScorer(LanguageProfile)` implement `FitnessScorer`.
  - `LanguageDetector` with `LanguageProfile detect(String text)`.

- [ ] **Step 1: Write the failing test** `src/test/java/ua/com/javarush/j4/crack/ScoringTest.java`

```java
package ua.com.javarush.j4.crack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoringTest {

    @Test
    void dictionaryScorerRanksRealEnglishHigher() {
        FitnessScorer scorer = new DictionaryScorer(LanguageProfiles.ENGLISH);
        double real = scorer.score("the cat sat on the mat and the dog");
        double garbage = scorer.score("xyz qrs tuv wxy zab cde fgh");
        assertTrue(real > garbage, "real English should score higher than gibberish");
    }

    @Test
    void frequencyScorerRanksRealEnglishHigher() {
        FitnessScorer scorer = new FrequencyScorer(LanguageProfiles.ENGLISH);
        double real = scorer.score("the quick brown fox jumps over the lazy dog");
        double garbage = scorer.score("zzzz qqqq xxxx jjjj kkkk wwww");
        assertTrue(real > garbage);
    }

    @Test
    void detectorIdentifiesEnglish() {
        assertEquals("en", new LanguageDetector().detect("Hello, this is plain English text.").name());
    }

    @Test
    void detectorIdentifiesUkrainianByDistinctiveLetters() {
        // Contains і, ї — distinctive to Ukrainian, absent in Russian.
        assertEquals("ua", new LanguageDetector().detect("Привіт, це українська їжа і мова.").name());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=ScoringTest test`
Expected: FAIL — `crack` package does not exist.

- [ ] **Step 3: Create `LanguageProfile`**

`src/main/java/ua/com/javarush/j4/crack/LanguageProfile.java`:
```java
package ua.com.javarush.j4.crack;

import ua.com.javarush.j4.alphabet.Alphabet;

import java.util.Map;
import java.util.Set;

/** Everything a scorer/detector needs to judge a text as a given language. */
public record LanguageProfile(
        String name,
        Alphabet alphabet,
        Set<String> commonWords,
        Map<Character, Double> letterFrequencies,
        Set<Character> distinctive) {
}
```

- [ ] **Step 4: Create `LanguageProfiles`**

`src/main/java/ua/com/javarush/j4/crack/LanguageProfiles.java`:
```java
package ua.com.javarush.j4.crack;

import ua.com.javarush.j4.alphabet.Alphabets;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Built-in language profiles (common words, letter frequencies, distinctive letters). */
public final class LanguageProfiles {

    public static final LanguageProfile ENGLISH = new LanguageProfile(
            "en",
            Alphabets.ENGLISH,
            Set.of("the", "and", "to", "of", "a", "in", "is", "it", "that", "he",
                    "was", "for", "on", "are", "as", "with", "his", "they", "at", "be",
                    "this", "from", "or", "had", "not", "but", "what", "all", "were", "we",
                    "when", "you", "your", "can", "said", "there", "which", "she", "do", "their"),
            Map.ofEntries(
                    Map.entry('e', 12.7), Map.entry('t', 9.1), Map.entry('a', 8.2),
                    Map.entry('o', 7.5), Map.entry('i', 7.0), Map.entry('n', 6.7),
                    Map.entry('s', 6.3), Map.entry('h', 6.1), Map.entry('r', 6.0),
                    Map.entry('d', 4.3), Map.entry('l', 4.0), Map.entry('u', 2.8),
                    Map.entry('c', 2.8), Map.entry('m', 2.4), Map.entry('w', 2.4),
                    Map.entry('f', 2.2), Map.entry('g', 2.0), Map.entry('y', 2.0),
                    Map.entry('p', 1.9), Map.entry('b', 1.5)),
            Set.of()); // English vs Cyrillic is decided by alphabet membership alone

    public static final LanguageProfile UKRAINIAN = new LanguageProfile(
            "ua",
            Alphabets.UKRAINIAN,
            Set.of("і", "в", "на", "з", "що", "не", "як", "до", "за", "це",
                    "та", "а", "по", "ні", "так", "він", "вона", "вони", "був", "була",
                    "було", "у", "від", "для", "при", "про", "але", "або", "її", "його",
                    "ми", "ви", "я", "ти", "де", "коли", "тут", "там", "ще", "вже"),
            Map.ofEntries(
                    Map.entry('о', 9.0), Map.entry('а', 7.0), Map.entry('н', 6.5),
                    Map.entry('и', 6.0), Map.entry('і', 5.7), Map.entry('в', 5.0),
                    Map.entry('т', 4.5), Map.entry('е', 4.5), Map.entry('р', 4.0),
                    Map.entry('с', 4.0), Map.entry('к', 3.5), Map.entry('л', 3.5),
                    Map.entry('д', 3.0), Map.entry('у', 3.0), Map.entry('м', 3.0),
                    Map.entry('п', 2.8), Map.entry('я', 2.0), Map.entry('з', 2.0),
                    Map.entry('б', 1.7), Map.entry('г', 1.4)),
            Set.of('і', 'І', 'ї', 'Ї', 'є', 'Є', 'ґ', 'Ґ'));

    public static final LanguageProfile RUSSIAN = new LanguageProfile(
            "ru",
            Alphabets.RUSSIAN,
            Set.of("и", "в", "не", "на", "я", "что", "тот", "быть", "с", "он",
                    "а", "по", "это", "она", "этот", "к", "но", "они", "мы", "как",
                    "из", "у", "который", "то", "за", "свой", "что", "весь", "год", "от",
                    "так", "о", "для", "бы", "вы", "со", "если", "уже", "или", "ни"),
            Map.ofEntries(
                    Map.entry('о', 10.9), Map.entry('е', 8.4), Map.entry('а', 8.0),
                    Map.entry('и', 7.4), Map.entry('н', 6.7), Map.entry('т', 6.3),
                    Map.entry('с', 5.5), Map.entry('р', 4.7), Map.entry('в', 4.5),
                    Map.entry('л', 4.4), Map.entry('к', 3.5), Map.entry('м', 3.2),
                    Map.entry('д', 3.0), Map.entry('п', 2.8), Map.entry('у', 2.6),
                    Map.entry('я', 2.0), Map.entry('ы', 1.9), Map.entry('з', 1.8),
                    Map.entry('б', 1.6), Map.entry('г', 1.7)),
            Set.of('ё', 'Ё', 'ъ', 'Ъ', 'ы', 'Ы', 'э', 'Э'));

    private LanguageProfiles() {
    }

    public static List<LanguageProfile> all() {
        return List.of(ENGLISH, UKRAINIAN, RUSSIAN);
    }
}
```

- [ ] **Step 5: Create `FitnessScorer`**

`src/main/java/ua/com/javarush/j4/crack/FitnessScorer.java`:
```java
package ua.com.javarush.j4.crack;

/** Strategy: scores how strongly a text resembles natural language. Higher is better. */
public interface FitnessScorer {
    double score(String text);
}
```

- [ ] **Step 6: Create `DictionaryScorer`**

`src/main/java/ua/com/javarush/j4/crack/DictionaryScorer.java`:
```java
package ua.com.javarush.j4.crack;

import java.util.Locale;

/** Counts how many whitespace/punctuation-delimited tokens are common words of the language. */
public final class DictionaryScorer implements FitnessScorer {
    private final LanguageProfile profile;

    public DictionaryScorer(LanguageProfile profile) {
        this.profile = profile;
    }

    @Override
    public double score(String text) {
        String[] tokens = text.toLowerCase(Locale.ROOT).split("[^\\p{L}]+");
        int hits = 0;
        for (String token : tokens) {
            if (!token.isEmpty() && profile.commonWords().contains(token)) {
                hits++;
            }
        }
        return hits;
    }
}
```

- [ ] **Step 7: Create `FrequencyScorer`**

`src/main/java/ua/com/javarush/j4/crack/FrequencyScorer.java`:
```java
package ua.com.javarush.j4.crack;

/**
 * Scores a text by how much its letters favour high-frequency letters of the
 * language: sum of expected frequencies over all letters, normalised by length.
 */
public final class FrequencyScorer implements FitnessScorer {
    private final LanguageProfile profile;

    public FrequencyScorer(LanguageProfile profile) {
        this.profile = profile;
    }

    @Override
    public double score(String text) {
        double sum = 0.0;
        int letters = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toLowerCase(text.charAt(i));
            Double freq = profile.letterFrequencies().get(c);
            if (freq != null) {
                sum += freq;
            }
            if (Character.isLetter(text.charAt(i))) {
                letters++;
            }
        }
        return letters == 0 ? 0.0 : sum / letters;
    }
}
```

- [ ] **Step 8: Create `LanguageDetector`**

`src/main/java/ua/com/javarush/j4/crack/LanguageDetector.java`:
```java
package ua.com.javarush.j4.crack;

/**
 * Picks the most likely language profile for a text by counting alphabet
 * membership, with a strong bonus for letters distinctive to one language
 * (e.g. і/ї/є/ґ for Ukrainian, ё/ъ/ы/э for Russian).
 */
public final class LanguageDetector {
    private static final int DISTINCTIVE_WEIGHT = 1000;

    public LanguageProfile detect(String text) {
        LanguageProfile best = LanguageProfiles.ENGLISH;
        long bestScore = Long.MIN_VALUE;
        for (LanguageProfile profile : LanguageProfiles.all()) {
            long score = scoreFor(profile, text);
            if (score > bestScore) {
                bestScore = score;
                best = profile;
            }
        }
        return best;
    }

    private long scoreFor(LanguageProfile profile, String text) {
        long membership = 0;
        long distinctive = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (profile.alphabet().contains(c)) {
                membership++;
            }
            if (profile.distinctive().contains(c)) {
                distinctive++;
            }
        }
        return membership + distinctive * DISTINCTIVE_WEIGHT;
    }
}
```

- [ ] **Step 9: Run tests to verify they pass**

Run: `./mvnw -q -Dtest=ScoringTest test`
Expected: PASS (4 tests).

- [ ] **Step 10: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/crack src/test/java/ua/com/javarush/j4/crack/ScoringTest.java
git commit -m "feat: add language profiles, fitness scorers, language detector"
```

---

### Task 6: crack package — Cracker + CaesarCracker

**Files:**
- Create: `src/main/java/ua/com/javarush/j4/crack/CrackResult.java`
- Create: `src/main/java/ua/com/javarush/j4/crack/Cracker.java`
- Create: `src/main/java/ua/com/javarush/j4/crack/CaesarCracker.java`
- Test: `src/test/java/ua/com/javarush/j4/crack/CaesarCrackerTest.java`

**Interfaces:**
- Consumes: `CaesarCipher` (Task 3), `Alphabet`/`Alphabets` (Task 2), `FitnessScorer`, `DictionaryScorer`, `LanguageProfiles` (Task 5).
- Produces:
  - `record CrackResult(int key, String plaintext)`.
  - `interface Cracker { CrackResult crack(String ciphertext); }`.
  - `CaesarCracker(Alphabet alphabet, FitnessScorer scorer)` implements `Cracker` — sweeps shifts `0..alphabet.keyspaceSize()-1`, returns the highest-scoring decryption.

- [ ] **Step 1: Write the failing test** `src/test/java/ua/com/javarush/j4/crack/CaesarCrackerTest.java`

```java
package ua.com.javarush.j4.crack;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.cipher.CaesarCipher;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaesarCrackerTest {

    @Test
    void recoversEnglishPlaintextExactly() {
        String original = "The quick brown fox jumps over the lazy dog. "
                + "And the dog was not amused, for that is what dogs do.";
        String ciphertext = new CaesarCipher(LanguageProfiles.ENGLISH.alphabet(), 7).encrypt(original);

        CrackResult result = new CaesarCracker(
                LanguageProfiles.ENGLISH.alphabet(),
                new DictionaryScorer(LanguageProfiles.ENGLISH)).crack(ciphertext);

        assertEquals(7, result.key());
        assertEquals(original, result.plaintext());
    }

    @Test
    void recoversUkrainianPlaintextExactly() {
        String original = "Він був високий і худий, а на обличчі його застигла "
                + "усмішка. Це не та людина, що боїться зими.";
        String ciphertext = new CaesarCipher(LanguageProfiles.UKRAINIAN.alphabet(), 12).encrypt(original);

        CrackResult result = new CaesarCracker(
                LanguageProfiles.UKRAINIAN.alphabet(),
                new DictionaryScorer(LanguageProfiles.UKRAINIAN)).crack(ciphertext);

        assertEquals(12, result.key());
        assertEquals(original, result.plaintext());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=CaesarCrackerTest test`
Expected: FAIL — cracker classes do not exist.

- [ ] **Step 3: Create `CrackResult`**

`src/main/java/ua/com/javarush/j4/crack/CrackResult.java`:
```java
package ua.com.javarush.j4.crack;

/** The outcome of a brute-force attack: the recovered key and plaintext. */
public record CrackResult(int key, String plaintext) {
}
```

- [ ] **Step 4: Create `Cracker`**

`src/main/java/ua/com/javarush/j4/crack/Cracker.java`:
```java
package ua.com.javarush.j4.crack;

/** Recovers plaintext from ciphertext without a key. */
public interface Cracker {
    CrackResult crack(String ciphertext);
}
```

- [ ] **Step 5: Create `CaesarCracker`**

`src/main/java/ua/com/javarush/j4/crack/CaesarCracker.java`:
```java
package ua.com.javarush.j4.crack;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.cipher.CaesarCipher;

/** Sweeps every Caesar shift and returns the decryption the scorer likes best. */
public final class CaesarCracker implements Cracker {
    private final Alphabet alphabet;
    private final FitnessScorer scorer;

    public CaesarCracker(Alphabet alphabet, FitnessScorer scorer) {
        this.alphabet = alphabet;
        this.scorer = scorer;
    }

    @Override
    public CrackResult crack(String ciphertext) {
        int keyspace = alphabet.keyspaceSize();
        int bestKey = 0;
        String bestText = ciphertext;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int key = 0; key < keyspace; key++) {
            String candidate = new CaesarCipher(alphabet, key).decrypt(ciphertext);
            double score = scorer.score(candidate);
            if (score > bestScore) {
                bestScore = score;
                bestKey = key;
                bestText = candidate;
            }
        }
        return new CrackResult(bestKey, bestText);
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./mvnw -q -Dtest=CaesarCrackerTest test`
Expected: PASS (2 tests).

- [ ] **Step 7: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/crack/CrackResult.java src/main/java/ua/com/javarush/j4/crack/Cracker.java src/main/java/ua/com/javarush/j4/crack/CaesarCracker.java src/test/java/ua/com/javarush/j4/crack/CaesarCrackerTest.java
git commit -m "feat: add CaesarCracker brute-force with pluggable scorer"
```

---

### Task 7: io package — OutputNaming, TextReader(s), TextWriter

**Files:**
- Create: `src/main/java/ua/com/javarush/j4/io/OutputNaming.java`
- Create: `src/main/java/ua/com/javarush/j4/io/TextReader.java`
- Create: `src/main/java/ua/com/javarush/j4/io/PlainTextReader.java`
- Create: `src/main/java/ua/com/javarush/j4/io/MarkdownReader.java`
- Create: `src/main/java/ua/com/javarush/j4/io/GzipTextReader.java`
- Create: `src/main/java/ua/com/javarush/j4/io/TextReaders.java`
- Create: `src/main/java/ua/com/javarush/j4/io/TextWriter.java`
- Test: `src/test/java/ua/com/javarush/j4/io/OutputNamingTest.java`
- Test: `src/test/java/ua/com/javarush/j4/io/TextReadersTest.java`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces:
  - `OutputNaming` with `Path forEncrypt(Path input)` and `Path forDecrypt(Path input)`.
  - `interface TextReader { boolean supports(Path p); String read(Path p) throws IOException; }`.
  - `PlainTextReader`, `MarkdownReader`, `GzipTextReader` implement `TextReader`.
  - `TextReaders` with `TextReader pick(Path p)` (chooses by extension, falls back to plain text).
  - `TextWriter` with `void write(Path p, String content) throws IOException`.

- [ ] **Step 1: Write the failing test** `src/test/java/ua/com/javarush/j4/io/OutputNamingTest.java`

```java
package ua.com.javarush.j4.io;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OutputNamingTest {

    private final OutputNaming naming = new OutputNaming();

    @Test
    void encryptInsertsMarkerBeforeExtension() {
        assertEquals("plain [ENCRYPTED].txt",
                naming.forEncrypt(Path.of("plain.txt")).getFileName().toString());
    }

    @Test
    void decryptReplacesEncryptedMarker() {
        String out = naming.forDecrypt(Path.of("plain [ENCRYPTED].txt")).getFileName().toString();
        assertTrue(out.contains("[DECRYPTED]"));
        assertFalse(out.contains("[ENCRYPTED]"));
    }

    @Test
    void decryptInsertsMarkerWhenNoEncryptedMarkerPresent() {
        assertEquals("plain [DECRYPTED].txt",
                naming.forDecrypt(Path.of("plain.txt")).getFileName().toString());
    }

    @Test
    void preservesParentDirectory() {
        Path out = naming.forEncrypt(Path.of("/tmp/sub/plain.txt"));
        assertEquals(Path.of("/tmp/sub"), out.getParent());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=OutputNamingTest test`
Expected: FAIL — `io` package does not exist.

- [ ] **Step 3: Create `OutputNaming`**

`src/main/java/ua/com/javarush/j4/io/OutputNaming.java`:
```java
package ua.com.javarush.j4.io;

import java.nio.file.Path;

/** Computes the output path: inserts/replaces the [ENCRYPTED]/[DECRYPTED] marker. */
public final class OutputNaming {
    private static final String ENCRYPTED = "[ENCRYPTED]";
    private static final String DECRYPTED = "[DECRYPTED]";

    public Path forEncrypt(Path input) {
        return sibling(input, insertMarker(input.getFileName().toString(), ENCRYPTED));
    }

    public Path forDecrypt(Path input) {
        String name = input.getFileName().toString();
        String renamed = name.contains(ENCRYPTED)
                ? name.replace(ENCRYPTED, DECRYPTED)
                : insertMarker(name, DECRYPTED);
        return sibling(input, renamed);
    }

    private static String insertMarker(String name, String marker) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return name + " " + marker;
        }
        return name.substring(0, dot) + " " + marker + name.substring(dot);
    }

    private static Path sibling(Path input, String newName) {
        Path parent = input.getParent();
        return parent == null ? Path.of(newName) : parent.resolve(newName);
    }
}
```

- [ ] **Step 4: Create `TextReader` and the three readers**

`src/main/java/ua/com/javarush/j4/io/TextReader.java`:
```java
package ua.com.javarush.j4.io;

import java.io.IOException;
import java.nio.file.Path;

/** Reads the textual content of a file. Implementations declare which paths they support. */
public interface TextReader {
    boolean supports(Path path);

    String read(Path path) throws IOException;
}
```

`src/main/java/ua/com/javarush/j4/io/PlainTextReader.java`:
```java
package ua.com.javarush.j4.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Reads .txt files (and acts as the universal fallback). */
public final class PlainTextReader implements TextReader {
    @Override
    public boolean supports(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt");
    }

    @Override
    public String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
```

`src/main/java/ua/com/javarush/j4/io/MarkdownReader.java`:
```java
package ua.com.javarush.j4.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Reads Markdown documents as plain text (content passes through unchanged). */
public final class MarkdownReader implements TextReader {
    @Override
    public boolean supports(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".md") || name.endsWith(".markdown");
    }

    @Override
    public String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
```

`src/main/java/ua/com/javarush/j4/io/GzipTextReader.java`:
```java
package ua.com.javarush.j4.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/** Reads gzip-compressed UTF-8 text files (.gz). */
public final class GzipTextReader implements TextReader {
    @Override
    public boolean supports(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gz");
    }

    @Override
    public String read(Path path) throws IOException {
        try (InputStream in = new GZIPInputStream(Files.newInputStream(path))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
```

- [ ] **Step 5: Create `TextReaders` registry**

`src/main/java/ua/com/javarush/j4/io/TextReaders.java`:
```java
package ua.com.javarush.j4.io;

import java.nio.file.Path;
import java.util.List;

/** Picks the right reader for a path by extension; falls back to plain text. */
public final class TextReaders {
    private final List<TextReader> readers;
    private final TextReader fallback = new PlainTextReader();

    public TextReaders() {
        this.readers = List.of(new GzipTextReader(), new MarkdownReader(), new PlainTextReader());
    }

    public TextReader pick(Path path) {
        return readers.stream()
                .filter(reader -> reader.supports(path))
                .findFirst()
                .orElse(fallback);
    }
}
```

- [ ] **Step 6: Create `TextWriter`**

`src/main/java/ua/com/javarush/j4/io/TextWriter.java`:
```java
package ua.com.javarush.j4.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes UTF-8 text content to a file. */
public final class TextWriter {
    public void write(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 7: Write the reader-registry test** `src/test/java/ua/com/javarush/j4/io/TextReadersTest.java`

```java
package ua.com.javarush.j4.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class TextReadersTest {

    private final TextReaders readers = new TextReaders();

    @Test
    void picksPlainReaderForTxt() {
        assertTrue(readers.pick(Path.of("a.txt")) instanceof PlainTextReader);
    }

    @Test
    void picksMarkdownReaderForMd() {
        assertTrue(readers.pick(Path.of("a.md")) instanceof MarkdownReader);
    }

    @Test
    void picksGzipReaderForGz() {
        assertTrue(readers.pick(Path.of("a.txt.gz")) instanceof GzipTextReader);
    }

    @Test
    void fallsBackToPlainForUnknownExtension() {
        assertTrue(readers.pick(Path.of("a.dat")) instanceof PlainTextReader);
    }

    @Test
    void gzipReaderReadsCompressedContent(@TempDir Path dir) throws IOException {
        Path gz = dir.resolve("hello.txt.gz");
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(gz))) {
            out.write("Привіт".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals("Привіт", readers.pick(gz).read(gz));
    }
}
```

- [ ] **Step 8: Run tests to verify they pass**

Run: `./mvnw -q -Dtest='OutputNamingTest,TextReadersTest' test`
Expected: PASS (9 tests total).

- [ ] **Step 9: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/io src/test/java/ua/com/javarush/j4/io
git commit -m "feat: add output naming, pluggable text readers, text writer"
```

---

### Task 8: app package — request, Template-Method commands, CryptoService facade

**Files:**
- Create: `src/main/java/ua/com/javarush/j4/app/Operation.java`
- Create: `src/main/java/ua/com/javarush/j4/app/CryptoRequest.java`
- Create: `src/main/java/ua/com/javarush/j4/app/command/CryptoCommand.java`
- Create: `src/main/java/ua/com/javarush/j4/app/command/EncryptCommand.java`
- Create: `src/main/java/ua/com/javarush/j4/app/command/DecryptCommand.java`
- Create: `src/main/java/ua/com/javarush/j4/app/command/BruteForceCommand.java`
- Create: `src/main/java/ua/com/javarush/j4/app/CryptoService.java`
- Test: `src/test/java/ua/com/javarush/j4/app/CryptoServiceTest.java`

**Interfaces:**
- Consumes: `Cipher`, `CipherFactory` (Tasks 3-4); `Alphabets` (Task 2); `LanguageDetector`, `LanguageProfile`, `LanguageProfiles`, `DictionaryScorer`, `CaesarCracker` (Tasks 5-6); `TextReaders`, `TextWriter`, `OutputNaming` (Task 7).
- Produces:
  - `enum Operation { ENCRYPT, DECRYPT, BRUTE_FORCE }`.
  - `record CryptoRequest(Operation operation, Path file, Integer key, String cipherName, String keyword, String alphabetName)`.
  - `abstract class CryptoCommand` with `final Path execute() throws IOException` (Template Method) and abstract `String transform(String text)` / `Path outputPath(OutputNaming naming, Path input)`.
  - `EncryptCommand`, `DecryptCommand`, `BruteForceCommand`.
  - `CryptoService` with `Path execute(CryptoRequest request) throws IOException`.

- [ ] **Step 1: Write the failing test** `src/test/java/ua/com/javarush/j4/app/CryptoServiceTest.java`

```java
package ua.com.javarush.j4.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {

    private final CryptoService service = new CryptoService();

    private Path write(Path dir, String name, String content) throws IOException {
        Path p = dir.resolve(name);
        Files.writeString(p, content);
        return p;
    }

    @Test
    void encryptThenDecryptRoundTrips(@TempDir Path dir) throws IOException {
        Path input = write(dir, "msg.txt", "Hello, World!");

        Path encrypted = service.execute(
                new CryptoRequest(Operation.ENCRYPT, input, 5, "caesar", null, "default"));
        assertTrue(encrypted.getFileName().toString().contains("[ENCRYPTED]"));

        Path decrypted = service.execute(
                new CryptoRequest(Operation.DECRYPT, encrypted, 5, "caesar", null, "default"));
        assertEquals("Hello, World!", Files.readString(decrypted));
        assertTrue(decrypted.getFileName().toString().contains("[DECRYPTED]"));
        assertFalse(decrypted.getFileName().toString().contains("[ENCRYPTED]"));
    }

    @Test
    void bruteForceRecoversEnglishWithAutoDetection(@TempDir Path dir) throws IOException {
        String original = "The quick brown fox jumps over the lazy dog and the cat.";
        Path input = write(dir, "secret.txt", original);
        Path encrypted = service.execute(
                new CryptoRequest(Operation.ENCRYPT, input, 9, "caesar", null, "default"));

        Path cracked = service.execute(
                new CryptoRequest(Operation.BRUTE_FORCE, encrypted, null, "caesar", null, "auto"));

        assertEquals(original, Files.readString(cracked));
    }

    @Test
    void vigenereRoundTripsThroughService(@TempDir Path dir) throws IOException {
        Path input = write(dir, "v.txt", "ATTACKATDAWN");
        Path encrypted = service.execute(
                new CryptoRequest(Operation.ENCRYPT, input, null, "vigenere", "LEMON", "en"));
        Path decrypted = service.execute(
                new CryptoRequest(Operation.DECRYPT, encrypted, null, "vigenere", "LEMON", "en"));
        assertEquals("ATTACKATDAWN", Files.readString(decrypted));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=CryptoServiceTest test`
Expected: FAIL — `app` package does not exist.

- [ ] **Step 3: Create `Operation` and `CryptoRequest`**

`src/main/java/ua/com/javarush/j4/app/Operation.java`:
```java
package ua.com.javarush.j4.app;

/** The three things the tool can do. */
public enum Operation {
    ENCRYPT, DECRYPT, BRUTE_FORCE
}
```

`src/main/java/ua/com/javarush/j4/app/CryptoRequest.java`:
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
        String alphabetName) {
}
```

- [ ] **Step 4: Create `CryptoCommand` (Template Method base)**

`src/main/java/ua/com/javarush/j4/app/command/CryptoCommand.java`:
```java
package ua.com.javarush.j4.app.command;

import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.io.IOException;
import java.nio.file.Path;

/** Template Method: read → transform → name → write. Subclasses supply the two varying steps. */
public abstract class CryptoCommand {
    private final Path input;
    private final TextReaders readers;
    private final TextWriter writer;
    private final OutputNaming naming;

    protected CryptoCommand(Path input, TextReaders readers, TextWriter writer, OutputNaming naming) {
        this.input = input;
        this.readers = readers;
        this.writer = writer;
        this.naming = naming;
    }

    public final Path execute() throws IOException {
        String text = readers.pick(input).read(input);
        String result = transform(text);
        Path output = outputPath(naming, input);
        writer.write(output, result);
        return output;
    }

    protected abstract String transform(String text);

    protected abstract Path outputPath(OutputNaming naming, Path input);
}
```

- [ ] **Step 5: Create the three commands**

`src/main/java/ua/com/javarush/j4/app/command/EncryptCommand.java`:
```java
package ua.com.javarush.j4.app.command;

import ua.com.javarush.j4.cipher.Cipher;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.nio.file.Path;

public final class EncryptCommand extends CryptoCommand {
    private final Cipher cipher;

    public EncryptCommand(Path input, Cipher cipher, TextReaders readers, TextWriter writer, OutputNaming naming) {
        super(input, readers, writer, naming);
        this.cipher = cipher;
    }

    @Override
    protected String transform(String text) {
        return cipher.encrypt(text);
    }

    @Override
    protected Path outputPath(OutputNaming naming, Path input) {
        return naming.forEncrypt(input);
    }
}
```

`src/main/java/ua/com/javarush/j4/app/command/DecryptCommand.java`:
```java
package ua.com.javarush.j4.app.command;

import ua.com.javarush.j4.cipher.Cipher;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.nio.file.Path;

public final class DecryptCommand extends CryptoCommand {
    private final Cipher cipher;

    public DecryptCommand(Path input, Cipher cipher, TextReaders readers, TextWriter writer, OutputNaming naming) {
        super(input, readers, writer, naming);
        this.cipher = cipher;
    }

    @Override
    protected String transform(String text) {
        return cipher.decrypt(text);
    }

    @Override
    protected Path outputPath(OutputNaming naming, Path input) {
        return naming.forDecrypt(input);
    }
}
```

`src/main/java/ua/com/javarush/j4/app/command/BruteForceCommand.java`:
```java
package ua.com.javarush.j4.app.command;

import ua.com.javarush.j4.crack.CaesarCracker;
import ua.com.javarush.j4.crack.DictionaryScorer;
import ua.com.javarush.j4.crack.LanguageDetector;
import ua.com.javarush.j4.crack.LanguageProfile;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.nio.file.Path;

/** Brute-force: detect language (unless one is forced), sweep keys, write best decryption. */
public final class BruteForceCommand extends CryptoCommand {
    private final LanguageDetector detector;
    private final LanguageProfile forcedProfile; // null => auto-detect

    public BruteForceCommand(Path input, LanguageDetector detector, LanguageProfile forcedProfile,
                             TextReaders readers, TextWriter writer, OutputNaming naming) {
        super(input, readers, writer, naming);
        this.detector = detector;
        this.forcedProfile = forcedProfile;
    }

    @Override
    protected String transform(String text) {
        LanguageProfile profile = forcedProfile != null ? forcedProfile : detector.detect(text);
        CaesarCracker cracker = new CaesarCracker(profile.alphabet(), new DictionaryScorer(profile));
        return cracker.crack(text).plaintext();
    }

    @Override
    protected Path outputPath(OutputNaming naming, Path input) {
        return naming.forDecrypt(input);
    }
}
```

- [ ] **Step 6: Create `CryptoService` facade**

`src/main/java/ua/com/javarush/j4/app/CryptoService.java`:
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
public final class CryptoService {
    private final TextReaders readers = new TextReaders();
    private final TextWriter writer = new TextWriter();
    private final OutputNaming naming = new OutputNaming();
    private final CipherFactory ciphers = new CipherFactory();
    private final LanguageDetector detector = new LanguageDetector();

    public Path execute(CryptoRequest request) throws IOException {
        return command(request).execute();
    }

    private CryptoCommand command(CryptoRequest request) {
        Path file = request.file();
        return switch (request.operation()) {
            case ENCRYPT -> new EncryptCommand(file, cipher(request), readers, writer, naming);
            case DECRYPT -> new DecryptCommand(file, cipher(request), readers, writer, naming);
            case BRUTE_FORCE -> new BruteForceCommand(
                    file, detector, forcedProfile(request.alphabetName()), readers, writer, naming);
        };
    }

    private Cipher cipher(CryptoRequest request) {
        Alphabet alphabet = Alphabets.byName(request.alphabetName());
        return ciphers.create(request.cipherName(), alphabet, request.key(), request.keyword());
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

- [ ] **Step 7: Run tests to verify they pass**

Run: `./mvnw -q -Dtest=CryptoServiceTest test`
Expected: PASS (3 tests).

- [ ] **Step 8: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/app src/test/java/ua/com/javarush/j4/app/CryptoServiceTest.java
git commit -m "feat: add CryptoService facade and Template-Method commands"
```

---

### Task 9: cli package — picocli CryptoCli, rewrite Main, delete old flat classes

**Files:**
- Create: `src/main/java/ua/com/javarush/j4/cli/CryptoCli.java`
- Modify: `src/main/java/ua/com/javarush/j4/Main.java`
- Delete: `src/main/java/ua/com/javarush/j4/ArgumentParser.java`
- Delete: `src/main/java/ua/com/javarush/j4/Arguments.java`
- Delete: `src/main/java/ua/com/javarush/j4/BruteForce.java`
- Delete: `src/main/java/ua/com/javarush/j4/CaesarCipher.java`
- Delete: `src/main/java/ua/com/javarush/j4/Command.java`
- Delete: `src/main/java/ua/com/javarush/j4/OutputFile.java`
- Test: `src/test/java/ua/com/javarush/j4/cli/CryptoCliTest.java`

**Interfaces:**
- Consumes: `CryptoService`, `CryptoRequest`, `Operation` (Task 8); `InvalidArgumentsException` (Task 2).
- Produces:
  - `CryptoCli implements Callable<Integer>` (picocli `@Command`), with `int run(String[] args)` that parses and executes without throwing.
  - `Main.main(String[])` delegating to `CryptoCli`, never propagating exceptions.

- [ ] **Step 1: Write the failing CLI integration test** `src/test/java/ua/com/javarush/j4/cli/CryptoCliTest.java`

```java
package ua.com.javarush.j4.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CryptoCliTest {

    private List<Path> list(Path dir) {
        try (Stream<Path> s = Files.list(dir)) {
            return s.toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void rot13EncryptViaCipherFlag(@TempDir Path dir) throws IOException {
        Path input = dir.resolve("a.txt");
        Files.writeString(input, "HELLO");

        new CryptoCli().run(new String[]{"-e", "-c", "rot13", "-f", input.toString()});

        Path out = dir.resolve("a [ENCRYPTED].txt");
        assertEquals("URYYB", Files.readString(out));
    }

    @Test
    void vigenereEncryptViaKeywordFlag(@TempDir Path dir) throws IOException {
        Path input = dir.resolve("b.txt");
        Files.writeString(input, "HELLO");

        new CryptoCli().run(new String[]{"-e", "-c", "vigenere", "--keyword", "KEY", "-f", input.toString()});

        assertEquals("RIJVS", Files.readString(dir.resolve("b [ENCRYPTED].txt")));
    }

    @Test
    void unknownCipherWritesNothingAndDoesNotThrow(@TempDir Path dir) throws IOException {
        Path input = dir.resolve("c.txt");
        Files.writeString(input, "HELLO");
        List<Path> before = list(dir);

        assertDoesNotThrow(() ->
                new CryptoCli().run(new String[]{"-e", "-c", "enigma", "-f", input.toString()}));
        assertEquals(before, list(dir));
    }

    @Test
    void helpReturnsZeroAndWritesNothing(@TempDir Path dir) {
        List<Path> before = list(dir);
        int code = new CryptoCli().run(new String[]{"--help"});
        assertEquals(0, code);
        assertEquals(before, list(dir));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=CryptoCliTest test`
Expected: FAIL — `cli` package does not exist.

- [ ] **Step 3: Create `CryptoCli`**

`src/main/java/ua/com/javarush/j4/cli/CryptoCli.java`:
```java
package ua.com.javarush.j4.cli;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ua.com.javarush.j4.app.CryptoRequest;
import ua.com.javarush.j4.app.CryptoService;
import ua.com.javarush.j4.app.Operation;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Command-line front end. The legacy contract (-e/-d/-b, -k, -f) is preserved;
 * new optional flags (--cipher, --keyword, --alphabet) are purely additive.
 */
@Command(name = "cryptanalyzer", mixinStandardHelpOptions = true, version = "cryptanalyzer 2.0",
        description = "Caesar-family cipher tool: encrypt, decrypt, or brute-force a text file.")
public final class CryptoCli implements Callable<Integer> {

    /** Exactly one command must be chosen. */
    @ArgGroup(multiplicity = "1")
    private CommandSelection command;

    static final class CommandSelection {
        @Option(names = "-e", description = "Encrypt") boolean encrypt;
        @Option(names = "-d", description = "Decrypt") boolean decrypt;
        @Option(names = "-b", description = "Brute-force (no key)") boolean brute;
    }

    @Option(names = "-k", description = "Key (required for -e/-d with the caesar cipher)")
    private Integer key;

    @Option(names = "-f", required = true, description = "Input file path")
    private Path file;

    @Option(names = {"-c", "--cipher"}, defaultValue = "caesar",
            description = "Cipher: caesar, rot13, atbash, vigenere")
    private String cipher;

    @Option(names = "--keyword", description = "Keyword for the vigenere cipher")
    private String keyword;

    @Option(names = {"-a", "--alphabet"}, defaultValue = "default",
            description = "Alphabet/language: en, ua, ru, auto")
    private String alphabet;

    @Override
    public Integer call() throws Exception {
        Operation operation = selectedOperation();
        new CryptoService().execute(
                new CryptoRequest(operation, file, key, cipher, keyword, alphabet));
        return 0;
    }

    private Operation selectedOperation() {
        if (command.brute) {
            return Operation.BRUTE_FORCE;
        }
        return command.encrypt ? Operation.ENCRYPT : Operation.DECRYPT;
    }

    /** Parses and executes; never throws — errors are reported and a non-zero code returned. */
    public int run(String[] args) {
        return new CommandLine(this)
                .setExecutionExceptionHandler((ex, cmd, parseResult) -> {
                    cmd.getErr().println("Error: " + ex.getMessage());
                    return 1;
                })
                .execute(args);
    }
}
```

- [ ] **Step 4: Rewrite `Main`**

`src/main/java/ua/com/javarush/j4/Main.java`:
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

- [ ] **Step 5: Delete the obsolete flat classes**

```bash
git rm src/main/java/ua/com/javarush/j4/ArgumentParser.java \
       src/main/java/ua/com/javarush/j4/Arguments.java \
       src/main/java/ua/com/javarush/j4/BruteForce.java \
       src/main/java/ua/com/javarush/j4/CaesarCipher.java \
       src/main/java/ua/com/javarush/j4/Command.java \
       src/main/java/ua/com/javarush/j4/OutputFile.java
```

- [ ] **Step 6: Run the new CLI test**

Run: `./mvnw -q -Dtest=CryptoCliTest test`
Expected: PASS (4 tests).

- [ ] **Step 7: Run the FULL suite, including the locked contract and Ukrainian cases**

Run: `./mvnw test -Dtest.lang.ua=true`
Expected: BUILD SUCCESS — `MainTest` (all nested groups, EN + UA) and every new unit/integration test pass.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/ua/com/javarush/j4/cli src/main/java/ua/com/javarush/j4/Main.java src/test/java/ua/com/javarush/j4/cli/CryptoCliTest.java
git commit -m "feat: add picocli CLI, rewire Main, remove legacy flat classes"
```

---

### Task 10: docs + packaging verification

**Files:**
- Modify: `CLAUDE.md`
- Modify: `readme.md`
- Test: build + run the jar (manual verification, no new test file).

**Interfaces:**
- Consumes: the finished application.
- Produces: updated docs describing the new architecture; a verified runnable fat jar.

- [ ] **Step 1: Update `CLAUDE.md` Architecture section.** Replace the "The starter ships exactly one class" paragraph and the single-`Main` bullet with the real package map. Apply this edit to the `## Architecture` section:

```markdown
## Architecture

Single-module Maven project, package root `ua.com.javarush.j4`, organised by responsibility:

- `Main` — entry point; delegates to the picocli CLI and never propagates exceptions.
- `cli/` — `CryptoCli` (picocli `@Command`); parses the legacy `-e/-d/-b`, `-k`, `-f` contract plus additive `--cipher`, `--keyword`, `--alphabet` flags.
- `app/` — `CryptoService` facade + `command/` (Template-Method `CryptoCommand`: Encrypt/Decrypt/BruteForce).
- `cipher/` — `Cipher` strategy + Caesar/ROT13/Atbash/Vigenère + `CipherFactory`.
- `alphabet/` — `Alphabet`/`CharacterRing` value objects + `Alphabets` registry (EN/UA/RU + composite default).
- `crack/` — `Cracker`/`CaesarCracker`, pluggable `FitnessScorer` (dictionary + frequency), `LanguageDetector`/`LanguageProfile`.
- `io/` — `TextReader` strategies (txt/md/gz) + `TextReaders` registry, `TextWriter`, `OutputNaming`.
- `error/` — `CryptanalysisException` hierarchy.

`MainTest` remains the authoritative externally-observable contract; the package layout above is the internal design that satisfies it.
```

- [ ] **Step 2: Update the build/run commands in `CLAUDE.md`.** The jar is now a shaded fat jar; confirm the run example still reads:

```
- Package the runnable jar: `./mvnw package` (output `target/J4-M1-FP-1.0-SNAPSHOT.jar`, a shaded fat jar including picocli)
- Run the jar: `java -jar target/J4-M1-FP-1.0-SNAPSHOT.jar -e -k 5 -f path.txt`
- Show help: `java -jar target/J4-M1-FP-1.0-SNAPSHOT.jar --help`
```

Apply this by editing the existing "Package the runnable jar" / "Run the jar" bullet lines in the `## Build, test, run` section to match the above (add the shaded-jar note and the `--help` line).

- [ ] **Step 3: Add a short "New capabilities" note to `readme.md`.** Append this section near the project overview:

```markdown
## Extended capabilities (reimplementation)

Beyond the base Caesar cryptanalyzer, this build supports:

- **Ciphers** (`-c/--cipher`): `caesar` (default), `rot13`, `atbash`, `vigenere` (`--keyword <word>`).
- **Alphabets** (`-a/--alphabet`): `en`, `ua`, `ru`, `auto` (default handles English + Ukrainian).
- **File formats**: `.txt`, `.md`, and gzip `.gz`, chosen automatically by extension.
- **Brute-force**: language auto-detection + pluggable fitness scoring (common-words and letter-frequency).

The original `-e/-d/-b -k -f` command line is unchanged.
```

- [ ] **Step 4: Build the fat jar and verify it runs end-to-end**

```bash
./mvnw -q clean package
printf 'Hello, World!' > /tmp/cli-demo.txt
java -jar target/J4-M1-FP-1.0-SNAPSHOT.jar -e -k 5 -f /tmp/cli-demo.txt
cat "/tmp/cli-demo [ENCRYPTED].txt"
```
Expected: `clean package` is BUILD SUCCESS (all tests run, including `MainTest`); the encrypted file contains `Mjqqt, Btwqi!`.

- [ ] **Step 5: Verify `--help` works from the jar**

Run: `java -jar target/J4-M1-FP-1.0-SNAPSHOT.jar --help`
Expected: picocli usage text listing `-e`, `-d`, `-b`, `-k`, `-f`, `-c`, `--keyword`, `-a`.

- [ ] **Step 6: Commit**

```bash
git add CLAUDE.md readme.md
git commit -m "docs: describe reimplemented architecture and extended capabilities"
```

---

## Self-Review

**Spec coverage:**
- Multiple ciphers → Tasks 3-4 (Caesar, ROT13, Atbash, Vigenère + factory). ✓
- Multiple alphabets → Task 2 (EN, UA, RU, composite default). ✓
- Multiple file readers → Task 7 (txt/md/gz + registry). ✓
- Pluggable brute-force scorers → Tasks 5-6 (FitnessScorer: dictionary + frequency; CaesarCracker takes a scorer). ✓
- picocli CLI, backward-compatible contract → Task 9 (ArgGroup for -e/-d/-b, additive flags). ✓
- MainTest stays green → Task 9 Step 7 runs full suite incl. UA. ✓
- ValidationTests (no throw / no file on bad input) → Task 9 (`run()` execution-exception handler; validation before any write). ✓
- Build: picocli dep + shade fat jar; CI `mvn package` green → Tasks 1 and 10. ✓
- Error handling hierarchy → Task 2 (error package). ✓
- Docs updated → Task 10. ✓

**Placeholder scan:** No TBD/TODO; every code step shows complete code; every command shows expected output.

**Type consistency:** `Cipher.encrypt/decrypt(String)`, `Alphabet.shift/mirror/position/keyspaceSize`, `CipherFactory.create(String, Alphabet, Integer, String)`, `FitnessScorer.score(String)`, `CaesarCracker(Alphabet, FitnessScorer)`, `CrackResult.key()/plaintext()`, `CryptoRequest(Operation, Path, Integer, String, String, String)`, `CryptoService.execute(CryptoRequest)`, `CryptoCommand.execute()/transform/outputPath`, `CryptoCli.run(String[])` — names and signatures are consistent across producing and consuming tasks.

**Scope:** Single cohesive subsystem (one CLI tool); appropriate for one plan.
```
