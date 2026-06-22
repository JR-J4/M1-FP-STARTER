package ua.com.javarush.j4.crack;

import java.util.Iterator;
import java.util.List;

/**
 * Combines multiple scorers into one by averaging their scores.
 *
 * <p>Implements {@link FitnessScorer} so it can be used anywhere a scorer is expected,
 * and {@link Iterable}{@code <FitnessScorer>} so callers can inspect the constituent
 * scorers with a standard for-each loop:
 * <pre>
 *   for (FitnessScorer s : composite) { System.out.println(s); }
 * </pre>
 */
public final class CompositeFitnessScorer implements FitnessScorer, Iterable<FitnessScorer> {

    private final List<FitnessScorer> scorers;

    public CompositeFitnessScorer(List<FitnessScorer> scorers) {
        if (scorers.isEmpty()) {
            throw new IllegalArgumentException("At least one scorer is required");
        }
        this.scorers = List.copyOf(scorers);
    }

    /** Average of all member scores. */
    @Override
    public double score(String text) {
        return scorers.stream()
                .mapToDouble(s -> s.score(text))
                .average()
                .orElse(0.0);
    }

    /** Iterates over the constituent scorers (enables for-each). */
    @Override
    public Iterator<FitnessScorer> iterator() {
        return scorers.iterator();
    }
}
