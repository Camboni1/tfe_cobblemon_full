package be.loic.tfe_cobblemon.pokemon.service.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePokemonCommand(
        @NotBlank String displayName,
        @NotBlank String generationCode,
        @NotNull Boolean implemented
) {}