package be.loic.tfe_cobblemon.dataset.importer.drop.service.impl;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.drop.service.PokemonDropImportService;
import be.loic.tfe_cobblemon.drop.entity.PokemonDrop;
import be.loic.tfe_cobblemon.drop.repository.PokemonDropRepository;
import be.loic.tfe_cobblemon.item.entity.Item;
import be.loic.tfe_cobblemon.item.repository.ItemRepository;
import be.loic.tfe_cobblemon.pokemon.entity.Pokemon;
import be.loic.tfe_cobblemon.pokemon.entity.PokemonForm;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonFormRepository;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PokemonDropImportServiceImpl implements PokemonDropImportService {

    private static final Logger log = LoggerFactory.getLogger(PokemonDropImportServiceImpl.class);

    private final PokemonRepository pokemonRepository;
    private final PokemonFormRepository pokemonFormRepository;
    private final PokemonDropRepository pokemonDropRepository;
    private final ItemRepository itemRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void importPokemonDrops(DatasetVersion datasetVersion, Path datasetRoot) {
        Path pokemonDirectory = datasetRoot.resolve("pokemon");

        if (!Files.exists(pokemonDirectory)) {
            throw new IllegalStateException("Le dossier pokemon est introuvable : " + pokemonDirectory);
        }

        if (!Files.isDirectory(pokemonDirectory)) {
            throw new IllegalStateException("Le chemin pokemon n'est pas un dossier : " + pokemonDirectory);
        }

        try (Stream<Path> pathStream = Files.walk(pokemonDirectory)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(this::isJsonFile)
                    .sorted(Comparator.naturalOrder())
                    .forEach(path -> importDropsFromSinglePokemonFile(datasetVersion, pokemonDirectory, path));
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors du parcours du dossier pokemon : " + pokemonDirectory, e);
        }
    }

    private void importDropsFromSinglePokemonFile(DatasetVersion datasetVersion, Path pokemonDirectory, Path filePath) {
        try {
            String rawJson = Files.readString(filePath, StandardCharsets.UTF_8);
            JsonNode rootNode = objectMapper.readTree(rawJson);

            String slug = resolveSlug(rootNode, pokemonDirectory, filePath);

            Pokemon pokemon = pokemonRepository.findByDatasetVersionIdAndSlug(datasetVersion.getId(), slug)
                    .orElseThrow(() -> new IllegalStateException("Pokémon introuvable pour le slug : " + slug));

            List<JsonNode> formNodes = resolveFormNodes(rootNode);

            for (int index = 0; index < formNodes.size(); index++) {
                JsonNode formNode = formNodes.get(index);
                String formCode = resolveFormCode(formNode, rootNode, index);

                PokemonForm pokemonForm = pokemonFormRepository.findByPokemonIdAndCode(pokemon.getId(), formCode)
                        .orElseThrow(() -> new IllegalStateException(
                                "Form introuvable pour le Pokémon " + slug + " et le code " + formCode
                        ));

                DropBlock dropBlock = resolveDropBlock(formNode, rootNode, index);

                List<PokemonDrop> existingDrops = pokemonDropRepository.findAllByPokemonFormIdOrderByIdAsc(pokemonForm.getId());
                if (!existingDrops.isEmpty()) {
                    pokemonDropRepository.deleteAll(existingDrops);
                }

                for (JsonNode dropNode : dropBlock.entries()) {
                    Item item = resolveItem(datasetVersion, dropNode);
                    if (item == null) {
                        continue;
                    }

                    ShortRange quantityRange = resolveQuantityRange(dropNode);

                    PokemonDrop pokemonDrop = new PokemonDrop();
                    pokemonDrop.setPokemonForm(pokemonForm);
                    pokemonDrop.setItem(item);
                    pokemonDrop.setDropPoolAmountMin(dropBlock.poolAmountMin());
                    pokemonDrop.setDropPoolAmountMax(dropBlock.poolAmountMax());
                    pokemonDrop.setQuantityMin(quantityRange.min());
                    pokemonDrop.setQuantityMax(quantityRange.max());
                    pokemonDrop.setPercentage(resolvePercentage(dropNode));
                    pokemonDrop.setRawJson(toJson(dropNode));

                    pokemonDropRepository.save(pokemonDrop);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lors de la lecture du fichier Pokémon : " + filePath, e);
        }
    }

    private List<JsonNode> resolveFormNodes(JsonNode rootNode) {
        JsonNode formsNode = rootNode.get("forms");
        if (formsNode != null && formsNode.isArray() && !formsNode.isEmpty()) {
            List<JsonNode> formNodes = new ArrayList<>();

            boolean hasExplicitDefault = false;
            for (JsonNode formNode : formsNode) {
                Boolean explicit = resolveBooleanOrNull(formNode, "isDefault", "is_default");
                if (Boolean.TRUE.equals(explicit)) {
                    hasExplicitDefault = true;
                    break;
                }
                String code = firstNonBlank(
                        text(formNode, "code"),
                        text(formNode, "name"),
                        text(formNode, "form"),
                        text(formNode, "id")
                );
                if (code != null) {
                    String normalized = normalizeCode(code);
                    if ("default".equals(normalized) || "base".equals(normalized) || "normal".equals(normalized)) {
                        hasExplicitDefault = true;
                        break;
                    }
                }
            }

            if (!hasExplicitDefault) {
                ObjectNode syntheticDefault = objectMapper.createObjectNode();
                syntheticDefault.put("code", "default");
                syntheticDefault.put("isDefault", true);
                formNodes.add(syntheticDefault);
            }

            formsNode.forEach(formNodes::add);
            return formNodes;
        }

        List<JsonNode> single = new ArrayList<>();
        single.add(rootNode);
        return single;
    }

    private Boolean resolveBooleanOrNull(JsonNode node, String... fieldNames) {
        JsonNode child = firstNode(node, fieldNames);
        if (child == null || child.isNull() || !child.isBoolean()) {
            return null;
        }
        return child.asBoolean();
    }

    private DropBlock resolveDropBlock(JsonNode formNode, JsonNode rootNode, int index) {
        JsonNode formDrops = formNode.get("drops");
        if (formDrops != null && !formDrops.isNull()) {
            return extractDropBlock(formDrops);
        }

        if (index == 0) {
            JsonNode rootDrops = rootNode.get("drops");
            if (rootDrops != null && !rootDrops.isNull()) {
                return extractDropBlock(rootDrops);
            }
        }

        return new DropBlock(List.of(), null, null);
    }

    private DropBlock extractDropBlock(JsonNode dropsNode) {
        if (dropsNode.isArray()) {
            List<JsonNode> nodes = new ArrayList<>();
            dropsNode.forEach(nodes::add);
            return new DropBlock(nodes, null, null);
        }

        if (dropsNode.isObject()) {
            JsonNode entriesNode = firstNode(dropsNode, "entries", "drops", "items");
            List<JsonNode> entries = new ArrayList<>();

            if (entriesNode != null && entriesNode.isArray()) {
                entriesNode.forEach(entries::add);
            }

            ShortRange poolAmountRange = resolvePoolAmountRange(dropsNode);
            return new DropBlock(entries, poolAmountRange.min(), poolAmountRange.max());
        }

        return new DropBlock(List.of(), null, null);
    }

    private ShortRange resolvePoolAmountRange(JsonNode dropsNode) {
        ShortRange range = resolveRange(firstNode(dropsNode, "amountRange", "amount_range"));
        if (range != null) {
            return range;
        }

        range = resolveRange(firstNode(dropsNode, "amount"));
        if (range != null) {
            return range;
        }

        return new ShortRange(null, null);
    }

    private ShortRange resolveQuantityRange(JsonNode dropNode) {
        ShortRange range = resolveRange(firstNode(dropNode, "quantityRange", "quantity_range"));
        if (range != null) {
            return range;
        }

        Short directMin = resolveShort(dropNode, "quantityMin", "quantity_min", "minQuantity", "min_quantity");
        Short directMax = resolveShort(dropNode, "quantityMax", "quantity_max", "maxQuantity", "max_quantity");

        if (directMin != null || directMax != null) {
            Short min = directMin != null ? directMin : directMax;
            Short max = directMax != null ? directMax : directMin;
            return new ShortRange(min, max);
        }

        Short quantity = resolveShort(dropNode, "quantity", "amount");
        if (quantity != null) {
            return new ShortRange(quantity, quantity);
        }

        return new ShortRange(null, null);
    }

    private ShortRange resolveRange(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isNumber()) {
            short value = node.shortValue();
            return new ShortRange(value, value);
        }

        if (!node.isTextual()) {
            return null;
        }

        String raw = node.asText();
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim()
                .replace("..", "-")
                .replace("—", "-")
                .replace("–", "-")
                .replace(",", "-");

        if (value.matches("-?\\d+")) {
            short parsed = Short.parseShort(value);
            return new ShortRange(parsed, parsed);
        }

        String[] parts = value.split("\\s*-\\s*");
        if (parts.length == 2 && parts[0].matches("-?\\d+") && parts[1].matches("-?\\d+")) {
            short min = Short.parseShort(parts[0]);
            short max = Short.parseShort(parts[1]);
            return new ShortRange(min, max);
        }

        return null;
    }

    private Item resolveItem(DatasetVersion datasetVersion, JsonNode dropNode) {
        String rawItemId = firstNonBlank(
                text(dropNode, "item"),
                text(dropNode, "itemId"),
                text(dropNode, "item_id"),
                text(dropNode, "identifier")
        );

        if (rawItemId == null) {
            JsonNode itemNode = dropNode.get("item");
            if (itemNode != null && itemNode.isObject()) {
                rawItemId = firstNonBlank(
                        text(itemNode, "id"),
                        text(itemNode, "item"),
                        text(itemNode, "itemId"),
                        text(itemNode, "item_id"),
                        text(itemNode, "identifier")
                );
            }
        }

        if (rawItemId == null) {
            return null;
        }

        String normalized = normalizeNamespacedLikeValue(rawItemId);

        Optional<Item> exact = itemRepository.findByDatasetVersionIdAndNamespacedId(datasetVersion.getId(), normalized);
        if (exact.isPresent()) {
            return exact.get();
        }

        if (!normalized.contains(":")) {
            Optional<Item> cobblemon = itemRepository.findByDatasetVersionIdAndNamespacedId(datasetVersion.getId(), "cobblemon:" + normalized);
            if (cobblemon.isPresent()) {
                return cobblemon.get();
            }

            Optional<Item> minecraft = itemRepository.findByDatasetVersionIdAndNamespacedId(datasetVersion.getId(), "minecraft:" + normalized);
            if (minecraft.isPresent()) {
                return minecraft.get();
            }

            normalized = "minecraft:" + normalized;
        }

        return createPlaceholderItem(datasetVersion, normalized);
    }

    private Item createPlaceholderItem(DatasetVersion datasetVersion, String namespacedId) {
        Optional<Item> existing = itemRepository.findByDatasetVersionIdAndNamespacedId(datasetVersion.getId(), namespacedId);
        if (existing.isPresent()) {
            return existing.get();
        }

        String namespace = extractNamespace(namespacedId);
        String path = extractPath(namespacedId);

        Item item = new Item();
        item.setDatasetVersion(datasetVersion);
        item.setNamespacedId(namespacedId);
        item.setNamespace(namespace);
        item.setPath(path);
        item.setDisplayName(humanize(path));
        item.setGeneratedPlaceholder(true);
        item.setRawJson(null);

        Item saved = itemRepository.save(item);

        log.warn("Item placeholder créé automatiquement pour les drops : {}", namespacedId);

        return saved;
    }

    private String extractNamespace(String namespacedId) {
        int index = namespacedId.indexOf(':');
        if (index > 0) {
            return namespacedId.substring(0, index);
        }
        return "minecraft";
    }

    private String extractPath(String namespacedId) {
        int index = namespacedId.indexOf(':');
        if (index >= 0 && index < namespacedId.length() - 1) {
            return namespacedId.substring(index + 1);
        }
        return namespacedId;
    }

    private BigDecimal resolvePercentage(JsonNode dropNode) {
        JsonNode node = firstNode(dropNode, "percentage", "chance");
        if (node == null || node.isNull()) {
            return BigDecimal.valueOf(100);
        }

        if (node.isNumber()) {
            BigDecimal value = node.decimalValue();
            if (value.compareTo(BigDecimal.ONE) <= 0) {
                return value.multiply(BigDecimal.valueOf(100));
            }
            return value;
        }

        if (node.isTextual()) {
            String value = node.asText().trim().replace("%", "");
            if (!value.isBlank()) {
                BigDecimal decimal = new BigDecimal(value);
                if (decimal.compareTo(BigDecimal.ONE) <= 0) {
                    return decimal.multiply(BigDecimal.valueOf(100));
                }
                return decimal;
            }
        }

        return BigDecimal.valueOf(100);
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
            if (!value.isBlank() && value.matches("-?\\d+")) {
                return Short.valueOf(value);
            }
        }

        return null;
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

    private String resolveSlug(JsonNode rootNode, Path pokemonDirectory, Path filePath) {
        String candidate = firstNonBlank(
                text(rootNode, "slug"),
                text(rootNode, "speciesId"),
                text(rootNode, "species_id")
        );

        if (candidate != null) {
            return normalizeSlug(candidate);
        }

        String relative = pokemonDirectory.relativize(filePath).toString().replace('\\', '/');
        String withoutExtension = removeJsonExtension(relative);
        return normalizeSlug(lastSegment(withoutExtension));
    }

    private String resolveFormCode(JsonNode formNode, JsonNode rootNode, int index) {
        String candidate = firstNonBlank(
                text(formNode, "code"),
                text(formNode, "name"),
                text(formNode, "form"),
                text(formNode, "id"),
                text(rootNode, "defaultForm"),
                text(rootNode, "default_form")
        );

        if (candidate == null || candidate.isBlank()) {
            return index == 0 ? "default" : "form-" + (index + 1);
        }

        String normalized = normalizeCode(candidate);
        return normalized.isBlank() ? (index == 0 ? "default" : "form-" + (index + 1)) : normalized;
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

    private String normalizeSlug(String value) {
        return value.trim()
                .replace('\\', '/')
                .toLowerCase(Locale.ROOT)
                .replace(' ', '-')
                .replace('_', '-');
    }

    private String normalizeCode(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '-')
                .replace('_', '-');
    }

    private String normalizeNamespacedLikeValue(String value) {
        return value.trim()
                .replace('\\', '/')
                .toLowerCase(Locale.ROOT);
    }

    private String removeJsonExtension(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".json")) {
            return value.substring(0, value.length() - 5);
        }
        return value;
    }

    private String lastSegment(String value) {
        String normalized = value.replace('\\', '/');
        int index = normalized.lastIndexOf('/');
        if (index >= 0 && index < normalized.length() - 1) {
            return normalized.substring(index + 1);
        }
        return normalized;
    }

    private String humanize(String value) {
        String normalized = value.replace('-', ' ').replace('_', ' ').trim();
        if (normalized.isBlank()) {
            return value;
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
            throw new IllegalStateException("Impossible de sérialiser un drop JSON", e);
        }
    }

    private record DropBlock(List<JsonNode> entries, Short poolAmountMin, Short poolAmountMax) {
    }

    private record ShortRange(Short min, Short max) {
    }
}