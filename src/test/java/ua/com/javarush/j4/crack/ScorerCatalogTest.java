package ua.com.javarush.j4.crack;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import static org.junit.jupiter.api.Assertions.*;

class ScorerCatalogTest {

    private final ScorerCatalog catalog = ScorerCatalog.withDefaults();

    @Test
    void buildsDictionaryScorer() {
        assertTrue(catalog.create("dictionary", Languages.ENGLISH) instanceof DictionaryScorer);
    }

    @Test
    void buildsFrequencyScorer() {
        assertTrue(catalog.create("frequency", Languages.ENGLISH) instanceof FrequencyScorer);
    }

    @Test
    void unknownScorerIsRejected() {
        assertThrows(InvalidArgumentsException.class, () -> catalog.create("magic", Languages.ENGLISH));
    }
}
