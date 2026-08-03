package ua.com.javarush.j4.app.command;

import ua.com.javarush.j4.concurrent.ParallelPolicy;
import ua.com.javarush.j4.concurrent.TaskExecutor;
import ua.com.javarush.j4.crack.Cracker;
import ua.com.javarush.j4.crack.DictionaryScorer;
import ua.com.javarush.j4.crack.FitnessScorer;
import ua.com.javarush.j4.crack.FrequencyScorer;
import ua.com.javarush.j4.crack.LanguageDetector;
import ua.com.javarush.j4.crack.LanguageProfile;
import ua.com.javarush.j4.crack.ParallelCaesarCracker;
import ua.com.javarush.j4.error.InvalidArgumentsException;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.nio.file.Path;

/** Brute-force: detect language (unless one is forced), sweep keys, write best decryption. */
public final class BruteForceCommand extends CryptoCommand {
    private final LanguageDetector detector;
    private final LanguageProfile forcedProfile; // null => auto-detect
    private final String scorerName;
    private final TaskExecutor executor;
    private final ParallelPolicy policy;

    public BruteForceCommand(Path input, LanguageDetector detector, LanguageProfile forcedProfile,
                             String scorerName, TextReaders readers, TextWriter writer,
                             OutputNaming naming, TaskExecutor executor, ParallelPolicy policy) {
        super(input, readers, writer, naming);
        this.detector = detector;
        this.forcedProfile = forcedProfile;
        this.scorerName = scorerName;
        this.executor = executor;
        this.policy = policy;
    }

    @Override
    protected String transform(String text) {
        /*
         * Language detection runs on the ciphertext, not plaintext. This is reliable
         * for distinguishing scripts (Latin vs Cyrillic) because a Caesar shift stays
         * within an alphabet ring and leaves the script unchanged. For two same-script
         * languages (e.g. Ukrainian vs Russian), detection relies on distinctive-letter
         * frequencies surviving the shift and has been validated for the shipped fixtures
         * (Hamlet EN, Orwell UA). For ambiguous real-world input, prefer passing an
         * explicit --alphabet flag to force a profile rather than relying on auto-detect.
         */
        LanguageProfile profile = forcedProfile != null ? forcedProfile : detector.detect(text);
        FitnessScorer scorer = buildScorer(scorerName, profile);
        Cracker cracker = new ParallelCaesarCracker(profile.alphabet(), scorer, executor, policy);
        return cracker.crack(text).plaintext();
    }

    private static FitnessScorer buildScorer(String name, LanguageProfile profile) {
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "dictionary" -> new DictionaryScorer(profile);
            case "frequency"  -> new FrequencyScorer(profile);
            default -> throw new InvalidArgumentsException(
                    "Unknown scorer '" + name + "'. Valid values: dictionary, frequency");
        };
    }

    @Override
    protected Path outputPath(OutputNaming naming, Path input) {
        return naming.forDecrypt(input);
    }
}
