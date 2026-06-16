package ua.com.javarush.j4;

 import java.nio.file.Path;
 import java.util.Objects;

public class RunOptions {
       private final Command command;
       private final Path filePath;
       private final int key;

       public RunOptions(Command command, Path filePath, int key ) {
           this.command = command;
           this.filePath = filePath;
           this.key = key;
       }

       public Command getCommand() {
           return command;

       }

    public Path getFilePath() {
        return filePath;
    }
    public int getKey() {
           return key;
    }

    @Override
    public boolean equals(Object obj) {
           if(this == obj) return true;
           if (obj == null || getClass() != obj.getClass()) return false;
           RunOptions options = (RunOptions) obj;
           return key == options.key && command == options.command && Objects.equals(filePath,options.filePath);

    }
    @Override
    public int hashCode(){
           return Objects.hash(command, filePath, key);
    }

}
















