package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.error.InvalidArgumentsException;

/** Polyalphabetic cipher: each enciphered letter is shifted by the next keyword letter. */
public final class VigenereCipher implements Cipher {
    private final Alphabet alphabet;
    private final String keyword;

    public VigenereCipher(Alphabet alphabet, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new InvalidArgumentsException("Vigenère cipher requires a non-empty --keyword");
        }
        for (int i = 0; i < keyword.length(); i++) {
            if (alphabet.position(keyword.charAt(i)).isEmpty()) {
                throw new InvalidArgumentsException(
                        "Vigenère keyword must contain only alphabet letters: '" + keyword + "'");
            }
        }
        this.alphabet = alphabet;
        this.keyword = keyword;
    }

    @Override
    public String encrypt(String text) {
        return process(text, 1);
    }

    @Override
    public String decrypt(String text) {
        return process(text, -1);
    }

    private String process(String text, int sign) {
        StringBuilder out = new StringBuilder(text.length());
        int keyIndex = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (alphabet.position(c).isPresent()) {
                char keyChar = keyword.charAt(keyIndex % keyword.length());
                int shift = alphabet.position(keyChar).orElse(0) * sign;
                out.append(alphabet.shift(c, shift));
                keyIndex++;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
