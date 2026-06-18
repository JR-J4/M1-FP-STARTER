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
