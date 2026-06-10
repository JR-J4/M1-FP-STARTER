package ua.com.javarush.j4.engine;

import ua.com.javarush.j4.crypty.BruteForce;
import ua.com.javarush.j4.crypty.Cipher;
import ua.com.javarush.j4.runner.RunOptions;

import java.nio.file.Files;
import java.nio.file.Path;

public class AppRunner {

    private final Cipher cipher = new Cipher();
    private final BruteForce bruteForce = new BruteForce(cipher);

    public void run(RunOptions options) {
        try {
            String content = Files.readString(options.path());
            String resultText;
            Path outputPath;

            switch (options.command()) {
                case ENCRYPT -> {
                    resultText = cipher.encrypt(content, options.key());
                    outputPath = buildOutputPath(options.path(), "[ENCRYPTED]", false);
                }
                case DECRYPT -> {
                    resultText = cipher.decrypt(content, options.key());
                    outputPath = buildOutputPath(options.path(), "[DECRYPTED]", true);
                }
                case BRUTEFORCE -> {
                    int foundKey = bruteForce.decryptByBruteForce(content);
                    System.out.println("[BruteForce]: Автоматично підібрано ключ: " + foundKey);

                    resultText = cipher.decrypt(content, foundKey);
                    outputPath = buildOutputPath(options.path(), "[DECRYPTED]", true);
                }
                default -> throw new IllegalArgumentException("Невідома команда: " + options.command());
            }

            Files.writeString(outputPath, resultText);
            System.out.println("Результат успішно записано у: " + outputPath.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Помилка при роботі з файлами: " + e.getMessage(), e);
        }
    }

    private Path buildOutputPath(Path originalPath, String newLabel, boolean replaceEncryptedLabel) {

        String fileName = originalPath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = (dotIndex != -1) ? fileName.substring(0, dotIndex) : fileName;
        String extension = (dotIndex != -1) ? fileName.substring(dotIndex) : "";

        if (replaceEncryptedLabel && baseName.contains("[ENCRYPTED]")) {
            if (baseName.contains(" [ENCRYPTED]")) {
                baseName = baseName.replace(" [ENCRYPTED]", "");
            } else {
                baseName = baseName.replace("[ENCRYPTED]", "");
            }
        }

        String newFileName = baseName + " " + newLabel + extension;
        return originalPath.resolveSibling(newFileName);
    }
}