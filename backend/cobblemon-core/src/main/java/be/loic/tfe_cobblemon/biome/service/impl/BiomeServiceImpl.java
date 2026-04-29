package be.loic.tfe_cobblemon.biome.service.impl;

import be.loic.tfe_cobblemon.biome.dto.BiomeListItemResponse;
import be.loic.tfe_cobblemon.biome.dto.BiomePokemonResponse;
import be.loic.tfe_cobblemon.biome.service.BiomeService;
import be.loic.tfe_cobblemon.common.asset.AssetUrlResolver;
import be.loic.tfe_cobblemon.dataset.service.DatasetVersionService;
import be.loic.tfe_cobblemon.pokemon.dto.PokemonListItemResponse;
import be.loic.tfe_cobblemon.pokemon.dto.PokemonSpriteSet;
import be.loic.tfe_cobblemon.pokemon.entity.Pokemon;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonRepository;
import be.loic.tfe_cobblemon.spawn.repository.SpawnConditionTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BiomeServiceImpl implements BiomeService {

    private final SpawnConditionTokenRepository tokenRepository;
    private final PokemonRepository pokemonRepository;
    private final DatasetVersionService datasetVersionService;
    private final AssetUrlResolver assetUrlResolver;

    @Override
    public Page<BiomeListItemResponse> listBiomes(String search, Pageable pageable) {
        String pattern = (search == null || search.isBlank())
                ? "%" : "%" + search.trim().toLowerCase() + "%";

        List<BiomeListItemResponse> all = tokenRepository.findDistinctBiomeValues(pattern)
                .stream()
                .map(value -> new BiomeListItemResponse(value, value.startsWith("#")))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<BiomeListItemResponse> page = start >= all.size() ? List.of() : all.subList(start, end);

        return new PageImpl<>(page, pageable, all.size());
    }

    @Override
    public BiomePokemonResponse getPokemonByBiome(String biomeValue) {
        Long datasetVersionId = datasetVersionService.getActiveDatasetVersionId();

        List<String> slugs = tokenRepository.findPokemonSlugsByBiomeValue(biomeValue, datasetVersionId);

        List<PokemonListItemResponse> pokemonList = slugs.stream()
                .map(slug -> pokemonRepository.findByDatasetVersionIdAndSlug(datasetVersionId, slug))
                .filter(opt -> opt.isPresent())
                .map(opt -> {
                    Pokemon p = opt.get();
                    return new PokemonListItemResponse(
                            p.getId(),
                            p.getSlug(),
                            p.getDisplayName(),
                            p.getNationalDexNumber(),
                            p.getGenerationCode(),
                            p.getImplemented(),
                            new PokemonSpriteSet(
                                    assetUrlResolver.resolveHomeSpriteUrl(p.getNationalDexNumber(), false, false),
                                    assetUrlResolver.resolveHomeSpriteUrl(p.getNationalDexNumber(), true,  false),
                                    assetUrlResolver.resolveHomeSpriteUrl(p.getNationalDexNumber(), false, true),
                                    assetUrlResolver.resolveHomeSpriteUrl(p.getNationalDexNumber(), true,  true)
                            )
                    );
                })
                .toList();

        return new BiomePokemonResponse(biomeValue, pokemonList);
    }
}