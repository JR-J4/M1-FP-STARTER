package ua.com.javarush.j4.app;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.app.command.BruteForceCommand;
import ua.com.javarush.j4.app.command.CryptoCommand;
import ua.com.javarush.j4.app.command.DecryptCommand;
import ua.com.javarush.j4.app.command.EncryptCommand;
import ua.com.javarush.j4.cipher.Cipher;
import ua.com.javarush.j4.cipher.CipherCatalog;
import ua.com.javarush.j4.crack.Language;
import ua.com.javarush.j4.crack.LanguageDetector;
import ua.com.javarush.j4.crack.Languages;
import ua.com.javarush.j4.crack.ScorerCatalog;
import ua.com.javarush.j4.error.InvalidArgumentsException;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.io.IOException;
import java.nio.file.Path;

/** Facade: turns a CryptoRequest into the right command and runs it. */
public final class CryptoService {
    private final CipherCatalog ciphers;
    private final ScorerCatalog scorers;
    private final LanguageDetector detector;
    private final TextReaders readers;
    private final TextWriter writer;
    private final OutputNaming naming;

    public CryptoService(CipherCatalog ciphers, ScorerCatalog scorers, LanguageDetector detector,
                         TextReaders readers, TextWriter writer, OutputNaming naming) {
        this.ciphers = ciphers;
        this.scorers = scorers;
        this.detector = detector;
        this.readers = readers;
        this.writer = writer;
        this.naming = naming;
    }

    public Path execute(CryptoRequest request) throws IOException {
        return command(request).execute();
    }

    private CryptoCommand command(CryptoRequest request) {
        Path file = request.file();
        return switch (request.operation()) {
            case ENCRYPT -> new EncryptCommand(file, cipher(request), readers, writer, naming);
            case DECRYPT -> new DecryptCommand(file, cipher(request), readers, writer, naming);
            case BRUTE_FORCE -> bruteForce(request, file);
        };
    }

    private Cipher cipher(CryptoRequest request) {
        Alphabet alphabet = Languages.byCode(request.languageCode())
                .map(Language::alphabet)
                .orElse(Alphabets.DEFAULT);
        return ciphers.create(request.cipherSpec(), alphabet);
    }

    private CryptoCommand bruteForce(CryptoRequest request, Path file) {
        if (!"caesar".equalsIgnoreCase(request.cipherSpec().cipherName())) {
            throw new InvalidArgumentsException("Brute-force is supported only for the caesar cipher");
        }
        return new BruteForceCommand(
                file, detector,
                Languages.byCode(request.languageCode()).orElse(null),
                scorers, request.scorerName(), readers, writer, naming);
    }
}
