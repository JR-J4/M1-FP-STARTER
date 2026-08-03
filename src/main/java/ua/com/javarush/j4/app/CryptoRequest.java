package ua.com.javarush.j4.app;

import java.nio.file.Path;

/** A fully-parsed user request, independent of how it was parsed. */
public record CryptoRequest(
        Operation operation,
        Path file,
        Integer key,
        String cipherName,
        String keyword,
        String alphabetName,
        String scorerName) {

    /** The same request pointed at a different file — used to fan a batch out. */
    public CryptoRequest withFile(Path other) {
        return new CryptoRequest(operation, other, key, cipherName, keyword, alphabetName, scorerName);
    }
}
