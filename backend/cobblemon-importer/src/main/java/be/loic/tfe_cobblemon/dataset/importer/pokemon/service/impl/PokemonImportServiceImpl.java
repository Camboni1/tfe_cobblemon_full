package be.loic.tfe_cobblemon.dataset.importer.pokemon.service.impl;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.pokemon.service.PokemonImportService;
import be.loic.tfe_cobblemon.pokemon.entity.Pokemon;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonRepository;
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
public class PokemonImportServiceImpl implements PokemonImportService {

    private final PokemonRepository pokemonRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void importPokemon(DatasetVersion datasetVersion, Path datasetRoot) {
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
                    .forEach(path -> importSinglePokemonFile(datasetVersion, pokemonDirectory, path));
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors du parcours du dossier pokemon : " + pokemonDirectory, e);
        }
    }

    private void importSinglePokemonFile(DatasetVersion datasetVersion, Path pokemonDirectory, Path filePath) {
        try {
            String rawJson = Files.readString(filePath, StandardCharsets.UTF_8);
            JsonNode rootNode = objectMapper.readTree(rawJson);

            String slug = resolveSlug(rootNode, pokemonDirectory, filePath);
            String displayName = resolveDisplayName(rootNode, slug);
            Short nationalDexNumber = resolveNationalDexNumber(rootNode);
            String generationCode = resolveGenerationCode(rootNode, nationalDexNumber);
            Boolean implemented = resolveBoolean(rootNode, true, "implemented");
            String sourceFile = pokemonDirectory.relativize(filePath).toString().replace('\\', '/');

            Pokemon pokemon = pokemonRepository
                    .findByDatasetVersionIdAndSlug(datasetVersion.getId(), slug)
                    .orElseGet(Pokemon::new);

            pokemon.setDatasetVersion(datasetVersion);
            pokemon.setSlug(slug);
            pokemon.setDisplayName(displayName);
            pokemon.setNationalDexNumber(nationalDexNumber);
            pokemon.setGenerationCode(generationCode);
            pokemon.setImplemented(implemented);
            pokemon.setSourceFile(sourceFile);
            pokemon.setRawJson(rawJson);

            pokemonRepository.save(pokemon);
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la lecture du fichier Pokémon : " + filePath, e);
        }
    }

    private boolean isJsonFile(Path path) {
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return filename.endsWith(".json");
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

    private String resolveDisplayName(JsonNode rootNode, String slug) {
        String explicitDisplayName = firstNonBlank(
                text(rootNode, "displayName"),
                text(rootNode, "display_name"),
                text(rootNode, "speciesName"),
                text(rootNode, "species_name"),
                text(rootNode, "name")
        );

        if (explicitDisplayName != null && !explicitDisplayName.contains(":")) {
            return explicitDisplayName;
        }

        return humanize(slug);
    }

    private Short resolveNationalDexNumber(JsonNode rootNode) {
        Short value = resolveShort(rootNode,
                "nationalDexNumber",
                "national_dex_number",
                "nationalPokedexNumber",
                "national_pokedex_number",
                "dexNumber",
                "dex_number",
                "dex"
        );

        if (value == null) {
            throw new IllegalStateException("Impossible de déterminer le national dex number pour le Pokémon");
        }

        return value;
    }

    private String resolveGenerationCode(JsonNode rootNode, Short nationalDexNumber) {
        String raw = firstNonBlank(
                text(rootNode, "generationCode"),
                text(rootNode, "generation_code"),
                text(rootNode, "generation")
        );

        if (raw != null) {
            return normalizeGenerationCode(raw);
        }

        return deriveGenerationCodeFromDex(nationalDexNumber);
    }

    private Boolean resolveBoolean(JsonNode rootNode, boolean defaultValue, String fieldName) {
        JsonNode child = rootNode.get(fieldName);
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

    private Short resolveShort(JsonNode rootNode, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode child = rootNode.get(fieldName);
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

        return null;
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

    private String normalizeSlug(String value) {
        return value.trim()
                .replace('\\', '/')
                .toLowerCase(Locale.ROOT)
                .replace(' ', '-')
                .replace('_', '-');
    }

    private String normalizeGenerationCode(String raw) {
        String value = raw.trim().toUpperCase(Locale.ROOT)
                .replace("GENERATION", "")
                .replace("GEN", "")
                .replace("_", "")
                .replace("-", "")
                .trim();

        if (value.matches("\\d+")) {
            return "GEN_" + value;
        }

        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private String deriveGenerationCodeFromDex(short dex) {
        if (dex <= 151) return "GEN_1";
        if (dex <= 251) return "GEN_2";
        if (dex <= 386) return "GEN_3";
        if (dex <= 493) return "GEN_4";
        if (dex <= 649) return "GEN_5";
        if (dex <= 721) return "GEN_6";
        if (dex <= 809) return "GEN_7";
        if (dex <= 905) return "GEN_8";
        return "GEN_9";
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
}