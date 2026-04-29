'use client';

import { cn } from '@/lib/utils/cn';
import type { PokemonForm } from '@/types/api/pokemon.types';

interface PokemonFormsTabsProps {
    forms: PokemonForm[];
    activeFormId: number;
    onSelect: (form: PokemonForm) => void;
}

export function PokemonFormsTabs({ forms, activeFormId, onSelect }: PokemonFormsTabsProps) {
    if (forms.length <= 1) return null;

    return (
        <section className="home-panel home-fade-in space-y-3">
            <h2 className="home-section-title">Formes</h2>
            <div className="flex flex-wrap gap-2">
                {forms.map((form) => {
                    const isActive = form.id === activeFormId;
                    return (
                        <button
                            key={form.id}
                            type="button"
                            onClick={() => onSelect(form)}
                            className={cn(
                                'home-toggle',
                                isActive && 'home-toggle-active',
                            )}
                            aria-pressed={isActive}
                        >
                            {form.displayName}
                            {form.battleOnly && (
                                <span className="ml-1 opacity-70 text-[0.7rem]">⚔</span>
                            )}
                        </button>
                    );
                })}
            </div>
        </section>
    );
}