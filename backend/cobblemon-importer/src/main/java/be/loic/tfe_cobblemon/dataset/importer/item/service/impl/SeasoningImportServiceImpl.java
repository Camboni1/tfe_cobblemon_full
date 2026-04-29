package be.loic.tfe_cobblemon.dataset.importer.item.service.impl;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.item.service.SeasoningImportService;
import be.loic.tfe_cobblemon.item.entity.Item;
import be.loic.tfe_cobblemon.item.entity.Seasoning;
import be.loic.tfe_cobblemon.item.entity.SeasoningMobEffect;
import be.loic.tfe_cobblemon.item.repository.ItemRepository;
import be.loic.tfe_cobblemon.item.repository.SeasoningRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SeasoningImportServiceImpl implements SeasoningImportService {

    private final ItemRepository itemRepository;
    private final SeasoningRepository seasoningRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void importSeasonings(DatasetVersion datasetVersion, Path datasetRoot) {
        Path itemsDirectory = datasetRoot.resolve("items");

        if (!Files.exists(itemsDirectory)) {
            throw new IllegalStateException("Le dossier items est introuvable : " + itemsDirectory);
        }

        if (!Files.isDirectory(itemsDirectory)) {
            throw new IllegalStateException("Le chemin items n'est pas un dossier : " + itemsDirectory);
        }

        try (Stream<Path> pathStream = Files.walk(itemsDirectory)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(this::isJsonFile)
                    .sorted(Comparator.naturalOrder())
                    .forEach(path -> importSeasoningFromItemFile(datasetVersion, itemsDirectory, path));
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors du parcours du dossier items : " + itemsDirectory, e);
        }
    }

    private void importSeasoningFromItemFile(DatasetVersion datasetVersion, Path itemsDirectory, Path filePath) {
        try {
            String rawJson = Files.readString(filePath, StandardCharsets.UTF_8);
            JsonNode rootNode = objectMapper.readTree(rawJson);

            JsonNode seasoningNode = firstNode(rootNode, "seasoning");
            if (seasoningNode == null || seasoningNode.isNull() || !seasoningNode.isObject()) {
                return;
            }

            String namespacedId = resolveNamespacedId(rootNode, itemsDirectory, filePath);

            Item item = itemRepository.findByDatasetVersionIdAndNamespacedId(datasetVersion.getId(), namespacedId)
                    .orElseThrow(() -> new IllegalStateException("Item introuvable pour seasoning : " + namespacedId));

            Seasoning seasoning = seasoningRepository.findByItemId(item.getId())
                    .orElseGet(Seasoning::new);

            seasoning.setItem(item);
            seasoning.setColour(resolveRequiredText(seasoningNode, "colour", "color"));
            seasoning.setFoodHunger(resolveShort(seasoningNode, "foodHunger", "food_hunger", "hunger"));
            seasoning.setFoodSaturation(resolveBigDecimal(seasoningNode, "foodSaturation", "food_saturation", "saturation"));
            seasoning.setRawJson(toJson(seasoningNode));

            seasoning.getMobEffects().clear();

            JsonNode mobEffectsNode = firstNode(seasoningNode, "mobEffects", "mob_effects", "effects");
            if (mobEffectsNode != null && mobEffectsNode.isArray()) {
                for (JsonNode effectNode : mobEffectsNode) {
                    SeasoningMobEffect mobEffect = new SeasoningMobEffect();
                    mobEffect.setSeasoning(seasoning);
                    mobEffect.setEffectId(resolveRequiredText(effectNode, "effectId", "effect_id", "effect"));
                    mobEffect.setDuration(resolveRequiredInteger(effectNode, "duration"));
                    mobEffect.setAmplifier(resolveShortOrDefault(effectNode, (short) 0, "amplifier"));
                    mobEffect.setAmbient(resolveBooleanOrDefault(effectNode, false, "ambient"));
                    mobEffect.setVisible(resolveBooleanOrDefault(effectNode, true, "visible"));
                    mobEffect.setShowIcon(resolveBooleanOrDefault(effectNode, true, "showIcon", "show_icon"));

                    seasoning.getMobEffects().add(mobEffect);
                }
            }

            seasoningRepository.save(seasoning);
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la lecture du fichier item : " + filePath, e);
        }
    }

    private boolean isJsonFile(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json");
    }

    private String resolveNamespacedId(JsonNode rootNode, Path itemsDirectory, Path filePath) {
        String candidate = firstNonBlank(
                text(rootNode, "namespacedId"),
                text(rootNode, "namespaced_id"),
                text(rootNode, "identifier"),
                text(rootNode, "resource"),
                text(rootNode, "item")
        );

        if (candidate != null && candidate.contains(":")) {
            return normalizeNamespacedId(candidate);
        }

        String relative = itemsDirectory.relativize(filePath).toString().replace('\\', '/');
        String withoutExtension = removeJsonExtension(relative);
        String normalizedPath = normalizePath(withoutExtension);

        return "cobblemon:" + normalizedPath;
    }

    private JsonNode firstNode(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode child = node.get(fieldName);
            if (child != null && !child.isNull()) {
                return child;
            }
        }
        return null;
    }

    private String resolveRequiredText(JsonNode node, String... fieldNames) {
        String value = firstNonBlank(extractTexts(node, fieldNames));
        if (value == null) {
            throw new IllegalStateException("Champ texte obligatoire introuvable");
        }
        return value;
    }

    private String[] extractTexts(JsonNode node, String... fieldNames) {
        String[] values = new String[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i++) {
            values[i] = text(node, fieldNames[i]);
        }
        return values;
    }

    private Integer resolveRequiredInteger(JsonNode node, String... fieldNames) {
        Integer value = resolveInteger(node, fieldNames);
        if (value == null) {
            throw new IllegalStateException("Champ entier obligatoire introuvable");
        }
        return value;
    }

    private Short resolveShort(JsonNode node, String... fieldNames) {
        JsonNode child = firstNode(node, fieldNames);
        if (child == null || child.isNull()) {
            return null;
        }

        if (child.isNumber()) {
            return child.shortValue();
        }

        if (child.isTextual()) {
            String value = child.asText().trim();
            if (!value.isBlank()) {
                return Short.valueOf(value);
            }
        }

        return null;
    }

    private Short resolveShortOrDefault(JsonNode node, short defaultValue, String... fieldNames) {
        Short value = resolveShort(node, fieldNames);
        return value != null ? value : defaultValue;
    }

    private Integer resolveInteger(JsonNode node, String... fieldNames) {
        JsonNode child = firstNode(node, fieldNames);
        if (child == null || child.isNull()) {
            return null;
        }

        if (child.isNumber()) {
            return child.intValue();
        }

        if (child.isTextual()) {
            String value = child.asText().trim();
            if (!value.isBlank()) {
                return Integer.valueOf(value);
            }
        }

        return null;
    }

    private BigDecimal resolveBigDecimal(JsonNode node, String... fieldNames) {
        JsonNode child = firstNode(node, fieldNames);
        if (child == null || child.isNull()) {
            return null;
        }

        if (child.isNumber()) {
            return child.decimalValue();
        }

        if (child.isTextual()) {
            String value = child.asText().trim();
            if (!value.isBlank()) {
                return new BigDecimal(value);
            }
        }

        return null;
    }

    private boolean resolveBooleanOrDefault(JsonNode node, boolean defaultValue, String... fieldNames) {
        Boolean value = resolveBoolean(node, fieldNames);
        return value != null ? value : defaultValue;
    }

    private Boolean resolveBoolean(JsonNode node, String... fieldNames) {
        JsonNode child = firstNode(node, fieldNames);
        if (child == null || child.isNull()) {
            return null;
        }

        if (child.isBoolean()) {
            return child.asBoolean();
        }

        if (child.isTextual()) {
            String value = child.asText().trim().toLowerCase(Locale.ROOT);
            if ("true".equals(value)) {
                return true;
            }
            if ("false".equals(value)) {
                return false;
            }
        }

        return null;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode child = node.get(fieldName);
        if (child == null || child.isNull() || !child.isTextual()) {
            return null;
        }

        String value = child.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeNamespacedId(String value) {
        String trimmed = value.trim().replace('\\', '/').toLowerCase(Locale.ROOT);

        if (!trimmed.contains(":")) {
            throw new IllegalStateException("Namespaced id invalide : " + value);
        }

        String[] parts = trimmed.split(":", 2);
        String namespace = parts[0].trim();
        String path = normalizePath(parts[1]);

        if (namespace.isBlank() || path.isBlank()) {
            throw new IllegalStateException("Namespaced id invalide : " + value);
        }

        return namespace + ":" + path;
    }

    private String normalizePath(String value) {
        return value.trim()
                .replace('\\', '/')
                .replaceAll("^/+", "")
                .replaceAll("/+$", "")
                .toLowerCase(Locale.ROOT);
    }

    private String removeJsonExtension(String value) {
        if (value.toLowerCase(Locale.ROOT).endsWith(".json")) {
            return value.substring(0, value.length() - 5);
        }
        return value;
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Impossible de sérialiser un seasoning JSON", e);
        }
    }
}