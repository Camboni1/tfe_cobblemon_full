package be.loic.tfe_cobblemon.spawn.enums;

public enum SpawnBucket implements DatabaseValuedEnum{
    COMMON("common"),
    UNCOMMON("uncommon"),
    RARE("rare"),
    ULTRA_RARE("ultra-rare");

    private final String databaseValue;

    SpawnBucket(String databaseValue) {
        this.databaseValue = databaseValue;
    }
    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
