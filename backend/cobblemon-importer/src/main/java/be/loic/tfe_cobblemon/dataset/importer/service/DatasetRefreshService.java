package be.loic.tfe_cobblemon.dataset.importer.service;

public interface DatasetRefreshService  {
    void refresh(String code, String label, String inputPath, boolean cleanBeforeImport);
}
