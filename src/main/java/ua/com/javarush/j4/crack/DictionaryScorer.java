package ua.com.javarush.j4.crack;

import java.util.Locale;

/** Counts how many whitespace/punctuation-delimited tokens are common words of the language. */
public final class DictionaryScorer implements FitnessScorer {
    private final LanguageProfile profile;

    public DictionaryScorer(LanguageProfile profile) {
        this.profile = profile;
    }

    @Override
    public double score(String text) {
        String[] tokens = text.toLowerCase(Locale.ROOT).split("[^\\p{L}]+");
        int hits = 0;
        for (String token : tokens) {
            if (!token.isEmpty() && profile.commonWords().contains(token)) {
                hits++;
            }
        }
        return hits;
    }
}
