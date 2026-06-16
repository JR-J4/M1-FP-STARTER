package ua.com.javarush.j4;

import java.util.Set;

public class BruteForce {
    public static int findKey(String text, CaesarCipher cipher) {
        // Длина самого большого алфавита (украинского) — 66 символов.
       // Перебираем ключи от 0 до 65.

        int maxKey = 66;

        int bestKey = 0;
        int maxScore = -1;

        // Создаем неизменяемое множество популярных слов
        Set<String> stopWords = Set.of("the", "and", "of", "що", "не", "і", "та", "у");

       for(int key = 0; key < maxKey; key++) {

           // Пробуем расшифровать текст текущим ключом (сдвиг назад, поэтому минус)
           String decrypted = cipher.encryptString(text, -key);

           // Переводим текст в нижний регистр и добавляем пробелы по бокам для безопасности
           String lowerText = " " + decrypted.toLowerCase() + " ";
           int score = 0;

          // 1.  ЧАСТОТНЫЙ ВАРИАНТ
           // Считаем пробелы и самые частые буквы ('e' для англ, 'о' для укр)

            for(int i = 0 ; i < lowerText.length(); i++) {
                char c = lowerText.charAt(i);
                if(c == ' '){
                    score += 3; // Пробел — важный маркер разделения слов
                } else if (c == 'e' || c == 'o') {
                    score += 1; // Самые частые буквы в языках
                }
            }
           // 2. ПОДХОД ЧЕРЕЗ СЛОВА
           // Проходим циклом  по множеству стоп-слов
            for(String word :stopWords) {
                // Если текст содержит слово с пробелами по бокам,то прибавляем баллы
                if(lowerText.contains(" " + word + " ")) {
                    score += 50;  // Даем 50 баллов за совпадение

                }
            }
           // Если этот ключ набрал больше очков, чем предыдущий
           if(score > maxScore) {
               maxScore = score;
               bestKey = key; //  запоминаем ключ,как лучший
           }

       }
        return bestKey;

    }
}
