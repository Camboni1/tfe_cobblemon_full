package be.loic.tfe_cobblemon.dataset.importer.drop.service;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;

import java.nio.file.Path;

public interface PokemonDropImportService {

    void importPokemonDrops(DatasetVersion datasetVersion, Path datasetRoot);
}