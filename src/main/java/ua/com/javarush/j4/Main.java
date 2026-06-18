package ua.com.javarush.j4;

/**
 * Entry point. Builds the object graph via the composition root and hands it to
 * the picocli front end, which parses arguments and runs the requested command.
 * Never propagates exceptions for bad input.
 */
public class Main {
    public static void main(String[] args) {
        new Composition().cli().run(args);
    }
}
