package ua.com.javarush.j4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Точка входу криптоаналізатора шифру Цезаря. Розбирає аргументи, читає вхідний
 * файл, виконує обрану команду і записує результат у новий файл. Будь-який виняток
 * (невалідні аргументи, відсутній файл) ловиться тут і не пропускається назовні.
 */
public class Main {

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception e) {
            System.out.println("Помилка: " + e.getMessage());
        }
    }

    private static void run(String[] args) throws IOException {
        Arguments arguments = ArgumentParser.parse(args);
        Path input = arguments.file();
        String text = Files.readString(input);
        CaesarCipher cipher = new CaesarCipher();

        switch (arguments.command()) {
            case ENCRYPT -> write(OutputFile.forEncrypt(input), cipher.encrypt(text, arguments.key()));
            case DECRYPT -> write(OutputFile.forDecrypt(input), cipher.decrypt(text, arguments.key()));
            case BRUTE_FORCE -> {
                int key = new BruteForce(cipher).findKey(text);
                write(OutputFile.forDecrypt(input), cipher.decrypt(text, key));
            }
        }
    }

    private static void write(Path output, String content) throws IOException {
        Files.writeString(output, content);
    }
}
