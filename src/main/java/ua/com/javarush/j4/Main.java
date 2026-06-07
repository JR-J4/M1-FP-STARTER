package ua.com.javarush.j4;

import ua.com.javarush.j4.runner.ArgumentParser;
import ua.com.javarush.j4.runner.Command;
import ua.com.javarush.j4.runner.RunOptions;

import java.sql.SQLOutput;

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
            ArgumentParser parser = new ArgumentParser();

            // Тут я буду передавати масив args у парсер і якщо користувач ввів щось не так,
            // парсер всередині себе викине помилку
            RunOptions options = parser.parse(args);

            // Якщо парсинг пройшов, то для краси виведемо в консоль, що ми успішно розпізнали команди
            System.out.println("Ваші аргументи успішно прийняті!");
            System.out.println("Режим: " + options.command());
            System.out.println("Файл: " + options.path().toAbsolutePath());
            System.out.println("Ключ: " + (options.key() != null ? options.key() : "не потрібен (брутфорс)"));

            // Тут напевно треба передати options у CipherService для виконання роботи (типу: cipherExecutionEngine.run(options));

        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            // Показуємо як правильно запускати програму. Може варто прибрати цей декор?! ще не вирішила
            System.out.println("  Шифрування:     -e -f <шлях> -k <ключ>");
            System.out.println("  Дешифрування:   -d -f <шлях> -k <ключ>");
            System.out.println("  Брутфорс:       -bf -f <шлях>");

            // Далі намагаюся перехопити все, що не врахувала
        } catch (Exception e) {
            System.err.println("Сталася критична помилка програми : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
