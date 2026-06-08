package ua.com.javarush.j4.fileManager;

import ua.com.javarush.j4.exception.FileException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileManager {
    protected static final String ENCRYPTED = " [ENCRYPTED]";
    protected static final String DECRYPTED = " [DECRYPTED]";

    public String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new FileException("File not found at path: " + path);
        }
    }

    public void writeToFile(Path inputPath, boolean isEncrypt, String text) {
        try {
            Path outputPath = makeOutputPath(inputPath, isEncrypt);
            Files.writeString(outputPath, text);
        } catch (IOException e) {
            throw new FileException("Can't make file at path: " + inputPath);
        }
    }

    private Path makeOutputPath(Path inputPath, boolean isEncrypt) {
        Path parentPath = inputPath.getParent();
        StringBuilder result = new StringBuilder();
        String fileName = inputPath.getFileName().toString();
        String prefix = isEncrypt ? ENCRYPTED : DECRYPTED;

        int indexOfSpace = fileName.lastIndexOf(" ");
        int indexOfDot = fileName.lastIndexOf(".");
        if (indexOfSpace == -1) {
            result.append(fileName.substring(0, indexOfDot));
        } else {
            result.append(fileName.substring(0, indexOfSpace));
        }
        result.append(prefix);
        result.append(fileName.substring(indexOfDot, fileName.length()));
        return parentPath.resolve(result.toString());
    }
}
