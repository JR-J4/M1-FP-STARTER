package ua.com.javarush.j4.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoredTest {

    @Test
    void bestOfPicksHighestScoringValue() {
        // T inferred as String from the argument.
        Scored<String> best = Scored.bestOf(List.of("a", "bbbb", "cc"), String::length);
        assertEquals("bbbb", best.value());
        assertEquals(4.0, best.score());
    }

    @Test
    void bestOfKeepsTheEarliestCandidateOnTies() {
        Scored<String> best = Scored.bestOf(List.of("xx", "yy", "zz"), String::length);
        assertEquals("xx", best.value());
    }

    @Test
    void bestOfWorksForAnyType() {
        // Same generic method, T = Integer this time.
        Scored<Integer> best = Scored.bestOf(List.of(3, 9, 5), i -> i);
        assertEquals(9, best.value());
    }

    @Test
    void bestOfRejectsEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> Scored.bestOf(List.<String>of(), String::length));
    }
}
