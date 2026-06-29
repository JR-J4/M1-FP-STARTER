package ua.com.javarush.j4.support;

import ua.com.javarush.j4.error.InvalidArgumentsException;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * A case-insensitive, name-keyed registry of values of some type {@code T}.
 *
 * <h2>Why generics here?</h2>
 * <p>{@code CipherCatalog} and {@code ScorerCatalog} used to contain the
 * <em>identical</em> plumbing: a {@code Map} keyed by a lower-cased name, a
 * {@code register} method, and a "look up or throw" method — the only thing that
 * differed was the <em>type of value</em> stored. Copy-pasting that logic once
 * per value type is exactly the duplication generics exist to remove.
 *
 * <p>The type parameter {@code <T>} is a placeholder for "whatever this particular
 * registry stores". Each catalog picks a concrete type when it creates one:
 * <pre>{@code
 *   new NamedRegistry<CipherCreator>("cipher")                       // T = CipherCreator
 *   new NamedRegistry<Function<Language, FitnessScorer>>("scorer")   // T = a function
 * }</pre>
 *
 * <p>Because {@link #get(String)} returns {@code T}, callers get the right type back
 * with <strong>no cast</strong>. Before generics this map would have been
 * {@code Map<String, Object>} and every caller would need {@code (Cipher) map.get(name)} —
 * a cast the compiler cannot check, so a wrong type only blows up at run time.
 *
 * @param <T> the type of value stored under each name
 */
public final class NamedRegistry<T> {

    /** Sorted so the "valid values" hint in error messages is deterministic. */
    private final Map<String, T> entries = new TreeMap<>();
    private final String kind;

    /** @param kind a human-readable noun for error messages, e.g. {@code "cipher"}. */
    public NamedRegistry(String kind) {
        this.kind = kind;
    }

    /** Registers {@code value} under {@code name} (matched case-insensitively). */
    public void register(String name, T value) {
        entries.put(name.toLowerCase(Locale.ROOT), value);
    }

    /**
     * Returns the value registered under {@code name}.
     *
     * @throws InvalidArgumentsException if nothing is registered under that name;
     *         the message lists the valid names.
     */
    public T get(String name) {
        T value = entries.get(name.toLowerCase(Locale.ROOT));
        if (value == null) {
            throw new InvalidArgumentsException(
                    "Unknown " + kind + " '" + name + "'. Valid values: " + String.join(", ", entries.keySet()));
        }
        return value;
    }
}
