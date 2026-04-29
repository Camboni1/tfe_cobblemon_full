package be.loic.tfe_cobblemon.pokemon.service;

import be.loic.tfe_cobblemon.pokemon.dto.PokemonDetailsResponse;
import be.loic.tfe_cobblemon.pokemon.dto.PokemonListItemResponse;
import be.loic.tfe_cobblemon.pokemon.dto.PokemonSearchCriteria;
import be.loic.tfe_cobblemon.pokemon.service.command.CreatePokemonCommand;
import be.loic.tfe_cobblemon.pokemon.service.command.UpdatePokemonCommand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PokemonService {

    Page<PokemonListItemResponse> search(PokemonSearchCriteria criteria, Pageable pageable);

    PokemonDetailsResponse getBySlug(String slug);
    PokemonDetailsResponse create(CreatePokemonCommand command);
    PokemonDetailsResponse update(String slug, UpdatePokemonCommand command);
    void delete(String slug);
}