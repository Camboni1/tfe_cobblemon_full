package be.loic.tfe_cobblemon.dataset.importer.item.service;

import java.util.Set;

public interface ItemAliasResolver {

    String resolveCanonicalNamespacedId(String detectedNamespacedId);

    Set<String> resolveShadowNamespacedIds(String canonicalNamespacedId, String detectedNamespacedId);
}