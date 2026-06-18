package ua.com.javarush.j4.crack;

/** Recovers plaintext from ciphertext without a key. */
public interface Cracker {
    CrackResult crack(String ciphertext);
}
