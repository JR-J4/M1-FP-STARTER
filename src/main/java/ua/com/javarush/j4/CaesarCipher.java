package ua.com.javarush.j4;

/**
 * Шифр Цезаря для англійського алфавіту (26 літер, регістр зберігається:
 * велика лишається великою, мала — малою). Цифри, пробіли, розділові знаки і
 * переходи рядка проходять без змін.
 */
public class CaesarCipher {

    private static final int ALPHABET_SIZE = 26;

    public String encrypt(String text, int key) {
        return shift(text, key);
    }

    /** Розшифрування з ключем {@code k} — це шифрування з ключем {@code -k}. */
    public String decrypt(String text, int key) {
        return shift(text, -key);
    }

    private String shift(String text, int key) {
        int normalizedKey = ((key % ALPHABET_SIZE) + ALPHABET_SIZE) % ALPHABET_SIZE;
        StringBuilder result = new StringBuilder(text.length());
        for (char symbol : text.toCharArray()) {
            result.append(shiftSymbol(symbol, normalizedKey));
        }
        return result.toString();
    }

    private char shiftSymbol(char symbol, int normalizedKey) {
        if (symbol >= 'A' && symbol <= 'Z') {
            return (char) ('A' + (symbol - 'A' + normalizedKey) % ALPHABET_SIZE);
        }
        if (symbol >= 'a' && symbol <= 'z') {
            return (char) ('a' + (symbol - 'a' + normalizedKey) % ALPHABET_SIZE);
        }
        return symbol;
    }
}
