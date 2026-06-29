package ua.com.javarush.j4.support;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamedRegistryTest {

    @Test
    void storesAndReturnsTheRegisteredValueWithoutCasting() {
        // T = String here; the same class works for any T.
        NamedRegistry<String> registry = new NamedRegistry<>("greeting");
        registry.register("hello", "world");

        String value = registry.get("hello"); // no cast needed: get() returns T
        assertEquals("world", value);
    }

    @Test
    void lookupIsCaseInsensitive() {
        NamedRegistry<Integer> registry = new NamedRegistry<>("number");
        registry.register("ONE", 1);
        assertEquals(1, registry.get("one"));
    }

    @Test
    void unknownNameIsRejectedWithAListOfValidNames() {
        NamedRegistry<Integer> registry = new NamedRegistry<>("number");
        registry.register("one", 1);
        registry.register("two", 2);

        InvalidArgumentsException ex =
                assertThrows(InvalidArgumentsException.class, () -> registry.get("three"));
        assertTrue(ex.getMessage().contains("one"));
        assertTrue(ex.getMessage().contains("two"));
    }
}
