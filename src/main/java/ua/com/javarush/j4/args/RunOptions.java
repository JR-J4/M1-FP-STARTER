package ua.com.javarush.j4.args;

import ua.com.javarush.j4.menu.Command;

import java.nio.file.Path;

public record RunOptions(Command command, Integer key , Path path) {
}
