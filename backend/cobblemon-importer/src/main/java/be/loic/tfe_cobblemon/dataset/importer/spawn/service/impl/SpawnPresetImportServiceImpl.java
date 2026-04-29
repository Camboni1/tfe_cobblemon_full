package be.loic.tfe_cobblemon.dataset.importer.spawn.service.impl;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.spawn.service.SpawnPresetImportService;
import be.loic.tfe_cobblemon.spawn.entity.SpawnPreset;
import be.loic.tfe_cobblemon.spawn.repository.SpawnPresetRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SpawnPresetImportServiceImpl implements SpawnPresetImportService {

    private final SpawnPresetRepository spawnPresetRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void importSpawnPresets(DatasetVersion datasetVersion, Path datasetRoot) {
        Path presetsDirectory = datasetRoot.resolve("presets");

        if (!Files.exists(presetsDirectory)) {
            throw new IllegalStateException("Le dossier presets est introuvable : " + presetsDirectory);
        }

        if (!Files.isDirectory(presetsDirectory)) {
            throw new IllegalStateException("Le chemin presets n'est pas un dossier : " + presetsDirectory);
        }

        try (Stream<Path> pathStream = Files.walk(presetsDirectory)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(this::isJsonFile)
                    .sorted(Comparator.naturalOrder())
                    .forEach(path -> importPresetFile(datasetVersion, presetsDirectory, path));
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors du parcours du dossier presets : " + presetsDirectory, e);
        }
    }

    private void importPresetFile(DatasetVersion datasetVersion, Path presetsDirectory, Path filePath) {
        try {
            String rawJson = Files.readString(filePath, StandardCharsets.UTF_8);
            JsonNode rootNode = objectMapper.readTree(rawJson);
            String sourceFile = presetsDirectory.relativize(filePath).toString().replace('\\', '/');

            List<JsonNode> presetNodes = resolvePresetNodes(rootNode);

            for (int index = 0; index < presetNodes.size(); index++) {
                JsonNode presetNode = presetNodes.get(index);
                String code = resolveCode(presetNode, sourceFile, index);

                SpawnPreset spawnPreset = spawnPresetRepository
                        .findByDatasetVersionIdAndCode(datasetVersion.getId(), code)
                        .orElseGet(SpawnPreset::new);

                spawnPreset.setDatasetVersion(datasetVersion);
                spawnPreset.setCode(code);
                spawnPreset.setSourceFile(sourceFile);
                spawnPreset.setConditionJson(resolveJson(presetNode, "condition"));
                spawnPreset.setAnticonditionJson(resolveJson(presetNode, "anticondition"));
                spawnPreset.setRawJson(toJson(presetNode));

                spawnPresetRepository.save(spawnPreset);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la lecture du fichier preset : " + filePath, e);
        }
    }

    private List<JsonNode> resolvePresetNodes(JsonNode rootNode) {
        if (rootNode.isArray()) {
            List<JsonNode> nodes = new ArrayList<>();
            rootNode.forEach(nodes::add);
            return nodes;
        }

        JsonNode presetsNode = rootNode.get("presets");
        if (presetsNode != null && presetsNode.isArray()) {
            List<JsonNode> nodes = new ArrayList<>();
            presetsNode.forEach(nodes::add);
            return nodes;
        }

        return List.of(rootNode);
    }

    private String resolveCode(JsonNode presetNode, String sourceFile, int index) {
        String code = firstNonBlank(
                text(presetNode, "code"),
                text(presetNode, "name"),
                text(presetNode, "id"),
                text(presetNode, "preset")
        );

        if (code != null && !code.isBlank()) {
            return normalizeCode(code);
        }

        String fallback = removeJsonExtension(sourceFile).replace('/', '-');
        if (index == 0) {
            return normalizeCode(fallback);
        }

        return normalizeCode(fallback + "-" + (index + 1));
    }

    private String resolveJson(JsonNode node, String fieldName) {
        JsonNode child = node.get(fieldName);
        if (child == null || child.isNull()) {
            return null;
        }
        return toJson(child);
    }

    private boolean isJsonFile(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json");
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

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Impossible de sérialiser un preset JSON", e);
        }
    }
}