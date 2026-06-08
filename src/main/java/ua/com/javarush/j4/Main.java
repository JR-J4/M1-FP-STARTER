package ua.com.javarush.j4;

import ua.com.javarush.j4.controller.MainController;

/**
 * Точка входу криптоаналізатора шифру Цезаря.
 *
 * <p>Реалізацію та структуру класів обирай самостійно. Контракт CLI і поведінки
 * визначений у {@code MainTest} — зеленій тести.
 */
public class Main {
    public static void main(String[] args) {
        MainController mainController = new MainController();
        mainController.start(args);
    }
}