package be.loic.tfe_cobblemon.drop.dto;

import java.math.BigDecimal;

public record PokemonDropResponse(
        Long id,
        String itemNamespacedId,
        String itemDisplayName,
        boolean itemIsPlaceholder,
        Short quantityMin,
        Short quantityMax,
        BigDecimal percentage,
        Short dropPoolAmountMin,
        Short dropPoolAmountMax
) {}