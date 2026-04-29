package be.loic.tfe_cobblemon.dataset.importer.item.service.impl;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.dataset.importer.item.entity.ItemCandidate;
import be.loic.tfe_cobblemon.dataset.importer.item.service.ItemCleanupService;
import be.loic.tfe_cobblemon.drop.repository.PokemonDropRepository;
import be.loic.tfe_cobblemon.item.entity.Item;
import be.loic.tfe_cobblemon.item.repository.BaitEffectRepository;
import be.loic.tfe_cobblemon.item.repository.ItemRepository;
import be.loic.tfe_cobblemon.item.repository.SeasoningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ItemCleanupServiceImpl implements ItemCleanupService {

    private final ItemRepository itemRepository;
    private final PokemonDropRepository pokemonDropRepository;
    private final SeasoningRepository seasoningRepository;
    private final BaitEffectRepository baitEffectRepository;

    @Override
    @Transactional
    public void cleanupShadowItems(DatasetVersion datasetVersion, Map<String, ItemCandidate> candidates) {
        Set<String> shadowIds = new LinkedHashSet<>();

        for (ItemCandidate candidate : candidates.values()) {
            shadowIds.addAll(candidate.shadowNamespacedIds());
        }

        if (shadowIds.isEmpty()) {
            return;
        }

        for (String shadowId : shadowIds) {
            itemRepository.findByDatasetVersionIdAndNamespacedId(datasetVersion.getId(), shadowId)
                    .filter(this::canBeDeletedSafely)
                    .ifPresent(itemRepository::delete);
        }
    }

    private boolean canBeDeletedSafely(Item item) {
        Long itemId = item.getId();

        return !pokemonDropRepository.existsByItemId(itemId)
                && !seasoningRepository.existsByItemId(itemId)
                && !baitEffectRepository.existsByItemId(itemId);
    }
}
