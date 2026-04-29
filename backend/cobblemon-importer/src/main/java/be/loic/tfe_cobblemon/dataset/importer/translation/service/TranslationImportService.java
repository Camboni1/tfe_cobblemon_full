package be.loic.tfe_cobblemon.dataset.importer.translation.service;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import java.nio.file.Path;

public interface TranslationImportService {
    void importTranslations(DatasetVersion datasetVersion, Path datasetRoot);
}