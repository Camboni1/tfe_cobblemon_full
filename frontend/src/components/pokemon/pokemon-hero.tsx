'use client';

import { useState } from 'react';
import Image from 'next/image';
import { TypeBadge } from '@/components/ui/badge';
import { formatDexNumber, formatGeneration } from '@/lib/utils/pokemon';
import { PokemonModel3DStage } from './pokemon-3d-stage';
import type { PokemonDetails, PokemonForm } from '@/types/api/pokemon.types';

interface PokemonHeroProps {
    pokemon: PokemonDetails;
    activeForm: PokemonForm;
}

const COBBLEMON_3D_ASSET_VERSION = 'cobblemon-gltf-v2';

function withAssetVersion(url: string | null): string | null {
    if (!url) return null;
    const separator = url.includes('?') ? '&' : '?';
    return `${url}${separator}v=${COBBLEMON_3D_ASSET_VERSION}`;
}

export function PokemonHero({ pokemon, activeForm }: PokemonHeroProps) {
    const [shiny, setShiny] = useState(false);
    const [female, setFemale] = useState(false);

    const sprites = activeForm.homeSprites ?? pokemon.homeSprites;
    const model = activeForm.model ?? pokemon.model;

    // 2D sprite (fallback)
    const pickSprite = () => {
        if (shiny && female && sprites.shinyFemaleUrl) return sprites.shinyFemaleUrl;
        if (shiny && sprites.shinyUrl) return sprites.shinyUrl;
        if (female && sprites.femaleUrl) return sprites.femaleUrl;
        return sprites.defaultUrl;
    };

    // 3D : Cobblemon garde le même modèle pour les aspects type shiny/female
    // et remplace surtout la texture. On ne change de GLB que si aucun PNG
    // de variante n'existe.
    const pickGltf = (): { url: string | null; textureOverrideUrl: string | null } | null => {
        if (!model) return null;
        if (shiny && female && model.textureShinyFemaleUrl) {
            return {
                url: model.gltfUrl ?? model.gltfShinyUrl,
                textureOverrideUrl: model.textureShinyFemaleUrl,
            };
        }
        if (shiny && model.textureShinyUrl) {
            return {
                url: model.gltfUrl ?? model.gltfShinyUrl,
                textureOverrideUrl: model.textureShinyUrl,
            };
        }
        if (shiny) {
            return {
                url: model.gltfShinyUrl ?? model.gltfUrl,
                textureOverrideUrl: null,
            };
        }
        if (female && model.textureFemaleUrl) {
            return {
                url: model.gltfUrl,
                textureOverrideUrl: model.textureFemaleUrl,
            };
        }
        return { url: model.gltfUrl, textureOverrideUrl: null };
    };

    const heroImageSrc =
        pickSprite() ??
        `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${pokemon.nationalDexNumber}.png`;

    const hasFemaleVariant = Boolean(sprites.femaleUrl) || Boolean(model?.textureFemaleUrl);

    const gltf = pickGltf();
    const gltfUrl = withAssetVersion(gltf?.url ?? null);
    const gltfTextureUrl = withAssetVersion(gltf?.textureOverrideUrl ?? null);
    const has3D = Boolean(gltfUrl && pokemon.implemented);

    const fallback2D = (
        <div className="relative w-full h-full flex items-center justify-center">
            <div className="absolute inset-0 rounded-full bg-white/5" />
            {pokemon.implemented ? (
                <Image
                    src={heroImageSrc}
                    alt={activeForm.displayName}
                    width={160}
                    height={160}
                    className="object-contain drop-shadow-2xl"
                    unoptimized
                />
            ) : (
                <div className="w-full h-full flex items-center justify-center text-5xl opacity-20">?</div>
            )}
        </div>
    );

    return (
        <div
            className="rounded-2xl border p-8 flex flex-col sm:flex-row items-center gap-8"
            style={{ backgroundColor: 'var(--color-bg-card)', borderColor: 'var(--color-border)' }}
        >
            {/* 3D viewer if available, else 2D image */}
            <div className="relative w-full sm:w-80 h-80 flex-shrink-0">
                {has3D ? (
                    <PokemonModel3DStage
                        url={gltfUrl!}
                        textureUrl={gltfTextureUrl}
                        fallback={fallback2D}
                    />
                ) : (
                    fallback2D
                )}
            </div>

            {/* Infos (inchangé) */}
            <div className="flex-1 text-center sm:text-left space-y-3">
                <div className="flex flex-wrap items-center gap-2 justify-center sm:justify-start">
                    <span className="text-sm font-mono" style={{ color: 'var(--color-text-secondary)' }}>
                        {formatDexNumber(pokemon.nationalDexNumber)}
                    </span>
                    {!pokemon.implemented && (
                        <span className="text-xs px-2 py-0.5 rounded-full bg-white/10 text-gray-400">
                            Non implémenté
                        </span>
                    )}
                </div>

                <h1 className="text-4xl font-bold text-white">{pokemon.displayName}</h1>

                {activeForm.displayName !== pokemon.displayName && (
                    <p className="text-sm" style={{ color: 'var(--color-text-secondary)' }}>
                        Forme : {activeForm.displayName}
                    </p>
                )}

                <div className="flex gap-2 justify-center sm:justify-start">
                    <TypeBadge type={activeForm.primaryType} />
                    {activeForm.secondaryType && <TypeBadge type={activeForm.secondaryType} />}
                </div>

                <p className="text-sm" style={{ color: 'var(--color-text-secondary)' }}>
                    {formatGeneration(pokemon.generationCode)}
                    {activeForm.battleOnly && (
                        <span className="ml-3 text-xs px-2 py-0.5 rounded-full bg-yellow-500/20 text-yellow-400">
                            Forme de combat uniquement
                        </span>
                    )}
                </p>

                {/* Switcher shiny / femelle (inchangé) */}
                <div className="flex gap-2 justify-center sm:justify-start pt-2">
                    <button
                        type="button"
                        onClick={() => setShiny((s) => !s)}
                        className={`text-xs px-3 py-1 rounded-full border transition ${
                            shiny
                                ? 'bg-yellow-400/20 border-yellow-400 text-yellow-200'
                                : 'bg-white/5 border-white/10 text-gray-300'
                        }`}
                    >
                        ✦ Shiny
                    </button>
                    {hasFemaleVariant && (
                        <button
                            type="button"
                            onClick={() => setFemale((f) => !f)}
                            className={`text-xs px-3 py-1 rounded-full border transition ${
                                female
                                    ? 'bg-pink-400/20 border-pink-400 text-pink-200'
                                    : 'bg-white/5 border-white/10 text-gray-300'
                            }`}
                        >
                            ♀ Femelle
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}
