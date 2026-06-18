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
}
