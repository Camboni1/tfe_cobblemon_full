package be.loic.tfe_cobblemon.spawn.service.command;

import be.loic.tfe_cobblemon.spawn.enums.SpawnBucket;
import be.loic.tfe_cobblemon.spawn.enums.SpawnType;
import be.loic.tfe_cobblemon.spawn.enums.SpawnablePositionType;

import java.math.BigDecimal;
import java.util.Set;

public record UpdateSpawnRuleCommand(
        Long spawnSourceFileId,
        String externalId,
        Long pokemonId,
        Long pokemonFormId,
        String targetExpression,
        SpawnType spawnType,
        SpawnablePositionType spawnablePositionType,
        SpawnBucket bucket,
        Short levelMin,
        Short levelMax,
        BigDecimal weight,
        Short maxHerdSize,
        BigDecimal minDistanceBetweenSpawns,
        String weightMultiplierJson,
        String weightMultipliersJson,
        String herdablePokemonJson,
        String rawJson,
        Set<Long> presetIds,
        SpawnConditionCommand condition
) {
}
