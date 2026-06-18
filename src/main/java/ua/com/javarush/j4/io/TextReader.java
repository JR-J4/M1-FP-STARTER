package ua.com.javarush.j4.io;

import java.io.IOException;
import java.nio.file.Path;

/** Reads the textual content of a file. Implementations declare which paths they support. */
public interface TextReader {
    boolean supports(Path path);

    String read(Path path) throws IOException;
}
