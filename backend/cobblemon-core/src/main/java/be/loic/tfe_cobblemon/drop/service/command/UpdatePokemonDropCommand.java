package be.loic.tfe_cobblemon.drop.service.command;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdatePokemonDropCommand(
        Short quantityMin,
        Short quantityMax,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal percentage,
        Short dropPoolAmountMin,
        Short dropPoolAmountMax
) {}