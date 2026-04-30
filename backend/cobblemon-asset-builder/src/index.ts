import { existsSync, mkdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { NodeIO } from '@gltf-transform/core';
import {
    parseAnimationFile,
    parseGeoFile,
    parsePoserPoseAnimation,
    type BedrockAnimationReference,
} from './parse-bedrock.js';
import { buildGltfDocument } from './build-gltf.js';
import { bakeAnimation, type SampledAnimation } from './animation-baker.js';
import type { BedrockAnimation, BedrockBone } from './bedrock-types.js';
import {
    CobblemonAssetRepository,
    type ResolvedPokemonAssets,
} from './resolve-cobblemon-assets.js';

interface CliOptions {
    all: boolean;
    slugs: string[];
    poseName: string;
    aspects: string[];
    force: boolean;
}

interface SelectedAnimation {
    path: string;
    key: string;
    animation: BedrockAnimation;
}

const ASSETS_PATH = resolve(process.env.ASSETS_PATH ?? '../assets/pokemon');

function parseArgs(args: string[]): CliOptions {
    const options: CliOptions = {
        all: false,
        slugs: [],
        poseName: 'standing',
        aspects: [],
        force: false,
    };

    for (let i = 0; i < args.length; i++) {
        const arg = args[i];
        if (arg === '--all') {
            options.all = true;
        } else if (arg === '--force') {
            options.force = true;
        } else if (arg === '--pose' || arg === '--state') {
            const value = args[++i];
            if (!value) throw new Error(`${arg} requires a pose name`);
            options.poseName = value;
        } else if (arg === '--aspects' || arg === '--aspect') {
            const value = args[++i];
            if (!value) throw new Error(`${arg} requires one or more aspects`);
            options.aspects.push(...value.split(','));
        } else if (arg.startsWith('--')) {
            throw new Error(`Unknown option: ${arg}`);
        } else {
            options.slugs.push(arg);
        }
    }

    options.aspects = normalizeAspects(options.aspects);
    return options;
}

function selectAnimation(
    repository: CobblemonAssetRepository,
    assets: ResolvedPokemonAssets,
    poseName: string,
): SelectedAnimation | null {
    const poseAnimation = assets.poserPath
        ? parsePoserPoseAnimation(assets.poserPath, poseName)
        : null;

    const candidates: BedrockAnimationReference[] = [];
    if (poseAnimation) candidates.push(poseAnimation);

    const fallbackGroup = poseAnimation?.group ?? assets.defaultAnimationGroup;
    for (const name of fallbackAnimationNames(poseName)) {
        candidates.push({ group: fallbackGroup, name });
    }

    const triedKeys = new Set<string>();
    const groupsToTry = new Set<string>([
        ...candidates.map((candidate) => candidate.group),
        assets.defaultAnimationGroup,
        assets.slug,
    ]);

    for (const candidate of candidates) {
        const selected = findAnimation(repository, candidate, triedKeys);
        if (selected) return selected;
    }

    for (const group of groupsToTry) {
        const path = repository.resolveAnimationPath(group);
        if (!path) continue;
        const animMap = parseAnimationFile(path);
        const firstKey = animMap.keys().next().value as string | undefined;
        if (!firstKey) continue;
        return {
            path,
            key: firstKey,
            animation: animMap.get(firstKey)!,
        };
    }

    return null;
}

function findAnimation(
    repository: CobblemonAssetRepository,
    candidate: BedrockAnimationReference,
    triedKeys: Set<string>,
): SelectedAnimation | null {
    const key = `animation.${candidate.group}.${candidate.name}`;
    if (triedKeys.has(key)) return null;
    triedKeys.add(key);

    const path = repository.resolveAnimationPath(candidate.group);
    if (!path) return null;

    const animMap = parseAnimationFile(path);
    const animation = animMap.get(key);
    return animation ? { path, key, animation } : null;
}

function fallbackAnimationNames(poseName: string): string[] {
    switch (poseName) {
        case 'battle-standing':
            return ['battle_idle', 'ground_idle', 'idle'];
        case 'walking':
            return ['ground_walk', 'walk', 'ground_idle', 'idle'];
        case 'running':
            return ['ground_run', 'run', 'ground_idle', 'idle'];
        case 'hover':
        case 'battle-flying':
            return ['air_idle', 'ground_idle', 'idle'];
        case 'fly':
            return ['air_fly', 'air_idle', 'ground_idle', 'idle'];
        case 'sleep':
            return ['sleep', 'ground_idle', 'idle'];
        default:
            return ['ground_idle', 'idle'];
    }
}

async function convertOne(
    repository: CobblemonAssetRepository,
    assets: ResolvedPokemonAssets,
    poseName: string,
    force: boolean,
): Promise<void> {
    const outDir = join(ASSETS_PATH, 'glb', assets.folder);
    const outPath = join(outDir, `${assets.outputSlug}.glb`);
    const selectedAnimation = selectAnimation(repository, assets, poseName);

    if (!force && existsSync(outPath)) {
        const outMtime = statSync(outPath).mtimeMs;
        const inputs = [
            assets.geoPath,
            assets.texturePath,
            assets.poserPath,
            selectedAnimation?.path,
            ...assets.resolverPaths,
        ].filter((path): path is string => Boolean(path));
        const newest = Math.max(...inputs.map((path) => statSync(path).mtimeMs));
        if (outMtime >= newest) {
            console.log(`[skip] ${assets.outputSlug}: up to date`);
            return;
        }
    }

    const geo = parseGeoFile(assets.geoPath);
    const textureBytes = new Uint8Array(readFileSync(assets.texturePath));

    let animation: SampledAnimation | undefined;
    if (selectedAnimation) {
        const bonesByName = new Map<string, BedrockBone>();
        for (const bone of geo.bones ?? []) bonesByName.set(bone.name, bone);
        animation = bakeAnimation(selectedAnimation.animation, {
            name: selectedAnimation.key,
            fps: 30,
            bonesByName,
        });
    }

    const doc = buildGltfDocument(geo, textureBytes, {
        modelName: assets.outputSlug,
        flipFront: true,
        animation,
    });

    mkdirSync(dirname(outPath), { recursive: true });
    await new NodeIO().write(outPath, doc);

    const sizeKb = (statSync(outPath).size / 1024).toFixed(1);
    const aspectLabel = assets.aspects.length > 0 ? ` aspects=[${assets.aspects.join(',')}]` : '';
    const animLabel = animation
        ? ` + pose "${poseName}" -> anim "${animation.name}" (${animation.durationSec}s @ ${animation.fps}fps)`
        : ` + pose "${poseName}" -> no animation`;
    console.log(
        `[ok] ${assets.outputSlug}: ${assets.modelResource} + ${assets.textureResource}${aspectLabel}${animLabel} -> ${outPath} (${sizeKb} KB)`,
    );
}

async function main() {
    const options = parseArgs(process.argv.slice(2));
    if (!options.all && options.slugs.length === 0) {
        console.error(
            'Usage:\n  tsx src/index.ts <slug> [--pose standing] [--aspects shiny,female] [--force]\n  tsx src/index.ts --all',
        );
        process.exit(1);
    }

    console.log(`Assets path: ${ASSETS_PATH}`);
    const repository = new CobblemonAssetRepository(ASSETS_PATH);
    const slugs = options.all ? repository.listSlugs() : options.slugs;
    console.log(
        `Converting ${slugs.length} model(s)... pose=${options.poseName} aspects=[${options.aspects.join(',')}]`,
    );

    let ok = 0;
    let skipped = 0;
    let failed = 0;

    for (const slug of slugs) {
        try {
            const assets = repository.resolvePokemon(slug, { aspects: options.aspects });
            if (!assets) {
                console.warn(`[skip] ${slug}: assets not found`);
                skipped++;
                continue;
            }
            await convertOne(repository, assets, options.poseName, options.force);
            ok++;
        } catch (err) {
            failed++;
            console.error(`[err] ${slug}:`, (err as Error).message);
        }
    }

    console.log(`\nDone. ok=${ok} skipped=${skipped} failed=${failed}`);
    if (failed > 0) process.exit(2);
}

function normalizeAspects(aspects: string[]): string[] {
    return [...new Set(aspects.flatMap((aspect) => aspect.split(',')))]
        .map((aspect) => aspect.trim().toLowerCase())
        .filter(Boolean)
        .sort();
}

main().catch((err) => {
    console.error(err);
    process.exit(1);
});
