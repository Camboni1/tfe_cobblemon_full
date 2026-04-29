package be.loic.tfe_cobblemon.dataset.importer.item.service.impl;

import be.loic.tfe_cobblemon.dataset.importer.item.service.ItemAliasResolver;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class ItemAliasResolverImpl implements ItemAliasResolver {

    private static final String COBBLEMON_BERRIES_PREFIX = "cobblemon:berries/";

    @Override
    public String resolveCanonicalNamespacedId(String detectedNamespacedId) {
        if (detectedNamespacedId.startsWith(COBBLEMON_BERRIES_PREFIX)) {
            return "cobblemon:" + detectedNamespacedId.substring(COBBLEMON_BERRIES_PREFIX.length());
        }

        return detectedNamespacedId;
    }

    @Override
    public Set<String> resolveShadowNamespacedIds(String canonicalNamespacedId, String detectedNamespacedId) {
        Set<String> shadows = new LinkedHashSet<>();

        if (!canonicalNamespacedId.equals(detectedNamespacedId)) {
            shadows.add(detectedNamespacedId);
        }

        return shadows;
    }
}