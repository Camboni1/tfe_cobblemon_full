import { readFileSync } from 'node:fs';
import type {
    BedrockAnimation,
    BedrockAnimationFile,
    BedrockGeoFile,
    BedrockGeometry,
} from './bedrock-types.js';

export interface BedrockAnimationReference {
    group: string;
    name: string;
}

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

export function parseAnimationFile(path: string): Map<string, BedrockAnimation> {
    const raw = readFileSync(path, 'utf-8');
    const data: BedrockAnimationFile = JSON.parse(raw);
    return new Map(Object.entries(data.animations ?? {}));
}

export function parsePoserPoseAnimationName(
    path: string,
    preferredPose = 'standing',
): string | null {
    return parsePoserPoseAnimation(path, preferredPose)?.name ?? null;
}

export function parsePoserPoseAnimation(
    path: string,
    preferredPose = 'standing',
): BedrockAnimationReference | null {
    const raw = readFileSync(path, 'utf-8');
    const poser = JSON.parse(raw) as {
        poses?: Record<
            string,
            {
                poseTypes?: string[];
                animations?: (
                    | string
                    | { animation?: string; condition?: string }
                    | unknown
                )[];
            }
        >;
    };
    const poses = poser.poses ?? {};

    let chosen = poses[preferredPose] ?? poses['standing'] ?? poses['portrait'];
    if (!chosen) {
        for (const [, pose] of Object.entries(poses)) {
            const types = pose.poseTypes ?? [];
            if (types.includes('STAND') || types.includes('PROFILE')) {
                chosen = pose;
                break;
            }
        }
    }
    if (!chosen) {
        const first = Object.values(poses)[0];
        if (first) chosen = first;
    }
    if (!chosen) return null;

    const conditionalEntries: string[] = [];
    for (const entry of chosen.animations ?? []) {
        if (typeof entry === 'string') {
            const parsed = parseBedrockCall(entry);
            if (parsed) return parsed;
            continue;
        }
        if (entry && typeof entry === 'object' && 'animation' in entry) {
            const animation = (entry as { animation?: unknown }).animation;
            if (typeof animation === 'string') conditionalEntries.push(animation);
        }
    }

    for (const entry of conditionalEntries) {
        const parsed = parseBedrockCall(entry);
        if (parsed) return parsed;
    }

    return null;
}

function parseBedrockCall(expr: string): BedrockAnimationReference | null {
    const match = expr.match(
        /q\.bedrock\(\s*['"]([^'"]+)['"]\s*,\s*['"]([^'"]+)['"]/i,
    );
    if (!match) return null;
    return { group: match[1], name: match[2] };
}
