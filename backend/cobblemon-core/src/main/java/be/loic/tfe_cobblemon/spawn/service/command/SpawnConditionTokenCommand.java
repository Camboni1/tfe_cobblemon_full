package be.loic.tfe_cobblemon.spawn.service.command;

import be.loic.tfe_cobblemon.spawn.enums.SpawnConditionTokenSide;
import be.loic.tfe_cobblemon.spawn.enums.SpawnConditionTokenType;

public record SpawnConditionTokenCommand (
    SpawnConditionTokenSide side,
    SpawnConditionTokenType tokenType,
    String tokenValue,
    Boolean isTag
    ) {

}

