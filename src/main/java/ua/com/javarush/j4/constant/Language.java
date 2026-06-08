package ua.com.javarush.j4.constant;

public enum Language {
    ENGLISH(1),
    UKRAINIAN(2);

    private final int code;

    Language(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Language fromCode(int code) {
        for (Language language : values()) {
            if (language.code == code) {
                return language;
            }
        }
        return null;
    }
}
