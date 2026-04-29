'use client';

import { Suspense, useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useDebounce } from '@/hooks/use-debounce';
import { usePokemonFilters } from '@/hooks/use-pokemon-filters';
import { pokemonApi } from '@/lib/api/pokemon.api';
import { PokemonGrid } from '@/components/pokedex/pokemon-grid';

/**
 * Générations Cobblemon. Les codes (GEN_1 ... GEN_9) correspondent à ceux
 * attendus par l'API Spring dans la colonne generationCode.
 */
const GENERATIONS = [
    { value: '',      label: 'Toutes les générations' },
    { value: 'GEN_1', label: 'Génération I' },
    { value: 'GEN_2', label: 'Génération II' },
    { value: 'GEN_3', label: 'Génération III' },
    { value: 'GEN_4', label: 'Génération IV' },
    { value: 'GEN_5', label: 'Génération V' },
    { value: 'GEN_6', label: 'Génération VI' },
    { value: 'GEN_7', label: 'Génération VII' },
    { value: 'GEN_8', label: 'Génération VIII' },
    { value: 'GEN_9', label: 'Génération IX' },
] as const;

const PAGE_SIZE = 48;

function PokedexContent() {
    const { filters, updateFilters } = usePokemonFilters();

    const [searchInput, setSearchInput] = useState(filters.search);
    const debouncedSearch = useDebounce(searchInput, 400);

    // Ouvre la barre par défaut si l'URL contient déjà des filtres actifs.
    const hasActiveFilters =
        Boolean(filters.search) ||
        filters.generationCode !== '' ||
        filters.implemented === true;
    const [searchOpen, setSearchOpen] = useState(hasActiveFilters);

    useEffect(() => {
        setSearchInput(filters.search);
    }, [filters.search]);

    useEffect(() => {
        if (debouncedSearch !== filters.search) {
            updateFilters({ search: debouncedSearch });
        }
    }, [debouncedSearch, filters.search, updateFilters]);

    const { data, isLoading, isError } = useQuery({
        queryKey: ['pokemon', filters],
        queryFn: () =>
            pokemonApi.search({
                search: filters.search || undefined,
                generationCode: filters.generationCode || undefined,
                implemented: filters.implemented,
                page: filters.page,
                size: PAGE_SIZE,
            }),
        placeholderData: (prev) => prev,
    });

    const entries = data?.content ?? [];
    const totalPages = Math.max(data?.totalPages ?? 1, 1);
    const currentPage = (data?.number ?? 0) + 1;
    const totalElements = data?.totalElements ?? 0;

    const goToPage = (nextPage: number) => {
        const clamped = Math.min(Math.max(nextPage, 0), totalPages - 1);
        updateFilters({ page: clamped });
        // Remonte discrètement en haut de la grille à chaque changement de page
        if (typeof window !== 'undefined') {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }
    };

    return (
        <div className="mx-auto w-full max-w-[1180px] px-3 pb-16 pt-4 sm:px-5 space-y-5">
            {/* ───────────── Titre ───────────── */}
            <h1 className="home-page-title">À la découverte des Pokémon</h1>

            {/* ───────────── Barre de recherche / filtres ───────────── */}
            {!searchOpen ? (
                <button
                    type="button"
                    onClick={() => setSearchOpen(true)}
                    className="home-search-bar"
                    aria-expanded={false}
                    aria-controls="pokedex-filters-panel"
                >
                    <FilterIcon />
                    <span className="flex-1 text-left">Recherche</span>
                    <ChevronDownIcon />
                </button>
            ) : (
                <div id="pokedex-filters-panel" className="home-panel home-fade-in">
                    <div className="flex items-center justify-between gap-3">
                        <div
                            className="flex items-center gap-2 text-sm font-semibold"
                            style={{ color: 'var(--color-primary-strong)' }}
                        >
                            <FilterIcon />
                            <span>Recherche</span>
                        </div>

                        <button
                            type="button"
                            onClick={() => setSearchOpen(false)}
                            aria-label="Fermer la recherche"
                            className="home-fab-outline"
                            style={{ width: 34, height: 34 }}
                        >
                            <ChevronUpIcon />
                        </button>
                    </div>

                    <div className="mt-4 space-y-3">
                        <input
                            type="text"
                            placeholder="Rechercher un Pokémon par nom..."
                            value={searchInput}
                            onChange={(e) => setSearchInput(e.target.value)}
                            className="home-input"
                        />

                        <select
                            value={filters.generationCode}
                            onChange={(e) => updateFilters({ generationCode: e.target.value })}
                            className="home-select"
                        >
                            {GENERATIONS.map((g) => (
                                <option key={g.value} value={g.value}>
                                    {g.label}
                                </option>
                            ))}
                        </select>

                        <button
                            type="button"
                            onClick={() =>
                                updateFilters({
                                    implemented:
                                        filters.implemented === true ? undefined : true,
                                })
                            }
                            className={
                                filters.implemented === true
                                    ? 'home-toggle home-toggle-active'
                                    : 'home-toggle'
                            }
                        >
                            Implémentés seulement
                        </button>
                    </div>
                </div>
            )}

            {/* ───────────── Compteur ───────────── */}
            {!isLoading && !isError && (
                <p
                    className="text-center text-sm"
                    style={{ color: 'var(--color-text-secondary)' }}
                >
                    {totalElements.toLocaleString('fr-FR')} Pokémon
                </p>
            )}

            {/* ───────────── Grille / états ───────────── */}
            {isLoading ? (
                <div className="flex justify-center py-16">
                    <div className="home-spinner" aria-label="Chargement" />
                </div>
            ) : isError ? (
                <div className="home-panel home-empty-state">
                    <p className="home-empty-title">Impossible de charger le Pokédex</p>
                    <p className="home-empty-desc">
                        Vérifie la connexion à l&apos;API et réessaie.
                    </p>
                </div>
            ) : (
                <PokemonGrid pokemon={entries} />
            )}

            {/* ───────────── Pagination FAB style HOME ───────────── */}
            {!isLoading && !isError && totalPages > 1 && (
                <div className="flex items-center justify-center gap-5 pt-6">
                    <button
                        type="button"
                        onClick={() => goToPage(filters.page - 1)}
                        disabled={currentPage <= 1}
                        aria-label="Page précédente"
                        className="home-fab"
                    >
                        <ArrowLeftIcon />
                    </button>

                    <div
                        className="min-w-[110px] text-center select-none"
                        aria-live="polite"
                    >
                        <div
                            className="text-[10px] font-semibold tracking-[0.2em]"
                            style={{ color: 'var(--color-text-muted)' }}
                        >
                            PAGE
                        </div>
                        <div
                            className="text-lg font-bold"
                            style={{ color: 'var(--color-primary-strong)' }}
                        >
                            {currentPage} <span style={{ color: 'var(--color-text-muted)' }}>/</span> {totalPages}
                        </div>
                    </div>

                    <button
                        type="button"
                        onClick={() => goToPage(filters.page + 1)}
                        disabled={currentPage >= totalPages}
                        aria-label="Page suivante"
                        className="home-fab"
                    >
                        <ArrowRightIcon />
                    </button>
                </div>
            )}
        </div>
    );
}

export default function PokedexPage() {
    return (
        <Suspense
            fallback={
                <div className="flex justify-center py-16">
                    <div className="home-spinner" aria-label="Chargement" />
                </div>
            }
        >
            <PokedexContent />
        </Suspense>
    );
}

/* ──────────────────────────────────────────────────────────────
   Icônes SVG inline (indépendantes de lucide-react)
   ────────────────────────────────────────────────────────────── */

function FilterIcon() {
    return (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
             stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
             aria-hidden="true">
            <path d="M22 3H2l8 9.46V19l4 2v-8.54L22 3z" />
        </svg>
    );
}

function ChevronDownIcon() {
    return (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none"
             stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"
             aria-hidden="true">
            <polyline points="6 9 12 15 18 9" />
        </svg>
    );
}

function ChevronUpIcon() {
    return (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
             stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"
             aria-hidden="true">
            <polyline points="18 15 12 9 6 15" />
        </svg>
    );
}

function ArrowLeftIcon() {
    return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none"
             stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"
             aria-hidden="true">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
        </svg>
    );
}

function ArrowRightIcon() {
    return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none"
             stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"
             aria-hidden="true">
            <line x1="5" y1="12" x2="19" y2="12" />
            <polyline points="12 5 19 12 12 19" />
        </svg>
    );
}
