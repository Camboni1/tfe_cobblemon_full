package be.loic.tfe_cobblemon.dataset.importer.item.service;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;

import java.nio.file.Path;

public interface BaitEffectImportService {

    void importBaitEffects(DatasetVersion datasetVersion, Path datasetRoot);
}