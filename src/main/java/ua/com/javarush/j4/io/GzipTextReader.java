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
