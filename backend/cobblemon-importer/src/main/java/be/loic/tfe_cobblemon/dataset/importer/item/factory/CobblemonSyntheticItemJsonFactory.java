package be.loic.tfe_cobblemon.dataset.importer.item.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class CobblemonSyntheticItemJsonFactory {

    private static final Set<String> WEARABLE_ITEMS = Set.of(
            "black_glasses"
    );

    private static final Set<String> CONSUMABLE_ITEMS = Set.of(
            "roasted_leek"
    );

    private static final Set<String> APRICORNS = Set.of(
            "red_apricorn",
            "blue_apricorn",
            "yellow_apricorn",
            "green_apricorn",
            "pink_apricorn",
            "white_apricorn",
            "black_apricorn"
    );

    private static final Set<String> EVOLUTION_ITEMS = Set.of(
            "black_augurite",
            "dubious_disc",
            "electirizer",
            "magmarizer",
            "protector",
            "dragon_scale",
            "metal_coat",
            "upgrade",
            "prism_scale",
            "razor_fang",
            "razor_claw",
            "reaper_cloth",
            "fire_stone",
            "water_stone",
            "thunder_stone",
            "leaf_stone",
            "moon_stone",
            "sun_stone",
            "dawn_stone",
            "dusk_stone",
            "shiny_stone",
            "ice_stone",
            "oval_stone",
            "sweet_apple",
            "tart_apple",
            "peat_block"
    );

    private static final Set<String> EXPLICIT_HELD_ITEMS = Set.of(
            "absorb_bulb",
            "auspicious_armor",
            "big_root",
            "bright_powder",
            "cell_battery",
            "electric_seed",
            "everstone",
            "expert_belt",
            "focus_band",
            "lagging_tail",
            "magnet",
            "malicious_armor",
            "mental_herb",
            "metronome",
            "misty_seed",
            "muscle_band",
            "mystic_water",
            "poison_barb",
            "psychic_seed",
            "rocky_helmet",
            "room_service",
            "shed_shell",
            "spell_tag",
            "twisted_spoon"
    );

    private final ObjectMapper objectMapper;

    public String create(
            String namespacedId,
            String path,
            String displayName
    ) {
        SyntheticDefinition definition = resolveDefinition(path);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("kind", definition.kind());
        root.put("path", path);
        root.put("source", "generated_from_cobblemon_kotlin_registry");
        root.put("displayName", displayName);
        root.put("namespacedId", namespacedId);
        root.put("registryGroup", definition.registryGroup());
        root.put("officialJsonExistsInCobblemonAssets", false);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Impossible de sérialiser le JSON synthétique pour l'item Cobblemon " + namespacedId,
                    e
            );
        }
    }

    private SyntheticDefinition resolveDefinition(String path) {
        if (WEARABLE_ITEMS.contains(path)) {
            return new SyntheticDefinition("wearable_item", "wearables");
        }

        if (CONSUMABLE_ITEMS.contains(path)) {
            return new SyntheticDefinition("consumable_item", "consumable_items");
        }

        if (APRICORNS.contains(path)) {
            return new SyntheticDefinition("apricorn_item", "apricorns");
        }

        if (EVOLUTION_ITEMS.contains(path)) {
            return new SyntheticDefinition("evolution_item", "evolution_items");
        }

        if (EXPLICIT_HELD_ITEMS.contains(path) || looksLikeHeldItem(path)) {
            return new SyntheticDefinition("held_item", "held_items");
        }

        return new SyntheticDefinition("misc_cobblemon_item", "misc");
    }

    private boolean looksLikeHeldItem(String path) {
        return path.endsWith("_seed")
                || path.endsWith("_band")
                || path.endsWith("_belt")
                || path.endsWith("_herb")
                || path.endsWith("_orb")
                || path.endsWith("_barb")
                || path.endsWith("_tag")
                || path.endsWith("_tail")
                || path.endsWith("_lens")
                || path.endsWith("_rock")
                || path.endsWith("_policy")
                || path.endsWith("_service")
                || path.endsWith("_water")
                || path.endsWith("_powder")
                || path.endsWith("_shell")
                || path.endsWith("_spoon")
                || path.endsWith("_root");
    }

    private record SyntheticDefinition(String kind, String registryGroup) {
    }
}