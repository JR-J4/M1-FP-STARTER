package ua.com.javarush.j4.cipher;

import ua.com.javarush.j4.alphabet.Alphabet;

import java.util.ArrayList;
import java.util.HashMap;

public class BruteForce {

    public int findKey(String input) {
        ArrayList<Character> languishes = Alphabet.getLanguage(input);
        HashMap<Character, Integer> mapLetters = getFrequencies(languishes, input);
        char mostFrequentLetter = 'e';
        if (languishes.contains('о') || languishes.contains('О')) mostFrequentLetter = 'о';
        int key = (languishes.indexOf(findMostFrequent(mapLetters)) - languishes.indexOf(mostFrequentLetter) + languishes.size()) % languishes.size();
        return key;
    }
    public HashMap<Character,Integer> getFrequencies(ArrayList<Character> letters , String input) {
        HashMap<Character, Integer> mapLetters = new HashMap<>();
        for (Character c : letters) {
            mapLetters.put(Character.toLowerCase(c),0);
        }
        for (int i = 0; i < input.length(); i++) {
            char lowerCase = Character.toLowerCase(input.charAt(i));
            if (mapLetters.containsKey(lowerCase)) {
                mapLetters.put(lowerCase, mapLetters.get(lowerCase)+1);
            }
        }
        return mapLetters;
    }

    public char findMostFrequent(HashMap<Character, Integer> map) {
        int maxCount = 0;
        char mostChar =' ';
        for(var c : map.keySet()){
            if(map.get(c)>maxCount) {
                maxCount = map.get(c);
                mostChar = c;
            }
        }
        return mostChar;
    }
}
