package be.loic.tfe_cobblemon.pokemon.service.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePokemonFormCommand(
        @NotBlank String displayName,
        @NotNull Boolean isDefault,
        @NotNull Boolean battleOnly,
        @NotBlank String primaryType,
        String secondaryType,
        @NotNull @Min(1) @Max(255) Short baseHp,
        @NotNull @Min(1) @Max(255) Short baseAttack,
        @NotNull @Min(1) @Max(255) Short baseDefense,
        @NotNull @Min(1) @Max(255) Short baseSpecialAttack,
        @NotNull @Min(1) @Max(255) Short baseSpecialDefense,
        @NotNull @Min(1) @Max(255) Short baseSpeed
) {}