package ua.com.javarush.j4.constant;

import java.util.HashMap;
import java.util.Map;

public class AlphabetConstant {
    public static final Map<Character, Integer> US_CHAR_TO_INDEX = new HashMap<>();
    public static final Map<Integer, Character> US_INDEX_TO_CHAR = new HashMap<>();
    public static final Map<Character, Integer> UA_CHAR_TO_INDEX = new HashMap<>();
    public static final Map<Integer, Character> UA_INDEX_TO_CHAR = new HashMap<>();
    public final static Map<Character, Double> CHAR_STATISTIC_ENGLISH =
            Map.of('e', 6.0, 't', 4.0, 'a', 3.0, 'o', 2.0);
    public final static Map<Character, Double> CHAR_STATISTIC_UKRAINIAN =
            Map.of('о', 6.0, 'а', 4.0, 'н', 3.0, 'и', 2.0);

    static {
        char[] engLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
        char[] uaLetters = "АБВГҐДЕЄЖЗИІЇЙКЛМНОПРСТУФХЦЧШЩЬЮЯабвгґдеєжзиіїйклмнопрстуфхцчшщьюя".toCharArray();

        for (int i = 0; i < engLetters.length; i++) {
            US_CHAR_TO_INDEX.put(engLetters[i], i);
            US_INDEX_TO_CHAR.put(i, engLetters[i]);
        }

        for (int i = 0; i < uaLetters.length; i++) {
            UA_CHAR_TO_INDEX.put(uaLetters[i], i);
            UA_INDEX_TO_CHAR.put(i, uaLetters[i]);
        }
    }
}
