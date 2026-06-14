package ua.com.javarush.j4.crypty;

import ua.com.javarush.j4.language.EN_language;

import javax.sql.rowset.serial.SerialBlob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ua.com.javarush.j4.language.EN_language.abc;

public class Cypher {
//    public EN_language abc;

    // private final int size;

//    public Cypher(int size) {
//        this.size = size;
//    }

//    public static char moveEncrypt(char symbol, int key){
////подумати над юнікей
//        if(EN_language.abc.indexOf(symbol) != -1) {
//            return abc.charAt((abc.indexOf(symbol) + key) % abc.length());
//        }else {
//            return symbol;
//        }
//    }

    public static char moveEncrypt(char symbol, int key){
//подумати над юнікей
        if(EN_language.getAbc().indexOf(symbol)!= -1) {
            return EN_language.getAbc().charAt((EN_language.getAbc().indexOf(symbol) + key) % EN_language.getAbc().length());
        }else {
            return symbol;
        }
    }

    public String encrypt(String text, int key) {
        return addShift(text, uniKey(key));
    }

    public String decrypt(String text, int key) {
        return addShift(text, uniKey(-key));
    }

    public String addShift(String text, int uniKey){
        StringBuilder newText = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); i++) {
            newText.append(moveEncrypt(text.charAt(i), uniKey));
        }
        return newText.toString();
    }

    public int uniKey(int key){
        return ((key % EN_language.getAbc().length()) + EN_language.getAbc().length()) % EN_language.getAbc().length();
    }



    //через масив
//    public String encrypt(String text, int key) {
//        List<Character> newText = new ArrayList<>();
//
//        for (int j = 0; j < text.length(); j++) {
//            newText.add(moveEncrypt(text.charAt(j), key));
//        }
//        return Arrays.toString(newText.toArray());
//    }


//    public static char moveDecrypt(char symbol, int key){
//
//        if(EN_language.abc.indexOf(symbol) != -1) {
//            return abc.charAt(((abc.indexOf(symbol) - key) + abc.length()) % abc.length());
//        }else {
//            return symbol;
//        }
//    }
//
//    public String decrypt(String text, int key){
//        List<Character> newText = new ArrayList<>();
//
//        for (int j = 0; j < text.length(); j++) {
//            newText.add(moveDecrypt(text.charAt(j), key));
//        }
//        return Arrays.toString(newText.toArray());

        // return "";

//        char[] originalTextArray = text.toCharArray();
//        int len = originalTextArray.length;
//
//        List<Character> newText = new ArrayList<>(len);
//
//     for (int i = 0; i < originalTextArray.length; i++) {
//         if (EN_language.alphabetToArray().contains(originalTextArray[i])){
//             int ind = (i + key + EN_language.alphabetToArray().size()) % EN_language.alphabetToArray().size();
//             newText.add(i, EN_language.alphabetToArray().get(ind));
//         } else {
//             // int a = EN_language.alphabetToArray().indexOf(originalTextArray[i]);
//
//             newText.add(originalTextArray[i]);
//            }
//         //System.out.print(ind + " ");
//     }
//
//        for (int i = 0; i < originalTextArray.length; i++) {
//            if (EN_language.alphabetToArray().contains(originalTextArray[i])) {
//                for (int j = 0; j < EN_language.alphabetToArray().size(); j++) {
//                    int ind = EN_language.alphabetToArray().get(i + key % EN_language.alphabetToArray().size());
//                    newText.add(i, EN_language.alphabetToArray().get(ind));
//
//                    int ind =  (i + key) % len -1;
//
//                    if (originalTextArray[i] == EN_language.alphabetToArray().get(j)) {
//                        newText.add(i, EN_language.alphabetToArray().get(j + key));
//                    }
//                }
//            } else {
//                newText.add(originalTextArray[i]);
//            }
//        }
//        return Arrays.toString(originalTextArray) + len + newText;

       //return text + key;
        //return toString(newText.size());

        //return newText.toString();
      //  return Arrays.toString(originalTextArray) + key+len+newText;


//        for (int i = 0; i < originalTextArray.length; i++){
//            //for (int j = 0; j < EN_language.alphabetToArray().size(); j++){
//                for (int k = 0; k < originalTextArray.length; k++){
//                    if (originalTextArray[i] == EN_language.alphabetToArray().get(j)){
//                        newText.add(i, EN_language.alphabetToArray().get(j + key));
//
//                       // newText.set(k, EN_language.alphabetToArray().get(j + key));
//                    }
//                }
//            //}
//        }

//        if(originalTextArray.length > j){
//                        if(originalTextArray.length % j == 0){
//                            j = 0;
//                        }else if(originalTextArray.length % j != 0){
//                            j = originalTextArray.length % j;
//                        }
//                        j = originalTextArray.length % j -1;
//                    }

//                if(originalTextArray.length > EN_language.alphabetToArray().size()) {
//                    if (originalTextArray.length % EN_language.alphabetToArray().size() != 0) {
//                                j = originalTextArray.length % EN_language.alphabetToArray().size();
//                        } else {
//                            j = 0;
//                    }
//                }


//

//                    if(i !=0  && originalTextArray.length > j){
//                        if(originalTextArray.length % j == 0){
//                            j = 0;
//                        }else if(originalTextArray.length % j != 0){
//                            j = originalTextArray.length % j;
//                        }
//                        j = originalTextArray.length % j -1;
//                    }
//    }


}
