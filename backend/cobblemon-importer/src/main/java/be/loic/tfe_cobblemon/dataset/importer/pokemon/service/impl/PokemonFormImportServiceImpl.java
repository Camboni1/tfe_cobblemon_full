package be.loic.tfe_cobblemon.dataset.importer.pokemon.service.impl;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.pokemon.service.PokemonFormImportService;
import be.loic.tfe_cobblemon.pokemon.entity.Pokemon;
import be.loic.tfe_cobblemon.pokemon.entity.PokemonForm;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonFormRepository;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
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
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PokemonFormImportServiceImpl implements PokemonFormImportService {

    private final PokemonRepository pokemonRepository;
    private final PokemonFormRepository pokemonFormRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void importPokemonForms(DatasetVersion datasetVersion, Path datasetRoot) {
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
                    .forEach(path -> importFormsFromSinglePokemonFile(datasetVersion, pokemonDirectory, path));
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors du parcours du dossier pokemon : " + pokemonDirectory, e);
        }
    }

    private void importFormsFromSinglePokemonFile(DatasetVersion datasetVersion, Path pokemonDirectory, Path filePath) {
        try {
            String rawJson = Files.readString(filePath, StandardCharsets.UTF_8);
            JsonNode rootNode = objectMapper.readTree(rawJson);

            String slug = resolveSlug(rootNode, pokemonDirectory, filePath);

            Pokemon pokemon = pokemonRepository.findByDatasetVersionIdAndSlug(datasetVersion.getId(), slug)
                    .orElseThrow(() -> new IllegalStateException("Pokémon introuvable pour le slug : " + slug));

            List<JsonNode> formNodes = resolveFormNodes(rootNode);
            if (formNodes.isEmpty()) {
                throw new IllegalStateException("Aucune form trouvée pour le Pokémon : " + slug);
            }

            List<PokemonForm> importedForms = new ArrayList<>();
            boolean defaultAssigned = false;

            for (int index = 0; index < formNodes.size(); index++) {
                JsonNode formNode = formNodes.get(index);

                String code = resolveFormCode(formNode, rootNode, index);
                boolean proposedDefault = resolveFormIsDefault(formNode, rootNode, index);

                PokemonForm form = pokemonFormRepository.findByPokemonIdAndCode(pokemon.getId(), code)
                        .orElseGet(PokemonForm::new);

                form.setPokemon(pokemon);
                form.setCode(code);
                form.setDisplayName(resolveFormDisplayName(formNode, rootNode, pokemon.getDisplayName(), code));
                form.setIsDefault(proposedDefault && !defaultAssigned);
                form.setBattleOnly(resolveBoolean(formNode, rootNode, false, "battleOnly", "battle_only"));
                form.setPrimaryType(resolvePrimaryType(formNode, rootNode));
                form.setSecondaryType(resolveSecondaryType(formNode, rootNode));
                form.setMaleRatio(resolveBigDecimal(formNode, rootNode, "maleRatio", "male_ratio"));
                form.setHeightDm(resolveInteger(formNode, rootNode, "heightDm", "height_dm"));
                form.setWeightHg(resolveInteger(formNode, rootNode, "weightHg", "weight_hg"));
                form.setCatchRate(resolveShort(formNode, rootNode, "catchRate", "catch_rate"));
                form.setBaseExperienceYield(resolveShort(formNode, rootNode, "baseExperienceYield", "base_experience_yield"));
                form.setExperienceGroup(resolveText(formNode, rootNode, "experienceGroup", "experience_group"));
                form.setEggCycles(resolveShort(formNode, rootNode, "eggCycles", "egg_cycles"));
                form.setBaseFriendship(resolveShort(formNode, rootNode, "baseFriendship", "base_friendship"));
                form.setBaseScale(resolveBigDecimal(formNode, rootNode, "baseScale", "base_scale"));

                form.setBaseHp(resolveRequiredStat(
                        formNode, rootNode,
                        new String[]{"hp"},
                        "baseHp", "base_hp"
                ));
                form.setBaseAttack(resolveRequiredStat(
                        formNode, rootNode,
                        new String[]{"attack"},
                        "baseAttack", "base_attack"
                ));
                form.setBaseDefense(resolveRequiredStat(
                        formNode, rootNode,
                        new String[]{"defense", "defence"},
                        "baseDefense", "base_defense", "baseDefence", "base_defence"
                ));
                form.setBaseSpecialAttack(resolveRequiredStat(
                        formNode, rootNode,
                        new String[]{"specialAttack", "special_attack"},
                        "baseSpecialAttack", "base_special_attack"
                ));
                form.setBaseSpecialDefense(resolveRequiredStat(
                        formNode, rootNode,
                        new String[]{"specialDefense", "special_defense", "specialDefence", "special_defence"},
                        "baseSpecialDefense", "base_special_defense", "baseSpecialDefence", "base_special_defence"
                ));
                form.setBaseSpeed(resolveRequiredStat(
                        formNode, rootNode,
                        new String[]{"speed"},
                        "baseSpeed", "base_speed"
                ));

                form.setEvHp(resolveEvStat(
                        formNode, rootNode,
                        new String[]{"hp"},
                        "evHp", "ev_hp"
                ));
                form.setEvAttack(resolveEvStat(
                        formNode, rootNode,
                        new String[]{"attack"},
                        "evAttack", "ev_attack"
                ));
                form.setEvDefense(resolveEvStat(
                        formNode, rootNode,
                        new String[]{"defense", "defence"},
                        "evDefense", "ev_defense", "evDefence", "ev_defence"
                ));
                form.setEvSpecialAttack(resolveEvStat(
                        formNode, rootNode,
                        new String[]{"specialAttack", "special_attack"},
                        "evSpecialAttack", "ev_special_attack"
                ));
                form.setEvSpecialDefense(resolveEvStat(
                        formNode, rootNode,
                        new String[]{"specialDefense", "special_defense", "specialDefence", "special_defence"},
                        "evSpecialDefense", "ev_special_defense", "evSpecialDefence", "ev_special_defence"
                ));
                form.setEvSpeed(resolveEvStat(
                        formNode, rootNode,
                        new String[]{"speed"},
                        "evSpeed", "ev_speed"
                ));

                form.setAspectsJson(resolveJson(formNode, rootNode, "aspects"));
                form.setLabelsJson(resolveJson(formNode, rootNode, "labels"));
                form.setRawJson(toJson(formNode));

                PokemonForm savedForm = pokemonFormRepository.save(form);
                importedForms.add(savedForm);

                if (Boolean.TRUE.equals(savedForm.getIsDefault())) {
                    defaultAssigned = true;
                }
            }

            if (!defaultAssigned && !importedForms.isEmpty()) {
                PokemonForm firstForm = importedForms.get(0);
                firstForm.setIsDefault(true);
                pokemonFormRepository.save(firstForm);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lors de l'import du fichier Pokémon : " + filePath, e);
        }
    }

    private boolean isJsonFile(Path path) {
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return filename.endsWith(".json");
    }

    private List<JsonNode> resolveFormNodes(JsonNode rootNode) {
        JsonNode formsNode = rootNode.get("forms");
        if (formsNode != null && formsNode.isArray() && !formsNode.isEmpty()) {
            List<JsonNode> formNodes = new ArrayList<>();

            // Le JSON déclare-t-il déjà une forme par défaut explicite ?
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

            // Sinon, on synthétise la forme "default" à partir du rootNode.
            // Les stats/types seront récupérés via le fallback rootNode dans les méthodes resolveXxx.
            if (!hasExplicitDefault) {
                ObjectNode syntheticDefault = objectMapper.createObjectNode();
                syntheticDefault.put("code", "default");
                syntheticDefault.put("isDefault", true);
                formNodes.add(syntheticDefault);
            }

            formsNode.forEach(formNodes::add);
            return formNodes;
        }

        // Cas Pokémon sans forms[] → le rootNode EST la forme normale
        List<JsonNode> single = new ArrayList<>();
        single.add(rootNode);
        return single;
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

    private String resolveFormDisplayName(JsonNode formNode, JsonNode rootNode, String pokemonDisplayName, String formCode) {
        String candidate = firstNonBlank(
                text(formNode, "displayName"),
                text(formNode, "display_name"),
                text(formNode, "name")
        );

        if (candidate != null && !candidate.isBlank()) {
            return candidate;
        }

        if ("default".equals(formCode)) {
            return pokemonDisplayName;
        }

        return pokemonDisplayName + " " + humanize(formCode);
    }

    private boolean resolveFormIsDefault(JsonNode formNode, JsonNode rootNode, int index) {
        Boolean explicit = resolveBooleanOrNull(formNode, "isDefault", "is_default");
        if (explicit != null) {
            return explicit;
        }

        String code = resolveFormCode(formNode, rootNode, index);
        if ("default".equals(code) || "base".equals(code) || "normal".equals(code)) {
            return true;
        }

        return index == 0;
    }

    private String resolvePrimaryType(JsonNode formNode, JsonNode rootNode) {
        String direct = firstNonBlank(
                resolveText(formNode, rootNode, "primaryType", "primary_type")
        );

        if (direct != null) {
            return normalizeType(direct);
        }

        JsonNode typesNode = firstNode(formNode, rootNode, "types");
        if (typesNode != null && typesNode.isArray() && !typesNode.isEmpty()) {
            JsonNode first = typesNode.get(0);
            if (first != null && first.isTextual()) {
                return normalizeType(first.asText());
            }
        }

        throw new IllegalStateException("Impossible de déterminer le type principal d'une form");
    }

    private String resolveSecondaryType(JsonNode formNode, JsonNode rootNode) {
        String direct = resolveText(formNode, rootNode, "secondaryType", "secondary_type");
        if (direct != null) {
            return normalizeType(direct);
        }

        JsonNode typesNode = firstNode(formNode, rootNode, "types");
        if (typesNode != null && typesNode.isArray() && typesNode.size() > 1) {
            JsonNode second = typesNode.get(1);
            if (second != null && second.isTextual()) {
                return normalizeType(second.asText());
            }
        }

        return null;
    }

    private Short resolveRequiredStat(JsonNode formNode, JsonNode rootNode, String[] nestedKeys, String... directFieldNames) {
        Short direct = resolveShort(formNode, rootNode, directFieldNames);
        if (direct != null) {
            return direct;
        }

        Short nested = resolveNestedShort(formNode, rootNode, new String[]{"baseStats", "base_stats"}, nestedKeys);
        if (nested != null) {
            return nested;
        }

        throw new IllegalStateException("Stat obligatoire introuvable : " + String.join(" / ", nestedKeys));
    }

    private Short resolveEvStat(JsonNode formNode, JsonNode rootNode, String[] nestedKeys, String... directFieldNames) {
        Short direct = resolveShort(formNode, rootNode, directFieldNames);
        if (direct != null) {
            return direct;
        }

        Short nested = resolveNestedShort(formNode, rootNode, new String[]{"evYield", "ev_yield"}, nestedKeys);
        if (nested != null) {
            return nested;
        }

        return 0;
    }

    private Short resolveNestedShort(JsonNode formNode, JsonNode rootNode, String[] parentFields, String... childFields) {
        for (String parentField : parentFields) {
            JsonNode parent = firstNode(formNode, rootNode, parentField);
            if (parent == null || parent.isNull()) {
                continue;
            }

            for (String childField : childFields) {
                JsonNode child = parent.get(childField);
                if (child == null || child.isNull()) {
                    continue;
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
            }
        }

        return null;
    }

    private Short resolveShort(JsonNode formNode, JsonNode rootNode, String... fieldNames) {
        JsonNode node = firstNode(formNode, rootNode, fieldNames);
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isNumber()) {
            return node.shortValue();
        }

        if (node.isTextual()) {
            String value = node.asText().trim();
            if (!value.isBlank()) {
                return Short.valueOf(value);
            }
        }

        return null;
    }

    private Integer resolveInteger(JsonNode formNode, JsonNode rootNode, String... fieldNames) {
        JsonNode node = firstNode(formNode, rootNode, fieldNames);
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isNumber()) {
            return node.intValue();
        }

        if (node.isTextual()) {
            String value = node.asText().trim();
            if (!value.isBlank()) {
                return Integer.valueOf(value);
            }
        }

        return null;
    }

    private BigDecimal resolveBigDecimal(JsonNode formNode, JsonNode rootNode, String... fieldNames) {
        JsonNode node = firstNode(formNode, rootNode, fieldNames);
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isNumber()) {
            return node.decimalValue();
        }

        if (node.isTextual()) {
            String value = node.asText().trim();
            if (!value.isBlank()) {
                return new BigDecimal(value);
            }
        }

        return null;
    }

    private boolean resolveBoolean(JsonNode formNode, JsonNode rootNode, boolean defaultValue, String... fieldNames) {
        Boolean explicit = resolveBooleanOrNull(formNode, rootNode, fieldNames);
        return explicit != null ? explicit : defaultValue;
    }

    private Boolean resolveBooleanOrNull(JsonNode formNode, String... fieldNames) {
        JsonNode node = firstNode(formNode, null, fieldNames);
        return asBoolean(node);
    }

    private Boolean resolveBooleanOrNull(JsonNode formNode, JsonNode rootNode, String... fieldNames) {
        JsonNode node = firstNode(formNode, rootNode, fieldNames);
        return asBoolean(node);
    }

    private Boolean asBoolean(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isBoolean()) {
            return node.asBoolean();
        }

        if (node.isTextual()) {
            String value = node.asText().trim().toLowerCase(Locale.ROOT);
            if ("true".equals(value)) {
                return true;
            }
            if ("false".equals(value)) {
                return false;
            }
        }

        return null;
    }

    private String resolveText(JsonNode formNode, JsonNode rootNode, String... fieldNames) {
        JsonNode node = firstNode(formNode, rootNode, fieldNames);
        if (node == null || node.isNull() || !node.isTextual()) {
            return null;
        }

        String value = node.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String resolveJson(JsonNode formNode, JsonNode rootNode, String... fieldNames) {
        JsonNode node = firstNode(formNode, rootNode, fieldNames);
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }

        return toJson(node);
    }

    private JsonNode firstNode(JsonNode primaryNode, JsonNode fallbackNode, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (primaryNode != null) {
                JsonNode primaryChild = primaryNode.get(fieldName);
                if (primaryChild != null && !primaryChild.isNull()) {
                    return primaryChild;
                }
            }

            if (fallbackNode != null) {
                JsonNode fallbackChild = fallbackNode.get(fieldName);
                if (fallbackChild != null && !fallbackChild.isNull()) {
                    return fallbackChild;
                }
            }
        }

        return null;
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null) {
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

    private String normalizeType(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
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
            throw new IllegalStateException("Impossible de sérialiser un nœud JSON", e);
        }
    }
}