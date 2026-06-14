package ua.com.javarush.j4.filemanager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileService {

    public String readFile(Path filePath) throws IOException {
        return Files.readString(filePath);
    }

    public void writeFile(Path path, String content) throws IOException {
        Files.writeString(path, content);
    }
}