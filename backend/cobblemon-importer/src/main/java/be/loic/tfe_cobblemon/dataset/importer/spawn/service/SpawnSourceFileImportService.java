package be.loic.tfe_cobblemon.dataset.importer.spawn.service;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;

import java.nio.file.Path;

public interface SpawnSourceFileImportService {

    void importSpawnSourceFiles(DatasetVersion datasetVersion, Path datasetRoot);
}