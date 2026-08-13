package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;

/** Shifts every character by a fixed key within its own alphabet ring. */
public final class CaesarCipher implements Cipher {
    private final Alphabet alphabet;
    private final int key;

    public CaesarCipher(Alphabet alphabet, int key) {
        this.alphabet = alphabet;
        this.key = key;
    }

    @Override
    public String encrypt(String text) {
        return shiftAll(text, key);
    }

    @Override
    public String decrypt(String text) {
        return shiftAll(text, -key);
    }

    // A char[] rather than a StringBuilder: the output is exactly as long as the input, so
    // there is nothing to grow, and a builder would additionally re-encode the whole buffer
    // the first time a non-Latin-1 character arrived.
    private String shiftAll(String text, int by) {
        char[] out = text.toCharArray();
        for (int i = 0; i < out.length; i++) {
            out[i] = alphabet.shift(out[i], by);
        }
        return new String(out);
    }
}
