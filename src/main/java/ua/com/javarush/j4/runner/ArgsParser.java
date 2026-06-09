package ua.com.javarush.j4.runner;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ArgsParser {

    private Mode mode;
    private Integer key;
    private Path file;

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public Path getFile() {
        return file;
    }

    public void setFile(Path file) {
        this.file = file;
    }

    public static ArgsParser parse(String[] argv) {
        ArgsParser args = new ArgsParser();
        for (int i = 0; i < argv.length; i++) {
            switch (argv[i]) {
                case "-e"  -> args.setMode(Mode.ENCRYPT);
                case "-d"  -> args.setMode(Mode.DECRYPT);
                case "-bf" -> args.setMode(Mode.BRUTE_FORCE);
                case "-k"  -> args.setKey(Integer.parseInt(argv[++i]));
                case "-f"  -> args.setFile(Paths.get(argv[++i]));
                default    -> throw new IllegalArgumentException("Unknown flag: " + argv[i]);
            }
        }
        return args;
    }
}