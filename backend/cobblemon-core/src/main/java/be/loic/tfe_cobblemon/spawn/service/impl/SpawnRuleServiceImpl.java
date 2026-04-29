package be.loic.tfe_cobblemon.spawn.service.impl;

import be.loic.tfe_cobblemon.common.exception.BusinessValidationException;
import be.loic.tfe_cobblemon.common.exception.ResourceNotFoundException;
import be.loic.tfe_cobblemon.common.translation.service.TranslationService;
import be.loic.tfe_cobblemon.dataset.service.DatasetVersionService;
import be.loic.tfe_cobblemon.pokemon.entity.Pokemon;
import be.loic.tfe_cobblemon.pokemon.entity.PokemonForm;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonFormRepository;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonRepository;
import be.loic.tfe_cobblemon.spawn.dto.SpawnConditionResponse;
import be.loic.tfe_cobblemon.spawn.dto.SpawnConditionTokenResponse;
import be.loic.tfe_cobblemon.spawn.dto.SpawnRuleResponse;
import be.loic.tfe_cobblemon.spawn.entity.*;
import be.loic.tfe_cobblemon.spawn.enums.SpawnBucket;
import be.loic.tfe_cobblemon.spawn.enums.SpawnType;
import be.loic.tfe_cobblemon.spawn.enums.SpawnablePositionType;
import be.loic.tfe_cobblemon.spawn.repository.SpawnPresetRepository;
import be.loic.tfe_cobblemon.spawn.repository.SpawnRuleRepository;
import be.loic.tfe_cobblemon.spawn.repository.SpawnSourceFileRepository;
import be.loic.tfe_cobblemon.spawn.service.SpawnRuleService;
import be.loic.tfe_cobblemon.spawn.service.command.CreateSpawnRuleCommand;
import be.loic.tfe_cobblemon.spawn.service.command.SpawnConditionCommand;
import be.loic.tfe_cobblemon.spawn.service.command.SpawnConditionTokenCommand;
import be.loic.tfe_cobblemon.spawn.service.command.UpdateSpawnRuleCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SpawnRuleServiceImpl implements SpawnRuleService {

    private final SpawnRuleRepository spawnRuleRepository;
    private final SpawnSourceFileRepository spawnSourceFileRepository;
    private final SpawnPresetRepository spawnPresetRepository;
    private final PokemonRepository pokemonRepository;
    private final PokemonFormRepository pokemonFormRepository;
    private final DatasetVersionService datasetVersionService;
    private final TranslationService translationService;

    @Override
    public SpawnRule getById(Long id) {
        return spawnRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SpawnRule introuvable : " + id));
    }

    @Override
    public List<SpawnRule> getByPokemonId(Long pokemonId) {
        return spawnRuleRepository.findAllByPokemonIdOrderByIdAsc(pokemonId);
    }

    @Override
    public List<SpawnRule> getByPokemonForm(Long pokemonId, Long pokemonFormId) {
        return spawnRuleRepository.findAllByPokemonIdAndPokemonFormIdOrderByIdAsc(pokemonId, pokemonFormId);
    }

    @Override
    @Transactional
    public SpawnRule create(CreateSpawnRuleCommand command) {
        validateCommonCommand(command);

        SpawnSourceFile spawnSourceFile = getSpawnSourceFile(command.spawnSourceFileId());
        Pokemon pokemon = getPokemon(command.pokemonId());
        PokemonForm pokemonForm = resolvePokemonForm(command.pokemonId(), command.pokemonFormId());
        Set<SpawnPreset> presets = resolvePresets(command.presetIds(), spawnSourceFile.getDatasetVersion().getId());

        validateDatasetVersionConsistency(spawnSourceFile, pokemon);
        validateExternalIdUniqueness(command.spawnSourceFileId(), command.externalId(), null);
        validateSpawnTypeSpecificFields(command.spawnType(), command.maxHerdSize(), command.herdablePokemonJson());
        validateCondition(command.condition());

        SpawnRule spawnRule = new SpawnRule();
        applyCommonValues(
                spawnRule,
                spawnSourceFile,
                pokemon,
                pokemonForm,
                command.externalId(),
                command.targetExpression(),
                command.spawnType(),
                command.spawnablePositionType(),
                command.bucket(),
                command.levelMin(),
                command.levelMax(),
                command.weight(),
                command.maxHerdSize(),
                command.minDistanceBetweenSpawns(),
                command.weightMultiplierJson(),
                command.weightMultipliersJson(),
                command.herdablePokemonJson(),
                command.rawJson(),
                presets,
                command.condition()
        );

        SpawnRule saved = spawnRuleRepository.save(spawnRule);
        return spawnRuleRepository.findById(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("SpawnRule introuvable après création : " + saved.getId()));
    }

    @Override
    @Transactional
    public SpawnRule update(Long id, UpdateSpawnRuleCommand command) {
        validateCommonCommand(command);

        SpawnRule existing = spawnRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SpawnRule introuvable : " + id));

        SpawnSourceFile spawnSourceFile = getSpawnSourceFile(command.spawnSourceFileId());
        Pokemon pokemon = getPokemon(command.pokemonId());
        PokemonForm pokemonForm = resolvePokemonForm(command.pokemonId(), command.pokemonFormId());
        Set<SpawnPreset> presets = resolvePresets(command.presetIds(), spawnSourceFile.getDatasetVersion().getId());

        validateDatasetVersionConsistency(spawnSourceFile, pokemon);
        validateExternalIdUniqueness(command.spawnSourceFileId(), command.externalId(), id);
        validateSpawnTypeSpecificFields(command.spawnType(), command.maxHerdSize(), command.herdablePokemonJson());
        validateCondition(command.condition());

        clearCondition(existing);

        applyCommonValues(
                existing,
                spawnSourceFile,
                pokemon,
                pokemonForm,
                command.externalId(),
                command.targetExpression(),
                command.spawnType(),
                command.spawnablePositionType(),
                command.bucket(),
                command.levelMin(),
                command.levelMax(),
                command.weight(),
                command.maxHerdSize(),
                command.minDistanceBetweenSpawns(),
                command.weightMultiplierJson(),
                command.weightMultipliersJson(),
                command.herdablePokemonJson(),
                command.rawJson(),
                presets,
                command.condition()
        );

        SpawnRule saved = spawnRuleRepository.save(existing);
        return spawnRuleRepository.findById(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("SpawnRule introuvable après mise à jour : " + saved.getId()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!spawnRuleRepository.existsById(id)) {
            throw new ResourceNotFoundException("SpawnRule introuvable : " + id);
        }
        spawnRuleRepository.deleteById(id);
    }

    private void applyCommonValues(
            SpawnRule spawnRule,
            SpawnSourceFile spawnSourceFile,
            Pokemon pokemon,
            PokemonForm pokemonForm,
            String externalId,
            String targetExpression,
            SpawnType spawnType,
            SpawnablePositionType spawnablePositionType,
            SpawnBucket bucket,
            Short levelMin,
            Short levelMax,
            BigDecimal weight,
            Short maxHerdSize,
            BigDecimal minDistanceBetweenSpawns,
            String weightMultiplierJson,
            String weightMultipliersJson,
            String herdablePokemonJson,
            String rawJson,
            Set<SpawnPreset> presets,
            SpawnConditionCommand conditionCommand
    ) {
        spawnRule.setSpawnSourceFile(spawnSourceFile);
        spawnRule.setExternalId(externalId);
        spawnRule.setPokemon(pokemon);
        spawnRule.setPokemonForm(pokemonForm);
        spawnRule.setTargetExpression(targetExpression);
        spawnRule.setSpawnType(spawnType);
        spawnRule.setSpawnablePositionType(spawnablePositionType);
        spawnRule.setBucket(bucket);
        spawnRule.setLevelMin(levelMin);
        spawnRule.setLevelMax(levelMax);
        spawnRule.setWeight(weight);
        spawnRule.setMaxHerdSize(maxHerdSize);
        spawnRule.setMinDistanceBetweenSpawns(minDistanceBetweenSpawns);
        spawnRule.setWeightMultiplierJson(weightMultiplierJson);
        spawnRule.setWeightMultipliersJson(weightMultipliersJson);
        spawnRule.setHerdablePokemonJson(herdablePokemonJson);
        spawnRule.setRawJson(rawJson);
        spawnRule.setPresets(new LinkedHashSet<>(presets));

        if (conditionCommand != null) {
            spawnRule.setSpawnCondition(buildCondition(conditionCommand));
        } else {
            spawnRule.setSpawnCondition(null);
        }
    }

    private SpawnCondition buildCondition(SpawnConditionCommand command) {
        SpawnCondition condition = new SpawnCondition();
        condition.setCanSeeSky(command.canSeeSky());
        condition.setIsRaining(command.isRaining());
        condition.setIsThundering(command.isThundering());
        condition.setIsSlimeChunk(command.isSlimeChunk());
        condition.setMinX(command.minX());
        condition.setMaxX(command.maxX());
        condition.setMinY(command.minY());
        condition.setMaxY(command.maxY());
        condition.setMinLight(command.minLight());
        condition.setMaxLight(command.maxLight());
        condition.setMinSkyLight(command.minSkyLight());
        condition.setMaxSkyLight(command.maxSkyLight());
        condition.setMinLureLevel(command.minLureLevel());
        condition.setMaxLureLevel(command.maxLureLevel());
        condition.setMoonPhase(command.moonPhase());
        condition.setTimeRange(command.timeRange());
        condition.setRodType(command.rodType());
        condition.setBaitItemExpression(command.baitItemExpression());
        condition.setConditionJson(command.conditionJson());
        condition.setAnticonditionJson(command.anticonditionJson());
        condition.setEffectiveConditionJson(command.effectiveConditionJson());
        condition.setEffectiveAnticonditionJson(command.effectiveAnticonditionJson());

        if (command.tokens() != null) {
            for (SpawnConditionTokenCommand tokenCommand : command.tokens()) {
                SpawnConditionToken token = new SpawnConditionToken();
                token.setSide(tokenCommand.side());
                token.setTokenType(tokenCommand.tokenType());
                token.setTokenValue(tokenCommand.tokenValue());
                token.setTag(Boolean.TRUE.equals(tokenCommand.isTag()));
                token.setSpawnCondition(condition);
                condition.getTokens().add(token);
            }
        }

        return condition;
    }

    private void clearCondition(SpawnRule spawnRule) {
        if (spawnRule.getSpawnCondition() != null) {
            spawnRule.getSpawnCondition().setSpawnRule(null);
        }
        spawnRule.setSpawnCondition(null);
    }

    private SpawnSourceFile getSpawnSourceFile(Long id) {
        return spawnSourceFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SpawnSourceFile introuvable : " + id));
    }

    private Pokemon getPokemon(Long id) {
        return pokemonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pokemon introuvable : " + id));
    }

    private PokemonForm resolvePokemonForm(Long pokemonId, Long pokemonFormId) {
        if (pokemonFormId == null) {
            return null;
        }

        return pokemonFormRepository.findByIdAndPokemonId(pokemonFormId, pokemonId)
                .orElseThrow(() -> new BusinessValidationException(
                        "La forme " + pokemonFormId + " n'appartient pas au Pokémon " + pokemonId
                ));
    }

    private Set<SpawnPreset> resolvePresets(Set<Long> presetIds, Long expectedDatasetVersionId) {
        Set<SpawnPreset> presets = new LinkedHashSet<>();

        if (presetIds == null || presetIds.isEmpty()) {
            return presets;
        }

        List<SpawnPreset> foundPresets = spawnPresetRepository.findAllById(presetIds);

        if (foundPresets.size() != presetIds.size()) {
            throw new ResourceNotFoundException("Un ou plusieurs SpawnPreset sont introuvables.");
        }

        for (SpawnPreset preset : foundPresets) {
            if (!Objects.equals(preset.getDatasetVersion().getId(), expectedDatasetVersionId)) {
                throw new BusinessValidationException(
                        "Le preset '" + preset.getCode() + "' n'appartient pas à la même dataset version que le fichier source."
                );
            }
            presets.add(preset);
        }

        return presets;
    }

    private void validateDatasetVersionConsistency(SpawnSourceFile spawnSourceFile, Pokemon pokemon) {
        if (!Objects.equals(spawnSourceFile.getDatasetVersion().getId(), pokemon.getDatasetVersion().getId())) {
            throw new BusinessValidationException(
                    "Le Pokémon et le fichier source n'appartiennent pas à la même dataset version."
            );
        }
    }

    private void validateExternalIdUniqueness(Long spawnSourceFileId, String externalId, Long currentRuleId) {
        spawnRuleRepository.findBySpawnSourceFileIdAndExternalId(spawnSourceFileId, externalId)
                .ifPresent(existing -> {
                    if (currentRuleId == null || !existing.getId().equals(currentRuleId)) {
                        throw new BusinessValidationException(
                                "Une SpawnRule existe déjà pour ce fichier source avec l'externalId '" + externalId + "'."
                        );
                    }
                });
    }

    private void validateCommonCommand(CreateSpawnRuleCommand command) {
        validateCommonFields(
                command.spawnSourceFileId(),
                command.externalId(),
                command.pokemonId(),
                command.targetExpression(),
                command.spawnType(),
                command.spawnablePositionType(),
                command.bucket(),
                command.levelMin(),
                command.levelMax(),
                command.weight(),
                command.rawJson()
        );
    }

    private void validateCommonCommand(UpdateSpawnRuleCommand command) {
        validateCommonFields(
                command.spawnSourceFileId(),
                command.externalId(),
                command.pokemonId(),
                command.targetExpression(),
                command.spawnType(),
                command.spawnablePositionType(),
                command.bucket(),
                command.levelMin(),
                command.levelMax(),
                command.weight(),
                command.rawJson()
        );
    }

    private void validateCommonFields(
            Long spawnSourceFileId,
            String externalId,
            Long pokemonId,
            String targetExpression,
            Object spawnType,
            Object spawnablePositionType,
            Object bucket,
            Short levelMin,
            Short levelMax,
            BigDecimal weight,
            String rawJson
    ) {
        if (spawnSourceFileId == null) {
            throw new BusinessValidationException("spawnSourceFileId est obligatoire.");
        }

        if (pokemonId == null) {
            throw new BusinessValidationException("pokemonId est obligatoire.");
        }

        if (isBlank(externalId)) {
            throw new BusinessValidationException("externalId est obligatoire.");
        }

        if (isBlank(targetExpression)) {
            throw new BusinessValidationException("targetExpression est obligatoire.");
        }

        if (spawnType == null) {
            throw new BusinessValidationException("spawnType est obligatoire.");
        }

        if (spawnablePositionType == null) {
            throw new BusinessValidationException("spawnablePositionType est obligatoire.");
        }

        if (bucket == null) {
            throw new BusinessValidationException("bucket est obligatoire.");
        }

        if (levelMin == null || levelMin < 1) {
            throw new BusinessValidationException("levelMin doit être supérieur ou égal à 1.");
        }

        if (levelMax == null || levelMax < levelMin) {
            throw new BusinessValidationException("levelMax doit être supérieur ou égal à levelMin.");
        }

        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("weight doit être strictement positif.");
        }

        if (isBlank(rawJson)) {
            throw new BusinessValidationException("rawJson est obligatoire.");
        }
    }

    private void validateSpawnTypeSpecificFields(SpawnType spawnType, Short maxHerdSize, String herdablePokemonJson) {
        if (spawnType == SpawnType.POKEMON_HERD && (maxHerdSize == null || maxHerdSize < 1)) {
            throw new BusinessValidationException("maxHerdSize est obligatoire et doit être > 0 pour un spawn de type pokemon-herd.");
        }

        if (spawnType == SpawnType.POKEMON && maxHerdSize != null && maxHerdSize < 1) {
            throw new BusinessValidationException("maxHerdSize doit être > 0 quand il est renseigné.");
        }

        if (spawnType == SpawnType.POKEMON_HERD && isBlank(herdablePokemonJson)) {
            throw new BusinessValidationException("herdablePokemonJson est attendu pour un spawn de type pokemon-herd.");
        }
    }

    private void validateCondition(SpawnConditionCommand condition) {
        if (condition == null) {
            return;
        }

        validateRangeOrder(condition.minX(), condition.maxX(), "X");
        validateRangeOrder(condition.minY(), condition.maxY(), "Y");
        validateRangeOrder(condition.minLight(), condition.maxLight(), "light");
        validateRangeOrder(condition.minSkyLight(), condition.maxSkyLight(), "skyLight");
        validateRangeOrder(condition.minLureLevel(), condition.maxLureLevel(), "lureLevel");

        validateLightValue(condition.minLight(), "minLight");
        validateLightValue(condition.maxLight(), "maxLight");
        validateLightValue(condition.minSkyLight(), "minSkyLight");
        validateLightValue(condition.maxSkyLight(), "maxSkyLight");

        validateYValue(condition.minY(), "minY");
        validateYValue(condition.maxY(), "maxY");

        if (condition.tokens() != null) {
            for (SpawnConditionTokenCommand token : condition.tokens()) {
                if (token.side() == null) {
                    throw new BusinessValidationException("Chaque token doit avoir un side.");
                }
                if (token.tokenType() == null) {
                    throw new BusinessValidationException("Chaque token doit avoir un tokenType.");
                }
                if (isBlank(token.tokenValue())) {
                    throw new BusinessValidationException("Chaque token doit avoir un tokenValue.");
                }
            }
        }
    }

    private void validateRangeOrder(Integer min, Integer max, String label) {
        if (min != null && max != null && min > max) {
            throw new BusinessValidationException("La borne min de " + label + " doit être <= à la borne max.");
        }
    }

    private void validateRangeOrder(Short min, Short max, String label) {
        if (min != null && max != null && min > max) {
            throw new BusinessValidationException("La borne min de " + label + " doit être <= à la borne max.");
        }
    }

    private void validateLightValue(Short value, String fieldName) {
        if (value != null && (value < 0 || value > 15)) {
            throw new BusinessValidationException(fieldName + " doit être entre 0 et 15.");
        }
    }

    private void validateYValue(Short value, String fieldName) {
        if (value != null && (value < -512 || value > 512)) {
            throw new BusinessValidationException(fieldName + " doit être entre -512 et 512.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public List<SpawnRuleResponse> findByPokemonSlug(String slug) {
        Long datasetVersionId = datasetVersionService.getActiveDatasetVersionId();
        Locale locale = LocaleContextHolder.getLocale();

        return spawnRuleRepository.findAllByPokemonSlugAndDatasetVersionId(slug, datasetVersionId)
                .stream()
                .map(sr -> toResponse(sr, locale))
                .toList();
    }

    @Override
    public SpawnRuleResponse getResponseById(Long id) {
        Locale locale = LocaleContextHolder.getLocale();
        SpawnRule sr = spawnRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SpawnRule introuvable : " + id));
        return toResponse(sr, locale);
    }

    private SpawnRuleResponse toResponse(SpawnRule sr, Locale locale) {
        return new SpawnRuleResponse(
                sr.getId(),
                sr.getExternalId(),
                sr.getPokemon().getSlug(),
                translationService.pokemonName(sr.getPokemon().getSlug(), locale), // ← pokemonDisplayName
                sr.getPokemonForm() != null ? sr.getPokemonForm().getCode() : null,
                sr.getTargetExpression(),
                translationService.spawnType(sr.getSpawnType().name(), locale),
                translationService.position(sr.getSpawnablePositionType().name(), locale),
                translationService.bucket(sr.getBucket().name(), locale),
                sr.getLevelMin(),
                sr.getLevelMax(),
                sr.getWeight(),
                sr.getMaxHerdSize(),
                sr.getSpawnSourceFile().getFilename(),
                toConditionResponse(sr.getSpawnCondition(), locale)
        );
    }

    private SpawnConditionResponse toConditionResponse(SpawnCondition condition, Locale locale) {
        if (condition == null) return null;

        List<SpawnConditionTokenResponse> tokens = condition.getTokens().stream()
                .map(t -> new SpawnConditionTokenResponse(
                        translationService.tokenType(t.getTokenType().name(), locale),
                        translationService.side(t.getSide().name(), locale),  // ← traduit
                        t.getTokenValue(),   // identifiant technique → on laisse tel quel
                        t.isTag()
                ))
                .toList();

        return new SpawnConditionResponse(
                condition.getCanSeeSky(),
                condition.getIsRaining(),
                condition.getIsThundering(),
                condition.getIsSlimeChunk(),
                condition.getMinY(),
                condition.getMaxY(),
                condition.getMinLight(),
                condition.getMaxLight(),
                condition.getTimeRange(),
                condition.getMoonPhase(),
                tokens
        );
    }
}
