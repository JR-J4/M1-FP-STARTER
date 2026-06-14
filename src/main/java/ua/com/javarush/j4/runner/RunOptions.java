package ua.com.javarush.j4.runner;

import java.nio.file.Path;

public record RunOptions(Command command, Integer key, Path path) {
}