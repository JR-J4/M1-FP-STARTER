package ua.com.javarush.j4;

public interface Language {

    /** All letters in order (uppercase first, then lowercase). */
    char[] alphabet();

    /** Frequent short words used to score brute-force candidates. */
    String[] commonWords();

    /** Auto-detects which language a piece of text is used */
    static Language detect(String text) {
        int ukCount = 0;
        String ukLetters =
                "АБВГҐДЕЄЖЗИІЇЙКЛМНОПРСТУФХЦЧШЩЬЮЯабвгґдеєжзиіїйклмнопрстуфхцчшщьюя";
        for (char c : text.toCharArray()) {
            if (ukLetters.indexOf(c) >= 0) {
                ukCount++;
            }
        }
        return ukCount > text.length() / 2 ? new UkrainianLanguage() : new EnglishLanguage();
    }
}