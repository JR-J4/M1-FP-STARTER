package ua.com.javarush.j4.cli;

import ua.com.javarush.j4.error.InvalidArgumentsException;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns the raw {@code -f} values into concrete paths.
 *
 * <p>A literal path passes through untouched, including one that does not exist — that
 * failure belongs to the reader, which reports it properly. A value whose file name holds
 * {@code *} or {@code ?} is matched inside its parent directory and sorted, so batch
 * output order never depends on how the filesystem enumerates entries.
 */
public final class PathExpander {

    public List<Path> expand(List<Path> raw) {
        List<Path> expanded = new ArrayList<>();
        for (Path candidate : raw) {
            if (isGlob(candidate)) {
                expanded.addAll(matches(candidate));
            } else {
                expanded.add(candidate);
            }
        }
        return expanded;
    }

    private static boolean isGlob(Path candidate) {
        String name = candidate.getFileName().toString();
        return name.indexOf('*') >= 0 || name.indexOf('?') >= 0;
    }

    private static List<Path> matches(Path pattern) {
        Path directory = pattern.getParent() != null ? pattern.getParent() : Path.of(".");
        String glob = pattern.getFileName().toString();

        List<Path> found = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, glob)) {
            stream.forEach(found::add);
        } catch (IOException e) {
            throw new InvalidArgumentsException(
                    "Cannot expand pattern '" + pattern + "': " + e.getMessage());
        }
        if (found.isEmpty()) {
            throw new InvalidArgumentsException("Pattern '" + pattern + "' matched no files");
        }
        found.sort(Comparator.comparing(Path::toString));
        return found;
    }
}
