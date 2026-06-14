package ua.com.javarush.j4.fileManager;

import ua.com.javarush.j4.runner.Command;

import java.nio.file.Path;

public class OutputFileName {
    private OutputFileName() {
    }

    public static Path build(Path inputFile, Command command) {
        String fileName = inputFile.getFileName().toString();

        String outputFileName;

        switch (command) {
            case ENCRYPT -> outputFileName = fileName.replace(".txt", " [ENCRYPTED].txt");
            case DECRYPT, BRUTE_FORCE -> {
                if (fileName.contains(" [ENCRYPTED]")) {
                    outputFileName = fileName.replace(" [ENCRYPTED]", " [DECRYPTED]");
                } else {
                    outputFileName = fileName.replace(".txt", " [DECRYPTED]");
                }
            }
            default -> throw new IllegalArgumentException("Unknown command");
        }
        return inputFile.resolveSibling(outputFileName);
    }
}