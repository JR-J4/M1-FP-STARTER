package ua.com.javarush.j4.cipher;

import java.util.ArrayList;
import java.util.Collections;

public class Cipher  {
    public String encrypt(String text, int key, ArrayList<Character> alphabet) {
        ArrayList <Character>newAlphabet = new ArrayList<>(alphabet);
        Collections.rotate(newAlphabet,-key );
        char[] chars = text.toCharArray();
        return cycle(chars,alphabet,newAlphabet);
    }

    public String decrypt(String text, int key, ArrayList<Character> alphabet) {
        ArrayList <Character> newAlphabet = new ArrayList<>(alphabet);

        Collections.rotate(newAlphabet,key );
        char[] chars = text.toCharArray();

        return cycle(chars,alphabet,newAlphabet);
    }
    public String cycle(char[] chars ,ArrayList<Character> alphabet,ArrayList<Character> newAlphabet) {
        StringBuilder result = new StringBuilder();
        for(char c : chars) {
            if(alphabet.contains(c)) {

                int index = alphabet.indexOf(c);
                result.append(newAlphabet.get(index));
            }else  {
                result.append(c);
            }
        }
        return result.toString();
    }
}
