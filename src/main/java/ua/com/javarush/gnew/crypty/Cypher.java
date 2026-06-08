package ua.com.javarush.gnew.crypty;

import ua.com.javarush.gnew.language.Language;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Cypher {

  public String encrypt(String text, int key, Language language) {
    List<Character> original = language.alphabet();
    List<Character> rotated = new ArrayList<>(original);
    Collections.rotate(rotated, Math.negateExact(key));

    StringBuilder out = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      int idx = original.indexOf(c);
      out.append(idx < 0 ? c : rotated.get(idx));
    }
    return out.toString();
  }

  public String decrypt(String text, int key, Language language) {
    return encrypt(text, -key, language);
  }
}
