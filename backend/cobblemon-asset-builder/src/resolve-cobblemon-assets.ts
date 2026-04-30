import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { basename, join } from 'node:path';

export interface ResolverLayer {
    name?: string;
    texture?: TextureReference;
    emissive?: boolean;
    translucent?: boolean;
}

export interface AnimatedTextureReference {
    frames: string[];
    fps?: number;
    loop?: boolean;
}

export type TextureReference = string | AnimatedTextureReference;

interface ResolverVariation {
    aspects?: string[];
    poser?: string;
    model?: string;
    texture?: TextureReference;
    layers?: ResolverLayer[];
}

interface ResolverFile {
    species?: string;
    order?: number;
    variations?: ResolverVariation[];
}

interface ResolverEntry {
    path: string;
    order: number;
    fileName: string;
    data: ResolverFile;
}

export interface ResolvedPokemonAssets {
    folder: string;
    slug: string;
    dexPadded: string;
    species: string;
    aspects: string[];
    resolverPaths: string[];
    modelResource: string;
    poserResource: string;
    textureResource: string;
    geoPath: string;
    texturePath: string;
    poserPath: string | null;
    defaultAnimationGroup: string;
    layers: ResolverLayer[];
    outputSlug: string;
}

export class CobblemonAssetRepository {
    private readonly modelIndex = new Map<string, string>();
    private readonly poserIndex = new Map<string, string>();
    private readonly animationIndex = new Map<string, string>();
    private readonly resolverFolders = new Map<string, string>();
    private readonly modelFolders = new Map<string, string>();

    constructor(private readonly root: string) {
        this.indexFolders();
        this.indexFiles(join(root, 'cobblemon-models'), '.geo.json', this.modelIndex);
        this.indexFiles(join(root, 'cobblemon-posers'), '.json', this.poserIndex);
        this.indexFiles(join(root, 'cobblemon-animations'), '.animation.json', this.animationIndex);
    }

    listSlugs(): string[] {
        const slugs = new Set<string>([
            ...this.resolverFolders.keys(),
            ...this.modelFolders.keys(),
        ]);
        return [...slugs].sort();
    }

    resolvePokemon(
        slugOrSpecies: string,
        options: { aspects?: string[] } = {},
    ): ResolvedPokemonAssets | null {
        const slug = normalizeSpeciesSlug(slugOrSpecies);
        const aspects = normalizeAspects(options.aspects ?? []);
        const resolverEntries = this.loadResolverEntries(slug);

        if (resolverEntries.length === 0) {
            return this.resolveWithoutResolver(slug, aspects);
        }

        const merged = mergeMatchingVariations(resolverEntries, aspects);
        const folder = this.folderForSlug(slug);
        const dexPadded = folder.split('_')[0] ?? '0000';
        const species = resolverEntries.find((e) => e.data.species)?.data.species ?? `cobblemon:${slug}`;

        const modelResource = merged.model ?? `cobblemon:${slug}.geo`;
        const poserResource = merged.poser ?? `cobblemon:${slug}`;
        if (!merged.texture) {
            throw new Error(`${slug}: resolver did not provide a base texture`);
        }

        const modelKey = modelKeyFromResource(modelResource);
        const poserKey = pathKeyFromResource(poserResource);
        const textureResource = firstTextureFrame(merged.texture);
        const geoPath = this.modelIndex.get(modelKey);
        const texturePath = this.resolveTexturePath(textureResource);
        const poserPath = this.poserIndex.get(poserKey) ?? null;

        if (!geoPath) throw new Error(`${slug}: model not found for ${modelResource}`);
        if (!texturePath) throw new Error(`${slug}: texture not found for ${textureResource}`);

        return {
            folder,
            slug,
            dexPadded,
            species,
            aspects,
            resolverPaths: resolverEntries.map((entry) => entry.path),
            modelResource,
            poserResource,
            textureResource,
            geoPath,
            texturePath,
            poserPath,
            defaultAnimationGroup: poserKey,
            layers: merged.layers,
            outputSlug: aspects.length > 0 ? `${slug}_${aspects.join('_')}` : slug,
        };
    }

    resolveAnimationPath(group: string): string | null {
        return this.animationIndex.get(pathKeyFromResource(group)) ?? null;
    }

    private resolveWithoutResolver(slug: string, aspects: string[]): ResolvedPokemonAssets | null {
        const folder = this.folderForSlug(slug);
        const dexPadded = folder.split('_')[0] ?? '0000';
        const geoPath = this.modelIndex.get(slug);
        const texturePath = this.resolveTexturePath(`cobblemon:textures/pokemon/${folder}/${slug}.png`);
        if (!geoPath || !texturePath) return null;

        return {
            folder,
            slug,
            dexPadded,
            species: `cobblemon:${slug}`,
            aspects,
            resolverPaths: [],
            modelResource: `cobblemon:${slug}.geo`,
            poserResource: `cobblemon:${slug}`,
            textureResource: `cobblemon:textures/pokemon/${folder}/${slug}.png`,
            geoPath,
            texturePath,
            poserPath: this.poserIndex.get(slug) ?? null,
            defaultAnimationGroup: slug,
            layers: [],
            outputSlug: aspects.length > 0 ? `${slug}_${aspects.join('_')}` : slug,
        };
    }

    private folderForSlug(slug: string): string {
        return basename(this.resolverFolders.get(slug) ?? this.modelFolders.get(slug) ?? slug);
    }

    private loadResolverEntries(slug: string): ResolverEntry[] {
        const dir = this.resolverFolders.get(slug);
        if (!dir) return [];

        const entries = readdirSync(dir)
            .filter((name) => name.endsWith('.json'))
            .map((fileName) => {
                const path = join(dir, fileName);
                const data = JSON.parse(readFileSync(path, 'utf-8')) as ResolverFile;
                return {
                    path,
                    order: data.order ?? 0,
                    fileName,
                    data,
                };
            });

        return entries.sort((a, b) => a.order - b.order || a.fileName.localeCompare(b.fileName));
    }

    private resolveTexturePath(textureResource: string): string | null {
        const path = resourcePath(textureResource);
        const prefix = 'textures/pokemon/';
        const textureRelativePath = path.startsWith(prefix) ? path.slice(prefix.length) : path;
        const texturePath = join(this.root, 'cobblemon-textures', ...textureRelativePath.split('/'));
        return existsSync(texturePath) ? texturePath : null;
    }

    private indexFolders() {
        this.indexPokemonFolders('cobblemon-resolvers', this.resolverFolders);
        this.indexPokemonFolders('cobblemon-models', this.modelFolders);
    }

    private indexPokemonFolders(childDir: string, target: Map<string, string>) {
        const dir = join(this.root, childDir);
        if (!existsSync(dir)) return;
        for (const name of readdirSync(dir)) {
            const path = join(dir, name);
            if (!statSync(path).isDirectory()) continue;
            const match = name.match(/^\d{4}_(.+)$/);
            if (match) target.set(match[1], path);
        }
    }

    private indexFiles(dir: string, suffix: string, target: Map<string, string>) {
        if (!existsSync(dir)) return;
        const stack = [dir];
        while (stack.length > 0) {
            const current = stack.pop()!;
            for (const name of readdirSync(current)) {
                const path = join(current, name);
                const stat = statSync(path);
                if (stat.isDirectory()) {
                    stack.push(path);
                    continue;
                }
                if (!name.endsWith(suffix)) continue;
                target.set(name.slice(0, -suffix.length), path);
            }
        }
    }
}

function mergeMatchingVariations(entries: ResolverEntry[], aspects: string[]) {
    const aspectSet = new Set(aspects);
    const layersByName = new Map<string, ResolverLayer>();
    const merged: {
        model?: string;
        poser?: string;
        texture?: TextureReference;
        layers: ResolverLayer[];
    } = { layers: [] };

    for (const entry of entries) {
        for (const variation of entry.data.variations ?? []) {
            const variationAspects = normalizeAspects(variation.aspects ?? []);
            if (!variationAspects.every((aspect) => aspectSet.has(aspect))) continue;

            if (variation.model !== undefined) merged.model = variation.model;
            if (variation.poser !== undefined) merged.poser = variation.poser;
            if (variation.texture !== undefined) merged.texture = variation.texture;
            for (const layer of variation.layers ?? []) {
                layersByName.set(layer.name ?? `layer_${layersByName.size}`, layer);
            }
        }
    }

    merged.layers = [...layersByName.values()];
    return merged;
}

function firstTextureFrame(texture: TextureReference): string {
    return typeof texture === 'string' ? texture : texture.frames[0];
}

function normalizeSpeciesSlug(slugOrSpecies: string): string {
    const value = slugOrSpecies.includes(':') ? slugOrSpecies.split(':')[1] : slugOrSpecies;
    return value.trim().toLowerCase();
}

function normalizeAspects(aspects: string[]): string[] {
    return [...new Set(aspects.flatMap((aspect) => aspect.split(',')))]
        .map((aspect) => aspect.trim().toLowerCase())
        .filter(Boolean)
        .sort();
}

function modelKeyFromResource(ref: string): string {
    const key = pathKeyFromResource(ref);
    return key.endsWith('.geo') ? key.slice(0, -4) : key;
}

function pathKeyFromResource(ref: string): string {
    const path = resourcePath(ref);
    return basename(path);
}

function resourcePath(ref: string): string {
    return ref.includes(':') ? ref.split(':').slice(1).join(':') : ref;
}
