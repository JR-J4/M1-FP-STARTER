package ua.com.javarush.j4.alphabet;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlphabetTest {

    private static final List<String> EN_RINGS = List.of(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            "abcdefghijklmnopqrstuvwxyz");
    private static final List<String> UA_RINGS = List.of(
            "АБВГҐДЕЄЖЗИІЇЙКЛМНОПРСТУФХЦЧШЩЬЮЯ",
            "абвгґдеєжзиіїйклмнопрстуфхцчшщьюя");
    private static final List<String> MIXED_RINGS = List.of(
            EN_RINGS.get(0), EN_RINGS.get(1), UA_RINGS.get(0), UA_RINGS.get(1));

    /**
     * Reference implementation: the linear ring scan {@link Alphabet} used before it grew a
     * lookup table. Every fast-path answer must still match this, character for character.
     */
    private static String naiveRingOf(List<String> rings, char c) {
        for (String ring : rings) {
            if (ring.indexOf(c) >= 0) {
                return ring;
            }
        }
        return null;
    }

    private static char naiveShift(List<String> rings, char c, int k) {
        String ring = naiveRingOf(rings, c);
        if (ring == null) {
            return c;
        }
        int n = ring.length();
        return ring.charAt(((ring.indexOf(c) + k) % n + n) % n);
    }

    private static char naiveMirror(List<String> rings, char c) {
        String ring = naiveRingOf(rings, c);
        return ring == null ? c : ring.charAt(ring.length() - 1 - ring.indexOf(c));
    }

    private static Alphabet build(String name, List<String> rings) {
        return new Alphabet(name, rings.stream().map(CharacterRing::new).toList());
    }

    /**
     * The property that licenses the lookup table: over the whole {@code char} range and a
     * spread of keys — negative, zero, one full cycle, and beyond — the alphabet agrees with
     * a naive scan of its own rings.
     */
    @Test
    void agreesWithANaiveRingScanForEveryCharacter() {
        record Case(String name, List<String> rings) {
        }
        List<Case> cases = List.of(
                new Case("en", EN_RINGS),
                new Case("ua", UA_RINGS),
                new Case("mixed", MIXED_RINGS));
        int[] keys = {-33, -26, -1, 0, 1, 7, 26, 33, 100};

        for (Case testCase : cases) {
            Alphabet alphabet = build(testCase.name(), testCase.rings());
            for (int codeUnit = 0; codeUnit <= Character.MAX_VALUE; codeUnit++) {
                char c = (char) codeUnit;
                String where = testCase.name() + " U+" + Integer.toHexString(codeUnit);

                assertEquals(naiveRingOf(testCase.rings(), c) != null, alphabet.contains(c),
                        "contains " + where);
                assertEquals(naiveMirror(testCase.rings(), c), alphabet.mirror(c), "mirror " + where);

                String ring = naiveRingOf(testCase.rings(), c);
                assertEquals(ring == null ? -1 : ring.indexOf(c), alphabet.position(c).orElse(-1),
                        "position " + where);

                for (int key : keys) {
                    assertEquals(naiveShift(testCase.rings(), c, key), alphabet.shift(c, key),
                            "shift " + where + " by " + key);
                }
            }
        }
    }

    @Test
    void keyspaceIsTheLargestRing() {
        assertEquals(26, build("en", EN_RINGS).keyspaceSize());
        assertEquals(33, build("ua", UA_RINGS).keyspaceSize());
        assertEquals(33, build("mixed", MIXED_RINGS).keyspaceSize());
    }

    @Test
    void shiftsWithinCaseAndWrapsByModulo() {
        Alphabet en = Alphabets.ENGLISH;
        assertEquals('B', en.shift('A', 1));
        assertEquals('z', en.shift('a', 25));
        assertEquals('Z', en.shift('A', -1));   // wraps within upper ring
        assertEquals('A', en.shift('A', 26));    // full cycle
        assertEquals('B', en.shift('A', 27));    // 27 mod 26 == 1
    }

    @Test
    void ukrainianIsA33LetterRing() {
        Alphabet ua = Alphabets.UKRAINIAN;
        assertEquals('Б', ua.shift('А', 1));
        assertEquals('Я', ua.shift('А', 32));
        assertEquals('я', ua.shift('а', 32));
    }

    @Test
    void nonMemberCharactersPassThrough() {
        Alphabet en = Alphabets.ENGLISH;
        assertEquals('5', en.shift('5', 3));
        assertEquals(' ', en.shift(' ', 3));
        assertEquals('А', en.shift('А', 3)); // Cyrillic not in English alphabet
    }

    @Test
    void mirrorImplementsAtbashWithinRing() {
        Alphabet en = Alphabets.ENGLISH;
        assertEquals('Z', en.mirror('A'));
        assertEquals('a', en.mirror('z'));
        assertEquals('5', en.mirror('5'));
    }

    @Test
    void positionReturnsRingIndex() {
        assertEquals(0, Alphabets.ENGLISH.position('A').orElse(-1));
        assertEquals(10, Alphabets.ENGLISH.position('k').orElse(-1));
        assertTrue(Alphabets.ENGLISH.position('5').isEmpty());
    }

    @Test
    void defaultAlphabetHandlesBothEnglishAndUkrainian() {
        Alphabet def = Alphabets.DEFAULT;
        assertEquals('B', def.shift('A', 1));
        assertEquals('Б', def.shift('А', 1));
        assertEquals(33, def.keyspaceSize()); // largest ring (Ukrainian)
    }

    @Test
    void byNameRejectsUnknownAlphabet() {
        assertThrows(InvalidArgumentsException.class, () -> Alphabets.byName("klingon"));
    }
}
