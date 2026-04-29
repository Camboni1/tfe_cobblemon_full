package be.loic.tfe_cobblemon.dataset.importer.spawn.service.impl;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.spawn.service.SpawnRuleImportService;
import be.loic.tfe_cobblemon.pokemon.entity.Pokemon;
import be.loic.tfe_cobblemon.pokemon.entity.PokemonForm;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonFormRepository;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonRepository;
import be.loic.tfe_cobblemon.spawn.entity.SpawnRule;
import be.loic.tfe_cobblemon.spawn.entity.SpawnSourceFile;
import be.loic.tfe_cobblemon.spawn.enums.SpawnBucket;
import be.loic.tfe_cobblemon.spawn.enums.SpawnType;
import be.loic.tfe_cobblemon.spawn.enums.SpawnablePositionType;
import be.loic.tfe_cobblemon.spawn.repository.SpawnRuleRepository;
import be.loic.tfe_cobblemon.spawn.repository.SpawnSourceFileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SpawnRuleImportServiceImpl implements SpawnRuleImportService {

    private static final Logger log = LoggerFactory.getLogger(SpawnRuleImportServiceImpl.class);

    private final SpawnSourceFileRepository spawnSourceFileRepository;
    private final SpawnRuleRepository spawnRuleRepository;
    private final PokemonRepository pokemonRepository;
    private final PokemonFormRepository pokemonFormRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void importSpawnRules(DatasetVersion datasetVersion, Path datasetRoot) {
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
                    .forEach(path -> importRulesFromSingleSpawnFile(datasetVersion, spawnsDirectory, path));
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors du parcours du dossier spawns : " + spawnsDirectory, e);
        }
    }

    private void importRulesFromSingleSpawnFile(DatasetVersion datasetVersion, Path spawnsDirectory, Path filePath) {
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
                PokemonResolution pokemonResolution = resolvePokemonResolution(datasetVersion.getId(), ruleNode);
                Pokemon pokemon = pokemonResolution.pokemon();

                String rawFormSelector = resolveRawFormSelector(ruleNode, pokemonResolution.formCodeHint());
                PokemonForm pokemonForm = resolvePokemonForm(pokemon, rawFormSelector);

                SpawnRule spawnRule = spawnRuleRepository
                        .findBySpawnSourceFileIdAndExternalId(spawnSourceFile.getId(), externalId)
                        .orElseGet(SpawnRule::new);

                spawnRule.setSpawnSourceFile(spawnSourceFile);
                spawnRule.setExternalId(externalId);
                spawnRule.setPokemon(pokemon);
                spawnRule.setPokemonForm(pokemonForm);
                spawnRule.setFormSelector(pokemonForm == null ? rawFormSelector : null);
                spawnRule.setTargetExpression(resolveTargetExpression(ruleNode));
                spawnRule.setSpawnType(resolveSpawnType(ruleNode));
                spawnRule.setSpawnablePositionType(resolveSpawnablePositionType(ruleNode));
                spawnRule.setBucket(resolveSpawnBucket(ruleNode));
                spawnRule.setLevelMin(resolveLevelMin(ruleNode));
                spawnRule.setLevelMax(resolveLevelMax(ruleNode, spawnRule.getLevelMin()));
                spawnRule.setWeight(resolveWeight(ruleNode));
                spawnRule.setMaxHerdSize(resolveShort(ruleNode, "maxHerdSize", "max_herd_size"));
                spawnRule.setMinDistanceBetweenSpawns(resolveBigDecimal(ruleNode, "minDistanceBetweenSpawns", "min_distance_between_spawns"));
                spawnRule.setWeightMultiplierJson(resolveJson(ruleNode, "weightMultiplier", "weight_multiplier"));
                spawnRule.setWeightMultipliersJson(resolveJson(ruleNode, "weightMultipliers", "weight_multipliers"));
                spawnRule.setHerdablePokemonJson(resolveJson(ruleNode, "herdablePokemon", "herdable_pokemon"));
                spawnRule.setRawJson(toJson(ruleNode));

                spawnRuleRepository.save(spawnRule);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lors de l'import du fichier spawn : " + filePath, e);
        }
    }

    private String resolveRawFormSelector(JsonNode ruleNode, String hintedFormCode) {
        String rawFormCode = firstNonBlank(
                text(ruleNode, "form"),
                text(ruleNode, "formId"),
                text(ruleNode, "form_id")
        );

        if (rawFormCode == null) {
            JsonNode pokemonNode = ruleNode.get("pokemon");
            if (pokemonNode != null && pokemonNode.isObject()) {
                rawFormCode = firstNonBlank(
                        text(pokemonNode, "form"),
                        text(pokemonNode, "formId"),
                        text(pokemonNode, "form_id")
                );
            }
        }

        if (rawFormCode == null) {
            rawFormCode = hintedFormCode;
        }

        if (rawFormCode == null || rawFormCode.isBlank()) {
            return null;
        }

        return rawFormCode.trim();
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

        JsonNode spawnsNode = firstNode(rootNode,
                "spawns",
                "spawnPool",
                "spawn_pool",
                "entries",
                "rules"
        );

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

    private PokemonResolution resolvePokemonResolution(Long datasetVersionId, JsonNode ruleNode) {
        String speciesReference = extractPokemonReference(ruleNode);

        if (speciesReference == null || speciesReference.isBlank()) {
            throw new IllegalStateException("Impossible de déterminer le Pokémon d'une spawn rule");
        }

        String normalizedReference = normalizeSlug(speciesReference);

        Optional<Pokemon> exactPokemon = pokemonRepository.findByDatasetVersionIdAndSlug(datasetVersionId, normalizedReference);
        if (exactPokemon.isPresent()) {
            return new PokemonResolution(exactPokemon.get(), null);
        }

        PokemonResolution embeddedFormResolution = tryResolvePokemonWithEmbeddedForm(datasetVersionId, normalizedReference);
        if (embeddedFormResolution != null) {
            return embeddedFormResolution;
        }

        throw new IllegalStateException("Pokémon introuvable pour la spawn rule : " + normalizedReference);
    }

    private PokemonResolution tryResolvePokemonWithEmbeddedForm(Long datasetVersionId, String normalizedReference) {
        int separatorIndex = normalizedReference.lastIndexOf('-');

        while (separatorIndex > 0) {
            String pokemonSlugCandidate = normalizedReference.substring(0, separatorIndex);
            String formCodeCandidate = normalizedReference.substring(separatorIndex + 1);

            Optional<Pokemon> pokemonCandidate = pokemonRepository.findByDatasetVersionIdAndSlug(datasetVersionId, pokemonSlugCandidate);
            if (pokemonCandidate.isPresent()) {
                return new PokemonResolution(pokemonCandidate.get(), formCodeCandidate);
            }

            separatorIndex = normalizedReference.lastIndexOf('-', separatorIndex - 1);
        }

        return null;
    }

    private String extractPokemonReference(JsonNode ruleNode) {
        String direct = firstNonBlank(
                text(ruleNode, "pokemon"),
                text(ruleNode, "species"),
                text(ruleNode, "speciesId"),
                text(ruleNode, "species_id"),
                text(ruleNode, "slug"),
                text(ruleNode, "name")
        );

        if (direct != null) {
            return direct;
        }

        JsonNode pokemonNode = ruleNode.get("pokemon");
        if (pokemonNode != null && !pokemonNode.isNull()) {
            if (pokemonNode.isTextual()) {
                String value = pokemonNode.asText();
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }

            if (pokemonNode.isObject()) {
                String nested = firstNonBlank(
                        text(pokemonNode, "species"),
                        text(pokemonNode, "speciesId"),
                        text(pokemonNode, "species_id"),
                        text(pokemonNode, "slug"),
                        text(pokemonNode, "pokemon"),
                        text(pokemonNode, "name")
                );
                if (nested != null) {
                    return nested;
                }
            }
        }

        JsonNode herdableNode = firstNode(ruleNode, "herdablePokemon", "herdable_pokemon");
        if (herdableNode != null && herdableNode.isArray() && !herdableNode.isEmpty()) {
            String leaderPokemon = extractLeaderPokemonFromHerd(herdableNode);
            if (leaderPokemon != null) {
                return leaderPokemon;
            }

            for (JsonNode herdEntry : herdableNode) {
                String herdPokemon = firstNonBlank(
                        text(herdEntry, "pokemon"),
                        text(herdEntry, "species"),
                        text(herdEntry, "speciesId"),
                        text(herdEntry, "species_id"),
                        text(herdEntry, "slug"),
                        text(herdEntry, "name")
                );
                if (herdPokemon != null) {
                    return herdPokemon;
                }
            }
        }

        return null;
    }

    private String extractLeaderPokemonFromHerd(JsonNode herdableNode) {
        for (JsonNode herdEntry : herdableNode) {
            Boolean isLeader = resolveBooleanOrNull(herdEntry, "isLeader", "is_leader");
            if (Boolean.TRUE.equals(isLeader)) {
                String leaderPokemon = firstNonBlank(
                        text(herdEntry, "pokemon"),
                        text(herdEntry, "species"),
                        text(herdEntry, "speciesId"),
                        text(herdEntry, "species_id"),
                        text(herdEntry, "slug"),
                        text(herdEntry, "name")
                );
                if (leaderPokemon != null) {
                    return leaderPokemon;
                }
            }
        }

        return null;
    }

    private PokemonForm resolvePokemonForm(Pokemon pokemon, String rawFormCode) {
        if (rawFormCode == null || rawFormCode.isBlank()) {
            return null;
        }

        Set<String> candidateCodes = buildFormCodeCandidates(pokemon.getSlug(), rawFormCode);

        for (String candidateCode : candidateCodes) {
            Optional<PokemonForm> pokemonForm = pokemonFormRepository.findByPokemonIdAndCode(pokemon.getId(), candidateCode);
            if (pokemonForm.isPresent()) {
                return pokemonForm.get();
            }
        }

        PokemonForm matchedLoadedForm = findBestMatchingLoadedForm(pokemon, candidateCodes);
        if (matchedLoadedForm != null) {
            return matchedLoadedForm;
        }

        for (String candidateCode : candidateCodes) {
            if ("default".equals(candidateCode) || "base".equals(candidateCode) || "normal".equals(candidateCode)) {
                return pokemonFormRepository.findByPokemonIdAndIsDefaultTrue(pokemon.getId()).orElse(null);
            }
        }

        log.warn(
                "Form non résolue pour la spawn rule. pokemon={}, rawFormCode={}, candidates={}. La règle sera importée avec formSelector.",
                pokemon.getSlug(),
                rawFormCode,
                candidateCodes
        );

        return null;
    }

    private Set<String> buildFormCodeCandidates(String pokemonSlug, String rawFormCode) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();

        String normalized = normalizeCode(rawFormCode);
        addFormCandidateVariants(candidates, normalized);

        String normalizedSlug = normalizeCode(pokemonSlug);
        String prefix = normalizedSlug + "-";
        if (normalized.startsWith(prefix) && normalized.length() > prefix.length()) {
            addFormCandidateVariants(candidates, normalized.substring(prefix.length()));
        }

        String suffix = "-" + normalizedSlug;
        if (normalized.endsWith(suffix) && normalized.length() > suffix.length()) {
            addFormCandidateVariants(candidates, normalized.substring(0, normalized.length() - suffix.length()));
        }

        return candidates;
    }

    private void addFormCandidateVariants(Set<String> candidates, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        String normalized = normalizeCode(value);
        candidates.add(normalized);

        if ("valencian".equals(normalized)) {
            candidates.add("valencia");
        }

        if (normalized.endsWith("ian") && normalized.length() > 3) {
            candidates.add(normalized.substring(0, normalized.length() - 1));
        }
    }

    private PokemonForm findBestMatchingLoadedForm(Pokemon pokemon, Set<String> candidateCodes) {
        if (pokemon.getForms() == null || pokemon.getForms().isEmpty()) {
            return null;
        }

        for (PokemonForm form : pokemon.getForms()) {
            if (form == null) {
                continue;
            }

            if (matchesFormCandidate(form, candidateCodes)) {
                return form;
            }
        }

        return null;
    }

    private boolean matchesFormCandidate(PokemonForm form, Set<String> candidateCodes) {
        for (String candidate : candidateCodes) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }

            if (equalsNormalized(form.getCode(), candidate)) {
                return true;
            }

            if (equalsNormalized(form.getDisplayName(), candidate)) {
                return true;
            }

            if (containsNormalizedToken(form.getAspectsJson(), candidate)) {
                return true;
            }

            if (containsNormalizedToken(form.getLabelsJson(), candidate)) {
                return true;
            }

            if (containsNormalizedToken(form.getRawJson(), candidate)) {
                return true;
            }
        }

        return false;
    }

    private boolean equalsNormalized(String value, String candidate) {
        if (value == null || candidate == null) {
            return false;
        }

        return normalizeCode(value).equals(normalizeCode(candidate));
    }

    private boolean containsNormalizedToken(String value, String candidate) {
        if (value == null || value.isBlank() || candidate == null || candidate.isBlank()) {
            return false;
        }

        String normalizedValue = normalizeSearchText(value);
        String normalizedCandidate = normalizeSearchText(candidate);

        if (normalizedValue.contains("\"" + normalizedCandidate + "\"")) {
            return true;
        }

        return normalizedValue.contains(normalizedCandidate);
    }

    private String normalizeSearchText(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-')
                .trim();
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

    private String resolveTargetExpression(JsonNode ruleNode) {
        String target = firstNonBlank(
                text(ruleNode, "target"),
                text(ruleNode, "targetExpression"),
                text(ruleNode, "target_expression"),
                text(ruleNode, "context")
        );

        if (target != null) {
            return target;
        }

        return "land";
    }

    private SpawnType resolveSpawnType(JsonNode ruleNode) {
        String value = firstNonBlank(
                text(ruleNode, "spawnType"),
                text(ruleNode, "spawn_type"),
                text(ruleNode, "type")
        );

        String normalized = value == null ? "pokemon" : value.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "pokemon", "spawn" -> SpawnType.POKEMON;
            case "pokemon-herd", "pokemon_herd", "herd" -> SpawnType.POKEMON_HERD;
            default -> throw new IllegalStateException("SpawnType inconnu : " + value);
        };
    }

    private SpawnablePositionType resolveSpawnablePositionType(JsonNode ruleNode) {
        String value = firstNonBlank(
                text(ruleNode, "context"),
                text(ruleNode, "spawnablePositionType"),
                text(ruleNode, "spawnable_position_type"),
                text(ruleNode, "positionType"),
                text(ruleNode, "position_type")
        );

        String normalized = value == null ? "grounded" : value.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "grounded", "land" -> SpawnablePositionType.GROUNDED;
            case "surface" -> SpawnablePositionType.SURFACE;
            case "submerged", "underwater" -> SpawnablePositionType.SUBMERGED;
            case "seafloor" -> SpawnablePositionType.SEAFLOOR;
            case "fishing" -> SpawnablePositionType.FISHING;
            default -> throw new IllegalStateException("SpawnablePositionType inconnu : " + value);
        };
    }

    private SpawnBucket resolveSpawnBucket(JsonNode ruleNode) {
        String value = firstNonBlank(
                text(ruleNode, "bucket"),
                text(ruleNode, "rarity")
        );

        String normalized = value == null ? "common" : value.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "common" -> SpawnBucket.COMMON;
            case "uncommon" -> SpawnBucket.UNCOMMON;
            case "rare" -> SpawnBucket.RARE;
            case "ultra-rare", "ultra_rare", "ultrarare" -> SpawnBucket.ULTRA_RARE;
            default -> throw new IllegalStateException("SpawnBucket inconnu : " + value);
        };
    }

    private Short resolveLevelMin(JsonNode ruleNode) {
        Short direct = resolveShort(ruleNode, "levelMin", "level_min", "minLevel", "min_level");
        if (direct != null) {
            return direct;
        }

        LevelRange range = resolveLevelRange(ruleNode);
        if (range != null) {
            return range.min();
        }

        return 1;
    }

    private Short resolveLevelMax(JsonNode ruleNode, Short fallbackMin) {
        Short direct = resolveShort(ruleNode, "levelMax", "level_max", "maxLevel", "max_level");
        if (direct != null) {
            return direct;
        }

        LevelRange range = resolveLevelRange(ruleNode);
        if (range != null) {
            return range.max();
        }

        return fallbackMin;
    }

    private LevelRange resolveLevelRange(JsonNode ruleNode) {
        JsonNode levelNode = firstNode(ruleNode, "level", "levelRange", "level_range");
        if (levelNode == null || levelNode.isNull()) {
            return null;
        }

        if (levelNode.isNumber()) {
            short value = levelNode.shortValue();
            return new LevelRange(value, value);
        }

        if (!levelNode.isTextual()) {
            return null;
        }

        String raw = levelNode.asText();
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim();

        if (value.matches("\\d+")) {
            short parsed = Short.parseShort(value);
            return new LevelRange(parsed, parsed);
        }

        String normalized = value.replace("..", "-")
                .replace("—", "-")
                .replace("–", "-")
                .replace(",", "-");

        String[] parts = normalized.split("\\s*-\\s*");

        if (parts.length == 2 && parts[0].matches("-?\\d+") && parts[1].matches("-?\\d+")) {
            short min = Short.parseShort(parts[0]);
            short max = Short.parseShort(parts[1]);
            return new LevelRange(min, max);
        }

        throw new IllegalStateException("Format de niveau invalide pour une spawn rule : " + value);
    }

    private BigDecimal resolveWeight(JsonNode ruleNode) {
        BigDecimal value = resolveBigDecimal(ruleNode, "weight");
        if (value == null) {
            throw new IllegalStateException("Weight obligatoire introuvable pour une spawn rule");
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
            if (!value.isBlank() && value.matches("-?\\d+")) {
                return Short.valueOf(value);
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

    private String resolveJson(JsonNode node, String... fieldNames) {
        JsonNode child = firstNode(node, fieldNames);
        if (child == null || child.isNull()) {
            return null;
        }
        return toJson(child);
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

    private Boolean resolveBooleanOrNull(JsonNode node, String... fieldNames) {
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
        String normalized = value.trim()
                .replace('\\', '/')
                .toLowerCase(Locale.ROOT);

        int colonIndex = normalized.lastIndexOf(':');
        if (colonIndex >= 0 && colonIndex < normalized.length() - 1) {
            normalized = normalized.substring(colonIndex + 1);
        }

        int slashIndex = normalized.lastIndexOf('/');
        if (slashIndex >= 0 && slashIndex < normalized.length() - 1) {
            normalized = normalized.substring(slashIndex + 1);
        }

        normalized = normalized
                .replace(' ', '-')
                .replace('_', '-');

        return removeJsonExtension(normalized);
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

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Impossible de sérialiser une spawn rule JSON", e);
        }
    }

    private record LevelRange(short min, short max) {
    }

    private record PokemonResolution(Pokemon pokemon, String formCodeHint) {
    }
}