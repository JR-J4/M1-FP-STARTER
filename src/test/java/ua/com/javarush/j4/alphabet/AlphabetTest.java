package ua.com.javarush.j4.alphabet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlphabetTest {

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

}
