package be.loic.tfe_cobblemon.spawn.enums;

public enum SpawnConditionTokenSide implements DatabaseValuedEnum {
    CONDITION("condition"),
    ANTICONDITION("anticondition");

    private final String databaseValue;

    SpawnConditionTokenSide(String databaseValue) {
        this.databaseValue = databaseValue;
    }
    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
