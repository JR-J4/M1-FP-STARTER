package ua.com.javarush.j4.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.com.javarush.j4.cipher.CipherSpec;

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
                new CryptoRequest(Operation.ENCRYPT, input, new CipherSpec("caesar", 5, null), "default", "dictionary"));
        assertTrue(encrypted.getFileName().toString().contains("[ENCRYPTED]"));

        Path decrypted = service.execute(
                new CryptoRequest(Operation.DECRYPT, encrypted, new CipherSpec("caesar", 5, null), "default", "dictionary"));
        assertEquals("Hello, World!", Files.readString(decrypted));
        assertTrue(decrypted.getFileName().toString().contains("[DECRYPTED]"));
        assertFalse(decrypted.getFileName().toString().contains("[ENCRYPTED]"));
    }

    @Test
    void bruteForceRecoversEnglishWithAutoDetection(@TempDir Path dir) throws IOException {
        String original = "The quick brown fox jumps over the lazy dog and the cat.";
        Path input = write(dir, "secret.txt", original);
        Path encrypted = service.execute(
                new CryptoRequest(Operation.ENCRYPT, input, new CipherSpec("caesar", 9, null), "default", "dictionary"));

        Path cracked = service.execute(
                new CryptoRequest(Operation.BRUTE_FORCE, encrypted, new CipherSpec("caesar", null, null), "auto", "dictionary"));

        assertEquals(original, Files.readString(cracked));
    }

    @Test
    void vigenereRoundTripsThroughService(@TempDir Path dir) throws IOException {
        Path input = write(dir, "v.txt", "ATTACKATDAWN");
        Path encrypted = service.execute(
                new CryptoRequest(Operation.ENCRYPT, input, new CipherSpec("vigenere", null, "LEMON"), "en", "dictionary"));
        Path decrypted = service.execute(
                new CryptoRequest(Operation.DECRYPT, encrypted, new CipherSpec("vigenere", null, "LEMON"), "en", "dictionary"));
        assertEquals("ATTACKATDAWN", Files.readString(decrypted));
    }
}
