package ua.com.javarush.j4.service;

import ua.com.javarush.j4.constant.AlphabetConstant;
import ua.com.javarush.j4.constant.Language;
import ua.com.javarush.j4.fileManager.FileManager;
import ua.com.javarush.j4.validator.Validator;

import java.nio.file.Path;
import java.util.Map;

public class CryptoService {
    protected FileManager fileManager;
    protected Validator validator;

    protected Map<Character, Integer> CHAR_TO_INDEX_ALPHABET;
    protected Map<Integer, Character> INDEX_TO_CHAR_ALPHABET;

    public CryptoService() {
        this.fileManager = new FileManager();
        this.validator = new Validator();
    }

    public void execute(String path, String key, boolean isEncrypt) {
        Path convertedPath = validator.validatePath(path);
        String originalText = fileManager.readFile(convertedPath);
        installAlphabet(originalText);
        int numericKey = validator.validateKey(key);
        int normalizedNumericKey = numericKey % CHAR_TO_INDEX_ALPHABET.size();
        String encryptedText = convertText(originalText, normalizedNumericKey, isEncrypt);
        fileManager.writeToFile(convertedPath, isEncrypt, encryptedText);
    }

    protected String convertText(String text, int key, boolean isEncrypt) {
        if (key == 0 || key == CHAR_TO_INDEX_ALPHABET.size()) {
            return text;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char oldChar = text.charAt(i);
            Integer oldIndex = CHAR_TO_INDEX_ALPHABET.get(oldChar);
            if (oldIndex == null) {
                stringBuilder.append(oldChar);
                continue;
            }
            int newIndex = makeNewIndex(oldIndex, key, isEncrypt);
            char newChar = INDEX_TO_CHAR_ALPHABET.get(newIndex);
            stringBuilder.append(newChar);
        }
        return stringBuilder.toString();
    }

    protected Integer makeNewIndex(Integer index, Integer key, boolean isEncrypt) {
        int newIndex;
        if (isEncrypt) {
            newIndex = index + key;
        } else {
            newIndex = index - key;
        }
        if (newIndex < 0) {
            newIndex = CHAR_TO_INDEX_ALPHABET.size() - Math.abs(newIndex);
        } else {
            newIndex = (newIndex) % CHAR_TO_INDEX_ALPHABET.size();
        }
        return newIndex;
    }

    protected Language installAlphabet(String text) {

        if (text.trim().isEmpty()){
            CHAR_TO_INDEX_ALPHABET = AlphabetConstant.US_CHAR_TO_INDEX;
            INDEX_TO_CHAR_ALPHABET = AlphabetConstant.US_INDEX_TO_CHAR;
            return Language.ENGLISH;
        }

        Language language;
        char ch = text.charAt(0);
        if (AlphabetConstant.UA_CHAR_TO_INDEX.containsKey(ch)) {
            language = Language.UKRAINIAN;
        } else {
            language = Language.ENGLISH;
        }
        if (language == Language.UKRAINIAN) {
            CHAR_TO_INDEX_ALPHABET = AlphabetConstant.UA_CHAR_TO_INDEX;
            INDEX_TO_CHAR_ALPHABET = AlphabetConstant.UA_INDEX_TO_CHAR;
        } else {
            CHAR_TO_INDEX_ALPHABET = AlphabetConstant.US_CHAR_TO_INDEX;
            INDEX_TO_CHAR_ALPHABET = AlphabetConstant.US_INDEX_TO_CHAR;
        }
        return language;
    }
}
