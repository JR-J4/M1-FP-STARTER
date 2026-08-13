package ua.com.javarush.j4.app;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.app.command.BruteForceCommand;
import ua.com.javarush.j4.app.command.CryptoCommand;
import ua.com.javarush.j4.app.command.DecryptCommand;
import ua.com.javarush.j4.app.batch.BatchProcessor;
import ua.com.javarush.j4.app.batch.BatchReport;
import ua.com.javarush.j4.app.command.EncryptCommand;
import ua.com.javarush.j4.cipher.Cipher;
import ua.com.javarush.j4.cipher.CipherFactory;
import ua.com.javarush.j4.cipher.ParallelCipher;
import ua.com.javarush.j4.concurrent.TaskExecutor;
import ua.com.javarush.j4.concurrent.TaskExecutors;
import ua.com.javarush.j4.crack.LanguageDetector;
import ua.com.javarush.j4.crack.LanguageProfile;
import ua.com.javarush.j4.crack.LanguageProfiles;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Facade: turns a CryptoRequest into the right command and runs it. */
// ── SOLID ▸ D — Принцип інверсії залежностей (DIP): «корінь композиції» ──
// Саме тут абстракції «зшиваються» з конкретними реалізаціями: сервіс створює
// CipherFactory, TextReaders, TextWriter, LanguageDetector і ВПРОВАДЖУЄ їх у
// команди через конструктори. Завдяки цьому команди й Cracker залишаються
// залежними лише від інтерфейсів, а всі рішення «що з чим з'єднати» зібрані в
// одному місці. Клас також є Фасадом (SRP: єдиний обов'язок — оркеструвати запит).
//
// Потоки теж живуть лише тут: команди отримують готовий Cipher і не знають, чи
// виконується він послідовно, чи розбивається на шматки по пулу потоків.
public final class CryptoService implements AutoCloseable {
    private final TextReaders readers = new TextReaders();
    private final TextWriter writer = new TextWriter();
    private final OutputNaming naming = new OutputNaming();
    private final CipherFactory ciphers = new CipherFactory();
    private final LanguageDetector detector = new LanguageDetector();
    private final TaskExecutor executor;

    /** Uses every available core. */
    public CryptoService() {
        this(0);
    }

    /** {@code threads <= 0} means every available core; {@code 1} means fully sequential. */
    public CryptoService(int threads) {
        this.executor = TaskExecutors.of(threads);
    }

    public Path execute(CryptoRequest request) throws IOException {
        return command(request, executor).execute();
    }

    /**
     * Runs the same request over several files. Fan-out happens here and nowhere else:
     * each file's command gets a same-thread executor, so chunking inside it stays
     * sequential and the pool is never oversubscribed.
     */
    public BatchReport executeAll(CryptoRequest template, List<Path> files) {
        return new BatchProcessor(executor).process(files,
                file -> command(template.withFile(file), TaskExecutors.sequential()));
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
                    request.scorerName(), readers, writer, naming);
        };
    }

    /** The chunking decorator only goes on when there is more than one thread to chunk across. */
    private Cipher cipher(CryptoRequest request, TaskExecutor taskExecutor) {
        Alphabet alphabet = Alphabets.byName(request.alphabetName());
        Cipher cipher = ciphers.create(request.cipherName(), alphabet, request.key(), request.keyword());
        return taskExecutor.parallelism() > 1 ? new ParallelCipher(cipher, taskExecutor) : cipher;
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
