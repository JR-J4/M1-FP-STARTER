package ua.com.javarush.j4;

import ua.com.javarush.j4.crypty.Cypher;
import ua.com.javarush.j4.file.FileManager;
import ua.com.javarush.j4.language.EN_language;
import ua.com.javarush.j4.runner.AppRunner;
import ua.com.javarush.j4.runner.ArgumentParser;
import ua.com.javarush.j4.runner.Command;
import ua.com.javarush.j4.runner.RunOptions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Точка входу криптоаналізатора шифру Цезаря.
 *
 * <p>Реалізацію та структуру класів обирай самостійно. Контракт CLI і поведінки
 * визначений у {@code MainTest} — зеленій тести.
 */
public class Main {
    public static void main(String[] args) {

//        Cypher cypher = new Cypher();
//        FileManager fileManager = new FileManager();


        try {
            ArgumentParser parser = new ArgumentParser();
            RunOptions options = parser.parse(args);
            AppRunner runner = new AppRunner();
            runner.run(options);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
//перенести в інше місце
//            if(options.command() == Command.ENCRYPT){
//               String result = cypher.encrypt("текст файлу", options.key());
//            }else if(options.command() == Command.DECRYPT){
//                String result = cypher.decrypt("текст файлу", options.key());
//            }

//            String result = switch(options.command()){
//              //  case ENCRYPT -> cypher.encrypt(fileManager.read(options.path()), options.key());
//                case ENCRYPT -> cypher.encrypt("текст файлу", options.key());
//                case DECRYPT -> cypher.decrypt("текст файлу", options.key());
//                case BRUTEFORCE -> null;
//            };


//            Path path = options.path();
//            Path path = Path.of("/var/folder/input.txt")
//
//            Path folder = path.getParent();
//          // Path outputFile = folder.resolve("output.txt");
//            String nameOutputFile = "";
//
//            try {
//                String originalName = path.getFileName().toString();
//                int dotIndex = originalName.lastIndexOf('.');
//
//                String name = originalName;
//                String extension = "";
//
//                // Відокремлюємо ім'я файлу від його розширення (наприклад, .txt)
//                if (dotIndex != -1) {
//                    name = originalName.substring(0, dotIndex);
//                    extension = originalName.substring(dotIndex);
//                }
//                // Створюємо нову назву: додаємо слово "[ENCRYPTED]"
//                nameOutputFile = name + "[ENCRYPTED]" + extension;
//                System.out.println(nameOutputFile);
//            }catch (Exception e){
//                System.out.println("Помилка при перейменуванні: " + e.getMessage());
//            }
//
//
//            Path outputFile = folder.resolve(nameOutputFile);
//            System.out.println(outputFile.toString());
//
//            fileManager.read(path);
//
//            fileManager.write(outputFile, "BCD"); //має прийти зашифр файл
//
//            String read = fileManager.read(path);
//
//
//        }catch (Exception e){
//
//        }

            //System.out.println(EN_language.alphabetToArray());
//        System.out.println(EN_language.getAlphabetEn());
//
//        // System.out.println(2 %52 );
//
//
//       System.out.println(cypher.encrypt("ABC DE",2));
//        System.out.println("ABC DE");
//      //  System.out.println(cypher.encrypt("A",1));
//
//       //System.out.println(cypher.encrypt("AB CDEFG HIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz", 2));
//        System.out.println("AB CDEFG HIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
//       System.out.println(cypher.decrypt("AB CDEFG HIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz", 2));
            //System.out.println(cypher.encrypt("hellohellohelhellohellohelhellohellohelhellohellohelhello", 2));
            //System.out.println(cypher.encrypt("A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z", 2));

            // Path pathOriginal = Paths.get("D:\\Nataliya\\JavaRush\\JavaProjects\\M1-FP-NataB-REAL\\src\\main\\resources\\input.txt");


            //System.out.println(runOptions);


            // TODO: реалізуй CLI шифру Цезаря. Дивись MainTest.

    }
}
