package be.loic.tfe_cobblemon.spawn.converter;

import be.loic.tfe_cobblemon.spawn.enums.AbstractDatabaseEnumConverter;
import be.loic.tfe_cobblemon.spawn.enums.SpawnablePositionType;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SpawnablePositionTypeConverter extends AbstractDatabaseEnumConverter<SpawnablePositionType> {
    public SpawnablePositionTypeConverter() {
        super(SpawnablePositionType.class);
    }
}
