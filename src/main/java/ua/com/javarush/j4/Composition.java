package ua.com.javarush.j4;

import ua.com.javarush.j4.app.CryptoService;
import ua.com.javarush.j4.cipher.CipherCatalog;
import ua.com.javarush.j4.cli.CryptoCli;
import ua.com.javarush.j4.crack.LanguageDetector;
import ua.com.javarush.j4.crack.ScorerCatalog;
import ua.com.javarush.j4.io.FileTextWriter;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;

/**
 * The composition root: the single place that constructs and wires the object
 * graph. Every other class receives its collaborators via the constructor.
 */
public final class Composition {

    public CryptoService service() {
        return new CryptoService(
                CipherCatalog.withDefaults(),
                ScorerCatalog.withDefaults(),
                new LanguageDetector(),
                TextReaders.withDefaults(),
                new FileTextWriter(),
                new OutputNaming());
    }

    public CryptoCli cli() {
        return new CryptoCli(service());
    }
}
