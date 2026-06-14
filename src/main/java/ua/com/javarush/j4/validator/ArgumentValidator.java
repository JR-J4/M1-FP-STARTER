package ua.com.javarush.j4.validator;

import ua.com.javarush.j4.runner.Command;

import java.nio.file.Path;

public class ArgumentValidator {
    public void validate(Command command, Integer key, Path path) {

        if (command == null) {
            throw new IllegalArgumentException("Command is required!");
        }
        if (path == null) {
            throw new IllegalArgumentException("Path is required!");
        }

        if ((command == Command.ENCRYPT || command == Command.DECRYPT) && key == null) {
            throw new IllegalArgumentException("Key is required for this command!");
        }
    }
}