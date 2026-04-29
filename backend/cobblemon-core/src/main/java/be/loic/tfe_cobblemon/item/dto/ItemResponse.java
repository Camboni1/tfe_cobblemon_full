package be.loic.tfe_cobblemon.item.dto;

public record ItemResponse(
        Long id,
        String namespacedId,
        String namespace,
        String path,
        String displayName,
        boolean isPlaceholder
) {}