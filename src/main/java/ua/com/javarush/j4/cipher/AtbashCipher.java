package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;

/** Atbash: mirror each letter within its ring. Self-inverse. */
public final class AtbashCipher implements Cipher {
    private final Alphabet alphabet;

    public AtbashCipher(Alphabet alphabet) {
        this.alphabet = alphabet;
    }

    @Override
    public String encrypt(String text) {
        return mirror(text);
    }

    @Override
    public String decrypt(String text) {
        return mirror(text);
    }

    private String mirror(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            out.append(alphabet.mirror(text.charAt(i)));
        }
        return out.toString();
    }
}
