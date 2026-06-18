package ua.com.javarush.j4.alphabet;

import java.util.List;

/** Built-in alphabets and a name-based lookup. Rings are shared so shifting is consistent. */
public final class Alphabets {

    private static final CharacterRing EN_UPPER = new CharacterRing("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    private static final CharacterRing EN_LOWER = new CharacterRing("abcdefghijklmnopqrstuvwxyz");
    private static final CharacterRing UA_UPPER = new CharacterRing("АБВГҐДЕЄЖЗИІЇЙКЛМНОПРСТУФХЦЧШЩЬЮЯ");
    private static final CharacterRing UA_LOWER = new CharacterRing("абвгґдеєжзиіїйклмнопрстуфхцчшщьюя");
    private static final CharacterRing RU_UPPER = new CharacterRing("АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ");
    private static final CharacterRing RU_LOWER = new CharacterRing("абвгдеёжзийклмнопрстуфхцчшщъыьэюя");

    public static final Alphabet ENGLISH = new Alphabet("en", List.of(EN_UPPER, EN_LOWER));
    public static final Alphabet UKRAINIAN = new Alphabet("ua", List.of(UA_UPPER, UA_LOWER));
    public static final Alphabet RUSSIAN = new Alphabet("ru", List.of(RU_UPPER, RU_LOWER));

    /** Default for encrypt/decrypt: Latin + Ukrainian rings (no overlap), so mixed text just works. */
    public static final Alphabet DEFAULT = new Alphabet("default", List.of(EN_UPPER, EN_LOWER, UA_UPPER, UA_LOWER));

    private Alphabets() {
    }
}
