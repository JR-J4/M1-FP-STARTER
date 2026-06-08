package ua.com.javarush.j4.constant;

public enum Action {
    ENCRYPT(1, "ENCRYPT text", "для ШИФРУВАННЯ тексту", "-e"),
    DECRYPT(2, "DECRYPT text", "для РОЗШИФРУВАННЯ тексту", "-d"),
    BRUTE_FORCE(3, "BRUTE FORCE text", "для ПЕРЕБОРУ (BRUTE FORCE) тексту", "-bf"),
    EXIT(0, "EXIT from app", "для ВИХОДУ з програми", "-q");

    private final int code;
    private final String flag;
    private final String englishText;
    private final String ukrainianText;

    Action(int code, String englishText, String ukrainianText, String flag) {
        this.code = code;
        this.englishText = englishText;
        this.ukrainianText = ukrainianText;
        this.flag = flag;
    }

    public int getCode() {
        return code;
    }

    public String getText(Language language) {
        if (language == Language.UKRAINIAN) {
            return ukrainianText;
        } else {
            return englishText;
        }
    }

    public static Action fromCode(int code) {
        for (Action action : values()) {
            if (action.code == code) {
                return action;
            }
        }
        return null;
    }

    public static Action fromFlag(String flag) {
        for (Action action : values()) {
            if (action.flag.equals(flag)) {
                return action;
            }
        }
        return null;
    }
}
