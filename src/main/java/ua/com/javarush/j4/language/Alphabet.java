package ua.com.javarush.j4.language;
import java.util.List;

public abstract class Alphabet {

    private List<Character> chars;

    protected void setChars(List<Character> chars) {
        this.chars = chars;
    }

    public int getSize() {
        return chars.size();
    }

    public int indexOf(char ch) {
        return chars.indexOf(ch);
    }

    public char charAt(int index) {
        return chars.get(index);
    }
}
