package be.loic.tfe_cobblemon.pokemon.controller;

import be.loic.tfe_cobblemon.pokemon.dto.PokemonDetailsResponse;
import be.loic.tfe_cobblemon.pokemon.dto.PokemonFormResponse;
import be.loic.tfe_cobblemon.pokemon.dto.PokemonListItemResponse;
import be.loic.tfe_cobblemon.pokemon.dto.PokemonSearchCriteria;
import be.loic.tfe_cobblemon.pokemon.service.PokemonFormService;
import be.loic.tfe_cobblemon.pokemon.service.PokemonService;
import be.loic.tfe_cobblemon.pokemon.service.command.CreatePokemonCommand;
import be.loic.tfe_cobblemon.pokemon.service.command.CreatePokemonFormCommand;
import be.loic.tfe_cobblemon.pokemon.service.command.UpdatePokemonCommand;
import be.loic.tfe_cobblemon.pokemon.service.command.UpdatePokemonFormCommand;
import be.loic.tfe_cobblemon.spawn.dto.SpawnRuleResponse;
import be.loic.tfe_cobblemon.spawn.service.SpawnRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pokemon")
@RequiredArgsConstructor
@Validated
@CrossOrigin( origins = "http://localhost:3000")
public class PokemonController {

    private final PokemonService pokemonService;
    private final SpawnRuleService spawnRuleService;
    private final PokemonFormService pokemonFormService;

    @GetMapping
    public Page<PokemonListItemResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String generationCode,
            @RequestParam(required = false) Boolean implemented,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return pokemonService.search(
                new PokemonSearchCriteria(search, generationCode, implemented),
                pageable
        );
    }

    @GetMapping("/{slug}")
    public PokemonDetailsResponse getBySlug(@PathVariable String slug) {
        return pokemonService.getBySlug(slug);
    }

    @GetMapping("/{slug}/spawns")
    public List<SpawnRuleResponse> getSpawns(
            @PathVariable String slug
    ) {
        return spawnRuleService.findByPokemonSlug(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PokemonDetailsResponse create(
            @RequestBody @Validated CreatePokemonCommand command
    ) {
        return pokemonService.create(command);
    }

    @PutMapping("/{slug}")
    public PokemonDetailsResponse update(
            @PathVariable String slug,
            @RequestBody @Validated UpdatePokemonCommand command
    ) {
        return pokemonService.update(slug, command);
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String slug) {
        pokemonService.delete(slug);
    }

    @PostMapping("/{slug}/forms")
    @ResponseStatus(HttpStatus.CREATED)
    public PokemonFormResponse createForm(
            @PathVariable String slug,
            @RequestBody @Validated CreatePokemonFormCommand command
    ) {
        return pokemonFormService.create(slug, command);
    }

    @PutMapping("/{slug}/forms/{code}")
    public PokemonFormResponse updateForm(
            @PathVariable String slug,
            @PathVariable String code,
            @RequestBody @Validated UpdatePokemonFormCommand command
    ) {
        return pokemonFormService.update(slug, code, command);
    }

    @DeleteMapping("/{slug}/forms/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteForm(
            @PathVariable String slug,
            @PathVariable String code
    ) {
        pokemonFormService.delete(slug, code);
    }
}