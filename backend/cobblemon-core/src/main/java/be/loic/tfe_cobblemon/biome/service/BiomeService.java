package be.loic.tfe_cobblemon.biome.service;

import be.loic.tfe_cobblemon.biome.dto.BiomeListItemResponse;
import be.loic.tfe_cobblemon.biome.dto.BiomePokemonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BiomeService {
    Page<BiomeListItemResponse> listBiomes(String search, Pageable pageable);
    BiomePokemonResponse getPokemonByBiome(String biomeValue);
}