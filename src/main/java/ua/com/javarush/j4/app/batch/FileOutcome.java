package ua.com.javarush.j4.app.batch;

import java.nio.file.Path;

/** What happened to one file in a batch: an output path, or the failure that stopped it. */
public record FileOutcome(Path input, Path output, Throwable failure) {

    public static FileOutcome success(Path input, Path output) {
        return new FileOutcome(input, output, null);
    }

    public static FileOutcome failed(Path input, Throwable failure) {
        return new FileOutcome(input, null, failure);
    }

    public boolean succeeded() {
        return failure == null;
    }
}
