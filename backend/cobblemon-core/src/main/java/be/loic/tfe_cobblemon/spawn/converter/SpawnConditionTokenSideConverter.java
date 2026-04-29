package be.loic.tfe_cobblemon.spawn.converter;

import be.loic.tfe_cobblemon.spawn.enums.AbstractDatabaseEnumConverter;
import be.loic.tfe_cobblemon.spawn.enums.SpawnConditionTokenSide;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SpawnConditionTokenSideConverter extends AbstractDatabaseEnumConverter<SpawnConditionTokenSide> {
    public SpawnConditionTokenSideConverter() {
        super(SpawnConditionTokenSide.class);
    }
}
