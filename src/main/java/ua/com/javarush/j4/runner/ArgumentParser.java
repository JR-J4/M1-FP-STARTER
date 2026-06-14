package ua.com.javarush.j4.runner;

import java.nio.file.Path;

public class ArgumentParser {
    public RunOptions parse(String[] args) {

        Command command = null;
        Integer key = null;
        Path path = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            Command parseCommand = Command.fromFlag(arg);
            if (parseCommand != null) {
                command = parseCommand;
                continue;
            }

            switch (arg) {
                case "-k" -> {
                    try {
                        key = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Key is not a number!", e);
                    }
                }
                case "-f" -> {
                    try {
                        path = Path.of(args[++i]);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Path is not a file!", e);
                    }
                }
                default ->  {
                    if (arg.startsWith("-")) {
                        throw new IllegalArgumentException("Unknown argument: " + arg);
                    }
                }
            }
        }
        if (command == null) {
            throw new IllegalArgumentException("Command is required!");
        }
        if (path == null) {
            throw new IllegalArgumentException("Path is required!");
        }
        if ((command == Command.ENCRYPT || command == Command.DECRYPT) && key == null) {
            throw new IllegalArgumentException("Key is required for this command!");
        }
        return new RunOptions(command, key, path);
    }
}