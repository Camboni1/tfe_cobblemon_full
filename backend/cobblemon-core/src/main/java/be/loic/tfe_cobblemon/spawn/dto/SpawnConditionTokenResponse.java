package be.loic.tfe_cobblemon.spawn.dto;

import be.loic.tfe_cobblemon.spawn.enums.SpawnConditionTokenSide;

public record SpawnConditionTokenResponse(
        String tokenType,  // ← String traduit
        String side,       // ← String au lieu de SpawnConditionTokenSide
        String tokenValue,
        boolean isTag
) {}