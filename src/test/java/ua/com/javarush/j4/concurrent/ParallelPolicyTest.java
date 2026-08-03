package ua.com.javarush.j4.concurrent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelPolicyTest {

    @Test
    void zeroOrLessMeansEveryAvailableCore() {
        int cores = Runtime.getRuntime().availableProcessors();
        assertEquals(cores, ParallelPolicy.of(0).threads());
        assertEquals(cores, ParallelPolicy.of(-1).threads());
    }

    @Test
    void anExplicitCountIsHonoured() {
        assertEquals(3, ParallelPolicy.of(3).threads());
    }

    @Test
    void oneThreadDisablesEveryParallelPath() {
        ParallelPolicy policy = ParallelPolicy.of(1);
        assertFalse(policy.shouldParallelizeCrack(Integer.MAX_VALUE));
        assertFalse(policy.shouldParallelizeTransform(Integer.MAX_VALUE));
        assertFalse(policy.shouldParallelizeBatch(Integer.MAX_VALUE));
    }

    @Test
    void crackThresholdIsInclusive() {
        ParallelPolicy policy = ParallelPolicy.of(4);
        assertFalse(policy.shouldParallelizeCrack(ParallelPolicy.MIN_CHARS_FOR_CRACK - 1));
        assertTrue(policy.shouldParallelizeCrack(ParallelPolicy.MIN_CHARS_FOR_CRACK));
    }

    @Test
    void transformThresholdIsInclusive() {
        ParallelPolicy policy = ParallelPolicy.of(4);
        assertFalse(policy.shouldParallelizeTransform(ParallelPolicy.MIN_CHARS_FOR_TRANSFORM - 1));
        assertTrue(policy.shouldParallelizeTransform(ParallelPolicy.MIN_CHARS_FOR_TRANSFORM));
    }

    @Test
    void batchNeedsAtLeastTwoFiles() {
        ParallelPolicy policy = ParallelPolicy.of(4);
        assertFalse(policy.shouldParallelizeBatch(1));
        assertTrue(policy.shouldParallelizeBatch(2));
    }

    @Test
    void shippedTestFixturesStayOnTheSequentialPath() {
        // hamlet.txt is 838 chars and orwell.txt is ~4 KB: MainTest must never go parallel.
        ParallelPolicy policy = ParallelPolicy.of(8);
        assertFalse(policy.shouldParallelizeCrack(4_200));
        assertFalse(policy.shouldParallelizeTransform(4_200));
    }
}
