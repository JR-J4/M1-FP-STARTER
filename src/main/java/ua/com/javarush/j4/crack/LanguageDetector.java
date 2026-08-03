package ua.com.javarush.j4.crack;

/**
 * Picks the most likely language profile for a text by counting alphabet
 * membership, with a strong bonus for letters distinctive to one language
 * (e.g. і/ї/є/ґ for Ukrainian, ё/ъ/ы/э for Russian).
 */
// ── SOLID ▸ S — Принцип єдиного обов'язку (SRP) ──
// Детектор відповідає лише за одне: вибрати найімовірніший мовний профіль тексту.
// Він не зламує шифр і не рахує «читабельність» — це робота Cracker та FitnessScorer.
public final class LanguageDetector {
    private static final int DISTINCTIVE_WEIGHT = 1000;

    public LanguageProfile detect(String text) {
        LanguageProfile best = LanguageProfiles.ENGLISH;
        long bestScore = Long.MIN_VALUE;
        for (LanguageProfile profile : LanguageProfiles.all()) {
            long score = scoreFor(profile, text);
            if (score > bestScore) {
                bestScore = score;
                best = profile;
            }
        }
        return best;
    }

    private long scoreFor(LanguageProfile profile, String text) {
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
