package ua.com.javarush.j4.io;

import java.nio.file.Path;
import java.util.List;

// C:\Users\foo\bar

// var/asd/asdasd/

/** Picks the right reader for a path by extension; falls back to plain text. */
public final class TextReaders {
    private final List<TextReader> readers;
    private final TextReader plain;

    // List<? extends TextReader>: "producer extends" (PECS). We only read from this
    // list, so accept a list of any subtype, e.g. a List<PlainTextReader>.
    public TextReaders(List<? extends TextReader> readers) {
        this.readers = List.copyOf(readers);
        // Derive from the copied field (a plain List<TextReader>), not the wildcard
        // parameter, so the PlainTextReader::new fallback unifies cleanly.
        this.plain = this.readers.stream()
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
