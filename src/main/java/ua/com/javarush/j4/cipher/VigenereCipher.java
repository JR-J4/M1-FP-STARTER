package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.error.InvalidArgumentsException;

/** Polyalphabetic cipher: each enciphered letter is shifted by the next keyword letter. */
public final class VigenereCipher implements Cipher, PositionDependentCipher {
    private final Alphabet alphabet;
    private final String keyword;

    public VigenereCipher(Alphabet alphabet, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new InvalidArgumentsException("Vigenère cipher requires a non-empty --keyword");
        }
        this.alphabet = alphabet;
        this.keyword = keyword;
    }

    @Override
    public String encrypt(String text) {
        return process(text, 1, 0);
    }

    @Override
    public String decrypt(String text) {
        return process(text, -1, 0);
    }

    @Override
    public String encryptFrom(String chunk, int letterOffset) {
        return process(chunk, 1, letterOffset);
    }

    @Override
    public String decryptFrom(String chunk, int letterOffset) {
        return process(chunk, -1, letterOffset);
    }

    @Override
    public int alphabetLetterCount(String chunk) {
        int count = 0;
        for (int i = 0; i < chunk.length(); i++) {
            if (alphabet.contains(chunk.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    // indexOf rather than position(): this runs twice per character, and OptionalInt would
    // put an allocation on the hottest path in the cipher.
    private String process(String text, int sign, int startLetterIndex) {
        char[] out = text.toCharArray();
        int keyIndex = startLetterIndex;
        for (int i = 0; i < out.length; i++) {
            char c = out[i];
            if (alphabet.contains(c)) {
                char keyChar = keyword.charAt(keyIndex % keyword.length());
                int shift = Math.max(alphabet.indexOf(keyChar), 0) * sign;
                out[i] = alphabet.shift(c, shift);
                keyIndex++;
            }
        }
        return new String(out);
    }
}
