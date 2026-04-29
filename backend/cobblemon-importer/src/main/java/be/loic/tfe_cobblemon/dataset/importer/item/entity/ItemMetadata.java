package be.loic.tfe_cobblemon.dataset.importer.item.entity;

import java.util.List;

public record ItemMetadata(
        String displayName,
        List<String> tooltips
) {
}