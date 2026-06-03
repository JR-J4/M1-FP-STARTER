package ua.com.javarush.j4;

import ua.com.javarush.j4.runner.ArgumentParser;
import ua.com.javarush.j4.runner.Command;

/**
 * Точка входу криптоаналізатора шифру Цезаря.
 *
 * <p>Реалізацію та структуру класів обирай самостійно. Контракт CLI і поведінки
 * визначений у {@code MainTest} — зеленій тести.
 */
public class Main {
    public static void main(String[] args) {
        ArgumentParser parser = new ArgumentParser();

        Command command = Command.fromFlag("-e");

        for (String arg : args) {
            System.out.println(Command.fromFlag(arg));

        }



//        for (String arg : args) {
//            System.out.println(arg);
//
//        }
        // TODO: реалізуй CLI шифру Цезаря. Дивись MainTest.
    }
}
