package ua.com.javarush.j4.service;

import ua.com.javarush.j4.constant.AlphabetConstant;
import ua.com.javarush.j4.constant.Language;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class BruteForceService extends CryptoService {

    private Map<Character, Double> referenceValues;
    private Language language;

    public void execute(String path) {
        Path convertedPath = validator.validatePath(path);
        String originalText = fileManager.readFile(convertedPath);
        this.language = installAlphabet(originalText);
        installReferenceValues();
        String decryptedText = decryptText(originalText);
        fileManager.writeToFile(convertedPath, false, decryptedText);
    }

    private void installReferenceValues() {
        if (language == Language.UKRAINIAN) {
            referenceValues = AlphabetConstant.CHAR_STATISTIC_UKRAINIAN;
        } else {
            referenceValues = AlphabetConstant.CHAR_STATISTIC_ENGLISH;
        }
    }

    private String decryptText(String originalText) {
        for (int i = 0; i < CHAR_TO_INDEX_ALPHABET.size(); i++) {
            String decryptedText = convertText(originalText, i, false);
            boolean isDecrypt = analyzeText(decryptedText);
            if (isDecrypt) {
                return decryptedText;
            }
        }
        return originalText;
    }

    private boolean analyzeText(String encryptedText) {
        boolean isDecrypt = true;
        Map<Character, Double> values = getCharStatistic(encryptedText);

        for (Map.Entry<Character, Double> valueEntrySet : values.entrySet()) {
            Double value = (valueEntrySet.getValue() * 100.0) / encryptedText.length();
            Double referenceValue = referenceValues.get(valueEntrySet.getKey());
            if (value < referenceValue) {
                isDecrypt = false;
            }
        }
        return isDecrypt;
    }

    private Map<Character, Double> getCharStatistic(String encryptedText) {
        Map<Character, Double> values = resetStatistic();
        for (int i = 0; i < encryptedText.length(); i++) {
            char ch = encryptedText.charAt(i);
            if (values.containsKey(ch)) {
                Double stat = values.get(ch);
                stat++;
                values.put(ch, stat);
            }
        }
        return values;
    }

    private Map<Character, Double> resetStatistic() {
        Map<Character, Double> values = new HashMap<>();
        if (language == Language.UKRAINIAN) {
            values.put('о', 0.0);
            values.put('а', 0.0);
            values.put('н', 0.0);
            values.put('и', 0.0);
        } else {
            values.put('e', 0.0);
            values.put('t', 0.0);
            values.put('a', 0.0);
            values.put('o', 0.0);
        }
        return values;
    }

}
