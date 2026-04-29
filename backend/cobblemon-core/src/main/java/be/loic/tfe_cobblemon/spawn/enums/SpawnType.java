package be.loic.tfe_cobblemon.spawn.enums;

public enum SpawnType implements DatabaseValuedEnum {
    POKEMON("pokemon"),
    POKEMON_HERD("pokemon-herd");

    private final String databaseValue;

    SpawnType(String databaseValue) {
        this.databaseValue = databaseValue;
    }
    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
