package ua.com.javarush.j4;

import java.io.FileReader;
import java.io.IOException;


/**
 * Точка входу криптоаналізатора шифру Цезаря.
 *
 * <p>Реалізацію та структуру класів обирай самостійно. Контракт CLI і поведінки
 * визначений у {@code MainTest} — зеленій тести.
 */

public class Main {
    public static void main(String[] args) throws IOException {
        // TODO: реалізуй CLI шифру Цезаря. Дивись MainTest.
        int kay = 1;

        FileReader file = new FileReader("C:\\Users\\user\\IdeaProjects\\M1-FP-Mykhailo\\src\\main\\resources\\input.txt");
        String text = file.readAllAsString();
        String text2 = "АААААВВВВ";
//          ArrayList<Character> englishAlphabet = new ArrayList<>(List.of(
//                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
//                'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
//                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
//                'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
//                '.', ',', '«', '»', '"', '\'', ':', '!', '?'
//        ));
          Cipher cipher = new Cipher();

        System.out.println(cipher.encrypt(text2, kay,Alphabet.getLanguishes(text2)));
        String newText = cipher.encrypt(text2, kay, Alphabet.UKR.getAlphabet());
        System.out.println(cipher.decrypt(newText, kay, Alphabet.UKR.getAlphabet()));





    }
}
