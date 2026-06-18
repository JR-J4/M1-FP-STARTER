package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Registry of named cipher creators. Replaces the old switch-based factory. */
public final class CipherCatalog {

    /** Builds a configured cipher from an alphabet plus optional key/keyword. */
    @FunctionalInterface
    public interface CipherCreator {
        Cipher create(Alphabet alphabet, Integer key, String keyword);
    }

    private final Map<String, CipherCreator> creators = new HashMap<>();

    public void register(String name, CipherCreator creator) {
        creators.put(name.toLowerCase(Locale.ROOT), creator);
    }

    public Cipher create(CipherSpec spec, Alphabet alphabet) {
        CipherCreator creator = creators.get(spec.cipherName().toLowerCase(Locale.ROOT));
        if (creator == null) {
            throw new InvalidArgumentsException("Unknown cipher: " + spec.cipherName());
        }
        return creator.create(alphabet, spec.key(), spec.keyword());
    }

    /** The built-in cipher set. */
    public static CipherCatalog withDefaults() {
        CipherCatalog catalog = new CipherCatalog();
        catalog.register("caesar", (alphabet, key, keyword) -> new CaesarCipher(alphabet, requireKey(key)));
        catalog.register("rot13", (alphabet, key, keyword) -> new Rot13Cipher(alphabet));
        catalog.register("atbash", (alphabet, key, keyword) -> new AtbashCipher(alphabet));
        catalog.register("vigenere", (alphabet, key, keyword) -> new VigenereCipher(alphabet, keyword));
        return catalog;
    }

    private static int requireKey(Integer key) {
        if (key == null) {
            throw new InvalidArgumentsException("Caesar cipher requires a key (-k <int>)");
        }
        return key;
    }
}
