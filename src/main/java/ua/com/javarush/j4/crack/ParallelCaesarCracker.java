package ua.com.javarush.j4.crack;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.cipher.CaesarCipher;
import ua.com.javarush.j4.concurrent.ParallelPolicy;
import ua.com.javarush.j4.concurrent.TaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * The same sweep as {@link CaesarCracker}, with the keyspace split into contiguous
 * ranges scored in parallel.
 *
 * <p>Results are identical to the sequential sweep, tie-breaking included. Ranges are
 * contiguous and ascending, each range is scanned in ascending key order with a strict
 * {@code >}, and the partial winners are folded left to right with a strict {@code >}.
 * So the lowest key wins a tie inside a range, and the lowest range wins a tie across
 * ranges — exactly what a single ascending scan would do.
 *
 * <p>Ranges rather than one task per key: a task holds a full decrypted copy of the text,
 * so per-key tasks would keep the whole keyspace of copies alive at once.
 */
public final class ParallelCaesarCracker implements Cracker {
    private final Alphabet alphabet;
    private final FitnessScorer scorer;
    private final TaskExecutor executor;
    private final ParallelPolicy policy;
    private final Cracker sequential;

    public ParallelCaesarCracker(Alphabet alphabet, FitnessScorer scorer,
                                 TaskExecutor executor, ParallelPolicy policy) {
        this.alphabet = alphabet;
        this.scorer = scorer;
        this.executor = executor;
        this.policy = policy;
        this.sequential = new CaesarCracker(alphabet, scorer);
    }

    @Override
    public CrackResult crack(String ciphertext) {
        int keyspace = alphabet.keyspaceSize();
        int partitions = Math.min(keyspace, executor.parallelism());
        if (partitions <= 1 || !policy.shouldParallelizeCrack(ciphertext.length())) {
            return sequential.crack(ciphertext);
        }

        List<Callable<Candidate>> tasks = new ArrayList<>(partitions);
        for (int partition = 0; partition < partitions; partition++) {
            int from = (int) ((long) keyspace * partition / partitions);
            int to = (int) ((long) keyspace * (partition + 1) / partitions);
            tasks.add(() -> bestInRange(ciphertext, from, to));
        }

        Candidate best = null;
        for (Candidate candidate : executor.invokeAll(tasks)) {
            if (best == null || candidate.score() > best.score()) {
                best = candidate;
            }
        }
        return new CrackResult(best.key(), best.plaintext());
    }

    private Candidate bestInRange(String ciphertext, int fromInclusive, int toExclusive) {
        Candidate best = null;
        for (int key = fromInclusive; key < toExclusive; key++) {
            String plaintext = new CaesarCipher(alphabet, key).decrypt(ciphertext);
            double score = scorer.score(plaintext);
            if (best == null || score > best.score()) {
                best = new Candidate(key, plaintext, score);
            }
        }
        return best;
    }

    private record Candidate(int key, String plaintext, double score) {
    }
}
