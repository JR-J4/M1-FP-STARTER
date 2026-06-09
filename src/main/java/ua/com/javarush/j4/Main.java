package ua.com.javarush.j4;


import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;


/**
 * Точка входу криптоаналізатора шифру Цезаря.
 *
 * <p>Реалізацію та структуру класів обирай самостійно. Контракт CLI і поведінки
 * визначений у {@code MainTest} — зеленій тести.
 */

public class Main {
    public static void main(String[] args) throws IOException {
        // TODO: реалізуй CLI шифру Цезаря. Дивись MainTest.



        try {
            Cipher cipher = new Cipher();
            RunOptions options = ArgsParser.parse(args);
            BruteForce bruteForce = new BruteForce();

            String text = FileService.read(options.path());
             ArrayList<Character> Languishes =  Alphabet.getLanguishes(text);
             switch (options.command()){
                 case ENCRYPT-> {
                     Path newPath = generateNewPath(options.path(),"[ENCRYPTED]");
                     String encrypted = cipher.encrypt(text, options.key(),  Languishes);
                     FileService.write(newPath,encrypted);
                 }
                 case DECRYPT->{
                     Path newPath = generateNewPath(options.path(),"[DECRYPTED]");
                     String decrypted = cipher.decrypt(text, options.key(), Languishes);
                     FileService.write(newPath,decrypted);

                 }
                 case BRUTE_FORCE -> {
                     Path newPath = generateNewPath(options.path(),"[BRUTE_FORCE]");
                     String bf = cipher.decrypt(text, bruteForce.findKey(text), Languishes );
                    FileService.write(newPath,bf);

                 }
             }
        }catch (Exception e){
            System.err.println("Помилка: " + e.getMessage());
        }
    }


    private static Path generateNewPath(Path originalPath, String suffix) {
        String name = originalPath.getFileName().toString();
        if (name.contains("[ENCRYPTED]") && suffix.equals("[DECRYPTED]")) {
            String newName = name.replace("[ENCRYPTED]", "[DECRYPTED]");
            return originalPath.resolveSibling(newName);
        }
        if(name.contains("[ENCRYPTED]") && suffix.equals("[BRUTE_FORCE]")) {
            String newName = name.replace("[ENCRYPTED]", "[BRUTE_FORCE]");
            return originalPath.resolveSibling(newName);
        }
        int dotIndex = name.lastIndexOf(".");
        String newName;
        if (dotIndex != -1) {
            newName = name.substring(0, dotIndex) + suffix + name.substring(dotIndex);
        } else {
            newName = name + suffix;
        }
        return originalPath.resolveSibling(newName);
    }
}
