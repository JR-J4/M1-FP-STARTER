package ua.com.javarush.j4.runner;

import java.nio.file.Path;
import java.util.Objects;

// -e -k 5 -f "folder/input.txt"
public record RunOptions(Command command, Integer key, Path path) {

}


