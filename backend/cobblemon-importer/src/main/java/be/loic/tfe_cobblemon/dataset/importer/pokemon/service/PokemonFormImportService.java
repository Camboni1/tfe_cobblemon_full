package be.loic.tfe_cobblemon.dataset.importer.pokemon.service;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;

import java.nio.file.Path;

public interface PokemonFormImportService {

    void importPokemonForms(DatasetVersion datasetVersion, Path datasetRoot);
}