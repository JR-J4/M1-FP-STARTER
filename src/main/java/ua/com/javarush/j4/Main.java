package ua.com.javarush.j4;

import ua.com.javarush.j4.cli.CryptoCli;

/**
 * Entry point. Delegates to the picocli front end, which parses arguments and
 * runs the requested command. Never propagates exceptions for bad input.
 */
public class Main {
    public static void main(String[] args) {
        new CryptoCli().run(args);
    }
}
