/**
 * Baker d'animation : prend une `BedrockAnimation` et la sample a fps fixe,
 * en evaluant les expressions MoLang.
 *
 * En sortie : un `SampledAnimation` qu'on peut directement écrire en
 * channels glTF (input = times, output = values, interpolation LINEAR).
 */

import type {
    BedrockAnimation,
    BedrockBone,
    BedrockKeyframe,
    BedrockTrack,
    MoLangVec3,
} from './bedrock-types.js';
import { evalAt } from './molang.js';

export interface SampledTrack {
    /** Times array (seconds), length = N */
    times: Float32Array;
    /**
     * Values flat array.
     *  - translation/scale : VEC3, length = N * 3
     *  - rotation          : VEC4 quaternion (x, y, z, w), length = N * 4
     */
    values: Float32Array;
    /** "translation" | "rotation" | "scale" */
    path: 'translation' | 'rotation' | 'scale';
}

export interface SampledAnimation {
    name: string;
    durationSec: number;
    fps: number;
    /** Keyed by bone name. Each bone may have 0..3 tracks. */
    tracks: Map<string, SampledTrack[]>;
}

interface BakeOptions {
    name: string;
    fps?: number;
    /** Default duration when animation_length is null/undefined. */
    defaultDuration?: number;
    /** Lookup of bones in the .geo.json. */
    bonesByName: Map<string, BedrockBone>;
}

export function bakeAnimation(
    anim: BedrockAnimation,
    opts: BakeOptions,
): SampledAnimation {
    const fps = opts.fps ?? 30;
    const duration =
        anim.animation_length && anim.animation_length > 0
            ? anim.animation_length
            : (opts.defaultDuration ?? 4);

    const frameCount = Math.max(2, Math.round(duration * fps) + 1);
    const times = new Float32Array(frameCount);
    for (let i = 0; i < frameCount; i++) {
        times[i] = (i / (frameCount - 1)) * duration;
    }

    const tracksByBone = new Map<string, SampledTrack[]>();

    for (const [boneName, boneAnim] of Object.entries(anim.bones ?? {})) {
        // Pour la translation, le repos est au pivot ; en glTF la translation
        // d'un node est (pivot - parent.pivot). Mais le BAKER produit un DELTA
        // par-dessus le node de repos, et ce delta sera ADDITIONNÉ par
        // build-gltf à la translation de repos avant écriture du channel.
        // (Gérer ça ici simplifierait, mais on le fait côté builder pour
        // garder la responsabilité claire.)

        const tracks: SampledTrack[] = [];

        if (boneAnim.rotation) {
            const restRotation = opts.bonesByName.get(boneName)?.rotation ?? [0, 0, 0];
            tracks.push(bakeRotationTrack(boneName, boneAnim.rotation, times, restRotation));
        }
        if (boneAnim.position) {
            tracks.push(bakeVec3Track(boneAnim.position, times, 'translation'));
        }
        if (boneAnim.scale) {
            tracks.push(bakeVec3Track(boneAnim.scale, times, 'scale'));
        }

        if (tracks.length > 0) tracksByBone.set(boneName, tracks);
    }

    return {
        name: opts.name,
        durationSec: duration,
        fps,
        tracks: tracksByBone,
    };
}

// =====================================================================
// Track baking
// =====================================================================

function bakeRotationTrack(
    boneName: string,
    track: BedrockTrack,
    times: Float32Array,
    restRotation: [number, number, number],
): SampledTrack {
    const N = times.length;
    const values = new Float32Array(N * 4);
    for (let i = 0; i < N; i++) {
        const t = times[i];
        const rotation = sampleVec3(track, t);

        // Bedrock animations are authored as deltas over the model's rest pose.
        // If we emit only the animated value, glTF replaces the node rotation and
        // bones like Charizard's wing bases lose their built-in spread angle.
        const x = restRotation[0] + rotation[0];
        const y = restRotation[1] + rotation[1];
        const z = restRotation[2] + rotation[2];

        const q = eulerXYZToQuat(-x, y, -z);
        values[i * 4 + 0] = q[0];
        values[i * 4 + 1] = q[1];
        values[i * 4 + 2] = q[2];
        values[i * 4 + 3] = q[3];
    }
    return { times, values, path: 'rotation' };
}

function bakeVec3Track(
    track: BedrockTrack,
    times: Float32Array,
    path: 'translation' | 'scale',
): SampledTrack {
    const N = times.length;
    const values = new Float32Array(N * 3);
    for (let i = 0; i < N; i++) {
        const t = times[i];
        const v = sampleVec3(track, t);
        values[i * 3 + 0] = v[0];
        values[i * 3 + 1] = v[1];
        values[i * 3 + 2] = v[2];
    }
    return { times, values, path };
}

/**
 * Sample un track Bedrock (constant ou keyframé) à un temps donné.
 * Toutes les expressions MoLang sont évaluées à `t`.
 */
function sampleVec3(track: BedrockTrack, t: number): [number, number, number] {
    if (Array.isArray(track)) {
        // Forme constante : [x, y, z] où chaque composante est éventuellement MoLang
        return [
            evalAt(track[0], t),
            evalAt(track[1], t),
            evalAt(track[2], t),
        ];
    }

    // Forme keyframée : { "0.0": [...], "1.5": [...], ... }
    const entries = Object.entries(track)
        .map(([k, v]) => ({ time: parseFloat(k), data: v }))
        .filter((e) => Number.isFinite(e.time))
        .sort((a, b) => a.time - b.time);

    if (entries.length === 0) return [0, 0, 0];
    if (t <= entries[0].time) return resolveFrame(entries[0].data, t);
    if (t >= entries[entries.length - 1].time) {
        return resolveFrame(entries[entries.length - 1].data, t);
    }

    // Lerp entre les deux keyframes encadrantes
    for (let i = 0; i < entries.length - 1; i++) {
        const a = entries[i];
        const b = entries[i + 1];
        if (t >= a.time && t <= b.time) {
            const aVec = resolveFrame(a.data, t);
            const bVec = resolveFrame(b.data, t);
            const u = (t - a.time) / (b.time - a.time || 1);
            return [
                aVec[0] + (bVec[0] - aVec[0]) * u,
                aVec[1] + (bVec[1] - aVec[1]) * u,
                aVec[2] + (bVec[2] - aVec[2]) * u,
            ];
        }
    }
    return [0, 0, 0];
}

function resolveFrame(
    data: MoLangVec3 | BedrockKeyframe,
    t: number,
): [number, number, number] {
    if (Array.isArray(data)) {
        return [evalAt(data[0], t), evalAt(data[1], t), evalAt(data[2], t)];
    }
    const v = data.post ?? data.pre;
    if (!v) return [0, 0, 0];
    return [evalAt(v[0], t), evalAt(v[1], t), evalAt(v[2], t)];
}

// =====================================================================
// Quaternion utils
// =====================================================================

/**
 * Euler XYZ (en degres) -> quaternion (x, y, z, w).
 * Les appelants appliquent la conversion Cobblemon/ModelPart vers glTF :
 * X et Z inverses, Y conserve.
 */
export function eulerXYZToQuat(
    xDeg: number,
    yDeg: number,
    zDeg: number,
): [number, number, number, number] {
    const hx = (xDeg * Math.PI) / 360; // /2 puis /180
    const hy = (yDeg * Math.PI) / 360;
    const hz = (zDeg * Math.PI) / 360;
    const cx = Math.cos(hx),
        sx = Math.sin(hx);
    const cy = Math.cos(hy),
        sy = Math.sin(hy);
    const cz = Math.cos(hz),
        sz = Math.sin(hz);

    return [
        sx * cy * cz + cx * sy * sz, // qx
        cx * sy * cz - sx * cy * sz, // qy
        cx * cy * sz + sx * sy * cz, // qz
        cx * cy * cz - sx * sy * sz, // qw
    ];
}
