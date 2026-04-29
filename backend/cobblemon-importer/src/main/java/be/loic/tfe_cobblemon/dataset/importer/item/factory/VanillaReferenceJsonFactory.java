package be.loic.tfe_cobblemon.dataset.importer.item.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VanillaReferenceJsonFactory {

    private final ObjectMapper objectMapper;

    public String create(
            String namespacedId,
            String namespace,
            String path,
            String displayName
    ) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("kind", "vanilla_reference");
        root.put("path", path);
        root.put("source", "minecraft_registry_reference");
        root.put("namespace", namespace);
        root.put("displayName", displayName);
        root.put("namespacedId", namespacedId);
        root.put("officialJsonExistsInCobblemonAssets", false);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Impossible de sérialiser le JSON synthétique pour l'item vanilla " + namespacedId,
                    e
            );
        }
    }
}