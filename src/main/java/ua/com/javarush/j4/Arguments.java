package ua.com.javarush.j4;

import java.nio.file.Path;

/**
 * Розібрані аргументи командного рядка. Для {@link Command#BRUTE_FORCE} ключ
 * не передається, тому {@code key} може бути {@code null}.
 */
public record Arguments(Command command, Integer key, Path file) {
}
