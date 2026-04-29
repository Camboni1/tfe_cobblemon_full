package be.loic.tfe_cobblemon.dataset.importer.item.service.impl;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.item.service.BaitEffectImportService;
import be.loic.tfe_cobblemon.item.entity.BaitEffect;
import be.loic.tfe_cobblemon.item.entity.Item;
import be.loic.tfe_cobblemon.item.repository.BaitEffectRepository;
import be.loic.tfe_cobblemon.item.repository.ItemRepository;
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
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BaitEffectImportServiceImpl implements BaitEffectImportService {

    private final ItemRepository itemRepository;
    private final BaitEffectRepository baitEffectRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void importBaitEffects(DatasetVersion datasetVersion, Path datasetRoot) {
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
                    .forEach(path -> importBaitEffectsFromItemFile(datasetVersion, itemsDirectory, path));
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors du parcours du dossier items : " + itemsDirectory, e);
        }
    }

    private void importBaitEffectsFromItemFile(DatasetVersion datasetVersion, Path itemsDirectory, Path filePath) {
        try {
            String rawJson = Files.readString(filePath, StandardCharsets.UTF_8);
            JsonNode rootNode = objectMapper.readTree(rawJson);

            JsonNode baitEffectsNode = firstNode(rootNode, "baitEffects", "bait_effects");
            if (baitEffectsNode == null || baitEffectsNode.isNull() || !baitEffectsNode.isArray()) {
                return;
            }

            String namespacedId = resolveNamespacedId(rootNode, itemsDirectory, filePath);

            Item item = itemRepository.findByDatasetVersionIdAndNamespacedId(datasetVersion.getId(), namespacedId)
                    .orElseThrow(() -> new IllegalStateException("Item introuvable pour bait effects : " + namespacedId));

            baitEffectRepository.deleteAll(baitEffectRepository.findAllByItemIdOrderByIdAsc(item.getId()));

            for (JsonNode baitEffectNode : baitEffectsNode) {
                BaitEffect baitEffect = new BaitEffect();
                baitEffect.setItem(item);
                baitEffect.setEffectType(resolveRequiredText(baitEffectNode, "effectType", "effect_type", "type"));
                baitEffect.setSubcategory(firstNonBlank(
                        text(baitEffectNode, "subcategory"),
                        text(baitEffectNode, "subCategory"),
                        text(baitEffectNode, "sub_category")
                ));
                baitEffect.setChance(resolveRequiredBigDecimal(baitEffectNode, "chance"));
                baitEffect.setValue(resolveBigDecimal(baitEffectNode, "value"));
                baitEffect.setRawJson(toJson(baitEffectNode));

                baitEffectRepository.save(baitEffect);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la lecture du fichier item : " + filePath, e);
        }
    }

    private boolean isJsonFile(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json");
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

    private BigDecimal resolveRequiredBigDecimal(JsonNode node, String... fieldNames) {
        BigDecimal value = resolveBigDecimal(node, fieldNames);
        if (value == null) {
            throw new IllegalStateException("Champ décimal obligatoire introuvable");
        }
        return value;
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

    private String text(JsonNode node, String fieldName) {
        JsonNode child = node.get(fieldName);
        if (child == null || child.isNull() || !child.isTextual()) {
            return null;
        }

        String value = child.asText();
        return value == null || value.isBlank() ? null : value.trim();
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
            throw new IllegalStateException("Impossible de sérialiser un bait effect JSON", e);
        }
    }
}