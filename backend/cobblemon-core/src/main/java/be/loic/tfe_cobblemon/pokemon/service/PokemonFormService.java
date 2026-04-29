package be.loic.tfe_cobblemon.pokemon.service;

import be.loic.tfe_cobblemon.pokemon.dto.PokemonFormResponse;
import be.loic.tfe_cobblemon.pokemon.service.command.CreatePokemonFormCommand;
import be.loic.tfe_cobblemon.pokemon.service.command.UpdatePokemonFormCommand;

public interface PokemonFormService {
    PokemonFormResponse create(String pokemonSlug, CreatePokemonFormCommand command);
    PokemonFormResponse update(String pokemonSlug, String formCode, UpdatePokemonFormCommand command);
    void delete(String pokemonSlug, String formCode);
}