package ua.com.javarush.j4.language;

import java.util.ArrayList;
import java.util.List;

public final class Alphabet {

    public static final List<Character> ENGLISH = createEnglish();
    public static final List<Character> UKRAINIAN = createUkrainian();

    private Alphabet() {
    }

    private static List<Character> createEnglish() {
        List<Character> alphabet = new ArrayList<>();
        for (char ch = 'A'; ch <= 'Z'; ch++) {
            alphabet.add(ch);
        }
        for (char ch = 'a'; ch <= 'z'; ch++) {
            alphabet.add(ch);
        }
//        addSymbols(alphabet);
        return List.copyOf(alphabet);
    }

    private static List<Character> createUkrainian() {
        List<Character> alphabet = new ArrayList<>();
        String letters = "АБВГҐДЕЄЖЗИІЇЙКЛМНОПРСТУФХЦЧШЩЬЮЯ" +
                "абвгґдеєжзиіїйклмнопрстуфхцчшщьюя";

        for (char ch : letters.toCharArray()) {
            alphabet.add(ch);
        }
//        addSymbols(alphabet);
        return List.copyOf(alphabet);
    }

    private static void addSymbols(List<Character> alphabet) {
        for (char ch : new char[]{'.', ',', '\'', '"', '«', '»', ':', '!', '?', ' '}) {
            alphabet.add(ch);
        }
    }
}