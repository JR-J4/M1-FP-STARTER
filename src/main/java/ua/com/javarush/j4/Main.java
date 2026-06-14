package ua.com.javarush.j4;

import ua.com.javarush.j4.crypty.BruteForce;
import ua.com.javarush.j4.crypty.Cipher;
import ua.com.javarush.j4.fileManager.FileService;
import ua.com.javarush.j4.fileManager.OutputFileName;
import ua.com.javarush.j4.language.LanguageDetector;
import ua.com.javarush.j4.runner.ArgumentParser;
import ua.com.javarush.j4.runner.RunOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            ArgumentParser parser = new ArgumentParser();
            RunOptions options = parser.parse(args);

            FileService fileService = new FileService();

            String text = fileService.readFile(options.path());
            List<Character> alphabet = LanguageDetector.detect(text);
            Cipher cipher = new Cipher(alphabet);

            String result;

            switch (options.command()) {
                case ENCRYPT -> result = cipher.encrypt(text, options.key());
                case DECRYPT -> result = cipher.decrypt(text, options.key());
                case BRUTE_FORCE -> result = cipher.decrypt(text, BruteForce.bruteForce(text, cipher));
                default -> throw new IllegalArgumentException("Unknown command");
            }

            Path outputPath = OutputFileName.build(options.path(), options.command());
            fileService.writeFile(outputPath, result);
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}