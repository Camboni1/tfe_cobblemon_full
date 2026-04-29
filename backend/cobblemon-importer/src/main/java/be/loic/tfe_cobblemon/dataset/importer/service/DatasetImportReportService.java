package be.loic.tfe_cobblemon.dataset.importer.service;

import java.time.Instant;

public interface DatasetImportReportService {
    void logFinalReport(Long datasetVersionId, String code, String label, String inputPath, boolean cleanBeforeImport, Instant startedAt);
}