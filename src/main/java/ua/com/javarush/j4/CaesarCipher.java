package ua.com.javarush.j4;

public class CaesarCipher {

    private final Language language;
    private final char[] alphabet;
    private final int size;

    public CaesarCipher(Language language) {
        this.language = language;
        this.alphabet = language.alphabet();
        this.size = alphabet.length;
    }

    /** Encrypt by shifting letters possition forward according to the key **/
    public String encrypt(String text, int key) {
        return applyShift(text, normalizeKey(key));
    }

    /** Decrypts by key entered by user **/
    public String decrypt(String text, int key) {
        return applyShift(text, normalizeKey(-key));
    }

    public String bruteForce(String text) {
        String bestCandidate = text;
        double bestScore = Double.NEGATIVE_INFINITY;

        int alphabetSize = alphabet.length / 2; // 26 для EN, 33 для UA

        for (int key = 0; key < alphabetSize; key++) {
            String candidate = decrypt(text, key);
            double score = score(candidate);

            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
            }
        }

        return bestCandidate;
    }

    private double score(String text) {
        String lower = text.toLowerCase();
        double score = 0;

        for (String word : language.commonWords()) {
            int index = 0;

            while ((index = lower.indexOf(word, index)) != -1) {
                score += 10;
                index += word.length();
            }
        }

        for (char c : text.toCharArray()) {
            if (c == ' ') {
                score += 0.5;
            }
            if (c == '.' || c == ',' || c == '!' || c == '?' ||
                    c == ';' || c == ':') {
                score += 0.2;
            }
        }

        return score;
    }
    private String applyShift(String text, int normKey) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            int idx = indexOf(c);
            sb.append(idx < 0 ? c : alphabet[(idx + normKey) % size]);
        }
        return sb.toString();
    }

    private int normalizeKey(int key) {
        return ((key % size) + size) % size;
    }

    private int indexOf(char c) {
        for (int i = 0; i < size; i++) {
            if (alphabet[i] == c)
                return i;
        }
        return -1;
    }
}