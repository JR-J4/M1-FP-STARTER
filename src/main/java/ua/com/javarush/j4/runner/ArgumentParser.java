package ua.com.javarush.j4.runner;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ArgumentParser {

    public RunOptions parse(String[] args) {

        Command command = null;
        Integer key = null;
        Path path = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            // Перевіряємо через енам, чи є цей аргумент головною командою
            Command parsedCommand = Command.fromFlag(arg);

            if (parsedCommand != null) {
                command = parsedCommand;
                continue;
            }

            // Якщо те, що було введено не є головною командою, треба перевірити інші прапорці (-k, -f)
            // треба спробувати через if, але поки вагаюся
            switch (arg) {
                case "-k" -> {
                    if (i + 1 < args.length) {
                        try {
                            // може варто змінити ?? (++i) одночасно: збільшує індекс 'i' на 1 (перестрибує на саме число)
                            // а потім зчитує це число з масиву та перетворює строку в int через Integer.parseInt.
                            key = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            // Якщо користувач написав, наприклад, "-k п'ять" замість числа, кидаємо помилку
                            throw new IllegalArgumentException("Ключ після -k має бути цілим числом!");
                        }
                    } else {
                        throw new IllegalArgumentException("Пропущено значення ключа після -k");
                    }
                }
                case "-f" -> {
                    if (i + 1 < args.length) {
                        path = Paths.get(args[++i]);
                    } else {
                        throw new IllegalArgumentException("Пропущено шлях до файлу після -f");
                    }
                }
                default -> throw new IllegalArgumentException("Невідомий аргумент: " + arg);
            }
        }

        // викликаємо метод валідації, щоб перевірити, чи користувач не забув ввести щось важливе
        validate(command, path, key);

        return new RunOptions(command, key, path);
    }

    private void validate(Command command, Path path, Integer key) {
        if (command == null) {
            throw new IllegalArgumentException("Помилка: Не вказано команду дії (-e, -d або -bf)!");
        }
        if (path == null) {
            throw new IllegalArgumentException("Помилка: Не вказано шлях до файлу (-f)!");
        }
        if (command != Command.BRUTEFORCE && key == null) {
            throw new IllegalArgumentException("Помилка: Для цього режиму необхідно вказати ключ (-k)!");
        }
    }
}
