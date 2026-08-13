package ua.com.javarush.j4.alphabet;

/** An ordered, cyclic sequence of characters of a single case (a Caesar "ring"). */
// ── SOLID ▸ S — Принцип єдиного обов'язку (SRP) ──
// Клас відповідає ВИКЛЮЧНО за арифметику кільця символів: зсув, дзеркало, індекс.
// Він нічого не знає про файли, ключі, мови чи шифри — тому має лише одну причину
// для зміни (зміну правил обходу кільця). Це і є Single Responsibility.
//
// Арифметика працює від ПОЗИЦІЇ, а не від символу: пошук позиції — обов'язок
// Alphabet, який робить це за O(1) через таблицю. Кільце ж лишається чистою
// математикою по колу.
public final class CharacterRing {
    private final String chars;

    public CharacterRing(String chars) {
        if (chars == null || chars.isEmpty()) {
            throw new IllegalArgumentException("ring must be non-empty");
        }
        this.chars = chars;
    }

    public int size() {
        return chars.length();
    }

    /** The character at {@code index}; the inverse of {@link #indexOf(char)}. */
    public char at(int index) {
        return chars.charAt(index);
    }

    /** Shifts the character at {@code index} by {@code k} positions around this ring. */
    public char shiftFrom(int index, int k) {
        return chars.charAt(Math.floorMod(index + k, chars.length()));
    }

    /** Atbash mirror: the character at {@code index} maps to the one at {@code size-1-index}. */
    public char mirrorFrom(int index) {
        return chars.charAt(chars.length() - 1 - index);
    }
}
