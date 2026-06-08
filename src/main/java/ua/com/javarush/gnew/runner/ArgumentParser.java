package ua.com.javarush.gnew.runner;

import java.nio.file.Path;

public class ArgumentParser {

  public RunOptions parse(String[] args) {
    Command command = null;
    Integer key = null;
    Path path = null;

    int i = 0;
    while (i < args.length) {
      String arg = args[i];

      Command parsedCommand = Command.fromFlag(arg);
      if (parsedCommand != null) {
        command = parsedCommand;
        i++;
        continue;
      }

      switch (arg) {
        case "-k" -> {
          if (i + 1 >= args.length) {
            throw new IllegalArgumentException("Missing value after -k");
          }
          try {
            key = Integer.parseInt(args[i + 1]);
          } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Key must be a number a.e. -k 5", e);
          }
          i += 2;
        }
        case "-f" -> {
          if (i + 1 >= args.length) {
            throw new IllegalArgumentException("Missing value after -f");
          }
          path = Path.of(args[i + 1]);
          i += 2;
        }
        default -> throw new IllegalArgumentException("Unknown argument: " + arg);
      }
    }

    if (command == null) {
      throw new IllegalArgumentException("Missing command (-e, -d, or -bf)");
    }
    if (path == null) {
      throw new IllegalArgumentException("Missing -f <path>");
    }
    if ((command == Command.ENCRYPT || command == Command.DECRYPT) && key == null) {
      throw new IllegalArgumentException("Missing -k <key>");
    }

    return new RunOptions(command, key, path);
  }
}
