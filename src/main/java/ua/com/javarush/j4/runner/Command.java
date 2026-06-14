package ua.com.javarush.j4.runner;

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

    public static Command fromFlag(String flag) {
        Command[] values = Command.values();
        for (Command command : values) {
            if (command.flag.equalsIgnoreCase(flag)) {
                return command;
            }
        }
        return null;
    }

    public boolean requiresKey() {
        return this == BRUTE_FORCE;
    }
}