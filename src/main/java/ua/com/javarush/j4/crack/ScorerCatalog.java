package ua.com.javarush.j4.crack;

import ua.com.javarush.j4.support.NamedRegistry;

import java.util.List;
import java.util.function.Function;

/** Registry of named fitness scorers for brute-force. Replaces the in-command switch. */
public final class ScorerCatalog {

    /**
     * Same generic {@link NamedRegistry} as {@code CipherCatalog}, but a different T:
     * here each value is a {@code Function<Language, FitnessScorer>}. Generics let the
     * one registry serve both catalogs without copy-pasting the lookup logic.
     */
    private final NamedRegistry<Function<Language, FitnessScorer>> creators = new NamedRegistry<>("scorer");

    public void register(String name, Function<Language, FitnessScorer> creator) {
        creators.register(name, creator);
    }

    public FitnessScorer create(String name, Language language) {
        return creators.get(name).apply(language);
    }

    public static ScorerCatalog withDefaults() {
        ScorerCatalog catalog = new ScorerCatalog();
        catalog.register("dictionary", DictionaryScorer::new);
        catalog.register("frequency", FrequencyScorer::new);
        catalog.register("combined", lang ->
                new CompositeFitnessScorer(List.of(
                        new DictionaryScorer(lang),
                        new FrequencyScorer(lang))));
        return catalog;
    }
}
