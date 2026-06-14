package ua.com.javarush.j4.runner;

public enum Command {
    ENCRYPT("-e"),
    DECRYPT("-d"),
    BRUTEFORCE("-bf");

    private final String flag;

    public String getFlag() {
        return flag;
    }

    Command(String flag) {
        this.flag = flag;
    }

    public static Command fromFlag(String flag){
        Command[] values = Command.values();
        for (Command command : values) {
            if(command.getFlag().equalsIgnoreCase(flag)){
                return command;
            }
        }

        return null;
    }
}
