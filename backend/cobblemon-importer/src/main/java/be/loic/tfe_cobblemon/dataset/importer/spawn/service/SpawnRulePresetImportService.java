package be.loic.tfe_cobblemon.dataset.importer.spawn.service;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;

import java.nio.file.Path;

public interface SpawnRulePresetImportService {

    void importSpawnRulePresets(DatasetVersion datasetVersion, Path datasetRoot);
}