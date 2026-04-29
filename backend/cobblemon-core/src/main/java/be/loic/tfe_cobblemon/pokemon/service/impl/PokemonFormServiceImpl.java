package be.loic.tfe_cobblemon.pokemon.service.impl;

import be.loic.tfe_cobblemon.common.asset.AssetUrlResolver;
import be.loic.tfe_cobblemon.common.exception.BusinessValidationException;
import be.loic.tfe_cobblemon.common.exception.ResourceNotFoundException;
import be.loic.tfe_cobblemon.dataset.service.DatasetVersionService;
import be.loic.tfe_cobblemon.drop.dto.PokemonDropResponse;
import be.loic.tfe_cobblemon.drop.repository.PokemonDropRepository;
import be.loic.tfe_cobblemon.pokemon.dto.PokemonFormResponse;
import be.loic.tfe_cobblemon.pokemon.dto.PokemonModelAssets;
import be.loic.tfe_cobblemon.pokemon.dto.PokemonSpriteSet;
import be.loic.tfe_cobblemon.pokemon.entity.Pokemon;
import be.loic.tfe_cobblemon.pokemon.entity.PokemonForm;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonFormRepository;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonRepository;
import be.loic.tfe_cobblemon.pokemon.service.PokemonFormService;
import be.loic.tfe_cobblemon.pokemon.service.command.CreatePokemonFormCommand;
import be.loic.tfe_cobblemon.pokemon.service.command.UpdatePokemonFormCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PokemonFormServiceImpl implements PokemonFormService {

    private final PokemonRepository pokemonRepository;
    private final PokemonFormRepository pokemonFormRepository;
    private final PokemonDropRepository pokemonDropRepository;
    private final DatasetVersionService datasetVersionService;
    private final AssetUrlResolver assetUrlResolver;

    @Override
    @Transactional
    public PokemonFormResponse create(String pokemonSlug, CreatePokemonFormCommand command) {
        Long datasetVersionId = datasetVersionService.getActiveDatasetVersionId();

        Pokemon pokemon = pokemonRepository.findByDatasetVersionIdAndSlug(datasetVersionId, pokemonSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Pokémon introuvable : " + pokemonSlug));

        if (pokemonFormRepository.existsByPokemonIdAndCode(pokemon.getId(), command.code())) {
            throw new BusinessValidationException(
                    "Une forme '" + command.code() + "' existe déjà pour ce Pokémon."
            );
        }

        PokemonForm form = new PokemonForm();
        form.setPokemon(pokemon);
        applyValues(form, command.code(), command.displayName(), command.isDefault(),
                command.battleOnly(), command.primaryType(), command.secondaryType(),
                command.baseHp(), command.baseAttack(), command.baseDefense(),
                command.baseSpecialAttack(), command.baseSpecialDefense(), command.baseSpeed());

        PokemonForm saved = pokemonFormRepository.save(form);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PokemonFormResponse update(String pokemonSlug, String formCode, UpdatePokemonFormCommand command) {
        Long datasetVersionId = datasetVersionService.getActiveDatasetVersionId();

        Pokemon pokemon = pokemonRepository.findByDatasetVersionIdAndSlug(datasetVersionId, pokemonSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Pokémon introuvable : " + pokemonSlug));

        PokemonForm form = pokemonFormRepository.findByPokemonIdAndCode(pokemon.getId(), formCode)
                .orElseThrow(() -> new ResourceNotFoundException("Forme introuvable : " + formCode));

        applyValues(form, form.getCode(), command.displayName(), command.isDefault(),
                command.battleOnly(), command.primaryType(), command.secondaryType(),
                command.baseHp(), command.baseAttack(), command.baseDefense(),
                command.baseSpecialAttack(), command.baseSpecialDefense(), command.baseSpeed());

        PokemonForm saved = pokemonFormRepository.save(form);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(String pokemonSlug, String formCode) {
        Long datasetVersionId = datasetVersionService.getActiveDatasetVersionId();

        Pokemon pokemon = pokemonRepository.findByDatasetVersionIdAndSlug(datasetVersionId, pokemonSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Pokémon introuvable : " + pokemonSlug));

        PokemonForm form = pokemonFormRepository.findByPokemonIdAndCode(pokemon.getId(), formCode)
                .orElseThrow(() -> new ResourceNotFoundException("Forme introuvable : " + formCode));

        if (Boolean.TRUE.equals(form.getIsDefault()) &&
                pokemonFormRepository.findAllByPokemonIdOrderByIsDefaultDescDisplayNameAsc(
                        pokemon.getId()).size() > 1) {
            throw new BusinessValidationException(
                    "Impossible de supprimer la forme par défaut. Assignez d'abord une autre forme par défaut."
            );
        }

        pokemonFormRepository.delete(form);
    }

    private void applyValues(PokemonForm form, String code, String displayName,
                             Boolean isDefault, Boolean battleOnly, String primaryType, String secondaryType,
                             Short baseHp, Short baseAttack, Short baseDefense,
                             Short baseSpecialAttack, Short baseSpecialDefense, Short baseSpeed) {
        form.setCode(code.trim().toLowerCase());
        form.setDisplayName(displayName.trim());
        form.setIsDefault(isDefault);
        form.setBattleOnly(battleOnly);
        form.setPrimaryType(primaryType.trim().toLowerCase());
        form.setSecondaryType(secondaryType != null ? secondaryType.trim().toLowerCase() : null);
        form.setBaseHp(baseHp);
        form.setBaseAttack(baseAttack);
        form.setBaseDefense(baseDefense);
        form.setBaseSpecialAttack(baseSpecialAttack);
        form.setBaseSpecialDefense(baseSpecialDefense);
        form.setBaseSpeed(baseSpeed);
        form.setRawJson("{}");
    }

    private PokemonFormResponse toResponse(PokemonForm form) {
        List<PokemonDropResponse> drops = pokemonDropRepository
                .findAllByPokemonFormIdOrderByIdAsc(form.getId())
                .stream()
                .map(drop -> new PokemonDropResponse(
                        drop.getId(),
                        drop.getItem().getNamespacedId(),
                        drop.getItem().getDisplayName(),
                        drop.getItem().isGeneratedPlaceholder(),
                        drop.getQuantityMin(),
                        drop.getQuantityMax(),
                        drop.getPercentage(),
                        drop.getDropPoolAmountMin(),
                        drop.getDropPoolAmountMax()
                ))
                .toList();

        Short dexNumber = form.getPokemon().getNationalDexNumber();
        String slug = form.getPokemon().getSlug();
        String formCode = Boolean.TRUE.equals(form.getIsDefault()) ? null : form.getCode();
        Integer homeFormId = form.getHomeFormId();

        PokemonSpriteSet homeSprites = new PokemonSpriteSet(
                assetUrlResolver.resolveHomeSpriteForForm(dexNumber, homeFormId, false, false),
                assetUrlResolver.resolveHomeSpriteForForm(dexNumber, homeFormId, true,  false),
                assetUrlResolver.resolveHomeSpriteForForm(dexNumber, homeFormId, false, true),
                assetUrlResolver.resolveHomeSpriteForForm(dexNumber, homeFormId, true,  true)
        );

        PokemonModelAssets model = new PokemonModelAssets(
                assetUrlResolver.resolveCobblemonModelUrl(dexNumber, slug, formCode),
                assetUrlResolver.resolveCobblemonTextureUrl(dexNumber, slug, formCode, false, false),
                assetUrlResolver.resolveCobblemonTextureUrl(dexNumber, slug, formCode, true,  false),
                assetUrlResolver.resolveCobblemonTextureUrl(dexNumber, slug, formCode, false, true),
                assetUrlResolver.resolveCobblemonTextureUrl(dexNumber, slug, formCode, true,  true)
        );

        return new PokemonFormResponse(
                form.getId(),
                form.getCode(),
                form.getDisplayName(),
                form.getIsDefault(),
                form.getBattleOnly(),
                form.getPrimaryType(),
                form.getSecondaryType(),
                form.getBaseHp(),
                form.getBaseAttack(),
                form.getBaseDefense(),
                form.getBaseSpecialAttack(),
                form.getBaseSpecialDefense(),
                form.getBaseSpeed(),
                homeSprites,
                model,
                drops
        );
    }
}