package ua.com.javarush.j4;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ArgsParser {
    public static RunOptions parse(String[] args) {

        Command command = null;

        Integer key = null;
        Path path = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-e", "-d", "-bf" -> command = Command.getCommand(args[i]);
                case "-k" -> {
                    if (i + 1 < args.length) {
                        key = Integer.parseInt(args[i + 1]);
                        i++;
                    } else {
                        throw new IllegalArgumentException("Key missing");
                    }
                }
                case "-f" -> {
                    if (i + 1 < args.length) {
                        path = Paths.get(args[i + 1]);
                        i++;
                    }else {
                        throw new IllegalArgumentException("Path missing");
                    }
                }
                default -> throw new IllegalArgumentException("Unknown command: " + args[i]);
            }

        }
        return new RunOptions(command, key, path);

    }
}
