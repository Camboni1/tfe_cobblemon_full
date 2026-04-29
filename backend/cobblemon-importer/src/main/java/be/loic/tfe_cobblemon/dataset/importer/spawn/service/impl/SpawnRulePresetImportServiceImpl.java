package be.loic.tfe_cobblemon.dataset.importer.spawn.service.impl;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.spawn.service.SpawnRulePresetImportService;
import be.loic.tfe_cobblemon.spawn.entity.SpawnPreset;
import be.loic.tfe_cobblemon.spawn.entity.SpawnRule;
import be.loic.tfe_cobblemon.spawn.entity.SpawnSourceFile;
import be.loic.tfe_cobblemon.spawn.repository.SpawnPresetRepository;
import be.loic.tfe_cobblemon.spawn.repository.SpawnRuleRepository;
import be.loic.tfe_cobblemon.spawn.repository.SpawnSourceFileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SpawnRulePresetImportServiceImpl implements SpawnRulePresetImportService {

    private final SpawnSourceFileRepository spawnSourceFileRepository;
    private final SpawnRuleRepository spawnRuleRepository;
    private final SpawnPresetRepository spawnPresetRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void importSpawnRulePresets(DatasetVersion datasetVersion, Path datasetRoot) {
        Path spawnsDirectory = datasetRoot.resolve("spawns");

        if (!Files.exists(spawnsDirectory)) {
            throw new IllegalStateException("Le dossier spawns est introuvable : " + spawnsDirectory);
        }

        if (!Files.isDirectory(spawnsDirectory)) {
            throw new IllegalStateException("Le chemin spawns n'est pas un dossier : " + spawnsDirectory);
        }

        try (Stream<Path> pathStream = Files.walk(spawnsDirectory)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(this::isJsonFile)
                    .sorted(Comparator.naturalOrder())
                    .forEach(path -> importRulePresetsFromSingleSpawnFile(datasetVersion, spawnsDirectory, path));
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors du parcours du dossier spawns : " + spawnsDirectory, e);
        }
    }

    private void importRulePresetsFromSingleSpawnFile(DatasetVersion datasetVersion, Path spawnsDirectory, Path filePath) {
        try {
            String rawJson = Files.readString(filePath, StandardCharsets.UTF_8);
            JsonNode rootNode = objectMapper.readTree(rawJson);
            String filename = spawnsDirectory.relativize(filePath).toString().replace('\\', '/');

            SpawnSourceFile spawnSourceFile = spawnSourceFileRepository
                    .findByDatasetVersionIdAndFilename(datasetVersion.getId(), filename)
                    .orElseThrow(() -> new IllegalStateException("SpawnSourceFile introuvable pour : " + filename));

            List<JsonNode> ruleNodes = resolveRuleNodes(rootNode);

            if (ruleNodes.isEmpty()) {
                return;
            }

            for (int index = 0; index < ruleNodes.size(); index++) {
                JsonNode ruleNode = ruleNodes.get(index);
                String externalId = resolveExternalId(ruleNode, filename, index);

                SpawnRule spawnRule = spawnRuleRepository
                        .findBySpawnSourceFileIdAndExternalId(spawnSourceFile.getId(), externalId)
                        .orElseThrow(() -> new IllegalStateException("SpawnRule introuvable pour externalId : " + externalId));

                Set<SpawnPreset> presets = resolvePresets(datasetVersion.getId(), ruleNode);

                spawnRule.getPresets().clear();
                spawnRule.getPresets().addAll(presets);

                spawnRuleRepository.save(spawnRule);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la lecture du fichier spawn : " + filePath, e);
        }
    }

    private Set<SpawnPreset> resolvePresets(Long datasetVersionId, JsonNode ruleNode) {
        Set<SpawnPreset> presets = new LinkedHashSet<>();

        JsonNode presetsNode = firstNode(ruleNode, "presets", "preset");
        if (presetsNode == null || presetsNode.isNull()) {
            return presets;
        }

        if (presetsNode.isArray()) {
            for (JsonNode child : presetsNode) {
                String code = extractPresetCode(child);
                if (code != null) {
                    presets.add(findPreset(datasetVersionId, code));
                }
            }
            return presets;
        }

        String code = extractPresetCode(presetsNode);
        if (code != null) {
            presets.add(findPreset(datasetVersionId, code));
        }

        return presets;
    }

    private SpawnPreset findPreset(Long datasetVersionId, String rawCode) {
        String normalizedCode = normalizeCode(rawCode);

        return spawnPresetRepository.findByDatasetVersionIdAndCode(datasetVersionId, normalizedCode)
                .orElseThrow(() -> new IllegalStateException("SpawnPreset introuvable pour le code : " + normalizedCode));
    }

    private String extractPresetCode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isTextual()) {
            String value = node.asText().trim();
            return value.isBlank() ? null : value;
        }

        if (node.isObject()) {
            return firstNonBlank(
                    text(node, "code"),
                    text(node, "name"),
                    text(node, "id"),
                    text(node, "preset")
            );
        }

        return null;
    }

    private List<JsonNode> resolveRuleNodes(JsonNode rootNode) {
        if (rootNode == null || rootNode.isNull()) {
            return List.of();
        }

        if (rootNode.isArray()) {
            List<JsonNode> nodes = new ArrayList<>();
            rootNode.forEach(nodes::add);
            return nodes;
        }

        JsonNode spawnsNode = firstNode(rootNode, "spawns", "spawnPool", "spawn_pool", "entries", "rules");
        if (spawnsNode != null && spawnsNode.isArray()) {
            List<JsonNode> nodes = new ArrayList<>();
            spawnsNode.forEach(nodes::add);
            return nodes;
        }

        if (looksLikeSpawnRuleNode(rootNode)) {
            return List.of(rootNode);
        }

        return List.of();
    }

    private boolean looksLikeSpawnRuleNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            return false;
        }

        if (node.hasNonNull("pokemon")
                || node.hasNonNull("species")
                || node.hasNonNull("speciesId")
                || node.hasNonNull("species_id")
                || node.hasNonNull("slug")) {
            return true;
        }

        if (node.hasNonNull("herdablePokemon") || node.hasNonNull("herdable_pokemon")) {
            return true;
        }

        return node.hasNonNull("weight")
                && (node.hasNonNull("context")
                || node.hasNonNull("type")
                || node.hasNonNull("bucket")
                || node.hasNonNull("condition")
                || node.hasNonNull("anticondition"));
    }

    private String resolveExternalId(JsonNode ruleNode, String filename, int index) {
        String value = firstNonBlank(
                text(ruleNode, "id"),
                text(ruleNode, "externalId"),
                text(ruleNode, "external_id")
        );

        if (value != null) {
            return truncate(value.trim(), 40);
        }

        String fallback = removeJsonExtension(filename).replace('/', '-') + "-" + (index + 1);
        return truncate(fallback, 40);
    }

    private JsonNode firstNode(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) {
            return null;
        }

        for (String fieldName : fieldNames) {
            JsonNode child = node.get(fieldName);
            if (child != null && !child.isNull()) {
                return child;
            }
        }
        return null;
    }

    private boolean isJsonFile(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json");
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }

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

    private String normalizeCode(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '-')
                .replace('_', '-');
    }

    private String removeJsonExtension(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".json")) {
            return value.substring(0, value.length() - 5);
        }
        return value;
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}