import { apiFetch } from './client';
import type { PokemonDetails, PokemonListItem, PokemonSearchParams } from '@/types/api/pokemon.types';
import type { SpringPage } from '@/types/api/common.types';
import type { SpawnRule } from '@/types/api/spawn.types';

export const pokemonApi = {
    search(params: PokemonSearchParams): Promise<SpringPage<PokemonListItem>> {
        return apiFetch('/api/v1/pokemon', {
            params: {
                search: params.search,
                generationCode: params.generationCode,
                implemented: params.implemented,
                page: params.page ?? 0,
                size: params.size ?? 20,
            },
        });
    },

    getBySlug(slug: string): Promise<PokemonDetails> {
        return apiFetch(`/api/v1/pokemon/${slug}`);
    },

    getSpawns(slug: string): Promise<SpawnRule[]> {
        return apiFetch(`/api/v1/pokemon/${slug}/spawns`);
    },
};