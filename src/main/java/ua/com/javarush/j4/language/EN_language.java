package ua.com.javarush.j4.language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EN_language {

   //вар 1 алфавіту
  public static String abc = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    public static String[] someEnWords = {"the", "and", "of", "is", "to", "in", "that", "it"};

    public static String getAbc() {
        return abc;
    }

    public static String[] getSomeEnWords() {
        return someEnWords;
    }

    public static ArrayList<Character> alphabetToArray(){
        char[] abcArray = abc.toCharArray();
        ArrayList<Character> alphabet = new ArrayList<Character>();
        for (int i = 0; i < abcArray.length; i++) {
            alphabet.add(abcArray[i]);
        }
        return alphabet;
        //System.out.println(alphabet);
    }

    //вар 2 -масив

    public static ArrayList<Character> ALPHABET_EN = new ArrayList<>(
            Arrays.asList(
                    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
                    'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b',
                    'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
                    'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
            )
    );

    public static ArrayList<Character> getAlphabetEn() {
        return ALPHABET_EN;
    }

//    public static void alphabetToArray() {
//        System.out.println("jhnjn");
//    }
}
