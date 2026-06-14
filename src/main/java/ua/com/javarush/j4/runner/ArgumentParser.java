package ua.com.javarush.j4.runner;

import ua.com.javarush.j4.validator.ArgumentValidator;

import java.nio.file.Path;

public class ArgumentParser {

    private final ArgumentValidator validator = new ArgumentValidator();

    public RunOptions parse(String[] args) {

        Command command = null;
        Integer key = null;
        Path path = null;

        int i = 0;

        while (i < args.length) {
            String arg = args[i];

            Command parseCommand = Command.fromFlag(arg);
            if (parseCommand != null) {
                command = parseCommand;
                i++;
                continue;
            }

            switch (arg) {
                case "-k" -> {
                    try {
                        key = Integer.parseInt(args[i + 1]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Key is not a number!", e);
                    }
                    i += 2;
                }
                case "-f" -> {
                    try {
                        path = Path.of(args[i + 1]);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Path is not a file!", e);
                    }
                    i += 2;
                }
                default -> {
                    if (arg.startsWith("-")) {
                        throw new IllegalArgumentException("Unknown argument: " + arg);
                    }
                    i++;
                }
            }
        }
        validator.validate(command, key, path);

        return new RunOptions(command, key, path);
    }
}