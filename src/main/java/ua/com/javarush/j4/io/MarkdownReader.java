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
