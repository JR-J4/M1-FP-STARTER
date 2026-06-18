package ua.com.javarush.j4;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CompositionTest {

    @Test
    void buildsAFullyWiredCli() {
        assertNotNull(new Composition().cli(), "composition root should assemble the CLI");
        assertNotNull(new Composition().service(), "composition root should assemble the service");
    }
}
