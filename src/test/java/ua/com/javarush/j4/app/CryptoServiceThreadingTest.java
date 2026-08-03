package ua.com.javarush.j4.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.com.javarush.j4.concurrent.ParallelPolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CryptoServiceThreadingTest {

    private static String largeEnglishText() {
        StringBuilder text = new StringBuilder();
        while (text.length() < ParallelPolicy.MIN_CHARS_FOR_TRANSFORM * 2) {
            text.append("to be or not to be that is the question whether it is nobler\n");
        }
        return text.toString();
    }

    private static Path write(Path dir, String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private static String encryptWith(Path dir, int threads, String content) throws IOException {
        Path input = write(dir, "in-" + threads + ".txt", content);
        try (CryptoService service = new CryptoService(ParallelPolicy.of(threads))) {
            Path output = service.execute(new CryptoRequest(
                    Operation.ENCRYPT, input, 5, "caesar", null, "default", "dictionary"));
            return Files.readString(output, StandardCharsets.UTF_8);
        }
    }

    @Test
    void encryptionIsIdenticalWhateverTheThreadCount(@TempDir Path dir) throws IOException {
        String text = largeEnglishText();

        assertEquals(encryptWith(dir, 1, text), encryptWith(dir, 8, text));
    }

    @Test
    void bruteForceIsIdenticalWhateverTheThreadCount(@TempDir Path dir) throws IOException {
        String plaintext = largeEnglishText();
        Path sequentialInput = write(dir, "seq.txt", plaintext);
        Path parallelInput = write(dir, "par.txt", plaintext);

        String sequential = crackRoundTrip(sequentialInput, 1);
        String parallel = crackRoundTrip(parallelInput, 8);

        assertEquals(plaintext, sequential);
        assertEquals(sequential, parallel);
    }

    private static String crackRoundTrip(Path input, int threads) throws IOException {
        try (CryptoService service = new CryptoService(ParallelPolicy.of(threads))) {
            Path encrypted = service.execute(new CryptoRequest(
                    Operation.ENCRYPT, input, 5, "caesar", null, "en", "dictionary"));
            Path cracked = service.execute(new CryptoRequest(
                    Operation.BRUTE_FORCE, encrypted, null, "caesar", null, "en", "dictionary"));
            return Files.readString(cracked, StandardCharsets.UTF_8);
        }
    }
}
