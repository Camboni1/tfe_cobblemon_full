package be.loic.tfe_cobblemon.pokemon.dto;

/** Sprites 2D Pokémon Home avec leurs variantes. */
public record PokemonSpriteSet(
        String defaultUrl,
        String shinyUrl,
        String femaleUrl,
        String shinyFemaleUrl
) {}