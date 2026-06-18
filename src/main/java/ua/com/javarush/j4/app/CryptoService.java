package ua.com.javarush.j4.app;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.app.command.BruteForceCommand;
import ua.com.javarush.j4.app.command.CryptoCommand;
import ua.com.javarush.j4.app.command.DecryptCommand;
import ua.com.javarush.j4.app.command.EncryptCommand;
import ua.com.javarush.j4.cipher.Cipher;
import ua.com.javarush.j4.cipher.CipherFactory;
import ua.com.javarush.j4.crack.Language;
import ua.com.javarush.j4.crack.LanguageDetector;
import ua.com.javarush.j4.crack.Languages;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/** Facade: turns a CryptoRequest into the right command and runs it. */
public final class CryptoService {
    private final TextReaders readers = new TextReaders();
    private final TextWriter writer = new TextWriter();
    private final OutputNaming naming = new OutputNaming();
    private final CipherFactory ciphers = new CipherFactory();
    private final LanguageDetector detector = new LanguageDetector();

    public Path execute(CryptoRequest request) throws IOException {
        return command(request).execute();
    }

    private CryptoCommand command(CryptoRequest request) {
        Path file = request.file();
        return switch (request.operation()) {
            case ENCRYPT -> new EncryptCommand(file, cipher(request), readers, writer, naming);
            case DECRYPT -> new DecryptCommand(file, cipher(request), readers, writer, naming);
            case BRUTE_FORCE -> new BruteForceCommand(
                    file, detector, forcedProfile(request.alphabetName()),
                    request.scorerName(), readers, writer, naming);
        };
    }

    private Cipher cipher(CryptoRequest request) {
        Alphabet alphabet = Alphabets.byName(request.alphabetName());
        return ciphers.create(request.cipherName(), alphabet, request.key(), request.keyword());
    }

    /** For brute force: a named language forces its profile; "default"/"auto" means auto-detect. */
    private Language forcedProfile(String alphabetName) {
        return switch (alphabetName.toLowerCase(Locale.ROOT)) {
            case "en", "english" -> Languages.ENGLISH;
            case "ua", "ukrainian" -> Languages.UKRAINIAN;
            case "ru", "russian" -> Languages.RUSSIAN;
            default -> null;
        };
    }
}
