package be.loic.tfe_cobblemon.spawn.converter;

import be.loic.tfe_cobblemon.spawn.enums.AbstractDatabaseEnumConverter;
import be.loic.tfe_cobblemon.spawn.enums.SpawnConditionTokenType;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SpawnConditionTokenTypeConverter extends AbstractDatabaseEnumConverter<SpawnConditionTokenType> {
    public SpawnConditionTokenTypeConverter() {
        super(SpawnConditionTokenType.class);
    }
}
