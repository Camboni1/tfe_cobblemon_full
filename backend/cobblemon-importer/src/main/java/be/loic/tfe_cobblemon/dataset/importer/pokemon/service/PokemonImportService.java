package be.loic.tfe_cobblemon.dataset.importer.pokemon.service;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;

import java.nio.file.Path;

public interface PokemonImportService {

    void importPokemon(DatasetVersion datasetVersion, Path datasetRoot);
}