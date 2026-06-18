package ua.com.javarush.j4;

import java.nio.file.Path;

/**
 * Розбирає опційно-стильові аргументи ({@code -e | -d | -b}, {@code -k <ціле>},
 * {@code -f <шлях>}) у будь-якому порядку. Невалідні набори кидають
 * {@link IllegalArgumentException}, який {@link Main} ловить і не пропускає назовні.
 */
public class ArgumentParser {

    public static Arguments parse(String[] args) {
        Command command = null;
        Integer key = null;
        Path file = null;

        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            Command parsed = Command.fromFlag(token);
            if (parsed != null) {
                command = parsed;
            } else if (token.equals("-k")) {
                key = Integer.parseInt(nextValue(args, ++i, "-k"));
            } else if (token.equals("-f")) {
                file = Path.of(nextValue(args, ++i, "-f"));
            } else {
                throw new IllegalArgumentException("Невідомий аргумент: " + token);
            }
        }

        validate(command, key, file);
        return new Arguments(command, key, file);
    }

    private static String nextValue(String[] args, int index, String flag) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Пропущено значення для " + flag);
        }
        return args[index];
    }

    private static void validate(Command command, Integer key, Path file) {
        if (command == null) {
            throw new IllegalArgumentException("Потрібна команда: -e, -d або -b");
        }
        if (file == null) {
            throw new IllegalArgumentException("Потрібен шлях до файлу: -f");
        }
        if (command != Command.BRUTE_FORCE && key == null) {
            throw new IllegalArgumentException("Для -e і -d потрібен ключ: -k");
        }
    }
}
