package ua.com.javarush.j4.alphabet;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

/**
 * An alphabet is an ordered set of case-independent character rings.
 *
 * <p>Every cipher in this project calls {@link #shift} once per character of the input, so
 * finding which ring a character belongs to is the hottest operation in the codebase.
 * Scanning the rings for each character costs a linear search per ring; instead the
 * constructor walks the rings once and records, for every character they span, which ring
 * it is in and where. Lookup is then two array reads.
 *
 * <p>The table covers the range from the lowest to the highest character in the rings —
 * about 1 KB for the shipped Latin + Cyrillic alphabet — and is built once per alphabet,
 * never per key. That matters: a brute-force sweep builds one cipher per key, and a
 * per-key table would cost more than the sweep it was meant to speed up.
 */
public final class Alphabet {
    private static final int NOT_A_MEMBER = -1;

    private final String name;
    private final List<CharacterRing> rings;
    private final int keyspaceSize;

    /** Character that {@code ringIndex[0]} describes. */
    private final char base;
    /** Ring each character belongs to, or {@link #NOT_A_MEMBER}, indexed by {@code c - base}. */
    private final byte[] ringIndex;
    /** Position of each character within its ring, indexed by {@code c - base}. */
    private final int[] ringPosition;

    public Alphabet(String name, List<CharacterRing> rings) {
        this.name = name;
        this.rings = List.copyOf(rings);
        if (this.rings.size() > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("an alphabet may hold at most " + Byte.MAX_VALUE + " rings");
        }

        int largest = 1;
        char lowest = Character.MAX_VALUE;
        char highest = Character.MIN_VALUE;
        for (CharacterRing ring : this.rings) {
            largest = Math.max(largest, ring.size());
            for (int i = 0; i < ring.size(); i++) {
                lowest = (char) Math.min(lowest, ring.at(i));
                highest = (char) Math.max(highest, ring.at(i));
            }
        }
        this.keyspaceSize = largest;

        int span = this.rings.isEmpty() ? 0 : highest - lowest + 1;
        this.base = this.rings.isEmpty() ? 0 : lowest;
        this.ringIndex = new byte[span];
        this.ringPosition = new int[span];
        Arrays.fill(this.ringIndex, (byte) NOT_A_MEMBER);
        for (int r = 0; r < this.rings.size(); r++) {
            CharacterRing ring = this.rings.get(r);
            for (int i = 0; i < ring.size(); i++) {
                int slot = ring.at(i) - this.base;
                if (this.ringIndex[slot] == NOT_A_MEMBER) { // first ring listed wins, as a scan would
                    this.ringIndex[slot] = (byte) r;
                    this.ringPosition[slot] = i;
                }
            }
        }
    }

    public String name() {
        return name;
    }

    public boolean contains(char c) {
        return slotOf(c) != NOT_A_MEMBER;
    }

    /** Shift within the char's own ring; characters outside the alphabet pass through. */
    public char shift(char c, int k) {
        int slot = slotOf(c);
        return slot == NOT_A_MEMBER ? c : ringAt(slot).shiftFrom(ringPosition[slot], k);
    }

    /** Atbash mirror within the char's own ring; non-members pass through. */
    public char mirror(char c) {
        int slot = slotOf(c);
        return slot == NOT_A_MEMBER ? c : ringAt(slot).mirrorFrom(ringPosition[slot]);
    }

    /** Index of the char within its ring, or {@code -1} if it is not in this alphabet. */
    public int indexOf(char c) {
        int slot = slotOf(c);
        return slot == NOT_A_MEMBER ? NOT_A_MEMBER : ringPosition[slot];
    }

    /** Index of the char within its ring (used by Vigenère for keyword shifts). */
    public OptionalInt position(char c) {
        int index = indexOf(c);
        return index == NOT_A_MEMBER ? OptionalInt.empty() : OptionalInt.of(index);
    }

    /** Largest ring size — the meaningful upper bound for a Caesar keyspace sweep. */
    public int keyspaceSize() {
        return keyspaceSize;
    }

    /** Table slot for {@code c}, or {@link #NOT_A_MEMBER} if it is outside the alphabet. */
    private int slotOf(char c) {
        int slot = c - base;
        return slot >= 0 && slot < ringIndex.length && ringIndex[slot] != NOT_A_MEMBER
                ? slot
                : NOT_A_MEMBER;
    }

    private CharacterRing ringAt(int slot) {
        return rings.get(ringIndex[slot]);
    }
}
