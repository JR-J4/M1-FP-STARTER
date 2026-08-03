package ua.com.javarush.j4.cipher;

/**
 * A cipher whose result for a chunk of text depends on how many alphabet letters
 * precede that chunk.
 *
 * <p>Implemented only by ciphers that advance a key per letter — {@link VigenereCipher}
 * is the only one here. {@link ParallelCipher} uses it to transform chunks out of order
 * without changing the output.
 *
 * <p>Deliberately narrow, and deliberately separate from {@link Cipher}: ciphers that do
 * not need it are not forced to implement it. Compare with the widened {@code TextReader}
 * in {@code docs/SOLID.md}, which is kept as the counter-example.
 */
public interface PositionDependentCipher {

    /** How many characters of {@code chunk} advance the key. */
    int alphabetLetterCount(String chunk);

    /** Encrypts {@code chunk} as if {@code letterOffset} alphabet letters came before it. */
    String encryptFrom(String chunk, int letterOffset);

    /** Decrypts {@code chunk} as if {@code letterOffset} alphabet letters came before it. */
    String decryptFrom(String chunk, int letterOffset);
}
