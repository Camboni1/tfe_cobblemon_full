package be.loic.tfe_cobblemon.dataset.importer.item.service;

import java.nio.file.Path;
import java.util.List;

public interface ItemFileCollector {

    List<Path> collectItemJsonFiles(Path datasetRoot);
}