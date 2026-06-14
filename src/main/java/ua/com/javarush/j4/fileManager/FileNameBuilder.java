package ua.com.javarush.j4.fileManager;

import java.nio.file.Path;

public class FileNameBuilder {
    public Path encrypted(Path file) {
        String name = file.getFileName().toString();
        int dotIndex = name.lastIndexOf('.');
        String newName = name.substring(0, dotIndex)
                + " [ENCRYPTED]" + name.substring(dotIndex);
        return file.resolveSibling(newName);
    }

    public Path decrypted(Path file) {
        String fileName = file.getFileName().toString();
        fileName = fileName.replace(" [ENCRYPTED]", " [DECRYPTED]");
        return file.resolveSibling(fileName);
    }
}