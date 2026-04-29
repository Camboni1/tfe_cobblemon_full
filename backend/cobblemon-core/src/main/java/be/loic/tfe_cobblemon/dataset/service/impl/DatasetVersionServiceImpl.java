package be.loic.tfe_cobblemon.dataset.service.impl;

import be.loic.tfe_cobblemon.common.exception.ResourceNotFoundException;
import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.repository.DatasetVersionRepository;
import be.loic.tfe_cobblemon.dataset.service.DatasetVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DatasetVersionServiceImpl implements DatasetVersionService {

    private final DatasetVersionRepository datasetVersionRepository;

    @Override
    public DatasetVersion getActiveDatasetVersion() {
        return datasetVersionRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active dataset version found"));
    }

    @Override
    @Transactional
    public void deactivate(Long datasetVersionId) {
        DatasetVersion target = datasetVersionRepository.findById(datasetVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset version not found for id: " + datasetVersionId));

        target.setActive(false);
        datasetVersionRepository.save(target);
    }

    @Override
    public Long getActiveDatasetVersionId() {
        return getActiveDatasetVersion().getId();
    }

    @Override
    public DatasetVersion getByCode(String code) {
        return datasetVersionRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset version not found for code: " + code));
    }

    @Override
    @Transactional
    public DatasetVersion createOrUpdateForImport(String code, String label) {
        OffsetDateTime now = OffsetDateTime.now();

        DatasetVersion datasetVersion = datasetVersionRepository.findByCode(code)
                .orElseGet(DatasetVersion::new);

        datasetVersion.setCode(code);
        datasetVersion.setLabel(label);
        datasetVersion.setImportedAt(now);

        if (datasetVersion.getId() == null) {
            datasetVersion.setActive(false);
        }

        return datasetVersionRepository.save(datasetVersion);
    }

    @Override
    @Transactional
    public void activate(Long datasetVersionId) {
        DatasetVersion target = datasetVersionRepository.findById(datasetVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset version not found for id: " + datasetVersionId));

        datasetVersionRepository.deactivateOtherActiveVersions(datasetVersionId);

        target.setActive(true);
        target.setImportedAt(OffsetDateTime.now());
        datasetVersionRepository.saveAndFlush(target);
    }
}
