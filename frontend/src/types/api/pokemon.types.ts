/** Sprites 2D Pokémon Home (matche PokemonSpriteSet côté back). */
export interface PokemonSpriteSet {
    defaultUrl: string | null;
    shinyUrl: string | null;
    femaleUrl: string | null;
    shinyFemaleUrl: string | null;
}

/** Modèle 3D Cobblemon + textures associées (matche PokemonModelAssets côté back). */
export interface PokemonModelAssets {
    modelUrl: string | null;
    textureUrl: string | null;
    textureShinyUrl: string | null;
    textureFemaleUrl: string | null;
    textureShinyFemaleUrl: string | null;
    gltfUrl: string | null;          // NOUVEAU
    gltfShinyUrl: string | null;     // NOUVEAU
}

export interface PokemonListItem {
    id: number;
    slug: string;
    displayName: string;
    nationalDexNumber: number;
    generationCode: string;
    implemented: boolean;
    homeSprites: PokemonSpriteSet;
}

export interface PokemonForm {
    id: number;
    code: string;
    displayName: string;
    isDefault: boolean;
    battleOnly: boolean;
    primaryType: string;
    secondaryType: string | null;
    baseHp: number;
    baseAttack: number;
    baseDefense: number;
    baseSpecialAttack: number;
    baseSpecialDefense: number;
    baseSpeed: number;
    homeSprites: PokemonSpriteSet;
    model: PokemonModelAssets;
    drops: PokemonDropItem[];
}

export interface PokemonDropItem {
    id: number;
    itemNamespacedId: string;
    itemDisplayName: string;
    itemIsPlaceholder: boolean;
    quantityMin: number | null;
    quantityMax: number | null;
    percentage: number;
    dropPoolAmountMin: number | null;
    dropPoolAmountMax: number | null;
}

export interface PokemonDetails extends PokemonListItem {
    model: PokemonModelAssets;
    forms: PokemonForm[];
}

export interface PokemonSearchParams {
    search?: string;
    generationCode?: string;
    implemented?: boolean;
    page?: number;
    size?: number;
}