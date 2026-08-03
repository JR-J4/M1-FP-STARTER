package ua.com.javarush.j4.io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

/** Reads the textual content of a file. Implementations declare which paths they support. */
// ── SOLID ▸ I — Принцип розділення інтерфейсів (ISP): ЖИВИЙ ПРИКЛАД ПОРУШЕННЯ ──
// «Чисте» ядро читача — це лише supports() + read(): рівно те, що потрібно клієнту
// TextReaders.pick(). Два останні члени інтерфейсу цей контракт РОЗДУВАЮТЬ:
//
//   1) extends Comparable<TextReader> та getSupportedFileTypes() — це ISP-порушення:
//      pick() ними НЕ користується, але кожна реалізація (Plain/Markdown/Gzip)
//      змушена їх писати. Інтерфейс нав'язує клієнтам методи, яких вони не просили.
//   2) compareTo() у реалізаціях сортує читачі за кількістю розширень
//      (size() - o.getSupportedFileTypes().size()). Це ще й неузгоджено з equals та
//      не задає осмисленого порядку — ознака проблеми і з боку контракту Comparable.
//
// ЯК ВИПРАВИТИ (правильний ISP): лишити в TextReader тільки supports()+read(), а
// потребу сортування/переліку типів винести в окремий вузький інтерфейс
// (напр. SupportedTypes) і додавати його ЛИШЕ тим реалізаціям, яким він дійсно
// потрібен. Порівняйте з Cipher/FitnessScorer/Cracker вище — там інтерфейси тонкі.
public interface TextReader extends Comparable<TextReader> {
    // ISP-чисте ядро: єдине, що справді потрібно клієнтові TextReaders.pick().
    boolean supports(Path path);

    HashMap<String, String> getMeta();

    String read(Path path) throws IOException;

    // ↓ Зайве для клієнта: саме ці члени роблять інтерфейс «товстим» (див. коментар вище).
    HashSet<String> getSupportedFileTypes();
}
