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

    /**
     * Integration test for the --scorer=frequency flag end-to-end.
     * Encrypts a long English passage with a known key, then brute-forces it
     * via the CLI with -s frequency and asserts exact recovery of the original.
     */
    @Test
    void frequencyScorerBruteForceRecoversCiphertext(@TempDir Path dir) throws IOException {
        // A multi-sentence English passage long enough for frequency analysis to be reliable.
        String original =
                "It is a truth universally acknowledged that a single man in possession of a " +
                "good fortune must be in want of a wife. However little known the feelings or " +
                "views of such a man may be on his first entering a neighbourhood, this truth " +
                "is so well fixed in the minds of the surrounding families that he is considered " +
                "as the rightful property of some one or other of their daughters.";

        Path input = dir.resolve("prose.txt");
        Files.writeString(input, original);

        // Encrypt with key 13 via the CLI
        new CryptoCli().run(new String[]{"-e", "-k", "13", "-f", input.toString()});
        Path encrypted = dir.resolve("prose [ENCRYPTED].txt");
        assertTrue(Files.exists(encrypted), "encrypted file should exist");

        // Brute-force with frequency scorer
        new CryptoCli().run(new String[]{"-b", "-s", "frequency", "-f", encrypted.toString()});
        Path cracked = dir.resolve("prose [DECRYPTED].txt");

        assertEquals(original, Files.readString(cracked),
                "frequency scorer should recover the exact original text");
    }
}
