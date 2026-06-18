package ua.com.javarush.j4.crack;

import ua.com.javarush.j4.alphabet.Alphabet;

import java.util.Map;
import java.util.Set;

/** Everything a scorer/detector needs to judge a text as a given language. */
public record LanguageProfile(
        String name,
        Alphabet alphabet,
        Set<String> commonWords,
        Map<Character, Double> letterFrequencies,
        Set<Character> distinctive) {
}
