package ua.com.javarush.j4.menu;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.args.RunOptions;
import ua.com.javarush.j4.cipher.BruteForce;
import ua.com.javarush.j4.cipher.Cipher;
import ua.com.javarush.j4.file.FileService;

import java.nio.file.Path;
import java.util.ArrayList;

public class MainMenu {
    public static void manu(RunOptions options) {

        try {
            String text = FileService.read(options.path());
            ArrayList<Character> languishes = Alphabet.getLanguage(text);
            Cipher cipher = new Cipher();
            BruteForce bruteForce = new BruteForce();
            switch (options.command()) {
                case ENCRYPT -> {
                    Path newPath = generateNewPath(options.path(), "[ENCRYPTED]");
                    String encrypted = cipher.encrypt(text, options.key(), languishes);
                    FileService.write(newPath, encrypted);
                }
                case DECRYPT -> {
                    Path newPath = generateNewPath(options.path(), "[DECRYPTED]");
                    String decrypted = cipher.decrypt(text, options.key(), languishes);
                    FileService.write(newPath, decrypted);

                }
                case BRUTE_FORCE -> {
                    Path newPath = generateNewPath(options.path(), "[BRUTE_FORCE]");
                    String bf = cipher.decrypt(text, bruteForce.findKey(text), languishes);
                    FileService.write(newPath, bf);

                }
            }
        } catch (Exception e) {
            System.out.println("Помилка: " + e.getMessage());
        }
    }


    private static Path generateNewPath(Path originalPath, String suffix) {
        String name = originalPath.getFileName().toString();
        if (name.contains("[ENCRYPTED]") && suffix.equals("[DECRYPTED]")) {
            String newName = name.replace("[ENCRYPTED]", "[DECRYPTED]");
            return originalPath.resolveSibling(newName);
        }
        if (name.contains("[ENCRYPTED]") && suffix.equals("[BRUTE_FORCE]")) {
            String newName = name.replace("[ENCRYPTED]", "[BRUTE_FORCE]");
            return originalPath.resolveSibling(newName);
        }
        int dotIndex = name.lastIndexOf(".");
        String newName;
        if (dotIndex != -1) {
            newName = name.substring(0, dotIndex) + suffix + name.substring(dotIndex);
        } else {
            newName = name + suffix;
        }
        return originalPath.resolveSibling(newName);
    }
}
