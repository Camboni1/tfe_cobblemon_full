package be.loic.tfe_cobblemon.spawn.converter;

import be.loic.tfe_cobblemon.spawn.enums.AbstractDatabaseEnumConverter;
import be.loic.tfe_cobblemon.spawn.enums.SpawnBucket;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SpawnBucketConverter extends AbstractDatabaseEnumConverter<SpawnBucket> {
    public SpawnBucketConverter() {
        super(SpawnBucket.class);
    }
}
