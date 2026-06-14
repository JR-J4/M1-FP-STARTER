package ua.com.javarush.j4.runner;

import java.nio.file.Path;

public class ArgumentParser {
    public RunOptions parse(String[] args) {

        Command command = null;
        int key = 0;
        Path path = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            Command parsedCommand = Command.fromFlag(arg);

            if (parsedCommand != null) {
                command = parsedCommand;
                continue;
            }

            switch (arg) {
                case "-k" -> {
                    try{
                        key = Integer.parseInt(args[i + 1]);

                        //потрібно обробити по-іншому, є інші винятки 1:29
                    }catch(NumberFormatException e){
                        throw new IllegalArgumentException("Key must be a number a.e. -k 5", e);

                    }

                }

                //НАПИСАТИ TRY CATCH
                case "-f" -> {
                   path = Path.of(args[i + 1]);

                }
            }
        }
        return new RunOptions(command, key, path);
    }

}
