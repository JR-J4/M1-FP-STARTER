package ua.com.javarush.j4.controller;

import ua.com.javarush.j4.constant.Action;
import ua.com.javarush.j4.constant.Language;

public class LanguageController {
    private final static String LANGUAGE_MENU_ENGLISH = Language.ENGLISH.getCode()
            + " - for next dialog on English";
    private final static String LANGUAGE_MENU_UKRAINIAN = Language.UKRAINIAN.getCode()
            + " - для наступного діалогу українською мовою";
    private final static String INCORRECT_NUMBER_MESSAGE_ENGLISH = "You entered incorrect number. Please try again";
    private final static String INCORRECT_NUMBER_MESSAGE_UKRAINIAN = "You entered incorrect number. Please try again";
    private final static String ALPHABET_MENU_ENGLISH = Language.ENGLISH.getCode() + " - for work with English text\n"
            + Language.UKRAINIAN.getCode() + " - for working with Ukrainian text";
    private final static String ALPHABET_MENU_UKRAINIAN = Language.ENGLISH.getCode()
            + " - для роботи з англiйським текстом\n" + Language.UKRAINIAN.getCode()
            + " - для роботи з українським текстом";
    private final static String PATH_MENU_ENGLISH = "Enter the path to the file";
    private final static String PATH_MENU_UKRAINIAN = "Введіть шлях до файлу";
    private final static String ACTION_MENU_ENGLISH = "Please Enter:";
    private final static String ACTION_MENU_UKRAINIAN = "Будьласка введіть:";
    private final static String KEY_MENU_ENGLISH = "Please enter the key";
    private final static String KEY_MENU_UKRAINIAN = "Будьласка введіть ключ";


    public void showLanguageMenu() {
        System.out.println(LANGUAGE_MENU_ENGLISH);
        System.out.println(LANGUAGE_MENU_UKRAINIAN);
    }

    public void showActionErrorMessage(Language language) {
        switch (language) {
            case ENGLISH -> System.out.println(INCORRECT_NUMBER_MESSAGE_ENGLISH);
            case UKRAINIAN -> System.out.println(INCORRECT_NUMBER_MESSAGE_UKRAINIAN);
        }
    }

    public void showPathMenu(Language language) {
        switch (language) {
            case ENGLISH -> System.out.println(PATH_MENU_ENGLISH);
            case UKRAINIAN -> System.out.println(PATH_MENU_UKRAINIAN);
        }
    }

    public void showActionMenu(Language language) {
        switch (language) {
            case ENGLISH -> System.out.println(ACTION_MENU_ENGLISH);
            case UKRAINIAN -> System.out.println(ACTION_MENU_UKRAINIAN);
        }
        for (Action action : Action.values()) {
            System.out.println(action.getCode() + " - " + action.getText(language));
        }
    }

    public void showKeyMenu(Language language) {
        switch (language) {
            case ENGLISH -> System.out.println(KEY_MENU_ENGLISH);
            case UKRAINIAN -> System.out.println(KEY_MENU_UKRAINIAN);
        }
    }
}
