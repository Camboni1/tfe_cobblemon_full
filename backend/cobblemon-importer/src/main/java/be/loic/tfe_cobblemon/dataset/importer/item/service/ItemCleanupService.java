package be.loic.tfe_cobblemon.dataset.importer.item.service;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.item.entity.ItemCandidate;

import java.util.Map;

public interface ItemCleanupService {

    void cleanupShadowItems(DatasetVersion datasetVersion, Map<String, ItemCandidate> candidates);
}