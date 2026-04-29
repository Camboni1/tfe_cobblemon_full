package be.loic.tfe_cobblemon.item.service.impl;

import be.loic.tfe_cobblemon.common.translation.service.TranslationService;
import be.loic.tfe_cobblemon.dataset.service.DatasetVersionService;
import be.loic.tfe_cobblemon.item.dto.ItemResponse;
import be.loic.tfe_cobblemon.item.entity.Item;
import be.loic.tfe_cobblemon.item.repository.ItemRepository;
import be.loic.tfe_cobblemon.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final DatasetVersionService datasetVersionService;
    private final TranslationService translationService;

    @Override
    public Page<ItemResponse> search(String search, Pageable pageable) {
        Long datasetVersionId = datasetVersionService.getActiveDatasetVersionId();
        Locale locale = LocaleContextHolder.getLocale();

        List<Item> items;
        if (search == null || search.isBlank()) {
            items = itemRepository.findAllByDatasetVersionIdOrderByNamespacedIdAsc(datasetVersionId);
        } else {
            items = itemRepository
                    .findByDatasetVersionIdAndDisplayNameContainingIgnoreCaseOrderByNamespacedIdAsc(
                            datasetVersionId, search.trim());
        }

        List<ItemResponse> responses = items.stream()
                .map(item -> toResponse(item, locale))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        List<ItemResponse> page = start >= responses.size()
                ? List.of()
                : responses.subList(start, end);

        return new PageImpl<>(page, pageable, responses.size());
    }

    private ItemResponse toResponse(Item item, Locale locale) {
        String displayName = translationService.itemName(item.getNamespacedId(), locale);
        // fallback sur le displayName en base si pas de traduction
        if (displayName.equals("item." + item.getNamespacedId().replace(":", "."))) {
            displayName = item.getDisplayName();
        }
        return new ItemResponse(
                item.getId(),
                item.getNamespacedId(),
                item.getNamespace(),
                item.getPath(),
                displayName,
                item.isGeneratedPlaceholder()
        );
    }
}