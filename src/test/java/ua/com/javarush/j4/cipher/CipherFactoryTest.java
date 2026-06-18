package ua.com.javarush.j4.cipher;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import static org.junit.jupiter.api.Assertions.*;

class CipherFactoryTest {

    private final CipherFactory factory = new CipherFactory();

    @Test
    void buildsCaesar() {
        Cipher c = factory.create("caesar", Alphabets.DEFAULT, 1, null);
        assertEquals("BCD", c.encrypt("ABC"));
    }

    @Test
    void caesarWithoutKeyIsRejected() {
        assertThrows(InvalidArgumentsException.class,
                () -> factory.create("caesar", Alphabets.DEFAULT, null, null));
    }

    @Test
    void rot13IsReversible() {
        Cipher c = factory.create("rot13", Alphabets.ENGLISH, null, null);
        assertEquals("URYYB", c.encrypt("HELLO"));
        assertEquals("HELLO", c.decrypt("URYYB"));
    }

    @Test
    void atbashIsSelfInverse() {
        Cipher c = factory.create("atbash", Alphabets.ENGLISH, null, null);
        assertEquals("ZYX", c.encrypt("ABC"));
        assertEquals("ABC", c.decrypt("ZYX"));
    }

    @Test
    void vigenereMatchesKnownVector() {
        Cipher c = factory.create("vigenere", Alphabets.ENGLISH, null, "KEY");
        assertEquals("RIJVS", c.encrypt("HELLO"));
        assertEquals("HELLO", c.decrypt("RIJVS"));
    }

    @Test
    void vigenereWithoutKeywordIsRejected() {
        assertThrows(InvalidArgumentsException.class,
                () -> factory.create("vigenere", Alphabets.ENGLISH, null, null));
    }

    @Test
    void unknownCipherIsRejected() {
        assertThrows(InvalidArgumentsException.class,
                () -> factory.create("enigma", Alphabets.ENGLISH, 1, null));
    }
}
