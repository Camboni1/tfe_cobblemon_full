package be.loic.tfe_cobblemon.spawn.service.command;

import java.util.List;

public record SpawnConditionCommand(
        Boolean canSeeSky,
        Boolean isRaining,
        Boolean isThundering,
        Boolean isSlimeChunk,
        Integer minX,
        Integer maxX,
        Short minY,
        Short maxY,
        Short minLight,
        Short maxLight,
        Short minSkyLight,
        Short maxSkyLight,
        Short minLureLevel,
        Short maxLureLevel,
        String moonPhase,
        String timeRange,
        String rodType,
        String baitItemExpression,
        String conditionJson,
        String anticonditionJson,
        String effectiveConditionJson,
        String effectiveAnticonditionJson,
        List<SpawnConditionTokenCommand> tokens
) {
}
