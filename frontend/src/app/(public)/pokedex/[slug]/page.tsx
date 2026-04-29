'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import Link from 'next/link';
import { usePokemonDetail, usePokemonSpawns } from '@/hooks/use-pokemon-detail';
import { PokemonHero } from '@/components/pokemon/pokemon-hero';
import { PokemonFormsTabs } from '@/components/pokemon/pokemon-forms-tabs';
import { PokemonStats } from '@/components/pokemon/pokemon-stats';
import { PokemonSpawnsSection } from '@/components/pokemon/pokemon-spawns-section';
import { ROUTES } from '@/lib/constants/routes';
import type { PokemonForm } from '@/types/api/pokemon.types';

export default function PokemonDetailPage() {
    const { slug } = useParams<{ slug: string }>();
    const { data: pokemon, isLoading, isError } = usePokemonDetail(slug);
    const { data: spawns = [], isLoading: spawnsLoading } = usePokemonSpawns(slug);

    const [activeForm, setActiveForm] = useState<PokemonForm | null>(null);

    if (isLoading) {
        return (
            <div className="space-y-6">
                <div className="home-panel h-48 animate-pulse" />
                <div className="home-panel h-24 animate-pulse" />
                <div className="home-panel h-48 animate-pulse" />
            </div>
        );
    }

    if (isError || !pokemon) {
        return (
            <section className="home-empty-state home-fade-in">
                <p className="text-5xl mb-3">❌</p>
                <p className="home-empty-title">Pokémon introuvable</p>
                <p className="home-empty-desc mb-4">
                    Ce slug ne correspond à aucun Pokémon du dataset actif.
                </p>
                <Link href={ROUTES.pokedex} className="home-btn-ghost">
                    ← Retour au Pokédex
                </Link>
            </section>
        );
    }

    const defaultForm = pokemon.forms.find((f) => f.isDefault) ?? pokemon.forms[0];
    const currentForm = activeForm ?? defaultForm;

    return (
        <div className="space-y-6">

            {/* Fil d'Ariane */}
            <nav className="flex items-center gap-2 text-sm home-fade-in"
                 style={{ color: 'var(--color-text-secondary)' }}>
                <Link href={ROUTES.pokedex} className="hover:underline">
                    Pokédex
                </Link>
                <span>›</span>
                <span style={{ color: 'var(--color-text-primary)' }}>
                    {pokemon.displayName}
                </span>
            </nav>

            {/* Héros : key={currentForm.id} → remount, reset toggles shiny/femelle,
                      et feedback visuel garanti quand on change de forme. */}
            <PokemonHero
                key={currentForm.id}
                pokemon={pokemon}
                activeForm={currentForm}
            />

            {/* Sélecteur de formes */}
            <PokemonFormsTabs
                forms={pokemon.forms}
                activeFormId={currentForm.id}
                onSelect={(form) => setActiveForm(form)}
            />

            {/* Stats + infos en deux colonnes sur grand écran */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                <PokemonStats form={currentForm} />

                <section className="home-panel home-fade-in space-y-3">
                    <h2 className="home-section-title">Informations</h2>
                    <div className="space-y-2 text-sm">
                        <InfoLine label="Slug" value={pokemon.slug} mono />
                        <InfoLine
                            label="N° Pokédex"
                            value={`N° ${String(pokemon.nationalDexNumber).padStart(4, '0')}`}
                            mono
                        />
                        <InfoLine
                            label="Génération"
                            value={pokemon.generationCode}
                        />
                        <InfoLine
                            label="Implémenté"
                            value={pokemon.implemented ? '✅ Oui' : '❌ Non'}
                        />
                        <InfoLine
                            label="Forme de combat"
                            value={currentForm.battleOnly ? 'Oui' : 'Non'}
                        />
                        {pokemon.forms.length > 1 && (
                            <InfoLine
                                label="Nombre de formes"
                                value={String(pokemon.forms.length)}
                            />
                        )}
                    </div>
                </section>
            </div>

            {/* Spawns */}
            <PokemonSpawnsSection
                spawns={spawns.filter(
                    (s) => !currentForm || !s.formCode || s.formCode === currentForm.code
                )}
                isLoading={spawnsLoading}
            />

        </div>
    );
}

function InfoLine({
                      label,
                      value,
                      mono = false,
                  }: {
    label: string;
    value: string;
    mono?: boolean;
}) {
    return (
        <div className="flex justify-between gap-4">
            <span style={{ color: 'var(--color-text-secondary)' }}>{label}</span>
            <span
                className={`text-right ${mono ? 'font-mono' : 'font-medium'}`}
                style={{ color: 'var(--color-text-primary)' }}
            >
                {value}
            </span>
        </div>
    );
}