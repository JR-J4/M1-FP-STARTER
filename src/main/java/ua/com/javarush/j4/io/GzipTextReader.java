package ua.com.javarush.j4.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/** Reads gzip-compressed UTF-8 text files (.gz). */
public final class GzipTextReader implements TextReader {

    private final HashSet<String> supportedFileTypes = new HashSet<>();

    public GzipTextReader() {
        supportedFileTypes.add(".gz");
        supportedFileTypes.add(".gzip");
    }

    @Override
    public boolean supports(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gz");
    }

    @Override
    public HashMap<String, String> getMeta() {
        return null;
    }

    @Override
    public String read(Path path) throws IOException {
        try (InputStream in = new GZIPInputStream(Files.newInputStream(path))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Override
    public int compareTo(TextReader o) {
        return supportedFileTypes.size() - o.getSupportedFileTypes().size();
    }

  public HashSet<String> getSupportedFileTypes() {
    return supportedFileTypes;
  }
}
