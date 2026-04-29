package be.loic.tfe_cobblemon.pokemon.service.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePokemonCommand(
        @NotBlank String slug,
        @NotBlank String displayName,
        @NotNull @Positive Short nationalDexNumber,
        @NotBlank String generationCode,
        @NotNull Boolean implemented
) {}