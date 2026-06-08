package ua.com.javarush.j4.controller;

import ua.com.javarush.j4.exception.FileException;
import ua.com.javarush.j4.exception.KeyException;

public class MainController {
    private ConsoleInterfaceController uiController;
    private CommandLineController clController;

    public MainController() {
        this.uiController = new ConsoleInterfaceController();
        this.clController = new CommandLineController();
    }


    public void start(String[] args) {
        try {
            if (args.length == 0) {
                uiController.start();
            } else {
                clController.start(args);
            }
        } catch (FileException | KeyException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
