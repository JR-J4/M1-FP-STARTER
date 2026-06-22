package ua.com.javarush.j4.crack;

/** Strategy: scores how strongly a text resembles natural language. Higher is better. */
public interface FitnessScorer {
    public final static int MIN_SCORE = 10;

    double score(String text);

    /** Returns whichever of the two texts scores higher. Ties favour {@code a}. */
    default String pickBest(String a, String b) {
        return score(a) >= score(b) ? a : b;
    }
}
