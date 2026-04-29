package be.loic.tfe_cobblemon.drop.service;

import be.loic.tfe_cobblemon.drop.dto.PokemonDropResponse;
import be.loic.tfe_cobblemon.drop.service.command.CreatePokemonDropCommand;
import be.loic.tfe_cobblemon.drop.service.command.UpdatePokemonDropCommand;

public interface PokemonDropService {
    PokemonDropResponse create(String pokemonSlug, String formCode, CreatePokemonDropCommand command);
    PokemonDropResponse update(Long dropId, UpdatePokemonDropCommand command);
    void delete(Long dropId);
}