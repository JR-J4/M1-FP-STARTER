package ua.com.javarush.j4.cli;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ua.com.javarush.j4.app.CryptoRequest;
import ua.com.javarush.j4.app.CryptoService;
import ua.com.javarush.j4.app.Operation;
import ua.com.javarush.j4.cipher.CipherSpec;

import java.nio.file.Path;
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

    @Option(names = "-f", required = true, description = "Input file path")
    private Path file;

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

    @Override
    public Integer call() throws Exception {
        Operation operation = selectedOperation();
        new CryptoService().execute(new CryptoRequest(
                operation, file, new CipherSpec(cipher, key, keyword), alphabet, scorer));
        return 0;
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
