package ua.com.javarush.j4.constant;

public enum Comand {
    KEY("-k"),
    PATH("-f");

    private final String flag;

    Comand(String flag) {
        this.flag = flag;
    }

    public static Comand fromFlag(String flag) {
        for (Comand comand : values()) {
            if (comand.flag.equals(flag)) {
                return comand;
            }
        }
        return null;
    }
}
