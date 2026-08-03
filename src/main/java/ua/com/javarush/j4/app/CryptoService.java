package ua.com.javarush.j4.app;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.app.command.BruteForceCommand;
import ua.com.javarush.j4.app.command.CryptoCommand;
import ua.com.javarush.j4.app.command.DecryptCommand;
import ua.com.javarush.j4.app.command.EncryptCommand;
import ua.com.javarush.j4.cipher.Cipher;
import ua.com.javarush.j4.cipher.CipherFactory;
import ua.com.javarush.j4.cipher.ParallelCipher;
import ua.com.javarush.j4.concurrent.DirectTaskExecutor;
import ua.com.javarush.j4.concurrent.LazyPooledTaskExecutor;
import ua.com.javarush.j4.concurrent.ParallelPolicy;
import ua.com.javarush.j4.concurrent.TaskExecutor;
import ua.com.javarush.j4.crack.LanguageDetector;
import ua.com.javarush.j4.crack.LanguageProfile;
import ua.com.javarush.j4.crack.LanguageProfiles;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/** Facade: turns a CryptoRequest into the right command and runs it. */
// ── SOLID ▸ D — Принцип інверсії залежностей (DIP): «корінь композиції» ──
// Саме тут абстракції «зшиваються» з конкретними реалізаціями: сервіс створює
// CipherFactory, TextReaders, TextWriter, LanguageDetector і ВПРОВАДЖУЄ їх у
// команди через конструктори. Завдяки цьому команди й Cracker залишаються
// залежними лише від інтерфейсів, а всі рішення «що з чим з'єднати» зібрані в
// одному місці. Клас також є Фасадом (SRP: єдиний обов'язок — оркеструвати запит).
//
// TaskExecutor впроваджується так само, як і решта залежностей: команди не знають,
// виконуються вони послідовно чи на пулі потоків.
public final class CryptoService implements AutoCloseable {
    private final TextReaders readers = new TextReaders();
    private final TextWriter writer = new TextWriter();
    private final OutputNaming naming = new OutputNaming();
    private final CipherFactory ciphers = new CipherFactory();
    private final LanguageDetector detector = new LanguageDetector();
    private final ParallelPolicy policy;
    private final TaskExecutor executor;

    public CryptoService() {
        this(ParallelPolicy.of(0));
    }

    public CryptoService(ParallelPolicy policy) {
        this.policy = policy;
        this.executor = policy.threads() > 1
                ? new LazyPooledTaskExecutor(policy.threads())
                : new DirectTaskExecutor();
    }

    public Path execute(CryptoRequest request) throws IOException {
        return command(request, executor).execute();
    }

    @Override
    public void close() {
        executor.close();
    }

    private CryptoCommand command(CryptoRequest request, TaskExecutor taskExecutor) {
        Path file = request.file();
        return switch (request.operation()) {
            case ENCRYPT -> new EncryptCommand(file, cipher(request, taskExecutor), readers, writer, naming);
            case DECRYPT -> new DecryptCommand(file, cipher(request, taskExecutor), readers, writer, naming);
            case BRUTE_FORCE -> new BruteForceCommand(
                    file, detector, forcedProfile(request.alphabetName()),
                    request.scorerName(), readers, writer, naming, taskExecutor, policy);
        };
    }

    private Cipher cipher(CryptoRequest request, TaskExecutor taskExecutor) {
        Alphabet alphabet = Alphabets.byName(request.alphabetName());
        Cipher cipher = ciphers.create(request.cipherName(), alphabet, request.key(), request.keyword());
        return new ParallelCipher(cipher, taskExecutor, policy);
    }

    /** For brute force: a named language forces its profile; "default"/"auto" means auto-detect. */
    private LanguageProfile forcedProfile(String alphabetName) {
        return switch (alphabetName.toLowerCase(Locale.ROOT)) {
            case "en", "english" -> LanguageProfiles.ENGLISH;
            case "ua", "ukrainian" -> LanguageProfiles.UKRAINIAN;
            case "ru", "russian" -> LanguageProfiles.RUSSIAN;
            default -> null;
        };
    }
}
