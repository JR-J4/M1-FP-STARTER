package ua.com.javarush.j4.crack;

/** Strategy: scores how strongly a text resembles natural language. Higher is better. */
// ── SOLID ▸ O — Принцип відкритості/закритості (OCP) + I — ISP + D — DIP ──
// Патерн «Стратегія». Нову метрику «читабельності» додають окремим класом
// (DictionaryScorer, FrequencyScorer), не чіпаючи CaesarCracker — це OCP.
// Інтерфейс має рівно один метод — це ISP (клієнти не залежать від зайвого).
// CaesarCracker залежить від цієї АБСТРАКЦІЇ, а не від конкретики — це DIP.
public interface FitnessScorer {
    double score(String text);
}
