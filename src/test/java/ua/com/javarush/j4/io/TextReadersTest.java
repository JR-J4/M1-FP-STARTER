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

    private final TextReaders readers = TextReaders.withDefaults();

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
    void picksPdfReaderForPdf() {
        assertTrue(TextReaders.withDefaults().pick(java.nio.file.Path.of("a.pdf")) instanceof PdfReader);
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
