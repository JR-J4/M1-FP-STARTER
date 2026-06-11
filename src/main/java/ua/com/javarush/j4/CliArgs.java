package ua.com.javarush.j4;

import java.nio.file.Path;

/** Сommand-line arguments into a typed value object */
public class CliArgs {

    public enum Command { ENCRYPT, DECRYPT, BRUTE_FORCE }

    private Command command;
    private Integer key;
    private Path    filePath;

    private CliArgs() {}


    public static CliArgs parse(String[] args) {
        CliArgs result = new CliArgs();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-e"  -> setCommand(result, Command.ENCRYPT);
                case "-d"  -> setCommand(result, Command.DECRYPT);
                case "-bf" -> setCommand(result, Command.BRUTE_FORCE);
                case "-k"  -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value after -k");
                    }
                    String raw = args[++i];
                    try {
                        result.key = Integer.parseInt(raw);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Key must be an integer, got: " + raw);
                    }
                }
                case "-f"  -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value after -f");
                    }
                    result.filePath = Path.of(args[++i]);
                }
                default -> throw new IllegalArgumentException(
                        "Unknown flag: " + args[i]);
            }
        }

        validate(result);
        return result;
    }

    public Command getCommand()  { return command;  }
    public int     getKey()      { return key;       }
    public Path    getFilePath() { return filePath;  }

    private static void setCommand(CliArgs a, Command cmd) {
        if (a.command != null) {
            throw new IllegalArgumentException(
                    "Only one command flag allowed (-e, -d, or -bf)");
        }
        a.command = cmd;
    }

    private static void validate(CliArgs a) {
        if (a.command == null) {
            throw new IllegalArgumentException(
                    "No command specified. Use -e, -d, or -bf.");
        }
        if (a.filePath == null) {
            throw new IllegalArgumentException(
                    "No input file specified. Use -f <path>.");
        }
        if (a.command == Command.ENCRYPT || a.command == Command.DECRYPT) {
            if (a.key == null) {
                throw new IllegalArgumentException(
                        "Key (-k) is required for " + a.command + ".");
            }
        }
    }
}