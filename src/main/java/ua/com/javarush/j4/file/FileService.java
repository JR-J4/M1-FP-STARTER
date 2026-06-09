package ua.com.javarush.j4.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileService {
    public static String read(Path path) throws IOException {
        return Files.readString(path);
    }

    public static void write(Path path, String content) throws IOException {
         Files.writeString(path,content);
    }
}
