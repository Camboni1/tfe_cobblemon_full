package be.loic.tfe_cobblemon.spawn.service;

import be.loic.tfe_cobblemon.spawn.dto.SpawnRuleResponse;
import be.loic.tfe_cobblemon.spawn.entity.SpawnRule;
import be.loic.tfe_cobblemon.spawn.service.command.CreateSpawnRuleCommand;
import be.loic.tfe_cobblemon.spawn.service.command.UpdateSpawnRuleCommand;

import java.util.List;

public interface SpawnRuleService {
    SpawnRule getById(Long id);

    List<SpawnRule> getByPokemonId(Long pokemonId);

    List<SpawnRule> getByPokemonForm(Long pokemonId, Long pokemonFormId);

    SpawnRule create(CreateSpawnRuleCommand command);

    SpawnRule update(Long id, UpdateSpawnRuleCommand command);

    List<SpawnRuleResponse> findByPokemonSlug(String slug);

    void delete(Long id);
    SpawnRuleResponse getResponseById(Long id);
}