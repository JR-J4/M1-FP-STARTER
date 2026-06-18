package ua.com.javarush.j4.crack;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import static org.junit.jupiter.api.Assertions.*;

class LanguagesTest {

    @Test
    void resolvesKnownCodes() {
        assertEquals(Languages.ENGLISH, Languages.byCode("en").orElseThrow());
        assertEquals(Languages.UKRAINIAN, Languages.byCode("UA").orElseThrow());
        assertEquals(Languages.RUSSIAN, Languages.byCode("russian").orElseThrow());
    }

    @Test
    void defaultAndAutoMeanNoSpecificLanguage() {
        assertTrue(Languages.byCode("default").isEmpty());
        assertTrue(Languages.byCode("auto").isEmpty());
    }

    @Test
    void unknownCodeIsRejected() {
        assertThrows(InvalidArgumentsException.class, () -> Languages.byCode("klingon"));
    }
}
