package ua.com.javarush.j4.cli;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import ua.com.javarush.j4.app.CryptoRequest;
import ua.com.javarush.j4.app.CryptoService;
import ua.com.javarush.j4.app.Operation;
import ua.com.javarush.j4.app.batch.BatchReport;
import ua.com.javarush.j4.concurrent.ParallelPolicy;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command-line front end. The legacy contract (-e/-d/-b, -k, -f) is preserved;
 * new optional flags (--cipher, --keyword, --alphabet) are purely additive.
 */
@Command(name = "cryptanalyzer", mixinStandardHelpOptions = true, version = "cryptanalyzer 2.0",
        description = "Caesar-family cipher tool: encrypt, decrypt, or brute-force a text file.")
public final class CryptoCli implements Callable<Integer> {

    /** Exactly one command must be chosen. */
    @ArgGroup(multiplicity = "1")
    private CommandSelection command;

    static final class CommandSelection {
        @Option(names = "-e", description = "Encrypt") boolean encrypt;
        @Option(names = "-d", description = "Decrypt") boolean decrypt;
        @Option(names = "-b", description = "Brute-force (no key)") boolean brute;
    }

    @Option(names = "-k", description = "Key (required for -e/-d with the caesar cipher)")
    private Integer key;

    @Option(names = "-f", required = true,
            description = "Input file path. Repeatable; a quoted glob such as '*.txt' is expanded.")
    private List<Path> files;

    @Spec
    private CommandSpec spec;

    @Option(names = {"-c", "--cipher"}, defaultValue = "caesar",
            description = "Cipher: caesar, rot13, atbash, vigenere")
    private String cipher;

    @Option(names = "--keyword", description = "Keyword for the vigenere cipher")
    private String keyword;

    @Option(names = {"-a", "--alphabet"}, defaultValue = "default",
            description = "Alphabet/language: en, ua, ru, auto")
    private String alphabet;

    @Option(names = {"-s", "--scorer"}, defaultValue = "dictionary",
            description = "Brute-force fitness scorer: dictionary, frequency")
    private String scorer;

    @Option(names = "--threads", defaultValue = "0",
            description = "Worker threads (0 = every available core, 1 = fully sequential)")
    private int threads;

    @Override
    public Integer call() throws Exception {
        Operation operation = selectedOperation();
        List<Path> resolved = new PathExpander().expand(files);
        CryptoRequest template = new CryptoRequest(
                operation, resolved.get(0), key, cipher, keyword, alphabet, scorer);

        try (CryptoService service = new CryptoService(ParallelPolicy.of(threads))) {
            if (resolved.size() == 1) {
                service.execute(template);
                return 0;
            }
            BatchReport report = service.executeAll(template, resolved);
            report.printTo(spec.commandLine().getOut());
            return report.anyFailed() ? 1 : 0;
        }
    }

    private Operation selectedOperation() {
        if (command.brute) {
            return Operation.BRUTE_FORCE;
        }
        return command.encrypt ? Operation.ENCRYPT : Operation.DECRYPT;
    }

    /** Parses and executes; never throws — errors are reported and a non-zero code returned. */
    public int run(String[] args) {
        return new CommandLine(this)
                .setExecutionExceptionHandler((ex, cmd, parseResult) -> {
                    cmd.getErr().println("Error: " + ex.getMessage());
                    return 1;
                })
                .execute(args);
    }
}
