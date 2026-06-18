package ua.com.javarush.j4.crack;

/**
 * Scores a text by how much its letters favour high-frequency letters of the
 * language: sum of expected frequencies over all letters, normalised by length.
 */
public final class FrequencyScorer implements FitnessScorer {
    private final LanguageProfile profile;

    public FrequencyScorer(LanguageProfile profile) {
        this.profile = profile;
    }

    @Override
    public double score(String text) {
        double sum = 0.0;
        int letters = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toLowerCase(text.charAt(i));
            Double freq = profile.letterFrequencies().get(c);
            if (freq != null) {
                sum += freq;
            }
            if (Character.isLetter(text.charAt(i))) {
                letters++;
            }
        }
        return letters == 0 ? 0.0 : sum / letters;
    }
}
