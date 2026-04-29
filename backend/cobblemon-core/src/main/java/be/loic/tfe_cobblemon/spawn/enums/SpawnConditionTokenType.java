package be.loic.tfe_cobblemon.spawn.enums;

public enum SpawnConditionTokenType implements DatabaseValuedEnum {
    BIOME("biome"),
    STRUCTURE("structure"),
    DIMENSION("dimension"),
    NEARBY_BLOCK("nearby_block"),
    BASE_BLOCK("base_block"),
    LABEL("label");

    private final String databaseValue;

    SpawnConditionTokenType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
