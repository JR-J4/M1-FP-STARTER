package ua.com.javarush.j4.app.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.app.command.CryptoCommand;
import ua.com.javarush.j4.app.command.EncryptCommand;
import ua.com.javarush.j4.cipher.CaesarCipher;
import ua.com.javarush.j4.concurrent.PooledTaskExecutor;
import ua.com.javarush.j4.concurrent.TaskExecutor;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchProcessorTest {

    private static CryptoCommand encryptCommand(Path input) {
        return new EncryptCommand(input, new CaesarCipher(Alphabets.DEFAULT, 3),
                new TextReaders(), new TextWriter(), new OutputNaming());
    }

    @Test
    void processesEveryFileAndReportsInInputOrder(@TempDir Path dir) throws IOException {
        List<Path> inputs = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Path file = dir.resolve("file" + i + ".txt");
            Files.writeString(file, "hello world " + i, StandardCharsets.UTF_8);
            inputs.add(file);
        }

        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            BatchReport report = new BatchProcessor(executor)
                    .process(inputs, BatchProcessorTest::encryptCommand);

            assertEquals(6, report.succeeded());
            assertEquals(0, report.failed());
            assertFalse(report.anyFailed());
            assertEquals(inputs, report.outcomes().stream().map(FileOutcome::input).toList());
        }
    }

    @Test
    void oneFailureDoesNotAbortTheRestOfTheBatch(@TempDir Path dir) throws IOException {
        Path good = dir.resolve("good.txt");
        Files.writeString(good, "hello", StandardCharsets.UTF_8);
        Path missing = dir.resolve("missing.txt");
        Path alsoGood = dir.resolve("also-good.txt");
        Files.writeString(alsoGood, "world", StandardCharsets.UTF_8);

        List<Path> inputs = List.of(good, missing, alsoGood);
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            BatchReport report = new BatchProcessor(executor)
                    .process(inputs, BatchProcessorTest::encryptCommand);

            assertEquals(2, report.succeeded());
            assertEquals(1, report.failed());
            assertTrue(report.anyFailed());
            assertEquals(inputs, report.outcomes().stream().map(FileOutcome::input).toList());
            assertTrue(report.outcomes().get(0).succeeded());
            assertFalse(report.outcomes().get(1).succeeded());
            assertTrue(report.outcomes().get(2).succeeded());
        }
    }

    @Test
    void writesAnOutputFilePerInput(@TempDir Path dir) throws IOException {
        Path input = dir.resolve("note.txt");
        Files.writeString(input, "abc", StandardCharsets.UTF_8);

        try (TaskExecutor executor = new PooledTaskExecutor(2)) {
            BatchReport report = new BatchProcessor(executor)
                    .process(List.of(input), BatchProcessorTest::encryptCommand);

            Path output = report.outcomes().get(0).output();
            assertEquals("note [ENCRYPTED].txt", output.getFileName().toString());
            assertEquals("def", Files.readString(output, StandardCharsets.UTF_8));
        }
    }
}
