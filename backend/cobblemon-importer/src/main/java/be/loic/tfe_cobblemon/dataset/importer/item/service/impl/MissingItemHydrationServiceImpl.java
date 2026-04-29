package be.loic.tfe_cobblemon.dataset.importer.item.service.impl;

import be.loic.tfe_cobblemon.dataset.importer.item.factory.CobblemonSyntheticItemJsonFactory;
import be.loic.tfe_cobblemon.dataset.importer.item.factory.VanillaReferenceJsonFactory;
import be.loic.tfe_cobblemon.dataset.importer.item.service.MissingItemHydrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MissingItemHydrationServiceImpl implements MissingItemHydrationService {

    private final CobblemonSyntheticItemJsonFactory cobblemonSyntheticItemJsonFactory;
    private final VanillaReferenceJsonFactory vanillaReferenceJsonFactory;

    public HydrationResult hydrateIfMissing(
            String namespacedId,
            String namespace,
            String path,
            String displayName,
            @Nullable String rawJson
    ) {
        if (StringUtils.hasText(rawJson)) {
            return new be.loic.tfe_cobblemon.dataset.importer.item.service.MissingItemHydrationService.HydrationResult(rawJson, false);
        }

        if ("minecraft".equalsIgnoreCase(namespace)) {
            return new be.loic.tfe_cobblemon.dataset.importer.item.service.MissingItemHydrationService.HydrationResult(
                    vanillaReferenceJsonFactory.create(namespacedId, namespace, path, displayName),
                    true
            );
        }

        if ("cobblemon".equalsIgnoreCase(namespace)) {
            return new be.loic.tfe_cobblemon.dataset.importer.item.service.MissingItemHydrationService.HydrationResult(
                    cobblemonSyntheticItemJsonFactory.create(namespacedId, path, displayName),
                    true
            );
        }

        return new be.loic.tfe_cobblemon.dataset.importer.item.service.MissingItemHydrationService.HydrationResult(null, false);
    }

}