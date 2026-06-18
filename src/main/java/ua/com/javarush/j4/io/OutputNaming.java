package ua.com.javarush.j4.io;

import java.nio.file.Path;

/** Computes the output path: inserts/replaces the [ENCRYPTED]/[DECRYPTED] marker. */
public final class OutputNaming {
    private static final String ENCRYPTED = "[ENCRYPTED]";
    private static final String DECRYPTED = "[DECRYPTED]";

    public Path forEncrypt(Path input) {
        return sibling(input, insertMarker(input.getFileName().toString(), ENCRYPTED));
    }

    public Path forDecrypt(Path input) {
        String name = input.getFileName().toString();
        String renamed = name.contains(ENCRYPTED)
                ? name.replace(ENCRYPTED, DECRYPTED)
                : insertMarker(name, DECRYPTED);
        return sibling(input, renamed);
    }

    private static String insertMarker(String name, String marker) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return name + " " + marker;
        }
        return name.substring(0, dot) + " " + marker + name.substring(dot);
    }

    private static Path sibling(Path input, String newName) {
        Path parent = input.getParent();
        return parent == null ? Path.of(newName) : parent.resolve(newName);
    }
}
