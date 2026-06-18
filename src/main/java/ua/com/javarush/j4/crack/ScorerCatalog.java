package ua.com.javarush.j4.crack;

import ua.com.javarush.j4.error.InvalidArgumentsException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** Registry of named fitness scorers for brute-force. Replaces the in-command switch. */
public final class ScorerCatalog {

    private final Map<String, Function<Language, FitnessScorer>> creators = new HashMap<>();

    public void register(String name, Function<Language, FitnessScorer> creator) {
        creators.put(name.toLowerCase(Locale.ROOT), creator);
    }

    public FitnessScorer create(String name, Language language) {
        Function<Language, FitnessScorer> creator = creators.get(name.toLowerCase(Locale.ROOT));
        if (creator == null) {
            throw new InvalidArgumentsException(
                    "Unknown scorer '" + name + "'. Valid values: dictionary, frequency");
        }
        return creator.apply(language);
    }

    public static ScorerCatalog withDefaults() {
        ScorerCatalog catalog = new ScorerCatalog();
        catalog.register("dictionary", DictionaryScorer::new);
        catalog.register("frequency", FrequencyScorer::new);
        return catalog;
    }
}
