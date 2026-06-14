package ua.com.javarush.j4.runner;

import ua.com.javarush.j4.crypty.BruteForce;
import ua.com.javarush.j4.crypty.Cypher;
import ua.com.javarush.j4.file.FileManager;

import java.nio.file.Files;
import java.nio.file.Path;

public class AppRunner {
    public void run(RunOptions options) {
        Cypher cypher = new Cypher();
        BruteForce bruteForce = new BruteForce();

        try {
            ArgumentParser parser = new ArgumentParser();
            System.out.println("Ваші аргументи успішно прийняті!");
            System.out.println("Режим: " + options.command());
            System.out.println("Файл: " + options.path().toAbsolutePath());


            FileManager fileManager = new FileManager();
            String content = Files.readString(options.path());
            //Path outputPath;
            switch (options.command()) {
                case ENCRYPT -> {
                    Path newPath = createNewPathEncr(options.path(), "[ENCRYPTED]");
                    String encrypted = cypher.encrypt(content, options.key());
                    fileManager.write(newPath, encrypted);
                }
                case DECRYPT -> {
                  Path newPath = createNewPathDecr(options.path(), "[DECRYPTED]");
                    String decrypted = cypher.decrypt(content, options.key());
                    fileManager.write(newPath, decrypted);
                }
                case BRUTEFORCE -> {
                    int foundKey = bruteForce.decryptBf(content);
                    Path newPath = createNewPathBf(options.path(), "[BRUTE_FORCE]");
                    String bf = cypher.decrypt(content, foundKey);
                    fileManager.write(newPath, bf);

                }

            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.out.println("  Шифрування:     -e -f <шлях> -k <ключ>");
            System.out.println("  Дешифрування:   -d -f <шлях> -k <ключ>");
            System.out.println("  Брутфорс:       -bf -f <шлях>");

        } catch (Exception e) {

            //подумати
            System.err.println("Сталася помилка програми : " + e.getMessage());
        }
    }

       public static Path createNewPathEncr(Path originalPath, String newLabel){

           // Path path = options.path();
//            Path path = Path.of("/var/folder/input.txt")


            Path folder = originalPath.getParent();
            // Path outputFile = folder.resolve("output.txt");
            String nameOutputFile = "";

            try {
                String originalName = originalPath.getFileName().toString();
                int dotIndex = originalName.lastIndexOf('.');

                String name = originalName;
                String extension = "";

                // Відокремлюємо ім'я файлу від його розширення (наприклад, .txt)
                if (dotIndex != -1) {
                    name = originalName.substring(0, dotIndex);
                    extension = originalName.substring(dotIndex);
                }
                // Створюємо нову назву: додаємо слово "[ENCRYPTED]"
                nameOutputFile = name + newLabel + extension;
//                System.out.println(nameOutputFile);
            }catch (Exception e){
                System.out.println("Помилка при перейменуванні: " + e.getMessage());
            }

            return folder.resolve(nameOutputFile);


            //Path outputFile = folder.resolve(nameOutputFile);



    }

    public static Path createNewPathDecr(Path originalPath, String newLabel){

        String name = originalPath.getFileName().toString();
        String newName = "";
        if(name.contains("[ENCRYPTED]")) {
            newName = name.replace("[ENCRYPTED]", newLabel);


        }

        return originalPath.resolveSibling(newName);

    }

    public static Path createNewPathBf(Path originalPath, String newLabel){

        String name = originalPath.getFileName().toString();
        String newName = "";
        if(name.contains("[ENCRYPTED]")) {
            newName = name.replace("[ENCRYPTED]", newLabel);
        }

        return originalPath.resolveSibling(newName);
    }
}
