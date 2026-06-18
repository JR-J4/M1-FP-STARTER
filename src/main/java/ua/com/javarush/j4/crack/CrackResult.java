package ua.com.javarush.j4.crack;

/** The outcome of a brute-force attack: the recovered key and plaintext. */
public record CrackResult(int key, String plaintext) {
}
