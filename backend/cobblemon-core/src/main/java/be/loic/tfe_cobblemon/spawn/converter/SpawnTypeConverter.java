package be.loic.tfe_cobblemon.spawn.converter;

import be.loic.tfe_cobblemon.spawn.enums.AbstractDatabaseEnumConverter;
import be.loic.tfe_cobblemon.spawn.enums.SpawnType;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SpawnTypeConverter extends AbstractDatabaseEnumConverter<SpawnType> {
    public SpawnTypeConverter() {
        super(SpawnType.class);
    }
}
