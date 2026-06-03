package ua.com.javarush.j4;

import java.util.ArrayList;
import java.util.List;

public enum Alphabet {
    ENG(List.of('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z')),
    UKR(List.of('А', 'Б', 'В', 'Г', 'Ґ', 'Д', 'Е', 'Є', 'Ж', 'З', 'И', 'І', 'Ї', 'Й', 'К', 'Л', 'М',
            'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ь', 'Ю', 'Я',
            'а', 'б', 'в', 'г', 'ґ', 'д', 'е', 'є', 'ж', 'з', 'и', 'і', 'ї', 'й', 'к', 'л', 'м',
            'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ь', 'ю', 'я'));
    private final List<Character> characters;
    Alphabet(List<Character> characters) {
        this.characters = characters;
    }
    public ArrayList<Character> getAlphabet() {
        return new ArrayList<>(this.characters);
    }

    public static ArrayList<Character> getLanguishes(String text){
        char[] chars = text.toCharArray();
        int en = 0,ukr = 0;
        for (char c : chars) {
            if(ENG.getAlphabet().contains(c)){
                ++en;
            }else if(UKR.getAlphabet().contains(c)){
                ++ukr;
            }
        }
        return en>ukr ? ENG.getAlphabet() : UKR.getAlphabet();
    }
}
