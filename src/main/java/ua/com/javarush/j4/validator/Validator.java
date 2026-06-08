package ua.com.javarush.j4.validator;

import ua.com.javarush.j4.exception.FileException;
import ua.com.javarush.j4.exception.KeyException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Validator {

    public Path validatePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new FileException("Path can't be null or empty");
        }

        Path normalizedPath = Paths.get(path).normalize();

        try {
            normalizedPath = normalizedPath.toAbsolutePath();
        } catch (Exception e) {
            throw new FileException("Incorrect path format: " + path);
        }

        if (!Files.exists(normalizedPath)) {
            throw new FileException("File or directory is not found: " + path);
        }

        if (!Files.isRegularFile(normalizedPath)) {
            throw new FileException("Path is not a file: " + path);
        }

        if (!Files.isReadable(normalizedPath)) {
            throw new FileException("Reading is forbidden: " + path);
        }

        return normalizedPath;
    }

    public int validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new KeyException("The number can't be null or empty");
        }

        key = key.trim();

        try {
            return Integer.parseInt(key);

        } catch (NumberFormatException e) {
            throw new KeyException("Incorrect format of number");
        }
    }
}
