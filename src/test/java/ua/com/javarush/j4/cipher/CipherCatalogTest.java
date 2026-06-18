package ua.com.javarush.j4.cipher;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import static org.junit.jupiter.api.Assertions.*;

class CipherCatalogTest {

    private final CipherCatalog catalog = CipherCatalog.withDefaults();

    private Cipher create(String name, Integer key, String keyword) {
        return catalog.create(new CipherSpec(name, key, keyword), Alphabets.ENGLISH);
    }

    @Test
    void buildsCaesar() {
        assertEquals("BCD", create("caesar", 1, null).encrypt("ABC"));
    }

    @Test
    void caesarWithoutKeyIsRejected() {
        assertThrows(InvalidArgumentsException.class, () -> create("caesar", null, null));
    }

    @Test
    void rot13IsReversible() {
        Cipher c = create("rot13", null, null);
        assertEquals("URYYB", c.encrypt("HELLO"));
        assertEquals("HELLO", c.decrypt("URYYB"));
    }

    @Test
    void atbashIsSelfInverse() {
        Cipher c = create("atbash", null, null);
        assertEquals("ZYX", c.encrypt("ABC"));
        assertEquals("ABC", c.decrypt("ZYX"));
    }

    @Test
    void vigenereMatchesKnownVector() {
        Cipher c = create("vigenere", null, "KEY");
        assertEquals("RIJVS", c.encrypt("HELLO"));
        assertEquals("HELLO", c.decrypt("RIJVS"));
    }

    @Test
    void vigenereWithoutKeywordIsRejected() {
        assertThrows(InvalidArgumentsException.class, () -> create("vigenere", null, null));
    }

    @Test
    void vigenereWithNonAlphabetKeywordIsRejected() {
        assertThrows(InvalidArgumentsException.class, () -> create("vigenere", null, "KE1"));
    }

    @Test
    void unknownCipherIsRejected() {
        assertThrows(InvalidArgumentsException.class, () -> create("enigma", 1, null));
    }
}
