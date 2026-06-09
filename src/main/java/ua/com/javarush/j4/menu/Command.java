package ua.com.javarush.j4.menu;

public enum Command {
    ENCRYPT("-e"),
    DECRYPT("-d"),
    BRUTE_FORCE("-bf");

    private final String flag;
    Command(String flag) {
        this.flag = flag;
    }
    public String getFlag() {
        return flag;
    }
    public static Command getCommand(String flag) {
        for (Command command : Command.values()) {
            if (command.getFlag().equals(flag)) {
                return command;
            }
        }
        throw new IllegalArgumentException("Невідома команда");
    }
}
