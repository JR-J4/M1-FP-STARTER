package ua.com.javarush.j4;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    static void run(String[] args) throws Exception {
        CliArgs cli = CliArgs.parse(args);

        Path inputPath = cli.getFilePath();
        if (!Files.exists(inputPath)) {
            throw new IllegalArgumentException(
                    "Input file not found: " + inputPath);
        }

        String text = Files.readString(inputPath, StandardCharsets.UTF_8);
        Language lang = Language.detect(text);
        CaesarCipher cipher = new CaesarCipher(lang);

        switch (cli.getCommand()) {
            case ENCRYPT -> Files.writeString(
                    FileNaming.encryptedPath(inputPath),
                    cipher.encrypt(text, cli.getKey()),
                    StandardCharsets.UTF_8);
            case DECRYPT -> Files.writeString(
                    FileNaming.decryptedPath(inputPath),
                    cipher.decrypt(text, cli.getKey()),
                    StandardCharsets.UTF_8);
            case BRUTE_FORCE -> Files.writeString(
                    FileNaming.decryptedPath(inputPath),
                    cipher.bruteForce(text),
                    StandardCharsets.UTF_8);
        }
    }
}