package ua.com.javarush.j4.crypto;

import ua.com.javarush.j4.language.Alphabet;

public record Cypher(Alphabet alphabet) {

    private int normalizeKey(int k) {
        return ((k % alphabet.getSize()) + alphabet.getSize()) % alphabet.getSize();
    }

    public String encrypt(String text, int key) {
        return shift(text, normalizeKey(key));
    }

    public String decrypt(String text, int key) {
        return shift(text, normalizeKey(-key));
    }

    private String shift(String text, int shift) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char ch : text.toCharArray()) {
            int idx = alphabet.indexOf(ch);
            if (idx != -1) {
                sb.append(alphabet.charAt((idx + shift) % alphabet.getSize()));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
