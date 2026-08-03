package ua.com.javarush.j4.cipher;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.alphabet.Alphabets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VigenereOffsetTest {

    private static final Alphabet EN = Alphabets.ENGLISH;
    private static final String TEXT =
            "Attack at dawn, and hold the line until the second company arrives!";

    private static VigenereCipher cipher() {
        return new VigenereCipher(EN, "lemon");
    }

    @Test
    void offsetZeroMatchesPlainEncrypt() {
        assertEquals(cipher().encrypt(TEXT), cipher().encryptFrom(TEXT, 0));
    }

    @Test
    void splittingAtAnyPointReproducesTheWholeEncryption() {
        VigenereCipher cipher = cipher();
        String expected = cipher.encrypt(TEXT);

        for (int split = 0; split <= TEXT.length(); split++) {
            String head = TEXT.substring(0, split);
            String tail = TEXT.substring(split);
            String joined = cipher.encryptFrom(head, 0)
                    + cipher.encryptFrom(tail, cipher.alphabetLetterCount(head));

            assertEquals(expected, joined, "split at " + split);
        }
    }

    @Test
    void splittingAtAnyPointReproducesTheWholeDecryption() {
        VigenereCipher cipher = cipher();
        String ciphertext = cipher.encrypt(TEXT);

        for (int split = 0; split <= ciphertext.length(); split++) {
            String head = ciphertext.substring(0, split);
            String tail = ciphertext.substring(split);
            String joined = cipher.decryptFrom(head, 0)
                    + cipher.decryptFrom(tail, cipher.alphabetLetterCount(head));

            assertEquals(TEXT, joined, "split at " + split);
        }
    }

    @Test
    void countsOnlyAlphabetMembers() {
        assertEquals(0, cipher().alphabetLetterCount(", .!?123"));
        assertEquals(3, cipher().alphabetLetterCount("a, b. c!"));
        assertEquals(0, cipher().alphabetLetterCount(""));
    }
}
