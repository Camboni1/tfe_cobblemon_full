package be.loic.tfe_cobblemon.spawn.dto;

import java.util.List;

public record SpawnConditionResponse(
        Boolean canSeeSky,
        Boolean isRaining,
        Boolean isThundering,
        Boolean isSlimeChunk,
        Short minY,
        Short maxY,
        Short minLight,
        Short maxLight,
        String timeRange,
        String moonPhase,
        List<SpawnConditionTokenResponse> tokens
) {}