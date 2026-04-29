'use client';

import { useRouter, useSearchParams } from 'next/navigation';
import { useCallback, useState } from 'react';

export interface PokemonFilters {
    search: string;
    generationCode: string;
    implemented: boolean | undefined;
    page: number;
}

export function usePokemonFilters() {
    const router = useRouter();
    const searchParams = useSearchParams();

    const [filters, setFilters] = useState<PokemonFilters>({
        search: searchParams.get('search') ?? '',
        generationCode: searchParams.get('generationCode') ?? '',
        implemented: searchParams.get('implemented') === 'true' ? true : undefined,
        page: Number(searchParams.get('page') ?? '0'),
    });

    const updateFilters = useCallback((updates: Partial<PokemonFilters>) => {
        setFilters((prev) => {
            const next = { ...prev, ...updates };
            // Reset page quand on change un filtre
            if (!('page' in updates)) next.page = 0;

            const params = new URLSearchParams();
            if (next.search) params.set('search', next.search);
            if (next.generationCode) params.set('generationCode', next.generationCode);
            if (next.implemented !== undefined) params.set('implemented', String(next.implemented));
            if (next.page > 0) params.set('page', String(next.page));

            router.push(`?${params.toString()}`, { scroll: false });
            return next;
        });
    }, [router]);

    return { filters, updateFilters };
}