package be.loic.tfe_cobblemon.pokemon.dto;

import be.loic.tfe_cobblemon.drop.dto.PokemonDropResponse;
import java.util.List;

public record PokemonFormResponse(
        Long id,
        String code,
        String displayName,
        Boolean isDefault,
        Boolean battleOnly,
        String primaryType,
        String secondaryType,
        Short baseHp,
        Short baseAttack,
        Short baseDefense,
        Short baseSpecialAttack,
        Short baseSpecialDefense,
        Short baseSpeed,
        PokemonSpriteSet homeSprites,
        PokemonModelAssets model,
        List<PokemonDropResponse> drops
) {}