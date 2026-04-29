package be.loic.tfe_cobblemon.pokemon.dto;

public record PokemonSearchCriteria(
        String search,
        String generationCode,
        Boolean implemented
) {
}
