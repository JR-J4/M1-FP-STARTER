package ua.com.javarush.j4.crack;

/** Strategy: scores how strongly a text resembles natural language. Higher is better. */
public interface FitnessScorer {
    double score(String text);
}
