package be.loic.tfe_cobblemon.dataset.importer.item.service;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;

import java.nio.file.Path;

public interface SeasoningImportService {

    void importSeasonings(DatasetVersion datasetVersion, Path datasetRoot);
}