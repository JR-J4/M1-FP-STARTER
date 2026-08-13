package ua.com.javarush.j4.cipher;

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

    /**
     * Below this, splitting does not pay for itself.
     *
     * <p><b>The rule this number follows:</b> a split must save more than the worst observed
     * cost of the pool that performs it. The first split in a process is what creates that
     * pool, and a cold pool is expensive and erratic — measured at 3.3, 7.9, 8.2 and 12.6 ms
     * across four runs on the same 12-core machine. It is one sample per JVM by construction,
     * so the spread cannot be averaged away; taking the worst is what keeps the gate stable
     * instead of drifting every time someone re-measures.
     *
     * <p>Against a ~13 ms worst case, a split saves 1.3 ms at 1 MB, 7.5 ms at 4 MB, ~15 ms at
     * 8 MB and 31 ms at 16 MB. 8 MB is the smallest power of two that clears it.
     *
     * <p>The cold bar is the right one for the CLI, which is one-shot: it starts a JVM, splits
     * at most a few times and exits. End-to-end that shows up as an absence — through the jar,
     * a 4.7 MB encrypt at {@code --threads 1} is no slower than at every core. Used as a
     * library with a pool that stays warm the bar is instead ~0.1 ms, which a 256 KB text
     * already clears, and a far lower gate would pay. Run {@code ConcurrencyBenchmark} §4,
     * which prints both bars and forces a split at every size, before moving this number.
     *
     * <p>It used to be 64 KB, back when the alphabet did a linear ring scan per character.
     * Making that lookup O(1) sped the sequential path up roughly fourfold, and the point
     * where threads start earning their keep moved out with it: 64 KB now gains 1.0×.
     */
    public static final int MIN_CHARS_FOR_TRANSFORM = 1 << 23;

    private static final int MIN_CHUNK_CHARS = 16_384;
    private static final int CHUNKS_PER_THREAD = 4;

    private final Cipher delegate;
    private final TaskExecutor executor;
    private final int minCharsToSplit;

    public ParallelCipher(Cipher delegate, TaskExecutor executor) {
        this(delegate, executor, MIN_CHARS_FOR_TRANSFORM);
    }

    /**
     * With an explicit threshold, so a caller can ask what splitting a given text
     * <em>would</em> cost. Used by tests, which would otherwise have to allocate megabytes to
     * reach the shipped gate, and by the benchmark, which has to measure below it to show why
     * the gate sits where it does.
     */
    public ParallelCipher(Cipher delegate, TaskExecutor executor, int minCharsToSplit) {
        this.delegate = delegate;
        this.executor = executor;
        this.minCharsToSplit = minCharsToSplit;
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
        if (!executor.worthSplitting(text.length(), minCharsToSplit)) {
            return encrypting ? delegate.encrypt(text) : delegate.decrypt(text);
        }

        List<String> chunks = split(text);
        List<String> transformed = delegate instanceof PositionDependentCipher positional
                ? transformPositionDependent(chunks, positional, encrypting)
                : transformIndependent(chunks, encrypting);

        return join(transformed, text.length());
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

    /** Copies the pieces into one buffer of the known final size — no growing, no second copy. */
    private static String join(List<String> parts, int totalLength) {
        char[] out = new char[totalLength];
        int at = 0;
        for (String part : parts) {
            part.getChars(0, part.length(), out, at);
            at += part.length();
        }
        return new String(out);
    }
}
