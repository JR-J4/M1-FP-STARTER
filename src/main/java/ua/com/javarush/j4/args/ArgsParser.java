package ua.com.javarush.j4.args;

import ua.com.javarush.j4.menu.Command;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ArgsParser {
    public static RunOptions parse(String[] args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Аргументи відсутні");
        }
        Command command = null;
        Integer key = null;
        Path path = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-e", "-d", "-bf" -> command = Command.getCommand(args[i]);
                case "-k" -> {
                    if (i + 1 < args.length) {

                        try {
                            key = Integer.parseInt(args[i + 1]);
                        }catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Ключ має бути цілим числом");
                        }
                        i++;
                    } else {
                        throw new IllegalArgumentException("Ключ відсутній");
                    }
                }
                case "-f" -> {
                    if (i + 1 < args.length) {
                        path = Paths.get(args[i + 1]);
                        i++;
                    }else {
                        throw new IllegalArgumentException("Відсутній шлях");
                    }
                }
                default -> throw new IllegalArgumentException("Невідома команда: " + args[i]);
            }

        }

        return new RunOptions(command, key, path);

    }
}
