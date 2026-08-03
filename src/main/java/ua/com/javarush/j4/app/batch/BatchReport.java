package ua.com.javarush.j4.app.batch;

import java.io.PrintWriter;
import java.util.List;

/** Every outcome of a batch run, in input order. */
public record BatchReport(List<FileOutcome> outcomes) {

    public BatchReport {
        outcomes = List.copyOf(outcomes);
    }

    public int succeeded() {
        return (int) outcomes.stream().filter(FileOutcome::succeeded).count();
    }

    public int failed() {
        return outcomes.size() - succeeded();
    }

    public boolean anyFailed() {
        return failed() > 0;
    }

    /** Prints one line per file in input order, then a summary. */
    public void printTo(PrintWriter out) {
        for (FileOutcome outcome : outcomes) {
            if (outcome.succeeded()) {
                out.println("  OK      " + outcome.input() + " -> " + outcome.output());
            } else {
                out.println("  FAILED  " + outcome.input() + ": " + outcome.failure().getMessage());
            }
        }
        out.println(succeeded() + " succeeded, " + failed() + " failed");
        out.flush();
    }
}
