package ua.com.javarush.j4.crypty;

public class BruteForce {

    private final Cipher cipher;

    private static final String[] COMMON_WORDS = {"the", "and", "of", "is", "to", "in", "that", "it"};

    // Конструктор приймає готовий об'єкт Cipher
    // Dependency Injection / впровадження залежностей
    public BruteForce(Cipher cipher) {
        this.cipher = cipher;
    }

    public int decryptByBruteForce(String encryptedText) {
        int rightKey = 0;
        int maxMatches = -1;

        for (int key = 0; key < cipher.getAlphabetLength(); key++) {
            String decryptedCandidate = cipher.decrypt(encryptedText, key);

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

        for (String word : COMMON_WORDS) {
            int index = 0;
            while ((index = lowerCaseText.indexOf(word, index)) != -1) {
                count++;
                index += word.length();
            }
        }
        return count;
    }
}