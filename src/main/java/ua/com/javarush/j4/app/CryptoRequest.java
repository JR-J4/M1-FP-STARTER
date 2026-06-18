package ua.com.javarush.j4.app;

import ua.com.javarush.j4.cipher.CipherSpec;

import java.nio.file.Path;

/** A fully-parsed user request, independent of how it was parsed. */
public record CryptoRequest(
        Operation operation,
        Path file,
        CipherSpec cipherSpec,
        String languageCode,
        String scorerName) {
}
