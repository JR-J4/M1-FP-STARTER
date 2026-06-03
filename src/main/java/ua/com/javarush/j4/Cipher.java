package ua.com.javarush.j4;

import java.util.ArrayList;
import java.util.Collections;

public class Cipher {
    public String encrypt(String text, String key, ArrayList<Character> alphabet) {
        ArrayList <Character>newAlphabet = new ArrayList(alphabet);
        int k = Integer.parseInt(key);
        Collections.rotate(newAlphabet,k );
        char[] chars = text.toCharArray();
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

    public String decrypt(String text, String key, ArrayList<Character> alphabet) {
        ArrayList <Character>newAlphabet = new ArrayList(alphabet);
        int k = (Integer.parseInt(key))*(-1);
        Collections.rotate(newAlphabet,k );
        char[] chars = text.toCharArray();
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
