import { SpawnCard } from './spawn-card';
import type { SpawnRule } from '@/types/api/spawn.types';

interface PokemonSpawnsSectionProps {
    spawns: SpawnRule[];
    isLoading: boolean;
}

export function PokemonSpawnsSection({ spawns, isLoading }: PokemonSpawnsSectionProps) {
    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-white">Règles de spawn</h2>
                {!isLoading && (
                    <span className="text-sm" style={{ color: 'var(--color-text-secondary)' }}>
            {spawns.length} règle{spawns.length !== 1 ? 's' : ''}
          </span>
                )}
            </div>

            {isLoading && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {Array.from({ length: 4 }).map((_, i) => (
                        <div key={i} className="h-36 rounded-xl bg-white/5 animate-pulse" />
                    ))}
                </div>
            )}

            {!isLoading && spawns.length === 0 && (
                <div
                    className="rounded-xl border p-8 text-center"
                    style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-secondary)' }}
                >
                    <p className="text-3xl mb-2">🌫️</p>
                    <p>Aucune règle de spawn enregistrée pour ce Pokémon.</p>
                </div>
            )}

            {!isLoading && spawns.length > 0 && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {spawns.map((spawn) => (
                        <SpawnCard key={spawn.id} spawn={spawn} />
                    ))}
                </div>
            )}
        </div>
    );
}