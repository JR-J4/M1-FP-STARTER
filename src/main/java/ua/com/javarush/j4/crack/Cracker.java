package ua.com.javarush.j4.crack;

/** Recovers plaintext from ciphertext without a key. */
// ── SOLID ▸ I — Принцип розділення інтерфейсів (ISP) ──
// «Товстий» інтерфейс — гірше за багато тонких. Тут інтерфейс вузький: один
// метод crack(). Клієнт (BruteForceCommand) не змушений знати про зайві операції,
// а нова реалізація зобов'язана реалізувати лише те, що справді потрібно.
public interface Cracker {
    CrackResult crack(String ciphertext);
}
