package ua.com.javarush.j4.crypty;

public class Cipher {

    //великі латинські літери (від A до Z).97–122: Маленькі латинські літери (від a до z).
    private static final int ALPHABET_LENGTH = 26;

    public String encrypt(String text, int key) {
        StringBuilder result = new StringBuilder(text.length());

        int normalKey = ((key % ALPHABET_LENGTH) + ALPHABET_LENGTH) % ALPHABET_LENGTH;

        for (int i = 0; i < text.length(); i++) {
            char symbol = text.charAt(i);
            if (symbol >= 'A' && symbol <= 'Z') {
                int index = symbol - 'A';
                int newIndex = (index + normalKey) % ALPHABET_LENGTH;
                result.append((char) ('A' + newIndex));
            } else if (symbol >= 'a' && symbol <= 'z') {
                int index = symbol - 'a';
                int newIndex = (index + normalKey) % ALPHABET_LENGTH;
                result.append((char) ('a' + newIndex));
            } else {
                result.append(symbol);
            }
        }
        return result.toString();
    }

    public String decrypt(String text, int key) {
        return encrypt(text, -key);
    }

    public int getAlphabetLength() {
        return ALPHABET_LENGTH;
    }

}
