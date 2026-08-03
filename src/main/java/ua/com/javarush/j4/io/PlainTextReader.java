package ua.com.javarush.j4.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/** Reads .txt files (and acts as the universal fallback). */
public final class PlainTextReader implements TextReader {

    private final HashSet<String> supportedFileTypes = new HashSet<>();

    public PlainTextReader() {
        supportedFileTypes.add(".txt");
    }

    @Override
    public boolean supports(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt");
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
}
