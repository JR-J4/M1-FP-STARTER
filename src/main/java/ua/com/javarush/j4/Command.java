package ua.com.javarush.j4;

/**
 * Команда, яку обрав користувач: шифрування, розшифрування або перебір ключів.
 * Кожній команді відповідає свій прапорець командного рядка.
 */
public enum Command {
    ENCRYPT("-e"),
    DECRYPT("-d"),
    BRUTE_FORCE("-b");

    private final String flag;

    Command(String flag) {
        this.flag = flag;
    }

    /** Повертає команду за її прапорцем, або {@code null}, якщо такого прапорця немає. */
    public static Command fromFlag(String flag) {
        for (Command command : values()) {
            if (command.flag.equals(flag)) {
                return command;
            }
        }
        return null;
    }
}
