package ua.com.javarush.j4;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ArgsParser {
    public static RunOptions parse(String[] args) {
        Command command = null;
        int key = 0;
        Path path = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-e","-d","-bf"->{
                    command = Command.getCommand(args[i]);
                }
                case "-k"->{
                    key = Integer.parseInt(args[i+1]);
                    i++;
                }
                case "-f"->{
                    path = Paths.get(args[i+1]);
                    i++;
                }
            }

        }
        return  new RunOptions(command,key,path);
    }
}
