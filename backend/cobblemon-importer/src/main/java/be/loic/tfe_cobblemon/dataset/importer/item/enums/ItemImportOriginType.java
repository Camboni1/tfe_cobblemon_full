package be.loic.tfe_cobblemon.dataset.importer.item.enums;

public enum ItemImportOriginType {
    SOURCE_FILE,
    SYNTHETIC_COBBLEMON_REGISTRY,
    SYNTHETIC_VANILLA_REFERENCE,
    UNRESOLVED_PLACEHOLDER,
    INVALID_RAW_JSON;

    public boolean isSynthetic() {
        return this == SYNTHETIC_COBBLEMON_REGISTRY || this == SYNTHETIC_VANILLA_REFERENCE;
    }

    public boolean isResolved() {
        return this != UNRESOLVED_PLACEHOLDER;
    }
}