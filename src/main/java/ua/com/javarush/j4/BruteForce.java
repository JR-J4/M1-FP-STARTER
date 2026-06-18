package ua.com.javarush.j4;

import java.util.List;

/**
 * Перебір ключів без знання правильного. Для кожного з 26 можливих ключів
 * розшифровує текст і оцінює, наскільки результат схожий на справжню англійську
 * мову (рахує входження частих слів). Перемагає ключ з найбільшою оцінкою.
 */
public class BruteForce {

    private static final List<String> COMMON_WORDS = List.of(
            "the", "and", "that", "have", "for", "not", "with", "you",
            "this", "but", "his", "her", "she", "they", "from", "was",
            "are", "were", "what", "which", "will", "would", "there",
            "their", "of", "to", "in", "is", "it");

    private final CaesarCipher cipher;

    public BruteForce(CaesarCipher cipher) {
        this.cipher = cipher;
    }

    public int findKey(String encryptedText) {
        int bestKey = 0;
        int bestScore = -1;
        for (int key = 0; key < 26; key++) {
            String candidate = cipher.decrypt(encryptedText, key);
            int score = scoreEnglish(candidate);
            if (score > bestScore) {
                bestScore = score;
                bestKey = key;
            }
        }
        return bestKey;
    }

    private int scoreEnglish(String text) {
        String lower = text.toLowerCase();
        int score = 0;
        for (String word : COMMON_WORDS) {
            int index = 0;
            while ((index = lower.indexOf(word, index)) != -1) {
                score++;
                index += word.length();
            }
        }
        return score;
    }
}
