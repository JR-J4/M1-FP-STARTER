package ua.com.javarush.j4.io;

import java.nio.file.Path;
import java.util.List;

/** Picks the right reader for a path by extension; falls back to plain text. */
public final class TextReaders {
    private final List<TextReader> readers;
    private final TextReader plain = new PlainTextReader();

    public TextReaders() {
        this.readers = List.of(new GzipTextReader(), new MarkdownReader(), plain);
    }

    public TextReader pick(Path path) {
        return readers.stream()
                .filter(reader -> reader.supports(path))
                .findFirst()
                .orElse(plain);
    }
}
