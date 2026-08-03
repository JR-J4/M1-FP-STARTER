package ua.com.javarush.j4.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/** Reads Markdown documents as plain text (content passes through unchanged). */
public final class MarkdownReader implements TextReader, Cloneable {

    private final HashSet<String> supportedFileTypes = new HashSet<>();

    public MarkdownReader() {
        supportedFileTypes.add(".md");
        supportedFileTypes.add(".markdown");
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        MarkdownReader that = (MarkdownReader) o;
        return supportedFileTypes.equals(that.supportedFileTypes);
    }

    @Override
    public int hashCode() {
        return supportedFileTypes.hashCode();
    }

    @Override
    public String toString() {
        return "MarkdownReader : supportedFileTypes " + supportedFileTypes;
    }

    @Override
    public boolean supports(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".md") || name.endsWith(".markdown");
    }

    public void addSupportedFileType(String type){
        supportedFileTypes.add(type);
    }

    @Override
    public HashMap<String, String> getMeta() {
        return null;
    }

    @Override
    public String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Override
    public int compareTo(TextReader o) {
        return supportedFileTypes.size() - o.getSupportedFileTypes().size();
    }

    public HashSet<String> getSupportedFileTypes() {
        return supportedFileTypes;
    }

    @Override
    public MarkdownReader clone() throws CloneNotSupportedException {
        return (MarkdownReader) super.clone();
    }
}
