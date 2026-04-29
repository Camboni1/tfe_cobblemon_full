import { readFileSync } from 'node:fs';
import type {
    BedrockAnimation,
    BedrockAnimationFile,
    BedrockGeoFile,
    BedrockGeometry,
} from './bedrock-types.js';

/**
 * Lit un .geo.json et retourne la géométrie "canonique" (celle dont
 * l'identifier a le moins de points = pas de suffixe de pose).
 */
export function parseGeoFile(path: string): BedrockGeometry {
    const raw = readFileSync(path, 'utf-8');
    const data: BedrockGeoFile = JSON.parse(raw);

    const geos = data['minecraft:geometry'];
    if (!geos || geos.length === 0) {
        throw new Error(`No minecraft:geometry entries in ${path}`);
    }

    if (geos.length === 1) return geos[0];

    const sorted = [...geos].sort((a, b) => {
        const idA = a.description.identifier;
        const idB = b.description.identifier;
        const dotsA = (idA.match(/\./g) ?? []).length;
        const dotsB = (idB.match(/\./g) ?? []).length;
        if (dotsA !== dotsB) return dotsA - dotsB;
        return idA.length - idB.length;
    });
    return sorted[0];
}

/**
 * Lit un .animation.json et retourne la map { animationName → BedrockAnimation }.
 */
export function parseAnimationFile(path: string): Map<string, BedrockAnimation> {
    const raw = readFileSync(path, 'utf-8');
    const data: BedrockAnimationFile = JSON.parse(raw);
    return new Map(Object.entries(data.animations ?? {}));
}

/**
 * Heuristique : à partir d'un poser Cobblemon, retourne le NOM de l'animation
 * qu'on doit utiliser pour la pose statique de portrait/ground.
 *
 * Cherche dans les `poses` :
 *   1. La pose `portrait` si elle existe
 *   2. Sinon une pose dont `poseTypes` contient `STAND` ou `PROFILE`
 *   3. Sinon la première pose
 *
 * Puis dans son tableau `animations`, le premier `q.bedrock('slug', 'name')`.
 * Retourne `name` (ex: `ground_idle`) ou null si rien trouvé.
 */
export function parsePoserPoseAnimationName(path: string): string | null {
    const raw = readFileSync(path, 'utf-8');
    const poser = JSON.parse(raw) as {
        poses?: Record<
            string,
            { poseTypes?: string[]; animations?: (string | unknown)[] }
        >;
    };
    const poses = poser.poses ?? {};

    // Choix de la pose
    let chosen = poses['portrait'];
    if (!chosen) {
        for (const [, p] of Object.entries(poses)) {
            const types = p.poseTypes ?? [];
            if (types.includes('STAND') || types.includes('PROFILE')) {
                chosen = p;
                break;
            }
        }
    }
    if (!chosen) {
        const first = Object.values(poses)[0];
        if (first) chosen = first;
    }
    if (!chosen) return null;

    for (const entry of chosen.animations ?? []) {
        if (typeof entry !== 'string') continue;
        const m = entry.match(
            /q\.bedrock\(\s*['"]([^'"]+)['"]\s*,\s*['"]([^'"]+)['"]/i,
        );
        if (m) return m[2]; // animation name (sans le préfixe "animation.<slug>.")
    }
    return null;
}
