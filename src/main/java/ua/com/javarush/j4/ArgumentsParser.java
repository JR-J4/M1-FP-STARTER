package ua.com.javarush.j4;

import java.nio.file.Path;
import java.util.concurrent.Callable;


public class ArgumentsParser {

    public static RunOptions parse(String[] args) {
        Command command = null;
        Path filePath = null;
         Integer key = null;

          if(args == null || args.length == 0){
              System.out.println("Ошибка: Не переданы аргументы командной строки!");
              return null;
          }
     try {
         for (int i = 0; i < args.length; i++) {
             String arg = args[i];
             switch (arg) {
                 case "-e":
                     command = Command.ENCRYPT;
                     break;
                 case "-d":
                     command = Command.DECRYPT;
                     break;
                 case "-bf":
                     command = Command.BRUTE_FORCE;
                     break;
                 case "-k":
                     if (i + 1 < args.length) {
                         key = Integer.parseInt(args[i + 1]);
                         i++;
                     } else {
                         System.out.println("Ошибка: После флага -k отсутствует значение ключа!");
                          return null;
                     }
                     break;

                 case "-f":
                     if (i + 1 < args.length) {
                         filePath = Path.of(args[i + 1]);
                         i++;
                     } else {
                         System.out.println("Ошибка: После флага -f отсутствует путь к файлу!");
                         return null;
                     }
                     break;
                 default:
                     if (arg.startsWith("-")) {
                         System.out.println("Ошибка: Неизвестный флаг " + arg);
                         return null;
                     }

             }

         }
     } catch (NumberFormatException e){
         System.out.println(" Ошибка - ключ должен быть числом");
          return  null ;
        }
     if(command == null || filePath == null || key == null) {
         System.out.println("Ошибка: Не указана команда (-e/-d/-bf) или путь к файлу (-f)!  или (-k)!");
         return null;
     }

     return  new RunOptions(command, filePath, key);

    }

}



