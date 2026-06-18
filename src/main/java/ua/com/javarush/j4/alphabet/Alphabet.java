package ua.com.javarush.j4.alphabet;

import java.util.List;
import java.util.OptionalInt;

/** An alphabet is an ordered set of case-independent character rings. */
public final class Alphabet {
    private final String name;
    private final List<CharacterRing> rings;

    public Alphabet(String name, List<CharacterRing> rings) {
        this.name = name;
        this.rings = List.copyOf(rings);
    }

    public String name() {
        return name;
    }

    public boolean contains(char c) {
        return ringOf(c) != null;
    }

    /** Shift within the char's own ring; characters outside the alphabet pass through. */
    public char shift(char c, int k) {
        CharacterRing ring = ringOf(c);
        return ring == null ? c : ring.shift(c, k);
    }

    /** Atbash mirror within the char's own ring; non-members pass through. */
    public char mirror(char c) {
        CharacterRing ring = ringOf(c);
        return ring == null ? c : ring.mirror(c);
    }

    /** Index of the char within its ring (used by Vigenère for keyword shifts). */
    public OptionalInt position(char c) {
        CharacterRing ring = ringOf(c);
        return ring == null ? OptionalInt.empty() : OptionalInt.of(ring.indexOf(c));
    }

    /** Largest ring size — the meaningful upper bound for a Caesar keyspace sweep. */
    public int keyspaceSize() {
        return rings.stream().mapToInt(CharacterRing::size).max().orElse(1);
    }

    private CharacterRing ringOf(char c) {
        for (CharacterRing ring : rings) {
            if (ring.contains(c)) {
                return ring;
            }
        }
        return null;
    }
}
