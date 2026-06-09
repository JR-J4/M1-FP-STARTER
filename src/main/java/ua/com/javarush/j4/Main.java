package ua.com.javarush.j4;

import ua.com.javarush.j4.args.ArgsParser;
import ua.com.javarush.j4.args.RunOptions;
import ua.com.javarush.j4.menu.MainMenu;


/**
 * Точка входу криптоаналізатора шифру Цезаря.
 *
 * <p>Реалізацію та структуру класів обирай самостійно. Контракт CLI і поведінки
 * визначений у {@code MainTest} — зеленій тести.
 */

public class Main {
    public static void main(String[] args) {
        // TODO: реалізуй CLI шифру Цезаря. Дивись MainTest.
       try {
           RunOptions options = ArgsParser.parse(args);
           MainMenu.manu(options);
       } catch (Exception e) {
           System.err.println("Помилка: " + e.getMessage());
       }
    }

}
