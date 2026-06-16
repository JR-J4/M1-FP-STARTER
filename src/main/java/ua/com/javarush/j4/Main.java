package ua.com.javarush.j4;

/**
 * Точка входу криптоаналізатора шифру Цезаря.
 *
 * <p>Реалізацію та структуру класів обирай самостійно. Контракт CLI і поведінки
 * визначений у {@code MainTest} — зеленій тести.
 */
public class Main {
    public static void main(String[] args) {
        RunOptions options = ArgumentsParser.parse(args);
        if (options != null && options.getFilePath() != null) {
            CaesarCipher cipher = new CaesarCipher();
            cipher.execute(options);

        }
    }
}
