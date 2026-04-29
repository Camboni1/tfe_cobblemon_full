package be.loic.tfe_cobblemon.dataset.importer.spawn.service.impl;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.spawn.service.SpawnSourceFileImportService;
import be.loic.tfe_cobblemon.spawn.entity.SpawnSourceFile;
import be.loic.tfe_cobblemon.spawn.repository.SpawnSourceFileRepository;
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
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SpawnSourceFileImportServiceImpl implements SpawnSourceFileImportService {

    private final SpawnSourceFileRepository spawnSourceFileRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void importSpawnSourceFiles(DatasetVersion datasetVersion, Path datasetRoot) {
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
                    .forEach(path -> importSingleSpawnSourceFile(datasetVersion, spawnsDirectory, path));
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors du parcours du dossier spawns : " + spawnsDirectory, e);
        }
    }

    private void importSingleSpawnSourceFile(DatasetVersion datasetVersion, Path spawnsDirectory, Path filePath) {
        try {
            String rawJson = Files.readString(filePath, StandardCharsets.UTF_8);
            JsonNode rootNode = objectMapper.readTree(rawJson);
            String filename = spawnsDirectory.relativize(filePath).toString().replace('\\', '/');

            SpawnSourceFile spawnSourceFile = spawnSourceFileRepository
                    .findByDatasetVersionIdAndFilename(datasetVersion.getId(), filename)
                    .orElseGet(SpawnSourceFile::new);

            spawnSourceFile.setDatasetVersion(datasetVersion);
            spawnSourceFile.setFilename(filename);
            spawnSourceFile.setCommentText(firstNonBlank(
                    text(rootNode, "comment"),
                    text(rootNode, "commentText"),
                    text(rootNode, "comment_text")
            ));
            spawnSourceFile.setEnabled(resolveBoolean(rootNode, true, "enabled"));
            spawnSourceFile.setNeededInstalledModsJson(resolveJson(rootNode, "neededInstalledMods", "needed_installed_mods"));
            spawnSourceFile.setNeededUninstalledModsJson(resolveJson(rootNode, "neededUninstalledMods", "needed_uninstalled_mods"));
            spawnSourceFile.setRawJson(rawJson);

            spawnSourceFileRepository.save(spawnSourceFile);
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la lecture du fichier spawn : " + filePath, e);
        }
    }

    private boolean isJsonFile(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json");
    }

    private String resolveJson(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode child = node.get(fieldName);
            if (child != null && !child.isNull()) {
                return toJson(child);
            }
        }
        return null;
    }

    private boolean resolveBoolean(JsonNode node, boolean defaultValue, String fieldName) {
        JsonNode child = node.get(fieldName);
        if (child == null || child.isNull()) {
            return defaultValue;
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

        return defaultValue;
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

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Impossible de sérialiser un spawn source file JSON", e);
        }
    }
}