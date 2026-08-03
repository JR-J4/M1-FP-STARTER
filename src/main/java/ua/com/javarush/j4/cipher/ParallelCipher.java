package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.concurrent.ParallelPolicy;
import ua.com.javarush.j4.concurrent.TaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Splits a large text into chunks, transforms them in parallel, and rejoins them in
 * order. Output is identical to the wrapped cipher's, character for character.
 *
 * <p>A position-dependent delegate (see {@link PositionDependentCipher}) needs two
 * phases: count the alphabet letters in each chunk, turn those counts into starting
 * offsets with a prefix sum, then transform each chunk from its own offset. Everything
 * else transforms in one phase, since each character is independent.
 */
public final class ParallelCipher implements Cipher {
    private static final int MIN_CHUNK_CHARS = 16_384;
    private static final int CHUNKS_PER_THREAD = 4;

    private final Cipher delegate;
    private final TaskExecutor executor;
    private final ParallelPolicy policy;

    public ParallelCipher(Cipher delegate, TaskExecutor executor, ParallelPolicy policy) {
        this.delegate = delegate;
        this.executor = executor;
        this.policy = policy;
    }

    @Override
    public String encrypt(String text) {
        return process(text, true);
    }

    @Override
    public String decrypt(String text) {
        return process(text, false);
    }

    private String process(String text, boolean encrypting) {
        if (executor.parallelism() <= 1 || !policy.shouldParallelizeTransform(text.length())) {
            return encrypting ? delegate.encrypt(text) : delegate.decrypt(text);
        }

        List<String> chunks = split(text);
        List<String> transformed = delegate instanceof PositionDependentCipher positional
                ? transformPositionDependent(chunks, positional, encrypting)
                : transformIndependent(chunks, encrypting);

        StringBuilder out = new StringBuilder(text.length());
        transformed.forEach(out::append);
        return out.toString();
    }

    private List<String> transformIndependent(List<String> chunks, boolean encrypting) {
        List<Callable<String>> tasks = new ArrayList<>(chunks.size());
        for (String chunk : chunks) {
            tasks.add(() -> encrypting ? delegate.encrypt(chunk) : delegate.decrypt(chunk));
        }
        return executor.invokeAll(tasks);
    }

    private List<String> transformPositionDependent(List<String> chunks,
                                                    PositionDependentCipher cipher,
                                                    boolean encrypting) {
        List<Callable<Integer>> counters = new ArrayList<>(chunks.size());
        for (String chunk : chunks) {
            counters.add(() -> cipher.alphabetLetterCount(chunk));
        }
        List<Integer> counts = executor.invokeAll(counters);

        int[] offsets = new int[chunks.size()];
        int running = 0;
        for (int i = 0; i < chunks.size(); i++) {
            offsets[i] = running;
            running += counts.get(i);
        }

        List<Callable<String>> tasks = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            int offset = offsets[i];
            tasks.add(() -> encrypting
                    ? cipher.encryptFrom(chunk, offset)
                    : cipher.decryptFrom(chunk, offset));
        }
        return executor.invokeAll(tasks);
    }

    /**
     * Splits into roughly equal chunks, never inside a surrogate pair. The alphabets
     * shipped here are all BMP, so the surrogate check never fires in practice — it keeps
     * the splitter correct for arbitrary input.
     */
    private List<String> split(String text) {
        int target = Math.max(MIN_CHUNK_CHARS,
                text.length() / (executor.parallelism() * CHUNKS_PER_THREAD) + 1);

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + target);
            if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) {
                end++;
            }
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks;
    }
}
