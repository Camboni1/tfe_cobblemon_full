package be.loic.tfe_cobblemon.common.translation.service;

import be.loic.tfe_cobblemon.common.translation.repository.TranslationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TranslationService {

    private final TranslationRepository translationRepository;

    private final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadCache() {
        reload();
    }

    public void reload() {
        for (String locale : new String[]{"en", "fr"}) {
            Map<String, String> translations = translationRepository.findAllByLocale(locale)
                    .stream()
                    .collect(Collectors.toMap(t -> t.getKey(), t -> t.getValue()));
            cache.put(locale, translations);
        }
    }

    public String translate(String key, Locale locale) {
        String lang = locale.getLanguage();
        Map<String, String> translations = cache.getOrDefault(lang, cache.get("en"));
        return translations.getOrDefault(key, key);
    }

    public String bucket(String bucketName, Locale locale) {
        return translate("bucket." + bucketName, locale);
    }

    public String position(String positionName, Locale locale) {
        return translate("position." + positionName, locale);
    }

    public String spawnType(String spawnTypeName, Locale locale) {
        return translate("spawnType." + spawnTypeName, locale);
    }

    public String tokenType(String tokenTypeName, Locale locale) {
        return translate("token." + tokenTypeName, locale);
    }

    public String side(String sideName, Locale locale) {
        return translate("side." + sideName, locale);
    }

    public String generation(String generationCode, Locale locale) {
        return translate("generation." + generationCode, locale);
    }
    public String pokemonName(String slug, Locale locale) {
        // "squirtle" → "cobblemon.species.squirtle.name"
        String key = "cobblemon.species." + slug.replace("-", "_") + ".name";
        return translate(key, locale);
    }
    public String pokemonDescription(String slug, Locale locale) {
        String key = "cobblemon.species." + slug.replace("-", "_") + ".desc";
        return translate(key, locale);
    }

    public String itemName(String namespacedId, Locale locale) {
        // "cobblemon:charcoal_stick" → "item.cobblemon.charcoal_stick"
        String key = "item." + namespacedId.replace(":", ".");
        return translate(key, locale);
    }
}