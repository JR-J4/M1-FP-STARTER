package ua.com.javarush.j4;

import java.nio.file.Path;

/** Output file paths according to the project naming convention */
public class FileNaming {

    private static final String ENCRYPTED_MARKER = " [ENCRYPTED]";
    private static final String DECRYPTED_MARKER = " [DECRYPTED]";
    private static final String TXT_EXTENSION = ".txt";

    private FileNaming() {}

    /** Returns the path where encrypted file should be written */
    public static Path encryptedPath(Path input) {
        return sibling(input, baseName(input) + ENCRYPTED_MARKER + TXT_EXTENSION);
    }

    /**
     * Returns the path where decrypted file should be written */
    public static Path decryptedPath(Path input) {
        String base = baseName(input);
        if (base.endsWith(ENCRYPTED_MARKER)) {
            base = base.substring(0, base.length() - ENCRYPTED_MARKER.length());
        }
        return sibling(input, base + DECRYPTED_MARKER + TXT_EXTENSION);
    }

    /** File name without the trailing */
    private static String baseName(Path p) {
        String name = p.getFileName().toString();
        return name.endsWith(TXT_EXTENSION)
                ? name.substring(0, name.length() - TXT_EXTENSION.length())
                : name;
    }

    /** Resolves the path in the same directory as the original file was. */
    private static Path sibling(Path input, String newName) {
        Path parent = input.getParent();
        return (parent != null) ? parent.resolve(newName) : Path.of(newName);
    }
}