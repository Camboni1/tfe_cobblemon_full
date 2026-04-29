package be.loic.tfe_cobblemon.dataset.importer.item.dto;

public record ItemImportReportMetrics(
        int total,
        int sourceFileCount,
        int syntheticCobblemonRegistryCount,
        int syntheticVanillaReferenceCount,
        int unresolvedPlaceholderCount,
        int invalidRawJsonCount
) {

    public int syntheticHydratedCount() {
        return syntheticCobblemonRegistryCount + syntheticVanillaReferenceCount;
    }

    public int resolvedCount() {
        return total - unresolvedPlaceholderCount;
    }

    public double unresolvedPlaceholderPercent() {
        if (total == 0) {
            return 0D;
        }
        return (unresolvedPlaceholderCount * 100D) / total;
    }
}