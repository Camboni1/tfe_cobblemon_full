package be.loic.tfe_cobblemon.pokemon.dto;

public record PokemonListItemResponse(
        Long id,
        String slug,
        String displayName,
        Short nationalDexNumber,
        String generationCode,
        Boolean implemented,
        PokemonSpriteSet homeSprites
) {}