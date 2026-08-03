package ua.com.javarush.j4.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.com.javarush.j4.error.InvalidArgumentsException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathExpanderTest {

    @Test
    void aLiteralPathPassesThroughUntouched(@TempDir Path dir) {
        Path missing = dir.resolve("nope.txt");

        assertEquals(List.of(missing), new PathExpander().expand(List.of(missing)));
    }

    @Test
    void aGlobExpandsInSortedOrder(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("b.txt"), "b", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("a.txt"), "a", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("c.md"), "c", StandardCharsets.UTF_8);

        List<Path> expanded = new PathExpander().expand(List.of(dir.resolve("*.txt")));

        assertEquals(List.of(dir.resolve("a.txt"), dir.resolve("b.txt")), expanded);
    }

    @Test
    void repeatedEntriesAreConcatenated(@TempDir Path dir) throws IOException {
        Path one = dir.resolve("one.txt");
        Path two = dir.resolve("two.txt");
        Files.writeString(one, "1", StandardCharsets.UTF_8);
        Files.writeString(two, "2", StandardCharsets.UTF_8);

        assertEquals(List.of(one, two), new PathExpander().expand(List.of(one, two)));
    }

    @Test
    void aGlobMatchingNothingIsRejected(@TempDir Path dir) {
        InvalidArgumentsException thrown = assertThrows(InvalidArgumentsException.class,
                () -> new PathExpander().expand(List.of(dir.resolve("*.nomatch"))));

        assertTrue(thrown.getMessage().contains("*.nomatch"));
    }
}
