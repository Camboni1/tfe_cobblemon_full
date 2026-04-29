package be.loic.tfe_cobblemon.dataset.importer.item.service.impl;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.item.entity.ItemCandidate;
import be.loic.tfe_cobblemon.dataset.importer.item.service.ItemHydrationService;
import be.loic.tfe_cobblemon.dataset.importer.item.service.ItemMetadataResolver;
import be.loic.tfe_cobblemon.item.entity.Item;
import be.loic.tfe_cobblemon.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import be.loic.tfe_cobblemon.dataset.importer.item.service.MissingItemHydrationService;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ItemHydrationServiceImpl implements ItemHydrationService {

    private final ItemRepository itemRepository;
    private final ItemMetadataResolver itemMetadataResolver;
    private final MissingItemHydrationService missingItemHydrationService;

    @Override
    @Transactional
    public void upsertCanonicalCandidates(DatasetVersion datasetVersion, Map<String, ItemCandidate> candidates) {
        for (ItemCandidate candidate : candidates.values()) {
            Item item = itemRepository
                    .findByDatasetVersionIdAndNamespacedId(datasetVersion.getId(), candidate.canonicalNamespacedId())
                    .orElseGet(Item::new);

            apply(item, datasetVersion, candidate);
            itemRepository.save(item);
        }
    }

    @Override
    @Transactional
    public void hydrateExistingPlaceholders(DatasetVersion datasetVersion, Path datasetRoot, Map<String, ItemCandidate> candidates) {
        var placeholders = itemRepository.findAllByDatasetVersionIdAndGeneratedPlaceholderTrueOrderByNamespacedIdAsc(datasetVersion.getId());

        for (Item placeholder : placeholders) {
            ItemCandidate candidate = candidates.get(placeholder.getNamespacedId());

            if (candidate == null) {
                candidate = itemMetadataResolver
                        .resolveMetadataCandidate(placeholder.getNamespacedId(), datasetRoot)
                        .orElse(null);
            }

            if (candidate == null) {
                continue;
            }

            apply(placeholder, datasetVersion, candidate);
            itemRepository.save(placeholder);
        }
    }

    private void apply(Item item, DatasetVersion datasetVersion, ItemCandidate candidate) {
        MissingItemHydrationService.HydrationResult hydrationResult =
                missingItemHydrationService.hydrateIfMissing(
                        candidate.canonicalNamespacedId(),
                        candidate.namespace(),
                        candidate.path(),
                        candidate.displayName(),
                        candidate.rawJson()
                );

        String finalRawJson = hydrationResult.rawJson();
        boolean unresolvedPlaceholder = !StringUtils.hasText(finalRawJson);

        item.setDatasetVersion(datasetVersion);
        item.setNamespacedId(candidate.canonicalNamespacedId());
        item.setNamespace(candidate.namespace());
        item.setPath(candidate.path());
        item.setDisplayName(candidate.displayName());
        item.setRawJson(finalRawJson);
        item.setGeneratedPlaceholder(unresolvedPlaceholder);
    }
}
