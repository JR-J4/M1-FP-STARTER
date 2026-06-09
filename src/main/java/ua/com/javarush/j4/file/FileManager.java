package ua.com.javarush.j4.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileManager {

    public static String read(Path path) throws IOException {
        return Files.readString(path);
    }

    public static void write(Path path, String content) throws IOException {
        Files.writeString(path, content);
    }

    public static Path buildOutputPath(Path input, String label) {
        String name = input.getFileName().toString();
        String newName = name
                .replace("[ENCRYPTED]", label)
                .replace("[DECRYPTED]", label);
        if (newName.equals(name)) {
            int dot = name.lastIndexOf('.');
            newName = (dot != -1)
                    ? name.substring(0, dot) + " " + label + name.substring(dot)
                    : name + " " + label;
        }
        return input.resolveSibling(newName);
    }
}
