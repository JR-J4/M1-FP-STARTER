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

    private String shiftAll(String text, int by) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            out.append(alphabet.shift(text.charAt(i), by));
        }
        return out.toString();
    }
}
