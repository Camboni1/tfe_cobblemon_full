package be.loic.tfe_cobblemon.dataset.importer.item.service;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.item.entity.ItemCandidate;

import java.nio.file.Path;
import java.util.Map;

public interface ItemHydrationService {

    void upsertCanonicalCandidates(DatasetVersion datasetVersion, Map<String, ItemCandidate> candidates);

    void hydrateExistingPlaceholders(DatasetVersion datasetVersion, Path datasetRoot, Map<String, ItemCandidate> candidates);
}