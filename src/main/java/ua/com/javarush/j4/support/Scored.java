package ua.com.javarush.j4.support;

import java.util.function.ToDoubleFunction;

/**
 * A value of type {@code T} paired with a numeric score.
 *
 * <h2>Two flavours of generics in one small file</h2>
 * <ul>
 *   <li>{@code Scored<T>} is a <em>generic type</em>: the type parameter belongs to
 *       the record, so a {@code Scored<String>} and a {@code Scored<CrackResult>} are
 *       distinct, fully type-checked types.</li>
 *   <li>{@link #bestOf} is a <em>generic method</em>: the {@code <T>} written
 *       <em>before the return type</em> is owned by the method, not the class, and is
 *       inferred fresh at each call from the arguments.</li>
 * </ul>
 *
 * <p>This replaces the hand-rolled "track the best so far" loop that the cracker used
 * to spell out (best value, best score, {@code if (score > bestScore) ...}). That
 * pattern is identical regardless of <em>what</em> is being scored, so it is a natural
 * generic method.
 *
 * @param <T>   the type of the scored value
 * @param value the value
 * @param score its score (higher is better)
 */
public record Scored<T>(T value, double score) {

    /**
     * Returns the highest-scoring candidate. Ties keep the earliest candidate, so an
     * ascending input order yields the lowest-keyed winner — matching the cracker's
     * original tie-breaking.
     *
     * @param candidates the candidates to score (must be non-empty)
     * @param scorer     how to score a candidate; higher is better
     * @param <T>        the candidate type, inferred from {@code candidates}
     * @throws IllegalArgumentException if {@code candidates} is empty
     */
    public static <T> Scored<T> bestOf(Iterable<T> candidates, ToDoubleFunction<T> scorer) {
        Scored<T> best = null;
        for (T candidate : candidates) {
            double score = scorer.applyAsDouble(candidate);
            if (best == null || score > best.score) {
                best = new Scored<>(candidate, score);
            }
        }
        if (best == null) {
            throw new IllegalArgumentException("Cannot pick the best of zero candidates");
        }
        return best;
    }
}
