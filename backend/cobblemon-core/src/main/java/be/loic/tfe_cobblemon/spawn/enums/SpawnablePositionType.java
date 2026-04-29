package be.loic.tfe_cobblemon.spawn.enums;

public enum SpawnablePositionType implements DatabaseValuedEnum {
    GROUNDED("grounded"),
    SURFACE("surface"),
    SUBMERGED("submerged"),
    SEAFLOOR("seafloor"),
    FISHING("fishing");

    private final String databaseValue;

    SpawnablePositionType(String databaseValue) {
        this.databaseValue = databaseValue;
    }
    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
