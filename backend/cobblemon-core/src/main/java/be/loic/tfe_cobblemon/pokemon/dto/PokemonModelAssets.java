package be.loic.tfe_cobblemon.pokemon.dto;

/** Modèle 3D Cobblemon + textures associées. */
public record PokemonModelAssets(
        String modelUrl,
        String textureUrl,
        String textureShinyUrl,
        String textureFemaleUrl,
        String textureShinyFemaleUrl
) {}