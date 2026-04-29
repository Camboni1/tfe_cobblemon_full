/**
 * CLI du converter Cobblemon → glTF.
 *
 * Usage :
 *   tsx src/index.ts charizard               # convertit un Pokémon par slug
 *   tsx src/index.ts --all                   # convertit tout le dossier
 *   ASSETS_PATH=/abs/path tsx src/index.ts charizard
 *
 * Layout d'entrée attendu (sous ASSETS_PATH) :
 *   cobblemon-models/{NNNN}_{slug}/{slug}.geo.json
 *   cobblemon-textures/{NNNN}_{slug}/{slug}.png
 *
 * Layout de sortie :
 *   glb/{NNNN}_{slug}/{slug}.glb
 */

import { existsSync, mkdirSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { NodeIO } from '@gltf-transform/core';
import {
    parseAnimationFile,
    parseGeoFile,
    parsePoserPoseAnimationName,
} from './parse-bedrock.js';
import { buildGltfDocument } from './build-gltf.js';
import { bakeAnimation, type SampledAnimation } from './animation-baker.js';
import type { BedrockBone } from './bedrock-types.js';

interface PokemonAssets {
    /** Préfixe du dossier, e.g. "0006_charizard". */
    folder: string;
    /** Le slug seul, e.g. "charizard". */
    slug: string;
    /** "0006" — extrait du folder. */
    dexPadded: string;
    geoPath: string;
    texturePath: string;
    /** Optionnel : chemin du .animation.json. */
    animationPath: string | null;
    /** Optionnel : chemin du poser .json. */
    poserPath: string | null;
}

const ASSETS_PATH = resolve(process.env.ASSETS_PATH ?? '../assets/pokemon');

function findPokemonAssets(slug: string): PokemonAssets | null {
    const modelsDir = join(ASSETS_PATH, 'cobblemon-models');
    if (!existsSync(modelsDir)) {
        throw new Error(`Models dir not found: ${modelsDir}`);
    }

    const folder = readdirSync(modelsDir).find((d) => {
        if (!statSync(join(modelsDir, d)).isDirectory()) return false;
        const m = d.match(/^(\d{4})_(.+)$/);
        return m !== null && m[2] === slug;
    });
    if (!folder) return null;

    const dexPadded = folder.split('_')[0];
    const geoPath = join(modelsDir, folder, `${slug}.geo.json`);
    const texturePath = join(ASSETS_PATH, 'cobblemon-textures', folder, `${slug}.png`);

    if (!existsSync(geoPath)) {
        console.warn(`[skip] ${slug}: missing geo file at ${geoPath}`);
        return null;
    }
    if (!existsSync(texturePath)) {
        console.warn(`[skip] ${slug}: missing texture at ${texturePath}`);
        return null;
    }

    const animationPath = join(
        ASSETS_PATH,
        'cobblemon-animations',
        folder,
        `${slug}.animation.json`,
    );
    const poserPath = join(ASSETS_PATH, 'cobblemon-posers', folder, `${slug}.json`);

    return {
        folder,
        slug,
        dexPadded,
        geoPath,
        texturePath,
        animationPath: existsSync(animationPath) ? animationPath : null,
        poserPath: existsSync(poserPath) ? poserPath : null,
    };
}

/**
 * Choisit l'animation à utiliser comme pose statique :
 *   1. Le poser dit `q.bedrock(slug, 'foo')` → on prend `animation.{slug}.foo`
 *   2. Sinon `animation.{slug}.ground_idle` si présente
 *   3. Sinon `animation.{slug}.idle`
 *   4. Sinon la première animation
 */
function pickIdleAnimationKey(
    slug: string,
    animMap: Map<string, unknown>,
    poserPath: string | null,
): string | null {
    if (poserPath) {
        const poseAnim = parsePoserPoseAnimationName(poserPath);
        if (poseAnim) {
            const key = `animation.${slug}.${poseAnim}`;
            if (animMap.has(key)) return key;
        }
    }
    const candidates = [
        `animation.${slug}.ground_idle`,
        `animation.${slug}.idle`,
    ];
    for (const c of candidates) if (animMap.has(c)) return c;
    const first = animMap.keys().next().value;
    return first ?? null;
}

function listAllSlugs(): string[] {
    const modelsDir = join(ASSETS_PATH, 'cobblemon-models');
    return readdirSync(modelsDir)
        .filter((d) => statSync(join(modelsDir, d)).isDirectory())
        .map((d) => {
            const m = d.match(/^\d{4}_(.+)$/);
            return m?.[1];
        })
        .filter((s): s is string => Boolean(s))
        .sort();
}

async function convertOne(assets: PokemonAssets): Promise<void> {
    const outDir = join(ASSETS_PATH, 'glb', assets.folder);
    const outPath = join(outDir, `${assets.slug}.glb`);

    // Idempotence : skip si le glb existe et est plus récent que TOUS les inputs
    if (existsSync(outPath)) {
        const outMtime = statSync(outPath).mtimeMs;
        const inputs = [
            assets.geoPath,
            assets.texturePath,
            assets.animationPath,
            assets.poserPath,
        ].filter((p): p is string => Boolean(p));
        const newest = Math.max(...inputs.map((p) => statSync(p).mtimeMs));
        if (outMtime >= newest) {
            console.log(`[skip] ${assets.slug}: up to date`);
            return;
        }
    }

    const geo = parseGeoFile(assets.geoPath);
    const textureBytes = new Uint8Array(readFileSync(assets.texturePath));

    // Bake l'animation idle si on en a une
    let animation: SampledAnimation | undefined;
    if (assets.animationPath) {
        const animMap = parseAnimationFile(assets.animationPath);
        const animKey = pickIdleAnimationKey(assets.slug, animMap, assets.poserPath);
        if (animKey && animMap.has(animKey)) {
            const bonesByName = new Map<string, BedrockBone>();
            for (const b of geo.bones ?? []) bonesByName.set(b.name, b);
            animation = bakeAnimation(animMap.get(animKey)!, {
                name: animKey,
                fps: 30,
                bonesByName,
            });
        }
    }

    const doc = buildGltfDocument(geo, textureBytes, {
        modelName: assets.slug,
        flipFront: true,
        animation,
    });

    mkdirSync(dirname(outPath), { recursive: true });
    const io = new NodeIO();
    await io.write(outPath, doc);

    const sizeKb = (statSync(outPath).size / 1024).toFixed(1);
    const animLabel = animation
        ? ` + anim "${animation.name}" (${animation.durationSec}s @ ${animation.fps}fps)`
        : '';
    console.log(
        `[ok] ${assets.slug}: ${geo.description.identifier}${animLabel} → ${outPath} (${sizeKb} KB)`,
    );
}

async function main() {
    const args = process.argv.slice(2);
    if (args.length === 0) {
        console.error(
            'Usage:\n  tsx src/index.ts <slug>\n  tsx src/index.ts --all',
        );
        process.exit(1);
    }

    console.log(`Assets path: ${ASSETS_PATH}`);

    const slugs = args[0] === '--all' ? listAllSlugs() : [args[0]];
    console.log(`Converting ${slugs.length} model(s)...`);

    let ok = 0;
    let skipped = 0;
    let failed = 0;

    for (const slug of slugs) {
        const assets = findPokemonAssets(slug);
        if (!assets) {
            skipped++;
            continue;
        }
        try {
            await convertOne(assets);
            ok++;
        } catch (err) {
            failed++;
            console.error(`[err] ${slug}:`, (err as Error).message);
        }
    }

    console.log(`\nDone. ok=${ok} skipped=${skipped} failed=${failed}`);
    if (failed > 0) process.exit(2);
}

main().catch((e) => {
    console.error(e);
    process.exit(1);
});
