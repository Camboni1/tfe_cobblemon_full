package be.loic.tfe_cobblemon.pokemon.service.impl;

import be.loic.tfe_cobblemon.common.asset.AssetUrlResolver;
import be.loic.tfe_cobblemon.common.exception.BusinessValidationException;
import be.loic.tfe_cobblemon.common.exception.ResourceNotFoundException;
import be.loic.tfe_cobblemon.common.translation.repository.TranslationRepository;
import be.loic.tfe_cobblemon.common.translation.service.TranslationService;
import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.service.DatasetVersionService;
import be.loic.tfe_cobblemon.drop.dto.PokemonDropResponse;
import be.loic.tfe_cobblemon.drop.repository.PokemonDropRepository;
import be.loic.tfe_cobblemon.pokemon.dto.*;
import be.loic.tfe_cobblemon.pokemon.entity.Pokemon;
import be.loic.tfe_cobblemon.pokemon.entity.PokemonForm;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonRepository;
import be.loic.tfe_cobblemon.pokemon.service.PokemonService;
import be.loic.tfe_cobblemon.pokemon.service.command.CreatePokemonCommand;
import be.loic.tfe_cobblemon.pokemon.service.command.UpdatePokemonCommand;
import be.loic.tfe_cobblemon.pokemon.specification.PokemonSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;


import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PokemonServiceImpl implements PokemonService {

    private final PokemonRepository pokemonRepository;
    private final PokemonDropRepository pokemonDropRepository;
    private final DatasetVersionService datasetVersionService;
    private final TranslationService translationService;
    private final TranslationRepository translationRepository;
    private final AssetUrlResolver assetUrlResolver;

    @Override
    public Page<PokemonListItemResponse> search(PokemonSearchCriteria criteria, Pageable pageable) {
        Long datasetVersionId = datasetVersionService.getActiveDatasetVersionId();
        Locale locale = LocaleContextHolder.getLocale();

        Specification<Pokemon> specification = PokemonSpecification.hasDatasetVersion(datasetVersionId)
                .and(PokemonSpecification.containsSearch(criteria.search()))
                .and(PokemonSpecification.hasGeneration(criteria.generationCode()))
                .and(PokemonSpecification.isImplemented(criteria.implemented()));

        Pageable effectivePageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "nationalDexNumber")
        );

        return pokemonRepository.findAll(specification, effectivePageable)
                .map(p -> toListItemResponse(p, locale));
    }

    @Override
    public PokemonDetailsResponse getBySlug(String slug) {
        Long datasetVersionId = datasetVersionService.getActiveDatasetVersionId();
        Locale locale = LocaleContextHolder.getLocale(); // ← ajout

        Pokemon pokemon = pokemonRepository.findByDatasetVersionIdAndSlug(datasetVersionId, slug)
                .orElseThrow(() -> new ResourceNotFoundException("Pokémon introuvable : " + slug));

        return toDetailsResponse(pokemon, locale);
    }

    @Override
    @Transactional
    public PokemonDetailsResponse create(CreatePokemonCommand command) {
        Long datasetVersionId = datasetVersionService.getActiveDatasetVersionId();

        if (pokemonRepository.existsByDatasetVersionIdAndSlug(datasetVersionId, command.slug())) {
            throw new BusinessValidationException(
                    "Un Pokémon avec le slug '" + command.slug() + "' existe déjà."
            );
        }

        if (pokemonRepository.existsByDatasetVersionIdAndNationalDexNumber(
                datasetVersionId, command.nationalDexNumber())) {
            throw new BusinessValidationException(
                    "Un Pokémon avec le numéro de dex #" + command.nationalDexNumber() + " existe déjà."
            );
        }

        DatasetVersion datasetVersion = datasetVersionService.getActiveDatasetVersion();

        Pokemon pokemon = new Pokemon();
        pokemon.setDatasetVersion(datasetVersion);
        pokemon.setSlug(command.slug().trim().toLowerCase());
        // Après pokemonRepository.save(pokemon)
        String translationKey = "cobblemon.species." + command.slug().replace("-", "_") + ".name";
        translationRepository.upsert(translationKey, "fr", command.displayName().trim());
        translationRepository.upsert(translationKey, "en", command.displayName().trim());
        translationService.reload();
        pokemon.setNationalDexNumber(command.nationalDexNumber());
        pokemon.setGenerationCode(command.generationCode().trim().toUpperCase());
        pokemon.setImplemented(command.implemented());
        pokemon.setSourceFile("manual");
        pokemon.setRawJson("{}");

        Pokemon saved = pokemonRepository.save(pokemon);
        Locale locale = LocaleContextHolder.getLocale();
        return toDetailsResponse(saved, locale);
    }

    @Override
    @Transactional
    public PokemonDetailsResponse update(String slug, UpdatePokemonCommand command) {
        Long datasetVersionId = datasetVersionService.getActiveDatasetVersionId();

        Pokemon pokemon = pokemonRepository.findByDatasetVersionIdAndSlug(datasetVersionId, slug)
                .orElseThrow(() -> new ResourceNotFoundException("Pokémon introuvable : " + slug));

        pokemon.setDisplayName(command.displayName().trim());
        pokemon.setGenerationCode(command.generationCode().trim().toUpperCase());
        pokemon.setImplemented(command.implemented());

        pokemonRepository.save(pokemon);

        // Met à jour aussi la traduction pour que la réponse reflète le changement
        String translationKey = "cobblemon.species." + slug.replace("-", "_") + ".name";
        translationRepository.upsert(translationKey, "fr", command.displayName().trim());
        translationRepository.upsert(translationKey, "en", command.displayName().trim());
        translationService.reload(); // recharge le cache

        Locale locale = LocaleContextHolder.getLocale();
        return toDetailsResponse(pokemon, locale);
    }

    @Override
    @Transactional
    public void delete(String slug) {
        Long datasetVersionId = datasetVersionService.getActiveDatasetVersionId();

        Pokemon pokemon = pokemonRepository.findByDatasetVersionIdAndSlug(datasetVersionId, slug)
                .orElseThrow(() -> new ResourceNotFoundException("Pokémon introuvable : " + slug));

        pokemonRepository.delete(pokemon);
    }

    private PokemonListItemResponse toListItemResponse(Pokemon pokemon, Locale locale) {
        return new PokemonListItemResponse(
                pokemon.getId(),
                pokemon.getSlug(),
                translationService.pokemonName(pokemon.getSlug(), locale),
                pokemon.getNationalDexNumber(),
                pokemon.getGenerationCode(),
                pokemon.getImplemented(),
                buildHomeSprites(pokemon.getNationalDexNumber())
        );
    }

    private PokemonDetailsResponse toDetailsResponse(Pokemon pokemon, Locale locale) {
        return new PokemonDetailsResponse(
                pokemon.getId(),
                pokemon.getSlug(),
                translationService.pokemonName(pokemon.getSlug(), locale),
                pokemon.getNationalDexNumber(),
                pokemon.getGenerationCode(),
                pokemon.getImplemented(),
                buildHomeSprites(pokemon.getNationalDexNumber()),
                buildDefaultModel(pokemon.getNationalDexNumber(), pokemon.getSlug()),
                pokemon.getForms().stream()
                        .sorted(Comparator.comparing(PokemonForm::getIsDefault).reversed())
                        .map(f -> toFormResponse(f, locale))
                        .toList()
        );
    }

    private PokemonFormResponse toFormResponse(PokemonForm form, Locale locale) {
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
                buildHomeSpritesForForm(dexNumber, homeFormId),
                buildModelAssets(dexNumber, slug, formCode),
                drops
        );
    }

    // Helper à ajouter dans la classe (à côté de buildHomeSprites / buildModelAssets) :
    private PokemonSpriteSet buildHomeSpritesForForm(Short dex, Integer homeFormId) {
        return new PokemonSpriteSet(
                assetUrlResolver.resolveHomeSpriteForForm(dex, homeFormId, false, false),
                assetUrlResolver.resolveHomeSpriteForForm(dex, homeFormId, true,  false),
                assetUrlResolver.resolveHomeSpriteForForm(dex, homeFormId, false, true),
                assetUrlResolver.resolveHomeSpriteForForm(dex, homeFormId, true,  true)
        );
    }

// Helpers à ajouter dans la classe :

    private PokemonSpriteSet buildHomeSprites(Short dex) {
        return new PokemonSpriteSet(
                assetUrlResolver.resolveHomeSpriteUrl(dex, false, false),
                assetUrlResolver.resolveHomeSpriteUrl(dex, true,  false),
                assetUrlResolver.resolveHomeSpriteUrl(dex, false, true),
                assetUrlResolver.resolveHomeSpriteUrl(dex, true,  true)
        );
    }

    private PokemonModelAssets buildDefaultModel(Short dex, String slug) {
        return buildModelAssets(dex, slug, null);
    }

    private PokemonModelAssets buildModelAssets(Short dex, String slug, String formCode) {
        // form null = forme par défaut : pas de suffixe dans le nom de fichier
        String modelVariant = formCode; // ex. "alolan", "cap_male"... à ajuster si tes codes diffèrent
        return new PokemonModelAssets(
                assetUrlResolver.resolveCobblemonModelUrl(dex, slug, modelVariant),
                assetUrlResolver.resolveCobblemonTextureUrl(dex, slug, formCode, false, false),
                assetUrlResolver.resolveCobblemonTextureUrl(dex, slug, formCode, true,  false),
                assetUrlResolver.resolveCobblemonTextureUrl(dex, slug, formCode, false, true),
                assetUrlResolver.resolveCobblemonTextureUrl(dex, slug, formCode, true,  true),
                assetUrlResolver.resolveGltfUrl(dex, slug, false),
                assetUrlResolver.resolveGltfUrl(dex, slug, true)
        );
    }
}
