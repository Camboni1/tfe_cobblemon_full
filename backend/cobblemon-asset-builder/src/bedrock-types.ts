/**
 * Types pour le format Bedrock Geometry (.geo.json) tel qu'utilisé par Cobblemon.
 * On reste minimal : on ne couvre que ce dont on a besoin pour produire
 * la géométrie statique. Animations, locators et texture_meshes ne sont pas ici.
 */

export interface BoxFaceUV {
    uv: [number, number];
    uv_size: [number, number];
}

export interface BedrockCube {
    origin: [number, number, number];
    size: [number, number, number];
    /**
     * Soit le format "box UV" (couple [u, v] et déroulé canonique),
     * soit un objet {north, south, east, west, up, down} avec UV explicites par face.
     */
    uv:
        | [number, number]
        | {
              north?: BoxFaceUV;
              south?: BoxFaceUV;
              east?: BoxFaceUV;
              west?: BoxFaceUV;
              up?: BoxFaceUV;
              down?: BoxFaceUV;
          };
    inflate?: number;
    mirror?: boolean;
    /** Rotation locale du cube autour de son propre pivot, en degrés. */
    rotation?: [number, number, number];
    /** Pivot du cube (sinon utilise le pivot du bone). */
    pivot?: [number, number, number];
}

export interface BedrockBone {
    name: string;
    parent?: string;
    pivot?: [number, number, number];
    /** Rotation de repos du bone, en degres (ordre Euler XYZ). */
    rotation?: [number, number, number];
    cubes?: BedrockCube[];
    mirror?: boolean;
}

export interface BedrockGeometry {
    description: {
        identifier: string;
        texture_width: number;
        texture_height: number;
        visible_bounds_width?: number;
        visible_bounds_height?: number;
        visible_bounds_offset?: [number, number, number];
    };
    bones?: BedrockBone[];
}

export interface BedrockGeoFile {
    format_version: string;
    'minecraft:geometry': BedrockGeometry[];
}

// =====================================================================
// Format animation (.animation.json)
// =====================================================================

/** Une valeur de transform : soit un literal numérique, soit une expression MoLang. */
export type MoLangValue = number | string;

/** Triplet [x, y, z] où chaque composante peut être numérique ou MoLang. */
export type MoLangVec3 = [MoLangValue, MoLangValue, MoLangValue];

/** Keyframe Bedrock (forme étendue avec pre/post pour les step modes). */
export interface BedrockKeyframe {
    pre?: MoLangVec3;
    post?: MoLangVec3;
    lerp_mode?: 'linear' | 'catmullrom';
}

/**
 * Un track (rotation, position, scale) peut être :
 *  - un triplet constant (souvent contenant des expressions MoLang dynamiques)
 *  - un objet { "0.0": ..., "2.0": ... } avec des keyframes explicites
 */
export type BedrockTrack =
    | MoLangVec3
    | { [time: string]: MoLangVec3 | BedrockKeyframe };

export interface BedrockBoneAnimation {
    rotation?: BedrockTrack;
    position?: BedrockTrack;
    scale?: BedrockTrack;
}

export interface BedrockAnimation {
    loop?: boolean | 'hold_on_last_frame';
    animation_length?: number | null;
    bones?: { [boneName: string]: BedrockBoneAnimation };
}

export interface BedrockAnimationFile {
    format_version: string;
    animations: { [name: string]: BedrockAnimation };
}
