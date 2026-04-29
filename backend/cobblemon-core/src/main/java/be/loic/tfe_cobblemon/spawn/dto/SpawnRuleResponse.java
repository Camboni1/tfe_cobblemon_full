package be.loic.tfe_cobblemon.spawn.dto;

import java.math.BigDecimal;

public record SpawnRuleResponse(
        Long id,
        String externalId,
        String pokemonSlug,
        String pokemonDisplayName,
        String formCode,
        String targetExpression,
        String spawnType,
        String spawnablePositionType,
        String bucket,
        Short levelMin,
        Short levelMax,
        BigDecimal weight,
        Short maxHerdSize,
        String sourceFilename,
        SpawnConditionResponse condition
) {}