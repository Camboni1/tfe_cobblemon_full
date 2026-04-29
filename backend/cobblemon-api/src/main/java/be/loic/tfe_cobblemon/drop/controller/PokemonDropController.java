package be.loic.tfe_cobblemon.drop.controller;

import be.loic.tfe_cobblemon.drop.dto.PokemonDropResponse;
import be.loic.tfe_cobblemon.drop.service.PokemonDropService;
import be.loic.tfe_cobblemon.drop.service.command.CreatePokemonDropCommand;
import be.loic.tfe_cobblemon.drop.service.command.UpdatePokemonDropCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class PokemonDropController {

    private final PokemonDropService pokemonDropService;

    @PostMapping("/api/v1/pokemon/{slug}/forms/{code}/drops")
    @ResponseStatus(HttpStatus.CREATED)
    public PokemonDropResponse create(
            @PathVariable String slug,
            @PathVariable String code,
            @RequestBody @Validated CreatePokemonDropCommand command
    ) {
        return pokemonDropService.create(slug, code, command);
    }

    @PutMapping("/api/v1/drops/{id}")
    public PokemonDropResponse update(
            @PathVariable Long id,
            @RequestBody @Validated UpdatePokemonDropCommand command
    ) {
        return pokemonDropService.update(id, command);
    }

    @DeleteMapping("/api/v1/drops/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        pokemonDropService.delete(id);
    }
}