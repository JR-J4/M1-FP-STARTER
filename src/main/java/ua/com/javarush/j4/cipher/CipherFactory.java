package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import java.util.Locale;

/** Builds a configured {@link Cipher} from a cipher name plus key/keyword. */
public final class CipherFactory {

    public Cipher create(String name, Alphabet alphabet, Integer key, String keyword) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "caesar" -> new CaesarCipher(alphabet, requireKey(key));
            case "rot13" -> new Rot13Cipher(alphabet);
            case "atbash" -> new AtbashCipher(alphabet);
            case "vigenere" -> new VigenereCipher(alphabet, keyword);
            default -> throw new InvalidArgumentsException("Unknown cipher: " + name);
        };
    }

    private int requireKey(Integer key) {
        if (key == null) {
            throw new InvalidArgumentsException("Caesar cipher requires a key (-k <int>)");
        }
        return key;
    }
}
