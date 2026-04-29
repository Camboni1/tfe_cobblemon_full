package be.loic.tfe_cobblemon.dataset.importer.item.service;


import org.springframework.lang.Nullable;

public interface MissingItemHydrationService {

    public HydrationResult hydrateIfMissing(
            String namespacedId,
            String namespace,
            String path,
            String displayName,
            @Nullable String rawJson
    );
    public record HydrationResult(
            @Nullable String rawJson,
            boolean hydrated
    ) {
    }
}