package ua.com.javarush.j4;


public class EnglishLanguage implements Language {

    private static final char[] ALPHABET = buildAlphabet();

    private static final String[] COMMON_WORDS = {
            "the", "and", "of", "to", "in", "is", "it", "that",
            "was", "for", "be", "or", "not", "he", "she", "his",
            "but", "with", "are", "this", "have", "from", "had"
    };

    private static char[] buildAlphabet() {
        char[] a = new char[52];
        for (int i = 0; i < 26; i++) {
            a[i] = (char) ('A' + i);
            a[i + 26] = (char) ('a' + i);
        }
        return a;
    }

    @Override
    public char[] alphabet() {
        return ALPHABET;
    }

    @Override
    public String[] commonWords() {
        return COMMON_WORDS;
    }
}