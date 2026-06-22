package ua.com.javarush.j4.io;

import java.nio.file.Path;
import java.util.List;

// C:\Users\foo\bar

// var/asd/asdasd/

/** Picks the right reader for a path by extension; falls back to plain text. */
public final class TextReaders {
    private final List<TextReader> readers;
    private final TextReader plain;

    public TextReaders(List<TextReader> readers) {
        this.readers = List.copyOf(readers);
        this.plain = readers.stream()
                .filter(r -> r instanceof PlainTextReader)
                .findFirst()
                .orElseGet(PlainTextReader::new);
    }

    public TextReader pick(Path path) {
        return readers.stream()
                .filter(reader -> reader.supports(path))
                .findFirst()
                .orElse(plain);
    }

    /** The built-in reader set, ordered so specific formats win before the plain fallback. */
    public static TextReaders withDefaults() {
        return new TextReaders(List.of(
                new GzipTextReader(), new MarkdownReader(), new PdfReader(), new PlainTextReader()));
    }
}
