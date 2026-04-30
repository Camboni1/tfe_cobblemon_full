package be.loic.tfe_cobblemon.common.asset;

import be.loic.tfe_cobblemon.common.config.AppProperties;
import be.loic.tfe_cobblemon.common.io.ConfiguredPathResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Résolution des URLs d'assets Pokémon.
 *
 * Structure sur disque (sous app.assets.path) :
 *   pokemon/home/{dex}.png                      -> sprite Home (male/défaut)
 *   pokemon/home/shiny/{dex}.png                -> sprite Home shiny
 *   pokemon/home/female/{dex}.png               -> sprite Home femelle
 *   pokemon/home/shiny/female/{dex}.png         -> sprite Home shiny + femelle
 *   pokemon/cobblemon-models/{NNNN}_{slug}/{slug}[_variant].geo.json
 *   pokemon/cobblemon-textures/{NNNN}_{slug}/{slug}[_form][_female][_shiny].png
 *
 * Note : le dex utilisé pour Home n'est PAS zéro-padded (ex. 25.png, pas 0025.png).
 * Pour cobblemon-models et cobblemon-textures, le dossier parent est padded (ex. 0025_pikachu/).
 */
@Service
@RequiredArgsConstructor
public class AssetUrlResolver {

    private final AppProperties appProperties;

    // ==================================================================
    // Pokémon Home (sprites 2D)
    // ==================================================================

    /** Sprite Home forme par défaut, male, normal. */
    public String resolveHomeSpriteUrl(Short nationalDexNumber) {
        return resolveHomeSpriteUrl(nationalDexNumber, false, false);
    }

    /** Sprite Home en choisissant shiny / femelle. */
    public String resolveHomeSpriteUrl(Short nationalDexNumber, boolean shiny, boolean female) {
        if (nationalDexNumber == null) return null;
        return resolveHomeSpriteByFormId(nationalDexNumber.intValue(), shiny, female);
    }

    /**
     * Sprite Home par ID de forme (utile pour formes alternatives comme 10001.png).
     */
    public String resolveHomeSpriteByFormId(int formId, boolean shiny, boolean female) {
        String sub = (shiny ? "shiny/" : "") + (female ? "female/" : "");
        return resolveUrl("pokemon/home/" + sub + formId + ".png");
    }

    /**
     * Sprite Home d'une forme : utilise homeFormId s'il est renseigné,
     * sinon retombe sur le dex national (sprite de la forme par défaut).
     */
    public String resolveHomeSpriteForForm(
            Short nationalDexNumber,
            Integer homeFormId,
            boolean shiny,
            boolean female
    ) {
        int id = (homeFormId != null) ? homeFormId : (nationalDexNumber != null ? nationalDexNumber.intValue() : -1);
        if (id < 0) return null;
        return resolveHomeSpriteByFormId(id, shiny, female);
    }

    // ==================================================================
    // Cobblemon : modèles 3D (.geo.json)
    // ==================================================================

    /** Modèle par défaut : pokemon/cobblemon-models/{NNNN}_{slug}/{slug}.geo.json */
    public String resolveCobblemonModelUrl(Short nationalDexNumber, String slug) {
        return resolveCobblemonModelUrl(nationalDexNumber, slug, null);
    }

    /**
     * Modèle avec variante : {slug}_{variant}.geo.json
     * Ex : ("venusaur", "female"), ("pikachu", "cap_male"), ("rattata", "alolan").
     */
    public String resolveCobblemonModelUrl(Short nationalDexNumber, String slug, String variant) {
        if (nationalDexNumber == null || slug == null || slug.isBlank()) return null;
        String baseDir = "pokemon/cobblemon-models/" + formatDexNumber(nationalDexNumber) + "_" + slug + "/";
        String fileName = slug + (variant == null || variant.isBlank() ? "" : "_" + variant) + ".geo.json";
        return resolveUrl(baseDir + fileName);
    }

    // ==================================================================
    // Cobblemon : textures (.png)
    // ==================================================================

    /** Texture par défaut : {slug}.png */
    public String resolveCobblemonTextureUrl(Short nationalDexNumber, String slug) {
        return resolveCobblemonTextureUrl(nationalDexNumber, slug, null, false, false);
    }

    /**
     * Texture avec forme et variantes. Convention de nommage :
     *   {slug}[_form][_female][_shiny].png
     * Ex :
     *   ("bulbasaur", null, false, false)     -> bulbasaur.png
     *   ("bulbasaur", null, true, false)      -> bulbasaur_shiny.png
     *   ("bulbasaur", null, false, true)      -> bulbasaur_female.png
     *   ("bulbasaur", null, true, true)       -> bulbasaur_female_shiny.png
     *   ("rattata", "alolan", true, false)    -> rattata_alolan_shiny.png
     *   ("arbok", "pattern_heart", false, false) -> arbok_pattern_heart.png
     */
    public String resolveCobblemonTextureUrl(
            Short nationalDexNumber,
            String slug,
            String form,
            boolean shiny,
            boolean female
    ) {
        if (nationalDexNumber == null || slug == null || slug.isBlank()) return null;
        String baseDir = "pokemon/cobblemon-textures/" + formatDexNumber(nationalDexNumber) + "_" + slug + "/";

        StringBuilder sb = new StringBuilder(slug);
        if (form != null && !form.isBlank()) sb.append("_").append(form);
        if (female) sb.append("_female");
        if (shiny) sb.append("_shiny");
        sb.append(".png");

        return resolveUrl(baseDir + sb.toString());
    }

    // ==================================================================
    // Alias rétrocompatibles — pointent désormais sur pokemon/home/
    // ==================================================================

    /** @deprecated utiliser {@link #resolveHomeSpriteUrl(Short)}. */
    @Deprecated
    public String resolveSpriteUrl(Short nationalDexNumber) {
        return resolveHomeSpriteUrl(nationalDexNumber);
    }

    /** @deprecated utiliser {@link #resolveHomeSpriteUrl(Short)}. */
    @Deprecated
    public String resolveImageUrl(Short nationalDexNumber) {
        return resolveHomeSpriteUrl(nationalDexNumber);
    }

    /**
     * @deprecated les sprites Home n'ont pas de mapping direct via formCode ;
     * on retombe sur la forme par défaut. Utiliser resolveHomeSpriteByFormId()
     * quand on connaît le form-ID numérique.
     */
    @Deprecated
    public String resolveImageUrl(Short nationalDexNumber, String formCode) {
        return resolveHomeSpriteUrl(nationalDexNumber);
    }

    /** @deprecated utiliser {@link #resolveHomeSpriteUrl(Short)}. */
    @Deprecated
    public String resolveIconUrl(Short nationalDexNumber) {
        return resolveHomeSpriteUrl(nationalDexNumber);
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private String formatDexNumber(Short dex) {
        return String.format("%04d", dex.intValue());
    }

    private String resolveUrl(String relativePath) {
        Path filePath = ConfiguredPathResolver.resolve(appProperties.assets().path())
                .resolve(relativePath)
                .normalize();

        if (!Files.exists(filePath)) {
            return null;
        }

        return appProperties.assets().baseUrl() + "/assets/" + relativePath;
    }

    /**
     * URL du .glb généré par cobblemon-asset-builder.
     * Layout : pokemon/glb/{NNNN}_{slug}/{slug}[_shiny].glb
     */
    public String resolveGltfUrl(Short dex, String slug, boolean shiny) {
        if (dex == null || slug == null || slug.isBlank()) return null;
        String suffix = shiny ? "_shiny" : "";
        String relative = "pokemon/glb/" + formatDexNumber(dex) + "_" + slug
                + "/" + slug + suffix + ".glb";
        return resolveUrl(relative);
    }
}
