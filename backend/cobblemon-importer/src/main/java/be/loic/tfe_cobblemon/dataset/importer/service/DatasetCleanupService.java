package be.loic.tfe_cobblemon.dataset.importer.service;

public interface DatasetCleanupService {

    void deleteDatasetContent(Long datasetVersionId);
}
