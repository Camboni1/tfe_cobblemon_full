package be.loic.tfe_cobblemon.dataset.importer.spawn.service.impl;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.spawn.service.SpawnConditionImportService;
import be.loic.tfe_cobblemon.spawn.entity.SpawnCondition;
import be.loic.tfe_cobblemon.spawn.entity.SpawnConditionToken;
import be.loic.tfe_cobblemon.spawn.entity.SpawnRule;
import be.loic.tfe_cobblemon.spawn.entity.SpawnSourceFile;
import be.loic.tfe_cobblemon.spawn.enums.SpawnConditionTokenSide;
import be.loic.tfe_cobblemon.spawn.enums.SpawnConditionTokenType;
import be.loic.tfe_cobblemon.spawn.repository.SpawnConditionRepository;
import be.loic.tfe_cobblemon.spawn.repository.SpawnRuleRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SpawnConditionImportServiceImpl implements SpawnConditionImportService {

    private final SpawnSourceFileRepository spawnSourceFileRepository;
    private final SpawnRuleRepository spawnRuleRepository;
    private final SpawnConditionRepository spawnConditionRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void importSpawnConditions(DatasetVersion datasetVersion, Path datasetRoot) {
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
                    .forEach(path -> importConditionsFromSingleSpawnFile(datasetVersion, spawnsDirectory, path));
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors du parcours du dossier spawns : " + spawnsDirectory, e);
        }
    }

    private void importConditionsFromSingleSpawnFile(DatasetVersion datasetVersion, Path spawnsDirectory, Path filePath) {
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

                SpawnCondition spawnCondition = spawnConditionRepository.findBySpawnRuleId(spawnRule.getId())
                        .orElseGet(SpawnCondition::new);

                JsonNode conditionNode = firstNode(ruleNode, "condition");
                JsonNode anticonditionNode = firstNode(ruleNode, "anticondition");
                JsonNode effectiveConditionNode = firstNode(ruleNode, "effectiveCondition", "effective_condition");
                JsonNode effectiveAnticonditionNode = firstNode(ruleNode, "effectiveAnticondition", "effective_anticondition");

                spawnCondition.setSpawnRule(spawnRule);
                spawnCondition.setCanSeeSky(resolveBooleanFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "canSeeSky", "can_see_sky"
                ));
                spawnCondition.setIsRaining(resolveBooleanFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "isRaining", "is_raining", "raining"
                ));
                spawnCondition.setIsThundering(resolveBooleanFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "isThundering", "is_thundering", "thundering"
                ));
                spawnCondition.setIsSlimeChunk(resolveBooleanFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "isSlimeChunk", "is_slime_chunk", "slimeChunk", "slime_chunk"
                ));
                spawnCondition.setMinX(resolveIntegerFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "minX", "min_x"
                ));
                spawnCondition.setMaxX(resolveIntegerFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "maxX", "max_x"
                ));
                spawnCondition.setMinY(resolveShortFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "minY", "min_y"
                ));
                spawnCondition.setMaxY(resolveShortFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "maxY", "max_y"
                ));
                spawnCondition.setMinLight(resolveShortFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "minLight", "min_light"
                ));
                spawnCondition.setMaxLight(resolveShortFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "maxLight", "max_light"
                ));
                spawnCondition.setMinSkyLight(resolveShortFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "minSkyLight", "min_sky_light"
                ));
                spawnCondition.setMaxSkyLight(resolveShortFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "maxSkyLight", "max_sky_light"
                ));
                spawnCondition.setMinLureLevel(resolveShortFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "minLureLevel", "min_lure_level"
                ));
                spawnCondition.setMaxLureLevel(resolveShortFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "maxLureLevel", "max_lure_level"
                ));
                spawnCondition.setMoonPhase(resolveTextFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "moonPhase", "moon_phase"
                ));
                spawnCondition.setTimeRange(resolveTextFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "timeRange", "time_range"
                ));
                spawnCondition.setRodType(resolveTextFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "rodType", "rod_type"
                ));
                spawnCondition.setBaitItemExpression(resolveTextFromSources(
                        ruleNode, conditionNode, effectiveConditionNode, anticonditionNode, effectiveAnticonditionNode,
                        "baitItemExpression", "bait_item_expression"
                ));
                spawnCondition.setConditionJson(resolveJson(ruleNode, "condition"));
                spawnCondition.setAnticonditionJson(resolveJson(ruleNode, "anticondition"));
                spawnCondition.setEffectiveConditionJson(resolveJson(ruleNode, "effectiveCondition", "effective_condition"));
                spawnCondition.setEffectiveAnticonditionJson(resolveJson(ruleNode, "effectiveAnticondition", "effective_anticondition"));

                spawnCondition.getTokens().clear();
                addTokens(spawnCondition, conditionNode, SpawnConditionTokenSide.CONDITION);
                addTokens(spawnCondition, anticonditionNode, SpawnConditionTokenSide.ANTICONDITION);
                addTokens(spawnCondition, effectiveConditionNode, SpawnConditionTokenSide.CONDITION);
                addTokens(spawnCondition, effectiveAnticonditionNode, SpawnConditionTokenSide.ANTICONDITION);

                spawnRule.setSpawnCondition(spawnCondition);
                spawnRuleRepository.save(spawnRule);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lors de la lecture du fichier spawn : " + filePath, e);
        }
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

    private void addTokens(SpawnCondition spawnCondition, JsonNode sourceNode, SpawnConditionTokenSide side) {
        if (sourceNode == null || sourceNode.isNull() || !sourceNode.isObject()) {
            return;
        }

        addTokensForField(spawnCondition, sourceNode.get("biomes"), side, SpawnConditionTokenType.BIOME);
        addTokensForField(spawnCondition, sourceNode.get("structures"), side, SpawnConditionTokenType.STRUCTURE);
        addTokensForField(spawnCondition, sourceNode.get("dimensions"), side, SpawnConditionTokenType.DIMENSION);
        addTokensForField(spawnCondition, sourceNode.get("nearbyBlocks"), side, SpawnConditionTokenType.NEARBY_BLOCK);
        addTokensForField(spawnCondition, sourceNode.get("nearby_blocks"), side, SpawnConditionTokenType.NEARBY_BLOCK);
        addTokensForField(spawnCondition, sourceNode.get("baseBlocks"), side, SpawnConditionTokenType.BASE_BLOCK);
        addTokensForField(spawnCondition, sourceNode.get("base_blocks"), side, SpawnConditionTokenType.BASE_BLOCK);
        addTokensForField(spawnCondition, sourceNode.get("labels"), side, SpawnConditionTokenType.LABEL);
    }

    private void addTokensForField(
            SpawnCondition spawnCondition,
            JsonNode tokenNode,
            SpawnConditionTokenSide side,
            SpawnConditionTokenType tokenType
    ) {
        if (tokenNode == null || tokenNode.isNull()) {
            return;
        }

        if (tokenNode.isArray()) {
            for (JsonNode child : tokenNode) {
                addSingleToken(spawnCondition, child, side, tokenType);
            }
            return;
        }

        addSingleToken(spawnCondition, tokenNode, side, tokenType);
    }

    private void addSingleToken(
            SpawnCondition spawnCondition,
            JsonNode tokenNode,
            SpawnConditionTokenSide side,
            SpawnConditionTokenType tokenType
    ) {
        if (tokenNode == null || tokenNode.isNull()) {
            return;
        }

        String tokenValue;
        boolean isTag = false;

        if (tokenNode.isTextual()) {
            tokenValue = tokenNode.asText().trim();
            isTag = tokenValue.startsWith("#");
        } else if (tokenNode.isObject()) {
            tokenValue = firstNonBlank(
                    text(tokenNode, "value"),
                    text(tokenNode, "id"),
                    text(tokenNode, "name"),
                    text(tokenNode, "tag")
            );
            Boolean explicitTag = resolveBoolean(tokenNode, "isTag", "is_tag");
            isTag = explicitTag != null ? explicitTag : text(tokenNode, "tag") != null;
        } else {
            return;
        }

        if (tokenValue == null || tokenValue.isBlank()) {
            return;
        }

        if (hasExistingToken(spawnCondition, side, tokenType, tokenValue, isTag)) {
            return;
        }

        SpawnConditionToken token = new SpawnConditionToken();
        token.setSpawnCondition(spawnCondition);
        token.setSide(side);
        token.setTokenType(tokenType);
        token.setTokenValue(tokenValue);
        token.setTag(isTag);

        spawnCondition.getTokens().add(token);
    }

    private boolean hasExistingToken(
            SpawnCondition spawnCondition,
            SpawnConditionTokenSide side,
            SpawnConditionTokenType tokenType,
            String tokenValue,
            boolean isTag
    ) {
        return spawnCondition.getTokens().stream().anyMatch(existing ->
                existing.getSide() == side
                        && existing.getTokenType() == tokenType
                        && tokenValue.equals(existing.getTokenValue())
                        && isTag == existing.isTag()
        );
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

    private Boolean resolveBooleanFromSources(
            JsonNode rootNode,
            JsonNode conditionNode,
            JsonNode effectiveConditionNode,
            JsonNode anticonditionNode,
            JsonNode effectiveAnticonditionNode,
            String... fieldNames
    ) {
        return firstBoolean(
                resolveBoolean(rootNode, fieldNames),
                resolveBoolean(conditionNode, fieldNames),
                resolveBoolean(effectiveConditionNode, fieldNames),
                resolveBoolean(anticonditionNode, fieldNames),
                resolveBoolean(effectiveAnticonditionNode, fieldNames)
        );
    }

    private Integer resolveIntegerFromSources(
            JsonNode rootNode,
            JsonNode conditionNode,
            JsonNode effectiveConditionNode,
            JsonNode anticonditionNode,
            JsonNode effectiveAnticonditionNode,
            String... fieldNames
    ) {
        return firstInteger(
                resolveInteger(rootNode, fieldNames),
                resolveInteger(conditionNode, fieldNames),
                resolveInteger(effectiveConditionNode, fieldNames),
                resolveInteger(anticonditionNode, fieldNames),
                resolveInteger(effectiveAnticonditionNode, fieldNames)
        );
    }

    private Short resolveShortFromSources(
            JsonNode rootNode,
            JsonNode conditionNode,
            JsonNode effectiveConditionNode,
            JsonNode anticonditionNode,
            JsonNode effectiveAnticonditionNode,
            String... fieldNames
    ) {
        return firstShort(
                resolveShort(rootNode, fieldNames),
                resolveShort(conditionNode, fieldNames),
                resolveShort(effectiveConditionNode, fieldNames),
                resolveShort(anticonditionNode, fieldNames),
                resolveShort(effectiveAnticonditionNode, fieldNames)
        );
    }

    private String resolveTextFromSources(
            JsonNode rootNode,
            JsonNode conditionNode,
            JsonNode effectiveConditionNode,
            JsonNode anticonditionNode,
            JsonNode effectiveAnticonditionNode,
            String... fieldNames
    ) {
        return firstNonBlank(
                resolveText(rootNode, fieldNames),
                resolveText(conditionNode, fieldNames),
                resolveText(effectiveConditionNode, fieldNames),
                resolveText(anticonditionNode, fieldNames),
                resolveText(effectiveAnticonditionNode, fieldNames)
        );
    }

    private Boolean firstBoolean(Boolean... values) {
        for (Boolean value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Integer firstInteger(Integer... values) {
        for (Integer value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Short firstShort(Short... values) {
        for (Short value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
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

    private String resolveText(JsonNode node, String... fieldNames) {
        JsonNode child = firstNode(node, fieldNames);
        if (child == null || child.isNull() || !child.isTextual()) {
            return null;
        }

        String value = child.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String resolveJson(JsonNode node, String... fieldNames) {
        JsonNode child = firstNode(node, fieldNames);
        if (child == null || child.isNull()) {
            return null;
        }
        return toJson(child);
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

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Impossible de sérialiser une spawn condition JSON", e);
        }
    }
}