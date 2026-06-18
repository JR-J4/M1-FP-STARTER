package ua.com.javarush.j4.crack;

import ua.com.javarush.j4.alphabet.Alphabets;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Built-in language profiles (common words, letter frequencies, distinctive letters). */
public final class LanguageProfiles {

    public static final LanguageProfile ENGLISH = new LanguageProfile(
            "en",
            Alphabets.ENGLISH,
            Set.of("the", "and", "to", "of", "a", "in", "is", "it", "that", "he",
                    "was", "for", "on", "are", "as", "with", "his", "they", "at", "be",
                    "this", "from", "or", "had", "not", "but", "what", "all", "were", "we",
                    "when", "you", "your", "can", "said", "there", "which", "she", "do", "their"),
            Map.ofEntries(
                    Map.entry('e', 12.7), Map.entry('t', 9.1), Map.entry('a', 8.2),
                    Map.entry('o', 7.5), Map.entry('i', 7.0), Map.entry('n', 6.7),
                    Map.entry('s', 6.3), Map.entry('h', 6.1), Map.entry('r', 6.0),
                    Map.entry('d', 4.3), Map.entry('l', 4.0), Map.entry('u', 2.8),
                    Map.entry('c', 2.8), Map.entry('m', 2.4), Map.entry('w', 2.4),
                    Map.entry('f', 2.2), Map.entry('g', 2.0), Map.entry('y', 2.0),
                    Map.entry('p', 1.9), Map.entry('b', 1.5)),
            Set.of()); // English vs Cyrillic is decided by alphabet membership alone

    public static final LanguageProfile UKRAINIAN = new LanguageProfile(
            "ua",
            Alphabets.UKRAINIAN,
            Set.of("і", "в", "на", "з", "що", "не", "як", "до", "за", "це",
                    "та", "а", "по", "ні", "так", "він", "вона", "вони", "був", "була",
                    "було", "у", "від", "для", "при", "про", "але", "або", "її", "його",
                    "ми", "ви", "я", "ти", "де", "коли", "тут", "там", "ще", "вже"),
            Map.ofEntries(
                    Map.entry('о', 9.0), Map.entry('а', 7.0), Map.entry('н', 6.5),
                    Map.entry('и', 6.0), Map.entry('і', 5.7), Map.entry('в', 5.0),
                    Map.entry('т', 4.5), Map.entry('е', 4.5), Map.entry('р', 4.0),
                    Map.entry('с', 4.0), Map.entry('к', 3.5), Map.entry('л', 3.5),
                    Map.entry('д', 3.0), Map.entry('у', 3.0), Map.entry('м', 3.0),
                    Map.entry('п', 2.8), Map.entry('я', 2.0), Map.entry('з', 2.0),
                    Map.entry('б', 1.7), Map.entry('г', 1.4)),
            Set.of('і', 'І', 'ї', 'Ї', 'є', 'Є', 'ґ', 'Ґ'));

    public static final LanguageProfile RUSSIAN = new LanguageProfile(
            "ru",
            Alphabets.RUSSIAN,
            Set.of("и", "в", "не", "на", "я", "что", "тот", "быть", "с", "он",
                    "а", "по", "это", "она", "этот", "к", "но", "они", "мы", "как",
                    "из", "у", "который", "то", "за", "свой", "весь", "год", "от",
                    "так", "о", "для", "бы", "вы", "со", "если", "уже", "или", "ни"),
            Map.ofEntries(
                    Map.entry('о', 10.9), Map.entry('е', 8.4), Map.entry('а', 8.0),
                    Map.entry('и', 7.4), Map.entry('н', 6.7), Map.entry('т', 6.3),
                    Map.entry('с', 5.5), Map.entry('р', 4.7), Map.entry('в', 4.5),
                    Map.entry('л', 4.4), Map.entry('к', 3.5), Map.entry('м', 3.2),
                    Map.entry('д', 3.0), Map.entry('п', 2.8), Map.entry('у', 2.6),
                    Map.entry('я', 2.0), Map.entry('ы', 1.9), Map.entry('з', 1.8),
                    Map.entry('б', 1.6), Map.entry('г', 1.7)),
            Set.of('ё', 'Ё', 'ъ', 'Ъ', 'ы', 'Ы', 'э', 'Э'));

    private LanguageProfiles() {
    }

    public static List<LanguageProfile> all() {
        return List.of(ENGLISH, UKRAINIAN, RUSSIAN);
    }
}
