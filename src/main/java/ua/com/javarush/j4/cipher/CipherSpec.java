package ua.com.javarush.j4.cipher;

/** The user-facing cipher selection: which cipher, plus its key/keyword (alphabet resolved separately). */
public record CipherSpec(String cipherName, Integer key, String keyword) {
}
