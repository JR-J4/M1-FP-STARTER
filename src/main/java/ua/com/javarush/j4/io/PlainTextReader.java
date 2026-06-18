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
