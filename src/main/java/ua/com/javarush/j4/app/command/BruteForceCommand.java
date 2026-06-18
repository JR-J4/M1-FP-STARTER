package ua.com.javarush.j4.app.command;

import ua.com.javarush.j4.crack.CaesarCracker;
import ua.com.javarush.j4.crack.DictionaryScorer;
import ua.com.javarush.j4.crack.FitnessScorer;
import ua.com.javarush.j4.crack.FrequencyScorer;
import ua.com.javarush.j4.crack.Language;
import ua.com.javarush.j4.crack.LanguageDetector;
import ua.com.javarush.j4.error.InvalidArgumentsException;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.nio.file.Path;

/** Brute-force: detect language (unless one is forced), sweep keys, write best decryption. */
public final class BruteForceCommand extends CryptoCommand {
    private final LanguageDetector detector;
    private final Language forcedProfile; // null => auto-detect
    private final String scorerName;

    public BruteForceCommand(Path input, LanguageDetector detector, Language forcedProfile,
                             String scorerName, TextReaders readers, TextWriter writer, OutputNaming naming) {
        super(input, readers, writer, naming);
        this.detector = detector;
        this.forcedProfile = forcedProfile;
        this.scorerName = scorerName;
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
        Language profile = forcedProfile != null ? forcedProfile : detector.detect(text);
        FitnessScorer scorer = buildScorer(scorerName, profile);
        CaesarCracker cracker = new CaesarCracker(profile.alphabet(), scorer);
        return cracker.crack(text).plaintext();
    }

    private static FitnessScorer buildScorer(String name, Language profile) {
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
