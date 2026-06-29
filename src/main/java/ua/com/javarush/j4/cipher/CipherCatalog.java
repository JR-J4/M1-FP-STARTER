package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.error.InvalidArgumentsException;
import ua.com.javarush.j4.support.NamedRegistry;

/** Registry of named cipher creators. Replaces the old switch-based factory. */
public final class CipherCatalog {

    /** Builds a configured cipher from an alphabet plus optional key/keyword. */
    @FunctionalInterface
    public interface CipherCreator {
        Cipher create(Alphabet alphabet, Integer key, String keyword);
    }

    /** The generic registry supplies all the name → value plumbing; T is CipherCreator here. */
    private final NamedRegistry<CipherCreator> creators = new NamedRegistry<>("cipher");

    public void register(String name, CipherCreator creator) {
        creators.register(name, creator);
    }

    public Cipher create(CipherSpec spec, Alphabet alphabet) {
        return creators.get(spec.cipherName()).create(alphabet, spec.key(), spec.keyword());
    }

    /** The built-in cipher set. */
    public static CipherCatalog withDefaults() {
        CipherCatalog catalog = new CipherCatalog();
        catalog.register("caesar", (alphabet, key, keyword) -> new CaesarCipher(alphabet, requireKey(key)));
        catalog.register("rot13", (alphabet, key, keyword) -> new Rot13Cipher(alphabet));
        catalog.register("atbash", (alphabet, key, keyword) -> new AtbashCipher(alphabet));
        catalog.register("vigenere", (alphabet, key, keyword) -> new VigenereCipher(alphabet, keyword));
        catalog.register("asd", (alphabet, key, keyword) -> new AsdCypher(alphabet, keyword));
        return catalog;
    }

    private static int requireKey(Integer key) {
        if (key == null) {
            throw new InvalidArgumentsException("Caesar cipher requires a key (-k <int>)");
        }
        return key;
    }
}
