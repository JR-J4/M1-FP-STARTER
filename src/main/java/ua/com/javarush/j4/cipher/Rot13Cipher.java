package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;

/** ROT13: a Caesar cipher with a fixed shift of 13. */
public final class Rot13Cipher implements Cipher {
    private final CaesarCipher delegate;

    public Rot13Cipher(Alphabet alphabet) {
        this.delegate = new CaesarCipher(alphabet, 13);
    }

    @Override
    public String encrypt(String text) {
        return delegate.encrypt(text);
    }

    @Override
    public String decrypt(String text) {
        return delegate.decrypt(text);
    }
}
