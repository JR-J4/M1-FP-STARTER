package ua.com.javarush.j4.controller;

import ua.com.javarush.j4.constant.Action;
import ua.com.javarush.j4.constant.Comand;
import ua.com.javarush.j4.exception.InvalidFlagException;
import ua.com.javarush.j4.service.BruteForceService;
import ua.com.javarush.j4.service.CryptoService;

public class CommandLineController {
    private CryptoService cryptoService;
    private BruteForceService bruteForceService;

    public CommandLineController() {
        this.cryptoService = new CryptoService();
        this.bruteForceService = new BruteForceService();
    }

    public void start(String[] args) {
        Action action = null;
        String path = null;
        String key = null;

        for (int i = 0; i < args.length; i++) {
            Comand comand = Comand.fromFlag(args[i]);
            Action act = Action.fromFlag(args[i]);
            if (act != null) {
                action = act;
            } else if (comand == Comand.KEY) {
                key = args[i + 1];
                i++;
            } else if (comand == Comand.PATH) {
                path = args[i + 1];
                i++;
            } else {
                throw new InvalidFlagException("Unknown flag");
            }
        }

        performAction(path, key, action);
    }

    private void performAction(String path, String key, Action action) {
        switch (action) {
            case ENCRYPT -> {
                cryptoService.execute(path, key, true);
            }
            case DECRYPT -> {
                cryptoService.execute(path, key, false);
            }
            case BRUTE_FORCE -> bruteForceService.execute(path);
        }
    }


}
