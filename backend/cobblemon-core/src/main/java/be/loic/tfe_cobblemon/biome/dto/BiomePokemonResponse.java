package be.loic.tfe_cobblemon.biome.dto;

import be.loic.tfe_cobblemon.pokemon.dto.PokemonListItemResponse;
import java.util.List;

public record BiomePokemonResponse(
        String biomeValue,
        List<PokemonListItemResponse> pokemon
) {}