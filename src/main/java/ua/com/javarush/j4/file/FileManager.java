package ua.com.javarush.j4.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileManager {

    public void write(Path path, String content) throws IOException {
        //не ефективний метод, подумати
        Files.writeString(path, content);
    }

    public String read(Path path) throws IOException{
        return Files.readString(path);
    }



    //прочитати файл

  // Files.readString(pathOriginal);

    //створити файл
   // Files.create
    //записати у файл
}
