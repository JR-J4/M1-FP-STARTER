package ua.com.javarush.j4.application;

import ua.com.javarush.j4.crypher.BruteForce;
import ua.com.javarush.j4.crypher.Cipher;
import ua.com.javarush.j4.filemanager.FileService;
import ua.com.javarush.j4.filemanager.OutputFileName;
import ua.com.javarush.j4.language.LanguageDetector;
import ua.com.javarush.j4.runner.ArgumentParser;
import ua.com.javarush.j4.runner.RunOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ApplicationService {

    public void run(String[] args) {
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