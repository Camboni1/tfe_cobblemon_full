package be.loic.tfe_cobblemon.dataset.importer.item.service.impl;

import be.loic.tfe_cobblemon.dataset.importer.item.entity.ItemCandidate;
import be.loic.tfe_cobblemon.dataset.importer.item.entity.ItemCandidateSource;
import be.loic.tfe_cobblemon.dataset.importer.item.service.ItemMetadataResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class ItemMetadataResolverImpl implements ItemMetadataResolver {

    private final ObjectMapper objectMapper;

    private final ConcurrentMap<Path, Optional<JsonNode>> jsonCache = new ConcurrentHashMap<>();

    @Override
    public Optional<ItemCandidate> resolveMetadataCandidate(String namespacedId, Path datasetRoot) {
        return resolveManualOverride(namespacedId, datasetRoot)
                .or(() -> resolveCobblemonLang(namespacedId, datasetRoot))
                .or(() -> resolveMinecraftCatalog(namespacedId, datasetRoot));
    }

    private Optional<ItemCandidate> resolveManualOverride(String namespacedId, Path datasetRoot) {
        Optional<JsonNode> root = readFirstExistingJson(
                datasetRoot,
                "cobblemon-item-overrides.json",
                "metadata/cobblemon-item-overrides.json",
                "items/cobblemon-item-overrides.json"
        );

        if (root.isEmpty()) {
            return Optional.empty();
        }

        String namespace = extractNamespace(namespacedId);
        String path = extractPath(namespacedId);

        JsonNode entry = findEntry(root.get(), namespacedId, path);
        if (entry == null || entry.isNull()) {
            return Optional.empty();
        }

        String displayName = resolveDisplayName(entry, humanize(path));
        String rawJson = buildMetadataJson(
                "manual_override",
                namespacedId,
                namespace,
                path,
                displayName,
                entry
        );

        return Optional.of(new ItemCandidate(
                namespacedId,
                namespace,
                path,
                displayName,
                rawJson,
                true,
                ItemCandidateSource.MANUAL_OVERRIDE,
                Set.of()
        ));
    }

    private Optional<ItemCandidate> resolveCobblemonLang(String namespacedId, Path datasetRoot) {
        if (!namespacedId.startsWith("cobblemon:")) {
            return Optional.empty();
        }

        Optional<JsonNode> root = readFirstExistingJson(
                datasetRoot,
                "lang/en_us.json",
                "assets/cobblemon/lang/en_us.json",
                "items/lang/en_us.json",
                "items/cobblemon/lang/en_us.json"
        );

        if (root.isEmpty() || !root.get().isObject()) {
            return Optional.empty();
        }

        String path = extractPath(namespacedId);
        List<String> languageKeys = languageKeysForPath(path);
        String displayName = firstNonBlankText(root.get(), languageKeys);

        if (displayName == null) {
            return Optional.empty();
        }

        List<String> tooltips = resolveTooltips(root.get(), languageKeys);

        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("source", "cobblemon_lang");
        metadata.put("namespacedId", namespacedId);
        metadata.put("namespace", "cobblemon");
        metadata.put("path", path);
        metadata.put("displayName", displayName);
        metadata.put("langKey", languageKeys.get(0));

        ArrayNode tooltipsNode = metadata.putArray("tooltips");
        tooltips.forEach(tooltipsNode::add);

        return Optional.of(new ItemCandidate(
                namespacedId,
                "cobblemon",
                path,
                displayName,
                toJson(metadata),
                true,
                ItemCandidateSource.COBBLEMON_LANG,
                Set.of()
        ));
    }

    private Optional<ItemCandidate> resolveMinecraftCatalog(String namespacedId, Path datasetRoot) {
        if (!namespacedId.startsWith("minecraft:")) {
            return Optional.empty();
        }

        Optional<JsonNode> root = readFirstExistingJson(
                datasetRoot,
                "minecraft-items.json",
                "metadata/minecraft-items.json",
                "items/minecraft-items.json"
        );

        if (root.isEmpty()) {
            return Optional.empty();
        }

        String namespace = extractNamespace(namespacedId);
        String path = extractPath(namespacedId);

        JsonNode entry = findEntry(root.get(), namespacedId, path);
        if (entry == null || entry.isNull()) {
            return Optional.empty();
        }

        String displayName = resolveDisplayName(entry, humanize(path));
        String rawJson = buildMetadataJson(
                "minecraft_catalog",
                namespacedId,
                namespace,
                path,
                displayName,
                entry
        );

        return Optional.of(new ItemCandidate(
                namespacedId,
                namespace,
                path,
                displayName,
                rawJson,
                true,
                ItemCandidateSource.MINECRAFT_CATALOG,
                Set.of()
        ));
    }

    private Optional<JsonNode> readFirstExistingJson(Path datasetRoot, String... relativePaths) {
        for (String relativePath : relativePaths) {
            Optional<JsonNode> root = readJson(datasetRoot.resolve(relativePath));
            if (root.isPresent()) {
                return root;
            }
        }

        return Optional.empty();
    }

    private Optional<JsonNode> readJson(Path path) {
        Path normalizedPath = path.toAbsolutePath().normalize();
        return jsonCache.computeIfAbsent(normalizedPath, this::loadJson);
    }

    private Optional<JsonNode> loadJson(Path path) {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readTree(path.toFile()));
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le JSON de metadata : " + path, e);
        }
    }

    private JsonNode findEntry(JsonNode root, String namespacedId, String path) {
        if (root == null || root.isNull()) {
            return null;
        }

        if (root.isObject()) {
            JsonNode directByNamespacedId = root.get(namespacedId);
            if (directByNamespacedId != null && !directByNamespacedId.isNull()) {
                return directByNamespacedId;
            }

            JsonNode directByPath = root.get(path);
            if (directByPath != null && !directByPath.isNull()) {
                return directByPath;
            }

            for (String containerName : List.of("items", "entries", "values", "overrides")) {
                JsonNode container = root.get(containerName);
                JsonNode entry = findEntry(container, namespacedId, path);
                if (entry != null && !entry.isNull()) {
                    return entry;
                }
            }

            if (matchesEntry(root, namespacedId, path)) {
                return root;
            }
        }

        if (root.isArray()) {
            for (JsonNode child : root) {
                if (matchesEntry(child, namespacedId, path)) {
                    return child;
                }
            }
        }

        return null;
    }

    private boolean matchesEntry(JsonNode entry, String namespacedId, String path) {
        if (entry == null || entry.isNull() || !entry.isObject()) {
            return false;
        }

        String entryNamespacedId = firstNonBlankText(entry, List.of(
                "namespacedId",
                "namespaced_id",
                "identifier",
                "resource",
                "id"
        ));

        if (namespacedId.equals(entryNamespacedId) || path.equals(entryNamespacedId)) {
            return true;
        }

        String entryNamespace = firstNonBlankText(entry, List.of("namespace"));
        String entryPath = firstNonBlankText(entry, List.of("path", "itemPath", "item_path"));

        if (entryNamespace != null && entryPath != null) {
            return namespacedId.equals(entryNamespace + ":" + entryPath);
        }

        return path.equals(entryPath);
    }

    private String resolveDisplayName(JsonNode entry, String fallback) {
        if (entry == null || entry.isNull()) {
            return fallback;
        }

        if (entry.isTextual()) {
            String value = entry.asText();
            return value == null || value.isBlank() ? fallback : value.trim();
        }

        String value = firstNonBlankText(entry, List.of(
                "displayName",
                "display_name",
                "translatedName",
                "translated_name",
                "name",
                "title"
        ));

        return value != null ? value : fallback;
    }

    private String buildMetadataJson(
            String source,
            String namespacedId,
            String namespace,
            String path,
            String displayName,
            JsonNode payload
    ) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("source", source);
        root.put("namespacedId", namespacedId);
        root.put("namespace", namespace);
        root.put("path", path);
        root.put("displayName", displayName);

        if (payload != null && !payload.isNull()) {
            root.set("metadata", payload.deepCopy());
        }

        return toJson(root);
    }

    private List<String> languageKeysForPath(String path) {
        String normalizedPath = path.replace('\\', '/').toLowerCase(Locale.ROOT);

        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.add("item.cobblemon." + normalizedPath.replace('/', '.'));

        if (normalizedPath.contains("/")) {
            keys.add("item.cobblemon." + normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1));
        }

        if (normalizedPath.startsWith("berries/")) {
            keys.add("item.cobblemon." + normalizedPath.substring("berries/".length()));
        }

        return List.copyOf(keys);
    }

    private List<String> resolveTooltips(JsonNode root, List<String> languageKeys) {
        LinkedHashSet<String> tooltips = new LinkedHashSet<>();

        for (String languageKey : languageKeys) {
            String inlineTooltip = text(root, languageKey + ".tooltip");
            if (inlineTooltip != null) {
                tooltips.add(inlineTooltip);
            }

            for (int index = 1; index <= 5; index++) {
                String indexedTooltip = text(root, languageKey + ".tooltip_" + index);
                if (indexedTooltip != null) {
                    tooltips.add(indexedTooltip);
                }
            }
        }

        return List.copyOf(tooltips);
    }

    private String firstNonBlankText(JsonNode node, List<String> fieldNames) {
        for (String fieldName : fieldNames) {
            String value = text(node, fieldName);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || !node.isObject()) {
            return null;
        }

        JsonNode child = node.get(fieldName);
        if (child == null || child.isNull() || !child.isTextual()) {
            return null;
        }

        String value = child.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String extractNamespace(String namespacedId) {
        int separatorIndex = namespacedId.indexOf(':');
        return separatorIndex >= 0 ? namespacedId.substring(0, separatorIndex) : "cobblemon";
    }

    private String extractPath(String namespacedId) {
        int separatorIndex = namespacedId.indexOf(':');
        return separatorIndex >= 0 ? namespacedId.substring(separatorIndex + 1) : namespacedId;
    }

    private String humanize(String itemPath) {
        String lastSegment = itemPath.contains("/")
                ? itemPath.substring(itemPath.lastIndexOf('/') + 1)
                : itemPath;

        String normalized = lastSegment.replace('-', ' ').replace('_', ' ').trim();
        if (normalized.isBlank()) {
            return itemPath;
        }

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

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Impossible de serialiser le JSON de metadata item.", e);
        }
    }
}
