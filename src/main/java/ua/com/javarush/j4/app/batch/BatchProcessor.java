package ua.com.javarush.j4.app.batch;

import ua.com.javarush.j4.app.command.CryptoCommand;
import ua.com.javarush.j4.concurrent.TaskExecutor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Runs one command per input file across the executor.
 *
 * <p>Each task captures its own failure instead of throwing, so a single unreadable file
 * never aborts the batch. Because {@link TaskExecutor#invokeAll} preserves submission
 * order, outcomes come back in input order however the work interleaves.
 */
public final class BatchProcessor {
    private final TaskExecutor executor;

    public BatchProcessor(TaskExecutor executor) {
        this.executor = executor;
    }

    public BatchReport process(List<Path> inputs, Function<Path, CryptoCommand> commandFactory) {
        List<Callable<FileOutcome>> tasks = new ArrayList<>(inputs.size());
        for (Path input : inputs) {
            tasks.add(() -> runOne(input, commandFactory));
        }
        return new BatchReport(executor.invokeAll(tasks));
    }

    private static FileOutcome runOne(Path input, Function<Path, CryptoCommand> commandFactory) {
        try {
            return FileOutcome.success(input, commandFactory.apply(input).execute());
        } catch (Exception e) {
            return FileOutcome.failed(input, e);
        }
    }
}
