package ua.com.javarush.j4;

import ua.com.javarush.j4.engine.AppRunner;
import ua.com.javarush.j4.runner.ArgumentParser;
import ua.com.javarush.j4.runner.RunOptions;


public class Main {
    public static void main(String[] args) {

        try {
            ArgumentParser parser = new ArgumentParser();

            RunOptions options = parser.parse(args);

            System.out.println("Ваші аргументи успішно прийняті!");
            System.out.println("Режим: " + options.command());
            System.out.println("Файл: " + options.path().toAbsolutePath());
            System.out.println("Ключ: " + (options.key() != null ? options.key() : "не потрібен (брутфорс)"));

            AppRunner runner = new AppRunner();
            runner.run(options);

        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.out.println("  Шифрування:     -e -f <шлях> -k <ключ>");
            System.out.println("  Дешифрування:   -d -f <шлях> -k <ключ>");
            System.out.println("  Брутфорс:       -bf -f <шлях>");

        } catch (Exception e) {
            System.err.println("Сталася критична помилка програми : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
