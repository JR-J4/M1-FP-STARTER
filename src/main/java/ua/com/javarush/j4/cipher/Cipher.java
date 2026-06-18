package ua.com.javarush.j4.cipher;

/** A configured text transform. Key/keyword are bound at construction, so this is a pure String → String. */
public interface Cipher {
    String encrypt(String text);

    String decrypt(String text);
}
