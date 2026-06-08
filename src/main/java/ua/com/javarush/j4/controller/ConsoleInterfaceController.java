package ua.com.javarush.j4.controller;

import ua.com.javarush.j4.constant.Action;
import ua.com.javarush.j4.constant.Language;
import ua.com.javarush.j4.service.BruteForceService;
import ua.com.javarush.j4.service.CryptoService;
import ua.com.javarush.j4.validator.Validator;

import java.util.Scanner;

public class ConsoleInterfaceController {

    private CryptoService cryptoService;
    private BruteForceService bruteForceService;
    private Scanner scanner;
    private Validator validator;
    private LanguageController languageController;

    private Language language = null;


    public ConsoleInterfaceController() {
        this.cryptoService = new CryptoService();
        this.bruteForceService = new BruteForceService();
        this.scanner = new Scanner(System.in);
        this.validator = new Validator();
        this.languageController = new LanguageController();
    }

    public void start() {
        while (true) {
            setUpLanguage();
            Action action = getAction();
            if (action == Action.EXIT) {
                break;
            }
            String stringPath = getPathForCryptedFile();
            performAction(stringPath, action);
        }
    }

    private void setUpLanguage() {
        while (language == null) {
            languageController.showLanguageMenu();
            String userChoice = scanner.nextLine();
            int numericUserChoice = validator.validateKey(userChoice);
            language = Language.fromCode(numericUserChoice);
            if (language == null) {
                languageController.showActionErrorMessage(Language.ENGLISH);
            }
        }
    }

    private Action getAction() {
        Action action = null;
        while (action == null) {
            languageController.showActionMenu(language);
            String userChoice = scanner.nextLine();
            int numericUserChoice = validator.validateKey(userChoice);
            action = Action.fromCode(numericUserChoice);
            if (action == null) {
                languageController.showActionErrorMessage(language);
            }

        }
        return action;
    }

    private String getPathForCryptedFile() {
        String path = null;
        while (path == null) {
            languageController.showPathMenu(language);
            path = scanner.nextLine();
        }
        return path;
    }

    private void performAction(String path, Action action) {
        switch (action) {
            case ENCRYPT -> {
                String key = getKey();
                cryptoService.execute(path, key, true);
            }
            case DECRYPT -> {
                String key = getKey();
                cryptoService.execute(path, key, false);
            }
            case BRUTE_FORCE -> bruteForceService.execute(path);
        }
    }

    private String getKey() {
        String key = null;
        while (key == null) {
            languageController.showKeyMenu(language);
            key = scanner.nextLine();
        }
        return key;
    }
}
