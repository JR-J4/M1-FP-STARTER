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
