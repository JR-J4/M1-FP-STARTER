package ua.com.javarush.j4.io;

import java.io.IOException;
import java.nio.file.Path;

/** Writes text content to a destination. */
public interface TextWriter {
    void write(Path path, String content) throws IOException;
}
