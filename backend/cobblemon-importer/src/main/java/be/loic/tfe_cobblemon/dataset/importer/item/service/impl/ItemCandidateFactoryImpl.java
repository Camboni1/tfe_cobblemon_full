package be.loic.tfe_cobblemon.dataset.importer.item.service.impl;

import be.loic.tfe_cobblemon.dataset.importer.item.entity.ItemCandidate;
import be.loic.tfe_cobblemon.dataset.importer.item.entity.ItemCandidateSource;
import be.loic.tfe_cobblemon.dataset.importer.item.service.ItemAliasResolver;
import be.loic.tfe_cobblemon.dataset.importer.item.service.ItemCandidateFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ItemCandidateFactoryImpl implements ItemCandidateFactory {

    private final ObjectMapper objectMapper;
    private final ItemAliasResolver itemAliasResolver;

    @Override
    public Optional<ItemCandidate> buildFromFile(Path itemsDirectory, Path filePath) {
        try {
            String rawJson = Files.readString(filePath, StandardCharsets.UTF_8);
            JsonNode rootNode = objectMapper.readTree(rawJson);

            String detectedNamespacedId = resolveNamespacedId(rootNode, itemsDirectory, filePath);
            String canonicalNamespacedId = itemAliasResolver.resolveCanonicalNamespacedId(detectedNamespacedId);
            Set<String> shadowIds = itemAliasResolver.resolveShadowNamespacedIds(canonicalNamespacedId, detectedNamespacedId);

            String namespace = extractNamespace(canonicalNamespacedId);
            String path = extractPath(canonicalNamespacedId);
            String displayName = resolveDisplayName(rootNode, path);

            return Optional.of(new ItemCandidate(
                    canonicalNamespacedId,
                    namespace,
                    path,
                    displayName,
                    rawJson,
                    false,
                    canonicalNamespacedId.equals(detectedNamespacedId)
                            ? ItemCandidateSource.DIRECT_JSON
                            : ItemCandidateSource.ALIAS_TO_DIRECT_JSON,
                    shadowIds
            ));
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la lecture du fichier item : " + filePath, e);
        }
    }

    private String resolveNamespacedId(JsonNode rootNode, Path itemsDirectory, Path filePath) {
        String explicitNamespacedId = firstNonBlank(
                text(rootNode, "namespacedId"),
                text(rootNode, "namespaced_id"),
                text(rootNode, "identifier"),
                text(rootNode, "resource"),
                text(rootNode, "id"),
                text(rootNode, "item")
        );

        if (explicitNamespacedId != null && explicitNamespacedId.contains(":")) {
            return normalizeNamespacedId(explicitNamespacedId);
        }

        String relative = itemsDirectory.relativize(filePath).toString().replace('\\', '/');
        String withoutExtension = removeJsonExtension(relative);

        return namespacedIdFromRelativePath(withoutExtension);
    }

    private String namespacedIdFromRelativePath(String relativePath) {
        String normalized = normalizePath(relativePath);

        int slashIndex = normalized.indexOf('/');
        if (slashIndex > 0 && slashIndex < normalized.length() - 1) {
            String namespace = normalized.substring(0, slashIndex);
            String path = normalized.substring(slashIndex + 1);
            return namespace + ":" + path;
        }

        return "cobblemon:" + normalized;
    }

    private String resolveDisplayName(JsonNode rootNode, String itemPath) {
        String explicitDisplayName = firstNonBlank(
                text(rootNode, "displayName"),
                text(rootNode, "display_name"),
                text(rootNode, "translatedName"),
                text(rootNode, "translated_name")
        );

        if (explicitDisplayName != null) {
            return explicitDisplayName;
        }

        return humanize(itemPath);
    }

    private String extractNamespace(String namespacedId) {
        return namespacedId.substring(0, namespacedId.indexOf(':'));
    }

    private String extractPath(String namespacedId) {
        return namespacedId.substring(namespacedId.indexOf(':') + 1);
    }

    private String normalizeNamespacedId(String value) {
        String trimmed = value.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
        String[] parts = trimmed.split(":", 2);
        return parts[0] + ":" + normalizePath(parts[1]);
    }

    private String normalizePath(String value) {
        return value.trim()
                .replace('\\', '/')
                .replaceAll("^/+", "")
                .replaceAll("/+$", "")
                .toLowerCase(Locale.ROOT);
    }

    private String removeJsonExtension(String value) {
        return value.toLowerCase(Locale.ROOT).endsWith(".json")
                ? value.substring(0, value.length() - 5)
                : value;
    }

    private String humanize(String itemPath) {
        String lastSegment = itemPath.contains("/") ? itemPath.substring(itemPath.lastIndexOf('/') + 1) : itemPath;
        String normalized = lastSegment.replace('-', ' ').replace('_', ' ').trim();

        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.toString();
    }

    private String text(JsonNode rootNode, String fieldName) {
        JsonNode child = rootNode.get(fieldName);
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
}