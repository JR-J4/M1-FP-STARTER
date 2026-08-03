package ua.com.javarush.j4.alphabet;

/** An ordered, cyclic sequence of characters of a single case (a Caesar "ring"). */
// ── SOLID ▸ S — Принцип єдиного обов'язку (SRP) ──
// Клас відповідає ВИКЛЮЧНО за арифметику кільця символів: зсув, дзеркало, індекс.
// Він нічого не знає про файли, ключі, мови чи шифри — тому має лише одну причину
// для зміни (зміну правил обходу кільця). Це і є Single Responsibility.
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

    public boolean contains(char c) {
        return chars.indexOf(c) >= 0;
    }

    public int indexOf(char c) {
        return chars.indexOf(c);
    }

    /** Shift {@code c} by {@code k} positions within this ring; caller guarantees membership. */
    public char shift(char c, int k) {
        int n = chars.length();
        int idx = chars.indexOf(c);
        int shifted = ((idx + k) % n + n) % n;
        return chars.charAt(shifted);
    }

    /** Atbash mirror: map index i to size-1-i. */
    public char mirror(char c) {
        return chars.charAt(size() - 1 - chars.indexOf(c));
    }
}
