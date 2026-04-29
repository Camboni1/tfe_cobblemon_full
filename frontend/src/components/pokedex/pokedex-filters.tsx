'use client';

import { useDebounce } from '@/hooks/use-debounce';
import { GENERATION_OPTIONS } from '@/lib/constants/filters';
import type { PokemonFilters } from '@/hooks/use-pokemon-filters';
import { useEffect, useState } from 'react';

interface PokedexFiltersProps {
    filters: PokemonFilters;
    onUpdate: (updates: Partial<PokemonFilters>) => void;
    total: number;
}

export function PokedexFilters({ filters, onUpdate, total }: PokedexFiltersProps) {
    const [searchInput, setSearchInput] = useState(filters.search);
    const debouncedSearch = useDebounce(searchInput, 400);

    useEffect(() => {
        if (debouncedSearch !== filters.search) {
            onUpdate({ search: debouncedSearch });
        }
    }, [debouncedSearch]); // eslint-disable-line react-hooks/exhaustive-deps

    const inputClass =
        'w-full px-4 py-2.5 rounded-lg text-sm text-white border outline-none transition-colors focus:border-red-500';
    const inputStyle = {
        backgroundColor: 'var(--color-bg-card)',
        borderColor: 'var(--color-border)',
    };

    return (
        <div className="flex flex-col sm:flex-row gap-3 items-start sm:items-center">
            {/* Barre de recherche */}
            <div className="relative flex-1 max-w-sm">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">🔍</span>
                <input
                    type="text"
                    placeholder="Rechercher un Pokémon..."
                    value={searchInput}
                    onChange={(e) => setSearchInput(e.target.value)}
                    className={inputClass}
                    style={{ ...inputStyle, paddingLeft: '2.25rem' }}
                />
            </div>

            {/* Filtre génération */}
            <select
                value={filters.generationCode}
                onChange={(e) => onUpdate({ generationCode: e.target.value })}
                className={inputClass}
                style={{ ...inputStyle, width: 'auto' }}
            >
                {GENERATION_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value} style={{ backgroundColor: '#161625' }}>
                        {opt.label}
                    </option>
                ))}
            </select>

            {/* Toggle Implémentés */}
            <label className="flex items-center gap-2 cursor-pointer flex-shrink-0">
                <div
                    onClick={() =>
                        onUpdate({ implemented: filters.implemented === true ? undefined : true })
                    }
                    className={`w-10 h-5 rounded-full transition-colors cursor-pointer ${
                        filters.implemented === true ? 'bg-red-600' : 'bg-white/20'
                    }`}
                >
                    <div
                        className={`w-4 h-4 bg-white rounded-full mt-0.5 transition-transform ${
                            filters.implemented === true ? 'translate-x-5' : 'translate-x-0.5'
                        }`}
                    />
                </div>
                <span className="text-sm" style={{ color: 'var(--color-text-secondary)' }}>
          Implémentés seulement
        </span>
            </label>

            {/* Compteur */}
            <span className="text-sm flex-shrink-0" style={{ color: 'var(--color-text-secondary)' }}>
        {total.toLocaleString('fr-FR')} résultats
      </span>
        </div>
    );
}