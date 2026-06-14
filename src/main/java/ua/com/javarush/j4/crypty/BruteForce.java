package ua.com.javarush.j4.crypty;

import ua.com.javarush.j4.language.EN_language;

import static ua.com.javarush.j4.language.EN_language.abc;

public class BruteForce {

   Cypher cypher = new Cypher();

    //private static final String[] COMMON_WORDS = {"the", "and", "of", "is", "to", "in", "that", "it"};

    // Конструктор приймає готовий об'єкт Cipher
    // Dependency Injection / впровадження залежностей
//    public BruteForce(Cypher cypher) {
//        this.cypher = cypher;
//    }

    public int decryptBf(String encryptedText) {
        int rightKey = 0;
        int maxMatches = -1;

        for (int key = 0; key < EN_language.getAbc().length(); key++) {
            String decryptedCandidate = cypher.decrypt(encryptedText, key);

            int matches = countCommonWords(decryptedCandidate);

            if (matches > maxMatches) {
                maxMatches = matches;
                rightKey = key;
            }
        }
        return rightKey;
    }

    private int countCommonWords(String text) {
        int count = 0;
        String lowerCaseText = text.toLowerCase();

        for (String word : EN_language.getSomeEnWords()) {
            int index = 0;
            while ((index = lowerCaseText.indexOf(word, index)) != -1) {
                count++;
                index += word.length();
            }
        }
        return count;
    }

}
