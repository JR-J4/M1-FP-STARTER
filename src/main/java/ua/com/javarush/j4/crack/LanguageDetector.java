package ua.com.javarush.j4.crack;

/**
 * Picks the most likely language profile for a text by counting alphabet
 * membership, with a strong bonus for letters distinctive to one language
 * (e.g. і/ї/є/ґ for Ukrainian, ё/ъ/ы/э for Russian).
 */
public final class LanguageDetector {
    private static final int DISTINCTIVE_WEIGHT = 1000;

    public Language detect(String text) {
        Language best = Languages.ENGLISH;
        long bestScore = Long.MIN_VALUE;
        for (Language profile : Languages.all()) {
            long score = scoreFor(profile, text);
            if (score > bestScore) {
                bestScore = score;
                best = profile;
            }
        }
        return best;
    }

    private long scoreFor(Language profile, String text) {
        long membership = 0;
        long distinctive = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (profile.alphabet().contains(c)) {
                membership++;
            }
            if (profile.distinctive().contains(c)) {
                distinctive++;
            }
        }
        return membership + distinctive * DISTINCTIVE_WEIGHT;
    }
}
