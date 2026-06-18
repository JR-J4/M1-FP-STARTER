package ua.com.javarush.j4;

import java.nio.file.Path;

/**
 * Формує ім'я вихідного файлу поруч із вхідним. Шифрування додає мітку
 * {@code [ENCRYPTED]} перед розширенням; розшифрування додає {@code [DECRYPTED]}
 * або ЗАМІНЮЄ нею наявну мітку {@code [ENCRYPTED]} (а не додає поряд).
 */
public class OutputFile {

    private static final String ENCRYPTED = "[ENCRYPTED]";
    private static final String DECRYPTED = "[DECRYPTED]";

    public static Path forEncrypt(Path input) {
        String name = input.getFileName().toString();
        return sibling(input, addMarker(name, ENCRYPTED));
    }

    public static Path forDecrypt(Path input) {
        String name = input.getFileName().toString();
        if (name.contains(ENCRYPTED)) {
            return sibling(input, name.replace(ENCRYPTED, DECRYPTED));
        }
        return sibling(input, addMarker(name, DECRYPTED));
    }

    private static String addMarker(String name, String marker) {
        int dot = name.lastIndexOf('.');
        if (dot == -1) {
            return name + " " + marker;
        }
        return name.substring(0, dot) + " " + marker + name.substring(dot);
    }

    private static Path sibling(Path input, String newName) {
        Path parent = input.getParent();
        return parent == null ? Path.of(newName) : parent.resolve(newName);
    }
}
