package ua.com.javarush.j4.crack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoringTest {

    @Test
    void dictionaryScorerRanksRealEnglishHigher() {
        FitnessScorer scorer = new DictionaryScorer(Languages.ENGLISH);
        double real = scorer.score("the cat sat on the mat and the dog");
        double garbage = scorer.score("xyz qrs tuv wxy zab cde fgh");
        assertTrue(real > garbage, "real English should score higher than gibberish");
    }

    @Test
    void frequencyScorerRanksRealEnglishHigher() {
        FitnessScorer scorer = new FrequencyScorer(Languages.ENGLISH);
        double real = scorer.score("the quick brown fox jumps over the lazy dog");
        double garbage = scorer.score("zzzz qqqq xxxx jjjj kkkk wwww");
        assertTrue(real > garbage);
    }

    @Test
    void detectorIdentifiesEnglish() {
        assertEquals("en", new LanguageDetector().detect("Hello, this is plain English text.").code());
    }

    @Test
    void detectorIdentifiesUkrainianByDistinctiveLetters() {
        // Contains і, ї — distinctive to Ukrainian, absent in Russian.
        assertEquals("ua", new LanguageDetector().detect("Привіт, це українська їжа і мова.").code());
    }
}
