package be.loic.tfe_cobblemon.dataset.service;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;

public interface DatasetVersionService {

    DatasetVersion getActiveDatasetVersion();

    Long getActiveDatasetVersionId();

    DatasetVersion getByCode(String code);

    DatasetVersion createOrUpdateForImport(String code, String label);

    void activate(Long datasetVersionId);
    void deactivate(Long datasetVersionId);
}
