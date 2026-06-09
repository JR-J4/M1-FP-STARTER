package ua.com.javarush.j4;

import ua.com.javarush.j4.bruteforce.BruteForce;
import ua.com.javarush.j4.crypto.Cypher;
import ua.com.javarush.j4.file.FileManager;
import ua.com.javarush.j4.language.Alphabet;
import ua.com.javarush.j4.language.AlphabetDetector;
import ua.com.javarush.j4.runner.ArgsParser;
import ua.com.javarush.j4.runner.Mode;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        try {
            ArgsParser a = ArgsParser.parse(args);

            if (a.getMode() == null) {
                throw new IllegalArgumentException("Missing command. Please choose mode");
            }
            if (a.getFile() == null) {
                throw new IllegalArgumentException("Missing -f (file path). Please specify file");
            }
            if (a.getMode() != Mode.BRUTE_FORCE && a.getKey() == null) {
                throw new IllegalArgumentException("Missing -k (shift key)");
            }
            if (!Files.exists(a.getFile())) {
                throw new IllegalArgumentException("File not found: " + a.getFile());
            }

            String input = FileManager.read(a.getFile());

            switch (a.getMode()) {
                case ENCRYPT -> {
                    String encrypted = input;
                    for (Alphabet alphabet : AlphabetDetector.ALPHABETS) {
                        encrypted = new Cypher(alphabet).encrypt(encrypted, a.getKey());
                    }
                    Path out = FileManager.buildOutputPath(a.getFile(), "[ENCRYPTED]");
                    FileManager.write(out, encrypted);
                    System.out.println("ENCRYPTED SUCCESSFULLY! → " + out);
                }
                case DECRYPT -> {
                    String decrypted = input;
                    for (Alphabet alphabet : AlphabetDetector.ALPHABETS) {
                        decrypted = new Cypher(alphabet).decrypt(decrypted, a.getKey());
                    }
                    Path out = FileManager.buildOutputPath(a.getFile(), "[DECRYPTED]");
                    FileManager.write(out, decrypted);
                    System.out.println("DECRYPTED SUCCESSFULLY! → " + out);
                }
                case BRUTE_FORCE -> {
                    Alphabet alphabet = AlphabetDetector.detect(input);
                    BruteForce bf = new BruteForce(new Cypher(alphabet));
                    String cracked = bf.crack(input);
                    Path out = FileManager.buildOutputPath(a.getFile(), "[DECRYPTED]");
                    FileManager.write(out, cracked);
                    System.out.println("BRUTE FORCE DONE! → " + out);
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}