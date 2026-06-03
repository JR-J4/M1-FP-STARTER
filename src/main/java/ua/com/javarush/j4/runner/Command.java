package ua.com.javarush.j4.runner;

public enum Command {
    ENCRYPT ("-e"),
    DECRYPT ("-d"),
    BRUTEFORCE ("-bf");

    private final String flag;

    Command(String flag) {
        this.flag = flag;
    }

    public String flag() {
        return flag;
    }

    public static Command fromFlag (String flag) {
        Command[] values = Command.values();
        for (Command command : values) {
            if (command.flag().equalsIgnoreCase(flag)) {
                return command;
            }
        }
        throw new IllegalArgumentException("Невідома команда: " + flag) ;
    }
}
