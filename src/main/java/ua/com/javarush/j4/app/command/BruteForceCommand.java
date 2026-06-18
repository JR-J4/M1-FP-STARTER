package ua.com.javarush.j4.app.command;

import ua.com.javarush.j4.crack.CaesarCracker;
import ua.com.javarush.j4.crack.DictionaryScorer;
import ua.com.javarush.j4.crack.LanguageDetector;
import ua.com.javarush.j4.crack.LanguageProfile;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.nio.file.Path;

/** Brute-force: detect language (unless one is forced), sweep keys, write best decryption. */
public final class BruteForceCommand extends CryptoCommand {
    private final LanguageDetector detector;
    private final LanguageProfile forcedProfile; // null => auto-detect

    public BruteForceCommand(Path input, LanguageDetector detector, LanguageProfile forcedProfile,
                             TextReaders readers, TextWriter writer, OutputNaming naming) {
        super(input, readers, writer, naming);
        this.detector = detector;
        this.forcedProfile = forcedProfile;
    }

    @Override
    protected String transform(String text) {
        LanguageProfile profile = forcedProfile != null ? forcedProfile : detector.detect(text);
        CaesarCracker cracker = new CaesarCracker(profile.alphabet(), new DictionaryScorer(profile));
        return cracker.crack(text).plaintext();
    }

    @Override
    protected Path outputPath(OutputNaming naming, Path input) {
        return naming.forDecrypt(input);
    }
}
