package be.loic.tfe_cobblemon.dataset.importer.item.service;

import be.loic.tfe_cobblemon.dataset.importer.item.entity.ItemCandidate;

import java.nio.file.Path;
import java.util.Optional;

public interface ItemMetadataResolver {

    Optional<ItemCandidate> resolveMetadataCandidate(String namespacedId, Path datasetRoot);
}