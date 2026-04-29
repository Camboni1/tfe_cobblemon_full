package be.loic.tfe_cobblemon.pokemon.dto;

import java.util.List;

public record PokemonDetailsResponse(
        Long id,
        String slug,
        String displayName,
        Short nationalDexNumber,
        String generationCode,
        Boolean implemented,
        PokemonSpriteSet homeSprites,
        PokemonModelAssets model,
        List<PokemonFormResponse> forms
) {}