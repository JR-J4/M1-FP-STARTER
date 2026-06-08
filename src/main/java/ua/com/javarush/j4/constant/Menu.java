package ua.com.javarush.j4.constant;

public enum Menu {

    LANGUAGE(Language.ENGLISH.getCode() + " - for next dialog on English",
            Language.UKRAINIAN.getCode() + " - для наступного діалогу українською мовою"),

    ACTION_ERROR("You entered incorrect number. Please try again",
            "You entered incorrect number. Please try again"),

    ALPHABET(Language.ENGLISH.getCode() + " - for work with English text\n"
            + Language.UKRAINIAN.getCode() + " - for working with Ukrainian text",
            Language.ENGLISH.getCode()
                    + " - для роботи з англiйським текстом\n" + Language.UKRAINIAN.getCode()
                    + " - для роботи з українським текстом"),
    PATH("Enter the path to the file",
            "Введіть шлях до файлу"),

    ACTION("Please Enter:", "Будьласка введіть:"),


    KEY("Please enter the key", "Будьласка введіть ключ");

    private final String englishText;
    private final String ukrainianText;

    Menu(String englishText, String ukrainianText) {
        this.englishText = englishText;
        this.ukrainianText = ukrainianText;
    }

    public String getText(Language language) {
        if (language == Language.UKRAINIAN) {
            return ukrainianText;
        } else {
            return englishText;
        }
    }
}
