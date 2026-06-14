package ua.com.javarush.j4.crypty;

import java.util.List;

public class Cipher {
    private final List<Character> alphabet;

    public Cipher(List<Character> alphabet) {
        this.alphabet = alphabet;
    }


    public String encrypt(String text, int key) {
        return shift(text, key);
    }

    public String decrypt(String text, int key) {
        return shift(text, -key);
    }

    private String shift(String text, int key) {
        StringBuilder result = new StringBuilder();
        for (char ch : text.toCharArray()) {
            int index = alphabet.indexOf(ch);
            if (index == -1) {
                result.append(ch);
                continue;
            }
            int newIndex = Math.floorMod(index + key, alphabet.size());
            result.append(alphabet.get(newIndex));
        }
        return result.toString();
    }
}