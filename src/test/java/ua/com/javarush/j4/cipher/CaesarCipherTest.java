package ua.com.javarush.j4.cipher;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.alphabet.Alphabets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaesarCipherTest {

    @Test
    void encryptsWithinCase() {
        Cipher c = new CaesarCipher(Alphabets.DEFAULT, 1);
        assertEquals("BCD", c.encrypt("ABC"));
        assertEquals("Бб", c.encrypt("Аа"));
    }

    @Test
    void decryptIsInverseOfEncrypt() {
        Cipher c = new CaesarCipher(Alphabets.DEFAULT, 5);
        String text = "Hello, World! Привіт!";
        assertEquals(text, c.decrypt(c.encrypt(text)));
    }

    @Test
    void negativeKeyWrapsWithinCase() {
        Cipher c = new CaesarCipher(Alphabets.DEFAULT, -1);
        assertEquals("Z", c.encrypt("A"));
        assertEquals("z", c.encrypt("a"));
    }

    @Test
    void nonLettersAndFullCyclesPassThrough() {
        assertEquals("0123456789", new CaesarCipher(Alphabets.DEFAULT, 5).encrypt("0123456789"));
        assertEquals("Hello", new CaesarCipher(Alphabets.DEFAULT, 26).encrypt("Hello"));
        assertEquals(".,!? \t", new CaesarCipher(Alphabets.DEFAULT, 5).encrypt(".,!? \t"));
    }

    @Test
    void multilinePreservesNewlines() {
        assertEquals("bcd\nefg\n", new CaesarCipher(Alphabets.DEFAULT, 1).encrypt("abc\ndef\n"));
    }
}
