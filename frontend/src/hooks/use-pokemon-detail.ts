import { useQuery } from '@tanstack/react-query';
import { pokemonApi } from '@/lib/api/pokemon.api';

export function usePokemonDetail(slug: string) {
    return useQuery({
        queryKey: ['pokemon', slug],
        queryFn: () => pokemonApi.getBySlug(slug),
        enabled: !!slug,
    });
}

export function usePokemonSpawns(slug: string) {
    return useQuery({
        queryKey: ['pokemon', slug, 'spawns'],
        queryFn: () => pokemonApi.getSpawns(slug),
        enabled: !!slug,
    });
}