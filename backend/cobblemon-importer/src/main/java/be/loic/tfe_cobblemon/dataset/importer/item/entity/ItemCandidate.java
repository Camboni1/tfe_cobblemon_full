package be.loic.tfe_cobblemon.dataset.importer.item.entity;

import java.util.Set;

public record ItemCandidate(
        String canonicalNamespacedId,
        String namespace,
        String path,
        String displayName,
        String rawJson,
        boolean metadataOnly,
        ItemCandidateSource source,
        Set<String> shadowNamespacedIds
) {
}