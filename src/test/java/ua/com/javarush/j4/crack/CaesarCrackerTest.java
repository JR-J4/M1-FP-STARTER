package ua.com.javarush.j4.crack;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.cipher.CaesarCipher;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaesarCrackerTest {

    @Test
    void recoversEnglishPlaintextExactly() {
        String original = "The quick brown fox jumps over the lazy dog. "
                + "And the dog was not amused, for that is what dogs do.";
        String ciphertext = new CaesarCipher(Languages.ENGLISH.alphabet(), 7).encrypt(original);

        CrackResult result = new CaesarCracker(
                Languages.ENGLISH.alphabet(),
                new DictionaryScorer(Languages.ENGLISH)).crack(ciphertext);

        assertEquals(7, result.key());
        assertEquals(original, result.plaintext());
    }

    @Test
    void recoversUkrainianPlaintextExactly() {
        String original = "Він був високий і худий, а на обличчі його застигла "
                + "усмішка. Це не та людина, що боїться зими.";
        String ciphertext = new CaesarCipher(Languages.UKRAINIAN.alphabet(), 12).encrypt(original);

        CrackResult result = new CaesarCracker(
                Languages.UKRAINIAN.alphabet(),
                new DictionaryScorer(Languages.UKRAINIAN)).crack(ciphertext);

        assertEquals(12, result.key());
        assertEquals(original, result.plaintext());
    }
}
