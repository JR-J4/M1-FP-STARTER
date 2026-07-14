package ua.com.javarush.j4.cipher;

/** A configured text transform. Key/keyword are bound at construction, so this is a pure String → String. */
// ── SOLID ▸ O — Принцип відкритості/закритості (OCP) ──
// Cipher — точка розширення. Щоб додати новий шифр (ROT13, Atbash, Vigenère…),
// достатньо СТВОРИТИ новий клас, що реалізує цей інтерфейс; жоден наявний клас
// (CaesarCipher, CaesarCracker, EncryptCommand) при цьому НЕ змінюється.
// Модуль відкритий для розширення, але закритий для модифікації.
//
// Також ▸ I (ISP): інтерфейс мінімальний — лише encrypt/decrypt, без зайвих методів.
public interface Cipher {
    String encrypt(String text);

    String decrypt(String text);
}
