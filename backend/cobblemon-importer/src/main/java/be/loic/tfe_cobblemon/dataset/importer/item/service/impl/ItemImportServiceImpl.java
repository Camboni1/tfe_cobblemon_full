package be.loic.tfe_cobblemon.dataset.importer.item.service.impl;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.item.entity.ItemCandidate;
import be.loic.tfe_cobblemon.dataset.importer.item.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ItemImportServiceImpl implements ItemImportService {

    private final ItemFileCollector itemFileCollector;
    private final ItemCandidateFactory itemCandidateFactory;
    private final ItemHydrationService itemHydrationService;
    private final ItemCleanupService itemCleanupService;

    @Override
    @Transactional
    public void importItems(DatasetVersion datasetVersion, Path datasetRoot) {
        Path itemsDirectory = datasetRoot.resolve("items");

        Map<String, ItemCandidate> candidates = new LinkedHashMap<>();

        for (Path filePath : itemFileCollector.collectItemJsonFiles(datasetRoot)) {
            itemCandidateFactory.buildFromFile(itemsDirectory, filePath)
                    .ifPresent(candidate -> candidates.merge(
                            candidate.canonicalNamespacedId(),
                            candidate,
                            this::preferCandidate
                    ));
        }

        itemHydrationService.upsertCanonicalCandidates(datasetVersion, candidates);
        itemHydrationService.hydrateExistingPlaceholders(datasetVersion, datasetRoot, candidates);
        itemCleanupService.cleanupShadowItems(datasetVersion, candidates);
    }

    private ItemCandidate preferCandidate(ItemCandidate left, ItemCandidate right) {
        if (left.metadataOnly() && !right.metadataOnly()) {
            return right;
        }
        return left;
    }
}